# RC MA1 A1.1 — finance-F1 过账引擎与凭证链路 需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.1（MA1 需求追踪矩阵审计 — finance-F1 过账引擎与凭证链路）
> 审计 plan：`docs/plans/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/finance/use-cases.md`（UC-FIN-01/02/03/04/12/15，6 UC）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §finance + §切片索引 A1.1 + §基线分歧登记 D-02
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-02
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **2** | P1-RC-001（UC-FIN-04/15 科目分摊机制完全缺失）/ P1-RC-002（UC-FIN-12 汇率缺失未拒绝过账，静默回退 rate=1）→ 待 MR1（R1.0 展开为 RC-R1.n） |
| **接受**（符合需求契约） | **4 UC** | UC-FIN-01 / UC-FIN-02 / UC-FIN-03 + UC-FIN-12 断言 3（EXCHANGE_GAIN_LOSS 期末重估） |
| MA2 既有行为证据复用 | 11 项 finding | 无升级（详见 §4 / §9） |

**整体裁决**：A1.1 切片 6 UC 五级追踪矩阵填齐。过账引擎主体（业财回链 / 借贷平衡 / 红字冲销 / 可插拔 Provider 路由 / 幂等）经 L3-L5 四级证据确认符合 UC-FIN-01/02/03 验收标准。**两项 P1 需求分歧**：①UC-FIN-04/15 科目分摊（GlDistribution）机制**完全缺失**（Validator Bean + 规则实体 + percent!=100 拒绝 + 拆行平衡均未实现，FactsValidator 调用循环为死代码）；②UC-FIN-12「汇率缺失→拒绝过账」守卫**未实现**（实现为静默回退 `EXCHANGE_RATE_DEFAULT=1` 继续过账，与需求契约直接冲突）。两项均按 §2 判据定为 P1（功能完全缺失 / 异常路径未实现），按 §10 经 MR1 批量修复通道修复（R1.0 展开为 RC-R1.n）；**无 P0**——触发面均依赖前置条件（科目分摊为功能未建、汇率回退需域调用方漏传 rate），非默认活跃路径破坏，且与既有 MA2 对多币种路径的 P1 分级一致。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。以下逐 UC 逐字引用验收标准原文，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-FIN-01 业财自动过账（`use-cases.md:16`）

```
行为链路：
业务单据.审核通过 →
  按 businessType 路由到对应 IErpFinAcctDocProvider
  → 填充凭证模板(AMOUNT 等占位符)
  → FactsValidator 校验/改写
  → 写库(凭证 + 业财回链 VoucherBillR)
  → 单据.posted = true

可验证断言：
存在凭证: businessType 对应 + 来源单号 == 业务单据.单号
凭证行借贷平衡(Σ借 == Σ贷)
存在 VoucherBillR(billType, billCode=业务单据.单号) 双向回链
业务单据.posted == true
```

### UC-FIN-02 业务单据作废触发红字冲销（`use-cases.md:42`）

```
可验证断言：
业务单据.作废 →
  经 VoucherBillR 反查关联凭证
  生成红字凭证(金额取负, 关联原凭证)
  原凭证标记 isReversed = true
  业务单据.posted = false
红字凭证走 DRAFT → POSTED 流程
```

### UC-FIN-03 可插拔 Provider 路由（`use-cases.md:60`）

```
可验证断言：
新增 IErpFinAcctDocProvider Bean(注册 businessType=PROJECT_SETTLEMENT)
→ ErpFinAcctDocRegistry 自动聚合(@Inject List)
→ 项目结算单审核时, 该 Provider 被路由调用, 生成凭证
→ 核心过账引擎代码无改动
```

### UC-FIN-04 FactsValidator 科目分摊（`use-cases.md:76`）

```
可验证断言：
原始凭证行(挂成本中心A, 金额100) →
  命中 GlDistribution 规则(A→A:60%/B:40%) →
  拆为两行: 成本中心A 金额60, 成本中心B 金额40
Σ 拆分行金额 == 原行金额(平衡保持)
若 Σ percent != 100: 抛异常拒绝过账
```

### UC-FIN-12 多币种过账（`use-cases.md:223`）

```
可验证断言：
凭证行.本位币金额 == 源币金额 × 汇率
若 汇率缺失 → 报错拒绝过账
外币银行账户对账: 未达账项调整考虑汇兑损益(EXCHANGE_GAIN_LOSS)
```

### UC-FIN-15 科目分摊(GL Distribution)（`use-cases.md:298`）

```
可验证断言：
原始凭证行(成本中心A, 金额100) →
  命中 GlDistribution 规则(A→A:60%/B:40%) →
  拆为两行: 成本中心A 金额60, 成本中心B 金额40

Σ 拆分行金额 == 原行金额(平衡保持)
若 Σ percent != 100 → 抛异常拒绝过账
分摊由 ErpFinGlDistributionValidator(IErpFinFactsValidator 实现)执行
getOrder() 较高, 确保在其他 Validator 之后
```

