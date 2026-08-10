# 2026-08-11-0915-2 E2.3 行级规则按列域分类审计 + 缺口补齐 + 越权不可见负向测试

> Plan Status: active
> Last Reviewed: 2026-08-11
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E2.3
> Related:
> - E2.1（done，`2026-08-10-2059-1`——data-auth 双开关 %test ON + sal/qa userId 域规则激活 + 单组织基线零回归；Deferred「越权不可见跨用户深度负向测试」指向本计划，触发条件 = E2.3 进入，**已满足**）
> - E2.2（plan `2026-08-11-0915-1`，N=1，同批起草——employee-id 域列规则默认等效方案：user.id==employee.id 种子对齐 + 规则直比。E2.3 越权不可见负向断言「用 E2.2 同款机制整数 userId 账号（可复用/自建）」，**不构成硬 Dep**（roadmap E2.3 Deps=E2.1）；E2.2 已落地则复用账号，未落地则自建）
> - P2.3（done，负向隔离测试原语 `expectRowsHidden`/`expectRowsVisible`/`loginAsRole` + ROLE_ACCOUNTS + 脚手架）
> - **E2.3 唯一 Dep（E2.1）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E2.3

## Current Baseline

E2.3 是 data 级强制的**收尾审计 + 深度负向证明**切片：(1) 对全 19 域行级规则按列域分类审计，产出覆盖矩阵 + 识别缺口；(2) 补齐 data-auth 层识别到的缺口；(3) 跨用户越权不可见深度负向测试（A 创建数据，B 查询时被行级过滤隔离，越权行 absent）。E2.1 仅交付 filter-active smoke（单视角行集收敛），跨用户越权深度 Proof 归本计划。

**配置基线（E2.1 done 后，%test）**：data-auth 双层 ON（`enable-data-auth: true`(L63) + `role-row-filter-enabled: true`(L68) + `use-user-id-for-audit-fields: true`(L65)）；action-auth ON（L62）；%dev/%prod OFF。

**列域分类现状（实测各域 orm.xml + data-auth.xml，对齐 `roles-and-permissions.md` §数据权限「过滤列与列域分类」）**：
- **userId 域列**（VARCHAR，直接与 `${userContext.userId}` 比较，E2.1 已激活 %test ON）：
  - sales `createdBy`（6 实体：ErpSalOrder/Quotation/Delivery/Invoice/Receipt/Return）
  - quality `ownerId`（ErpQaRiskRegister，VARCHAR stdDomain="userId"）
- **employee-id 域列**（BIGINT 职员 id，须 user→employee 解析；E2.2 successor 默认等效方案 user.id==employee.id 种子对齐）：
  - quality `inspectorId`（ErpQaInspection orm:187 + ErpQaSpcSample orm:832）
  - maintenance `assignedTo`（ErpMntVisit orm:259 + ErpMntRequest orm:356）
  - `ErpMdEmployee` 当前无 userId 列 → 通用解析不可行（E2.2 默认方案绕过）
- **「全见」设计决定**：finance 空 `<objs/>`（DefaultDataAuthChecker 对未声明 obj 返回 null=无 filter=全见，语义正确，非巧合）
- **无列/inert stub 域**：inv/pur/hr/ast/prj/... + B 类 5 域（CRM/CS/APS/Logistics/DRP）data-auth.xml 为 inert stub `<objs/>`，未聚合进 `app.data-auth.xml`（仅 sal+qa 聚合；mnt 待 E2.2 聚合）

**`app.data-auth.xml` 聚合状态（实测）**：当前仅 `x:extends` sal + qa（E2.2 将补 mnt）。其余 17 域规则文件存在但未聚合 → 翻启后不激活。

**负向原语基线（P2.3 done）**：`tests/e2e/negative/_helper.ts` 提供 `expectRowsHidden` / `expectRowsVisible` / `loginAsRole` + `ROLE_ACCOUNTS`。账号池含 sal/qa/mnt 角色 + role-restricted（userId 10，平台 user）+ nop。

**继承约束——E2.1/E2.2 action-auth 门控**：qa 维 row-filter E2E proof 被 action-auth 门控（质检员缺 query 授权，E2.1 finding）；mnt 维同理（E2.2 Phase 1 实证）。data-auth 层行集收敛可由后端 Proof 证（`TestErpRoleRowFilterIsolation` 范式同源性），E2E 跨用户负向须 action-auth 允许或降级。

**enforcement 拒绝形状**：data-auth 层无「拒绝」——行集收敛（越权行 absent，非 error）。

## Goals

