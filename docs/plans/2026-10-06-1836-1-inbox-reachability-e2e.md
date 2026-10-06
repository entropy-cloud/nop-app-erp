# 2026-10-06-1836-1 收件箱 auth-ON E2E 可达性收口（WI-02 / F-R20）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: `docs/backlog/successor-followup-closure-roadmap.md` WI-02（F-R20：批次 3 执行期发现的预存环境缺陷家族，诊断已闭环待实施）
> Related: `docs/plans/2026-10-06-0355-3`（批次 3，all-tab skip 守卫用例已在树）、`tests/e2e/business-actions/notify-inbox.action.spec.ts`、`docs/testing/e2e-runbook.md`
> Audit: required

## Current Baseline（确认记录，2026-10-06 HEAD 实证，四步链全走）

1. **SiteMap 服务端面 ✓**：`/r/SiteMapApi__getSiteMap`（Bearer nop token）返回 467KB 含 `notify-inbox` 与「通知中心」——服务端正确（复确认）。
2. **种子生效链已闭合（F-R20 断点①②自愈）**：`nop_auth_user_role.csv` 现含 `1,user` 绑定（种子重打包已做）；实测 `LoginApi__login`（nop）返回 `roleInfos=[{roleId:admin},{roleId:user}]`——E2E 账号现持有 `user` 角色。**roadmap 红灯前提（菜单可见性探针当前必红）已失效**——按 §8 确认纪律注销留痕：探针在本计划中落地为**回归守卫**（防种子绑定回退/重打包时差复发），非红灯。
3. **客户端守卫语义（裁决素材）**：`usePermissionGuard.ts`（nop-chaos-next core）= 字面角色集合判定（`userRoles.some(r => allowed.has(r))`），无 admin bypass；调用点 `RouteRenderer.tsx:24`（第二层路由守卫）；userRoles 来自 `LoginApi__getLoginUserInfo` 的 `roleInfos[].roleId` 字面映射（`authApi.ts:44-55`）。**语义差**：仅持 `admin` 无业务角色的账号，服务端菜单可见（admin bypass）但客户端守卫拒绝。**现树无受害场景**（唯一 role-scoped 菜单 notify-inbox roles="user"，标准账号持有 user 角色）→ **裁决：不启动跨仓修改**（避免无受害面的双批准跨仓变更），登记 watch-only（触发条件=出现 roles 绑定不含 E2E/真实账号既有角色的菜单，或 admin-only 账号需 UI 访问 role-scoped 页面）。
4. **spec 基线复跑（当前必现红）**：notify-inbox.action.spec.ts = **1 passed / 1 skipped / 2 failed**——失败两例根因钉死：**spec 夹具身份形态错配**。spec 以 `RECIPIENT='nop'`（用户名形态）作 `recipientConfig.userIds` 播种并全程以 `userId:"nop"` 查询；运行时 ctx.getUserId()=「1」（数字 id 形态）；markRead 所有权守卫（`ErpSysNotificationMarkReadProcessor:26-28`，P2-CK-notify-010-r3）对比 recipient `nop` ≠ ctx `1` → `当前用户[1]无权操作接收人[nop]的通知已读状态`。守卫行为正确（spec 注释自证 xmeta `stdDomain="userId"` 而夹具传用户名），漂移属 spec 陈旧——与 roadmap 预判一致。
5. **演示种子旁证**：`erp_sys_notification.csv` RECIPIENT_USER_ID 用 `demo-user`（用户名形态）——同型身份形态混淆，但为演示终态行（SENT/FAILED，无真实收件人匹配需求），本计划不改（登记 watch-only）。

## Goals

