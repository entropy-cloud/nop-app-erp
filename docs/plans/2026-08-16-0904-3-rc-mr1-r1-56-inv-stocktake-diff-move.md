# 2026-08-16-0904-3-rc-mr1-r1-56-inv-stocktake-diff-move RC-R1.56 — inventory 盘点完成自动差异移动单（MR1 越界项：代码逻辑预授权 + product-scope 确认义务）

> Plan Status: active
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.56（P1-MA2-062 reuse 重开 inventory 盘点完成自动差异移动单[UC-INV-07 四断言]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.56 行 + `docs/audits/arm-index.md` P1-MA2-062 行（:554）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（P1-MA2-062 属「RC 报告显式重开/归 MR1 的既有 audit-remediation P1 finding」；**2026-08-12 批量裁决未覆盖本项**——A/B/C 三类清单无 RC-R1.56 行，维持 越界项标准流程：独立 fix plan + plan-audit + 双独立子 agent 批准 checkbox + product-scope 确认义务）
> Related: `docs/design/inventory/use-cases.md`（L1 UC-INV-07 :122-136）；`docs/design/inventory/state-machine.md`（§盘点单状态机 :158-175）；`docs/audits/2026-08-05-0900-rc-ma1-a1-27-inventory-f3-stocktake-valuation-concurrency-dashboard.md`（A1.27 §4 三判据 :160-174 + §6.2）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-74-82-inventory-stockmove-batch-stocktake-runtime.md`（A4.2.80 :76-83）；`docs/plans/2026-07-30-0512-2-r1-19-inventory-stocktake-picking-deferred.md`（R1.19 方案 B Deferred 关闭 + successor）
> Audit: required

## Current Baseline

- **finding P1-MA2-062（arm-index:554，UC-INV-07）**：L1（`use-cases.md:127-134`）逐字断言：①「盘点单.确认 → 计算差异: 差异 = 实盘数量 - 账面数量」②「若 差异 > 0: 生成盘盈移动单(incoming)」③「若 差异 < 0: 生成盘亏移动单(outgoing)」④「盘点单本身不改余额」⑤「盘盈/盘亏移动单走 DRAFT→DONE 后才影响余额」。L3 实仓（HEAD 核查）：
  - `ErpInvStockTakeBizModel.completeTake:45-62` **STUB**：requireEntity + CONFIRMED 守卫（:52-58，stateMachine.assertCanCompleteTake）+ `setDocStatus(DONE)`（:59）+ updateEntity——**无 line 加载、无 actualQuantity/bookQuantity/differenceQuantity 消费、无 `IErpInvStockMoveBiz` 注入、无 generateMove 调用**（A4.2.80 :80 证实，行号漂移：报告引用 :40-50，2026-08-14 状态机 Bean 接线后方法移至 :45-62，语义未变）；
  - `ErpInvStockTakeLine`（`app-erp-inventory.orm.xml:745-776`）：`bookQuantity`(:757 账面数量, mandatory)/`actualQuantity`(:758 实盘数量, mandatory)/`differenceQuantity`(:759 差异数量, mandatory)/`differenceAmount`(:761)——**differenceQuantity/differenceAmount 零生产 writer**（仅 `_gen/_ErpInvStockTakeLine.java:955/:993` setter + api beans，无业务代码计算）；
  - `ErpInvStockTake`（:698-742）：docStatus dict `erp-inv/move-status`（DRAFT/CONFIRMED/DONE/CANCELLED :37-42，无 COUNTING——:170 标签漂移注记）；posted 三列（:710-712）零消费；UK `UK_INV_STOCK_TAKE_CODE_ORG`（:727）；
  - 差异自动生成代码 grep `盘盈|盘亏|generateGain|generateLoss|gainMove|lossMove|StockTakeLine.*Move`（erp-inv-service）= **0 命中**（A2.11 :392 + A3.2 :86 双重证实）。
- **owner doc 状态（state-machine.md §盘点单状态机 :158-175）**：**:172 差异调整移动单的自动生成 = Deferred（owner doc 语义对齐）**——completeTake 当前仅置 DONE，差异调整经库管员**手工 generateMove** 处置；**:173 Successor 触发条件** = 盘点闭环自动化需求落地时在 completeTake 内自动比对 → 经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单；:174-175 差异不直接改余额原则保留。A1.27 §4 三判据（:160-174）：(i) R1.19 plan 含 AI 子代理草案审查但「代理独立审计 ≠ 人工批准」+ Closure Audit Evidence 自承「执行代理（本会话）」= hollow closure；(ii) `state-machine.md:159` Deferred 标注无人工批准痕迹（git log 全 AI commits）；(iii) `product-scope.md:16` 仅列「盘点」为域能力，**未裁剪**「盘点自动生成移动单」→ **三判据均不满足 → 倾向重开 P1 入 MR1**（:170-172），修复方向 = completeTake 自动比对 `StockTakeLine.actualQuantity` vs `ErpInvStockBalance.totalQuantity` → 差异经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单（**代码逻辑类预授权**，复用既有 Facade + 既有字段，不触 ORM 结构变更 / 不触 §5 ask-first——A1.27 :172 明示）。
- **product-scope 确认义务**：A1.27 §6.2 (iii) 已核 `product-scope.md:16`「库存移动单、库存流水、库存余额、调拨、盘点、批次/序列号」未裁剪自动生成——**未裁剪 → P1 强制实现**（与 Q5 换货先例同型：真相源无排除即义务成立）。本计划在 Phase 1 复核 product-scope 现行文本并登记确认结论。
- **复用机制（generateMove Facade 全链，A4.2.80 :82 证实手工入口可追溯）**：`ErpInvStockMoveBizModel.generateMove:56-60` → `ErpInvStockMoveGenerateMoveProcessor.generateMove:28-47`——business-linked（relatedBillType+relatedBillCode 非空）→ 幂等 findExisting 反查去重（:29-35）；newMove（ErpInvStockMoveProcessor:234-253 moveType/orgId/businessDate/仓库库位/docStatus=DRAFT/approveStatus=UNSUBMITTED/posted=false/relatedBillType+Code/remark/originMoveId）→ save → persistLines（:49-57）→ doConfirm（:41）→ business-linked 时 doComplete（:42-45）；doConfirm（:101-116 DRAFT 守卫 + validateAvailable:137-159 仅 OUTGOING/INTERNAL + applyReservation + CONFIRMED）；doComplete（:118-135 CONFIRMED 守卫 + releaseReservation + bookkeeper.bookCompletion + DONE + dispatchIfApplicable）。`StockMoveRequest`（erp-inv-dao:19-35）：relatedBillType 为空 → **独立移动单停 CONFIRMED 待库管员二次确认**（:13-14 javadoc）；业务联动移动单自动 DRAFT→DONE 不产生库管员待办（state-machine.md:129）。
- **过账交互事实（本计划新增设计面，既有审计未覆盖）**：`InvPostingDispatcher.resolveBusinessType:152-179`——跳过集 {ERP_PUR_RETURN, ERP_SAL_RETURN, ERP_MNT_SPARE_PART, ERP_MFG_ISSUE}（ErpInvConstants:75-81）→ return null；否则 INCOMING→PURCHASE_INPUT、OUTGOING→SALES_OUTPUT、MANUFACTURE→MANUFACTURING_RECEIPT（:168-177）。**含义**：盘点差异移动单若走 business-linked generateMove，盘盈 INCOMING 会误派「借 1401/贷 2202 暂估应付」PURCHASE_INPUT 凭证、盘亏 OUTGOING 会误派 SALES_OUTPUT 成本凭证——**盘点差异过账语义（盘盈/盘亏）当前无专属 businessType/Provider，须在 Phase 1 D3 裁决过账处理**（跳过错账 vs 专属过账——触会计过账逻辑须按保护区域规则）。
- **预授权判据**：RC-R1.56 行标注「代码逻辑预授权 + product-scope 确认义务」；roadmap:13/29 + ai-autonomy-policy:79/83（2026-08-15 升级：越界项按 `auto + dual-agent-approval` 执行，两个独立子 agent 批准记录落盘计划）。**须 双独立子 agent 批准 + 独立 plan-audit**。roadmap 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`ErpInvStockTakeBizModel.java`（completeTake 重写）；新差异移动单生成 helper/Processor；`InvPostingDispatcher.java`（跳过集或新 businessType——D3 裁决）；`ErpInvConstants.java`（新 relatedBillType 常量 + D4-b config 键登记 ErpInvConfigs）；`ErpInvErrors.java`（新 ErrorCode——D4-c 定稿）；`ErpInvStockTake.view.xml`/`stock-take-flow/main.page.yaml`（:6/:13 文案同步——「未生成盘盈/盘亏移动单」注释更新）；owner doc `state-machine.md`/`ui-patterns.md` 注记 + arm-index/roadmap/logs 回填。
- **测试基线**：**零 `TestErpInvStockTake*` 测试**（全仓 glob 零命中）；唯一触碰 stocktake 的测试 = `TestErpInvStockMoveAndStockTakeStateMachines`（Bean 级矩阵 :150-174/:226/:268，无 BizModel 级 completeTake 行为测试）；generateMove 复用参考测试：`TestErpInvStockMoveGraphQL:35-58`/`TestErpInvStockMoveCrudSmoke`/`TestErpInvStockMoveBizModel` + 跨域 `TestErpPurReceiveStockMove`/`TestErpPurToInvToFinPostingEnd:110,216`/`TestErpSalDeliveryStockMove`。E2E：`tests/e2e/crud/placeholder-pages.smoke.spec.ts:32` 仅占位页冒烟（stock-take-flow）；无 inventory ErpInvStockTake E2E。

## Goals

- **UC-INV-07 五断言运行时成立（P1-MA2-062 核心）**：completeTake 从 STUB 升级为完整盘点闭环——确认时加载行 → 计算差异（断言①；口径 Phase 1 D1 裁决：L1 字面 `actualQuantity − bookQuantity` vs 审计方向 `actualQuantity vs StockBalance.totalQuantity`）→ 差异 >0 生成盘盈 INCOMING 移动单（断言②）/ 差异 <0 生成盘亏 OUTGOING 移动单（断言③）→ 盘点单本身不改余额（断言④，差异经移动单状态机落地）→ 移动单 DRAFT→DONE 后才影响余额（断言⑤，经 generateMove 既有链）。
- **差异移动单生成语义**：经 `IErpInvStockMoveBiz.generateMove` Facade 复用既有幂等/状态机链（Phase 1 D2 裁决：独立移动单停 CONFIRMED 待库管员二次确认[对齐 state-machine.md:129 盘点调整 = 独立创建产生待办] vs business-linked 自动 DONE[对齐业务联动语义]）；逐行差异移动单（行级 material/sku/uoM/batchNo/quantity=差异量）；零差异行跳过；生成失败逐行隔离（单行失败不阻断整单，LOG.warn + D4-b 告警派发）。
- **过账处理裁决**（D3，触会计过账逻辑）：盘点差异移动单经 InvPostingDispatcher 的过账语义——选项 A 跳过集扩展（盘盈/盘亏不生成误派凭证，owner doc 注记「盘点差异过账 successor」）vs 选项 B 专属 businessType + Provider（盘盈/盘亏会计凭证，触会计核心须双独立子 agent 批准 + 独立 plan-audit）——按 Phase 1 裁决 + 保护区域规则执行；D2/D3 一致性：独立移动单判别载体 = `relatedBillType=ERP_INV_STOCK_TAKE` + null code（`isBusinessLinked()==false` 保持 CONFIRMED 停 + 跳过集条目生效）。
- **product-scope 确认义务闭环**：Phase 1 复核 product-scope.md 现行文本 → 未裁剪确认登记（对齐 Q5 换货先例）→ 修复义务成立。
- **测试**：新增 `TestErpInvStockTakeCompleteDiffMove`——① 盘盈（差异>0）生成 INCOMING 移动单；② 盘亏（差异<0）生成 OUTGOING 移动单；③ 零差异不生成；④ 移动单 DONE 后余额变化（断言⑤，按 D2 裁决驱动完成路径）；⑤ 盘点单本身余额不变（断言④）；⑥ 幂等/重复 completeTake 守卫；⑦ 部分行失败隔离 + 告警；⑧ differenceQuantity/differenceAmount 回填 + D3 过账处理断言；既有状态机矩阵测试零回归（零回归为整体验证门，不占测试组编号）。
- **零回归**：erp-inv-service 全量测试（218 基线）全绿 + 全仓 `mvn test` + 全量构建 + compliance checker 零漂移（或基线上调带 per-site 证据）。
- **owner doc 收敛**：`state-machine.md §盘点单状态机` :172-173 Deferred 标注更新为已实现 + successor 边界声明；`ui-patterns.md` 流程同步；`stock-take-flow` 页面文案更新；arm-index P1-MA2-062 → done (RC-R1.56) + roadmap 行 done + logs 条目。

## Non-Goals

- **不实现盘点差异的专属会计过账凭证**（除非 Phase 1 D3 裁决选项 B——默认倾向 A 跳过 + successor；触会计核心路径则按保护区域流程）。
- **不实现盘点单自动审核/自动完成**（completeTake 由操作员触发，仅完成时自动生成差异移动单）。
- **不实现盘点差异的自动平衡/四舍五入策略**（逐行差异量 = L1 公式，零行长差异舍入策略按既有移动单行量精度）。
- **不重写 generateMove 状态机**（复用既有 Facade 全链，不改 doConfirm/doComplete 语义）。
- **不改 ORM 结构**（既有 differenceQuantity/differenceAmount 字段 + StockMoveRequest 全能力复用；differenceQuantity/differenceAmount 写入为行级计算回填——不改模型）。
- **不改真相源契约段落**（use-cases L1 不动；state-machine.md 契约段不动，仅补实现注记 + Deferred 标注更新为已实现）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：代码逻辑类预授权[completeTake 重写 + generateMove 复用] + product-scope 确认义务；D3 若涉会计过账则按保护区域双独立子 agent 批准；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/inventory/use-cases.md`（L1 UC-INV-07）+ `docs/design/inventory/state-machine.md`（§盘点单状态机）
- Skill Selection Basis: BizModel/Processor 编排 + 跨实体调用（`nop-backend-dev`：per-mutation Processor 模式 + IBiz 注入 + 失败隔离）；测试（`nop-testing`：JunitBaseTestCase + `_cases/` 快照 + 既有状态机矩阵零回归）。