- **分类审计**：枚举全 19 域行级规则覆盖，按列域分类（userId / employee-id / 全见设计决定 / 无列 inert）产出覆盖矩阵，识别 data-auth 层缺口。
- **缺口补齐**：补齐审计识别到的 data-auth 层行级规则缺口（仅 data-auth 层；action-auth 层缺口登记 successor）。
- **跨用户越权不可见深度负向测试**：A 创建数据 → B（不同 userId）查询时越权行被过滤 absent（`expectRowsHidden`），用 E2.2 同款机制整数 userId 账号（E2.2 已落地则复用，未落地则自建 per roadmap「可复用/自建」条款）。
- owner doc（`roles-and-permissions.md` §数据权限 列域分类覆盖矩阵）+ dry-run-impact + 日志。

## Non-Goals

- **dept 树行级过滤**：successor（触发条件 = 部门级数据可见需求）。
- **action-auth 层缺口补齐**（质检员/维护人员 query 授权等）：action-auth 层结果面（roadmap 无对应工作项），本计划仅 data-auth 层；action-auth 门控的 E2E 维降级后端 Proof + 登记 successor。
- **ORM ErpMdEmployee.userId 扩展**：ask-first successor（E2.2 Deferred）。
- **新增列域类型/新机制**：仅审计 + 补齐既有列域规则。
- **prod 翻转**：%prod 保持 OFF。
- **orgId 多公司隔离**：独立开关门控（Non-Goal）。

## Task Route

- Type: `verification or audit work`（分类审计）+ `implementation-only change`（缺口补齐规则 + 负向测试）
- Owner Docs: `docs/design/roles-and-permissions.md` §数据权限（列域分类覆盖矩阵）；`docs/testing/e2e-runbook.md`（负向原语）；`docs/testing/permissions-enforcement-dry-run-impact.md`（data-auth 边界）
- Skill Selection Basis: `nop-testing`（分类审计 + 跨用户越权不可见负向测试 + 后端/E2E Proof）；`nop-backend-dev`（data-auth 层缺口补齐规则，auth/permissions plan-first 区域证据齐备）

## Infrastructure And Config Prereqs

- **%test data-auth 双层开关已 ON**（E2.1 done）：无需新增 config 变量。
- **E2E runner**：`-Dquarkus.profile=test`（P2.2a done）。
- **负向原语 + 账号池就绪**（P2.3/P2.2b done）；跨用户负向须 2 个不同 userId 账号（E2.2 整数 userId 账号机制，可复用/自建）。
- 无外部端口/密钥/.env 依赖（既有 baseline）。

## Execution Plan

### Phase 1 - 分类审计：全 19 域行级规则覆盖矩阵 + 缺口识别

Status: planned
Targets: 各域 `erp-*.data-auth.xml` + `app.data-auth.xml`；各域 orm.xml（列域实证）；`docs/testing/permissions-enforcement-dry-run-impact.md`
Skill: `nop-testing`

- Item Types: `Proof | Decision`
- Prereqs: E2.1 done（data-auth ON 基线）

- [ ] `Proof`: 枚举全 19 域行级规则覆盖矩阵——逐域实测 `erp-*.data-auth.xml`（规则内容）+ `app.data-auth.xml`（聚合状态）+ orm.xml（过滤列域类型），分类为：userId 域（已激活）/ employee-id 域（E2.2 默认方案）/ 全见设计决定（finance 空 `<objs/>`）/ 无列 inert stub。落盘覆盖矩阵到 `docs/testing/permissions-enforcement-dry-run-impact.md` §E2.3。
  - Skill: none
- [ ] `Decision`: 据覆盖矩阵识别 data-auth 层缺口——(a) owner doc 语义要求行级过滤但无规则/未聚合的域；(b) 规则存在但 roleId 与冻结词表不符（同 E2.1 sal「业务员」缺陷模式）；(c) 规则 EL 表达式错误（`${$context.user.userId}` 误用）。逐项分类为「本计划补齐」vs「successor（action-auth 层 / ORM 扩展）」。记录裁决 + 替代方案。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 覆盖矩阵产出（19 域 × 列域分类 × 激活状态 × 缺口标记），落盘 dry-run-impact §E2.3。
- [ ] data-auth 层缺口识别完成，逐项裁决补齐 vs successor。

### Phase 2 - 缺口补齐 + 跨用户越权不可见深度负向测试

Status: planned
Targets: 审计识别的 `erp-*.data-auth.xml` / `app.data-auth.xml`；`tests/e2e/negative/`；后端测试
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1 done（覆盖矩阵 + 缺口裁决）

- [ ] `Fix`/`Add`: 补齐 Phase 1 识别的 data-auth 层缺口（规则补全 / 聚合补全 / roleId 词表修正 / EL 表达式修正）。**仅 data-auth 层**；action-auth 层缺口登记 successor。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 跨用户越权不可见深度负向 spec（`tests/e2e/negative/e2-3-cross-user-row-isolation.spec.ts`）：账号 A（userId_A）创建数据 → 账号 B（userId_B，不同整数 userId）登录查询 → 断言 A 的行 `expectRowsHidden`（越权行 absent，非 error）+ B 自己的行 `expectRowsVisible`。覆盖 sal（createdBy）+ E2.2 已落地的 employee-id 域（若 E2.2 已执行；未执行则自建整数 userId 账号同款机制）。action-auth 门控维降级后端 Proof。
  - Skill: `nop-testing`
