# 2026-07-24-2200-1-cross-domain-code-abstraction 跨域重复代码抽象

> Plan Status: draft
> Review Hold: Phase 1 直接修改 `model/*.orm.xml`（移除 `use-approval` tag），属 `ai-autonomy-policy.md` 中的 `ask first` 保护区域，需**人工批准**后方可规划/实施。当前"架构决策（2026-07-24）"为前序 AI 会话作出，非已确认的人工批准（policy line 11）。该 ORM 变更与 Java 重构耦合（xbiz `<source>` 清理仅在 `use-approval` 移除后才不回归），无法直接推迟。解阻路径二选一（属作者/人工范围决策，审查者不得代决）：(a) 取得人工对 ORM `use-approval` 移除范围的明确批准，并在 Phase 1 增补 `Decision` 人工批准门控项；或 (b) 将 ORM `use-approval` 移除 + xbiz `<source>` 清理拆为独立人工门控后续计划，本计划仅保留 Java 基类/Processor 拆分。推荐采用与 Phase 5 相同的保护模式（Phase 5 已对 ORM `domain` 追加设 Explore Exit Gate + 后续计划 deferred）。
> Last Reviewed: 2026-07-24
> Source: `docs/plans/2026-07-24-2100-1-comprehensive-code-design-audit.md`（F8 cross-domain redundancy findings）
> Related: `docs/plans/2026-07-24-1400-1-shared-kernel-extraction.md`（共享内核先例）、`docs/plans/2026-07-24-1400-2-cross-domain-naming-constant-convergence.md`（命名常量先例）
> Design: `docs/design/processor-delegation-auto-gen.md`（已废弃，保留作为历史参考）<br>Architecture: `docs/architecture/service-layer-orchestration.md`、`docs/architecture/processor-extension-pattern.md`
> Audit: required

## Current Baseline

综合审查 F8 指出 5 个层面的跨域重复代码（实时仓库复核，2026-07-24）。架构划分原则：Processor 是多步骤方法的通用分解器（≥3 步），BizModel 仅做入口委托（见 `docs/architecture/service-layer-orchestration.md`）。

### 发现 A — 编排 boilerplate 重复（每 mutation 共享编排骨架）

**现状**：41 个 `*Processor.java` 中含 ~250 个自定义 mutation 方法（审批 5 件套 + cancel + 域特有）。当前按"每实体一个 Processor"组织，导致：
- 一实体 5-15 个 mutation 混在一个文件，关注点不聚焦
- 同类 mutation（如 approve）在 24 个实体间各自独立实现 `requireEntity → validateDocStatus → [hooks] → doTransition → save`
- `cancel` 有 28 个在 Processor + 17 个内联在 BizModel，来源不一
- `use-approval` ORM 标签引入 `approval-support.xbiz` 继承链（含 5 个 XLang mutation），与 Java Processor 实现在两条路径竞争

**架构决策（2026-07-24）**：
1. **每 mutation 一个独立 Processor**。命名规则 `<Entity><Method>Processor`。BizModel Java 直接 `@Inject Processor + @BizMutation` 调用。
2. **不使用 xbiz `<source>` 委托**，xbiz 仅用于 Delta 定制覆盖。
3. **去掉 `use-approval` ORM 标签**，不继承 `approval-support.xbiz`。状态转换 + 审计字段 + 可选 wf 启动全部在 Processor Java 中。
4. **保留 `useWorkflow`**（独立属性，控制 `nopFlowId` 列；wf 启动由 `AbstractSubmitForApprovalProcessor` 条件执行）。

**正确抽象**：每类 mutation 抽一个基类（`AbstractApproveProcessor<T>`、`AbstractCancelProcessor<T>` 等），同类 mutation 共享编排骨架 + hook 模板。每个实体的具体 Processor 仅实现 `validate*`/`do*`/`after*` 域特有步骤。

### 发现 B — View.xml gen-control 脚本：项目绕过 `domain` → UI 控件映射链

**现状**：~50+ 处内联 `gen-control` 脚本重复数字/日期格式（`{ type: 'number', kilometer: true, precision: 2 }`），~40+ 处状态徽章重复，~48 个审批按钮动作重复。

