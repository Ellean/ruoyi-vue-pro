package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 读 xq-erp Giga 产品（数据源 xq → xq_finance_test）
 */
@Mapper
@DS("xq")
public interface XqGigaProductMapper {

    Long selectPageCount(@Param("name") String name,
                         @Param("categoryIds") Collection<Long> categoryIds);

    List<String> selectPageIds(@Param("name") String name,
                               @Param("categoryIds") Collection<Long> categoryIds,
                               @Param("offset") long offset,
                               @Param("limit") long limit);

    List<XqGigaProductRow> selectListByIds(@Param("ids") Collection<String> ids);

    XqGigaProductRow selectById(@Param("id") String id);

    XqGigaProductRow selectBySku(@Param("sku") String sku);

    List<XqGigaProductRow> selectListBySkus(@Param("skus") Collection<String> skus);

}
