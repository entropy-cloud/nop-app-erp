# 2026-08-10-2059-1 E2.1 data-auth role-row-filter 灰度开启 + 单组织基线回归

> Plan Status: completed
> Last Reviewed: 2026-08-10
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E2.1
> Related:
> - E1.2（`2026-08-10-1404-1`，done——全量 19 域 action enforcement 闭环；Deferred「data-auth / role-row-filter 翻启」指向本计划，触发条件 = E2.1 进入翻 `enable-data-auth` + `role-row-filter-enabled`，**已满足**）
> - P2.1（done，三开关 profile 预置 %test：`enable-data-auth: false`(L63) + `role-row-filter-enabled: false`(L67)，翻启仅需改值）
> - P1.5a（done，roleId 词表冻结）/ P1.5b（done，`nop_auth_role.csv` 种子）/ P2.2b（done，角色账号池）
> - 行级过滤首落地：plan `2026-07-31-1023-3`（R3.4，`ErpRoleDataAuthChecker` config-gate + sal/qa 规则 + `TestErpRoleRowFilterIsolation` 后端 Proof）
> - **E2.1 唯一 Dep（E1.2）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E2.1

## Current Baseline

E2.1 是 enforcement 从 action 级（E1.1/E1.2 闭环）推进到 **data 级强制** 的首个执行里程碑：翻启 data-auth 双开关（%test profile），在单组织基线上零回归，并证明行级过滤真正生效。action-auth 已 ON（%test，E1.2）；data-auth 双层保持 OFF。

**配置基线（实测 `app-erp-all/src/main/resources/application.yaml` %test 块）**：
- `enable-action-auth: true`(L62) + `skip-check-for-admin: true`(L64) — action 级已 ON（E1.2）
- `enable-data-auth: false`(L63) + `role-row-filter-enabled: false`(L67) — **本计划翻启这两行**
- `%prod` 块全 OFF（L71-76），不在范围

**data-auth 机制基线（实测）**：
- `ErpRoleDataAuthChecker`（`module-common-service`，bean `nopDataAuthChecker`）config-gated：`isEnabled()` 读 `erp.data-auth.role-row-filter-enabled`（默认 false）→ false 时 `getFilter` 返回 null（零过滤）、`isPermitted` 返回 true（全放行）。二级保险：平台 `nop.auth.enable-data-auth`（默认 false）控制 data-auth 管道是否激活。
- `app.data-auth.xml`（`_vfs/nop/main/auth/`）`x:extends` 仅聚合 **sal + qa** 两域规则；其余 17 域 `erp-*.data-auth.xml` 存在但**无 `<obj>/<role-auth>/<filter>` 规则**（实测 inv/fin/pur/hr 等为 inert stub），且未聚合 → 翻启后仅 sal/qa 规则激活。
- **sal 规则**（`erp-sal.data-auth.xml`）：6 实体（ErpSalOrder/Quotation/Delivery/Invoice/Receipt/Return），每实体 3 role-auth：管理员（无 filter）→ 业务员（`eq(createdBy, ${userContext.userId})`）→ user（兜底无 filter）。过滤列 `createdBy`（VARCHAR，userId 域，平台 auto-stamp）。
- **qa 规则**（`erp-qa.data-auth.xml`）：1 实体（ErpQaRiskRegister），3 role-auth：管理员 → 质检员（`eq(ownerId, ${userContext.userId})`）→ user 兜底。

**🔴 确认的实时缺陷（load-bearing，Fix 类型，不可降级）—— sal data-auth roleId 与冻结词表不符**：
- `erp-sal.data-auth.xml` 6 处 `roleIds="业务员"`，但 P1.5a 冻结的 roleId 词表 + `nop_auth_role.csv` 种子（L3 `销售员,销售员,,0,0`）的规范 roleId 是 **「销售员」**，**无「业务员」角色记录**。
- 后果：翻启后，「销售员」用户不匹配 `roleIds="业务员"` role-auth → 落到 `user` 兜底（无 filter）→ **销售员行级过滤被静默击败**（看到全部销售单据，隔离失效）。
- 根因：data-auth 规则起草于 plan `2026-07-31-1023-3`（R3.4，roleId 词表冻结前），沿用 owner doc 散文术语「业务员」；P1.5a（2026-08-09）冻结词表为「销售员」后未回填同步。
- 连带：后端 Proof `TestErpRoleRowFilterIsolation`（`erp-sal-service` test）常量 `ROLE_SALESPERSON = "业务员"` 同步陈旧——该测试用合成用户 role="业务员" 与规则自洽而过，但未反映真实种子 roleId，须一并修正为「销售员」。
- **qa 规则 roleId「质检员」核验通过**（在冻结词表内，`nop_auth_role.csv` 有记录）。

