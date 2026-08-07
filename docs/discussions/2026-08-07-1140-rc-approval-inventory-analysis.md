# 需求-实现符合性审计：人工审批内容全景分析与审批登记

> 日期：2026-08-07
> 类型：审批盘点 + 问询记录（非审计报告；审计按各 MA 报告归口）
> 依据：`docs/audits/requirement-compliance-methodology.md` §4 三判据 / §5 预授权清单与暂停协议 / §9 真相源冻结 / Q4=(a) 裁决；`docs/context/ai-autonomy-policy.md` 保护区域表；`docs/backlog/requirement-compliance-roadmap.md` 预授权声明
> 目的：① 全量盘点 requirement-compliance mission 当前需要人工审批的内容；② 对有把握可自动批准的类目登记为**自动批准**；③ 对有疑问的决策点逐个提出 3 选项问题（推荐在前），并记录回答。

---

## §1 审批体系现状（事实基线）

| 通道 | 规则 | 依据 |
|------|------|------|
| 预授权自动执行 | 文档更新类修复（owner doc / use-cases / arm-index）；代码逻辑修复（BizModel / Processor / xbiz / view.xml） | roadmap 预授权声明 + methodology §5 |
| **须 ask-first + 独立 plan-audit** | ORM 结构变更 / 会计过账逻辑（VoucherFact / PostingProcessor 核心路径）/ 数据删除·迁移 / 未列明类目默认 ask-first | methodology §5 三类门控 |
| 真相源冻结 | product-scope / use-cases / owner doc 需求契约段的修订须人工批准 + 登记 | methodology §9 |
| 暂停协议 | 触及行 plan 须含 `- [ ] ask-first 人工确认` checkbox；driver 执行到触及行暂停，非触及行继续 | methodology §5 |
| Q4=(a) | P0/P1 必须实现，禁止方案 B；唯一出口 = 需求本身不合理经人工批准改 product-scope（需求变更非降级） | 讨论文档 2026-08-02-1700 |

**当前执行状态（2026-08-07 11:40 快照）**：

- MA1 全部 done；MA2/MA3/MA4（A4.1/A4.2 至 A4.2.123）done；A4.2.124-146 todo（两份草稿 plan `2026-08-08-0015-1`（cs）/`2026-08-08-0015-2`（master-data）已过独立草案审查）
- **MR1 R1.0 展开器 = todo（从未启动）**；MR0 无活跃行
- **已注册的"人工批准"记录共 6 处**，全部为 mission-driver / 代理授权形式（`2026-07-02-0900-1`、`2026-07-02-1000-1`、`2026-07-01-1900-1`、P0-MA2-016 fix plan、`2026-07-31-0010-2` P1-MA3-011 等），**无任何真实人类逐条签字记录**
- **零 MR1 修复行已执行**；A4.2.119（P1-RC-049 重建优先级成本归集）因 P1-RC-049 未落地保持 todo + blocker

> **注（2026-08-08 快照修正）**：上述 §1 快照时点为 2026-08-07 11:40。实仓 2026-08-08 已确认：cs `2026-08-08-0015-1` 与 master-data `2026-08-08-0015-2` **均已 completed**（A4.2.124-146 全 done，含 A4.2.130/137/144/146 checkbox 勾选 + Closure 审计通过 + roadmap 回写 done），表 G/§1 中「草稿/未勾选/active」描述均属历史时点，以此条修正为准。

---

## §2 需要人工审批的内容全量清单

### 表 A：ORM 结构变更类（须 ask-first + 独立 plan-audit）

