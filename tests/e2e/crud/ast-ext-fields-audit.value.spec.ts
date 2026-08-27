import { test, expect, loginAndNavigate } from './_helper';
import { GraphQLClient, CrudListPage, getEngine } from '../pages';

/**
 * E3.3 + E3.4 assets 扩展能力 E2E（plan 2026-08-26-0735-2 Phase 4，
 * `audit-trail-and-custom-fieldsets.md` §1/§2）：
 *
 *   1. 字段集管理界面（ErpAstAssetModel main 页面渲染 + GraphQL CRUD 写周期）；
 *   2. 实例资产 ext 字段按型号校验负路径（非法键拒绝 + 未指定型号携带值拒绝）；
 *   3. 审计时间轴 `getAssetAuditTrail`（倒序 + CREATE/STATUS_CHANGE 事件 + from/to 快照）。
 *
 * 清理：finally 删除测试资产与型号，保护共享 DB。
 */

const SEED = {
  CURRENCY: '1',
} as const;

const MODEL_DEFS = JSON.stringify([
  { key: 'cpu', label: 'CPU', type: 'string', required: true },
  { key: 'ramGb', label: '内存GB', type: 'number', required: true },
  { key: 'ssd', label: 'SSD', type: 'boolean', required: false },
]);

test.describe('E3.3 asset model fieldset management', () => {
  test('ErpAstAssetModel page renders and CRUD cycle persists extFieldDefs', async ({ page }) => {
    const engine = getEngine();
    const crud = new CrudListPage(page, engine, { entityRoute: 'ErpAstAssetModel' });
    await crud.navigate();
    await crud.waitForList();

    const gql = new GraphQLClient(page);
    const code = `E2E-AST-MDL-${Date.now()}`;
    let modelId: string | null = null;
    try {
      const created = await gql.save<{ id: string; code: string }>(
        'ErpAstAssetModel',
        { code, name: `E2E 型号 ${code}`, extFieldDefs: MODEL_DEFS },
        'id code extFieldDefs',
      );
      modelId = created?.id ?? null;
      expect(modelId, 'ErpAstAssetModel: create should persist').toBeTruthy();
      expect(created?.extFieldDefs, 'ErpAstAssetModel: extFieldDefs should round-trip').toContain('"cpu"');

      const updated = await gql.update(
        'ErpAstAssetModel',
        { id: modelId, remark: 'E2E updated remark' },
        'id remark',
      );
      expect(updated?.remark, 'ErpAstAssetModel: update should persist remark').toBe('E2E updated remark');
    } finally {
      if (modelId) {
        await gql.delete('ErpAstAssetModel', modelId);
      }
    }
    const gone = modelId ? await gql.get('ErpAstAssetModel', modelId, 'id') : null;
    expect(gone, 'ErpAstAssetModel: delete should logically remove row').toBeFalsy();
  });
});

