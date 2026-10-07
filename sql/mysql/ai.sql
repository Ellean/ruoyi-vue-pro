-- AI MySQL schema generated from yudao-module-ai DO (no official zip available offline)
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `ai_api_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `api_key` varchar(255) NOT NULL COMMENT '密钥',
  `platform` varchar(255) NOT NULL COMMENT '平台',
  `url` varchar(255) DEFAULT NULL COMMENT 'API Base URL（官方或中转）',
  `gateway_type` varchar(32) DEFAULT 'openai_compatible' COMMENT '接入类型：openai_official/aixoras/hao/toapis/cun/openai_compatible/custom',
  `capabilities` varchar(128) DEFAULT 'chat,vision' COMMENT '能力：chat,vision,image_gen,image_edit',
  `chat_model` varchar(128) DEFAULT NULL COMMENT '对话模型（可选）',
  `vision_model` varchar(128) DEFAULT NULL COMMENT '识图模型（可选）',
  `image_model` varchar(128) DEFAULT NULL COMMENT '生图模型（可选）',
  `image_edit_model` varchar(128) DEFAULT NULL COMMENT '改图模型（可选）',
  `image_body_style` varchar(32) DEFAULT NULL COMMENT '生图请求风格（中转用）',
  `supports_async` tinyint DEFAULT NULL COMMENT '异步生图',
  `prefer_responses_api` tinyint DEFAULT NULL COMMENT '优先 Responses',
  `vision_image_detail` varchar(16) DEFAULT NULL COMMENT '识图 detail',
  `extra_config` text DEFAULT NULL COMMENT '扩展 JSON',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `status` int NOT NULL COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI API 密钥（一条=官方或某一中转）';

CREATE TABLE IF NOT EXISTS `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `key_id` bigint NOT NULL COMMENT 'API 秘钥编号',
  `name` varchar(64) NOT NULL COMMENT '模型名字',
  `model` varchar(64) NOT NULL COMMENT '模型标识',
  `platform` varchar(32) NOT NULL COMMENT '模型平台',
  `type` int NOT NULL COMMENT '模型类型',
  `sort` int NOT NULL COMMENT '排序',
  `status` tinyint NOT NULL COMMENT '状态',
  `temperature` double DEFAULT NULL COMMENT '温度参数',
  `max_tokens` int DEFAULT NULL COMMENT '单条回复最大 Token',
  `max_contexts` int DEFAULT NULL COMMENT '上下文最大 Message 数',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型表';

CREATE TABLE IF NOT EXISTS `ai_chat_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NOT NULL COMMENT '角色名称',
  `avatar` varchar(512) DEFAULT NULL COMMENT '角色头像',
  `category` varchar(64) DEFAULT NULL COMMENT '角色分类',
  `description` varchar(512) DEFAULT NULL COMMENT '角色描述',
  `system_message` varchar(2048) DEFAULT NULL COMMENT '角色设定',
  `user_id` bigint DEFAULT NULL COMMENT '用户编号',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `knowledge_ids` varchar(1024) DEFAULT NULL COMMENT '知识库编号列表',
  `tool_ids` varchar(1024) DEFAULT NULL COMMENT '工具编号列表',
  `mcp_client_names` varchar(1024) DEFAULT NULL COMMENT 'MCP Client 名字列表',
  `public_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天角色表';

CREATE TABLE IF NOT EXISTS `ai_chat_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对话编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `title` varchar(256) NOT NULL COMMENT '对话标题',
  `pinned` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶',
  `pinned_time` datetime DEFAULT NULL COMMENT '置顶时间',
  `role_id` bigint DEFAULT NULL COMMENT '聊天角色',
  `model_id` bigint NOT NULL COMMENT '模型编号',
  `model` varchar(64) NOT NULL COMMENT '模型标识',
  `system_message` varchar(1024) DEFAULT NULL COMMENT '角色设定',
  `temperature` double NOT NULL COMMENT '温度参数',
  `max_tokens` int NOT NULL COMMENT '单条回复最大 Token',
  `max_contexts` int NOT NULL COMMENT '上下文最大 Message 数',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天对话表';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `conversation_id` bigint NOT NULL COMMENT '对话编号',
  `reply_id` bigint DEFAULT NULL COMMENT '回复消息编号',
  `type` varchar(32) NOT NULL COMMENT '消息类型',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `role_id` bigint DEFAULT NULL COMMENT '角色编号',
  `model` varchar(64) DEFAULT NULL COMMENT '模型标识',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `content` text COMMENT '消息内容',
  `reasoning_content` text COMMENT '推理内容',
  `use_context` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否携带上下文',
  `segment_ids` varchar(2048) DEFAULT NULL COMMENT '知识库段落编号',
  `web_search_pages` text COMMENT '联网搜索网页',
  `attachment_urls` varchar(2048) DEFAULT NULL COMMENT '附件 URL',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 聊天消息表';

CREATE TABLE IF NOT EXISTS `ai_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `prompt` varchar(2048) NOT NULL COMMENT '提示词',
  `platform` varchar(64) NOT NULL COMMENT '平台',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `model` varchar(64) DEFAULT NULL COMMENT '模型标识',
  `width` int DEFAULT NULL COMMENT '宽度',
  `height` int DEFAULT NULL COMMENT '高度',
  `status` int NOT NULL COMMENT '状态',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `error_message` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `pic_url` varchar(1024) DEFAULT NULL COMMENT '图片地址',
  `public_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `options` text COMMENT '绘制参数',
  `buttons` text COMMENT 'mj buttons',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务编号',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 绘画表';

CREATE TABLE IF NOT EXISTS `ai_music` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `title` varchar(255) DEFAULT NULL COMMENT '音乐名称',
  `lyric` text COMMENT '歌词',
  `image_url` varchar(1024) DEFAULT NULL COMMENT '图片地址',
  `audio_url` varchar(1024) DEFAULT NULL COMMENT '音频地址',
  `video_url` varchar(1024) DEFAULT NULL COMMENT '视频地址',
  `status` int NOT NULL COMMENT '状态',
  `generate_mode` int DEFAULT NULL COMMENT '生成模式',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述词',
  `platform` varchar(64) DEFAULT NULL COMMENT '平台',
  `model` varchar(64) DEFAULT NULL COMMENT '模型',
  `tags` varchar(1024) DEFAULT NULL COMMENT '风格标签',
  `duration` double DEFAULT NULL COMMENT '时长',
  `public_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否公开',
  `task_id` varchar(128) DEFAULT NULL COMMENT '任务编号',
  `error_message` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 音乐表';

