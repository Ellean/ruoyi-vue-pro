package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store.*;

import java.util.List;

public interface XqStorePlatformService {

    List<XqPlatformRespVO> listPlatforms();

    Long savePlatform(XqPlatformSaveReqVO reqVO);

    void deletePlatform(Long id);

    List<XqStoreRespVO> listStores(Long platformId);

    Long saveStore(XqStoreSaveReqVO reqVO);

    void deleteStore(Long id);

    List<XqPlatformAliasRespVO> listAliases();

    Long saveAlias(XqPlatformAliasSaveReqVO reqVO);

    void deleteAlias(Long id);

    XqUserStoreRespVO getUserStores(Long userId);

    void bindUserStores(XqUserStoreBindReqVO reqVO);

    List<XqWarehouseRespVO> listWarehouses();

    Long saveWarehouse(XqWarehouseSaveReqVO reqVO);

    void deleteWarehouse(Long id);

    List<XqStoreWarehouseRespVO> listStoreWarehouses(Long storeId);

    Long saveStoreWarehouse(XqStoreWarehouseSaveReqVO reqVO);

    void deleteStoreWarehouse(Long id);

    /**
     * 从原库拉取并写入主业务表：
     * 平台/店铺凭据、t_user→system_users、scope→角色、t_sys_user_group→xq_user_store。
     */
    XqStoreSyncRespVO syncFromLegacy();

}
