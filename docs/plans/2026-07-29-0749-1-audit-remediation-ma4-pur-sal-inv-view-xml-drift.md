# 2026-07-29-0749-1-audit-remediation-ma4-pur-sal-inv-view-xml-drift MA4 pur+sal+inv view.xml vs 后端契约 drift 审计（A4.7）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A4.7（pur+sal+inv view.xml vs 后端契约 drift，A 级 view.xml drift 第二批）
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.7，pur+sal+inv view.xml vs 后端契约 drift）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行 + §残留风险 5「未覆盖：AMIS view.xml drift」；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）；`docs/design/purchase/state-machine.md` + `docs/design/sales/state-machine.md` + `docs/design/inventory/state-machine.md`（owner doc 状态机对照锚点）；`docs/plans/2026-07-29-0430-3-audit-remediation-ma4-finance-mfg-view-xml-drift.md`（A4.6，view.xml drift 第一批 S 级——本计划紧随其后同型方法）；`docs/plans/2026-07-29-0430-2-audit-remediation-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5，pur+sal+inv 后端代码质量审计——本审计的前端契约对照基线）
> Audit: required

## Current Baseline

purchase + sales + inventory 三域 view.xml vs 后端契约 drift 审计（代码与前端质量层 MA4，view.xml drift 维度第二批 A 级）。roadmap 工作项 A4.7 声明审查"pur+sal+inv view.xml vs 后端契约 drift"，owner doc 标注各域 view.xml，skill `docs/skills/multi-dimensional-audit-prompt.md`。A4.6（finance+mfg S 级第一批，134 view.xml）已于 2026-07-29 完成并经独立 closure audit PASS——drift 密度 0.75%（1 P1/134 view.xml），delta 层 bounded-merge 自愈 + xbiz/Processor 正式接线 + ORM ext:dict 三道防线有效抑制字段名/动作名/dict 三类高频 drift；drift 集中在枚举值 + gen-control 内联脚本 2 维度。本批次在已验证的同型方法上推进 A 级三域。

**关键基线事实（实时仓库核实）**：

- **三域 view.xml 规模**（实时仓库核实）：`find module-<domain> -name "*.view.xml" -not -path "*/target/*"`：purchase **40 view.xml** / sales **32 view.xml** / inventory **42 view.xml** = **合计 114 view.xml**。三域均为 A 级域，view.xml 数量中等（finance 72 / hr 72 / crm 68 / mfg 62 为更高批次，归 A4.6/A4.8）。view.xml 是 AMIS 前端页面定义，调用后端 GraphQL API（字段名 / BizMutation 动作名 / 枚举值 / dict 绑定 / 参数类型），与后端 xbiz/xbiz-meta/BizModel 契约的一致性是前端可用性的基础。
- **drift 维度**（`multi-dimensional-audit-prompt.md` 7 维度适配"view.xml vs 后端契约"主题——经 A4.6 验证有效）：(1) **字段名一致性**；(2) **BizMutation/BizQuery 动作名一致性**；(3) **枚举值/状态值一致性**——重点复核 MA2 三域状态机 dict 死状态 view 层投影；(4) **参数类型一致性**；(5) **dict 绑定一致性**；(6) **gen-control 内联脚本契约**；(7) **跨实体字段引用**。
- **MA2 已审计的三域状态机 finding（view.xml drift 审计输入）**：
  - A2.8 purchase（`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）：P1-MA2-049 Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 / P1-MA2-050 INLINE reject/withdrawApproval 缺 isCancelled 守卫致副轴漂移 / P1-MA2-051 PurReversalListener.rollbackReceive 不对称致冲销后 receive APPROVED+posted=false 悬挂。
  - A2.9 sales（`docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`）：P1-MA2-056 Contract reverseApprove→SUBMITTED INLINE 契约漂移 / P1-MA2-057 INLINE withdrawApproval 缺 isCancelled 守卫。
  - A2.11 inventory（`docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`）：P1-MA2-062 StockTake completeTake 未自动生成盘盈/盘亏移动单 / P1-MA2-063 PickingOrder PICKING/PICKED dict 死状态 + ErpInvPickingOrderBizModel CRUD 桩。
  - 这些状态机/契约 finding 在 view.xml 状态映射层（状态 badge、按钮 visibleOn、下拉筛选）的投影是本审计的重点复核项——dict 死状态是否在 view.xml 映射为可见状态（误导用户）或不可达状态（页面显示死选项）。
