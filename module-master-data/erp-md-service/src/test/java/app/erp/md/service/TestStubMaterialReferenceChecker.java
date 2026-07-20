package app.erp.md.service;

import app.erp.md.spi.IErpMdMaterialReferenceChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 测试专用物料引用计数 SPI 桩（plan 2026-07-20-1020-2 Phase 2）。
 *
 * <p>替代真实下游域（purchase/sales/inventory）的引用计数实现（master-data 不得反向依赖下游域）。
 * 由 {@code test-material-reference-checker.beans.xml} 注册，经 {@code @Nullable @Inject} 注入到
 * {@code ErpMdMaterialBizModel.materialReferenceChecker}。
 *
 * <p>测试方法在 seed 阶段调 {@link #markReferenced} 标记被引用的 materialId 及其引用计数 Map。
 */
public class TestStubMaterialReferenceChecker implements IErpMdMaterialReferenceChecker {

    private final Map<Long, Map<String, Long>> refs = new HashMap<>();
    private final Set<Long> knownIds = new HashSet<>();

    @Override
    public Map<String, Long> countReferences(Long materialId) {
        if (materialId == null) {
            return java.util.Collections.emptyMap();
        }
        knownIds.add(materialId);
        Map<String, Long> m = refs.get(materialId);
        return m == null ? java.util.Collections.emptyMap() : new HashMap<>(m);
    }

    public void markReferenced(Long materialId, String domain, long count) {
        refs.computeIfAbsent(materialId, k -> new HashMap<>()).put(domain, count);
    }

    public void clear() {
        refs.clear();
        knownIds.clear();
    }
}
