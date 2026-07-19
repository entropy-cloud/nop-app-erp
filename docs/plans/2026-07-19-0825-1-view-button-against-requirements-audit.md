# 2026-07-19-0825-1-view-button-against-requirements-audit 视图按钮与需求覆盖审计

> Plan Status: completed
> Last Reviewed: 2026-07-19
> Source: 前端按钮/动作（view.xml）对业务需求的覆盖完整性验证
> Related: `_tmp/view-buttons/SUMMARY.md`（按域按钮现状快照）
> Audit: required

## Current Baseline

- 已完成 394 个实体视图的按钮自动采集（`tools/analyze-view-buttons.mjs` → `_tmp/view-buttons/` 23 域分文件 + SUMMARY.md）。
- 分类结果：CRUD 375（95.2%）、Custom 17（4.3%）、Other 2（0.5%）。
- 各域 `docs/design/<domain>/ui-patterns.md` 及 `docs/design/domain-design-guidelines.md` 已定义业务期望的页面按钮与交互。
- **18** 域有独立 ui-patterns.md（master-data/purchase/sales/inventory/finance/assets/manufacturing/projects/quality/maintenance/crm/cs/hr/aps/logistics/b2b/contract/drp）；nop-auth/nop-wf/nop-sys/nop-report + notify 共 5 域无 ui-patterns.md。
- 未完成：对每个域的 view.xml 实际按钮声明与 ui-patterns.md + domain-design-guidelines + roles-and-permissions.md 的**逐域人工比对**。
- 未完成：Custom 和 Other 分类的 19 个实体是否存在合理设计理由，还是遗漏了标准 CRUD 按钮。
- 未完成：各域业务单据（采购订单、销售订单、工单等）是否已正确声明 submit/approve/reject/cancel 等审批流按钮。

## Goals

- 对每域创建一个独立分析报告（`docs/analysis/view-button-coverage/<domain>.md`），包含：
  1. 本域期望的页面按钮清单（从 ui-patterns.md + CRUD 基线 + 审批/业务动作推导）
  2. 实际按钮清单（从 `_tmp/view-buttons/<domain>.md` 提取）
  3. 差距分析 — 缺失项与多余项
  4. 每个差距的严重级别（blocker / major / minor / info）与修复建议
- 创建一个总目录 `docs/analysis/view-button-coverage/README.md`，汇总各域覆盖率和整体结论。
- 审计过程中发现的任何真实页面缺陷应记录在 `docs/analysis/view-button-coverage/` 报告中并标记 severity。

## Non-Goals

- 不修改代码或 view.xml — 仅审计和记录。
- 不深入 BizModel 端动作是否已接线（超出 view 层范围）。
- 不审计按钮的权限约束（action-auth），仅审计按钮存在性。
- 不审计看板页面（`*Dashboard*.view.xml`）按钮 — 看板已在另一路线中验证。

## Task Route

- Type: `verification or audit work`
- Owner Docs:
  - `docs/design/domain-design-guidelines.md`（CRUD 基线 / 审批动作定义）
  - `docs/design/roles-and-permissions.md`（角色→动作映射）
  - `docs/design/<domain>/ui-patterns.md`（各域 UI 设计）
  - `docs/design/<domain>/README.md` + `state-machine.md`（业务状态驱动动作）
  - `_tmp/view-buttons/`（按钮现状）
  - `docs/analysis/README.md`（输出格式惯例）
- Skill Selection Basis: 本任务是纯审计验证，不涉及代码修改。需加载 `multi-dimensional-audit-prompt.md` 的技能方法，因为要跨多维度（CRUD 基线、业务状态机、各域 UI 设计）比对。同时适合 `document-audit-prompt.md` 来确保输出审计报告的一致性和完整性。

## Infrastructure And Config Prereqs

- 无基础设施依赖。分析读取文件，不运行应用或数据库。
- 独立的子代理审计会话（每次调用一个域，避免互相干扰）。

## Execution Plan

### Phase 1 — 确定性规则定义

Status: completed
Targets: `docs/analysis/view-button-coverage/`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Decision`

- [x] Decision: 定义每域期望按钮清单的推导规则和优先级体系，写入 `docs/analysis/view-button-coverage/METHODOLOGY.md`。
  - 优先级：ui-patterns.md 声明的按钮 > state-machine.md 隐含的状态驱动动作 > domain-design-guidelines CRUD 基线 > roles-and-permissions 角色操作。
  - ui-patterns.md 用业务语言（"[保存草稿]"），view.xml 用按钮 ID（`row-submit-button`）。METHODOLOGY.md 必须定义可复用的 prose→button-id 翻译字典，确保各子代理审计结果可对账。
  - 分类正式定义：
    - CRUD = toolbar: `add-button`, `batch-delete-button` + row: `row-view-button`, `row-update-button`, `row-delete-button`。
    - CRUD+WF = CRUD + row 含 `row-submit-button` / `row-approve-button` / `row-reject-button` / `row-withdraw-approval-button` / `row-reverse-approve-button` / `row-cancel-button`。
    - Custom = 含有任何其他业务专用按钮的实体。
  - 缺失门控（什么算 blocker）：若 ui-patterns.md 明确列出的按钮在 view.xml 中不存在 → blocker。若 CRUD 基线按钮缺失（且实体为业务主实体）→ major。
  - 技能: `multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] METHODOLOGY.md 写入 `docs/analysis/view-button-coverage/METHODOLOGY.md`，被后续所有域审计引用。

