# 2026-08-25-0330-3-adjudicate-integration-test-defect-routing ASN 过账悬挂裁决修复 + 集成测试勘误↔缺陷路由通道绑定

> Plan Status: completed
> Last Reviewed: 2026-08-25
> Source: 开放式审计 `docs/audits/2026-08-24-2233-open-audit-integration-test.md` P1 发现 OA-03（含 C18/C20b 同型待分诊项与路由通道立法诉求）
> Related: `docs/plans/2026-08-24-0749-2-b9-c18-c19-contract-b2b.md`（C19 fixture 遮蔽落地处）、`docs/plans/2026-08-25-0330-1-aggregate-notify-cs-seeds-into-app-init-data.md`、`docs/plans/2026-08-25-0330-2-fix-closeperiod-fx-flush-profit-loss.md`（同批计划 1/2，先行）
> Audit: required

## Current Baseline

- **OA-03 主缺陷（缺 writer，grep 实证）**：`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java:49-90` `createReceiveFromAsn` 建 `ErpPurReceive` 头时写入 code/orderId/supplierId/warehouseId/currencyId/businessDate/docStatus/approveStatus/receiveStatus/remark——**零 orgId 写入**（全文件 `rg orgId` 零命中；对照同链 PO 有 orgId 可透传）。
- **后果链（C19 勘误(2)，integration-testing.md:507）**：orgId=null → 账套解析为 null → 收货 approve 内建过账零凭证 → `posted` 永久悬挂 false。属已知失败模式 #11（业财过账悬挂）家族的「缺 writer」新控制点（R1.16/P1-MA2-074 清扫的是 catch 吞异常站点，非缺 writer 站点）。
- **测试遮蔽**：`TestErpC19B2bAsnAutoReceiveLandedCost` 以 DAO fixture 补全 orgId 后全绿 → 生产悬挂对黄金路径回归套件不可见，且无 `workaround-for` 标注。
- **未裁决未知数**：生产 GraphQL/webhook 上下文下平台是否自动回填 orgId——从未被裁决或文档化（audit OA-03 明示此为路由义务的构成部分）。
- **同型未路由项（audit 佐证，供同通道批量分诊）**：
  - C18 勘误(4)（integration-testing.md:492）：返利 `postSettlement` 产物 = DRAFT 负额 credit memo，**非已过账凭证、无红字反向分录**——实现-设计实质差距仅勘误落盘。
  - C20b 勘误(3)（integration-testing.md:541）：DRP 仿真覆盖不写回参数表 → 提升后 `runDrp` 重算回落基线参数——同上。
- **路由通道缺失（open-audit 结论 #4）**：集成测试 mission 的「勘误」口径未与缺陷路由通道接通——发现实现-设计实质差距时，勘误落盘、backlog/bugs 路由、测试遮蔽处 `workaround-for` 标注三者未绑定，黄金路径套件会系统性把生产缺陷钉死为期望行为（OA-01/02/03 均为此形态的实例）。
- **登记基建在位**：`docs/bugs/` 命名范式 `YYYY-MM-DD-slug.md`；arm-index 有 P1 发现汇总区可反向回填。

## Goals

- **平台 orgId 回填行为获得裁决**：以可复现实证（非推测）确定生产 GraphQL mutation / webhook 上下文是否自动回填 `orgId`，裁决记录含证据。
- **裁决分支落地（二者必居其一，均关闭 OA-03）**：
  - 分支 A（回填成立）→ b2b owner doc 注记平台回填契约 + C19 fixture 注释修正为 `workaround-for` 语义标注（说明 fixture 补值仅为快照稳定性，非生产必需）；
  - 分支 B（回填不成立/仅部分上下文）→ Processor 补 orgId 透传 writer（`receive.setOrgId(po.getOrgId())` 同型）+ C19 去 fixture 遮蔽（或保留但带 `workaround-for` 注明差异源）+ `docs/bugs/` 登记。