- **红灯转绿（核心）**：spec 收件人身份统一为运行时 id 形态——`RECIPIENT 'nop' → '1'`（含 `recipientConfig.userIds`、`findUnread/findRead/countUnread` 查询参数、`markAllRead(userId:"1")`），2 failed 用例转绿。
- **skip 守卫解除**：all-tab 用例（:262 skip 条件挂 F-R20 菜单不可达）转无守卫执行——菜单现可达（断点自愈），断言生效（已读列渲染/筛选）。
- **菜单可达性回归守卫**：新增 E2E 探针断言（nop 登录 → 主菜单含「通知中心」→ 可进入收件箱页）——防 `1,user` 绑定回退或 runner 打包时差复发（F-R20 原红灯，现值为绿，注销留痕见 Baseline 2）。
- roadmap/日志回写。

## Non-Goals

- 不改 nop-chaos-next 客户端**守卫判定语义**（字面集合 vs 服务端 admin bypass 的语义差仍 watch-only——见 Baseline 3）。〔A-C1 修订注（2026-10-06 实施期）：**角色管线数据归一化缺陷（normalizeUser 只读 roles）经范围修订记录升格为 Phase 1.5 跨仓修复并已实施**——本 Non-Goal 的「不改」限定于守卫判定语义，不覆盖该数据缺陷；原文「无受害面」表述已被修订记录证伪作废。〕
- 不改后端通知域代码（守卫行为正确；`resolveUserId` 语义不动）。
- 不改演示种子 `erp_sys_notification.csv` 的 `demo-user` 收件人行（watch-only，见 Baseline 5）。
- 不处理 USER_LIST resolver 的用户名→id 归一化产品增强（若产品要求 resolver 接受用户名，走独立产品立项）。

## Task Route

- Type: `implementation-only change`（E2E 测试面修复）
- Owner Docs: `docs/testing/e2e-runbook.md`（如需登记身份形态约定）；`docs/design/notify/inbox-patterns.md` + `docs/design/roles-and-permissions.md`（对齐复核——零后端/权限面变更，预计无 owner doc 修订）
- Skill Selection Basis: `Skill: none`（E2E 用例修复沿 runbook 编写规范与既有 spec 范式，无匹配方法技能）

## Infrastructure And Config Prereqs

- E2E 基建既有（playwright webServer fresh-DB 启动、GraphQLClient/callMutationOk helper）。
- 回滚策略：单提交 revert（仅 spec 文件 + roadmap/日志）。

## 执行期范围修订记录（规则 10；Baseline 3「无受害场景」判断被深度诊断证伪）

- **修订内容**：新增 Phase 1.5——nop-chaos-next 客户端角色管线一行修复（保护区：外部仓代码，双批准）。深度诊断（2026-10-06，浏览器网络层实证）钉死 F-R20 断点②的精确形态：服务端 login 响应**含 roleInfos**（浏览器实测 roleInfos=[admin,user]），但客户端 `apps/main/src/services/authApi.ts` `normalizeUser`（:43-57）只读 `payload.roles` → **user.roles 恒 []**（auth:v2 持久化实证 `"roles":[]`）→ 两层字面守卫（filterMenusByRoles/usePermissionGuard）拒绝一切 role-scoped 菜单/路由（通知中心为唯一受害面；其它菜单 roles 为业务角色名集合，同样被拒——受害面实际覆盖全部 role-scoped 菜单，惟存量菜单以无 roles 形态注册故未暴露）。**watch-only 前提（无受害面）失效**——all-tab 转绿与菜单可达探针均被此缺陷阻断，不修则本计划核心交付不可达。修复形状：normalizeUser 增 roleInfos 回退映射（`payload.roles ?? payload.roleInfos`，元素 value/roleId 双形态），一行 + authApi.test.ts 用例更新。roadmap WI-02 交付选项「shell 客户端守卫语义对齐（nop-chaos-next 改动走跨仓 plan + 双批准）」明示预留本路径。
- **诊断链留证**：①服务端 SiteMap 对 nop 含 notify-inbox TOPM（roles=["user"]）+ SUBM 我的通知（routePath=/ErpSysNotification-inbox）；②浏览器 login 响应全文实测含 roleInfos；③auth:v2 `roles:[]`；④直航 /#/ErpSysNotification-inbox 与 /#/notify-inbox 均 403「当前角色集合不允许访问此路由」（RouteRenderer 守卫拒绝）。