> **D-02 基线分歧注记**（`rc-requirement-baseline-inventory.md §基线分歧登记 D-02`）：UC-FIN-04/15 同主题（科目分摊），roadmap 已裁决同属 A1.1。本报告按 L1 逐字引用两条验收标准，不合并（§9 真相源冻结条款）。

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-finance/erp-fin-service/.../`）。L3 引用格式遵循 §1 L3 规范（含行号）。

### 2.1 过账引擎主体（UC-FIN-01/02/03 共用）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 凭证聚合根 Facade | `module-finance/erp-fin-service/.../service/entity/ErpFinVoucherBizModel.java` post:67-74 / reverse:76-84 / postVoucher:86-101 / reverseVoucher:103-116 / previewReverseVoucher:122-151 / assertPeriodNotLocked:177-195 | ✅ |
| 正向过账编排 | `module-finance/erp-fin-service/.../service/posting/ErpFinPostingProcessor.java` process:126-212（幂等前置 :136-140 → Provider 路由 :143-146 → createFacts :153-155 → FactsValidator 链 :563-565 → resolveSubjects :156 → balanceTotals :158-159 → assertBalanced :160 → 多账套循环 persistVoucher :164-191） | ✅ |
| 红字冲销编排 | `ErpFinPostingProcessor.java` reverseProcess:217-270（findAllPostedVouchers :223-224 → buildReversalDraft :239 → persistVoucher REVERSAL :241-242 → markOriginalVoucherReversed :252 → dispatchReversalEvent :253-254） | ✅ |
| 幂等前置 | `ErpFinPostingProcessor.java` alreadyPosted:485-497（billR 反查 + POSTED + !isReversed + acctSchemaId 四重过滤） | ✅ |
| 期间门控 | `ErpFinPostingProcessor.java` resolveOpenPeriod:508-529（仅 OPEN 放行，否则 `ERR_PERIOD_CLOSED`） | ✅ |
| 借贷平衡 | `ErpFinPostingProcessor.java` balanceTotals:722-734 / assertBalanced:736-742（不平衡抛 `ERR_UNBALANCED`） | ✅ |
| 凭证写库 | `ErpFinPostingProcessor.java` persistVoucher:783-870（voucher save :797-814 / line loop :822-855 / billR save :857-867） | ✅ |
| 红字凭证构造 | `ErpFinPostingProcessor.java` buildReversalDraft:744-774（金额取负 negDebit/negCredit :752-753） | ✅ |
| 原凭证补标 | `ErpFinPostingProcessor.java` markOriginalVoucherReversed:933-947（仅标记 NORMAL+POSTED+!isReversed :939-942） | ✅ |
| 已过账凭证反查 | `ErpFinPostingProcessor.java` findAllPostedVouchers:890-906（过滤 `postingType==NORMAL||null` :899-900，阻断红字凭证再红冲） | ✅ |
| Provider 注册中心 | `ErpFinPostingProcessor.java` resolveProvider:499-506 → `ErpFinAcctDocRegistry.java` init:45-79（非默认优先 + 重复 fail-fast :55-60 / 默认填充 :64-71）/ getProvider:81-83 O(1) | ✅ |
| FactsValidator 集合 | `ErpFinAcctDocRegistry.java` getValidators:85-87（按 getOrder 升序 :76-78）/ 调用点 `ErpFinPostingProcessor.java` generateFacts:562-565 | ⚠️ **死代码**（见 §2.3） |
| 多账套传播 | `ErpFinPostingProcessor.java` schemaPropagator.resolveTargetSchemas 调用 :134 → `SchemaPropagator.java` resolveTargetSchemas:44 + translateFactsForSchema 调用 :176 → `ErpFinPostingProcessor.java` translateFactsForSchema:665-720 | ✅ |
| 冲销事件派发 | `ErpFinPostingProcessor.java` dispatchReversalEvent:376-401（SYNC 默认 / ASYNC afterCommit :388-394） | ✅ |
| 跨域执行器 | `FinPostingExecutor.java`（postEvent/reverse 经 `IErpFinVoucherBiz`，per plan baseline） | ✅ |

### 2.2 多币种路径（UC-FIN-12）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| 上下文汇率填充 | `ErpFinPostingProcessor.java` prepareContext:537（`event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT`） | ⚠️ **静默回退 rate=1**（见 §5 UC-FIN-12） |
| 凭证行汇率填充 | `ErpFinPostingProcessor.java` persistVoucher:817-820（ctx.getExchangeRate 回退 `EXCHANGE_RATE_DEFAULT`） | ⚠️ 同上 |
| 行级双金额 | `ErpFinPostingProcessor.java` persistVoucher:826-828（`amtSource = fact.getAmountSource() != null ? ... : amt` / `amtFunctional = fact.getAmountFunctional() != null ? ... : amt`） | ⚠️ 单币种回退（amountSource==amountFunctional，无 FX 折算） |
| 默认汇率常量 | `ErpFinPostingProcessor.java` EXCHANGE_RATE_DEFAULT:78（`new BigDecimal("1")`） | ✅ 存在 |
| 期末 FX 重估 | `ExchangeRevaluationService.java` revalue（期末 AR/AP + 银行存款重估→`EXCHANGE_GAIN_LOSS`，per plan baseline + MA2 §5.11/§5.12 复核） | ✅（断言 3） |

### 2.3 科目分摊路径（UC-FIN-04/15）— **机制完全缺失**

| 期望组件 | 实仓核实结果 | 证据 |
|---|---|---|
| `ErpFinGlDistributionValidator`（IErpFinFactsValidator 实现） | **类不存在** | `rg "implements IErpFinFactsValidator"` 全仓零命中；接口 `IErpFinFactsValidator.java:11-22` 仅声明 `validate`/`getOrder` 两方法，**无任何业务实现 Bean**（接口仅被 `ErpFinAcctDocRegistry:31,35,41,76-78` 作为集合元素类型引用） |
| `ErpFinGlDistribution` 规则实体（ORM） | **实体不存在** | `rg "ErpFinGlDistribution" --glob "*.orm.xml"` 零命中；`module-finance/model/app-erp-finance.orm.xml` 无该实体声明 |
| FactsValidator 调用循环 | **死代码**（无 Delta 注入） | `ErpFinPostingProcessor.java:563-565` `for (IErpFinFactsValidator validator : registry.getValidators()) { facts = validator.validate(facts, ctx); }`——`registry.getValidators()` 在无实现 Bean 时返回空列表，循环体永不执行 |
| Σ percent != 100 拒绝过账 | **未实现** | 上述死代码循环无任何 percent 校验逻辑 |
| 拆行平衡 | **未实现** | 同上 |
| "科目分摊" UI 菜单 | **仅 UI 资源存在** | `module-finance/erp-fin-web/.../auth/erp-fin.action-auth.xml:189-196`（`fin-distribution` / `gl-distribution` 资源 + `app:useCases="UC-FIN-04,UC-FIN-15"`），但无后端实体/服务支撑 |

**结论**：UC-FIN-04/15 的整条验收链（GlDistribution 规则实体 → ErpFinGlDistributionValidator Bean → FactsValidator 链注入 → 拆行 + Σpercent 校验 → 平衡保持）**在 L3 完全无实现证据**。仅 L2 owner doc（`cost-center.md:64` / `posting.md §FactsValidator`）与 UI 菜单资源有设计声明。

---

## 3. 测试证据（L4，注明断言强度）

> 断言强度分档引用 MA5（`docs/audits/2026-07-29-1430-arm-ma5-finance-test-coverage.md`）评级口径：强断言 = 断言验收标准数值/状态；弱断言 = 仅断言不抛异常或仅冒烟。

| UC | 测试引用 | 断言强度 | 覆盖判定 |
|---|---|---|---|
| UC-FIN-01 | `TestErpFinPostingService.java#testPostHappyPath:73-104` | **强** | ✅ 断言 docStatus=POSTED / 借贷合计 113 / 3 行分录 / 1 条 billR 回链 |
| UC-FIN-01（幂等） | `TestErpFinPostingService.java#testPostIdempotent:107-130` | **强** | ✅ 重复过账返回 null + 不产生第二张回链 |
| UC-FIN-01（不平衡拒绝） | `TestErpFinPostingService.java#testPostUnbalancedRejected:133-152` | **强** | ✅ 抛 NopException + 0 回链 |
| UC-FIN-01（期间关闭拒绝） | `TestErpFinPostingService.java#testPostPeriodClosedRejected:155-173` | **强** | ✅ 抛 NopException + 0 回链 |
| UC-FIN-02 | `TestErpFinPostingService.java#testReverse:176-227` | **强** | ✅ 断言红字凭证 isReversed=true / reversalOfVoucherId / POSTED / 借贷为负 / 原+红净额为 0 / 反查可达 / 回链 2 条 |
| UC-FIN-02（无源凭证） | `TestErpFinPostingService.java#testReverseNotFound:230-234` | **强** | ✅ 抛 NopException |
| UC-FIN-03 | `TestErpFinAcctDocRegistry`（per baseline：fallback/域优先级 + 重复 fail-fast） | **强** | ✅ 路由表构建 + 冲突 fail-fast + fallback 填充 |
| UC-FIN-04/15 | **无测试**（类不存在） | — | ❌ 零覆盖 |
| UC-FIN-12（多币种行级） | `TestErpFinPostingService.java#testMultiCurrencyPostingLineLevelAssertions:250-283` | **强（断言回退行为，非需求行为）** | ⚠️ 断言 `amountSource == amountFunctional == 源币金额`（单币种回退，**与 UC-FIN-12 断言 1「本位币金额 == 源币 × 汇率」冲突**）；javadoc:243-247 自述「完整多币种源币金额迁移为 documented successor」 |
| UC-FIN-12（汇率缺失拒绝） | **无测试**（实现是回退 rate=1 而非拒绝） | — | ❌ 需求要求的「拒绝过账」路径零覆盖 |
| UC-FIN-12（EXCHANGE_GAIN_LOSS） | `TestErpFinExchangeRevaluation`（per baseline + MA2 §5.11） | **强** | ✅ 期末重估→`EXCHANGE_GAIN_LOSS(130)` 凭证 |

