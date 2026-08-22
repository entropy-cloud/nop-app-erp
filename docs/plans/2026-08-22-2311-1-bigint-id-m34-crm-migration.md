# 2026-08-22-2311-1-bigint-id-m34-crm-migration 主键/外键 string 化 M3.4：crm 域迁移（冻结序位次 17）

> Plan Status: completed（2026-08-23：四 Phase 全部完成——Phase 1/2 前次运行（orm 落源 126 列三重证明 + 双批准 + 7 链重生成 main 绿 + B main 4 条退役 + cs 链重建），Phase 3/4 本次执行（29 测试类 String 化 + retired crm 半边回收 113/115/116/136 + 快照重录 767→884 + crm 188/188 绿 + cs 185/185 复跑维持 + grep 门控清零 + page.yaml 7 处 + owner doc/登记册/路线图/日志收尾）；结束审计已由独立子代理 CLOSURE_VERIFY 通过（2026-08-23，见 Closure Audit Evidence））
> Mission: id-string-migration
> Work Item: M3.4（crm，冻结序位次 17）
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 17（M3.4）
> Related: `docs/plans/2026-08-22-1814-2-bigint-id-m26-sales-migration.md`（crm-dao 76 错中间态 + backward-143/202 登记来源）、`docs/plans/2026-08-22-0002-3-bigint-id-m35-cs-migration.md`（bridge-main-053..056 + bridge-test-113/115/116 crm 半边登记来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **冻结序位置**：位次 17；精确前置 M2.6 + M2.3 + M2.2 + M2.1 + M1.1 + M1.2 + M3.6 全部 `done`（本计划批内序 1，位次 18/19 按冻结总序后行）。
- **工具现状（2026-08-22 scan）**：`module-crm/model/app-erp-crm.orm.xml` **126 列 NEEDS FIX**（PK 40 + FK 86；其中 notGenCode stub 6 列——5 个 md stub 实体（`ErpMdCurrency`/`ErpMdMaterial`/`ErpMdOrganization` ×2 含 parentId FK/`ErpMdPartner`/`ErpMdPartnerContact`），stub 实体全部属已迁移域 md（M1.1），随本域翻转、与权威源对齐（M1.1 先例；分解以 Phase 1 dry-run 为准）），0 DEFERRED。
- **登记册义务（`tools/id-migration-registry.json5` + 审计文档 §6.17，Phase 1 强制全量复核）**：
  - A1 延后列：0；**A2 main 前向桥：0；A3 test 前向桥：0**（本域不引用任何晚域——登记册 §6.17 A 节明示「无」，crm 为冻结序位次 17，仅 drp/logistics 晚于本域且登记册零 refDomain=crm 的 drp/logistics 条目）。
  - **B main 桥接退役：4 条**（全部早域 cs，`TicketAssignResolver`）：bridge-main-053/054（import `ErpCrmTeam`/`ErpCrmTeamMember` 类型级引用）+ bridge-main-055/056（`IErpCrmTeamBiz.findList`/`IErpCrmTeamMemberBiz.findList`——M3.5 cs plan 已核证零 id 穿越，预期以签名对齐核证为主、大概率零代码变更）。
  - **B retired-test crm 半边回收（候选招单，Phase 1 逐条对 registry retired note 重验）**：bridge-test-113（`TestErpCsCatalogFulfillmentEngine:72` `CRM_TEAM_ID` Long 常量 + `seedMember(Long,Long)` crm 种子局部桥）、bridge-test-115（`TestErpCsTicketCreateEnrichment:75` `seedMemberInSession(Long,Long)` 局部桥）——note 明示「晚域 M3.4 翻转时移除局部桥」；bridge-test-116（`TestMockCrmBizModels` Long 桩签名）+ bridge-test-136（`app-test-mock-crm.beans.xml` ioc:type FQN——与 116 同 crm mock 基建的 A3' 补登条目，refDomain=crm）——note 明示「晚域 M3.4 翻转时随 mock 一并回收」（M3.2 mock 桩 String 化先例）。bridge-test-114/117/135 为 qa 面（refDomain=quality），不属本域义务。
  - C1 后向 main 3 条（backward-141 md 5 文件 / 142 notify 2 文件 / 143 sal 8 文件；含 **crm-dao 3 手写文件**（`IErpCrmLeadBiz`/`IErpCrmConversionBiz`/`IErpCrmProductConfiguratorBiz`——M0 审计登记的唯一 dao 手写跨域实体 import 簇，随本域编译器驱动翻转））；C2 后向 test 2 条（backward-201 md 3 文件 / 202 sal 2 文件，去重后 3 个测试类）——全部指向已迁移域。
  - 被引用面（未迁移域 → 登记中间态）：**零**（drp/logistics orm 无 crm stub、登记册零 refDomain=crm 的 service/test 条目——本域翻转不新增任何下游破坏；M2.6 被引用面为 crm→sal 方向，属本域 C1 消费面）。