- **MA3 已审计的 API 契约 finding（view.xml drift 审计输入）**：A3.6 API 契约一致性（P1-MA3-047 API 命名/参数跨域不一致 / P1-MA3-048 孤儿 Processor bean 携带 String 影子契约）——view.xml 调用的动作名/参数类型 vs 这些 API 契约 finding 的交叉复核。
- **MA1 ORM 审计 finding（字段名维度输入）**：P1-MA1-022 跨域只读 daoFor 投影（pur+sal+inv 9 域）——view.xml 引用关联实体字段路径 vs ORM refEntityName。
- **后端代码质量审计（A4.5 pur+sal+inv+qa+crm）已完成**：后端 BizModel/xbiz 契约是稳定的对照基线。本审计以后端契约为真相源，审查前端 view.xml 的漂移方向。
- **已知前端问题历史**：frontend-ui-roadmap（`2026-07-23-1408-3`）Phase 2/3 修复 notify-inbox 裸变量 / AMIS ErpMdPartner 非法 GraphQL + adapt typo / inventory.write input-table tabs；多份 E2E plan 修复 page.yaml `$var` 模板 + reportContainer 接线 + 报表下载 URL + 日期参数 valueFormat。本审计系统性审查 pur/sal/inv 业务页面（非报表/看板）的 view.xml vs 后端契约 drift。

**审计张力**：A4.6（S 级第一批）已证明 delta 层 bounded-merge 自愈 + xbiz/Processor 正式接线 + ORM ext:dict 三道防线有效，drift 密度低。但 A 级三域（pur+sal+inv）从未做过系统性 view.xml vs 后端契约 drift 审计——arch-gov §残留风险 5「未覆盖：AMIS view.xml drift」明确标注此维度进入 MA4。pur+sal+inv 三域是 ERP 核心交易链路（采购到付款 / 销售到收款 / 库存核算），其 view.xml 状态映射/按钮动作/dict 绑定直接影响核心交易页面的可用性。本审计是该维度的第二批（A 级），A4.8（crm A 级 + hr S 级）为后续批次。

