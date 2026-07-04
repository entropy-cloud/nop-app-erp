
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtRebateAgreement;

/**
 * 返利协议业务接口。除标准 CRUD 外，定义返利计提契约
 * （对齐 {@code docs/design/contract/volume-discount.md} §年度返利协议 / §追溯调整）：
 *
 * <ul>
 *   <li>{@link #runAccrual}：聚合期间已过账 AP/AR 发票，按 {@code accrualMethod}
 *       （PERIOD_END / PROGRESSIVE）驱动 {@code RebateEngine.accrue}，更新累计/预估金额。</li>
 * </ul>
 *
 * <p>协议须 ACTIVE，否则抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpCtRebateAgreementBiz extends ICrudBiz<ErpCtRebateAgreement> {

    @BizMutation
    ErpCtRebateAgreement runAccrual(@Name("agreementId") Long agreementId,
                                    @Name("asOfDate") java.time.LocalDate asOfDate,
                                    IServiceContext context);
}
