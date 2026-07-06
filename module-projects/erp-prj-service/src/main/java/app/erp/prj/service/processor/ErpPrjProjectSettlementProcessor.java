package app.erp.prj.service.processor;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.prj.biz.IErpPrjProjectPnlBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.dao.entity.ErpPrjProjectSettlementLine;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.posting.ProjectSettlementPostingDispatcher;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 项目结算单编排 Processor（{@code profitability.md §关键流程 2/3}，镜像 {@code ErpAstAssetCapitalizationProcessor}
 * 范式：Facade + Processor，protected step 方法支持下游逐个覆盖）。
 *
 * <p>三轴状态机（docStatus/approveStatus/posted）：
 * <ul>
 *   <li>{@code createSettlement}：基于最新 PnL 快照建头 + 来源单据行。</li>
 *   <li>{@code submit}/{@code approve}/{@code reject}/{@code cancel}：审批轴迁移。</li>
 *   <li>{@code approve} 末尾按 settlementType 分派：FINAL/INTERIM 仅过账；CLOSE 额外转固建卡。</li>
 *   <li>{@code reverseSettlement}：红冲凭证 + 回退卡片状态。</li>
 * </ul>
 *
 * <p>转固（CLOSE）经跨域 {@link IErpAstAssetBiz} 创建资产卡片（status=IN_SERVICE），回写 {@code assetCardId}。
 * 过账经 {@link ProjectSettlementPostingDispatcher}（失败隔离，不阻塞终态）；冲销是硬前置。
 */
public class ErpPrjProjectSettlementProcessor {

    static final String ASSET_STATUS_IN_SERVICE = "IN_SERVICE";
    static final String ASSET_STATUS_DRAFT = "DRAFT";

    @Inject
    IErpPrjProjectPnlBiz pnlBiz;
    @Inject
    IErpAstAssetBiz assetBiz;
    @Inject
    ProjectSettlementPostingDispatcher postingDispatcher;
    @Inject
    IDaoProvider daoProvider;

    public ErpPrjProjectSettlement createSettlement(Long projectId, String settlementType, IServiceContext context) {
        ErpPrjProject project = loadProject(projectId);
        ErpPrjProjectPnl snapshot = pnlBiz.getProjectPnl(projectId, context);
        if (snapshot == null) {
            throw new NopException(ErpPrjErrors.ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }

        IEntityDao<ErpPrjProjectSettlement> dao = daoProvider.daoFor(ErpPrjProjectSettlement.class);
        ErpPrjProjectSettlement settlement = dao.newEntity();
        settlement.setCode("STL-" + projectId + "-" + CoreMetrics.currentTimeMillis());
        settlement.setProjectId(projectId);
        settlement.setOrgId(project.getOrgId());
        settlement.setCustomerId(project.getCustomerId());
        settlement.setBusinessDate(CoreMetrics.today());
        settlement.setSettlementType(settlementType);
        settlement.setPnlSnapshotId(snapshot.getId());
        settlement.setCurrencyId(snapshot.getCurrencyId());
        settlement.setExchangeRate(snapshot.getExchangeRate() != null ? snapshot.getExchangeRate() : BigDecimal.ONE);
        settlement.setFinalRevenue(nz(snapshot.getRevenueAmount()));
        settlement.setFinalCost(nz(snapshot.getTotalCost()));
        settlement.setFinalProfit(nz(snapshot.getGrossProfit()));
        settlement.setTransferToAsset(ErpPrjConstants.SETTLEMENT_TYPE_CLOSE.equals(settlementType));
        settlement.setDocStatus(ErpPrjConstants.DOC_STATUS_DRAFT);
        settlement.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        settlement.setPosted(false);
        dao.saveEntity(settlement);

        buildLines(settlement, context);
        return settlement;
    }

    public ErpPrjProjectSettlement submit(Long id, IServiceContext context) {
        ErpPrjProjectSettlement settlement = requireSettlement(id);
        validateTransitionForSubmit(settlement);
        doSubmit(settlement, context);
        save(settlement);
        return settlement;
    }

    public ErpPrjProjectSettlement approve(Long id, IServiceContext context) {
        ErpPrjProjectSettlement settlement = requireSettlement(id);
        validateTransitionForApprove(settlement);
        if (ErpPrjConstants.SETTLEMENT_TYPE_CLOSE.equals(settlement.getSettlementType())
                && Boolean.TRUE.equals(settlement.getTransferToAsset()) && settlement.getAssetCardId() == null) {
            createAndActivateAsset(settlement, context);
        }
        doPost(settlement, context);
        doApprove(settlement, context);
        save(settlement);
        return settlement;
    }

    public ErpPrjProjectSettlement reject(Long id, IServiceContext context) {
        ErpPrjProjectSettlement settlement = requireSettlement(id);
        validateTransitionForReject(settlement);
        doReject(settlement, context);
        save(settlement);
        return settlement;
    }

    public ErpPrjProjectSettlement cancel(Long id, IServiceContext context) {
        ErpPrjProjectSettlement settlement = requireSettlement(id);
        validateTransitionForCancel(settlement);
        if (Boolean.TRUE.equals(settlement.getPosted())) {
            postingDispatcher.reverse(settlement);
            rollbackAssetIfNeeded(settlement);
            settlement = requireSettlement(id);
            settlement.setPosted(false);
            settlement.setPostedAt(null);
            settlement.setPostedBy(null);
        }
        doCancel(settlement, context);
        save(settlement);
        return settlement;
    }

    public ErpPrjProjectSettlement reverseSettlement(Long settlementId, IServiceContext context) {
        ErpPrjProjectSettlement settlement = requireSettlement(settlementId);
        if (!Boolean.TRUE.equals(settlement.getPosted())) {
            throw new NopException(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, settlement.getCode())
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, "posted=false")
                    .param(ErpPrjErrors.ARG_EXPECTED_STATUS, "posted=true");
        }
        postingDispatcher.reverse(settlement);
        rollbackAssetIfNeeded(settlement);
        settlement = requireSettlement(settlementId);
        settlement.setPosted(false);
        settlement.setPostedAt(null);
        settlement.setPostedBy(null);
        save(settlement);
        return settlement;
    }

