-- =============================================================================
-- Tappsy Backend — MySQL 8 Database Schema
-- Clean DDL-only schema synchronized with Hibernate JPA entities (User, FriendList, Chat)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `tappsy`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `tappsy`;

-- -----------------------------------------------------------------------------
-- Table: user
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(45) NOT NULL,
  `last_name` VARCHAR(45) NOT NULL,
  `country_code` VARCHAR(5) NOT NULL,
  `contact_no` VARCHAR(45) NOT NULL,
  `pincode` VARCHAR(100) NOT NULL,
  `status` VARCHAR(45) NOT NULL DEFAULT 'OFFLINE',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_contact_no` (`contact_no`),
  UNIQUE KEY `uk_user_pincode` (`pincode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- Table: friend_list
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `friend_list` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT DEFAULT NULL,
  `friend_id` INT DEFAULT NULL,
  `user_status` VARCHAR(30) DEFAULT 'ACTIVE',
  `display_name` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_friend_list_user_id` (`user_id`),
  KEY `idx_friend_list_friend_id` (`friend_id`),
  CONSTRAINT `fk_friend_list_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_friend_list_friend` FOREIGN KEY (`friend_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- Table: chat
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `from_user` INT DEFAULT NULL,
  `to_user` INT DEFAULT NULL,
  `message` LONGTEXT NOT NULL,
  `files` LONGTEXT NOT NULL,
  `status` VARCHAR(30) DEFAULT 'SENT',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_from_user` (`from_user`),
  KEY `idx_chat_to_user` (`to_user`),
  CONSTRAINT `fk_chat_from_user` FOREIGN KEY (`from_user`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_chat_to_user` FOREIGN KEY (`to_user`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
