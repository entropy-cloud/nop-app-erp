package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.posting.OwnershipTransferPostingDispatcher;
import app.erp.inv.service.stock.StockMoveBookkeeper;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * ErpInvOwnershipTransfer done per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含完成编排：require → status 守卫 → 不变量校验 → ownership 追踪守卫 → 同库位余额重分类 → 翻 DONE → 业财过账派发。
 * 共享 protected helper（{@code reclassifyBalance}/{@code findBalance}/{@code validateInvariants}）单一真相源在
 * {@link ErpInvOwnershipTransferProcessor}（delete-after-extract facade，类保留为 helper 持有者）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvOwnershipTransferDoneProcessor {

    @Inject
    ErpInvOwnershipTransferProcessor facade;

    @Inject
    StockMoveBookkeeper bookkeeper;

    @Inject
    OwnershipTransferPostingDispatcher postingDispatcher;

    public ErpInvOwnershipTransfer done(Long transferId, IServiceContext context) {
        ErpInvOwnershipTransfer transfer = facade.requireTransfer(transferId, context);
        facade.assertStatus(transfer, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED,
                ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE);
        facade.validateInvariants(transfer);
        if (!bookkeeper.isOwnershipTrackingEnabled()) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRACKING_DISABLED)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode());
        }

        List<ErpInvOwnershipTransferLine> lines = reclassifyBalances(transfer);
        transfer.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE);
        facade.transferDao().saveOrUpdateEntity(transfer);

        postingDispatcher.dispatchIfApplicable(transfer, lines);
        return transfer;
    }

    protected List<ErpInvOwnershipTransferLine> reclassifyBalances(ErpInvOwnershipTransfer transfer) {
        List<ErpInvOwnershipTransferLine> lines = facade.loadLines(transfer.getId());
        for (ErpInvOwnershipTransferLine line : lines) {
            facade.reclassifyBalance(transfer, line);
        }
        return lines;
    }
}
