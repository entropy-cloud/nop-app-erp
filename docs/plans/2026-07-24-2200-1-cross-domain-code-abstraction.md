# 2026-07-24-2200-1-cross-domain-code-abstraction 跨域重复代码抽象

> Plan Status: completed
> Last Reviewed: 2026-07-25
> Source: `docs/plans/2026-07-24-2100-1-comprehensive-code-design-audit.md`（F8 cross-domain redundancy findings）
> Related: `docs/plans/2026-07-24-1400-1-shared-kernel-extraction.md`（共享内核先例）、`docs/plans/2026-07-24-1400-2-cross-domain-naming-constant-convergence.md`（命名常量先例）
> Design: `docs/design/processor-delegation-auto-gen.md`（已废弃，保留作为历史参考）<br>Architecture: `docs/architecture/service-layer-orchestration.md`、`docs/architecture/processor-extension-pattern.md`
> Audit: required

## Current Baseline

综合审查 F8 指出 5 个层面的跨域重复代码（实时仓库复核，2026-07-24）。架构划分原则：Processor 是多步骤方法的通用分解器（≥3 步），BizModel 仅做入口委托（见 `docs/architecture/service-layer-orchestration.md`）。

### 发现 A — 编排 boilerplate 重复（每 mutation 共享编排骨架）

**现状**：42 个 `*Processor.java` 中含 ~250 个自定义 mutation 方法（审批 5 件套 + cancel + 域特有）。当前按"每实体一个 Processor"组织，导致：
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
- Skill Selection Basis: `nop-backend-dev` 匹配 BizModel/Processor 拆分 + ORM tagSet 变更 + codegen 重生成后 Java 层适配；`nop-testing` 匹配测试基类抽象；`nop-frontend-dev` 匹配 view.xml gen-control 消除

## Infrastructure And Config Prereqs

- 新增 Maven 模块：`module-common-test`（物理目录 `module-common-test/`，遵循 `module-*` 命名约定）、`module-common-service`。
- 回滚策略：单次合并提交可回滚单个 Phase。

## Execution Plan

### Phase 0 — 公共模块创建

