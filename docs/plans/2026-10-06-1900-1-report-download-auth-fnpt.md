# 2026-10-06-1900-1 报表下载 auth 资源收口（WI-01 / F-R19）

> Plan Status: active
> Last Reviewed: 2026-10-06
> Source: `docs/backlog/successor-followup-closure-roadmap.md` WI-01（F-R19 预存失败家族）+ 根因报告（2026-10-06 独立调查子代理，四步确认链）
> Related: `docs/design/roles-and-permissions.md`（auth/permissions owner doc——保护区 plan-first）、`docs/backlog/successor-followup-closure-roadmap.md` WI-02（正交的收件箱家族；**执行期协调**：WI-02 探针备选并入 menu-filter 套件与本计划 negative 回归门同文件面——后提交者合并对齐）
> Audit: required
> **Protected Area**: auth/permissions（`erp-*.action-auth.xml` 资源注册）——plan-first + owner doc 对齐；本计划为静态 VFS 资源变更（非代码），按 autonomy 政策 auth/permissions=plan-first 档执行（计划审计 + owner doc 证据）

## Current Baseline（确认记录，2026-10-06 HEAD 实证）

1. **根因（三选一钉死：action-auth 资源缺口）**：`/p/{Biz}__download` 权限链 = QuarkusGraphQLWebService → ActionAuthMeta 恒定派生（`ReflectionBizModelBuilder.java:360-366`，无 @Auth 时 permission=`{biz}:{opType}|{biz}:{action}`）→ `DefaultActionAuthChecker`（①skip-check-for-admin 对 admin/nop-admin 直放；②否则 SiteCacheData `permissionToRoles` 判定，空集 fail-closed）。四个报表 BizModel（ErpFin/Md/Mfg/CsReport）为**非实体纯 BizModel，codegen 永不生成资源点**；手写 overlay 只注册菜单 SUBM（无 FNPT 子节点/permissions）→ `permissionToRoles` 无 `ErpFinReport:query` 键 → 全部非 admin 账号 403 信封 `nop.err.auth.no-permission`。
2. **实证探针（执行者，应用 8011 在线）**：admin（nop）四域 200+XLSX binary；role-finance（财务员）/role-mfg-lead（生产主管）/role-biz-admin（业务管理员）四域统一 93B JSON 信封。**roadmap 红灯前提「fin 分支预期绿作对照组」被证伪**——四域全红（fin/md 通过假象 = E2E global-setup 默认 admin 上下文 + skip-check 恒放行；首轮 mfg 双挂/cs 波动为运行时层失败，与 auth 正交）。红灯预期修正：新 auth spec 四域全红→修后全绿。
3. **加重项（%prod）**：`%prod` skip-check-for-admin=false（application.yaml :105 注记）→ 报表下载在 %prod 对**含 admin 在内一切账号** fail-closed（%dev enable-action-auth=false 整链不注入，开发态从不暴露）。USC-02a 已为 NopAuthOpLog 补 roles="admin" 窄种子先例，报表面无同类处理。
4. **修复形状（根因报告 §四）**：四域手写 overlay（`x:extends` 生成基，非生成文件）报表分组下各加一个 FNPT 节点带 `<permissions>ErpXxxReport:query</permissions>`——**用 `:query` 桶**（报表页数据源走同对象其它 @BizQuery，每方法默认权限含 `:query` OR 组；先例 USC-02b 读桶窄种子 + `FNPT:NopAuthOpLog:query`）。硬约束：TOPM/SUBM 不得携带 permissions=（P1.5a 不变量）；roles 逗号分隔 csv-set（E1.2 教训）；不改 `_erp-*.action-auth.xml` 生成文件。
5. **正交项**：md partner-list UI 层失败（admin 上下文可复现，页面特定运行时问题）与本 auth 家族正交——随本计划实施期单独诊断（见 Phase 2）。

## 根因报告（自包含——审查 F2 修订；调查者=独立子代理 fresh session 2026-10-06）

