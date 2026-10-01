# 2026-10-01-2142-1 M1.2 fixture-bundle 分层导入器

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M1.2（base 业务键对账 + payload 剥 PK 重映射 + 引用重写 + requires 校验）
> Related: `docs/plans/2026-10-01-2049-1-m11-fixture-bundle-format-exporter.md`（包格式/导出器/校验器已落地 done）；`docs/plans/2026-10-01-1853-1-m01-fixture-bundle-spike-six-decisions.md` Decision ③（remap 主策略 + 取号无冲突断言口径三件套）
> Audit: required

## Current Baseline

- **M1.1 导出侧已落地**（`io.nop.autotest.bundle` 10 文件入库 nop-entropy `9b0a704f79`）：manifest schema（roadmap 强制清单 11 项全承载）、`FixtureBundleRecordingSession`、`FixtureBundleExporter`、`FixtureBundleValidator`+`FixtureBundleSensitiveRules`、`TestFixtureBundleExport` 5 测试；runbook `fixture-bundle.md` 导出侧章节 + INDEX 路由。M1.1 预留了列指纹的**前向扩展授权**（TableEntry javadoc + 批准条件 B-C2），字段本体与导出侧生产者由本计划新增（roadmap 强制清单 11 项的措辞指 M1.1 Decision A 口径，非字段计数）。
- **M0.1 Decision ③（ID 策略）**：remap 主策略——payload 剥 PK 交平台生成 + `oldId→newId` 映射；「后续取号无冲突」断言口径三件套（新 ID ∉ oldId 集 / 单调序列 `NEXT_VALUE > max(新 ID)` / 再取一号 ∉ 已导入集）为 M1.3 验收①使用，本计划实现导入器使其可执行。
- **平台事实**（roadmap §当前基线 + M0.1/M1.1 实测）：直插不触发 `validateRefValue`（悬空引用静默成功 → **导入器必须自带引用完整性断言**）；平台 DDL 零 FK（插入序非 DB 约束问题，拓扑序 = 业务前置态可读性）；`sortEntityModelInTopoOrder(Collection)` 子集排序；`AutoTestCaseDataBaseInitializer` 为导入路径参考实现（public、单事务、只为自身 CSV 建表——但按文件名字母序、不重映射 ID）；手工 ID 不消耗不推进 `nop_sys_sequence`、无 `syncCurrentSeq` API、序列修正唯一现成手段 = `NopSysSequence` CRUD（`afterEntityChange` → `removeCache`）；nop-autotest-core 无 nop-sys 依赖（动态实体按名操作可绕开编译期依赖，但 BizModel 级 removeCache 钩子不在 ORM 层）；roadmap 复用表为 M1.2/M1.3 预留 `UuidSequenceGenerator` 无 nop-sys 环境的序列覆盖选项（测试 bean 覆盖先例 nop-datav）——Decision E 裁决输入。
- 剩余差距：无导入器；manifest 无列指纹字段；runbook 导入节为占位。

## Goals

- **分层导入器**（`io.nop.autotest.bundle` 新增类）：
  - base 趟：按业务键对账——目标库已存在同业务键行则登记映射不覆盖；不存在则剥 PK 由平台生成并登记映射；base 趟整趟单事务（与 payload 趟各自独立边界，base 失败不进入 payload 趟）。
  - payload 趟：全部行剥 PK 由平台生成 + `oldId→newId` 映射；to-one 引用列按映射重写；**按 manifest `loadOrder`（子集拓扑序）装载**；单事务、失败整体回滚；**系统表/序列表（`nop_sys_*` 形）不属可导入面，manifest 命中即拒绝（具名 ErrorCode）**；**`version`/`delVersion` 一律置初值（0），不读包值**。
  - requires 校验：manifest.requires 中任一 bundle 无法在目标环境发现/装载时抛 `NopException` + 具名 ErrorCode。
  - 漂移校验：导入前按 manifest 列指纹比对目标 `EntityModel`，不一致默认 fail-fast（可配 tolerant 登记告警）。
  - 原子性与幂等：导入前目标残留检测（业务键命中即跳过登记，不重复插入）；payload 行携带包内稳定标识（源 ID 映射表）支持幂等跳过；业务键 UK 冲突转 `NopException` + ErrorCode 而非底层 SQL 异常。
  - 逻辑删除口径：manifest 表条目 `includeLogicalDeleted` 显式声明（导出器依据导出行集写入；默认语义 = 不含 `delVersion > 0` 行）；导入侧声明语义：`includeLogicalDeleted=true` 的包 → 导入拒绝（具名 ErrorCode，已删行不进目标库）——导入器永不重建已删行。
