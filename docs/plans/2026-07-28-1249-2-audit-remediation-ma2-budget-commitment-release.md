# 2026-07-28-1249-2-audit-remediation-ma2-budget-commitment-release MA2 预算与承付正确性（commitment 释放路径完整性）审查（A2.16）

> Plan Status: active
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.16）
> Related: `docs/plans/2026-07-21-1206-2-finance-budget-multi-year-carryforward.md`（承付会计 COMMITMENT 凭证 + 占用释放 SPI owner doc §实现偏离补注来源）；`docs/plans/2026-07-24-1351-3-commitment-accounting-expansion.md`（sales 承付扩展 + Generator billType 泛化 owner doc §实现偏离补注来源）；`docs/plans/2026-07-26-0410-2-commitment-accounting-browser-e2e.md`（承付会计浏览器层 E2E——commit/release 正路径已验证）；`docs/plans/2026-07-26-1407-2-budget-rollforward-carryforward-browser-e2e.md`（预算结转 commitment 不结转 Deferred owner doc §236 来源）；`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 P2P done——承付 commit/release 接入点 ErpPurOrder.approve / ErpPurInvoice.approve 经运行时复核）；`docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（A2.8 purchase 状态机——三轴 reverseApprove/cancel 承付释放触发路径同型）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）；`docs/design/finance/budget.md` §承付会计 + §sales 承付扩展（owner doc）
> Audit: required

## Current Baseline

预算与承付正确性（commitment 释放路径完整性）多维业务正确性审查。承付会计（Commitment Accounting）是 ERP **预算控制**的核心机制：采购订单/销售订单审批时生成 `postingType=COMMITMENT` 凭证占用预算（支出面采购承付 + 收入面销售承付对称），在订单取消或发票过账（实际占用产生）时红冲释放。承付释放路径完整性直接决定**预算余量（availableAmount = budget − actual − commitment）**正确性——任何释放路径缺口（commitment 泄漏，永不释放）或误释放点（actual + commitment 双重占用）都会导致预算余量失真，进而误导预算控制决策（超预算放行或误拦截）。

承付会计经 plan 2026-07-21-1206-2 落地（采购支出面 commit/release SPI）+ plan 2026-07-24-1351-3 扩展（sales 收入面对称 + Generator billType 泛化）+ plan 2026-07-26-0410-2 浏览器层 E2E（commit/release 正路径验证）。但承付释放路径完整性从未做过系统性多维审查。承付触及 finance 凭证保护区域（IErpFinVoucherBiz.post + posting-exemptions.md 登记复核），但本审计不改代码，只审查正确性。

实时仓库已落地的承付释放路径（待审查，严格对齐 budget.md §承付会计 §3 接入点表）：

