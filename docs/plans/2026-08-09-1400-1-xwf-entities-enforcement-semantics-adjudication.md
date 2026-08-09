# 2026-08-09-1400-1 xwf-entities-enforcement-semantics-adjudication

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.6
> Related: `docs/plans/2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md`（xwf 浏览器层 NOT FEASIBLE 权威裁决）；mission `permissions-enforcement`；P1.3（粒度裁决，已 done）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.6

## Current Baseline

4 个 `useWorkflow="true"` 实体（实测 ORM `useWorkflow="true"` 标记）的 xwf 审批轴在浏览器层 E2E **不可达**（`docs/plans/2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md` 权威裁决 NOT FEASIBLE）；其 DIRECT 三轴审批状态机（`approveStatus` DIRECT 路径）浏览器层可达且全绿。`docs/design/roles-and-permissions.md` §浏览器层审批路径已知限制 已记录此事实与 4 实体影响范围——**但该 owner doc 表格将 Payment/Receipt 误标为 finance 域 `ErpFinPayment`/`ErpFinReceipt`，实测真值为 purchase 域 `ErpPurPayment` 与 sales 域 `ErpSalReceipt`（ORM `module-purchase/.../app-erp-purchase.orm.xml`、`module-sales/.../app-erp-sales.orm.xml` 的 `useWorkflow="true"` 表 `erp_pur_payment`/`erp_sal_receipt`），无 finance xwf 实体。本计划 Phase 3 顺带订正该 owner doc 漂移。**

**4 实体 per-action FNPT 声明现状**（实测生成文件 `_erp-*.action-auth.xml` + delta `erp-*.action-auth.xml`）：

| 域 | 实体 | xwf 文件 | per-action `:approve` FNPT 声明状态 | roles 种子 |
|----|------|---------|--------------------------------------|-----------|
| human-resource | ErpHrSalary | `salary-approval/v1.xwf`（三级链） | **已声明**（delta `erp-hr.action-auth.xml` `:approve`/`:markPaid`） | 薪酬审批人 |
| assets | ErpAstDisposal | `disposal-approval/v1.xwf` | **未声明**（生成 `_erp-ast.action-auth.xml` 仅 `:query`/`:mutation`，delta 未补）→ 归 P1.4b（sibling `2026-08-09-1400-3`）声明 | — |
| purchase | ErpPurPayment | `payment-approval/v1.xwf` | **未声明**（生成 `_erp-pur.action-auth.xml` 仅 `:query`/`:mutation`，delta 未补）→ 归 P1.4a（sibling `2026-08-09-1400-2`）声明 | — |
| sales | ErpSalReceipt | `receipt-approval/v1.xwf` | **未声明**（生成 `_erp-sal.action-auth.xml` 仅 `:query`/`:mutation`，delta 未补）→ 归 P1.4a（sibling `2026-08-09-1400-2`）声明 | — |

**已裁决的上游**：P1.3 确认映射收敛粒度 = 角色×SUBM + 敏感动作 per-action FNPT + 兜底策略（管理员=平台 admin 兜底 + 业务角色显式种子，双命名空间分离）。P1.3 明确：approve 属敏感动作，**应作为独立 per-action FNPT 点脱离泛化 `mutation` 桶**。

**声明归属已由 sibling 计划承接**：本批 sibling `2026-08-09-1400-2`（P1.4a）覆盖 ErpPurPayment/ErpSalReceipt approve 声明；sibling `2026-08-09-1400-3`（P1.4b）覆盖 ErpAstDisposal approve 声明；ErpHrSalary 已声明。故 4 实体 `:approve` FNPT 的**声明落地**不在本计划——本计划仅裁决 enforcement 绑定**规则**与 DIRECT 三轴**测试策略**（保持 doc-only，Skill none，与 roadmap P1.6 一致）。

**缺口**：P1.6 未裁决前，P1.5a（静态种子）不知道这 4 实体 approve 权限点绑定规则；P2.1（配置 profile 化）/E1.x（高危翻转）不知道这 4 实体 enforcement 测试策略（DIRECT 三轴可测、xwf 轴后端单测覆盖如何与 enforcement 负向测试衔接）。

## Goals

