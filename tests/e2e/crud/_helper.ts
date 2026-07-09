import { test, expect, loginAndNavigate } from '../fixtures';

export interface CrudSmokeOptions {
  entityRoute: string;
  domain: string;
  addFormField: string;
}

export function runCrudListSmoke(opts: CrudSmokeOptions): void {
  const { entityRoute, domain, addFormField } = opts;

  test.describe(`${domain} CRUD list/form smoke`, () => {
    test('renders list DOM, add button, GraphQL 200, and add form field', async ({ page }) => {
      const graphqlResponses: number[] = [];
      page.on('response', (resp) => {
        if (resp.url().includes('/graphql')) {
          graphqlResponses.push(resp.status());
        }
      });

      await loginAndNavigate(page, `/${entityRoute}-main`);

      const mainContent = page
        .locator('#main-content, main, .cxd-Page, [role="main"]')
        .first();
      await mainContent.waitFor({ state: 'visible', timeout: 15_000 });

      const tableOrCrud = mainContent.locator('.cxd-Crud, .cxd-Table, table').first();
      await tableOrCrud.waitFor({ state: 'visible', timeout: 15_000 });

      const addBtn = mainContent.locator('button:has(.fa-plus)').first();
      await addBtn.waitFor({ state: 'visible', timeout: 15_000 });

      expect(
        graphqlResponses.length,
        `${domain}: should have at least one GraphQL call`,
      ).toBeGreaterThan(0);
      for (const status of graphqlResponses) {
        expect(status, `${domain}: GraphQL should return 200`).toBe(200);
      }

      await addBtn.click({ force: true });

      const formField = page.locator(`input[name="${addFormField}"]`).first();
      await formField.waitFor({ state: 'visible', timeout: 15_000 });
      expect(await formField.isVisible(), `${domain}: add form field ${addFormField} visible`).toBe(true);
    });
  });
}

export interface CrudListValueOptions {
  entityName: string;
  route: string;
  expectedCount: number;
  expectedTokens: string[];
  fields?: string;
}

export function assertCrudListValues(opts: CrudListValueOptions): void {
  const { entityName, route, expectedCount, expectedTokens } = opts;
  const selection = opts.fields || 'id code';

  test.describe(`${entityName} CRUD list seed values`, () => {
    test('findPage returns seed rows containing expected tokens', async ({ page }) => {
      await loginAndNavigate(page, route);

      const resp = await page.request.post('/graphql', {
        data: {
          query: `{ ${entityName}__findPage(query:{offset:0,limit:200}){ total items{ ${selection} } } }`,
        },
      });
      expect(resp.status(), `${entityName}: findPage GraphQL should return 200`).toBe(200);

      const json = await resp.json();
      const result = json?.data?.[`${entityName}__findPage`];
      expect(result, `${entityName}: findPage result should be present`).toBeTruthy();
      expect(
        result.items.length,
        `${entityName}: findPage items should be >= ${expectedCount} (seed rows)`,
      ).toBeGreaterThanOrEqual(expectedCount);

      const body = JSON.stringify(result.items);
      for (const token of expectedTokens) {
        expect(
          body.includes(token),
          `${entityName}: findPage items should contain token "${token}"`,
        ).toBe(true);
      }
    });
  });
}

export interface CrudWriteOptions {
  entityName: string;
  route: string;
  fields: Record<string, string>;
  editField: { name: string; value: string };
}

const MAX_SEQ_RETRIES = 30;

export function runCrudWriteCycle(opts: CrudWriteOptions): void {
  const { entityName, route, fields, editField } = opts;
  const saveInput = `${entityName}__save_input`;
  const updateInput = `${entityName}__update_input`;

  test.describe(`${entityName} CRUD write cycle`, () => {
    test('create/update/delete persists correctly (verified via findPage/get)', async ({ page }) => {
      await loginAndNavigate(page, route);

      const baseCode = `E2E-${entityName}-${Date.now()}`;

      let createdId: string | null = null;
      for (let attempt = 0; attempt < MAX_SEQ_RETRIES; attempt++) {
        const tryCode = attempt === 0 ? baseCode : `${baseCode}-${attempt}`;
        const resp = await page.request.post('/graphql', {
          data: {
            query: `mutation($d:${saveInput}){ ${entityName}__save(data:$d){ id code } }`,
            variables: { d: { code: tryCode, ...fields } },
          },
        });
        const json = await resp.json();
        const saved = json?.data?.[`${entityName}__save`];
        if (saved?.id) {
          createdId = saved.id;
          break;
        }
      }
      expect(createdId, `${entityName}: create should succeed after sequence warm-up`).toBeTruthy();

      const verifyCreate = await page.request.post('/graphql', {
        data: { query: `{ ${entityName}__get(id:${createdId}){ id ${editField.name} } }` },
      });
      const createJson = await verifyCreate.json();
      expect(
        createJson?.data?.[`${entityName}__get`]?.id,
        `${entityName}: created entity should be retrievable via get`,
      ).toBe(createdId);

      const updateResp = await page.request.post('/graphql', {
        data: {
          query: `mutation($d:${updateInput}){ ${entityName}__update(data:$d){ id ${editField.name} } }`,
          variables: { d: { id: createdId, [editField.name]: editField.value } },
        },
      });
      const updateJson = await updateResp.json();
      expect(
        updateJson?.data?.[`${entityName}__update`]?.[editField.name],
        `${entityName}: update should return new ${editField.name} value`,
      ).toBe(editField.value);

      const verifyUpdate = await page.request.post('/graphql', {
        data: { query: `{ ${entityName}__get(id:${createdId}){ id ${editField.name} } }` },
      });
      const updatedJson = await verifyUpdate.json();
      expect(
        updatedJson?.data?.[`${entityName}__get`]?.[editField.name],
        `${entityName}: get after update should reflect new ${editField.name}`,
      ).toBe(editField.value);

      const deleteResp = await page.request.post('/graphql', {
        data: { query: `mutation{ ${entityName}__delete(id:${createdId}) }` },
      });
      const deleteJson = await deleteResp.json();
      expect(
        deleteJson?.data?.[`${entityName}__delete`],
        `${entityName}: delete should return true`,
      ).toBe(true);

      const verifyDelete = await page.request.post('/graphql', {
        data: { query: `{ ${entityName}__get(id:${createdId}){ id } }` },
      });
      const deletedJson = await verifyDelete.json();
      expect(
        deletedJson?.data?.[`${entityName}__get`],
        `${entityName}: get after delete should be absent (logically deleted)`,
      ).toBeFalsy();
    });
  });
}
