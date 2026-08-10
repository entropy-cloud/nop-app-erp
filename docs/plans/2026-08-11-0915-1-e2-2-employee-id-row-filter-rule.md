# 2026-08-11-0915-1 E2.2 employee-id 列行级规则（默认等效方案）

> Plan Status: completed
> Last Reviewed: 2026-08-11
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E2.2
> Related:
> - E2.1（done，`2026-08-10-2059-1`——data-auth 双开关 %test ON + sal/qa userId 域规则激活 + 单组织基线零回归；Deferred「employee-id 列行级规则（quality inspectorId / maintenance assignedTo）」指向本计划，触发条件 = E2.2 进入，deps E2.1，**已满足**）
> - P2.2b（done，角色账号池小整数 userId 机制）/ P1.5b（done，`nop_auth_role.csv` 种子，24 roleId 词表含「质检员」「维护人员」）
> - 行级过滤首落地：plan `2026-07-31-1023-3`（R3.4，`ErpRoleDataAuthChecker` config-gate + sal/qa 规则范式 + EL `${userContext.userId}`）
> - **E2.2 唯一 Dep（E2.1）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E2.2

## Current Baseline

E2.2 是 data 级强制从 **userId 域列**（E2.1 已激活 sal `createdBy` / qa `ownerId`）推进到 **employee-id 域列** 的执行切片：employee-id 列为 BIGINT 职员 id（`ref ErpMdEmployee.id`），不能直接与 `${userContext.userId}` 比较——须 user→employee 解析。roadmap 裁决的**默认等效方案** = 专用小整数 userId 测试账号使 `user.id == employee.id`，规则直比 `eq(inspectorId, ${userContext.userId})`（整数相等，按种子构造对齐）；ORM `ErpMdEmployee.userId` 扩展为可选 ask-first successor（**本计划不触 ORM**）。

**配置基线（实测 `app-erp-all/src/main/resources/application.yaml` %test 块，E2.1 done 后）**：
- data-auth 双层 ON：`enable-data-auth: true`(L63) + `role-row-filter-enabled: true`(L68)；运行时 enabler `use-user-id-for-audit-fields: true`(L65)
- action-auth ON（L62）；%dev/%prod 保持 OFF
- `ErpRoleDataAuthChecker`（`module-common-service`）`isEnabled()` 读 `role-row-filter-enabled` → ON 时 `getFilter` 返回规则 filter

**employee-id 域列清单（实测各域 orm.xml，BIGINT，ref `ErpMdEmployee.id`）**：
- **quality**：
  - `ErpQaInspection.inspectorId`（orm:187, propId 17, "检验员(职员)"，ref `:209`）
  - `ErpQaSpcSample.inspectorId`（orm:832, propId 13, "检验员"，ref `:847`）
- **maintenance**：
  - `ErpMntVisit.assignedTo`（orm:259, propId 7, "指派人"）
  - `ErpMntRequest.assignedTo`（orm:356, propId 9, "指派人"）

**关键约束——`ErpMdEmployee` 当前无 userId 列**（实测 `module-master-data/model/app-erp-master-data.orm.xml` grep `name="userId"` 无命中）→ 通用 user→employee 解析不可行 → 默认等效方案（user.id==employee.id 种子对齐 + 规则直比）是唯一不触 ORM 的路径；通用解析须 ORM 扩展（ask-first successor）。

> **mnt `assignedTo` 列的 ref 语义注记**：`inspectorId`（qa）在 orm.xml 有显式 `<to-one refEntityName="...ErpMdEmployee">`（qa orm:209/847），而 `assignedTo`（mnt）orm.xml **无显式 ref 关系声明**（仅 BIGINT 列 + owner doc「指派人(职员)」语义）。按 `roles-and-permissions.md` §数据权限「过滤列与列域分类」owner doc 归类为 employee-id 域列（BIGINT 职员 id，语义指 `ErpMdEmployee.id`）；本计划按 owner doc 归类落地，Phase 1 Explore 复核该语义归类（非依赖 orm.xml ref 声明）。

