package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source.XqSourceItemPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSourceItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqSourceItemMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_SOURCE_ALREADY_CLAIMED;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_SOURCE_NOT_EXISTS;

@Service
@Validated
public class XqSourceItemServiceImpl implements XqSourceItemService {

    /** 进行中 */
    public static final int WORK_STATUS_DOING = 10;

    @Resource
    private XqSourceItemMapper sourceItemMapper;
    @Resource
    private XqWorkOrderMapper workOrderMapper;

    @Override
    public PageResult<XqSourceItemDO> getSourcePage(XqSourceItemPageReqVO pageReqVO) {
        return sourceItemMapper.selectPage(pageReqVO);
    }

    @Override
    public XqSourceItemDO getSource(Long id) {
        return sourceItemMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XqWorkOrderDO claimSource(Long sourceId, Long userId) {
        XqSourceItemDO source = sourceItemMapper.selectById(sourceId);
        if (source == null) {
            throw exception(XQ_SOURCE_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(source.getClaimed())) {
            throw exception(XQ_SOURCE_ALREADY_CLAIMED);
        }
        // 标记已认领
        XqSourceItemDO update = new XqSourceItemDO();
        update.setId(sourceId);
        update.setClaimed(true);
        sourceItemMapper.updateById(update);

        XqWorkOrderDO order = XqWorkOrderDO.builder()
                .no("WO" + IdUtil.getSnowflakeNextIdStr())
                .sourceId(sourceId)
                .externalSku(source.getExternalSku())
                .title(source.getTitle())
                .status(WORK_STATUS_DOING)
                .assigneeUserId(userId)
                .build();
        workOrderMapper.insert(order);
        return order;
    }

}
