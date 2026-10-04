package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store.*;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.*;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.*;
import cn.iocoder.yudao.module.fms.framework.xq.XqSecretCrypto;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class XqStorePlatformServiceImpl implements XqStorePlatformService {

    @Resource
    private XqPlatformMapper platformMapper;
    @Resource
    private XqStoreMapper storeMapper;
    @Resource
    private XqPlatformAliasMapper aliasMapper;
    @Resource
    private XqUserStoreMapper userStoreMapper;
    @Resource
    private XqListingPlatformMapper listingPlatformMapper;
    @Resource
    private XqSysStoreMapper sysStoreMapper;
    @Resource
    private XqLegacyUserMapper legacyUserMapper;
    @Resource
    private XqSysUserGroupMapper sysUserGroupMapper;
    @Resource
    private XqLegacyRoleMapper legacyRoleMapper;
    @Resource
    private XqLegacyUserRoleMapper legacyUserRoleMapper;
    @Resource
    private XqWarehouseMapper warehouseMapper;
    @Resource
    private XqStoreWarehouseMapper storeWarehouseMapper;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private ConfigApi configApi;
    @Resource
    private XqSecretCrypto secretCrypto;

    private static final String USER_INIT_PASSWORD_KEY = "system.user.init-password";
    private static final String DEFAULT_INIT_PASSWORD = "xq123456";
    /** 导入用户不强制部门，避免部门 ID 不存在导致插入失败 */
    private static final Long DEFAULT_IMPORT_DEPT_ID = null;

    @Override
    public List<XqPlatformRespVO> listPlatforms() {
        List<XqPlatformDO> rows = platformMapper.selectListAll();
        List<XqPlatformRespVO> out = new ArrayList<>();
        for (XqPlatformDO row : rows) {
            XqPlatformRespVO vo = BeanUtils.toBean(row, XqPlatformRespVO.class);
            vo.setStoreCount((int) storeMapper.countByPlatformId(row.getId()));
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlatform(XqPlatformSaveReqVO reqVO) {
        String code = normalizeCode(reqVO.getCode());
        XqPlatformDO same = platformMapper.selectByCode(code);
        if (same != null && (reqVO.getId() == null || !same.getId().equals(reqVO.getId()))) {
            throw exception(XQ_PLATFORM_CODE_DUPLICATE);
        }
        if (reqVO.getId() == null) {
            XqPlatformDO row = XqPlatformDO.builder()
                    .code(code)
                    .name(reqVO.getName().trim())
                    .sourcePlatformId(trimOrNull(reqVO.getSourcePlatformId()))
                    .sort(reqVO.getSort() == null ? 0 : reqVO.getSort())
                    .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                    .remark(trimOrNull(reqVO.getRemark()))
                    .build();
            platformMapper.insert(row);
            return row.getId();
        }
        XqPlatformDO existing = requirePlatform(reqVO.getId());
        XqPlatformDO update = new XqPlatformDO();
        update.setId(existing.getId());
        update.setCode(code);
        update.setName(reqVO.getName().trim());
        update.setSourcePlatformId(trimOrNull(reqVO.getSourcePlatformId()));
        update.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(trimOrNull(reqVO.getRemark()));
        platformMapper.updateById(update);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatform(Long id) {
        requirePlatform(id);
        if (storeMapper.countByPlatformId(id) > 0) {
            throw exception(XQ_PLATFORM_HAS_STORE);
        }
        for (XqPlatformAliasDO alias : aliasMapper.selectByPlatformId(id)) {
            aliasMapper.deleteById(alias.getId());
        }
        platformMapper.deleteById(id);
    }

    @Override
    public List<XqStoreRespVO> listStores(Long platformId) {
        List<XqStoreDO> rows = platformId == null
                ? storeMapper.selectListAll()
                : storeMapper.selectByPlatformId(platformId);
        Map<Long, XqPlatformDO> platforms = platformMapper.selectListAll().stream()
                .collect(Collectors.toMap(XqPlatformDO::getId, p -> p, (a, b) -> a));
        List<XqStoreRespVO> out = new ArrayList<>();
        for (XqStoreDO row : rows) {
            out.add(toStoreVo(row, platforms.get(row.getPlatformId())));
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStore(XqStoreSaveReqVO reqVO) {
        requirePlatform(reqVO.getPlatformId());
        String password = StrUtil.trim(reqVO.getPassword());
        if (reqVO.getId() == null) {
            XqStoreDO row = XqStoreDO.builder()
                    .name(reqVO.getName().trim())
                    .platformId(reqVO.getPlatformId())
                    .sourceStoreId(reqVO.getSourceStoreId())
                    .account(trimOrNull(reqVO.getAccount()))
                    .imageUrl(trimOrNull(reqVO.getImageUrl()))
                    .status(reqVO.getStatus() == null ? 1 : reqVO.getStatus())
                    .deptId(trimOrNull(reqVO.getDeptId()))
                    .remark(trimOrNull(reqVO.getRemark()))
                    .build();
            if (StrUtil.isNotBlank(password)) {
                row.setPasswordEnc(secretCrypto.encrypt(password));
                row.setPasswordMask(XqSecretCrypto.mask(password));
            }
            storeMapper.insert(row);
            return row.getId();
        }
        XqStoreDO existing = requireStore(reqVO.getId());
        XqStoreDO update = new XqStoreDO();
        update.setId(existing.getId());
        update.setName(reqVO.getName().trim());
        update.setPlatformId(reqVO.getPlatformId());
        update.setSourceStoreId(reqVO.getSourceStoreId());
        update.setAccount(trimOrNull(reqVO.getAccount()));
        if (StrUtil.isNotBlank(password)) {
            update.setPasswordEnc(secretCrypto.encrypt(password));
            update.setPasswordMask(XqSecretCrypto.mask(password));
        }
        update.setImageUrl(trimOrNull(reqVO.getImageUrl()));
        update.setStatus(reqVO.getStatus() == null ? 1 : reqVO.getStatus());
        update.setDeptId(trimOrNull(reqVO.getDeptId()));
        update.setRemark(trimOrNull(reqVO.getRemark()));
        storeMapper.updateById(update);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStore(Long id) {
        requireStore(id);
        storeMapper.deleteById(id);
    }

    @Override
    public List<XqPlatformAliasRespVO> listAliases() {
        Map<Long, XqPlatformDO> platforms = platformMapper.selectListAll().stream()
                .collect(Collectors.toMap(XqPlatformDO::getId, p -> p, (a, b) -> a));
        List<XqPlatformAliasRespVO> out = new ArrayList<>();
        for (XqPlatformAliasDO row : aliasMapper.selectListAll()) {
            XqPlatformAliasRespVO vo = new XqPlatformAliasRespVO();
            vo.setId(row.getId());
            vo.setPlatformId(row.getPlatformId());
            vo.setAlias(row.getAlias());
            XqPlatformDO p = platforms.get(row.getPlatformId());
            if (p != null) {
                vo.setPlatformCode(p.getCode());
                vo.setPlatformName(p.getName());
            }
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAlias(XqPlatformAliasSaveReqVO reqVO) {
        requirePlatform(reqVO.getPlatformId());
        String alias = normalizeAlias(reqVO.getAlias());
        XqPlatformAliasDO same = aliasMapper.selectByAlias(alias);
        if (same != null && (reqVO.getId() == null || !same.getId().equals(reqVO.getId()))) {
            throw exception(XQ_PLATFORM_ALIAS_DUPLICATE);
        }
        if (reqVO.getId() == null) {
            XqPlatformAliasDO row = XqPlatformAliasDO.builder()
                    .platformId(reqVO.getPlatformId())
                    .alias(alias)
                    .build();
            aliasMapper.insert(row);
            return row.getId();
        }
        XqPlatformAliasDO existing = aliasMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(XQ_PLATFORM_ALIAS_NOT_EXISTS);
        }
        XqPlatformAliasDO update = new XqPlatformAliasDO();
        update.setId(existing.getId());
        update.setPlatformId(reqVO.getPlatformId());
        update.setAlias(alias);
        aliasMapper.updateById(update);
        return existing.getId();
    }

    @Override
    public void deleteAlias(Long id) {
        if (aliasMapper.selectById(id) == null) {
            throw exception(XQ_PLATFORM_ALIAS_NOT_EXISTS);
        }
        aliasMapper.deleteById(id);
    }

    @Override
    public XqUserStoreRespVO getUserStores(Long userId) {
        XqUserStoreRespVO vo = new XqUserStoreRespVO();
        vo.setUserId(userId);
        List<XqUserStoreDO> rows = userStoreMapper.selectByUserId(userId);
        List<Long> ids = rows.stream().map(XqUserStoreDO::getStoreId).collect(Collectors.toList());
        vo.setStoreIds(ids);
        vo.setRestricted(!ids.isEmpty());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindUserStores(XqUserStoreBindReqVO reqVO) {
        userStoreMapper.deleteByUserId(reqVO.getUserId());
        if (CollUtil.isEmpty(reqVO.getStoreIds())) {
            return;
        }
        for (Long storeId : new LinkedHashSet<>(reqVO.getStoreIds())) {
            if (storeId == null) {
                continue;
            }
            requireStore(storeId);
            userStoreMapper.insert(XqUserStoreDO.builder()
                    .userId(reqVO.getUserId())
                    .storeId(storeId)
                    .build());
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public XqStoreSyncRespVO syncFromLegacy() {
        XqStoreSyncRespVO resp = new XqStoreSyncRespVO();
        int platformUpserted = 0;
        int storeUpserted = 0;
        int aliasBound = 0;

        List<XqListingPlatformDO> legacyPlatforms;
        try {
            legacyPlatforms = listingPlatformMapper.selectAllList();
        } catch (Exception ex) {
            log.warn("[syncFromLegacy] 读原库平台失败: {}", ex.getMessage());
            legacyPlatforms = List.of();
        }
        for (XqListingPlatformDO lp : legacyPlatforms) {
            if (lp == null || StrUtil.isBlank(lp.getCode())) {
                continue;
            }
            String code = normalizeCode(lp.getCode());
            XqPlatformDO existing = platformMapper.selectByCode(code);
            if (existing == null) {
                platformMapper.insert(XqPlatformDO.builder()
                        .code(code)
                        .name(StrUtil.blankToDefault(lp.getName(), code))
                        .sourcePlatformId(lp.getId())
                        .sort(lp.getSortOrder() == null ? 0 : lp.getSortOrder())
                        .enabled(lp.getEnabled() == null || lp.getEnabled())
                        .remark(lp.getRemark())
                        .build());
            } else {
                XqPlatformDO update = new XqPlatformDO();
                update.setId(existing.getId());
                update.setName(StrUtil.blankToDefault(lp.getName(), existing.getName()));
                update.setSourcePlatformId(lp.getId());
                update.setSort(lp.getSortOrder() == null ? existing.getSort() : lp.getSortOrder());
                update.setEnabled(lp.getEnabled() == null || lp.getEnabled());
                platformMapper.updateById(update);
            }
            platformUpserted++;
        }

        Map<String, XqPlatformDO> byCode = platformMapper.selectListAll().stream()
                .collect(Collectors.toMap(p -> p.getCode().toLowerCase(Locale.ROOT), p -> p, (a, b) -> a));
        Map<String, Long> aliasToPlatform = new HashMap<>();
        for (XqPlatformAliasDO a : aliasMapper.selectListAll()) {
            aliasToPlatform.put(a.getAlias().toLowerCase(Locale.ROOT), a.getPlatformId());
        }

        List<XqSysStoreDO> legacyStores;
        try {
            legacyStores = sysStoreMapper.selectAllActive();
        } catch (Exception ex) {
            log.warn("[syncFromLegacy] 读原库店铺失败: {}", ex.getMessage());
            legacyStores = List.of();
        }
        for (XqSysStoreDO ss : legacyStores) {
            Long platformId = resolvePlatformId(ss.getPlatform(), byCode, aliasToPlatform);
            if (platformId == null) {
                // 未知平台：按别名新建占位平台
                String raw = normalizeAlias(ss.getPlatform());
                if (StrUtil.isBlank(raw)) {
                    continue;
                }
                String code = raw.replaceAll("[^a-z0-9_]+", "_");
                XqPlatformDO created = platformMapper.selectByCode(code);
                if (created == null) {
                    created = XqPlatformDO.builder()
                            .code(code)
                            .name(StrUtil.blankToDefault(ss.getPlatform(), code))
                            .sort(99)
                            .enabled(true)
                            .remark("sync from sys_store.platform")
                            .build();
                    platformMapper.insert(created);
                    platformUpserted++;
                    byCode.put(code, created);
                }
                platformId = created.getId();
                if (aliasMapper.selectByAlias(raw) == null) {
                    aliasMapper.insert(XqPlatformAliasDO.builder()
                            .platformId(platformId)
                            .alias(raw)
                            .build());
                    aliasBound++;
                    aliasToPlatform.put(raw, platformId);
                }
            }
            XqStoreDO existing = storeMapper.selectBySourceStoreId(ss.getId());
            String pwd = StrUtil.trim(ss.getPassword());
            if (existing == null) {
                XqStoreDO row = XqStoreDO.builder()
                        .name(StrUtil.blankToDefault(ss.getName(), String.valueOf(ss.getId())))
                        .platformId(platformId)
                        .sourceStoreId(ss.getId())
                        .account(trimOrNull(ss.getAccount()))
                        .imageUrl(trimOrNull(ss.getImageUrl()))
                        .status(ss.getStatus() == null ? 1 : ss.getStatus())
                        .deptId(trimOrNull(ss.getDeptId()))
                        .remark(trimOrNull(ss.getRemark()))
                        .build();
                if (StrUtil.isNotBlank(pwd)) {
                    row.setPasswordEnc(secretCrypto.encrypt(pwd));
                    row.setPasswordMask(XqSecretCrypto.mask(pwd));
                }
                storeMapper.insert(row);
            } else {
                XqStoreDO update = new XqStoreDO();
                update.setId(existing.getId());
                update.setName(StrUtil.blankToDefault(ss.getName(), existing.getName()));
                update.setPlatformId(platformId);
                update.setSourceStoreId(ss.getId());
                update.setAccount(trimOrNull(ss.getAccount()));
                if (StrUtil.isNotBlank(pwd)) {
                    update.setPasswordEnc(secretCrypto.encrypt(pwd));
                    update.setPasswordMask(XqSecretCrypto.mask(pwd));
                }
                update.setImageUrl(trimOrNull(ss.getImageUrl()));
                update.setStatus(ss.getStatus() == null ? 1 : ss.getStatus());
                update.setDeptId(trimOrNull(ss.getDeptId()));
                update.setRemark(trimOrNull(ss.getRemark()));
                storeMapper.updateById(update);
            }
            storeUpserted++;
        }

        // 用户 + 角色 + 绑店：写入芋道 system_users / system_user_role / xq_user_store
        syncLegacyUsersRolesAndStores(resp);

        resp.setPlatformUpserted(platformUpserted);
        resp.setStoreUpserted(storeUpserted);
        resp.setAliasBound(aliasBound);
        return resp;
    }

    @Override
    public List<XqWarehouseRespVO> listWarehouses() {
        return BeanUtils.toBean(warehouseMapper.selectListAll(), XqWarehouseRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveWarehouse(XqWarehouseSaveReqVO reqVO) {
        String code = normalizeCode(reqVO.getCode());
        XqWarehouseDO same = warehouseMapper.selectByCode(code);
        if (same != null && (reqVO.getId() == null || !same.getId().equals(reqVO.getId()))) {
            throw exception(XQ_WAREHOUSE_CODE_DUPLICATE);
        }
        if (reqVO.getId() == null) {
            XqWarehouseDO row = XqWarehouseDO.builder()
                    .code(code)
                    .name(reqVO.getName().trim())
                    .countryCode(trimOrNull(reqVO.getCountryCode()))
                    .countryName(trimOrNull(reqVO.getCountryName()))
                    .sort(reqVO.getSort() == null ? 0 : reqVO.getSort())
                    .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                    .remark(trimOrNull(reqVO.getRemark()))
                    .build();
            warehouseMapper.insert(row);
            return row.getId();
        }
        if (warehouseMapper.selectById(reqVO.getId()) == null) {
            throw exception(XQ_WAREHOUSE_NOT_EXISTS);
        }
        XqWarehouseDO update = new XqWarehouseDO();
        update.setId(reqVO.getId());
        update.setCode(code);
        update.setName(reqVO.getName().trim());
        update.setCountryCode(trimOrNull(reqVO.getCountryCode()));
        update.setCountryName(trimOrNull(reqVO.getCountryName()));
        update.setSort(reqVO.getSort() == null ? 0 : reqVO.getSort());
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(trimOrNull(reqVO.getRemark()));
        warehouseMapper.updateById(update);
        return reqVO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouse(Long id) {
        if (warehouseMapper.selectById(id) == null) {
            throw exception(XQ_WAREHOUSE_NOT_EXISTS);
        }
        warehouseMapper.deleteById(id);
    }

    @Override
    public List<XqStoreWarehouseRespVO> listStoreWarehouses(Long storeId) {
        List<XqStoreWarehouseDO> rows = storeId == null
                ? storeWarehouseMapper.selectListAll()
                : storeWarehouseMapper.selectByStoreId(storeId);
        Map<Long, XqStoreDO> stores = storeMapper.selectListAll().stream()
                .collect(Collectors.toMap(XqStoreDO::getId, s -> s, (a, b) -> a));
        Map<Long, XqPlatformDO> platforms = platformMapper.selectListAll().stream()
                .collect(Collectors.toMap(XqPlatformDO::getId, p -> p, (a, b) -> a));
        Map<Long, XqWarehouseDO> warehouses = warehouseMapper.selectListAll().stream()
                .collect(Collectors.toMap(XqWarehouseDO::getId, w -> w, (a, b) -> a));
        List<XqStoreWarehouseRespVO> out = new ArrayList<>();
        for (XqStoreWarehouseDO row : rows) {
            XqStoreWarehouseRespVO vo = BeanUtils.toBean(row, XqStoreWarehouseRespVO.class);
            XqStoreDO store = stores.get(row.getStoreId());
            if (store != null) {
                vo.setStoreName(store.getName());
                XqPlatformDO p = platforms.get(store.getPlatformId());
                if (p != null) {
                    vo.setPlatformCode(p.getCode());
                    vo.setPlatformName(p.getName());
                }
            }
            XqWarehouseDO wh = warehouses.get(row.getWarehouseId());
            if (wh != null) {
                vo.setWarehouseCode(wh.getCode());
                vo.setWarehouseName(wh.getName());
            }
            out.add(vo);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStoreWarehouse(XqStoreWarehouseSaveReqVO reqVO) {
        requireStore(reqVO.getStoreId());
        if (reqVO.getWarehouseId() != null && warehouseMapper.selectById(reqVO.getWarehouseId()) == null) {
            throw exception(XQ_WAREHOUSE_NOT_EXISTS);
        }
        if (reqVO.getId() == null) {
            XqStoreWarehouseDO row = XqStoreWarehouseDO.builder()
                    .storeId(reqVO.getStoreId())
                    .warehouseId(reqVO.getWarehouseId())
                    .physicalCode(trimOrNull(reqVO.getPhysicalCode()))
                    .physicalName(trimOrNull(reqVO.getPhysicalName()))
                    .countryCode(StrUtil.blankToDefault(trimOrNull(reqVO.getCountryCode()), "US"))
                    .priority(reqVO.getPriority() == null ? 1 : reqVO.getPriority())
                    .shippingMethod(trimOrNull(reqVO.getShippingMethod()))
                    .courierAccount(trimOrNull(reqVO.getCourierAccount()))
                    .enabled(reqVO.getEnabled() == null || reqVO.getEnabled())
                    .remark(trimOrNull(reqVO.getRemark()))
                    .build();
            storeWarehouseMapper.insert(row);
            return row.getId();
        }
        if (storeWarehouseMapper.selectById(reqVO.getId()) == null) {
            throw exception(XQ_STORE_WAREHOUSE_NOT_EXISTS);
        }
        XqStoreWarehouseDO update = new XqStoreWarehouseDO();
        update.setId(reqVO.getId());
        update.setStoreId(reqVO.getStoreId());
        update.setWarehouseId(reqVO.getWarehouseId());
        update.setPhysicalCode(trimOrNull(reqVO.getPhysicalCode()));
        update.setPhysicalName(trimOrNull(reqVO.getPhysicalName()));
        update.setCountryCode(StrUtil.blankToDefault(trimOrNull(reqVO.getCountryCode()), "US"));
        update.setPriority(reqVO.getPriority() == null ? 1 : reqVO.getPriority());
        update.setShippingMethod(trimOrNull(reqVO.getShippingMethod()));
        update.setCourierAccount(trimOrNull(reqVO.getCourierAccount()));
        update.setEnabled(reqVO.getEnabled() == null || reqVO.getEnabled());
        update.setRemark(trimOrNull(reqVO.getRemark()));
        storeWarehouseMapper.updateById(update);
        return reqVO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStoreWarehouse(Long id) {
        if (storeWarehouseMapper.selectById(id) == null) {
            throw exception(XQ_STORE_WAREHOUSE_NOT_EXISTS);
        }
        storeWarehouseMapper.deleteById(id);
    }

    /**
     * 把原库用户/权限迁进主业务表（不是第二套账号）：
     * - t_user → system_users（无则创建，有则复用；默认初始密码）
     * - t_role.scope → 芋道角色 code（xq_copy / xq_image…）并 assignUserRole 合并
     * - t_sys_user_group → xq_user_store（店铺范围）
     */
    private void syncLegacyUsersRolesAndStores(XqStoreSyncRespVO resp) {
        List<XqLegacyUserDO> legacyUsers;
        List<XqSysUserGroupDO> groups;
        List<XqLegacyRoleDO> legacyRoles;
        List<XqLegacyUserRoleDO> legacyUserRoles;
        try {
            legacyUsers = legacyUserMapper.selectActiveList();
            groups = sysUserGroupMapper.selectAll();
            legacyRoles = legacyRoleMapper.selectAll();
            legacyUserRoles = legacyUserRoleMapper.selectAll();
        } catch (Exception ex) {
            log.warn("[syncFromLegacy] 读原库用户/角色/绑店失败: {}", ex.getMessage());
            return;
        }
        if (CollUtil.isEmpty(legacyUsers)) {
            return;
        }

        Map<String, XqLegacyRoleDO> roleById = legacyRoles.stream()
                .filter(r -> r != null && StrUtil.isNotBlank(r.getId()))
                .collect(Collectors.toMap(r -> r.getId().trim(), r -> r, (a, b) -> a));
        Map<String, Set<String>> legacyUserScopes = new HashMap<>();
        for (XqLegacyUserRoleDO ur : CollUtil.emptyIfNull(legacyUserRoles)) {
            if (ur == null || StrUtil.isBlank(ur.getUserId()) || StrUtil.isBlank(ur.getRoleId())) {
                continue;
            }
            XqLegacyRoleDO role = roleById.get(ur.getRoleId().trim());
            if (role == null) {
                continue;
            }
            // scope 优先；美工等 scope 为空时用 type（mg / cs / phyy）
            String key = StrUtil.blankToDefault(role.getScope(), role.getType());
            if (StrUtil.isBlank(key)) {
                continue;
            }
            legacyUserScopes
                    .computeIfAbsent(ur.getUserId().trim(), k -> new LinkedHashSet<>())
                    .add(key.trim().toLowerCase(Locale.ROOT));
        }
        Map<String, Set<Integer>> legacyUserStores = new HashMap<>();
        for (XqSysUserGroupDO g : CollUtil.emptyIfNull(groups)) {
            if (g == null || StrUtil.isBlank(g.getUserId()) || g.getGroupId() == null) {
                continue;
            }
            legacyUserStores
                    .computeIfAbsent(g.getUserId().trim(), k -> new LinkedHashSet<>())
                    .add(g.getGroupId());
        }
        Map<Integer, Long> sourceStoreToXqId = storeMapper.selectListAll().stream()
                .filter(s -> s.getSourceStoreId() != null)
                .collect(Collectors.toMap(XqStoreDO::getSourceStoreId, XqStoreDO::getId, (a, b) -> a));
        Map<String, Long> roleCodeToId = roleMapper.selectList().stream()
                .filter(r -> StrUtil.isNotBlank(r.getCode()))
                .collect(Collectors.toMap(
                        r -> r.getCode().trim().toLowerCase(Locale.ROOT),
                        RoleDO::getId,
                        (a, b) -> a));
        // 补齐财务/采购等业务角色（当前租户）
        ensureXqBusinessRoles(roleCodeToId);

        String initPassword = StrUtil.blankToDefault(
                configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY), DEFAULT_INIT_PASSWORD);
        String encodedPassword = passwordEncoder.encode(initPassword);

        int created = 0;
        int matched = 0;
        int roleAssigned = 0;
        int bound = 0;
        Set<String> failed = new LinkedHashSet<>();

        for (XqLegacyUserDO legacy : legacyUsers) {
            if (legacy == null || StrUtil.isBlank(legacy.getAccount())) {
                continue;
            }
            String account = legacy.getAccount().trim();
            String legacyUserId = StrUtil.trim(legacy.getId());
            try {
                AdminUserDO user = adminUserService.getUserByUsername(account);
                if (user == null) {
                    user = AdminUserDO.builder()
                            .username(account)
                            .nickname(account)
                            .password(encodedPassword)
                            .deptId(DEFAULT_IMPORT_DEPT_ID)
                            .postIds(new HashSet<>())
                            .status(Boolean.TRUE.equals(legacy.getDisable())
                                    ? CommonStatusEnum.DISABLE.getStatus()
                                    : CommonStatusEnum.ENABLE.getStatus())
                            .remark("sync from legacy t_user")
                            .build();
                    adminUserMapper.insert(user);
                    created++;
                } else {
                    matched++;
                }

                // 角色：按原库 scope 映射到芋道角色，合并已有角色后写回
                Set<Long> mappedRoleIds = mapScopesToRoleIds(
                        legacyUserScopes.getOrDefault(legacyUserId, Set.of()),
                        legacyUserStores.containsKey(legacyUserId),
                        roleCodeToId);
                if (!mappedRoleIds.isEmpty()) {
                    Set<Long> merged = new LinkedHashSet<>(
                            permissionService.getUserRoleIdListByUserId(user.getId()));
                    int before = merged.size();
                    merged.addAll(mappedRoleIds);
                    if (merged.size() > before) {
                        permissionService.assignUserRole(user.getId(), merged);
                        roleAssigned += (merged.size() - before);
                    }
                }

                // 绑店
                Set<Integer> sourceStoreIds = legacyUserStores.get(legacyUserId);
                if (CollUtil.isNotEmpty(sourceStoreIds)) {
                    Set<Long> xqStoreIds = new LinkedHashSet<>();
                    for (Integer sourceStoreId : sourceStoreIds) {
                        Long xqStoreId = sourceStoreToXqId.get(sourceStoreId);
                        if (xqStoreId != null) {
                            xqStoreIds.add(xqStoreId);
                        }
                    }
                    if (!xqStoreIds.isEmpty()) {
                        userStoreMapper.deleteByUserId(user.getId());
                        for (Long storeId : xqStoreIds) {
                            userStoreMapper.insert(XqUserStoreDO.builder()
                                    .userId(user.getId())
                                    .storeId(storeId)
                                    .build());
                            bound++;
                        }
                    }
                }
            } catch (Exception ex) {
                failed.add(account + "(" + ex.getMessage() + ")");
                log.warn("[syncFromLegacy] 同步用户 {} 失败: {}", account, ex.getMessage());
            }
        }

        resp.setUserCreated(created);
        resp.setUserMatched(matched);
        resp.setRoleAssigned(roleAssigned);
        resp.setUserStoreBound(bound);
        resp.setUnmatchedAccounts(new ArrayList<>(failed).stream().limit(50).collect(Collectors.toList()));
        log.info("[syncFromLegacy] 用户创建={} 复用={} 角色新增={} 绑店={} 失败={}",
                created, matched, roleAssigned, bound, failed.size());
    }

    /** 当前租户缺少财务/采购角色时补建 */
    private void ensureXqBusinessRoles(Map<String, Long> roleCodeToId) {
        ensureRole(roleCodeToId, "xq_finance", "财务", 23, "原库财务岗");
        ensureRole(roleCodeToId, "xq_purchase", "采购", 24, "原库采购岗");
        ensureRole(roleCodeToId, "xq_copy", "文案人员", 20, "工作台文案");
        ensureRole(roleCodeToId, "xq_image", "图片人员", 21, "工作台图片");
        ensureRole(roleCodeToId, "xq_store_admin", "店铺管理员", 22, "店铺平台维护");
    }

    private void ensureRole(Map<String, Long> roleCodeToId, String code, String name, int sort, String remark) {
        String key = code.toLowerCase(Locale.ROOT);
        if (roleCodeToId.containsKey(key)) {
            return;
        }
        RoleDO role = new RoleDO();
        role.setName(name);
        role.setCode(code);
        role.setSort(sort);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        role.setType(2); // 自定义
        role.setDataScope(1); // 全部数据权限
        role.setDataScopeDeptIds(Collections.emptySet());
        role.setRemark(remark);
        roleMapper.insert(role);
        roleCodeToId.put(key, role.getId());
        log.info("[syncFromLegacy] 补建角色 {} -> id={}", code, role.getId());
    }

    /** 原库 scope/type → 芋道角色 code；有绑店无角色时默认给文案岗 */
    private static Set<Long> mapScopesToRoleIds(Set<String> scopes,
                                                boolean hasStoreBind,
                                                Map<String, Long> roleCodeToId) {
        Set<String> codes = new LinkedHashSet<>();
        for (String scope : CollUtil.emptyIfNull(scopes)) {
            switch (scope) {
                case "operator", "yyzz", "phyy", "shaoshanbu", "xiongxiongbu" -> codes.add("xq_copy");
                case "leader" -> {
                    codes.add("xq_copy");
                    codes.add("xq_image");
                }
                case "admin" -> {
                    codes.add("xq_copy");
                    codes.add("xq_image");
                    codes.add("xq_store_admin");
                }
                case "finance" -> codes.add("xq_finance");
                case "purchase" -> codes.add("xq_purchase");
                case "mg" -> codes.add("xq_image"); // 美工
                case "cs" -> {
                    // 测试账号不自动赋业务角色
                }
                default -> codes.add("xq_copy");
            }
        }
        if (codes.isEmpty() && hasStoreBind) {
            codes.add("xq_copy");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String code : codes) {
            Long id = roleCodeToId.get(code.toLowerCase(Locale.ROOT));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Long resolvePlatformId(String storePlatform,
                                   Map<String, XqPlatformDO> byCode,
                                   Map<String, Long> aliasToPlatform) {
        String alias = normalizeAlias(storePlatform);
        if (StrUtil.isBlank(alias)) {
            return null;
        }
        Long byAlias = aliasToPlatform.get(alias);
        if (byAlias != null) {
            return byAlias;
        }
        XqPlatformDO byCodeHit = byCode.get(alias);
        if (byCodeHit != null) {
            return byCodeHit.getId();
        }
        return null;
    }

    private XqStoreRespVO toStoreVo(XqStoreDO row, XqPlatformDO platform) {
        XqStoreRespVO vo = BeanUtils.toBean(row, XqStoreRespVO.class);
        vo.setHasPassword(StrUtil.isNotBlank(row.getPasswordEnc()));
        vo.setPasswordMask(row.getPasswordMask());
        if (platform != null) {
            vo.setPlatformCode(platform.getCode());
            vo.setPlatformName(platform.getName());
            vo.setSourcePlatformId(platform.getSourcePlatformId());
        }
        return vo;
    }

    private XqPlatformDO requirePlatform(Long id) {
        XqPlatformDO row = platformMapper.selectById(id);
        if (row == null) {
            throw exception(XQ_PLATFORM_NOT_EXISTS);
        }
        return row;
    }

    private XqStoreDO requireStore(Long id) {
        XqStoreDO row = storeMapper.selectById(id);
        if (row == null) {
            throw exception(XQ_STORE_NOT_EXISTS);
        }
        return row;
    }

    private static String normalizeCode(String code) {
        return StrUtil.blankToDefault(code, "").trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\s'\"-]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String normalizeAlias(String alias) {
        return normalizeCode(alias);
    }

    private static String trimOrNull(String s) {
        String t = StrUtil.trim(s);
        return StrUtil.isBlank(t) ? null : t;
    }

}
