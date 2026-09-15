# 数据库与业务设计优化评估 — 输出索引

> **Roadmap**：`docs/backlog/optimization-audit-roadmap.md`（v3.3 深度调研版）
> **真相源**：`00-methodology.md`（调研方法、产出清单、反空话门槛、深度门槛）
> **验证标准**：`00-checklist.md`
> **本文档职责**：文件索引与背景，不复述方法论内容（**避免失效漂移**）

---

## ⚠️ 重要：框架是脚手架，不是天花板

> **给执行者/子 agent 的提醒**：
>
> 本 README、方法论、checklist 是**参考框架**，不是评估的全部。
>
> 真正有价值的评估来自：
> - 子 agent 的**独特洞察**——那些不在框架中、但确实存在于代码中的别扭模式
> - 子 agent 对**框架本身的质疑**——如果发现框架的某条标准不适用本域，应在评估文档中说明
> - 子 agent 的**跨域关联发现**——X 域与 Y 域的共性、Z 域与 W 域的反差
>
> **底线**：满足框架最低门槛 + 独特洞察 = 合格的深度评估。

---

## 评估背景

### 为什么需要本路线图

`nop-app-erp` 当前（2026-09-12）状态：
- 18 业务域 + 1 跨域通知派发子系统（共 19 个 `module-*`）
- ORM 模型：19 orm.xml / 21,669 行 / ~495 本模块实体
- BizModel：86+ 含真实 `@BizMutation`/`@BizQuery` 方法
- 状态机 Bean：105 个
- E2E 测试：298 spec 业务动作 + 24 报表 + 10 看板

**既有审计/分析已覆盖**（背景详见本节下方清单）：
- `audit-remediation-roadmap.md`（v28）— P0/P1 修复
- `requirement-compliance-roadmap.md` — 需求-实现符合性
- `entity-state-machine-migration-roadmap.md` — 状态机 Bean 迁移

**本路线图填补**：
- 🆕 **设计层优雅性评估**——非缺陷但可优化
- 🆕 **业务需求-模型契合度评估**——业务实现是否"别扭"
- 🆕 **数据量视角的设计评估**——大数据量表的影响
- 🆕 **跨域数据流"别扭"模式识别**

### 与既有审计的根本区别

| 既有审计 | 本路线图 |
|----------|----------|
| 找 bug 并修复 | 评估设计是否最优 |
| 大批量并发调研 | **每域一个独立子 agent 单点深度调研** |
| 修复导向 | **审查导向** |
| 模板化扫描 | **每个工作项有专属调研清单 + 产出清单** |

## 评估路线图结构

```
M0 编排基线（done）
M1 单域数据库设计评估（19 域，每域 1 独立子 agent）
  ├ M1.S（4 域：finance/mfg/hr/crm）— 双独立子 agent 审查
  ├ M1.A（9 域）— 单独立子 agent
  ├ M1.B（3 域）— 单独立子 agent
  └ M1.C（3 域）— 单独立子 agent
M2 单域业务逻辑评估（19 域，每域 1 独立子 agent）
  ├ M2.S（4 域）— 双独立子 agent 审查
  ├ M2.A（9 域）— 单独立子 agent
  ├ M2.B（3 域）— 单独立子 agent
  └ M2.C（3 域）— 单独立子 agent
M3 跨域综合分析（8 维度）— 全部双独立子 agent 审查
M4 UI/UX 一致性评估（5 维度，条件触发）
M5 汇总报告 — 双独立子 agent 审查
```

## 阅读路径

### 主入口路径（先看这个）

1. `docs/backlog/optimization-audit-roadmap.md` — roadmap（必读）
2. `00-methodology.md` — 调研方法论（必读）
3. `00-checklist.md` — 验证标准（必读）
4. `00-summary-report.md`（M5.2 完成后）— 汇总报告

### 单域深度路径（按需深入）

- 数据库层：`domain-NN-{slug}.md`（19 域，按 S→A→B→C 优先级）
- 业务层：`biz-NN-{slug}.md`（与 domain 一一对应）
- 跨域综合：`cross-NN-{slug}.md`（8 维度）