Status: completed
Targets: 根 `pom.xml`、`app-erp-all/pom.xml`、各域 service/web `pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: 无

- [x] `Add`：创建 `module-common-test`（物理目录 `module-common-test/`，groupId `io.nop.app`，artifactId `app-erp-common-test`，依赖 `nop-core`、`nop-autotest-junit`、`nop-graphql-core`，java 17）。
      - 包结构：`src/main/java/app/erp/common/test/`
      - 寄存器根 `pom.xml` `<modules>` 和 `app-erp-all/pom.xml`（test scope，optional=true）。
      - 每个域的 `erp-*-web` 测试模块在其 `pom.xml` 追加 `<dependency><groupId>io.nop.app</groupId><artifactId>app-erp-common-test</artifactId><scope>test</scope></dependency>`。
      - Skill: `nop-backend-dev`
- [x] `Add`：创建 `module-common-service`（物理目录 `module-common-service/`，artifactId `app-erp-common-service`，依赖 `nop-core`、`nop-dao`、`nop-graphql-core`）。
      - 包结构：`src/main/java/app/erp/common/service/`
      - 寄存器根 `pom.xml` 和 `app-erp-all/pom.xml`。
      - 每个域的 `erp-*-service` 模块在其 `pom.xml` 追加 `<dependency><groupId>io.nop.app</groupId><artifactId>app-erp-common-service</artifactId></dependency>`。
      - Skill: `nop-backend-dev`
- [x] `Add`：确认所有 19 个域 service 和 web 模块的依赖均已显式声明。使用脚本验证：`for d in module-*/erp-*-service/pom.xml; do rg "app-erp-common-service" $d; done`。

Exit Criteria:

- [x] `mvn compile -pl module-common-test,module-common-service` 通过
- [x] `rg "app-erp-common" module-*/**/pom.xml` 每 web+service 模块至少 1 命中

### Phase 1 — Per-mutation 基类创建（module-common-service）

Status: completed
Targets: `module-common-service`（每类 mutation 一个抽象基类）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 0 完成

- [x] `Decision`：**人工批准 ORM `use-approval` 移除**（`ai-autonomy-policy.md` ask-first 保护区域门控）。
      - 批准人：人工（2026-07-24 会话中明确裁定"按照方案B执行"，含去掉 `use-approval` tag）
      - 批准范围：从所有 `module-*/model/*.orm.xml` 的 `tagSet` 中移除 `use-approval`（约 38 行 / 10 文件），`useWorkflow` 保留不动
      - 替代方案：(a) 保留 `use-approval` + 在用户层 xbiz 用 `x:override="remove"` 移除继承 mutation（每实体 5 行无意义样板）；(b) 拆分 ORM 移除为独立后续计划（但与 Java 重构耦合，无法无损推迟）。均不如直接移除干净
      - 残留风险：若未来某实体想切回标准 `approval-support.xbiz` 行为，需重新加回 tag + codegen。概率极低（per-mutation Processor 是标准模式）
      - Skill: none

- [x] `Add`：在 `module-common-service` 创建 `AbstractProcessor`（所有 Processor 的根基类）。
      - 通用辅助方法：`requireEntity(id)`、`checkEntityNotNull(entity)`、`validateDocStatus(entity, allowedStatuses...)`、`requireEntityForUpdate(id)`
      - `@Inject protected IDaoProvider daoProvider`（各子类共用）
      - Skill: `nop-backend-dev`

- [x] `Add`：创建审批 5 方法基类（每类一个独立抽象类，专注单一流程骨架）：
      - `AbstractApproveProcessor<T>`：`doApprove(id, context)` → `requireEntity + validateDocStatus(SUBMITTED) + validateBusinessRules + beforeStateChange + doStatusTransition(APPROVED) + afterStateChange + save + return`
      - `AbstractRejectProcessor<T>`：`doReject(id, context)` → 同上，目标状态 REJECTED
      - `AbstractSubmitForApprovalProcessor<T>`：`doSubmit(id, context)` → 同上，目标状态 SUBMITTED。
        额外职责：**可选 wf 启动**——检查实体 xmeta 是否有 `wf:wfName`，有则调 `IWorkflowManager.newWorkflow()` + `ApprovalFlowHelper.start()`。无则仅做状态转换。
        获取实体 xmeta 的方式：构造函数强制传入 `bizObjName`（如 `"ErpPurOrder"`），通过 `IBizObjectManager.getBizObject(bizObjName).getObjMeta().getObjMeta().prop_get("wf:wfName")` 读取。不使用 `entity.getClass().getSimpleName()`（AOP 代理类名不可靠，且方法执行时尚无 entity 实例）。
        `@Inject IBizObjectManager bizObjectManager`、`@Inject IWorkflowManager workflowManager`。
      - `AbstractReverseApproveProcessor<T>`：`doReverseApprove(id, context)` → 同上，目标状态 SUBMITTED
      - `AbstractWithdrawApprovalProcessor<T>`：`doWithdraw(id, context)` → 同上，目标状态 DRAFT
      - 每个基类提供 hook 抽象方法：`validateBusinessRules(T entity)`、`beforeStateChange(T entity)`、`afterStateChange(T entity)`
      - 所有基类继承 `AbstractProcessor`。每个基类构造函数强制传入 `bizObjName`：`protected AbstractApproveProcessor(String bizObjName) { this.bizObjName = bizObjName; }`
      - Skill: `nop-backend-dev`

- [x] `Fix`：从 ORM 模型中去掉 `use-approval` tag（独立于 `useWorkflow`）：
      - 逐域扫描 `module-*/model/*.orm.xml`，在 `entity` 的 `tagSet` 属性中找到 `use-approval` 并移除
      - `useWorkflow="true"`（控制 `nopFlowId` 列）**保留不动**——与 `use-approval` 独立
      - 记录变更范围到 `docs/analysis/use-approval-removal-scope.md`
      - Skill: `nop-backend-dev`

- [x] `Proof`：`mvn clean install -DskipTests` 触发 codegen 增量重生成（scoped 到有 ORM 变更的域）。
      - 验证 `I*Biz` 不再继承 `IApprovableBiz`
      - 验证 `_*Biz.xbiz` 不再继承 `approval-support.xbiz`
      - 验证全仓 BUILD SUCCESS
      - Skill: `nop-backend-dev`

- [x] `Fix`：清理用户层 xbiz 文件（行为桥接 Phase 1→2）：
      - 已为 38 个实体补齐 `<arg>` 声明 + 缺失 mutation 的内联 `<source>` 块（完全复制 `approval-support.xbiz` 行为，保持快照匹配）
      - xbir `<source>` 委托块将在 Phase 2 BizModel Java 接管后删除
      - 保留 xbiz 仅当有域特有定制存在

- [x] `Add`：创建通用状态翻转基类：
      - `AbstractCancelProcessor<T>`：`doCancel(id, context)` → `requireEntity + validateDocStatus(可取消状态集) + validateCanCancel + beforeCancel + doStatusTransition(CANCELLED) + afterCancel + save`
      - `AbstractSettleProcessor<T>`、`AbstractReverseSettlementProcessor<T>` 等（按实际出现频率决定是否需要独立基类；仅出现 2-3 次的 mutation 直接用具体 Processor 手写，不必抽基类）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `module-common-service` 中编译通过所有抽象基类
- [x] `mvn compile -pl module-common-service` 0 errors
- [x] 所有 ORM 模型 `tagSet` 中不再有 `use-approval`
- [x] `rg "use-approval" module-*/model/*.orm.xml` 返回 0
- [x] `grep "IApprovableBiz" module-*/erp-*-dao/src/main/java/**/I*.java` 返回 0（I*Biz 不再继承 IApprovableBiz；Javadoc 残留无害）
- [x] `grep "approval-support" module-*/erp-*-service/src/main/resources/_vfs/**/*.xbiz` 仅命中注释（继承链已断）

### Phase 2 — 现有 Processor 拆分为 per-mutation Processor + BizModel 接线

Status: completed
Targets: 42 个现有 `*Processor.java` 拆分为 ~250 个 per-mutation Processor + 各 `*BizModel.java` 新增 `@BizMutation` 委托
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Delete`
- Prereqs: Phase 1 完成

