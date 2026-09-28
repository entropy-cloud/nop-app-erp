## SNAPSHOT (machine-readable)

> 本块由 `node tools/check-plan-gates.mjs --baseline` 自动整体重生成（勿手改；误改可 `git checkout` 还原复验）。
> 门控方向：单向收紧（actual 只降不升）。`--strict` 解析本块做 per-file per-检查类计数比对：新增违规 = 非零退出；
> 计数下降仅在报告列 IMPROVEMENTS 提示收紧，**永不自动改写本快照**。快照的违规登记 ≠ 合法化：
> 存量清单即整改分诊面（C1 f38 类仅需补勾 / f310、01-product-grade 类需重审 / C5 旧约定类可批量裁决后收紧），
> 整改随所属 mission 复访触发（见 GATE-02 计划 Deferred）。
> C2 已知局限：自审自认证据形态机械不可靠定（01-product-grade 为首例），列入人工分诊清单。

```yaml
generated: 2026-09-28T11:29:49.542Z
scanned: 944
  docs/plans/01-product-grade-erp-model-overhaul.md: { C1: 58 }
  docs/plans/02-documentation-improvement-plan.md: { C1: 6, C2: 1 }
  docs/plans/2026-07-03-1000-1-bizmodel-productization-refactor.md: { C2: 1 }
  docs/plans/2026-07-03-1018-1-m4-business-finance-e2e-tests.md: { C2: 1 }
  docs/plans/2026-07-03-1018-2-projects-cost-collection.md: { C2: 1 }
  docs/plans/2026-07-03-1018-3-maintenance-visit-request-sparepart-downtime.md: { C2: 1 }
  docs/plans/2026-07-03-1707-3-quality-recall-event.md: { C2: 1 }
  docs/plans/2026-07-04-0831-1-aps-operation-order-scheduling-engine.md: { C2: 1 }
  docs/plans/2026-07-05-1000-1-unique-key-constraints.md: { C2: 1 }
  docs/plans/2026-07-07-1530-1-errorcode-nop-to-erp-migration.md: { C2: 1 }
  docs/plans/2026-07-15-2246-2-plan-2256-2-closure-audit.md: { C1: 18 }
  docs/plans/2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-reconciliation.md: { C1: 23, C2: 1 }
  docs/plans/2026-08-02-1815-2-rc-ma1-a1-5-finance-f5-costing.md: { C2: 1 }
  docs/plans/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md: { C2: 1 }
  docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md: { C2: 1 }
  docs/plans/2026-08-07-0330-1-rc-ma4-a4-1-1-posted-false-listener-coverage.md: { C2: 1 }
  docs/plans/2026-08-07-0330-3-rc-ma4-a4-1-3-project-settlement-provider-census.md: { C2: 1 }
  docs/plans/2026-08-08-0015-2-rc-ma4-a4-2-143-146-master-data-runtime.md: { C2: 1 }
  docs/plans/2026-08-12-1841-3-erpmfg-forecast-state-machine-bean.md: { C2: 1 }
  docs/plans/2026-08-13-1430-1-erpmfg-jobcard-mrpplan-state-machine-beans.md: { C2: 1 }
  docs/plans/2026-08-14-0930-1-manufacturing-m4-state-machine-beans.md: { C2: 1 }
  docs/plans/2026-08-25-1956-1-confidential-read-path-masking.md: { C5: 1 }
  docs/plans/2026-08-25-1956-2-action-auth-seed-comma-fix.md: { C5: 1 }
  docs/plans/2026-08-25-1956-3-approval-adjacent-xbiz-auth-backfill.md: { C5: 1 }
  docs/plans/2026-08-26-0330-1-ai-check-fix-f1-2-requires-new-orphan-voucher-family.md: { C1: 1 }
  docs/plans/2026-08-26-0430-1-ai-check-fix-f1-3-crud-status-guard-base.md: { C1: 3, C4: 2 }
  docs/plans/2026-08-26-0530-1-ai-check-fix-f1-4-acct-schema-fail-closed.md: { C1: 3, C4: 2 }
  docs/plans/2026-08-28-1607-1-entity-state-machine-m5-1-matrix-audit.md: { C5: 1 }
  docs/plans/2026-08-28-1607-2-entity-state-machine-m5-2-guard-script-and-ci.md: { C5: 1 }
  docs/plans/2026-09-02-2028-1-flux-picker-schema-override.md: { C2: 1 }
  docs/plans/2026-09-03-1930-1-flux-runtime-datasource-formula-regression-fix.md: { C1: 42 }
  docs/plans/2026-09-04-1721-1-m2-visual-pixel-assertion-expansion.md: { C5: 1 }
  docs/plans/2026-09-06-1451-1-i18n-compliance-standard.md: { C5: 1 }
  docs/plans/2026-09-06-1451-2-cjk-detection-baseline.md: { C5: 1 }
  docs/plans/2026-09-06-1451-3-m0-audit-checklists-closure.md: { C5: 1 }
  docs/plans/2026-09-06-2104-1-mi1-errors-locale-zhcn.md: { C5: 1 }
  docs/plans/2026-09-06-2104-2-mi2-log-english-finance-assets.md: { C5: 1 }
  docs/plans/2026-09-06-2104-3-mi3-log-english-batch2.md: { C5: 1 }
  docs/plans/2026-09-07-0043-1-mi4-log-english-batch3-cat1-zero.md: { C5: 1 }
  docs/plans/2026-09-07-0043-2-mi5a-exception-param-batch1.md: { C5: 1 }
  docs/plans/2026-09-07-0043-3-mi5b-exception-param-batch2-cat2-zero.md: { C5: 1 }
  docs/plans/2026-09-07-0902-1-mi6-runtime-string-cat3-batch1.md: { C5: 1 }
  docs/plans/2026-09-07-0902-2-mi7-page-yaml-codegen-source-verify.md: { C5: 1 }
  docs/plans/2026-09-07-0902-3-mi8-handwritten-yaml-i18nen-batch1.md: { C5: 1 }
  docs/plans/2026-09-07-1715-1-mi6-runtime-string-cat3-batch2.md: { C5: 1 }
  docs/plans/2026-09-07-1715-2-mi8-handwritten-yaml-i18nen-batch2.md: { C5: 1 }
  docs/plans/2026-09-07-1715-3-mi9-mi-closure-strict-green.md: { C5: 1 }
  docs/plans/2026-09-08-1042-1-m11-finance-posting-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-1042-2-m15-mfg-workorder-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-1042-3-m18-ast-lifecycle-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-1454-1-m110-hr-org-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-1454-2-m112-projects-quality-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-1454-3-m113-pur-sal-inv-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-2238-1-m12-finance-arap-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-2238-2-m13-finance-budget-costing-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-08-2238-3-m14-finance-period-close-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-0232-1-m16-mfg-bom-mrp-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-0232-2-m17-mfg-subcontract-genealogy-variance-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-0547-1-m111-hr-attendance-payroll-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-0547-2-m114-crm-cs-ct-b2b-drp-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-0547-3-m115-mnt-aps-log-notify-md-common-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-09-2100-1-compliance-baseline-raise-adjudication.md: { C5: 1 }
  docs/plans/2026-09-09-2100-2-m116-app-erp-all-cross-cutting-five-dim-audit.md: { C5: 1 }
  docs/plans/2026-09-10-0251-1-m117-audit-phase-closure.md: { C5: 1 }
  docs/plans/2026-09-10-0425-1-m20-fix-method-baseline-family-adjudication.md: { C5: 1 }
  docs/plans/2026-09-10-0425-2-m23-manufacturing-p1-fix-batch.md: { C5: 1 }
  docs/plans/2026-09-10-0705-1-m22-finance-intercompany-dimension-fix.md: { C5: 1 }
  docs/plans/2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix.md: { C5: 1 }
  docs/plans/2026-09-10-0705-3-m25-pur-inv-settlement-serial-fix.md: { C5: 1 }
  docs/plans/2026-09-10-1141-1-m26-qa-domain-fix-batch.md: { C5: 1 }
  docs/plans/2026-09-10-1141-2-m27-remaining-domains-p1-fix-batch.md: { C5: 1 }
  docs/plans/2026-09-10-1141-3-m28-cross-cutting-family-fix-batch.md: { C5: 1 }
  docs/plans/2026-09-11-0457-1-m29-remediation-closure-index-backfill.md: { C5: 1 }
  docs/plans/2026-09-11-0457-2-mv1-full-regression-final-baseline.md: { C5: 1 }
  docs/plans/2026-09-11-0906-1-m29-r1b-r1d-baseline-adjudication.md: { C5: 1 }
  docs/plans/2026-09-11-0906-2-mv2-index-terminal-state-verification.md: { C5: 1 }
  docs/plans/2026-09-11-0906-3-mv3-methodology-lessons.md: { C5: 1 }
  docs/plans/2026-09-11-1425-1-mg1-lessons-skills-consolidation.md: { C5: 1 }
  docs/plans/2026-09-11-1425-2-mg2-roadmap-status-writeback.md: { C5: 1 }
  docs/plans/2026-09-17-0330-1-ai-check-fix-f38-fin-posting-p2-batch.md: { C1: 2 }
  docs/plans/2026-09-17-0800-1-ai-check-fix-f310-mfg-p2-batch.md: { C1: 1 }
```