**测试证据汇总**：UC-FIN-01/02/03 强断言覆盖；UC-FIN-04/15 零覆盖（实现缺失）；UC-FIN-12 断言 1 的测试断言的是**与需求相反的回退行为**，断言 2「汇率缺失拒绝」零覆盖。

---

## 4. 运行时行为证据（L5，复用 MA2/E2E + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实的状态机/链路行为直接引用，**不重新核实行为本身**；本切片只补"需求契约↔实际行为"差异。

### 4.1 复用 MA2 已证实行为（`2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`）

| MA2 已证实行为 | 引用 | 本切片复用判定 |
|---|---|---|
| DRAFT→POSTED 经 persistVoucher 直写（自动过账不经 DRAFT） | MA2 §5.2 + §5.9 场景A | ✅ 复用（UC-FIN-01 行为链路证实） |
| 红字凭证 postingType=REVERSAL 被 findAllPostedVouchers 过滤 → 无限循环阻断 | MA2 §5.5 | ✅ 复用（UC-FIN-02 红冲链安全性证实） |
| 8 域 posted 反写经域调用方；ASYNC listener 失败入异常工作台 | MA2 §5.7 + §5.9 场景C/D | ✅ 复用（UC-FIN-02 断言「业务单据.posted=false」经域 listener 回写证实） |
| 幂等键 (billHeadCode, businessType) 经 billR 反查 + alreadyPosted 排除已红冲凭证 | MA2 §5.4 + §5.9 场景F | ✅ 复用（UC-FIN-01 幂等证实） |
| 跨域 grep `daoFor(ErpFinVoucher` 在 finance 外零命中 → 无域绕过引擎直写凭证 | MA2 §5.7（P0-MA1-021 sustained done） | ✅ 复用（UC-FIN-01 业财回链统一路径证实） |

