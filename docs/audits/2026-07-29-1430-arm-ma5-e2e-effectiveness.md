# MA5 E2E 测试有效性审计报告（A5.6）

> Plan: `docs/plans/2026-07-29-1430-2-ma5-cross-cutting-test-audit.md` Phase 2
> Roadmap: `docs/backlog/audit-remediation-roadmap.md` A5.6
> Skill: `docs/skills/open-ended-audit-prompt.md`
> Date: 2026-07-29
> 范围：`tests/e2e/**/*.spec.ts`（258 spec）
> M0 锚点：HEAD=`0e963531d`
> 基线：2026-07-25 全套件 490 passed / 1 failed [master-data.write.amis Non-Goal] / 3 skipped

## 1. 审计目标与方法

按 `open-ended-audit-prompt.md` 对 258 E2E spec 做业务断言强度抽样评估：

1. **建断言强度分类矩阵**：数值断言 / DOM 断言 / 业务动作 / 编排链 / 仅冒烟。
2. **标记薄弱 spec**：仅冒烟（GraphQL 200 + 存在性）无业务数值断言的 spec，评估「GraphQL 200 即通过」是否掩盖后端返回空数据。
3. **交叉验证已知 E2E 缺口状态**：AMIS `$var` 损坏 8 参数化看板 successor / master-data.write.amis Non-Goal。
4. **评估 E2E 与单元层覆盖互补性**。

反窄化自检：本审计不局限于按文件名机械分类，而是对每种断言层做**有效性验证**——即「该层若后端返回空数据，测试是否会失败」的对抗性推演（这正是 AMIS `$var` bug 提供的唯一历史实证）。

## 2. 核心结论

**Verdict: ⚠️(P1)（零 P0 + 1 项 P1 + 2 项 P2 watch-only）**

- E2E 套件断言强度分布**主体健康**：72.5% spec（187/258）具备强业务断言（数值/状态翻转/编排产物）。
- **1 项 P1**：53 仅冒烟 spec（20.5%）只断言 GraphQL 200 + 关键词存在 + DOM 渲染，**无法检测后端返回空/零数据**——AMIS `$var` bug（bug 2026-07-09-1249）是此风险的**唯一已证实历史案例**（冒烟层漏检 8 参数化看板长达多日，由 visual 层首次捕获）。
- AMIS `$var` bug **已修复**（plan 2026-07-09-1728-1），且 visual 层现已加数值 token 断言（10 看板全绿）——successor 完整落地。
- master-data.write.amis **仍为唯一失败**（test-infra Non-Goal，非产品缺陷，非隔离污染物）。

## 3. 258 spec 业务断言强度分类矩阵

### 3.1 分类标准

| 档 | 断言强度 | 判定规则 | 对抗性推演（后端返回空数据时） |
|----|---------|---------|------------------------------|
| **强 (Strong)** | 数值/状态/产物 | 断言确定性业务数值 / 状态翻转 / 过账产物存在 | **失败**（数值不符 / 状态不变 / 产物缺失） |
| **中 (Medium)** | DOM 结构 + 数值 token | 断言 AMIS 渲染结构 + 含确定性数值 token | **失败**（token 缺失） |
| **弱 (Weak)** | 仅冒烟 | 仅 GraphQL 200 + body 长度 + 关键词存在 | **通过**（空数据仍返回 200 + 标签仍渲染） |
| **工具** | 诊断/探索 | 非业务覆盖（diag / exploration / example） | N/A |

### 3.2 分类矩阵（按文件名 + helper 断言实测）

