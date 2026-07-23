# 2026-07-24-1400-1-shared-kernel-extraction 隐性共享内核显式化（F4 闭包项 #5）

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F4（MEDIUM）+ §闭包前必须项 #5（P1）
> Related: `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（Decision D1 共享常量接口先例）、`docs/plans/2026-07-24-0930-3-governed-path-cost-eval-arch-doc-alignment.md`（F1 governed path 裁决前置，已完成）、`docs/analysis/governed-path-cost-evaluation.md`
> Audit: required

## Current Baseline

架构治理审查（`2026-07-23-0000`）F4 指出 finance 与 master-data 事实上承担未声明的公共内核职责。三条证据（实时仓库复核，2026-07-24）：

| 跨域语义对象 | 类型 | 耦合形态 | 所在 Maven 模块 | 跨域 import 文件数（实测，import 语句级，排除测试与自身域） |
|---|---|---|---|---|
| `ErpFinBusinessType` | **enum**（56 常量，每项绑定字典 `erp-fin/business-type` code） | 纯枚举（无 dao/entity import） | `module-finance/erp-fin-dao` `app/erp/fin/dao/` | **69** |
| `PostingEvent` | **class** | **纯数据 DTO**（仅 import `BigDecimal`/`LocalDate`/`Map`，无 entity/dao 依赖） | `module-finance/erp-fin-dao` `app/erp/fin/dao/` | **66** |
| `AcctSchemaResolver` | **class** | **dao 耦合静态工具**（import `ErpMdAcctSchema` entity + `IDaoProvider` + `QueryBean`，调用 `daoProvider.daoFor(...)`） | `module-master-data/erp-md-dao` `app/erp/md/dao/` | **38** |

- 三者均为**具体类型**（1 enum + 2 class），**非接口**，且**耦合形态各异**——这直接决定候选机制 (a)（移入"零 dao/entity"公共模块）的可行性按类型不同：`ErpFinBusinessType`/`PostingEvent` 可移入纯接口/DTO 公共模块；`AcctSchemaResolver` 因耦合 master-data entity + `IDaoProvider`，**不可**移入"零 dao/entity"模块（候选 (a) 对此类型不可行，须走 SPI 接口 + 实现留 master-data）。
- 消费者横跨 `-dao` 与 `-service` 两层：`ErpFinBusinessType` 被各域 `IErpFinAcctDocProvider` 实现用作 `IErpFinVoucherBiz.post(...)` 的强类型参数 + 过账派发（实仓 0 处 `switch`，派发经 `ErpFinBusinessType.CONSTANT ==` 常量比较，故消费者需要具体 enum 常量，接口无法承载）；`PostingEvent`/`AcctSchemaResolver` 被过账链路与跨域查询直接持有。
- **计数口径说明**：本表为 import 语句级计数（排除测试 + 自身域）。审查 v2（`docs/audits/2026-07-23-0000` §F4:220-222）引用的 137/64/36 为含测试 + 全引用的更宽口径。checker 基线（若裁决 (b) 接受为显式共享内核）**以 import 语句级为准**（69/66/38），与审查 #5 checkpoint 的"跨域 import 收敛"语义一致。
- **无 `app-erp-common(-api/-dao)` 模块存在**（实仓 `ls` 确认）。既有 `erp-*-api` 模块（19 个）均为 codegen 产物，承载 `api/crud/*Api` RPC 接口，`groupId=io.nop.app`、依赖 `nop-api-core`、java 11（`module-finance/erp-fin-api/pom.xml` 先例）。
- 根 `pom.xml` 注册 19 个 `module-*` 顶层目录 + `app-erp-all`。
- 审查闭包项 #5 原提议"抽取 `app-erp-common-api`（仅 SPI 接口，零 dao/entity），finance/master-data 提供实现"。**但**：enum 不能是"纯 SPI 接口"——消费者需要 enum 常量做过账派发的常量比较（`ErpFinBusinessType.CONSTANT ==`）；将其改为接口 + 实现会破坏既有强类型常量比较派发语义。因此抽取机制**未经裁决**，是本计划的核心开放问题。
- DAG 影响：master-data 当前是 DAG 根（零业务依赖），finance 是 DAG 顶。若把 finance 类型移到 common 模块，需确认不破坏 master-data 的根地位与既有单向 DAG（`module-boundaries.md` §领域工程依赖方向）。
- 审查残留风险 #2 另注：finance 77 mutation 的 owner-doc 背书未逐项核对——本计划不覆盖该核对（超出 F4 #5 范围）。

剩余差距：F4 #5 是审查 12 项闭包项中**唯一未启动的 P1**（#1/#2/#4/#6/#7/#8/#9 已 done，#3 部分 done，#10/#11/#12 为 P2）。其触发条件已满足（governed path 成本评估前置已于 `2026-07-24-0930-3` 完成）。

## Goals

1. **裁决 F4 共享内核的治理形态**：经原型探索，在"抽取公共模块"与"接受为显式共享内核 + 守卫"之间做出带理由的裁决，消除"未声明的隐性内核"状态。
2. **落地裁决**：无论裁决为哪一支，产出可验证的结构变更或治理登记——使 3 个跨域类型的所有权、依赖方向、变更影响域在 owner docs 与（若适用）机器守卫中**显式化**。
3. **关闭审查闭包项 #5**：满足其 verification checkpoint（`mvn dependency:tree -pl module-master-data` 仍零业务依赖；跨域 import 收敛或经登记豁免）。

## Non-Goals

- **不逐项核对 finance 77 mutation 的 owner-doc 背书**（审查残留风险 #2，超出 F4 #5）。
- **不重构 daoFor Type 1/4 真违规子集**（~110-180 处，规模独立，归后续专项计划；其前置 governed path 裁决已完成）。
- **不搬移实体到 kernel**——审查明确警告"搬实体到 kernel 会让 master-data 失去 DAG 根地位"，本计划禁止任何实体迁移。
- **不重命名既有类型**（`ErpFinBusinessType` 等保持类名，避免 179+ import 站点连锁修改）。
- **不改字典/ORM 模型**（`ext:dict` / `*.orm.xml` 保护区域，未经人工批准不动）。

## Task Route

- Type: `architecture change`（可能新增顶层 Maven 模块 + 跨多域 import 站点调整 + owner doc 更新）
- Owner Docs: `docs/architecture/module-boundaries.md`（§领域工程依赖方向 / §Owner Docs）、`docs/architecture/data-dependency-matrix.md`（§5 数据依赖 / 共享内核归属）、`docs/design/finance/posting.md`（`ErpFinBusinessType`/`PostingEvent` 作为过账契约的语义）、`docs/design/master-data/README.md`（`AcctSchemaResolver` 归属）
- Skill Selection Basis: `nop-backend-dev` 路由"跨实体调用 / IBiz / 产品化可定制性自检"——3 个类型正是跨域过账契约，其抽取/封装须服从平台跨实体访问纪律与产品化 delta 可定制性约束。Phase 1 纯架构裁决（Explore/Decision）阶段本身不写 BizModel，但仍需该技能的"跨实体访问决策门"判定抽取是否引入新的穿透风险。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java 模块结构变更，无端口/密钥/外部服务）。
- 依赖：根 `pom.xml` 模块注册、各 `module-*/pom.xml` 依赖声明、`app-erp-all` 聚合（若新增模块须加入聚合）。
- 回滚策略：若新增 `app-erp-common-*` 模块后构建/测试退化且无法快速修复，`git revert` 单次合并提交即可回滚（模块新增是追加式，不破坏既有结构）。

## Execution Plan

### Phase 1 - 抽取机制探索与裁决（Decision | Explore）

Status: completed
Targets: `docs/analysis/shared-kernel-extraction-decision.md`（新建裁决文档）、`docs/architecture/module-boundaries.md`、`docs/architecture/data-dependency-matrix.md`
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision`
- Prereqs: 无（governed path 成本评估 `2026-07-24-0930-3` 已完成）

