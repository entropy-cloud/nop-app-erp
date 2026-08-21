package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.job.ErpCsQualityEscalationRetryJob;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * cs 质量事件联动端到端测试（RC-R1.68，P1-RC-057，UC-CS-06 流程①-⑤ + 后置 + 异常；
 * plan 2026-08-18-1849-1 Phase 1 Proof ①-⑨）。
 *
 * <p>覆盖 {@code ErpCsTicketEscalateToQualityProcessor} + {@code ErpCsQualityEscalationRetryJob}：
 * ① IN_PROCESS 成功（NCR data map 全字段断言 + QUALITY_ESCALATE 审计 content=NCR:{code} + 工单状态不变
 * + SLA ESCALATE 语义隔离）② 非 IN_PROCESS 拒绝（错误码 + 零 NCR 零审计）③ materialId/defectDescription
 * 缺失拒绝 ④ quality 服务不可用 → PENDING 审计行 + 工单保持 + 零 NCR ⑤ 重试 job 成功
 * PENDING→NCR:{code} 修正（含既有 NCR 反查幂等）⑥ 重试超限跳过（retry 计数封顶）⑦ cron 空值跳过
 * ⑧ findQualityNcrs 闭环投影 ⑨ GraphQL RPC 真实引擎冒烟（全 mutation/query 经 rpc() 走引擎，
 * qaNcrBiz mock 注册）。
 *
 * <p>断言式测试 + 空 autotest.yaml 标记（镜像 R1.65/R1.67 范式——审计行/NCR 行含真实时钟审计列，
 * 录制表快照会随日期漂移翻红）。冻结时钟 2026-07-17 使 ncrDate 确定性。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsQualityEscalation extends JunitAutoTestCase {

    @RegisterExtension
    static CsFrozenClockExtension frozenClock = new CsFrozenClockExtension();

    static final LocalDate TODAY = LocalDate.of(2026, 7, 17);
    static final String CUSTOMER_ID = "8101";
    static final String TICKET_TYPE_ID = "8201";
    static final String MATERIAL_ID = "8301";
    // bridge-test-114/135: qa 未迁移（M2.3）Long 实体侧局部桥（cs String ↔ qa Long，退役 owner M2.3）
    static final Long MATERIAL_ID_LONG = 8301L;
    static final String SUPPLIER_ID = "8401";
    static final String ASSIGNEE = "cs-qa-handler";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ErpCsQualityEscalationRetryJob retryJob;
    @Inject
    app.erp.qa.biz.IErpQaNonConformanceBiz qaNcrBiz;

    /** 测试容器唯一 IErpQaNonConformanceBiz bean 即 mock（app-test-mock-qa.beans.xml 注册，ioc:type 按接口注册）。 */
    private TestMockQaBizModels.MockErpQaNonConformanceBiz mockNcr() {
        return (TestMockQaBizModels.MockErpQaNonConformanceBiz) qaNcrBiz;
    }

    // ---------- ① 成功：NCR 创建 + QUALITY_ESCALATE 审计 + 工单状态不变 ----------

    @Test
    public void testEscalateToQualitySuccessCreatesNcrAndAudit() {
        String ticketId = seedTicket("TK-QA-OK", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                "ticketId", ticketId,
                "materialId", MATERIAL_ID,
                "defectDescription", "屏幕出现坏点",
                "batchInfo", "B20260801",
                "quantity", new BigDecimal("5"),
                "supplierId", SUPPLIER_ID));
        assertEquals(0, resp.getStatus(), "escalateToQuality 应成功: " + resp);

        // NCR data map 全字段断言（severity 缺省 NORMAL + 批次追加描述）
        Map<String, Object> data = mockNcr().lastSaveData;
        assertNotNull(data, "mock 应捕获 save data map");
        assertEquals("NCR-CS-TK-QA-OK", data.get("code"));
        assertEquals(TODAY, data.get("ncrDate"));
        assertEquals(ErpCsConstants.NCR_SOURCE_TYPE_CS_TICKET, data.get("sourceType"));
        assertEquals("TK-QA-OK", data.get("sourceCode"));
        assertEquals(MATERIAL_ID_LONG, data.get("materialId"));
        assertEquals("屏幕出现坏点；批次：B20260801", data.get("description"));
        assertEquals(new BigDecimal("5"), data.get("quantity"));
        assertEquals("NORMAL", data.get("severity"));
        assertEquals("OPEN", data.get("status"));
        assertEquals(Long.valueOf(8401L), data.get("supplierId"));

        // NCR 落库 + 双弱指针
        ErpQaNonConformance ncr = findNcr("NCR-CS-TK-QA-OK");
        assertNotNull(ncr, "NCR 应落库");
        assertEquals(ErpCsConstants.NCR_SOURCE_TYPE_CS_TICKET, ncr.getSourceType());
        assertEquals("TK-QA-OK", ncr.getSourceCode());

        // QUALITY_ESCALATE 审计行（content=NCR:{code}）；工单不改状态（L1 ④）
        ErpCsTicketAction action = findQualityAction(ticketId);
        assertNotNull(action, "应写 QUALITY_ESCALATE 审计行");
        assertEquals("NCR:NCR-CS-TK-QA-OK", action.getContent());
        assertEquals(ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE, action.getActionType());
        ErpCsTicket ticket = reloadTicket(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, ticket.getStatus(),
                "L1 ④ 工单保持 IN_PROCESS（NCR 流程独立）");

        // 与 SLA ESCALATE 语义隔离：无 ESCALATE 类型审计行
        assertEquals(0, countActions(ticketId, ErpCsConstants.ACTION_TYPE_ESCALATE),
                "质量升级不应产生 SLA ESCALATE 审计");
    }

    // ---------- ② 非 IN_PROCESS 拒绝 ----------

    @Test
    public void testEscalateToQualityRejectsNonInProgress() {
        String ticketId = seedTicket("TK-QA-ST", ErpCsConstants.TICKET_STATUS_NEW);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                "ticketId", ticketId,
                "materialId", MATERIAL_ID,
                "defectDescription", "缺陷"));
        assertEquals(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "非 IN_PROCESS 应拒绝并返回状态迁移错误码: " + resp);
        assertNull(findNcr("NCR-CS-TK-QA-ST"), "零 NCR");
        assertNull(findQualityAction(ticketId), "零审计");
    }

    // ---------- ③ 必填参数缺失拒绝 ----------

    @Test
    public void testEscalateToQualityRejectsMissingParams() {
        String noMaterial = seedTicket("TK-QA-NM", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        String noDesc = seedTicket("TK-QA-ND", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);

        ApiResponse<?> r1 = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                "ticketId", noMaterial,
                "defectDescription", "缺陷"));
        assertEquals(ErpCsErrors.ERR_CS_QUALITY_ESCALATION_PARAM_REQUIRED.getErrorCode(), r1.getCode(),
                "materialId 缺失应拒绝: " + r1);

        ApiResponse<?> r2 = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                "ticketId", noDesc,
                "materialId", MATERIAL_ID,
                "defectDescription", "  "));
        assertEquals(ErpCsErrors.ERR_CS_QUALITY_ESCALATION_PARAM_REQUIRED.getErrorCode(), r2.getCode(),
                "defectDescription 空白应拒绝: " + r2);

        assertNull(findQualityAction(noMaterial), "零审计（materialId 缺失）");
        assertNull(findQualityAction(noDesc), "零审计（描述空白）");
    }

    // ---------- ④ quality 服务不可用 → PENDING 降级（工单保持） ----------

    @Test
    public void testQualityUnavailableDegradesToPendingAudit() {
        String ticketId = seedTicket("TK-QA-PD", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        mockNcr().failSave = true;
        try {
            ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                    "ticketId", ticketId,
                    "materialId", MATERIAL_ID,
                    "defectDescription", "接口降级场景"));
            assertEquals(0, resp.getStatus(),
                    "quality 不可用不应 rethrow（外层 @BizMutation 正常提交，L1 异常条款）: " + resp);
        } finally {
            mockNcr().failSave = false;
        }

        ErpCsTicketAction action = findQualityAction(ticketId);
        assertNotNull(action, "应写 PENDING 审计行（重试队列载体）");
        assertTrue(action.getContent().startsWith("PENDING:"),
                "content 应为 PENDING 前缀载荷: " + action.getContent());
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reloadTicket(ticketId).getStatus(),
                "工单保持 IN_PROCESS（延迟创建 NCR，工单先保留状态）");
        assertEquals(0, countNcrs(), "零 NCR");
    }

    // ---------- ⑤ 重试 job 成功：PENDING → NCR:{code} ----------

    @Test
    public void testRetryJobFixesPendingAction() {
        String ticketId = seedTicket("TK-QA-RT", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        mockNcr().failSave = true;
        rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                "ticketId", ticketId,
                "materialId", MATERIAL_ID,
                "defectDescription", "重试场景"));
        mockNcr().failSave = false;

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "0 0/10 * * * ?");
        try {
            retryJob.execute();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "");
        }

        ErpCsTicketAction action = findQualityAction(ticketId);
        assertNotNull(action);
        assertEquals("NCR:NCR-CS-TK-QA-RT", action.getContent(),
                "PENDING 行应修正为 NCR:{code}: " + action.getContent());
        assertNotNull(findNcr("NCR-CS-TK-QA-RT"), "重试应创建 NCR");

        // 既有 NCR 反查幂等：再次运行不重复创建（content 不变 + NCR 计数不变）
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "0 0/10 * * * ?");
        try {
            retryJob.execute();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "");
        }
        assertEquals(1, countNcrs(), "幂等：不重复创建 NCR");
        assertEquals("NCR:NCR-CS-TK-QA-RT", findQualityAction(ticketId).getContent());
    }

    // ---------- ⑥ 重试超限跳过 ----------

    @Test
    public void testRetryJobSkipsWhenRetryExceeded() {
        String ticketId = seedTicket("TK-QA-MX", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        seedPendingAction(ticketId, "PENDING:{\"materialId\":" + MATERIAL_ID
                + ",\"defectDescription\":\"超限场景\",\"severity\":\"NORMAL\"}#retry=3");

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "0 0/10 * * * ?");
        try {
            retryJob.execute();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_QUALITY_RETRY_CRON, "");
        }

        ErpCsTicketAction action = findQualityAction(ticketId);
        assertNotNull(action);
        assertTrue(action.getContent().startsWith("PENDING:"),
                "重试计数已达上限（默认 3）应跳过不修正: " + action.getContent());
        assertEquals(0, countNcrs(), "超限跳过零 NCR");
    }

    // ---------- ⑦ cron 空值跳过 ----------

    @Test
    public void testRetryJobSkipsWhenCronEmpty() {
        String ticketId = seedTicket("TK-QA-CR", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        seedPendingAction(ticketId, "PENDING:{\"materialId\":" + MATERIAL_ID
                + ",\"defectDescription\":\"cron 空场景\",\"severity\":\"NORMAL\"}");

        retryJob.execute(); // cron 未配置（默认空）= 「不调度」语义

        ErpCsTicketAction action = findQualityAction(ticketId);
        assertNotNull(action);
        assertTrue(action.getContent().startsWith("PENDING:"),
                "cron 空值应跳过扫描（content 不变）: " + action.getContent());
        assertEquals(0, countNcrs());
    }

    // ---------- ⑧ findQualityNcrs 闭环投影（L1 ⑤） ----------

    @Test
    public void testFindQualityNcrsProjectsClosure() {
        String ticketId = seedTicket("TK-QA-FN", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        seedNcr("NCR-CS-TK-QA-FN-1", "TK-QA-FN", "OPEN", null, null);
        seedNcr("NCR-CS-TK-QA-FN-2", "TK-QA-FN", "RESOLVED",
                Timestamp.valueOf(LocalDateTime.of(2026, 7, 18, 10, 0)), "换货处理");

        ApiResponse<?> resp = rpc(query, "ErpCsTicket__findQualityNcrs", args("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "findQualityNcrs 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ncrs = (List<Map<String, Object>>) resp.getData();
        assertEquals(2, ncrs.size(), "弱指针反查应命中 2 条 NCR");

        Map<String, Object> open = findNcrSummary(ncrs, "NCR-CS-TK-QA-FN-1");
        assertEquals("OPEN", open.get("status"));
        assertEquals("NORMAL", open.get("severity"));
        assertNull(open.get("resolvedAt"), "未闭环 NCR 无 resolvedAt");
        assertNull(open.get("resolution"));

        Map<String, Object> resolved = findNcrSummary(ncrs, "NCR-CS-TK-QA-FN-2");
        assertEquals("RESOLVED", resolved.get("status"));
        assertEquals("换货处理", resolved.get("resolution"), "闭环结果可查（L1 ⑤）");
        assertNotNull(resolved.get("resolvedAt"));
    }

    // ---------- ⑨ GraphQL RPC 冒烟：escalateToQuality 失败后再成功（真实引擎路径复验） ----------

    @Test
    public void testGraphqlRpcSmokeEndToEnd() {
        String ticketId = seedTicket("TK-QA-RPC", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        // config 门控关闭 → 拒绝
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_QUALITY_ESCALATION_ENABLED, "false");
        try {
            ApiResponse<?> disabled = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                    "ticketId", ticketId,
                    "materialId", MATERIAL_ID,
                    "defectDescription", "门控场景"));
            assertEquals(ErpCsErrors.ERR_CS_QUALITY_ESCALATION_DISABLED.getErrorCode(), disabled.getCode(),
                    "config 门控关闭应拒绝: " + disabled);
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_QUALITY_ESCALATION_ENABLED, "true");
        }

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__escalateToQuality", args(
                "ticketId", ticketId,
                "materialId", MATERIAL_ID,
                "defectDescription", "RPC 冒烟",
                "severity", "HIGH"));
        assertEquals(0, resp.getStatus(), "RPC 冒烟应成功: " + resp);
        assertNotNull(resp.getData(), "应返回工单实体");
        assertEquals(1, countNcrs());
        assertEquals("HIGH", findNcr("NCR-CS-TK-QA-RPC").getSeverity(),
                "显式 severity 透传（非缺省 NORMAL）");
    }

    // ---------- helpers ----------

    private static Map<String, Object> args(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private String seedTicket(String code, String status) {
        String id = String.valueOf(7600 + Math.abs(code.hashCode()) % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(LocalDate.of(2026, 7, 1));
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_HIGH);
            t.setStatus(status);
            t.setAssignedToId(ASSIGNEE);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            dao.saveEntity(t);
        });
        return id;
    }

    private void seedPendingAction(String ticketId, String content) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketAction> dao = daoProvider.daoFor(ErpCsTicketAction.class);
            ErpCsTicketAction a = new ErpCsTicketAction();
            a.orm_propValueByName("id", String.valueOf(9700 + Math.abs(content.hashCode()) % 200));
            a.setTicketId(ticketId);
            a.setActionType(ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE);
            a.setContent(content);
            dao.saveEntity(a);
        });
    }

    private void seedNcr(String code, String ticketCode, String status, Timestamp resolvedAt, String resolution) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
            ErpQaNonConformance n = new ErpQaNonConformance();
            n.orm_propValueByName("id", 9800L + (long) (Math.abs(code.hashCode()) % 150));
            n.setCode(code);
            n.setNcrDate(LocalDate.of(2026, 7, 17));
            n.setSourceType(ErpCsConstants.NCR_SOURCE_TYPE_CS_TICKET);
            n.setSourceCode(ticketCode);
            n.setMaterialId(MATERIAL_ID_LONG);
            n.setSeverity("NORMAL");
            n.setStatus(status);
            if (resolvedAt != null) {
                n.setResolvedAt(resolvedAt);
            }
            if (resolution != null) {
                n.setResolution(resolution);
            }
            dao.saveEntity(n);
        });
    }

    private ErpCsTicket reloadTicket(String ticketId) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
    }

    private ErpCsTicketAction findQualityAction(String ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE));
        q.setLimit(1);
        List<ErpCsTicketAction> list = daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countActions(String ticketId, String actionType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", actionType));
        return daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q).size();
    }

    private ErpQaNonConformance findNcr(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpQaNonConformance> list = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countNcrs() {
        return daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(new QueryBean()).size();
    }

    private static Map<String, Object> findNcrSummary(List<Map<String, Object>> ncrs, String code) {
        for (Map<String, Object> m : ncrs) {
            if (code.equals(m.get("code"))) {
                return m;
            }
        }
        throw new AssertionError("未找到 NCR 投影: " + code);
    }
}
