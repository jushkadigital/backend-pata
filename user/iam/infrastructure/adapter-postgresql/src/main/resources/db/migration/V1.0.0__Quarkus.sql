CREATE TABLE quarkus.users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    user_type VARCHAR(50) NOT NULL,
    sync_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE quarkus.roles (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description VARCHAR(255),
    client_id VARCHAR(255)  NOT NULL,
    sync_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);


create TABLE quarkus.admins (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255)  NOT NULL,
    admin_type VARCHAR(50) NOT NULL,
    external_id VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

create TABLE quarkus.passengers (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255)  NOT NULL,
    passenger_type VARCHAR(50) NOT NULL,
    external_id VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
