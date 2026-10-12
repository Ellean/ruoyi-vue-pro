-- RPA 执行器扩展：支持控制中枢 / 自写脚本 / exe，随时切换
-- 在主库执行（已有 xq_rpa_user_config 表）

ALTER TABLE `xq_rpa_user_config`
  ADD COLUMN `runner_mode` varchar(32) NOT NULL DEFAULT 'commander'
    COMMENT '执行器：commander=影刀控制中枢；script=自写脚本；exe=本地可执行文件' AFTER `password`,
  ADD COLUMN `runner_path` varchar(512) DEFAULT NULL
    COMMENT 'script/exe 路径，如 D:\\bots\\image_runner.exe' AFTER `runner_mode`,
  ADD COLUMN `runner_args` varchar(1024) DEFAULT NULL
    COMMENT '附加参数，可用占位 {jobKind} {workUuid}' AFTER `runner_path`,
  ADD COLUMN `runner_workdir` varchar(512) DEFAULT NULL
    COMMENT '工作目录（可选）' AFTER `runner_args`;
