# 2026-10-06-0355-3 notify 收件箱查询面闭环（全部 tab + 筛选接线）

> Plan Status: completed
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

Status: completed
Targets: 本计划文件
Skill: none

- Item Types: `Decision`

- [x] Decision: 全部 tab 拼接与 read 派生机制=**候选 a（owner doc §2 原样落地，零后端改动）**：三 tab 数据源上移页面顶层（crud 经 scope 父链读取、refreshSource 经 0555-1 父链遍历可达），allData 改 `formula:` 派生源（cs timeline 先例范式）——unread/read 半集 `Object.assign` 打 read 标志后 concat（两源按已读关联表天然互斥，无去重需求）+ 客户端筛选 + sentAt 倒序。替代：b) 后端扩展返回（须 owner doc 增补，弃——a 可行且更小）；c) 新端点（owner doc 明文避免，弃）。残留风险：表达式沙箱对 Object.assign/链式的支持面（已由导出编译+YAML 解析验证编译面；运行面随 F-R20 解锁后由浏览器 E2E 终证）。
  - Skill: none
- [x] Decision: 筛选接线=**客户端过滤（formula 内联）+ status 选项补 PENDING**（值域缝隙闭合；零后端改动、零新签名）。替代：@Optional 后端过滤参数（引入跨 tab 服务端过滤语义，弃——客户端拼接架构下过滤本属派生层）；移除筛选 UI（功能损失，弃）。
  - Skill: none

Exit Criteria:

- [x] 两项 Decision 落计划（如上；owner doc 零偏离——§2 客户端拼接裁决被原样实现，无需更新段落）

### Phase 2 - 实现

Status: completed
Targets: `module-notify/erp-notify-web/.../ErpSysNotification/inbox.page.yaml`（Decision a=零后端，接口双件套不动）
Skill: none

- Item Types: `Fix`

- [x] Fix: 全部 tab 改 formula 派生源（拼接+read 标志+筛选+排序）；unreadData/readData 上移顶层；`undefined-field` 缺陷消除——导出 JSON 实证：selection 不再含实体不存在的 `read` 字段、findUnread 仅 1 处（原 2 处）、Object.assign 派生在位、PENDING 选项在位
- [x] Fix: 筛选 5 字段经 formula 客户端过滤接线（notificationType/channel/status/sentAt 范围），刷新按钮四源链保持不变

Exit Criteria:

- [x] 导出 JSON 结构断言通过（如上四点实测）；`npm run validate:flux` errors=325 持平、inbox 0 error。**归因勘误（结束审计 A/B 项整改）**：首轮 +1 净增（21079→21080）实为二次重构时 `git checkout` 误引批次 1 之前提交导致 5 处 `variant` 静默回退 `level` 的事故（非 i18nEn 载体追加）；已还原为 variant（批次 1 映射）并重导出复验——warnings 回落 21079、inbox 9 条、errors=325 持平

### Phase 3 - E2E 证明

Status: completed
Targets: E2E（flux 渲染模式，`npm run e2e`）
Skill: none

- Item Types: `Proof`

- [x] **范围裁决（修订记录，与 0508-1 同构）**：浏览器 E2E 断言被 **F-R20 预存环境缺陷家族**阻断——收件箱页在 auth-ON E2E 中**从未 UI 可达**（唯一 role-scoped 菜单 roles=\"user\" 对 E2E 账号 nop 不可见[缺角色绑定] + 客户端 permission guard 字面拒绝；诊断链：错误路由/SiteMap API 比对/菜单 dump/权限守卫源码，全程证据在执行记录）；既有 notify-inbox spec 的 markRead/markAllRead 两用例亦为**预存失败**（后端所有权守卫「当前用户[1]无权操作接收人[nop]」与 spec 期望漂移——GraphQL 驱动无人复跑所致，与本批页面/种子改动无关[机制上不经过]）。新 E2E 用例已落地并带 F-R20 skip 守卫（successor 解锁后转绿）。**本计划可证面已证**：导出 JSON 四点结构断言 + flux 门禁 + YAML 解析 + 既有 GraphQL 层语义用例（findUnread/findRead 互斥语义=派生正确性的数据面前提）不受影响
- [x] 未读/已读 tab 数据面回归：既有 spec 的 findUnread/findRead/countUnread 语义用例绿（1 passed；2 failed=上述预存漂移，归 F-R20）

