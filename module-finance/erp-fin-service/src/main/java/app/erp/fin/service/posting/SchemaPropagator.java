package app.erp.fin.service.posting;

import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 多套账（并行账簿）传播解析器。根据 {@code erp-fin.multi-schema-enabled} 开关和源账套的 {@code isPropagate} 标志，
 * 解析一笔业务需要过账到的全部目标账套。
 *
 * <p>关闭时（默认）：仅返回源账套本身（单账套行为，向后兼容）。
 * <p>启用时：若源账套 {@code isPropagate=true}，返回同组织下所有 ACTIVE 账套（源账套优先）；
 * 若 {@code isPropagate=false}，仅返回源账套。
 *
 * <p>账套排序：按 nature 优先级（FINANCIAL→MANAGEMENT→TAX→CONSOLIDATION→BUDGET），
 * 确保主账套凭证先落库。
 *
 * <p>权威：{@code docs/design/finance/multiple-accounting-schemas.md}。
 */
public class SchemaPropagator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 解析目标账套列表。
     *
     * @param orgId            业务组织 ID
     * @param primarySchemaId  源（主）账套 ID（由调用方 resolveAcctSchemaId 解析）
     * @return 目标账套 ID 列表（至少包含 primarySchemaId），primarySchemaId 排首位
     */
    public List<Long> resolveTargetSchemas(Long orgId, Long primarySchemaId) {
        if (!isMultiSchemaEnabled()) {
            List<Long> single = new ArrayList<>();
            if (primarySchemaId != null) {
                single.add(primarySchemaId);
            }
            return single;
        }

        if (primarySchemaId == null) {
            return new ArrayList<>();
        }

        ErpMdAcctSchema primary = loadSchema(primarySchemaId);
        if (primary == null) {
            List<Long> single = new ArrayList<>();
            single.add(primarySchemaId);
            return single;
        }

        if (!Boolean.TRUE.equals(primary.getIsPropagate())) {
            List<Long> single = new ArrayList<>();
            single.add(primarySchemaId);
            return single;
        }

        List<ErpMdAcctSchema> allActive = findActiveSchemasByOrg(
                orgId != null ? orgId : primary.getOrgId());

        Set<Long> targets = new LinkedHashSet<>();
        targets.add(primarySchemaId);
        for (ErpMdAcctSchema schema : allActive) {
            if (!schema.getId().equals(primarySchemaId)) {
                targets.add(schema.getId());
            }
        }
        return new ArrayList<>(targets);
    }

    /**
     * 判断是否启用多套账并行传播。
     */
    public boolean isMultiSchemaEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_MULTI_SCHEMA_ENABLED, Boolean.FALSE));
    }

    /**
     * 查找组织下指定 nature 的主账套（用于 Dispatcher 层 resolveAcctSchemaId 按 nature 选取而非 LIMIT 1）。
     */
    public Long findPrimarySchemaId(Long orgId) {
        String nature = AppConfig.var(ErpFinConstants.CONFIG_DEFAULT_SCHEMA_NATURE,
                ErpFinConstants.DEFAULT_SCHEMA_NATURE_FINANCIAL);
        List<ErpMdAcctSchema> schemas = findActiveSchemasByOrg(orgId);
        for (ErpMdAcctSchema s : schemas) {
            if (nature.equals(s.getNature())) {
                return s.getId();
            }
        }
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }

    private ErpMdAcctSchema loadSchema(Long schemaId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        return dao.getEntityById(schemaId);
    }

    private List<ErpMdAcctSchema> findActiveSchemasByOrg(Long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        List<ErpMdAcctSchema> list = dao.findAllByQuery(q);
        list.sort(Comparator.comparingInt(s -> {
            int statusScore = "ACTIVE".equals(s.getStatus()) ? 0 : 100;
            return statusScore + app.erp.md.dao.AcctSchemaResolver.naturePriority(s.getNature());
        }));
        return list;
    }

    private int naturePriority(String nature) {
        if (nature == null) return 99;
        switch (nature) {
            case "FINANCIAL": return 0;
            case "MANAGEMENT": return 1;
            case "TAX": return 2;
            case "CONSOLIDATION": return 3;
            case "BUDGET": return 4;
            default: return 99;
        }
    }
}
