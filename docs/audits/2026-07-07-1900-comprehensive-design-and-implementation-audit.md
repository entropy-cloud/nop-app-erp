# 综合审计报告：设计与实现深度审核

**审计日期**：2026-07-07 19:00
**审计类型**：全维度设计+实现审计
**审计方法**：4 路并行子代理（ORM 模型/架构文档/项目结构/流程文档）→ 人工综合 → 交叉验证
**仓库快照**：`HEAD`（2026-07-07 最新提交，全绿验证）

---

## 执行摘要

nop-app-erp 是一个**质量极高的 AGE 工作流项目**。18 个业务域 + 1 个通知子系统（共 279 实体）的 ORM 模型设计完整，codegen 骨架已生成（2,687 个 Java 文件），154 reactor 模块已构建通过。

本次综合审计发现 **4 个严重**、**6 个高**、**9 个中**、**6 个低**严重性问题。核心风险集中在：

1. **3 个域中 6 处跨模块表名双前缀错误**（`erp_md_md_partner` → 应为 `erp_md_partner`）→ 阻断 EQL 跨模块查询
2. **7 个扩展域缺少 posted/businessDate 标准字段** → 业财一体化基线合规缺口
3. **文档间事务语义矛盾**（REQUIRES_NEW vs SYNC）→ 直接影响实现决策
4. **known-good-baselines.md 为空**、缺陷记录仅 1 篇 → 验证与知识沉淀缺口

---

## 审计范围

| 维度 | 范围 | 方法 |
|------|------|------|
| ORM 模型 | 19 个 `module-*/model/app-erp-*.orm.xml` | 全量读取 + 交叉验证 |
| 设计文档 | `docs/design/`（167 文件）、各域 README + state-machine | 路由表 + 关键内容比对 |
| 架构文档 | `docs/architecture/`（30 文件） | 一致性比对 + 完整度评估 |
| 流程文档 | `docs/plans/`（109）、`docs/audits/`（10）、`docs/logs/`（15） | 纪律性 + 质量评估 |
| 项目结构 | 根目录 + 19 子模块 + codegen 产物 | 结构完整性 + 命名一致性 |
| 代码与设计一致性 | ORM 模型 ↔ 设计文档 ↔ 架构文档 | 三向交叉验证 |

---

## 一、ORM 模型审计

### 1.1 命名规范 — 基本符合，发现异常

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 实体类名前缀 `Erp<DomainShort>` | ✅ 19/19 域一致 | master-data=`ErpMd*`, purchase=`ErpPur*` 等 |
| 表名前缀 `erp_<short>_` | ✅ 19/19 域一致 | `erp_md_material`, `erp_pur_order` 等 |
| 字典命名空间 `erp-<domain>/<dict>` | ⚠️ 基本一致 | 见下文 |

### 1.2 [严重] 跨模块外部实体表名双前缀错误

**路径**：`module-*/model/app-erp-*.orm.xml` 中的 `notGenCode="true"` `<to-one>` 引用

| 域 | 错误 tableName | 应为 | 出现次数 |
|----|---------------|------|---------|
| **b2b** | `erp_md_md_partner` | `erp_md_partner` | 1 |
| **b2b** | `erp_md_md_material` | `erp_md_material` | 1 |
| **cs** | `erp_md_md_partner` | `erp_md_partner` | 1 |
| **logistics** | `erp_md_md_partner` | `erp_md_partner` | 1 |
| **logistics** | `erp_md_md_employee` | `erp_md_employee` | 1 |
| **logistics** | `erp_md_md_material` | `erp_md_material` | 1 |
| **contract** | `erp_md_md_partner` | `erp_md_partner` | 1（源模型） |

> **影响**：这些错误导致 EQL 跨模块查询在运行时查找错误的物理表名，阻塞 b2b/cs/logistics/contract 域的跨域数据访问。已传播到生成的 `_app.orm.xml`（`b2b/_app.orm.xml:1079/1095`、`cs/_app.orm.xml:1319`、`log/_app.orm.xml:669/685/693`、`ct/_app.orm.xml:1197`）。

### 1.3 [高] 文件头注释与实际实现不一致