### 4.2 本切片需求视角差异增量（MA2 未覆盖）

| 差异点 | MA2 视角 | RC 视角（需求契约） | 本切片裁决 |
|---|---|---|---|
| 科目分摊（GlDistribution） | MA2 未审查（状态机维度无此对象） | UC-FIN-04/15 明确要求「拆行 + Σpercent!=100 拒绝 + 平衡保持」 | **P1-RC-001**（机制完全缺失，§5 详述） |
| 汇率缺失处理 | MA2 §5.12 仅述「line 级无 FX 折算，状态机不因币种失败」，归 P1-MA2-002/009 MR1 | UC-FIN-12 断言 2 明确要求「汇率缺失→报错拒绝过账」，实现是**静默回退 rate=1 继续过账** | **P1-RC-002**（守卫未实现，§5 详述） |

### 4.3 E2E 行为证据（复用）

- `tests/e2e/business-actions/finance-voucher-post.action.spec.ts` / `fin-gl-mapping-routing.action.spec.ts`：过账主路径 E2E（per plan baseline，断言强度引用 A5.6 评级）。
- 跨域链：`tests/e2e/orchestration/p2p-chain.spec.ts` / `o2c-chain.spec.ts`（业财端到端过账链，MA2 §5.9 场景 A/D 行为背书）。
- 本切片无新 E2E 探针需求（存疑点见 §7）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 符合性结论，§2 判据）