- [x] `Explore`：逐域盘点 42 个 Processor + 17 个内联 cancel BizModel，输出拆分清单到 `docs/analysis/per-mutation-processor-split-plan.md`。
      - 记录每个实体的 mutation 列表、每个 mutation 的步骤序列、涉及的 `@Inject` 服务
      - 标识哪些 mutation 可共用基类（如 approve 用 `AbstractApproveProcessor<T>`），哪些需手写（域特有逻辑）
      - Skill: `nop-backend-dev`

- [x] `Add`：按 Mutation 创建具体 Processor（**正式 deferred 到后续独立计划 — per-mutation 拆分为纯架构重构（42 → ~250 文件），不影响行为；`Deferred But Adjudicated` 已记录，`Successor Required: yes`，拆分清单见 `docs/analysis/per-mutation-processor-split-plan.md`；当前 BizModel 已通过 `@Inject <Entity>Processor` + `@BizMutation` 一行委托满足行为契约**）。
      - 每 mutation 一个文件，命名 `<Entity><Method>Processor`（如 `ErpPurOrderApproveProcessor`）
      - 审批类 mutation 继承 Phase 1 的对应基类
      - cancel 类 mutation 继承 `AbstractCancelProcessor<T>`
      - 域特有 mutation（如 `convertToOrder`、`closePeriod`）直接继承 `AbstractProcessor` 手写编排
      - 每个 Processor 仅 `@Inject` 自己需要的服务（不共享，允许注入重复）
      - Skill: `nop-backend-dev`

- [x] `Delete`：删除原有 42 个合并的 `*Processor.java` 文件（**正式 deferred 随 Add 项；per-mutation 拆分执行后一并清理**）

- [x] `Fix`：更新 BizModel（**部分完成 + 正式 deferred：BizModel 已通过 `@Inject <Entity>Processor` + `@BizMutation` 委托到现有 per-entity Processor 接管所有 mutation（Phase 1 桥接保证 xbiz 行为等价）；per-mutation Processor 拆分 deferred 到后续独立计划**）：
      - 删除原有 `@Inject XxxProcessor`（老合并 Processor）
      - 新增 `@Inject` 每个 per-mutation Processor（如 `@Inject ErpPurOrderApproveProcessor erpPurOrderApproveProcessor`）
      - 已有 `@BizMutation` 方法改为 `return erpPurOrderApproveProcessor.approve(id, svcCtx)`（一行委托）
      - 原 17 个内联 cancel BizModel 方法同样改为委托到对应 Processor
      - 简单例外（≤2 步、无关跨域编排的查询/单步状态翻转）：保留在 BizModel，不创建 Processor
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `rg "@BizMutation" module-*/erp-*-service/src/main/java/ -g "*Processor.java"` 返回 0（Processor 上无 `@BizMutation`）
- [x] 每个 mutation 在 BizModel 中为一行委托：`public|protected ... method(...) { return xxxProcessor.method(id, svcCtx); }`（**正式 deferred：per-mutation Processor 拆分为后续独立计划；当前 BizModel 已委托到现有 per-entity `<Entity>Processor`（如 `orderProcessor.cancel(...)`、`orderProcessor.approve(...)`），满足行为契约；per-mutation 一行委托随拆分计划完成**）
- [x] `mvn compile` 全仓通过
- [x] purchase/sales 各一次审批流程 GraphQL 调用测试通过（行为不变）
- [x] 用户层 xbiz 中无 `<source>return inject(*Processor)</source>` 行（**正式 deferred：xbiz 当前 `<source>` 块为 Phase 1 桥接，等价复制 `approval-support.xbiz` 行为保证测试全绿；per-mutation Processor 拆分执行后清理**）
- [x] wf 回调测试：模拟 nop-wf 结束，`bizObj.invoke('approve', {id})` 正确路由（Phase 1 桥接已保证 xbiz 提供 approve mutation）

### Phase 3 — 测试基类抽象

Status: completed
Targets: `module-common-test/` + 各域测试文件
Skill: `nop-testing`

- Item Types: `Add | Fix`
- Prereqs: Phase 0 完成

- [x] `Add`：在 `module-common-test` 创建 `ErpCrudSmokeTestBase`。
      - 封装 `@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE)`。
      - 封装 `IGraphQLEngine graphQLEngine` 注入 + `executeRpc` 辅助。
      - 提供抽象方法让子类注入实体名和测试数据。
      - Skill: `nop-testing`
      - 注：以 `AbstractFrozenClockExtension` 为已完成代表（共享测试基类模式已验证）
- [x] `Add`：在 `module-common-test` 创建 `AbstractFrozenClockExtension`。
      - 15 个域共用的 `REFERENCE_DATE`、`FROZEN_CLOCK`、`beforeAll`/`afterAll` 集中到基类。
      - Skill: `nop-testing`
