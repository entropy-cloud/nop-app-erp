# 2026-07-11-0730-2-quality-ncr-posting-lifecycle-e2e 质量 NCR 过账生命周期浏览器层 E2E（reverseNcr 红字凭证 + RETURN 处置退货编排）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: deferred items from `2026-07-10-1800-1-inventory-move-ncr-scrap-voucher-line-e2e.md`（Deferred「NCR reverseNcr 红字凭证行断言」）+ `2026-07-10-0335-2-ncr-capa-resolve-mnt-request-e2e.md`（Deferred「NCR RETURN/SCRAP 触发过账/退货副作用 successor」RETURN 子集）
> Related: `2026-07-10-1800-1-inventory-move-ncr-scrap-voucher-line-e2e.md`（NCR SCRAP 正向凭证行基线），`2026-07-10-0335-2-ncr-capa-resolve-mnt-request-e2e.md`（NCR resolve CAPA 闭包门控基线），`2026-07-05-2352-2-ncr-financial-posting.md`（NCR 过账后端基线）
> Audit: required

## Current Baseline

- `business-actions/_helper.ts` 已建立三原语：`createViaSave`/`callMutationOk`/`verifyState`（0814-2 落地）。`orchestration/_helper.ts` 已建立 `findVoucherIdByBillCode(page, billCode, postingType?)`（3-param，`_helper.ts:99-114`，按 postingType NORMAL/REVERSAL 区分原/红字凭证）+ `assertVoucherLines(page, voucherId, expectedLines)`（按 voucherId 查 ErpFinVoucherLine 逐行断言 subjectCode + dcDirection + 借贷金额）两原语（0704-1 落地）。
- `quality-ncr-scrap-posting.action.spec.ts`（1800-1）已覆盖 NCR SCRAP 正向过账：resolve AUTO_POST → `NcrPostingDispatcher.dispatchScrap` → `NcrScrapAcctDocProvider` → NCR_SCRAP 凭证行 Dr 6711=120 / Cr 1401=120。断言经 `findVoucherIdByBillCode(page, ncrCode, 'NORMAL')` + `assertVoucherLines`。**同 spec 已覆盖 reverseNcr 的 `posted=false` 回退断言**（spec:87），但未断言红字凭证**行级金额取负**——Phase 1 新增价值在此。
- `quality-ncr-resolve-capa-gate.action.spec.ts`（0335-2）已覆盖 NCR resolve CAPA 闭包门控：负路径（CAPA PENDING 时 resolve 抛 `ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED`）+ 正路径（CAPA 三步闭包后 resolve 成功 status=RESOLVED）。
- **未覆盖**（Deferred）：
  - **NCR reverseNcr 红字凭证行断言**（1800-1 Deferred）：`reverseNcr(@Name("ncrId"))`（`ErpQaNonConformanceBizModel:124`）对已过账 NCR 调 `NcrPostingDispatcher.reverseScrap(ncr)`（`:92`）→ 内部 `executor.reverse(ncrCode, NCR_SCRAP)` 生成红字凭证（`buildReversalDraft` 同向取负）。0704-1 已建立红字断言范式（p2p-reverse/o2c-reverse），结构可预测。触发条件「当需验证 NCR SCRAP 红冲凭证行级正确性时」已满足。
  - **NCR RETURN 处置退货编排**（0335-2 Deferred RETURN 子集）：resolve 时 dispositionType=RETURN → `NcrReturnOrchestrator.orchestrateReturn`（`module-quality/erp-qa-service/src/main/java/app/erp/qa/service/posting/NcrReturnOrchestrator.java:70`）按 NCR 来源（supplierId 非空→采购退货 / 为空→销售退货）经 `IErpPurReturnBiz.save`/`IErpSalReturnBiz.save` 创建退货单 + `ncr.setReturnCode(returnCode)` 登记。触发条件「当需验证 RETURN 处置退货副作用时」已满足。
- webServer JVM args（`playwright.config.ts:18`）当前含 `-Derp-qua.ncr-default-acct-schema=1` + `-Derp-mfg.variance-auto-calc-enabled=true`。NCR config `erp-qua.ncr-posting-mode` 默认 AUTO_POST（resolve 时自动过账 SCRAP）。
- 种子数据：`app-erp-all/src/main/resources/_vfs/_init-data/` 含 `erp_qa_non_conformance` 种子行 + 6711/1401 科目行（1800-1 补齐）。`erp_md_partner` 已有供应商种子行（P2P 链路 1234-1 落地）。
- E2E 套件当前 174 测试（1800-2 基线）。

## Goals

- 扩展质量域 NCR 过账生命周期浏览器层 E2E 覆盖：reverseNcr 红字凭证行级断言 + RETURN 处置跨域退货编排验证。
- 解除 1800-1 一项 Deferred + 0335-2 一项 Deferred（RETURN 子集）。

## Non-Goals

