# 2026-10-01-2049-1 M1.1 fixture-bundle 包格式 + manifest + 观测式导出器

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M1.1（包格式 + manifest schema + 观测式全行导出器，导出侧闭环）
> Related: `docs/plans/2026-10-01-1853-1-m01-fixture-bundle-spike-six-decisions.md`（6 条前置 Decision 全 done）；后续 M1.2/M1.3/M2.1/M2.2 依赖本计划的包格式与导出 API
> Audit: required

## Current Baseline

- **M0.1 六 Decision 已定**（`2026-10-01-1853-1` `## Decisions` 节，结束审计 ACCEPT）：录制会话可行（bootstrap 复刻路径 + nop-ioc test-scope 依赖已在 pom）；JVM 独占 + try/finally 注销；ID 策略 remap（口径三件套已定，属 M1.2 消费）；导出选型 **ORM + CsvHelper 自写**（零新增依赖边）；直插不触发 `validateRefValue`（M1.2 义务）；bundle 载体 = 消费方 test-scope VFS（`_vfs/<module-short>/fixture-bundle/<bundle-name>/` 建议布局，本计划定稿）。
- **原型资产**：nop-autotest-core test scope 7 文件（`M01SpikeRecordingSession` 等，plan 原型处置节登记）作为参考实现起点；pom 已含 test-scope `nop-ioc`。
- **平台事实**（roadmap §当前基线）：`AutoTestOrmHook` 容器级注册、收集粒度 onSave→`changedData`（onLoad→`initData`）→ 全行导出须按收集 ID 重载；`CsvHelper.readCsv/writeCsv` 在 nop-core；`EntityRow` 双容器；`_cases` 布局与 CHECKING 模式零改动（真增量守则）。
- 剩余差距：无包格式、无 manifest schema、无独立导出入口；owner doc `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md` 不存在；`app-erp-test-data/load-order.txt` 与 manifest 序源关系未裁。

## Goals

- **包格式定稿**：bundle 目录布局（manifest + base/ + snapshots/）与 manifest schema 字段级定义（序列化形态按 Goals 约束为 JSON5；字段集见 Decision A——roadmap M1.1 强制字段清单为不可缩水输入）。
- **观测式全行导出器**：独立录制会话（沿原型）+ 按收集 ID 重载全行 + 按表分层落 base/payload CSV + masked 列占位 + manifest 写出；会话独占按 M0.1 ② 残留口径升级为 tryLock + 语义化 ErrorCode。
- **导出侧校验器**：manifest 自校验（字段完整性/行数/校验和/captureGaps 对账/敏感列扫描；校验和用途限定为「检测 CSV 手改/截断」，非防篡改安全保证）。
- **导出侧验收测试**（nop-autotest-core test scope）：构造→导出→独立校验全链 + masked 纪律 + captureGaps（会话内删除行）+ 脱离 `_cases` 布局断言。
- **owner doc 新建**：`../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md` 导出侧章节（manifest schema、导出 API、bundle 布局；导入章节留 M1.2 扩写）+ INDEX.md 路由登记。

## Non-Goals

- 不实现导入器（M1.2）、验收用例三件套（M1.3）、批量/CLI 入口（M2.1）、skill（M2.2）、本仓消费示例（M2.3）。
- 不改动既有 `_cases/` 布局、CHECKING 模式与 `AutoTestCase` 公开行为（真增量守则：M1.1 产物全部走新包路径）。
- 不触碰本仓 `module-*/model/*.orm.xml` 与 `_init-data/**`。
- 不提供构造 DSL/构造器（构造由消费方编排 `@BizMutation`/GraphQL，M2.2 承载）；导出 API 接受任意构造 body。
- 不做全表全量导出（范围口径：构造会话触及行的完整行内容）。
- 不迁移既有 30k+ `_cases` CSV。

## Task Route

- Type: `architecture change`（nop-entropy 平台层新增框架能力）+ `implementation-only change`（本仓零代码）
- Owner Docs: `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md`（新建）+ `docs-for-ai/INDEX.md`（路由行）
- Skill Selection Basis: `Skill: nop-testing`（工具原生技能，实存于 `.opencode/skills/nop-testing/SKILL.md`——M0.1 已确认来源口径；其测试基础设施路由适用于框架实现与测试设计）。保护区：nop-entropy **全部写入（main 代码 / test 代码 / docs-for-ai 文档）= `auto + dual-agent-approval`**（与 roadmap M1.1 Guard 列对齐，不收窄）——草案审查通过后、实施开始前两名独立子代理批准，记录落盘本计划。

