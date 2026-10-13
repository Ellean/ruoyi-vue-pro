package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductListMeta;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 读 Giga 选品产品（主库 ruoyi-vue-pro 内 t_gigab2b_*）
 */
@Mapper
public interface XqGigaProductMapper {

    Long selectPageCount(@Param("name") String name,
                         @Param("categoryIds") Collection<Long> categoryIds,
                         @Param("excludeSkus") Collection<String> excludeSkus);

    List<String> selectPageIds(@Param("name") String name,
                               @Param("categoryIds") Collection<Long> categoryIds,
                               @Param("excludeSkus") Collection<String> excludeSkus,
                               @Param("offset") long offset,
                               @Param("limit") long limit);

    List<XqGigaProductRow> selectListByIds(@Param("ids") Collection<String> ids);

    XqGigaProductRow selectById(@Param("id") String id);

    /** 仅列表行，不 JOIN snapshot（详情缺失时仍能解析 SKU） */
    XqGigaProductListMeta selectListMetaById(@Param("id") String id);

    XqGigaProductRow selectBySku(@Param("sku") String sku);

    /** 按需补详情：写入/更新快照（含合并后的自提/代发价） */
    int upsertSnapshot(@Param("id") String id,
                       @Param("userId") String userId,
                       @Param("gigaConfigId") String gigaConfigId,
                       @Param("sku") String sku,
                       @Param("productName") String productName,
                       @Param("skuAvailable") Boolean skuAvailable,
                       @Param("currency") String currency,
                       @Param("price") BigDecimal price,
                       @Param("exclusivePrice") BigDecimal exclusivePrice,
                       @Param("discountedPrice") BigDecimal discountedPrice,
                       @Param("shippingFee") BigDecimal shippingFee,
                       @Param("sellerAvailableInventory") Integer sellerAvailableInventory,
                       @Param("buyerAvailableInventory") Integer buyerAvailableInventory,
                       @Param("detailJson") String detailJson,
                       @Param("priceJson") String priceJson,
                       @Param("inventoryJson") String inventoryJson,
                       @Param("syncBatchNo") String syncBatchNo,
                       @Param("now") LocalDateTime now);

    List<XqGigaProductRow> selectListBySkus(@Param("skus") Collection<String> skus);

    XqGigaProductRow selectIndexBySku(@Param("sku") String sku);

    List<XqGigaProductRow> selectIndexByListIds(@Param("ids") Collection<String> ids);

    List<XqGigaProductRow> selectIndexMembersByFamilySkus(@Param("familySkus") Collection<String> familySkus);

    /** 批量读取快照合并价：自提 / 一件代发 */
    List<XqGigaProductRow> selectPriceRolesBySkus(@Param("skus") Collection<String> skus);

    /** 下发成功：在快照 detail_json.xqPickedPlatforms 打平台标记 */
    int mergePickedPlatform(@Param("sku") String sku,
                            @Param("platformId") String platformId,
                            @Param("platformName") String platformName);

    /** 关闭后无进行中/已上架任务时，去掉该平台标记 */
    int removePickedPlatform(@Param("sku") String sku,
                             @Param("platformId") String platformId);

    /** 定时同步：已存在的 SKU（全局，相同 SKU 不录入） */
    List<String> selectExistingListSkus(@Param("skus") Collection<String> skus);

    /** 定时同步：只增不覆盖 */
    int insertProductList(@Param("id") String id,
                          @Param("userId") String userId,
                          @Param("gigaConfigId") String gigaConfigId,
                          @Param("sku") String sku,
                          @Param("productName") String productName,
                          @Param("updateTime") String updateTime,
                          @Param("firstArrivalDate") String firstArrivalDate,
                          @Param("addedTime") String addedTime,
                          @Param("syncBatchNo") String syncBatchNo,
                          @Param("now") LocalDateTime now);

    /** 新 SKU 写入家族索引桩，使选品列表可见（详情可按需补） */
    int upsertFamilyIndexStub(@Param("sku") String sku,
                              @Param("listId") String listId,
                              @Param("productName") String productName,
                              @Param("now") LocalDateTime now);

}
