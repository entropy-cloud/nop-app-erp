# Codebase Map

## Purpose

This file gives AI agents a compact map of the live repository so they do not rediscover the structure by repeatedly searching imports and directories.

Keep it current enough to route common work. Do not turn it into a full architecture document.

## Entry Points

> **Bootstrap stage:** Java modules do not exist yet. The live sources of truth are the 5 `model/app-erp-<domain>.orm.xml` files and the `docs/` tree. Rows marked "after codegen" describe the future structure. Module split decision见 `docs/architecture/domain-module-split-analysis.md`。

| Area                | Path                                  | Notes                                                       | Last Verified | Confidence |
| ------------------- | ------------------------------------- | ----------------------------------------------------------- | ------------- | ---------- |
| ORM model (truth)   | `model/master-data/app-erp-master-data.orm.xml` | 主数据域权威模型（物料/SKU/往来单位/仓库/科目表） | 2026-06-22 | high |
| ORM model (truth)   | `model/inventory/app-erp-inventory.orm.xml` | 库存域权威模型（移动单/流水/余额/调拨/盘点） | 2026-06-22 | high |
| ORM model (truth)   | `model/purchase/app-erp-purchase.orm.xml` | 采购域权威模型（订单/入库/发票/付款/退货） | 2026-06-22 | high |
| ORM model (truth)   | `model/sales/app-erp-sales.orm.xml` | 销售域权威模型（订单/出库/发票/收款/退货） | 2026-06-22 | high |
| ORM model (truth)   | `model/finance/app-erp-finance.orm.xml` | 财务域权威模型（凭证/科目/核销/期末结账） | 2026-06-22 | high |
| ORM model (truth)   | `model/assets/app-erp-assets.orm.xml` | 固定资产域权威模型（资产卡片/折旧/资本化/处置） | 2026-06-22 | high |
| ORM model (truth)   | `model/projects/app-erp-projects.orm.xml` | 项目管理域权威模型（项目/任务/工时） | 2026-06-22 | high |
| ORM model (truth)   | `model/manufacturing/app-erp-manufacturing.orm.xml` | 制造域权威模型（BOM/工单/作业卡/工艺路线） | 2026-06-22 | high |
| ORM model (truth)   | `model/quality/app-erp-quality.orm.xml` | 质量管理域权威模型（质检/NCR/CAPA） | 2026-06-22 | high |
| ORM model (truth)   | `model/maintenance/app-erp-maintenance.orm.xml` | 设备维护域权威模型（设备/维护计划/访问/请求） | 2026-06-22 | high |
| ORM model (deprecated) | `model/app-erp.orm.xml` | bootstrap 占位骨架，已废弃，待删除或转聚合入口 | 2026-06-22 | high |
| API model           | `model/<domain>/app-erp-<domain>.api.xml` | Generated during/after codegen; not yet present | — | high |
| Docs router         | `docs/index.md`                      | Top-level navigation                                        | 2026-06-22    | high       |
| 领域工程（10 个）    | `app-erp-<domain>/`                   | 每域独立 Maven 工程，由 nop-cli gen 生成 — after codegen    | —             | medium     |
| 聚合启动工程         | `app-erp-app/`                       | Quarkus main + 聚合所有领域工程依赖 — after codegen         | —             | medium     |

## Common Change Routes

| Task Type                 | Start Here                       | Then Check                                          | Verification            | Last Verified | Confidence |
| ------------------------- | -------------------------------- | --------------------------------------------------- | ----------------------- | ------------- | ---------- |
| Design entity / dict      | `model/app-erp.orm.xml`          | `docs/design/`, `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` | schema review           | 2026-06-22    | high       |
| Change model/schema       | `model/app-erp.orm.xml`          | owner doc + plan (protected: `ask first`)           | regenerate + build      | 2026-06-22    | high       |
| Generate modules          | `model/app-erp.orm.xml`          | `codegen.sh`, `../nop-entropy/docs-for-ai/03-runbooks/` | `mvn compile`           | —             | medium     |
| Add page/screen (later)   | `app-erp-web/_vfs/.../*.view.xml`| relevant design doc                                 | `mvn test` / manual     | —             | medium     |
| Add API/handler (later)   | `app-erp-service/` BizModel      | `../nop-entropy/docs-for-ai/03-runbooks/write-bizmodel-method.md` | `mvn test`        | —             | medium     |
| Change permissions (later)| `app-erp-app/_vfs/app/erp/auth/` | `docs/design/roles-and-permissions.md`              | manual / e2e            | —             | medium     |

## Large Or Fragile Files

List files that agents should treat carefully because they are large, central, generated, or easy to edit incorrectly.

| Path                            | Risk                                           | Preferred Approach                                         |
| ------------------------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| `model/app-erp.orm.xml`         | Central; drives all codegen; protected area    | Edit directly; require plan/design doc for schema changes  |
| `model/app-erp.api.xml`         | Generated contract (after codegen)             | Never hand-edit; regenerate from orm model                 |
| `*/_gen/`, `_`-prefixed files   | Auto-generated; manual edits silently lost     | Never hand-edit; change source model and regenerate        |
| `app-erp-app/_vfs/_delta/`      | Delta overrides over nop core                  | Edit via delta mechanism, never patch nop core             |

## Project-Specific Search Hints

- Use file patterns: `model/*.orm.xml`, `model/*.api.xml`, `docs/design/*.md`, `app-erp-*/src/**/*.java` (after codegen)
- Use content anchors: entity class names use `Erp` prefix (e.g. `ErpMaterial`), dict namespace `erp/<name>`
- Avoid editing generated files: any `_gen/` directory, any file with `_` prefix, `_app.orm.xml`, `_service.beans.xml`

## Update Rule

Update this file when a change creates a new major entry point, moves common code, adds a new test location, or repeatedly causes agents to rediscover the same path.

If a listed path is missing, placeholders remain, or live imports contradict this map, do not treat the map as authority. Verify with the live repo, then update the map or mark the row low confidence before implementation.

If `Last Verified` is old for the project's pace, predates major structural changes, or the task touches a listed route's boundary, verify the live repo before relying on the row. Low-confidence rows do not block low-risk work after live verification, but protected-area, migration, or cross-module work should update the row before implementation.
