
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IErpAstCipBiz extends ICrudBiz<ErpAstCip> {

    /**
     * CIP 草稿 → 建设中（DRAFT → IN_CONSTRUCTION）。
     */
    @BizMutation
    ErpAstCip startConstruction(@Name("cipId") Long cipId, IServiceContext context);

    /**
     * CIP 成本归集行录入（仅 IN_CONSTRUCTION 状态允许）。累加 CIP.accumulatedCost。
     * INTEREST_CAPITALIZATION 类型受配置 {@code erp-ast.cip-interest-capitalization-enabled} 控制。
     */
    @BizMutation
    ErpAstCipCostItem addCostItem(@Name("cipId") Long cipId,
                                  @Name("costType") String costType,
                                  @Name("amountFunctional") BigDecimal amountFunctional,
                                  @Name("sourceBillType") String sourceBillType,
                                  @Name("sourceBillCode") String sourceBillCode,
                                  @Name("remark") String remark,
                                  IServiceContext context);

    /**
     * CIP 进度付款记录（仅 IN_CONSTRUCTION 允许）。进度款本身不转固，只作已付工程款记录。
     */
    @BizMutation
    ErpAstCipProgressBilling addProgressBilling(@Name("cipId") Long cipId,
                                                @Name("billingDate") LocalDate billingDate,
                                                @Name("billingMilestone") String billingMilestone,
                                                @Name("amountFunctional") BigDecimal amountFunctional,
                                                @Name("paymentVoucherCode") String paymentVoucherCode,
                                                IServiceContext context);

    /**
     * 查询 CIP 成本归集行（按 cipId）。{@code onlyUntransferred=true} 仅返回未转固行。
     */
    @BizQuery
    List<ErpAstCipCostItem> findCostItems(@Name("cipId") Long cipId,
                                          @Name("onlyUntransferred") boolean onlyUntransferred,
                                          IServiceContext context);

    /**
     * 查询 CIP 进度付款记录（按 cipId）。
     */
    @BizQuery
    List<ErpAstCipProgressBilling> findProgressBillings(@Name("cipId") Long cipId,
                                                        IServiceContext context);

    /**
     * CIP 完工转固（IN_CONSTRUCTION → TRANSFERRED）。
     * 汇总所选 CostItem → 调既有资本化通道建卡 + 出 CAPITALIZATION(80) 凭证 → 回写 CIP 终态。
     * {@code costItemIds=null} 表示全部 CostItem。
     */
    @BizMutation
    ErpAstCip transferToAsset(@Name("cipId") Long cipId,
                              @Name("costItemIds") List<Long> costItemIds,
                              @Name("transferDate") LocalDate transferDate,
                              IServiceContext context);

    /**
     * CIP 转固红字冲销：调既有 {@code IErpAstAssetCapitalizationBiz.reverseApprove}
     * → 回退 CostItem.postedTransferFlag → CIP 从 TRANSFERRED 回 IN_CONSTRUCTION（仅全部红冲）。
     */
    @BizMutation
    ErpAstCip reverseTransfer(@Name("cipId") Long cipId,
                              @Name("capitalizationId") Long capitalizationId,
                              IServiceContext context);
}