**资源解析链**：`POST /p/{Biz}__download` → QuarkusGraphQLWebService（`@Path("/p/{query}")`，按 `__` 拆 operation）→ GraphQL operation=`{Biz}/download` → ActionAuthMeta 恒定派生（`ReflectionBizModelBuilder.java:364-365`：无 @Auth 时 permission=`{biz}:{opType}|{biz}:{action}`，MultiCsvSet `|` 分 OR 组）→ `GraphQLActionAuthChecker.isAllowAccess`（enableActionAuth=true 才注入）→ `DefaultActionAuthChecker.isPermitted`（①skip-check-for-admin 且 user∈{admin,nop-admin} 直放；②否则 SiteCacheData `permissionToRoles` 判定——键=带 `<permissions>` 的 FNPT 资源 × 角色绑定，空集 fail-closed）→ 不满足抛 `nop.err.auth.no-permission`。

**探针 verbatim（2026-10-06 8011 在线应用，角色账号直连）**：
```
POST /p/ErpFinReport__download {reportName:ar-ap-aging,renderType:xlsx,data:{asOfDate:2026-07-08}}
  Authorization: Bearer <role-finance token> → 200 {"bizFatal":false,"code":"nop.err.auth.no-permission","msg":"没有访问权限","status":-1}（93B）
  同型：role-mfg-lead / role-biz-admin × Erp{Md,Mfg,Cs}Report__download → 全部 93B 同码信封（12/12）
  Authorization: Bearer <nop/admin token> → 200 application/octet-stream len=8488 magic=PK\x03\x04（4/4 域全过）
```

**三选一归因定论（排除法）**：①菜单 FNPT 派生差异——排除（md/mfg 报表 SUBM 确有 roles= 种子，但 cascadeResourceToRoles 只上溯用于菜单可见性过滤、从不进 permissionToRoles；有种子与没种子的 fin/cs 行为完全一致）；②渲染期错误——排除（admin 上下文同请求 200+binary；错误码为 auth 域非渲染域）；③**action-auth 资源缺口——钉死**：四报表 BizModel 为非实体纯 BizModel（codegen 不产资源点），手写 overlay 仅注册菜单 SUBM 无 FNPT/permissions，全仓 action-auth.xml + 种子 grep 报表资源零命中。

**修复形状推导**：FNPT 节点 `<permissions>ErpXxxReport:query</permissions>` 用 `:query` 桶——报表页数据源与下载按钮同对象（balance-sheet.page.yaml 数据源 `@query:ErpFinReport__renderHtml` + 按钮 `/p/ErpFinReport__download`），每方法默认权限 OR 首组均含 `{obj}:query`，一次注册覆盖全部读动作（先例 `FNPT:NopAuthOpLog:query` USC-03 + `FNPT:ErpMfgCrpLoad:calculateLoad`）。owner doc 镜像规则四条：E1.1 根因段（恒定派生+permissionToRoles 判定）/ per-action FNPT 声明层（R2.7 表格式）/ USC-02b `:query` 桶窄种子语义 / P1.5a「TOPM/SUBM 无 permissions=」不变量；roles 逗号分隔 csv-set（E1.2 教训）。

## Goals

- 四域 action-auth overlay 注册 `FNPT:ErpXxxReport:query`（roles 镜像既有菜单种子：fin=财务员、md=该组 13 角色 CSV、mfg=生产主管、cs=管理员〔镜像 reverseApprove 先例；cs 域零业务角色种子为已知缺口 R1-E1-2〕）。
- 红灯 spec `tests/e2e/reports/reports.download.auth.spec.ts`：真实角色账号断言四代表域下载端点 200+binary magic——实施前四域全红（问题存在性），实施后全绿。
- roles-and-permissions.md 增补报表 FNPT 条款（新增条款，遵循 E1.1 根因段/per-action FNPT 声明层/`:query` 桶窄种子/P1.5a 不变量四条既有规则）+ :153 角色映射表行注记。
- md partner-list UI 层失败单独诊断（正交项，Phase 2）。