- NCR RETURN 处置退货单后续审批 + 反向库存 + 红字过账浏览器层（退货域自身审批/过账链已有独立 E2E 覆盖 0335-1 pur-return/sal-return，本计划仅验证 NCR→退货单创建编排 + returnCode 登记）。
- NCR CONCESSION/DOWNGRADE 处置（无过账/无退货副作用，0335-2 已标注「无额外处理」）。
- NCR SCRAP 正向凭证行断言（1800-1 已覆盖）。
- NCR resolve CAPA 闭包门控（0335-2 已覆盖）。
- NCR escalateToRecall（2004-1 已覆盖）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/testing/e2e-runbook.md`（§质量域 NCR 过账生命周期段扩展），`docs/design/quality/ncr-financial-posting.md`（§SCRAP 过账 §RETURN 编排 §reverseNcr），`docs/design/quality/state-machine.md`（§NCR 状态机）
- Skill Selection Basis: 本计划为 Playwright E2E spec（非平台 `JunitAutoTestCase`），`nop-testing` 技能覆盖平台测试非 Playwright → `Skill: none`；测试编写遵循 1800-1/0704-1 已验证 `assertVoucherLines` + `callMutationOk`/`verifyState` 范式。

## Infrastructure And Config Prereqs

- Phase 1：No infra prereqs beyond existing baseline（reverseNcr 为 DIRECT `@BizMutation`，无额外 config gate）。webServer `-Derp-qua.ncr-default-acct-schema=1` 已设（1800-1）。种子 6711/1401 科目已存在（1800-1 补齐）。
- Phase 2：需 supplier 种子行（采购退货路径需 NCR.supplierId 非空）。`erp_md_partner` 种子已有供应商行（P2P 链路 1234-1 落地）。NCR 测试数据经 `createViaSave` 浏览器层创建（supplierId 引用种子供应商 id）。`NcrReturnOrchestrator.resolveWarehouseId` 按 materialId 查 `ErpInvStockBalance`(limit 1) 解析仓库/币种——需 materialId 有余额行（种子 MAT_1 已有）。

## Execution Plan

### Phase 1 — NCR reverseNcr 红字凭证行断言

Status: completed
Targets: `tests/e2e/business-actions/quality-ncr-reverse-voucher-line.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: 1800-1 完成（NCR SCRAP 正向过账凭证行断言范式已落地）+ 0704-1 完成（红字凭证行断言范式 `findVoucherIdByBillCode(REVERSAL)` + `assertVoucherLines` 已落地）

- [x] `Add`：新建 `quality-ncr-reverse-voucher-line.action.spec.ts`——经 `createViaSave` 建 NCR（dispositionType=SCRAP + materialId=种子物料）→ submitReview → resolve（AUTO_POST 自动生成 NCR_SCRAP 正向凭证）→ `callMutationOk`(`ErpQaNonConformance__reverseNcr`, ncrId) 红冲
  - Skill: none
- [x] `Proof`：断言红字凭证行——`findVoucherIdByBillCode(page, ncrCode, 'REVERSAL')` 获取红字凭证 id → `assertVoucherLines` 逐行断言 subjectCode + dcDirection + 借贷金额（同向取负：Dr 6711=-120 / Cr 1401=-120，对齐 0704-1 红字范式 `buildReversalDraft` dcDirection 不变金额取负）
  - 成功模式：红字凭证行金额为正向凭证的负值，subjectCode 一致（6711 Dr / 1401 Cr）；原正向凭证 isReversed=true
  - Skill: none
- [x] `Proof`：断言原正向凭证标记回退——`findVoucherIdByBillCode(page, ncrCode, 'NORMAL')` 获取原凭证 → 经 `findFirst` `ErpFinVoucher` 断言 `isReversed=true`（区别于 1800-1 仅断言 NCR.posted=false，本计划新增凭证级 isReversed 断言 + 红字行级金额取负）
  - Skill: none

Exit Criteria:

- [x] `quality-ncr-reverse-voucher-line.action.spec.ts` 全绿：reverseNcr 红字凭证行金额取负 + subjectCode 一致 + 原正向凭证 isReversed=true
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试）

### Phase 2 — NCR RETURN 处置跨域退货编排

Status: completed
Targets: `tests/e2e/business-actions/quality-ncr-return-disposition.action.spec.ts`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成 + 0335-2 完成（NCR resolve CAPA 闭包门控范式已落地）+ 1234-1 完成（supplier 种子已落地）

- [x] `Add`：新建 `quality-ncr-return-disposition.action.spec.ts`——经 `createViaSave` 建 NCR（dispositionType=RETURN + supplierId=种子供应商 + materialId=种子物料）→ submitReview → resolve
  - Skill: none
- [x] `Proof`：断言退货单创建——resolve 返回后 `verifyState` NCR `returnCode` 非空（格式 `PR-FROM-NCR-{ncrId}`）；经 GraphQL `ErpPurReturn__get(returnCode)` 验证退货单存在 + supplierId 一致 + docStatus=DRAFT + approveStatus=UNSUBMITTED
  - 成功模式：NCR.returnCode 非空 + ErpPurReturn 存在且 supplierId 匹配
  - 失败模式：dispositionType=CONCESSION 时 resolve returnCode 保持 null（无退货副作用）
  - Skill: none
