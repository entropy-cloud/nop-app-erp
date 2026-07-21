package app.erp.fin.service.posting;

import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import app.erp.fin.dao.dto.GlMappingDimensions;
import app.erp.fin.dao.entity.ErpFinGlMappingRule;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * GL 映射规则解析器单元测试（plan 2026-07-21-0827-1 A1 Phase 2）。
 *
 * <p>覆盖 8 场景：(a) exact match 优先 / (b) partial-wildcard / (c) default fallback / (d) 空匹配 null /
 * (e) acctSchemaId specific > wildcard / (f) priority 打破并列维度 / (g) 维度扩展（materialId → materialCategoryId）/
 * (h) 缓存失效后 reload。
 *
 * <p>种子规则经 DAO 直建（与 {@code TestErpPurInvoicePosting.seedPeriodAndSubjects} 同范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinGlMappingResolver extends JunitAutoTestCase {

    static final String BT = "AP_INVOICE";
    static final String AK_PURCHASE = "PURCHASE";
    static final Long ACCT_SCHEMA_7 = 7L;
    static final Long ACCT_SCHEMA_8 = 8L;
    static final Long PARTNER_GROUP_A = 100L;
    static final Long PARTNER_GROUP_B = 200L;
    static final Long MATERIAL_CATEGORY_42 = 42L;
    static final Long MATERIAL_CATEGORY_43 = 43L;
    static final Long WAREHOUSE_1 = 101L;
    static final Long MATERIAL_ID_5001 = 5001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinGlMappingResolver resolver;

    /**
     * (a) exact match 优先：materialCategoryId=42 命中精确规则 R3 而非 default R1。
     */
    @Test
    public void testExactMatchWins() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-A-DEFAULT", BT, AK_PURCHASE, null, null, null,
                    null, null, null, null, "1403", 0);
            seedRule("RULE-A-EXACT-MAT42", BT, AK_PURCHASE, null, null, MATERIAL_CATEGORY_42,
                    null, null, null, null, "1404", 100);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setMaterialCategoryId(MATERIAL_CATEGORY_42);
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1404", result, "exact match materialCategoryId=42 应命中 priority=100 精确规则");
    }

    /**
     * (b) partial-wildcard：维度部分通配命中（acctSchemaId=null + materialCategoryId=null + partnerGroupId=A）。
     */
    @Test
    public void testPartialWildcardMatch() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-B-DEFAULT", BT, AK_PURCHASE, null, null, null, null, null, null, null, "1403", 0);
            seedRule("RULE-B-PARTNER-A", BT, AK_PURCHASE, null, PARTNER_GROUP_A, null, null, null, null, null,
                    "1405", 100);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setPartnerGroupId(PARTNER_GROUP_A);
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1405", result, "partnerGroupId=A 应命中 partial-wildcard 规则");
    }

    /**
     * (c) default fallback：无精确匹配时回落到 priority=0 全 NULL 维度 default 规则。
     */
    @Test
    public void testDefaultFallback() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-C-DEFAULT", BT, AK_PURCHASE, null, null, null, null, null, null, null, "1403", 0);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setMaterialCategoryId(MATERIAL_CATEGORY_42); // 无精确规则匹配
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1403", result, "应回落到 default 规则");
    }

    /**
     * (d) 空匹配返回 null（不抛异常）。
     */
    @Test
    public void testEmptyMatchReturnsNull() {
        ormTemplate.runInSession(() -> {
            // 不种子任何规则
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setMaterialCategoryId(MATERIAL_CATEGORY_42);
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertNull(result, "无规则时返回 null（保留 Provider fallback）");
    }

    /**
     * (e) 多账套 specific acctSchemaId > wildcard acctSchemaId=NULL：相同 priority 下具体度更高者胜。
     */
    @Test
    public void testSpecificAcctSchemaWinsOnEqualPriority() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-E-GENERIC", BT, AK_PURCHASE, null, null, null, null, null, null, null, "1403", 100);
            seedRule("RULE-E-SCHEMA7", BT, AK_PURCHASE, ACCT_SCHEMA_7, null, null, null, null, null, null,
                    "5001", 100);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        String resultSchema7 = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, ACCT_SCHEMA_7);
        assertEquals("5001", resultSchema7, "acctSchemaId=7 应命中具体度更高的 SCHEMA7 规则");

        String resultSchema8 = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, ACCT_SCHEMA_8);
        assertEquals("1403", resultSchema8, "acctSchemaId=8 无 specific 规则，回落到 GENERIC");
    }

    /**
     * (f) priority 打破并列维度：相同具体度下，priority 高者胜。
     */
    @Test
    public void testPriorityBreaksSpecificityTie() {
        ormTemplate.runInSession(() -> {
            // 两条规则都是 partnerGroupId 维度（相同具体度=1），但 priority 不同
            seedRule("RULE-F-LOW", BT, AK_PURCHASE, null, PARTNER_GROUP_A, null, null, null, null, null,
                    "1403", 100);
            seedRule("RULE-F-HIGH", BT, AK_PURCHASE, null, PARTNER_GROUP_A, null, null, null, null, null,
                    "9999", 200);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setPartnerGroupId(PARTNER_GROUP_A);
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("9999", result, "相同具体度下 priority=200 应胜出");
    }

    /**
     * (g) 维度扩展：materialId → materialCategoryId 经 ErpMdMaterial.categoryId lookup。
     */
    @Test
    public void testDimensionExpansionMaterialIdToCategoryId() {
        ormTemplate.runInSession(() -> {
            seedMaterial(MATERIAL_ID_5001, MATERIAL_CATEGORY_42);
            seedRule("RULE-G-DEFAULT", BT, AK_PURCHASE, null, null, null, null, null, null, null, "1403", 0);
            seedRule("RULE-G-MAT-CAT-42", BT, AK_PURCHASE, null, null, MATERIAL_CATEGORY_42, null, null, null,
                    null, "1404", 100);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setMaterialId(MATERIAL_ID_5001); // 仅传 materialId，期望 resolver 扩展为 categoryId=42
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1404", result, "materialId=5001 应扩展为 categoryId=42 后命中精确规则");
    }

    /**
     * (h) 缓存失效后重新 load：新增规则后未 invalidate → 旧结果；invalidate 后 → 新结果。
     */
    @Test
    public void testCacheInvalidationReload() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-H-DEFAULT", BT, AK_PURCHASE, null, null, null, null, null, null, null, "1403", 0);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        String before = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1403", before, "初始只有 default 规则");

        // 新增精确规则但未 invalidate → 仍是 default
        ormTemplate.runInSession(() -> {
            seedRule("RULE-H-EXACT", BT, AK_PURCHASE, null, PARTNER_GROUP_B, null, null, null, null, null,
                    "1406", 100);
        });
        dims.setPartnerGroupId(PARTNER_GROUP_B);
        String stale = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1403", stale, "未 invalidate 时缓存仍返回旧 default 结果");

        // invalidate → reload
        resolver.invalidateCache();
        String fresh = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1406", fresh, "invalidate 后应 reload 并命中新精确规则");
    }

    // ---------- helpers ----------

    private void seedRule(String code, String businessType, String accountKey, Long acctSchemaId,
                          Long partnerGroupId, Long materialCategoryId, Long warehouseId, Long departmentId,
                          Long projectId, Long orgId, String targetSubjectCode, int priority) {
        IEntityDao<ErpFinGlMappingRule> dao = daoProvider.daoFor(ErpFinGlMappingRule.class);
        ErpFinGlMappingRule rule = new ErpFinGlMappingRule();
        rule.setCode(code);
        rule.setName(code);
        rule.setOrgId(orgId == null ? 1L : orgId);
        rule.setBusinessType(businessType);
        rule.setAccountKey(accountKey);
        rule.setAcctSchemaId(acctSchemaId);
        rule.setPartnerGroupId(partnerGroupId);
        rule.setMaterialCategoryId(materialCategoryId);
        rule.setWarehouseId(warehouseId);
        rule.setDepartmentId(departmentId);
        rule.setProjectId(projectId);
        rule.setTargetSubjectCode(targetSubjectCode);
        rule.setPriority(priority);
        rule.setIsActive(Boolean.TRUE);
        dao.saveEntity(rule);
    }

    private void seedMaterial(Long id, Long categoryId) {
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        ErpMdMaterial material = new ErpMdMaterial();
        material.setId(id);
        material.setCode("MAT-" + id);
        material.setName("物料" + id);
        material.setMaterialType("FINISHED");
        material.setUoMId(1L);
        material.setStatus("ACTIVE");
        material.setCategoryId(categoryId);
        dao.saveEntity(material);
    }
}