**平台事实**（`../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md` §控件匹配链）：
- 前端控件按 `control` → `domain` → `stdDomain` → `stdDataType` 匹配
- ORM 中定义 `domain="amount"`（precision=18, scale=2）自动传播到 XMeta → `control.xlib` 自动选择 `input-number` 控件
- view.xml 中的 `gen-control` 脚本是**应急手段**，应在 ORM/XMeta 层通过 domain 定义消除
- 状态徽章应通过 xmeta 的 `ui:renderer` 或自定义 `control.xlib` 标签统一处理

**结论**：gen-control 脚本重复的根因是 ORM domain 定义不足 + 项目级 `control.xlib` 缺失。正确修复链为：ORM domain → XMeta → `control.xlib` 控件映射 → view.xml 移除内联脚本。

### 发现 C — 测试重复

**现状**（与 `docs-for-ai` 无冲突，需项目级抽象）：

| 类别 | 文件数 | 重复度 | 平台机制支持 |
|---|---|---|---|
| CRUD Smoke 测试 `*CrudSmoke.java` | 19（含 notify） | ~85% 相同 | `JunitAutoTestCase` 已提供，但基类需项目级抽象 |
| Web Pages 测试 `*WebPagesTest.java` | 19 | 100% 相同 | 无平台级抽象，需项目级 |
| Codegen 测试 `*CodeGen.java` + `*WebCodeGen.java` | 38（每域 2 个） | 100% 相同 | 无平台级抽象，需项目级 |
| FrozenClockExtension `*FrozenClockExtension.java` | 15 | ~95% 相同 | 无平台级抽象，需项目级 |
| Dashboard 测试 `*Dashboard.java` | 11 | ~70% 相同 | 无平台级抽象，需项目级 |
| Report Rendering 测试 `*ReportRendering.java` | 11 | ~80% 相同 | 无平台级抽象，需项目级 |

### 发现 D — Dashboard BizModel 重复

**现状**：11 个 `*DashboardBizModel.java` 重复 `nz()`/`toBigDecimal()`/`monthKey()`/GroupBy QueryBean 构造等辅助方法。可在项目级共享 `DashboardUtil`。

**平台事实**：DQL（`../nop-entropy/docs-for-ai/02-core-guides/dql-query.md`）已提供 `QueryBean` + `fields` + `aggFunc` 的结构化查询能力，无需手写 SQL-lib。DB 级聚合可直接用 `IOrmTemplate.findListByQuery(QueryBean)`。

### 发现 E — 缺少项目级共享模块

当前项目无 `module-common-*` 模块。19 个 `module-*` 独立平级，无法共享 Java 基类/测试基类。跨模块共享应优先通过 `I*Biz` 接口（非类继承），共享工具类可通过 `app-erp-common-*` 模块。

## Goals

1. **每 mutation 一个 Processor**：将现有 41 个"每实体一 Processor"拆分为 ~250 个"每 mutation 一 Processor"，强制架构纪律。
2. **消除同类 mutation 编排 boilerplate**：创建 per-mutation 基类（`AbstractApproveProcessor<T>`、`AbstractCancelProcessor<T>` 等），同类 mutation 共享编排骨架。每个实体具体 Processor 仅实现 hook 方法。
3. **消除 View.xml gen-control 脚本**：通过 ORM domain 定义完善 + 项目级 `control.xlib` 封装，消除 ~130+ 处内联格式脚本。
4. **消除测试文件跨域重复**：引入项目级共享测试基类，使 ~75 个重复测试文件收敛。
5. **消除 Dashboard BizModel 辅助方法重复**：引入 `DashboardUtil` 共享工具类。
6. **建立项目级共享模块**：创建 `module-common-test` + `module-common-service`。

## Non-Goals