- **C18/C20b 同型批量分诊完成**：每项裁决为「设计为准的实现缺口（登记 bugs + successor 触发条件）」或「实现为准的设计漂移（owner doc 修正确认）」，结论与证据落盘；不在本计划内实施其修复（结束面 = 分诊与路由，非各自修复）。
- **勘误↔缺陷路由通道绑定规则落盘**：集成测试（及后续同类 mission）发现实现-设计实质差距时的三绑定义务（勘误落盘 + bugs/backlog 路由 + 测试遮蔽处 `workaround-for` 标注）成为成文规则。
- OA-03 缺陷控制点反向回填 arm-index（#13 对偶形态闭环）。

## Non-Goals

- 不修复 C18 返利过账缺口与 C20b 参数写回缺口本身（分诊裁决后归 successor 计划；避免与 OA-03 修复混在同一结束面）。
- 不清扫已知失败模式 #11 的其他潜在缺 writer 站点（全仓扫描归 backlog follow-up，见 audit P2 处置）。
- 不处理 OA-01（种子聚合）/ OA-02（FX flush）——归本批计划 1/2。
- 不改造 xwf/webhook 平台机制。

## Task Route

- Type: `bug investigation` + `implementation-only change`（裁决为主 + 条件性单点修复）
- Owner Docs: `docs/design/b2b/`（ASN 链语义）、`docs/design/finance/posting.md`（账套解析/orgId 过账前置）、`docs/design/integration-testing.md`（勘误纪律规则落点）、`docs/design/contract/` + `docs/design/drp/`（C18/C20b 分诊落点）
- Skill Selection Basis: 裁决实证与（分支 B）Processor 修复 → `nop-debugging`（根因实证纪律）+ `nop-backend-dev`（修复落点）；C19 快照若变 → `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - 平台 orgId 回填行为裁决

Status: completed
Targets: 裁决记录（落本计划 `Draft Review Record` 后执行日志 + owner doc 注记）
Skill: nop-debugging

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] Proof: 实证设计——最小可复现实验（非推测）：①生产路径取向：以 app-erp-all 装配（或 b2b-service 集成测试上下文）走 GraphQL `ErpB2bAsn__save` → `matchPurchaseOrder` → `createReceiveFromAsn` 真实链，断言产物 receive.orgId；②平台机制取向：nop-entropy 平台 save 路径是否对 orgId 列有 auto-fill/default 逻辑（ORM 列定义 + `IEntityDao` 装饰链源码级核验）。两取向结论一致方可裁决；不一致时以生产路径取向为准并记录差异。
      - Skill: nop-debugging
- [x] Decision: 裁决记录——选择（A 回填成立 / B 回填不成立或仅部分上下文）、证据、影响面（哪些上下文悬挂）；框架强制或明显的选择无需完整替代方案分析，但「部分上下文」情形必须枚举已证上下文清单。
      - Skill: none

**裁决记录（2026-08-25，双取向实证）——选择 B：回填不成立（全上下文）**

- ①生产路径取向实证：`TestErpC19B2bAsnAutoReceiveLandedCost` 真实链（app-erp-all 装配，GraphQL `ErpPurOrder__save`(orgId=2) → `ErpB2bAsn__save` → `matchPurchaseOrder` → `ErpB2bAsn__createReceiveFromAsn`）在 fixture 前插入临时断言 `assertNull(draft.getOrgId())` → **测试通过**（`mvn test -pl app-erp-all -Dtest=TestErpC19B2bAsnAutoReceiveLandedCost`：1/0/0/0 BUILD SUCCESS）——生产 GraphQL mutation 上下文下产物 receive.orgId = null，平台不回填。
- ②平台机制取向实证（源码级）：
  - ORM 列定义：`module-purchase/model/app-erp-purchase.orm.xml:693` `ErpPurReceive.orgId` 列**无 `defaultValue`**（对照同表 `exchangeRate` 列 defaultValue=1 兜底）。
  - nop-entropy 平台层：`nop-orm`/`nop-core`/`nop-service-framework` 全源码 `rg orgId|ORG_ID` 零命中（IEntityDao 装饰链 / OrmSession / 拦截器均无 orgId 概念——orgId 是应用列非平台列）。
  - `CrudBizModel.defaultPrepareSave`（nop-biz CrudBizModel.java:771）仅做状态机 initState，无任何字段回填。
  - 应用层惯例佐证：全仓 100+ 处派生实体显式 `setOrgId(src.getOrgId())` 透传（如 `ReceiveStockMoveBuilder.java:33`、`ErpInvLandedCostProcessor.java:264`、`RequisitionToOrderConverter.java:56`）——orgId 写入义务在应用 writer，ASN Processor 为唯一缺 writer 站点。
- 两取向结论**一致**（均为不回填），无需差异仲裁。
- 影响面（悬挂上下文清单）：`createReceiveFromAsnProcessor` 唯一入口 = `ErpB2bAsnBizModel.createReceiveFromAsn`（GraphQL mutation）→ 所有触达该 mutation 的上下文（交互 UI 流 + webhook 编排流汇于此）全数悬挂，无豁免上下文。下游级联：`receive.orgId=null` → `ReceiveStockMoveBuilder` 透传 null → 移动/余额/台账 orgId=null → 过账事件账集解析 null → 零凭证 → `posted` 永久悬挂 false。

Exit Criteria:

- [x] 裁决记录含双取向实证证据（后续阶段阻塞解除）

### Phase 2 - 裁决分支落地（ASN orgId 修复或契约注记）

Status: completed
Targets: `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`、`app-erp-all/src/test/java/io/nop/app/all/it/TestErpC19B2bAsnAutoReceiveLandedCost.java`、`docs/design/b2b/`、`docs/bugs/`
Skill: nop-backend-dev

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1

- [x] Fix（分支 B 触发时）: Processor 补 orgId 透传（PO 头 orgId 同型透传，对齐 supplierId/warehouseId 既有模式）；`docs/bugs/2026-08-25-asn-receive-orgid-missing-writer-posting-dangling.md` 登记（机制/裁决证据/影响面）。
      - Skill: nop-backend-dev
- [x] Fix（分支 B 触发时）: C19 遮蔽解除——移除 DAO fixture orgId 补值（或保留并注明 `workaround-for` + 差异源：如 fixture 仅为控制快照稳定性）；受影响快照重录（RECORDING→CHECKING 往返，通配纪律不变）。
      - Skill: nop-testing
- [x] Add（分支 A 触发时）: N/A——裁决为 B（平台无回填），分支 A 未触发。（裁决 B 共通面对齐已落：b2b owner doc `asn-processing.md` §实现约定补 orgId 透传契约注记 + 无回填裁决回链 bugs 登记。）
      - Skill: none
- [x] Fix（两分支共通）: `docs/audits/arm-index.md` 反向回填——OA-03 控制点行（ASN 链 orgId 缺 writer → 过账悬挂）登记并回链源审计。
      - Skill: none
- [x] Proof: 分支 B → `mvn test -pl module-b2b/erp-b2b-service` + `mvn test -pl app-erp-all`（54/0/0/1）全绿；分支 A → `mvn test -pl app-erp-all`（54/0/0/1）全绿（无生产代码变更时仅回归证明）。
      - Skill: nop-testing

**执行证据（2026-08-25，分支 B）**：

- Processor：`ErpB2bAsnCreateReceiveFromAsnProcessor` 建头序列补 `receive.setOrgId(po.getOrgId())`（紧跟 setOrderId，对齐 supplierId/warehouseId/currencyId 透传模式）。
- C19 去 fixture 遮蔽：移除 ormTemplate DAO fixture 块 + `IOrmTemplate` 死注入，改断言 `assertEquals("2", draft.getOrgId())`（透传实证）；javadoc 三处同步（链路描述 / Phase 2 Decision 勘误口径）。
- 快照重录：fixture 的额外 saveOrUpdateEntity 曾使 receive version +1——version 级联 = 9_receive_submit（2→1）/ 10_receive_approve（3→2）/ tables erp_pur_receive.csv VERSION（3→2），恰 3 处行级变更；@EnableSnapshot(saveOutput=true) 单方法重录后**通配纪律漂移已人工回正**（重录把既有 `*` 时间戳通配改为 `@var:` 引用——git checkout 回正后仅重应用 3 处 version 变更，`*` 通配全保留），CHECKING 往返绿。module-b2b 3 个收货快照用例 PO 种子 orgId 为空 → writer 写 null → 快照零变化（无需重录）。
- 验证：`mvn test -pl module-b2b/erp-b2b-service` = 80/0/0/0；`mvn clean install -DskipTests`（全仓 156 模块）= BUILD SUCCESS；`mvn test -pl app-erp-all` = **54/0/0/1 BUILD SUCCESS**（首轮 C16 出现 1ms 时间戳 var 比较瞬时抖动 `updateTime .424 vs createTime_1 .423`——C16 case 目录 git 零触碰 + 单测重跑绿 + 复跑全量绿，判定与本次变更无关的既有脆弱基线）。
- 登记与回填：bugs 登记（机制/双取向裁决证据/影响面/修复/successor 触发条件）；arm-index :728 OA-03 行更新为已修复 + 控制点表述 + bug/plan 回链；integration-testing.md C19 勘误(2) 更新为「orgId 勘误已修复」（对齐 :409 C13 勘误(2) FX 修复先例风格）。

Exit Criteria:

- [x] 裁决分支落地物齐备（修复+登记 或 注记+标注，二者必居其一全部完成）
- [x] C19 测试不再无声遮蔽生产悬挂（fixture 若在则带 `workaround-for` 标注）

### Phase 3 - C18/C20b 批量分诊 + 勘误↔路由通道绑定立法

Status: completed
Targets: `docs/bugs/`、`docs/design/contract/`、`docs/design/drp/`、`docs/design/integration-testing.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 2（通道规则先行试用于 Phase 2 登记物）

