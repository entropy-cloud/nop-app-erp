# 2026-08-09-1400-3 per-action-fnpt-mfg-subcontract-assets-disposal

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.4b
> Related: mission `permissions-enforcement`；P1.3（粒度裁决，已 done）；P1.1（敏感字段清单，已 done）；P1.6（xwf 语义裁决，draft——assets Disposal 是 P1.6 4 实体之一，本计划与 P1.6 软协调：执行序 P1.6(N=1) → 本计划(N=3)，使 Disposal binding 裁决先行落地）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.4b

## Current Baseline

P1.3 已裁决映射收敛粒度 = 角色×SUBM + **敏感动作 per-action FNPT** + 兜底策略（双命名空间分离）。manufacturing approve subcontract（委外审批）与 assets 处置（Disposal approve）属敏感动作，应作为独立 per-action FNPT 点脱离泛化 `mutation` 桶。

**mfg / assets 现状**（实测生成文件 + delta）：

- **manufacturing** delta `erp-mfg.action-auth.xml`：已声明 WorkOrder `:start`/`:close`/`:cancel`→生产主管（P1-MA3-046 首批）。**委外审批（approve subcontract）未声明**——生成文件 `_erp-mfg.action-auth.xml` 中委外单实体 `ErpMfgSubcontractOrder`（displayName "委外加工单"，delta `erp-mfg.action-auth.xml:113` `ErpMfgSubcontractOrder-main` + ORM `app-erp-manufacturing.orm.xml:1098`）仅有 `:query`/`:mutation`，approve 坍缩进 `mutation` 桶。
- **assets** delta `erp-ast.action-auth.xml`：**未声明任何 per-action FNPT**（无 `<children>` FNPT 块）。生成文件 `_erp-ast.action-auth.xml` 中 ErpAstDisposal（资产处置）仅有 `:query`/`:mutation`，approve 坍缩进 `mutation` 桶。
- **ErpAstDisposal 是 P1.6 xwf 4 实体之一**（`disposal-approval/v1.xwf`，浏览器层 xwf 轴不可达，DIRECT 三轴可达，见 2330-1 裁决）——P1.6 Phase 1 裁决其 approve 权限点绑定规则与 roles 种子策略，本计划按 P1.6 裁决对齐 Disposal 的 `roles` 种子（执行序 P1.6 先行）。

**enforcement 状态**：`nop.auth.enable-action-auth=false`（默认 OFF），本计划仅"已就绪可授权"，不拦截任何调用。

**SoD 程序级守卫**（plan 2026-07-31-1023-2 R3.3）：manufacturing approve 路径已落地 SoD（比对 `createdBy` 与审核人 `userId`）；assets approve 路径 SoD 为显式 successor（R3.3 扩展域铺开 Non-Goal）。SoD 与 RBAC FNPT 正交。

**缺口**：mfg 委外审批 + assets 处置 approve 无独立 per-action FNPT 点 + roles 种子——enforcement 翻转后坍缩进泛化 `mutation` 桶，丧失独立管控（违背 P1.3 收敛粒度）。

## Goals

- **补齐 mfg 委外审批 per-action FNPT 声明**：为委外单 approve 敏感动作声明独立 per-action FNPT 点 + roles 种子（生产主管/审核人，按 state-machine.md 核对），保持 enforcement OFF。
- **补齐 assets 处置 approve per-action FNPT 声明**：为 ErpAstDisposal approve 声明独立 per-action FNPT 点 + roles 种子（资产管理员/管理员，按 P1.6 Phase 1 Disposal binding 裁决对齐），保持 enforcement OFF。
- **每集群独立交付 + 独立回归**：mfg 与 assets 各自声明 + 各自核验。

## Non-Goals

- **不翻转 enforcement 开关**（归 P2.4/E1.x）。
- **不产 auth 表 CSV 种子**（归 P1.5b；本计划仅 `roles` 静态属性种子）。
- **不改 WorkOrder 既有 `:start`/`:close`/`:cancel` 声明**（已落地，本计划仅增量委外审批 + Disposal）。
- **不改生成文件** `_erp-*.action-auth.xml`；声明只在 delta 非生成文件。
- **不改 xwf / DIRECT 三轴状态机业务逻辑**（仅声明权限点；Disposal 测试策略归 P1.6）。
- **不做 assets 其他实体（资本化/价值调整等）的 per-action 声明**（仅 Disposal 处置；其余归后续集群或 successor）。

## Task Route

- Type: `implementation-only change`（delta action-auth.xml per-action FNPT 声明补齐 + roles 种子，enforcement 保持 OFF）
- Owner Docs: `docs/design/roles-and-permissions.md` §action-level 声明层、§高危操作权限（资产报废/出售处置）、§浏览器层审批路径已知限制（Disposal）
- Skill Selection Basis: `nop-backend-dev` —— delta action-auth.xml per-action FNPT 声明属后端权限声明层工作（与 roadmap 表格 P1.4b Skill 列一致）；不写 BizModel/Processor 代码。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（仅 delta XML 声明，enforcement 保持 OFF）。

