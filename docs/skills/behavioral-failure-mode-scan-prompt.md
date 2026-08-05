# 行为失败模式扫描提示（Behavioral Failure Mode Scan）


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


在 closure 前对一个**单域 / 单切片**做代码层行为失败模式扫描时使用此提示。它把 `docs/lessons/07-11-*.md` 与 `docs/audits/2026-07-04-0000-batch-scheduling-audit.md` 中跨多轮审计反复复发的 4 类代码层行为失败模式（业财过账吞异常悬挂 / dict 死状态 / 调度链断裂 / 守卫散点）固化为可被独立子代理照做的 grep 程式 + 决策树 + 反模式自检表。

**使用场景**：单域 / 单切片代码层行为失败模式扫描，在 closure 前由独立子代理（fresh session）运行；可作为 `closure-audit-prompt.md` 的强制前置，也可作为 `multi-dimensional-audit-prompt.md` 某个维度内的输入证据采集步骤。

**不使用场景**：
- 设计层状态机图审查 → 用 `state-machine-business-review-prompt.md`（设计层迁移图 / 角色 / 待办策略审查；本技能只做代码层 `catch` / `setStatus` / 调度拓扑扫描）。两者互补——`state-machine-business-review` 是设计层入口，本技能 §2 dict 可达性是其代码层落地核查，详细 grep 程式在本技能，设计层提示只交叉引用。
- 单计划闭合审查 → 用 `closure-audit-prompt.md`（覆盖验收标准 / 证明 / 闭合门控）；本技能是「单域代码层行为扫描」，不是计划级闭合审查，但可作 closure-audit 的强制前置。
- 平台规则合规（异常是否 extends NopException / @Inject private / 字符串 == 比较） → 用 `nop-platform-conformance-audit-prompt.md`；本技能审的是 catch 宽度 / 标志位终态 / 告警通道，与平台规则正交。
- 多维整件工作挑战 → 用 `multi-dimensional-audit-prompt.md`（跨多维度同时挑战整件工作）；本技能抽象层更低（单域代码层扫描），可作为 multi-dim 某维度内的证据采集输入，但不替代其维度框架。
- 需求→实现五级追踪 → 用 `requirement-compliance-audit-prompt.md`（指向 methodology）；本技能不审需求契约符合性。

**必需输入**：
- 目标域范围（一个 `module-<domain>/` 或一个功能切片）
- 该域 owner doc 的状态机章节（`docs/design/<domain>/state-machine.md` 等，用于 dict 可达性裁决对照）
- 该域 ORM 模型 `<domain>/model/app-erp-<domain>.orm.xml`（dict / 状态字段 / 标志位字段来源）
- 该域调度链描述（`docs/architecture/job-scheduling.md` 对应行 + 该域 `*.batch.xml` / cron 配置键）

**预期输出**：
按 4 类失败模式（B1/B2/B3/B4）分组的 finding 清单，每个 finding 含：失败模式类别、控制点（file:line）、4 信号核查结果、裁决（pass / 改进 / Deferred 显式触发条件）、引用证据编号（P1-MA2-xxx / P1-MA4-xxx / P1-RC-xxx / lesson 编号）。最终裁决 `passes behavioral scan` 或 `needs revision`。

