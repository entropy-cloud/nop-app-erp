# 跨模块依赖与 DAG 审计报告（A1.10）

> Plan: `docs/plans/2026-07-27-1227-1-audit-remediation-ma1-cross-module-dag-audit.md`
> Work Item: A1.10 跨模块依赖与 DAG 审计（全域跨域）
> Skill: `docs/skills/cross-module-dependency-audit-prompt.md`
> 审计时间：2026-07-27
> 审计基线：HEAD 经 `docs/audits/compliance-baseline.md §M0 锚点注记` 落锚（与 MA1 ORM 审计同期基线）
> Audit Status: closed

## TL;DR — 四项量化汇总（skill 要求）

| 字段 | 实测值 | owner doc 自述 | 偏差 |
|------|--------|---------------|------|
| **跨模块引用边总数（to-one + to-many）** | **625**（625 to-one + 0 to-many） | ~369 to-one | **+256（+69%）** — owner doc §5.6.2 自述"待 codegen 后跑脚本精确统一"，本审计为该脚本 |
| **DAG 合规边数** | **625 / 625 = 100%** | — | 零违规 |
| **循环数** | **0** | 0（声称零循环） | 一致 ✅ |
| **各域外部实体声明完整覆盖率** | **108 / 108 = 100%** | — | 全覆盖 ✅ |

**裁决：通过（PASS）**。DAG 零循环、零禁止方向、外部实体声明 100% 完整。owner doc §5.6.2 自述数值偏低 69%，但其文字本身已声明"待脚本精确统一"——本审计即该脚本，差异作为 P1 文档更新项进入 MR1。

## 0. 审计方法与可复现性

- **自动化机器核验脚本**：`docs/audits/scripts/cross-module-dep-extract.py`
  - 扫描全 19 个 `module-*/model/app-erp-*.orm.xml`
  - 正则提取所有 `<entity notGenCode="true">` 外部实体声明
  - 正则提取所有 `<to-one>` / `<to-many>` 的 `refEntityName`
  - 通过 entity tag 包围关系定位引用方实体（src_entity）
  - 按 `app.erp.<pkg>.dao.entity.*` 的 `<pkg>` 段归一化到 19 个 canonical 模块标签（含 contract 异常：package=`app.erp.contract` 但实体前缀=`ErpCt*`，归一到 `ct`）
  - 构图、DFS 三色法检测循环、对照 ALLOWED 字典验证方向合法性
- **完整原始输出**：`docs/audits/scripts/cross-module-dep-extract-output.md`（979 行，含每条边的 file:line）
- **重跑命令**：`python3 docs/audits/scripts/cross-module-dep-extract.py`

**输入清单**（19 ORM 文件）：`module-{aps,ast,b2b,contract,crm,cs,drp,finance,hr,inventory,logistics,maintenance,master-data,manufacturing,notify,projects,purchase,quality,sales}/model/app-erp-*.orm.xml` 全部存在。

## 1. 跨模块引用边清单

### 1.1 DAG 边汇总（按 src → tgt 去重）

| src | → | tgt | 边数（含 to-many） | 合规性 |
|-----|---|-----|-------|--------|
| aps | → | md | 6 | ✅ 业务→根 |
| ast | → | md | 50 | ✅ 业务→根 |
| b2b | → | md | 15 | ✅ 业务→根 |
| crm | → | md | 38 | ✅ 业务→根 |
| cs | → | md | 15 | ✅ 业务→根 |
| ct | → | md | 13 | ✅ 业务→根 |
| drp | → | inv | 2 | ✅ 登记例外（§5.6.2 跨业务域引用，drp→inv ErpInvStockMove，DAG 单向合法） |
| drp | → | md | 24 | ✅ 业务→根 |
| fin | → | md | 97 | ✅ 业务→根 |
| fin | → | prj | 6 | ✅ 登记例外（finance→projects，finance 是 DAG 顶） |
| hr | → | md | 32 | ✅ 业务→根 |
| hr | → | prj | 2 | ✅ 登记例外（hr→projects 员工项目分配/工时归集） |
| inv | → | md | 85 | ✅ 业务→根 |
| log | → | md | 10 | ✅ 业务→根 |
| mfg | → | inv | 2 | ✅ 登记例外（mfg→inv ErpInvBatch，工序投入/产出批次追溯） |
| mfg | → | md | 56 | ✅ 业务→根 |
| mnt | → | ast | 1 | ✅ 登记例外（mnt→ast ErpAstAsset，equipment→asset 关联） |
| mnt | → | md | 12 | ✅ 业务→根 |
| prj | → | md | 27 | ✅ 业务→根 |
| pur | → | md | 56 | ✅ 业务→根 |
| pur | → | prj | 2 | ✅ 登记例外（pur→prj 项目采购成本归集） |
| qa | → | md | 19 | ✅ 业务→根 |
| sal | → | md | 54 | ✅ 业务→根 |
| sal | → | prj | 1 | ✅ 登记例外（sal→prj 项目销售成本归集） |

