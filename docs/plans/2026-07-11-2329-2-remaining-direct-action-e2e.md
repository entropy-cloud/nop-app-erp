# 2026-07-11-2329-2 remaining-direct-action-e2e

> Plan Status: active
> Last Reviewed: 2026-07-11
> Source: Deferred items from `2026-07-10-0335-2`：① maintenance Request→Visit 副作用编排 E2E（trigger met：当需推进 maintenance Request→Visit 编排 E2E 时）② finance voucher 手工 post 业务动作（trigger met：当需验证 voucher 手工过账浏览器层可达性时）
> Related: `2026-07-10-0335-2`（Deferred source）; `2026-07-09-2004-2`（reverse 已覆盖）; `2026-07-09-0814-2`（DIRECT 业务动作 E2E 范式）
> Audit: required

## Current Baseline

- **business-action E2E 套件已覆盖 12 代表域**（`tests/e2e/business-actions/` 共 20 spec 文件，~44 tests），套件总数 184（`docs/testing/e2e-runbook.md` L5/L111 计数）。
- **maintenance Request→Visit 副作用未编排 E2E**：`mnt-request.action.spec.ts`（plan 0335-2 Phase 2）验证 Request 5 态状态机（accept/startRepair/complete/rejectRequest/cancel），但**显式 Non-Goal 不编排 Visit**（spec L19-22）。`accept` 经 `generateResponsiveVisit`（`ErpMntRequestBizModel.java:104-113`）创建 DRAFT Visit（code=`VST-REQ-{requestId}`, equipmentId, visitDate=today, visitType=RESPONSIVE, status=DRAFT, assignedTo）。现有 spec 清理 visit（按 code 删除防看板基线污染）但不断言其字段。
- **finance voucher `post` 未覆盖浏览器层 E2E**：`IErpFinVoucherBiz.post(PostingEvent event, IServiceContext)` （`IErpFinVoucherBiz.java:31-32`）是业财过账工厂+入口。`reverse` 已在 `2004-2` orchestration 覆盖（p2p-reverse/o2c-reverse spec）。`post` 入参复杂（`PostingEvent` 含 businessType enum + billData Map + acctSchemaId + currencyId 等），0335-2 显式标 Deferred「入参复杂、边际收益递减」。
- **E2E helper 原语**：`_helper.ts` 已有三原语 `createViaSave`/`callMutation`/`callMutationOk`/`verifyState` + `deleteById`/`deleteByFilter`。`orchestration/_helper.ts` 有 `findVoucherIdByBillCode`/`assertVoucherLines`（plan 0704-1）。
- **PostingEvent 结构**（`module-finance/erp-fin-dao/.../PostingEvent.java`）：businessType(ErpFinBusinessType), billHeadCode(String), traceId, tenantId, acctSchemaId, orgId, currencyId, exchangeRate(BigDecimal), voucherDate(Date), billData(Map)。经 GraphQL 暴露为 input object，枚举值以 String scalar 传递（对齐 2004-2 `reverse` 中 businessType 裁决）。
- **种子基线**：ErpMdSubject 科目表完备（1401/1403/1131/2202/2221/6401/6711 等，经多计划补齐）。acctSchemaId=1 为默认账套。

## Goals

- maintenance Request `accept` → 验证响应式 Visit 创建（字段断言：code/equipmentId/visitDate/visitType/status/assignedTo），关闭 0335-2 Deferred「maintenance Request→Visit 编排」。
- finance voucher `post` 浏览器层 E2E 可达性验证（构造 PostingEvent → `ErpFinVoucher__post` → 断言凭证创建 + 行明细），关闭 0335-2 Deferred「finance voucher 手工 post」。

## Non-Goals

