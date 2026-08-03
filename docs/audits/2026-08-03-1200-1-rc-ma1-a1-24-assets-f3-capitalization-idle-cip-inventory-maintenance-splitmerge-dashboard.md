# rc-ma1-a1-24 assets-F3 资本化/闲置/转固/盘点/维修/拆分合并/看板 需求-实现符合性五级追踪审计

> 报告状态：done
> 使命：requirement-compliance（MA1 五级追踪审计）
> 工作项：A1.24（MA1 需求追踪矩阵审计 — assets-F3 资本化/拆分/盘点/维修/看板）
> 切片：UC-AST-01/03/06/09/10/11/12（7 UC，逐 UC 一矩阵行，§3 完整枚举）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> 计划：`docs/plans/2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md`
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.24 UC 清单 = UC-AST-01/03/06/09/10/11/12，覆盖率 ✅ 一致，无基线分歧）

## 裁决摘要

| 级别 | 新登记 | 复用既有 | 明细 |
|------|--------|---------|------|
| **接受** | — | — | UC-AST-01 资本化入账 / UC-AST-06 在建工程转固 / UC-AST-10 资产维修（全验收标准 L3-L5 证据一致） |
| **接受（含 caveat）** | — | — | UC-AST-11 拆分合并（不可逆性是 owner doc 要求非缺口；DISPOSED vs SCRAPPED 复用 P2-MA1-023）/ UC-AST-12 看板（①③ 接受，④ 行级权限复用 P1-MA2-093） |
| **P1**（复用既有） | **0** | **1** | **P1-MA2-061**（UC-AST-03 IDLE 闲置停提 Deferred — §4 三判据复核：R1.18 resolution 为 owner doc Deferred 标注 only，(i) AI 子代理审计记录存在但非人工批准[methodology §4 line 168] + (ii) owner doc Deferred 无人工批准痕迹[git log 全 AI commits] + (iii) product-scope 未将 IDLE 列入范围裁剪 → 三判据在"人工批准"意义上不满足 → 倾向重开 P1 入 MR1，须人工确认 product-scope 范围裁剪） |
| **P2**（新登记） | **1** | — | **P2-RC-028**（UC-AST-09 盘点盘盈/盘亏完整处置链复用收窄为直接建卡/SCRAPPED — owner doc `inventory.md §四/§八` 显式 documented simplification，主路径[ASSET_INVENTORY_ADJUSTMENT 凭证]OK 边界[独立 CAPITALIZATION/DISPOSAL 凭证链]弱）→ successor watch-only |
| **P2**（复用既有） | — | **2** | **P1-MA2-093**（UC-AST-12 ④ 看板行级权限 — `loadInServiceAssets:176-181` 无 orgId scope，同根因同控制点[A2.18 orgId 查询隔离全仓未落地]）/ **P2-MA1-023**（UC-AST-11 DISPOSED vs L1:195 SCRAPPED — owner doc split-merge.md:103-104 documented 语义，watch-only） |

**整体裁决**：A1.24 切片 7 UC 五级追踪矩阵填齐。**零 P0**（无活跃数据破坏 / 会计过账破坏 / 安全漏洞 / 核心循环断裂）。7 UC 中 **3 UC 完全接受**（UC-AST-01/06/10）+ **2 UC 接受含 caveat**（UC-AST-11 不可逆性+DISPOSED drift / UC-AST-12 ①③接受④复用）+ **1 UC P1**（UC-AST-03 IDLE reuse P1-MA2-061，须人工确认 product-scope 范围裁剪）+ **1 UC P2**（UC-AST-09 盘点偏离 new P2-RC-028 watch-only）。**§4 三判据复核 P1-MA2-061 结论：在"人工批准"意义上不满足**（(i) AI 子代理审计 ≠ 人工批准 + (ii) 无人工批准痕迹 + (iii) product-scope 未裁剪），但 R1.18 plan 含独立子代理审计记录（criterion (i) 字面满足）；倾向重开 P1 入 MR1，**须人工确认 product-scope 是否裁剪 IDLE**（若裁剪 → §4 (iii) 改真相源非降级；若未裁剪 → P1 强制实现 suspend/resume BizMutation，Q4 会计正确性类无例外）。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## §1 需求契约原文（L1 use-case 验收标准逐字引用）

> L1 真相源 = `docs/design/assets/use-cases.md`（权威功能契约）。逐字引用，禁止转述（§1 L1 格式）。

### UC-AST-01 设备购置资本化入账（`use-cases.md:15-27`）

```
卡片: DRAFT → IN_SERVICE(资本化)
生成入账凭证: 借 固定资产, 贷 在建工程/银行存款/应付
自动生成折旧计划(按折旧方法/年限/残值)
卡片.已过账 == true
```

### UC-AST-03 资产闲置停提与恢复（`use-cases.md:50-61`）

```
IN_SERVICE → IDLE(闲置): 期间不参与折旧计提
IDLE → IN_SERVICE(恢复): 恢复计提
闲置期间不计提(折旧计划跳过)
```

### UC-AST-06 在建工程转固（`use-cases.md:101-112`）

```
在建工程余额 → 结转到 固定资产
生成转固凭证: 借 固定资产, 贷 在建工程
转固后开始折旧(生成折旧计划)
```

### UC-AST-09 资产盘点（`use-cases.md:147-162`）

```
盘点单(范围:部门/类别) → 录入实盘数量
差异 = 实盘 - 账面(卡片.数量)
盘盈 → 生成资产卡片增加(价值评估入账)
盘亏 → 触发处置流程(UC-AST-04 报废)或调查
盘点差异生成调整凭证(借/贷固定资产, 差额)
```

### UC-AST-10 资产维修（`use-cases.md:166-181`）

```
维修单(关联资产卡片) → 记录维修费用
若 维修延长寿命/提升效能 → 资本化(增加原值, 重算折旧计划)
否则 → 费用化(借 维修费用, 贷 存货/银行)
资本化维修: 卡片.原值 += 资本化金额, 折旧计划调整
维修费用可关联维护域(ErpMntVisit, 若设备资产)
```

