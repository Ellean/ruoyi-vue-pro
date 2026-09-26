package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductSaveReqVO;

import jakarta.validation.Valid;

public interface XqProductService {

    Long createProduct(@Valid XqProductSaveReqVO createReqVO);

    void updateProduct(@Valid XqProductSaveReqVO updateReqVO);

    void deleteProduct(Long id);

    /** 品库详情：读 xq-erp Giga 产品 */
    XqProductRespVO getProduct(String id);

    /** 品库分页：读 xq-erp Giga 产品 */
    PageResult<XqProductRespVO> getProductPage(XqProductPageReqVO pageReqVO);

}
