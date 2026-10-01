# 2026-10-01-2255-1 M1.3 fixture-bundle 三条验收用例 + runbook 定稿

> Plan Status: active
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M1.3（三条验收用例及其落位 + 平台文档定稿）
> Related: `2026-10-01-1853-1`（Decision ③ 断言口径三件套）；`2026-10-01-2049-1`（M1.1 导出侧）；`2026-10-01-2142-1`（M1.2 导入器，两轮审计 ACCEPT）
> Audit: required

## Current Baseline

- **M1.1/M1.2 已 done**：`io.nop.autotest.bundle` 导出器+导入器+校验器 17 文件入库（nop-entropy `9b0a704f79`+`bb2edc774c`）；runbook `fixture-bundle.md` 导出+导入节已成稿（M1.3 定稿=复核非重写）；INDEX 路由已登记（M1.1 提前完成，本计划转为复核项；路由行描述「M1.1 导出侧」已陈旧——定稿时刷新为覆盖导出/导入/验收，Approver B 附注 1）。**基线订正（批准者 B BLOCKER 实证）**：`m01-fixture-spike.beans.xml` 自 M0.1 起为静默 no-op（文件名不符 IoC 装载扫描规则 `app*.beans.xml`），模块全绿实际由 orm-defaults 的 UuidSequenceGenerator 兜底——spike/bundle 测试的序列行为基线 = 随机长整型。
- **M1.2 验收测试与 M1.3 三用例的关系**（M1.2 计划 Non-Goals 预留的落位裁决点）：`TestFixtureBundleImport`（nop-autotest-core，7 测试）已覆盖干净回放/脏环境对账/requires 缺失的开发验证形态；M1.3 按 roadmap 落位要求收敛为正式验收：
  - ①（干净库回放）落位 = nop-autotest-core——**晋升既有 `testCleanReplayWithIdRemapAndFkRewrite` 为验收①载体并增补**：补齐 M0.1 Decision ③ 口径 (ii)。机制 v3（双批准者 B REJECT 后再修正，机制沿革：v1 运行时换装被快照机制证伪 → v2 bean 覆盖被装载链路证伪 → v3）：`m01-fixture-spike.beans.xml` **从未被 IoC 装载**（`AppBeanContainerLoader.isAppBeans` 仅认 `app.beans.xml`/`app-*.beans.xml` 文件名；无 autoconfig/testBeansFile/merged 引用；nop-datav「先例」实为 `@NopTestConfig(testBeansFile=...)` 显式机制）——自 M0.1 起为静默 no-op，模块全绿实际由 `orm-defaults.beans.xml:34` 的 UuidSequenceGenerator（2025-11-05 引入）兜底解释。v3 = **启动期显式装载**（方向正确、先例充分；v3 实施细节经 B 第二轮复审三处活码缺陷否决后升级 **v4**）：① `M01SpikeBoot.start()` 在 `CoreInitialization.initialize()` 前设置 test config 键 **`nop.ioc.app-beans.files`**（点分形态——权威定义 `IocConfigs.java:62`，全链精确匹配无归一化；v3 误写连字符形态）= `/nop/spike/beans/m01-fixture-spike.beans.xml`（沿 `IocSpikeConfigs` 镜像模式）；② beans 文件根元素补 **`xmlns:ioc="ioc"`**（从未被解析故从未暴露）；③ bean 行补 **`ioc:allow-override="true"`**（orm-defaults 经 `/nop/autoconfig/nop-orm.beans` 先注册，同 id 再定义缺省抛 duplicate-bean-definition——`BeansDefinition.java:91-95`、缺省 false 于 `_BeanModel.java:38`；先例 nop-task-ext test-reliability.beans.xml；`ioc:default` 的豁免逻辑为死代码）+ 类名替换为单调计数器 + 文件头注释订正 + **正向判别断言**（容器解析的 `nopSequenceGenerator` 实例类型——v3 的键名错误正是该断言会当场抓红的形态，印证其必要性）。(ii) 断言取**计数器等价形式**（环境无 nop_sys_sequence/NEXT_VALUE 可读——M1.2 Decision E；等价性前提 = 测试自建单调环境，M0.1 ③ 原文为条件式）。
  - ②（脏环境 base 不覆盖）落位 = 本仓 `app-erp-all` `ErpIntegrationTestCase`（文件 H2 + 全量 seed 天然脏环境，M1.2 审计确认其验收②前置）——**新增消费者侧验收测试**：base = `ErpMdCurrency`（业务键 CODE，seed 含 CNY/USD），payload = `ErpMdExchangeRate`（`FROM_CURRENCY_ID`→构造币种[映射重写]，`TO_CURRENCY_ID`→seed CNY id=1[映射外目标库存在分支]）；录制会话构造新币种+汇率行 → **不删除**（脏环境语义）→ 导入 → 断言 base 对账 skip、seed 行业务键值不变、payload 按 manifest 行数落地。bundle 仅落 `target/`（运行时产物不入库——repo 级敏感门控移交义务不触发，M1.1 Deferred 登记的 successor 仍为首个真实 bundle 入库 plan）。
  - ③（requires 缺失具名 ErrorCode）落位 = nop-autotest-core——**晋升既有 `testRequiresMissing`**（M1.2 已实现具名码断言形态，roadmap 要求形态一致）。
