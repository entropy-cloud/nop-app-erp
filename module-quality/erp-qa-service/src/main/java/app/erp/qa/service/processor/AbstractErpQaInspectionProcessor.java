package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.statemachine.ErpQaInspectionResultStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 质检单 per-mutation Processor 共享基类（R6.6）。承载 recordResult/passInspection/failInspection/
 * createForBusinessBill 四个 per-mutation Processor 共用的加载、状态守卫与 posted 簿记辅助（单一真相源，对齐
 * {@code processor-extension-pattern.md} facade protected helper 范式）。子类只编排单 mutation 步骤顺序。
 *
 * <p>result 轴固定来源态/目标态判断委托 {@link ErpQaInspectionResultStateMachine}（实体级状态机 Bean，
 * 契约 §4/§7）；动态业务守卫（行级评测、posted 簿记、NCR auto-create）保留原位。
 */
public abstract class AbstractErpQaInspectionProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpQaInspectionResultStateMachine resultStateMachine;

    protected IEntityDao<ErpQaInspection> inspectionDao() {
        return daoProvider.daoFor(ErpQaInspection.class);
    }

    protected IEntityDao<ErpQaInspectionLine> lineDao() {
        return daoProvider.daoFor(ErpQaInspectionLine.class);
    }

    protected ErpQaInspection requireInspection(Long inspectionId, IServiceContext context) {
        if (inspectionId == null) {
            throw new NopException(ErpQaErrors.ERR_INSPECTION_NOT_FOUND)
                    .param(ErpQaErrors.ARG_INSPECTION_ID, inspectionId);
        }
        ErpQaInspection inspection = inspectionDao().getEntityById(inspectionId);
        if (inspection == null) {
            throw new NopException(ErpQaErrors.ERR_INSPECTION_NOT_FOUND)
                    .param(ErpQaErrors.ARG_INSPECTION_ID, inspectionId);
        }
        return inspection;
    }

    protected List<ErpQaInspectionLine> loadLines(Long inspectionId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("inspectionId", inspectionId));
        q.addOrderField("lineNo", false);
        return lineDao().findAllByQuery(q);
    }

    protected NopException illegalInspectionTransition(ErpQaInspection ins, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_INSPECTION_CODE, ins.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected void markPosted(ErpQaInspection inspection, IServiceContext context) {
        inspection.setPosted(Boolean.TRUE);
        inspection.setPostedAt(CoreMetrics.currentTimestamp());
        inspection.setPostedBy(context.getUserId());
    }
}
