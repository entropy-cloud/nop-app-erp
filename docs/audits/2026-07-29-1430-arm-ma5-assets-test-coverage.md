# ARM-MA5 assets 测试覆盖深度审计报告（A5.4）

> 里程碑：MA5（测试层审计 / 测试覆盖深度维度）
> Roadmap 工作项：A5.4（assets 测试覆盖深度——14 测试 / 61 mutation，比 0.23）
> Plan：`docs/plans/2026-07-29-1430-1-ma5-s-tier-test-coverage-audit.md`（Phase 4）
> 行为基线：`docs/design/assets/{state-machine,depreciation-and-posting}.md`
> 计数基线：`docs/testing/test-depth-classification.md` + roadmap「14 测试 / 61 mutation / 0.23」
> Skill：`docs/skills/open-ended-audit-prompt.md`（项目定制化层已注入）
> 实仓快照：2026-07-29（`find module-assets/erp-ast-service/src/test -name "Test*.java"` 排除 AstTestSupport/CodeGen = **14 测试文件 / 4390 行**）
> 裁决：**Verdict = ⚠️(P1)**——assets 测试覆盖**文件数与 roadmap 一致（14）**且**零浅测**（全部 ≥100 行，3 深 + 11 中），主路径断言强度扎实（折旧三方法数值 + 残值约束 + 凭证 + 红冲闭环 + 7 业务单据状态机），但**存在系统性问题**：(1) **`docs/testing/test-depth-classification.md` 深度分类轻微错误**——登记 14[深3/中10/浅1]，实测 **14[深3/中11/浅0]**（文件数准确，但 1 文件被误分为浅实际为中——无 <100 行文件）；(2) **折旧引擎 48 Processor 测试覆盖结构性偏斜**——折旧算术三方法（直线法/DDB/SYD）主路径有数值断言，但**残值边界仅 residual=0**（非零残值截断分支 + 已达残值返 0 分支零覆盖）；(3) **业财异常路径零覆盖**（折旧 posted=false 业财悬挂 P1-MA4-013 + 并发首次折旧重复 P1-MA2-089 + 批量部分失败隔离 + Cap/Disposal posted=false 窗口 reverseApprove 不对称 4 类异常路径无测试触发）；(4) **48 Processor 审批轴对称性测试覆盖薄**（9 对 dispatcher tryPost 吞咽一致性仅 Cap/Disposal 有部分覆盖）。零 P0（折旧算术经残值约束兜底 + 累计折旧/净值数值断言覆盖；并发双计 P1-MA2-089 已登记 deferred；posted=false 业财悬挂有重跑自愈 + LOG.warn）。**2 项新 P1**（P1-MA5-010 assets 计数口径文档深度分类错误[浅1→浅0] / P1-MA5-011 assets 折旧引擎与业财异常路径测试系统性空洞[MA4 P1-MA4-014 + MA2 P1-MA2-060/089 测试层投影，归并登记]）+ **1 项新 P2** watch-only（P2-MA5-004 assets 48 Processor 审批轴对称性测试覆盖薄）。本审计与 MA4 P1-MA4-013/014 经交叉确认：P1-MA5-011 标注为 MA4 同根因在测试层的系统化投影，**不重复计入 MR2**。

---

## 1. 范围与计数口径对账

### 1.1 在范围

`module-assets/erp-ast-service/src/test/java/**` 全部测试文件（排除 `AstTestSupport.java` 测试基类 + `ErpAstCodeGen.java` codegen 冒烟 + web 层 CodeGen/WebPagesTest）。**14 真实测试文件**。

### 1.2 计数口径对账表

| 数据源 | 口径 | assets 文件数 | 深(≥400) | 中(100-399) | 浅(<100) | 备注 |
|--------|------|-------------|---------|------------|---------|------|
| **roadmap**（A5.4） | 测试/mutation | **14** 测试 / 61 mutation / 比 0.23 | — | — | — | 文件数准确 |
| **test-depth-classification.md** | 文件行数分档 | **14** | 3 | 10 | **1** | **深度分类轻微错误**——无 <100 行文件 |
| **本审计实仓实测**（2026-07-29） | 文件行数分档 | **14** | **3** | **11** | **0** | 权威值 |

**差异根因裁决**：

1. **roadmap 14 = 实测 14 = test-depth-classification.md 14（三源一致）**——assets 是四 S 级域中**唯一文件数三源一致**的域。
2. **test-depth-classification.md 深度分类轻微错误**：登记 1 浅测，但实测**最小文件 TestErpAstAcctDocProviderAccountKey(169 行) + TestErpAstAssetCrudSmoke(170 行) 均 >100 属中测**——assets **零浅测**（无 <100 行文件）。文档误分 1 文件为浅（实际为中）。**非文件数过时，而是深度分类错误**（assets 是四域中文件数最准但分类唯一有误的域）。