- **继承中间态（M2.6 Phase 2 实测登记，2026-08-22）**：crm-dao 76 javac 错 100% `_gen`（md/sal 关系胶水对称耦合，D3 已登记）；crm-service main 编译绿（backward-143 8 文件类型级零编译破坏）。本域翻转 + 重生成自愈 crm-dao `_gen` 破坏。
- **findFirstByOrg 消费点：零**（2026-08-22 grep 实证；无 md 语义参数桥义务）。
- **page.yaml**：crm-web 手写面 `:Long` **7 处/4 文件**（`ErpCrmActivity/timeline:38` `$lid` / `calendar:39` `$lid` / `lead-conversion/main:93` `$lid`+`$sid` / `ErpCrmLead/opportunity-kanban:49` `$sid` + `:97` `$leadId`+`$sid`）——Phase 4 就地 String 化（mfg CRP / M2.6 three-way-match adaptor 静默降级同型缺陷修复先例）+ YAML 良构校验 + crm-web 重建绿。
- **代码与测试规模**：crm-service main 手写修复面以 C1 全清单为准（crm-dao 3 手写文件 + service 8 文件：`ErpCrmLeadBizModel`/`ErpCrmProductConfiguratorBizModel` entity 2 + `ErpCrmConversionProcessor`/`ErpCrmConversionConvertToCustomerProcessor`/`ErpCrmConversionConvertToQuotationProcessor`/`ErpCrmProductConfiguratorGenerateQuoteProcessor` processor 4 + `ErpCrmEventReminderJob`/`ErpCrmSequenceOverdueJob` job 2；编译器可发现更多本域 Long id 面）；test 29 个 `Test*.java`（递归计）；快照 `_cases/` 现值 **767** 文件（重录基线）。
- **早域复跑基线（B 义务验证）**：cs 185/185（B main 4 条 + retired-test crm 半边）。
- **owner doc**：`docs/design/crm/`——`territory.md` 含 Long id/BIGINT 陈述（2026-08-22 grep），Phase 4 就地注记；其余文件零命中复核。

## Goals

- 126 列 `stdDataType long→string` 落源（stdSqlType 保持 BIGINT，DDL 零变化），三重证明（新鲜度门控 + git diff + 工具重扫 crm 段 0 NEEDS FIX）。
- crm 7 模块链 no-am main 绿 + 域级测试全绿（`mvn test -pl module-crm/erp-crm-service,module-crm/erp-crm-web`）。
- B main 退役 4 条兑付（cs 侧签名对齐核证/零代码变更核证 + cs 7 链重建绿 + grep 桥残留清零 + cs 测试基线复跑维持）；retired-test crm 半边回收（113/115 局部桥移除 + 116 mock 桩 String 化随 IBiz 翻转 + beans ioc:type 核证）。
- 快照 RECORDING→CHECKING 每域重录（force-save-output 系统属性模式，mfg/sal 先例）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 7 处就地 String 化 + owner doc 注记 + 登记册状态更新 + 路线图 M3.4 → done + 日志。

## Non-Goals

- 不迁移 drp/logistics（冻结序位次 18/19，successor 各自 plan）；本域翻转预期零下游破坏（登记册实证），若发现未登记 drp/logistics 破坏按 rule-6/D4 停止回报。
- 不改 `stdSqlType`/DDL/CSV 种子/序列号引擎；不动 `delVersion` 等非 PK/FK BIGINT 列（孤儿操作人列规则 4 保持 long）。
- 不跑全量构建/全量测试/E2E（M4.1 专属）；不修 M4.1 登记的存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）。
- 不翻转 md 侧 `findFirstByOrg(Long)` 签名（本域零消费点，无桥义务；pur/sal 先例登记例外不适用）。

## Task Route

