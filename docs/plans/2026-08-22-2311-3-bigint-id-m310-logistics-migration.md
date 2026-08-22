# 2026-08-22-2311-3-bigint-id-m310-logistics-migration 主键/外键 string 化 M3.10：logistics 域迁移（冻结序位次 19，末位域）

> Plan Status: completed（2026-08-23：四 Phase 全部执行完成 + 独立结束审计 `passes closure audit`（fresh session ses_fd4db70bdffemG16KVMBSGvPYj，13 项 live-repo 抽检全对，0 MAJOR / 0 MINOR）；**mission 里程碑：M1-M3 全部 19 域迁移完成（总待改 32→0），M4.1 为唯一剩余工作项**。批准记录见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.10（logistics，冻结序位次 19）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 19（M3.10）
> Related: `docs/plans/2026-08-22-2311-1-bigint-id-m34-crm-migration.md`（批内序 1）、`docs/plans/2026-08-22-2311-2-bigint-id-m37-drp-migration.md`（批内序 2；两者均按冻结总序先行、非本域编译硬前置）、`docs/plans/2026-08-22-1814-2-bigint-id-m26-sales-migration.md`（backward-163/224 + log 链零破坏登记来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **冻结序位置**：位次 19（冻结序末位业务域）；精确前置 M2.6 + M2.3 + M2.2 + M2.1 + M1.1 + M1.2 + M3.6 全部 `done`（本计划批内序 3；crm/drp 按冻结总序先行，非本域编译硬前置）。**本域完成后 19 域全数迁移，M4.1 收尾解锁**。
- **工具现状（2026-08-22 scan）**：`module-logistics/model/app-erp-logistics.orm.xml` **32 列 NEEDS FIX**（PK 13 + FK 19；其中 notGenCode stub 5 列——5 个 md stub 实体（`ErpMdCurrency`/`ErpMdEmployee`/`ErpMdMaterial`/`ErpMdOrganization`/`ErpMdPartner`），stub 实体全部属已迁移域 md（M1.1），随本域翻转、与权威源对齐（M1.1 先例；分解以 Phase 1 dry-run 为准）），0 DEFERRED。
- **登记册义务（`tools/id-migration-registry.json5` + 审计文档 §6.19，Phase 1 强制全量复核）**：
  - A1 延后列：0；**A2 main 前向桥：0；A3 test 前向桥：0**（本域不引用任何晚域——冻结序末位，无晚域）。
  - **B main 桥接退役：0；B retired-test 半边回收：0**（登记册零 refDomain=logistics 条目——双向零桥接义务，mission 最末一个「净域」迁移）。
  - C1 后向 main 4 条（backward-160 fin 1 文件 / 161 inv 1 文件 / 162 notify 3 文件 / 163 sal 1 文件；去重后 3 个手写文件：`AbstractErpLogShipmentDeliveredProcessor`/`GatewayDispatcher`/`ErpLogDraftEscalationJob`）；C2 后向 test 6 条（backward-219 fin 4 文件 / 220 inv 1 / 221 md 3 / 222 notify 1 / 223 pur 1 / 224 sal 1；去重后 6 个测试类：`TestErpLogFreightPosting`/`TestErpLogShipmentPostingEnd`/`TestErpLogTrackingPollJob`/`TestErpLogSalesDeliveryLinkage`/`TestErpLogPath2LandedCost`/`TestErpLogDraftEscalationJob`）——全部指向已迁移域。
  - 被引用面（未迁移域 → 登记中间态）：**零**（冻结序末位，无任何晚域）。
- **继承中间态（M2.6 Phase 2 实测登记，2026-08-22）**：log-dao + log-service main 编译全绿（backward-163 类型级零编译破坏）——本域为「无继承破坏」迁移，修复面为本域 `Long id` 参数族翻转 + IBiz 重生成涟漪。
- **findFirstByOrg 消费点：零**（2026-08-22 grep 实证；无 md 语义参数桥义务）。
- **page.yaml**：log-web 手写面 `:Long` **1 处**（`dashboard/shipment-tracking:38` `$sid`）——Phase 4 就地 String 化（mfg CRP / M2.6 three-way-match 先例）+ YAML 良构校验 + log-web 重建绿。
- **代码与测试规模**：log-service main 手写修复面以 C1 全清单 3 文件为定位面（编译器可发现更多本域 Long id 面：shipment/carrier/booking 族 BizModel 与 IBiz + posting 族）；test 15 个 `Test*.java`（C2 登记 6 个）；快照 `_cases/` 现值 **275** 文件（重录基线）。
- **早域复跑基线**：无 B 义务即无早域复跑义务（B=0；Phase 4 工具重扫即终态证明）。
- **owner doc**：`docs/design/logistics/`——`delivery-window.md` 含 BIGINT 列型陈述（2026-08-22 grep），Phase 4 就地注记；其余文件零命中复核。

