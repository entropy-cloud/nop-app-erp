# RC MA4 A4.1 — 业财域 MA1 存疑点运行时确认展开器（展开映射记录）

> Audit Status: closed
> 里程碑：MA4（运行时行为验证 / 展开器工作项）
> 工作项：A4.1（业财域 MA1 存疑点运行时确认**展开器**）
> 展开器 plan：`docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 展开器范式 + §10 R1.0 同构 + §去重协议 + §5 保护区域暂停协议 + §8 过程纪律自检）
> 输入：finance MA1 切片报告 A1.1-A1.7 §7 静态存疑点清单（7 报告，业财域范围）
> 结果表面：`docs/backlog/requirement-compliance-roadmap.md` MA4 表内追加的 A4.1.1-A4.1.25 实体行（本记录的映射对象）
> 审计性质：**只读展开器**（读 MA1 报告 §7 + 追加 roadmap 表；不改代码/ORM/api.xml/真相源；§9 冻结条款）
> 展开完成判据：全部 §7 存疑点已展开为 A4.1.n 行（done = 展开完成，非验证完成；§MA4 + §10 R1.0 同构）
> 审计日期：2026-08-07
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. 展开结论（TL;DR）

| 项 | 数量 |
|---|---|
| 输入 finance MA1 切片报告（§7 存疑点清单） | 7（A1.1-A1.7） |
| §7 存疑点条目（逐条提取，条目制非词频制） | **25** |
| 跨报告同根因同控制点去重合并 | **0**（25 条均为不同控制点，各自一行；2 条跨报告主题相关但控制点不同，保留独立行 + 交叉引用） |
| 展开为 A4.1.n 实体行 | **A4.1.1 - A4.1.25**（25 行，已追加到 roadmap MA4 表） |
| 触及保护区域（验证探针需 ask-first） | **2**（A4.1.15 成本过账行为探针 / A4.1.16 数据删除行为探针）；余 23 行否 |
| A4.1 状态 | **done**（展开完成） |
| A4.1.n 状态 | **todo**（各自待独立 plan + 独立验证 done；mission driver 逐项 DRAFT_PLANS → 验证） |

**整体裁决**：finance MA1 切片报告（业财域）A1.1-A1.7 的 §7 静态存疑点清单**全集**经逐条提取（条目制）+ 跨报告去重，展开为 25 条 A4.1.n 实体行，已追加到 `requirement-compliance-roadmap.md` MA4 表。展开完成即 A4.1 done（对齐 §MA4 + §10 R1.0 done 判据 = 展开完成而非验证完成）。本展开器**不执行运行时验证本身**——验证属后续各 A4.1.n 实体行（各自独立 plan + 独立 done）。零代码/ORM/api.xml/真相源变更（roadmap MA4 表追加是工作项追踪更新，非冻结真相源）。

---

## 1. 输入报告 §7 存在性核对（逐报告）

| 切片 | 报告文件 | §7 段落存在性 | §7 存疑点条目数（条目制） |
|------|---------|--------------|------------------------|
| A1.1 finance-F1 过账引擎 | `2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` | ✅ §7 存在 | 3 |
| A1.2 finance-F2 预算与承付 | `2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` | ✅ §7 存在 | 4 |
| A1.3 finance-F3 AR/AP 核销与坏账 | `2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` | ✅ §7 存在 | 3 |
| A1.4 finance-F4 银行对账 | `2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` | ✅ §7 存在 | 4 |
| A1.5 finance-F5 成本核算 | `2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` | ✅ §7 存在 | 3 |
| A1.6 finance-F6 期间与结账 | `2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` | ✅ §7 存在 | 4 |
| A1.7 finance-F7 报表/看板/多账套 | `2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` | ✅ §7 存在（SP-1..SP-4） | 4 |
| **合计** | 7 报告 | 7/7 §7 段落存在 | **25** |

> 「mentions」词频（plan baseline 表 5/6/12/7/4/4/6=44）≠ 实际存疑点条目数（25）。本展开器按方法论表注（条目制非词频制）逐条提取 §7 编号清单，25 为权威条目数。

---

## 2. 存疑点 → A4.1.n 行映射表（全 25 行，含完整字段）

> 每行字段：编号 / 来源切片 + §7 锚点 / §7 存疑点原文摘要 / 运行时验证方法（复用 E2E + `_helper.ts` 原语 / 既有 JUnit / 临时探针）/ 预期证实·证伪 / 触及保护区域（是/否 + 类别）。验证方法标注引用的真实原语：`assertVoucherLines` / `findVoucherIdByBillCode` / `findCommitmentVoucherIdByBillCode` / `runP2pChain` / `runO2cChain` / `runP2pReverse` / `runO2cReverse`（`tests/e2e/orchestration/_helper.ts`）。

### 2.1 A1.1 finance-F1 过账引擎（3 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.1** | A1.1 §7-1 | UC-FIN-02 断言④「业务单据.posted=false」域 listener 实际回写覆盖率——`dispatchReversalEvent:376-401` 派发 VoucherReversedEvent，MA2 §5.9 场景D 证实 8 域 reversal writeback 测试矩阵，但逐域 listener 实现未逐域核验 | 复用 `runP2pReverse`/`runO2cReverse` + 各域 reverse E2E + `rg` 各域 VoucherReversedEvent listener；逐域 reverse 后断言源单据 posted==false | **证实**：8 域（purchase/sales/inventory/assets/projects/mfg/quality/maintenance 等）reverse 后源单据 posted 一致回写 false；**证伪**：某域 listener 未实现/吞异常致 posted 残留 true | 否（read-only 行为普查 + 既有 E2E 复用） |
| **A4.1.2** | A1.1 §7-2 | UC-FIN-12 汇率缺失触发面实测——L3 静态确认回退逻辑（`prepareContext:537`），各域 Provider 是否在所有外币场景显式传 rate 属运行时调用面普查 | `rg "setExchangeRate"` 各域 PostingEvent 构造点普查 + 复用既有外币 E2E 跑外币过账观测凭证行 amtFunctional；构造 1 个外币场景漏传 rate 观测是否回退 1（关联 P1-RC-002 守卫未实现） | **证实**：当前各域 Provider 显式传 rate，无活跃错误数据（与 A1.1 P1-RC-002 评估一致）；**证伪**：某外币调用路径漏传 rate 致 amtFunctional=amtSource | 否（grep 普查 + 既有 E2E 观测，不改过账核心） |
| **A4.1.3** | A1.1 §7-3 | UC-FIN-03 PROJECT_SETTLEMENT businessType 是否已有 Provider 注册——L3 证实可插拔机制通用，但该具体 businessType 是否已有 `IErpFinAcctDocProvider` Bean 属实例普查 | `rg "PROJECT_SETTLEMENT"` + `getSupportedBusinessTypes` 各 Provider；查 `ErpFinAcctDocRegistry` 注册表运行时 businessType→Provider 映射 | **证实**：机制通用（已由 A1.1 接受），PROJECT_SETTLEMENT 是否有 Bean 仅影响「是否需新增」非合规；**证伪**：无 Bean 时项目结算审核不产凭证（属功能配置非契约分歧） | 否（实例普查） |

### 2.2 A1.2 finance-F2 预算与承付（4 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.4** | A1.2 §7-1 caveat① | config 默认关闭（`isBudgetCheckEnabled`/`isCommitmentEnabled` 默认 false）是否与「开箱即用预算硬拦截」部署契约冲突——L1 未显式声明默认开启 | 核对 `product-scope.md` + 部署文档 + config 默认值；确认是否存在「开箱即用预算控制」隐含契约 | **证实**：L1 描述控制语义未强制默认开启 → 维持接受；**证伪**：部署契约要求默认开启则属默认行为分歧（升级评估） | 否（doc/config 普查） |
| **A4.1.5** | A1.2 §7-2 caveat③ | 承付凭证 header 借贷不平（totalDebit≠totalCredit，单行单边）是否被通用试算平衡报表/三表暴露——COMMITMENT 凭证经专用 Generator 直写不经 assertBalanced | seed COMMITMENT 凭证后跑试算平衡 + BS/IS 报表观测；**注**：A1.7 已静态证 BS/IS 安全（`orm.xml:1740-1742` BUDGET/COMMITMENT 不入 GlBalance），本行聚焦独立试算平衡报表（若有） | **证实**：BS/IS 安全（A1.7 静态结论运行时复核成立）；**证伪**：独立试算平衡报表对所有 postingType 求和致恒等式破坏 | 否（read-only 报表观测） |
| **A4.1.6** | A1.2 §7-3 | UC-FIN-13 断言④ E2E `fin-budget-vs-actual.value.spec.ts` 是否断言 commitment 独立列——单测仅断言两列且不 seed commitment，E2E 断言强度属运行时确认 | 读 E2E spec 断言强度评估（A5.6 评级视角）；与 P1-RC-003 修复协同（修复后 E2E 须同步补 commitment 列断言） | **证实**：E2E 同样未断言三列（与单测同步偏离 L1）；**证伪**：E2E 已隐含断言 commitment | 否（测试断言强度评估） |
| **A4.1.7** | A1.2 §7-4 | 承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态——钩子已落地但 config 默认 false | 核查部署 config（`commitment-release-on-return` + `budget-commitment-enabled`）；属既有 P1-MA2-082 行为证据 | **证实**：config 默认 off（保守方向偏移，P1-MA2-082 已登记）；**证伪**：部署已显式启用 | 否（config/deploy 普查） |

### 2.3 A1.3 finance-F3 AR/AP 核销与坏账（3 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.8** | A1.3 §7-1 注意点②边界 | `PartnerBalanceUpdater.sumOpen` 对 WRITTEN_OFF 的隐式排除——sumOpen 仅显式排除 SETTLED/CANCELLED，WRITTEN_OFF 隐式排除依赖 `executeWriteOff:168` 置 openAmount=0；「部分核销后坏账」（PARTIAL→WRITTEN_OFF）边界运行时未覆盖 | JUnit 补强：构造 PARTIAL 辅助账项执行 writeOff，断言 partner.receivableBalance 正确反映剩余（openAmount 归零） | **证实**：`validateAmount` amount≤open + executeWriteOff open-=amount 保证归零，余额正确；**证伪**：边界场景 openAmount 残留致余额虚高 | 否（JUnit 辅助账补强，非 GL 过账核心） |
| **A4.1.9** | A1.3 §7-2 | `TestErpFinBadDebt.testWriteOffSetsStatusAndVoucherNoPL:140` 凭证 businessType 断言强度——断言科目/方向/金额/无费用科目，未断言 businessType==BAD_DEBT_WRITE_OFF 枚举值 | JUnit 断言补强评估：是否补 businessType 枚举断言（次要，凭证内容已实质覆盖核销语义） | **证实**：凭证内容已覆盖核销语义（次要缺口）；**证伪**：businessType 实际偏离 BAD_DEBT_WRITE_OFF | 否（测试断言补强评估） |
| **A4.1.10** | A1.3 §7-3 | `TestErpFinAutoReconciliation.testConfigGatedDisabled:120` 禁用路径覆盖缺口——javadoc 自述类级 @NopTestConfig 无法测 auto-reconcile 禁用路径 | 测试缺口评估：是否重构 @NopTestConfig 隔离以测禁用路径（in-code 声明的已知缺口） | **证实**：禁用路径无覆盖（已知 in-code 声明）；**证伪**：禁用路径已有间接覆盖 | 否（测试覆盖评估） |

### 2.4 A1.4 finance-F4 银行对账（4 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.11** | A1.4 §7-1 | UC-FIN-09/14 断言② 对方账号缺失致错误 MATCHED 的实际触发率——`BankLedgerQuery.findCandidates` 无对方账号过滤（关联 P1-RC-004） | 构造同额同日不同 partner 的 voucher line + bank line，跑 `BankStatementMatcher.autoMatch` 观察是否错误 MATCHED | **证实**：同额同日不同对方账号且账面仅 1 候选时错误 MATCHED（P1-RC-004 触发面）；**证伪**：余额恒等式下游兜底或候选≥2 转 SUSPENSE | 否（read-only 勾对观测；关联 P1-RC-004 修复触 ORM） |
| **A4.1.12** | A1.4 §7-2 | UC-FIN-09/14 断言④ 调整凭证行级 Dr/Cr/科目/金额正确性——L4 仅断言凭证存在性+计数，`BankReconAdjAcctDocProvider.createFacts:51-70` 产出的 2-4 条 VoucherFact 行级无断言 | post 后断言 ErpFinVoucherLine 行级 subjectCode/dcDirection/debitAmount/creditAmount（bankCredit>0 → Dr 银行 / Cr 调整；bankDebit>0 → Dr 调整 / Cr 银行） | **证实**：行级 Dr/Cr/科目/金额符合 `createFacts:51-70` 设计；**证伪**：行级方向或金额错误 | 否（read-only 凭证行级观测） |
| **A4.1.13** | A1.4 §7-3 | UC-FIN-09/14 断言① 跨多条 statement refNo 重复的实际检出——`findStatementIdByAccount:198-207` 仅查最近一条 statement（关联 P2-RC-001） | 构造多条 statement 场景（同 account 不同 statementDate，重复 refNo），跑 import 观测是否漏检致重复入账 | **证实**：跨 statement 重复 refNo 漏检（P2-RC-001 触发面）；**证伪**：refNo 全局唯一性实际使漏检罕见 | 否（read-only 导入观测） |
| **A4.1.14** | A1.4 §7-4 | UC-FIN-14 断言⑤ config key 默认 true 但无 scheduler 消费的运维认知——`CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH` 定义无消费（关联 P1-RC-005） | 核查 `scheduler.yaml` + nop-batch `job.yaml` 全量 + 部署文档，确认运维是否误以为自动红冲生效 | **证实**：无 scheduler 消费（隐性失效，P1-RC-005）；**证伪**：部署已有外部 cron 触发 reverse | 否（scheduler/doc 普查） |

### 2.5 A1.5 finance-F5 成本核算（3 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.15** | A1.5 §7-1 | FIFO 物料 + 到岸成本 delta 层 + 后续出库消耗的运行时数值正确性——delta 层 unitCost=Δ 被后续 FIFO 出库消耗时出库成本是否正确含调整；多 delta 层 + 原入库层混合排序 Σ 正确性（关联 P2-RC-004） | 新增 E2E/JUnit：FIFO 物料 receive → landed cost delta 层追加 → 多笔出库，断言出库成本含 delta 单价（多 delta 层混合排序 Σ） | **证实**：FIFO delta 层经出库生成 COGS/SALES_OUTPUT 凭证，出库成本正确含调整（闭合 P2-RC-004）；**证伪**：delta 层消耗顺序/Σ 错误致成本偏移 | **是（成本过账行为探针）** — 无既有 E2E 覆盖 FIFO+landed cost 交互，新探针 exercise costing→posting 核心行为；§5 会计过账行为探针 ask-first |
| **A4.1.16** | A1.5 §7-2 | FIFO 物料到岸成本红冲 delta 层部分消耗后物理删除的余额守恒——`removeFifoAdjustLayer:194-202` 按 `-line.id` 哨兵物理删除 delta 层，若已部分消耗物理删除可能破坏已扣减层（复用 P2-MA2-029 子场景） | 新增探针：FIFO 物料 landed cost 分摊后部分出库消耗 delta 层，再红冲，断言余额 totalCost 守恒 + 已扣减层未被破坏 | **证实**：物理删除已部分消耗 delta 层致余额漂移/层破坏（闭合 P2-MA2-029 子场景）；**证伪**：余额守恒（哨兵精确删除未误伤） | **是（数据删除行为探针）** — `removeFifoAdjustLayer` 物理删除属数据删除逻辑；§5 数据删除类 ask-first |
| **A4.1.17** | A1.5 §7-3 | P1-MA2-085 SELECT FOR UPDATE 路径在 H2 内存库外的真实 DB（PG/MySQL）的锁行为——`ormTemplate.lock(receive)` 跨数据库方言一致性 | 生产部署观测（PG/MySQL）`ormTemplate.lock` 锁行为；非本切片阻塞（P1-MA2-085 已 resolved） | **证实**：跨方言锁行为一致（P1-MA2-085 修复有效）；**证伪**：某方言锁退化致并发同 receiveId 审核 | 否（部署 DB 锁观测） |

### 2.6 A1.6 finance-F6 期间与结账（4 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.18** | A1.6 §7-1 | PC-3 AR/AP reminder 模式运行时行为——auto-post-on-close=true 提示模式下未核销 AR/AP 经 hasReminders() 列出但 closePeriod 不阻断，是否符合「前置门禁」期望（关联 P2-RC-006） | 实际启用强制核销模式（未文档化 config）+ 月末大额未核销 AR/AP 跑 closePeriod，观测 reminder 不阻断行为（闭合 P2-RC-006 决策） | **证实**：reminder 模式列出但不阻断（P2-RC-006 倾向接受）；**证伪**：强制核销模式已 hard block | 否（read-only 结账观测） |
| **A4.1.19** | A1.6 §7-2 | PC-4 资产折旧 auto-execute + 悬挂阻断交互——`runDepreciation` G3 容错跳过（impl 未就绪）与 `findUnresolvedDepreciationSchedules` 悬挂阻断的交互（折旧配置错误时 rethrow 阻断 vs 悬挂扫描是否双重报告） | assets 域部署 + 折旧配置错误（如 ERR_DEPRECIATION_RATE_MISSING）跑 closePeriod，观测 rethrow 阻断 vs 悬挂扫描报告行为 | **证实**：配置错误 rethrow 阻断 + impl 未就绪容错跳过语义清晰；**证伪**：双重报告或静默跳过致悬挂未阻断 | 否（read-only 编排观测，跨域 assets） |
| **A4.1.20** | A1.6 §7-3 | RC-9 反结账审计缺失的实际合规影响——无操作人/原因/时间记录；当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖可作降级审计证据（关联 P1-RC-006） | 实际反结账操作，核查 `updatedBy`/`updateTime` 是否被覆盖（提供降级审计证据，闭合 P1-RC-006 修复方案优先级裁决） | **证实**：通用审计列被反结账覆盖（降级证据可用，P1-RC-006 部分缓解）；**证伪**：通用列不被覆盖（合规可追溯性完全破坏） | 否（read-only 审计列观测；关联 P1-RC-006 修复触 ORM） |
| **A4.1.21** | A1.6 §7-4 | 年末反结账阻断边界——`ReverseCloseProcessor:32-36` 年末反结账阻断，若次年期间已手动删除但 ErpFinGlBalance/yearOpening 残留，反结账红冲年度结转凭证是否致次年年初余额与凭证不一致 | 12 月期间反结账（手动删次年期间后）观测红冲年度结转凭证 vs 次年残留 ErpFinGlBalance/yearOpening 一致性 | **证实**：阻断守卫有效（次年期间存在即拒绝）；**证伪**：手动删次年期间后绕过阻断致年初余额/凭证不一致 | 否（read-only 边界观测；测试数据构造非生产数据删除） |

### 2.7 A1.7 finance-F7 报表/看板/多账套（4 行）

| A4.1.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| **A4.1.22** | A1.7 SP-1 | cash flow 读 VoucherLine 不过滤 postingType，BUDGET/COMMITMENT 影子凭证是否含现金科目（1001/1002/1012/1031）行 → 现金流量表是否被影子凭证污染（静态推断：实操不触现金科目，低风险，但代码无显式守卫） | seed 含 BUDGET postingType + 现金科目行的凭证，跑 `buildCashFlowDataset` 断言是否计入现金流量 | **证实**：BUDGET/COMMITMENT 实操不触现金科目，cash flow 未被污染（低风险成立）；**证伪**：某 BUDGET/COMMITMENT 凭证触现金科目致现金流量虚增 | 否（read-only 报表观测） |
| **A4.1.23** | A1.7 SP-2 | 多账套部署（`multi-schema-enabled=true`）下「每账套独立三表」运行时渲染——当前读路径取主账套 FINANCIAL，非按账套切换渲染（`balanceSheetData(periodId)` 无 acctSchemaId 参数） | 多账套部署下按 acctSchemaId 参数切换报表渲染观测（当前无 acctSchemaId 参数） | **证实**：GlBalance 物理按 acctSchemaId 隔离已落地（写路径），读路径取主账套是合理简化（UC-FIN-05 ② 接受范围）；**证伪**：L1 UC-FIN-16「每账套独立三表」要求按账套切换渲染（升级评估） | 否（read-only 报表观测） |
| **A4.1.24** | A1.7 SP-3 | CLOSED 期间门控缺失的运行时数据完整性影响——`loadGlBalances` 按 periodId 取数，OPEN 期间数据可能不完整（未过账凭证不入 GlBalance）（关联 P2-RC-008） | OPEN 期间 + 未过账凭证场景跑 BS，对比 CLOSED 后 BS 差异 | **证实**：OPEN 期间 BS 数据不完整（P2-RC-008 数据完整性关注，非会计正确性破坏）；**证伪**：差异在可接受范围 | 否（read-only 报表观测） |
| **A4.1.25** | A1.7 SP-4 | 看板行级权限运行时过滤——`period.orgId` scope 在跨组织用户场景下是否泄漏（单组织种子 orgId=2 掩盖跨组织泄漏，A2.18 已证实；关联 P1-MA2-093） | 多组织部署 + 用户归属 orgA 但查 orgB 期间，断言看板数据是否泄漏（复用 P1-MA2-093 运行时确认） | **证实**：跨组织泄漏（P1-MA2-093 行级权限未落地）；**证伪**：scope 已隔离跨组织 | 否（read-only 权限观测；关联 P1-MA2-093） |

---

## 3. 跨报告去重依据（§去重协议）

> 方法论 §去重协议 + plan Phase 1 item 2：同根因同控制点合并为一行（显式标注合并依据 + 涉及切片）；不同根因/不同控制点各一行。

**去重结论：0 次合并**。25 条存疑点均为不同控制点，各自独立成行。1 对跨报告主题相关但控制点不同，保留独立行 + 交叉引用：

| 候选对 | 主题关系 | 控制点差异 | 裁决 |
|--------|---------|-----------|------|
| **A4.1.5**（A1.2 §7-2）↔ **A4.1.22**（A1.7 SP-1） | 均涉及「报表层对 BUDGET/COMMITMENT 影子凭证的过滤」 | A4.1.5 = **试算平衡/三表恒等式**对 COMMITMENT 单行单边凭证（header 借贷不平）的暴露（关注 GlBalance 聚合恒等式）；A4.1.22 = **现金流量表**（读 VoucherLine 非 GlBalance）对 BUDGET/COMMITMENT 现金科目行的污染（关注交易级现金分类）。不同数据源（GlBalance vs VoucherLine）+ 不同关注点（恒等式破坏 vs 分类污染） | **不同控制点，各自独立行 + 交叉引用**。注：A1.7 已静态证 BS/IS 安全（BUDGET/COMMITMENT 不入 GlBalance），A4.1.5 运行时确认聚焦独立试算平衡报表；A4.1.22 聚焦 cash flow VoucherLine 路径 |

> 其余 23 条均无跨报告同根因同控制点候选（各报告 §7 存疑点主题互不重叠：过账回链/Provider 普查、预算 config/承付凭证/释放路径、坏账状态轴/断言强度、银行勾对/调整凭证/导入幂等/自动红冲、FIFO+到岸成本数值/红冲/锁、期间 reminder/折旧交互/审计/年末、报表 cash flow/多账套/CLOSED 门控/看板权限）。
>
> **既有 arm MA2 证据去重**（§去重协议）：25 条均经各 MA1 报告 §7 显式标注为「静态存疑、需运行时确认」（未被既有 MA2 报告证实），符合展开条件。无存疑点的切片不产生行（A1.1-A1.7 均有存疑点，故均产生行）。

---

## 4. 计数自检（plan Phase 2 item 2）

| 报告 | §7 条目数（条目制） | 已展开为 A4.1.n | 编号区间 |
|------|-------------------|----------------|---------|
| A1.1 | 3 | 3 | A4.1.1 - A4.1.3 |
| A1.2 | 4 | 4 | A4.1.4 - A4.1.7 |
| A1.3 | 3 | 3 | A4.1.8 - A4.1.10 |
| A1.4 | 4 | 4 | A4.1.11 - A4.1.14 |
| A1.5 | 3 | 3 | A4.1.15 - A4.1.17 |
| A1.6 | 4 | 4 | A4.1.18 - A4.1.21 |
| A1.7 | 4 | 4 | A4.1.22 - A4.1.25 |
| **合计** | **25** | **25** | A4.1.1 - A4.1.25 |

**自检结论**：
- ✅ A4.1.n 行数（25）= finance §7 存疑点去重并集数（25，0 次跨报告合并）。
- ✅ 逐报告核对：A1.1-A1.7 各报告 §7 存疑点均已纳入（7/7 报告，无遗漏；无「无存疑点」切片）。
- ✅ 无抽样：每个 §7 存疑点恰好一行（完整枚举纪律 §3）。
- ✅ roadmap MA4 表 A4.1.1-A4.1.25 已追加（见 `requirement-compliance-roadmap.md` MA4 表）。

---

## 5. 触及保护区域标注汇总（§5 保护区域暂停协议）

> 展开器不执行验证；本汇总标注各 A4.1.n **验证探针**是否触及保护区域（供 mission driver 执行到该行时按 §5 暂停协议门控）。

| 保护区域类别 | A4.1.n 行 | 数量 | 门控 |
|------------|----------|------|------|
| 会计过账/成本过账行为探针 | A4.1.15（FIFO+到岸成本 delta 出库，无既有 E2E，新探针 exercise costing→posting 核心行为） | 1 | **ask-first + 独立 plan-audit**（该 A4.1.n plan 执行时） |
| 数据删除行为探针 | A4.1.16（`removeFifoAdjustLayer` 物理删除 delta 层，exercise 数据删除逻辑） | 1 | **ask-first + 独立 plan-audit**（该 A4.1.n plan 执行时） |
| 不触及（read-only 普查/既有 E2E 复用/JUnit 断言补强/报表·凭证·审计列观测/部署 config·doc 普查） | A4.1.1-A4.1.14, A4.1.17-A4.1.25 | 23 | 预授权自动执行（验证探针不改 ORM/过账核心/删除逻辑） |
| **合计** | 25 | 2 触及 + 23 不触及 | — |

> 注：A4.1.n 验证探针的 ask-first 门控属各 A4.1.n 实体行的保护区域门控（§5），**不在本展开器范围**。本展开器仅标注，不执行验证。关联 MR1 修复（P1-RC-002/004/005/006 + P2-RC-001/004/006/008 + P1-MA2-093）的 ORM/过账变更属各修复行的门控，不在本展开器。

---

## 6. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本展开器产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（方法论 §8），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本展开器**不**以 checker 脚本退出码作为门控通过依据。**本展开器无生产代码变更**（纯 roadmap 表追加 + 展开映射记录），checker 无回归风险。actual vs baseline 实测见下表。

  | 规则 | Baseline（machine-readable） | Actual（本展开器 HEAD 实测） | 状态 |
  |------|------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3-R12 | （既有基线） | 脚本输出在 R3 header 后截断（既有工具行为，非本展开器引入；本展开器零代码变更，无回归风险） | ✅（无回归风险） |

  > R1/R2 全部 actual == baseline，**0 漂移**。R3-R12 脚本输出截断是既有工具行为（与零代码变更的展开器无关）；权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 为准。本展开器不触发 CI（无代码变更）。

- [x] **closure-audit 独立性声明**：本展开器的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本展开器为**展开器工作项**（只读收集 + roadmap 表追加），**不新建 finding**。25 条 A4.1.n 行是运行时验证项的展开（非 finding 结论）；各 A4.1.n 验证结论才新建/复用 finding（届时按 §7 规则 grep arm-index 后裁决）。本展开器无未经比对直接新建的 finding。

---

## 7. 真相源冻结声明（§9）

本展开器未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。roadmap MA4 表追加 A4.1.1-A4.1.25 行 + A4.1 状态 `todo → done` 是工作项追踪更新（非冻结真相源），属展开器既定动作（plan Non-Goals 明示「roadmap MA4 表追加 A4.1.n 行是工作项追踪更新，属展开器既定动作」）。

---

## 8. Verdict

**Verdict: passes expander completion**（A4.1 展开完成）

- **展开完整性**：finance A1.1-A1.7 §7 存疑点全集（25 条，条目制）已逐条展开为 A4.1.1-A4.1.25，追加到 roadmap MA4 表，0 遗漏、0 抽样、0 误合并。
- **去重**：0 次跨报告合并（25 条均不同控制点）；1 对主题相关（A4.1.5 ↔ A4.1.22）保留独立行 + 交叉引用。
- **计数自检**：A4.1.n 行数（25）= §7 去重并集（25），逐报告核对 7/7 通过。
- **保护区域标注**：2 行触及（A4.1.15 成本过账探针 / A4.1.16 数据删除探针，ask-first 门控属各 A4.1.n plan），23 行不触及。
- **A4.1 状态**：done（展开完成，非验证完成）。
- **A4.1.n 状态**：todo（各自待独立 plan + 独立验证 done；mission driver 逐项 DRAFT_PLANS → 验证；触及保护区域行按 §5 暂停协议）。

**剩余工作**（非本展开器范围）：各 A4.1.n 实体行的运行时验证（各自独立 plan + 独立 done）。MA4 里程碑 done 判据 = A4.1 + A4.2 done + 全部 A4.1.n/A4.2.n done。
