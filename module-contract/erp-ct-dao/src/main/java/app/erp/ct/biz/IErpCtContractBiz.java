
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
 *   <li>{@link #submit}：DRAFT → NEGOTIATION（提交谈判，零版本自动创建 v1，已有版本保留既有 DRAFT 当前版本不动）。</li>
 *   <li>{@link #activate}：NEGOTIATION → ACTIVE（前置 contractType↔contractDirection 组合合法、当前版本定稿或同步签署）。</li>
 *   <li>{@link #suspend}：ACTIVE → SUSPENDED。</li>
 *   <li>{@link #resume}：SUSPENDED → ACTIVE。</li>
 *   <li>{@link #terminate}：ACTIVE/NEGOTIATION → TERMINATED（终态，未开票 InvoicePlan 隐式作废）。NEGOTIATION 源态对应谈判破裂放弃（未生效，版本归档经 useLogicalDelete 既有语义）。</li>
 *   <li>{@link #expire}：ACTIVE → EXPIRED（终态）。</li>
 *   <li>{@link #amend}：ACTIVE → DRAFT 修订，新建版本（versionNo 递增，原子翻转 isCurrent）。</li>
 *   <li>{@link #rejectAmend}：DRAFT → ACTIVE 驳回恢复（恢复前任 current 版本 isCurrent=true——D5 选项 B：
 *       优先 SIGNED 最大 versionNo，无 SIGNED 回落 FINALIZED 最大者）。</li>
 * </ul>
 *
 * <p>每条迁移校验前置状态，违反抛 {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpCtContractBiz extends ICrudBiz<ErpCtContract> {

    @BizMutation
    ErpCtContract submit(@Name("contractId") Long contractId, IServiceContext context);

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

    @BizMutation
    ErpCtContract rejectAmend(@Name("contractId") Long contractId, IServiceContext context);
}
