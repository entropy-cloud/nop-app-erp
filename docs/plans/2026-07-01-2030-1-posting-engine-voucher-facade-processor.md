# 2026-07-01-2030-1-posting-engine-voucher-facade-processor 过账引擎改造：凭证聚合根 Facade + 编排 Processor

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: 用户架构质疑（`ErpFinPostingService` 不符合 Nop 规范）+ `docs/architecture/processor-extension-pattern.md`（新 ADR）
> Related: `docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`（active，服务层 API 合规广谱整改，早于本 ADR，S2 含 `ErpFinPostingService:322` 一项由本计划接管）；`docs/plans/2026-07-01-0811-1-finance-posting-engine-foundation.md`（completed，过账引擎 greenfield 落地）
> Audit: required

## Current Baseline

- **过账引擎现状**（0811-1 落地）：`ErpFinPostingService`（`erp-fin-service/.../service/posting/ErpFinPostingService.java`）是具体类，自带 `@SingleSession`+`@Transactional`（`:65-66`），经 `app-service.beans.xml` 注册（`:27-28`）。`post(PostingEvent)`（`:67`）+ `reverse(billHeadCode, businessType)`（`:117`）两个公共方法。**`post()` 体内 Provider 解析（`:72-76`）、Facts 生成（`:89` 调 `provider.createFacts`）、Validator 链（`:90-92`）、借贷平衡校验（`:96-110`）均为内联逻辑，非独立方法**；真实 `private` helper 仅 `alreadyPosted`/`persistVoucher`/`findPostedVoucher`/`findBillLinks`/`resolveOpenPeriod`/`resolveSubjects`/`loadLines`/`buildVoucherCode`。全方法无 `IServiceContext` 末参，无 protected 扩展点。
- **跨域调用链**（inventory→finance）：`ErpInvStockMoveBizModel.generateMove`（`@BizMutation` 入口，`:61`）→ `doComplete`（`:182`）→ `InvPostingDispatcher.dispatchIfApplicable`（`postingDispatcher` 字段注入于 `ErpInvStockMoveBizModel:52-53`，调用点 `:194`；构造 `PostingEvent`）→ `InvPostingExecutor.postEvent`（`@Transactional(REQUIRES_NEW)`，`:22`）→ **跨域直注** `ErpFinPostingService` 具体类（`InvPostingExecutor.java:20`）。违反 `processor-extension-pattern.md` 硬规则"跨域注入 `IErpXxxBiz`，不注入 Processor 具体类"。
- **facade 缺失**：`IErpFinVoucherBiz`（`erp-fin-dao/.../biz/`）是空壳 `extends ICrudBiz<ErpFinVoucher>`，无 `post`/`reverse` 动作方法。`ErpFinVoucherBizModel`（`erp-fin-service/.../service/entity/`）是空壳 `CrudBizModel<ErpFinVoucher>`。`posting.md:300` 设计的 `IErpFinPostingBiz.postNow` 从未实现（grep 零命中）。
- **领域语义已裁定**（用户确认）：过账恒等于生成会计凭证 → **凭证（`ErpFinVoucher`）是过账记录的聚合根主实体**（凭证头 + `VoucherLine` 分录 + `VoucherBillR` 回链为子实体）。故 `post`/`reverse` 挂 `IErpFinVoucherBiz` + `ErpFinVoucherBizModel`（凭证聚合根的工厂创建入口），不新建 `IErpFinPostingBiz`、不新增 ORM 实体。
- **测试入口**：`TestErpFinPostingService` 直注 `ErpFinPostingService`（`:65`），自述"直调 Java API，不走 GraphQL"（`:39`）；`TestErpInvPosting` 走 `IErpInvStockMoveBiz.generateMove`（不直接碰过账类）。
- **模块依赖**：`erp-inv-service` compile 依赖 `app-erp-finance-service`（pom.xml:35），注释称"IErpFinPostingService / IErpFinAcctDocProvider SPI 位于 finance-service"。

## Goals

