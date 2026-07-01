# 文档命名和时效性

## 目的

本指南区分稳定的 owner docs 与时敏性流程记录。

对于中小型项目，这使仓库易于导航，而无需将每个文件强制转换为相同的命名风格。

## 两类文档

### 1. 稳定的 Owner Docs

这些描述当前支持的基线，通常应保持稳定名称而不包含日期。

使用稳定名称：

- `docs/process/`
- `docs/architecture/`
- `docs/design/`
- `docs/references/`
- `docs/skills/`
- 长期需求基线文件，如 `docs/requirements/product-scope.md` 和 `docs/requirements/product-baseline.md`

示例：

- `docs/design/app-overview.md`
- `docs/architecture/system-baseline.md`
- `docs/process/application-development-workflow.md`

规则：

- 这些文件应就地更新
- 不要仅仅因为内容改变就创建新的带日期版本

### 2. 时敏性记录

这些捕获执行历史、调查上下文或日期决策。

这些文件通常应在路径或文件名中包含日期。

使用日期命名：

- `docs/logs/`
- `docs/testing/`
- `docs/discussions/`
- `docs/analysis/`
- `docs/audits/`
- `docs/retrospectives/`
- 大多数一次性需求综合文件和实施计划

## 推荐路径约定

### 日志

- `docs/logs/YYYY/MM-DD.md`

### 测试笔记

- `docs/testing/YYYY/MM-DD.md`

### 讨论

- `docs/discussions/YYYY-MM-DD-HHmm-topic.md`

示例：

- `docs/discussions/2026-05-21-0000-user-management-scope.md`
- `docs/discussions/2026-05-21-0000-order-status-rules.md`
- `docs/discussions/2026-05-21-0000-prototype-gap-checkout-flow.md`

### 分析

- `docs/analysis/YYYY-MM-DD-HHmm-topic.md`

示例：

- `docs/analysis/2026-05-21-0000-menu-structure-options.md`
- `docs/analysis/2026-05-21-0000-prototype-feasibility-review.md`
- `docs/analysis/2026-05-21-0000-auth-strategy-comparison.md`

### 审计

- `docs/audits/YYYY-MM-DD-HHmm-<kind>-<topic>.md`

示例：

- `docs/audits/2026-05-21-0000-document-audit-user-management.md`
- `docs/audits/2026-05-21-0000-closure-audit-order-list.md`

仅当存储的审计记录是非平凡的、有争议的、可复用的或以后可能重要时，才使用 `docs/audits/`。创建计划的草案审查通常保留在计划正文中，结束证据通常保留在计划的 `## Closure` 部分。

### 回顾

- `docs/retrospectives/YYYY-MM-DD-HHmm-topic.md`

示例：

- `docs/retrospectives/2026-05-21-0000-checkout-prototype-gap.md`
- `docs/retrospectives/2026-05-21-0000-pm-handoff-missing-analysis.md`

### 计划

对于中小型项目，首选简单的带日期计划名称：

- `docs/plans/YYYY-MM-DD-HHmm-topic-plan.md`

示例：

- `docs/plans/2026-05-21-0000-user-list-plan.md`
- `docs/plans/2026-05-21-0000-role-permission-alignment-plan.md`

如果项目后来积累了许多计划并需要更强的索引，可以添加数字前缀：

- `docs/plans/NNN-YYYY-MM-DD-HHmm-topic-plan.md`

示例：

- `docs/plans/012-2026-05-21-0000-user-list-plan.md`
- `docs/plans/013-2026-05-21-0000-checkout-validation-plan.md`

### 一次性需求综合文件

如果文件是一次性切片而非稳定基线文件，首选带日期名称：

- `docs/requirements/YYYY-MM-DD-HHmm-feature-name.md`

示例：

- `docs/requirements/2026-05-21-0000-user-management.md`
- `docs/requirements/2026-05-21-0000-order-refund-flow.md`
- `docs/requirements/2026-05-21-0000-dashboard-homepage.md`

## Bug 笔记

Bug 笔记是历史记录，但通常按问题标识而非日期引用。

对于中小型项目，以下两种方式均可接受：

- `docs/bugs/01-short-bug-name.md`
- `docs/bugs/YYYY-MM-DD-HHmm-short-bug-name.md`

示例：

- `docs/bugs/01-order-status-double-submit.md`
- `docs/bugs/2026-05-21-0000-login-token-refresh-loop.md`

建议：

- 如果 bug 笔记将成为长期参考库，首选编号文件名
- 如果 bug 笔记数量较少且主要服务于本地团队记忆，基于日期的文件名是可以接受的

## 简单经验法则

- 如果文件回答"当前支持的基线是什么？" -> 稳定名称
- 如果文件回答"这一轮/这一天/这次调查发生了什么？" -> 带日期名称

## 归档组织

当文件根据人工决策归档时，保持可预测的子结构，以便历史材料保持可恢复：

- 设计和架构文档：归档在 `docs/archive/design/` 和 `docs/archive/architecture/` 下，按原始模块或主题名称组织
- 带日期的计划：归档在 `docs/archive/plans/YYYY-MM/` 下，按关闭年份和月份分组
- 其他带日期的记录（日志、bug、审计、测试、分析、回顾）：在 `docs/archive/` 下保留其原始相对路径

归档文件保留其原始相对名称。未经人工批准，不要将文件移入或移出 `docs/archive/`。

## 快速复制集

使用这些作为现成模式：

```text
docs/logs/2026/05-21.md
docs/testing/2026/05-21.md
docs/discussions/2026-05-21-0900-user-management-scope.md
docs/analysis/2026-05-21-1030-auth-strategy-comparison.md
docs/audits/2026-05-21-1410-document-audit-user-management.md
docs/plans/2026-05-21-1530-user-list-plan.md
docs/requirements/2026-05-21-1100-order-refund-flow.md
docs/retrospectives/2026-05-21-1630-checkout-prototype-gap.md
docs/bugs/01-order-status-double-submit.md
```