- **manifest schema 扩展 + 导出侧生产者**（本计划自身 dual-agent-approval 范围内明示扩面）：表条目新增 `columnFingerprint`（导出时由列名序集计算写入——生产者在导出器，杜绝「测试手工构造才可达」的死代码；规范化两侧同源：同一工具方法，导出与导入均对**目标 EntityModel 列集**取排序摘要——结束审计 MINOR-2 措辞更正：实现比对目标 EntityModel 与 roadmap 漂移定义一致且强于 CSV 并集口径，批准条件 B3 的核心[同源]已履行）+ `includeLogicalDeleted`（导出依据行集声明）+ 导入器对 `version`/`delVersion` 一律置初值不读包值。格式版本口径（M1.1 Decision A 受控要求）：`formatVersion` 保持 1，新字段为可选字段+读取容错（旧 manifest 无指纹 → 跳过漂移校验并登记告警）。
- **导入侧验收测试**（nop-autotest-core test scope，沿 spike 模型）：干净库回放 / 脏环境 base 不覆盖 / requires 缺失报错 / 漂移 fail-fast / 引用重写与悬空检测 / 幂等重导入。
- **owner doc 扩写**：runbook「导入」节（导入 API、两趟事务边界、映射表形态、指纹与漂移策略、序列对齐口径）。

## Non-Goals

- 不实现批量/CLI 入口（M2.1）、skill（M2.2）、本仓消费示例（M2.3）、三条验收用例的正式落位（M1.3——本计划的导入侧测试是开发验证，M1.3 按其 plan 落位到指定模块）。
- 不改变既有 `_cases`/CHECKING 行为；不触碰 `AutoTestCaseDataBaseInitializer`。
- 不触碰本仓 `module-*/model/*.orm.xml` 与 `_init-data/**`；`erp_fin_*` plan-first 叠加条件 = 包内含 `erp_fin_*` 表——本计划测试 bundle 使用 spike 合成实体，包内容不含 `erp_fin_*`，不触发。
- 不提供序列自动对齐的 BizModel 级实现（见 Decision E 口径）。
- 不做跨方言回放（Non-Goal 沿 roadmap）。

## Task Route

- Type: `architecture change`（nop-entropy 平台层框架能力续建）
- Owner Docs: `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md`「导入」节扩写
- Skill Selection Basis: `Skill: nop-testing`（工具原生技能，`.opencode/skills/nop-testing/SKILL.md`，M0.1 确认口径）。保护区：nop-entropy 全部写入 = `auto + dual-agent-approval`——草案审查通过后、实施开始前两名独立子代理批准，记录落盘本计划。

## Infrastructure And Config Prereqs

- 跨仓验证命令：`cd ../nop-entropy && ./mvnw test -pl nop-autotest/nop-autotest-core -am`；长输出落 `_tmp/2026-10-01-2142-1-*.log`。
- 零新增依赖（复用 M1.1 依赖面：nop-orm/nop-core/nop-ioc[test]/h2[test]/junit-jupiter[test]）。
- 回滚策略：全部产物为新文件 + runbook 单节扩写 + manifest schema 加字段（向后兼容：新增可选字段，M1.1 导出的旧 manifest 无指纹时按「无指纹=跳过漂移校验+登记告警」处理）——revert 单提交即可。

## Execution Plan

### Phase 1 - 导入器实现（main scope）

