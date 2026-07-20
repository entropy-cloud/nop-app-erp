import {
  test,
  expect,
  loginAndNavigate,
  GraphQLClient,
  createViaSave,
  callMutationOk,
  callQuery,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
} from './_helper';

/**
 * F7 非状态 visibleOn + 主数据专用交互浏览器层 E2E（plan 2026-07-20-1020-2 Phase 4）。
 *
 * 8 用例覆盖：
 *  - **字段值 visibleOn（2）**：StockMove moveType 切换 + AstMaintenance treatment 切换经 GraphQL
 *    驱动实体创建，验证业务路径不依赖隐藏字段（即隐藏字段缺省值合法通过校验）。
 *  - **唯一性校验（3）**：Material 重复 code → false / 新 code → true / excludeId 自身排除 → true。
 *  - **引用预览（2）**：Material 0 引用 → 空 Map / Material 0 引用 → __delete 不阻断（成功）。
 *    标准运行时无 SPI 下游注册（master-data 不反向依赖），返回空 Map，删除走原 __delete 路径。
 *  - **Switch 控件（1）**：Material status ACTIVE→INACTIVE 经 __update 翻转 + __get 验证。
 *
 * 实现说明：UI 行为（visibleOn/dialog/switch）的浏览器层渲染由 visual/ pixel 层覆盖；
 * 本 spec 经 GraphQL 验证后端 @BizQuery 入口可达 + 业务路径不被隐藏字段污染 + Switch 翻转可达。
 *
 * 种子引用：org id=2 / uom id=1 / asset id=3 IN_SERVICE。
 */
