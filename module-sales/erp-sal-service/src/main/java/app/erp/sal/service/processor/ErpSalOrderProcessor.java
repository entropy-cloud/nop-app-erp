package app.erp.sal.service.processor;

import app.erp.fin.biz.IErpFinBudgetCommitmentBiz;
import app.erp.fin.biz.IErpFinIntercompanyTransferBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.biz.IErpInvStockBalanceBiz;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.SoDGuard;
import app.erp.sal.service.entity.CreditLimitChecker;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 销售订单审批状态机编排 Processor。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由本类全权处理：加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpSalOrderProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpSalOrderProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    @Inject
    IErpInvStockBalanceBiz stockBalanceBiz;

    @Inject
    CreditLimitChecker creditLimitChecker;

    @Inject
    IErpFinIntercompanyTransferBiz intercompanyTransferBiz;

    @Inject
    IErpFinBudgetCommitmentBiz budgetCommitmentBiz;

    @Inject
    ErpSalOrderSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpSalOrderApproveProcessor approveProcessor;

    @Inject
    ErpSalOrderRejectProcessor rejectProcessor;

    @Inject
    ErpSalOrderReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpSalOrderWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpSalOrderCancelProcessor cancelProcessor;

    public ErpSalOrder submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpSalOrder withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpSalOrder approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpSalOrder reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpSalOrder reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpSalOrder cancel(String orderId, IServiceContext context) {
        return cancelProcessor.cancel(orderId, context);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null) {
            status = ErpSalConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpSalConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(order, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(order, status, ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpSalOrder order, IServiceContext context) {
        String status = order.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(order, status, ErpSalConstants.APPROVE_STATUS_APPROVED);
        }
    }

    protected void validateTransitionForCancel(ErpSalOrder order, IServiceContext context) {
        String docStatus = order.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpSalConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(order, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpSalOrder order, IServiceContext context) {
        requireLinesNonEmpty(order, context);
        requireCustomerActive(order, context);
    }

    protected void validateBusinessRulesForApprove(ErpSalOrder order, IServiceContext context) {
        requireCustomerActive(order, context);
        creditLimitChecker.check(order.getCustomerId(), order.getTotalAmountWithTax(), order.getExchangeRate(),
                order.getCode(), context);
        validateOrderAvailability(order, context);
    }

    /**
     * 订单级可用量预校验（RC-R1.13，P1-RC-020）：可选前置校验，config-gated
     * {@code erp-sal.order-availability-check-level}（默认 OFF，部署启用时设置；对齐 credit-check-level 三级范式）。
     *
     * <p>per 订单行：行 {@code materialId} + 行 {@code warehouseId}（null 回退订单头 {@code warehouseId}，
     * 仍 null 跳过该行）；经 {@link IErpInvStockBalanceBiz} 聚合 {@code availableQuantity}（null 视为 0，
     * 无余额行保守按 0 计——HARD 下新物料无库存记录即拒绝）与行 {@code quantity} 比对：
     * WARN 级别不足时 LOG.warn 放行 / HARD 级别抛 {@link ErpSalErrors#ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT}。
     * 出库审核仍是强制校验点，本步骤只是可选预校验（不做预留/reservation，只读查询）。
     *
     * <p>跨域查询经 I*Biz 管道（ICrudBiz findList + QueryBean），天然经过 R1.29 组织隔离 transformer 过滤。
     */
    protected void validateOrderAvailability(ErpSalOrder order, IServiceContext context) {
        String level = resolveAvailabilityCheckLevel();
        if (ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_OFF.equals(level)) {
            return;
        }
        boolean hard = ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_HARD.equals(level);
        Long orderWarehouseId = order.getWarehouseId();
        for (ErpSalOrderLine line : loadLines(order.getId())) {
            Long materialId = line.getMaterialId();
            if (materialId == null) {
                continue;
            }
            Long warehouseId = line.getWarehouseId() != null ? line.getWarehouseId() : orderWarehouseId;
            if (warehouseId == null) {
                continue;
            }
            BigDecimal required = line.getQuantity();
            if (required == null || required.signum() <= 0) {
                continue;
            }
            BigDecimal available = resolveAvailableQuantity(materialId, warehouseId, context);
            if (available.compareTo(required) >= 0) {
                continue;
            }
            if (hard) {
                throw new NopException(ErpSalErrors.ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT)
                        .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                        .param(ErpSalErrors.ARG_LINE_NO, line.getLineNo())
                        .param(ErpSalErrors.ARG_MATERIAL_ID, materialId)
                        .param(ErpSalErrors.ARG_WAREHOUSE_ID, warehouseId)
                        .param(ErpSalErrors.ARG_AVAILABLE, available)
                        .param(ErpSalErrors.ARG_REQUIRED, required);
            }
            LOG.warn("销售订单 {} 第 {} 行物料 {}（仓库 {}）可用量 {} 不足需求 {}，订单级预校验 WARN 放行（出库审核仍会强制校验）",
                    order.getCode(), line.getLineNo(), materialId, warehouseId, available, required);
        }
    }

    protected String resolveAvailabilityCheckLevel() {
        String level = AppConfig.var(ErpSalConstants.CONFIG_ORDER_AVAILABILITY_CHECK_LEVEL,
                ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_OFF);
        return level == null ? ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_OFF : level;
    }

    protected BigDecimal resolveAvailableQuantity(Long materialId, Long warehouseId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        q.setLimit(1);
        List<ErpInvStockBalance> balances = stockBalanceBiz.findList(q, null, context);
        if (balances.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = balances.get(0).getAvailableQuantity();
        return available == null ? BigDecimal.ZERO : available;
    }

    // ---------- step：执行（状态推进 + 持久化） ----------

    protected void doSubmit(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_SUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doWithdrawSubmit(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        orderDao().updateEntity(order);
    }

    protected void doApprove(ErpSalOrder order, IServiceContext context) {
        SoDGuard.assertApproverNotCreator(order.getCreatedBy(), currentUserId(), ErpSalErrors.ERR_SAL_APPROVER_IS_CREATOR);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_APPROVED);
        order.setApprovedBy(currentUserId());
        order.setApprovedAt(CoreMetrics.currentTimestamp());
        orderDao().updateEntity(order);
    }

    protected void doReject(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        orderDao().updateEntity(order);
    }

    protected void doReverseApprove(ErpSalOrder order, IServiceContext context) {
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        order.setApprovedBy(null);
        order.setApprovedAt(null);
        orderDao().updateEntity(order);
    }

    protected void doCancel(ErpSalOrder order, IServiceContext context) {
        order.setDocStatus(ErpSalConstants.DOC_STATUS_CANCELLED);
        orderDao().updateEntity(order);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpSalOrder requireOrder(String id, IServiceContext context) {
        ErpSalOrder order = orderDao().getEntityById(id);
        if (order == null) {
            throw new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                    .param(ErpSalErrors.ARG_ORDER_CODE, id);
        }
        return order;
    }

    protected void validateNotCancelled(ErpSalOrder order, IServiceContext context) {
        if (order.isCancelled()) {
            throw illegalDocTransition(order, order.getDocStatus(), "非已作废");
        }
    }

    protected void requireLinesNonEmpty(ErpSalOrder order, IServiceContext context) {
        if (loadLines(order.getId()).isEmpty()) {
            throw new NopException(ErpSalErrors.ERR_ORDER_LINES_EMPTY)
                    .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode());
        }
    }

    protected void requireCustomerActive(ErpSalOrder order, IServiceContext context) {
        if (order.getCustomerId() == null) {
            return;
        }
        ErpMdPartner partner = mdPartnerBiz.findById(order.getCustomerId(), context);
        if (partner == null || partner.getStatus() == null
                || !Objects.equals(partner.getStatus(), ErpSalConstants.PARTNER_STATUS_ACTIVE)) {
            throw new NopException(ErpSalErrors.ERR_PARTNER_INACTIVE)
                    .param(ErpSalErrors.ARG_CUSTOMER_ID, order.getCustomerId());
        }
    }

    /**
     * UC-SAL-11：审核时审计 pricingSource 分布（不重取价，仅记录日志用于审计追踪）。
     */
    protected void auditPricingSourceDistribution(ErpSalOrder order, IServiceContext context) {
        List<ErpSalOrderLine> lines = loadLines(order.getId());
        int manual = 0, priceList = 0, promotion = 0, skuDefault = 0, unknown = 0;
        for (ErpSalOrderLine line : lines) {
            String src = line.getPricingSource();
            if (src == null || src.isEmpty()) {
                unknown++;
            } else if (Objects.equals(src, ErpSalConstants.PRICING_SOURCE_MANUAL)) {
                manual++;
            } else if (Objects.equals(src, ErpSalConstants.PRICING_SOURCE_PRICE_LIST)) {
                priceList++;
            } else if (Objects.equals(src, ErpSalConstants.PRICING_SOURCE_PROMOTION)) {
                promotion++;
            } else if (Objects.equals(src, ErpSalConstants.PRICING_SOURCE_SKU_DEFAULT)) {
                skuDefault++;
            } else {
                unknown++;
            }
        }
        LOG.debug("orderCode={} pricingSourceAudit: manual={} priceList={} promotion={} skuDefault={} unknown={}",
                order.getCode(), manual, priceList, promotion, skuDefault, unknown);
    }

    /**
     * 跨公司销售订单 intercompany approve 钩子（plan 2026-07-24-1351-2，multi-company.md §跨公司 PO/SO 触发路径）。
     * 订单审核后置 → 跨法人时生成配对内部销售/采购凭证。
     * config-gated（{@code erp-fin.intercompany-posting-enabled} 默认 false，SPI 自门控）；非阻塞 try-catch
     * （对齐 inventory confirm 范式，凭证生成失败不阻塞订单审核）。金额取 totalAmountWithTax（本位币）。
     */
    protected void runIntercompanyApproveHook(ErpSalOrder order, IServiceContext context) {
        if (order.getOrgId() == null || order.getBusinessDate() == null) {
            return;
        }
        BigDecimal amount = order.getTotalAmountWithTax() != null
                ? order.getTotalAmountWithTax() : BigDecimal.ZERO;
        try {
            intercompanyTransferBiz.onTradeDocumentApproved(
                    ErpFinConstants.INTERCOMPANY_DOC_TYPE_SALES_ORDER, order.getId(), order.getCode(),
                    order.getOrgId(), amount, order.getBusinessDate(), context);
        } catch (RuntimeException e) {
            LOG.warn("intercompany posting failed for sales order {}: {}", order.getCode(), e.getMessage());
        }
    }

    /**
     * 跨公司销售订单 intercompany 红冲钩子（plan 2026-07-24-1351-2）。
     * 反审核/作废 → 红冲原配对 intercompany 凭证。config-gated；无原凭证静默跳过；非阻塞 try-catch。
     */
    protected void runIntercompanyReverseHook(ErpSalOrder order, IServiceContext context) {
        try {
            intercompanyTransferBiz.onTradeDocumentReversed(
                    ErpFinConstants.INTERCOMPANY_DOC_TYPE_SALES_ORDER, order.getId(), order.getCode(), context);
        } catch (RuntimeException e) {
            LOG.warn("intercompany reversal failed for sales order {}: {}", order.getCode(), e.getMessage());
        }
    }

    /**
     * sales 承付 commit 钩子（plan 2026-07-24-1351-3，budget.md §sales 承付扩展 §接入点 #1）。
     * 订单审核通过后置 → 生成 postingType=COMMITMENT 凭证（billType=SALES_ORDER_COMMITMENT）。
     * config-gated（{@code erp-fin.budget-commitment-enabled} 默认 false）；科目/期间/金额缺失时静默跳过（不阻塞业务流）。
     * 科目经 {@code erp-fin.budget-commitment-sales-subject-code} 独立配置（与采购科目分离）。
     */
    protected void runCommitmentCommitHook(ErpSalOrder order, IServiceContext context) {
        if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE))) {
            return;
        }
        Long subjectId = resolveBudgetSubjectId(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_SALES_SUBJECT_CODE);
        if (subjectId == null) {
            return;
        }
        Long periodId = resolvePeriodId(order.getBusinessDate());
        BigDecimal amount = order.getTotalAmountWithTax() != null
                ? order.getTotalAmountWithTax() : BigDecimal.ZERO;
        budgetCommitmentBiz.commit(
                ErpFinConstants.COMMITMENT_SOURCE_BILL_SALES_ORDER, order.getCode(),
                subjectId, null, periodId, amount, context);
    }

    /**
     * sales 承付 release 钩子（plan 2026-07-24-1351-3，budget.md §sales 承付扩展 §接入点 #2 release-on-cancel）。
     * 订单反审核/作废 → 红冲原 COMMITMENT 凭证。
     * config-gated；无原凭证静默跳过（reverseApprove/cancel 路径容错，避免阻塞业务流）。
     */
    protected void runCommitmentReleaseHook(ErpSalOrder order, IServiceContext context) {
        if (!Boolean.TRUE.equals(AppConfig.var(ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE))) {
            return;
        }
        try {
            budgetCommitmentBiz.release(
                    ErpFinConstants.COMMITMENT_SOURCE_BILL_SALES_ORDER, order.getCode(), context);
        } catch (NopException e) {
            // 容错：无原凭证（ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）静默跳过
            LOG.debug("commitment release skipped for sales order {}: {}", order.getCode(), e.getMessage());
        }
    }

    protected Long resolveBudgetSubjectId(String configKey) {
        String code = AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            return null;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    protected Long resolvePeriodId(LocalDate businessDate) {
        if (businessDate == null) {
            return null;
        }
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(le("startDate", businessDate));
        q.addFilter(ge("endDate", businessDate));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    protected List<ErpSalOrderLine> loadLines(Long orderId) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpSalOrder> orderDao() {
        return daoProvider.daoFor(ErpSalOrder.class);
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpSalOrder order, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpSalErrors.ARG_CURRENT_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpSalOrder order, String current, String expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, order.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