| 域 | 文件头声称 | 实际实现 | 影响 |
|----|-----------|---------|------|
| **master-data** | `dict valueType=int` + `option value 10/20/30` | 所有 16 个字典 `valueType="string"` + 字符串值 | 误导性注释 |
| **sales** | `dict valueType=int` | 所有字典 `valueType="string"` | 同上 |

### 1.4 [中] 冗余 approveStatus 字典（死代码）

**8 个域**定义了 `erp-<domain>/approve-status` 字典，但所有列引用 `wf/approve-status`：

`purchase`, `sales`, `manufacturing`, `quality`, `maintenance`, `cs`, `finance`, `inventory`（混用）

> **影响**：代码生成产物中会产生冗余字典文件，增加维护成本。

### 1.5 [中] 7 个扩展域缺少标准业财字段

`domain-design-guidelines.md §14` 要求所有业务单据头携带 `orgId/businessDate/posted/postedAt/postedBy/currencyId/exchangeRate/amountSource/amountFunctional/version`。

| 域 | 缺失情况 |
|----|---------|
| **cs** (Ticket) | 无 posted/At/By, 无 businessDate |
| **hr** (Employee, Timesheet, Salary) | 无 posted/At/By, 无 businessDate |
| **logistics** (Shipment) | 无 posted/At/By, 无 businessDate |
| **b2b** (Asn) | 无 posted, 无 businessDate |
| **contract** (Contract, InvoicePlan) | 无 posted, 无 businessDate |
| **drp** (Line) | 无 posted, 无 businessDate |
| **aps** (Demand, OperationOrder) | 无 posted, 无 businessDate |

> **影响**：这些域的单据无法参与业财过账链路，posted 兜底扫描失效，跨域统计数据口径不一致。

### 1.6 [低] 其他 ORM 发现

| 问题 | 域 | 说明 |
|------|----|------|
| `ErpSalOrderLine.project` 缺少 `tagSet="pub"` | sales | 与其他 to-one 不一致 |
| `approvedBy/approvedAt` 使用非连续 propId(200/201) | 8+ 域 | 不影响功能，但可能影响序列化工具 |

---

## 二、架构与设计文档一致性审计

### 2.1 [严重] 事务语义矛盾：REQUIRES_NEW vs SYNC

> **审查修正（2026-07-07）**：本节原判断为误判。`ErpFinVoucherBizModel.post()` 实际叠加 `@Transactional(REQUIRES_NEW)`，所有 posting executor JavaDoc 引用独立事务（证据：`docs/analysis/2026-07-05-1300-platform-best-practices-extended-audit.md:231`、`docs/design/finance/posting.md:380`）。`flow-overview.md §6.1` 的 REQUIRES_NEW 描述与实现一致。整改方向：修正 `data-dependency-matrix.md §4.1/§4.4`，使其与 `flow-overview.md` 对齐（详见 `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` C-2）。

原审计观察（保留作历史记录）：

| 文档 | 表述 | 影响 |
|------|------|------|
| `docs/design/flow-overview.md §6.1 (L497-498)` | 单据审核+凭证生成 = **REQUIRES_NEW** 独立事务隔离 | ✅ 与实现一致 |
| `docs/architecture/data-dependency-matrix.md §4.1 (L168-198)` | 默认 **SYNC 同事务** | ❌ 误描述，需改为"同步调用（独立事务 REQUIRES_NEW）" |
| `docs/architecture/data-dependency-matrix.md §4.4 (L249)` | "本工程默认 SYNC"（隐含同事务） | ❌ 误描述，需澄清为"同步调用（独立事务 REQUIRES_NEW）" |

> **修正后裁决**：REQUIRES_NEW 是有意架构决策（凭证失败不回滚业务+库存主事务，跨域失败经 `posted` 标志 + 兜底扫描保证最终一致）。`flow-overview.md` 描述正确，`data-dependency-matrix.md` §4.1/§4.4 已修正。

### 2.2 [严重] DAG 层级矛盾

| 文档 | 层级描述 |
|------|---------|
| `domain-design-guidelines.md §1.2 (L36)` | manufacturing/finance 同层 |
| `data-dependency-matrix.md §2.1` | finance 为 L3（顶层），manufacturing 为 L2（扩展域） |
| `module-boundaries.md` | 同上，finance 是唯一的 L3 域 |

