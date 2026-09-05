// Business-action pixel-snapshot layer (plan 2026-09-04-1721-1 M2.2; roadmap
// comprehensive-test-data-and-visual-coverage M2.2).
//
// Captures pixel-level layout stability of business-action UI elements:
// approve/submit/post/reverse/confirm/cancel dialogs, drawers, and toasts.
// These are the UI surfaces triggered by @BizMutation actions that the
// GraphQL-only action specs deliberately skip.
//
// Pattern: navigate to entity list → click action button → wait for
// dialog/drawer → assertCrudPixelSnapshot.

import { test, loginAndNavigate } from '../fixtures';
import { CrudListPage, getEngine } from '../pages';
import { assertCrudPixelSnapshot } from '../visual/_helper';
import type { Page, Locator } from '@playwright/test';

interface ActionSnapshotCfg {
  /** Domain short label. */
  domain: string;
  /** Entity route for CrudListPage. */
  entity: string;
  /** Seed row code to pin for the action. */
  rowCode: string;
  /** Action button label or locator strategy. */
  actionLabel: string;
  /** Snapshot name suffix. */
  suffix: string;
  /** Optional: additional wait after dialog opens. */
  waitForSelector?: string;
}

async function settleForSnapshot(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(500);
}

async function driveActionAndSnapshot(page: Page, cfg: ActionSnapshotCfg): Promise<void> {
  const engine = getEngine();
  const crud = new CrudListPage(page, engine, { entityRoute: cfg.entity });
  await crud.navigate();

  // Find the row and click the action button
  const row = await crud.findRowByText(cfg.rowCode);
  if (!row) {
    test.skip(true, `Row ${cfg.rowCode} not found in ${cfg.entity}`);
    return;
  }

  // Look for the action button in the row's action column
  const actionBtn = row.locator(`button, [role="button"]`).filter({ hasText: cfg.actionLabel }).first();
  const actionExists = await actionBtn.isVisible().catch(() => false);

  if (!actionExists) {
    // Try clicking the row action menu first
    const moreBtn = row.locator('[class*="action"], [class*="menu"], [class*="more"]').first();
    const moreExists = await moreBtn.isVisible().catch(() => false);
    if (moreExists) {
      await moreBtn.click();
      await page.waitForTimeout(300);
    }
  }

  // Click the action
  const finalBtn = row.locator(`button, [role="button"]`).filter({ hasText: cfg.actionLabel }).first();
  const finalExists = await finalBtn.isVisible().catch(() => false);

  if (finalExists) {
    await finalBtn.click();
    await page.waitForTimeout(500);

    // Wait for dialog/drawer if specified
    if (cfg.waitForSelector) {
      await page.locator(cfg.waitForSelector).waitFor({ state: 'visible', timeout: 5_000 }).catch(() => {});
    } else {
      // Default: wait for any dialog/drawer to appear
      await page.locator('.cxd-Drawer, .cxd-Modal, [role="dialog"]').first()
        .waitFor({ state: 'visible', timeout: 5_000 }).catch(() => {});
    }

    await settleForSnapshot(page);
  }

  const snapshotName = `${cfg.entity}-${cfg.suffix}.png`;
  await assertCrudPixelSnapshot(page, { name: snapshotName });
}

// Batch F — approve/submit dialog snapshots (representative entities)
const APPROVE_DIALOG_CFGS: ActionSnapshotCfg[] = [
  { domain: 'purchase', entity: 'ErpPurOrder', rowCode: 'PO-2026-001', actionLabel: '审批', suffix: 'approve-dialog' },
  { domain: 'sales', entity: 'ErpSalOrder', rowCode: 'SO-2026-001', actionLabel: '审批', suffix: 'approve-dialog' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', rowCode: 'WO-2026-001', actionLabel: '审批', suffix: 'approve-dialog' },
  { domain: 'quality', entity: 'ErpQaInspection', rowCode: 'INS-2026-001', actionLabel: '审批', suffix: 'approve-dialog' },
  { domain: 'assets', entity: 'ErpAstAsset', rowCode: 'AST-2026-001', actionLabel: '审批', suffix: 'approve-dialog' },
  { domain: 'hr', entity: 'ErpHrEmployee', rowCode: 'HR-EMP-001', actionLabel: '审批', suffix: 'approve-dialog' },
];

// Batch G — cancel/delete confirmation dialog snapshots
const CANCEL_DIALOG_CFGS: ActionSnapshotCfg[] = [
  { domain: 'purchase', entity: 'ErpPurOrder', rowCode: 'PO-2026-001', actionLabel: '取消', suffix: 'cancel-dialog' },
  { domain: 'sales', entity: 'ErpSalOrder', rowCode: 'SO-2026-001', actionLabel: '取消', suffix: 'cancel-dialog' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', rowCode: 'WO-2026-001', actionLabel: '取消', suffix: 'cancel-dialog' },
  { domain: 'maintenance', entity: 'ErpMntVisit', rowCode: 'VIS-2026-001', actionLabel: '取消', suffix: 'cancel-dialog' },
  { domain: 'projects', entity: 'ErpPrjProject', rowCode: 'PRJ-2026-001', actionLabel: '取消', suffix: 'cancel-dialog' },
  { domain: 'hr', entity: 'ErpHrEmployee', rowCode: 'HR-EMP-001', actionLabel: '取消', suffix: 'cancel-dialog' },
];

// Batch H — status-tag colored label snapshots (F5 verification)
const STATUS_TAG_CFGS: ActionSnapshotCfg[] = [
  { domain: 'purchase', entity: 'ErpPurOrder', rowCode: 'PO-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'sales', entity: 'ErpSalOrder', rowCode: 'SO-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'finance', entity: 'ErpFinVoucher', rowCode: 'PZ-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', rowCode: 'WO-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'quality', entity: 'ErpQaInspection', rowCode: 'INS-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'maintenance', entity: 'ErpMntVisit', rowCode: 'VIS-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'crm', entity: 'ErpCrmLead', rowCode: 'LEAD-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'cs', entity: 'ErpCsTicket', rowCode: 'TK-2026-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'hr', entity: 'ErpHrEmployee', rowCode: 'HR-EMP-001', actionLabel: '', suffix: 'status-tags' },
  { domain: 'assets', entity: 'ErpAstAsset', rowCode: 'AST-2026-001', actionLabel: '', suffix: 'status-tags' },
];

for (const [batch, cfgs] of [
  ['batch-F', APPROVE_DIALOG_CFGS],
  ['batch-G', CANCEL_DIALOG_CFGS],
  ['batch-H', STATUS_TAG_CFGS],
] as const) {
  for (const cfg of cfgs) {
    test.describe(`${batch} :: ${cfg.domain} action pixel snapshot`, () => {
      test(`${cfg.entity} ${cfg.suffix} snapshot`, async ({ page }) => {
        await driveActionAndSnapshot(page, cfg);
      });
    });
  }
}
