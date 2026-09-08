# 状态机直抛领域错误码：退役"先抛 common 码再外层转码"反模式

> Plan Status: active
> Last Reviewed: 2026-09-08
> Source: `docs/lessons/19-statemachine-throw-domain-error-code-directly.md`（用户 2026-09-07 裁定："底层不直接把错误码搞好，先抛通用码再转码，多此一举"——全仓核实成立）
> Related: `docs/lessons/15-xbiz-xscript-no-trycatch-sink-to-java-bean.md`（2026-09-07 勘误版，转码断链的技术根源）、`docs/design/assets/state-machine.md` §错误码迁移说明、`docs/architecture/entity-state-machine-bean.md` §错误码契约
> Audit: required

## Purpose

消除"先抛通用码再外层转码"的被迫转码反模式：**所有 StateMachine 的 `illegal()` helper 直接抛领域错误码**；退役 Guard/Processor/xbiz 中的 catch-and-remap 转码层；矩阵测试断言随契约翻转。端到端用户可见错误码除"裸奔通道从 common 码修正为领域码"（逐一在 Phase 内声明）外全部保持不变。

## Task Route

- Type: `implementation-only change`（错误码契约收敛重构；不改 API 签名、不改 ORM、不改用户可见行为除声明的裸奔通道修正）
- Owner Docs: `docs/architecture/entity-state-machine-bean.md`、`docs/design/assets/state-machine.md`、`docs/lessons/19-statemachine-throw-domain-error-code-directly.md`、`docs/lessons/15-xbiz-xscript-no-trycatch-sink-to-java-bean.md`
- Skill Selection Basis: 草案审查用 `plan-audit-prompt`（已执行，见 Draft Review Record）；结束审计将用 `closure-audit-prompt`（独立子代理）；执行阶段无匹配操作技能（机械换码配方由本计划 Phase 1/2 沉淀），记录 `Skill: none`

## Current Baseline（2026-09-08 执行期复盘 + 独立审查实仓复核，取代 2026-09-07 初盘）

盘点命令（执行期复核口径）：

```bash
# 抛 common 码的 StateMachine 清单（按模块计数）
rg -l "ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION" --glob "*StateMachine.java" module-*
# 真 common 码生产引用（非 StateMachine 文件、限定 ErpCommonErrors 前缀）
rg -l "ErpCommonErrors\.ERR_ILLEGAL_STATUS_TRANSITION" --glob "*.java" module-* | rg -v "StateMachine"
# catch-and-remap / 码值判别转码站点
rg -U -l "try \{\s*\n\s*stateMachine\.assertCan|catch \(NopException e\) \{\s*\n\s*throw illegalStatusException" --glob "*.java" module-* | rg -v src/test
# 断言 common 码的测试
rg -l "ErpCommonErrors\.ERR_ILLEGAL_STATUS_TRANSITION" --glob "*.java" module-*/**/src/test
```

