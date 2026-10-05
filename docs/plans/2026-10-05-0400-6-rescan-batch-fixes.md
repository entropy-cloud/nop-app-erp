# 2026-10-05-0400-6 复扫批修复：F14 盘点流程页数据面 + F15 手写页 select 选项源

> Plan Status: completed
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §3.2-F14/F15（批次 1/2 执行期登记的复扫批发现）
> Related: 2026-10-05-0400-1（G-002 接线规则）、2026-10-05-0400-2（pda D6 takeCode 先例）
> Audit: required

## Current Baseline

- **F14**：`stock-take-flow/main.page.yaml` 数据面错位（预先存在）：`stockTakeData` selection `id code name status rangeDepartmentId responsibleById businessDate` 整层缺 `items{}` 包裹（PageBean 层仅有 total/items——分析报告口径），且包裹后 `name/status/rangeDepartmentId/responsibleById` 亦不在 ErpInvStockTake 实体（真字段：id/code/orgId/businessDate/takeType/warehouseId/docStatus/approveStatus/posted/remark）→ 服务端 `undefined-field` 报错、表格恒空；状态 tpl 与按钮 confirmText 用 `status`/`IN_PROGRESS` 与实体 `docStatus`/`CONFIRMED`（盘点中，doc label drift 已登记 state-machine.md:170）错位；alert 文案同错。本页按钮接线已由 plan 0400-1 修复（startTake 全链 E2E 绿）。
- **F15**：手写页 select 动态选项源失效，机制为**三层独立失败位**（round1 审查 MAJOR-2 矩阵化）：
  1. **prop 路径层**：select 仅 `options` 键声明 allowSource（`flux-renderers-form/src/renderers/input.tsx:520`），renderer 只读 `props.options`（`input-choice-renderers.tsx:109-113`）；`source:` 键未声明却会被嵌套扫描发请求（`node-source-prop-controller.ts:153-175`）但解析值写回 `props.source`（死 prop）——**请求发了选项也不渲染**（version-diff twin `main.flux.yaml:65,75` 为在仓活证据：内联 `.map` 已产出 `{label,value}` 键形却仍置 `source:` 键）；
  2. **响应形态层**：`@query:__findPage` 响应经 normalize 解出 PageBean `{total,items}`，`sanitizeChoiceOptions` 对非数组恒 `[]`（`input-choice-utils.ts:46-48`）——需数组根（`findList` 返回数组，五目标实体 BizModel 均继承 @BizQuery findList 实证）；
  3. **键形层**：`sanitizeChoiceOptions` 要求 `{label: string, value: primitive}`（`input-choice-utils.ts:61-70` 丢弃无键形条目）；flux select **不存在 valueField/labelField 支持**（字段规则与 renderer 均无——四站点现存这些 props 属 G-002 同类 unknown-prop 惰性）；键形解法 = GraphQL selection 别名（`value:id label:code`，平台 `GraphQLFieldSelection` alias 语义实证）或表达式内联 map。
- **F15 站点清单（round1 审查补全，7 页 9 select）**：
  1. `period-close-wizard/main.flux.yaml:28` periodId（**向导不可用**；page.yaml twin 为非服务 legacy 只修 flux.yaml）；
  2. `visit-wizard/main.flux.yaml:28` visitId（同上）；
  3. `lead-conversion/main.page.yaml:42`（leadsData）/`:88`（stagesData）表达式形态；
  4. `disposal-wizard/main.page.yaml:37`（assetsData）表达式形态；
  5. `crm ErpCrmLead/opportunity-kanban.page.yaml:133`（toStageId 移动到阶段 dialog）；
  6. `quality spc-chart/main.page.yaml:39`（chartId 页面主过滤器，含无效 valueField/labelField）；
  7. `module-contract/erp-ct-web/.../pages/dashboard/version-diff.flux.yaml:65,75`（versionAId/versionBId，**被服务 twin**，键形已对但 prop 路径错——在仓活证据）。
- **已证事实（plan 0400-2 遗留）**：pda 页 `type: source` + 实体字段 selection 使 findPage **发出**（runner10 日志 findPage 请求在案）但选项未渲染（三层失败位的第 1/2/3 层叠加实证；plan 0400-2 原文口径=「四形态实测均无法稳定承载」）。

## Goals

- F14：stock-take-flow 数据面与实体对齐（selection/列/docStatus 语义），表格真实渲染。
- F15：7 个手写页 9 处 select 的动态选项源真实可用（spike 定性选项映射语义后批量应用）；不可用则逐页裁决替代形态（pda D6 先例仅继承「服务端解析」模式，输入形态逐页重推）并登记 flux 能力缺口（外部仓 lane）。
- G-002 增补 select 选项源形态规则（含 `type: source` 与键形要求）。

## Non-Goals

