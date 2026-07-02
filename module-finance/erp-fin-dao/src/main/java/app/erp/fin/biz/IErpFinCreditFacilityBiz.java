
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;

import app.erp.fin.dao.entity.ErpFinCreditFacility;

/**
 * 银行授信额度 Biz 契约（{@code treasury.md §关键业务规则 1}）。CRUD 之外承载额度占用回写：
 * 开银承时 {@link #reserveCredit} 强一致校验可用额度并 increment usedAmount（乐观锁 version 兜底并发竞争）；
 * 兑付/注销时 {@link #releaseCredit} decrement usedAmount。
 *
 * <p>availableAmount 派生（=total−used），每次回写同步重算。
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
}