- **抛 common 码的 StateMachine = 103（18 个模块）**：ast×15、pur×16、sal×12、fin×12、mfg×8、prj×5、qa×5、inv×6、hr×5、mnt×5、ct×3、b2b×3、drp×2、crm×2、cs×1、md×1、aps×1、log×1。全部形态同构：私有 `illegal(action, currentStatus, expectedStatus)` 返回 `new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)` + params(currentStatus/expectedStatus/action)；**无共享基类**。（独立审查 iteration 1 实仓复核一致）
- **common 码字符串字面量 `nop.err.erp.common.illegal-status-transition` 全仓仅定义处 1 文件**（无 yaml/快照/字符串断言消费）。Java 常量引用是唯一消费通道 → lesson 19 判据 1（无通用消费方）成立。
- **转码站点（catch-and-remap / 码值判别）≈153 个生产文件**（审查复核分模块：sal36/pur34/mfg13/ast12/inv10/mnt9/fin9/prj6/ct6/hr4/crm4/aps3/md2/cs2/b2b2/log1），四类形态：
  1. **纯转码→域通用码**：`try { sm.assertCanX(s) } catch (NopException e) { throw illegalStatusException(entity, ...) }`，重抛 `erp.err.{sal,pur,inv}.illegal-status-transition` + 单据码参数。**仅限** sal Delivery 族、pur Receive 族、inv StockMove/CostAdjust/LandedCost 族等——今日即域通用码的实体。
  2. **纯转码→实体专属码（最大族群，初盘漏列）**：sal Order/Quotation/Invoice/Receipt/Return 5 实体用 `erp.err.sal.{entity}-illegal-status-transition`（`ErpSalErrors.java:79/112/140/157/187`）；pur Order/Requisition/Invoice/Payment/Return 5 实体用 `erp.err.pur.{entity}-illegal-status-transition`（`ErpPurErrors.java:61/78/132/160/196`）；inv StockTake 用 `ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION`、OwnershipTransfer 用 `ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS`（`ErpInvStockTakeBizModel.java:46-79`、`ErpInvOwnershipTransferProcessor.java:57-61`）。
  3. **码值判别转码**（fin 4 文件：`ErpFinReconciliationBizModel/PostProcessor/ReverseProcessor`、`ErpFinBudgetScenarioProcessor`）：`catch (NopException e) { if (ErpCommonErrors...getErrorCode().equals(e.getErrorCode())) throw statusError(head, e); throw e; }`——转实体专属码 `ERR_RECONCILIATION_STATUS_INVALID`（params=reconciliationId+docStatus，见 `AbstractErpFinReconciliationProcessor.java:164-176`）/ `ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION`。换码后判据失配，须令 StateMachine 直抛同一终码，调用点改同码补参。
  4. **直抛兜底**（mfg 2 文件：`ErpMfgWorkOrderProcessor:555`、`ErpMfgSubcontractOrderProcessor:488` expected-switch 的 else 分支直抛 common）——换抛 mfg 域码。
- **双轴双码实体为常态形态（审查 MAJOR-1 登记）**：同一实体审批轴（approveStatus）与 docStatus 轴各自收敛于不同终码，成对存在（如 ast DISPOSAL `ERR_AST_DISPOSAL_ILLEGAL_STATUS_TRANSITION`/`..._DOC_TRANSITION`，`ErpAstErrors.java:152/156`；ADJUSTMENT :174/178；SPLIT :274/278；MERGE :324/328；sal/pur 各实体 STATUS/DOC 成对）。**码选择粒度 = 每个 StateMachine Bean（每轴）**，非每实体。
- **common 码生产引用全集（G4 零引用判据的对象）**：103 StateMachine + 转码站点中的 `ErpCommonErrors` 判别/直抛引用（fin×4、mfg×2）+ `module-common-service AbstractProcessor` 的 common 通道（`defaultIllegalStatusException` 默认实现 + `validateDocStatus`（**外部直调 = 0**，审查复核证实；crm 3 文件的 `validateDocStatus` 系 crm facade 同名自有方法、抛 `ERR_LEAD_NOT_QUALIFIED` 域码，非本通道消费方））。
- **骨架守卫裸奔通道（第二大盘点增量）**：`Abstract*Processor` 骨架的守卫 hook 默认实现（`validateTransitionForXxx`/`validateNotCancelled` 等）经 `illegalStatusException` hook 默认走 common 码。149 个具体 Processor 子类（155 − 6 抽象骨架；test 目录与 module-* 之外均无子类，审查复核）中 **77 个未触 `illegalStatusException`**（ast×30、fin×20、mfg×10、inv×7、qa×5、prj×4、crm×1）——其骨架守卫路径 fire 时 common 码直达用户（裸奔，同 Movement 类别，有意契约修正）。闭包方案：各域批内为每个未覆写子类补 5 行域码 `illegalStatusException` 覆写（码=该 Bean 轴映射终码）；Phase 5 将 `AbstractProcessor.illegalStatusException` 改为 abstract + 删除 `defaultIllegalStatusException`，编译器强制 149/149 闭环（审查复核：编译闭环方案无遗漏消费方）。
- **裸奔实体（无转码层，common 码直达调用方）**：`ErpAstMovement` 为确认样本；其余无 Processor 的实体同型。
- **领域错误码现状**：域通用码已存在 inv/sal/pur；实体专属码已存在 hr salary/leave、ast 全族、sal/pur 各 5 实体族、inv stock-take/ownership-transfer、fin reconciliation/budget-scenario、ct、cs 等。缺失域通用码（按各 Bean 轴 1:1 判据，部分改用既有实体专属码，不新建）：fin/mfg/prj/qa/mnt/log/crm/drp/b2b/md/aps/ast movement。
- **owner 契约冲突（本计划一并改写，审查 MAJOR-2 枚举）**：`docs/architecture/entity-state-machine-bean.md` 三处编码旧契约——§7 错误码契约（:153-156 "Bean 不组装领域 ErrorCode"）、§11.1 步骤 3（:254 "非法边由 Bean 抛 common 层码…Processor 映射"）、§11.4（:312 "对终态报告 common 非法边"）；`docs/design/assets/state-machine.md` §适用对象二 §4 异常路径行（:219）、§适用对象三 §4（:304-305）、§适用对象四 §4（:367-368）同样声明旧契约。
- **矩阵/单元测试断言 common 常量 = 119 个测试文件**（审查复核三种 glob 口径一致；含矩阵测试、BaselineIoC/DeltaOverride 测试与 test 内 Delta fixture）；执行期逐域翻转。
- **历史根源**：2026-08 时 XScript 无 try/catch（plan 2258 前），xbiz 无法内联转码 → 转码被迫落在 Java Guard/Processor 或干脆缺失。