- Type: `implementation-only change`（orm 模型变更 → 增量重生成 → 编译器驱动修复，路线图 M1-M3 标准结构）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（标准结构 + 规则 1-8 + 横切 §5 保护区域 design 证据链）、`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.17 + `tools/id-migration-registry.json5`（消费协议）、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`（M0 裁决与冻结序结论）、`docs/design/crm/`（业务 owner doc，Phase 4 注记对象）
- Skill Selection Basis: 域迁移 plan 预期技能 = `nop-backend-dev`（BizModel/IBiz 手写修复 + 跨实体调用规则）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；orm 回写机制走 M0.1 裁定工具链，不经技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具链：`node tools/check-bigint-id-types.mjs dry-run` + `node tools/verify-id-fix-copy-diff.mjs module-crm` + `node tools/scan-cross-domain-id-coupling.mjs`；回写一律走「时点 dry-run + 新鲜度门控」三步，禁止盲 cp/apply 模式。
- 硬前置 = 最后全绿基线 commit 的全量 install + 每个已完成域链 install 已在本地 Maven 仓库（D3 修订口径「每个已完成域链」；crm 位次 17 前的 16 域链全部 install）。
- 回滚策略（mfg/qa/sal 先例）：orm 落源后回滚 = `git revert` orm 变更 + 7 模块链增量重生成；Phase 2/3 完成后回滚需先 revert cs 桥接退役/测试半边回收与测试代码变更，再 revert 本域 orm + 重生成。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed
Targets: `module-crm/model/app-erp-crm.orm.xml`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 精确前置 M2.6 + M2.3 + M2.2 + M2.1 + M1.1 + M1.2 + M3.6 全部 done（roadmap 规则 1 readiness 口径）；批内序 1（本计划为批次首项）

- [x] Proof: 登记册全量消费——`registry §6.17 + json5` 逐条核对与 Baseline 一致（A1=0 / A2=0 / A3=0 / B main 4 / retired-test crm 半边候选 113/115/116+136 / C1 141-143 / C2 201-202 / 被引用面零）；差异即补登落册。
      - Skill: none
      - 执行证据（2026-08-22）：json5 逐条 grep 复核——domain=crm 的 orm-deferral 0 条、service-bridge main/test 0 条；refDomain=crm 恰 8 条 = bridge-main-053..056（active，retireOwner=M3.4）+ bridge-test-113/115/116/136（retired，note 均含「晚域 M3.4 翻转时…」crm 半边指针）；domain=crm backward-pointer 5 条 = 141（md 5 文件）/142（notify 2 文件）/143（sal 8 文件）/201（md 3 测试）/202（sal 2 测试）——与 Baseline 全等，零未解释差异。
- [x] Proof: FQN 复扫（b2b A3' 盲区先例：java 内联 FQN 非 import 行 + `*.beans.xml` ioc:type 双口径）——crm 引用面与被引用面双向补登核对，预期仅命中已登记条目。
      - Skill: none
      - 执行证据（2026-08-22）：`rg 'app\.erp\.crm\.'` 全仓（排除 module-crm）java 双口径命中恰 4 文件 = `TicketAssignResolver`（main，053..056）+ `TestErpCsCatalogFulfillmentEngine`/`TestErpCsTicketCreateEnrichment`/`TestMockCrmBizModels`（113/115/116）+ beans 命中恰 1 = `app-test-mock-crm.beans.xml`（136）；被引用面 `rg 'ErpCrm' module-drp module-logistics` 零命中（`ErpCsTicketScanOverdueTicketsProcessor:68` 为注释提及非代码引用）——零补登。
- [x] Proof: 双独立子 agent 批准（治理 + 技术，fresh session）落盘 Draft Review Record（保护区域 `model/*.orm.xml` 要求）。
      - Skill: none
      - 执行证据：已落盘本文件 Draft Review Record（技术侧 ses_fd5f748bbffeguuYD97aTdQTvI iteration 1 `approve` + 治理侧 ses_fd5edb06fffeEMcdR0Rqa8yIpQ iteration 2 `approve`，2026-08-22 共识达成），转 `active` 前完成，无需重录。