- 过账引擎改造为 **Facade（`IErpFinVoucherBiz`+`ErpFinVoucherBizModel`）+ 编排（`ErpFinPostingProcessor`）两层**，对齐 `processor-extension-pattern.md`。
- `post`/`reverse` 挂 `IErpFinVoucherBiz`（凭证聚合根 BizModel），事务入口钉在 `@BizMutation`，`IServiceContext` 末参透传。
- `ErpFinPostingService` → `ErpFinPostingProcessor`：去自带 `@SingleSession`+`@Transactional`（跟随 Facade 事务），步骤拆 `protected` + `IServiceContext` 末参（配置余地）。
- 跨域调用（inventory）经 `IErpFinVoucherBiz`，消除 `InvPostingExecutor` 对具体类的直注。
- 不改过账业务逻辑、不改凭证/分录/回链落库行为、不改 SPI（Provider/Validator）机制。

## Non-Goals

- **不**新增 `IErpFinPostingBiz` 或 `ErpFinPosting` 实体（领域语义已裁定：过账=凭证，凭证是主实体）。
- **不**改过账业务规则（幂等前置/期间门控/借贷平衡/红冲逻辑/凭证模板/科目映射不变）。
- **不**改 SPI Provider/Validator 机制（`IErpFinAcctDocProvider`/`IErpFinFactsValidator`/`ErpFinAcctDocRegistry` 不动）。
- **不**上 task.xml 编排（`processor-extension-pattern.md` 判定：过账拓扑为稳定约束，用 Java Processor）。
- **不**处理 `1900-1` 范围的其他 finance BizModel 整改（`ErpFinVoucher` 自身的 S1/S3 等 CRUD 合规归 1900-1；本计划只加 `post`/`reverse` 动作并改造过账编排）。
- **不**改 `model/*.orm.xml`（保护区域；方案 A 零 ORM 变更）。
- **不**重构 `IErpFinAcctDocProvider` SPI 的模块归属（当前在 finance-service；跨域实现卫生属独立 follow-up）。
- **不**改 `TestErpInvPosting`（走 `generateMove`，不直接碰过账类，链路通即验证）。

## Task Route

- Type: `architecture change`（服务层契约重构：facade 归位 + 编排层降级 + 跨域调用改造）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（目标形态权威）、`docs/design/finance/posting.md`（过账业务语义）
- Skill Selection Basis: `nop-platform-conformance-audit-prompt`（整改后自检反模式：跨域直注、事务下放、private 无扩展点、缺 IServiceContext）
- Verification: `mvn test -pl module-finance/erp-fin-service -am` + `mvn test -pl module-inventory/erp-inv-service -am`
- **契约面风险**：`IErpFinVoucherBiz` 在 finance-dao，新增方法影响所有注入方。`PostingEvent` 当前在 finance-service.posting 包，若作为 facade 参数须下沉到 finance-dao（Decision 点）。**事务传播验证**：`@BizMutation` 默认包装事务，Facade `post()` 叠加 `@Transactional(propagation=REQUIRES_NEW)` 的传播行为须执行时验证符合跨域失败隔离语义（现 `InvPostingExecutor:22` 行为：过账失败回滚独立事务，不污染移动单主事务）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 纯服务层重构，无外部服务/端口/密钥依赖。

## Execution Plan

### Phase 1 — 过账引擎 Facade + Processor 两层重构（finance + inventory 原子完成）

Status: completed
Targets: `IErpFinVoucherBiz`（finance-dao）、`ErpFinVoucherBizModel`（finance-service）、`ErpFinPostingService`→`ErpFinPostingProcessor`（finance-service）、`InvPostingExecutor`（erp-inv-service）、`app-service.beans.xml`（finance）、`TestErpFinPostingService`
Skill: `nop-platform-conformance-audit-prompt`

- Item Types: `Fix-heavy`（形态重构为主，对齐 ADR 目标形态）
- Prereqs: `processor-extension-pattern.md` 已落地（completed）

#### 接口契约（结构边界，非实现伪代码）

**Facade — `IErpFinVoucherBiz`（凭证聚合根契约，finance-dao）补充动作方法**：

