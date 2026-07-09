# 2026-07-09-1249-1-p2p-o2c-orchestration-e2e 核心业财端到端编排链浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Mission: erp
> Work Item: 各域细化端到端验证（P2P + O2C 跨域编排链浏览器层 E2E + 业财过账产物验证）
> Source: `docs/backlog/extended-roadmap.md` 全 done 后剩余 deferred 项；`2026-07-09-0814-2` Deferred「跨域编排链完整 E2E（P2P/O2C 全链）」+「业财过账凭证精确数值断言」
> Related: `2026-07-09-0814-2`（业务动作浏览器层 E2E 范式源）、`2026-07-03-1018-1`（P2P/O2C 后端集成测试范式源）、`2026-07-08-1445-1`（P2P/O2C 种子源）
> Audit: required

## Current Baseline

- E2E 套件已建立（`2026-07-08-0637-1` 起建立 Playwright 基础设施），当前 108 测试全绿（10 看板冒烟 + 28 数值断言 + 24 报表冒烟 + 18 CRUD 冒烟 + 13 列表断言 + 4 写路径 + 1 KB + 3 单域业务动作）。运行手册 `docs/testing/e2e-runbook.md`；基线见 `docs/testing/known-good-baselines.md`。
- **单域业务动作 E2E 范式已建立**（`2026-07-09-0814-2`）：`tests/e2e/business-actions/_helper.ts` 提供三原语 `createViaSave`（经 `__save` 建实体）/ `callMutation`（经 GraphQL mutation 调自定义 `@BizMutation`）/ `verifyState`（经 `__get` 断言状态字段），覆盖 inventory StockMove 状态机+过账、CRM Lead 状态迁移、CS Ticket 六态状态机。
- **跨域编排链后端集成测试已全绿**（`2026-07-03-1018-1`）：`TestErpPurProcureToPayEnd`（P2P：PO→Receive→Invoice→Pay→settle + 反向冲销）、`TestErpSalOrderToCashEnd`（O2C：SO→Delivery→Invoice→Receipt→settle + 反向冲销），经 `IGraphQLEngine.executeRpc` 调用与浏览器层相同的 `@BizMutation`。后端测试种子经直接 ORM 创建（非 `__save`），用户上下文为 SYS(id=0)。
- **P2P/O2C 部署期种子已落地**（`2026-07-08-1445-1`）：23 张交易 CSV（PO/Receive/Invoice/Payment + SO/Delivery/Invoice/Receipt 头+行 + 已过账财务产物），种子库 91 CSV 全绿。但种子是「已过账终态直 seed」，非经业务动作链路驱动。
- **链路 BizModel @BizMutation 全可达**（经 explore 核实，见 Task Route）：
  - PO/SO：`submitForApproval`→`approve`（DIRECT 模式，无过账无库存，SO approve 含信用控制 `CreditLimitChecker.check`）。
  - Receive/Delivery：`submitForApproval`→`approve`（DIRECT 模式，approve 触发库存移动 `ErpInvStockMove` + posted 标记 + 订单 receiveStatus/deliveryStatus 回写；含质检门控 config-gated）。
  - Invoice：`submitForApproval`→`approve`（DIRECT 模式，采购发票含三单匹配 `ThreeWayMatcher.match`，approve 触发 GL 过账 posted=true）。
  - Payment/Receipt：`submitForApproval`→`approve`（**WORKFLOW 模式 xwf**，`wf:wfName`=payment-approval/receipt-approval，submit 启动 wf 实例含 finance-approval 手动步骤；approve 经 wf 结束 listener 回调）。
- **剩余差距**：(1) 核心业财循环（P2P/O2C）的浏览器层端到端验证缺失——当前仅后端集成测试 + 种子静态终态，未经浏览器层 GraphQL 链路驱动验证全链状态流转 + 过账产物。(2) 过账凭证数值断言在浏览器层缺失（0814-2 仅验证「过账触发且产物存在」，未断言凭证借贷数值）。

## Goals