### UC-AST-11 资产拆分与合并（`use-cases.md:185-205`）

```
// 拆分
原卡片 → 拆为 N 张新卡片
新卡片.原值/累计折旧/净值 按 proportion 分配
Σ 新卡片.原值 == 原卡片.原值(平衡)
原卡片状态 → SCRAPPED 或保留(按配置), 生成拆分凭证

// 合并
N 张卡片 → 合并为 1 张
新卡片.原值 = Σ 原卡片.原值
新卡片.累计折旧 = Σ 原卡片.累计折旧
原卡片状态 → SCRAPPED, 生成合并凭证
拆分/合并不影响总账平衡(总资产不变)
```

### UC-AST-12 资产看板（`use-cases.md:213-228`）

```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  原值/累计折旧/净值, 本期折旧, 类别分布, 折旧未计提预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

---

## §2 实现证据（L3 代码路径，含跨域调用链）

> §1 L3 格式：`file:line`，含行号，跨域调用链列全。

| UC | L3 代码路径（含跨域调用链） |
|----|------|
| **UC-AST-01** | `ErpAstAssetCapitalizationBizModel.java:17-28`（thin Facade）→ `ErpAstAssetCapitalizationProcessor.java:65 approve()` → `:69 executeApprove()`：`:70 createAndActivateAsset()`（`:201 setStatus(IN_SERVICE)` + 设 originalValue/currentValue/netBookValue/residualValue/usefulLifeMonths）→ `:71 generateDepreciationSchedule()`（`:206-241` 从 capitalizationDate.plusMonths(1) 生成 N 月 PENDING 计划，直线法含末期残值修正 `:243-253`）→ `:74 postingDispatcher.tryPost(cap)`。reverse `:92-115`。跨域 `CapitalizationPostingDispatcher.java:55 tryPost()` → `AssetPostingExecutor.java:27 postEvent()` → `IErpFinVoucherBiz.post()`；失败 `IErpSysNotificationBiz.notify("ast.capitalization-posting-failure",...)` |
| **UC-AST-03** | `ErpAstConstants.java:67 ASSET_STATUS_IDLE="IDLE"` 常量定义；`setStatus(IDLE)` 在 `src/main` **零 writer**（实测 4 命中 = 1 def + 3 read-only guard：`ErpAstValueAdjustmentProcessor:196` + `ErpAstDisposalProcessor:194` + `ErpAstInventoryProcessor:113`）；`ErpAstAssetBizModel.java`（17 行 CRUD 桩，`ErpAstAsset.xbiz` `<actions/>` 空）；grep `pauseDepreciation\|resumeDepreciation\|setIdle\|markIdle\|suspend` **零匹配**；折旧引擎 `ErpAstDepreciationScheduleExecuteDepreciationProcessor.java:40 validateAssetInService()` + `ExecuteBatchDepreciationProcessor:42` 仅查 `IN_SERVICE`（IDLE 被静默跳过≈pause 语义，但无 resume 动作） |
| **UC-AST-06** | `ErpAstCipBizModel.java:33-120`（Facade 6 mutations：`:60 startConstruction` / `:66 addCostItem` / `:79 addProgressBilling` / `:106 transferToAsset(cipId,costItemIds,transferDate)` / `:115 reverseTransfer`）→ `ErpAstCipTransferToAssetProcessor.java:22-34 transferToAsset()`（6 步：requireCip→resolveCostItems→validateTransferable→buildCapitalizationRequest→doTransfer 创建 ErpAstAssetCapitalization→postProcess，支持 costItemIds 白名单 partial transfer）；doTransfer 复用 UC-AST-01 资本化链 → `CapitalizationPostingDispatcher`（creditSubject 默认 1603 CIP，`:127`） |
| **UC-AST-09** | `ErpAstInventoryBizModel.java:26-104`（8 mutations）→ `ErpAstInventoryProcessor.java:42`（`:90 expandAssetsToLines()` 范围扩展 filter status IN (IN_SERVICE,IDLE) `:112-113`，range 字段 orgId/rangeDepartmentId/rangeCategoryId/rangeLocationId）+ 7 per-mutation processors（R6.3 split）。跨域过账 `AssetInventoryPostingDispatcher` → businessType `ASSET_INVENTORY_ADJUSTMENT(460)` + `AssetInventoryAcctDocProvider`（盘盈 借1601/贷6301，盘亏 借6711/贷1601）。盘盈直接建卡 IN_SERVICE（`ErpAstInventoryProcessor:237`）+ 盘亏直接 SCRAPPED |
| **UC-AST-10** | `ErpAstMaintenanceBizModel.java:33-131`（9 mutations：`:70 createMaintenance(assetId,code,name,businessDate,maintenanceVisitId,reason)` / `:101 decideTreatment(treatment,capitalizedAmount)` CAPITALIZE/EXPENSE / `:110 approve` / `:116 post` / `:128 reverse`）→ `ErpAstMaintenanceProcessor.java` + 8 per-mutation processors（R6.3 split）。跨域 `MaintenanceExpensePostingDispatcher`(470) + `MaintenanceCapitalizationPostingDispatcher`(480) → `IErpFinVoucherBiz.post()`；资本化路径 `ErpAstMaintenanceProcessor:96` 调 `IErpAstDepreciationScheduleBiz.recalculateForCapitalizationMaintenance(assetId,increment)` 加性扩展重算折旧（reverse `:113` 调 `.negate()`）。weak link `maintenanceVisitId`→`IErpMntVisitBiz`（assets→maintenance R-only） |
| **UC-AST-11** | `ErpAstSplitBizModel.java:24-39` + `ErpAstMergeBizModel.java:24-39`（Facade）→ `ErpAstSplitProcessor.java:46-546`（`:89 executeApprove()` 6 步：validateBeforeExecute→`:95 computeAllocation()` 含 `PROPORTION_TOLERANCE=0.000001`[:48] + `AMOUNT_TOLERANCE=0.01`[:49] + max-item residual fix→`:98 createTargetAssets()` N 张新 ErpAstAsset+折旧计划 target IN_SERVICE `:368`→`:101 disposeSourceAsset()` source→**DISPOSED**[NBV=0]→`:112 doPost()`→postProcess）+ `ErpAstMergeProcessor.java`（对称，`:329 target IN_SERVICE`）+ 12 per-mutation processors。跨域 `AssetSplitPostingDispatcher` + `AssetMergePostingDispatcher` → `IErpFinVoucherBiz.post()`。**特殊契约**：`reverseApprove` 抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`（`ErpAstSplitReverseApproveProcessor`）/ `ERR_AST_MERGE_REVERSE_NOT_SUPPORTED`（owner doc `split-merge.md §关键业务规则 5`） |
| **UC-AST-12** | `ErpAstDashboardBizModel.java:47-229`（service-type BizObject，4 @BizQuery：`:56 getDashboardKpi(periodId)` → originalValue/accumulatedDepreciation/netBookValue/periodDepreciation/cipBalance / `:84 getAssetCategoryDistribution()` / `:111 getDashboardTrend(months=12)` / `:141 findDepreciationMissingAlert()` IN_SERVICE 无 EXECUTED 折旧计划资产）。`loadInServiceAssets():176-181` 发出 `QueryBean` 仅 `eq("status", IN_SERVICE)` **无 orgId scope**；`IServiceContext context` 收而不用（`:57/:84/:112/:141`）。纯域内读聚合（ErpAstAsset/Category/DepreciationSchedule/Cip），无跨域调用 |

