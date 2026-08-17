package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpPrjProjectSettlement createSettlement per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含基于最新 PnL 快照建头 + 来源单据行的结算单创建编排；共享 protected helper 单一真相源在
 * {@link ErpPrjProjectSettlementProcessor}（slim-to-S-delegation facade）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectSettlementCreateSettlementProcessor {

    @Inject
    ErpPrjProjectSettlementProcessor facade;

    public ErpPrjProjectSettlement createSettlement(Long projectId, String settlementType, IServiceContext context) {
        ErpPrjProject project = facade.loadProject(projectId);
        ErpPrjProjectPnl snapshot = facade.pnlBiz.getProjectPnl(projectId, context);
        if (snapshot == null) {
            throw new NopException(ErpPrjErrors.ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }

        IEntityDao<ErpPrjProjectSettlement> dao = facade.daoProvider.daoFor(ErpPrjProjectSettlement.class);
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
        settlement.setFinalRevenue(facade.nz(snapshot.getRevenueAmount()));
        settlement.setFinalCost(facade.nz(snapshot.getTotalCost()));
        settlement.setFinalProfit(facade.nz(snapshot.getGrossProfit()));
        settlement.setTransferToAsset(ErpPrjConstants.SETTLEMENT_TYPE_CLOSE.equals(settlementType));
        // RC-R1.63 / P1-RC-052（UC-PRJ-07 ④）：仅 FINAL（竣工结算）自动留存质保金——D1 选项 A（config 驱动）。
        // retentionAmount = finalRevenue × erp-prj.settlement-retention-ratio（默认 0=设计性 opt-in，零非静默缺失）；
        // retentionDueDate = businessDate + erp-prj.settlement-retention-due-months（默认 12）。
        // INTERIM（阶段结算无尾款留存语义）/CLOSE（自建转固非应收）不填；手工覆盖路径保留（CRUD update 可改）。
        if (ErpPrjConstants.SETTLEMENT_TYPE_FINAL.equals(settlementType)) {
            BigDecimal retention = facade.computeRetentionAmount(settlement.getFinalRevenue());
            settlement.setRetentionAmount(retention);
            if (retention.signum() > 0) {
                settlement.setRetentionDueDate(facade.computeRetentionDueDate(settlement.getBusinessDate()));
            }
        }
        settlement.setDocStatus(ErpPrjConstants.DOC_STATUS_DRAFT);
        settlement.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        settlement.setPosted(false);
        dao.saveEntity(settlement);

        facade.buildLines(settlement, context);
        return settlement;
    }
}
