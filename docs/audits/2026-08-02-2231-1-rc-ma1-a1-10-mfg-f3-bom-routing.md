# rc-ma1-a1-10 mfg-F3 BOM 与工艺路线 需求-实现符合性五级追踪审计报告

> 报告类型：requirement-compliance MA1 切片 A1.10
> 切片：mfg-F3 BOM 与工艺路线（roadmap 标签；权威 UC 范围 = UC-MFG-02/10 共 2 UC）
> 审计时间：2026-08-02
> 审计基线 HEAD：`15bf103d2`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 上游计划：`docs/plans/2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（草案审查 `acceptable as-is`，独立子代理 `ses_03d1a2debffen81hLkNNsZ1fQ1`）
> 真相源层级（§4 Q1）：L1 = `docs/design/manufacturing/use-cases.md`（UC-MFG-02 `:43` / UC-MFG-10 `:176`）；L2 = `bom-and-routing.md`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.6b/A4.2a + 本切片差异。

---

## 9. 与既有 MA2/MA4 报告差异增量声明（前置声明，便于读者识别复用边界）

> 依方法论 §6 段落 9 + §去重协议，本报告前置声明与既有 MA2/MA4 报告的差异增量。

| 既有报告 | 覆盖维度 | 已证实结论（本切片复用） | 本切片补的差异增量（需求契约视角） |
|---------|---------|----------------------|--------------------------|
| `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（A2.6b） | MRP 计划头/预测/建议单隐式生命周期/BOM 激活/仿真状态机业务正确性 + 事务回滚一致性 | BOM 无独立状态机（`is_active`/`is_default` 治理，A2.6b 已声明）；`BomExpander` 经 `TestErpMfgBomExplosion` 覆盖；MRP 运算+释放事务原子性确认；**3 P1（P1-MA2-036/037/038）+ 2 P2** | BOM 状态机/激活治理 + MRP 运算/释放事务原子性**复用 A2.6b pass 结论**（不重审）；本切片只补**需求契约↔实现符合性**视角（UC-MFG-02 phantom 展开需求验收 + UC-MFG-10 快照原则 L1 字面要求 vs 推定未实现的符合性裁决） |
| `2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（A4.2a） | 工单/BOM 链路**代码质量**（编排健壮性/BOM 展开/算术/错误处理/失败恢复闭环/架构边界/测试有效性） | `BomExpander` DFS 环检测 + 深度上限 + path 回溯 + phantom 展开 + 算术正确性判定为**扎实 PASS**（§5 维度 2）；P1-MA4-008 含 BomExpander 跨域 daoFor 投影；**3 P1（P1-MA4-007/008/009）+ 1 P2** | 本切片不重审代码质量维度；只补**需求契约 vs 实现符合性**（UC-MFG-02 phantom 展开需求验收 + UC-MFG-10 快照原则符合性裁决 + resolved finding HEAD 复核） |
| `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（A3.4） | mfg owner doc vs code **drift**（文本一致性） | A3.4 范围内 mfg drift = material-reservation/差异公式/质检约束/超产/DowntimeEntry/差异预警，**未含 BOM 快照维度**（A3.4 未从需求契约视角裁决快照义务） | 本切片不复审 doc↔code 文本一致性；UC-MFG-10 快照缺失为**新功能点维度**（既有审计未从需求契约视角裁决快照义务），执行时 grep `arm-index.md` mfg BOM/快照同域同控制点后裁决（§6） |
| `2026-07-29-1430-arm-ma5-mfg-test-coverage.md`（A5.2） | mfg 测试覆盖深度 | `TestErpMfgBomExplosion` 覆盖 BOM 多级展开 + phantom（强断言）；UC-MFG-10 快照无测试（功能推定未实现） | 本切片不复审测试维度；L4 测试断言强度引用 A5.2 + A4.2a 已评级，不重复登记 |

**结论**：本切片裁决焦点 = **UC-MFG-02/10 需求契约↔实现符合性**。BOM 展开算法正确性/代码质量/状态机治理**复用 A2.6b/A4.2a pass 结论**（不重审，§去重协议）；本切片只补需求视角差异（UC-MFG-02 phantom 展开的需求验收 + UC-MFG-10 快照原则 L1 字面要求 vs 推定完全未实现的符合性裁决）。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 真相源：`docs/design/manufacturing/use-cases.md`（UC 锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.10 确认 = `:43`/`:176`，inventory `:344` 一致 ✅）。

### UC-MFG-02 多级 BOM 展开(phantom 虚拟件)（`use-cases.md:43-55`）

逐字引用验收标准：

```
BOM.is_phantom == true 的组件 →
  不生成该组件的生产订单                                        [断言①]
  其子件直接展开到当前工单的物料需求                              [断言②]
齐套校验基于展开后的全部子件(含虚拟件子件)                         [断言③]
```

涉及机制：`bom-and-routing.md §多级 BOM 展开`。

### UC-MFG-10 BOM 变更不影响已开工工单(快照原则)（`use-cases.md:176-187`）

逐字引用验收标准：

```
工单审核时快照 BOM(工单行记录当时 BOM 内容)                       [断言④]
BOM 后续修改 → 不影响已审核工单的物料需求/成本                      [断言⑤]
新建工单才用新 BOM                                                [断言⑥]
```

涉及机制：`state-machine.md §4`。

**断言计数**：UC-MFG-02 ×3（①②③）+ UC-MFG-10 ×3（④⑤⑥）= **6 条验收标准**（草案审查 iter1 实测一致，覆盖 2 UC 无跳号无合并）。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### 2.1 BOM 展开器（UC-MFG-02 核心 + UC-MFG-10 读侧）

