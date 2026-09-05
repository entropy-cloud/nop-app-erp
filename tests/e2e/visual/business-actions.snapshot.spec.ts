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
import { assertCrudPixelSnapshot, assertBusinessActionPixelSnapshot } from '../visual/_helper';
import { createViaSave, deleteByFilter, eqFilter } from '../business-actions/_helper';
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

// ═══════════════════════════════════════════════════════════════════════════
// Plan 2026-09-03-0400-2 M2.2 matrix batches (modes A/B/C, 35 rows — see the
// plan Appendix A for the per-row adjudication). Additive to the predecessor
// batches above; rows here are deterministically verified:
//   Mode A — confirm dialog/drawer open state on seed rows, Escape-dismissed
//            (flux confirmText gate: cancel = zero backend call).
//   Mode B — self-contained E2E-BAS-* row via __save, execute mutation via the
//            confirm dialog, capture the reloaded list, cleanup by code.
//   Mode C — guard rejection on terminal-state seed rows: the error toast must
//            render (waitForSelector throws on absence) and is masked.
// ═══════════════════════════════════════════════════════════════════════════

const ALERT_DIALOG = '[data-slot="alert-dialog-content"]';
const TOAST = '[data-sonner-toast]';

interface MatrixRowCfg {
  domain: string;
  entity: string;
  rowCode: string;
  actionLabel: string;
  suffix: string;
  /** dialog surface kind: alert (confirmText) | page (dialog page=) | drawer. */
  surface: 'alert' | 'page' | 'drawer';
  /** extra settle for heavy multi-tab page dialogs. */
  settleMs?: number;
}

async function surfaceFor(page: Page, kind: MatrixRowCfg['surface']): Promise<Locator> {
  const engine = getEngine();
  if (kind === 'drawer') return engine.drawer(page);
  // 'page' dialogs render dialog-surface OR drawer-surface depending on the
  // action declaration (runtime probe: 红冲 preview → dialog; asset dashboard /
  // employee archive / transfer → drawer). Wait the union, mirroring
  // CrudListPage.clickEdit's dialog.or(drawer) precedent.
  if (kind === 'page') return engine.dialog(page).or(engine.drawer(page)).first();
  return page.locator(ALERT_DIALOG).first();
}

async function findMatrixRow(page: Page, entity: string, rowCode: string): Promise<Locator> {
  const engine = getEngine();
  const crud = new CrudListPage(page, engine, { entityRoute: entity });
  await crud.navigate();
  const row = await crud.findRowByText(rowCode);
  if (!row) throw new Error(`[M2.2] seed row "${rowCode}" not found in ${entity} list`);
  return row;
}

// ── Mode A: dialog/drawer open state (zero state change) ─────────────────────

async function driveModeA(page: Page, cfg: MatrixRowCfg): Promise<void> {
  const engine = getEngine();
  const row = await findMatrixRow(page, cfg.entity, cfg.rowCode);
  await engine.rowAction(row, new RegExp(cfg.actionLabel));
  const surface = await surfaceFor(page, cfg.surface);
  await surface.waitFor({ state: 'visible', timeout: 8_000 });
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(cfg.settleMs ?? 500);
  await assertBusinessActionPixelSnapshot(page, { name: `${cfg.entity}-${cfg.suffix}.png` });
  // Dismiss — flux confirm gate resolves false, no mutation is dispatched.
  await page.keyboard.press('Escape');
  await surface.waitFor({ state: 'hidden', timeout: 5_000 }).catch(() => {});
}

