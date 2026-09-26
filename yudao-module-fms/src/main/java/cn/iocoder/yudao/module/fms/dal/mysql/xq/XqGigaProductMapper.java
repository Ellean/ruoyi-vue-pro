package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 读 xq-erp Giga 产品（数据源 xq → xq_finance_test）
 */
@Mapper
@DS("xq")
public interface XqGigaProductMapper {

    IPage<XqGigaProductRow> selectPage(IPage<XqGigaProductRow> page,
                                       @Param("name") String name,
                                       @Param("categoryIds") Collection<Long> categoryIds);

    XqGigaProductRow selectById(@Param("id") String id);

}