## Infrastructure And Config Prereqs

- 新 config key（D4-b 定稿时）：`erp-inv.stocktake-diff-alert-enabled`（默认 false，单键门控对齐 R1.4 范式，经 ErpInvConfigs 登记 + 生产 yaml 零 override 普查）；其余行为由单据流程驱动非部署配置。
- 无 ORM 变更（既有字段全复用）——无需增量重生成。
- 分域验证前置：`mvn test -pl module-inventory/erp-inv-service`（无 ORM 变更可不跑全量 install 前置）。

## Execution Plan

### Phase 1 - 差异口径/生成语义/过账处理裁决 + product-scope 确认（Decision）

Status: planned
Targets: `use-cases.md`（L1 复核）；`product-scope.md`（确认复核）；`InvPostingDispatcher.java`（过账面调研）；`state-machine.md`（注记设计）
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [ ] `Decision` **D1 差异口径**：**选项 A（倾向选定）** = L1 字面公式 `差异 = actualQuantity − bookQuantity`（`use-cases.md:129` 逐字——盘点行字段快照对账，bookQuantity 为盘点单行账面数量快照）；**选项 B（否决）** = 审计方向 `actualQuantity vs StockBalance.totalQuantity`（实时余额比对——A1.27 :172/P1-MA2-062 方案 A/state-machine.md:173 三处审计文档引用；实时比对含盘点期间发生的移动，口径更严但偏离 L1 字面且需跨实体实时查询）。**理由**：真相源优先级 Q1=(c)（use-cases L1 权威 > owner doc 参考）——L1 公式为唯一契约；bookQuantity 字段在盘点行上为快照值（盘点时点账面），与 L1「账面数量」逐字一致。**残留风险**：盘点期间发生库存移动时 bookQuantity 快照口径与实时余额有差（盘点业务惯例为冻结账面快照，接受；owner doc 注记引导「盘点期间冻结出入库」运营建议）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **D2 移动单生成语义**：**选项 A（倾向选定）** = 独立移动单（`relatedBillType=ERP_INV_STOCK_TAKE` + `relatedBillCode=null`——`StockMoveRequest` javadoc :13-14「relatedBillType 为空 → 停 CONFIRMED 待库管员二次确认」的**判别键 = `isBusinessLinked()`（两字段均非空才 true）**，故带类型键 + null code 时仍 `isBusinessLinked()==false` → 停 CONFIRMED 待二次确认 + state-machine.md:129「盘点调整 = 独立创建产生库管员待办」；类型键提供 D3 过账跳过的判别载体，code 置空保持独立语义）；**选项 B（否决）** = business-linked 自动 DONE（relatedBillType=ERP_INV_STOCK_TAKE + relatedBillCode=take.code——幂等 findExisting 自动去重 + doComplete 自动落余额，但绕过库管员二次确认 + 触发 InvPostingDispatcher 误派（见 D3））。**理由**：owner doc 双重证据（state-machine.md:129 独立创建产生待办 + :172 Deferred 语义「经手工 generateMove」= 独立移动单语义）+ 断言⑤「走 DRAFT→DONE 后才影响余额」天然契合停 CONFIRMED 语义 + 库管员二次确认防误盘；与 D3 选项 A 判别载体一致（见 D2/D3 一致性注记）。**残留风险**：独立移动单无 business-linked 幂等键（findExisting 不命中）——completeTake 幂等经状态守卫（CONFIRMED→DONE 单次）保证；移动单与盘点单关联经 remark 承载（code 引用）+ D4-a 裁决（remark 承载，不加新列）。
      - Skill: `nop-backend-dev`
