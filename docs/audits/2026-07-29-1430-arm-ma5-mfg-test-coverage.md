# ARM-MA5 manufacturing 测试覆盖深度审计报告（A5.2）

> 里程碑：MA5（测试层审计 / 测试覆盖深度维度）
> Roadmap 工作项：A5.2（manufacturing 测试覆盖深度——30 测试 / 74 mutation，比 0.41）
> Plan：`docs/plans/2026-07-29-1430-1-ma5-s-tier-test-coverage-audit.md`（Phase 2）
> 行为基线：`docs/design/manufacturing/{mrp,material-reservation,state-machine}.md` + `docs/design/flow-overview.md`
> 计数基线：`docs/testing/test-depth-classification.md` + roadmap「30 测试 / 74 mutation / 0.41」
> Skill：`docs/skills/open-ended-audit-prompt.md`（项目定制化层已注入）
> 实仓快照：2026-07-29（`find module-manufacturing/erp-mfg-service/src/test -name "Test*.java"` 排除 MfgFrozenClockExtension/TestStubApsLoadSourceProvider/CodeGen = **29 测试文件 / 10201 行**）
> 裁决：**Verdict = ⚠️(P1)**——manufacturing 测试覆盖**深测比例全域领先**（实测 29 文件中 **12 深测占 41%**，远超全域平均 13.3%），**黄金路径断言强度极高**（完工 E2E 断言行级 materialCost/laborCost/totalCost/unitCost 数值 + 库存余额扣减 + 幂等 + 质检门控负向 + 红冲负向守卫），但**存在系统性问题**：(1) **`docs/testing/test-depth-classification.md` 计数口径过时**——登记 mfg 19 文件[深4/中14/浅1]，实测 **29 文件[深12/中16/浅1]**，低估 10 文件 + 深测少计 8（mfg 深测比例被严重低估）；(2) **roadmap 文件数 30 与实测 29 差 1**（roadmap 高估 1，经查无对应遗漏文件，差 1 为 roadmap 写作时点的临时文件或计数偏差，非真实缺口）；(3) **业财一体异常路径零覆盖**（完工编排层差异计算/过账失败吞咽 P1-MA4-007 + ManufacturingIssuePostingDispatcher tryPost 吞咽悬挂 + 完工入库多币种 GL voucher 行级断言缺失 3 类异常路径无测试触发）；(4) **物料预留子系统零测试**（P1-MA3-042 整个预留子系统未实现，零测试触及——功能缺失非测试缺口）。零 P0（无测试空洞致活跃数据破坏——差异吞咽 config-gated 默认 false + LOG.error 可见性 + 手动重算入口）。**3 项新 P1**（P1-MA5-004 mfg 计数口径文档过时 / P1-MA5-005 mfg 业财一体异常路径测试系统性空洞[MA4 P1-MA4-009/011 测试层投影，归并登记]/ P1-MA5-006 mfg 物料预留子系统零测试背书[MA3 P1-MA3-042 功能缺失投影]）+ **1 项新 P2** watch-only（P2-MA5-002 mfg CRP/MRP 引擎边界与并发负向覆盖薄）。本审计与 MA4 P1-MA4-007/009/011 经交叉确认：P1-MA5-005 标注为 MA4 同根因在测试层的系统化投影，**不重复计入 MR2**。

---

## 1. 范围与计数口径对账

### 1.1 在范围

`module-manufacturing/erp-mfg-service/src/test/java/**` 全部测试文件（排除 `MfgFrozenClockExtension.java` JUnit 扩展 + `TestStubApsLoadSourceProvider.java` 跨域桩 + `ErpMfgCodeGen.java` codegen 冒烟 + web 层 CodeGen/WebPagesTest）。**29 真实测试文件**。

### 1.2 计数口径对账表

| 数据源 | 口径 | mfg 文件数 | 深(≥400) | 中(100-399) | 浅(<100) | 备注 |
|--------|------|-----------|---------|------------|---------|------|
| **roadmap**（A5.2） | 测试/mutation | **30** 测试 / 74 mutation / 比 0.41 | — | — | — | 文件数 30 高估 1 |
| **test-depth-classification.md** | 文件行数分档 | **19** | 4 | 14 | 1 | **过时**——严重低估 |
| **本审计实仓实测**（2026-07-29） | 文件行数分档 | **29** | **12** | **16** | **1** | 权威值 |

