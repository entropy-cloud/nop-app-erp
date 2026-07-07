
package app.erp.ast.biz;

import app.erp.ast.dao.entity.ErpAstInventory;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

/**
 * 资产盘点业务接口（UC-AST-09）。
 *
 * <p>盘点全流程动作经 {@code ErpAstInventory.xbiz} 单行委托 {@code ErpAstInventoryProcessor}。
 * 状态机：DRAFT→COUNTING→RECONCILING→POSTED（+ CANCELLED 终态）。详见 owner doc {@code inventory.md}。
 */
public interface IErpAstInventoryBiz extends ICrudBiz<ErpAstInventory> {

    @BizMutation
    ErpAstInventory createInventory(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory submitForCount(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory reconcile(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory processVariance(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory approve(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory post(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory cancel(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstInventory reverse(@Name("id") Long id, IServiceContext context);
}
