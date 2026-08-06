# 2026-08-06-1926-3 rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime 预留量并发扣减与 STOCK_PARTIAL 齐套可用量运行时确认（reserved 恒为 0 当前行为）

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.1 + A4.2.2（合并：MA4 运行时行为验证 — A1.8 §7 SP-1/SP-2 同 owner doc[manufacturing/mrp.md + material-reservation.md + flow-overview.md]同结果表面[预留写路径 Deferred 下当前 reserved=0 行为的运行时安全性确认]；SP-1 = 预留量并发扣减运行时行为[reserved 恒为 0，多工单并发领料 stock move bookkeeper negative-stock 防护兜底]；SP-2 = STOCK_PARTIAL 强制开工后领料 KitAvailabilityChecker 只读路径补料后可用量[无缓存/陈旧读]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.1 + A4.2.2；存疑点来源 `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md` §7 SP-1/SP-2
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.1/A4.2.2 实体行）、`docs/plans/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（A1.8 done，P1-RC-008 预留写路径 Deferred 已登记 + §7 SP-1/SP-2/SP-3 已落盘）、`docs/audits/arm-index.md`（P1-RC-008 finding 行）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：grep reservedQty writer 全集[确认恒为 0] + KitAvailabilityChecker 只读路径可用量查询 + stock move bookkeeper negative-stock 防护 + config consumption 模式 + 并发场景推理）。范式对齐 A4.1.18（done — PC-3 AR/AP reminder 非阻塞运行时行为同 config-gated 路径确认）+ A4.1.21（done — 年末反结账边界同边界行为确认）。

- **存疑点原文**（A1.8 报告 §7 SP-1/SP-2，`2026-08-02-2042-2-...-a1-8-...md` §7:243-244）：
  - **SP-1**：「预留量并发扣减运行时行为：当前 reserved 恒为 0（无 writer），多工单同时通过齐套校验后并发领料时，stock move bookkeeper 的 negative-stock 防护（P0-MA2-020 UK + 余额守恒）是否在所有并发场景下兜底（无 silent split-quantity corruption）。」触发条件：多工单并发领料同一物料同一仓库。
  - **SP-2**：「STOCK_PARTIAL 强制开工后领料可用量校验运行时：部分齐套强制开工（UC-MFG-04，config-gated consumption != STRICT）后，缺件部分后续补料时，KitAvailabilityChecker 只读路径是否正确反映补料后的可用量（无缓存/无陈旧读）。」触发条件：STOCK_PARTIAL 强制开工后补料。

- **关联既有 finding**：
  - **P1-RC-008**（arm-index）：UC-MFG-05/08 物料预留写路径 Deferred——制造域无审核触发预留的写实现（ErpMfgWorkOrderProcessor 无预留创建调用），预留实体在库存域 ErpInvReservation*（写接口存在但制造域未接线），齐套校验 = KitAvailabilityChecker 只读。§4 Q1（L1 为准）+ §5 Q4（P0/P1 必须实现）→ P1。触及跨域 + ORM 结构（reservationStatus 字段）须 ask-first。
  - **SP-1/SP-2 是 P1-RC-008 的运行时安全性子问题**：在预留写路径 Deferred（reserved 恒为 0）的当前状态下，并发领料 + STOCK_PARTIAL 补料是否依赖 negative-stock 防护兜底（而非预留隔离），是否存在 silent split-quantity corruption 或陈旧读风险。

- **需求契约（L1 权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-05（`:90`）工单审核触发物料预留 + UC-MFG-08（`:144`）工单取消/完工释放预留。当前实现 Deferred（owner doc material-reservation.md `:9-16` 明确）。本验证确认 Deferred 状态下的运行时安全性。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **reservedQty writer 全集**（待运行时 census）：A1.8 报告静态确认 `ErpInvStockBalance.reservedQty` 无 writer（reserved 恒为 0）。本验证运行时复核确认。
  - **KitAvailabilityChecker**（`module-manufacturing/erp-mfg-service/.../workorder/KitAvailabilityChecker.java`）：展开工单 BOM 子件 × plannedQuantity，对照 inventory 余额可用量（n = onHand − reserved）。resolveBomId wo.bomId 优先。只读路径，不写预留。
  - **stock move bookkeeper**（库存域）：领料出库移动单 DONE 扣减 onHand。negative-stock 防护（P0-MA2-020 UK + 余额守恒）在并发场景下兜底。
  - **config consumption 模式**：config-gated（STRICT vs 非 STRICT），决定 STOCK_PARTIAL 强制开工可达性。待运行时核验默认值。

- **既有证据（复用输入）**：
  - A1.8 §5（P1-RC-008 静态裁决：预留写路径 Deferred → P1，触及跨域 + ORM 须 ask-first）
  - A1.8 §7 SP-1/SP-2（静态存疑点清单）
  - P0-MA2-020（stock move bookkeeper negative-stock 防护 UK + 余额守恒，resolved）

- **剩余差距**：reserved 恒为 0 状态下，多工单并发领料的 negative-stock 防护兜底完整性（SP-1）+ STOCK_PARTIAL 补料后 KitAvailabilityChecker 只读路径可用量正确性（SP-2）未做运行时确认。本验证闭合 P1-RC-008 在当前 Deferred 状态下的运行时安全性裁决。

- **保护区域**：只读评估（grep reservedQty writer + KitAvailabilityChecker 只读路径 + stock move bookkeeper 防护 + config 默认值 + 并发场景推理），不触及 ORM/代码逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若发现风险，登记 finding 归 MR1；修复 = 预留写路径实现触及跨域 + ORM 结构须 ask-first）。

## Goals

- reservedQty writer 全集 census（SP-1 前置）：grep 全 module-inventory + module-manufacturing `reservedQty` / `setReservedQty` writer，运行时复核确认 reserved 恒为 0（无 writer 或仅初始化为 0）。产出 writer census 矩阵（类 × 方法 × 行号 × 写/读）。
- negative-stock 防护并发兜底核验（SP-1 核心）：核验 stock move bookkeeper 的 negative-stock 防护（P0-MA2-020 UK + 余额守恒）在多工单并发领料同一物料同一仓库场景下是否兜底（无 silent split-quantity corruption）。给出防护机制 file:line 证据 + 并发场景推理。
- KitAvailabilityChecker 只读路径可用量正确性核验（SP-2 核心）：核验 KitAvailabilityChecker.checkAvailability 查询 ErpInvStockBalance.availableQuantity 是否实时读（无缓存/无陈旧读）。STOCK_PARTIAL 强制开工后补料，二次齐套校验是否反映补料后的可用量。给出查询路径 file:line 证据。
- config consumption 模式核验：确认 config consumption 默认值（STRICT vs 非 STRICT），决定 STOCK_PARTIAL 强制开工可达性 + 预留隔离模式。
- 并发场景推理（SP-1）：多工单同时通过齐套校验（reserved=0 故同时看到全部可用量）后并发领料，stock move bookkeeper 按到达顺序扣减 onHand，若总量 > onHand 则最后一个/几个工单领料失败（negative-stock 防护抛异常）。确认无 silent split-quantity corruption（即不会出现部分工单领料成功但总量超扣的静默错误）。
- 对齐 P1-RC-008 + §2 判据给出运行时裁决：①若 negative-stock 防护并发兜底完整 + KitAvailabilityChecker 实时读无陈旧 → 维持 P1-RC-008 P1（预留写路径 Deferred 合规缺口，但当前行为不致活跃数据破坏），登记 P2 watch-only（并发竞争下的可用量超额承诺运营影响）或维持接受无新 finding；②若存在 silent split-quantity corruption 或陈旧读 → 登记 P1/P0（归 MR0/MR1）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 P1-RC-008[P1]分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复预留写路径**（若发现风险，登记 finding 归 MR0/MR1；修复 = 预留写路径实现触及跨域[库存域 ErpInvReservation* 写接口 + 制造域接线] + ORM 结构[reservationStatus 字段]须 ask-first + 独立 plan-audit）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-MFG-05/08 全部验收标准**（A1.8 §5 已判 P1[P1-RC-008]；本验证只评当前 Deferred 状态下运行时安全性差异）。
- **不展开 A1.8 §7 SP-3**（预留实现后 reservedQty/availableQuantity 一致性 = MR1 修复落地后 successor，Deps 不满足，归独立工作项 A4.2.3）。
- **不裁决 P1-RC-008 的 P1 定级本身**（A1.8 §5 已裁决 P1；本验证只评当前 Deferred 状态下运行时安全性是否需升 P0）。
- **不实际执行并发领料注入重现**（只读 reservedQty writer census + KitAvailabilityChecker 查询路径 + stock move bookkeeper 防护推理 + config 普查；真实并发注入重现属 MR1 修复验证范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（预留量并发扣减与 STOCK_PARTIAL 齐套可用量运行时确认 + P1-RC-008 当前 Deferred 状态安全性裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.1 + A4.2.2 行）+ `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md` §7 SP-1/SP-2 + §5 P1-RC-008 裁决（输入）+ `docs/design/manufacturing/material-reservation.md`（`:9-16` Deferred 说明）+ `docs/design/manufacturing/mrp.md` + `docs/design/flow-overview.md`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。预留并发扣减 + 齐套可用量评估需多维度归类（reservedQty writer census / negative-stock 防护并发兜底 / KitAvailabilityChecker 只读路径实时性 / config consumption 模式 / 并发场景推理 / P1-RC-008 分层 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep reservedQty writer + KitAvailabilityChecker 查询路径 + stock move bookkeeper 防护 + config 默认值 + 并发场景推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - reservedQty writer census + negative-stock 防护并发兜底 + KitAvailabilityChecker 只读路径核验

Status: completed
Targets: `docs/audits/2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（5/5 Proof items；Phase 2 含 Decision/Add）
- Prereqs: A4.2 done（展开器已追加 A4.2.1/A4.2.2 行）；A1.8 done（§7 SP-1/SP-2 已落盘 + §5 P1-RC-008 裁决已登记）