```text
您是高级 ERP 架构师和 Nop Platform 行为闭环专家。对一个单域 / 单切片做代码层行为失败模式扫描。

首先阅读这些文件：
- `AGENTS.md`
- `docs/skills/README.md §项目定制化层` 与 `§已知失败模式`（扩展后 13 项清单，含本技能捕获的 B1-B4）
- `docs/lessons/09-posting-exception-swallow-suspension.md`（B1 业财过账吞异常悬挂的方法论）
- `docs/lessons/10-dict-dead-state-unreachable.md`（B2 dict 死状态的方法论）
- `docs/audits/2026-07-04-0000-batch-scheduling-audit.md`（B3 调度链专项审计方法）
- `docs/audits/2026-07-2*-arm-ma6-*.md`（B4 守卫散点的既有证据：状态机非法迁移守卫 / 并发乐观锁守卫 / @BizMutation/@BizQuery 权限注解守卫）
- 目标域 owner doc 的状态机章节 + ORM 模型 + 调度链描述

审计对象 = 单域代码层行为闭环，**不是**设计文档审查（用 state-machine-business-review）、**不是**计划闭合审查（用 closure-audit）、**不是**平台规则合规（用 nop-platform-conformance）。这四类共享「行为正确性」标签但抽象层不同——本技能专注代码层 catch 宽度 / setStatus writer / 调度拓扑 / 守卫清单的可 grep 信号。

逐类执行扫描。每类给出 grep 程式、4 信号核查、决策树与反模式自检表。

## §1 业财过账 / 异步任务异常闭环扫描（来自 lesson 09，B1）

**目标**：找出 `try { post(...) } catch (Exception e) { log.warn(e); }` 类吞异常致业务侧 `posted=false` 永久悬挂、无告警闭环的代码路径。

### grep 程式

```
# 1. 定位过账 / 异步任务的 catch 块
rg -n 'catch\s*\(\s*(Exception|Throwable)\s+\w+\s*\)' module-<domain>/erp-<short>-service/src/main/java
# 2. 聚焦 PostingDispatcher / Processor / Job 类（过账链路高发区）
rg -n 'catch\s*\(\s*Exception' module-<domain>/erp-<short>-service/src/main/java \
  -g '*PostingDispatcher*.java' -g '*Processor*.java' -g '*Job*.java' -g '*Saga*.java'
# 3. 找吞咽特征（catch 后只 log，不 rethrow / 不落状态）
rg -n -A2 'catch\s*\(\s*Exception' <上面命中的文件> | rg 'log\.(warn|error)'
# 4. 找悬挂标志位
rg -n 'posted\s*=\s*false|\.setPosted\(false\)' module-<domain>/
```

### 4 信号核查（对每个命中的 catch 块逐项问）

1. **catch 是否吞咽**：catch 后是否 rethrow（throw / throw new NopException）或落 FAILED 状态？若只 `log.warn` / `log.error` 后继续 → 吞咽。
2. **业务标志位终态**：过账失败后 `posted` / `docStatus` 是什么？`posted=false` 且 `docStatus=APPROVED`（业务终态）→ 悬挂（既不能重试也无失败信号）。正确：`posted=FAILED` 且保持可重试状态，或回退到非终态。
3. **告警通道**：失败是否经 `IErpSysNotificationBiz`（或同等通知 Facade）发出告警？仅 `log.warn` → 不够。
4. **调度链 preCheck 覆盖**：期末结账前置检查（`ErpFinAccountingPeriodProcessor.preCheck` 或同型）是否扫描 IGNORED / 已吞咽悬挂（非仅 PENDING）？只扫 PENDING → 已吞咽悬挂逃逸日常运营监控。

### 决策树（搬自 lesson 09:43-57，简化）

```
1. catch 块是否吞咽异常（不 rethrow / 不落 FAILED）？
   → 否（rethrow 或落 FAILED）：正常。
   → 是：禁止。进入步骤 2。

2. 过账失败后业务单据 posted 标志 + docStatus 是什么？
   → posted=FAILED 且可重试：进入步骤 3。
   → posted=false 且 docStatus=APPROVED（终态）：悬挂。禁止。改为不进终态（保持可重试）。

3. 失败是否有可观测告警闭环？
   → 仅 log.warn：不够。补 IErpSysNotificationBiz 告警 + 调度链 preCheck 扫 IGNORED。
   → 通知 + preCheck 扫描（含 IGNORED）：正常。
