package app.erp.mnt.service.processor;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.ErpMntErrors;
import app.erp.mnt.service.posting.MaintenanceLaborPostingDispatcher;
import app.erp.mnt.service.support.EquipmentStatusLinker;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * 维护访问 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 schedule/start/complete/cancel 四个 per-mutation Processor 共用的加载、状态守卫辅助（单一真相源）。
 * complete/cancel 含维修工时费用化 GL 过账（会计保护区域），其 config-gating / try-catch / session-reload 惯法逐字保留在
 * 各子类 doComplete/doCancel 内（对照 TestErpMntLaborPosting 校验语义不变）。
 */
public abstract class AbstractErpMntVisitProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    EquipmentStatusLinker equipmentStatusLinker;

    @Inject
    MaintenanceLaborPostingDispatcher laborPostingDispatcher;

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

    protected void validateTransition(ErpMntVisit visit, String expected, String expectedName) {
        String status = visit.getStatus();
        if (status == null || !Objects.equals(status, expected)) {
            throw illegalVisitTransition(visit, status, expectedName);
        }
    }

    protected void validateNotTerminal(ErpMntVisit visit, IServiceContext context) {
        String status = visit.getStatus();
        if (status != null && (Objects.equals(status, ErpMntDaoConstants.VISIT_STATUS_COMPLETED)
                || Objects.equals(status, ErpMntDaoConstants.VISIT_STATUS_CANCELLED))) {
            throw illegalVisitTransition(visit, status, "非终态");
        }
    }

    protected NopException illegalVisitTransition(ErpMntVisit visit, String current, String expected) {
        return new NopException(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION)
                .param(ErpMntErrors.ARG_VISIT_CODE, visit.getCode())
                .param(ErpMntErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMntErrors.ARG_EXPECTED_STATUS, expected);
    }
}
