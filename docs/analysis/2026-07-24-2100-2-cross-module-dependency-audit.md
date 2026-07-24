# 跨模块数据依赖审计（2026-07-24-2100-1 Phase 2）

> Plan: `docs/plans/2026-07-24-2100-1-comprehensive-code-design-audit.md` Phase 2
> Skill: `docs/skills/cross-module-dependency-audit-prompt.md`（7 维度框架）
> 审计类型：纯审计（无代码/配置/ORM 模型修改）
> 审计日期：2026-07-24
> 扫描脚本：`/var/folders/.../erp_audit_scan.py`（Python，19 orm.xml 全量扫描）

## 执行摘要

**裁决：通过（PASS）**。19 个 `module-*/model/*.orm.xml` 全量扫描，7 维度全部合规。跨模块 to-one/to-many 引用边 **625 条**，DAG 拓扑排序**零循环**，22 个跨模块 refEntityName 目标 100% 有对应 `notGenCode="true"` 声明，外部实体声明全部最小化（最大 6 列），业财一体边界零违规（无 business→finance ORM 边），Maven pom 依赖与 ORM 引用完全对齐，`data-dependency-matrix.md §5.6.2` 声明的 R/S/P 分类与实测一致。

## 扫描范围

19 个 orm.xml 源模型文件（排除 `_gen`/`target`），共 **351 个本模块实体 + 111 个 notGenCode 外部实体声明 + 281 个字典定义**。

## 维度 1 — DAG 合规性（拓扑排序循环检测）

**扫描方法**：提取所有跨模块 `<to-one>`/`<to-many>` 的 `refEntityName`，按 `app.erp.<short>.dao.entity` 提取引用边，构建有向图，Kahn 拓扑排序检测循环。

**实测结果**：
- 跨模块引用边总数：**625 条**
- 唯一引用对（from→to）：**24 对**
- DAG 节点数：18（master-data 根 + 17 业务域；notify/sys 无跨模块出向引用）
- 拓扑排序：**成功完成**，无残留节点
- **循环数：0** ✅

**拓扑序（源在前）**：`hr, mfg, aps, fin, qa, ct, pur, drp, log, crm, mnt, cs, b2b, sal, inv, ast, prj, md`（master-data 为 DAG 汇/根，被所有业务域引用）

**裁决：✅ 通过**。零循环依赖。

## 维度 2 — 外部实体声明完整性（机制 B）

**扫描方法**：每个跨模块 `<to-one refEntityName="app.erp.X.dao.entity.Y">` 的 Y 必须在引用方模块 orm.xml 有对应 `<entity notGenCode="true">` 声明。

**实测结果**：
- 跨模块 refEntityName 目标数：**22 个唯一实体**
- 有对应 notGenCode 声明：**22/22（100%）** ✅
- 无声明遗漏：**0**

**裁决：✅ 通过**。所有跨模块 to-one 引用都有对应外部实体声明，codegen 不会因找不到 refEntityName 而失败。

## 维度 3 — 外部实体声明列最小化

**扫描方法**：外部实体声明的列只列关键列（不全量复制），阈值 15 列。

**实测结果**：
- 最大外部实体声明列数：**6 列**（远低于阈值 15）
- 超阈值声明：**0** ✅

**裁决：✅ 通过**。所有外部实体声明均为最小化关键列集（id/code/name + 个别业务列），运行时由被引用模块的完整 Entity 类提供所有列。

## 维度 4 — 业财一体边界一致性

**扫描方法**：finance 处于 DAG 顶，业务域（purchase/sales/inventory/assets/projects/manufacturing/maintenance）不应 ORM to-one 引用 finance（应走弱指针字符串三元组 + I*Biz 只读）。

**实测结果**：
- business→finance ORM to-one 边：**0** ✅
- finance→business ORM to-one 边：**0**（finance 对业务域是纯读，经 I*Biz 接口，非 ORM 字段引用）

**裁决：✅ 通过**。业财一体边界严格单向：业务→财务走 S 写（同事务）+ P 弱指针（反查），财务不回写业务表，业务表不感知凭证存在。

## 维度 5 — 冗余字段策略

抽检凭证行 `ErpFinVoucherLine`（高频多维筛选场景）：既建立 to-one（subject/partner/project/warehouse/material，机制 B 支持 EQL 点导航 + GraphQL 展开），又保留冗余显示名字段（列表零 join）。两者并存，符合 `data-dependency-matrix.md §5.6.1 设计原则 5`。

