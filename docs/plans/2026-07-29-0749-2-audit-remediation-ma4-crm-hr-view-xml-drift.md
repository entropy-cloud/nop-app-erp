# 2026-07-29-0749-2-audit-remediation-ma4-crm-hr-view-xml-drift MA4 crm+hr view.xml vs 后端契约 drift 审计（A4.8）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A4.8（crm+hr view.xml vs 后端契约 drift，crm A 级 + hr S 级 view.xml drift 第三批）
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.8，crm+hr view.xml vs 后端契约 drift）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行 + §残留风险 5「未覆盖：AMIS view.xml drift」；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法）；`docs/design/human-resource/state-machine.md` + `docs/design/crm/state-machine.md`（owner doc 状态机对照锚点）；`docs/plans/2026-07-29-0430-3-audit-remediation-ma4-finance-mfg-view-xml-drift.md`（A4.6，view.xml drift 第一批 S 级——本计划紧随其后同型方法）；`docs/plans/2026-07-29-0430-1-audit-remediation-ma4-hr-code-quality.md`（A4.4，hr 后端代码质量——本审计的前端契约对照基线）+ `docs/plans/2026-07-29-0430-2-audit-remediation-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5，crm 后端代码质量——本审计的前端契约对照基线）
> Audit: required

## Current Baseline

crm + hr 两域 view.xml vs 后端契约 drift 审计（代码与前端质量层 MA4，view.xml drift 维度第三批 crm A 级 + hr S 级）。roadmap 工作项 A4.8 声明审查"crm+hr view.xml vs 后端契约 drift"，owner doc 标注各域 view.xml，skill `docs/skills/multi-dimensional-audit-prompt.md`。A4.6（finance+mfg S 级第一批，134 view.xml）已完成并经独立 closure audit PASS——drift 密度 0.75%，drift 集中在枚举值 + gen-control 内联脚本 2 维度。

**关键基线事实（实时仓库核实）**：

- **两域 view.xml 规模**（实时仓库核实）：`find module-<domain> -name "*.view.xml" -not -path "*/target/*"`：crm **68 view.xml** / hr **72 view.xml** = **合计 140 view.xml**。两域 view.xml 数量高（hr 72 与 finance 72 持平为全域最高；crm 68 次之），drift 风险面大。view.xml 是 AMIS 前端页面定义，调用后端 GraphQL API（字段名 / BizMutation 动作名 / 枚举值 / dict 绑定 / 参数类型），与后端 xbiz/xbiz-meta/BizModel 契约的一致性是前端可用性的基础。
- **drift 维度**（`multi-dimensional-audit-prompt.md` 7 维度适配"view.xml vs 后端契约"主题——经 A4.6 验证有效）：(1) **字段名一致性**；(2) **BizMutation/BizQuery 动作名一致性**；(3) **枚举值/状态值一致性**——重点复核 MA2 hr 死状态密集 finding view 层投影；(4) **参数类型一致性**；(5) **dict 绑定一致性**；(6) **gen-control 内联脚本契约**；(7) **跨实体字段引用**。
- **MA2 已审计的 hr 状态机 finding（view.xml drift 审计输入——hr 死状态密集，view 层投影是本批次重点）**：
  - A2.7a hr 员工与组织（`docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`）：P1-MA2-039 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + 离职/终止/退休/转正迁移完全未实现 / P1-MA2-040 / P1-MA2-041 员工调查 OPEN/CLOSED/ARCHIVED 三态死状态 + SurveyBizModel CRUD 桩 / P1-MA2-042。
  - A2.7b hr 考勤与工资（`docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`）：P1-MA2-043 工时单 APPROVED/REJECTED dict 死状态 + TimesheetBizModel 仅 submit / P1-MA2-044 / P1-MA2-045 银行付款文件 UPLOADED/CONFIRMED dict 死状态 + PayrollBankFileBizModel CRUD 桩 / P1-MA2-046 排班分配无 dict 绑定 / P1-MA2-047 salary posted 死字段 / P1-MA2-048 SalaryPostingDispatcher javadoc drift + posted 死字段。
  - 这些 hr 死状态（RESIGNED/TERMINATED/RETIRED/OPEN/CLOSED/ARCHIVED/APPROVED/REJECTED/UPLOADED/CONFIRMED）在 view.xml 状态映射层（员工状态 badge、调查/工时单状态筛选下拉、银行文件生命周期页面）的投影是本批次最高优先级复核项——dict 死状态是否在 view.xml 映射为可见状态（误导用户）或映射为不可达状态（页面显示死选项）。
- **MA2 已审计的 crm 状态机 finding（view.xml drift 审计输入）**：
  - A2.14 crm+cs+contract+b2b+maintenance（`docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`，crm 部分）：P1-MA2-076 crm Event reminderMinutesBefore 字段死字段（silent functional gap——findDueReminders 用全局 60 分钟窗口从不读 per-event reminderMinutesBefore）。
- **MA3 已审计的 API 契约 finding（view.xml drift 审计输入）**：A3.6 API 契约一致性（P1-MA3-047 API 命名/参数跨域不一致 / P1-MA3-048 孤儿 Processor bean 携带 String 影子契约）——view.xml 调用的动作名/参数类型 vs 这些 API 契约 finding 的交叉复核。
- **后端代码质量审计（A4.4 hr + A4.5 crm）已完成**：后端 BizModel/xbiz 契约是稳定的对照基线。本审计以后端契约为真相源，审查前端 view.xml 的漂移方向。
- **已知前端问题历史**：frontend-ui-roadmap（`2026-07-23-1408-3`）Phase 2/3 修复 notify-inbox 裸变量 / AMIS ErpMdPartner 非法 GraphQL + adapt typo；多份 E2E plan 修复 page.yaml `$var` 模板 + reportContainer 接线 + 报表下载 URL + 日期参数 valueFormat。本审计系统性审查 crm/hr 业务页面（非报表/看板）的 view.xml vs 后端契约 drift。

**审计张力**：A4.6（S 级第一批）已证明三道防线有效、drift 密度低。但 crm+hr 从未做过系统性 view.xml vs 后端契约 drift 审计——arch-gov §残留风险 5「未覆盖：AMIS view.xml drift」明确标注此维度进入 MA4。**hr 死状态密集（P1-MA2-039~048 十项）+ view.xml 数 72（全域最高）**，其状态映射/下拉绑定直接决定员工/调查/工时单/银行文件/薪酬页面的可用性与正确性——A4.6 注记明确指出"A4.8 需重点复核 hr view 层投影"。crm A 级 68 view.xml 涵盖线索/商机/活动/事件（含 reminderMinutesBefore 死字段 P1-MA2-076）。本审计是该维度的第三批（最后一批 view.xml drift），完成后 view.xml drift 维度（MA4）全域收口。

剩余差距：需要一次 crm+hr view.xml vs 后端契约 7 维度 drift 系统性审计。发现的缺陷分类为：(a) **字段名 drift**（major）；(b) **动作名 drift**（major）；(c) **枚举/dict drift**（major——重点复核 hr 十项死状态 view 层投影 + crm reminderMinutesBefore 死字段）；(d) **参数类型 drift**（major）；(e) **内联脚本契约 drift**（major/P2）；(f) **跨实体字段引用 drift**（P2）。major 登记为 P1（代码类目标 MR2——view.xml 修复属代码变更）。view.xml 修改不触及 ORM 保护区域，但触及用户可见前端行为。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 7 维度（适配"view.xml vs 后端契约 drift"主题，经 A4.6 验证有效）对 crm 68 view.xml + hr 72 view.xml 做系统性前端-后端契约一致性审计，产出审计报告。
- 审计覆盖 7 drift 维度：字段名一致性 / BizMutation 动作名一致性 / 枚举值状态值一致性 / 参数类型一致性 / dict 绑定一致性 / gen-control 内联脚本契约 / 跨实体字段引用。
- 复核 MA2 hr 状态机死状态密集 finding（P1-MA2-039~048）+ crm finding（P1-MA2-076）+ MA3 API 契约 finding（P1-MA3-047/048）+ 前端 UI-roadmap Phase 3 残留在 view.xml 层的投影，标记是否有 MA2/MA3/前端-roadmap 未发现的 view.xml 层 drift。**hr view 层投影复核为本批次最高优先级**。
- scope matrix §2.4「view.xml drift（MA4）」行 + arch-gov §残留风险 5 推进至完成（crm+hr 维度，view.xml drift 维度全域收口）。
- 发现的 major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（起始编号 = A4.7 已分配最大 P1-MA4-N + 1，避免命名空间碰撞）。roadmap A4.8 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做 finance+mfg view.xml drift — 归 A4.6（S 级第一批，已 done）。
- **不**做 pur+sal+inv view.xml drift — 归 A4.7（A 级第二批）。
- **不**做后端 BizModel/xbiz 代码实现质量 — 归 A4.4（hr，已 done）+ A4.5（crm，已 done）。本审计以后端契约为真相源审前端 view.xml 漂移方向。
- **不**做报表/看板 page.yaml 渲染层问题 — 前端 UI-roadmap + 多份 E2E plan 已修复。本审计审业务页面 view.xml（grid/form/page 结构），非报表 page.yaml。
- **不**做像素级视觉回归 — 归前端 UI-roadmap Deferred（以 DOM 内容/结构断言替代）。
- **不**做 i18n 完整性 — 归 A4.9（全域合并跑 checker）。
- **不**做 owner doc vs 代码 drift — 归 A3.3/A3.4（已 done）。
- **不**在本计划内批量修复 view.xml drift — P1 经 R2.0 展开机制进入 MR2（view.xml 修复属代码变更）。本审计只识别 drift + 分类。
- **不**手改 view.xml 或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/human-resource/state-machine.md` + `docs/design/crm/state-machine.md`（两域状态机/字段语义 owner doc——view.xml 枚举/dict 对照锚点）；`module-{crm,hr}/erp-*-web/src/main/resources/_vfs/`（crm 68 + hr 72 view.xml——审计对象）；`module-{crm,hr}/erp-*-service/` + `*-meta/`（后端 xbiz/XMeta 契约——真相源对照）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A4.8 指定此 skill——7 维度 + 严重性指南。项目定制化层见 `docs/skills/README.md`）。与 A4.6/A4.7 同型结果表面（view.xml 契约 drift），方法经 A4.6 验证有效。A4.4（hr）+ A4.5（crm）已建立稳定的后端契约真相源。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。view.xml drift 修复在 MR2 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为 view.xml 静态审查 + 后端契约交叉对照，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。view.xml 静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - crm+hr view.xml vs 后端契约 7 维度 drift 系统性审计