## Infrastructure And Config Prereqs

- 跨仓验证命令：`cd ../nop-entropy && ./mvnw test -pl nop-autotest/nop-autotest-core -am`（含 `-am` 上游构建；长输出落 `_tmp/2026-10-01-2049-1-*.log`）。
- nop-autotest-core 既有依赖（nop-xlang/nop-match/nop-orm/nop-ioc[test]/h2/junit-jupiter）已覆盖导出器所需（CsvHelper 在 nop-core 经 nop-xlang 传递）；**不新增任何依赖**。
- 回滚策略：全部产物为新文件（新包 + 新测试 + 新 runbook），revert 单提交即可；零既有文件语义修改（pom 增量行随原型处置裁决一并处理，见 Decision D）。

## Execution Plan

### Phase 1 - 包格式与导出器实现（nop-autotest-core main scope）

Status: completed
Targets: `../nop-entropy/nop-autotest/nop-autotest-core/src/main/java/io/nop/autotest/bundle/`（新包）
Skill: `nop-testing`

- Item Types: `Add`（框架实现）、`Decision`（4 条，见 Decision 节）
- Prereqs: dual-agent-approval 双批准；M0.1 六 Decision（done）

- [x] （Decision | Add）Decision A/B/C 定稿并落 `## Decisions` 节（manifest schema 字段集与序列化形态 / bundle 目录布局与快照契约 / load-order.txt 关系）——连同 Decision D 已回填
      - Skill: `nop-testing`
- [x] （Add）`FixtureBundleManifest` + `FixtureBundleTableEntry` + `FixtureBundleSnapshotEntry`（@DataBean POJO，JSON 写 manifest.json5，typed parse 读回）
      - Skill: `nop-testing`
- [x] （Add）`FixtureBundleRecordingSession`（tryLock + `nop.err.fixture-bundle.export-session-busy`）+ `FixtureBundleExporter`（fresh-session 全行重载 → 分层 CSV → masked 占位 → SHA-256 → manifest 合并写出）
      - Skill: `nop-testing`
- [x] （Add）`FixtureBundleValidator` + `FixtureBundleSensitiveRules`（只读磁盘态四轴校验；敏感规则可扩展[批准条件 B-C2]，内建 PASSWORD/PASSWD/PWD/SALT 形列默认规则）
      - Skill: `nop-testing`
- [x] （Decision | Add）Decision D：原型 7 文件 + pom test-scope `nop-ioc` 行随 M1.1 提交入库（裁决已回填；与 M0.1 批准「留作 M1.1 使能项」分支合流）
      - Skill: `none`

Exit Criteria:

- [x] 新包编译通过（`./mvnw test-compile -pl nop-autotest/nop-autotest-core -am` 绿）；main scope 仅新增 `io/nop/autotest/bundle/` 10 文件，既有平台文件零修改（pom 行按 Decision D 入库）

### Phase 2 - 导出侧验收测试（test scope）

Status: completed
Targets: `../nop-entropy/nop-autotest/nop-autotest-core/src/test/java/io/nop/autotest/bundle/`（新测试包）；`src/test/resources/_vfs/`（沿 spike 模型）
Skill: `nop-testing`

- Item Types: `Proof`（验收测试）、`Add`（测试资源）
- Prereqs: Phase 1 完成

- [x] （Proof）全链测试 `testFullChainExportValidate`：base+payload+to-one 构造 → 导出 → 独立校验通过 → manifest 字段/行数/目录布局断言 + 脱离 `_cases` 断言
      - Skill: `nop-testing`
- [x] （Proof）masked 纪律测试 `testMaskedColumnReplacedAndUnmaskedSensitiveRejected`：masked 列写 `MASKED-BUNDLE-SEED` 占位且原值不出现；敏感规则 PASSWORD/SALT 未标记即拦截
      - Skill: `nop-testing`
- [x] （Proof）captureGaps 测试 `testCaptureGapsForSessionDeletedRow`：已持久化行在录制窗口内删除 → 不出 CSV、gaps=1、导出+gaps=收集（执行期发现：同会话 save+delete 抵消不入库不产生 gap——测试语义已按真实平台行为修正）
      - Skill: `nop-testing`
- [x] （Proof）多快照契约测试 `testMultiSnapshotContract`：同 bundle 两命名快照 manifest 合并、目录并存、独立校验通过
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试全绿（`TestFixtureBundleExport` 5/5）；**全模块跑法（批准条件 A③）零回归**：nop-autotest-core 14/14 全绿（新 5 + spike 2+2 + 既有 TestTestClockAnchoredSim 5）——证据 `_tmp/2026-10-01-2049-1-full-module.log`、`_tmp/2026-10-01-2049-1-test-final.log`