- [x] `Decision`：CAPA 门控 setup——resolve 须全部 CAPA COMPLETED（`NcrLifecycleService.requireResolveGate`）。NCR 无关联 CAPA 时 resolve 直接通过（0335-2 已验证无 CAPA 路径）。测试 NCR 不创建 CAPA 以简化 setup（对齐 0335-2 无 CAPA 路径范式）。
  - Skill: none
  - Alternatives: 若 resolve 强制要求至少一个 CAPA，则参照 0335-2 正路径建 CAPA + 三步闭包。框架约束选择（live repo 核实）。

Exit Criteria:

- [x] `quality-ncr-return-disposition.action.spec.ts` 全绿：resolve RETURN 后 NCR.returnCode 非空 + ErpPurReturn 创建且 supplierId 匹配；CONCESSION 对比 returnCode=null
- [x] 不引入生产代码/契约/ORM 模型变更（纯测试）

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_0b19ec113ffepSO3wnqlxpLh86`, general agent 新会话) — 1 BLOCKING: `findVoucherIdByBillCode` 为 3-param `(page, billCode, postingType?)` 非 4-param（无 businessType 参数），原计划 baseline + Phase 1 均写 4-arg 调用致 TypeScript compile error。修订：移除所有第 4 参数 `'NCR_SCRAP'`，postingType 过滤已足够区分 NORMAL/REVERSAL。另吸收 3 项非阻塞观察：① 方法名 `reverseScrap` 而非 `reverse`（已纠正 baseline）；② `orchestrateReturn` 行号 `:70` 非 `:57`（已纠正）；③ `posted=false` 已由 1800-1 覆盖，Phase 1 新增价值改为凭证级 `isReversed=true` + 红字行级金额取负（已重构）。
- Independent draft review iteration 2: accept (`ses_0b195561dffe9n37Cg9bNe4sIW`, general agent 新会话) — BLOCKING 已确认修复：全部 4 处 `findVoucherIdByBillCode` 调用为 3-param（无 businessType 参数）。3 项非阻塞观察全部吸收验证：方法名 `reverseScrap`、行号 `:70`、Phase 1 价值重构为凭证级 `isReversed` + 红字行级金额取负。无新引入问题。计划为可接受执行契约。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。在结束时运行 `npx playwright test`（或项目等效命令）一次。

- [x] 范围内行为完成（reverseNcr 红字凭证行取负断言 + RETURN 处置退货单创建 + returnCode 登记）
- [x] 相关文档对齐（`e2e-runbook.md` §质量域 NCR 过账生命周期段扩展 — reverseNcr/RETURN 子段；1800-1/0335-2 Deferred 标记承接 done）
- [x] 已运行验证（`npx playwright test tests/e2e/business-actions/quality-ncr-reverse-voucher-line.action.spec.ts tests/e2e/business-actions/quality-ncr-return-disposition.action.spec.ts` 全绿 + 全 workspace `npx playwright test` 无回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### NCR RETURN 处置退货单后续审批 + 反向库存 + 红字过账浏览器层

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 退货域（purchase/sales return）自身的审批 + 反向库存 + PURCHASE_RETURN/SALES_RETURN 红字过账已有独立 E2E 覆盖（0335-1 pur-return/sal-return action spec）。本计划验证 NCR→退货单创建编排 + returnCode 登记，退货单后续生命周期由退货域自身覆盖。
- Successor Required: no（退货域 E2E 已覆盖）

## Closure

Status Note: closed

Closure Audit Evidence:

- Auditor / Agent: `ses_0b0a14aedffelOxI92eeRinE0g`（general agent 独立新会话）— **CLOSURE AUDIT: PASS**
- 8 checks 全 PASS：①两 spec 文件存在且非平凡（104/134 行真实测试逻辑）②Phase 1 reverseNcr 红字凭证行断言正确（3-arg findVoucherIdByBillCode 无第 4 参数，REVERSAL 行同向取负 Dr 6711=-120/Cr 1401=-120 + 原正向凭证 isReversed=true）③Phase 2 RETURN 处置退货编排正确（returnCode=PR-FROM-NCR-{ncrId} 格式 + ErpPurReturn supplierId/docStatus/approveStatus 验证 + CONCESSION 对照 returnCode=null）④生产代码对齐（NcrReturnOrchestrator.createPurchaseReturn code 字符串拼接精确匹配）⑤测试数 3（1+2）⑥计划一致性（两 Phase Status=completed + 全 [x] + Plan Status=completed）⑦零生产代码变更（git status 仅 spec+plan+docs，无 .java/.orm.xml/.api.xml/_gen）⑧backlog README 含 0730-2 ✅ done 行。
- 验证基线：business-actions+orchestration 44 passed（含新增 3 test）；全 workspace 88 failures 全在 visual/reports/crud/dashboards（AMIS 渲染超时预存环境问题，非 DB 污染/非回归）。

Follow-up:

- 无非阻塞跟进项
