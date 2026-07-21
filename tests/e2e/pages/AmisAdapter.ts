import type { Locator, Page } from '@playwright/test';
import type { EngineAdapter } from './types';

export class AmisAdapter implements EngineAdapter {
  engineName = 'amis';

  crudContainer(page: Page): Locator {
    return page.locator('#main-content, main, .cxd-Page').first();
  }

  table(page: Page): Locator {
    return page.locator('.cxd-Crud, .cxd-Table').first();
  }

  rows(page: Page): Locator {
    return page.locator('tr, .cxd-Table-row');
  }

  async cellValue(row: Locator, fieldName: string, _columnHeaders: string[]): Promise<string> {
    const index = _columnHeaders.indexOf(fieldName);
    if (index === -1) return '';
    const cell = row.locator(`> td:nth-child(${index + 1})`);
    return (await cell.textContent()) ?? '';
  }

  addButton(page: Page): Locator {
    return page.locator('button:has(.fa-plus)').first();
  }

  async rowAction(row: Locator, actionNamePattern: RegExp): Promise<void> {
    const button = row.locator('button').filter({ hasText: actionNamePattern }).first();
    await button.click();
  }

  dialog(page: Page): Locator {
    return page.locator('.cxd-Modal, .cxd-Dialog').first();
  }

  formField(dialog: Locator, fieldName: string): Locator {
    return dialog.locator(`input[name="${fieldName}"]`);
  }

  submitButton(dialog: Locator): Locator {
    return dialog.getByRole('button', {
      name: /确定|确认|保存|Confirm|Save/,
    }).first();
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
      return { ok: true, componentTypes: target.className };
    }, { labels: fieldLabels });

    if (!probe.ok) {
      throw new Error(`selectOption(${fieldLabels.join('|')}) failed: ${JSON.stringify(probe)}`);
    }

    const componentResult = await page.evaluate(({ labels, opts }) => {
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
      if (select) {
        select.click();
        return { ok: true, type: 'select' };
      }

      const switchEl = target.querySelector('.cxd-Switch') as HTMLElement | null;
      if (switchEl) {
        const isOn = switchEl.classList.contains('cxd-Switch--on');
        const wantOn = opts.some((o: string) => /ACTIVE|TRUE|ON|启用|ENABLED/i.test(o));
        if ((wantOn && !isOn) || (!wantOn && isOn)) {
          switchEl.click();
        }
        return { ok: true, type: 'switch' };
      }

      return { ok: false, reason: 'no-select-or-switch' };
    }, { labels: fieldLabels, opts: optionTexts });

    if (!componentResult.ok) {
      throw new Error(`selectOption(${fieldLabels.join('|')}) failed: ${JSON.stringify(componentResult)}`);
    }

    if (componentResult.type === 'select') {
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
  }

  dateInputByLabel(page: Page, labelText: string): Locator {
    return page.locator('.cxd-Form-item').filter({ hasText: labelText }).locator('input').first();
  }
}