- [x] Decision: C18 分诊——`postSettlement` 产物形态（DRAFT credit memo 非凭证、无红字分录）对照 `docs/design/contract/` 返利结算语义与 finance 过账契约，裁决「实现缺口（登记 bugs + successor 触发条件）」或「设计漂移（owner doc 修正确认 + 勘误(4) 转正式裁决记录）」；结论 + 证据落盘。裁决为实现缺口时同步履行三绑定义务之③：遮蔽测试 `TestErpC18CtLifecycleRebate` 断言处标注 `workaround-for`（按本阶段立法规则回溯适用首个实例群）。
      - Skill: none
- [x] Decision: C20b 分诊——仿真参数不写回（提升后重算回落基线）对照 `docs/design/drp/` 仿真语义，同上二岔裁决；结论 + 证据落盘；实现缺口分支同样标注 `TestErpC20bDrpSimulationPromote` 遮蔽断言 `workaround-for`。
      - Skill: none
- [x] Add: 勘误↔缺陷路由通道绑定规则落盘 `integration-testing.md`（勘误纪律节）：后续集成测试/同类 mission 发现实现-设计实质差距时，三绑定义务生效——①勘误落盘（现状）；②bugs/backlog 路由（新增）；③测试以 fixture/断言遮蔽生产行为处标注 `workaround-for`（新增）；并回链 open-audit 结论 #4 作为立法来源。
      - Skill: none

