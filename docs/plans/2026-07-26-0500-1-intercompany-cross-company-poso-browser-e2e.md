# 2026-07-26-0500-1 跨公司 Intercompany PO/SO 配对凭证浏览器层 E2E

> Plan Status: completed
> Mission: erp
> Work Item: A3 Intercompany 跨公司 PO/SO 浏览器层端到端验证
> Last Reviewed: 2026-07-26
> Source: 近期深化后端特性浏览器层验证缺口 —— A3 多公司运营深度（plan `2026-07-22-1000-1`）+ 跨公司 PO/SO intercompany 生命周期（plan `2026-07-24-1351-2`）+ GL Mapping orgId 维度（plan `2026-07-25-1016-2`）三者经 JUnit 覆盖（`TestErpFinIntercompanyTransfer` 6 场景 + finance-service 279 测试 + purchase/sales service 全绿），但**零浏览器层 E2E**。AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点。
> Related: `docs/plans/2026-07-22-1000-1-finance-multi-company-operational-depth.md`（A3 基础设施）、`docs/plans/2026-07-24-1351-2-intercompany-cross-company-po-so-lifecycle.md`（PO/SO intercompany 生命周期）、`docs/plans/2026-07-26-0410-2-commitment-accounting-browser-e2e.md`（同型 config-gated 跨切面过账 hook 浏览器层 E2E 先例）
> Audit: required

## Current Baseline

跨公司 intercompany 配对凭证范式已落地（A3 + 1351-2）：跨法人 PO/SO approve → `IErpFinIntercompanyTransferBiz.onTradeDocumentApproved(docType, docId, docCode, executingOrgId, amount, businessDate, context)` → `IntercompanyVoucherGenerator.generatePairedVouchers` 生成 2 配对凭证（AR 侧 INTERCOMPANY_SALE + AP 侧 INTERCOMPANY_PURCHASE），各 2 行 Dr/Cr。reverseApprove/cancel → `onTradeDocumentReversed` → `reverseIntercompany` 红冲（借贷互换 + isReversed + reversalOfVoucherId 回链，镜像 `CommitmentVoucherGenerator.reverseCommitment` 范式）。

**三接入点**（config-gated `erp-fin.intercompany-posting-enabled` 默认 false）：
- 采购 `ErpPurOrderProcessor.approve/reverseApprove/cancel` 三处接 `runIntercompanyApproveHook`/`runIntercompanyReverseHook`（非阻塞 try-catch）
- 销售 `ErpSalOrderProcessor.approve/reverseApprove/cancel` 三处接同型 hook
- 既有 inventory transfer confirm（A3 基础设施，不在本计划范围）

**跨法人判定**（1351-2 Decision B）：执行方 = `resolveLegalEntityRoot(order.orgId)` 沿 `ErpMdOrganization.parentId` 链向上找首个 `orgType=COMPANY`（带环检测）；对手方 = 转移定价规则表反向查找（PO 查 toOrgId、SO 查 fromOrgId）。同法人 skip（零凭证）。AR/AP 方向（Decision C）：PO 执行方=买方、SO 执行方=卖方。

**科目解析**：4 INTERCOMPANY_* accountKey（INTERCOMPANY_AR/INTERCOMPANY_REVENUE/INTERCOMPANY_COST/INTERCOMPANY_AP）经 A1 GlMappingResolver 解析（fallback 硬编码 1131/5001/1401/2202）。billType = `INTERCOMPANY_SALE` / `INTERCOMPANY_PURCHASE`。

既有浏览器层凭证行断言范式（`tests/e2e/orchestration/_helper.ts`）：`findVoucherIdByBillCode(page, billCode, postingType?)` + `assertVoucherLines(page, voucherId, expected[])`。既有 `runP2pChain`/`runO2cChain` 编排原语固定使用 `SEED.ORG=2`。

