package app.erp.hr.service.statemachine;

import app.erp.hr.service.ErpHrConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 层 1 矩阵完备性表驱动测试（契约 §10 层 1；plan Phase 1 Proof）——<strong>退化解</strong>。
 *
 * <p>针对 {@link ErpHrEmployeeStateMachine} Bean 的纯分类 + 死状态登记完备性遍历：不经 BizModel 入口（层 3 职责）。
 * 覆盖：
 * <ul>
 *   <li>(a) {@code transitions()} <strong>空</strong>（退化解，如实反映零命名动作迁移 writer）；</li>
 *   <li>(b) {@code isTransferable}(ACTIVE/PROBATION=true, RESIGNED/TERMINATED/RETIRED=false)；</li>
 *   <li>(c) {@code nonTransferableStatuses()}=[RESIGNED,TERMINATED,RETIRED]；</li>
 *   <li>(d) initial={ACTIVE,PROBATION}（owner doc §1 业务语义）；</li>
 *   <li>(e) terminal={RESIGNED,TERMINATED,RETIRED}（owner doc §3 业务语义，对齐显式声明）；</li>
 *   <li>(f) <strong>断言三终态不在 transitions/initial 集合（死状态：零入边）但 isTerminal=true（业务终态 per §3）</strong>。
 *       「死」（无入边）与「终态」（业务生命周期终点）不矛盾——退化解下全部状态无出边，故「终态」按 §3 业务语义
 *       而非图论定义裁定（否则 ACTIVE/PROBATION 也无出边会被误判终态）。</li>
 * </ul>
 *
 * <p>Bean 严格无状态，直接 {@code new} 实例化测试，无需 IoC 容器。
 */
public class TestErpHrEmployeeStateMachineMatrix {

    /** dict 全集（erp-hr/employment-status 5 值）。 */
    static final List<String> DICT_STATUSES = Arrays.asList(
            ErpHrConstants.EMPLOYMENT_ACTIVE,
            ErpHrConstants.EMPLOYMENT_PROBATION,
            ErpHrConstants.EMPLOYMENT_RESIGNED,
            ErpHrConstants.EMPLOYMENT_TERMINATED,
            ErpHrConstants.EMPLOYMENT_RETIRED);

    private final ErpHrEmployeeStateMachine sm = new ErpHrEmployeeStateMachine();

    // ---------- (a) transitions() 空（退化解） ----------

    @Test
    public void testTransitionsEmptyDegenerateAxis() {
        assertTrue(sm.transitions().isEmpty(),
                "退化解：transitions 应为空（零命名动作迁移 writer，如实反映无活态迁移）");
    }

    // ---------- (b) isTransferable ----------

    @Test
    public void testIsTransferableActiveOrProbationOnly() {
        assertTrue(sm.isTransferable(ErpHrConstants.EMPLOYMENT_ACTIVE),
                "ACTIVE 可调动");
        assertTrue(sm.isTransferable(ErpHrConstants.EMPLOYMENT_PROBATION),
                "PROBATION 可调动");
        assertFalse(sm.isTransferable(ErpHrConstants.EMPLOYMENT_RESIGNED),
                "RESIGNED 不可调动");
        assertFalse(sm.isTransferable(ErpHrConstants.EMPLOYMENT_TERMINATED),
                "TERMINATED 不可调动");
        assertFalse(sm.isTransferable(ErpHrConstants.EMPLOYMENT_RETIRED),
                "RETIRED 不可调动");
    }

    // ---------- (c) nonTransferableStatuses ----------

    @Test
    public void testNonTransferableStatuses() {
        assertEquals(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_RESIGNED,
                ErpHrConstants.EMPLOYMENT_TERMINATED,
                ErpHrConstants.EMPLOYMENT_RETIRED), sm.nonTransferableStatuses(),
                "不可调动集合 = 三终态 {RESIGNED, TERMINATED, RETIRED}");
    }

    // ---------- (d) initialStatuses（owner doc §1） ----------

    @Test
    public void testInitialStatusesAlignedWithOwnerDocSection1() {
        assertEquals(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_ACTIVE,
                ErpHrConstants.EMPLOYMENT_PROBATION), sm.initialStatuses(),
                "初始态集合 = {ACTIVE, PROBATION}（owner doc §1 业务语义：在职/试用期入口）");
    }

    // ---------- (e) terminalStatuses（owner doc §3） ----------

    @Test
    public void testTerminalStatusesAlignedWithOwnerDocSection3() {
        assertEquals(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_RESIGNED,
                ErpHrConstants.EMPLOYMENT_TERMINATED,
                ErpHrConstants.EMPLOYMENT_RETIRED), sm.terminalStatuses(),
                "终态集合 = {RESIGNED, TERMINATED, RETIRED}（owner doc §3 显式声明，业务生命周期终点）");

        assertTrue(sm.isTerminal(ErpHrConstants.EMPLOYMENT_RESIGNED),
                "RESIGNED isTerminal=true（§3 业务终态）");
        assertTrue(sm.isTerminal(ErpHrConstants.EMPLOYMENT_TERMINATED),
                "TERMINATED isTerminal=true（§3 业务终态）");
        assertTrue(sm.isTerminal(ErpHrConstants.EMPLOYMENT_RETIRED),
                "RETIRED isTerminal=true（§3 业务终态）");
        assertFalse(sm.isTerminal(ErpHrConstants.EMPLOYMENT_ACTIVE),
                "ACTIVE isTerminal=false（§1 初始态）");
        assertFalse(sm.isTerminal(ErpHrConstants.EMPLOYMENT_PROBATION),
                "PROBATION isTerminal=false（§1 初始态）");
    }

    // ---------- (f) 三终态不在 transitions/initial 集合（死状态：零入边）但 isTerminal=true ----------

    @Test
    public void testTerminalStatusesAreDeadButBusinessTerminal() {
        // RESIGNED/TERMINATED/RETIRED = 死状态（零 writer = 无入边），不出现在 transitions/initial 任一集合
        List<String> deadStatuses = Arrays.asList(
                ErpHrConstants.EMPLOYMENT_RESIGNED,
                ErpHrConstants.EMPLOYMENT_TERMINATED,
                ErpHrConstants.EMPLOYMENT_RETIRED);

        // transitions 空（已在 testTransitionsEmptyDegenerateAxis 断言），故死状态天然不在任何边——
        // 此处显式断言强化语义：死 = 无入边（不在 transitions 任何 toStatus），但因 transitions 空必然成立。
        for (String dead : deadStatuses) {
            assertFalse(sm.initialStatuses().contains(dead),
                    "死状态 " + dead + " 不应在 initial 集合（零 writer 不可达为初始态）");
            // isTerminal=true 表达业务终态（§3），与「死」（无入边）不矛盾
            assertTrue(sm.isTerminal(dead),
                    "死状态 " + dead + " isTerminal=true（§3 业务终态，dead≠terminal）");
        }
    }

    @Test
    public void testNonTerminalStatusesNotInTerminalSet() {
        // ACTIVE/PROBATION 不在 terminal 集合（§1 初始态，非终态）
        assertFalse(sm.terminalStatuses().contains(ErpHrConstants.EMPLOYMENT_ACTIVE),
                "ACTIVE 不在 terminal 集合（退化解无出边≠终态，按 §1 业务语义归 initial）");
        assertFalse(sm.terminalStatuses().contains(ErpHrConstants.EMPLOYMENT_PROBATION),
                "PROBATION 不在 terminal 集合（§1 初始态）");
    }
}
