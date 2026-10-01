# 2026-10-02-0100-1 M2.3 fixture-bundle 消费落地 + owner doc 对齐

> Plan Status: completed
> Last Reviewed: 2026-10-02
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M2.3（bundle 配置 + ≥2 测试类复用示例 + 资产边界表扩列 + 实测基线）
> Related: M1.1-M1.3（框架/验收 done）；M2.1（批量入口 done）；M2.2（skill done）；M1.1 Deferred（repo 级敏感门控 successor）
> Audit: required

## Current Baseline

- 框架/验收/入口/skill 全 done（nop-entropy 三提交 + 本仓 `785e217a0`/`86bd5f6cd`）；`docs/architecture/testing-strategy.md:94-105` 四类测试资产边界表**无**夹具包列（M2.2 审计 MINOR-2 前向引用留本计划扩列）；`docs/architecture/seed-data.md` 无 bundle 关系记录；`app-erp-test-data/_vfs/test-data/load-order.txt` 零消费（M1.1 Decision C：manifest 唯一序源，本计划登记退役）。
- 验收②（`TestErpFixtureBundleDirtyImport`）与批量入口（`TestErpFixtureBundleBatchEntry`）各自构造+导出+导入——尚无「同一 bundle 被 ≥2 测试类经 M1.2 装载路径复用」的真增量证明。
- 剩余差距：消费示例、owner doc 扩列/关系记录/退役登记、实测基线。

## Goals

- **跨类复用示例**：共享导出 helper（JVM 级一次构造+导出，synchronized；**导出后清理构造行**——per-class fresh-DB 语义下两类断言同一集合、任意 surefire 顺序/单类 `-Dtest` 运行均成立）+ **两个不同测试类经 M1.2 导入路径消费同一 bundle**（各持独立 Importer 实例；断言 baseImported=1 + payloadImported=1 + 回读——宿主类 fresh seeded DB = 天然 H2→H2 实例间搬运语义）。demo bundle 配置**含敏感列声明**（currency NAME 列声明 masked 展示机制链路：声明→导出 `MASKED-BUNDLE-SEED` 占位→导入）。bundle 仅落 `target/`（零 `_init-data`/零 seed CSV 变更——**seed 联动义务不触发**，本 Guard 的 plan-first 即本计划自身）。
- **实测基线（roadmap 口径：base=全量主数据 + 1 payload；结束审计 MAJOR-1 口径订正）**：录制会话 findAll 触达 md 域全量 **25 实体** seed 行 + 1 汇率 payload → 基线 bundle 实测导入耗时（System.nanoTime，前后 `System.gc()` 括弧）；**实测路径 = payload 层直插（Decision K），base 业务键对账路径未测**（20+ 表逐表业务键声明的配置成本 vs 判据目的——登记为已测边界）；堆占用**未记录**（GC 干扰下无可信采样——审计确认原「信息性记录」措辞暗示存在并不存在的观察值，已订正）。
- **owner doc 对齐**：`testing-strategy.md` 资产边界表扩夹具包行（M2.2 前向引用兑现）；`seed-data.md` 记录 bundle base 层与部署 seed 的 requires 依赖关系 + `load-order.txt` 退役登记（M1.1 Decision C）。

## Non-Goals

- 不做真实数据 bundle 入库（repo 级敏感门控 successor 义务不变——本计划 bundle 全部运行时 `target/` 产物）。
- 不触 `_init-data/**`、`module-*/model/*.orm.xml`、`_cases` 布局。
- 不做拆分/增量化导入（实测基线仅为判据记录）。

## Task Route

- Type: `implementation-only change`（测试示例 + 纯文档对齐）
- Owner Docs: `docs/architecture/testing-strategy.md` + `docs/architecture/seed-data.md`
- Skill Selection Basis: `Skill: nop-testing`。Guard：plan-first（seed 联动义务）——本计划自身即该 plan；零 `_init-data` 触碰即不触发双面重录。nop-entropy 零写入（M2.3 交付全在本仓）→ 无外部仓保护区环节，结束审计门控不变。

## Infrastructure And Config Prereqs

- `mvn test -pl app-erp-all -Dtest=TestErpFixtureBundle*` + 全模块复验；日志落 `_tmp/2026-10-02-0100-1-*.log`。
- 回滚：新增文件 + 两处文档段落——revert 单提交。