### Phase 3 - owner doc 与 roadmap 回写

Status: in progress
Targets: `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md`（新建）、`../nop-entropy/docs-for-ai/INDEX.md`（路由行）、本仓 roadmap
Skill: `none`

- Item Types: `Add`（文档）、`Proof`（收口核验）
- Prereqs: Phase 2 完成

- [x] runbook 新建：bundle 布局 + manifest schema + 导出 API + masked 纪律 + 校验和用途限定（检测 CSV 手改/截断，非防篡改安全保证）+ 可见性矩阵（Decision ⑥）+ 与 `_cases` 的关系（旁路新增）；导入节留 M1.2 占位
      - Skill: `none`
- [x] INDEX.md 登记路由行（runbooks 分区；roadmap 将 INDEX 登记列于 M1.3 定稿项下，本计划提前完成后 M1.3 对应行转为复核——此处登记避免 M1.3 起草时误判遗漏）
      - Skill: `none`
- [x] 本仓 roadmap：§审查记录 追加 M1.1 完成行 + 状态翻转（独立结束审计 ACCEPT 后凭授权执行 `ready`→`done`）
      - Skill: `none`

Exit Criteria:

- [x] runbook 存在且含导出侧全部契约；INDEX 路由可达；roadmap 审查记录行落盘

## Decisions

> Decision A 的输入约束（roadmap M1.1 强制字段清单，**不可缩水**，字段级增删在回填时逐项裁决）：版本、requires、表清单、层归属、业务键声明、加载序、行数、来源标记、校验和、列级 masked 敏感标记、captureGaps（导出侧验收要求）。其中「业务键声明」是 M1.2 base 对账的前置契约，「来源标记」承载行的采集来源区分。

### Decision A — manifest schema 字段集与序列化形态

- 选择：manifest 顶层 = `formatVersion`(=1) / `bundleName` / `requires`(bundle 名数组，M1.2 导入时校验) / `baseTables`(表条目数组) / `snapshots`(命名快照数组)；表条目 = `table`/`layer`/`businessKeys`(base 必填)/`loadOrder`(拓扑序号)/`rowCount`/`csv`(相对路径)/`sha256`/`maskedColumns`/`source`(=observed-session)/`captureGaps`——roadmap 强制清单 11 项全部承载。序列化 = JSON（JSON5 合法子集）写 `manifest.json5`；POJO 标注平台 `@DataBean`；读回 `JsonTool.parseBeanFromResource(resource, Manifest.class)` typed parse。敏感列规则集 `FixtureBundleSensitiveRules` 可扩展（内建 PASSWORD/PASSWD/PWD/SALT 形列默认规则，消费者可 addRule）；manifest 前向扩展注记：新增字段须格式版本受控，M1.2 列指纹由 M1.2 自身双批准扩展。
- 替代方案：YAML（拒绝——平台 JSON 工具链原生，@DataBean 契约现成）；XML（拒绝——冗长且无 bean 映射收益）；字段缩水（拒绝——11 项强制清单为绑定输入）。
- 残留风险：JSON 非严格 JSON5 全集（不支持注释/尾逗号）——schema 由 @DataBean 类型承载不受影响；requires 的语义 enforcement 属 M1.2。

### Decision B — bundle 目录布局与快照契约

- 选择：`<bundle-dir>/manifest.json5 + base/<table>.csv + snapshots/<name>/<table>.csv`（载体位置沿 M0.1 Decision ⑥ = 消费方 test-scope classpath；导出器只写目标目录，VFS 发现规则归消费方布局约定）。**术语桥**：`snapshots/<name>/` 即 roadmap「payload 层」的目录实现——manifest `snapshots[].tables[].layer` 恒为 "payload"，M1.2 契约术语无损承接。多快照 = 同 bundle 目录多次 export（manifest 合并 base 表 + 追加/覆盖同名快照节）。**重载语义（批准条件 B-C2）**：全行重载在 hook 窗口关闭后的 fresh ORM session 执行（`reloadFullRow` 每次 `runInSession`），杜绝会话实体缓存命中导致 DB 默认值列缺位。
- 替代方案：payload 文件平铺 bundle 根（拒绝——多快照同名表冲突）；单 manifest 内联数据（拒绝——CSV 与校验和绑定文件粒度）。
- 残留风险：同名快照重复 export 覆盖（幂等语义，已在 manifest 合并中明确）；base 表跨快照共享（by design，requires 依赖而非复制）。