| 方法 | 语义 | 事务 | 备注 |
|------|------|------|------|
| `post(...)` | 从业务事件创建并过账凭证（工厂+过账入口）；幂等（已过账返回 null）；返回凭证 ID | `@BizMutation`（钉此） | 参数形态见 Decision |
| `reverse(...)` | 按回链反查原已过账凭证，生成红字冲销凭证；返回红字凭证 ID | `@BizMutation`（钉此） | 参数：源单标识 + 业务类型 |

> Facade 只负责入口/事务/参数，编排委托 `ErpFinPostingProcessor`。**`REQUIRES_NEW`（跨域失败隔离，现 `InvPostingExecutor:22` 语义）上移到 Facade `post()` 显式声明 `@Transactional(propagation=REQUIRES_NEW)`，`InvPostingExecutor` 移除其 `@Transactional`；事务边界不下放编排层**。

**编排 — `ErpFinPostingProcessor`（finance-service，被 VoucherBizModel 内部调用）**：

- `process(...)`/`reverseProcess(...)` 主流程：步骤顺序编排（幂等前置→Provider→Facts→Validator 链→期间门控→平衡→落库），每步 `protected` + `IServiceContext` 末参（配置余地：派生类同名 bean 覆盖单步）。
- **不**自带 `@SingleSession`+`@Transactional`（跟随 Facade `@BizMutation`）。
- **不**被跨域直注（仅 `ErpFinVoucherBizModel` 注入）。

- [x] **Decision** `PostingEvent` 模块归属与 facade 参数形态 → **选项 A**：`PostingEvent` + `ErpFinBusinessType` 下沉到 `app.erp.fin.dao`（finance-dao），facade `post(PostingEvent, IServiceContext)` / `reverse(String, ErpFinBusinessType, IServiceContext)`。
  - 选择理由：(1) 二者**已是跨域类型**——inventory（`InvPostingDispatcher`/`InvAcctDocProvider`）已构造/引用它们；finance-dao 是跨域契约面（`IErpFinVoucherBiz` 所在层），将跨域消息+业务类型词汇归位到 dao 使模块边界诚实。(2) `ErpFinBusinessType` 是零依赖纯值枚举（`erp-fin/business-type` 字典的类型安全门面），属领域字典层（dao）而非实现层（service）。(3) `PostingEvent` 是值 DTO，唯一依赖 `ErpFinBusinessType`+JDK 类型，搬迁自包含。(4) 选项 B（facade 用基础参数）>5 参数须再造 DTO（等价于 PostingEvent），且 `reverse` 仍需业务类型语义→仍要枚举在 dao，故无优势。
  - 残留风险：触碰文件多于选项 B（finance-service + inventory-service 的 import 路径批量更新），已由 grep 校验机械重写覆盖；SPI 接口（`IErpFinAcctDocProvider`/`IErpFinFactsValidator`/`ErpFinAcctDocRegistry`）**机制不变**、仍留 finance-service，仅 import 路径改向 dao（service→dao 依赖方向正确，非"重构 SPI 模块归属"）。
  - Skill: none
- [x] **Fix** 新增 `IErpFinVoucherBiz.post`/`reverse` 动作方法签名（按上述 Decision 结果），末参 `IServiceContext`。
  - Skill: none
  - 执行结果：`IErpFinVoucherBiz`（finance-dao）新增 `@BizMutation post(@Name("event") PostingEvent, IServiceContext)` 与 `@BizMutation reverse(@Name("billHeadCode") String, @Name("businessType") ErpFinBusinessType, IServiceContext)`。