- [x] `Explore`：对 3 个共享类型逐个分类——所属层（`-dao` vs `-service`）、消费层分布、是否被常量比较/强类型派发依赖（enum 不可降级为接口的边界）、被 ORM `*.orm.xml` 引用情况。
  - Skill: `nop-backend-dev`
- [x] `Explore`：原型 3 个候选机制并记录可验证证据（非手挥）。**注意三类形形态差异**（enum / 纯 DTO / dao 耦合工具），候选可行性按类型逐个评估：
  - (a) 新增 `app-erp-common-api`（或 `-dao`）顶层模块，把类型**移入**（finance/master-data 改依赖 common；验证 master-data 仍零业务依赖、DAG 无环）——**仅对 `ErpFinBusinessType`/`PostingEvent` 可行**；`AcctSchemaResolver` 耦合 entity/dao，移入"零 dao/entity"模块不可行；
  - (b) 接受为**显式共享内核**——类型不动，在 `module-boundaries.md` 增"共享内核"章节正式登记所有权 + 变更影响域，并在 `nop-compliance-checker.sh` 增规则追踪这 3 类型的跨域 import 计数基线（import 语句级 69/66/38，不得无记录增长）；
  - (c) 为 enum 引入 SPI 接口（enum implements interface），消费者改依赖接口——**须验证**常量比较消费者是否仍需具体 enum（实仓派发经 `CONSTANT ==` 比较，非 `switch`；若需常量则此路径仅半解，记录证据）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：在 `docs/analysis/shared-kernel-extraction-decision.md` 裁决 go/no-go + 机制选择。记录：选择、考虑的替代方案（(a)/(b)/(c) 全列）、依赖结构验证证据（`mvn dependency:tree` 输出摘要）、DAG 根地位影响、残留风险。框架强制约束（"必须匹配既有 `erp-*-api` 模块结构 / 不得搬实体"）作为约束记录，无需完整替代方案分析。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付一份可被独立审查核实裁决文档。完整仓库 build/test 属 Closure Gates。