---

## §3 测试证据（L4 测试断言，注明断言强度）

| UC | L4 测试断言 | 断言强度 |
|----|------------|---------|
| **UC-AST-01** | `TestErpAstCapitalization.java`（3 @Test：asserts IN_SERVICE + posted + 折旧计划生成）+ `TestErpAstPostingReverse.java`（posted=true 红冲闭环）+ `TestErpAstAcctDocProviderAccountKey.java`（4 facts 科目方向） | **强**（状态迁移 + 过账 + 科目 + 红冲） |
| **UC-AST-03** | **无测试**（IDLE 仅 `TestErpAstDashboard.java` 用作 filter 测试数据，无 pause/resume 行为测试） | **零覆盖** |
| **UC-AST-06** | `TestErpAstCipTransfer.java`（8 @Test：asserts IN_SERVICE + 转固凭证 + partial transfer + reverse） | **强**（转固链 + partial + reverse） |
| **UC-AST-09** | `TestErpAstInventory.java`（5 @Test：surplus 新卡 IN_SERVICE + SHORTAGE SCRAPPED + 差异凭证 + reverse） | **强**（差异引擎 + 盘盈建卡 + 盘亏处置 + 凭证） |
| **UC-AST-10** | `TestErpAstMaintenance.java`（15 @Test：CAPITALIZE 原值增量 + 折旧重算 + EXPENSE 凭证 + reverse + 阈值门控 + maintenanceVisitId 分支） | **强**（资本化/费用化裁决 + 折旧重算 + 防双重扣减） |
| **UC-AST-11** | `TestErpAstSplitMerge.java`（8 @Test：Σ 平衡 + target IN_SERVICE + source DISPOSED + 凭证 + reverse 抛 NOT_SUPPORTED） | **强**（平衡约束 + 状态迁移 + 不可逆契约） |
| **UC-AST-12** | `TestErpAstDashboard.java`（KPI 聚合数值 + 类别分布 + 折旧未计提预警） | **强**（聚合算术 + 预警）；**无行级权限测试**（`enableActionAuth=FALSE`） |

---

## §4 运行时行为证据（L5）

> §1 L5 格式：复用既有 MA2 报告已证实行为（§去重协议），不重复验证行为本身。

| UC | L5 行为证据来源 |
|----|----------------|
| **UC-AST-01** | A2.10（`2026-07-28-0400-arm-ma2-assets-state-machine.md` §场景(a) 设备购置与折旧 happy path PASS + §维度 2/3 资本化迁移守卫 PASS）；A4.3（`2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md` 48 Processor 代码质量 PASS）；A5.4（`2026-07-29-1430-arm-ma5-assets-test-coverage.md` 主路径断言强度扎实） |
| **UC-AST-03** | A2.10（§场景(b) 资产闲置与恢复 **FAIL** — P1-MA2-061；IDLE 经证据确认无任何 writer，是事实上的死状态）；折旧引擎仅查 IN_SERVICE 等价于"IDLE 默认停提"语义（owner doc §1 设计意图） |
| **UC-AST-06** | A2.10（§场景(a) 转固路径 PASS — 复用资本化链）；本切片 HEAD 静态确认 `ErpAstCipTransferToAssetProcessor` 6 步编排 + costItemIds 白名单 partial transfer 实现 |
| **UC-AST-09** | A2.10（盘点 Processor 副作用 + ASSET_INVENTORY_ADJUSTMENT 过账 PASS）；A4.3（代码质量 PASS）；本切片 HEAD 静态确认盘盈直接建卡 IN_SERVICE `:237` + 盘亏 SCRAPPED（owner doc `inventory.md §四/§八` 显式偏离记录） |
| **UC-AST-10** | A2.10（维修 Processor 六态状态机 + 470/480 过账 PASS）；A4.3（代码质量 PASS）；A5.4（15 @Test 强覆盖）；本切片 HEAD 静态确认 `recalculateForCapitalizationMaintenance` 加性扩展重算（删 PENDING + 按剩余月数重生成 + 残值约束） |
| **UC-AST-11** | A2.10（§场景(f) 拆分合并 PASS — DISPOSED 经证据可达非死状态，证伪 P2-MA1-023；Split/Merge reverseApprove THROW 不可逆契约合规）；A4.3（代码质量 PASS）；A5.4（平衡约束 + target IN_SERVICE 强断言） |
| **UC-AST-12** | A2.10（dashboard BizModel 聚合查询 PASS）；A5.4（聚合算术强覆盖）；本切片 HEAD 静态确认 `loadInServiceAssets:176-181` 无 orgId scope（同 P1-MA2-093 根因） |