**分诊证据与落点（2026-08-25）**：

- **C18 裁决 = 实现为准的设计漂移（owner doc 修正确认）**：owner doc `contract/volume-discount.md` §结算流程本就记载「负额发票经 IDaoProvider 直接持久化 + **后续**走标准审核过账（红字凭证） + Settlement→POSTED + isSettled→true」，实仓 `ErpCtRebateSettlementPostSettlementProcessor` 逐项一致（负额发票 DRAFT + orgId 透传 agreement / 计提 isSettled+settledDate / 状态机 DRAFT→POSTED）；「凭证生成 + 红字同向取负」为 C18 用例层 1 期望的过度表述（owner doc 从未承诺结算时点产凭证或计提行取负——红字凭证来自贷项发票后续标准审批过账，路径可达：sal/pur invoice 审批面无负额阻断守卫，负额先例 = SALES_RETURN 负 AR 辅助账/ReceiptSettler negate）；架构层裁决在位 = `architecture/posting-exemptions.md` §ErpCtRebateSettlementBizModel（O-4 豁免 + 补偿「须人工审核后提交审批」+ **具名 successor 收敛条件**「purchase/sales 提供 purpose-built createCreditMemo」）。落点：勘误(4) 追加分诊裁决记录（integration-testing.md）+ `volume-discount.md` §实现约定补时点语义注记。无 bugs 义务（无生产缺陷）、无 workaround-for 义务（断言为文档化既定行为、无遮蔽）。
- **C20b 裁决 = 实现为准的设计漂移（owner doc 语义确认注记）**：「重算回落基线参数」是仿真隔离原则的直接推论——`manufacturing/simulation-engine.md` 明文「不修改主数据/`ErpDrpParameter`」+ §转正路径（Decision D）定义 promote = 复制 computed 行到新 DRAFT plan +「走既有单次引擎释放路径」，设计从未定义「携带仿真参数重算」（若携带反而要求参数落库、与隔离原则矛盾）；实仓逐项一致（复制行为 SUGGESTED 保留仿真口径 SS / DRAFT / ARCHIVED 防重复转正）。落点：勘误(3) 追加分诊裁决记录 + `simulation-engine.md` §Decision D 后补转正后重算语义注记。无 bugs 义务、无 workaround-for 义务。
- **立法落盘**：`integration-testing.md` 新增 **§3.5 勘误↔缺陷路由通道绑定纪律（三绑定义务）**——①勘误落盘（既有）+ ②bugs/backlog 路由（缺口分支须具名 successor 触发条件，禁匿名延迟）+ ③测试遮蔽处 `workaround-for` 标注；回链 open-audit 结论 #4 + 立法 plan 本文件；首个应用实例群（OA-03 全三义务 / OA-02 ①② / C18、C20b ②之漂移分支）具名在节内。

