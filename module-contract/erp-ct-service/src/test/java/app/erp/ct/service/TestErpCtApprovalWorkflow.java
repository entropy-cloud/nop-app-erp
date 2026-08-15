package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtApprovalMatrix;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
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
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批工作流引擎测试（RC-R1.34，P1-RC-077，UC-CT-07）。
 *
 * <p>覆盖（对齐 plan 2026-08-15-1023-1 Phase 5 测试矩阵 ②③⑤）：
 * 金额匹配节点生成（首 PENDING 其余 WAITING）/ 逐节点 approve 推进 / 全通过 activate 联动 /
 * reject 保持 NEGOTIATION + 通知 / 审批人守卫 / config-gated 双路径 / D3 超限锁定 +
 * D7 驳回→重提→再驳回×3→锁定闭环（追加行生命周期断言）。
 *
 * <p>沿用 R1.32/R1.33 直断言范式（无快照）。config 经 AppConfig.assignConfigValue 操控，
 * @AfterEach 恢复。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtApprovalWorkflow extends JunitAutoTestCase {

    static final String APPROVER_USER = "ct-approver-user";
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
    }

    @AfterEach
    void restoreContextAndConfig() {
        IUserContext.set(prevCtx);
        ContextProvider.getOrCreateContext().setUserRefNo(null);
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "false");
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_MAX_RETRIES, "3");
    }

    // ---------- ⑤ config-gated 双路径 ----------

    @Test
    public void testSubmitNoRecordsWhenApprovalDisabled() {
        long contractId = createContract("DRAFT", "CT-AWF-1-", new BigDecimal("1000"));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__submit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, resp.getStatus(), "submit 应成功: " + resp);
        assertEquals(0, findRecords(contractId).size(), "approval-enabled=false 时零生成");
    }

    @Test
    public void testSubmitGeneratesRecordsFirstPendingRestWaiting() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-A", "role-a-1", APPROVER_USER);
        seedRole("CT-ROLE-B", "role-b-1", OTHER_USER);
        long matrixA = seedMatrix("CT-MTX-A-1", "CT-ROLE-A", 1, null, new BigDecimal("2000"));
        long matrixB = seedMatrix("CT-MTX-B-1", "CT-ROLE-B", 2, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-2-", new BigDecimal("1000"));

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__submit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, resp.getStatus(), "submit 应成功: " + resp);

        List<ErpCtApprovalRecord> records = findRecords(contractId);
        assertEquals(2, records.size(), "金额匹配应生成 2 条记录");
        ErpCtApprovalRecord first = records.stream().filter(r -> r.getApprovalOrder() == 1).findFirst().orElseThrow();
        ErpCtApprovalRecord second = records.stream().filter(r -> r.getApprovalOrder() == 2).findFirst().orElseThrow();
        assertEquals(ErpCtConstants.APPROVAL_STATUS_PENDING, first.getApprovalStatus(), "首节点 PENDING");
        assertEquals(ErpCtConstants.APPROVAL_STATUS_WAITING, second.getApprovalStatus(), "次节点 WAITING");
        assertEquals(matrixA, first.getApprovalMatrixId());
        assertEquals(matrixB, second.getApprovalMatrixId());
        assertEquals(APPROVER_USER, first.getApproverId(), "D2 角色解析应落 approverId");
        assertEquals(OTHER_USER, second.getApproverId());
    }

    @Test
    public void testSubmitNoNodesWhenMatrixNotMatching() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedMatrix("CT-MTX-HI-1", "CT-ROLE-HI", 1, new BigDecimal("5000"), new BigDecimal("10000"));
        long contractId = createContract("DRAFT", "CT-AWF-3-", new BigDecimal("1000"));

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__submit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, resp.getStatus(), "submit 应成功: " + resp);
        assertEquals(0, findRecords(contractId).size(), "金额窗口外零节点零记录（无需审批）");
    }

    // ---------- ② 引擎全链（逐节点推进 + 全通过 activate 联动） ----------

    @Test
    public void testApproveActivatesNextNode() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-C", "role-c-1", APPROVER_USER);
        seedMatrix("CT-MTX-C-1", "CT-ROLE-C", 1, null, new BigDecimal("2000"));
        seedMatrix("CT-MTX-D-1", "CT-ROLE-C", 2, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-4-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
        long firstId = latestRecord(contractId, 1).getId();
        long secondId = latestRecord(contractId, 2).getId();

        loginAs(APPROVER_USER);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtApprovalRecord__approve",
                ApiRequest.build(Map.of("recordId", firstId)));
        assertEquals(0, resp.getStatus(), "approve 首节点应成功: " + resp);
        assertEquals(ErpCtConstants.APPROVAL_STATUS_APPROVED, latestRecord(contractId, 1).getApprovalStatus());
        assertEquals(ErpCtConstants.APPROVAL_STATUS_PENDING, latestRecord(contractId, 2).getApprovalStatus(),
                "通过后激活下一节点");
        assertNotNull(latestRecord(contractId, 1).getApprovedAt());
        assertNotNull(secondId);
    }

    @Test
    public void testAllApprovedAllowsActivate() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-E", "role-e-1", APPROVER_USER);
        seedMatrix("CT-MTX-E-1", "CT-ROLE-E", 1, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-5-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
        // 版本定稿（activate 级联签署前置）
        long versionId = findVersionId(contractId);
        executeRpc(mutation, "ErpCtContractVersion__finalizeVersion",
                ApiRequest.build(Map.of("versionId", versionId)));
        loginAs(APPROVER_USER);
        executeRpc(mutation, "ErpCtApprovalRecord__approve",
                ApiRequest.build(Map.of("recordId", latestRecord(contractId, 1).getId())));

        ApiResponse<?> act = executeRpc(mutation, "ErpCtContract__activate",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, act.getStatus(), "全通过后 activate 应成功: " + act);
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, contract.getStatus());
    }

    @Test
    public void testActivateRejectedWhenChainIncomplete() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-F", "role-f-1", APPROVER_USER);
        seedMatrix("CT-MTX-F-1", "CT-ROLE-F", 1, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-6-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
        long versionId = findVersionId(contractId);
        executeRpc(mutation, "ErpCtContractVersion__finalizeVersion",
                ApiRequest.build(Map.of("versionId", versionId)));

        ApiResponse<?> act = executeRpc(mutation, "ErpCtContract__activate",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertNotEquals(0, act.getStatus(), "链未全通过 activate 应拒绝");
        assertTrue(String.valueOf(act).contains("approval-not-complete")
                        || String.valueOf(act).contains("不可激活"),
                "应报 ERR_CT_APPROVAL_NOT_COMPLETE: " + act);
    }

    @Test
    public void testRejectKeepsNegotiationAndNotifies() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-G", "role-g-1", APPROVER_USER);
        seedMatrix("CT-MTX-G-1", "CT-ROLE-G", 1, null, new BigDecimal("2000"));
        loginAs(OWNER_USER);
        long contractId = createContract("DRAFT", "CT-AWF-7-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
        seedNotifyTemplate(8801L, ErpCtConstants.NOTIFY_EVENT_APPROVAL_REJECTED);
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals(OWNER_USER, contract.getCreatedBy(), "合同应以经办人身份创建（createdBy 断言）");

        loginAs(APPROVER_USER);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtApprovalRecord__reject",
                ApiRequest.build(Map.of("recordId", latestRecord(contractId, 1).getId(), "comment", "条款不合规")));
        assertEquals(0, resp.getStatus(), "reject 应成功: " + resp);
        ErpCtApprovalRecord record = latestRecord(contractId, 1);
        assertEquals(ErpCtConstants.APPROVAL_STATUS_REJECTED, record.getApprovalStatus());
        assertNotNull(record.getRejectedAt());
        assertEquals(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION, contract(contractId).getStatus(),
                "驳回保持 NEGOTIATION");
        assertEquals(1, notificationsOf(OWNER_USER, ErpCtConstants.NOTIFY_EVENT_APPROVAL_REJECTED).size(),
                "驳回应通知经办人");
    }

    private ErpCtContract contract(long contractId) {
        return daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
    }

    // ---------- 审批人守卫 ----------

    @Test
    public void testApproveGuardApproverMismatch() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-H", "role-h-1", APPROVER_USER);
        seedMatrix("CT-MTX-H-2", "CT-ROLE-H", 1, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-8-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));

        loginAs(OTHER_USER);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtApprovalRecord__approve",
                ApiRequest.build(Map.of("recordId", latestRecord(contractId, 1).getId())));
        assertNotEquals(0, resp.getStatus(), "审批人不匹配应拒绝");
        assertTrue(String.valueOf(resp).contains("approver-mismatch")
                        || String.valueOf(resp).contains("无权操作"),
                "应报 ERR_CT_APPROVAL_APPROVER_MISMATCH: " + resp);
    }

    @Test
    public void testApproveGuardNonPendingRejected() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-I", "role-i-1", APPROVER_USER);
        seedMatrix("CT-MTX-I-1", "CT-ROLE-I", 1, null, new BigDecimal("2000"));
        seedMatrix("CT-MTX-J-1", "CT-ROLE-I", 2, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-9-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
        loginAs(APPROVER_USER);
        long secondId = latestRecord(contractId, 2).getId();

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtApprovalRecord__approve",
                ApiRequest.build(Map.of("recordId", secondId)));
        assertNotEquals(0, resp.getStatus(), "WAITING 节点不可直接 approve");
        assertTrue(String.valueOf(resp).contains("approval-illegal-status")
                        || String.valueOf(resp).contains("不允许该操作"),
                "应报 ERR_CT_APPROVAL_ILLEGAL_STATUS: " + resp);
    }

    // ---------- ③ D7 驳回→重提→再驳回×3→锁定闭环（追加行生命周期） ----------

    @Test
    public void testResubmitAppendsRowsAndLockAfterRetries() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_MAX_RETRIES, "3");
        seedRole("CT-ROLE-K", "role-k-1", APPROVER_USER);
        seedMatrix("CT-MTX-K-1", "CT-ROLE-K", 1, null, new BigDecimal("2000"));
        seedMatrix("CT-MTX-L-1", "CT-ROLE-K", 2, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-10-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
        loginAs(APPROVER_USER);

        // 轮次 1：驳回节点 1
        reject(contractId, 1);
        assertEquals(1, rejectedCount(contractId, 1), "第 1 轮驳回后派生计数=1");
        assertEquals(1, recordsOfNode(contractId, 1).size(), "轮次 1 节点 1 记录数=1");
        assertEquals(1, recordsOfNode(contractId, 2).size(), "轮次 1 节点 2 记录数=1");

        // 重提 1 → 追加节点 1、2 新行（节点 1 PENDING 节点 2 WAITING），REJECTED 历史保留
        ApiResponse<?> resub1 = executeRpc(mutation, "ErpCtApprovalRecord__resubmit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, resub1.getStatus(), "resubmit 应成功: " + resub1);
        assertEquals(2, recordsOfNode(contractId, 1).size(), "重提后节点 1 追加新行（历史 REJECTED 保留）");
        assertEquals(2, recordsOfNode(contractId, 2).size());
        assertEquals(ErpCtConstants.APPROVAL_STATUS_PENDING, latestRecord(contractId, 1).getApprovalStatus(),
                "重提后驳回节点重新 PENDING");
        assertEquals(ErpCtConstants.APPROVAL_STATUS_WAITING, latestRecord(contractId, 2).getApprovalStatus(),
                "重提后后续节点 WAITING（链序保持）");

        // 轮次 2：再次驳回节点 1
        reject(contractId, 1);
        assertEquals(2, rejectedCount(contractId, 1), "第 2 轮驳回后派生计数=2");

        // 重提 2 → 追加行
        ApiResponse<?> resub2 = executeRpc(mutation, "ErpCtApprovalRecord__resubmit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, resub2.getStatus(), "二次 resubmit 应成功: " + resub2);
        assertEquals(3, recordsOfNode(contractId, 1).size());

        // 轮次 3：第三次驳回 → 派生计数=3 == maxRetries → 锁定
        reject(contractId, 1);
        assertEquals(3, rejectedCount(contractId, 1), "第 3 轮驳回后派生计数=3（达上限）");

        // 锁定后 resubmit / approve 均拒绝
        ApiResponse<?> lockedResub = executeRpc(mutation, "ErpCtApprovalRecord__resubmit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertNotEquals(0, lockedResub.getStatus(), "超限后 resubmit 应拒绝（锁定）");
        assertTrue(String.valueOf(lockedResub).contains("approval-locked")
                        || String.valueOf(lockedResub).contains("已锁定"),
                "应报 ERR_CT_APPROVAL_LOCKED: " + lockedResub);

        // 重提 3（未锁定路径不可达——直接以锁前第 3 轮 PENDING 断言 approve 拒绝）
        // 第 3 轮节点 1 已是 REJECTED，approve 守卫先拒（状态非法）——直接构造守卫前置态：
        // 清空锁定依赖（计数不变）无法绕过——此处断言 approve 对 REJECTED 记录拒绝（双守卫叠加）
        ApiResponse<?> approveRejected = executeRpc(mutation, "ErpCtApprovalRecord__approve",
                ApiRequest.build(Map.of("recordId", latestRecord(contractId, 1).getId())));
        assertNotEquals(0, approveRejected.getStatus(), "锁定态节点 approve 应拒绝");
    }

    @Test
    public void testResubmitNoRejectedRejected() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_ENABLED, "true");
        seedRole("CT-ROLE-M", "role-m-1", APPROVER_USER);
        seedMatrix("CT-MTX-M-1", "CT-ROLE-M", 1, null, new BigDecimal("2000"));
        long contractId = createContract("DRAFT", "CT-AWF-11-", new BigDecimal("1000"));
        executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtApprovalRecord__resubmit",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertNotEquals(0, resp.getStatus(), "无驳回记录时 resubmit 应拒绝");
        assertTrue(String.valueOf(resp).contains("no-rejected")
                        || String.valueOf(resp).contains("无被驳回"),
                "应报 ERR_CT_APPROVAL_NO_REJECTED: " + resp);
    }

    // ---------- helpers ----------

    private void reject(long contractId, int order) {
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtApprovalRecord__reject",
                ApiRequest.build(Map.of("recordId", latestRecord(contractId, order).getId())));
        assertEquals(0, resp.getStatus(), "reject 应成功: " + resp);
    }

    private int rejectedCount(long contractId, int order) {
        int n = 0;
        for (ErpCtApprovalRecord r : findRecords(contractId)) {
            if (r.getApprovalOrder() == order
                    && ErpCtConstants.APPROVAL_STATUS_REJECTED.equals(r.getApprovalStatus())) {
                n++;
            }
        }
        return n;
    }

    private List<ErpCtApprovalRecord> recordsOfNode(long contractId, int order) {
        return findRecords(contractId).stream()
                .filter(r -> r.getApprovalOrder() == order)
                .collect(java.util.stream.Collectors.toList());
    }

    private ErpCtApprovalRecord latestRecord(long contractId, int order) {
        return recordsOfNode(contractId, order).stream()
                .max(java.util.Comparator.comparing(ErpCtApprovalRecord::getId))
                .orElseThrow();
    }

    private List<ErpCtApprovalRecord> findRecords(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        return daoProvider.daoFor(ErpCtApprovalRecord.class).findAllByQuery(q);
    }

    private long findVersionId(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        q.addFilter(eq("isCurrent", true));
        return daoProvider.daoFor(app.erp.contract.dao.entity.ErpCtContractVersion.class)
                .findFirstByQuery(q).getId();
    }

    private void loginAs(String userId) {
        UserContextImpl uc = new UserContextImpl();
        uc.setUserId(userId);
        uc.setUserName(userId);
        IUserContext.set(uc);
        ContextProvider.getOrCreateContext().setUserRefNo(userId);
    }

    private void seedRole(String roleName, String roleId, String userId) {
        ormTemplate.runInSession(() -> {
            NopAuthRole role = new NopAuthRole();
            role.setRoleId(roleId);
            role.setRoleName(roleName);
            daoProvider.daoFor(NopAuthRole.class).saveEntity(role);
            NopAuthUserRole ur = new NopAuthUserRole();
            ur.setRoleId(roleId);
            ur.setUserId(userId);
            daoProvider.daoFor(NopAuthUserRole.class).saveEntity(ur);
        });
    }

    private long seedMatrix(String code, String roleName, int order, BigDecimal min, BigDecimal max) {
        long[] holder = new long[1];
        ormTemplate.runInSession(() -> {
            ErpCtApprovalMatrix m = daoProvider.daoFor(ErpCtApprovalMatrix.class).newEntity();
            m.setCode(code);
            m.setMinAmount(min);
            m.setMaxAmount(max);
            m.setApproverRole(roleName);
            m.setApprovalOrder(order);
            m.setIsActive(true);
            daoProvider.daoFor(ErpCtApprovalMatrix.class).saveEntity(m);
            holder[0] = m.getId();
        });
        return holder[0];
    }

    private long createContract(String status, String codePrefix, BigDecimal totalAmount) {
        long[] ids = new long[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode(codePrefix + "P-" + System.nanoTime());
            p.setName("审批测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-AWF");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", codePrefix + System.nanoTime());
        data.put("contractName", "审批工作流测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", ids[0]);
        data.put("currencyId", ids[1]);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("totalAmount", totalAmount);
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private void seedNotifyTemplate(Long id, String eventType) {
        ormTemplate.runInSession(() -> {
            app.erp.notify.dao.entity.ErpSysNotificationTemplate t =
                    daoProvider.daoFor(app.erp.notify.dao.entity.ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(eventType);
            t.setName("TPL-" + eventType);
            t.setChannelSet(app.erp.notify.service.ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("审批通知 ${contractCode}");
            t.setBodyTpl("合同 ${contractCode} 审批事项，请及时处理");
            t.setRecipientResolver(app.erp.notify.service.ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"${submitterUserId}\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(app.erp.notify.service.ErpNotifyConstants.MERGE_NONE);
            t.setStatus(app.erp.notify.service.ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(app.erp.notify.dao.entity.ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<app.erp.notify.dao.entity.ErpSysNotification> notificationsOf(String userId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(app.erp.notify.dao.entity.ErpSysNotification.class).findAllByQuery(q);
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
