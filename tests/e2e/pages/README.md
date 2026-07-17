# PageObject 基础设施 — E2E 测试

## 架构

```
tests/e2e/pages/
├── index.ts          # 公共导出
├── engine.ts         # 全局引擎工厂（读 E2E_ENGINE 环境变量）
├── types.ts          # 共享类型和常量
├── AmisAdapter.ts    # AMIS 引擎适配器（.cxd-* / input[name]）
├── FluxAdapter.ts    # Flux 引擎适配器（data-slot / data-testid / getByLabel）
├── Navigation.ts     # 登录 + 页面导航
├── GraphQLClient.ts  # 中心化 GraphQL 操作
├── Page.ts           # 基类
├── CrudListPage.ts   # CRUD 列表页面对象
├── FormDialog.ts     # 表单对话框对象
└── README.md
```

## 引擎切换

所有测试通过环境变量 `E2E_ENGINE` 全局切换前端引擎，无需修改测试代码：

```bash
# 默认：AMIS 引擎
npx playwright test

# Flux 引擎
E2E_ENGINE=flux npx playwright test
```

引擎选择逻辑集中在 `pages/engine.ts` 的 `getEngine()` 函数中，由 `fixtures.ts` 自动注入为 Playwright fixture `engine`。所有 `_helper.ts` 和 PageObject 内部调用 `getEngine()`，不硬编码引擎实例。

## 设计原则

1. **按业务字段名操作** — 测试代码通过实体字段名读写值，不直接操作 DOM
2. **引擎适配器模式** — `EngineAdapter` 接口封装了 AMIS/Flux 的 DOM 差异；`E2E_ENGINE` 环境变量全局切换
3. **条件等待** — 所有显式等待使用 engine 提供的容器定位器，消除硬编码 15s 超时
4. **GraphQL 中心化** — `GraphQLClient` 封装所有 `/graphql` 通信，避免测试直接拼接查询语句

## 使用示例

### CRUD 冒烟测试（替代现有 crud/*.smoke.spec.ts）

```typescript
import { test, expect } from '@playwright/test';
import { CrudListPage, AmisAdapter } from '../pages';

test.describe('master-data CRUD list smoke', () => {
  test('renders list, GraphQL 200, and add dialog', async ({ page }) => {
    const crud = new CrudListPage(page, {
      entityRoute: 'ErpMdPartner',
      domain: 'master-data',
    }, new AmisAdapter());

    await crud.navigate();
    await crud.waitForList();

    // 断言 GraphQL 请求都返回 200
    const responses: number[] = [];
    page.on('response', (resp) => {
      if (resp.url().includes('/graphql')) {
        responses.push(resp.status());
      }
    });
    await crud.waitForList();
    for (const status of responses) {
      expect(status).toBe(200);
    }

    // 点击新增，验证对话框出现
    const dialog = await crud.clickAdd();
    await dialog.waitForVisible();
    await dialog.setField('code', 'E2E-TEST-001');
    await dialog.setField('name', 'Test Partner');
    await dialog.submit();
  });
});
```

### CRUD 写周期（替代现有 crud/*.write.spec.ts）

```typescript
import { test, expect } from '@playwright/test';
import { CrudListPage, GraphQLClient, AmisAdapter } from '../pages';

test.describe('ErpMdPartner write cycle', () => {
  test('create/update/delete via GraphQL', async ({ page }) => {
    const gql = new GraphQLClient(page);
    const crud = new CrudListPage(page, { entityRoute: 'ErpMdPartner' }, new AmisAdapter());
    await crud.navigate();

    // CREATE
    const saved = await gql.save('ErpMdPartner', {
      code: `E2E-${Date.now()}`,
      name: 'E2E Test',
      partnerType: 'CUSTOMER',
      status: 'ACTIVE',
    }, 'id code');
    expect(saved.id).toBeTruthy();

    // GET verify
    const fetched = await gql.get('ErpMdPartner', saved.id, 'id code name');
    expect(fetched.name).toBe('E2E Test');

    // UPDATE
    await gql.callMutationOk('ErpMdPartner', 'update', {
      id: saved.id, name: 'Updated',
    }, 'id name');

    // DELETE
    const deleted = await gql.delete('ErpMdPartner', saved.id);
    expect(deleted).toBe(true);
  });
});
```

### CRUD 列表数值断言（替代现有 crud/*.list-value.spec.ts）

```typescript
import { test, expect } from '@playwright/test';
import { CrudListPage, GraphQLClient, AmisAdapter } from '../pages';

test.describe('ErpMdPartner list seed values', () => {
  test('contains expected seed data', async ({ page }) => {
    const gql = new GraphQLClient(page);

    const items = await gql.findPage('ErpMdPartner', 'id code partnerType status');
    expect(items.length).toBeGreaterThanOrEqual(5);

    const body = JSON.stringify(items);
    expect(body).toContain('CUST-001');
    expect(body).toContain('SUP-001');
  });
});
```

## 迁移指南 — AMIS → Flux

当前端切换到 Flux 引擎时，只需设置环境变量：

```bash
E2E_ENGINE=flux npx playwright test
```

`engine.ts` 的 `getEngine()` 会自动返回 `FluxAdapter`，所有 PageObject 和测试代码无需修改。
`GraphQLClient` 和 `Navigation` 完全引擎无关，不受影响。

Flux 适配器预期使用以下 data 属性：
- `[data-slot="crud-table"]` — CRUD 表格容器
- `[data-slot="dialog-surface"]` — 弹窗表面
- `[data-field="fieldName"]` — 单元格
- `data-testid="btn-*"` — 操作按钮
- `getByLabel('Field Label')` — 表单字段

## 常量配置

| 常量 | 默认值 | 说明 |
|------|--------|------|
| `DEFAULT_LIST_TIMEOUT` | 15000ms | 列表渲染等待 |
| `DEFAULT_DIALOG_TIMEOUT` | 15000ms | 对话框渲染等待 |
| `DEFAULT_NAV_TIMEOUT` | 30000ms | 页面导航等待 |
