/**
 * CRUD 冒烟测试 — PageObject 模式示例
 *
 * 本文件是现有 crud/*.smoke.spec.ts 的 PageObject 重构示例，
 * 展示如何使用 pages/ 基础设施替代 AMIS 类名直接操作。
 *
 * 引擎切换：E2E_ENGINE=flux npx playwright test（全局切换，测试代码无需改动）
 */
import { test, expect } from '../fixtures';
import { CrudListPage, FormDialog, getEngine } from '../pages';

interface CrudSmokeExampleOptions {
  entityRoute: string;
  domain: string;
  addFormField: string;
}

function runCrudListSmokePageObject(opts: CrudSmokeExampleOptions): void {
  const { entityRoute, domain, addFormField } = opts;

  test.describe(`${domain} CRUD list/form smoke (PageObject)`, () => {
    test('renders list DOM, add button, GraphQL 200, and add form field', async ({ page, engine }) => {
      const crud = new CrudListPage(page, { entityRoute, domain }, engine);

      const graphqlResponses: number[] = [];
      page.on('response', (resp) => {
        if (resp.url().includes('/graphql')) {
          graphqlResponses.push(resp.status());
        }
      });

      await crud.navigate();
      await crud.waitForList();
      const addBtn = await crud.getAddButton();
      await expect(addBtn).toBeVisible();

      expect(graphqlResponses.length).toBeGreaterThan(0);
      for (const status of graphqlResponses) {
        expect(status).toBe(200);
      }

      await addBtn.click({ force: true });
      const dialog = new FormDialog(page, engine);
      await dialog.waitForVisible();
      const field = engine.formField(dialog.dialog, addFormField);
      await expect(field).toBeVisible();
    });
  });
}

// 各域按需调用
runCrudListSmokePageObject({
  domain: 'master-data',
  entityRoute: 'ErpMdPartner',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'purchase',
  entityRoute: 'ErpPurOrder',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'sales',
  entityRoute: 'ErpSalOrder',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'finance',
  entityRoute: 'ErpFinVoucher',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'inventory',
  entityRoute: 'ErpInvStockMove',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'manufacturing',
  entityRoute: 'ErpMfgWorkOrder',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'quality',
  entityRoute: 'ErpQaRiskRegister',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'assets',
  entityRoute: 'ErpAstAsset',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'maintenance',
  entityRoute: 'ErpMntVisit',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'projects',
  entityRoute: 'ErpPrjProject',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'crm',
  entityRoute: 'ErpCrmLead',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'cs',
  entityRoute: 'ErpCsTicket',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'hr',
  entityRoute: 'ErpHrEmployee',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'logistics',
  entityRoute: 'ErpLogShipment',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'b2b',
  entityRoute: 'ErpB2bAsn',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'contract',
  entityRoute: 'ErpCtContract',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'drp',
  entityRoute: 'ErpDrpPlan',
  addFormField: 'code',
});

runCrudListSmokePageObject({
  domain: 'aps',
  entityRoute: 'ErpApsOperationOrder',
  addFormField: 'code',
});
