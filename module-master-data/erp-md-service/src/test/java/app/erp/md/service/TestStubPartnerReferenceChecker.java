package app.erp.md.service;

import app.erp.md.spi.IErpMdPartnerReferenceChecker;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试专用往来单位引用计数 SPI 桩（plan 2026-07-20-1020-2 Phase 2）。
 *
 * <p>替代真实下游域（purchase/sales/inventory）的引用计数实现（master-data 不得反向依赖下游域）。
 * 由 {@code test-partner-reference-checker.beans.xml} 注册，经 {@code @Nullable @Inject} 注入到
 * {@code ErpMdPartnerBizModel.partnerReferenceChecker}。
 */
public class TestStubPartnerReferenceChecker implements IErpMdPartnerReferenceChecker {

    private final Map<Long, Map<String, Long>> refs = new HashMap<>();

    @Override
    public Map<String, Long> countReferences(Long partnerId) {
        if (partnerId == null) {
            return java.util.Collections.emptyMap();
        }
        Map<String, Long> m = refs.get(partnerId);
        return m == null ? java.util.Collections.emptyMap() : new HashMap<>(m);
    }

    public void markReferenced(Long partnerId, String domain, long count) {
        refs.computeIfAbsent(partnerId, k -> new HashMap<>()).put(domain, count);
    }

    public void clear() {
        refs.clear();
    }
}