> **影响**：如果按 domain-design-guidelines 的设计顺序做依赖分析，会导致 manufacturing 的错误层级定位。

### 2.3 [高] docStatus 通用值与实际域值不匹配

`docs/architecture/document-engine.md §三轴` 定义通用 docStatus：
```
DRAFT / PREPARED / COMPLETED / CLOSED / CANCELLED / VOIDED / REVERSED
```

但每个域的实际取值不同：
- inventory: `DRAFT / CONFIRMED / DONE / CANCELLED`
- finance voucher: `DRAFT / POSTED / CANCELLED`
- assets: `DRAFT / IN_SERVICE / IDLE / SCRAPPED / SOLD`

> **影响**：通用 docStatus 集与任何域的实际取值都不匹配，造成概念混淆。

### 2.4 [高] 单据编号唯一性矛盾

| 文档 | 约束 |
|------|------|
| `doc-model-design.md (L304-310)` | **全局唯一** `unique="true"` |
| `domain-design-guidelines.md §14.1.1` | **orgId 内唯一**（多公司场景） |

> **裁决**：`domain-design-guidelines.md` 是设计权威，`doc-model-design.md` 需更新。

### 2.5 [高] doc-model-design.md 是陈旧文档

该文档使用已淘汰的 `docType/bizType` 双维分类法，包含过时 Java 代码（`@Component`, `Long` ID 等），未反映当前的 `billType/businessType` 体系和 String ID 约定。

> **建议**：归档到 `docs/archive/` 或全面重写。

### 2.6 [中] manufacturing docStatus 包含 INSPECTING 态

`domain-design-guidelines.md §16.2 (L542)` 列出 `INSPECTING`，但 `flow-overview.md §3.3 (L313-314)` 明确指出**无 INSPECTING 态**，质检通过 config-gated 钩子实现，不引入独立工单状态。

### 2.7 [中] 文档 §编号跳变

`domain-design-guidelines.md` 从 §12（版本演进策略，内部编号 12.1）直接跳到 §14，中间缺失 §13。

---

## 三、流程文档成熟度审计

### 3.1 [高] known-good-baselines.md 为空

**严重缺口**：`docs/testing/known-good-baselines.md` 仅有模板占位符，无任何实际基线记录。鉴于项目日常构建全绿通过，此文件应密集填充。

### 3.2 [高] 缺陷记录仅 1 篇

整个项目仅 `docs/bugs/` 有 1 篇缺陷记录（表名拼写传播 C-1）。审计发现的 C-2（销售信用控制多币种遗漏/财务风险）、C-4（凭证明文 GraphQL 可查/安全风险）、C-5（12 域 60 处 `LocalDateTime.now()`/测试不可控）等均未记录为独立缺陷。

### 3.3 [中] 审计质量事件（2026-07-05）

审计 `2026-07-05-1300` 的服务层评分误导性偏高（声称 7.5-8.0），被当天的补充审计 `2026-07-05-1500` 修正为 6.0-6.5。1300 遗漏了 48 处 `dao().updateEntity()` 违规和 600 处 `daoFor()` 总计（声称仅 46 处）。这是审计方法的系统性短板。

### 3.4 [中] 状态冲突：计划标记 completed 但关闭审计待处理

`docs/plans/2026-07-03-2108-1-dict-int-to-string-refactor.md` 显示 `_pending independent closure audit_` 但计划状态为 "已完成"——违反了规则 12。

### 3.5 [中] 计划命名冲突

`2026-07-05-1500-1` 被两个不同计划使用：
- `cross-review-remediation.md`
- `nop-platform-compliance-remediation.md`

### 3.6 [中] 回顾文档空白

虽然有明确候选（平台合规债务、字典类型迁移反转决策、审计质量事件），`docs/retrospectives/` 仍无任何实际回顾。

### 3.7 [低] docs/testing/ 无探索性测试记录

### 3.8 [低] 经验教训 01-03 缺失（仅 04 存在）

