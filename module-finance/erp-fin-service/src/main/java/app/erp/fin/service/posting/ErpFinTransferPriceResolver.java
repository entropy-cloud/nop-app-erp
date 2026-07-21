package app.erp.fin.service.posting;

import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.dto.TransferPriceResult;
import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 转移定价规则解析器实现（plan 2026-07-22-1000-1 A3，multi-company.md §转移定价规则模型）。
 *
 * <p>三策略（cost-plus / market / negotiated）解析 + 进程内缓存 + eager load + CRUD 主动失效。
 * 优先级链：精确(fromOrgId+toOrgId+materialId) → materialCategoryId 回落 → 全通配 default → 空匹配返回 {@code null}。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §转移定价规则模型}。
 */
public class ErpFinTransferPriceResolver implements IErpFinTransferPriceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinTransferPriceResolver.class);

    @Inject
    IDaoProvider daoProvider;

    private final Map<String, List<ErpFinIntercompanyTransferPrice>> cache = new ConcurrentHashMap<>();
    private volatile boolean cacheLoaded = false;

    @PostConstruct
    public void init() {
        try {
            reloadCache();
        } catch (RuntimeException e) {
            LOG.warn("转移定价规则缓存初始化失败，降级为每次查 DB：{}", e.getMessage());
            cacheLoaded = false;
        }
    }

    @Override
    public TransferPriceResult resolvePrice(Long fromOrgId, Long toOrgId, Long materialId,
                                            LocalDate businessDate) {
        if (fromOrgId == null || toOrgId == null) {
            return null;
        }
        Long materialCategoryId = lookupMaterialCategoryId(materialId);
        List<ErpFinIntercompanyTransferPrice> candidates = loadCandidates(fromOrgId, toOrgId);

        ErpFinIntercompanyTransferPrice winner = pickBest(candidates, fromOrgId, toOrgId,
                materialId, materialCategoryId, businessDate);
        if (winner == null) {
            LOG.info("transfer-price rule miss: fromOrgId={} toOrgId={} materialId={}", fromOrgId, toOrgId, materialId);
            return null;
        }
        TransferPriceResult result = new TransferPriceResult();
        result.setPricingMethod(winner.getPricingMethod());
        result.setUnitPrice(computeUnitPrice(winner));
        result.setRuleId(winner.getId());
        result.setRuleCode(winner.getCode());
        LOG.debug("transfer-price rule hit: fromOrgId={} toOrgId={} materialId={} → method={} price={} (rule={})",
                fromOrgId, toOrgId, materialId, winner.getPricingMethod(), result.getUnitPrice(), winner.getCode());
        return result;
    }

    @Override
    public void invalidateCache() {
        cache.clear();
        cacheLoaded = false;
        reloadCache();
    }

    // ---------- 内部辅助 ----------

    private ErpFinIntercompanyTransferPrice pickBest(List<ErpFinIntercompanyTransferPrice> candidates,
                                                     Long fromOrgId, Long toOrgId, Long materialId,
                                                     Long materialCategoryId, LocalDate businessDate) {
        List<ErpFinIntercompanyTransferPrice> matched = new ArrayList<>();
        for (ErpFinIntercompanyTransferPrice rule : candidates) {
            if (!Boolean.TRUE.equals(rule.getIsActive())) {
                continue;
            }
            if (rule.getFromOrgId() != null && !Objects.equals(rule.getFromOrgId(), fromOrgId)) {
                continue;
            }
            if (rule.getToOrgId() != null && !Objects.equals(rule.getToOrgId(), toOrgId)) {
                continue;
            }
            if (!matchesMaterial(rule, materialId, materialCategoryId)) {
                continue;
            }
            if (!withinValidity(rule, businessDate)) {
                continue;
            }
            matched.add(rule);
        }
        if (matched.isEmpty()) {
            return null;
        }
        matched.sort((a, b) -> Integer.compare(specificity(b), specificity(a)));
        return matched.get(0);
    }

    private boolean matchesMaterial(ErpFinIntercompanyTransferPrice rule, Long materialId, Long materialCategoryId) {
        if (rule.getMaterialId() != null) {
            return Objects.equals(rule.getMaterialId(), materialId);
        }
        if (rule.getMaterialCategoryId() != null) {
            return Objects.equals(rule.getMaterialCategoryId(), materialCategoryId);
        }
        return true;
    }

    private boolean withinValidity(ErpFinIntercompanyTransferPrice rule, LocalDate businessDate) {
        if (businessDate == null) {
            return true;
        }
        if (rule.getValidFrom() != null && businessDate.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidTo() != null && businessDate.isAfter(rule.getValidTo())) {
            return false;
        }
        return true;
    }

    private int specificity(ErpFinIntercompanyTransferPrice rule) {
        int count = 0;
        if (rule.getFromOrgId() != null) count++;
        if (rule.getToOrgId() != null) count++;
        if (rule.getMaterialId() != null) count += 100;
        else if (rule.getMaterialCategoryId() != null) count += 10;
        return count;
    }

    private BigDecimal computeUnitPrice(ErpFinIntercompanyTransferPrice rule) {
        String method = rule.getPricingMethod();
        BigDecimal base = rule.getFixedPrice() != null ? rule.getFixedPrice() : BigDecimal.ZERO;
        BigDecimal markup = rule.getMarkupRate() != null ? rule.getMarkupRate() : BigDecimal.ZERO;
        if (app.erp.fin.service.ErpFinConstants.TRANSFER_PRICING_COST_PLUS.equals(method)) {
            // COST_PLUS = 成本(此处以 fixedPrice 兜底为成本基线) × (1 + markupRate)
            return base.multiply(BigDecimal.ONE.add(markup));
        }
        // MARKET / NEGOTIATED 均取 fixedPrice（MARKET 真实市场价接入归 successor）
        return base;
    }

    private Long lookupMaterialCategoryId(Long materialId) {
        if (materialId == null) {
            return null;
        }
        try {
            ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(materialId);
            return material == null ? null : material.getCategoryId();
        } catch (RuntimeException e) {
            LOG.debug("materialCategoryId lookup 失败 materialId={}: {}", materialId, e.getMessage());
            return null;
        }
    }

    private List<ErpFinIntercompanyTransferPrice> loadCandidates(Long fromOrgId, Long toOrgId) {
        if (!cacheLoaded) {
            reloadCache();
        }
        List<ErpFinIntercompanyTransferPrice> result = new ArrayList<>();
        // 精确 key + 双向通配 key 合并候选
        result.addAll(cache.getOrDefault(cacheKey(fromOrgId, toOrgId), Collections.emptyList()));
        result.addAll(cache.getOrDefault(cacheKey(null, null), Collections.emptyList()));
        result.addAll(cache.getOrDefault(cacheKey(fromOrgId, null), Collections.emptyList()));
        result.addAll(cache.getOrDefault(cacheKey(null, toOrgId), Collections.emptyList()));
        return result;
    }

    private synchronized void reloadCache() {
        IEntityDao<ErpFinIntercompanyTransferPrice> dao = daoProvider.daoFor(ErpFinIntercompanyTransferPrice.class);
        QueryBean q = new QueryBean();
        List<ErpFinIntercompanyTransferPrice> all = dao.findAllByQuery(q);
        Map<String, List<ErpFinIntercompanyTransferPrice>> newCache = new HashMap<>();
        for (ErpFinIntercompanyTransferPrice rule : all) {
            String key = cacheKey(rule.getFromOrgId(), rule.getToOrgId());
            newCache.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
        }
        cache.clear();
        cache.putAll(newCache);
        cacheLoaded = true;
        LOG.info("转移定价规则缓存已加载：{} 条规则，{} 个 (fromOrgId, toOrgId) 索引", all.size(), newCache.size());
    }

    private static String cacheKey(Long fromOrgId, Long toOrgId) {
        return (fromOrgId == null ? "_" : fromOrgId.toString()) + ":"
                + (toOrgId == null ? "_" : toOrgId.toString());
    }
}
