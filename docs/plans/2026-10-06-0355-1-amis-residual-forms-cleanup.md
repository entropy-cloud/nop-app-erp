# 2026-10-06-0355-1 AMIS 残留形态清零（hybrid 按钮 + download 按钮 + 死 prop 视觉族）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md §3.1 F-R3/F-R4 + §3.3 F-R13/F-R14
> Related: 2026-10-05-0400-1-flux-handwritten-action-rewiring.md（批次 1 范式先例）；docs/architecture/flux-integration-gotchas.md G-002
> Audit: required

## Current Baseline

- 16 个被服务 `*.flux.yaml` 孪生页存在 17 个「hybrid 形态」惰性按钮：`actionType: ajax` + `action: ajax` + `args:` + `target:` 平铺按钮级、缺 `onClick` 包裹（grep `actionType: ajax` --include="*.flux.yaml" 计数 16 文件 17 处，version-diff ×2）。按钮 renderer 仅消费 onClick（batch-1 探针实锤机制同源），点击零效果。
- 25 个 `report/*.page.yaml`（无 flux 孪生，活页）× 各 2 个 `actionType: download` + `api:` 惰性下载按钮（XLSX/PDF），flux 模式下全部种子报表下载不可用。flux 内置 `download` 动作（`flux-core/src/constants.ts:54`）Form 1（`{api:{...}}`，responseType 强制 blob；实现于 `packages/flux-runtime/src/runtime-action-helpers.ts:337-388`，Form 1 分发 :373-388）与页面现有 `api:{url,method,data}` 形状直接同构。
- 手写页活面 `level: primary/danger` 为 flux button 不存在的死 prop（flux 用 `variant`，danger→destructive）：危险按钮无红色、主按钮无强调（grep `level: danger|level: primary` 命中 16 文件，含部分死文件须剔除）。
- select `labelField`/`valueField` 死 prop 3 处活面（disposal-wizard :38-43、lead-conversion :43-47,:89-90），富标签意图未实现。
- `_tmp/flux-page-validation-report.json`（2026-10-05 新鲜）：上述接线面每按钮 3 条 actionType/action/args unknown-property 交叉印证。

## Goals

- 全部被服务活页的 AMIS 残留形态清零：17 hybrid 按钮 → `onClick: {action:'refreshSource', targetId:<同名数据源>}`（同参同 URL 数据源先例：prj boardData）或 `onClick:{action:'ajax',args}`（无对应数据源时，Decision 逐个裁决）；50 download 按钮 → `onClick:{action:'download', args:{api:{...}}}`。
- 活面 `level:` → `variant:` 映射改写（primary→primary、danger→destructive，其余形态逐个 Decision）；活面 labelField/valueField 富标签化（label 表达式 map）并删死 prop。
- G-002 增补两条具名形态：hybrid（actionType+action+args 平铺）、download 转换行（09-amis-migration.md 无该行，本仓先例补登记）。

## Non-Goals

- 不改 nop-chaos-flux / nop-entropy 任何文件（验证器 prop 表缺口不属本批）。
- 不处理 legacy 死孪生文件（*.page.yaml 非活面）的残留（归 plan-6 parse 门禁与 Deferred 政策项）。
- 不做日期/金额格式化（Deferred F-R16）。

## Task Route

- Type: `implementation-only change`（用户可见行为修复：按钮从惰性变可用）
- Owner Docs: docs/architecture/flux-integration-gotchas.md（G-002）、docs/architecture/flux-page-export-and-validation.md（门禁对照）
- Skill Selection Basis: 页面机械改写无匹配技能（Skill: none）；验证遵循 e2e-runbook flux 渲染模式强制

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（`npm run validate:flux`、E2E flux 引擎均既有）。

## Execution Plan

### Phase 1 - 活面盘点与转换范式定案

Status: completed
Targets: 16 个 flux.yaml + 25 个 report page.yaml + level/labelField 命中清单
Skill: none

- Item Types: `Decision | Proof`

- [x] Decision: 17 hybrid 按钮逐个映射同名同参数据源——实测 15 个 (url+data) 精确匹配、2 个 timeline 按钮 URL 同但数据源 args 更全（含 orderBy，refreshSource 重载后排序对齐初始加载），统一 `onClick:{action:'refreshSource', targetId:<数据源>}`，无 NO-MATCH 站点。底表=round-3 报告 §3.1 F-R3 行（16 文件清单）。
  - Skill: none
- [x] Decision: `level:X` 活面映射表=primary→variant:primary、danger→variant:destructive、success→variant:secondary（flux 调色板无绿，secondary 保留区分）；死文件剔除口径=有 flux 孪生的 *.page.yaml。活面实测 21 处。底表=round-3 报告 §3.3 F-R14 行。
  - Skill: none
- [x] Proof: 盘点底表齐备——17 hybrid（16 文件）/50 download（25 文件）/level 21 处/labelField select 3 处；`org-chart.flux.yaml:58`、`bom-tree.flux.yaml:80` 的 labelField 挂在 `type: tree`（graph 渲染器 `flux-renderers-graph/src/schemas.ts:38` 合法 prop）→ **豁免登记**（gotchas G-002 落地状态节）。底表=round-3 报告 §3.1 F-R4 行 + §3.3 F-R13 行。

Exit Criteria:

- [x] 三张盘点底表齐备且与 grep 计数一致（17/50/21/3；tree 渲染器 2 处豁免已登记）

