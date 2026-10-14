-- AI API 密钥 + 模型：本地 → 线上同步
-- 来源：本地 ruoyi-vue-pro.ai_api_key / ai_model（deleted=0）
-- 含租户 1 与 162（星企）；REPLACE 按主键覆盖
-- 执行前确认线上已有扩展字段（gateway_type 等，见 ai-api-key-relay-config*.sql）
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- MySQL dump 10.13  Distrib 8.0.13, for Win64 (x86_64)
--
-- Host: localhost    Database: ruoyi-vue-pro
-- ------------------------------------------------------
-- Server version	8.0.13

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 SET NAMES utf8mb4 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `ai_api_key`
--
-- WHERE:  deleted=0

LOCK TABLES `ai_api_key` WRITE;
/*!40000 ALTER TABLE `ai_api_key` DISABLE KEYS */;
REPLACE INTO `ai_api_key` (`id`, `name`, `api_key`, `platform`, `url`, `gateway_type`, `capabilities`, `chat_model`, `vision_model`, `image_model`, `image_edit_model`, `image_body_style`, `supports_async`, `prefer_responses_api`, `vision_image_detail`, `extra_config`, `remark`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1,'gpt-image-2.5-flare','sk-wwS7WtGFzEjIwDLFjsreeEoAx8wsmdZmtg6sEPynvNPgGD7T','OpenAI','https://api.aixoras.com/v1','openai_compatible','chat,vision',NULL,NULL,NULL,NULL,NULL,0,0,'low',NULL,NULL,0,'1','2026-09-24 16:22:30','1','2026-09-24 17:24:01',_binary '\0',1);
REPLACE INTO `ai_api_key` (`id`, `name`, `api_key`, `platform`, `url`, `gateway_type`, `capabilities`, `chat_model`, `vision_model`, `image_model`, `image_edit_model`, `image_body_style`, `supports_async`, `prefer_responses_api`, `vision_image_detail`, `extra_config`, `remark`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2,'gpt-5.6-terra','sk-XUrX74Wd4mTJIrLOv3gB6avv8wgrAtbppSws7jfw031o7CD0','OpenAI','https://api.aixoras.com/v1','openai_compatible','chat,vision',NULL,NULL,NULL,NULL,NULL,0,0,'low',NULL,NULL,0,'1','2026-10-03 15:29:38','1','2026-10-03 15:34:49',_binary '\0',1);
REPLACE INTO `ai_api_key` (`id`, `name`, `api_key`, `platform`, `url`, `gateway_type`, `capabilities`, `chat_model`, `vision_model`, `image_model`, `image_edit_model`, `image_body_style`, `supports_async`, `prefer_responses_api`, `vision_image_detail`, `extra_config`, `remark`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (5,' gpt-image-2.5-flare-to','sk-ryIxJC8XswRyOL5EAEV44T3Cz23ohPqrYg0BsEgNLFeu96Xd','OpenAI','https://toapis.cn/v1','openai_compatible','vision,image_gen',' gpt-image-2.5-flare',' gpt-image-2.5-flare',' gpt-image-2.5-flare',NULL,'openai',0,0,'low',NULL,NULL,0,'249','2026-10-08 11:18:04','249','2026-10-10 13:20:34',_binary '\0',162);
REPLACE INTO `ai_api_key` (`id`, `name`, `api_key`, `platform`, `url`, `gateway_type`, `capabilities`, `chat_model`, `vision_model`, `image_model`, `image_edit_model`, `image_body_style`, `supports_async`, `prefer_responses_api`, `vision_image_detail`, `extra_config`, `remark`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (6,'gpt-5.6-terra-toapis','sk-rXZ9oZMENFV2VhGPW41SLMqC3GgayWNkg6nBunkNHj4LKi7H','OpenAI',' https://toapis.cn/v1','openai_compatible','chat,vision','gpt-5.6-terra','gpt-5.6-terra',NULL,NULL,'openai',0,0,'low',NULL,NULL,0,'249','2026-10-08 15:05:44','249','2026-10-08 15:16:33',_binary '\0',162);
/*!40000 ALTER TABLE `ai_api_key` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `ai_model`
--
-- WHERE:  deleted=0

LOCK TABLES `ai_model` WRITE;
/*!40000 ALTER TABLE `ai_model` DISABLE KEYS */;
REPLACE INTO `ai_model` (`id`, `key_id`, `name`, `model`, `platform`, `type`, `sort`, `status`, `temperature`, `max_tokens`, `max_contexts`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1,1,'gpt-image-2.5-flare','gpt-image-2.5-flare','OpenAI',2,1,0,NULL,NULL,NULL,'1','2026-09-24 16:47:26','1','2026-09-24 17:06:14',_binary '\0',1);
REPLACE INTO `ai_model` (`id`, `key_id`, `name`, `model`, `platform`, `type`, `sort`, `status`, `temperature`, `max_tokens`, `max_contexts`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2,2,'gpt-5.6-terra','gpt-5.6-terra','OpenAI',1,2,0,2,8192,4,'1','2026-10-03 15:30:44','1','2026-10-03 15:35:03',_binary '\0',1);
REPLACE INTO `ai_model` (`id`, `key_id`, `name`, `model`, `platform`, `type`, `sort`, `status`, `temperature`, `max_tokens`, `max_contexts`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (5,5,' gpt-image-2.5-flare',' gpt-image-2.5-flare','OpenAI',2,2,0,NULL,NULL,NULL,'249','2026-10-08 11:18:23','249','2026-10-08 11:18:23',_binary '\0',162);
REPLACE INTO `ai_model` (`id`, `key_id`, `name`, `model`, `platform`, `type`, `sort`, `status`, `temperature`, `max_tokens`, `max_contexts`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (6,6,'gpt-5.6-terra','gpt-5.6-terra','OpenAI',1,3,0,1,8192,1,'249','2026-10-08 15:07:40','249','2026-10-08 15:07:54',_binary '\0',162);
/*!40000 ALTER TABLE `ai_model` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-14  9:55:56