| Finding ID | 域 | 变更内容（摘要） | 状态 |
|---|---|---|---|
| P1-RC-001 | finance | GlDistribution 机制缺失；新增 Validator 注入 FactsValidator 链 + 可能新增 ErpFinGlDistribution 实体 | todo |
| P1-RC-004 | finance | 自动勾对对方账号维度缺失；ErpFinBankStatementLine/VoucherLine 增 counterpartyAccount 列 | todo |
| P1-RC-006 | finance | 反结账审计轨迹缺失；ORM 增 reversedBy/reverseCloseReason/reverseCloseAt 列或新 ReverseCloseLog 实体（含会计过账逻辑） | todo |
| P1-RC-007 | finance | 现金流量三分类缺失；ErpMdSubject 增 cashFlowType 字段 | todo |
| P1-RC-008 | mfg | 物料预留写路径完全缺失；reservationStatus 字段或 ErpMfgMaterialReservation 实体（含跨域写） | todo |
| P1-RC-009 | mfg | BOM 快照原则缺失；ErpMfgWorkOrder 增 snapshotBomVersion 列 + 快照内容列 | todo |
| P1-RC-013（方案 B） | hr | 跨天打卡；方案 B 触 ORM（方案 A 纯逻辑预授权） | todo |
| P1-RC-016 | hr | 匿名 respondentHash 防重复；ORM UK 若增设则 ask-first（纯 BizModel 部分预授权） | todo |
| P1-RC-025 | sales | 换货功能完全缺失；ORM 无 returnType 列 + **product-scope 裁剪须人工确认** | todo |
| P1-RC-029 | assets | 方式 B 补提缺失；catchUp mutation + isCatchUp 列 + 会计过账重算 + **product-scope 裁剪须人工确认** | todo |
| P1-RC-036 | crm | ROUND_ROBIN 降级 ownerId 不设；assignmentMethod 挑人需 ErpCrmTeamMember | todo |
| P1-RC-040 | quality | 关键项否决缺失；orm.xml 增 isCritical 列 | todo |
| P1-RC-055 | cs | 计时器 session 未实现；新增 ErpCsTicketTimerSession 实体 | todo |
| P1-RC-056 | cs | 重复升级/L2-L3 结构性不可实现；ErpCsTicket 增 lastEscalationLevel 等 + ErpCsSlaPolicy 增 secondEscalationUserId 等 | todo |
| P1-RC-057 | cs | 质量联动全域未实现；ErpCsTicket/Action 增 NCR 关联列 + 可能新增 Job 实体 | todo |
| P1-RC-058 | cs | 知识库采纳统计缺失；ErpCsKnowledgeBase 增 usageCount 列 | todo |
| P1-RC-059 | cs | 调查延迟派发缺失；ORM 增 status/failedAt/retryCount 列（核心为 cron+notify 预授权） | todo |
| P1-RC-061 | cs | 履行引擎未实现；ORM 增 retryCount/lastError 列（须协调 notify 域 + nop-workflow） | todo |
| P1-RC-062 | md | SKU 独立停用 + 引用检查缺失；ErpMdMaterialSku 增 status 列 + IErpMdSkuReferenceChecker 接线（含删除路径 + **product-scope 须人工确认**） | todo |
| P1-RC-079 | contract | 文档仓库引擎缺失；ErpCtDocument 增 legalHold 字段 + **purgeDate→delete 数据删除 ask-first** | todo |
| P1-RC-090 | aps | 自动派工引擎缺失；operation-order-status dict 加 HOLD/ON_HOLD（含跨域 inventory/mfg 协调） | todo |
| P2-RC-001 | finance | 导入幂等 dedup key；ORM 加 bankTxnCode 列（或改代码去 limit 1 预授权） | todo |
| P2-RC-002 | finance | valueDate 简化；ORM 加 valueDate 列 | todo |
| P2-RC-043 | quality | verificationResult 承载；方案 A 触 ORM / 方案 B 纯文档预授权 | todo |
| P2-RC-045 | quality | QualityGoal 名称回写静默 no-op；ORM QualityGoal FK | todo |
| P2-RC-057 | md | minPrice 派生；方案 A 独立 minPrice 列 ORM / 方案 B 文档补注 | todo |
| P2-RC-058 | md | 条码 DB UK 缺失；UK_MD_MATERIAL_SKU_BARCODE | todo |
| P2-RC-059 | md | (materialId,isDefault) UK 缺失；方案 A 部分 UK / 方案 B 应用层守卫（推荐 B） | todo |
| P2-RC-061 | maintenance | 设备 IDLE 恢复；preMaintenanceStatus 快照列（视形态） | todo |

### 表 B：会计过账逻辑类（须 ask-first + 独立 plan-audit）