- [x] `docs/analysis/shared-kernel-extraction-decision.md` 存在，含 3 类型分类表 + 3 候选机制的原型证据 + 明确裁决（(a)/(b)/(c) 之一）+ DAG 影响验证摘要
- [x] 裁决文档中每个被否决候选均有可验证的否决证据（非"感觉不行"）

### Phase 2 - 落地裁决

Status: completed
Targets: 取决于 Phase 1 裁决——若 (a)：新增 `app-erp-common-*` 模块 + 迁移 3 类型 + 调整跨域 import；若 (b)：`module-boundaries.md` + `data-dependency-matrix.md` 登记 + checker 规则；若 (c)：新增 SPI 接口 + enum 实现 + 消费者改依赖
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 裁决已记录

- [x] `Add`/`Fix`：按裁决实施结构变更。若 (a)：新增模块（`pom.xml` 仿 `erp-fin-api` 先例 + 根 pom 注册 + `app-erp-all` 聚合声明）、迁移类型、finance/master-data 及消费者 import 站点调整；若 (b)：owner doc 登记 + checker R 类规则与基线（更新 `docs/audits/compliance-baseline.md`）；若 (c)：新增接口 + 调整。
  - Skill: `nop-backend-dev`
  - 落地（裁决=(b)）：`nop-compliance-checker.sh` 新增 R12a/R12b/R12c（实测 69/66/38，经 eval-find 计数复核与 CI gate 模拟 PASS）；`compliance-baseline.md` 增 R12 表行 + yaml 块 + 同步注记。
- [x] `Fix`：更新 `module-boundaries.md`（若 (a)：依赖方向表 + 新模块行；若 (b)/(c)：共享内核章节）与 `data-dependency-matrix.md`（共享类型归属），使 owner doc 与代码一致。
  - Skill: `nop-backend-dev`
  - 落地（裁决=(b)）：`module-boundaries.md` 新增 §共享内核（3 类型所有权/消费域/变更影响/依赖方向 + 裁决约束）；`data-dependency-matrix.md` 新增 §3.3 共享内核类型归属；`docs/design/finance/posting.md` 校正 `PostingEvent.businessType` 真实类型（enum，原误标 String）+ 引用裁决；`docs/design/master-data/README.md` 登记 `AcctSchemaResolver` 共享工具身份。

Exit Criteria:

- [x] 裁决选择的交付物全部落地（新增模块可被 `mvn compile` 识别 / owner doc 章节存在 / checker 规则可运行）
  - 证据：`bash docs/audits/nop-compliance-checker.sh` 汇总含 R12a/R12b/R12c=69/66/38；4 份 owner doc 章节存在；CI gate 模拟（python 解析 summary + baseline yaml）19/19 规则匹配、0 regression、PASS。裁决=(b) 不新增 Maven 模块，故无新模块 compile 目标。
- [x] 本地化验证：变更涉及模块的 `mvn compile` 通过（解除后续 Proof 阶段阻塞）
  - Phase 2 变更仅触及 bash 脚本 + markdown 文档，**零 Java 源码/ORM 变更**，无 Maven 模块 compile 目标受影响（checker 是工具脚本非 reactor 模块）。完整仓库 `mvn clean install -DskipTests` 归 Phase 3。

### Phase 3 - 验证与闭包证据