---

## §5 符合性结论（五级追踪矩阵 + 每 UC 符合性结论）

### 五级追踪矩阵

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---------|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-AST-01** | `use-cases.md:15-27`（4 断言：①DRAFT→IN_SERVICE ②入账凭证[借固定资产/贷在建工程·银行·应付] ③自动生成折旧计划[按方法/年限/残值] ④卡片.已过账==true）— 原文见 §1 | `depreciation-and-posting.md §二 资本化入账流程:77-127`（§2.1 场景表 + §2.2 流程 + §2.3 在建工程转固）+ `state-machine.md §2 迁移 DRAFT→IN_SERVICE:40`（**设计参考，与 L1 一致**） | 见 §2 UC-AST-01 行（资本化链 + 跨域 CapitalizationPostingDispatcher→IErpFinVoucherBiz） | 见 §3（3 @Test 强 + 红冲 + 科目方向） | 行为已证实（A2.10 §场景(a) PASS + A4.3 + A5.4） | **接受**（4 断言全实现：①`:201 setStatus(IN_SERVICE)` ②CapitalizationPostingDispatcher→IErpFinVoucherBiz ③`generateDepreciationSchedule:206-241` 直线法+末期残值修正 ④`tryPost:74` posted=true） |
| **UC-AST-03** | `use-cases.md:50-61`（3 断言：①IN_SERVICE→IDLE 闲置期间不参与折旧 ②IDLE→IN_SERVICE 恢复计提 ③闲置期间不计提[折旧计划跳过]）— 原文见 §1 | `state-machine.md §1:19`（IDLE ⚠ 预留状态）+ `§2:41-42`（IN_SERVICE↔IDLE ⚠ **Deferred**——本期无 writer 不可达）+ `§5:74`（⚠ 可达性修正 Deferred）+ `§8:109,114`（闲置超期 TODO Deferred）— **owner doc 显式 Deferred 标注** | 见 §2 UC-AST-03 行（IDLE 零 writer + CRUD 桩 + 折旧引擎仅查 IN_SERVICE） | **零覆盖**（无 pause/resume 行为测试） | 行为已证实缺失（A2.10 §场景(b) **FAIL** — P1-MA2-061；IDLE 死状态） | **P1**（reuse **P1-MA2-061**）— §4 三判据复核见下；断言①②迁移完全缺失，断言③折旧引擎仅查 IN_SERVICE 等价"默认停提"部分满足 |
| **UC-AST-06** | `use-cases.md:101-112`（3 断言：①在建工程余额→结转到固定资产 ②生成转固凭证[借固定资产/贷在建工程] ③转固后开始折旧[生成折旧计划]）— 原文见 §1 | `depreciation-and-posting.md §二:77-127`（§2.3 在建工程转固:116-126）— **设计参考，与 L1 一致** | 见 §2 UC-AST-06 行（CipBizModel.transferToAsset:106 + CipTransferToAssetProcessor:22 + 复用资本化链 creditSubject=1603） | 见 §3（8 @Test 强） | 行为已证实（A2.10 §场景(a) 转固路径 PASS） | **接受**（3 断言全实现：①transferToAsset+复用资本化建卡 ②CapitalizationPostingDispatcher creditSubject=1603 CIP ③复用 generateDepreciationSchedule） |
| **UC-AST-09** | `use-cases.md:147-162`（5 断言：①盘点单[范围:部门/类别]→录入实盘 ②差异=实盘−账面 ③盘盈→生成资产卡片增加[价值评估入账] ④盘亏→触发处置流程[UC-AST-04 报废]或调查 ⑤盘点差异生成调整凭证[借/贷固定资产,差额]）— 原文见 §1 | `inventory.md §一-§八`（四态状态机 + 差异引擎 + ASSET_INVENTORY_ADJUSTMENT 科目映射）+ **§四:60-64**（盘盈/盘亏处置链复用收窄为直接建卡/SCRAPPED，避免双重过账）+ **§八:88-92**（实现约定：复用收窄 documented）— **owner doc 显式 documented simplification** | 见 §2 UC-AST-09 行（InventoryBizModel 8 mutations + expandAssetsToLines:90 range + 盘盈直接建卡:237 + 盘亏 SCRAPPED + 460 凭证） | 见 §3（5 @Test 强） | 行为已证实（A2.10 盘点 PASS + A4.3） | **接受 on ①②⑤；P2 on ③④**（盘盈直接建卡+盘亏SCRAPPED 实现 ③④ 的净效果[资产增加/处置终态]，但未走独立 CAPITALIZATION/DISPOSAL 凭证链，owner doc §四/§八 显式 documented → **新建 P2-RC-028** watch-only） |
| **UC-AST-10** | `use-cases.md:166-181`（5 断言：①维修单关联资产卡片→记录维修费用 ②延长寿命/提升效能→资本化[增加原值,重算折旧计划] ③否则→费用化[借维修费用,贷存货/银行] ④资本化维修:卡片.原值+=资本化金额,折旧计划调整 ⑤维修费用可关联维护域[ErpMntVisit]）— 原文见 §1 | `maintenance.md §一-§十`（六态状态机 + CAPITALIZE/EXPENSE 裁决 + §三 折旧重算规则 + §四 科目映射 470/480 + §五 maintenance 域边界）— **设计参考，与 L1 一致** | 见 §2 UC-AST-10 行（MaintenanceBizModel 9 mutations + decideTreatment + recalculateForCapitalizationMaintenance:96 + 470/480 凭证 + maintenanceVisitId 弱关联） | 见 §3（15 @Test 强） | 行为已证实（A2.10 维修 PASS + A4.3 + A5.4） | **接受**（5 断言全实现：①createMaintenance ②decideTreatment(CAPITALIZE)+recalculate ③decideTreatment(EXPENSE)+470 ④原值+=increment+折旧重算 ⑤maintenanceVisitId→IErpMntVisitBiz R-only） |
| **UC-AST-11** | `use-cases.md:185-205`（拆分 4 断言：①原卡片→N 张新卡片 ②按 proportion 分配 ③Σ 平衡 ④原卡片→SCRAPPED 或保留+凭证；合并 4 断言：⑤N→1 ⑥原值=Σ ⑦原卡片→SCRAPPED+凭证 ⑧不影响总账平衡）— 原文见 §1 | `split-merge.md §流程 + §关键业务规则:70-78`（规则 5 **不可逆**：执行后不可撤销）+ `§实现注记:87-130`（DISPOSED 语义 + 分离实体 + 不可逆契约遵守声明）— **设计参考；L1:195 SCRAPPED vs 实现 DISPOSED 语义漂移**（owner doc split-merge.md:103-104 documented） | 见 §2 UC-AST-11 行（SplitProcessor 546L + MergeProcessor 486L + PROPORTION_TOLERANCE/AMOUNT_TOLERANCE + max-item residual fix + reverse 抛 NOT_SUPPORTED） | 见 §3（8 @Test 强） | 行为已证实（A2.10 §场景(f) PASS — DISPOSED 经证据可达非死状态，证伪 P2-MA1-023；不可逆契约合规） | **接受 on ①②③⑤⑥⑦⑧；④ DISPOSED vs SCRAPPED reuse P2-MA1-023**（拆分合并全断言实现；不可逆性是 owner doc §关键业务规则 5 要求非缺口；DISPOSED vs L1:195 SCRAPPED 是 owner doc split-merge.md:103-104 documented 语义=内部重组无损 vs 报废有损，**reuse P2-MA1-023** owner doc drift） |
| **UC-AST-12** | `use-cases.md:213-228`（3 断言：①KPI 数据源正确[实时聚合,非硬编码] ②预警项==满足阈值条件[阈值来自系统配置] ③看板数据受行级权限约束[只看自己组织/部门/成本中心]）— 原文见 §1 | `dashboards.md §5 资产看板:125-141`（指标表 + 数据源）+ `§设计原则 4:12`（行级权限）+ `§实现约定 3:240`（权限过滤 orgId/部门/成本中心）— **设计参考** | 见 §2 UC-AST-12 行（DashboardBizModel 4 @BizQuery 实时聚合 + loadInServiceAssets:176-181 **无 orgId scope**） | 见 §3（强聚合 + 预警；**无行级权限测试**） | 行为已证实（A2.10 dashboard 聚合 PASS）；**行级权限未落地**（loadInServiceAssets 无 orgId scope，同 P1-MA2-093 根因） | **接受 on ①②；③ reuse P1-MA2-093**（实时聚合 + config 阈值实现；行级权限 `loadInServiceAssets:176-181` 无 orgId scope + IServiceContext 收而不用，**reuse P1-MA2-093**[A2.18 orgId 查询隔离全仓未落地]） |