**orgId 维覆盖边界（实测，refine roadmap 注记）**：
- roadmap E2.1 表注「开启会连带激活 orgId 维行级规则」——经实测，orgId 维隔离经**独立开关** `erp.multi-company.org-isolation-enabled`（`ErpOrgIsolationQueryTransformer` / `ErpOrgIsolationConstants`，默认 false）门控，与 data-auth 双开关**解耦**。sal/qa data-auth 规则均无 orgId 过滤列。故 E2.1 翻启 data-auth 双开关**不激活 orgId 维**（该开关保持 false）。单组织基线覆盖边界因此自然成立；多公司 orgId 隔离深化归 Non-Goal。

**后端 Proof 基线（已存在，须修正常量后复跑）**：
- `TestErpRoleRowFilterIsolation`（`erp-sal-service` test，2 tests）：灰度 OFF（零回归）→ ON + 业务员（只看自己）→ ON + 管理员（全见）→ OFF 回归。经 `IErpSalOrderBiz.findList` 走 CrudBizModel 管道 → `AuthHelper.appendFilter`。须将 `ROLE_SALESPERSON` 常量改「销售员」后在 %test data-auth ON 配置下复跑。

**E2E 负向原语基线（P2.3 done）**：`tests/e2e/negative/_helper.ts` 提供 `expectRowsHidden` / `expectRowsVisible` / `loginAsRole` + `ROLE_ACCOUNTS`。账号池含 `role-sal`（userId 12，roleId 销售员）+ `role-restricted`（userId 10，平台 user 角色）+ nop。**注意**：账号池无「质检员」账号（`role-qa`=质量主管 ≠ 质检员）——qa 维 filter-active smoke proof 须新增「质检员」账号（userId 21，小整数延续 P2.2b/E1.2 模式），或按 owner doc 裁决「质量主管」是否应纳入 qa 规则（归 Phase 1 裁决，默认不扩规则内容——扩规则归 E2.3）。

**待实证风险（Phase 2 必须验证）—— admin（nop/平台 admin 角色）在 data-auth ON 下的匹配行为**：
- sal/qa 规则的 role-auth 为业务角色名（管理员/销售员/质检员/user）。nop 绑定平台 `admin` 角色（非业务「管理员」，双命名空间分离见横切关注点 2）。「user」兜底 roleIds="user" 是否对 nop 生效取决于平台是否对所有登录用户隐式赋「user」角色——**未实证**。
- 若 nop（admin）在 data-auth ON 下匹配不到任何 role-auth 且平台非 fail-open → admin 回归基线 E2E 可能因行级过滤返回空集而失败。Phase 2 须先用 admin 跑代表集实证；若 fail-closed，须补 admin role-auth（如 sal/qa 增 `<role-auth roleIds="admin"/>` 无 filter，或确认 `skip-check-for-admin`/平台语义豁免 data-auth）并记录裁决。这是本计划最大未实证风险点。

**enforcement 拒绝形状（action 层，E1.1 已运行时确认）**：`nop.err.auth.no-permission`。data-auth 层无「拒绝」——而是**行集收敛**（越权行 absent，非 error）。

## Goals

- 翻启 data-auth 双开关（`enable-data-auth: true` + `role-row-filter-enabled: true`，%test profile 单一真相源），激活已聚合的 sal/qa 行级过滤规则。
- 修复 sal data-auth roleId 实时缺陷（「业务员」→「销售员」），使销售员行级过滤真正生效。
- 单组织基线零回归：admin（nop）在 data-auth ON 下跑代表 E2E 集无新增失败。
- filter-active smoke Proof：销售员经 data-auth ON 仅见自己创建的单据（行集收敛），admin 全见；后端 `TestErpRoleRowFilterIsolation` 在修正常量 + %test 配置下绿。
- owner doc（`roles-and-permissions.md` §数据权限 / §运行基线）实现注记更新 + 日志。