## Non-Goals

- 不改任何 Java 代码（BizModel 无 @Auth 需求——`:query` 桶已覆盖读动作 OR 组）。
- 不改 `_erp-*.action-auth.xml` 生成文件（保护区永不手写）。
- 不做 %prod admin 角色补种（roles CSV 追加 admin 属部署面裁决，登记 Deferred 触发条件=生产部署阶段；本计划仅 %test 面 + 文档）。
- 不收口 crm/cs 渲染期波动与 mfg crp-load 运行时层失败（首轮诊断已证与 auth 正交且后续复跑已消失/属稳定性面——如复现按独立缺陷立项）。

## Task Route

- Type: `implementation-only change`（auth 静态资源 + E2E spec）——**保护区：auth/permissions，plan-first 已履行（本计划 + 独立草案审计 + owner doc 对齐）**
- Owner Docs: `docs/design/roles-and-permissions.md`（增补条款）
- Skill Selection Basis: `Skill: none`（根因调查已完成，修复为声明式资源注册 + spec；roles-and-permissions 镜像规则由根因报告 §五给出）

## Infrastructure And Config Prereqs

- E2E 基建既有；site-map cache-timeout 1s（静态 VFS 资源随 jar classpath 装载——**重新打包 + 重启**生效，无 codegen）。
- 回滚策略：单提交 revert（4 个 xml overlay + 1 spec + 1 owner doc + 计划）。

## Execution Plan

### Phase 1 — 四域 FNPT 注册 + 红灯 spec

Status: completed（红灯 4/4 verbatim：`Error: XLSX magic (PK zip header) / Expected: "PK" / element(s) not found`——四域角色账号统一下载断言失败留证；FNPT 落位后 4/4 绿 + reachability 同批绿）
Targets: `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/auth/erp-fin.action-auth.xml`（fin-report 组 :234-259）、`module-master-data/erp-md-web/.../erp-md.action-auth.xml`（:180-193）、`module-manufacturing/erp-mfg-web/.../erp-mfg.action-auth.xml`（:247-271）、`module-cs/erp-cs-web/.../erp-cs.action-auth.xml`（:127-136）、`tests/e2e/reports/reports.download.auth.spec.ts`
Skill: none

- Item Types: `Fix | Add`（FNPT ×4 为已确诊缺陷修复 + spec 新增）、`Proof`（红→绿）

- [ ] （Proof·红灯先行）新增 `reports.download.auth.spec.ts`：具名角色账号 × 每域具名报表——fin=`role-finance`/income-statement、md=`role-pur`(采购员，13 角色 CSV 成员)/material-price-list（避开 partner-list 正交缺陷）、mfg=`role-mfg-lead`/production-variance-report（避开 crp-load 运行时波动）、cs=`role-biz-admin`(业务管理员，双命名空间不触发 skip-check)/ticket-sla-csat-summary——POST 断言 200 + XLSX magic + token。**禁用 nop（平台 admin）作断言主体**（skip-check 恒放行假绿正是 fin/md 假通过根因）。**实施 FNPT 前四域全红**（verbatim 失败输出留本计划；探针级红灯已在根因报告节留证）。
      - Skill: none
      - Skill: none
- [ ] （Add）四域 overlay 各注册 `FNPT:ErpXxxReport:query`（displayName 中文、resourceType=FNPT、roles 镜像该域报表菜单种子、`<permissions>ErpXxxReport:query</permissions>`；TOPM/SUBM 无 permissions 不变量保持）。
      - Skill: none
- [ ] （Proof·转绿）重新打包（`mvn clean install -DskipTests`）+ 重启后同 spec 四域全绿；**negative 目录全量套件零回归**（不窄化—— roadmap 验收原文；本变更仅新增四个 `ErpXxxReport:query` permissionToRoles 键 + cs-report 对业务「管理员」新可见，e1-1/e1-3 权限键零交集、e1-2 逐断言核对无翻转）；48 用例 admin 套件零回归。
      - Skill: none