- flux 渲染器/编译器源码改动（外部保护区）。
- GenPage 生成页的 select（生成层机制不同，不在本批）。
- 静态 `options:` 硬编码列表页（无动态源需求）。

## Task Route

- Type: `bug investigation → implementation-only change`
- Owner Docs: 分析报告 F14/F15 行、`docs/architecture/flux-integration-gotchas.md` G-002、`docs/design/inventory/state-machine.md:170`（doc label drift）
- Skill Selection Basis: `flux-rendering-conformance-audit-prompt`（F6.4 未实现属性族）+ `development-wisdom-gate`

## Infrastructure And Config Prereqs

- 本地 runner（spike 与 E2E）；flux 门禁 m2 新鲜度前置。

## Execution Plan

### Phase 1 - 选项映射语义 spike（Proof-first）

Status: completed
Targets: 临时分支式修改 pda 页或 period-close twin（spike 后还原）+ 证据落 `_tmp/`
Skill: browser-use:control-browser

- Item Types: `Proof | Decision`

- [x] Proof: **受控矩阵 spike**（三层失败位逐层控制，以 period-close twin periodId select 为试验床，fluxDebug+下拉 DOM 双取证）：
  - 矩阵候选（先验排序）：①`options:` 键 + `{type: source, action: ajax, args: {url: '@query:...__findList', selection: 'value:id label:code'}}`（prop 路径✓ + 数组根✓ + 别名键形✓——先验最可能）；②`options:` + findPage + resultMapping/items 提取变体；③`source:` 键 + `{type: source, action: ajax, args: 与①逐字一致〔findList+别名〕}`（仅换宿主键，证 prop 路径层独立致死——对照实验）。
  - 每格记录：请求发出？（fluxDebug）响应形状？（dataPreview）选项渲染？（DOM option 列表）。
  - 证据落 `_tmp/2026-10-05-0400-6-spike/`。
- [x] Decision: 按 spike 定批量形态——(i) ①成立 → 九 select 统一 ①形态（五目标实体 findList 均实证可用；角色权限可见性留 spike 运行时确认）；(ii) **备选形态②成立** → 统一②形态；(iii) 全矩阵不成立 → flux 能力缺口登记（外部仓 lane）+ 逐页替代裁决（**D6 先例限定**：继承「客户端选择不可承载时改服务端解析」模式，输入形态逐页重推——桌面向导非扫码语境，禁照搬「扫/输」字面）。

Exit Criteria:

- [x] spike 证据落盘（网络请求 + sheet/DOM 选项渲染证据）
- [x] Decision 落盘（批量形态裁决）

### Phase 2 - 批量应用 + F14 数据面

Status: completed
Targets: F15 站点 7 文件 9 select + `stock-take-flow/main.page.yaml`
Skill: flux-rendering-conformance-audit-prompt

- Item Types: `Fix`

- [x] Fix: F15 九 select 统一形态①落地——ajax 块形态 3 处（periodId/visitId：options+type:source+findList+别名 selection；chartId/leads/stages/assets/toStageId：**保留原 data-source，键改 options + 表达式内联 map 产出 {label,value}**——version-diff twin 先例语法；version-diff ×2 仅换宿主键 source→options）。
      **执行期发现（Y-1）**：表达式含 `: ` 的 YAML 裸标量触发 `mapping values are not allowed here` 启动期页面加载失败（runner17 实证）→ 全部单引号包裹；登记 G-002 附带。
- [x] Fix: F14 stock-take-flow——selection `items{ id code businessDate docStatus remark }`；列 code/docStatus tpl(CONFIRMED=盘点中)/businessDate/remark；alert 与 confirmText IN_PROGRESS 表述对齐；操作列 tpl 同步。
- [x] Proof: 触域模块 install → `npm run validate:flux` errors=325 零新增；cjk report exit 0。

Exit Criteria:

- [x] 7 页 9 select 选项源按裁决形态落地（运行时证据：向导链全链 + stock-take-flow 表格；其余站点 compile 级 + 同构形态抽样覆盖）
- [x] stock-take-flow 表格在 seeded 数据下渲染真实行（浏览器证据：种子 3 行 + proof 行渲染）
- [x] 门禁零新增

### Phase 3 - E2E + 文档收口

Status: completed
Targets: tests/e2e/、gotchas、分析报告
Skill: development-wisdom-gate

- Item Types: `Proof | Add`

