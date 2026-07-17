import { expect } from '@playwright/test';
import type { Page as PlaywrightPage, Locator } from '@playwright/test';
import type { EngineAdapter } from './types';
import { DEFAULT_DIALOG_TIMEOUT } from './types';

export class FormDialog {
  constructor(
    private readonly page: PlaywrightPage,
    private readonly engine: EngineAdapter,
  ) {}

  get locator(): Locator {
    return this.engine.dialog(this.page);
  }

  async waitForVisible(): Promise<void> {
    await this.locator.waitFor({ state: 'visible', timeout: DEFAULT_DIALOG_TIMEOUT });
  }

  async waitForHidden(): Promise<void> {
    await this.locator.waitFor({ state: 'hidden', timeout: DEFAULT_DIALOG_TIMEOUT }).catch(() => {});
  }

  async setField(name: string, value: string): Promise<void> {
    const field = this.engine.formField(this.locator, name);
    await field.waitFor({ state: 'visible', timeout: DEFAULT_DIALOG_TIMEOUT });
    await field.fill(value);
  }

  async getField(name: string): Promise<string> {
    const field = this.engine.formField(this.locator, name);
    await field.waitFor({ state: 'visible', timeout: DEFAULT_DIALOG_TIMEOUT });
    return (await field.inputValue()) || '';
  }

  async selectOption(fieldLabels: string[], optionTexts: string[]): Promise<void> {
    await this.engine.selectOption(this.locator, fieldLabels, optionTexts);
  }

  async submit(): Promise<void> {
    const btn = this.engine.submitButton(this.locator);
    await btn.click();
  }
}