### 3.9 [低] 审计合规检查器未集成 CI

`nop-compliance-checker.sh` 是发现的强大工具，但未在 `project-context.md` 中列为验证命令或 CI 集成。

---

## 四、项目结构与代码生成审计

### 4.1 结构完整性 — 优秀

| 检查项 | 结果 |
|--------|------|
| 19 个模块目录全部存在 | ✅ |
| 模块内部 8 层骨架（codegen/dao/meta/service/web/app/api + model） | ✅ 19/19 |
| 代码生成产物（Java 实体/DAO/BizModel/xbiz/xmeta/view.xml） | ✅ 2,687 Java 文件 |
| 构建状态（`mvn clean install -DskipTests`） | ✅ 全绿（154 reactor 模块） |
| 部署 SQL（MySQL/Oracle/PG × 19 域 × 12 文件） | ✅ 228 SQL 文件 |

### 4.2 文档结构 — 优秀

| 维度 | 值 |
|------|-----|
| 总文档文件 | 456 |
| 设计文档覆盖 | 18 域全部有 README + state-machine + use-cases + ui-patterns |
| 计划文件 | 109（含 80+ 活跃已完成） |
| 审计文件 | 10（含方法论 + 8 审计报告 + 合规检查器） |
| 日志文件 | 15（逐日记录，今日已更新） |

### 4.3 唯一差异

| 文档描述 | 实际状态 |
|---------|---------|
| AGENTS.md 写 "bootstrap/预代码生成阶段" | codegen 已完成，进入 BizModel 深化阶段 |
| AGENTS.md 列 10 域 | 实际 18 域 + notify（2026-06-30 新增 8 域） |

---

## 五、汇总问题清单

### 严重（4 项）

| ID | 问题 | 位置 | 建议 |
|----|------|------|------|
| **C-1** | 6 处外部实体表名双前缀 | b2b/cs/logistics/contract orm.xml | 立即修正源 orm.xml，`mvn clean install` 重新生成 |
| **C-2** | transactions REQUIRES_NEW vs SYNC 矛盾 | flow-overview.md §6 vs data-dependency-matrix.md §4 | 统一为 SYNC（默认）+ ASYNC（可配），修正 flow-overview |
| **C-3** | DAG 层级：manufacturing/finance 同层 vs finance L3 | domain-design-guidelines.md §1.2 | 对齐到 data-dependency-matrix.md |
| **C-4** | docStatus 通用集与实际域值不匹配 | document-engine.md | 转为概念框架 + 指针到各域 state-machine.md |

### 高（6 项）

| ID | 问题 | 位置 |
|----|------|------|
| **H-1** | 7 扩展域缺少 posted/businessDate 标准字段 | cs/hr/logistics/b2b/contract/drp/aps |
| **H-2** | known-good-baselines.md 为空 | docs/testing/ |
| **H-3** | 缺陷记录仅 1 篇 | docs/bugs/ |
| **H-4** | master-data/sales 文件头注释与实际 dict 定义矛盾 | 对应 orm.xml |
| **H-5** | docNo 唯一性：全局 vs orgId 内矛盾 | doc-model-design.md vs domain-design-guidelines.md |
| **H-6** | doc-model-design.md 陈旧 | 需归档或重写 |

### 中（9 项）

| ID | 问题 | 位置 |
|----|------|------|
| **M-1** | 8 域冗余 approveStatus 字典（死代码） | purchase/sales/manufacturing/quality/maintenance/cs/finance/inventory |
| **M-2** | manufacturing docStatus 含 INSPECTING | domain-design-guidelines.md §16.2 |
| **M-3** | 审计 1300 质量事件（评分偏差 +2.0） | docs/audits/ |
| **M-4** | 计划 2108-1 状态冲突（completed + pending closure audit） | docs/plans/ |
| **M-5** | 计划命名冲突 1500-1 | docs/plans/ |
| **M-6** | 回顾文档空白 | docs/retrospectives/ |
| **M-7** | 合规检查器未集成 CI | nop-compliance-checker.sh |
| **M-8** | contract 前缀 `erp-ct` 非直观 | 命名约定 |
| **M-9** | 技能未针对项目定制化 | docs/skills/README.md |