- [ ] `Decision` **D3 过账处理（触会计过账逻辑，保护区域门控）**：**选项 A（倾向选定）** = 差异移动单跳过过账——`InvPostingDispatcher.resolveBusinessType` 跳过集扩展（跳过集加 `ERP_INV_STOCK_TAKE`——判别载体 = D2 选项 A 的 relatedBillType 类型键；实仓核实结论：:152-179 对 relatedBillType==null 的 INCOMING/OUTGOING 仍按 moveType 映射 PURCHASE_INPUT/SALES_OUTPUT 误派，故**必须**有类型键 + 跳过集条目，不能依赖 null 判别）+ owner doc 注记「盘点差异过账（盘盈/盘亏会计凭证）successor」；**选项 B（否决）** = 专属 businessType + AcctDocProvider（盘盈借 1401/贷 待处理财产损溢 或盘亏贷 1401/借 损益——实现完整会计闭环但触会计核心路径，须双独立子 agent 批准 + 独立 plan-audit + 新 Provider 面）。**理由**：L1 断言面仅「移动单 + 余额影响」，无过账断言——A 满足 L1 全断言且最小面；B 为会计完善项超断言面。**残留风险**：盘盈/盘亏金额不入 GL（盘点差异未会计化——登记 successor，触发条件=运营/审计要求盘点差异会计化时立项）。**⚠ 若执行期裁决为 B：本 Phase 升级为双独立子 agent 批准门控前置项（会计过账逻辑变更类，roadmap:17），Phase 2 批准记录同步扩展。**
      - Skill: `nop-backend-dev`