### 5.1 五级追踪矩阵（6 UC，每 UC 一行，不合并）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-FIN-01** 业财自动过账 | `use-cases.md:16` ①存在凭证(businessType+来源单号) ②借贷平衡 ③VoucherBillR 双向回链 ④单据.posted=true | `posting.md §过账引擎/§业财回链`（设计参考，与 L1 一致） | `ErpFinVoucherBizModel.post:72-74` → `ErpFinPostingProcessor.process:127-212`（路由 :143-146 / 平衡 :158-160 / persistVoucher :180-182 写 voucher+line+billR :797-867） | `TestErpFinPostingService#testPostHappyPath:73-104`（强：4 断言全覆盖）+ `#testPostIdempotent` / `#testPostUnbalancedRejected` / `#testPostPeriodClosedRejected` | MA2 §5.9 场景A/F 证实（billR 反查幂等 + 跨域零绕过） | **接受**（4 验收标准 L3-L5 全证据一致） |
| **UC-FIN-02** 业务单据作废触发红字冲销 | `use-cases.md:42` ①billR 反查 ②红字凭证(金额取负+关联原凭证) ③原凭证 isReversed=true ④单据.posted=false ⑤红字凭证走 DRAFT→POSTED | `posting.md §冲销机制` + `state-machine.md`（设计参考；红字凭证终态归属未定义归 P1-MA2-031，非本切片范围） | `ErpFinVoucherBizModel.reverse:80-84` → `ErpFinPostingProcessor.reverseProcess:218-270`（findAllPostedVouchers :223 / buildReversalDraft 取负 :744-774 / persistVoucher REVERSAL :241 / markOriginalVoucherReversed :252 / dispatchReversalEvent :253）；断言④由域 listener 经 VoucherReversedEvent 回写（dispatchReversalEvent:376-401） | `TestErpFinPostingService#testReverse:176-227`（强：红字凭证 isReversed/reversalOfVoucherId/POSTED/负数/净额 0/反查可达/回链 2 条）+ `#testReverseNotFound` | MA2 §5.9 场景D 证实（8 域 reversal writeback 测试矩阵） | **接受**（5 验收标准 L3-L5 全证据一致；断言④经域 listener 模式，MA2 背书） |
| **UC-FIN-03** 可插拔 Provider 路由 | `use-cases.md:60` ①新增 Provider Bean 注册 businessType ②Registry 自动聚合(@Inject List) ③该 Provider 被路由调用 ④核心引擎零改 | `posting.md §过账引擎(可插拔)`（设计参考，与 L1 一致） | `ErpFinAcctDocRegistry.java` init:45-79（List 注入 :37/41 + EnumMap :47-74 + 非默认优先 :49-62 + 重复 fail-fast :55-60 + 默认填充 :64-71）/ getProvider:81-83 O(1)；`ErpFinPostingProcessor.resolveProvider:499-506` 经 registry.getProvider 路由 | `TestErpFinAcctDocRegistry`（强：fallback/域优先级 + 重复 fail-fast，per baseline） | MA2 §5.7 证实（38 Provider 一致经引擎路径，零绕过） | **接受**（4 验收标准 L3-L5 全证据一致；可插拔机制通用，PROJECT_SETTLEMENT 为示例 businessType） |
| **UC-FIN-04** FactsValidator 科目分摊 | `use-cases.md:76` ①命中 GlDistribution 规则拆多行 ②Σ拆分行==原行(平衡) ③Σpercent!=100 抛异常拒绝 | `posting.md §FactsValidator` + `cost-center.md §ErpFinGlDistribution:64`（设计参考，**与实现冲突——以 L1 为准**） | **机制完全缺失**：`ErpFinGlDistributionValidator` 类不存在（`rg implements IErpFinFactsValidator`=0）；`ErpFinGlDistribution` ORM 实体不存在（`rg *.orm.xml`=0）；FactsValidator 调用循环 `ErpFinPostingProcessor:563-565` 为死代码 | **无测试**（类不存在） | 无（L3 无实现，L5 无行为可证） | **P1**（§2 P1① 功能完全缺失）→ **P1-RC-001** |
| **UC-FIN-12** 多币种过账 | `use-cases.md:223` ①本位币金额==源币×汇率 ②汇率缺失→报错拒绝过账 ③外币对账考虑汇兑损益 EXCHANGE_GAIN_LOSS | `posting.md §多币种`（设计参考，**与实现冲突——以 L1 为准**） | 断言①：`persistVoucher:826-828` 双金额回退到 amount（无 FX 折算）→ 已归 P1-MA3-039 / P1-MA2-002/009；断言②：`prepareContext:537` + `persistVoucher:817-820` 静默回退 `EXCHANGE_RATE_DEFAULT=1`（:78）**而非拒绝**；断言③：`ExchangeRevaluationService.revalue` 存在（期末重估→EXCHANGE_GAIN_LOSS） | 断言①：`#testMultiCurrencyPostingLineLevelAssertions:250-283`（强，但断言的是**回退行为** amountSource==amountFunctional==源币，与需求冲突）；断言②：**无测试**；断言③：`TestErpFinExchangeRevaluation`（强） | MA2 §5.12 证实 line 级无 FX 折算（状态机不因币种失败） | **P1**（断言① REUSE P1-MA3-039/MA2-002/009；断言② §2 P1② 异常路径未实现 → **P1-RC-002**；断言③ 接受。取最高=P1） |
| **UC-FIN-15** 科目分摊(GL Distribution) | `use-cases.md:298` ①命中规则拆行 ②Σ拆分行==原行 ③Σpercent!=100 拒绝 ④由 ErpFinGlDistributionValidator(IErpFinFactsValidator 实现)执行 ⑤getOrder() 较高 | `cost-center.md §ErpFinGlDistribution:64,81` + `posting.md §FactsValidator`（设计参考，**与实现冲突——以 L1 为准**） | 同 UC-FIN-04：`ErpFinGlDistributionValidator` 类 + `ErpFinGlDistribution` 实体均不存在；死代码循环 :563-565 | **无测试** | 无 | **P1**（§2 P1① 功能完全缺失，与 UC-FIN-04 同根因 D-02）→ 归 **P1-RC-001**（不重复计 ID） |

