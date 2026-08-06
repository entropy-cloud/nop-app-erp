# RC MA4 A4.1.14 — UC-FIN-14 断言⑤ config key 孤儿化运维认知影响面 验证报告

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 运行时行为验证）
> 工作项：A4.1.14（MA4 运行时行为验证 — A1.4 §7-4：UC-FIN-14 断言⑤ config key `erp-fin.bank-recon-auto-reverse-next-month` 默认 true 但无 scheduler/cron/Job bean 消费的运维认知，关联 P1-RC-005）
> 验证 plan：`docs/plans/2026-08-07-1400-2-rc-ma4-a4-1-14-bank-recon-auto-reverse-config-orphan-awareness.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据[含 P0④活跃数据破坏 / P0①] / §4 Q1 真相源层级与冲突裁决 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0 即时通道 / §去重协议）
> 输入存疑点：A1.4 §7 存疑点 4（`docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md:328`）
> 输入 finding：`P1-RC-005`（A1.4 §5.2 / §6，UC-FIN-09 断言④ / UC-FIN-14 断言⑤ 下月初自动红冲缺失——config key 无 scheduler 消费）
> 关联 finding：`P1-RC-004`（对方账号匹配维度缺失，A4.1.11 done 同 §7-1 家族）/ `P2-RC-001`（跨多条 statement refNo 去重，A4.1.13 done 同 §7-3 家族）/ `P2-RC-002`（valueDate→transactionDate 简化，watch-only）/ MA2 银行对账解耦既有行为（`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`）
> 关联同型工作项：A4.1.7（`2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`，done — config 默认 off 部署普查同型范式，本验证复用其「config-default vs deployment 覆盖」判据框架，但方向相反：A4.1.7 config 默认 off 探「是否隐式启用」，A4.1.14 config 默认 true 探「是否被消费」）+ A4.1.11（`2026-08-06-1044-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`，done — A1.4 §7-1 同型部署/运维面普查范式）
> 验证性质：**只读部署/运维认知影响面评估**（读 config key 定义 + 全量普查 scheduler.yaml / nop-batch job.yaml / app-service.beans.xml 消费点 + 读部署/运维文档 + 引用 MA2/A1.4 + config 孤儿化运维认知影响面推理；不改代码/ORM/api.xml/config 默认值/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 验证日期：2026-08-07
> 验证者：主代理（独立结束审计由独立子代理执行，见 plan §Closure）

---

## 0. 验证结论（TL;DR）

| 项 | 结果 | 处置 |
|---|---|---|
| **P1-RC-005 分级再评估** | **维持 P1（不升 P0，不降 P2）** | 不触发 MR0 即时通道，归 MR1（R1.0→RC-R1.n） |
| config 消费点全集 | **0 处消费**（config key `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH` 定义于 `ErpFinConstants.java:289` 默认 true，全仓 grep 仅 1 命中 = 定义本身；零 scheduler / cron / Job bean / IJobInvoker / nop-batch job.yaml / `.batch.xml` 消费） | CONFIRMED 孤儿化（A1.4 §2/§5.2 复核为全集） |
| 调度配置全集普查 | scheduler.yaml（`enabled: true`，零银行对账条目）+ 全 19 个 `.job.yaml`（无 `erp-fin-bank-recon-auto-reverse`）+ 全 9 个 `.batch.xml`（无 bank-recon-auto-reverse）+ app-service.beans.xml（仅手动组件，无 Job bean） | 零调度接线确认 |
| 部署/运维认知影响面 | **部分缓解（asymmetric）**：调度架构层 `job-scheduling.md:110-111` **诚实**登记 `erp-fin-bank-recon-adj-reverse` 为 DESIGN（待实现）+ L2 `bank-reconciliation.md:150` schema 补注显式承认「实际红冲由定时任务触发，本计划交付 reverse 入口 + 手动可触发」；**但** config key 默认 true 本身对仅查 config 的运维具**残余误导面**（config 名暗示生效 + 默认 true 暗示启用） | 残余误导存在但不升 P0（架构文档诚实 + 手动补救存在） |
| 手动补救有效性 | `BankReconciliationBuilder.reverse:133-142`（POSTED→CANCELLED + 调 adjustmentVoucherBuilder.reverse）+ `BankReconAdjustmentVoucherBuilder.reverse:97-102`（hasAdjustmentVoucher guard + voucherBiz.reverse） | 手动 reverse 入口运行时有效（出纳可补救恢复正确余额） |
| 新 finding | **0** | 无新控制点（全部归既有 P1-RC-005） |
| MR0 触发 | **无** | — |

**整体裁决**：A1.4 §7 存疑点 4（「config key 默认 true 但无消费的运维认知」）经 config 消费点全集普查 + 调度配置文件普查 + 部署/运维文档声称面评估 + 手动补救有效性核验 **CONFIRMED 维持 P1-RC-005 = P1**。config key `erp-fin.bank-recon-auto-reverse-next-month`（`ErpFinConstants.java:289`，默认 true）**经全量普查确认零消费**——跨 module-finance + app-erp-all grep（config key + auto-reverse/AutoReverse/autoReverse/bank-recon-auto-reverse/AutoReverseNextMonth/auto-reverse-next-month 变体全集）**仅 1 命中 = 定义本身**；调度配置文件全集（`scheduler.yaml` + 全 19 `.job.yaml` + 全 9 `.batch.xml` + `app-service.beans.xml`）**零银行对账红冲作业条目**——核心 config 孤儿化主张 CONFIRMED。经 §2 分级判据三源复核：①**P0 不成立**——P0④「会计过账正确性破坏」不成立（错误限于未达调整凭证不自动跨期还原致银行存款 + 未达调整对方科目余额潜在错报，但 GL 过账本身正确 + 对账子系统与过账解耦[MA2 :48,223,365]）；P0①「活跃数据破坏」不成立（需出纳遗漏手动 reverse 触发，非默认活跃路径每次操作即破坏；与 P0 示例「凭证重复过账」默认触发面不同）；**且 config 孤儿化的运维误导面经评估不构成 P0**——调度架构层 `job-scheduling.md §3.1:110-111` **诚实**登记 `erp-fin-bank-recon-adj-reverse` 为 DESIGN（待实现），L2 `bank-reconciliation.md §schema 补注:150` 显式承认「实际红冲由定时任务触发，本计划交付 `reverse` 入口 + 手动可触发」，故**架构/设计文档层不存在「自动红冲生效」的虚假声称**；残余误导面仅限于「config 名 + 默认 true 对仅查 config 的运维」的局部不对称（未上升到 P0 隐性致会计余额错报且无补救的判据——因补救存在）。②**P2 不适用**——L1 UC-FIN-14:288 逐字「下月初**自动**红冲(跨期还原)」显式「自动」是**主验收标准**（非次要边界），config 孤儿化属 §2 P1①「功能完全缺失」（"自动"红冲的调度功能完全缺失），非 §2 P2①「次要验收标准未完全满足」。③**分层一致**：与 A1.4 §5.2 P1-RC-005 P1 结论分层一致；与 arm-index `:130` P1-RC-005 行衔接——本验证**确认维持 P1**，不升 P0 不降 P2。**不触发 MR0 即时通道，不升 P0，不降 P2，无新 finding。** P1-RC-005 修复仍归 MR1（R1.0→RC-R1.n）：修复 = 接线 scheduler（nop-batch `job.yaml` 注册下月初红冲作业 + 消费 config key 门控 + 批量调 `BankReconciliationBuilder.reverse`），纯调度接线 + BizModel 调用，按 roadmap 预授权类目（代码逻辑修复）可自动执行，**不触发 §5 ask-first**（不触及 ORM/会计过账核心路径 VoucherFact/PostingProcessor，仅调用既有 reverse 入口）。本验证**不实施修复**（plan Non-Goals）。

> **与 A4.1.7 方向差异声明**：A4.1.7（承付 release-on-return）config 默认 **false**，探「部署是否隐式启用」（保守方向偏移）；本验证 A4.1.14 config 默认 **true** 但**零消费**，探「运维是否误以为生效」（孤儿化隐性失效）。两者判据框架互补但方向相反：A4.1.7 = config-off-but-maybe-enabled；A4.1.14 = config-on-but-no-consumer。后者误导面更隐蔽（config 名 + 默认 true 暗示生效），但因架构文档诚实登记 DESIGN + 手动补救存在，未升 P0。

---

## 1. 输入存疑点原文 + L1/L2/L3 锚点

### 1.1 输入存疑点原文（A1.4 §7 存疑点 4，逐字引用）

> **UC-FIN-14 断言⑤ config key 默认 true 但无消费的运维认知**：L3 静态确认无 scheduler 消费 `erp-fin.bank-recon-auto-reverse-next-month`（`CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH` 定义于 `ErpFinConstants.java:289` 但 grep 仅 1 命中即定义本身），但「运维是否误以为自动红冲生效」属部署面普查——交 MA4 A4.1 按需展开（核查 scheduler.yaml / nop-batch job.yaml 全量 + 部署文档）。
> — `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md:328`

### 1.2 L1 权威（UC-FIN-09/14 断言⑤ 红冲，逐字引用）

```
UC-FIN-09 (use-cases.md:176):
  未达账项 → 生成调整凭证(businessType=BANK_RECON_ADJ), 下月红冲   ← 断言④