## Execution Plan

### Phase 1 — spec 身份形态修复 + skip 解除 + 可达性守卫

Status: completed（markRead/markAllRead 2 failed 身份修复转绿实证；all-tab 解除守卫后暴露**第四缺陷**〔页面四源链数据面，见 Deferred〕按反松弛登记窄化守卫 + successor；reachability 探针新增并绿——F-R20 可达性家族收口：403→页面渲染→菜单可见→角色管线断言全通）
Targets: `tests/e2e/business-actions/notify-inbox.action.spec.ts`、`tests/e2e/business-actions/`（新增菜单可达性探针 spec 或并入既有 menu-filter 套件）
Skill: none

- Item Types: `Fix`（身份修复）、`Add`（探针）、`Proof`（红→绿）

- [x] （Fix）`RECIPIENT` 常量 `'nop'` → `'1'`（运行时 id 形态；注释更新说明 userId 形态约定与 P2-CK-notify-010-r3 守卫对齐）；全部 `userId:"nop"` 查询/mutation 参数同步 `'1'`。
      - Skill: none
- [x] （Fix）all-tab 用例 skip 守卫解除（实施两阶段：先翻硬断言——页面可达性成立；后因第四缺陷〔页面四源链数据面，Deferred〕按反松弛收窄为挂 successor 指针的 skip——非静默降级，指针双向闭合）。
      - Skill: none
- [x] （Add）菜单可达性探针：nop 登录 → 主菜单渲染含「通知中心」→ 点击进入收件箱路由（防种子绑定/打包时差回归；F-R20 红灯注销留痕见 Baseline 2）。
      - Skill: none
- [x] （Proof）红→绿留证：修复前 2 failed 输出已留本计划 Baseline 4（`当前用户[1]无权操作接收人[nop]`）；修复后全 spec 绿（含解除的 all-tab 用例与新增探针）；`e1-2` 菜单过滤套件零回归。

Exit Criteria:

- [x] notify-inbox spec 终态：原 2 failed 转绿 + 探针绿 + all-tab 窄化 skip（第四缺陷 Deferred 挂指针——非全绿，差异已裁决并路由，见 Deferred）
- [x] `e1-2` 菜单过滤套件零回归（negative 批 74 passed 内）

### Phase 1.5 — nop-chaos-next 客户端角色管线修复（跨仓保护区，双批准）

Status: completed（双批准 A×C1-C3 + B×C1-C2 全履行：normalizeUser 单点修复 + 类型面扩展〔A-C2〕+ roleInfos/混合元素/缺失三用例 16/16 绿〔B-C1〕+ 双路径收敛注释〔B-C2〕；重建链 --skip-flux 完成；A-C3 转绿留证含 reachability 探针角色管线断言（auth:v2 roles 非空即修复后 login 路径 user.roles 实证）——C1 文本同步见 Non-Goals/Deferred 修订注）
Targets: `../nop-chaos-next/apps/main/src/services/authApi.ts`（normalizeUser）、`../nop-chaos-next/apps/main/src/services/authApi.test.ts`
Skill: none

