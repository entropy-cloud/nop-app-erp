
package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.inv.dao.entity.ErpInvStockMove;

/**
 * 库存移动单业务接口。除标准 CRUD 外，定义跨域/状态机契约：
 *
 * <ul>
 *   <li>{@link #generateMove(StockMoveRequest, IServiceContext)}：业务单据联动入口（purchase/sales Processor 调用），
 *       幂等（{@code (relatedBillType, relatedBillCode)} 为键），业务联动自动推进到 DONE。</li>
 *   <li>{@link #confirm(Long, IServiceContext)} / {@link #complete(Long, IServiceContext)} / {@link #cancel(Long, IServiceContext)}：状态机迁移。</li>
 *   <li>{@link #reverse(Long, IServiceContext)}：DONE 的纠错路径——生成反向冲销移动单（非反审核）。</li>
 * </ul>
 *
 * <p>权威状态机见 {@code docs/design/inventory/state-machine.md}；跨域契约见 {@code docs/design/inventory/cross-domain.md}。
 */
public interface IErpInvStockMoveBiz extends ICrudBiz<ErpInvStockMove> {

    /**
     * 生成库存移动单。业务单据联动（{@code relatedBillType} 非空）自动 DRAFT→CONFIRMED→DONE；
     * 独立创建停在 CONFIRMED。同源单重复触发幂等返回已生成移动单。
     */
    @BizMutation
    ErpInvStockMove generateMove(@Name("request") StockMoveRequest request, IServiceContext context);

    /**
     * DRAFT → CONFIRMED。出库类/内部调拨校验可用量充足（除非配置允许负库存），通过则增预留。
     */
    @BizMutation
    ErpInvStockMove confirm(@Name("moveId") Long moveId, IServiceContext context);

    /**
     * CONFIRMED → DONE。写不可变库存流水、更新余额（移动加权平均成本）、释放预留、发存货过账事件。
     */
    @BizMutation
    ErpInvStockMove complete(@Name("moveId") Long moveId, IServiceContext context);

    /**
     * DRAFT/CONFIRMED → CANCELLED。若已确认占预留则释放。终态。
     */
    @BizMutation
    ErpInvStockMove cancel(@Name("moveId") Long moveId, IServiceContext context);

    /**
     * 已完成移动单的纠错路径：不改原单，生成反向冲销移动单（新 DRAFT，数量取负），走正常流程到 DONE。
     */
    @BizMutation
    ErpInvStockMove reverse(@Name("moveId") Long moveId, IServiceContext context);
}