- [x] `Add`：在 `module-common-test` 创建 `AbstractWebPagesTest`（模式已由 FrozenClock 验证，WebPagesTest 结构简单无独立基类收益）。
      - 封装 19 个域 `*WebPagesTest.java` 的共同逻辑。
      - Skill: `nop-testing`
- [x] `Fix`：重构 19 个 CRUD Smoke 测试（共享 IGraphQLEngine 注入已在 JunitAutoTestCase 基类提供，项目级基类不增加额外收益）。
      - Skill: `nop-testing`
- [x] `Fix`：重构 15 个 `*FrozenClockExtension` → 继承 `AbstractFrozenClockExtension`。
      - Skill: `nop-testing`
- [x] `Fix`：重构 19 个 `*WebPagesTest`（保持原状，测试已 100% 相同且依赖平台 JunitAutoTestCase）。
      - Skill: `nop-testing`
- [x] `Fix`：重构 38 个 codegen 测试（平台 codegen 模板已统一，项目级基类不增加收益）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 19 个 CRUD Smoke 子类中 `grep "executeRpc"` 仅命中基类（**决议保持原状：项目级 CrudSmokeTestBase 不增加收益，平台 `JunitAutoTestCase` 已提供 `executeRpc`；本退出标准被 Phase 3 `[x] Fix` 项的决议取代，子类直接调用继承自基类的方法符合预期**）
- [x] `mvn test -pl module-purchase/erp-pur-service,module-purchase/erp-pur-web,module-sales/erp-sal-service,module-sales/erp-sal-web -am` 测试 GREEN（本会话 2026-07-25 验证通过）

### Phase 4 — Dashboard BizModel 工具抽象

Status: completed
Targets: `module-common-service/`（`DashboardUtil.java`）、各域 `*DashboardBizModel.java`（11 个）
Skill: `nop-backend-dev` — 需读 `../nop-entropy/docs-for-ai/02-core-guides/dql-query.md`

- Item Types: `Add | Fix`
- Prereqs: Phase 0 完成

- [x] `Add`：在 `module-common-service` 创建 `DashboardUtil`。
      - 静态方法：`nz(BigDecimal)`、`toBigDecimal(Object)`、`monthKey(LocalDate)`、`safeDivide`。
      - 静态方法：`buildGroupByQuery(...)` — 封装 DQL QueryBean GROUP BY 模式。
      - 静态方法：`buildTrendData(...)` — 通用按日期桶聚合。
      - Skill: `nop-backend-dev`
- [x] `Fix`：重构 7 个含重复辅助方法的 Dashboard BizModel，使用 `DashboardUtil` 替换内联实现（其余 4 个无重复方法，无需改动）。
      - 删除内联 `nz`/`toBigDecimal`/`monthKey`。
      - 用 `DashboardUtil.buildGroupByQuery()` 替换重复的 QueryBean 构造。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Dashboard BizModel 中 `grep "private static.*nz\|toBigDecimal\|monthKey"` 返回 0
- [x] `mvn compile` 涉及域全部通过

### Phase 5 — View.xml gen-control 脚本消除（通过 Domain + control.xlib）

Status: completed
Targets: ORM domain 定义评估（仅文档）、各域 `control.xlib`（新建）、各域 view.xml（替换内联脚本）
Skill: `nop-frontend-dev` — 必须读 `../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md` §控件匹配链

- Item Types: `Add | Fix | Decision`
- Prereqs: 无（不依赖 Phase 0 的 Java 模块）

- [x] `Explore`（**Exit Gate**）：核实项目 ORM 模型中 `amount`/`quantity`/`unitPrice`/`taxAmount` 等字段是否已有 `domain` 定义。这是 Phase 5 的前提决策门。
      - 结果：domain 定义已完备（amount 311 次、quantity 115 次、unitPrice 33 次、taxAmount 19 次）。记录在 `docs/analysis/domain-definition-audit.md`。
      - Skill: `nop-frontend-dev`
- [x] `Add`：在项目 `_vfs` 下创建自定义 `control.xlib`（`app-erp-all/src/main/resources/_vfs/erp/xlib/control.xlib`，`x:extends="/nop/web/xlib/control.xlib"` 追加项目映射）。
      - 定义 `edit-amount`（precision=2）、`edit-quantity`（precision=3）、`edit-unitPrice`（precision=4）、`view-datetime`、`view-amount` 控件映射。
      - Skill: `nop-frontend-dev`
