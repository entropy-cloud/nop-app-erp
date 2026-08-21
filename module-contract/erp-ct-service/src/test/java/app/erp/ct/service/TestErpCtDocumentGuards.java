package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtDocument;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同文档 Legal Hold / 归档只读 / ACTIVE 不归档守卫测试（RC-R1.80 Phase 2，P1-RC-079，UC-CT-10 D）。
 *
 * <p>覆盖：① legalHold=true 阻止归档 + 删除（错误码断言 + 零状态变更）；② setLegalHold 角色守卫
 * 双侧（无角色 fail-closed 拒绝 / admin 通过）+ generic update 携带 legalHold 防绕过；③ 归档只读
 * （归档后 generic update/delete 拒绝，admin 合规字段 legalHold 调整例外放行）；④ ACTIVE 合同
 * 阻止归档 + 非 ACTIVE 放行 + 二次归档幂等。
 *
 * <p>角色守卫经 {@link IUserContext#isUserInRole(String)}（roleId 判定，Set 注入无需 DB 角色
 * seed，对齐 TestErpHrTimesheetFamily 范式）；时间冻结 {@link CtFrozenClockExtension#REFERENCE_DATE}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtDocumentGuards extends JunitAutoTestCase {

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private IUserContext prevCtx;

    @BeforeEach
    void saveContext() {
        prevCtx = IUserContext.get();
        loginAsAdmin();
    }

    @AfterEach
    void restoreContext() {
        IUserContext.set(prevCtx);
    }

    // ---------- ① legalHold=true 阻止归档 + 删除 ----------

    @Test
    public void testLegalHoldBlocksArchiveAndDelete() {
        String docId = seedDocument(null, false, null);
        ApiResponse<?> set = executeRpc(mutation, "ErpCtDocument__setLegalHold",
                ApiRequest.build(Map.of("documentId", docId, "legalHold", true)));
        assertEquals(0, set.getStatus(), "admin 设置 legalHold 应成功: " + set);
        assertTrue(Boolean.TRUE.equals(document(docId).getLegalHold()), "legalHold 应落库为 true");

        ApiResponse<?> archive = executeRpc(mutation, "ErpCtDocument__archive",
                ApiRequest.build(Map.of("documentId", docId)));
        assertNotEquals(0, archive.getStatus(), "legalHold 文档归档应被拒");
        assertTrue(String.valueOf(archive).contains("document-legal-hold")
                        || String.valueOf(archive).contains("法律保留"),
                "应报 ERR_CT_DOCUMENT_LEGAL_HOLD: " + archive);

        ErpCtDocument doc = document(docId);
        assertFalse(Boolean.TRUE.equals(doc.getIsArchived()), "零状态变更：不应归档");
        assertNull(doc.getArchiveDate(), "零状态变更：archiveDate 应为空");

        ApiResponse<?> delete = executeRpc(mutation, "ErpCtDocument__delete",
                ApiRequest.build(Map.of("id", String.valueOf(docId))));
        assertNotEquals(0, delete.getStatus(), "legalHold 文档删除应被拒（法律保留阻止所有销毁操作）");
        assertTrue(String.valueOf(delete).contains("document-legal-hold")
                        || String.valueOf(delete).contains("法律保留"),
                "删除应报 ERR_CT_DOCUMENT_LEGAL_HOLD: " + delete);
        assertTrue(document(docId) != null, "文档不应被删除（仍可加载）");
    }

    // ---------- ② setLegalHold 角色守卫双侧 + generic update 防绕过 ----------

    @Test
    public void testSetLegalHoldRoleGuardBothSides() {
        String docId = seedDocument(null, false, null);

        loginAsPlainUser();
        ApiResponse<?> denied = executeRpc(mutation, "ErpCtDocument__setLegalHold",
                ApiRequest.build(Map.of("documentId", docId, "legalHold", true)));
        assertNotEquals(0, denied.getStatus(), "无角色用户设置 legalHold 应 fail-closed 拒绝");
        assertTrue(String.valueOf(denied).contains("document-role-required")
                        || String.valueOf(denied).contains("角色"),
                "应报 ERR_CT_DOCUMENT_ROLE_REQUIRED: " + denied);
        assertFalse(Boolean.TRUE.equals(document(docId).getLegalHold()), "拒绝路径零状态变更");

        // generic update 携带 legalHold 同样要求 admin（防绕过专用入口）
        ApiResponse<?> bypass = executeRpc(mutation, "ErpCtDocument__update",
                ApiRequest.build(Map.of("data", Map.of("id", docId, "legalHold", true))));
        assertNotEquals(0, bypass.getStatus(), "非 admin generic update 携带 legalHold 应被拒");
        assertTrue(String.valueOf(bypass).contains("document-role-required"),
                "防绕过守卫应报 ERR_CT_DOCUMENT_ROLE_REQUIRED: " + bypass);

        loginAsAdmin();
        ApiResponse<?> allowed = executeRpc(mutation, "ErpCtDocument__setLegalHold",
                ApiRequest.build(Map.of("documentId", docId, "legalHold", true)));
        assertEquals(0, allowed.getStatus(), "admin 设置 legalHold 应成功: " + allowed);
        assertTrue(Boolean.TRUE.equals(document(docId).getLegalHold()));
    }

    // ---------- ③ 归档只读 ----------

    @Test
    public void testArchivedDocumentReadOnlyExceptLegalHold() {
        String docId = seedDocument(null, true, LocalDate.of(2026, 7, 1));

        ApiResponse<?> update = executeRpc(mutation, "ErpCtDocument__update",
                ApiRequest.build(Map.of("data", Map.of("id", docId, "docName", "篡改归档文档"))));
        assertNotEquals(0, update.getStatus(), "归档文档 generic update 应被拒（归档只读）");
        assertTrue(String.valueOf(update).contains("document-archived-immutable")
                        || String.valueOf(update).contains("归档"),
                "应报 ERR_CT_DOCUMENT_ARCHIVED_IMMUTABLE: " + update);

        ApiResponse<?> delete = executeRpc(mutation, "ErpCtDocument__delete",
                ApiRequest.build(Map.of("id", String.valueOf(docId))));
        assertNotEquals(0, delete.getStatus(), "归档文档删除应被拒（不可删除）");
        assertTrue(String.valueOf(delete).contains("document-archived-immutable"),
                "删除应报 ERR_CT_DOCUMENT_ARCHIVED_IMMUTABLE: " + delete);

        // admin 合规字段例外：归档文档仍可加法律保留（阻止后续销毁）
        ApiResponse<?> hold = executeRpc(mutation, "ErpCtDocument__setLegalHold",
                ApiRequest.build(Map.of("documentId", docId, "legalHold", true)));
        assertEquals(0, hold.getStatus(), "归档文档 admin 调整 legalHold 应放行（合规例外）: " + hold);
        assertTrue(Boolean.TRUE.equals(document(docId).getLegalHold()));
        assertEquals("CT-DOC-GUARD-ARCHIVED", document(docId).getDocName(), "业务字段零变更");
    }

    // ---------- ④ ACTIVE 合同阻止归档 + 非 ACTIVE 放行 + 幂等 ----------

    @Test
    public void testActiveContractBlocksArchive() {
        String contractId = seedContract("CT-DOC-GUARD-ACTIVE", ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        String docId = seedDocument(contractId, false, null);

        ApiResponse<?> blocked = executeRpc(mutation, "ErpCtDocument__archive",
                ApiRequest.build(Map.of("documentId", docId)));
        assertNotEquals(0, blocked.getStatus(), "ACTIVE 合同文档归档应被拒");
        assertTrue(String.valueOf(blocked).contains("document-contract-active")
                        || String.valueOf(blocked).contains("ACTIVE"),
                "应报 ERR_CT_DOCUMENT_CONTRACT_ACTIVE: " + blocked);
        assertFalse(Boolean.TRUE.equals(document(docId).getIsArchived()), "零状态变更");

        // 合同离开 ACTIVE（EXPIRED）后放行
        ormTemplate.runInSession(session -> {
            ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
            contract.setStatus(ErpCtConstants.CONTRACT_STATUS_EXPIRED);
            daoProvider.daoFor(ErpCtContract.class).updateEntity(contract);
            return null;
        });

        ApiResponse<?> ok = executeRpc(mutation, "ErpCtDocument__archive",
                ApiRequest.build(Map.of("documentId", docId)));
        assertEquals(0, ok.getStatus(), "非 ACTIVE 合同文档归档应成功: " + ok);
        ErpCtDocument archived = document(docId);
        assertTrue(Boolean.TRUE.equals(archived.getIsArchived()), "应置 isArchived=true");
        assertEquals(LocalDate.of(2026, 7, 17), archived.getArchiveDate(), "archiveDate = 冻结当日");

        // 二次归档幂等（archiveDate 不变）
        ApiResponse<?> again = executeRpc(mutation, "ErpCtDocument__archive",
                ApiRequest.build(Map.of("documentId", docId)));
        assertEquals(0, again.getStatus(), "重复归档应幂等成功: " + again);
        assertEquals(LocalDate.of(2026, 7, 17), document(docId).getArchiveDate(), "幂等：archiveDate 不变");
    }

    // ---------- helpers ----------

    private void loginAsAdmin() {
        loginAs("ct-doc-admin", ErpCtConstants.LEGAL_HOLD_ROLE_ID);
    }

    private void loginAsPlainUser() {
        loginAs("ct-doc-user", "合同专员");
    }

    private void loginAs(String userId, String roleId) {
        UserContextImpl uc = new UserContextImpl();
        uc.setUserId(userId);
        uc.setUserName(userId);
        uc.setRoles(java.util.Set.of(roleId));
        IUserContext.set(uc);
    }

    private ErpCtDocument document(String docId) {
        return ormTemplate.runInSession(session -> daoProvider.daoFor(ErpCtDocument.class).getEntityById(docId));
    }

    /** seed 文档：docType=CONTRACT_SCAN，可选关联合同/已归档态。 */
    private String seedDocument(String contractId, boolean archived, LocalDate archiveDate) {
        String[] ret = new String[1];
        ormTemplate.runInSession(session -> {
            ErpCtDocument doc = daoProvider.daoFor(ErpCtDocument.class).newEntity();
            doc.setCode("CT-DOC-GUARD-" + System.nanoTime());
            doc.setDocName(archived ? "CT-DOC-GUARD-ARCHIVED" : "CT-DOC-GUARD");
            doc.setDocType("10");
            if (contractId != null) {
                doc.setContractId(contractId);
            }
            doc.setIsArchived(archived);
            doc.setArchiveDate(archiveDate);
            daoProvider.daoFor(ErpCtDocument.class).saveEntity(doc);
            ret[0] = doc.getId();
            return null;
        });
        return ret[0];
    }

    private String seedContract(String code, String status) {
        String[] ids = new String[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-DOC-PARTNER");
            p.setName("文档守卫测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-DG");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        String[] ret = new String[1];
        ormTemplate.runInSession(session -> {
            ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).newEntity();
            contract.orm_disableAutoStamp(true);
            contract.setCode(code);
            contract.setContractName("文档守卫测试合同 " + code);
            contract.setContractType("PURCHASE");
            contract.setContractDirection("INBOUND");
            contract.setPartnerId(ids[0]);
            contract.setCurrencyId(ids[1]);
            contract.setOrgId("1");
            contract.setStartDate(LocalDate.of(2026, 1, 1));
            contract.setEndDate(LocalDate.of(2026, 12, 31));
            contract.setTotalAmount(new BigDecimal("1000"));
            contract.setStatus(status);
            contract.setBusinessDate(LocalDate.of(2026, 7, 17));
            contract.setCreatedBy("ct-doc-admin");
            contract.setUpdatedBy("ct-doc-admin");
            contract.setCreateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            contract.setUpdateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            daoProvider.daoFor(ErpCtContract.class).saveEntity(contract);
            ret[0] = contract.getId();
            return null;
        });
        return ret[0];
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