- [ ] `Decision` **D4 关联载体 + 失败告警派发 + ErrorCode（原 D2 残留 + completeTake 失败路径合并）**：**D4-a 移动单↔盘点单关联载体**：**选项 A（选定）** = remark 承载（移动单 remark 写「盘点差异 {take.code} 盘盈/盘亏」——零 ORM 变更，Non-Goal「不改 ORM 结构」一致）；**选项 B（否决）** = 新增关联列（如 ErpInvStockMove.stockTakeId——触 ORM 结构变更，违反本计划 Non-Goal 且超代码逻辑预授权范围）。**理由**：A 零模型变更 + 可读性足够（审计经 code 引用追溯）；B 扩 ORM 面无 L1 断言支撑。**残留风险**：remark 关联非结构化——移动单不可按盘点单直接查询（审计仅经 code 引用反向追溯），watch-only 登记。**D4-b 生成失败告警派发**：**选项 A（选定）** = 失败逐行隔离 + LOG.warn + 经 `IErpSysNotificationBiz.notify` 派发盘点差异生成失败告警（对齐 A4.2.4 `dispatchVarianceFailureAlert` 范式——notify 事件 `inv.stocktake-diff-generation-failed` + config `erp-inv.stocktake-diff-alert-enabled` 默认 false 门控[对齐 R1.4 单键门控范式] + 无 ACTIVE 模板静默跳过）；**选项 B（否决）** = 仅 LOG.warn 无告警（运营无感知，与 A4.2.4/A4.2.9 可观测性改进方向相悖）。**理由**：A 对齐既有 best-effort 可观测性范式（R1.3/R1.48 先例），失败可见性运营闭环。**残留风险**：notify 通道零模板/config 默认关闭时静默跳过（既有范式接受，config 键经 ErpInvConfigs 登记）。**D4-c ErrorCode 定义决策**：**选项 A（选定）** = 需要时定义（如盘点差异移动单生成守卫错误码 `ERR_INV_STOCK_TAKE_MOVE_GENERATE`，中文描述 + define 参数表——行加载/差异计算失败守卫与状态迁移守卫不同面）；**选项 B（否决）** = 复用既有错误码族（`ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION` 等——若生成守卫语义可完整映射则零新增）。**理由**：A 显式化盘点差异生成守卫独立语义；B 仅在语义可完整映射时采用。**残留风险**：零新增码（选 B）时守卫错误复用既有码族，Phase 2 实施按 Phase 1 定稿执行。
      - Skill: `nop-backend-dev`
      - Skill: `nop-backend-dev`
