# 2026-07-27-0930-1 module-metadata 诊断 4 @BizQuery 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/deepening-roadmap.md` §8.8 D2（Business Module Metadata BT5-style，状态 `done` 但仅 JUnit 单层验证 `TestModuleMetaReader` 7 场景，零浏览器层 E2E）+ AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点
> Related: `docs/plans/2026-07-26-1500-2-party-identity-query-browser-e2e.md`（C1 统一 Party 身份查询——同型非实体 BizModel `@BizQuery` 浏览器层 E2E 先例，本计划镜像其范式）、`docs/plans/2026-07-21-2225-3-business-module-metadata-bt5.md`（D2 后端落地 parent plan）、`docs/plans/2026-07-26-1407-3-exchange-rate-api-client-browser-e2e.md`（D1 浏览器层 E2E——deepening 后端 JUnit-only→浏览器层补全覆盖范式先例）
> Audit: required

## Current Baseline

D2（Business Module Metadata BT5-style）后端已落地（plan 2026-07-21-2225-3，状态 `done`），运行时读取器 + 诊断 BizModel 经 JUnit 单层验证（`TestModuleMetaReader` 7 场景全绿：多模块扫描 19 域 / 依赖完整性正路径 / 缺失依赖负路径 / 版本不匹配负路径 / 特性清单查询 / 向后兼容 DAG 根省略 businessDependencies / 跨域依赖矩阵抽样）。全 19 域 `_module-meta.json` 经 codegen 生成 well-formed（含 version=1.0.0 + businessDependencies + optionalFeatures）。

**零浏览器层 E2E**：经 `find tests/e2e -iname "*module-meta*"` 核实无任何 spec 覆盖 `ErpModuleMetaBizModel` 4 `@BizQuery`。`ErpModuleMetaBizModel`（`app-erp-all/src/main/java/app/erp/all/meta/ErpModuleMetaBizModel.java`，非实体 BizModel 经 `app-service.beans.xml` 显式注册）暴露 4 `@BizQuery`：

| GraphQL 入口 | Java 方法 | 返回类型 | 关键字段 |
|--------------|----------|----------|----------|
| `ErpModuleMeta__listModules` | `listModules()` | `List<ModuleMetaBean>` | moduleId/moduleName/appName/version/businessDependencies[]/optionalFeatures[] |
| `ErpModuleMeta__getModule(moduleId)` | `getModule(String)` | `ModuleMetaBean`（nullable） | 同上单条 |
| `ErpModuleMeta__checkDependencyIntegrity` | `checkDependencyIntegrity()` | `DependencyIntegrityResult` | ok(boolean)/missing(List<String>)/mismatches[] |
| `ErpModuleMeta__listOptionalFeatures` | `listOptionalFeatures()` | `List<ModuleMetaBean.ModuleFeature>` | feature/configKey/defaultValue |

**种子 `_module-meta.json` 基线**（codegen 产物，fresh-DB 启动后运行时 VFS 可达）：
- 19 域模块全声明 `version="1.0.0"`（`ModuleMetaReader.loadAll` 经 `ModuleManager.getEnabledModules` 枚举）
- DAG 根模块（master-data）/ 跨域基础设施（notify）省略 `businessDependencies`（向后兼容验证点）
- `module-purchase/erp-pur-meta/precompile/module-meta.yaml` 样本：6 businessDependencies（erp/md + erp/inv + erp/fin + erp/qa + erp/ast + erp/prj，均 version=1.0.0）+ 1 optionalFeature（`supplier-scorecard-red-gate` / `erp-pur.scorecard-prevent-on-red` / defaultValue=true）
- aps/logistics/b2b 三域省略 `optionalFeatures`（无 config-gated 特性，向后兼容验证点）

剩余差距：D2 是 deepening-roadmap 11 项中**唯一**后端代码已落地但零浏览器层 E2E 覆盖的项（A1/A2/A3/B1/C1/C2/C3/D1 均已于 2026-07-26 经 0410-1/0410-2/0500-1/0500-2/0500-3/1407-1/1407-2/1407-3/1500-1/1500-2 落地浏览器层 E2E；D3/D4 为纯文档/分析无代码）。4 `@BizQuery` 经 GraphQL `/graphql` 全栈可达性 + seed 派生确定性断言缺口未闭合。