**裁决**：assets 文件数 14 准确，测试/mutation 比 0.23（14/61）分子准确。深度分类轻微错误（浅1→浅0）需刷新（P1-MA5-010，轻微）。assets **零浅测**反映测试质量基准良好（无仅 CRUD 冒烟的测试）。

### 1.3 不在范围

- A4.3 assets 折旧引擎代码质量（done）——本审计复核其测试 finding 的测试层系统化投影
- A2.10 assets 状态机业务正确性（done）
- 测试修复（属 MR3）

---

## 2. 关键业务路径覆盖矩阵

| 业务链路 | 测试文件 | 覆盖档 | 断言强度 | 备注 |
|---------|---------|--------|---------|------|
| **折旧引擎**（3 方法：直线法/DDB/SYD） | TestErpAstDepreciation(280) | 🟡 中 | ✅ 数值（部分） | 三方法主路径数值断言；**残值边界仅 residual=0**（非零残值截断 + 已达残值返 0 零覆盖）（P1-MA5-011） |
| **折旧过账红冲** | TestErpAstPostingReverse(351) | 🟡 中 | ✅ 红冲闭环 | posted=true 红冲闭环；**posted=false 窗口 reverseApprove 不对称零覆盖**（P1-MA5-011） |
| **资产卡片 CRUD** | TestErpAstAssetCrudSmoke(170) | 🟡 中 | 🟡 CRUD | 资产卡片 CRUD 冒烟 |
| **处置** | TestErpAstDisposal(202)/DisposalWorkflowApproval(225) | 🟡 中 | ✅ 状态+过账 | 处置状态机 + 过账 + 审批 |
| **资本化** | TestErpAstCapitalization(267) | 🟡 中 | ✅ 过账 | 资本化过账 |
| **价值调整** | TestErpAstValueAdjustment(286) | 🟡 中 | ✅ 过账 | 价值调整过账 |
| **分割/合并** | TestErpAstSplitMerge(496) | ✅ 深 | ✅ 分割合并 | 分割 + 合并状态机 + 过账 |
| **CIP 转在建转固** | TestErpAstCipTransfer(433) | ✅ 深 | ✅ 转固 | CIP 转固链路 |
| **资产盘点** | TestErpAstInventory(359) | 🟡 中 | ✅ 盘点 | 资产盘点 |
| **维修资本化/费用化** | TestErpAstMaintenance(660) | ✅ 深 | ✅ 维修过账 | 维修资本化 + 费用化过账（全域最大 assets 测试） |
| **AcctDoc 键** | TestErpAstAcctDocProviderAccountKey(169) | 🟡 中 | ✅ 键映射 | 账户键解析 |
| **看板/报表** | TestErpAstDashboard(233)/ReportRendering(259) | 🟡 中 | 🟡 渲染 | 看板 + 报表 |
| **转移** | （集成在 PostingReverse/Disposal） | 🟡 中 | 🟡 转移 | 资产转移（无独立测试，集成在 posting reverse） |
| **重估** | TestErpAstValueAdjustment 覆盖 | 🟡 中 | 🟡 重估 | 重估与价值调整共享测试 |

**覆盖矩阵裁决**：assets 折旧引擎 48 Processor + 资产卡片 + 7 业务单据（处置/转移/分割/合并/重估/资本化/价值调整）+ CIP + 盘点**全部有测试覆盖**（转移/重估集成在其他测试中）。深测集中在分割合并/CIP/维修三大复杂链路。

---

## 3. Assertion 强度分档分布

| 强度档 | 文件数 | 占比 | 特征 | 代表测试 |
|--------|--------|------|------|---------|
| **深断言**（数值/凭证/状态机/过账） | ~5 | 36% | 折旧数值 + 凭证生成 + 红冲闭环 + 分割合并 + CIP 转固 + 维修过账 | SplitMerge/CipTransfer/Maintenance/PostingReverse/Depreciation(部分) |
| **中断言**（状态/过账/键映射） | ~9 | 64% | 状态迁移 + 过账存在性 + 键映射 + CRUD | Disposal/Capitalization/ValueAdjustment/Inventory/AssetCrudSmoke/AcctDocKey/Dashboard/ReportRendering |
| **浅断言** | 0 | 0% | — | 零浅测（全域唯一与 hr 并列零浅测） |

**「伪覆盖」标记**：