| Finding ID | 域 | 变更内容（摘要） | 状态 |
|---|---|---|---|
| P1-RC-001 | finance | FactsValidator 链注入（GlDistribution） | todo |
| P1-RC-002 | finance | prepareContext 汇率缺失守卫（拒过账替代静默回退 1） | todo |
| P1-RC-018 | purchase | PurAcctDocProvider.createFacts 增 PPV 行（差异从 1403 剥离） | todo |
| P1-RC-029 | assets | 补提重算过账（与表 A 行协同） | todo |
| P1-RC-030 | assets | DisposalAcctDocProvider 科目腿（1606 固定资产清理） | todo |
| P1-RC-049/050/051 | projects | ExpenseCostAggregator 状态/超预算归集 + InvPostingDispatcher PROJECT_COST 分支 | todo |
| P1-RC-052 | projects | ProjectSettlementAcctDocProvider 质保金分录 | todo |
| P1-MA1-010 | projects | TimesheetPostingDispatcher.buildEvent 多币种 setExchangeRate | todo |
| P1-MA4-017（reuse） | hr | 薪酬 270/290/300 过账接线（会计+ORM ask-first） | todo |
| P1-MA2-083（reuse 重开） | purchase | 承付恢复对称性（invoice/return reverseApprove 无 commit()） | todo |
| P2-RC-003 | finance | BankReconAdjustmentVoucherBuilder 汇率解析 + 汇兑损益科目 | todo |
| P2-RC-004（**升级 P1**） | inventory/finance | FIFO 物料到岸成本 delta 层结构性永不被消耗（CostAdjustmentService/FifoCostingStrategy） | todo |
| P1-RC-091 | finance | 试算平衡 BUDGET 过滤（R1.0 展开时须显式裁决是否属"核心路径"） | todo |
| P1-RC-092 | finance | SELECT FOR UPDATE 跨方言 TOCTOU（MySQL-RR 退化；锁语义改写） | todo |

### 表 C：数据删除 / 数据迁移类（须 ask-first）

| Finding ID | 域 | 变更内容 | 状态 |
|---|---|---|---|
| P1-RC-062 | md | 被引用 SKU 删除路径（引用检查守卫） | todo |
| P1-RC-079 | contract | 保留策略 purgeDate→删除逻辑 | todo |

### 表 D：product-scope 范围确认类（Q4 唯一合法出口：需求变更非降级）

| Finding ID | 域 | 待确认内容 | 默认倾向 |
|---|---|---|---|
| P1-RC-025 | sales | 换货是否在 product-scope 内（L1 UC-SAL-06 显式含换货） | 维持 P1 强制实现 |
| P1-RC-029 | assets | 方式 B 补提是否裁剪（L1 明确"先补提"） | 维持 P1 强制实现 |
| P1-RC-031 | inventory | 效期拦截是否裁剪（L1 明确要求"出库确认失败"） | 维持 P1 强制实现 |
| P1-RC-037/038/039 | crm | UTM 归因报表 + 区域 tier rollup 是否要求 | 维持 P1 强制实现 |
| P1-RC-062 | md | SKU 独立停用是否要求（L1 活跃要求） | 维持 P1 强制实现 |
| P1-MA2-062（reuse 复核） | inventory | completeTake 账实差异处置是否要求 | 维持重开 P1 复核 |

### 表 E：owner doc AI 自标 Deferred 重开类（§4 三判据均不成立 → Q4 强制实现）

| Finding ID | 域 | 自标位置 | 裁决 |
|---|---|---|---|
| P1-RC-008 | mfg | material-reservation.md:9-16 | 三判据不成立 → 重开 P1 强制 |
| P1-RC-009 | mfg | bom-and-routing.md §实现注记:147 | 三判据不成立 → 重开 P1 强制 |
| P1-RC-056 | cs | README.md:98 + sla.md:346 Non-Goal | 三判据不成立 → 重开 P1 强制 |
| P1-RC-061 | cs | service-catalog.md §9.1 "产品基线外 protected 扩展点" | 三判据不成立 → 重开 P1 强制 |
| P1-MA2-071（reuse） | contract | state-machine.md R1.22 resolved-via-deferral | 三判据不成立 → 重开 P1 强制 |
| P1-RC-063 | md | SPI doc "下游接线归 Deferred" | 三判据不成立 → P1（纯 SPI 实现预授权） |

### 表 F：真相源冻结修订类（use-cases / owner doc 契约段，须人工批准登记）

| Finding ID | 域 | 内容 | 修订类型 |
|---|---|---|---|
| P2-RC-005 | inventory/finance | StockQueue↔ErpInvCostLayer 命名漂移 | L1 use-cases + L2 costing-methods（或仅 L2 补注） |
| P2-RC-011 | purchase | L1 命名 GOODS_RECEIPT/PURCHASE_INVOICE vs 实现 PURCHASE_INPUT/AP_INVOICE | use-cases 命名对齐 |
| P2-RC-016 | sales | L1 命名 SALES_DELIVERY vs 实现 SALES_OUTPUT | use-cases 命名对齐 |
| P2-RC-012 | purchase | "已转订单"字段命名/语义漂移 | owner doc/use-cases 对齐 |
| P2-RC-006/007 | finance | period-close.md 补注（reminder 模式 / 反结账成本凭证 Non-Goal 交叉引用） | 纯文档（预授权，不涉冻结） |