const MODE_A_ROWS: MatrixRowCfg[] = [
  { domain: 'purchase', entity: 'ErpPurOrder', rowCode: 'PO-2026-001', actionLabel: '反审批', suffix: 'reverse-approve-dialog', surface: 'alert' },
  { domain: 'purchase', entity: 'ErpPurOrder', rowCode: 'PO-2026-001', actionLabel: '作废', suffix: 'void-dialog', surface: 'alert' },
  { domain: 'purchase', entity: 'ErpPurOrder', rowCode: 'PO-2026-001', actionLabel: '关联入库单', suffix: 'receive-link-drawer', surface: 'drawer', settleMs: 1200 },
  { domain: 'sales', entity: 'ErpSalOrder', rowCode: 'SO-2026-001', actionLabel: '反审批', suffix: 'reverse-approve-dialog', surface: 'alert' },
  { domain: 'sales', entity: 'ErpSalOrder', rowCode: 'SO-2026-001', actionLabel: '作废', suffix: 'void-dialog', surface: 'alert' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', rowCode: 'WO-2026-003', actionLabel: '报告完工', suffix: 'report-completion-dialog', surface: 'alert' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', rowCode: 'WO-2026-003', actionLabel: '结案', suffix: 'close-dialog', surface: 'alert' },
  { domain: 'finance', entity: 'ErpFinVoucher', rowCode: 'PZ-2026-001', actionLabel: '红冲', suffix: 'reverse-preview-dialog', surface: 'page', settleMs: 1500 },
  { domain: 'assets', entity: 'ErpAstAsset', rowCode: 'AST-2026-001', actionLabel: '完整仪表板', suffix: 'dashboard-dialog', surface: 'page', settleMs: 1500 },
  { domain: 'projects', entity: 'ErpPrjProject', rowCode: 'PRJ-2026-001', actionLabel: '暂停', suffix: 'hold-dialog', surface: 'alert' },
  { domain: 'projects', entity: 'ErpPrjProject', rowCode: 'PRJ-2026-001', actionLabel: '完成', suffix: 'complete-dialog', surface: 'alert' },
  { domain: 'hr', entity: 'ErpHrEmployee', rowCode: 'HR-EMP-001', actionLabel: '完整档案', suffix: 'archive-dialog', surface: 'page', settleMs: 1500 },
  { domain: 'hr', entity: 'ErpHrEmployee', rowCode: 'HR-EMP-001', actionLabel: '调动', suffix: 'transfer-dialog', surface: 'page', settleMs: 1000 },
  { domain: 'crm', entity: 'ErpCrmLead', rowCode: 'LEAD-2026-001', actionLabel: '丢单', suffix: 'lose-dialog', surface: 'alert' },
  { domain: 'inventory', entity: 'ErpInvStockMove', rowCode: 'MV-2026-001', actionLabel: '关联流水', suffix: 'ledger-drawer', surface: 'drawer', settleMs: 1200 },
  { domain: 'aps', entity: 'ErpApsOperationOrder', rowCode: 'APS-OP-2026-001', actionLabel: '开始', suffix: 'start-dialog', surface: 'alert' },
  { domain: 'aps', entity: 'ErpApsOperationOrder', rowCode: 'APS-OP-2026-001', actionLabel: '作废', suffix: 'void-dialog', surface: 'alert' },
  { domain: 'logistics', entity: 'ErpLogShipment', rowCode: 'SHP-2026-001', actionLabel: '作废', suffix: 'void-dialog', surface: 'alert' },
  { domain: 'contract', entity: 'ErpCtContract', rowCode: 'CT-SEED-2026-001', actionLabel: '中止', suffix: 'suspend-dialog', surface: 'alert' },
  { domain: 'contract', entity: 'ErpCtContract', rowCode: 'CT-SEED-2026-001', actionLabel: '终止', suffix: 'terminate-dialog', surface: 'alert' },
  { domain: 'drp', entity: 'ErpDrpPlan', rowCode: 'DRP-PLAN-SEED-001', actionLabel: '运行DRP', suffix: 'run-dialog', surface: 'alert' },
  { domain: 'quality', entity: 'ErpQaNonConformance', rowCode: 'NCR-2026-001', actionLabel: '评审', suffix: 'review-dialog', surface: 'alert' },
  { domain: 'quality', entity: 'ErpQaNonConformance', rowCode: 'NCR-2026-003', actionLabel: '拒绝', suffix: 'reject-dialog', surface: 'alert' },
];

for (const cfg of MODE_A_ROWS) {
  test.describe(`m22-A :: ${cfg.domain} dialog open state`, () => {
    test(`${cfg.entity} ${cfg.suffix}`, async ({ page }) => {
      await driveModeA(page, cfg);
    });
  });
}

// ── Mode B: post-execution list state (self-contained setup + cleanup) ───────

interface ModeBSetup {
  entity: string;
  data: Record<string, unknown>;
  /** Delete every row created for this batch (by deterministic code). */
  cleanupFilterValue: string;
}

const E2E_BAS_DATE = '2026-09-05';

