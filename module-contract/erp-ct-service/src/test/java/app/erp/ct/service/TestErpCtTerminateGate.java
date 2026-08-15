package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.auth.core.login.UserContextImpl;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUserRole;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * terminate 两段化法务门控测试（RC-R1.34，P1-RC-076，UC-CT-06）。
 *
 * <p>覆盖（对齐 plan 2026-08-15-1023-1 Phase 5 测试矩阵 ①）：
 * 发起（terminate 生成 PENDING 法务记录 + 合同保持原状态）/ 法务通过（approveTermination →
 * TERMINATED + 版本 isCurrent=false 归档 + InvoicePlan 逻辑删除截停 + 善后通知）/
 * 法务驳回（合同保持原状态 + 通知）/ 重复发起拒绝 / 审批人守卫 / 未过法务不执行终止副作用。
 *
 * <p>法务角色：seed nop-auth 角色 + 用户绑定（D2 解析）；approverId 落解析用户，
 * approveTermination 须以该用户上下文执行。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtTerminateGate extends JunitAutoTestCase {

    static final String LEGAL_ROLE_NAME = "CT-LEGAL-ROLE";
    static final String LEGAL_USER = "ct-legal-user";
    static final String OTHER_USER = "ct-other-user";
    static final String OWNER_USER = "ct-owner-user";

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
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpCtConfigs.CFG_TERMINATE_APPROVER_ROLE, LEGAL_ROLE_NAME);
    }

    @AfterEach
    void restoreContextAndConfig() {
        IUserContext.set(prevCtx);
        ContextProvider.getOrCreateContext().setUserRefNo(null);
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_TERMINATE_APPROVER_ROLE,
                ErpCtConfigs.DEFAULT_TERMINATE_APPROVER_ROLE);
    }

    // ---------- ① 发起 ----------

    @Test
    public void testTerminateCreatesPendingRecordAndKeepsStatus() {
        seedLegalRole();
        long contractId = setupActiveContract();
        ErpCtContract before = contract(contractId);

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "供应商违约")));
        assertEquals(0, resp.getStatus(), "terminate 发起应成功: " + resp);

        ErpCtContract after = contract(contractId);
        assertEquals(before.getStatus(), after.getStatus(), "发起终止申请合同保持原状态");
        ErpCtApprovalRecord record = pendingTerminationRecord(contractId);
        assertNotNull(record, "应生成 PENDING 法务审批记录");
        assertNull(record.getApprovalMatrixId(), "终止记录 approvalMatrixId=null（D1 判别）");
        assertEquals(LEGAL_USER, record.getApproverId(), "D2 解析法务角色用户");
        assertEquals("供应商违约", record.getRemark());
    }

    @Test
    public void testTerminateDuplicatePendingRejected() {
        seedLegalRole();
        long contractId = setupActiveContract();
        executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "协商解约")));

        ApiResponse<?> dup = executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "再次发起")));
        assertNotEquals(0, dup.getStatus(), "重复发起应拒绝");
        assertTrue(String.valueOf(dup).contains("terminate-already-pending")
                        || String.valueOf(dup).contains("不可重复发起"),
                "应报 ERR_CT_TERMINATE_ALREADY_PENDING: " + dup);
    }

    @Test
    public void testTerminateKeepsInvoicePlanTriggerableUntilApproved() {
        // 未过法务：合同保持 ACTIVE，InvoicePlan 仍可触发（终止副作用未执行）
        seedLegalRole();
        long[] setup = setupActiveContractWithLine();
        long contractId = setup[0];
        long lineId = setup[1];
        long planId = saveInvoicePlan(lineId, new BigDecimal("1000"));

        executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "违约")));

        ApiResponse<?> trigger = executeRpc(mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertEquals(0, trigger.getStatus(), "法务未通过前合同仍 ACTIVE，计划可触发: " + trigger);
    }

    // ---------- ② 法务通过 → 副作用全落地 ----------

    @Test
    public void testApproveTerminationExecutesAllSideEffects() {
        seedLegalRole();
        loginAs(OWNER_USER);
        long[] setup = setupActiveContractWithLine();
        long contractId = setup[0];
        long lineId = setup[1];
        long planId = saveInvoicePlan(lineId, new BigDecimal("1000"));
        // 版本归档断言载体：v1 为当前版本（activate 级联 SIGNED），v2 非 current 历史版本
        createVersion(contractId, 2, false, "SIGNED");
        seedNotifyTemplate(8811L, ErpCtConstants.NOTIFY_EVENT_TERMINATE_WINDDOWN);
        executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "供应商违约")));
        long recordId = pendingTerminationRecord(contractId).getId();
        assertEquals(OWNER_USER, contract(contractId).getCreatedBy(), "合同应以经办人身份创建");

        loginAs(LEGAL_USER);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__approveTermination",
                ApiRequest.build(Map.of("recordId", recordId, "comment", "法务审核通过")));
        assertEquals(0, resp.getStatus(), "approveTermination 应成功: " + resp);

        // 合同 → TERMINATED
        assertEquals(ErpCtConstants.CONTRACT_STATUS_TERMINATED, contract(contractId).getStatus());
        // 记录 → APPROVED
        ErpCtApprovalRecord record = daoProvider.daoFor(ErpCtApprovalRecord.class).getEntityById(recordId);
        assertEquals(ErpCtConstants.APPROVAL_STATUS_APPROVED, record.getApprovalStatus());
        assertNotNull(record.getApprovedAt());
        // 版本归档：当前版本 isCurrent=false
        List<ErpCtContractVersion> versions = findVersions(contractId);
        for (ErpCtContractVersion v : versions) {
            assertTrue(!Boolean.TRUE.equals(v.getIsCurrent()), "终止后无 current 版本（v2 归档 isCurrent=false）");
        }
        // InvoicePlan 截停：逻辑删除（读路径零命中 + delVersion 置删除标记[非 0]）
        ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
        assertNotNull(plan.getDelVersion(), "未执行 InvoicePlan 应逻辑删除（delVersion 置删除标记）");
        assertTrue(plan.getDelVersion() != 0, "delVersion 应非 0（逻辑删除标记）");
        assertTrue(findUnexecutedPlans(lineId).isEmpty(), "读路径应零未执行计划");
        // 善后 TODO 通知落库
        List<ErpSysNotification> notifications = notificationsOf(OWNER_USER,
                ErpCtConstants.NOTIFY_EVENT_TERMINATE_WINDDOWN);
        assertEquals(1, notifications.size(), "善后 TODO 通知应落库 1 条");
    }

    @Test
    public void testApproveTerminationGuardApproverMismatch() {
        seedLegalRole();
        long contractId = setupActiveContract();
        executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "违约")));
        long recordId = pendingTerminationRecord(contractId).getId();

        loginAs(OTHER_USER);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__approveTermination",
                ApiRequest.build(Map.of("recordId", recordId)));
        assertNotEquals(0, resp.getStatus(), "非法务用户 approveTermination 应拒绝");
        assertTrue(String.valueOf(resp).contains("approver-mismatch")
                        || String.valueOf(resp).contains("无权操作"),
                "应报 ERR_CT_APPROVAL_APPROVER_MISMATCH: " + resp);
        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, contract(contractId).getStatus(),
                "拒绝后合同保持原状态");
    }

    // ---------- ③ 法务驳回 → 保持原状态 ----------

    @Test
    public void testRejectTerminationKeepsStatusAndNotifies() {
        seedLegalRole();
        loginAs(OWNER_USER);
        long contractId = setupActiveContract();
        seedNotifyTemplate(8812L, ErpCtConstants.NOTIFY_EVENT_TERMINATE_REJECTED);
        executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "违约")));
        long recordId = pendingTerminationRecord(contractId).getId();
        String statusBefore = contract(contractId).getStatus();

        loginAs(LEGAL_USER);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__rejectTermination",
                ApiRequest.build(Map.of("recordId", recordId, "comment", "终止理由不充分")));
        assertEquals(0, resp.getStatus(), "rejectTermination 应成功: " + resp);

        assertEquals(statusBefore, contract(contractId).getStatus(), "驳回后合同保持原状态");
        ErpCtApprovalRecord record = daoProvider.daoFor(ErpCtApprovalRecord.class).getEntityById(recordId);
        assertEquals(ErpCtConstants.APPROVAL_STATUS_REJECTED, record.getApprovalStatus());
        assertNotNull(record.getRejectedAt());
        List<ErpSysNotification> notifications = notificationsOf(OWNER_USER,
                ErpCtConstants.NOTIFY_EVENT_TERMINATE_REJECTED);
        assertEquals(1, notifications.size(), "驳回应通知经办人 1 条");

        // 驳回后可重新发起（无 PENDING 残留）
        ApiResponse<?> again = executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId, "reason", "补充材料后重新申请")));
        assertEquals(0, again.getStatus(), "驳回后重新发起应成功: " + again);
    }

    // ---------- helpers ----------

    private void seedLegalRole() {
        ormTemplate.runInSession(() -> {
            NopAuthRole role = new NopAuthRole();
            role.setRoleId("ct-legal-role-id");
            role.setRoleName(LEGAL_ROLE_NAME);
            daoProvider.daoFor(NopAuthRole.class).saveEntity(role);
            NopAuthUserRole ur = new NopAuthUserRole();
            ur.setRoleId("ct-legal-role-id");
            ur.setUserId(LEGAL_USER);
            daoProvider.daoFor(NopAuthUserRole.class).saveEntity(ur);
        });
    }

    private void loginAs(String userId) {
        UserContextImpl uc = new UserContextImpl();
        uc.setUserId(userId);
        uc.setUserName(userId);
        IUserContext.set(uc);
        ContextProvider.getOrCreateContext().setUserRefNo(userId);
    }

    private ErpCtContract contract(long contractId) {
        return daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
    }

    private ErpCtApprovalRecord pendingTerminationRecord(long contractId) {
        List<ErpCtApprovalRecord> records = findRecords(contractId);
        for (ErpCtApprovalRecord r : records) {
            if (r.getApprovalMatrixId() == null
                    && ErpCtConstants.APPROVAL_STATUS_PENDING.equals(r.getApprovalStatus())) {
                return r;
            }
        }
        return null;
    }

    private List<ErpCtApprovalRecord> findRecords(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        return daoProvider.daoFor(ErpCtApprovalRecord.class).findAllByQuery(q);
    }

    private List<ErpCtContractVersion> findVersions(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        return daoProvider.daoFor(ErpCtContractVersion.class).findAllByQuery(q);
    }

    private List<ErpCtInvoicePlan> findUnexecutedPlans(long contractLineId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractLineId", contractLineId));
        q.addFilter(eq("isInvoiced", false));
        return daoProvider.daoFor(ErpCtInvoicePlan.class).findAllByQuery(q);
    }

    private long setupActiveContract() {
        long[] setup = setupActiveContractWithLine();
        return setup[0];
    }

    private long[] setupActiveContractWithLine() {
        long[] ids = new long[2];
        ormTemplate.runInSession(session -> {
            ids[0] = createPartner();
            ids[1] = createCurrency();
            return null;
        });
        long partnerId = ids[0];
        long currencyId = ids[1];
        long contractId = createContract(partnerId, currencyId, "ACTIVE");
        long lineId = saveLine(contractId);
        createVersion(contractId, 1, true, "FINALIZED");
        executeRpc(mutation, "ErpCtContract__activate",
                ApiRequest.build(Map.of("contractId", contractId)));
        return new long[]{contractId, lineId};
    }

    private long createPartner() {
        ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        p.setCode("CT-TG-PARTNER-" + System.nanoTime());
        p.setName("终止门控测试伙伴");
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long createCurrency() {
        ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
        c.setCode("CNY-TG");
        c.setName("人民币");
        daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
        return c.getId();
    }

    private long createContract(long partnerId, long currencyId, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-TG-" + System.nanoTime());
        data.put("contractName", "终止门控测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("totalAmount", new BigDecimal("1000"));
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private long saveLine(long contractId) {
        long[] holder = new long[1];
        ormTemplate.runInSession(session -> {
            ErpMdUoM uom = daoProvider.daoFor(ErpMdUoM.class).newEntity();
            uom.setCode("PCS-TG");
            uom.setName("个");
            daoProvider.daoFor(ErpMdUoM.class).saveEntity(uom);
            ErpMdMaterial material = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
            material.setCode("MAT-TG-" + System.nanoTime());
            material.setName("终止测试物料");
            material.setMaterialType("GOODS");
            material.setUoMId(uom.getId());
            material.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(material);
            holder[0] = material.getId();
            return null;
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lineNo", 1);
        data.put("contractId", contractId);
        data.put("materialId", holder[0]);
        data.put("quantity", new BigDecimal("100"));
        data.put("unitPrice", new BigDecimal("10"));
        data.put("amount", new BigDecimal("1000"));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractLine__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractLine__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private long saveInvoicePlan(long contractLineId, BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractLineId", contractLineId);
        data.put("planDate", "2026-06-01");
        data.put("amount", amount);
        data.put("invoiceTerm", "MILESTONE");
        data.put("isInvoiced", false);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtInvoicePlan__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private void createVersion(long contractId, int versionNo, boolean isCurrent, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contractId);
        data.put("versionNo", versionNo);
        data.put("versionDate", "2026-01-01");
        data.put("isCurrent", isCurrent);
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractVersion__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractVersion__save 应成功: " + resp);
    }

    private void seedNotifyTemplate(Long id, String eventType) {
        ormTemplate.runInSession(() -> {
            ErpSysNotificationTemplate t = daoProvider.daoFor(ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(eventType);
            t.setName("TPL-" + eventType);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("合同终止 ${contractCode}");
            t.setBodyTpl("合同 ${contractCode} 终止流程 ${terminationReason}");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"${submitterUserId}\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String userId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
