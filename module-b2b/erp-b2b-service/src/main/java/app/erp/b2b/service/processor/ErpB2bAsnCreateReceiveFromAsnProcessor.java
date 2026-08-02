package app.erp.b2b.service.processor;

import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.dao.entity.ErpB2bAsnLine;
import app.erp.b2b.service.ErpB2bConfigs;
import app.erp.b2b.service.ErpB2bConstants;
import app.erp.b2b.service.ErpB2bErrors;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpB2bAsn createReceiveFromAsn per-mutation Processor。
 * 自包含 config-gated 入库草稿创建编排（MATCHED→RECEIVED_TO_STOCK）：建 ErpPurReceive 头 + 行级回填（核心零污染，仅弱指针 orderId）。
 * （R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpB2bAsnCreateReceiveFromAsnProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ErpB2bAsnCreateReceiveFromAsnProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    public ErpB2bAsn createReceiveFromAsn(Long asnId, IServiceContext context) {
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
        ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).newEntity();
        receive.setCode("RCV-FROM-ASN-" + asn.getCode());
        receive.setOrderId(po.getId());
        receive.setSupplierId(po.getSupplierId());
        receive.setWarehouseId(po.getWarehouseId());
        receive.setCurrencyId(po.getCurrencyId());
        receive.setBusinessDate(CoreMetrics.today());
        receive.setDocStatus("UNSUBMITTED");
        receive.setApproveStatus("UNSUBMITTED");
        receive.setReceiveStatus("NOT_RECEIVED");
        receive.setRemark("由 ASN 自动创建（B2B_ASN 弱指针）");
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);

        // 行级回填：iterate AsnLine → ErpPurReceiveLine（plan 2026-07-19-0849-1 Phase 1 Decision）
        fillReceiveLinesFromAsn(asn, receive, po);

        // ASN → RECEIVED_TO_STOCK
        asn.setStatus(ErpB2bConstants.ASN_STATUS_RECEIVED_TO_STOCK);
        daoProvider.daoFor(ErpB2bAsn.class).saveOrUpdateEntity(asn);

        LOG.info("ASN {} 创建采购入库草稿 {} 成功（RECEIVED_TO_STOCK）", asn.getCode(), receive.getCode());
        return asn;
    }

    /**
     * 按 Phase 1 Decision 落地行级回填。迭代 {@code asn.lines}，为每条 AsnLine 创建一条 {@link ErpPurReceiveLine}：
     *
     * <ul>
     *   <li>materialId：透传 AsnLine.materialId；为 null 或 ErpMdMaterial 不存在 → 抛
     *       {@link ErpB2bErrors#ERR_B2B_ASN_LINE_MATERIAL_REQUIRED} 守卫（Decision (f)）。</li>
     *   <li>uoMId：materialId → ErpMdMaterial.uoMId 反查（mandatory 主数据必然有值，Decision (f)①）。</li>
     *   <li>quantity：AsnLine.shippedQty 优先（实际发货数量），fallback AsnLine.quantity。</li>
     *   <li>lineNo：透传 AsnLine.lineNo（Decision (c)①）。</li>
     *   <li>unitPrice / taxRate / orderLineId：经 materialId 反查 PO line 透传（Decision (a)①）。</li>
     *   <li>amount：派生 unitPrice × quantity（HALF_UP scale=4），任一为 null 则 null（Decision (b)①）。</li>
     *   <li>warehouseId：复用 receive.warehouseId（Decision (d)②）。</li>
     * </ul>
     *
     * <p><b>空白 AsnLine 边界</b>（Decision (e)①）：0 行合法，Receive 头仍创建，仅 LOG.warn 提示。
     *
     * <p><b>失败回滚</b>（Decision 实现策略 (b)）：任一行持久化失败或守卫触发 → 抛 NopException，@BizMutation
     * 事务回滚，已写入的 Receive 头 + ReceiveLine 全部回滚（强一致）。
     */
    protected void fillReceiveLinesFromAsn(ErpB2bAsn asn, ErpPurReceive receive, ErpPurOrder po) {
        List<ErpB2bAsnLine> asnLines = findAsnLines(asn.getId());
        if (asnLines.isEmpty()) {
            LOG.warn("ASN {} 无 AsnLine 行，createReceiveFromAsn 仅建 Receive 头不回填行级", asn.getCode());
            return;
        }
        // 一次性拉取 PO 行列表，O(N) 反查 unitPrice/taxRate/orderLineId（Decision (a)①）
        List<ErpPurOrderLine> poLines = findPoLines(po.getId());
        IEntityDao<ErpPurReceiveLine> lineDao = daoProvider.daoFor(ErpPurReceiveLine.class);
        IEntityDao<ErpMdMaterial> materialDao = daoProvider.daoFor(ErpMdMaterial.class);

        for (ErpB2bAsnLine asnLine : asnLines) {
            Long materialId = asnLine.getMaterialId();
            if (materialId == null) {
                throw new NopException(ErpB2bErrors.ERR_B2B_ASN_LINE_MATERIAL_REQUIRED)
                        .param(ErpB2bErrors.ARG_ASN_CODE, asn.getCode())
                        .param(ErpB2bErrors.ARG_LINE_NO, asnLine.getLineNo())
                        .param(ErpB2bErrors.ARG_MATERIAL_ID, null);
            }
            ErpMdMaterial material = materialDao.getEntityById(materialId);
            if (material == null) {
                throw new NopException(ErpB2bErrors.ERR_B2B_ASN_LINE_MATERIAL_REQUIRED)
                        .param(ErpB2bErrors.ARG_ASN_CODE, asn.getCode())
                        .param(ErpB2bErrors.ARG_LINE_NO, asnLine.getLineNo())
                        .param(ErpB2bErrors.ARG_MATERIAL_ID, materialId);
            }

            ErpPurReceiveLine receiveLine = lineDao.newEntity();
            receiveLine.setReceiveId(receive.getId());
            receiveLine.setLineNo(asnLine.getLineNo());
            receiveLine.setMaterialId(materialId);
            receiveLine.setUoMId(material.getUoMId());

            // quantity：shippedQty 优先（实际发货），fallback quantity
            BigDecimal qty = asnLine.getShippedQty() != null ? asnLine.getShippedQty() : asnLine.getQuantity();
            receiveLine.setQuantity(qty);

            // PO line 反查（按 materialId）：unitPrice / taxRate / orderLineId 透传
            ErpPurOrderLine matchedPoLine = findMatchingPoLine(poLines, materialId);
            if (matchedPoLine != null) {
                receiveLine.setOrderLineId(matchedPoLine.getId());
                receiveLine.setUnitPrice(matchedPoLine.getUnitPrice());
                receiveLine.setTaxRate(matchedPoLine.getTaxRate());
                if (matchedPoLine.getUnitPrice() != null && qty != null) {
                    receiveLine.setAmount(matchedPoLine.getUnitPrice().multiply(qty).setScale(4, RoundingMode.HALF_UP));
                }
            }

            // warehouseId 复用 Receive 头（Decision (d)②）
            receiveLine.setWarehouseId(receive.getWarehouseId());
            receiveLine.setRemark("由 ASN AsnLine #" + asnLine.getLineNo() + " 自动回填");

            try {
                lineDao.saveEntity(receiveLine);
            } catch (RuntimeException e) {
                LOG.warn("ASN {} 行 {} 持久化 ReceiveLine 失败：{}", asn.getCode(), asnLine.getLineNo(), e.getMessage());
                throw e;
            }
        }
        LOG.info("ASN {} 行级回填完成：{} 行 ErpPurReceiveLine", asn.getCode(), asnLines.size());
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
}
