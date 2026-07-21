package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;

import java.math.BigDecimal;

/**
 * 预算承付占用/释放跨域 SPI（A2，plan 2026-07-21-1206-2，budget.md §承付会计 §承付占用/释放 SPI）。
 *
 * <p>严格对齐 {@code budget.md §业务规则3}："采购订单 APPROVED 时生成 postingType=COMMITMENT 凭证；
 * 订单 CANCELLED 或被发票接收时红冲 COMMITMENT"。**3 接入点**（commit / release-on-cancel / release-on-invoice-approve）
 * 经本 SPI 由采购域调用；reject release-receive-complete（ErpPurReceive 入库路径）——入库是库存移动不产生 AP ACTUAL 占用。
 *
 * <p>实现：{@code ErpFinBudgetCommitmentBizModel}（finance-service），config-gated
 * （{@code erp-fin.budget-commitment-enabled} 默认 false，向后兼容保护既有 113 purchase 测试）。
 *
 * <p>事务边界：commit 与既有 {@link IErpFinBudgetControlBiz#check} SYNC 同事务（强一致）；
 * release SYNC 同事务（与既有 reverseApprove 同事务；不走事件总线 ASYNC，避免事务跨域复杂度）。
 *
 * <p>本接口位于 finance-dao（跨层契约面），供 purchase 域注入。
 */
public interface IErpFinBudgetCommitmentBiz {

    /**
     * 占用预算：生成 postingType=COMMITMENT 凭证（Dr 预算占用科目 / Cr 应付-承付）。
     *
     * <p>调用点：{@code ErpPurOrder.approve} 后置 hook。
     *
     * @param sourceBillType  触发单据类型（如 PURCHASE_ORDER）
     * @param sourceBillCode  触发单据号（用于业财回链反查）
     * @param subjectId       预算科目（承付占用落账科目）
     * @param costCenterId    成本中心（可空）
     * @param periodId        会计期间
     * @param amount          占用金额（本位币）
     * @param context         服务上下文
     * @return 承付凭证 ID（commitmentVoucherId）；config-gated 关闭或参数非法时返回 null
     */
    @BizMutation
    Long commit(@Name("sourceBillType") String sourceBillType,
                @Name("sourceBillCode") String sourceBillCode,
                @Name("subjectId") Long subjectId,
                @Name("costCenterId") Long costCenterId,
                @Name("periodId") Long periodId,
                @Name("amount") BigDecimal amount,
                IServiceContext context);

    /**
     * 释放预算：红冲原 COMMITMENT 凭证（金额取负）。
     *
     * <p>调用点（严格对齐 budget.md:78）：
     * <ul>
     *   <li>{@code ErpPurOrder.reverseApprove} / {@code cancel}（订单取消路径 release-on-cancel）</li>
     *   <li>{@code ErpPurInvoice.approve}（AP 发票过账 = 实际占用产生 = 释放承付 release-on-invoice-approve）</li>
     * </ul>
     * <b>reject release-receive-complete（ErpPurReceive 入库路径）</b>——入库是库存移动不产生 AP ACTUAL 占用。
     *
     * @param sourceBillType  触发单据类型（与 commit 时一致）
     * @param sourceBillCode  触发单据号（用于反查原承付凭证）
     * @param context         服务上下文
     * @return 红冲凭证 ID（reversalVoucherId）；config-gated 关闭或无原凭证可红冲时返回 null
     * @throws app.erp.fin.service.ErpFinErrors#ERR_BUDGET_COMMITMENT_ALREADY_RELEASED
     *         当原承付凭证已红冲或不存在时抛出（守卫防重复占用预算）
     */
    @BizMutation
    Long release(@Name("sourceBillType") String sourceBillType,
                 @Name("sourceBillCode") String sourceBillCode,
                 IServiceContext context);
}
