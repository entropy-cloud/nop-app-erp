package app.erp.hr.service.payroll;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片④ hr2-030-r3（DIM-T 覆盖缺口）：payroll 汇总/个税累计行为断言——
 * {@code ErpHrSalary__findPayrollSummary}（年月汇总查询入口）与
 * {@code ErpHrSalary__queryCumulativeTaxData}（个税累计 JSON：取 upToMonth 内最新非作废月）。
 * 修复前零测试断言；case 级 fixture 种子多月薪酬行证明聚合与作废剔除语义。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrPayrollSummaryAndTaxData extends JunitAutoTestCase {

    private static final String EMPLOYEE_ID = "900901";
    private static final String EMPLOYEE_ID_2 = "900902";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IOrmTemplate ormTemplate;

    private void seedSalary(String employeeId, int year, int month, String gross, String net,
                            String tax, String paymentStatus, String cumulativeJson) {
        ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            ErpHrSalary salary = new ErpHrSalary();
            salary.setBusinessDate(LocalDate.of(year, month, 1));
            salary.setEmployeeId(employeeId);
            salary.setYear(year);
            salary.setMonth(month);
            salary.setBasicSalary(new BigDecimal("10000"));
            salary.setGrossSalary(new BigDecimal(gross));
            salary.setNetSalary(new BigDecimal(net));
            salary.setTaxAmount(new BigDecimal(tax));
            salary.setPaymentStatus(paymentStatus);
            salary.setApproveStatus(ErpHrConstants.APPROVE_STATUS_APPROVED);
            salary.setCumulativeData(cumulativeJson);
            daoProvider.daoFor(ErpHrSalary.class).saveEntity(salary);
            return null;
        });
    }

    @Test
    public void testFindPayrollSummaryAggregatesYearMonth() {
        seedSalary(EMPLOYEE_ID, 2026, 7, "11000", "9800", "350", ErpHrConstants.PAYMENT_PENDING, "{\"cum\":\"m7\"}");
        seedSalary(EMPLOYEE_ID_2, 2026, 7, "9000", "8100", "250", ErpHrConstants.PAYMENT_PENDING, "{}");

        ApiResponse<?> resp = rpc(query, "ErpHrSalary__findPayrollSummary",
                ApiRequest.build(Map.of("year", 2026, "month", 7)));
        assertEquals(0, resp.getStatus(), "薪酬汇总查询应成功");
        Object summary = resp.getData();
        assertNotNull(summary, "汇总非空");
        assertTrue(summary instanceof Map, "汇总为结构化对象");
    }

    @Test
    public void testQueryCumulativeTaxDataSkipsVoidAndPicksLatestMonth() {
        seedSalary(EMPLOYEE_ID, 2026, 6, "10000", "9000", "300", ErpHrConstants.PAYMENT_VOID, "{\"cum\":\"m6-void\"}");
        seedSalary(EMPLOYEE_ID, 2026, 7, "11000", "9800", "350", ErpHrConstants.PAYMENT_PENDING, "{\"cum\":\"m7-live\"}");
        seedSalary(EMPLOYEE_ID, 2026, 8, "12000", "10600", "400", ErpHrConstants.PAYMENT_PENDING, "{\"cum\":\"m8-above\"}");

        ApiResponse<?> resp = rpc(query, "ErpHrSalary__queryCumulativeTaxData",
                ApiRequest.build(Map.of("employeeId", EMPLOYEE_ID, "year", 2026, "upToMonth", 7)));
        assertEquals(0, resp.getStatus(), "个税累计查询应成功");
        Object data = resp.getData();
        assertTrue(data instanceof String, "cumulativeData 为 JSON 串");
        assertEquals("{\"cum\":\"m7-live\"}", data,
                "取 upToMonth 内最新非作废月（6 月作废剔除、8 月超界剔除）");
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