- **裁决权限点绑定规则**：为 4 实体 approve 动作确定 enforcement 绑定规则——approve 经 `:approve` per-action FNPT（声明随 sibling P1.4a/P1.4b 落地，ErpHrSalary 已声明），并确认 4 实体 `:approve` 声明归属（P1.4a/P1.4b/已声明）。
- **裁决 DIRECT 三轴浏览器层测试策略**：为 enforcement 负向测试确定这 4 实体的可测路径——DIRECT 三轴 approve 为浏览器层 E2E 负向断言主体；xwf 审批轴保持后端单测覆盖（不可达，非负向 E2E 缺陷）。为 P2.1（配置翻启节奏）与 E1.x（高危翻转）提供冻结输入。
- **订正 owner doc 漂移 + 落盘裁决**：订正 `roles-and-permissions.md` §浏览器层审批路径已知限制 表格的 Payment/Receipt 域/实体误标（finance→purchase/sales）；将权限点绑定规则 + 测试策略写入该节 enforcement 语义子节。

## Non-Goals

- **不翻转 enforcement 开关**（归 P2.4/E1.x，本计划仅裁决语义）。
- **不改 xwf 浏览器层可达性**（2330-1 裁决 NOT FEASIBLE，解除条件属平台 successor）。
- **不改 DIRECT 三轴状态机业务逻辑**（仅读作裁决输入）。
- **不落地 4 实体 `:approve` FNPT 声明**（声明归 sibling P1.4a/P1.4b；ErpHrSalary 已声明。本计划仅裁决绑定规则 + 测试策略，保持 doc-only / Skill none）。
- **不裁决 pur/sal 通用 submitForApproval FNPT 处理**（submit 非 P1.6 4 实体范围；pur/sal submit 处理由 P1.4a 内部 Decision 承接）。
- **不做生产环境翻转决策**（successor）。

## Task Route

- Type: `requirement clarification`（xwf 4 实体 enforcement 语义裁决 + owner doc 回写，产决策为主）
- Owner Docs: `docs/design/roles-and-permissions.md` §浏览器层审批路径已知限制、§action-level 声明层
- Skill Selection Basis: `none` —— 纯 enforcement 语义/测试策略裁决与文档回写，不动状态机/xwf 业务逻辑，不落地 `:approve` 声明（声明归 sibling P1.4a/P1.4b）；与 roadmap 表格 P1.6 Skill 列一致。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（只读 xwf/DIRECT 状态机/生成 action-auth.xml 作裁决输入，不改运行时）。

## Execution Plan

### Phase 1 - 权限点绑定规则与声明归属确认

Status: completed
Targets: `docs/design/roles-and-permissions.md` §浏览器层审批路径已知限制、§action-level 声明层
Skill: none

- Item Types: `Decision` / `Proof`
- Prereqs: P1.3（done，提供收敛粒度 + 兜底策略裁决）；2330-1（done，提供 xwf 不可达裁决）

- [x] **Proof**：核实并记录 4 实体 per-action 声明现状（Current Baseline 表）——ErpHrSalary 已声明 `:approve`/`:markPaid`→薪酬审批人（delta `erp-hr.action-auth.xml`）；ErpAstDisposal/ErpPurPayment/ErpSalReceipt 未声明（生成 `_erp-{ast,pur,sal}.action-auth.xml` 仅 `:query`/`:mutation`，delta 未补）。引用生成文件 + delta 文件行号作证据。**顺带核实 owner doc §浏览器层表格的域/实体误标**（finance→purchase/sales）。
  - Skill: none
  - 证据落地：owner doc §浏览器层审批路径已知限制「域/实体名订正注记」+ §enforcement 语义裁决「4 实体 `:approve` 声明归属」表（含实测行号：`erp-hr.action-auth.xml` L101-108、`_erp-pur.action-auth.xml` L73-79、`_erp-sal.action-auth.xml` L185-191、`_erp-ast.action-auth.xml` L115-121；ORM `useWorkflow="true"` 行号：purchase orm L923 / sales orm L735 / hr orm L722 / assets orm L578；finance orm 无 `useWorkflow`）。
- [x] **Decision**：裁决 approve 动作 enforcement 绑定规则。考虑的替代方案：(a) approve 一律经独立 `:approve` per-action FNPT（与 P1.3 收敛粒度一致，敏感动作脱离 `mutation` 桶）；(b) 未声明者暂入 `:mutation` 桶（违背 P1.3，拒绝）；(c) 混合（已声明者经 `:approve`，未声明者先声明再绑定）。选定方案 + 残留风险记录。
  - Skill: none
  - 裁决落地：owner doc §enforcement 语义裁决「A. 权限点绑定规则」——采纳 (a)；(b) 拒绝；(c) 拒绝为终态、采纳为过渡（收敛于 (a)，过渡期由 admin 兜底 + enforcement 翻启门控兜住）。残留风险记录于同节。
