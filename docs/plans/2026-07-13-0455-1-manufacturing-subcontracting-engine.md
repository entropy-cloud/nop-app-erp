# 2026-07-13-0455-1-manufacturing-subcontracting-engine 制造委外引擎（委外单生命周期 + MRP 释放 + GL 过账）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Source: `docs/audits/2026-07-12-1504-competitive-levers-implementation-audit.md` 裁决 M-1（委外 successor）；`docs/backlog/README.md` P8 行
> Related: `2026-07-13-0455-2-manufacturing-cost-element-decomposition.md`（N=2，承接本计划委外费归集源）；`2026-07-12-1504-1-competitive-comparison-correction.md`（裁决源）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD=6d34e665 范围）：

- **委外 BizModel 为 15 行 CRUD 空壳**：`module-manufacturing/erp-mfg-service/.../entity/ErpMfgSubcontractOrderBizModel.java`（及 `ErpMfgSubcontractOrderLineBizModel.java`）仅 `extends CrudBizModel` 构造器，零自定义方法，`IErpMfgSubcontractOrderBiz` 接口空。`competitive-comparison.md` 杠杆 F 声明"委外"未兑现（审计 `docs/audits/2026-07-12-1504-...` §五 M-1）。
- **MRP 委外释放明示不支持**：`MrpReleaseService.java:49` 注释「SUBCONTRACT_REQUEST（委外）释放——委外流程独立面，本期不支持」；`releasePurchaseRequest`/`releaseWorkRequest` 之外无 `releaseSubcontractRequest`；`requireReleasable`（:91）仅接受 PURCHASE_REQUEST/WORK_ORDER_REQUEST，`ErpMfgConstants.MRP_ORDER_TYPE_SUBCONTRACT_REQUEST="SUBCONTRACT_REQUEST"`（:100）+ ORM 字典 `erp-mfg/mrp-order-type`（app-erp-manufacturing.orm.xml:83）SUBCONTRACT_REQUEST 选项均已就位待用。
- **委外订单实体骨架已就位**：`app-erp-manufacturing.orm.xml` `ErpMfgSubcontractOrder`（:1088-1169）头含 supplierId/productId/businessDate/currencyId/exchangeRate/processingFee/totalAmount/docStatus/approveStatus + **posted/postedStatus/postedAt/postedBy 过账四件套**（:1110-1113）+ `tagSet="use-approval"`；`ErpMfgSubcontractOrderLine`（:1172-1208）含 materialId/quantity/unitProcessingFee/amount。**但 `erp-mfg/subcontract-status` 字典（:97-101）仅 3 态 DRAFT/ACTIVE/CANCELLED**，设计文档 `docs/design/manufacturing/subcontracting.md:82-93` 定义 10 态——字典 materially 不完整。
- **缺设计文档所述的 Issue/Receipt/Invoice 三个业务对象**：`subcontracting.md:54-60` 列出委外发料单/收货单/发票，ORM 中均无对应实体。
- **业财过账类型无 SUBCONTRACT**：`ErpFinBusinessType.java`（module-finance/erp-fin-dao/...）最高码=501（MANUFACTURING_ISSUE），无任何 SUBCONTRACT_* 类型；新增须同步枚举常量 + `erp-fin/business-type` 字典（该枚举文档 :10-11 明示）。
- **可镜像的成熟范式**：WorkOrder BizModel（thin Facade，:20-30）+ `ErpMfgWorkOrderProcessor`（510 行 protected step 编排：状态守卫→跨域 `IErpInvStockMoveBiz` 写移动单→reload→过账→状态迁移）；`ManufacturingIssuePostingDispatcher`+`ManufacturingIssueAcctDocProvider`（Dispatcher build PostingEvent→`executor.postEvent`→成功标 posted=true / 失败捕获保持 posted=false 由 DeferredPostingSweepJob 兜底）+ `ErpFinAcctDocRegistry` 注册。
- **种子 COA 缺"委外物资/在途物资"科目**：既有 `erp_md_subject.csv` 经 0704-1/1800-1/0413-2 多次补充含 1401/1403/1411 等，但委外专用中转科目（如 1408 委外物资）尚未确认存在，需核实/补种。

