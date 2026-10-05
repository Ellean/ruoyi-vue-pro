package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_PRODUCT_SKU_DUPLICATE;

@Service
@Validated
public class XqProductServiceImpl implements XqProductService {

    /** 无筛选 COUNT 短缓存，避免每次全表 distinct */
    private static final long UNFILTERED_COUNT_TTL_MS = 60_000L;
    private final AtomicReference<Long> unfilteredCountCache = new AtomicReference<>();
    private final AtomicLong unfilteredCountCachedAt = new AtomicLong(0);

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
    public PageResult<XqProductRespVO> getProductPage(XqProductPageReqVO pageReqVO) {
        Collection<Long> categoryIds = null;
        if (pageReqVO.getGigaCategoryId() != null) {
            List<Long> ids = categoryService.listSubtreeGigaIds(pageReqVO.getGigaCategoryId());
            categoryIds = ids.isEmpty() ? Collections.singletonList(-1L) : ids;
        }
        int pageNo = pageReqVO.getPageNo() == null || pageReqVO.getPageNo() < 1
                ? 1 : pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize() == null || pageReqVO.getPageSize() < 1
                ? 20 : pageReqVO.getPageSize();
        long offset = (long) (pageNo - 1) * pageSize;

        Long total;
        boolean unfiltered = StrUtil.isBlank(pageReqVO.getName()) && CollUtil.isEmpty(categoryIds);
        if (unfiltered) {
            long now = System.currentTimeMillis();
            Long cached = unfilteredCountCache.get();
            if (cached != null && now - unfilteredCountCachedAt.get() < UNFILTERED_COUNT_TTL_MS) {
                total = cached;
            } else {
                total = gigaProductMapper.selectPageCount(pageReqVO.getName(), categoryIds);
                unfilteredCountCache.set(total);
                unfilteredCountCachedAt.set(now);
            }
        } else {
            total = gigaProductMapper.selectPageCount(pageReqVO.getName(), categoryIds);
        }
        if (total == null || total <= 0) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<String> ids = gigaProductMapper.selectPageIds(
                pageReqVO.getName(), categoryIds, offset, pageSize);
        if (CollUtil.isEmpty(ids)) {
            return new PageResult<>(Collections.emptyList(), total);
        }
        List<XqGigaProductRow> rows = gigaProductMapper.selectListByIds(ids);
        Map<String, XqGigaProductRow> byId = new LinkedHashMap<>();
        for (XqGigaProductRow row : rows) {
            byId.put(row.getId(), row);
        }
        // 保持分页顺序
        List<XqProductRespVO> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            XqGigaProductRow row = byId.get(id);
            if (row != null) {
                list.add(toResp(row, false));
            }
        }
        hydrateVariantCovers(list);
        return new PageResult<>(list, total);
    }

    @Override
    public XqProductRespVO getProduct(String id) {
        XqGigaProductRow row = gigaProductMapper.selectById(id);
        if (row == null) {
            throw exception(XQ_PRODUCT_NOT_EXISTS);
        }
        XqProductRespVO vo = toResp(row, true);
        hydrateVariantDetails(vo, row);
        return vo;
    }

    private XqProductRespVO toResp(XqGigaProductRow row, boolean withGallery) {
        XqProductRespVO vo = BeanUtils.toBean(row, XqProductRespVO.class);
        if (Boolean.FALSE.equals(row.getSkuAvailable())) {
            vo.setStatus(1);
        } else {
            vo.setStatus(0);
        }
        if (withGallery) {
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
        } else {
            // 列表：只回封面 + 数量，不带全量 imageUrls / HTML 文案
            vo.setImageUrls(Collections.emptyList());
            vo.setDescription(null);
            if (vo.getImageCount() == null) {
                vo.setImageCount(0);
            }
        }
        vo.setMainColor(row.getMainColor());
        vo.setVariants(buildVariantStubs(row));
        return vo;
    }

    private List<XqProductRespVO.Variant> buildVariantStubs(XqGigaProductRow row) {
        List<XqProductRespVO.Variant> out = new ArrayList<>();
        String self = StrUtil.blankToDefault(row.getSku(), row.getItemCode());
        List<String> family = XqGigaAssociateSupport.familySkus(self, row.getAssociateProductListJson(),
                row.getAssociateProductInfoJson());
        Map<String, String> names = XqGigaAssociateSupport.parseInfoNames(row.getAssociateProductInfoJson());
        for (String sku : family) {
            if (StrUtil.equals(sku, self)) {
                continue;
            }
            XqProductRespVO.Variant v = new XqProductRespVO.Variant();
            v.setSku(sku);
            v.setItemCode(sku);
            v.setName(XqGigaAssociateSupport.variantLabel(sku, null, row.getAssociateProductInfoJson()));
            if (names.containsKey(sku)) {
                v.setMainColor(names.get(sku));
            }
            out.add(v);
        }
        return out;
    }

    private void hydrateVariantCovers(List<XqProductRespVO> list) {
        Set<String> skus = new LinkedHashSet<>();
        for (XqProductRespVO vo : list) {
            for (XqProductRespVO.Variant v : vo.getVariants()) {
                if (StrUtil.isNotBlank(v.getSku())) {
                    skus.add(v.getSku());
                }
            }
        }
        if (skus.isEmpty()) {
            return;
        }
        Map<String, XqGigaProductRow> bySku = new LinkedHashMap<>();
        for (XqGigaProductRow row : gigaProductMapper.selectListBySkus(skus)) {
            bySku.put(row.getSku(), row);
        }
        for (XqProductRespVO vo : list) {
            for (XqProductRespVO.Variant v : vo.getVariants()) {
                XqGigaProductRow row = bySku.get(v.getSku());
                if (row == null) {
                    continue;
                }
                v.setId(row.getId());
                v.setImageUrl(row.getImageUrl());
                v.setQtyAvailable(row.getQtyAvailable());
                v.setPrice(row.getPrice());
                v.setDiscountedPrice(row.getDiscountedPrice());
                v.setMainColor(StrUtil.blankToDefault(row.getMainColor(), v.getMainColor()));
                if (StrUtil.isBlank(v.getName()) || v.getName().equals(v.getSku())) {
                    v.setName(XqGigaAssociateSupport.variantLabel(
                            v.getSku(), row.getMainColor(), row.getAssociateProductInfoJson()));
                }
            }
        }
    }

    private void hydrateVariantDetails(XqProductRespVO vo, XqGigaProductRow row) {
        List<XqProductRespVO.Variant> stubs = vo.getVariants();
        if (stubs == null || stubs.isEmpty()) {
            stubs = buildVariantStubs(row);
        }
        List<String> keys = new ArrayList<>();
        for (XqProductRespVO.Variant v : stubs) {
            if (StrUtil.isNotBlank(v.getSku())) {
                keys.add(v.getSku());
            }
            if (StrUtil.isNotBlank(v.getItemCode()) && !StrUtil.equals(v.getItemCode(), v.getSku())) {
                keys.add(v.getItemCode());
            }
        }
        if (keys.isEmpty()) {
            vo.setVariants(stubs);
            return;
        }
        Map<String, XqGigaProductRow> byKey = new LinkedHashMap<>();
        for (XqGigaProductRow sib : gigaProductMapper.selectListBySkus(keys)) {
            byKey.put(sib.getSku(), sib);
            if (StrUtil.isNotBlank(sib.getItemCode())) {
                byKey.putIfAbsent(sib.getItemCode(), sib);
            }
        }
        for (XqProductRespVO.Variant v : stubs) {
            XqGigaProductRow sib = byKey.get(v.getSku());
            if (sib == null) {
                sib = byKey.get(v.getItemCode());
            }
            if (sib == null) {
                continue;
            }
            v.setId(sib.getId());
            v.setSku(sib.getSku());
            v.setItemCode(StrUtil.blankToDefault(sib.getItemCode(), sib.getSku()));
            v.setImageUrl(sib.getImageUrl());
            v.setQtyAvailable(sib.getQtyAvailable());
            v.setPrice(sib.getPrice());
            v.setDiscountedPrice(sib.getDiscountedPrice());
            v.setMainColor(StrUtil.blankToDefault(sib.getMainColor(), v.getMainColor()));
            v.setName(XqGigaAssociateSupport.variantLabel(
                    sib.getSku(), sib.getMainColor(), sib.getAssociateProductInfoJson()));
        }
        vo.setVariants(stubs);
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