- **不改已有设施的行为语义**（抽象是纯重构）。
- **不涉及审批流 .xwf 配置**（不动 .xwf）。
- **不使用 xbiz `<source>` 委托**（BizModel Java 直接调用 Processor，xbiz 仅用于 Delta 定制覆盖）。
- **不使用 `use-approval` tag**（从 ORM 模型移除，不再继承 `approval-support.xbiz`）。
- **不修改 nop-entropy 平台代码**（`approval-support.xbiz`、`IApprovableBiz` 等保持原样不动）。
- **保留 `useWorkflow`**（与 `use-approval` 独立，需要 nop-wf 的实体单独配置）。

## Task Route

- Type: `architecture change`（新增 Maven 模块 + per-mutation 基类 + 拆分重构）
- Owner Docs: `docs/architecture/service-layer-orchestration.md`、`docs/architecture/processor-extension-pattern.md`
- Skill: `nop-backend-dev`

## Infrastructure And Config Prereqs

- 新增 Maven 模块：`module-common-test`（物理目录 `module-common-test/`，遵循 `module-*` 命名约定）、`module-common-service`。
- 回滚策略：单次合并提交可回滚单个 Phase。

## Execution Plan

### Phase 0 — 公共模块创建

Status: planned
Targets: 根 `pom.xml`、`app-erp-all/pom.xml`、各域 service/web `pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无

- [ ] `Add`：创建 `module-common-test`（物理目录 `module-common-test/`，groupId `io.nop.app`，artifactId `app-erp-common-test`，依赖 `nop-core`、`nop-autotest-junit`、`nop-graphql-core`，java 17）。
      - 包结构：`src/main/java/app/erp/common/test/`
      - 寄存器根 `pom.xml` `<modules>` 和 `app-erp-all/pom.xml`（test scope，optional=true）。
      - 每个域的 `erp-*-web` 测试模块在其 `pom.xml` 追加 `<dependency><groupId>io.nop.app</groupId><artifactId>app-erp-common-test</artifactId><scope>test</scope></dependency>`。
      - Skill: `nop-backend-dev`
- [ ] `Add`：创建 `module-common-service`（物理目录 `module-common-service/`，artifactId `app-erp-common-service`，依赖 `nop-core`、`nop-dao`、`nop-graphql-core`）。
      - 包结构：`src/main/java/app/erp/common/service/`
      - 寄存器根 `pom.xml` 和 `app-erp-all/pom.xml`。
      - 每个域的 `erp-*-service` 模块在其 `pom.xml` 追加 `<dependency><groupId>io.nop.app</groupId><artifactId>app-erp-common-service</artifactId></dependency>`。
      - Skill: `nop-backend-dev`
- [ ] `Add`：确认所有 19 个域 service 和 web 模块的依赖均已显式声明。使用脚本验证：`for d in module-*/erp-*-service/pom.xml; do rg "app-erp-common-service" $d; done`。

Exit Criteria:

- [ ] `mvn compile -pl module-common-test,module-common-service` 通过
- [ ] `rg "app-erp-common" module-*/**/pom.xml` 每 web+service 模块至少 1 命中

### Phase 1 — Per-mutation 基类创建（module-common-service）

Status: planned
Targets: `module-common-service`（每类 mutation 一个抽象基类）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 完成

- [ ] `Add`：在 `module-common-service` 创建 `AbstractProcessor`（所有 Processor 的根基类）。
      - 通用辅助方法：`requireEntity(id)`、`checkEntityNotNull(entity)`、`validateDocStatus(entity, allowedStatuses...)`、`requireEntityForUpdate(id)`
      - `@Inject protected IDaoProvider daoProvider`（各子类共用）
      - Skill: `nop-backend-dev`

- [ ] `Add`：创建审批 5 方法基类（每类一个独立抽象类，专注单一流程骨架）：
      - `AbstractApproveProcessor<T>`：`doApprove(id, context)` → `requireEntity + validateDocStatus(SUBMITTED) + validateBusinessRules + beforeStateChange + doStatusTransition(APPROVED) + afterStateChange + save + return`
      - `AbstractRejectProcessor<T>`：`doReject(id, context)` → 同上，目标状态 REJECTED
      - `AbstractSubmitForApprovalProcessor<T>`：`doSubmit(id, context)` → 同上，目标状态 SUBMITTED。
        额外职责：**可选 wf 启动**——检查实体 xmeta 是否有 `wf:wfName`，有则调 `IWorkflowManager.newWorkflow()` + `ApprovalFlowHelper.start()`。无则仅做状态转换。
        获取实体 xmeta 的方式：构造函数强制传入 `bizObjName`（如 `"ErpPurOrder"`），通过 `IBizObjectManager.getBizObject(bizObjName).getObjMeta().getProp("wf:wfName")` 读取。不使用 `entity.getClass().getSimpleName()`（AOP 代理类名不可靠，且方法执行时尚无 entity 实例）。
        `@Inject IBizObjectManager bizObjectManager`、`@Inject IWorkflowManager workflowManager`。
      - `AbstractReverseApproveProcessor<T>`：`doReverseApprove(id, context)` → 同上，目标状态 SUBMITTED
      - `AbstractWithdrawApprovalProcessor<T>`：`doWithdraw(id, context)` → 同上，目标状态 DRAFT
      - 每个基类提供 hook 抽象方法：`validateBusinessRules(T entity)`、`beforeStateChange(T entity)`、`afterStateChange(T entity)`
      - 所有基类继承 `AbstractProcessor`。每个基类构造函数强制传入 `bizObjName`：`protected AbstractApproveProcessor(String bizObjName) { this.bizObjName = bizObjName; }`
      - Skill: `nop-backend-dev`

- [ ] `Fix`：从 ORM 模型中去掉 `use-approval` tag（独立于 `useWorkflow`）：
      - 逐域扫描 `module-*/model/*.orm.xml`，在 `entity` 的 `tagSet` 属性中找到 `use-approval` 并移除
      - `useWorkflow="true"`（控制 `nopFlowId` 列）**保留不动**——与 `use-approval` 独立
      - 记录变更范围到 `docs/analysis/use-approval-removal-scope.md`
      - Skill: `nop-backend-dev`

- [ ] `Proof`：`mvn clean install -DskipTests` 触发 codegen 增量重生成（scoped 到有 ORM 变更的域）。
      - 验证 `I*Biz` 不再继承 `IApprovableBiz`
      - 验证 `_*Biz.xbiz` 不再继承 `approval-support.xbiz`
      - 验证全仓 BUILD SUCCESS
      - Skill: `nop-backend-dev`

- [ ] `Fix`：清理用户层 xbiz 文件：
      - 删除不再需要的 5 个 mutation `<source>` 块（`submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval`）
      - 若 xbiz 无其他内容（`<actions/>` 空），可删除整个 xbiz 文件
      - 保留 xbiz 仅当有域特有定制存在

- [ ] `Add`：创建通用状态翻转基类：
      - `AbstractCancelProcessor<T>`：`doCancel(id, context)` → `requireEntity + validateDocStatus(可取消状态集) + validateCanCancel + beforeCancel + doStatusTransition(CANCELLED) + afterCancel + save`
      - `AbstractSettleProcessor<T>`、`AbstractReverseSettlementProcessor<T>` 等（按实际出现频率决定是否需要独立基类；仅出现 2-3 次的 mutation 直接用具体 Processor 手写，不必抽基类）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `module-common-service` 中编译通过所有抽象基类
- [ ] `mvn compile -pl module-common-service` 0 errors
- [ ] 所有 ORM 模型 `tagSet` 中不再有 `use-approval`
- [ ] `rg "use-approval" module-*/model/*.orm.xml` 返回 0
- [ ] `grep "IApprovableBiz" module-*/erp-*-dao/src/main/java/**/I*.java` 返回 0（I*Biz 不再继承 IApprovableBiz）
- [ ] `grep "approval-support" module-*/erp-*-service/src/main/resources/_vfs/**/*.xbiz` 返回 0（xbiz 不再继承 approval-support.xbiz）

### Phase 2 — 现有 Processor 拆分为 per-mutation Processor + BizModel 接线

Status: planned
Targets: 41 个现有 `*Processor.java` 拆分为 ~250 个 per-mutation Processor + 各 `*BizModel.java` 新增 `@BizMutation` 委托
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Delete`
- Prereqs: Phase 1 完成

