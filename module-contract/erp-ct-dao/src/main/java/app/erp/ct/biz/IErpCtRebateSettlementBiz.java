
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtRebateSettlement;

/**
 * 返利结算单业务接口。除标准 CRUD 外，定义结算过账契约
 * （对齐 {@code docs/design/contract/volume-discount.md} §返利信用单 / §结算流程）：
 *
 * <ul>
 *   <li>{@link #postSettlement}：DRAFT → POSTED，汇总关联未结算计提，
 *       按 {@code rebateType} 生成贷项凭证（PURCHASE→AP 负额发票，SALES→AR 负额发票），
 *       标记计提 {@code isSettled=true}。CANCELLED 状态抛
 *       {@link io.nop.api.core.exceptions.NopException}。</li>
 * </ul>
 */
public interface IErpCtRebateSettlementBiz extends ICrudBiz<ErpCtRebateSettlement> {

    @BizMutation
    ErpCtRebateSettlement postSettlement(@Name("settlementId") Long settlementId, IServiceContext context);
}