### UI 路径（M4 条件触发）

- `ui-NN-{slug}.md`（5 维度）

## 文件索引

### 0. 方法论与汇总

| 文件 | 描述 | 状态 |
|------|------|------|
| `00-methodology.md` | 真相源：调研方法 + 产出清单 + 反空话门槛 + 深度门槛 + 四大量表 | M0.2 done（v3.3） |
| `00-checklist.md` | 验证标准 | M0.3 done（v3.3） |
| `00-summary-findings.md` | 优化机会清单（M5.1） | M5.1 todo |
| `00-summary-report.md` | 汇总报告（M5.2） | M5.2 todo |

### 1. 单域数据库设计评估（M1，19 域）

> **门槛**：见 `00-methodology.md §5`（本文不复述，避免漂移）。

| 文件 | 域 | 实体数 | 复杂度 | 工作量 | 状态 |
|------|----|--------|--------|--------|------|
| `domain-01-finance.md` | finance | 51 | S | 3-4d + 0.5-1d 审查 | ready |
| `domain-02-manufacturing.md` | manufacturing | 45 | S | 3-4d + 0.5-1d 审查 | ready |
| `domain-03-hr.md` | hr | 42 | S | 3-4d + 0.5-1d 审查 | ready |
| `domain-04-crm.md` | crm | 41 | S | 3-4d + 0.5-1d 审查 | ready |
| `domain-05-purchase.md` | purchase | 34 | A | 1-2d | ready |
| `domain-06-sales.md` | sales | 29 | A | 1-2d | ready |
| `domain-07-inventory.md` | inventory | 32 | A | 1-2d | ready |
| `domain-08-assets.md` | assets | 27 | A | 1-2d | ready |
| `domain-09-maintenance.md` | maintenance | 24 | A | 1-2d | ready |
| `domain-10-projects.md` | projects | 23 | A | 1-2d | ready |
| `domain-11-quality.md` | quality | 22 | A | 1-2d | ready |
| `domain-12-cs.md` | cs (customer-service) | 21 | A | 1-2d | ready |
| `domain-13-master-data.md` | master-data | 25 | A | 1-2d | ready |
| `domain-14-contract.md` | contract | 20 | B | 0.5-1d | ready |
| `domain-15-b2b.md` | b2b | 17 | B | 0.5-1d | ready |
| `domain-16-drp.md` | drp | 17 | B | 0.5-1d | ready |
| `domain-17-aps.md` | aps | 8 | C | 1d | ready |
| `domain-18-logistics.md` | logistics | 14 | C | 1d | ready |
| `domain-19-notify.md` | notify | 3 | C | 1d | ready |

### 2. 单域业务逻辑评估（M2，19 域）

| 文件 | 域 | 复杂度 | 工作量 | 状态 |
|------|----|--------|--------|------|
| `biz-01-finance.md` | finance | S | 3-4d + 0.5-1d 审查 | ready |
| `biz-02-manufacturing.md` | manufacturing | S | 3-4d + 0.5-1d 审查 | ready |
| `biz-03-hr.md` | hr | S | 3-4d + 0.5-1d 审查 | ready |
| `biz-04-crm.md` | crm | S | 3-4d + 0.5-1d 审查 | ready |
| `biz-05-purchase.md` | purchase | A | 1-2d | ready |
| `biz-06-sales.md` | sales | A | 1-2d | ready |
| `biz-07-inventory.md` | inventory | A | 1-2d | ready |
| `biz-08-assets.md` | assets | A | 1-2d | ready |
| `biz-09-maintenance.md` | maintenance | A | 1-2d | ready |
| `biz-10-projects.md` | projects | A | 1-2d | ready |
| `biz-11-quality.md` | quality | A | 1-2d | ready |
| `biz-12-cs.md` | cs | A | 1-2d | ready |
| `biz-13-master-data.md` | master-data | A | 1-2d | ready |
| `biz-14-contract.md` | contract | B | 0.5-1d | ready |
| `biz-15-b2b.md` | b2b | B | 0.5-1d | ready |
| `biz-16-drp.md` | drp | B | 0.5-1d | ready |
| `biz-17-aps.md` | aps | C | 1d | ready |
| `biz-18-logistics.md` | logistics | C | 1d | ready |
| `biz-19-notify.md` | notify | C | 1d | ready |

