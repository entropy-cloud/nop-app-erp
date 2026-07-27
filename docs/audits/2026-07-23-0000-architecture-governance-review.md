# 架构治理审查：Architecture Governance Prompt 全量复盘

**审查日期**：2026-07-23 00:00（v2 — 经 2 路独立子代理审查 + 主代理证据复核后修订）
**审查类型**：架构治理审计（既有结构审查 + 漂移审计，`rot_audit` + `architecture_review` 双路由）
**审查方法**：主代理读取 5 份核心 owner docs + 2 路并行 `explore` 子代理（跨域访问漂移 / Rot 指标）→ v1 → 2 路独立 `general` 子代理冷重播挑战（事实严谨性视角 / 遗漏公平性视角）→ 主代理复核关键异议（实仓 grep + 读取遗漏文档）→ v2
**审查依据**：`C:\can\nop\attractor-guided-engineering-template\docs\skills\architecture-governance-prompt.md`（8 核心问题 + Design Review Matrix + 9 Rot Indicators + Review Rules）
**审查对象**：`nop-app-erp` 全仓（19 个 `module-*/` + `app-erp-all` + `docs/`）

---

## v1 → v2 修订摘要

两份独立审查一致认为 v1 整体可信度"中"（非"高"），主要异议已全部由主代理实仓复核确认：

| 异议 | 来源 | 复核结果 |
|---|---|---|
| F1 把 `SafetyStockEngine.java:135` 列为"跨域写" | MF-1/M3 双源 | ✅ `ErpInvDrpSafetyStockCalc` 实属 drp 域（`app.erp.drp.dao.entity.*`），是**同域写**；与 F7 自相矛盾 |
| F1 把 `MaintenanceIssuePostingDispatcher.java:174,186,195,206,217` 列为"跨域写" | MF-2 | ✅ 全是 `getEntityById`/`findAllByQuery` 读操作 |
| 漏看 `docs/architecture/posting-exemptions.md` | MF-3/M1 双源 | ✅ 已存在，登记 MrpReleaseService + ErpCtRebateSettlementBizModel 跨域写豁免（含理由/风险/补偿/收敛条件） |
| 漏看 `docs/plans/2026-07-16-2134-1-*` 已完成计划 | M1 | ✅ 已 completed，确认生产 daoFor 实测 **965 处**（v1 报 142）+ 6 类分类 |
| "仅 1 处有绕过注释" | MF-4/M2 双源 | ✅ 实际至少 **10 个文件**有显式跨域注释（contract 3/b2b 2/aps/master-data/quality/crm/md-dao） |
| 漏看 `docs/audits/nop-compliance-checker.sh` | M4 | ✅ 已存在（312 行、11 规则 R1-R11），覆盖 daoFor/saveEntity/@Inject private/Processor 等；但无 CI 集成、无 last-failed 基线 |
| F4 `ErpFinBusinessType` import 数 66 | S4 | ✅ 实测 **137 个文件**（v1 低估 2 倍） |
| F2(e) drp "反向命名空间污染" | N1 | ✅ 3 个 dict 文件名都带 `drp-` 前缀，是目录归属语义不明，非"冒充 inv" |
| 漏 19 个 `@Disabled` web 测试 | M5 | ✅ 19 域每个一个 `Erp*WebPagesTest.java` 全 `@Disabled` |
| Core Question 7（Governed Path Cheapest）未答 | S3 | ✅ contract/aps 多处注释说明 I*Biz 真实成本（依赖级联/破坏单模块测试） |

---

## 执行摘要

nop-app-erp 的**主脊健康**——DAG 单向依赖、`I*Biz` 写契约、ORM 模型驱动生成纪律均落地：

- 532 处合规 `I*Biz` 接口注入，0 处跨域内部 impl 类穿透
- `-web` 层零跨域 import
- 693 个 `@Inject` 字段全部包级可见（Nop IoC 规则零违规）
- 0 个 `@SqlLibMapper` 跨域使用
- 22,413 个 `_` 前缀生成文件（**口径调和**：实测 git-tracked `_` 前缀 java/xml = 797，any-ext = 2,238；22,413 不匹配任何 git-tracked 口径，推测为口误/位序错排或含工作树未跟踪文件，以 797 为权威——见 `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md §1.2`）。最近 2 月 `_app.orm.xml` 94 次 commit（全历史）均成对出现源模型变更（**已 content-diff 实证**：19 个 `_app.orm.xml` 全量 94/94 配对 model 源 + 全 115 commit 级配对扫描零真手编辑漂移，详见闭包项 #12）

漂移与可改进点集中在以下几处（按风险降序）：

- **F1（HIGH）** daoFor 跨域访问：生产代码 965 处 daoFor 中，~110-180 处是真违规子集（Type 1 ORM 导航可替代 + Type 4 设计边界错误），其中跨域**写**仅 2 处已登记豁免（`posting-exemptions.md`）+ 1 处半治理（b2b→pur）。
- **F2（HIGH）** 字典与状态枚举真相碎裂：`approve-status.dict.yaml` 在 9 域字节级重复。
- **F8（MEDIUM，新增）** 既有 compliance checker 未集成 CI、无 last-failed 基线——guard 可能是 dead armor。
- **F9（MEDIUM，新增）** 19 个 web 冒烟测试在模块级构建系统性跳过。
- **F4（MEDIUM）** finance/master-data 成为隐性共享内核（`ErpFinBusinessType` 被 137 文件跨域 import）。
- **F3/F5（MEDIUM）** ORM DAG 边表格维护瑕疵 + notify 子系统无 owner doc。

**核心修订**：v1 把已治理豁免误判为静默漂移、把同域写/读操作误判为跨域写、统计数字虚高（混入测试与同域）。v2 引入项目既有的 `posting-exemptions.md` + `2026-07-16-2134-1` daoFor 分类计划作为前置工作，把 F1 的"系统性绕过"改为"~110-180 处真违规子集 + 已登记豁免 + 已有 9 类合规分类"。

---

## Design Review Matrix

> 按 Architecture Governance Prompt §Design Review Matrix 的 8 槽位。每行需有具体 file/contract/test/owner-doc 引用。