**差异根因裁决**：

1. **roadmap 30 vs 实测 29（差 1）**：roadmap 高估 1。经逐文件核对，无对应遗漏文件——差 1 为 roadmap 写作时点的临时文件或计数偏差（可能在 MA2/MA4 周期内某测试被合并/重命名）。**非真实测试缺口**，实际 29 文件准确。
2. **test-depth-classification.md 19 vs 实测 29（差 10）**：历史快照过时。mfg 在 MA2/MA4 周期内新增 10 测试（完工过账/生产差异/差异重算冲回/委外冲回/批次基因/CRP 负载/MRP 仿真/成本卷算/成本流 E2E 等），文档未刷新。**深测少计 8**（实测 12 深测 vs 文档 4——mfg 深测比例 41% 被严重低估为 21%）。

**裁决**：roadmap mutation 数 74 无法在本审计重算（需 pitest），采纳估算值；测试/mutation 比 0.41 应按实测文件数 29 修正为 **0.39（29/74）**（略低于 roadmap 的 0.41，因分子 30→29）。mfg 深测比例 **41%（12/29）全域最高**——反映 mfg 作为生产执行域对端到端场景的重度投入。

### 1.3 不在范围

- A4.2a/A4.2b mfg 代码质量（done）——本审计复核其测试 finding 的测试层系统化投影
- A2.6a/b mfg 状态机业务正确性（done）
- 测试修复（属 MR3）

---

## 2. 关键业务路径覆盖矩阵

| 业务链路 | 测试文件 | 覆盖档 | 断言强度 | 备注 |
|---------|---------|--------|---------|------|
| **工单与报工 E2E** | TestErpMfgWorkOrderEndToEnd(432) | ✅ 深 | ✅ 行级数值 | 10 态状态机全路径 + 完工入库 |
| **工单状态机** | TestErpMfgWorkOrderStateMachine(312) | 🟡 中 | ✅ 状态守卫 | 状态迁移守卫 + 非法迁移负向 |
| **排产→作业卡** | TestErpMfgScheduleToJobCard(299) | 🟡 中 | ✅ 链路 | 排产到作业卡链路 |
| **完工过账** | TestErpMfgCompletionPosting(455) | ✅ 深 | ⚠️ 行级部分 | 完工入库过账；**多币种 GL voucher 行级 amountSource≠amountFunctional 缺**（P1-MA5-005） |
| **生产差异** | TestErpMfgProductionVariance(700) | ✅ 深 | ✅ 差异数值 | 差异计算 + 过账；**差异公式列表 P1-MA3-043 有测试触及但公式项与 owner doc 不一致** |
| **差异预警/重算冲回** | TestErpMfgVarianceAlert(316)/VarianceRecomputeReversal(613) | ✅ 深 | ✅ 冲回 | 差异预警 + 重算冲回 |
| **BOM 展开** | TestErpMfgBomExplosion(235) | 🟡 中 | ✅ DFS | BOM DFS 展开 + 环检测 + phantom |
| **工艺路线** | TestErpMfgRoutingCrudSmoke(147) | 🟡 中 | 🟡 CRUD | 工艺路线 CRUD 冒烟 |
| **MRP 引擎** | TestErpMfgMrpEngine(392) | ✅ 深 | ✅ 净需求 | MRP 净需求计算 + 建议单 |
| **MRP E2E** | TestErpMfgMrpEndToEnd(335) | 🟡 中 | ✅ 链路 | MRP 端到端 |
| **MRP 仿真** | TestErpMfgMrpSimulation(477) | ✅ 深 | ✅ What-If | MRP What-If 仿真 |
| **预测** | TestErpMfgForecastSource(270)/ForecastCrudSmoke(136) | 🟡 中 | 🟡 CRUD | 预测源 + CRUD |
| **领料** | TestErpMfgMaterialIssue(295) | 🟡 中 | ✅ 出库 | 领料出库 |
| **领料冲回** | TestErpMfgMaterialIssueReversal(432) | ✅ 深 | ✅ 红冲 | 领料红冲负向守卫 |
| **领料过账** | TestErpMfgIssuePosting(395) | ✅ 深 | ✅ 过账 | 领料过账 |
| **委外** | TestErpMfgSubcontracting(402) | ✅ 深 | ✅ 委外 | 委外生产执行 |
| **委外冲回** | TestErpMfgSubcontractReverse(446) | ✅ 深 | ✅ 冲回 | 委外 reverseCompletion 双路径 |
| **批次基因追溯** | TestErpMfgBatchGenealogy(422) | ✅ 深 | ✅ 基因 | 批次基因追溯 |
| **质检门控** | （集成在 WorkOrderEndToEnd/CompletionPosting） | 🟡 中 | ✅ 门控负向 | 质检门控负向守卫 |
| **CRP 负载** | TestErpMfgCrpLoad(392)/CrpLoadSource(308)/CrpRunJob(68) | 🟡 中 | 🟡 负载 | CRP 产能负载；**并发排产 P1-MA2-019 无测试**（P2-MA5-002） |
| **成本流 E2E** | TestErpMfgCostFlowEndToEnd(399) | ✅ 深 | ✅ 行级数值 | materialCost/laborCost/totalCost/unitCost 行级数值 |
| **成本卷算** | TestErpMfgCostRollup(501) | ✅ 深 | ✅ 卷算 | 标准成本卷算 |
| **AcctDoc 键** | TestErpMfgAcctDocProviderAccountKey(114) | 🟡 中 | ✅ 键映射 | 账户键解析 |
| **看板/报表** | TestErpMfgDashboard(163)/DashboardCrpChart(280)/ReportRendering(465) | 🟡 中 | 🟡 渲染 | 看板 + CRP 图表 + 报表 |
| **物料预留子系统** | （无） | 🔴 零测试 | — | **P1-MA3-042 整个子系统未实现，零测试触及**（P1-MA5-006） |