### 5.2 分级判据命中明细（§2）

#### P1-RC-001 — UC-FIN-04/15 科目分摊机制完全缺失

- **命中判据**：§2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」
- **三源对照**：
  - L1（`use-cases.md:76,298`）：明确要求 GlDistribution 规则拆行 + Σpercent!=100 拒绝 + 平衡保持 + ErpFinGlDistributionValidator Bean。
  - L2（`cost-center.md:64` + `posting.md §FactsValidator`）：设计声明 ErpFinGlDistributionValidator implements IErpFinFactsValidator + getOrder() 较高——**L2 与 L1 一致，但均与实现冲突，以 L1 为准**（§4 冲突裁决规则）。
  - L3（实仓）：Validator 类 + 规则实体均不存在；FactsValidator 调用循环为死代码。
- **运行时影响**：科目分摊功能零可用——配置的分摊规则不生效，凭证行不拆分，percent!=100 不拒绝。**不破坏现有数据**（无错误分摊），但需求要求的功能完全缺失。
- **严重性**：major（功能完全缺失，但非活跃数据破坏——分摊未启用时主路径不受影响）
- **修复义务**：§5 Q4=(a) 强制实现，禁止方案 B。经 MR1（R1.0 展开为 RC-R1.n）。修复触及会计过账逻辑（新增 Validator 注入 FactsValidator 链）+ 可能新增 ORM 实体（ErpFinGlDistribution）→ **须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账逻辑 + ORM 结构变更两类）。
- **与既有 finding 关系**：grep arm-index 无同域同控制点 finding（科目分摊主题）。**新建 P1-RC-001**（§7 裁决详见 §6）。

#### P1-RC-002 — UC-FIN-12 汇率缺失未拒绝过账（静默回退 rate=1）

- **命中判据**：§2 **P1②**「需求契约要求的异常路径未实现」（汇率缺失→拒绝过账是异常路径守卫）
- **三源对照**：
  - L1（`use-cases.md:230`）：逐字「若 汇率缺失 → 报错拒绝过账」。
  - L2（`posting.md §多币种`）：设计参考。
  - L3（`ErpFinPostingProcessor.prepareContext:537` + `persistVoucher:817-820`）：实现为 `event.getExchangeRate() != null ? event.getExchangeRate() : EXCHANGE_RATE_DEFAULT`（EXCHANGE_RATE_DEFAULT=`new BigDecimal("1")` :78）——**静默回退到 1 继续过账，与「拒绝」直接冲突**。
- **运行时影响**：当域调用方在构造外币 PostingEvent 时漏传 exchangeRate，引擎以 rate=1 静默过账 → 凭证行 amountFunctional=amountSource（本位币金额错误）→ GL 本位币余额错误。**触发面依赖域调用方漏传**（非默认活跃路径；当前各域 Provider 均显式传 rate），故不构成 §2 P0④「活跃数据破坏」（与 P0 示例「期间 CLOSED 后禁止过账但实际可过」的默认触发面不同）。
- **严重性**：major（会计过账正确性潜在破坏，但触发需前置条件；与 MA2 §5.12 对多币种路径的 P1 分级一致）
- **P0 升级评估**：经评估**维持 P1 不升 P0**。理由：(1) 触发需域调用方漏传 rate（caller bug），非默认路径；(2) 当前各域 Provider 显式传 rate（`NotesReceivableAcctDocProvider` 等 per baseline），无活跃错误数据；(3) MA2 §5.12 已对同代码路径定 P1，本切片无新活跃破坏证据，重定 P0 与既有分级矛盾。
- **修复义务**：§5 Q4=(a) 强制实现。经 MR1。修复触及会计过账逻辑（prepareContext 加 `event.getExchangeRate()==null && currencyId != functionalCurrency` 抛异常）→ **须 ask-first + 独立 plan-audit**（§5 保护区域，会计过账逻辑类）。
- **与既有 finding 关系**：P1-MA3-039（persistVoucher amountSource=amountFunctional）覆盖 UC-FIN-12 **断言 1**（FX 折算缺失）；本 finding 覆盖 **断言 2**（rate 缺失守卫未实现）——**不同控制点**（折算 vs 守卫），§7 裁决=新建 P1-RC-002 + 交叉引用 P1-MA3-039。