## Goals

- **G1**：全部 StateMachine `illegal()` 直抛领域码。**码选择粒度 = 每个 StateMachine Bean（每轴）**；每 Bean 先建"终码映射"：该 Bean 的非法边出口今天被调用方转码/直抛收敛到的码（**端到端不变锚，1:1 优先**）> 该 Bean 对应实体轴的既有实体专属码 > 域通用码（`erp.err.<domain>.illegal-status-transition`，缺失则本计划新建；仅适用于今天无转码层的裸奔 Bean）。params 保持 `action/currentStatus/expectedStatus`；域码模板参数键不同或需实体元数据时由 G2 同码补参补齐。
- **G2**：退役全部纯转码层（Guard.map / Processor catch-and-remap / facade try/catch / fin 码值判别 / mfg else 兜底）。两类保留形态（**码值与参数不变，cause 链退役**）：①带实体元数据增强的转码点改为**同码补参** `catch (NopException e) { throw e.param(<单据码/id 键>, ...); }`；②fin 码值判别在 StateMachine 直抛同一终码后，判别分支退役为同码补参（fin reconciliation 参数键为 `reconciliationId`+`docStatus`，注意非 `currentDocStatus`）。
- **G3**：矩阵/单元测试断言从 common 常量翻转为领域码；域内 E2E/集成测试断言的最终错误码值**不变**（迁移正确性的锚；适用范围 = 今日已有领域终码的实体——今日 common 裸奔的实体除外，其断言随码翻转并逐一声明）。
- **G4**：`ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 全仓零生产引用后标记 `@Deprecated`（javadoc 指向 lesson 19），保留定义防外部断裂。零引用判据覆盖：103 StateMachine、fin×4、mfg×2、`AbstractProcessor` common 通道（`defaultIllegalStatusException` 删除 + `illegalStatusException` 抽象化编译闭环；`validateDocStatus` 外部直调 0，直接删除）。
- **G5**：契约与文档同步（逐文件逐节）：`docs/architecture/entity-state-machine-bean.md` §7 错误码契约（:153-156）+ §11.1 步骤 3（:254）+ §11.4（:312）改写为"Bean 直抛领域码（实体元数据经同码补参在调用点补齐，不入 Bean，无状态性理由保留）"；`docs/design/assets/state-machine.md` §错误码迁移说明 + §适用对象二 §4（:219）+ §适用对象三 §4（:304-305）+ §适用对象四 §4（:367-368）同步；新建状态机照 lesson 19 自检清单执行。

## Non-Goals

- **不改 StateMachine assert API 签名**（不引入实体参数）——控制 103 类的 API churn；实体 id 元数据增强仅经 G2 同码补参通道，且仅在既有增强点保留。
- **不改变既有端到端错误码值**（今日已有领域终码的 Bean 码值不变；裸奔通道 common→domain 是有意的契约修正，逐一在 Phase 内声明）。
- 不动 `ERR_ENTITY_NOT_FOUND`/`ERR_CRUD_STATUS_LOCKED`/`ERR_CRUD_IMMUTABLE_ENTITY`（有与实体无关的直接消费方，不符合 lesson 19 反模式判据）。
- 不做 wf 引擎 / approval-support.xbiz 的任何改动。

## Scope

- In：18 模块全部 `*StateMachine.java` 的 `illegal()`；四类转码站点（≈153 文件）与 77 个骨架裸奔 Processor 的域码覆写；`AbstractProcessor` common 通道退役；矩阵/单元测试断言；各域 `*Errors.java` 新增域通用码；`ErpCommonErrors` deprecate；`state-machine.md`、`docs/architecture/entity-state-machine-bean.md`、lesson 15/19 关联更新。
- Out：前端错误码渲染、`_cases` 快照（错误码值不变则快照稳定；如个别快照内嵌 common 码字面量，按"运行时字面量批量改写协议"全仓清扫——盘点已证零命中）、数据库/模型。

## Execution Plan

### Phase 1 - 试点：assets Movement（裸奔实体，验证换码配方 form-0）

Status: completed
Targets: `module-assets/erp-ast-service`（ErpAstErrors、ErpAstMovementApprovalStateMachine、statemachine 测试）
Skill: none
Item Types: `Add | Fix`（新码 = Add；契约漂移修正 = Fix）
Prereqs: 无（首个 Phase）

- [x] `ErpAstErrors` 新增 `ERR_AST_MOVEMENT_ILLEGAL_STATUS_TRANSITION`（`erp.err.ast.movement.illegal-status-transition`，args: action/currentStatus/expectedStatus，与 Bean params 对齐）+ `ARG_ACTION` 键
      - Skill: none
- [x] `ErpAstMovementApprovalStateMachine.illegal()` 换抛新领域码 + javadoc 更新（裸奔通道修正声明：Movement 非法边端到端码从 common → 领域码，有意契约修正；xbiz 无 try/catch 已复核，common 码原样直达）
      - Skill: none
- [x] Movement 相关矩阵/域测试断言翻转（`TestErpAstMovementStateMachines` 2 处断言 + import 换 `ErpAstErrors`）
      - Skill: none
- [x] `module-assets` `mvn test -pl module-assets/erp-ast-service -am` 绿（**Tests run: 339, Failures: 0, Errors: 0，BUILD SUCCESS**）
      - Skill: none
- [x] **配方确认（form-0）**：沉淀"换码 + 测试翻转"标准 diff 形态——① Errors 接口补 ARG 键（缺失时）+ `ErrorCode.define(领域码, 中文模板, args)`；② StateMachine 换 import + `illegal()` 三 param 换源 + 类 javadoc/方法 javadoc 同步；③ 测试断言常量与 param 键来源换 `ErpXxErrors`。**本试点仅覆盖裸奔形态（form-0）**——catch-remap 退役（form-1）、同码补参（form-2）、骨架覆写（form-3）配方确认延伸至 Phase 2 首个实体批
      - Skill: none

Exit Criteria:

- [x] `ErpAstMovementApprovalStateMachine` 对 common 常量引用归零；Movement 非法边测试断言新领域码全绿
- [x] `module-assets` 测试绿（本地化验证，Phase 2 ast 批依赖本配方）

### Phase 2 - 存量转码域：sal / pur / inv（转码层退役主战场，配方 form-1/2/3 沉淀批）

Status: completed
Targets: `module-sales` / `module-purchase` / `module-inventory` 的 service 模块（StateMachine、Processor、Errors、测试）
Skill: none
Item Types: `Fix`-heavy
Prereqs: Phase 1 配方

- [x] **先建三域 Bean→终码映射表**（逐 Bean 落表后执行，证据见执行报告）：sal Delivery 双轴→域通用码，Order/Quotation/Invoice/Receipt/Return 双轴→各自实体专属码；pur Receive 双轴→域通用码，Order/Req/Invoice/Payment/Return/Quotation/Rfq 双轴→各自实体专属码；inv StockMove/CostAdjust/LandedCost→域通用码，StockTake/OwnershipTransfer→实体专属码，**TransferOrder→域通用码（映射冲突修正：原 confirm 站点 copy-paste 误用 StockTake 实体码 `erp.err.inv.stock-take.illegal-transition`，按映射规则修正为域通用码——有意契约修正，已记入）**
      - Skill: none
- [x] 三域全部 StateMachine `illegal()` 按映射表换抛终码（sal×12 / pur×16 / inv×6）+ javadoc 更新；**pur 裁定**：DOC 轴 SM 状态参数键用 `ARG_CURRENT_DOC_STATUS/ARG_EXPECTED_DOC_STATUS`（模板占位符即该键；缺失参数渲染空串会破坏端到端消息，nop-api-core `ApiStringHelper.renderTemplate:295-297` 实证），审批轴仍用 `ARG_CURRENT_STATUS/ARG_EXPECTED_STATUS`
      - Skill: none
- [x] 转码层退役：sal form-2×36 站点 / pur form-2×48 站点 / inv form-2×13 站点（全部同码补参——三域所有终码模板均含单据码参数，form-1 零站点符合预期）；pur 两 BizModel 死 helper `illegalStatus(...)` 删除
      - Skill: none
- [x] inv 7 个骨架裸奔 Processor 补域码 `illegalStatusException` 覆写（CostAdjust×5 + LandedCost×2 → 域通用码 + moveCode 参数；裸奔通道 common→领域码为有意契约修正）
      - Skill: none
- [x] 域测试断言核对：端到端锚零改动通过（sal TestErpSalOrderApproval/Invoice/DeliveryApproval/ReturnExchange 族、pur TestErpPur*Approval 族、inv TestErpInvStockMoveBizModel/Bookkeeping/StockTakeCompleteDiffMove）；矩阵级断言 common 翻转（sal×9 / pur×13 / inv×2 测试文件，inv 参数化套件按 Bean 断言各自终码）
      - Skill: none
- [x] 三模块 `mvn test` 绿：**sal 316 / pur 341 / inv 253，Failures 0 Errors 0（BUILD SUCCESS ×3）**
      - Skill: none

Exit Criteria:

- [x] 三域 StateMachine + Processor 生产文件对 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 引用归零（rg 复核零命中）
- [x] 三模块测试绿；sal/pur/inv 各抽实体域集成测试零改动通过（端到端码值不变锚成立）

### Phase 3 - hr（实体专属码 + Guard 退役）

Status: completed
Targets: `module-hr/erp-hr-service`（StateMachine×5、ErpHrSalaryApprovalGuard、AbstractErpHrSalaryProcessor、测试）
Skill: none
Item Types: `Fix`-heavy
Prereqs: Phase 2 配方

- [x] hr 5 个 StateMachine 按终码映射换抛实体专属码：SalaryApproval/SalaryPayment→`ERR_SALARY_ILLEGAL_STATUS_TRANSITION`（Guard :56 / ErpHrSalaryBizModel :125 证据）、LeaveRequest→`ERR_LEAVE_ILLEGAL_STATUS_TRANSITION`、EmploymentContract→`ERR_CONTRACT_ILLEGAL_STATUS_TRANSITION`、Timesheet→`ERR_HR_TIMESHEET_ILLEGAL_TRANSITION`；javadoc 同步
      - Skill: none
- [x] **Guard 转码退役（G2 ①裁定：保留 Bean、转同码补参富化委托）**：`ErpHrSalaryApprovalGuard.map()` 原 catch 重建异常 + cause 链退役为 `throw e.param(ARG_SALARY_ID, ...)`——salaryId 是端到端消息参数（删 Bean 则 xbiz 直调丢参数、违反 Gate 2 不变性），故不物理删除 Bean；`ErpHrSalary.xbiz` 机制注记同步更新；`AbstractErpHrSalaryProcessor` 旧契约 javadoc 修正（无活转码，仅注释）
      - Skill: none
- [x] 其余 hr 转码点同码补参退役：`AbstractErpHrLeaveRequestProcessor`（×3 站点 + `illegalTransition` helper 删除）、`ErpHrLeaveRequestBizModel`/`ErpHrEmploymentContractBizModel`/`ErpHrTimesheetBizModel`（×3）/`ErpHrSalaryBizModel`/`ErpHrSalaryMarkPaidProcessor`（×2 + 双 helper 删除，执行期盘点新增发现站点）
      - Skill: none
- [x] hr 测试绿：8 个测试文件断言翻转（含 `ErpHrLeaveRequestStateMachineDelta` fixture 换抛领域码）；**mvn test：Tests run: 249, Failures 0, Errors 0，BUILD SUCCESS**（`TestErpHrSalaryApprovalStateMachineMatrix` 断言翻转后全绿）
      - Skill: none

Exit Criteria:

- [x] hr 生产+测试文件对 common 常量引用归零（rg 复核零命中）；hr 模块测试绿

### Phase 4 - 其余域批次（ast 其余 + fin/mfg/prj/qa/mnt/log/crm/drp/b2b/ct/cs/md/aps）

Status: planned
Targets: 各域 service 模块（批次序固定，见下）
Skill: none
Item Types: `Add | Fix`-heavy（新建域通用码 = Add；换码/退役/覆写 = Fix）
Prereqs: Phase 2 配方（form-1/2/3）

- [ ] **固定批次序**（按工作量降序，逐批"映射表 → 换码 → 转码退役 → 裸奔覆写 → 测试翻转 → 模块测试绿 → 独立 commit"）：ast(15 SM + 12 转码 + 30 裸奔) → fin(12 SM + 4 判别 + 20 裸奔) → mfg(8 SM + 13 转码 + 2 兜底 + 10 裸奔) → prj(5+6+4) → qa(5+5) → mnt(5+9) → ct(3+6) → b2b(3+2) → drp(2) → crm(2+4+1 裸奔) → 单实体域（cs 1+2 / md 1+2 / aps 1+3 / log 1+1）
      - Skill: none
- [ ] fin 批次按 G2 ②处理码值判别 4 文件（参数键 `reconciliationId`+`docStatus`）；mfg 批次按 G2 处理 else 兜底 2 文件
      - Skill: none
- [ ] crm 批次复核 `AbstractProcessor.validateDocStatus` 外部直调数（已复核 = 0，执行期再证）；如 0 维持删除计划
      - Skill: none
- [ ] 每批次独立 commit（`refactor(<domain>): StateMachine 直抛领域码`）
      - Skill: none

Exit Criteria:

- [ ] 每批次：该域生产文件对 common 常量引用归零 + 该模块测试绿 + 独立 commit 落库

### Phase 5 - 收口

Status: planned
Targets: `module-common-service`、`ErpCommonErrors`、docs（architecture/design/lessons）、全仓验证
Skill: none
Item Types: `Fix | Proof`
Prereqs: Phase 1-4 全部完成

- [ ] `AbstractProcessor` common 通道退役：删除 `defaultIllegalStatusException` 与 `validateDocStatus`（外部直调 0 已证），`illegalStatusException` 改 abstract——149 具体 Processor 编译闭环（编译即门禁）
      - Skill: none
- [ ] `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 标记 `@Deprecated` + javadoc 指向 lesson 19（全仓零生产引用复核）
      - Skill: none
