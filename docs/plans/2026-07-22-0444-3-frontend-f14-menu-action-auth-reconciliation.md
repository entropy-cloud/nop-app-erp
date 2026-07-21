# 2026-07-22-0444-3 frontend F14 — Menu action-auth Reconciliation

> Plan Status: active
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F14 — Menu action-auth 对账（P3）
> Related: `docs/plans/2026-07-22-0444-2-frontend-f11-domain-batch-operations.md`（同批 plan 2，先执行）
> Audit: required

## Current Baseline

- **F1-F10 全部 done**；F11 独立 plan 推进中（plan 2）。F14（菜单对账）为 `todo`，P3，属跨域前端一致性收尾项。
- **action-auth.xml 结构现状**：19 域各有 2 文件 — `_erp-<domain>.action-auth.xml`（codegen 生成）+ `erp-<domain>.action-auth.xml`（手写 delta），位于 `module-<domain>/erp-<domain>-web/.../_vfs/erp/<domain>/auth/`。共 38 文件。
- **已知已修复域（基线非全量缺口）**：早期核心域（如 purchase）的 `erp-pur.action-auth.xml` 已有业务流程 orderNo（100/110/120/200…）+ 资源 `displayName` 含 `i18n-en`。故 F14 缺口**非全域性**，主要集中在后续扩展域（crm/cs/hr/aps/logistics/b2b/contract/drp）及各深化计划新增实体菜单（A1/A2/A3/B1/C2 等新实体菜单分组/orderNo 跨域不一致）。**实际缺口范围需 Phase 0 审计确定。**
- **菜单三类已知问题**（roadmap §F14）：
  1. **可达性**：部分业务实体页面可能无菜单项（孤儿页面）— 需审计确认。
  2. **orderNo 排序**：部分域沿用 codegen 字母序，未按业务流程排列（如采购 RFQ→Quotation→PO→Receive→Invoice→Payment 流程顺序）。
  3. **分组命名一致性**：新增实体菜单分组命名（如"跨境贸易"/"公司间"/"多年度"等）跨域风格不统一。
- **roadmap 域计数差异**：roadmap §F14 文本写"18 域"，实际仓库为 19 域（含 notify 跨域子系统）。本计划以仓库实际 19 域为准。
- **Non-Goal 边界**：action-auth.xml 的资源 `displayName` 已含 `i18n-en`（codegen 生成）；F14 不涉及 displayName i18n（那是 F15 范围 — view.xml/page.yaml 手写层 label i18n）。F14 聚焦菜单结构（可达性 + orderNo + 分组）。
- **看板/报表菜单**已由既有计划覆盖；本项聚焦 CRUD 业务页面菜单。

## Goals

- 19 域 action-auth.xml 菜单对账完成：所有业务实体页面菜单可达、`orderNo` 按业务流程排列（非 codegen 字母序）、菜单分组命名跨域一致。
- 关闭 frontend-ui-roadmap F14 退出标准。

## Non-Goals

- **F15 i18n 标签补充**（view.xml/page.yaml 手写层 `label=` 的 `i18n-en`）— 独立结果表面，roadmap 显式声明 `F14 -.->|独立| F15`，归独立 successor plan。
- 权限颗粒度审计（roadmap 明确 Non-Goal — action-auth.xml 除菜单可达性外的角色/资源权限映射）。
- 新增页面或交互（F12/F13/F16 范围）。
- 看板/报表菜单（已由既有计划覆盖）。
- 像素级视觉回归。

## Task Route

