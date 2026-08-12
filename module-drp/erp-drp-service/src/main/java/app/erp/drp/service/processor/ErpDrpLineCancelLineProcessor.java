package app.erp.drp.service.processor;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.service.ErpDrpErrors;
import app.erp.drp.service.statemachine.ErpDrpLineStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpDrpLine cancelLine per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含行取消编排（SUGGESTED/APPROVED→CANCELLED）：回读行 + 终态门（ORDERED/CANCELLED 不可再取消）+ 置 CANCELLED。
 * 与 {@link ErpDrpLineRejectLineProcessor} 同语义（原 BizModel rejectLine/cancelLine 共用 doCancel），按自包含要求各自内联副本。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>接线（plan 2026-08-12-1841-1 Phase 2）：固定来源态/终态守卫经 {@link ErpDrpLineStateMachine}（cancel 多源 SUGGESTED/APPROVED）。
 */
public class ErpDrpLineCancelLineProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ErpDrpLineStateMachine lineStateMachine;

    public ErpDrpLine cancelLine(Long lineId, IServiceContext context) {
        return doCancel(lineId);
    }

    // ---------- 内部辅助 ----------

    protected ErpDrpLine doCancel(Long lineId) {
        ErpDrpLine line = requireLine(lineId);
        // 固定来源态守卫经 Line StateMachine Bean（cancel 多源 SUGGESTED/APPROVED，对终态 ORDERED/CANCELLED 抛 common 码）；
        // 映射为既有 ERR_DRP_LINE_ILLEGAL_TRANSITION（参数 drpLineId/currentStatus 不变，common 层码作 cause）。
        try {
            lineStateMachine.assertCanCancel(line.getStatus());
        } catch (NopException e) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_ILLEGAL_TRANSITION, e)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId)
                    .param(ErpDrpErrors.ARG_CURRENT_STATUS, line.getStatus());
        }
        line.setStatus(lineStateMachine.cancelTargetStatus());
        dao().updateEntity(line);
        return line;
    }

    protected ErpDrpLine requireLine(Long lineId) {
        ErpDrpLine line = dao().getEntityById(lineId);
        if (line == null) {
            throw new NopException(ErpDrpErrors.ERR_DRP_LINE_NOT_FOUND)
                    .param(ErpDrpErrors.ARG_DRP_LINE_ID, lineId);
        }
        return line;
    }

    private IEntityDao<ErpDrpLine> dao() {
        return daoProvider.daoFor(ErpDrpLine.class);
    }
}