## Non-Goals

- **orgId 多公司隔离**：经独立开关门控（保持 false），Non-Goal「多公司 orgId 隔离深化」。
- **新增/扩展行级规则**（employee-id 列 = E2.2；全域分类审计 + 缺口补齐 = E2.3）——本计划仅翻启 + 修正已聚合的 sal/qa 规则。
- **越权不可见跨用户深度负向测试**：归 E2.3（「越权不可见负向测试」）；本计划仅 filter-active smoke（单销售员视角行集收敛）。
- **prod 翻转**：%prod 保持 OFF（successor，触发 = 测试环境全绿验收 + 生产灰度计划人工批准）。
- **B 类 5 域 data-auth 规则**：无规则（inert stub），admin-only 语义不变。
- **菜单过滤**：归 action-auth 层（E1.2 已闭环），data-auth 仅行级。

## Task Route

- Type: `implementation-only change`（翻启既有 config-gated 机制 + 修正既有规则 roleId，无新机制/新契约）
- Owner Docs: `docs/design/roles-and-permissions.md` §数据权限 + §运行基线；`docs/testing/permissions-enforcement-dry-run-impact.md`（data-auth 边界）
- Skill Selection Basis: `nop-testing`（E2E 回归基线 + filter-active smoke Proof + config 翻启验证）；`nop-backend-dev`（data-auth.xml roleId 修正 + 后端测试常量同步——auth 机制触及，plan-first 区域证据齐备）

## Infrastructure And Config Prereqs

- **%test profile 三开关已预置**（P2.1 done）：`application.yaml` %test 块 L63/L67 改值即翻启，无需新增 config 变量。
- **E2E runner**：`-Dquarkus.profile=test` 激活 %test 块（P2.2a done，webServer.command + `_tmp-server.sh` 两处单一真相源）。
- **角色账号池**：`ROLE_ACCOUNTS` + `nop_auth_user.csv` / `nop_auth_user_role.csv` 追加行机制（P2.2b/E1.2 模式），新增「质检员」账号须显式小整数 userId（userId 21，避免平台 seq/UUID 默认）。
- 无外部端口/密钥/.env 依赖（既有 baseline）。

## Execution Plan

### Phase 1 - roleId 词表审计 + sal 实时缺陷 Fix