### §4 三判据复核：P1-MA2-061（UC-AST-03 IDLE Deferred）

> 方法论 §4「显式人工批准记录」三判据，用于复核 R1.18 resolution（方案 A owner doc Deferred 标注 only）是否构成合法 documented simplification。

| 判据 | 证据要求 | P1-MA2-061 复核结果 |
|------|---------|---------------------|
| **(i) plan 含独立 plan-audit 通过记录** | plan 的 `Draft Review Record` / `## Closure` 含独立子代理或审查者的通过证据 | **字面满足但非人工批准**：R1.18 plan（`docs/plans/2026-07-30-0512-1-r1-18-assets-idle-state-machine-deferred.md`）含 `Draft Review Record`（`ses_05030be58ffeFgem8okz3j5sGt` accept）+ `Closure Audit Evidence`（`ses_05026887effe2xh4KPe9r17nDY` PASS）。但 methodology §4 line 168 明确：「代理独立审计通过 = '审计裁决质量证据'...**不算**人工批准」。两 ses_ 均为 AI 子代理会话，非人工审查者 |
| **(ii) owner doc 显式 documented simplification + 人工批准痕迹** | owner doc 含显式 Deferred 段落 + 批准来源可追溯（git log / commit message / 讨论文档） | **owner doc Deferred 存在但无人工批准痕迹**：`state-machine.md §1:19 / §2:41-46 / §5:74 / §8:109,114` 含详尽 Deferred 标注（successor 触发条件 + 折旧引擎等价语义说明）。但 git log（`f02b763d9` 等）全为 AI-authored `docs(audit-remediation):` commits，无讨论文档 / 无人工批准 marker |
| **(iii) product-scope 范围裁剪登记** | product-scope 明确将功能列入"不在范围"或"后续阶段" + 理由 + 影响面 + 批准人 | **不满足**：`docs/requirements/product-scope.md` grep `IDLE\|闲置\|资产暂停` **零命中**——IDLE 未列入范围裁剪 |

**§4 三判据复核结论**：**在"人工批准"意义上不满足**。(i) 字面满足（AI 子代理审计记录存在）但 methodology §4 line 168 明确独立审计 ≠ 人工批准；(ii) owner doc Deferred 标注存在但无人工批准痕迹；(iii) product-scope 未裁剪。按 Q4=(a) P1 禁方案 B 关闭规则，**倾向重开 P1 入 MR1**——但本切片 Non-Goal 不自决范围（§11 范围内项目降级须人工确认），**须人工确认 product-scope 是否裁剪 IDLE**：
- 若裁剪 → §4 (iii) 改 product-scope 真相源（需求变更非降级）
- 若未裁剪 → P1 强制实现 `suspend`/`resume` BizMutation（IN_SERVICE↔IDLE 迁移 + setStatus writer + 折旧引擎扩展查询 + 闲置超期 TODO），Q4 会计正确性类无例外（闲置期间折旧错报影响累计折旧/净值/折旧费用）

**与既有 finding 关系**：P1-MA2-061 是 IDLE 死状态的既有 finding（A2.10 登记）。本切片为**复用**（§7 同根因同控制点）——追加 RC 视角 §4 三判据复核注记，不新建编号。

### 候选缺口分级裁决汇总

