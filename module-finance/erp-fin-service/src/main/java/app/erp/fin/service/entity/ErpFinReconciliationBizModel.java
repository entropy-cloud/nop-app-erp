
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.dto.AutoReconResult;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.dto.ReconciliationReversePreview;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.close.CloseVoucherWriter;
import app.erp.fin.service.close.CloseVoucherWriter.Line;
import app.erp.fin.service.reconciliation.AutoReconciliationEngine;
import app.erp.fin.service.reconciliation.DualSideConsistencyChecker;
import app.erp.fin.service.reconciliation.PartnerBalanceUpdater;
import app.erp.fin.service.reconciliation.ReconciliationSettler;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 核销单聚合根 Biz（{@code ar-ap-reconciliation.md}）。CRUD 之外承载 create（草稿头+行）/
 * post（结算 + 余额重算）/ reverse（恢复 + 余额重算）。
 *
 * <p>核销单是 finance 域 GL/账龄视角的正式核销（period-end 正式核销），独立作用于辅助账
 * {@link ErpFinArApItem}；purchase/sales 域级核销（ErpPurPaymentLine/ErpSalReceiptLine）作为运营核销权威并行，
 * 二者关系见 plan Phase 2 Decision。
 *
 * <p>核销不直接生成 GL 凭证（凭证由收付款审核时生成，{@code ar-ap-reconciliation.md §核销流程} 步骤5）。
 * 事务入口钉在 {@code @BizMutation}。
 */
@BizModel("ErpFinReconciliation")
public class ErpFinReconciliationBizModel extends CrudBizModel<ErpFinReconciliation> implements IErpFinReconciliationBiz {
    public ErpFinReconciliationBizModel() {
        setEntityName(ErpFinReconciliation.class.getName());
    }

    @Inject
    ReconciliationSettler settler;
    @Inject
    PartnerBalanceUpdater partnerBalanceUpdater;
    @Inject
    AutoReconciliationEngine autoReconciliationEngine;
    @Inject
    DualSideConsistencyChecker dualSideConsistencyChecker;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Override
    @BizMutation
    public ErpFinReconciliation create(@Name("direction") String direction,
                                       @Name("partnerId") Long partnerId,
                                       @Name("businessDate") LocalDate businessDate,
                                       @Name("lines") List<ReconciliationLineInput> lines,
                                       IServiceContext context) {
        if (direction == null || partnerId == null || businessDate == null
                || lines == null || lines.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_DIRECTION_MISMATCH)
                    .param(ErpFinErrors.ARG_DIRECTION, direction);
        }

        ErpFinArApItem sample = loadItem(lines.get(0).getInvoiceItemId());

        IEntityDao<ErpFinReconciliation> headDao = daoProvider().daoFor(ErpFinReconciliation.class);
        ErpFinReconciliation head = headDao.newEntity();
        head.setCode("REC-" + StringHelper.generateUUID().substring(0, 12));
        head.setOrgId(sample.getOrgId());
        head.setAcctSchemaId(sample.getAcctSchemaId());
        head.setDirection(direction);
        head.setPartnerId(partnerId);
        head.setBusinessDate(businessDate);
        head.setCurrencyId(sample.getCurrencyId());
        head.setExchangeRate(sample.getExchangeRate() != null ? sample.getExchangeRate() : BigDecimal.ONE);
        head.setTotalAmountSource(BigDecimal.ZERO);
        head.setTotalAmountFunctional(BigDecimal.ZERO);
        head.setFxGainLoss(BigDecimal.ZERO);
        head.setDocStatus(ErpFinConstants.RECON_STATUS_DRAFT);
        headDao.saveEntity(head);

