package app.erp.mfg.service.processor;

import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.biz.IErpMfgWorkOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.entity.MaterialIssueStockMoveBuilder;
import app.erp.mfg.service.posting.ManufacturingIssuePostingDispatcher;
import app.erp.mfg.service.statemachine.ErpMfgMaterialIssueStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 领料单 confirm/reverseConfirm per-mutation Processor 共享基类（R6.2）。持有领料出库编排所需的共享注入与
 * protected helper（实体加载/状态守卫/同聚合子表查询/成本回写/移动单反查），供
 * {@link ErpMfgMaterialIssueConfirmProcessor} 与 {@link ErpMfgMaterialIssueReverseConfirmProcessor} 复用。
 *
 * <p>同域持久化用 {@link IDaoProvider}（Processor 非 BizModel，对齐既有 mfg Processor 层范式）。
 * 事务边界跟随 Facade {@code @BizMutation} 事务，本类不带 {@code @Transactional}。
 *
 * <p>权威：{@code docs/design/manufacturing/state-machine.md}、{@code docs/design/inventory/cross-domain.md}。
 */
public class AbstractErpMfgMaterialIssueProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpInvStockLedgerBiz stockLedgerBiz;
    @Inject
    IErpMfgWorkOrderBiz workOrderBiz;
    @Inject
    IErpMfgWorkOrderLineBiz workOrderLineBiz;
    @Inject
    MaterialIssueStockMoveBuilder stockMoveBuilder;
    @Inject
    ManufacturingIssuePostingDispatcher issuePostingDispatcher;
    @Inject
    ErpMfgMaterialIssueStateMachine stateMachine;

    // ---------- 实体加载/守卫（protected，供派生复用与覆盖） ----------

    protected ErpMfgMaterialIssue requireIssue(Long issueId, IServiceContext context) {
        ErpMfgMaterialIssue issue = issueDao().getEntityById(issueId);
        if (issue == null) {
            throw new NopException(ErpMfgErrors.ERR_ISSUE_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_ISSUE_ID, issueId);
        }
        return issue;
    }

    /**
     * confirm 路径固定来源态守卫（plan 2026-08-14-0930-1 M4.39）：委托
     * {@link ErpMfgMaterialIssueStateMachine#assertCanConfirm(String)}，非法边由 Bean 抛 common 层码，
     * 此处映射领域码 {@code ERR_INVALID_STATUS_TRANSITION}（misnamed 复用，common 码作 cause）。
     *
     * <p>已 DONE 的幂等短路（重复确认空操作）为动态幂等守卫，保留在 {@code ErpMfgMaterialIssueConfirmProcessor} 原位。
     */
    protected void validateTransition(ErpMfgMaterialIssue issue, IServiceContext context) {
        String status = issue.getDocStatus();
        try {
            stateMachine.assertCanConfirm(status);
        } catch (NopException e) {
            throw illegalTransition(issue, status, "DRAFT");
        }
    }

    protected NopException illegalTransition(ErpMfgMaterialIssue issue, String current, String expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, issue.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- 同聚合子表/关联查询（protected） ----------

    /**
     * 加载领料单行（同聚合子表，父领料单已由 requireIssue 经管道授权）。
     */
    protected List<ErpMfgMaterialIssueLine> loadLines(Long issueId) {
        IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("issueId", issueId));
        q.addOrderField("lineNo", false);
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    /**
     * 反查领料单关联的 OUTGOING 移动单（按 {@code relatedBillType=ERP_MFG_ISSUE}+{@code relatedBillCode=issue.code}）。
     * 不存在返回 null（红冲步骤对此容忍）。
     */
    protected ErpInvStockMove findIssueMove(String issueCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE));
        q.addFilter(eq("relatedBillCode", issueCode));
        q.setLimit(1);
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected void recomputeTotals(ErpMfgWorkOrder wo) {
        BigDecimal total = nz(wo.getMaterialCost()).add(nz(wo.getLaborCost()))
                .add(nz(wo.getOverheadCost())).add(nz(wo.getSubcontractCost()));
        wo.setTotalCost(total);
        BigDecimal completed = nz(wo.getCompletedQuantity());
        wo.setUnitCost(completed.signum() != 0 ? total.divide(completed, 4, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpMfgMaterialIssue> issueDao() {
        return daoProvider.daoFor(ErpMfgMaterialIssue.class);
    }
}