Status: planned
Targets: crm 68 view.xml + hr 72 view.xml（`module-{crm,hr}/erp-*-web/src/main/resources/_vfs/`）；后端真相源对照 `module-{crm,hr}/erp-*-service/`（BizModel/xbiz）+ `*-meta/`（XMeta）+ `module-{crm,hr}/model/app-erp-*.orm.xml`（ORM 字段/dict 绑定）；owner docs `docs/design/{human-resource,crm}/state-machine.md`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA2 done（hr/crm 状态机 finding 作为输入）；MA3 done（API 契约 finding 作为输入）；A4.4（hr）+ A4.5（crm）done（后端代码质量审计已建立稳定契约真相源）；A4.6 done（同型方法已验证）；前端 UI-roadmap done（通用层 page.yaml 问题已修复，本审计审业务页面 view.xml）。

- [ ] 维度「字段名一致性」：核查 crm 68 + hr 72 view.xml grid/form 列引用的字段名 vs ORM/XMeta 实体字段——字段删除/重命名后 view.xml 未同步致页面报错/空白。重点关注 hr 员工/合同/考勤/工资/调查/银行文件 + crm 线索/商机/活动/事件/合同实体的字段集稳定性（含 crm reminderMinutesBefore 死字段[P1-MA2-076]在 view 层是否暴露）。标记悬挂字段引用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「BizMutation/BizQuery 动作名一致性」：核查 view.xml 按钮调用的自定义动作（hire/transferEmployee/submit/submitForApproval/approve/reject/generateBankFile/findDueReminders 等）vs BizModel/xbiz 声明的方法名——方法删除/重命名后按钮失效。重点关注 hr 招聘 hire + 调岗 transferEmployee + 工资核算 generateBankFile + crm 线索评分/预测聚合动作集合。标记悬挂动作引用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「枚举值/状态值一致性」：核查 view.xml 状态映射/下拉绑定的枚举值 vs dict/常量——**重点复核 MA2 hr 十项死状态 view 层投影**（P1-MA2-039 员工 RESIGNED/TERMINATED/RETIRED / P1-MA2-041 调查 OPEN/CLOSED/ARCHIVED / P1-MA2-043 工时单 APPROVED/REJECTED / P1-MA2-045 银行文件 UPLOADED/CONFIRMED / P1-MA2-046 排班无 dict 绑定）+ crm（P1-MA2-076 Event reminderMinutesBefore 死字段）。dict 死状态是否在 view.xml 映射为可见状态（误导用户）或映射为不可达状态（页面显示死选项）。标记枚举/dict drift。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「参数类型一致性」：核查 view.xml 传参类型 vs BizModel 方法签名——Long/String 混淆 + 日期序列化（前端 UI-roadmap 已修复 12 日期参数报表，本审计复核 crm/hr 业务页面的日期字段、DatePicker 序列化残留）。标记参数类型 drift。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「dict 绑定一致性」：核查 view.xml 下拉绑定的 dict 路径 vs 实际 dict yaml 存在性——复核 P1-MA2-046 排班分配无 dict 绑定同型在 crm/hr view 层投影（status 字段无 ext:dict 致 UI 无法枚举合法值）。标记 dict 绑定缺失/路径错误。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「gen-control 内联脚本契约」：核查 view.xml `<gen-control>` 内联脚本引用的 API/字段 vs 后端契约——复核前端 UI-roadmap Phase 3 修复（AMIS ErpMdPartner 非法 GraphQL + adapt typo）在 crm/hr 残留。标记内联脚本契约 drift（major/P2）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 维度「跨实体字段引用」：核查 view.xml 引用关联实体的字段路径 vs ORM refEntityName 实际关系——refEntityName 重命名/关系变更后 view.xml 字段路径悬挂；复核 crm/hr 跨实体 picker 快照注入字段。标记跨实体字段引用 drift（P2）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [ ] 产出审计报告 `docs/audits/2026-07-29-0749-arm-ma4-crm-hr-view-xml-drift.md`（含：7 维度逐项审查结果 / MA2 hr 死状态密集 + crm finding + MA3 API 契约 + 前端-roadmap 已知 finding view 层投影复核 / P0-P3 finding 清单按严重性排序 / 每项含 view.xml 文件路径+行引用 + 后端对照 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 7 维度逐项审查结果产出（每维度至少一句裁决，含"本维度无 drift"）
- [ ] MA2 hr 十项死状态 + crm reminderMinutesBefore + MA3 API 契约 + 前端-roadmap Phase 3 残留 view 层投影复核产出（每项标记"无 view 层投影"或"发现 view 层 drift"）
- [ ] P0-P3 finding 清单产出按严重性排序，每个含 view.xml 文件路径+行引用+后端对照+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: planned
Targets: crm+hr view.xml drift finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「view.xml drift（MA4）」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] finding 汇总：全部缺陷 major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = A4.7 已分配最大 P1-MA4-N + 1，避免命名空间碰撞；报告、领域、缺陷描述、目标 MR2[view.xml 代码类]、修复状态 todo）。与 MA2/MA3/A4.1a-A4.7 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [ ] 分类裁决：view.xml drift finding 目标 MR2（view.xml 修复属代码变更）；活跃数据破坏/页面完全不可用走 P0 即时通道（升级评估），在报告中明确标注。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「view.xml drift（MA4）」行（标记 view.xml drift 维度全域收口）+ arch-gov §残留风险 5 反映 crm+hr 维度进度（view.xml drift 维度全域完成）。
      - Skill: none