### Decision C — manifest 加载序与 `app-erp-test-data/load-order.txt` 的关系

- 选择：**manifest 为唯一序源**——每表条目 `loadOrder` 字段 = bundle 表子集经 `IOrmModel.sortEntityModelInTopoOrder(Collection)` 的拓扑序号（bundle 内单调递增）；采纳 roadmap 建议：`app-erp-test-data/load-order.txt` 标 deprecated，退役登记归 M2.3 owner-doc 扩列时执行（本计划不触碰该文件）。
- 替代方案：沿用 load-order.txt（拒绝——零 loader 消费的空骨架，脱离 bundle 目录语义）；导入时动态重算拓扑序（拒绝——manifest 静态可审校，导入器应校验而非重推导）。
- 残留风险：手工改 manifest 表顺序不被校验器拦截（loadOrder 单调性未检——后续工作项可加；不阻塞：导入器按 loadOrder 排序自带兜底）。

### Decision D — M0.1 原型资产入库形态

- 选择：**随 M1.1 提交入库**——原型 7 文件（5 Java + 2 测试资源，test scope）+ pom 单条 test-scope `nop-ioc` 依赖行，作为 M1.1 参考实现与使能依赖一次性入库（消除 untracked 可失性与 Phase 2 零回归断言的不可复现性；与 M0.1 批准记录「留作 M1.1 使能项」分支合流，非新增量批准需求——Approver A 实测确认自洽）。
- 替代方案：保持 untracked + 登记废弃时点（拒绝——fresh clone/CI 不可复现 Phase 2 判据）。
- 残留风险：pom 行永久改变 nop-autotest-core 测试 classpath（IoC 可达）——Approver A 条件③以全模块测试跑法复验零回归（已执行：14/14 绿）。

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_7ab87845, fresh session) — 2 MAJOR（B-1/D-1：Decision A 缺 roadmap 强制 manifest 字段清单输入；C-1：M0.1 移交的原型 7 文件+pom 行入库形态未裁决）+ 8 MINOR（Deferred 节/JSON5 约束对齐/-am/校验和用途限定/payload↔snapshots 术语桥/tryLock+ErrorCode 承接/Guard 口径放宽/INDEX 提前登记注记）；8 条基线抽查全命中活码。上列全部修订已落实（Decision A 输入约束块 + Decision D 新增 + 九处对齐）。
- Independent draft review iteration 2: acceptable as-is (agent_7ab87845 增量复审) — 两 MAJOR 实质关闭、10 处修订全落实；R-1（Item Types 计数 3→4）与 R-2（Approval Record 节标题随 Guard 口径同步）两处单行修正已随转 active 落实，不构成重审条件。计划转为可执行契约，dual-agent-approval 先于 Phase 1。

## Dual-Agent Approval Record（保护区：nop-entropy 全部写入 main/test/docs）

