// CRUD pixel-snapshot layer (plan 2026-09-03-0400-1 M2.1; roadmap
// comprehensive-test-data-and-visual-coverage M2.1).
//
// Mirrors dashboards.snapshot.spec.ts structure: a cfg-driven single spec
// asserting pixel-level layout stability of representative CRUD page states
// via assertCrudPixelSnapshot (which reuses the assertSnapshot paradigm —
// font hardening + canonical mask header/canvas + 1% ratio tolerance — with
// the echarts settle wait skipped by default, since CRUD surfaces are
// table/form pages without charts).
//
// This layer catches regressions the DOM-content layers cannot: CSS
// misalignment, table-header collapse, missing required-field markers,
// element overlap, dialog/drawer layout breakage.
//
// Page-driving goes through the engine-agnostic PageObject (CrudListPage /
// FormDialog) so specs stay business-semantic; no framework selectors appear
// here (E2E 编写规范 §PageObject 模式).
//
// Determinism notes:
// - All 18 crud-domain main entities reuse the crud/ smoke-suite selection
//   (proven add-button + add-form 'code' field visibility).
// - Add-forms open empty (no ${NOW()} defaults exist in ERP web resources —
//   verified by grep at plan time); seed rows carry static dates.
// - Drawer states pin a stable seed row by code (order-independent).
// - Baseline update: when a view.xml/page.yaml changes intentionally,
//   re-record with `--update-snapshots` (M0.3 §4 dual-side protocol).

import { test, loginAndNavigate } from '../fixtures';
import { CrudListPage, getEngine } from '../pages';
import { assertCrudPixelSnapshot } from './_helper';
import type { Page } from '@playwright/test';

type CrudSnapshotState =
  | 'list' // grid + data rows + toolbar + pagination
  | 'add-form' // add dialog open (empty form, required-field markers)
  | 'edit-drawer' // edit dialog/drawer open on a pinned seed row
  | 'view-drawer' // read-only view dialog open on a pinned seed row
  | 'page'; // non-standard full page (kanban/timeline/org-chart), plain navigation

interface CrudSnapshotCfg {
  /** Domain short label used for describe/batch grouping. */
  domain: string;
  /** Entity name for CrudListPage routing (/{entity}-main); unused for 'page'. */
  entity?: string;
  state: CrudSnapshotState;
  /** Explicit hash route (required for 'page', optional override otherwise). */
  route?: string;
  /** Stable seed code pinning the row for drawer states. */
  rowCode?: string;
}

async function settleForSnapshot(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(500);
}

async function driveAndSnapshot(page: Page, cfg: CrudSnapshotCfg): Promise<void> {
  const engine = getEngine();
  const route = cfg.route ?? `/${cfg.entity}-main`;
  const snapshotName = `${cfg.entity ?? route.slice(1)}-${cfg.state}.png`;

  if (cfg.state === 'page') {
    await loginAndNavigate(page, route);
    await settleForSnapshot(page);
  } else {
    const crud = new CrudListPage(page, engine, { entityRoute: cfg.entity! });
    await crud.navigate();

    if (cfg.state === 'add-form') {
      const dialog = await crud.clickAdd();
      await dialog.waitForVisible();
      await settleForSnapshot(page);
    } else if (cfg.state === 'edit-drawer') {
      await crud.clickEdit(cfg.rowCode!);
      await settleForSnapshot(page);
    } else if (cfg.state === 'view-drawer') {
      await crud.clickView(cfg.rowCode!);
      await settleForSnapshot(page);
    }
    // 'list': waitForList already ran inside crud.navigate().
  }

  await assertCrudPixelSnapshot(page, { name: snapshotName });
}

