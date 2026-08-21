package app.erp.b2b.service.processor;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiLog;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;
import app.erp.b2b.service.ErpB2bConfigs;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.b2b.service.codemapping.CodeMappingResolver;
import app.erp.b2b.service.spi.ErpB2bEdiRegistry;
import app.erp.b2b.service.spi.IErpB2bEdiProvider;
import app.erp.b2b.service.spi.model.ParsedPayload;
import app.erp.b2b.service.spi.model.ParsedPayload.ParsedLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpB2bAsn handleInboundWebhook per-mutation Processor。
 * 自包含 webhook 入站编排：HMAC 校验 + 幂等 → 解析报文（{@link #parseToAsn}）→ 建 ASN/AsnLine。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpB2bAsnHandleInboundWebhookProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ErpB2bAsnHandleInboundWebhookProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpB2bEdiRegistry ediRegistry;

    @Inject
    CodeMappingResolver codeMappingResolver;

    @Inject
    IErpB2bEdiDocBiz ediDocBiz;

    public String handleInboundWebhook(String formatCode, String partnerCode, String signature,
                                     String eventId, String payload, IServiceContext context) {
        // 1. 查 PartnerProfile → webhookSecret
        ErpB2bPartnerProfile profile = findPartnerProfileByCode(partnerCode);
        if (profile == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_WEBHOOK_SIGNATURE_INVALID)
                    .param(ErpB2bErrors.ARG_PARTNER_CODE, partnerCode);
        }

        // 2. HMAC 校验
        boolean required = AppConfig.var(ErpB2bConfigs.CONFIG_WEBHOOK_SIGNATURE_REQUIRED,
                ErpB2bConfigs.DEFAULT_WEBHOOK_SIGNATURE_REQUIRED);
        if (required && !verifySignature(payload, signature, profile.getWebhookSecret())) {
            throw new NopException(ErpB2bErrors.ERR_B2B_WEBHOOK_SIGNATURE_INVALID)
                    .param(ErpB2bErrors.ARG_PARTNER_CODE, partnerCode);
        }

        // 3. 幂等检查（eventId+formatCode → EdiDoc.remark）
        if (eventId != null && isDuplicateEvent(eventId, formatCode)) {
            throw new NopException(ErpB2bErrors.ERR_B2B_WEBHOOK_DUPLICATE_EVENT)
                    .param(ErpB2bErrors.ARG_EVENT_ID, eventId)
                    .param(ErpB2bErrors.ARG_EDI_FORMAT_CODE, formatCode);
        }

        // 4. 解析 + 建 ASN
        String asnId = parseToAsn(formatCode, payload, profile, eventId, context);
        return asnId;
    }

    // ---------- parseToAsn (内部方法) ----------

    protected String parseToAsn(String formatCode, String payload, ErpB2bPartnerProfile profile,
                              String eventId, IServiceContext context) {
        IErpB2bEdiProvider provider;
        ParsedPayload parsed;
        try {
            provider = ediRegistry.getProvider(formatCode);
            parsed = provider.parsePayload(formatCode, payload);
        } catch (NopException e) {
            // 解析失败：建 EdiDoc(state=ERROR) + 保留 rawPayload
            ErpB2bEdiDoc errorDoc = ediDocBiz.createInbound(
                    ErpB2bConstants.RELATED_BILL_TYPE_ASN_INBOUND, null, payload, formatCode, context);
            ediDocBiz.markError(errorDoc.getId(), e.getMessage(), context);
            writeEdiLog(errorDoc, ErpB2bConstants.DIRECTION_INBOUND, true,
                    payload, e.getMessage());
            throw e;
        }

        // 成功：建 EdiDoc(state=RECEIVED)
        ErpB2bEdiDoc ediDoc = ediDocBiz.createInbound(
                parsed.getRelatedBillType() != null ? parsed.getRelatedBillType() : ErpB2bConstants.RELATED_BILL_TYPE_PO_ORDER,
                parsed.getRelatedBillCode(), payload, formatCode, context);
        if (eventId != null) {
            ediDoc.setRemark("WEBHOOK eventId=" + eventId + " formatCode=" + formatCode);
            daoProvider.daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(ediDoc);
        }

        // 建 ASN
        // 确定性 code（plan 2026-07-30-0841-2 R1.28 P1-MA2-088）：复用既有 (code,orgId) UK 作为重复 webhook 兜底。
        // eventId 非空 → ASN-WEBHOOK-{eventId}（同事件重投命中 UK）；eventId==null → 退化为时间戳保证唯一（避免
        // ASN-WEBHOOK-null 塌缩导致后续 null-eventId webhook 互撞）。
        ErpB2bAsn asn = dao().newEntity();
        asn.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        asn.setCode(eventId != null
                ? "ASN-WEBHOOK-" + eventId
                : "ASN-WEBHOOK-" + CoreMetrics.currentTimeMillis());
        asn.setSourceEdiDocId(ediDoc.getId());
        asn.setPartnerId(profile.getPartnerId());
        asn.setRelatedBillType(ErpB2bConstants.RELATED_BILL_TYPE_PO_ORDER);
        asn.setRelatedBillCode(parsed.getRelatedBillCode());
        asn.setStatus(ErpB2bConstants.ASN_STATUS_RECEIVED);
        asn.setShipmentDate(CoreMetrics.today());
        if (parsed.getHeaders().get("estimatedArrivalDate") instanceof LocalDate) {
            asn.setEstimatedArrivalDate((LocalDate) parsed.getHeaders().get("estimatedArrivalDate"));
        }
        // flush 触发 INSERT，命中 (code,orgId) UK（确定性 code + 并发 TOCTOU 越过 isDuplicateEvent 时）→ 翻译为友好错误码
        try {
            daoProvider.daoFor(ErpB2bAsn.class).saveEntity(asn);
            io.nop.orm.IOrmTemplate ormTemplate =
                    ((io.nop.orm.dao.IOrmEntityDao<?>) dao()).getOrmTemplate();
            ormTemplate.flushSession();
        } catch (Exception e) {
            if (app.erp.common.service.UniqueConstraintHelper.isUniqueConstraintViolation(e)) {
                throw new NopException(ErpB2bErrors.ERR_B2B_WEBHOOK_DUPLICATE_EVENT)
                        .param(ErpB2bErrors.ARG_EVENT_ID, eventId)
                        .param(ErpB2bErrors.ARG_EDI_FORMAT_CODE, formatCode);
            }
            throw e;
        }

        // 建 AsnLine(s) + 代码映射
        IEntityDao<ErpB2bAsnLine> lineDao = daoProvider.daoFor(ErpB2bAsnLine.class);
        int lineNo = 1;
        for (ParsedLine parsedLine : parsed.getLines()) {
            ErpB2bAsnLine line = lineDao.newEntity();
            line.setAsnId(asn.getId());
            line.setLineNo(lineNo++);
            line.setSupplierPartNo(parsedLine.getSupplierPartNo());

            // 代码映射：partnerId + MATERIAL + externalCode → internalCode
            String internalMaterial = codeMappingResolver.resolveInbound(
                    profile.getPartnerId(), ErpB2bConstants.MAPPING_TYPE_MATERIAL, parsedLine.getSupplierPartNo());
            // internalMaterial 为物料 code 字符串，实际 materialId 需查 ErpMdMaterial。
            // 本期保留 code 值到 supplierPartNo + 映射结果到 remark 供后续处理。
            line.setRemark(internalMaterial);

            line.setShippedQty(parsedLine.getShippedQty());
            line.setQuantity(parsedLine.getQuantity());
            lineDao.saveEntity(line);
        }

        writeEdiLog(ediDoc, ErpB2bConstants.DIRECTION_INBOUND, false, payload, null);
        LOG.info("ASN 入站解析成功：asnCode={} partnerCode={} lines={}",
                asn.getCode(), profile.getCode(), parsed.getLines().size());
        return asn.getId();
    }

    // ---------- 内部辅助 ----------

    protected ErpB2bPartnerProfile findPartnerProfileByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        // O-5：追加 id DESC 确保确定性
        q.addOrderField("id", true);
        return daoProvider.daoFor(ErpB2bPartnerProfile.class).findFirstByQuery(q);
    }

    protected boolean isDuplicateEvent(String eventId, String formatCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("remark", "WEBHOOK eventId=" + eventId + " formatCode=" + formatCode));
        // O-5：追加 id DESC 确保确定性
        q.addOrderField("id", true);
        return daoProvider.daoFor(ErpB2bEdiDoc.class).findFirstByQuery(q) != null;
    }

    protected void writeEdiLog(ErpB2bEdiDoc doc, String direction, boolean error,
                               String requestPayload, String errorMsg) {
        IEntityDao<ErpB2bEdiLog> dao = daoProvider.daoFor(ErpB2bEdiLog.class);
        ErpB2bEdiLog log = dao.newEntity();
        log.setEdiDocId(doc.getId());
        log.setOrgId(doc.getOrgId());
        log.setDirection(direction);
        log.setRequestPayload(requestPayload);
        log.setResultCode(error ? ErpB2bConstants.EDI_RESULT_ERROR : ErpB2bConstants.EDI_RESULT_SUCCESS);
        log.setResultMsg(error ? ("PARSE_FAILED: " + errorMsg) : "RECEIVE: 入站报文解析成功");
        log.setLogTime(CoreMetrics.currentTimestamp());
        dao.saveEntity(log);
    }

    protected boolean verifySignature(String payload, String signature, String secret) {
        if (signature == null || signature.isEmpty() || secret == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            String expected = sb.toString();
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private IEntityDao<ErpB2bAsn> dao() {
        return daoProvider.daoFor(ErpB2bAsn.class);
    }
}