### Phase 2 - 机械改写

Status: completed
Targets: Phase 1 底表所列文件
Skill: none

- Item Types: `Fix`

- [x] 17 hybrid 按钮 onClick 化（统一 refreshSource+targetId），删除平铺 actionType/action/args/target 死键——脚本转换计数 17/17，转换后全量 yaml parse 零失败
- [x] 50 download 按钮 download 化（`onClick:{action:'download',args:{api:{...原 api 对象}}}`），删除 actionType/api 死键——计数 50/50
- [x] level→variant 活面改写 21 处 + labelField/valueField 富标签化 3 处（disposal 资产/lead-conversion 线索富 label 表达式、toStageId 冗余死 prop 删除）

Exit Criteria:

- [x] 三张底表全部行改写完成，grep 复核：被服务活文件（flux.yaml+report 页+7 AMIS 页）中 `actionType: ajax|download` 零命中（死孪生文件残留按 Non-Goals 不动）
- [x] `npm run validate:flux` errors=325 精确持平（零新增）、warnings 21254→21079、**触及文件 wiring 族 warning 归零（实测 0 条）**；余 28 条 wiring warning 位于 view.xml delta/生成链页面（ErpFinVoucher/ErpMdMaterial 等）——非本批范围，登记 round-3 报告新 finding（F-R17，需运行时可达性证据再立项）

### Phase 3 - 运行时证明

Status: completed
Targets: E2E（本仓 tests/e2e 布局，npm run e2e，E2E_ENGINE=flux）
Skill: none

- Item Types: `Proof`

- [x] 探针：`tests/e2e/business-actions/amis-residual-refresh.action.spec.ts` 2/2 绿——负对照=修复前 hybrid 点击零请求 + fluxDebug notify error「Source not found」（执行期实锤，引出 F-R18 跨仓修复）；正对照=修复后（含 flux scope 修复 b18117e97）点击触发 boardData 重发。
- [x] 真实下载断言：`reports.amis-download.spec.ts` 6 用例绿（fin income-statement/crm funnel/cs ticket-sla × {xlsx,pdf}，真实点击 + download 事件 + 魔数 + token 断言，三种参数形态全覆盖）——满足并超出「≥1 页」要求；余 4 挂（mfg crp-load/md partner-list）=F-R19 预存后端/稳定性家族（直连层复现，非本批范围）。
- [x] 验证命令与落点：spec 落本仓 tests/e2e 布局；运行命令=`npm run e2e`（E2E_ENGINE=flux）；既有回归同 run 零失败。

Exit Criteria:

- [x] 负/正对照探针证据在案；下载断言通过（6 用例）
- [x] 既有 E2E 回归（pda/menu-filter/rewired-buttons/f13）零失败

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_18b3414c, 2026-10-06) — runtime-action-helpers 包路径与行号修正；view.xml Deferred 缺重开触发；labelField 底表须含 org-chart/bom-tree 两处裁决；Phase 3 缺确切 spec/命令 → 全部采纳修订
- Independent draft review iteration 2: ACCEPT (agent_e418a158, 2026-10-06) — 迭代 1 全部要求实证级修复核验通过，无残留

## Closure Gates

- [x] 范围内行为完成（三张底表清零 + 探针/下载证明）
- [x] 相关文档对齐（G-002 增补 hybrid/download 形态；gotchas 修复落地状态 2026-10-06 节补本批）
- [x] 已运行验证（validate:flux 零新增 + E2E 探针/下载断言 + 回归零失败）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（agent_22618dfe (独立结束审计 2026-10-06：0355-1/0508-1 文本回填后通过，0555-1 ACCEPT；审计者独立复跑 E2E：refresh 2/2、download 6/4[F-R19 精确复现]、回归 13 绿 3 skip；commit 指纹 34fb37d/b18117e97 范围核验通过)）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### legacy 死孪生文件残留

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 死文件不被服务，归 plan-6 parse 门禁 + Deferred 政策项
- Successor Required: `yes`（plan-6）

### view.xml gen-control 层 AMIS 风格

- Classification: `watch-only residual`
- Why Not Blocking Closure: 不在 flux 导出范围、无惰性实证（round-3 报告 §4）
- Reopen Trigger: flux 导出范围扩展覆盖 view.xml/gen-control 产物时；或 gen-control 层出现运行期惰性实证时
- Successor Required: `no`

## Closure

Status Note: 三 Phase 全部完成——Phase 1 底表落 round-3 报告 §3.1 F-R3/F-R4 行（17 hybrid 16 文件、50 download 25 文件）与 §3.3 F-R13/F-R14 行（labelField 3 处+tree 豁免 2 处、level 21 处）；Phase 2 机械改写+门禁归零；Phase 3 探针 2/2 绿（独立审计复跑确认）+ 真实下载断言 6 用例（超出 ≥1 要求）。两处范围修订（下载 10/10→6/10[F-R19 successor]、无）均有成文记录。外部仓双修复（0508-1/0555-1）各自独立提交。

Closure Audit Evidence:

- Auditor / Agent: agent_22618dfe (独立结束审计 2026-10-06：0355-1/0508-1 文本回填后通过，0555-1 ACCEPT；审计者独立复跑 E2E：refresh 2/2、download 6/4[F-R19 精确复现]、回归 13 绿 3 skip；commit 指纹 34fb37d/b18117e97 范围核验通过)

Follow-up:

- (none)
