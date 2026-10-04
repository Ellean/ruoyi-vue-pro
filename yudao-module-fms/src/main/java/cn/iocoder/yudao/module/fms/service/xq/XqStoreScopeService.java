package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqUserStoreDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqStoreMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqUserStoreMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 店铺/平台业务范围：只认主库 xq_platform / xq_store / xq_user_store。
 * source_* 仅作对接原库分类/规则的外键，不参与权限判定。
 */
@Service
public class XqStoreScopeService {

    @Resource
    private XqPlatformMapper platformMapper;
    @Resource
    private XqStoreMapper storeMapper;
    @Resource
    private XqUserStoreMapper userStoreMapper;

    /**
     * @return null = 不限店；非 null = 白名单（可为空集合=无可见店铺）
     */
    public Set<Long> getAllowedStoreIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<XqUserStoreDO> rows = userStoreMapper.selectByUserId(userId);
        if (CollUtil.isEmpty(rows)) {
            return null;
        }
        return rows.stream()
                .map(XqUserStoreDO::getStoreId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isStoreAllowed(Long userId, Long storeId) {
        if (storeId == null) {
            return false;
        }
        Set<Long> allowed = getAllowedStoreIds(userId);
        return allowed == null || allowed.contains(storeId);
    }

    public boolean isStoreAllowed(Long userId, String storeIdText) {
        Long storeId = parseLong(storeIdText);
        return storeId != null && isStoreAllowed(userId, storeId);
    }

    public XqPlatformDO getPlatform(Long id) {
        return id == null ? null : platformMapper.selectById(id);
    }

    public XqStoreDO getStore(Long id) {
        return id == null ? null : storeMapper.selectById(id);
    }

    /**
     * 将业务平台 ID（xq_platform.id）解析为原库分类/规则用的 sourcePlatformId。
     * 若传入本身已是 source UUID，则原样返回（兼容历史工单）。
     */
    public String resolveSourcePlatformId(String platformId) {
        if (StrUtil.isBlank(platformId)) {
            return "";
        }
        String raw = platformId.trim();
        Long xqId = parseLong(raw);
        if (xqId != null) {
            XqPlatformDO p = platformMapper.selectById(xqId);
            if (p != null) {
                return StrUtil.blankToDefault(p.getSourcePlatformId(), "");
            }
        }
        XqPlatformDO bySource = platformMapper.selectBySourcePlatformId(raw);
        if (bySource != null) {
            return StrUtil.blankToDefault(bySource.getSourcePlatformId(), raw);
        }
        return raw;
    }

    public static Long parseLong(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