- [x] Fix: orm 落源三步——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-crm` 新鲜度门控（126 行 stdDataType-only、零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核（stdSqlType 全保持 BIGINT、非 PK/FK 列零变化、md stub 6 列与 md 权威源一致）。
      - Skill: none
      - 执行证据（2026-08-22）：① dry-run 汇总 213 列/3 文件；② 门控输出「module-crm 变更行 126 / 非法差异行 0 / 门控通过」；③ cp 落源 + git diff 126 insertions/126 deletions 全为 stdDataType long→string（新增/删除行 126/126 均含 `stdSqlType="BIGINT"`），md stub（ErpMdOrganization id+parentId / ErpMdPartner id / ErpMdPartnerContact id+partnerId / ErpMdCurrency id / ErpMdMaterial id）与 md 权威源 string 形态一致；残留 `stdDataType="long"` 35 处全为 delVersion 等 非 PK/FK 列。

Exit Criteria:

- [x] 工具重扫 crm 段 0 NEEDS FIX / 0 DEFERRED（总待改 213→87 = drp 55 + logistics 32，213 − 126 本域口径），三重证明落盘。
      - 执行证据：`scan` 汇总「实际修改 stdDataType 的列: 87（不含延后列）/ DEFERRED: 0」，crm 段全部 `ok`，NEEDS FIX 仅 drp/logistics 段（含 drp/logistics orm 内 md/inv stub，successor M3.7/M3.10 自有列口径）。
- [x] 登记册消费零未解释差异。

### Phase 2 - 增量重生成 + 主代码编译修复 + B main 退役 + 早域链重建

Status: completed（2026-08-23：crm 7 模块链 no-am `mvn clean install -Dmaven.test.skip=true` BUILD SUCCESS（前次运行已落 orm 重生成 `_gen` + dao 3 手写 IBiz + service 大部分手写面，本次续接编译器驱动清零余量：entity BizModel 6 文件 @Override 签名翻转（Quota/Event/LeadScore/Forecast/ForecastPeriod/ProductConfigurator）+ processor 11 文件签名对齐 + `EventTimelineAggregator` buildTimeline/loadEvents/loadActivities String 化 + dao 值对象 `ErpCrmTerritoryPipeline.territoryId` Long→String + `ErpCrmLeadCancelProcessor` Long.valueOf(id) 转换边界移除；继承 crm-dao 76 `_gen` 错误随重生成自愈实证——dao/meta 先行模块 BUILD SUCCESS）；B main 4 条退役兑付：`TicketAssignResolver` 053/054 类型级 import 自然对齐零代码变更核证 + 055/056 `eq("code")` VARCHAR / `eq("teamId", getId())` String↔String 直传核证（M3.5 已证零 id 穿越），bridge 注释 4 处移除 + cs grep `bridge-main-05[3-6]` 清零；cs 7 模块链 no-am 重建 BUILD SUCCESS；下游核证 `rg 'app\.erp\.crm\.|ErpCrm' module-drp module-logistics` 零命中 + drp/logistics orm 零 crm stub（与 Phase 1 证据源一致，零新增破坏）；登记册 4 条 → retired 附逐条兑付 note + fail-closed 解析验证（scan 汇总 87 待改 = drp 55 + logistics 32，crm 段零 NEEDS FIX））

Targets: `module-crm/erp-crm-{codegen,dao,meta,service,web,app,api}`、cs 桥接点文件
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 完成

- [x] Fix: 7 模块链 no-am 重生成构建 main 绿（`-pl` 显式列表 + `-Dmaven.test.skip=true`）；编译器驱动修复手写面（**crm-dao 3 手写 IBiz 翻转**（`IErpCrmLeadBiz`/`IErpCrmConversionBiz`/`IErpCrmProductConfiguratorBiz` id 参数族 + md/sal 实体 import 随 String jar 类型对齐，fin/ast/sal dao 手写先例对齐）+ service C1 全清单 8 文件为定位面；继承 crm-dao 76 `_gen` 错误随重生成自愈复核）。
      - Skill: `nop-backend-dev`
      - 执行证据（2026-08-23）：dao 3 手写 IBiz 前次运行已翻转（本计划 Phase 2 中断点续接），编译器驱动扩展清零至 BizModel 6 + processor 11 + support 3 + dao 值对象 1 = 21 文件余量（`ErpCrmQuotaBizModel` 16 错 / `ErpCrmEventBizModel` 14 错 / `ErpCrmLeadBizModel` 10 错涟漪全清）；crm 7 链 BUILD SUCCESS。
- [x] Fix: B main 4 条退役兑付——cs 侧 `TicketAssignResolver` 053/054 类型级 import 核证 + 055/056 `findList` 调用点签名对齐/String 直传核证（M3.5 已核证零 id 穿越，预期零或微小代码变更），cs grep `bridge-main-05[3-6]` 引用清零。
      - Skill: `nop-backend-dev`
      - 执行证据（2026-08-23）：零代码变更核证成立（String jar 下 import/调用点自然对齐）——仅移除 4 处 bridge 注释；`rg 'bridge-main-05[3-6]' module-cs/` 零命中；登记册 bridge-main-053..056 → retired 附逐条兑付 note。
- [x] Proof: cs 7 模块链 no-am 重建 BUILD SUCCESS。
      - Skill: none
      - 执行证据（2026-08-23）：`mvn clean install -pl module-cs/erp-cs-{codegen,dao,meta,service,web,app,api} -Dmaven.test.skip=true` BUILD SUCCESS（7/7 模块绿）。

Exit Criteria:

- [x] crm 7 链 main 绿；cs 7 链重建绿。
      - 执行证据（2026-08-23）：两链 BUILD SUCCESS（reactor summary 7/7 各绿）。
- [x] 下游破坏核证：drp/logistics 链零新增破坏（证据源 = Phase 1 FQN 双向复扫被引用面零 + drp/logistics orm 无 crm stub + `rg 'app\.erp\.crm\.' module-drp module-logistics` 零命中机械复核；出现未登记破坏即 rule-6 停止回报）。
      - 执行证据（2026-08-23）：`rg 'app\.erp\.crm\.|ErpCrm' module-drp module-logistics` 零命中 + 两 orm 零 crm stub（rg exit=1 双口径）——与登记册「被引用面零」一致，零新增破坏。

### Phase 3 - 测试修复 + retired 半边回收 + 快照重录 + 域级测试 + 早域复跑

Status: completed（2026-08-23：29 Test*.java 编译器驱动清零（首轮 ~200 错/11 文件 + 涟漪批 7 文件 + nanoTime 收尾；种子常量 String 字面量数字保真（"7001"/"1301" 等）+ 算术派生 id 数值保真 `String.valueOf(Long.parseLong(RULE_ID)+1)` 既有保持 + `extractId` String 直返 ×2（CPQ/LeadConversion）+ `orm_propValueByName("id", String)` 形态；非确定性 id 修复 1 处：`TestErpCrmSequenceAndFunnel.newCompletedEvent` `System.nanoTime()` → 确定性字面量 "6021"（output-row-not-exits id=805776190856375 复现根因，ast flake 同型；`*` 通配不可用于 PK 列——checker 按 id getEntityById）；retired-test crm 半边回收全兑付：bridge-test-113（`CRM_TEAM_ID` "9405" + seedMember(String,String) + bridge 注释移除）+ 115（"6601"/"6701".."6703" 同型）+ 116（`TestMockCrmBizModels` 桩随 String IBiz 自然对齐，stale bridge 注释移除）+ 136（`app-test-mock-crm.beans.xml` ioc:type FQN 核证 = 接口 FQN 未变零变更）；快照重录 force-save-output 系统属性模式：188 测试 122 snapshot-finished 零真实失败初录 + CHECKING 复跑 1 失败修复（上述 nanoTime）后 188/188 绿 ×2；`_cases/` 767 → 884（117 首录 + 41 内容 diff，全部快照类零「断言式」误录（9 新录类 grep 断言式标记零命中）+ json5 id String 实证（`"orgId": "1301"`/`"teamId": "11002"`/`"territoryId": "11001"`）；注解零残留 grep 实证；域级测试 `mvn test -pl module-crm/erp-crm-service,module-crm/erp-crm-web` 188/188 绿 + web 0 tests（治理排除 successor M4.1）；平台 IoC 回归 self-wait 未复现零 delta（既有 test-crm-delta 为 M2.2 状态机 bean 非本域新增）；早域复跑 cs 185/185 基线维持全绿（快照零形态漂移））

Targets: `module-crm/erp-crm-service/src/test`、cs test 桥文件
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [x] Fix: 测试修复（29 个 `Test*.java` 编译器驱动清零 + 语义陷阱：id 序比较 → `ConvertHelper.toLong` 数值序（contract idOrder 先例）、装箱 `==` → `.equals()`、算术派生 id 数值保真 `String.valueOf(Long.parseLong(...)+N)`、种子常量 String 字面量数字保真、`orm_propValueByName("id", String)` 形态）。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：~200 编译错/18 文件四轮 javac 清零（TerritoryQuota/TerritoryAssignment/CpqGenerateQuote/LeadScoringRecalcJob 24+44+44+24 错头部批 + SequenceAndFunnel/EventReminderTimeline/PriceRuleEngine/StageDirectionGuard×2/ProductConfigRuleEngine/ForecastRecalcJob/UtmAttribution 小批 + ForecastAndScoring/LeadConversion/ForecastTerritoryRollup/ConversionGuards/EventReminderDisabled/FunnelAggregationEngine/SequenceAssignmentEngine 涟漪批 + `System.nanoTime()` lowercase long 收尾）；语义陷阱 grep：装箱 `==` id 比较零命中（仅 null 守卫）、`Map<Long` 零残留、id 序比较零（阶段序走 stage.sequence int 非幂）。
- [x] Fix: retired-test crm 半边回收（bridge-test-113/115 cs 测试内 crm 种子局部桥移除（`CRM_TEAM_ID` String 化 + `seedMember`/`seedMemberInSession` String 直传）+ bridge-test-116 `TestMockCrmBizModels` 桩签名随 `IErpCrmTeamBiz`/`IErpCrmTeamMemberBiz` String 化 + bridge-test-136 `app-test-mock-crm.beans.xml` ioc:type FQN 核证（M3.2 mock 先例））；cs test-compile 绿。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：113/115 局部桥 String 直传 + 注释移除；116 桩零方法级 id 签名 = 自然对齐 + stale 注释移除；136 ioc:type = `app.erp.crm.biz.IErpCrmTeam(Biz|MemberBiz)` FQN 未变核证零变更；cs test-compile 0 错。
- [x] Proof: 快照重录 RECORDING→CHECKING（`-Dnop.autotest.force-save-output=true` 全局重录 + grep 零注解残留；`_cases/` 767 基线 → String 形态落盘；非确定性单元格 `*` 通配（hr/mfg 先例）；逐案审核 id String 实证 + 拒绝「断言式」类误录（cs 81 目录/sal 7 类超录回退先例））。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：初录 122 snapshot-finished = 100% 错误（零真实失败）；CHECKING 188/188 绿；_cases 767→884 = 117 首录 + 41 diff（首录类全部 JunitAutoTestCase 快照类、grep「断言式」标记零命中）；非确定性处理 = PK 列不可 `*`（checker getEntityById 语义）→ 测试侧确定性 id 修复 1 处（"6021"），非 PK 非确定性单元格零需求；注解零残留（RECORDING/forceSaveOutput/saveOutput grep 空）。
- [x] Proof: 域级测试 `mvn test -pl module-crm/erp-crm-service,module-crm/erp-crm-web` 全绿（web 0 tests 治理排除 `@Tag("full-app")`，successor M4.1）；平台 IoC 回归 self-wait 处置双分支证据——未复现零 delta（mfg/sal 先例）或复现按 fin 修正版先例 delta 落位（含 hr 第二环 nopDataAuthChecker daoProvider lazy-property 变体先例）。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：service 188/188 + web 0 tests 双绿 BUILD SUCCESS；self-wait 未复现 → 零新增 delta（既有 `test-crm-delta` 为 M2.2 状态机 bean 提交 610826e6d 预存，非本域产物）。
- [x] Proof: 早域复跑 cs 基线维持（185/185；快照形态漂移就地 String 化 + `*` 通配按先例处置）。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：`mvn test -pl module-cs/erp-cs-service` 185/185 Failures 0 Errors 0 BUILD SUCCESS；crm 半边种子 String 化后 cs 快照零形态漂移（受影响类断言式 + 空 autotest.yaml，无录制表比对面）。

Exit Criteria:

- [x] crm 域级测试全绿 + cs 基线维持全绿。
      - 执行证据（2026-08-23）：crm 188/188 + web 0 tests；cs 185/185。
- [x] retired crm 半边 grep 清零（`bridge-test-113|115|116|136` cs 侧桥形态清零或 ioc:type 核证）。
      - 执行证据（2026-08-23）：`rg 'bridge-test-11[356]|bridge-test-136' module-cs/` 零命中（exit=1）。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml String 化 + 收尾登记

Status: completed（2026-08-23：grep 门控清零——合法非 id 逐条注明（`.longValue()` ×2 = ErpCrmReportBizModel leadCount 聚合计数 + `String.format("%d")` ×2 = QuotaRollupCalculator fiscalYear/seq 期间号；`Long.parseLong`/`Map<Long`/装箱 ==/id 序比较 main 全零；sql-lib 零存在注明）；id 残余修复 ×2（`ErpCrmSequenceOverdueJob.notifyOverdue` String 直传 + toLong 帮助方法移除（String→Long→String 无谓往返）+ `ErpCrmProductConfiguratorGenerateQuoteProcessor.readLong` Phase 2 后死代码移除，修复后 crm-service 188/188 复跑维持）；page.yaml `:Long` 7 处就地 String 化（timeline/calendar `$lid` + lead-conversion `$lid`+`$sid` + opportunity-kanban `$sid`/`$leadId`+`$sid`，BizModel String 签名核证前置）+ YAML 良构校验 ×4 + crm-web 重建绿 + 手写 view 零被动变更核证（非 `_gen` 变更 = 恰 4 page.yaml）；owner doc territory.md Long id/BIGINT 陈述就地注记引用本计划（其余文件 grep 零命中维持）；登记册 retired-test crm 半边 note 补指针 ×4（113/115/116/136）+ fail-closed 解析验证（8/8 id 在位）+ roadmap M3.4 → done（位次 18 解锁 + 头部续链）+ 日志 docs/logs/2026/08-23.md；收尾验证 crm 7 链 no-am `mvn clean install -DskipTests` BUILD SUCCESS 7/7）

Targets: crm-service main、crm-web 手写面、`docs/design/crm/`、登记册、路线图、日志
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3 完成

- [x] Fix: 语义陷阱 grep 门控清零（路线图横切 §3 清单：`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较；合法非 id 项逐条注明；sql-lib 零存在注明）。
      - Skill: none
      - 执行证据（2026-08-23）：合法非 id ×4 逐条注明（leadCount 聚合计数 ×2 + fiscalYear/seq 期间号格式化 ×2）；id 残余修复 ×2（SequenceOverdueJob toLong 移除 + CPQ readLong 死代码移除）；`Map<Long`/装箱 ==/id 序比较 main 全零；sql-lib 零存在（module-crm 无 `*.sql-lib.xml`）；修复后 188/188 复跑维持绿。