剩余差距：委外订单生命周期（发料出库→收回入库→加工费过账）+ MRP 委外释放全缺；字典状态不全；无 SUBCONTRACT 过账类型。

## Goals

- 交付委外加工单核心生命周期：审批后**发料给供应商**（inventory OUTGOING 移动 + 材料成本出库）→**收回加工品入库**（inventory INCOMING 移动 + 成品入库）→**加工费过账**（生成应付凭证，借委外物资/贷应付账款），全部走既有 `IErpInvStockMoveBiz` + finance 过账通道。
- 释放 MRP SUBCONTRACT_REQUEST → 自动创建 APPROVED 委外订单（镜像 releaseWorkRequest 范式）。
- 状态机对齐设计的核心可执行态（DRAFT→SUBMITTED→APPROVED→ISSUED→RECEIVED→COMPLETED + CANCELLED/REJECTED），`posted` 四件套随加工费过账翻转。
- 新增 SUBCONTRACT_* 过账业务类型 + 字典 + COA 科目，经 PostingDispatcher/Provider 两类范式落库。
- 委外费归集数据源就位，解除 `2026-07-13-0455-2`（N=2 成本要素拆分 subcontract 列）阻塞。

## Non-Goals

- **不引入独立的 SubcontractIssue/SubcontractReceipt/SubcontractInvoice 三个实体**（设计 `subcontracting.md` 的完整四业务对象形态）——本期以委外订单为编排根，发料/收货经订单动作 + inventory StockMove 留痕（对齐 Odoo mrp_subcontracting 复用 stock.picking 范式，非每动作一实体）；独立单据实体归 successor。
- 不做供应商 Portal 协同 / 来料质检触发（quality 跨域）/ 损耗核算 / 退货红字 / 批次序列号控制（均 design `subcontracting.md` 完整态，归 successor）。
- 不做成本滚算 overhead/subcontract 要素拆分——归 N=2 计划 `2026-07-13-0455-2`。
- 不做委外差异（ProductionVarianceCalculator subcontract 差异类型）——successor。
- 不做委外浏览器层 E2E（Playwright）——后端落地后 successor。
- 不做委外前端 AMIS 页面定制（codegen 已生成基础 CRUD 页；状态机/动作按钮前端接入归 successor）。

## Task Route

- Type: `implementation-only change`（承接已审计 successor，设计 `subcontracting.md` 已就位；核心是补业务逻辑，非新设计）
- Owner Docs: `docs/design/manufacturing/subcontracting.md`（主）、`docs/design/manufacturing/state-machine.md`、`docs/design/manufacturing/README.md`、`docs/design/finance/posting.md`
- Skill Selection Basis: 委外 BizModel/Processor/跨实体编排/过账 → 匹配 `nop-backend-dev`（BizModel/Processor/task 选择、protected step、跨实体调用、错误码）；ORM 字典扩展属保护区域 ask-first，技能不替代人工批准。
- Protected Areas: `model/*.orm.xml`（字典扩展）= **ask-first**；`accounting/finance postings`（SUBCONTRACT_* 类型 + 凭证）= **plan-first**。实施须在本计划经独立草案审查 + ORM 字典变更人工批准后方可开始。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（复用既有 inventory/finance 服务）。
- 新增配置键（config-gated，默认安全值）：`erp-mfg.subcontract-posting-enabled`（默认 false 向后兼容，对齐既有制造过账 config 范式）；MRP 委外释放复用既有 `erp-mfg.forecast-consume-enabled` 同层 config 风格。
- COA 种子：`_vfs/_init-data/erp_md_subject.csv` 补"委外物资"中转科目（核实后补种，码值对齐 owner doc `subcontracting.md:111` 借/贷科目）。

## Execution Plan

### Phase 1 — 字典 / 过账类型 / COA 契约（保护区域 ask-first + plan-first）

Status: completed
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（`erp-mfg/subcontract-status` 字典）；`module-finance/erp-fin-dao/.../ErpFinBusinessType.java` + `erp-fin/business-type` 字典；`_vfs/_init-data/erp_md_subject.csv`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] **Decision: 委外单状态机裁剪口径**——设计定义 10 态，本期落地哪些态。裁决：采用核心可执行子集 DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED/COMPLETED/CANCELLED/REJECTED（舍 PRODUCED/RETURNED 两态——前者供应商确认属 Portal 协同 successor、后者退货归 successor），在计划中记录选择/替代/残留风险。
  - Skill: `none`（设计裁决，非代码方法）
