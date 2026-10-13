package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductRespVO;

/**
 * 选品详情按需补齐：无详情/缺自提·代发价时，用全部启用凭证调大健云仓拉取并写回快照。
 */
public interface XqGigaProductEnsureService {

    /**
     * 按列表 id 拉取详情 + 多密钥价格合并（自提/代发），写 snapshot 后返回最新详情。
     */
    XqProductRespVO ensureDetail(String listId);

    /** 本地行是否缺详情或缺自提/代发运费（前端/调用方判断是否触发 ensure） */
    boolean needsEnsure(XqProductRespVO vo);

}