### 表 G：在途 plan 中未勾选 ask-first checkbox（5 处）—— 历史快照，全部已收口

> ⚠ 本表为 2026-08-07 11:40 快照。实仓 2026-08-08：`0015-1`（cs）与 `0015-2`（master-data）均已 completed（checkbox 勾选 + Closure 通过 + roadmap done），仅 `r2-7` 的 `enable-action-auth=true` 翻转仍保持 config-gated successor（不翻转、不属本盘点待批项）。

| plan | 行 | 内容（历史快照） | 2026-08-08 状态 |
|---|---|---|---|
| 2026-08-08-0015-1（cs） | A4.2.130/137 | 只读确认（不实施修复，无修复义务） | completed ✅ |
| 2026-08-08-0015-2（master-data） | A4.2.144/146 | 只读确认；修复义务归 MR1（P2-RC-058 / P1-RC-062 触 ORM/删除 ask-first） | completed ✅ |
| 2026-07-31-0310-2（r2-7） | Phase 4 | `enable-action-auth=true` 翻转须人工批准 + 灰度（config-gated successor，当前保持 false） | pending（不翻转，不属本 MR 待批项） |

### 表 H：待执行 MA4 只读审计行（无需批准，仅登记）

- A4.2.124-142（cs，19 项只读运行时确认，plan `2026-08-08-0015-1` 已 done ✅）
- A4.2.143-146（master-data，4 项只读运行时确认，plan `2026-08-08-0015-2` 已 done ✅）
- **剩余 todo 42 行**：A4.2.147-185（39 行：maintenance/contract/b2b/drp/logistics/aps/notify 域只读审计）+ A4.2.3/79/119（3 行 MR1 successor 阻塞——P1-RC-008/031/049 落地前不可执行），全部为只读审计（预授权类目），不触及保护区域，无需人工批准，待后续自动执行

---

## §3 有把握自动批准项（登记为自动批准）

**依据**：roadmap 预授权声明 + methodology §5 预授权类目清单 + ai-autonomy-policy（预授权不触保护区域的可自动执行）。以下类目**确认自动批准**，driver 可自动执行，无需逐项人工签字：

| 类目 | 自动批准范围 | 已验证代表项 |
|------|-------------|-------------|
| **A1 文档更新类修复** | owner doc 设计参考段 / README / arm-index / 审计自文件（roadmap/log）修正 | 647c75b99（job-scheduling.md §3.15）；P2-RC-006/007 纯文档补注 |
| **A2 代码逻辑修复**（BizModel / Processor / xbiz / view.xml / DTO / XPT / 调度接线 / 纯测试补充） | 不触 ORM 结构 / 不触 VoucherFact·PostingProcessor 核心路径 / 不触数据删除 | P1-RC-003（DTO+XPT 三通道）、P1-RC-005（红冲调度接线）、P1-RC-010/042/053（测试/调度）、P1-RC-011/012/014/015（hr 纯逻辑）、P1-RC-017/019（pur 校验）、P1-RC-020-024/026-028（看板报表）、P1-RC-032-035/037-039/041（crm 纯逻辑）、P1-RC-054/060（cs 纯 BizModel）、P1-RC-063（SPI 实现）、P1-RC-072-077/080/084-088（contract/各域纯逻辑）、P2-RC-008/009/010/012/013/014/018/022-024/048-050/052-056（除 ORM 部分）/062-071/073-077/083/085-087 等 |
| **A3 P2 登记不强制** | P2 finding 仅登记 watch-only，无修复义务 | 全部 P2 |
| **A4 MA4 只读审计继续执行** | A4.2.124-146 及后续 todo 行（零生产代码变更） | 已审草稿两份 |

**自动批准的范围限定（关键）**：自动批准**不包括** §2 表 A/B/C/D/E 中任何触及 ORM 结构 / 会计过账核心路径 / 数据删除 / 真相源契约段的修复项——这些仍须人工批准（见 §4 问询）。

