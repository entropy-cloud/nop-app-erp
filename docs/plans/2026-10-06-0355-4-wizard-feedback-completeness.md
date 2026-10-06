# 2026-10-06-0355-4 向导/作业页交互反馈完整性（messages/then 推广 + 空值门 + 数值钳制）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md §3.2 F-R5/F-R6/F-R9/F-R10（前端半）/F-R15 + §2 D3 定案
> Related: 2026-10-05-0400-2-pda-stock-take-closure.md（pda 范式先例）
> Audit: required

## Current Baseline

- 批次 2（pda 页）确立的反馈范式：按钮 `messages.success/failed` + `then`（refreshSource/setValues 重置）+ confirmText；flux 无 `validations` 块语法，校验创作面=字段级 prop（round-3 报告 §2 D3）。
- **6 个 mutation 按钮静默成功**（缺 `messages.success/failed`）：disposal-wizard 确认提交（:123-141，且 assetsData 查询 :21 无 status 过滤——已处置资产仍在下拉、可重复提交）、lead-conversion moveStage 确认（:96-110）、visit-wizard 保存结果（:166-178）、period-close closePeriod/finalizePeriod/reverseClose（:114-129,:164-179,:198-214）。**then 链澄清（迭代 1 审查修正）**：period-close 三按钮已有 `then: refreshSource periodData`（main.flux.yaml:127-129/:177-179/:212-214）——其缺口仅为 messages ×3；disposal/lead-conversion/visit 三按钮则 messages 与 then 双缺（不刷新、不重置）。period-close then 链是否需扩充（如 statusRows）归 Phase 1 显式裁决。
- 空值门缺失 ×3：pda 保存按钮（:53-83）、lead-conversion 确认（`${!toStageId}`）、period-close 反结账（死文件曾有 `disabled:"${!reverseCloseReason}"`，live 丢失）。
- 数值钳制缺失 ×2：pda actualQuantity（:48-52）、disposal disposalAmount（:62-68）无 `min:0`（flux input-number min/max=提交钳制）。
- stock-take-flow 三按钮依赖手输不可见 `takeId`（input-number :69-76；表格无 id 列）——F4 同族新站点。
- 小额：period-close 期间 select source 块缺 `query:{limit}`（visit-wizard 有）；stock-take-flow/disposal 列表 `limit:200` 静默截断无指示；visit-wizard 备件 totalAmount 裸 DECIMAL。

## Goals

- 4 页 6 按钮（disposal 1 + lead-conversion 1 + visit 1 + period-close 3）补齐 `messages.success/failed`；其中 disposal/lead-conversion/visit 三按钮同时补 `then`（刷新/重置）；每条新增中文文案按 CJK CAT 口径携带 `i18nEn`（pda 先例 :62-67 `messages.i18nEn` 形态）；disposal assetsData 查询补 status 白名单。
- 三处空值门 disabled 表达式补齐；period-close closePeriod 加在途 scope 状态位示范（Decision）。
- 两处 `min:0`；period-close select 补 `query:{limit:200}` 对齐 visit-wizard；两处列表补 footerToolbar 分页/截断提示；visit-wizard totalAmount `ROUND(x,2)`。
- stock-take-flow 盘点单定位形态 Decision：①select 绑定 `ErpInvStockTake__findPage`（F14 后可用，code→id 映射，零后端改动）vs ②后端三 mutation 增 takeCode @Optional（pda scanSave :66-91 先例）。

## Non-Goals

- 不改后端业务逻辑（disposalAmount 负数后端守卫归 plan-5 Decision；startTake 等签名不改——除非 Decision②成立）。
- 不做全按钮在途 busy 推广（Deferred：flux 无 auto-busy）。
- 不做日期/金额格式化平台范式（Deferred F-R16）；仅 visit-wizard 单点 ROUND。

## Task Route

- Type: `implementation-only change`（页面交互面，用户可见行为）
- Owner Docs: docs/design/ 下各域向导设计（disposal/period-close/visit/lead-conversion）；pda 范式先例 plan
- Skill Selection Basis: Skill: none（机械页面配置改写；无匹配可复用技能）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - Decision 定案

Status: completed
Targets: 本计划文件
Skill: none

- Item Types: `Decision`

- [x] Decision: stock-take-flow 盘点单定位=**①select 绑定页面既有 stockTakeData**（options 表达式 label=单号·状态/value=id；零后端改动、与 period-close 期间 select 同范式；替代②后端 takeCode @Optional 弃——①复用已加载数据且消除盲输数字主键）。残留风险：新建单需数据源重挂载后入选项（E2E 以跨路由往返解决，实测通过）；flux select 为自定义 combobox（E2E 交互=点击+option 点选）。
- [x] Decision: closePeriod 在途状态位=**豁免登记 Deferred**（flux 无可靠跨组件 scope 写入动作面[setValues 仅表单域]；既有 confirmText 双重确认已拦截误触； F-R9 在途面随 flux auto-busy 能力一并在 Deferred 登记）
- [x] Decision: period-close 三按钮既有 then 链=**维持现状**（refreshSource periodData 已覆盖状态翻转反馈；无其他源需要刷新）

