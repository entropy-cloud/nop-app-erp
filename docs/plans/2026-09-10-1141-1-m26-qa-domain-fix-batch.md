---
status: active
mission: ai-check-r3
work-item: M2.6
group: "2026-09-10-1141"
verify: [test]
---

# 2026-09-10-1141-1 M2.6 hr+projects+quality P1 修复批（域批独立项 P2-CK-qa-026-r3 severity 字典污染 + P3-CK-qa-030-r3 collectSamples 容错）

## Current Baseline

- 批内面 = 恰 2 条（hr/prj/qa 三域 r3 新立 P1 = 0）：`P2-CK-qa-026-r3`（P2，DIM-B ⑧/②）+ `P3-CK-qa-030-r3`（P3，DIM-B ⑨作业）。M2.0 族裁决（`docs/audits/check/2026-09-06-1645-ai-check-r3/m2-0-family-adjudication.md` §1 row 79/89 + §4 并行域批通道行「M2.6（qa-026-r3 + qa-030-r3）」）为本批范围权威。
- hr/prj 域 r3 新立 finding 全数他往（M2.0 族裁决，均非本批）：hr2-027-r3→M2.8 doc 分片5、hr2-028-r3→M2.8 B 分片1、hr2-029-r3→M2.8 A 分片2、hr2-030-r3→M2.8 D 分片3；prj-022-r3→M2.8 B 分片1、prj-023-r3→M2.8 C 分片4、prj-024-r3→M2.7（dict 族）、prj-025-r3→M2.8 doc 分片5。qa 其余 6 条：qa-027/028-r3→M2.8 A 分片2、qa-029-r3→M2.8 B 分片1、qa-032-r3→M2.8 doc 分片5、qa-031/033-r3→逐条 deferred（§2.10，疑似需求分歧待产品裁决，禁止本批重开）。
- `P2-CK-qa-026-r3` 缺陷面（`docs/audits/check/ai-check-index.md` L284 + `ck-quality-r3.md` §2）：`ErpQaNonConformanceUpgradeToRecallProcessor:52-54` NCR severity=NORMAL 直写 recall.severityLevel，而 recall-severity 字典值域 LOW/MEDIUM/HIGH/CRITICAL 无 NORMAL，:52 注释谎称「码值对齐」——非法字典值污染召回单 UI 映射与报表聚合。索引修复方向：NORMAL→MEDIUM 显式映射（常量级）+ 修注释 + 升级路径负向测试。
- `P3-CK-qa-030-r3` 缺陷面（索引 L288）：`spc-sampling.batch.xml` collectSamples 无 per-item 容错——parameterId 空 / subgroupSize<2 时 chart 抛错毒化整个 chunk（失败隔离仅 chunk 级；evaluate 段 per-sample catch 达标为对照范式——〔2026-09-10 审查实核〕该 catch 位于 `SpcRuleEngine.evaluate` 级联段 `SpcRuleEngine.java:124-129`（WARN 隔离先例），batch XML 本体无 catch，索引「对照 evaluate 段」按此实址消费）。索引修复方向：collectSamples 包 per-chart try/catch WARN 隔离。
- 修复方法约束（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：先写失败测试 → 修复 → 测试绿 + 既有测试零回归；机械/常量级修复以测试红绿为等价物；异常参数传码不传散文（CAT-2 契约）；新增调用点优先 I*Biz 注入（禁止新增无豁免 daoFor 站点——A 族分片 2 正在 M2.8 收敛）；索引修复方向若与执行时 HEAD 实核不符，按 owner doc 证伪/勘误路径登记（lesson 13），不得静默改向。
- 零触碰预期：两 finding 修复面均为常量映射与 job 容错，零 ORM/api.xml/seed/页面变更（不触发保护区双批准与 seed 双面重录）；执行后以 `git status --porcelain` 机械核证。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 4006/0/0/1，41 含测试模块聚合口径；执行期以最新登记行为权威，姊妹计划已披露增量允许并披露——M2.2~M2.5 组内增量先例）；compliance checker 机器块 R2b=242/R2c=1542/R12a=71（R2c 实测 1543 的 +1 漂移已登记 successor baseline-raise 裁决在案，本批不得再新增漂移）；CJK `--strict` PASS（0/0/0/0）。qa 模块参照 184 全绿（M1.12/M1.15 锚点）；`TestErpQaSpcOutOfControl` 首跑实钟跨秒界 flake 已裁决非生产回归（MI.5b 先例），复跑协议沿用。
- 剩余差距：两条 finding 修复 + 负路径测试缺位；M2.9 收官需本批 fixed 证据指针。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`）已完成；本计划组 `2026-09-10-1141` 内执行序 N=1。组内三计划文件面不相交（本批 qa 两站点 / M2.7 域批独立项 / M2.8 族批 base class、action-auth、页面与 doc 面），唯 M2.8 分片1 qa 片触 `erp-qa.action-auth.xml`、分片2 qa 片触 NcrPostingDispatcher 豁免注释——与本批 `UpgradeToRecallProcessor`/`spc-sampling.batch.xml` 站点零重叠。

## Goals

- `P2-CK-qa-026-r3` 修复落地：NCR→召回升级路径 severity=NORMAL 按索引方向显式映射至 recall-severity 字典合法值（MEDIUM），:52 失实注释同步修正，非法字典值不再落入 `recall.severityLevel`；升级路径行为不变式（合法 severity 直通）保持。
- `P3-CK-qa-030-r3` 修复落地：collectSamples 补 per-chart try/catch WARN 隔离（镜像 evaluate 段范式）——parameterId 空 / subgroupSize<2 的坏 chart 不再毒化 chunk，其余 chart 采样结果照常产出，降级可诊断（WARN 携 chart 标识）。
- 先写失败测试：两 finding 各自负路径测试先红后绿（NORMAL 污染断言 / 坏 chart 毒化 chunk 断言），控制组绿。
- 零回归 + 门控持平：qa 套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker 不高于基线；seed 零改动证明。
- M2.9 消费证据落盘：两 finding fixed 证据指针（测试类/方法 + 修复站点）+ 零触碰声明。

## Non-Goals

- 不修 hr/prj/qa 其余 r3 finding（hr2-027~030-r3、prj-022/023/024/025-r3、qa-027/028/029/032-r3——去向以 M2.0 族裁决为准，归 M2.8 分片与 M2.7）。
- 不重开 qa-031/qa-033-r3（§2.10 逐条 deferred，疑似需求分歧归产品裁决通道）与 SPC 双层门控既有裁决（seed-data.md L19 冻结，M1.12 复核成立不重开）。
- 不扩 qa-026 映射面至 r1 qa-004 主路径已修面之外的词表治理（本站为升级路径窄面）；不改 recall-severity 字典值域本身。
- 不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转；不改 ORM/api.xml/页面/`_init-data/`。

## Phase 1 — 失败测试先行（两 finding 负路径）

> 统一类型：Proof（2 项 Proof）。
> Skill: bug-diagnosis-prompt（roadmap M2.6 行指定；先读 `docs/skills/bug-diagnosis-prompt.md` 四阶段定位纪律再动手）
> Targets: `module-quality/erp-qa-service/src/test/java/`（落点沿既有 `TestErpQa*` 布局）；测试数据用 case 级 fixture，禁触 `_init-data/`
> Prereqs: M2.0 计划完成（修复方法 owner doc 在位）

- [x] <Proof> qa-026 失败测试：构造 severity=NORMAL 的 NCR 执行升级召回，断言 `recall.severityLevel` 落 recall-severity 字典合法值（MEDIUM）且无 NORMAL 直写；执行确认红：现状 NORMAL 直写（非法字典值污染复现，`expected: <MEDIUM> but was: <NORMAL>` 类失败输出记入勾选注记）；合法 severity（LOW/HIGH/CRITICAL）直通控制组绿。
      - Skill: bug-diagnosis-prompt
      - 注记：测试类 `TestErpQaNcrUpgradeToRecallSeverity`（`module-quality/erp-qa-service/src/test/java/app/erp/qa/service/`）。红：`normalSeverityMappedToMediumNotPoisoned` 失败输出 `expected: <MEDIUM> but was: <NORMAL>`（surefire `app.erp.qa.service.TestErpQaNcrUpgradeToRecallSeverity.txt`）；控制组 `legalSeveritiesPassThrough`（LOW/HIGH/CRITICAL 直通）绿。
      - Skill: bug-diagnosis-prompt
- [x] <Proof> qa-030 失败测试：构造含坏 chart（parameterId 空 / subgroupSize<2）的 collectSamples 输入，断言同 chunk 其余 chart 采样结果完整产出且坏 chart 被隔离（WARN 可观察）；执行确认红：现状坏 chart 抛错毒化 chunk（其余 chart 结果丢失复现），失败输出记入勾选注记；全合法输入控制组绿。
      - Skill: bug-diagnosis-prompt
      - 注记：测试类 `TestErpQaSpcSamplingBatchFaultIsolation`（`.../spc/`）。红：`badChartDoesNotPoisonChunkSampling` 失败输出为批任务抛 `NopEvalException:invoke-method-fail←NopException:erp.err.qa.spc.subgroup-size-invalid(params={chartCode=CHART-ISO-BAD-SUB1})`（毒化复现，坏 chart 于 `SpcSamplingService.java:124` 抛错致整批中断，good chart 样本丢失）；控制组 `allLegalChartsStillSampled` 绿。〔HEAD 实核勘误〕parameterId 空变体不可持久化——ORM `ErpQaSpcChart.parameterId` mandatory=true（NOT NULL，app-erp-quality.orm.xml:770），空值 chart 无法入库，该分支为防御路径；毒化面以可持久化 subgroupSize=1 坏 chart 构造，per-chart 容错修复覆盖两分支（try/catch 不区分异常来源）。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [x] 两条负路径失败测试在位且当前树红（NORMAL 直写污染 + 坏 chart 毒化 chunk 复现），失败输出注记在案；控制组绿

## Phase 2 — 修复落地（Fix）

> 统一类型：Fix（2 项 Fix）。
> Skill: bug-diagnosis-prompt + nop-backend-dev（Processor/常量面修改反模式自检）
> Targets: `ErpQaNonConformanceUpgradeToRecallProcessor.java`（severity 映射 + 注释修正）+ `spc-sampling.batch.xml` collectSamples 段（per-chart 容错）；owner doc 对账面 = `docs/design/quality/`（spc.md / ncr 相关节零语义变更预期）
> Prereqs: Phase 1 完成（失败测试在位）

- [x] <Fix> qa-026：`UpgradeToRecallProcessor` severity=NORMAL → MEDIUM 显式常量级映射（对齐索引修复方向），修正 :52 失实「码值对齐」注释为真实映射语义说明；合法 severity 直通行为逐字节保持（Phase 1 控制组回归证明）。执行时 HEAD 实核若发现索引处方与实码不符，按 `finding-remediation-method.md` 证伪/勘误路径登记后处置，不得静默改向。
      - Skill: bug-diagnosis-prompt
      - 注记：落地于 `ErpQaNonConformanceUpgradeToRecallProcessor`——新增私有 `mapSeverityToRecall`（null/NORMAL→`RECALL_SEVERITY_MEDIUM`，其余直通），失实「码值对齐」注释替换为真实映射语义（recall-severity 无 NORMAL）；常量级零新 import。索引处方与实码相符，勘误路径未触发。控制组 `legalSeveritiesPassThrough` 绿 = 直通逐字节保持证明。
      - Skill: bug-diagnosis-prompt
- [x] <Fix> qa-030：collectSamples 包 per-chart try/catch WARN 隔离（镜像 evaluate 段 per-sample catch 范式——`SpcRuleEngine.java:124-129` 级联 WARN 隔离先例），WARN 携 chart 标识与失败原因（CAT-2 传码不传散文——消息载体英文，参数传 parameterId/chart 标识本身）；坏 chart 隔离后 chunk 其余 chart 结果完整产出（Phase 1 负路径转绿证明）。
      - Skill: bug-diagnosis-prompt
      - 注记：落地于 `spc-sampling.batch.xml` processor 段——collectSamples 单语句包 try/catch + `logWarn("qa.spc-sampling.collect-samples-skipped: chartId={} reason={}", item.id, e.message)`（静态英文模板 + chartId/reason 参数；NopException message 即错误码，root-cause `erp.err.qa.spc.subgroup-size-invalid` 可 grep）。recalculate/evaluate 对坏 chart 为安全 no-op（无样本路径，已实码核证），维持 chunk-fatal 语义不动（窄面）。测试运行输出实证 WARN 可观察（surefire XML 捕获 `qa.spc-sampling.collect-samples-skipped: chartId=93002 reason=...`）。

Exit Criteria:

- [x] Phase 1 两条负路径测试全绿 + 合法路径控制组与既有 qa 套件保持绿
      - 注记：`TestErpQaNcrUpgradeToRecallSeverity` 2/2 + `TestErpQaSpcSamplingBatchFaultIsolation` 2/2 全绿；qa 域级 `mvn test -pl module-quality/erp-qa-service -am` = 188/0/0/0 BUILD SUCCESS（184 参照 + 本批 4 新增，零新增失败，`TestErpQaSpcOutOfControl` 未触发 flake 无需复跑）
- [x] 修复站点最小面落地、零 daoFor/ORM/页面新增站点；勘误路径未触发或已按路径登记
      - 注记：生产改动仅 2 文件（Processor 私有映射方法 + batch XML 脚本段）；compliance checker stash 对比（改/不改生产文件 R1b=1/R1d=15/R2b=242/R2c=1543/R12a=71 逐值相同）机械证明零新增 checker 站点；`git status --porcelain` 无 `module-*/model/`、`*.api.xml`、页面命中。qa-030 parameterId 空变体不可持久化（mandatory=true）勘误已按路径登记于 Phase 1 注记

## Phase 3 — 批级证明与 M2.9 消费证据

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [x] <Proof> 域级回归：`mvn test -pl module-quality/erp-qa-service -am` 全绿零新增失败（参照 qa 184，数字记入注记；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露；`TestErpQaSpcOutOfControl` 实钟 flake 按 MI.5b 复跑协议处置）。
      - Skill: none
      - 注记：**188 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS**（02:03 min）= 参照 184 + 本批新增 4（`TestErpQaNcrUpgradeToRecallSeverity` 2 + `TestErpQaSpcSamplingBatchFaultIsolation` 2），零新增失败；`TestErpQaSpcOutOfControl` 首跑即绿，未触发 MI.5b 复跑协议。
      - Skill: none
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 known-good-baselines 最新登记行，姊妹计划已披露增量允许并披露）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则不高于机器块（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径非空集证明构成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
      - 注记：①全 reactor `mvn test` **BUILD SUCCESS，4012/0/0/1**（surefire 22 模块聚合；= 09-09 基线行 4006 + 姊妹 M2.2~M2.5 组内已披露增量 +2（其 feat 提交 `bcc15634b`/`e62d04988`/`7848d65e1`/`48b57cd06` 于基线行后落 HEAD `3cd66dff7`）+ 本批 +4；skipped=1 与基线剖析一致，零新增失败）；②`mvn clean install -DskipTests` **BUILD SUCCESS 156/156**（01:38 min）；③compliance checker **exit 0**：机器块 R2b=242 ✓ / R2c=1543（= 计划已登记的 +1 漂移实测值，本批零新增——stash 对比改/不改生产文件逐值相同）/ R12a=71 ✓（R1b=1/R1d=15 为 M2.2~M2.5 姊妹提交后非机器块规则值，本批 stash 对比零贡献）；④CJK `--strict` **PASS exit 0**（0 new violations vs 冻结快照；本批触及文件零 CJK 条目）；⑤`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` **空**（`git ls-files` 同路径 = 373 文件非空集构成）。
      - Skill: none
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P2-CK-qa-026-r3 / P3-CK-qa-030-r3 fixed 证据指针（测试类/方法 + 修复站点）；零触碰声明（`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中）。
      - Skill: none
      - 注记：**P2-CK-qa-026-r3 fixed**：修复站点 `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/processor/ErpQaNonConformanceUpgradeToRecallProcessor.java`（`mapSeverityToRecall` 私有映射 + 注释修正）；测试 `app.erp.qa.service.TestErpQaNcrUpgradeToRecallSeverity#normalSeverityMappedToMediumNotPoisoned`（红→绿）+ `#legalSeveritiesPassThrough`（直通控制组常绿）。**P3-CK-qa-030-r3 fixed**：修复站点 `module-quality/erp-qa-service/src/main/resources/_vfs/nop/batch-task/qa/spc-sampling.batch.xml`（collectSamples per-chart try/catch WARN 隔离）；测试 `app.erp.qa.service.spc.TestErpQaSpcSamplingBatchFaultIsolation#badChartDoesNotPoisonChunkSampling`（红→绿）+ `#allLegalChartsStillSampled`（控制组常绿）。**零触碰声明**：`git status --porcelain` 全树仅 2 生产文件修改（上述两站点）+ 2 测试类新增 + 2 `_cases` autotest.yaml 目录 + 计划/姊妹计划文档文件，无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中（不触发保护区双批准与 seed 双面重录）。
      - Skill: none

