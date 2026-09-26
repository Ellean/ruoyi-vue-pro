package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingPlatformRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingShopRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqCopyGenRuleDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingShopDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqCopyGenRuleMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingPlatformMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqListingShopMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
public class XqListingCatalogService {

    @Resource
    private XqListingPlatformMapper platformMapper;
    @Resource
    private XqListingShopMapper shopMapper;
    @Resource
    private XqCopyGenRuleMapper copyGenRuleMapper;

    public List<XqListingPlatformRespVO> listPlatforms() {
        return BeanUtils.toBean(platformMapper.selectEnabledList(), XqListingPlatformRespVO.class);
    }

    public List<XqListingShopRespVO> listShops(String platformId) {
        return BeanUtils.toBean(shopMapper.selectByPlatformId(platformId), XqListingShopRespVO.class);
    }

    public List<XqCopyGenRuleRespVO> listCopyRules() {
        List<XqListingPlatformDO> platforms = platformMapper.selectEnabledList();
        Map<String, XqListingPlatformDO> platformMap = platforms.stream()
                .collect(Collectors.toMap(XqListingPlatformDO::getId, p -> p, (a, b) -> a));
        List<XqCopyGenRuleDO> rules = copyGenRuleMapper.selectAll();
        List<XqCopyGenRuleRespVO> result = new ArrayList<>();
        for (XqCopyGenRuleDO rule : rules) {
            XqCopyGenRuleRespVO vo = BeanUtils.toBean(rule, XqCopyGenRuleRespVO.class);
            XqListingPlatformDO p = platformMap.get(StrUtil.blankToDefault(rule.getPlatformId(), ""));
            if (p != null) {
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
            } else if (StrUtil.isBlank(rule.getPlatformId())) {
                vo.setPlatformCode("global");
                vo.setPlatformName("通用规则");
            }
            result.add(vo);
        }
        // 补齐尚无规则的平台，方便前端配置
        for (XqListingPlatformDO p : platforms) {
            boolean exists = rules.stream().anyMatch(r -> p.getId().equals(r.getPlatformId()));
            if (!exists) {
                XqCopyGenRuleRespVO vo = new XqCopyGenRuleRespVO();
                vo.setPlatformId(p.getId());
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
                vo.setCode("deep");
                vo.setName(p.getName() + "文案规则");
                vo.setEnabled(true);
                vo.setConfigJson(defaultConfigJson());
                result.add(vo);
            }
        }
        return result;
    }

    public XqCopyGenRuleRespVO getCopyRule(String platformId) {
        XqCopyGenRuleDO rule = copyGenRuleMapper.selectByPlatformId(platformId);
        if (rule == null) {
            XqCopyGenRuleRespVO vo = new XqCopyGenRuleRespVO();
            vo.setPlatformId(StrUtil.blankToDefault(platformId, ""));
            vo.setCode("deep");
            vo.setName("文案生成规则");
            vo.setEnabled(true);
            vo.setConfigJson(defaultConfigJson());
            return vo;
        }
        return BeanUtils.toBean(rule, XqCopyGenRuleRespVO.class);
    }

    public void saveCopyRule(XqCopyGenRuleSaveReqVO reqVO) {
        String platformId = StrUtil.blankToDefault(reqVO.getPlatformId(), "");
        XqCopyGenRuleDO exists = copyGenRuleMapper.selectByPlatformId(platformId);
        LocalDateTime now = LocalDateTime.now();
        if (exists == null) {
            XqCopyGenRuleDO insert = new XqCopyGenRuleDO();
            insert.setId(IdUtil.fastUUID());
            insert.setPlatformId(platformId);
            insert.setCode("deep");
            insert.setName(reqVO.getName());
            insert.setConfigJson(reqVO.getConfigJson());
            insert.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
            insert.setRemark(reqVO.getRemark());
            insert.setCreateDate(now);
            insert.setUpdateDate(now);
            copyGenRuleMapper.insert(insert);
            return;
        }
        XqCopyGenRuleDO update = new XqCopyGenRuleDO();
        update.setId(exists.getId());
        update.setName(reqVO.getName());
        update.setConfigJson(reqVO.getConfigJson());
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(reqVO.getRemark());
        update.setUpdateDate(now);
        copyGenRuleMapper.updateById(update);
    }

    private static String defaultConfigJson() {
        return "{\"defaultFeatureCount\":5,\"allowedFeatureCounts\":[5,8],"
                + "\"generateTitle\":true,"
                + "\"limits\":{\"titleMaxLen\":200,\"descriptionMaxLen\":2000,\"featureMaxLen\":200}}";
    }

}