UC-FIN-14 (use-cases.md:288):
  // 未达账项调整
  RECONCILED 时若存在未达 → 生成调整凭证(businessType=BANK_RECON_ADJ)
  下月初自动红冲(跨期还原)                                         ← 断言⑤
```

- **本验证对象** = 断言⑤「下月初**自动**红冲」的 config 孤儿化部署/运维认知影响面。「自动」是 UC-FIN-14 显式验收词（UC-FIN-09:176 仅「下月红冲」无「自动」，UC-FIN-14:288 显式「自动」——L1 权威以 UC-FIN-14 为准）。
- 断言④「调整凭证生成」= 接受类（A1.4 §5.5 + A4.1.12 done 证实 `BankReconAdjustmentVoucherBuilder.post:58-95` 行级正确）；本验证**不重复核实断言④**，只评断言⑤ config 孤儿化差异。

### 1.3 L2 owner doc 契约（`bank-reconciliation.md`，逐字引用）

```
§业务规则 6 (bank-reconciliation.md:108):
  6. posted 联动:调节表 RECONCILED 时若存在未达账项,生成调整凭证(isReversed=false),
     下月初自动红冲(跨期还原)。

§schema 补注 (bank-reconciliation.md:150):
  - **新增配置项**：... `erp-fin.bank-recon-auto-reverse-next-month`
    （默认 true，实际红冲由定时任务触发，本计划交付 `reverse` 入口 + 手动可触发）...