        IEntityDao<ErpFinReconciliationLine> lineDao = daoProvider().daoFor(ErpFinReconciliationLine.class);
        int lineNo = 1;
        for (ReconciliationLineInput in : lines) {
            ErpFinReconciliationLine line = lineDao.newEntity();
            line.setReconciliationId(head.getId());
            line.setLineNo(lineNo++);
            line.setPaymentItemId(in.getPaymentItemId());
            line.setInvoiceItemId(in.getInvoiceItemId());
            line.setSettledAmountSource(nz(in.getSettledAmountSource()));
            line.setSettledAmountFunctional(nz(in.getSettledAmountFunctional()));
            lineDao.saveEntity(line);
        }
        return head;
    }

    @Override
    @BizMutation
    public ErpFinReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        if (!ErpFinConstants.RECON_STATUS_DRAFT.equals(head.getDocStatus())) {
            throw statusError(head);
        }
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);
        if (lines.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_RECONCILIATION_ID, reconciliationId);
        }

        BigDecimal precision = reconcilePrecision();
        for (ErpFinReconciliationLine line : lines) {
            validateLine(head, line, precision);
        }

        if (isReconFxGainLossEnabled()) {
            BigDecimal fxGainLoss = settler.settleWithFx(head, lines);
            generateReconFxVoucher(head, fxGainLoss);
        } else {
            settler.settle(head, lines);
        }
        head.setDocStatus(ErpFinConstants.RECON_STATUS_POSTED);
        head.setPostedAt(CoreMetrics.currentTimestamp());
        head.setPostedBy(context.getUserContext() != null ? context.getUserContext().getUserId() : null);

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
    }

    @Override
    @BizMutation
    public ErpFinReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        if (!ErpFinConstants.RECON_STATUS_POSTED.equals(head.getDocStatus())) {
            throw statusError(head);
        }
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);

        settler.reverseSettle(lines);
        reverseReconFxVoucher(head, context);
        head.setDocStatus(ErpFinConstants.RECON_STATUS_REVERSED);

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
    }

    /**
     * F7 §3 核销单冲销预览。只读，不执行实际冲销。镜像 {@link #reverse} 的前置校验（须 POSTED），
     * 预览双方辅助账回退项 + partner 余额刷新影响，供前端 dialog 展示后再确认执行。
     */
    @Override
    @BizQuery
    public ReconciliationReversePreview previewReverse(@Name("reconciliationId") Long reconciliationId,
                                                       IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId, context);
        if (!ErpFinConstants.RECON_STATUS_POSTED.equals(head.getDocStatus())) {
            throw statusError(head);
        }
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);

        ReconciliationReversePreview preview = new ReconciliationReversePreview();
        preview.setReconciliationId(head.getId());
        preview.setCode(head.getCode());
        preview.setDirection(head.getDirection());
        preview.setTotalAmountFunctional(nz(head.getTotalAmountFunctional()));
        preview.setPartnerId(head.getPartnerId());
        preview.setWillSetReversed(true);
        preview.setWillRefreshPartnerBalance(true);

        for (ErpFinReconciliationLine line : lines) {
            BigDecimal restore = nz(line.getSettledAmountFunctional());
            preview.getRevertedItems().add(toRevertedItem(line.getPaymentItemId(), "payment",
                    loadItem(line.getPaymentItemId()), restore));
            preview.getRevertedItems().add(toRevertedItem(line.getInvoiceItemId(), "invoice",
                    loadItem(line.getInvoiceItemId()), restore));
        }
        return preview;
    }

    private ReconciliationReversePreview.RevertedItem toRevertedItem(Long itemId, String side,
                                                                     ErpFinArApItem item, BigDecimal restoreAmount) {
        ReconciliationReversePreview.RevertedItem ri = new ReconciliationReversePreview.RevertedItem();
        ri.setArApItemId(itemId);
        ri.setSourceBillType(item.getSourceBillType());
        ri.setSourceBillCode(item.getSourceBillCode());
        ri.setSide(side);
        ri.setCurrentStatus(item.getStatus());
        ri.setRestoreAmountFunctional(restoreAmount);
        ri.setWillBecomeStatus(estimateStatusAfterRevert(item, restoreAmount));
        return ri;
    }

    /** 反推 reverseSettle 后的辅助账状态：回退后已核销额 ≤ 0 → OPEN，否则 PARTIAL。 */
    private static String estimateStatusAfterRevert(ErpFinArApItem item, BigDecimal restoreAmount) {
        BigDecimal remainingSettled = nz(item.getSettledAmountFunctional()).subtract(restoreAmount);
        if (remainingSettled.compareTo(BigDecimal.ZERO) <= 0) {
            return ErpFinConstants.AR_AP_STATUS_OPEN;
        }
        return ErpFinConstants.AR_AP_STATUS_PARTIAL;
    }

    // ---------- 自动核销 ----------

    @Override
    @BizMutation
    public AutoReconResult runAutoReconciliation(@Name("direction") String direction,
                                                  @Name("partnerId") Long partnerId,
                                                  @Name("strategy") String strategy,
                                                  IServiceContext context) {
        if (!isAutoReconcileEnabled()) {
            throw new NopException(ErpFinErrors.ERR_AUTO_RECON_DISABLED);
        }
        IServiceContext ctx = context != null ? context : new ServiceContextImpl();
        String effectiveStrategy = resolveStrategy(strategy);
        LocalDate businessDate = CoreMetrics.today();

        AutoReconResult result = new AutoReconResult();
        List<Long> partnerIds = partnerId != null
                ? Collections.singletonList(partnerId)
                : autoReconciliationEngine.findPartnersWithOpenItems(direction, ctx);

        for (Long pid : partnerIds) {
            AutoReconciliationEngine.MatchResult match =
                    autoReconciliationEngine.matchAndBuild(direction, pid, effectiveStrategy, ctx);
            result.getUnmatched().addAll(match.getUnmatched());
            if (match.getLines().isEmpty()) {
                continue;
            }
            ErpFinReconciliation head = create(direction, pid, businessDate, match.getLines(), ctx);
            orm().flushSession();
            post(head.getId(), ctx);
            result.getReconciliationIds().add(head.getId());
        }
        return result;
    }

    @Override
    @BizQuery
    public DualSideDiffReport checkDualSideConsistency(@Name("direction") String direction,
                                                       @Name("partnerId") Long partnerId,
                                                       IServiceContext context) {
        IServiceContext ctx = context != null ? context : new ServiceContextImpl();
        return dualSideConsistencyChecker.check(direction, partnerId, ctx);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。

    // ---------- helpers ----------

    protected boolean isAutoReconcileEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECONCILE, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    protected String resolveStrategy(String strategy) {
        if (!StringHelper.isBlank(strategy)) {
            return strategy.toUpperCase();
        }
        String s = AppConfig.var(ErpFinConstants.CONFIG_AUTO_RECON_STRATEGY,
                ErpFinConstants.AUTO_RECON_STRATEGY_FIFO);
        return !StringHelper.isBlank(s) ? s.toUpperCase() : ErpFinConstants.AUTO_RECON_STRATEGY_FIFO;
    }

    // ---------- 校验 ----------

    protected void validateLine(ErpFinReconciliation head, ErpFinReconciliationLine line, BigDecimal precision) {
        ErpFinArApItem paymentItem = loadItem(line.getPaymentItemId());
        ErpFinArApItem invoiceItem = loadItem(line.getInvoiceItemId());

        if (!head.getDirection().equals(paymentItem.getDirection())
                || !head.getDirection().equals(invoiceItem.getDirection())) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_DIRECTION_MISMATCH)
                    .param(ErpFinErrors.ARG_DIRECTION, head.getDirection());
        }
        if (!paymentItem.getPartnerId().equals(invoiceItem.getPartnerId())) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_PARTNER_MISMATCH)
                    .param(ErpFinErrors.ARG_PAYMENT_ITEM_ID, line.getPaymentItemId())
                    .param(ErpFinErrors.ARG_INVOICE_ITEM_ID, line.getInvoiceItemId());
        }
        assertOpen(paymentItem, line.getPaymentItemId());
        assertOpen(invoiceItem, line.getInvoiceItemId());

        BigDecimal amt = nz(line.getSettledAmountFunctional());
        if (!isAllowOverReconcile()) {
            assertNotOver(amt, paymentItem, line.getPaymentItemId(), precision);
            assertNotOver(amt, invoiceItem, line.getInvoiceItemId(), precision);
        }
        if (head.getBusinessDate() != null && invoiceItem.getBusinessDate() != null
                && head.getBusinessDate().isBefore(invoiceItem.getBusinessDate())) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_DATE_BEFORE_INVOICE)
                    .param(ErpFinErrors.ARG_RECON_DATE, head.getBusinessDate())
                    .param(ErpFinErrors.ARG_INVOICE_DATE, invoiceItem.getBusinessDate());
        }
    }

    protected void assertOpen(ErpFinArApItem item, Long itemId) {
        if (item.getStatus() != null
                && (Objects.equals(item.getStatus(), ErpFinConstants.AR_AP_STATUS_SETTLED)
                || Objects.equals(item.getStatus(), ErpFinConstants.AR_AP_STATUS_CANCELLED))) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_ITEM_NOT_OPEN)
                    .param(ErpFinErrors.ARG_PAYMENT_ITEM_ID, itemId);
        }
    }

    protected void assertNotOver(BigDecimal amt, ErpFinArApItem item, Long itemId, BigDecimal precision) {
        BigDecimal open = nz(item.getOpenAmountFunctional());
        if (amt.subtract(open).compareTo(precision) > 0) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_OVER_AMOUNT)
                    .param(ErpFinErrors.ARG_SETTLE_AMOUNT, amt)
                    .param(ErpFinErrors.ARG_OPEN_AMOUNT, open)
                    .param(ErpFinErrors.ARG_PAYMENT_ITEM_ID, itemId);
        }
    }

    // ---------- helpers ----------

    protected ErpFinReconciliation requireHead(Long id, IServiceContext context) {
        ErpFinReconciliation head = get(String.valueOf(id), true, context);
        if (head == null) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_RECONCILIATION_ID, id);
        }
        return head;
    }

    protected List<ErpFinReconciliationLine> loadLines(Long reconciliationId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireHead/get 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpFinReconciliationLine> dao = daoProvider().daoFor(ErpFinReconciliationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("reconciliationId", reconciliationId));
        List<ErpFinReconciliationLine> lines = new ArrayList<>(dao.findAllByQuery(q));
        lines.sort((a, b) -> Integer.compare(
                a.getLineNo() == null ? Integer.MAX_VALUE : a.getLineNo(),
                b.getLineNo() == null ? Integer.MAX_VALUE : b.getLineNo()));
        return lines;
    }

    protected ErpFinArApItem loadItem(Long id) {
        // D2 边界场景：跨实体只读加载辅助账项（核销内部实体，无独立 IBiz），数据权限由核销单聚合根访问控制覆盖。
        IEntityDao<ErpFinArApItem> dao = daoProvider().daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.getEntityById(id);
        if (item == null) {
            throw new NopException(ErpFinErrors.ERR_AR_AP_ITEM_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ID, id);
        }
        return item;
    }
    protected BigDecimal reconcilePrecision() {
        BigDecimal p = AppConfig.var(ErpFinConstants.CONFIG_RECONCILE_PRECISION, new BigDecimal("0.01"));
        return p != null ? p : new BigDecimal("0.01");
    }

    /**
     * 在重算往来余额前刷新 ORM 会话，确保 settler 对辅助账的脏改动已落库，
     * 使 {@link PartnerBalanceUpdater#refresh} 的聚合查询读到最新 openAmount。
     */
    protected void flushBeforeBalance() {
        orm().flushSession();
    }

    protected boolean isAllowOverReconcile() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_ALLOW_OVER_RECONCILE, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    protected NopException statusError(ErpFinReconciliation head) {
        return new NopException(ErpFinErrors.ERR_RECONCILIATION_STATUS_INVALID)
                .param(ErpFinErrors.ARG_RECONCILIATION_ID, head.getId())
                .param(ErpFinErrors.ARG_DOC_STATUS, head.getDocStatus());
    }

    // ---------- 多币种核销汇兑损益（R1.9 / P1-MA2-009） ----------

    protected boolean isReconFxGainLossEnabled() {
        return Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_RECON_FX_GAIN_LOSS_ENABLED, Boolean.FALSE));
    }

    /**
     * 核销结算后生成已实现汇兑损益凭证（{@code ar-ap-reconciliation.md §汇兑损益核销规则}）。差额=0 不生成。
     * 经 {@link CloseVoucherWriter} 直写（对齐 {@code ExchangeRevaluationService} 范式）。
     */
    protected void generateReconFxVoucher(ErpFinReconciliation head, BigDecimal fxGainLoss) {
        if (fxGainLoss == null || fxGainLoss.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        boolean isReceivable = ErpFinConstants.DIRECTION_RECEIVABLE.equals(head.getDirection());
        ErpMdSubject counterpart = requireReconSubject(isReceivable
                ? ErpFinConstants.CONFIG_AR_SUBJECT_CODE : ErpFinConstants.CONFIG_AP_SUBJECT_CODE);
        ErpMdSubject fxSubject = requireReconSubject(ErpFinConstants.CONFIG_FX_GAIN_LOSS_SUBJECT_CODE);

        // 收益：借往来 / 贷汇兑损益；损失：借汇兑损益 / 贷往来。AR gain=fx>0；AP gain=fx<0。
        boolean gain = isReceivable == (fxGainLoss.compareTo(BigDecimal.ZERO) > 0);
        BigDecimal abs = fxGainLoss.abs();
        String counterpartDc = gain ? ErpFinConstants.DC_DEBIT : ErpFinConstants.DC_CREDIT;
        String fxDc = gain ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT;

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(counterpart.getId(), counterpart.getCode(), counterpart.getName(),
                counterpartDc, abs, head.getPartnerId()));
        lines.add(new Line(fxSubject.getId(), fxSubject.getCode(), fxSubject.getName(), fxDc, abs, null));

        CloseVoucherWriter.writeVoucher(daoProvider(), "RFX",
                RECON_FX_BILL_CODE_PREFIX + head.getCode(),
                ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name(),
                head.getOrgId(), head.getAcctSchemaId(), resolvePeriodId(head.getBusinessDate()), head.getCurrencyId(),
                head.getExchangeRate() != null ? head.getExchangeRate() : BigDecimal.ONE,
                head.getBusinessDate(), lines, "核销已实现汇兑损益");
    }

    /** 红冲核销汇兑损益凭证（若存在）。无 FX 凭证时静默跳过（兼容 config 关闭或差额=0 场景）。 */
    protected void reverseReconFxVoucher(ErpFinReconciliation head, IServiceContext context) {
        if (!hasFxVoucher(head)) {
            return;
        }
        voucherBiz.reverse(RECON_FX_BILL_CODE_PREFIX + head.getCode(), ErpFinBusinessType.EXCHANGE_GAIN_LOSS, context);
    }

    /** 检查是否存在该核销单的已过账 FX 凭证（经业财回链反查）。 */
    protected boolean hasFxVoucher(ErpFinReconciliation head) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherBillR> dao =
                daoProvider().daoFor(app.erp.fin.dao.entity.ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", RECON_FX_BILL_CODE_PREFIX + head.getCode()));
        q.addFilter(eq("businessType", ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    protected ErpMdSubject requireReconSubject(String configKey) {
        String code = AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        IEntityDao<ErpMdSubject> dao = daoProvider().daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        return list.get(0);
    }

    static final String RECON_FX_BILL_CODE_PREFIX = "RECON-FX-";

    /** 按业务日期解析所属会计期间 ID（FX 凭证需要 periodId 非空）。无匹配期间返回 null。 */
    protected Long resolvePeriodId(LocalDate businessDate) {
        if (businessDate == null) {
            return null;
        }
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider().daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.le("startDate", businessDate));
        q.addFilter(io.nop.api.core.beans.FilterBeans.ge("endDate", businessDate));
        q.setLimit(1);
        List<app.erp.fin.dao.entity.ErpFinAccountingPeriod> periods = dao.findAllByQuery(q);
        return periods.isEmpty() ? null : periods.get(0).getId();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