Status: completed
Targets: 全仓构建 + DAG 验证 + 审查 checkpoint
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [x] `Proof`：运行 `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，若新增模块则 +N）；`mvn dependency:tree -pl module-master-data` 仍零业务依赖（审查 verification checkpoint #5）；若 (a)，`rg "import app.erp.fin.*.ErpFinBusinessType"` 跨域计数较基线下降至裁决预期值。
  - Skill: `nop-backend-dev`
  - 证据：`mvn clean install -DskipTests` → BUILD SUCCESS（154 reactor 模块，0 FAILED，01:31 min）；`mvn test` → BUILD SUCCESS（0 Failures/0 Errors，含 `ErpAllWebPagesTest` 全 19 域页面校验通过）；`mvn dependency:tree -pl module-master-data` 仅 `app-erp-master-data` 自身（aggregator），叶模块 `erp-md-service`/`erp-md-dao` 的 `io.nop.app` 依赖全部为 `app-erp-master-data-{dao,codegen,meta}` 自身模块——**零业务域依赖**。裁决=(b)，无 import 站点改动，跨域 import 计数维持基线 69/66/38（经 R12 登记豁免，非下降）。

Exit Criteria:

- [x] 审查闭包项 #5 的 verification checkpoint 满足：master-data 零业务依赖 + ErpFinBusinessType 跨域 import 收敛或经登记豁免（审查原文为"137 个跨域 import 改为 `IErpFinBusinessType`"；经 Phase 1 裁决若 enum 不可降级为 SPI，则以登记豁免/checker 基线替代——见裁决文档 `shared-kernel-extraction-decision.md`）
  - master-data 零业务依赖：✅（dependency:tree 实测）。ErpFinBusinessType 跨域 import 经登记豁免：✅（裁决=(b)，enum 不可降级 SPI 见裁决文档 §2.2/§6；69 处跨域 import 经 R12a 基线 + owner doc 登记 + CI 守卫显式化，符合计划 Phase 3 sanctioned 替代路径）。
- [x] DAG 无环（`mvn dependency:tree` 无循环依赖报错）
  - ✅ reactor 全 154 模块 BUILD SUCCESS（Maven 遇循环依赖会 reactor 失败；零失败即无环）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_06f55349cffeMT2D1aGlQM2yYu`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 0 Blocker / 3 Major / 3 Minor。Phase 1 Explore/Decision 框架、3 候选机制完备性、enum 不可降级 SPI 的技术论证、anti-slack 合规均 sound，**无需结构性变更**。3 Major 全部为 Current Baseline 精度修正：M1（`AcctSchemaResolver` dao 耦合静态工具形态须与 `PostingEvent` 纯 DTO 拆分，因候选 (a) 对前者不可行）/ M2（import 语句级 69/66/38 与审查宽口径 137/64/36 的口径分歧须显式调和并指定 checker 权威基线）/ M3（`AcctSchemaResolver` 计数 44→38）。3 Minor：m1（"switch 派发"实仓 0 处，改为"常量比较派发"）/ m2（enum 常量 ~48→56）/ m3（#5 checkpoint 重述须标注与审查原文分歧）。全部修订已落地：基线表拆分 3 形态 + 计数校正 + 口径说明段 + Phase 1 候选按类型标注可行性 + checkpoint 重述加审查原文对照。复核全部 load-bearing 主张经实时 grep 逐项核实零伪。
