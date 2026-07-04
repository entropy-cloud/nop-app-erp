package app.erp.inv.service.costing;

import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 标准成本解析器（plan 2026-07-05-0427-2）。解析顺序：
 * <ol>
 *   <li>最近一条 status=FIRMED 的 {@code ErpMfgCostRollupLine.unitCost}（直接查 erp_mfg_cost_rollup +
 *       erp_mfg_cost_rollup_line，inventory→manufacturing 经 mfg-dao 编译期依赖，无环）</li>
 *   <li>（config-gated）物料主数据标准成本列（当前 Non-Goal，{@code ErpMdMaterial} 无 standardCost 列，
 *       本路径恒返回 {@code null}；后续主数据冗余发布列落地后自动生效）</li>
 *   <li>均无抛 {@link ErpInvErrors#ERR_STANDARD_COST_NOT_AVAILABLE}</li>
 * </ol>
 *
 * <p>本类非 BizModel（同 {@link CostMethodResolver} 范式），经 {@link IDaoProvider} 直接读 mfg-dao 实体，
 * 避免跨域 BizObject 解析在单域测试中的注册复杂性。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md}（STANDARD 方法）。
 */
public class StandardCostResolver {

    static final String STATUS_FIRMED = "FIRMED";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    /**
     * 解析物料标准成本。无可用标准成本时抛 {@link ErpInvErrors#ERR_STANDARD_COST_NOT_AVAILABLE}。
     */
    public BigDecimal resolve(Long materialId) {
        if (materialId == null) {
            throw new NopException(ErpInvErrors.ERR_STANDARD_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, materialId);
        }
        BigDecimal standard = resolveFromRollup(materialId);
        if (standard == null && isMaterialMasterFallbackEnabled()) {
            standard = resolveFromMaterialMaster(materialId);
        }
        if (standard == null) {
            throw new NopException(ErpInvErrors.ERR_STANDARD_COST_NOT_AVAILABLE)
                    .param(ErpInvErrors.ARG_MATERIAL_ID, materialId);
        }
        return standard;
    }

    private BigDecimal resolveFromRollup(Long materialId) {
        ormTemplate.flushSession();
        IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
        List<ErpMfgCostRollup> firmedList = headerDao.findAllByQuery(
                new QueryBean().addFilter(eq("status", STATUS_FIRMED)));
        if (firmedList.isEmpty()) {
            return null;
        }
        firmedList.sort(Comparator.comparing(
                h -> h.getBusinessDate() != null ? h.getBusinessDate() : LocalDate.MIN,
                Comparator.reverseOrder()));

        IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        for (ErpMfgCostRollup header : firmedList) {
            List<ErpMfgCostRollupLine> lines = lineDao.findAllByQuery(
                    new QueryBean()
                            .addFilter(eq("costRollupId", header.getId()))
                            .addFilter(eq("materialId", materialId)));
            if (!lines.isEmpty()) {
                return lines.get(0).getUnitCost();
            }
        }
        return null;
    }

    private BigDecimal resolveFromMaterialMaster(Long materialId) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = dao.getEntityById(materialId);
        if (material == null) {
            return null;
        }
        try {
            Object value = material.orm_propValueByName("standardCost");
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value != null) {
                return new BigDecimal(value.toString());
            }
        } catch (Exception ignored) {
            // 属性不存在时反射读取抛错，返回 null 由调用方抛 ERR_STANDARD_COST_NOT_AVAILABLE
        }
        return null;
    }

    private boolean isMaterialMasterFallbackEnabled() {
        Boolean flag = AppConfig.var(
                ErpInvConstants.CONFIG_STANDARD_COST_FALLBACK_TO_MATERIAL_MASTER, Boolean.TRUE);
        return !Boolean.FALSE.equals(flag);
    }
}
