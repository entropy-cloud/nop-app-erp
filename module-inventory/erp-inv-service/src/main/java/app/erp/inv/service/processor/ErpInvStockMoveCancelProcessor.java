package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpInvStockMove cancel per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含取消编排：require → status 守卫（DRAFT/CONFIRMED）→ CONFIRMED 时条件释放预留量（跨实体 ErpInvStockBalance 写）
 * → 翻 CANCELLED。共享 protected helper（{@code requireMove}/{@code loadLines}/{@code releaseReservation}）单一真相源在
 * {@link ErpInvStockMoveProcessor}（delete-after-extract facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvStockMoveCancelProcessor {

    @Inject
    ErpInvStockMoveProcessor facade;

    public ErpInvStockMove cancel(Long moveId, IServiceContext context) {
        ErpInvStockMove move = facade.requireMove(moveId, context);
        String status = move.getDocStatus();
        if (status == null
                || (!Objects.equals(status, ErpInvConstants.DOC_STATUS_DRAFT)
                        && !Objects.equals(status, ErpInvConstants.DOC_STATUS_CONFIRMED))) {
            throw new NopException(ErpInvErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpInvErrors.ARG_MOVE_CODE, move.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "DRAFT或CONFIRMED");
        }
        if (Objects.equals(status, ErpInvConstants.DOC_STATUS_CONFIRMED)) {
            facade.releaseReservation(move, facade.loadLines(move.getId()), context);
        }
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CANCELLED);
        facade.moveDao().saveOrUpdateEntity(move);
        return move;
    }
}
