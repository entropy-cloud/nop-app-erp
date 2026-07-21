package app.erp.fin.service.posting;

import app.erp.fin.dao.api.IErpFinTransferPriceResolver;
import app.erp.fin.dao.dto.TransferPriceResult;
import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A3 转移定价解析器测试（plan 2026-07-22-1000-1 §Phase 2 Proof）。
 *
 * <p>覆盖三策略（cost-plus / market / negotiated）+ 精确匹配 + 通配回落 + 缓存失效。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:intercompany-test.yaml")
public class TestErpFinTransferPriceResolver extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinTransferPriceResolver resolver;

    @Test
    public void testCostPlusStrategy() {
        Long fromOrg = 1001L;
        Long toOrg = 1002L;
        seedReturn(() -> {
            seedRule("TP-COSTPLUS", fromOrg, toOrg, null, null,
                    "COST_PLUS", new BigDecimal("0.10"), new BigDecimal("100"), null);
            return null;
        });
        resolver.invalidateCache();

        TransferPriceResult result = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 7, 1));
        assertNotNull(result, "COST_PLUS 规则应命中");
        assertEquals("COST_PLUS", result.getPricingMethod());
        // 100 × (1 + 0.10) = 110
        assertEquals(0, result.getUnitPrice().compareTo(new BigDecimal("110")),
                "COST_PLUS 单价应为 110");
    }

    @Test
    public void testNegotiatedStrategy() {
        Long fromOrg = 1003L;
        Long toOrg = 1004L;
        seedReturn(() -> {
            seedRule("TP-NEG", fromOrg, toOrg, null, null,
                    "NEGOTIATED", null, new BigDecimal("250"), null);
            return null;
        });
        resolver.invalidateCache();

        TransferPriceResult result = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 7, 1));
        assertNotNull(result);
        assertEquals("NEGOTIATED", result.getPricingMethod());
        assertEquals(0, result.getUnitPrice().compareTo(new BigDecimal("250")));
    }

    @Test
    public void testMarketStrategyFallbackToFixedPrice() {
        Long fromOrg = 1005L;
        Long toOrg = 1006L;
        seedReturn(() -> {
            seedRule("TP-MKT", fromOrg, toOrg, null, null,
                    "MARKET", null, new BigDecimal("300"), "上月采购均价");
            return null;
        });
        resolver.invalidateCache();

        TransferPriceResult result = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 7, 1));
        assertNotNull(result);
        assertEquals("MARKET", result.getPricingMethod());
        assertEquals(0, result.getUnitPrice().compareTo(new BigDecimal("300")));
    }

    @Test
    public void testWildcardFallbackToDefault() {
        // 全通配 default 规则（fromOrgId/toOrgId 均为 null）
        seedReturn(() -> {
            seedRule("TP-DEFAULT", null, null, null, null,
                    "NEGOTIATED", null, new BigDecimal("180"), null);
            return null;
        });
        resolver.invalidateCache();

        TransferPriceResult result = resolver.resolvePrice(9999L, 8888L, null, LocalDate.of(2026, 7, 1));
        assertNotNull(result, "应回落到全通配 default 规则");
        assertEquals("NEGOTIATED", result.getPricingMethod());
        assertEquals(0, result.getUnitPrice().compareTo(new BigDecimal("180")));
    }

    @Test
    public void testNoMatchReturnsNull() {
        // 清缓存后查不存在的组合（无 default 规则覆盖）
        resolver.invalidateCache();
        TransferPriceResult result = resolver.resolvePrice(7777L, 6666L, null, LocalDate.of(2026, 7, 1));
        assertNull(result, "无匹配规则且无 default 时应返回 null");
    }

    @Test
    public void testCacheInvalidate() {
        Long fromOrg = 1007L;
        Long toOrg = 1008L;
        seedReturn(() -> {
            seedRule("TP-CACHE", fromOrg, toOrg, null, null,
                    "NEGOTIATED", null, new BigDecimal("500"), null);
            return null;
        });
        resolver.invalidateCache();

        TransferPriceResult before = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 7, 1));
        assertNotNull(before);
        assertEquals(0, before.getUnitPrice().compareTo(new BigDecimal("500")));

        // 失效缓存后重新查询仍命中
        resolver.invalidateCache();
        TransferPriceResult after = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 7, 1));
        assertNotNull(after);
        assertEquals(0, after.getUnitPrice().compareTo(new BigDecimal("500")));
    }

    @Test
    public void testValidityPeriodFiltering() {
        Long fromOrg = 1009L;
        Long toOrg = 1010L;
        seedReturn(() -> {
            seedRule("TP-VALID", fromOrg, toOrg, null, null,
                    "NEGOTIATED", null, new BigDecimal("200"), null,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
            return null;
        });
        resolver.invalidateCache();

        // 业务日期在有效期内
        TransferPriceResult inRange = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 3, 15));
        assertNotNull(inRange, "有效期内应命中");
        assertEquals(0, inRange.getUnitPrice().compareTo(new BigDecimal("200")));

        // 业务日期在有效期外（之后）→ 该精确规则不命中，可能回落 default 或 null
        TransferPriceResult outOfRange = resolver.resolvePrice(fromOrg, toOrg, null, LocalDate.of(2026, 7, 1));
        // 7月不在 1-6月区间内，精确规则不命中；若无 default 则 null
        assertNull(outOfRange, "有效期外精确规则不命中，且无 default → null");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private void seedRule(String code, Long fromOrgId, Long toOrgId, Long materialId, Long materialCategoryId,
                          String method, BigDecimal markupRate, BigDecimal fixedPrice, String marketRefSource) {
        seedRule(code, fromOrgId, toOrgId, materialId, materialCategoryId, method, markupRate, fixedPrice,
                marketRefSource, null, null);
    }

    private void seedRule(String code, Long fromOrgId, Long toOrgId, Long materialId, Long materialCategoryId,
                          String method, BigDecimal markupRate, BigDecimal fixedPrice, String marketRefSource,
                          LocalDate validFrom, LocalDate validTo) {
        IEntityDao<ErpFinIntercompanyTransferPrice> dao = daoProvider.daoFor(ErpFinIntercompanyTransferPrice.class);
        ErpFinIntercompanyTransferPrice rule = new ErpFinIntercompanyTransferPrice();
        rule.setCode(code);
        rule.setName(code);
        rule.setOrgId(1L);
        rule.setFromOrgId(fromOrgId);
        rule.setToOrgId(toOrgId);
        rule.setMaterialId(materialId);
        rule.setMaterialCategoryId(materialCategoryId);
        rule.setPricingMethod(method);
        rule.setMarkupRate(markupRate);
        rule.setFixedPrice(fixedPrice);
        rule.setMarketRefSource(marketRefSource);
        rule.setValidFrom(validFrom);
        rule.setValidTo(validTo);
        rule.setIsActive(true);
        dao.saveEntity(rule);
    }
}