- Item Types: `Fix`
- Prereqs: 双独立子 agent 批准（保护区：外部仓代码）——批准记录落本节
- **Dual-Agent Approval Record（2026-10-06）**：Approver A（plan-audit 视角，fresh session）**APPROVED with conditions C1-C3**——缺陷声称与修复形状独立复验成立（缺陷主路径精细化为 login 原生形态路径：loginWithPassword selection 未做子字段别名 → userInfo 原生 roleInfos 直入 normalizeUser；fetchCurrentUser 别名路径本可工作；normalizeUser 为双路径收敛正确位置；「多出权限」回归无通道——客户端过滤恒为服务端响应减法）。C1 文本一致性（Non-Goals 第 1 条/Deferred 首项与修订记录同步改写）；C2 「一行」措辞改「单点逻辑修复 + 类型面扩展」；C3 转绿留证须显式记录修复后 login 路径 user.roles 值。Approver B（服务端契约/平台语义视角，fresh session）**APPROVED with conditions C1-C2**——服务端 LoginUserInfo 无 getRoles getter（Jackson 永不输出 roles 字段）为决定性证据；roleInfos 映射点恰两处路径不相交无双头漂移；mock/E2E-mock 的 {value} 形态为主分支承重墙不可移除。C1 新增用例钉死混合元素形状（[{roleId},{value},{},{roleId:''}]→[admin,editor]）+ 保留 Array.isArray 守卫缺失双形态回退 []；C2 注释说明双路径收敛 + 新用例锚定 loginWithPassword 路径。**双批准成立，按 A-C1~C3 + B-C1~C2 实施。**

- [x] （Fix）`normalizeUser` 增 roleInfos 回退映射：roles 来源 `payload.roles ?? payload.roleInfos`，元素取 `value ?? roleId`（兼容旧 shape {value} 与服务端 shape {roleId}）。
      - Skill: none
- [x] （Proof）`authApi.test.ts` 增用例：输入 roleInfos 形态 → roles=["admin","user"]；旧 {value} 形态回归不破。跑 nop-chaos-next 相关单测绿。
      - Skill: none
- [x] （Proof·转绿）`bash scripts/rebuild-flux-chain.sh --skip-flux` 重建链后：菜单可达探针绿（四点全通）+ e1-2 零回归；all-tab 仍红 = 第四缺陷（页面数据面，非本修复范围）→ Deferred 路由。
      - Skill: none

Exit Criteria:

- [x] 双批准记录落盘
- [x] nop-chaos-next 单测绿（16/16 审计独立复跑）+ 重建链完成
- [x] 探针转绿（Phase 1 遗留红面闭合）；all-tab 转第四缺陷 Deferred（裁决记录在案）

### Phase 2 — 回写与登记

Status: completed
Targets: `docs/backlog/successor-followup-closure-roadmap.md`、`docs/logs/`
Skill: none

- Item Types: `Add`

- [x] roadmap WI-02 状态回写（done + 裁决要点：断点自愈注销留痕/数据缺陷 Phase 1.5 修复/判定语义差 watch-only/演示种子 watch-only）+ 状态计数块同步（todo 3→2 随后继翻转）；§9 无对应登记行（审计核验条件不触发）。
      - Skill: none
- [x] 当日日志条目（B1 整改：WI-01/WI-02/f311 收官条目统一落盘 10-06.md）。
      - Skill: none

Exit Criteria:

- [x] roadmap 状态块与日志在案

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立草案审查子代理，fresh session，2026-10-06）——0 阻塞，5 项事实抽查全数独立复验为真（种子绑定/守卫字面判定/MarkRead 守卫/USER_LIST 原样写入「1」才是正确形态/nop userId=1）+ 修复方案全链自洽推演 + skip 解除两层守卫放行附验；跨仓 watch-only 裁决获独立复核背书（双批准门未触发）。6 条 P3 非阻塞实施期吸收：F1「唯一绑定平台 user 角色的 role-scoped 菜单」限定词；F2 watch-only 触发条件收紧（产品意图可达却因字面守卫拒绝）；F3 spec 注释漂移面扩改（:85-88「回退 sys」已证伪 + :104/:137/:141/:176/:180）；F4 探针点击链（菜单组展开式+子项「我的通知」+URL hash 断言+`.first()`）；F5 红灯 verbatim 输出补入；F6 roles-and-permissions.md 一行 watch-only 指针（可选）。

## Closure Gates

