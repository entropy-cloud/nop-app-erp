# 代码库映射

## 目的

本文件为 AI 代理提供实时仓库的紧凑映射，使其不必通过反复搜索导入和目录来重新发现结构。

保持足够新以路由常见工作。不要将其变成完整的架构文档。

## 入口点

> **Bootstrap 阶段：** Java 模块尚不存在。实时真相源是 5 个 `model/app-erp-<domain>.orm.xml` 文件和 `docs/` 树。标记为"codegen 后"的行描述未来结构。模块拆分决策见 `docs/architecture/domain-module-split-analysis.md`。

| 区域 | 路径 | 说明 | 最后验证 | 置信度 |
| ------------------- | ------------------------------------- | ----------------------------------------------------------- | ------------- | ---------- |
| ORM 模型（真相） | `module-master-data/model/app-erp-master-data.orm.xml` | 主数据域权威模型（物料/SKU/往来单位/仓库/科目表） | 2026-06-22 | high |
| ORM 模型（真相） | `module-inventory/model/app-erp-inventory.orm.xml` | 库存域权威模型（移动单/流水/余额/调拨/盘点） | 2026-06-22 | high |
| ORM 模型（真相） | `module-purchase/model/app-erp-purchase.orm.xml` | 采购域权威模型（订单/入库/发票/付款/退货） | 2026-06-22 | high |
| ORM 模型（真相） | `module-sales/model/app-erp-sales.orm.xml` | 销售域权威模型（订单/出库/发票/收款/退货） | 2026-06-22 | high |
| ORM 模型（真相） | `module-finance/model/app-erp-finance.orm.xml` | 财务域权威模型（凭证/科目/核销/期末结账） | 2026-06-22 | high |
| ORM 模型（真相） | `module-assets/model/app-erp-assets.orm.xml` | 固定资产域权威模型（资产卡片/折旧/资本化/处置） | 2026-06-22 | high |
| ORM 模型（真相） | `module-projects/model/app-erp-projects.orm.xml` | 项目管理域权威模型（项目/任务/工时） | 2026-06-22 | high |
| ORM 模型（真相） | `module-manufacturing/model/app-erp-manufacturing.orm.xml` | 制造域权威模型（BOM/工单/作业卡/工艺路线） | 2026-06-22 | high |
| ORM 模型（真相） | `module-quality/model/app-erp-quality.orm.xml` | 质量管理域权威模型（质检/NCR/CAPA） | 2026-06-22 | high |
| ORM 模型（真相） | `module-maintenance/model/app-erp-maintenance.orm.xml` | 设备维护域权威模型（设备/维护计划/访问/请求） | 2026-06-22 | high |
| ORM 模型（已废弃） | `model/app-erp.orm.xml` | bootstrap 占位骨架，已废弃，待删除或转聚合入口 | 2026-06-22 | high |
| API 模型 | `<domain>/model/app-erp-<domain>.api.xml` | 代码生成期间/之后生成；尚未存在 | — | high |
| 文档路由器 | `docs/index.md` | 顶层导航 | 2026-06-22 | high |
| 领域工程（10 个） | `app-erp-<domain>/` | 每域独立 Maven 工程，由 nop-cli gen 生成 — codegen 后 | — | medium |
| 聚合启动工程 | `app-erp-app/` | Quarkus main + 聚合所有领域工程依赖 — codegen 后 | — | medium |

## 常见变更路由

| 任务类型 | 从这里开始 | 然后检查 | 验证 | 最后验证 | 置信度 |
| ------------------------- | -------------------------------- | --------------------------------------------------- | ----------------------- | ------------- | ---------- |
| 设计实体/字典 | `model/app-erp.orm.xml` | `docs/design/`、`../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` | schema review | 2026-06-22 | high |
| 更改模型/模式 | `model/app-erp.orm.xml` | owner doc + plan（保护区域：`ask first`） | regenerate + build | 2026-06-22 | high |
| 生成模块 | `model/app-erp.orm.xml` | `codegen.sh`、`../nop-entropy/docs-for-ai/03-runbooks/` | `mvn compile` | — | medium |
| 添加页面/屏幕（后期） | `app-erp-web/_vfs/.../*.view.xml` | 相关设计文档 | `mvn test` / manual | — | medium |
| 添加 API/处理器（后期） | `app-erp-service/` BizModel | `../nop-entropy/docs-for-ai/03-runbooks/write-bizmodel-method.md` | `mvn test` | — | medium |
| 更改权限（后期） | `app-erp-app/_vfs/app/erp/auth/` | `docs/design/roles-and-permissions.md` | manual / e2e | — | medium |

## 大型或脆弱文件

列出代理应谨慎处理的文件，因为它们很大、是核心文件、是生成的或容易编辑错误。

| 路径 | 风险 | 首选方法 |
| ------------------------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| `model/app-erp.orm.xml` | 核心文件；驱动所有代码生成；保护区域 | 直接编辑；模式更改需要计划/设计文档 |
| `model/app-erp.api.xml` | 生成的契约（代码生成后） | 切勿手动编辑；从 ORM 模型重新生成 |
| `*/_gen/`、`_` 前缀文件 | 自动生成；手动编辑会静默丢失 | 切勿手动编辑；更改源模型并重新生成 |
| `app-erp-app/_vfs/_delta/` | Delta 覆盖 nop core | 通过 delta 机制编辑，切勿修补 nop core |

## 项目特定搜索提示

- 使用文件模式：`model/*.orm.xml`、`model/*.api.xml`、`docs/design/*.md`、`app-erp-*/src/**/*.java`（代码生成后）
- 使用内容锚点：实体类名使用 `Erp` 前缀（例如 `ErpMaterial`），字典命名空间 `erp/<name>`
- 避免编辑生成文件：任何 `_gen/` 目录、任何带 `_` 前缀的文件、`_app.orm.xml`、`_service.beans.xml`

## 更新规则

当更改创建新的主要入口点、移动公共代码、添加新测试位置或反复导致代理重新发现相同路径时，更新此文件。

如果列出的路径缺失、占位符仍然存在或实时导入与此映射冲突，请不要将映射视为权威。使用实时仓库验证，然后在实现之前更新映射或标记该行置信度低。

如果"最后验证"对于项目的节奏来说已经过时、早于重大结构更改或任务触及列出路由的边界，请在依赖该行之前验证实时仓库。低置信度行在实时验证后不阻止低风险工作，但保护区域、迁移或跨模块工作应在实现之前更新该行。