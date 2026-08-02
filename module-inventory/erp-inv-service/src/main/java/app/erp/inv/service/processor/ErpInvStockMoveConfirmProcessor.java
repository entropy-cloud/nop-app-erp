package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpInvStockMove confirm per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含确认编排：require → loadLines → doConfirm（可用量校验 + 预留量占用 + 翻 CONFIRMED）。共享 protected helper
 * 单一真相源在 {@link ErpInvStockMoveProcessor}（delete-after-extract facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvStockMoveConfirmProcessor {

    @Inject
    ErpInvStockMoveProcessor facade;

    public ErpInvStockMove confirm(Long moveId, IServiceContext context) {
        ErpInvStockMove move = facade.requireMove(moveId, context);
        List<ErpInvStockMoveLine> lines = facade.loadLines(move.getId());
        facade.doConfirm(move, lines, context);
        return move;
    }
}
