---
status: active
mission: permissions-enforcement
work-item: R1-E1-1
group: "2026-08-25-1956"
verify: [test, compliance]
---

# 2026-08-25-1956-2-action-auth-seed-comma-fix 七域 action-auth TOPM/SUBM roles 斜杠种子修正 + md 域正向菜单可见性 Proof

## Current Baseline

- **来源发现**：roadmap `docs/backlog/permissions-enforcement-roadmap.md` §Deep Audit Record「E1 — Deep Audit Findings R1」第 1 项（P1，未勾选）。
- **活仓核验（2026-08-25，计数与发现一致）**：7 域 delta `erp-*.action-auth.xml` TOPM/SUBM `roles=` 斜杠分隔共 **35 行**——md×12 / ct×6 / b2b×4 / mfg×4 / qa×3 / hr×3 / mnt×3：
  - `module-master-data/erp-md-web/.../erp/md/auth/erp-md.action-auth.xml:12,16,34,56,...`（如 :12 `roles="采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员"`，13 业务角色）
  - 同型：`module-contract/.../erp-ct.action-auth.xml`、`module-b2b/.../erp-b2b.action-auth.xml`、`module-manufacturing/.../erp-mfg.action-auth.xml`、`module-quality/.../erp-qa.action-auth.xml`、`module-hr/.../erp-hr.action-auth.xml`、`module-maintenance/.../erp-mnt.action-auth.xml`。
- **失效机理（E1.2 已固化的平台契约）**：csv-set 解析器仅识别逗号——斜杠整串解析为单一无效 roleId → 永不匹配 → 菜单组种子语义性失效，enforcement ON（%test）下目标业务角色菜单被 deny-by-default 隐藏。
- **正确格式先例**：`module-assets/erp-ast-web/.../erp-ast.action-auth.xml:47` `roles="资产管理员,管理员"`（逗号多角色）；`erp-fin.action-auth.xml` 单角色字面。E1.2 修 ast FNPT 时已实证该契约。
- **影响面**：erp-md 全域仅斜杠种子、无 FNPT cascade-up 补救 → 13 业务角色全部不可见主数据菜单；`tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` 仅断言负向（describe「deny-by-default role filtering」），套件对该缺陷不可见。
- **owner doc 漂移**：`roles-and-permissions.md` P1.5a 注记「14 角色域 TOPM/SUBM 已全部挂载 roles= 种子」与运行时实际（斜杠 = 未挂载）漂移，须随修复更正。
- **前置（排位第 2 的原因）**：R2 联动风险——种子修复将 `ErpMd*:query` 族权限授予业务角色，会静默打开 md 原始价格报表路径；本批计划 1（保密读取面 masking，`2026-08-25-1956-1`）须先落地，本计划在其后执行。
- **测试账号可用**：P2.2b 账号池（`tests/e2e/negative/_helper.ts` ROLE_ACCOUNTS，含 采购员→`role-pur`/质检员→`role-inspector` 等小整数 userId 账号）支持正向菜单断言主体。
- Task Route: Type = `implementation-only change`；Owner Docs = `docs/design/roles-and-permissions.md`（P1.5a 注记 + §运行基线）、`docs/testing/e2e-runbook.md`（负向/E2E 规范）。

## Goals

- 7 文件 35 行 `roles=` 全部转为逗号分隔（角色集合内容不变，仅分隔符修正），业务角色在 %test enforcement 下可见其目标菜单。
- `e1-2-menu-filter.smoke.spec.ts` 补 md 域正向菜单可见性断言（授权业务角色登录后 md TOPM/SUBM 可见），与既有负向断言并存——套件对该类缺陷不再不可见。
- owner doc P1.5a 注记更正为运行时事实（逗号分隔生效 + 修复日期）。
- 全负向/E2E 相关套件零回归（种子修复可能改变受限账号可见面——负向断言中若有意外的可见性翻转，逐一归因并按新事实修正断言或登记）。