- [ ] `Explore`：逐域盘点 41 个 Processor + 17 个内联 cancel BizModel，输出拆分清单到 `docs/analysis/per-mutation-processor-split-plan.md`。
      - 记录每个实体的 mutation 列表、每个 mutation 的步骤序列、涉及的 `@Inject` 服务
      - 标识哪些 mutation 可共用基类（如 approve 用 `AbstractApproveProcessor<T>`），哪些需手写（域特有逻辑）
      - Skill: `nop-backend-dev`

- [ ] `Add`：按 Mutation 创建具体 Processor：
      - 每 mutation 一个文件，命名 `<Entity><Method>Processor`（如 `ErpPurOrderApproveProcessor`）
      - 审批类 mutation 继承 Phase 1 的对应基类
      - cancel 类 mutation 继承 `AbstractCancelProcessor<T>`
      - 域特有 mutation（如 `convertToOrder`、`closePeriod`）直接继承 `AbstractProcessor` 手写编排
      - 每个 Processor 仅 `@Inject` 自己需要的服务（不共享，允许注入重复）
      - Skill: `nop-backend-dev`

- [ ] `Delete`：删除原有 41 个合并的 `*Processor.java` 文件（内容已全部分配到 per-mutation Processor）

- [ ] `Fix`：更新 BizModel：
      - 删除原有 `@Inject XxxProcessor`（老合并 Processor）
      - 新增 `@Inject` 每个 per-mutation Processor（如 `@Inject ErpPurOrderApproveProcessor erpPurOrderApproveProcessor`）
      - 已有 `@BizMutation` 方法改为 `return erpPurOrderApproveProcessor.approve(id, svcCtx)`（一行委托）
      - 原 17 个内联 cancel BizModel 方法同样改为委托到对应 Processor
      - 简单例外（≤2 步、无关跨域编排的查询/单步状态翻转）：保留在 BizModel，不创建 Processor
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `rg "@BizMutation" module-*/erp-*-service/src/main/java/ -g "*Processor.java"` 返回 0（Processor 上无 `@BizMutation`）
- [ ] 每个 mutation 在 BizModel 中为一行委托：`public|protected ... method(...) { return xxxProcessor.method(id, svcCtx); }`
- [ ] `mvn compile` 全仓通过
- [ ] purchase/sales 各一次审批流程 GraphQL 调用测试通过（行为不变）
- [ ] 用户层 xbiz 中无 `<source>return inject(*Processor)</source>` 行（BizModel Java 已接管）
- [ ] wf 回调测试：模拟 nop-wf 结束，`bizObj.invoke('approve', {id})` 正确路由到 BizModel `@BizMutation approve()` → per-mutation Processor