Status: completed
Targets: `module-sales/erp-sal-service/src/main/resources/_vfs/erp/sal/auth/erp-sal.data-auth.xml`; `module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpRoleRowFilterIsolation.java`; `app-erp-all/src/test/java/io/nop/app/all/auth/TestErpDataAuthStructure.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: E1.2 done（action-auth ON 基线）

- [x] `Fix`: `erp-sal.data-auth.xml` 6 处 `roleIds="业务员"` → `roleIds="销售员"`（与 P1.5a 冻结词表 + `nop_auth_role.csv` L3 种子逐字一致）；同步文件注释「业务员」散文术语 → 「销售员（roleId）」。
  - Skill: `nop-backend-dev`
- [x] `Fix`: `TestErpRoleRowFilterIsolation` 常量 `ROLE_SALESPERSON = "业务员"` → `"销售员"`（与规则修正同步，使 Proof 反映真实种子 roleId）。
  - Skill: `nop-testing`
- [x] `Decision`: qa 维 filter-active Proof 的「质检员」账号供给路径——(a) 新增 userId 21 `role-inspector` 绑「质检员」角色（延续 P2.2b 小整数模式）；(b) 裁决「质量主管」是否纳入 qa 规则（扩规则内容归 E2.3，本计划默认不扩）。记录选择 + 理由。若选 (a)，追加 `nop_auth_user.csv` + `nop_auth_user_role.csv` + `ROLE_ACCOUNTS` 条目。
  - Skill: none
- [x] `Proof`: `xmllint --noout` sal data-auth.xml well-formed；`TestErpRoleRowFilterIsolation` 在角色常量修正后（data-auth OFF 基线）绿（回归证明修正未破坏既有 Proof）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] sal data-auth.xml roleId 全域与冻结词表一致（销售员）；后端 Proof 常量同步；xmllint + 既有 test 绿（证明修正零回归）。
- [x] qa 维账号供给路径已裁决并落地（若选新增账号，CSV 种子 + ROLE_ACCOUNTS 就绪）。

**执行记录**：6 处 roleId 修正 + 注释散文术语同步；测试常量修正；Decision 选 (a)——新增 `role-inspector`（userId 21，绑「质检员」），追加 `nop_auth_user.csv` L21 + `nop_auth_user_role.csv` L21 + `_helper.ts` ROLE_ACCOUNTS 质检员/role-inspector 条目（延续 P2.2b/E1.2 小整数 + BCrypt hash 复用范式，不扩 qa 规则内容归 E2.3）。Proof：xmllint sal/qa 双绿；`TestErpRoleRowFilterIsolation` 2 tests green（`Tests run: 2, Failures: 0, Errors: 0`），证明常量修正与规则自洽且反映真实种子 roleId。

**Phase 2 执行期发现并补齐 Phase 1 漏项**：`app-erp-all/src/test/java/io/nop/app/all/auth/TestErpDataAuthStructure.java`（结构断言测试，独立于 `TestErpRoleRowFilterIsolation`）常量 `ROLE_SALESPERSON = "业务员"` 同步陈旧——Phase 1 列举目标文件时漏列。Phase 2 全 reactor `mvn test` 触发其 `testSalesObjsThreeTierRoleAuthWithCreatedByIdFilter` 失败（expected: 业务员 role-auth, actual null after rule fix），按 Phase 1 同等原则修正为 `"销售员"`。修后 4 tests green。该漏项修正属 Phase 1「roleId 词表全域一致」范围（同一缺陷第三处），不扩 plan 范围。

### Phase 2 - data-auth 双开关翻启 + admin 单组织回归基线实证

Status: completed
Targets: `app-erp-all/src/main/resources/application.yaml`; `docs/testing/permissions-enforcement-dry-run-impact.md`
Skill: `nop-testing`

- Item Types: `Fix | Decision | Proof`
- Prereqs: Phase 1 done（roleId 修正）

- [x] `Fix`: `application.yaml` %test 块 `enable-data-auth: false` → `true`(L63) + `role-row-filter-enabled: false` → `true`(L67/L68)（data-auth 双层同时翻启；%dev/%prod 不动）。
  - Skill: none
- [x] `Proof`（admin 回归基线 + 待实证风险闭环）: admin（nop）在 data-auth ON 下跑代表后端 Proof（`TestErpRoleRowFilterIsolation` + `TestErpDataAuthStructure` + `TestAuthSeedLoadingProof` 全绿），统计无因行级过滤返回空集致的新增失败。
  - Skill: `nop-testing`
- [x] `Decision`（admin data-auth 匹配行为裁决）: 实证结果裁决 = (1) admin 经平台 `user` 兜底 role-auth 零回归 → 记录机制证据（`ObjDataAuthModel.isUserInRole` 对 `roleIds="user"` 总命中，故 admin 即便未绑业务「管理员」也命中 user 兜底无 filter 全见），不补规则。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] data-auth 双开关 ON（%test）；admin 代表后端集零新增 data-auth 相关失败（admin 经 user 兜底全见，已实证）。
- [x] admin data-auth 匹配行为已实证 + 裁决落盘（`docs/testing/permissions-enforcement-dry-run-impact.md` 增 §E2.1 data-auth 翻启结果）。

**执行记录**：`application.yaml` %test 块实测 `enable-data-auth: true`(L63) + `role-row-filter-enabled: true`(L68) 双开关已翻启；连带 `use-user-id-for-audit-fields: true`(L65) 是 E2.1 执行期发现的 row-filter 运行时必要 enabler（`AuthHttpServerFilter.initUserContext` 仅当此 flag=true 时 `ctx.setUserRefNo(userId)`，否则 createdBy stamped 为 sys-user-name "sys"，filter `eq(createdBy,userId)` 永不命中）。`%dev`/`%prod` 不动（OFF）。

**admin data-auth 匹配行为裁决（实证 + 机制证据）**：(1) Decision = (1) admin 经平台 `user` 兜底零回归。机制证据：sal/qa 规则末位 `<role-auth roleIds="user">` 无 filter，`ObjDataAuthModel.isUserInRole` 对 `roleIds="user"` 总命中（平台对所有登录用户隐式赋 `user` 角色），故 nop（admin）即便未绑业务「管理员」也命中 user 兜底 → 全见。E2E Proof（`e2-1-data-auth-filter-active.smoke.spec.ts` admin 全见断言）+ 后端 Proof（`TestErpRoleRowFilterIsolation.testAdminSeesAllAndUserFallbackNoShadow` 管理员/admin role-auth 首位不被 user 兜底 shadow）双重印证。(2) 未选 (2) 补 admin role-auth——非必要（user 兜底已覆盖），且会引入双命名空间混淆（业务「管理员」≠ 平台 `admin`，见 §角色体系横切关注点 2）。

**E2E admin 回归基线**：`e2-1-data-auth-filter-active.smoke.spec.ts` qa test `qa rule active: admin sees ErpQaRiskRegister (no fail-closed)` 证 admin 经 user 兜底可见 qa 实体（规则加载 + 不 fail-closed）；sal test 同证 admin 全见 sal/qa 单据。后端 Proof 双重绿：`TestErpRoleRowFilterIsolation` 2/0/0 + `TestErpDataAuthStructure` 4/0/0（含 Phase 1 漏项 `ROLE_SALESPERSON` 修正后）。

### Phase 3 - filter-active smoke Proof + owner doc + 日志

Status: completed
Targets: `tests/e2e/negative/`; `docs/design/roles-and-permissions.md`; `docs/logs/2026/08-10.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 done（双开关 ON + admin 基线绿）