**关键约束（与 0410-2 同型）**：intercompany 需要跨法人（两个不同 COMPANY 根的组织）。`runP2pChain`/`runO2cChain` 固定 `SEED.ORG=2`（ERP-CO），其法人根即自身。要验证跨法人配对凭证生成，须自包含 setup 建**跨法人组织对** + 转移定价规则 + 订单（PO/SO），不能复用固定 orgId=2 的既有链路原语（同法人 skip → 零凭证）。

剩余差距：跨公司 PO/SO intercompany 配对凭证 + 红冲闭环经 JUnit 单层验证，但**全栈浏览器层路径未验证**——订单经 GraphQL `__save` + approve → intercompany hook 触发 → 配对凭证生成可观测。转移定价规则表（C3 日期范围有效性 MUTEX 实体，`ErpFinIntercompanyTransferPrice`）+ GL Mapping 4 accountKey 路由同样零浏览器层覆盖。

## Goals

- 验证跨法人 PO approve → INTERCOMPANY 配对凭证生成（AR 侧 + AP 侧各 2 行 Dr/Cr 精确数值断言）
- 验证跨法人 SO approve → INTERCOMPANY 配对凭证生成（AR/AP 方向对称性，Decision C）
- 验证 reverseApprove → 配对凭证红冲（isReversed + reversalOfVoucherId 回链 + 借贷互换）
- 验证同法人 PO/SO approve → 零配对凭证（控制对照）
- 验证转移定价规则使对手方发现可达（fromOrgId/toOrgId + isActive 过滤），配对凭证金额 = order.totalAmountWithTax（Decision C：订单已含明确交易价，不需 resolver 计算金额；定价规则仅用于对手方发现）

## Non-Goals

- inventory transfer confirm 触发的 intercompany —— A3 基础设施（1000-1），属不同触发路径
- 发票级 intercompany —— 1351-2 Deferred（触发：跨法人开票业务需求）
- Receive/Delivery 联级 intercompany —— 1351-2 Deferred（触发：货物实际跨法人移动需独立凭证）
- 合并抵消候选识别 —— A3 已落地（`ErpFinConsolidationElimination`），经 JUnit 覆盖，浏览器层归 successor
- 实时合并报表渲染 —— A3 Deferred（触发：业务客户合并报表需求）
- MARKET 策略真实市场价接入 —— A3 Deferred（触发：市场价数据源集成）
- 生产 Java/ORM/契约/codegen/字典/种子变更 —— 纯测试 + 文档

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/architecture/multi-company.md`（§跨公司 PO/SO 触发路径 + §Phase 1 决策记录）、`docs/design/finance/posting.md`（§跨法人内部交易凭证 + §PO/SO 触发路径扩展）、`docs/design/finance/gl-mapping-rules.md`（§intercompany 维度接入 A3）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E + 既有 orchestration/_helper 复用 + config-gated 特性 webServer JVM arg 启用范式，对齐 0410-2 同型先例）

## Infrastructure And Config Prereqs

- webServer JVM args（`playwright.config.ts` webServer.command）追加 `-Derp-fin.intercompany-posting-enabled=true`（config-gated 默认 false）。
- No infra prereqs beyond existing baseline（fresh-DB H2 + 既有 webServer 启动链）。

## Execution Plan

### Phase 1 - Explore（跨法人组织对 + 转移定价规则 + 订单 setup 可达性核实）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_md_organization.csv`（组织种子）、`module-finance/erp-fin-service/.../intercompany/ErpFinIntercompanyTransferBizModel.java`、`IntercompanyVoucherGenerator.java`、`ErpFinTransferPriceResolver.java`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: 无