- [ ] `Proof` **product-scope 确认复核**：`product-scope.md:16` 现行文本 grep `盘点|盘盈|盘亏|差异` 复核 → 未裁剪确认登记（对齐 Q5 换货先例 :190 登记格式）；`use-cases.md:127-134` L1 四断言原文复抄本计划；`InvPostingDispatcher.resolveBusinessType` 对 relatedBillType==null 行为的实仓核实（:152-179 读码 + 单测证实——D3 设计输入）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] D1-D4 裁决记录落盘（选择 + 备选 + 理由 + 残留风险），product-scope 未裁剪确认登记完成，resolveBusinessType null 行为核实结论产出（D2/D3 一致性注记：独立移动单判别载体 = `relatedBillType=ERP_INV_STOCK_TAKE` + null code，`isBusinessLinked()==false` 保持 CONFIRMED 停 + 跳过集条目生效）
- [ ] 差异移动单生成设计定稿（逐行差异量/零差异跳过/失败隔离 + D4-b 告警派发/幂等守卫）

### Phase 2 - completeTake 重写 + 差异移动单生成（双独立子 agent 批准门控）

Status: planned
Targets: `ErpInvStockTakeBizModel.java`；新差异移动单生成 helper/Processor；`ErpInvConstants.java`；`ErpInvErrors.java`（决策：按 Phase 1 定稿决定是否定义，定义时中文描述）；`InvPostingDispatcher.java`（D3 选项 A 时跳过集扩展）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Proof` **双独立子 agent 批准（越界项标准流程门控，硬门，批准落盘前不得实施 Phase 2 变更）**：两个独立子代理（fresh session，无执行者上下文）分别检查批准（批准记录落盘本计划 Draft Review Record/Closure 段，对齐 roadmap:13/29 + ai-autonomy-policy:79/83 2026-08-15 升级「越界项按 auto + dual-agent-approval 执行」）。批准前置条件：D1-D4 裁决与 L1 契约一致、既有 generateMove 链零改动、盘点单本身不改余额原则保持、若 D3 裁决 A 则跳过集扩展语义安全（不影响既有 PURCHASE_INPUT/SALES_OUTPUT 路径）、若 D3 裁决 B 则按会计核心路径复核 Provider 借贷恒等。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpInvStockTakeBizModel.completeTake` 重写：加载 lines（:45-62 现状 + 行加载）→ D1 公式逐行计算差异 → 零差异行跳过 → 差异行聚合 → 生成盘盈 INCOMING/盘亏 OUTGOING 差异移动单（D2 选项 A：`relatedBillType=ERP_INV_STOCK_TAKE` + `relatedBillCode=null` → 停 CONFIRMED 待库管员二次确认；按裁决定稿）+ differenceQuantity/differenceAmount 回填（既有零 writer 字段首次业务写入）→ setDocStatus(DONE) 保持既有状态守卫；@BizMutation 事务包裹 + 失败隔离（单行失败不阻断整单，LOG.warn + D4-b 选项 A 告警派发——notify 通道 + 无模板静默跳过）。
      - Skill: `nop-backend-dev`