- [x] **Add: 扩展 `erp-mfg/subcontract-status` 字典**（orm.xml:97-101）由 3 态 → 裁剪后状态集；保护区域 ask-first，须人工批准后再生效。
  - Skill: `nop-backend-dev`
- [x] **Add: 新增 SUBCONTRACT_* ErpFinBusinessType 枚举常量 + `erp-fin/business-type` 字典项**——至少 SUBCONTRACT_ISSUE（材料发出，借委外物资/贷原材料）、SUBCONTRACT_RECEIPT（成品入库，借产成品/贷委外物资）、SUBCONTRACT_FEE（加工费，借委外物资/贷应付）；码值续 502+。镜像 MANUFACTURING_RECEIPT(500)/ISSUE(501) 既有范式。
  - Skill: `nop-backend-dev`
- [x] **Add: COA 种子补"委外物资"中转科目**（核实 `_vfs/_init-data/erp_md_subject.csv` 后补种，使过账 Provider 科目码可达）。
  - Skill: `none`

Exit Criteria:

- [x] 字典扩展经人工批准（ask-first 证据记录于本计划）；SUBCONTRACT_* 类型枚举+字典同步存在且编译通过（fin-dao 局部 `mvn -pl module-finance/erp-fin-dao -am compile` 通过，解除后续阶段依赖）。

### Phase 2 — 委外 BizModel + Processor 生命周期编排

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../entity/ErpMfgSubcontractOrderBizModel.java`（新 Processor `ErpMfgSubcontractOrderProcessor.java`）；xbiz delta 接审批/状态动作
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（字典 + 类型就位）

- [x] **Add: ErpMfgSubcontractOrderBizModel 补 thin Facade 动作**——submit/approve/reject/cancel（审批轴复用平台 approval-support + docStatus 联动）+ `issueMaterials`/`receiveFinished`/`postProcessingFee` 三个 `@BizMutation` 业务动作，全部委托注入的 `ErpMfgSubcontractOrderProcessor`（镜像 `ErpMfgWorkOrderBizModel` Facade→Processor 两层范式）。
  - Skill: `nop-backend-dev`
- [x] **Add: ErpMfgSubcontractOrderProcessor protected step 编排**——`issueMaterials`（状态守卫 APPROVED→调 `IErpInvStockMoveBiz.generateOutgoingMove` 发料出库→置 ISSUED）、`receiveFinished`（守卫 ISSUED→调 generateIncomingMove 成品入库→置 RECEIVED）、`postProcessingFee`（守卫 RECEIVED→构造 PostingEvent→过账→posted 翻转→置 COMPLETED）；跨 BizModel 调用后 reload 实体（镜像 WorkOrderProcessor.reportCompletion 的 evict+reload）。
  - Skill: `nop-backend-dev`
- [x] **Add: ErrorCode 守门**——非法状态迁移 / 发料超 BOM 标准 / 收货超发料（扣损耗）等守卫错误码，扩 `ErpMfgErrors`（中文描述，i18n 处理翻译）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 委外单审批+三动作生命周期经单测可驱动：状态按设计子集流转，非法迁移抛 ErrorCode；发料/收货各产 1 条 inventory StockMove（OUTGOING/INCOMGING 方向正确）。

### Phase 3 — 委外 GL 过账（Dispatcher + Provider）

Status: completed
Targets: 新 `SubcontractPostingDispatcher.java` + `SubcontractIssueAcctDocProvider.java` / `SubcontractReceiptAcctDocProvider.java` / `SubcontractFeeAcctDocProvider.java`；`ErpFinAcctDocRegistry` 注册
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2（生命周期动作产出 PostingEvent）

- [x] **Add: SubcontractPostingDispatcher**——镜像 `ManufacturingIssuePostingDispatcher`：从库存移动/订单构造 PostingEvent（businessType=SUBCONTRACT_*、billHeadCode=委外单 code、orgId/currencyId/voucherDate、billData 含行级成本/科目/物料码）→ `executor.postEvent`→ 成功标 posted=true / 失败捕获保持 posted=false（DeferredPostingSweepJob 兜底）。
  - Skill: `nop-backend-dev`
- [x] **Add: 三个 AcctDocProvider**（按 SUBCONTRACT_ISSUE/RECEIPT/FEE 各一）声明 `EnumSet.of(...)` 支持类型 + 按设计 `subcontracting.md:111-113` 科目分解（ISSUE 借委外物资/贷原材料；RECEIPT 借产成品/贷委外物资；FEE 借委外物资/贷应付账款），注册 `ErpFinAcctDocRegistry`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三类过账各产 1 张凭证，凭证行科目分解符合设计（借/贷方向 + 科目码对齐 owner doc）；加工费过账后委外单 `posted=true`；失败路径 posted=false 且入 `ErpFinPostingException`。

### Phase 4 — MRP 委外释放

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../mrp/MrpReleaseService.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2（委外单可创建）

- [x] **Add: `releaseSubcontractRequest`**——镜像 releaseWorkRequest：取 SUBCONTRACT_REQUEST 计划订单 → 创建 APPROVED 委外单（回填 supplierId/productId/quantity/processingFee 来源计划订单）+ 幂等门控；`requireReleasable` 接受 SUBCONTRACT_REQUEST；config-gated `erp-mfg.subcontract-release-enabled`（默认关向后兼容）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] MRP 产出的 SUBCONTRACT_REQUEST 经释放生成 APPROVED 委外单，字段回填正确；重复释放被幂等门控拦截。

### Phase 5 — 测试 + owner-doc 对齐

Status: completed
Targets: `module-manufacturing/erp-mfg-service/src/test/...`；`docs/design/manufacturing/subcontracting.md`（实现偏离注）；`docs/design/manufacturing/state-machine.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-4

