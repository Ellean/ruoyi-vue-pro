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
-- Dumping data for table `system_role`
--
-- WHERE:  tenant_id=162 AND deleted=0

LOCK TABLES `system_role` WRITE;
/*!40000 ALTER TABLE `system_role` DISABLE KEYS */;
REPLACE INTO `system_role` (`id`, `name`, `code`, `sort`, `data_scope`, `data_scope_dept_ids`, `status`, `type`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (12804,'文案人员','xq_copy',20,1,'',0,2,'工作台文案：生成编辑文案','1','2026-10-04 11:27:01','1','2026-10-04 11:27:01',_binary '\0',162),(12805,'图片人员','xq_image',21,1,'',0,2,'工作台图片：文案完成后生成图片','1','2026-10-04 11:27:01','1','2026-10-04 11:27:01',_binary '\0',162),(12806,'店链管理员','xq_store_admin',22,1,'',0,2,'维护上架平台、店链账号、映射与用户绑店','1','2026-10-04 11:27:01','1','2026-10-04 11:27:01',_binary '\0',162),(12809,'财务','xq_finance',23,1,'',0,2,'原库财务岗：FMS 财务管理','1','2026-10-04 13:09:43','1','2026-10-04 13:09:43',_binary '\0',162),(12810,'采购','xq_purchase',24,1,'',0,2,'原库采购岗','1','2026-10-04 13:09:43','1','2026-10-04 13:09:43',_binary '\0',162),(12811,'租户管理员','tenant_admin',0,1,'',0,1,'系统自动生成','1','2026-10-05 14:46:10','1','2026-10-05 14:46:10',_binary '\0',162),(12812,'管理员','xq_GLY',1,1,'',0,2,NULL,'1','2026-10-05 14:56:33','1','2026-10-05 14:56:33',_binary '\0',162);
/*!40000 ALTER TABLE `system_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-13 11:53:15