- [ ] `Add`（D3 选项 A 时）`InvPostingDispatcher.resolveBusinessType` 跳过集扩展（跳过集加 `ERP_INV_STOCK_TAKE`——判别载体 = D2 选项 A 类型键；实仓核实结论：null relatedBillType 仍按 moveType 映射误派，故类型键 + 跳过集条目为必要组合）+ `ErpInvConstants` 新常量 + `ErpInvErrors` 新 ErrorCode（按 Phase 1 D4-c 定稿：需要时定义中文描述——如盘点差异移动单生成守卫错误码）。
      - Skill: `nop-backend-dev`
- [ ] `Add`（D3 选项 B 时）专属 businessType + AcctDocProvider（盘盈/盘亏凭证借贷恒等 + seed 科目行纯加性如 R1.50 先例）——按 Phase 1 裁决落地，会计核心路径复核。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 双独立子 agent 批准记录落盘（批准人 2 个独立子代理 + 结论），completeTake 重写 + 差异移动单生成 + 过账处理落地（grep 证据）
- [ ] 盘点单本身不改余额（断言④——completeTake 无余额直写代码路径，git diff 证明）+ 既有 generateMove 链零改动

### Phase 3 - 测试 + 文档回填 + 零回归验证

Status: planned
Targets: 新增 `TestErpInvStockTakeCompleteDiffMove`；`state-machine.md`；`ui-patterns.md`；`stock-take-flow/main.page.yaml`；arm-index/roadmap/`docs/logs/`
Skill: `nop-testing` + `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [ ] `Add` 新增 `TestErpInvStockTakeCompleteDiffMove`（8 组）：① 盘盈（差异>0）生成 INCOMING 移动单（行数/量/方向 + D4-a remark 关联断言）；② 盘亏（差异<0）生成 OUTGOING 移动单；③ 零差异不生成；④ 移动单 DONE 后余额变化（断言⑤——按 D2 裁决的完成路径驱动：D2 选项 A 时测试先驱动 confirm→complete（库管员二次确认语义），D2 选项 B 时经 generateMove 自动 DONE；bookCompletion 余额断言）；⑤ 盘点单本身余额不变（断言④）；⑥ 幂等/重复 completeTake 守卫（CONFIRMED→DONE 单次 + 非法边拒绝）；⑦ 部分行失败隔离（单行失败 WARN + D4-b 告警不阻断 + 其余行移动单生成）；⑧ differenceQuantity/differenceAmount 回填断言 + D3 过账处理断言（A：零凭证生成[跳过集生效]；B：凭证借贷恒等）。
      - Skill: `nop-testing`
- [ ] `Add` owner doc 注记：`state-machine.md §盘点单状态机` :172-173 Deferred 标注更新为已实现（completeTake 自动差异移动单 + D1-D4 裁决 + successor 边界声明「盘点差异会计化」）；`ui-patterns.md` 流程同步；`stock-take-flow/main.page.yaml`/`ErpInvStockTake.view.xml` 文案更新（「未生成盘盈/盘亏移动单」注释 → 已实现）；`use-cases.md` 不动。
      - Skill: `nop-backend-dev`
- [ ] `Proof` 分域零回归 + 回填：`mvn test -pl module-inventory/erp-inv-service` 全绿（218 基线 + 新增零回归）+ 回填（arm-index P1-MA2-062 → done (RC-R1.56) + roadmap 行 done + `docs/logs/2026/08-16.md` 日志条目）。全仓 `mvn test` + 全量构建 + compliance checker 归 Closure Gates 统一执行（guide 执行时规则 7）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新测试全绿（①-⑧）+ erp-inv-service 既有测试零回归（含状态机矩阵）
- [ ] owner doc 注记（Deferred→已实现）+ 三处回填（arm-index/roadmap/log）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_ff7e177c3ffef2vPEh5Qg8ic59`）——2 MAJOR + 5 MINOR。MAJOR-1 已修正：**D4 悬空引用**——原两处「D4 内联裁决」标签无对应 Decision 项（关联载体 + 失败告警两个不同主题共用一个标签）——升级为显式 D4 Decision 项（D4-a 关联载体：remark 承载[选项 A 选定] vs 新列[否决——触 ORM 违反 Non-Goal]；D4-b 失败告警派发：notify 通道[选项 A 选定，对齐 A4.2.4 dispatchFailureAlert 范式] vs 仅 LOG.warn[否决]），「告警可选」反松弛措辞移除。MAJOR-2 已修正：**D2-A 与 D3-A 判别载体不一致**——D2-A「relatedBillType 为空」与 D3-A「跳过集加 ERP_INV_STOCK_TAKE 类型键」互斥——统一判别载体 = `relatedBillType=ERP_INV_STOCK_TAKE` + `relatedBillCode=null`（`isBusinessLinked()` 两字段均非空才 true → false 保持 CONFIRMED 停 + 跳过集条目生效），D2/D3 均引用该载体（D2 理由 + D3 选项 A + Phase 1 exit criteria 一致性注记）。5 MINOR 已修正：(1)「如需」限定词改为显式决策措辞（ErrorCode 定义与否 Phase 1 定稿）；(2) Goals 测试编号与 Phase 3 对齐（⑧ 状态机矩阵零回归移出测试组编号，归零回归整体验证门；Phase 3 ⑧ = 回填 + D3 过账断言）；(3) 测试④ 补 D2 裁决依赖说明（D2=A 时驱动 confirm→complete 二次确认路径，D2=B 时自动 DONE）；(4) `_gen/_ErpInvStockTakeLine.java` 行号修正 :947-958 → :955/:993；(5) Phase 3 Exit Criteria 全仓验证归 Closure Gates（对齐 guide 执行时规则 7，Phase 3 保留分域 + 回填门）。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_ff7d8e087ffeyrq07RARlUCo32`）——0 MAJOR + 4 MINOR。已修正：(1) Phase 3 Exit Criteria/Proof 全仓验证与 Closure Gates 重复——Proof 项改为分域零回归 + 回填，全仓验证归 Closure Gates（guide 执行时规则 7）；(2) D4-b「config-gated 可选」反松弛词——改为显式 config `erp-inv.stocktake-diff-alert-enabled` 默认 false + notify 事件 `inv.stocktake-diff-generation-failed` + ErpInvConfigs 登记（对齐 R1.4 单键门控）+ Infra prereqs 同步；(3) ErrorCode 决策无主——新增 D4-c 子裁决（需要时定义 `ERR_INV_STOCK_TAKE_MOVE_GENERATE` vs 复用既有码族，理由 + 残留风险），baseline/Phase 2 引用同步；(4) D4-a 补残留风险（remark 非结构化载体 watch-only）+ D2 残留风险「倾向」陈旧措辞修正为「D4-a 裁决（remark 承载）」。
- Independent draft review iteration 3: `accept`（独立子代理 `ses_ff7d2c3e1ffe80d2pZri1pzGdW`）——四项修正逐项验证（config-gated 可选零残留/D4-c 落位/「如需」从 baseline 移除/「倾向 remark」零残留）+ 全链一致性复核（Phase 1→3 prereqs 链/D3-B 升级注记接线/D1-D4 rule-9 完整/D2-D3 判别载体三处一致/Goals↔Phase 3 测试 ①-⑧ 1:1 对齐/review record 迭代描述与实仓一致/无重复覆盖计划；1 非阻塞观察 = D4 Skill 行重复属 cosmetic）→ **共识达成，计划可转 active**。

## Closure Gates

- [ ] 范围内行为完成（P1-MA2-062：completeTake 差异计算 + 盘盈/盘亏移动单 + 过账处理 + 测试）
- [ ] 相关文档对齐（state-machine.md Deferred→已实现 + ui-patterns.md + 页面文案 + arm-index P1-MA2-062 → done (RC-R1.56) + roadmap 行 done）
- [ ] 已运行验证（`mvn test -pl module-inventory/erp-inv-service` + 全仓 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 盘点差异会计化（盘盈/盘亏 GL 凭证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 断言面无过账断言（仅移动单 + 余额）；D3 裁决选项 A 时差异不入 GL——运营/审计未要求会计化前不阻塞（对齐 A4.2.40 类 documented-simplification 判据）
- Successor Required: `yes`（触发条件：运营/审计要求盘点差异会计化[盘盈/盘亏凭证]时，按会计核心路径立项 + 双独立子 agent 批准）

### 盘点期间出入库冻结运营建议

- Classification: `watch-only residual`
- Why Not Blocking Closure: D1 选项 A bookQuantity 快照口径与盘点期间实时余额有差——运营惯例冻结账面快照，owner doc 注记引导
- Successor Required: `no`

## Closure

Status Note: 草案待独立审查（Plan Status 保持 draft 直至审查收敛）。

Closure Audit Evidence:

- Auditor / Agent: 未执行（待实施后独立结束审计）
- Evidence: —

Follow-up:

- （无）
