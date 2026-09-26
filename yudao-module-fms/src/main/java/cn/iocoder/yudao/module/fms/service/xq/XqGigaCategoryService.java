package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.category.XqGigaCategoryTreeRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaSiteCategoryDO;

import java.util.List;

public interface XqGigaCategoryService {

    List<XqGigaCategoryTreeRespVO> getTree();

    List<XqGigaSiteCategoryDO> getLevel1();

    /** 返回某节点自身 + 全部子孙的 gigaId，用于产品筛选 */
    List<Long> listSubtreeGigaIds(Long gigaId);

}