1. **TestErpAstDepreciation**（280 行，折旧核心）——三方法（直线法/DDB/SYD）主路径数值断言扎实，**但残值边界仅 residual=ZERO**——DepreciationCalculator 残值约束兜底（nbv−amount<residual 截断）+ 已达残值返 0 两个分支零覆盖（P1-MA4-014）。**非零残值折旧算术缺陷对测试不可见**（P1-MA5-011）。
2. **TestErpAstPostingReverse**（351 行）——posted=true 红冲闭环扎实，**但 posted=false 窗口零覆盖**——Cap/Disposal posted=false 时 reverseApprove 不回滚资产（P1-MA2-060），无 mock tryPost 返回 false→reverseApprove 测试（P1-MA5-011）。
3. **TestErpAstAssetCrudSmoke**——资产卡片 CRUD 冒烟，仅存在性（可接受——CRUD 冒烟定位）。

---

## 4. 负路径与错误处理覆盖

| 负路径类型 | 覆盖 | 证据 |
|-----------|------|------|
| 非法状态迁移（资产/单据状态机） | ✅ 良好 | Disposal/SplitMerge 状态机守卫 assertThrows |
| 红冲负向守卫 | ✅ 良好 | PostingReverse posted=true 红冲闭环 |
| **折旧 posted=false 业财悬挂** | 🔴 零覆盖 | Depreciation dispatcher tryPost 吞咽返回 null（P1-MA4-013），无 mock post 抛异常测试（P1-MA5-011） |
| **并发首次折旧重复** | 🔴 零覆盖 | P1-MA2-089 executeDepreciation 缺 PENDING 守卫致并发双计，无并发测试（P1-MA5-011） |
| **批量折旧部分失败隔离** | 🔴 零覆盖 | executeBatchDepreciation 单资产失败 try/catch 跳过，无测试（P1-MA5-011） |
| **Cap/Disposal posted=false 窗口 reverseApprove 不对称** | 🔴 零覆盖 | P1-MA2-060 reverseApprove 仅 posted=true 回滚资产，无 posted=false 测试（P1-MA5-011） |
| **非零残值折旧算术边界** | 🔴 零覆盖 | 残值约束兜底分支零覆盖（P1-MA5-011） |

---

## 5. 与 MA2/MA4 已确认 finding 的测试背书关系

| Finding ID | 描述 | 测试背书 | 裁决 |
|-----------|------|---------|------|
| **P1-MA4-013** | 折旧 dispatcher posted=false 业财悬挂无自动重试 | 🔴 **零测试**——折旧过账悬挂无 mock 测试（P1-MA5-011） | 测试空洞 |
| **P1-MA4-014** | 折旧/Processor 链路测试有效性不足 | 🔴 **本审计系统化确认**——异常路径零覆盖 + 残值边界仅=0（P1-MA5-011 测试层投影） | 归并登记 |
| **P1-MA2-060** | Cap/Disposal tryPost 吞咽 + reverseApprove 不对称 | 🔴 **零测试**——posted=false 窗口无测试（P1-MA5-011） | 测试空洞 |
| **P1-MA2-089** | 并发首次折旧重复双计 | 🔴 **零测试**——无并发测试（P1-MA5-011） | 测试空洞 |
| **A4.3**（48 Processor） | 折旧引擎 48 Processor 全域最高密度 | 🟡 **部分覆盖**——折旧三方法主路径有覆盖，但 48 Processor 审批轴对称性 + 9 对 dispatcher tryPost 一致性覆盖薄（P2-MA5-004） | 部分覆盖 |
| **A2.10**（状态机） | assets 18 状态字段状态机 | ✅ **良好**——Disposal/SplitMerge/Capitalization 状态机测试覆盖正向 + 部分负向 | 良好 |

**背书关系裁决**：assets 6 类已确认 finding 中**A2.10 状态机有良好测试背书**，**其余 5 类均零完整测试背书**。折旧 posted=false 悬挂/并发双计/reverseApprove 不对称/残值边界 4 类异常路径系统性零覆盖。

---

## 6. P0/P1/P2 finding 清单

### 6.1 P0 finding

**无 P0**——折旧算术经残值约束兜底 + 累计折旧/净值数值断言覆盖；并发双计 P1-MA2-089 已登记 deferred；posted=false 业财悬挂有重跑 executeDepreciation 自愈 + LOG.warn + 期末试算平衡兜底。测试空洞致缺陷不可见但无活跃数据破坏路径因测试缺失而恶化。

### 6.2 P1 finding（2 项）

