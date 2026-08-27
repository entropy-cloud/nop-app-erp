package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.fin.dao.entity.ErpFinApDocumentLog;
import app.erp.fin.service.ErpFinConfigs;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.batch.dsl.runner.IBatchTaskRunner;
import io.nop.core.lang.json.JsonTool;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.job.api.config.LocalJobConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-14 fin 部分（plan 2026-08-28-0219-1 Phase 2）：AP 文档管道异步消费接线覆盖——
 * `fin/ap-document.batch.xml` + `erp-fin-ap-doc-processing.job.yaml` 三件套此前零测试覆盖
 * （接线缺陷实证：原 processor 的 {@code inject('IErpFinApDocumentBiz')} 非注册 bean id，
 * 运行时 unknown-bean-for-name——本测试落地时发现并修正，对齐 bank-recon-auto-reverse
 * 按 bean id 注入范式）。
 *
 * <ul>
 *   <li>batch 任务级执行（经 {@link IBatchTaskRunner#execute}，对齐
 *       {@code TestErpCrmLeadScoringRecalcJob} 范式）：RECEIVED 文档经 batch processor
 *       （processor bean 解析 + {@code processPending} 调用）被真实处理（MANUAL_REVIEW + 轨迹行）；</li>
 *   <li>job yaml 注册形态：`/nop/job/conf/erp-fin-ap-doc-processing.job.yaml` VFS 可见、
 *       按 {@code TestErpAllJobYamlLoading} 同路径解析为 {@link LocalJobConfig}，
 *       invoker（nopBatchTaskRunner.executeAsync）与 params.taskPath 指向 batch 资源。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@io.nop.api.core.annotations.autotest.NopTestProperty(name = "nop.file.store-dir", value = "./target/ap-doc-batch-file-store")
@Timeout(180)
public class TestErpFinApDocBatchWiring extends JunitAutoTestCase {

    private static final String BATCH_PATH = "/nop/batch-task/fin/ap-document.batch.xml";
    private static final String JOB_PATH = "/nop/job/conf/erp-fin-ap-doc-processing.job.yaml";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IBatchTaskRunner batchTaskRunner;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    public void resetConfig() {
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_PIPELINE_ENABLED, "false");
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_UPLOAD_RATE_LIMIT_RPS,
                String.valueOf(ErpFinConfigs.DEFAULT_AP_DOC_UPLOAD_RATE_LIMIT_RPS));
    }

    /** batch 任务级执行：RECEIVED 文档经 nop-batch 接线被真实处理（bean 解析 + processPending + 轨迹）。 */
    @Test
    public void testBatchTaskProcessesReceivedDocument() {
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_PIPELINE_ENABLED, "true");
        // 单上传（限流关零避免默认 10rps 令牌桶拦截）；经 GraphQL 管道上传（事务随 RPC 提交，
        // 供 batch 独立事务可见——直调 biz 会话未提交会被 REQUIRES_NEW 阻塞）
        setConfig(ErpFinConfigs.CONFIG_AP_DOC_UPLOAD_RATE_LIMIT_RPS, "0");
        String content = "收据\n收款单位：接线测试不知名公司\n金额：30.00\n";
        Map<String, Object> uploaded = mutation("ErpFinApDocument__uploadApDocument", Map.of(
                "fileName", "batch-wiring.txt", "mimeType", "text/plain",
                "fileBase64", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));
        String docId = String.valueOf(uploaded.get("id"));
        assertEquals("RECEIVED", uploaded.get("status"));

        batchTaskRunner.execute(BATCH_PATH);

        ErpFinApDocument doc = daoProvider.daoFor(ErpFinApDocument.class).getEntityById(docId);
        assertEquals("MANUAL_REVIEW", doc.getStatus(), "batch 接线应真实处理 RECEIVED 文档（解析→分类→低置信人工门）");
        assertNotNull(doc.getErrorMsg());
        List<String> steps = logSteps(docId);
        assertTrue(steps.contains("PARSE"), "batch 处理应落 PARSE 轨迹");
        assertTrue(steps.contains("MANUAL_REVIEW"), "低置信应落 MANUAL_REVIEW 轨迹");
    }

    /** job yaml 注册形态 + taskPath 指向的 batch 资源存在（对齐 TestErpAllJobYamlLoading 解析路径）。 */
    @Test
    public void testJobYamlWiringPointsToBatchTask() {
        IResource jobResource = VirtualFileSystem.instance().getResource(JOB_PATH);
        assertTrue(jobResource != null && jobResource.exists(), "job.yaml 应经 VFS 可见: " + JOB_PATH);

        LocalJobConfig config = JsonTool.loadDeltaBeanFromResource(jobResource, LocalJobConfig.class);
        assertEquals("erp-fin-ap-doc-processing", config.getJobName());
        assertNotNull(config.getTrigger(), "job.yaml 必须有 trigger");
        assertNotNull(config.getTrigger().getCronExpr(), "job.yaml 必须有 trigger.cronExpr（@cfg 部署键）");
        assertNotNull(config.getInvoker(), "job.yaml 必须有 invoker");
        assertEquals("nopBatchTaskRunner", config.getInvoker().getBean());
        assertEquals("executeAsync", config.getInvoker().getMethod());

        Map<String, Object> params = config.getParams();
        assertNotNull(params, "job.yaml 必须有 params.taskPath");
        assertEquals(BATCH_PATH, String.valueOf(params.get("taskPath")), "taskPath 应指向 ap-document.batch.xml");

        IResource batchResource = VirtualFileSystem.instance().getResource(BATCH_PATH);
        assertTrue(batchResource != null && batchResource.exists(), "taskPath 指向的 batch 资源应经 VFS 可见: " + BATCH_PATH);
    }

    // ---------- helpers ----------

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private Map<String, Object> mutation(String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(GraphQLOperationType.mutation, action,
                ApiRequest.build(args));
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
        return (Map<String, Object>) resp.getData();
    }

    private List<String> logSteps(String docId) {
        IEntityDao<ErpFinApDocumentLog> dao = daoProvider.daoFor(ErpFinApDocumentLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("documentId", docId));
        List<ErpFinApDocumentLog> logs = dao.findAllByQuery(q);
        logs.sort(java.util.Comparator.comparingLong(l -> Long.parseLong(l.getId())));
        return logs.stream().map(ErpFinApDocumentLog::getStep).collect(java.util.stream.Collectors.toList());
    }
}
