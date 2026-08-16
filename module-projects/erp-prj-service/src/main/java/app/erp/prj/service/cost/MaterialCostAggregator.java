package app.erp.prj.service.cost;

import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
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
 * 物料成本归集聚合器（RC-R1.61 / P1-RC-049）。purchase 侧入库审核经
 * {@code IErpPrjCostCollectionBiz.aggregateMaterialCost} Facade 触发本聚合器，
 * 生成/追加 {@link ErpPrjCostCollectionLine}（costCategory=MATERIAL）。
 *
 * <p>对齐 {@link ProjectCostAggregator}（工时）与 {@link ExpenseCostAggregator}（费用）既有范式：
 * <ul>
 *   <li>每个项目维护单个「APPROVED」归集头（{@link ErpPrjCostCollection}），不存在则新建——
 *       三来源（LABOR/EXPENSE/MATERIAL）复用同一 head 查找/累加逻辑，避免多 head 分叉。</li>
 *   <li>幂等：按 {@code sourceBillType + sourceBillCode} 去重，已归集的入库行不重复入账。</li>
 *   <li>归集行：{@code costCategory=MATERIAL}、{@code sourceBillType=PURCHASE_RECEIVE}、
 *       {@code sourceBillCode=入库单号-行号}、{@code amount=入库行金额(不含税)}、
 *       {@code subjectId=项目类型默认成本科目}。</li>
 *   <li>归集头 {@code totalAmount} 同步累加 + {@link ErpPrjProject#getActualCost()} 增量回写。</li>
 * </ul>
 *
 * <p>守卫链（requireReferenceable / 预算检查）由调用侧 Processor
 * {@code ErpPrjCostCollectionAggregateMaterialCostProcessor} 完成，本类仅承担幂等 + 写入。
 */
public class MaterialCostAggregator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 归集一笔物料成本。返回新增金额；幂等命中或入参无效返回 0。
     */
    public BigDecimal aggregateMaterial(Long projectId, BigDecimal amount, String sourceBillCode) {
        if (projectId == null || amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (existsLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, sourceBillCode)) {
            return BigDecimal.ZERO;
        }
        ErpPrjProject project = loadProject(projectId);
        Long subjectId = resolveMaterialSubjectId(project);

        ErpPrjCostCollection existingHead = findHead(projectId);
        if (existingHead != null) {
            saveLine(existingHead.getId(), nextLineNo(existingHead.getId()),
                    sourceBillCode, amount, subjectId);
            existingHead.setTotalAmount(nz(existingHead.getTotalAmount()).add(amount));
            daoProvider.daoFor(ErpPrjCostCollection.class).updateEntity(existingHead);
        } else {
            ErpPrjCostCollection newHead = daoProvider.daoFor(ErpPrjCostCollection.class).newEntity();
            newHead.setCode("CC-" + projectId + "-" + CoreMetrics.currentTimeMillis());
            newHead.setProjectId(projectId);
            newHead.setOrgId(project != null ? project.getOrgId() : null);
            newHead.setBusinessDate(CoreMetrics.today());
            newHead.setTotalAmount(amount);
            newHead.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
            newHead.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
            newHead.setPosted(false);
            newHead.setExchangeRate(BigDecimal.ONE);
            newHead.setAmountSource(amount);
            newHead.setAmountFunctional(amount);
            daoProvider.daoFor(ErpPrjCostCollection.class).saveEntity(newHead);
            saveLine(newHead.getId(), 1, sourceBillCode, amount, subjectId);
        }

        if (project != null) {
            project.setActualCost(nz(project.getActualCost()).add(amount));
            daoProvider.daoFor(ErpPrjProject.class).updateEntity(project);
        }
        return amount;
    }

    private void saveLine(Long headId, int lineNo, String sourceBillCode, BigDecimal amount, Long subjectId) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        ErpPrjCostCollectionLine line = dao.newEntity();
        line.setCostCollectionId(headId);
        line.setLineNo(lineNo);
        line.setCostCategory(ErpPrjConstants.COST_CATEGORY_MATERIAL);
        line.setSourceBillType(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE);
        line.setSourceBillCode(sourceBillCode);
        line.setSubjectId(subjectId);
        line.setAmount(amount);
        dao.saveEntity(line);
    }

    private boolean existsLine(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
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

    private int nextLineNo(Long headId) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("costCollectionId", headId));
        return (int) dao.findAllByQuery(q).size() + 1;
    }

    private Long resolveMaterialSubjectId(ErpPrjProject project) {
        if (project != null && project.getProjectTypeId() != null) {
            ErpPrjProjectType projectType = project.getProjectType();
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
