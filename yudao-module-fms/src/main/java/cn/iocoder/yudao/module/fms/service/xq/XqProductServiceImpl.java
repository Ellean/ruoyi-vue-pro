package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductSaveReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqProductDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqProductMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_PRODUCT_SKU_DUPLICATE;

@Service
@Validated
public class XqProductServiceImpl implements XqProductService {

    @Resource
    private XqProductMapper productMapper;
    @Resource
    private XqGigaProductMapper gigaProductMapper;
    @Resource
    private XqGigaCategoryService categoryService;

    @Override
    public Long createProduct(XqProductSaveReqVO createReqVO) {
        validateSkuUnique(null, createReqVO.getSku());
        XqProductDO product = BeanUtils.toBean(createReqVO, XqProductDO.class);
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void updateProduct(XqProductSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateSkuUnique(updateReqVO.getId(), updateReqVO.getSku());
        productMapper.updateById(BeanUtils.toBean(updateReqVO, XqProductDO.class));
    }

    @Override
    public void deleteProduct(Long id) {
        validateExists(id);
        productMapper.deleteById(id);
    }

    @Override
    public XqProductRespVO getProduct(String id) {
        XqGigaProductRow row = gigaProductMapper.selectById(id);
        if (row == null) {
            throw exception(XQ_PRODUCT_NOT_EXISTS);
        }
        return toResp(row);
    }

    @Override
    public PageResult<XqProductRespVO> getProductPage(XqProductPageReqVO pageReqVO) {
        Collection<Long> categoryIds = null;
        if (pageReqVO.getGigaCategoryId() != null) {
            List<Long> ids = categoryService.listSubtreeGigaIds(pageReqVO.getGigaCategoryId());
            categoryIds = ids.isEmpty() ? Collections.singletonList(-1L) : ids;
        }
        IPage<XqGigaProductRow> page = gigaProductMapper.selectPage(
                new Page<>(pageReqVO.getPageNo(), pageReqVO.getPageSize()),
                pageReqVO.getName(),
                categoryIds);
        List<XqProductRespVO> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private XqProductRespVO toResp(XqGigaProductRow row) {
        XqProductRespVO vo = BeanUtils.toBean(row, XqProductRespVO.class);
        if (Boolean.FALSE.equals(row.getSkuAvailable())) {
            vo.setStatus(1);
        } else {
            vo.setStatus(0);
        }
        List<String> urls = parseImageUrls(row.getImageUrlsJson());
        if (StrUtil.isNotBlank(row.getImageUrl())
                && urls.stream().noneMatch(u -> u.equals(row.getImageUrl()))) {
            urls.add(0, row.getImageUrl());
        }
        vo.setImageUrls(urls);
        vo.setImageCount(urls.isEmpty() ? row.getImageCount() : urls.size());
        if (StrUtil.isBlank(vo.getImageUrl()) && !urls.isEmpty()) {
            vo.setImageUrl(urls.get(0));
        }
        return vo;
    }

    private List<String> parseImageUrls(String json) {
        if (StrUtil.isBlank(json) || "null".equalsIgnoreCase(json)) {
            return new ArrayList<>();
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            Set<String> set = new LinkedHashSet<>();
            for (Object o : arr) {
                String url = StrUtil.trim(String.valueOf(o));
                if (StrUtil.isNotBlank(url) && !"null".equalsIgnoreCase(url)) {
                    set.add(url);
                }
            }
            return new ArrayList<>(set);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private void validateExists(Long id) {
        if (id == null || productMapper.selectById(id) == null) {
            throw exception(XQ_PRODUCT_NOT_EXISTS);
        }
    }

    private void validateSkuUnique(Long id, String sku) {
        XqProductDO exists = productMapper.selectBySku(sku);
        if (exists == null) {
            return;
        }
        if (id == null || !exists.getId().equals(id)) {
            throw exception(XQ_PRODUCT_SKU_DUPLICATE);
        }
    }

}