- 建立 P2P（PO→Receive→Invoice）+ O2C（SO→Delivery→Invoice）核心链路浏览器层 E2E：经 GraphQL `__save` + `submitForApproval` + `approve` mutation 驱动全链，每步断言状态流转（docStatus/approveStatus/posted/receiveStatus/deliveryStatus）。
- 断言业财过账产物：Receive/Delivery approve 后 `ErpInvStockMove` 产物存在；Invoice approve 后 GL 凭证产物存在（posted=true + voucher bill_r 回链 + AR/AP 辅助账项），凭证含确定性数值 token。
- 扩展业务动作 E2E 范式至跨域编排（多实体链式创建 + 状态联动 + 过账产物清理），为后续域 successor 提供可复用范式。

## Non-Goals

- Payment/Receipt + 域级 settle 核销链路浏览器层 E2E——Payment/Receipt 为 xwf WORKFLOW 模式，浏览器层 `nop` 用户上下文与后端测试 SYS(id=0) 不同，xwf caller 解析存在未验证风险。归 Phase 1 Explore 裁决后决定是否纳入或归 Deferred successor。
- 审批工作流（xwf）浏览器层完整审批流 E2E（wf 步骤 agree/disagree 经 wf API）——独立能力面（0814-2 Deferred「审批工作流 xwf 业务动作 E2E」）。
- 全 18 域全业务动作覆盖——3 代表域 + 本计划 2 核心链已证明范式，其余域同范式 successor（0814-2 Deferred「全 18 域全业务动作覆盖」）。
- 反向冲销（reverseApprove 红字凭证）浏览器层 E2E——后端 1018-1 已覆盖反向路径；浏览器层反向归 successor。
- 财务正式核销单 `ErpFinReconciliation` 浏览器层 E2E——独立财务面 successor。

## Task Route

- Type: `verification or audit work`（浏览器层端到端验证，纯消费侧测试新增，零生产代码/契约/模型变更）
- Owner Docs: `docs/design/flow-overview.md`（P2P/O2C 全链设计）、`docs/architecture/posting.md`（过账机制）、`docs/testing/e2e-runbook.md`（E2E 运行手册）
- Skill Selection Basis: `nop-testing`（E2E 套件 @BizMutation 经 GraphQL 驱动范式，0814-2 已验证 helper 复用）；`nop-frontend-dev`（浏览器层 page.request 认证会话建立，既有 fixtures 复用，本期无页面定制）。**已加载 explore 核实结果**（P2P/O2C BizModel @BizMutation 全链可达性 + xwf 风险），作为基线事实来源。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer 含 fresh-DB + 91 CSV 种子 + 序列推进 + auth fixtures）。
- 种子库已有 ACTIVE 供应商/客户/物料/仓库/科目体系/会计期间（1445-1 种子）。链路创建的实体需引用种子 id 或经 `__save` 创建后获取 id。
- **前置 Explore**：确认浏览器层 `nop` 用户上下文下 Receive/Delivery approve 的质检门控不阻塞（config-gated `erp-qua.*` 默认关），以及 SO approve 信用控制不超限（种子客户 creditLimit 足够覆盖测试订单金额）。

## Execution Plan

### Phase 1 - P2P 核心链路 E2E + 过账产物验证（PO→Receive→Invoice）

Status: completed
Targets: `tests/e2e/orchestration/_helper.ts`（新建跨域编排 helper）、`tests/e2e/orchestration/p2p-chain.spec.ts`
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: 既有 0814-2 `_helper.ts` 三原语（createViaSave/callMutation/verifyState）+ 既有 fixtures（loginAndNavigate）

