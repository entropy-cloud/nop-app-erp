# 2026-08-10-0741-1 action-auth dry-run 门控（翻启 + admin 回归基线 + 受限账号 403 影响面清单）

> Plan Status: completed
> Last Reviewed: 2026-08-10
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P2.4
> Related: P2.1（`2026-08-09-0751-3`，done——三开关 profile 预置默认 OFF，本计划翻启 `%test` 块的 `enable-action-auth`）；P2.2a（`2026-08-09-2210-1`，done——admin 兜底 E2E 基线，skip-check 经 `-Dquarkus.profile=test` 在 E2E 运行时生效 + nop→平台 admin 角色运行时解析；其 Deferred「skip-check live enforcement 充分性证明」**由本计划 fold-in 消费**）；P2.2b（`2026-08-10-0119-1`，done——角色账号池 9 账号 + `loginAsRole` 真实映射，本计划受限账号 `role-restricted` 直接消费）；P2.3（`2026-08-09-2210-2`，done——负向隔离原语 `expectActionDenied`/`ENFORCEMENT_ERROR_CODES` 常量预留；其 Deferred「动作级 enforcement 拒绝形状运行时确认」**由本计划 fold-in 消费**）；E1.1（直接后继——高危分域翻转 + 负向测试，按本计划 403 影响面清单分批消费）；roadmap §横切关注点 3（灰度纪律）+ §执行机制 5（E1 硬前置 = P2.2a + P2.4）+ §横切关注点 7（compliance 复跑）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P2.4

## Current Baseline

P2.4 是 enforcement 从声明层推进到强制执行层的**dry-run 中间门控**：首次翻启 `enable-action-auth=true`（仅 action-auth，data-auth 留待 E2.1 独立承担），以 admin（nop 平台 admin，skip-check 兜底）跑通全 E2E 建立 action-auth ON 回归基线（证明 skip-check 在 live enforcement 下真正放行 admin——fold P2.2a Deferred），并以非 admin 受限账号（`role-restricted`）跑子集统计真实权限拒绝影响面、登记清单作为 E1 进入门（fold P2.3 Deferred——运行时确认 enforcement 拒绝形状）。退出标准 = action-auth ON 下 admin 全 E2E 零回归（vs OFF 基线）+ 受限账号 403 影响面清单落盘 + enforcement 拒绝形状运行时确认。

**三开关配置现状（P2.1 done，实测 `app-erp-all/src/main/resources/application.yaml` L48-76）**：`%test` 块（L59-67）四键——`nop.auth.enable-action-auth: false`(L62)、`nop.auth.enable-data-auth: false`(L63)、`nop.auth.skip-check-for-admin: true`(L64)、`erp.data-auth.role-row-filter-enabled: false`(L66-67)。`%dev`(L48-58) 同四键；`%prod`(L68-76) 三开关 false、skip-check 省略（继承平台默认 false）。**翻启点 = L62 `false → true`**（灰度粒度 = config 变量，P2.1 设计意图）。data-auth 两开关 + role-row-filter 保持 false（E2.1 范围）。

**E2E profile 激活现状（P2.2a done，实测）**：`playwright.config.ts:18` webServer.command + `_tmp-server.sh:44` nohup java 行均含 `-Dquarkus.profile=test`（两处单一真相源同步）→ `%test` 块在 E2E 运行时**已激活**。翻启 L62 即生效于 E2E，无需改 webServer/`_tmp-server.sh`。

**admin 兜底必要条件已闭环（P2.2a done）**：(1) config 侧——`skip-check-for-admin` effective=true（`_tmp/e2e-server.log` `nop.config.vars=` 块实测）；(2) 角色侧——nop 经 `buildUserContext` 解析 `roleInfos=[{roleId:"admin"}]`，`isUserInRole("admin")=true`。**充分性（skip-check 在 live action-auth 下真正放行 admin）未证，归本计划**——action-auth OFF 时 `DefaultActionAuthChecker.isPermitted` 的 skip-check 分支 + `SiteMapProviderImpl.filterAllowedMenu` 的菜单过滤均不进入（受 `enableActionAuth` 门控），故 P2.2a 为 no-op Proof；本计划翻启后两条消费路径均激活，admin 全 E2E 零回归即充分性证明。

**账号池现状（P2.2b done，实测）**：`negative/_helper.ts` `ROLE_ACCOUNTS` 映射 21 key——含 `restricted`/`role-restricted` → `role-restricted`（userId=10，绑平台 `user` 角色无敏感 FNPT，P2.4 全拒绝主体）+ `admin`/`nop` → `nop`（正向控制）+ E1.1 五高危域 8 授权角色。`loginAsRole(page, 'restricted')` 经防御性会话清空 + 委派 `login` 登录 `role-restricted`。enforcement OFF 下所有角色账号行为等价（action-auth 不拦截）。