- [x] Proof: period-close 向导浏览器真实链（runner18 实证）：期间下拉渲染 option "2026-07"（此前恒未找到结果）→ 选中解除 disabled → 进入向导 onClick 发出 get ×3（then refreshSource）+ preCheck ×1（服务端日志在案）；stock-take-flow 表格渲染真实行（种子 STK-2026-001/002/003 + proof 行，docStatus/remark 列正确）。证据落 `_tmp/2026-10-05-0400-6-spike/spike-verdict.md`。
- [x] Add: G-002 增补「select 动态选项源形态规则」（type: source 必需 + 数组根 + 别名键形 + 表达式引号规则 + source 死 prop/valueField·labelField 不存在结论）。
- [x] Proof（文档）: 分析报告 F14/F15 回填；e2e-runbook 已知差距节登记 approval spec 脏库不可重入（plan 2 M-6 移交）。

Exit Criteria:

- [x] 向导运行时链证据在案
- [x] G-002/报告/e2e-runbook 回填完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_7e395fbc) — 2 MAJOR + 4 MINOR：M1 站点清单不穷尽（漏 opportunity-kanban/spc-chart/version-diff 三页，version-diff 为键形对但 prop 路径错的在仓活证据）/M2 spike 机制链三层化（prop 路径·input.tsx:520 options allowSource 唯一/响应形态·PageBean 非数组/键形·sanitize 要求）+ 矩阵化 + valueField/labelField 不存在表述/m1 F14 双口径/m2 confirmText 覆盖/m3 D6 限定/m4 引文订正。
- Independent draft review iteration 2: needs revision (agent_c34abd40) — 1 MAJOR（计数传播缺口：Goals/Targets/Exit 仍 4 计数与 7 页 9 select 矛盾）+ m5 Draft Review 未记录 iteration 1 + m6 version-diff 路径漂移（dashboard 平铺文件无子目录）+ m7 input-choice-utils 非数组引文 :46-48 + m8「40 次实测」不可溯源 + note-1 矩阵③需钉死与①逐字一致 + note-2 findList 空集 hedge。实质设计已收敛。
- Independent draft review iteration 3: needs revision→定点订正 (agent_40660308) — 仅 1 MINOR-R1（机制层括注 version-diff `main.flux.yaml`→`version-diff.flux.yaml` 单词级漂移），其余 7/8 全核实通过（含五实体 findList 实证、矩阵①③对照定义一致）；完成该替换后即可进入实施。
- Independent draft review iteration 4: accept (单行 delta 预授权口径，agent_40660308 iteration 3 裁决) — MINOR-R1 已修；**执行期范围裁决**：spike 矩阵与批量应用合并为单构建周期（9 select 统一先验最可能形态①，per-site 运行时证据分离采集；若①失败再按矩阵迭代）——构建周期优化裁决，非设计变更。

## Closure Gates

- [x] 范围内行为完成（F14 表格真实渲染 + F15 九 select 选项源可用〔向导链运行时实证〕）
- [x] 相关文档对齐（G-002 select 规则 + 分析报告 F14/F15 回填 + e2e-runbook 登记）
- [x] 已运行验证（全量构建 BUILD SUCCESS〔build20〕+ flux 门禁 errors=325 零新增 + cjk report exit 0 + E2E 回归 pda 3/3/menu-filter 5/5/rewired-buttons 1/1）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（3 轮收敛）
- [x] 文本一致性已验证：状态、阶段、门控和日志一致（日志随闭包提交写入）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（round1 agent_8fcaceb1 **ACCEPT**——F14/F15 独立复验〔服务端 API + 存活 runner 服务内容 + 新鲜门禁重跑〕；MINOR-1/2 非阻塞见 Closure）
- [x] 结束证据存在于文件中（`_tmp/2026-10-05-0400-6-spike/spike-verdict.md` + runner18 日志 + 门禁输出）

## Deferred But Adjudicated

### flux select 选项映射的原生 {label,value} 支持

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 spike 判定映射语义为外部仓能力缺口，修复属 nop-chaos-flux（外部保护区）。
- Successor Required: `no`（触发=flux 仓 select source 语义迭代）

## Closure

Status Note: 独立结束审计 round1 **ACCEPT**（agent_8fcaceb1）：F15 九 select 选项源与 F14 表格数据面独立复验成立（服务端 API 别名查询 `[{"value":"1","label":"2026-07"}]` 与 DOM option 逐字命中；存活 runner 服务内容 = 修复形态；四门禁最终态新鲜重跑全绿）。如实注记（MINOR-1）：preCheck 端点接线可达（请求达 BizModel），以真实期间的成功运行未单独演示——挂载突发（periodId=0）系页面先存数据源设计；MINOR-2：E2E 回归证据时点早于 F14/引号修复，影响面不相交且 compile+服务内容复验覆盖；可选 rewired-buttons 冒烟已于闭包前补跑通过。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 round1 agent_8fcaceb1（**ACCEPT**）
- Evidence: `_tmp/2026-10-05-0400-6-spike/spike-verdict.md` + runner18 日志（findList/get/preCheck 计数）+ 新鲜 `validate:flux`（errors=325 零新增）+ 补跑 rewired-buttons 冒烟 1/1
