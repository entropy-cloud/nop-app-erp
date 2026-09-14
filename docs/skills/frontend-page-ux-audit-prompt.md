# 前端业务页面配置审计提示（Frontend Business Page Config Audit）

> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` auto + dual-agent-approval、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。

对**典型复杂页面 / 页面批量**做**业务配置层**审计时使用此提示。审计范围是 flux 层无法统一配置、必须按业务语义逐页决策的内容：字段布局与分组、tabs/wizard/单表单形式选择、控件选择、表格列数/列序/列宽、按钮分组、控件联动、参数收集、反馈闭环。**通用样式/CSS/主题/组件渲染/平台运行时约定不属于本技能**——那些由 flux 统一负责，其审查用 `flux-rendering-conformance-audit-prompt.md`。

本技能把 `docs/analysis/2026-09-14-complex-page-ux-review.md` 中确认的 P1/P2/P3 问题与通用交互设计最佳实践固化为可被独立子代理照做的 grep 程式 + 判定阈值 + 反模式自检表。**审查导向——产出发现文档，不引入代码变更**（修复走专门 plan）。

**使用场景**：
- 新增/重构复杂页面后的业务配置自查（字段分组、tabs vs wizard、子表编辑、参数 dialog、行级业务动作）
- 对一批页面做业务配置一致性审计（如 5+ 页批量扫描、某域全量页面）
- 页面级 closure 前的业务交互维度（与 `code-quality-audit-prompt.md` 互补：后者审 Java 行为质量）

**不使用场景**：
- CSS/主题/样式/组件渲染/平台运行时约定（adaptor 作用域、空串序列化、列虚拟化机制）→ 用 `flux-rendering-conformance-audit-prompt.md`
- 页面资产盘点/设计-实现映射 → 用 `docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md` 既有结论，不重复盘点
- Java BizModel / 后端服务审查 → 用 `nop-backend-dev` / `code-quality-audit-prompt.md`；本技能只以 BizModel 签名佐证前端 action 参数是否被错误硬编码
- 平台合规（@Inject private / 异常 extends NopException）→ 用 `nop-platform-conformance-audit-prompt.md`
- 需求→实现符合性 → 用 `requirement-compliance-audit-prompt.md`

**必需输入**：
- 目标页面集合（view.xml 路径列表；批量扫描时为某域 `module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/`）
- 相关页面 `_gen/_*.view.xml`（判断默认 actionType/尺寸基线用）
- 相关 `ref-*.page.yaml`、`*.page.yaml`（关联 drawer 与页面结构内容）
- 被调用的 BizModel/Processor 方法签名（佐证硬编码参数是否被后端容忍/吞掉）
- 该域 ORM 模型或 XMeta（核对 label/字典/精度与前端一致性）
- 业务复杂度参照：`docs/design/flux-complex-pages.md`（wizard/tabs/子表映射表）
- 既有 UX 审计基线（如 `docs/analysis/2026-09-14-complex-page-ux-review.md`，避免重复已记录问题）

**预期输出**：
按 P1（确认缺陷）/ P2（一致性问题）/ P3（体验优化）分级的 finding 清单，每个 finding 含：问题类别、控制点（`file:line` 或 `file#actionId`）、量化证据（列数/按钮数/字段数/尺寸）、判定阈值命中情况、修复建议（复用既有范式时给出范式来源页）。最终裁决 `passes UX audit` 或 `needs revision`。

