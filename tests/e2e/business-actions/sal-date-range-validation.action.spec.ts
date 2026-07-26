import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  verifyState,
  deleteById,
  deleteByFilter,
  eqFilter,
  andFilter,
} from './_helper';
import { GraphQLClient } from '../pages';
import type { Page } from '@playwright/test';

/**
 * sales 定价 3 实体日期范围有效性校验钩子浏览器层 E2E（plan 2026-07-26-0500-3）。
 *
 * 验证 C3 三策略经 GraphQL `__save`/`__update` 写路径全栈可达（CrudBizModel
 * `defaultPrepareSave/Update` 钩子触发，对齐 master-data.write.spec.ts GraphQL 写路径范式 +
 * hr-leave-attendance.action.spec.ts:130-144 日期重叠拒绝断言范式）：
 *
 * (1) ErpSalPriceListLine MUTEX —— 同 priceListId+materialId 维度重叠区间 __save 抛
 *     ERR_SAL_PRICE_LIST_LINE_OVERLAP，相邻日通过。
 * (2) ErpSalPricingRule STACKABLE 混合 —— 双非 stackable 重叠抛 ERR_SAL_PRICING_RULE_OVERLAP，
 *     任一方 stackable=true 允许重叠。
 * (3) ErpSalPriceList PRIORITY warn-only —— 同维度同优先级多份有效清单均 __save 成功
 *     （warn 不阻断）+ __get 断言记录共存。
 * (4) ErpSalPriceListLine __update 自身排除 —— 更新既有行 validTo 不与自身重叠通过
 *     （enforceMutex selfId 排除路径）。
 *
 * 自包含 setup：每测试建测试专用 priceList/pricingRule（唯一 code 前缀 E2E-SAL-DR-），
 * materialId 复用种子 MAT_1（id=1）。cleanup 经 __delete 逐域清理，保护共享 DB 基线。
 *
 * 区间字段类型：PriceList/PriceListLine.validFrom/validTo = DATE（ISO 日期）；
 * PricingRule.validFrom/validTo = TIMESTAMP（ISO 时间戳，PricingRuleDateRange 适配器截断到 LocalDate）。
 */

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

const MAT_1 = 1;

/**
 * 经 GraphQL `__save` 原始 mutation（不经 createViaSave 的成功断言），返回完整 envelope。
 * 用于拒绝路径——`createViaSave` 内置 `expect(errors).toBeNull()` 会在拒绝时失败，
 * 故拒绝路径需直取 `{data, errors, json}` 自行断言。
 */
async function saveRaw(
  page: Page,
  entityName: string,
  data: Record<string, unknown>,
  selection = 'id',
): Promise<{ data: any | null; errors: any[] | null; json: any }> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    `mutation($d:${entityName}__save_input){ ${entityName}__save(data:$d){ ${selection} } }`,
    { d: data },
  );
  return {
    data: json?.data?.[`${entityName}__save`] ?? null,
    errors: json?.errors ?? null,
    json,
  };
}

async function buildPriceList(page: Page, tag: string, extra: Record<string, unknown>): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpSalPriceList',
    {
      code: uniq(`E2E-SAL-DR-PL-${tag}`),
      name: `E2E日期范围清单${tag}`,
      customerGroupCode: 'E2E-DR-CG',
      partnerId: 1,
      priority: 100,
      ...extra,
    },
    'id',
  );
}

async function buildPriceListLine(
  page: Page,
  priceListId: string | number,
  tag: string,
  validFrom: string,
  validTo: string,
): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpSalPriceListLine',
    {
      priceListId,
      materialId: MAT_1,
      unitPrice: 10,
      validFrom,
      validTo,
    },
    'id',
  );
}

