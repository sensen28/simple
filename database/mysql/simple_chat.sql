-- Bootstrap only: create the database first, then let Flyway manage all schema changes.
CREATE DATABASE IF NOT EXISTS `simple_chat`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
