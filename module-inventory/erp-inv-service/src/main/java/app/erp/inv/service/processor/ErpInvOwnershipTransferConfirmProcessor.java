package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.service.ErpInvConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpInvOwnershipTransfer confirm per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含确认编排：require → status 守卫 → 不变量校验 → 翻 CONFIRMED。共享 protected helper 单一真相源在
 * {@link ErpInvOwnershipTransferProcessor}（delete-after-extract facade，类保留为 helper 持有者）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvOwnershipTransferConfirmProcessor {

    @Inject
    ErpInvOwnershipTransferProcessor facade;

    public ErpInvOwnershipTransfer confirm(Long transferId, IServiceContext context) {
        ErpInvOwnershipTransfer transfer = facade.requireTransfer(transferId, context);
        facade.assertStatus(transfer, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT,
                ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
        facade.validateInvariants(transfer);
        transfer.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
        facade.transferDao().saveOrUpdateEntity(transfer);
        return transfer;
    }
}
