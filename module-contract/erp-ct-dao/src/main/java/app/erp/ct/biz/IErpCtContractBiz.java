
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtContract;

/**
 * 合同头业务接口。除标准 CRUD 外，定义合同全生命周期状态机契约
 * （对齐 {@code docs/design/contract/state-machine.md}）：
 *
 * <ul>
 *   <li>{@link #activate}：NEGOTIATION → ACTIVE（前置 contractType↔contractDirection 组合合法、当前版本定稿或同步签署）。</li>
 *   <li>{@link #suspend}：ACTIVE → SUSPENDED。</li>
 *   <li>{@link #resume}：SUSPENDED → ACTIVE。</li>
 *   <li>{@link #terminate}：ACTIVE → TERMINATED（终态，未开票 InvoicePlan 隐式作废）。</li>
 *   <li>{@link #expire}：ACTIVE → EXPIRED（终态）。</li>
 *   <li>{@link #amend}：ACTIVE → DRAFT 修订，新建版本（versionNo 递增，原子翻转 isCurrent）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpCtContractBiz extends ICrudBiz<ErpCtContract> {

    @BizMutation
    ErpCtContract activate(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract suspend(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract resume(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract terminate(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract expire(@Name("contractId") Long contractId, IServiceContext context);

    @BizMutation
    ErpCtContract amend(@Name("contractId") Long contractId, IServiceContext context);
}
