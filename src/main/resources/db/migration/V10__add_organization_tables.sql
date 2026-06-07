-- Organization table
CREATE TABLE organization (
    id                  INT          NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(255) NOT NULL UNIQUE,
    description         TEXT,
    logo_path           VARCHAR(500),
    owner_person_id     INT          NOT NULL,
    subscription_tier   VARCHAR(50)  NOT NULL DEFAULT 'FREE',
    status              VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_org_owner FOREIGN KEY (owner_person_id) REFERENCES person(person_id)
);

-- Organization member table
CREATE TABLE organization_member (
    id              INT      NOT NULL AUTO_INCREMENT,
    organization_id INT      NOT NULL,
    person_id       INT      NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'STAFF',
    joined_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_person (organization_id, person_id),
    CONSTRAINT fk_member_org    FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_member_person FOREIGN KEY (person_id)       REFERENCES person(person_id)
);

-- Extend advertisement with org fields
ALTER TABLE advertisement
    ADD COLUMN organization_id      INT  NULL,
    ADD COLUMN posted_by_person_id  INT  NULL,
    ADD CONSTRAINT fk_ad_org    FOREIGN KEY (organization_id)     REFERENCES organization(id),
    ADD CONSTRAINT fk_ad_poster FOREIGN KEY (posted_by_person_id) REFERENCES person(person_id);

-- Extend vehicle with org field
ALTER TABLE vehicle
    ADD COLUMN organization_id INT NULL,
    ADD CONSTRAINT fk_vehicle_org FOREIGN KEY (organization_id) REFERENCES organization(id);