| Slot | 答案 |
|---|---|
| **Spine** | DAG 单向依赖 + 每域独立 Maven 工程 + `I*Biz` 跨域写契约（`module-boundaries.md` / `domain-module-split-analysis.md §4.1`）。主脊健康。 |
| **Surface** | 写契约（`I*Biz`，`@BizMutation`）等级清晰；但 daoFor 数据访问层（965 处）退化为半公共表面，仅 11 条规则 checker 监管，未集成 CI。 |
| **Truth** | `*.orm.xml` 真相源健康（最近 2 月 94 次 commit 配对源模型，**content-diff 抽样已实证零手编辑漂移**——见闭包项 #12 + `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md`）；字典/状态枚举真相碎裂（F2）。 |
| **Ownership** | 模块级 DAG 清晰；finance/master-data 事实上成为隐性共享内核（`ErpFinBusinessType` 137 文件 import）；`notify` 子系统无 owner doc（F5）。 |
| **Negative path** | 业财一体失败路径清晰（`posted` 兜底 + 兜底扫描）；2 处跨域写豁免的失败/回滚路径已在 `posting-exemptions.md` 显式记录；其余 daoFor 跨域写无文档承载。 |
| **Time** | 重试/重放由 `posted` 标志 + `idempotency-pattern.md` 覆盖；daoFor 跨域写无独立幂等性论证。 |
| **Guards** | **既有 `nop-compliance-checker.sh`（11 规则）但无 CI 集成、无 last-failed 基线日志——guard 可能是 dead armor**（见 F8）。这是本审查最重要的修订：v1 错误陈述为"无机器检查强制"。 |
| **Budget** | 概念增长：finance 77 mutation / hr 64 / mfg 58；累计 42 Provider + 32 Dispatcher + 28 Engine + 11 Resolver + 3 Orchestrator（集中分布在 finance/inventory/mfg 的过账与定价链路）。抽样均有 owner doc 背书，但未全量核对（见残留风险 2）。 |

---

## Findings（按风险降序）

### 🔴 F1 — daoFor 跨域访问真违规子集 + 已登记豁免（HIGH）

