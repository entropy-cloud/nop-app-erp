# 2026-08-25-0330-3-adjudicate-integration-test-defect-routing ASN 过账悬挂裁决修复 + 集成测试勘误↔缺陷路由通道绑定

> Plan Status: active
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

Status: planned
Targets: 裁决记录（落本计划 `Draft Review Record` 后执行日志 + owner doc 注记）
Skill: nop-debugging

- Item Types: `Decision | Proof`
- Prereqs: 无

- [ ] Proof: 实证设计——最小可复现实验（非推测）：①生产路径取向：以 app-erp-all 装配（或 b2b-service 集成测试上下文）走 GraphQL `ErpB2bAsn__save` → `matchPurchaseOrder` → `createReceiveFromAsn` 真实链，断言产物 receive.orgId；②平台机制取向：nop-entropy 平台 save 路径是否对 orgId 列有 auto-fill/default 逻辑（ORM 列定义 + `IEntityDao` 装饰链源码级核验）。两取向结论一致方可裁决；不一致时以生产路径取向为准并记录差异。
      - Skill: nop-debugging
- [ ] Decision: 裁决记录——选择（A 回填成立 / B 回填不成立或仅部分上下文）、证据、影响面（哪些上下文悬挂）；框架强制或明显的选择无需完整替代方案分析，但「部分上下文」情形必须枚举已证上下文清单。
      - Skill: none

Exit Criteria:

- [ ] 裁决记录含双取向实证证据（后续阶段阻塞解除）

### Phase 2 - 裁决分支落地（ASN orgId 修复或契约注记）

Status: planned
Targets: `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`、`app-erp-all/src/test/java/io/nop/app/all/it/TestErpC19B2bAsnAutoReceiveLandedCost.java`、`docs/design/b2b/`、`docs/bugs/`
Skill: nop-backend-dev

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1

- [ ] Fix（分支 B 触发时）: Processor 补 orgId 透传（PO 头 orgId 同型透传，对齐 supplierId/warehouseId 既有模式）；`docs/bugs/2026-08-25-asn-receive-orgid-missing-writer-posting-dangling.md` 登记（机制/裁决证据/影响面）。
      - Skill: nop-backend-dev
- [ ] Fix（分支 B 触发时）: C19 遮蔽解除——移除 DAO fixture orgId 补值（或保留并注明 `workaround-for` + 差异源：如 fixture 仅为控制快照稳定性）；受影响快照重录（RECORDING→CHECKING 往返，通配纪律不变）。
      - Skill: nop-testing
- [ ] Add（分支 A 触发时）: b2b owner doc（ASN 链节）注记平台 orgId 回填契约（上下文清单）；C19 fixture 注释修正为 `workaround-for` 标注（补值非生产必需的说明）。
      - Skill: none
- [ ] Fix（两分支共通）: `docs/audits/arm-index.md` 反向回填——OA-03 控制点行（ASN 链 orgId 缺 writer → 过账悬挂）登记并回链源审计。
      - Skill: none
- [ ] Proof: 分支 B → `mvn test -pl module-b2b/erp-b2b-service` + `mvn test -pl app-erp-all`（54/0/0/1）全绿；分支 A → `mvn test -pl app-erp-all`（54/0/0/1）全绿（无生产代码变更时仅回归证明）。
      - Skill: nop-testing

Exit Criteria:

- [ ] 裁决分支落地物齐备（修复+登记 或 注记+标注，二者必居其一全部完成）
- [ ] C19 测试不再无声遮蔽生产悬挂（fixture 若在则带 `workaround-for` 标注）

### Phase 3 - C18/C20b 批量分诊 + 勘误↔路由通道绑定立法