| 组件 | 文件:行 | 作用 |
|------|---------|------|
| BOM 展开入口 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/bom/BomExpander.java:77-83`（`explode(bomId, requestedQty, useMultiLevel)` → `expandLines` DFS） | 多级展开主入口（IErpMfgBomBiz.explode 委托） |
| DFS 递归 + 环检测 | `BomExpander.java:85-119`（`expandLines`：`:88-92` `path.contains(product)` → `ERR_BOM_CYCLE`；`:93-97` `level > maxDepth` → `ERR_BOM_MAX_DEPTH_EXCEEDED`；`:99` `path.add`；`:116-118` `finally { path.remove(product); }` 路径回溯） | 环检测 + 深度上限 + 路径回溯（兄弟节点不互相阻塞） |
| phantom 展开（断言①②） | `BomExpander.java:101-108`（`scale = divide(qty, nz(bom.getQty()))`；`:104-108` `childBom != null && bomType==PHANTOM` → `expandLines(childBom, effQty, level, path, ...)` **并入当前层级不产生独立节点**） | 虚拟件子件并入父级，不产生独立节点 |
| 有效用量算术 | `BomExpander.java:103`（`effQty = nz(line.getQuantity()).multiply(scale)`）+ `:155-160` `divide`（`b.signum()==0` 守护返回 ZERO）+ `:162-164` `nz` null→ZERO 守护 | `line.quantity × requestedQty / BOM.qty` 逐层乘积 |
| 默认 BOM 解析 | `BomExpander.java:55-68`（`findDefaultBomOrNull(productId)`：filter `productId + isDefault=true + isActive=true`，`order by id desc limit 1`） | 取物料默认且有效的 BOM |
| config 常量 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgConstants.java:26-27`（`CONFIG_BOM_MAX_DEPTH = "erp-mfg.bom-max-depth"` + `DEFAULT_BOM_MAX_DEPTH = 15`）+ `:18` `BOM_TYPE_PHANTOM = "PHANTOM"` + `:17` `BOM_TYPE_MANUFACTURED = "NORMAL"` | 深度上限 config + phantom 类型码 |
| 错误码 | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgErrors.java:58-66`（`ERR_BOM_CYCLE` + `ERR_BOM_MAX_DEPTH_EXCEEDED`，附 `ARG_MATERIAL_ID/ARG_PATH/ARG_DEPTH` 上下文） | NopException + ErrorCode 规范化 |
| 齐套展开读侧（断言③） | `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/workorder/KitAvailabilityChecker.java:62-89`（`check` → `:64` `resolveBomId(wo)` → `:66` `bomExpander.explode(bomId, plannedQty, true)` 多级展开 → 逐子件对可用量） | 齐套基于展开后全部子件（含虚拟件子件） |

### 2.2 BOM 快照（UC-MFG-10 核心）—— grep 零命中证据

> 起草时 + 执行时对 HEAD `15bf103d2` 复核确认。

| grep 关键字 | 范围 | 命中 | 裁决 |
|------------|------|------|------|
| `snapshotBomVersion` | 全 `module-manufacturing/` | **0** | 快照版本字段未实现 |
| `bomSnapshotStrategy` | 全 `module-manufacturing/` | **0** | 快照策略字段未实现 |
| `LOCK_AT_CREATION` | 全 `module-manufacturing/` | **0** | L2 `:136` 声明的 LOCK_AT_CREATION 策略未实现 |
| `AUTO_UPGRADE` | 全 `module-manufacturing/` | **0** | L2 `:136` 声明的 AUTO_UPGRADE 策略未实现 |
| `bom-snapshot` / `BOM_SNAPSHOT` / `snapshot_bom` | 全 `module-manufacturing/` | **0** | 无快照 config key / 表 / 列 |
| `snapshot`（全 `module-manufacturing/`，含 ORM + Java） | 全 `module-manufacturing/` | 仅 `SimulationMrpEngine.java:155,447`（MRP 仿真场景版本 `snapshotSummary` 字段，**非工单 BOM 快照**）+ pom `1.0-SNAPSHOT`（Maven 版本，无关）+ ORM `app-erp-manufacturing.orm.xml:1580`（`snapshotSummary` 列在 `ErpMfgMrpScenarioVersion`，**非工单快照**） | 唯一 snapshot* 命中均为 MRP 仿真场景版本摘要，**与工单 BOM 快照无关**——"推定快照子系统未实现"主张事实正确 |

**`ErpMfgWorkOrder` / `ErpMfgWorkOrderLine` ORM 快照列核验**：
- `app-erp-manufacturing.orm.xml:577` `ErpMfgWorkOrder` 含 `bomId` 列（`code="BOM_ID" propId="4"`，**无 `mandatory`**——可空，仅捕获 BOM 引用）。
- `ErpMfgWorkOrder` / `ErpMfgWorkOrderLine` ORM 实测**无 `snapshotBomVersion` / `snapshotBomHead` / `snapshotBomLines` 等任何快照内容列**（rg `snapshot` 两实体范围内零命中）。
- 工单仅以 `bomId` **引用**当前 BOM（弱快照——捕获 WHICH bomId，非捕获 BOM 内容）；BOM 内容（头/子件行/工艺行）**未快照入工单字段**。

### 2.3 L3 关键事实（实测核验，区分"已实现"vs"未实现"）

- **已实现（UC-MFG-02）**：①phantom（`bomType=PHANTOM`）子件不生成独立节点（`BomExpander.java:104-108` `expandLines(childBom, effQty, level, ...)` 并入当前层级，`nodes.add` 跳过 phantom 本身）；②其子件直接展开到当前层级物料需求（同 `:108`）；③齐套校验基于展开后全部子件（`KitAvailabilityChecker.java:66` `explode(bomId, plannedQty, true)` 多级展开 + 逐子件对可用量，含虚拟件子件）；④环检测 `ERR_BOM_CYCLE`（`:88-92`）+ 深度上限 `ERR_BOM_MAX_DEPTH_EXCEEDED`（`:93-97`，config `erp-mfg.bom-max-depth` 默认 15）异常路径完整；⑤有效用量算术 `line.quantity × scale`（`:101-103`，scale=requestedQty/bom.qty，divide 守护 b.signum()==0）。
- **未实现（UC-MFG-10）**：④审核时快照 BOM 内容——**完全未实现**（grep 零命中 + ORM 无快照列 + 无快照 config key + 无快照写路径）；工单仅以 `bomId` 弱引用 BOM，BOM 内容（头/子件行/工艺行）未复制入工单快照字段。⑤BOM 后续修改不影响已审核工单物料需求/成本——**部分**：`KitAvailabilityChecker.resolveBomId:134-137` `if (wo.getBomId() != null) return wo.getBomId()`（bomId 引用锁定，新建 BOM 不影响已建工单的 bomId 解析）；**但**若同一 bomId 的 BOM 内容（子件行）被编辑，齐套重算/差异计算若再次 `explode(bomId, ...)` 会读取**编辑后**的内容（BomExpander.loadLines 实时查 ErpMfgBomLine）——内容级隔离缺失。⑥新建工单才用新 BOM——**已实现**（新工单创建时解析 bomId 或回落到当前默认 BOM，新默认 BOM 仅对新工单生效）。
- **L2 owner doc 与 L1 关系**：L2 `bom-and-routing.md §BOM 版本快照规则 :128-136` 描述快照机制（LOCK_AT_CREATION/AUTO_UPGRADE），但 `§实现注记（漂移补注）:147` 显式列「BOM 版本快照」为**本期 Non-Goal**。按 §4 Q1，L2 与 L1 冲突时**一律以 L1 为准**——L1 `:176` 字面要求"审核时快照"，L2 §实现注记 Non-Goal 推定已向实现妥协；§实现注记 Non-Goal 标注是否构成 §4 三判据"显式人工批准 documented simplification"须执行时核验批准来源（见 §5.2 三判据核验）。

---

## 3. 测试证据（L4 测试断言 + 断言强度）

| 测试 | 文件:方法 | 覆盖断言 | 断言强度（A5.2/A4.2a 已评级） |
|------|----------|---------|------|
| phantom 展开（断言①②） | `TestErpMfgBomExplosion.java#testPhantomExpandsIntoParentLevel:119-134` | ①② | **强**（P→PH(qty1, phantom)→M1(qty3)，断言 `nodes.size()==1` 仅其子件 + phantom 物料本身不出现 + M1 level=1 并入父级 + 有效用量 1×3=3） |
| 环检测（异常路径） | `TestErpMfgBomExplosion.java#testCycleDetection:136-147` | ④ ERR_BOM_CYCLE | **强**（P→SA→P 环，`assertThrows(NopException)` + `assertEquals(ERR_BOM_CYCLE.getErrorCode(), ex.getCode())`） |
| 深度上限（异常路径） | `TestErpMfgBomExplosion.java#testDepthLimitTruncation:149-172` | ④ ERR_BOM_MAX_DEPTH_EXCEEDED | **强**（制造链 P→CA→CB→CC 深度 4，设上限 2，`assertThrows` + `assertEquals(ERR_BOM_MAX_DEPTH_EXCEEDED.getErrorCode())` + finally 恢复默认） |
| 有效用量算术（单级） | `TestErpMfgBomExplosion.java#testSingleLevelExplosionQuantities:82-99` | ⑤ | **强**（req=1 → M1=2/M2=3；req=2 → M1=4/M2=6，逐量精确） |
| 有效用量算术（多级乘积） | `TestErpMfgBomExplosion.java#testMultiLevelExplosionQtyProduct:101-117` | ⑤ | **强**（P→SA(qty2)→M1(qty5)，M1 有效用量 2×5=10 逐层乘积 + SA level=1 manufactured=true） |
| 默认 BOM 选择 | `TestErpMfgBomExplosion.java#testFindDefaultBomPicksDefaultActive:62-73` + `#testFindDefaultBomNotFound:75-80` | 默认 BOM 解析 | **强**（isDefault=true AND isActive=true 过滤 + 非默认/停用排除 + 无默认返回错误） |
| GraphQL 装配 | `TestErpMfgBomExplosion.java#testExplodeViaGraphQLWiring:174-188` | BizModel→BomExpander 装配 | **中**（explode 经 GraphQL 调用成功 + 单级展开两子件，装配验证） |
| **齐套基于展开后全部子件（断言③）** | `TestErpMfgWorkOrderStateMachine` + `TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion`（A1.9 §3 引用） | ③ 齐套对展开结果 | **强**（A1.9 已评级：齐套校验决定 STOCK_RESERVED/STOCK_PARTIAL，基于 explode 多级展开结果） |
| **UC-MFG-10 快照隔离** | —（无） | ④⑤⑥ | **无测试**（功能推定未实现，与 owner doc §实现注记 Non-Goal 一致） |