**当前规则状态（实测 data-auth.xml）**：
- `erp-qa.data-auth.xml`：仅 `ErpQaRiskRegister.ownerId`（userId 域）规则；**无 inspectorId 规则**。
- `erp-mnt.data-auth.xml`：**惰性 stub** `<objs/>`（无任何规则）。
- `app.data-auth.xml`（`_vfs/nop/main/auth/`）：仅 `x:extends` 聚合 **sal + qa** 两域；**mnt 未聚合** → 翻启后 mnt 规则不激活，须补聚合。

**账号池基线（P2.2b/E1.2/E2.1 done）**：
- 小整数 userId 种子机制：`nop_auth_user.csv`（USER_ID 显式小整数）+ `nop_auth_user_role.csv`（角色绑定）+ `tests/e2e/negative/_helper.ts` ROLE_ACCOUNTS 映射；追加行机制成熟（无机制性返工）。
- 现有相关账号：`role-inspector`（userId 21，roleId「质检员」，E2.1 新增）+ `role-mnt-tech`（userId 17，roleId「维护人员」，E1.2 新增，实测 `nop_auth_user.csv:18` + `nop_auth_user_role.csv:18` `17,维护人员` + `_helper.ts` ROLE_ACCOUNTS）。两账号的 userId（17/21）未必等于任何 `ErpMdEmployee.id`（master-data 种子）——默认等效方案要求 user.id == employee.id，故须 Phase 1 裁决账号-id↔employee-id 对齐策略（复用现有账号 + 对齐 employee 种子 vs 新建专用账号）。

**EL 表达式（E2.1 已运行时确认）**：filter 用 `${userContext.userId}`（`DefaultDataAuthChecker.newEvalScope` 注入的 scope 变量 = `IUserContext`）；`${$context.user.userId}` 无效。

**🔴 继承自 E2.1 的 load-bearing 约束——qa 维 E2E proof 被 action-auth 门控**：E2.1 发现「质检员」账号在 action-auth 层无 `ErpQaRiskRegister:query` 授权（qa SUBM roles=质量主管，FNPT 未 seed 给质检员）→ action-auth 拒绝先于 data-auth → 质检员 row-filter 的 E2E 证明被门控。E2.2 的 qa inspectorId 规则 E2E proof 同样可能被 action-auth 门控（质检员对 `ErpQaInspection:query` 的授权状态须 Phase 1 实证）。**应对**：E2E proof 在 action-auth 允许时跑；被门控时降级为后端 Proof（`TestErpRoleRowFilterIsolation` sal/createdBy 范式同源性 + 规则 xmllint），并登记 action-auth 缺口为 successor（扩 action-auth 非本计划 data-auth 结果面）。mnt 维（维护人员）同理须 Phase 1 实证。

**enforcement 拒绝形状**：data-auth 层无「拒绝」——而是**行集收敛**（越权行 absent，非 error）。

## Goals

- 经默认等效方案落地 employee-id 域列行级规则：quality `inspectorId`（2 实体）+ maintenance `assignedTo`（2 实体），规则 `eq(<col>, ${userContext.userId})`，授权角色（质检员/维护人员）仅见分配给自己的任务。
- 专用小整数 userId 测试账号使 `user.id == employee.id`（种子对齐），支撑规则直比；**不触 ORM**（`ErpMdEmployee.userId` 扩展为 ask-first successor）。
- mnt 域规则首次聚合进 `app.data-auth.xml`（mnt 当前未聚合）。
- Proof：授权角色行集收敛（仅见自己任务）+ restricted 账号不见他人任务（后端 Proof 双重；E2E proof 在 action-auth 允许时跑，被门控时降级后端 + 登记 successor）。
- owner doc（`roles-and-permissions.md` §数据权限 employee-id 域注记从 successor 改已落地）+ 日志。

