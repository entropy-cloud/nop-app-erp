package app.erp.prj.service.validator;

import app.erp.prj.dao.entity.ErpPrjTask;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link TaskDependencyValidator} 纯逻辑单测（task-dag.md §2 算法）。
 *
 * <p>纯逻辑、无 IoC 无 DB —— 直接构造 ErpPrjTask 实例（经 orm_internalSet 设置 id 与 dependsOnId），
 * 用 Map<Long, ErpPrjTask> 模拟加载源。验证算法本身（自环/成环/深度上限/上行链收集）。
 */
public class TestTaskDependencyValidator {

    /**
     * 自环 A.dependsOnId=A → ERR_TASK_SELF_DEPENDENCY。
     */
    @Test
    public void testSelfDependency() {
        Map<Long, ErpPrjTask> tasks = new HashMap<>();
        tasks.put(1L, newTask(1L, 1L)); // A depends on A

        NopException ex = assertThrows(NopException.class,
                () -> TaskDependencyValidator.detectCycle(1L, 1L, loader(tasks), 100));
        assertEquals(ErpPrjErrors.ERR_TASK_SELF_DEPENDENCY.getErrorCode(), ex.getErrorCode());
    }

    /**
     * 二环 A→B→A → ERR_TASK_DEPENDENCY_CYCLE，chain="1→2→1"。
     */
    @Test
    public void testTwoNodeCycle() {
        Map<Long, ErpPrjTask> tasks = new HashMap<>();
        // A.dependsOnId=B(2), B.dependsOnId=A(1) ⇒ 环
        tasks.put(1L, newTask(1L, 2L));
        tasks.put(2L, newTask(2L, 1L));

        NopException ex = assertThrows(NopException.class,
                () -> TaskDependencyValidator.detectCycle(1L, 2L, loader(tasks), 100));
        assertEquals(ErpPrjErrors.ERR_TASK_DEPENDENCY_CYCLE.getErrorCode(), ex.getErrorCode());
        assertEquals("1→2→1", ex.getParam(ErpPrjErrors.ARG_CHAIN));
    }

    /**
     * 三环 A→B→C→A → ERR_TASK_DEPENDENCY_CYCLE。
     */
    @Test
    public void testThreeNodeCycle() {
        Map<Long, ErpPrjTask> tasks = new HashMap<>();
        // A.dependsOnId=B(2), B.dependsOnId=C(3), C.dependsOnId=A(1) ⇒ 环
        tasks.put(1L, newTask(1L, 2L));
        tasks.put(2L, newTask(2L, 3L));
        tasks.put(3L, newTask(3L, 1L));

        NopException ex = assertThrows(NopException.class,
                () -> TaskDependencyValidator.detectCycle(1L, 2L, loader(tasks), 100));
        assertEquals(ErpPrjErrors.ERR_TASK_DEPENDENCY_CYCLE.getErrorCode(), ex.getErrorCode());
        assertEquals("1→2→3→1", ex.getParam(ErpPrjErrors.ARG_CHAIN));
    }

    /**
     * 长链无环 A→B→C→D（D 无前置）→ validate 通过。
     */
    @Test
    public void testLongChainNoCycle() {
        Map<Long, ErpPrjTask> tasks = new HashMap<>();
        // A(1)→B(2)→C(3)→D(4)，D 无前置
        tasks.put(1L, newTask(1L, 2L));
        tasks.put(2L, newTask(2L, 3L));
        tasks.put(3L, newTask(3L, 4L));
        tasks.put(4L, newTask(4L, null));

        assertDoesNotThrow(() -> TaskDependencyValidator.detectCycle(1L, 2L, loader(tasks), 100));
    }

    /**
     * 深度超限 101 链（A1→A2→...→A101，自 A101 上行追溯至 A1 步数=100）：
     * maxDepth=100 容许通过；maxDepth=99 触发 ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED，actualDepth=100。
     *
     * <p>节点数=101，边数=100（A1.dependsOnId=A2, A2.dependsOnId=A3, ..., A100.dependsOnId=A101, A101.dependsOnId=null）。
     * 从 A1 出发，经 dependsOnId=A2 上行追溯 100 步到 A101。
     */
    @Test
    public void testDepthExceeded() {
        Map<Long, ErpPrjTask> tasks = new HashMap<>();
        // 101 节点链：节点 i 的 dependsOnId = i+1（i 从 1 到 100）；节点 101 无前置
        for (long i = 1; i <= 100; i++) {
            tasks.put(i, newTask(i, i + 1));
        }
        tasks.put(101L, newTask(101L, null));

        // maxDepth=100 容许：从 A1 经 A2 上行追溯 100 步到 A101，depth 到 100 时 cursor=A101，A101.dependsOnId=null，退出。
        assertDoesNotThrow(() -> TaskDependencyValidator.detectCycle(1L, 2L, loader(tasks), 100));

        // maxDepth=99 触发超限：depth=100 时 cursor=A101 > maxDepth=99
        NopException ex = assertThrows(NopException.class,
                () -> TaskDependencyValidator.detectCycle(1L, 2L, loader(tasks), 99));
        assertEquals(ErpPrjErrors.ERR_TASK_DEPENDENCY_DEPTH_EXCEEDED.getErrorCode(), ex.getErrorCode());
        assertEquals(100, ex.getParam(ErpPrjErrors.ARG_ACTUAL_DEPTH));
        assertEquals(99, ex.getParam(ErpPrjErrors.ARG_MAX_DEPTH));
    }

    /**
     * 上行链全量 collectPredecessors(A) 在 A→B→C→D 链返回 [B,C,D]。
     */
    @Test
    public void testCollectPredecessors() {
        Map<Long, ErpPrjTask> tasks = new HashMap<>();
        // A(1)→B(2)→C(3)→D(4)，D 无前置
        tasks.put(1L, newTask(1L, 2L));
        tasks.put(2L, newTask(2L, 3L));
        tasks.put(3L, newTask(3L, 4L));
        tasks.put(4L, newTask(4L, null));

        List<ErpPrjTask> predecessors = TaskDependencyValidator.collectPredecessors(1L, loader(tasks), 100);
        assertEquals(3, predecessors.size(), "应返回 3 个前置任务");
        List<Long> ids = new ArrayList<>();
        for (ErpPrjTask t : predecessors) {
            ids.add(t.getId());
        }
        assertEquals(java.util.Arrays.asList(2L, 3L, 4L), ids, "上行链顺序应为 [B, C, D]");
    }

    // ---------- helpers ----------

    private ErpPrjTask newTask(Long id, Long dependsOnId) {
        ErpPrjTask t = new ErpPrjTask();
        t.orm_internalSet(ErpPrjTask.PROP_ID_id, id);
        t.orm_internalSet(ErpPrjTask.PROP_ID_dependsOnId, dependsOnId);
        return t;
    }

    private Function<Long, ErpPrjTask> loader(Map<Long, ErpPrjTask> tasks) {
        return tasks::get;
    }
}
