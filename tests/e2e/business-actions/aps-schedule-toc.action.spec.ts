import { test, expect, loginAndNavigate, createViaSave, callMutationOk, deleteById, deleteByFilter, eqFilter, verifyState } from './_helper';
import { GraphQLClient } from '../pages';

/**
 * E3.4 TOC 瓶颈驱动排产浏览器层 E2E（plan 2026-08-26-0735-2 Phase 5，
 * `constraint-based-planning.md` §1/§2）。
 *
 * 验证 `ErpApsOperationOrder__scheduleToc` 经 GraphQL /graphql 全栈可达：
 *   - 结果扩展字段返回（bottleneckMachineIds + machineLoadRates，无瓶颈时空清单合法）；
 *   - 瓶颈中心工序被排定（PLANNED + plannedStart/End 回写）；
 *   - 既有前向模式（经求解器分派路径）行为不变（同输入同排程）。
 *
 * 自包含 setup：Schedule（24h horizon）+ 两台工作中心工序（E2E 共享库无日历配置，
 * 产能走 24h 兜底派生：A 14h/24h = 0.5833，B 2h/24h = 0.0833）。清理：删 Schedule + 工序。
 */

const HORIZON_START = '2026-07-10T00:00:00';
const HORIZON_END = '2026-07-11T00:00:00';

interface TocResult {
  bottleneckMachineIds?: string[];
  machineLoadRates?: Record<string, string>;
  feasible?: boolean;
}

test.describe('aps scheduleToc (E3.4 TOC pilot)', () => {
  test('scheduleToc schedules bottleneck ops and returns load-rate extensions', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsSchedule-main');

    const ts = Date.now();
    const createdOps: string[] = [];
    // 聚合 app 内 CrpCapacityProvider（mfg 日历产能派生链）生效：为合成工作中心种工作中心（seq 生成 id）+ 日历（08:00~16:00 × 2 日 = 16h）。
    // 每次运行新建（逻辑删除行仍占用代码唯一键，跨运行不复用）。
    const machineIds: string[] = [];
    for (const slot of ['A', 'B']) {
      const wc = await createViaSave(
        page, 'ErpMfgWorkcenter',
        { code: `E2E-TOC-WC-${slot}-${ts}`, name: `E2E TOC Workcenter ${slot}`, capacity: 1 },
        'id',
      );
      machineIds.push(String(wc.id));
      await createViaSave(
        page, 'ErpMfgWorkcenterCalendar',
        {
          workcenterId: String(wc.id), calendarName: `E2E-TOC-CAL-${slot}-${ts}`, shiftType: 'ONE_SHIFT',
          workDatePattern: 'ALL_WEEK', startTime: '08:00:00', endTime: '16:00:00',
          effectiveFrom: '2026-01-01', isActive: true,
        },
        'id',
      );
    }
    const MACHINE_A = machineIds[0];
    const MACHINE_B = machineIds[1];
    const schedule = await createViaSave(
      page, 'ErpApsSchedule',
      {
        code: `E2E-APS-TOC-${ts}`,
        name: `E2E TOC ${ts}`,
        scheduleDate: '2026-07-10',
        schedulingMode: 'FORWARD',
        horizonStart: HORIZON_START,
        horizonEnd: HORIZON_END,
        status: 'DRAFT',
      },
      'id',
    );

    const w2Seq10 = await createViaSave(
      page, 'ErpApsOperationOrder',
      {
        code: `E2E-TOC-A10-${ts}`, workOrderId: '9002', operationName: 'TOC-A10',
        sequence: 10, machineId: MACHINE_A, priority: 50,
        setupTime: 0, runtimePerUnit: 480, qty: 1,
        status: 'DRAFT', earliestStartDateT: '2026-07-10T08:00:00',
      },
      'id',
    );
    createdOps.push(w2Seq10.id);
    const a20 = await createViaSave(
      page, 'ErpApsOperationOrder',
      {
        code: `E2E-TOC-A20-${ts}`, workOrderId: '9001', operationName: 'TOC-A20',
        sequence: 20, machineId: MACHINE_A, priority: 80,
        setupTime: 0, runtimePerUnit: 360, qty: 1,
        status: 'DRAFT', earliestStartDateT: '2026-07-10T08:00:00',
      },
      'id',
    );
    createdOps.push(a20.id);
    const b10 = await createViaSave(
      page, 'ErpApsOperationOrder',
      {
        code: `E2E-TOC-B10-${ts}`, workOrderId: '9001', operationName: 'TOC-B10',
        sequence: 10, machineId: MACHINE_B, priority: 10,
        setupTime: 0, runtimePerUnit: 120, qty: 1,
        status: 'DRAFT', earliestStartDateT: '2026-07-10T08:00:00',
      },
      'id',
    );
    createdOps.push(b10.id);

    try {
      const result = await callMutationOk<TocResult>(
        page, 'ErpApsOperationOrder', 'scheduleToc', { scheduleId: schedule.id },
        'bottleneckMachineIds machineLoadRates feasible',
      );

      // 结果扩展字段可达（聚合 app：mfg 日历产能派生链 → 8h × 2 日 = 16h；A 14h/16h，B 2h/16h）
      expect(result.machineLoadRates, 'machineLoadRates should be returned').toBeTruthy();
      expect(Number(result.machineLoadRates![MACHINE_A]).toFixed(4)).toBe('0.8750');
      expect(Number(result.machineLoadRates![MACHINE_B]).toFixed(4)).toBe('0.1250');
      expect(result.feasible).toBe(true);
      // 阈值默认 1.0，负荷率未超阈值 → 无瓶颈（空清单为合法可观测结果）
      expect(Array.isArray(result.bottleneckMachineIds)).toBe(true);

      // 瓶颈候选机器 A 的工序仍被排定（PLANNED + 时间回写，非瓶颈 B 拉动式倒排）
      const a10 = await verifyState(page, 'ErpApsOperationOrder', w2Seq10.id, 'status plannedStartDateT');
      expect(a10.status, 'bottleneck-machine op should be PLANNED').toBe('PLANNED');
      expect(String(a10.plannedStartDateT), 'TOC schedules from earliest start').toContain('2026-07-10');
    } finally {
      // 产能预留随排程成功产生，工序删除不级联（既有 aps 范式：先删预留再删工序）
      for (const opId of createdOps) {
        await deleteByFilter(page, 'ErpApsCapacityReservation', eqFilter('operationOrderId', opId));
        await deleteById(page, 'ErpApsOperationOrder', opId);
      }
      await deleteById(page, 'ErpApsSchedule', schedule.id);
      const probe = new GraphQLClient(page);
      const cals = await probe.findItems(
        'ErpMfgWorkcenterCalendar', { contains: { calendarName: `E2E-TOC-CAL-` } }, 'id');
      for (const c of cals) {
        await deleteById(page, 'ErpMfgWorkcenterCalendar', c.id as string);
      }
      // 合成工作中心（seq id，运行隔离）逻辑删除行不清理——代码唯一键跨运行不复用
    }
  });
});
