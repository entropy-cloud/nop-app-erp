package app.erp.ct.service.entity;

import app.erp.contract.dao.entity.ErpCtVolumeDiscount;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-CK-ct-003 边界回归（量折扣侧，plan 2026-09-12-0400-1 Phase 1）：matchBand toQty 含上界
 * （owner doc「toQty 截止数量（含）」）+ validateNoOverlap 闭区间重叠判定同步。
 */
public class TestVolumeDiscountBandBoundary {

    private ErpCtVolumeDiscount band(String from, String to, String discountPercent) {
        ErpCtVolumeDiscount b = new ErpCtVolumeDiscount();
        b.setFromQty(from == null ? null : new BigDecimal(from));
        b.setToQty(to == null ? null : new BigDecimal(to));
        b.setDiscountPercent(discountPercent == null ? null : new BigDecimal(discountPercent));
        return b;
    }

    @Test
    public void testBoundaryQtyHitsItsOwnBand() {
        ErpCtVolumeDiscountBizModel biz = new ErpCtVolumeDiscountBizModel();
        List<ErpCtVolumeDiscount> bands = List.of(
                band("0", "100", "5"),
                band("150", "500", "10"));
        // qty 恰等于 toQty=100（次档 from=150 有间隙）→ 应命中 band 1（修复前：排他跳过 → null）
        ErpCtVolumeDiscount hit = biz.matchBand(bands, new BigDecimal("100"));
        assertNotNull(hit, "边界数量 100 应命中 [0,100] 档（含上界）");
        assertEquals(0, new BigDecimal("100").compareTo(hit.getToQty()));
    }

    @Test
    public void testOverlapsClosedIntervalDetectsSharedBoundary() {
        ErpCtVolumeDiscountBizModel biz = new ErpCtVolumeDiscountBizModel();
        // 闭区间语义：[0,100] 与 [100,200] 共享边界点 100 → 重叠（半开旧判定为 false）
        assertTrue(biz.overlaps(new BigDecimal("0"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("200")), "共享边界点应判重叠");
        // from=to+1 相邻配置不重叠
        org.junit.jupiter.api.Assertions.assertFalse(biz.overlaps(new BigDecimal("0"), new BigDecimal("100"),
                new BigDecimal("101"), new BigDecimal("200")), "from=to+1 相邻不重叠");
    }
}