Status: completed
Targets: `../nop-entropy/nop-autotest/nop-autotest-core/src/main/java/io/nop/autotest/bundle/`（新增导入类 + manifest 扩展）
Skill: `nop-testing`

- Item Types: `Add`（导入器）、`Decision`（3 条：序列对齐口径 / 引用完整性断言范围 / 映射表与幂等标识形态）
- Prereqs: dual-agent-approval 双批准；M1.1 done

- [x] （Decision | Add）Decision E/F/G 定稿并回填 `## Decisions` 节
      - Skill: `nop-testing`
- [x] （Add）`FixtureBundleManifest` 扩展 `columnFingerprint` 与 `includeLogicalDeleted`（TableEntry 加可选字段）；**执行期发现**：@DataBean JSON parse 严格拒绝未知属性 → `Manifest.read` 已知键过滤（root/table/snapshot 三层 sanitize）兑现前向兼容（formatVersion 保持 1）
      - Skill: `nop-testing`
- [x] （Add）`FixtureBundleImporter`（含 `FixtureBundleImportErrors` 7 具名码 + `FixtureBundleImportResult` + 同源指纹方法）：requires 校验 → 漂移校验（指纹比对 EntityModel，fail-fast/tolerant）→ base 趟（业务键对账+映射登记+**包内重复业务键拒绝**）→ payload 趟（单事务剥 PK 重映射 + to-one 引用重写 + 悬空引用断言 + nop_sys_* 拒绝 + version/delVersion 置初值 + loadOrder 装载序）→ 同实例幂等跳过 → 结果报告（导入/跳过计数 + maxNewIds）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 编译通过（含旧 manifest 向后兼容）；main scope 变更面 = 新增 3 文件（Importer/ImportErrors/ImportResult）+ TableEntry 加 2 可选字段 + Exporter 单类扩面（指纹+声明两项加法，批准范围内）+ Manifest.read 前向兼容 sanitize

### Phase 2 - 导入侧验收测试（test scope）