Status: planned
Targets: `docs/bugs/`、`docs/design/contract/`、`docs/design/drp/`、`docs/design/integration-testing.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 2（通道规则先行试用于 Phase 2 登记物）

- [ ] Decision: C18 分诊——`postSettlement` 产物形态（DRAFT credit memo 非凭证、无红字分录）对照 `docs/design/contract/` 返利结算语义与 finance 过账契约，裁决「实现缺口（登记 bugs + successor 触发条件）」或「设计漂移（owner doc 修正确认 + 勘误(4) 转正式裁决记录）」；结论 + 证据落盘。裁决为实现缺口时同步履行三绑定义务之③：遮蔽测试 `TestErpC18CtLifecycleRebate` 断言处标注 `workaround-for`（按本阶段立法规则回溯适用首个实例群）。
      - Skill: none
- [ ] Decision: C20b 分诊——仿真参数不写回（提升后重算回落基线）对照 `docs/design/drp/` 仿真语义，同上二岔裁决；结论 + 证据落盘；实现缺口分支同样标注 `TestErpC20bDrpSimulationPromote` 遮蔽断言 `workaround-for`。
      - Skill: none
- [ ] Add: 勘误↔缺陷路由通道绑定规则落盘 `integration-testing.md`（勘误纪律节）：后续集成测试/同类 mission 发现实现-设计实质差距时，三绑定义务生效——①勘误落盘（现状）；②bugs/backlog 路由（新增）；③测试以 fixture/断言遮蔽生产行为处标注 `workaround-for`（新增）；并回链 open-audit 结论 #4 作为立法来源。
      - Skill: none

Exit Criteria:

- [ ] C18/C20b 各有显式分诊结论（二岔之一 + 证据），无「未裁决」残留
- [ ] 实现缺口分支的 successor 触发条件已具名（无匿名延迟）
- [ ] 绑定规则成文且以本计划 Phase 2 产物为首个应用实例

## Draft Review Record

- Independent draft review iteration 1: acceptable (task `ses_fca9ae0eaffem0tozwRdjxMErY`, fresh session) — 全部基线断言活仓核验通过（Processor 零 orgId + 字段清单逐一核对、PO/Receive ORM 有 orgId 列可透传、C19 fixture :169-173 补 orgId 且零 workaround-for、三处勘误原文 :492/:507/:541、arm-index P1 汇总区 :455、四 owner doc 目录在位、slack 词扫描零命中）；OA-03 双分支闭环 + C18/C20b 分诊 + 通道立法全覆盖；分支条件性合规（guide 规则 9：Phase 1 Decision 门控 + 二必居一退出）；C18/C20b Decision-only 分诊经审查确认不违反规则 13（审计原文定性为「待分诊项」非已确认缺陷，分诊不可延迟且缺口分支落地 bugs+具名触发条件 = 显式 successor 终态）。无阻塞项。采纳非阻塞注记：C18/C20b 实现缺口分支补三绑定之③（遮蔽测试 workaround-for 回溯标注）、Phase 3 退出标准补「successor 触发条件已具名」。已按注记修订，共识达成 → active。

## Closure Gates

- [ ] 范围内行为完成（裁决 + 分支落地 + C18/C20b 分诊 + 通道立法 + arm-index 回填）
- [ ] 相关文档对齐（b2b/contract/drp owner docs + integration-testing.md + bugs）
- [ ] 已运行验证：分支 B 含生产代码变更 → `mvn test -pl module-b2b/erp-b2b-service` + `mvn test -pl app-erp-all`（54/0/0/1）+ `mvn clean install -DskipTests`；分支 A（无生产代码变更）→ `mvn test -pl app-erp-all`（54/0/0/1）回归证明；compliance checker 零漂移或逐项归因
- [ ] 无范围内项目降级为 deferred/follow-up（OA-03 主缺陷闭环；C18/C20b 为分诊类 Decision 项，其修复归 successor 属裁决产物而非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 全仓 #11 家族「缺 writer」站点扫描

- Classification: `optimization candidate`
- Why Not Blocking Closure: OA-03 单点已闭环；全仓扫描是审计方法论增强，非本 P1 的构成义务。
- Successor Required: yes（触发条件：下一轮 arm/审计 mission 或过账悬挂类 bug 再现时）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- （无；已确认缺陷不得出现在此处）
