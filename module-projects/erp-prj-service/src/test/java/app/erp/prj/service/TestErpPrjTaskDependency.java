package app.erp.prj.service;

import app.erp.prj.biz.IErpPrjTaskBiz;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务依赖与状态机端到端集成测试（task-dag.md）。覆盖：
 * <ol>
 *   <li>保存自环依赖 → ERR_TASK_SELF_DEPENDENCY。</li>
 *   <li>成环保存 → ERR_TASK_DEPENDENCY_CYCLE。</li>
 *   <li>跨项目保存 → ERR_TASK_DEPENDENCY_CROSS_PROJECT。</li>
 *   <li>startTask 前置未完成 → ERR_TASK_PREDECESSOR_NOT_DONE（STRICT）；WARN 模式放行。</li>
 *   <li>startTask happy path（前置 DONE）→ IN_PROGRESS。</li>
 *   <li>非法迁移 → ERR_TASK_ILLEGAL_STATUS_TRANSITION；blockReason 缺失 → ERR_TASK_BLOCK_REASON_REQUIRED。</li>
 *   <li>findPredecessors / findSuccessors / getDependencyChain。</li>
 *   <li>a. 链头自环优先（SELF_DEPENDENCY 优先于深度判定）。</li>
 *   <li>b. 长链深度超限 → ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjTaskDependency extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjTaskBiz taskBiz;

    // ============ 场景 1：自环依赖 ============

    @Test
    public void scenario1_saveSelfDependency() {
        Long projectId = seedProject();
        Long taskId = seedTask(projectId, "自环任务", ErpPrjConstants.TASK_STATUS_TODO, null);

        // 经 biz.update 触发 defaultPrepareUpdate 钩子；dependsOnId=自身 → SELF_DEPENDENCY
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(taskId));
        data.put("dependsOnId", String.valueOf(taskId));

        NopException ex = assertThrows(NopException.class, () -> taskBiz.update(data, CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_SELF_DEPENDENCY.getErrorCode(), ex.getErrorCode());
    }

    // ============ 场景 2：成环 ============

    @Test
    public void scenario2_saveCycleDependency() {
        Long projectId = seedProject();
        Long bId = seedTask(projectId, "前置-B", ErpPrjConstants.TASK_STATUS_DONE, null);
        Long aId = seedTask(projectId, "后继-A", ErpPrjConstants.TASK_STATUS_TODO, bId); // A→B，无环

        // 更新 B.dependsOnId=A → 形成 A→B→A 环
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(bId));
        data.put("dependsOnId", String.valueOf(aId));

        NopException ex = assertThrows(NopException.class, () -> taskBiz.update(data, CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_DEPENDENCY_CYCLE.getErrorCode(), ex.getErrorCode());
    }

    // ============ 场景 3：跨项目 ============

    @Test
    public void scenario3_saveCrossProjectDependency() {
        Long p1 = seedProject();
        Long p2 = seedProject();
        Long aId = seedTask(p1, "项目1-任务A", ErpPrjConstants.TASK_STATUS_DONE, null);

        // 新建任务属于项目 2，依赖项目 1 的任务 A → 跨项目
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "项目2-任务X");
        data.put("status", ErpPrjConstants.TASK_STATUS_TODO);
        data.put("projectId", String.valueOf(p2));
        data.put("dependsOnId", String.valueOf(aId));

        NopException ex = assertThrows(NopException.class, () -> taskBiz.save(data, CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_DEPENDENCY_CROSS_PROJECT.getErrorCode(), ex.getErrorCode());
    }

    // ============ 场景 4：startTask 前置未完成 ============

    @Test
    public void scenario4_startTask_predecessorNotDone_strict() {
        System.setProperty(ErpPrjConstants.CONFIG_TASK_STRICT_PREDECESSOR_CHECK, "true");
        try {
            Long projectId = seedProject();
            Long bId = seedTask(projectId, "前置-B-未完成", ErpPrjConstants.TASK_STATUS_IN_PROGRESS, null);
            Long aId = seedTask(projectId, "后继-A", ErpPrjConstants.TASK_STATUS_TODO, bId);

            NopException ex = assertThrows(NopException.class, () -> taskBiz.startTask(aId, CTX));
            assertEquals(ErpPrjErrors.ERR_TASK_PREDECESSOR_NOT_DONE.getErrorCode(), ex.getErrorCode());
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_TASK_STRICT_PREDECESSOR_CHECK);
        }
    }

    @Test
    public void scenario4_startTask_predecessorNotDone_warn() {
        System.setProperty(ErpPrjConstants.CONFIG_TASK_STRICT_PREDECESSOR_CHECK, "false");
        try {
            Long projectId = seedProject();
            Long bId = seedTask(projectId, "前置-B-WARN", ErpPrjConstants.TASK_STATUS_IN_PROGRESS, null);
            Long aId = seedTask(projectId, "后继-A-WARN", ErpPrjConstants.TASK_STATUS_TODO, bId);

            // WARN 模式：仅日志告警，迁移放行
            ErpPrjTask started = taskBiz.startTask(aId, CTX);
            assertEquals(ErpPrjConstants.TASK_STATUS_IN_PROGRESS, started.getStatus());
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_TASK_STRICT_PREDECESSOR_CHECK);
        }
    }

    // ============ 场景 5：startTask happy path ============

    @Test
    public void scenario5_startTask_happyPath() {
        Long projectId = seedProject();
        Long bId = seedTask(projectId, "前置-B-完成", ErpPrjConstants.TASK_STATUS_DONE, null);
        Long aId = seedTask(projectId, "后继-A", ErpPrjConstants.TASK_STATUS_TODO, bId);

        ErpPrjTask started = taskBiz.startTask(aId, CTX);
        assertEquals(ErpPrjConstants.TASK_STATUS_IN_PROGRESS, started.getStatus());
    }

    // ============ 场景 6：非法迁移 ============

    @Test
    public void scenario6_illegalTransition() {
        Long projectId = seedProject();
        Long taskId = seedTask(projectId, "TODO-任务", ErpPrjConstants.TASK_STATUS_TODO, null);

        // TODO 直接 completeTask → 非法（须先 IN_PROGRESS）
        NopException ex1 = assertThrows(NopException.class, () -> taskBiz.completeTask(taskId, CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex1.getErrorCode());

        // 进入 BLOCKED 后直接 completeTask → 非法
        Long progressId = seedTask(projectId, "IN_PROGRESS-任务", ErpPrjConstants.TASK_STATUS_IN_PROGRESS, null);
        ErpPrjTask blocked = taskBiz.blockTask(progressId, "等待外部依赖", CTX);
        assertEquals(ErpPrjConstants.TASK_STATUS_BLOCKED, blocked.getStatus());

        NopException ex2 = assertThrows(NopException.class, () -> taskBiz.completeTask(blocked.getId(), CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex2.getErrorCode());
    }

    @Test
    public void scenario6_blockReasonRequired() {
        Long projectId = seedProject();
        Long taskId = seedTask(projectId, "待阻塞", ErpPrjConstants.TASK_STATUS_IN_PROGRESS, null);

        NopException ex = assertThrows(NopException.class, () -> taskBiz.blockTask(taskId, "", CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_BLOCK_REASON_REQUIRED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void scenario6_stateMachineRoundTrip() {
        Long projectId = seedProject();
        Long taskId = seedTask(projectId, "全链路", ErpPrjConstants.TASK_STATUS_TODO, null);

        ErpPrjTask started = taskBiz.startTask(taskId, CTX);
        assertEquals(ErpPrjConstants.TASK_STATUS_IN_PROGRESS, started.getStatus());

        ErpPrjTask blocked = taskBiz.blockTask(taskId, "缺人", CTX);
        assertEquals(ErpPrjConstants.TASK_STATUS_BLOCKED, blocked.getStatus());

        ErpPrjTask unblocked = taskBiz.unblockTask(taskId, CTX);
        assertEquals(ErpPrjConstants.TASK_STATUS_IN_PROGRESS, unblocked.getStatus());

        ErpPrjTask done = taskBiz.completeTask(taskId, CTX);
        assertEquals(ErpPrjConstants.TASK_STATUS_DONE, done.getStatus());
    }

    // ============ 场景 7：findPredecessors / findSuccessors ============

    @Test
    public void scenario7_findPredecessorsAndSuccessors() {
        Long projectId = seedProject();
        // 链 A→B→C：A.dependsOnId=B, B.dependsOnId=C
        Long cId = seedTask(projectId, "C", ErpPrjConstants.TASK_STATUS_DONE, null);
        Long bId = seedTask(projectId, "B", ErpPrjConstants.TASK_STATUS_DONE, cId);
        Long aId = seedTask(projectId, "A", ErpPrjConstants.TASK_STATUS_TODO, bId);
        // D 反向引用 A：D.dependsOnId=A
        Long dId = seedTask(projectId, "D", ErpPrjConstants.TASK_STATUS_TODO, aId);

        // findPredecessors(A)=[B,C]：A 的前置是 B，B 的前置是 C → 上行链全量 [B,C]
        List<ErpPrjTask> preds = taskBiz.findPredecessors(aId, CTX);
        assertEquals(2, preds.size(), "A 的上行链应含 B、C");
        assertTrue(containsId(preds, bId), "应含直接前置 B");
        assertTrue(containsId(preds, cId), "应含间接前置 C");

        // findSuccessors(C)=[B,A,D]：C 的后继是 B，B 的后继是 A，A 的后继是 D → 下行反查全量 3 个
        List<ErpPrjTask> succs = taskBiz.findSuccessors(cId, CTX);
        assertEquals(3, succs.size(), "C 的下行链应含 B、A、D（A 的后继 D 也经传递含入）");
        assertTrue(containsId(succs, bId), "应含直接后继 B");
        assertTrue(containsId(succs, aId), "应含间接后继 A");
        assertTrue(containsId(succs, dId), "应含传递后继 D（D.dependsOnId=A → D 是 A 的后继 → 经传递归入 C 的下行链）");

        // getDependencyChain(A) 与 findPredecessors(A) 在单前置模型下结构一致
        List<ErpPrjTask> chain = taskBiz.getDependencyChain(aId, CTX);
        assertEquals(2, chain.size(), "getDependencyChain 应与 findPredecessors 一致");
    }

    // ============ 场景 8a：链头自环优先（SELF_DEPENDENCY 优先于深度判定） ============

    @Test
    public void scenario8a_headSelfLoopFirst() {
        Long projectId = seedProject();
        // 构造 101 节点长链 A1→A2→...→A101（A1.dependsOnId=A2，...）
        Long[] chain = seedLongChain(projectId, 101);

        // 在链头 A1 上设置自环 A1.dependsOnId=A1 → SELF_DEPENDENCY 优先（在 detectCycle 第一步即被检测）
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(chain[0]));
        data.put("dependsOnId", String.valueOf(chain[0]));

        NopException ex = assertThrows(NopException.class, () -> taskBiz.update(data, CTX));
        assertEquals(ErpPrjErrors.ERR_TASK_SELF_DEPENDENCY.getErrorCode(), ex.getErrorCode(),
                "自环应优先于深度判定");
    }

    // ============ 场景 8b：长链深度超限 ============

    @Test
    public void scenario8b_longChainDepthExceeded() {
        System.setProperty(ErpPrjConstants.CONFIG_TASK_DEPENDENCY_MAX_DEPTH, "100");
        try {
            Long projectId = seedProject();
            // 102 节点链 A1→A2→...→A102（边数=101，自 A1 上行追溯至 A102 步数=101）
            Long[] chain = seedLongChain(projectId, 102);

            // findPredecessors(A1) 上行追溯 101 步 → 超过 maxDepth=100
            NopException ex = assertThrows(NopException.class,
                    () -> taskBiz.findPredecessors(chain[0], CTX));
            assertEquals(ErpPrjErrors.ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED.getErrorCode(), ex.getErrorCode());
            assertEquals(101, ex.getParam(ErpPrjErrors.ARG_ACTUAL_DEPTH),
                    "actualDepth 应=101（节点数−1=边数=深度）");
            assertEquals(100, ex.getParam(ErpPrjErrors.ARG_MAX_DEPTH));
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_TASK_DEPENDENCY_MAX_DEPTH);
        }
    }

    // ============ helpers ============

    private Long seedProject() {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode("PRJ-DAG-" + System.nanoTime());
        p.setName("DAG 测试项目");
        p.setOrgId(1L);
        p.setCurrencyId(1L);
        p.setStatus(ErpPrjConstants.PROJECT_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedTask(Long projectId, String title, String status, Long dependsOnId) {
        IEntityDao<ErpPrjTask> dao = daoProvider.daoFor(ErpPrjTask.class);
        ErpPrjTask t = new ErpPrjTask();
        t.setProjectId(projectId);
        t.setTitle(title);
        t.setStatus(status);
        t.setDependsOnId(dependsOnId);
        dao.saveEntity(t);
        return t.getId();
    }

    /**
     * 构造 N 节点链 A1.dependsOnId=A2, A2.dependsOnId=A3, ..., A(N-1).dependsOnId=A(N), A(N) 无前置。
     * 返回数组 chain[0]=A1.id, chain[1]=A2.id, ..., chain[N-1]=A(N).id。
     * 从末尾向前建（末尾无前置），保证外键引用顺序。
     */
    private Long[] seedLongChain(Long projectId, int nodeCount) {
        Long[] ids = new Long[nodeCount];
        for (int i = nodeCount - 1; i >= 0; i--) {
            Long dependsOn = (i + 1 < nodeCount) ? ids[i + 1] : null;
            ids[i] = seedTask(projectId, "A" + (i + 1), ErpPrjConstants.TASK_STATUS_DONE, dependsOn);
        }
        return ids;
    }

    private boolean containsId(List<ErpPrjTask> tasks, Long id) {
        for (ErpPrjTask t : tasks) {
            if (id.equals(t.getId())) {
                return true;
            }
        }
        return false;
    }
}