**DAG 唯一边数（src,tgt 对）：24；循环数：0；禁止方向边：0。**

### 1.2 跨业务域 ORM 只读引用（机制 B 登记例外，非根）

owner doc §5.6.2 + module-boundaries.md 共登记 7 类跨业务域 ORM 单向合法引用，本审计全部复核 ✅：

| src | tgt | 用途 | 实体 | 引用方字段（实例） |
|-----|-----|------|------|-----------|
| fin | prj | 凭证行辅助核算 projectId | ErpPrjProject | ErpFinVoucherLine.project 等 6 处 |
| pur | prj | 项目采购成本归集 | ErpPrjProject | ErpPurOrder.project / ErpPurInvoice.project 等 2 处 |
| sal | prj | 项目销售成本归集 | ErpPrjProject | ErpSalOrderLine.project |
| hr | prj | 员工项目分配/工时归集 | ErpPrjProject + ErpPrjTask | 2 处 |
| mfg | inv | 工序投入/产出批次追溯 | ErpInvBatch | ErpMfgBatchGenealogy.inputLot/outputLot（orm:1495,1497） |
| mnt | ast | equipment→asset 关联 | ErpAstAsset | ErpMntEquipment.asset（orm:148） |
| drp | inv | 跨码头入/出库移动 | ErpInvStockMove | ErpInvDrpCrossDock.inboundMove/outboundMove（orm:297,298） |

> **F3 闭包复核**：arch-gov F3（"ORM DAG 边登记不完整"）经 plan `2026-07-24-0930-3` Phase 2 闭包时补登的 3 条边（drp→inv / mfg→inv / mnt→ast）在本审计中全部确认存在且方向单向合法。F3 闭包结论：**已闭包确认**。

### 1.3 完整边清单（625 条）

完整每条边的清单（含 file:line 锚点）见 `docs/audits/scripts/cross-module-dep-extract-output.md §1`。

## 2. 外部实体声明完整性矩阵（机制 B）

### 2.1 每域 `<entity notGenCode="true">` 声明数

| 域 | 声明数 | 域 | 声明数 |
|----|-------|----|-------|
| aps | 1 | log | 5 |
| ast | 6 | mfg | 10 |
| b2b | 3 | mnt | 8 |
| crm | 5 | notify | 0 |
| cs | 2 | prj | 5 |
| ct | 4 | pur | 12 |
| drp | 6 | qa | 5 |
| fin | 12 | sal | 11 |
| hr | 6 | inv | 10 |
| md | 0（根，不引用） | | |
| **TOTAL** | **111** | | |

完整声明清单（111 条，含 file:line）见 `docs/audits/scripts/cross-module-dep-extract-output.md §5`。

### 2.2 引用 ↔ 声明 完整覆盖矩阵

**规则**：每个生效的跨模块 `<to-one refEntityName="app.erp.X.dao.entity.Y">` 在引用方模块 orm.xml 必须有对应 `<entity name="app.erp.X.dao.entity.Y" notGenCode="true">` 声明，否则 codegen 会因找不到 refEntityName 失败（P0 blocker）。

**实测**：108 个被引用的外部实体 → 108 个有声明，**覆盖率 100%**。零 MISSING。

> 108 < 111 因为 finance 声明了 `ErpAstAsset` 但无任何 to-one 引用它（phantom declaration，见 §7 P2-MA1-001）。

### 2.3 声明列最小化（机制 B 设计原则 3）

