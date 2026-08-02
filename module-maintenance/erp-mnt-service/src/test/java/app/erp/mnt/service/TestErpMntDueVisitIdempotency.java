package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntSchedule;
import app.erp.mnt.dao.entity.ErpMntVisit;
import app.erp.mnt.service.support.ScheduleDueGenerator;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 到期访问生成幂等测试（plan 2026-07-30-0841-2 R1.28 P1-MA2-086 数据腐败类）。
 *
 * <p>验证 {@link ScheduleDueGenerator#generateDueVisits} 连续两次调用（同 asOfDate、schedule 仍 due）
 * 不产生重复 VST-SCH-{schedId}-{date} 访问行——insert 前经 code existence check 去重。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntDueVisitIdempotency extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final Long EQUIPMENT_ID = 41001L;
    private static final Long SCHEDULE_ID = 42001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ScheduleDueGenerator scheduleDueGenerator;

    @Test
    public void testRepeatGenerateSameDateNoDuplicateVisit() {
        LocalDate asOfDate = LocalDate.of(2026, 7, 15);
        ormTemplate.runInSession(s -> {
            seedEquipment(EQUIPMENT_ID);
            seedSchedule(SCHEDULE_ID, EQUIPMENT_ID, LocalDate.of(2026, 7, 1));
            return null;
        });

        // 首次生成：创建 1 条访问 VST-SCH-{schedId}-2026-07-15
        int first = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(asOfDate, CTX));
        assertEquals(1, first, "首次生成应创建 1 条访问");

        // 重置 nextDueDate 使 schedule 再次 due（模拟重复触发/重试）
        ormTemplate.runInSession(s -> {
            ErpMntSchedule managed = daoProvider.daoFor(ErpMntSchedule.class).getEntityById(SCHEDULE_ID);
            managed.setNextDueDate(LocalDate.of(2026, 7, 1));
            daoProvider.daoFor(ErpMntSchedule.class).updateEntity(managed);
            return null;
        });

        // 再次生成同 asOfDate：existence check 命中已存在访问 → 跳过，无重复
        int second = ormTemplate.runInSession(s -> scheduleDueGenerator.generateDueVisits(asOfDate, CTX));
        assertEquals(0, second, "重复生成同日期应被 existence check 跳过（0 新建）");

        // 仅 1 条访问（无重复实体行）
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", "VST-SCH-" + SCHEDULE_ID + "-" + asOfDate));
        long count = daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q).size();
        assertEquals(1L, count, "同 (schedule, asOfDate) 仅 1 条访问（无重复）");
    }

    private void seedEquipment(Long id) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = new ErpMntEquipment();
        equipment.setId(id);
        equipment.setCode("EQ-IDEM-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
        dao.saveEntity(equipment);
    }

    private void seedSchedule(Long id, Long equipmentId, LocalDate nextDueDate) {
        IEntityDao<ErpMntSchedule> dao = daoProvider.daoFor(ErpMntSchedule.class);
        ErpMntSchedule schedule = new ErpMntSchedule();
        schedule.setId(id);
        schedule.setCode("SCH-IDEM-" + id);
        schedule.setName("计划" + id);
        schedule.setEquipmentId(equipmentId);
        schedule.setScheduleType(ErpMntDaoConstants.SCHEDULE_TYPE_PREVENTIVE);
        schedule.setFrequency(1);
        schedule.setRecurrenceType(ErpMntDaoConstants.RECURRENCE_TYPE_MONTHLY);
        schedule.setStartDate(LocalDate.of(2026, 1, 1));
        schedule.setNextDueDate(nextDueDate);
        schedule.setIsActive(1);
        dao.saveEntity(schedule);
    }
}
