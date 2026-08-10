package app.erp.mfg.service.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.common.service.MaskHelper;
import app.erp.mfg.biz.IErpMfgCostRollupLineBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;

@BizModel("ErpMfgCostRollupLine")
public class ErpMfgCostRollupLineBizModel extends CrudBizModel<ErpMfgCostRollupLine> implements IErpMfgCostRollupLineBiz{
    public ErpMfgCostRollupLineBizModel(){
        setEntityName(ErpMfgCostRollupLine.class.getName());
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2 Phase 4）----------
    // 授权 = 管理员/财务员；非授权 = null。委托 MaskHelper（fail-closed）。
    // 成本区域 plan-first 证据：E3.2 取值豁免不变量保证服务端卷算（CostRollupService）经 DAO 直读不经此
    // @BizLoader——仅 BizModel/GraphQL 边界 masking，不触 CostRollupService 业务逻辑（守卫测试复跑见 Proof）。
    private static final Set<String> COST_ROLES = Set.of(MaskHelper.ROLE_BIZ_ADMIN, MaskHelper.ROLE_FINANCE_STAFF);

    @BizLoader("materialCost")
    public BigDecimal materialCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getMaterialCost(), COST_ROLES);
    }

    @BizLoader("laborCost")
    public BigDecimal laborCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getLaborCost(), COST_ROLES);
    }

    @BizLoader("overheadCost")
    public BigDecimal overheadCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getOverheadCost(), COST_ROLES);
    }

    @BizLoader("subcontractCost")
    public BigDecimal subcontractCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getSubcontractCost(), COST_ROLES);
    }

    @BizLoader("totalCost")
    public BigDecimal totalCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getTotalCost(), COST_ROLES);
    }

    @BizLoader("unitCost")
    public BigDecimal unitCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getUnitCost(), COST_ROLES);
    }

}