- [ ] G5 文档同步（三方逐节）：`entity-state-machine-bean.md` §7/§11.1/§11.4、`state-machine.md` §错误码迁移说明 + §适用对象二/三/四 异常路径行、lesson 15 关联行补计划回链
      - Skill: none
- [ ] 全仓 `mvn test` full-green verification（erp 权威口径：surefire XML 计数）
      - Skill: none
- [ ] 独立 closure audit（fresh session 子代理，逐 Phase 核对 Evidence）
      - Skill: closure-audit-prompt

Exit Criteria:

- [ ] `rg -l "ERR_ILLEGAL_STATUS_TRANSITION" --glob "*.java" module-*` 仅剩 `ErpCommonErrors.java` 定义处；定义处带 `@Deprecated`
- [ ] 全 reactor `mvn test` 0 失败（full-green，surefire XML 口径）

## Verification

- 每 Phase：`mvn test -pl <module-service> -am` 通过 + 该域生产文件对 `ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION` 引用归零（rg 复核）
- 最终：全 reactor `mvn test` 0 失败（full-green）；`rg -l "ERR_ILLEGAL_STATUS_TRANSITION" --glob "*.java" module-*` 仅剩 `ErpCommonErrors.java` 定义处；`rg -l "nop\.err\.erp\.common\.illegal-status-transition" --glob "!*.md" .` 仅剩定义处