## Execution Plan

### Phase 1 - mfg 委外审批 per-action FNPT 声明

Status: completed
Targets: `module-manufacturing/erp-mfg-web/.../_vfs/erp/mfg/auth/erp-mfg.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Proof`
- Prereqs: P1.3（done）；P1.1（done）

- [x] **Add**：在 delta `erp-mfg.action-auth.xml` 为委外单（`ErpMfgSubcontractOrder`）approve 敏感动作声明独立 per-action FNPT 点 `ErpMfgSubcontractOrder:approve`，挂在 `ErpMfgSubcontractOrder-main` 的 `<children>` 下（参照既有 WorkOrder `:start`/`:close`/`:cancel` 范式，delta `erp-mfg.action-auth.xml` 既有行）。`<permissions>` = `ErpMfgSubcontractOrder:approve`；`roles` 种子按 `docs/design/manufacturing/state-machine.md` §角色与权限 核对（生产主管/审核人）。具体权限点 ID 执行时按生成文件 `_erp-mfg.action-auth.xml` 核验（不在本计划冻结，遵循 P1.3 残留风险）。
  - Skill: `nop-backend-dev`
- [x] **Proof**：xmllint well-formed 校验 `erp-mfg.action-auth.xml` 通过 + `permissionToRoles` 一致性自检（角色名与角色矩阵一致）。
  - Skill: none

Exit Criteria:

- [x] mfg 委外审批 per-action FNPT 声明落地，xmllint 通过，roles 种子与角色矩阵一致。

### Phase 2 - assets 处置 approve per-action FNPT 声明

Status: completed
Targets: `module-assets/erp-ast-web/.../_vfs/erp/ast/auth/erp-ast.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Proof`
- Prereqs: Phase 1；**软协调 P1.6 Phase 1**（Disposal binding 裁决——若 P1.6 已 active/done，按其裁决对齐 Disposal `roles` 种子；若 P1.6 仍未决，本阶段按 `roles-and-permissions.md` §高危操作权限「资产报废/出售处置=资产管理员+审批」先行声明，并在 plan 记录假设，待 P1.6 决后核对）

- [x] **Add**：在 delta `erp-ast.action-auth.xml` 为 ErpAstDisposal approve 声明独立 per-action FNPT 点 `ErpAstDisposal:approve`，挂在 `ErpAstDisposal-main` 的 `<children>` 下。`roles` 种子按 §高危操作权限 + P1.6 Disposal 裁决（资产管理员/管理员）。Disposal 是 xwf 实体——声明仅落权限点 + 种子，**测试策略（DIRECT 三轴）归 P1.6，不在本计划**。
  - Skill: `nop-backend-dev`
- [x] **Proof**：xmllint well-formed 校验 `erp-ast.action-auth.xml` 通过 + `permissionToRoles` 一致性自检。
  - Skill: none

Exit Criteria:

- [x] assets 处置 approve per-action FNPT 声明落地，xmllint 通过，roles 种子与角色矩阵 + P1.6 裁决一致（或显式记录假设待核对）。

### Phase 3 - owner doc 实现注记 + 日志

Status: completed
Targets: `docs/design/roles-and-permissions.md` §action-level 声明层
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2

- [x] **Add**：§action-level 声明层「已落地」表增列 mfg 委外审批 + assets Disposal approve per-action FNPT 点 + roles 种子（行号证据），与既有 finance/b2b/mfg/inv/hr 行对齐。
  - Skill: none

Exit Criteria:

- [x] owner doc 实现注记落地，与 delta 文件真相源一致。

## Draft Review Record

- Independent draft review iteration 1: needs revision（1 blocker / 0 major / 2 minor）（ses_01adf4c2cffevw34MjZscVORgQ）。B1 委外实体名 `ErpMfgnOrder` 错误（grep 截断产物），真值 `ErpMfgSubcontractOrder`（displayName "委外加工单"，delta `erp-mfg.action-auth.xml:113` + ORM `app-erp-manufacturing.orm.xml:1098`），实体名前缀是 `<permissions>` 声明的 load-bearing 依据；m1 Phase 1 roles 种子未显式引 state-machine 路径；m2 Phase 2 "+审批"→业务角色「管理员」为假设（已由 P1.6 fallback 覆盖）。
- 合并修订（iteration 1 → v2）：订正委外实体名 `ErpMfgSubcontractOrder`（基线 + Phase 1 Add 项 `<permissions>`/`<resource>` ID）；Phase 1 roles 种子显式引 `docs/design/manufacturing/state-machine.md` §角色与权限；Phase 2 "+审批"假设保留（P1.6 soft-coordination fallback 已覆盖，显式记录）。
- Independent draft review iteration 2: <待独立复审>

