import type { Page, Locator } from '@playwright/test';
import type { EngineAdapter } from './types';
import { DEFAULT_LIST_TIMEOUT, DEFAULT_DIALOG_TIMEOUT } from './types';

export class AmisAdapter implements EngineAdapter {
  readonly engineName = 'amis';

  crudContainer(page: Page): Locator {
    return page.locator('#main-content, main, .cxd-Page, [role="main"]').first();
  }

  table(page: Page): Locator {
    return page.locator('.cxd-Crud, .cxd-Table, table').first();
  }

  rows(page: Page): Locator {
    return this.crudContainer(page).locator('tr, .cxd-Table-row, .cxd-List-item');
  }

  async cellValue(row: Locator, fieldName: string, columnHeaders: string[]): Promise<string> {
    const colIndex = columnHeaders.indexOf(fieldName);
    if (colIndex === -1) {
      const allCols = await row.locator('> td').count();
      for (let i = 0; i < allCols; i++) {
        const headerText = columnHeaders[i];
        if (headerText && fieldName.toLowerCase().includes(headerText.toLowerCase())) {
          return (await row.locator(`> td:nth-child(${i + 1})`).textContent()) || '';
        }
      }
      return '';
    }
    return (await row.locator(`> td:nth-child(${colIndex + 1})`).textContent()) || '';
  }

  addButton(page: Page): Locator {
    return this.crudContainer(page).locator('button:has(.fa-plus)').first();
  }

  async rowAction(row: Locator, actionNamePattern: RegExp): Promise<void> {
    for (const pat of [actionNamePattern]) {
      const direct = row.getByRole('button', { name: pat }).first();
      if (await direct.count().catch(() => 0)) {
        await direct.click({ force: true });
        return;
      }
    }

    const moreBtn = row.getByRole('button', { name: /^更多$|More/ }).first();
    if (await moreBtn.count().catch(() => 0)) {
      await moreBtn.click({ force: true });
      await row.page().waitForTimeout(400);

      for (const pat of [actionNamePattern]) {
        const item = row.page()
          .locator('.cxd-PopOver, .cxd-DropDown-popover, [role="menu"]')
          .last()
          .locator('a, button, [role="menuitem"], [role="button"]')
          .filter({ hasText: pat })
          .last();
        if (await item.count().catch(() => 0)) {
          await item.click({ force: true });
          return;
        }
      }
    }

    const fallback = row.page().getByText(actionNamePattern).last();
    if (await fallback.count().catch(() => 0)) {
      await fallback.click({ force: true });
      return;
    }

    throw new Error(`Row action ${actionNamePattern.source} not found`);
  }

  dialog(page: Page): Locator {
    return page.locator('.cxd-Modal, .cxd-Dialog, [role="dialog"]').last();
  }

  formField(dialog: Locator, fieldName: string): Locator {
    return dialog.locator(`input[name="${fieldName}"]`).first();
  }

  submitButton(dialog: Locator): Locator {
    return dialog
      .getByRole('button', { name: /^确定$|^确认$|^保存$|Confirm|Save/ })
      .first();
  }

  async selectOption(
    dialog: Locator,
    fieldLabels: string[],
    optionTexts: string[],
  ): Promise<void> {
    const page = dialog.page();

    const probe = await page.evaluate(({ labels }) => {
      const dialogs = Array.from(document.querySelectorAll('[role="dialog"], .cxd-Modal'));
      const dlg = dialogs[dialogs.length - 1] || null;
      if (!dlg) return { ok: false, reason: 'no-dialog' };
      const items = Array.from(dlg.querySelectorAll('.cxd-Form-item'));
      const target = items.find((it) => {
        const label = it.querySelector('.cxd-Form-label');
        const text = (label?.textContent || '').trim();
        return labels.some((l) => text.includes(l));
      });
      if (!target) return { ok: false, reason: 'no-form-item' };
      const select = target.querySelector('.cxd-Select') as HTMLElement | null;
      if (!select) return { ok: false, reason: 'no-select' };
      select.click();
      return { ok: true };
    }, { labels: fieldLabels });

    if (!probe.ok) {
      throw new Error(`selectOption(${fieldLabels.join('|')}) failed: ${JSON.stringify(probe)}`);
    }

    await page.waitForTimeout(300);

    const picked = await page.evaluate((opts) => {
      const menus = Array.from(document.querySelectorAll('.cxd-Select-menu'));
      const menu = menus[menus.length - 1] || null;
      if (!menu) return { ok: false, reason: 'no-menu' };
      const optEls = Array.from(menu.querySelectorAll('.cxd-Select-option')) as HTMLElement[];
      const texts = optEls.map((o) => (o.textContent || '').trim());
      const match = optEls.find((o) => {
        const t = (o.textContent || '').trim();
        return opts.some((o2) => t.includes(o2));
      });
      if (!match) return { ok: false, reason: 'no-option', wanted: opts, optionTexts: texts };
      match.click();
      return { ok: true };
    }, optionTexts);

    if (!picked.ok) {
      throw new Error(`selectOption option(${optionTexts.join('|')}) failed: ${JSON.stringify(picked)}`);
    }

    await page.waitForTimeout(300);
  }

  dateInputByLabel(page: Page, labelText: string): Locator {
    return page
      .locator('.cxd-Form-item')
      .filter({ hasText: labelText })
      .locator('input')
      .first();
  }
}