```text
您是高级 ERP 前端架构师和 Nop XView 业务交互专家。对目标页面集合做 10 维**业务配置**审计：信息架构与导航 / 字段布局与分组 / 弹框与页面容器 / 控件选择 / 控件联动与数据流 / 表单配置 / 表格配置 / 反馈与状态 / 按钮与操作组织 / 国际化文案。

范围纪律：只审「flux 层无法统一配置、必须按业务语义决策」的内容。CSS/主题/组件渲染/平台运行时约定一律不审（那是 flux-rendering-conformance-audit 的范围），避免与本技能重叠。

首先阅读这些文件：
- `AGENTS.md`
- `docs/skills/README.md §项目定制化层` 与 `§已知失败模式`
- `docs/analysis/2026-09-14-complex-page-ux-review.md`（首轮审计证据与既有 P1-P3 清单，避免重复报告）
- `docs/design/flux-complex-pages.md`（wizard/tabs/子表/flux 控件映射总表——判断形式选择与控件选择的业务依据）
- `docs/design/visible-on-patterns.md`、`docs/design/child-table-editor-patterns.md`（域级交互范式）
- 目标域 ORM 模型 / XMeta（label/字典/精度核对源）

审计对象 = view.xml 保留层文件（`*_gen*` 除外）+ *.page.yaml + 被调 BizModel 签名。**不是** Java 行为质量审计（用 code-quality-audit），**不是** flux 渲染合规审计（用 flux-rendering-conformance-audit）。

## 0. 量化基线采集（每页必做）

```
# 列表列数（>13 触发 AMIS 列虚拟化告警——列数决策的业务依据）
sed -n '/<grid id="list">/,/<\/grid>/p' <file> | grep -c '<col id='
# 列宽/列对齐配置（列宽决策核查）
grep -c 'width=' <file>; grep -c 'align=' <file>
# 行动作总数（>10 触发按钮过载告警）
grep -c '<action id=' <file>
# 表单形式（tabs/wizard/单表单）
grep -c 'layoutControl="tabs"' <file>
grep -rn 'type: wizard\|<wizard' <dir>
# 编辑/查看动作的容器类型与尺寸
grep -B1 -A3 'row-update-button\|row-view-button' <file>
grep -o '<drawer[^>]*size="[a-z]*"\|<dialog[^>]*size="[a-z]*"' <file> | sort | uniq -c
# 硬编码/空参数 mutation（P1 高发）
grep -rn 'url="@mutation[^"]*=[0-9][0-9]*"\|url="@mutation[^"]*=""' <dir>
# 联动实现面
grep -c 'onEvent\|visibleOn\|clearValueOnHidden\|disabledOn' <file>
# 反馈覆盖（confirmText / messages）
grep -c 'confirmText\|<messages>' <file>
# 查询入口（双查询入口冗余核查）
grep -c 'asideFilter\|editMode="query"' <file>
# 控件类型分布（控件选择核查）
grep -o "type: '[a-z-]*'\|<input-[a-z]*\|<select\|<picker\|<tree-select\|<checkbox\|<switch\|<textarea\|<button-group-select\|<input-table" <file> | sort | uniq -c
```

## §1 信息架构与导航（NA）

| # | 检查项 | 判定阈值 | 检查方法 / 证据页示例 |
|---|--------|----------|----------------------|
| NA1 | 查询入口冗余 | 同一页同时存在 `asideFilter` + `query` 双查询表单 → P3（入口分散，用户困惑；保留主查询，次查询折叠） | 全库 43 页有 asideFilter，多数同时有 query |
| NA2 | 跨页跳转上下文保留 | link 跳转目标页是否带 filter 预填（`?filter_xxx=`）→ 不带 → P3（用户到目标页后需重新筛选） | 正例：ErpPurOrder.view.xml:228-232（创建入库单带 `filter_orderId`） |
| NA3 | 页面标题与菜单一致性 | 页面/弹框标题与菜单名、实体名不一致 → P3（导航迷失） | 对照 `docs/analysis/2026-06-23-0003-menu-and-feature-completeness.md` |
| NA4 | 弹框标题语义 | 弹框标题未含操作对象（如"编辑"而非"编辑采购订单 #PO-xxx"）→ P3（多开弹框时无法区分） | — |

## §2 字段布局与分组（L）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| L1 | **表单形式选择**：单表单 / tabs / wizard | 线性多步流程且每步有校验闸 → 应 wizard（`docs/design/flux-complex-pages.md §4.5`）；并行字段组 > 2 组 → tabs；≤ 12 字段单屏可容 → 单表单。选错 → P2（如把线性审批流程做成 tabs，或把独立字段组硬塞单表单） | 正例：disposal-wizard（3 步）、lead-conversion；反例：§L2 |
| L2 | tabs 表单 × drawer 容器 | `layoutControl="tabs"` 且经 `row-update-button actionType="drawer"` 打开 → P2（弹框+tab 双重折叠；高频录入页考虑头部+子表同屏或改 wizard） | ErpPurOrder、ErpMfgWorkOrder、ErpFinVoucher |
| L3 | 表单分组语义与顺序 | 字段未按业务语义分组（基本信息/金额/明细/审计）或主键字段不在首组 → P3；审计字段（createdBy/createTime）应折叠区 `^` 而非主区 | 对照 ErpPurOrder form 分组（baseInfo/amount/lines/approval/audit） |
| L4 | 长文本/备注跨列 | 长字段（description/remark）未标 `(2)` 跨列 → P3（单列窄输入，录入体验差） | 正例：ErpCsTicket description(2) |
| L5 | 标签与模型一致性 | 表单/列表 label 与 ORM 注释、字典语义不一致 → P3（术语漂移） | 对照 `docs/design/domain-design-guidelines.md` 命名约定 |
| L6 | 编辑态表单暴露状态机/派生字段 | `status`/`docStatus`/`approveStatus`/`paidStatus`/`receiveStatus` 出现在 edit/add 表单且未 `readOnly` → P2（与行级动作语义冲突） | ErpPurOrder.view.xml:123-124、ErpCsTicket.view.xml:114-115 |
| L7 | 子表编辑形态选择 | 行内编辑（sub-grid-edit）vs 弹框编辑 vs 独立页——列数 > 12 或含复杂联动时行内编辑挤压 → 评估改弹框行编辑；简单行（< 6 列）却用独立页 → 过重 → P3 | ErpFinVoucherLine 17 列、ErpPurOrderLine 11 列 |

## §3 弹框与页面容器选择（D）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| D1 | 容器形式选择：dialog / drawer / 独立页 | 只读查看/参数收集 ≤ 6 字段 → dialog；含子表或 tabs 的编辑/档案 → drawer（宽）或独立页；多层嵌套（dialog 内 dialog > 1 层）→ P2（应拆页） | 正例：ErpHrEmployee 完整档案 drawer（5-tab） |
| D2 | 编辑容器显式尺寸 | `row-update-button actionType="drawer"` 无 `<drawer size>` → P2（表单 `size="lg"` 语义与容器默认宽度不匹配）；业务单据类 ≥ `xl`、纯表单 `lg` | 全库 68 处 |
| D3 | 内容-尺寸匹配 | 字段数 1 用 sm / 2-5 用 md / 6-10 用 lg / >10 或含子表用 xl（缺省默认 md）→ 偏差 P3 | ErpCsTicket escalateQuality md 5 字段、adoptKnowledge sm 1 字段（良好） |
| D4 | 参数收集动作 | 应收集参数的业务动作直接 `@mutation`（无 dialog/simple 页）→ P2；正确范式对照 ErpFinBudgetScenario rollForward + ErpCsTicket escalateQuality | — |
| D5 | 关闭行为与脏数据 | 长表单弹框 ESC/遮罩关闭无未保存确认 → P3（误关丢数据） | — |
| D6 | 弹框加载与提交反馈 | 弹框打开/提交无 loading 态、提交按钮可重复点击 → P3（防重复提交） | — |

## §4 控件选择（W）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| W1 | 实体选择控件 | 选实体却用 `input-number`/`input-text` 手输 ID（无 picker）→ P3 | ErpCsTicket escalateQuality materialId（ErpCsTicket.view.xml:326-330） |
| W2 | 层级/树形数据控件 | 树形主数据（科目/组织/物料分类）用平铺 select 而非 `tree-select` → P3（层级信息丢失） | 对照 `docs/design/flux-complex-pages.md §3.6` 树形 4 页 |
| W3 | 互斥少量选项 | 2-4 个互斥选项用 select 而非 `button-group-select` → P3（多一次点击） | 正例：ErpFinVoucherLine dcDirection 借贷切换 |
| W4 | 布尔/开关语义 | 状态启停用 `checkbox` 而非 `switch`、或 switch 无二次确认（停用类）→ P3/P2 | 正例：ErpMdMaterial status switch + 停用确认 |
| W5 | 日期范围 | 起止日期用两个独立 date 而非 range → P3（查询条件） | 对照 query form `filterOp="date-between"` |
| W6 | 长文本 | 长描述用 `input-text` 而非 `textarea` + 跨列 → P3 | 正例：ErpCsTicket defectDescription textarea |
| W7 | 数值控件精度 | 金额/数量未用 `input-number` 或 `step`/`precision` 与 ORM scale 不一致 → P2（精度截断/多余小数） | 对照 `docs/context/conventions.md` 精度约定 |
| W8 | 条件字段呈现方式 | 条件字段用 disable 而非 `visibleOn`（或反之该显隐却常显）→ P3（表单噪音） | 对照 `docs/design/visible-on-patterns.md` |
| W9 | 枚举多选 | 少量多选用 `checkbox-group`、大量多选用 multiple select；错配 → P3 | — |

## §5 控件联动与数据流（C）

**正面范式（出现即通过，不要求改）**：行金额自动计算（数量×单价→金额/税额/含税金额）、借贷方向切换显隐+清空、科目快照驱动辅助核算显隐、状态开关二次确认、编码唯一性 blur 校验、删除引用阻断、data-source sendOn 条件加载。

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| C1 | 头聚合实时性 | 头合计（totalDebit/totalCredit 等）需按钮刷新而非行变更联动 → P3（标注 DEFERRED 的须确认是否有降级闭环） | ErpFinVoucher.view.xml:125-132 |
| C2 | 币种→汇率联动 | 表单含 `currencyId + exchangeRate` 且无 onEvent 带出 → P3 | ErpPurOrder.view.xml:88 |
| C3 | 物料→计量单位联动 | 子表含 `materialId + uoMId` 且 materialId change 不带出 uoMId → P3 | ErpPurOrderLine.view.xml:27-28 |
| C4 | 行级与头级字段继承 | 头有仓库/组织而子表行同字段可独立编辑且无默认继承 → P3 | ErpPurOrderLine warehouseId |
| C5 | 状态显隐覆盖 | 行级业务动作缺 `visibleOn`（状态机前置条件已知时）→ P2（按钮常显但点击即报错） | ErpCsTicket assign 无 visibleOn |
| C6 | 新建默认值 | add 表单主业务字段无合理默认（业务日期=今天、币种=本位币、orgId=当前组织、订单行数量=1）→ P3（录入摩擦） | 对照 `docs/design/visible-on-patterns.md` 默认值约定 |
| C7 | 已审批/已过账表单只读 | 单据进入 APPROVED/POSTED 后 edit 表单仍可编辑全部字段 → P2（应与行级动作的 status 守卫一致） | 对照 ErpFinVoucher post 后不可编辑语义 |
| C8 | 依赖字段校验时机 | 必填校验全部提交时才触发（无 blur/change 即时反馈）→ P3（长表单提交失败率） | 正例：ErpMdMaterial code 唯一性 blur 校验 |

## §6 表单配置（F）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| F1 | 前端必填与后端校验一致 | 前端未标 `mandatory`/`required` 而后端 `@NotEmpty` → P2（提交才报错，体验差）；反向冗余 → P3 | 对照各域 BizModel `@Name` + `@NotEmpty` |
| F2 | 校验消息可操作 | 校验消息仅"非法值"类无具体期望 → P3（应给出取值范围/格式，如"数量必须大于 0"） | 正例：ErpPurOrderLine quantity validations.minimum |
| F3 | 字段顺序符合录入习惯 | 高频字段（供应商/物料/日期）不前置、只读字段混入录入区 → P3 | 对照 ErpPurOrder edit 布局顺序 |
| F4 | 表单动作区组织 | 提交/取消按钮位置与语义（`useFormActions`）不一致 → P3 | 对照 simple 页 actions 范式 |

## §7 表格配置（G）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| G1 | **列表列数** | > 13 列 → P3（虚拟化后视口外列不可见，须列裁剪/折叠/分组） | ErpLogShipment 40 列、ErpMdMaterial 28 列 |
| G2 | **列序（关键列前置）** | 状态/金额/编码等高频判断列被排到 13 列之后 → P2（视口外不可见） | 对照 ErpPurOrder 列序（code→supplier→date→amount→status） |
| G3 | **列宽配置** | 超长文本列（remark/description）无宽度约束导致撑列、关键短列（状态/日期）无最小宽导致折行 → P3（列宽依赖自动分配，业务上需要时应显式配置） | 全库当前 0 处 `width=` 配置（2026-09-14 核查） |
| G4 | 列分组与折叠 | 主数据页字段全平铺（28+ 列）无列分组/折叠策略 → P3（按业务场景拆"基础/库存/财务"列组或详情页承载） | ErpMdMaterial 28 列 |
| G5 | 金额/数量列对齐 | `align="right"` + `domain="amount"/"quantity"` 缺失 → P3 | ErpPurOrderLine sub-grid-view |
| G6 | 时间/日期格式 | 日期列未 `align="center"` + 格式不一致 → P3 | 对照 ErpPurOrder businessDate |
| G7 | 排序/分页 | 关键列（code/日期/金额）缺 `sortable` → P3；大数据量表默认分页大小未考虑 → P3 | — |
| G8 | 空值展示 | 空字段渲染为空白而非 "—" → P3（数据密度阅读困难） | — |
| G9 | 子表列数 | sub-grid-edit > 12 列 → P2（窄容器挤压；>13 列另有虚拟化问题） | ErpFinVoucherLine 17 列、ErpPurOrderLine 11 列 |
| G10 | 操作列组织 | 行动作列位置与固定（固定列需求）、操作列宽度挤压关键列 → P3 | 对照 §O3 按钮分组 |

## §8 反馈与状态（S）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| S1 | 成功/失败消息覆盖 | 行级 mutation 无 `<messages><success>` → P3（操作后无反馈）；失败消息缺失 → P2 | 全库行级动作 messages 覆盖率 |
| S2 | 危险操作二次确认 | 作废/删除/红冲/驳回等破坏性动作无 `confirmText` → P1（误操作不可恢复） | 正例：ErpFinVoucher 红冲有预览+确认双闸 |
| S3 | 服务端错误呈现 | 服务端校验/业务异常无前端呈现路径（默认 toast 也算）→ 核查错误消息是否可读（非堆栈）→ P3 | — |
| S4 | 空态设计 | 列表/子表/联动建议列表无空态文案 → P3 | 正例：ErpCsTicket kbSuggestion empty 文案 |
| S5 | 加载态 | 异步取数（data-source/initApi）无 loading/占位 → P3 | 正例：ErpFinVoucher reversePreview"加载预览中…" |
| S6 | 操作结果闭环 | 状态流转动作成功后用户能否追踪（操作历史/通知/跳转关联单）→ 不可追踪 → P3 | 正例：ErpCsTicket 操作历史 drawer |

## §9 按钮与操作组织（O）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| O1 | **行级动作硬编码/空参数** | `@mutation` URL 携带 `=0` / `=""` 占位 → **P1 必查**：追查 BizModel 签名确认后端语义（`@Optional` 容忍则 P1 确认缺陷；后端有 fallback 仍要确认占位值非空绕过 fallback） | ErpCsTicket assign?assignedToId=0（真 bug：非空 "0" 绕过空值回落，分派给不存在用户）；resolve?resolution=；ErpMfgWorkOrder reportCompletion?completedQty=0；ErpQaNonConformance resolve；ErpApsOperationOrder scheduleForward?scheduleId=0 |
| O2 | 死按钮 | confirmText/文案提示"去别处操作"但仍调 mutation → P1 或 P2（改 link 跳转或删除） | ErpApsOperationOrder.view.xml:144-145 |
| O3 | **行按钮分组与过载** | 行动作 ≥ 10 → P3（高频平铺 + 低频进 `dropdown-button`"更多"；参考 flux `design-patterns/button-group.md` 的 button-group/dropdown-button 组织方式） | ErpCsTicket 16、ErpMfgWorkOrder 15 |
| O4 | 动作频率与层级 | 高频状态流转（提交/审批/开工）埋在详情页而非列表行、或低频动作（关联查看）占据主按钮位 → P2/P3（主次颠倒） | 正例：全库行级 action 已覆盖主要流转 |
| O5 | 批量操作覆盖 | 有批量审批/启停但缺同族批量（驳回/撤回/作废）→ P3 | ErpPurOrder 仅 batch-approve |
| O6 | 跨页跳转创建 | 创建下游单据用 `link` 跳列表而非参数化生成 → P3 可选优化（若后端已有 createFrom* mutation 优先 drawer） | ErpPurOrder.view.xml:228-232 |
| O7 | 按钮 level 语义 | 破坏性动作不用 `danger`（作废/删除/红冲）、主操作不用 `primary`、次要动作用 `default` → P2（语义色错位） | 对照全库 rowActions level 分布 |
| O8 | 同类动作图标一致 | 同语义动作（提交/审批/作废/查看关联）跨页图标不一致 → P3 | 全库 fa fa-* 对照 |
| O9 | 操作路径长度 | 完成一个完整业务流程的最少点击数 > 5（列表→详情→编辑→子表→保存）→ P3（评估 tabs×drawer 折叠与 §L1 形式选择） | 对照 §L2 |

## §10 国际化与文案（I）

| # | 检查项 | 判定阈值 | 证据页示例 |
|---|--------|----------|-----------|
| I1 | 硬编码中文 | page.yaml / gen-control 字符串中文未走 i18n（`i18n-en` 缺失）→ P3（无 i18n 资源则 P2） | 对照 `docs/architecture/i18n-compliance.md`（唯一权威） |
| I2 | 双语资源覆盖 | 页面有英文文案属性（`i18n-en:`）但部分 label 缺失 → P3 | — |
| I3 | 字典选项双语 | dict/select 选项无 `i18n-en` → P3 | — |
| I4 | 术语与翻译一致 | i18n-en 缺失或同义术语（入库单/收货单）混用 → P3 | 对照 `docs/design/domain-design-guidelines.md` 术语表 |

## P1 核查决策树（O1/O2/S2 命中时逐项走）

```
1. mutation URL 含 =0 / ="" 占位参数？
   → 否：通过。
   → 是：进入步骤 2。