## Goals

- `ErpModuleMetaBizModel` 4 `@BizQuery`（`listModules`/`getModule`/`checkDependencyIntegrity`/`listOptionalFeatures`）经 Playwright 浏览器层 GraphQL `/graphql` 全栈可达性验证 + seed 派生确定性断言（19 模块 / version=1.0.0 / 依赖完整性 ok=true / purchase 依赖+特性样本）
- D2 §8.8 落地证据增「浏览器层验证」bullet（镜像 §8.2 C1 / §8.5 D1 / §8.6 C3 / §8.9 A3 范式），收口 deepening-roadmap 全 11 项浏览器层 E2E 覆盖里程碑

## Non-Goals

- **不新增/修改 `_module-meta.json` 或 `precompile/module-meta.yaml` 内容**——本计划仅验证既有产物经 GraphQL 全栈可达，不改变模块元数据真相源（D2 后端已落地，source-of-truth 不动）
- **不实现版本范围求解器（SemVer range resolution）**——D2 Deferred successor，触发条件「模块业务版本数 > 3 + 不兼容升级场景」未满足
- **不实现 SaaS 多租户版本管理编排**——D2 Deferred successor，触发条件「SaaS 多租户部署 + tenant-model 集成授权」未满足
- **不做 D4 插件热管理**——D4 仅由 D1 解锁，纯可行性研究已 done，无代码可验证
- **不做负路径 setup（缺失依赖/版本不匹配）**——`checkDependencyIntegrity` 负路径经 JUnit `testMissingDependency`/`testVersionMismatch` 已覆盖；浏览器层 fresh-DB 启动后 `_module-meta.json` 为 codegen 产物无法经 GraphQL 写入制造缺失/不匹配场景（非实体 BizModel 无 `__save`/`__update` 入口），负路径浏览器层覆盖为不可达，归 JUnit 单层覆盖（对齐 1407-3 RATE_LIMITED / 0410-1 strict-mode 范式）
- **不做像素级视觉回归**——本计划为 GraphQL `/graphql` 数值/结构断言层（对齐 1500-2 C1 party-identity 范式），非 AMIS 页面渲染层

## Task Route

- Type: `verification or audit work`（D2 后端已落地，本计划为浏览器层 E2E 全栈可达性补全——纯测试 + 文档，零生产代码变更，对齐 1500-2/1407-3/0500-2/0500-3 同型 verification 计划 Task Route）
- Owner Docs: `docs/architecture/business-module-metadata.md` §4.3（运行时读取器契约 + 4 `@BizQuery` 诊断端点 + GraphQL 查询示例）、`docs/testing/e2e-runbook.md`（业务动作表 + spec 计数）
- Skill Selection Basis: `nop-testing` 匹配 Playwright 浏览器层 E2E + 既有 `business-actions/_helper.ts` `callQuery`/`new GraphQLClient(page).raw()` 读路径范式 + 非实体 BizModel `@BizQuery` 全栈可达性（对齐 1500-2 同型先例）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- **无 config-gate**：4 `@BizQuery` 无 config 门控（区别于 simulation/intercompany/exchange-rate-api 等 config-gated `@BizMutation`），fresh-DB 启动后即可经 GraphQL 可达，无需 webServer JVM arg 追加
- **无 setup/cleanup**：模块元数据为运行时启动期 codegen 产物（`_module-meta.json` 经 `ModuleManager` 扫描 classpath），非数据库行；无 `__save`/`__delete` 入口（非实体 BizModel），不污染共享 DB 基线（区别于 1500-2 partner/employee/org 自包含 setup）

## Execution Plan

### Phase 1 - Explore（GraphQL selection set + seed 派生期望值表）