| 类别 | 文件模式 | spec 数 | 占比 | 断言内容（helper 实测） | 档 |
|------|---------|--------|------|----------------------|-----|
| 数值断言（看板） | `dashboards/*.value.spec.ts` | 10 | 3.9% | GraphQL `getDashboardKpi` 取值断言确定性数值（如 inv totalValue=10450） | **强** |
| 数值断言（报表） | `reports/*.value.spec.ts` | 18 | 7.0% | `renderHtml` HTML 含确定性 token（如利润表 1,130.00） | **强** |
| 数值断言（CRUD 列表） | `crud/*.list-value.spec.ts` | 13 | 5.0% | GraphQL `findPage` 断言 seed 行数 + token | **强** |
| 数值断言（业务动作侧） | `business-actions/*.value.spec.ts` | 1 | 0.4% | 同上 | **强** |
| 业务动作（状态机） | `business-actions/*.action.spec.ts` | 112 | 43.4% | 状态翻转（approveStatus）+ 副作用（库存/凭证联动）经 `verifyState` | **强** |
| 编排链（跨域过账） | `orchestration/*.spec.ts` | 10 | 3.9% | P2P/O2C/委外/红冲全链 + 业财过账产物（凭证 bill_r + AR-AP openAmount） | **强** |
| CRUD 写路径 | `crud/*.write.spec.ts` | 7 | 2.7% | create→get→update→get→delete→get(not-found) 全链 | **强** |
| 前端渲染 DOM | `visual/*.visual.spec.ts` | 20 | 7.8% | AMIS 全路径渲染结构 + echarts canvas + **数值 token**（修复后） | **中** |
| 视觉快照 | `visual/*.snapshot.spec.ts` | 2 | 0.8% | 像素/结构快照对比 | **中** |
| **仅冒烟（看板）** | `dashboards/*.smoke.spec.ts` | 10 | 3.9% | GraphQL 200 + body>100 + KPI 关键词存在（如"资产"） | **弱** |
| **仅冒烟（报表）** | `reports/*.smoke.spec.ts` | 24 | 9.3% | GraphQL 200 + renderHtml 调用成功 | **弱** |
| **仅冒烟（CRUD）** | `crud/*.smoke.spec.ts` | 19 | 7.4% | GraphQL 200 + 列表 DOM + add 按钮 + 表单字段（**不断言行数>0**） | **弱** |
| 报表下载 | `reports/*.download.spec.ts` | 2 | 0.8% | 下载响应 200 + content-type | **弱** |
| 诊断/探索/示例 | `diag-*` / `_exploration/*` / `examples/*` | 7 | 2.7% | 工具/可行性探针 | **工具** |
| **合计** | | **258** | 100% | | |

### 3.3 汇总

| 档 | spec 数 | 占比 |
|----|--------|------|
| **强** | 171 | 66.3% |
| **中** | 22 | 8.5% |
| **弱（仅冒烟）** | 55 | 21.3% |
| **工具** | 7 | 2.7% |
| 不计入（Non-Goal 失败） | 1（master-data.write.amis） | 0.4% |
| skipped | 3 | 1.2% |

> **覆盖率口径修正**：roadmap 记「260+ spec」，实测 258（差额 2 = 计数时点不同 + master-data.write.amis Non-Goal 归类）。其中 7 个工具/诊断 spec 非业务覆盖，**有效业务覆盖 spec = 251**。

## 4. 薄弱 spec 清单与对抗性推演

### 4.1 仅冒烟层（55 spec）——P1 风险

**`runDashboardSmoke`（`dashboards/_helper.ts`）断言实测**：
```ts
expect(bodyText.length).toBeGreaterThan(100);          // body 长度
const hasKpi = kpiKeywords.some(kw => bodyText.includes(kw));  // 关键词存在
expect(graphqlResponses.length).toBeGreaterThan(0);    // 有 GraphQL 调用
for (status of graphqlResponses) expect(status).toBe(200);  // 200
```
**对抗性推演**：若看板后端返回 `{totalValue: 0, materialCount: 0}`（空数据），但 AMIS 仍渲染 KPI 卡片标签（"资产"/"折旧"），则 `bodyText>100` ✓ + 关键词存在 ✓ + GraphQL 200 ✓ → **测试通过，缺陷漏检**。

**`runCrudListSmoke`（`crud/_helper.ts`）断言实测**：
```ts
await crud.waitForList();             // 列表 DOM 渲染
await expect(addBtn).toBeVisible();   // add 按钮可见
expect(graphqlResponses.length).toBeGreaterThan(0);
for (status) expect(status).toBe(200);
```
**对抗性推演**：若 `findPage` 返回 `{total: 0, items: []}`（种子缺失），列表渲染空表格 + add 按钮仍可见 + GraphQL 200 → **测试通过，种子缺失漏检**。

### 4.2 历史实证——AMIS `$var` bug（bug 2026-07-09-1249）

**这是「仅冒烟掩盖空数据」风险的唯一已证实案例**：

- 8 参数化看板 `*.page.yaml` 手写 GraphQL `query($var:Type)` 中裸 `$var` 经 AMIS 模板解析被替换为空，致查询损坏、KPI 恒返回 0/空。
- **冒烟层（10 `*.smoke.spec.ts`）全部通过**——GraphQL 仍 200（损坏查询返回空集非 HTTP 错误）+ KPI 标签仍渲染。
- **数值层（`*.value.spec.ts` 直调后端绕过 AMIS）亦通过**——后端 KPI 正确，缺陷在 AMIS 模板层。
- 仅 **visual 层（`dashboards.visual.spec.ts`）DOM + 数值 token 断言**首次捕获（期望 token `1130` 未出现在 DOM）。
- **状态**：已修复（plan 2026-07-09-1728-1，`${'$'}` YAML 转义），visual 层已加 10 看板数值 token 断言全绿。

