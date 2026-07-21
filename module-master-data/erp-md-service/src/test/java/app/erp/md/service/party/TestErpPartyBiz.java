package app.erp.md.service.party;

import app.erp.md.biz.IErpPartyBiz;
import app.erp.md.dao.dto.ErpPartyType;
import app.erp.md.dao.dto.PartyRef;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C1 {@code ErpPartyBizModel} 单元测试（plan 2026-07-21-0827-2 Phase 2）。
 *
 * <p>覆盖 7 场景：
 * <ol>
 *   <li>{@code testFindPartiesAcrossThreeEntities}：关键字跨 3 实体检索。</li>
 *   <li>{@code testFindPartiesByPartyTypeFilter}：partyType 过滤。</li>
 *   <li>{@code testFindPartiesWithShortKeywordReturnsEmpty}：keyword &lt; 2 字符返回空。</li>
 *   <li>{@code testFindPartiesLimitTruncation}：limit 截断。</li>
 *   <li>{@code testGetPartyAllThreeTypes}：getParty 三类型 + 投影正确性（含 Organization phone/email=null）。</li>
 *   <li>{@code testPartyRefProjection}：PartyRef 字段投影正确性 + extension Map。</li>
 *   <li>{@code testFindReferencesPartnerPathAndMissingSpisReturnEmpty}：Partner 路径经既有 partnerCheckers 收集
 *       + Employee/Organization SPI 端口存在但下游实现未注册时返回空 Map 不抛异常。</li>
 * </ol>
 *
 * <p>Partner 路径经 {@code TestStubPartnerReferenceChecker} 桩注入（{@code test-partner-reference-checker.beans.xml}）；
 * Employee/Organization SPI 端口存在但下游实现未注册（master-data 不反向依赖下游域）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/md/beans/test-partner-reference-checker.beans.xml")