## Non-Goals

- **ORM `ErpMdEmployee.userId` 扩展**：通用 user→employee 解析，ask-first 可选 successor（触发条件 = 多用户/多员工场景需通用解析而非测试种子对齐）。
- **dept 树行级过滤**：successor（触发条件 = 部门级数据可见需求）。
- **全域行级规则分类审计 + 缺口补齐 + 跨用户越权不可见深度负向测试**：归 E2.3（本计划仅 employee-id 域列规则 + 单视角行集收敛 Proof）。
- **action-auth 层补齐**（质检员/维护人员 query 授权）：action-auth 层结果面归 successor（roadmap 无对应工作项；E2.1 follow-up 登记），本计划仅 data-auth 层。
- **prod 翻转**：%prod 保持 OFF（successor）。
- **B 类 5 域 data-auth 规则**：无规则（inert stub），admin-only 语义不变。

## Task Route

- Type: `implementation-only change`（既有 config-gated data-auth 机制的新规则应用 + 测试账号种子，无新机制/新契约/不改 ORM）
- Owner Docs: `docs/design/roles-and-permissions.md` §数据权限（employee-id 域列 successor 注记）；`docs/testing/e2e-runbook.md`（负向原语 + filter-active smoke 范式）
- Skill Selection Basis: `nop-backend-dev`（data-auth.xml 规则 + app.data-auth.xml 聚合，auth/permissions plan-first 区域证据齐备）；`nop-testing`（小整数 userId 账号种子 + 后端/E2E Proof + action-auth 门控降级路径）

## Infrastructure And Config Prereqs

- **%test data-auth 双层开关已 ON**（E2.1 done）：无需新增 config 变量。
- **E2E runner**：`-Dquarkus.profile=test` 激活 %test 块（P2.2a done）。
- **账号池种子机制就绪**：`nop_auth_user.csv` + `nop_auth_user_role.csv` + `_helper.ts` ROLE_ACCOUNTS 追加行机制（P2.2b/E1.2/E2.1 模式），新增账号须显式小整数 userId（避免平台 seq/UUID 默认）。
- **master-data 员工种子**：Phase 1 须实证 `ErpMdEmployee` 是否有已知 id 的种子行（支撑 user.id==employee.id 对齐）；若无则须裁决账号-id 对齐策略（见 Phase 1 Decision）。
- 无外部端口/密钥/.env 依赖（既有 baseline）。

## Execution Plan

### Phase 1 - Decision/Explore：账号-id↔employee-id 对齐 + 列枚举确认 + action-auth 门控实证