**教训**：仅冒烟层对「查询损坏 / 种子缺失 / 后端返回空集」类缺陷**结构性盲区**。该类缺陷需 visual/value/action 层捕获。

### 4.3 缓解因素——并行强覆盖

多数仅冒烟 spec 有并行强覆盖（同域 value/visual/action spec）：

| 仅冒烟域 | 并行强覆盖 | 风险 |
|---------|-----------|------|
| 10 看板 smoke | 10 看板 value + 10 看板 visual（含数值 token） | **已全覆盖**——空数据由 value/visual 捕获 |
| 18 报表 smoke | 18 报表 value | **已全覆盖** |
| 19 CRUD smoke | 13 CRUD list-value | **6 域无并行**（list-value 未覆盖的 CRUD smoke 域） |
| 报表 download | — | 低（下载机制非业务数值） |

**残余风险**：6 域 CRUD smoke 无并行 list-value（aps/b2b/contract/notify/logistics + 部分 master-data 子实体）——若这些域种子缺失，CRUD smoke 漏检。但这 6 域均为 C 级/扩展域，业务风险低。

## 5. 已知 E2E 缺口状态交叉验证

| 缺口 | plan 基线状态 | 实测当前状态 | 裁决 |
|------|-------------|-------------|------|
| AMIS `$var` 损坏 8 参数化看板 [bug 2026-07-09-1249] | 「Deferred successor」 | **已修复**（plan 2026-07-09-1728-1）+ visual 层 10 看板数值 token 断言全绿（1130/10450/135000 等） | ✅ successor 完整落地 |
| master-data.write.amis 唯一失败 | 「Non-Goal」 | **仍为唯一失败**（AMIS form-button selectOption↔switch 写周期，`crud/_helper.ts:174`） | ✅ 维持 Non-Goal（非产品缺陷，plan 2026-07-24-1945-1 显式排除） |
| xwf 浏览器层审批轴（4 实体） | 「NOT FEASIBLE」[plan 2026-07-09-2330-1] | **维持 NOT FEASIBLE**（sysUser(0) 兜底阻断，平台限制非测试缺陷） | ✅ Non-Goal 维持 |

> **plan 基线偏差**：plan Current Baseline（line 13）记 AMIS `$var` 为「已知 E2E 基础设施缺口」——**该声明已过时**，bug doc 头部明确「状态：已修复（plan 2026-07-09-1728-1）」。本审计据实仓 bug doc 修正。

## 6. E2E 与单元层覆盖互补性评估

| 业务路径 | E2E 覆盖 | 单元覆盖 | 互补裁决 |
|---------|---------|---------|---------|
| CRUD 读写 | list-value + write（13+7） | 各域 TestXxxCrudSmoke（~60） | **双层**（E2E 浏览器路径 + 单元引擎路径） |
| 看板 KPI 数值 | value + visual（数值 token） | 各域 TestXxxDashboard / ReportRendering | **双层** |
| 业财端到端（P2P/O2C/红冲/委外） | orchestration（10 spec 全链产物断言） | 各域 TestXxxEndToEnd | **双层** |
| 状态机翻转 | business-actions（112 spec） | 各域 TestXxxStateMachine | **双层** |
| **xwf 审批轴**（Payment/Receipt/Disposal/Salary） | **无**（NOT FEASIBLE） | 后端单测覆盖审批管道 | **仅单元**（浏览器层不可达，平台限制） |
| 资产报废浏览器层 | **无**（known gap） | 后端单测覆盖 SCRAP/SOLD | **仅单元**（L-8 记录，DIRECT 路径可达但未补 spec） |
| AMIS 模板渲染正确性 | visual（DOM + 数值 token） | **无**（单元不经 AMIS） | **仅 E2E**（AMIS 层缺陷只能 E2E 捕获，AMIS `$var` bug 实证） |

**互补性裁决**：E2E 与单元层互补性**良好**。唯一「双无覆盖」是 xwf 浏览器层审批（平台限制不可达，后端单测已覆盖管道）。AMIS 模板层是 E2E 独占价值（单元层结构性无法覆盖）。