Status: completed
Targets: `../nop-entropy/nop-autotest/nop-autotest-core/src/test/java/io/nop/autotest/bundle/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 完成

- [x] （Proof）`testCleanReplayWithIdRemapAndFkRewrite`：行数一致 + 新 ID ∉ 源 ID + FK 重写读穿验证 + (iii) 取号无冲突 + `testDanglingRefRejected` 悬空引用具名拒绝；(ii) 不适用理由在案
      - Skill: `nop-testing`
- [x] （Proof）`testDirtyTargetBaseNotOverwritten`：对账命中登记映射零插入、原行数值不变
      - Skill: `nop-testing`
- [x] （Proof）`testRequiresMissing`：具名 ErrorCode 断言（NopException.getErrorCode() 返回 String——执行期确认）
      - Skill: `nop-testing`
- [x] （Proof）`testSchemaDriftFailFastAndTolerant`：指纹篡改 fail-fast + tolerant 告警继续
      - Skill: `nop-testing`
- [x] （Proof）`testInRunIdempotencyUkConflictAndGuards`：同实例二次导入 skip=1；**同包重复业务键拒绝**（执行期发现：逐行对账会将重复键静默合并进第一行——包缺陷显式拒绝，并发窗口 duplicate-key wrap 保留）；`includeLogicalDeleted=true` 拒绝；`nop_sys_*` 入口拒绝 + `testVersionColumnsStrippedAndManifestForwardCompat`（normalizeRow 剥 PK/剥版本列断言 + 未知 manifest 字段前向兼容）
      - Skill: `nop-testing`
- [x] （Proof）全模块跑法 21/21 全绿（导出 5 + 导入 7 + spike 2+2 + 既有 5，既有断言零改动）——证据 `_tmp/2026-10-01-2142-1-full-module.log`

Exit Criteria:

- [x] 新增测试全绿 + 全模块零回归（证据落 `_tmp/`）

### Phase 3 - owner doc 与 roadmap 回写

Status: completed
Targets: runbook「导入」节 + INDEX（已登记不重复）+ 本仓 roadmap
Skill: `none`

- Item Types: `Add`（文档）、`Proof`（收口核验）
- Prereqs: Phase 2 完成

- [x] runbook「导入」节扩写：导入 API、两趟事务边界、映射表、指纹/漂移、requires、幂等口径（含同包重复键执行期发现）、序列对齐（Decision E）
      - Skill: `none`
- [x] 本仓 roadmap：§审查记录 追加 M1.2 完成行 + 状态翻转（独立结束审计 ACCEPT 后凭授权执行 `ready`→`done`）
      - Skill: `none`

Exit Criteria:

- [x] runbook 导入节完整；roadmap 审查记录行落盘

## Decisions

### Decision E — 序列对齐口径（框架层无 nop-sys 依赖如何兑现 Decision ③ 断言口径）

- 选择：**框架层不内置序列改写**——remap 主策略下 payload 新 ID 由目标平台生成（`seq-default` 序列取号自然推进，无冲突面）；导入结果报告记录各 payload 表 `max(newId)` 供消费方断言；框架测试环境（无 nop-sys）由 `UuidSequenceGenerator` bean 覆盖，取号无冲突以 Decision ③ 口径 (iii) 经验断言（M1.3① 行使），(ii) 留具单调序列环境。
- 替代方案：动态实体直改 `nop_sys_sequence` 行（拒绝——`SysSequenceGenerator` 进程内 cache 的 removeCache 仅经 BizModel `afterEntityChange` 接线，ORM 层直写缓存不失效，制造更隐蔽的不一致）；框架内置对齐 API（拒绝——需 nop-sys 依赖或 BizModel 反射，超写入面且属消费方环境职责）。
- 残留风险：非单调序列（uuid/snowflake）无冲突为概率性（(iii) 经验口径）；`cacheSize` 批量取号在途时导入 ID 可能落在已缓存区间外——消费方断言义务，runbook 记录。

### Decision F — 引用完整性断言范围（直插不触发 validateRefValue 的导入侧兜底）

- 选择：**导入前置全量校验（fail-fast）**——payload 趟写库前，每行每个 to-one join FK 列值须满足「在包内 oldId→newId 映射集中 ∨ 目标库已存在（`orm.get` 非空）」，否则具名 ErrorCode 拒绝整趟 payload（M0.1 Decision ⑤ 残留义务的直接兑现：悬空引用直插静默成功，导入器为唯一兜底）。范围 = 模型声明的 to-one join 列；不扫描软引用（`relatedBillType+Code` 语义链、操作人列等非 FK 引用属 roadmap §Non-Goals 查询式闭包触发条件门控能力）。
- 替代方案：导入后校验（拒绝——违反原子性，悬空行已落库）；仅校验包内映射不查目标库（拒绝——跨 bundle requires 场景 FK 指向既有 base 行是合法形态）。
- 残留风险：软引用悬空不拦截（by design，触发条件门控）；`orm.get` 逐值查询在大包场景 O(n)——测试/演示环境规模可接受，性能留 M2.3 实测基线观察。

### Decision G — 映射表与幂等标识形态

- 选择：**内存映射表 + 同运行幂等**——`Map<entityName, Map<oldId,newId>>` 导入运行内有效；幂等作用域如实收窄（批准条件 B2/A3）：(a) base 趟业务键对账天然幂等（已存在即跳过登记）；(b) payload 幂等 = 同一 Importer 实例运行内登记已导入 (table, 源 ID)，重复调用跳过；**跨进程幂等不在本计划写入面内**（无足迹方案），runbook 明文消费纪律 = bundle 一次性导入 + 测试 fresh 库语义。复合 PK 表：**base 与 payload 两趟同拒**（`composite-pk-payload` 具名码）——结束审计 MAJOR-1 裁决记录：初稿「复合 PK base 表不受影响」条款与实现相反且未记录，实况为两趟同拒；该行为与 roadmap M1.2 一致（base 缺失行同样「剥 PK 由平台生成」，复合主键无平台生成语义，base 插入路径同样无法落地），故采纳**记录裁决路径**：维持两趟同拒实现，语义按 roadmap 归一；重开触发条件 = 出现复合 PK 表 bundle 的真实消费需求（结构变更审查，含 base 保 PK 对账模式的专项设计）。
- 替代方案：目标库持久化映射/标记构件（拒绝——批准条件 A3 边界）；bundle 目录内持久映射文件（拒绝——污染只读包语义 + A3 精神）。
- 残留风险：跨进程重复导入 payload 产生重复行——消费纪律兜底；复合 PK payload 场景被拒（真实需求出现时走结构变更审查）。

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_f5eda6fe, fresh session) — 4 MAJOR（系统表/序列表不随包导入零承载；version/delVersion 置初值零承载且逻辑删除声明语义悬空；columnFingerprint/声明字段生产者缺位[死代码风险]且「导出侧已具备承载点」与活码不符；测试矩阵缺悬空引用/UK 冲突/逻辑删除声明 Proof）+ 10 MINOR（loadOrder 装载/base 趟事务边界/Decision C→E 笔误/ImportErrors 落点/formatVersion 口径/UuidSequenceGenerator 输入/(ii) 弃用理由/erp_fin_* 触发对象错位/横切 7 登记槽/11 字段措辞等）；6 条基线抽查全命中。上列全部修订已落实（扩面至导出器为 Approver 审查点）。
- Independent draft review iteration 2: acceptable as-is (agent_f5eda6fe 增量复审) — 4 MAJOR 实质关闭、10 MINOR 全落实；R-1（重复 Skill 行）/R-2（两处概括行同步）/R-3（MINOR 计数笔误）已随转 active 落实；实施期斟酌点登记（导出侧对含已删行包是否前置拒绝→Decision 回填裁量；系统表命中判定时点在导入器入口）。计划转为可执行契约，dual-agent-approval 先于 Phase 1。

## Dual-Agent Approval Record（保护区：nop-entropy 全部写入 main/test/docs）

- Approver A: **APPROVED with conditions**（agent_d361e836，fresh session，风险与边界视角，2026-10-01）。实测：Exporter 扩面确认最小加法（entry-setter 两项写入、export() 签名不变、masking/merge/sha256/topo 零改动）；TableEntry javadoc 授权链自洽；本仓保护区零触碰。条件（执行者全部接受）：①**显式路径 staging**（M1.1 条件①沿例，工作树无关脏文件不卷入）；②**写入面冻结**（新文件：导入器/`FixtureBundleImportErrors`/测试/runbook 导入节；Manifest/TableEntry 加可选字段；Exporter 单类扩面限于指纹+声明两项加法；新常量进新文件，不改 Constants/Errors/Validator；零新增依赖；**不为导入器添加任何自动装配入口**——仅可被显式代码调用，超面重走 dual-agent-approval）；③**Decision G 持久化边界**——映射表/幂等标识不得在目标环境新增持久化构件或 bundle 目录外持久文件，无法面内兑现即停下重走批准，不得临时表变通；④零回归硬门控（全模块跑法复验）+ 既有 5 导出测试断言只增不改 + 非阻塞建议：顺带断言 JsonTool typed parse 对未知 manifest 字段容错。
- Approver B: **APPROVED with conditions**（agent_216c9c2c，fresh session，技术方案正确性与 roadmap 忠实度视角，2026-10-01）。核验：to-one 引用重写路径源码级证实（`IEntityRelationModel.getSingleJoinColumn` 解析为 owner 普通 FK 列，导出 CSV 列集 round-trip 自洽）；剥 PK 生成成立（`OrmSessionImpl:508/:921/:966` → `OrmEntityIdGenerator`，deleteVersion 缺省自动置 0 :56-58 与导入器强制置初值同向）；roadmap M1.2 十三项交付面逐项承载核对通过；E 三输入源码级属实。条件：B1 Phase 2 补 `nop_sys_*` 拒绝与 version/delVersion 置 0 两处显式断言承载（可沿场景⑤一并承载）；B2 Decision G 钉死幂等作用域（跨进程幂等在声明写入面内无足迹方案——面内路径 = base 业务键对账天然幂等 + 同运行映射跳过 + 断言表述如实收窄）；B3 指纹规范化两侧同源钉死（同一工具方法，导出取 getColumns() 列名集、导入取 CSV 表头列集）；B4 提交纪律沿 A①。附注：复合 PK payload 表行为随 Decision G 一并声明；(ii) 断言 M1.3 须自带单调 ISequenceGenerator 测试 bean（接口在 nop-dao 面内可得）；requires「装载失败」随实现形态自然覆盖。

## Closure Gates

- [x] 范围内行为完成（导入器[含系统表拒绝/版本置初值/逻辑删除声明拒绝] + manifest 扩展与导出器扩面[指纹+声明生产者] + 导入侧验收测试 + runbook 导入节 + roadmap 回写）
- [x] 相关文档对齐（runbook 导入节 + roadmap 审查记录/状态翻转）
- [x] 已运行验证（`./mvnw test -pl nop-autotest/nop-autotest-core -am` 全绿含全模块零回归；本仓 compliance checker 零新增漂移；plan-gates --strict PASS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项；M1.3 验收用例正式落位、批量入口、skill、消费示例分属后续工作项。

### version/delVersion 显式置 0 分支的富模型测试覆盖（结束审计 MINOR-6 登记）

- Classification: `watch-only residual`
- spike 模型无版本列，测试断言走 strip 分支（非模型列剥离）；「模型自有版本列强制置 0」分支（2 行）无测试承载，正确性由平台缺省同向佐证（`OrmEntityIdGenerator` deleteVersion 缺省置 0）+ 代码指认（`normalizeRow`）。
- Why Not Blocking Closure: 行为与平台缺省同向且代码路径单一；新增富测试模型超出本批准写入面（test 资源 app.orm.xml 为既有文件）。
- Successor Required: `yes`——M1.3①（干净库回放正式落位）若引入富模型则顺带补断言；否则在首个真实消费工作项补。

## Closure

Status Note: 分层导入器全部交付面在码且经两轮独立审计收敛——roadmap M1.2 十一项交付面逐项 file:line 指认（审计方独立核验）；Decisions E/F/G 落地（框架不内置序列对齐+maxNewIds/前置悬空校验/内存映射同实例幂等+零持久化构件）；执行期发现三项（同包重复业务键静默合并→显式拒绝/getErrorCode 返 String/@DataBean 严格 parse）全部转化为代码与测试；21/21 全绿 + 门禁全绿。round1 MAJOR-1 经「记录裁决」路径关闭（记录—活码—owner doc 三面一致+具名重开触发）。残留 3 nit 非阻塞已登记（ImportErrors 文案 payload 措辞/证据日志汇总行/trivial 2 项）。

Closure Audit Evidence:

- Auditor / Agent: independent subagent agent_49256a30（fresh session，同会话两轮）——round1 **NEEDS REVISION**（MAJOR-1 Decision G 复合 PK 条款与活码相反零记录 + MINOR-2/3/4/6）→ 整改（Decision G 记录裁决路径/runbook 更正/parseNonStrict 修复/roadmap 翻转回退/Deferred 登记）→ round2 **ACCEPT**（0 阻塞；五项整改逐项核验关闭：MAJOR-1 记录—活码—owner doc 三面一致、MINOR-4 五处状态核对、独立复跑 21/21 exit 0 + plan-gates PASS；残留非阻塞 3 nit 随关闭登记）。
- Evidence: 两轮审计报告在案；审计方独立复跑两轮（21/21，exit 0）；git diff 9b0a704f79 写入面核实恰为批准清单（4 修改 + 4 新增 + runbook）；证据日志 `_tmp/2026-10-01-2142-1-*.log`。

横切关注点 7（`AutoTestCaseDataSaver.removeInputTable` 缺 variant 保护的既有缺陷）：**不适用**——本计划零触碰该路径，按 roadmap 规则在此登记一次。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