| # | UC | 候选缺口 | 分级 | §2 判据 | finding 裁决 |
|---|-----|---------|------|---------|-------------|
| #1 | UC-AST-03 | IDLE 闲置停提 Deferred（零 writer + 无 BizMutation + 折旧引擎仅查 IN_SERVICE） | **P1**（须人工确认范围裁剪） | §2 P1①（功能完全缺失——①②迁移零实现）+ §5 Q4（会计正确性类——闲置期间折旧错报） | **reuse P1-MA2-061**（§4 三判据复核不满足人工批准意义 → 倾向重开 P1，须人工确认 product-scope） |
| #2 | UC-AST-11 | 拆分合并不可逆（`ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`） | **接受** | L1:185-205 未要求 reverse；owner doc `split-merge.md §关键业务规则 5` 显式要求不可逆 | **接受**（owner doc 契约要求，非缺口） |
| #3 | UC-AST-09 | 盘点盘盈/盘亏完整处置链复用收窄（直接建卡/SCRAPPED，非独立 CAPITALIZATION/DISPOSAL 凭证） | **P2**（watch-only） | §2 P2①（次要验收标准——③④净效果实现但凭证链复用收窄；owner doc §四/§八 documented） | **新建 P2-RC-028** |
| #4 | UC-AST-12 | 看板行级权限 caveat（`loadInServiceAssets:176-181` 无 orgId scope） | **P1**（复用） | §2 P1①（功能未落地——IServiceContext 收而不用） | **reuse P1-MA2-093**（A2.18 orgId 查询隔离全仓未落地，同根因同控制点） |
| #5 | UC-AST-10 | 维修资本化折旧重算正确性（`recalculateForCapitalizationMaintenance` 加性扩展） | **接受** | L1:173「重算折旧计划」+ L1:175「折旧计划调整」——实现删 PENDING + 按剩余月数重生成 + 残值约束，语义等价 | **接受**（加性扩展满足"折旧计划调整"语义，15 @Test 强覆盖） |
| #6 | UC-AST-01 | 资本化折旧计划生成（直线法 + 末期残值修正） | **接受** | L1:23「按折旧方法/年限/残值」——`generateDepreciationSchedule:206-241` 从 capitalizationDate.plusMonths(1) 生成 N 月 PENDING + 直线法末期残值修正 :243-253 | **接受**（直线法 + 残值约束实现，3 @Test 强覆盖） |

---

## §6 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：每条 finding 产出前必须 grep arm-index 同域同控制点后给出裁决。

### §6.1 复用 or 新增 裁决表

| 候选缺口 | arm-index grep 结果 | 裁决 | 差异依据 |
|---------|---------------------|------|---------|
| **#1 UC-AST-03 IDLE Deferred** | **P1-MA2-061**（A2.10 `:315`，IDLE 状态机迁移完全未实现 + 联动缺失，resolved R1.18 方案 A doc-only Deferred）— **同根因同控制点**（IDLE 零 writer + 折旧引擎仅查 IN_SERVICE + 无 suspend/resume BizMutation） | **reuse P1-MA2-061** | §7 同根因（IDLE 死状态）同控制点（IN_SERVICE↔IDLE 迁移）→ 复用不新建。本切片追加 RC 视角 §4 三判据复核注记（R1.18 resolution 在"人工批准"意义上不满足 → 倾向重开 P1，须人工确认 product-scope） |
| **#2 UC-AST-11 不可逆性** | L1 未要求 reverse + owner doc `split-merge.md §关键业务规则 5` 显式要求不可逆 | **不新建 finding**（接受，owner doc 契约要求非缺口） | — |
| **#3 UC-AST-09 盘点偏离** | arm-index grep「盘点」「盘盈」「盘亏」「inventory 偏离」「SURPLUS」「SHORTAGE」assets 域——P2-MA2-061（6 业务单据 cancel 死代码，**不同控制点**）/ P1-MA1-008（propId，不同维度）；**RC 系列对 assets 盘点偏离零命中** | **新建 P2-RC-028** | assets 域首个盘点盘盈/盘亏完整处置链复用收窄 finding。owner doc `inventory.md §四/§八` 显式 documented simplification（避免双重过账），主路径[ASSET_INVENTORY_ADJUSTMENT 凭证]OK 边界[独立 CAPITALIZATION/DISPOSAL 凭证链]弱 |
| **#4 UC-AST-12 行级权限** | **P1-MA2-093**（A2.18 `:99-101`，orgId 查询隔离全仓未落地，显式列 dashboard BizModel 经 IDaoProvider 直访）— **同根因同控制点**（loadInServiceAssets 无 orgId scope + IServiceContext 收而不用 + 空 data-auth.xml）；resolved R1.29（`ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer 注入） | **reuse P1-MA2-093** | §7 同根因（orgId 查询隔离未落地）同控制点（dashboard 行级权限）→ 复用不新建。A1.7 UC-FIN-17⑫ + A1.11 UC-MFG-11 已复用同一 finding，本切片追加 assets dashboard 投影注记。**注**：R1.29 已 resolved（全局 IQueryTransformer），故本 finding 维持 resolved watch-only，assets 视角无增量升级 |
| **#6 UC-AST-11 DISPOSED vs SCRAPPED** | **P2-MA1-023**（A2.10 `:301,281`，state-machine.md §1 列 5 态 vs dict 6 态[多 DISPOSED]，owner doc drift）— **同根因**（DISPOSED 由 split-merge 引入，owner doc state-machine.md §1 漏更新） | **reuse P2-MA1-023** | §7 同根因（DISPOSED 语义引入）→ 复用。本切片追加 RC 视角注记：L1:195「SCRAPPED 或保留」vs 实现 DISPOSED 是 owner doc split-merge.md:103-104 documented 语义细化（DISPOSED=内部重组无损 vs SCRAPPED=报废有损），watch-only |

### §6.2 P1-MA2-061 §4 复核结论记录