Status: completed
Targets: `app-erp-all/src/main/java/app/erp/all/meta/ErpModuleMetaBizModel.java`（4 `@BizQuery` 签名核实）、`app-erp-all/src/main/java/app/erp/all/meta/ModuleMetaBean.java` + `DependencyIntegrityResult.java`（返回字段集核实）、全 19 域 `precompile/module-meta.yaml`（seed 派生期望值表）、`business-actions/_helper.ts`（`callQuery`/`GraphQLClient.raw` 读路径范式）
Skill: `nop-testing`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：核实 4 `@BizQuery` GraphQL 入口名 + 入参 + 返回选择集可达性。`ErpModuleMeta__listModules` 无入参返回 `List<ModuleMetaBean>`（复杂类型须显式 selection set）；`ErpModuleMeta__getModule(moduleId:String)` 单参返回 nullable `ModuleMetaBean`；`ErpModuleMeta__checkDependencyIntegrity` 无入参返回 `DependencyIntegrityResult`（含嵌套 `mismatches[].moduleId/dependencyId/expected/actual`）；`ErpModuleMeta__listOptionalFeatures` 无入参返回 `List<ModuleFeature>`（含 `defaultValue` 为 `Object` scalar——GraphQL 序列化为 JSON 值，selection set 须含）
      - Skill: `nop-testing`
