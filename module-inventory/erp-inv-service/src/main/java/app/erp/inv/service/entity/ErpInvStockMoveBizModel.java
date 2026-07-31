package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.processor.ErpInvStockMoveCancelProcessor;
import app.erp.inv.service.processor.ErpInvStockMoveCompleteProcessor;
import app.erp.inv.service.processor.ErpInvStockMoveConfirmProcessor;
import app.erp.inv.service.processor.ErpInvStockMoveGenerateMoveProcessor;
import app.erp.inv.service.processor.ErpInvStockMoveProcessor;
import app.erp.inv.service.processor.ErpInvStockMoveReverseProcessor;
import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 库存移动单 BizModel（Facade）。状态机迁移（DRAFT→CONFIRMED→DONE/CANCELLED）、冲销经 per-mutation Processor 编排
 * （{@link ErpInvStockMoveGenerateMoveProcessor}/{@link ErpInvStockMoveConfirmProcessor}/
 * {@link ErpInvStockMoveCompleteProcessor}/{@link ErpInvStockMoveCancelProcessor}/{@link ErpInvStockMoveReverseProcessor}，
 * protected step 方法，下游可逐 step 覆盖）。{@code findByRelatedBill}（{@code :45} 查询）与追溯链（{@code @BizQuery}）
 * 委托 {@link ErpInvStockMoveProcessor}（不在 MR6 范围）。
 *
 * <p>权威状态机见 {@code docs/design/inventory/state-machine.md}；跨域契约见 {@code docs/design/inventory/cross-domain.md}。
 */
@BizModel("ErpInvStockMove")
public class ErpInvStockMoveBizModel extends CrudBizModel<ErpInvStockMove> implements IErpInvStockMoveBiz {

    @Inject
    ErpInvStockMoveProcessor stockMoveProcessor;

    @Inject
    ErpInvStockMoveGenerateMoveProcessor generateMoveProcessor;

    @Inject
    ErpInvStockMoveConfirmProcessor confirmProcessor;

    @Inject
    ErpInvStockMoveCompleteProcessor completeProcessor;

    @Inject
    ErpInvStockMoveCancelProcessor cancelProcessor;

    @Inject
    ErpInvStockMoveReverseProcessor reverseProcessor;

    public ErpInvStockMoveBizModel() {
        setEntityName(ErpInvStockMove.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvStockMove generateMove(@Name("request") StockMoveRequest request, IServiceContext context) {
        return generateMoveProcessor.generateMove(request, context);
    }

    @Override
    @BizMutation
    public ErpInvStockMove confirm(@Name("moveId") Long moveId, IServiceContext context) {
        return confirmProcessor.confirm(moveId, context);
    }

    @Override
    @BizMutation
    public ErpInvStockMove complete(@Name("moveId") Long moveId, IServiceContext context) {
        return completeProcessor.complete(moveId, context);
    }

    @Override
    @BizMutation
    public ErpInvStockMove cancel(@Name("moveId") Long moveId, IServiceContext context) {
        return cancelProcessor.cancel(moveId, context);
    }

    @Override
    @BizMutation
    public ErpInvStockMove reverse(@Name("moveId") Long moveId, IServiceContext context) {
        return reverseProcessor.reverse(moveId, context);
    }

    @Override
    @BizAction
    public ErpInvStockMove findByRelatedBill(@Name("relatedBillType") String relatedBillType,
                                             @Name("relatedBillCode") String relatedBillCode,
                                             IServiceContext context) {
        return stockMoveProcessor.findByRelatedBill(relatedBillType, relatedBillCode, context);
    }

    @Override
    @BizQuery
    public TraceChainResult forwardTrace(@Name("moveId") Long moveId, IServiceContext context) {
        return stockMoveProcessor.forwardTrace(moveId, context);
    }

    @Override
    @BizQuery
    public TraceChainResult backwardTrace(@Name("moveId") Long moveId, IServiceContext context) {
        return stockMoveProcessor.backwardTrace(moveId, context);
    }

    @Override
    @BizQuery
    public TraceChainResult returnTrace(@Name("moveId") Long moveId, IServiceContext context) {
        return stockMoveProcessor.returnTrace(moveId, context);
    }

    @Override
    @BizQuery
    public TraceChainResult batchTrace(@Name("batchNo") String batchNo, IServiceContext context) {
        return stockMoveProcessor.batchTrace(batchNo, context);
    }

    // 经 orm().batchLoadProps 一次性批量加载 to-one 关系（DataLoader 机制），再读取名称。
    // 容错：实体可能因 REQUIRES_NEW 过账事务 evict 而脱离 session（见 StockMoveProcessor reload 注释），
    // 此时 batchLoadProps 触发 session-closed。catch 后返回 null 名称，不阻塞主操作（*Name 仅为展示字段）。

}