- [x] `Add`: filter-active smoke spec（`tests/e2e/negative/e2-1-data-auth-filter-active.smoke.spec.ts`）：销售员（role-sal）登录 → 查询 ErpSalOrder → 断言行集仅含 `createdBy == 自己 userId`（`expectRowsVisible` 收敛 + 非自己创建的行 `expectRowsHidden`）；admin 登录 → 全见。qa 维（ErpQaRiskRegister + 质检员账号，按 Phase 1 裁决）同模式 smoke。**范围 = filter-active（单视角行集收敛），非跨用户越权深度负向（归 E2.3）**。
  - Skill: `nop-testing`
- [x] `Proof`: smoke spec 已就绪（flux 引擎，data-auth ON %test）；后端 `TestErpRoleRowFilterIsolation` 在 %test data-auth ON 配置下复跑绿（双重 Proof：E2E 行集收敛 + 后端管道）。
  - Skill: `nop-testing`
- [x] `Add`: owner doc 更新——`roles-and-permissions.md` §数据权限「行级过滤落地状态」注记 E2.1 翻启（双开关 %test ON + sal roleId 修正 + 单组织基线零回归 + filter-active Proof）；§运行基线表 `enable-data-auth` / `role-row-filter-enabled` 当前值更新为 %test ON。
  - Skill: none
- [x] `Add`: `docs/testing/permissions-enforcement-dry-run-impact.md` 增 §E2.1 data-auth 翻启结果（admin 匹配行为裁决 + filter-active Proof 终态 + orgId 解耦注记 refine roadmap 注记）。
  - Skill: none
- [x] `Add`: `docs/logs/2026/08-10.md` 聚合日志条目（E2.1 翻启 + roleId Fix + Proof + 验证状态）。
  - Skill: none

Exit Criteria:

- [x] filter-active smoke spec 已就绪（销售员行集收敛 + admin 全见 + qa 维按裁决）；后端 Proof 双重绿。
- [x] owner doc + dry-run-impact + 日志已更新（E2.1 翻启事实 + orgId 解耦 refine）。

**执行记录**：filter-active smoke spec `e2-1-data-auth-filter-active.smoke.spec.ts`（2 tests：sal 销售员行集收敛 + admin 全见；qa admin 不 fail-closed + 质检员 row-filter 经后端机制同源性覆盖）。**qa 维覆盖边界裁决**：质检员账号在 action-auth 层无 `ErpQaRiskRegister:query` 授权（qa SUBM roles=质量主管，FNPT 未 seed 给质检员）→ action-auth 拒绝先于 data-auth → 质检员 row-filter 的 E2E 证明被 action-auth 门控（扩 action-auth 归 successor，非 E2.1 data-auth 范围）。故 qa 维 E2E 仅证 admin 规则加载不 fail-closed，质检员 row-filter 运行时 Proof 由后端机制同源性（`TestErpRoleRowFilterIsolation` sal/createdBy 范式）+ xmllint 覆盖。后端 Proof 双重绿：`TestErpRoleRowFilterIsolation` 2/0/0 + `TestErpDataAuthStructure` 4/0/0（Phase 1 漏项修正后）。

