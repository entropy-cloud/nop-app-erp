package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.ErpFinConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinBadDebt recover per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含收回恢复编排；共享 protected helper 单一真相源在 {@link ErpFinBadDebtProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinBadDebtRecoverProcessor {

    @Inject
    ErpFinBadDebtProcessor facade;

    public ErpFinBadDebt recover(String arApItemId, String reason, IServiceContext context) {
        ErpFinArApItem item = facade.requireWrittenOffArApItem(arApItemId);
        ErpFinBadDebt debt = facade.newBadDebt(ErpFinConstants.BAD_DEBT_TYPE_RECOVERY, item,
                facade.debtAmountOf(item), reason);
        if (!facade.isWriteOffApprovalRequired()) {
            facade.executeRecovery(debt, item, context);
            debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        }
        facade.badDebtDao().saveEntity(debt);
        return debt;
    }
}
