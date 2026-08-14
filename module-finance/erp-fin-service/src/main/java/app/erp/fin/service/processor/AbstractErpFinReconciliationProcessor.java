package app.erp.fin.service.processor;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.close.CloseVoucherWriter;
import app.erp.fin.service.close.CloseVoucherWriter.Line;
import app.erp.fin.service.reconciliation.PartnerBalanceUpdater;
import app.erp.fin.service.reconciliation.ReconciliationSettler;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import io.nop.orm.dao.IOrmEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 核销单 per-mutation Processor 共享基类（R6.1）。承载 create/post/reverse/runAutoReconciliation 四个
 * per-mutation Processor 共用的校验、结算、汇兑损益凭证与往来余额刷新辅助（单一真相源，对齐
 * {@code processor-extension-pattern.md} facade protected helper 范式）。子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpFinReconciliationProcessor {

    static final String RECON_FX_BILL_CODE_PREFIX = "RECON-FX-";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ReconciliationSettler settler;
    @Inject
    PartnerBalanceUpdater partnerBalanceUpdater;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) daoProvider.daoFor(ErpFinReconciliation.class)).getOrmTemplate();
    }

    protected IDaoProvider daoProvider() {
        return daoProvider;
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
        ErpFinReconciliation head = daoProvider.daoFor(ErpFinReconciliation.class).getEntityById(id);
        if (head == null) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_RECONCILIATION_ID, id);
        }
        return head;
    }

    protected List<ErpFinReconciliationLine> loadLines(Long reconciliationId) {
        // D2 边界场景：同聚合子表加载，父实体已由 requireHead/get 经数据权限/Meta 管道授权，子行无独立权限规则。
        IEntityDao<ErpFinReconciliationLine> dao = daoProvider.daoFor(ErpFinReconciliationLine.class);
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
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
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
        return statusError(head, null);
    }

    /**
     * 领域码 {@code ERR_RECONCILIATION_STATUS_INVALID}（Bean common 码作 cause 保留，契约 §7）。
     * 参数 reconciliationId/docStatus 由本层组装（唯一真相源在实体），cause 来自状态机 Bean 非法边。
     */
    protected NopException statusError(ErpFinReconciliation head, NopException cause) {
        return new NopException(ErpFinErrors.ERR_RECONCILIATION_STATUS_INVALID, cause)
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

        CloseVoucherWriter.writeVoucher(daoProvider, "RFX",
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
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
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
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
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

    /** 按业务日期解析所属会计期间 ID（FX 凭证需要 periodId 非空）。无匹配期间返回 null。 */
    protected Long resolvePeriodId(LocalDate businessDate) {
        if (businessDate == null) {
            return null;
        }
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.le("startDate", businessDate));
        q.addFilter(io.nop.api.core.beans.FilterBeans.ge("endDate", businessDate));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> periods = dao.findAllByQuery(q);
        return periods.isEmpty() ? null : periods.get(0).getId();
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