**结论**：UC-MFG-02 phantom 展开 + 环检测/深度上限 + 算术**强覆盖**（5 强 + 1 中）；UC-MFG-10 快照隔离**零测试**（功能推定未实现）。

---

## 4. 运行时行为证据（L5，复用 A2.6b/A4.2a + 本切片差异）

### 4.1 复用 A2.6b 已证实行为（BOM 状态机治理）

- **BOM 无独立状态机（`is_active`/`is_default` 治理）**（A2.6b §2.4 + §3.1）：`ErpMfgBom.isActive` 布尔（默认 true）+ `isDefault` 布尔；BOM 生命周期简单（有效/无效两态），用布尔比 status 列轻量；`findDefaultBomOrNull` 查询 `isDefault=true AND isActive=true` 保证默认 BOM 有效性。owner doc `bom-and-routing.md` 未声明 BOM 状态机（一致）。
- **MRP 运算/释放事务原子性**（A2.6b §1.4 + §3.4）：与本切片 BOM 展开读侧无冲突（MRP 引擎本身不在本切片范围，UC-MFG-05/08 归 A1.8）。

### 4.2 复用 A4.2a 已证实代码质量（BomExpander 算法正确性）

- **BomExpander DFS 环检测 + 深度上限 + path 回溯 + phantom 展开 + 算术正确性 = 扎实 PASS**（A4.2a §2.2 维度 2）：递归终止 + 成环检测均正确；`finally { path.remove(product); }` 路径回溯正确（兄弟节点不互相阻塞）；phantom（bomType=PHANTOM）展开其子件并入当前层级不产生独立节点；有效用量 `line.quantity × scale`（scale=requestedQty/bom.qty，divide 守护 b.signum()==0）。
- **错误处理规范化**（A4.2a §2.4）：`ERR_BOM_CYCLE`/`ERR_BOM_MAX_DEPTH_EXCEEDED`/`ERR_BOM_NOT_FOUND` 全 NopException + ErrorCode（erp.err.mfg.*）+ 上下文齐全（materialId/path/depth/bomId）。

### 4.3 本切片补的差异（需求契约↔实现符合性运行时行为）

