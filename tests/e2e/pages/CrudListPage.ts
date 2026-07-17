import type { Locator } from '@playwright/test';
import { expect } from '@playwright/test';
import type { Page as PlaywrightPage } from '@playwright/test';
import type { EngineAdapter, CrudPageConfig } from './types';
import { DEFAULT_LIST_TIMEOUT } from './types';
import { AmisAdapter } from './AmisAdapter';
import { BasePage } from './Page';
import { FormDialog } from './FormDialog';
import { GraphQLClient } from './GraphQLClient';

export interface FindRowOptions {
  fieldName: string;
  value: string;
}

export class CrudListPage extends BasePage {
  private gql: GraphQLClient;
  private columnHeaders: string[];

  constructor(
    page: PlaywrightPage,
    private config: CrudPageConfig,
    engine?: EngineAdapter,
  ) {
    super(page, engine || new AmisAdapter());
    this.gql = new GraphQLClient(page);
    this.columnHeaders = config.columnHeaders || [];
  }

  get graphQL(): GraphQLClient {
    return this.gql;
  }

  async navigate(): Promise<void> {
    await this.goto(`/${this.config.entityRoute}-main`);
  }

  async waitForList(): Promise<void> {
    const table = this.engine.table(this.page);
    await table.waitFor({ state: 'visible', timeout: DEFAULT_LIST_TIMEOUT });
  }

  async getAddButton(): Promise<Locator> {
    await this.waitForList();
    return this.engine.addButton(this.page);
  }

  async clickAdd(): Promise<FormDialog> {
    const btn = await this.getAddButton();
    await btn.click({ force: true });
    return new FormDialog(this.page, this.engine);
  }

  async getCellText(rowIndex: number, fieldName: string): Promise<string> {
    const rows = this.engine.rows(this.page);
    const row = rows.nth(rowIndex);
    return this.engine.cellValue(row, fieldName, this.columnHeaders);
  }

  async findRowByField(fieldName: string, value: string): Promise<Locator | null> {
    const rows = this.engine.rows(this.page);
    const count = await rows.count();
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i);
      const cellText = await this.engine.cellValue(row, fieldName, this.columnHeaders);
      if (cellText.trim() === value) {
        return row;
      }
    }
    return null;
  }

  async findRowByText(text: string): Promise<Locator | null> {
    const rows = this.engine.rows(this.page);
    const matched = rows.filter({ hasText: text }).first();
    if (await matched.count().catch(() => 0)) {
      return matched;
    }
    return null;
  }

  async editRow(row: Locator): Promise<FormDialog> {
    await this.engine.rowAction(row, /^编辑$|^修改$|Edit/);
    return new FormDialog(this.page, this.engine);
  }

  async deleteRow(row: Locator): Promise<void> {
    await this.engine.rowAction(row, /^删除$|^Delete$/);
  }

  /** Actually deletes via GraphQL directly (more reliable than AMIS confirm dialog). */
  async deleteEntityViaApi(entityName: string, id: string | number): Promise<void> {
    await this.gql.delete(entityName, id);
  }

  async assertGraphQLOk(): Promise<void> {
    const responses: number[] = [];
    this.page.on('response', (resp) => {
      if (resp.url().includes('/graphql')) {
        responses.push(resp.status());
      }
    });
    await this.waitForList();
    expect(responses.length).toBeGreaterThan(0);
    for (const status of responses) {
      expect(status).toBe(200);
    }
  }
}
