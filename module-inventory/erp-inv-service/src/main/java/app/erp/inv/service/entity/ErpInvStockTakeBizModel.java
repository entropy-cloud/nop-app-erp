
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
import app.erp.inv.service.statemachine.ErpInvStockTakeStateMachine;

@BizModel("ErpInvStockTake")
public class ErpInvStockTakeBizModel extends CrudBizModel<ErpInvStockTake> implements IErpInvStockTakeBiz {
    public ErpInvStockTakeBizModel(){
        setEntityName(ErpInvStockTake.class.getName());
    }

    @Inject
    ErpInvStockTakeStateMachine stateMachine;

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
        String status = take.getDocStatus();
        // 固定来源态守卫委托 StateMachine Bean（非法边 Bean 抛 common 层码，映射为领域码 + common 作 cause）
        // completeTake 不自动生成差异移动单（owner doc §盘点 Deferred：当前手工 generateMove）
        try {
            stateMachine.assertCanCompleteTake(status);
        } catch (NopException e) {
            throw new NopException(ErpInvErrors.ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION, e)
                    .param(ErpInvErrors.ARG_TAKE_ID, takeId)
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status);
        }
        take.setDocStatus(stateMachine.completeTakeTargetStatus());
        updateEntity(take, null, context);
        return take;
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