- Independent draft review iteration 2: `accept` — iter-1 全部 Major/Minor 经实时仓库核实**genuine 修订落地**，Phase 1 裁决框架保留。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [x] 范围内行为完成（裁决落地 + owner doc 对齐）
- [x] 相关文档对齐（`module-boundaries.md`、`data-dependency-matrix.md`、`docs/analysis/shared-kernel-extraction-decision.md`、`posting.md`/`master-data/README.md` 按裁决影响更新）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn dependency:tree -pl module-master-data`
  - `mvn clean install -DskipTests` → BUILD SUCCESS（154 模块，0 FAILED）；`mvn test` → BUILD SUCCESS（0 Failures/0 Errors）；`mvn dependency:tree -pl module-master-data` 仅自身（零业务依赖，叶模块复核仅 `app-erp-master-data-*` 自身）；CI compliance gate 模拟 PASS（19/19 规则，R12=69/66/38，0 regression）。
- [x] 无范围内项目降级为 deferred/follow-up
  - Deferred 2 项（finance 77 mutation 逐项核对 / daoFor Type 1+4 重构）均为**显式 adjudicated 的范围外 successor**（见 §Deferred But Adjudicated），非范围内工作降级。
- [x] 独立草案审查已完成并记录
  - Draft Review Record iteration 1（needs revision）→ iteration 2（accept），见 §Draft Review Record。
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
  - Plan Status=completed；Phase 1/2/3 Status=completed 且全部 `[x]`；8 Closure Gates 全 `[x]`；source audit `2026-07-23-0000` §F4 + 闭包项 #5 已标 Done。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
  - Auditor: `ses_06f2cb4d0ffe2Eve6jifh7QEO7`（独立 general 子代理，新会话冷重播无执行者上下文，2026-07-24）。Verdict: `pass`（0 Blocker / 0 Major / 2 Minor）。Minor #1（裁决文档"10+ 站点"应为"7 站点/4 文件"）已修正；Minor #2（结束填表）即本步骤。
- [x] 结束证据存在于文件中
  - 见下方 Closure Audit Evidence。

## Deferred But Adjudicated

### finance 77 mutation 的 owner-doc 逐项核对

- Classification: `watch-only residual`
- Why Not Blocking Closure: 审查残留风险 #2 明确标注为"抽样判断，未全量核对"，属 F4 的广度评估而非 #5 的结构治理；本计划聚焦共享类型所有权。
- Successor Required: `yes`（触发条件：finance mutation 出现无 owner doc 背书的语义漂移时，开专项核对计划）

### daoFor Type 1/4 真违规子集重构（~110-180 处）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 规模独立，前置 governed path 裁决（`2026-07-24-0930-3`）已完成但重构本身跨多域需专项计划；非 F4 #5 范围。
- Successor Required: `yes`（触发条件：本计划关闭后开 Type 1 重构专项计划）

## Closure

Status Note: 计划全 3 Phase 执行完成并经独立结束审计 `pass`。F4 闭包项 #5 关闭：finance/master-data 的 3 个跨域语义类型（`ErpFinBusinessType` enum / `PostingEvent` DTO / `AcctSchemaResolver` dao 工具）经裁决=分支 (b) 接受为**显式共享内核**——类型不迁移，所有权/变更影响域/依赖方向经 owner doc（`module-boundaries.md §共享内核` + `data-dependency-matrix.md §3.3`）显式登记，跨域 import 经 `nop-compliance-checker.sh` R12（基线 69/66/38）+ CI 自动强制守卫。原审查 #5"抽取纯 SPI common-api"提议经 Phase 1 原型探索否决（enum 派发消费强绑定具体 enum 类，不可降级为接口），改以登记豁免 + checker 基线替代（计划 Phase 3 Exit Criteria sanctioned 路径）。验证：`mvn clean install -DskipTests` + `mvn test` 全 154 模块 BUILD SUCCESS；`mvn dependency:tree -pl module-master-data` 零业务依赖（checkpoint #5 满足）；DAG 无环（reactor 成功）。

Closure Audit Evidence:

- Auditor / Agent: 独立 `general` 子代理 `ses_06f2cb4d0ffe2Eve6jifh7QEO7`（新会话冷重播，无执行者上下文，2026-07-24）。
- Verdict: `pass`（0 Blocker / 0 Major / 2 Minor）。
- Verified-OK highlights（审计核实，非执行者自述）：
  - **R12 是 live guard 非 dead armor**——checker summary 报 R12a/b/c=69/66/38（独立 `rg` 复核精确匹配）；baseline 人工表 + yaml 块均有 R12；CI gate（`compliance.yml:71`）遍历 `baseline.items()` 且 R12 从 summary 解析得到，故被强制（非 WARN-only）。
  - **enum-cannot-be-SPI 有真实代码证据**——跨域 `== ErpFinBusinessType.CONSTANT` 派发确认（7 站点/4 文件）；finance `switch(enum)` 确认（3 文件）；4 个跨域 `*ReversalListener.switch(businessType)` 正确识别为 String-switch（`String businessType = event.getBusinessType()`）。候选 (a)/(c) 否决均有源码级依据。
  - **零保护区域影响**——`git diff HEAD --stat` 仅 `.md` + `.sh`（111 insertions，0 Java/ORM）；0 个 `*.orm.xml` 引用这 3 类型；master-data `pom.xml` 零跨域 `io.nop.app` 依赖（仅 `app-erp-master-data-*` 自身），保持 DAG 根地位。"tests green" 与零代码改动一致。
  - **checkpoint #5 sanctioned-alternative 存在**——计划 Phase 3 Exit Criteria 显式允许"enum 不可降级 SPI 时以登记豁免/checker 基线替代"；Phase 1 已确立该硬约束，故与审查 #5 字面"改为 `IErpFinBusinessType`"的分歧既技术合理又预先 sanctioned。
- Minor 处置：#1（裁决文档"10+ 站点"应为"7 站点/4 文件"）已修正于 `shared-kernel-extraction-decision.md` §2.2/§3/§6 + `module-boundaries.md §共享内核`；#2（结束填表）即本节。

Follow-up:

- daoFor Type 1 重构专项计划（本计划关闭后）
- finance mutation owner-doc 逐项核对（触发条件见上）