Exit Criteria:

- [ ] 红灯四域留证 → FNPT 落位 → 同 spec 全绿
- [ ] negative 目录全量零回归 + reports.download（48）零回归

### Phase 2 — owner doc 对齐 + md partner-list 正交诊断

Status: completed
Targets: `docs/design/roles-and-permissions.md`、`docs/backlog/successor-followup-closure-roadmap.md`、`docs/logs/`
Skill: none

- Item Types: `Add`（doc 条款）、`Decision`（md partner-list 诊断分流）

- [x] roles-and-permissions.md：报表 FNPT 条款（FNPT 权限点模式说明节后新增整段：非实体纯 BizModel 资源缺口根因 + `:query` 桶语义 + roles 镜像 + %prod fail-closed 加重项与 successor 指针）——「菜单可见≠动作可用」缺口在条款内显式命名（:153 财务员行「报表 sys-report」仅覆盖菜单面的注记并入条款）。
      - Skill: none
- [x] （Decision）md partner-list UI 层失败诊断：**注销留痕（随环境自愈）**——本计划两轮 48 用例 admin 套件（重建链前后）md partner-list xlsx/pdf 均 GREEN（`48 passed`，直连层亦过）；F-R19 首轮观察的 UI 层持续失败未能在 HEAD 复现，归 round-3 负载波动或批次修复已覆盖。若复发按独立缺陷立项（本注销不排除再发现）。
      - Skill: none
- [x] roadmap WI-01 回写（done + 裁决要点 + 红灯前提修正留痕「fin 对照组绿被证伪，四域全红」）+ 状态计数块 + 当日日志（随收官轮）。
      - Skill: none

Exit Criteria:

- [ ] owner doc 条款在案
- [ ] md partner-list 诊断分流落盘（successor 或注销）
- [ ] roadmap/日志回写完成

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立草案审查子代理，fresh session，2026-10-06）——2 P2（F1 roadmap「negative 套件零回归」被窄化为 e1-2 / F2 根因报告悬空指针）+ 5 P3；根因链/修复形状/红灯设计/保护区程序（plan-first 档非 dual-approval）独立复验全部成立。
- Independent draft review iteration 2: `accept`（同审查者预授权，2026-10-06）——F1 扩为全 negative 目录零回归；F2 根因报告自包含落本计划（资源解析链/探针 verbatim/排除法归因/修复形状推导）；F3 具名四域账号+报表；F4 Item Types `Fix | Add`；F5 重新打包+重启表述；F6 与 WI-02 执行期协调注记；F7 行号精度记录。按审查者预授权翻 active。

## Closure Gates

- [ ] 范围内行为完成（四域 FNPT + auth spec + owner doc + md partner-list 分流）
- [ ] 相关文档对齐（roles-and-permissions 条款 + roadmap + 日志）
- [ ] 已运行验证（auth spec 红→绿 + e1-2 + 48 用例零回归 + 四门禁矩阵）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### %prod 报表下载 admin 可用性（fail-closed 加重项）

- Classification: `watch-only residual`
- Why Not Blocking Closure: %prod 部署未启动（roadmap §9 生产 DDL 迁移触发未命中）；修复属部署面角色种子裁决
- Successor Required: `yes`（触发条件：生产部署阶段 plan——roles CSV 追加 admin 或独立报表角色，随 USC-02a/02b 豁免面注记）

### cs 域报表业务角色种子缺口（R1-E1-2 既有登记）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划以管理员（管理员角色）最小注册保下载可用；cs 专属业务角色为既有 owner doc 缺口
- Successor Required: `yes`（触发条件：cs 域角色种子立项时——FNPT roles 扩列）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <closure 时填写>
- Evidence: <closure 时填写>

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