- **commit**（接入点 1）：`ErpPurOrder.approve` 后置 → `IErpFinBudgetCommitmentBiz.commit(PURCHASE_ORDER, orderCode, ...)` 生成 COMMITMENT 凭证（Dr 预算占用科目 / Cr 应付-承付），SYNC 同事务（与 `IErpFinBudgetControlBiz.check()` 强一致）。
- **release-on-cancel**（接入点 2）：`ErpPurOrder.reverseApprove` / `cancel`（订单取消路径）→ `IErpFinBudgetCommitmentBiz.release(PURCHASE_ORDER, orderCode)` 红冲原 COMMITMENT 凭证（金额取负），SYNC 同事务。
- **release-on-invoice-approve**（接入点 3）：`ErpPurInvoice.approve`（AP 发票过账 = 实际占用产生 = 释放承付）→ 红冲原 COMMITMENT 凭证，SYNC 同事务。
- **reject release-on-receive**（owner doc §reject 显式裁决）：`ErpPurReceive.approve`（采购入库）是**库存移动**，**不产生 AP ACTUAL 占用**——承付**不应**在入库时释放。owner doc budget.md:260 显式声明："被发票接收" = `ErpPurInvoice.approve`（AP 发票过账产生 ACTUAL 应付），**不是** `ErpPurReceive.approve`。在入库时释放承付会导致 actual + commitment 双重占用预算（红冲 commitment 但 actual 尚未产生）。
- **sales 承付对称**（plan 2026-07-24-1351-3）：销售订单 approve 时 commit（收入预算预留），销售发票过账时 release（实际收入产生）。Generator 按 sourceBillType 派发，Dr/Cr 方向经 subject.direction 自动取，与采购科目独立（`erp-fin.budget-commitment-sales-subject-code`）。
- **Provider**：`CommitmentAcctDocProvider` 支持 `PURCHASE_ORDER_COMMITMENT`（commit 生成）/ `PURCHASE_ORDER_COMMITMENT_REVERSAL`（release 红冲）。
- **守卫**：`release` 无原凭证可红冲时抛 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`。`isReversed` 标记原凭证是否已被红冲（红冲凭证自身 `isReversed=true` 不参与余量聚合）。
- **配置**：`erp-fin.budget-commitment-enabled`（总开关默认 **false**，保护既有 113 purchase 测试不触发承付凭证）+ `erp-fin.budget-commitment-subject-code`（采购承付占用科目编码，启用采购承付时必填）+ `erp-fin.budget-commitment-sales-subject-code`（销售承付占用科目编码，启用 sales 承付时必配）。
- **结转**：A2 默认 **commitment 不结转**（与 actualAmount 合并记录在源 Scenario 余量计算，结转后由源 Scenario CLOSED 终态保留审计轨迹；"未释放 commitment 一并结转至下年度" 归 Deferred successor）。

**但从未做过一次系统性多维审查覆盖承付释放路径完整性**。已知未核验控制点（budget.md §承付会计 §审查维度 + 实现偏离补注 + A2.1 P2P 运行时复核）：

- **释放路径完整性（核心）**：是否所有应释放承付的场景都有 release 触发？— (1) 全额发票过账释放（ErpPurInvoice.approve）；(2) 订单取消释放（reverseApprove/cancel）；(3) **部分开票**（一张 PO 多次部分开票——commitment 是全额释放还是按开票金额部分释放？全额释放会过早释放未开票部分的占用）；(4) **采购退货/退款**（ErpPurReturn 经反向出库 + PURCHASE_RETURN 过账——是否触发承付释放或红冲 ACTUAL？）；(5) **AP 发票冲销**（reverse 红字凭证——原 ACTUAL 占用回退后 commitment 是否恢复？）；(6) **多年度跨期发票**（PO 在 N 年 approve commit，发票在 N+1 年 approve release——承付与 actual 落在不同期间，预算余量跨期一致性）。
- **误释放防护**：是否在错误时点释放承付？— (1) **release-on-receive 误释放**（owner doc §reject 显式禁止——复核 ErpPurReceive.approve 是否真的不调 release）；(2) **重复释放**（同一订单多次发票 approve 是否重复红冲原 commitment → ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 守卫是否覆盖）；(3) **取消后再发票**（PO cancel 释放 commitment 后，若误开票 approve 是否无 commitment 可释放抛守卫）。
- **对称性**：sales 承付（SO approve commit / sales invoice approve release）与采购承付（PO approve commit / AP invoice approve release）是否真对称？— sales 收入面 Dr/Cr 方向经 subject.direction 自动取是否正确；sales 承付科目独立配置是否落实。
- **预算控制一致性**：`IErpFinBudgetControlBiz.check()`（预算校验）与 commitment 释放是否强一致（SYNC 同事务）？— commit 与 budget check 同事务保证占用可见性；release 与 reverseApprove 同事务保证释放原子性。跨事务失败是否导致占用/释放悬挂。
- **聚合与余量正确性**：`isReversed` 标记 + `postingType=COMMITMENT` 余量聚合——红冲凭证不参与余量聚合是否落实？availableAmount = budget − actual − commitment 实时计算是否排除已红冲 commitment。
- **守卫覆盖**：`ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（无原凭证可红冲）守卫边界——是否所有 release 路径都经此守卫？是否存在绕过守卫的裸 voucher 操作。
- **config-gate 边界**：总开关默认 false——启用前/后行为是否一致？113 purchase 测试在默认关闭下不触发承付凭证，启用后是否引入回归（commitment 凭证意外生成）。
- **与设计文档一致性**：budget.md §承付会计 §3 接入点表 vs 实现——重点核验：(1) §3 三个接入点（commit / release-on-cancel / release-on-invoice-approve）落地；(2) §reject release-on-receive 显式裁决（ErpPurReceive.approve 不释放）；(3) §sales 承付扩展对称；(4) §配置项默认值；(5) §commitment 不结转（Deferred successor）。

