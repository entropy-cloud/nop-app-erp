package app.erp.sal.service.support;

import app.erp.sal.dao.entity.ErpSalPriceListLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-CK-sal-008：价格清单行「最优命中」确定性 comparator 纯函数测试——
 * skuId 专属命中 > materialId 通用命中 > 更窄数量阶梯（minQuantity 更大）> validFrom 更晚 > id 兜底。
 * 修复原 matchLine 取 DB 返回序第一条的不确定性。
 */
public class TestErpSalPriceLineDeterminism {

    private final ErpSalCustomerPriceResolver resolver = new ErpSalCustomerPriceResolver();

    @Test
    public void testSkuSpecificBeatsMaterialGeneric() {
        ErpSalPriceListLine sku = line("id-1", "sku-1", null, "10", null);
        ErpSalPriceListLine material = line("id-2", null, "mat-1", "10", null);
        assertTrue(resolver.comparePriceLineCandidates(sku, material) < 0,
                "skuId 专属命中优于 materialId 通用命中");
        assertTrue(resolver.comparePriceLineCandidates(material, sku) > 0);
    }

    @Test
    public void testNarrowerQuantityTierWins() {
        ErpSalPriceListLine narrow = line("id-1", null, "mat-1", "50", null);
        ErpSalPriceListLine wide = line("id-2", null, "mat-1", "10", null);
        assertTrue(resolver.comparePriceLineCandidates(narrow, wide) < 0,
                "更窄数量阶梯（minQuantity 更大）优先");
    }

    @Test
    public void testLaterValidFromWins() {
        ErpSalPriceListLine later = line("id-1", null, "mat-1", "10", LocalDate.of(2026, 6, 1));
        ErpSalPriceListLine earlier = line("id-2", null, "mat-1", "10", LocalDate.of(2026, 1, 1));
        assertTrue(resolver.comparePriceLineCandidates(later, earlier) < 0,
                "validFrom 更晚（更近生效定价）优先");
    }

    @Test
    public void testIdFallbackDeterministic() {
        ErpSalPriceListLine a = line("id-1", null, "mat-1", "10", null);
        ErpSalPriceListLine b = line("id-2", null, "mat-1", "10", null);
        assertEquals(0, resolver.comparePriceLineCandidates(a, a), "同元素比较为 0");
        assertTrue(resolver.comparePriceLineCandidates(a, b) < 0, "id 字符串序兜底（稳定确定性）");
    }

    private ErpSalPriceListLine line(String id, String skuId, String materialId, String minQty,
                                     LocalDate validFrom) {
        ErpSalPriceListLine line = new ErpSalPriceListLine();
        line.setId(id);
        line.setSkuId(skuId);
        line.setMaterialId(materialId);
        if (minQty != null) {
            line.setMinQuantity(new BigDecimal(minQty));
        }
        line.setValidFrom(validFrom);
        return line;
    }
}
