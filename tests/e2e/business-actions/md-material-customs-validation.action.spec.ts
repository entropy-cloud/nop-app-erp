import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  verifyState,
  deleteById,
} from './_helper';
import { GraphQLClient } from '../pages';
import type { Page } from '@playwright/test';

/**
 * master-data 物料报关记录 3 校验钩子浏览器层 E2E（plan 2026-07-26-1500-1）。
 *
 * 验证 C2 跨境贸易扩展 `ErpMdMaterialCustomsBizModel` 3 校验钩子经 GraphQL `__save`/`__update`
 * 写路径全栈可达（CrudBizModel `defaultPrepareSave/Update` → `validateOnPersist` 统一触发，
 * 对齐 master-data.write.spec.ts GraphQL 写路径范式 + sal-date-range-validation.action.spec.ts
 * 拒绝路径 `saveRaw` 裸 mutation 断言范式）：
 *
 * (1) **正路径** —— 合法 `ErpMdMaterialCustoms__save`（declarationNo 唯一 + partnerId=CUSTOMS_BROKER
 *     partner + sourceBillType 非空）→ `data` 非空 + `errors` null + `__get` 反查持久化字段。
 * (2) **declarationNo 重复守卫** —— 第二次 `__save` 同 declarationNo → `errors` 含「报关单号」
 *     语义 token（ERR_CUSTOMS_DECLARATION_NO_DUPLICATE）+ `data` null。
 * (3) **partnerType 非 CUSTOMS_BROKER 守卫** —— `__save` partnerId=种子 CUSTOMER partner
 *     → `errors` 含「报关行」语义 token（ERR_PARTNER_NOT_CUSTOMS_BROKER）。
 * (4) **sourceBill 均空守卫** —— `__save` sourceBillType=null + sourceBillCode=null → `errors`
 *     含「业务单据」语义 token（ERR_CUSTOMS_SOURCE_BILL_REQUIRED）。
 * (5) **`__update` 自身排除** —— 正路径建记录后 `__update` 修改 remark（保持 declarationNo 不变）
 *     → 成功（`enforceDeclarationNoUnique` 经 `entity.getId()` 排除自身，对齐 0500-3
 *     `enforceMutex selfId` 范式）。
 *
 * 自包含 setup：每测试经 `ErpMdPartner__save` 建 CUSTOMS_BROKER partner（唯一 code 前缀
 * `E2E-MC-BROKER-`，种子 0 CUSTOMS_BROKER 须自建），materialId 复用种子 MAT_1（id=1）。
 * cleanup 经 `deleteById` 删本 spec 产物（customs 记录 + 自建 partner），保护共享 DB 基线。
 */

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

const MAT_1 = 1; // 种子 MAT-001（FINISHED_PRODUCT/ACTIVE），material FK 复用

