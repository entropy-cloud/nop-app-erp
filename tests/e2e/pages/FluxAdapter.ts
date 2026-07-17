import type { Page, Locator } from '@playwright/test';
import type { EngineAdapter } from './types';

export class FluxAdapter implements EngineAdapter {
  readonly engineName = 'flux';

  crudContainer(page: Page): Locator {
    return page.locator('[data-slot="crud-table"]').first();
  }

  table(page: Page): Locator {
    return page.locator('[data-slot="crud-table"] .nop-table').first();
  }

  rows(page: Page): Locator {
    return page.locator('tbody tr[data-slot="table-row"]');
  }

  async cellValue(row: Locator, fieldName: string, _columnHeaders: string[]): Promise<string> {
    const cell = row.locator(`[data-field="${fieldName}"]`);
    if (await cell.count().catch(() => 0)) {
      return (await cell.textContent()) || '';
    }
    return (await row.locator(`> td`).nth(0).textContent()) || '';
  }

  addButton(page: Page): Locator {
    return page.getByTestId('btn-add').first();
  }

  async rowAction(row: Locator, actionNamePattern: RegExp): Promise<void> {
    const button = row.getByRole('button', { name: actionNamePattern }).first();
    if (await button.count().catch(() => 0)) {
      await button.click({ force: true });
      return;
    }
    const testId = `btn-${actionNamePattern.source.toLowerCase()}`;
    const btnById = row.page().getByTestId(testId).first();
    if (await btnById.count().catch(() => 0)) {
      await btnById.click({ force: true });
      return;
    }
    throw new Error(`Flux row action ${actionNamePattern.source} not found`);
  }

  dialog(page: Page): Locator {
    return page.locator('[data-slot="dialog-surface"]').last();
  }

  formField(dialog: Locator, fieldName: string): Locator {
    return dialog.getByLabel(fieldName).first();
  }

  submitButton(dialog: Locator): Locator {
    return dialog.getByRole('button', { name: /^确定$|^确认$|^保存$|Confirm|Save/ }).first();
  }

  async selectOption(dialog: Locator, fieldLabels: string[], optionTexts: string[]): Promise<void> {
    for (const label of fieldLabels) {
      const select = dialog.getByLabel(label).first();
      if (await select.count().catch(() => 0)) {
        await select.click();
        await dialog.page().waitForTimeout(200);
        for (const opt of optionTexts) {
          const option = dialog.page().getByRole('option', { name: opt }).first();
          if (await option.count().catch(() => 0)) {
            await option.click();
            return;
          }
        }
        return;
      }
    }
    throw new Error(`Flux selectOption(${fieldLabels.join('|')}) failed`);
  }

  dateInputByLabel(page: Page, labelText: string): Locator {
    return page.getByLabel(labelText).first();
  }
}