- 晋升而非新增的口径：既有测试为开发验证命名，晋升 = 保留测试本体 + 补齐验收缺口（①的 (ii)）+ 在测试类 javadoc 登记验收用例编号（acceptance case ①/③），不改断言语义。
- 剩余差距：①缺 (ii) 断言；②消费者侧验收不存在；runbook 定稿复核未做。

## Goals

- **验收①（干净库回放）**：nop-autotest-core——晋升（javadoc 登记验收编号）+ **新增独立测试方法**承载 (ii) 单调断言（既有断言行零改动——assertion add-only；(i)(ii)(iii) 三件套全齐）。
- **验收②（脏环境 base 不覆盖）**：本仓 app-erp-all 新增 `TestErpFixtureBundleDirtyImport`（extends `ErpIntegrationTestCase`），按上述设计全链断言。
- **验收③（requires 缺失）**：晋升既有测试并登记验收编号。
- **平台 runbook 定稿**：复核 manifest schema 与导出/导入 API 形状文档与实现一致（M1.2 后 字段/语义如 `includeLogicalDeleted`、`columnFingerprint` 已在）+ INDEX 路由复核 + 定稿标记。
- **roadmap 回写**：审查记录行 + 状态翻转（结束审计 ACCEPT 后）。

## Non-Goals

- 不实现批量/CLI 入口（M2.1）、skill（M2.2）、≥2 测试类共享消费示例与 owner-doc 扩列（M2.3）。
- 不新增框架 main 代码（本计划纯测试晋升/新增 + 文档定稿）。
- 不触碰 `_cases`/CHECKING、`module-*/model/*.orm.xml`、`_init-data/**`（**零 seed 联动**：测试 bundle 为运行时产物落 target/，零 `_init-data` 写入）。
- `erp_fin_*` plan-first 叠加条件不触发（验收②用 master-data 币种/汇率实体，包内容不含财务表）。

## Task Route

- Type: `verification or audit work`（验收用例落位 + 文档定稿）
- Owner Docs: `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md`（定稿复核）
- Skill Selection Basis: `Skill: nop-testing`（工具原生技能，M0.1 确认口径）。保护区：nop-entropy test scope 写入 + 本仓 app-erp-all test scope 写入 = `auto + dual-agent-approval`（roadmap M1.3 Guard 列）——草案审查通过后、实施前两名独立子代理批准，记录落盘本计划。

## Infrastructure And Config Prereqs