- [x] Proof: 核实种子 `erp_md_organization.csv` 含至少 2 个 `orgType=COMPANY` 的组织（法人根），以及一个子组织挂在不同法人根下（使 `resolveLegalEntityRoot` 产出不同法人）。若无合适数据，裁决自包含 setup 经 `ErpMdOrganization__save` 建测试专用跨法人组织对
- [x] Proof: 核实 `ErpFinIntercompanyTransferPrice` 实体经 GraphQL `__save` 创建所需最小必填字段集（fromOrgId/toOrgId/materialId + pricingMethod + markupRate + validFrom/validTo + `isActive=true`）。**注意**：trade-document 路径（`onTradeDocumentApproved`）中 `resolveCounterpartyLegalEntity` 仅按 `fromOrgId`/`toOrgId`/`isActive=true` 过滤用于对手方发现（`ErpFinIntercompanyTransferBizModel:174-192`），**不读 pricingMethod/markupRate/materialId**；定价金额计算属 inventory transfer 路径（`onTransferConfirmed`，Non-Goal）。实体必填字段仍须满足 NOT NULL 约束
- [x] Proof: 核实配对凭证行结构（`generatePairedVouchers` AR 侧 Dr INTERCOMPANY_AR/Cr INTERCOMPANY_REVENUE + AP 侧 Dr INTERCOMPANY_COST/Cr INTERCOMPANY_AP），确定断言期望值表（金额 = order.totalAmountWithTax，Decision C：`onTradeDocumentApproved` 将入参 amount 直接传入 generator，不经转移定价 resolver 计算金额）
- [x] Proof: 核实红冲凭证行结构（`reverseIntercompany` 借贷互换 + dcDirection 不变 + amountSource/Functional 正数），对齐 0410-2 commitment 红冲范式
- [x] Proof: 核实 config 启用后既有 `runP2pChain`/`runO2cChain` 链路不受影响（intercompany hook 非阻塞 try-catch + SEED.ORG=2 同法人 skip 零凭证）

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] Explore 笔记记录跨法人组织对 setup + 转移定价字段集 + 凭证行期望值表 + 全链路回归影响（写入 plan Execution Decision 段，不新建独立文档）

### Phase 2 - spec 实现（跨法人 PO/SO 配对凭证 + 红冲 + 同法人控制对照）

Status: completed
Targets: `tests/e2e/business-actions/fin-intercompany-cross-company.action.spec.ts`（NEW）+ `tests/e2e/orchestration/_helper.ts`（新增按 `ErpFinVoucherBillR.billType` 反查 intercompany 配对凭证的原语）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 新建 `fin-intercompany-cross-company.action.spec.ts`，**自包含跨法人 setup**（经 GraphQL 建跨法人组织对 + 转移定价规则 + 测试专用物料 + PO/SO 订单 `__save` + `submitForApproval`/`approve`），不复用固定 orgId=2 的 `runP2pChain`/`runO2cChain`（同法人 skip 无配对凭证）。helper 新增按 `ErpFinVoucherBillR.billType`（INTERCOMPANY_SALE/INTERCOMPANY_PURCHASE）反查配对凭证的原语（intercompany 凭证无独立 postingType，且同 orderCode 可能含 COMMITMENT 凭证须按 billType 区分）
      - Skill: `nop-testing`
- [x] Proof: (1) **跨法人 PO approve 配对凭证** —— PO approve → 按 orderCode 反查 INTERCOMPANY_SALE + INTERCOMPANY_PURCHASE 两张凭证 + `assertVoucherLines` 逐行断言（Dr/Cr 科目 + 金额 = PO.totalAmountWithTax，Decision C）；(2) **跨法人 SO approve 配对凭证** —— SO approve → 同型反查 + AR/AP 方向对称性断言（Decision C：SO 执行方=卖方）；(3) **reverseApprove 红冲** —— PO/SO reverseApprove → 原配对凭证 isReversed=true + 红冲凭证行借贷互换（按 Phase 1 期望值表）；(4) **同法人控制对照** —— 同法人组织 PO/SO approve → 显式断言无 INTERCOMPANY 凭证（经 `ErpFinVoucherBillR` 按 billCode + billType 反查返回空）
      - Skill: `nop-testing`
