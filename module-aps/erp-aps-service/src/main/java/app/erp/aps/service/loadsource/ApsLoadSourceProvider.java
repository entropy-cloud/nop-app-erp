package app.erp.aps.service.loadsource;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConstants;
import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.biz.IErpApsLoadSourceProvider;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * APS 排程时间负荷来源 SPI 实现（CRP 跨域读 APS OperationOrder 排程时间，plan 2026-07-05-0306-2）。
 *
 * <p>实现 {@link IErpApsLoadSourceProvider}（声明于 mfg-dao 的消费方接口），读自身 {@link ErpApsOperationOrder}
 * 按 {@code workOrderId} 聚合 {@code sequence}×{@code machineId}×{@code plannedStartDateT/plannedEndDateT}×
 * {@code setupTime} 时段。仅返回已排程成功（{@code status=PLANNED}）且时间字段非空的工序；冲突时
 * 引擎已清空时间为 null（见 {@code ErpApsSchedulingEngine:81-82}），自然被过滤。
 *
 * <p>本类为非 BizModel 服务助手（对齐 {@code ErpApsAtpCtpServiceImpl} 范式——aps 域内只读聚合用
 * {@link IDaoProvider}，无业务写、无状态机，{@code IDaoProvider} 读取等价且零启动耦合）。
 * 跨域 I*Biz 不适用：本接口不是制造/库存域业务方法的调用方，而是 APS 自身实体的只读导出。
 */
public class ApsLoadSourceProvider implements IErpApsLoadSourceProvider {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    @Override
    public List<ApsLoadSlot> findScheduledSlots(List<Long> workOrderIds, LocalDate periodFrom, LocalDate periodTo) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }

        QueryBean q = new QueryBean();
        q.addFilter(in("workOrderId", new ArrayList<>(workOrderIds)));
        // 仅 PLANNED/IN_PROGRESS 状态的 OperationOrder 才有有效排程时间；IN_PROGRESS（已派工，RC-R1.88 D6
        // 裁决）一并导出——派工先于 mfg 日批建卡的工序仍须进入建卡来源（mfg 建卡 seam 幂等增量，零双卡）；
        // FINISHED/CANCELLED 已完工/取消不再构成未来负荷。DRAFT 冲突时引擎已清空时间为 null，自然被过滤。
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_IN_PROGRESS)));
        // 排程时段与 CRP 窗口相交：plannedEndT >= periodFrom AND plannedStartT <= periodTo+1day
        LocalDateTime winFrom = periodFrom == null ? null : periodFrom.atStartOfDay();
        LocalDateTime winTo = periodTo == null ? null : periodTo.plusDays(1).atStartOfDay();
        if (winFrom != null) {
            q.addFilter(ge("plannedEndDateT", winFrom));
        }
        if (winTo != null) {
            q.addFilter(le("plannedStartDateT", winTo));
        }

        List<ErpApsOperationOrder> orders = daoProvider.daoFor(ErpApsOperationOrder.class).findAllByQuery(q);
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApsLoadSlot> slots = new ArrayList<>(orders.size());
        for (ErpApsOperationOrder op : orders) {
            if (op.getPlannedStartDateT() == null || op.getPlannedEndDateT() == null) {
                continue;
            }
            if (op.getMachineId() == null) {
                continue;
            }
            ApsLoadSlot slot = new ApsLoadSlot();
            slot.setOperationOrderId(op.getId());
            slot.setWorkOrderId(op.getWorkOrderId());
            slot.setSequence(op.getSequence());
            slot.setWorkcenterId(op.getMachineId());
            slot.setPlannedStartT(op.getPlannedStartDateT().toLocalDateTime());
            slot.setPlannedEndT(op.getPlannedEndDateT().toLocalDateTime());
            slot.setSetupTime(op.getSetupTime());
            slots.add(slot);
        }
        return slots;
    }
}
