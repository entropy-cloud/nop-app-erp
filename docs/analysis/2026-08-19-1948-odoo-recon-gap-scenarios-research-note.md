---
分析日期: 2026-08-19
类型: 外部文章未实现场景联网调研深化笔记（web 检索证据收集）
状态: 已完成
输入源: `docs/input/source-article-odoo-auto-reconciliation-engine.md`（原文转录件）+ `docs/analysis/2026-08-19-1925-odoo-auto-reconciliation-vs-nop-app-erp-design.md`（基准差异分析报告）
证据图例: 🔵=联网实测（web 抓取/检索返回的页面原文片段，可复核）/ ⚪=领域常识（通用知识，无独立出处）/ 🔵⚪=检索摘要佐证 + 领域常识交叉印证
证据诚实性: 按 `2026-06-30-0001-advanced-scenario-design-comparison.md` §1.2 与 `treasury.md:25` 范式标注；所有互联网来源均为厂商/服务商/社区文档，属「调研记载」层级，非本仓实测，落地前须按保护区域流程以 Nop 平台实测复核。
---

# Odoo 对账引擎三大未实现场景：落地模式联网调研笔记

> **本文档定位**：为差异分析报告（1925）中判定的 3 个未实现场景（PSP 批量 1:N / 模糊匹配 / 数电发票联动）+ 1 个部分实现场景（小额容差写销）补充「行业落地模式」证据，作为未来触发落地时的**调研起点**与 backlog 登记依据。**不修改任何 owner doc / design / architecture**（AGENTS.md rule 6：未实现场景不写设计文档章节）。

## 1. 场景 1：PSP 支付平台 1:N 批量对账（未实现）

### 1.1 行业标准落地模式（文献证据）

**核心范式 =「两段式对账」**，将「毛额↔销售」与「净额↔银行」拆成两笔独立核对，手续费是桥：

> 🔵 ReconFiles：`Gross payments to your sales records`（每笔 charge 对应一笔 sale/invoice，确认收入全录）+ `Net payout to your bank statement`（税后转账金额等于银行入账）；**桥 = 手续费**——`Gross charges − fees = net payout`，手续费是成本而非差异，单独记为费用。金额等式成立即对账成功。退货/拒付会减少后续 payout，须回链原交易。
> 🔵 Stripe 官方（bank-reconciliation / payouts）：payout 按 `po_XXX` Payout ID 唯一标识，`payout.reconciliation_completed` webhook 事件触发；Payouts report 含 `payout_date` / `matching_key`（payout 与银行流水之间的共同引用键）/ `payout_amount`（净额）/ `received_in_bank` / `reconciliation_status`。Stripe 原生 bank reconciliation 仅限美国直连账户 + 自动 payout 排程（Connect 账户 / 非美国 / 手动即时 payout 不支持）。
> 🔵 Tidy / 业界普及做法：银行流水（lump-sum payout）↔ Stripe Payouts 列表先匹配（金额+到达日期），再 Stripe Payments 列表 ↔ 内部交易明细匹配；**首次 payout 约 7–10 营业日后**，payout 内跨期（月末收集、次月初到账 = 在途存款，同标准银行调节表）。

**数据结构抽象（Odoo 生态承接）**：

- 中间表模型：settlement.batch（承接网关 payout/settlement 文件 CSV/Excel/API）→ 明细 settlement.line（gateway transaction id / gross / fee / tax on fee / net / currency / payout date）逐行对应一笔订单收款。
- Stripe 表现（🔵 文档）：单个 payment 处理为一笔 charge，累积后以 lump-sum payout 存入银行；payout 汇总 52 笔收款 £2,415 gross − £75 fees = £2,340 net 落入银行 → 银行流水一行，销售记录 52 行，**one-to-many**。

### 1.2 落地要点（映射到 nop-app-erp）

