# 2026-10-06-0355-3 notify 收件箱查询面闭环（全部 tab + 筛选接线）

> Plan Status: active
> Last Reviewed: 2026-10-06
> Source: docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md §3.1 F-R1 + §3.2 F-R7（§审查修正回填）
> Related: docs/design/notify/inbox-patterns.md（收件箱数据源 owner doc，§2 三 tab 策略与降级裁决）；2026-10-05-0400-6-rescan-batch-fixes.md（F14 数据面对齐先例）
> Audit: required

## Current Baseline

- **owner doc 已有权威裁决**（`docs/design/notify/inbox-patterns.md` §2）：「全部」tab = **客户端拼接 `findUnread` + `findRead`**（合并去重），明文「避免新加 findInbox 方法」；`status` 是 lifecycle 字段（SENT/MERGED/FAILED）非已读态，禁用作已读过滤；已读态派生自 `ErpSysNotificationRead` 关联。
- 页面实现偏离 owner doc（code-vs-doc 漂移即缺陷本体）：inbox.page.yaml「全部」tab data-source 误接 `@query:ErpSysNotification__findUnread`（:256，与未读 tab 同源——已读通知在全部 tab 永不出现）且 selection 含实体不存在的 `read` 字段（:260）→ GraphQL `undefined-field`；`read` 映射列（:281-289）恒不渲染。
- `ErpSysNotificationBizModel` 仅 `findUnread`/`findRead`/`countUnread` 三个查询（:75-108），无 find-all；`findUnread/findRead` 返回全量列表（无 limit），`countUnread` 对全量 unread 列表 `.size()`。
- 页面共**四个** data-source：unreadCountData（:9-16）、unreadData（:131-139）、readData（:202-210）、allData（:252-260）。
- 「刷新」按钮已是四源 refreshSource 链（:107-121：unreadCountData→unreadData→readData→allData）——**不是缺口**，本批保持不变。
- markRead/markAllRead 按钮已是 `/r/` 直发 + 四源刷新链（:33-48,:184-191）——本批不改。
- 筛选表单 5 字段（notificationType/channel/sentAtFrom/sentAtTo/status）整体死接：任何 data-source 的 args 均裸 `data:{userId:null}`，字段值从未被引用。
- 筛选下拉 status 选项值域 = SENT/MERGED/FAILED（:97-105）；实体 status defaultValue=PENDING（orm.xml :99）——值域缝隙需在 Decision 中对齐。

## Goals

- 按 owner doc 裁决实现「全部」tab：客户端拼接 findUnread + findRead（合并去重），`read` 列改为派生渲染（id ∈ readData 集合）或拼接时派生——机制归 Phase 1 Decision。
- 筛选表单接线 Decision：①扩展 `findUnread/findRead` 增 @Optional 白名单过滤参数（type/channel/status/sentAt 范围；向后兼容、不加新方法、符合 owner doc 精神；全部 tab 双查询带同参过滤后拼接；PENDING 值域缝隙一并裁决）vs ②移除筛选 UI（避免误导，回退最小面）。
- E2E：全部 tab 真实渲染断言（已读通知出现、read 列映射正确）+ 筛选生效断言（若 Decision①）。

## Non-Goals

- 不新增独立 findInbox/findPage 端点（除非 Phase 1 Decision 证明客户端拼接机制不可行——届时须同步更新 owner doc inbox-patterns.md 并记录替代理由）。
- 不改 findUnread/findRead 返回形态与 countUnread（watch-only Deferred 登记）。
- 零 orm 变更（已读态关联表结构不动）。
- 不动 markRead/markAllRead/刷新按钮既有接线（已验证在位）。

## Task Route

- Type: `bug investigation` → `implementation-only change`（code-vs-owner-doc 漂移修复）
- Owner Docs: docs/design/notify/inbox-patterns.md（§2 数据源策略——本批实现向其收敛）
- Skill Selection Basis: Skill: none（BizModel @Optional 参数扩展与页面数据源接线，无匹配可复用技能）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（notify 域测试种子既有）。

