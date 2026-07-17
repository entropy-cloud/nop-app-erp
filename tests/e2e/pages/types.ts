import type { Page, Locator } from '@playwright/test';

export const DEFAULT_LIST_TIMEOUT = 15_000;
export const DEFAULT_DIALOG_TIMEOUT = 15_000;
export const DEFAULT_NAV_TIMEOUT = 30_000;

export interface EngineAdapter {
  engineName: string;

  crudContainer(page: Page): Locator;
  table(page: Page): Locator;
  rows(page: Page): Locator;
  cellValue(row: Locator, fieldName: string, columnHeaders: string[]): Promise<string>;
  addButton(page: Page): Locator;
  rowAction(row: Locator, actionNamePattern: RegExp): Promise<void>;

  dialog(page: Page): Locator;
  formField(dialog: Locator, fieldName: string): Locator;
  submitButton(dialog: Locator): Locator;
  selectOption(dialog: Locator, fieldLabels: string[], optionText: string[]): Promise<void>;

  /**
   * Locates a form-date input by its visible label text. AMIS date inputs have
   * no fillable `<input name>`, so they are targeted via their enclosing form-item
   * wrapper's label. Flux targets inputs by accessible label.
   */
  dateInputByLabel(page: Page, labelText: string): Locator;
}

export interface CrudPageConfig {
  entityRoute: string;
  entityName?: string;
  domain?: string;
  engine?: EngineAdapter;
  columnHeaders?: string[];
}
