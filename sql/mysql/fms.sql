-- FMS MySQL schema (converted from module test H2 DDL)
-- Import into ruoyi-vue-pro
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- FMS 单元测试表结构，与 sql/mysql/fms.sql 业务字段保持同步

CREATE TABLE IF NOT EXISTS `fms_account_set` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `company_code` varchar(64) DEFAULT NULL,
    `company_name` varchar(255) DEFAULT NULL,
    `company_profile` varchar(500) DEFAULT NULL,
    `industry` varchar(255) DEFAULT NULL,
    `location` varchar(255) DEFAULT NULL,
    `legal_representative` varchar(255) DEFAULT NULL,
    `legal_representative_id_number` varchar(255) DEFAULT NULL,
    `business_license_number` varchar(255) DEFAULT NULL,
    `organization_code` varchar(255) DEFAULT NULL,
    `remark` varchar(500) DEFAULT NULL,
    `contact_name` varchar(255) DEFAULT NULL,
    `office_telephone` varchar(32) DEFAULT NULL,
    `mobile` varchar(32) DEFAULT NULL,
    `fax_number` varchar(32) DEFAULT NULL,
    `qq_number` varchar(255) DEFAULT NULL,
    `email` varchar(255) DEFAULT NULL,
    `other_contact` varchar(255) DEFAULT NULL,
    `address` varchar(255) DEFAULT NULL,
    `currency_id` bigint DEFAULT NULL,
    `start_time` datetime DEFAULT NULL,
    `standard` int DEFAULT NULL,
    `initialized` bit(1) NOT NULL DEFAULT b'0',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE (`company_code`, `deleted`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_finance_parameter` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `account_set_id` bigint NOT NULL,
    `level` int NOT NULL,
    `subject_code_rule` varchar(64) NOT NULL,
    `ledger_balance_mode` int NOT NULL,
    `deficit_check` bit(1) NOT NULL DEFAULT b'0',
    `voucher_review_required` bit(1) NOT NULL DEFAULT b'0',
    `asset_period_locked` bit(1) NOT NULL DEFAULT b'0',
    `taxpayer_name` varchar(255) DEFAULT NULL,
    `taxpayer_number` varchar(64) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE (`account_set_id`, `deleted`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_account_user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `account_set_id` bigint DEFAULT NULL,
    `user_id` bigint DEFAULT NULL,
    `default_status` bit(1) NOT NULL DEFAULT b'0',
    `founder` bit(1) NOT NULL DEFAULT b'0',
    `level` int NOT NULL DEFAULT 2,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_currency` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `exchange_rate` decimal(18,6) NOT NULL DEFAULT 0,
    `standard` bit(1) NOT NULL DEFAULT b'0',
    `account_set_id` bigint DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE (`account_set_id`, `code`, `deleted`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_subject` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `parent_id` bigint NOT NULL DEFAULT 0,
    `type` int DEFAULT NULL,
    `category` int DEFAULT NULL,
    `balance_direction` int DEFAULT NULL,
    `quantity_unit` varchar(255) DEFAULT NULL,
    `cash` bit(1) NOT NULL DEFAULT b'0',
    `status` tinyint NOT NULL DEFAULT 0,
    `level` int DEFAULT NULL,
    `quantity_accounting` bit(1) NOT NULL DEFAULT b'0',
    `account_set_id` bigint DEFAULT NULL,
    `auxiliary_type_ids` varchar(10000) DEFAULT NULL,
    `currency_ids` varchar(4000) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_subject_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `parent_id` bigint NOT NULL DEFAULT 0,
    `type` int DEFAULT NULL,
    `category` int DEFAULT NULL,
    `balance_direction` int DEFAULT NULL,
    `quantity_unit` varchar(255) DEFAULT NULL,
    `cash` bit(1) NOT NULL DEFAULT b'0',
    `status` int DEFAULT NULL,
    `level` int DEFAULT NULL,
    `quantity_accounting` bit(1) NOT NULL DEFAULT b'0',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_auxiliary_type` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `system_preset` bit(1) NOT NULL DEFAULT b'0',
    `account_set_id` bigint DEFAULT NULL,
    `type` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE (`account_set_id`, `name`, `deleted`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_auxiliary_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `auxiliary_type_id` bigint DEFAULT NULL,
    `status` int DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `remark` varchar(500) DEFAULT NULL,
    `specification` varchar(255) DEFAULT NULL,
    `unit` varchar(255) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE (`account_set_id`, `auxiliary_type_id`, `code`, `deleted`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_assist_combination` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `subject_id` bigint DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `items` varchar(10000) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_initial_balance` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `subject_id` bigint DEFAULT NULL,
    `auxiliary_accounting` bit(1) NOT NULL DEFAULT b'0',
    `opening_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `opening_quantity` decimal(18,4) NOT NULL DEFAULT 0,
    `year_debit_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_debit_quantity` decimal(18,4) NOT NULL DEFAULT 0,
    `year_credit_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_credit_quantity` decimal(18,4) NOT NULL DEFAULT 0,
    `year_opening_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_opening_quantity` decimal(18,4) NOT NULL DEFAULT 0,
    `profit_loss_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `profit_loss_quantity` decimal(18,4) NOT NULL DEFAULT 0,
    `account_set_id` bigint DEFAULT NULL,
    `assist_balances` varchar(10000) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE (`account_set_id`, `subject_id`, `deleted`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_voucher` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `voucher_word_id` bigint DEFAULT NULL,
    `voucher_number` int DEFAULT NULL,
    `voucher_time` datetime DEFAULT NULL,
    `voucher_period` int GENERATED ALWAYS AS (YEAR(`voucher_time`) * 100 + MONTH(`voucher_time`)) STORED,
    `attachment_urls` varchar(10000) DEFAULT NULL,
    `attachment_count` int NOT NULL DEFAULT 0,
    `debit_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `credit_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `total` decimal(18,2) NOT NULL DEFAULT 0.00,
    `status` int DEFAULT NULL,
    `reviewer_user_id` bigint DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_voucher_entry` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `digest` varchar(500) DEFAULT NULL,
    `subject_name` varchar(255) DEFAULT NULL,
    `quantity` decimal(18,4) NOT NULL DEFAULT 0,
    `debit_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `credit_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `voucher_id` bigint DEFAULT NULL,
    `subject_code` varchar(64) DEFAULT NULL,
    `sort` int DEFAULT NULL,
    `subject_id` bigint DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `unit_price` decimal(18,6) NOT NULL DEFAULT 0,
    `assist_combination_id` bigint DEFAULT NULL,
    `auxiliaries` varchar(10000) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_voucher_word` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `print_title` varchar(255) DEFAULT NULL,
    `default_status` bit(1) NOT NULL DEFAULT b'0',
    `sort` int NOT NULL DEFAULT 0,
    `account_set_id` bigint DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_digest` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `content` varchar(500) NOT NULL,
    `account_set_id` bigint NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_finance_indicator` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `account_set_id` bigint NOT NULL,
    `name` varchar(100) NOT NULL,
    `code` varchar(64) NOT NULL,
    `type` int NOT NULL,
    `formula` varchar(2000) NOT NULL,
    `sort` int NOT NULL DEFAULT 0,
    `status` int NOT NULL DEFAULT 0,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_voucher_template_category` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `account_set_id` bigint NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_voucher_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `category_id` bigint NOT NULL,
    `entries` text NOT NULL,
    `account_set_id` bigint NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_balance_sheet_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `sort` int DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `level` int DEFAULT NULL,
    `row_id` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_balance_sheet_report` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `from_period` int DEFAULT NULL,
    `to_period` int DEFAULT NULL,
    `type` int DEFAULT NULL,
    `level` int DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `sort` int DEFAULT NULL,
    `opening_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `closing_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `account_set_id` bigint DEFAULT NULL,
    `settled` bit(1) NOT NULL DEFAULT b'0',
    `row_id` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_income_statement_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `sort` int DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `account_set_id` bigint DEFAULT NULL,
    `level` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_income_statement_report` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `type` int DEFAULT NULL,
    `from_period` int DEFAULT NULL,
    `to_period` int DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `sort` int DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `current_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `account_set_id` bigint DEFAULT NULL,
    `level` int DEFAULT NULL,
    `settled` bit(1) NOT NULL DEFAULT b'0',
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_cash_flow_statement_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `sort` int DEFAULT NULL,
    `category` int DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `level` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_cash_flow_statement_report` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `from_period` int DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `current_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `sort` int DEFAULT NULL,
    `category` int DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `to_period` int DEFAULT NULL,
    `type` int DEFAULT NULL,
    `level` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_cash_flow_extend_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `category` int DEFAULT NULL,
    `type` int DEFAULT NULL,
    `current_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `account_set_id` bigint DEFAULT NULL,
    `sort` int DEFAULT NULL,
    `level` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_cash_flow_extend_data` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `category` int DEFAULT NULL,
    `current_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `year_amount` decimal(18,2) NOT NULL DEFAULT 0,
    `from_period` int DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `account_set_id` bigint DEFAULT NULL,
    `sort` int DEFAULT NULL,
    `to_period` int DEFAULT NULL,
    `type` int DEFAULT NULL,
    `level` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_report_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `row_no` int DEFAULT NULL,
    `formula` text,
    `remark` varchar(500) DEFAULT NULL,
    `editable` bit(1) NOT NULL DEFAULT b'0',
    `sort` int DEFAULT NULL,
    `row_id` int DEFAULT NULL,
    `type` int DEFAULT NULL,
    `category` int DEFAULT NULL,
    `level` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_closing` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255) DEFAULT NULL,
    `period_end` bit(1) NOT NULL DEFAULT b'0',
    `subject_id` bigint DEFAULT NULL,
    `formula_rule` int DEFAULT NULL,
    `time_type` int DEFAULT NULL,
    `voucher_word_id` bigint DEFAULT NULL,
    `digest` varchar(500) DEFAULT NULL,
    `voucher_type` int DEFAULT NULL,
    `prior_year_adjustment_subject_id` bigint DEFAULT NULL,
    `adjustment_closing_subject_id` bigint DEFAULT NULL,
    `other_closing_subject_id` bigint DEFAULT NULL,
    `reverse_balance` bit(1) NOT NULL DEFAULT b'0',
    `type` int DEFAULT NULL,
    `account_set_id` bigint DEFAULT NULL,
    `closing_day` int DEFAULT NULL,
    `subject_rules` varchar(10000) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_closing_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `account_set_id` bigint DEFAULT NULL,
    `preset_code` varchar(64) DEFAULT NULL,
    `name` varchar(255) DEFAULT NULL,
    `category` int DEFAULT NULL,
    `period_end` bit(1) NOT NULL DEFAULT b'0',
    `subject_id` bigint DEFAULT NULL,
    `formula_rule` int DEFAULT NULL,
    `time_type` int DEFAULT NULL,
    `subject_rules` varchar(10000) DEFAULT NULL,
    `sort` int DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `fms_closing_voucher` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `closing_id` bigint DEFAULT NULL,
  `voucher_id` bigint DEFAULT NULL,
  `voucher_time` datetime DEFAULT NULL,
  `amount` decimal(24,6) NOT NULL DEFAULT 0,
  `closed` bit(1) NOT NULL DEFAULT b'0',
  `account_set_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `fms_closing_period` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `closing_time` datetime DEFAULT NULL,
  `account_set_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