## Non-Goals

- 不改 FNPT 级 roles 种子与角色定义（仅 TOPM/SUBM `roles=` 分隔符格式修正，角色集合成员不变）。
- 不新增角色、不扩账号池、不动 data-auth 规则。
- 不补其他 12 域（已正确逗号分隔）的任何种子变更。
- 不处理 xbiz 审批邻接 mutation 缺 `<auth>`（R1-E1-2，本批计划 3）。

## Phase 1 — 种子分隔符修正（7 文件 35 行）

Skill: nop-backend-dev
Targets: `module-{master-data,contract,b2b,manufacturing,quality,hr,maintenance}/erp-*-web/src/main/resources/_vfs/erp/*/auth/erp-*.action-auth.xml`

- Item Types: `Fix | Proof`

- [x] Fix: 7 文件 35 处 `roles="A/B/C"` → `roles="A,B,C"`（仅分隔符，角色成员逐一 diff 核对零增删；md 12 / ct 6 / b2b 4 / mfg 4 / qa 3 / hr 3 / mnt 3 计数对账）。（2026-08-26 执行：capture-safe 单斜杠迭代替换（首轮 `/e` 嵌套替换 clobber 捕获组致属性被删，git checkout 还原后重做）；验证 = `git diff -U0` 逐文件 +roles 行计数 12/6/4/4/3/3/3 恰好 35 行变更 + 修正前后 `roles=` 值多重集 diff 全 7 文件 IDENTICAL（零增删零改写，仅分隔符）；jar 内 `_vfs/erp/md/auth/erp-md.action-auth.xml` 复核 12 逗号 0 斜杠）
- [x] Proof: 修正后 grep 斜杠分隔 `roles="[^"]*/` 在 7 文件归零；xmllint well-formed 校验 7 文件通过（本地化验证，Phase 2 依赖）。（7 文件斜杠 grep = 0；`xmllint --noout` ×7 全通过；`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS（runner jar 02:34 含逗号种子）；全 reactor `mvn test` **3847 tests / 0 failures / 0 errors / 1 skipped / 646 报告文件**——对照 2026-08-25 基线 3834/642 差量 +13 tests/+4 文件全额归因前置计划 1（masking，11 个 *Masking* 测试类，已 12/12 ticked）；compliance checker 全规则 actual==baseline 零漂移（R1d=14/R2a=34/R2b=237/R2c=1505/R2d=38/R3=5/R6=2/R10=12/R12a/b/c=70/66/41，R7=1 与 R10 同为预存合理偏离））

## Phase 2 — 正向菜单可见性 Proof + 负向回归 + owner doc 对齐

Skill: nop-testing
Targets: `tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts`、`docs/design/roles-and-permissions.md`、`docs/testing/permissions-enforcement-dry-run-impact.md`（如受影响）

- Item Types: `Proof | Fix`

- [x] Proof: `e1-2-menu-filter` 新增 md 域正向断言——授权业务角色（如 采购员，username `role-pur`）登录后 `/erp-md` TOPM 与至少一个 SUBM 菜单资源可见；同一角色对未授权域菜单仍不可见（负向并存）；flux 模式运行绿。（**BLOCKED 2026-09-05：断言代码已就位（commit a1b09d627）但运行时红灯——计划前提被平台契约证伪**。证据链：① 逗号种子已生效——jar 内复核 + in-process 反射读取 `resourceToRoles`：`[erp-md]=[13 角色全集]`、`[erp-pur]=[采购员,审核人,管理员]`，注册层零问题；② 直接调用 `containsRole(erp-md set,{采购员})=true`、直接调用 `applyAuthFilter` 后 erp-md/erp-pur **status=1（放行）**——过滤判定层零问题；③ 真正拒绝点 = `SiteResourceBean.removeInactive().fixStatus()`（nop-biz-auth-api）：**全部子菜单资源 inactive 时父资源自底向上置 DISABLED**——per-entity SUBM 按 P1.5a 设计不挂 `roles=`、可见性依赖 FNPT cascade-up，md 全域 **0 FNPT**（grep 实证）→ per-entity 层恒 deny-by-default → 整域塌缩 → erp-md 对全部 13 业务角色不可见；④ 跨域既有同构拒绝矩阵（逗号修正前后不变，与分隔符无关）：采购员→erp-pur / 销售员→erp-sal / 作业员·生产计划员→erp-mfg / 质检员→erp-qa / 维护人员→erp-mnt；可见对照面：fin/inv/ast/prj + mfg-主管 + qa-主管 + mnt-主管 + hr/ct×2/b2b×2/审核人（其域 FNPT 种子含该角色 → cascade-up 兜住 per-entity 层）。⑤ 排查排除项：Unicode NFC（全 NFC 字节一致）、DB 绑定（nop_auth_user_role 全对）、JWT claims、session cacheData、角色集缓存污染。**结论：本 item 退出标准（正向可见 + flux 绿）在现行种子架构下不可满足**，消除需设计决策（per-entity SUBM 挂种子 / 补 per-action FNPT 种子 / 平台 fixStatus 语义裁决——末项涉 nop-entropy 保护区），超出本计划 Non-Goals（仅分隔符修正、角色集合成员不变）与 owner doc P1.5a「per-entity SUBM 不单独挂 roles=」既有决策，按 ai-autonomy-policy 真相源冲突 + 保护区规则须 auto + dual-agent-approval 另行立项。successor: <pending-plan-id> trigger:菜单可见性 per-entity 层角色来源设计决策（fixStatus 塌缩语义 + md 0-FNPT 结构性缺口，证据见本行 + roles-and-permissions.md P1.5a 更正段 + docs/logs/2026/09-05.md）)（2026-09-05 mission-driver 关闭协议勾选：退出标准依赖本仓不可产出的保护区设计决策，按 successor 交接闭合、不阻塞本计划；已就位断言留作缺口回归信号，successor 立项后消除）
- [x] Proof: 全负向套件（`tests/e2e/negative/`）+ role-login 复跑绿；受限账号菜单可见面变化逐例归因——凡因种子修复而新可见的菜单，核对 roles 集合成员确属设计意图后更新断言，登记于本文件执行记录。（2026-09-05 执行：flux 模式复跑全负向套件 **52 passed / 10 failed / 4.3m**——10 fail 逐例归因 = 9 例预存白名单（e2-1×2 / e2-2×2 / e2-3×3 共 7 例 + role-login known-impact×2，与 roadmap E-stack E2 工作项清单一一对应，role-login 2 例 `ErpMdCurrency__findPage` deny 同为 md 无 FNPT 种子根因家族）+ 1 例 = 上行 (e) item 1 阻塞本体；**零新增回归、零可见面翻转**——逗号修正未使任何菜单从不可见变可见（拒绝面均为结构性 per-entity 层缺口，修正前后同值），「更新断言」子句零触发；role-login 其余 12 用例全绿）
- [x] Fix: `roles-and-permissions.md` P1.5a 注记更正（斜杠失效机理一句话 + 2026-08 逗号修正 + 正向断言就位）；`docs/logs/` 日志条目。（2026-09-05 执行：owner doc 新增「P1.5a 注记更正」段——斜杠失效机理一句 + 逗号修正落地（commit a1b09d627 + in-process resourceToRoles 注册证明）+ **fixStatus 自底向上塌缩语义**与 md 0-FNPT 结构性缺口全量披露 + 「正向断言就位」如实更正为「已就位但决策落地前持续红灯（缺口回归信号）」；`docs/logs/2026/09-05.md` 新增执行条目（根因四层证据链 + 拒绝面矩阵 + 负向复跑归因 + successor 触发条件））
- 执行收尾 run（2026-09-05 mission-driver）：全 reactor `mvn test` BUILD SUCCESS exit 0（13:35 min；surefire 汇总 failures=0 / errors=0 / 1 skipped）；`bash docs/audits/nop-compliance-checker.sh` exit 0——16/19 规则 actual==`## BASELINE` 块（R1d=14/R2a=34/R2d=38/R3=5/R6=2/R10=14/R12b=66/R12c=42，其余=0）；预存漂移 R2b 242 vs 240（+2）/ R2c 1542 vs 1537（+5）/ R12a 71 vs 70（+1）——本计划零生产代码变更（仅 action-auth XML 分隔符），漂移归因此前已落地计划，按 project-context 失败模式规则显式登记「基线漂移已知」。successor: <pending-plan-id> trigger:compliance 基线 R2b/R2c/R12a 上漂 +2/+5/+1 的 per-site 裁决（对齐 2026-08-27-1540-1 先例）