- **复核结论**：§4 三判据在"人工批准"意义上**不满足**（(i) AI 子代理审计 ≠ 人工批准[methodology §4 line 168] + (ii) owner doc Deferred 无人工批准痕迹 + (iii) product-scope 未裁剪）。
- **裁决**：**倾向重开 P1 入 MR1**，但须人工确认 product-scope 是否裁剪 IDLE（本审计 Non-Goal 不自决范围）。若裁剪 → §4 (iii) 改真相源；若未裁剪 → P1 强制实现。
- **MR1 修复方向**（登记，不实施）：`ErpAstAssetBizModel` 增 `suspend(assetId)`/`resume(assetId)` BizMutation（IN_SERVICE↔IDLE 迁移 + setStatus writer）+ 折旧引擎扩展查询 IN_SERVICE+IDLE（config-gated 是否对 IDLE 计提）+ 闲置超期 TODO cron（经 `IErpSysNotificationBiz`）。**触及 ORM 结构变更[IDLE 相关字段] + 折旧计提业务逻辑，须 ask-first + 独立 plan-audit（§5）**。

### §6.3 finding → 修复追踪

| Finding | 目标 MR | 触及保护区域 | 修复状态 |
|---------|--------|-------------|---------|
| **P1-MA2-061**（reuse，§4 复核倾向重开） | MR1（R1.0 → RC-R1.n）/ §4 (iii) product-scope 修订（若人工确认裁剪） | **是——ORM 结构变更 + 折旧计提逻辑**（suspend/resume BizMutation + 折旧引擎扩展 + 闲置超期 cron） | todo（本审计仅登记；**先须人工确认 product-scope 是否裁剪 IDLE**） |
| **P2-RC-028**（新建） | successor watch-only（P2 登记不强制） | 否（纯 BizModel 代码逻辑——盘盈路径可选触发 IErpAstAssetCapitalizationBiz 完整链 / 盘亏路径可选触发 IErpAstDisposalBiz 完整链，config-gated；或 owner doc §四/§八 已 documented 维持。**按 roadmap 预授权类目[代码逻辑修复]可自动执行，不触发 §5 ask-first**） | todo |
| **P1-MA2-093**（reuse，resolved R1.29） | MR1（resolved） | N/A（已 resolved） | resolved（R1.29 全局 IQueryTransformer 注入；assets dashboard 视角维持 resolved watch-only） |
| **P2-MA1-023**（reuse，watch-only） | MR1（owner doc drift） | 否（纯文档修复——state-machine.md §1 补 DISPOSED 态 + 注明 L1:195 SCRAPPED 与 DISPOSED 语义关系；**纯文档修复可自动执行，不触发 §5 ask-first**） | todo（watch-only） |

---

## §7 静态存疑点清单（供 MA4 展开）

> §1 L5 存疑点：L5 无法静态定论、需运行时确认的点。每存疑点一行。

