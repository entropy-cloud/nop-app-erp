package app.erp.ct.service.rebate;

import app.erp.contract.dao.entity.ErpCtRebateTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P1-CK-ct-003 边界回归（plan 2026-09-12-0400-1 Phase 1）：tier 区间上界含（owner doc
 * volume-discount.md「toAmount 截止金额（含）」权威语义）——修复前 matchTier 对 toAmount 排他
 * （`< 0`），边界命中落空整档归零。
 */
public class TestRebateTierBoundary {

    private ErpCtRebateTier tier(String from, String to, String percent) {
        ErpCtRebateTier t = new ErpCtRebateTier();
        t.setFromAmount(from == null ? null : new BigDecimal(from));
        t.setToAmount(to == null ? null : new BigDecimal(to));
        t.setRebatePercent(new BigDecimal(percent));
        return t;
    }

    @Test
    public void testBoundaryAmountHitsItsOwnTier() {
        RebateEngine engine = new RebateEngine();
        List<ErpCtRebateTier> tiers = List.of(
                tier("0", "1000000", "2"),
                tier("1500000", null, "5"));
        // amount 恰等于 toAmount=1M（且次档 from=1.5M 有间隙）→ 应命中档 1（修复前：排他落空 → null）
        ErpCtRebateTier hit = engine.matchTier(tiers, new BigDecimal("1000000"));
        assertNotNull(hit, "边界金额 1M 应命中 [0,1M] 档（含上界）");
        assertEquals(0, new BigDecimal("1000000").compareTo(hit.getToAmount()));
    }

    @Test
    public void testAdjacentTiersUpperBoundGoesToUpperTier() {
        RebateEngine engine = new RebateEngine();
        List<ErpCtRebateTier> tiers = List.of(
                tier("0", "1000000", "2"),
                tier("1000000", null, "5"));
        // 紧邻档（次档 from=前档 to）配置：边界值归属由 from ≥ 命中优先（max fromAmount 匹配）
        ErpCtRebateTier hit = engine.matchTier(tiers, new BigDecimal("1000000"));
        assertNotNull(hit);
        assertEquals(0, new BigDecimal("5").compareTo(hit.getRebatePercent()),
                "紧邻档配置下边界值命中更高档（from 也是含）");
    }

    @Test
    public void testBelowAllTiersReturnsNull() {
        RebateEngine engine = new RebateEngine();
        List<ErpCtRebateTier> tiers = List.of(tier("500000", "1000000", "2"));
        org.junit.jupiter.api.Assertions.assertNull(engine.matchTier(tiers, new BigDecimal("100")),
                "低于全部档仍不命中");
    }
}
