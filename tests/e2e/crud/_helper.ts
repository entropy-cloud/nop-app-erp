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