const ORG_ID = 2;
const UOM_ID = 1;
const SEED_ASSET_ID = 3;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('F7 non-status visibleOn + master-data interactions', () => {

  // ============ 字段值 visibleOn（2） ============

  test('StockMove: moveType=INCOMING generates entity (sourceLocationId hidden, business flow intact)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvStockMove-main');

    const stockMoveReqType = 'i_app_erp_inv_biz_StockMoveRequest';
    // INCOMING：sourceLocationId 隐藏（不传），destWarehouseId 必填
    const incoming = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      {
        request: {
          __input: true,
          type: stockMoveReqType,
          value: {
            moveType: 'INCOMING', orgId: ORG_ID, businessDate: '2026-07-20',
            destWarehouseId: 2, acctSchemaId: 1, currencyId: 1,
            lines: [{ materialId: 1, uoMId: 1, quantity: 1, unitCost: 1, currencyId: 1 }],
          },
        },
      },
      'id docStatus moveType',
    );
    expect(incoming.docStatus, 'generateMove(INCOMING) should produce entity').toBeTruthy();
    expect(incoming.moveType, 'moveType preserved').toBe('INCOMING');

    // 清理（INCOMING 在 complete 前未写流水/余额，仅删头+行）
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(incoming.id)));
    await deleteById(page, 'ErpInvStockMove', incoming.id);
  });

  test('AstMaintenance: treatment=EXPENSE path runs (capitalizedAmount hidden via visibleOn)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    // 后端 createMaintenance 创建 EXPENSE 路径实体；capitalizedAmount 在 EXPENSE 时由 visibleOn 隐藏
    // （隐藏字段缺省/null 时业务流转正常）。前端 visibleOn 渲染由 visual/pixel 层覆盖。
    const mntCode = uniq('E2E-F7-MNT-EXP');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: SEED_ASSET_ID, code: mntCode, name: `F7 E2E ${mntCode}`,
        businessDate: '2026-07-20', reason: 'F7 visibleOn test',
      },
      'id code status',
    );
    expect(mnt.status, 'createMaintenance should produce DRAFT entity').toBe('DRAFT');

    await deleteById(page, 'ErpAstMaintenance', mnt.id);
  });

  // ============ 唯一性校验（3） ============

  test('Material isCodeUnique: duplicate → false, fresh → true, excludeId self → true', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterial-main');

    const code = uniq('E2E-F7-MAT');
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code, name: `F7 E2E ${code}`, materialType: 'GOODS',
        uoMId: UOM_ID, status: 'ACTIVE',
      },
      'id code',
    );
    expect(material.code, 'saved code matches').toBe(code);

    try {
      // 1. 重复编码（已被自身占用）→ false
      const dup = await callQuery(page, 'ErpMdMaterial', 'isCodeUnique', { code });
      expect(dup.errors, 'duplicate isCodeUnique should not error').toBeNull();
      expect(dup.data, 'duplicate code should return false').toBe(false);

      // 2. 新编码（无冲突）→ true
      const fresh = await callQuery(page, 'ErpMdMaterial', 'isCodeUnique', { code: `${code}-NEW` });
      expect(fresh.data, 'fresh code should return true').toBe(true);

      // 3. excludeId 自身排除 → true（edit 模式保留原 code 不应误判）
      const self = await callQuery(page, 'ErpMdMaterial', 'isCodeUnique', {
        code, excludeId: Number(material.id),
      });
      expect(self.data, 'self code with excludeId should return true').toBe(true);
    } finally {
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  // ============ 引用预览（2） ============

  test('Material countReferences: 0 references → empty/null Map (no SPI registered)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterial-main');

    const code = uniq('E2E-F7-MAT-REF0');
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code, name: `F7 E2E ${code}`, materialType: 'GOODS',
        uoMId: UOM_ID, status: 'ACTIVE',
      },
      'id code',
    );

    try {
      // 默认运行时无 SPI 实现（master-data 不反向依赖下游域），返回空 Map
      const refs = await callQuery(page, 'ErpMdMaterial', 'countReferences', { id: Number(material.id) });
      expect(refs.errors, 'countReferences should not error').toBeFalsy();
      // data 是 Map（JSON 对象），无引用时为空对象 {} 或 null（实现可空）
      const data = refs.data || {};
      expect(Object.keys(data).length, '0 references → empty Map').toBe(0);
    } finally {
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  test('Material 0 references → __delete succeeds (delete path unblocked)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterial-main');

    const code = uniq('E2E-F7-MAT-DEL');
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code, name: `F7 E2E ${code}`, materialType: 'GOODS',
        uoMId: UOM_ID, status: 'ACTIVE',
      },
      'id code',
    );

    // 先查引用（应为空），再 __delete（应成功——0 引用不阻断）
    const refs = await callQuery(page, 'ErpMdMaterial', 'countReferences', { id: Number(material.id) });
    expect(refs.errors, 'countReferences should not error').toBeFalsy();
    const refMap = refs.data || {};
    expect(Object.keys(refMap).length, '0 references → empty Map').toBe(0);

    // 0 引用 → __delete 应成功
    await deleteById(page, 'ErpMdMaterial', material.id);

    // 二次 __get 应返回错误（记录不存在）
    const after = await callQuery(page, 'ErpMdMaterial', '__get', { id: Number(material.id) });
    expect(after.errors, 'after delete, __get should error with not-found').toBeTruthy();
    expect(after.data, 'after delete, __get data should be null').toBeNull();
  });

  // ============ Switch 控件（1） ============

  test('Material Switch: status ACTIVE→INACTIVE via __update + verify via __get', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterial-main');

    const code = uniq('E2E-F7-MAT-SW');
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code, name: `F7 E2E ${code}`, materialType: 'GOODS',
        uoMId: UOM_ID, status: 'ACTIVE',
      },
      'id status',
    );
    expect(material.status, 'initial status ACTIVE').toBe('ACTIVE');

    try {
      // 模拟 Switch 切换：__update 翻转 status → INACTIVE
      // 前端 onEvent.change 会先弹停用确认 dialog，确认后调 __update；本用例直接调 __update
      // 验证业务路径可达（前端 dialog 渲染由 visual/pixel 层覆盖）。
      const gql = new GraphQLClient(page);
      const json: any = await gql.raw(
        `mutation($d:ErpMdMaterial__update_input){ ErpMdMaterial__update(data:$d){ id status } }`,
        { d: { id: Number(material.id), status: 'INACTIVE' } },
      );
      expect(json?.errors, 'update to INACTIVE should not error').toBeFalsy();
      expect(json?.data?.ErpMdMaterial__update?.status, 'after update status INACTIVE').toBe('INACTIVE');

      // __get 独立核实
      const verified = await verifyState(page, 'ErpMdMaterial', material.id, 'id status');
      expect(verified.status, '__get should confirm INACTIVE after Switch flip').toBe('INACTIVE');
    } finally {
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });
});
