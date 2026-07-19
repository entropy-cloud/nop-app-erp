# 视图按钮审计方法论

> 本文档定义从业务需求推导期望按钮清单的规则，以及各域审计报告的统一格式。

## 1. 基线按钮定义

### 1.1 CRUD 基线（所有实体默认期望）

| 位置 | 按钮 ID | 业务语义 | 来源 |
|------|---------|----------|------|
| toolbar | `add-button` | 新建/新增 | 列表页标准新建 |
| toolbar | `batch-delete-button` | 批量删除 | 列表页标准批量操作 |
| row | `row-view-button` | 查看详情 | 行数据查看 |
| row | `row-update-button` | 编辑 | 行数据编辑 |
| row | `row-delete-button` | 删除 | 行数据删除 |

> CRUD 基线适用于所有实体，除非 ui-patterns.md 或 state-machine.md 明确说明某页面不需要某按钮（如只读配置页不需要 add-button）。

### 1.2 审批/工作流按钮（业务单据实体期望）

| 按钮 ID | 业务语义 | 触发状态 | 来源 |
|---------|----------|----------|------|
| `row-submit-button` | 提交审核 | DRAFT → SUBMITTED | domain-design-guidelines |
| `row-withdraw-approval-button` | 撤回审核 | SUBMITTED → DRAFT | domain-design-guidelines |
| `row-approve-button` | 审核通过 | SUBMITTED → APPROVED | domain-design-guidelines |
| `row-reject-button` | 驳回 | SUBMITTED → REJECTED | domain-design-guidelines |
| `row-reverse-approve-button` | 反审核（已审批单据） | APPROVED → REJECTED | domain-design-guidelines |
| `row-cancel-button` | 作废/取消 | 多种 → CANCELLED | domain-design-guidelines |

### 1.3 域专用按钮（按 ui-patterns / state-machine 决定）

项目当前发现的域专用按钮：

| 按钮 ID | 业务语义 | 所在域 |
|---------|----------|--------|
| `row-screening-button` | 简历筛选 | hr |
| `row-interview-button` | 安排面试 | hr |
| `row-offer-button` | 发录用通知 | hr |
| `row-hire-button` | 确认入职 | hr |
| `row-transfer-button` | 转移/调拨 | 多域 |
| `row-allocate-preview-button` | 分配预览 | 多域 |
| `row-issue-materials-button` | 发料 | manufacturing |
| `row-receive-finished-button` | 完工入库 / 报工 | manufacturing |
| `row-post-processing-fee-button` | 加工费录入 | manufacturing |
| `row-authorization-button` | 授权设置 | nop-auth |
| `row-user-button` | 用户管理 | nop-auth |
| `row-add-child-button` | 添加子节点 | nop-* 树形 |
| `publish-button` | 发布 | nop-wf |
| `unpublish-button` | 取消发布 | nop-wf |
| `items-button` | 排期/列表项 | nop-wf |

### 1.4 审批按钮的自动生成机制（审计重要前提）

**在期望按钮推导之前，必须先判断实体所属的审批模式：**

| 模式 | ORM 标记 | 后端 xbiz | 前端 view.xml 按钮 | 审计预期 |
|------|----------|-----------|-------------------|----------|
| **DIRECT** | `tagSet="...use-approval..."` | codegen 自动生成 `submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval` | codegen **自动生成** `row-submit-button`/`row-approve-button`/`row-reject-button`/`row-withdraw-approval-button`/`row-reverse-approve-button`。**不自动生成** `row-cancel-button` | 无需检查 submit/approve/reject 按钮存在性（codegen 已覆盖）；只检查 `row-cancel-button` 及域专用按钮 |
| **WORKFLOW** | `useWorkflow="true"` | codegen + nop-wf `.xwf` 定义 | nop-wf 引擎运行时动态注入审批按钮；view.xml 不需要静态声明 | 不检查 submit/approve/reject（由 wf 引擎提供）；只检查域专用按钮 |
| **无审批** | 无 `use-approval` 标记 | 无标准审批 action | 无审批按钮 | 不检查 submit/approve/reject；只检查域专用按钮 |

**关键审计规则**：
1. `row-submit-button` / `row-approve-button` / `row-reject-button` / `row-withdraw-approval-button` / `row-reverse-approve-button`：对 DIRECT 模式的 `use-approval` 实体，codegen 自动生成，**不视为差距**。对 WORKFLOW 模式的 `useWorkflow` 实体，由 nop-wf 引擎提供，**不视为差距**。无审批模式的实体，**不应期望**这些按钮。
2. `row-cancel-button`：codegen **不自动生成**。无论何种模式，若 state-machine.md 或 ui-patterns.md 定义了 CANCELLED 状态且需要用户手动触发作废，视为期望按钮。
3. 域专用按钮（如 pass/fail、issue-materials、start/complete 等 codegen 无法自动推断的按钮）：在所有模式下均需手动实现，若设计文档明确要求而 view.xml 中不存在，视为真实差距。