- **UC-MFG-02 phantom 展开运行时行为 = 完全符合 L1**：phantom（bomType=PHANTOM）子件并入当前层级不产生独立节点 + 其子件直接展开到当前工单物料需求 + 齐套校验基于展开后全部子件（含虚拟件子件，经 KitAvailabilityChecker.explode 多级展开）。运行时行为经 `TestErpMfgBomExplosion` 强断言覆盖。**接受**。
- **UC-MFG-10 快照原则运行时行为 = 内容级快照完全缺失**：
  - ④审核时快照 BOM 内容——**运行时不存在**：工单仅以 `wo.bomId` 弱引用 BOM（`orm.xml:577`，可空），无 `snapshotBomVersion` / 快照内容字段；审核（DRAFT→SUBMITTED→NOT_STARTED）时无任何 BOM 内容复制动作（grep 零命中）。
  - ⑤BOM 后续修改不影响已审核工单物料需求/成本——**部分**：bomId 引用锁定提供"新建 BOM 不影响已建工单"的弱隔离（resolveBomId 优先用 wo.bomId），但**同 bomId 内容编辑无隔离**——若运营编辑同一 BOM 的子件行（增/删/改物料或数量），后续齐套重算/差异计算若再次 `explode(bomId, ...)` 会读取编辑后内容（BomExpander.loadLines 实时查 ErpMfgBomLine，无版本/快照门控）。是否实际致成本结转凭证错误取决于完工成本是否经 BOM 重展开（A4.2a §2.2：完工 materialCost = Σ 领料单成本聚合，**不**经 BOM 重展开；但差异计算/重算路径若读 BOM 则受影响）→ 列静态存疑点 SP-1 交 MA4 运行时确认。
  - ⑥新建工单才用新 BOM——**已实现**：新工单创建时 resolveBomId 解析当前 bomId 或回落到当前默认 BOM，新默认 BOM 仅对新工单生效。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论 + resolved finding HEAD 复核）

### 5.1 五级追踪矩阵（每 UC 一行，方法论 §1 格式）

| UC | L1 需求契约（逐字） | L2 owner doc（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 |
|----|-------------------|-----------------------|-----------|-----------|-------------|
| **UC-MFG-02** 多级 BOM 展开(phantom 虚拟件)（3 断言：①phantom 不生成生产订单 ②其子件直接展开到当前工单物料需求 ③齐套基于展开后全部子件含虚拟件子件） | `use-cases.md:43-55`（§1 逐字引用） | `bom-and-routing.md §多级 BOM 展开 :62-72`（phantom 展开规则 + 多级展开 + 缓存）+ `§齐套校验 :74-82`。**L2 与 L1 一致**（§实现注记 Non-Goal 仅涉 BOM 快照/缓存表，不涉 phantom 展开本身） | `BomExpander.java:77-83` explode + `:85-119` expandLines DFS + `:104-108` phantom 展开（bomType=PHANTOM 子件并入当前层级不产生独立节点）+ `:88-92` 环检测 ERR_BOM_CYCLE + `:93-97` 深度上限 ERR_BOM_MAX_DEPTH_EXCEEDED + `:99,116-118` path 回溯 + `:101-103` 有效用量算术 + `ErpMfgConstants.java:26-27` CONFIG_BOM_MAX_DEPTH + `ErpMfgErrors.java:58-66`；齐套读侧 `KitAvailabilityChecker.java:62-89`（`:66` explode 多级展开 → 逐子件对可用量） | `TestErpMfgBomExplosion#testPhantomExpandsIntoParentLevel:119-134`（**强**：①② phantom 不产生独立项 + 子件并入父级 + 有效用量 1×3=3）+ `#testCycleDetection:136-147`（**强**：④环检测）+ `#testDepthLimitTruncation:149-172`（**强**：④深度上限）+ `#testSingleLevelExplosionQuantities:82-99` + `#testMultiLevelExplosionQtyProduct:101-117`（**强**：⑤算术）+ 齐套 `TestErpMfgWorkOrderStateMachine`/`TestErpMfgWorkOrderEndToEnd`（**强**：③齐套基于展开结果，A1.9 已评级） | 行为已证实（引用 A4.2a §2.2 维度 2 BomExpander DFS/环检测/phantom 展开/算术正确性 PASS + A2.6b §2.4 BOM 无独立状态机） |
| **UC-MFG-10** BOM 变更不影响已开工工单(快照原则)（3 断言：④审核时快照 BOM 工单行记录当时内容 ⑤BOM 修改不影响已审核工单物料需求/成本 ⑥新建工单才用新 BOM） | `use-cases.md:176-187`（§1 逐字引用） | `bom-and-routing.md §BOM 版本快照规则 :128-136`（快照时机 DRAFT→SUBMITTED + snapshotBomVersion 字段 + LOCK_AT_CREATION/AUTO_UPGRADE 策略）+ `§实现注记 :147`（**本期 Non-Goal 显式列"BOM 版本快照"**）。**L2 §实现注记与 L1 冲突裁决：以 L1 为准，L2 §实现注记 Non-Goal 推定已向实现妥协**（§4 Q1）；§实现注记 Non-Goal 标注为 AI 落地补注，**不构成 §4 三判据"显式人工批准 documented simplification"**（§5.2 三判据核验） | **断言④完全未实现**：grep `snapshotBomVersion`/`bomSnapshotStrategy`/`LOCK_AT_CREATION`/`AUTO_UPGRADE`/`bom-snapshot` 全 `module-manufacturing/` **零命中**；`ErpMfgWorkOrder` ORM（`orm.xml:577`）仅 `bomId`（可空，弱引用）**无 snapshotBomVersion 列**；`ErpMfgWorkOrderLine` 无快照内容列；审核路径（`ErpMfgWorkOrderProcessor` submitForApproval/approve + `ErpMfgWorkOrderReportCompletionProcessor`）无 BOM 内容复制动作。**断言⑤部分**：`KitAvailabilityChecker.resolveBomId:134-137`（wo.bomId 优先 → 锁定 bomId 引用，新建 BOM 不影响已建工单）+ BomExpander.loadLines 实时查 ErpMfgBomLine（**同 bomId 内容编辑无隔离**）。**断言⑥已实现**：新工单创建时 resolveBomId 解析当前 bomId 或回落当前默认 BOM | **断言④⑤⑥零测试**（功能推定未实现，与 owner doc §实现注记 Non-Goal 一致；无快照隔离 E2E 测试） | ④**运行时不存在**（无快照写路径）；⑤**部分**（bomId 引用弱隔离 + 内容级隔离缺失，是否致成本凭证错误取决于完工/差异路径是否 BOM 重展开 → SP-1 交 MA4）；⑥**已实现**（新工单解析新 bomId/默认 BOM） |

