
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinEmployeeAdvanceBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.EmployeeAdvancePostingDispatcher;
import app.erp.fin.service.processor.ErpFinEmployeeAdvanceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.like;

/**
 * 员工借款单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 {@link ErpFinEmployeeAdvanceProcessor} 全权处理。
 *
 * <p>{@link #cashRepay} 为金额更新 + 凭证生成动作（非状态机迁移），BizModel Facade 直落（plan 2026-07-18-0718-2）。
 * {@link #reverseCashRepay} 反向现金还款红冲闭环（plan 2026-07-18-1745-3）。
 */
@BizModel("ErpFinEmployeeAdvance")
public class ErpFinEmployeeAdvanceBizModel extends CrudBizModel<ErpFinEmployeeAdvance> implements IErpFinEmployeeAdvanceBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinEmployeeAdvanceBizModel.class);

    @Inject
    ErpFinEmployeeAdvanceProcessor advanceProcessor;

    @Inject
    EmployeeAdvancePostingDispatcher advancePostingDispatcher;

    public ErpFinEmployeeAdvanceBizModel() {
        setEntityName(ErpFinEmployeeAdvance.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinEmployeeAdvance cancel(@Name("advanceId") Long advanceId, IServiceContext context) {
        return advanceProcessor.cancel(advanceId, context);
    }

    @Override
    @BizMutation
    public ErpFinEmployeeAdvance cashRepay(@Name("advanceId") Long advanceId,
                                           @Name("amount") BigDecimal amount,
                                           IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);

        // 守卫 1：须已过账且 APPROVED
        if (!Boolean.TRUE.equals(advance.getPosted())
                || !Objects.equals(advance.getApproveStatus(), ErpFinConstants.APPROVE_STATUS_APPROVED)) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_REPAYABLE)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId);
        }

        // 守卫 2：amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_AMOUNT_INVALID)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId)
                    .param(ErpFinErrors.ARG_SETTLE_AMOUNT, amount);
        }

        // 守卫 3：amount <= outstandingAmount
        BigDecimal outstanding = nz(advance.getOutstandingAmount());
        if (amount.compareTo(outstanding) > 0) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_EXCEEDS_OUTSTANDING)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId)
                    .param(ErpFinErrors.ARG_SETTLE_AMOUNT, amount)
                    .param(ErpFinErrors.ARG_OPEN_AMOUNT, outstanding);
        }

        // 字段翻转（先持久化，对齐 postSettle 失败不阻断范式——残留风险：字段已更新但凭证缺失，归异常工作台补录）
        advance.setSettledAmount(nz(advance.getSettledAmount()).add(amount));
        advance.setOutstandingAmount(outstanding.subtract(amount));
        // docStatus 保持 APPROVED 不变（owner doc §还款状态派生：outstandingAmount=0 由派生投影表达「已结清」）
        updateEntity(advance, null, context);

        // 委派过账派发器：失败不阻断字段更新（仅 log warn）
        boolean posted = advancePostingDispatcher.postCashRepay(advance, amount, context);
        if (!posted) {
            LOG.warn("员工借款现金还款凭证生成失败但字段已更新：advanceId={}, amount={}", advanceId, amount);
        }

        return advance;
    }

    /**
     * 反向现金还款（红冲闭环，plan 2026-07-18-1745-3）。
     *
     * <p>反查 advance 最近一笔未红冲的 cashRepay NORMAL 凭证 → 调 {@link EmployeeAdvancePostingDispatcher#reverseSettle}
     * 红冲 → 按红冲前原凭证 totalDebit 回退 advance 字段（settled-=amount / outstanding+=amount）。
     *
     * <p>字段回退次序：先调 reverseSettle 红冲凭证（红冲失败抛异常事务回滚，强一致保证无残留字段回退）→
     * 再回退字段。这与 {@link #cashRepay} 的"字段先于凭证"范式相反，因反审核属补救路径，
     * 须保证无残留半状态——对齐 plan 1745-3 §Decision (b) 残留风险注释。
     */
    @Override
    @BizMutation
    public ErpFinEmployeeAdvance reverseCashRepay(@Name("advanceId") Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = requireAdvance(advanceId, context);

        // 反查最近一笔未红冲的 cashRepay NORMAL 凭证（按 billCode 前缀 + businessType + voucher.isReversed=false）
        String prefix = "EA-CASH-REPAY-" + advance.getCode() + "-";
        ErpFinVoucherBillR latestCashLink = findLatestUnreversedCashRepayLink(prefix);
        if (latestCashLink == null) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_CASH_REPAY_VOUCHER_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId)
                    .param(ErpFinErrors.ARG_ADVANCE_CODE, advance.getCode());
        }

        ErpFinVoucher originalVoucher = daoProvider().daoFor(ErpFinVoucher.class)
                .getEntityById(latestCashLink.getVoucherId());
        BigDecimal voucherAmount = originalVoucher != null && originalVoucher.getTotalDebit() != null
                ? originalVoucher.getTotalDebit() : BigDecimal.ZERO;

        // 先红冲凭证（强一致：失败抛异常事务回滚，无残留字段回退）
        advancePostingDispatcher.reverseSettle(latestCashLink.getBillCode());

        // 凭证红冲成功后回退字段
        advance = requireAdvance(advanceId, context);
        advance.setSettledAmount(nz(advance.getSettledAmount()).subtract(voucherAmount));
        advance.setOutstandingAmount(nz(advance.getOutstandingAmount()).add(voucherAmount));
        updateEntity(advance, null, context);

        return advance;
    }

    /**
     * 反查最近一笔未红冲的 cashRepay 凭证 voucher_bill_r 链接。
     * 条件：billCode LIKE 'EA-CASH-REPAY-{advanceCode}-%' + businessType=EMPLOYEE_ADVANCE_SETTLE
     * + 关联 voucher.postingType=NORMAL + isReversed=false。
     * 排序：按 voucherId desc（cashRepay billHeadCode 含 millis 后缀，时间序≈voucherId 序）。
     */
    private ErpFinVoucherBillR findLatestUnreversedCashRepayLink(String billCodePrefix) {
        IDaoProvider dp = daoProvider();
        IEntityDao<ErpFinVoucherBillR> linkDao = dp.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                like("billCode", billCodePrefix + "%"),
                eq("businessType", ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE.name())
        ));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        IEntityDao<ErpFinVoucher> voucherDao = dp.daoFor(ErpFinVoucher.class);
        return links.stream()
                .filter(lnk -> {
                    ErpFinVoucher v = voucherDao.getEntityById(lnk.getVoucherId());
                    return v != null
                            && Objects.equals(ErpFinConstants.POSTING_TYPE_NORMAL, v.getPostingType())
                            && !Boolean.TRUE.equals(v.getIsReversed());
                })
                .max(Comparator.comparing(ErpFinVoucherBillR::getVoucherId))
                .orElse(null);
    }

    private ErpFinEmployeeAdvance requireAdvance(Long advanceId, IServiceContext context) {
        ErpFinEmployeeAdvance advance = get(String.valueOf(advanceId), true, context);
        if (advance == null) {
            throw new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_NOT_FOUND)
                    .param(ErpFinErrors.ARG_ADVANCE_ID, advanceId);
        }
        return advance;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

}