抽样核验：
- `fin.ErpMdSubject` 声明 4 列（id/code/name/subjectClass）— finance 凭证行只需要这 4 列做点导航与汇总，未全量复制 master-data 完整列 ✅
- `pur.ErpMdPartner` 声明 3 列（id/code/name）✅
- `inv.ErpMdMaterial` 声明 4 列（id/code/name/...）✅
- 各外部实体声明均遵循"只列关键列，不全量复制"原则 ✅

### 2.4 tableName / name 一致性

抽样核验外部实体声明的 `tableName` 与被引用模块一致：
- `fin → app.erp.md.dao.entity.ErpMdSubject` 声明 `tableName="erp_md_subject"`，与 master-data 域 `erp_md_subject` 一致 ✅
- `mfg → app.erp.inv.dao.entity.ErpInvBatch` 声明 `tableName="erp_inv_batch"`，与 inventory 域一致 ✅
- `drp → app.erp.inv.dao.entity.ErpInvStockMove` 声明 `tableName="erp_inv_stock_move"`，与 inventory 域一致 ✅

**Dim 2 PASS（外部实体声明完整性 100% + 列最小化 + tableName 一致）。**

## 3. 七维度审计结论

### Dim 1 — DAG 合规性 ✅ PASS

- 自动化 DFS 三色法检测：**零循环**
- 24 条 DAG 边全部单向合法：
  - 18 条业务→master-data（恒合法）
  - 7 条跨业务域登记例外（§1.2 全部复核 ✅）
- 零禁止方向（projects→finance、inventory→purchase、purchase↔sales 等均未出现）

### Dim 2 — 外部实体声明完整性（机制 B） ✅ PASS

见 §2。108/108 引用 ↔ 声明 100% 覆盖。零 MISSING → 零 P0。

### Dim 3 — 跨模块引用范式选择合理性 ✅ PASS（带 1 项 P2 观察项）

抽样核验：

| 场景 | 期望范式 | 实测 | 结论 |
|------|---------|------|------|
| finance 凭证行多维筛选（subject/partner/project/warehouse/material） | 机制 B（to-one + EQL 点导航） | ErpFinVoucherLine 全部建 to-one ✅ | PASS |
| inventory 列表显示物料/仓库名 | L1 冗余字段 OR 机制 B | 用机制 B（无冗余 `materialName` 字段） | P2-MA1-005 观察项（见 §7） |
| 详情页带出完整关联对象 | L3（@BizLoader + requireBiz） | 用机制 B（to-one 天然支持） | PASS |
| inventory stock_move 反查业务源单 | 弱指针三元组（机制 P） | `relatedBillType + relatedBillCode` + `sourceBillType + sourceBillCode` ✅ | PASS |
| finance voucher_bill_r 反查业务源单 | 弱指针三元组（机制 P） | `billType + billCode + billLineCode` ✅（owner doc 文字 `billHeadCode` 与实际字段名 `billCode` 偏差见 P2-MA1-003） | PASS |

### Dim 4 — 业财一体边界一致性 ⚠️ 1 项 P1（文档+代码层）