### 5.2 每 UC 符合性结论（§2 判据 + §4 三判据核验）

| UC | 结论 | 命中判据 | 三源对照 |
|----|------|---------|---------|
| **UC-MFG-02** | **接受** | §2 接受（验收标准全证据一致） | L1 3 断言 L3/L4/L5 全对齐（A4.2a §2.2 BomExpander 算法正确性 PASS + TestErpMfgBomExplosion 强断言） |
| **UC-MFG-10** | **P1**（新建 `P1-RC-009`） | §2 P1①（功能完全缺失——④审核时快照 BOM 内容完全未实现）+ §2 P1①（⑤行为实质偏离——同 bomId 内容编辑无隔离）+ §5 Q4（会计/成本正确性类无例外，禁方案 B） | L1 3 断言中 ④完全未实现（grep 零命中 + ORM 无快照列 + 无快照写路径）、⑤部分（bomId 引用弱隔离 + 内容级缺失）、⑥已实现。**§4 三判据核验**（Non-Goal 是否构成"显式人工批准 documented simplification"）：**(i) plan 含独立 plan-audit 通过记录**——`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md` 含独立草案审查（iter-1 needs revision / iter-2 accept）+ 独立结束审计，**但均为 AI 代理审查非人工批准**（§4：「代理独立审计通过 = 审计裁决质量证据，不算人工批准」）；**(ii) owner doc 显式 documented simplification 标注且经人工批准**——`bom-and-routing.md:147` §实现注记 Non-Goal 为 AI 落地补注（plan 2026-07-02-1538-2 AI 执行），**无 git log/commit message/讨论文档人工批准痕迹**；**(iii) product-scope 范围裁剪登记**——product-scope 未将 BOM 快照列入"不在范围"或"后续阶段"。**三判据均不成立** → Non-Goal 标注**不构成** documented simplification，按 §2 P1① 裁决。**且 §5 Q4 会计/成本正确性类无例外**：BOM 变更后已开工工单若按新 BOM 算物料需求/成本将破坏成本结转正确性（⑤内容级隔离缺失），Q4 无例外通道 → 必须实现，禁止方案 B 关闭 |

### 5.3 候选缺口/偏离逐条分级（计划 Phase 1 ①-⑧ 候选清单裁决）

| 候选 | 描述 | 裁决 | 命中判据 |
|------|------|------|---------|
| ① UC-MFG-02 phantom(bomType=PHANTOM) 子件不生成生产订单 | BomExpander `:104-108` 并入当前层级不产生独立节点 | **接受** | TestErpMfgBomExplosion#testPhantomExpandsIntoParentLevel 强断言（nodes.size()==1 仅子件 + phantom 物料不出现） |
| ② UC-MFG-02 其子件直接展开到当前工单物料需求 | BomExpander `:108` expandLines(childBom, effQty, level, ...) | **接受** | 同上（M1 level=1 并入父级） |
| ③ UC-MFG-02 齐套校验基于展开后全部子件（含虚拟件子件） | KitAvailabilityChecker `:66` explode(bomId, plannedQty, true) 多级展开 | **接受** | TestErpMfgWorkOrderStateMachine/EndToEnd 强断言（A1.9 已评级：齐套决定 STOCK_RESERVED/STOCK_PARTIAL） |
| ④ UC-MFG-02 环检测 ERR_BOM_CYCLE + 深度上限 ERR_BOM_MAX_DEPTH_EXCEEDED 异常路径 | BomExpander `:88-97` + ErpMfgErrors `:58-66` | **接受** | TestErpMfgBomExplosion#testCycleDetection/#testDepthLimitTruncation 强断言（assertThrows + 错误码比对） |
| ⑤ UC-MFG-02 有效用量算术 `line.quantity × scale` | BomExpander `:101-103` + divide 守护 | **接受** | TestErpMfgBomExplosion#testSingleLevel/#testMultiLevel 强断言（req=1/2 精确 + 多级乘积 2×5=10） |
| ⑥ UC-MFG-10 审核(DRAFT→SUBMITTED)时快照 BOM（工单行记录当时 BOM 内容） | grep 零命中 + ORM 无 snapshotBomVersion 列 + 无快照写路径 | **P1（新建 P1-RC-009）** | §2 P1①（功能完全缺失）+ §5 Q4（会计/成本正确性无例外）+ §4 三判据均不成立（见 §5.2） |
| ⑦ UC-MFG-10 BOM 后续修改不影响已审核工单物料需求/成本 | bomId 引用弱隔离（resolveBomId 优先 wo.bomId）+ 同 bomId 内容编辑无隔离 | **P1（合并 P1-RC-009）** | §2 P1①（行为实质偏离——内容级隔离缺失）+ §5 Q4；与 ⑥同根因（快照子系统缺失），合并裁决 |
| ⑧ UC-MFG-10 新建工单才用新 BOM | resolveBomId 新工单解析当前 bomId/默认 BOM | **接受** | 新默认 BOM 仅对新工单生效（设计正确） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**——UC-MFG-10 快照缺失定 P1 非 P0：快照缺失属"功能完全缺失"（§2 P1①），非 §2 P0④"活跃数据破坏防护未实现/会计过账正确性破坏"——当前完工 materialCost = Σ 领料单成本聚合（**不**经 BOM 重展开，A4.2a §2.2 证实），故快照缺失**不默认破坏活跃路径会计过账**（是否在差异计算/重算路径破坏凭证需运行时确认 → SP-1 交 MA4）；与 A1.8 P1-RC-008（物料预留）同型裁决逻辑（功能缺失类非活跃数据破坏类）。**不触发 MR0**，无 R0.n 实体行追加。

### 5.4 resolved finding HEAD 复核（HEAD `15bf103d2`，按逻辑非行号核验）

> BOM 相关 resolved finding 逐条在当前 HEAD 代码实际落地核验。

