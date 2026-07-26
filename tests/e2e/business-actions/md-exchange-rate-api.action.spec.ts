import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  findExchangeRatesByBase,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import type { Page } from '@playwright/test';

/**
 * Master-data ErpMdCurrency 汇率查询 API 客户端 refreshRatesFromApi 浏览器层 E2E
 * （plan 2026-07-26-1407-3）。
 *
 * 验证 D1 外部 API 集成参考实现的 `refreshRatesFromApi(baseCurrency)` `@BizMutation` 入口经
 * GraphQL `/graphql` 全栈可达 + Mock provider 确定性汇率写入 + findPage 反查持久化 + 幂等重写：
 *   (1) refreshRatesFromApi(baseCurrency:"USD") 返回非空 List + 逐条断言 Mock 确定性 rate 值
 *       （USD→CNY 7.20 / USD→EUR 0.92，对齐 JUnit testRefreshRatesFromApiWritesExchangeRate）
 *   (2) ErpMdExchangeRate__findPage 反查新增行断言 fromCurrency/toCurrency code + rate + rateType=MIDDLE
 *   (3) 幂等重写覆盖：第二次 refreshRatesFromApi 重写同区间汇率行不累积（count 稳定）
 *
 * 权威设计（docs/design/master-data/exchange-rate-management.md §自动汇率刷新（API 客户端，D1））：
 *   - refreshRatesFromApi 读 ErpMdCurrency.findAll() 作目标币种集 → Factory.fetchRates（config-gated +
 *     限流 + 缓存 + provider 派发）→ upsert ErpMdExchangeRate（幂等键 fromCurrencyId+toCurrencyId+validFrom）
 *   - MockExchangeRateApiClient 固定汇率表（USD→CNY 7.20 / USD→EUR 0.92 / USD→JPY 150.00）
 *
 * Explore Decision（Phase 1，落盘 plan Execution Decisions 段）：
 *   - **config-gate 启用**：webServer JVM args 全局启用 5 项 exchange-rate-api config（enabled/provider=mock/
 *     key/rate-limit-rps=100/cache-ttl-secs=60），config 关闭路径守卫 ERR_EXCHANGE_RATE_API_UNAVAILABLE
 *     经 JUnit testConfigGatedDefaultDisabled 已覆盖（浏览器层不重复，对齐 simulation/intercompany 范式）。
 *   - **自包含 EUR setup**：种子 erp_md_currency.csv 仅含 CNY(id=1)+USD(id=2)，EUR 不在种子 → 经
 *     ErpMdCurrency__save 自包含建 code="EUR" 币种（mock 按目标币种 code 匹配），使 refreshRatesFromApi
 *     写入 2 条汇率（USD→CNY 7.20 + USD→EUR 0.92）；JPY 不在主数据 → 跳过（部分成功语义）。
 *   - **MIDDLE/SPOT 隔离**：BizModel 写 rateType="MIDDLE"（:103），种子 erp_md_exchange_rate.csv 用
 *     RATE_TYPE=SPOT（validFrom=2026-01-01）→ cleanup 按 fromCurrencyId=USD AND rateType=MIDDLE 过滤仅删
 *     本 spec 产物，不触碰种子 SPOT 行（field-format.value.spec.ts 依赖种子 7.25 8 位精度渲染）。
 *   - **双层断言**：层 1 mutation 返回值 rate 字段 + 层 2 findPage 反查持久化 code/rate/rateType 字段。
 *
 * 种子引用：USD id=2 / CNY id=1（erp_md_currency.csv）。种子汇率 erp_md_exchange_rate.csv id=1 USD→CNY
 * SPOT 7.25 validFrom=2026-01-01（与本 spec MIDDLE/today 行区间互斥，cleanup 按 rateType 隔离）。
 */
const USD_ID = 2;
const CNY_ID = 1;
const RATE_TYPE_MIDDLE = 'MIDDLE';

interface RateRow {
  rate: string | number;
  rateType: string;
  fromCurrencyId: string | number;
  toCurrencyId: string | number;
  validFrom?: string;
}

interface RateWithCode {
  rate: string | number;
  rateType: string;
  fromCurrency: { code: string };
  toCurrency: { code: string };
}

