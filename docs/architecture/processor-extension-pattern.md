# Processor 扩展模式（Java 编排的配置余地）

## 定位

定义 nop-app-erp 中**拓扑稳定的复杂业务流程**在 Java 层面的实现规范：何时用 Processor 而非 task.xml、Facade 与 Processor 的两层职责分工、以及如何通过 protected 步骤 + `IServiceContext` + 派生 bean 覆盖为产品化留出配置余地。

本文是 `service-layer-orchestration.md`（task.xml 编排首选）的**补充**：task.xml 适用于拓扑可变的编排，本文适用于拓扑稳定但需要按客户/行业覆盖单步实现的编排。两者并行，按判定表选择。

> 平台机制权威：Processor / Step / `IServiceContext` 的完整说明见 `../nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md` 与 `03-runbooks/implement-complex-business-flow.md`。本文只规定本项目的判定规则与配置余地约定。

## 核心判定：task.xml vs Java Processor

| 编排特征 | 推荐手段 | 理由 |
|----------|----------|------|
| 拓扑可变（不同客户/行业步骤顺序或增删不同，上线后要调） | **task.xml** | VFS 模型，Delta 覆盖某步即生效，不重发版 |
| 拓扑稳定，但单步实现需按客户/行业覆盖 | **Java Processor + 派生 bean 覆盖**（本文） | 强类型编排锁死流程骨架，派生类覆盖单步实现 |
| 拓扑稳定，单步实现也稳定 | Java Processor（无需派生设计）或直接 BizModel 方法 | 不必为不复现的扩展需求过度设计 |
| 需断点重启 / 并行 / 限流 / 挂起 / 人机协作 | task flow / Workflow | Processor 不具备这些能力 |
| 同一单步会被多个流程稳定复用 | 抽 Step（task.xml `<simple>`/`<call-step>` 或 Processor 的独立 Step bean） | — |

> **判定原则**：问"上线后，实施人员/客户会不会改步骤顺序或增删步骤？"。会 → task.xml；不会但会改单步实现 → 本文 Processor 模式；都不会 → 普通 Java。

"拓扑稳定"的判定依据是各域设计文档的"稳定约束 vs 可配置策略"裁定表。凡是被某域裁定为"稳定约束（不可配置）"的流程骨架，落地时优先选 Processor 而非 task.xml——把刻意锁死的业务不变量放进可拖拽的 task.xml 反而违背设计意图。

## 两层结构：Facade + Processor

复杂流程的 Java 实现拆为两层，职责严格分离：

| 层 | 承载者（命名） | 职责 | 不做的事 |
|----|--------------|------|----------|
| Facade | `IErpXxxBiz` + `ErpXxxBizModel`（`@BizMutation`/`@BizQuery`） | API 入口、参数类型、事务入口、状态真相源写回、post-commit 副作用、跨域契约面 | 不堆编排逻辑 |
| 编排 | `ErpXxxProcessor`（具体类，经 beans.xml 注册） | 流程的步骤分解与顺序、跨聚合协作、调 `I*Biz`/`CrudBizModel` 安全能力 | 不发明持久化旁路、不直接 `dao()` 写状态、不自带事务边界 |

**硬规则**：

1. **事务入口钉在 Facade 的 `@BizMutation`，不下放到 Processor**。Processor 方法默认跟随外层事务；需要独立事务边界（如跨域失败隔离）时由 Facade 显式声明传播策略（`@Transactional(propagation=...)`），而非在编排层自行叠加 `@SingleSession`+`@Transactional`。

   > **ORM Session 作用域（事务/Session 分层）**：事务边界（`@Transactional`）钉 Facade，但 **ORM Session 的刷新作用域（`@SingleSession`）应钉在编排方法（`process()`）上，而非 Facade**。原因：Facade 的 `@BizMutation` 拦截器会把 Session 的 flush 推迟到外层 mutation 作用域，导致编排方法抛出的异常（如落库前的强制属性校验）在外层事务提交时才以 `CompletionException`/`invoke-listener-fail` 形式抛出，逃出跨域调用方的 `try/catch` 并污染外层事务。`@SingleSession` 钉在编排方法上，使 Session 作用域精确覆盖 ORM 工作、在编排方法返回时同步刷新，异常稳定落入调用方的 `try/catch`。这是事务边界（`@Transactional`）与 ORM Session 刷新作用域（`@SingleSession`）的两个不同关注点，分别归属 Facade 与编排层。参照实例：业财过账引擎（`IErpFinVoucherBiz` + `ErpFinPostingProcessor`，见 `docs/plans/2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`）。