2. 后端方法该参数是否 @Optional？
   → 否：前端漏传必填参数 → P1（动作必炸或语义错误）。
   → 是：进入步骤 3。
3. 后端对空值/占位值是否有 fallback？
   → 有 fallback 且占位值（如 "0"）非空 → fallback 被绕过 → P1 确认缺陷。
   → 有 fallback 且占位值会被判空 → 功能可用但参数无收集入口 → P2（缺参数 dialog）。
   → 无 fallback → P1（参数丢失）。
4. 动作是否需要用户输入业务参数（数量/原因/对象）？
   → 需要 → P1：补参数 dialog + simple 提交页（复用 escalateQuality/rollForward 范式）。
5. S2 破坏性动作是否无二次确认？
   → 无 confirmText/预览 → P1（误操作不可恢复）。
```

## 形式选择决策表（L1 判定辅助）

| 业务形态 | 推荐形式 | 判据 | 项目范例 |
|----------|----------|------|----------|
| 标准 CRUD 编辑 | 单表单（drawer/dialog） | 字段 ≤ 12、无分组 | 多数主数据页 |
| 多组独立字段（基本信息/金额/明细/审计） | tabs | 分组 ≥ 3 且无强制顺序 | ErpPurOrder（4 段） |
| 线性多步流程 + 每步校验闸 | wizard | 步骤 ≥ 3、前步输出是后步输入、不可乱序 | disposal-wizard、lead-conversion、期末结账向导 |
| 主从（头 + 子表） | 头部表单 + 子表同屏；长表单才用 tabs 承载 | 子表是核心录入对象 | ErpPurOrder 编辑（考虑改为头+子表同屏） |
| 多实体档案查看 | drawer + tabs（跨实体 crud） | 只读聚合多实体 | ErpHrEmployee employeeArchive |

## 正面范式清单（项目内已建立，审计时以"复用优先"评估）

| 范式 | 代表页 | 复用建议 |
|------|--------|----------|
| 行金额自动计算（数量×单价→金额/税额/含税） | ErpPurOrderLine sub-grid-edit | 所有含金额子表应同构 |
| 借贷方向切换显隐+清空 | ErpFinVoucherLine dcDirection | 互斥字段对通用模式 |
| 科目快照驱动辅助核算显隐 | ErpFinVoucherLine subjectId | 选择器→派生字段显隐的模板 |
| 状态开关二次确认 | ErpMdMaterial/ErpMdPartner status | 停用/启用类开关通用 |
| 编码唯一性 blur 校验 | ErpMdMaterial code | 主数据编码校验通用 |
| 删除引用阻断（countReferences + 条件 dialog） | ErpMdMaterial/ErpMdPartner/ErpHrEmployee delete | 主数据删除通用 |
| 参数收集 dialog + simple 提交页 | ErpFinBudgetScenario rollForward / ErpCsTicket escalateQuality | 所有需要业务参数的 mutation |
| 状态语义色统一表 | ErpCsTicket status gen-control（success/danger/warning/primary 五段色表） | 新状态字段复制色表并核对语义 |
| 关联列表 drawer（ref-*.page.yaml + fixedProps） | 全库 36 处 xl drawer | 跨实体查看统一入口 |
| tabs 懒加载 + 跨实体 crud 组合页 | ErpHrEmployee employeeArchive | 复杂档案页模板 |
| KB 建议条件加载（sendOn 防抖触发） | ErpCsTicket kbSuggestion | 输入联想类控件模板 |
| wizard 三步参数化流程 | disposal-wizard / lead-conversion | 线性流程模板 |
| 工单进度仪表板（派生展示 cell） | ErpMfgWorkOrder workOrderProgress | 头部聚合展示模板 |

## 反模式自检表（每个页面审计完成后逐项校验）

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 线性多步流程做成 tabs | 用 wizard（每步校验闸 + valuesPath 分区） |
| 编辑 drawer 不标尺寸，内部却是 lg tabs + 宽子表 | 容器尺寸与内容语义对齐（单据类 xl） |
| 弹框内嵌弹框 3+ 层 | 拆独立页或 tabs |
| edit/add 表单直接可改 status/docStatus/approveStatus | 状态机派生字段 readOnly 或仅展示 |
| 业务动作硬编码 `?param=0` / `?param=` | 参数 dialog 收集（参照 escalateQuality/rollForward） |
| 行内手输实体 ID（input-number） | picker 选实体 |
| 树形数据用平铺 select | tree-select |
| 确认框文案说"请去别处操作"却仍调 mutation | link 跳转（参照 ErpPurOrder 创建入库单）或删除按钮 |
| 列表 20+ 列全量平铺 | 列裁剪/分组/折叠，关键列前置 |
| 10+ 个行按钮平铺 | 高频平铺 + 低频"更多"下拉（dropdown-button） |
| 联动缺失且无对侧兜底（如头合计靠手点刷新） | 行→头实时聚合，或至少保留降级按钮 + 显式说明 |
| 破坏性动作无确认直接执行 | confirmText +（高危动作）预览/二次确认 |
| 查询条件双入口并存且内容重叠 | 单一主查询 + 可选侧边筛选（内容不重叠） |
| 必填只靠后端提交时拦截 | 前端 mandatory/required 与后端 @NotEmpty 对齐 |
| 主次按钮 level 错位（作废用 default、保存用 danger） | 破坏性 danger、主操作 primary、次要 default |

## 输出格式

按 P1/P2/P3 分组的 finding 清单，每个 finding 必含：

```
- [P1] <类别> <页面/actionId> <file:line>
  证据：<量化值 / 代码摘录 / 后端签名核对结果>
  判定：<命中阈值 / 决策树结论>
  建议：<修复方式 + 复用范式来源（file:line）>
```

每条 finding 标注命中维度（NA/L/D/W/C/F/G/S/O/I）。结尾给总数统计（按 P 级与维度两维）+ 最终裁决 `passes UX audit` / `needs revision`。审计产出落 `docs/analysis/`（单页自查可不落盘，仅日志记录）。
```

## 关联

- flux 渲染/样式/组件/运行时合规（本技能范围之外）→ `docs/skills/flux-rendering-conformance-audit-prompt.md`
- 首轮审计证据与已确认问题基线：`docs/analysis/2026-09-14-complex-page-ux-review.md`
- 页面资产全景（避免重复盘点）：`docs/analysis/2026-08-03-1000-frontend-complex-page-deep-analysis.md`
- 前端开发范式（写页面时用）：`.opencode/skills/nop-frontend-dev/SKILL.md`
- 复杂页形式与控件映射：`docs/design/flux-complex-pages.md`
- 域级交互范式：`docs/design/<domain>/ui-patterns.md`、`docs/design/visible-on-patterns.md`、`docs/design/child-table-editor-patterns.md`
- i18n 合规唯一权威：`docs/architecture/i18n-compliance.md`