| 落地元素 | 模式证据要点 | 本项目落点示例（引用自 1925 报告） |
|---|---|---|
| Payout Batch 中间表 | batch(头：payout id/gross/net 净值/到账日) + line(交易 id/手续费/净额) | `ErpFinPayoutBatch/Line`（ORM 纯加性，model 保护区域 auto + dual-agent-approval） |
| 交易 ID 聚合键 | matching_key / po_XXX / gateway transaction id 是「点对面」匹配的公共引用键 | 匹配器按 Transaction ID 聚合未结收款项（AR/AP 辅助账） |
| 毛额/净额双检 | gross↔销售 与 net↔银行 两笔独立核对，fee 是桥且单独记账 | 差额归集「平台手续费/支付渠道费用」科目 |
| 不盲改单一流水 | 银行侧一行（payout）↔ 多笔组分；**先生成收款再按 payout 批量匹配更干净** | 建议「批量归集差额默认生成建议、人工确认后过账」（对齐文章置信度三级） |

### 1.3 现状结论（对齐 1925 报告）

未实现；本骨架无任何 PSP 业务来源实体。触发条件 = 出现平台结算/跨境电商业务需求。检索证实该场景是**成熟、有厂商标准做法**的领域，落地路径清晰，但属于垂直行业特性需求。

## 2. 场景 2：模糊匹配与智能文本清洗（未实现，已登记 successor）

### 2.1 算法选型证据

| 算法 | 特性 | 文献示例 |
|---|---|---|
| Jaro-Winkler | 短名字符串匹配字段标准；前缀加权；值域 [0,1] | 🔵 Reconart：变体处理拼写错误+词序；`Colour`↔`Color` ≈ 0.97；`ABC Corporation`↔`ABC Corp` ≈ 0.82 |
| Levenshtein | 编辑距离；对词序不敏感、长串代价高 | 🔵 Reconart：`ABC Corporation`↔`ABC Corp` 相似度 ≈ 0.80 |
| Token-based | 按词匹配、对词序鲁棒（如 rapidfuzz/结巴分词 + ngram） | ⚪ 文章原文方案（rapidfuzz + 结巴分词）+ 权重 名称40/金额50/时间窗口10 |
| SQL 内置函数 | SQL Server 原生 `JARO_WINKLER_SIMILARITY` / `EDIT_DISTANCE` | ⚪ 通用数据库能力；本项目 H2 兼容性须实测（落地时验证） |

### 2.2 分级与性能证据

- **置信度分级（三级）**：🔵 Optimus/Reconart 均建议 高(≥90-95)自动核销 / 中(≥85)建议+抽样复核 / 低(<85)人工或异常台——与文章坑点 3 及本项目 matchStatus 三态四值结构一致。
- **性能（blocking 分桶）**：🔵 Reconart：先按金额区间/日期窗口/首字符分桶缩小候选集，避免 O(n²) 全量比较（1 万条 → 5000 万对）。

### 2.3 落地要点（映射到 nop-app-erp）

| 落地元素 | 证据要点 | 本项目落点示例（1925 报告） |
|---|---|---|
| 候选评分排序 | 不动匹配主链路，在候选结果上叠加评分 | `BankLedgerQuery.findCandidates` 之上追加评分排序（纯 service 层，零 ORM 变更） |
| 只建议不自动过账 | 模糊匹配只产出建议，人工确认 | 评分 >85% 建议 + 人工确认（对齐坑点 3 + 会计严禁误配自动过账） |
| 文本清洗组件化 | 摘要去噪 Filter 挂匹配前 | 组件化 Filter（独立低风险） |
| 触发条件 | 实际误配率上升 | `bank-reconciliation.md:152` 已登记 successor（LIKE 模糊增强） |

### 2.4 现状结论

`bank-reconciliation.md:152` 已诚实登记「户名差一」SUSPENSE 语义缺失 + LIKE 模糊增强留待误配暴露后按需追加（Successor Required: no）。检索确认该类增强**业界成熟、算法有标准选择、成本最低**（无 ORM 变更），但当前无实际误配率数据支撑立项，维持 watch-only 正确。

## 3. 场景 3：汇率微小差额与抹零自动核销（部分实现）

### 3.1 行业模式额外证据（补强 1925 报告）

