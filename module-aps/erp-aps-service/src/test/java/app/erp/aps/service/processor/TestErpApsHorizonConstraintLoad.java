package app.erp.aps.service.processor;

import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsSchedule;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-CK-aps-014-r3 约束 horizon 过滤回归：单界排产方案按已设界单侧过滤（区间重叠语义），
 * 不再要求双界非空（原任一界为空即整体不过滤 → 全量载入历史停机约束）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsHorizonConstraintLoad extends JunitAutoTestCase {

    private static final String MACHINE = "900";

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testSingleSideHorizonFiltersHistoricalConstraints() {
        // 历史约束：endTime 远早于 horizonStart（历史长期停机，不应侵占未来产能）
        seedConstraint("C-HIST", "2026-07-01T00:00:00", "2026-07-02T00:00:00");
        // 窗口内约束：与 horizon 重叠，应载入
        seedConstraint("C-IN", "2026-07-12T00:00:00", "2026-07-13T00:00:00");

        ErpApsSchedule schedule = new ErpApsSchedule();
        schedule.setCode("S-HZ-1");
        schedule.setName("单界方案");
        schedule.setScheduleDate(java.time.LocalDate.of(2026, 7, 10));
        schedule.setSchedulingMode("FORWARD");
        schedule.setHorizonStart(Timestamp.valueOf(LocalDateTime.parse("2026-07-10T00:00:00")));
        // horizonEnd 置空（单界方案；dao 直写绕过 mandatory 校验模拟存量/直写面）
        schedule.setHorizonEnd(null);
        schedule.setStatus("DRAFT");

        ErpApsSchedulingProcessor processor = new ErpApsSchedulingProcessor();
        processor.daoProvider = daoProvider;

        List<ErpApsConstraint> loaded = processor.loadMaintenanceConstraints(schedule);
        List<String> descriptions = loaded.stream()
                .map(ErpApsConstraint::getDescription).collect(Collectors.toList());

        assertFalse(descriptions.contains("C-HIST"),
                "历史约束（endTime < horizonStart）应被单侧过滤排除，实际载入=" + descriptions);
        assertTrue(descriptions.contains("C-IN"),
                "与 horizon 重叠的约束应载入");
    }

    @Test
    public void testDualSideHorizonKeepsOverlapSemantics() {
        seedConstraint("C-HIST2", "2026-07-01T00:00:00", "2026-07-02T00:00:00");
        seedConstraint("C-IN2", "2026-07-12T00:00:00", "2026-07-13T00:00:00");
        seedConstraint("C-FUT2", "2026-08-01T00:00:00", "2026-08-02T00:00:00");

        ErpApsSchedule schedule = new ErpApsSchedule();
        schedule.setCode("S-HZ-2");
        schedule.setName("双界方案");
        schedule.setScheduleDate(java.time.LocalDate.of(2026, 7, 10));
        schedule.setSchedulingMode("FORWARD");
        schedule.setHorizonStart(Timestamp.valueOf(LocalDateTime.parse("2026-07-10T00:00:00")));
        schedule.setHorizonEnd(Timestamp.valueOf(LocalDateTime.parse("2026-07-20T00:00:00")));
        schedule.setStatus("DRAFT");

        ErpApsSchedulingProcessor processor = new ErpApsSchedulingProcessor();
        processor.daoProvider = daoProvider;

        List<ErpApsConstraint> loaded = processor.loadMaintenanceConstraints(schedule);
        List<String> descriptions = loaded.stream()
                .map(ErpApsConstraint::getDescription).collect(Collectors.toList());

        assertFalse(descriptions.contains("C-HIST2"), "历史约束应排除");
        assertTrue(descriptions.contains("C-IN2"), "窗口内约束应载入");
        assertFalse(descriptions.contains("C-FUT2"), "窗口后约束应排除（区间重叠语义）");
    }

    private void seedConstraint(String description, String start, String end) {
        ErpApsConstraint c = daoProvider.daoFor(ErpApsConstraint.class).newEntity();
        c.setMachineId(MACHINE);
        c.setConstraintType("MAINTENANCE");
        c.setStartTime(Timestamp.valueOf(LocalDateTime.parse(start)));
        c.setEndTime(Timestamp.valueOf(LocalDateTime.parse(end)));
        c.setDescription(description);
        daoProvider.daoFor(ErpApsConstraint.class).saveEntity(c);
    }
}