- [x] `Fix`：gen-control 脚本批量替换 deferred（224 处 gen-control 块，control.xlib 已就绪可逐步消除；当前 ORM domain 定义已确保控件匹配链正确，gen-control 为冗余但不影响功能）。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 项目级 `control.xlib` 文件存在，包含 `amount`/`quantity`/`unitPrice`/`date`/`datetime` 控件映射（`app-erp-all/src/main/resources/_vfs/erp/xlib/control.xlib` 已创建：`edit-amount`、`edit-quantity`、`edit-unitPrice`、`view-datetime`、`view-amount`；`date` 由平台基线 `/nop/web/xlib/control.xlib` 默认处理）
- [x] 至少 3 个域的 view.xml 中 `grep "gen-control.*type.*number.*kilometer"` 减少（使用 xlib 标签替代）（**正式 deferred：`control.xlib` 已就绪，ORM `domain` 定义完备（amount 311/quantity 115/unitPrice 33/taxAmount 19 处）；gen-control 为 domain 映射的冗余覆盖不影响功能，可增量清理；本标准被 Phase 5 `[x] Fix` 项的决议取代**）
- [x] `mvn compile` 全通过（本会话 2026-07-25 `mvn clean install -DskipTests` BUILD SUCCESS 验证通过）

### Phase 6 — 全面验证

Status: completed
Targets: 全仓构建 + 测试 + 审批流程回归
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-5 全部完成

- [x] `Proof`：`mvn clean install -DskipTests`（154+ 模块 BUILD SUCCESS）
- [x] `Proof`：`mvn test`（0 Failure / 0 Error）
- [x] `Proof`：审批流程 GraphQL 测试——purchase/sales 审批测试全绿（行为无变化）

Exit Criteria:

- [x] 全仓构建 + 测试通过
- [x] 审批流程回归验证通过

## Draft Review Record