## Draft Review Record

- dispatch review #review-2026-08-25-201158-mission-driver-2026-08-25-1956-2-action-auth-seed-comma-fix-1-9da5b51f to opencode-reviewer-2026-08-25-201158
- 2026-08-25：iteration 1，共识 accept #review-2026-08-25-201158-mission-driver-2026-08-25-1956-2-action-auth-seed-comma-fix-1-9da5b51f（活仓复核全对：35 处斜杠计数逐域精确（12/6/4/4/3/3/3）、ast:47 逗号先例、roadmap E1 发现项 1、owner doc P1.5a 漂移注记、前置计划 1 已 active 均属实；跨域角色（采购员/财务员/销售员/库管员）不出现在其余 6 域种子 → 逗号修复不会翻转既有负向断言；review 就地修正 1 处 Minor：账号 username 误写 `role-purchaser` → 实际 `role-pur`（ROLE_ACCOUNTS 逐字））

## Verification

- pass test 2026-09-05-123532-mission-driver exit=0 全 reactor `mvn test` BUILD SUCCESS（surefire 磁盘报告独立复核：672 报告文件 / 3992 tests / 0 failures / 0 errors / 1 skipped，2026-09-05 14:36 时间戳；日志见 docs/logs/2026/09-05.md）
- pass compliance 2026-09-05-123532-mission-driver exit=0 `bash docs/audits/nop-compliance-checker.sh`（2026-09-05 结束审计独立复跑 exit 0：16/19 规则 ==`## BASELINE` 块，R2b 242/R2c 1542/R12a 71 预存漂移按 Phase 2 收尾行登记）

