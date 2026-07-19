
package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.inv.dao.entity.ErpInvStockTake;

public interface IErpInvStockTakeBiz extends ICrudBiz<ErpInvStockTake>{

    /**
     * 开始盘点：DRAFT→CONFIRMED（盘点中）。锁定当前库存快照，进入实际盘点阶段。
     */
    @BizMutation
    ErpInvStockTake startTake(@Name("takeId") Long takeId, IServiceContext context);

    /**
     * 完成盘点：CONFIRMED→DONE。根据盘点结果生成盘盈/盘亏移动单（如配置启用）。
     */
    @BizMutation
    ErpInvStockTake completeTake(@Name("takeId") Long takeId, IServiceContext context);

    /**
     * 作废盘点：DRAFT/CONFIRMED→CANCELLED。
     */
    @BizMutation
    ErpInvStockTake cancelTake(@Name("takeId") Long takeId, IServiceContext context);

}