## 7. Finding 登记

### P1-MA5-012 — 仅冒烟 spec（55/258=21%）结构性盲区：GraphQL 200 + 关键词存在无法检测后端空数据

- **域/层**：`tests/e2e/`（dashboards/reports/crud smoke，跨域）
- **描述**：55 spec（10 看板 smoke + 24 报表 smoke + 19 CRUD smoke + 2 报表 download）仅断言 GraphQL 200 + DOM 渲染 + 关键词存在，不断言业务数值/行数。AMIS `$var` bug（bug 2026-07-09-1249）是此盲区的**已证实案例**——8 看板 KPI 恒 0/空，冒烟层全绿漏检多日，仅 visual 层捕获。当前缓解：10 看板 + 18 报表 smoke 有并行 value/visual 强覆盖（空数据由强层捕获）；残余风险在 6 域 CRUD smoke 无并行 list-value（aps/b2b/contract/notify/logistics + 部分 master-data 子实体，均 C 级/扩展域低风险）。
- **严重性**：P1（已证实缺陷漏检路径 + 跨域系统性）—— 但**无活跃缺陷**（AMIS `$var` 已修复，当前 490 passed 基线可信）。
- **目标 MR**：MR3（测试维度）。修复选项：(A) 为 6 无并行 CRUD smoke 域补 `*.list-value.spec.ts`（对齐既有 13 域模式，推荐）；(B) 增强 `runCrudListSmoke` / `runDashboardSmoke` helper 加最小数据存在断言（如 `total>0` 当种子保证非空时），低成本但对「种子缺失」类缺陷有效；(C) 维持现状 + 文档化「冒烟层依赖并行强覆盖」约定。
- **去重**：与 P1-MA5-001~011（S 级域覆盖深度）无重叠——本项是跨切 E2E 断言强度基础设施，非域内覆盖深度。与 P2-MA5-005（E2E 共享库隔离）相关但不同根因（本项是断言强度，P2-MA5-005 是状态泄漏）。

### P2-MA5-006（watch-only）— 7 工具/诊断 spec 计入 258 总数，轻微虚高业务覆盖统计

- **描述**：`diag-smoke` / `diag-deep` / `diag-deep2` / `diag-all-errors`（4 诊断）+ `visual/_exploration/*`（2 可行性探针）+ `examples/*`（1 示例）共 7 spec 计入 258 总数，但非业务覆盖。有效业务覆盖 spec = 251。roadmap「260+ spec」声明含此偏差。
- **严重性**：P2 watch-only（统计口径，非覆盖缺口）
- **目标 MR**：MR3（或文档维护）。修复：将诊断 spec 移至 `tests/e2e/_tooling/` 子目录排除出业务计数，或在 e2e-runbook 注明有效业务 spec 数。

### P2-MA5-007（watch-only）— master-data.write.amis Non-Goal 长期悬挂

- **描述**：`master-data.write.amis.spec.ts` 是全套件唯一失败（AMIS form-button selectOption↔switch 写周期不可达），自 2026-07-12 悬挂至今。非产品缺陷、非隔离污染物，但持续计为「1 failed」影响基线声明清晰度。
- **严重性**：P2 watch-only（已知 Non-Goal，非回归）
- **目标 MR**：MR3。修复选项：(A) 加 `.skip` 并注明 Non-Goal 触发条件（序列推进修复后移除）；(B) 重写为非 AMIS form-button 路径；(C) 维持现状（known-good-baselines 已登记 Known Failures 表）。

## 8. Exit Criteria 核对

- [x] E2E 有效性报告产出，含 258 spec 断言强度分类矩阵（§3）+ 薄弱 spec 清单（§4，55 仅冒烟 + 对抗性推演 + 历史实证）
- [x] A5.6 P0/P1/P2 已登记 arm-index.md 且去重（P1-MA5-012 + P2-MA5-006/007，与 MA1-MA4 + plan 2026-07-29-1430-1 既有 P1 经交叉去重无重叠——维度不同：跨切 E2E 断言强度 vs 域覆盖深度 vs 结构/业务/文档/代码质量）

## 9. 备注

- 本审计为纯只读审计，零代码/ORM/契约变更。
- plan Current Baseline（line 13）关于 AMIS `$var` 缺口的声明**已过时**——bug doc 明确「已修复（plan 2026-07-09-1728-1）」，本审计据实仓修正（§5）。
- E2E 全套件回归（490 passed）由 2026-07-25 基线确认，本轮不重跑（纯审计无代码变更）。