| Finding | 描述 | HEAD `15bf103d2` 实测 | 裁决 |
|---------|------|----------------------|------|
| **P1-MA4-008**（工单/BOM 链路跨域 daoFor 绕 I\*Biz） | A4.2a：5 站点 mfg→md/inv 只读 daoFor（含 BomExpander 跨域 daoFor 投影） | BomExpander 本身仅用 `daoFor(ErpMfgBom.class)`/`daoFor(ErpMfgBomLine.class)`（**同域 mfg**，**非跨域**——A4.2a §2.1 原列举 5 站点为 KitAvailabilityChecker:107 daoFor ErpInvStockBalance + ErpMfgWorkOrderProcessor:364 daoFor ErpMdMaterial + dispatcher AcctSchemaResolver 等，**不含 BomExpander**）；daoFor 跨域站点仍存在（KitAvailabilityChecker + WorkOrderProcessor）—— read-only 无活跃数据破坏；arm-index P1-MA4-008 ✅ resolved (plan 2026-07-29-2225-1: 读侧统一裁决登记于 `data-dependency-matrix.md §9`——md 子集=可迁移 / inv 子集=永久只读豁免)；A1.9 §5.4 已复核维持 | **resolved 维持**（永久只读豁免登记于 HEAD；BomExpander 同域 daoFor 不涉此 finding） |
| **P1-MA2-036**（MRP CANCELLED/预测 CONSUMED dict 死状态） | A2.6b：dict 死状态无 writer | A1.8 §5.4 已复核（HEAD `5953f07c1`）：方案 A 保留为预留 + mrp.md 措辞修正；本切片 HEAD `15bf103d2` 维持（BOM 相关分区无变化） | **resolved 维持**（与本切片 BOM 展开读侧无冲突） |
| **P1-MA2-037**（mrp.md RELEASED 文字 vs 实现 isFirmed 布尔） | A2.6b：owner doc 漂移 | A1.8 §5.4 已复核：mrp.md RELEASED→isFirmed 措辞修正落地；本切片维持 | **resolved 维持** |
| **P1-MA2-038**（委外 APPROVED O-4 豁免登记缺失） | A2.6b：posting-exemptions.md 未登记委外单豁免 | A1.8 §5.4 已复核：posting-exemptions.md §MrpReleaseService 登记落地；本切片维持 | **resolved 维持** |
| **P1-MA4-007**（完工编排层差异吞咽致业财悬挂） | A4.2a：reportCompletion catch 吞咽 GL 缺凭证 | A1.9 §5.4 已复核（HEAD `3c4beba78`）：R1.16 G3 错误分级 + 告警派发落地；本切片 HEAD `15bf103d2` 维持（BOM 快照缺失不涉完工差异过账路径，但完工 materialCost 不经 BOM 重展开故 P1-MA4-007 修复与本切片 P1-RC-009 互补不重叠） | **resolved 维持**（与本切片互补不重叠） |
| **P1-MA4-009**（工单/领料/BOM 测试有效性） | A4.2a：业财异常路径零覆盖 + 完工入库 GL voucher 行级断言缺失 | A1.9 §5.4 已复核：R2.11 done 测试补强；本切片 UC-MFG-10 快照零测试为**新功能点维度**（快照未实现→无测试可补），与 P1-MA4-009 不同控制点 | **resolved 维持**（本切片 P1-RC-009 含测试建立义务 successor） |

**结论**：6/6 BOM 相关 resolved finding 在 HEAD `15bf103d2` 实际落地，无回退、无部分落地、无 documented simplification 仍 open successor 升级。其中 BomExpander 同域 daoFor（ErpMfgBom/ErpMfgBomLine）**不涉** P1-MA4-008 跨域 daoFor 投影（A4.2a 原列举 5 站点不含 BomExpander）。

---

## 6. 与 arm-index 衔接（复用 or 新增裁决，§7）

### 6.1 产出 finding 前 grep 比对（禁止未经比对直接新建）

`grep arm-index.md mfg BOM/展开/快照 同域同控制点` 结果：

| 既有 finding | 控制点 | 根因 | 维度 | 与本切片候选 finding 的关系裁决 |
|-------------|--------|------|------|----------------------------|
| `P1-RC-008`（A1.8 预留写路径 Deferred） | mfg 工单审核触发/释放物料预留（UC-MFG-05/08） | MaterialReservation 子系统预留写路径完全缺失 | requirement-compliance 需求契约 | **不同控制点**（物料预留子系统 vs BOM 快照子系统，不同 UC、不同字段、不同写路径），不复用 |
| `P1-MA4-008`（跨域 daoFor 绕 I\*Biz） | 工单/BOM 链路 daoFor 跨域只读 | 跨域 daoFor 直访 | audit-remediation 架构边界 | **不同控制点**（BomExpander 同域 daoFor 不涉此 finding；快照缺失是功能缺失非架构边界），不复用 |
| `P1-MA3-042`（material-reservation.md 整个子系统未实现） | owner doc vs code drift | doc↔code 文本一致性 | audit-remediation owner-doc drift | **不同维度不同控制点**（物料预留 drift vs BOM 快照缺失；A3.4 范围未含 BOM 快照维度），不复用 |
| `P1-MA2-036/037/038`（MRP 状态机） | MRP dict 死状态 / owner doc 漂移 / O-4 豁免 | MRP 状态机 | audit-remediation 状态机 | **不同控制点**（MRP 状态机 vs BOM 快照），不复用 |
| `P1-MA4-007/009`（完工编排差异吞咽 / 测试有效性） | 完工差异过账 / 业财异常路径测试 | 编排层异常吞咽 / 测试覆盖 | audit-remediation 代码质量 / 测试 | **不同控制点**（完工差异过账 vs BOM 快照；完工 materialCost 不经 BOM 重展开），不复用 |

### 6.2 新建 finding 裁决

**裁决：本切片新建 1 项 P1 finding `P1-RC-009`**。