- 跨仓命令：`cd ../nop-entropy && ./mvnw test -pl nop-autotest/nop-autotest-core -am`；本仓 `mvn test -pl app-erp-all -Dtest=TestErpFixtureBundleDirtyImport`（产物日志落 `_tmp/2026-10-01-2255-1-*.log`）。
- 零新增依赖（①单调发生器用 nop-dao 既有 `ISequenceGenerator` 接口；②零框架变更）。
- 回滚策略（机制 v4 全量写入物枚举——批准条件 v3-B）：beans 文件三处（类名/根元素 xmlns/allow-override）+ 文件头注释订正 + `M01SpikeBoot.start()` 一行配置 + `IocSpikeConfigs` 镜像常量 + 新发生器类 + 测试新增/javadoc + 本仓测试类 + runbook/INDEX 行——revert 单提交即可；`db/erp.mv.db` 为测试运行时产物。

## Execution Plan

### Phase 1 - 验收①③晋升补齐（nop-autotest-core test scope）

Status: completed
Targets: `../nop-entropy/nop-autotest/nop-autotest-core/src/test/java/io/nop/autotest/bundle/` + `src/test/resources/_vfs/nop/spike/beans/m01-fixture-spike.beans.xml`（单行覆盖类名替换）
Skill: `nop-testing`

- Item Types: `Proof`（验收测试）、`Add`（单调测试发生器）
- Prereqs: dual-agent-approval 双批准

- [x] （Add | Decision）机制 v4 全部落地：`M13MonotonicSequenceGenerator`（AtomicLong 双方法+head()）+ `M01SpikeBoot.start()` 设点分键 + beans 三处（xmlns:ioc/allow-override/类名替换）+ 头注释订正 + stop() clearTestConfig 卫生项（B 附注采纳）
      - Skill: `nop-testing`
- [x] （Proof）验收①晋升：类 javadoc 登记 acceptance case ①（零断言触碰）+ 新增 `testCleanReplayMonotonicSequenceNoConflict`：正向判别断言（instanceof，失败消息含装载未生效提示——B 附注采纳）+ (i)(ii)(iii) 全过——**正向判别通过即机制 v4 装载生效的运行时实证**
      - Skill: `nop-testing`
- [x] （Proof）验收③晋升：`testRequiresMissing` javadoc 登记 acceptance case ③
      - Skill: `nop-testing`

Exit Criteria:

- [x] 全模块跑法 **22/22 全绿**（21 基线 + 1 新增，既有断言零触碰）——证据 `_tmp/2026-10-01-2255-1-full-module.log`

