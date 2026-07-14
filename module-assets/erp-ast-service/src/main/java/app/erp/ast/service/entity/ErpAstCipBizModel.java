
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstCipBiz;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;
import app.erp.ast.service.processor.ErpAstCipProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 在建工程（CIP）BizModel（Facade）。三态状态机 + 成本归集 + 进度付款 + 完工转固/部分转固 +
 * reverseTransfer 委托 {@link ErpAstCipProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code docs/design/assets/cip.md}；状态字典 {@code erp-ast/cip-status}。
 */
@BizModel("ErpAstCip")
public class ErpAstCipBizModel extends CrudBizModel<ErpAstCip> implements IErpAstCipBiz {

    @Inject
    ErpAstCipProcessor cipProcessor;

    public ErpAstCipBizModel() {
        setEntityName(ErpAstCip.class.getName());
    }

    @BizLoader(forType = ErpAstCip.class)
    public List<String> orgName(@ContextSource List<ErpAstCip> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCip row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCip.class)
    public List<String> categoryName(@ContextSource List<ErpAstCip> rows) {
        orm().batchLoadProps(rows, Collections.singleton("category"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCip row : rows) {
            result.add(row.getCategory() != null ? row.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCip.class)
    public List<String> currencyName(@ContextSource List<ErpAstCip> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCip row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstCip.class)
    public List<String> completedAssetCode(@ContextSource List<ErpAstCip> rows) {
        orm().batchLoadProps(rows, Collections.singleton("completedAsset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstCip row : rows) {
            result.add(row.getCompletedAsset() != null ? row.getCompletedAsset().getCode() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    public ErpAstCip startConstruction(@Name("cipId") Long cipId, IServiceContext context) {
        return cipProcessor.startConstruction(cipId, context);
    }

    @Override
    @BizMutation
    public ErpAstCipCostItem addCostItem(@Name("cipId") Long cipId,
                                         @Name("costType") String costType,
                                         @Name("amountFunctional") BigDecimal amountFunctional,
                                         @Name("sourceBillType") String sourceBillType,
                                         @Name("sourceBillCode") String sourceBillCode,
                                         @Name("remark") String remark,
                                         IServiceContext context) {
        return cipProcessor.addCostItem(cipId, costType, amountFunctional, sourceBillType, sourceBillCode,
                remark, context);
    }

    @Override
    @BizMutation
    public ErpAstCipProgressBilling addProgressBilling(@Name("cipId") Long cipId,
                                                       @Name("billingDate") LocalDate billingDate,
                                                       @Name("billingMilestone") String billingMilestone,
                                                       @Name("amountFunctional") BigDecimal amountFunctional,
                                                       @Name("paymentVoucherCode") String paymentVoucherCode,
                                                       IServiceContext context) {
        return cipProcessor.addProgressBilling(cipId, billingDate, billingMilestone, amountFunctional,
                paymentVoucherCode, context);
    }

    @Override
    @BizQuery
    public List<ErpAstCipCostItem> findCostItems(@Name("cipId") Long cipId,
                                                  @Name("onlyUntransferred") boolean onlyUntransferred,
                                                  IServiceContext context) {
        return cipProcessor.findCostItems(cipId, onlyUntransferred, context);
    }

    @Override
    @BizQuery
    public List<ErpAstCipProgressBilling> findProgressBillings(@Name("cipId") Long cipId,
                                                                IServiceContext context) {
        return cipProcessor.findProgressBillings(cipId, context);
    }

    @Override
    @BizMutation
    public ErpAstCip transferToAsset(@Name("cipId") Long cipId,
                                     @Name("costItemIds") List<Long> costItemIds,
                                     @Name("transferDate") LocalDate transferDate,
                                     IServiceContext context) {
        return cipProcessor.transferToAsset(cipId, costItemIds, transferDate, context);
    }

    @Override
    @BizMutation
    public ErpAstCip reverseTransfer(@Name("cipId") Long cipId,
                                     @Name("capitalizationId") Long capitalizationId,
                                     IServiceContext context) {
        return cipProcessor.reverseTransfer(cipId, capitalizationId, context);
    }
}
