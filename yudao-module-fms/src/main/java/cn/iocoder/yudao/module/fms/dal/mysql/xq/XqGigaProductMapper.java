package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    XqGigaProductRow selectBySku(@Param("sku") String sku);

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

}
