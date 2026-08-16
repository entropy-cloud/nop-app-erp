
package app.erp.inv.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.inv.biz.IErpInvStockTakeBiz;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.processor.ErpInvStockTakeCompleteTakeProcessor;
import app.erp.inv.service.statemachine.ErpInvStockTakeStateMachine;

/**
 * 盘点单 BizModel（Facade）。域动作 {@code startTake}/{@code completeTake}/{@code cancelTake}：{@code startTake}/
 * {@code cancelTake} 为单步状态翻转保留在 Facade；{@code completeTake}（RC-R1.56 / P1-MA2-062，UC-INV-07）
 * 委托 per-mutation {@link ErpInvStockTakeCompleteTakeProcessor}（protected step 方法，下游可逐 step 覆盖）——
 * 源态守卫（委托 {@link ErpInvStockTakeStateMachine}）+ 行加载 + D1 差异计算回填 + 逐行差异移动单生成（失败隔离）
 * + 终态回写。{@code requireEntity} 保留在 Facade 维持权限管道。
 *
 * <p>权威状态机见 {@code docs/design/inventory/state-machine.md} §盘点单状态机。
 */
@BizModel("ErpInvStockTake")
public class ErpInvStockTakeBizModel extends CrudBizModel<ErpInvStockTake> implements IErpInvStockTakeBiz {
    public ErpInvStockTakeBizModel(){
        setEntityName(ErpInvStockTake.class.getName());
    }

    @Inject
    ErpInvStockTakeStateMachine stateMachine;

    @Inject
    ErpInvStockTakeCompleteTakeProcessor completeTakeProcessor;

    @Override
    @BizMutation
    public ErpInvStockTake startTake(@Name("takeId") Long takeId, IServiceContext context) {
        ErpInvStockTake take = requireEntity(String.valueOf(takeId), null, context);
        String status = take.getDocStatus();
        // 固定来源态守卫委托 StateMachine Bean（非法边 Bean 抛 common 层码，映射为领域码 + common 作 cause）
        try {
            stateMachine.assertCanStartTake(status);
        } catch (NopException e) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION, e)
                    .param(ErpInvErrors.ARG_TAKE_ID, takeId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status);
        }
        // 目标态 CONFIRMED 对应 owner doc 标签「盘点中 (COUNTING)」——dict erp-inv/move-status 无 COUNTING（标签漂移，行为一致）
        take.setDocStatus(stateMachine.startTakeTargetStatus());
        updateEntity(take, null, context);
        return take;
    }

    @Override
    @BizMutation
    public ErpInvStockTake completeTake(@Name("takeId") Long takeId, IServiceContext context) {
        ErpInvStockTake take = requireEntity(String.valueOf(takeId), null, context);
        // RC-R1.56 / P1-MA2-062（UC-INV-07）：完整盘点闭环委托 per-mutation Processor——
        // 行加载 + D1 差异计算回填 + 逐行盘盈/盘亏移动单生成（D2 独立移动单停 CONFIRMED，失败逐行隔离 + D4-b 告警）
        // + 置 DONE。盘点单本身不改余额（断言④），差异经移动单状态机落地（断言⑤）。
        return completeTakeProcessor.completeTake(take, context);
    }

    @Override
    @BizMutation
    public ErpInvStockTake cancelTake(@Name("takeId") Long takeId, IServiceContext context) {
        ErpInvStockTake take = requireEntity(String.valueOf(takeId), null, context);
        String status = take.getDocStatus();
        // 固定来源态守卫委托 StateMachine Bean（守卫非终态 {DONE,CANCELLED}；非法边 Bean 抛 common 层码，映射为领域码 + common 作 cause）
        try {
            stateMachine.assertCanCancel(status);
        } catch (NopException e) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION, e)
                    .param(ErpInvErrors.ARG_TAKE_ID, takeId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status);
        }
        take.setDocStatus(stateMachine.cancelTargetStatus());
        updateEntity(take, null, context);
        return take;
    }
}
