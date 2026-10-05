# 2026-10-06-0355-4 向导/作业页交互反馈完整性（messages/then 推广 + 空值门 + 数值钳制）

> Plan Status: active
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

Status: planned
Targets: 本计划文件
Skill: none

- Item Types: `Decision`

- [ ] Decision: stock-take-flow 盘点单定位形态（① select 绑定 findPage vs ② 后端 takeCode @Optional）——记录选择、替代、残留风险（预期①：零后端改动、与 period-close 期间 select 同范式）
- [ ] Decision: closePeriod 在途状态位示范形态（scope 变量 + disabled 表达式）或豁免登记（若 flux 无可靠 scope 写入面则改 confirmText 强化并登记 Deferred）
- [ ] Decision: period-close 三按钮既有 `then: refreshSource periodData` 是否需扩充（如状态行/其他源）——或缺省维持现状（仅补 messages）

Exit Criteria:

- [ ] 三项 Decision 落计划（选择+理由+替代+风险）

### Phase 2 - 机械改写（4+1 页）

Status: planned
Targets: disposal-wizard / lead-conversion / visit-wizard / period-close-wizard / stock-take-flow
Skill: none

- Item Types: `Fix`

- [ ] messages 补齐 ×6（disposal/lead-conversion/visit 三按钮另补 then：step1Form 重置 + assetsData status 白名单 / stages/leads 刷新 / visitData 刷新；period-close 三按钮按 Phase 1 裁决维持或扩充 then）；全部新增文案带 i18nEn
- [ ] 空值门 ×3 + min:0 ×2 + select limit + footerToolbar 分页/截断指示 ×2 + ROUND ×1
- [ ] stock-take-flow 按 Phase 1 Decision 改写盘点单定位
- [ ] 执行注意：本计划与 plan-1（批次 1）同改 disposal-wizard/lead-conversion 文件，行号引用以现场重读为准（guide 规则 1）

Exit Criteria:

- [ ] 全部改写项 grep 复核在位；新增文案 i18nEn 零缺失
- [ ] 若 Decision②：inv-service 触域单测绿

### Phase 3 - 运行时证明

Status: planned
Targets: E2E（flux 渲染模式）
Skill: none

- Item Types: `Proof`

- [ ] E2E（`npm run e2e`，E2E_ENGINE=flux；spec 落 tests/e2e 既有布局）：≥1 页成功路径断言 success toast + 数据刷新（建议 lead-conversion moveStage：拖拽/选择后阶段文案更新）；≥1 页空值门断言（reason 为空时按钮 disabled）
- [ ] 既有 E2E 回归零失败；触运行时字符串 → `cjk checker --strict` 回绿（如触发 baseline 需逐站点裁决）

Exit Criteria:

- [ ] 新断言通过 + 回归零失败

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_3826a1be, 2026-10-06) — period-close 三按钮 then 链已在位（基线修正，缺口=messages ×3）；×6 vs「两按钮」计数矛盾消解；新增文案 i18nEn 义务 + Closure Gates 增 cjk strict；Deferred 补触发；pda「复核」短语删除；E2E 命令写死；validate:flux 归 Closure Gates；同文件行号漂移注意 → 全部采纳修订
- Independent draft review iteration 2: NEEDS REVISION→条件收敛 (agent_e418a158, 2026-10-06) — 残留 2 处机械缺陷（Targets 残留 pda 短语、Exit Criteria「两项」应为「三项」），已按审查者枚举逐字修复；审查者裁决「不动摇技术方案/基线/范围，修订后可执行」→ 达成 ACCEPT 条件

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（如 Decision② 改后端则同步盘点 owner doc）
- [ ] 已运行验证（Closure 统一：`npm run validate:flux` errors=325 零新增 + E2E 新断言+回归 + 触域单测[仅 Decision②时] + cjk strict）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 在途 busy 全按钮推广

- Classification: `watch-only residual`
- Why Not Blocking Closure: flux 无 auto-busy（button.tsx:71,210 仅显式 prop），逐按钮状态位高 churn
- Reopen Trigger: flux runtime auto-busy 能力落地；或重编排按钮实测误触反馈
- Successor Required: `no`

## Closure

Status Note: (pending)

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (none)
