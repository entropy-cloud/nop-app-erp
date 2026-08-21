package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

/**
 * ErpAstCip transferToAsset per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含完工转固编排（全部 + 部分转固）；共享 protected step 单一真相源在 {@link ErpAstCipProcessor}（slim-to-query-only facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstCipTransferToAssetProcessor {

    @Inject
    ErpAstCipProcessor facade;

    public ErpAstCip transferToAsset(String cipId, List<String> costItemIds, LocalDate transferDate,
                                       IServiceContext context) {
        ErpAstCip cip = facade.requireCip(cipId, context);
        List<ErpAstCipCostItem> costItems = facade.resolveCostItems(cip, costItemIds);
        facade.validateTransferable(cip, costItems, context);
        ErpAstAssetCapitalization cap = facade.buildCapitalizationRequest(cip, costItems, transferDate, context);
        ErpAstAssetCapitalization approved = facade.doTransfer(cap, cip, costItems, context);
        ErpAstCip managedCip = facade.cipDao().getEntityById(cipId);
        facade.postProcess(managedCip, costItems, approved, context);
        facade.cipDao().saveOrUpdateEntity(managedCip);
        facade.orm().flushSession();
        return managedCip;
    }
}
