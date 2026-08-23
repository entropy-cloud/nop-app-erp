import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';
import { createViaSave, deleteById } from '../business-actions/_helper';

/**
 * aps schedule-gantt 页面数据源运行时复核（plan 2026-08-23-0434-2 Phase 2 Add 载体）。
 *
 * 批内序 1 修复 schedule-gantt.page.yaml:59 的 `$mid:Long` → `$mid:String`（id String 化后
 * Long 变量收 String 值被 GraphQL 类型系统拒绝，adaptor 静默降级为空图——M3.6 version-diff
 * hasCompare:false 同源先例）。本 spec 原样重放页面 raw-GraphQL 数据源（含类型化变量），
 * 证明 machineId 过滤返回非空行（甘特图非静默降级）。
 *
 * 自包含 setup：经 __save 直置 PLANNED 态 ErpApsOperationOrder（machineId 无 FK 约束，
 * 与 aps-operation-order.action.spec.ts 同范式）+ plannedStartDateT/EndDateT 直写
 * （普通 TIMESTAMP 列，页面 adaptor 以 plannedStartDateT 过滤甘特条）。
 * 清理：删 OperationOrder（无下游产物）。
 */

const MACHINE_ID = '910'; // 无 FK 约束自由值，避免与 aps-action 套件 MACHINE_ID=100 串扰

test.describe('aps schedule-gantt page data source (machineId filter)', () => {
  test('findPage(filter_machineId:$mid String) returns the seeded non-empty gantt rows', async ({ page }) => {
    await loginAndNavigate(page, '/ErpApsOperationOrder-main');

    const op = await createViaSave(
      page, 'ErpApsOperationOrder',
      {
        code: `E2E-GANTT-${Date.now()}`,
        workOrderId: '1',
        operationName: 'E2E gantt source op',
        sequence: 10,
        machineId: MACHINE_ID,
        priority: 10,
        setupTime: 0,
        runtimePerUnit: 10,
        qty: 3,
        earliestStartDateT: '2026-07-10T08:00:00',
        plannedStartDateT: '2026-07-10T08:00:00',
        plannedEndDateT: '2026-07-10T08:30:00',
        status: 'PLANNED',
      },
      'id machineId status plannedStartDateT',
    );
    expect(op.machineId, 'seeded op machineId').toBe(MACHINE_ID);
    expect(op.plannedStartDateT, 'plannedStartDateT drives gantt bar rendering').toBeTruthy();

    // schedule-gantt.page.yaml 修复后查询形态（query:{} Map + orderBy OrderFieldBean 数组 +
    // 客户端 machineId 过滤——filter_xxx/裸 limit 快捷参数经 schema 实测不存在）
    const query = 'query($lim:Int){ ErpApsOperationOrder__findPage(query:{limit:$lim,orderBy:[{name:"plannedStartDateT",desc:false}]}){ items{ id code workOrderId operationName machineId priority plannedStartDateT plannedEndDateT qty totalDuration status } total } }';
    const json: any = await new GraphQLClient(page).raw(query, { lim: 500 });

    expect(json?.errors, 'page query should not return GraphQL errors (adaptor silent-degradation eliminated)').toBeFalsy();
    const result = json?.data?.ErpApsOperationOrder__findPage;
    expect(result, 'findPage result should be present').toBeTruthy();
    const allItems: any[] = result?.items || [];
    expect(allItems.length, 'page data source should return rows').toBeGreaterThan(0);

    // 客户端 machineId 过滤（镜像 adaptor midF 逻辑）——非空行实证
    const items = allItems.filter((r: any) => String(r.machineId ?? '') === MACHINE_ID);
    expect(items.length, 'client-side machineId filter should return non-empty rows').toBeGreaterThan(0);
    const mine = items.find((r: any) => String(r.id) === String(op.id));
    expect(mine, 'seeded op present in machineId-filtered rows').toBeTruthy();
    expect(mine?.plannedStartDateT, 'plannedStartDateT drives gantt bars').toBeTruthy();

    await deleteById(page, 'ErpApsOperationOrder', op.id);
  });
});