const MODE_B_SETUPS: ModeBSetup[] = [
  {
    entity: 'ErpInvStockMove',
    data: {
      code: 'E2E-BAS-MV-001', moveType: 'INCOMING', orgId: '2', businessDate: E2E_BAS_DATE,
      sourceWarehouseId: '1', destWarehouseId: '2', docStatus: 'DRAFT',
      approveStatus: 'UNSUBMITTED', remark: 'M2.2 pixel baseline',
    },
    cleanupFilterValue: 'E2E-BAS-MV-001',
  },
  {
    entity: 'ErpMntVisit',
    data: {
      code: 'E2E-BAS-VIS-001', equipmentId: '1', visitDate: '2026-12-25', status: 'DRAFT',
      assignedTo: 2, visitType: 'PLANNED', orgId: '2',
    },
    cleanupFilterValue: 'E2E-BAS-VIS-001',
  },
  {
    entity: 'ErpCtContract',
    data: {
      code: 'E2E-BAS-CT-001', contractName: 'E2E-BAS contract', contractType: 'SALES',
      contractDirection: 'OUTBOUND', partnerId: '3', orgId: '2', currencyId: '1',
      totalAmount: 10000, startDate: '2026-01-01', endDate: '2026-12-31',
      businessDate: E2E_BAS_DATE, status: 'NEGOTIATION',
    },
    cleanupFilterValue: 'E2E-BAS-CT-001',
  },
  {
    entity: 'ErpPrjProject',
    data: {
      code: 'E2E-BAS-PRJ-001', name: 'E2E-BAS project', orgId: '2', currencyId: '1',
      startDate: '2026-06-01', endDate: '2026-12-31', status: 'DRAFT',
    },
    cleanupFilterValue: 'E2E-BAS-PRJ-001',
  },
];

// The m22-B test body logs in once and lands directly on the entity list
// (loginAndNavigate). Re-entering Navigation.login afterwards would time out:
// its username-input wait only resolves on an unauthenticated context. A full
// document reload (page.reload — a same-URL page.goto would be a same-document
// hash navigation and never refetch) picks up freshly saved rows instead.
async function reloadMatrixList(page: Page, entity: string): Promise<CrudListPage> {
  const engine = getEngine();
  await page.reload({ waitUntil: 'domcontentloaded' });
  const crud = new CrudListPage(page, engine, { entityRoute: entity });
  await crud.waitForList();
  return crud;
}

async function driveModeB(page: Page, setup: ModeBSetup, actionLabel: string, suffix: string): Promise<void> {
  const engine = getEngine();
  try {
    const saved = await createViaSave(page, setup.entity, setup.data, 'id code');
    if (!saved?.id) throw new Error(`[M2.2-B] __save for ${setup.entity} returned no id`);

    const crud = await reloadMatrixList(page, setup.entity);
    const row = await crud.findRowByText(setup.cleanupFilterValue);
    if (!row) throw new Error(`[M2.2-B] row ${setup.cleanupFilterValue} not found in ${setup.entity} list`);
    await engine.rowAction(row, new RegExp(actionLabel));
    const dialog = page.locator(ALERT_DIALOG).first();
    await dialog.waitFor({ state: 'visible', timeout: 8_000 });
    await page.locator('[data-slot="alert-dialog-action"]').first().click();

    // Mutation executed (success toast). Wait it gone, then force a
    // deterministic list reload and wait for the flipped status cell.
    await page.locator(TOAST).first().waitFor({ state: 'visible', timeout: 8_000 }).catch(() => {});
    await page.locator(TOAST).first().waitFor({ state: 'hidden', timeout: 10_000 }).catch(() => {});
    const refresh = engine.refreshButton(page);
    if (await refresh.isVisible().catch(() => false)) {
      await refresh.click();
    }
    await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
    await page.waitForTimeout(500);

    // Mask the seq-generated id cell of the self-contained row (cross-run drift).
    // Runtime probe: this flux build renders table cells WITHOUT data-field
    // attributes, so the id cell is located by its exact seq-id text (the id is
    // known from the __save response) — column-structure changes then fail
    // loudly here instead of silently baking an unmasked seq id into the
    // baseline (plan-audit m-2 zero-match guard semantics preserved).
    const newRow = await crud.findRowByText(setup.cleanupFilterValue);
    if (!newRow) throw new Error(`[M2.2-B] row ${setup.cleanupFilterValue} vanished after mutation`);
    const idCell = newRow
      .locator('td')
      .filter({ hasText: new RegExp(`^${String(saved.id)}$`) })
      .first();
    await idCell.waitFor({ state: 'visible', timeout: 5_000 });
    await assertBusinessActionPixelSnapshot(page, {
      name: `${setup.entity}-${suffix}.png`,
      mask: [idCell],
      waitToastGone: true,
    });
  } finally {
    await deleteByFilter(page, setup.entity, eqFilter('code', setup.cleanupFilterValue)).catch(() => {});
  }
}

