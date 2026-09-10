package app.erp.mnt.service.report;

import app.erp.mnt.dao.entity.ErpMntDowntimeEntry;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片④ mnt-020-r3（DIM-T 覆盖缺口）：两报表数据集查询行为断言——
 * {@code ErpMntReport__maintenanceHistoryData}（访问×设备聚合任务数/备件消耗单数）与
 * {@code ErpMntReport__downtimeSummaryData}（equipmentId × reason 聚合停机分钟）。
 * 修复前零测试引用；本测试以 case 级 fixture（种子设备/访问/停机）证明两数据集行结构与聚合语义。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntReportDatasets extends JunitAutoTestCase {

    private static final String EQUIPMENT_ID = "900801";
    private static final LocalDate VISIT_DATE = LocalDate.of(2026, 7, 10);

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private void seedFixtures() {
        ormTemplate.runInSession(session -> {
            ErpMntEquipment equipment = new ErpMntEquipment();
            equipment.setId(EQUIPMENT_ID);
            equipment.setCode("EQ-M28-RPT");
            equipment.setName("M2.8 报表测试设备");
            equipment.setStatus("RUNNING");
            daoProvider.daoFor(ErpMntEquipment.class).saveEntity(equipment);

            ErpMntVisit visit = new ErpMntVisit();
            visit.setId("900802");
            visit.setCode("VIS-M28-RPT");
            visit.setEquipmentId(EQUIPMENT_ID);
            visit.setVisitDate(VISIT_DATE);
            visit.setStatus("COMPLETED");
            visit.setVisitType("REPAIR");
            visit.setTotalMinutes(new BigDecimal("90"));
            daoProvider.daoFor(ErpMntVisit.class).saveEntity(visit);

            ErpMntDowntimeEntry down1 = new ErpMntDowntimeEntry();
            down1.setId("900803");
            down1.setEquipmentId(EQUIPMENT_ID);
            down1.setReason("MECH-FAIL");
            down1.setStartTime(Timestamp.valueOf(LocalDate.of(2026, 7, 9).atTime(8, 0)));
            down1.setTotalMinutes(new BigDecimal("60"));
            daoProvider.daoFor(ErpMntDowntimeEntry.class).saveEntity(down1);

            ErpMntDowntimeEntry down2 = new ErpMntDowntimeEntry();
            down2.setId("900804");
            down2.setEquipmentId(EQUIPMENT_ID);
            down2.setReason("ELEC-FAIL");
            down2.setStartTime(Timestamp.valueOf(LocalDate.of(2026, 7, 9).atTime(10, 0)));
            down2.setTotalMinutes(new BigDecimal("30"));
            daoProvider.daoFor(ErpMntDowntimeEntry.class).saveEntity(down2);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryDataset(String action, Map<String, Object> vars) {
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(GraphQLOperationType.query,
                action, ApiRequest.build(vars)));
        assertEquals(0, resp.getStatus(), action + " 查询应成功");
        Object data = resp.getData();
        assertTrue(data instanceof List, action + " 应返回数据集列表");
        return (List<Map<String, Object>>) data;
    }

    @Test
    public void testMaintenanceHistoryDatasetAggregatesVisits() {
        seedFixtures();
        List<Map<String, Object>> rows = queryDataset("ErpMntReport__maintenanceHistoryData",
                Map.of("equipmentId", EQUIPMENT_ID,
                        "startDate", "2026-07-01", "endDate", "2026-07-31"));
        assertEquals(1, rows.size(), "区间内应聚合出 1 行访问历史");
        Map<String, Object> row = rows.get(0);
        assertEquals("900802", String.valueOf(row.get("visitId")));
        assertEquals(EQUIPMENT_ID, row.get("equipmentId"));
        assertEquals("COMPLETED", row.get("status"));
        assertNotNull(row.get("visitDate"), "visitDate 应在行中");
        assertTrue(((Number) row.get("totalMinutes")).intValue() == 90,
                "访问工时分钟应在行中: " + row.get("totalMinutes"));
    }

    @Test
    public void testDowntimeSummaryDatasetAggregatesByEquipmentAndReason() {
        seedFixtures();
        List<Map<String, Object>> rows = queryDataset("ErpMntReport__downtimeSummaryData",
                Map.of("equipmentId", EQUIPMENT_ID,
                        "startDate", "2026-07-01", "endDate", "2026-07-31"));
        assertEquals(2, rows.size(), "两原因各聚合 1 行（MECH-FAIL/ELEC-FAIL）");
        Map<String, Object> mech = rows.stream()
                .filter(r -> "MECH-FAIL".equals(r.get("reason"))).findFirst().orElse(null);
        Map<String, Object> elec = rows.stream()
                .filter(r -> "ELEC-FAIL".equals(r.get("reason"))).findFirst().orElse(null);
        assertNotNull(mech, "MECH-FAIL 行在位");
        assertNotNull(elec, "ELEC-FAIL 行在位");
        assertEquals(60, ((Number) mech.get("downtimeMinutes")).intValue(), "MECH-FAIL 停机分钟聚合");
        assertEquals(30, ((Number) elec.get("downtimeMinutes")).intValue(), "ELEC-FAIL 停机分钟聚合");
        assertEquals(1, ((Number) mech.get("entryCount")).intValue(), "entryCount 计数");
    }
}
