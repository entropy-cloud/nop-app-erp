package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurReceiveDocumentStateMachine;
import app.erp.common.service.AbstractCancelProcessor;
import app.erp.qa.biz.IErpQaInspectionBiz;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ErpPurReceive cancel per-mutation Processor (plan 2026-07-25-1057-2；
 * StateMachine 接线 plan 2026-08-13-0810-1 M4.13)。
 * Overrides the public cancel method to replicate the facade flow (stock move reversal if approved + doc status),
 * calling facade helper methods for each step. Downstream can override via Delta beans.xml with same bean id.
 *
 * <p>固定来源态/目标态判断委托 {@link ErpPurReceiveDocumentStateMachine}（docStatus 业务生命周期轴 Bean，契约 §4/§7）。
 * 非法边映射：Bean 抛 common 层 {@code ERR_ILLEGAL_STATUS_TRANSITION}（含 {@code action=cancel}/
 * {@code fromStatus} 元数据）作 cause，{@link #validateTransitionForCancel} 捕获后映射领域码
 * {@link ErpPurErrors#ERR_ILLEGAL_DOC_STATUS_TRANSITION}（泛型命名漂移，路线图 Non-Goal 不重命名；
 * {@code receiveCode}/{@code currentDocStatus}/{@code expectedDocStatus} 参数对外不变）。
 */
public class ErpPurReceiveCancelProcessor extends AbstractCancelProcessor<ErpPurReceive> {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPurReceiveCancelProcessor.class);

    @Inject
    ErpPurReceiveProcessor processor;

    @Inject
    ErpPurReceiveDocumentStateMachine stateMachine;

    @Inject
    IErpQaInspectionBiz inspectionBiz;

    @Override
    public ErpPurReceive cancel(String id, IServiceContext context) {
        ErpPurReceive receive = requireEntity(id);
        validateTransitionForCancel(receive, context);
        if (receive.isApproved()) {
            processor.ensureReversed(receive, context);
            receive = dao().getEntityById(id);
        }
        setDocStatus(receive, cancelledDocStatus());
        dao().updateEntity(receive);
        cancelLinkedInspections(receive, context);
        return receive;
    }

    /**
     * 作废联动取消质检（RC-R1.59 UC-QA-08，config-gated 在 Facade 内）：作废成功后置调
     * {@code cancelForBusinessBill}（仅软删 PENDING，终态不动，历史完整）。失败 LOG.warn 降级不阻断作废主流程
     * （联动为辅助语义，业务作废不受 quality 故障影响）。billType 用本域创建路径同源常量
     * {@code RELATED_BILL_TYPE_PUR_RECEIVE}（"ERP_PUR_RECEIVE"，与强制质检触发写入值一致，非 qa 域
     * ERP_PUR_RECEIPT 镜像值）。
     */
    protected void cancelLinkedInspections(ErpPurReceive receive, IServiceContext context) {
        try {
            inspectionBiz.cancelForBusinessBill(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE,
                    receive.getCode(), context);
        } catch (Exception e) {
            LOG.warn("入库单作废联动取消质检失败（降级不阻断）：receiveCode={}, reason={}",
                    receive.getCode(), e.getMessage());
        }
    }

    @Override
    protected IEntityDao<ErpPurReceive> dao() {
        return daoProvider.daoFor(ErpPurReceive.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpPurErrors.ERR_RECEIVE_NOT_FOUND)
                .param(ErpPurErrors.ARG_RECEIVE_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpPurReceive entity, String current, String... expected) {
        return new NopException(ErpPurErrors.ERR_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpPurErrors.ARG_RECEIVE_CODE, entity.getCode())
                .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void validateTransitionForCancel(ErpPurReceive entity, IServiceContext context) {
        try {
            stateMachine.assertCanCancel(entity.getDocStatus());
        } catch (NopException e) {
            throw illegalStatusException(entity, entity.getDocStatus(), "非已作废");
        }
    }

    @Override
    protected String getDocStatus(ErpPurReceive entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpPurReceive entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return stateMachine.cancelTargetStatus();
    }
}
