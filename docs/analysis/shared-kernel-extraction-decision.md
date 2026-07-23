# 共享内核抽取裁决（F4 闭包项 #5）

> **创建日期**: 2026-07-24
> **来源**: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F4（MEDIUM）+ §闭包前必须项 #5（P1）
> **触发计划**: `docs/plans/2026-07-24-1400-1-shared-kernel-extraction.md` Phase 1
> **前置**: `docs/analysis/governed-path-cost-evaluation.md`（F1 governed path 裁决，已完成）
> **裁决范围**: 对 finance/master-data 事实上承担的 3 个跨域语义类型，裁决其治理形态（抽取公共模块 / 接受为显式共享内核 + 守卫 / SPI 接口降级），消除"未声明的隐性内核"状态
> **非目标**: 不重构 daoFor 真违规子集；不搬实体；不重命名既有类型；不改 ORM 字典

## 1. 评估动机

架构治理审查 §F4 指出 finance 与 master-data 事实上承担未声明的公共内核职责：3 个跨域语义对象（1 enum + 2 class）被多域跨模块 import，但所有权、变更影响域、依赖方向均未在任何 owner doc 显式登记。审查闭包项 #5 原提议"抽取 `app-erp-common-api`（仅 SPI 接口，零 dao/entity），finance/master-data 提供实现"，但**该提议未考虑 3 类型的形态差异**——尤其 enum 能否降级为 SPI 接口未经技术论证。本裁决经原型探索逐类型评估 3 个候选机制，产出带证据的 go/no-go + 机制选择。

## 2. Phase 1.1 — 3 类型分类（实仓实测，2026-07-24）

| 跨域语义对象 | 类型 | 形态 | 所属 Maven 模块 | 跨域 import 文件数¹ | ORM `*.orm.xml` 引用² |
|---|---|---|---|---|---|
| `ErpFinBusinessType` | **enum**（56 常量，绑定字典 `erp-fin/business-type` code） | 纯枚举（import 仅 `java.*`，零 dao/entity） | `module-finance/erp-fin-dao` `app/erp/fin/dao/` | **69** | **0** |
| `PostingEvent` | **class** | **纯数据 DTO**（import 仅 `BigDecimal`/`LocalDate`/`Map`，零 entity/dao） | `module-finance/erp-fin-dao` `app/erp/fin/dao/` | **66** | **0** |
| `AcctSchemaResolver` | **class** | **dao 耦合静态工具**（import `ErpMdAcctSchema` entity + `IDaoProvider` + `QueryBean`，方法体调 `daoProvider.daoFor(ErpMdAcctSchema.class)`） | `module-master-data/erp-md-dao` `app/erp/md/dao/` | **38** | **0** |

¹ import 语句级计数，排除 `**/test/**` + 自身域（finance / master-data）。计数命令：
`rg -l "import app\.erp\.fin\.dao\.ErpFinBusinessType;" --glob '!**/test/**' --glob '!**/module-finance/**' | wc -l`（=69）；`PostingEvent` 同型（=66）；`AcctSchemaResolver` 排除 `module-master-data`（=38）。三数均与计划基线 69/66/38 一致。
² `rg -n "<Type>" --glob '**/*.orm.xml'` 对三者均 0 命中——3 类型均不被 ORM 模型引用，非 ORM 绑定。

### 2.1 消费层分布（关键证据）

3 类型的跨域消费者**全部位于 `-service` 层**（非 `-dao`），分布在 10 个业务域：

| 消费域 | ErpFinBusinessType | PostingEvent | AcctSchemaResolver |
|---|---|---|---|
| assets | 19 | 19 | 9 |
| inventory | 11 | 10 | 3 |
| manufacturing | 10 | 9 | 5 |
| sales | 6 | 5 | 3 |
| purchase | 5 | 5 | 3 |
| projects | 5 | 5 | 2 |
| maintenance | 5 | 5 | 2 |
| quality | 3 | 3 | 0 |
| hr | 3 | 3 | 0 |
| logistics | 2 | 2 | 1 |
| finance³ | — | — | 10 |
| **合计** | **69** | **66** | **38** |