**裁决：✅ 通过**（抽检合规，与 owner doc 一致）。

## 维度 6 — Maven 依赖与 orm 声明对齐

**扫描方法**：引用方 `erp-xxx-dao/pom.xml` 应依赖被引用方的 `-dao` 包。

**实测结果**（全部跨业务域 ORM 引用方）：

| 引用方 dao | 被引用方 dao（应依赖） | pom 实测 | 状态 |
|---|---|---|---|
| finance-dao | master-data-dao + projects-dao | ✅ 两者均在 | ✅ |
| purchase-dao | master-data-dao + projects-dao | ✅ 两者均在 | ✅ |
| sales-dao | master-data-dao + projects-dao | ✅ 两者均在 | ✅ |
| hr-dao | master-data-dao + projects-dao | ✅ 两者均在 | ✅ |
| manufacturing-dao | master-data-dao + inventory-dao | ✅ 两者均在 | ✅ |
| maintenance-dao | master-data-dao + inventory-dao + assets-dao | ✅ 三者均在 | ✅ |
| drp-dao | master-data-dao + inventory-dao | ✅ 两者均在 | ✅ |

**说明**：finance-dao 不依赖 assets-dao —— 这是因为 finance→assets **无 ORM to-one**（凭证反查资产走 `voucher_bill_r` 弱指针 `billType=AST_DEPRECIATION`，见 `data-dependency-matrix.md §6.3` 与 §5.6.2 脚注），finance 对 assets 的只读在 service 层经 `IErpAstDepreciationScheduleBiz` I*Biz 接口。与本审计维度 1 的 DAG 边清单一致（0 条 fin→ast 边）。

**裁决：✅ 通过**。Maven 依赖与 ORM 声明完全对齐。

## 维度 7 — 与 data-dependency-matrix.md 一致性

**扫描方法**：矩阵 §5.6.2 声明的跨业务域 ORM 只读引用（机制 B 单向合法）与实测 DAG 边对比。

**矩阵声明 vs 实测**：

| 矩阵声明 | 实测 DAG 边数 | 一致性 |
|---|---|---|
| finance → projects（凭证行辅助核算 projectId） | fin→prj: 6 | ✅ |
| purchase → projects（项目采购成本归集） | pur→prj: 2 | ✅ |
| sales → projects（项目销售成本归集） | sal→prj: 1 | ✅ |
| hr → projects（员工项目分配/工时归集） | hr→prj: 2 | ✅ |
| manufacturing → inventory.ErpInvBatch（投入/产出批次追溯） | mfg→inv: 2 | ✅ |
| maintenance → assets.ErpAstAsset（设备→资产关联） | mnt→ast: 1 | ✅ |
| drp → inventory.ErpInvStockMove（跨码头 inbound/outbound） | drp→inv: 2 | ✅ |

所有业务域对 master-data 的引用（17 域 → md）也与矩阵 §3.1 主数据表族一致。

**裁决：✅ 通过**。矩阵 R/S/P 分类与实际引用方式一致（跨业务域 ORM 引用均为 R 只读 + 机制 B 单向合法）。

## 发现清单（按严重性分级）

### Blocker（0 项）
无。

### Major（0 项）
无。

### Minor（0 项）
无。

## 残留风险

1. **第二批 8 个扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b）业务层 S/P 关联**：`data-dependency-matrix.md §2.1` 明确标注「业务层关联待各域设计深化后补充」。当前 ORM 层引用清晰（本审计覆盖），但 S 写/弱指针业务层关联尚未在矩阵 §4.2/§5.1 全量登记。这不属 ORM 审计范围（属业务设计深化），记录为已知技术债。
2. **DAG 拓扑序中 fin 排在第 3 位**：这是 Kahn 算法按入度排序的结果（fin 入度非零但出度也非零），不表示违规——fin 对 master-data/projects 有 ORM 出向引用，同时被业务域 S 写（service 层，非 ORM）。ORM 层 fin 无环即合法（裁决原则 4：分层独立校验）。

## 关联

- Plan: `docs/plans/2026-07-24-2100-1-comprehensive-code-design-audit.md` Phase 2
- Skill: `docs/skills/cross-module-dependency-audit-prompt.md`
- Owner docs: `docs/architecture/module-boundaries.md`, `docs/architecture/data-dependency-matrix.md`
- 平台机制: `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`