```

- **L2 §业务规则 6:108 与 L1 一致**，均要求「下月初自动红冲」。
- **L2 §schema 补注:150 显式承认调度未接线**：「实际红冲由定时任务触发，本计划交付 `reverse` 入口 + 手动可触发」——L2 诚实记录了「自动红冲依赖定时任务但本计划仅交付手动入口」的实现偏离。**该 schema 补注未经 §4 三判据人工批准**（无 plan-audit / 无 documented simplification 人工批准痕迹 / product-scope 未裁剪），故按 §4 冲突裁决规则「以 L1 为准」——L2 schema 补注仅作实现偏离的诚实记录，不构成合法降级。

### 1.4 L3 实仓锚点（写时实测）

| 组件 | 文件:行 | 审计状态 |
|---|---|---|
| **config key 定义** | `ErpFinConstants.java:289` `String CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH = "erp-fin.bank-recon-auto-reverse-next-month";` | ✅ 定义存在，默认 true（per `AppConfig.var(..., true)` 语义；L2 :150 显式「默认 true」） |
| **手动红冲入口** | `BankReconciliationBuilder.reverse:133-142`（POSTED 守卫 :135-137 → 调 `adjustmentVoucherBuilder.reverse` :138 → docStatus=CANCELLED :140） | ✅ 手动 reverse 存在 |
| **手动红冲凭证** | `BankReconAdjustmentVoucherBuilder.reverse:97-102`（hasAdjustmentVoucher guard :98-100 → voucherBiz.reverse :101） | ✅ 手动 reverse 存在 |
| **自动红冲调度（本验证核心）** | grep `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH\|auto-reverse\|AutoReverse\|autoReverse\|bank-recon-auto-reverse\|AutoReverseNextMonth\|auto-reverse-next-month` 跨 module-finance + app-erp-all（excl target）→ **仅 1 命中 = 定义本身**（`ErpFinConstants.java:289`） | ⚠️ **零 scheduler/cron/Job bean/IJobInvoker 消费**（§2 详述） |

---

## 2. config 消费点全集普查（核心证据）

> 方法论 §1 L3 引用规范：方法锚点 + 关键行为断言。本节为 config 孤儿化主张的全集普查，复核 A1.4 §2「grep 仅 1 命中」为全集。

### 2.1 grep config key 全变体（跨 module-finance + app-erp-all，excl target/test）

```
$ grep -rn "CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH\|auto-reverse\|AutoReverse\|autoReverse\|bank-recon-auto-reverse\|AutoReverseNextMonth\|auto-reverse-next-month" \
    module-finance app-erp-all --include="*.java" --include="*.xml" --include="*.yaml" --include="*.yml"
# 结果（excl target/classes）：
module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinConstants.java:289:
    String CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH = "erp-fin.bank-recon-auto-reverse-next-month";
# 仅 1 命中 = 定义本身。零消费点。
```

**裁决**：config key `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH` **经全变体 grep 确认零消费**。A1.4 §2「grep 仅 1 命中」**经全集复核 CONFIRMED**——无遗漏消费路径（无 `AppConfig.var(CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH...)` 读值点、无条件分支、无日志、无监控）。

### 2.2 调度配置文件全集普查

#### 2.2.1 scheduler.yaml（nop-batch 作业注册中枢）

- 文件：`app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`
- 内容（实测全文）：`enabled: true`
- **裁决**：scheduler.yaml 为启用开关，**零银行对账红冲条目**。作业注册经独立 `.job.yaml` 文件（见 2.2.2）。

#### 2.2.2 全 `.job.yaml` 文件清单（19 个，nop/job/conf/）

nop-job 注册的 19 个作业（全量枚举）：

| 域 | .job.yaml | 是否银行对账红冲 |
|---|---|---|
| finance | `erp-fin-deferred-posting-sweep.job.yaml` | ❌（递延过账清理） |
| finance | `erp-fin-cash-forecast-refresh.job.yaml` | ❌（现金预测刷新） |
| finance | `erp-fin-ar-ap-auto-recon.job.yaml` | ❌（**AR/AP** 自动核销，非银行对账——A1.4 §报告校正项已证实 `ErpFinAutoReconJob` = AR/AP 子系统 `IErpFinReconciliationBiz.runAutoReconciliation`，已于 2026-07-18 batch 迁移） |
| mfg | `erp-mfg-crp-run.job.yaml` / `erp-mfg-jobcard-auto-generate.job.yaml` | ❌ |
| ast | `erp-ast-depreciation.job.yaml` | ❌ |
| qa | `erp-qa-spc-sampling.job.yaml` / `erp-qa-spc-capability.job.yaml` | ❌ |
| crm | `erp-crm-sequence-overdue.job.yaml` / `erp-crm-forecast-recalc.job.yaml` / `erp-crm-funnel-aggregation.job.yaml` / `erp-crm-event-reminder.job.yaml` / `erp-crm-lead-scoring-recalc.job.yaml` | ❌ |
| cs | `erp-cs-sla-scan.job.yaml` / `erp-cs-csat-reminder.job.yaml` / `erp-cs-entitlement-expiry.job.yaml` | ❌ |
| hr | `erp-hr-contract-expiry.job.yaml` | ❌ |
| prj | `erp-prj-pnl-calc.job.yaml` | ❌ |
| mnt | `erp-mnt-due-visit-generation.job.yaml` | ❌ |

**裁决**：全 19 个 `.job.yaml` **无 `erp-fin-bank-recon-auto-reverse.job.yaml` 或任何银行对账红冲作业**。最易混淆的 `erp-fin-ar-ap-auto-recon` 经证实为 AR/AP 核销子系统（不同机制，A1.4 §报告校正项已裁决）。

#### 2.2.3 全 `.batch.xml` 文件清单（9 个，nop-batch 分 chunk 作业）

```
module-manufacturing: mfg/jobcard-auto-generate.batch.xml
module-assets:        ast/depreciation.batch.xml
module-crm:           crm/lead-scoring-recalc.batch.xml
module-finance:       fin/deferred-posting-sweep.batch.xml
                      fin/ar-ap-auto-recon.batch.xml      ← AR/AP，非银行对账
                      fin/cash-forecast-refresh.batch.xml