- 🔵 Odoo 生态：`allow_payment_tolerance` 容差可配**百分比或绝对值**两种类型，差额可自动生成 Write-off 计入费用科目；**非对称容差**常见（现金折扣允收 3% 少收、舍入尾差 $0.01–$1.00 允许抹零）。
- ⚪ 通用会计实践：舍入/汇率尾差写销分录计入「营业外收支」或「汇兑损益」，与文章及本项目 `EXCHANGE_GAIN_LOSS` 方向一致。

### 3.2 现状结论（对齐 1925 报告）

部分实现——AR/AP 侧核销汇兑差额凭证已实现（config-gated）；银行对账侧缺「单笔勾对的金额微量差额自动生成凭证强制置 MATCHED」能力。缺的维度 = 银行勾对侧容差写销。落地建议：`BankStatementMatcher.autoMatch` config-gated 容差分支 + 默认 0（关闭）+ 会计保护区域 plan-first + 防容差过大的审计约束（同文章坑点 3）。

## 4. 场景 4：数电发票联动（完全未实现，保护区域）

### 4.1 数电发票体系背景（⚪ 领域常识 + 🔵 文献佐证）

- 数电发票（全面数字化的电子发票）自 2021 年试点，**去介质、去版式、标签化**，全国统一赋码、自动流转交付；与纸质发票同等法律效力。
- **乐企（Natural System）**：国家税务总局向符合条件企业提供的「税务系统↔企业自有信息系统直连」服务平台，能力包括开票、归集、查验、勾选认证、入账、电子凭证归档、后续申报/风险管理（🔵 锦天城律所《乐企自用直连服务的规范指引》解读 + 🔵 开灵科技 + 🔵 知乎乐企试点指南）。
- **直连单位（平台所有者，集团总部/总分公司/控股主体）vs 使用单位（下属成员企业）** 两类主体，责任与监管义务分明；接入门槛高（网络/安全/系统设计/运维投入 + 数据安全连带责任）→ 大量企业走第三方服务商。

### 4.2 对接模式（两条路线）

| 路线 | 特征 | 文献证据 |
|---|---|---|
| 乐企直连 | 税局官方 API，大集团总对总，`交易即开票 / 开票即交付 / 交付即归集`；接口迭代不稳定需补偿机制 + RPA 备用；企业自研风控/红冲余额校验 | 🔵 发票通（fapiao.com）乐企数电页：IT 能力要求高、接口多变、需补偿机制；🔵 乐企平台服务商（leqifuwu.com）「一次对接、全局复用」架构：上层调用乐企能力、下层赋能 ERP/财务/合同系统 |
| 第三方服务商 | 百望云（国家队，服务 >1500 万企业）、金蝶发票云、泛微业票通、票总管、发票通、企享云等；提供开票/查验/认证勾选/入账/归档全流程 API；适配税控+数电双轨 | 🔵 泛微 2025 数电发票管理软件 TOP5 评测：百望云率先通过税局认证、乐企平台直连、发票池归集；其他均列「与 ERP 深度集成」 |
| 进项认证/勾选 | 抵扣勾选 / 不抵扣勾选 / 注销勾选 / 出口退税勾选 / 逾期勾选 + 统计确认，走税务数字账户；进项发票池（全量进项归集）是勾选认证和「账、钱、票」三相符落地前提 | 🔵 发票通：OCR 识别、采集、查验认证、发票入账、预警全流程用票管理；🔵 泛微评测：发票池 + 自动查验/勾选 |

### 4.3 与文章「账、钱、票三相符」的对应

文章场景 4 的意图 = 核销时校验发票核销状态（三相符）。落地映射：
- **票端能力**：开票（销项）+ 归集/查验/勾选（进项）→ 对应一个「发票凭证状态」维度。
- **联动点**：核销/付款审批时经 API 校验发票状态 + 异常锁报销/锁核销（🔵 乐企风控「异常发票自动锁报销流程」）。
- **本项目对接路径**：`nop-integration-api`（已有邮件/短信 SPI 的可插拔 provider 模式）接线第三方服务商 API；税务局/区域税制差异大 → 客户化运营层。

### 4.4 现状结论（对齐 1925 报告）