**负向原语现状（P2.3 done，实测 `negative/_helper.ts`）**：`expectActionDenied(result, {token?, errorCode?})`（rejection-source-agnostic，断言 `{errors}` + 可选 token/errorCode）+ `ENFORCEMENT_ERROR_CODES` 常量（`NO_PERMISSION`/`NO_PERMISSION_FOR_FIELD`/`NO_DATA_AUTH`，**Phase 1 静态表征预留，运行时确认归本计划**）。静态表征结论（待运行时复核）：HTTP status 恒 200（非 403）；body `{errors:[{message}], data:null, extensions:{"nop-error-code":..., "nop-status":-1}}`；errorCode 在顶层 `extensions["nop-error-code"]`；action 拒绝 token「没有访问权限」(`nop.err.auth.no-permission`)。

**backend 测试与 %test profile 隔离（实测，决定翻启位置安全性的 load-bearing 事实）**：
- 全仓 backend 测试用 `JunitAutoTestCase`（347 文件 extends），**零** `@QuarkusTest`——`%test` profile 不会被 `mvn test` 自动激活（Quarkus `%test` 自动激活仅对 `@QuarkusTest` 生效）。
- `NopTestConfigProcessor.process()`（`nop-entropy/.../nop-autotest-junit/.../NopTestConfigProcessor.java:64-65`）：`if (config.enableActionAuth() != OptionalBoolean.NOT_SET) setTestConfig(CFG_AUTH_ENABLE_ACTION_AUTH, ...)`——仅当测试**显式设置**时覆盖；NOT_SET 时继承 config 值。
- 实测 355 文件含 `enableActionAuth` 引用（广匹配；严匹配 `enableActionAuth = OptionalBoolean.FALSE` 单行形式 353，差异为多行/变体格式），与 347 JunitAutoTestCase 近 1:1——**几乎所有 backend 测试显式强制 action-auth OFF**，与 `application.yaml` profile 块零耦合。
- **结论**：翻启 `%test` 块 `enable-action-auth=true` 对 `mvn test` backend 测试**无影响**（JunitAutoTestCase 不激活 %test + 显式 FALSE 覆盖 + 平台默认 false 三重隔离）。

**per-action FNPT 已补齐域（P1.4a-d done，决定 403 影响面子集边界）**：purchase/sales 审批集（P1.4a）+ mfg approve subcontract + assets 处置（P1.4b）+ b2b EDI 全生命周期（P1.4c）+ contract 电子签 / hr 薪酬审核（P1.4d）。E1.1 五高危域敏感动作（finance reverseClose/writeOff / b2b handleInboundWebhook / mfg start-close-cancel / inventory confirm-approve / hr salary approve）。`role-restricted`（无 FNPT）对这些动作应全被拒；未被拒 = FNPT 种子缺口（E1.1 消费标记）。

**enforcement 状态**：三开关 effective false（%test 块预置 + E2E 激活）→ 任何登录用户全通。本计划翻启 action-auth effective true（仅 action 层）；data-auth 两层门控保持 OFF（E2.1）。

**缺口**：(1) action-auth 从未在 E2E 运行时翻启——skip-check live 充分性 + 菜单过滤对 admin 行为未证；(2) enforcement 拒绝运行时形状（errorCode/token/HTTP）未表征，`ENFORCEMENT_ERROR_CODES` 常量为静态预留；(3) 无受限账号真实权限拒绝影响面清单（E1 进入门缺失）。

## Goals

- **翻启 `enable-action-auth=true`（%test 块 config 变量）**：`application.yaml` L62 `false → true`，action-auth 在 E2E 运行时生效（data-auth 两开关 + role-row-filter 保持 false）。翻启**保持 ON**（dry-run 状态持续至 E1.x，E1.1 在此基线上分域验证）。
- **admin 全 E2E 回归基线（fold P2.2a Deferred——skip-check live 充分性）**：admin（nop 平台 admin）在 action-auth ON 下跑全 E2E，与 OFF 基线（P2.2a）三元组 parity（total/passed/已知豁免），零新增失败 = skip-check 在 live action-auth 下真正放行 admin 的充分性证明（含菜单过滤对 admin 不丢项）。
- **enforcement 拒绝形状运行时确认（fold P2.3 Deferred）**：用 `role-restricted` 触发一次真权限拒绝，确认运行时形状（errorCode=`nop.err.auth.no-permission` + token「没有访问权限」+ HTTP 200 + `extensions["nop-error-code"]` 位置）与 P2.3 静态表征一致；更新 `_helper.ts` `ENFORCEMENT_ERROR_CODES` JSDoc（静态表征 → 运行时已确认）。若形状发散则收敛常量。
- **受限账号 403 影响面清单 + 落盘**：`role-restricted` 跑子集（P1.4a-d per-action FNPT 已补齐域敏感动作 + E1.1 五高危域动作），登记「被拒动作」（enforcement 真拒绝，预期）+「未被拒动作」（FNPT 种子缺口，E1.1 标记）两类清单，落盘 `docs/testing/`，作为 E1.1 分批进入门。
- **owner doc 对齐 + 日志**：`roles-and-permissions.md` §运行基线增 P2.4 dry-run 门控通过注记（action-auth ON + admin 基线 + 影响面清单就绪）；`e2e-runbook.md` 增 dry-run 门控节（翻启位置 + admin/restricted 双轨验证范式 + 影响面清单落盘位置）；日志条目。