module-quality:       qa/spc-capability.batch.xml
                      qa/spc-sampling.batch.xml
module-projects:      prj/pnl-calc.batch.xml
```

**裁决**：全 9 个 `.batch.xml` **无 `bank-recon-auto-reverse.batch.xml` 或任何银行对账红冲批处理**。finance 域 3 个 batch 均非银行对账红冲。

#### 2.2.4 app-service.beans.xml（Job bean 注册）

- grep `BankReconciliation|bankrecon|BankRecon|BANK_RECON` 跨 `app-service.beans.xml` + `merged-app.beans.xml`（_dump）→ 命中**仅手动组件**：
  - `BankReconciliationBuilder`（手动 reverse 入口承载）
  - `BankReconAdjustmentVoucherBuilder`（手动 reverse 凭证承载）
  - `BankStatementImporter` / `BankStatementMatcher` / `BankLedgerQuery`（导入/勾对）
  - `BankReconAdjAcctDocProvider`（调整凭证科目提供器）
  - 3 Processor：`ErpFinBankReconciliationGenerateProcessor` / `PostProcessor` / `ReverseProcessor`（绑定 BizModel 的 generate/post/reverse mutation）
- **裁决**：beans.xml 注册**仅手动 mutation 组件**，**无 scheduler/Job bean / IJob / IJobInvoker / @CronProvider 引用银行对账红冲**。

#### 2.2.5 IJob/@CronProvider/Job 类全集

- grep `implements IJob|IJobInvoker|@CronProvider` 跨 module-finance main 源码 → **零银行对账相关 Job 类**。finance 仅有的「Job」语义类是 mfg 域 `ErpMfgJobCard*`（作业卡，非调度）+ finance `ErpFinBadDebtProcessor`/`AnnualCloseService`（非 Job bean，Processor/Service）。

### 2.3 config 消费点全集裁决

**config key `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH`（默认 true）经五维全集普查确认零消费**：
1. grep config key 全变体（§2.1）→ 仅定义本身；
2. scheduler.yaml（§2.2.1）→ 零银行对账条目；
3. 全 19 `.job.yaml`（§2.2.2）→ 零银行对账红冲作业；
4. 全 9 `.batch.xml`（§2.2.3）→ 零银行对账红冲批处理；
5. app-service.beans.xml + IJob/@CronProvider 全集（§2.2.4/§2.2.5）→ 仅手动组件，零 Job bean。

**config 孤儿化主张 CONFIRMED（A1.4 §2/§5.2 复核为全集，无遗漏消费路径）。**

---

## 3. 部署/运维认知影响面评估（§Phase 1 Proof 2）

> 评估 config key 默认 true 但零消费的「运维以为生效实际不执行」隐性失效误导面。

### 3.1 部署/运维文档声称面普查（grep 全集）

```
$ grep -rni "auto.*reverse|自动红冲|bank-recon-auto-reverse|auto-reverse-next-month|自动.*还原|跨期还原" \
    docs/ --include="*.md" | grep -vi "audit|plan|retrospective|lessons|bugs|input|discussions"
