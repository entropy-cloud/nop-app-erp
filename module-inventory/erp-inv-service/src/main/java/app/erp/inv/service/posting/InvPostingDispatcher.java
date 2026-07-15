package app.erp.inv.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 存货过账派发器。移动单 DONE 后（流水/余额同事务确立终态之后）按移动类型派生业务类型，
 * 构造 {@link PostingEvent} 经 {@link InvPostingExecutor}（独立新事务）调用财务过账引擎；
 * 同法人内部调拨不发事件。
 *
 * <p>过账失败不阻塞移动单终态（cross-domain §与财务域协作 + state-machine §7）：以 try/catch 包裹，
 * 成功置 {@code posted=true}，失败吞异常记日志、保持 {@code posted=false}（由 DeferredPostingSweepJob（app.erp.fin.service.job）兜底扫描重试）。
 */
public class InvPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(InvPostingDispatcher.class);

    @Inject
    InvPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    /**
     * DONE 后调用。入库→PURCHASE_INPUT、出库→SALES_OUTPUT；同法人内部调拨跳过。
     * 采购退货（{@code relatedBillType=ERP_PUR_RETURN}）/ 销售退货（{@code ERP_SAL_RETURN}）的存货估值过账
     * 分别由 purchase/sales 域独占（PURCHASE_RETURN/SALES_RETURN：借暂估应付或成本/贷存货 与 借存货/贷成本），
     * 故此处跳过，避免 SALES_OUTPUT/PURCHASE_INPUT（借成本或存货/贷存货或暂估应付）与之双计存货。
     * 成功置移动单 {@code posted=true}；失败吞异常保持 {@code posted=false}。
     */
    public void dispatchIfApplicable(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        ErpFinBusinessType businessType = resolveBusinessType(move);
        if (businessType == null) {
            return;
        }

        PostingEvent event = buildEvent(move, lines, businessType);
        try {
            Long voucherId = executor.postEvent(event);
            if (voucherId != null) {
                markMovePosted(move.getId());
            }
        } catch (Exception e) {
            // 过账失败不阻塞移动单终态：保持 DONE + posted=false，由兜底扫描重试。
            if (e instanceof NopException) {
                LOG.warn("存货过账失败，移动单 {} 保持 DONE、posted=false：{}", move.getCode(), e.getMessage());
            } else {
                LOG.error("存货过账异常，移动单 {} 保持 DONE、posted=false", move.getCode(), e);
            }
        }

        // STANDARD 物料采购入库：捕获采购价差（PPV），config-gated 总开关
        dispatchPurchasePriceVariance(move, lines);
    }

    /**
     * 标记移动单 posted=true。过账经 {@code IErpFinVoucherBiz.post} 的 REQUIRES_NEW 事务执行，
     * 成功返回后当前 session 的移动单实体可能被 evict（saveOrUpdateEntity 报 save-entity-not-transient），
     * 故按 ID 重新加载后再设值，确保 posted 标志在当前事务提交时持久化。
     */
    private void markMovePosted(Long moveId) {
        ErpInvStockMove managed = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(moveId);
        if (managed != null) {
            managed.setPosted(true);
            managed.setPostedAt(CoreMetrics.currentTimestamp());
        }
    }

    /**
     * 采购价差（PPV）捕获（plan 2026-07-05-0427-2）：STANDARD 物料采购入库 DONE 时，实际入库成本
     * （移动单行 {@code unitCost}）与标准成本（流水 {@code ledger.unitCost}）差额 × qty = PPV，
     * 经 {@link ErpFinBusinessType#PURCHASE_PRICE_VARIANCE} 过账。config {@code erp-inv.standard-cost-ppv-enabled=false}
     * 时跳过（标准成本仍记账，差异不分离）。
     *
     * <p>过账失败不阻塞主过账状态（try/catch 吞异常告警，对齐主过账语义）。
     */
    protected void dispatchPurchasePriceVariance(ErpInvStockMove move, List<ErpInvStockMoveLine> lines) {
        if (!isPpvEnabled()) {
            return;
        }
        if (!Objects.equals(move.getMoveType(), ErpInvConstants.MOVE_TYPE_INCOMING)) {
            return;
        }
        // 采购退货入库移动：存货估值过账由 purchase 域独占，PPV 同样跳过
        if (ErpInvConstants.RELATED_BILL_TYPE_PUR_RETURN.equals(move.getRelatedBillType())
                || ErpInvConstants.RELATED_BILL_TYPE_SAL_RETURN.equals(move.getRelatedBillType())) {
            return;
        }
        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        for (ErpInvStockMoveLine line : lines) {
            ErpInvStockLedger ledger = findLedgerForLine(ledgers, line.getId());
            if (ledger == null) {
                continue;
            }
            if (!Objects.equals(ledger.getCostMethod(), ErpInvConstants.COST_METHOD_STANDARD)) {
                continue;
            }
            BigDecimal standardUnitCost = nz(ledger.getUnitCost());
            BigDecimal actualUnitCost = nz(line.getUnitCost());
            BigDecimal qty = nz(line.getQuantity());
            BigDecimal variancePerUnit = actualUnitCost.subtract(standardUnitCost);
            if (variancePerUnit.signum() == 0) {
                continue;
            }
            BigDecimal ppvAmount = variancePerUnit.abs().multiply(qty);
            String direction = variancePerUnit.signum() > 0
                    ? PurchasePriceVarianceAcctDocProvider.DIRECTION_DEBIT
                    : PurchasePriceVarianceAcctDocProvider.DIRECTION_CREDIT;

            PostingEvent ppvEvent = buildPpvEvent(move, line, ppvAmount, direction, ledger);
            try {
                executor.postEvent(ppvEvent);
            } catch (Exception e) {
                // 安全（O-22）：PPV 金额属于敏感采购成本数据，错误日志中必须脱敏（仅保留量级，隐藏末尾精度）
                String maskedAmount = maskAmount(ppvAmount);
                if (e instanceof NopException) {
                    LOG.warn("采购价差过账失败，移动单 {} 行 {} 金额(脱敏){}：{}",
                            move.getCode(), line.getId(), maskedAmount, sanitizeMessage(e.getMessage()));
                } else {
                    LOG.error("采购价差过账异常，移动单 {} 行 {} 金额(脱敏){}", move.getCode(), line.getId(), maskedAmount, e);
                }
            }
        }
    }

    private ErpFinBusinessType resolveBusinessType(ErpInvStockMove move) {
        // 采购退货出库移动 / 销售退货入库移动：存货估值过账由 purchase/sales 域独占，inventory 跳过。
        // 维护领料出库移动：非销售出库，维修费用过账（MAINTENANCE_ISSUE）由 maintenance 域独占（当前 Non-Goal），
        // inventory 跳过，避免误派 SALES_OUTPUT 凭证；移动单 DONE 即代表库存已出库。
        // 制造领料出库移动：非销售出库，WIP 过账（MANUFACTURING_ISSUE）由 manufacturing 域独占
        // （Dr: WIP / Cr: Inventory，借方 WIP 科目需 WorkOrder 上下文），inventory 跳过避免误派 SALES_OUTPUT。
        if (ErpInvConstants.RELATED_BILL_TYPE_PUR_RETURN.equals(move.getRelatedBillType())
                || ErpInvConstants.RELATED_BILL_TYPE_SAL_RETURN.equals(move.getRelatedBillType())
                || ErpInvConstants.RELATED_BILL_TYPE_MNT_SPARE_PART.equals(move.getRelatedBillType())
                || ErpInvConstants.RELATED_BILL_TYPE_MFG_ISSUE.equals(move.getRelatedBillType())) {
            return null;
        }
        String moveType = move.getMoveType();
        if (moveType == null) {
            return null;
        }
        if (Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_INCOMING)) {
            return ErpFinBusinessType.PURCHASE_INPUT;
        }
        if (Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_OUTGOING)) {
            return ErpFinBusinessType.SALES_OUTPUT;
        }
        // 完工入库（MANUFACTURE）：产成品存货估值过账（Dr: Inventory / Cr: WIP）属 inventory 域职责
        if (Objects.equals(moveType, ErpInvConstants.MOVE_TYPE_MANUFACTURING)) {
            return ErpFinBusinessType.MANUFACTURING_RECEIPT;
        }
        return null;
    }

    private PostingEvent buildEvent(ErpInvStockMove move, List<ErpInvStockMoveLine> lines,
                                    ErpFinBusinessType businessType) {
        List<ErpInvStockLedger> ledgers = loadLedgers(move.getId());
        BigDecimal totalCost = BigDecimal.ZERO;
        Long acctSchemaId = null;
        Long currencyId = null;
        for (ErpInvStockLedger ledger : ledgers) {
            BigDecimal lineCost = ledger.getTotalCost() != null ? ledger.getTotalCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(lineCost.abs());
            if (acctSchemaId == null) {
                acctSchemaId = ledger.getAcctSchemaId();
            }
            if (currencyId == null) {
                currencyId = ledger.getCurrencyId();
            }
        }
        if (currencyId == null && !lines.isEmpty()) {
            currencyId = lines.get(0).getCurrencyId();
        }

        PostingEvent event = new PostingEvent();
        event.setBusinessType(businessType);
        event.setBillHeadCode(move.getCode());
        event.setOrgId(move.getOrgId());
        event.setAcctSchemaId(acctSchemaId);
        event.setCurrencyId(currencyId);
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = move.getBusinessDate() != null ? move.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("TOTAL_COST", totalCost);
        billData.put("MOVE_CODE", move.getCode());
        if (!lines.isEmpty()) {
            billData.put("MATERIAL_ID", lines.get(0).getMaterialId());
            billData.put("WAREHOUSE_ID", businessType == ErpFinBusinessType.PURCHASE_INPUT
                    ? move.getDestWarehouseId() : move.getSourceWarehouseId());
        }
        event.setBillData(billData);
        return event;
    }

    private List<ErpInvStockLedger> loadLedgers(Long moveId) {
        ormTemplate.flushSession();
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private ErpInvStockLedger findLedgerForLine(List<ErpInvStockLedger> ledgers, Long lineId) {
        for (ErpInvStockLedger ledger : ledgers) {
            if (Objects.equals(ledger.getMoveLineId(), lineId)) {
                return ledger;
            }
        }
        return null;
    }

    private PostingEvent buildPpvEvent(ErpInvStockMove move, ErpInvStockMoveLine line,
                                       BigDecimal ppvAmount, String direction, ErpInvStockLedger ledger) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PURCHASE_PRICE_VARIANCE);
        event.setBillHeadCode(move.getCode() + "-PPV");
        event.setOrgId(move.getOrgId());
        event.setAcctSchemaId(ledger.getAcctSchemaId());
        event.setCurrencyId(line.getCurrencyId() != null ? line.getCurrencyId() : ledger.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = move.getBusinessDate() != null ? move.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(PurchasePriceVarianceAcctDocProvider.KEY_PPV_AMOUNT, ppvAmount);
        billData.put(PurchasePriceVarianceAcctDocProvider.KEY_PPV_DIRECTION, direction);
        billData.put(PurchasePriceVarianceAcctDocProvider.KEY_MATERIAL_ID, line.getMaterialId());
        billData.put(PurchasePriceVarianceAcctDocProvider.KEY_WAREHOUSE_ID,
                move.getDestWarehouseId() != null ? move.getDestWarehouseId() : ledger.getWarehouseId());
        event.setBillData(billData);
        return event;
    }

    private boolean isPpvEnabled() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_STANDARD_COST_PPV_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * 金额脱敏：仅保留高位量级，末尾精度归零，避免敏感采购成本数据在日志中明文泄露（O-22）。
     * 例：1234.5678 → 1200.0000
     */
    private static String maskAmount(BigDecimal amount) {
        if (amount == null) {
            return "null";
        }
        BigDecimal masked = amount.setScale(-2, java.math.RoundingMode.HALF_UP).setScale(amount.scale());
        return masked.toPlainString();
    }

    /**
     * 异常消息脱敏（O-22）：将消息中可能内嵌的数字金额模式替换为占位符，避免 PPV 等敏感金额随异常消息写入日志。
     */
    private static String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("\\d+\\.\\d{2,}", "[MASKED_AMOUNT]");
    }
}
