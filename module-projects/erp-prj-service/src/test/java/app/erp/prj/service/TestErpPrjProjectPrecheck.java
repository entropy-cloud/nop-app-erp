package app.erp.prj.service;

import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjTask;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目状态机前置校验端到端集成测试（plan 2026-07-30-0631-1，P1-MA2-067/070）。覆盖：
 * <ul>
 *   <li>P1-MA2-067 {@code closeProject} 任务结束前置（config-gated STRICT/WARN）：
 *       STRICT 模式存在未结束任务抛 {@code ERR_PROJECT_HAS_UNFINISHED_TASKS}；
 *       任务全 DONE 放行；WARN 模式放行。</li>
 *   <li>P1-MA2-070 {@code startProject} 字段前置（config-gated STRICT/WARN）：
 *       STRICT 模式缺必填字段抛 {@code ERR_PROJECT_START_PRECONDITION_FAILED}；
 *       字段完整放行；WARN 模式放行。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjProjectPrecheck extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjProjectBiz projectBiz;

    // ============ P1-MA2-067 closeProject 任务结束前置 ============

    @Test
    public void testCloseProject_strict_unfinishedTasks_throws() {
        Long projectId = ormTemplate.runInSession(session -> {
            Long pid = seedProject("PRJ-UT-STRICT", "未结束任务项目-严格",
                    ErpPrjConstants.PROJECT_STATUS_OPEN, true);
            seedTask(pid, "TODO 任务", ErpPrjConstants.TASK_STATUS_TODO);
            seedTask(pid, "IN_PROGRESS 任务", ErpPrjConstants.TASK_STATUS_IN_PROGRESS);
            seedTask(pid, "BLOCKED 任务", ErpPrjConstants.TASK_STATUS_BLOCKED);
            return pid;
        });

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> projectBiz.closeProject(projectId, CTX)));
        assertEquals(ErpPrjErrors.ERR_PROJECT_HAS_UNFINISHED_TASKS.getErrorCode(), ex.getErrorCode());
        assertEquals(projectId, ex.getParam(ErpPrjErrors.ARG_PROJECT_ID));
    }

    @Test
    public void testCloseProject_allTasksDone_success() {
        Long projectId = ormTemplate.runInSession(session -> {
            Long pid = seedProject("PRJ-DONE-OK", "全完成任务项目",
                    ErpPrjConstants.PROJECT_STATUS_OPEN, true);
            seedTask(pid, "完成 1", ErpPrjConstants.TASK_STATUS_DONE);
            seedTask(pid, "完成 2", ErpPrjConstants.TASK_STATUS_DONE);
            return pid;
        });

        ErpPrjProject closed = ormTemplate.runInSession(session -> projectBiz.closeProject(projectId, CTX));
        assertEquals(ErpPrjConstants.PROJECT_STATUS_COMPLETED, closed.getStatus());
    }

    @Test
    public void testCloseProject_warnMode_allowsUnfinished() {
        System.setProperty(ErpPrjConstants.CONFIG_STRICT_PROJECT_TASK_COMPLETION_CHECK, "false");
        try {
            Long projectId = ormTemplate.runInSession(session -> {
                Long pid = seedProject("PRJ-UT-WARN", "未结束任务项目-WARN",
                        ErpPrjConstants.PROJECT_STATUS_OPEN, true);
                seedTask(pid, "TODO 任务-WARN", ErpPrjConstants.TASK_STATUS_TODO);
                return pid;
            });

            // WARN 模式：仅日志告警，迁移放行
            ErpPrjProject closed = ormTemplate.runInSession(session -> projectBiz.closeProject(projectId, CTX));
            assertEquals(ErpPrjConstants.PROJECT_STATUS_COMPLETED, closed.getStatus());
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_STRICT_PROJECT_TASK_COMPLETION_CHECK);
        }
    }

    // ============ P1-MA2-070 startProject 字段前置 ============

    @Test
    public void testStartProject_strict_missingFields_throws() {
        Long projectId = ormTemplate.runInSession(session ->
                seedProject("PRJ-START-STRICT", "立项缺字段项目",
                        ErpPrjConstants.PROJECT_STATUS_DRAFT, false));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> projectBiz.startProject(projectId, CTX)));
        assertEquals(ErpPrjErrors.ERR_PROJECT_START_PRECONDITION_FAILED.getErrorCode(), ex.getErrorCode());
        assertEquals(projectId, ex.getParam(ErpPrjErrors.ARG_PROJECT_ID));
        // NopException.normalizeValue 将集合参数转为 String（"[startDate, endDate]"）
        String missingFields = String.valueOf(ex.getParam(ErpPrjErrors.ARG_MISSING_FIELDS));
        assertTrue(missingFields.contains("startDate"), "应报告缺失 startDate，实际=" + missingFields);
        assertTrue(missingFields.contains("endDate"), "应报告缺失 endDate，实际=" + missingFields);
    }

    @Test
    public void testStartProject_completeFields_success() {
        Long projectId = ormTemplate.runInSession(session ->
                seedProject("PRJ-START-OK", "立项完整项目",
                        ErpPrjConstants.PROJECT_STATUS_DRAFT, true));

        ErpPrjProject started = ormTemplate.runInSession(session -> projectBiz.startProject(projectId, CTX));
        assertEquals(ErpPrjConstants.PROJECT_STATUS_OPEN, started.getStatus());
    }

    @Test
    public void testStartProject_warnMode_missingFields_allows() {
        System.setProperty(ErpPrjConstants.CONFIG_STRICT_PROJECT_START_PRECHECK, "false");
        try {
            Long projectId = ormTemplate.runInSession(session ->
                    seedProject("PRJ-START-WARN", "立项缺字段项目-WARN",
                            ErpPrjConstants.PROJECT_STATUS_DRAFT, false));

            // WARN 模式：仅日志告警，迁移放行
            ErpPrjProject started = ormTemplate.runInSession(session -> projectBiz.startProject(projectId, CTX));
            assertEquals(ErpPrjConstants.PROJECT_STATUS_OPEN, started.getStatus());
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_STRICT_PROJECT_START_PRECHECK);
        }
    }

    // ---------- seed helpers ----------

    /**
     * @param withDates  true=填充 startDate/endDate/budget（立项完整）；false=留空（缺必填字段）
     */
    private Long seedProject(String code, String name, String status, boolean withDates) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(1L);
        p.setCurrencyId(1L);
        p.setStatus(status);
        p.setActualCost(BigDecimal.ZERO);
        if (withDates) {
            p.setStartDate(LocalDate.of(2026, 1, 1));
            p.setEndDate(LocalDate.of(2026, 12, 31));
            p.setBudget(new BigDecimal("100000"));
        }
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedTask(Long projectId, String title, String status) {
        IEntityDao<ErpPrjTask> dao = daoProvider.daoFor(ErpPrjTask.class);
        ErpPrjTask t = new ErpPrjTask();
        t.setProjectId(projectId);
        t.setTitle(title);
        t.setStatus(status);
        dao.saveEntity(t);
        return t.getId();
    }
}
