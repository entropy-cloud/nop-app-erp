package io.nop.app.all.it;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsSchedule;
import app.erp.aps.service.ErpApsConstants;
import app.erp.mfg.dao.entity.ErpMfgCrpLoad;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B4 C21：APS 排程发布与产能负荷（按 {@code docs/design/integration-testing.md §6 C21} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / WC-001 工作中心 1（含日历 08:00~16:00、产能 eff 1.0）/
 * WO-2026-001 工单 1 弱指针）：自包含工序订单（machineId=WC-001，status=DRAFT 排程候选）save →
 * 排程方案 save → {@code ErpApsSchedule__publish}（PUBLISHED）→
 * {@code ErpMfgReport__renderHtml(reportName="crp-load-report")} 负荷率断言——seed crp_load 行
 * （WC-001 loadDate=2026-07-15 loadHours=4.00 setupHours=1.00）确定性派生
 * capacityHours=8.00（日历 08:00~16:00 = 8h × eff 1.0）/ loadRate=0.50，HTML 含确定性 token
 * （WC-001/8.00/0.50）；随后断言静态行不被重算覆盖（{@code ErpMfgCrpRunJob} config
 * {@code erp-mfg.crp-run-cron} 为空默认不调度，本用例不调用 calculateLoad）。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（排程发布态 PUBLISHED、报表 HTML token、静态 crp_load 行未被覆盖）；
 * 层 2 = 每步 response 快照；层 3 = output/tables 变更行。RECORDING→CHECKING 往返已按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC21ApsCapacityLoad extends ErpIntegrationTestCase {

    static final String OP_CODE = "IT-C21-OP-001";
    static final String SCHEDULE_CODE = "IT-C21-SCH-001";
    static final BigDecimal STATIC_LOAD_HOURS = new BigDecimal("4.00");  // seed crp_load 静态行

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testApsCapacityLoad() {
        // ---------- 1. 自包含工序订单（排程候选，machineId=WC-001） ----------
        ApiResponse<?> opSave = rpcMutation("ErpApsOperationOrder__save", request("1_op_order_save.json5", Map.class));
        output("1_op_order_save_response.json5", opSave);
        assertEquals(0, opSave.getStatus(), "工序订单保存应成功");
        String opOrderId = idOf(opSave);
        addVar("opOrderId", opOrderId);

        // ---------- 2. 排程方案 save → publish（PUBLISHED） ----------
        ApiResponse<?> scheduleSave = rpcMutation("ErpApsSchedule__save", request("2_schedule_save.json5", Map.class));
        output("2_schedule_save_response.json5", scheduleSave);
        assertEquals(0, scheduleSave.getStatus(), "排程方案保存应成功");
        String scheduleId = idOf(scheduleSave);
        addVar("scheduleId", scheduleId);

        ApiResponse<?> publish = rpcMutation("ErpApsSchedule__publish", request("3_schedule_publish.json5", Map.class));
        output("3_schedule_publish_response.json5", publish);
        assertEquals(0, publish.getStatus(), "排程发布应成功");
        // 层 1 锚点：排程发布状态 PUBLISHED
        assertEquals(ErpApsConstants.SCHEDULE_STATUS_PUBLISHED,
                reload(ErpApsSchedule.class, scheduleId).getStatus(), "排程发布后 status=PUBLISHED");
        assertEquals(ErpApsConstants.OP_STATUS_DRAFT,
                reload(ErpApsOperationOrder.class, opOrderId).getStatus(), "工序订单保持 DRAFT（排程候选）");

        // ---------- 3. crp-load 报表渲染：确定性 token 断言 ----------
        ApiResponse<?> report = executeRpc(GraphQLOperationType.query, "ErpMfgReport__renderHtml",
                request("4_report_render.json5", Map.class));
        output("4_report_render_response.json5", report);
        assertEquals(0, report.getStatus(), "crp-load 报表渲染应成功");
        String html = String.valueOf(report.getData());
        // 层 1 锚点：报表 HTML 含 WC-001 / capacityHours 8.00 / loadRate 0.50 确定性 token
        assertTrue(html.contains("WC-001"), "报表 HTML 应含 WC-001");
        assertTrue(html.contains("8.00"), "报表 HTML 应含 capacityHours=8.00 token");
        assertTrue(html.contains("0.50"), "报表 HTML 应含 loadRate=0.50 token");

        // ---------- 4. 静态行不被重算覆盖 ----------
        ErpMfgCrpLoad staticRow = findCrpLoad("1", "2026-07-15");
        assertNotNull(staticRow, "seed crp_load 静态行（WC-001/2026-07-15）应存在");
        assertEquals(0, STATIC_LOAD_HOURS.compareTo(staticRow.getLoadHours()),
                "静态行 loadHours=4.00 未被重算覆盖（CrpRunJob 默认不调度）");
    }

    // ---------- helpers ----------

    private ErpMfgCrpLoad findCrpLoad(String workcenterId, String loadDate) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workcenterId", workcenterId));
        q.addFilter(eq("loadDate", loadDate));
        List<ErpMfgCrpLoad> list = daoProvider.daoFor(ErpMfgCrpLoad.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}