**`P1-RC-009` UC-MFG-10 BOM 快照原则完全缺失（内容级快照未实现，会计/成本正确性类 Q4 无例外）**

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——功能完全缺失 + 会计/成本正确性类，但非默认活跃路径破坏） |
| 目标 MR | **MR1**（R1.0 展开为 RC-R1.n） |
| 域 / UC | manufacturing / UC-MFG-10（断言④⑤，⑥已实现） |
| 文件 / 行 | 工单 BOM 快照写路径**全仓零命中**（grep `snapshotBomVersion`/`bomSnapshotStrategy`/`LOCK_AT_CREATION`/`AUTO_UPGRADE`/`bom-snapshot` 全 `module-manufacturing/` 零命中）；`ErpMfgWorkOrder` ORM（`app-erp-manufacturing.orm.xml:577`）仅 `bomId`（可空弱引用）**无 `snapshotBomVersion` 列**；`ErpMfgWorkOrderLine` 无快照内容列；审核路径（`ErpMfgWorkOrderProcessor` submitForApproval/approve + `ErpMfgWorkOrderReportCompletionProcessor`）无 BOM 内容复制动作；`KitAvailabilityChecker.resolveBomId:134-137`（wo.bomId 优先，新建 BOM 不影响已建工单——弱隔离）+ BomExpander.loadLines 实时查 ErpMfgBomLine（同 bomId 内容编辑无隔离） |
| 缺陷描述 | L1（`use-cases.md:182-184`）逐字「工单审核时快照 BOM(工单行记录当时 BOM 内容) / BOM 后续修改 → 不影响已审核工单的物料需求/成本 / 新建工单才用新 BOM」。L3 实仓：**断言④完全未实现**——工单仅以 `wo.bomId` 弱引用 BOM（orm.xml:577，可空），无 `snapshotBomVersion`/快照内容字段，审核时无 BOM 内容复制动作（grep 零命中 + ORM 无快照列 + 无快照 config key）；**断言⑤部分**——bomId 引用锁定提供"新建 BOM 不影响已建工单"弱隔离（resolveBomId 优先 wo.bomId），但同 bomId 内容编辑无隔离（BomExpander.loadLines 实时查 ErpMfgBomLine，无版本/快照门控）。owner doc `bom-and-routing.md §实现注记 :147` 显式列「BOM 版本快照」为 Non-Goal，但 §4 三判据核验均不成立（plan 2026-07-02-1538-2 独立审查为 AI 代理非人工批准 / owner doc Non-Goal 为 AI 落地补注无人工批准痕迹 / product-scope 未裁剪登记）。 |
| 影响 | BOM 内容（子件行）编辑后，已审核工单若再次展开 BOM（齐套重算/差异计算/重算路径）会读取编辑后内容 → 物料需求/成本计算基于错误 BOM 内容。**非默认活跃路径破坏**：完工 materialCost = Σ 领料单成本聚合（A4.2a §2.2 证实，不经 BOM 重展开），故默认完工过账不受影响；但差异计算/重算路径若读 BOM 则受影响（运行时确认 → SP-1）。Q4 会计/成本正确性类无例外 → 必须实现，禁方案 B。 |
| 修复方向 | MR1 裁决——方案 A（推荐）：`ErpMfgWorkOrder` ORM 增 `snapshotBomVersion` 字段 + `ErpMfgWorkOrderLine` 增快照内容列（或新增 `ErpMfgWorkOrderBomSnapshot`/`ErpMfgWorkOrderBomLineSnapshot` 快照实体）+ 审核（DRAFT→SUBMITTED）Processor 复制 BOM 头/子件行/工艺行到快照 + KitAvailabilityChecker/BomExpander 读侧改为读快照（已审核工单）vs 读 live BOM（新建工单）+ config `erp-mfg.bom-snapshot-strategy`（LOCK_AT_CREATION/AUTO_UPGRADE）接线；**触及 ORM 结构变更 + 成本计算读侧，须 ask-first + 独立 plan-audit**（§5 ORM 结构变更类 + 成本正确性类）。方案 B 简化为工单行 INPUT 类型在审核时固化（materialId/quantity 不可改）+ BOM 编辑经新版本（新建 bomId）非原地改——仍触及 ORM + KitAvailabilityChecker 读侧。 |

### 6.3 双向可追溯

- **本切片 → 新 finding**：UC-MFG-10 ④⑤ → `P1-RC-009`（本切片新建，目标 MR1，修复触及 ORM 结构变更 + 成本计算读侧，须 ask-first + 独立 plan-audit）。
- **新 finding → arm-index**：`P1-RC-009` 写入 arm-index MA1(RC) finding 分区（§P1-RC-009 行）。
- **修复行引用 finding**：MR1 的 RC-R1.n 修复行须含 `P1-RC-009` 交叉引用。
- **MV V.3 校验**：closure audit 核验 `P1-RC-009` 修复状态为 `done` 或显式 successor。

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行。

| # | 存疑点 | 触发条件 | 交 MA4 展开 |
|---|--------|---------|------------|
| SP-1 | **BOM 内容编辑后已开工工单运行时是否实际按新 BOM 重算物料需求/成本**：UC-MFG-10 快照缺失（P1-RC-009）的运行时会计影响。完工 materialCost = Σ 领料单成本聚合（不经 BOM 重展开，默认完工过账不受影响），但差异计算（`ErpMfgCostVarianceCalculator`）/成本重算路径若读 BOM（经 BomExpander.explode）则受同 bomId 内容编辑影响。需运行时确认：(a) 差异计算是否经 BOM 重展开读标准用量；(b) 成本重算路径是否读 BOM；(c) 齐套重算（checkAvailability 二次调用）是否在 BOM 编辑后读新内容。 | BOM 子件行编辑（增/删/改物料或数量）+ 已审核工单触发差异计算/重算/二次齐套 | A4.2 运行时探针（确认 P1-RC-009 是否致成本结转凭证错误，决定是否升 P0） |
| SP-2 | **快照缺失运行时是否致成本结转凭证错误**：与 SP-1 同根因，聚焦 GL 凭证层面——若 SP-1 确认差异计算/重算读新 BOM，则 PRODUCTION_VARIANCE 凭证 + 成本结转凭证金额错误。需运行时确认凭证行级金额是否偏离审核时 BOM 内容。 | 同 SP-1 + config `erp-mfg.variance-auto-calc-enabled=true` | A4.2 运行时探针（与 SP-1 协同，闭合 P1-RC-009 会计影响裁决） |
| SP-3 | **bomId 弱隔离运行时边界**：`KitAvailabilityChecker.resolveBomId:134-137` wo.bomId 优先 → 新建 BOM（新 bomId）不影响已建工单。但运营若"编辑同一 bomId 的 BOM 内容"（而非新建 bomId）则无隔离。需运行时确认运营 BOM 变更实践（编辑 vs 新建）+ 是否存在 BOM 版本化实践（同 bomId 多版本）。 | 运营 BOM 变更操作 | A4.2 运行时探针（确认 P1-RC-009 修复方案的实践约束） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（详见 §5.3），故**不触发 MR0**。无 R0.n 实体行追加。SP-1/SP-2 若运行时确认致成本结转凭证错误，下一轮审计 OPEN_AUDIT 将重新定级（非本审计重开）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总表见下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（UC-MFG-10 BOM 快照 → `P1-RC-009` 新建）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 比对表），**无未经比对直接新建的 finding**（P1-RC-009 经比对 6 个候选既有 finding 均不同控制点/不同维度后新建）。

