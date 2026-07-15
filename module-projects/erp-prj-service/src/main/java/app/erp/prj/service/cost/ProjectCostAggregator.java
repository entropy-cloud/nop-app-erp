package app.erp.prj.service.cost;

import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 项目成本归集聚合器。工时 APPROVED 时同步生成/追加 {@link ErpPrjCostCollectionLine}
 * （{@code cost-collection.md §4.2}，归集与过账同事务——强一致）。
 *
 * <p>归集策略：
 * <ul>
 *   <li>每个项目维护单个「OPEN」归集头（{@link ErpPrjCostCollection}），不存在则新建。</li>
 *   <li>幂等：按 {@code sourceBillType + sourceBillCode} 去重，已归集的工时不重复入账。</li>
 *   <li>归集行：{@code costCategory=LABOR}、{@code sourceBillType=TIMESHEET}、
 *       {@code sourceBillCode=工时单号}、{@code amount=costAmount}、
 *       {@code subjectId=活动类型 subjectId（回退项目类型 defaultSubjectId）}、{@code taskId}。</li>
 *   <li>归集头 {@code totalAmount} 同步累加。</li>
 * </ul>
 */
public class ProjectCostAggregator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 从工时单归集人工成本。返回 true 表示新增了归集行；false 表示幂等命中（已归集）。
     */
    public boolean aggregateFromTimesheet(ErpPrjTimesheet timesheet) {
        if (timesheet == null || timesheet.getProjectId() == null) {
            return false;
        }
        String sourceBillCode = timesheet.getCode();
        if (existsLine(ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET, sourceBillCode)) {
            return false;
        }

        BigDecimal amount = nz(timesheet.getCostAmount());
        ErpPrjProject project = loadProject(timesheet.getProjectId());
        Long subjectId = resolveLaborSubjectId(timesheet, project);

        ErpPrjCostCollection existingHead = findOpenHead(timesheet.getProjectId());
        ErpPrjCostCollectionLine line = daoProvider.daoFor(ErpPrjCostCollectionLine.class).newEntity();
        line.setCostCategory(ErpPrjConstants.COST_CATEGORY_LABOR);
        line.setSourceBillType(ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET);
        line.setSourceBillCode(sourceBillCode);
        line.setSubjectId(subjectId);
        line.setTaskId(timesheet.getTaskId());
        line.setAmount(amount);

        if (existingHead != null) {
            line.setCostCollectionId(existingHead.getId());
            line.setLineNo(nextLineNo(existingHead.getId()));
            daoProvider.daoFor(ErpPrjCostCollectionLine.class).saveEntity(line);
            existingHead.setTotalAmount(nz(existingHead.getTotalAmount()).add(amount));
            daoProvider.daoFor(ErpPrjCostCollection.class).updateEntity(existingHead);
        } else {
            ErpPrjCostCollection newHead = daoProvider.daoFor(ErpPrjCostCollection.class).newEntity();
            newHead.setCode("CC-" + timesheet.getProjectId() + "-" + CoreMetrics.currentTimeMillis());
            newHead.setProjectId(timesheet.getProjectId());
            newHead.setOrgId(timesheet.getOrgId());
            newHead.setBusinessDate(CoreMetrics.today());
            newHead.setTotalAmount(amount);
            newHead.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
            newHead.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
            newHead.setPosted(false);
            newHead.setExchangeRate(BigDecimal.ONE);
            newHead.setAmountSource(amount);
            newHead.setAmountFunctional(amount);
            daoProvider.daoFor(ErpPrjCostCollection.class).saveEntity(newHead);

            line.setCostCollectionId(newHead.getId());
            line.setLineNo(1);
            daoProvider.daoFor(ErpPrjCostCollectionLine.class).saveEntity(line);
        }

        // 增量回写项目 actualCost（避免 sum 查询在未 flush 时读不到新行）
        if (project != null) {
            BigDecimal newActual = nz(project.getActualCost()).add(amount);
            project.setActualCost(newActual);
            daoProvider.daoFor(ErpPrjProject.class).updateEntity(project);
        }
        return true;
    }

    /**
     * 聚合项目所有归集行金额 → 回写 {@link ErpPrjProject#getActualCost()}。
     */
    public BigDecimal refreshActualCost(Long projectId) {
        if (projectId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal used = sumCollectedAmount(projectId);

        ErpPrjProject project = loadProject(projectId);
        if (project != null) {
            project.setActualCost(used);
            daoProvider.daoFor(ErpPrjProject.class).updateEntity(project);
        }
        return used;
    }

    private BigDecimal sumCollectedAmount(Long projectId) {
        IEntityDao<ErpPrjCostCollection> headDao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean headQuery = new QueryBean();
        headQuery.addFilter(eq("projectId", projectId));
        List<ErpPrjCostCollection> heads = headDao.findAllByQuery(headQuery);
        if (heads.isEmpty()) {
            return BigDecimal.ZERO;
        }
        java.util.List<Long> headIds = new java.util.ArrayList<>(heads.size());
        for (ErpPrjCostCollection h : heads) {
            headIds.add(h.getId());
        }
        IEntityDao<ErpPrjCostCollectionLine> lineDao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean lineQuery = new QueryBean();
        lineQuery.addFilter(io.nop.api.core.beans.FilterBeans.in("costCollectionId", headIds));
        List<ErpPrjCostCollectionLine> lines = lineDao.findAllByQuery(lineQuery);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPrjCostCollectionLine l : lines) {
            sum = sum.add(nz(l.getAmount()));
        }
        return sum;
    }

    private boolean existsLine(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private ErpPrjCostCollection findOpenHead(Long projectId) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addOrderField("id", true);
        q.setLimit(1);
        List<ErpPrjCostCollection> existing = dao.findAllByQuery(q);
        return existing.isEmpty() ? null : existing.get(0);
    }

    private int nextLineNo(Long headId) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("costCollectionId", headId));
        return (int) dao.findAllByQuery(q).size() + 1;
    }

    private Long resolveLaborSubjectId(ErpPrjTimesheet timesheet, ErpPrjProject project) {
        if (timesheet.getActivityTypeId() != null) {
            IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
            ErpPrjActivityType activityType = dao.getEntityById(timesheet.getActivityTypeId());
            if (activityType != null && activityType.getSubjectId() != null) {
                return activityType.getSubjectId();
            }
        }
        if (project != null && project.getProjectTypeId() != null) {
            IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
            ErpPrjProjectType projectType = dao.getEntityById(project.getProjectTypeId());
            if (projectType != null) {
                return projectType.getDefaultSubjectId();
            }
        }
        return null;
    }

    private ErpPrjProject loadProject(Long projectId) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