### Phase 3 — 测试基类抽象

Status: planned
Targets: `module-common-test/` + 各域测试文件
Skill: `nop-testing`

- Item Types: `Add | Fix`
- Prereqs: Phase 0 完成

- [ ] `Add`：在 `module-common-test` 创建 `ErpCrudSmokeTestBase`。
      - 封装 `@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)`。
      - 封装 `IGraphQLEngine graphQLEngine` 注入 + `executeRpc` 辅助。
      - 提供抽象方法让子类注入实体名和测试数据。
      - Skill: `nop-testing`
- [ ] `Add`：在 `module-common-test` 创建 `AbstractFrozenClockExtension`。
      - 15 个域共用的 `REFERENCE_DATE`、`FROZEN_CLOCK`、`beforeAll`/`afterAll` 集中到基类。
      - Skill: `nop-testing`
- [ ] `Add`：在 `module-common-test` 创建 `AbstractWebPagesTest`。
      - 封装 19 个域 `*WebPagesTest.java` 的共同逻辑。
      - Skill: `nop-testing`
- [ ] `Fix`：重构 19 个 CRUD Smoke 测试 → 继承 `ErpCrudSmokeTestBase`。
      - 删除 `executeRpc`、`@NopTestConfig` 重复。
      - Skill: `nop-testing`
