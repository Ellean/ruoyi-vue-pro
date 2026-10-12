package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent.XqAgentAiTestPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent.XqAgentAiTestRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqAgentAiResultRow;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqAgentAiResultMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 本地 AI 图结果测试页：只读展示，不触发处理 */
@Service
public class XqAgentAiTestService {

    @Resource
    private XqAgentAiResultMapper agentAiResultMapper;

    public PageResult<XqAgentAiTestRespVO> getPage(XqAgentAiTestPageReqVO reqVO) {
        String sku = StrUtil.blankToDefault(reqVO.getSku(), "").trim();
        String keyword = StrUtil.blankToDefault(reqVO.getKeyword(), "").trim();
        Boolean assignedOnly = Boolean.TRUE.equals(reqVO.getAssignedOnly()) ? Boolean.TRUE : null;
        long total = agentAiResultMapper.selectPageCount(sku, keyword, assignedOnly);
        if (total <= 0) {
            return new PageResult<>(List.of(), 0L);
        }
        int pageNo = Math.max(reqVO.getPageNo(), 1);
        int pageSize = Math.min(Math.max(reqVO.getPageSize(), 1), 100);
        long offset = (long) (pageNo - 1) * pageSize;
        List<XqAgentAiResultRow> rows =
                agentAiResultMapper.selectPage(sku, keyword, assignedOnly, offset, pageSize);
        List<XqAgentAiTestRespVO> list = new ArrayList<>(rows.size());
        for (XqAgentAiResultRow row : rows) {
            list.add(toBrief(row));
        }
        return new PageResult<>(list, total);
    }

    public XqAgentAiTestRespVO get(String id) {
        if (StrUtil.isBlank(id)) {
            return null;
        }
        XqAgentAiResultRow row = agentAiResultMapper.selectById(id.trim());
        if (row == null) {
            return null;
        }
        XqAgentAiTestRespVO vo = toBrief(row);
        vo.setImages(parseImages(row.getAgentResultsJson()));
        vo.setCopy(parseCopy(row.getAgentResultsJson()));
        if (vo.getCopy() != null) {
            vo.setHasCopy(true);
            if (StrUtil.isBlank(vo.getCopyTitle())) {
                vo.setCopyTitle(vo.getCopy().getTitle());
            }
            if (vo.getBulletCount() == null || vo.getBulletCount() <= 0) {
                vo.setBulletCount(vo.getCopy().getBullets() == null ? 0 : vo.getCopy().getBullets().size());
            }
        }
        return vo;
    }

    private static XqAgentAiTestRespVO toBrief(XqAgentAiResultRow row) {
        XqAgentAiTestRespVO vo = new XqAgentAiTestRespVO();
        vo.setId(row.getId());
        vo.setSku(row.getSku());
        vo.setProductName(row.getProductName());
        vo.setImageCount(row.getImageCount());
        vo.setCoverUrl(row.getCoverUrl());
        vo.setBatchNo(row.getBatchNo());
        vo.setGeneratedAt(row.getGeneratedAt());
        vo.setUpdateDate(row.getUpdateDate());
        vo.setListingPlatformId(row.getListingPlatformId());
        vo.setPlatformName(row.getPlatformName());
        vo.setShopId(row.getShopId());
        vo.setListingCategoryId(row.getListingCategoryId());
        vo.setCategoryName(row.getCategoryName());
        vo.setAssignOwnerUserId(row.getAssignOwnerUserId());
        vo.setAssignOwnerName(row.getAssignOwnerName());
        vo.setAssignCopyUserId(row.getAssignCopyUserId());
        vo.setAssignCopyName(row.getAssignCopyName());
        vo.setAssignImageUserId(row.getAssignImageUserId());
        vo.setAssignImageName(row.getAssignImageName());
        vo.setAssignPhase(row.getAssignPhase());
        vo.setCopyTitle(row.getCopyTitle());
        vo.setHasCopy(Boolean.TRUE.equals(row.getHasCopy()));
        vo.setBulletCount(row.getBulletCount());
        return vo;
    }

