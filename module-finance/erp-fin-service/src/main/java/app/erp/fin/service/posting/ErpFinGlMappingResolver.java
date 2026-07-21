package app.erp.fin.service.posting;

import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import app.erp.fin.dao.dto.GlMappingDimensions;
import app.erp.fin.dao.entity.ErpFinGlMappingRule;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * GL 映射规则解析器实现（plan 2026-07-21-0827-1 A1）。
 *
 * <p>进程内 {@link ConcurrentHashMap} 缓存 + 启动期 eager load + CRUD mutation 主动失效（全量 reload）。
 * 优先级链算法：(priority DESC, 维度具体度 DESC) 排序匹配；空匹配返回 {@code null}（保留 Provider fallback）。
 *
 * <p>缓存配置项：
 * <ul>
 *   <li>{@code erp-fin.gl-mapping.cache-enabled}（默认 {@code true}）：{@code false} 时每次 resolve 查 DB。</li>
 *   <li>{@code erp-fin.gl-mapping.cache-ttl-seconds}（默认 {@code 3600}）：TTL 降级备用。</li>
 * </ul>
 *
 * <p>维度扩展（best-effort）：{@code materialId → materialCategoryId} 经 {@link ErpMdMaterial#getCategoryId()}
 * 取数；{@code partnerGroupId} 不扩展（{@code ErpMdPartnerGroup} 实体当前不存在），调用方需显式传入。
 *
 * <p>权威：{@code docs/design/finance/gl-mapping-rules.md §3 优先级链算法 + §4 缓存策略}。
 */
public class ErpFinGlMappingResolver implements IErpFinGlMappingResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinGlMappingResolver.class);

    static final String CONFIG_CACHE_ENABLED = "erp-fin.gl-mapping.cache-enabled";
    static final String CONFIG_CACHE_TTL_SECONDS = "erp-fin.gl-mapping.cache-ttl-seconds";

    @Inject
    IDaoProvider daoProvider;

    /**
     * 进程内缓存：(orgId, businessType, accountKey) → 候选规则列表。
     * 启动期 eager load；CRUD mutation 后全量 reload。
     */
    private final Map<String, List<ErpFinGlMappingRule>> cache = new ConcurrentHashMap<>();

    /** 缓存是否已加载（避免多线程下重复 reload）。 */
    private volatile boolean cacheLoaded = false;

    /** 缓存上次 reload 时间戳（用于 TTL 降级；当前实现仅作记录）。 */
    private volatile long lastLoadTimeMillis = 0L;

    @PostConstruct
    public void init() {
        if (isCacheEnabled()) {
            try {
                reloadCache();
            } catch (RuntimeException e) {
                LOG.warn("GL 映射规则缓存初始化失败，降级为每次查 DB：{}", e.getMessage());
                cacheLoaded = false;
            }
        }
    }

    @Override
    public String resolveSubjectCode(String businessType, String accountKey, GlMappingDimensions dimensions,
                                     Long acctSchemaId) {
        if (businessType == null || accountKey == null) {
            return null;
        }
        GlMappingDimensions effectiveDims = expandDimensions(dimensions);
        Long orgId = resolveOrgIdFromDimensions(effectiveDims);

        List<ErpFinGlMappingRule> candidates = isCacheEnabled()
                ? loadFromCache(orgId, businessType, accountKey)
                : loadFromDb(orgId, businessType, accountKey);

        if (candidates == null || candidates.isEmpty()) {
            LOG.info("gl-mapping rule miss: businessType={} accountKey={} dimensions={}", businessType, accountKey,
                    effectiveDims);
            return null;
        }

        List<ErpFinGlMappingRule> matched = new ArrayList<>();
        for (ErpFinGlMappingRule rule : candidates) {
            if (!Boolean.TRUE.equals(rule.getIsActive())) {
                continue;
            }
            if (!matches(rule, effectiveDims, acctSchemaId)) {
                continue;
            }
            matched.add(rule);
        }

        if (matched.isEmpty()) {
            LOG.info("gl-mapping rule miss (no active match): businessType={} accountKey={} dimensions={}",
                    businessType, accountKey, effectiveDims);
            return null;
        }

        matched.sort(RULE_PRIORITY_COMPARATOR);
        ErpFinGlMappingRule winner = matched.get(0);
        LOG.debug("gl-mapping rule hit: businessType={} accountKey={} → subjectCode={} (priority={} dimensions={})",
                businessType, accountKey, winner.getTargetSubjectCode(), winner.getPriority(), effectiveDims);
        return winner.getTargetSubjectCode();
    }

    @Override
    public void invalidateCache() {
        cache.clear();
        cacheLoaded = false;
        if (isCacheEnabled()) {
            reloadCache();
        }
    }

    // ---------- 内部辅助 ----------

    private static final Comparator<ErpFinGlMappingRule> RULE_PRIORITY_COMPARATOR =
            Comparator.comparingInt((ErpFinGlMappingRule r) -> -safeInt(r.getPriority()))
                    .thenComparingInt(r -> -specificity(r));

    private static int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * 维度具体度：非 NULL 维度字段数（acctSchemaId + partnerGroupId + materialCategoryId + warehouseId +
     * departmentId + projectId + fromOrgId + toOrgId，共 8 维）。数值越大越具体。
     */
    private static int specificity(ErpFinGlMappingRule rule) {
        int count = 0;
        if (rule.getAcctSchemaId() != null) count++;
        if (rule.getPartnerGroupId() != null) count++;
        if (rule.getMaterialCategoryId() != null) count++;
        if (rule.getWarehouseId() != null) count++;
        if (rule.getDepartmentId() != null) count++;
        if (rule.getProjectId() != null) count++;
        return count;
    }

    private boolean matches(ErpFinGlMappingRule rule, GlMappingDimensions dims, Long acctSchemaId) {
        if (rule.getAcctSchemaId() != null && !Objects.equals(rule.getAcctSchemaId(), acctSchemaId)) {
            return false;
        }
        if (rule.getPartnerGroupId() != null && !Objects.equals(rule.getPartnerGroupId(), dims.getPartnerGroupId())) {
            return false;
        }
        if (rule.getMaterialCategoryId() != null
                && !Objects.equals(rule.getMaterialCategoryId(), dims.getMaterialCategoryId())) {
            return false;
        }
        if (rule.getWarehouseId() != null && !Objects.equals(rule.getWarehouseId(), dims.getWarehouseId())) {
            return false;
        }
        if (rule.getDepartmentId() != null && !Objects.equals(rule.getDepartmentId(), dims.getDepartmentId())) {
            return false;
        }
        if (rule.getProjectId() != null && !Objects.equals(rule.getProjectId(), dims.getProjectId())) {
            return false;
        }
        return true;
    }

    /**
     * 维度扩展（best-effort）：materialId → materialCategoryId；其他维度直接透传。
     * partnerGroupId 不扩展（{@code ErpMdPartnerGroup} 实体不存在），仅当调用方显式传入时使用。
     */
    private GlMappingDimensions expandDimensions(GlMappingDimensions input) {
        if (input == null) {
            return new GlMappingDimensions();
        }
        GlMappingDimensions expanded = new GlMappingDimensions();
        expanded.setPartnerId(input.getPartnerId());
        expanded.setPartnerGroupId(input.getPartnerGroupId());
        expanded.setWarehouseId(input.getWarehouseId());
        expanded.setDepartmentId(input.getDepartmentId());
        expanded.setProjectId(input.getProjectId());
        // A3 intercompany 维度透传（multi-company.md §与 Posting+GL Mapping 关系）
        expanded.setFromOrgId(input.getFromOrgId());
        expanded.setToOrgId(input.getToOrgId());

        // materialId → materialCategoryId 经 ErpMdMaterial.categoryId 扩展
        if (input.getMaterialCategoryId() != null) {
            expanded.setMaterialCategoryId(input.getMaterialCategoryId());
        } else if (input.getMaterialId() != null) {
            Long categoryId = lookupMaterialCategoryId(input.getMaterialId());
            expanded.setMaterialCategoryId(categoryId); // null 表示找不到 → 参与通配
            expanded.setMaterialId(input.getMaterialId());
        } else {
            expanded.setMaterialId(null);
        }
        return expanded;
    }

    private Long lookupMaterialCategoryId(Long materialId) {
        try {
            ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
            return material == null ? null : material.getCategoryId();
        } catch (RuntimeException e) {
            LOG.debug("materialCategoryId lookup 失败 materialId={}: {}", materialId, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 orgId：A1 当前不依赖 orgId 维度（规则表 orgId 字段为多组织隔离预留，匹配逻辑暂不参与）。
     * 返回 {@code null} 使 cache key 退化为 (businessType, accountKey)；后续多组织支持时再扩展。
     */
    private Long resolveOrgIdFromDimensions(GlMappingDimensions dims) {
        return null;
    }

    private boolean isCacheEnabled() {
        return AppConfig.var(CONFIG_CACHE_ENABLED, true);
    }

    private List<ErpFinGlMappingRule> loadFromCache(Long orgId, String businessType, String accountKey) {
        if (!cacheLoaded) {
            reloadCache();
        }
        return cache.getOrDefault(cacheKey(orgId, businessType, accountKey), Collections.emptyList());
    }

    private List<ErpFinGlMappingRule> loadFromDb(Long orgId, String businessType, String accountKey) {
        IEntityDao<ErpFinGlMappingRule> dao = daoProvider.daoFor(ErpFinGlMappingRule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("businessType", businessType));
        q.addFilter(eq("accountKey", accountKey));
        return dao.findAllByQuery(q);
    }

    private synchronized void reloadCache() {
        IEntityDao<ErpFinGlMappingRule> dao = daoProvider.daoFor(ErpFinGlMappingRule.class);
        QueryBean q = new QueryBean();
        // 仅加载未逻辑删除的规则（useLogicalDelete 由 ORM 自动加 delVersion 过滤）
        List<ErpFinGlMappingRule> all = dao.findAllByQuery(q);
        Map<String, List<ErpFinGlMappingRule>> newCache = new HashMap<>();
        for (ErpFinGlMappingRule rule : all) {
            String key = cacheKey(null, rule.getBusinessType(), rule.getAccountKey());
            newCache.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
        }
        cache.clear();
        cache.putAll(newCache);
        cacheLoaded = true;
        lastLoadTimeMillis = System.currentTimeMillis();
        LOG.info("GL 映射规则缓存已加载：{} 条规则，{} 个 (businessType, accountKey) 索引", all.size(), newCache.size());
    }

    private static String cacheKey(Long orgId, String businessType, String accountKey) {
        return (orgId == null ? "_" : orgId.toString()) + ":" + businessType + ":" + accountKey;
    }
}