- ORM 层：finance 零业务表外键、零业务表 to-one 反向（除 phantom `ErpAstAsset` 未引用，见 P2-MA1-001） ✅
- 凭证反查源单用 `(billType, billCode, billLineCode)` 三元组，业务表不感知凭证 ✅
- 代码层（抽样 `ErpFinAccountingPeriodProcessor`）：
  - 期末结账调用 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation/reverseDepreciation`、`IErpInvCostingBiz.reclosePeriodCosts` —— **command/request 跨域编排**（finance 触发业务域自管实体的写），owner doc §3.2 仅描述 finance 对业务域的"只读查源单"，未覆盖此编排范式 → **P1-MA1-017**（owner doc §3.2/§4.4 规则不完整）
  - 第 389 行 `daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)` 直接跨域 DAO 查询 assets 实体，违反 `AGENTS.md "跨实体访问"` 规则（必须经 `I*Biz` 接口） → **P1-MA1-016**
  - 注：以上为 **I*Biz / IDaoProvider 层**，不影响 ORM DAG（ORM 层 finance 不反向引用业务域）。skill severity 指南中"blocker: 跨模块写反向（finance 回写业务）"针对 ORM 反向 to-one 写，此处是 I*Biz 命令调用，分类为 P1（major）。

### Dim 5 — 冗余字段策略 ⚠️ 1 项 P2（观察项）

owner doc §5.5.1 推荐 L1+L2+L3 三级并存（冗余显示名 + 外键 + @BizLoader）。实测：

- pur/sal/inv 主流业务表**几乎无冗余显示名字段**（`supplierName` / `customerName` / `materialName`），完全依赖机制 B（to-one + EQL 自动 LEFT JOIN）
- 散见冗余字段：cs.`partnerName`、fin 部分 `subjectName` 等

→ **P2-MA1-005**：实现路线偏 owner doc 推荐（用机制 B 替代冗余字段）。机制 B 支持 EQL 自动 LEFT JOIN 不产生 N+1，性能可接受；不构成违规，仅为策略偏差。owner doc §5.5.1 可补注"机制 B 单独使用亦为合法实现，L1 冗余字段为可选优化"。

### Dim 6 — Maven 依赖与 orm 声明对齐 ✅ PASS

逐域核验 `erp-<short>-dao/pom.xml` 与跨模块 ORM 边：

| 跨模块 ORM 边 | pom 是否含被引用方 -dao 依赖 | 备注 |
|--------------|-----------------------------|------|
| aps→md | ✅ md-dao | |
| ast→md | ✅ md-dao | pom 另有 fin-dao（code-level voucherId 弱指针反查 IErpFinVoucherBiz，owner doc §6.5 登记允许） |
| b2b→md | ✅ md-dao | |
| crm→md | ✅ md-dao | pom 另有 sal-dao（code-level 线索转客户 handoff，待 crm 业务深化） |
| cs→md | ✅ md-dao | |
| ct→md | ✅ md-dao | |
| drp→inv+md | ✅ inv-dao + md-dao | |
| fin→md+prj | ✅ md-dao + prj-dao | pom 不含 ast-dao（因 fin→ast ORM 边为 phantom，无实际 to-one 消费 ErpAstAsset 类） |
| hr→md+prj | ✅ md-dao + prj-dao | |
| inv→md | ✅ md-dao | |
| log→md | ✅ md-dao | |
| mfg→inv+md | ✅ inv-dao + md-dao | |
| mnt→ast+md | ✅ ast-dao + md-dao | |
| prj→md | ✅ md-dao | |
| pur→md+prj | ✅ md-dao + prj-dao | |
| qa→md | ✅ md-dao | |
| sal→md+prj | ✅ md-dao + prj-dao | |
| notify | （无跨模块 ORM 边） | pom 仅自身（notify 是跨域通知派发的叶订阅方） ✅ |

**Maven pom 与 ORM 声明完全对齐**。少量"超集"pom 依赖（ast→fin-dao、crm→sal-dao）属 code-level I*Biz 用途，已在 owner doc 登记。

### Dim 7 — 与 data-dependency-matrix.md 一致性 ⚠️ 文档数值偏差（P1）

| owner doc §5.6.2 自述 | 实测 | 偏差 | 处理 |
|---------------------|------|------|------|
| 17 业务域共约 **369 个跨模块 to-one** | **625 个 to-one**（不含 to-many；to-many = 0） | **+256（+69%）** | P1-MA1-015 — owner doc 自述"待 codegen 后跑脚本精确统一"，本审计即该脚本，更新 §5.6.2 数值 |
| 引用约 **68+ 个外部实体声明** | **111 个 notGenCode 声明** | **+43（+63%）** | 同上 P1-MA1-015 |
| inventory 引用 8 类 master-data（material/materialSku/warehouse/location/uom/currency/organization/acctSchema） | 实测 10 类（**+ Employee + Partner**） | +2 类 | P2-MA1-002 — owner doc §5.6.2 inventory 行补 Employee + Partner |
| §5.1 弱指针三元组 `(billType, billHeadCode, lineCode)` | voucher_bill_r 实际字段 `(billType, billCode, billLineCode)` | 字段名偏差 | P2-MA1-003 — owner doc 文字与实际字段名对齐 |
| §5.6.1 "finance → projects/assets" | 实测仅 projects 被 to-one 引用（assets 为 phantom 声明） | 文字 vs 实现偏差 | P2-MA1-004（与 P2-MA1-001 同源） |
| §5.6.3 DAG 合规性规则（禁止方向清单） | 全部禁止方向边数 = 0 | 一致 ✅ | 无偏差 |
| R/S/P 分类（R=只读外键、S=同事务写、P=弱指针反查） | ORM 层全是 R 类型；S 写在 code 层；P 用字符串三元组 | 一致 ✅ | 无偏差 |

## 4. F1–F9 部分闭包复核结论表

> 来源：`docs/audits/2026-07-23-0000-architecture-governance-review.md` + `docs/audits/audit-remediation-scope-and-dimension-matrix.md §3.1`。本审计**不重开**已闭包 finding，仅引用其结论；若复现已闭包 finding，标注引用不升级。

| Finding | 闭包 plan | 本审计复核结论 | 终态 |
|---------|----------|---------------|------|
| **F1** daoFor 跨域访问（~110-180 处真违规子集） | `2026-07-24-0941-1` | ORM 层 0 跨模块 refEntityName 漂移；daoFor Type 1/Type 4 residual 在 daoFor 调用层（非 ORM 层），不构成本审计的 P0；新发现 finance `daoProvider.daoFor(ErpAstDepreciationSchedule)` 是同类型 daoFor 跨域 → 登记 P1-MA1-016（不重开 F1，作为新 finding 进入 MR1） | ✅ 已闭包（watch-only residual 仍有效） |
| **F2** 字典与状态枚举真相碎裂 | `2026-07-24-0930-2` + successors | 不在本审计范围（字典非跨模块依赖结构维度） | ✅ 已闭包（不引用） |
| **F3** ORM DAG 边登记不完整 | `2026-07-24-0930-3` Phase 2 | §1.2 全部 7 类跨业务域登记例外（含 F3 闭包时补登的 drp→inv/mfg→inv/mnt→ast）确认存在且方向单向合法 | ✅ 已闭包确认 |
| **F4** 隐性共享内核（ErpFinBusinessType / PostingEvent / AcctSchemaResolver） | `2026-07-24-1400-1` | 本审计未触碰 code-level 共享类型；R12 守卫仍激活，基线 69/66/38 不变 | ✅ 已闭包确认（守卫持续生效） |
| **F5** notify 子系统无 owner doc | `2026-07-24-0930-3` Phase 3 | notify 在本审计中：0 跨模块 ORM 引用、0 外部实体声明（叶订阅方），符合"跨域通知派发子系统"定位；module-boundaries.md §Owner Docs 已含 notify 行 | ✅ 已闭包确认 |
| **F6** mfg 依赖 qa 生成常量 | `2026-07-24-1400-2` Phase 1 | mfg orm.xml 零 qa 引用（DAG 边仅 mfg→md + mfg→inv ErpInvBatch）；qa 常量依赖在 code 层（不在本审计范围） | ✅ 已闭包确认 |
| **F7** drp 命名前缀（ErpInvDrp*） | 裁决=登记例外 | drp→inv ErpInvStockMove 引用合法；4 实体 `ErpInvDrp*` 命名异常已在 P1-MA1-014 登记（A1.8 BC tier report），与本审计无新增 | ✅ 已闭包确认 |
| **F8** compliance checker 未集成 CI | `2026-07-24-0930-1` | 不在本审计范围 | ✅ 已闭包（不引用） |
| **F9** 19 web 冒烟测试 @Disabled | `2026-07-24-0930-1` | 不在本审计范围 | ✅ 已闭包（不引用） |

## 5. arch-gov §3.2 残留风险当前状态

| 来源 | 描述 | 严重性 | 当前状态 |
|------|------|--------|---------|
| arch-gov §残留风险 1 | daoFor `findAllByQuery` Type 1 watch-only residual（113 处站点） | P2 | watch-only，本审计发现 finance 新增 1 处（P1-MA1-016）登记入 MR1；其余 residual 仍 watch-only |
| arch-gov §残留风险 1 | daoFor Type 4 设计边界错误 | P1 | 本审计 ORM 层 0 残留（与 MA1 ORM 审计 A1.1-A1.9 结论一致）；codegen/daoFor 调用层归 MA4 代码质量审计（A4.5） |
| arch-gov §残留风险 1 | b2b→pur 跨域写 ErpB2bAsnBizModel 收敛条件 | P2 | deferred（待 pur 提供 createFromAsn I*Biz），本审计不重开 |
| arch-gov §残留风险 3 | §5.6.3 禁止清单是否增列 mfg→inv/drp→inv/mnt→ast | P2 | deferred（业务裁决归人工），本审计仅标注：当前 3 方向均为登记例外状态，未列入禁止清单 |
| arch-gov §残留风险 4 | governed path 成本评估需 nop-entropy 平台层 lazy/SPI 解耦 | P2 | deferred（超本项目范围） |

## 6. Finding 汇总

### 6.1 P0 发现（即时通道） — **0 项**

| Finding ID | 报告 | 描述 | 修复路径 | 修复状态 |
|-----------|------|------|---------|---------|
| _（全域跨模块审计 0 P0：DAG 零循环、外部实体声明 100% 覆盖、ORM 层零业务域反向写）_ | | | | |

**P0 即时通道未触发**。无需 fix plan 异步注入。

### 6.2 P1 发现（汇总至 arm-index §P1，目标 MR1） — **3 项**

| Finding ID | 域 | 描述 | 目标 MR | 修复状态 |
|-----------|----|------|--------|---------|
| `P1-MA1-015` | docs（全域） | owner doc §5.6.2 自述数值偏低：声称 ~369 to-one / ~68 external，实测 625 to-one / 111 external（+69% / +63%）。owner doc 文字已声明"待 codegen 后跑脚本精确统一"，本审计即该脚本。需用机器核验值更新 §5.6.2 数值与每域明细表 | MR1 | todo |
| `P1-MA1-016` | finance | `ErpFinAccountingPeriodProcessor.reverseDepreciation`（line ~389）使用 `daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)` 直接跨域 DAO 查询 assets 实体，违反 `AGENTS.md "跨实体访问"` + `data-dependency-matrix.md §5.3` 规则（跨域查询必须经 I*Biz 接口）。应改为 `IErpAstDepreciationScheduleBiz.findList()` 或等价 I*Biz 只读方法 | MR1 | todo |
| `P1-MA1-017` | docs（finance） | owner doc §3.2 / §4.4 "finance 对业务域纯读不回写"规则不完整：未覆盖期末结账期间的跨域 command/request 编排（finance 调 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation/reverseDepreciation` + `IErpInvCostingBiz.reclosePeriodCosts`）。需补注："纯读"指 ORM 层无反向 to-one；期间结账的 command 编排在 I*Biz 层合法（业务域自管实体的写） | MR1 | todo |