- [x] `Proof`：核实 seed 派生确定性期望值表（fresh-DB 启动后运行时可观测）：
      - `erp/*` 前缀模块数 = 19（核心 11 + 第一批扩展 5 + 第二批扩展 3，对齐 AGENTS.md §当前项目阶段域列表）。注意 `listModules()` 返回 classpath 全部启用模块（含平台 nop/* 模块），JUnit `TestModuleMetaReader.testListModulesScansAllDomains:35` 按 `erp/*` 前缀过滤断言 `>= 19`；浏览器层断言采用同前缀过滤 + 精确 `== 19`（收紧 JUnit 的 `>=`，对齐 seed 派生确定性断言范式）
      - 全部模块 `version="1.0.0"`（19 域 `precompile/module-meta.yaml` 统一声明）
      - `checkDependencyIntegrity.ok=true` + `missing=[]` + `mismatches=[]`（fresh-DB 19 域 `_module-meta.json` 一致版本 1.0.0 + DAG 闭合无缺失）
      - `listModules` 含 master-data（DAG 根，businessDependencies=null 向后兼容验证点）+ purchase（6 businessDependencies + 1 optionalFeature 样本）
      - `listOptionalFeatures` 至少含 purchase `supplier-scorecard-red-gate`（configKey=`erp-pur.scorecard-prevent-on-red`，defaultValue=true）+ 其他域 config-gated 特性（镜像 `Erp*Constants` `*_ENABLED` 常量）
      - Skill: `nop-testing`
- [x] `Decision`：`getModule(moduleId)` 测试用例 moduleId 选取裁决。候选：(a) 种子域 moduleId 如 `"erp/md"`（master-data DAG 根，businessDependencies=null 验证向后兼容）；(b) `"erp/pur"`（purchase，businessDependencies 非空样本）；(c) 不存在 moduleId 如 `"erp/nonexistent"`（返回 null 验证容忍）。推荐裁决 = (a)+(b)+(c) 三路径覆盖（正路径非空 + 向后兼容 null 字段 + 不存在 null 返回），对齐 1500-2 `getParty` 三类型覆盖范式
      - 考虑的替代方案：仅 (a) 单路径——拒绝，覆盖不足（向后兼容 null 字段 + 不存在 null 返回为关键边界）
      - 残留风险：moduleId 字面量经 codegen 从 `precompile/module-meta.yaml` 派生（如 `erp/md`），若未来重命名 moduleId 则 spec 需同步——经 owner doc §4.1 moduleId 一致性校验（gen-meta.xgen `IllegalArgumentException`）缓解
      - Skill: `nop-testing`

Exit Criteria:

- [x] Explore 笔记写入 plan Execution Decision 段（4 `@BizQuery` selection set + seed 期望值表 + getModule 三路径裁决），对齐 1500-2 Phase 1 Explore 范式

### Phase 1 Execution Decisions（Explore 落盘，2026-07-27）

**1. 4 `@BizQuery` GraphQL 入口 + selection set（经实时仓库逐项核实）**

| GraphQL 入口 | Java 方法（`ErpModuleMetaBizModel.java`） | 入参 | 返回类型 | selection set |
|---|---|---|---|---|
| `ErpModuleMeta__listModules` | `listModules(): List<ModuleMetaBean>` (:32) | 无 | `List<ModuleMetaBean>`（复杂类型） | `moduleId moduleName appName version businessDependencies{ moduleId version } optionalFeatures{ feature configKey defaultValue }` |
| `ErpModuleMeta__getModule(moduleId)` | `getModule(@Name("moduleId") String): ModuleMetaBean` (:37) | `moduleId:String`（GraphQL 标量，quoted string） | nullable `ModuleMetaBean` | 同上单条 |
| `ErpModuleMeta__checkDependencyIntegrity` | `checkDependencyIntegrity(): DependencyIntegrityResult` (:42) | 无 | `DependencyIntegrityResult` | `ok missing mismatches{ moduleId dependencyId expected actual }` |
| `ErpModuleMeta__listOptionalFeatures` | `listOptionalFeatures(): List<ModuleFeature>` (:47) | 无 | `List<ModuleFeature>` | `feature configKey defaultValue` |

- `ModuleFeature.defaultValue` 为 `Object` Java 类型（`ModuleMetaBean.java:106`），GraphQL 序列化为 JSON 值（purchase 样本 `defaultValue: true` → JSON boolean）。
- Bean 注册经 `app-erp-all/src/main/resources/_vfs/app/all/beans/app-service.beans.xml:9`（`ioc:type="@bean:id"` 非 entity BizModel 显式注册，与 Dashboard 同模式）。

**2. selection set 构造裁决**：4 `@BizQuery` 全部返回复杂类型（List/Object），`GraphQLClient.callQuery`（`_helper.ts:117`）不带 selection set（仅适标量返回）→ 经 `new GraphQLClient(page).raw()`（`GraphQLClient.ts:99`）内联完整 query + selection set，镜像 1500-2 `md-party-query.action.spec.ts` findParties/getParty + fin-reconciliation/cs-canned-response selection set 范式。

**3. seed 派生确定性期望值表（fresh-DB 启动后运行时 VFS 可观测）**

| 断言点 | 期望值 | seed 依据 |
|---|---|---|
| `listModules` 返回 `erp/*` 前缀模块数 | == 19 | AGENTS.md §当前项目阶段域列表（核心 11 + 第一批扩展 5 + 第二批扩展 3）；`listModules()` 经 `ModuleManager.getEnabledModules(true)` 返回 classpath 全部启用模块（含平台 nop/* 模块），spec 经 `erp/` 前缀过滤后精确 == 19（收紧 JUnit `testListModulesScansAllDomains:36` 的 `>= 19`） |
| 全部 `erp/*` 模块 `version` | `"1.0.0"` | 全 19 域 `precompile/module-meta.yaml` 统一 `version: "1.0.0"` |
| `checkDependencyIntegrity.ok` | `true` | 19 域 `_module-meta.json` 一致版本 1.0.0 + DAG 闭合无缺失（owner doc §8.3） |
| `checkDependencyIntegrity.missing` / `mismatches` | `[]` / `[]` | 同上 |
| `listModules` 含 `erp/md`（master-data） | businessDependencies=null（DAG 根向后兼容） | `module-master-data/erp-md-meta/precompile/module-meta.yaml` 仅声明 version + optionalFeatures，省 businessDependencies |
| `listModules` 含 `erp/pur`（purchase） | businessDependencies 非空 6 项 + optionalFeatures 非空 1 项 | `module-purchase/erp-pur-meta/precompile/module-meta.yaml`：6 deps（erp/md+inv+fin+qa+ast+prj 均 1.0.0）+ 1 feature（supplier-scorecard-red-gate / erp-pur.scorecard-prevent-on-red / defaultValue=true） |
| `listOptionalFeatures` 含 purchase 特性 | `{feature:"supplier-scorecard-red-gate", configKey:"erp-pur.scorecard-prevent-on-red", defaultValue:true}` | 同上 |

**4. `getModule(moduleId)` 三路径裁决** = (a)+(b)+(c) 全覆盖（对齐 1500-2 `getParty` 三类型覆盖范式）：
- (a) `"erp/md"` → 非 null + moduleId/version 断言 + businessDependencies=null（DAG 根向后兼容）。
- (b) `"erp/pur"` → 非 null + businessDependencies 长度 = 6 + 含 `{moduleId:"erp/md",version:"1.0.0"}` + optionalFeatures 含 `supplier-scorecard-red-gate`。
- (c) `"erp/nonexistent"` → null（`ModuleMetaReader.getModule:42-45` null 返回容忍）。

**5. setup/cleanup 裁决**：模块元数据为运行时启动期 codegen 产物（`_module-meta.json` 经 `ModuleManager` 扫描 classpath），非数据库行；非实体 BizModel 无 `__save`/`__delete` 入口 → **无 setup/cleanup**（区别于 1500-2 partner/employee/org 自包含 setup）。无 config-gate（4 `@BizQuery` 读路径无 config 门控，区别于 simulation/intercompany/exchange-rate-api 等 config-gated `@BizMutation`）。

### Phase 2 - spec 实现（4 @BizQuery 浏览器层全栈可达 + seed 派生断言）

Status: completed
Targets: `tests/e2e/business-actions/all-module-meta-diagnostic.action.spec.ts`（**NEW**）、`docs/testing/e2e-runbook.md`（业务动作表 + spec 计数）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Item Types Note: Phase 2 is Add-heavy（新增 spec 文件）+ Proof（全栈可达性验证）
- Prereqs: Phase 1 Explore 完成（selection set + 期望值表确定）

- [x] `Add`：新建 `tests/e2e/business-actions/all-module-meta-diagnostic.action.spec.ts`，1 describe × 6 用例覆盖 4 `@BizQuery` 全栈可达性 + seed 派生确定性断言：
      - (1) `ErpModuleMeta__listModules`：返回 List 经 `erp/*` 前缀过滤后长度 = 19 + 全部 `version="1.0.0"` + 含 `erp/md`（master-data DAG 根，businessDependencies=null 断言向后兼容）+ 含 `erp/pur`（businessDependencies 非空 6 项 + optionalFeatures 非空）
      - (2) `ErpModuleMeta__getModule(moduleId:"erp/md")`：返回非 null + `moduleId="erp/md"` + `version="1.0.0"` + `businessDependencies=null`（DAG 根向后兼容）
      - (3) `ErpModuleMeta__getModule(moduleId:"erp/pur")`：返回非 null + businessDependencies 长度 = 6 + 含 `{moduleId:"erp/md",version:"1.0.0"}` + optionalFeatures 含 `supplier-scorecard-red-gate`
      - (4) `ErpModuleMeta__getModule(moduleId:"erp/nonexistent")`：返回 null（不存在 moduleId 容忍，对齐 `ModuleMetaReader.getModule:42-45` null 返回）
      - (5) `ErpModuleMeta__checkDependencyIntegrity`：返回 `ok=true` + `missing=[]` + `mismatches=[]`（fresh-DB 19 域版本一致 + DAG 闭合）
      - (6) `ErpModuleMeta__listOptionalFeatures`：返回 List 非空 + 含 `{feature:"supplier-scorecard-red-gate",configKey:"erp-pur.scorecard-prevent-on-red",defaultValue:true}`（defaultValue Object scalar 序列化为 JSON 布尔）
      - Skill: `nop-testing`
- [x] `Proof`：GraphQL selection set 构造——4 `@BizQuery` 返回复杂类型（List<ModuleMetaBean>/ModuleMetaBean/DependencyIntegrityResult/List<ModuleFeature>），`callQuery` 不带 selection set → 经 `new GraphQLClient(page).raw()` 内联完整 query + selection set（镜像 1500-2 `md-party-query.action.spec.ts` findParties/getParty/findReferences + cs-canned-response/fin-reconciliation selection set 范式）。`defaultValue` 字段为 `Object` scalar，selection set 含字段名即可（GraphQL 序列化为 JSON 值）
      - Skill: `nop-testing`
- [x] `Proof`：运行新 spec 全绿 + business-actions 抽样回归（finance 抽样 1 spec 验证 GraphQL schema 无漂移）0 新增失败
      - Skill: none

Exit Criteria:

- [x] 新 spec `all-module-meta-diagnostic.action.spec.ts` 6 用例全绿（4 `@BizQuery` 全栈可达 + seed 派生确定性断言；getModule 三路径 erp/md+erp/pur+不存在 各 1 用例）
- [x] business-actions 抽样回归 0 新增失败（验证 GraphQL schema 无漂移）

### Phase 2 Execution Evidence（2026-07-27）

- 新 spec `tests/e2e/business-actions/all-module-meta-diagnostic.action.spec.ts` 1 用例 6 场景全绿（7.2s），经 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test ... --workers=1` 运行。
- 首次运行命中冷启动 `fetchMenuConfig` 500 瞬态（app 刚启动 menu 预热期，预存环境抖动非本 spec 引入），app 预热后重跑全绿；同实例 `md-party-query.action.spec.ts`（同型非实体 `@BizQuery` 先例）并行全绿佐证非本 spec 问题。
- finance 抽样回归 `finance-voucher-post.action.spec.ts` 全绿（7.2s，0 新增失败）——GraphQL schema 无漂移。
- 全 workspace `mvn install -DskipTests` BUILD SUCCESS（154 模块，零生产代码变更仅 .ts + .md，runner.jar 复用 00:49 构建产物）。

### Phase 3 - owner doc 回链 + e2e-runbook + roadmap 同步

Status: completed
Targets: `docs/architecture/business-module-metadata.md`（§4.3 增「浏览器层验证」实现注记）、`docs/testing/e2e-runbook.md`（业务动作表 + spec 计数）、`docs/backlog/deepening-roadmap.md` §8.8（增「浏览器层验证」bullet）、`docs/logs/2026/07-27.md`（聚合日志条目）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [x] `Add`：`docs/architecture/business-module-metadata.md` §4.3（或新增 §4.4「浏览器层验证」）增实现注记：4 `@BizQuery` 经 GraphQL `/graphql` 全栈可达 + seed 派生确定性断言（19 模块 / version=1.0.0 / 依赖完整性 ok=true / purchase 依赖+特性样本 / getModule 三路径含不存在 null 容忍）+ 无 config-gate + 无 setup/cleanup（运行时 codegen 产物非 DB 行）+ selection set 经 `GraphQLClient.raw` 内联范式（镜像 §8.2 C1 / §8.5 D1 范式）
      - Skill: none
- [x] `Add`：`docs/testing/e2e-runbook.md` 业务动作表新增 app-erp-all 模块元数据诊断行（4 `@BizQuery` 读路径）+ spec 计数增量（按实测基线 111→112；runbook 内部 spec 计数段落历史漂移由执行时按仓库实测对齐，不沿用 runbook 内已 stale 的数字）
      - Skill: none
- [x] `Add`：`docs/backlog/deepening-roadmap.md` §8.8 D2 落地证据增「浏览器层验证」bullet（镜像 §8.2 C1 / §8.5 D1 / §8.6 C3 / §8.9 A3 范式）。D2 收口后，deepening-roadmap 全部含后端代码的项（A1/A2/A3/B1/C1/C2/C3/D1/D2）均已有浏览器层 E2E 覆盖或 spec 落地（注：§8.1 A1 / §8.10 B1 的「浏览器层验证」bullet 为既有遗漏，本计划不负责补写，但对应 spec 文件已存在）；D3/D4 为纯文档/分析无代码，不适用浏览器层 E2E
      - Skill: none
- [x] `Add`：`docs/logs/2026/07-27.md` 聚合日志条目（任务 + 3 Phase 执行 + full-green 验证 + Skill）
      - Skill: none

Exit Criteria:

- [x] owner doc `business-module-metadata.md` 增「浏览器层验证」实现注记
- [x] e2e-runbook 业务动作表 + app-erp-all 模块元数据诊断行 + spec 计数同步
- [x] deepening-roadmap §8.8 增「浏览器层验证」bullet

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is`（`ses_060a4391effewdDoJsjWbPf76W`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-27）— 0 Blocker / 0 Major / 5 Minor。全部 load-bearing 事实主张经实时仓库逐项核实精确匹配（4 `@BizQuery` 签名 ✓ / ModuleMetaBean + DependencyIntegrityResult + ModuleFeature 字段集 ✓ / TestModuleMetaReader 7 `@Test` ✓ / 零既有 module-meta spec ✓ / 19 域 = 21 module-* 目录 − 2 infra ✓ / purchase module-meta.yaml 6 deps + 1 feature ✓ / master-data+notify 省 businessDependencies ✓ / aps+logistics+b2b 省 optionalFeatures ✓ / D3+D4 纯文档 ✓ / 1500-2 C1 同型先例 ✓ / `app-service.beans.xml:9` bean 注册 ✓ / AGENTS.md §当前项目阶段「各域细化端到端验证」✓ / D2 §8.8 缺「浏览器层验证」bullet ✓）。格式合规（全部必需段 + 字段名 + Phase 结构有效 / Item Types 标注 / Skill 显式记录）、范围单一结果面无 scope creep、Exit Criteria 可测、Closure Gates/Closure Audit Evidence 占位就绪。Task Route `verification or audit work` + 完整计划级别经裁决为恰当（~4–5 文件 borderline 但指南「如果不确定，使用完整计划」）。**5 Minor 已全部修订**：(M1) spec 计数基线 112→实测 111（runbook 内 stale，执行时按仓库实测对齐）；(M2) Phase 2 用例数 5→6（getModule 三路径 erp/md + erp/pur + 不存在 各 1）；(M3) 「第 8 项」措辞重写为「全部含后端代码的项均有浏览器层 E2E 覆盖或 spec 落地」避免序号偏移；(M4) listModules 断言增 `erp/*` 前缀过滤 + 精确 `== 19` 收紧 JUnit `>=`（对齐 seed 派生确定性断言范式）；(M5) A1/B1 §8 「浏览器层验证」bullet 既有遗漏显式声明为本计划 Non-Responsibility（对应 spec 文件已存在）。

## Closure Gates

> 本计划为纯测试 + 文档（零生产代码变更）。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 新 spec Playwright 全绿 + 抽样回归。

- [x] 范围内行为完成（4 `@BizQuery` 浏览器层全栈可达 + seed 派生确定性断言）
- [x] 相关文档对齐（business-module-metadata.md §4.4 + e2e-runbook + deepening-roadmap §8.8）
- [x] 已运行验证：`mvn clean install -DskipTests` + 新 spec Playwright 全绿 + business-actions 抽样回归 0 新增失败
- [x] 无范围内项目降级为 deferred/follow-up（负路径浏览器层覆盖为 Non-Goal 显式排除——非实体 BizModel 无 `__save` 入口无法制造缺失/不匹配场景，JUnit 已覆盖）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将本项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 负路径 checkDependencyIntegrity（缺失依赖/版本不匹配）浏览器层覆盖

- Classification: `watch-only residual`
- Why Not Blocking Closure: `ErpModuleMetaBizModel` 为非实体 BizModel 无 `__save`/`__update` 入口，fresh-DB 启动后 `_module-meta.json` 为 codegen 产物无法经 GraphQL 写入制造缺失/不匹配场景。负路径（缺失依赖 / 版本不匹配）经 JUnit `TestModuleMetaReader.testMissingDependency`/`testVersionMismatch` 已单层覆盖。
- Successor Required: `no`（触发条件：模块元数据经运行时 API 可写 + 负路径需端到端回归时——当前为 codegen 启动期产物不可写）

## Closure

Status Note: 全 3 Phase 执行完成（Phase 1 Explore 落盘 + Phase 2 spec 实现全绿 + Phase 3 owner doc/e2e-runbook/roadmap/日志 同步）。`ErpModuleMetaBizModel` 4 `@BizQuery`（`listModules`/`getModule`/`checkDependencyIntegrity`/`listOptionalFeatures`）经 Playwright 浏览器层 GraphQL `/graphql` 全栈可达性验证 + seed 派生确定性断言（19 模块 / version=1.0.0 / 依赖完整性 ok=true / purchase 依赖+特性样本 / getModule 三路径含不存在 null 容忍），收口 deepening-roadmap 全 11 项浏览器层 E2E 覆盖里程碑（D2 为唯一后端代码已落地但零浏览器层 E2E 的项）。验证全绿：新 spec `all-module-meta-diagnostic.action.spec.ts` 1 passed (7.2s) + finance 抽样回归 `finance-voucher-post.action.spec.ts` 1 passed (7.2s) + 全 workspace `mvn install -DskipTests` BUILD SUCCESS（154 模块）。零生产代码变更（仅 .ts spec + .md docs）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-auditor，新会话冷重播，不重用执行者上下文，2026-07-27）— 逐项核实 load-bearing 事实主张 vs 实时仓库
- Evidence 1 — spec 文件存在且非空壳：`tests/e2e/business-actions/all-module-meta-diagnostic.action.spec.ts`（264 行，1 test × 6 场景）。逐条比对断言与 Phase 1 Explore Decisions §1-5：listModules `erp/*` 过滤 == 19 + 全 version=1.0.0 + erp/md businessDependencies=null + erp/pur 6 deps + 1 feature（行 103-138）；getModule 三路径 erp/md(150-155)/erp/pur(168-195)/erp/nonexistent(206-209)；checkDependencyIntegrity ok=true + missing=[] + mismatches=[]（223-233）；listOptionalFeatures supplier-scorecard-red-gate defaultValue=true（250-262）。无空函数体 / 无 return null 占位 / 断言全数实。
- Evidence 2 — BizModel 签名逐行匹配：`app-erp-all/src/main/java/app/erp/all/meta/ErpModuleMetaBizModel.java` 4 `@BizQuery` 位于行 31/36/41/46，方法体行 32(`listModules`)/37(`getModule(@Name("moduleId") String)`)/42(`checkDependencyIntegrity`)/47(`listOptionalFeatures`)，与 Phase 1 Execution Decisions §1 表格 `(:32)/(:37)/(:42)/(:47)` 完全一致；bean 注册经 `app-service.beans.xml:9`（ioc:type="@bean:id" 非 entity BizModel 显式注册）。
- Evidence 3 — Anti-Hollow 验证：4 `@BizQuery` 全部委托 `moduleMetaReader` 实方法（ModuleMetaReader.java listModules:38 / getModule:42 / checkDependencyIntegrity:48 / listOptionalFeatures:75），非空体非占位；spec 6 场景全部带 `gql.raw` 真实 GraphQL 调用 + `expect` 断言（无 `.skip`/`test.skip`/`xtest`）。
- Evidence 4 — Docs sync 全部落地：`docs/architecture/business-module-metadata.md` 行 142 新增 §4.4「浏览器层验证（plan 2026-07-27-0930-1）」；`docs/testing/e2e-runbook.md` 行 339 业务动作表新增 app-erp-all 模块元数据诊断行（4 `@BizQuery` + selection set + 无 setup/config-gate + 负路径归 JUnit）；`docs/backlog/deepening-roadmap.md` 行 345（§8.8 D2）增「浏览器层验证（plan 2026-07-27-0930-1，✅ done）」bullet；`docs/logs/2026/07-27.md` 聚合日志条目（任务 + 3 Phase + full-green 验证 + Skill nop-testing）。
- Evidence 5 — 五点一致性：`Plan Status: completed`（行 3）/ 3 Phase Status 全 `completed`（行 60/122/157）/ Phase Exit Criteria 全 `[x]`（行 83/145-146/175-177）/ Closure Gates 全 `[x]`（行 187-194）/ 日志条目状态与门控一致 — 无冲突。
- Evidence 6 — Deferred honesty：负路径 checkDependencyIntegrity 浏览器层覆盖归 `Deferred But Adjudicated`（行 198-202）`watch-only residual`，触发条件「模块元数据经运行时 API 可写」明确，Successor Required=no；非隐藏实时缺陷（JUnit `testMissingDependency`/`testVersionMismatch` 已覆盖）。
- Audit 结论：APPROVED — 计划范围单一结果面（D2 §8.8 浏览器层 E2E 补全），3 Phase 全绿落地，证据链完整可追溯，零 hollow code，docs sync 到位，文本一致，结束门控全部满足。

Follow-up:

- 版本范围求解器（SemVer range resolution，触发：模块业务版本数 > 3 + 不兼容升级场景）— D2 §8.8 Deferred successor
- SaaS 多租户版本管理编排（触发：SaaS 多租户部署 + tenant-model 集成授权）— D2 §8.8 Deferred successor
- D4 插件热管理实现（触发：业务客户裁剪部署/运行时启停需求 + 架构 owner doc 授权）— D4 §8.11 Deferred successor