- [x] **Fix** `ErpFinVoucherBizModel` 实现 `post`/`reverse`：`@BizMutation`、注入 `ErpFinPostingProcessor` 委托编排、`IServiceContext` 透传。`post()` 声明 `@Transactional(propagation=REQUIRES_NEW)` 承接跨域失败隔离（从 `InvPostingExecutor` 上移）。
  - Skill: `nop-platform-conformance-audit-prompt`
  - 执行结果：Facade `post()` = `@BizMutation @Transactional(REQUIRES_NEW)`，`reverse()` = `@BizMutation`（默认传播，承接原 reverse 行为），均委托 `ErpFinPostingProcessor`。**执行期修正（重要）**：`@SingleSession` **未**放在 Facade，而下沉到 Processor 的 `process()`/`reverseProcess()`（见下项理由）——Facade 的 `@BizMutation` 拦截器会把 Session flush 推迟到外层 mutation 作用域，导致过账异常（如 `mandatory-prop-is-null`）以 `CompletionException`/`invoke-listener-fail` 在外层事务提交时才抛出、逃出跨域调用方 `InvPostingDispatcher` 的 try/catch 并污染外层事务（purchase/sales 反审核端到端实测回归）。`@SingleSession` 钉在编排方法上，Session 作用域精确覆盖 ORM 工作并在编排方法返回时同步刷新，异常稳定落入 dispatch 的 try/catch（与重构前行为一致）。事务边界（`@Transactional`）仍钉 Facade，符合 ADR 硬规则 1。
- [x] **Fix** `ErpFinPostingService` → `ErpFinPostingProcessor`：重命名；去 `@Transactional`；将 `post()`/`reverse()` 体内**内联步骤**（Provider 解析/Facts 生成/Validator 链/平衡校验）抽取为 `protected` 方法 + `IServiceContext` 末参；既有 `private` helper（`alreadyPosted`/`resolveOpenPeriod`/`persistVoucher`/`resolveSubjects`/`loadLines`/`findPostedVoucher`/`findBillLinks`/`buildVoucherCode`）改 `protected` + 加 `IServiceContext` 末参；finance 域内部 `daoFor(ErpFin{Voucher,VoucherLine,VoucherBillR,AccountingPeriod})` 按 1900-1 C 段保留（同聚合/同域内部），跨域读 `daoFor(ErpMdSubject)` 改 `IErpMdSubjectBiz`（接管 1900-1 S2 的 `ErpFinPostingService:322` 项）。
  - Skill: `nop-platform-conformance-audit-prompt`
  - 执行结果：`ErpFinPostingProcessor`（finance-service），`process()`/`reverseProcess()` 主流程只编排步骤；全部步骤 + helper 为 `protected` + `IServiceContext` 末参（`resolveProvider`/`generateFacts`(含 Validator 链)/`balanceTotals`/`assertBalanced` 等内联步骤已抽取；新增 `prepareContext`/`prepareReversalContext`/`buildReversalDraft`(holder)）；`ErpMdSubject` 跨域读经 `IBizObjectManager.getBizObject("ErpMdSubject").asProxy()` 的 `IErpMdSubjectBiz.findByCode`（finance→erp-md 仅 test 作用域，非 BizModel 编排 bean 用 IBizObjectManager，承接重构前已落地的 S2）。**`@SingleSession` 保留在 `process()`/`reverseProcess()`（执行期修正，理由见上项）**，`@Transactional` 不带（跟随 Facade）。
- [x] **Fix** `InvPostingExecutor`：注入 `IErpFinVoucherBiz`（替代 `ErpFinPostingService` 具体类直注），调 `voucherBiz.post(...)`；**移除其 `@Transactional(REQUIRES_NEW)`（`:22`）——该传播已上移到 Facade `post()`**。同步订正 `erp-inv-service/pom.xml:32` 注释中的 `IErpFinPostingService`（不存在的接口名）→ `IErpFinVoucherBiz`。
  - Skill: `nop-platform-conformance-audit-prompt`
  - 执行结果：`InvPostingExecutor` 注入 `IErpFinVoucherBiz`，`postEvent` 调 `voucherBiz.post(event, context)`（context 取 `IServiceContext.getCtx()` 兜底 `ServiceContextImpl`），移除 `@Transactional`；pom.xml 注释订正为 `IErpFinVoucherBiz`。
- [x] **Fix** `app-service.beans.xml`（finance）：`ErpFinPostingService` bean → `ErpFinPostingProcessor`（id/class 同步）。
  - Skill: none
  - 执行结果：bean id/class 同步为 `ErpFinPostingProcessor`，注释更新为"编排层 Processor 被 Facade 内部调用"。