- Approver A: **APPROVED with conditions**（agent_997d5bd0，fresh session，风险与边界视角，2026-10-01）。实测证据：nop-autotest 全树仅 pom +8 行修改、共享类零触碰、原型 7 文件 untracked test-scope、runbook/INDEX 路由均不存在（纯新建）；Decision D 与 M0.1 既有批准自洽（「留作 M1.1 使能项」分支）、test scope 不传递零下游影响。5 项条件（执行者全部接受）：①**显式路径 staging**——nop-entropy 提交逐路径 git add，禁 `git add -A`/`git add .`，工作树既有无关脏文件（nop-code-web/nop-metadata/nop-jq 等）不得卷入；②**写入面冻结**——仅新包 io.nop.autotest.bundle（main+test）+ 新测试资源 + runbook 新建 + INDEX 单行路由 + pom 单条 test-scope 依赖 + 原型 7 文件入库；Decision A-D 回填不得扩面：不新增依赖、不改既有平台文件、**不为新包添加自动装配入口**（IoC beans.xml/ServiceLoader/静态 hook 注册），超面重走 dual-agent-approval；③**零回归硬门控**——Phase 2 判据为硬门控，closure 时同轮以**全模块测试跑法（不限 -Dtest 过滤）**复验一次 classpath 变化无意外回归；④**敏感数据门控移交留痕**——closure 时登记「repo 级 bundle 入库门控未随 M1.1 落地，移交首个真实 bundle 入库的 plan」，本计划校验器扫描仅为代码级纪律；⑤不预设 M1.2/M1.3，各自重走批准。
- Approver B: **APPROVED with conditions**（agent_de6bdf90，fresh session，技术方案正确性与 roadmap 忠实度视角，2026-10-01）。核验：复合主键重载链 `OrmEntityHelper.castId:178-179`（String id→`OrmCompositePk.parse`）源码级证实可逆；to-one/大字段/删除行/masked→校验和顺序无缺口；A/B/C/D 四 Decision 裁决空间与 roadmap M1.1 及 M0.1 Decision ⑥/原型处置移交逐一一致；校验器四轴（完整性/真实性/可对账/敏感纪律）足以支撑 M1.3① 前置；导入侧零实现确认。条件（不阻塞开工）：C1 提交范围纪律（同 A①）；C2 Decision B 回填**固定重载 session 语义**（fresh session/清缓存读 DB，避免会话实体缓存命中致 DB 默认值列缺位）、Decision A 回填敏感列规则集**可扩展**（内建 `nop_auth_user` PASSWORD/SALT 形列规则为默认）+ manifest 字段前向扩展注记（M1.2 列指纹由 M1.2 自己的双批准扩展 schema）。附注（落入 Phase 3 runbook）：no-PK 实体（`isNoPrimaryKey`）在 hook `orm_idString()` 键控下不可收集（既有 AutoTestOrmHook 共有约束）；JDBC 直操不经 hook 不可见（与 M2.2 构造必走 @BizMutation/GraphQL 互为支撑）；校验和用途限定措辞保持；checker 零漂移维持 Closure Gate。

## Closure Gates

- [x] 范围内行为完成（包格式 + 导出器 + 校验器 + 导出侧验收测试 + runbook/INDEX/roadmap 回写 + Decision D 原型入库）
- [x] 相关文档对齐（runbook 新建 + INDEX 路由 + roadmap 审查记录/状态翻转）
- [x] 已运行验证（`./mvnw test -pl nop-autotest/nop-autotest-core -am` 全绿含既有测试零回归；本仓 compliance checker 零新增漂移；plan-gates --strict PASS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项；导入侧验收、批量入口、skill、消费示例分属 M1.2/M1.3/M2.x 工作项，不属本计划 deferred。

### repo 级敏感数据门控移交（批准条件 A④ 留痕）

- Classification: `successor ownership`
- 本计划交付的 `FixtureBundleSensitiveRules`/`FixtureBundleValidator` 为**代码级纪律**；「防止真实 bundle（含真实业务/凭据数据）入库 git」的 repo 级门控（compliance checker 站点扩展或门禁测试常量表）未随 M1.1 落地。
- Successor Required: `yes`——移交**首个携带真实数据 bundle 入库的 plan**（预计 M2.3 消费落地，或更早的具名消费请求）；届时按 roadmap 横切关注点 5 扩展门禁并登记。

## Closure

Status Note: 导出侧闭环全部落地——包格式（manifest 11 强制字段 @DataBean JSON）/观测式全行导出器（tryLock 独占 + fresh-session 重载 + 分层 CSV + masked 占位 + SHA-256）/独立校验器（只读磁盘态四轴 + 可扩展敏感规则）/验收测试 5/5 + 全模块 14/14 零回归；runbook + INDEX 路由落地；Decisions A-D 回填、双批准条件全履行；Phase 3 的 nop-entropy 侧产物（新包/测试/runbook/INDEX/pom/原型 7 文件）按审计关闭义务以显式路径 staging 单独提交入库。审计 5 MINOR 均非阻塞（其中提交态收敛义务已随本提交履行、状态断言经 ACCEPT 追溯为真）。

Closure Audit Evidence:

- Auditor / Agent: independent subagent agent_e0b4ef03（fresh session，2026-10-01）
- Evidence: 审计 **ACCEPT**（0 BLOCKER / 0 MAJOR / 5 MINOR 非阻塞：①证据日志缺 Maven 汇总行[surefire 报告独立佐证]②状态断言早于审计[ACCEPT 后追溯为真]③Decision D 提交态收敛义务[本提交履行]④第 5 测试未列计划项[加向增量]⑤Item Types Add 标注与零资源实际+未用错误码常量）；审计方独立复跑 `node tools/check-plan-gates.mjs --strict` PASS（0 新增违规）；实测 14/14 surefire 报告 + 产物目录 target/fixture-bundle-test/export/{full-chain,gaps,masked,multi}；nop-entropy 零提交核对（执行期间）+ 无关脏文件未卷入。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