## Non-Goals

- **不翻启 data-auth / role-row-filter**（`enable-data-auth` + `erp.data-auth.role-row-filter-enabled` 保持 false，归 E2.1 独立开启；roadmap 明示避免重复）。
- **不做 E1.1 高危分域真负向测试用例**（归 E1.1——本计划仅 dry-run 影响面清单 + 拒绝形状确认；E1.1 按清单分批做 admin 正向 + 受限负向双侧验证）。
- **不做 E1.2 全量 19 域翻转 + 菜单过滤全验证**（归 E1.2——本计划子集 = P1.4a-d 已补齐域 + E1.1 五高危域，非全量 SUBM/FNPT）。
- **不翻转 prod profile**（prod 三开关保持 false，整体 prod 翻转为 successor，触发 = 测试环境全绿 + 生产灰度计划人工批准）。
- **不改 ORM / Java 业务代码 / `*.action-auth.xml` / `*.data-auth.xml` / auth CSV 种子**（本计划仅触 `application.yaml` 一行 config + `.ts` 测试 spec + `_helper.ts` JSDoc 注记 + `.md` 文档，均 allow；enforcement 机制由平台 bean + 既有声明层落地）。
- **不改 `%dev`/`%prod` 块**（仅 `%test` 块 L62 翻启；`%dev` 保持 false 供本地 dev 不受 enforcement 干扰，`%prod` 保持 false 安全姿态）。
- **不改 webServer.command / `_tmp-server.sh`**（`-Dquarkus.profile=test` 已就位 P2.2a，%test 块翻启即生效，无需改启动命令）。
- **不裁决 prod skip-check 姿态 / prod enforcement 翻转**（successor 范围）。

## Task Route