### Phase 2 — 按域独立审计（23 个子任务）

Status: completed
Targets: 每个域一个独立子代理会话
Skill: `document-audit-prompt.md`（审计报告格式）+ `multi-dimensional-audit-prompt.md`（跨维度比对）

- Item Types: `Add | Fix`
- Prereqs: Phase 1

对以下 23 域逐一执行独立子代理审计。

**分组 A — 有 ui-patterns.md（18 域）：**
1. master-data（md）
2. purchase（pur）
3. sales（sal）
4. inventory（inv）
5. finance（fin）
6. assets（ast）
7. manufacturing（mfg）
8. projects（prj）
9. quality（qa）
10. maintenance（mnt）
11. crm
12. customer-service（cs）
13. human-resource（hr）
14. aps
15. logistics（log）
16. b2b
17. contract（ct）
18. drp

**分组 B — 无 ui-patterns.md，使用 CRUD 基线 + 系统语义：**
19. nop-auth
20. nop-wf
21. nop-sys
22. nop-report
23. notify

每个子代理的任务模板：

```
审计域: <domain>
参考文件:
  - docs/design/<domain>/ui-patterns.md（如存在）
  - docs/design/<domain>/state-machine.md（如存在）
  - docs/design/<domain>/README.md
  - docs/design/domain-design-guidelines.md（CRUD 基线 + 审批动作）
  - docs/design/roles-and-permissions.md（角色操作定义）
  - docs/analysis/view-button-coverage/METHODOLOGY.md（按钮推导规则）
  - _tmp/view-buttons/<domain>.md（按钮现状）
  - 实际 view.xml 文件路径（从 _dump/nop-app/erp/<domain>/pages/ 或 _dump/nop-app/nop/<domain>/pages/）

任务:
1. 根据参考文件推导此域所有实体的期望按钮清单。
2. 与 _tmp/view-buttons/<domain>.md 中的实际按钮清单逐实体比对。
3. 记录每个差距（missing / extra）并标注 severity。
4. 特别注意 Custom 和 Other 分类的实体 — 验证其合理性。
5. 写报告到 docs/analysis/view-button-coverage/<domain>.md。
```

- [x] 域 nop-auth 审计
- [x] 域 nop-wf 审计
- [x] 域 nop-sys 审计
- [x] 域 nop-report 审计
- [x] 域 notify 审计
- [x] 域 master-data（md）审计
- [x] 域 purchase（pur）审计
- [x] 域 sales（sal）审计
- [x] 域 inventory（inv）审计
- [x] 域 finance（fin）审计
- [x] 域 assets（ast）审计
- [x] 域 manufacturing（mfg）审计
- [x] 域 projects（prj）审计
- [x] 域 quality（qa）审计
- [x] 域 maintenance（mnt）审计
- [x] 域 crm 审计
- [x] 域 customer-service（cs）审计
- [x] 域 human-resource（hr）审计
- [x] 域 aps 审计
- [x] 域 logistics（log）审计
- [x] 域 b2b 审计
- [x] 域 contract（ct）审计
- [x] 域 drp 审计

Exit Criteria:

- [x] 上述 23 项全部完成
- [x] 每个 `<domain>.md` 都包含：期望清单、实际清单、差距表、severity、修复建议

### Phase 3 — 全局汇总

Status: completed
Targets: `docs/analysis/view-button-coverage/README.md`
Skill: `document-audit-prompt.md`

- Item Types: `Add`

- [x] 汇总各域的覆盖率（CRUD 基线满足率 / 业务动作覆盖率），写入 README.md。
  - 附带按 domain 排序的摘要表：domain, entity count, CRUD coverage %, button gaps found（blocker/major/minor）。
  - 整体结论和建议。
  - 技能: `document-audit-prompt.md`

Exit Criteria:

- [x] `docs/analysis/view-button-coverage/README.md` 写入并链接所有域报告
- [x] `25` 个生成文件全部到位（METHODOLOGY + 23 域报告 + README）

## Draft Review Record

- Independent draft review iteration 1: **accept** (`ses_088358c5bffeFvo3xrBGh7gA9P`) — 两个小修正已应用（ui-patterns 计数 21→18；METHODOLOGY 需定义 prose→button-id 翻译）

## Closure Gates

- [x] 范围内行为完成（23 域审计 + 全局汇总）
- [x] 相关文档对齐（METHODOLOGY.md + 23 域报告 + README.md）
- [x] 已运行验证（人工抽查 2-3 份报告与原始 view.xml 对比 — 审计子代理已做）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（本计划暂无 defer 项目）

## Closure

Status Note: All 3 phases complete. 23 domain reports + METHODOLOGY + README + log written. Closure audit by independent sub-agent passes.

Closure Audit Evidence:

- Auditor / Agent: independent sub-agent (`ses_088244377ffeT0hLqlSUCZGBN4`)
- Evidence: closure audit verdict "accept" — all 8 gates pass, 2 minor text issues fixed post-audit

Follow-up:

- 25 blocker-gap entities should enter `docs/backlog/` for fix planning
- Systematic `row-cancel-button` missing pattern across 7 domains needs a cross-plan
- Read-only entities (StockLedger, Balance, Batch, SerialNumber, OpLog, Session) should have CRUD buttons hidden
