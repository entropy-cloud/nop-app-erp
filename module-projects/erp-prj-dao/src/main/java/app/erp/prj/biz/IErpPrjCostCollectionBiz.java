package app.erp.prj.biz;

import app.erp.prj.dao.entity.ErpPrjCostCollection;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;

/**
 * 项目成本归集 Biz 契约。CRUD 之上承载费用报销归集接入 + 物料（采购入库）归集接入 + 标准审批动作（{@link IApprovableBiz}）：
 * <ul>
 *   <li>{@link #refreshExpenseCost(Long, IServiceContext)}：projects 驱动只读聚合——
 *       经 {@code IErpFinExpenseClaimBiz} 只读查已审核报销单（行 projectId 命中），
 *       projects 自写 {@code erp_prj_cost_collection} 行（对齐 data-dependency-matrix
 *       §3.2/§4.2：finance 从不写业务表，归集由 projects 触发）。</li>
 *   <li>{@link #aggregateMaterialCost(Long, BigDecimal, String, IServiceContext)}：跨域物料归集入口
 *       （RC-R1.61 / P1-RC-049）——purchase 侧入库移动单生成后逐行调本方法（行级 projectId 已解析），
 *       projects 侧经 {@code IErpPrjProjectBiz.requireReferenceable} 单一咽喉守卫 + 幂等去重
 *       （sourceBillType=PURCHASE_RECEIVE + sourceBillCode）+ 预算检查（STRICT 拒绝）后自写归集行
 *       （costCategory=MATERIAL）。</li>
 * </ul>
 *
 * <p>费用归集受 {@code erp-prj.expense-aggregation-enabled}（默认 true）config-gated。
 * 关闭时 {@code refreshExpenseCost} 直接返回 0。物料归集受 {@code erp-prj.material-aggregation-enabled}
 * （默认 true）config-gated，关闭时 {@code aggregateMaterialCost} 直接返回 0。
 */
public interface IErpPrjCostCollectionBiz extends ICrudBiz<ErpPrjCostCollection> {

    /**
     * 刷新项目的费用报销归集。返回本次新增的归集金额合计。
     */
    @BizMutation
    BigDecimal refreshExpenseCost(@Name("projectId") Long projectId, IServiceContext context);

    /**
     * 跨域物料归集入口（采购入库→项目，RC-R1.61）。purchase 侧入库移动单生成后按行调用：
     * 行级 projectId 解析在 purchase 侧完成（{@code ErpPurReceiveLine.orderLineId → ErpPurOrderLine.projectId}），
     * 本方法在 projects 侧完成守卫链（requireReferenceable → 幂等 → 预算检查 STRICT 拒绝）后
     * 自写归集行（costCategory=MATERIAL / sourceBillType=PURCHASE_RECEIVE / sourceBillCode）。
     * 返回本次新增的归集金额；幂等命中或 config 关闭返回 0。
     *
     * @param projectId    项目 id（行级解析值，null 直接返回 0）
     * @param amount       归集金额（入库行金额不含税）
     * @param sourceBillCode 来源单据行标识（{@code 入库单号-行号}，幂等去重键之一）
     */
    @BizMutation
    BigDecimal aggregateMaterialCost(@Name("projectId") Long projectId,
                                     @Name("amount") BigDecimal amount,
                                     @Name("sourceBillCode") String sourceBillCode,
                                     IServiceContext context);
}