```

### 反模式自检表（搬自 lesson 09:60-66）

- [ ] catch 块**没有**宽 `catch (Exception)` 吞咽后只 `log.warn`？
- [ ] 过账失败时业务单据**不进业务终态**（保持可重试状态）？
- [ ] 失败经 `IErpSysNotificationBiz` 发出告警（非仅日志）？
- [ ] 期末结账前置检查扫描覆盖 IGNORED / 已吞咽悬挂（非仅 PENDING）？
- [ ] 是否有「过账失败 → posted 悬挂 → 运维介入 → 重试成功」的测试覆盖？
- [ ] IGNORED 状态是否强制处置理由 + 关联单据 visible flag？

**参考证据**：P1-MA2-032/048/060/068/074/080 + P1-MA4-001/004/007/010/013/020（R1.16 跨 12 站点同族）。

## §2 状态机 dict 可达性扫描（来自 lesson 10，B2）

**目标**：找出 dict 声明但无任何 `setStatus(...)` writer 的「死状态」——运行时永不出现、UI 下拉永远空命中、owner doc 迁移图空头承诺。

### grep 程式

```
# 1. 枚举目标域 dict 声明的每个状态值（从 ORM <dict> 驱动的 *.dict.yaml，或直接读 orm.xml <dict>）
rg -n '<dict name="' module-<domain>/model/app-erp-<domain>.orm.xml
# 读 dict yaml：module-<domain>/erp-<short>-meta/src/main/resources/_dict/*.dict.yaml（生成产物，仅读不改）
# 2. 对 dict 每个状态值 X，grep writer
rg -n "setStatus\(*_X\)|setStatus\(\"X\"\)" module-<domain>/erp-<short>-service/src/main/java
# 3. 交叉核查常量定义零使用
rg -n 'STATUS_X|STATE_X' module-<domain>/erp-<short>-service/src/main/java/.../*Constants.java
```

### 4 信号核查（对 dict 每个状态值逐项问）

1. **writer 存在性**：`setStatus(该值)` 在 src/main 是否有 writer？无 writer → 死状态候选。
2. **owner doc 迁移图一致性**：owner doc 迁移图是否声明了进入该值的迁移？声明了但代码无实现 → 承诺漂移；未声明 → 纯预留死状态。两者都需裁决。
3. **常量零使用**：`*Constants.java` 中的状态常量是否都有使用方？零使用 = 死常量。
4. **三处同步**：裁决为删除时，是否同步删了 dict 项 + 常量 + owner doc 迁移图（三处）？只删一处 → 残留漂移。

### 决策树（搬自 lesson 10:43-59）

```
对 dict 的每个状态值 X：
1. grep `setStatus(*_X)` / `setStatus("X")` 全 src/main 是否有 writer？
   → 有 writer：可达，正常。
   → 无 writer：死状态，进入步骤 2。

2. owner doc 迁移图是否声明了进入 X 的迁移？
   → 声明了但代码无实现：承诺漂移，进入步骤 3。
   → 未声明：纯预留死状态，进入步骤 3。

3. 显式裁决（二选一，禁止沉默保留）：
   a. 删 dict 项 + 删 *Constants 常量 + owner doc 标注「走 useLogicalDelete / 经 YY 表达 / Deferred 触发条件」。
   b. 实现迁移 BizMutation + owner doc 同步实际实现。
   → 裁决理由记录在 plan / owner doc。
```

### 反模式自检表（搬自 lesson 10:63-67）

- [ ] 对 dict 每个值 grep 了 `setStatus` writer？无 writer 的值已显式裁决（删 / 实现 / Deferred 标注）？
- [ ] owner doc 迁移图声明的每个「进 / 出迁移」在代码中有对应 action / setStatus？
- [ ] `*Constants.java` 中的状态常量是否都有使用方（零使用 = 死常量）？
- [ ] 删除 dict 项时是否同步删了常量 + 更新 owner doc（三处同步）？
- [ ] Deferred 处置是否在 owner doc 标注了**触发条件**（何时补实现）而非仅标 Deferred？

**参考证据**：lesson 10 表格列 finance/mfg/hr/inv/qa/prj/contract/aps/logistics（R1.13/14/15/19/20/21/22/25）。

## §3 调度链 + 守卫完整性扫描

### §3.1 调度链扫描（审计对象 = job 级调度拓扑，参考 `docs/audits/2026-07-04-0000-batch-scheduling-audit.md`）

**与 §3.2 审计对象不同**：§3.1 审调度拓扑（job 间接力 / 依赖图 / 重试对称 / 状态闭合），§3.2 审代码层守卫清单（状态机非法迁移 / 并发 / 权限注解 / 输入边界）。两者分立避免膨胀（README §反模式 #4「一个 skill 试图覆盖所有检查」）。

**目标**：找出调度链节点缺失败接力 / 重试拓扑 / 状态闭合的链路断裂。

#### 调度链拓扑分析程式

```
# 1. 定位该域所有 job（cron 配置键 + batch.xml + nop-job 直调）
rg -n 'erp-<short>\.' docs/architecture/job-scheduling.md  # 该域作业目录行
rg -n '<job|<batch|cron' module-<domain>/erp-<short>-service/src/main/resources/
# 2. 对每个 batch.xml，列 processor / step 段的全部调用，核对 job 声明的语义是否被实现
rg -n '<processor|<step|<action' module-<domain>/.../something.batch.xml
# 3. 核对 BizModel 入口可达性（catalog 声称的入口方法是否真实存在）
rg -n 'public.*<methodName>\(' module-<domain>/erp-<short>-service/src/main/java/
```

#### 4 信号核查（对每条调度链逐项问）

1. **失败接力策略**：调度链中 job A 失败时，下游 job B / C 的接力策略是否定义？（A 失败 → B 跳过 / B 仍跑 / B 回滚）未定义 → 链断裂。
2. **依赖图无环无孤立**：调度依赖图是否有环（死锁）或孤立节点（永不触发）？孤立节点 = 死调度（如 `spc-sampling.batch.xml` 漏调 `evaluateRules` → 规则评估在生产调度路径永不触达）。
3. **重试对称性**：失败重试是否对称（同步路径有兜底 / 异步路径有 nop-job 重试）？单边重试 → 异常路径悬挂。
4. **状态闭合**：调度链节点是否显式三态（成功 / 失败 / 放弃）？只有成功 / 失败二态 → 放弃类（IGNORED）悬挂，逃逸监控。

#### 反模式自检表

- [ ] 调度链 job A 失败 → 下游 B/C 接力策略已定义（跳过 / 仍跑 / 回滚）？
- [ ] 调度依赖图无环、无孤立节点（每个 job 都有触发路径）？
- [ ] batch.xml processor 段声明的全部调用都真实存在且被调度链触达（无漏调）？
- [ ] 失败重试对称（同步兜底 + 异步 nop-job 双路径）？
- [ ] 调度链节点状态显式三态（成功 / 失败 / 放弃），放弃类有处置理由？

**参考证据**：`docs/audits/2026-07-04-0000-batch-scheduling-audit.md`（6 维度方法）+ P1-RC-042（`spc-sampling.batch.xml` 漏调 `evaluateRules`，生产 cron 永不触达规则评估 → NCR/CAPA 自动级联断裂）。

### §3.2 守卫完整性扫描（审计对象 = 代码层守卫清单）

**目标**：把散落在 3 个技能（`state-machine-business-review` / `nop-platform-conformance` / `code-quality-audit`）的守卫类检查合并为单切片单域的统一清单。

#### 守卫清单核查（4 类守卫逐项问）

1. **状态机非法迁移守卫**：每个状态迁移 BizMutation 是否前置校验当前状态合法性（如 `validateNotCancelled` / `validateTransition*`）？缺守卫 → CANCELLED 单据仍可 approve（approveStatus 副轴漂移）。
2. **并发乐观锁守卫**：状态承载实体是否声明 `versionProp="version"`（@Version 透明乐观锁）？无 versionProp → silent lost-update（应降级为 detectable conflict）。核对 `docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`。
3. **`@BizMutation` / `@BizQuery` 权限注解守卫**：每个公开动作是否绑定权限注解（`@BizMutation` 自动事务 + 权限；`@BizQuery` 读取权限）？裸 `@BizModel` 不继承 CrudBizModel + `IServiceContext` 全文零引用 → 绕过认证管道（核对 `docs/audits/2026-07-29-1410-arm-ma6-permission-*.md`）。
4. **输入边界守卫**：BizMutation 入参是否做边界校验（空集合 / 负数 / 非法枚举 / 跨租户）？缺边界守卫 → 脆弱代码路径（换一个输入值就崩）。

#### 反模式自检表

- [ ] 状态迁移 BizMutation 前置校验当前状态合法性（CANCELLED / 终态不可恢复守卫齐全）？
- [ ] 状态承载实体声明 `versionProp`（@Version 透明乐观锁）？
- [ ] 公开 `@BizMutation` / `@BizQuery` 动作绑定权限注解，无裸 `@BizModel` 直访绕过认证管道？
- [ ] BizMutation 入参做边界校验（空集合 / 负数 / 非法枚举 / 跨租户）？
- [ ] 守卫失败经 `NopException` + `ErrorCode` 抛出（非 silent warn / return）？

**参考证据**：`docs/audits/2026-07-29-1410-arm-ma6-permission-annotation-completeness.md` / `2026-07-29-1410-arm-ma6-data-permission-runtime.md` / `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`。

## 严重性指南

- `blocker`：吞异常致 posted 悬挂且影响 GL 平衡 / 调度链断裂致核心业务循环不可达 / 守卫缺失致活跃数据破坏。
- `major`：dict 死状态 owner doc 承诺漂移 / 调度链孤立节点 / 守卫缺失致状态副轴漂移 / catch 吞咽但已有兜底重试。
- `minor`：调色板视觉类（非本技能范围）/ 命名规范类（用 orm-model-audit）。
- `note`：观察项或可选优化。

## 输出格式

按 B1 / B2 / B3.1 / B3.2 / B4 五段分组返回 finding，每个含：失败模式类别、控制点（file:line）、4 信号核查结果、裁决（pass / 改进 / Deferred 显式触发条件）、参考证据编号。

然后返回：
- 裁决：`passes behavioral scan` 或 `needs revision`
- 扫描范围（域 / 切片）
- B1/B2/B3.1/B3.2/B4 各类摘要（命中数 + 典型控制点）
- 与既有 arm-index finding 的「复用 or 新增」裁决（同型归并，避免重复登记）
- 剩余风险或跳过的区域

如果 B1/B2/B3.1 任一发现 blocker，或累计 major 超过阈值（建议 ≥3），说 `needs revision`。否则 `passes behavioral scan` 并列出剩余风险。
```

## 与现有技能的边界声明

- **不替代 `state-machine-business-review-prompt.md`**：那是设计层状态机图审查（状态定义 / 转换完整性 / 角色 / 待办策略），本技能 §2 dict 可达性是其代码层落地核查。两者互补——设计层提示给出迁移图正确性，本技能给出 dict 值的 writer 可达性。
- **不替代 `closure-audit-prompt.md`**：那是单计划闭合审查（验收标准 / 证明 / 闭合门控），本技能是单域代码层行为扫描，可作 closure-audit 的强制前置或独立维度。
- **不替代 `nop-platform-conformance-audit-prompt.md`**：那是平台规则合规（异常 extends NopException / @Inject private / 字符串 ==），本技能审的是 catch 宽度 / 标志位终态 / 告警通道，与平台规则正交。
- **不替代 `multi-dimensional-audit-prompt.md`**：那是多维整件工作挑战（跨需求 / owner-doc / 架构 / 验证 / 回归 / 路由 / 范围 7 维度），本技能抽象层更低（单域代码层扫描）。behavioral-failure-mode-scan 可作 multi-dim 某维度内的输入证据采集，但不替代其维度框架——两者抽象层不同。
- **理由**：避免单 skill 膨胀（README §反模式 #4「一个 skill 试图覆盖所有检查 = 该 skill 膨胀，其他 skill 闲置」）。B1/B2/B3/B4 四类共享同一审计对象（代码层行为闭环）/ 同一生命周期（closure 前由独立子代理运行）/ 同一证据来源（lessons 09/10 + batch-scheduling-audit + MA6 守卫证据），合并优于分立。若拆为 4 个独立技能，会让技能库退化为「按业务标签分文件夹」（README §技能路由规则 #3「按工作方法匹配而非仅业务标签」），且独立子代理在 closure 前需轮询 4 个技能入口，违反「按阶段交替使用」的组合设计。