2. **跨域调用方注入 `IErpXxxBiz`（Facade 接口），不直接注入 Processor 具体类**。Processor 是 BizModel 内部的编排手段，不是跨域契约。直接注入编排具体类会绕过 `I*Biz` 管道，丢失数据权限并耦合实现细节。
3. **Processor 内部优先调 `I*Biz` 或 `CrudBizModel` 安全能力**；直接 `dao()` 仅限同聚合子实体或域内部组件的标准用法（见 `service-layer-orchestration.md` 与平台 `implement-complex-business-flow.md`）。
4. **命名遵守平台规范**：不创建 `*Service`/`*Controller` 类（`../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`）；编排类用 `*Processor` 后缀，复用单步用 `*Step`。

## 配置余地：protected 步骤 + IServiceContext + 派生覆盖

产品化的 Java 编排按下列模式为单步实现留覆盖余地：

1. **步骤拆分为 `protected` 方法**。Processor 的 `process(...)` 主流程只编排步骤顺序；每个步骤是一个 `protected` 方法、单一职责。**步骤顺序本身是稳定不变量（流程骨架），不暴露为可配**——这是它与 task.xml 的根本区别。

2. **每个步骤以 `IServiceContext` 为末参**。`IServiceContext` 承载用户身份（`IUserContext`）、数据权限、缓存与事务上下文；跨步骤、跨 `I*Biz` 调用必须透传，否则下游静默跳过数据权限、丢失缓存。**步骤方法不需要 `@BizMutation`/`@BizQuery`**（那是 Facade 的注解），只是普通 Java 方法。

   ```java
   public class ErpXxxProcessor {
       @Inject protected IErpYyyBiz yyyBiz;

       public Result process(XxxRequest req, IServiceContext context) {
           validate(req, context);
           Entity e = prepare(req, context);
           Result r = execute(e, req, context);
           return r;
       }

       protected void validate(XxxRequest req, IServiceContext context) { /* ... */ }
       protected Entity prepare(XxxRequest req, IServiceContext context) { /* ... */ }
       protected Result execute(Entity e, XxxRequest req, IServiceContext context) { /* ... */ }
   }
   ```

3. **派生类覆盖 + 同名 bean 注册**。客户/行业需覆盖某步实现时，写一个继承基线 Processor 的派生类，重载目标 `protected` 方法，然后在 Delta 的 beans.xml 中以**同名 bean id** 注册派生类覆盖基线 bean。基线其余步骤不变，升级时自动合并。

   ```xml
   <!-- Delta: _delta/{deltaDir}/erp/{domain}/beans/app-service.beans.xml -->
   <bean id="与基线同名" class="客户派生 Processor 全限定名"/>
   ```

   > 覆盖单元是**单个步骤方法**，不是整个流程。流程骨架（步骤顺序）刻意不可配，以保证稳定约束（见各域设计文档的"稳定约束 vs 可配置策略"表）。

4. **SPI 扩展点优先于派生覆盖**。如果扩展需求是"按类型路由到不同实现"（如同类型业务的不同生成规则），优先用 SPI 接口 + 注册中心（`@Inject List<IXxxProvider>` 经 `<ioc:collect-beans>` 收集 + Map 路由），而非派生覆盖整个 Processor。SPI 更细粒度、支持多个实现共存；派生覆盖是"整体替换某一步"的单点扩展。两者可组合：Processor 主流程内部某步通过 SPI 路由到不同 Provider，既保留骨架又支持多实现。

## 反模式

