package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.statemachine.ErpMntRequestStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;

/**
 * ErpMntVisit complete per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 IN_PROGRESS→COMPLETED 编排：状态守卫 + 状态翻转 + endTime/totalMinutes/completedAt 计算 + 落库 + 设备状态恢复
 * + [会计保护区域] 维修工时费用化 GL 过账（Dr: 折旧费用 6602 / Cr: 应付职工薪酬 2211），config-gated 默认关
 * + [RC-R1.75 / UC-MAIN-05] 关联请求写回（visit.requestId 非空时经既有状态机合法边置 COMPLETED，见 {@link #completeLinkedRequest}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntVisitProcessor}。
 *
 * <p>[会计保护区域] GL 过账惯法逐字搬运自原 ErpMntVisitBizModel.doComplete：config-gating（isPostingEnabled）+
 * 失败不阻断 complete 终态（吞异常告警），对照 TestErpMntLaborPosting 校验语义不变。
 */
public class ErpMntVisitCompleteProcessor extends AbstractErpMntVisitProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMntVisitCompleteProcessor.class);

    @Inject
    ErpMntRequestStateMachine requestStateMachine;

    public ErpMntVisit complete(Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        String from = visit.getStatus();
        try {
            stateMachine.assertCanComplete(from);
        } catch (NopException e) {
            throw illegalVisitTransition(visit, from, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, e);
        }
        doComplete(visit, context);
        equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context);
        completeLinkedRequest(visit, context);
        return visit;
    }

    protected void doComplete(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(stateMachine.completeTargetStatus());
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

    /**
     * RC-R1.75 / UC-MAIN-05（D6）visit→request 完成侧写回：requestId 非空时读 request——
     * IN_PROGRESS → 经既有 complete 边置 COMPLETED；ACCEPTED → 先 startRepair 后 complete 合成迁移
     * （两条均为既有合法边，不加新边不改状态机契约）；REJECTED/CANCELLED/COMPLETED 终态 → no-op
     * LOG.warn 不阻断访问完成（幂等 + 容忍请求侧独立关闭）；PLANNED 访问（requestId null）零影响。
     * 写回失败（乐观锁冲突等）异常自然传播回滚 visit complete（联动为 L1 硬语义非 best-effort，
     * 区别于 R1.59 辅助语义降级先例）。visit cancel 对 request 无动作（L1 无断言）。
     */
    protected void completeLinkedRequest(ErpMntVisit visit, IServiceContext context) {
        if (visit.getRequestId() == null) {
            return;
        }
        ErpMntRequest request = daoProvider.daoFor(ErpMntRequest.class).getEntityById(visit.getRequestId());
        if (request == null) {
            LOG.warn("Linked request {} missing for visit {}, skip writeback", visit.getRequestId(), visit.getCode());
            return;
        }
        String status = request.getStatus();
        if (requestStateMachine.isTerminal(status)) {
            LOG.warn("Linked request {} already terminal ({}), skip writeback", request.getCode(), status);
            return;
        }
        if (ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED.equals(status)) {
            try {
                requestStateMachine.assertCanStartRepair(status);
            } catch (NopException e) {
                throw illegalLinkedRequestTransition(request, status,
                        ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, e);
            }
            request.setStatus(requestStateMachine.startRepairTargetStatus());
            daoProvider.daoFor(ErpMntRequest.class).updateEntity(request);
            status = request.getStatus();
        }
        try {
            requestStateMachine.assertCanComplete(status);
        } catch (NopException e) {
            throw illegalLinkedRequestTransition(request, status,
                    ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, e);
        }
        request.setStatus(requestStateMachine.completeTargetStatus());
        request.setCompletedAt(CoreMetrics.currentTimestamp());
        daoProvider.daoFor(ErpMntRequest.class).updateEntity(request);
    }

    private NopException illegalLinkedRequestTransition(ErpMntRequest request, String current,
                                                        String expected, Throwable cause) {
        return new NopException(ErpMntErrors.ERR_INVALID_REQUEST_STATUS_TRANSITION, cause)
                .param(ErpMntErrors.ARG_REQUEST_CODE, request.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