- [x] **Add: 单元/集成测试**——委外生命周期全链（创建→审批→发料→收货→加工费过账）+ MRP 释放 + 非法迁移守卫 + 过账凭证行科目分解断言（镜像 0704-1/1800-1 凭证行数值断言范式）。
  - Skill: `nop-testing`
- [x] **Proof: mfg-service 全量测试无回归**——`mvn test -pl module-manufacturing/erp-mfg-service -am` 0 failures/0 errors。
  - Skill: `nop-testing`
- [x] **Add: owner-doc 实现偏离注**——`subcontracting.md` 补注本期裁剪（状态子集、订单根编排而非四实体、config-gated）+ successor 触发条件；`state-machine.md` 补委外状态机段。
  - Skill: `none`

Exit Criteria:

- [x] 委外全链测试全绿且凭证行科目分解断言通过；owner-doc 实现偏离与 successor 记录到位。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0a7dfdbf8ffeA6RvlWdQPBf2Ta) — 全部 load-bearing 事实主张经实时仓库核实零伪（15 行 CRUD 空壳、MrpReleaseService:49 Non-Goal、subcontract-status 字典仅 3 态、ErpFinBusinessType 无 SUBCONTRACT 最高 501、subcontracting.md 10 态+四业务对象、WorkOrder/ManufacturingIssue Dispatcher+Provider+Registry 镜像范式均真）。0 Blocker / 0 Major。2 非阻塞 Minor：M1（Deferred Classification 用 `successor` 非模板受控词汇 `watch-only residual|optimization candidate|out-of-scope improvement`——但本仓 1504-1 等多计划已采用 `successor` 为事实约定，反 Slack 实质满足，保留以对齐仓内风格）；M2（"镜像 MANUFACTURING_RECEIPT/ISSUE"枚举侧真但 `erp-fin/business-type` 字典侧无制造类型先例——本期将建立字典侧先例，非纯镜像，实施者留意）。ORM ask-first 保护区域门控范例级合规；规则 4 单结果面/N=2 依赖/退出标准可测/Deferred 触发条件均满足。草案可接受执行。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（委外生命周期 + MRP 释放 + GL 过账 + 字典/类型/COA）
- [x] 相关文档对齐（`subcontracting.md`/`state-machine.md` 实现偏离 + successor 注；`competitive-comparison.md` 杠杆 F 委外声明可对齐）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-manufacturing/erp-mfg-service -am` + `mvn test -pl module-finance/erp-fin-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] ORM ask-first 字典变更经人工批准（保护区域证据记录）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符（本轮 closure audit 由独立子代理执行，见 Closure 证据）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 独立 SubcontractIssue/Receipt/Invoice 实体（设计四业务对象完整态）