剩余差距：需要一次系统性多维审查，发现任何遗漏的 P0（**部分开票全额释放致未开票部分占用过早释放** [若破坏预算余量] / **release-on-receive 误释放致 actual+commitment 双重占用** [若实现违反 owner doc §reject] / **采购退货/退款未释放承付致 commitment 泄漏** [若破坏释放路径完整性] / **AP 发票冲销后 commitment 未恢复致余量永久偏移** [若破坏跨冲销一致性] / **取消后再开票绕过守卫致裸 voucher 操作** [若破坏守卫覆盖] / **sales 承付 Dr/Cr 方向经 subject.direction 错误致收入预算余量符号反转** [若破坏对称性]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 对 **承付释放路径完整性**做系统性多维业务正确性审查，产出审计报告。审查维度：释放路径完整性（核心）/ 误释放防护 / 采购-sales 对称性 / 预算控制一致性 / 聚合与余量正确性 / 守卫覆盖 / config-gate 边界 / 与设计文档一致性。
- 重点核验已识别控制点：(1) 三个接入点（commit / release-on-cancel / release-on-invoice-approve）落地；(2) **§reject release-on-receive**（ErpPurReceive.approve 真的不释放）；(3) **部分开票释放语义**（全额 vs 部分释放）；(4) **采购退货/退款释放**；(5) **AP 发票冲销后 commitment 恢复**；(6) **重复释放守卫**（ERR_BUDGET_COMMITMENT_ALREADY_RELEASED）；(7) **多年度跨期发票**余量一致性；(8) **sales 承付对称**（Dr/Cr 方向 + 独立科目）；(9) **预算控制强一致**（SYNC 同事务）；(10) **聚合排除已红冲**（isReversed + postingType 过滤）。
- scope matrix §业财端到端/业务正确性 承付释放路径行终态标记（`❓` → `✅`/`⚠️(P1)`）。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（按 finding 逐项提议目标：承付业务正确性代码缺陷 → **MR1** [承付属 MA2 业务正确性批次，与既有 A2.1-A2.16 的业务正确性 P1 同型]，owner-doc drift 类 → MR2；最终路由在 R*.0 展开机制确定）。

## Non-Goals