```

**命中归类**（excl 审计/计划/日志类文档，仅留设计/架构/真相源）：

| 命中 | 文件:行 | 内容性质 | 误导面判定 |
|---|---|---|---|
| `bank-reconciliation.md:108` | L2 §业务规则 6 | 「下月初自动红冲(跨期还原)」= **L1 一致的设计要求** | ❌ 无误导（与 L1 一致，是需求声明非实现声称） |
| `bank-reconciliation.md:150` | L2 §schema 补注 | 「默认 true，**实际红冲由定时任务触发，本计划交付 `reverse` 入口 + 手动可触发**」= **诚实承认调度未接线** | ❌ 无误导（显式承认仅交付手动入口） |
| `use-cases.md:288` | L1 权威 | 「下月初自动红冲(跨期还原)」= 需求契约 | ❌ 无误导（L1 需求声明） |
| `job-scheduling.md:111` | **架构层调度登记** | `erp-fin-bank-recon-adj-reverse`「下月初自动红冲上月银行对账调整凭证」触发「下月初」**状态：DESIGN**「（待实现）」+ 调用入口「（待实现）」 | ❌ 无误导（**诚实登记为 DESIGN/待实现**） |
| `job-scheduling.md:110` | 架构层调度登记 | `erp-fin-bank-recon`「月末银行对账、自动匹配」**状态：DESIGN** | ❌ 无误导（诚实登记 DESIGN） |

### 3.2 运维认知影响面 asymmetric 裁决

**架构/设计文档层 = 诚实（无虚假声称「自动红冲生效」）**：
- `job-scheduling.md §3.1 Finance`（调度架构真相源）**显式登记** `erp-fin-bank-recon-adj-reverse` 为 **DESIGN（待实现）**——状态机规范 REGISTERED > SCHEDULED > WIRED > **DESIGN** > DEFERRED 中，DESIGN = 已设计未实现。运维查阅调度架构文档会**正确得知**该作业未实现。
- L2 `bank-reconciliation.md §schema 补注:150` **显式承认**「实际红冲由定时任务触发，本计划交付 `reverse` 入口 + 手动可触发」——诚实记录仅交付手动入口。
- **结论**：不存在任何部署/运维/owner doc 虚假声称「自动红冲已生效」。误导面**不在文档层**。

**config key 层 = 残余局部误导（asymmetric）**：
- config key `erp-fin.bank-recon-auto-reverse-next-month` **默认 true**——config 名「auto-reverse-next-month」+ 默认 true，对**仅查 config（不查阅 job-scheduling.md 架构文档）的运维**具残余误导面：config 名暗示功能存在 + 默认 true 暗示启用。
- 该残余误导面**经评估不升 P0**：①误导不致「无补救的会计余额错报」——手动 `reverse()` 补救存在（§4）；②架构文档诚实登记 DESIGN，尽责运维经交叉查阅可正确认知；③残余误导限于「config 名 + 默认 true 的局部不对称」，非系统性虚假声称。

### 3.3 运维认知影响面总结

| 层 | 误导面 | 处置 |
|---|---|---|
| 调度架构文档（`job-scheduling.md`） | **零**（诚实登记 DESIGN） | 无需文档修订 |
| L2 设计文档（`bank-reconciliation.md`） | **零**（schema 补注诚实承认） | 无需文档修订 |
| L1 真相源（`use-cases.md`） | **零**（需求声明） | 冻结（§9） |
| **config key 默认 true**（仅查 config 的运维） | **残余局部误导**（config 名 + 默认 true 暗示生效） | 经 P1-RC-005 修复（接线 scheduler 消费 config key）消除——修复后 config 真正门控红冲作业，误导面消失 |

**裁决**：运维认知误导面**部分缓解**——架构/设计文档层诚实（DESIGN 登记 + schema 补注承认），残余误导限于 config key 默认 true 的局部不对称。**不构成 P0 升级依据**（无虚假声称 + 手动补救存在 + 架构文档可交叉查阅）。

---

## 4. 手动补救有效性核验（§Phase 1 Proof 3）

> 确认手动 `reverse()` 入口作为出纳补救路径的运行时有效性（复用 MA2 A2.5c + A1.4 §2.4）。

### 4.1 手动 reverse 调用链（写时实测）

```
出纳触发 reverse(reconciliationId)
  → BankReconciliationBuilder.reverse:133-142
      - requireRecon(reconciliationId) :134,166-174  （存在性守卫）
      - POSTED 守卫 :135-137  （非 POSTED 抛 illegalTransition）
      - adjustmentVoucherBuilder.reverse(recon, context) :138
          → BankReconAdjustmentVoucherBuilder.reverse:97-102
              - hasAdjustmentVoucher(recon) guard :98-100  （无调整凭证返回 null）
              - voucherBiz.reverse(recon.getCode(), ErpFinBusinessType.BANK_RECON_ADJ, context) :101
      - recon.setDocStatus(CANCELLED) :140