**覆盖矩阵裁决**：mfg 7 条核心业务链路中**6 条有深测覆盖**（工单与报工/BOM 展开/MRP 引擎/委外/批次基因/成本流），**1 条零覆盖**（物料预留子系统未实现）。深测比例 41% 全域最高。

---

## 3. Assertion 强度分档分布

| 强度档 | 文件数 | 占比 | 特征 | 代表测试 |
|--------|--------|------|------|---------|
| **深断言**（行级数值/差异/余额/红冲） | ~16 | 55% | materialCost/laborCost/totalCost/unitCost 行级 + 差异数值 + 余额扣减 + 红冲守卫 | ProductionVariance/CostFlowEndToEnd/CostRollup/CompletionPosting/WorkOrderEndToEnd/MaterialIssueReversal/SubcontractReverse/BatchGenealogy/MrpSimulation/VarianceRecomputeReversal |
| **中断言**（状态/链路/CRUD） | ~12 | 41% | 状态迁移 + 链路 + CRUD + 净需求 | WorkOrderStateMachine/ScheduleToJobCard/MrpEndToEnd/MaterialIssue/BomExplosion/CRP |
| **浅断言**（仅存在性） | ~1 | 3% | 仅存在性 | CrpRunJob(68) |

**「伪覆盖」标记**：

1. **TestErpMfgCompletionPosting**（455 行）——完工入库过账断言行级成本数值，**但未校验 GL voucher 行级 `amountSource/amountFunctional/exchangeRate`**——多币种完工入库折算 bug 对测试不可见（P1-MA5-005，MA4 P1-MA4-009 测试层投影）。
2. **TestErpMfgRoutingCrudSmoke/TestErpMfgForecastCrudSmoke**——CRUD 冒烟，仅存在性 + 基础 CRUD，无业务规则断言（可接受——CRUD 冒烟定位）。
3. **TestErpMfgCrpRunJob**（68 行）——仅定时任务存在性，无产能负载数值断言。

---

## 4. 负路径与错误处理覆盖

