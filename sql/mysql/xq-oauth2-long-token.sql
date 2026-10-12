-- RPA / 工作台长跑：默认 access token 1800 秒（30 分钟）会中途「账号未登录」
-- default 客户端改为 7 天；refresh 保持 30 天
-- 已签发的旧 token 仍按原过期时间，需重新登录后才生效

UPDATE `system_oauth2_client`
SET `access_token_validity_seconds` = 604800
WHERE `client_id` = 'default'
  AND `deleted` = 0;