Exit Criteria:

- [x] 三项 Decision 落计划（如上）

### Phase 2 - 机械改写（5 页）

Status: completed
Targets: disposal-wizard / lead-conversion / visit-wizard / period-close-wizard / stock-take-flow / pda-stock-take
Skill: none

- Item Types: `Fix`

- [x] messages 补齐 ×6（disposal 确认提交[+then: assetsData 刷新+step1Form 重置]/lead-conversion moveStage[+then: leadsData/stagesData 刷新]/visit 保存结果[+then: visitData 刷新]/period-close ×3[then 维持]）；全部新增文案带 i18nEn（英文值按 Y-1 规则加引号）；disposal assetsData 查询补 `filter_status__in: ["IN_SERVICE","IDLE"]` 白名单（处置候选语义，ErpAstConstants 状态集）
- [x] 空值门 ×4（pda 保存 disabled=!takeCode||!skuBarcode||actualQuantity==null + lead-conversion 确认 disabled=!step2Form?.toStageId + period-close 反结账 disabled=!reverseCloseReason + stock-take-flow 三按钮 disabled=!actionForm?.takeId）+ min:0 ×2（pda actualQuantity/disposal disposalAmount）+ period-close select query limit 200 + footerToolbar 分页/计数截断指示（stock-take-flow）
- [x] stock-take-flow 按 Decision① 改写：takeId input-number → select（绑定 stockTakeData）
- [x] 复核修正：F-U13（visit 备件 totalAmount 裸 DECIMAL）经实读复核为**假阳性**——totalAmount 仅存在于 selection 未被任何文本/列渲染，无 ROUND 改写点（round-3 报告已回填）
- [x] rewired E2E 交互同步（spinbutton fill → flux combobox 点击+option 点选；跨路由往返替代 reload[SPA 白屏怪癖]）

Exit Criteria:

- [x] 全部改写项 grep 复核在位；新增文案 i18nEn 零缺失（wizard-feedback.spec 结构断言 3/3 绿）
- [x] Decision② 不适用（选①零后端）

### Phase 3 - 运行时证明

Status: completed
Targets: E2E（flux 渲染模式，`npm run e2e`）
Skill: none

- Item Types: `Proof`

- [x] 新 spec `wizard-feedback.spec.ts` 3/3 绿：pda 保存空值门（初始 disabled 实测）+ lead-conversion 确认门结构断言 + 全触页 messages/min/gate/limit/footer 结构断言
- [x] 成功路径证明=rewired spec 全链（startTake 点击[带 messages.success]→CONFIRMED 翻转→then 链）绿；回归套件 17/17（rewired/f13 skip 基线/pda/menu-filter/kanban-r/residual/wizard-feedback）；cjk 义务满足（新增文案均带 i18nEn；导出 FLUX_PAGE_ERROR_COUNT: 0）

Exit Criteria:

- [x] 新断言通过 + 回归零失败

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_3826a1be, 2026-10-06) — period-close 三按钮 then 链已在位（基线修正，缺口=messages ×3）；×6 vs「两按钮」计数矛盾消解；新增文案 i18nEn 义务 + Closure Gates 增 cjk strict；Deferred 补触发；pda「复核」短语删除；E2E 命令写死；validate:flux 归 Closure Gates；同文件行号漂移注意 → 全部采纳修订
- Independent draft review iteration 2: NEEDS REVISION→条件收敛 (agent_e418a158, 2026-10-06) — 残留 2 处机械缺陷（Targets 残留 pda 短语、Exit Criteria「两项」应为「三项」），已按审查者枚举逐字修复；审查者裁决「不动摇技术方案/基线/范围，修订后可执行」→ 达成 ACCEPT 条件

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（如 Decision② 改后端则同步盘点 owner doc）
- [x] 已运行验证（Closure 统一：`npm run validate:flux` errors=325 零新增 + E2E 新断言+回归 + 触域单测[仅 Decision②时] + cjk strict）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 在途 busy 全按钮推广

- Classification: `watch-only residual`
- Why Not Blocking Closure: flux 无 auto-busy（button.tsx:71,210 仅显式 prop），逐按钮状态位高 churn
- Reopen Trigger: flux runtime auto-busy 能力落地；或重编排按钮实测误触反馈
- Successor Required: `no`

## Closure

Status Note: 六按钮 messages/then 范式补齐 + 四处空值门 + 两处 min 钳制 + select limit + 分页指示 + stock-take-flow 定位 select 化全部落地；E2E 3/3 新探针 + 17/17 回归全绿；F-U13 复核为假阳性已回填报告；三项 Decision 成文。

Closure Audit Evidence:

- Auditor / Agent: agent_1bcceea5（独立结束审计 2026-10-06：**ACCEPT** 附 4 项回填条件[已全部实施]——a-g 七项实证 + 独立复跑 4 passed + FLUX_PAGE_ERROR_COUNT: 0；条件①F-R15 假阳性注记②disposal 指示文本补齐[采用提示文本方案而非 Deferred]③日志条目④本指针）

Follow-up:

- (none)