function uniq(tag: string): string {
  return `${tag}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

test.describe('Master-data ErpMdCurrency refreshRatesFromApi (D1 exchange-rate API client) browser-layer E2E', () => {
  test('(1)+(2)+(3) refreshRatesFromApi(USD) writes deterministic rates (CNY 7.20 + EUR 0.92) + findPage reverse-query + idempotent rewrite', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdCurrency-main');

    const eurName = uniq('E2E-EUR');
    let eurId: string | number | undefined;
    try {
      // ---- Setup: self-contained EUR currency (mock matches by target code "EUR") ----
      const eur = await createViaSave(
        page,
        'ErpMdCurrency',
        { code: 'EUR', name: eurName, symbol: 'EUR', decimalPlaces: 2, isActive: true },
        'id code',
      );
      eurId = eur.id;

      // ---- (1) refreshRatesFromApi positive path: return List + deterministic rate assertion ----
      const result = await callMutationOk<RateRow[]>(
        page,
        'ErpMdCurrency',
        'refreshRatesFromApi',
        { baseCurrency: 'USD' },
        'rate rateType fromCurrencyId toCurrencyId validFrom',
      );
      expect(Array.isArray(result), 'refreshRatesFromApi should return a List').toBe(true);
      expect(result.length, 'should write >=2 rates (USD→CNY + USD→EUR; JPY not in master-data → skipped)').toBeGreaterThanOrEqual(2);

      const byTarget = new Map<string, RateRow>();
      for (const r of result) {
        byTarget.set(String(r.toCurrencyId), r);
      }
      // USD→CNY = 7.20 (mock deterministic, MockExchangeRateApiClient.java:32)
      const cnyRow = byTarget.get(String(CNY_ID));
      expect(cnyRow, 'return list should contain USD→CNY row').toBeTruthy();
      expect(Number(cnyRow!.rate), 'USD→CNY rate should be 7.20 (mock deterministic)').toBeCloseTo(7.20, 6);
      expect(cnyRow!.rateType, 'rateType should be MIDDLE').toBe(RATE_TYPE_MIDDLE);
      expect(Number(cnyRow!.fromCurrencyId), 'fromCurrencyId should be USD').toBe(USD_ID);
      // USD→EUR = 0.92 (mock deterministic, MockExchangeRateApiClient.java:33)
      const eurRow = byTarget.get(String(eurId));
      expect(eurRow, 'return list should contain USD→EUR row').toBeTruthy();
      expect(Number(eurRow!.rate), 'USD→EUR rate should be 0.92 (mock deterministic)').toBeCloseTo(0.92, 6);
      expect(Number(eurRow!.fromCurrencyId), 'EUR row fromCurrencyId should be USD').toBe(USD_ID);

      // ---- (2) findPage reverse-query: persisted fields with currency code join ----
      const persisted = await findExchangeRatesByBase<RateWithCode>(
        page,
        USD_ID,
        'rate rateType fromCurrency{ code } toCurrency{ code }',
      );
      // 隔离本 spec 产物（rateType=MIDDLE），排除种子 SPOT 行（USD→CNY 7.25 validFrom=2026-01-01）
      const middleRows = persisted.filter(r => r.rateType === RATE_TYPE_MIDDLE);
      const byToCode = new Map<string, RateWithCode>();
      for (const r of middleRows) {
        byToCode.set(r.toCurrency.code, r);
      }
      const cnyPersisted = byToCode.get('CNY');
      expect(cnyPersisted, 'findPage should persist USD→CNY MIDDLE row').toBeTruthy();
      expect(Number(cnyPersisted!.rate), 'persisted USD→CNY rate = 7.20').toBeCloseTo(7.20, 6);
      expect(cnyPersisted!.fromCurrency.code, 'persisted fromCurrency code = USD').toBe('USD');
      expect(cnyPersisted!.toCurrency.code, 'persisted toCurrency code = CNY').toBe('CNY');

      const eurPersisted = byToCode.get('EUR');
      expect(eurPersisted, 'findPage should persist USD→EUR MIDDLE row').toBeTruthy();
      expect(Number(eurPersisted!.rate), 'persisted USD→EUR rate = 0.92').toBeCloseTo(0.92, 6);
      expect(eurPersisted!.toCurrency.code, 'persisted toCurrency code = EUR').toBe('EUR');

      // ---- (3) idempotent rewrite: second refresh overwrites same interval, no accumulation ----
      const countBefore = (
        await findExchangeRatesByBase<{ id: string | number }>(
          page,
          USD_ID,
          'id rateType',
        )
      ).filter(r => r.rateType === RATE_TYPE_MIDDLE).length;
      expect(countBefore, 'before 2nd refresh: 2 MIDDLE USD-based rates (CNY + EUR)').toBe(2);

      await callMutationOk(
        page,
        'ErpMdCurrency',
        'refreshRatesFromApi',
        { baseCurrency: 'USD' },
        'rate',
      );
      const countAfter = (
        await findExchangeRatesByBase<{ id: string | number }>(
          page,
          USD_ID,
          'id rateType',
        )
      ).filter(r => r.rateType === RATE_TYPE_MIDDLE).length;
      expect(countAfter, '2nd refresh should overwrite (upsert), not accumulate').toBe(countBefore);

      // 断言 rewrite 后 rate 值仍稳定（mock 确定性 + cache 复用）
      const afterRates = await findExchangeRatesByBase<RateWithCode>(
        page,
        USD_ID,
        'rate rateType fromCurrency{ code } toCurrency{ code }',
      );
      const afterCny = afterRates.find(r => r.rateType === RATE_TYPE_MIDDLE && r.toCurrency.code === 'CNY');
      expect(afterCny, 'USD→CNY MIDDLE row should still exist after rewrite').toBeTruthy();
      expect(Number(afterCny!.rate), 'USD→CNY rate stable after idempotent rewrite = 7.20').toBeCloseTo(7.20, 6);
    } finally {
      // ---- Cleanup (reverse dependency): MIDDLE rate rows → EUR currency ----
      // 仅删本 spec 产物（fromCurrencyId=USD AND rateType=MIDDLE），保留种子 SPOT 行
      await deleteByFilter(
        page,
        'ErpMdExchangeRate',
        andFilter(eqFilter('fromCurrencyId', USD_ID), eqFilter('rateType', RATE_TYPE_MIDDLE)),
      );
      if (eurId !== undefined) {
        await deleteById(page, 'ErpMdCurrency', eurId);
      }
    }
  });
});