³ `AcctSchemaResolver` 所属域是 master-data，故 finance 是其跨域消费者（10 处，最大消费方）。

> 计划 Current Baseline 措辞"消费者横跨 `-dao` 与 `-service` 两层"**经实仓复核修正为：消费者全部位于 `-service` 层**（0 处跨域 `-dao` 消费）。这不改变架构结论，反而简化了候选 (a) 的影响面评估（仅需调整 `-service` 层 import）。

### 2.2 enum 的派发消费形态（候选 (c) 可行性的决定性证据）

`ErpFinBusinessType` 的跨域派发消费有两种形态，**均要求编译期持有具体 enum 类**：

1. **`==` 常量比较**（跨域，7 站点 / 4 文件）：`event.getBusinessType() == ErpFinBusinessType.PURCHASE_INPUT`（`InvAcctDocProvider.java:61,64`、`PurAcctDocProvider.java:70,79`、`SalAcctDocProvider.java:64,71`、`InvPostingDispatcher.java:216`）。
2. **`switch(enum)`**（finance 自身域，3 站点）：`switch (event.getBusinessType()) { case NOTES_PAYABLE_ISSUED: ... }`（`NotesPayableAcctDocProvider.java:47`、`NotesReceivableAcctDocProvider.java:55`）、`switch (businessType)` 其中 `businessType` 类型为 `ErpFinBusinessType`（`ErpFinArApItemGenerator.java:140,144`）。

> **澄清计划基线措辞**：计划称"实仓 0 处 `switch`，派发经 `CONSTANT ==` 比较"。实仓复核更精确：**跨域消费者 0 处 `switch(enum)`**（4 个 `*ReversalListener` 的 `switch(businessType)` 是对 `VoucherReversedEvent.getBusinessType()` 返回的 **String** 做 switch，非 enum；其 enum import 仅用于 javadoc `{@link}`）；**finance 自身域有 3 处 `switch(enum)`**。两类形态共同结论不变：**enum 派发依赖具体 enum 常量，接口无法承载**——`==` 比较的右操作数必须是具体 enum 常量，`switch(enum)` 的 case 标签必须是 unqualified enum 常量，二者在 Java 语义下均强制要求具体 enum 类存在于编译期 classpath。

## 3. Phase 1.2 — 3 候选机制原型证据

### 候选 (a)：新增 `app-erp-common-api`/`-dao` 顶层模块，把类型移入

逐类型可行性：

| 类型 | 可移入"零 dao/entity"common 模块？ | 证据 |
|---|---|---|
| `ErpFinBusinessType` | 物理可行（纯枚举） | import 仅 `java.*`（源码 line 1 无 dao/entity import） |
| `PostingEvent` | 物理可行（纯 DTO） | import 仅 `BigDecimal`/`LocalDate`/`Map`（源码 line 3-6） |
| `AcctSchemaResolver` | **不可行**（dao 耦合） | import `ErpMdAcctSchema` entity + `IDaoProvider` + `QueryBean`（源码 line 3-6），方法体 `daoProvider.daoFor(ErpMdAcctSchema.class)`（line 32）——移入"零 dao/entity"模块会违反该模块的定位前提 |

**对 2 个物理可行类型的成本/收益评估**：