> **注（2026-08-08）：** Q3/Q4 已裁决后，表 A 中「纯加性 ORM 变更」（加列 / 加 UK / 新增实体，不改既有语义，判据见 §5 Q3 行机检细则）与表 B 中「收敛性会计修复」（使实现向 owner doc 契约收敛、不反向改契约段，判据见 §5 Q4 行）两个**子集**不再落入本段否定范围，按 §5 批量授权执行；其余（改既有语义 / 核心路径改行为 / 数据删除 / 契约段）仍受本段否定。

---

## §4 有疑问项与问询记录（3 选项，推荐在前）

> 回答由用户逐一选择，记录回填于 §5。

| # | 问题 | 选项 A（推荐） | 选项 B | 选项 C |
|---|------|---------------|--------|--------|
| Q1 | MR1 R1.0 展开器启动时机？ | **MA4 全部完成（A4.2.124-146 done）后启动 R1.0**，一次性汇总全部 P0/P1，按暂停协议逐行处理 | 立即启动 R1.0，与 MA4 并行 | 维持现状不启动（全部修复无限期挂起） |
| Q2 | §3 预授权类目（文档更新 + 代码逻辑 + P2 登记 + MA4 只读）确认自动执行？ | **确认**，按方法论 §5 自动执行 | 仅文档类自动，代码逻辑类逐项过目 | 全部逐项人工批准 |
| Q3 | 表 A ORM 结构变更类（约 28 项）授权方式？ | **逐项独立 fix plan + 独立 plan-audit + plan 内 ask-first checkbox 逐项确认**（暂停协议标准流程） | 一次性批量授权"纯加性 ORM 变更"（加列/UK/实体，不改既有语义） | 全部保持阻塞不修 |
| Q4 | 表 B 会计过账逻辑类（约 15 项）授权方式？ | **逐项独立 fix plan + 独立 plan-audit + ask-first checkbox 逐项确认** | 批量授权"收敛性修复"（使实现符合 owner doc 契约的修复） | 全部保持阻塞不修 |
| Q5 | P1-RC-025 换货（sales）product-scope 裁决？ | **维持 P1 强制实现**（L1 UC-SAL-06 显式含换货，不裁剪） | 人工批准裁剪（product-scope 登记，需求变更） | 暂缓裁决 |
| Q6 | P1-RC-029 折旧补提（assets）product-scope 裁决？ | **维持 P1 强制实现**（会计正确性类 Q4 无例外） | 人工批准裁剪（product-scope 登记） | 暂缓裁决 |
| Q7 | P1-RC-031 效期拦截（inventory）product-scope 裁决？ | **维持 P1 强制实现**（食品/药品合规风险） | 人工批准裁剪（product-scope 登记） | 暂缓裁决 |
| Q8 | P1-RC-062 SKU 独立停用 + 引用检查（md）裁决？ | **维持 P1 强制实现**（数据完整性，会计/数据安全类无例外） | 人工批准裁剪（product-scope 登记） | 暂缓裁决 |
| Q9 | 表 E owner doc AI 自标 Deferred 重开族（P1-RC-008/009/056/061 + P1-MA2-071；P1-RC-063 清查补入）裁决？ | **维持重开 P1 强制实现**（三判据不成立，Q4 无例外） | 经 §4 (ii) 补齐人工批准痕迹后维持 Deferred | 暂缓裁决 |
| Q9b | 表 E 漏列项 P1-RC-063（md SPI doc 自标）随 Q9 清查裁决？ | **维持 P1 强制实现**（三判据不成立；纯 SPI 实现预授权，无 ORM 变更） | 经 §4 (iii) product-scope 裁剪登记后 Deferred | 暂缓裁决 |
| Q10 | 表 F 真相源命名漂移修订（P2-RC-005/011/016/012）授权？ | **批准修订 use-cases 命名对齐**（登记变更理由 + 影响面 + 批准人，§9 流程） | 不改真相源，仅 owner doc 补注对齐 | 暂缓裁决 |

---

## §5 审批登记（回填区）

> 批准人：用户（人工逐项裁决）；批准时间：2026-08-07。裁决结果如下，选项号对应 §4 问询表。