### 5.3 接受类 UC 结论汇总

| UC | 接受依据 |
|---|---|
| UC-FIN-01 | 4 验收标准 L3（process:127-212 + persistVoucher:783-870 + billR:857-867）+ L4（testPostHappyPath 强断言）+ L5（MA2 场景A/F）全一致 |
| UC-FIN-02 | 5 验收标准 L3（reverseProcess:218-270 + markOriginalVoucherReversed:933-947 + dispatchReversalEvent:376-401）+ L4（testReverse 强断言）+ L5（MA2 场景D 8 域 reversal）全一致；断言④ posted=false 经域 listener 模式（posting.md §反写契约 + 引擎 javadoc:59-60 明示） |
| UC-FIN-03 | 4 验收标准 L3（ErpFinAcctDocRegistry:45-83 List 注入 + EnumMap + fail-fast + fallback）+ L4（TestErpFinAcctDocRegistry）+ L5（MA2 §5.7 38 Provider 一致）全一致 |
| UC-FIN-12 断言③ | EXCHANGE_GAIN_LOSS 期末重估经 `ExchangeRevaluationService.revalue` 实现 + `TestErpFinExchangeRevaluation` 强断言（接受） |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 产出 finding 前已 grep `arm-index.md` finance 过账同域同控制点。裁决遵循 §7 规则。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本切片 finding 关系 | 裁决 |
|---|---|---|---|
| P1-MA2-001 GRNI 自动冲回缺失 | receive→invoice 暂估应付双计未自动清理 | 不同根因（核销/冲回 vs 分摊）+ 不同控制点 | 不相关 |
| P1-MA2-002 / P1-MA2-009 多币种 P2P/O2C | line 级无 FX 折算 + 收款核销汇兑损益未实现 | 覆盖 UC-FIN-12 **断言 1**（FX 折算） | **复用**（UC-FIN-12 断言 1 引用，不新建） |
| P1-MA3-039 persistVoucher amountSource=amountFunctional | 双金额回退（通用模板路径单币种回退） | 覆盖 UC-FIN-12 **断言 1**（owner-doc drift 视角） | **复用**（UC-FIN-12 断言 1 引用） |
| P1-MA2-031 / P1-MA2-032 凭证状态机 | CANCELLED 不可达 / IGNORED 悬挂 | 不同维度（状态机 vs 需求契约分摊/汇率守卫） | 不相关 |
| P0-MA2-016 / P0-MA2-018 | FX 损益结转 / billR UK | 不同控制点 | 不相关 |

### 6.2 新建 finding 裁决

| Finding ID | UC | 根因/控制点 | 与既有 finding 差异依据 | 裁决 |
|---|---|---|---|---|
| **P1-RC-001** | UC-FIN-04/15 | 科目分摊（GlDistribution）机制完全缺失 | arm-index 无任何 finding 覆盖科目分摊主题；既有 P1 均为多币种/核销/状态机/UK 等不同控制点 | **新建** |
| **P1-RC-002** | UC-FIN-12 断言 2 | 汇率缺失守卫未实现（静默回退 rate=1 而非拒绝） | P1-MA3-039 覆盖断言 1（折算缺失，doc-drift 视角）；本 finding 覆盖断言 2（rate 缺失守卫，需求契约视角）——**不同控制点**（折算逻辑 vs 缺失守卫），不可合并 | **新建**（交叉引用 P1-MA3-039） |

### 6.3 双向可追溯

- **新 finding → arm-index**：P1-RC-001 / P1-RC-002 将写入 `arm-index.md` MA1 RC finding 分区（§7 归档纪律）。
- **finding → 修复**：两 finding 均待 MR1 R1.0 展开为 RC-R1.n 修复行（本审计不实施修复）。
- **既有 finding 复用注记**：UC-FIN-12 断言 1 引用 P1-MA2-002/009 + P1-MA3-039（不新建编号）。

---

## 7. 静态存疑点清单（供 MA4 A4.1 展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行；无则注明。

1. **UC-FIN-02 断言④「业务单据.posted=false」域 listener 实际回写覆盖率**：L3 证实 `dispatchReversalEvent:376-401` 派发 `VoucherReversedEvent`，MA2 §5.9 场景D 证实 8 域 reversal writeback 测试矩阵。但「posted=false 是否在所有 8 域 listener 中一致回写」属逐域运行时行为，本切片仅引用 MA2 证实，未逐域核验 listener 实现——交 MA4 A4.1（业财展开器，Deps=MA1 done）按需追加 A4.1.n 实体行展开运行时验证。
2. **UC-FIN-12 汇率缺失触发面实测**：L3 静态确认回退逻辑（prepareContext:537），但「当前各域 Provider 是否在所有外币场景显式传 rate」属运行时调用面普查——交 MA4 A4.1 按需展开（grep 各域 `PostingEvent.setExchangeRate` 调用点）。
3. **UC-FIN-03 PROJECT_SETTLEMENT businessType 是否已有 Provider 注册**：L3 证实可插拔机制通用，但 UC 文本以 PROJECT_SETTLEMENT 为例，该具体 businessType 是否已有 `IErpFinAcctDocProvider` Bean 注册属实例普查——交 MA4 A4.1 按需展开（rg `getSupportedBusinessTypes` 含 PROJECT_SETTLEMENT 的 Provider）。

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（P1-RC-001/002 均为 P1），按 §10 **不触发 MR0**。两 finding 经 MR1 批量修复通道（R1.0 展开为 RC-R1.n）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。

