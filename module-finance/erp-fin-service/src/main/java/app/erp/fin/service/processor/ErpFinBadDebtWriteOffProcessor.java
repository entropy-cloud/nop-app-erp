package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.ErpFinConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinBadDebt writeOff per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含核销编排；共享 protected helper 单一真相源在 {@link ErpFinBadDebtProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinBadDebtWriteOffProcessor {

    @Inject
    ErpFinBadDebtProcessor facade;

    public ErpFinBadDebt writeOff(Long arApItemId, String reason, IServiceContext context) {
        ErpFinArApItem item = facade.requireOpenArApItem(arApItemId);
        ErpFinBadDebt debt = facade.newBadDebt(ErpFinConstants.BAD_DEBT_TYPE_WRITE_OFF, item,
                item.getOpenAmountFunctional(), reason);
        if (!facade.isWriteOffApprovalRequired()) {
            facade.executeWriteOff(debt, item, context);
            debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        }
        facade.badDebtDao().saveEntity(debt);
        return debt;
    }
}
