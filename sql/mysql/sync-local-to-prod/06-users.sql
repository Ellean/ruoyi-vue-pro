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
-- Dumping data for table `system_users`
--
-- WHERE:  tenant_id=162 AND deleted=0

LOCK TABLES `system_users` WRITE;
/*!40000 ALTER TABLE `system_users` DISABLE KEYS */;
REPLACE INTO `system_users` (`id`, `username`, `password`, `nickname`, `remark`, `dept_id`, `post_ids`, `email`, `mobile`, `sex`, `avatar`, `status`, `login_ip`, `login_date`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (249,'张龙龙','$2a$04$L0W6Nrx6Nb1AdikP7aa9fu0BnomMeFsQRE74aYGfTX8LRMcQhP/eu','张龙龙','sync from legacy t_user',122,'[]','','',0,'',0,'0:0:0:0:0:0:0:1','2026-10-12 13:56:31','1','2026-10-04 11:19:07',NULL,'2026-10-12 13:56:31',_binary '\0',162),(250,'高少山','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','高少山','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(251,'段文燕','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','段文燕','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(252,'王舒宁','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','王舒宁','sync from legacy t_user',124,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(253,'杜豪','$2a$04$2lCJOg7AF8hpGq1zmTP2SumnKSKv48ThpApZmZpydnkNp9zeNU1hS','杜豪','sync from legacy t_user',124,NULL,'','',0,'',0,'0:0:0:0:0:0:0:1','2026-10-12 14:53:31','1','2026-10-04 11:19:07',NULL,'2026-10-12 14:53:31',_binary '\0',162),(254,'刘云丽','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','刘云丽','sync from legacy t_user',124,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(255,'侯佳佳','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','侯佳佳','sync from legacy t_user',126,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(256,'王俊','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','王俊','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(257,'刘源','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','刘源','sync from legacy t_user',125,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(258,'尹宁','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','尹宁','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(259,'张雄雄','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','张雄雄','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(260,'王霄峰','$2a$04$u05iH4D57Si09dxf.2N4oOBmfFEzojpHWIMic9bh39A82Eh/s2qYy','王霄峰','sync from legacy t_user',122,NULL,'','',0,'',0,'0:0:0:0:0:0:0:1','2026-10-10 15:03:34','1','2026-10-04 11:19:07',NULL,'2026-10-10 15:03:34',_binary '\0',162),(261,'宋瑞琴','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','宋瑞琴','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(262,'冀雅丽','$2b$10$N5yCovGaYnIHHqlCbe8UvOQPD0RmrihYbZ3HQ8CakOpz03G8zdymO','冀雅丽','sync from legacy t_user',122,NULL,'','',0,'',0,'',NULL,'1','2026-10-04 11:19:07','1','2026-10-04 11:27:01',_binary '\0',162),(263,'牛鑫江','$2a$04$vfWeZsOcfT79wlJUecxGiu1sRNPDMIpNZR1h0uzvoCN5H1.3C00Wu','牛鑫江','sync from legacy t_user',122,'[]','','',0,'',0,'0:0:0:0:0:0:0:1','2026-10-10 10:30:39','1','2026-10-04 11:19:07',NULL,'2026-10-10 10:30:39',_binary '\0',162);
/*!40000 ALTER TABLE `system_users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-13 11:53:16
