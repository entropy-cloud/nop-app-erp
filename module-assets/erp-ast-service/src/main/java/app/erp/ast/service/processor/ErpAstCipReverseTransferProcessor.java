package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

/**
 * ErpAstCip reverseTransfer per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含转固红字冲销编排；共享 protected helper 单一真相源在 {@link ErpAstCipProcessor}（slim-to-query-only facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstCipReverseTransferProcessor {

    @Inject
    ErpAstCipProcessor facade;

    @Inject
    ErpAstAssetCapitalizationProcessor capitalizationProcessor;

    public ErpAstCip reverseTransfer(String cipId, String capitalizationId, IServiceContext context) {
        ErpAstCip cip = facade.requireCip(cipId, context);
        if (!Objects.equals(cip.getStatus(), ErpAstConstants.CIP_STATUS_TRANSFERRED)) {
            throw facade.illegalTransition(cip, cip.getStatus(), ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        }

        List<ErpAstCipCostItem> capCostItems = facade.findCostItemsByCapitalization(capitalizationId);
        List<ErpAstCipCostItem> allCipCostItems = facade.findCostItems(cipId, false, context);
        if (capCostItems.size() < allCipCostItems.size()) {
            throw new NopException(ErpAstErrors.ERR_CIP_PARTIAL_REVERSE_NOT_SUPPORTED)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
        }

        capitalizationProcessor.reverseApprove(capitalizationId, context);

        IEntityDao<ErpAstCipCostItem> dao = facade.costItemDao();
        for (ErpAstCipCostItem item : capCostItems) {
            ErpAstCipCostItem managed = dao.getEntityById(item.getId());
            managed.setPostedTransferFlag(false);
            managed.setCapitalizationId(null);
        }

        ErpAstCip managedCip = facade.cipDao().getEntityById(cip.getId());
        managedCip.setStatus(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        managedCip.setIsCompleted(false);
        managedCip.setCompletedAssetId(null);
        facade.orm().flushSession();
        return managedCip;
    }
}