- [x] `Proof` reservedQty writer 全集 census（SP-1 前置）：grep 全 module-inventory + module-manufacturing `reservedQty` / `setReservedQty` writer，运行时复核确认 reserved 恒为 0（无 writer 或仅初始化为 0）。产出 writer census 矩阵（类 × 方法 × 行号 × 写/读）。同时核验 `availableQuantity` 派生公式（= onHand − reserved − locked）在 reserved=0 下退化为 onHand − locked。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 报告 §2（writer census 矩阵 W1-W5/R1-R3）+ §1（baseline 精化：字段实为 `reservedQuantity` 非 `reservedQty`；库存域 `ErpInvStockMoveProcessor.applyReservation:150` 有瞬时 writer，mfg 域零持久化 writer，mfg 领料经 stock move confirm net-zero apply-release 不持久化；`availableQuantity` 派生公式 `total − reserved − locked` 在 `StockMoveBookkeeper.recomputeAvailable:223-227` Java 层重算持久化）。
- [x] `Proof` negative-stock 防护并发兜底核验（SP-1 核心）：核验 stock move bookkeeper 的 negative-stock 防护（P0-MA2-020 UK + 余额守恒）在多工单并发领料同一物料同一仓库场景下是否兜底。给出防护机制 file:line 证据（validateAvailable / bookkeeper 扣减路径）+ 并发场景推理（多工单同时通过齐套校验[reserved=0 故同时看到全部可用量]→并发领料→按到达顺序扣减→超量抛异常 vs silent split-quantity corruption 二选一裁决）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 报告 §3（防护机制 4 层 file:line + 并发场景推理表）。裁决：**无 silent split-quantity corruption**（`updateBalanceWithRetry:256-328` versionProp 乐观锁 + P0-MA2-020 UK + 重试串行化，无 delta 丢失）；残留真并发 over-commitment 窗口归 A2.17 既有追踪（§去重协议）。
- [x] `Proof` KitAvailabilityChecker 只读路径可用量正确性核验（SP-2 核心）：核验 KitAvailabilityChecker.checkAvailability 查询 ErpInvStockBalance.availableQuantity 是否实时读（无缓存/无陈旧读）。给出查询路径 file:line 证据（checkAvailability → availableQuantity 查询 → ErpInvStockBalance 读取）。确认 STOCK_PARTIAL 强制开工后补料，二次齐套校验是否反映补料后的可用量。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 报告 §4（查询路径 4 步 file:line）。裁决：**无陈旧读**（`KitAvailabilityChecker.check:109` 每次 `findAllByQuery` 实时读，无缓存层）。SP-2 消解。
- [x] `Proof` config consumption 模式核验：确认 config consumption 默认值（STRICT vs 非 STRICT），决定 STOCK_PARTIAL 强制开工可达性 + 预留隔离模式。grep ErpMfgConstants + application.yaml + AppConfig 消费点。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 报告 §5。精化：`ErpMfgBom.consumption` 是 per-BOM 字段（orm.xml:201 nullable 无 default，dict FLEXIBLE/WARNING/STRICT）运行时 service 零消费；STOCK_PARTIAL 强制开工实际由 `erp-mfg.allow-partial-kit-start`（默认 FALSE）门控（`validateTransitionForStart:256-267`）→ 默认不可达。
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（并发领料是否致 silent corruption / 齐套是否陈旧读），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 报告 §6（MA4↔A5.6 边界声明）。

