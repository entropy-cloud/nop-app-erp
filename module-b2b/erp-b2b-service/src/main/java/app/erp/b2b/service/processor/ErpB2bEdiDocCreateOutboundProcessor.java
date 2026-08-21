package app.erp.b2b.service.processor;

import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.spi.ErpB2bEdiRegistry;
import app.erp.b2b.service.spi.IErpB2bEdiProvider;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpB2bEdiDoc createOutbound per-mutation Processor。
 * 自包含出站报文创建编排：Registry 派发 Provider → 查 EdiFormat → 防重守门 → 生成 payload → 建 TO_SEND 信封 + 写 EdiLog。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpB2bEdiDocCreateOutboundProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ErpB2bEdiDocCreateOutboundProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpB2bEdiRegistry ediRegistry;

    public ErpB2bEdiDoc createOutbound(String relatedBillType, String relatedBillCode, IServiceContext context) {
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

        ErpB2bEdiDoc doc = dao().newEntity();
        doc.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        doc.setCode("EDI-OUT-" + CoreMetrics.currentTimeMillis());
        doc.setFormatId(format.getId());
        doc.setRelatedBillType(relatedBillType);
        doc.setRelatedBillCode(relatedBillCode);
        doc.setState(ErpB2bConstants.EDI_DOC_STATE_TO_SEND);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        doc.setRetryCount(0);
        daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);

        writeLog(doc, ErpB2bConstants.DIRECTION_OUTBOUND, ErpB2bConstants.EDI_RESULT_SUCCESS,
                "SEND: 生成出站 EDI 报文，待发送", payload, null);
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