- Classification: `successor`
- Why Not Blocking Closure: 本期以委外订单为编排根 + inventory StockMove 留痕交付核心价值（对齐 Odoo mrp_subcontracting 复用 stock.picking 范式）。独立单据实体为审计/Portal 增强面，非业财打通必需。
- Successor Required: `yes`（触发条件：供应商 Portal 协同 / 来料质检触发 / 损耗核算业务需求落地时——需独立单据承载明细）

### 供应商 Portal / 来料质检 / 损耗 / 退货 / 批次序列号

- Classification: `successor`
- Why Not Blocking Closure: 均 design `subcontracting.md` 完整态增强面，本期 Non-Goal 明示。
- Successor Required: `yes`（触发条件：对应业务需求落地时）

### 委外浏览器层 E2E + 前端 AMIS 动作页面

- Classification: `successor`
- Why Not Blocking Closure: 后端生命周期落地后才有 E2E 可验证对象；前端 codegen 已生成基础 CRUD 页，状态机/动作按钮接入为前端 successor。
- Successor Required: `yes`（触发条件：本计划 completed 后）

## Closure

Status Note: <completed — 全 5 阶段执行完毕，106 mfg + 185 fin 测试全绿，154 模块 mvn clean install -DskipTests 通过>

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，不重用执行者上下文）
- 实时仓库核实（语义验证通过，非盲信 `[x]`）：
  - 字典扩展：`app-erp-manufacturing.orm.xml:100-108` `erp-mfg/subcontract-status` 已含 ISSUED/RECEIVED/COMPLETED/REJECTED（裁剪子集与 Phase 1 Decision 一致）。
  - 过账类型：`ErpFinBusinessType.java:65-67` SUBCONTRACT_ISSUE(502)/RECEIPT(503)/FEE(504)；`_ErpFinDaoConstants.java:319-329` 同步常量。
  - BizModel Facade：`ErpMfgSubcontractOrderBizModel.java` 三个 `@BizMutation`（issueMaterials/receiveFinished/postProcessingFee）委托 Processor，非空壳。
  - Processor：`ErpMfgSubcontractOrderProcessor.java`（448 行）protected step 编排真实——状态守卫 `requireStatus`、`generateIssueMove`/`generateReceiptMove`、`subcontractPostingDispatcher.dispatch*Posting`、reload+状态迁移，无 `{}`/`return null` 空体。
  - Beans 注册：`app-service.beans.xml:42-51` 注册 Processor + Dispatcher + 3 AcctDocProvider 五 bean，运行时可注入可达。
  - MRP 释放：`MrpReleaseService.java:100` `releaseSubcontractRequest`，`requireReleasable`(:109/117) 接受 SUBCONTRACT_REQUEST，幂等门控就位。
  - 测试：`TestErpMfgSubcontracting.java`（402 行）三测方法 testFullLifecycleWithPosting/testIllegalTransitionsRejected/testMrpSubcontractRelease 覆盖全链+非法迁移+释放幂等。
  - Anti-Hollow：所有新代码运行时经 BizModel→Processor→Dispatcher→Provider 链可达，无未注册组件、无吞噬异常、无占位返回。
- 执行证据：TestErpMfgSubcontracting 3/3 绿（全链生命周期 + 非法迁移守卫 + MRP 释放幂等）；mvn clean install -DskipTests 154 模块 BUILD SUCCESS。

Follow-up:

- 委外费归集源就位后启动 N=2 计划 `2026-07-13-0455-2-manufacturing-cost-element-decomposition.md`（subcontract 成本列）。
- 独立单据实体 / Portal / 质检 / 损耗 / 退货 / 批次 / 委外差异 / E2E / 前端（见 Deferred But Adjudicated 各 successor 触发条件）。
