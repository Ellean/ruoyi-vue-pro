package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.category.XqGigaCategoryTreeRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaSiteCategoryDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaSiteCategoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Validated
public class XqGigaCategoryServiceImpl implements XqGigaCategoryService {

    private final AtomicReference<List<XqGigaSiteCategoryDO>> allRowsCache = new AtomicReference<>();

    @Resource
    private XqGigaSiteCategoryMapper categoryMapper;

    private List<XqGigaSiteCategoryDO> loadAllRows() {
        List<XqGigaSiteCategoryDO> cached = allRowsCache.get();
        if (cached != null) {
            return cached;
        }
        List<XqGigaSiteCategoryDO> rows = categoryMapper.selectAllOrdered();
        allRowsCache.compareAndSet(null, rows);
        return allRowsCache.get();
    }

    @Override
    public List<XqGigaCategoryTreeRespVO> getTree() {
        List<XqGigaSiteCategoryDO> rows = loadAllRows();
        return buildTree(rows);
    }

    @Override
    public List<XqGigaSiteCategoryDO> getLevel1() {
        return categoryMapper.selectByLevel(1);
    }

    @Override
    public List<Long> listSubtreeGigaIds(Long gigaId) {
        if (gigaId == null) {
            return List.of();
        }
        List<XqGigaSiteCategoryDO> rows = loadAllRows();
        List<XqGigaSiteCategoryDO> selves = rows.stream()
                .filter(r -> Objects.equals(r.getGigaId(), gigaId))
                .toList();
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(gigaId);
        if (selves.isEmpty()) {
            return new ArrayList<>(ids);
        }
        for (XqGigaSiteCategoryDO self : selves) {
            String base = self.getPathIds();
            if (base == null || base.isEmpty()) {
                base = String.valueOf(gigaId);
            }
            String prefix = base + "/";
            for (XqGigaSiteCategoryDO row : rows) {
                String path = row.getPathIds();
                if (path == null) {
                    continue;
                }
                if (path.equals(base) || path.startsWith(prefix)) {
                    ids.add(row.getGigaId());
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private List<XqGigaCategoryTreeRespVO> buildTree(List<XqGigaSiteCategoryDO> rows) {
        List<XqGigaSiteCategoryDO> level1 = rows.stream()
                .filter(r -> Objects.equals(r.getLevel(), 1))
                .sorted(Comparator.comparing(XqGigaSiteCategoryDO::getSortOrder)
                        .thenComparing(XqGigaSiteCategoryDO::getGigaId))
                .toList();
        List<XqGigaCategoryTreeRespVO> roots = new ArrayList<>();
        for (XqGigaSiteCategoryDO l1 : level1) {
            XqGigaCategoryTreeRespVO n1 = toNode(l1);
            List<XqGigaSiteCategoryDO> l2s = rows.stream()
                    .filter(r -> Objects.equals(r.getLevel(), 2) && Objects.equals(r.getParentId(), l1.getGigaId()))
                    .sorted(Comparator.comparing(XqGigaSiteCategoryDO::getSortOrder)
                            .thenComparing(XqGigaSiteCategoryDO::getGigaId))
                    .toList();
            for (XqGigaSiteCategoryDO l2 : l2s) {
                XqGigaCategoryTreeRespVO n2 = toNode(l2);
                List<XqGigaSiteCategoryDO> l3s = rows.stream()
                        .filter(r -> Objects.equals(r.getLevel(), 3) && Objects.equals(r.getParentId(), l2.getGigaId()))
                        .sorted(Comparator.comparing(XqGigaSiteCategoryDO::getSortOrder)
                                .thenComparing(XqGigaSiteCategoryDO::getGigaId))
                        .toList();
                if (l3s.isEmpty()) {
                    n2.getChildren().add(toNode(l2));
                } else {
                    for (XqGigaSiteCategoryDO l3 : l3s) {
                        n2.getChildren().add(toNode(l3));
                    }
                }
                n1.getChildren().add(n2);
            }
            roots.add(n1);
        }
        return roots;
    }

    private XqGigaCategoryTreeRespVO toNode(XqGigaSiteCategoryDO row) {
        XqGigaCategoryTreeRespVO vo = new XqGigaCategoryTreeRespVO();
        vo.setId(row.getGigaId());
        vo.setGigaId(row.getGigaId());
        vo.setParentId(row.getParentId());
        vo.setName(row.getName());
        vo.setLevel(row.getLevel());
        vo.setSortOrder(row.getSortOrder());
        vo.setImagePath(row.getImagePath());
        vo.setPathIds(row.getPathIds());
        vo.setPathNames(row.getPathNames());
        vo.setHref(row.getHref());
        return vo;
    }

}
