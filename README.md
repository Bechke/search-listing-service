# prerequisites installations
1. Java 21
2. Docker desktop
3. Minikube(optional)
4. kubectl(optional)

# gradle
./gradlew clean build --refresh-dependencies -x test
./gradlew clean assemble
./gradlew build
./gradlew dependencies #check for dependencies conflict
./gradlew build --continuous #continuous build feature to automatically rebuild when files change.
./gradlew bootRun -Dspring.profiles.active=dev


# Docker
docker build -t search-listing-service:latest .
docker-compose up --build
docker-compose up -d
# optional
docker images
docker run -p 9191:9191 search-listing-service:latest

# Stop App and Mysql image
docker-compose stop

# Removes App containers, networks and  the volumes will also be removed (all data stored in them will be lost).
docker-compose down -v


# Local Url
http://localhost:9191/swagger-ui/index.html