- [x] Add: cleanup 清理测试专用组织对 + 转移定价规则 + 物料 + 订单 + 配对凭证（经既有 `cleanupVoucherByBillCode` + 实体 `__delete`）
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] `fin-intercompany-cross-company.action.spec.ts` 全绿，断言跨法人 PO/SO 配对凭证 + 红冲 + 同法人控制对照四组可观察结果

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/architecture/multi-company.md`（§跨公司 PO/SO 触发路径补浏览器层覆盖注记）、`docs/testing/e2e-runbook.md`（业务动作表 + intercompany 行 + webServer JVM arg 段）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `multi-company.md` §跨公司 PO/SO 触发路径 增「浏览器层验证」实现注记（自包含跨法人 setup 范式 + 配对凭证反查 + 同法人控制对照 + config-gated 启用）
- [x] Add: `e2e-runbook.md` 业务动作表 +finance intercompany 跨公司 PO/SO 行 + webServer JVM arg 段补 `intercompany-posting-enabled`

Exit Criteria:

- [x] owner doc + runbook 更新落地（仅此阶段实际更改 owner 行为文档）

## Execution Decision（Phase 1 Explore 笔记）

经实时仓库核实（2026-07-26）：

1. **种子组织数据核实**（`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_organization.csv`）：种子仅含 2 行——org 1 `GROUP-HQ`（orgType=GROUP，无 parentId）+ org 2 `ERP-CO`（orgType=COMPANY，无 parentId）。**仅 1 个 COMPANY 法人根，无挂在不同法人根下的子组织**。裁决：自包含 setup 经 `ErpMdOrganization__save` 建测试专用跨法人组织对（无先例，generic `createViaSave` 对 registerShortName=true 实体可用）。Setup 设计（4 组织 + 1 规则）：
   - `buyerCo`（orgType=COMPANY，无 parentId）——买方法人根
   - `sellerCo`（orgType=COMPANY，无 parentId）——卖方法人根
   - `buyerDiv`（orgType=DEPARTMENT，parentId=buyerCo）——买方执行组织（PO.orgId 挂此，使 `resolveLegalEntityRoot` 沿 parentId 链向上走到 buyerCo，浏览器层验证 walk-up 路径）
   - `sellerDiv`（orgType=DEPARTMENT，parentId=sellerCo）——卖方执行组织（SO.orgId 挂此）
   - `ErpFinIntercompanyTransferPrice` 规则：fromOrgId=sellerCo，toOrgId=buyerCo，isActive=true（对手方发现键）
   - org-type 字典（`erp-md/org-type.dict.yaml`）含 GROUP/COMPANY/BRANCH/DEPARTMENT/WORKSHOP/STORE；DEPARTMENT 非法人根（resolveLegalEntityRoot walk-up 越过）
2. **转移定价规则字段集核实**（权威 `module-finance/model/app-erp-finance.orm.xml:2076-2122`）：mandatory = code/name/orgId/pricingMethod/isActive；fromOrgId/toOrgId/materialId/materialCategoryId/validFrom/validTo 均 nullable（"空=通配"）。trade-document 路径 `ErpFinIntercompanyTransferBizModel.resolveCounterpartyLegalEntity:174-192` 仅按 `fromOrgId`/`toOrgId`/`isActive=true` 过滤用于对手方发现，**不读 pricingMethod/markupRate/materialId/validFrom/validTo**（Decision C）。setup 设 pricingMethod=COST_PLUS + markupRate=0.1 + validFrom/validTo 覆盖 BDATE（满足 NOT NULL + C3 日期范围 MUTEX 约束，虽 trade-document 路径不读）。`orgId`（核算组织）mandatory，设 buyerCo（任意有效 org，trade-document 路径不读）。
3. **配对凭证行结构核实**（权威 `IntercompanyVoucherGenerator.generatePairedVouchers:69-112` + `writeIntercompanyVoucher:283-355`）：amount = onTradeDocumentApproved 入参 amount（= order.totalAmountWithTax，**直接传入 generator 不经转移定价 resolver 计算**，Decision C 已核实 `ErpFinIntercompanyTransferBizModel:149-150`）。科目经 A1 GlMappingResolver 解析，**fresh-DB 无 `erp_fin_gl_mapping_rule.csv` 种子** → resolver 返回 null → 回落硬编码默认（`IntercompanyVoucherGenerator:46-49`）：
   - **AR 侧凭证**（INTERCOMPANY_SALE，orgId=sellerLegal，voucherType=TRANSFER）：2 行
     - 行1 Dr `1131`（应收账款，ASSET/DEBIT 种子 id=11）dcDirection=DEBIT，debit=amount，credit=0
     - 行2 Cr `5001`（主营业务收入，INCOME/CREDIT 种子 id=6）dcDirection=CREDIT，debit=0，credit=amount
   - **AP 侧凭证**（INTERCOMPANY_PURCHASE，orgId=buyerLegal，voucherType=TRANSFER）：2 行
     - 行1 Dr `1401`（原材料，ASSET/DEBIT 种子 id=9）dcDirection=DEBIT，debit=amount，credit=0
     - 行2 Cr `2202`（应付账款，LIABILITY/CREDIT 种子 id=5）dcDirection=CREDIT，debit=0，credit=amount
   - 4 fallback 科目码（1131/5001/1401/2202）均在 `erp_md_subject.csv` 种子中可达（`findSubjectByCode` 非 null → subjectId/subjectCode/subjectName 完整填充）
   - 业财回链：`ErpFinVoucherBillR`（billType=INTERCOMPANY_SALE/PURCHASE，billCode=orderCode，businessType 同 billType）；voucher.code 前缀 `INTERCOMPANY-`+uuid12；voucher.postingType **未设**（null，区别于 COMMITMENT/BUDGET 影子凭证，故反查须按 billType 非 postingType）
4. **红冲凭证行结构核实**（权威 `IntercompanyVoucherGenerator.writeIntercompanyReversalFromLines:180-251`）：对原凭证逐行复制并翻转金额（对齐 `CommitmentVoucherGenerator.writeReversalFromLines` 范式，plan 2026-07-24-1351-2）：
   - `dcDirection` **不变**（`:229` `line.setDcDirection(ol.getDcDirection())`）
   - `debitAmount ↔ creditAmount` **互换**（`:230-231` `setDebitAmount(origCredit)` / `setCreditAmount(origDebit)`）
   - `amountSource` / `amountFunctional` = `origDebit + origCredit`（**正数和**，`:234-235`）
   - `subjectId/subjectCode/subjectName/businessType` 复制（`:226-228,238`）
   - 红冲凭证头：`isReversed=true` + `reversalOfVoucherId=originalId`（`:212-213`）+ voucher.code 前缀 `INTERCOMPANY-REVERSAL-`+uuid12；原凭证头同步置 `isReversed=true`（`reverseIntercompany:133`，经 updateEntity）
   - 红冲凭证业财回链：billType=原 INTERCOMPANY_SALE/PURCHASE，billCode=`findOriginalIntercompanyBillCode(originalId)`=orderCode（`:243-248`，红冲与原凭证共用 billCode+billType）
   - **红冲行期望值表**（amount=order.totalAmountWithTax，4 fallback 科目）：
     - 红冲 AR：行1 1131 dcDirection=DEBIT，debit=0，credit=amount（互换自原 debit=amount）；行2 5001 dcDirection=CREDIT，debit=amount，credit=0
     - 红冲 AP：行1 1401 dcDirection=DEBIT，debit=0，credit=amount；行2 2202 dcDirection=CREDIT，debit=amount，credit=0
   - **原/红冲凭证区分**：均 isReversed=true（红冲后原凭证被置 true），唯一区分项 `reversalOfVoucherId`（原=null，红冲=originalId 非空）—— helper 按此裁定（镜像 `findCommitmentVoucherIdByCode` 范式）
5. **全链路回归影响核实**（config 启用后既有 `runP2pChain`/`runO2cChain` 链路）：链路固定 `SEED.ORG=2`（ERP-CO，COMPANY 法人根）。`resolveCounterpartyLegalEntity(executingLegal=2, docType, businessDate)` 按 fromOrgId/toOrgId=2 + isActive=true 查 `ErpFinIntercompanyTransferPrice`——fresh-DB 无该表种子（`_init-data/` 下无 `erp_fin_intercompany_transfer_price.csv`）→ 查询空 → 返回 null → `onTradeDocumentApproved` LOG.debug "no counterparty pricing rule" → return emptyList（零凭证）。**config 启用对既有链路零影响**（与承付不同：intercompany hook 两域均为非阻塞 try-catch，无 release-hook 容错不对称问题；同法人 skip 路径在既有链路下未触发，因 counterparty=null 提前返回）。既有 e2e 套件无任何 spec 建转移定价规则（grep 全无 `ErpFinIntercompanyTransferPrice` 命中），故 intercompany 在既有套件下全程静默。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_06430268fffe8IpDWFAR89JWFm`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 1 Major / 4 Minor。全部非 Major 负载事实经实时仓库逐项核实**精确匹配**（config-gate / SPI 方法签名 / billType / 零浏览器 E2E / helper 函数 / runP2pChain 固定 orgId / 凭证行结构 / 红冲结构 / Processor hooks 非阻塞 try-catch / webServer JVM args 未含 intercompany ✓）。**Major M1**：Goal #5 + Phase 1 期望值表 + Phase 2 断言声称「COST_PLUS 转移定价驱动配对凭证金额」事实错误——`onTradeDocumentApproved:106-151` 将入参 amount（=order.totalAmountWithTax）直接传入 generator，**不调 transferPriceResolver**；owner doc `multi-company.md` Decision C 明示「订单金额作为配对凭证金额，不需 resolver 计算金额，定价规则仅用于对手方发现」。**已修订**：Goal #5 改为验证对手方发现 + 凭证金额=order.totalAmountWithTax（Decision C）；Phase 1 期望值表 + Phase 2 断言金额改为 order.totalAmountWithTax；Phase 1 转移定价字段集补 isActive + 注明 trade-document 路径仅读 fromOrgId/toOrgId/isActive。**Minor**：(1) 缺 Draft Review Record 段——本次新增 ✓；(2) isActive 字段遗漏——Phase 1 字段集已补 ✓；(3) helper 设计须按 billType 非 postingType 反查（intercompany 无独立 postingType + 同 orderCode 含 COMMITMENT 凭证）——Phase 2 Targets + Add item 已明确 ✓；(4) `ErpMdOrganization__save` 建跨法人组织对无先例——Phase 1 Proof 已含可达性验证 ✓。
- Independent draft review iteration 2: `acceptable as-is`（`ses_0642c3b92ffeqXQmYaeJNjxukC`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-26）— 0 Blocker / 0 Major。M1 修订经实时仓库逐项核实**精确匹配**：Goal #5 + Phase 1 字段集/期望值表 + Phase 2 断言均改为 order.totalAmountWithTax（Decision C）+ 转移定价仅对手方发现；`ErpFinIntercompanyTransferBizModel:149-150` 确认 amount 直传 generator 不调 resolver + `:174-192` 确认 resolveCounterpartyLegalEntity 仅按 fromOrgId/toOrgId/isActive 过滤 + `multi-company.md:142` Decision C 逐字匹配。4 Minors 全部 addressed。无新 anti-slack/format 问题。计划为可接受的执行契约。

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + 受影响 Playwright 套件一次。

