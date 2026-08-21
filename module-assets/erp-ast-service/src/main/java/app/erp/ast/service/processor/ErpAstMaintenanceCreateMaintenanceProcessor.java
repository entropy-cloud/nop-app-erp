package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.statemachine.ErpAstMaintenanceStateMachine;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * ErpAstMaintenance createMaintenance per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含维修建单编排；共享 protected helper 单一真相源在 {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstMaintenanceCreateMaintenanceProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    @Inject
    ErpAstMaintenanceStateMachine stateMachine;

    public ErpAstMaintenance createMaintenance(String assetId, String code, String name, String businessDate,
                                                String maintenanceVisitId, String reason, IServiceContext context) {
        ErpAstAsset asset = facade.requireAsset(assetId);
        facade.validateAssetNotTerminal(asset, context);

        IEntityDao<ErpAstMaintenance> dao = facade.maintenanceDao();
        ErpAstMaintenance maintenance = dao.newEntity();
        maintenance.setCode(code);
        maintenance.setName(name);
        maintenance.setOrgId(asset.getOrgId());
        maintenance.setAssetId(String.valueOf(assetId));
        maintenance.setMaintenanceVisitId(maintenanceVisitId);
        // 创建种子初始态目标态委托 StateMachine Bean（M4.53，契约 §4）
        maintenance.setStatus(stateMachine.createTargetStatus());
        maintenance.setBusinessDate(facade.parseDate(businessDate));
        maintenance.setCurrencyId(asset.getCurrencyId());
        maintenance.setCapitalizedAmount(BigDecimal.ZERO);
        maintenance.setTotalCostAmount(BigDecimal.ZERO);
        maintenance.setPosted(false);
        maintenance.setReversed(false);
        maintenance.setReason(reason);
        dao.saveEntity(maintenance);
        return maintenance;
    }
}
