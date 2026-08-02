package app.erp.b2b.service.processor;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpB2bAsn matchPurchaseOrder per-mutation Processor。
 * 自包含采购订单匹配编排（RECEIVED→MATCHED）：PO 查找 + 关闭/取消判定 + 逐行物料匹配与超量校验 + EdiDoc 归档。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpB2bAsnMatchPurchaseOrderProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ErpB2bAsnMatchPurchaseOrderProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpB2bEdiDocBiz ediDocBiz;

    public ErpB2bAsn matchPurchaseOrder(Long asnId, IServiceContext context) {
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
            daoProvider.daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);
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
        daoProvider.daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);

        // EdiDoc → ARCHIVED
        try {
            ediDocBiz.archive(asn.getSourceEdiDocId(), context);
        } catch (Exception e) {
            LOG.warn("ASN {} 归档 EdiDoc 失败（不阻塞匹配）：{}", asn.getCode(), e.getMessage());
        }

        LOG.info("ASN {} 匹配采购订单 {} 成功（MATCHED）", asn.getCode(), asn.getRelatedBillCode());
        return asn;
    }

    // ---------- 内部辅助 ----------

    protected ErpB2bAsn requireAsn(Long asnId) {
        ErpB2bAsn asn = daoProvider.daoFor(ErpB2bAsn.class).getEntityById(asnId);
        if (asn == null) {
            throw new NopException(ErpB2bErrors.ERR_B2B_ASN_ILLEGAL_TRANSITION)
                    .param(ErpB2bErrors.ARG_ASN_ID, asnId);
        }
        return asn;
    }

    protected ErpPurOrder findPurchaseOrder(String code) {
        if (code == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        // O-5：追加 id DESC 确保确定性
        q.addOrderField("id", true);
        return daoProvider.daoFor(ErpPurOrder.class).findFirstByQuery(q);
    }

    protected boolean isPoClosedOrCancelled(ErpPurOrder po) {
        String docStatus = po.getDocStatus();
        return "CANCELLED".equals(docStatus) || "CLOSED".equals(docStatus);
    }

    @SuppressWarnings("unchecked")
    protected List<ErpB2bAsnLine> findAsnLines(Long asnId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("asnId", asnId));
        return daoProvider.daoFor(ErpB2bAsnLine.class).findAllByQuery(q);
    }

    @SuppressWarnings("unchecked")
    protected List<ErpPurOrderLine> findPoLines(Long orderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return daoProvider.daoFor(ErpPurOrderLine.class).findAllByQuery(q);
    }

    protected ErpPurOrderLine findMatchingPoLine(List<ErpPurOrderLine> poLines, Long materialId) {
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

    protected void markEdiDocError(Long ediDocId, String error, IServiceContext context) {
        if (ediDocId == null) {
            return;
        }
        try {
            ediDocBiz.markError(ediDocId, error, context);
        } catch (Exception e) {
            LOG.warn("回填 EdiDoc markError 失败：{}", e.getMessage());
        }
    }
}
