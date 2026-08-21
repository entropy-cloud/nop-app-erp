
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstCipBiz;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;
import app.erp.ast.service.processor.ErpAstCipAddCostItemProcessor;
import app.erp.ast.service.processor.ErpAstCipAddProgressBillingProcessor;
import app.erp.ast.service.processor.ErpAstCipProcessor;
import app.erp.ast.service.processor.ErpAstCipReverseTransferProcessor;
import app.erp.ast.service.processor.ErpAstCipStartConstructionProcessor;
import app.erp.ast.service.processor.ErpAstCipTransferToAssetProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 在建工程（CIP）BizModel（Facade）。三态状态机 + 成本归集 + 进度付款 + 完工转固/部分转固 +
 * reverseTransfer 委托对应 per-mutation Processor（R6.3 拆分，protected step 方法，下游可逐 step 覆盖）。
 * `:45` 只读查询（findCostItems/findProgressBillings）保留委托 {@link ErpAstCipProcessor}。
 *
 * <p>语义见 {@code docs/design/assets/cip.md}；状态字典 {@code erp-ast/cip-status}。
 */
@BizModel("ErpAstCip")
public class ErpAstCipBizModel extends CrudBizModel<ErpAstCip> implements IErpAstCipBiz {

    @Inject
    ErpAstCipProcessor cipProcessor;

    @Inject
    ErpAstCipStartConstructionProcessor startConstructionProcessor;

    @Inject
    ErpAstCipAddCostItemProcessor addCostItemProcessor;

    @Inject
    ErpAstCipAddProgressBillingProcessor addProgressBillingProcessor;

    @Inject
    ErpAstCipTransferToAssetProcessor transferToAssetProcessor;

    @Inject
    ErpAstCipReverseTransferProcessor reverseTransferProcessor;

    public ErpAstCipBizModel() {
        setEntityName(ErpAstCip.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstCip startConstruction(@Name("cipId") String cipId, IServiceContext context) {
        return startConstructionProcessor.startConstruction(cipId, context);
    }

    @Override
    @BizMutation
    public ErpAstCipCostItem addCostItem(@Name("cipId") String cipId,
                                         @Name("costType") String costType,
                                         @Name("amountFunctional") BigDecimal amountFunctional,
                                         @Name("sourceBillType") String sourceBillType,
                                         @Name("sourceBillCode") String sourceBillCode,
                                         @Name("remark") String remark,
                                         IServiceContext context) {
        return addCostItemProcessor.addCostItem(cipId, costType, amountFunctional, sourceBillType, sourceBillCode,
                remark, context);
    }

    @Override
    @BizMutation
    public ErpAstCipProgressBilling addProgressBilling(@Name("cipId") String cipId,
                                                       @Name("billingDate") LocalDate billingDate,
                                                       @Name("billingMilestone") String billingMilestone,
                                                       @Name("amountFunctional") BigDecimal amountFunctional,
                                                       @Name("paymentVoucherCode") String paymentVoucherCode,
                                                       IServiceContext context) {
        return addProgressBillingProcessor.addProgressBilling(cipId, billingDate, billingMilestone, amountFunctional,
                paymentVoucherCode, context);
    }

    @Override
    @BizQuery
    public List<ErpAstCipCostItem> findCostItems(@Name("cipId") String cipId,
                                                  @Name("onlyUntransferred") boolean onlyUntransferred,
                                                  IServiceContext context) {
        return cipProcessor.findCostItems(cipId, onlyUntransferred, context);
    }

    @Override
    @BizQuery
    public List<ErpAstCipProgressBilling> findProgressBillings(@Name("cipId") String cipId,
                                                                IServiceContext context) {
        return cipProcessor.findProgressBillings(cipId, context);
    }

    @Override
    @BizMutation
    public ErpAstCip transferToAsset(@Name("cipId") String cipId,
                                     @Name("costItemIds") List<String> costItemIds,
                                     @Name("transferDate") LocalDate transferDate,
                                     IServiceContext context) {
        return transferToAssetProcessor.transferToAsset(cipId, costItemIds, transferDate, context);
    }

    @Override
    @BizMutation
    public ErpAstCip reverseTransfer(@Name("cipId") String cipId,
                                     @Name("capitalizationId") String capitalizationId,
                                     IServiceContext context) {
        return reverseTransferProcessor.reverseTransfer(cipId, capitalizationId, context);
    }
}