Exit Criteria:

- [ ] 所有缺陷 major 已登记 arm-index §P1 汇总（view.xml 代码类 MR2），待展开
- [ ] 与 MA2/MA3/A4.1a-A4.7 已登记 P1 经交叉去重无重复登记
- [ ] arm-index 报告清单 + scope matrix + arch-gov §残留风险 5 已反映审计结论（view.xml drift 维度全域收口）

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（`ses_054da5062ffeNRzkzDcQnGVHqh`，独立 general 子代理 fresh-context）——VERDICT: needs-revision，1 项 BLOCKER：维度 1（字段名一致性）将 `reminderMinutesBefore` 误标为 hr 字段——它是 **crm** ErpCrmEvent 字段（P1-MA2-076），与维度 3 自相矛盾。LIVE-REPO 复核其余全通过：crm 68 + hr 72 = 140 ✓；MA2 hr 死状态密集 P1-MA2-039~048 + crm P1-MA2-076 全存在且域归属正确 ✓；skill/引用 plan/scope matrix/roadmap A4.8 行 全 ✓；Guide 合规全通过。2 项非阻塞 SUGGESTION（roadmap A4.8 行 view.xml 数 "34+36" 笔误待刷新；Closure Gate 验证措辞可对齐 A4.6）。修订：维度 1 "hr reminderMinutesBefore" → "crm reminderMinutesBefore[P1-MA2-076]"。
- Independent draft review iteration 2: **accept**（`ses_054d5b87fffenCujNd7hN4SLTF`，独立 general 子代理 fresh-context）——VERDICT: accept，BLOCKER 已解决。复核：维度 1 与维度 3 现均归属 crm，一致；grep 全文 reminderMinutesBefore 无残留 hr 归属 ✓；crm 68 / hr 72 计数 + P1-MA2-076 域=crm 独立复核通过 ✓；无新问题；Guide R2/3/4/7/8/12 + anti-slack 全通过。Plan Status 转 active。

