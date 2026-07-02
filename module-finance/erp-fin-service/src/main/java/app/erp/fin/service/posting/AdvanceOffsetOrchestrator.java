package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 报销抵扣借款编排器。报销审核过账（EXPENSE_CLAIM 生成员工应付辅助账）成功后，若
 * {@code erp-fin.advance-auto-offset-on-expense=true}（默认 true）且报销人存在未还借款（员工预支应收辅助账 open>0），
 * 以净额 = min(借款未还, 报销应付-员工) 抵扣：回写双方辅助账 open/settled/status + 触发
 * {@link ErpFinBusinessType#EMPLOYEE_ADVANCE_SETTLE} 净额清算凭证（借应付-员工 / 贷其他应收-员工预支）+ 回写借款单
 * settled/outstanding。不足部分（报销应付 > 借款未还）留作应付-员工。
 *
 * <p><b>机制偏离说明（plan Task Route Decision 补注）</b>：plan 原文「复用 ErpFinReconciliation 核销 EMPLOYEE_ADVANCE
 * 应收 vs EXPENSE_CLAIM 应付」。实测 0300-3 {@code ErpFinReconciliationBizModel.validateLine} 强制双方同 direction，
 * 而员工抵扣是 RECEIVABLE（借款）vs PAYABLE（报销）跨方向净额，无法经 ErpFinReconciliation 同方向校验。为保持纯加性
 * （不触动 0300-3 不变量），本编排器<b>复用 open-item 逐笔核销算术（ReconciliationSettler 模式）</b>直接回写双方辅助账，
 * 不经 ErpFinReconciliation 头。每张报销单抵扣单笔最旧未还借款（多借款分多次报销抵扣），保证抵扣可逆。
 *
 * <p>反审核/作废时 {@link #reverseOffset} 先反向：红冲 SETTLE 凭证 + 恢复借款应收辅助账 open + 回滚借款单 settled/outstanding，
 * 再由 EXPENSE_CLAIM 红冲取消报销应付辅助账。
 */
public class AdvanceOffsetOrchestrator {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    EmployeeAdvancePostingDispatcher advanceDispatcher;

    /**
     * 报销审核过账成功后调用。返回 true 表示发生了抵扣。
     */
    public boolean offset(ErpFinExpenseClaim claim) {
        if (!autoOffsetEnabled()) {
            return false;
        }
        // 报销应付辅助账的 partnerId 即已解析的 claimant.partnerId（过账时由 generator 从 billData.EMPLOYEE_ID 写入），
        // 直接采用，避免跨会话关系懒加载 claim.getClaimant()。
        ErpFinArApItem payableItem = findOpenItem(ErpFinConstants.SOURCE_BILL_EXPENSE_CLAIM,
                claim.getCode(), ErpFinConstants.DIRECTION_PAYABLE);
        if (payableItem == null) {
            return false;
        }
        Long partnerId = payableItem.getPartnerId();
        if (partnerId == null) {
            return false;
        }
        BigDecimal payableOpen = nz(payableItem.getOpenAmountFunctional());
        if (payableOpen.signum() <= 0) {
            return false;
        }

        ErpFinArApItem receivableItem = findOldestOpenAdvanceItem(partnerId);
        if (receivableItem == null) {
            return false;
        }
        BigDecimal receivableOpen = nz(receivableItem.getOpenAmountFunctional());
        if (receivableOpen.signum() <= 0) {
            return false;
        }

        BigDecimal net = payableOpen.min(receivableOpen);
        if (net.signum() <= 0) {
            return false;
        }

        // 回写双方辅助账（open/settled/status，复用 ReconciliationSettler 算术）
        applySettlement(payableItem, net);
        applySettlement(receivableItem, net);

        // EMPLOYEE_ADVANCE_SETTLE 净额清算凭证（借应付-员工 / 贷其他应收-员工预支）
        advanceDispatcher.postSettle(claim.getCode(), partnerId, net, claim.getOrgId(),
                claim.getCurrencyId(), claim.getBusinessDate());

        // 回写借款单 settled/outstanding
        ErpFinEmployeeAdvance advance = findAdvanceByCode(receivableItem.getSourceBillCode());
        if (advance != null) {
            advance.setSettledAmount(nz(advance.getSettledAmount()).add(net));
            advance.setOutstandingAmount(nz(advance.getOutstandingAmount()).subtract(net));
            daoProvider.daoFor(ErpFinEmployeeAdvance.class).updateEntity(advance);
            claim.setSettleAdvanceId(advance.getId());
        }
        return true;
    }

    /**
     * 报销反审核/作废前反向抵扣：红冲 SETTLE 凭证 + 恢复借款应收辅助账 open + 回滚借款单 settled/outstanding。
     * 报销应付辅助账由随后的 EXPENSE_CLAIM 红冲（cancelOnReverse）取消，此处不处理。
     */
    public void reverseOffset(ErpFinExpenseClaim claim) {
        BigDecimal settledNet = findSettleVoucherAmount(claim.getCode());
        if (settledNet == null || settledNet.signum() <= 0) {
            return;
        }
        // 红冲 SETTLE 凭证
        advanceDispatcher.reverseSettle(claim.getCode());
        // 恢复借款应收辅助账 open（按 settleAdvanceId 定位借款单 → 其应收辅助账）
        Long advanceId = claim.getSettleAdvanceId();
        ErpFinEmployeeAdvance advance = advanceId != null
                ? daoProvider.daoFor(ErpFinEmployeeAdvance.class).getEntityById(advanceId) : null;
        if (advance != null) {
            ErpFinArApItem receivableItem = findOpenItem(ErpFinConstants.SOURCE_BILL_EMPLOYEE_ADVANCE,
                    advance.getCode(), ErpFinConstants.DIRECTION_RECEIVABLE);
            if (receivableItem != null) {
                reverseSettlement(receivableItem, settledNet);
            }
            advance.setSettledAmount(nz(advance.getSettledAmount()).subtract(settledNet));
            advance.setOutstandingAmount(nz(advance.getOutstandingAmount()).add(settledNet));
            daoProvider.daoFor(ErpFinEmployeeAdvance.class).updateEntity(advance);
        }
    }

    // ---------- helpers ----------

    private boolean autoOffsetEnabled() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_ADVANCE_AUTO_OFFSET_ON_EXPENSE, Boolean.TRUE);
        return flag == null || flag;
    }

    private ErpFinArApItem findOpenItem(String sourceBillType, String sourceBillCode, int direction) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("sourceBillType", sourceBillType),
                eq("sourceBillCode", sourceBillCode),
                eq("direction", direction),
                notIn("status", List.of(ErpFinConstants.AR_AP_STATUS_SETTLED, ErpFinConstants.AR_AP_STATUS_CANCELLED))
        ));
        q.setLimit(1);
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        return items.isEmpty() ? null : items.get(0);
    }

    private ErpFinArApItem findOldestOpenAdvanceItem(Long partnerId) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("sourceBillType", ErpFinConstants.SOURCE_BILL_EMPLOYEE_ADVANCE),
                eq("partnerId", partnerId),
                eq("direction", ErpFinConstants.DIRECTION_RECEIVABLE),
                in("status", List.of(ErpFinConstants.AR_AP_STATUS_OPEN, ErpFinConstants.AR_AP_STATUS_PARTIAL))
        ));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        return items.stream()
                .min(Comparator.comparing(i -> i.getBusinessDate() == null
                        ? java.time.LocalDate.MAX : i.getBusinessDate()))
                .orElse(null);
    }

    private ErpFinEmployeeAdvance findAdvanceByCode(String code) {
        IEntityDao<ErpFinEmployeeAdvance> dao = daoProvider.daoFor(ErpFinEmployeeAdvance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpFinEmployeeAdvance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private BigDecimal findSettleVoucherAmount(String claimCode) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(and(eq("billCode", claimCode),
                eq("businessType", ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE.getCode())));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(lq);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = voucherDao.getEntityById(link.getVoucherId());
            if (v != null && Integer.valueOf(20).equals(v.getDocStatus())
                    && !Boolean.TRUE.equals(v.getIsReversed())) {
                return nz(v.getTotalDebit());
            }
        }
        return null;
    }

    private void applySettlement(ErpFinArApItem item, BigDecimal amount) {
        BigDecimal settled = nz(item.getSettledAmountFunctional()).add(amount);
        BigDecimal open = nz(item.getAmountFunctional()).subtract(settled);
        item.setSettledAmountFunctional(settled);
        item.setSettledAmountSource(settled);
        item.setOpenAmountFunctional(open);
        item.setOpenAmountSource(open);
        item.setStatus(resolveStatus(settled, item.getAmountFunctional()));
    }

    private void reverseSettlement(ErpFinArApItem item, BigDecimal amount) {
        BigDecimal settled = nz(item.getSettledAmountFunctional()).subtract(amount);
        BigDecimal open = nz(item.getAmountFunctional()).subtract(settled);
        item.setSettledAmountFunctional(settled);
        item.setSettledAmountSource(settled);
        item.setOpenAmountFunctional(open);
        item.setOpenAmountSource(open);
        item.setStatus(resolveStatus(settled, item.getAmountFunctional()));
    }

    private int resolveStatus(BigDecimal settledFunctional, BigDecimal amountFunctional) {
        BigDecimal settled = nz(settledFunctional);
        BigDecimal total = nz(amountFunctional);
        if (settled.compareTo(BigDecimal.ZERO) <= 0) {
            return ErpFinConstants.AR_AP_STATUS_OPEN;
        }
        if (settled.compareTo(total) >= 0) {
            return ErpFinConstants.AR_AP_STATUS_SETTLED;
        }
        return ErpFinConstants.AR_AP_STATUS_PARTIAL;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