- [x] `Decision`：P2P 链路实体创建策略裁决——(a) 经 `__save` 全链创建（PO head+line → Receive head+line → Invoice head+line），每步获取返回 id 驱动下一步；vs (b) 引用种子既有 PO 经 `__save` 仅创建 Receive/Invoice。选择 (a) 全链 `__save` 创建以验证完整创建→审批→过账路径；唯一编码用唯一前缀（如 `E2E-P2P-<ts>`）避免与种子/其他 spec 冲突。
  - 记录替代方案：(b) 引用种子 PO 更简但仅验证下游链路，遗漏 PO 创建+审批路径。
  - 残留风险：`__save` 头-行实体的 GraphQL input 类型字段集需与 ORM mandatory 字段对齐（经 ORM 核实填充）。
  - **实现裁决**：行实体 `registerShortName=true`（独立 GraphQL 端点），采用头 `__save` + 行独立 `__save`（FK 显式引用头 id），比嵌套 `lines` 更可控。mandatory 字段经 ORM 逐项核实填充。
  - **执行期发现（COA 完备性）**：执行发现种子 COA（`erp_md_subject.csv` 8 科目：1001/1002/1122/1405/2202/5001/6001/6601）与过账 Provider 硬编码科目码不一致——`PurAcctDocProvider`(1403/2221/2202)、`SalAcctDocProvider`(1131/6001/2221)、`InvAcctDocProvider`(1401/6401/2202) 所需的 1403/2221/1131/1401/6401 在种子中缺失，致 `resolveSubjects` 抛 `ERR_SUBJECT_NOT_FOUND`→过账优雅降级 posted=false（与 0814-2 inventory spec 观测一致）。计划 front matter「No infra prereqs」系作者错误假设。裁决：补齐种子 COA（`erp_md_subject.csv` +5 行：1401/1403/1131/2221/6401），`findByCode` 全局按码解析无需 COA 映射；补齐后发票/移动单过账 happy-path 可达（posted=true + 凭证 + AR-AP 辅助账）。安全性核证：`ErpFinPostingProcessor.persistVoucher` 仅写 voucher/voucher_line/voucher_bill_r（**不写 gl_balance**），finance 看板/资产负债表/利润表读 gl_balance 不受影响；既有 inventory spec 独立 INCOMING 移动现也过账（posted 由 false→true），其断言 `typeof posted==='boolean'` 仍通过、清理不涉凭证且 finance list-value 用 `>=4` 容差，全套件 0 回归（110 passed 实证）。此为种子演示数据完备性修复（非生产代码/契约/模型变更），透明记录于本计划 + e2e-runbook + 日志。
  - Skill: `nop-testing`
- [x] `Add`：`tests/e2e/orchestration/_helper.ts`——跨域编排 helper，扩展 0814-2 三原语为链式驱动：`runP2pChain(page)` 编排 PO `__save`(head+line) → `submitForApproval` → `approve` → Receive `__save`(head+line, orderId 引用) → `submitForApproval` → `approve`（断言 posted=true + stockMove 产物）→ Invoice `__save`(head+line, receiveLineId 引用) → `submitForApproval` → `approve`（断言 posted=true + voucher 产物）；每步经 `verifyState` 断言 approveStatus 翻转；返回各实体 id 供断言/清理。含清理原语 `cleanupP2p`（逐域逻辑删除：AP 辅助账→发票凭证(行/回链)→发票(行)→移动单凭证+流水+余额+移动单(行)→入库(行)→订单(行)，保护共享 DB 数值断言基线）。
  - Skill: `nop-testing`
- [x] `Add`：`tests/e2e/orchestration/p2p-chain.spec.ts`——P2P 全链 E2E：(1) 状态流转断言——PO/Receive/Invoice approveStatus UNSUBMITTED→SUBMITTED→APPROVED（helper 内每步 `verifyState` 经 `__get` 独立验证）；(2) 库存移动断言——Receive approve 后 `ErpInvStockMove`(relatedBillType=ERP_PUR_RECEIVE, relatedBillCode=receive.code) findPage total≥1 + docStatus=DONE；(3) GL 过账断言——Invoice approve 后 Invoice posted=true + `ErpFinVoucherBillR`(billCode=invoice.code) total≥1 + `ErpFinArApItem`(sourceBillType=AP_INVOICE, sourceBillCode) direction=PAYABLE + openAmountSource=含税总额 56.5（确定性：行 10×5=50，税 6.5）+ status=OPEN。
  - Skill: `nop-testing`
- [x] `Proof`：运行 `npx playwright test tests/e2e/orchestration/p2p-chain.spec.ts --workers=1`，P2P 全链全绿（7.3s）；全套件 `npx playwright test --workers=1` 110 passed（108 既有 + 2 orchestration）0 回归。
  - 验证命令：`npx playwright test tests/e2e/orchestration/ --workers=1`（2 passed）+ 全套件 `npx playwright test --workers=1`（110 passed, 14.9m）
  - Skill: `nop-testing`

Exit Criteria:

- [x] P2P 链路 PO→Receive→Invoice 全状态流转断言全绿（每步 approveStatus 翻转经 __get 独立验证）
- [x] Receive approve 后 stockMove 产物存在（findPage total≥1，docStatus=DONE）；Invoice approve 后 posted=true + voucher 回链存在 + AP 辅助账项存在（PAYABLE/openAmount=56.5/OPEN）