Exit Criteria:

- [x] qa 模块全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
- [x] M2.9 消费证据（fixed 指针×2 + 零触碰声明）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-1141-1-m26-qa-domain-fix-batch-1-95c3874b to 2026-09-09-210030-mission-driver
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-1141-1-m26-qa-domain-fix-batch-1-95c3874b

## Verification

- pass test 2026-09-10-1336 exit=0

## Closure

Status Note: 独立闭包审计（本访问实跑验证套件全绿）通过——两 finding 修复实码核证落地且被负路径测试消费，机械门 12/12 全勾，台账齐备，可派生 completed。

Closure Audit Evidence:

- dispatch audit #audit-2026-09-10-1336-2026-09-10-1141-1-m26-qa-domain-fix-batch-1-18be3258 to 2026-09-09-210030-mission-driver models={exec:zhipuai/glm-5.2,aud:zhipuai/glm-5.2}
- accepted #audit-2026-09-10-1336-2026-09-10-1141-1-m26-qa-domain-fix-batch-1-18be3258：审计结论 approved——P2-CK-qa-026-r3（`ErpQaNonConformanceUpgradeToRecallProcessor.mapSeverityToRecall` NORMAL/null→MEDIUM 显式映射 + :52 注释修正）与 P3-CK-qa-030-r3（`spc-sampling.batch.xml` collectSamples per-chart try/catch WARN 隔离）实码核证落地、调用点接线在位、零触碰声明成立（porcelain 无 model/api.xml/_init-data/页面命中）；本闭包访问实跑：`mvn clean install -DskipTests` 156/156 BUILD SUCCESS exit 0（01:38 min）+ 全 reactor `mvn test` **4027 tests / 0 failures / 0 errors / 1 skipped BUILD SUCCESS exit 0**（41 测试模块聚合，13:20 min；= M2.5 收官 4023 + 本批新增 4，零新增失败 vs 09-09 基线行 4006/0/0/1）；`plan-check.mjs --strict` 结构项 12/12 全勾，Verification pass line + Closure 回执配对齐备。