### 6.3 P2 发现（watch-only / out-of-scope，不进入 MR） — **5 项**

| Finding ID | 域 | 描述 | 处理 |
|-----------|----|------|------|
| `P2-MA1-001` | finance | `app-erp-finance.orm.xml:2355` 声明 `<entity notGenCode="true" name="app.erp.ast.dao.entity.ErpAstAsset">` 但全 orm 零 to-one 引用它（phantom declaration，dead ORM 声明） | watch-only：移除声明 OR 将来按 §5.6.1 设计原则 1 实现 finance→assets 凭证行辅助核算时启用 |
| `P2-MA1-002` | docs（inventory） | owner doc §5.6.2 inventory 行自述引用 8 类 master-data 表（material/materialSku/warehouse/location/uom/currency/organization/acctSchema），实测 10 类（**+ Employee 用于 picker + Partner 用于 owner/reservedForPartner/supplier/apPartner**） | watch-only：与 P1-MA1-015 一同更新 §5.6.2 inventory 行 |
| `P2-MA1-003` | docs（finance） | owner doc §5.1 / §5.6.4 文字 voucher_bill_r 弱指针三元组 `(billType, billHeadCode, lineCode)`，实际 ORM 字段为 `(billType, billCode, billLineCode)` — 文字 `billHeadCode` 应改为 `billCode`，`lineCode` 应改为 `billLineCode` | watch-only：文档校对项，与 P1-MA1-015 一并更新 |
| `P2-MA1-004` | docs（finance） | owner doc §5.6.1 设计原则 1 文字声称跨业务域允许引用含 "finance → projects/assets"，但实测 finance→assets ORM 边为 phantom（见 P2-MA1-001），仅 projects 被 to-one 实际使用 | watch-only：与 P1-MA1-015 / P2-MA1-001 一并裁决（实现 phantom 移除 OR 补 to-one 落实文字） |
| `P2-MA1-005` | docs（全域） | owner doc §5.5.1 推荐 L1 冗余显示名字段与 to-one 并存，实测 pur/sal/inv 等主流业务表几乎完全用机制 B（to-one + EQL 自动 LEFT JOIN）替代冗余字段 | watch-only：机制 B 单独使用合法（不产生 N+1），非违规。建议 §5.5.1 补注"机制 B 单独使用亦为合法实现，L1 冗余字段为可选优化" |