| # | 存疑点 | 静态判定 | MA4 展开方向 |
|---|--------|---------|-------------|
| SP-1 | UC-AST-01 资本化折旧计划末期残值修正的实际取值行为（`:243-253` 直线法末期补差到残值——非整数月数/非零残值边界下每期折旧额是否精确收敛到残值） | 静态：直线法 (原值−残值)/月数 + 末期补差，残值约束兜底；非零残值 + 非整除月数边界需运行时确认 | A4.1 运行时：构造原值=100000/残值=5000/月数=36（非整除），断言末期净值精确=残值 |
| SP-2 | UC-AST-10 维修资本化重算后折旧计划行 PENDING→EXECUTED 的迁移正确性（`recalculateForCapitalizationMaintenance` 删 PENDING + 重生成——已 EXECUTED 条目保留，重生成的新 PENDING 行在后续 executeDepreciation 是否正确消费） | 静态：重算保留 EXECUTED + 重生成 PENDING（owner doc maintenance.md §三:67-72）；后续 executeDepreciation 消费新 PENDING 的端到端正确性需运行时确认 | A4.1 运行时：构造资产已执行 3 月折旧 → 资本化维修增量 → 重算 → 第 4 月 executeDepreciation 消费新 PENDING，断言折旧额=新基数/剩余月数 |
| SP-3 | UC-AST-11 拆分 proportion tolerance 在极端比例下的平衡行为（PROPORTION_TOLERANCE=0.000001 + AMOUNT_TOLERANCE=0.01 + max-item residual fix——3+ 目标 + 比例和=1.000001 边界下 Σ 原值是否精确=源原值） | 静态：tolerance 校验 + max-item 补差（owner doc split-merge.md §关键业务规则 1）；极端比例边界需运行时确认 | A4.1 运行时：构造 3 目标比例 0.333333+0.333333+0.333334，断言 Σ 原值==源原值（AMOUNT_TOLERANCE 内） |
| SP-4 | UC-AST-12 看板 `loadInServiceAssets` 无 orgId scope 在跨组织部署下的数据泄漏（R1.29 全局 IQueryTransformer 是否实际注入到 dashboard 查询路径） | 静态：R1.29 `ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer 注入（reuse P1-MA2-093 resolved）；dashboard `daoProvider.daoFor().findAllByQuery()` 是否经 IQueryTransformer 需运行时确认 | A4.1 运行时：多组织部署 + 用户归属 orgA 但 orgB 有资产，断言 loadInServiceAssets 是否泄漏（复用 P1-MA2-093 运行时确认） |
| SP-5 | UC-AST-09 盘亏 SCRAPPED 资产的折旧计划 CANCELLED 是否同步触发（盘亏直接 setStatus(SCRAPPED) `:237`——是否同步 cancelPendingSchedules 致后续折旧不重复计提） | 静态：盘亏走 `ErpAstInventoryProcessor` 直接 SCRAPPED，未显式调 cancelPendingSchedules（区别于处置链 ErpAstDisposalProcessor:214）；需确认是否有 listener/trigger 同步取消 PENDING 折旧 | A4.1 运行时：盘亏一有 PENDING 折旧计划的资产 → 断言该资产后续 executeBatchDepreciation 是否跳过（或双计） |

---

## §8 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更（纯审计报告），checker 无回归风险**。

  | 规则 | 描述 | actual | baseline | 状态 |
  |------|------|--------|----------|------|
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | ✓ ≤ |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 240 | ✓ ≤（下降） |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1380 | ⚠ +2（pre-existing baseline 漂移，本审计无代码变更不引入） |
  | R3 | new Erp*() 构造实体 | 5 | 5 | ✓ ≤ |
  | R5 | @Inject private | 0 | 0 | ✓ |
  | R6 | @Transactional in BizModel | 2 | 2 | ✓ |
  | R8 | Processor 无 xbiz 接线 | 0 | 0 | ✓ |
  | R10 | REQUIRES_NEW 事务 | 6 | 6 | ✓ |
  | R11 | Processor 重复状态判断方法 | 0 | 0 | ✓ |

  > R2c actual=1382 vs baseline=1380 为 **pre-existing** 基线漂移（本审计为只读审计，零生产代码变更），非本审计引入。CI workflow 门控会捕获此漂移；本报告不处理基线对齐（属 compliance-baseline 维护范畴，非审计范围）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index assets 同域同控制点后给出"复用 or 新增"裁决（§6.1 裁决表），无未经比对直接新建的 finding。P2-RC-028（盘点偏离）经 grep 确认为 assets 域新控制点；P1-MA2-061（IDLE）/ P1-MA2-093（orgId）/ P2-MA1-023（DISPOSED drift）经 grep 确认同根因同控制点 → 复用并列明差异依据。

---

## §9 与 MA2 报告差异增量声明

> §去重协议：本审计不复跑 MA1-MA7，复用既有 MA2 报告已证实行为，只补"需求契约↔实际行为"差异（需求视角）。

### 复用的 MA2/MA4/MA5 既有证据

| 报告 | 复用内容 | 本切片差异增量 |
|------|---------|----------------|
| `2026-07-28-0400-arm-ma2-assets-state-machine.md`（A2.10 状态机） | 资产卡片生命周期迁移 + 7 业务单据 6 PROC+1 INLINE 模式 + reverseApprove 红冲闭环 + 跨域 I*Biz Facade 全合规；P1-MA2-061 IDLE resolved R1.18 doc-only Deferred | **UC-AST-03 需求视角差异**：A2.10 从状态机行为视角判 P1-MA2-061（IDLE 死状态），resolved R1.18 方案 A（owner doc Deferred）。本切片从需求契约视角复核 §4 三判据——R1.18 resolution 在"人工批准"意义上不满足（(i) AI 子代理审计 ≠ 人工批准 + (ii) 无人工批准痕迹 + (iii) product-scope 未裁剪）→ 倾向重开 P1，须人工确认 product-scope |
| `2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`（A4.3 代码质量） | 48 Processor 全域最高密度代码质量 PASS | **无差异增量**（A4.3 代码质量视角与本切片需求契约视角正交，互补不重复） |
| `2026-07-29-1430-arm-ma5-assets-test-coverage.md`（A5.4 测试覆盖） | 14 测试零浅测 + 折旧引擎异常路径空洞 + 48 Processor 对称性薄 | **UC-AST-03 需求视角差异**：A5.4 从测试覆盖视角记 UC-AST-03 零测试（归 P1-MA5-011 异常路径空洞）。本切片从需求契约视角确认 UC-AST-03 断言①②迁移完全缺失（功能缺失非仅测试缺失） |

### 本切片只补的需求视角差异（6 项）

1. **UC-AST-03 闲置 Deferred §4 三判据复核**（#1）：A2.10 resolved R1.18 doc-only Deferred（状态机行为视角），本切片复核 §4 三判据在"人工批准"意义上不满足 → 倾向重开 P1（需求契约视角 Q4=(a)）
2. **UC-AST-09 盘点实现偏离**（#3）：A2.10/A4.3 证实盘点 Processor 行为正确（状态机/代码质量视角），本切片从 L1 字面「盘亏→触发处置流程(UC-AST-04 报废)」视角识别完整处置链复用收窄（净效果实现但凭证链未走）→ P2-RC-028
3. **UC-AST-10 维修资本化重算正确性**（#5）：A2.10/A4.3 证实维修 Processor 行为正确，本切片从 L1「折旧计划调整」语义视角确认 `recalculateForCapitalizationMaintenance` 加性扩展语义等价 → 接受
4. **UC-AST-11 拆分平衡 + 不可逆性**（#2 + DISPOSED）：A2.10 证实 DISPOSED 可达 + 不可逆契约合规（状态机视角），本切片从 L1:185-205 需求视角确认不可逆性是 owner doc 要求非缺口 + Σ 平衡满足 L1 → 接受（DISPOSED vs SCRAPPED reuse P2-MA1-023）
5. **UC-AST-12 看板行级权限 caveat**（#4）：A2.10 证实 dashboard 聚合行为正确，本切片从 L1「行级权限约束」视角识别 `loadInServiceAssets` 无 orgId scope → reuse P1-MA2-093
6. **UC-AST-01 资本化折旧计划生成**（#6）：A4.3 证实折旧算术正确，本切片从 L1「按折旧方法/年限/残值」视角确认直线法 + 末期残值修正满足 → 接受

---

## Verdict

**pass（零 P0、1 reuse P1[P1-MA2-061 §4 复核倾向重开须人工确认]、1 new P2[P2-RC-028]、3 reuse watch-only[P1-MA2-093 resolved / P2-MA1-023 / 接受]、4 UC 接受[UC-AST-01/06/10 全接受 + UC-AST-11/12 接受含 caveat]）**。resolved finding HEAD 复核：P1-MA2-061（resolved R1.18 doc-only Deferred，本切片 §4 复核倾向重开须人工确认）/ P1-MA2-093（resolved R1.29 全局 IQueryTransformer，assets dashboard 视角维持 resolved watch-only）/ P2-MA1-023（watch-only owner doc drift 维持）。本切片解除 A1.24 在 MA4（A4.1 扩展域展开器）及 MR1（R1.0）链路的该切片证据缺口。