### actual vs baseline 汇总表（HEAD `15bf103d2`，2026-08-02 实测）

| 规则 | 描述 | actual | baseline | 漂移 |
|------|------|--------|----------|------|
| R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 |
| R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 |
| R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 |
| R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 |
| R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 |
| R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | 0 |
| R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | 0 |
| R2d | Processor daoFor(ErpMd*) | 34 | 34 | 0 |
| R3 | new Erp*() 构造实体 | 5 | 5 | 0 |
| R4 | extends RuntimeException | 0 | 0 | 0 |
| R5 | @Inject private | 0 | 0 | 0 |
| R6 | @Transactional in BizModel | 2 | 2 | 0 |
| R7 | System.currentTimeMillis() | 0 | 0 | 0 |
| R8 | Processor 无 xbiz 接线 | 0 | 0 | 0 |
| R10 | REQUIRES_NEW 事务 | 6 | 6 | 0 |
| R11 | Processor 重复状态判断方法 | 0 | 0 | 0 |
| R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | 0 |
| R12b | 共享内核 import PostingEvent | 66 | 66 | 0 |
| R12c | 共享内核 import AcctSchemaResolver | 40 | 40 | 0 |

**汇总**：全 19 可计数规则 actual **精确等于** baseline，**0 漂移**（0 regression + 0 improvement）。本审计为**只读审计**（无生产代码/ORM/api.xml/view.xml/真相源变更），checker **无回归风险**（纯 reporter，退出码 0；本报告不以退出码 0 作为门控通过依据，对齐 R6.9 教训）。

---

## 报告 9 段完整性自检

| # | 段落 | 存在 | 备注 |
|---|------|------|------|
| 1 | 需求契约原文（L1 逐字引用） | ✅ | UC-MFG-02/10 共 6 断言逐字 |
| 2 | 实现证据（L3 file:line + 跨域链） | ✅ | BomExpander DFS/phantom/环检测/深度上限/算术 实测 + UC-MFG-10 快照 grep 零命中证据 + ORM 快照列核验 |
| 3 | 测试证据（L4 + 断言强度） | ✅ | UC-MFG-02 5 强 + 1 中；UC-MFG-10 快照零测试（功能推定未实现） |
| 4 | 运行时行为证据（L5） | ✅ | 复用 A2.6b/A4.2a + 本切片差异（UC-MFG-02 接受 / UC-MFG-10 内容级快照缺失） |
| 5 | 符合性结论（矩阵 + 每 UC + resolved finding HEAD 复核） | ✅ | 2 UC 结论（UC-MFG-02 接受 + UC-MFG-10 P1 新建 P1-RC-009）+ 候选缺口 ①-⑧ 分级 + §4 三判据核验 + 6 resolved finding HEAD 复核 |
| 6 | 与 arm-index 衔接（复用 or 新增） | ✅ | 1 新建（P1-RC-009）+ 比对表（6 候选既有 finding 均不同控制点） |
| 7 | 静态存疑点清单 | ✅ | SP-1/SP-2/SP-3（交 MA4 运行时确认 P1-RC-009 会计影响） |
| 8 | 过程纪律自检段 | ✅ | checker actual=baseline 0 漂移 + 独立性 + 去重 |
| 9 | 与 MA2/MA4 报告差异增量声明 | ✅ | 前置声明（报告开头） |

**9 段齐全，完整性自检 PASS。**

---

## Verdict

- **UC-MFG-02**（多级 BOM 展开 phantom 虚拟件）：**接受**（3 断言全证据一致——phantom 子件并入当前层级不产生独立节点 + 其子件直接展开到当前工单物料需求 + 齐套基于展开后全部子件含虚拟件子件；A4.2a §2.2 BomExpander DFS/环检测/phantom 展开/算术 PASS + TestErpMfgBomExplosion 强断言）
- **UC-MFG-10**（BOM 变更不影响已开工工单快照原则）：**P1**（§2 P1①——④审核时快照 BOM 内容完全未实现 + ⑤同 bomId 内容编辑无隔离；§4 三判据均不成立[plan-audit 为 AI 代理非人工批准 / owner doc Non-Goal 为 AI 落地补注无人工批准痕迹 / product-scope 未裁剪]；§5 Q4 会计/成本正确性类无例外禁方案 B）→ 新建 `P1-RC-009`（⑥新建工单才用新 BOM 已实现归接受）
- **resolved finding HEAD 复核**：6/6（P1-MA4-008 / P1-MA2-036 / P1-MA2-037 / P1-MA2-038 / P1-MA4-007 / P1-MA4-009）在 HEAD `15bf103d2` **全部已落地无回退**（BomExpander 同域 daoFor 不涉 P1-MA4-008 跨域投影）
- **新 finding**：**1 项**（`P1-RC-009` UC-MFG-10 BOM 快照原则完全缺失，目标 MR1，修复触及 ORM 结构变更 + 成本计算读侧，须 ask-first + 独立 plan-audit）
- **P0 即时通道**：**未触发**（本切片无 P0——UC-MFG-10 快照缺失属功能完全缺失类[§2 P1①]非活跃数据破坏类[§2 P0④]，完工 materialCost 不经 BOM 重展开故默认完工过账不受影响，会计影响需运行时确认 SP-1/SP-2）

**整体 Verdict**：⚠️(P1) — 2 UC 中 1 UC（UC-MFG-02 phantom 展开）接受、1 UC（UC-MFG-10 快照原则）P1 新建 `P1-RC-009`，零 P0。本切片主要价值 = **UC-MFG-02 phantom 展开需求验收**（接受，复用 A4.2a 算法正确性结论）+ **UC-MFG-10 快照原则 L1 字面要求 vs 推定完全未实现的符合性裁决**（P1-RC-009，§4 三判据核验 + Q4 会计/成本无例外）+ **BomExpander 同域 daoFor 不涉 P1-MA4-008 跨域投影的澄清**（A4.2a 原列举 5 站点不含 BomExpander）+ **3 项静态存疑点**（SP-1/SP-2/SP-3 交 MA4 运行时确认 P1-RC-009 会计影响）。
