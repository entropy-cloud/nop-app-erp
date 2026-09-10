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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMntVisit）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 维护访问 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 schedule/start/complete/cancel 四个 per-mutation Processor 共用的加载、状态守卫辅助（单一真相源）。
 * complete/cancel 含维修工时费用化 GL 过账（会计保护区域），其 config-gating / try-catch / session-reload 惯法逐字保留在
 * 各子类 doComplete/doCancel 内（对照 TestErpMntLaborPosting 校验语义不变）。
 *
 * <p>状态守卫经实体级 {@link ErpMntVisitStateMachine} Bean（契约 §5）：各 Processor 直接调用 {@code assertCanXxx}，
 * Bean 自 plan 2026-09-07-2200-1 起直抛领域码 {@code ERR_INVALID_VISIT_STATUS_TRANSITION}（非法边 catch 同码补参
 * {@code visitCode}）。目标态经 {@code *TargetStatus()}。
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

    protected ErpMntVisit requireVisit(String visitId, IServiceContext context) {
        ErpMntVisit visit = visitDao().getEntityById(visitId);
        if (visit == null) {
            throw new NopException(ErpMntErrors.ERR_VISIT_NOT_FOUND).param(ErpMntErrors.ARG_VISIT_ID, visitId);
        }
        return visit;
    }

    protected NopException illegalVisitTransition(ErpMntVisit visit, String current, String expected) {
        return new NopException(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION)
                .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
