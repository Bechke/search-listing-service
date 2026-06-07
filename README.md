# Search Listing Service

Maintains the searchable vehicle index, advertisement records, organisation profiles, location hierarchy, and vehicle metadata for the Bechke marketplace. Consumes vehicle events from Kafka and exposes REST APIs for search and management.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3 / Spring MVC (servlet) |
| Database | MySQL (JPA + Flyway migrations) |
| Messaging | Kafka consumer (topic: `vehicle-ads`) |
| Service discovery | Eureka client |
| Port | 9191 |

---

## Role in Platform

- Receives `VehicleAdEvent` messages from `vehicle-service` via Kafka and upserts Vehicle + Advertisement rows
- Serves all search/browse queries from the mobile app through the gateway (`/listings/**`)
- Stores Person, Organization, and OrganizationMember rows created during registration and org setup
- Hosts the location hierarchy (countries → states → cities → neighbourhoods) used by the location picker

---

## Database Tables

**person** — one row per registered user
```
person_id PK, keycloak_id UNIQUE, full_name, email, mobile_number, company, timestamps
```
Created via `POST /api/v1/persons/sync` (called by gateway on registration) or lazily by the Kafka consumer on first vehicle post.

**vehicle** — search index for vehicle listings
```
vehicle_id PK, vehicle_source_id UNIQUE (= MongoDB _id), person_id FK, organization_id FK
adSubcategory, brand, year, fuelType, transmission, odometer, numOwners
title, description, price, feature_image, image_urls_json
country, state, city, neighbourhood, listing_status, timestamps
```

**advertisement** — ad records linked to vehicles
```
advertisement_id PK, vehicle_source_id UNIQUE, person_id FK, organization_id FK
posted_by_person_id FK, adCategory, adSubcategory, title, defaultImgPath
country, state, city, neighbourhood, status, timestamps
```

**organization**
```
id PK, name, slug UNIQUE, description, logo_path
owner_person_id FK → person, subscription_tier DEFAULT 'FREE', status DEFAULT 'ACTIVE', timestamps
```

**organization_member**
```
id PK, organization_id FK, person_id FK, role DEFAULT 'STAFF', joined_at
UNIQUE(organization_id, person_id)
```

**Location hierarchy:** country → state → city → neighbourhood (pre-seeded with Indian cities)
**Vehicle metadata:** vehicle_category, vehicle_brand

---

## Kafka Consumer

`VehicleConsumerListener` listens on topic `vehicle-ads`:

| Event type | Action |
|-----------|--------|
| `CREATE` | Upsert Person (by `keycloakId`) + insert Vehicle row + insert Advertisement row |
| `UPDATE` | Update Vehicle and Advertisement rows |
| `DELETE` | Soft-delete (set `listing_status = DELETED`) |

The Person upsert uses `orElseGet` — if the Person row doesn't exist yet (e.g. gateway sync hadn't fired), a stub is created automatically.

---

## REST Endpoints

### Persons (internal)

```
POST /api/v1/persons/sync
Body: { keycloakId, fullName, email, mobileNumber, company? }
Upserts a Person row. Called by the gateway on registration.
```

### Vehicles (search index)

```
GET  /api/v1/vehicles/search?country=&state=&city=&neighbourhood=&brand=&subCategory=&minPrice=&maxPrice=
GET  /api/v1/vehicles/{id}
GET  /api/v1/vehicles          (paginated list)
```

### Advertisements

```
GET    /api/v1/ads             — all ads
GET    /api/v1/ads/my          — current user's ads (requires X-User-Id header)
GET    /api/v1/ads/org/{orgId} — org's ads
GET    /api/v1/ads/{id}
POST   /api/v1/ads
PUT    /api/v1/ads/{id}
DELETE /api/v1/ads/{id}
```

### Organizations

```
POST   /api/v1/orgs                           — create org
GET    /api/v1/orgs/my                        — user's org memberships (requires X-User-Id)
GET    /api/v1/orgs/{slug}                    — public org profile
PUT    /api/v1/orgs/{id}                      — update org (owner only)
GET    /api/v1/orgs/search?q=                 — search orgs by name
GET    /api/v1/orgs/{id}/members              — list members (requires membership)
POST   /api/v1/orgs/{id}/members              — add member (owner only)
PUT    /api/v1/orgs/{id}/members/{personId}   — change member role
DELETE /api/v1/orgs/{id}/members/{personId}   — remove member
```

### Locations

```
GET /api/v1/locations/countries
GET /api/v1/locations/states/{countryId}
GET /api/v1/locations/cities/{stateId}
GET /api/v1/locations/neighbourhoods/{cityId}
```

### Brands & Categories

```
GET /api/v1/vehicle-brands
GET /api/v1/vehicle-brands/category/{categoryName}
GET /api/v1/vehicle-categories
```

---

## Environment Variables

| Variable | Description | Docker default |
|----------|-------------|---------------|
| `MYSQL_DATABASE` / `SPRING_DATASOURCE_URL` | MySQL connection | see compose.yaml |
| `MYSQL_USER` / `SPRING_DATASOURCE_USERNAME` | MySQL user | — |
| `MYSQL_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | MySQL password | — |
| `DISCOVERY_URL` | Eureka server URL | `http://discovery:8761` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker | `kafka-container:9092` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |

---

## Running with Docker

```bash
cd C:\Bechke-Workspace\search-listing-service

# Build the JAR first
./gradlew clean build --refresh-dependencies -x test

# Start MySQL + Kafka + search-listing-service
docker-compose -f compose.yaml up -d --build

# Stop
docker-compose -f compose.yaml stop

# Rebuild without cache
docker-compose -f compose.yaml build --no-cache
docker-compose -f compose.yaml up -d
```

The `bechke-network` Docker bridge network must already exist (created by `gateway-service/docker-compose.yml`).

Flyway runs automatically on startup and applies all migrations in `src/main/resources/db/migration/V*.sql`, including the seed data (`V5__seed_sample_vehicles.sql`).

### Useful Gradle commands

```bash
./gradlew clean build --refresh-dependencies -x test
./gradlew build
./gradlew dependencies                    # check for dependency conflicts
./gradlew bootRun -Dspring.profiles.active=dev
```

---

## Swagger UI

```
http://localhost:9191/swagger-ui/index.html
```

---

## Schema Migrations

Managed by Flyway. Migration files live in `src/main/resources/db/migration/`:

- `V1__*` — initial schema
- `V5__seed_sample_vehicles.sql` — 5 demo listings for local development (idempotent)

`spring.jpa.hibernate.ddl-auto=none` — Hibernate never touches the schema; Flyway owns all DDL.