    // ---- protected step methods（下游可逐个覆盖） ----

    protected void validateTransitionForSubmit(ErpPrjProjectSettlement settlement) {
        if (!Objects.equals(settlement.getApproveStatus(), ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED)) {
            throw illegalTransition(settlement, settlement.getApproveStatus(), ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpPrjProjectSettlement settlement) {
        String approveStatus = settlement.getApproveStatus();
        boolean requireApproval = ErpPrjConfigs.settlementRequireApproval();
        if (requireApproval && !Objects.equals(approveStatus, ErpPrjConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(settlement, approveStatus, ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
        }
        if (!requireApproval && !Objects.equals(approveStatus, ErpPrjConstants.APPROVE_STATUS_SUBMITTED)
                && !Objects.equals(approveStatus, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED)) {
            throw illegalTransition(settlement, approveStatus, "UNSUBMITTED|SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpPrjProjectSettlement settlement) {
        if (!Objects.equals(settlement.getApproveStatus(), ErpPrjConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(settlement, settlement.getApproveStatus(), ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForCancel(ErpPrjProjectSettlement settlement) {
        if (Objects.equals(settlement.getDocStatus(), ErpPrjConstants.DOC_STATUS_CANCELLED)) {
            throw illegalTransition(settlement, settlement.getDocStatus(), "非CANCELLED");
        }
    }

    protected void doSubmit(ErpPrjProjectSettlement settlement, IServiceContext context) {
        settlement.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
    }

    protected void doApprove(ErpPrjProjectSettlement settlement, IServiceContext context) {
        settlement.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        settlement.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        settlement.setApprovedBy(resolveUserId(context));
        settlement.setApprovedAt(LocalDateTime.now());
    }

    protected void doReject(ErpPrjProjectSettlement settlement, IServiceContext context) {
        settlement.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_REJECTED);
    }

    protected void doCancel(ErpPrjProjectSettlement settlement, IServiceContext context) {
        settlement.setDocStatus(ErpPrjConstants.DOC_STATUS_CANCELLED);
    }

    protected void doPost(ErpPrjProjectSettlement settlement, IServiceContext context) {
        boolean posted = postingDispatcher.tryPost(settlement);
        if (posted) {
            settlement.setPosted(true);
            settlement.setPostedAt(LocalDateTime.now());
            settlement.setPostedBy(resolveUserId(context));
        }
    }

    protected void buildLines(ErpPrjProjectSettlement settlement, IServiceContext context) {
        IEntityDao<ErpPrjProjectSettlementLine> lineDao = daoProvider.daoFor(ErpPrjProjectSettlementLine.class);
        int lineNo = 1;
        for (ErpPrjBilling billing : findBillings(settlement.getProjectId())) {
            ErpPrjProjectSettlementLine line = lineDao.newEntity();
            line.setSettlementId(settlement.getId());
            line.setLineNo(lineNo++);
            line.setLineType(ErpPrjConstants.SETTLEMENT_LINE_TYPE_INCOME);
            line.setSourceBillType(ErpPrjConstants.SOURCE_BILL_TYPE_BILLING);
            line.setSourceBillCode(billing.getCode());
            line.setAmount(nz(billing.getAmountFunctional()));
            lineDao.saveEntity(line);
        }
        for (ErpPrjCostCollection cc : findCostCollections(settlement.getProjectId())) {
            ErpPrjProjectSettlementLine line = lineDao.newEntity();
            line.setSettlementId(settlement.getId());
            line.setLineNo(lineNo++);
            line.setLineType(ErpPrjConstants.SETTLEMENT_LINE_TYPE_COST);
            line.setSourceBillType(ErpPrjConstants.SOURCE_BILL_TYPE_COST_COLLECTION);
            line.setSourceBillCode(cc.getCode());
            line.setAmount(parseAmount(cc.getAmountFunctional()));
            lineDao.saveEntity(line);
        }
    }

    protected void createAndActivateAsset(ErpPrjProjectSettlement settlement, IServiceContext context) {
        ErpPrjProject project = loadProject(settlement.getProjectId());
        Map<String, Object> data = new HashMap<>();
        data.put("code", "AST-PRJ-" + settlement.getProjectId() + "-" + CoreMetrics.currentTimeMillis());
        data.put("name", (project != null ? project.getName() : "项目") + "-转固");
        data.put("orgId", settlement.getOrgId());
        data.put("acquisitionDate", settlement.getBusinessDate());
        data.put("originalValue", nz(settlement.getFinalCost()));
        data.put("currentValue", nz(settlement.getFinalCost()));
        data.put("residualValue", BigDecimal.ZERO);
        data.put("status", ASSET_STATUS_IN_SERVICE);
        try {
            ErpAstAsset asset = assetBiz.save(data, context);
            if (asset != null && asset.getId() != null) {
                settlement.setAssetCardId(asset.getId());
            }
        } catch (Exception e) {
            throw new NopException(ErpPrjErrors.ERR_SETTLEMENT_CAPITALIZATION_FAILED, e)
                    .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, settlement.getCode());
        }
    }

    protected void rollbackAssetIfNeeded(ErpPrjProjectSettlement settlement) {
        if (settlement.getAssetCardId() == null) {
            return;
        }
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset asset = dao.getEntityById(settlement.getAssetCardId());
        if (asset != null) {
            asset.setStatus(ASSET_STATUS_DRAFT);
            dao.updateEntity(asset);
        }
    }

    // ---- helpers ----

    private ErpPrjProjectSettlement requireSettlement(Long id) {
        IEntityDao<ErpPrjProjectSettlement> dao = daoProvider.daoFor(ErpPrjProjectSettlement.class);
        ErpPrjProjectSettlement settlement = dao.getEntityById(id);
        if (settlement == null) {
            throw new NopException(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpPrjErrors.ARG_SETTLEMENT_ID, id);
        }
        return settlement;
    }

    private void save(ErpPrjProjectSettlement settlement) {
        daoProvider.daoFor(ErpPrjProjectSettlement.class).updateEntity(settlement);
    }

    private List<ErpPrjBilling> findBillings(Long projectId) {
        IEntityDao<ErpPrjBilling> dao = daoProvider.daoFor(ErpPrjBilling.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addFilter(ne("docStatus", ErpPrjConstants.DOC_STATUS_CANCELLED));
        return dao.findAllByQuery(q);
    }

    private List<ErpPrjCostCollection> findCostCollections(Long projectId) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addFilter(ne("docStatus", ErpPrjConstants.DOC_STATUS_CANCELLED));
        return dao.findAllByQuery(q);
    }

    private ErpPrjProject loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectId);
    }

    private String resolveUserId(IServiceContext context) {
        return context != null && context.getUserId() != null ? context.getUserId() : "system";
    }

    private NopException illegalTransition(ErpPrjProjectSettlement settlement, String current, String expected) {
        return new NopException(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, settlement.getCode())
                .param(ErpPrjErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPrjErrors.ARG_EXPECTED_STATUS, expected);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal parseAmount(String s) {
        if (s == null || s.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