// Batch A — 18 crud-domain main-entity list states. Selection mirrors the
// crud/ smoke suite one-to-one (runbook §CRUD 套件 同源选型).
const LIST_CFGS: CrudSnapshotCfg[] = [
  { domain: 'master-data', entity: 'ErpMdPartner', state: 'list' },
  { domain: 'inventory', entity: 'ErpInvStockMove', state: 'list' },
  { domain: 'purchase', entity: 'ErpPurOrder', state: 'list' },
  { domain: 'sales', entity: 'ErpSalOrder', state: 'list' },
  { domain: 'finance', entity: 'ErpFinVoucher', state: 'list' },
  { domain: 'assets', entity: 'ErpAstAsset', state: 'list' },
  { domain: 'projects', entity: 'ErpPrjProject', state: 'list' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', state: 'list' },
  { domain: 'quality', entity: 'ErpQaInspection', state: 'list' },
  { domain: 'maintenance', entity: 'ErpMntVisit', state: 'list' },
  { domain: 'crm', entity: 'ErpCrmLead', state: 'list' },
  { domain: 'cs', entity: 'ErpCsTicket', state: 'list' },
  { domain: 'hr', entity: 'ErpHrEmployee', state: 'list' },
  { domain: 'aps', entity: 'ErpApsOperationOrder', state: 'list' },
  { domain: 'logistics', entity: 'ErpLogShipment', state: 'list' },
  { domain: 'b2b', entity: 'ErpB2bAsn', state: 'list' },
  { domain: 'contract', entity: 'ErpCtContract', state: 'list' },
  { domain: 'drp', entity: 'ErpDrpPlan', state: 'list' },
];

// Batch B — 18 add-form open states (matrix R19~R36: empty form + required
// markers; audit-stamp fields only populate on insert, no run-varying prefill).
const ADD_FORM_CFGS: CrudSnapshotCfg[] = [
  { domain: 'master-data', entity: 'ErpMdPartner', state: 'add-form' },
  { domain: 'inventory', entity: 'ErpInvStockMove', state: 'add-form' },
  { domain: 'purchase', entity: 'ErpPurOrder', state: 'add-form' },
  { domain: 'sales', entity: 'ErpSalOrder', state: 'add-form' },
  { domain: 'finance', entity: 'ErpFinVoucher', state: 'add-form' },
  { domain: 'assets', entity: 'ErpAstAsset', state: 'add-form' },
  { domain: 'projects', entity: 'ErpPrjProject', state: 'add-form' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', state: 'add-form' },
  { domain: 'quality', entity: 'ErpQaInspection', state: 'add-form' },
  { domain: 'maintenance', entity: 'ErpMntVisit', state: 'add-form' },
  { domain: 'crm', entity: 'ErpCrmLead', state: 'add-form' },
  { domain: 'cs', entity: 'ErpCsTicket', state: 'add-form' },
  { domain: 'hr', entity: 'ErpHrEmployee', state: 'add-form' },
  { domain: 'aps', entity: 'ErpApsOperationOrder', state: 'add-form' },
  { domain: 'logistics', entity: 'ErpLogShipment', state: 'add-form' },
  { domain: 'b2b', entity: 'ErpB2bAsn', state: 'add-form' },
  { domain: 'contract', entity: 'ErpCtContract', state: 'add-form' },
  { domain: 'drp', entity: 'ErpDrpPlan', state: 'add-form' },
];

// Batch C — non-standard views (matrix R37~R42) + pinned-seed-row drawers
// (R43~R44). Drawer rows pin stable seed codes so row order is irrelevant.
const NONSTANDARD_CFGS: CrudSnapshotCfg[] = [
  { domain: 'master-data', entity: 'ErpMdMaterialCategory', state: 'list' },
  { domain: 'inventory', entity: 'ErpInvStockLedger', state: 'list' },
  { domain: 'finance', entity: 'ErpFinTrialBalance', state: 'list' },
  { domain: 'projects', state: 'page', route: '/prj-task-kanban' },
  { domain: 'crm', state: 'page', route: '/crm-activity-timeline' },
  { domain: 'hr', state: 'page', route: '/hr-org-chart' },
  { domain: 'logistics', entity: 'ErpLogShipment', state: 'edit-drawer', rowCode: 'SHP-2026-001' },
  { domain: 'drp', entity: 'ErpDrpPlan', state: 'view-drawer', rowCode: 'DRP-PLAN-SEED-001' },
];

// Batch D — secondary-entity list states (plan 2026-09-04-1721-1 M2.1).
// Covers a second representative entity per domain to catch domain-specific
// layout regressions beyond the main CRUD entity in Batch A.
const SECONDARY_LIST_CFGS: CrudSnapshotCfg[] = [
  { domain: 'master-data', entity: 'ErpMdMaterial', state: 'list' },
  { domain: 'inventory', entity: 'ErpInvStockBalance', state: 'list' },
  { domain: 'purchase', entity: 'ErpPurReceive', state: 'list' },
  { domain: 'sales', entity: 'ErpSalDelivery', state: 'list' },
  { domain: 'finance', entity: 'ErpFinArApItem', state: 'list' },
  { domain: 'assets', entity: 'ErpAstDepreciationSchedule', state: 'list' },
  { domain: 'projects', entity: 'ErpPrjTask', state: 'list' },
  { domain: 'manufacturing', entity: 'ErpMfgBom', state: 'list' },
  { domain: 'quality', entity: 'ErpQaNonConformance', state: 'list' },
  { domain: 'maintenance', entity: 'ErpMntEquipment', state: 'list' },
  { domain: 'crm', entity: 'ErpCrmCampaign', state: 'list' },
  { domain: 'cs', entity: 'ErpCsKnowledgeBase', state: 'list' },
  { domain: 'hr', entity: 'ErpHrSalary', state: 'list' },
  { domain: 'logistics', entity: 'ErpLogCarrier', state: 'list' },
  { domain: 'contract', entity: 'ErpCtTemplate', state: 'list' },
];

// Batch E — edit-drawer states for main entities (plan 2026-09-04-1721-1 M2.1).
// Pins a stable seed row per entity to capture the edit-form layout with
// pre-populated fields, required-field markers, and tab structures.
const EDIT_DRAWER_CFGS: CrudSnapshotCfg[] = [
  { domain: 'master-data', entity: 'ErpMdPartner', state: 'edit-drawer', rowCode: 'CUST-001' },
  { domain: 'purchase', entity: 'ErpPurOrder', state: 'edit-drawer', rowCode: 'PO-2026-001' },
  { domain: 'sales', entity: 'ErpSalOrder', state: 'edit-drawer', rowCode: 'SO-2026-001' },
  { domain: 'finance', entity: 'ErpFinVoucher', state: 'edit-drawer', rowCode: 'PZ-2026-001' },
  { domain: 'manufacturing', entity: 'ErpMfgWorkOrder', state: 'edit-drawer', rowCode: 'WO-2026-001' },
  { domain: 'assets', entity: 'ErpAstAsset', state: 'edit-drawer', rowCode: 'AST-2026-001' },
  { domain: 'projects', entity: 'ErpPrjProject', state: 'edit-drawer', rowCode: 'PRJ-2026-001' },
  { domain: 'quality', entity: 'ErpQaInspection', state: 'edit-drawer', rowCode: 'INS-2026-001' },
  { domain: 'maintenance', entity: 'ErpMntVisit', state: 'edit-drawer', rowCode: 'VIS-2026-001' },
  { domain: 'hr', entity: 'ErpHrEmployee', state: 'edit-drawer', rowCode: 'HR-EMP-001' },
];

for (const [batch, cfgs] of [
  ['batch-A', LIST_CFGS],
  ['batch-B', ADD_FORM_CFGS],
  ['batch-C', NONSTANDARD_CFGS],
  ['batch-D', SECONDARY_LIST_CFGS],
  ['batch-E', EDIT_DRAWER_CFGS],
] as const) {
  for (const cfg of cfgs) {
    test.describe(`${batch} :: ${cfg.domain} CRUD pixel snapshot`, () => {
      test(`${cfg.entity ?? cfg.route} ${cfg.state} snapshot`, async ({ page }) => {
        await driveAndSnapshot(page, cfg);
      });
    });
  }
}
