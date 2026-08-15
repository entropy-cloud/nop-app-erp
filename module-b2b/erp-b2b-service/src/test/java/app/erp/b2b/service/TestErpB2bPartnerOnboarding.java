package app.erp.b2b.service;

import app.erp.b2b.biz.IErpB2bEdiDocBiz;
import app.erp.b2b.biz.IErpB2bEdiFormatBiz;
import app.erp.b2b.biz.IErpB2bPartnerProfileBiz;
import app.erp.b2b.dao.entity.ErpB2bCertificationChecklist;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
import app.erp.b2b.dao.entity.ErpB2bEdiFormat;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;
import app.erp.b2b.dao.entity.ErpB2bTestExchange;
import app.erp.b2b.service.job.ErpB2bOnboardingMonitorJob;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B2B 伙伴上线状态机推进测试（RC-R1.36，P1-RC-080，UC-B2B-007）。
 *
 * <p>直断言范式（R1.32/R1.33，拒绝路径不录快照）——GraphQL RPC 实调全部五个状态 mutation +
 * 推进门槛矩阵 + 字段回写断言 + 24h 监控 job 行为。覆盖：
 * <ul>
 *   <li>① 状态守卫：非法迁移拒绝矩阵（REGISTERED/TESTING 跳 PRODUCTION 拒绝、终态再操作拒绝、
 *       已暂停再暂停拒绝）+ 零落库断言 + 错误码断言；</li>
 *   <li>② 推进门槛：基本配置不完整拒绝 / 通过率不达标拒绝 / 关键用例缺失拒绝 / 空清单拒绝 /
 *       必检项未全过拒绝 / 门槛全满足放行；</li>
 *   <li>③ 字段回写：activate 设 goLiveDate + deactivate 设 archivedAt（DAO 落库断言）；</li>
 *   <li>④ 24h 监控 job：超阈值通知落库 / 未超阈值零动作 / 窗口外不扫描 / 单伙伴失败隔离 /
 *       cron 空值跳过 / 无 ACTIVE 模板静默跳过。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bPartnerOnboarding extends JunitAutoTestCase {

    @RegisterExtension
    static B2bFrozenClockExtension frozenClock = new B2bFrozenClockExtension();

    static final Long ORG_ID = 1601L;
    static final LocalDate FROZEN_TODAY = B2bFrozenClockExtension.REFERENCE_DATE; // 2026-07-17
    static final String ADMIN_USER = "b2b-admin-user";

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpB2bPartnerProfileBiz partnerProfileBiz;
    @Inject
    IErpB2bEdiFormatBiz ediFormatBiz;
    @Inject
    IErpB2bEdiDocBiz ediDocBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;

    @BeforeEach
    void setup() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_CRON, "0 0 * * * ?");
    }

    @AfterEach
    void teardown() {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_CRON, "");
        AppConfig.getConfigProvider().assignConfigValue(
                ErpB2bConfigs.CONFIG_ONBOARDING_TEST_PASS_RATE, ErpB2bConfigs.DEFAULT_ONBOARDING_TEST_PASS_RATE);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpB2bConfigs.CONFIG_ONBOARDING_PRODUCTION_MONITOR_HOURS,
                ErpB2bConfigs.DEFAULT_ONBOARDING_PRODUCTION_MONITOR_HOURS);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_FAILURE_RATE,
                ErpB2bConfigs.DEFAULT_ONBOARDING_MONITOR_FAILURE_RATE);
    }

    // ==================== ① 状态守卫（非法迁移拒绝矩阵 + 合法迁移成功） ====================

    @Test
    public void testStateGuardRejectsIllegalTransitions() {
        Long regProfile = seedCompleteProfile(6001L, "P-GUARD-REG", ErpB2bConstants.PARTNER_STATUS_REGISTERED);
        Long testingProfile = seedCompleteProfile(6002L, "P-GUARD-TST", ErpB2bConstants.PARTNER_STATUS_TESTING);
        Long certifiedProfile = seedCompleteProfile(6003L, "P-GUARD-CER", ErpB2bConstants.PARTNER_STATUS_CERTIFIED);
        Long productionProfile = seedCompleteProfile(6004L, "P-GUARD-PRD", ErpB2bConstants.PARTNER_STATUS_PRODUCTION);
        Long suspendedProfile = seedCompleteProfile(6005L, "P-GUARD-SUS", ErpB2bConstants.PARTNER_STATUS_SUSPENDED);
        Long terminatedProfile = seedCompleteProfile(6006L, "P-GUARD-TER", ErpB2bConstants.PARTNER_STATUS_TERMINATED);

        // REGISTERED/TESTING 直跳 PRODUCTION（P1-RC-080 根因）拒绝
        assertRejected(activateRpc(regProfile), "partner-illegal-transition", "不允许执行该操作");
        assertRejected(activateRpc(testingProfile), "partner-illegal-transition", "不允许执行该操作");
        // 非法推进
        assertRejected(promoteToTestingRpc(testingProfile), "partner-illegal-transition", "不允许执行该操作");
        assertRejected(promoteToTestingRpc(certifiedProfile), "partner-illegal-transition", "不允许执行该操作");
        assertRejected(promoteToCertifiedRpc(regProfile), "partner-illegal-transition", "不允许执行该操作");
        assertRejected(promoteToCertifiedRpc(productionProfile), "partner-illegal-transition", "不允许执行该操作");
        // 终态再操作拒绝（deactivate TERMINATED / suspend TERMINATED）
        assertRejected(deactivateRpc(terminatedProfile), "partner-illegal-transition", "不允许执行该操作");
        assertRejected(suspendRpc(terminatedProfile), "partner-illegal-transition", "不允许执行该操作");
        // 已暂停再暂停拒绝
        assertRejected(suspendRpc(suspendedProfile), "partner-illegal-transition", "不允许执行该操作");

        // 拒绝路径零落库：状态全部保持
        assertEquals(ErpB2bConstants.PARTNER_STATUS_REGISTERED, reload(regProfile).getStatus());
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, reload(testingProfile).getStatus());
        assertEquals(ErpB2bConstants.PARTNER_STATUS_CERTIFIED, reload(certifiedProfile).getStatus());
        assertEquals(ErpB2bConstants.PARTNER_STATUS_PRODUCTION, reload(productionProfile).getStatus());
        assertEquals(ErpB2bConstants.PARTNER_STATUS_SUSPENDED, reload(suspendedProfile).getStatus());
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TERMINATED, reload(terminatedProfile).getStatus());
    }

    /** 合法全生命周期：REGISTERED→TESTING→CERTIFIED→PRODUCTION(goLiveDate)→SUSPENDED→TERMINATED(archivedAt)。 */
    @Test
    public void testLegalFullLifecycleWithFieldWriteBack() {
        Long profileId = seedCompleteProfile(6101L, "P-LIFE", ErpB2bConstants.PARTNER_STATUS_REGISTERED);
        seedPassedExchanges(profileId);
        seedAllMandatoryPassedChecklist(profileId);

        assertOk(promoteToTestingRpc(profileId));
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, reload(profileId).getStatus());

        assertOk(promoteToCertifiedRpc(profileId));
        assertEquals(ErpB2bConstants.PARTNER_STATUS_CERTIFIED, reload(profileId).getStatus());

        assertOk(activateRpc(profileId));
        ErpB2bPartnerProfile production = reload(profileId);
        assertEquals(ErpB2bConstants.PARTNER_STATUS_PRODUCTION, production.getStatus());
        assertEquals(FROZEN_TODAY, production.getGoLiveDate(), "activate 应回写 goLiveDate=now");

        assertOk(suspendRpc(profileId));
        assertEquals(ErpB2bConstants.PARTNER_STATUS_SUSPENDED, reload(profileId).getStatus());

        assertOk(deactivateRpc(profileId));
        ErpB2bPartnerProfile terminated = reload(profileId);
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TERMINATED, terminated.getStatus());
        assertNotNull(terminated.getArchivedAt(), "deactivate 应回写 archivedAt=now");
    }

    // ==================== ② 推进门槛 ====================

    @Test
    public void testPromoteToTestingRejectsIncompleteProfile() {
        Long profileId = seedProfile(6201L, "P-INCOMPLETE", ErpB2bConstants.PARTNER_STATUS_REGISTERED,
                partnerId(), "HTTPS", "HMAC", null, "[\"UBL_INVOICE\"]", null);
        assertRejected(promoteToTestingRpc(profileId), "profile-incomplete", "基本配置不完整");
        assertEquals(ErpB2bConstants.PARTNER_STATUS_REGISTERED, reload(profileId).getStatus(),
                "门槛拒绝后状态不变");
    }

    @Test
    public void testPromoteToCertifiedRejectsLowPassRate() {
        Long profileId = seedCompleteProfile(6202L, "P-LOWRATE", ErpB2bConstants.PARTNER_STATUS_TESTING);
        seedTestExchange(6203L, profileId, "TC-001", true);
        seedTestExchange(6204L, profileId, "TC-002", true);
        seedTestExchange(6205L, profileId, "TC-003", false);

        assertRejected(promoteToCertifiedRpc(profileId), "pass-rate-not-met", "测试通过率");
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, reload(profileId).getStatus());
    }

    @Test
    public void testPromoteToCertifiedRejectsMissingKeyCase() {
        Long profileId = seedCompleteProfile(6206L, "P-NOKEY", ErpB2bConstants.PARTNER_STATUS_TESTING);
        seedTestExchange(6207L, profileId, "TC-001", true);
        seedTestExchange(6208L, profileId, "TC-002", true);
        // TC-004 无任何通过记录
        seedAllMandatoryPassedChecklist(profileId);

        assertRejected(promoteToCertifiedRpc(profileId), "pass-rate-not-met", "测试通过率");
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, reload(profileId).getStatus());
    }

    @Test
    public void testPromoteToCertifiedRejectsEmptyChecklist() {
        Long profileId = seedCompleteProfile(6209L, "P-NOLIST", ErpB2bConstants.PARTNER_STATUS_TESTING);
        seedPassedExchanges(profileId);

        assertRejected(promoteToCertifiedRpc(profileId), "certification-not-met", "认证清单");
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, reload(profileId).getStatus());
    }

    @Test
    public void testPromoteToCertifiedRejectsUnpassedMandatoryItem() {
        Long profileId = seedCompleteProfile(6210L, "P-NOTPASS", ErpB2bConstants.PARTNER_STATUS_TESTING);
        seedPassedExchanges(profileId);
        seedChecklist(6211L, profileId, "传输连接测试通过", true, true);
        seedChecklist(6212L, profileId, "发票格式正确", true, false);
        seedChecklist(6213L, profileId, "ASN 格式正确", false, false);

        assertRejected(promoteToCertifiedRpc(profileId), "certification-not-met", "认证清单");
        assertEquals(ErpB2bConstants.PARTNER_STATUS_TESTING, reload(profileId).getStatus());
    }

    @Test
    public void testPromoteToCertifiedSucceedsWhenGatesMet() {
        Long profileId = seedCompleteProfile(6214L, "P-GATES", ErpB2bConstants.PARTNER_STATUS_TESTING);
        seedPassedExchanges(profileId);
        seedAllMandatoryPassedChecklist(profileId);

        assertOk(promoteToCertifiedRpc(profileId));
        assertEquals(ErpB2bConstants.PARTNER_STATUS_CERTIFIED, reload(profileId).getStatus());
    }

    // ==================== ④ 24h 上线监控 job ====================

    @Test
    public void testMonitorJobAlertsWhenFailureRateAboveThreshold() {
        seedAlertTemplate(8801L);
        seedFormat(8901L, "UBL_INVOICE");
        Long profileId = seedProfile(8902L, "P-MON-ALERT", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock.endpoint/webhook", "[\"UBL_INVOICE\"]", FROZEN_TODAY);
        seedEdiDoc(8903L, 8901L, profileId, ErpB2bConstants.EDI_DOC_STATE_SENT, FROZEN_TODAY.atTime(9, 0));
        seedEdiDoc(8904L, 8901L, profileId, ErpB2bConstants.EDI_DOC_STATE_SENT, FROZEN_TODAY.atTime(10, 0));
        seedEdiDoc(8905L, 8901L, profileId, ErpB2bConstants.EDI_DOC_STATE_ERROR, FROZEN_TODAY.atTime(11, 0));

        newWiredJob().execute();

        // 2/3 成功，1 ERROR → 失败率 33.3% > 5% → 告警
        List<ErpSysNotification> notifications = notificationsOf(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT);
        assertEquals(1, notifications.size(), "超阈值应派发 1 条告警通知");
        assertEquals(ADMIN_USER, notifications.get(0).getRecipientUserId());
    }

    @Test
    public void testMonitorJobSilentWhenBelowThresholdAndOutsideWindow() {
        seedAlertTemplate(8802L);
        seedFormat(8906L, "UBL_INVOICE");
        // 窗口内但失败率未超阈值：1 ERROR / 30 总件 = 3.3% < 5%
        Long inWindow = seedProfile(8907L, "P-MON-LOW", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock.endpoint/webhook", "[\"UBL_INVOICE\"]", FROZEN_TODAY);
        for (int i = 0; i < 29; i++) {
            seedEdiDoc(9000L + i, 8906L, inWindow, ErpB2bConstants.EDI_DOC_STATE_SENT, FROZEN_TODAY.atTime(9, 0));
        }
        seedEdiDoc(9090L, 8906L, inWindow, ErpB2bConstants.EDI_DOC_STATE_ERROR, FROZEN_TODAY.atTime(10, 0));
        // 窗口外（goLiveDate 早于窗口起点）：即使高失败率也不扫描。
        // 独立格式（UBL_DESPATCH_ADVICE）防跨伙伴泄漏（D4 锚点近似：同 org+同格式伙伴的
        // 事务会互相计入——测试用独立格式隔离，窗口排除语义独立验证）
        seedFormat(8911L, "UBL_DESPATCH_ADVICE");
        Long outsideWindow = seedProfile(8908L, "P-MON-OUT", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock.endpoint/webhook", "[\"UBL_DESPATCH_ADVICE\"]",
                FROZEN_TODAY.minusDays(3));
        seedEdiDoc(9091L, 8911L, outsideWindow, ErpB2bConstants.EDI_DOC_STATE_ERROR,
                FROZEN_TODAY.minusDays(3).atTime(9, 0));

        newWiredJob().execute();

        assertTrue(notificationsOf(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT).isEmpty(),
                "未超阈值 + 窗口外伙伴均不应派发告警");
    }

    @Test
    public void testMonitorJobPerPartnerFailureIsolation() {
        seedAlertTemplate(8803L);
        seedFormat(8910L, "UBL_INVOICE");
        // 坏数据伙伴：allowedFormats 非法 JSON → 解析失败 WARN 隔离，不阻断正常伙伴
        Long badPartner = seedProfile(8911L, "P-MON-BAD", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock/endpoint", "NOT-JSON", FROZEN_TODAY);
        seedEdiDoc(8912L, 8910L, badPartner, ErpB2bConstants.EDI_DOC_STATE_ERROR, FROZEN_TODAY.atTime(9, 0));
        // 正常伙伴：超阈值
        Long goodPartner = seedProfile(8913L, "P-MON-GOOD", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock.endpoint/webhook", "[\"UBL_INVOICE\"]", FROZEN_TODAY);
        seedEdiDoc(8914L, 8910L, goodPartner, ErpB2bConstants.EDI_DOC_STATE_ERROR, FROZEN_TODAY.atTime(10, 0));

        newWiredJob().execute();

        List<ErpSysNotification> notifications = notificationsOf(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT);
        assertEquals(1, notifications.size(), "坏伙伴失败隔离，正常伙伴告警仍派发");
        ErpSysNotification alert = notifications.get(0);
        assertTrue(String.valueOf(alert.getSubject()).contains("P-MON-GOOD"),
                "告警标题应渲染正常伙伴编码: " + alert.getSubject());
        assertTrue(String.valueOf(alert.getBody()).contains("P-MON-GOOD"),
                "告警正文应渲染正常伙伴编码: " + alert.getBody());
    }

    @Test
    public void testMonitorJobCronEmptySkips() {
        seedAlertTemplate(8804L);
        seedFormat(8915L, "UBL_INVOICE");
        Long profileId = seedProfile(8916L, "P-MON-SKIP", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock.endpoint/webhook", "[\"UBL_INVOICE\"]", FROZEN_TODAY);
        seedEdiDoc(8917L, 8915L, profileId, ErpB2bConstants.EDI_DOC_STATE_ERROR, FROZEN_TODAY.atTime(9, 0));

        AppConfig.getConfigProvider().assignConfigValue(ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_CRON, "");
        try {
            newWiredJob().execute();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpB2bConfigs.CONFIG_ONBOARDING_MONITOR_CRON, "0 0 * * * ?");
        }
        assertTrue(notificationsOf(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT).isEmpty(),
                "cron 空值应跳过扫描（不派发告警）");
    }

    @Test
    public void testMonitorJobSilentlySkipsWhenNoActiveTemplate() {
        seedFormat(8918L, "UBL_INVOICE");
        Long profileId = seedProfile(8919L, "P-MON-NOTPL", ErpB2bConstants.PARTNER_STATUS_PRODUCTION,
                partnerId(), "HTTPS", "HMAC", "https://mock.endpoint/webhook", "[\"UBL_INVOICE\"]", FROZEN_TODAY);
        seedEdiDoc(8920L, 8918L, profileId, ErpB2bConstants.EDI_DOC_STATE_ERROR, FROZEN_TODAY.atTime(9, 0));

        // 无模板 seed：notify 静默跳过（R1.4 范式），job 不抛错
        newWiredJob().execute();

        assertTrue(notificationsOf(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT).isEmpty(),
                "无 ACTIVE 模板应静默跳过");
    }

    // ==================== helpers ====================

    private ErpB2bOnboardingMonitorJob newWiredJob() {
        ErpB2bOnboardingMonitorJob job = new ErpB2bOnboardingMonitorJob();
        job.setPartnerProfileBiz(partnerProfileBiz);
        job.setEdiFormatBiz(ediFormatBiz);
        job.setEdiDocBiz(ediDocBiz);
        job.setNotificationBiz(notificationBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    private ErpB2bPartnerProfile reload(Long profileId) {
        return daoProvider.daoFor(ErpB2bPartnerProfile.class).getEntityById(profileId);
    }

    private Long partnerId() {
        ErpMdPartner partner = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        partner.setCode("MD-P-" + System.nanoTime());
        partner.setName("测试伙伴");
        partner.setPartnerType("VENDOR");
        partner.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(partner);
        return partner.getId();
    }

    private Long seedProfile(Long id, String code, String status, Long partnerId, String protocol,
                             String authMethod, String transportEndpoint, String allowedFormats,
                             LocalDate goLiveDate) {
        ErpB2bPartnerProfile profile = daoProvider.daoFor(ErpB2bPartnerProfile.class).newEntity();
        profile.setId(id);
        profile.setCode(code);
        profile.setOrgId(ORG_ID);
        profile.setPartnerName("伙伴-" + code);
        profile.setStatus(status);
        profile.setPartnerId(partnerId);
        profile.setProtocol(protocol);
        profile.setAuthMethod(authMethod);
        profile.setTransportEndpoint(transportEndpoint);
        profile.setAllowedFormats(allowedFormats);
        profile.setGoLiveDate(goLiveDate);
        daoProvider.daoFor(ErpB2bPartnerProfile.class).saveEntity(profile);
        return id;
    }

    private Long seedCompleteProfile(Long id, String code, String status) {
        return seedProfile(id, code, status, partnerId(), "HTTPS", "HMAC",
                "https://mock.endpoint/webhook", "[\"UBL_INVOICE\"]", null);
    }

    private void seedPassedExchanges(Long profileId) {
        seedTestExchange(7001L, profileId, "TC-001", true);
        seedTestExchange(7002L, profileId, "TC-002", true);
        seedTestExchange(7003L, profileId, "TC-003", true);
        seedTestExchange(7004L, profileId, "TC-004", true);
    }

    private void seedTestExchange(Long id, Long profileId, String testCaseCode, boolean passed) {
        ErpB2bTestExchange exchange = daoProvider.daoFor(ErpB2bTestExchange.class).newEntity();
        exchange.setId(id);
        exchange.setPartnerProfileId(profileId);
        exchange.setDirection(ErpB2bConstants.DIRECTION_OUTBOUND);
        exchange.setFormatCode("UBL_INVOICE");
        exchange.setTestCaseCode(testCaseCode);
        exchange.setPassed(passed);
        daoProvider.daoFor(ErpB2bTestExchange.class).saveEntity(exchange);
    }

    private void seedChecklist(Long id, Long profileId, String item, boolean mandatory, boolean passed) {
        ErpB2bCertificationChecklist checklist = daoProvider.daoFor(ErpB2bCertificationChecklist.class).newEntity();
        checklist.setId(id);
        checklist.setPartnerProfileId(profileId);
        checklist.setChecklistItem(item);
        checklist.setIsMandatory(mandatory);
        checklist.setIsPassed(passed);
        daoProvider.daoFor(ErpB2bCertificationChecklist.class).saveEntity(checklist);
    }

    private void seedAllMandatoryPassedChecklist(Long profileId) {
        seedChecklist(7101L, profileId, "传输连接测试通过", true, true);
        seedChecklist(7102L, profileId, "入站端点可达", true, true);
        seedChecklist(7103L, profileId, "证书未过期", true, true);
    }

    private void seedFormat(Long id, String code) {
        ErpB2bEdiFormat format = daoProvider.daoFor(ErpB2bEdiFormat.class).newEntity();
        format.setId(id);
        format.setCode(code);
        format.setOrgId(ORG_ID);
        format.setFormatName("测试格式-" + code);
        format.setFormatStandard("UBL");
        format.setDirection(ErpB2bConstants.DIRECTION_OUTBOUND);
        format.setNeedsWebService(0);
        format.setIsActive(1);
        daoProvider.daoFor(ErpB2bEdiFormat.class).saveEntity(format);
    }

    private void seedEdiDoc(Long id, Long formatId, Long profileId, String state, LocalDateTime createTime) {
        ErpB2bEdiDoc doc = daoProvider.daoFor(ErpB2bEdiDoc.class).newEntity();
        doc.setId(id);
        doc.setCode("EDI-MON-" + id);
        doc.setOrgId(ORG_ID);
        doc.setFormatId(formatId);
        doc.setRelatedBillType(ErpB2bConstants.RELATED_BILL_TYPE_SALES_ORDER);
        doc.setRelatedBillCode("SO-MON-" + id);
        doc.setState(state);
        doc.setBlockingLevel(ErpB2bConstants.BLOCKING_LEVEL_INFO);
        doc.setRetryCount(0);
        doc.setCreateTime(Timestamp.valueOf(createTime));
        doc.setBusinessDate(createTime.toLocalDate());
        daoProvider.daoFor(ErpB2bEdiDoc.class).saveEntity(doc);
    }

    private void seedAlertTemplate(Long id) {
        ormTemplate.runInSession(() -> {
            ErpSysNotificationTemplate t = daoProvider.daoFor(ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT);
            t.setName("TPL-ONBOARDING-MONITOR-ALERT");
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("伙伴上线监控告警: ${partnerCode}");
            t.setBodyTpl("伙伴 ${partnerCode} 上线监控窗口内 EDI 失败率 ${failureRate}（${errorCount}/${totalCount}），请核查");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"" + ADMIN_USER + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String notificationType) {
        IEntityDao<ErpSysNotification> dao = daoProvider.daoFor(ErpSysNotification.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", notificationType));
        return dao.findAllByQuery(q);
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> promoteToTestingRpc(Long profileId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpB2bPartnerProfile__promoteToTesting",
                ApiRequest.build(Map.of("profileId", profileId)));
    }

    private ApiResponse<?> promoteToCertifiedRpc(Long profileId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpB2bPartnerProfile__promoteToCertified",
                ApiRequest.build(Map.of("profileId", profileId)));
    }

    private ApiResponse<?> activateRpc(Long profileId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpB2bPartnerProfile__activate",
                ApiRequest.build(Map.of("profileId", profileId)));
    }

    private ApiResponse<?> suspendRpc(Long profileId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpB2bPartnerProfile__suspend",
                ApiRequest.build(Map.of("profileId", profileId)));
    }

    private ApiResponse<?> deactivateRpc(Long profileId) {
        return executeRpc(GraphQLOperationType.mutation, "ErpB2bPartnerProfile__deactivate",
                ApiRequest.build(Map.of("profileId", profileId)));
    }

    private void assertOk(ApiResponse<?> resp) {
        assertEquals(0, resp.getStatus(), "应成功: " + resp);
    }

    private void assertRejected(ApiResponse<?> resp, String errorCodeTail, String messageFragment) {
        assertNotEquals(0, resp.getStatus(), "应被拒绝: " + resp);
        String body = String.valueOf(resp);
        assertTrue(body.contains(errorCodeTail), "拒绝应携带错误码 " + errorCodeTail + ": " + body);
        assertTrue(body.contains(messageFragment), "拒绝应携带中文消息片段: " + body);
    }
}
