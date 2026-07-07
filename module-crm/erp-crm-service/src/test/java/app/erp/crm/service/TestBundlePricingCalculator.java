package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmBundlePricing;
import app.erp.crm.dao.entity.ErpCrmBundlePricingLine;
import app.erp.crm.service.support.BundlePricingCalculator;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CPQ 捆绑定价计算器单元测试（plan 2026-07-07-1430-2 §Phase 3）。
 *
 * <p>纯函数式：无 DB 依赖、无 IoC、无 XLang，使用 {@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>覆盖：PERCENTAGE/FIXED 折扣、bundleAmount 覆盖、空行处理。
 */
public class TestBundlePricingCalculator extends BaseTestCase {

    private final BundlePricingCalculator calculator = new BundlePricingCalculator();

    @Test
    public void testPercentageDiscount() {
        ErpCrmBundlePricing bundle = newBundle("PERCENTAGE", BigDecimal.valueOf(15), null);
        List<ErpCrmBundlePricingLine> lines = List.of(
                newLine(BigDecimal.valueOf(100000), BigDecimal.valueOf(1)),
                newLine(BigDecimal.valueOf(5000), BigDecimal.valueOf(1)),
                newLine(BigDecimal.valueOf(15000), BigDecimal.valueOf(1))
        );
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, lines);
        assertEquals(0, result.getSubtotal().compareTo(BigDecimal.valueOf(120000)),
                "subtotal = 100000 + 5000 + 15000");
        assertEquals(0, result.getFinalAmount().compareTo(BigDecimal.valueOf(102000.0)),
                "120000 × (1 - 15/100) = 102000");
        assertEquals("PERCENTAGE", result.getAppliedRule());
    }

    @Test
    public void testFixedDiscount() {
        ErpCrmBundlePricing bundle = newBundle("FIXED", BigDecimal.valueOf(5000), null);
        List<ErpCrmBundlePricingLine> lines = List.of(
                newLine(BigDecimal.valueOf(50000), BigDecimal.valueOf(2))   // 100000
        );
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, lines);
        assertEquals(0, result.getSubtotal().compareTo(BigDecimal.valueOf(100000)));
        assertEquals(0, result.getFinalAmount().compareTo(BigDecimal.valueOf(95000)),
                "100000 - 5000 = 95000");
        assertEquals("FIXED", result.getAppliedRule());
    }

    @Test
    public void testBundleAmountOverride() {
        ErpCrmBundlePricing bundle = newBundle("PERCENTAGE", BigDecimal.valueOf(15),
                BigDecimal.valueOf(100000));
        List<ErpCrmBundlePricingLine> lines = List.of(
                newLine(BigDecimal.valueOf(100000), BigDecimal.valueOf(1)),
                newLine(BigDecimal.valueOf(20000), BigDecimal.valueOf(1))
        );
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, lines);
        assertEquals(0, result.getSubtotal().compareTo(BigDecimal.valueOf(120000)));
        assertEquals(0, result.getFinalAmount().compareTo(BigDecimal.valueOf(100000)),
                "bundleAmount=100000 覆盖折扣计算");
        assertEquals("BUNDLE_AMOUNT_OVERRIDE", result.getAppliedRule());
    }

    @Test
    public void testFixedDiscountNotNegative() {
        ErpCrmBundlePricing bundle = newBundle("FIXED", BigDecimal.valueOf(200000), null);
        List<ErpCrmBundlePricingLine> lines = List.of(
                newLine(BigDecimal.valueOf(50000), BigDecimal.valueOf(1))
        );
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, lines);
        assertEquals(0, result.getFinalAmount().compareTo(BigDecimal.ZERO),
                "FIXED 折扣超出 subtotal 时取 0（不低于 0）");
    }

    @Test
    public void testEmptyLines() {
        ErpCrmBundlePricing bundle = newBundle("PERCENTAGE", BigDecimal.valueOf(15), null);
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, List.of());
        assertEquals(0, result.getSubtotal().compareTo(BigDecimal.ZERO), "空行 subtotal=0");
        assertEquals(0, result.getFinalAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testNullLines() {
        ErpCrmBundlePricing bundle = newBundle("PERCENTAGE", BigDecimal.valueOf(15), null);
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, null);
        assertEquals(0, result.getSubtotal().compareTo(BigDecimal.ZERO), "null lines 安全返回 subtotal=0");
    }

    @Test
    public void testNoDiscount() {
        ErpCrmBundlePricing bundle = newBundle(null, null, null);
        List<ErpCrmBundlePricingLine> lines = List.of(
                newLine(BigDecimal.valueOf(10000), BigDecimal.valueOf(2))
        );
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, lines);
        assertEquals(0, result.getFinalAmount().compareTo(BigDecimal.valueOf(20000)),
                "无 discountType/value → subtotal 即 finalAmount");
        assertEquals("NO_DISCOUNT", result.getAppliedRule());
    }

    @Test
    public void testNullFieldsSkipped() {
        ErpCrmBundlePricing bundle = newBundle("PERCENTAGE", BigDecimal.valueOf(10), null);
        // 含 null unitPrice/quantity 的行应被跳过
        ErpCrmBundlePricingLine bad = new ErpCrmBundlePricingLine();
        bad.setUnitPrice(null);
        bad.setQuantity(BigDecimal.valueOf(2));
        List<ErpCrmBundlePricingLine> lines = List.of(
                bad,
                newLine(BigDecimal.valueOf(5000), BigDecimal.valueOf(2))  // 10000
        );
        BundlePricingCalculator.BundleResult result = calculator.calculate(bundle, lines);
        assertTrue(result.getSubtotal().compareTo(BigDecimal.valueOf(10000)) == 0,
                "null unitPrice 行被跳过，subtotal=10000");
    }

    private ErpCrmBundlePricing newBundle(String discountType, BigDecimal discountValue, BigDecimal bundleAmount) {
        ErpCrmBundlePricing bundle = new ErpCrmBundlePricing();
        bundle.setDiscountType(discountType);
        bundle.setDiscountValue(discountValue);
        bundle.setBundleAmount(bundleAmount);
        return bundle;
    }

    private ErpCrmBundlePricingLine newLine(BigDecimal unitPrice, BigDecimal quantity) {
        ErpCrmBundlePricingLine line = new ErpCrmBundlePricingLine();
        line.setUnitPrice(unitPrice);
        line.setQuantity(quantity);
        return line;
    }
}
