package io.nop.app.all.it;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.audit.AuditRequest;
import io.nop.api.core.audit.IAuditService;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.ioc.BeanContainer;
import io.nop.api.core.beans.ApiResponse;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.IGraphQLLogger;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * USC-03 审计生产化「查得到」集成测试（plan 2026-10-01-0723-1，双独立子代理批准）。
 *
 * <p>证明审计写入的生产落点（IAuditService.saveAudit → 异步批 → NopAuthOpLog）在
 * enforcement/config 生产同构形态下经 GraphQL 查询面可查回——补全「只测开了开关不测查得到」的缺口。
 *
 * <p>语义边界（计划登记）：①FIELD_READ_DISCLOSURE 的 MaskHelper 触发层由 module 测试覆盖
 * （fake IAuditService），本测试验证真 IAuditService 持久化与查询面；②patterns 匹配层为平台
 * GraphQLAuditLogger.shouldAudit（StringHelper 简单通配，设计评审枚举 174→179 项覆盖），
 * HTTP 层端到端接线不在断言面——② 经 newRpcContext + 手动 onRpcExecute（平台先例：
 * executeRpc 不经 logger，仅 GraphQLWebService 挂载）。红冲回链回归由 TestErpC13 承载。
 *
 * <p>落库同步纪律（A 路 C-2/B 路）：isAllProcessed() 有 drainTo 竞态误报窗口，必须叠加
 * DB 侧 await 重试（双段同步），禁止单段判定。
 */
@NopTestConfig(localDb = false, enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.auth.graphql.enable-audit", value = "true")
@NopTestProperty(name = "nop.auth.graphql.audit-mutation-patterns", value = "*__approve,*__reverseApprove,*__reverse*,*__cancel*,*__writeOff,*__void*,*__closePeriod,*__reverseClose,*__post*")
public class TestErpAuditProductionPath extends ErpIntegrationTestCase {
    @Inject
    IAuditService auditService;

    @Inject
    IGraphQLLogger graphqlAuditLogger;

    /** ① E4.2 保密字段读披露（FIELD_READ_DISCLOSURE，MaskAuditRecorder 生产形状）落 NopAuthOpLog 可查回。 */
    @Test
    public void testFieldReadDisclosureQueryable() {
        AuditRequest audit = new AuditRequest();
        audit.setActionTime(new Timestamp(System.currentTimeMillis()));
        audit.setOperation("FIELD_READ_DISCLOSURE");
        audit.setDescription("USC-03 集成证明：保密字段明文披露留痕可查");
        audit.setUserName("-");
        audit.setRequestData("{\"entity\":\"ErpHrSalary\",\"field\":\"netSalary\",\"objId\":\"probe\",\"authorizedRole\":\"薪酬审批人\"}");
        auditService.saveAudit(audit);

        awaitProcessed();
        long total = awaitFindTotal("FIELD_READ_DISCLOSURE");
        assertTrue(total >= 1, "FIELD_READ_DISCLOSURE 应落 NopAuthOpLog 且可查回，实际 total=" + total);
    }

    /** ② 高危 mutation 操作留痕（D2 patterns 命中族代表 ErpSalOrder__approve）经 logger 生产路径落库可查回。 */
    @Test
    public void testHighRiskMutationAuditQueryable() {
        IGraphQLExecutionContext ctx = getGraphQLEngine().newRpcContext(
                GraphQLOperationType.mutation, "ErpSalOrder__approve",
                ApiRequest.build(Map.of("id", "1")));
        graphqlAuditLogger.onRpcExecute(ctx, System.currentTimeMillis() - 10, null, null);

        awaitProcessed();
        long total = awaitFindTotal("ErpSalOrder__approve");
        assertTrue(total >= 1, "高危 mutation 操作留痕应落 NopAuthOpLog 且可查回，实际 total=" + total);
    }

    private IGraphQLEngine getGraphQLEngine() {
        return BeanContainer.getBeanByType(IGraphQLEngine.class);
    }

    /** 段 1：异步批排空（isAllProcessed 轮询，存在竞态窗口故仅作前置）。 */
    private void awaitProcessed() {
        for (int i = 0; i < 40; i++) {
            if (auditService.isAllProcessed())
                break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** 段 2：DB 侧 await 重试（双段同步的权威判定，先例 awaitLogByOperation）。 */
    private long awaitFindTotal(String operation) {
        long total = -1;
        for (int i = 0; i < 20; i++) {
            total = findOpLogTotal(operation);
            if (total >= 1)
                return total;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return total;
    }

    private long findOpLogTotal(String operation) {
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.query, "NopAuthOpLog__findPage",
                ApiRequest.build(Map.of("query", Map.of(
                        "offset", 0, "limit", 50,
                        "filter", Map.of("$type", "eq", "name", "operation", "value", operation)))));
        assertTrue(resp.getData() != null, "NopAuthOpLog__findPage 应可达（读路径收口），响应：" + resp);
        Object data = resp.getData();
        Object total;
        if (data instanceof Map) {
            total = ((Map<?, ?>) data).get("total");
        } else {
            try {
                total = data.getClass().getMethod("getTotal").invoke(data);
            } catch (Exception e) {
                throw new IllegalStateException("findPage total 读取失败： " + resp, e);
            }
        }
        return total == null ? -1 : ((Number) total).longValue();
    }
}
