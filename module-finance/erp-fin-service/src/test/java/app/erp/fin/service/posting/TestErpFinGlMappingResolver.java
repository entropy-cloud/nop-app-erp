package app.erp.fin.service.posting;

import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import app.erp.fin.dao.dto.GlMappingDimensions;
import app.erp.fin.dao.entity.ErpFinGlMappingRule;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * GL 映射规则解析器单元测试（plan 2026-07-21-0827-1 A1 Phase 2 + 2026-07-25-1016-2 orgId 维度激活）。
 *
 * <p>覆盖场景：(a) exact match 优先 / (b) partial-wildcard / (c) default fallback / (d) 空匹配 null /
 * (e) acctSchemaId specific > wildcard / (f) priority 打破并列维度 / (g) 维度扩展（materialId → materialCategoryId）/
 * (h) 缓存失效后 reload / (i) 新增域专用键命中 / (j) 新增域专用键未命中 /
 * (k) 关闭态忽略 orgId（零回归）/ (l) 开启态 org 精确匹配 / (m) 开启态 org 不匹配返回 null /
 * (n) 开启态不同组织不同科目（cache 按 orgId 分桶）/ (o) 开启态 specificity 含 orgId 计数。
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
    static final Long ORG_1 = 1L;
    static final Long ORG_2 = 2L;

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

    /**
     * (i) 新增域专用键命中覆盖（plan 2026-07-24-1351-1）：MANUFACTURING_WIP 规则覆盖 subjectCode。
     */
    @Test
    public void testNewDomainKeyManufacturingWipHit() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-I-MFG-WIP", "MANUFACTURING_RECEIPT", "MANUFACTURING_WIP",
                    null, null, null, null, null, null, null, "1411", 0);
        });
        resolver.invalidateCache();

        String result = resolver.resolveSubjectCode("MANUFACTURING_RECEIPT", "MANUFACTURING_WIP",
                new GlMappingDimensions(), null);
        assertEquals("1411", result, "MANUFACTURING_WIP 命中 default 规则应覆盖 subjectCode");
    }

    /**
     * (j) 新增域专用键未命中返回 null（保留 Provider fallback）（plan 2026-07-24-1351-1）：
     * NOTES_RECEIVABLE 无规则 → null（向后兼容关键）。
     */
    @Test
    public void testNewDomainKeyNotesReceivableMissReturnsNull() {
        resolver.invalidateCache();
        String result = resolver.resolveSubjectCode("NOTES_RECEIVABLE_RECEIVED", "NOTES_RECEIVABLE",
                new GlMappingDimensions(), null);
        assertNull(result, "无规则时返回 null（保留 Provider fallback，向后兼容）");
    }

    // ---------- orgId 维度激活场景（plan 2026-07-25-1016-2） ----------

    /**
     * (k) 关闭态（默认）忽略 orgId：dims.orgId=2 仍命中 orgId=1 规则（orgId 维度不参与匹配，向后兼容）。
     */
    @Test
    public void testOrgDimensionDisabledIgnoresOrgId() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-K-ORG1", BT, AK_PURCHASE, null, null, null, null, null, null, ORG_1, "1403", 0);
        });
        resolver.invalidateCache();

        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setOrgId(ORG_2); // dims orgId 与 rule orgId 不同
        String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
        assertEquals("1403", result, "关闭态应忽略 orgId：dims.orgId=2 仍命中 orgId=1 规则（向后兼容）");
    }

    /**
     * (l) 开启态 org 精确匹配：orgId=2 命中 orgId=2 规则。
     */
    @Test
    public void testOrgDimensionEnabledExactMatch() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-L-ORG2", BT, AK_PURCHASE, null, null, null, null, null, null, ORG_2, "5001", 0);
        });
        withOrgDimensionEnabled(() -> {
            resolver.invalidateCache();
            GlMappingDimensions dims = new GlMappingDimensions();
            dims.setOrgId(ORG_2);
            String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
            assertEquals("5001", result, "开启态应按 orgId 精确匹配：orgId=2 命中 orgId=2 规则");
        });
    }

    /**
     * (m) 开启态 org 不匹配返回 null：orgId=2 不命中 orgId=1 规则（cache 按 orgId 分桶，orgId=2 桶为空）。
     */
    @Test
    public void testOrgDimensionEnabledMismatchReturnsNull() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-M-ORG1", BT, AK_PURCHASE, null, null, null, null, null, null, ORG_1, "1403", 0);
        });
        withOrgDimensionEnabled(() -> {
            resolver.invalidateCache();
            GlMappingDimensions dims = new GlMappingDimensions();
            dims.setOrgId(ORG_2);
            String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
            assertNull(result, "开启态 org 不匹配应返回 null：orgId=2 不命中 orgId=1 规则（cache 按 orgId 分桶）");
        });
    }

    /**
     * (n) 开启态不同组织不同科目 + cache 按 orgId 分桶：orgId=1 → "1403"，orgId=2 → "5001"。
     */
    @Test
    public void testOrgDimensionEnabledDifferentOrgsDifferentSubjects() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-N-ORG1", BT, AK_PURCHASE, null, null, null, null, null, null, ORG_1, "1403", 0);
            seedRule("RULE-N-ORG2", BT, AK_PURCHASE, null, null, null, null, null, null, ORG_2, "5001", 0);
        });
        withOrgDimensionEnabled(() -> {
            resolver.invalidateCache();
            GlMappingDimensions dims1 = new GlMappingDimensions();
            dims1.setOrgId(ORG_1);
            assertEquals("1403", resolver.resolveSubjectCode(BT, AK_PURCHASE, dims1, null),
                    "orgId=1 桶应命中 orgId=1 规则 → 1403");

            GlMappingDimensions dims2 = new GlMappingDimensions();
            dims2.setOrgId(ORG_2);
            assertEquals("5001", resolver.resolveSubjectCode(BT, AK_PURCHASE, dims2, null),
                    "orgId=2 桶应命中 orgId=2 规则 → 5001（cache 按 orgId 分桶生效）");
        });
    }

    /**
     * (o) 开启态 specificity 含 orgId 计数：同 org 下 orgId+materialCategoryId（具体度 2）
     * 在等 priority 下胜过仅 orgId（具体度 1）。
     */
    @Test
    public void testOrgDimensionEnabledSpecificityIncludesOrgId() {
        ormTemplate.runInSession(() -> {
            seedRule("RULE-O-DEFAULT", BT, AK_PURCHASE, null, null, null, null, null, null, ORG_1, "1403", 100);
            seedRule("RULE-O-MAT42", BT, AK_PURCHASE, null, null, MATERIAL_CATEGORY_42, null, null, null, ORG_1, "1404", 100);
        });
        withOrgDimensionEnabled(() -> {
            resolver.invalidateCache();
            GlMappingDimensions dims = new GlMappingDimensions();
            dims.setOrgId(ORG_1);
            dims.setMaterialCategoryId(MATERIAL_CATEGORY_42);
            String result = resolver.resolveSubjectCode(BT, AK_PURCHASE, dims, null);
            assertEquals("1404", result, "等 priority 下 orgId+materialCategoryId（具体度 2）应胜过仅 orgId（具体度 1）");
        });
    }

    /** 在 org-dimension-enabled=true 作用域内执行 action，结束后恢复默认 false 并 invalidate cache。 */
    private void withOrgDimensionEnabled(Runnable action) {
        Boolean original = AppConfig.var(ErpFinGlMappingResolver.CONFIG_ORG_DIMENSION_ENABLED, Boolean.FALSE);
        try {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinGlMappingResolver.CONFIG_ORG_DIMENSION_ENABLED, Boolean.TRUE);
            action.run();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinGlMappingResolver.CONFIG_ORG_DIMENSION_ENABLED, original);
            resolver.invalidateCache();
        }
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