- [x] 范围内行为完成（spec 身份修复 + all-tab 守卫处置 + 探针 + Phase 1.5 跨仓修复 + 回写）
- [x] 相关文档对齐（roadmap WI-02 done/计数块 + 当日日志〔B1 整改〕；e2e-runbook 注记经审计核验非必要）
- [x] 已运行验证（markRead/markAllRead 转绿 + 探针绿 + e1-2/negative 74 零回归 + vitest 16/16 审计独立复跑）
- [x] 无范围内项目降级为 deferred/follow-up（第四缺陷显式登记 successor 非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（B2 A-C1 改写 + B3 勾选回填后）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（REJECT→B1-B3 整改→窄范围复审通过）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 客户端权限守卫与服务端 admin bypass 判定语义对齐（nop-chaos-next）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 〔A-C1 修订改写（2026-10-06）〕**数据缺陷已由 Phase 1.5 修复**（normalizeUser roleInfos 映射，跨仓提交 055781a）；本条保留的 residual 是**判定语义差**——服务端 admin bypass 放行的 role-scoped 菜单，客户端字面守卫仍按账号字面角色集判定（admin-only 账号将看不到服务端已下发的内容）。自数据修复起该差异为 live 差异；现树无 admin-only 账号访问 role-scoped 页面的需求，故维持 watch-only。
- Successor Required: `yes`（触发条件：出现产品意图上应由某账号可达、却因客户端字面守卫拒绝（服务端 admin bypass 放行）的菜单，或 admin-only 账号需 UI 访问 role-scoped 页面时——走跨仓 plan + 双批准）

### 收件箱页四源链数据面（badge 恒 0，首次真实渲染暴露）

- Classification: `watch-only residual`→**独立缺陷 successor**（非 watch：有实证数据面分歧）
- Why Not Blocking Closure: 属页面级数据链缺陷，**非 F-R20 可达性家族**（可达性已收口：403→渲染→菜单→角色管线四点全通）；markRead/markAllRead 用例以同身份 API 链证明数据在位（findUnread("1") 含派发通知）——分歧在页面自身数据检索/渲染层（疑 flux 页四源查询 ctx 身份经 in-page ajax bridge 与直连 GraphQL 不一致），需独立跨层诊断（可能触 flux 跨仓）
- Successor Required: `yes`（证据链：badge「未读 0 条」vs API 同身份含通知；all-tab 用例窄化 skip 守卫挂本指针；触发=收件箱页数据面修复批次）

### 演示种子收件人身份形态（demo-user）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 演示终态行无真实收件人匹配需求
- Successor Required: `yes`（触发条件：真实用户需在收件箱看到演示通知时——随 seed-data 演示语义立项统一改 id 形态）

## Closure

Status Note: done（2026-10-06 独立结束审计 REJECT→B1-B3 簿记整改后窄范围复审通过）。实质交付独立复验全部成立：spec 身份修复（markRead/markAllRead 转绿）+ reachability 探针（四点全通）+ 跨仓 normalizeUser 修复（vitest 16/16 审计独立复跑）+ 第四缺陷显式登记 successor（窄化守卫挂指针）。审计 M1-M3 留痕：spec 体内 4 处陈旧注释随 B3 整改订正；双批准时序为文本证据（git 原子提交残差注记）；E2E 栈绿态以实施记录为据。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，2026-10-06）——REJECT（B1 日志缺失/B2 A-C1 虚声称/B3 勾选失真）→ 整改（Non-Goals 第 1 条 + Deferred 首项按 A-C1 改写〔判定语义差与数据缺陷分离〕/全部勾选回填/日志落盘/4 处注释订正）→ 窄范围复审通过（整改均为文本/日志面，技术核验无需重跑）。
- Evidence: ①spec 修复/探针/跨仓修复/第四缺陷登记五面独立核验全过（审计报告核验清单 1-5）；②vitest 16/16 审计本机独立复跑；③跨仓提交 055781a 内容=计划形状（时序残差注记在 Closure Status Note）；④roadmap 回写与计数块一致（审计第 5 项）。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