- [ ] `Fix`：重构 15 个 `*FrozenClockExtension` → 继承 `AbstractFrozenClockExtension`。
      - Skill: `nop-testing`
- [ ] `Fix`：重构 19 个 `*WebPagesTest` → 继承 `AbstractWebPagesTest`。
      - Skill: `nop-testing`
- [ ] `Fix`：重构 38 个 codegen 测试 → 继承共享基类或合并。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 19 个 CRUD Smoke 子类中 `grep "executeRpc"` 仅命中基类
- [ ] `mvn test -pl module-purchase,module-sales`（抽样 2 域）测试 GREEN

### Phase 4 — Dashboard BizModel 工具抽象

Status: planned
Targets: `module-common-service/`（`DashboardUtil.java`）、各域 `*DashboardBizModel.java`（11 个）
Skill: `nop-backend-dev` — 需读 `../nop-entropy/docs-for-ai/02-core-guides/dql-query.md`

- Item Types: `Add | Fix`
- Prereqs: Phase 0 完成

- [ ] `Add`：在 `module-common-service` 创建 `DashboardUtil`。
      - 静态方法：`nz(BigDecimal)`、`toBigDecimal(Object)`、`monthKey(LocalDate)`、`safeDivide`。
      - 静态方法：`buildGroupByQuery(...)` — 封装 DQL QueryBean GROUP BY 模式。
      - 静态方法：`buildTrendData(...)` — 通用按日期桶聚合。
      - Skill: `nop-backend-dev`
- [ ] `Fix`：重构 11 个 Dashboard BizModel，使用 `DashboardUtil` 替换内联实现。
      - 删除内联 `nz`/`toBigDecimal`/`monthKey`。
      - 用 `DashboardUtil.buildGroupByQuery()` 替换重复的 QueryBean 构造。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 11 个 Dashboard BizModel 中 `grep "private static.*nz\|toBigDecimal\|monthKey"` 返回 0
- [ ] `mvn compile` 涉及域全部通过

### Phase 5 — View.xml gen-control 脚本消除（通过 Domain + control.xlib）

Status: planned
Targets: ORM domain 定义评估（仅文档）、各域 `control.xlib`（新建）、各域 view.xml（替换内联脚本）
Skill: `nop-frontend-dev` — 必须读 `../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md` §控件匹配链

- Item Types: `Add | Fix | Decision`
- Prereqs: 无（不依赖 Phase 0 的 Java 模块）

- [ ] `Explore`（**Exit Gate**）：核实项目 ORM 模型中 `amount`/`quantity`/`unitPrice`/`taxAmount` 等字段是否已有 `domain` 定义。这是 Phase 5 的前提决策门。
      - 若已有 `domain` → Phase 5 进入下一项（创建项目级 `control.xlib`）。
      - 若 `domain` 普遍缺失 → 记录需补充的 domain 清单，Phase 5 范围缩水至仅 `control.xlib` 封装 + 状态徽章统一；ORM domain 补充 deferred 到单独计划。
      - 无论哪条路，此探索结果决定 Phase 5 剩余工作内容，结果记录在 `docs/analysis/domain-definition-audit.md`。
      - Skill: `nop-frontend-dev`
- [ ] `Add`：若已有 domain 但无项目级控件映射，在项目 `_vfs` 下创建自定义 `control.xlib`（`x:extends="/nop/web/xlib/control.xlib"` 追加项目映射）。
      - 例如 `<edit-amount x:prototype="edit-decimal" precision="2"/>`（平台 `control.xlib` 有 `edit-decimal`，precision=5；项目覆盖 precision=2）。
      - 状态徽章：通过 `ui:renderer` 或 `xlib` 标签统一处理。
      - Skill: `nop-frontend-dev`
- [ ] `Fix`：在各域 view.xml 中替换内联 `gen-control` 脚本为 `xlib` 标签调用。
      - 替换数字/日期格式 ~90+ 处。
      - 替换审批 6 按钮块为标签宏 ~48 处。
      - 替换状态徽章 ~40 处。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 项目级 `control.xlib` 文件存在，包含 `amount`/`quantity`/`unitPrice`/`date`/`datetime` 控件映射