## Execution Plan

### Phase 1 - 跨类复用示例与实测基线

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/it/`
Skill: `nop-testing`

- Item Types: `Add`（共享 helper）、`Proof`（两个消费测试类 + 基线记录）

- [x] （Add）`FixtureBundleDemoSupport`（ensureExported/ensureBaselineExported 均 JVM 级幂等——nit-1 采纳；导出后清理构造行；基线 gc 括弧采集）
      - Skill: `nop-testing`
- [x] （Proof）`TestErpFixtureBundleConsumerA` 1/1 绿（baseImported=1/baseSkipped=0/payloadImported=1 + masked 占位入库[原始名值不出现]；跨类共享消费 #1）
      - Skill: `nop-testing`
- [x] （Proof）`TestErpFixtureBundleConsumerB` 1/1 绿（同一断言集合，顺序无关；基线耗时断言 >0）
      - Skill: `nop-testing`
- [x] （Proof）基线实测（MAJOR-1 整改后 v2）：全量主数据 **payload 层直插路径**（md 25 实体触达 + 1 构造 payload；bundle **115 行**[25 表全非零，逐表 rowCount 见 bundle manifest；触达含同 JVM 先行 demo 导入残留]）→ 导入 **≈25ms**（gc 括弧；堆未记录；base 对账路径未测——Decision K）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 两消费测试绿（顺序无关）；同 bundle 两类复用成立；**零 `_cases` CSV 副本**（措辞订正[M-2]：两 consumer 各新增框架标准 `_cases/.../autotest.yaml`[随闭包提交，按仓惯例]，零 CSV 实质成立）；基线数字落 plan（115 行/25ms）与 `_tmp/2026-10-02-0100-1-baseline-rerun.log`（M23BASELINE 行在案；M-1 已补产留痕）

### Phase 2 - owner doc 对齐与 roadmap 回写

Status: completed
Targets: `docs/architecture/testing-strategy.md` + `docs/architecture/seed-data.md` + roadmap
Skill: `none`

- Item Types: `Add`

- [x] testing-strategy.md 资产边界表扩「夹具包（fixture bundle）」行（M2.2 前向引用兑现）
      - Skill: `none`
- [x] seed-data.md：requires 依赖关系 + `load-order.txt` 退役登记（M1.1 Decision C）+ 实测基线（115 行/≈25ms/前置态注明 fresh seeded + demo 残留）
      - Skill: `none`
- [x] roadmap：审查记录行 + 状态翻转（独立结束审计 ACCEPT 后凭授权执行）
      - Skill: `none`

Exit Criteria:

- [x] 两 owner doc 落盘；roadmap 审查记录行落盘

## Decisions

### Decision J — 跨类复用的共享与重复语义（per-class fresh-DB 收敛）

- 选择：**拥抱 per-class fresh DB = 天然 H2→H2 实例间搬运语义**——helper `ensureExported` 导出后**删除构造行**（宿主类 DB 回到干净 seed 态），两类对同一 bundle 断言同一集合（baseImported=1/payloadImported=1/回读/masked 行为），任意 surefire 执行顺序与单类 `-Dtest` 运行均成立；masked 机制链路（声明→占位导出→导入）由 demo bundle 的 NAME 列声明承载。
- 替代方案：共享文件 H2 载体（拒绝——预设固定执行顺序、与 per-class fresh-DB 机制对抗）；第二类断言 payload 重复行（拒绝——初稿缺陷：fresh-DB 重灌下重复行永不发生，且预设顺序）。
- 残留风险：跨类共享的证明力 = 「同一 bundle 文件被两个测试类经 M1.2 路径各自成功导入」，非同库并存；payload 重复语义由 M1.2 Decision G 消费纪律文档承载（runbook），不在测试断言面。

### Decision K — 实测基线路径选型（结束审计 MAJOR-1 补登）

- 选择：**payload 层直插路径**测量全量主数据基线（25 实体 findAll 触达 + 1 构造 payload，bundle 115 行/25 表，导入 ≈25ms）——避免 20+ 表逐表业务键声明的配置成本；判据目的（此规模是否需要拆分/增量化）由写入吞吐即可裁答。
- 替代方案：base 层业务键对账路径重测（20+ 表逐表业务键声明 + 对账查询开销）——**未测，登记为已测边界**；若后续真实需求是对账型导入性能，须另行测量。
- 残留风险：基线数字只代表写入型导入；对账型导入性能无判据（seed-data.md 已同轮订正）。

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_f775ed03, fresh session) — 1 BLOCKER（roadmap M2.3 判据「含敏感列声明，列级 masked」零承载→demo bundle NAME 列声明 masked 展示机制链路+占位断言）+ 2 MAJOR（跨类时序与 per-class fresh-DB 矛盾且 Decision J 全空→拥抱 fresh-DB 语义的顺序无关重设计+导出后清理构造行；实测基线口径缩水[2 行 demo ≠ 全量主数据]→新增 findAll 触达 md 全量 seed 行的基线 bundle 场景）+ 3 MINOR（堆测量 gc 括弧+断言仅耗时>0/Phase 1 退出去 owner-doc 化/零 _cases 副本显式验收）；5 条基线抽查全命中。全部修订已落实。
- Independent draft review iteration 2: acceptable as-is (agent_f775ed03 增量复审) — 六项修订全部落实（占位符字面值与 MASKED_PLACEHOLDER 实现精确一致/顺序无关机制推演成立/基线口径标注 roadmap 原文/gc 括弧/退出倒挂消除/零副本显式验收）；2 nit（ensureBaselineExported 同型幂等口径+基线数字注明 DB 前置态）随实施落盘。计划转 active 进入实施。
- **状态流订正（结束审计 MAJOR-2 登记）**：iteration 2 accept 后 Plan Status 未即时翻 active 即进入实施（draft→completed 跳跃），增量复审实际发生但收敛记录迟记至此——本行即补记；实施后结束审计 round1 NEEDS REVISION（见 Closure），整改后由同一审计者增量复审。

## Closure Gates

- [x] 范围内行为完成（跨类复用示例 + 实测基线 + 两 owner doc + roadmap 回写）
- [x] 相关文档对齐（testing-strategy 扩列 + seed-data 关系/退役/基线 + roadmap 审查记录/状态翻转）
- [x] 已运行验证（消费测试绿 + app-erp-all 全模块复验；compliance checker 零新增漂移；plan-gates --strict PASS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项；拆分/增量化导入以实测基线为判据、按 roadmap Non-Goals 触发条件由人工追加。

## Closure

Status Note: 跨类复用示例（两消费类×同一 bundle×顺序无关）+ masked 机制链路 + 全量主数据基线（payload 直插路径 115 行/25 表/≈25ms——Decision K 登记选型与已测边界）+ 两 owner doc 对齐（testing-strategy 五类措辞订正/seed-data 诚实口径）全部落地。审计 round1 2 MAJOR（基线登记语义三处失实+状态流簿记簇）+ 4 MINOR 整改后待增量复审。

Closure Audit Evidence:

- Auditor / Agent: independent subagent agent_c06c1a97（fresh session，同会话两轮）——round1 **NEEDS REVISION**（MAJOR-1 基线登记语义与实测路径实质不符三处失实+4 实体覆盖缺口+堆承诺未兑现；MAJOR-2 草案收敛未记录+状态流 draft 停留；MINOR M-1~M-4）→ 整改（路径 ii：seed-data 诚实口径重写+实体补全 25 重测+Decision K 补登+状态流补记+strategy 订正+M-1/M-2）→ round2 **ACCEPT**（0 BLOCKER / 0 MAJOR / 4 MINOR 随闭包提交订正：MINOR-A 24→25 表三处[已订正]/MINOR-B plan L71 旧数字[已订正 115/≈25ms]/MINOR-C Plan Status completed[已置]/MINOR-D v2 后留证[`_tmp/2026-10-02-0100-1-appfull-summary.log` 80/0/0 + `checker.log` exit 0，本轮补产]）；审计方独立复跑两测试 1/1×2 exit 0 + M23BASELINE elapsedMs=26 rows=115 可复现 + plan-gates --strict PASS。

横切关注点 7（`AutoTestCaseDataSaver.removeInputTable` 路径）：**不适用**——本计划零触碰，按 roadmap 规则在此登记一次。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
