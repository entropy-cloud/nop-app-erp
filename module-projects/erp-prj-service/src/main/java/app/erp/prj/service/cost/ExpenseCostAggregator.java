package app.erp.prj.service.cost;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 费用报销归集聚合器（projects 驱动只读聚合）。对齐 {@code data-dependency-matrix.md §3.2:160}
 * 「finance 对业务域是纯读——从不写业务表」+ {@code §4.2:217} 成本归集为 projects 触发：
 * <ul>
 *   <li>projects 经 {@link IErpFinExpenseClaimBiz} <b>只读</b>查询已审核报销单（projects→finance R 读）。</li>
 *   <li>projects <b>自写</b> {@code erp_prj_cost_collection}（归集表由 projects 域写入，非 finance 回写）。</li>
 *   <li>幂等：按 {@code sourceBillType=EXPENSE + sourceBillCode=报销单号} 去重。</li>
 * </ul>
 *
 * <p>归集行：{@code costCategory=EXPENSE}、{@code sourceBillType=EXPENSE}、
 * {@code sourceBillCode=报销单号}、{@code amount=行金额(不含税)}、{@code subjectId=行科目}。
 */
public class ExpenseCostAggregator {

    /** finance 域审核通过状态值（对齐 ErpFinConstants.APPROVE_STATUS_APPROVED）。 */
    private static final String FIN_APPROVE_STATUS_APPROVED = ErpFinConstants.APPROVE_STATUS_APPROVED;
    /** finance 域单据作废状态值（对齐 ErpFinConstants.DOC_STATUS_CANCELLED）。 */
    private static final String FIN_DOC_STATUS_CANCELLED = ErpFinConstants.DOC_STATUS_CANCELLED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpFinExpenseClaimBiz expenseClaimBiz;

    /**
     * 刷新项目的费用报销归集。扫描所有已审核报销单中 projectId 命中的行，
     * 幂等地写入归集行 + 增量回写 actualCost。返回本次新增的归集金额合计。
     */
    public BigDecimal refreshExpenseCost(Long projectId) {
        if (projectId == null) {
            return BigDecimal.ZERO;
        }
        ErpPrjProject project = loadProject(projectId);
        if (project == null) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }

