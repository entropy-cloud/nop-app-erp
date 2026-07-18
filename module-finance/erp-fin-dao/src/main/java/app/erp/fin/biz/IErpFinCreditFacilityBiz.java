
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.erp.fin.dao.entity.ErpFinCreditFacility;

/**
 * 银行授信额度 Biz 契约（{@code treasury.md §关键业务规则 1}）。CRUD 之外承载额度占用回写：
 * 开银承时 {@link #reserveCredit} 强一致校验可用额度并 increment usedAmount（乐观锁 version 兜底并发竞争）；
 * 兑付/注销时 {@link #releaseCredit} decrement usedAmount。
 *
 * <p>availableAmount 派生（=total−used），每次回写同步重算。
 *
 * <p>{@link #accrueInterest}（plan 2026-07-18-0718-1）：按区间计提授信利息并经
 * {@code IErpFinVoucherBiz.post} 生成 {@code CREDIT_FACILITY_INTEREST} 凭证（Dr 财务费用-利息支出 / Cr 银行存款）。
 * 计息基数=开始时点 {@code usedAmount}；闭区间天数；年化基准 360 天；{@code billHeadCode}=区间级幂等键。
 */
public interface IErpFinCreditFacilityBiz extends ICrudBiz<ErpFinCreditFacility> {

    /**
     * 占用授信额度：校验 availableAmount >= amount，increment usedAmount 并重算 available。
     * 可用不足抛 {@code NopException}（{@code erp.err.fin.credit-facility.insufficient}）。
     */
    @BizMutation
    ErpFinCreditFacility reserveCredit(@Name("creditFacilityId") Long creditFacilityId,
                                       @Name("amount") BigDecimal amount,
                                       IServiceContext context);

    /**
     * 释放授信额度：decrement usedAmount（不低于 0）并重算 available。
     */
    @BizMutation
    ErpFinCreditFacility releaseCredit(@Name("creditFacilityId") Long creditFacilityId,
                                       @Name("amount") BigDecimal amount,
                                       IServiceContext context);

    /**
     * 计提授信利息：按 {@code fromDate} 时点 {@code usedAmount} × 年化利率 × 闭区间天数 / 360 计算，
     * 经 {@code CREDIT_FACILITY_INTEREST} 业务类型生成凭证（Dr 6603 财务费用-利息支出 / Cr 1002 银行存款）。
     *
     * <p>幂等：同 facility + 同区间二次调用经 {@code IErpFinVoucherBiz.post} 内置 {@code alreadyPosted}
     * 命中返回 {@code null}（{@code billHeadCode}=「CFI-INT-{facilityId}-{fromDate}_{toDate}」区间级幂等键）。
     *
     * @return 新生成的凭证 ID；幂等命中（已计提过同区间）或 usedAmount=0 空操作时返回 {@code null}
     */
    @BizMutation
    Long accrueInterest(@Name("creditFacilityId") Long creditFacilityId,
                        @Name("fromDate") LocalDate fromDate,
                        @Name("toDate") LocalDate toDate,
                        IServiceContext context);
}