- [x] **Decision**：确认 4 实体 `:approve` 声明归属——ErpHrSalary 已声明；ErpPurPayment/ErpSalReceipt 归 sibling P1.4a（`2026-08-09-1400-2`）；ErpAstDisposal 归 sibling P1.4b（`2026-08-09-1400-3`）。声明落地不在本计划（保持 doc-only）；本计划仅记录归属，确保 P1.5a 种子时 4 实体 approve 点均已存在。
  - Skill: none
  - 归属落地：owner doc §enforcement 语义裁决「4 实体 `:approve` 声明归属」表——ErpHrSalary（已声明，L101-108）/ ErpPurPayment（P1.4a，sibling active）/ ErpSalReceipt（P1.4a，sibling active）/ ErpAstDisposal（P1.4b，sibling active）。两 sibling 计划 Plan Status 实测均为 `active`。

Exit Criteria:

- [x] 权限点绑定规则（approve → per-action `:approve`）+ 4 实体声明归属确认落地，含替代方案与残留风险，可被 P1.5a / sibling P1.4a/P1.4b 直接消费。

### Phase 2 - DIRECT 三轴浏览器层测试策略裁决

Status: completed
Targets: `docs/design/roles-and-permissions.md` §浏览器层审批路径已知限制
Skill: none

- Item Types: `Decision`
- Prereqs: Phase 1

- [x] **Decision**：裁决 4 实体 enforcement 负向测试路径。考虑的替代方案：(a) DIRECT 三轴 approve 为浏览器层 E2E 负向断言主体（非 admin/未授权角色经 DIRECT 路径 approve 被拒），xwf 轴保持后端单测覆盖（不可达非缺陷）；(b) 仅后端单测覆盖 approve enforcement，浏览器层不测（拒绝：削弱 enforcement 真拒绝证明）；(c) 强行修复 xwf 可达性（2330-1 NOT FEASIBLE，拒绝）。选定方案 + 对 P2.1（翻启节奏）/E1.x（高危翻转）的影响 + 残留风险记录。
  - Skill: none
  - 裁决落地：owner doc §enforcement 语义裁决「B. DIRECT 三轴浏览器层负向测试策略」——采纳 (a)；(b)/(c) 拒绝。对 P2.1（dev/test profile 可预置 enable-action-auth，DIRECT 三轴浏览器层可覆盖，xwf 轴仅后端单测层验证须登记覆盖边界）/ E1.x（4 实体 approve 翻启凭 DIRECT 轴 E2E 负向 + xwf 后端单测证据推进，DIRECT 轴无浏览器层缺口）冻结输入已记录。残留风险（xwf 多级链无浏览器层 E2E 负向证明，按 2330-1 可接受）记录于同节。

Exit Criteria:

- [x] DIRECT 三轴测试策略裁决落地，含替代方案与残留风险，可作为 P2.1 / E1.x 冻结输入。

### Phase 3 - owner doc 回写与漂移订正

Status: completed
Targets: `docs/design/roles-and-permissions.md` §浏览器层审批路径已知限制、§action-level 声明层
Skill: none

- Item Types: `Add` / `Fix`
- Prereqs: Phase 1 + Phase 2

- [x] **Fix**：订正 §浏览器层审批路径已知限制 表格的 Payment/Receipt 域/实体误标——finance `ErpFinPayment`/`ErpFinReceipt` → purchase `ErpPurPayment` / sales `ErpSalReceipt`（与 ORM `useWorkflow="true"` 真相一致；4 实体 = ErpHrSalary/ErpAstDisposal/ErpPurPayment/ErpSalReceipt，无 finance xwf 实体）。
  - Skill: none
  - 落地：`docs/design/roles-and-permissions.md` L251-256 表格两行订正（finance ErpFinPayment→purchase ErpPurPayment；finance ErpFinReceipt→sales ErpSalReceipt）+ L258 新增「域/实体名订正注记」引用 ORM 实测行号。
- [x] **Add**：§浏览器层审批路径已知限制 新增 **enforcement 语义子节**——权限点绑定规则（approve → per-action `:approve`，4 实体声明归属）+ DIRECT 三轴浏览器层负向测试策略 + xwf 轴后端单测覆盖。交叉引用 2330-1 裁决与 P1.3 粒度裁决。
  - Skill: none
  - 落地：`docs/design/roles-and-permissions.md` L270-303 新增子节「xwf 4 实体 enforcement 语义裁决」——含 A. 权限点绑定规则（替代方案 a/b/c + 4 实体声明归属表 + 残留风险）、B. DIRECT 三轴浏览器层负向测试策略（替代方案 a/b/c + 对 P2.1/E1.x 冻结输入 + 残留风险），交叉引用 2330-1 + P1.3 收敛粒度裁决。