test.describe('sales C3 日期范围有效性校验钩子（3 实体 × 3 策略 + __update 自身排除）', () => {
  test('(1) ErpSalPriceListLine MUTEX：重叠拒绝 + 相邻日通过', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalPriceList-main');

    const pl = await buildPriceList(page, 'MUTEX', { validFrom: '2026-01-01', validTo: '2026-12-31' });
    const createdLineIds: string[] = [];

    try {
      // 第一行 [2026-01-01, 2026-12-31] 成功
      const line1 = await buildPriceListLine(page, pl.id, 'L1', '2026-01-01', '2026-12-31');
      createdLineIds.push(line1.id);

      // 第二行重叠 [2026-06-01, 2027-06-30] → 拒绝 ERR_SAL_PRICE_LIST_LINE_OVERLAP
      const rej = await saveRaw(page, 'ErpSalPriceListLine', {
        priceListId: pl.id,
        materialId: MAT_1,
        unitPrice: 12,
        validFrom: '2026-06-01',
        validTo: '2027-06-30',
      });
      expect(rej.errors, 'overlapping PriceListLine __save should be rejected').toBeTruthy();
      expect(
        JSON.stringify(rej.errors),
        'reject should carry overlap-conflict semantic token (ERR_SAL_PRICE_LIST_LINE_OVERLAP message 含「冲突」)',
      ).toContain('冲突');

      // 相邻日 [2027-01-01, 2027-12-31] 通过（line1 validTo=2026-12-31，相邻不重叠）
      const line2 = await buildPriceListLine(page, pl.id, 'L2', '2027-01-01', '2027-12-31');
      createdLineIds.push(line2.id);
      expect(line2.id, 'adjacent-day PriceListLine __save succeeds').toBeTruthy();
    } finally {
      for (const id of createdLineIds) await deleteById(page, 'ErpSalPriceListLine', id);
      await deleteById(page, 'ErpSalPriceList', pl.id);
    }
  });

  test('(2) ErpSalPricingRule STACKABLE：双非重叠拒绝 + 任一方 stackable=true 通过', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalPricingRule-main');

    const ruleIds: string[] = [];
    const baseRule = {
      ruleType: 'PERCENT_DISCOUNT',
      targetType: 'LINE',
      materialId: MAT_1,
      priority: 100,
      customerGroupCode: 'E2E-DR-CG',
      partnerId: 1,
    };

    try {
      // 规则 #1 stackable=false [2026-01-01, 2026-12-31] 成功
      const r1 = await createViaSave(
        page,
        'ErpSalPricingRule',
        {
          ruleCode: uniq('E2E-SAL-DR-PR-R1'),
          ruleName: 'E2E STACKABLE 规则1',
          stackable: false,
          validFrom: '2026-01-01T00:00:00',
          validTo: '2026-12-31T23:59:59',
          ...baseRule,
        },
        'id',
      );
      ruleIds.push(r1.id);

      // 规则 #2 stackable=false 重叠 → 拒绝 ERR_SAL_PRICING_RULE_OVERLAP
      const rej = await saveRaw(page, 'ErpSalPricingRule', {
        ruleCode: uniq('E2E-SAL-DR-PR-R2'),
        ruleName: 'E2E STACKABLE 规则2(重叠拒绝)',
        stackable: false,
        validFrom: '2026-06-01T00:00:00',
        validTo: '2027-06-30T23:59:59',
        ...baseRule,
      });
      expect(rej.errors, 'double-non-stackable overlap should be rejected').toBeTruthy();
      expect(
        JSON.stringify(rej.errors),
        'reject should carry overlap-conflict semantic token (ERR_SAL_PRICING_RULE_OVERLAP message 含「冲突」)',
      ).toContain('冲突');

      // 规则 #3 stackable=true 重叠 → 通过（允许叠加）
      const r3 = await createViaSave(
        page,
        'ErpSalPricingRule',
        {
          ruleCode: uniq('E2E-SAL-DR-PR-R3'),
          ruleName: 'E2E STACKABLE 规则3(可叠加)',
          stackable: true,
          validFrom: '2026-06-01T00:00:00',
          validTo: '2027-06-30T23:59:59',
          ...baseRule,
        },
        'id stackable',
      );
      ruleIds.push(r3.id);
      expect(r3.id, 'stackable=true overlapping rule __save succeeds').toBeTruthy();
    } finally {
      for (const id of ruleIds) await deleteById(page, 'ErpSalPricingRule', id);
    }
  });

  test('(3) ErpSalPriceList PRIORITY warn-only：同维度多份有效清单均保存成功 + 共存', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalPriceList-main');

    const plIds: string[] = [];
    // 3 份以真正触达 effective.size()>=2 + top==next 歧义分支（见 plan Phase 1 Execution Decisions §6）
    const common = {
      customerGroupCode: 'E2E-DR-PRI-CG',
      partnerId: 1,
      priority: 100,
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
    };

    try {
      const pl1 = await createViaSave(
        page,
        'ErpSalPriceList',
        { code: uniq('E2E-SAL-DR-PRI-1'), name: 'E2E PRIORITY 清单1', ...common },
        'id',
      );
      plIds.push(pl1.id);

      const pl2 = await createViaSave(
        page,
        'ErpSalPriceList',
        { code: uniq('E2E-SAL-DR-PRI-2'), name: 'E2E PRIORITY 清单2', ...common },
        'id',
      );
      plIds.push(pl2.id);

      // 第 3 份 save 时 [#1,#2] 已落库 → effective size=2 → top==next（均 priority=100）→ LOG.warn（不阻断）
      const pl3 = await createViaSave(
        page,
        'ErpSalPriceList',
        { code: uniq('E2E-SAL-DR-PRI-3'), name: 'E2E PRIORITY 清单3', ...common },
        'id',
      );
      plIds.push(pl3.id);

      // 三份均 __save 成功（warn-only 不阻断）+ __get 断言共存
      for (const id of plIds) {
        const got = await verifyState(page, 'ErpSalPriceList', id, 'id priority customerGroupCode');
        expect(got.id, 'PRIORITY price list coexists after warn-only hook').not.toBeNull();
        expect(got.priority, 'priority preserved').toBe(100);
      }
      expect(plIds.length, '3 overlapping same-dimension price lists coexist (warn-only)').toBe(3);
    } finally {
      for (const id of plIds) await deleteById(page, 'ErpSalPriceList', id);
    }
  });

  test('(4) ErpSalPriceListLine __update 自身排除：更新 validTo 不与自身重叠通过', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalPriceListLine-main');

    const pl = await buildPriceList(page, 'UPD', { validFrom: '2026-01-01', validTo: '2026-12-31' });

    try {
      // 单行 [2026-01-01, 2026-12-31]
      const line = await buildPriceListLine(page, pl.id, 'U1', '2026-01-01', '2026-12-31');

      // __update 缩窄 validTo → 2026-06-30：候选 [2026-01-01, 2026-06-30] 与落库自身
      // [2026-01-01, 2026-12-31] 区间相交，enforceMutex 经 selfId=entity.getId() 排除自身 → 通过
      const gql = new GraphQLClient(page);
      const json: any = await gql.raw(
        `mutation($d:ErpSalPriceListLine__update_input){ ErpSalPriceListLine__update(data:$d){ id validTo } }`,
        { d: { id: line.id, validTo: '2026-06-30' } },
      );
      expect(json?.errors, '__update with self-exclusion should not return errors').toBeFalsy();
      expect(json?.data?.ErpSalPriceListLine__update?.validTo, '__update persists validTo').toBe('2026-06-30');

      // __get 独立断言
      const got = await verifyState(page, 'ErpSalPriceListLine', line.id, 'id validTo');
      expect(got.validTo, '__get confirms validTo updated').toBe('2026-06-30');

      await deleteById(page, 'ErpSalPriceListLine', line.id);
    } finally {
      await deleteById(page, 'ErpSalPriceList', pl.id);
    }
  });
});
