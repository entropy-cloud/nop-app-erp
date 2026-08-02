package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;

/**
 * ErpMntVisit complete per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 IN_PROGRESS→COMPLETED 编排：状态守卫 + 状态翻转 + endTime/totalMinutes/completedAt 计算 + 落库 + 设备状态恢复
 * + [会计保护区域] 维修工时费用化 GL 过账（Dr: 折旧费用 6602 / Cr: 应付职工薪酬 2211），config-gated 默认关。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntVisitProcessor}。
 *
 * <p>[会计保护区域] GL 过账惯法逐字搬运自原 ErpMntVisitBizModel.doComplete：config-gating（isPostingEnabled）+
 * 失败不阻断 complete 终态（吞异常告警），对照 TestErpMntLaborPosting 校验语义不变。
 */
public class ErpMntVisitCompleteProcessor extends AbstractErpMntVisitProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMntVisitCompleteProcessor.class);

    public ErpMntVisit complete(Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateTransition(visit, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, "IN_PROGRESS");
        doComplete(visit, context);
        equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context);
        return visit;
    }

    protected void doComplete(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_COMPLETED);
        Timestamp endTime = visit.getEndTime() == null ? CoreMetrics.currentTimestamp() : visit.getEndTime();
        visit.setEndTime(endTime);
        if (visit.getStartTime() != null) {
            long minutes = Duration.between(visit.getStartTime().toLocalDateTime(), endTime.toLocalDateTime()).toMinutes();
            visit.setTotalMinutes(BigDecimal.valueOf(minutes));
        }
        visit.setCompletedAt(CoreMetrics.currentTimestamp());
        visitDao().updateEntity(visit);

        // 维修工时费用化 GL 过账（Dr: 折旧费用 6602 / Cr: 应付职工薪酬 2211），config-gated 默认关
        //（plan 2026-07-18-0949-1 Phase 1 Decision (c) 内嵌触发 + (e) 显式消费 boolean 返回值）。
        // 失败不阻断 complete 终态（吞异常范式，对齐 MaintenanceIssuePostingDispatcher.dispatchIfApplicable）。
        if (laborPostingDispatcher.isPostingEnabled()) {
            if (!laborPostingDispatcher.postLabor(visit, context)) {
                LOG.warn("Labor posting skipped or failed for visit {}", visit.getCode());
            }
        }
    }
}
