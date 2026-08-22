# 2026-08-22-2311-2-bigint-id-m37-drp-migration 主键/外键 string 化 M3.7：drp 域迁移（冻结序位次 18）

> Plan Status: active（2026-08-22：独立草案审查共识达成——技术侧 + 治理侧 iteration 1 双 `acceptable as-is`/`accept` + 双 `approve`，全部 MINOR 已就地修正并记录；保护区域双独立子 agent 批准已落盘 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.7（drp，冻结序位次 18）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 18（M3.7）
> Related: `docs/plans/2026-08-22-2311-1-bigint-id-m34-crm-migration.md`（批内序 1，冻结总序先于本域执行；非编译硬前置）、`docs/plans/2026-08-22-1814-1-bigint-id-m25-purchase-migration.md`（bridge-main-090/091 落桥来源 + drp 破坏登记来源）、`docs/plans/2026-08-22-1814-2-bigint-id-m26-sales-migration.md`（backward-151/210 登记来源 + drp-service 终态复测来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **冻结序位置**：位次 18；精确前置 M2.5 + M2.6 + M2.7 + M2.3 + M2.2 + M2.1 + M2.4 + M1.1 + M1.2 + M3.6 全部 `done`（drp 为冻结序前置最广域，10 前置；crm 位次 17 非编译前置，但按冻结总序先行执行——本计划批内序 2）。
- **工具现状（2026-08-22 scan）**：`module-drp/model/app-erp-drp.orm.xml` **55 列 NEEDS FIX**（PK 17 + FK 38；其中 notGenCode stub 6 列——inv 1（`ErpInvStockMove`，M2.2 登记「drp orm 内 inv stub 列为位次 18 域自有列，successor M3.7」随本域翻转）+ md 5（`ErpMdLocation`/`ErpMdMaterial`/`ErpMdOrganization`/`ErpMdPartner`/`ErpMdWarehouse`），stub 实体全部属已迁移域（md M1.1/inv M2.2），随本域翻转、与权威源对齐（M1.1 先例；分解以 Phase 1 dry-run 为准）），0 DEFERRED。
- **登记册义务（`tools/id-migration-registry.json5` + 审计文档 §6.18，Phase 1 强制全量复核）**：
  - A1 延后列：0；**A2 main 前向桥：0；A3 test 前向桥：0**（本域不引用任何晚域——登记册 §6.18 A 节明示「无」，仅 logistics 晚于本域且登记册零 refDomain=drp 的 logistics 条目）。
  - **B main 桥接退役：2 条**（早域 purchase）：bridge-main-090/091（`ErpPurReceiveProcessor` ConvertHelper.toLong 桥 + drpMaterialIds 循环桥——M2.5 已落桥 + 双向注释，pur 334/334 绿；本域翻转 IBiz 参数签名后桥接点/注释移除 + String 直传）。
  - **B retired-test drp 半边回收：0**（登记册零 refDomain=drp 的 bridge-test 条目——实证复查于 Phase 1）。
  - C1 后向 main 6 条（backward-146 inv 5 文件 / 147 mfg 1 文件 / 148 md 3 文件 / 149 pur 3 文件 / 150 qa 2 文件 / 151 sal 1 文件；去重后 6 个手写文件：`DrpDemandAggregator`/`DrpReleaseService`/`ErpDrpCrossDockStagingTimeoutJob`/`ErpInvDrpCrossDockProcessor`/`ErpInvDrpLeadTimeProcessor`/`SafetyStockEngine`）；C2 后向 test 6 条（backward-205 inv 9 文件 / 206 mfg 1 / 207 md 9 / 208 pur 3 / 209 qa 2 / 210 sal 1；去重后 9 个测试类）——全部指向已迁移域。
  - 被引用面（未迁移域 → 登记中间态）：**零**（logistics orm 无 drp stub、登记册除 090/091 main 桥（B 义务）外零 logistics 的 refDomain=drp service/test 条目——本域翻转不新增任何下游破坏）。