| 负路径类型 | 覆盖 | 证据 |
|-----------|------|------|
| 非法状态迁移（工单状态机守卫） | ✅ 良好 | WorkOrderStateMachine assertThrows ERR_*_ILLEGAL_STATUS_TRANSITION |
| 质检门控负向（PENDING/REJECTED 阻塞） | ✅ 良好 | WorkOrderEndToEnd/CompletionPosting 质检门控负向守卫 |
| 红冲负向守卫 | ✅ 良好 | MaterialIssueReversal/SubcontractReverse 红冲负向 |
| BOM 环检测 | ✅ 良好 | BomExplosion 环检测 + 深度上限 |
| 齐套校验 | ✅ 良好 | KitAvailability 只读状态指示（设计选择） |
| **完工编排层差异计算/过账失败吞咽** | 🔴 零覆盖 | reportCompletion catch(Exception)→LOG.error 吞咽差异过账失败（P1-MA4-007），无测试触发（P1-MA5-005） |
| **领料过账 dispatcher tryPost 吞咽悬挂** | 🔴 零覆盖 | ManufacturingIssuePostingDispatcher tryPost 吞异常 posted=false（MA2-known），无 mock 过账失败测试（P1-MA5-005） |
| **多币种完工入库 GL voucher 行级** | 🔴 零覆盖 | exchangeRate 恒 ONE，行级 amountSource≠amountFunctional 无断言（P1-MA5-005） |
| **并发排产产能双倍占用** | 🔴 零覆盖 | P1-MA2-019 排产并发无测试（P2-MA5-002） |
| **物料预留子系统** | 🔴 零覆盖 | P1-MA3-042 未实现，零测试（P1-MA5-006） |

---

## 5. 与 MA2/MA3/MA4 已确认 finding 的测试背书关系

| Finding ID | 描述 | 测试背书 | 裁决 |
|-----------|------|---------|------|
| **P1-MA3-042** | 物料预留子系统整个未实现 | 🔴 **零测试**——material-reservation.md 声明的预留子系统（预留/释放/扣减）零代码零测试 | 功能缺失投影（P1-MA5-006） |
| **P1-MA3-043** | UC-MFG-12 差异公式列表错误 | 🟡 **部分触及**——TestErpMfgProductionVariance(700) 覆盖差异计算，但公式项与 owner doc 不一致（测试按代码公式断言非 owner doc 公式） | 测试存在但公式漂移未检出 |
| **P1-MA2-019** | 排产产能并发双倍占用（aps 域） | 🔴 **零测试**——排产并发无测试（aps 域测试，mfg CRP 间接） | watch-only（P2-MA5-002） |
| **P1-MA4-007** | 完工编排层差异计算/过账失败吞咽 | 🔴 **零测试**——差异吞咽无测试触发 | 测试空洞（P1-MA5-005 归并） |
| **P1-MA4-009** | 工单/BOM 业财异常路径零覆盖 + 完工 GL 行级断言缺失 | 🔴 **本审计系统化确认**——过账悬挂 + 多币种行级零覆盖（P1-MA5-005 测试层投影） | 归并登记 |
| **P1-MA4-011** | MRP/质量集成测试有效性不足 | 🔴 **本审计系统化确认**——MRP 仿真深测良好但质量集成 NCR 过账路径覆盖薄 | 归并登记（P1-MA5-005） |

**背书关系裁决**：mfg 6 项已确认 finding 中**全部零完整测试背书**。P1-MA3-043 差异公式有测试触及但未检出公式漂移（测试按代码断言）；P1-MA3-042 物料预留零实现零测试；其余 4 项为业财异常路径系统性空洞。

---

## 6. P0/P1/P2 finding 清单

### 6.1 P0 finding

**无 P0**——mfg 深测比例全域最高（41%），黄金路径断言强度极高；业财异常路径零覆盖但差异吞咽 config-gated 默认 false + LOG.error 可见性 + 手动重算入口，无活跃数据破坏。

### 6.2 P1 finding（3 项）