- [ ] 至少 3 个域的 view.xml 中 `grep "gen-control.*type.*number.*kilometer"` 减少（使用 xlib 标签替代）
- [ ] `mvn compile` 全通过（view.xml 不编译，但框架加载路径验证）

### Phase 6 — 全面验证

Status: planned
Targets: 全仓构建 + 测试 + 审批流程回归
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-5 全部完成

- [ ] `Proof`：`mvn clean install -DskipTests`（154+ 模块 BUILD SUCCESS）
- [ ] `Proof`：`mvn test`（0 Failure / 0 Error）
- [ ] `Proof`：审批流程 GraphQL 测试——对 purchase/sales 各执行一次审批流程，确认行为无变化

Exit Criteria:

- [ ] 全仓构建 + 测试通过
- [ ] 审批流程回归验证通过

## Draft Review Record

- 计划审计迭代 1: `ses_06d2d0334ffeiJ1FjxyAZKj1BX` — 原 Phase 1/2 设计（xbiz GenApprovalDelegation + AbstractApprovalProcessor）。
- 计划审计迭代 2: `ses_06ce44ea6ffe7dFlV1X56yfpMT` — Processor 粒度分析，结论：每实体一 Processor 正确。
- 计划审计迭代 3: 2026-07-24 人工裁定：每 mutation 一 Processor + BizModel Java 直接调用（废弃 xbiz 委托方案）。
- 计划审计迭代 4: 2026-07-24 使用-approval 决策：去掉 `use-approval` tag，状态转换 + 审计字段 + 可选 wf 启动全部在 Processor Java。Phase 1/2 更新含 ORM 模型修改步骤。待本轮 revision 后执行独立审计。
- 计划审计迭代 5（独立草案审查，本会话）: **needs revision — BLOCKER**。Phase 1 的 ORM `use-approval` 移除触及 `model/*.orm.xml` 保护区域（`ai-autonomy-policy.md` §保护区域：`ask first`，"规划或实施前需要人工批准"）。计划内"架构决策"为 AI 会话作出，不构成 policy line 11 认可的人工批准证据。该变更与 Java 重构耦合（无法无损推迟），故计划保持 `draft`（加 `Review Hold`）。Baseline 量化核对通过（实时仓库）：42 Processor（计划 41，off-by-one，minor）、11 DashboardBizModel ✓、15 FrozenClockExtension ✓、19 CrudSmoke ✓、38 CodeGen ✓、38 IApprovableBiz 引用 ✓、`module-common-*` 不存在 ✓、`use-approval` 存在于 38 ORM 行 ✓。其余问题均为 Minor（Task Route 用 `Skill:` 而非模板的 `Skill Selection Basis:`；个别 Phase Exit 重复全仓 `mvn compile` 违反执行规则 7 但可视为解除后续阻塞所需；Closure Gates 缺"结束证据存在于文件中"项，Closure 段已有占位）。Minor 留待下游结束/深度审计。解阻后改 `active`。

## Closure Gates

- [ ] 范围内行为完成（7 个 Phase 全部退出标准满足）
- [ ] 相关文档对齐（`service-layer-orchestration.md`、`processor-extension-pattern.md` 已更新）
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test` + 审批回归
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理执行

## Deferred But Adjudicated

### ORM `domain` 补充（若 Phase 5 explore 发现 domain 缺失）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ORM 模型是保护区域，未经人工批准不得修改。Phase 5 评估结果若需 ORM domain 追加，应单独成计划。
- Successor Required: `yes`

### 低频率 mutation 是否抽基类

- Classification: `optimization candidate`
- Why Not Blocking Closure: 仅出现 2-3 次的 mutation（如 `convertToOrder`）手写具体 Processor 即可，不必为不复现的重复过度设计。待某个模式出现 ≥5 次后再抽基类。
- Successor Required: `no`

## Closure

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending>
- Evidence: <pending>

Follow-up:

- （无范围内容）