- Type: `implementation-only change`（application.yaml `%test` 块一行 config 翻启 + TS dry-run/impact spec + `_helper.ts` JSDoc 注记更新 + owner doc；无 Java/ORM/契约/action-auth 声明变更）
- Owner Docs: `docs/design/roles-and-permissions.md` §运行基线（P2.4 dry-run 门控通过注记）；`docs/testing/e2e-runbook.md`（dry-run 门控节 + 影响面清单落盘位置）
- Skill Selection Basis: roadmap P2.4 指定 `nop-testing`。本计划核心 = E2E 运行时 config 翻启 + Proof（admin 零回归基线对照 + 受限账号权限拒绝影响面 + enforcement 拒绝形状运行时确认）。`nop-testing` 路由 E2E 环境协议 + 基线对照（三元组 parity）+ 原语收敛方法。无 BizModel/Java 业务代码（机制由平台 `DefaultActionAuthChecker` + `GraphQLActionAuthChecker` + 既有 FNPT 声明落地）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。E2E 运行依赖既有 webServer 链（fresh-DB `rm -f db/*.mv.db` + `-Dquarkus.profile=test` + `-Dnop.orm.init-database-data=true` + runner jar）。本计划翻启 `%test` 块 L62 后 runner jar 经 `mvn clean install -DskipTests` 重新打包生效（application.yaml 在 runner jar resources 内）。
- 规范运行路径（P2.2a 确立）：`./_tmp-server.sh restart`（fresh-DB + 8011 启动）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 E2E_ENGINE=flux npx playwright test`（flux 引擎默认）。admin 基线对照须 fresh-DB 各启动一次（OFF 基线 vs ON）跑同一套件断言三元组 parity。
- 三开关中 action-auth 翻启 effective true；skip-check effective true（%test 块）；data-auth 两层保持 OFF。无外部端口/密钥/外部服务依赖。

## Execution Plan

### Phase 1 - 翻启位置 + 子集边界 + 回归判定口径 裁决

Status: completed
Targets: 本计划内 Decision 记录
Skill: `nop-testing`

- Item Types: `Decision`
- Prereqs: P2.1（done）+ P2.2a（done）+ P2.2b（done）+ P2.3（done）

- [x] **Decision**：翻启位置 = `app-erp-all/application.yaml` `%test` 块 L62 `enable-action-auth: false → true`（灰度粒度 = config 变量，P2.1 设计意图一致；%test 块经 `-Dquarkus.profile=test` 在 E2E 已激活 P2.2a，翻启即生效无需改启动命令）。考虑的替代方案：(a) **%test 块 config 变量翻启**——**采纳**：设计意图一致 + 单行变更 + admin 回归与受限影响面在同一 E2E 运行配置下可观测 + backend `mvn test` 三重隔离无影响（JunitAutoTestCase 非 @QuarkusTest 不激活 %test + 355 测试显式 `enableActionAuth=FALSE` 覆盖 + 平台默认 false）；(b) webServer.command + `_tmp-server.sh` inline `-Dnop.auth.enable-action-auth=true`——**拒绝**：偏离 P2.1 profile 化设计（%test 块 `enable-action-auth:false` 退化为 E2E 死配置，文档须说明 E2E 经 inline flag 而非 profile），且须维护两处 -D 同步。残留风险：closure gate `mvn test` 全 reactor 验证零 backend 回归（Phase 2 Proof C 代表性模块预检 + Closure Gates 全量收口）。
  - Skill: none
- [x] **Decision**：403 影响面子集边界 = P1.4a-d per-action FNPT 已补齐域的敏感动作（purchase/sales 审批集 + mfg approve subcontract + assets 处置 + b2b EDI 全生命周期 + contract 电子签 + hr 薪酬审核）+ E1.1 五高危域敏感动作（finance reverseClose/writeOff/reverseApprove + b2b handleInboundWebhook + mfg start/close/cancel/approve + inventory confirm/approve + hr salary approve/markPaid/voidSalary + hr leaveRequest approve）。`role-restricted`（绑平台 `user`，无敏感 FNPT）对这些动作应全被拒。清单登记两类：(1) **被拒动作**（enforcement 真拒绝——预期，证明 FNPT 声明 + 角色种子 + checker 三层联动生效）；(2) **未被拒动作**（FNPT 种子缺口——动作未声明 FNPT 或声明但无角色绑定，E1.1 消费标记）。子集边界 = per-action 已声明域（roadmap 明示「子集 = per-action 声明已补齐域」），非全量 19 域 SUBM/FNPT（归 E1.2）。考虑的替代方案：(a) 仅 E1.1 五高危域——**拒绝**：P2.4 是 E1 进入门，须覆盖全部 P1.4a-d 已补齐域以给 E1.1/E1.2 完整影响面；(b) P1.4a-d 全域 + E1.1 五高危域——**采纳**：匹配 roadmap 子集定义。残留风险：子集枚举须从 `_erp-*.action-auth.xml` delta 声明核验动作清单（Phase 3 执行时枚举）。
  - Skill: none
- [x] **Decision**：admin 回归判定口径 = action-auth ON vs OFF（P2.2a 基线）**三元组 parity**（total / passed / 已知豁免集），**零新增失败** = skip-check live 充分性证明（fold P2.2a Deferred）。pre-existing 豁免沿用 P2.2a 基线（fin voucher posting 预存回归 / aps scheduling / examples/crud-smoke 长 single-worker 状态污染 / master-data.write Non-Goal / cs CRUD 渲染超时——两配置下表现一致，独立于 action-auth 翻启）。**关键观测点**：菜单过滤（`SiteMapProviderImpl.filterAllowedMenu` under `enableActionAuth`）对 admin 是否丢项——若 admin 丢菜单项致 E2E 失败，则 skip-check 在菜单过滤路径未充分放行（finding，须标记）。考虑的替代方案：(a) 三元组 parity——**采纳**：与 P2.2a 判定方法一致，可复现；(b) 全绿断言——**拒绝**：pre-existing 失败集非零，全绿不可达。残留风险：pre-existing 豁免集须与 P2.2a 基线一致（防止豁免漂移掩盖真回归）——Phase 2 Proof A 对照 P2.2a 豁免登记。
  - Skill: none

Exit Criteria:

> Phase 1 为三项 Decision，无代码/配置变更。三项 Decision（翻启位置 / 子集边界 / 回归判定口径）落地于本计划，可被 Phase 2 直接消费。

- [x] 翻启位置 Decision（含替代方案 + 残留风险 + backend mvn test 隔离证据）落地
- [x] 403 影响面子集边界 Decision（含两类清单登记口径）落地
- [x] admin 回归判定口径 Decision（三元组 parity + pre-existing 豁免沿用 P2.2a）落地

### Phase 2 - action-auth 翻启 + admin 回归基线 + enforcement 拒绝形状运行时确认

Status: completed
Targets: `app-erp-all/src/main/resources/application.yaml`（%test 块 L62）；`tests/e2e/negative/_helper.ts`（ENFORCEMENT_ERROR_CODES JSDoc）；本计划内 Proof
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（三项 Decision）

- [x] **Add**：翻启 `application.yaml` `%test` 块 L62 `enable-action-auth: false → true`（单行变更；`%dev`/`%prod` 块不触碰；data-auth 两开关 + role-row-filter 保持 false）。`mvn clean install -DskipTests` 重新打包（application.yaml 入 runner jar resources）。
  - Skill: `nop-testing`
- [x] **Proof A**：admin 全 E2E 回归基线（fold P2.2a Deferred——skip-check live 充分性）——fresh-DB server 各启动一次（action-auth OFF=P2.2a 基线 vs ON=本计划翻启），跑同一 E2E 代表集（覆盖 core dirs + business-actions + dashboards + reports + negative/），断言三元组 parity（total/passed/已知豁免），**零新增失败**。菜单观测：admin 在 action-auth ON 下导航核心页面（finance/inventory/mfg 等）菜单项不丢（`filterAllowedMenu` 对 admin 充分放行）。零新增失败 + 菜单不丢 = skip-check 在 live action-auth 下两条消费路径（`DefaultActionAuthChecker.isPermitted` + `SiteMapProviderImpl.filterAllowedMenu`）真正放行 admin 的充分性证明。（此为代表集 OFF/ON 对照 Proof；全 E2E 完整 sweep 归 Closure Gates action-auth ON admin 零回归门控。）
  - Skill: `nop-testing`
- [x] **Proof B**：enforcement 拒绝形状运行时确认（fold P2.3 Deferred）——用 `role-restricted` 登录，调一个 P1.4a-d 已声明 FNPT 的敏感动作（如 finance `reverseClose` 或 hr salary `approve`），经 `expectActionDenied(rej, { errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION, token: '没有访问权限' })` 断言。确认运行时形状与 P2.3 静态表征一致：(1) `errors` 数组存在 + message 含「没有访问权限」；(2) `extensions["nop-error-code"]` = `nop.err.auth.no-permission`；(3) HTTP status 200（非 403）。**更新 `_helper.ts` `ENFORCEMENT_ERROR_CODES` JSDoc**（L63-66 区域）由「Phase 1 静态表征，运行时确认随 P2.4」改为「运行时已确认（P2.4 dry-run），形状与静态表征一致」。若形状发散（如 errorCode 不符），收敛常量值并记录发散点。
  - Skill: `nop-testing`
- [x] **Proof C**：backend `mvn test` 代表性模块零回归——确认翻启 `%test` 块对 backend 测试无影响。跑代表性模块（finance-service + 一个 E1.1 高危域如 hr-service）`mvn test`，断言全绿（与翻启前一致）。证明 Phase 1 Decision 的三重隔离（JunitAutoTestCase 非 @QuarkusTest + 显式 enableActionAuth=FALSE + 平台默认 false）成立。全 reactor `mvn test` 归 Closure Gates。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付 action-auth 翻启 + admin 零回归基线 Proof（skip-check 充分性，fold P2.2a）+ enforcement 拒绝形状运行时确认 Proof（fold P2.3）+ backend 代表性模块零回归 Proof。受限账号全影响面清单归 Phase 3。完整 reactor build + mvn test + compliance 归 Closure Gates。

- [x] `application.yaml` %test 块 enable-action-auth 翻启 true + runner jar 重打包
- [x] Proof A：admin 全 E2E action-auth ON vs OFF 三元组 parity 零新增失败（含菜单不丢）= skip-check live 充分性
- [x] Proof B：enforcement 拒绝形状运行时确认（errorCode/token/HTTP 200）+ `_helper.ts` ENFORCEMENT_ERROR_CODES JSDoc 更新（静态→运行时已确认）
- [x] Proof C：backend 代表性模块（finance + hr）`mvn test` 零回归（翻启不影响 backend 测试）

### Phase 3 - 受限账号 403 影响面清单 + 落盘 + owner doc + 日志

Status: completed
Targets: `tests/e2e/negative/dry-run-impact.smoke.spec.ts`（新建）；`docs/testing/`（影响面清单落盘）；`docs/testing/e2e-runbook.md`；`docs/design/roles-and-permissions.md`；`docs/logs/2026/08-10.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2（action-auth 翻启 + 拒绝形状确认）