> **解决状态（2026-07-24，plan `2026-07-24-0930-3`）**：(闭包项 #1) governed path 成本评估 ✅ Done——裁决=分支 (b)，I*Biz 强注入会破坏单模块测试（实测 contract 37/aps 22 全绿因回避 I*Biz），Type 1 可安全重构、Type 4 需平台解耦，裁决文档 `docs/analysis/governed-path-cost-evaluation.md`；(闭包项 #2) `ErpB2bAsnBizModel` 跨域写豁免 ✅ Done——已补登 `posting-exemptions.md`。真违规子集 Type 1+4 的 ~110-180 处重构归 successor（依赖 Phase 1 裁决前置条件）。
>
> **进展（2026-07-24，plan `2026-07-24-0605-3`）**：Type 1 第一批 safe 子集重构 ✅ Done——18 处 `daoFor().getEntityById()` → ORM `<to-one>` 关系 getter（assets 12/finance 5/mfg 1），ORM-gap=0，全 154 模块 BUILD SUCCESS + 受影响域单模块测试全绿（ast 78/fin 264/mfg 136）。checker 基线下降：R2b 319→317 / R2c 1108→1090 / R2d 34→31。剩余 Type 1 估算≈82-132 处分布在其余 14 域，为按域分批 successor。
>
> **收尾（2026-07-24，plan `2026-07-24-2000-1`）**：Type 1 第二批 + `getEntityById(FK)` chained 模式全域收尾 ✅ Done——累计 19 处 `daoFor().getEntityById()` → ORM `<to-one>` 关系 getter（drp 2/logistics 2/maintenance 2/manufacturing 9/hr 2/sales 1/finance 1），ORM-gap=0，全 154 模块 BUILD SUCCESS + 受影响域单模块测试全绿（mfg 136/sal 119/mnt 54/hr 113/drp 34/log 23/fin 264）。checker 基线下降：R2b 317→314 / R2c 1090→1071 / R2d 31→27。两批累计重构 37 处，`getEntityById(FK)` **chained** 模式生产站点全域清零（仅余 2 处 Type 5 dashboard + 2 处 ErpCtRebateSettlementBizModel Non-Goal 排除）。闭包审计修正：初始单行 grep 漏看 4 处多行 chained 站点，经独立结束审计发现并补重构（19 = 15 单行 + 4 多行）。`findAllByQuery` Type 1 评估：113 处站点可机械替换候选 <10 → watch-only residual。MrpReleaseService 同域只读 3 处局部重构（选 A）。Type 1 chained 工作流**收尾**。
>
> **variable-split 收尾（2026-07-24，plan `2026-07-24-0941-1`）**：`getEntityById(FK)` variable-split 子模式（`IEntityDao<X> dao = daoFor(X); dao.getEntityById(FK)`）全域分类 + safe 重构 ✅ Done。多行 regex 权威枚举 58 文件（>> 候选清单 16 站点）。三态分类：safe Type 1 = 8 处（重构为 ORM getter）/ Type 2 会话存活豁免 = 7 处 voucher-by-link 循环（登记保留）/ ORM-gap = 1 处（`ErpPrjProjectSettlementProcessor:270` `assetCardId` 弱指针，DAG 环约束）/ 其余 ~90 处 not-Type-1（raw ID 参数 load-by-id）。checker 基线下降：R2c 1071→1065（-6）。R2b/R2d/R2a 不变。全 154 模块 BUILD SUCCESS + 4 受影响域测试全绿（inv 114/ast 78/prj 67/fin）。**`getEntityById(FK)` chained + variable-split 两形态全域清零**，Type 1 `getEntityById(FK)` 工作流**收尾**。残余：`findAllByQuery` watch-only residual + Type 4 阻塞 successor + helper-wrapped chained-via-helper 变体（独立 successor 边界）。

**违反**：
- `AGENTS.md`："跨实体访问：始终为其他实体注入 `I*Biz` 接口。仅当 `I*Biz` 无法满足需求时才使用 `IDaoProvider` / `IOrmTemplate` / `@SqlLibMapper`，并在代码注释中记录原因。"
- `cross-domain-constraints.md §写引用`："跨域写必须经接口封装业务规则，禁止直接 ORM 跨域写。"
- `data-dependency-matrix.md §5.3`："禁止用 `IDaoProvider` / `IOrmTemplate` 直接跨域查表。"

**前置工作（v1 漏看，v2 引入）**：

1. `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（已 completed）实测生产代码 `daoFor()` = **965 处**（v1 报 142，因未排除测试与同域），分 6 类：

   | 类型 | 估算处数 | 处理策略 |
   |---|---|---|
   | 1. ORM 导航可替代 | ~100-150 | **应重构（真违规）** |
   | 2. 同域子实体 | ~300-350 | 可接受 |
   | 3. Processor 架构约束（H-4 裁决认可） | ~150-200 | 保留，文档化 |
   | 4. 设计边界错误 | ~10-30 | **应重构（真违规）** |
   | 5. 看板/报表只读聚合 | ~80-120 | 保留，文档化 |
   | 6. 历史残留 | ~100-150 | 逐步清理 |

   **真违规子集 = Type 1 + Type 4 ≈ 110-180 处**（不是 v1 的"28 个含写动词文件"）。其余 ~785-855 处是已分类的合法或可接受模式。

2. `docs/architecture/posting-exemptions.md`（v1 完全漏看）显式登记 2 处跨域**写**豁免，每条含理由/风险/补偿机制/收敛条件：
   - `MrpReleaseService`（mfg→pur，写 `ErpPurOrder(Line)`，理由：MRP 字段有限无法走通用 save；收敛条件：待采购域提供 `createFromMrpLine`）
   - `ErpCtRebateSettlementBizModel`（contract→pur/sal，写负额发票；理由：避免服务依赖级联；收敛条件：待提供 `createCreditMemo`）

**已治理例外（不再算违规）**：

| 文件:行 | 源→目 | 行为 | 治理状态 |
|---|---|---|---|
| `MrpReleaseService.java:139,154` | mfg→pur | 跨域写 `ErpPurOrder(Line)` | ✅ 已登记豁免（`posting-exemptions.md §MrpReleaseService`，含收敛条件） |
| `ErpCtRebateSettlementBizModel.java` | contract→pur/sal | 跨域写负额发票 | ✅ 已登记豁免（`posting-exemptions.md §ErpCtRebateSettlementBizModel`） |

**半治理违规（有代码注释，无架构登记）**：

| 文件:行 | 源→目 | 行为 | 治理状态 |
|---|---|---|---|
| `ErpB2bAsnBizModel.java:212,223` | b2b→pur | `daoProvider().daoFor(ErpPurReceive.class).newEntity()` + `saveEntity(receive)` | ⚠️ :59 有 javadoc 解释，但 `posting-exemptions.md` 未登记 → **应补登记** |

**v1 误判样本（已删除）**：

| v1 主张 | 复核结论 |
|---|---|
| `SafetyStockEngine.java:135` 是 drp→inv 跨域写 | ❌ `ErpInvDrpSafetyStockCalc` 属 drp 域（`app.erp.drp.dao.entity.*`），是**同域写**；该文件真正的跨域问题是 :8-9 import `ErpInvStockMove`(Line) 做**只读聚合**（:52 注释说明） |
| `MaintenanceIssuePostingDispatcher.java:174,186,195,206,217` 是跨域写 | ❌ 全是 `getEntityById`/`findAllByQuery` 读操作；finance 写路径走 `executor.postEvent()` → `IErpFinVoucherBiz`（合规 I*Biz） |

**绕过原因注释覆盖（v1 修正）**：

v1 错误断言"仅 1 处有"。实际至少 **12 个文件**有显式跨域 bypass rationale 注释（实仓 narrow grep 验证）：

- contract 域 3 处：`ErpCtRebateSettlementBizModel`、`ErpCtRebateAgreementBizModel`、`ErpCtInvoicePlanBizModel`（"避免服务依赖级联"）
- b2b 域 2 处：`ErpB2bAsnBizModel`、`UblInvoiceEdiProvider`
- aps 域：`ErpApsAtpCtpServiceImpl`（ATP/CTP 跨域选择理由）
- quality 域：`NcrReturnOrchestrator`（跨域 Bean 延迟查找）
- manufacturing / drp / pur / sal 等域亦有 bypass rationale 文件

**覆盖不均**：contract/b2b/aps 域注释详尽，**finance/projects/maintenance/logistics 域缺失**——这才是真 gap，比"几乎全无"更可操作。

> **注**：grep 命中也包括少量**合规声明类**注释（如 crm 域声明"走 I*Biz 而非 IDaoProvider"），与 bypass rationale 性质不同；本统计已尽量区分。

** governed path 真实成本（Core Question 7）**：

Architecture Governance Prompt §7 要求回答"if people keep bypassing the intended path, what cost is selecting the bypass?"。仓库内多处注释说明 I*Biz 的真实成本（**不是偷懒**）：

- `ErpCtInvoicePlanBizModel.java` 类 Javadoc："硬注入跨域发票 BizModel 会将其完整服务依赖链（sales→inventory→...）级联进合同域，破坏其隔离单元测试"
- `ErpApsAtpCtpServiceImpl.java` 类 Javadoc："跨域 I*Biz 强注入会在 aps-service 单模块部署/测试时因依赖模块未组装而启动失败"

**结论**：整改前必须先评估 I*Biz 注入对单模块测试启动的影响（可能需要 Nop 平台层的 lazy/SPI 解耦），再封堵 DAO 直访——否则会重复制造同样的痛点。

**伤害**：真违规子集虽小（~110-180 处）但：① 消费者无法被工具枚举；② 失败/回滚路径部分无文档（如 b2b→pur）；③ checker 已能检测但未集成 CI（F8）。

---

### 🔴 F2 — 字典与状态枚举真相碎裂（HIGH）

> **解决状态（2026-07-27，plan `2026-07-24-0930-2` + successor `2026-07-24-0605-2` + `2026-07-24-1600-1` + `2026-07-26-0300-1`）**：(a) approve-status 字典字节级重复——8 份无 ORM 消费者的 per-domain 文件已移除；**inventory ORM ext:dict 统一——RELEASED（successor `2026-07-24-1600-1` 已完成：inventory 5 列 `ext:dict="erp-inv/approve-status"` → `wf/approve-status` + 内联 dict 移除 + YAML 删除 + `ErpInvDaoConstants extends ErpInvDocStatus` 兼容；154 模块 BUILD SUCCESS + inv 114 测试 0 失败 + checker 零回归；inventory per-domain `approve-status.dict.yaml` 现已彻底删除）**；(b)(c) Java 常量重复——D1 `Erp*DocStatus` 接口已推广至 cs/mnt/mfg/qa（全域 9 域）；**(d) 硬编码字面量→`Erp*DocStatus` 常量替换——RELEASED（successor `2026-07-24-0605-2` 已完成：服务层全 9 域 doc/approve 轴字面量替换为常量引用，含 inv/mfg 本地重复常量定义消除与跨域 MrpRelease/NcrReturn 收敛，R3/R11 checker 基线无回归）**。**doc-status 6 域共享 dict 统一——RELEASED（successor `2026-07-26-0300-1` 已完成：6 域 28 列 ext:dict 统一为 `erp/doc-status` + 共享 dict 创建于 `module-common-service` + 6 处内联 dict + 6 份 per-domain YAML + logistics stale 清理 + `ErpMntDaoConstants extends ErpMntDocStatus` 兼容；154 模块 BUILD SUCCESS + 6 域+logistics 测试 0 失败 + checker 零回归）**。**cs `time-entry-approve-status`——永久裁决特化（Successor: no）**：值集合 PENDING/APPROVED/REJECTED ≠ wf/approve-status，为合法特化非冗余。详见闭包前必须项 #3（✅ Done，全部子项已闭合）/ #9（✅ Done）。

**违反**：
- Architecture Governance Prompt §3 "Keep One Truth"
- Architecture Governance Prompt §Rot Indicators "duplicate truth: the same rule or list encoded in multiple places"

> **修订**：v1 引用 `system-baseline.md §字段与类型约定` 作为"统一 owner"违规依据是**推断**（该条款仅约束 `valueType=string`，未规定字典归属）。本 finding 的违规依据改为仅引用 Architecture Governance Prompt 的两条明文规则。

**证据**：

**（a）字典文件跨域字节级重复**：

| Dict 文件 | 重复域数 | 域列表 |
|---|---|---|
| `approve-status.dict.yaml` | **9** | erp-ast, erp-cs, erp-fin, erp-inv, erp-mfg, erp-mnt, erp-pur, erp-qa, erp-sal |
| `doc-status.dict.yaml` | **7** | erp-ast, erp-cs, erp-log, erp-mnt, erp-pur, erp-qa, erp-sal |
| `priority.dict.yaml` | 3 | erp-mfg, erp-mnt, erp-prj |
| `contract-type.dict.yaml` / `contract-status.dict.yaml` | 各 3 | erp-cs, erp-ct, erp-hr |
| 其余 10+ dict | 各 2 | 各域 |

`approve-status.dict.yaml` 在 9 域**字节级完全相同**（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。

**（b）Java 常量类重复**：38 个 `Erp*Constants.java` 重复声明同一组状态字面量。

**（c）跨域同字面量常量重复声明**：同一字面量 `"CANCELLED"` 在多域以不同常量名重复声明——`ErpDrpConstants.SAL_DOC_STATUS_CANCELLED`（:59）、`ErpMfgConstants.SAL_DOC_STATUS_CANCELLED`（:126）、`ErpCrmConstants.DOC_STATUS_CANCELLED`（:18，命名前缀不同但字面量相同）。这是无共享枚举类型的症状——`2026-07-16-2134-1` D1 已为 purchase/sales 建立 `Erp*DocStatus` 接口先例，但全域未推广。

> **注**：`docs/plans/2026-07-16-2134-1` 的 Decision D1 已建立共享常量接口先例（`ErpPurDocStatus` 在 `-dao` 模块，`ErpPurConstants extends ErpPurDocStatus`），但仅落地 purchase/sales 两域，未推广。

**（d）服务层硬编码状态字面量**：`"DRAFT"` 126 处、`"APPROVED"` 73 处、`"UNSUBMITTED"` 41 处。**[RELEASED 2026-07-24，successor `2026-07-24-0605-2`]** 服务层 doc/approve 轴裸字面量已替换为 `Erp*DocStatus` 常量引用（含 mfg approve 轴；异语义/mfg doc-status 轴/跨域镜像常量定义按 Phase 1 语义轴判定排除，见 `docs/audits/hardcoded-status-literal-inventory.md`）。

**（e）drp 在 erp-inv 命名空间下放置字典**（v1 措辞"反向命名空间污染"过强，已修订）：

`module-drp/erp-drp-meta/_vfs/dict/erp-inv/` 下有 3 个 dict 文件：`drp-service-level.dict.yaml`、`drp-ss-method.dict.yaml`、`drp-xdock-status.dict.yaml`。文件名都带 `drp-` 前缀（未冒充 inv 字典），但**目录归属语义不明**——按字典描述对象归 inv 邻接？还是按拥有者归 drp？需裁决。

**伤害**：状态语义漂移无防御；字典唯一真相源失效。

---

### 🟠 F3 — ORM 跨业务域 DAG 边登记不完整（MEDIUM，已降级）

> **解决状态（2026-07-24，plan `2026-07-24-0930-3` Phase 2）**：✅ Done。§5.6.2 汇总表已补全 drp→inv `ErpInvStockMove` + mfg→inv `ErpInvBatch` + mnt→ast `ErpAstAsset` 三边（均标注批准保留），同步 §6.5。详见闭包前必须项 #7。

**违反**：`data-dependency-matrix.md §5.6.2` 依赖方向矩阵汇总表。

> **修订**：v1 把所有 5 条边都定性为"文档与代码漂移"过于严厉。复核显示部分边在多处 owner doc 已记载，仅 §5.6.2 汇总表的"跨业务域引用"列遗漏。区分两类：

**（a）完全未登记的边（真漂移）**：

| 文件:行 | 源→目 | 实体 | 状态 |
|---|---|---|---|
| `module-drp/model/app-erp-drp.orm.xml:297,298` | drp → inv | `ErpInvStockMove` | 仅 `§6.6` 标"待深化"但 ORM 层已落地，未在 §5.6.2 登记 |

**（b）§5.6.2 表格遗漏但多处文档已记载（表格维护瑕疵）**：

| 文件:行 | 源→目 | 已记载位置 |
|---|---|---|
| `module-manufacturing/.../app-erp-manufacturing.orm.xml:1495,1497` | mfg → inv | `ErpInvBatch` | `data-dependency-matrix.md §2.2:84`（"manufacturing R: master-data / inventory"）+ `§6.5:773` |
| `module-maintenance/.../app-erp-maintenance.orm.xml:153` | mnt → ast | `ErpAstAsset` | `module-boundaries.md:49` + `data-dependency-matrix.md §2.2:86` + `§4.1:179` + `§6.5:774`（4 处记载） |

**伤害**：§5.6.2 作为 DAG 真相源的可信度下降；但 mfg→inv/mnt→ast 边本身已在多处声明合法，只是汇总表维护不全。

---

### 🟠 F4 — 隐性共享内核 finance + master-data（MEDIUM）

> **解决状态（2026-07-24，plan `2026-07-24-1400-1`）**：✅ Done。裁决=分支 (b)：接受 3 个跨域语义类型（`ErpFinBusinessType` enum / `PostingEvent` DTO / `AcctSchemaResolver` dao 工具）为**显式共享内核**（类型不迁移）。原"抽取 `app-erp-common-api` 纯 SPI"提议经原型探索否决：enum 派发消费（`==` 常量比较 + `switch(enum)`）强绑定具体 enum 类不可降级为接口；`AcctSchemaResolver` dao 耦合不可入"零 dao/entity"模块。落地：`module-boundaries.md §共享内核` + `data-dependency-matrix.md §3.3` 登记 + `nop-compliance-checker.sh` R12 守卫（基线 69/66/38）+ CI 自动强制。详见闭包前必须项 #5。

**违反**：Architecture Governance Prompt §4 "Make Ownership Explicit" + `module-boundaries.md`（master-data 仅声明为"被引用根域"，未承担公共内核职责）。

**证据**：

| 跨域语义对象 | 所在 Maven 模块 | 被跨域 import 文件数（实测） |
|---|---|---|
| `ErpFinBusinessType` | `module-finance/erp-fin-dao` | **137**（v1 报 66，低估 2 倍） |
| `PostingEvent` | `module-finance/erp-fin-dao` | 64 |
| `AcctSchemaResolver` | `module-master-data/erp-md-dao` | 36 |

**抽样印证**：`MaintenanceIssuePostingDispatcher.java:4-5,12` 单文件即同时跨域 import 这 3 个类型。

**闭包项的更小切片**（v2 新增评估，见闭包 #2）：项目已有 19 个 `module-*/erp-*-api/` 模块作为 RPC 契约承载的既有模式，最小可行方案是建 `app-erp-common-api`（纯 SPI 接口，零 dao/entity），让 finance/master-data 提供实现——比 v1 提议的"搬实体到 kernel"小得多，且不会破坏 master-data 的 DAG 根地位。

**伤害**：finance/master-data 实际承担未声明的公共内核职责；任何 breaking change 会级联到 6+ 域。

---

### 🟠 F5 — notify 子系统无 owner docs（MEDIUM）

> **解决状态（2026-07-24，plan `2026-07-24-0930-3` Phase 3）**：✅ Done。基线修正为"缺 README.md + module-boundaries.md 行"（既有 inbox-patterns.md 129 行 + notification-strategy.md 73 行）。已创建 `docs/design/notify/README.md` 域级入口 + `module-boundaries.md §Owner Docs` 增 notify 行。详见闭包前必须项 #8。

**违反**：`module-boundaries.md §Owner Docs` 表 + `AGENTS.md §文档所有权`。

**证据**：
- `docs/design/notification-dispatch/` 目录不存在
- 近 2 月 `module-notify/` 有 13 次 code commit
- 对照其余 18 域均有 `docs/design/<domain>/README.md`

**根因**：`module-boundaries.md §Owner Docs` 表（line 82-103）本身就没有 notify 行——只有 18 个业务域。notify 作为"跨域通知派发子系统"在 `domain-module-split-analysis.md §2.0:66` 被单独归类（二级简称 `sys`）。所以 F5 与其说违反了 module-boundaries.md，不如说 **module-boundaries.md 的 Owner Docs 表本身未覆盖 notify**——是 owner doc 表的设计遗漏。

**伤害**：`AGENTS.md §任务路由` 要求"实现一个功能 → 从 `docs/design/` 起步"——notify 子系统违反此规则。

---

### 🟠 F8 — Compliance checker 未集成 CI、无基线日志（MEDIUM，新增）

> **v2 新增 finding**（v1 把 §Design Review Matrix 的 Guards 槽错误陈述为"无机器检查强制"）。

**违反**：Architecture Governance Prompt §Rot Indicators "**guard last-failed date**: guards that never fail may be wallpaper or dead armor" + §6 "Require Falsifiable Guards: can those checks actually fail, or are they wallpaper?"。

**证据**：

- `docs/audits/nop-compliance-checker.sh` 已存在（312 行、11 规则 R1-R11），覆盖：
  - R1 dao().saveEntity/updateEntity/getEntityById/findAllByQuery
  - R2 daoFor 跨域（R2a ErpMd*、R2b Erp*、R2c 总量、R2d Processor）
  - R3 new Erp*() 直接构造
  - R4 业务异常 extends RuntimeException（违反 NopException 约束）
  - R5 @Inject private
  - R6 @Transactional
  - R7 System.currentTimeMillis
  - R8 Processor 缺 xbiz
  - R9 doReverseApprove 一致性
  - R10 REQUIRES_NEW
  - R11 Processor 状态方法重复
- **但**：无 `.github/workflows/`、无 `.gitlab-ci.yml`，checker 未集成 CI
- **且**：无任何"checker 上次失败/通过"的日志基线
- `2026-07-16-2134-1` 计划 line 332 闭包证据记录过 "compliance checker R11=0"，但这是一次性快照，无持续追踪

**伤害**：checker 可能从未被定期运行或从来没失败过——正是 Architecture Governance Prompt §Rot Indicators 警告的 "dead armor"。已有检查能力但未发挥防护作用，比"无检查"更隐蔽。

---

### 🟠 F9 — 19 个 web 冒烟测试在模块级构建系统性跳过（MEDIUM，新增）

> **v2 新增 finding**（v1 把 19 个 @Disabled 测试埋在绿色信号里未单独评估）。

**违反**：Architecture Governance Prompt §Rot Indicators "**suppression count**: lint disables, skipped tests, unchecked casts, exception baselines"。

**证据**：

全仓 19 个 `module-*/erp-*-web/.../Erp*WebPagesTest.java` 全部 `@Disabled`，理由完全相同：

```java
@Disabled("WebPagesTest requires full app classpath (all module page resources). Run in app-erp-all context.")
```

样本：`module-finance/erp-fin-web/.../ErpFinWebPagesTest.java:11`，其余 18 域同模式。

**含义**：所有域级 web 冒烟测试在模块级 Maven 构建中**系统性跳过**，只在 `app-erp-all` 聚合工程上下文运行。若 CI 默认按模块级跑测试，则这 19 个测试永远不会执行。

**伤害**：web 层（AMIS view.xml）的回归保护在模块级构建中不存在；任何 view.xml 损坏只能等聚合工程阶段才暴露。

**附**（suppression count 全景）：非生成 Java 中 `@SuppressWarnings` 共 715 处（含测试代码），其中生产代码（`src/main/java`）**666 处**，含 `unchecked`/`rawtypes`/`all`/`deprecation` 共 86 处。多数是 JSON 解析 helper 的局部 `unchecked`——非系统性 lint 抑制文化。

---

### 🟡 F6 — `module-manufacturing` 依赖质量域生成常量（LOW）

**证据**：`module-manufacturing/.../processor/ErpMfgWorkOrderProcessor.java:20` → `import app.erp.qa.dao._ErpQaDaoConstants;`（在 :195 使用）。

**违反**：`domain-module-split-analysis.md §5` "永不手写 `_` 前缀文件"的衍生规则——不应跨域依赖对方生成产物。

**闭包项更小切片**（v2 新增）：项目已有 `2026-07-16-2134-1` Decision D1 完全同型先例——在 `erp-qa-dao` 创建轻量常量接口 `ErpQaDocStatus`，`_ErpQaDaoConstants` extends 它。mfg 改 import 这个非生成接口即可。

---

### 🟡 F7 — drp 实体命名前缀越界（LOW）

**证据**：drp 域的 4 个实体使用 `Inv` 前缀，但 `className=app.erp.drp.dao.entity.*`：
- `ErpInvDrpCrossDock`、`ErpInvDrpSafetyStockCalc`、`ErpInvDrpDockAppointment`、`ErpInvDrpLeadTimeRecord`

**违反**：`domain-module-split-analysis.md §3` 命名约定（类名前缀应匹配所属域）。

**关联**：该命名越界正是导致 v1 F1 把 `SafetyStockEngine.java:135` 误判为跨域写的成因——**类名误导比想象中更早产生代价**。这反而强化 F7 的优先级论证：命名不仅是审美，直接影响架构审查的准确性。

---

## 绿色信号（值得保持）

| 信号 | 证据 | caveat |
|---|---|---|
| 写契约面 I*Biz 注入零违规 | 532 处合规，0 处内部 impl 类穿透 | — |
| `-web` 层零跨域 import | 全 19 域 `-web` 模块扫描清零 | — |
| `@Inject` 可见性零违规 | 693 处全部包级可见，0 处 `private`/`protected` | — |
| `@SqlLibMapper` 零使用 | 服务层主动注释规避（`ErpSalCustomerPriceResolver.java:38`） | — |
| 生成文件 commit 配对源模型 | 最近 2 月 `_app.orm.xml` 94 次 commit（全历史）均成对出现源模型变更 | **已实证消除**：content-diff 抽样（19 个 `_app.orm.xml` 全量 + 全 115 commit 级配对扫描 + 跨域实体样本）验证零手编辑漂移；口径调和 22,413→797（见 `docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md`） |
| 业财 SPI 注册机制合规 | `IErpFinAcctDocProvider` + `ErpFinAcctDocRegistry` 模式落地 | — |
| DAG 主依赖单向 | master-data 根 + finance 顶的 DAG 无环；600 跨域 refEntityName 中除 F3 的 1 条真漂移（drp→inv ErpInvStockMove）外全部合法 | — |
| 真 `// TODO` 标记仅 1 处 | `TestErpPrjTaskDependency.java:158` | — |

---

## 闭包前必须项（Required Checks Before Closure）

> v2 修订：每条补 **verification checkpoint**（N4 异议），引用既有先例与更小切片。

| # | 必须动作 | 关联 | 优先级 | 验证 checkpoint |
|---|---|---|---|---|
| 1 | ✅ **裁决完成（2026-07-24，plan `2026-07-24-0930-3` Phase 1）**。实测 contract/aps 单模块测试全绿（ct 37 tests / aps 22 tests，0 失败），根因：两域零跨域 I*Biz 注入 + 仅 DAO 级 Maven 依赖，回避 I*Biz 以保单模块测试独立性。裁决=分支 (b)：I*Biz 强注入会破坏单模块测试（反事实经依赖结构验证为真）；Type 1（~100-150 ORM 导航可替代）可安全重构，Type 4（~10-30 跨域写/读）需 nop-entropy 平台 lazy/SPI 解耦或保留豁免。裁决文档：`docs/analysis/governed-path-cost-evaluation.md` | F1 | P0 | ✅ `mvn test -pl module-contract/erp-ct-service`=37 通过 + `mvn test -pl module-aps/erp-aps-service`=22 通过；`docs/analysis/governed-path-cost-evaluation.md` 含实测证据 + 分类型前置条件 |
| 2 | ✅ **Done（2026-07-24，plan `2026-07-24-0930-3` Phase 2）**。`ErpB2bAsnBizModel`（b2b→pur）跨域写豁免已补登 `posting-exemptions.md`，含位置（:215,226 + :266 行级回填）/config-gated（`erp-b2b.asn-auto-create-receive` 默认 false）/理由（核心零污染）/风险/补偿机制/收敛条件（待采购域 `createFromAsn` I*Biz，前置需平台解耦） | F1 | P0 | ✅ `grep ErpB2bAsn posting-exemptions.md` 命中 4 处 |
| 3 | ✅ **Done（2026-07-27，plan `2026-07-24-0930-2` + successor `2026-07-24-1600-1` + `2026-07-26-0300-1`）**。D1 全域推广完成：`Erp*DocStatus` dao 层接口现已覆盖 9 域（既有 pur/sal/fin/ast/inv + 新增 cs/mnt/mfg/qa）；8 份无 ORM 消费者的冗余 per-domain `approve-status.dict.yaml` 已移除（ast/cs/fin/mnt/mfg/pur/qa/sal）。**inventory approve-status ORM ext:dict 统一——RELEASED（successor `2026-07-24-1600-1`：inventory 5 列→`wf/approve-status` + 内联 dict + per-domain YAML 移除 + `ErpInvDaoConstants extends ErpInvDocStatus` 兼容；inv 114 测试 0 失败 + checker 零回归）**。**doc-status 6 域共享 dict 统一——RELEASED（successor `2026-07-26-0300-1`：6 域 28 列 `ext:dict="erp-<domain>/doc-status"` → `erp/doc-status` + 6 处内联 dict 移除 + 6 份 per-domain YAML 删除 + logistics stale 清理（内联 dict + YAML + en i18n label）+ 共享 dict 创建于 `module-common-service/_vfs/dict/erp/doc-status.dict.yaml`（选址候选 (c)：6 域 service 均依赖 common-service，单模块测试 classpath 自然可达）+ `ErpMntDaoConstants extends ErpMntDocStatus` 兼容链修复（maintenance 13 处直接引用）；154 模块 BUILD SUCCESS + 6 域 + logistics 7 service 测试 0 失败 + checker 零回归（R3=5 不变））**。**cs `time-entry-approve-status`——永久裁决特化（Successor: no）**：值集合 PENDING/APPROVED/REJECTED ≠ wf/approve-status UNSUBMITTED/SUBMITTED/APPROVED/REJECTED，为合法特化非冗余，不统一 | F2 | P0 | D1：`find module-*/erp-*-dao -name 'Erp*DocStatus.java'` 覆盖 9 域（✅）；`find module-* -name 'approve-status.dict.yaml'` 现返回 0 条（inventory per-domain 已删除，经 1600-1）；`find module-* -name 'doc-status.dict.yaml' -path '*/src/*'` 仅返回 `module-common-service/.../erp/doc-status.dict.yaml`（6 per-domain + logistics stale 已删，经 0300-1）；详见 plan `2026-07-24-0930-2` §Deferred But Adjudicated + `2026-07-24-1600-1` + `2026-07-26-0300-1` |
| 4 | ✅ **既有 `nop-compliance-checker.sh` 接入 CI**（非新写），记录当前精确基线（R2c=1108, R3=19, R11=0，见 `docs/audits/compliance-baseline.md`），后续每次增量不得超过基线。**Done（2026-07-24，plan `2026-07-24-0930-1`）** | F8 | P1 | ✅ CI 配置文件存在（`.github/workflows/compliance.yml`）；checker 在 PR 检查中实际运行（compliance job）；基线日志可查（`docs/audits/compliance-baseline.md` 含 16 行精确基线 + 门控规则 + 机器可读 yaml 块；门控经 anti-fake-green 三例证明有效） |
| 5 | ✅ **Done（2026-07-24，plan `2026-07-24-1400-1`）**。裁决=分支 (b)：接受 finance/master-data 的 3 个跨域语义类型为**显式共享内核**（类型不迁移）。原"抽取 `app-erp-common-api` SPI"提议经 Phase 1 原型探索否决——`ErpFinBusinessType` 是 enum，跨域消费方用 `== CONSTANT` 常量比较（10+ 站点）+ finance 自身 `switch(enum)`（3 站点）派发，强绑定具体 enum 类，**不可降级为 SPI 接口**；`AcctSchemaResolver` dao 耦合不可入"零 dao/entity"模块。落地：(1) `module-boundaries.md §共享内核` + `data-dependency-matrix.md §3.3` 显式登记 3 类型所有权/消费域/变更影响/依赖方向；(2) `nop-compliance-checker.sh` 新增 R12a/R12b/R12c 守卫追踪跨域 import 基线（69/66/38）+ `compliance-baseline.md` 基线行 + CI 自动强制（gate 模拟 PASS）；(3) `posting.md` 校正 `PostingEvent.businessType` 真实类型 + `master-data/README.md` 登记 `AcctSchemaResolver`。裁决文档 `docs/analysis/shared-kernel-extraction-decision.md` | F4 | P1 | ✅ `mvn dependency:tree -pl module-master-data` 仅自身（零业务依赖，叶模块 erp-md-service/dao 复核仅 `app-erp-master-data-*` 自身）；`ErpFinBusinessType` 跨域 import 经登记豁免（enum 不可降级 SPI，sanctioned 替代路径，R12a=69 守卫）；`mvn clean install -DskipTests` + `mvn test` 全 154 模块 BUILD SUCCESS |
| 6 | ✅ 19 个 `Erp*WebPagesTest` 改为 `@Tag("full-app")` + 19 个 `erp-*-web/pom.xml` surefire `excludedGroups=full-app`（模块级跳过，保留"仅全量 classpath 可运行"语义）+ CI `app-erp-all` 阶段强制运行页面校验。**Done（2026-07-24，plan `2026-07-24-0930-1`）** | F9 | P1 | ✅ CI `compliance.yml` 含 `web-pages-validation` job 跑 `mvn -pl app-erp-all -am test -Dtest=ErpAllWebPagesTest` 全绿（Tests run: 1, Failures: 0；该聚合测试调用 `pageProvider.validateAllPages()` 覆盖全 19 域页面，Decision a 复用既有非 @Disabled 聚合测试而非重跑 19 个域级测试；模块级 `mvn test -pl module-finance/erp-fin-web` Tests run: 0 验证 tag 排除生效） |
| 7 | ✅ **Done（2026-07-24，plan `2026-07-24-0930-3` Phase 2）**。`data-dependency-matrix.md §5.6.2` 跨业务域引用汇总表已补 3 边：drp→inv `ErpInvStockMove`（orm:297,298）+ mfg→inv `ErpInvBatch`（orm:1495,1497）+ mnt→ast `ErpAstAsset`（orm:153），均标注"批准保留"（DAG 单向合法）；同步补全 §6.5 manufacturing/maintenance/drp 行与第二批域注释 | F3 | P1 | ✅ §5.6.2 含 3 边（`grep ErpInvStockMove\|ErpInvBatch\|ErpAstAsset` lines 518/520/526 命中） |
| 8 | ✅ **Done（2026-07-24，plan `2026-07-24-0930-3` Phase 3）**。创建 `docs/design/notify/README.md`（域级入口，索引既有 inbox-patterns.md + notification-strategy.md，补全派发链/接收人解析/消费者关系/配置项/lifecycle 状态机/失败降级语义）+ `module-boundaries.md §Owner Docs` 表增 notify 行。注：路径为 `docs/design/notify/README.md`（草案审查 iteration 2 修正自 `notification-dispatch/`，对齐既有 inbox-patterns.md 所在目录） | F5 | P1 | ✅ `ls docs/design/notify/README.md` 存在；`grep notify module-boundaries.md` 返回 Owner Docs 行（line 104） |
| 9 | ✅ **裁决完成（2026-07-24，plan `2026-07-24-0930-2`）**。裁决=保留在 `erp-inv/` 命名空间并登记命名例外（方案 b）；3 个 dict（`drp-service-level`/`drp-ss-method`/`drp-xdock-status`）ORM 定义+物理文件+消费者全归属 module-drp，迁移到 `erp-drp/` 需改 ORM `ext:dict`（保护区域），登记例外零 ORM 风险。命名例外已登记于 `docs/design/drp/README.md §命名例外登记` | F2 | P2 | ✅ `docs/design/drp/README.md` 含命名例外登记小节；3 dict 文件物理归属与裁决一致（保留 module-drp 内） |
| 10 | ✅ **Done（2026-07-24，plan `2026-07-24-1400-2` Phase 1）**。在 `erp-qa-dao` 新建非生成常量接口 `ErpQaInspectionType`（`module-quality/erp-qa-dao/.../constants/ErpQaInspectionType.java`，承载 `INSPECTION_TYPE_INCOMING/IN_PROCESS/FINAL/OUTGOING` 四值，D1 先例同型产物；选新建专用接口而非扩展现有 `ErpQaDocStatus`，因检验类型与 doc/approve 状态属不同语义轴）；`ErpMfgWorkOrderProcessor.java` import 与使用点改引 `ErpQaInspectionType.INSPECTION_TYPE_FINAL`，删除 `_ErpQaDaoConstants` import。`mvn clean install -DskipTests` 全 154 模块 BUILD SUCCESS + checker 16 规则零回归 | F6 | P2 | ✅ `grep _ErpQaDaoConstants module-manufacturing` 返回 0 |
| 11 | ✅ **裁决完成（2026-07-24，plan `2026-07-24-1400-2` Phase 2）**。裁决=登记命名例外（方案 b，零 ORM 风险）：4 实体（`ErpInvDrpSafetyStockCalc`/`ErpInvDrpCrossDock`/`ErpInvDrpDockAppointment`/`ErpInvDrpLeadTimeRecord`）保留 `ErpInvDrp*` 类名 + `erp_inv_drp_*` 表前缀，物理归属已正确（`module-drp`，className `app.erp.drp.dao.entity.*`），重命名触及 ORM 保护区域+表名+66 文件生成产物连锁，风险高于收益。逐项登记于 `docs/design/drp/README.md §ErpInvDrp* 实体命名例外登记`（类名/className/表名/所属域/消费 dict/豁免理由/收敛触发条件）；`docs/architecture/domain-module-split-analysis.md §3` 追加已登记命名例外交叉引用。重命名移入 Deferred（触发条件：drp 域重大 ORM 变更时顺带） | F7 | P2 | ✅ `grep ErpInvDrp module-drp` 全部 66 命中文件均落入 drp owner doc 命名例外登记覆盖范围 |
| 12 | ✅ **Done（2026-07-24，plan `2026-07-24-0605-1`）**。执行 content-diff 抽样验证生成文件零手编辑漂移。权威人口口径裁决=(a) git-tracked `_` 前缀 java/xml（实测 797，0 gitignored）；22,413↔797 口径调和（22,413 不匹配任何 git-tracked 口径，推测口误/位序错排或含工作树未跟踪文件，以 797 为权威）。分层 content-diff：(1) 19 个 `_app.orm.xml` 全量——94/94 commit 配对 model 源；(2) 全 115 commit 级配对扫描——21 初筛候选经复核（修正配对口径：`_gen/_Erp*.view.xml` 源为 XMeta+template+parent-view）全部判为 codegen 驱动；(3) 关键可证伪证据：action-auth post-extends 经 3 轮 ORM-regen 存活、view 布局变更纯机械字段重排经 2 轮 regen 保留。三态判定：codegen 驱动 115/115、已认可例外 0、**真手编辑漂移 0**（无需 Fix successor）。证据 + 方法论：`docs/audits/2026-07-24-0605-generated-file-content-diff-evidence.md` | 绿色信号 | P2 | ✅ 抽样结果可复现（命令记录在证据文件）；caveat（单 author 无法 blame 区分）经 content-diff 实证消除 |

---

## 残留风险与缺失证据

1. **daoFor 真违规子集的精确计数**需依赖 `nop-compliance-checker.sh` 的 R2 规则在全仓跑一次才能得到（本审查用的是 2134-1 计划的估算 ±15pp）。建议闭包前先跑 checker 得到精确基线。
2. **finance 的 77 个 mutation 是否每个都有 owner doc 背书**未逐项核对——子代理抽样显示有 `treasury.md`/`period-close`/`bad-debt` owner docs，但 `ErpFinNotesReceivableBizModel`/`ErpFinNotesPayableBizModel` 各 6+ mutation 的 owner doc 归属未确认。Budget 槽提到的 42 Provider + 32 Dispatcher + 28 Engine + 11 Resolver 也只抽样判断，未全量核对 owner doc 背书。
3. **`§5.6.3` 禁止清单是否应增列 `mfg→inv`/`drp→inv`/`mnt→ast`** 取决于业务语义裁决——需要业务设计确认（mfg 批次是否真应引用 inv 批次表，还是应建 mfg 自己的批次表；drp 跨码头操作是否真应直接关联 inv 移动单）。
4. **governed path 成本评估需 Nop 平台层支持**：若 I*Biz 强注入确实破坏单模块测试启动（contract/aps 注释声明），整改需要平台层 lazy/SPI 解耦机制——超出本项目范围，可能需 `nop-entropy` 协同。
5. **本审查未覆盖**：① 性能/索引层面（跨域 join 的 N+1）；② 安全/认证层面（`@BizMutation` 权限注解完整性）；③ AMIS 前端 view.xml 与后端契约的 drift；④ 测试覆盖率深度（仅查了"是否存在"，未查"是否充分"）。这些超出 Architecture Governance Prompt 的当前路由范围。

---

## 附录：方法与工具

**审查路由**：`architecture_review` + `rot_audit` 双路由（按 Architecture Governance Prompt §Task Routes）。

**证据来源**：
- 5 份核心 owner docs（`docs/architecture/`）
- 2 份 v1 漏看的 owner docs（v2 补入）：`posting-exemptions.md`、`nop-compliance-checker.sh`
- 1 份已完成的前置计划（v2 补入）：`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`
- 2 路并行 `explore` 子代理（v1 证据收集）
- 2 路并行 `general` 子代理（v1 → v2 独立审查）
- 主代理实仓复核：`rg ErpFinBusinessType`（137 文件）、`rg @Disabled`（19 文件）、`rg 跨域访问注释`（10+ 文件）、`ls erp-inv/dict/`（3 个 drp- 前缀文件）

**未执行的核查**：
- 未跑 `mvn clean install -DskipTests` 验证编译
- 未跑 `nop-compliance-checker.sh` 得到精确 R2 基线（建议闭包项 #1 前置）
- ~~未跑 content-diff 验证生成文件零手编辑（建议闭包项 #12）~~ **已完成（闭包项 #12，2026-07-24，plan `2026-07-24-0605-1`）：content-diff 抽样验证 797 个 git-tracked `_` 前缀 java/xml 生成文件零手编辑漂移**

---

## 结论

nop-app-erp 的**主脊（DAG + I*Biz 写契约 + ORM 模型驱动生成纪律）经两轮独立审查依然健康**。v1 报告的主要缺陷是**未充分调研项目既有的治理工作**（`posting-exemptions.md` 已登记豁免、`2026-07-16-2134-1` 已分类 965 处 daoFor、`nop-compliance-checker.sh` 已有 11 规则），把已治理的例外当作静默漂移，导致 F1 的"系统性绕过"措辞过强。

v2 修订后，漂移集中在**可定位、可量化、可闭包**的 9 个 finding：

- F1（daoFor 真违规子集）≈ F2（字典真相碎裂）> F8（checker dead armor）≈ F9（19 disabled web tests）≈ F4（隐性共享内核）> F3（DAG 表格维护瑕疵）≈ F5（notify owner doc）> F6 > F7

**关键洞察**：本项目最大的治理缺口不是"代码违规多"，而是**已有检查能力（compliance checker + 豁免登记机制 + daoFor 分类计划）未充分发挥作用**——checker 未集成 CI（F8）、豁免登记不完整（F1 半治理）、daoFor 分类未推广为持续追踪。整改优先级应是**激活既有 guard 而非新建 wall**（呼应 Architecture Governance Prompt §7 "Keep The Governed Path Cheapest"）。

本审查未发现需要立即回滚或阻断发布的阻断性架构问题；所有 finding 均可通过有计划的整改收口。
