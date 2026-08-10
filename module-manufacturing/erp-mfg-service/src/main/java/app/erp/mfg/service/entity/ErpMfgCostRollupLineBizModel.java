package app.erp.mfg.service.entity;

import java.math.BigDecimal;
import java.util.Set;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.common.service.MaskHelper;
import app.erp.mfg.biz.IErpMfgCostRollupLineBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.service.costing.CostBandClassifier;

/**
 * E4.1 字段级可见性 + 代理视图落地（plan 2026-08-11-0915-3）。
 *
 * <p><b>双层分工</b>（Q1 (d) 冻结）：
 * <ul>
 *   <li>4 要素成本（material/labor/overhead/subcontract）：xmeta {@code published=false} 全局隐藏，
 *       经 {@code @BizLoader(autoCreateField=true)} 暴露为 high/mid/low 档位代理字段（精确值不可见）。</li>
 *   <li>totalCost/unitCost（聚合）：保持 E3.1 masking（授权管理员/财务员见明文，非授权 null）。
 *       Phase 1 Decision (a)#2 裁决：隐藏 + passthrough 代理功能等价于 masking，保持 masking 避免契约面振荡。</li>
 * </ul>
 *
 * <p><b>E3.2 取值豁免不变量</b>（load-bearing）：本类仅 BizModel/GraphQL 边界控制；
 * {@code CostRollupService} 经 DAO 写入要素成本（{@code setMaterialCost} 等，生产者），
 * {@code StandardCostResolver} 经 DAO 读 unitCost（unitCost 保持 published）——均不遍历此 @BizLoader。
 * 守卫测试 {@code TestErpMfgCostRollupValueExemptionInvariant} /
 * {@code TestErpInvStandardCostResolverValueExemptionInvariant} 复跑绿。
 */
@BizModel("ErpMfgCostRollupLine")
public class ErpMfgCostRollupLineBizModel extends CrudBizModel<ErpMfgCostRollupLine> implements IErpMfgCostRollupLineBiz {
    public ErpMfgCostRollupLineBizModel() {
        setEntityName(ErpMfgCostRollupLine.class.getName());
    }

    private static final Set<String> COST_ROLES = Set.of(MaskHelper.ROLE_BIZ_ADMIN, MaskHelper.ROLE_FINANCE_STAFF);

    // ---------- E3.1 masking（保持，totalCost/unitCost 聚合字段授权可见）----------

    @BizLoader("totalCost")
    public BigDecimal totalCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getTotalCost(), COST_ROLES);
    }

    @BizLoader("unitCost")
    public BigDecimal unitCostMask(@ContextSource ErpMfgCostRollupLine entity) {
        return MaskHelper.maskDecimal(entity.getUnitCost(), COST_ROLES);
    }

    // ---------- E4.1 代理视图：要素成本档位映射（autoCreateField=true，Q1 (d)）----------
    // 原始 4 要素字段经 xmeta published=false 隐藏；此处新增代理字段暴露 high/mid/low 离散值。
    // autoCreateField=true 平台语义：字段不在 schema 中时自动创建（bypass objMeta 检查）。

    @BizLoader(autoCreateField = true)
    public String materialBand(@ContextSource ErpMfgCostRollupLine entity) {
        return CostBandClassifier.classify(entity.getMaterialCost());
    }

    @BizLoader(autoCreateField = true)
    public String laborBand(@ContextSource ErpMfgCostRollupLine entity) {
        return CostBandClassifier.classify(entity.getLaborCost());
    }

    @BizLoader(autoCreateField = true)
    public String overheadBand(@ContextSource ErpMfgCostRollupLine entity) {
        return CostBandClassifier.classify(entity.getOverheadCost());
    }

    @BizLoader(autoCreateField = true)
    public String subcontractBand(@ContextSource ErpMfgCostRollupLine entity) {
        return CostBandClassifier.classify(entity.getSubcontractCost());
    }
}