- **成本**：移入新模块 = 新 package（`app.erp.fin.dao` → `app.erp.common.*`）→ 135 个跨域 import 站点（69+66）连锁修改 + 新顶层 Maven 模块（root pom 注册 + `app-erp-all` 聚合）+ DAG 结构变更（finance 从 DAG 顶新增对 common 的下行依赖）。
- **收益**：仅"代码物理位置"反映共享内核身份。但 F4 的问题是**所有权/治理未声明**，是文档/治理问题，非代码结构问题——移位不提供任何 (b) 不能提供的额外所有权清晰度。
- **与审查 #5 原意不符**：审查 #5 字面要求"`ErpFinBusinessType` 改为 `IErpFinBusinessType`"（**接口**）。但 enum 不可降级为接口（见 §2.2 + 候选 (c)）。把具体 enum 移到 common-api **并不产生接口**，只是换了 package——既不满足审查 #5 的 SPI 意图，又付出 135 站点改动成本。
- **与计划 Non-Goals 冲突**：计划明令"不重命名既有类型（避免 179+ import 站点连锁修改）"。移 package 是同类 churn（虽非重命名，但 import 路径全量改写）。

**否决**：成本（135 站点 + 新模块 + DAG 改动）与收益（仅物理位移，零治理增量）严重不匹配；且无法满足审查 #5 的 SPI 接口意图（enum 不可接口化）。

### 候选 (b)：接受为显式共享内核（类型不动）+ owner doc 登记 + checker 规则追踪基线

- **owner doc 登记**：在 `module-boundaries.md` 增"共享内核（Shared Kernel）"章节，显式登记 3 类型的所有权（finance/master-data）、消费域、变更影响域、依赖方向；在 `data-dependency-matrix.md` 增共享类型归属。
- **机器守卫**：在 `nop-compliance-checker.sh` 增 R12 规则，追踪 3 类型的跨域 import 语句级计数（基线 69/66/38），并在 `docs/audits/compliance-baseline.md` 增 R12 基线行。CI（`.github/workflows/compliance.yml`）已实现"baseline 规则逐一比对"门控——R12 进入 checker 汇总表 + baseline yaml 块后即被自动强制（actual > baseline → CI fail）。任何未记录增长会被守卫捕获。
- **零代码位移**：135 个 import 站点零改动；零新模块；零 DAG 结构变更。
- **满足审查 #5 checkpoint 意图**：审查 #5 的 verification checkpoint 为"master-data 零业务依赖 + `ErpFinBusinessType` 跨域 import 收敛或经登记豁免"。计划 Phase 3 Exit Criteria 第 1 条已显式 sanctioned 此路径："经 Phase 1 裁决若 enum 不可降级为 SPI，则以登记豁免/checker 基线替代"。本裁决已确立 enum 不可降级（§2.2），故登记豁免 + checker 基线是审查认可的唯一可行替代。
- **与项目既有治理模式一致**：`posting-exemptions.md` 登记跨域写豁免、`governed-path-cost-evaluation.md` 把 Type 4 daoFor 登记为 deferred——"激活既有 guard 而非新建 wall"是本项目已确立的治理范式（审查 §结论 关键洞察）。

### 候选 (c)：为 enum 引入 SPI 接口（enum implements interface），消费者改依赖接口

- **技术否决（决定性）**：enum 派发消费经 `== ErpFinBusinessType.CONSTANT`（7 跨域站点 / 4 文件）与 `switch(enum)`（3 finance 站点）。Java 中 `==` 比较的右操作数与 `switch` 的 case 标签**必须是具体 enum 常量**，接口无法提供。引入 `IErpFinBusinessType` 接口后，这些站点仍需 import 具体 `ErpFinBusinessType` 类取得常量——接口只覆盖"参数类型声明"（如 `IErpFinVoucherBiz.post` 的形参），**不覆盖派发消费**，是半解。
- **D1 先例不适用**：`2026-07-16-2134-1` Decision D1 的 `Erp*DocStatus` 接口先例针对**字符串状态常量**（`"APPROVED"` 等字面量），消费者用 `Objects.equals(getApproveStatus(), ErpPurDocStatus.APPROVE_STATUS_APPROVED)` 比较——接口提供 `String` 常量字段即可承载。但 `ErpFinBusinessType` 是 **enum 类型**（非 String），消费者比较的是 enum 实例（`== PurBusinessType.X`），不是 String 字面量；`switch(enum)` 更是 Java 编译器生成 `Enum.ordinal()` 表，强绑定具体 enum 类。故 D1 的"接口承载常量"模式对 enum 类型不成立。
- **半解的代价**：新增一个仅被形参使用的接口 + 仍保留具体 enum 给派发消费 → 双重真相（接口 + enum），增加维护负担而无实质解耦。