### 低（6 项）

| ID | 问题 |
|----|------|
| **L-1** | ErpSalOrderLine.project 缺少 tagSet="pub" |
| **L-2** | approvedBy/approvedAt propId 200/201 非连续 |
| **L-3** | 领域设计指南 §编号跳变（§12→§14） |
| **L-4** | 经验教训 01-03 缺失 |
| **L-5** | 无探索性测试记录 |
| **L-6** | 无 *.api.xml RPC 契约文件（可能是有意选择） |

---

## 六、推荐修复优先级

### P0（立即修复）

1. **修正 6 处外部实体表名**（b2b/cs/logistics/contract orm.xml）
2. **统一事务语义**（flow-overview.md 对齐到 data-dependency-matrix.md SYNC 默认）
3. **补充 known-good-baselines.md**

### P1（本迭代修复）

1. **为 7 个扩展域补充 posted/businessDate 标准字段**（cs/hr/logistics/b2b/contract/drp/aps）
2. **创建缺陷记录**（C-2 财务风险、C-4 安全风险、C-5 测试不可控性）
3. **归档 doc-model-design.md** 或全面重写
4. **解决计划状态冲突**（2108-1 + 1500-1 命名冲突）

### P2（下迭代修复）

1. **审计 1300 质量事件 → 提取经验教训 & 创建回顾**
2. **删除 8 域冗余 approve-status 字典**
3. **重置 contract 前缀** 或文档说明
4. **配置合规检查器 CI 集成**

### P3（持续改进）

1. 修正 master-data/sales 文件头注释
2. 文档编号修正（§12→§14 跳变）
3. 技能模板定制化
4. 添加 `tagSet="pub"` 到 sales to-one

---

## 七、亮点与积极发现

除上述问题外，项目整体质量**极高**：

| 维度 | 评估 |
|------|------|
| **ORM 模型覆盖** | 19 域 279 实体覆盖 ERP 核心 + 扩展全场景 |
| **设计文档深度** | 18 域均有 README + state-machine + use-cases + ui-patterns，部分域有 10+ 子文档 |
| **代码生成完整** | 2,687 Java 文件 / 19 域代码生成全部完成 |
| **构建状态** | 154 reactor 模块全绿 |
| **计划纪律** | 80+ 计划结构化良好，有草案审查 + 关闭审计 |
| **审计基础设施** | 多层对抗性子代理审计 + 合规检查器脚本 |
| **日志纪律** | 逐日记录，验证证据详实，今日已更新 |
| **跨域 DAG** | 已验证零循环，多文档交叉确认 |
| **三轴状态分离** | 全域一致应用 docStatus/approveStatus/posted 三轴范式 |
| **开发活跃度** | 今日 67 次提交，全部全绿验证 |

---

## 八、文档交叉引用

| 引用 | 来源 | 目标 |
|------|------|------|
| 事务矛盾 | 本报告 §2.1 | `docs/design/flow-overview.md §6.1` ↔ `docs/architecture/data-dependency-matrix.md §4.1` |
| DAG 矛盾 | 本报告 §2.2 | `docs/design/domain-design-guidelines.md §1.2` ↔ `docs/architecture/data-dependency-matrix.md §2.1` |
| 表名错误 | 本报告 §1.2 | b2b/cs/logistics/contract `model/*.orm.xml` |
| 审计质量事件 | 本报告 §3.3 | `docs/audits/2026-07-05-1300-*` ↔ `docs/audits/2026-07-05-1500-*` |

---

## 九、方法说明

本报告由主代理调度 4 路子代理并行审计后综合而成：

1. **ORM 模型审计** → 全量读取 19 个 orm.xml + 生成文件交叉验证
2. **架构文档审计** → 读取 30 架构 + 167 设计文档，三向一致性比对
3. **项目结构审计** → 目录遍历 + 文件计数 + git 历史分析
4. **流程文档审计** → 计划/审计/日志/缺陷/技能的纪律性评估

每路代理独立生成结构化报告后，主代理交叉验证冲突点（如事务矛盾在 3 处文档中存在 3 种不同描述），并经 grep 确认实际代码状态后定稿。