### Phase 2 - O2C 核心链路 E2E + 过账产物验证（SO→Delivery→Invoice）

Status: completed
Targets: `tests/e2e/orchestration/_helper.ts`（增 `runO2cChain`）、`tests/e2e/orchestration/o2c-chain.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 helper 范式已建立

- [x] `Add`：`_helper.ts` 增 `runO2cChain(page)`——SO `__save`(head+line) → `submitForApproval` → `approve`（信用控制通过，CUST-001 creditLimit=500000 覆盖订单含税 113）→ Delivery `__save`(head+line, orderId 引用) → `submitForApproval` → `approve`（库存出库 + posted + SO deliveryStatus 回写）→ Invoice `__save`(head+line, deliveryLineId 引用) → `submitForApproval` → `approve`（GL 过账 posted=true）；清理原语 `cleanupO2c` 同 Phase 1 范式。
  - **备货前置裁决**：WH-RAW/MAT-1 种子无余额，出库会因负库存禁止（`CONFIG_ALLOW_NEGATIVE_STOCK` 默认 false）失败。链路前先 `generateMove` INCOMING 备货 20（独立移动 → CONFIRMED → `complete` → DONE），出库消费 10 余 10。WH-RAW/MAT-1 余额无种子行，清理时整行删除安全（不污染 inventory dashboard totalValue 基线）。
  - Skill: `nop-testing`
- [x] `Add`：`tests/e2e/orchestration/o2c-chain.spec.ts`——O2C 全链 E2E：状态流转（SO/Delivery/Invoice approveStatus UNSUBMITTED→SUBMITTED→APPROVED）+ 库存出库移动断言（Delivery approve 后 stockMove relatedBillType=ERP_SAL_DELIVERY 存在 + DONE）+ GL 过账断言（Invoice approve 后 posted=true + voucher 回链 + AR 辅助账项 direction=RECEIVABLE + openAmountSource=含税总额 113（行 10×10=100，税 13）+ status=OPEN）。
  - Skill: `nop-testing`
- [x] `Proof`：运行 `npx playwright test tests/e2e/orchestration/o2c-chain.spec.ts --workers=1`，O2C 全链全绿（7.7s）；全套件 0 回归。
  - Skill: `nop-testing`

Exit Criteria:

- [x] O2C 链路 SO→Delivery→Invoice 全状态流转断言全绿
- [x] Delivery approve 后 stockMove 产物存在（docStatus=DONE）；Invoice approve 后 posted=true + voucher 回链存在 + AR 辅助账项存在（RECEIVABLE/openAmount=113/OPEN）

### Phase 3 - Payment/Receipt xwf 可行性裁决 + 文档对齐

Status: completed
Targets: `tests/e2e/orchestration/`（条件性新增）、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: `nop-testing`

- Item Types: `Decision | Add | Follow-up`
- Prereqs: Phase 1/2 全绿

- [x] `Decision`：Payment/Receipt xwf 浏览器层可行性裁决——经原型 spec 验证浏览器层 `nop` 用户上下文下 Payment `submitForApproval`→`approve` 是否成功。
  - **裁决结论：不可行 → 归 Deferred successor。** 原型实证（`_prototype-payment.spec.ts`，裁决后删除）：`nop` 用户创建 Payment 后调 `submitForApproval`，xwf 引擎返回 `步骤[submit:...]不允许被用户[<nop uuid>]调用,步骤的参与者限定为[user:$0]`——wf `submit` 步骤参与者限定为 `user:$0`（SYS id=0，后端测试 `setUserId("0")` 规避点），`nop` 浏览器用户（UUID id）不匹配致 submit 被拒，Payment 停留 UNSUBMITTED，后续 `approve` 因状态守卫（期望 SUBMITTED）失败。根因：xwf WORKFLOW 模式的步骤参与者授权与浏览器层默认 `nop` 用户上下文不兼容。
  - 记录替代方案：(a) 经 wf API GraphQL 推进 finance-approval 步骤 agree——但 `submit` 步骤本身即受 `user:$0` 限制，wf API agree 同样面临参与者授权，不可绕过；(b) 浏览器层注入 SYS 上下文——平台不支持（浏览器认证固定 `nop` 用户）；(c) 仅到 Invoice 截断——**采纳**（Phase 1/2 已完整覆盖 PO/SO→入库/出库→发票→过账核心业财链）。
  - 残留风险：无——核心业财正确性（创建→审批→过账→AR-AP）经 DIRECT 审批链已完整验证；Payment/Receipt 属资金收付+核销层，xwf 浏览器层属独立能力面。
  - Skill: `nop-testing`
- [x] `Add`：（条件性，仅 Phase 3 Decision 裁决为「可行」时）——**未触发**（裁决为不可行）。`tests/e2e/orchestration/p2p-payment.spec.ts` + `o2c-receipt.spec.ts` 不创建，归 Deferred successor。
  - Skill: `nop-testing`
- [x] `Add`：`docs/testing/e2e-runbook.md` 增跨域编排层段（P2P/O2C 链路 spec + helper 范式 + 种子 COA 完备性修复 + xwf 裁决结论）+ 套件计数更新（108→110）；`docs/testing/known-good-baselines.md` 增本计划基线行。
  - Skill: none

Exit Criteria:

- [x] Payment/Receipt xwf 浏览器层可行性裁决已记录（裁决不可行 → Deferred 带触发条件，见下「Deferred But Adjudicated」）
- [x] e2e-runbook 含跨域编排段 + 套件计数（110）；known-good-baselines 含基线行

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0babea457ffejBDvNDrfWIHtAf`，独立 general 子代理，新会话冷重播无执行者上下文) — 基线逐项 live 验真全 PASS（_helper.ts 三原语 / TestErpPurProcureToPayEnd RPC 驱动 / 108 测试计数精确 / Receive approve→posted / Invoice approve→GL posted / PO-SO DIRECT 无过账 / SO CreditLimitChecker / Payment-Receipt xwf WORKFLOW wf:wfName 核实）。结构合规（item typing / 反松弛 / skill 记录 / Decision 替代方案 + 残留风险 / Deferred 诚实 / 单结果面 Rule 4 / Closure Gates 行为驱动适配）。3 NOTE（mvn 门控 rationale 弱但无害 / 种子 COA 对齐隐含风险已覆盖 / 头-行 __save 无 E2E 先例但残留风险已诚实标注）。无 BLOCKER/MAJOR。草案收敛为可接受执行契约，计划转 `active`。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），结束前除下方门控外运行一次完整 E2E 套件（含新增 orchestration spec + 既有 spec）+ 既有后端构建（确认 E2E 未污染后端）。