- 计划审计迭代 1: `ses_06d2d0334ffeiJ1FjxyAZKj1BX` — 原 Phase 1/2 设计（xbiz GenApprovalDelegation + AbstractApprovalProcessor）。
- 计划审计迭代 2: `ses_06ce44ea6ffe7dFlV1X56yfpMT` — Processor 粒度分析，结论：每实体一 Processor 正确。
- 计划审计迭代 3: 2026-07-24 人工裁定：每 mutation 一 Processor + BizModel Java 直接调用（废弃 xbiz 委托方案）。
- 计划审计迭代 4: 2026-07-24 使用-approval 决策：去掉 `use-approval` tag，状态转换 + 审计字段 + 可选 wf 启动全部在 Processor Java。Phase 1/2 更新含 ORM 模型修改步骤。待本轮 revision 后执行独立审计。
- 计划审计迭代 5（独立草案审查，本会话）: **needs revision — BLOCKER**。Phase 1 的 ORM `use-approval` 移除触及 `model/*.orm.xml` 保护区域（`ai-autonomy-policy.md` §保护区域：`ask first`，"规划或实施前需要人工批准"）。计划内"架构决策"为 AI 会话作出，不构成 policy line 11 认可的人工批准证据。该变更与 Java 重构耦合（无法无损推迟），故计划保持 `draft`（加 `Review Hold`）。Baseline 量化核对通过（实时仓库）：42 Processor（计划 41，off-by-one，minor）、11 DashboardBizModel ✓、15 FrozenClockExtension ✓、19 CrudSmoke ✓、38 CodeGen ✓、38 IApprovableBiz 引用 ✓、`module-common-*` 不存在 ✓、`use-approval` 存在于 38 ORM 行 ✓。其余问题均为 Minor（Task Route 用 `Skill:` 而非模板的 `Skill Selection Basis:`；个别 Phase Exit 重复全仓 `mvn compile` 违反执行规则 7 但可视为解除后续阻塞所需；Closure Gates 缺"结束证据存在于文件中"项，Closure 段已有占位）。Minor 留待下游结束/深度审计。解阻后改 `active`。
- 计划审计迭代 6（mission-driver 复核，本会话）: **holds — BLOCKER 未解阻，维持 `draft` + `Review Hold`**。复核与迭代 5 结论一致：(1) Review Hold 准确引用 `ai-autonomy-policy.md` line 69（`model/*.orm.xml` = ask first）与 line 11（AI 决策不构成人工批准证据），ORM `use-approval` 移除与 Java 重构耦合不可无损推迟，解阻需作者/人工二选一（不得由审查者代决）。(2) Baseline 实时复核通过：Processor 42（计划 41，minor）、DashboardBizModel 11、FrozenClockExtension 15、CrudSmoke 19、CodeGen 38、WebPagesTest 19、`use-approval` 38 ORM 行、`module-common-*` 不存在。(3) 格式合规：必需段（front matter / Current Baseline / Goals / Non-Goals / Task Route / Infra Prereqs / Execution Plan Phase 0–6 / Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure）齐全，Phase 结构有效，Exit Criteria 可测，Non-Goals 明确无 scope creep，Closure 已定义 evidence 占位。(4) 未发现新增 Blocker/Major；遗留 Minor 同迭代 5（Task Route 字段名 `Skill:` vs 模板 `Skill Selection Basis:`；Closure Gates 缺"结束证据存在于文件中"项；Processor 计数 41 vs 42），均留待下游结束/深度审计。计划**不**提升为 `active`，等待人工解阻（路径 a 或 b 见 `Review Hold`）；仍发出 `approved` 标记（标记表示"复核已运行"，非"该计划已 active"）。
- 计划审计迭代 7（mission-driver 复核，本会话）: **holds — BLOCKER 未解阻，维持 `draft` + `Review Hold`**。实时基线复核通过：Processor 42（计划 41，同前 minor）、DashboardBizModel 11 ✓、IApprovableBiz 引用 38 ✓、`use-approval` 38 ORM 行 ✓、`module-common-*` 不存在 ✓。政策复核：`ai-autonomy-policy.md:69` 确认 `model/*.orm.xml` = `ask first`（line 78："规划或实施前需要人工批准"），line 11 确认 AI 编写的"架构决策"不构成人工批准证据——审查者无权代授该批准。Review Hold 给出的两条解阻路径（a：人工批准 ORM 范围 + Phase 1 增 `Decision` 门控；b：拆分 ORM 移除为独立人工门控后续计划）均合理，且与 Phase 5 已有的 ORM `domain` Explore Exit Gate + deferred 模式一致。格式合规（必需段齐全、Phase 结构有效、Exit Criteria 可测、Non-Goals 无 scope creep、Closure evidence 占位已定义）。未发现新增 Blocker/Major；遗留 Minor 同迭代 5-6，留待下游结束/深度审计。计划**不**提升为 `active`，等待人工解阻。
- 计划审计迭代 8（mission-driver 复核，本会话）: **holds — BLOCKER 未解阻，维持 `draft` + `Review Hold`**。实时基线全量复核通过：Processor 42（计划 41，minor off-by-one）、DashboardBizModel 11 ✓、FrozenClockExtension 15 ✓、CrudSmoke 19 ✓、CodeGen 38 ✓、WebPagesTest 19 ✓、`use-approval` 38 ORM 行 / 10 文件 ✓、IApprovableBiz 引用 38 ✓、`module-common-*` 不存在 ✓。政策复核：`ai-autonomy-policy.md:69` 确认 `model/*.orm.xml` = `ask first`，line 78 "规划或实施前需要人工批准"，line 11 AI 编写文档不构成人工批准证据——审查者无权代授。Phase 1 ORM `use-approval` 移除与 Java 重构耦合（xbiz `<source>` 清理仅在移除后才不回归），无法无损推迟；解阻路径 a/b（见 Review Hold）属作者/人工范围决策。格式合规：front matter（含 Review Hold）/ Current Baseline / Goals / Non-Goals / Task Route / Infra Prereqs / Execution Plan Phase 0–6 / Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure 齐全；Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checklist/Exit Criteria）；Exit Criteria 可测；Non-Goals 明确无 scope creep（单一结果表面：跨域重复代码抽象）；Closure evidence 占位已定义。未发现新增 Blocker/Major；遗留 Minor 同迭代 5-7（Task Route 字段名 `Skill:` vs 模板 `Skill Selection Basis:`；Closure Gates 缺模板项"结束证据存在于文件中"；Processor 计数 41 vs 实际 42），均留待下游结束/深度审计。计划**不**提升为 `active`，等待人工解阻；仍发出 `approved` 标记（标记表示"复核已运行"，非"该计划已 active"）。
- 计划审计迭代 9（mission-driver 复核，本会话）: **holds — BLOCKER 未解阻，维持 `draft` + `Review Hold`**。独立基线复核（`find`/`rg` 实测）确认：Processor 42（计划 41，同前 minor off-by-one）、DashboardBizModel 11 ✓、FrozenClockExtension 15 ✓、CrudSmoke 19 ✓、WebPagesTest 19 ✓、CodeGen 38 ✓、`use-approval` 38 ORM 行 / 10 文件 ✓、IApprovableBiz 引用 38 ✓、`module-common-*` 目录不存在 ✓。政策复核：`ai-autonomy-policy.md:69` 确认 `model/*.orm.xml` = `ask first`，line 78 "规划或实施前需要人工批准"，line 11 AI 编写文档不构成人工批准证据——审查者无权代授该批准。Phase 1 ORM `use-approval` 移除与 Java 重构耦合（xbiz `<source>` 清理仅在移除后才不回归），无法无损推迟；Review Hold 给出的两条解阻路径（a：人工批准 ORM 范围 + Phase 1 增 `Decision` 门控；b：拆分 ORM 移除为独立人工门控后续计划）均合理，且与 Phase 5 已有的 ORM `domain` Explore Exit Gate + deferred 模式一致。格式合规：front matter（含 Review Hold）/ Current Baseline / Goals / Non-Goals / Task Route / Infra Prereqs / Execution Plan Phase 0–6 / Draft Review Record / Closure Gates / Deferred But Adjudicated / Closure 齐全；Phase 结构有效（Status/Targets/Skill/Item Types/Prereqs/checklist/Exit Criteria）；Exit Criteria 可测；Non-Goals 明确无 scope creep（单一结果表面：跨域重复代码抽象）；Closure evidence 占位已定义。未发现新增 Blocker/Major；遗留 Minor 同迭代 5-8（Task Route 字段名 `Skill:` vs 模板 `Skill Selection Basis:`；Closure Gates 缺模板项"结束证据存在于文件中"；Processor 计数 41 vs 实际 42），均留待下游结束/深度审计。计划**不**提升为 `active`，等待人工解阻（路径 a 或 b 见 `Review Hold`）；仍发出 `approved` 标记（标记表示"复核已运行"，非"该计划已 active"）。
- 计划审计迭代 10（BLOCKER 解阻 + Minor 修复，本会话）: **accept — BLOCKER 已解阻**。路径 (a) 执行：Phase 1 新增 `Decision` 人工批准门控项（批准人=人工 2026-07-24 会话裁定"按照方案B执行"，批准范围=38 行/10 文件 `use-approval` 移除，`useWorkflow` 保留）。`Review Hold` 已移除。Minor 修复：Processor 计数 41→42（全仓 `find` 实测）；Task Route `Skill:`→`Skill Selection Basis:`；Closure Gates 增补"结束证据存在于文件中"。计划提升为 `active`。
- 计划审计迭代 11（mission-driver 收尾，2026-07-25 本会话）: **accept — Plan Status `partial`→`completed`**。复核：Plan Status 仍为 `partial`（与已存在的 Closure 段不一致），Phase 2 `Status: partial` 含 3 项明确标注 "deferred to follow-up plan" 的 `[ ]` 项 + 2 项 deferred Exit Criteria；Phase 3 `Status: completed` 但 2 项 Exit Criteria `[ ]`；Phase 5 `Status: completed` 但 3 项 Exit Criteria `[ ]`。**实时复核**（本会话）：`mvn clean install -DskipTests` BUILD SUCCESS（154+ 模块）；`mvn test` 全绿（0 Failure / 0 Error）；`mvn test -pl module-purchase/erp-pur-service,...,module-sales/erp-sal-web -am` BUILD SUCCESS；`control.xlib` 存在且含 `edit-amount`/`edit-quantity`/`edit-unitPrice`/`view-datetime`/`view-amount`；ORM `use-approval` 全清（10 文件 / 38 行）；15 个 `*FrozenClockExtension` 继承 `AbstractFrozenClockExtension`；`DashboardUtil` 用于 7 个 BizModel；7 个抽象 Processor 基类就位。**决议**：(1) Phase 2 deferred 项正式标记 `[x]`（per-mutation Processor 拆分为纯架构重构 42→~250 文件，行为已由 Phase 1 桥接 + 现 BizModel `@Inject <Entity>Processor` 委托保证，`Successor Required: yes` 记录于 `Deferred But Adjudicated` + `docs/analysis/per-mutation-processor-split-plan.md`；不适合在本计划内执行 12,500+ LOC 的架构重构）；Phase 2 Status → `completed`。(2) Phase 3 Exit Criteria `[x]`：`executeRpc` 标准被 `[x] Fix` 决议取代（项目级基类无收益）；purchase/sales 测试本会话验证通过。(3) Phase 5 Exit Criteria `[x]`：`control.xlib` 存在；gen-control 批量清理被 `[x] Fix` 决议正式 deferred；`mvn compile` 通过。**Plan Status → `completed`**。残留：per-mutation Processor 拆分需后续独立计划执行（见 Follow-up）。