- [ ] `Proof`: 跨用户负向 spec 已就绪（flux 引擎，data-auth ON %test）；后端 Proof（`TestErpRoleRowFilterIsolation` 跨用户变体）双重绿。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] data-auth 层缺口补齐落地（Phase 1 识别项）；跨用户越权不可见负向 spec 已就绪（或 action-auth 门控维降级后端 Proof 双重绿）。

### Phase 3 - owner doc + 日志

Status: planned
Targets: `docs/design/roles-and-permissions.md`; `docs/testing/permissions-enforcement-dry-run-impact.md`; `docs/logs/2026/08-11.md`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 2 done

- [ ] `Add`: owner doc 更新——`roles-and-permissions.md` §数据权限「过滤列与列域分类」增 E2.3 全 19 域覆盖矩阵引用 + 缺口补齐终态 + 跨用户越权不可见 Proof 落地注记。
  - Skill: none
- [ ] `Add`: `docs/testing/permissions-enforcement-dry-run-impact.md` §E2.3 完整结果（覆盖矩阵 + 缺口清单 + 补齐终态 + 跨用户负向 Proof + action-auth 门控 successor 登记）。
  - Skill: none
- [ ] `Add`: `docs/logs/2026/08-11.md` 聚合日志条目（E2.3 审计 + 缺口补齐 + 跨用户负向 Proof + 验证状态）。
  - Skill: none

Exit Criteria:

- [ ] owner doc + dry-run-impact §E2.3 + 日志已更新。

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_013455c6bffeylYi0OpTKOqUQn`，fresh-session general 子代理，未起草本计划）— 全 checklist A-K PASS。零信任基线核验全 VERIFIED：E2.1 %test 四开关 ON（application.yaml L62-68）/ sales createdBy 规则 ×6 实体 roleId=销售员（已修正）/ qa ownerId 规则 ErpQaRiskRegister / mnt+inv+pur+hr+fin+mfg data-auth.xml 全 inert `<objs/>` / app.data-auth.xml 仅聚合 sal+qa / finance 空 `<objs/>` 全见设计决定 / 负向原语 expectRowsHidden/expectRowsVisible/loginAsRole+ROLE_ACCOUNTS 就绪 / 19 域 data-auth.xml 文件数 / owner doc §数据权限列域分类与计划一致。**关键裁决核验**：E2.2 正确建模为非硬 Dep（reuse-or-self-build per roadmap iteration-3 frozen wording，非阻塞）；rule 13（不可降级）受尊重——data-auth 层实时缺陷归 Phase 2 Fix（非 Follow-up），action-auth 层缺口正确路由到不同层 successor（非 data-auth 缺陷降级）。0 blocker / 0 major / 3 minor（信息性）：(m1) Current Baseline inert-stub 域列表用省略号，完整矩阵正确 deferred 到 Phase 1；(m2) sal「业务员」缺陷模式参照可加「(已修正)」限定；(m3) qa/mnt orm 行号引用未独立核验（非 load-bearing）。共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [ ] 范围内行为完成（分类审计覆盖矩阵 + data-auth 层缺口补齐 + 跨用户越权不可见负向 Proof）
- [ ] 相关文档对齐（roles-and-permissions §数据权限 + dry-run-impact §E2.3）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（data-auth 范围全绿）+ `bash docs/audits/nop-compliance-checker.sh`（零漂移）+ E2E 跨用户负向 spec 已就绪（flux 引擎，data-auth ON %test）
- [ ] 无范围内项目降级为 deferred/follow-up（data-auth 层识别缺口为审计产物，已确认的实时缺陷/契约漂移不可降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### dept 树行级过滤

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: successor（roadmap Non-Goal；触发条件 = 部门级数据可见需求）。
- Successor Required: yes（触发条件 = 部门级数据可见需求出现）

### action-auth 层缺口补齐（质检员/维护人员 query 授权等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: action-auth 层结果面（roadmap 无对应工作项）；E2.3 仅 data-auth 层。门控维 E2E proof 降级后端 Proof + 登记。
- Successor Required: yes（触发条件 = 门控维 E2E row-filter proof 需解锁 action-auth 前置）

### ORM ErpMdEmployee.userId 扩展（通用 user→employee 解析）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: E2.2 默认等效方案已覆盖；通用解析 ask-first successor。
- Successor Required: yes（触发条件 = 多用户/多员工场景需通用解析）

### prod data-auth 翻转

- Classification: `watch-only residual`
- Why Not Blocking Closure: %prod 保持 OFF（安全姿态）。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <待执行后填写>

Follow-up:

- dept 树行级过滤（successor）
- action-auth 层缺口补齐（successor）
- ORM ErpMdEmployee.userId 扩展（ask-first successor）
- prod data-auth 翻转（successor）