| Finding ID | 描述 | 严重性 | 目标 MR | 与 MA4 关系 |
|-----------|------|-------|---------|------------|
| `P1-MA5-010` | **assets 计数口径文档深度分类轻微错误（浅1→浅0）**：`docs/testing/test-depth-classification.md` 登记 assets 14[深3/中10/浅1]，实测 **14[深3/中11/浅0]**。文件数 14 三源一致（准确），但**深度分类错误**——误分 1 文件为浅（实际最小文件 TestErpAstAcctDocProviderAccountKey=169 行属中测），assets **零浅测**（无 <100 行文件）。后果：assets 测试质量基准被低估（实际零浅测优于文档的 1 浅测），但影响轻微（文件数准确，仅分档误差）。**注**：assets 是四 S 级域中文件数最准但深度分类唯一有误的域。 | minor→major（文档完整性——分档错误但文件数准确，严重性低于 finance/mfg/hr 的文件数过时） | MR3——刷新 test-depth-classification.md assets 行至 14[深3/中11/浅0]（与 P1-MA5-001/004/007 协同） | 独立登记 |
| `P1-MA5-011` | **assets 折旧引擎与业财异常路径测试系统性空洞**：5 类异常路径零覆盖——(a) 折旧 posted=false 业财悬挂（P1-MA4-013 dispatcher tryPost 吞咽返回 null）；(b) 并发首次折旧重复双计（P1-MA2-089 缺 PENDING 守卫）；(c) 批量折旧部分失败隔离（executeBatchDepreciation 单失败 try/catch 跳过）；(d) Cap/Disposal posted=false 窗口 reverseApprove 不对称（P1-MA2-060）；(e) 非零残值折旧算术边界（残值约束兜底截断 + 已达残值返 0 两分支，残值仅 residual=0）。后果：折旧悬挂/并发双计/reverseApprove 不对称/残值边界 4 类缺陷回归无防护，折旧正确性直接影响财务报表（累计折旧/折旧费用）。 | major（测试空洞致折旧算术边界 + 业财不一致悬挂不可见——折旧直接影响财务报表） | MR3（归并 P1-MA4-014 + P1-MA4-013 + P1-MA2-060/089 测试补齐时一并闭合）——**不重复计入 MR2** | MA4 P1-MA4-014/013 + MA2 P1-MA2-060/089 测试层投影（归并） |

### 6.3 P2 finding（1 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA5-004` | **assets 48 Processor 审批轴对称性测试覆盖薄**：折旧引擎 48 Processor（全域最高密度，A4.3）的 9 对 dispatcher tryPost 吞咽一致性仅 Cap/Disposal 有部分覆盖（P1-MA2-060 原枚举），其余 7 对（Depreciation/ValueAdjustment/AssetInventory/MaintenanceCapitalization/MaintenanceExpense/AssetSplit/AssetMerge）tryPost 吞咽一致性无独立测试。48 Processor 审批轴对称性无系统化测试矩阵。 | watch-only，MR3 顺手——随 P1-MA5-011 异常路径测试补齐时建立 9 对 dispatcher 对称性测试矩阵 |

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——assets 测试覆盖**文件数三源一致（14）+ 零浅测**（全域唯一与 hr 并列）+ 主路径断言强度扎实（折旧数值 + 凭证 + 红冲 + 7 业务单据状态机），但**计数文档深度分类轻微错误（P1-MA5-010）+ 折旧引擎与业财异常路径系统性空洞（P1-MA5-011）** 两项问题需 MR3 修复。

### 7.2 P0 评估

**无 P0**——折旧算术经残值约束兜底 + 数值断言覆盖；并发双计 P1-MA2-089 已登记 deferred；posted=false 业财悬挂有重跑自愈 + LOG.warn + 期末试算兜底。测试空洞致缺陷不可见但无活跃数据破坏。

### 7.3 0.23 比裁决

assets 测试/mutation 比 **0.23（14/61）** 文件数分子准确。0.23 低于 mfg 0.39 / finance 0.47 但高于 hr 0.16——反映 assets mutation 数中等（61，四域第三）+ 测试文件数最少（14，四域最少，因 assets 业务单据虽多但 48 Processor 共享折旧算术逻辑，测试集中在 7 业务单据而非 48 Processor 逐一）。**比 0.23 主要反映异常路径覆盖缺口**（5 类异常路径零覆盖），非广度缺口（14 文件覆盖折旧+卡片+7 单据+CIP+盘点全链路）。

### 7.4 与 MA4 交叉去重

- **P1-MA5-010**（深度分类错误）独立登记 MR3
- **P1-MA5-011**（异常路径空洞）= P1-MA4-014/013 + P1-MA2-060/089 测试层投影，**归并不重复计入 MR2**

**assets 域 MA5 测试覆盖深度终态：2 P1（1 独立 + 1 归并）+ 1 P2，零 P0。** roadmap A5.4 推进至 ready（待独立 closure audit）。
