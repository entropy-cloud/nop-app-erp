
package app.erp.b2b.service.entity;

import app.erp.b2b.biz.IErpB2bAsnBiz;
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
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * ASN 入站处理聚合根 Biz。承载 ASN 入站全流程（{@code asn-processing.md}）：
 *
 * <p>webhook 入站（{@link #handleInboundWebhook}）→ HMAC 校验 + 幂等 → 解析报文（{@link #parseToAsn}）
 * → 建 ASN/AsnLine → 采购订单匹配（{@link #matchPurchaseOrder}，RECEIVED→MATCHED）
 * → config-gated 创建入库草稿（{@link #createReceiveFromAsn}，MATCHED→RECEIVED_TO_STOCK）。
 *
 * <p><b>核心零污染</b>：不在 {@code ErpPurReceive} 加 asnId 列，仅弱指针 orderId（→PO→ASN 经 relatedBillCode）。
 *
 * <p><b>跨域访问</b>：采购订单匹配 + 入库草稿创建经 {@link io.nop.dao.api.IDaoProvider} 只读/写
 * ErpPurOrder/ErpPurOrderLine/ErpPurReceive（purchase 域，参 logistics IDaoProvider 范式）。
 */
@BizModel("ErpB2bAsn")
public class ErpB2bAsnBizModel extends CrudBizModel<ErpB2bAsn> implements IErpB2bAsnBiz {
    private static final Logger LOG = LoggerFactory.getLogger(ErpB2bAsnBizModel.class);

    @Inject
    ErpB2bEdiRegistry ediRegistry;
    @Inject
    CodeMappingResolver codeMappingResolver;
    @Inject
    IErpB2bEdiDocBiz ediDocBiz;

    public ErpB2bAsnBizModel() {
        setEntityName(ErpB2bAsn.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public Long handleInboundWebhook(@Name("formatCode") String formatCode,
                                     @Name("partnerCode") String partnerCode,
                                     @Name("signature") String signature,
                                     @Name("eventId") String eventId,
                                     @Name("payload") String payload,
                                     IServiceContext context) {
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
        Long asnId = parseToAsn(formatCode, payload, profile, eventId, context);
        return asnId;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bAsn matchPurchaseOrder(@Name("asnId") Long asnId, IServiceContext context) {
        ErpB2bAsn asn = requireAsn(asnId);
        String status = asn.getStatus();
        if (!ErpB2bConstants.ASN_STATUS_RECEIVED.equals(status)) {
            throw new NopException(ErpB2bErrors.ERR_B2B_ASN_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_ASN_CODE, asn.getCode())
                    .param(ErpB2bErrors.ARG_CURRENT_STATE, status)
                    .param(ErpB2bErrors.ARG_EXPECTED_STATE, ErpB2bConstants.ASN_STATUS_RECEIVED);
        }

        // 查采购订单
        ErpPurOrder po = findPurchaseOrder(asn.getRelatedBillCode());
        if (po == null) {
            LOG.info("ASN {} 未匹配到采购订单 {}（保留 RECEIVED）", asn.getCode(), asn.getRelatedBillCode());
            return asn;
        }

        // PO 已关闭/取消 → blockingLevel=ERROR
        if (isPoClosedOrCancelled(po)) {
            asn.setRemark("采购订单已关闭/取消：" + asn.getRelatedBillCode());
            daoProvider().daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);
            markEdiDocError(asn.getSourceEdiDocId(), "PO_CLOSED: " + asn.getRelatedBillCode(), context);
            LOG.warn("ASN {} 关联采购订单 {} 已关闭/取消", asn.getCode(), asn.getRelatedBillCode());
            return asn;
        }

        // 逐行物料匹配 + 数量校验
        List<ErpB2bAsnLine> asnLines = findAsnLines(asn.getId());
        List<ErpPurOrderLine> poLines = findPoLines(po.getId());
        boolean overQuantity = false;
        for (ErpB2bAsnLine asnLine : asnLines) {
            ErpPurOrderLine matchedPoLine = findMatchingPoLine(poLines, asnLine.getMaterialId());
            if (matchedPoLine == null) {
                LOG.warn("ASN {} 行物料 {} 未在 PO {} 中找到", asn.getCode(), asnLine.getMaterialId(), asn.getRelatedBillCode());
                continue;
            }
            if (asnLine.getShippedQty() != null && matchedPoLine.getQuantity() != null) {
                BigDecimal remaining = matchedPoLine.getQuantity().subtract(
                        matchedPoLine.getReceivedQuantity() != null ? matchedPoLine.getReceivedQuantity() : BigDecimal.ZERO);
                if (asnLine.getShippedQty().compareTo(remaining) > 0) {
                    overQuantity = true;
                }
            }
        }

        // 匹配成功 → MATCHED
        asn.setStatus(ErpB2bConstants.ASN_STATUS_MATCHED);
        if (overQuantity) {
            asn.setRemark("部分行超 PO 数量（blockingLevel=WARN）");
        }
        daoProvider().daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);

        // EdiDoc → ARCHIVED
        try {
            ediDocBiz.archive(asn.getSourceEdiDocId(), context);
        } catch (Exception e) {
            LOG.warn("ASN {} 归档 EdiDoc 失败（不阻塞匹配）：{}", asn.getCode(), e.getMessage());
        }

        LOG.info("ASN {} 匹配采购订单 {} 成功（MATCHED）", asn.getCode(), asn.getRelatedBillCode());
        return asn;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bAsn createReceiveFromAsn(@Name("asnId") Long asnId, IServiceContext context) {
        boolean enabled = AppConfig.var(ErpB2bConfigs.CONFIG_ASN_AUTO_CREATE_RECEIVE,
                ErpB2bConfigs.DEFAULT_ASN_AUTO_CREATE_RECEIVE);
        if (!enabled) {
            LOG.info("ASN→采购入库自动创建未启用（erp-b2b.asn-auto-create-receive=false），跳过");
            return null;
        }

        ErpB2bAsn asn = requireAsn(asnId);
        String status = asn.getStatus();
        if (!ErpB2bConstants.ASN_STATUS_MATCHED.equals(status)) {
            throw new NopException(ErpB2bErrors.ERR_B2B_ASN_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_ASN_CODE, asn.getCode())
                    .param(ErpB2bErrors.ARG_CURRENT_STATE, status)
                    .param(ErpB2bErrors.ARG_EXPECTED_STATE, ErpB2bConstants.ASN_STATUS_MATCHED);
        }

        ErpPurOrder po = findPurchaseOrder(asn.getRelatedBillCode());
        if (po == null) {
            LOG.warn("ASN {} 创建入库失败：PO {} 不存在", asn.getCode(), asn.getRelatedBillCode());
            return asn;
        }

        // 创建采购入库草稿（核心零污染：仅弱指针 orderId，不加 asnId 列）
        ErpPurReceive receive = new ErpPurReceive();
        receive.setCode("RCV-FROM-ASN-" + asn.getCode());
        receive.setOrderId(po.getId());
        receive.setSupplierId(po.getSupplierId());
        receive.setWarehouseId(po.getWarehouseId());
        receive.setBusinessDate(CoreMetrics.today());
        receive.setDocStatus("UNSUBMITTED");
        receive.setApproveStatus("UNSUBMITTED");
        receive.setReceiveStatus("NOT_RECEIVED");
        receive.setRemark("由 ASN " + asn.getCode() + " 自动创建（B2B_ASN 弱指针）");
        daoProvider().daoFor(ErpPurReceive.class).saveEntity(receive);

        // ASN → RECEIVED_TO_STOCK
        asn.setStatus(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK);
        daoProvider().daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);

        LOG.info("ASN {} 创建采购入库草稿 {} 成功（RECEIVED_TO_STOCK）", asn.getCode(), receive.getCode());
        return asn;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpB2bAsn retryMatch(@Name("asnId") Long asnId, IServiceContext context) {
        ErpB2bAsn asn = requireAsn(asnId);
        // 幂等：已 MATCHED/RECEIVED_TO_STOCK 直接返回
        if (ErpB2bConstants.ASN_STATUS_MATCHED.equals(asn.getStatus())
                || ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK.equals(asn.getStatus())) {
            return asn;
        }
        // 回到 RECEIVED 后重新匹配
        if (!ErpB2bConstants.ASN_STATUS_RECEIVED.equals(asn.getStatus())) {
            asn.setStatus(ErpB2bConstants.ASN_STATUS_RECEIVED);
            daoProvider().daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);
        }
        return matchPurchaseOrder(asnId, context);
    }

    @Override
    @BizQuery
    public List<ErpB2bAsn> findUnmatchedAsns(@Name("asOfDate") LocalDate asOfDate, IServiceContext context) {
        IEntityDao<ErpB2bAsn> dao = daoProvider().daoFor(ErpB2bAsn.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpB2bConstants.ASN_STATUS_RECEIVED));
        if (asOfDate != null) {
            q.addFilter(le("estimatedArrivalDate", asOfDate));
        }
        q.setLimit(200);
        return dao.findAllByQuery(q);
    }

    // ---------- parseToAsn (内部方法) ----------

    Long parseToAsn(String formatCode, String payload, ErpB2bPartnerProfile profile,
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
            daoProvider().daoFor(ErpB2bEdiDoc.class).saveOrUpdateEntity(ediDoc);
        }

        // 建 ASN
        ErpB2bAsn asn = new ErpB2bAsn();
        asn.setCode("ASN-" + System.currentTimeMillis());
        asn.setSourceEdiDocId(ediDoc.getId());
        asn.setPartnerId(profile.getPartnerId());
        asn.setRelatedBillType(ErpB2bConstants.RELATED_BILL_TYPE_PO_ORDER);
        asn.setRelatedBillCode(parsed.getRelatedBillCode());
        asn.setStatus(ErpB2bConstants.ASN_STATUS_RECEIVED);
        asn.setShipmentDate(CoreMetrics.today());
        if (parsed.getHeaders().get("estimatedArrivalDate") instanceof LocalDate) {
            asn.setEstimatedArrivalDate((LocalDate) parsed.getHeaders().get("estimatedArrivalDate"));
        }
        daoProvider().daoFor(ErpB2bAsn.class).saveEntity(asn);

        // 建 AsnLine(s) + 代码映射
        IEntityDao<ErpB2bAsnLine> lineDao = daoProvider().daoFor(ErpB2bAsnLine.class);
        int lineNo = 1;
        for (ParsedLine parsedLine : parsed.getLines()) {
            ErpB2bAsnLine line = new ErpB2bAsnLine();
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

    // ---------- helpers ----------

    private ErpB2bAsn requireAsn(Long asnId) {
        ErpB2bAsn asn = daoProvider().daoFor(ErpB2bAsn.class).getEntityById(asnId);
        if (asn == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_ASN_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_ASN_ID, asnId);
        }
        return asn;
    }

    private ErpB2bPartnerProfile findPartnerProfileByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider().daoFor(ErpB2bPartnerProfile.class).findFirstByQuery(q);
    }

    private boolean isDuplicateEvent(String eventId, String formatCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("remark", "WEBHOOK eventId=" + eventId + " formatCode=" + formatCode));
        return daoProvider().daoFor(ErpB2bEdiDoc.class).findFirstByQuery(q) != null;
    }

    @SuppressWarnings("unchecked")
    private List<ErpB2bAsnLine> findAsnLines(Long asnId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("asnId", asnId));
        return daoProvider().daoFor(ErpB2bAsnLine.class).findAllByQuery(q);
    }

    private ErpPurOrder findPurchaseOrder(String code) {
        if (code == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider().daoFor(ErpPurOrder.class).findFirstByQuery(q);
    }

    @SuppressWarnings("unchecked")
    private List<ErpPurOrderLine> findPoLines(Long orderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return daoProvider().daoFor(ErpPurOrderLine.class).findAllByQuery(q);
    }

    private ErpPurOrderLine findMatchingPoLine(List<ErpPurOrderLine> poLines, Long materialId) {
        if (materialId == null) {
            return null;
        }
        for (ErpPurOrderLine line : poLines) {
            if (materialId.equals(line.getMaterialId())) {
                return line;
            }
        }
        return null;
    }

    private boolean isPoClosedOrCancelled(ErpPurOrder po) {
        String docStatus = po.getDocStatus();
        return "CANCELLED".equals(docStatus) || "CLOSED".equals(docStatus);
    }

    private void markEdiDocError(Long ediDocId, String error, IServiceContext context) {
        if (ediDocId == null) {
            return;
        }
        try {
            ediDocBiz.markError(ediDocId, error, context);
        } catch (Exception e) {
            LOG.warn("回填 EdiDoc markError 失败：{}", e.getMessage());
        }
    }

    private void writeEdiLog(ErpB2bEdiDoc doc, String direction, boolean error,
                             String requestPayload, String errorMsg) {
        IEntityDao<ErpB2bEdiLog> dao = daoProvider().daoFor(ErpB2bEdiLog.class);
        ErpB2bEdiLog log = new ErpB2bEdiLog();
        log.setEdiDocId(doc.getId());
        log.setOrgId(doc.getOrgId());
        log.setDirection(direction);
        log.setRequestPayload(requestPayload);
        log.setResultCode(error ? ErpB2bConstants.EDI_RESULT_ERROR : ErpB2bConstants.EDI_RESULT_SUCCESS);
        log.setResultMsg(error ? ("PARSE_FAILED: " + errorMsg) : "RECEIVE: 入站报文解析成功");
        log.setLogTime(CoreMetrics.currentDateTime());
        dao.saveEntity(log);
    }

    private boolean verifySignature(String payload, String signature, String secret) {
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

    /** 内部占位接口，用于 writeEdiLog 的 errorCode 参数标记。 */
    private interface ErrorCodeHolder {
    }
}