完全未实现 + 保护区域。`treasury.md:208` 的「外部票据系统对接属强本地化运营层、本项目不承担」立场可类推到税务接口（项目是产品骨架，非客户定制交付物）。技术上可行（乐企直连 vs 第三方 API 两条成熟路线），但属 external integrations + plan-first + owner doc + 测试，且随地区/税制变化，建议维持 **Non-Goal（客户项目定制层）** 不纳入产品基线。

## 5. 三场景对接路径与触发条件汇总

| 功能 | 行业成熟度 | 落地路径 | 保护区域门控 | 触发条件 | 建议 |
|---|---|---|---|---|---|
| PSP 批量 1:N | 成熟（厂商标准两段式） | ORM 加 `ErpFinPayoutBatch/Line` + 匹配器按交易 ID 聚组 + 差额科目凭证 | model（auto + dual-agent-approval）+ 会计过账（plan-first） | 平台结算/跨境电商业务来源出现 | backlog watch-only |
| 模糊匹配/文本清洗 | 成熟（Jaro-Winkler/Levenshtein/token + 分级 + blocking） | 候选评分排序 + 建议 UI + 人工确认 | 纯 service 层（零 ORM 变更） | 实际误配率上升（已登记 successor） | backlog watch-only |
| 银行对账小额容差写销 | 成熟（allow_payment_tolerance 比例/绝对值 + 非对称） | config + 差额凭证分支（复用 CloseVoucherWriter） | 会计过账（plan-first），默认容差=0 | 外币结算/舍入尾差业务需求 | 部分实现，补银行侧分支 |
| 数电发票联动 | 成熟（乐企直连 / 第三方 API 双路线） | `nop-integration-api` 接线 + 发票凭证/勾选状态模型 | external integrations（plan-first） | 税务数字化客户定制 | 维持 Non-Goal |

## 6. 证据来源

- 🔵 联网抓取/检索（调研记载层级，非本仓实测）：
  - Stripe 官方 bank-reconciliation 与 payouts 文档（payout 字段/po_XXX/matching_key/webhook）：`docs.stripe.com/bank-reconciliation`、`docs.stripe.com/payouts/reconciliation`
  - ReconFiles《payout reconciliation 两段式 + 手续费桥》：`reconfiles.co.uk/blog/reconcile-stripe-payouts.html`
  - Tidy Help《Reconciling Stripe Payouts》（lump-sum / 两段步骤）：`support.tidyhq.com`
  - Reconart《Fuzzy Matching in Financial Reconciliation》（Jaro-Winkler/Levenshtein 数值示例 + 分级阈值 + blocking）：`reconart.com/blog/fuzzy-matching-in-financial-reconciliation`
  - Odoo 生态 Payout Batch 中间表模型：`ecosire.com/apps/odoo/odoo-payment-reconciliation-settlement`、`odoo.com/forum/help-1/odoo-19-stripe-integration-reconciliation-309387`、`braincuber.com/tutorial/batch-payments-reconciliation-odoo-18`
  - Odoo 容差/写销：orange 类 Odoo 二开资料（allow_payment_tolerance 百分比/绝对值 + 非对称）
  - 数电发票背景/乐企：锦天城律所《乐企自用直连服务的规范指引》解读（allbrightlaw.com）、发票通乐企数电页（fapiao.com）、开灵科技数电乐企方案、乐企平台服务商（leqifuwu.com）、知乎乐企试点指南（zhihu.com/p/684589166）、泛微《2025 数电发票管理软件 TOP5 评测》（weaver.com.cn）、百度智能云乐企页（baiwang.com/m/leqi/）
- ⚪ 领域常识：数电发票改革背景、会计舍入/汇兑尾差写销记账规范、SQL 内置相似度函数可用性
- 基准：`docs/analysis/2026-08-19-1925-odoo-auto-reconciliation-vs-nop-app-erp-design.md`（§5 实现路径与保护区域表格）
- 项目锚点：`docs/design/finance/bank-reconciliation.md:152`（模糊匹配 successor）、`docs/design/finance/treasury.md:208`（外部票据系统 Non-Goal 立场）、`module-finance/erp-fin-service/.../bankrecon/`（自动勾对实现）