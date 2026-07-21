package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.dao.entity.ErpMdMaterialCategory;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSupplierApproval;
import app.erp.md.dao.entity.ErpMdTaxRate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * C3 日期范围有效性模式试点实体集成测试
 * （plan 2026-07-21-2225-1 Phase 3，docs/design/date-ranged-validity-pattern.md §7）。
 *
 * <p>覆盖 3 试点实体的 MUTEX 区间互斥校验（经 GraphQL {@code __save} 路径触发
 * {@code defaultPrepareSave} 钩子）：
 * <ol>
 *   <li>{@code ErpMdExchangeRate}：同 fromCurrency/toCurrency/rateType 维度互斥</li>
 *   <li>{@code ErpMdTaxRate}：同 taxType 维度互斥</li>
 *   <li>{@code ErpMdSupplierApproval}：同 partnerId 维度互斥（status != REJECTED 时）</li>
 * </ol>
 *
 * <p>每个试点覆盖：正路径（不重叠通过）+ 负路径（重叠拒绝）+ 边界（相邻日不重叠通过）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdDateRangePilots extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ErpMdExchangeRate ----------

    @Test
    public void exchangeRate_overlapSameCurrencyPairRejected() {
        Long usd = seedCurrency("USD-C3-1");
        Long cny = seedCurrency("CNY-C3-1");

        // 第一条 [2026-01-01..2026-06-30]
        saveExchangeRateOk(usd, cny, "SPOT", "2026-01-01", "2026-06-30");

        // 第二条同维度重叠 [2026-04-01..2026-12-31] → 拒绝
        ApiResponse<?> resp = saveExchangeRate(usd, cny, "SPOT", "2026-04-01", "2026-12-31");
        assertNotEquals(0, resp.getStatus(),
                "同币种对+同 rateType 重叠应被拒绝（status=" + resp.getStatus() + "）");
        assertEquals(ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP.getErrorCode(), resp.getCode(),
                "错误码应为 ERR_MD_DATE_RANGE_OVERLAP");
    }

    @Test
    public void exchangeRate_adjacentDayNoOverlapPasses() {
        Long usd = seedCurrency("USD-C3-2");
        Long cny = seedCurrency("CNY-C3-2");

        // 第一条 [2026-01-01..2026-06-30]
        saveExchangeRateOk(usd, cny, "SPOT", "2026-01-01", "2026-06-30");

        // 第二条相邻 [2026-07-01..2026-12-31] → 通过
        saveExchangeRateOk(usd, cny, "SPOT", "2026-07-01", "2026-12-31");
    }

    @Test
    public void exchangeRate_differentRateTypeOverlapPasses() {
        Long usd = seedCurrency("USD-C3-3");
        Long cny = seedCurrency("CNY-C3-3");

        // 第一条 SPOT [2026-01-01..2026-12-31]
        saveExchangeRateOk(usd, cny, "SPOT", "2026-01-01", "2026-12-31");

        // 第二条同币种对但不同 rateType=AVERAGE，区间重叠 → 通过（不同维度）
        saveExchangeRateOk(usd, cny, "AVERAGE", "2026-03-01", "2026-09-30");
    }

    @Test
    public void exchangeRate_updateSelfExcluded() {
        Long usd = seedCurrency("USD-C3-4");
        Long cny = seedCurrency("CNY-C3-4");

        // 新建 [2026-01-01..2026-06-30]
        Map<?, ?> saved = saveExchangeRateOk(usd, cny, "SPOT", "2026-01-01", "2026-06-30");
        String id = String.valueOf(saved.get("id"));

        // 更新自身（同区间或微调）→ 不应与自身冲突
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("id", id);
        update.put("rate", new BigDecimal("7.50"));
        ApiResponse<?> resp = rpc(mutation, "ErpMdExchangeRate__update", ApiRequest.build(Map.of("data", update)));
        assertEquals(0, resp.getStatus(),
                "更新自身不应触发冲突（status=" + resp.getStatus() + " code=" + resp.getCode() + "）");
    }

    // ---------- ErpMdTaxRate ----------

    @Test
    public void taxRate_overlapSameTaxTypeRejected() {
        // 第一条 VAT [2026-01-01..2026-06-30]
        saveTaxRateOk("VAT-C3-1", "VAT", "2026-01-01", "2026-06-30");

        // 第二条同 taxType=VAT 重叠 [2026-03-01..2026-12-31] → 拒绝
        ApiResponse<?> resp = saveTaxRate("VAT-C3-2", "VAT", "2026-03-01", "2026-12-31");
        assertNotEquals(0, resp.getStatus(),
                "同 taxType 重叠应被拒绝（status=" + resp.getStatus() + "）");
        assertEquals(ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP.getErrorCode(), resp.getCode());
    }

    @Test
    public void taxRate_adjacentDayPasses() {
        saveTaxRateOk("VAT-C3-3", "VAT", "2026-01-01", "2026-06-30");
        saveTaxRateOk("VAT-C3-4", "VAT", "2026-07-01", "2026-12-31");
    }

    @Test
    public void taxRate_differentTaxTypeOverlapPasses() {
        saveTaxRateOk("VAT-C3-5", "VAT", "2026-01-01", "2026-12-31");
        saveTaxRateOk("ST-C3-6", "SALES_TAX", "2026-01-01", "2026-12-31");
    }

    // ---------- ErpMdSupplierApproval ----------

    @Test
    public void supplierApproval_overlapSamePartnerRejected() {
        Long partnerId = seedPartner("C3-PARTNER-1");
        Long categoryId = seedMaterialCategory("C3-CAT-1");

        // 第一条 [2026-01-01..2026-06-30] APPLIED
        saveApprovalOk(partnerId, categoryId, "APPLIED", "2026-01-01", "2026-06-30");

        // 第二条同 partnerId 重叠 [2026-03-01..2026-12-31] → 拒绝
        ApiResponse<?> resp = saveApproval(partnerId, categoryId, "APPLIED", "2026-03-01", "2026-12-31");
        assertNotEquals(0, resp.getStatus(),
                "同 partnerId 重叠应被拒绝（status=" + resp.getStatus() + "）");
        assertEquals(ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP.getErrorCode(), resp.getCode());
    }

    @Test
    public void supplierApproval_rejectedStatusOverlapPasses() {
        Long partnerId = seedPartner("C3-PARTNER-2");
        Long categoryId = seedMaterialCategory("C3-CAT-2");

        // 第一条 REJECTED [2026-01-01..2026-12-31]（status=REJECTED 不参与互斥）
        saveApprovalOk(partnerId, categoryId, "REJECTED", "2026-01-01", "2026-12-31");

        // 第二条同 partnerId 同区间但 APPLIED → 通过（REJECTED 不算占用区间）
        saveApprovalOk(partnerId, categoryId, "APPLIED", "2026-01-01", "2026-12-31");
    }

    @Test
    public void supplierApproval_adjacentDayPasses() {
        Long partnerId = seedPartner("C3-PARTNER-3");
        Long categoryId = seedMaterialCategory("C3-CAT-3");
        saveApprovalOk(partnerId, categoryId, "APPLIED", "2026-01-01", "2026-06-30");
        saveApprovalOk(partnerId, categoryId, "APPLIED", "2026-07-01", "2026-12-31");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long seedCurrency(String code) {
        ErpMdCurrency c = new ErpMdCurrency();
        c.setCode(code);
        c.setName("E2E-" + code);
        c.setIsActive(true);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c));
        return c.getId();
    }

    private Long seedPartner(String code) {
        ErpMdPartner p = new ErpMdPartner();
        p.setCode(code);
        p.setName("E2E-" + code);
        p.setPartnerType("SUPPLIER");
        p.setStatus("ACTIVE");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMdPartner.class).saveEntity(p));
        return p.getId();
    }

    private Long seedMaterialCategory(String code) {
        ErpMdMaterialCategory c = new ErpMdMaterialCategory();
        c.setCode(code);
        c.setName("E2E-" + code);
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpMdMaterialCategory.class).saveEntity(c));
        return c.getId();
    }

    private Map<String, Object> exchangeRatePayload(Long from, Long to, String rateType,
                                                     String validFrom, String validTo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fromCurrencyId", String.valueOf(from));
        data.put("toCurrencyId", String.valueOf(to));
        data.put("rateType", rateType);
        data.put("rate", new BigDecimal("7.0"));
        data.put("validFrom", validFrom);
        if (validTo != null) {
            data.put("validTo", validTo);
        }
        return data;
    }

    private ApiResponse<?> saveExchangeRate(Long from, Long to, String rateType,
                                            String validFrom, String validTo) {
        return rpc(mutation, "ErpMdExchangeRate__save",
                ApiRequest.build(Map.of("data", exchangeRatePayload(from, to, rateType, validFrom, validTo))));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> saveExchangeRateOk(Long from, Long to, String rateType,
                                          String validFrom, String validTo) {
        ApiResponse<?> resp = saveExchangeRate(from, to, rateType, validFrom, validTo);
        assertEquals(0, resp.getStatus(),
                "exchangeRate save 应成功，实际 status=" + resp.getStatus() + " code=" + resp.getCode()
                        + " body=" + resp.toString());
        return (Map<String, Object>) resp.getData();
    }

    private Map<String, Object> taxRatePayload(String code, String taxType,
                                                String validFrom, String validTo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("name", "E2E-" + code);
        data.put("taxType", taxType);
        data.put("rate", new BigDecimal("13.00"));
        data.put("validFrom", validFrom);
        if (validTo != null) {
            data.put("validTo", validTo);
        }
        data.put("status", "ACTIVE");
        return data;
    }

    private ApiResponse<?> saveTaxRate(String code, String taxType,
                                       String validFrom, String validTo) {
        return rpc(mutation, "ErpMdTaxRate__save",
                ApiRequest.build(Map.of("data", taxRatePayload(code, taxType, validFrom, validTo))));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> saveTaxRateOk(String code, String taxType,
                                    String validFrom, String validTo) {
        ApiResponse<?> resp = saveTaxRate(code, taxType, validFrom, validTo);
        assertEquals(0, resp.getStatus(),
                "taxRate save 应成功，实际 status=" + resp.getStatus() + " code=" + resp.getCode());
        return (Map<String, Object>) resp.getData();
    }

    private Map<String, Object> approvalPayload(Long partnerId, Long categoryId, String status,
                                                 String validFrom, String validTo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("partnerId", String.valueOf(partnerId));
        data.put("approvalType", "NEW");
        data.put("materialCategoryId", String.valueOf(categoryId));
        data.put("validFrom", validFrom);
        data.put("validTo", validTo);
        data.put("qualificationDoc", "ISO9001");
        data.put("status", status);
        return data;
    }

    private ApiResponse<?> saveApproval(Long partnerId, Long categoryId, String status,
                                        String validFrom, String validTo) {
        return rpc(mutation, "ErpMdSupplierApproval__save",
                ApiRequest.build(Map.of("data", approvalPayload(partnerId, categoryId, status, validFrom, validTo))));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> saveApprovalOk(Long partnerId, Long categoryId, String status,
                                     String validFrom, String validTo) {
        ApiResponse<?> resp = saveApproval(partnerId, categoryId, status, validFrom, validTo);
        assertEquals(0, resp.getStatus(),
                "approval save 应成功，实际 status=" + resp.getStatus() + " code=" + resp.getCode()
                        + " body=" + resp.toString());
        return (Map<String, Object>) resp.getData();
    }
}