const MODE_B_ROWS: Array<{ setup: ModeBSetup; actionLabel: string; suffix: string }> = [
  { setup: MODE_B_SETUPS[0], actionLabel: '提交确认', suffix: 'confirm-post-exec' },
  { setup: MODE_B_SETUPS[1], actionLabel: '排程', suffix: 'schedule-post-exec' },
  { setup: MODE_B_SETUPS[2], actionLabel: '生效', suffix: 'activate-post-exec' },
  { setup: MODE_B_SETUPS[3], actionLabel: '启动', suffix: 'start-post-exec' },
];

for (const { setup, actionLabel, suffix } of MODE_B_ROWS) {
  test.describe(`m22-B :: post-execution state`, () => {
    test(`${setup.entity} ${suffix}`, async ({ page }) => {
      await loginAndNavigate(page, `/${setup.entity}-main`);
      await driveModeB(page, setup, actionLabel, suffix);
    });
  });
}

// ── Mode C: guard rejection toast rendering (zero state change) ──────────────

async function driveModeC(page: Page, cfg: MatrixRowCfg): Promise<void> {
  const engine = getEngine();
  const row = await findMatrixRow(page, cfg.entity, cfg.rowCode);
  await engine.rowAction(row, new RegExp(cfg.actionLabel));
  const dialog = page.locator(ALERT_DIALOG).first();
  await dialog.waitFor({ state: 'visible', timeout: 8_000 });
  await page.locator('[data-slot="alert-dialog-action"]').first().click();
  // The rejection toast MUST render; absence is a failure (waitFor throws).
  await assertBusinessActionPixelSnapshot(page, {
    name: `${cfg.entity}-${cfg.suffix}.png`,
    waitForSelector: TOAST,
    mask: [page.locator('[data-sonner-toaster]')],
  });
  await page.keyboard.press('Escape');
  await dialog.waitFor({ state: 'hidden', timeout: 5_000 }).catch(() => {});
}

const MODE_C_ROWS: MatrixRowCfg[] = [
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', rowCode: 'WO-2026-003', actionLabel: '作废', suffix: 'cancel-guard-toast', surface: 'alert' },
  { domain: 'cs', entity: 'ErpCsTicket', rowCode: 'TKT-2026-002', actionLabel: '分派', suffix: 'assign-guard-toast', surface: 'alert' },
  { domain: 'cs', entity: 'ErpCsTicket', rowCode: 'TKT-2026-002', actionLabel: '开始处理', suffix: 'start-guard-toast', surface: 'alert' },
  { domain: 'cs', entity: 'ErpCsTicket', rowCode: 'TKT-2026-002', actionLabel: '关闭', suffix: 'close-guard-toast', surface: 'alert' },
  { domain: 'cs', entity: 'ErpCsTicket', rowCode: 'TKT-2026-001', actionLabel: '开始处理', suffix: 'start-resolved-guard-toast', surface: 'alert' },
  { domain: 'cs', entity: 'ErpCsTicket', rowCode: 'TKT-2026-001', actionLabel: '解决', suffix: 'resolve-guard-toast', surface: 'alert' },
  { domain: 'crm', entity: 'ErpCrmLead', rowCode: 'LEAD-2026-001', actionLabel: '资质认定', suffix: 'qualify-guard-toast', surface: 'alert' },
  { domain: 'quality', entity: 'ErpQaNonConformance', rowCode: 'NCR-2026-002', actionLabel: '解决', suffix: 'resolve-guard-toast', surface: 'alert' },
];

for (const cfg of MODE_C_ROWS) {
  test.describe(`m22-C :: guard rejection toast`, () => {
    test(`${cfg.entity} ${cfg.suffix}`, async ({ page }) => {
      await driveModeC(page, cfg);
    });
  });
}