- Type: `implementation-only change`（前端 action-auth.xml 手写 delta 调整）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图策略）、各域 `ui-patterns.md`（业务流程顺序参考）
- Skill Selection Basis: 前端 action-auth.xml 定制 → `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 0 - 菜单缺口审计 + 分组命名约定裁决

Status: planned
Targets: 19 域 `erp-<domain>.action-auth.xml`（审计对象）
Skill: nop-frontend-dev

- Item Types: `Decision | Explore`
- Prereqs: F1-F10 done（实体页面已稳定）

- [ ] `Explore`: 19 域菜单缺口全量审计 — 逐域扫描 action-auth.xml，产出三份清单：(1) 孤儿页面清单（有 view.xml 无菜单项）；(2) orderNo 非业务流程序域清单；(3) 分组命名不一致清单。标注哪些域已修复（如 purchase）无需变更
  - Skill: nop-frontend-dev
- [ ] `Decision`: 分组命名约定裁决 — 选定跨域统一分组命名约定（候选：按业务语义"主数据/单据/配置/查询/查询分析"或按现有先行域 purchase/sales 的实际分组风格），记录选择 + 替代方案 + 残留风险
  - Skill: nop-frontend-dev

Exit Criteria:

> 本阶段产出缺口清单 + 命名约定裁决，解除后续修复的范围模糊阻塞。

- [ ] 三份缺口清单产出（孤儿页面 / orderNo / 分组命名），标注已修复域
- [ ] 分组命名约定裁决已记录（含选择、替代方案、残留风险）

### Phase 1 - 菜单可达性 + orderNo + 分组一致性修复

Status: planned
Targets: Phase 0 审计标记需修复的 `erp-<domain>.action-auth.xml` 文件
Skill: nop-frontend-dev

- Item Types: `Fix | Add`
- Item Type Note: 本阶段 `Fix-heavy`（修复可达性缺口 + orderNo 重排 + 分组命名统一）。
- Prereqs: Phase 0 缺口清单 + 命名约定裁决

- [ ] `Fix`: 孤儿页面菜单补全 — 为 Phase 0 审计标记的孤儿页面补全 action-auth.xml 菜单项（含正确分组 + orderNo）
  - Skill: nop-frontend-dev
- [ ] `Fix`: orderNo 业务流程重排 — 按 Phase 0 命名约定 + 各域 `ui-patterns.md` 业务流顺序，重排需修复域的菜单 orderNo（如采购 RFQ→Quotation→PO→Receive→Invoice→Payment→Return）
  - Skill: nop-frontend-dev
- [ ] `Fix | Add`: 分组命名统一 — 按 Phase 0 裁决的命名约定，统一跨域分组 displayName（含新增实体菜单如"跨境贸易"/"公司间"等的归类）
  - Skill: nop-frontend-dev

Exit Criteria:

- [ ] Phase 0 标记的全部缺口已修复（0 孤儿页面 + orderNo 按业务流程 + 分组命名一致）

### Phase 2 - 回归验证 + roadmap 同步

Status: planned
Targets: `docs/backlog/frontend-ui-roadmap.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 修复完成

- [ ] `Proof`: 菜单对账回归 — 启动 app 验证菜单树结构：所有业务实体可达 + orderNo 排序正确 + 分组命名一致；以 visual.spec.ts 或手动审计记录佐证
  - Skill: none
- [ ] `Add`: roadmap 同步 — F14 状态 `todo → done` + 退出标准 F14 项勾选 + 落地证据（含 Phase 0 审计清单摘要）
  - Skill: none

Exit Criteria:

- [ ] 菜单对账回归通过（可达性 + 排序 + 分组）
- [ ] F14 roadmap 退出标准勾选

## Draft Review Record

- Independent draft review iteration 1: needs-revision（独立子代理 ses_079909671）— 原草案将 F14+F15 捆绑，违反 Rule 4（两个独立结果表面）。修订：拆分为 F14-only plan，F15 归独立 successor。
- Independent draft review iteration 2: accept（独立子代理 ses_0798cac88）— Rule 4 捆绑阻塞已解决（F14-only 单结果表面，F15 显式 deferred successor 含触发条件）；无阻塞项。非阻塞建议：分组命名统一项已从 `Add` 调整为 `Fix | Add`（兼具修复不一致 + 统一标签）。

## Closure Gates

> 完整仓库验证在此处。

- [ ] 范围内行为完成（菜单可达性 + orderNo + 分组一致性 + roadmap 同步）
- [ ] 相关文档对齐
- [ ] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `npx playwright test` 回归全绿 + 菜单对账手动/visual 验证
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### F15 i18n 标签补充

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F15（view.xml/page.yaml 手写层 label i18n-en 补充 + CI check）是独立结果表面，roadmap 显式声明 `F14 -.->|独立| F15`；归独立 successor plan
- Successor Required: yes（触发：本 F14 plan 完成 + 启动 F15 独立 plan 轮次）

### 权限颗粒度审计（action-auth.xml 资源/角色映射）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 明确 Non-Goal — 权限颗粒度为独立审计项
- Successor Required: yes（触发：安全合规审计需求 + security owner doc 授权）

## Closure

Status Note: _待执行后填写_

Closure Audit Evidence:

- Auditor / Agent: _待独立结束审计_
- Evidence: _待记录_

Follow-up:

- F15 i18n 标签补充（触发条件见上）
- 权限颗粒度审计（触发条件见上）