**否决**：enum 派发消费强绑定具体 enum 类，接口仅半解且引入双重真相。

## 4. Phase 1.3 — 裁决

### 裁决 = 候选 (b)：接受为显式共享内核 + owner doc 登记 + checker R12 守卫

**理由汇总**：

1. **问题定性**：F4 是所有权/治理未声明问题（"Make Ownership Explicit"），不是代码结构问题。(b) 直接治理问题本身（显式化所有权 + 机器守卫），(a)/(c) 是用代码重构解决治理问题——药不对症。
2. **enum 不可降级**：经实仓证据确立（§2.2），审查 #5 的"SPI 接口"路径对 enum 类型在技术上不可行。这是裁决 (b) 而非 (a)/(c) 的技术硬约束。
3. **`AcctSchemaResolver` 不可移**：dao 耦合形态使其无法进入"零 dao/entity" common 模块（§3 候选 a 表），(a) 对此类型不可行。
4. **成本/收益**：(b) 零代码位移、零 DAG 风险、零 import 站点改动，即达成所有权显式化 + 机器守卫；(a) 付 135 站点 + 新模块 + DAG 改动代价却零治理增量。
5. **审查 sanctioned**：计划 Phase 3 Exit Criteria 已明确允许"enum 不可降级时以登记豁免/checker 基线替代"。
6. **范式一致**：与 `posting-exemptions.md` / `governed-path-cost-evaluation.md` 既有"登记 + 守卫"治理范式一致。

### 框架强制约束（作为约束记录，无需完整替代方案分析）

- **必须匹配既有 `erp-*-api` 模块结构**：本项目 19 个 `erp-*-api` 模块均为 codegen 产物（`groupId=io.nop.app`、依赖 `nop-api-core`、java 11，见 `module-finance/erp-fin-api/pom.xml`）。裁决 (b) 不新增模块，此约束自动满足。
- **不得搬实体**：审查警告"搬实体到 kernel 会让 master-data 失去 DAG 根地位"。裁决 (b) 零实体迁移，自动满足（master-data 根地位经 §5 验证保持）。

## 5. DAG 影响验证

裁决 (b) **不改动任何 Maven 依赖结构**，故 DAG 零变化。基线验证（供审查 checkpoint #5）：

- **master-data 零业务依赖**（实仓复核，2026-07-24）：`module-master-data/` 全部子模块（erp-md-{codegen,api,dao,meta,service,web,app}）的 `pom.xml` 中 `io.nop.app` 依赖仅指向自身（`app-erp-master-data-{codegen,dao,meta}` 等），**0 个跨业务域依赖**（`rg -A1 "io\.nop\.app" module-master-data/*/pom.xml | rg "<artifactId>" | rg -v "master-data"` 全空）。master-data 保持 DAG 根地位。
- **finance 是 DAG 顶**：`erp-fin-dao` 依赖 `app-erp-master-data-dao` + `app-erp-projects-dao`（master-data + projects 均在其下游），单向合法，无环。
- **裁决 (b) 后 `mvn dependency:tree -pl module-master-data` 仍零业务依赖**（Phase 3 闭包验证项；因 (b) 零依赖改动，结果与基线一致）。

> 反事实验证（候选 (a) 若被选）：把 `ErpFinBusinessType`/`PostingEvent` 移入 common 模块会使 finance 新增对 common 的依赖（finance → common），common 成为新的根级依赖；同时 10 个消费域的 `-service` 改依赖 common。虽 DAG 仍可保持无环（common 在 finance 与各域之下），但引入了新的依赖层 + 135 站点改动，与裁决 (b) 的零风险形成对比——这是 (a) 被否决的 DAG 维度补充证据。