- **不**审计 A2.3 期末结账端到端完整性 — done（承付不结转已裁定 Deferred successor）；本审计只复核 commitment 不结转在余量计算上的影响。
- **不**审计 A2.1 P2P / A2.2 O2C 端到端 — done；本审计只复核承付 commit/release 接入点经 P2P/O2C 运行时复核的状态。
- **不**审计 A2.8 purchase / A2.9 sales 状态机 — done；本审计只复核 reverseApprove/cancel/invoice approve 承付释放触发路径的正确性，不复核状态机迁移本身。
- **不**审计 A2.5a/b finance 凭证/期间状态机 — done；本审计只复核 COMMITMENT postingType 凭证聚合与余量计算。
- **不**审计 A2.17 并发与乐观锁 — 并发 commit/release 同事务竞争归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 A4.x CommitmentAcctDocProvider 代码质量抽样 — 归 MA4；本审计只核验释放路径正确性。
- **不**审计 config-gate / Deferred 偏离是否应实现（commitment 一并结转 / 多公司合并预算 / 预算物化快照表） — owner doc 已裁定为 Deferred，本审计只确认其在释放路径完整性上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R2.0 展开机制进入 MR2（承付属 finance owner doc + 代码维度）。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/budget.md`（§承付会计 §3 接入点 + §reject release-on-receive + §CommitmentAcctDocProvider + §SPI 契约 + §配置项 + §sales 承付扩展 + §commitment 不结转）；`docs/design/finance/posting.md`（COMMITMENT postingType 凭证结构）；`docs/architecture/posting-exemptions.md`（承付 Facade 豁免登记复核）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor + Provider 两层）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A2.16 指定此 skill，多维业务正确性审查专用方法，项目定制化层见 `docs/skills/README.md`）。承付释放路径完整性跨 release 触发点（pur order/invoice/return + sal order/invoice）+ 聚合（凭证红冲 + 余量计算）+ 对称（采购-sales）+ 配置（config-gate）多维度，适合多维审查而非单维状态机审查。
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试（含 commitment E2E 2026-07-26-0410-2 回归）。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **承付触发需启用 config**：审查承付实际行为时需关注 `erp-fin.budget-commitment-enabled`（默认 false）+ `erp-fin.budget-commitment-subject-code`（必配）+ `erp-fin.budget-commitment-sales-subject-code`（sales 承付必配）。E2E（2026-07-26-0410-2）已配置这些 webServer JVM arg。本审计不改 config，只复核 config-gate 边界正确性。
- **保护区域门控**：承付触及 finance 凭证保护区域（IErpFinVoucherBiz.post COMMITMENT postingType）+ 预算控制保护区域（IErpFinBudgetControlBiz.check）。P0 即时修复若触及 `IErpFinBudgetCommitmentBiz`/`CommitmentVoucherGenerator`/`CommitmentAcctDocProvider`/pur/sal posting hook/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计/预算保护区域）。xbiz 文件变更属承付契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 承付释放路径完整性多维审查

Status: planned
Targets: `module-finance/`（IErpFinBudgetCommitmentBiz SPI + CommitmentVoucherGenerator + CommitmentAcctDocProvider + postingType=COMMITMENT 凭证聚合 + isReversed 余量计算 + ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 守卫 + config-gate）；`module-purchase/`（ErpPurOrder.approve commit 接入点 + ErpPurOrder.reverseApprove/cancel release-on-cancel + ErpPurInvoice.approve release-on-invoice-approve + ErpPurReceive.approve reject-release 复核 + ErpPurReturn 退货释放路径）；`module-sales/`（ErpSalOrder.approve commit + ErpSalInvoice.approve release sales 承付对称）；`module-finance/` 余量计算（availableAmount = budget − actual − commitment 实时计算 + 红冲凭证排除）
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（承付 Facade 经 posting-exemptions 登记复核）；A2.1 P2P done（承付 commit/release 接入点 ErpPurOrder.approve / ErpPurInvoice.approve 经运行时复核）；A2.2 O2C done（sales 承付对称复核角度）；A2.3 期末结账 done（commitment 不结转 Deferred 裁决）；A2.8 purchase done（reverseApprove/cancel 承付释放触发路径）；A2.9 sales done（sales 承付触发路径）；承付会计实现 plan 2026-07-21-1206-2 + 扩展 plan 2026-07-24-1351-3 + E2E 2026-07-26-0410-2 已落地（commit/release 正路径已验证，本审计补系统性释放路径完整性审查）

- [ ] 维度「释放路径完整性（核心）」：核验所有应释放承付场景都有 release 触发——(1) 全额发票过账释放（ErpPurInvoice.approve）；(2) 订单取消释放（reverseApprove/cancel）；(3) **部分开票释放语义**（一张 PO 多次部分开票——commitment 全额释放 vs 按开票金额部分释放，owner doc 是否声明）；(4) **采购退货/退款释放**（ErpPurReturn 反向出库 + PURCHASE_RETURN 过账——是否触发承付释放或红冲 ACTUAL）；(5) **AP 发票冲销**（reverse 红字凭证——原 ACTUAL 占用回退后 commitment 是否恢复）；(6) **多年度跨期发票**（PO N 年 approve commit，发票 N+1 年 approve release——承付与 actual 跨期余量一致性）。无 commitment 泄漏（永不释放）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「误释放防护」：核验不在错误时点释放——(1) **release-on-receive 误释放**（owner doc §reject 显式禁止——复核 ErpPurReceive.approve 是否真的不调 release，实现是否违反裁决）；(2) **重复释放**（同一订单多次发票 approve 是否重复红冲原 commitment → ERR_BUDGET_COMMITMENT_ALREADY_RELEASED 守卫覆盖）；(3) **取消后再发票**（PO cancel 释放 commitment 后误开票 approve 是否无 commitment 可释放抛守卫）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「采购-sales 对称性」：核验 sales 承付（SO approve commit / sales invoice approve release）与采购承付（PO approve commit / AP invoice approve release）真对称——sales 收入面 Dr/Cr 方向经 subject.direction 自动取是否正确；sales 承付科目独立配置（budget-commitment-sales-subject-code）是否落实；sales 收入预算余量符号是否与采购支出面对称。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「预算控制一致性」：核验 `IErpFinBudgetControlBiz.check()`（预算校验）与 commitment 释放强一致（SYNC 同事务）——commit 与 budget check 同事务保证占用可见性；release 与 reverseApprove/invoice approve 同事务保证释放原子性；跨事务失败是否导致占用/释放悬挂。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「聚合与余量正确性」：核验 `isReversed` 标记 + `postingType=COMMITMENT` 余量聚合——红冲凭证（isReversed=true）不参与余量聚合是否落实；availableAmount = budget − actual − commitment 实时计算是否排除已红冲 commitment；budgetLine.commitmentAmount = Σ Commitment 凭证派生是否正确。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「守卫覆盖」：核验 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（无原凭证可红冲）守卫边界——是否所有 release 路径都经此守卫；是否存在绕过守卫的裸 voucher 操作（直接 updateEntity 置 isReversed 绕过 IErpFinBudgetCommitmentBiz.release）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「config-gate 边界」：核验总开关默认 false 行为——启用前/后是否一致；113 purchase 测试在默认关闭下不触发承付凭证；启用后是否引入回归（commitment 凭证意外生成）；科目配置缺失时是否 fail-fast。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「与设计文档一致性」：budget.md §承付会计 §3 接入点表 vs 实现——重点核验：(1) §3 三接入点（commit / release-on-cancel / release-on-invoice-approve）落地；(2) §reject release-on-receive 显式裁决（ErpPurReceive.approve 不释放）；(3) §sales 承付扩展对称（plan 2026-07-24-1351-3）；(4) §配置项默认值；(5) §commitment 不结转（Deferred successor，余量计算影响）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 复核已登记 finding 承付释放路径角度：P1-MA1-022（finance 跨域只读——承付接入点 pur/sal 跨域调用经 I*Biz Facade 非异常路径）；P1-MA2-001（P2P 暂估冲回——承付释放与暂估冲回语义边界）；P1-MA2-009（O2C 收款核销汇兑损益——sales 承付释放与汇兑分支），标注终态。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`（含：承付释放路径完整矩阵 [所有 commit/release 场景 × 触发点 × 事务边界 × 守卫]、各维度通过/失败裁决、控制点 PASS/FAIL、部分开票释放语义/退货释放/AP 冲销恢复/重复释放守卫/release-on-receive 误释放防护/sales 对称/预算控制强一致/聚合排除已红冲/config-gate 边界裁决、MA1/MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 承付释放路径完整矩阵产出（所有 commit/release 场景 × 触发点 × 事务边界 × 守卫），每个场景有通过/失败裁决与证据
- [ ] 已识别控制点（释放完整性[含部分开票/退货/AP 冲销/跨期] / 误释放防护[含 release-on-receive/重复释放] / 对称性 / 预算控制一致性 / 聚合余量 / 守卫覆盖 / config-gate 边界 / 与设计文档一致性）均有通过/失败裁决与证据
- [ ] multi-dimensional-audit 8 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR2 + 索引/矩阵更新