## Goals

- 32 列 `stdDataType long→string` 落源（stdSqlType 保持 BIGINT，DDL 零变化），三重证明（新鲜度门控 + git diff + 工具重扫 log 段 0 NEEDS FIX）。
- log 7 模块链 no-am main 绿 + 域级测试全绿（`mvn test -pl module-logistics/erp-log-service,module-logistics/erp-log-web`）。
- 快照 RECORDING→CHECKING 每域重录（force-save-output 系统属性模式，mfg/sal 先例）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 1 处就地 String 化 + owner doc 注记 + 登记册状态更新 + 路线图 M3.10 → done（**19 域全数完成 + M4.1 解锁注记**）+ 日志。

## Non-Goals

- 不执行 M4.1 收尾（全量构建恢复/全量测试/E2E/compliance/baseline/文档——本域完成后由 successor plan 承载；本计划仅在路线图登记解锁）。
- 不改 `stdSqlType`/DDL/CSV 种子/序列号引擎；不动 `delVersion` 等非 PK/FK BIGINT 列（孤儿操作人列规则 4 保持 long）。
- 不修 M4.1 登记的存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）。
- 不翻转 md 侧 `findFirstByOrg(Long)` 签名（本域零消费点；pur/sal 先例登记例外不适用）。

## Task Route

- Type: `implementation-only change`（orm 模型变更 → 增量重生成 → 编译器驱动修复，路线图 M1-M3 标准结构）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（标准结构 + 规则 1-8 + 横切 §5 保护区域 design 证据链）、`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.19 + `tools/id-migration-registry.json5`（消费协议）、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`（M0 裁决与冻结序结论）、`docs/design/logistics/`（业务 owner doc，Phase 4 注记对象）
- Skill Selection Basis: 域迁移 plan 预期技能 = `nop-backend-dev`（BizModel/IBiz 手写修复 + 跨实体调用规则）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；orm 回写机制走 M0.1 裁定工具链，不经技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具链：`node tools/check-bigint-id-types.mjs dry-run` + `node tools/verify-id-fix-copy-diff.mjs module-logistics` + `node tools/scan-cross-domain-id-coupling.mjs`；回写一律走「时点 dry-run + 新鲜度门控」三步，禁止盲 cp/apply 模式。
- 硬前置 = 最后全绿基线 commit 的全量 install + 每个已完成域链 install 已在本地 Maven 仓库（D3 修订口径「每个已完成域链」；logistics 位次 19 前的 18 域链全部 install）。
- 回滚策略（mfg/qa/sal 先例）：orm 落源后回滚 = `git revert` orm 变更 + 7 模块链增量重生成；Phase 2/3 完成后回滚需先 revert 测试代码变更，再 revert 本域 orm + 重生成（本域零早域桥接退役面，回滚链最短）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed（2026-08-23：登记册全量消费与 Baseline 一致（A1=0/A2=0/A3=0/B main 0/retired-test 0/C1 160-163/C2 219-224/被引用面零）+ FQN 复扫双口径补登 ×3（backward-160 补 `LogisticsFreightProvider`（fin setPartnerId id 流，M0.2 id-flow 启发式漏报，ast 134 同类）/ backward-221 补 `TestErpLogSalesDeliveryLinkage`（内联 FQN md 种子实体，A3' 同型）/ 新增 backward-255（main logistics→md `AcctSchemaResolver.resolvePrimarySchemaId` 方法调用口径 id 流）+ §6.19 追注 + beans.xml ioc:type 双口径零跨域命中（仅 `app.erp.log.*`）+ 双独立子 agent 批准已在 Draft Review Record 落盘（治理 ses_fd5ed909bffeR7Iuyd59brvJ1x + 技术 ses_fd5f6bf75ffexiJQiz8tuCtEoN 双 approve，共识达成转 active）；orm 落源三步：dry-run 时点刷新 → 新鲜度门控 32 行 stdDataType-only/0 非法 → 落源 + git diff 64 行（32 对）100% stdDataType、stdSqlType 前后分布 IDENTICAL（全 BIGINT 保持）、非 PK/FK 零变化、md stub 5 列与权威源一致（id ×13 + FK ×19 = orgId ×6/shipmentId ×4/partnerId ×2/carrierId ×2/windowId/shipperId/materialId/freightCurrencyId/carrierConfigId 各 1）；工具重扫「实际修改 0 列」= 213 − 126 crm − 55 drp − 32 log = 总待改 0，mission 19 域 orm 全数落源）
Targets: `module-logistics/model/app-erp-logistics.orm.xml`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 精确前置 M2.6 + M2.3 + M2.2 + M2.1 + M1.1 + M1.2 + M3.6 全部 done（roadmap 规则 1 readiness 口径）；批内序 3（crm/drp 按冻结总序先行，非本域编译硬前置）