- [x] Fix: page.yaml `:Long` 7 处就地 String 化（timeline/calendar/lead-conversion/opportunity-kanban 四文件；BizModel 签名一致性核证，mfg CRP / sal three-way-match 先例）+ YAML 良构校验 + crm-web 重建绿 + 手写 view 零被动变更核证（git status 非 `_gen` 变更 = 预期仅 page.yaml 主动 Fix）。
      - Skill: `nop-backend-dev`
      - 执行证据（2026-08-23）：7 处全落（1+1+2+3）；python yaml.safe_load ×4 OK；`:Long` 复扫零命中；crm-web clean install BUILD SUCCESS；非 `_gen` 资源变更 = 恰 4 个 page.yaml。
- [x] Fix: owner doc 注记——`docs/design/crm/territory.md` Long id/BIGINT 陈述就地注记引用本计划；其余文件 grep 复核零命中维持。
      - Skill: none
      - 执行证据（2026-08-23）：territory.md:221 实现注记追加迁移翻转注（stdDataType=long 为落地时点形态 → 现 string，stdSqlType BIGINT/DDL 零变化，Java String）；`grep 'Long\|BIGINT' docs/design/crm/*.md` 其余文件零命中。
- [x] Add: 登记册状态更新（B main 4 条 → retired 附逐条兑付 note + retired-test crm 半边 note 补指针（含 136）+ fail-closed 解析验证）+ 路线图 M3.4 → done（M2/M3 表位次 17 证据摘要 + 头部「最后更新」续链 + 位次 18 解锁）+ 日志 `docs/logs/2026/{执行当日}.md`（rule 8 日期口径，含验证状态 + B 义务兑付 + cs 复跑基线）。
      - Skill: none
      - 执行证据（2026-08-23）：B main 053..056 Phase 2 已 retired 附 note；本次补 retired-test 113/115/116/136 crm 半边 note 指针（含 136 ioc:type 核证）；fail-closed 解析验证 8/8 id 在位（json5 宽松解析 + 结构断言）；roadmap 位次 17 `done` 证据摘要 + 头部 2026-08-23 M3.4 续链 + 位次 18 drp 解锁；日志 `docs/logs/2026/08-23.md` 落盘。