- [x] 范围内行为完成（跨法人 PO/SO 配对凭证 + 红冲 + 同法人控制对照四组断言全绿）
- [x] 相关文档对齐（multi-company.md + posting.md + e2e-runbook）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（2:51 min）+ `npx playwright test tests/e2e/business-actions/fin-intercompany-cross-company.action.spec.ts` 全绿（4 passed 30.0s）+ business-actions/orchestration 既有 spec 回归 0 新增失败（26 passed 3.3m，含 fin-commitment-accounting + fin-gl-mapping-routing + p2p-reverse-approve + 全 orchestration P2P/O2C/mfg chain/subcontract/variance/genealogy/inspection-gate））
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 发票级 intercompany 凭证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1351-2 Deferred —— 发票级（实际开票跨法人）语义更复杂（含税/结算），属不同结果面
- Successor Required: `yes`（触发条件：跨法人开票业务需求 + finance owner doc 授权）

### Receive/Delivery 联级 intercompany

- Classification: `optimization candidate`
- Why Not Blocking Closure: 1351-2 Decision 裁定 —— 订单级已足够表达跨法人交易，联级为增强
- Successor Required: `yes`（触发条件：货物实际跨法人移动需独立凭证的业务需求）

## Closure

Status Note: 跨公司 PO/SO Intercompany 配对凭证 + 红冲 + 同法人控制对照四组浏览器层 E2E 全绿（独立结束审计复跑 4 passed 29.8s）+ 后端代码逐行核实（IntercompanyVoucherGenerator 回退科目 1131/5001/1401/2202 + Decision C amount 直传 generator + resolveCounterpartyLegalEntity 仅按 fromOrgId/toOrgId/isActive 过滤 + 红冲 dcDirection 不变/debit-credit 互换 + Processor 钩子非阻塞 try-catch）+ owner docs（multi-company.md/e2e-runbook.md/posting.md）回链落地 + roadmap（README.md/deepening-roadmap.md）✅ done + 2 Deferred 项（发票级 + Receive/Delivery 联级）含后继触发条件非阻塞。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_063857582ffe2k6VH2O7tCX0Ui`，无执行者上下文冷重播，2026-07-26）
- Verdict: `pass`（0 Blocker / 0 Major / 0 Minor）
- Evidence:
  - 反空壳核实：`fin-intercompany-cross-company.action.spec.ts`（19,213 字节）4 个 `test(...)` 实质用例（L203 跨法人 PO 配对凭证 / L235 跨法人 SO 配对凭证 / L267 reverseApprove 红冲 / L312 同法人控制对照），均含 `assertVoucherLines`/`expect` 断言非桩；`_helper.ts:198-223` `findIntercompanyVoucherIdByBillCode` 按 `ErpFinVoucherBillR.billType` + `reversalOfVoucherId != null` 反查（L205-220）；`playwright.config.ts:18` webServer JVM arg `-Derp-fin.intercompany-posting-enabled=true` 逐字匹配。
  - 后端代码逐行核实：`IntercompanyVoucherGenerator.java:46-49` 回退 1131/5001/1401/2202 + `:69-112` AR SALE（Dr 1131/Cr 5001）+ AP PURCHASE（Dr 1401/Cr 2202）；`:180-251` 红冲 `dcDirection` 不变（`:229`）+ debit↔credit 互换（`:230-231`）+ isReversed/reversalOfVoucherId 回链（`:212-213`）；`ErpFinIntercompanyTransferBizModel.java:149-150` amount 直传 generator（Decision C）+ `:174-192` resolveCounterpartyLegalEntity 仅按 toOrgId/fromOrgId + isActive 过滤；`ErpPurOrderProcessor.java:267-295` + `ErpSalOrderProcessor.java:304-327` 钩子非阻塞 try-catch。
  - owner docs 回链：`multi-company.md:131` §跨公司 PO/SO 触发路径「浏览器层验证」注记 + 引本 plan；`e2e-runbook.md:57/121/325` webServer JVM arg + 套件计数 91→92 + 业务动作表 intercompany 行；`posting.md:555-595` 跨法人内部交易凭证段。
  - roadmap ✅ done：`backlog/README.md:120` + `backlog/deepening-roadmap.md:460`。
  - 验证复跑：`npx playwright test tests/e2e/business-actions/fin-intercompany-cross-company.action.spec.ts` → 4 passed 29.8s（独立复跑，匹配计划声称 30.0s）；4 用例名 ↔ 4 Goals 1:1 映射。
  - Deferred 项：发票级 intercompany（L174-178）+ Receive/Delivery 联级（L180-184）均含 successor 触发条件非阻塞。
  - 一致性：Phase 1/2/3 全 `[x]` + `Status: completed`；Goals/Non-Goals 匹配实建；Draft Review Record 2 轮（iteration 1 Major M1 已修订）齐备。

Follow-up:

- 发票级 intercompany + Receive/Delivery 联级（触发条件见上 Deferred But Adjudicated 段，非阻塞）