- [x] Proof: 登记册全量消费——`registry §6.19 + json5` 逐条核对与 Baseline 一致（A1=0 / A2=0 / A3=0 / B main 0 / retired-test 0 / C1 160-163 / C2 219-224 / 被引用面零）；差异即补登落册。
      - Skill: none
- [x] Proof: FQN 复扫（b2b A3' 盲区先例：java 内联 FQN 非 import 行 + `*.beans.xml` ioc:type 双口径）——log 引用面补登核对，预期仅命中已登记条目。
      - Skill: none（实测命中 3 处未登记差异已全部补登：160/221 evidence 扩充 + 新增 backward-255，§6.19 追注落盘；其余命中全部落于已登记 C2 文件面 / `_gen` 胶水 / javadoc 注释）
- [x] Proof: 双独立子 agent 批准（治理 + 技术，fresh session）落盘 Draft Review Record（保护区域 `model/*.orm.xml` 要求）。
      - Skill: none（批准已于 2026-08-22 落盘本文件 Draft Review Record，双 approve + 共识达成转 active，见上）
- [x] Fix: orm 落源三步——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-logistics` 新鲜度门控（32 行 stdDataType-only、零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核（stdSqlType 全保持 BIGINT、非 PK/FK 列零变化、md stub 5 列与权威源一致）。
      - Skill: none

Exit Criteria:

- [x] 工具重扫 log 段 0 NEEDS FIX / 0 DEFERRED（本域硬门口径 = 213 − 126 crm − 55 drp = 32 全数落源），三重证明落盘。
- [x] 登记册消费零未解释差异。

### Phase 2 - 增量重生成 + 主代码编译修复

Status: completed（2026-08-23：`mvn clean install -pl` 7 模块显式列表 no-am + `-Dmaven.test.skip=true` BUILD SUCCESS；首轮 13 编译错 4 手写文件（GatewayDispatcher ×4 / AbstractErpLogShipmentDeliveredProcessor ×5 / LogisticsFreightProvider ×1 / ErpLogDeliveryBookingBizModel ×3）一轮清零；手写修复面 14 文件 = dao 2 IBiz（IErpLogShipmentBiz advise/completeShipment/cancelShipment + IErpLogDeliveryBookingBiz book/releaseForShipment/markArrived/markMissed/findActiveByShipment id 参数族 String）+ service 12（ShipmentDeliveredEvent 3 id 字段 / GatewayDispatcher 4 方法 + getClient carrierId + releaseForShipment 直传 / AbstractErpLogShipmentDeliveredProcessor voucherId String + resolveAcctSchemaId String（backward-255 兑付）+ resolveCarrierPartnerId String + sal get 直传 / LogisticsFreightProvider setPartnerId String 直传 + asLong 桥移除（drp B 退役同型）/ ErpLogDeliveryBookingBizModel idOrder 数值序（contract 先例）+ String.valueOf ×3 移除 / ErpLogShipmentBizModel 3 参数 / 3 per-mutation Processor / spi Registry + Factory + MockFactory carrierId String）；变更面 67 文件中生成件全经 codegen 重生成（`_gen` 8 实体 + xmeta 8 + api beans 16 + view `_gen` 8 + `_app.orm.xml`），手写 view 零被动变更）
Targets: `module-logistics/erp-log-{codegen,dao,meta,service,web,app,api}`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 完成

- [x] Fix: 7 模块链 no-am 重生成构建 main 绿（`-pl` 显式列表 + `-Dmaven.test.skip=true`）；编译器驱动修复手写面（log-dao IBiz/值对象 id 参数族 + service C1 全清单 3 文件为定位面：`AbstractErpLogShipmentDeliveredProcessor`（fin/inv/sal posting 族 String 值流转，mnt/qa 先例）+ `GatewayDispatcher` + `ErpLogDraftEscalationJob`（notify String API 消费））。
      - Skill: `nop-backend-dev`（实际修复面超出 C1 3 文件定位面：+ LogisticsFreightProvider（Phase 1 补登 backward-160 兑付）+ ErpLogDeliveryBookingBizModel/ShipmentBizModel/spi 族——「以编译器实际清单为准，不以 C1 文件为界」ast 134 追注先例；ErpLogDraftEscalationJob 零 Long 修复需求（notify String API 消费签名不变，编译绿零改动））
- [x] Proof: 继承中间态基线复核——编译错误清单 100% 为本域翻转涟漪（非继承破坏），与 M2.6 登记「log-dao + log-service main 编译全绿」一致；异常即登记并按 rule-6/D4 处置。
      - Skill: none（**偏差如实登记**：orm 落源前实测 log-dao 13 编译错 100% `_gen` md 关系胶水（对称耦合已登记中间态，crm-dao 76/drp-dao 58 同型，随重生成自愈实证）+ log-service 2 处潜伏编译破坏（LogisticsFreightProvider setPartnerId vs fin VoucherFact String（M2.7 翻转后未再编译本域）+ AbstractErpLogShipmentDeliveredProcessor resolveAcctSchemaId vs md String（M1.1 后同因））——plan 引用的 M2.6「log-dao + log-service main 编译全绿」口径为 M2.6 时点实测，M2.7 fin 延后列兑付翻转 VoucherFact 后本域未复编；全部属已登记中间态模式或本域适配面，编译器可发现并已全数修复，不触发 rule-6 停止（D4）；冻结序末位无晚域，工具重扫 0 = 本域终态证明）

Exit Criteria:

- [x] log 7 链 main 绿。
- [x] 零继承破坏复核落盘 + 下游破坏核证：冻结序末位无晚域，工具重扫为本域终态证明。

### Phase 3 - 测试修复 + 快照重录 + 域级测试

Status: completed（2026-08-23：测试修复 12 文件两轮 javac 清零（首轮 100 错截断实测 200 错误行 → 二轮 0：种子常量 String 字面量数字保真（`1L`→`"1"`/`8801L`→`"8801"`/`seedTemplate(9201L`→`"9201"`）+ 算术派生 id 数值保真 `String.valueOf(partnerId)` ×4 + extractId String 直返 ×2（DuplicateGuard/TrackingNoUk `Long.valueOf(String.valueOf(get("id")))` 残留移除）+ `orm_propValueByName("id", String)` 形态（DraftEscalationJob seedTemplate）+ 合法非 id Long 保留（assertEquals(1L,count) 计数 / CONFIG_DRAFT_ESCALATION_HOURS 48L/24L 配置 / Timestamp 100L*3600_000L）+ 语义陷阱 grep 零命中（id 序比较/装箱 ==/parseLong/longValue 全零）+ 运行时断言修复 ×1（TestErpLogPath2LandedCost:111 `assertEquals(partnerId, apPartnerId)` Long vs String 恒败 → `String.valueOf(partnerId)`）；快照重录 force-save-output 系统属性模式（免注解编辑 + 注解零残留实证）：66 测试全 snapshot-finished 预期错（1 真实失败即上述断言修复）→ CHECKING 复跑 **66/66 绿**；`_cases` 275 → 650 = 375 首录 + 57 diff + 0 删除，零「断言式」类误录（14 快照类全部基线既有，无新类目录首录；TestLogPostingFaultInjection/StateMachineMatrix 纯单元类零 _cases 维持）+ json5 id String 实证（`"id": "2"` 等）+ 非确定性单元格 1 处 `*` 通配（DeliveryBooking testShipmentDeliveredReleasesBooking ACTUAL_DELIVERY_DATE=CoreMetrics.today() 跨日漂移，hr/mfg 先例）；域级测试 `mvn test -pl module-logistics/erp-log-service,module-logistics/erp-log-web` 全绿（service 66/66 + web 0 tests 治理排除 `@Tag("full-app")`，successor M4.1）；平台 IoC 回归 self-wait **未复现零 delta**（mfg/sal/crm/drp 先例分支））
Targets: `module-logistics/erp-log-service/src/test`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [x] Fix: 测试修复（15 个 `Test*.java` 编译器驱动清零 + 语义陷阱：id 序比较 → `ConvertHelper.toLong` 数值序（contract idOrder 先例）、装箱 `==` → `.equals()`、算术派生 id 数值保真、种子常量 String 字面量数字保真、`orm_propValueByName("id", String)` 形态；notify test 引用面 = `TestErpLogDraftEscalationJob`（backward-222）种子 String 化）。
      - Skill: `nop-testing`（实测 12 文件需改动（15 中 3 类零 Long 面零改动）；主代码 idOrder 修复落位 ErpLogDeliveryBookingBizModel（Phase 2），测试侧 id 序比较零命中）
- [x] Proof: 快照重录 RECORDING→CHECKING（`-Dnop.autotest.force-save-output=true` 全局重录 + grep 零注解残留；`_cases/` 275 基线 → String 形态落盘；非确定性单元格 `*` 通配（hr/mfg 先例）；逐案审核 id String 实证 + 拒绝「断言式」类误录（cs 81 目录/sal 7 类超录回退先例））。
      - Skill: `nop-testing`
- [x] Proof: 域级测试 `mvn test -pl module-logistics/erp-log-service,module-logistics/erp-log-web` 全绿（web 0 tests 治理排除 `@Tag("full-app")`，successor M4.1）；平台 IoC 回归 self-wait 处置双分支证据——未复现零 delta（mfg/sal 先例）或复现按 fin 修正版先例 delta 落位（含 hr 第二环 nopDataAuthChecker daoProvider lazy-property 变体先例）。
      - Skill: `nop-testing`（未复现零 delta 分支实证）

Exit Criteria:

- [x] log 域级测试全绿。
- [x] 本域无 retired-test/B 义务（Phase 1 零实证复核）。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml String 化 + 收尾登记

Status: completed（2026-08-23：grep 门控 main+test 全清零——`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/id 序比较全零命中；装箱 `==` 仅 `GatewayDispatcher:315 getId() == null` null 检查合法保留；`Long` 残留 = `_gen` delVersion 规则 4 + idOrder `Long.compare(ConvertHelper.toLong)` 数值序合法；sql-lib 零存在注明（find 零命中）；page.yaml `:Long` 1 处就地 String 化（shipment-tracking:38 `$sid:String` + `sid: "${shipmentId \|\| null}"` 兜底 + adaptor 空值守卫 `!== ''`，drp net-requirement/pur three-way-match 先例形态）+ python yaml.safe_load 良构校验 PASS + log-web 重建 BUILD SUCCESS + log-web 全资源 `:Long` 复扫零命中 + 手写 view 零被动变更（git status 非 `_gen` 变更 = 恰 1 page.yaml 主动 Fix）；owner doc `docs/design/logistics/delivery-window.md` 数据模型节 BIGINT 列型注记（数据库列型 vs Java 层 String 区分 + 引用本计划；其余 5 文件 grep 复核零命中维持）；登记册 11 条 logistics backward 指针（160-163/255/219-224）核销注记 append「M3.10 已兑付」+ fail-closed 解析验证 11/11（check-bigint-id-types scan 通过 + loadRegistry 直读断言）；roadmap M3.10 → done（位次 19 证据摘要 + 头部「最后更新」mission 里程碑注记：**M1-M3 全部 19 域完成 + M4.1 唯一剩余工作项**）+ 日志 `docs/logs/2026/08-23.md` M3.10 条目（rule 8 日期口径，含验证状态 + mission 域迁移段完成里程碑））
Targets: log-service main、log-web 手写面、`docs/design/logistics/`、登记册、路线图、日志
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3 完成

- [x] Fix: 语义陷阱 grep 门控清零（路线图横切 §3 清单：`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较；合法非 id 项逐条注明；sql-lib 零存在注明）。
      - Skill: none（合法非 id 项：`getId() == null` null 检查 ×1 + `_gen` delVersion 规则 4 + idOrder Long.compare 数值序合法；测试侧合法：assertEquals(1L,count) 计数 / CONFIG_DRAFT_ESCALATION_HOURS 48L/24L / Timestamp 100L*3600_000L）
- [x] Fix: page.yaml `:Long` 1 处就地 String 化（shipment-tracking `$sid`；BizModel 签名一致性核证，mfg CRP / sal three-way-match 先例）+ YAML 良构校验 + log-web 重建绿 + 手写 view 零被动变更核证（git status 非 `_gen` 变更 = 预期仅 page.yaml 主动 Fix）。
      - Skill: `nop-backend-dev`（BizModel 签名一致性：`ErpLogShipment__get(id:String)` 平台 ICrudBiz String id + `ErpLogShipmentLog__findPage` 无 id 变量）
- [x] Fix: owner doc 注记——`docs/design/logistics/delivery-window.md` BIGINT 列型陈述就地注记引用本计划；其余文件 grep 复核零命中维持。
      - Skill: none（其余 5 文件 carrier-integration/README/state-machine/ui-patterns/use-cases 零命中）
- [x] Add: 登记册状态更新（本域零 active 条目翻转——C1/C2 backward 指针核销注记 + fail-closed 解析验证）+ 路线图 M3.10 → done（M2/M3 表位次 19 证据摘要 + 头部「最后更新」续链 + **19 域全数完成 + M4.1 唯一剩余工作项注记**）+ 日志 `docs/logs/2026/{执行当日}.md`（rule 8 日期口径，含验证状态 + mission 域迁移段完成里程碑）。
      - Skill: none（核销注记 = 11 条 backward 指针 note append「M3.10 已兑付」，status 保持 active（drp C 指针先例——定位面非义务条目）；日志落位 `docs/logs/2026/08-23.md` 顶部倒序）

Exit Criteria:

- [x] grep 门控清零（登记例外除外）+ page.yaml `:Long` 清零。
- [x] 登记册/路线图/日志三处一致落盘 + 工具 scan 里程碑证据（「总待改 0」以批内序 1/2（crm/drp）先行完成为前提；否则如实登记 crm/drp 残留数）。（三处一致落盘 + 工具 scan「实际修改 stdDataType 的列: 0」终态证据——crm 126/drp 55 先行完成前提成立）

## Draft Review Record

- Independent draft review iteration 1（技术/执行契约视角，fresh session，ses_fd5f6bf75ffexiJQiz8tuCtEoN）: `acceptable as-is` + 保护区域技术侧 `approve`——13 项事实验证全对（32 列/PK 13+FK 19/stub 5/登记册零 refDomain=logistics 与零 retireOwner=M3.10/C1 3 文件+C2 6 类/15 测试/_cases 275/page.yaml 1 处/notify test 唯一 DraftEscalationJob/7 模块/继承零破坏双源核证/delivery-window 命中/213 算术/结构+D3 no-am/反松弛/M4.1 framing）；3 MINOR（批内序条件口径 / 「Long id/BIGINT」措辞 / Phase 2 Item Types 与条目不对齐）已全部修正（Phase 1/4 退出口径改本域硬门+批内序前提 / 措辞改「BIGINT 列型陈述」 / Phase 2 补 Proof 条目继承基线复核）。
- Independent draft review iteration 1（治理/规范视角，fresh session，ses_fd5ed909bffeR7Iuyd59brvJ1x；首轮子代理因速率限制失败，本条为重试补录）: `accept` + 保护区域治理侧 `approve`——模板/命名（文件出生时间 23:11:32/23:12:17/23:13:01 与批序 1/2/3 一致、无回溯倒置）/状态生命周期/反松弛/保护区域证据链/Closure Gates 域级口径（B=0 无早域复跑 = 登记册机器核证的 evidence-based omission，hr M3.3 先例）/M4.1 仅登记不执行/回滚策略/Prereqs 全 PASS；1 MINOR（Phase 4 Item Types 声明 `Fix | Add | Proof` 但无 Proof 条目）已修正（改 `Fix | Add`）。
- **共识达成（2026-08-22）**：技术侧（ses_fd5f6bf75ffexiJQiz8tuCtEoN）+ 治理侧（ses_fd5ed909bffeR7Iuyd59brvJ1x）iteration 1 双 approve、双独立子 agent 批准完成，3+1 MINOR 已全部就地修正并记录于上，转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（32 列落源 + 7 链绿 + 快照重录 + 测试绿）
- [x] 相关文档对齐（登记册 + 路线图 + owner doc + 日志）
- [x] 已运行验证：`mvn clean install -pl module-logistics/erp-log-{codegen,dao,meta,service,web,app,api} -DskipTests`（no-am）BUILD SUCCESS + `mvn test -pl module-logistics/erp-log-service,module-logistics/erp-log-web` 全绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（保护区域双批准落盘）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 执行证据（2026-08-23）：独立结束审计子代理（fresh session ses_fd4db70bdffemG16KVMBSGvPYj，非执行者上下文）对 live repo 现场抽检 13 项全对（orm 32 列 string/BIGINT 保持 + delVersion 恰 8 处合法 / 工具 scan 实际修改 0 / 手写面 idOrder+String 直传核证 / 测试陷阱零 / 快照 650 文件 String id 实证 + 注解零残留 / page.yaml $sid:String + :Long 零 / 登记册 11 条兑付 note + fail-closed / §6.19 追注 / roadmap 位次 19 done + 里程碑注记 + M4.1 todo / owner doc 注记 / 日志条目 / 四 Phase 文本一致 / TestErpLogDeliveryBooking 9/9 复验绿），verdict **passes closure audit**，0 MAJOR / 0 MINOR。
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### M4.1 收尾（全量构建恢复 + E2E + compliance + baseline + 文档）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 路线图 M4.1 独立工作项（依赖全部 M1-M3）；本域完成后仅登记解锁，不在本计划执行
- Successor Required: yes（M4.1 plan）

### 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.1 专项全量清扫已登记（M3.6 结束审计发现的模式盲区）；本域 1 处属本计划范围就地修复
- Successor Required: yes（M4.1）

### web 页面测试治理排除（`@Tag("full-app")`）

- Classification: `watch-only residual`
- Why Not Blocking Closure: M1.1 已提交治理决策（surefire excludedGroups），实证依赖全量 classpath
- Successor Required: yes（M4.1 `ErpAllWebPagesTest` 兜底核证）

## Closure

Status Note: 四 Phase 全部 completed，Exit Criteria / Closure Gates 全 `[x]`，独立结束审计通过（passes closure audit），M3.10 可关闭（roadmap 位次 19 done；**mission 里程碑：M1-M3 全部 19 域完成，M4.1 为唯一剩余工作项**）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session ses_fd4db70bdffemG16KVMBSGvPYj，2026-08-23，非执行者上下文）；verdict **passes closure audit**（0 MAJOR / 0 MINOR），live repo 现场抽检 13 项全对：
  - orm：`module-logistics/model/app-erp-logistics.orm.xml` PK 13（含 md stub 5）+ FK 19 全 `stdDataType="string"` + `stdSqlType="BIGINT"` 保持；`stdDataType="long"` 残余恰 8 处全部 `delVersion`（规则 4 合法）。
  - 工具 scan：`check-bigint-id-types.mjs scan` 实际修改 0 列 / DEFERRED 0 / 告警 0（19 域终态）。
  - 手写面：`LogisticsFreightProvider` setPartnerId String 直传 + asLong 零残留；`ErpLogDeliveryBookingBizModel:188` idOrder 数值序在位；main 手写 Long 残留 = api-bean delVersion + idOrder 合法。
  - 测试：`Long.parseLong`/`.longValue()`/`Map<Long` 零命中；快照 `_cases` 650 文件 json5 id String 形态实证（`"id": "2"`）+ 数字形态零 + RECORDING/saveOutput 注解零残留。
  - page.yaml：shipment-tracking `$sid:String` + `${shipmentId || null}` + adaptor `!== ''` 守卫；log-web `:Long` 复扫零。
  - 登记册：11 条 logistics backward 指针全含「M3.10 已兑付」note（160-163/255/219-224 行号逐一核证）；retireOwner=M3.10 零条；fail-closed 解析通过。
  - 文档链：§6.19 追注（三处补登）+ roadmap 位次 19 done + 头部 mission 里程碑注记 + M4.1 保持 todo + owner doc delivery-window.md 注记（引用本计划）+ `docs/logs/2026/08-23.md` M3.10 条目（验证状态全绿）。
  - 文本一致性：四 Phase Status 全 completed + 全条目 `[x]`；`TestErpLogDeliveryBooking` 复验 9/9 绿。
- Evidence: 本节上述独立抽检记录 + 执行者验证（7 链 `mvn clean install -DskipTests` BUILD SUCCESS ×2 + `mvn test -pl erp-log-service,erp-log-web` 66/66 绿 + web 0 tests 治理排除）。

Follow-up:

- 无（已确认缺陷不得出现在此处）