| 反模式 | 为什么错 | 正确做法 |
|--------|----------|----------|
| 跨域注入 `*Processor` 具体类 | 绕过 `I*Biz` 契约面，丢失数据权限管道，耦合实现细节 | 注入 `IErpXxxBiz`，由 Facade 内部调 Processor |
| Processor 自带 `@SingleSession`+`@Transactional` 且无 Facade 包裹 | 事务入口下放，跨域调用方难以控制传播行为 | 事务钉在 Facade `@BizMutation`；独立事务边界由 Facade 显式声明 |
| Processor 所有 helper 是 `private` | 无法派生覆盖，产品化零配置余地 | 可覆盖步骤用 `protected`；仅真正不可变内部细节用 `private` |
| 步骤方法无 `IServiceContext` 末参 | 下游 `I*Biz`/`CrudBizModel` 调用拿不到上下文，静默跳过权限/缓存 | 每个步骤方法末参带 `IServiceContext context`，主流程透传 |
| 把流程骨架放进可配 task.xml 只为"留余地" | 稳定约束被实施人员误改，违背域设计文档的"不可配"裁定 | 骨架锁在 Java；只把单步实现留派生覆盖 |
| 为不复现的扩展需求过早拆 Step / 过度派生设计 | 增加无稳定职责的类 | 先留在 Processor 的 `protected` 方法；复用边界稳定后再抽 Step |
| 命名 `*Service`/`*Controller` | 与 Spring 混淆，违反平台 `service-layer.md` | 用 `*Processor` / `*BizModel` / `I*Biz` |

## 与现有 owner docs 的关系

| 文档 | 关系 |
|------|------|
| `service-layer-orchestration.md` | task.xml 编排的 owner doc；本文是其 Java 侧补充（拓扑稳定场景） |
| `system-baseline.md §服务层编写规范` | 核心规则基线；本文是其 Processor 分支的展开 |
| `customization-capabilities.md` | 定制能力总览；本文的"派生 bean 覆盖"是其"BizModel/Processor 手写"能力的配置余地细化 |
| `../nop-entropy/docs-for-ai/03-runbooks/implement-complex-business-flow.md` | Processor vs task flow 判定的平台权威 |
| `../nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md` | 何时拆 Processor / Step 的平台权威 |
| `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md` | `IServiceContext` 末参与命名规范的平台权威 |

## Processor → xbiz 桥接：何时不需 xbiz（M-5，plan 2026-07-20-2200-1）

> 本节回应 M-5 审计发现："42 个 Processor 无对应 `.xbiz.xml` 桥接文件，Delta 定制方缺 VFS 层切入点"。审计结论：**42 Processor 全部合法，不需要新增 xbiz 桥接**——xbiz 不是 Processor 的契约层。

### 合规检查器 R8 误报根因

`docs/audits/nop-compliance-checker.sh` 的 R8 规则查找 `<ProcessorBase>.xbiz.xml` 文件（如 `ErpPurOrder.xbiz.xml`），但 **xbiz 文件按实体命名而非 Processor**：

- `ErpPurOrder.xbiz`（实体 xbiz） → 存在
- `ErpPurOrderProcessor.xbiz.xml`（Processor xbiz） → 不存在（按设计）

R8 把"实体 xbiz 存在"误判为"Processor 缺 xbiz"。**42 全部是误报**。

### Nop 平台的 Processor 桥接规则（权威：`nop-backend-dev` skill §xbiz 的定位）

| Processor 调用方式 | 是否需要 xbiz 桥接 | 模式 |
|--------------------|-------------------|------|
| BizModel `@Inject Processor` + `@BizMutation` 方法内调用 `processor.process()` | **不需要** | 41/42 Processor 走此模式（最常见，xbiz `<actions/>` 为空是正确状态） |
| xbiz `<mutation><source>inject('FQCN').method()</source></mutation>` | **已存在 xbiz**（实体 xbiz 内的 source 脚本） | 1/42 Processor 走此模式（`ErpQaRecallProcessor`，经 `ErpQaRecall.xbiz` 的 source 脚本注入） |
| 客户/行业 Delta 想覆盖 Processor 单步实现 | **不需要 xbiz** | 写派生 Processor + Delta beans.xml 同名 bean 覆盖（见本文"配置余地"节） |
| 客户/行业 Delta 想用脚本完全替换 Processor 实现 | 需要 xbiz source | 在 Delta xbiz 中新增 `<mutation><source>...</source></mutation>` 覆盖 Java 默认 |

### 42 Processor 实际接线统计（M-5 audit）

- **41 个 Processor**：经 BizModel `@Inject` 注入，从 `@BizMutation` 方法内调用。Java 完整实现，xbiz `<actions/>` 为空（正确状态）。
- **1 个 Processor**（`ErpQaRecallProcessor`）：经 `ErpQaRecall.xbiz` 的 `<source>` 脚本注入（`inject('app.erp.qa.service.processor.ErpQaRecallProcessor').submitForApproval(id, svcCtx)`）。xbiz 桥接**已存在**，只是 R8 因文件名匹配规则误判为缺失。
- **0 个 Processor** 是真正的 orphan（无任何接线）。