Exit Criteria:

- [x] grep 门控清零（登记例外除外）+ page.yaml `:Long` 清零。
      - 执行证据（2026-08-23）：main 门控命中全为逐条注明合法非 id + id 残余 ×2 已修零；page.yaml `:Long` 复扫零命中。
- [x] 登记册/路线图/日志三处一致落盘。
      - 执行证据（2026-08-23）：registry（4 note 指针 + 解析验证）/ roadmap（位次 17 done + 头部续链）/ log（08-23.md）三处 M3.4 口径一致。

## Draft Review Record

- Independent draft review iteration 1（技术/执行契约视角，fresh session，ses_fd5f748bbffeguuYD97aTdQTvI）: `acceptable as-is` + 保护区域技术侧 `approve`——14 项事实验证全对（126 列/PK 40+FK 86/stub 6/B main 053..056/C1C2 清单/29 测试/_cases 767/page.yaml 7 处/findFirstByOrg 0/继承中间态/drp+logistics 零 ErpCrm 引用/213−126=87）；1 MINOR（bridge-test-136 为 refDomain=crm 的 A3' 补登条目，应随 116 一并核证回收）已修正（Baseline/Phase 1/3/4 四处落点补 136）。
- Independent draft review iteration 1（治理/规范视角，fresh session，ses_fd5f7223cffegZHMuPKWiDE3tz）: `needs revision`——1 MAJOR（文件名时间戳 0702 回溯失真：内容引用 M2.6 22:53 后事实，且跨批次字母序倒置）+ 2 MINOR（136 措辞 / Phase 2 下游破坏核证缺证据源指针）。**已修正**：三计划批重命名 `2026-08-22-2311-{1,2,3}`（实际起草时间 23:11-23:13）+ H1/Related 交叉引用同步；136 措辞与 Phase 2 证据源指针（Phase 1 FQN 复扫 + orm stub 零 + `rg 'app\.erp\.crm\.'` 机械复核）已落位；Phase 1 退出口径补 213−126 算式。
- Independent draft review iteration 2（治理/规范视角，fresh session，ses_fd5edb06fffeEMcdR0Rqa8yIpQ）: `accept` + 保护区域治理侧 `approve`——iteration-1 三项修复逐项实仓核证 RESOLVED（2311 重命名全批落位零残留 0702 引用 / 136 五处落点一致无内部矛盾 / Phase 2 证据源在位）；治理健康维持（draft/planned/零 [x]/反松弛/记录诚实）；0 BLOCKER / 0 MAJOR。
- **共识达成（2026-08-22）**：技术侧 iteration 1 `acceptable as-is` + `approve`（ses_fd5f748bbffeguuYD97aTdQTvI）与治理侧 iteration 2 `accept` + `approve`（ses_fd5edb06fffeEMcdR0Rqa8yIpQ）双落盘，保护区域双独立子 agent 批准完成，转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（126 列落源 + 7 链绿 + B/C 兑付 + 快照重录 + 测试绿）
- [x] 相关文档对齐（登记册 + 路线图 + owner doc + 日志）
- [x] 已运行验证：`mvn clean install -pl module-crm/erp-crm-{codegen,dao,meta,service,web,app,api} -DskipTests`（no-am）BUILD SUCCESS + `mvn test -pl module-crm/erp-crm-service,module-crm/erp-crm-web` 全绿 + cs 复跑基线（185/185）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（保护区域双批准落盘）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### drp/logistics 下游破坏（预期零）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册实证零被引用面（drp/logistics orm 无 crm stub、零 service/test 条目）；若 Phase 2 实测出现未登记破坏按 rule-6/D4 停止回报，不静默登记
- Successor Required: no（预期不存在；实测存在时升级为 M0 裁决）