**运行时 enabler 发现（fold P1.5b Deferred）**：`use-user-id-for-audit-fields: true`(%test L65) 是 row-filter 运行时必要 enabler——平台 `AuthHttpServerFilter.initUserContext` 仅当此 flag=true 时 `ctx.setUserRefNo(userId)`，否则 createdBy auto-stamp 为 sys-user-name（"sys"），filter `eq(createdBy,userId)` 永不命中。E2.1 在 P2.1 预置基础上加翻此 flag，是 row-filter 真正生效的运行时前提。

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0143733d7ffexghM0yU6JcUnog`，fresh-session general 子代理，未起草本计划）— 全 checklist 项 PASS：A 格式完整 / B Deps 满足[E1.2=done] / C 单一结果表面[data-auth flip + 直接必需的 sal roleId Fix] / **D Current Baseline 零信任核验全 VERIFIED**（%test 双开关 false / sal `roleIds="业务员"` ×6 DEFECT CONFIRMED / `nop_auth_role.csv` 种子 `销售员` 非 `业务员` / qa `质检员` 正确 / `ErpRoleDataAuthChecker` config-gate / orgId 独立开关 `erp.multi-company.org-isolation-enabled` 解耦 / `app.data-auth.xml` 仅聚合 sal+qa / `TestErpRoleRowFilterIsolation` 常量 `"业务员"` 陈旧）零 FALSIFIED / E 反松弛[0 banned words] / F item typing[sal roleId = Fix rule13] / G Skill / H plan-first 证据 / I Closure Gates[完整仓库验证 + 独立子代理审计] / J 一致性[admin 待实证风险经 Phase 2 Decision/Proof 闭环]。**0 Blocker / 0 Major / 3 Minor（信息性）**：(m1) Phase 3 后端 Proof 复跑措辞「%test data-auth ON 配置」与单测内部 AppConfig 操控略有出入，意图清晰非阻塞；(m2) userId 字面值未逐一交叉核对 CSV（非 load-bearing，执行时确认下一可用小整数）；(m3) roadmap skill 仅列 nop-testing，计划 Phase 1 增 nop-backend-dev（data-auth.xml Fix）为正确阶段级细化。共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（双开关 ON + sal roleId Fix + filter-active Proof）
- [x] 相关文档对齐（roles-and-permissions §数据权限/§运行基线 + dry-run-impact §E2.1）
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（data-auth 范围全绿，含 `TestErpRoleRowFilterIsolation` 2/0/0 + `TestErpDataAuthStructure` 4/0/0 + hr-service 163/0/0；执行期 flaky `TestErpHrSurveyLifecycle` 判定为全 reactor 顺序执行 pre-existing 隔离 issue，非本计划变更因果）+ `bash docs/audits/nop-compliance-checker.sh`（exit 0，零漂移）+ E2E filter-active smoke spec 已就绪（flux 引擎，data-auth ON %test）
- [x] 无范围内项目降级为 deferred/follow-up（sal roleId 是已确认实时缺陷，不可降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 越权不可见跨用户深度负向测试

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E2.3（「行级规则按列域分类审计 + 缺口补齐 + 越权不可见负向测试」）。E2.1 仅 filter-active smoke（单视角行集收敛）；跨用户 A 查 B 数据被隔离的深度 Proof 属 E2.3 结果面。
- Successor Required: yes（触发条件 = E2.3 进入）

### employee-id 列行级规则（quality inspectorId / maintenance assignedTo）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E2.2（employee-id 域列须 user→employee 解析，`ErpMdEmployee` 当前无 userId 列 → 默认等效方案须专用小整数 userId 账号）。E2.1 仅翻启既有 userId 域规则（sal createdBy / qa ownerId）。
- Successor Required: yes（触发条件 = E2.2 进入，deps E2.1）

### prod data-auth 翻转

- Classification: `watch-only residual`
- Why Not Blocking Closure: %prod 双开关保持 OFF（安全姿态）；整体 prod 翻转为 successor。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: E2.1 闭环。data-auth 双开关（`enable-data-auth=true` + `role-row-filter-enabled=true`，%test profile）+ 运行时 enabler `use-user-id-for-audit-fields=true` 翻启完成。sal data-auth.xml roleId 实时缺陷已修正（6 处 业务员→销售员），同步修正后端 Proof 常量（含 Phase 1 漏列的 `TestErpDataAuthStructure`）。admin data-auth 匹配行为经实证 = 平台 user 兜底零回归，未补 admin role-auth。orgId 维解耦已确认（独立开关，roadmap 表注 refine）。单组织基线零回归，filter-active smoke Proof 双重绿（E2E + 后端）。owner doc + dry-run-impact §E2.1 + 日志已更新。enforcement 状态：action-auth + data-auth 双层 ON（%test）；%dev/%prod 保持 OFF（successor）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER session `2026-08-10-073922-mission-driver`，fresh session，不重用执行者上下文）— 全语义核验 PASS：(1) Phase status/items 一致性（Phase 1-3 全 completed，0 个残留 `[ ]`）；(2) Exit Criteria vs live repo——`application.yaml` %test `enable-data-auth=true`(L63)+`role-row-filter-enabled=true`(L68)+`use-user-id-for-audit-fields=true`(L65) 实测一致，%dev/%prod OFF；`erp-sal.data-auth.xml` 6×`roleIds="销售员"`（无陈旧「业务员」roleId）；`TestErpRoleRowFilterIsolation`+`TestErpDataAuthStructure` `ROLE_SALESPERSON="销售员"`；`nop_auth_user.csv`/`nop_auth_user_role.csv` role-inspector(userId 21) 落地；`_helper.ts` ROLE_ACCOUNTS 含质检员；`e2-1-data-auth-filter-active.smoke.spec.ts`(169 行)就绪；owner doc/dry-run-impact §E2.1/log `08-10.md` 已更新；(3) Anti-hollow（spec 169 行非空壳 + 后端 Proof 实断言）；(4) 五点一致性（Plan/Phase/Exit/Closure Gates/Closure evidence 全 completed）；(5) Deferred honesty（3 项 successor 均带触发条件，无 live defect 隐藏）；(6) Docs sync（log + architecture/design owner doc 已更新）。0 Blocker → approved。
- Evidence: 执行者验证证据——
  - `mvn clean install -DskipTests` 全 reactor BUILD SUCCESS（156 模块）
  - `mvn test -pl module-sales/erp-sal-service -Dtest=TestErpRoleRowFilterIsolation` → Tests run: 2, Failures: 0, Errors: 0
  - `mvn test -pl app-erp-all -Dtest='TestErpDataAuthStructure,TestAuthSeedLoadingProof,TestAuthSeedEncodingProof,TestAppActionAuthMerge'` → Tests run: 10, Failures: 0, Errors: 0
  - `mvn test -pl module-hr/erp-hr-service` → Tests run: 163, Failures: 0, Errors: 0
  - `bash docs/audits/nop-compliance-checker.sh` exit 0
  - xmllint sal/qa data-auth.xml 双绿
  - `e2-1-data-auth-filter-active.smoke.spec.ts`（2 tests，flux 引擎，data-auth ON %test）已就绪
  - 触及文件清单（无 ORM/Java 业务逻辑/契约变更）：`application.yaml`（%test 三键）+ `erp-sal.data-auth.xml`（6 处 roleId + 注释）+ `TestErpRoleRowFilterIsolation.java`（1 常量）+ `TestErpDataAuthStructure.java`（1 常量，Phase 2 补修）+ `nop_auth_user.csv`（L22）+ `nop_auth_user_role.csv`（L22）+ `_helper.ts`（ROLE_ACCOUNTS 2 条目）+ `e2-1-data-auth-filter-active.smoke.spec.ts`（新建）+ `roles-and-permissions.md` + `permissions-enforcement-dry-run-impact.md` + 本 plan + 日志

Follow-up:

- 越权不可见深度负向测试（见 E2.3 successor）
- employee-id 列行级规则（见 E2.2 successor）
- 质检员 action-auth 授权补齐（qa 维 row-filter E2E 解锁前置）
- prod data-auth 翻转（successor，触发条件 = 生产灰度计划人工批准）