Exit Criteria:

- [x] owner doc 漂移订正 + enforcement 语义子节落地，与 2330-1 / P1.3 / ORM 真相 / roadmap 措辞一致、无矛盾。

## Draft Review Record

- Independent draft review iteration 1: needs revision（1 blocker / 1 major / 3 minor）（ses_01adfa89bffeETeyXIxk25QsfR）。B1 Payment/Receipt 误标 finance（ErpFinPayment/ErpFinReceipt），真值为 purchase ErpPurPayment / sales ErpSalReceipt（ORM `useWorkflow` + 2330-1 证实），致证据路径 `_erp-fin.action-auth.xml` 错误；M1 sibling 协调——Payment/Receipt approve 已由 sibling P1.4a 承接，缺口声明失实；m1「直系 2 实体」歧义；m2 Phase 2 submit Decision 偏弱；m3 Closure Gates 条件分支缺交叉引用。
- 合并修订（iteration 1 → v2）：订正 4 实体表域/实体名 + 证据路径（`_erp-{pur,sal,ast,hr}`）；Phase 1 routing Decision 改为「确认声明归属」（sibling P1.4a/P1.4b 承接，本计划不落地声明，保持 doc-only）；Phase 3 增 owner doc 漂移订正 Fix 项（§浏览器层表格 finance→purchase/sales）；删除 Phase 2 越界 submit Decision（pur/sal submit 归 P1.4a 内部 Decision，非 P1.6 4 实体范围）；Non-Goals 显式排除声明落地 + pur/sal submit；Closure Gates 去条件分支（纯 doc-only）。
- Independent draft review iteration 2: accept（0 blocker / 0 major / 0 minor）（ses_01ad90ba4ffeVqTZ5De2qljUsH）。B1/M1/m1/m2/m3 全部 RESOLVED 无回归：4 实体表域/实体名 + 证据路径订正（purchase ErpPurPayment / sales ErpSalReceipt，`_erp-{pur,sal,ast,hr}`）；Phase 1 改为确认声明归属（sibling P1.4a/P1.4b 承接，本计划 doc-only）；Phase 3 增 owner doc 漂移订正 Fix；Phase 2 越界 submit Decision 已删；Closure Gates 无条件分支。纯 doc-only（Skill none，无 FNPT-declaration Add），与 ORM `useWorkflow` 真相 + 既有声明一致。Plan Status → active。

## Closure Gates

> 本计划为纯裁决/文档工作（无生产代码、无状态机/xwf/ORM 改动、不落地 `:approve` 声明——仅读 action-auth.xml/ORM/状态机作裁决输入 + owner doc 回写）。移除 build/test 验证命令门控，理由：无代码变更；验证对象为裁决内部一致性、与 2330-1/P1.3/ORM 真相不漂移、owner doc 漂移订正准确。

- [x] 范围内行为完成（权限点绑定规则 + DIRECT 三轴测试策略 + 声明归属确认 + owner doc 漂移订正 + enforcement 语义子节）
- [x] 相关文档对齐（`roles-and-permissions.md` §浏览器层审批路径已知限制 / §action-level 声明层）
- [x] 无代码变更 → 跳过 build/test 门控（已说明理由）；裁决与 ORM `useWorkflow` 真相 + 既有声明/生成文件一致
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### xwf 浏览器层审批轴可达性修复

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 2330-1 权威裁决 NOT FEASIBLE，解除条件属平台 successor（nop-wf 修复 sysUser(0) 物化路径等）；本计划采用 DIRECT 三轴为浏览器层测试路径。
- Successor Required: yes（触发条件 = nop-wf 平台修复 sysUser(0) 浏览器层物化路径，或 4 实体改回 DIRECT 移除 useWorkflow）

### 生产环境 enforcement 翻转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本裁决限定测试环境；生产翻转需独立灰度计划 + 人工批准。
- Successor Required: yes（触发条件 = 测试环境全绿验收 + 生产灰度计划人工批准）

## Closure