    private static XqAgentAiTestRespVO.CopyItem parseCopy(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            JSONObject root = JSONUtil.parseObj(json);
            JSONObject copy = root.getJSONObject("copy");
            if (copy == null || copy.isEmpty()) {
                return null;
            }
            XqAgentAiTestRespVO.CopyItem item = new XqAgentAiTestRespVO.CopyItem();
            item.setTitle(StrUtil.trim(copy.getStr("title")));
            item.setDescription(StrUtil.trim(copy.getStr("description")));
            item.setProductStyle(StrUtil.trim(copy.getStr("productStyle")));
            List<String> bullets = new ArrayList<>();
            Object rawBullets = copy.get("bullets");
            if (!(rawBullets instanceof JSONArray) || ((JSONArray) rawBullets).isEmpty()) {
                rawBullets = copy.get("bulletPoints");
            }
            if (rawBullets instanceof JSONArray arr) {
                for (Object b : arr) {
                    String text = bulletText(b);
                    if (StrUtil.isNotBlank(text)) {
                        bullets.add(text);
                    }
                }
            }
            item.setBullets(bullets);
            boolean empty = StrUtil.isBlank(item.getTitle())
                    && StrUtil.isBlank(item.getDescription())
                    && bullets.isEmpty();
            return empty ? null : item;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String bulletText(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof CharSequence || raw instanceof Number) {
            return String.valueOf(raw).trim();
        }
        try {
            JSONObject obj = JSONUtil.parseObj(raw);
            return StrUtil.blankToDefault(
                    obj.getStr("text"),
                    StrUtil.blankToDefault(obj.getStr("content"), obj.getStr("value"))
            ).trim();
        } catch (Exception ignored) {
            return String.valueOf(raw).trim();
        }
    }

    private static List<XqAgentAiTestRespVO.ImageItem> parseImages(String json) {
        List<XqAgentAiTestRespVO.ImageItem> out = new ArrayList<>();
        if (StrUtil.isBlank(json)) {
            return out;
        }
        try {
            JSONObject root = JSONUtil.parseObj(json);
            Object images = root.get("images");
            if (images instanceof JSONArray arr) {
                for (Object item : arr) {
                    XqAgentAiTestRespVO.ImageItem img = toImageItem(item);
                    if (img != null) {
                        out.add(img);
                    }
                }
            } else if (images instanceof JSONObject obj) {
                XqAgentAiTestRespVO.ImageItem img = toImageItem(obj);
                if (img != null) {
                    out.add(img);
                }
            }
        } catch (Exception ignored) {
            // ignore bad json
        }
        return out;
    }

    private static XqAgentAiTestRespVO.ImageItem toImageItem(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof CharSequence || raw instanceof Number) {
            String url = String.valueOf(raw).trim();
            if (StrUtil.isBlank(url)) {
                return null;
            }
            XqAgentAiTestRespVO.ImageItem item = new XqAgentAiTestRespVO.ImageItem();
            item.setUrl(url);
            item.setDisplayUrl(url);
            return item;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(raw);
            String url = firstUrl(obj, "url", "imageUrl", "src");
            String oss = firstUrl(obj, "ossUrl", "oss_url");
            String display = firstUrl(obj, "displayUrl", "display_url");
            String pick = StrUtil.blankToDefault(display, StrUtil.blankToDefault(oss, url));
            if (StrUtil.isBlank(pick)) {
                return null;
            }
            XqAgentAiTestRespVO.ImageItem item = new XqAgentAiTestRespVO.ImageItem();
            item.setName(StrUtil.blankToDefault(obj.getStr("name"), obj.getStr("imageName")));
            item.setUrl(url);
            item.setOssUrl(oss);
            item.setDisplayUrl(pick);
            return item;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstUrl(JSONObject obj, String... keys) {
        for (String key : keys) {
            String v = StrUtil.blankToDefault(obj.getStr(key), "").trim();
            if (StrUtil.isNotBlank(v) && !"null".equalsIgnoreCase(v)) {
                return v;
            }
        }
        return "";
    }

}