Exit Criteria:

- [x] reservedQty writer census 矩阵 + negative-stock 防护并发兜底 + KitAvailabilityChecker 只读路径证据落盘（全集，无遗漏），每条有证据（file:line）
- [x] 并发场景推理有明确结论（silent corruption 不存在/存在 + 陈旧读不存在/存在），与 P0-MA2-020[resolved] + P1-RC-008[P1]分层一致

### Phase 2 - 运行时安全性裁决 + finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-008 注记更新或新 finding，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 reservedQty writer census + negative-stock 防护并发兜底 + KitAvailabilityChecker 只读路径核验完成

- [x] `Decision` P1-RC-008 当前 Deferred 状态运行时安全性裁决（方法论 §2 判据 + 三源对照）：①若 negative-stock 防护并发兜底完整 + KitAvailabilityChecker 实时读无陈旧 → 维持 P1-RC-008 P1（预留写路径 Deferred 合规缺口，但当前行为不致活跃数据破坏），登记 P2 watch-only（并发竞争下可用量超额承诺运营影响）或维持接受无新 finding；②若存在 silent split-quantity corruption 或陈旧读 → 登记 P1/P0（归 MR0/MR1）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 P1-RC-008[P1] + P0-MA2-020[resolved]分层一致。
      - Skill: none
      - Evidence: 报告 §7（§2 判据三源复核表 + 三源对照 + 分层一致性）。裁决：**维持 P1-RC-008 P1，不升 P0，不降级，无新 finding**（无 silent corruption + 无陈旧读 → 当前 Deferred 状态运行时安全；残留并发归 A2.17）。