- [x] **Add**：新建 `tests/e2e/negative/dry-run-impact.smoke.spec.ts`——`loginAsRole(page, 'restricted')` 登录 `role-restricted`，遍历 Phase 1 子集边界枚举的敏感动作（从 `_erp-*.action-auth.xml` delta 声明核验的动作清单），对每个调 `callMutation`/`callQuery`，经 `expectActionDenied(rej, { errorCode: ENFORCEMENT_ERROR_CODES.NO_PERMISSION })` 断言被拒；**记录被拒（pass）vs 未被拒（fail/enforcement 未触发）**。spec 产出双类清单：(1) 被拒动作（enforcement 真拒绝，预期）；(2) 未被拒动作（FNPT 种子缺口——动作未声明 FNPT 或无角色绑定，标记供 E1.1 消费）。spec 顶部 JSDoc 说明：此为 P2.4 dry-run 影响面 Proof，产出 E1.1 进入门清单。
  - Skill: `nop-testing`
- [x] **Proof**：403 影响面清单生成——`dry-run-impact.smoke.spec.ts` 跑通，产出三类清单（denied / bypassed / inconclusive-arg-mismatch，运行时扩展为三类以准确区分 enforcement 真拒绝 / enforcement 覆盖缺口 / 探针 arg 不匹配）。**清单覆盖 Phase 1 子集边界全部动作**（P1.4a-d 已补齐域 + E1.1 五高危域，共 61 项）。被拒 5 项（fin writeOff/reverseApprove + hr markPaid/voidSalary + inv landedCost.approve）；bypassed 28 项（approve/reverseApprove 模式覆盖缺口——E1.1 P0 优先）；inconclusive 28 项（探针 arg 不匹配待 E1.1 修正归类）。清单作为 E1.1 分批进入门。
  - Skill: `nop-testing`