test.describe('E3.3 asset ext field validation (instance negative paths)', () => {
  test('asset save with undeclared ext key / values without model is rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstAssetModel-main');
    const gql = new GraphQLClient(page);

    const modelCode = `E2E-AST-MDL-${Date.now()}`;
    const assetCode = `E2E-AST-EXT-${Date.now()}`;
    let modelId: string | null = null;
    let assetId: string | null = null;
    try {
      const model = await gql.save<{ id: string }>(
        'ErpAstAssetModel',
        { code: modelCode, name: `E2E 型号 ${modelCode}`, extFieldDefs: MODEL_DEFS },
        'id',
      );
      modelId = model?.id ?? null;
      expect(modelId, 'seed model should persist').toBeTruthy();

      // 负路径（GraphQL 变量形式承载 Map data，错误信封原样返回）
      const saveExpectError = async (data: Record<string, unknown>, token: string) => {
        const r: any = await (gql as any).post(
          'mutation($d:ErpAstAsset__save_input){ ErpAstAsset__save(data:$d){ id } }',
          { d: data },
        );
        expect(r?.errors?.length, `${token} should be rejected`).toBeGreaterThan(0);
        expect(
          String(r?.extensions?.['nop-error-code'] ?? ''),
          `${token} rejection carries errorCode`,
        ).toContain(token);
        return r;
      };

      // 负路径 1：非法键（未在型号 extFieldDefs 声明）
      await saveExpectError(
        {
          code: assetCode, name: `E2E 资产 ${assetCode}`,
          acquisitionDate: '2026-08-01', originalValue: 10000, currentValue: 10000,
          status: 'DRAFT', currencyId: SEED.CURRENCY,
          modelId, extFieldValues: '{"cpu":"i7","unknownKey":1}',
        },
        'ext-field.not-declared',
      );

      // 负路径 2：未指定型号却携带扩展字段值
      await saveExpectError(
        {
          code: assetCode + '-2', name: `E2E 资产 ${assetCode}-2`,
          acquisitionDate: '2026-08-01', originalValue: 10000, currentValue: 10000,
          status: 'DRAFT', currencyId: SEED.CURRENCY,
          extFieldValues: '{"any":"value"}',
        },
        'ext-field.without-model',
      );

      // 正路径：按型号声明校验通过
      const ok = await gql.save<{ id: string }>(
        'ErpAstAsset',
        {
          code: assetCode, name: `E2E 资产 ${assetCode}`,
          acquisitionDate: '2026-08-01', originalValue: 10000, currentValue: 10000,
          status: 'DRAFT', currencyId: SEED.CURRENCY,
          modelId, extFieldValues: '{"cpu":"i7","ramGb":32,"ssd":true}',
        },
        'id',
      );
      assetId = ok?.id ?? null;
      expect(assetId, 'valid ext values should persist').toBeTruthy();
    } finally {
      if (assetId) await gql.delete('ErpAstAsset', assetId);
      if (modelId) await gql.delete('ErpAstAssetModel', modelId);
    }
  });
});

test.describe('E3.8 asset audit trail', () => {
  test('getAssetAuditTrail returns reverse-chronological timeline with from/to snapshots', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstAsset-main');
    const gql = new GraphQLClient(page);

    const code = `E2E-AST-AUD-${Date.now()}`;
    let assetId: string | null = null;
    try {
      const created = await gql.save<{ id: string }>(
        'ErpAstAsset',
        {
          code, name: `E2E 资产 ${code}`,
          acquisitionDate: '2026-08-01', originalValue: 10000, currentValue: 10000,
          status: 'DRAFT', currencyId: SEED.CURRENCY,
        },
        'id',
      );
      assetId = created?.id ?? null;
      expect(assetId, 'asset should persist').toBeTruthy();

      // 触发 STATUS_CHANGE（DRAFT → IN_SERVICE）
      await gql.update('ErpAstAsset', { id: assetId, status: 'IN_SERVICE' }, 'id status');
      // 触发 UPDATE（信息字段 diff）
      await gql.update('ErpAstAsset', { id: assetId, name: `E2E 资产 ${code}-改名` }, 'id name');

      const resp: any = await gql.raw(
        'query($assetId:String){ ErpAstAsset__getAssetAuditTrail(assetId:$assetId) }',
        { assetId },
      );
      const trail = resp?.data?.ErpAstAsset__getAssetAuditTrail;
      expect(Array.isArray(trail), 'trail should be an array').toBe(true);
      expect(trail.length, 'CREATE + STATUS_CHANGE + UPDATE events should be recorded').toBeGreaterThanOrEqual(3);

      // 倒序：最新事件在前
      const types: string[] = trail.map((r: any) => r.eventType);
      expect(types[0], 'latest event first (UPDATE after STATUS_CHANGE)').toBe('UPDATE');
      expect(types, 'CREATE event present').toContain('CREATE');
      expect(types, 'STATUS_CHANGE event present').toContain('STATUS_CHANGE');

      const statusChange = trail.find((r: any) => r.eventType === 'STATUS_CHANGE');
      expect(statusChange?.fromStatus, 'STATUS_CHANGE fromStatus snapshot').toBe('DRAFT');
      expect(statusChange?.toStatus, 'STATUS_CHANGE toStatus snapshot').toBe('IN_SERVICE');

      const createEvent = trail.find((r: any) => r.eventType === 'CREATE');
      expect(createEvent?.fromStatus ?? null, 'CREATE has no from snapshot').toBeNull();
      expect(createEvent?.toStatus, 'CREATE toStatus=DRAFT').toBe('DRAFT');
    } finally {
      if (assetId) await gql.delete('ErpAstAsset', assetId);
    }
  });
});
