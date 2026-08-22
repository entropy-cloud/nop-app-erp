package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntEquipmentCategory;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntSparePartUsage;
import app.erp.mnt.dao.entity.ErpMntTaskTemplate;
import app.erp.mnt.dao.entity.ErpMntTaskTemplateLine;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.dao.entity.ErpMntVisitTask;
import app.erp.mnt.service.support.ScheduleDueGenerator;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * UC-MAIN-01 任务模板套用链测试组（RC-R1.74 / plan 2026-08-19-0445-2 Phase 2）。
 *
 * <p>覆盖：①显式 templateId 套用（VisitTask 行数/描述/标准工时断言，行级缺失回落模板级）
 * ②categoryId 自动匹配回退（唯一 active）③无匹配/多匹配跳过不阻断（visit 仍生成，任务零行）
 * ④标准备件行携带提示字段不产生 SparePartUsage ⑤模板 CRUD 冒烟（GraphQL save/findPage + `_cases/` 快照）
 * ⑥无模板计划零回归（既有 6 基本字段行为）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntTaskTemplate extends JunitAutoTestCase {

    @RegisterExtension
    static MntFrozenClockExtension frozenClock = new MntFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 17);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ScheduleDueGenerator scheduleDueGenerator;

    // ---------- Proof ①：显式 templateId 套用 ----------

    @Test
    public void testExplicitTemplateAppliedToVisit() {
        ormTemplate.runInSession(s -> {
            seedEquipment("63001", null);
            seedTemplate("64001", "TPL-EXPLICIT-001", null, "120", 1);
            seedTemplateLine("65001", "64001", 1, "检查传动皮带", "30", null, null);
            seedTemplateLine("65002", "64001", 2, "更换润滑油", null, null, null);
            seedTimeSchedule("66001", "63001", "64001", LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "到期计划生成 1 条访问");

        ErpMntVisit visit = findVisitByCode("VST-SCH-66001-" + AS_OF_DATE);
        assertNotNull(visit, "访问已生成");
        List<ErpMntVisitTask> tasks = findTasksByVisitId(visit.getId());
        assertEquals(2, tasks.size(), "模板 2 行逐行复制为 2 条任务行");

        tasks.sort(java.util.Comparator.comparing(ErpMntVisitTask::getLineNo));
        assertEquals("检查传动皮带", tasks.get(0).getTaskDescription(), "taskDescription=taskName");
        assertEquals(0, tasks.get(0).getStandardMinutes().compareTo(new BigDecimal("30")),
                "行级标准工时 30 透传");
        assertEquals(ErpMntDaoConstants.VISIT_TASK_STATUS_PENDING, tasks.get(0).getStatus());
        assertEquals("更换润滑油", tasks.get(1).getTaskDescription());
        assertEquals(0, tasks.get(1).getStandardMinutes().compareTo(new BigDecimal("120")),
                "行级缺失回落模板级标准工时 120");
        assertEquals(ErpMntDaoConstants.VISIT_TASK_STATUS_PENDING, tasks.get(1).getStatus());
        assertNull(visit.getTotalMinutes(), "visit.totalMinutes 不预填（执行时长语义）");
    }

    // ---------- Proof ②：categoryId 自动匹配回退（唯一 active） ----------

    @Test
    public void testCategoryFallbackUniqueActiveTemplate() {
        ormTemplate.runInSession(s -> {
            seedCategory("67001", "CAT-FALLBACK");
            seedEquipment("63002", "67001");
            seedTemplate("64002", "TPL-FALLBACK-001", "67001", null, 1);
            seedTemplateLine("65003", "64002", 1, "校准检查", "45", null, null);
            seedTimeSchedule("66002", "63002", null, LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "visit 仍生成（模板回退不阻断）");

        ErpMntVisit visit = findVisitByCode("VST-SCH-66002-" + AS_OF_DATE);
        List<ErpMntVisitTask> tasks = findTasksByVisitId(visit.getId());
        assertEquals(1, tasks.size(), "唯一 active 匹配模板套用");
        assertEquals("校准检查", tasks.get(0).getTaskDescription());
        assertEquals(0, tasks.get(0).getStandardMinutes().compareTo(new BigDecimal("45")),
                "行级缺失回落模板级 45");
    }

    // ---------- Proof ③：无匹配/多匹配跳过不阻断 ----------

    @Test
    public void testNoCategoryMatchSkipsWithoutBlocking() {
        ormTemplate.runInSession(s -> {
            seedCategory("67002", "CAT-EMPTY");
            seedEquipment("63003", "67002");
            seedTimeSchedule("66003", "63003", null, LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "零匹配跳过套用不阻断访问生成");

        ErpMntVisit visit = findVisitByCode("VST-SCH-66003-" + AS_OF_DATE);
        assertNotNull(visit);
        assertEquals(0, findTasksByVisitId(visit.getId()).size(), "任务零行");
    }

    @Test
    public void testMultipleActiveMatchesSkipsWithoutBlocking() {
        ormTemplate.runInSession(s -> {
            seedCategory("67003", "CAT-AMBIG");
            seedEquipment("63004", "67003");
            seedTemplate("64004", "TPL-AMBIG-001", "67003", null, 1);
            seedTemplateLine("65004", "64004", 1, "模糊匹配任务A", "10", null, null);
            seedTemplate("64005", "TPL-AMBIG-002", "67003", null, 1);
            seedTemplateLine("65005", "64005", 1, "模糊匹配任务B", "20", null, null);
            seedTimeSchedule("66004", "63004", null, LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "多匹配跳过套用不阻断访问生成");

        ErpMntVisit visit = findVisitByCode("VST-SCH-66004-" + AS_OF_DATE);
        assertEquals(0, findTasksByVisitId(visit.getId()).size(), "多匹配不猜测，任务零行");
    }

    // ---------- Proof ④：标准备件提示不产生消耗单据 ----------

    @Test
    public void testSparePartHintDoesNotCreateUsage() {
        ormTemplate.runInSession(s -> {
            seedEquipment("63006", null);
            seedTemplate("64006", "TPL-SPARE-001", null, null, 1);
            seedTemplateLine("65006", "64006", 1, "更换滤芯", "15", "8888", "2");
            seedTimeSchedule("66006", "63006", "64006", LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count);

        ErpMntVisit visit = findVisitByCode("VST-SCH-66006-" + AS_OF_DATE);
        List<ErpMntVisitTask> tasks = findTasksByVisitId(visit.getId());
        assertEquals(1, tasks.size(), "备件提示行仍复制为任务行");

        QueryBean q = new QueryBean();
        q.addFilter(eq("equipmentId", "63006"));
        assertEquals(0, daoProvider.daoFor(ErpMntSparePartUsage.class).findAllByQuery(q).size(),
                "标准备件为提示字段，不自动产生 SparePartUsage（实际消耗走既有 confirm 链）");
    }

    // ---------- Proof ⑥：无模板计划零回归 ----------

    @Test
    public void testNoTemplateScheduleZeroRegression() {
        ormTemplate.runInSession(s -> {
            seedEquipment("63007", null);
            seedTimeSchedule("66007", "63007", null, LocalDate.of(2026, 7, 15));
            return null;
        });

        int count = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(AS_OF_DATE, CTX));
        assertEquals(1, count, "无模板计划既有行为不变");

        ErpMntVisit visit = findVisitByCode("VST-SCH-66007-" + AS_OF_DATE);
        assertNotNull(visit);
        assertEquals(LocalDate.of(2026, 7, 15), visit.getVisitDate(), "visitDate=nextDueDate（既有行为）");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT, visit.getStatus());
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_PLANNED, visit.getVisitType());
        assertEquals(0, findTasksByVisitId(visit.getId()).size(), "无模板 → 任务零行");

        ErpMntSchedule managed = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(66007L);
        assertEquals(LocalDate.of(2026, 8, 15), managed.getNextDueDate(), "nextDueDate 推进（既有行为）");
    }

    // ---------- Proof ⑤：模板 CRUD 冒烟 + 快照 ----------

    @EnableSnapshot
    @Test
    public void testTemplateCrudSnapshot() {
        ApiResponse<?> created = executeRpc(mutation, "ErpMntTaskTemplate__save",
                request("1_save.json5", Map.class));
        output("1_save_response.json5", created);
        assertEquals(0, created.getStatus(), "模板 save 成功");

        ApiResponse<?> page = executeRpc(query, "ErpMntTaskTemplate__findPage",
                request("2_findPage.json5", Map.class));
        output("2_findPage_response.json5", page);
        assertEquals(0, page.getStatus(), "模板 findPage 成功");
        assertTrue(page.getData().toString().contains("TPL-SNAP-001"), "可查询到已保存模板");
    }

    // ---------- helpers ----------

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedCategory(String id, String code) {
        IEntityDao<ErpMntEquipmentCategory> dao = daoProvider.daoFor(ErpMntEquipmentCategory.class);
        ErpMntEquipmentCategory category = dao.newEntity();
        category.setId(id);
        category.setCode(code);
        category.setName("分类" + code);
        dao.saveEntity(category);
    }

    private void seedEquipment(String id, String categoryId) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = dao.newEntity();
        equipment.setId(id);
        equipment.setCode("EQ-TPL-" + id);
        equipment.setName("模板测试设备" + id);
        equipment.setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        equipment.setCategoryId(categoryId);
        dao.saveEntity(equipment);
    }

    private void seedTemplate(String id, String code, String categoryId, String standardMinutes, Integer isActive) {
        IEntityDao<ErpMntTaskTemplate> dao = daoProvider.daoFor(ErpMntTaskTemplate.class);
        ErpMntTaskTemplate template = dao.newEntity();
        template.setId(id);
        template.setCode(code);
        template.setName("模板" + code);
        template.setEquipmentCategoryId(categoryId);
        if (standardMinutes != null) {
            template.setStandardMinutes(new BigDecimal(standardMinutes));
        }
        template.setInstruction("按操作手册执行");
        template.setIsActive(isActive);
        dao.saveEntity(template);
    }

    private void seedTemplateLine(String id, String templateId, int lineNo, String taskName,
                                  String standardMinutes, String materialId, String quantity) {
        IEntityDao<ErpMntTaskTemplateLine> dao = daoProvider.daoFor(ErpMntTaskTemplateLine.class);
        ErpMntTaskTemplateLine line = dao.newEntity();
        line.setId(id);
        line.setTemplateId(templateId);
        line.setLineNo(lineNo);
        line.setTaskName(taskName);
        if (standardMinutes != null) {
            line.setStandardMinutes(new BigDecimal(standardMinutes));
        }
        line.setMaterialId(materialId);
        if (quantity != null) {
            line.setQuantity(new BigDecimal(quantity));
        }
        dao.saveEntity(line);
    }

    private void seedTimeSchedule(String id, String equipmentId, String templateId, LocalDate nextDueDate) {
        IEntityDao<ErpMntSchedule> dao = daoProvider.daoFor(ErpMntSchedule.class);
        ErpMntSchedule schedule = dao.newEntity();
        schedule.setId(id);
        schedule.setCode("SCH-TPL-" + id);
        schedule.setName("模板测试计划" + id);
        schedule.setEquipmentId(equipmentId);
        schedule.setScheduleType(ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        schedule.setFrequency(1);
        schedule.setRecurrenceType(ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY);
        schedule.setStartDate(LocalDate.of(2026, 1, 1));
        schedule.setIsActive(1);
        schedule.setNextDueDate(nextDueDate);
        schedule.setTemplateId(templateId);
        dao.saveEntity(schedule);
    }

    private ErpMntVisit findVisitByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpMntVisitTask> findTasksByVisitId(String visitId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("visitId", visitId));
        return daoProvider.daoFor(ErpMntVisitTask.class).findAllByQuery(q);
    }
}