### Phase 2 - 验收②消费者侧（本仓 app-erp-all）

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/it/TestErpFixtureBundleDirtyImport.java`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 完成（bundle 格式定稿依赖）

- [x] （Proof）`TestErpFixtureBundleDirtyImport` 1/1 绿（见上设计；TO 列断言按实体列型契约 stdDataType=string 比较 seed CNY id 字符串形态——执行期确认 FK 列惯用形态）；**执行期发现两项转化为代码**：① nop-sys 环境 ID 取号经 ORM 读 NopSysSequence 行被 hook 收集 → Exporter 增系统表静默跳过（与 M1.2 导入拒绝同口径，roadmap「系统表不随包导入」）；② CSV 字符串值未经列型转换直插在全局实体缓存下以 String 形态被读回 → normalizeRow 增 stdDataType 转换 + base 趟统一复用 normalizeRow/FK 重写（base 表带 to-one 的场景与本场景一致化）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试绿 + `mvn test -pl app-erp-all` 全模块 **77/0/0 零回归**——证据 `_tmp/2026-10-01-2255-1-appfull.log`

### Phase 3 - runbook 定稿 + roadmap 回写

Status: completed
Targets: runbook + INDEX（复核）+ 本仓 roadmap
Skill: `none`

- Item Types: `Add`（定稿标记）、`Proof`（复核）
- Prereqs: Phase 2 完成

- [x] runbook 定稿：头部定稿标记（三条验收落位+载体登记）+ §3 表补两字段行 + §5 增补 M1.3 执行期发现（系统表跳过/列型转换/单调 beans 显式装载三要点）+ INDEX 描述刷新为全链
      - Skill: `none`
- [x] 本仓 roadmap：§审查记录 追加 M1.3 完成行 + 状态翻转（独立结束审计 ACCEPT 后凭授权执行）
      - Skill: `none`

Exit Criteria:

- [x] runbook 定稿标记 + 路由可达；roadmap 审查记录行落盘

## Decisions

### Decision H — 验收①晋升 vs 新增的落位口径

- 选择：**晋升既有测试（javadoc 登记验收编号，既有断言零触碰）+ (ii) 以新增独立测试方法承载**——避免同场景双测试漂移（原测试保留 (i)(iii) 断言，新增方法专职 (ii) 单调断言）；增补/新方法对既有测试文件的修改 = javadoc 行，属批准写入面审查点。
- 替代方案：原方法内增补 (ii)（拒绝——与「既有断言零改动」承诺张力 + 原方法环境改为单调态会改变 (iii) 的验证语境）；同场景全新复制测试（拒绝——双测试漂移）。
- 残留风险：单调 bean 态主隔离面 = test-scope 资源/类路径作用域（其他模块看不到该 beans 文件）；surefire fork 隔离为次级防线。既有测试仅断言 ≠ 比较、单调语义全兼容（审查确认）。

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_1ef46462, fresh session) — 1 BLOCKER（验收①运行时换装机制被活码证伪：PersistEnvBuilder.java:104 模型构建期快照 sequenceGenerator 进每实体 final idGenerator，运行时换装对 PK 取号零效果且恢复不可逆→改 M1.2 Approver B 处方的 bean 覆盖路径[m01 spike beans 覆盖行类名替换+单调计数器新类]，(ii) 断言改计数器等价形式）+ 2 MAJOR（Decision H 占位无执行项→预填晋升+新增独立方法口径；「既有断言零改动」与「增补」矛盾→assertion add-only 精确化+(ii) 独立方法承载）+ 4 MINOR（try/finally→bean 路径下类级 boot 隔离重述/并发作用域论据入计划[surefire forkCount=4+类间串行]/runbook §3 表补两字段行点名/审计戳事实入 Phase 2[OrmTimestampHelper 自动戳无需手工置]）；7 条基线抽查全命中。上列全部修订已落实，待增量复审。
- Independent draft review iteration 2: acceptable as-is (agent_1ef46462 增量复审) — 1 BLOCKER + 2 MAJOR 实质关闭（新机制经活码二次验证：覆盖文件实存/先例自证/加载链路被 21 测试全绿实证/时序正确）；MINOR-a（Phase 1 Targets 补 beans 资源路径）/MINOR-b（(iii) 措辞回归 M0.1 ③ 口径原文）两处单行修正已随转 active 落实。计划转为可执行契约，dual-agent-approval 先于 Phase 1。
- **Dual-agent approval round 1：Approver A APPROVED with conditions（4 条件）；Approver B REJECTED（agent_90263bd1，1 BLOCKER）**——`m01-fixture-spike.beans.xml` 不在任何 IoC 装载路径（isAppBeans 仅认 `app*.beans.xml` 文件名；无 autoconfig/testBeansFile 引用），实际生效者为 orm-defaults 的 UuidSequenceGenerator（2025-11-05 引入早于 M0.1），该文件自 M0.1 起为静默 no-op；iteration 2「加载链路被 21 测试全绿实证」被证伪（全绿由 orm-defaults 解释）。修复：机制 v3（启动期显式装载 + 正向判别断言）+ Baseline 订正 + Decision H 隔离措辞降级 + INDEX 刷新项。
- **Dual-agent approval round 2：Approver B REJECTED（同会话第二轮，机制 v3 三处活码缺陷）**——① config 键误写 `nop.ioc.app-beans-files`，权威键 `nop.ioc.app-beans.files`（点分，IocConfigs.java:62，全链精确匹配无归一化）；② 同 id 覆盖缺 `ioc:allow-override="true"`（orm-defaults 经 autoconfig 先注册，缺省抛 duplicate-bean-definition，`ioc:default` 豁免为死代码）——该缺陷破坏性最高（容器启动失败全模块红）；③ beans 根元素缺 `xmlns:ioc="ioc"`（从未被解析故从未暴露，佐证 no-op 判定）。处方 = 机制 v4 三处一并修改；正向判别断言保留（v3 键名错误正是它会抓红的形态）。治理面各项（Baseline 订正/审查记录/INDEX/Decision H/(ii) 条件性）核实合格。按 v4 修订后重新提交双批准。

## Dual-Agent Approval Record（保护区：nop-entropy test 写入 + 本仓 app-erp-all test 写入）

- Approver A: **APPROVED with conditions**（round1 agent_86b80e48 4 条件；round2 agent_86b80e48 复审机制 v3 **APPROVED with conditions**——独立复核 B 的 REJECT 五点全证实[含自省 round1 论据被证伪]、v3 新增条件 **v3-A** 配置名钉死 `nop.ioc.app-beans.files` 点分形态[与 B 缺陷①交叉收敛]、**v3-B** 回滚枚举更新为 v4 全量写入物[已落实于 Infrastructure 节]；staging 清单枚举：beans/M01SpikeBoot/IocSpikeConfigs/TestFixtureBundleImport/新发生器类/本仓测试类/文档行）。4 条件沿例全接受 + v3-A/v3-B 履行。
- Approver B: **APPROVED**（round1 agent_90263bd1 REJECTED[1 BLOCKER：spike beans 从未装载]；round2 REJECTED[机制 v3 三处活码缺陷：键名连字符误写/allow-override 缺失破坏性/xmlns:ioc 未声明]；round3 agent_90263bd1 机制 v4 **APPROVED**——三处修复逐锚点核实[键名点分=IocConfigs.java:62/xmlns 与 orm-defaults:7 形态一致/allow-override 覆盖链 BeansDefinition.java:91-95+orm-defaults autoconfig 先注册+nop-task-ext test-reliability.beans.xml 生产实证]，完整装载链七环逐环复核无新增缺陷，test config 经 endTest/resetAll 清理不外溢，单调态兼容既有全部断言[round1 全文件扫描]；2 非阻塞附注：stop() 补 clearTestConfig 卫生项+判别断言失败消息可读性——执行者均采纳实施）。批准 Phase 1-3 按 v4 契约实施。

## Closure Gates

- [ ] 范围内行为完成（验收①②③落位 + runbook 定稿 + roadmap 回写）
- [ ] 相关文档对齐（runbook 定稿标记 + INDEX 复核 + roadmap 审查记录/状态翻转）
- [ ] 已运行验证（跨仓全模块零回归；本仓 app-erp-all 新测试绿 + 既有零回归；compliance checker 零新增漂移；plan-gates --strict PASS）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项；M2.x 分属后续工作项。

### 执行期范围修订记录（框架 main 代码 Fix ×2，验收驱动的 M1.2 交付缺陷）

- **范围变更声明**：计划 Non-Goals 原「不新增框架 main 代码」——执行中验收②暴露两项 M1.2 交付缺陷，修复触及 main 代码（`FixtureBundleExporter`/`FixtureBundleImporter`，均属 M1.3 批准写入面内的同包文件，但语义上为超出「纯测试+文档」原声明的修订，按 guide 规则 10 记录）：
  1. **Exporter 系统表静默跳过**：nop-sys 环境下 ID 取号经 ORM 读 `NopSysSequence` 行被 hook 收集 → 导出器报 table-not-configured 阻断一切真实环境导出——修复 = 对 sys 形表跳过（与 M1.2 导入侧拒绝、roadmap「系统表不随包导入」同口径）。缺陷性质：M1.2/导出侧在真实环境不可用级。
  2. **normalizeRow 列型转换 + base 趟统一复用**：CSV 字符串值直插后经工厂级全局实体缓存以 String 形态读回（TO 列断言实证）——修复 = stdDataType 转换 + base 趟统一 normalizeRow/FK 重写（base 表带 to-one 场景与 payload 一致化，消除 base 趟无悬空检查的缺陷）。
  - 两项均经独立结束审计复核；nop-entropy 侧模块测试与 app-erp-all 消费侧全绿为修复验证。

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

横切关注点 7（`AutoTestCaseDataSaver.removeInputTable` 路径）：**不适用**——本计划零触碰，按 roadmap 规则在此登记一次。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
