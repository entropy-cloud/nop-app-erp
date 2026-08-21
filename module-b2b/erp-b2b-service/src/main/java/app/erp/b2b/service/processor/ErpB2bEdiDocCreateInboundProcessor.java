package app.erp.b2b.service.processor;

import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpB2bEdiDoc createInbound per-mutation Processor。
 * 自包含入站报文创建编排：查 EdiFormat（可空）→ 防重守门 → 建 RECEIVED 信封 + 写 EdiLog。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpB2bEdiDocCreateInboundProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpB2bEdiDoc createInbound(String relatedBillType, String relatedBillCode, String rawPayload,
                                      String formatCode, IServiceContext context) {
        ErpB2bEdiFormat format = findFormatByCode(formatCode);
        if (format != null) {
            checkDuplicate(format.getId(), relatedBillType, relatedBillCode);
        }

        ErpB2bEdiDoc doc = dao().newEntity();
        doc.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        doc.setCode("EDI-IN-" + CoreMetrics.currentTimeMillis());
        doc.setFormatId(format != null ? format.getId() : null);
        doc.setRelatedBillType(relatedBillType);
        doc.setRelatedBillCode(relatedBillCode);
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_RECEIVED);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        doc.setRetryCount(0);
        daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);

        writeLog(doc, ErpB2bConstants.DIRECTION_INBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "RECEIVE: 收到入站报文", rawPayload, null);
        return doc;
    }

    // ---------- 内部辅助 ----------

    protected void checkDuplicate(String formatId, String relatedBillType, String relatedBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("formatId", formatId),
                eq("relatedBillType", relatedBillType),
                eq("relatedBillCode", relatedBillCode)));
        // O-5：追加 id DESC 确保确定性
        q.addOrderField("id", true);
        ErpB2bEdiDoc existing = daoProvider.daoFor(ErpB2bEdiDoc.class).findFirstByQuery(q);
        if (existing != null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_EDI_DOC_ALREADY_PROCESSED)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_TYPE, relatedBillType)
                    .param(ErpB2bErrors.ARG_RELATED_BILL_CODE, relatedBillCode);
        }
    }

    protected ErpB2bEdiFormat findFormatByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        // O-5：追加 code 排序确保确定性
        q.addOrderField("code", false);
        return daoProvider.daoFor(ErpB2bEdiFormat.class).findFirstByQuery(q);
    }

    protected void writeLog(ErpB2bEdiDoc doc, String direction, String resultCode, String resultMsg,
                            String requestPayload, String responsePayload) {
        IEntityDao<ErpB2bEdiLog> dao = daoProvider.daoFor(ErpB2bEdiLog.class);
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

    private IEntityDao<ErpB2bEdiDoc> dao() {
        return daoProvider.daoFor(ErpB2bEdiDoc.class);
    }
}
