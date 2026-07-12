package app.erp.md.dao;

import app.erp.md.dao.entity.ErpMdAcctSchema;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 会计账套解析工具。跨域共享的 primary schema 解析逻辑，避免各域 Dispatcher 重复实现
 * 且保证一致的行为：按 nature 优先级（FINANCIAL→MANAGEMENT→TAX→CONSOLIDATION→BUDGET）
 * 选取组织的 ACTIVE 账套。
 *
 * <p>多套账传播由 {@code SchemaPropagator}（finance-service）在过账处理器层面统一处理，
 * 本类仅负责为 {@code PostingEvent.acctSchemaId} 设置正确的主账套。
 */
public class AcctSchemaResolver {

    /**
     * 解析组织的主账套 ID（按 nature 优先级选取 FINANCIAL 账套）。
     *
     * @return 主账套 ID；无 ACTIVE 账套时返回 null
     */
    public static Long resolvePrimarySchemaId(IDaoProvider daoProvider, Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.stream()
                .min(Comparator.comparingInt(AcctSchemaResolver::schemaPriority))
                .map(ErpMdAcctSchema::getId)
                .orElse(null);
    }

    private static int schemaPriority(ErpMdAcctSchema s) {
        int statusScore = "ACTIVE".equals(s.getStatus()) ? 0 : 1;
        return statusScore * 100 + naturePriority(s.getNature());
    }

    /**
     * nature 优先级：FINANCIAL(0) → MANAGEMENT(1) → TAX(2) → CONSOLIDATION(3) → BUDGET(4) → 其他(99)。
     */
    public static int naturePriority(String nature) {
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