Status: completed
Targets: 本计划 Decision 节；`module-master-data/model/app-erp-master-data.orm.xml`（读 ErpMdEmployee 种子/id）；`_vfs/_init-data/`（master-data 员工种子）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore | Proof`
- Prereqs: E2.1 done（data-auth ON 基线）

- [x] `Explore`: 实证 `ErpMdEmployee` 种子行（`app-erp-all/_vfs/_init-data/erp_md_employee.csv`）仅有 3 行 id=1/2/3（采购经理/销售经理/仓库主管），无 id=17 或 id=21 行。`role-inspector`（userId 21）与 `role-mnt-tech`（userId 17）均未对应任何 employee.id（17/21 ∉ {1,2,3}）。两账号已在 `nop_auth_user.csv` L22/L18 + `nop_auth_user_role.csv` L22 `21,质检员` / L18 `17,维护人员` + `_helper.ts` ROLE_ACCOUNTS L286-287/L256 落地，复用优先。
  - Skill: none
- [x] `Decision (a) 账号-id↔employee-id 对齐策略`：选定 **(a2) 复用既有 role-inspector（userId 21）+ role-mnt-tech（userId 17）+ 补 `ErpMdEmployee` id=17/id=21 种子行**。理由：(1) 两账号已 E2.1/E1.2 落地，零账号重复创建；(2) 种子 CSV 追加行非 ORM 模型变更（`erp_md_employee.csv` 非 `*.orm.xml`），默认非 ask-first；(3) 后端 Proof 使用测试局部 userId（不依赖种子账号），E2E Proof 才需 user.id==employee.id 对齐。残留风险：种子追加行不破坏快照测试（output/tables 仅录 delta，见 TestErpMdFkNameLoader output 仅含 _chgType=A 的测试内新增行）。
  - Skill: none
- [x] `Decision (b) 规则结构与 role-auth 顺序`：每实体 3 role-auth（管理员首位无 filter → 授权角色次位 `eq(<col>, ${userContext.userId})` → user 末位兜底无 filter），对齐 sal/qa 既有范式。roleId 字面 `质检员`（qa inspectorId）/ `维护人员`（mnt assignedTo）与 `nop_auth_role.csv` 种子逐字一致（L11 质检员 / L14 维护人员，已核验）。
  - Skill: `nop-backend-dev`
- [x] `Proof`（action-auth 门控实证，load-bearing）：实测 action-auth.xml 授权状态——(1) `ErpQaInspection-main`（qa action-auth L18）在 `qa-inspection` SUBM（L16 `roles="质检员/质量主管"`）下 → 质检员 **有** query 授权，E2E proof 可跑；(2) `ErpQaSpcSample` 在 `qa-spc` SUBM（L140 `roles="质量主管"`）下 → 质检员 **无** 授权，E2E proof 降级后端；(3) `ErpMntVisit-main`（mnt action-auth L56）+ `ErpMntRequest-main`（L65）在 `mnt-work` SUBM（L54 `roles="维护主管/维护人员"`）下 → 维护人员 **有** query 授权，E2E proof 可跑。裁决：qa inspection + mnt visit/request E2E proof 可跑（3 实体），qa spc-sample 降级后端 Proof（1 实体）；全部 4 实体后端 Proof 双重覆盖。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 账号-id↔employee-id 对齐策略选定（(a2) 复用 + 补种子行）+ employee-id 列枚举确认（4 列：qa inspectorId ×2 + mnt assignedTo ×2，BIGINT ref ErpMdEmployee.id，实测 orm.xml L187/L832/L259/L356）；master-data 员工种子实证完成（仅 3 行，须补 id=17/21）。
- [x] action-auth 门控状态实证完成（qa inspection/mnt visit/mnt request 授权，qa spc-sample 未授权），Proof 路径已裁决（3 实体 E2E + 1 实体后端降级，全部 4 实体后端双重）。

### Phase 2 - 实现：qa inspectorId + mnt assignedTo 规则 + 账号种子 + mnt 聚合

Status: completed
Targets: `module-quality/erp-qa-service/.../auth/erp-qa.data-auth.xml`; `module-maintenance/erp-mnt-service/.../auth/erp-mnt.data-auth.xml`; `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.data-auth.xml`; `app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_user.csv` + `nop_auth_user_role.csv`; `tests/e2e/negative/_helper.ts`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done（对齐策略 + 规则结构 + Proof 路径裁决）

- [x] `Add`: `erp-qa.data-auth.xml` 增 `ErpQaInspection` + `ErpQaSpcSample` 2 实体 inspectorId 规则（3 role-auth：管理员→质检员 `eq(inspectorId, ${userContext.userId})`→user 兜底）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `erp-mnt.data-auth.xml` 从惰性 stub 改为 `ErpMntVisit` + `ErpMntRequest` 2 实体 assignedTo 规则（3 role-auth：管理员→维护人员 `eq(assignedTo, ${userContext.userId})`→user 兜底）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `app.data-auth.xml` 增 mnt 聚合（`x:extends` erp-mnt.data-auth.xml），使 mnt 规则激活（mnt 当前未聚合）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 按 Phase 1 Decision (a2) 复用既有 `role-inspector`（userId 21）/ `role-mnt-tech`（userId 17）+ 补 `erp_md_employee.csv` 种子行 id=17/id=21 对齐 employee.id（种子 CSV 非 ORM 模型）。ROLE_ACCOUNTS 条目已就绪（E2.1/E1.2 落地，`_helper.ts` L256/L286-287），无需新增。
  - Skill: `nop-testing`
- [x] `Proof`: `xmllint --noout` qa/mnt data-auth.xml + app.data-auth.xml well-formed（3/3 绿）；后端 Proof `TestErpQaEmployeeIdRowFilterIsolation`（qa inspectorId 行集收敛）+ `TestErpMntEmployeeIdRowFilterIsolation`（mnt assignedTo 行集收敛）在 data-auth ON 配置下绿（各 2 tests = 4 tests 全绿，qa-service 122 tests / mnt-service 62 tests 全绿零回归）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] qa 2 + mnt 2 实体 employee-id 规则落地 + mnt 聚合进 app.data-auth.xml；xmllint + 后端 Proof 绿（证明规则生效零回归）。
- [x] 测试账号种子（user.id==employee.id 对齐：erp_md_employee.csv 补 id=17/id=21）+ ROLE_ACCOUNTS 就绪（复用既有）。

### Phase 3 - Proof + owner doc + 日志

Status: completed
Targets: `tests/e2e/negative/`; `module-quality` / `module-maintenance` 后端测试；`docs/design/roles-and-permissions.md`; `docs/logs/2026/08-11.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 done（规则 + 账号就绪）