### 6.4 已知 deferred（不在本审计范围，引用 arch-gov §3.2 + plan §Deferred But Adjudicated）

- F1 daoFor Type 1 watch-only residual（~110-180 处真违规子集，机械替换候选 <10）：watch-only，不阻塞本审计闭包
- b2b→pur 跨域写半治理（待 pur 提供 createFromAsn）：deferred，successor = yes
- §5.6.3 禁止清单是否增列 mfg→inv / drp→inv / mnt→ast：业务裁决归人工，deferred，successor = yes

## 7. 残留风险

1. **owner doc §5.6.2 数值偏差 69%**（P1-MA1-015）：当前 owner doc 仍引用偏低的自述值。MR1 修复前，依赖该数值的下游文档/讨论需注意实际值为 625/111。审计脚本 `docs/audits/scripts/cross-module-dep-extract.py` 已可重跑提供权威值。
2. **finance 期末结账跨域编排无文档背书**（P1-MA1-017）：当前代码合法运行（mvn test 全绿），但 owner doc §3.2/§4.4 文字描述不完整可能误导后续审计。MA2 业务正确性审计（A2.3 期末结账端到端）将更深入复核该编排的正确性。
3. **daoFor 跨域 watch-only residual 仍存在**（arch-gov §3.2 残留风险 1）：本审计新增 P1-MA1-016 一例进入 MR1，其余 ~110-180 处仍 watch-only。
4. **命名异常（contract package 用全词 `contract` 而非短码 `ct`）**：与其他 18 域不一致（其他域 package 短码 = 实体前缀 = tableName 前缀）。不阻塞 DAG 合规，但增加工具脚本复杂度（本审计脚本已通过 `CANON_OF_PKG` 字典处理）。建议未来命名规范化时统一为 `ct`（与 drp F7 命名问题同类，归 MA1.14 治理复审）。