## 2. Prose → Button-ID 翻译字典

ui-patterns.md 使用中文业务语言描述按钮。以下是常见映射：

| ui-patterns.md 原文 | 对应按钮 ID | 备注 |
|---------------------|-------------|------|
| `[新建]` / `[新增]` | `add-button` | toolbar |
| `[批量删除]` | `batch-delete-button` | toolbar |
| `[导出]` | 无标准 ID | view.xml 没有标准导出按钮；差距项 |
| `[批量审核]` | 无标准 ID | view.xml 没有标准批量审核按钮；差距项 |
| `[编辑]` | `row-update-button` | row |
| `[查看]` | `row-view-button` | row |
| `[删除]` | `row-delete-button` | row |
| `[提交]` / `[提交审核]` / `[提交审批]` | `row-submit-button` | row |
| `[撤回]` | `row-withdraw-approval-button` | row |
| `[审核]` / `[审核通过]` | `row-approve-button` | row |
| `[驳回]` | `row-reject-button` | row |
| `[反审核]` | `row-reverse-approve-button` | row |
| `[作废]` / `[取消]` | `row-cancel-button` | row |
| `[打印]` | 无标准 ID | view.xml 没有标准打印按钮；视为 navigational |
| `[操作历史]` / `[▸关联单据]` | 无标准 ID | 属导航/信息面板，非 action 按钮；不记为期望 |
| `[更多]` | `row-more-button` | row |

**重要规则**：
- ui-patterns.md 中的 `[保存草稿]` 通常指 edit 页内置提交行为，不独立对应 action 按钮 ID。若 view.xml 无此独立按钮，不计为差距。
- `[打印]`、`[导出]`、`[批量审核]` 如果 ui-patterns.md 明确描述但在 view.xml 不存在，记为 **info** 级差距（属于可增强点，非阻塞）。
- `[操作历史]`、`[关联单据]` 属于 UI 布局层面的链接/导航，不属于 action 按钮，不列为期望。

## 3. 实体分类标准（正式定义）

基于 Phase 2 审计结果，每个实体归类为：

| 分类 | 条件 | 含义 |
|------|------|------|
| 分类 | 条件 | 含义 |
|------|------|------|
| **CRUD** | toolbar = {add-button, batch-delete-button} + row = {row-view-button, row-update-button, row-delete-button}，无域专用按钮 | 标准 CRUD（纯 codegen） |
| **CRUD+WF** | CRUD 基线 + row 含 codegen 自动生成的标准审批按钮（submit/withdraw/approve/reject/reverse-approve），可能有 cancel | 审批流单据（DIRECT 或 WORKFLOW 模式） |
| **CRUD+Custom** | CRUD 基线 + 域专用按钮（不可 codegen 自动生成的按钮，如 screening/interview/issue-materials/pass/fail） | 有扩展动作，需手动实现 |
| **Custom** | 缺失 CRUD 基线项，或包含无法归类的非标准按钮组织 | 非标准，需关注 |
| **Other** | 无 main crud page，或页面无任何 action 定义 | 特殊页面 |

> **注意**：`CRUD+WF` 不依赖 view.xml 是否实际存在 submit/approve/reject 按钮——它们可由 codegen 自动生成或由 nop-wf 引擎提供。`CRUD+Custom` 的域专用按钮才是审计关注的核心目标。

## 4. 严重级别定义

| 级别 | 含义 | 示例 |
|------|------|------|
| **blocker** | ui-patterns.md 明确列举的域专用按钮（非 codegen 审批按钮）缺失，且阻断核心业务操作 | 工单没有 issue-materials/receive-finished 按钮 |
| **major** | CRUD 基线按钮缺失（且实体是业务主实体），或 state-machine 关键状态迁移按钮缺失 | 物料管理没有 add-button；项目缺少 start/complete |
| **minor** | 非核心按钮缺失，或 CRUD 基线缺失但实体是次要/配置类实体 | 字典管理没有 batch-delete |
| **info** | ui-patterns.md 提到但非关键的功能增强 | 没有导出/打印按钮 |
| **clean** | 无差距或差距已合理被 classify 字段说明 | — |

> **重要**：`row-submit-button` / `row-approve-button` / `row-reject-button` 等标准审批按钮由 codegen 自动生成（DIRECT 模式）或 nop-wf 引擎提供（WORKFLOW 模式），**不应作为差距触发条件**。只评估域专用按钮、`row-cancel-button` 和 CRUD 基线按钮。

