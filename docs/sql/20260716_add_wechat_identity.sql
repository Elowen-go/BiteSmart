CREATE TABLE IF NOT EXISTS `user_third_party_identity` (
  `id` bigint(20) NOT NULL COMMENT 'primary key',
  `user_id` bigint(20) NOT NULL COMMENT 'BiteSmart user id',
  `provider` varchar(32) NOT NULL COMMENT 'identity provider',
  `open_id` varchar(128) NOT NULL COMMENT 'provider open id',
  `union_id` varchar(128) DEFAULT NULL COMMENT 'provider union id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_open_id` (`provider`, `open_id`),
  UNIQUE KEY `uk_user_provider` (`user_id`, `provider`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_third_party_identity_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user third-party identity binding';