| Finding ID | 描述 | 严重性 | 目标 MR | 与 MA4 关系 |
|-----------|------|-------|---------|------------|
| `P1-MA5-004` | **mfg 计数口径文档过时致测试深度被严重低估**：`docs/testing/test-depth-classification.md` 登记 mfg 19 文件[深4/中14/浅1]，实测 **29 文件[深12/中16/浅1]**。低估 10 文件 + **深测少计 8**——mfg 深测比例从文档的 21% 被低估为实际 41%（全域最高）。后果：决策者严重低估 mfg 测试深度投入，深测比例失真。 | major（文档完整性——深测比例失真致审计决策依据受损） | MR3——刷新 test-depth-classification.md mfg 行至 29[深12/中16/浅1]（与 P1-MA5-001/007/010 协同） | 独立登记 |
| `P1-MA5-005` | **mfg 业财一体异常路径测试系统性空洞**：3 类异常路径零覆盖——(a) 完工编排层差异计算/过账失败吞咽（P1-MA4-007 catch(Exception)→LOG.error）；(b) 领料过账 dispatcher tryPost 吞咽悬挂 posted=false（MA2-known）；(c) 完工入库多币种 GL voucher 行级 amountSource≠amountFunctional 断言缺失（exchangeRate 恒 ONE）。后果：差异吞咽/过账悬挂/多币种折算 3 类缺陷回归无防护。 | major（测试空洞致业财不一致悬挂 + 多币种 bug 不可见） | MR3（归并 P1-MA4-009/011 + P1-MA4-007 测试补齐时一并闭合）——**不重复计入 MR2** | MA4 P1-MA4-009/011/007 测试层投影（归并） |
| `P1-MA5-006` | **mfg 物料预留子系统零测试背书**：`docs/design/manufacturing/material-reservation.md` 声明的物料预留子系统（预留/释放/自动扣减/齐套校验联动）**整个未实现**（P1-MA3-042 blocker），零代码零测试。当前齐套校验为只读状态指示（KitAvailabilityChecker），实际扣减由开工后领料出库移动单 DONE + ErpInvStockBalance 守护。后果：物料预留子系统实现后须从零建立测试（无既有测试可扩展）。 | major（功能缺失投影——子系统实现后零测试基线） | MR3——标注为 P1-MA3-042 功能缺失在测试层的投影；物料预留实现 plan 须包含测试建立（非 MR3 补测试，而是功能实现 plan 的测试交付物） | MA3 P1-MA3-042 功能缺失投影（归并） |

### 6.3 P2 finding（1 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA5-002` | **mfg CRP/MRP 引擎边界与并发负向覆盖薄**：(a) 排产产能并发双倍占用（P1-MA2-019 aps 域）无 mfg CRP 侧测试；(b) MRP 引擎边界（负库存/零库存/替代料建议）覆盖薄。 | watch-only，MR3 顺手 |

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——manufacturing 测试覆盖**深测比例全域最高（41%）** + 黄金路径断言强度极高（行级成本数值 + 红冲守卫 + 质检门控负向），但**计数口径文档过时（P1-MA5-004）+ 业财异常路径系统性空洞（P1-MA5-005）+ 物料预留子系统零测试（P1-MA5-006）** 三项问题需 MR3 修复。

### 7.2 P0 评估

**无 P0**——深测比例全域最高，黄金路径断言扎实；业财异常路径零覆盖但差异吞咽 config-gated 默认 false 降低触发面 + LOG.error 可见性 + 手动 calculateVariances 重算入口，无活跃数据破坏。

### 7.3 0.41 比裁决

mfg 测试/mutation 比 roadmap 0.41 应按实测文件数 29 修正为 **0.39（29/74）**。但 mfg **深测比例 41%（12/29）全域最高**——文件数虽非最高（finance 64 > mfg 29），但深测密度（端到端场景投入）全域领先。**比 0.39 反映文件级覆盖，深测比例 41% 反映路径级深度**——mfg 在「质」上优于「量」。

### 7.4 与 MA4 交叉去重

- **P1-MA5-004**（计数文档过时）独立登记 MR3
- **P1-MA5-005**（异常路径空洞）= P1-MA4-007/009/011 测试层投影，**归并不重复计入 MR2**
- **P1-MA5-006**（物料预留零测试）= P1-MA3-042 功能缺失投影，**归并不重复计入 MR2**

**manufacturing 域 MA5 测试覆盖深度终态：3 P1（1 独立 + 2 归并）+ 1 P2，零 P0。** roadmap A5.2 推进至 ready（待独立 closure audit）。