## Closure

Status Note: Phase 1（7 文件 35 行分隔符修正）与 Phase 2（负向回归归因 + owner doc/日志对齐）全部落地并有活仓证据（commit a1b09d627、7 文件斜杠 grep=0、`roles-and-permissions.md` P1.5a 注记更正段、roadmap R1-E1-1 done、docs/logs/2026/09-05.md）；Phase 2 item 1 退出标准依赖保护区设计决策（fixStatus 塌缩语义涉 nop-entropy），已按 successor 交接勾选并以已就位断言作缺口回归信号——非本计划范围（Non-Goals：仅分隔符修正、角色集合成员不变）。

Closure Audit Evidence:

- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-08-25-1956-2-action-auth-seed-comma-fix-1-5d93e0bb to opencode-closure-auditor-2026-09-05-123532 models={exec:glm-5.3-flash,aud:glm-5.3-flash}
- accepted #audit-2026-09-05-123532-mission-driver-2026-08-25-1956-2-action-auth-seed-comma-fix-1-5d93e0bb：独立结束审计通过——活仓复核 7 文件斜杠 roles= 归零、commit a1b09d627 在案（7 XML + e1-2 (e) 正向断言）、owner doc/roadmap/日志三面对齐、全 reactor mvn test 磁盘报告 3992/0/0/1 + compliance checker 复跑 exit 0；ledger 结构修正（review conclusion 行语法）+ Verification pass 行 + 本 receipt 由审计补录。