- `ErpFinVoucher__reverse` 覆盖——已在 `2004-2` p2p-reverse/o2c-reverse spec 覆盖。
- maintenance Visit 状态机 E2E——已在 `maintenance-visit.action.spec.ts`（plan 2004-1）覆盖。
- useWorkflow (xwf) 审批轴浏览器层 E2E——NOT FEASIBLE（`2330-1` 权威裁决，sysUser(0) 根因）。
- 全 18 域全业务动作覆盖——per-domain push，本期仅覆盖 0335-2 两项显式 Deferred。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/e2e-runbook.md`（增 DIRECT 业务动作子段）; `docs/design/maintenance/state-machine.md`（Request→Visit 偏离补注已在场）
- Skill Selection Basis: `nop-testing`（浏览器层 E2E、GraphQL mutation 断言范式）; 不涉及后端代码变更（`nop-backend-dev` 不匹配）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. E2e 套件经 `npx playwright test` 运行，webServer 已配置（`playwright.config.ts`）。

## Execution Plan

### Phase 1 - maintenance Request→Visit 副作用编排 E2E

Status: planned
Targets: `tests/e2e/business-actions/mnt-request-visit-orchestration.action.spec.ts`; `tests/e2e/business-actions/_helper.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: none

- [ ] Add: `tests/e2e/business-actions/_helper.ts` 新增 `findVisitByCode(code: string)` 原语——经 GraphQL `ErpMntVisit__findPage` filter `code eq '{exactCode}'` 返回首条 Visit 实体（Visit code 为精确值 `VST-REQ-{requestId}`，无后缀，用 eqFilter 非 like）
  - Skill: `nop-testing`
- [ ] Add: `mnt-request-visit-orchestration.action.spec.ts`——新建 Request（OPEN）→ `accept`（ACCEPTED）→ `findVisitByCode('VST-REQ-{requestId}')` → 断言 Visit 字段：code 精确匹配 / equipmentId == request.equipmentId / visitDate == today / visitType == RESPONSIVE / status == DRAFT / assignedTo 匹配
  - Skill: `nop-testing`
- [ ] Add: 清理——accept 路径完成后删除生成的 Visit（按 code，对齐既有 mnt-request spec 清理范式防看板基线污染）+ 删除 Request
  - Skill: `nop-testing`
- [ ] Proof: `npx playwright test tests/e2e/business-actions/mnt-request-visit-orchestration.action.spec.ts --workers=1` → 新增 test 全绿 + 无回归
  - Skill: none

Exit Criteria:

> Phase 1 交付 Request→Visit 副作用编排 E2E。

- [ ] Request accept 后 Visit 字段断言全绿（code/equipmentId/visitDate/visitType/status/assignedTo 6 字段）
- [ ] 清理完整（无看板基线污染）

### Phase 2 - finance voucher 手工 post E2E

Status: planned
Targets: `tests/e2e/business-actions/finance-voucher-post.action.spec.ts`; `tests/e2e/orchestration/_helper.ts`
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof | Explore`
- Prereqs: none

- [ ] Explore: 读 Provider 代码确定 `ErpFinVoucher__post` 浏览器层可达性 + 最小 billData 构造。读 `ErpFinPostingProcessor`（post 入口编排）+ 至少一个 AcctDocProvider（如 `LandedCostAcctDocProvider` / `LogisticsFreightProvider`）确定 billData 键值结构。确认：(a) PostingEvent 能否经 GraphQL input object 序列化（枚举/Map/BigDecimal）; (b) 哪个 businessType 的 Provider 接受最简 billData（无库存/订单状态依赖）; (c) `post` 返回 Long scalar 是否被 `_helper.ts callMutation` 正确处理（现有 helper 默认包装 selection set，scalar return 需特殊处理）。
  - 若探针发现 post 不可浏览器层可达（如 PostingEvent 无法序列化、Provider 强依赖运行时上下文），将 Phase 2 voucher post 项移入 Deferred But Adjudicated 并记录不可行裁决（对齐 `2330-1` xwf 不可行裁决范式）
  - Skill: `nop-testing`
- [ ] Decision: PostingEvent 构造方案——基于 Explore 结果选定 businessType + billData 结构。
  - 候选优先级：① `LANDED_COST(490)`（`LandedCostAcctDocProvider` billData=ALLOCATIONS+COST_ELEMENTS，结构最规则、最近实现、文档最全）; ② `FREIGHT(310)`（`LogisticsFreightProvider`，需 SALES_DELIVERY 上下文）; ③ 经 `SALES_OUTPUT`/`PURCHASE_INPUT`（InvAcctDocProvider，需库存 avgCost 快照——复杂度最高）
  - billHeadCode 用唯一 `E2E-VCH-POST-{tag}-{ts}`
  - Explore 必须在此 Decision 解决之前完成
  - Skill: `nop-testing`
- [ ] Add: `finance-voucher-post.action.spec.ts`——构造 PostingEvent input → `ErpFinVoucher__post` mutation（注意 Long scalar return 处理）→ 断言返回 voucherId 非 null → `ErpFinVoucher__get` 查凭证头（billHeadCode 匹配、voucherType/businessType 匹配）→ 查凭证行（subjectCode/amount 方向正确）
  - 幂等路径：重复 post 同 billHeadCode → 返回 null（幂等命中）
  - 注意：ErpFinVoucher 是过账产物本身，`posted` 字段是源单据标记不是凭证标记——断言凭证头字段（billHeadCode/voucherNo/voucherType）+ 行明细，不断言 `posted=true`
  - Skill: `nop-testing`
- [ ] Add: 清理——按 billHeadCode 删除凭证 + 凭证行（复用 `cleanupVoucherByBillCode` 或等价清理原语）
  - Skill: `nop-testing`
- [ ] Proof: `npx playwright test tests/e2e/business-actions/finance-voucher-post.action.spec.ts --workers=1` → 新增 test 全绿
  - Skill: none

Exit Criteria:

> Phase 2 交付 voucher post 浏览器层 E2E 覆盖。

- [ ] post 正路径：凭证创建 + 头行字段断言全绿
- [ ] post 幂等路径：重复 post 返回 null
- [ ] 清理完整（无凭证残留）

### Phase 3 - e2e-runbook 更新 + 日志

Status: planned
Targets: `docs/testing/e2e-runbook.md`; `docs/logs/2026/07-11.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1, Phase 2

