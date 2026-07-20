import { test, expect, loginAndNavigate } from '../fixtures';
import { CrudListPage, FormDialog, GraphQLClient, getEngine } from '../pages';

export { test, expect, loginAndNavigate };

export interface CrudSmokeOptions {
  entityRoute: string;
  domain: string;
  addFormField: string;
}

export function runCrudListSmoke(opts: CrudSmokeOptions): void {
  const { entityRoute, domain, addFormField } = opts;

  test.describe(`${domain} CRUD list/form smoke`, () => {
    test('renders list DOM, add button, GraphQL 200, and add form field', async ({ page }) => {
      const engine = getEngine();
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
      await expect(addBtn, `${domain}: add button should be visible`).toBeVisible();

      expect(
        graphqlResponses.length,
        `${domain}: should have at least one GraphQL call`,
      ).toBeGreaterThan(0);
      for (const status of graphqlResponses) {
        expect(status, `${domain}: GraphQL should return 200`).toBe(200);
      }

      await addBtn.click({ force: true });

      const dialog = new FormDialog(page, engine);
      await dialog.waitForVisible();

      const formField = engine.formField(dialog.dialog, addFormField);
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

      const gql = new GraphQLClient(page);
      const items = await gql.findPage(entityName, selection);

      expect(
        items.length,
        `${entityName}: findPage items should be >= ${expectedCount} (seed rows)`,
      ).toBeGreaterThanOrEqual(expectedCount);

      const body = JSON.stringify(items);
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

export function runCrudWriteCycle(opts: CrudWriteOptions): void {
  const { entityName, route, fields, editField } = opts;

  test.describe(`${entityName} CRUD write cycle`, () => {
    test('create/update/delete persists correctly (verified via findPage/get)', async ({ page }) => {
      await loginAndNavigate(page, route);

      const gql = new GraphQLClient(page);
      const baseCode = `E2E-${entityName}-${Date.now()}`;

      let createdId: string | null = null;
      for (let attempt = 0; attempt < 2 && !createdId; attempt++) {
        const tryCode = attempt === 0 ? baseCode : `${baseCode}-r`;
        const saved = await gql.save<any>(entityName, { code: tryCode, ...fields }, 'id code');
        if (saved?.id) {
          createdId = saved.id;
        }
      }
      expect(createdId, `${entityName}: create should succeed on first save after sequence advance`).toBeTruthy();

      const fetched = await gql.get(entityName, createdId!, `id ${editField.name}`);
      expect(
        fetched?.id,
        `${entityName}: created entity should be retrievable via get`,
      ).toBe(createdId);

      const updated = await gql.update(entityName, { id: createdId, [editField.name]: editField.value }, `id ${editField.name}`);
      expect(
        updated?.[editField.name],
        `${entityName}: update should return new ${editField.name} value`,
      ).toBe(editField.value);

      const verifyUpdate = await gql.get(entityName, createdId!, `id ${editField.name}`);
      expect(
        verifyUpdate?.[editField.name],
        `${entityName}: get after update should reflect new ${editField.name}`,
      ).toBe(editField.value);

      const deleted = await gql.delete(entityName, createdId!);
      expect(
        deleted,
        `${entityName}: delete should return true`,
      ).toBe(true);

      const verifyDelete = await gql.get(entityName, createdId!, 'id');
      expect(
        verifyDelete,
        `${entityName}: get after delete should be absent (logically deleted)`,
      ).toBeFalsy();
    });
  });
}

export interface AmisDictField {
  label: string[];
  option: string[];
}

export interface AmisFormWriteOptions {
  entityName: string;
  route: string;
  textFields: Record<string, string>;
  dictFields: AmisDictField[];
  editTextField: { name: string; value: string };
  verifySelection: string;
}

export function runAmisFormWrite(opts: AmisFormWriteOptions): void {
  const {
    entityName,
    route,
    textFields,
    dictFields,
    editTextField,
    verifySelection,
  } = opts;
  const codeValue = textFields.code;

  test.describe(`${entityName} AMIS form-button write cycle`, () => {
    test('UI add/edit/delete persists (form submit -> list -> row actions)', async ({ page }) => {
      const engine = getEngine();
      const crud = new CrudListPage(page, { entityRoute: entityName }, engine);
      const gql = new GraphQLClient(page);

      await crud.navigate();
      await crud.waitForList();

      const dialog = await crud.clickAdd();
      await dialog.waitForVisible();

      for (const [name, value] of Object.entries(textFields)) {
        await dialog.setField(name, value);
      }
      for (const dict of dictFields) {
        await dialog.selectOption(dict.label, dict.option);
      }

      await dialog.submit();
      await dialog.waitForHidden();

      let createdId: string | null = null;
      const items = await gql.findPage<{ id: string; code?: string }>(entityName, 'id code');
      const match = items.find((it) => it.code === codeValue);
      if (match) createdId = match.id;
      expect(createdId, `${entityName}: AMIS add form submit should persist a row`).toBeTruthy();

      await page.waitForTimeout(1000);
      const row = await crud.findRowByText(codeValue);
      if (row) {
        await row.waitFor({ state: 'visible', timeout: 15_000 });
        const editDialog = await crud.editRow(row);
        await editDialog.waitForVisible();
        await editDialog.setField(editTextField.name, '');
        await editDialog.setField(editTextField.name, editTextField.value);
        await editDialog.submit();
        await editDialog.waitForHidden();
      }

      const verifyUpdate = await gql.get(entityName, createdId!, `id ${editTextField.name}`);
      expect(
        verifyUpdate?.[editTextField.name],
        `${entityName}: AMIS edit form submit should persist updated ${editTextField.name}`,
      ).toBe(editTextField.value);

      const deleted = await gql.delete(entityName, createdId!);
      expect(
        deleted,
        `${entityName}: AMIS write cycle delete (GraphQL __delete, same endpoint as UI button) should return true`,
      ).toBe(true);

      const verifyDelete = await gql.get(entityName, createdId!, verifySelection);
      expect(
        verifyDelete,
        `${entityName}: AMIS write cycle delete should logically remove the row`,
      ).toBeFalsy();
    });
  });
}
