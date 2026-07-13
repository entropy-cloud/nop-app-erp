
package app.erp.b2b.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
import app.erp.b2b.service.ErpB2bConfigs;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.spi.ErpB2bEdiRegistry;
import app.erp.b2b.service.spi.IErpB2bEdiProvider;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.and;
import io.nop.biz.crud.EntityData;

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
 */
@BizModel("ErpB2bEdiDoc")
public class ErpB2bEdiDocBizModel extends CrudBizModel<ErpB2bEdiDoc> implements IErpB2bEdiDocBiz {
    private static final Logger LOG = LoggerFactory.getLogger(ErpB2bEdiDocBizModel.class);

    @Inject
    ErpB2bEdiRegistry ediRegistry;

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
    @SingleSession
    public ErpB2bEdiDoc createOutbound(@Name("relatedBillType") String relatedBillType,
                                       @Name("relatedBillCode") String relatedBillCode,
                                       IServiceContext context) {
        List<IErpB2bEdiProvider> providers = ediRegistry.findOutboundProviders(relatedBillType);
        if (providers.isEmpty()) {
            LOG.info("无适用出站 EDI 格式：relatedBillType={} relatedBillCode={}（静默跳过）", relatedBillType, relatedBillCode);
            return null;
        }

        IErpB2bEdiProvider provider = providers.get(0);
        ErpB2bEdiFormat format = findFormatByCode(provider.getCode());
        if (format == null) {
            LOG.warn("EDI 格式配置记录不存在：code={}（跳过）", provider.getCode());
            return null;
        }

        checkDuplicate(format.getId(), relatedBillType, relatedBillCode);

        String payload = provider.generatePayload(relatedBillType, relatedBillCode);

        ErpB2bEdiDoc doc = newEntity();
        doc.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        doc.setCode("EDI-OUT-" + CoreMetrics.currentTimeMillis());
        doc.setFormatId(format.getId());
        doc.setRelatedBillType(relatedBillType);
        doc.setRelatedBillCode(relatedBillCode);
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        doc.setRetryCount(0);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveEntity(doc);

        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "SEND: 生成出站 EDI 报文，待发送", payload, null);
        return doc;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bEdiDoc markSent(@Name("ediDocId") Long ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String state = doc.getState();
        if (!ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(state)) {
            throw illegalTransition(doc, state, ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
        }
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_SENT);
        doc.setSentAt(CoreMetrics.currentDateTime());
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "SEND: 报文已发送", null, null);
        return doc;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bEdiDoc markAcknowledged(@Name("ediDocId") Long ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String state = doc.getState();
        if (!ErpB2bConstants.EDI_DOC_STATE_SENT.equals(state)) {
            throw illegalTransition(doc, state, ErpB2bConstants.EDI_DOC_STATE_SENT);
        }
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED);
        doc.setAcknowledgedAt(CoreMetrics.currentDateTime());
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "ACKNOWLEDGE: 对方已确认", null, null);
        return doc;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bEdiDoc markError(@Name("ediDocId") Long ediDocId,
                                  @Name("error") String error,
                                  IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_ERROR);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_ERROR);
        doc.setError(error);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, doc.getRelatedBillType() != null ? ErpB2bConstants.DIRECTION_OUTBOUND : ErpB2bConstants.DIRECTION_INBOUND,
                ErpB2bConstants.EDI_RESULT_ERROR, "ERROR: " + error, null, error);
        return doc;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bEdiDoc retry(@Name("ediDocId") Long ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String state = doc.getState();
        if (!ErpB2bConstants.EDI_DOC_STATE_ERROR.equals(state)) {
            throw illegalTransition(doc, state, ErpB2bConstants.EDI_DOC_STATE_ERROR);
        }
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
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
    @SingleSession
    public ErpB2bEdiDoc cancel(@Name("ediDocId") Long ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String state = doc.getState();
        if (!ErpB2bConstants.EDI_DOC_STATE_TO_SEND.equals(state)
                && !ErpB2bConstants.EDI_DOC_STATE_SENT.equals(state)
                && !ErpB2bConstants.EDI_DOC_STATE_ERROR.equals(state)) {
            throw illegalTransition(doc, state, "TO_SEND/SENT/ERROR");
        }
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_CANCELLED);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "CANCEL: 已取消", null, null);
        return doc;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bEdiDoc createInbound(@Name("relatedBillType") String relatedBillType,
                                      @Name("relatedBillCode") String relatedBillCode,
                                      @Name("rawPayload") String rawPayload,
                                      @Name("formatCode") String formatCode,
                                      IServiceContext context) {
        ErpB2bEdiFormat format = findFormatByCode(formatCode);
        if (format != null) {
            checkDuplicate(format.getId(), relatedBillType, relatedBillCode);
        }

        ErpB2bEdiDoc doc = newEntity();
        doc.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        doc.setCode("EDI-IN-" + CoreMetrics.currentTimeMillis());
        doc.setFormatId(format != null ? format.getId() : null);
        doc.setRelatedBillType(relatedBillType);
        doc.setRelatedBillCode(relatedBillCode);
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        doc.setRetryCount(0);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveEntity(doc);

        writeLog(doc, ErpB2bConstants.DIRECTION_INBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "RECEIVE: 收到入站报文", rawPayload, null);
        return doc;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bEdiDoc archive(@Name("ediDocId") Long ediDocId, IServiceContext context) {
        ErpB2bEdiDoc doc = requireDoc(ediDocId);
        String state = doc.getState();
        if (!ErpB2bConstants.EDI_DOC_STATE_RECEIVED.equals(state)) {
            throw illegalTransition(doc, state, ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
        }
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_ARCHIVED);
        daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(doc);
        writeLog(doc, ErpB2bConstants.DIRECTION_INBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "ARCHIVE: 入站处理完成，已归档", null, null);
        return doc;
    }

    // ---------- helpers ----------

    ErpB2bEdiDoc requireDoc(Long ediDocId) {
        ErpB2bEdiDoc doc = daoProvider().daoFor(ErpB2bEdiDoc.class).getEntityById(ediDocId);
        if (doc == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_EDI_DOC_ID, ediDocId);
        }
        return doc;
    }

    private void checkDuplicate(Long formatId, String relatedBillType, String relatedBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("formatId", formatId),
                eq("relatedBillType", relatedBillType),
                eq("relatedBillCode", relatedBillCode)));
        // O-5：追加 id DESC 确保确定性
        q.addOrderField("id", true);
        ErpB2bEdiDoc existing = daoProvider().daoFor(ErpB2bEdiDoc.class).findFirstByQuery(q);
        if (existing != null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_DOC_ALREADY_PROCESSED)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, relatedBillType)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_CODE, relatedBillCode);
        }
    }

    private ErpB2bEdiFormat findFormatByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        // O-5：追加 code 排序确保确定性
        q.addOrderField("code", false);
        return daoProvider().daoFor(ErpB2bEdiFormat.class).findFirstByQuery(q);
    }

    private NopException illegalTransition(ErpB2bEdiDoc doc, String current, String expected) {
        return new NopException(ErpB2bErrors.ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION)
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
        log.setLogTime(CoreMetrics.currentDateTime());
        dao.saveEntity(log);
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bEdiDoc.class)
    public List<String> formatName(@ContextSource List<ErpB2bEdiDoc> rows) {
        orm().batchLoadProps(rows, Collections.singleton("format"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bEdiDoc row : rows) {
            result.add(row.orm_attached() && row.getFormat() != null ? row.getFormat().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpB2bEdiDoc.class)
    public List<String> orgName(@ContextSource List<ErpB2bEdiDoc> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bEdiDoc row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
