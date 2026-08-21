package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.pur.dao.entity.ErpPurInvoice;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.auth.biz.INopAuthDeptBiz;
import io.nop.auth.biz.INopAuthUserBiz;
import io.nop.auth.dao.entity.NopAuthDept;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同到期扫描 Job 测试（RC-R1.35，P1-MA2-071，UC-CT-05）。
 *
 * <p>覆盖 7 组：① 30/15/7 三级通知（窗口边界 + 落库 + recipient[经办人/经办人上级] 解析 +
 * 无模板静默跳过）；② 批量 expire（ACTIVE+过期 → EXPIRED；未过期零动作）；③ 未完成开票异常路径
 * （D3：isInvoiced=false + planDate≤today 的 InvoicePlan 先 triggerInvoice 再 EXPIRED——发票草稿落库 +
 * plan 回写 + 合同 EXPIRED + 单 plan 触发失败隔离）；④ 续期草稿（config on/off 双路径 +
 * parentContractId 关联 + DRAFT 状态 + 幂等守卫）；⑤ cron 空值跳过；⑥ 单条失败隔离
 * （续期草稿 code 冲突 → 该合同 WARN 跳过不 EXPIRED，其它合同正常推进）；⑦ GraphQL RPC 冒烟
 * （scanExpiringContracts/expireOverdueContracts 经 IGraphQLEngine 可路由）。
 *
 * <p>时间冻结在 {@link CtFrozenClockExtension#REFERENCE_DATE}（2026-07-17）。手工装配 Job bean
 * （镜像 TestErpCtApprovalTimeoutJob.newWiredJob 范式——biz_* 代理 bean 的 lazy props 在测试容器
 * 按需创建时不赋值）。通知模板按 R1.4/R1.34 范式 seed（USER_LIST + ${ownerUserId}/${escalationUserId}
 * 插值；无 ACTIVE 模板时 notify config-gated 静默跳过）。通知与合同关联经 payloadJson.contractId 断言。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtContractExpiryJob extends JunitAutoTestCase {

    static final String JOB_ENABLED_KEY = "nop.job.erp-ct-contract-expiry.enabled";
    static final String OWNER_USER = "ct-exp-owner";
    static final String ESCALATION_USER = "ct-exp-manager";
    static final String DEPT_MANAGER_USER = "ct-exp-dept-mgr";
    static final String DEPT_ID = "ct-exp-dept-01";

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    app.erp.ct.biz.IErpCtContractBiz contractBiz;
    @Inject
    INopAuthUserBiz authUserBiz;
    @Inject
    INopAuthDeptBiz authDeptBiz;
    @Inject
    app.erp.notify.biz.IErpSysNotificationBiz notificationBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    public void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_CONTRACT_EXPIRY_CRON, "");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCtConfigs.CFG_AUTO_CREATE_RENEWAL_DRAFT, "false");
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
    }

    private app.erp.ct.service.job.ErpCtContractExpiryJob newWiredJob() {
        app.erp.ct.service.job.ErpCtContractExpiryJob job =
                new app.erp.ct.service.job.ErpCtContractExpiryJob();
        job.setContractBiz(contractBiz);
        job.setAuthUserBiz(authUserBiz);
        job.setAuthDeptBiz(authDeptBiz);
        job.setNotificationBiz(notificationBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    // ---------- ① 30/15/7 三级通知（窗口边界 + 落库 + recipient + 无模板静默跳过） ----------

    @Test
    public void testTieredWarningsByRemainingDays() {
        seedOwnerUser();
        // 30 天档（16 天 > 15 → 30 天通知经办人）
        String c30 = seedContract("CT-EXP-30", LocalDate.of(2026, 8, 2));
        // 15 天档（10 天 ≤ 15 → 15 天通知经办人）
        String c15 = seedContract("CT-EXP-15", LocalDate.of(2026, 7, 27));
        // 7 天档（3 天 ≤ 7 → 升级通知经办人上级）
        String c7 = seedContract("CT-EXP-7", LocalDate.of(2026, 7, 20));
        seedTemplate("8831", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_30,
                "{\"userIds\":[\"${ownerUserId}\"]}");
        seedTemplate("8832", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_15,
                "{\"userIds\":[\"${ownerUserId}\"]}");
        seedTemplate("8833", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7,
                "{\"userIds\":[\"${escalationUserId}\"]}");
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        List<ErpSysNotification> n30 = notificationsOf(OWNER_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_30);
        assertEquals(1, n30.size(), "30 天档应通知经办人 1 条");
        assertEquals(c30, payloadContractId(n30.get(0)), "30 天档通知应指向 30 天档合同");
        assertEquals(ErpNotifyConstants.STATUS_SENT, n30.get(0).getStatus());
        List<ErpSysNotification> n15 = notificationsOf(OWNER_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_15);
        assertEquals(1, n15.size(), "15 天档应通知经办人 1 条");
        assertEquals(c15, payloadContractId(n15.get(0)), "15 天档通知应指向 15 天档合同");
        List<ErpSysNotification> esc = notificationsOf(ESCALATION_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7);
        assertEquals(1, esc.size(), "7 天档应升级通知经办人上级 1 条");
        assertEquals(ESCALATION_USER, esc.get(0).getRecipientUserId(), "升级接收人应为经办人上级");
        assertEquals(c7, payloadContractId(esc.get(0)), "升级通知应指向 7 天档合同");
        assertTrue(notificationsOf(DEPT_MANAGER_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7).isEmpty(),
                "经办人有直接上级时不使用部门负责人兜底");
    }

    @Test
    public void testTierBoundaries() {
        seedOwnerUser();
        // 边界：剩余 15 天 → 15 天档；剩余 16 天 → 30 天档
        String c15 = seedContract("CT-EXP-B15", LocalDate.of(2026, 8, 1));
        String c30 = seedContract("CT-EXP-B30", LocalDate.of(2026, 8, 2));
        seedTemplate("8834", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_30,
                "{\"userIds\":[\"${ownerUserId}\"]}");
        seedTemplate("8835", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_15,
                "{\"userIds\":[\"${ownerUserId}\"]}");
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertTrue(notificationsOf(OWNER_USER, ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_15)
                        .stream().anyMatch(n -> payloadContractId(n).equals(c15)),
                "剩余 15 天应落入 15 天档");
        assertTrue(notificationsOf(OWNER_USER, ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_WARNING_30)
                        .stream().anyMatch(n -> payloadContractId(n).equals(c30)),
                "剩余 16 天应落入 30 天档");
    }

    @Test
    public void testEscalationFallsBackToDeptManager() {
        // 经办人无 managerId，但有 deptId → 部门负责人兜底
        seedUser(OWNER_USER, null, DEPT_ID);
        seedDept(DEPT_ID, DEPT_MANAGER_USER);
        seedContract("CT-EXP-DEPT", LocalDate.of(2026, 7, 20));
        seedTemplate("8836", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7,
                "{\"userIds\":[\"${escalationUserId}\"]}");
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        List<ErpSysNotification> esc = notificationsOf(DEPT_MANAGER_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7);
        assertEquals(1, esc.size(), "无直接上级时应兜底部门负责人");
    }

    @Test
    public void testEscalationSkippedWhenNoSuperior() {
        seedUser(OWNER_USER, null, null);
        seedContract("CT-EXP-NO-SUP", LocalDate.of(2026, 7, 20));
        seedTemplate("8837", ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7,
                "{\"userIds\":[\"${escalationUserId}\"]}");
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertTrue(notificationsOf(ESCALATION_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7).isEmpty(),
                "双 null（无上级且无部门负责人）应跳过升级通知（LOG.warn）");
    }

    @Test
    public void testNoTemplateSilentlySkips() {
        seedOwnerUser();
        seedContract("CT-EXP-NO-TPL", LocalDate.of(2026, 7, 20));
        setCron("0 0 1 * * ?");

        // 无 ACTIVE 模板 → notify config-gated 静默跳过，不抛异常
        newWiredJob().execute();

        assertTrue(notificationsOf(ESCALATION_USER,
                ErpCtConstants.NOTIFY_EVENT_CONTRACT_EXPIRY_ESCALATION_7).isEmpty());
    }

    // ---------- ② 批量 expire ----------

    @Test
    public void testBatchExpireOverdueContracts() {
        String overdue = seedContract("CT-EXP-OVERDUE", LocalDate.of(2026, 7, 16));
        String future = seedContract("CT-EXP-FUTURE", LocalDate.of(2026, 12, 31));
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED,
                contract(overdue).getStatus(), "过期合同应批量置 EXPIRED");
        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE,
                contract(future).getStatus(), "未过期合同零动作");
    }

    // ---------- ③ 未完成开票异常路径（D3） ----------

    @Test
    public void testDueInvoiceTriggeredBeforeExpire() {
        String contractId = seedContract("CT-EXP-INV", LocalDate.of(2026, 7, 16));
        String lineId = seedContractLine(contractId, true);
        // isInvoiced=false 且 planDate ≤ today → 先完成开票再 EXPIRED
        String planId = seedInvoicePlan(lineId, LocalDate.of(2026, 7, 1), false);
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
        assertTrue(Boolean.TRUE.equals(plan.getIsInvoiced()), "到期未开票计划应先触发开票");
        assertNotNull(plan.getInvoiceBillCode(), "应回写发票单号");
        List<ErpPurInvoice> invoices = daoProvider.daoFor(ErpPurInvoice.class).findAllByQuery(
                eqQuery("code", plan.getInvoiceBillCode()));
        assertEquals(1, invoices.size(), "应生成 AP 发票草稿");
        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED, contract(contractId).getStatus(),
                "完成开票后合同应 EXPIRED");
    }

    @Test
    public void testInvoiceTriggerFailureIsolatedPerPlan() {
        // 构造 plan 对应合同行物理删除 → triggerInvoice 行加载失败 → 单 plan WARN 隔离，
        // 合同仍 EXPIRED（D3 触发失败不影响 expire 主路径的失败隔离语义）
        String contractId = seedContract("CT-EXP-INV-FAIL", LocalDate.of(2026, 7, 16));
        String lineId = seedContractLine(contractId, true);
        String planId = seedInvoicePlan(lineId, LocalDate.of(2026, 7, 1), false);
        ormTemplate.runInSession(session -> {
            ErpCtContractLine line = daoProvider.daoFor(ErpCtContractLine.class).getEntityById(lineId);
            line.orm_disableLogicalDelete(true);
            daoProvider.daoFor(ErpCtContractLine.class).deleteEntity(line);
            return null;
        });
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
        assertFalse(Boolean.TRUE.equals(plan.getIsInvoiced()), "触发失败的计划保持未开票");
        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED, contract(contractId).getStatus(),
                "单计划触发失败不阻断合同 expire（D3 失败隔离）");
    }

    // ---------- ④ 续期草稿（config on/off + parentContractId + DRAFT + 幂等） ----------

    @Test
    public void testRenewalDraftCreatedWhenConfigOn() {
        String contractId = seedContract("CT-EXP-RN-ON", LocalDate.of(2026, 7, 16));
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCtConfigs.CFG_AUTO_CREATE_RENEWAL_DRAFT, "true");
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        ErpCtContract original = contract(contractId);
        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED, original.getStatus());
        List<ErpCtContract> drafts = daoProvider.daoFor(ErpCtContract.class).findAllByQuery(
                eqQuery("parentContractId", contractId));
        assertEquals(1, drafts.size(), "config on 应创建 1 份续期草稿");
        ErpCtContract draft = drafts.get(0);
        assertEquals(ErpCtConstants.CONTRACT_STATUS_DRAFT, draft.getStatus(), "续期草稿应为 DRAFT");
        assertEquals(contractId, draft.getParentContractId(), "parentContractId 关联原合同");
        assertEquals("CT-EXP-RN-ON-RN", draft.getCode(), "草稿 code 带 -RN 后缀");
        assertTrue(draft.getStartDate().isAfter(original.getEndDate()), "草稿生效日应在原到期日后");
    }

    @Test
    public void testRenewalDraftSkippedWhenConfigOff() {
        String contractId = seedContract("CT-EXP-RN-OFF", LocalDate.of(2026, 7, 16));
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED, contract(contractId).getStatus());
        assertTrue(daoProvider.daoFor(ErpCtContract.class).findAllByQuery(
                eqQuery("parentContractId", contractId)).isEmpty(),
                "config off 不创建续期草稿");
    }

    @Test
    public void testRenewalDraftIdempotentGuard() {
        String contractId = seedContract("CT-EXP-RN-IDEM", LocalDate.of(2026, 7, 16));
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCtConfigs.CFG_AUTO_CREATE_RENEWAL_DRAFT, "true");
        // 预置一份已存在续期草稿 → 幂等守卫应跳过创建（仅 1 份）
        seedRenewalDraft(contractId, "CT-EXP-RN-IDEM-RN");
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertEquals(1, daoProvider.daoFor(ErpCtContract.class).findAllByQuery(
                eqQuery("parentContractId", contractId)).size(),
                "已存在续期草稿时幂等守卫不重复建");
    }

    // ---------- ⑤ cron 空值跳过 ----------

    @Test
    public void testCronEmptySkipsScan() {
        String overdue = seedContract("CT-EXP-CRON-EMPTY", LocalDate.of(2026, 7, 16));
        setCron("");

        newWiredJob().execute();

        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, contract(overdue).getStatus(),
                "cron 空值应跳过扫描，合同保持 ACTIVE");
    }

    // ---------- ⑥ 单条失败隔离 ----------

    @Test
    public void testSingleContractFailureIsolated() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCtConfigs.CFG_AUTO_CREATE_RENEWAL_DRAFT, "true");
        // 合同 A：预置同 orgId 同 code 草稿行 → 建续期草稿 UK 冲突 → 该合同 WARN 跳过不 EXPIRED
        String bad = seedContract("CT-EXP-ISO-BAD", LocalDate.of(2026, 7, 16));
        seedDraftWithCode("CT-EXP-ISO-BAD-RN");
        // 合同 B：正常 → 应正常推进 EXPIRED
        String good = seedContract("CT-EXP-ISO-GOOD", LocalDate.of(2026, 7, 16));
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertEquals(ErpCtConstants.CONTRACT_STATUS_ACTIVE, contract(bad).getStatus(),
                "草稿冲突合同应 WARN 跳过不 EXPIRED（失败隔离）");
        assertEquals(ErpCtConstants.CONTRACT_STATUS_EXPIRED, contract(good).getStatus(),
                "其它合同正常推进");
    }

    // ---------- ⑦ GraphQL RPC 冒烟 ----------

    @Test
    public void testGraphQLRpcSmoke() {
        seedContract("CT-EXP-RPC", LocalDate.of(2026, 7, 16));
        setCron("0 0 1 * * ?");

        // scanExpiringContracts 可路由（@BizQuery）
        ApiResponse<?> scan = executeRpc(GraphQLOperationType.query,
                "ErpCtContract__scanExpiringContracts", ApiRequest.build(new LinkedHashMap<>()));
        assertEquals(0, scan.getStatus(), "scanExpiringContracts 冒烟应成功: " + scan);

        // expireOverdueContracts 可路由（@BizMutation）
        ApiResponse<?> expire = executeRpc(GraphQLOperationType.mutation,
                "ErpCtContract__expireOverdueContracts", ApiRequest.build(new LinkedHashMap<>()));
        assertEquals(0, expire.getStatus(), "expireOverdueContracts 冒烟应成功: " + expire);
    }

    // ---------- helpers ----------

    private void setCron(String cron) {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_CONTRACT_EXPIRY_CRON, cron);
    }

    private void seedOwnerUser() {
        seedUser(OWNER_USER, ESCALATION_USER, null);
    }

    private void seedUser(String userId, String managerId, String deptId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopAuthUser> userDao = daoProvider.daoFor(NopAuthUser.class);
            NopAuthUser user = new NopAuthUser();
            user.setUserId(userId);
            user.setUserName(userId);
            user.setNickName(userId);
            user.setPassword("dummy-pwd");
            user.setOpenId(userId);
            user.setGender(0);
            user.setUserType(0);
            user.setStatus(0);
            user.setTenantId("0");
            if (managerId != null) {
                user.setManagerId(managerId);
            }
            if (deptId != null) {
                user.setDeptId(deptId);
            }
            userDao.saveEntity(user);
        });
    }

    private void seedDept(String deptId, String managerId) {
        ormTemplate.runInSession(() -> {
            NopAuthDept dept = new NopAuthDept();
            dept.setDeptId(deptId);
            dept.setDeptName("到期升级测试部门");
            dept.setManagerId(managerId);
            daoProvider.daoFor(NopAuthDept.class).saveEntity(dept);
        });
    }

    private String seedContract(String code, LocalDate endDate) {
        String[] ids = new String[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-EXP-PARTNER");
            p.setName("到期 job 测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-EXP");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        return seedContractWithMaster(code, endDate, ids[0], ids[1], null);
    }

    private String seedContractWithMaster(String code, LocalDate endDate, String partnerId, String currencyId,
                                        String parentContractId) {
        String[] ret = new String[1];
        ormTemplate.runInSession(session -> {
            ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).newEntity();
            contract.orm_disableAutoStamp(true);
            contract.setCode(code);
            contract.setContractName("到期 job 测试合同 " + code);
            contract.setContractType(ErpCtConstants.CONTRACT_TYPE_PURCHASE);
            contract.setContractDirection(ErpCtConstants.CONTRACT_DIRECTION_INBOUND);
            contract.setPartnerId(partnerId);
            contract.setCurrencyId(currencyId);
            contract.setOrgId("1");
            contract.setStartDate(LocalDate.of(2026, 1, 1));
            contract.setEndDate(endDate);
            contract.setTotalAmount(new BigDecimal("1000"));
            contract.setStatus(ErpCtConstants.CONTRACT_STATUS_ACTIVE);
            contract.setBusinessDate(LocalDate.of(2026, 7, 17));
            if (parentContractId != null) {
                contract.setStatus(ErpCtConstants.CONTRACT_STATUS_DRAFT);
                contract.setParentContractId(parentContractId);
            }
            contract.setCreatedBy(OWNER_USER);
            contract.setUpdatedBy(OWNER_USER);
            contract.setCreateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            contract.setUpdateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            daoProvider.daoFor(ErpCtContract.class).saveEntity(contract);
            ret[0] = contract.getId();
            return null;
        });
        return ret[0];
    }

    /** 预置续期草稿（parentContractId 关联 + DRAFT），供幂等守卫测试。 */
    private void seedRenewalDraft(String parentContractId, String code) {
        String[] ids = new String[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-EXP-RN-PARTNER");
            p.setName("到期 job 测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-RN");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        ormTemplate.runInSession(() -> {
            ErpCtContract draft = daoProvider.daoFor(ErpCtContract.class).newEntity();
            draft.orm_disableAutoStamp(true);
            draft.setCode(code);
            draft.setContractName("续期草稿 " + code);
            draft.setContractType(ErpCtConstants.CONTRACT_TYPE_PURCHASE);
            draft.setContractDirection(ErpCtConstants.CONTRACT_DIRECTION_INBOUND);
            draft.setPartnerId(ids[0]);
            draft.setCurrencyId(ids[1]);
            draft.setOrgId("1");
            draft.setStartDate(LocalDate.of(2026, 8, 1));
            draft.setEndDate(LocalDate.of(2027, 7, 31));
            draft.setTotalAmount(new BigDecimal("1000"));
            draft.setStatus(ErpCtConstants.CONTRACT_STATUS_DRAFT);
            draft.setBusinessDate(LocalDate.of(2026, 7, 17));
            draft.setParentContractId(parentContractId);
            draft.setCreatedBy(OWNER_USER);
            draft.setUpdatedBy(OWNER_USER);
            draft.setCreateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            draft.setUpdateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            daoProvider.daoFor(ErpCtContract.class).saveEntity(draft);
        });
    }

    /** 预置独立草稿（无 parentContractId），供 UK 冲突隔离测试。 */
    private void seedDraftWithCode(String code) {
        String[] ids = new String[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-EXP-CONFLICT-PARTNER");
            p.setName("到期 job 测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-CF");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        ormTemplate.runInSession(() -> {
            ErpCtContract draft = daoProvider.daoFor(ErpCtContract.class).newEntity();
            draft.orm_disableAutoStamp(true);
            draft.setCode(code);
            draft.setContractName("预置冲突草稿 " + code);
            draft.setContractType(ErpCtConstants.CONTRACT_TYPE_PURCHASE);
            draft.setContractDirection(ErpCtConstants.CONTRACT_DIRECTION_INBOUND);
            draft.setPartnerId(ids[0]);
            draft.setCurrencyId(ids[1]);
            draft.setOrgId("1");
            draft.setStartDate(LocalDate.of(2026, 1, 1));
            draft.setEndDate(LocalDate.of(2027, 12, 31));
            draft.setTotalAmount(new BigDecimal("1000"));
            draft.setStatus(ErpCtConstants.CONTRACT_STATUS_DRAFT);
            draft.setBusinessDate(LocalDate.of(2026, 7, 17));
            draft.setCreatedBy(OWNER_USER);
            draft.setUpdatedBy(OWNER_USER);
            draft.setCreateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            draft.setUpdateTime(new java.sql.Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            daoProvider.daoFor(ErpCtContract.class).saveEntity(draft);
        });
    }

    private String seedContractLine(String contractId, boolean withMaterial) {
        final String materialId = withMaterial ? seedMaterial() : null;
        String[] ret = new String[1];
        ormTemplate.runInSession(() -> {
            ErpCtContractLine line = daoProvider.daoFor(ErpCtContractLine.class).newEntity();
            line.setContractId(contractId);
            line.setLineNo(1);
            if (withMaterial) {
                line.setMaterialId(materialId);
            }
            line.setQuantity(new BigDecimal("100"));
            line.setUnitPrice(new BigDecimal("10"));
            line.setAmount(new BigDecimal("1000"));
            daoProvider.daoFor(ErpCtContractLine.class).saveEntity(line);
            ret[0] = line.getId();
        });
        return ret[0];
    }

    private String seedMaterial() {
        String[] ret = new String[2];
        ormTemplate.runInSession(session -> {
            ErpMdUoM u = daoProvider.daoFor(ErpMdUoM.class).newEntity();
            u.setCode("PCS-EXP");
            u.setName("个");
            daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
            ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
            m.setCode("MAT-CT-EXP");
            m.setName("到期 job 测试物料");
            m.setMaterialType("GOODS");
            m.setUoMId(u.getId());
            m.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
            ret[0] = u.getId();
            ret[1] = m.getId();
            return null;
        });
        return ret[1];
    }

    private String seedInvoicePlan(String lineId, LocalDate planDate, boolean isInvoiced) {
        String[] ret = new String[1];
        ormTemplate.runInSession(() -> {
            ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).newEntity();
            plan.setContractLineId(lineId);
            plan.setPlanDate(planDate);
            plan.setAmount(new BigDecimal("1000"));
            plan.setInvoiceTerm("MILESTONE");
            plan.setIsInvoiced(isInvoiced);
            daoProvider.daoFor(ErpCtInvoicePlan.class).saveEntity(plan);
            ret[0] = plan.getId();
        });
        return ret[0];
    }

    private void seedTemplate(String id, String notificationType, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            ErpSysNotificationTemplate t = daoProvider.daoFor(ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("TPL-" + notificationType);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("合同到期提醒: ${contractCode}");
            t.setBodyTpl("合同 ${contractCode} 即将到期，请及时处理");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String userId, String notificationType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", notificationType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private ErpCtContract contract(String contractId) {
        return daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
    }

    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }

    /** 从通知 payloadJson 提取 contractId（通知与合同关联断言载体）。 */
    @SuppressWarnings("unchecked")
    private String payloadContractId(ErpSysNotification notification) {
        Object parsed = JsonTool.parseNonStrict(notification.getPayloadJson());
        Map<String, Object> payload = (Map<String, Object>) parsed;
        return String.valueOf(payload.get("contractId"));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType,
                                      String action, ApiRequest<?> request) {
        io.nop.graphql.core.IGraphQLExecutionContext ctx =
                graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