### checker actual vs baseline 实测表（2026-08-02 实测）

> 本审计为**只读审计**（无生产代码变更），故 checker 无回归风险；actual vs baseline 实测记录如下（基线源 `compliance-baseline.md §BASELINE (machine-readable)`）。

| 规则 | Baseline | Actual | 状态 |
|------|----------|--------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
| R1d | 14 | 14 | ✅ |
| R2a | 34 | 34 | ✅ |
| R2b | 229 | 229 | ✅ |
| R2c | 1382 | 1382 | ✅ |
| R2d | 34 | 34 | ✅ |
| R3 | 5 | 5 | ✅ |
| R4/R5 | 0/0 | 0/0 | ✅ |
| R6 | 2 | 2 | ✅ |
| R7 | 0 | 0 | ✅ |
| R8 | 0 | 0 | ✅ |
| R10 | 6 | 6 | ✅ |
| R11 | 0 | 0 | ✅ |
| R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

全 19 规则 actual ≤ baseline，**0 漂移**。本审计无生产代码变更，无回归风险。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

本切片声明与既有 MA2 报告的差异增量：

- **复用 MA2 已证实行为**（不重新核实）：
  - `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`：会计凭证状态机（DRAFT/POSTED/CANCELLED + isReversed + postingType 三轴）+ 9 控制点 + 11 项 MA1/MA2 finding 运行时复核（P0-MA1-021 done / P0-MA2-016 fix in place / 余无升级）。本切片 UC-FIN-01/02/03 的 L5 行为证据直接引用该报告 §5.9 场景 A/D/F + §5.5（无限循环阻断）+ §5.7（跨域零绕过）。
  - `2026-07-28-2130-arm-ma4-finance-posting-voucher-code-quality.md`：过账引擎代码质量（异常类型/N+1/索引），本切片未重复审查代码质量维度。
  - `2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md` / `...-order-to-cash-e2e.md` / `...-period-close-e2e.md`：跨域过账链 E2E 行为，本切片 UC-FIN-02 断言④域 listener 回写引用。
- **本切片只补的需求视角差异**（MA2 未覆盖）：
  1. **UC-FIN-04/15 科目分摊缺失**（P1-RC-001）：MA2 状态机/代码质量维度均无此对象（GlDistribution 不存在于代码），本切片从需求契约视角首次定级。
  2. **UC-FIN-12 汇率缺失守卫未实现**（P1-RC-002）：MA2 §5.12 仅述「line 级无 FX 折算」归 P1-MA2-002/009，未单独审视「汇率缺失→拒绝过账」守卫；本切片从 UC-FIN-12 断言 2 逐字对照，定级守卫未实现为独立 P1。
- **MA2 finding 复核无升级**：本切片复核 MA2 已登记的 11 项 finance 过账 finding（P0-MA1-021 / P0-MA2-016 / P1-MA1-018/022 / P1-MA2-001/002/009/021/022 / P2-MA1-019 / P2-MA2-025），运行时行为与 MA2 登记一致，**无升级 P0**（对齐 MA2 §6 结论）。

---

## 10. Verdict

**Verdict: passes requirement-compliance audit**（带 2 项 P1 残留 + 4 UC 接受）

**审查范围**：UC-FIN-01/02/03/04/12/15 共 6 UC 五级追踪矩阵（L1-L5）+ 每 UC 符合性结论（§2 判据）+ 与 arm-index 衔接（§7 复用/新增裁决）+ 静态存疑点清单（供 MA4 A4.1 展开）+ 过程纪律自检 + 与 MA2 差异增量声明。

**接受类**：UC-FIN-01/02/03 全验收标准 L3-L5 一致；UC-FIN-12 断言 3 接受。

**P1 残留**：P1-RC-001（科目分摊缺失）/ P1-RC-002（汇率缺失守卫未实现）→ MR1（R1.0 展开为 RC-R1.n），修复触及会计过账逻辑/ORM 须 ask-first + 独立 plan-audit（§5）。

**P0**：无。不触发 MR0。

**剩余风险**：见 §7 静态存疑点清单（3 项交 MA4 A4.1 运行时展开）。