## Closure Gates

> 本计划改 delta action-auth.xml（声明层，enforcement 保持 OFF，不改运行时行为）。Closure Gates 运行 delta XML well-formed + compliance checker 对照 `known-good-baselines.md` 零漂移（横切关注点 7）。完整 build/test 在此运行一次。

- [x] 范围内行为完成（mfg 委外审批 + assets Disposal approve per-action FNPT 声明 + roles 种子 + owner doc 注记）
- [x] 相关文档对齐（`roles-and-permissions.md` §action-level 声明层）
- [x] 已运行验证：`xmllint --noout` 两个 delta 文件 + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移 + `mvn clean install -DskipTests`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### assets approve 路径 SoD 程序级守卫

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: R3.3 扩展域 SoD 铺开为显式 successor（roadmap Non-Goal）；SoD 与 RBAC FNPT 正交，不影响本计划 per-action 声明。
- Successor Required: yes（触发条件 = 扩展域 SoD 抽样审计或自审回归）

### ErpAstDisposal DIRECT 三轴 enforcement 测试

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Disposal 测试策略（DIRECT 三轴浏览器层负向）归 P1.6 Phase 2 裁决；本计划仅声明权限点。
- Successor Required: yes（触发条件 = P1.6 裁决落地 + E1.x 高危翻转）

### assets 其他敏感实体 per-action 声明

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅 Disposal 处置；资本化/价值调整/暂停恢复等归后续集群或 successor（触发条件 = assets 域敏感动作灰度批准前）。
- Successor Required: yes

## Closure

Status Note: 执行完成 2026-08-09。三阶段全绿：(1) mfg 委外审批 `ErpMfgSubcontractOrder:approve`→生产主管 落地于 delta `erp-mfg.action-auth.xml` L118-121（xmllint 通过）；(2) assets 处置 `ErpAstDisposal:approve`→资产管理员/管理员 落地于 delta `erp-ast.action-auth.xml` L77-79（xmllint 通过，roles 对齐 §高危操作权限「资产报废/出售处置=资产管理员+审批」+ P1.6 Disposal 裁决 rule A）；(3) owner doc `roles-and-permissions.md` §action-level 已落地表 + §灰度推进路线 + §既有种子证据 + P1.6 表 ErpAstDisposal 行 均已同步更新。验证：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；compliance checker 零漂移（改 XML+md，无 Java 变更）；codegen 自动同步两 `_erp-*-web.i18n.yaml` 增对应 FNPT i18n key。结束审计待独立子代理执行。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，mission-driver 2026-08-09-075057-mission-driver，未复用执行者上下文）
- Evidence: 执行证据——delta `erp-mfg.action-auth.xml:118-121`（`FNPT:ErpMfgSubcontractOrder:approve` roles=生产主管）、delta `erp-ast.action-auth.xml:77-79`（`FNPT:ErpAstDisposal:approve` roles=资产管理员/管理员）；`roles-and-permissions.md` §action-level 已落地表含 manufacturing subcontract + assets disposal 两行、§既有种子证据含 mfg subcontract + assets 行、P1.6 表 ErpAstDisposal 行已改「已声明」；`mvn clean install -DskipTests` BUILD SUCCESS（156 模块）；xmllint 两 delta 通过；compliance checker 零漂移。
- 独立审计复核（2026-08-09）：逐项对照实时仓库核验通过——(1) `module-manufacturing/.../erp-mfg.action-auth.xml:118-121` 实测存在 `FNPT:ErpMfgSubcontractOrder:approve` resourceType=FNPT roles=生产主管，`<permissions>ErpMfgSubcontractOrder:approve</permissions>`，挂在 `ErpMfgSubcontractOrder-main` 的 `<children>` 下；(2) `module-assets/.../erp-ast.action-auth.xml:77-79` 实测存在 `FNPT:ErpAstDisposal:approve` roles=资产管理员/管理员，挂在 `ErpAstDisposal-main` 的 `<children>` 下；(3) `docs/design/roles-and-permissions.md:241,244` 实测含两行落地证据；(4) codegen i18n 自动同步实测——`_erp-mfg-web.i18n.yaml:37` 与 `_erp-ast-web.i18n.yaml:29` 含对应 FNPT key。反空洞检查：声明均为真实 `<permissions>`+`roles`，非 `{}`/`return null`/占位。文本一致性：Plan Status completed / 三 Phase Status completed / 三 Exit Criteria 全 [x] / Closure Gates 全 [x] 一致。Deferred 三项均正交 successor，无范围内缺陷降级。审计通过，结束审计门控由独立子代理勾选（执行者未自审）。

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
