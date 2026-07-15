package app.erp.mfg.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 委外加工 GL 过账派发器（manufacturing 域侧独立 dispatcher，plan 2026-07-13-0455-1 §Phase 3）。
 *
 * <p>承接 {@link ManufacturingIssuePostingDispatcher} 范式：委外单 → 加载关联库存移动单 → 读流水成本 →
 * 装配 {@link PostingEvent} → 调 {@link MfgPostingExecutor} post → 成功置 posted=true。
 * 过账失败以 try/catch 吞异常告警，保持 posted=false（DeferredPostingSweepJob 兜底）。
 *
 * <p>三段过账：
 * <ul>
 *   <li>{@link #dispatchIssuePosting}：SUBCONTRACT_ISSUE 材料发出（Dr 委外物资 / Cr 原材料）。</li>
 *   <li>{@link #dispatchReceiptPosting}：SUBCONTRACT_RECEIPT 成品入库（Dr 产成品 / Cr 委外物资）。</li>
 *   <li>{@link #dispatchFeePosting}：SUBCONTRACT_FEE 加工费（Dr 委外物资 / Cr 应付账款）→ posted=true。</li>
 * </ul>
 */
public class SubcontractPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SubcontractPostingDispatcher.class);

    @Inject
    MfgPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockLedgerBiz stockLedgerBiz;

    public void setExecutor(MfgPostingExecutor executor) {
        this.executor = executor;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setStockLedgerBiz(IErpInvStockLedgerBiz stockLedgerBiz) {
        this.stockLedgerBiz = stockLedgerBiz;
    }

    /**
     * 委外发料过账：加载发料出库移动单 → 读流水材料成本 → 装配 SUBCONTRACT_ISSUE 事件 → 过账。
     */
    public void dispatchIssuePosting(Long subcontractOrderId) {
        ErpMfgSubcontractOrder order = loadOrder(subcontractOrderId);
        if (order == null) {
            return;
        }
        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_ISSUE, order.getCode());
        if (move == null) {
            return;
        }
        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        if (ledgers.isEmpty()) {
            return;
        }
        List<ErpMfgSubcontractOrderLine> lines = loadLines(subcontractOrderId);
        PostingEvent event = buildIssueEvent(order, move, ledgers, lines);
        postEvent(event, order, "发料");
    }

    /**
     * 委外收货过账：加载成品入库移动单 → 读流水成本 → 装配 SUBCONTRACT_RECEIPT 事件 → 过账。
     */
    public void dispatchReceiptPosting(Long subcontractOrderId) {
        ErpMfgSubcontractOrder order = loadOrder(subcontractOrderId);
        if (order == null) {
            return;
        }
        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_SUBCONTRACT_RECEIPT, order.getCode());
        if (move == null) {
            return;
        }
        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        if (ledgers.isEmpty()) {
            return;
        }
        PostingEvent event = buildReceiptEvent(order, move, ledgers);
        postEvent(event, order, "收货");
    }

    /**
     * 委外加工费过账：读订单头加工费 → 装配 SUBCONTRACT_FEE 事件 → 过账 → 成功回写 posted=true。
     */
    public void dispatchFeePosting(Long subcontractOrderId) {
        ErpMfgSubcontractOrder order = loadOrder(subcontractOrderId);
        if (order == null) {
            return;
        }
        if (Boolean.TRUE.equals(order.getPosted())) {
            return;
        }
        BigDecimal fee = order.getProcessingFee() != null ? order.getProcessingFee() : BigDecimal.ZERO;
        if (fee.signum() <= 0) {
            return;
        }
        PostingEvent event = buildFeeEvent(order);
        try {
            Long voucherId = executor.postEvent(event);
            if (voucherId != null) {
                markPosted(subcontractOrderId);
            }
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("委外加工费过账失败，委外单 {} 保持 posted=false：{}", order.getCode(), e.getMessage());
            } else {
                LOG.error("委外加工费过账异常，委外单 {} 保持 posted=false", order.getCode(), e);
            }
        }
    }

    private void postEvent(PostingEvent event, ErpMfgSubcontractOrder order, String stageLabel) {
        try {
            executor.postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("委外{}过账失败，委外单 {}：{}", stageLabel, order.getCode(), e.getMessage());
            } else {
                LOG.error("委外{}过账异常，委外单 {}", stageLabel, order.getCode(), e);
            }
        }
    }

    private PostingEvent buildIssueEvent(ErpMfgSubcontractOrder order, ErpInvStockMove move,
                                         List<ErpInvStockLedger> ledgers, List<ErpMfgSubcontractOrderLine> lines) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.SUBCONTRACT_ISSUE);
        event.setBillHeadCode(order.getCode() + "-SI");
        event.setOrgId(order.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));
        event.setCurrencyId(order.getCurrencyId());
        event.setExchangeRate(order.getExchangeRate() != null ? order.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = order.getBusinessDate() != null ? order.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(SubcontractIssueAcctDocProvider.KEY_SUBCONTRACT_CODE, order.getCode());

        List<Map<String, Object>> ledgerLines = new ArrayList<>();
        for (ErpInvStockLedger ledger : ledgers) {
            BigDecimal lineCost = ledger.getTotalCost() != null ? ledger.getTotalCost().abs() : BigDecimal.ZERO;
            if (lineCost.signum() == 0) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put(SubcontractIssueAcctDocProvider.KEY_MATERIAL_COST, lineCost);
            line.put(SubcontractIssueAcctDocProvider.KEY_INVENTORY_SUBJECT, ErpMfgConstants.SUBJECT_FINISHED_GOODS);
            ErpMdMaterial material = ledger.getMaterialId() != null
                    ? daoProvider.daoFor(ErpMdMaterial.class).getEntityById(ledger.getMaterialId()) : null;
            line.put(SubcontractIssueAcctDocProvider.KEY_MATERIAL_CODE,
                    material != null ? material.getCode() : "");
            ledgerLines.add(line);
        }
        billData.put(SubcontractIssueAcctDocProvider.KEY_LINES, ledgerLines);
        event.setBillData(billData);
        return event;
    }

    private PostingEvent buildReceiptEvent(ErpMfgSubcontractOrder order, ErpInvStockMove move,
                                           List<ErpInvStockLedger> ledgers) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.SUBCONTRACT_RECEIPT);
        event.setBillHeadCode(order.getCode() + "-SR");
        event.setOrgId(order.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));
        event.setCurrencyId(order.getCurrencyId());
        event.setExchangeRate(order.getExchangeRate() != null ? order.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = order.getBusinessDate() != null ? order.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(SubcontractReceiptAcctDocProvider.KEY_SUBCONTRACT_CODE, order.getCode());

        List<Map<String, Object>> receiptLines = new ArrayList<>();
        for (ErpInvStockLedger ledger : ledgers) {
            BigDecimal lineCost = ledger.getTotalCost() != null ? ledger.getTotalCost().abs() : BigDecimal.ZERO;
            if (lineCost.signum() == 0) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put(SubcontractReceiptAcctDocProvider.KEY_FINISHED_COST, lineCost);
            line.put(SubcontractReceiptAcctDocProvider.KEY_FINISHED_SUBJECT,
                    ErpMfgConstants.SUBJECT_SUBCONTRACT_FINISHED_GOODS);
            ErpMdMaterial material = ledger.getMaterialId() != null
                    ? daoProvider.daoFor(ErpMdMaterial.class).getEntityById(ledger.getMaterialId()) : null;
            line.put(SubcontractReceiptAcctDocProvider.KEY_MATERIAL_CODE,
                    material != null ? material.getCode() : "");
            receiptLines.add(line);
        }
        billData.put(SubcontractReceiptAcctDocProvider.KEY_LINES, receiptLines);
        event.setBillData(billData);
        return event;
    }

    private PostingEvent buildFeeEvent(ErpMfgSubcontractOrder order) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.SUBCONTRACT_FEE);
        event.setBillHeadCode(order.getCode() + "-SF");
        event.setOrgId(order.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(order.getOrgId()));
        event.setCurrencyId(order.getCurrencyId());
        event.setExchangeRate(order.getExchangeRate() != null ? order.getExchangeRate() : BigDecimal.ONE);
        LocalDate voucherDate = order.getBusinessDate() != null ? order.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(SubcontractFeeAcctDocProvider.KEY_SUBCONTRACT_CODE, order.getCode());
        billData.put(SubcontractFeeAcctDocProvider.KEY_PROCESSING_FEE,
                order.getProcessingFee() != null ? order.getProcessingFee() : BigDecimal.ZERO);
        event.setBillData(billData);
        return event;
    }

    private void markPosted(Long orderId) {
        ErpMfgSubcontractOrder managed = daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
        if (managed != null) {
            managed.setPosted(true);
            managed.setPostedAt(CoreMetrics.currentTimestamp());
        }
    }

    private ErpMfgSubcontractOrder loadOrder(Long orderId) {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
    }

    private ErpInvStockMove findMove(String relatedBillType, String relatedBillCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", relatedBillType));
        q.addFilter(eq("relatedBillCode", relatedBillCode));
        q.setLimit(1);
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvStockLedger> loadLedgers(Long moveId) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private List<ErpMfgSubcontractOrderLine> loadLines(Long orderId) {
        IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subcontractOrderId", orderId));
        return dao.findAllByQuery(q);
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }
}