- [ ] Add: `docs/testing/e2e-runbook.md` 增「maintenance Request→Visit 副作用编排」子段 + 「finance voucher 手工 post」子段；套件计数更新（+2 spec）
  - Skill: none
- [ ] Add: `docs/logs/2026/07-11.md` 追加聚合日志条目（验证状态、套件计数变化）
  - Skill: none

Exit Criteria:

> Phase 3 交付文档对齐。

- [ ] e2e-runbook 两新子段在场 + 套件计数正确
- [ ] 日志条目含验证状态

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_0ae28270effeu9S54bnx30Y0ZY) — B1: Phase 2 Decision 未解决（"经运行时探针裁决"是执行期延迟非决策），需前置 Explore 项读 Provider 代码确定 billData 可构造性 + post 浏览器层可达性，Decision 须基于 Explore 结果解决; SALES_OUTPUT billData 是 InvAcctDocProvider 经库存状态计算非手工可构造。S1: 套件计数 180 应为 184; S2: findVisitByCodePattern 应简化为精确 eq 匹配（code 无后缀）; S3: posted=true 断言有误——ErpFinVoucher 是过账产物本身，posted 是源单据标记。
- Independent draft review iteration 2: accept (ses_0ae1de922ffeyYn2u5wrO232KG) — B1/S1/S2/S3 全部修订正确，Explore→Decision 排序显式，回退路径在场。无新阻塞项。**草案审查收敛，状态 draft→active。**

## Closure Gates

> 纯测试+文档计划，无生产代码/契约/ORM 模型变更。验证命令为 Playwright E2E + Maven 构建。

- [ ] 范围内行为完成（两 spec 全绿）
- [ ] 相关文档对齐（e2e-runbook + 日志）
- [ ] 已运行验证：`npx playwright test tests/e2e/business-actions/mnt-request-visit-orchestration.action.spec.ts tests/e2e/business-actions/finance-voucher-post.action.spec.ts --workers=1` + `mvn clean install -DskipTests`（回归）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finance voucher post 复杂 billData 多 businessType 覆盖矩阵

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期验证一条 businessType 路径（最小 PostingEvent 构造）。全 businessType × billData 组合覆盖属回归矩阵扩展。
- Successor Required: no

## Closure

Status Note: pending implementation

Closure Audit Evidence:

- (pending)

Follow-up:

- (none — 两项 Deferred 均为本计划范围内关闭目标)