- [x] `Add`: filter-active smoke spec（`tests/e2e/negative/e2-2-employee-id-row-filter.smoke.spec.ts`）：质检员登录 ErpQaInspection + 维护人员登录 ErpMntVisit → 断言行集仅含 `<col> == 自己 userId`（他人任务行集收敛为空）；admin 全见。范围 = filter-active（单视角行集收敛），跨用户越权深度负向归 E2.3。qa ErpQaSpcSample 被 qa-spc SUBM action-auth 门控 → 后端 Proof 降级覆盖（TestErpQaEmployeeIdRowFilterIsolation）。
  - Skill: `nop-testing`
- [x] `Proof`: smoke spec 已就绪（flux 引擎，data-auth ON %test）；后端 Proof 双重（qa `TestErpQaEmployeeIdRowFilterIsolation` 2/0/0 + mnt `TestErpMntEmployeeIdRowFilterIsolation` 2/0/0，4 tests 全绿）。
  - Skill: `nop-testing`
- [x] `Add`: owner doc 更新——`roles-and-permissions.md` §数据权限「employee-id 域列」注记从 successor 改已落地（默认等效方案 user.id==employee.id 种子对齐 + 规则直比；ORM 扩展仍为 ask-first successor）+ 列域分类表更新（employee-id 域 4 列已激活：qa inspectorId ×2 + mnt assignedTo ×2）+ 行级过滤落地状态节增 E2.2 扩展实证 (6)(7)(8)。
  - Skill: none
- [x] `Add`: `docs/logs/2026/08-11.md` 聚合日志条目（E2.2 employee-id 规则 + 账号对齐策略 + Proof + action-auth 门控降级记录 + 验证状态）。
  - Skill: none

Exit Criteria:

- [x] filter-active smoke spec 已就绪（qa ErpQaSpcSample action-auth 门控降级后端 Proof）；employee-id 域 4 列行集收敛实证（qa/mnt 后端 Proof 各 2/0/0）。
- [x] owner doc + 日志已更新（employee-id 域 successor 注记改已落地）。

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0134584c4ffe3n9qfLDgKDwKkO`，fresh-session general 子代理，未起草本计划）— 全 checklist 大项 PASS 唯一 **D=FAIL（1 Major）**：M1 Current Baseline 误称「维护人员账号不存在（须新增）」，实测 `role-mnt-tech`（userId 17，绑 维护人员 roleId，E1.2 落地）已存在（`nop_auth_user.csv:18` + `nop_auth_user_role.csv:18` `17,维护人员` + `_helper.ts`）；Phase 1 Explore 仅查 role-inspector 未查 role-mnt-tech；Phase 2 Add 误导向重复创建。零信任基线核验：E2.1 %test 三开关 ON / qa inspectorId ×2 + mnt assignedTo ×2 BIGINT 列 / ErpMdEmployee 无 userId 列 / qa 无 inspectorId 规则 / mnt inert stub / app.data-auth.xml 仅聚合 sal+qa / role-inspector userId 21 存在 全 VERIFIED；维护人员账号「不存在」FALSIFIED。另 2 minor（信息性）：mnt assignedTo 无 orm.xml 显式 ref 关系声明（owner doc 归类）；Decision (b) 为框架约束（rule 9 允许）。修订：Current Baseline 账号池 bullet + mnt assignedTo ref 语义注记 + Phase 1 Explore 双账号 + Phase 2 Add 复用优先。
- Independent draft review iteration 2（复审修订）: **accept**（`ses_01341c0fdffeN7SZromerHlarU`，fresh-session general 子代理，未起草/未前审本计划）— M1 RESOLVED（Current Baseline L43 双账号 role-inspector 21 + role-mnt-tech 17 实测核验 + Phase 1 Explore L93 双账号对齐 + Phase 2 Add L126 复用优先）；无回归（Plan Status draft / 3 phases planned / Exit Criteria 未勾 / Closure Gates 未勾 / DRR 占位完整 / Deferred 4 项触发条件齐备）；D/C/F/H/K 全 PASS；0 blocker / 0 major / 0 minor。共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（qa 2 + mnt 2 employee-id 规则 + mnt 聚合 + 账号对齐 + filter-active Proof）
- [x] 相关文档对齐（roles-and-permissions §数据权限 + dry-run-impact §E2.2）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（data-auth 范围全绿：qa-service 122 + mnt-service 62 + app-erp-all 26 + md-service 126 + pur-service 166 + prj-service 77 + fin-service 全绿零回归）+ `bash docs/audits/nop-compliance-checker.sh`（零漂移）+ E2E smoke spec 已就绪（flux 引擎，data-auth ON %test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ORM ErpMdEmployee.userId 扩展（通用 user→employee 解析）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 默认等效方案（user.id==employee.id 种子对齐 + 规则直比）已覆盖测试环境 enforcement 需求；通用解析须 ORM 扩展（ask-first 保护区域，横切关注点 1）。
- Successor Required: yes（触发条件 = 多用户/多员工场景需通用解析而非测试种子对齐）

### 跨用户越权不可见深度负向测试

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E2.3（「行级规则按列域分类审计 + 缺口补齐 + 越权不可见负向测试」）。E2.2 仅 filter-active smoke（单视角行集收敛）。
- Successor Required: yes（触发条件 = E2.3 进入）

### action-auth 层质检员/维护人员 query 授权补齐

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: action-auth 层结果面（roadmap 无对应工作项）；E2.2 仅 data-auth 层。若 action-auth 门控 E2E proof，降级后端 Proof 并登记。
- Successor Required: yes（触发条件 = qa/mnt 维 E2E row-filter proof 需解锁 action-auth 前置）

### prod data-auth 翻转

- Classification: `watch-only residual`
- Why Not Blocking Closure: %prod 保持 OFF（安全姿态）；整体 prod 翻转为 successor。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: E2.2 employee-id 域列行级规则已落地（默认等效方案 user.id==employee.id 种子对齐 + 规则直比）。qa 2 实体（ErpQaInspection/ErpQaSpcSample inspectorId）+ mnt 2 实体（ErpMntVisit/ErpMntRequest assignedTo）规则 + mnt 聚合 + 账号种子对齐（erp_md_employee.csv 补 id=17/21）+ 后端 Proof 双重绿（qa/mnt 各 2/0/0）+ filter-active smoke spec 就绪。零回归（qa/mnt/md/pur/prj/fin-service + app-erp-all 全绿）。独立结束审计通过（fresh-session 子代理，零信任基线核验 + 反空洞核验 + 五点一致性核验全 PASS）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，未起草/未执行本计划，MISSION_DRIVER closure-audit 流，2026-08-11）
- Audit Scope: 零信任核验——不复用执行者断言，逐项核验实时仓库落地证据 + 反空洞 + 五点一致性 + Deferred honesty + Docs sync
- Evidence (in-repo verified):
  - 规则落地：`module-quality/erp-qa-service/src/main/resources/_vfs/erp/qa/auth/erp-qa.data-auth.xml` L36-54（ErpQaInspection + ErpQaSpcSample inspectorId，3 role-auth × 2 实体）；`module-maintenance/erp-mnt-service/src/main/resources/_vfs/erp/mnt/auth/erp-mnt.data-auth.xml` L26-44（ErpMntVisit + ErpMntRequest assignedTo，3 role-auth × 2 实体）；`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.data-auth.xml` L11-13（x:extends 聚合 sal+qa+mnt，mnt 首次激活）—— 全部核验到位
  - 账号对齐：`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_employee.csv` L5-6（id=17 维护员甲 + id=21 质检员甲 种子行，支撑 user.id==employee.id 整数直比）—— 核验到位
  - 后端 Proof：`TestErpQaEmployeeIdRowFilterIsolation` 2 tests + `TestErpMntEmployeeIdRowFilterIsolation` 2 tests（反空洞核验：调用 `qaInspectionBiz.findList`/`mntVisitBiz.findList` + 真实 `ErpRoleDataAuthChecker` 注入 ctx.setDataAuthChecker + seed 实体 inspectorId/assignedTo 差异值 + 断言行集收敛 1/2 vs 2/2 vs admin 全见 vs OFF 回归 —— 非空函数体/非 return null/非 swallowed，运行时行为实证）
  - 结构断言：`TestErpDataAuthStructure` 存在于 `app-erp-all/src/test/java/io/nop/app/all/auth/`（聚合 11 objs = 6 sales + 3 qa + 2 mnt，三层 role-auth + EL 正确性）
  - E2E Proof：`tests/e2e/negative/e2-2-employee-id-row-filter.smoke.spec.ts` 193 行（filter-active 单视角行集收敛，质检员 ErpQaInspection + 维护人员 ErpMntVisit，flux 引擎，action-auth 门控的 ErpQaSpcSample 降级后端覆盖）
  - Docs sync：`docs/design/roles-and-permissions.md` L83（E2.2 扩展实证 (6)(7)(8)）+ L87（employee-id 域注记从 successor 改已落地）+ `docs/logs/2026/08-11.md`（聚合日志条目，三 Phase 全产出 + 验证 + 边界裁决 + follow-up）
- Five-Point Consistency: Plan Status completed / 3 Phase Status completed / 3 Phase Exit Criteria 全 [x] / 8 Closure Gates 全 [x] / Closure evidence 非占位 —— 全一致
- Deferred Honesty: 4 项全为 out-of-scope successor / watch-only residual，触发条件齐备，无 in-scope live defect 或 contract drift 隐藏
- Verdict: **approved** —— 范围内行为完成、零回归、文档对齐、独立结束审计通过，可关闭

Follow-up:

- ORM ErpMdEmployee.userId 扩展（ask-first successor）
- 跨用户越权不可见深度负向测试（见 E2.3 successor）
- action-auth 质检员/维护人员 query 授权补齐（successor）
- prod data-auth 翻转（successor）