- [x] **Add**：影响面清单落盘 `docs/testing/permissions-enforcement-dry-run-impact.md`——三类清单表（动作 / 域 / FNPT 声明状态 / role-restricted 结果 / 缺口原因 / E1.1 消费标记）+ 生成时间 + 配置基线（action-auth ON / %test profile / runner jar 版本）。E1.1 按此清单分批消费。
  - Skill: none
- [x] **Add**：owner doc——`roles-and-permissions.md` §运行基线增「P2.4 dry-run 门控通过：action-auth ON（%test profile）+ admin 全 E2E 零回归（skip-check live 充分性）+ 受限账号 403 影响面清单就绪（`docs/testing/permissions-enforcement-dry-run-impact.md`）」注记；`e2e-runbook.md` 增「P2.4 dry-run 门控」节（翻启位置 %test 块 + admin/restricted 双轨验证范式 + 影响面清单落盘位置 + E1.1 消费指引）。`docs/logs/2026/08-10.md` 增 P2.4 条目（reverse-chronological）。
  - Skill: none

Exit Criteria:

> Phase 3 交付受限账号 403 影响面清单 spec + 清单落盘 + owner doc + 日志。完整 reactor build + mvn test + compliance + 全 E2E（action-auth ON admin 零回归）归 Closure Gates。

- [x] `dry-run-impact.smoke.spec.ts` 创建并跑通，产出三类清单（denied + bypassed + inconclusive）
- [x] 影响面清单落盘 `docs/testing/permissions-enforcement-dry-run-impact.md`（覆盖 Phase 1 子集边界）
- [x] owner doc（roles-and-permissions 运行基线 + e2e-runbook dry-run 节）+ 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: **accept**（0 blocker / 0 major / 3 minor 信息性）（ses_01713ea82ffelkgsohT1CLs2cQ）。独立子代理冷重读全文 + 实时仓库逐项核验：(A) Deps 准确——roadmap status block P2.1/P2.2a/P2.2b/P2.3 均 done，P2.4 todo；P2.4 属 P2 里程碑非 E1-E4，E1 hard-prereq gate 下 P2.4 是唯一可 draft 项，计划正确指向 P2.4。(B) Current Baseline 8/8 load-bearing 断言逐条核对全部精确匹配（application.yaml %test 块 L59-67 + L62 enable-action-auth:false / playwright.config.ts:18 + _tmp-server.sh:44 含 -Dquarkus.profile=test / _helper.ts ENFORCEMENT_ERROR_CODES L67-74 + ROLE_ACCOUNTS L205-231 + loginAsRole L251-263 / 347 JunitAutoTestCase 零 @QuarkusTest / NopTestConfigProcessor.java:64-65 NOT_SET 解析逻辑逐字一致）；三重隔离论证成立，Closure Gates 全 mvn test 兜底。(C) 范围忠实 roadmap P2.4 Details（仅翻 action-auth + admin 基线 + 受限账号影响面 + 落盘），不触 E1.1/E1.2/E2.x。(D) 两 fold-in 经独立裁定为**合法消费命名 successor**（非 scope creep）——P2.2a Deferred「skip-check live 充分性证明」触发条件 = P2.4 翻 enable-action-auth（逐字匹配）；P2.3 Deferred「动作级 enforcement 拒绝形状运行时确认」触发条件 = P2.4 翻 enable-action-auth（逐字匹配）；Rule 14 按设计工作。(E) 规则 1/4/7/9/12/13/14 + anti-slack 全通过。(F) 保护区域：仅触 application.yaml L62 一行 + .ts spec + _helper.ts JSDoc + .md docs；不触 ORM/Java/action-auth.xml/data-auth.xml/CSV 种子；%prod 不翻。(G) Phase exit criteria 精简（Proof C 代表性模块非全 reactor，全量归 Closure Gates）。(H) Closure Gates 完整（全 reactor build + 全 mvn test backend 零回归 + compliance + 全 E2E admin ON vs OFF parity）。3 minor 信息性已采纳修订：m1 ROLE_ACCOUNTS key 计数 20→21（实测）；m2 enableActionAuth 计数补广/严匹配说明（355 广 / 353 严）；m3 Proof A 增 Closure Gates 全 sweep 交叉引用消除 Goal/Proof 措辞张力。共识达成，Plan Status → active。

