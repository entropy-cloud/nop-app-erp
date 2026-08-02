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
        settlement.setDocStatus(ErpPrjConstants.DOC_STATUS_DRAFT);
        settlement.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        settlement.setPosted(false);
        dao.saveEntity(settlement);

        facade.buildLines(settlement, context);
        return settlement;
    }
}