Exit Criteria:

- [x] C18/C20b 各有显式分诊结论（二岔之一 + 证据），无「未裁决」残留
- [x] 实现缺口分支的 successor 触发条件已具名（无匿名延迟）——C18/C20b 均裁决为漂移分支无缺口登记义务；C18 记录中仍回链了在位的具名 successor 收敛条件（posting-exemptions createCreditMemo）
- [x] 绑定规则成文且以本计划 Phase 2 产物为首个应用实例

## Draft Review Record

- Independent draft review iteration 1: acceptable (task `ses_fca9ae0eaffem0tozwRdjxMErY`, fresh session) — 全部基线断言活仓核验通过（Processor 零 orgId + 字段清单逐一核对、PO/Receive ORM 有 orgId 列可透传、C19 fixture :169-173 补 orgId 且零 workaround-for、三处勘误原文 :492/:507/:541、arm-index P1 汇总区 :455、四 owner doc 目录在位、slack 词扫描零命中）；OA-03 双分支闭环 + C18/C20b 分诊 + 通道立法全覆盖；分支条件性合规（guide 规则 9：Phase 1 Decision 门控 + 二必居一退出）；C18/C20b Decision-only 分诊经审查确认不违反规则 13（审计原文定性为「待分诊项」非已确认缺陷，分诊不可延迟且缺口分支落地 bugs+具名触发条件 = 显式 successor 终态）。无阻塞项。采纳非阻塞注记：C18/C20b 实现缺口分支补三绑定之③（遮蔽测试 workaround-for 回溯标注）、Phase 3 退出标准补「successor 触发条件已具名」。已按注记修订，共识达成 → active。