- [x] 范围内行为完成（P2P + O2C 核心链路浏览器层 E2E 全绿 + 过账产物断言）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines）
- [x] 已运行验证：`npx playwright test`（全套件 110 passed 0 回归）+ `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，E2E 新增文件在根 tests/ 非 reactor 模块，无后端污染；jar 含新种子 COA 核实）
- [x] 无范围内项目降级为 deferred/follow-up（Payment/Receipt xwf 经 Phase 3 原型裁决不可行 → 归 Deferred 带触发条件，系计划内 Non-Goal 裁决，非范围内缺陷降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话 `ses_0ba72ff31ffeCByLk9fdQcEt4X`）执行；执行者未自我审计
- [x] 结束证据存在于文件中（见下 Closure Audit Evidence）

## Deferred But Adjudicated

### Payment/Receipt xwf 浏览器层 E2E + 域级 settle 核销

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Payment/Receipt 为 xwf WORKFLOW 模式，浏览器层 `nop` 用户上下文下 wf caller 解析存在未验证风险（后端测试用 SYS(id=0) 规避）。核心链路（PO/SO→入库/出库→发票→过账）经 DIRECT 审批已完整验证业财打通正确性；Payment/Receipt 属资金收付+核销层，经 Phase 3 Explore 裁决后定。
- Successor Required: `yes`
- Trigger Condition: 当 xwf 浏览器层审批 API 验证可行或 nop 用户 wf 委托配置落地时；或经 Phase 3 Explore 原型验证浏览器层直接 approve 成功时纳入本计划。
- **经 plan `2026-07-09-2330-1` 权威裁决：不可行（NOT FEASIBLE）**。3 条候选路径均阻断：① wf 委托机制（`NopAuthUserSubstitution` nop→0）存在但 sysUser(0) 无法经浏览器层 `__save` 物化（`NopAuthUser.userId` `tagSet="seq"` 覆盖显式 "0" 为 UUID）；② 浏览器层无用户身份注入/伪装 API；③ `.xwf` submit step `<assignment>` 放宽属生产审批契约变更。后续步骤（finance-approval/cc-finance）经 `WorkflowService__invokeAction` GraphQL 浏览器层可达（actorType=all），但依赖先突破 submit user:$0 阻塞。**重评触发条件更新**：当 nop-entropy 平台支持浏览器层测试用户身份映射 / 委托免 sysUser(0) 物化 / sysUser 种子物化时。详见 `docs/testing/e2e-runbook.md`「useWorkflow 审批轴浏览器层（xwf）」段 + plan 2330-1 Phase 1 Decision。

### 反向冲销（reverseApprove 红字凭证）浏览器层 E2E

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端 1018-1 已覆盖反向路径（发票/付款 reverseApprove 红字冲销 + posted 反转）。浏览器层反向归 successor。
- Successor Required: `yes`
- Trigger Condition: 当需浏览器层验证反向冲销端到端时。

## Closure

Status Note: 已完成。P2P（PO→Receive→Invoice）+ O2C（SO→Delivery→Invoice）核心业财循环跨域编排链浏览器层 E2E 全绿（2 spec）：经 GraphQL 驱动全链 `__save`(头+行)→`submitForApproval`→`approve`，每步 `verifyState` 经 `__get` 断言 approveStatus 翻转 + 业财过账产物断言（Receive/Delivery approve→stockMove DONE；Invoice approve→posted=true + voucher bill_r 回链 + AR-AP 辅助账 PAYABLE 56.5 / RECEIVABLE 113 / OPEN）。**执行期发现并修复种子 COA 完备性缺口**（`erp_md_subject.csv` +5 科目 1401/1403/1131/2221/6401，使过账 Provider 硬编码科目码可达；persistVoucher 不写 gl_balance 故 finance 看板基线不受影响）。**Payment/Receipt xwf 裁决不可行**（nop 用户被 wf 步骤参与者 user:$0 拒绝）→ 归 Deferred successor 带触发条件。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test` 全套件 110 passed 0 回归（108→110）。纯测试新增 + 种子演示数据完备性修复，零 ORM/契约/Java 业务代码变更。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0ba72ff31ffeCByLk9fdQcEt4X`（explore，新会话冷重播无执行者上下文，2026-07-09）。审计对 LIVE 仓库逐项核实，结论 **ACCEPT（实质内容全 PASS，非空壳/正确/诚实）**：
  - 文件落地非空壳 PASS：`_helper.ts`（414 行真实 GraphQL 逻辑 runP2pChain/runO2cChain/cleanup*/findItems/cleanupVoucherByBillCode）+ 2 spec（真实链驱动 + stockMove/voucher/AR-AP 断言）+ 原型 spec 已删除。
  - 种子 COA +5 科目（1401/1403/1131/2221/6401）实测存在 PASS。
  - 计划内部一致性 PASS（Plan/Phase Status 全 completed，Phase/Exit 全 [x]，Phase 3 xwf 裁决不可行诚实记录）。
  - 文档对齐 PASS（e2e-runbook 110 计数 + 跨域编排段、known-good-baselines 基线行、backlog/README ✅ done）。
  - Deferred 诚实 PASS（Payment/Receipt xwf 带触发条件，非隐藏缺陷）。
  - 反空壳抽查 PASS（helper 真实调 createViaSave/callMutationOk/verifyState + 清理真实调 deleteByFilter/deleteById；实体名/常量 ERP_PUR_RECEIVE/ERP_SAL_DELIVERY/AP_INVOICE/AR_INVOICE/PAYABLE/RECEIVABLE 经 ORM/常量类核实；金额 56.5/113 自洽）。
  - 文本一致性 PASS（审计首轮发现 8 个 Closure Gates 未勾 + Closure 占位符未填——已据此修订：勾全部 8 Gates + 填本段 Status Note/Audit Evidence）。
  - 唯一遗留：无 BLOCKER/MAJOR；审计报告的 closure bookkeeping 缺陷已闭环修复。

Follow-up:

- Payment/Receipt xwf 浏览器层 E2E + 域级 settle 核销（Deferred，触发条件见上「Deferred But Adjudicated」）。
- 反向冲销（reverseApprove 红字凭证）浏览器层 E2E（Deferred）。
