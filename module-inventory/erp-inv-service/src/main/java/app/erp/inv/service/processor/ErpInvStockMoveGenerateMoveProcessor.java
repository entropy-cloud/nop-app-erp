package app.erp.inv.service.processor;

import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpInvStockMove generateMove per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含生成编排：幂等查询 → 建头 → 建行 → 确认 → 业务关联时直接完成。共享 protected helper（{@code newMove}/
 * {@code newLines}/{@code doConfirm}/{@code doComplete}/{@code findExisting}/{@code requireMove}）单一真相源在
 * {@link ErpInvStockMoveProcessor}（delete-after-extract facade，类保留为 helper 持有者）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvStockMoveGenerateMoveProcessor {

    @Inject
    ErpInvStockMoveProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    public ErpInvStockMove generateMove(StockMoveRequest request, IServiceContext context) {
        if (request.isBusinessLinked()) {
            ErpInvStockMove existing = facade.findExisting(request.getRelatedBillType(),
                    request.getRelatedBillCode(), context);
            if (existing != null) {
                return existing;
            }
        }

        ErpInvStockMove move = facade.newMove(request);
        facade.moveDao().saveEntity(move);
        List<ErpInvStockMoveLine> lines = persistLines(move, request);

        facade.doConfirm(move, lines, context);
        if (request.isBusinessLinked()) {
            facade.doComplete(move, lines, request.getAcctSchemaId(), context);
            move = facade.requireMove(move.getId(), context);
        }
        return move;
    }

    protected List<ErpInvStockMoveLine> persistLines(ErpInvStockMove move, StockMoveRequest request) {
        List<ErpInvStockMoveLine> lines = facade.newLines(move, request);
        IEntityDao<ErpInvStockMoveLine> lineDao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        for (ErpInvStockMoveLine line : lines) {
            line.setMoveId(move.getId());
            lineDao.saveEntity(line);
        }
        return lines;
    }
}