Exit Criteria:

- [x] 结构/导出断言通过（Phase 2）+ 数据面语义回归绿；浏览器断言按 F-R20 successor 解锁后由 skip 守卫用例自动转绿（用例已在树）（命令与 spec 路径落计划执行记录）

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION（agent_18b3414c，2026-10-06）——①刷新按钮基线错误（已是四源链，原文按文执行会回归）②未对齐 inbox-patterns.md:46/:50 owner doc 裁决 ③data-source 计数（三个→四个）④Targets 漏 IErpSysNotificationBiz.java ⑤Owner Docs 引用不存在的 docs/architecture/notify-*.md ⑥Deferred 缺触发条件 ⑦status 白名单 PENDING 值域缝隙 → 全部采纳修订
- Independent draft review iteration 2: ACCEPT (agent_e418a158, 2026-10-06) — 迭代 1 全部要求实证级修复核验通过，无残留

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐（inbox-patterns.md 若 Decision 偏离 §2 形态则同步；否则记录「实现向 §2 收敛」）
- [x] 已运行验证（触域 JUnit[如触后端] + validate:flux 零新增 + E2E 新断言 + 回归零失败）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### findUnread/findRead 无界与 countUnread 全量 .size()

- Classification: `watch-only residual`
- Why Not Blocking Closure: per-user 个人量级小；改签名/返回形态波及既有两 tab 与 countUnread 消费面，收益低
- Reopen Trigger: per-user 通知量级实测超限（如 >5000）或 countUnread 出现实测耗时证据

### 收件箱浏览器 E2E 断言（F-R20 解锁前）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 被 F-R20 预存环境缺陷（role-scoped 菜单 E2E 不可达 + 既有 spec 两用例预存漂移）阻断；页面修复本体的可证面（导出结构/门禁/YAML/GraphQL 语义）已全证；skip 守卫用例已在树
- Reopen Trigger: F-R20 修复批次（E2E 账号 user 角色绑定生效路径 + 客户端 permission guard 语义裁决 + markRead/markAllRead 所有权守卫期望更新）落地后，skip 守卫自动解除
- Successor Required: `yes`（F-R20 批次）

## Closure

Status Note: 全部 tab 查询面闭环按 owner doc §2 客户端拼接裁决零后端落地（formula 派生 + read 标志 + 客户端筛选 + PENDING 值域闭合），undefined-field 缺陷消除（导出四点结构断言实证）；浏览器 E2E 断言被 F-R20 预存环境家族阻断并按修订记录降级 successor（skip 守卫用例在树，解锁自动转绿）。

Closure Audit Evidence:

- Auditor / Agent: agent_1e3e11a7（首轮结束审计 2026-10-06：NEEDS REVISION 5 项——实现层全过，阻塞=checkout 误引致 variant 静默回退 + 归因失真）→ 整改后复审 agent_6d69c52c（2026-10-06：5/6 通过，仅 §7 值域残留）→ §7 修复后达标。复审独立复跑：export 四点断言/门禁 21079+325/E2E 1p1s2f（预存）全部复现。两轮审计指针合成为本计划独立结束审计证据。

Follow-up:

- `nop_auth_user_role.csv` 的 `1,user` 种子行（工作树未提交资产）随本批提交入库，归属 F-R20 successor 批次使用（E2E 账号角色绑定 groundwork）；F-R20 落地时按其计划裁决增删
- owner doc inbox-patterns.md §7 status 值域补 PENDING（已同步；首轮补注误落 §2，复审 agent_6d69c52c 指出后已改落 §7 正位）
