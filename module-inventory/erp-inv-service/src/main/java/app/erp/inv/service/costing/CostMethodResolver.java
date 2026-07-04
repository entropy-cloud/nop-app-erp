package app.erp.inv.service.costing;

import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * 成本方法解析器。按 {@code ErpMdMaterial.costMethod} → {@code ErpMdAcctSchema.costingMethod} →
 * 配置默认（{@code erp-inv.default-cost-method}）顺序解析计价方法；{@code erp-inv.costing-enabled=false}
 * 时一律回退 {@link ErpInvConstants#COST_METHOD_MOVING_AVERAGE}（兜底，对齐既有硬编码行为）。
 *
 * <p>未识别码值（20/40/50/60/70 等本期 Non-Goal）回退默认，避免记账中断——后续 successor 接管时改策略分派表即可。
 */
public class CostMethodResolver {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 解析移动单行的计价方法。
     *
     * <p>注：accountSchemaId 优先取行级（如有），回退调用方提供的账套；二者皆无时直接走配置默认。
     */
    public String resolve(ErpInvStockMoveLine line, Long acctSchemaId) {
        if (!isCostingEnabled()) {
            return ErpInvConstants.COST_METHOD_MOVING_AVERAGE;
        }
        String method = readMaterialCostMethod(line.getMaterialId());
        if (method == null && acctSchemaId != null) {
            method = readAcctSchemaCostingMethod(acctSchemaId);
        }
        if (method == null || !isSupported(method)) {
            return defaultCostMethod();
        }
        return method;
    }

    /** 当前已实现策略：MOVING_AVERAGE + FIFO + STANDARD。其他码值回退默认，待 successor 接管。 */
    private boolean isSupported(String method) {
        return Objects.equals(method, ErpInvConstants.COST_METHOD_MOVING_AVERAGE)
                || Objects.equals(method, ErpInvConstants.COST_METHOD_FIFO)
                || Objects.equals(method, ErpInvConstants.COST_METHOD_STANDARD);
    }

    private String readMaterialCostMethod(Long materialId) {
        if (materialId == null) {
            return null;
        }
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = dao.getEntityById(materialId);
        return material != null ? material.getCostMethod() : null;
    }

    private String readAcctSchemaCostingMethod(Long acctSchemaId) {
        if (acctSchemaId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = dao.getEntityById(acctSchemaId);
        return schema != null ? schema.getCostingMethod() : null;
    }

    private boolean isCostingEnabled() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_COSTING_ENABLED, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }

    private String defaultCostMethod() {
        String configured = AppConfig.var(ErpInvConstants.CONFIG_DEFAULT_COST_METHOD,
                ErpInvConstants.DEFAULT_COST_METHOD);
        return configured != null ? configured : ErpInvConstants.DEFAULT_COST_METHOD;
    }
}
