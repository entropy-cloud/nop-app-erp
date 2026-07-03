
package app.erp.inv.biz;

import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 所有权转移单契约（consignment.md §ErpInvOwnershipTransfer）。CRUD 之外承载状态机三动作：
 * {@link #confirm}（DRAFT→CONFIRMED）、{@link #done}（CONFIRMED→DONE，触发同库位余额重分类 + 业财过账派发）、
 * {@link #cancel}（DRAFT/CONFIRMED→CANCELLED）。
 */
public interface IErpInvOwnershipTransferBiz extends ICrudBiz<ErpInvOwnershipTransfer> {

    @BizMutation
    ErpInvOwnershipTransfer confirm(@Name("transferId") Long transferId, IServiceContext context);

    @BizMutation
    ErpInvOwnershipTransfer done(@Name("transferId") Long transferId, IServiceContext context);

    @BizMutation
    ErpInvOwnershipTransfer cancel(@Name("transferId") Long transferId, IServiceContext context);
}