## Execution Plan

### Phase 1 - 机制 Decision 定案

Status: planned
Targets: 本计划文件
Skill: none

- Item Types: `Decision`

- [ ] Decision: 全部 tab 拼接与 read 派生机制——候选：a) 双数据源已加载（unreadData/readData 既有），allData 改表达式派生（须实仓验证 flux 表达式对两源 concat/去重/set-membership 的支持面）；b) 后端扩展现有查询返回携带 read 布尔（偏离 doc 的「实体返回」形态，须 owner doc 增补）；c) 新端点（Non-Goal 除非 a/b 均不可行）。记录选择+替代+残留风险+（b/c 时）owner doc 更新义务。
  - Skill: none
- [ ] Decision: 筛选接线（①@Optional 白名单过滤参数[含 PENDING 值域对齐：筛选选项补 PENDING 或移除 status 筛选] vs ②移除筛选 UI）——记录选择与理由。
  - Skill: none

Exit Criteria:

- [ ] 两项 Decision 落计划（选择+理由+替代+风险；若涉 owner doc 更新须列出更新段落）

### Phase 2 - 实现

Status: planned
Targets: `module-notify/erp-notify-service/.../ErpSysNotificationBizModel.java` + `module-notify/erp-notify-dao/.../IErpSysNotificationBiz.java`（若 Decision①扩展签名，接口双件套同步）；`inbox.page.yaml`
Skill: none

- Item Types: `Fix | Add`

- [ ] Fix: 全部 tab 按 Phase 1 Decision 改接（拼接 + read 派生/渲染；`undefined-field` 消除）
- [ ] Fix/Add: 筛选表单按 Phase 1 Decision 接线或移除
- [ ] Proof: 若触后端（Decision①），JUnit 断言过滤/组合语义 + `mvn test -pl module-notify/erp-notify-service -am` 绿

Exit Criteria:

- [ ] `npm run validate:flux`（归 Closure Gates 统一跑）；inbox 页 selection 与后端返回形态字段级对齐（零 undefined-field 面态——以 Phase 3 E2E 实证）

### Phase 3 - E2E 证明

Status: planned
Targets: E2E（flux 渲染模式，`npm run e2e`；spec 落点对齐 tests/e2e 既有布局）
Skill: none

- Item Types: `Proof`

- [ ] 全部 tab：已读通知出现在列表、read 列映射渲染正确、（Decision①时）notificationType/status 筛选各 ≥1 断言
- [ ] 未读/已读 tab 回归断言（数据面不回归）+ 既有 E2E 回归零失败

Exit Criteria:

- [ ] 新断言通过 + 回归零失败（命令与 spec 路径落计划执行记录）

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION（agent_18b3414c，2026-10-06）——①刷新按钮基线错误（已是四源链，原文按文执行会回归）②未对齐 inbox-patterns.md:46/:50 owner doc 裁决 ③data-source 计数（三个→四个）④Targets 漏 IErpSysNotificationBiz.java ⑤Owner Docs 引用不存在的 docs/architecture/notify-*.md ⑥Deferred 缺触发条件 ⑦status 白名单 PENDING 值域缝隙 → 全部采纳修订
- Independent draft review iteration 2: ACCEPT (agent_e418a158, 2026-10-06) — 迭代 1 全部要求实证级修复核验通过，无残留

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（inbox-patterns.md 若 Decision 偏离 §2 形态则同步；否则记录「实现向 §2 收敛」）
- [ ] 已运行验证（触域 JUnit[如触后端] + validate:flux 零新增 + E2E 新断言 + 回归零失败）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### findUnread/findRead 无界与 countUnread 全量 .size()

- Classification: `watch-only residual`
- Why Not Blocking Closure: per-user 个人量级小；改签名/返回形态波及既有两 tab 与 countUnread 消费面，收益低
- Reopen Trigger: per-user 通知量级实测超限（如 >5000）或 countUnread 出现实测耗时证据

## Closure

Status Note: (pending)

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (none)