- [x] `Add` finding/注记更新：若 P2 watch-only → 新建 finding（P2-RC-xxx，并发竞争可用量超额承诺 watch-only）；若 P1/P0 → reopen 或新建 finding（P1-RC-xxx/P0-RC-xxx，归 MR0/MR1）；若维持接受无新 finding（防护完整 + 实时读）→ arm-index P1-RC-008 行追加运行时安全性确认注记。禁止未经比对新建重复 finding（grep arm-index 同域同控制点后裁决）。
      - Skill: none
      - Evidence: 报告 §8（grep arm-index 同域同控制点比对表 + arm-index P1-RC-008 行注记更新）。**不新建 finding**（残留并发 over-commitment 归 A2.17 既有追踪，§去重协议）；arm-index P1-RC-008 行已追加【MA4 A4.2.1+A4.2.2 运行时确认 done】注记（状态/分级/修复通道[MR1 ORM ask-first] 不变）。
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-RC-008[P1] / P0-MA2-020[resolved] / A1.8 §5/§7 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none
      - Evidence: 报告 §9（checker actual 计数 R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=229/R2c=1382/R2d=34，零生产代码变更→零漂移无回归风险；closure-audit 独立性声明；arm-index 交叉去重声明；MA4↔A5.6 边界；保护区域声明）。

