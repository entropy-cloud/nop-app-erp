package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ErpMntVisit cancel per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含非终态→CANCELLED 编排：非终态守卫 + 状态翻转 + 落库 + 设备状态恢复
 * + [会计保护区域] 维修工时费用化 GL 红冲（cancel 时已生成 MAINTENANCE_LABOR 凭证则红冲），config-gated 与正向对称。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntVisitProcessor}。
 *
 * <p>[会计保护区域] GL 红冲惯法逐字搬运自原 ErpMntVisitBizModel.doCancel：config-gating（isPostingEnabled）+
 * try-catch 吞异常告警保持 cancel 终态不阻断，对照 TestErpMntLaborPosting 校验语义不变。
 */
public class ErpMntVisitCancelProcessor extends AbstractErpMntVisitProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpMntVisitCancelProcessor.class);

    public ErpMntVisit cancel(Long visitId, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        validateNotTerminal(visit, context);
        doCancel(visit, context);
        equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context);
        return visit;
    }

    protected void doCancel(ErpMntVisit visit, IServiceContext context) {
        visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_CANCELLED);
        visitDao().updateEntity(visit);

        // 维修工时费用化 GL 红冲（cancel 时已生成 MAINTENANCE_LABOR 凭证则红冲），config-gated 与正向对称
        //（plan 2026-07-18-1745-1）。失败不阻断 cancel 终态（吞异常告警，对齐 doComplete 内 postLabor 失败语义）。
        if (laborPostingDispatcher.isPostingEnabled()) {
            try {
                laborPostingDispatcher.reverseLabor(visit);
            } catch (Exception e) {
                if (e instanceof NopException) {
                    LOG.warn("维修工时费用化红冲失败，访问 {} 保持 CANCELLED 终态（凭证孤儿由人工或兜底处理）：{}",
                            visit.getCode(), e.getMessage());
                } else {
                    LOG.error("维修工时费用化红冲异常，访问 {} 保持 CANCELLED 终态", visit.getCode(), e);
                }
            }
        }
    }
}