- **继承中间态（M2.6 Phase 2 实测登记，2026-08-22；M2.5 早期 78 错/5 文件口径已被 M2.6 翻转后复测取代）**：drp-dao 58 javac 错 100% `_gen`（md 关系胶水对称耦合，D3 已登记）——本域翻转 + 重生成自愈；drp-service 错误仅 `DrpDemandAggregator` + `DrpReleaseService` 两文件且 import 面仅 inv/pur（零 sal import）——由 Phase 2 编译器驱动手写修复愈合（非重生成自愈）。
- **findFirstByOrg 消费点：零**（2026-08-22 grep 实证；无 md 语义参数桥义务）。
- **page.yaml**：drp-web 手写面 `:Long` **1 处**（`dashboard/net-requirement:38` `$pid`）——Phase 4 就地 String 化（mfg CRP / M2.6 three-way-match 先例）+ YAML 良构校验 + drp-web 重建绿。
- **代码与测试规模**：drp-service main 手写修复面以 C1 全清单 6 文件为定位面（编译器可发现更多本域 Long id 面：drp engine/scenario/plan 族 BizModel 与 IBiz）；test 14 个 `Test*.java`（C2 登记 9 个）；快照 `_cases/` 现值 **518** 文件（重录基线）。
- **早域复跑基线（B 义务验证）**：pur 334/334（B main 2 条）。
- **owner doc**：`docs/design/drp/` grep 复核零 Long id/BIGINT 陈述（2026-08-22 实证；Phase 4 复核确认）。

## Goals

- 55 列 `stdDataType long→string` 落源（stdSqlType 保持 BIGINT，DDL 零变化），三重证明（新鲜度门控 + git diff + 工具重扫 drp 段 0 NEEDS FIX）。
- drp 7 模块链 no-am main 绿 + 域级测试全绿（`mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web`）；继承中间态（drp-dao `_gen` + drp-service 两文件前向边破坏）全数自愈。
- B main 退役 2 条兑付（pur 侧桥接点/注释移除 + String 直传 + pur 7 链重建绿 + grep 桥残留清零 + pur 测试基线复跑维持）。
- 快照 RECORDING→CHECKING 每域重录（force-save-output 系统属性模式，mfg/sal 先例）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 1 处就地 String 化 + owner doc 复核 + 登记册状态更新 + 路线图 M3.7 → done + 日志。

## Non-Goals

- 不迁移 logistics（冻结序位次 19，successor plan）；本域翻转预期零下游破坏（登记册实证），若发现未登记 logistics 破坏按 rule-6/D4 停止回报。
- 不改 `stdSqlType`/DDL/CSV 种子/序列号引擎；不动 `delVersion` 等非 PK/FK BIGINT 列（孤儿操作人列规则 4 保持 long）。
- 不跑全量构建/全量测试/E2E（M4.1 专属）；不修 M4.1 登记的存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）。
- 不翻转 md 侧 `findFirstByOrg(Long)` 签名（本域零消费点；pur/sal 先例登记例外不适用）。

## Task Route

