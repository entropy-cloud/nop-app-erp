
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinReconciliationBiz;
import app.erp.fin.dao.dto.ReconciliationLineInput;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.dao.entity.ErpFinReconciliationLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.reconciliation.PartnerBalanceUpdater;
import app.erp.fin.service.reconciliation.ReconciliationSettler;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Override
    @BizMutation
    @SingleSession
    public ErpFinReconciliation create(@Name("direction") Integer direction,
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
    @SingleSession
    public ErpFinReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId);
        if (!Integer.valueOf(ErpFinConstants.RECON_STATUS_DRAFT).equals(head.getDocStatus())) {
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

        settler.settle(head, lines);
        head.setDocStatus(ErpFinConstants.RECON_STATUS_POSTED);
        head.setPostedAt(CoreMetrics.currentDateTime());
        head.setPostedBy(context.getUserContext() != null ? context.getUserContext().getUserId() : null);

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context) {
        ErpFinReconciliation head = requireHead(reconciliationId);
        if (!Integer.valueOf(ErpFinConstants.RECON_STATUS_POSTED).equals(head.getDocStatus())) {
            throw statusError(head);
        }
        List<ErpFinReconciliationLine> lines = loadLines(reconciliationId);

        settler.reverseSettle(lines);
        head.setDocStatus(ErpFinConstants.RECON_STATUS_REVERSED);

        flushBeforeBalance();
        partnerBalanceUpdater.refresh(head.getPartnerId());
        return head;
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
                && (item.getStatus() == ErpFinConstants.AR_AP_STATUS_SETTLED
                || item.getStatus() == ErpFinConstants.AR_AP_STATUS_CANCELLED)) {
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

    protected ErpFinReconciliation requireHead(Long id) {
        IEntityDao<ErpFinReconciliation> dao = daoProvider().daoFor(ErpFinReconciliation.class);
        ErpFinReconciliation head = dao.getEntityById(id);
        if (head == null) {
            throw new NopException(ErpFinErrors.ERR_RECONCILIATION_NOT_FOUND)
                    .param(ErpFinErrors.ARG_RECONCILIATION_ID, id);
        }
        return head;
    }

    protected List<ErpFinReconciliationLine> loadLines(Long reconciliationId) {
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

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
