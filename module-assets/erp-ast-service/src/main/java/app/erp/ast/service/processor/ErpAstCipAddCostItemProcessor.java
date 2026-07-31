package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * ErpAstCip addCostItem per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含成本归集编排；共享 protected helper 单一真相源在 {@link ErpAstCipProcessor}（slim-to-query-only facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstCipAddCostItemProcessor {

    @Inject
    ErpAstCipProcessor facade;

    public ErpAstCipCostItem addCostItem(Long cipId, String costType, BigDecimal amountFunctional,
                                          String sourceBillType, String sourceBillCode, String remark,
                                          IServiceContext context) {
        ErpAstCip cip = facade.requireCip(cipId, context);
        facade.requireInConstruction(cip);
        facade.validateCostType(costType, cip);
        facade.validateAmountPositive(amountFunctional, cip);
        if (Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_INTEREST_CAPITALIZATION)
                && !facade.isInterestCapitalizationEnabled()) {
            throw new NopException(ErpAstErrors.ERR_CIP_INTEREST_CAPITALIZATION_DISABLED)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
        }

        BigDecimal exchangeRate = ErpAstCipProcessor.nz(cip.getExchangeRate(), BigDecimal.ONE);
        BigDecimal amountSource = amountFunctional.divide(exchangeRate, ErpAstCipProcessor.SCALE, RoundingMode.HALF_UP);

        IEntityDao<ErpAstCipCostItem> dao = facade.costItemDao();
        ErpAstCipCostItem item = dao.newEntity();
        item.setCipId(cip.getId());
        item.setOrgId(cip.getOrgId());
        item.setLineNo(facade.nextCostItemLineNo(cip.getId()));
        item.setCostType(costType);
        item.setAmountFunctional(amountFunctional);
        item.setExchangeRate(exchangeRate);
        item.setAmountSource(amountSource);
        item.setCurrencyId(cip.getCurrencyId());
        item.setSourceBillType(sourceBillType);
        item.setSourceBillCode(sourceBillCode);
        item.setPostedTransferFlag(false);
        item.setBusinessDate(CoreMetrics.today());
        item.setRemark(remark);
        dao.saveEntity(item);

        cip.setAccumulatedCost(ErpAstCipProcessor.nz(cip.getAccumulatedCost(), BigDecimal.ZERO).add(amountFunctional));
        facade.cipDao().saveOrUpdateEntity(cip);
        facade.orm().flushSession();
        return item;
    }
}