```

### 4.2 补救有效性裁决

- **入口可达**：`BankReconciliationBuilder.reverse:133-142` 经 `ErpFinBankReconciliationReverseProcessor` 接线（beans.xml §2.2.4 确认），绑定 `IErpFinBankReconciliationBiz` 的 reverse mutation——出纳可经领域操作手动触发。
- **行为正确**：POSTED 守卫 + 调凭证红冲 + docStatus=CANCELLED 三段完整（A1.4 §2.4 + A4.1.12 done 行级证实）。`voucherBiz.reverse` 经 `ErpFinBusinessType.BANK_RECON_ADJ` + `billCode` 精确反查调整凭证（`countAdjustmentLinks:109-116` 范式），红冲凭证经标准凭证流程生成。
- **运行时有效**：手动 reverse 可恢复正确余额——未达调整凭证经红冲后，银行存款科目 + 未达调整对方科目余额恢复（跨期还原经手动 reverse 实现，但**非下月初自动**触发，需出纳主动操作）。
- **复用 MA2 A2.5c**：`2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md` 已证实银行对账手动 `reverse()` 入口存在（出纳可手动补救）。本验证复用其「手动补救路径存在」结论，只补「config 孤儿化运维认知」差异。

**裁决**：手动 `reverse()` 入口**运行时有效**——出纳可手动补救，恢复正确余额，调整凭证不再持续挂账。**这是 P1-RC-005 不升 P0 的关键判据**（补救存在 → 非无补救的隐性致会计余额错报 → 非 P0④/P0①）。

> **补救的局限性（强化 P1 不可降 P2）**：手动 reverse 依赖**出纳主动记忆 + 每月手动触发**——config key 默认 true 暗示自动，运维若信 config 名可能**遗漏手动触发**（残余误导面的运行时后果）。该局限性强化「自动调度缺失」为真实合规缺陷（非仅文档偏差），但补救的存在使其不升 P0。

---

## 5. P1-RC-005 分级确认/调整裁决（§Phase 1 Decision + §2 判据三源复核）

### 5.1 §2 分级判据三源复核

#### 5.1.1 P0 判据复核（均不成立）

| §2 P0 判据 | 是否成立 | 依据 |
|---|---|---|
| P0①「活跃数据破坏防护未实现」 | **不成立** | 未达调整凭证不自动红冲致余额潜在错报，但**需出纳遗漏手动 reverse 触发**（非默认活跃路径每次操作即破坏）；与 P0 示例「凭证重复过账」的默认触发面不同。手动补救存在（§4）。 |
| P0②「安全/数据隔离未实现」 | 不适用 | 银行对账红冲非安全/隔离维度。 |
| P0③「核心业务循环断裂」 | **不成立** | 银行对账主循环（导入→勾对→调节→post→reverse）完整可手工走完，仅「自动」触发缺失。 |
| P0④「会计过账正确性破坏」 | **不成立** | 错误限于未达调整凭证不自动跨期还原（调整凭证持久挂账致余额潜在错报），但**GL 过账本身正确** + 对账子系统与过账解耦（MA2 :48,223,365 证实 `ErpFinBankStatement` vs `ErpFinReconciliation` 独立）。错误不污染凭证过账/GL 余额正确性。 |

**P0 升级路径复核（plan §Phase 1 Decision ③）**：「config 默认 true 运维误导面显著且无补救 → 考虑升 P0」**不成立**——运维误导面经 §3 评估为**部分缓解**（架构文档诚实登记 DESIGN + L2 schema 补注承认）+ 手动补救存在（§4），非「无补救的隐性致会计余额错报」。**不升 P0，不触发 MR0 即时通道。**

#### 5.1.2 P2 判据复核（不适用）

- §2 P2①「次要验收标准未完全满足（主路径 OK，边界场景弱）」**不适用**——L1 UC-FIN-14:288 逐字「下月初**自动**红冲」显式「自动」是**主验收标准**（UC-FIN-14 为详细断言版，断言⑤ 独立编号），config 孤儿化属 §2 P1①「功能完全缺失」（"自动"红冲的调度功能**完全缺失**，非仅边界场景弱）。
- **强化不可降 P2**：§4.2 补救局限性（依赖出纳主动记忆）+ §3.2 config 默认 true 残余误导面 → 自动调度缺失是真实合规缺陷，非文档偏差。

#### 5.1.3 P1 判据命中（成立）

- §2 **P1①**「需求契约要求的功能完全缺失或行为实质偏离验收标准」**成立**——L1 UC-FIN-14:288 显式要求「下月初**自动**红冲」，"自动"红冲的调度功能**完全缺失**（§2 config 消费点全集零消费 CONFIRMED）。

### 5.2 分级裁决

**维持 P1-RC-005 = P1。不升 P0，不降 P2。不触发 MR0 即时通道。**

| 裁决维度 | 结论 | 依据 |
|---|---|---|
| 分级 | **P1 维持** | §2 P1① 命中（"自动"红冲调度完全缺失）；P0①③④均不成立；P2 不适用（主验收标准）。 |
| 与 A1.4 §5.2 分层一致性 | **一致** | A1.4 §5.2 P1-RC-005 P1 结论维持（§2 P1①「功能完全缺失」+ 非默认活跃路径破坏 + 手动补救存在 → P1 非 P0）。 |
| 与 arm-index `:130` 衔接 | **确认维持 P1** | arm-index P1-RC-005 行（todo，MR1）——本验证确认维持 P1，不升 P0 不降 P2，追加 A4.1.14 运行时全集普查注记（§7）。 |
| 修复通道 | **MR1（R1.0→RC-R1.n）** | 修复 = 接线 scheduler（nop-batch `job.yaml` 注册下月初红冲作业 + 消费 config key 门控 + 批量调 `BankReconciliationBuilder.reverse`）→ 纯调度接线 + BizModel 调用，按 roadmap 预授权类目（代码逻辑修复）可自动执行，**不触发 §5 ask-first**（不触及 ORM/会计过账核心路径 VoucherFact/PostingProcessor，仅调用既有 reverse 入口）。 |

### 5.3 与 A1.4 §5 P1 结论对照

| A1.4 §5.2 P1-RC-005 维持 P1 的依据 | 本验证核实 |
|---|---|
| 「config key 默认 true 但无消费」 | **CONFIRMED**：§2 五维全集普查零消费（复核 A1.4 §2「grep 仅 1 命中」为全集） |
| 「运维以为自动生效但实际不执行，属隐性失效」 | **精化（部分缓解）**：架构文档（`job-scheduling.md:111`）诚实登记 DESIGN + L2 schema 补注承认，文档层无虚假声称；残余误导限于 config 默认 true 局部不对称（§3） |
| 「可由出纳手动触发 reverse() 补救，故非 P0④活跃数据破坏」 | **CONFIRMED**：§4 手动 reverse 调用链运行时有效（POSTED 守卫 + 凭证红冲 + CANCELLED），补救存在 → 非 P0 |
| 「严重性 major（会计余额潜在错报，但手动补救存在 + 需人工遗漏触发）」 | **维持**：补救局限性（依赖出纳主动记忆）+ config 残余误导面强化 major，但不升 P0 |

---

## 6. §去重声明（与 arm-index 交叉比对）

本验证**未产生新 finding**。全部 config 孤儿化/运维认知/手动补救证据归以下既有 finding：

| 既有 finding | 控制点 | 与本验证关系 |
|---|---|---|
| `P1-RC-005` | UC-FIN-09 断言④/UC-FIN-14 断言⑤ 下月初自动红冲缺失（config key 无 scheduler 消费） | **本验证对象**。config 消费点全集普查确认零消费 + 运维认知影响面部分缓解 + 手动补救有效 → 维持 P1，不升 P0，不降 P2。 |
| `P1-RC-004` | UC-FIN-09/14 断言② 对方账号匹配维度缺失 | **不同断言**（断言② 勾对维度 vs 断言⑤ 红冲调度），不可合并。A4.1.11 done 同 §7-1 家族。 |
| `P2-RC-001` | 跨多条 statement refNo 去重范围 | **不同断言**（① dedup vs ⑤ 红冲），不可合并。A4.1.13 done 同 §7-3 家族。 |
| `P2-RC-002` | valueDate→transactionDate 简化 | **不同断言**（② 日期列 vs ⑤ 红冲），不可合并。 |
| MA2 银行对账解耦 | 银行对账为独立子系统（`ErpFinBankStatement` vs `ErpFinReconciliation`）+ 手动 reverse 入口存在 | 本验证 §4 手动补救有效性 + §5.1.1 P0④ 不成立的关键依据（对账子系统与过账解耦），引用 MA2 :48,223,365 + A2.5c。 |

**无未经比对直接新建的 finding。** P1-RC-005 已登记（arm-index `:130`），本验证只更新分级注记（确认维持 P1 + 运维认知部分缓解 + 手动补救证据，§7）。

---

## 7. P1-RC-005 分级注记更新（arm-index `:130`）

本验证确认 **P1-RC-005 维持 P1**，在 arm-index `:130` P1-RC-005 行追加 A4.1.14 运行时 config 消费点全集普查注记：

- **config 消费点全集普查确认零消费**：五维普查（grep config key 全变体 + scheduler.yaml + 全 19 `.job.yaml` + 全 9 `.batch.xml` + beans.xml/IJob/@CronProvider）→ 仅 1 命中 = 定义本身（`ErpFinConstants.java:289`），零 scheduler/cron/Job bean/IJobInvoker/nop-batch 消费（§2，A1.4 §2 grep 全集复核 CONFIRMED）。
- **运维认知影响面部分缓解**：架构层 `job-scheduling.md:110-111` 诚实登记 `erp-fin-bank-recon-adj-reverse` 为 DESIGN（待实现）+ L2 `bank-reconciliation.md:150` schema 补注承认「本计划交付 reverse 入口 + 手动可触发」→ 文档层无虚假声称；残余误导限于 config key 默认 true 局部不对称（§3）。
- **手动补救有效**：`BankReconciliationBuilder.reverse:133-142` + `BankReconAdjustmentVoucherBuilder.reverse:97-102` 运行时有效（POSTED 守卫 + 凭证红冲 + CANCELLED，出纳可手动恢复正确余额）（§4）。
- **分级结论**：维持 P1（§2 P1① 自动调度完全缺失命中 + P0①③④均不成立 + P2 不适用主验收标准 + 补救存在故非 P0）。
- **修复通道**：仍归 MR1（R1.0→RC-R1.n），纯调度接线预授权不触 §5 ask-first。

---

## 8. 过程纪律自检（§8 模板）

### 8.1 checker actual vs baseline（实测记录）

运行 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；方法论 §8 不以退出码 0 为门控；真正门控在 CI workflow `.github/workflows/compliance.yml`）。本计划为**只读评估**（零生产代码变更），故 checker 无回归风险。

| 规则 | actual（实测） | baseline（compliance-baseline.md §BASELINE machine-readable :296-316） | 判定 |
|---|---|---|---|
| R1a (dao().saveEntity BizModel) | 0 | 0 | = ✅ |
| R1b (dao().updateEntity BizModel) | 0 | 0 | = ✅ |
| R1c (dao().getEntityById BizModel) | 0 | 0 | = ✅ |
| R1d (dao().findAllByQuery BizModel) | 14 | 14 | = ✅ |
| R2a (BizModel daoFor ErpMd*) | 34 | 34 | = ✅ |
| R2b (BizModel daoFor Erp* 跨域) | 229 | 229 | = ✅ |
| R2c (全生产 daoFor 总量) | （脚本 R2c 段既有行为：未输出计数即返回——A4.1.11 已记录同型行为） | 1382 | 不适用（脚本行为） |
| R2d (Processor daoFor ErpMd*) | 34 | 34 | = ✅ |
| R3+ | （脚本在 R3 header 处退出，非异常——A4.1.11 同型行为） | — | 不适用 |

**说明**：
1. **checker 可达规则（R1a-R2d，除 R2c）actual 全部 = baseline（0 漂移）**——本计划零生产代码变更，结构上对计数零贡献。R2c/R3+ 的脚本既有早退行为（未输出即返回）与 A4.1.11 实测一致，与本验证无关。
2. **门控结论**：本验证**无回归风险**（零生产代码变更），checker 仅作过程记录，不作通过/失败门控依据（方法论 §8）。本报告**不**以 checker 退出码（脚本在 R3 退出非 0）作为门控通过依据——真正门控在 CI workflow。

### 8.2 closure-audit 独立性声明

本验证报告由主代理（执行者）起草。**结束审计将由独立子代理（新会话）执行**（plan §Closure Gates），执行者未自我审计，未将结束审计留为 `[ ]` 人工门控占位符。

### 8.3 与 arm-index 交叉去重声明

见 §6。全部 config 孤儿化/运维认知/手动补救证据归既有 finding（`P1-RC-005` 本验证对象 / `P1-RC-004` / `P2-RC-001` / `P2-RC-002` / MA2 解耦），无新建 finding。P1-RC-005 分级注记更新见 §7（确认维持 P1 + 运维认知部分缓解 + 手动补救证据）。

---

## 9. 与 MA2/A1.4 报告差异增量声明（§去重协议）

本验证声明与既有 MA2/A1.4 报告的差异增量：

- **复用 MA2/A1.4 已证实行为**（不重新核实）：
  - `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md:48,223,365`：银行对账为独立子系统（与 AR/AP 解耦），手动 `reverse()` 入口存在。本验证 §4 手动补救有效性 + §5.1.1 P0④ 不成立直接引用。
  - A1.4 §2.4 + §5.2（P1-RC-005）：config key 无 scheduler 消费静态确认 + P1 定级。本验证复核其「grep 仅 1 命中」为全集 + 补部署/运维认知影响面评估。
- **本验证只补的差异**（MA2/A1.4 未覆盖）：
  1. **config 消费点全集普查**（§2）：A1.4 §2 grep「仅 1 命中」经五维普查（scheduler.yaml + 全 19 `.job.yaml` + 全 9 `.batch.xml` + beans.xml + IJob/@CronProvider）复核为全集，确认零消费无遗漏。
  2. **部署/运维认知影响面评估**（§3）：MA2 状态机维度无调度对象，A1.4 §7 存疑点 4 交 MA4 展开部署面普查。本验证普查部署/运维文档声称面 + 评估 config 孤儿化运维误导面（架构文档诚实登记 DESIGN 部分缓解 + config 默认 true 残余局部不对称）。
  3. **手动补救有效性核验**（§4）：复用 MA2 手动 reverse 入口存在结论，补调用链运行时有效性（POSTED 守卫 + 凭证红冲 + CANCELLED）+ 补救局限性（依赖出纳主动记忆）。
  4. **P1-RC-005 分级确认**（§5）：经 §2 判据三源复核确认维持 P1（不升 P0[补救存在 + 文档诚实] / 不降 P2[主验收标准 + 补救局限性强化]）。

---

## 10. 验证范围与非目标

- **本验证只读**：读 config key 定义（`ErpFinConstants.java:289`）+ 全量普查调度配置文件（scheduler.yaml / `.job.yaml` / `.batch.xml` / beans.xml）+ 读部署/运维文档（`bank-reconciliation.md` / `job-scheduling.md`）+ 引用 MA2/A1.4 + config 孤儿化运维认知影响面推理。未改任何 `.java`/`.xml`/`.orm.xml`/config 默认值/真相源。
- **不修复 P1-RC-005**（下月自动红冲调度缺失——修复为接线 scheduler，归 MR1 预授权类目，不触发 §5 ask-first）。
- **不重新核实 UC-FIN-09/14 全部验收标准**（A1.4 §5 已判 P1-RC-005；本验证只评 config 孤儿化部署/运维认知差异）。
- **不展开 A1.4 §7-1/§7-2/§7-3**（A4.1.11 对方账号触发率 done / A4.1.12 调整凭证行级 done / A4.1.13 跨多条 statement refNo done）。

## 11. MR0 触发登记

**无**。Phase 1 裁决为维持 P1（§5.2），不触发 MR0 即时通道（方法论 §10）。本验证不实施修复。

## 12. 结论

UC-FIN-14 断言⑤「下月初**自动**红冲(跨期还原)」的 **config key 孤儿化运维认知影响面**（P1-RC-005）经运行时部署/运维面评估：config key `erp-fin.bank-recon-auto-reverse-next-month`（`ErpFinConstants.java:289`，默认 true）**经五维全集普查确认零消费**（grep config key 全变体 + scheduler.yaml + 全 19 `.job.yaml` + 全 9 `.batch.xml` + beans.xml/IJob/@CronProvider → 仅 1 命中 = 定义本身，核心 config 孤儿化主张 CONFIRMED，A1.4 §2 grep 全集复核完成）。**运维认知影响面部分缓解**：架构层 `job-scheduling.md:110-111` 诚实登记 `erp-fin-bank-recon-adj-reverse` 为 DESIGN（待实现）+ L2 `bank-reconciliation.md:150` schema 补注承认仅交付手动入口 → 文档层无虚假声称「自动红冲生效」；残余误导限于 config key 默认 true 局部不对称（仅查 config 的运维可能误以为生效）。**手动补救有效**：`BankReconciliationBuilder.reverse:133-142` + `BankReconAdjustmentVoucherBuilder.reverse:97-102` 运行时有效（出纳可手动恢复正确余额）。**维持 P1-RC-005 = P1，不升 P0**：P0①③④均不成立（补救存在 + 需出纳遗漏触发非默认活跃路径 + 对账子系统与过账解耦[MA2 证实] + GL 过账本身正确）+ 运维误导面经评估不构成 P0 升级依据（架构文档诚实 + 手动补救存在）；**不降 P2**：L1 UC-FIN-14:288「自动」是主验收标准 + 补救局限性（依赖出纳主动记忆）强化合规缺陷。**不触发 MR0，不升 P0，不降 P2，无新 finding。** P1-RC-005 修复仍归 MR1（纯调度接线预授权不触 §5 ask-first）。本验证不实施修复。