## Closure Gates

> 本计划主体是 view.xml 静态审查 + 后端契约交叉对照（不改代码；产出为审计报告 + arm-index/scope-matrix/arch-gov 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。view.xml drift 修复在 MR2 批量进行。本审计只识别 drift + 分类。

- [ ] 范围内行为完成（A4.8 crm+hr view.xml drift 审计报告产出 + arm-index 更新 + scope matrix/arch-gov 标记完成，view.xml drift 维度全域收口）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、arch-gov §残留风险 5 结论已反映）
- [ ] 已运行验证：view.xml 静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### i18n 完整性（A4.9）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: view.xml i18n 标签完整性归 A4.9（全域合并跑 checker）。本审计审契约 drift（字段/动作/枚举/参数），非 i18n 覆盖。
- Successor Required: `yes`——A4.9 执行时复核全域 i18n。

### finance+mfg view.xml drift（A4.6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审 crm+hr；finance+mfg 归 A4.6（已 done）。
- Successor Required: `no`——A4.6 已 done。

### pur+sal+inv view.xml drift（A4.7）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审 crm+hr；pur+sal+inv 归 A4.7。
- Successor Required: `yes`——A4.7 执行时复核 pur/sal/inv view。

### 后端代码实现质量（A4.4 hr + A4.5 crm）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计以后端契约为真相源审前端 view.xml 漂移方向；后端代码实现质量归 A4.4（hr，已 done）+ A4.5（crm，已 done）。
- Successor Required: `no`——A4.4 + A4.5 已 done。

## Closure

Status Note: <待执行与独立结束审计填充>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填充>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 无非阻塞跟进项目（view.xml drift 修复按设计进入 MR2 批量进行；本批次完成后 view.xml drift 维度[MA4]全域收口）。
