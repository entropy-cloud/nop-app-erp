package app.erp.ct.service;

import app.erp.ct.biz.IErpCtDocumentBiz;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtDocument;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同文档保留策略归档/销毁测试（RC-R1.80 Phase 4，P1-RC-079，UC-CT-10 D）。
 *
 * <p>覆盖：① 到期归档（retentionDate≤today→isArchived+archiveDate；未到期零动作）；
 * ② legalHold 双阻断（阻断归档 + 阻断销毁，行仍可见）；③ ACTIVE 合同阻断归档（job 守卫短路）；
 * ④ 销毁 D4-a 逻辑删除断言——行从常规查询消失 + disableLogicalDelete 复核 delVersion 软删行
 * 仍在（耐久销毁证据）+ remark 销毁事件记录 + 审计通知派发（有 ACTIVE 模板）；
 * ⑤ purge 守卫——未归档拒（ERR_PURGE_NOT_ARCHIVED）、purgeDate 未到拒（ERR_PURGE_NOT_DUE，
 * 保留义务禁止提前销毁）、非 admin 角色拒（fail-closed）；⑥ config 关闭零动作
 * （doc-auto-archive=false 不归档；doc-auto-purge=false 不销毁——人工确认语义）；
 * ⑦ job 幂等（二次运行零新增）+ cron 空值跳过；⑧ 无 ACTIVE 模板时审计通知静默跳过不阻断销毁。
 *
 * <p>D4 双独立子 agent 批准：ses_fe312b7c5ffe8EW1Va8o42aJZ2 / ses_fe312882dffeVh8nosoYox3lGN
 * （数据删除保护区域 auto + dual-agent-approval）。时间冻结 2026-07-17。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtDocRetention extends JunitAutoTestCase {

    static final String JOB_ENABLED_KEY = "nop.job.erp-ct-doc-retention.enabled";
    static final String PURGE_ADMIN_USER = "ct-purge-admin";

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpCtDocumentBiz documentBiz;

    private IUserContext prevCtx;

    @BeforeEach
    void loginAdmin() {
        prevCtx = IUserContext.get();
        UserContextImpl uc = new UserContextImpl();
        uc.setUserId(PURGE_ADMIN_USER);
        uc.setUserName(PURGE_ADMIN_USER);
        uc.setRoles(java.util.Set.of(ErpCtConstants.LEGAL_HOLD_ROLE_ID));
        IUserContext.set(uc);
        setCron("0 0 2 * * ?");
    }

    @AfterEach
    void restore() {
        IUserContext.set(prevCtx);
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_RETENTION_CRON, "");
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_AUTO_ARCHIVE, "true");
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_AUTO_PURGE, "false");
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
    }

    // ---------- ① 到期归档 ----------

    @Test
    public void testDueRetentionArchives() {
        long due = seedDocument("CT-RET-DUE", LocalDate.of(2026, 7, 1), null, false, null);
        long notDue = seedDocument("CT-RET-FUTURE", LocalDate.of(2027, 12, 31), null, false, null);

        newWiredJob().execute();

        ErpCtDocument archived = document(due);
        assertTrue(Boolean.TRUE.equals(archived.getIsArchived()), "到期文档应自动归档");
        assertEquals(LocalDate.of(2026, 7, 17), archived.getArchiveDate(), "archiveDate = 扫描日");
        assertFalse(Boolean.TRUE.equals(document(notDue).getIsArchived()), "未到期文档零动作");
    }

    @Test
    public void testAutoArchiveConfigOffZeroAction() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_AUTO_ARCHIVE, "false");
        long due = seedDocument("CT-RET-OFF", LocalDate.of(2026, 7, 1), null, false, null);

        newWiredJob().execute();

        assertFalse(Boolean.TRUE.equals(document(due).getIsArchived()),
                "doc-auto-archive=false 时到期文档零归档动作");
    }

    // ---------- ② legalHold 双阻断 ----------

    @Test
    public void testLegalHoldBlocksArchiveAndPurge() {
        long holdArchive = seedDocument("CT-RET-HOLD-A", LocalDate.of(2026, 7, 1), null, false, true);
        // 已归档 + 到期销毁 + legalHold
        long holdPurge = seedDocument("CT-RET-HOLD-P", LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 7, 1), true, true);

        newWiredJob().execute();

        assertFalse(Boolean.TRUE.equals(document(holdArchive).getIsArchived()), "legalHold 阻断自动归档");
        assertNotNull(visibleRow(holdPurge), "legalHold 阻断销毁（行仍可见）");
        assertTrue(Boolean.TRUE.equals(document(holdPurge).getIsArchived()));
    }

    // ---------- ③ ACTIVE 合同阻断归档 ----------

    @Test
    public void testActiveContractBlocksAutoArchive() {
        long activeId = seedContract("CT-RET-ACTIVE", ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        long expiredId = seedContract("CT-RET-EXPIRED", ErpCtConstants.CONTRACT_STATUS_EXPIRED);
        long docActive = seedDocument("CT-RET-DOC-A", LocalDate.of(2026, 7, 1), null, false, null);
        long docExpired = seedDocument("CT-RET-DOC-E", LocalDate.of(2026, 7, 1), null, false, null);
        linkContract(docActive, activeId);
        linkContract(docExpired, expiredId);

        newWiredJob().execute();

        assertFalse(Boolean.TRUE.equals(document(docActive).getIsArchived()),
                "ACTIVE 合同文档不归档（owner doc §合规规则第一行）");
        assertTrue(Boolean.TRUE.equals(document(docExpired).getIsArchived()),
                "非 ACTIVE 合同文档正常归档（对照）");
    }

    // ---------- ④ D4 逻辑删除 + 审计 ----------

    @Test
    public void testPurgeLogicalDeleteWithAuditRecord() {
        seedTemplate(8841L, ErpCtConstants.NOTIFY_EVENT_DOCUMENT_PURGED,
                "{\"userIds\":[\"" + PURGE_ADMIN_USER + "\"]}");
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_AUTO_PURGE, "true");
        long docId = seedDocument("CT-RET-PURGE", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 7, 1),
                true, null);

        newWiredJob().execute();

        // 常规查询不可见（销毁语义）
        assertNull(visibleRow(docId), "销毁后行应从常规查询消失");
        assertNull(documentBiz.get(String.valueOf(docId), true, new io.nop.core.context.ServiceContextImpl()),
                "biz get 不可见");

        // D4-a 耐久销毁证据：软删行仍在（delVersion>0）+ remark 销毁事件
        QueryBean recovery = new QueryBean();
        recovery.addFilter(eq("id", docId));
        recovery.setDisableLogicalDelete(true);
        List<ErpCtDocument> rows = daoProvider.daoFor(ErpCtDocument.class).findAllByQuery(recovery);
        assertEquals(1, rows.size(), "disableLogicalDelete 复核软删行仍在（耐久证据）");
        ErpCtDocument purged = rows.get(0);
        assertTrue(purged.getDelVersion() != null && purged.getDelVersion() > 0,
                "delVersion 软删标记已置（实际: " + purged.getDelVersion() + "）");
        assertNotNull(purged.getRemark(), "remark 销毁事件记录（耐久审计载体）");
        assertTrue(purged.getRemark().contains("已销毁(purge)"), "remark 含销毁事件: " + purged.getRemark());
        assertTrue(purged.getRemark().contains(PURGE_ADMIN_USER), "remark 含操作人");

        // 审计通知派发（有 ACTIVE 模板）
        assertEquals(1, notificationsOf(PURGE_ADMIN_USER).size(), "销毁审计通知应派发 1 条");
        assertEquals(ErpCtConstants.NOTIFY_EVENT_DOCUMENT_PURGED, notificationsOf(PURGE_ADMIN_USER).get(0).getNotificationType());
    }

    @Test
    public void testPurgeSilentSkipWithoutTemplate() {
        // 无 ACTIVE 模板 → best-effort 静默跳过，不阻断销毁主流程
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_AUTO_PURGE, "true");
        long docId = seedDocument("CT-RET-PURGE-NT", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 7, 1),
                true, null);

        newWiredJob().execute();

        assertNull(visibleRow(docId), "无模板时销毁主流程不受影响");
        assertEquals(0, notificationsOf(PURGE_ADMIN_USER).size(), "无模板零通知（静默跳过）");
    }

    // ---------- ⑤ purge 守卫（mutation 面） ----------

    @Test
    public void testPurgeGuards() {
        long notArchived = seedDocument("CT-RET-PG-NA", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 7, 1),
                false, null);
        long notDue = seedDocument("CT-RET-PG-ND", LocalDate.of(2020, 1, 1), LocalDate.of(2027, 12, 31),
                true, null);

        ApiResponse<?> na = executeRpc(mutation, "ErpCtDocument__purge",
                ApiRequest.build(Map.of("documentId", notArchived)));
        assertNotEquals(0, na.getStatus(), "未归档文档销毁应被拒");
        assertTrue(String.valueOf(na).contains("purge-not-archived") || String.valueOf(na).contains("未归档"),
                "应报 ERR_CT_DOCUMENT_PURGE_NOT_ARCHIVED: " + na);

        ApiResponse<?> nd = executeRpc(mutation, "ErpCtDocument__purge",
                ApiRequest.build(Map.of("documentId", notDue)));
        assertNotEquals(0, nd.getStatus(), "purgeDate 未到销毁应被拒（保留义务，禁止提前销毁）");
        assertTrue(String.valueOf(nd).contains("purge-not-due") || String.valueOf(nd).contains("未到达"),
                "应报 ERR_CT_DOCUMENT_PURGE_NOT_DUE: " + nd);
        assertNotNull(visibleRow(notDue), "拒绝路径零状态变更");

        // 非 admin 角色 fail-closed 拒绝
        UserContextImpl plain = new UserContextImpl();
        plain.setUserId("ct-plain");
        plain.setUserName("ct-plain");
        plain.setRoles(java.util.Set.of("合同专员"));
        IUserContext.set(plain);
        long due = seedDocument("CT-RET-PG-ROLE", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 7, 1),
                true, null);
        ApiResponse<?> denied = executeRpc(mutation, "ErpCtDocument__purge",
                ApiRequest.build(Map.of("documentId", due)));
        assertNotEquals(0, denied.getStatus(), "非 admin 销毁应 fail-closed 拒绝");
        assertTrue(String.valueOf(denied).contains("document-role-required"),
                "应报 ERR_CT_DOCUMENT_ROLE_REQUIRED: " + denied);
        assertNotNull(visibleRow(due), "拒绝路径零状态变更");
    }

    // ---------- ⑥ doc-auto-purge=false 不销毁（人工确认语义） ----------

    @Test
    public void testAutoPurgeConfigOff() {
        long docId = seedDocument("CT-RET-PURGE-OFF", LocalDate.of(2020, 1, 1), LocalDate.of(2026, 7, 1),
                true, null);

        newWiredJob().execute();

        assertNotNull(visibleRow(docId), "doc-auto-purge=false（默认）到期文档不销毁——需人工确认");
    }

    // ---------- ⑦ job 幂等 + cron 空值跳过 ----------

    @Test
    public void testJobIdempotentAndCronEmptySkips() {
        long due = seedDocument("CT-RET-IDEM", LocalDate.of(2026, 7, 1), null, false, null);

        newWiredJob().execute();
        LocalDate firstArchiveDate = document(due).getArchiveDate();
        newWiredJob().execute();

        assertEquals(LocalDate.of(2026, 7, 17), document(due).getArchiveDate(), "幂等：archiveDate 不变");
        assertEquals(firstArchiveDate, document(due).getArchiveDate());

        // cron 空值 = 不调度语义
        long another = seedDocument("CT-RET-CRON", LocalDate.of(2026, 7, 1), null, false, null);
        setCron("");
        newWiredJob().execute();
        assertFalse(Boolean.TRUE.equals(document(another).getIsArchived()),
                "cron 空值应跳过扫描（不调度语义）");
    }

    // ---------- helpers ----------

    private void setCron(String cron) {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_DOC_RETENTION_CRON, cron);
    }

    private app.erp.ct.service.job.ErpCtDocRetentionJob newWiredJob() {
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
        app.erp.ct.service.job.ErpCtDocRetentionJob job = new app.erp.ct.service.job.ErpCtDocRetentionJob();
        job.setDocumentBiz(documentBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    private ErpCtDocument document(long docId) {
        return ormTemplate.runInSession(session -> daoProvider.daoFor(ErpCtDocument.class).getEntityById(docId));
    }

    private ErpCtDocument visibleRow(long docId) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("id", docId));
            List<ErpCtDocument> rows = daoProvider.daoFor(ErpCtDocument.class).findAllByQuery(q);
            return rows.isEmpty() ? null : rows.get(0);
        });
    }

    /** seed 文档：retentionDate/purgeDate/isArchived/legalHold 可配。 */
    private long seedDocument(String code, LocalDate retentionDate, LocalDate purgeDate,
                              boolean archived, Boolean legalHold) {
        long[] ret = new long[1];
        ormTemplate.runInSession(session -> {
            ErpCtDocument doc = daoProvider.daoFor(ErpCtDocument.class).newEntity();
            doc.orm_disableAutoStamp(true);
            doc.setCode(code);
            doc.setDocName("保留策略测试文档 " + code);
            doc.setDocType("10");
            doc.setRetentionDate(retentionDate);
            doc.setPurgeDate(purgeDate);
            doc.setIsArchived(archived);
            if (archived) {
                doc.setArchiveDate(LocalDate.of(2026, 6, 1));
            }
            doc.setLegalHold(legalHold);
            doc.setCreatedBy("ct-ret-test");
            doc.setUpdatedBy("ct-ret-test");
            doc.setCreateTime(java.sql.Timestamp.valueOf(CtFrozenClockExtension.REFERENCE_DATE.atStartOfDay()));
            doc.setUpdateTime(java.sql.Timestamp.valueOf(CtFrozenClockExtension.REFERENCE_DATE.atStartOfDay()));
            daoProvider.daoFor(ErpCtDocument.class).saveEntity(doc);
            ret[0] = doc.getId();
            return null;
        });
        return ret[0];
    }

    private void linkContract(long docId, long contractId) {
        ormTemplate.runInSession(session -> {
            ErpCtDocument doc = daoProvider.daoFor(ErpCtDocument.class).getEntityById(docId);
            doc.setContractId(contractId);
            daoProvider.daoFor(ErpCtDocument.class).updateEntity(doc);
            return null;
        });
    }

    private long seedContract(String code, String status) {
        long[] ids = new long[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-RET-PARTNER");
            p.setName("保留策略测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-RET");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        long[] ret = new long[1];
        ormTemplate.runInSession(session -> {
            ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).newEntity();
            contract.orm_disableAutoStamp(true);
            contract.setCode(code);
            contract.setContractName("保留策略测试合同 " + code);
            contract.setContractType("PURCHASE");
            contract.setContractDirection("INBOUND");
            contract.setPartnerId(ids[0]);
            contract.setCurrencyId(ids[1]);
            contract.setOrgId(1L);
            contract.setStartDate(LocalDate.of(2026, 1, 1));
            contract.setEndDate(LocalDate.of(2026, 12, 31));
            contract.setTotalAmount(new BigDecimal("1000"));
            contract.setStatus(status);
            contract.setBusinessDate(LocalDate.of(2026, 7, 17));
            contract.setCreatedBy("ct-ret-test");
            contract.setUpdatedBy("ct-ret-test");
            contract.setCreateTime(java.sql.Timestamp.valueOf(CtFrozenClockExtension.REFERENCE_DATE.atStartOfDay()));
            contract.setUpdateTime(java.sql.Timestamp.valueOf(CtFrozenClockExtension.REFERENCE_DATE.atStartOfDay()));
            daoProvider.daoFor(ErpCtContract.class).saveEntity(contract);
            ret[0] = contract.getId();
            return null;
        });
        return ret[0];
    }

    private void seedTemplate(Long id, String notificationType, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            ErpSysNotificationTemplate t = daoProvider.daoFor(ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("TPL-" + notificationType);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("文档销毁审计: ${code}");
            t.setBodyTpl("文档 ${code} 已按保留策略销毁（D4 逻辑删除）");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        return ormTemplate.runInSession(session ->
                daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