## Draft Review Record

- Independent draft review iteration 1: needs revision (subagent agent_38d23d6e, fresh session, 2026-09-08) because BLOCKER-1：Phase 2 原案"全部换抛既有域通用码"与 G1 端到端不变锚冲突——sal/pur 各 5 实体与 inv 2 实体今日转码终码为实体专属码（实仓复核 `ErpSalErrors.java:79/112/140/157/187` 等），一刀切将翻转约 12 个实体端到端码；MAJOR-1：G1 判据粒度应为每 Bean（每轴）而非每实体（双轴双码实体为常态）；MAJOR-2：G5 文档清单不足（entity-state-machine-bean.md §11.1/§11.4 与 state-machine.md §适用对象二/三/四异常路径行）；MINOR×4（测试文件 109→119、G4 validateDocStatus 归因更正、guide 规则 7/8 类型与 Skill 标注缺失、试点仅覆盖 form-0）。同时复核确认：核心计数底盘（103/153/77/149/fin4/mfg2/字面量1文件）全部诚实，Phase 5 编译闭环方案无遗漏消费方，G2 在 fin 判别形态上可行（参数键 docStatus 而非 currentDocStatus）。计划已按修订要求清单全量重写（本轮全文即修订版）。
- Independent draft review iteration 2: accept (subagent agent_38d23d6e, fresh session, 2026-09-08) after 全量重写落实 iteration 1 全部修订要求（BLOCKER-1 / MAJOR-1 / MAJOR-2 / MINOR 1-4 / 两处措辞逐项 pass）+ 修订版新增断言实仓抽查通过（ERR_BUDGET_SCENARIO_ILLEGAL_TRANSITION 实存 ErpFinErrors.java:418、fin 转码 9 文件构成、软措辞零命中）→ Plan Status 置 active 进入实施。非阻塞风险 3 项（fin 批括注未单列 6 纯转码文件 / Phase 2 Fix-heavy 计数表述 / hr-ct-cs 专属码清单未穷举）由每批映射表先行 + 归零门 + 模块测试门结构兜底，执行时补记。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成（G1-G5 全勾且与 live repo 证据成对：文件路径 + 测试名）
- [ ] 端到端码值不变性抽查：sal/pur/inv 各抽 1 实体的域集成测试零改动通过（今日已有领域终码实体）
- [ ] 相关文档对齐：lesson 19 / state-machine.md / entity-state-machine-bean.md / 本 plan 四方口径一致
- [ ] 已运行验证：全 reactor `mvn test` full-green（surefire XML 口径）+ 双 rg 门（java 常量引用仅定义处；字符串字面量仅定义处）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（iteration 1 needs revision → 修订 → iteration 2 accept）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无——范围内项目不降级；执行期若发现单个实体多轴多码无法 1:1 收敛，按 G1 落到域通用码并在该批次 commit message 与计划 Phase 条目声明，不作 silent deferred。）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理指针>
- Evidence: <task id / 逐 Phase 核对记录>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现>