## 6. 与审查闭包项 #5 的分歧处理

审查 #5 原文 verification checkpoint："`ErpFinBusinessType` 的 137 个跨域 import 改为 `IErpFinBusinessType`"。

**分歧**：本裁决**不**将 enum 改为接口，改以"owner doc 登记 + checker R12 基线（69，import 语句级）"替代。

**理由（技术硬约束）**：enum 派发消费（`==` 常量比较 7 跨域站点 + `switch(enum)` 3 finance 站点）强绑定具体 enum 类，接口无法承载（§2.2、§3 候选 c）。审查原文的"137"为含测试的宽口径；本裁决 checker 权威基线采用 import 语句级（69，排除测试 + 自身域），与审查 #5 的"跨域 import 收敛"语义一致（见计划基线 §计数口径说明）。

**Sanctioned 路径**：计划 Phase 3 Exit Criteria 第 1 条已显式允许此替代。审查 checkpoint 的**意图**（让共享内核所有权显式化 + 跨域 import 可追踪/可门控）经 (b) 完整满足。

## 7. 残留风险

1. **守卫依赖 CI 活跃**：R12 防护力取决于 `.github/workflows/compliance.yml` 持续运行。若 CI 停用则守卫退化为 dead armor（同 F8 风险）。缓解：R12 进入既有 baseline yaml 块，CI 解析逻辑无需改动即自动覆盖。
2. **登记 ≠ 阻止变更**：(b) 使共享内核变更**可见**（守卫捕获 import 增长 + owner doc 记录影响域），但不**阻止**合理的类型演进。新增 enum 常量（如新业务类型）仍允许，仅需同步更新 owner doc 与 checker 基线（经独立计划）。这是预期行为，非缺陷。
3. **`posting.md` 契约表 drift**：`docs/design/finance/posting.md §PostingEvent 契约`（line 50）将 `businessType` 标为 `String`，但实仓 `PostingEvent.java` 类型为 `ErpFinBusinessType` enum。这是既存的文档-代码 drift，非本裁决引入；Phase 2 更新 `posting.md` 时顺带校正（标注真实类型）。
4. **D1 推广的边界**：D1 的 `Erp*DocStatus` 接口模式适用于 String 状态常量，不适用于 enum 类型。未来若有人误将 D1 模式套用到 `ErpFinBusinessType`，会重蹈候选 (c) 的半解。缓解：本裁决文档 §3 候选 (c) + §6 显式记录此边界。

## 8. 落地动作（Phase 2 执行依据）

1. `module-boundaries.md`：新增"共享内核（Shared Kernel）"章节，登记 3 类型所有权/消费域/变更影响域/依赖方向。
2. `data-dependency-matrix.md`：增共享类型归属（与 `module-boundaries.md` 交叉引用）。
3. `nop-compliance-checker.sh`：新增 R12 规则（追踪 3 类型跨域 import 语句级计数）。
4. `compliance-baseline.md`：新增 R12 基线行（R12a=69 / R12b=66 / R12c=38）+ 机器可读 yaml 块。
5. `docs/design/finance/posting.md`：校正 `PostingEvent` 契约表 `businessType` 真实类型 + 引用本裁决。
6. `docs/design/master-data/README.md`：登记 `AcctSchemaResolver` 跨域共享工具身份 + 引用本裁决。

## 9. 关联文档

- 审查来源：`docs/audits/2026-07-23-0000-architecture-governance-review.md` §F4 / 闭包前必须项 #5
- 触发计划：`docs/plans/2026-07-24-1400-1-shared-kernel-extraction.md`
- governed path 前置裁决：`docs/analysis/governed-path-cost-evaluation.md`
- D1 共享常量接口先例：`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md` Decision D1
- checker 与基线：`docs/audits/nop-compliance-checker.sh` / `docs/audits/compliance-baseline.md`
- CI 门控：`.github/workflows/compliance.yml`