public class TestErpPartyBiz extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPartyBiz partyBiz;
    @Inject
    app.erp.md.service.TestStubPartnerReferenceChecker refChecker;

    @Test
    public void testFindPartiesAcrossThreeEntities() {
        ormTemplate.runInSession(() -> {
            seedPartner(101L, "P-KW-ACROSS", "关键字ACROSS", "ACTIVE");
            seedEmployee(201L, "E-KW-ACROSS", "职员-ACROSS", "ACTIVE");
            seedOrganization(301L, "O-KW-ACROSS", "组织-ACROSS", "ACTIVE");
        });

        List<PartyRef> refs = partyBiz.findParties("ACROSS", null, 50, CTX);
        assertEquals(3, refs.size(), "跨 3 实体应命中 3 条");
        // 验证 3 类型都出现
        EnumSet<ErpPartyType> hitTypes = EnumSet.noneOf(ErpPartyType.class);
        for (PartyRef ref : refs) {
            hitTypes.add(ref.getPartyType());
        }
        assertTrue(hitTypes.contains(ErpPartyType.PARTNER), "应含 PARTNER");
        assertTrue(hitTypes.contains(ErpPartyType.EMPLOYEE), "应含 EMPLOYEE");
        assertTrue(hitTypes.contains(ErpPartyType.ORGANIZATION), "应含 ORGANIZATION");
    }

    @Test
    public void testFindPartiesByPartyTypeFilter() {
        ormTemplate.runInSession(() -> {
            seedPartner(102L, "P-KW-FILTER", "FILTER-PARTNER", "ACTIVE");
            seedEmployee(202L, "E-KW-FILTER", "FILTER-EMPLOYEE", "ACTIVE");
            seedOrganization(302L, "O-KW-FILTER", "FILTER-ORG", "ACTIVE");
        });

        // 只查 EMPLOYEE
        List<PartyRef> refs = partyBiz.findParties("FILTER",
                EnumSet.of(ErpPartyType.EMPLOYEE), 50, CTX);
        assertEquals(1, refs.size(), "partyType 过滤应仅返回 1 条");
        assertEquals(ErpPartyType.EMPLOYEE, refs.get(0).getPartyType());

        // 查 PARTNER + ORGANIZATION（不应含 EMPLOYEE）
        List<PartyRef> refs2 = partyBiz.findParties("FILTER",
                EnumSet.of(ErpPartyType.PARTNER, ErpPartyType.ORGANIZATION), 50, CTX);
        assertEquals(2, refs2.size(), "PARTNER+ORGANIZATION 过滤应返回 2 条");
        for (PartyRef r : refs2) {
            assertFalse(r.getPartyType() == ErpPartyType.EMPLOYEE, "不应含 EMPLOYEE");
        }
    }

    @Test
    public void testFindPartiesWithShortKeywordReturnsEmpty() {
        ormTemplate.runInSession(() -> {
            seedPartner(103L, "P-KW-AB", "关键字AB", "ACTIVE");
        });

        // keyword = 1 字符 → 空
        List<PartyRef> refs1 = partyBiz.findParties("A", null, 50, CTX);
        assertNotNull(refs1);
        assertTrue(refs1.isEmpty(), "1 字符 keyword 应返回空");

        // keyword = null → 空
        List<PartyRef> refsNull = partyBiz.findParties(null, null, 50, CTX);
        assertNotNull(refsNull);
        assertTrue(refsNull.isEmpty(), "null keyword 应返回空");

        // keyword = 空白 → 空
        List<PartyRef> refsBlank = partyBiz.findParties("   ", null, 50, CTX);
        assertNotNull(refsBlank);
        assertTrue(refsBlank.isEmpty(), "空白 keyword 应返回空");
    }

    @Test
    public void testFindPartiesLimitTruncation() {
        ormTemplate.runInSession(() -> {
            // Partner 命中 5 条 + Employee 命中 5 条 = 10 条总命中
            for (int i = 1; i <= 5; i++) {
                seedPartner(1000L + i, "P-KW-LIMIT-" + i, "LIMIT-PARTNER-" + i, "ACTIVE");
            }
            for (int i = 1; i <= 5; i++) {
                seedEmployee(2000L + i, "E-KW-LIMIT-" + i, "LIMIT-EMPLOYEE-" + i, "ACTIVE");
            }
        });

        // limit = 3 → 至多 3 条
        List<PartyRef> refs = partyBiz.findParties("LIMIT", null, 3, CTX);
        assertTrue(refs.size() <= 3, "limit=3 截断后应至多 3 条，实际：" + refs.size());

        // limit = null → 走 DEFAULT_LIMIT (50)，不应超 50
        List<PartyRef> refsDefault = partyBiz.findParties("LIMIT", null, null, CTX);
        assertTrue(refsDefault.size() <= 50, "默认 limit=50 应不超 50，实际：" + refsDefault.size());

        // limit = -1（无效）→ 走 DEFAULT_LIMIT
        List<PartyRef> refsInvalid = partyBiz.findParties("LIMIT", null, -1, CTX);
        assertTrue(refsInvalid.size() <= 50, "无效 limit 应走默认 50");
    }

    @Test
    public void testGetPartyAllThreeTypes() {
        ormTemplate.runInSession(() -> {
            seedPartner(1101L, "P-GET-1", "GET-PARTNER", "ACTIVE");
            seedEmployee(1201L, "E-GET-1", "GET-EMPLOYEE", "ACTIVE");
            seedOrganization(1301L, "O-GET-1", "GET-ORG", "ACTIVE");
        });

        PartyRef partner = partyBiz.getParty(ErpPartyType.PARTNER, 1101L, CTX);
        assertNotNull(partner, "PARTNER getParty 不应返回 null");
        assertEquals(ErpPartyType.PARTNER, partner.getPartyType());
        assertEquals("P-GET-1", partner.getCode());
        assertEquals("GET-PARTNER", partner.getName());

        PartyRef employee = partyBiz.getParty(ErpPartyType.EMPLOYEE, 1201L, CTX);
        assertNotNull(employee, "EMPLOYEE getParty 不应返回 null");
        assertEquals(ErpPartyType.EMPLOYEE, employee.getPartyType());
        assertEquals("E-GET-1", employee.getCode());

        PartyRef org = partyBiz.getParty(ErpPartyType.ORGANIZATION, 1301L, CTX);
        assertNotNull(org, "ORGANIZATION getParty 不应返回 null");
        assertEquals(ErpPartyType.ORGANIZATION, org.getPartyType());
        assertEquals("O-GET-1", org.getCode());

        // 不存在的 ID → null
        PartyRef missing = partyBiz.getParty(ErpPartyType.PARTNER, 99999999L, CTX);
        assertNull(missing, "不存在的 ID 应返回 null");
    }

    @Test
    public void testPartyRefProjection() {
        ormTemplate.runInSession(() -> {
            seedPartnerWithContact(2101L, "P-PROJ", "PROJ-PARTNER",
                    "CUSTOMER", "ACTIVE", "13800000001", "partner@test.com");
            seedEmployeeWithDetails(2201L, "E-PROJ", "PROJ-EMPLOYEE",
                    "ACTIVE", "13800000002", "emp@test.com", "MANAGER", 9999L, 8888L);
            seedOrganization(2301L, "O-PROJ", "PROJ-ORG", "ACTIVE");
        });

        // Partner 投影：含 phone/email + extension.partnerType
        PartyRef partner = partyBiz.getParty(ErpPartyType.PARTNER, 2101L, CTX);
        assertEquals("13800000001", partner.getPhone());
        assertEquals("partner@test.com", partner.getEmail());
        assertEquals("CUSTOMER", partner.getExtension().get("partnerType"));
        assertEquals("P-PROJ - PROJ-PARTNER", partner.getDisplayName());

        // Employee 投影：含 phone/email + extension.position/orgId/partnerId
        PartyRef employee = partyBiz.getParty(ErpPartyType.EMPLOYEE, 2201L, CTX);
        assertEquals("13800000002", employee.getPhone());
        assertEquals("emp@test.com", employee.getEmail());
        assertEquals("MANAGER", employee.getExtension().get("position"));
        assertEquals(9999L, employee.getExtension().get("orgId"));
        assertEquals(8888L, employee.getExtension().get("partnerId"));

        // Organization 投影：phone/email=null 容忍 + extension.orgType
        PartyRef org = partyBiz.getParty(ErpPartyType.ORGANIZATION, 2301L, CTX);
        assertNull(org.getPhone(), "Organization 无 phone 列，投影为 null");
        assertNull(org.getEmail(), "Organization 无 email 列，投影为 null");
        assertNotNull(org.getExtension(), "Organization extension Map 应非 null");
    }

    @Test
    public void testFindReferencesPartnerPathAndMissingSpisReturnEmpty() {
        Long partnerId;
        Long employeeId;
        Long orgId;
        // seed 取得 ID（不固定 ID 便于换库测试）
        ormTemplate.runInSession(() -> {
            seedPartner(3101L, "P-REF", "REF-PARTNER", "ACTIVE");
            seedEmployee(3201L, "E-REF", "REF-EMPLOYEE", "ACTIVE");
            seedOrganization(3301L, "O-REF", "REF-ORG", "ACTIVE");
        });
        partnerId = 3101L;
        employeeId = 3201L;
        orgId = 3301L;

        // 1. Partner 路径：桩注入 partnerReferenceChecker → 经既有 partnerCheckers 收集
        refChecker.clear();
        refChecker.markReferenced(partnerId, "purchaseOrder", 5L);
        refChecker.markReferenced(partnerId, "salesOrder", 3L);
        Map<String, Long> partnerRefs = partyBiz.findReferences(ErpPartyType.PARTNER, partnerId, CTX);
        assertNotNull(partnerRefs, "Partner 路径应返回非 null Map");
        assertEquals(5L, partnerRefs.get("purchaseOrder"));
        assertEquals(3L, partnerRefs.get("salesOrder"));

        // 2. Partner 路径：无引用 → 空 Map
        refChecker.clear();
        Map<String, Long> emptyPartner = partyBiz.findReferences(ErpPartyType.PARTNER, partnerId, CTX);
        assertNotNull(emptyPartner, "无引用应返回非 null 空 Map");
        assertTrue(emptyPartner.isEmpty(), "无引用应返回空 Map");

        // 3. Employee 路径：SPI 端口存在但下游实现未注册 → 空 Map 不抛异常
        Map<String, Long> empRefs = partyBiz.findReferences(ErpPartyType.EMPLOYEE, employeeId, CTX);
        assertNotNull(empRefs, "Employee SPI 未注册时应返回非 null 空 Map");
        assertTrue(empRefs.isEmpty(), "Employee SPI 未注册时应返回空 Map，不抛异常");

        // 4. Organization 路径：同上 → 空 Map 不抛异常
        Map<String, Long> orgRefs = partyBiz.findReferences(ErpPartyType.ORGANIZATION, orgId, CTX);
        assertNotNull(orgRefs, "Organization SPI 未注册时应返回非 null 空 Map");
        assertTrue(orgRefs.isEmpty(), "Organization SPI 未注册时应返回空 Map，不抛异常");

        // 5. partyType=null 或 partyId=null → 空 Map
        Map<String, Long> nullTypeRefs = partyBiz.findReferences(null, partnerId, CTX);
        assertNotNull(nullTypeRefs);
        assertTrue(nullTypeRefs.isEmpty());
        Map<String, Long> nullIdRefs = partyBiz.findReferences(ErpPartyType.PARTNER, null, CTX);
        assertNotNull(nullIdRefs);
        assertTrue(nullIdRefs.isEmpty());
    }

    @Test
    public void testFindPartiesEmptyDatasetReturnsEmpty() {
        // 无种子数据时关键字查询 → 空 List
        List<PartyRef> refs = partyBiz.findParties("NO-SUCH-KEYWORD", null, 50, CTX);
        assertNotNull(refs);
        assertTrue(refs.isEmpty(), "空数据集应返回空 List");
    }

    // ---------- seed helpers ----------

    private void seedPartner(long id, String code, String name, String status) {
        seedPartnerWithContact(id, code, name, "CUSTOMER", status, null, null);
    }

    private void seedPartnerWithContact(long id, String code, String name, String partnerType,
                                        String status, String phone, String email) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName(name);
        p.setPartnerType(partnerType);
        p.setStatus(status);
        p.setPhone(phone);
        p.setEmail(email);
        p.setReceivableBalance(java.math.BigDecimal.ZERO);
        p.setPayableBalance(java.math.BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedEmployee(long id, String code, String name, String status) {
        seedEmployeeWithDetails(id, code, name, status, null, null, null, null, null);
    }

    @SuppressWarnings("SameParameterValue")
    private void seedEmployeeWithDetails(long id, String code, String name, String status,
                                         String phone, String email, String position,
                                         Long orgId, Long partnerId) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode(code);
        e.setName(name);
        e.setStatus(status);
        e.setPhone(phone);
        e.setEmail(email);
        e.setPosition(position);
        e.setOrgId(orgId);
        e.setPartnerId(partnerId);
        dao.saveEntity(e);
    }

    private void seedOrganization(long id, String code, String name, String status) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode(code);
        o.setName(name);
        o.setStatus(status);
        o.setOrgType("COMPANY");
        dao.saveEntity(o);
    }
}
