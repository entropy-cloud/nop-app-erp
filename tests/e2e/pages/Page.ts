import type { Page as PlaywrightPage } from '@playwright/test';
import type { EngineAdapter } from './types';
import { loginAndNavigate } from './Navigation';

export abstract class BasePage {
  constructor(
    protected readonly page: PlaywrightPage,
    protected readonly engine: EngineAdapter,
  ) {}

  async goto(hashRoute: string): Promise<void> {
    await loginAndNavigate(this.page, hashRoute);
  }
}
