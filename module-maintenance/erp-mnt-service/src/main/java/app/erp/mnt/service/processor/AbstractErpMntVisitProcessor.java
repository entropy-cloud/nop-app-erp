package app.erp.mnt.service.processor;

import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.posting.MaintenanceLaborPostingDispatcher;
import app.erp.mnt.service.statemachine.ErpMntVisitStateMachine;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * 维护访问 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 schedule/start/complete/cancel 四个 per-mutation Processor 共用的加载、状态守卫辅助（单一真相源）。
 * complete/cancel 含维修工时费用化 GL 过账（会计保护区域），其 config-gating / try-catch / session-reload 惯法逐字保留在
 * 各子类 doComplete/doCancel 内（对照 TestErpMntLaborPosting 校验语义不变）。
 *
 * <p>状态守卫经实体级 {@link ErpMntVisitStateMachine} Bean（契约 §5）：各 Processor 直接调用 {@code assertCanXxx}，
 * common 层非法迁移码经 {@link #illegalVisitTransition(ErpMntVisit, String, String, Throwable)} cause-chain 映射为领域
 * {@code ERR_INVALID_VISIT_STATUS_TRANSITION}（契约 §7）。目标态经 {@code *TargetStatus()}。
 */
public abstract class AbstractErpMntVisitProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EquipmentStatusLinker equipmentStatusLinker;

    @Inject
    MaintenanceLaborPostingDispatcher laborPostingDispatcher;

    @Inject
    ErpMntVisitStateMachine stateMachine;

    protected IEntityDao<ErpMntVisit> visitDao() {
        return daoProvider.daoFor(ErpMntVisit.class);
    }

    protected ErpMntVisit requireVisit(Long visitId, IServiceContext context) {
        ErpMntVisit visit = visitDao().getEntityById(visitId);
        if (visit == null) {
            throw new NopException(ErpMntErrors.ERR_VISIT_NOT_FOUND).param(ErpMntErrors.ARG_VISIT_ID, visitId);
        }
        return visit;
    }

    protected NopException illegalVisitTransition(ErpMntVisit visit, String current, String expected) {
        return illegalVisitTransition(visit, current, expected, null);
    }

    /**
     * 非法迁移领域异常（带 cause）：StateMachine Bean 报告 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}，
     * 此处映射为领域 {@code ERR_INVALID_VISIT_STATUS_TRANSITION} + 实体编号/上下文，common 码作 cause 保留（契约 §7）。
     */
    protected NopException illegalVisitTransition(ErpMntVisit visit, String current, String expected, Throwable cause) {
        return new NopException(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION, cause)
                .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
