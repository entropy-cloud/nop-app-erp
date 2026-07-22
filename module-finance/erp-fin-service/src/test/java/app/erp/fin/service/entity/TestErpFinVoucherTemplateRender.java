package app.erp.fin.service.entity;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 凭证模板预览 {@code ErpFinVoucherTemplate__renderTemplate} @BizMutation 端到端测试
 * （F16 P1，plan §Phase 2，Phase 0 Explore (b) 候选 (c)）。
 *
 * <p>新建模板 + 2 行（amountKey 路径 + amountExpression 算术路径），调用 renderTemplate 断言：
 * <ul>
 *   <li>行数与录入一致；</li>
 *   <li>debit/credit 按 dcDirection 拆分；</li>
 *   <li>subjectCode/memoTemplate 占位符替换；</li>
 *   <li>amountExpression 算术求值（{@code DOC_TOTAL * 0.13}）与 amountKey 查找均生效。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinVoucherTemplateRender extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @SuppressWarnings("unchecked")
    @Test
    public void testRenderAmountKeyAndArithmetic() {
        // 1. 新建模板（isActive=true + 宽 valid range，确保 findActiveTemplate 命中本模板）
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "F16-PREVIEW");
        headData.put("name", "F16 预览测试模板");
        headData.put("businessType", "PURCHASE_INPUT");
        headData.put("isActive", true);
        headData.put("validFrom", "2020-01-01");
        headData.put("validTo", "2099-12-31");
        ApiResponse<?> head = executeRpc(GraphQLOperationType.mutation, "ErpFinVoucherTemplate__save",
                ApiRequest.build(Map.of("data", headData)));
        assertEquals(0, head.getStatus());
        String headId = String.valueOf(((Map<?, ?>) head.getData()).get("id"));

        // 2. 行 1：DEBIT + amountKey=DOC_TOTAL + subjectCode 占位符 + memo 占位符
        Map<String, Object> line1 = new LinkedHashMap<>();
        line1.put("lineNo", 1);
        line1.put("subjectCode", "${PARTNER}");
        line1.put("dcDirection", "DEBIT");
        line1.put("amountKey", "DOC_TOTAL");
        line1.put("memoTemplate", "采购入库 ${DOC_TOTAL}");
        line1.put("templateId", headId);
        ApiResponse<?> l1 = executeRpc(GraphQLOperationType.mutation, "ErpFinVoucherTemplateLine__save",
                ApiRequest.build(Map.of("data", line1)));
        assertEquals(0, l1.getStatus());

        // 3. 行 2：CREDIT + amountExpression 算术 DOC_TOTAL * 0.13
        Map<String, Object> line2 = new LinkedHashMap<>();
        line2.put("lineNo", 2);
        line2.put("subjectCode", "2202");
        line2.put("dcDirection", "CREDIT");
        line2.put("amountExpression", "DOC_TOTAL * 0.13");
        line2.put("memoTemplate", "应付 13%");
        line2.put("templateId", headId);
        ApiResponse<?> l2 = executeRpc(GraphQLOperationType.mutation, "ErpFinVoucherTemplateLine__save",
                ApiRequest.build(Map.of("data", line2)));
        assertEquals(0, l2.getStatus());

        // 4. 调用 renderTemplate
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("DOC_TOTAL", 1000);
        ctx.put("PARTNER", "P001");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("businessType", "PURCHASE_INPUT");
        args.put("context", ctx);
        ApiResponse<?> rendered = executeRpc(GraphQLOperationType.mutation, "ErpFinVoucherTemplate__renderTemplate",
                ApiRequest.build(args));
        assertEquals(0, rendered.getStatus());

        List<Map<String, Object>> rows = (List<Map<String, Object>>) rendered.getData();
        assertNotNull(rows, "renderTemplate 应返回非空列表");
        assertEquals(2, rows.size(), "应返回 2 行预览");

        Map<String, Object> r1 = rows.get(0);
        assertEquals("P001", r1.get("subjectCode"), "subjectCode 占位符应被替换");
        assertEquals("DEBIT", r1.get("dcDirection"));
        assertEquals(0, new java.math.BigDecimal("1000").compareTo(toBd(r1.get("debitAmount"))), "DEBIT 行借方=DOC_TOTAL");
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(toBd(r1.get("creditAmount"))), "DEBIT 行贷方=0");
        assertEquals("采购入库 1000", r1.get("memo"), "memo 占位符应被替换");

        Map<String, Object> r2 = rows.get(1);
        assertEquals("2202", r2.get("subjectCode"));
        assertEquals("CREDIT", r2.get("dcDirection"));
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(toBd(r2.get("debitAmount"))), "CREDIT 行借方=0");
        assertEquals(0, new java.math.BigDecimal("130").compareTo(toBd(r2.get("creditAmount"))), "CREDIT 行贷方=DOC_TOTAL*0.13=130");
        assertEquals("应付 13%", r2.get("memo"));
    }

    private static java.math.BigDecimal toBd(Object v) {
        return new java.math.BigDecimal(v.toString());
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