Exit Criteria:

- [x] 验证报告定稿（reservedQty writer census + negative-stock 防护并发兜底 + KitAvailabilityChecker 只读路径 + 运行时安全性裁决 + finding 衔接 + §8 自检齐全）
- [x] P1-RC-008 注记更新或新 finding 已登记入 arm-index（若有变更）或有明确「维持接受无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_029277536ffeUZUsRQwOyoFzGw，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 done] / C 规则14 合并成立[同 owner doc + 同结果表面 reserved=0 Deferred 状态运行时安全性 + 源报告 §7 SP-1/SP-2 同 A1.8] / D 单一结果表面 / E baseline 零信任核验[§7 SP-1/SP-2 逐字匹配 + P1-RC-008 arm-index:143 + P0-MA2-020 arm-index:417 resolved + KitAvailabilityChecker.java 存在 + material-reservation.md:9-16 Deferred 声明确认 + A4.2.3[MR1 successor]正确排除于 Non-Goals:55] / F 反松弛 / G item typing / H Skill / I 保护区域 / J 无矛盾）。零 Blocker。Non-blocking 已吸收：Phase 1 item typing 从 Proof|Decision 修正为 Proof[5/5]。共识达成，转 active。

## Closure Gates

> 本计划为**只读预留/可用量运行时安全性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = reservedQty writer census + negative-stock 防护并发兜底 + KitAvailabilityChecker 只读路径 + 运行时安全性裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.2.1 + A4.2.2 验证报告 reservedQty writer census + negative-stock 防护并发兜底 + KitAvailabilityChecker 只读路径 + 运行时安全性裁决齐全 + finding/注记更新（若有）
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.8 §7 SP-1/SP-2 + §5 P1-RC-008 裁决 + material-reservation.md Deferred 说明一致
- [x] 已运行验证：reservedQty writer census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 预留写路径修复（P1-RC-008 修复归口）

- Classification: `out-of-scope improvement`（本验证是运行时安全性评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是运行时安全性评估，结果表面 = 验证报告 + finding/注记登记。修复归 MR1（R1.0→RC-R1.n），修复触及跨域（库存域 ErpInvReservation* 写接口 + 制造域接线）+ ORM 结构（reservationStatus 字段）属 **ask-first + 独立 plan-audit**（§5 保护区域）。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding[若有] + P1-RC-008 → RC-R1.n 修复；A4.2.3 successor[预留实现后 reservedQty/availableQuantity 一致性]的触发条件 = MR1 P1-RC-008 修复落地后）

## Closure

Status Note: closed (independent closure audit passed)

Closure Audit Evidence:

- Auditor / Agent: independent closure-audit subagent (fresh session, ses auto) — 全部关键证据锚点（updateBalanceWithRetry:256-328 / KitAvailabilityChecker.check:62-89+findAllByQuery:109 / validateAvailable:116-136 / applyReservation:150 / onOutgoing:73-84 / orm.xml versionProp:369+UK:415 / validateTransitionForStart:256-267 + isAllowPartialKitStart:385-387 default FALSE / setReservedQuantity 全集 grep）经实仓复核准确；裁决（维持 P1-RC-008 P1、零 P0、零新 finding、残留并发归 A2.17）逻辑成立且与 §2 判据+三源+P1-RC-008[P1]/P0-MA2-020[resolved] 分层一致；arm-index 注记仅追加运行时安全性确认未改状态/分级/修复通道；文本一致性（plan Status=completed / 双 Phase completed / 门控全 [x] / 报告 verdict）自洽

Follow-up:

- MR1 修复预留写路径（P1-RC-008）：跨域 + ORM 结构变更须 ask-first + 独立 plan-audit
- A4.2.3 successor（预留实现后 reservedQty/availableQuantity 一致性）：触发条件 = MR1 P1-RC-008 修复落地后