        // 只读查询已审核且未作废的报销单（projects→finance R 读，对齐 matrix §3.2）
        List<ErpFinExpenseClaim> approvedClaims = findApprovedClaims();
        if (approvedClaims.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 第一遍：收集待新增的归集行（幂等去重）
        List<PendingExpenseLine> pending = new java.util.ArrayList<>();
        for (ErpFinExpenseClaim claim : approvedClaims) {
            List<ErpFinExpenseClaimLine> lines = findLinesForProject(claim.getId(), projectId);
            for (ErpFinExpenseClaimLine line : lines) {
                String sourceBillCode = claim.getCode();
                if (existsLine(ErpPrjConstants.SOURCE_BILL_TYPE_EXPENSE, sourceBillCode)) {
                    continue;
                }
                pending.add(new PendingExpenseLine(sourceBillCode, nz(line.getAmountWithoutTax()),
                        line.getSubjectId()));
            }
        }
        if (pending.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal addedTotal = BigDecimal.ZERO;
        for (PendingExpenseLine p : pending) {
            addedTotal = addedTotal.add(p.amount);
        }

        // 找已有头；不存在则新建（totalAmount 在 save 前设好，避免 SAVING 态 updateEntity 违例）
        ErpPrjCostCollection existingHead = findHead(projectId);
        if (existingHead != null) {
            for (PendingExpenseLine p : pending) {
                saveExpenseLine(existingHead.getId(), p.sourceBillCode, p.amount, p.subjectId);
            }
            existingHead.setTotalAmount(nz(existingHead.getTotalAmount()).add(addedTotal));
            daoProvider.daoFor(ErpPrjCostCollection.class).updateEntity(existingHead);
        } else {
            ErpPrjCostCollection newHead = daoProvider.daoFor(ErpPrjCostCollection.class).newEntity();
            newHead.setCode("CC-" + projectId + "-" + CoreMetrics.currentTimeMillis());
            newHead.setProjectId(projectId);
            newHead.setOrgId(project.getOrgId());
            newHead.setBusinessDate(CoreMetrics.today());
            newHead.setTotalAmount(addedTotal);
            newHead.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
            newHead.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
            newHead.setPosted(false);
            newHead.setExchangeRate("1");
            newHead.setAmountSource(addedTotal.toPlainString());
            newHead.setAmountFunctional(addedTotal.toPlainString());
            daoProvider.daoFor(ErpPrjCostCollection.class).saveEntity(newHead);
            int lineNo = 0;
            for (PendingExpenseLine p : pending) {
                saveExpenseLine(newHead.getId(), ++lineNo, p.sourceBillCode, p.amount, p.subjectId);
            }
        }

        // 增量回写 actualCost
        project.setActualCost(nz(project.getActualCost()).add(addedTotal));
        daoProvider.daoFor(ErpPrjProject.class).updateEntity(project);
        return addedTotal;
    }

    private static final class PendingExpenseLine {
        final String sourceBillCode;
        final BigDecimal amount;
        final Long subjectId;

        PendingExpenseLine(String sourceBillCode, BigDecimal amount, Long subjectId) {
            this.sourceBillCode = sourceBillCode;
            this.amount = amount;
            this.subjectId = subjectId;
        }
    }

    private List<ErpFinExpenseClaim> findApprovedClaims() {
        // 跨域只读查询经 IBiz 走权限管道（对齐 skill 跨实体访问规则）。
        // xmeta 限制 docStatus 仅允许 eq/in 过滤，故仅按 approveStatus=APPROVED 查询，
        // 作废单据（docStatus=CANCELLED）在 Java 侧过滤。
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("approveStatus", FIN_APPROVE_STATUS_APPROVED));
        List<ErpFinExpenseClaim> all = expenseClaimBiz.findList(q, null, context);
        List<ErpFinExpenseClaim> result = new java.util.ArrayList<>(all.size());
        for (ErpFinExpenseClaim c : all) {
            if (c.getDocStatus() == null || !Objects.equals(c.getDocStatus(), FIN_DOC_STATUS_CANCELLED)) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * 报销行经 daoProvider 直接查（跨域只读，简单过滤查询；IErpFinExpenseClaimLineBiz
     * 仅为 CRUD 壳，无业务逻辑，daoProvider 等效且避免引入额外 IBiz 依赖）。
     */
    private List<ErpFinExpenseClaimLine> findLinesForProject(Long claimId, Long projectId) {
        IEntityDao<ErpFinExpenseClaimLine> dao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("claimId", claimId), eq("projectId", projectId)));
        return dao.findAllByQuery(q);
    }

    private boolean existsLine(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private void saveExpenseLine(Long headId, String sourceBillCode, BigDecimal amount, Long subjectId) {
        saveExpenseLine(headId, nextLineNo(headId), sourceBillCode, amount, subjectId);
    }

    private void saveExpenseLine(Long headId, int lineNo, String sourceBillCode, BigDecimal amount,
                                 Long subjectId) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        ErpPrjCostCollectionLine line = dao.newEntity();
        line.setCostCollectionId(headId);
        line.setLineNo(lineNo);
        line.setCostCategory(ErpPrjConstants.COST_CATEGORY_EXPENSE);
        line.setSourceBillType(ErpPrjConstants.SOURCE_BILL_TYPE_EXPENSE);
        line.setSourceBillCode(sourceBillCode);
        line.setSubjectId(subjectId);
        line.setAmount(amount);
        dao.saveEntity(line);
    }

    private int nextLineNo(Long headId) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("costCollectionId", headId));
        return (int) dao.findAllByQuery(q).size() + 1;
    }

    private ErpPrjCostCollection findHead(Long projectId) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addOrderField("id", true);
        q.setLimit(1);
        List<ErpPrjCostCollection> existing = dao.findAllByQuery(q);
        return existing.isEmpty() ? null : existing.get(0);
    }

    private ErpPrjProject loadProject(Long projectId) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
