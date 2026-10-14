package cn.iocoder.yudao.module.fms.service.config;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.fms.dal.dataobject.config.FmsAuxiliaryCombinationDO;
import cn.iocoder.yudao.module.fms.dal.mysql.config.FmsAuxiliaryCombinationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(FmsAuxiliaryCombinationServiceImpl.class)
public class FmsAuxiliaryCombinationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private FmsAuxiliaryCombinationServiceImpl auxiliaryCombinationService;
    @Resource
    private FmsAuxiliaryCombinationMapper auxiliaryCombinationMapper;

    @Test
    public void testDeleteAuxiliaryCombinationByAuxiliaryItemIds() {
        // mock 数据
        FmsAuxiliaryCombinationDO.AuxiliaryItem deletedItem = FmsAuxiliaryCombinationDO.AuxiliaryItem.builder()
                .typeId(31L).itemId(41L).build();
        FmsAuxiliaryCombinationDO deletedCombination = new FmsAuxiliaryCombinationDO().setAccountSetId(1L)
                .setSubjectId(101L).setItems(Collections.singletonList(deletedItem));
        auxiliaryCombinationMapper.insert(deletedCombination);
        FmsAuxiliaryCombinationDO retainedCombination = new FmsAuxiliaryCombinationDO().setAccountSetId(1L)
                .setSubjectId(101L).setItems(Collections.singletonList(
                        FmsAuxiliaryCombinationDO.AuxiliaryItem.builder().typeId(31L).itemId(42L).build()));
        auxiliaryCombinationMapper.insert(retainedCombination);

        // 调用
        auxiliaryCombinationService.deleteAuxiliaryCombinationByAuxiliaryItemIds(
                1L, Collections.singletonList(41L));

        // 断言
        assertNull(auxiliaryCombinationMapper.selectById(deletedCombination.getId()));
        // createTime/updateTime 由 MyBatis Plus 自动填充为纳秒精度，H2 的 TIMESTAMP(6) 回读会
        // 四舍五入到微秒，逐位比对必然随机失败；按上游约定（见 OAuth2TokenServiceImplTest）忽略
        assertPojoEquals(retainedCombination,
                auxiliaryCombinationMapper.selectById(retainedCombination.getId()),
                "createTime", "updateTime", "deleted");
    }

}