### 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.1 专项全量清扫已登记（M3.6 结束审计发现的模式盲区）；本域 7 处属本计划范围就地修复
- Successor Required: yes（M4.1）

### web 页面测试治理排除（`@Tag("full-app")`）

- Classification: `watch-only residual`
- Why Not Blocking Closure: M1.1 已提交治理决策（surefire excludedGroups），实证依赖全量 classpath
- Successor Required: yes（M4.1 `ErpAllWebPagesTest` 兜底核证）

## Closure

Status Note: M3.4（crm，冻结序位次 17）四 Phase 全部落地且经独立结束审计实仓复核通过——126 列 stdDataType 落源三重证明、crm 7 链 main 绿、B main 4 条退役兑付、retired-test crm 半边回收（113/115/116/136）、快照重录 767→884、crm 域级测试 188/188 绿（审计者独立复跑确认）、cs 早域复跑基线 185/185 维持、grep 门控清零、page.yaml 7 处 String 化、登记册/路线图/owner doc/日志四处一致落盘。Deferred But Adjudicated 三项均为 watch-only/out-of-scope 且命名 successor，无范围内项目降级。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（CLOSURE_VERIFY，fresh session，mission-driver 2026-08-22-070202-mission-driver，2026-08-23 执行；非执行者会话）
- Evidence: 审计者独立实仓复核（2026-08-23）：
  - 独立复跑 `mvn test -pl module-crm/erp-crm-service,module-crm/erp-crm-web`：`Tests run: 188, Failures: 0, Errors: 0, Skipped: 0` + web 0 tests（治理排除）+ BUILD SUCCESS——与 Phase 3 声明一致。
  - orm 实仓：`module-crm/model/app-erp-crm.orm.xml` `stdDataType="string"` 364 处；残留 `stdDataType="long"` 35 处全为 delVersion 等非 PK/FK 列（逐条抽验）。
  - 登记册实仓：`tools/id-migration-registry.json5` bridge-main-053..056 全部 `status: retired` 附 M3.4 兑付 note；bridge-test-113/115/116/136 retired note 含 crm 半边核证指针（136 note 实仓确认）。
  - grep 门控复验：`bridge-main-05[3-6]` / `bridge-test-11[356]|bridge-test-136` 在 module-cs 零命中；`TicketAssignResolver` 零 bridge 残留；crm-service main `Map<Long` / `Long.parseLong` 零命中；drp/logistics `ErpCrm|app\.erp\.crm\.` 零命中（下游零破坏实证）。
  - page.yaml 复验：crm-web 全部 `.page.yaml` `:Long` 零命中；timeline:38 `$lid:String` / opportunity-kanban:49 `$sid:String` + :97 `$leadId:String` String 形态实仓确认。
  - 快照：`_cases` 递归文件数 884（767→884 口径一致）；`TestErpCrmSequenceAndFunnel` 确定性 id `"6021"` 实仓确认（:532）。
  - 文档一致：roadmap 头部「最后更新 2026-08-23 M3.4 done + 位次 18 drp 解锁」+ 位次 17 行证据摘要；`docs/logs/2026/08-23.md` M3.4 条目含全绿验证状态；`docs/design/crm/territory.md:222` 迁移翻转注记引用本计划。
  - 反空洞核验：翻转后的 String id 在运行时真实生效（188 测试经 GraphQL/ORM 引擎执行全绿，快照 json5 id String 形态实证），无仅签名存在的行为空壳。

Follow-up:

- 无（已确认缺陷不得出现在此处）