Status Note: 纯 doc-only 裁决计划已全 3 Phase 执行完成。Phase 1（权限点绑定规则 = approve→独立 per-action `:approve` FNPT，采纳方案 a；4 实体声明归属确认：ErpHrSalary 已声明 / ErpPurPayment+ErpSalReceipt→sibling P1.4a / ErpAstDisposal→sibling P1.4b）+ Phase 2（DIRECT 三轴 approve 为浏览器层 E2E 负向断言主体，xwf 轴后端单测覆盖，采纳方案 a；P2.1/E1.x 冻结输入已记录）+ Phase 3（owner doc §浏览器层审批路径已知限制 表格 finance→purchase/sales 漂移订正 + 新增 §enforcement 语义裁决子节）均已落地于 `docs/design/roles-and-permissions.md`。无代码变更 → 跳过 build/test 门控（Closure Gates 已说明理由）。roadmap P1.6 状态 todo→done。无 `> Source Audits:` 行（roadmap-sourced plan）→ 跳过 source audit 关闭步骤。

Closure Audit Evidence:

- Auditor / Agent: Independent closure subagent (fresh session)
- Evidence: 独立结束审计 PASS（fresh session，未执行本计划）。逐项实测复核：(1) ORM 真相——`useWorkflow="true"` 在 4 个规范模型 `module-*/model/app-erp-*.orm.xml` 恰好 4 处：`module-hr/.../app-erp-hr.orm.xml:722`(ErpHrSalary)、`module-purchase/.../app-erp-purchase.orm.xml:923`(ErpPurPayment)、`module-assets/.../app-erp-assets.orm.xml:578`(ErpAstDisposal)、`module-sales/.../app-erp-sales.orm.xml:735`(ErpSalReceipt)；`module-finance/model/app-erp-finance.orm.xml` 无任何匹配（finance 无 xwf 实体）。其余 4 处匹配均在生成文件 `_app.orm.xml`（非规范源，从规范模型派生）。行号与计划声称完全一致。(2) 声明真相——delta `erp-hr.action-auth.xml` L101-107 声明 `ErpHrSalary:approve`(L101-103)+`:markPaid`(L105-107)→薪酬审批人；生成 `_erp-pur.action-auth.xml` L73-79 / `_erp-sal.action-auth.xml` L185-191 / `_erp-ast.action-auth.xml` L115-121 各仅 `:query`/`:mutation` 无 `:approve`；delta `erp-{pur,sal,ast}.action-auth.xml` 对这 3 实体仅有 `-main` 资源声明、无任何 `:approve` FNPT。(3) Sibling 计划——`2026-08-09-1400-2`(P1.4a) 与 `2026-08-09-1400-3`(P1.4b) 均存在，Plan Status 均为 `active`。(4) Owner doc——`docs/design/roles-and-permissions.md` §浏览器层审批路径已知限制 L251-256 影响表已订正为 purchase ErpPurPayment / sales ErpSalReceipt / assets ErpAstDisposal / hr ErpHrSalary（无 finance）；L258 域/实体名订正注记含 ORM 行号；L270-303 新增「xwf 4 实体 enforcement 语义裁决」子节含 A 权限点绑定规则（替代方案 a/b/c + 4 实体声明归属表 L283-288 + 残留风险 L290）+ B DIRECT 三轴浏览器层负向测试策略（替代方案 a/b/c + 对 P2.1/E1.x 冻结输入 L299-301 + 残留风险 L303）；L272 交叉引用 2330-1 + P1.3。内部一致性已复核：与 §action-level 声明层 L213（ErpHrSalary `:approve`/`:markPaid`→薪酬审批人）、§角色→权限点映射「映射粒度裁决」L116-121（采纳收敛粒度、拒绝 (b) 坍缩 mutation）、§兜底策略裁决 L221-241（双命名空间分离）均无矛盾。(5) Roadmap——`docs/backlog/permissions-enforcement-roadmap.md:38` P1.6 行状态为 `done`。(6) 计划内部一致性——3 Phase 均 `Status: completed`（L59/L82/L99），全部 Item 与 Exit Criteria 为 `[x]`，Plan Status: completed（L3），Closure Gates 唯一 `[ ]` 项为本结束审计项（已勾选）。未重开或重验 source audits（本计划无 `> Source Audits:` 行，roadmap-sourced）。

Follow-up:

- 独立结束审计待执行（Closure Gates 对应项，须独立子代理新会话）。
- 非阻塞 successor 已记录于 §Deferred But Adjudicated（xwf 浏览器层可达性修复 / 生产环境 enforcement 翻转）。