| 问 | 裁决 | 说明 |
|----|------|------|
| Q1 | **维持现状不启动**（选项 C） | R1.0 展开器保持 todo，全部 P0/P1 修复继续挂起，直至后续另行裁决启动 |
| Q2 | **确认自动执行**（选项 A） | §3 A1-A4 四类目按方法论 §5 自动执行，无需逐项人工签字 |
| Q3 | **批量授权纯加性变更**（选项 B） | 表 A 中"加列/UK/新增实体、不改既有语义"的 ORM 变更批量授权；改既有语义或行为者仍须独立 fix plan + 独立 plan-audit |
| Q4 | **批量授权收敛性修复**（选项 B） | 表 B 中"使实现符合 owner doc 契约"的会计收敛修复批量授权；VoucherFact/PostingProcessor 核心路径改动行为仍须独立 plan-audit |
| Q5 | **维持 P1 强制实现**（选项 A） | P1-RC-025 换货不裁剪，product-scope 不变 |
| Q6 | **维持 P1 强制实现**（选项 A） | P1-RC-029 补提不裁剪，product-scope 不变 |
| Q7 | **维持 P1 强制实现**（选项 A） | P1-RC-031 效期拦截不裁剪，product-scope 不变 |
| Q8 | **维持 P1 强制实现**（选项 A） | P1-RC-062 SKU 独立停用 + 引用检查不裁剪，product-scope 不变 |
| Q9 | **维持重开 P1 强制实现**（选项 A） | 表 E 重开族（P1-RC-008/009/056/061 + P1-MA2-071）按三判据不成立维持强制；**P1-MA2-083 不属重开族**——归表 B 会计收敛类，随 Q4 批量授权 + 独立 plan-audit 回队（A4.2.31/45 已证纯 Processor 逻辑调既有 commit() 入口预授权） |
| Q9b | **维持 P1 强制实现**（随 Q9 清查补） | P1-RC-063（md SPI doc 自标）三判据不成立，维持 P1 强制（纯 SPI 实现预授权，不触 ORM/会计/删除） |
| Q10 | **批准修订 use-cases**（选项 A） | P2-RC-005/011/016/012 命名对齐修订经批准，按 §9 登记变更理由 + 影响面 + 批准人 |
| 自动批准类目（§3 A1-A4） | **确认** | 文档更新 / 代码逻辑 / P2 登记 / MA4 只读四类自动执行 |
| **生效收口（§6）** | **自 2026-08-08 生效** | 见 §6 覆盖声明：Q1 覆盖 MR1 自动展开时机；Q3/Q4 覆盖 ORM/会计两列预授权；Q10 触发 §9；§3 A1-A4 与否定范围不废止 |

---

## §6 本裁决生效与范围（审批登记的正式收口）

> 本段为本次审批盘点的收口声明，明确裁决对既有机制的覆盖范围与执行衔接，供 driver / 后续会话按此自动执行。

**生效**：本文件 §5 裁决自 **2026-08-08 生效**，是 Q1-Q10（含 Q9b 清查补）的唯一权威登记。

**覆盖声明**：

- **Q1（不启动 R1.0）覆盖 methodology §10 / MR1 的「MA1-MA4 完成后 R1.0 自动展开」**：R1.0 保持 `todo` 不启动，直至另行人工裁决。driver 不得在 MA4 done 后自动触发 R1.0。
- **Q3（纯加性 ORM 批量授权）覆盖 ORM ask-first 门**：限于 *加列 / 加 UK / 新增实体，不改既有语义 / 无 NOT NULL 无默认值列 / 无涉及既有数据的 UK 增设（须数据变更评估）/ 无删除、迁移、索引结构改造*，超出即回落独立 fix plan + plan-audit。
- **Q4（收敛性会计修复批量授权）覆盖会计核心路径门**：限于 *使实现向 owner doc 契约收敛的修复*，**不得反向修改 owner doc 契约段**（§9 冻结不因此解除）；核心路径（VoucherFact / PostingProcessor）改动行为仍须独立 plan-audit。
- **Q10 触发 §9 真相源修订流程**：P2-RC-005/011/016/012 use-cases 命名对齐修订按 §9 登记变更理由 + 影响面 + 批准人。
- **不被废止的**：§3 A1-A4 预授权范围及其否定范围（Q3/Q4 的扩权仍受 §3 限制）；Q4=(a) P0/P1 强制实现禁方案 B；§5 其余行。
- **后续动作**：MA4 完成后，driver 将本裁决回写 `docs/backlog/requirement-compliance-roadmap.md`（预授权声明追加 Q3/Q4 批量授权行 + MR1 R1.0 行标注「Q1 人工裁决：不自动启动，待另行裁决」），并更新 `docs/audits/requirement-compliance-methodology.md §5 预授权类目清单` 登记批量授权边界，确保 cold-start driver 只读 roadmap + methodology 即可读到本裁决。
