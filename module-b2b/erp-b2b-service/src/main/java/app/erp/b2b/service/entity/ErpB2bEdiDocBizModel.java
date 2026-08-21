
package app.erp.b2b.service.entity;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.processor.ErpB2bEdiDocCreateInboundProcessor;
import app.erp.b2b.service.processor.ErpB2bEdiDocCreateOutboundProcessor;
import app.erp.b2b.service.statemachine.ErpB2bEdiDocStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * EDI 事务信封聚合根 Biz。承载 EDI 信封状态机（{@code edi-formats.md §七}）：
 *
 * <p>出站：{@link #createOutbound}→TO_SEND→{@link #markSent}→SENT→{@link #markAcknowledged}→ACKNOWLEDGED（终态）；
 * 失败 {@link #markError}→ERROR→{@link #retry}→TO_SEND；取消 {@link #cancel}→CANCELLED（终态）。
 *
 * <p>入站：{@link #createInbound}→RECEIVED→{@link #archive}→ARCHIVED（终态）。
 *
 * <p>每次迁移写 {@link ErpB2bEdiLog}（动作语义编码到 {@code direction}+{@code resultCode}+{@code resultMsg}，
 * 不新增列——{@code ErpB2bEdiLog} 无 actionType/httpStatus 列，design {@code edi-formats.md §8.1} 列出但 ORM 未落地）。
 * {@code UNIQUE(formatId,relatedBillType,relatedBillCode)} 守门防重。
 *
 * <p>固定来源/目标态判断经 {@link ErpB2bEdiDocStateMachine} Bean（契约 {@code entity-state-machine-bean.md}）。
 * Bean 抛 common 层非法迁移码，本类映射为 {@link ErpB2bErrors#ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION}（参数不变，common 码作 cause）。
 * 动态守卫保留原位：retry 的 retryCount++/error/blockingLevel 清除、sentAt/acknowledgedAt 写入、markError 的 error msg。
 */
@BizModel("ErpB2bEdiDoc")
public class ErpB2bEdiDocBizModel extends CrudBizModel<ErpB2bEdiDoc> implements IErpB2bEdiDocBiz {

    @Inject
    ErpB2bEdiDocCreateOutboundProcessor createOutboundProcessor;
    @Inject
    ErpB2bEdiDocCreateInboundProcessor createInboundProcessor;
    @Inject
    ErpB2bEdiDocStateMachine stateMachine;

    public ErpB2bEdiDocBizModel() {
        setEntityName(ErpB2bEdiDoc.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpB2bEdiDoc> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpB2bEdiDoc entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc createOutbound(@Name("relatedBillType") String relatedBillType,
                                       @Name("relatedBillCode") String relatedBillCode,
                                       IServiceContext context) {
        return createOutboundProcessor.createOutbound(relatedBillType, relatedBillCode, context);
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc markSent(@Name("ediDocId") String ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String from = doc.getState();
        assertCan("markSent", doc, from, ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
        doc.setState(stateMachine.markSentTargetStatus());
        doc.setSentAt(CoreMetrics.currentTimestamp());
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "SEND: 报文已发送", null, null);
        return doc;
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc markAcknowledged(@Name("ediDocId") String ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String from = doc.getState();
        assertCan("markAcknowledged", doc, from, ErpB2bConstants.EDI_DOC_STATE_SENT);
        doc.setState(stateMachine.markAcknowledgedTargetStatus());
        doc.setAcknowledgedAt(CoreMetrics.currentTimestamp());
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "ACKNOWLEDGE: 对方已确认", null, null);
        return doc;
    }

    /**
     * D-B2B-3 Fix：新增 Bean 守卫收紧来源至 {TO_SEND, SENT, RECEIVED}（原生产代码无守卫允许任意态→ERROR）。
     * 经层 2 四方对照裁定为 implementation drift → Fix。终态/ERROR markError 现抛领域码。
     * 保留 blockingLevel=ERROR + error msg 副作用（动态守卫原位）。
     */
    @Override
    @BizMutation
    public ErpB2bEdiDoc markError(@Name("ediDocId") String ediDocId,
                                  @Name("error") String error,
                                  IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String from = doc.getState();
        assertCan("markError", doc, from, "TO_SEND/SENT/RECEIVED");
        doc.setState(stateMachine.markErrorTargetStatus());
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_ERROR);
        doc.setError(error);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, doc.getRelatedBillType() != null ? ErpB2bConstants.DIRECTION_OUTBOUND : ErpB2bConstants.DIRECTION_INBOUND,
                ErpB2bConstants.EDI_RESULT_ERROR, "ERROR: " + error, null, error);
        return doc;
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc retry(@Name("ediDocId") String ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String from = doc.getState();
        assertCan("retry", doc, from, ErpB2bConstants.EDI_DOC_STATE_ERROR);
        // 当前实现出站 retry（ERROR→TO_SEND）。入站 ERROR→RECEIVED 路径为 owner doc §2 Deferred successor（D-B2B-6）。
        doc.setState(stateMachine.retryOutboundTargetStatus());
        doc.setRetryCount((doc.getRetryCount() != null ? doc.getRetryCount() : 0) + 1);
        doc.setError(null);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "RETRY: 从 ERROR 恢复到 TO_SEND", null, null);
        return doc;
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc cancel(@Name("ediDocId") String ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String from = doc.getState();
        assertCan("cancel", doc, from, "TO_SEND/SENT/ERROR");
        doc.setState(stateMachine.cancelTargetStatus());
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "CANCEL: 已取消", null, null);
        return doc;
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc createInbound(@Name("relatedBillType") String relatedBillType,
                                      @Name("relatedBillCode") String relatedBillCode,
                                      @Name("rawPayload") String rawPayload,
                                      @Name("formatCode") String formatCode,
                                      IServiceContext context) {
        return createInboundProcessor.createInbound(relatedBillType, relatedBillCode, rawPayload, formatCode, context);
    }

    @Override
    @BizMutation
    public ErpB2bEdiDoc archive(@Name("ediDocId") String ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String from = doc.getState();
        assertCan("archive", doc, from, ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
        doc.setState(stateMachine.archiveTargetStatus());
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_INBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "ARCHIVE: 入站处理完成，已归档", null, null);
        return doc;
    }

    // ---------- helpers ----------

    ErpB2bEdiDoc requireDoc(String ediDocId) {
        ErpB2bEdiDoc doc = daoProvider().daoFor(ErpB2bEdiDoc.class).getEntityById(ediDocId);
        if (doc == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_EDI_DOC_ID, ediDocId);
        }
        return doc;
    }

    /**
     * 经 StateMachine Bean 断言来源态合法；非法边（Bean 报告 common 层码）映射为领域
     * {@code ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION} + 实体编号/上下文，common 码作 cause 保留（契约 §7）。
     */
    private void assertCan(String action, ErpB2bEdiDoc doc, String from, String expected) {
        try {
            switch (action) {
                case "markSent":
                    stateMachine.assertCanMarkSent(from);
                    break;
                case "markAcknowledged":
                    stateMachine.assertCanMarkAcknowledged(from);
                    break;
                case "markError":
                    stateMachine.assertCanMarkError(from);
                    break;
                case "retry":
                    stateMachine.assertCanRetry(from);
                    break;
                case "cancel":
                    stateMachine.assertCanCancel(from);
                    break;
                case "archive":
                    stateMachine.assertCanArchive(from);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected action: " + action);
            }
        } catch (NopException e) {
            throw illegalTransition(doc, from, expected, e);
        }
    }

    private NopException illegalTransition(ErpB2bEdiDoc doc, String current, String expected) {
        return illegalTransition(doc, current, expected, null);
    }

    private NopException illegalTransition(ErpB2bEdiDoc doc, String current, String expected, Throwable cause) {
        return new NopException(ErpB2bErrors.ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION, cause)
                .param(ErpB2bErrors.ARG_EDI_DOC_CODE, doc.getCode())
                .param(ErpB2bErrors.ARG_CURRENT_STATE, current)
                .param(ErpB2bErrors.ARG_EXPECTED_STATE, expected);
    }

    void writeLog(ErpB2bEdiDoc doc, String direction, String resultCode, String resultMsg,
                  String requestPayload, String responsePayload) {
        IEntityDao<ErpB2bEdiLog> dao = daoProvider().daoFor(ErpB2bEdiLog.class);
        ErpB2bEdiLog log = dao.newEntity();
        log.setEdiDocId(doc.getId());
        log.setOrgId(doc.getOrgId());
        log.setDirection(direction);
        log.setRequestPayload(requestPayload);
        log.setResponsePayload(responsePayload);
        log.setResultCode(resultCode);
        log.setResultMsg(resultMsg);
        log.setLogTime(CoreMetrics.currentTimestamp());
        dao.saveEntity(log);
    }

}