- Type: `implementation-only change`（orm 模型变更 → 增量重生成 → 编译器驱动修复，路线图 M1-M3 标准结构）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（标准结构 + 规则 1-8 + 横切 §5 保护区域 design 证据链）、`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.18 + `tools/id-migration-registry.json5`（消费协议）、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`（M0 裁决与冻结序结论）、`docs/design/drp/`（业务 owner doc，Phase 4 复核对象）
- Skill Selection Basis: 域迁移 plan 预期技能 = `nop-backend-dev`（BizModel/IBiz 手写修复 + 跨实体调用规则）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；orm 回写机制走 M0.1 裁定工具链，不经技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具链：`node tools/check-bigint-id-types.mjs dry-run` + `node tools/verify-id-fix-copy-diff.mjs module-drp` + `node tools/scan-cross-domain-id-coupling.mjs`；回写一律走「时点 dry-run + 新鲜度门控」三步，禁止盲 cp/apply 模式。
- 硬前置 = 最后全绿基线 commit 的全量 install + 每个已完成域链 install 已在本地 Maven 仓库（D3 修订口径「每个已完成域链」；drp 位次 18 前的 17 域链全部 install）。
- 回滚策略（mfg/qa/sal 先例）：orm 落源后回滚 = `git revert` orm 变更 + 7 模块链增量重生成；Phase 2/3 完成后回滚需先 revert pur 桥接退役与测试代码变更，再 revert 本域 orm + 重生成。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-drp/model/app-erp-drp.orm.xml`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 精确前置 M2.5 + M2.6 + M2.7 + M2.3 + M2.2 + M2.1 + M2.4 + M1.1 + M1.2 + M3.6 全部 done（roadmap 规则 1 readiness 口径）；批内序 2（crm 位次 17 按冻结总序先行，非本域编译硬前置）

- [ ] Proof: 登记册全量消费——`registry §6.18 + json5` 逐条核对与 Baseline 一致（A1=0 / A2=0 / A3=0 / B main 2 / retired-test drp 半边零实证 / C1 146-151 / C2 205-210 / 被引用面零）；差异即补登落册。
      - Skill: none
- [ ] Proof: FQN 复扫（b2b A3' 盲区先例：java 内联 FQN 非 import 行 + `*.beans.xml` ioc:type 双口径）——drp 引用面与被引用面双向补登核对，预期仅命中已登记条目。
      - Skill: none
- [ ] Proof: 双独立子 agent 批准（治理 + 技术，fresh session）落盘 Draft Review Record（保护区域 `model/*.orm.xml` 要求）。
      - Skill: none
- [ ] Fix: orm 落源三步——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-drp` 新鲜度门控（55 行 stdDataType-only、零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核（stdSqlType 全保持 BIGINT、非 PK/FK 列零变化、inv stub 1 + md stub 5 列与权威源一致）。
      - Skill: none

Exit Criteria:

- [ ] 工具重扫 drp 段 0 NEEDS FIX / 0 DEFERRED（总待改 213 − 126 crm（批内序 1 先行前提）− 55 本域 = logistics 32 余量），三重证明落盘。
- [ ] 登记册消费零未解释差异。

### Phase 2 - 增量重生成 + 主代码编译修复 + B main 退役 + 早域链重建

Status: planned
Targets: `module-drp/erp-drp-{codegen,dao,meta,service,web,app,api}`、pur 桥接点文件
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 完成

- [ ] Fix: 7 模块链 no-am 重生成构建 main 绿（`-pl` 显式列表 + `-Dmaven.test.skip=true`）；编译器驱动修复手写面（drp-dao IBiz/值对象 id 参数族 + service C1 全清单 6 文件为定位面：`DrpDemandAggregator`/`DrpReleaseService`/`ErpDrpCrossDockStagingTimeoutJob`/`ErpInvDrpCrossDockProcessor`/`ErpInvDrpLeadTimeProcessor`/`SafetyStockEngine`；drp-dao 58 `_gen` 错误随重生成自愈复核 + drp-service 两文件继承破坏随编译器驱动手写修复愈合复核）。
      - Skill: `nop-backend-dev`
- [ ] Fix: B main 2 条退役兑付——pur 侧 `ErpPurReceiveProcessor` ConvertHelper.toLong 桥 + drpMaterialIds 循环桥移除、String 直传 + 双向 bridge 注释删除 + ConvertHelper import 清零复核，pur grep `bridge-main-09[01]` 引用清零。
      - Skill: `nop-backend-dev`
- [ ] Proof: pur 7 模块链 no-am 重建 BUILD SUCCESS。
      - Skill: none

Exit Criteria:

- [ ] drp 7 链 main 绿；pur 7 链重建绿。
- [ ] 下游破坏核证：logistics 链零新增破坏（证据源 = Phase 1 FQN 双向复扫被引用面零 + logistics orm 无 drp stub + `rg 'app\.erp\.drp\.' module-logistics` 零命中机械复核；出现未登记破坏即 rule-6 停止回报）。

### Phase 3 - 测试修复 + 快照重录 + 域级测试 + 早域复跑

Status: planned
Targets: `module-drp/erp-drp-service/src/test`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [ ] Fix: 测试修复（14 个 `Test*.java` 编译器驱动清零 + 语义陷阱：id 序比较 → `ConvertHelper.toLong` 数值序（contract idOrder 先例）、装箱 `==` → `.equals()`、算术派生 id 数值保真、种子常量 String 字面量数字保真、`orm_propValueByName("id", String)` 形态）。
      - Skill: `nop-testing`
- [ ] Proof: 快照重录 RECORDING→CHECKING（`-Dnop.autotest.force-save-output=true` 全局重录 + grep 零注解残留；`_cases/` 518 基线 → String 形态落盘；非确定性单元格 `*` 通配（hr/mfg 先例）；逐案审核 id String 实证 + 拒绝「断言式」类误录（cs 81 目录/sal 7 类超录回退先例））。
      - Skill: `nop-testing`
- [ ] Proof: 域级测试 `mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web` 全绿（web 0 tests 治理排除 `@Tag("full-app")`，successor M4.1）；平台 IoC 回归 self-wait 处置双分支证据——未复现零 delta（mfg/sal 先例）或复现按 fin 修正版先例 delta 落位（含 hr 第二环 nopDataAuthChecker daoProvider lazy-property 变体先例）。
      - Skill: `nop-testing`
- [ ] Proof: 早域复跑 pur 基线维持（334/334；快照形态漂移就地 String 化 + `*` 通配按先例处置）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] drp 域级测试全绿 + pur 基线维持全绿。
- [ ] 本域无 retired-test 半边义务（Phase 1 零实证复核）。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml String 化 + 收尾登记

Status: planned
Targets: drp-service main、drp-web 手写面、`docs/design/drp/`、登记册、路线图、日志
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 3 完成

- [ ] Fix: 语义陷阱 grep 门控清零（路线图横切 §3 清单：`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较；合法非 id 项逐条注明；sql-lib 零存在注明）。
      - Skill: none
- [ ] Fix: page.yaml `:Long` 1 处就地 String 化（net-requirement `$pid`；BizModel 签名一致性核证，mfg CRP / sal three-way-match 先例）+ YAML 良构校验 + drp-web 重建绿 + 手写 view 零被动变更核证（git status 非 `_gen` 变更 = 预期仅 page.yaml 主动 Fix）。
      - Skill: `nop-backend-dev`
- [ ] Proof: owner doc 复核——`docs/design/drp/` grep 复核 Long id 陈述（2026-08-22 基线零命中，Phase 4 复核确认；命中即就地注记引用本计划）。
      - Skill: none
- [ ] Add: 登记册状态更新（B main 2 条 → retired 附逐条兑付 note + fail-closed 解析验证）+ 路线图 M3.7 → done（M2/M3 表位次 18 证据摘要 + 头部「最后更新」续链 + 位次 19 解锁）+ 日志 `docs/logs/2026/{执行当日}.md`（rule 8 日期口径，含验证状态 + B 义务兑付 + pur 复跑基线）。
      - Skill: none

Exit Criteria:

- [ ] grep 门控清零（登记例外除外）+ page.yaml `:Long` 清零。
- [ ] 登记册/路线图/日志三处一致落盘。

## Draft Review Record

- Independent draft review iteration 1（技术/执行契约视角，fresh session，ses_fd5f6fea7ffeTT78V27peu1PnH）: `acceptable as-is` + 保护区域技术侧 `approve`——14 项事实验证全对（55 列/PK 17+FK 38/stub 6 含 inv stub 1/090/091 active+M2.5 落桥核证/零 bridge-test refDomain=drp/C1 6 文件+C2 9 类/14 测试/_cases 518/page.yaml 1 处/findFirstByOrg 0/继承中间态双源核证/owner doc 零命中/logistics 零 drp 引用/334/334）；5 MINOR（M2.6 归属 / 「自愈全部」措辞 / 被引用面字面冲突 / logistics 核证缺机械命令 / 批内序算术前提）已全部修正（继承中间态改 M2.6 单源 + 自愈/手写修复分述 / 被引用面加 090/091 限定 / Phase 2 证据源补 `rg` 机械复核 / Phase 1 退出口径补 213−126−55 算式与批内序前提）。
- Independent draft review iteration 1（治理/规范视角，fresh session，ses_fd5f6e3a4ffeTJR34PhIaf8ikr）: `accept` + 保护区域治理侧 `approve`——模板/命名/状态生命周期/Item typing/Skill/反松弛/保护区域证据链/Closure Gates 域级口径/路线图规则/文本一致性/回滚策略/Prereqs 11 项全 PASS；2 MINOR（时间戳溯源——批重命名 2311 已修复 / 批内序算术括号——已补）已修正。
- **共识达成（2026-08-22）**：技术侧（ses_fd5f6fea7ffeTT78V27peu1PnH）+ 治理侧（ses_fd5f6e3a4ffeTJR34PhIaf8ikr）iteration 1 双 approve、双独立子 agent 批准完成，5+2 MINOR 已全部就地修正并记录于上，转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（55 列落源 + 7 链绿 + B/C 兑付 + 快照重录 + 测试绿）
- [ ] 相关文档对齐（登记册 + 路线图 + owner doc + 日志）
- [ ] 已运行验证：`mvn clean install -pl module-drp/erp-drp-{codegen,dao,meta,service,web,app,api} -DskipTests`（no-am）BUILD SUCCESS + `mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web` 全绿 + pur 复跑基线（334/334）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（保护区域双批准落盘）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### logistics 下游破坏（预期零）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册实证零被引用面（logistics orm 无 drp stub、零 service/test 条目）；若 Phase 2 实测出现未登记破坏按 rule-6/D4 停止回报，不静默登记
- Successor Required: no（预期不存在；实测存在时升级为 M0 裁决）

### 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.1 专项全量清扫已登记（M3.6 结束审计发现的模式盲区）；本域 1 处属本计划范围就地修复
- Successor Required: yes（M4.1）

### web 页面测试治理排除（`@Tag("full-app")`）

- Classification: `watch-only residual`
- Why Not Blocking Closure: M1.1 已提交治理决策（surefire excludedGroups），实证依赖全量 classpath
- Successor Required: yes（M4.1 `ErpAllWebPagesTest` 兜底核证）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（已确认缺陷不得出现在此处）