## 8. 与 skill 要求的对齐

`docs/skills/cross-module-dependency-audit-prompt.md` 要求报告必含：

- ✅ 引用边清单（§1）
- ✅ DAG 验证结果 ✅/❌（§1.1 + §3 Dim 1：✅ 零循环零禁止方向）
- ✅ 外部实体声明完整性矩阵（§2）
- ✅ 四个量化汇总字段（TL;DR §顶部）
- ✅ F1–F9 复核结论表（§4）
- ✅ 残留风险（§7）
- ✅ Finding 按 P0/P1/P2 分级（§6）
- ✅ 裁决：**通过（PASS）** — DAG 合规、外部声明 100% 覆盖、零 P0；3 项 P1（含 1 项代码 + 2 项文档）登记 MR1

## 9. 相关文档

- `docs/architecture/data-dependency-matrix.md` — 数据层依赖权威（本审计为 §5.6.2 的脚本核验）
- `docs/architecture/module-boundaries.md` — 模块级 DAG 方向
- `docs/skills/cross-module-dependency-audit-prompt.md` — 审计方法 skill
- `docs/audits/scripts/cross-module-dep-extract.py` — 自动化核验脚本（可重跑）
- `docs/audits/scripts/cross-module-dep-extract-output.md` — 完整原始输出（625 边 + 111 声明，含 file:line 锚点）
- `docs/audits/2026-07-23-0000-architecture-governance-review.md` — F1–F9 闭包来源
- `docs/audits/arm-index.md` — P1 发现汇总索引（本报告 3 项 P1 已登记）
