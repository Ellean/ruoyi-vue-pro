package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqAgentAiResultRow;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 读本地 xq_finance_test 已同步的 AI 图结果（测试用，不走处理流水线）
 */
@Mapper
@DS("xq")
public interface XqAgentAiResultMapper {

    long selectPageCount(@Param("sku") String sku, @Param("keyword") String keyword);

    List<XqAgentAiResultRow> selectPage(
            @Param("sku") String sku,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") long limit);

    XqAgentAiResultRow selectById(@Param("id") String id);

}
