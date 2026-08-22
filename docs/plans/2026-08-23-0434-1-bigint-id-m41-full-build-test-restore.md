# 2026-08-23-0434-1-bigint-id-m41-full-build-test-restore 主键/外键 string 化 M4.1（1/3）：JVM 层全量恢复——全量构建 + 全量测试 + String 化残留资产清扫

> Plan Status: completed（2026-08-23 三 Phase 全部执行完成 + 独立结束审计 ACCEPT（`ses_fd496ec73ffeYmmm2JsnTNegCg`），证据见 Closure 节；roadmap M4.1 状态按计划门控保持 `todo` 至批内序 3 终态更新。前期：独立草案审查两轮收敛 iteration 2 `acceptable as-is`，批准记录见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M4.1（其一：JVM 层全量构建/测试恢复 + String 化残留资产清扫）
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/id-string-migration-roadmap.md` M4.1 行（全量构建恢复 + 全量测试；依赖全部 M1-M3 ✅）+ 各域 plan successor=M4.1 登记义务（page.yaml `:Long` 盲区 / IoC delta 回收 / self-wait delta 复核 / ErpAllWebPagesTest / 快照全量复查）
> Related: `docs/plans/2026-08-23-0434-2-bigint-id-m41-e2e-suite-id-assertion-repair.md`（批内序 2，依赖本计划 runner jar 与全绿基础）、`docs/plans/2026-08-23-0434-3-bigint-id-m41-compliance-baseline-docs-closure.md`（批内序 3，依赖本计划 build+test 证据）、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（page.yaml `:Long` 盲区登记来源，version-diff 修复先例）、`docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`、`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-after-platform-reinstall.md`、`docs/testing/known-good-baselines.md`（2026-08-20 全量基线 = 恢复目标参照）
> Audit: required（独立草案审查 + 独立结束审计；无 `model/*.orm.xml` 变更，不触发保护区域双批准）

## Current Baseline

（以下均为 2026-08-23 live-repo 实测）

- **mission 迁移面完成态**：M1-M3 全部 19 域 + common-service `done`；`node tools/check-bigint-id-types.mjs scan` = 实际修改 0 列（1662 列全落源，19 域 orm 全 String）；登记册 `tools/id-migration-registry.json5` 259 条 = orm-deferral 8 全 retired + service-bridge 128 全 retired + backward-pointer 123 active（全部已附兑付注记）。
- **全量构建/测试空白**：mission 期间（08-21 起）全量 `mvn clean install` / 全量 `mvn test` 从未执行（D3 域级 no-am 口径设计使然，横切 §1「中间态全量构建失败属设计使然」）。最后全绿全量基线 = **2026-08-20**（known-good-baselines 08-20 行）：156 reactor 模块 BUILD SUCCESS（1:40）+ `mvn test` 全 reactor **3789 tests / 0 failures / 0 errors / 1 skipped**（12:51）+ compliance 19 规则零漂移。本计划的恢复目标 = 重建同等口径全绿（测试计数允许差量，须逐项解释）。
- **String 化残留资产盘点（live 实测）**：
  1. **手写 page.yaml raw-GraphQL `:Long` 变量 4 处**：`module-aps/erp-aps-web/.../dashboard/schedule-gantt.page.yaml:59`（`$mid`，:61 兜底 `${machineId || null}` 已合法）、`module-notify/erp-notify-web/.../ErpSysNotification/inbox.page.yaml:177`（`$id` markRead mutation）、`module-b2b/erp-b2b-web/.../dashboard/edi-detail.page.yaml:45`（`$did`，:48 兜底 `"${ediDocId || 0}"` 为 Int 字面量须同型改 String/空值守卫）、`module-b2b/.../dashboard/asn-flow.page.yaml:79`（`$aid`）。M3.6 已证该形态致 adaptor 静默降级（version-diff hasCompare:false 功能失效先例）；各域 plan Non-Goal 登记 successor=M4.1 mission 级清扫。
  2. **IoC delta 缺 `x:extends="super"`：live 实测仅 2 处**——md/notify 的 `erp-*-service/src/test/resources/_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`。bug doc `2026-08-22-ioc-delta-missing-extends-super.md` 声称「四域（md/notify/b2b/ct）未修正」已过时：b2b 侧已由 M2.5 早域复跑时按修正版先例补齐（文件头 2026-08-22 修正注记实证）、ct 侧实测 HAS-EXTENDS；仅剩 md/notify 两文件缺该属性（整文件替换平台 beans，静默丢 `nopOrmEntityChangeLogInterceptor` 等——md/notify 无 audit tagSet 实体故测试未暴露）。bug doc 状态段需同步更正 + 回收注记。
  3. **nopSequenceGenerator self-wait 兼容层 delta 9 域在位**（aps/ast/b2b/ct/cs/fin/hr/md/notify 的 `default/nop/sys/beans/app-dao.beans.xml`；hr 另有第二环 nopDataAuthChecker delta）。successor 登记 =「平台修复后统一移除，M4.1 复核」：复核义务 = 检查 nop-entropy（外部仓库，只读）修复状态 + bug doc 记录；移除与否按平台状态裁决。
  4. **json5 快照数字 id 残留 6 文件 / 8 命中点**（全 fin，`rg '"\w*[Ii]d": [0-9]' <全部 _cases> --glob '*.json5'` 实测）：`TestErpFinPostingService` 2 文件（`"id": 9`）+ `TestErpFinPeriodCloseEndToEnd` 4 文件（`"id": 1`）+ `testMultiCurrencyPostingLineLevelAssertions/1_multiccy_line_level.json5` 内 `"currencyId": 2` ×3。fin CHECKING 497/497 绿与数字形态并存 → 需查明（比对语义宽容或该输出面未参与比较），并对齐 String 形态（mission 裁决：快照不依赖 Number 宽容）。
  5. **md `findFirstByOrg(Long)` 签名残留 + 5 处语义参数桥**（live 实测）：`IErpMdAcctSchemaBiz.findFirstByOrg(@Name("orgId") Long orgId)`（`erp-md-dao/.../IErpMdAcctSchemaBiz.java:17`）+ `ErpMdAcctSchemaBizModel.java:28` 同型；5 处 `ConvertHelper.toLong(orgId)` 桥（mfg `MaterialIssueStockMoveBuilder:55` / sal `DeliveryStockMoveBuilder:52`、`ReturnStockMoveBuilder:61` / pur `ReceiveStockMoveBuilder:54`、`ReturnStockMoveBuilder:55`，注释均「md 侧签名翻转后退役」）。退役条件 = md 侧签名翻转——pur plan `2026-08-22-1814-1`、sal plan `2026-08-22-1814-2` Deferred 节登记「另案/M4.1 兜底」，mfg plan `2026-08-22-1302-3` 登记 M0.1 附录 C 遗留。**M4.1 为唯一剩余载体，本计划兑付**（orgId 为 FK 语义参数，mission 完成态承诺 Java 层全覆盖）。
- **app-erp-all `reflect-config.json`**：hr plan `2026-08-22-0731-1` 登记「归 M4.1 全量重建」——类名未变仅字段类型变化，反射注册预期不受影响，Phase 2 全量构建 + Phase 3 全量测试即兑付载体（本计划显式裁决注记）。
- **web 页面测试治理排除（已提交决策）**：19 域 `Erp*WebPagesTest` `@Tag("full-app")` + 各域 web pom surefire `<excludedGroups>` 模块级排除（依赖全量 classpath，如 `/erp/xlib/control.xlib`）；successor = **app-erp-all `ErpAllWebPagesTest`**（`app-erp-all/src/test/java/io/nop/app/all/web/ErpAllWebPagesTest.java`，validateAllPages 口径）。
- **预存 known failure（与本 mission 无关）**：`ErpAllWebPagesCollectTest` `@Disabled`（JDK26/ANTLR H-2，2026-07-20 登记，重新启用条件 A/B/C 见 known-good-baselines Known Failures）。app-erp-all 其余测试（seed/auth/meta/flux demo）自 08-20 后未跑。

## Goals

- 4 处 page.yaml `:Long` → `:String` 化（含 `|| 0` Int 兜底同型修正与 adaptor 空值守卫，pur/drp/log 先例形态）+ YAML 良构 + 对应 web 模块重建绿 + 全仓 page.yaml `:Long` 复扫清零（仅余 `$lim:Int` 类合法非 id 声明）。
- md/notify 2 处 IoC delta 补 `x:extends="super"` + `xmlns:feature="feature"`（fin/b2b 修正版先例）+ md/notify service 测试复跑绿 + bug doc 状态更正与回收注记。
- md `findFirstByOrg(Long)` → `(String)` 签名翻转（IBiz + BizModel）+ 5 处 `ConvertHelper.toLong(orgId)` 桥退役（mfg/sal/pur，pur/sal/mfg plan Deferred 登记的 M4.1 兜底义务兑付）+ md/mfg/sal/pur service 测试复跑绿。
- self-wait delta 平台修复状态复核 + bug doc 记录（保持/移除裁决与理由落盘）。
- `mvn clean install -DskipTests` **156 reactor 模块 BUILD SUCCESS**（mission 后首次全量构建恢复）。
- `mvn test` 全 reactor **0 failures / 0 errors**（skipped 仅 ErpAllWebPagesCollectTest 预存口径）；测试计数落盘并对照 08-20 基线 3789 解释差量。
- `ErpAllWebPagesTest` validateAllPages 0 errors——19 域 web 页面治理排除的 successor 义务兑付。
- 6 处 fin json5 快照数字 id 对齐 String 形态（6 文件 8 命中点，含 `currencyId` ×3）+ fin CHECKING 复跑绿 + 全量 `_cases` json5 数字 id 残留 grep = 0（roadmap M4「快照重录全量复查」义务兑付）。
- known-good-baselines 新增「full mvn build + test」基线条目 + 日志。

## Non-Goals

- 不修 E2E 套件（批内序 2）；不跑 compliance checker 与文档/登记册/roadmap 收尾（批内序 3）。
- 不改任何 `model/*.orm.xml` 与生成件；不新增/移除测试类。
- 不处理 `ErpAllWebPagesCollectTest` `@Disabled`（预存 JDK26/ANTLR H-2 known failure，启用条件已登记，与迁移无关）。
- 不修改 nop-entropy（外部仓库；self-wait 根因的平台修复属外部事件，本计划只读复核 + 登记）。
- 不移除 nopSequenceGenerator delta 本体，除非复核证实平台修复已落地且移除后受影响域测试复跑全绿（裁决落盘）。
- fin web/app 补做不复做（M2.7 已兑现：fin-web/fin-app String 形态重建绿 + `:Long` 清零复核；本计划仅在全量构建中覆盖验证）。

## Task Route

- Type: `implementation-only change`（残留资产 Fix + JVM 层全量恢复验证）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（M4.1 行 + 横切 §1/§4 + M4 收尾清单）、`docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`、`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-after-platform-reinstall.md`、`docs/testing/known-good-baselines.md`、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md` Deferred 节（page.yaml 盲区登记）
- Skill Selection Basis: `nop-debugging`（全量构建/测试失败与新破坏先诊断后修——已知失败模式清单优先对照）；`nop-testing`（快照比对语义复核与 String 形态对齐、CHECKING 复跑）；`nop-backend-dev`（md IBiz/BizModel `findFirstByOrg` 签名翻转——跨实体调用与签名规则）。page.yaml 修复为已固化先例模式（M3.6 version-diff / M2.5 three-way-match / M3.7 net-requirement / M3.10 shipment-tracking），仅改变量类型声明与兜底字面量，不涉新增页面/控件，无需 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。本地 Maven 仓库已含全部 19 域链 install（各域 plan 兑现）+ 08-20 全量 install 基线；全量构建预期 ~2min、全量测试 ~13min（08-20 基线时长参照）。
- 工具：`node tools/check-bigint-id-types.mjs scan`（终态复核）、`rg`（grep 门控与快照残留复查）。

## Execution Plan

### Phase 1 - String 化残留资产清扫（page.yaml ×4 + IoC delta ×2 + self-wait 复核 + 快照残留对齐）

Status: completed（2026-08-23 执行：4 处 page.yaml `:Long` → `:String` 就地修复（aps `$mid`/notify `$id`/b2b `$did`+`$aid`；edi-detail:48 `|| 0` Int 兜底改 `|| null` + adaptor `!== ''` 守卫、asn-flow adaptor 补 `!== ''` 守卫，drp/log 先例形态）+ YAML 良构 ×4 + aps/notify/b2b 三 web 模块 no-am clean install BUILD SUCCESS + 全仓 page.yaml `:Long` 复扫清零；md/notify 首环 delta 补 `x:extends="super"` + `xmlns:feature`（fin/b2b 修正版先例对齐 + xmllint 良构）——**执行期发现：extends 恢复后被整文件替换屏蔽的第二 self-wait 环（hr 2026-08-22 补记同源）暴露（md 21 类 self-wait 实证），按 hr 先例落位第二 delta `erp/common/beans/app-service.beans.xml`（nopDataAuthChecker daoProvider lazy-property，x:extends="super" 保全 E4.2 MaskAuditRecorder）后 md 155/155 + notify 23/23 复跑绿**（双基线维持）；bug doc `ioc-delta-missing-extends-super` 状态段更正（四域声称过时 → b2b/ct M2.5 已补齐 + md/notify 本计划回收 → 九域全部 HAS-EXTENDS）+ 回收进度注记 + 第二环暴露因果注记；`findFirstByOrg` 签名翻转（IBiz:17 + BizModel:28 `Long orgId` → `String orgId`，String 直传 eq 匹配与 orgId String 列一致）+ 5 桥退役（mfg/sal/pur StockMoveBuilder `ConvertHelper.toLong(orgId)` + 桥注释 + ConvertHelper import 三重清零，String 直传）+ 全仓调用点复扫（main 7 处 = 2 声明 + 5 消费全 String，test 零调用点）+ mfg 289/289 + sal 309/309 + pur 334/334 复跑绿（md 已含于上项）；self-wait 复核裁决落盘（见下方 Decision 记录）；fin 6 文件 8 命中点数字 id 手工对齐 String 形态（`"id": 9`→`"9"` ×1、`"id": 1`→`"1"` ×4、`"currencyId": 2`→`"2"` ×3）+ 并存原因查明（**比对语义宽容**：`MatchPatternCompileConfig` 默认 equalsChecker = `JsonMatchHelper::valueEquals`（nop-match jar BootstrapMethods 实证），String↔Number 混合先 `ConvertHelper.toString` 归一再比较——非「输出面未参与比较」；mission 裁决快照不依赖宽容 → 对齐）+ fin 497/497 CHECKING 复跑绿（**附带修复**：TestErpFinBankReconAutoReverseJob.testCurrentMonthReconNotReversed `RECONCILIATION_DATE`/`STATEMENT_DATE`/`TRANSACTION_DATE` 三 today 派生单元格日期漂移（2026-08-22 录制 → 08-23 运行即红，非 id 缺陷）按 ct SIGN_DATE/mfg delVersion `*` 通配先例 ×3 单元格修复）+ 全仓 `_cases` json5 数字 id grep = 0）
Targets: `module-aps/erp-aps-web`（1 page.yaml）、`module-notify/erp-notify-web`（1 page.yaml）+ `erp-notify-service`（1 delta xml）、`module-b2b/erp-b2b-web`（2 page.yaml）、`module-master-data/erp-md-service`（1 delta xml + `ErpMdAcctSchemaBizModel` 签名）+ `erp-md-dao`（`IErpMdAcctSchemaBiz` 签名）、`module-manufacturing/erp-mfg-service` + `module-sales/erp-sal-service` + `module-purchase/erp-pur-service`（各 StockMoveBuilder 桥退役）、`module-finance/erp-fin-service/_cases`（6 json5）、`docs/bugs/`（2 文件）
Skill: `nop-testing`（快照项）/ `nop-debugging`（如遇行为异常先诊断）

- Item Types: `Fix | Decision | Proof`
- Prereqs: 无（全部 M1-M3 done）

- [x] Fix: 4 处 page.yaml `:Long` → `:String`（aps `$mid` / notify `$id` / b2b `$did` / b2b `$aid`）；edi-detail:48 `"${ediDocId || 0}"` Int 字面量兜底按先例改 String/空值守卫（ct `|| ''` 或 drp/log `|| null` + adaptor `!== ''` 守卫形态）；python `yaml.safe_load` 良构校验 ×4。
  - Skill: none（先例模式复制）
- [x] Proof: aps/notify/b2b 三个 web 模块 no-am 重建 BUILD SUCCESS（`mvn clean install -pl <3 个 web 模块> -DskipTests`）+ 全仓 page.yaml `:Long` 复扫（`rg ':Long' --glob '*.page.yaml'` 仅余 `$lim:Int` 类合法非 id 声明，逐条注明）。
  - Skill: none
- [x] Fix: md/notify delta 根元素补 `x:extends="super"` + `xmlns:feature="feature"`（仅 `nopSequenceGenerator` 保持 `x:override="replace"`；fin/b2b 修正版先例一字不差对齐）+ `xmllint --noout` 良构。
  - Skill: none
- [x] Proof: md/notify service 测试复跑绿（`mvn test -pl module-master-data/erp-md-service,module-notify/erp-notify-service`；参照基线 md 155 / notify 23）——容器启动 + 兼容层行为双验证。
  - Skill: `nop-testing`
- [x] Fix: bug doc `2026-08-22-ioc-delta-missing-extends-super.md` 状态段更正（「四域未修正」→ 实测 b2b/ct 均已按 M2.5 早域复跑修正版先例补齐（文件头 2026-08-22 修正注记实证）+ 本计划回收 md/notify → 全部回收完成）+ 回收进度注记。
  - Skill: none
- [x] Fix: md `findFirstByOrg` 签名翻转——`IErpMdAcctSchemaBiz.java:17` + `ErpMdAcctSchemaBizModel.java:28` 的 `Long orgId` → `String orgId`（orgId FK 语义参数终态 String；xA 层语义为 eq 匹配、String 直传与 md orgId String 列一致）+ 5 处桥退役（mfg/sal/pur StockMoveBuilder 的 `ConvertHelper.toLong(orgId)` 与桥注释移除，String 直传）+ `rg 'findFirstByOrg'` 全仓调用点复扫（main + test，编译器驱动 + grep 双定位，test 侧 Long 字面量同步 String 化）。
  - Skill: `nop-backend-dev`（IBiz/BizModel 签名翻转跨实体调用规则）
- [x] Proof: md/mfg/sal/pur service 测试复跑绿（`mvn test -pl module-master-data/erp-md-service,module-manufacturing/erp-mfg-service,module-sales/erp-sal-service,module-purchase/erp-pur-service`；参照基线 md 155 / mfg 289 / sal 309 / pur 334）。
  - Skill: `nop-testing`
- [x] Decision: self-wait delta 平台修复状态复核——只读检查 nop-entropy 修复是否落地（bug doc 登记的移除触发条件）；未落地 → 保持 delta + bug doc 补「M4.1 已复核」注记 + 移除触发条件维持；已落地 → 移除 delta 并复跑受影响域测试。选择、替代方案与风险落盘本计划。
  - Skill: none
- [x] Proof: 6 处 fin json5 快照数字 id 残留处置——先查明并存原因（JsonMatchHelper 比对语义 vs 输出面未参与比较），再对齐 String 形态（重录 force-save-output 或手工对齐，以查明结论为准）+ `mvn test -pl module-finance/erp-fin-service` CHECKING 复跑绿（497 基线）+ 全量 `_cases` json5 数字 id grep = 0。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 3 个 web 模块重建 BUILD SUCCESS + 4 处 `:Long` 清零（全仓 page.yaml 复扫仅余合法非 id）
- [x] md/notify service 测试复跑全绿 + 2 处 delta HAS-EXTENDS 复扫通过
- [x] md/mfg/sal/pur service 测试复跑全绿 + `findFirstByOrg` 全仓调用点清零复核（Long 桥零残留）
- [x] 两份 bug doc 状态/裁决落盘；fin CHECKING 复跑绿 + 全量 json5 快照数字 id grep = 0

#### Phase 1 Decision 记录：self-wait delta 平台修复状态复核（2026-08-23）

- **第一环（nopSequenceGenerator）平台修复已落地**：上游 nop-entropy commit `d2c8e7ed42`（2026-08-22「sequenceGenerator ref 加 ignore-depends 断开声明环」），live 源码 `nop-persistence/nop-orm/src/main/resources/_vfs/nop/orm/beans/orm-defaults.beans.xml:42` 实证 `ioc:ignore-depends="true"`。
- **第二环（nopDataAuthChecker → nopDaoProvider → nopOrmTemplate → sessionFactory）平台未修复**：同文件 `nopDaoProvider`（:56-59）ctor-arg ref `nopOrmTemplate` 仍无豁免；该文件最后一次变更即第一环修复。且本计划 md/notify 执行期实证第二环仍可触发（见 Phase 1 状态注记）。
- **选择：保持全部兼容层 delta（首环 ×9 域 + 第二环 ×9 域，md/notify 第二 delta 本计划补位）**，移除触发条件维持登记（平台落地 nopDaoProvider/ormTemplate 豁免后一次性回收）。
- **替代方案（被否决）**：仅移除首环 delta（平台已修复使其冗余）+ 9 域复跑。否决理由：验证成本 = 9 域全量测试复跑，兑付收益 = 零行为变化（delta 形态无害），且与第二环 delta 同生共管拆分移除徒增回归风险。
- **风险**：平台未来落地第二环修复后需一次性回收 18 个 delta 文件（bug doc successor 登记维持）；在此外部事件前 delta 为 test-scope 兼容层，生产零影响。

### Phase 2 - 全量构建恢复

Status: completed（2026-08-23 执行：`mvn clean install -DskipTests` 全仓 **156 reactor 模块 BUILD SUCCESS（02:11 min，0 FAILURE / 0 SKIPPED）**——模块数与 08-20 基线 156 一致零漂移，mission 后首次全量构建恢复；破坏修复项零触发（D3 域级 no-am 口径设计预期兑现：19 域全迁移后域间破坏随重生成自愈，全量构建零修复通过）。app-erp-all `reflect-config.json` 裁决注记：hr plan `2026-08-22-0731-1` 登记 successor=M4.1 的全量重建义务——类名未变仅字段类型 Long→String 变化，反射注册按类/字段名匹配不受类型变化影响；本 Phase 全量重建 BUILD SUCCESS + Phase 3 全量测试绿（含 app-erp-all seed/auth/meta/flux demo 全套）即兑付载体，reflect-config 无需内容变更）
Targets: 156 reactor 模块（全仓）
Skill: `nop-debugging`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（资产清扫先行，全量构建须包含清扫后源码）

- [x] Proof: `mvn clean install -DskipTests` 156 reactor 模块 BUILD SUCCESS（mission 后首次全量；模块数与 08-20 基线一致性核对）+ app-erp-all `reflect-config.json` 裁决注记（hr plan 登记 successor=M4.1：类名未变仅字段类型变化，反射注册不受影响——全量重建 + Phase 3 测试绿即兑付）。
  - Skill: none
- [x] Fix: 全量构建破坏修复（若有）——预期为零（D3 设计：域间破坏随 19 域全迁移自愈）；出现破坏时按编译器驱动修复 + 逐点登记（不触发 roadmap rule-6 停止——该规则适用 M1-M3 域迁移期，M4.1 即兜底载体）。
  - Skill: `nop-debugging`

Exit Criteria:

- [x] 156 模块 BUILD SUCCESS（模块数如与基线有已登记差异，按实际 reactor 数落盘说明）

### Phase 3 - 全量测试 + successor 义务兑付 + 基线登记

Status: completed（2026-08-23 执行：全 reactor `mvn test` **BUILD SUCCESS（12:44 min）——surefire XML 权威计数 3808 tests / 0 failures / 0 errors / 1 skipped（617 报告文件）**，对照 08-20 基线 3789/0/0/1（614 文件）差量 **+19 tests / +3 报告文件，逐项归因（git log --diff-filter=A 考古 + 今日 surefire 计数）**：① `TestErpInvLandedCostAllocatedGuard`（inv，3 tests）——RC-R1.47 plan `2026-08-20-2052` commit `22197e930` 2026-08-20 23:11 落库，晚于 08-20 12:55 MV 基线运行时点故未计入 3789；② `TestSeqStringIdProof`（module-common-test，4 tests）——M0.1 plan `2026-08-21-1045` commit `25dce4cba`；③ `TestErpOrgContext`（common-service，12 tests）——M1.3 plan `2026-08-21-1045` commit `ff9af1b73`；3+4+12=19 全额解释、零移除（mission M1-M3 为存量测试 String 化改写非新增）；唯一 skipped = `ErpAllWebPagesCollectTest` @Disabled 预存 JDK26/ANTLR H-2 口径维持；执行期修复 2 处**日期漂移预存缺陷**（非 id 缺陷，08-22 录制快照 today 派生单元格跨日即红，按 ct SIGN_DATE/mfg delVersion `*` 通配先例）：fin `TestErpFinBankReconAutoReverseJob.testCurrentMonthReconNotReversed`（RECONCILIATION_DATE/STATEMENT_DATE/TRANSACTION_DATE ×3 单元格，fin 497/497 复跑绿）+ qa `TestErpQaSpcCapability`/`TestErpQaSpcSampling`（RISK_DATE/PERIOD_FROM/PERIOD_TO/INSPECTION_DATE ×127 单元格/8 文件，qa 182/182 复跑绿）；`mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` **1/1 绿（validateAllPages 0 errors，22.3s）**——19 域 `Erp*WebPagesTest` 治理排除 successor=M4.1 义务兑付（含本计划修复的 4 个 page.yaml 结构校验）；known-good-baselines 新增「full mvn build + test」基线条目 + `docs/logs/2026/08-23.md` 日志条目（含验证状态段））
Targets: 全 reactor 测试 + `app-erp-all` + `docs/testing/known-good-baselines.md` + `docs/logs/2026/08-23.md`
Skill: `nop-debugging`（失败先诊断）

- Item Types: `Fix | Proof | Add`
- Prereqs: Phase 2

- [x] Proof: `mvn test` 全 reactor 0 failures / 0 errors；surefire XML 权威计数落盘（对照 08-20 基线 3789/0/0/1 逐项解释差量——mission 期间新增/移除测试的来源逐条注明）。
  - Skill: none
- [x] Proof: `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` validateAllPages 0 errors——19 域 `Erp*WebPagesTest` 治理排除 successor=M4.1 义务兑付（含 plan 1 修复的 4 个 page.yaml 结构校验）。
  - Skill: none
- [x] Fix: 全量测试失败修复与分流——id 迁移引发 = 本计划修复；预存非 id 缺陷 = `nop-debugging` 诊断 + bug 登记（已确认缺陷不得降级为 follow-up，修复或显式登记裁决）。
  - Skill: `nop-debugging`
- [x] Add: known-good-baselines 新增「full mvn build + test」条目（日期/git state/命令/计数/known failures 口径）+ `docs/logs/2026/08-23.md` 日志条目（含验证状态段）。
  - Skill: none

Exit Criteria:

- [x] 全 reactor test SUCCESS + 计数与 skipped 口径落盘（唯一 skipped = ErpAllWebPagesCollectTest 预存）
- [x] ErpAllWebPagesTest validateAllPages 0 errors
- [x] 基线条目 + 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_fd4cbd6fcffe1g2X4HVI6AjRG0，fresh session，治理+技术双视角）——1 BLOCKER / 0 MAJOR / 3 MINOR。**B-1**：遗漏已登记 successor=M4.1 义务——md `findFirstByOrg(Long)` 签名残留 + 5 处 `ConvertHelper.toLong(orgId)` 语义参数桥（pur/sal/mfg plan Deferred 登记「另案/M4.1 兜底」，M4.1 为唯一剩余载体）→ 已修订：Baseline 补第 5 项盘点（file:line）+ Phase 1 新增 Fix（IBiz/BizModel 签名翻转 + 5 桥退役 + 全仓调用点复扫，Skill: nop-backend-dev）+ Proof（md/mfg/sal/pur service 测试复跑，基线 155/289/309/334）+ Exit Criteria 增列。**M-1**：fin 快照残留计数口径（6 文件 8 命中点，含 `currencyId: 2` ×3）→ 已修订 Baseline/Goals/Phase 1 措辞。**M-2**：app-erp-all reflect-config.json（hr plan 登记 successor=M4.1）→ 已修订 Phase 2 裁决注记。**M-3**：ct 修正出处措辞（b2b/ct 均按 M2.5 先例）+ Decision 项脱落引用排版 → 已修订。Baseline 六项抽检全部证实（4 page.yaml 行号/delta HAS-EXTENDS 分布/fin _cases/ErpAllWebPagesTest/08-20 基线行/bug doc 四域声称 vs live 两域）。
- Independent draft review iteration 2: `acceptable as-is`（ses_fd4bf86b9ffe0igC8sVeUlIJcv，fresh session）——iteration 1 全部 4 项发现核验解决（B-1 经 live 逐行核证：全仓 `findFirstByOrg` 恰 7 调用点与计划清单零偏差）；无 BLOCKER/MAJOR 新发现；1 新 MINOR（Phase 1 Targets 行未随 B-1 更新 Java 面）+ 1 nit（fin 快照计数措辞）已当场修订。**共识达成，Plan Status → active。**

## Closure Gates

> 本计划交付物即全仓验证本身（全量构建 + 全量测试）；Phase 2/3 已逐项覆盖，此处汇总口径与证据要求。

- [x] 范围内行为完成（4 page.yaml + 2 delta（+ 执行期必需的第二环 delta ×2，见 Phase 1 状态注记与 bug doc 复核段）+ `findFirstByOrg` 翻转与 5 桥退役 + 6 快照 + 2 bug doc + self-wait 裁决 + ErpAllWebPagesTest/reflect-config 兑付）
- [x] 相关文档对齐（bug docs ×2 + known-good-baselines 2026-08-23 条目 + `docs/logs/2026/08-23.md` 日志；roadmap M4.1 状态保持 `todo` 至批内序 3 终态更新——本计划未提前标 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（全 reactor 3808/0/0/1）+ `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest`（0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 三项均为计划起草期已裁决口径，非执行期降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 2 共识）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（三 Phase Status: completed + 全 checklist [x] + 基线/日志/bug doc 交叉引用一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（Phase 状态注记 + known-good-baselines + 日志 + surefire 报告）

## Deferred But Adjudicated

### ErpAllWebPagesCollectTest @Disabled（预存 known failure）

- Classification: `watch-only residual`
- Why Not Blocking Closure: JDK26/ANTLR H-2 平台兼容问题（2026-07-20 登记），与 id 迁移无关；重新启用条件 A/B/C 已在 known-good-baselines Known Failures 登记。
- Successor Required: `no`（known failure 登记载体）

### nopSequenceGenerator self-wait delta 全量移除（若 Phase 1 复核证实平台未修复）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 平台（nop-entropy）修复为外部事件，delta 为 test-scope 兼容层且各域测试全绿实证无害；M4.1 义务 = 复核 + 记录（roadmap successor 登记「平台修复后统一移除，M4.1 复核」字面兑付）。
- Successor Required: `yes`（触发条件：nop-entropy 平台修复落地后的任意触碰计划）

### 4 处 page.yaml 修复的运行时行为级验证

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本计划验证到结构级（web 模块重建 + ErpAllWebPagesTest validateAllPages）；adaptor 行为级（KPI/列表非空、markRead 生效）由批内序 2 E2E 运行时复核。
- Successor Required: `yes`（批内序 2 plan Phase 2 显式复核项）

## Closure

Status Note: 执行完成（2026-08-23，单次运行全量执行三 Phase）。全部范围内行为落地：4 page.yaml String 化 + md/notify 双 delta（首环 extends 修正 + 执行期必需的第二环补位）+ findFirstByOrg 翻转与 5 桥退役 + fin 6 快照 String 对齐（并存原因查明 = JsonMatchHelper 宽容语义）+ 2 bug doc 更正/复核落盘 + self-wait 平台修复裁决（保持 delta，理由与风险落盘）+ 全量构建 156 模块 + 全量测试 3808/0/0/1（+19 差量全额归因）+ ErpAllWebPagesTest 0 errors + 基线/日志登记。执行期附带修复 2 处预存日期漂移快照（fin ×3 / qa ×127 单元格，`*` 通配先例）。roadmap M4.1 保持 `todo` 至批内序 3（计划门控明确禁止本计划提前标 done）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session `ses_fd496ec73ffeYmmm2JsnTNegCg`，general agent，2026-08-23）
- Evidence: **ACCEPT（可关闭）**——9 组关键声明 live repo 逐项实证全 PASS：(a) page.yaml `:Long` 全仓 0 命中 + 4 处 `:String`/`|| null`/`!== ''` 守卫实证；(b) md/notify 首环 delta `x:extends="super"` + `xmlns:feature` + 九域 HAS-EXTENDS 复扫全在位；(c) md/notify 第二环 delta 与 hr 先例形态一致；(d) findFirstByOrg 恰 7 处全 String + `ConvertHelper.toLong(orgId)` 0 命中；(e) `_cases` json5 数字 id 0 命中；(f) 两份 bug doc 状态/复核段落完整；(g) surefire 617 XML 求和 3808/0/0/1 与计划声明零偏差 + 唯一 skipped = ErpAllWebPagesCollectTest + ErpAllWebPagesTest 1/1 绿 + 全部报告 2026-08-23 当天生成；(h) 基线/日志条目在位含 +19 全额归因；(i) 三 Phase completed + checklist 全 [x] + roadmap M4.1 保持 todo（门控正确）。范围降级检查 PASS（Deferred 三项均起草期裁决；执行期新增处置三重落盘非降级）。2 MINOR 不阻塞（Evidence 占位符设计如此——本条即补填；extends 计数口径说明）。

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。）