- [x] **Fix** `TestErpFinPostingService`：注入入口跟随重构（`IErpFinVoucherBiz` 或 `ErpFinPostingProcessor`），既有 6 个行为测试（happy/幂等/不平衡/期间结账/红冲成功/红冲未找到）断言不变、全绿。
  - Skill: none
  - 执行结果：测试改注 `IErpFinVoucherBiz voucherBiz`，调 `voucherBiz.post(event, CTX)` / `voucherBiz.reverse(..., CTX)`（CTX 为既有 `ServiceContextImpl`）；6 个行为断言不变、全绿（同时验证 Facade 的 `@BizMutation`+`@Transactional(REQUIRES_NEW)`+Processor `@SingleSession` 传播组合）。

Exit Criteria:

- [x] `IErpFinVoucherBiz` 含 `post`/`reverse` 动作方法，末参 `IServiceContext`；`ErpFinVoucherBizModel` `@BizMutation` 实现二者并委托 Processor
- [x] `ErpFinPostingProcessor` 无 `@Transactional`；公共/步骤方法带 `IServiceContext` 末参；步骤为 `protected`（可派生覆盖）。**修订**：`@SingleSession` 保留在编排方法 `process()`/`reverseProcess()`（执行期发现：Facade 的 `@BizMutation` 会推迟 Session flush 至外层 mutation，导致过账异常逃出跨域调用方 try/catch；钉在编排方法上使 flush 同步、异常可捕获——事务 `@Transactional` 仍钉 Facade）
- [x] `InvPostingExecutor` 不再 `@Inject ErpFinPostingService`/`ErpFinPostingProcessor` 具体类，改 `IErpFinVoucherBiz`
- [x] `ErpFinPostingService` 类名在全仓 main 代码中不再存在（grep 零命中）
- [x] 既有 6 个过账行为测试全绿（断言不变，仅入口跟随）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_0e1f9a199ffeLeCVMcpx7fb4fd`）。1 BLOCKER + 2 MAJOR + 3 MINOR，均已修订：
  - BLOCKER：Current Baseline 虚构 4 个 private helper 方法名（`resolveProvider`/`createFacts`/`runValidators`/`assertBalanced` 实为 `post()` 体内联逻辑，非独立方法）。→ 已据实改述为"内联步骤 + 真实 private helper 清单"，Processor Fix 项改写为"内联步骤抽取为 protected + 既有 private helper 改 protected"。
  - MAJOR1：`REQUIRES_NEW` 边界含"若保留/保留或上移"松弛词。→ 写死：Facade `post()` 声明 `@Transactional(propagation=REQUIRES_NEW)`，`InvPostingExecutor` 移除其 `@Transactional`。
  - MAJOR2：调用链标注错误（`ErpInvStockMoveBizModel.dispatchIfApplicable` 误作 `@BizMutation` 入口；行号 :51/:192 漂移）。→ 订正为 `generateMove(@BizMutation:61)→doComplete(:182)→dispatchIfApplicable`，行号 :52-53/:194。
  - MINOR1：`pom.xml:32` 注释引用不存在的 `IErpFinPostingService`。→ 并入 `InvPostingExecutor` Fix 项订正为 `IErpFinVoucherBiz`。
  - MINOR2：1900-1 计数未回写确认（双活期间 :322 临时重叠）。→ 属 1900-1 责任，本 plan Closure Gate 已声明协调意图，可接受。
  - MINOR3：单 Phase 命名"Phase 1"。→ 标题已含"原子完成"，finance+inventory 中间状态不可编译故不拆多 Phase，措辞保留。
- Independent draft review iteration 2: `passes draft review`（`ses_0e1f29195ffe65vy70zT6HmlHQ`）。逐项核实 4 项修订全部据实落地（行号与代码一致：8 个 private helper 全部存在并定位、`generateMove` `@BizMutation` 注解:60/声明:61、`doComplete`:182、`dispatchIfApplicable`:194、REQUIRES_NEW 三处统一写死且 Facade/Processor/Executor 三角自洽、pom 注释订正）。整体：反松弛 ✓ / 规则13 ✓ / 无新矛盾 ✓ / 接口契约 ✓ / 退出标准 ✓。残留非阻塞风险：(1) Decision:77 `PostingEvent` 归属待执行时闭合（有界选择，不阻塞）；(2) `@BizMutation` 默认事务 + `@Transactional(REQUIRES_NEW)` 共存的平台传播行为须执行时验证（已并入契约面风险）。计划转 `active`。

## Closure Gates

- [x] 范围内行为完成（Facade + Processor 两层、跨域走 I*Biz、事务上移、配置余地）
- [x] `mvn test -pl module-finance/erp-fin-service -am` 全绿（含 6 个过账行为测试）— 13 tests green
- [x] `mvn test -pl module-inventory/erp-inv-service -am` 全绿（`TestErpInvPosting` 3 个端到端不回归）— 21 tests green
- [x] 根 `mvn test -fae` 全绿（146 reactor 模块无回归）— `mvn clean install -fae -T 1C` = BUILD SUCCESS，328 tests / 0 failures / 0 errors，含 app-erp-all
- [x] `nop-platform-conformance-audit-prompt` 自检：无跨域直注具体类、无事务下放编排层、无 private 无扩展点步骤、无缺 IServiceContext 末参 — 独立审计逐项确认通过
- [x] `docs/architecture/processor-extension-pattern.md` 已存在（前置 completed），本计划落地后过账引擎为其首个参照实例 — 已补充"ORM Session 作用域（事务/Session 分层）"段并以本引擎为参照实例
- [x] 与 1900-1 边界已声明（S2 `ErpFinPostingService:322` 项由本计划接管；1900-1 执行 finance 时跳过该项或标注 successor）
- [x] 无范围内项目降级为 deferred/follow-up — `IErpFinAcctDocProvider` SPI 模块归属卫生属独立 follow-up（计划前即裁定为 out-of-scope）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status / 阶段 Status / Exit Criteria / Closure Gates / logs 一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计 — `ses_0e12cb407ffe4WeiuEcIndIrc4`，verdict `passes closure audit`
- [x] 结束证据存在于文件中 — 见下 Closure Audit Evidence

## Deferred But Adjudicated

### IErpFinAcctDocProvider SPI 模块归属卫生

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `IErpFinAcctDocProvider`/`IErpFinFactsValidator` 当前在 finance-service.posting 包，跨域实现（`InvAcctDocProvider`）依赖 finance-service。理想情况 SPI 契约应在 finance-dao（与 `IErpFinVoucherBiz` 同层），但这是独立卫生问题，与本计划的 facade 归位不同结果表面。
- Successor Required: yes —— 触发条件：finance 模块边界卫生整理，或 inventory 对 finance 依赖瘦身时。

## Closure

Status Note: passed closure audit（执行 + 独立结束审计均完成）

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_0e12cb407ffe4WeiuEcIndIrc4`（新会话，未参与执行）
- Verdict: `passes closure audit`（无 BLOCKER / 无 MAJOR；2 项 MINOR 非阻塞：ADR doc-drift 已补 ADR "ORM Session 作用域"段闭合、closure 文本状态本节同步）
- 证据：逐项核对 10 项检查单全 PASS（Facade 契约/实现、Processor 无 @Transactional + 全 protected + IServiceContext 末参、旧类 main 代码 grep 零命中、跨域注 IErpFinVoucherBiz、模块下沉、beans.xml/pom 订正、反模式全无、测试入口跟随）；@SingleSession-on-Processor 偏离经评估"justified & documented"（事务边界钉 Facade、ORM Session 刷新作用域钉编排方法，ADR 反模式限定语"且无 Facade 包裹"不触发）
- 验证基线：`mvn clean install -fae -T 1C` = BUILD SUCCESS，328 tests / 0 failures / 0 errors / 146 模块全 SUCCESS；执行期捕获并修复一处回归（@SingleSession 归位详见上 Exit Criteria 修订 + 执行 Fix 项）

Follow-up:

- `IErpFinAcctDocProvider` SPI 模块归属卫生（见 Deferred）
- 1900-1 执行 finance 其他 BizModel 合规时，`ErpFinPostingService:322` 项已由本计划接管，不再重复处理