/**
 * 经 GraphQL `__save` 原始 mutation（不经 createViaSave 的成功断言），返回完整 envelope。
 * 用于拒绝路径——`createViaSave` 内置 `expect(errors).toBeNull()` 会在拒绝时失败，
 * 故拒绝路径需直取 `{data, errors, json}` 自行断言。对齐 sal-date-range-validation 范式。
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

async function buildCustomsBroker(page: Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page,
    'ErpMdPartner',
    {
      code: uniq(`E2E-MC-BROKER-${tag}`),
      name: `E2E报关行${tag}`,
      partnerType: 'CUSTOMS_BROKER',
      status: 'ACTIVE',
    },
    'id',
  );
}

function customsPayload(
  tag: string,
  partnerId: string | number,
  extra?: Record<string, unknown>,
): Record<string, unknown> {
  return {
    code: uniq(`E2E-MC-${tag}`),
    materialId: MAT_1,
    partnerId,
    declarationNo: uniq(`DECL-E2E-MC-${tag}`),
    declarationDate: '2026-07-26',
    qtyDeclared: 100,
    uomDeclared: '千克',
    amountDeclared: 10000.0,
    sourceBillType: 'PURCHASE_RECEIVE',
    sourceBillCode: uniq(`PR-E2E-MC-${tag}`),
    ...extra,
  };
}

test.describe('master-data 物料报关记录 3 校验钩子（正路径 + 3 守卫拒绝 + __update 自身排除）', () => {
  test('(1) 正路径：合法 __save 持久化成功 + __get 反查', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterialCustoms-main');

    const broker = await buildCustomsBroker(page, 'POS');
    const createdIds: string[] = [];

    try {
      const payload = customsPayload('POS', broker.id);
      const saved = await createViaSave(
        page,
        'ErpMdMaterialCustoms',
        payload,
        'id declarationNo partnerId sourceBillType sourceBillCode',
      );
      expect(saved.id, 'positive __save should persist and return id').toBeTruthy();
      createdIds.push(saved.id);
      expect(saved.declarationNo, 'persisted declarationNo matches input').toBe(payload.declarationNo);
      expect(saved.sourceBillType, 'persisted sourceBillType matches input').toBe('PURCHASE_RECEIVE');

      // __get 独立反查持久化字段
      const got = await verifyState(
        page,
        'ErpMdMaterialCustoms',
        saved.id,
        'id declarationNo partnerId sourceBillType sourceBillCode',
      );
      expect(got.declarationNo, '__get confirms declarationNo persisted').toBe(payload.declarationNo);
      expect(String(got.partnerId), '__get confirms partnerId persisted').toBe(String(broker.id));
      expect(got.sourceBillType, '__get confirms sourceBillType persisted').toBe('PURCHASE_RECEIVE');
    } finally {
      for (const id of createdIds) await deleteById(page, 'ErpMdMaterialCustoms', id);
      await deleteById(page, 'ErpMdPartner', broker.id);
    }
  });

  test('(2) declarationNo 重复守卫拒绝', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterialCustoms-main');

    const broker = await buildCustomsBroker(page, 'DUP');
    const createdIds: string[] = [];

    try {
      // 第一次保存成功（唯一 declarationNo）
      const dupDeclarationNo = `DECL-E2E-MC-DUP-${Date.now()}`;
      const first = await createViaSave(
        page,
        'ErpMdMaterialCustoms',
        { ...customsPayload('DUP1', broker.id), declarationNo: dupDeclarationNo },
        'id',
      );
      createdIds.push(first.id);

      // 第二次同 declarationNo（不同 code）→ 拒绝 ERR_CUSTOMS_DECLARATION_NO_DUPLICATE
      const rej = await saveRaw(page, 'ErpMdMaterialCustoms', {
        ...customsPayload('DUP2', broker.id),
        declarationNo: dupDeclarationNo,
      });
      expect(rej.errors, 'duplicate declarationNo __save should be rejected').toBeTruthy();
      expect(
        JSON.stringify(rej.errors),
        'reject should carry declaration-no semantic token (ERR_CUSTOMS_DECLARATION_NO_DUPLICATE message 含「报关单号」)',
      ).toContain('报关单号');
      expect(rej.data, 'rejected __save should return null data').toBeNull();
    } finally {
      for (const id of createdIds) await deleteById(page, 'ErpMdMaterialCustoms', id);
      await deleteById(page, 'ErpMdPartner', broker.id);
    }
  });

  test('(3) partnerType 非 CUSTOMS_BROKER 守卫拒绝', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterialCustoms-main');

    // 种子 CUSTOMER partner id=1（CUST-001，partnerType=CUSTOMER）—— 不新建（仅引用读）
    const CUSTOMER_PARTNER_ID = 1;
    const createdIds: string[] = [];

    try {
      const rej = await saveRaw(page, 'ErpMdMaterialCustoms', {
        ...customsPayload('PART', CUSTOMER_PARTNER_ID),
      });
      expect(rej.errors, 'non-CUSTOMS_BROKER partner __save should be rejected').toBeTruthy();
      expect(
        JSON.stringify(rej.errors),
        'reject should carry customs-broker semantic token (ERR_PARTNER_NOT_CUSTOMS_BROKER message 含「报关行」)',
      ).toContain('报关行');
      expect(rej.data, 'rejected __save should return null data').toBeNull();
    } finally {
      for (const id of createdIds) await deleteById(page, 'ErpMdMaterialCustoms', id);
    }
  });

  test('(4) sourceBill 均空守卫拒绝', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterialCustoms-main');

    const broker = await buildCustomsBroker(page, 'SRC');
    const createdIds: string[] = [];

    try {
      // sourceBillType=null + sourceBillCode=null → 拒绝 ERR_CUSTOMS_SOURCE_BILL_REQUIRED
      const rej = await saveRaw(page, 'ErpMdMaterialCustoms', {
        ...customsPayload('SRC', broker.id, { sourceBillType: null, sourceBillCode: null }),
      });
      expect(rej.errors, 'both-null sourceBill __save should be rejected').toBeTruthy();
      expect(
        JSON.stringify(rej.errors),
        'reject should carry source-bill semantic token (ERR_CUSTOMS_SOURCE_BILL_REQUIRED message 含「业务单据」)',
      ).toContain('业务单据');
      expect(rej.data, 'rejected __save should return null data').toBeNull();
    } finally {
      for (const id of createdIds) await deleteById(page, 'ErpMdMaterialCustoms', id);
      await deleteById(page, 'ErpMdPartner', broker.id);
    }
  });

  test('(5) __update 自身排除：修改 remark（保持 declarationNo 不变）通过', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdMaterialCustoms-main');

    const broker = await buildCustomsBroker(page, 'UPD');

    try {
      // 正路径建记录
      const payload = customsPayload('UPD', broker.id);
      const saved = await createViaSave(
        page,
        'ErpMdMaterialCustoms',
        payload,
        'id declarationNo remark',
      );

      // __update 修改 remark（保持 declarationNo 不变）→ enforceDeclarationNoUnique 经
      // entity.getId() 排除自身（更新场景 entity 已含 id），查询命中自身记录但 id 相等 → 通过
      const gql = new GraphQLClient(page);
      const json: any = await gql.raw(
        `mutation($d:ErpMdMaterialCustoms__update_input){ ErpMdMaterialCustoms__update(data:$d){ id declarationNo remark } }`,
        { d: { id: saved.id, remark: 'E2E-UPDATED-REMARK' } },
      );
      expect(json?.errors, '__update with self-exclusion should not return errors').toBeFalsy();
      expect(
        json?.data?.ErpMdMaterialCustoms__update?.remark,
        '__update persists remark',
      ).toBe('E2E-UPDATED-REMARK');
      expect(
        json?.data?.ErpMdMaterialCustoms__update?.declarationNo,
        '__update preserves declarationNo',
      ).toBe(payload.declarationNo);

      // __get 独立断言
      const got = await verifyState(
        page,
        'ErpMdMaterialCustoms',
        saved.id,
        'id declarationNo remark',
      );
      expect(got.remark, '__get confirms remark updated').toBe('E2E-UPDATED-REMARK');
      expect(got.declarationNo, '__get confirms declarationNo unchanged').toBe(payload.declarationNo);

      await deleteById(page, 'ErpMdMaterialCustoms', saved.id);
    } finally {
      await deleteById(page, 'ErpMdPartner', broker.id);
    }
  });
});