## 5. 报告模板

每个 `<domain>.md` 使用以下结构：

```markdown
# <Domain> 视图按钮需求覆盖分析

## 分析范围
<列出本域所有实体及分类>

## 期望按钮推导依据
<引用 ui-patterns.md / state-machine.md / CRUD 基线的关键条款>

## 逐实体分析

### <实体名> — <分类>
- **期望按钮**：<列表>
- **实际按钮**：<列表>
- **差距**：
  - <按钮ID>: <missing/extra> (<severity>) — <说明>
- **判定**：<clean / blocker / major / minor>

...

## 摘要

| 分类 | 实体 | 差距数 | 最高严重级 | 备注 |
|------|------|--------|-----------|------|
| ... | ... | ... | ... | ... |

### 总评
- 总实体数：<N>
- 无差距实体：<N>（<percentage>%）
- Blocker 差距：<N>
- Major 差距：<N>
- Minor/Info 差距：<N>
```

## 6. 无 ui-patterns.md 域的处理规则（Group B）

对于 nop-auth、nop-wf、nop-sys、nop-report、notify 五个域：

- 预期按钮 = CRUD 基线 + 平台约定。
- 如果实体有 state-machine 或审批行为，按 `domain-design-guidelines.md` 加上工作流按钮。
- 由于无业务 ui-patterns.md，列出的期望唯一来源是 CRUD 基线。报告应标注"无 ui-patterns.md，期望仅 CRUD 基线"。
- notify 域含跨域通知派发，期望 include 收件人管理、通知模板管理等专用按钮（如 `row-preview-button`、`row-send-button`），需人工判断。

## 7. 审计规则

1. **子代理独立性**：每个域审计必须使用独立的子代理会话（新 task），不重用其他域的审计上下文。
2. **引用原文**：每个差距记录必须引用 ui-patterns.md 原文或 CRUD 基线规则出处。
3. **实体排除**：若某实体 view.xml 仅含 `main` 且 `main` 无 toolbar/row（空 CRUD），标记为 Other 并在报告中注明"空页面"。
4. **不计为差距的情况**：对 tree 类实体（分类、科目表）允许无 CRUD toolbar，因为树形操作在节点级。

## 8. 目录结构

```
docs/analysis/view-button-coverage/
├── METHODOLOGY.md           # 本文档
├── README.md                # Phase 3 全局汇总
├── master-data.md
├── purchase.md
├── sales.md
├── inventory.md
├── finance.md
├── assets.md
├── manufacturing.md
├── projects.md
├── quality.md
├── maintenance.md
├── crm.md
├── customer-service.md
├── human-resource.md
├── aps.md
├── logistics.md
├── b2b.md
├── contract.md
├── drp.md
├── nop-auth.md
├── nop-wf.md
├── nop-sys.md
├── nop-report.md
└── notify.md
```

## 9. 已知局限与工具注意事项

### 9.1 自动采集工具 `tools/analyze-view-buttons.mjs` 的已知缺陷

1. **分类 bug — CRUD+WF 实体被误标为 CRUD**：工具的 `classifyMain()` 函数优先匹配 `isCrud`（当 extraRow 和 missingRow 均为空时），在同时包含 CRUD 基线和审批按钮的实体上返回 CRUD 而非 CRUD+WF。各域审计报告已经独立重新分类，但 `_tmp/view-buttons/` 中的源数据分类列不可直接用于计数。
2. **action 去重不彻底**：对无 `id` 属性的 `<action>` 元素，去重逻辑退化为空字符串，导致少数实体出现重复行。
3. **page 类型检测静态**：只识别 `<crud>`、`<simple>`、`<picker>` 三种 pages 类型，不识别可能的自定义 page 类型。

### 9.2 域报告间"Clean"口径差异

各独立子代理对 `info` 级别差距的处理存在分歧：部分报告将 info 差距实体计入 clean（因为 info 非 blocker/major/minor），部分报告排除。README.md 的 clean 计数已使用 `~` 标注为近似值。如需精确对比，请按各域报告摘要表逐项确认。

### 9.3 未覆盖的检查项

- 不检查 edit 页面 `<form>` 内的 action buttons（非 toolbar/rowActions）。
- 不检查 `visibleOn` 条件 —— 按钮存在但条件配置错误不计为差距。
- 不检查 nop-wf 审批流的 `step` 级动作绑定（属 wf `.xwf` 文件范畴）。
- 不检查按钮的国际化和 label 文本准确性。
- 不检查 action-auth 权限配置。
- 不检查 BizModel 端 action 方法是否存在（仅检查 view.xml 声明）。