CREATE TABLE IF NOT EXISTS `ai_write` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `type` int NOT NULL COMMENT '写作类型',
  `platform` varchar(64) DEFAULT NULL COMMENT '平台',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `model` varchar(64) DEFAULT NULL COMMENT '模型',
  `prompt` text COMMENT '生成内容提示',
  `generated_content` mediumtext COMMENT '生成的内容',
  `original_content` mediumtext COMMENT '原文',
  `length` int DEFAULT NULL COMMENT '长度提示',
  `format` int DEFAULT NULL COMMENT '格式提示',
  `tone` int DEFAULT NULL COMMENT '语气提示',
  `language` int DEFAULT NULL COMMENT '语言提示',
  `error_message` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 写作表';

CREATE TABLE IF NOT EXISTS `ai_mind_map` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `platform` varchar(64) DEFAULT NULL COMMENT '平台',
  `model_id` bigint DEFAULT NULL COMMENT '模型编号',
  `model` varchar(64) DEFAULT NULL COMMENT '模型',
  `prompt` text COMMENT '生成内容提示',
  `generated_content` mediumtext COMMENT '生成的内容',
  `error_message` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 思维导图表';

CREATE TABLE IF NOT EXISTS `ai_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(255) NOT NULL COMMENT '知识库名称',
  `description` varchar(1024) DEFAULT NULL COMMENT '知识库描述',
  `embedding_model_id` bigint DEFAULT NULL COMMENT '向量模型编号',
  `embedding_model` varchar(64) DEFAULT NULL COMMENT '模型标识',
  `top_k` int DEFAULT NULL COMMENT 'topK',
  `similarity_threshold` double DEFAULT NULL COMMENT '相似度阈值',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库表';

CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `knowledge_id` bigint NOT NULL COMMENT '知识库编号',
  `name` varchar(255) NOT NULL COMMENT '文档名称',
  `url` varchar(1024) DEFAULT NULL COMMENT '文件 URL',
  `content` mediumtext COMMENT '内容',
  `content_length` int DEFAULT NULL COMMENT '文档长度',
  `tokens` int DEFAULT NULL COMMENT 'token 数量',
  `segment_max_tokens` int DEFAULT NULL COMMENT '分片最大 Token 数',
  `retrieval_count` int NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库文档表';

CREATE TABLE IF NOT EXISTS `ai_knowledge_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `knowledge_id` bigint NOT NULL COMMENT '知识库编号',
  `document_id` bigint NOT NULL COMMENT '文档编号',
  `content` text COMMENT '切片内容',
  `content_length` int DEFAULT NULL COMMENT '切片内容长度',
  `vector_id` varchar(128) DEFAULT NULL COMMENT '向量库编号',
  `tokens` int DEFAULT NULL COMMENT 'token 数量',
  `retrieval_count` int NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 知识库分段表';

CREATE TABLE IF NOT EXISTS `ai_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NOT NULL COMMENT '工具名称',
  `description` varchar(1024) DEFAULT NULL COMMENT '工具描述',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 工具表';

CREATE TABLE IF NOT EXISTS `ai_workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NOT NULL COMMENT '工作流名称',
  `code` varchar(64) NOT NULL COMMENT '工作流标识',
  `graph` mediumtext COMMENT '工作流模型 JSON',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 工作流表';

SET FOREIGN_KEY_CHECKS = 1;