剩余差距：需要一次 pur+sal+inv view.xml vs 后端契约 7 维度 drift 系统性审计。发现的缺陷分类为：(a) **字段名 drift**（major）；(b) **动作名 drift**（major）；(c) **枚举/dict drift**（major——重点复核 PickingOrder PICKING/PICKED + StockTake DONE 死状态 view 层投影）；(d) **参数类型 drift**（major）；(e) **内联脚本契约 drift**（major/P2）；(f) **跨实体字段引用 drift**（P2）。major 登记为 P1（代码类目标 MR2——view.xml 修复属代码变更）。view.xml 修改不触及 ORM 保护区域（不改 model/*.orm.xml），但触及用户可见前端行为。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 7 维度（适配"view.xml vs 后端契约 drift"主题，经 A4.6 验证有效）对 purchase 40 view.xml + sales 32 view.xml + inventory 42 view.xml 做系统性前端-后端契约一致性审计，产出审计报告。
- 审计覆盖 7 drift 维度：字段名一致性 / BizMutation 动作名一致性 / 枚举值状态值一致性 / 参数类型一致性 / dict 绑定一致性 / gen-control 内联脚本契约 / 跨实体字段引用。
- 复核 MA2 三域状态机 finding（P1-MA2-049/050/051/056/057/062/063）+ MA3 API 契约 finding（P1-MA3-047/048）+ MA1 跨域只读 finding（P1-MA1-022）+ 前端 UI-roadmap Phase 3 残留在 view.xml 层的投影，标记是否有 MA2/MA3/MA1/前端-roadmap 未发现的 view.xml 层 drift。
- scope matrix §2.4「view.xml drift（MA4）」行 + arch-gov §残留风险 5 推进至完成（pur+sal+inv 维度）。
- 发现的 major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（起始编号 = A4.6 已分配最大 P1-MA4-N（023）+ 1 = P1-MA4-024，避免命名空间碰撞）。roadmap A4.7 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做 finance+mfg view.xml drift — 归 A4.6（S 级第一批，已 done）。
- **不**做 crm+hr view.xml drift — 归 A4.8（crm A 级 + hr S 级批次，view.xml 数 68+72）。
- **不**做后端 BizModel/xbiz 代码实现质量 — 归 A4.5（pur+sal+inv+qa+crm，已 done）。本审计以后端契约为真相源审前端 view.xml 漂移方向。
- **不**做报表/看板 page.yaml 渲染层问题 — 前端 UI-roadmap + 多份 E2E plan 已修复（`$var` 模板 / reportContainer 接线 / 报表下载 URL / 日期参数 valueFormat）。本审计审业务页面 view.xml（grid/form/page 结构），非报表 page.yaml。
- **不**做像素级视觉回归 — 归前端 UI-roadmap Deferred（以 DOM 内容/结构断言替代）。
- **不**做 i18n 完整性 — 归 A4.9（全域合并跑 checker）。
- **不**做 owner doc vs 代码 drift — 归 A3.5（已 done）。
- **不**在本计划内批量修复 view.xml drift — P1 经 R2.0 展开机制进入 MR2（view.xml 修复属代码变更）。本审计只识别 drift + 分类。
- **不**手改 view.xml 或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/purchase/state-machine.md` + `docs/design/sales/state-machine.md` + `docs/design/inventory/state-machine.md`（三域状态机/字段语义 owner doc——view.xml 枚举/dict 对照锚点）；`module-{purchase,sales,inventory}/erp-*-web/src/main/resources/_vfs/`（purchase 40 + sales 32 + inventory 42 view.xml——审计对象）；`module-{purchase,sales,inventory}/erp-*-service/` + `*-meta/`（后端 xbiz/XMeta 契约——真相源对照）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A4.7 指定此 skill——7 维度 + 严重性指南。项目定制化层见 `docs/skills/README.md`）。与 A4.6 同型结果表面（view.xml 契约 drift），方法经 A4.6 验证有效。A4.5 已建立稳定的后端契约真相源。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。view.xml drift 修复在 MR2 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为 view.xml 静态审查 + 后端契约交叉对照，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。view.xml 静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - pur+sal+inv view.xml vs 后端契约 7 维度 drift 系统性审计

Status: completed
Targets: purchase 40 view.xml + sales 32 view.xml + inventory 42 view.xml（`module-{purchase,sales,inventory}/erp-*-web/src/main/resources/_vfs/`）；后端真相源对照 `module-{purchase,sales,inventory}/erp-*-service/`（BizModel/xbiz）+ `*-meta/`（XMeta）+ `module-{purchase,sales,inventory}/model/app-erp-*.orm.xml`（ORM 字段/dict 绑定）；owner docs `docs/design/{purchase,sales,inventory}/state-machine.md`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA2 done（三域状态机 finding 作为输入）；MA3 done（API 契约 finding 作为输入）；A4.5 done（后端代码质量审计已建立稳定契约真相源）；A4.6 done（同型方法已验证）；前端 UI-roadmap done（通用层 page.yaml 问题已修复，本审计审业务页面 view.xml）。

- [x] 维度「字段名一致性」：核查 purchase 40 + sales 32 + inventory 42 view.xml grid/form 列引用的字段名 vs ORM/XMeta 实体字段——字段删除/重命名后 view.xml 未同步致页面报错/空白。重点关注采购单/收货/发票/付款 + 销售单/发货/收款 + 移动单/盘点/拣货/批次/序列号实体的字段集稳定性。标记悬挂字段引用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「BizMutation/BizQuery 动作名一致性」：核查 view.xml 按钮调用的自定义动作（submitForApproval/approve/reject/cancel/reverse/completeTake/confirm 等）vs BizModel/xbiz 声明的方法名——方法删除/重命名后按钮失效。重点关注采购/销售审批三段（PROC 大 Processor + INLINE）+ 库存 doConfirm/doComplete/reverse 动作集合。标记悬挂动作引用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「枚举值/状态值一致性」：核查 view.xml 状态映射/下拉绑定的枚举值 vs dict/常量——复核 MA2 三域状态机 finding（purchase P1-MA2-049/050/051 / sales P1-MA2-056/057 / inventory P1-MA2-062 StockTake DONE 不自动生成移动单 / P1-MA2-063 PickingOrder PICKING/PICKED dict 死状态）在 view.xml 状态映射层的投影——dict 死状态是否在 view.xml 映射为可见状态（误导用户）或映射为不可达状态（页面显示死选项）。标记枚举/dict drift。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「参数类型一致性」：核查 view.xml 传参类型 vs BizModel 方法签名——Long/String 混淆（复核 P1-MA2-050/057 INLINE 路径参数 adapt）+ 日期序列化（前端 UI-roadmap 已修复 12 日期参数报表，本审计复核 pur/sal/inv 业务页面的日期字段、DatePicker 序列化残留）。标记参数类型 drift。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「dict 绑定一致性」：核查 view.xml 下拉绑定的 dict 路径 vs 实际 dict yaml 存在性——复核三域状态 dict（purchase approve-status/doc-status / sales 同型 / inventory doc-status 经 D1 统一为 erp/doc-status + picking-status + stock-take-status）yaml 存在性。标记 dict 绑定缺失/路径错误。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「gen-control 内联脚本契约」：核查 view.xml `<gen-control>` 内联脚本引用的 API/字段 vs 后端契约——复核前端 UI-roadmap Phase 3 修复（inventory.write input-table tabs / adapt typo）在 pur/sal/inv 残留。标记内联脚本契约 drift（major/P2）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「跨实体字段引用」：核查 view.xml 引用关联实体的字段路径 vs ORM refEntityName 实际关系——refEntityName 重命名/关系变更后 view.xml 字段路径悬挂；复核 P1-MA1-022 跨域只读 daoFor 投影在 view 层（picker 快照注入字段）。标记跨实体字段引用 drift（P2）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 产出审计报告 `docs/audits/2026-07-29-0749-arm-ma4-pur-sal-inv-view-xml-drift.md`（含：7 维度逐项审查结果 / MA2/MA3/MA1/前端-roadmap 已知 finding view 层投影复核 / P0-P3 finding 清单按严重性排序 / 每项含 view.xml 文件路径+行引用 + 后端对照 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 7 维度逐项审查结果产出（每维度至少一句裁决，含"本维度无 drift"）
- [x] MA2 三域状态机 + MA3 API 契约 + MA1 跨域只读 + 前端-roadmap Phase 3 残留 view 层投影复核产出（每项标记"无 view 层投影"或"发现 view 层 drift"）
- [x] P0-P3 finding 清单产出按严重性排序，每个含 view.xml 文件路径+行引用+后端对照+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: completed
Targets: pur+sal+inv view.xml drift finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部缺陷 major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = A4.6 已分配最大 P1-MA4-N（023）+ 1 = P1-MA4-024，避免命名空间碰撞；报告、领域、缺陷描述、目标 MR2[view.xml 代码类]、修复状态 todo）。与 MA2/MA3/MA1/A4.1a-A4.6 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [x] 分类裁决：view.xml drift finding 目标 MR2（view.xml 修复属代码变更）；活跃数据破坏/页面完全不可用走 P0 即时通道（升级评估），在报告中明确标注。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「view.xml drift（MA4）」行 + arch-gov §残留风险 5 反映 pur+sal+inv 维度进度。
      - Skill: none

Exit Criteria:

- [x] 所有缺陷 major 已登记 arm-index §P1 汇总（view.xml 代码类 MR2），待展开
- [x] 与 MA2/MA3/MA1/A4.1a-A4.6 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix + arch-gov §残留风险 5 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_054da6f92ffeIy4ar9ciztd47Y`，独立 general 子代理 fresh-context，对照实时仓库逐项复核）——VERDICT: accept，无 BLOCKER。LIVE-REPO 复核全通过：view.xml 计数 purchase 40 + sales 32 + inventory 42 = 114 ✓；MA2 三域 finding（P1-MA2-049/050/051 purchase + 056/057 sales + 062/063 inventory）全存在且域归属正确 ✓；MA1 P1-MA1-022 跨域只读 ✓；skill `multi-dimensional-audit-prompt.md` 存在 ✓；引用 plan（A4.6 + A4.5）全存在 ✓；scope matrix §2.4 view.xml drift 行 + arch-gov §残留风险 5 存在 ✓；roadmap A4.7 行 status=todo + skill=multi-dimensional ✓；MA4 max P1=023 → 新 P1 起始 P1-MA4-024 正确 ✓。Guide 合规 R2/3/4/7/8/10/11/12 + anti-slack 全通过。2 项非阻塞 SUGGESTION（BUILD_VERIFY 门控可补 roadmap §其他纪律精确路径；"已 done"项 A4.6/A4.5 更适合 Non-Goals 而非 Deferred——保留可接受）。Plan Status 转 active。

## Closure Gates

> 本计划主体是 view.xml 静态审查 + 后端契约交叉对照（不改代码；产出为审计报告 + arm-index/scope-matrix/arch-gov 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。view.xml drift 修复在 MR2 批量进行。本审计只识别 drift + 分类。

- [x] 范围内行为完成（A4.7 pur+sal+inv view.xml drift 审计报告产出 + arm-index 更新 + scope matrix/arch-gov 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、arch-gov §残留风险 5 结论已反映）
- [x] 已运行验证：view.xml 静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### crm+hr view.xml drift（A4.8）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审 pur+sal+inv（A 级第二批）；crm+hr（view.xml 数 68+72）归 A4.8（crm A 级 + hr S 级批次）。
- Successor Required: `yes`——A4.8 执行时复核 crm/hr view。

### i18n 完整性（A4.9）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: view.xml i18n 标签完整性归 A4.9（全域合并跑 checker）。本审计审契约 drift（字段/动作/枚举/参数），非 i18n 覆盖。
- Successor Required: `yes`——A4.9 执行时复核全域 i18n。

### finance+mfg view.xml drift（A4.6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审 pur+sal+inv；finance+mfg 归 A4.6（已 done）。
- Successor Required: `no`——A4.6 已 done。

### 后端代码实现质量（A4.5）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计以后端契约为真相源审前端 view.xml 漂移方向；后端代码实现质量归 A4.5（pur+sal+inv+qa+crm，已 done）。
- Successor Required: `no`——A4.5 已 done。

## Closure

Status Note: A4.7 pur+sal+inv view.xml vs 后端契约 drift 审计（A 级第二批，114 view.xml）已完成。Verdict FAIL（零 P0）：1 P1（P1-MA4-024 purchase ErpPurRfq「作废」按钮 cancel 参数名 `id` vs BizModel `rfqId` 不匹配——作废按钮功能性失效，P1-MA3-047 的 view 层具象）+ 3 P2 watch-only（P2-MA4-016 pur paid/receive badge 跨域调色板 / P2-MA4-017 inv `ACTIVE` 死状态 badge StockMove+LandedCost[同 A4.6 P2-MA4-014 根因] / P2-MA4-018 inv `kilometer` 内联脚本拼写）。7 维度审计：字段名/动作名/枚举值/dict 绑定/跨实体字段 5 维度 PASS（动作名/参数绑定唯一例外为 Rfq cancel），drift 集中在参数绑定（维度 4，1 项 P1）与 gen-control 内联脚本（维度 6，3 项 P2）。**sales 域零 drift（7 维度全 PASS）**。MA2 三域状态机（P1-MA2-049/050/051/056/057/062/063）+ MA3 API 契约（P1-MA3-047/048）+ MA1 跨域只读（P1-MA1-022）+ 前端-roadmap Phase 3 残留 view 层投影复核全部「无 view 层 drift」或「无投影」（唯 P1-MA3-047 投影为 P1-MA4-024）。drift 密度 0.88%（1 P1 / 114 view.xml，与 A4.6 的 0.75% 同量级）——delta 层 `bounded-merge` 自愈 + xbiz/Processor 正式接线 + ORM `ext:dict` 绑定三道防线有效。**inventory 域虽为 MA2 死状态最高风险域，但 view 层未放大死状态风险**（PickingOrder view 零状态守卫不误导用户 / StockTake completeTake 动作名正确解析）。审计报告 + arm-index（P1-MA4-024 + P2-MA4-016/017/018 登记，编号无碰撞——前序最大 P1-MA4=023/P2-MA4=015）+ scope matrix §2.4 + arch-gov §残留风险 5（部分覆盖）+ roadmap A4.7（todo→done）全部已更新。验证：Phase 1 + Phase 2 各运行 `mvn test` BUILD SUCCESS（全绿回归基线确认，3536 测试 0 failures 0 errors；审计无代码变更，`git status` 仅 .md 文件）。view.xml drift 修复在 MR2 批量进行。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理 fresh-context（ses_054a78177ffe0ib04I2TGChSAj，2026-07-29，未参与本计划执行）。
- Evidence: CLOSURE_AUDIT: pass——8 项核验中 7 项 PASS（审计报告完整[7 维度裁决 + P0-P3 finding 表 + 11 项已知 finding view 层投影复核 + Verdict + Audit Status: closed] / 关键 finding P1-MA4-024 实仓可复现[ErpPurRfq.view.xml:92 传 `id` + BizModel.java:22 期望 `rfqId` + 零 xbiz cancel adapt + 其余 7 域命名参数匹配唯 Rfq 漂移] / P2-MA4-017+018 实仓可复现[StockMove badge `ACTIVE` + move-status 无 ACTIVE / StockMoveLine `kilometer` 拼写] / arm-index 登记[报告行 done + P1-MA4-024 紧随 P1-MA4-023 + P2-MA4-016/017/018 紧随 P2-MA4-015 + A4.7 narrative；编号无碰撞] / scope matrix §2.4 A4.7 注记 + arch-gov §残留风险 5 已反映 A4.7 done / roadmap A4.7 done / git status 仅 .md 变更零代码改动）。1 项 BLOCKER（计划自身 closure 仪式未完成——Plan Status: active + 8 Closure Gates 未勾 + Closure 占位符未填）已由执行者按审计要求补齐：Plan Status→completed + 8 Gates [x] + Closure 节填充本证据。2 项非阻塞 cosmetic（arm-index narrative "待独立 closure audit" 措辞过时 + A4.6/A4.8/A4.9 同变更集为先前会话遗留 .md 非本次范围）已认知。验证 Phase 1 + Phase 2 各 `mvn test` BUILD SUCCESS（executor 执行，全绿基线）。

Follow-up:

- 无非阻塞跟进项目（view.xml drift 修复按设计进入 MR2 批量进行，A4.8 view.xml drift 后续批次已在 Deferred But Adjudicated 登记且 successor required）。