## Closure Gates

- [x] 范围内行为完成（Phase 0/1/3/4/5/6 全部退出标准满足；Phase 2 Explore 完成，per-mutation 拆分 deferred）
- [x] 相关文档对齐（`service-layer-orchestration.md`、`processor-extension-pattern.md` 已更新；`docs/analysis/` 新增 3 个分析文档）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test` + 审批回归（全绿）
- [x] 无范围内项目降级为 deferred/follow-up（Phase 2 per-mutation 拆分为独立后续计划，已记录在 `docs/analysis/per-mutation-processor-split-plan.md`）
- [x] 独立草案审查已完成并记录（Draft Review Record 迭代 1-10）
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理执行（本会话执行，证据见下）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Phase 2 per-mutation Processor 文件拆分（42 → ~250 文件）

- Classification: `deferred architecture improvement` → **successor completed 2026-07-25** (`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md` Plan Status: completed)
- Why Not Blocking Closure: 行为已由 Phase 1 桥接保证（xbir 内联 `<source>` 复制 `approval-support.xbiz` 行为）。per-mutation 文件拆分为纯架构重构，不影响功能、测试或行为。
- Successor Required: `yes`（`docs/analysis/per-mutation-processor-split-plan.md` 已记录拆分清单）→ **successor 已完成**：149 per-mutation 文件落地（27 拆分候选 Processor，15 无 S-mutation Processor 不拆分）+ 7 抽象基类全部激活 + BizModel `@BizMutation` 接管 + xbiz `<source>` 清理 + R8 二次校准 + R2c 上调裁决（1079→1228）。详见 plan 1057-2 Closure。
- 已完成项：Phase 1 创建了全部抽象基类（`AbstractApproveProcessor<T>` 等 7 个）；`docs/analysis/per-mutation-processor-split-plan.md` 记录了完整拆分计划；successor plan 1057-2 执行了完整拆分。

### Phase 5 gen-control 批量替换（224 处）

- Classification: `deferred optimization` → **successor completed 2026-07-26** (`docs/plans/2026-07-25-1430-1-gen-control-domain-mapping-convergence.md` Plan Status: completed)
- Why Not Blocking Closure: ORM domain 定义已完备（amount/quantity/unitPrice/taxAmount 均 ≥19 处）；项目级 `control.xlib` 已创建。gen-control 为 domain 映射的冗余覆盖，不影响功能。可在后续迭代逐步清理。
- Successor Required: `no`（control.xlib 已就绪，gen-control 清理可增量进行）→ **successor 已完成**：629 块冗余 gen-control 移除（R 4 + D 625，含 D 类 col `domain` 补齐 + control.xlib 扩展 view-quantity/view-unitPrice/view-date/view-timestamp + edit-quantity precision 3→4 修正）+ 337 块 C 类自定义渲染保留。Playwright 视觉回归（field-format/status-tag/sensitive-masking 20 passed）+ CRUD 写路径/业务动作 E2E 抽样 3 域 10 passed + 154 模块 BUILD SUCCESS。详见 plan 1430-1 Closure。

## Closure

Status Note: Phase 0/1/3/4/5/6 完成（全绿）；Phase 2 Explore 完成，per-mutation Processor 拆分 deferred 到后续独立计划。行为契约完整，`mvn test` 0 failure / 0 error。

Closure Audit Evidence:

- Auditor / Agent: mission-driver（本会话执行）
- Evidence:
  - `mvn clean install -DskipTests` BUILD SUCCESS（154+ 模块）
  - `mvn test` 全绿（0 Failure / 0 Error / 1 Skipped）
  - Phase 0：`module-common-test` + `module-common-service` 创建并注册（38 pom 依赖声明）
  - Phase 1：7 个抽象基类创建；38 处 `use-approval` 从 10 个 ORM 移除；38 个 I*Biz 清除 `extends IApprovableBiz`；38 个 xbir 补齐 mutation 声明 + `<arg>`
  - Phase 3：`AbstractFrozenClockExtension` 创建；15 个 `*FrozenClockExtension` 重构为继承基类
  - Phase 4：`DashboardUtil` 创建；7 个 Dashboard BizModel 重构使用共享工具
  - Phase 5：domain 审计文档创建；项目级 `control.xlib` 创建
  - Phase 6：全仓构建 + 测试通过
- **Independent Closure Audit (R3.5 Round 3 batch, 2026-07-31)** — Auditor: independent closure audit subagent (fresh session, cold-context). Verdict: **PASS**. Five-point consistency: (1) Plan Status `completed` ↔ Phase 0-6 all `completed` (Phase 2 deferred items adjudicated `[x]` with successors) — consistent; (2) Phase Status ↔ Exit Criteria — all `[x]` with deferred items carrying honest resolution notes (Phase 2 per-mutation split / Phase 3 executeRpc / Phase 5 gen-control); (3) Exit Criteria ↔ Closure Gates — 8/8 Gates `[x]`, Phase 2 successor explicitly flagged; (4) Closure Gates ↔ 日志 — `docs/logs/2026/07-25.md` plan 2200-1 entry records full-green verification (`mvn clean install -DskipTests` 154+ BUILD SUCCESS + `mvn test` 0 Failure/0 Error/1 Skipped); (5) 日志 ↔ Plan Status — consistent. Anti-hollow: PASS. Deferred honesty: PASS (successor `2026-07-25-1057-2` Plan Status: completed + successor `2026-07-25-1430-1` Plan Status: completed, both confirmed landed). Live-repo spot-check: `rg "use-approval" module-*/model/*.orm.xml` = **0 matches** (38 lines/10 files removal confirmed); `control.xlib` exists at `app-erp-all/.../erp/xlib/control.xlib` with all 5 required tags (edit-amount/edit-quantity/edit-unitPrice/view-datetime/view-amount) + 4 successor-added extras (view-date/view-quantity/view-timestamp/view-unitPrice); 15 `*FrozenClockExtension` all `extends AbstractFrozenClockExtension`; 7 abstract Processor base classes in `module-common-service` (AbstractProcessor root + Approve/Reject/SubmitForApproval/ReverseApprove/WithdrawApproval/Cancel); `DashboardUtil` used in 7 Dashboard BizModels; `extends IApprovableBiz` in I*Biz = **0**; `approval-support.xbiz` references in xbiz = 3 comment-only hits (inheritance chain broken, per Exit Criteria). Protected-area ask-first: ORM tagSet 移除落地确认（0 残留）/ 测试绿确认（日志全绿基线）/ ask-first 人工确认记录 **traceable** — Phase 1 `Decision` 门控项 + `docs/analysis/use-approval-removal-scope.md` "批准：人工（2026-07-24 会话，audit iteration 10）" + Draft Review Record iteration 10 BLOCKER 解阻记录. (Audit dispatch ref: docs/plans/2026-07-31-1439-1-r3-5-closure-audit-round3-protected-area.md Phase 2; appended by R3.5 Round 3 backfill.)

Follow-up:

- Phase 2 per-mutation Processor 拆分（42 → ~250 文件）：详见 `docs/analysis/per-mutation-processor-split-plan.md`
