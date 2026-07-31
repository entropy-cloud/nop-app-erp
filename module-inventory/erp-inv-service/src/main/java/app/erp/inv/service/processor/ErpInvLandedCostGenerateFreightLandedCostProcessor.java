package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.ErpInvErrors;
import app.erp.pur.dao.entity.ErpPurReceive;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpInvLandedCost generateFreightLandedCost per-mutation Processor（R6.4，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 path-2 运费→到岸成本自动创建编排：解析入库单 → 防重 → 建头 → 建费用行。共享 protected helper 单一真相源在
 * {@link ErpInvLandedCostProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpInvLandedCostGenerateFreightLandedCostProcessor {

    @Inject
    ErpInvLandedCostProcessor facade;

    public ErpInvLandedCost generateFreightLandedCost(String receiveCode, BigDecimal freightAmount,
                                                       Long freightCurrencyId, BigDecimal freightExchangeRate,
                                                       IServiceContext context) {
        ErpPurReceive receive = resolveReceive(receiveCode);
        facade.validateNoDraftExists(receive.getId());

        Long currencyId = freightCurrencyId != null ? freightCurrencyId : receive.getCurrencyId();
        BigDecimal exchangeRate = facade.resolveExchangeRate(freightExchangeRate, freightCurrencyId, receive);

        ErpInvLandedCost landedCost = facade.createLandedCostHead(receive, freightAmount, currencyId, exchangeRate);
        facade.createFreightLine(landedCost, freightAmount, receive.getSupplierId());
        return landedCost;
    }

    protected ErpPurReceive resolveReceive(String receiveCode) {
        ErpPurReceive receive = facade.loadReceiveByCode(receiveCode);
        if (receive == null) {
            throw new NopException(ErpInvErrors.ERR_LANDED_COST_RECEIVE_NOT_FOUND)
                    .param("receiveCode", receiveCode);
        }
        return receive;
    }
}