Status: planned
Targets: 承付释放路径审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §业务正确性 承付释放路径行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（**部分开票全额释放致未开票部分占用过早释放** [若破坏预算余量] / **release-on-receive 误释放致 actual+commitment 双重占用** [若实现违反 owner doc §reject] / **采购退货/退款未释放承付致 commitment 泄漏** [若破坏释放路径完整性] / **AP 发票冲销后 commitment 未恢复致余量永久偏移** [若破坏跨冲销一致性] / **取消后再开票绕过守卫致裸 voucher 操作** [若破坏守卫覆盖] / **sales 承付 Dr/Cr 方向经 subject.direction 错误致收入预算余量符号反转** [若破坏对称性]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计/预算保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR、修复状态 todo）。按 finding 逐项提议目标：承付业务正确性代码缺陷 → MR1（承付属 MA2 业务正确性批次，与既有 A2.1-A2.16 的业务正确性 P1 同型）；owner-doc drift 类 → MR2。新 P1（如部分开票释放语义 owner doc 未声明 / commitment 不结转余量偏移 / config-gate 边界回归 [若确认]）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §业务正确性 承付释放路径行终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R*.0 展开（按 finding 逐项 MR1/MR2 路由）
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_058eb09a5ffeLWNrWbTzLMLaJf`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`IErpFinBudgetCommitmentBiz`（erp-fin-dao）+ `CommitmentVoucherGenerator`（erp-fin-service）+ `CommitmentAcctDocProvider` 存在 ✓；三个接入点实仓确认——commit hook（ErpPurOrderProcessor:223）+ release-on-cancel（reverseApprove:118 + cancel:130）+ release-on-invoice-approve（ErpPurInvoiceProcessor:306）✓；**§reject release-on-receive 经全文阅读 ErpPurReceiveProcessor（427 行）确认零 commitment/release 调用 + 零 SPI 注入**，reject rationale 在代码 ErpPurInvoiceProcessor:93-94,304 文档化 ✓；sales 对称（ErpSalOrderProcessor:66 + ErpSalInvoiceProcessor:62 注入 SPI）+ `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`（ErpFinErrors:415，ErpFinBudgetCommitmentBizModel:89 throw）+ 3 config keys（ErpFinConstants:412,414,416）✓；6 related plans 存在 + 3 finding ID 存在 ✓；budget.md §承付会计 §3 接入点 + §reject + §sales + §配置 + §不结转 全部命中 ✓；A2.16 与 done A2.3 distinct ✓；最低规则 R1/R2/R4/R7/R8 全 PASS；反松弛零禁词。**采纳 1 项非阻塞修订**：P1 目标由 blanket MR2 改为按 finding 逐项路由（业务正确性代码缺陷 → MR1，owner-doc drift → MR2）——与既有 A2.1-A2.16 业务正确性 P1 路由一致。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。承付触及 finance 凭证 + 预算控制保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [ ] 范围内行为完成（A2.16 承付释放路径完整性系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、budget.md §承付会计 owner doc 结论已反映）
- [ ] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.3 期末结账 commitment 不结转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.3 done（期末结账端到端完整性已审查）。owner doc budget.md:236 显式裁定 commitment 不结转（与 actualAmount 合并记录在源 Scenario 余量计算）。"未释放 commitment 一并结转至下年度" 归 Deferred successor。本审计只复核 commitment 不结转在余量计算上的影响。
- Successor Required: `yes`——"commitment 一并结转" Deferred 触发时复核。

### A2.17 并发与乐观锁（并发 commit/release 同事务竞争）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（并发 commit/release 同事务竞争 / 部分开票并发释放），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated / Deferred 偏离本身（commitment 一并结转 / 多公司合并预算 / 预算物化快照表 / 预算编制工作流）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc budget.md 已裁定为 Deferred（§Deferred successor 列表）。本审计只确认其在释放路径完整性上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待执行后填写>
- Evidence: <待执行后填写>

Follow-up:

- <待执行后填写；已确认的缺陷不得出现在此处>
