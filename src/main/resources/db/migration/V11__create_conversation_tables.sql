CREATE TABLE conversation (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    vehicle_source_id VARCHAR(255) NOT NULL,
    buyer_person_id   INT          NOT NULL,
    seller_person_id  INT          NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_buyer  FOREIGN KEY (buyer_person_id)  REFERENCES person(person_id),
    CONSTRAINT fk_conv_seller FOREIGN KEY (seller_person_id) REFERENCES person(person_id),
    CONSTRAINT uq_conversation UNIQUE (vehicle_source_id, buyer_person_id)
);

CREATE TABLE message (
    id               BIGINT   AUTO_INCREMENT PRIMARY KEY,
    conversation_id  BIGINT   NOT NULL,
    sender_person_id INT      NOT NULL,
    content          TEXT     NOT NULL,
    sent_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at          DATETIME NULL,
    CONSTRAINT fk_msg_conv   FOREIGN KEY (conversation_id)  REFERENCES conversation(id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_person_id) REFERENCES person(person_id)
);