### 复现命令

```bash
python3 << 'PY'
import os, re, glob
REPO_ROOT = "/Users/abc/app/nop-app-erp"
processors = [(f, os.path.basename(f).replace('.java',''))
              for f in glob.glob(f"{REPO_ROOT}/module-*/erp-*-service/src/main/java/**/*Processor.java", recursive=True)
              if '/target/' not in f and '/_gen/' not in f and '/test/' not in f]
all_files = []
for pat in ['module-*/erp-*-service/src/main/java/**/*.java',
            'module-*/erp-*-service/src/main/resources/**/*.xbiz',
            'module-*/erp-*-service/src/main/resources/**/app-service.beans.xml']:
    all_files.extend([f for f in glob.glob(f"{REPO_ROOT}/{pat}", recursive=True) if '/target/' not in f])
for f, cls in processors:
    p = re.compile(rf'\b{cls}\b')
    hits = {'java': 0, 'xbiz': 0, 'beans': 0}
    for other in all_files:
        if other == f: continue
        try:
            c = open(other).read()
            if p.search(c):
                if other.endswith('.java'): hits['java'] += 1
                elif '.xbiz' in other: hits['xbiz'] += 1
                elif 'beans.xml' in other: hits['beans'] += 1
        except: pass
    print(f"{cls}: java={hits['java']} xbiz={hits['xbiz']} beans={hits['beans']}")
PY
```

### 后续行动（M-5）

- **不需要新增任何 xbiz 桥接文件**
- 修复合规检查器 R8 的命名匹配（查找 `<Entity>.xbiz` 而非 `<Processor>.xbiz.xml`）→ **列为 Follow-up**（不阻塞本计划，当前命中数已稳定为基线）
- Delta 定制方覆盖 Processor 单步实现的标准入口已明确：派生 bean + beans.xml 同名覆盖（本文 §配置境地已说明）

## 状态判断方法的复用约定（L-6，plan 2026-07-20-2200-1）

> L-6 原审计假设：Processor 中存在重复的 `isAlreadyApproved`/`isAlreadyRejected` 方法，应上提到实体或工具类。
>
> **审计结果**：`protected boolean (isAlreadyApproved|isAlreadyRejected)` 模式在 production 代码中**实际不存在 0 处命中**（合规检查器 R11 报 0）。

### 实际的等价重复（Decision + Follow-up）

虽然方法名形式的重复不存在，但**等价的内联状态判断**有显著重复：

- `Objects.equals(status, ErpXxxConstants.APPROVE_STATUS_APPROVED)` 在 22 个 Processor 中累计出现 132 次
- `Objects.equals(status, ErpXxxConstants.APPROVE_STATUS_REJECTED)` 类似

这是**形式不同但语义相同**的重复：每个 Processor 在自己的状态守卫方法中重复写 `Objects.equals(getApproveStatus(), CONSTANTS.APPROVED)`，没有抽到实体方法（如 `ErpPurOrder.isApproved()`）或共享工具（如 `ApproveStatusHelper.isApproved(entity)`）。

### 为什么不立即改造（裁决）

- **影响范围大**：22 Processor + 多个实体类 + 跨域常量；L-6 标记为低严重性，与改造风险不匹配
- **改造路径未定型**：实体方法上提 vs 共享工具 vs 平台 `use-approval` 机制标准化，需要专门的 ADR
- **当前代码无 bug**：内联判断虽冗余但正确，无运行时风险

### Follow-up 触发条件（满足任一即创建专门计划）

1. 新增审批流单据 ≥ 5 个（重复模式继续累积）
2. 平台 nop-entropy 提供 `ApproveStatusHelper` 或等价标准机制
3. 任何 Processor 因内联状态判断错误导致 bug（说明抽象缺失开始付出代价）

### 推荐改造方向（仅作 successor 计划参考）

- **优先方向 A**：实体方法上提（`ErpPurOrder.isApproved()` / `isRejected()` / `isSubmitted()`）——DDD 正统，调用点最自然
- **方向 B**：共享工具类（`ApprovalStatusHelper.isApproved(ApprovableEntity)`）——跨实体通用但需引入新接口
- **方向 C**：依赖平台 `use-approval` 机制标准化（`ApprovalSupportBizModel` 提供 helper）——平台对齐但需 nop-entropy 协调