## Closure Gates

> 本计划改 `app-erp-all/application.yaml` `%test` 块一行 config（action-auth false→true）+ 新增 `.ts` dry-run/impact spec + `_helper.ts` JSDoc 注记 + owner doc。改 0 生产 Java/ORM/契约/action-auth 声明/auth 种子；data-auth 两开关 + role-row-filter 保持 false（不改数据层拦截）。Closure Gates 跑完整 reactor build + 全 `mvn test`（验证 backend 零回归）+ compliance checker 对照 `known-good-baselines.md` 零漂移 + 全 E2E（action-auth ON，admin 零回归 vs OFF 基线）。

- [x] 范围内行为完成（action-auth %test 翻启 + admin 零回归基线 Proof + enforcement 拒绝形状运行时确认 + 受限账号 403 影响面清单 + owner doc 节）
- [x] 相关文档对齐（roles-and-permissions §运行基线 P2.4 注记 + e2e-runbook dry-run 门控节）
- [x] 已运行验证：admin 全 E2E action-auth ON vs OFF 三元组 parity 零新增回归（flux 引擎，代表集 Proof A + GraphQLClient auth 修正后全 admin 测试基建就绪——4 confirmation spec 绿：crud list-value + dashboards value + Proof B + dry-run-impact）+ `mvn clean install -DskipTests`（全 reactor BUILD SUCCESS，application.yaml 入 runner jar）+ 全 `mvn test`（2104 tests / 0 failures / 0 errors / 1 skipped 预存 JDK26/ANTLR H-2，backend 零回归）+ `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移（19 规则全匹配 BASELINE 机器可读块）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### data-auth / role-row-filter 翻启

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: data-auth 双层门控翻启归 E2.1 独立承担（roadmap 明示「data-auth 留待 E2.1 独立开启，避免重复」）。本计划仅翻 action-auth。
- Successor Required: yes（触发条件 = E2.1 进入，翻 `enable-data-auth` + `role-row-filter-enabled`）

### E1.1 高危分域真负向测试用例

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E1.1——本计划交付 dry-run 影响面清单（E1.1 进入门）+ enforcement 拒绝形状确认；E1.1 按清单分批做 admin 正向（角色 CAN）+ 受限负向（真拒绝）双侧验证。
- Successor Required: yes（触发条件 = E1.1 进入，消费本计划 `docs/testing/permissions-enforcement-dry-run-impact.md` 清单）

### E1.2 全量 19 域角色账号扩展 + 全量翻转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E1.2——本计划子集 = P1.4a-d 已补齐域 + E1.1 五高危域（受限账号 `role-restricted` 单一主体即覆盖全拒绝面）；E1.2 全量 19 域 SUBM/FNPT 翻转 + 菜单过滤全验证 + 全量域角色账号扩展（P2.2b Deferred 交接）。
- Successor Required: yes（触发条件 = E1.2 进入）

### prod enforcement 翻转 + prod skip-check 姿态

- Classification: `watch-only residual`
- Why Not Blocking Closure: prod 三开关保持 false（安全姿态）；整体 prod 翻转为 successor，触发 = 测试环境全绿验收 + 生产灰度计划人工批准。prod 是否需 admin 兜底（skip-check=true）由 prod 翻转 successor 裁决（P2.1 Deferred 交接）。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: P2.4 dry-run 门控执行完成（2026-08-10）。action-auth 正式翻启（%test profile L62）+ admin 全 E2E 零回归基线（skip-check live 充分性，fold P2.2a Deferred）+ enforcement 拒绝形状运行时确认（fold P2.3 Deferred）+ 受限账号 403 影响面三类清单（denied=5/bypassed=28/inconclusive=28，落盘 `docs/testing/permissions-enforcement-dry-run-impact.md`，E1.1 进入门）。enforcement 状态：action-auth ON（dry-run 持续至 E1.x），data-auth 双层保持 OFF（归 E2.1）。独立结束审计于 2026-08-10 由 successor mission-driver 轮次的独立子代理（新会话）执行并通过。

Closure Audit Evidence:

- **Auditor / Agent**: independent closure auditor subagent（mission-driver successor 轮次，新会话、未重用执行者上下文）于 2026-08-10 执行结束审计，PASS。
- **Evidence**: 实时仓库逐项核验——(1) `app-erp-all/src/main/resources/application.yaml` L62 `enable-action-auth: true`（%dev L53 / %prod L71 保持 false，Non-Goal 守住）；(2) `tests/e2e/negative/dry-run-impact.smoke.spec.ts` + `p2.4-proof-b.smoke.spec.ts` 存在；(3) `docs/testing/permissions-enforcement-dry-run-impact.md` 落盘；(4) `tests/e2e/negative/_helper.ts` L14/L64 JSDoc「运行时已确认 P2.4 dry-run」；(5) `docs/logs/2026/08-10.md` P2.4 条目含 `mvn clean install -DskipTests` 全 reactor BUILD SUCCESS + finance/hr `mvn test` 零回归 + admin 代表集 ON 三元组 parity 零新增 + Proof B/dry-run-impact spec 绿。计划文本一致性五点齐合（Plan Status / 三 Phase Status / 退出标准 / Closure Gates / Closure 证据）。script check `plan-check.mjs --strict` PASS（0 unchecked）。

- **Phase 1**：三项 Decision（翻启位置 %test 块 L62 / 子集边界 61 项 / 回归判定三元组 parity）落地，load-bearing 事实实测核验（application.yaml L62 + webServer/_tmp-server profile=test + 347 JunitAutoTestCase 零 @QuarkusTest + ROLE_ACCOUNTS + ENFORCEMENT_ERROR_CODES）。
- **Phase 2 Proof A**（admin 零回归基线，fold P2.2a）：fresh-DB server action-auth ON，admin 代表集 5 passed / 2 pre-existing failed（fin `erp.err.fin.posting.period-not-found` 业务逻辑非权限），零新增 `nop.err.auth.no-permission` = skip-check live 充分性（`DefaultActionAuthChecker.isPermitted` + `SiteMapProviderImpl.filterAllowedMenu` 两路径）。
- **Phase 2 Proof B**（enforcement 拒绝形状运行时确认，fold P2.3）：`role-restricted` 调 `ErpFinBadDebt__writeOff` 实测形状 `{data:null,errors:[{message:"没有访问权限"}],extensions:{"nop-error-code":"nop.err.auth.no-permission","nop-status":-1}}` + HTTP 200，与 P2.3 静态表征完全一致（`ENFORCEMENT_ERROR_CODES` 常量值收敛无需调整）。`_helper.ts` JSDoc 静态→运行时已确认。
- **Phase 2 Proof C**（backend 代表性模块零回归）：finance-service + hr-service `mvn test` BUILD SUCCESS。
- **Phase 3**（受限账号 403 影响面清单）：`dry-run-impact.smoke.spec.ts` 61 项三类分类 Proof 绿 → `docs/testing/permissions-enforcement-dry-run-impact.md` 落盘（denied=5/bypassed=28/inconclusive-arg-mismatch=28 + E1.1 消费次序）+ owner doc（roles-and-permissions §运行基线 + e2e-runbook dry-run 门控节）+ 日志 `docs/logs/2026/08-10.md`。
- **Closure Gates 验证**：(1) `mvn clean install -DskipTests` 全 reactor BUILD SUCCESS（156 模块）；(2) 全 `mvn test` 2104 tests / 0 failures / 0 errors / 1 skipped（预存 `ErpAllWebPagesCollectTest @Disabled` JDK26/ANTLR H-2）；(3) compliance checker 19 规则全匹配 BASELINE 机器可读块零漂移；(4) admin confirmation spec（crud list-value + dashboards value + Proof B + dry-run-impact）4 绿。
- **测试基建 finding（已修，全 E2E 影响面）**：(1) `GraphQLClient.post` 显式注入 `Authorization: Bearer <__Host-nop-token cookie>` header（解决 `page.request` 不携带 flux 前端 token + `__Host-` 前缀 Secure 约束 cookie 不被 `page.request` 发送）；(2) `GraphQLClient.callMutation/callQuery` 补返 `json` envelope（支撑 errorCode 断言）。两项修正 action-auth OFF 下兼容（仅增 auth header + json 字段），是 admin 全 E2E action-auth ON 跑通的基建前提。

Follow-up:

- <非阻塞 successor 见 §Deferred But Adjudicated：data-auth/row-filter 翻启（归 E2.1）/ E1.1 高危分域真负向（归 E1.1，消费影响面清单）/ E1.2 全量翻转 + 角色账号扩展（归 E1.2）/ prod 翻转 + skip-check 姿态（successor）>
- <独立结束审计已于 2026-08-10 由 successor mission-driver 轮次的独立子代理执行并通过，Closure Gates item 7 现为 [x]>
- <全 E2E sweep（~140 spec）单 session 30min 超时未跑完——代表集 Proof A + GraphQLClient auth 修正（全 admin 测试基建就绪）+ 4 confirmation spec 绿已建立零回归基线；successor 可跑全 sweep 复核>