### 3. 跨域综合分析（M3，8 维度）— **全部双独立子 agent 审查**

| 文件 | 维度 | 工作量 | 状态 |
|------|------|--------|------|
| `cross-01-entity-overlap.md` | 跨域同名/同义实体对比 | 2-3d + 0.5-1d 审查 | ready |
| `cross-02-naming-conventions.md` | 跨域命名规范评估 | 1-2d + 1d 审查 | ready |
| `cross-03-field-standards.md` | 跨域字段标准评估 | 2-3d + 0.5-1d 审查 | ready |
| `cross-04-dictionary-reuse.md` | 跨域字典复用与冲突 | 1-2d + 1d 审查 | ready |
| `cross-05-dependency-flow.md` | 跨域依赖与数据流 | 2-3d + 0.5-1d 审查 | ready |
| `cross-06-redundancy-and-restructure.md` | 跨域冗余与可优化结构 | 2-3d + 0.5-1d 审查 | ready |
| `cross-07-index-and-performance.md` | 跨域性能与索引 | 2-3d + 0.5-1d 审查 | ready |
| `cross-08-business-rule-consistency.md` | 跨域业务规则一致性 | 2-3d + 0.5-1d 审查 | ready |

### 4. UI/UX 一致性评估（M4，5 维度，条件触发）

> **触发条件**（二元化）：M1+M2+M3 完成后剩余预算 ≥ 10 工作日 AND M1+M2+M3 输出中 UI 相关优化机会 ≥ 5 条（"UI 相关"= 触动 view.xml/page.yaml/UX 一致性的建议）

| 文件 | 维度 | 工作量 | 状态 |
|------|------|--------|------|
| `ui-01-menu-structure.md` | 菜单结构与权限呈现 | 1d | ready（条件触发） |
| `ui-02-grid-consistency.md` | 列表页一致性 + 大数据量表性能 | 1-2d | ready（条件触发） |
| `ui-03-form-consistency.md` | 表单页一致性 + 业务需求表达 | 1-2d | ready（条件触发） |
| `ui-04-button-interaction.md` | 按钮与交互一致性 + 业务动作覆盖度 | 1d | ready（条件触发） |
| `ui-05-dashboard-report.md` | 看板与报表一致性 + 跨域数据流可视化 | 1-2d | ready（条件触发） |

## 评估硬约束

> **详见** `00-methodology.md §9 评估硬约束`（本文不复述）

本节要点：不引入代码变更 / 不重复既有审计 / 达到深度门槛 / 关键工作项双审查 / 保护区域标注 / auditRef 默认 NEW。**文件大小约束见 methodology §5**。

## 优先级矩阵

> **详见** `00-methodology.md §5`（优先级/风险/工作量判定规则；本文不复述）

## 数据量估算口径

> **详见** `00-methodology.md §6`（口径表 + 估算模板；本文不复述）

## 四大量表（v3.3 增补）

> **详见** `00-methodology.md §11`：
> - §11.2 业务需求-模型契合度 4 级量表（优/良/中/差判据）
> - §11.3 状态机"别扭"评分维度（轴数/死状态/可达性/守卫/命名）
> - §11.4 UI 一致性评分维度（菜单/列表/表单/按钮/看板）
> - §11.5 优化机会"维度"编码表（D1-D6 / B1-B4 / U1-U5 / C1-C8）

## 给子 agent 的最后提醒

> **本 README + 方法论 + checklist 是脚手架，不是天花板**。
>
> 真正有价值的评估来自你的**独特洞察**——那些不在框架中、但确实存在于代码中的别扭模式。
>
> **鼓励**：
> - 质疑框架的标准
> - 发现框架未覆盖的别扭模式
> - 提出比框架建议更深入的改进方案
> - 跨子 agent 关联发现
>
> **底线**：框架最低门槛 + 独特洞察 = 合格的深度评估。