## Closure Gates

- [x] 范围内行为完成（裁决 + 分支落地 + C18/C20b 分诊 + 通道立法 + arm-index 回填）
- [x] 相关文档对齐（b2b/contract(+mfg simulation-engine)/drp owner docs + integration-testing.md + bugs + arm-index + 源审计 Remediation 行）
- [x] 已运行验证：分支 B 含生产代码变更 → `mvn test -pl module-b2b/erp-b2b-service`（**80/0/0/0**）+ `mvn test -pl app-erp-all`（**54/0/0/1**，首轮 C16 1ms 时间戳 var 比较瞬时抖动——C16 case 目录 git 零触碰 + 单测重跑绿 + 全量复跑绿，与本次变更无关）+ `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）；`bash docs/audits/nop-compliance-checker.sh` **全规则 actual==baseline 零漂移**（R1d=14/R2a=34/R2b=237/R2c=1505/R2d=38/R3=5/R6=2/R10=12/R12a/b/c=70/66/41，对照 compliance-baseline.md §BASELINE）
- [x] 无范围内项目降级为 deferred/follow-up（OA-03 主缺陷闭环；C18/C20b 为分诊类 Decision 项，其修复归 successor 属裁决产物而非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全仓 #11 家族「缺 writer」站点扫描

- Classification: `optimization candidate`
- Why Not Blocking Closure: OA-03 单点已闭环；全仓扫描是审计方法论增强，非本 P1 的构成义务。
- Successor Required: yes（触发条件：下一轮 arm/审计 mission 或过账悬挂类 bug 再现时）

## Closure

Status Note: closed（2026-08-25 三 Phase 全部完成 + 全量验证绿（b2b-service 80/0/0/0 + app-erp-all 54/0/0/1 + install 156 模块 + compliance 零漂移）+ 独立结束审计 PASS；Plan Status → completed。）

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（fresh session task `ses_fc9bbd5a2ffehJjamSdwiWv2GQ`，非执行者）
- Evidence: **CLOSURE VERDICT: PASS，零阻塞项**——11 项活仓核验全过：①计划三 Phase Status: completed + 全 item/gate/Exit Criteria [x]（Plan Status active 为审计时点预期态，审计后翻 completed）；②Processor :71 `receive.setOrgId(po.getOrgId())` 位于 setOrderId/setSupplierId 之间；③C19 零 runInSession/IOrmTemplate/workaround 残留 + :167 orgId 透传断言（OA-03 注释）；④快照纪律 = git diff 恰 3 文件各 1 行（9_submit 2→1 / 10_approve 3→2 / CSV VERSION 3→2），`*` 通配全保留、@var 仅既有；⑤bug 文档五节齐备（现象/根因双取向证据/证据链/修复/状态 fixed）；⑥arm-index :728 OA-03 已修复 + 控制点表述 + bug/plan 双回链；⑦integration-testing.md 四处（C19 勘误(2) 已修复 + §3.5 三绑定义务立法含 open-audit 回链 + C18 勘误(4) 漂移分诊 + C20b 勘误(3) 漂移分诊）；⑧三个 owner doc 注记（b2b asn-processing orgId 透传契约 / contract volume-discount 时点语义 / mfg simulation-engine 转正后重算语义）；⑨源审计 Remediation 行已更新三计划 completed；⑩无越权（C18/C20b 漂移分支零 workaround-for 义务 + 零新 bugs 文件——与分诊结论一致）；⑪MINOR 一项 = 日志条目待补（执行者已于审计后落盘 `docs/logs/2026/08-25.md`）。

Follow-up:

- （无；已确认缺陷不得出现在此处）
