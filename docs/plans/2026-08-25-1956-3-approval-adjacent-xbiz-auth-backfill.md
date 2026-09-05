---
status: active
mission: permissions-enforcement
work-item: R1-E1-2
group: "2026-08-25-1956"
verify: [test, compliance]
---

# 2026-08-25-1956-3-approval-adjacent-xbiz-auth-backfill 审批邻接 xbiz mutation（submitForApproval/reject/withdrawApproval）auth 补齐

## Current Baseline

- **来源发现**：roadmap `docs/backlog/permissions-enforcement-roadmap.md` §Deep Audit Record「E1 — Deep Audit Findings R1」第 2 项（P1，未勾选）；范围计数以 R2 修正为准（Follow-up Backlog 登记）：**117 处 / 11 模块 / 39 文件**，R1 原记 105/8 作废。
- **活仓核验（2026-08-25，与 R2 修正一致）**：39 个 xbiz 文件 × 3 mutation（`submitForApproval`/`reject`/`withdrawApproval`）均无 `<auth>`——ast 6 / cs 1 / fin 2 / hr 1 / inv 1 / mfg 3 / mnt 2 / prj 3 / pur 8 / qa 5 / sal 7 文件；全仓 xbiz mutation 195 中无 auth 的 117 全部为该三动作，无其他缺 auth mutation。
- **失效机理（E1.1 根因同款）**：xbiz `<mutation>` 无 `<auth>` → `field.auth=null` → `isAllowAccess` 放行 = **fail-open 旁路**——restricted 账号当前可在无权限校验下执行审批轴状态变更（含 reject 驳回已提交单据 / withdrawApproval 撤销已审核单据），与「全量 19 域 action enforcement 闭环」mission 目标漂移（E1.x closure 范围仅 approve/reverseApprove 对）。
- **已验证修复模式（E1.1 落地先例）**：`<mutation>` 首子元素 `<auth permissions="..."/>`（schema `xbiz.xdef:35`；活例 `ErpHrSalary.xbiz:57,102`、`ErpMfgSubcontractOrder.xbiz:16,37`——Java @BizMutation 经 ReflectionBizModelBuilder 恒定附加非空 ActionAuthMeta，enforcement 必达）。
- **权限点现状**：三动作在全仓（生成 `_erp-*.action-auth.xml` + delta `erp-*.action-auth.xml`）**均无 FNPT 声明**（submitForApproval/withdrawApproval/reject 零命中；唯一近似 = ct 域 `ErpCtSignatureRequest:rejectSignature`，不同动作）；每实体既有可用 FNPT = `:query`/`:mutation`（生成）+ `:approve`/`:reverseApprove`（delta 审批域）。**例外（2026-08-25 活仓复核，draft review 修正）**：`ErpCsTicket`（cs）无 `:approve`/`:reverseApprove` FNPT 声明，其既有 `approve`/`reverseApprove` mutation `<auth>` 已引用这两个未声明权限（`ErpCsTicket.xbiz:29,80`）= 该实体审批轴既有 fail-closed admin-only 姿态；cs delta 亦无任何 `roles=` 种子 → 无业务角色持有任何 ErpCsTicket 权限（38/39 实体四权限点齐备，cs 为唯一缺口）。
- **动作语义锚点**：`submitForApproval` = DRAFT→PENDING_APPROVAL 提交（编制者轴）；`reject` = 审批人驳回（审批者轴）；`withdrawApproval` = 经 `AbstractWithdrawApproval` Processor 撤审（反审核轴，如 `ErpSalQuotationWithdrawApprovalProcessor`）。
- **前置（排位第 3 的原因）**：本批计划 2（`2026-08-25-1956-2`）修复 7 域斜杠种子前，斜杠域业务角色不持有任何有效 permission——补 auth 后正向回归会假阴性；种子修复先行，本计划在其后执行与验证。
- Task Route: Type = `implementation-only change`（auth/permissions 为 plan-first 区域：owner doc + tests + 独立 plan-audit 三证齐备）；Owner Docs = `docs/design/roles-and-permissions.md` §action-level 声明层、`docs/testing/permissions-enforcement-dry-run-impact.md`（E1.1 重归类先例）。

## Goals

- 117 处审批邻接 mutation 全部携带 `<auth permissions="..."/>`，映射到实体审批轴语义权限点（Phase 1 冻结逐实体映射表）——38/39 实体为**既有已声明** FNPT；唯一例外 `ErpCsTicket` 按 Phase 1 预裁决映射到其既有引用中的未声明 `:approve`/`:reverseApprove`（fail-closed admin-only，不产生 fail-open）——fail-open 旁路归零。
- 负向 Proof：restricted 账号在补齐动作上被真拒绝（按模块×动作族抽样）；正向 Proof：admin 与持权业务角色仍可通过。
- 逐文件×逐 mutation 映射表落盘（本文件或 `docs/testing/`），作为结束审计冻结输入。
- 全量 xbiz mutation census 复验：无 `<auth>` 的 mutation 计数归零（195/195）。

## Non-Goals

- 不新增 FNPT 权限点声明、不新增 role-resource 种子（复用既有权限点——P1.3 收敛粒度「角色×SUBM + 敏感动作 per-action」既定，117 个新 FNPT 与其矛盾）。
- 不改 Java BizModel/Processor 代码（仅保留层 xbiz `<mutation>` 首子元素补 `<auth>`）。
- 不动 approve/reverseApprove 对（E1.x 已闭环）与 Java @BizMutation 面。
- 不处理 SoD 守卫语义（R2-E-stack-1，独立工作项——本计划补 auth 后 restricted 在这些动作上被拒，但创建人自审守卫的另一缺陷不在范围）。

## Phase 1 — 逐实体映射冻结（Decision + 清点 Proof）

Skill: nop-backend-dev
Targets: 39 个 xbiz 文件对应实体的 `_erp-*.action-auth.xml` / delta `erp-*.action-auth.xml` 权限点真源

- Item Types: `Decision | Proof`

- [x] Decision: 权限映射裁决（推荐基线，逐实体核对后冻结）——`submitForApproval` → `<Entity>:mutation`（编制者轴，生成 FNPT 全实体必有）；`reject` → `<Entity>:approve`（审批者轴）；`withdrawApproval` → `<Entity>:reverseApprove`（撤审轴）。逐实体核验三点后定稿：(a) 三权限点在其 action-auth 真源中确实声明（`ErpCsTicket` 按下方预裁决例外处理）；(b) 状态机语义与轴归属无例外（有例外者单独记录并选最近语义权限点）；(c) 映射不产生「提交者即可自审」式越权放大（与 SoD 守卫正交，仅核对轴方向）。替代方案（新增 117 FNPT）按 Non-Goals 理由否决。**已知例外预裁决（draft review 注入，活仓复核）**：`ErpCsTicket`（cs）`reject`/`withdrawApproval` 映射到 `ErpCsTicket:approve`/`ErpCsTicket:reverseApprove`（未声明权限）——与该实体既有 `approve`/`reverseApprove` `<auth>` 引用一致（`ErpCsTicket.xbiz:29,80`），轴语义正确、无越权放大（未声明 = 无人可持有 = fail-closed admin-only，非 fail-open），不违反 Non-Goals（不新增声明、不新增种子）；残留风险 = cs 审批轴对业务角色暂不可用（与该实体 approve/reverseApprove 既有姿态一致，非本计划引入），successor 触发条件 = cs 域引入业务角色/FNPT 种子时一并声明 `ErpCsTicket:approve`/`:reverseApprove` 并补角色映射。
  **执行核验（2026-09-05）**：(a) 活仓核验 38/39 实体三权限点齐备（`:mutation`+`:query`=生成 `_erp-*.action-auth.xml`；`:approve`/`:reverseApprove`=delta `erp-*.action-auth.xml`，种子 pur/sal=审核人、ast=资产管理员,管理员、mfg=生产主管、prj=项目经理、qa=质量主管、fin=财务员、mnt=维护主管、inv=库管员、hr=薪酬审批人、`:reverseApprove`=管理员）；`ErpCsTicket` 仅 `:query`/`:mutation`，例外按预裁决处理；(b) 39 实体三动作状态机均为标准审批轴（withdrawApproval 经 `AbstractWithdrawApprovalProcessor` 或 inline source 撤回→UNSUBMITTED），无例外记录项；(c) 映射均为同实体审批轴对应权限点，无越权放大。
- [x] Proof: 39 文件 × 3 mutation 逐项映射表落盘（实体 / 文件 / 动作 / 权限点 / 语义核验结论），作为 Phase 2 冻结输入。
  **执行落盘（2026-09-05）**：`docs/testing/permissions-enforcement-approval-adjacent-xbiz-auth-mapping.md`（39 行 + Decision 三点核验结论 + 种子现状 + 声明核验机读校验 PASS）。

## Phase 2 — 按模块集群补齐 117 处（Fix）

Skill: nop-backend-dev
Targets: 11 模块 39 个 `module-*/erp-*-service/src/main/resources/_vfs/erp/*/model/*/*.xbiz` 文件（ast/cs/fin/hr/inv/mfg/mnt/prj/pur/qa/sal）

- Item Types: `Fix | Proof`

- [x] Fix: 按模块集群（每集群 = pur 8 → sal 7 → ast 6 → mfg 3 / prj 3 → qa 5 → fin 2 / mnt 2 → cs 1 / hr 1 / inv 1 顺序）在 `<mutation name="submitForApproval|reject|withdrawApproval">` 首子元素补 `<auth permissions="<Entity>:<mapped>"/>`（E1.1 位置先例，xbiz.xdef:35）。
  **执行落地（2026-09-05）**：39 文件 / 117 处全部补齐（单会话逐脚本插入，xmllint 全部 well-formed；git diff = 39 files changed / 117 insertions / 0 deletions，零其他改动）。
- [x] Proof: 每集群完成后本地化校验——全仓 grep 无 auth 的该三动作计数递减归零 + 该模块 `mvn test` 编译/快照绿（集群间独立交付，单集群失败不阻塞已交付集群）。
  **执行校验（2026-09-05，单会话全集群一次交付后整体验证，覆盖强于逐集群门）**：117 处插入后全仓 grep 该三动作无 auth 计数 = 0（195/195 xbiz mutation 全携带 `<auth>`）；全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` BUILD SUCCESS（failures=0 / errors=0，含各域快照测试）。

## Phase 3 — 负向/正向 Proof + census 归零 + owner doc

Skill: nop-testing
Targets: `tests/e2e/negative/`（e1-1 族扩展或新 spec）、`docs/design/roles-and-permissions.md`、`docs/testing/permissions-enforcement-dry-run-impact.md`

- Item Types: `Proof | Fix`

- [x] Proof: 负向——restricted 账号对补齐动作真拒绝抽样 Proof（覆盖三动作族 × 代表模块，范式 = e1-1 族 `expectActionDenied`；至少含 pur/sal 审批集 + 一个扩展域；cs 若入负向抽样：业务角色被拒为预期正确结果——未声明/无种子权限 deny-by-default，非缺陷）；正向——admin 与持权业务角色（如 采购员 对 pur 域）三动作均可通过抽样 Proof（正向抽样限持权域，cs 无持权业务角色不入正向）。
  **执行 Proof（2026-09-05，flux 引擎 %test enforcement ON 运行时实测）**：新 spec `tests/e2e/negative/e1-3-approval-adjacent-auth.smoke.spec.ts` **11/11 绿**——负向 restricted：pur/sal/prj × 三动作族 9 探针全拒（`nop.err.auth.no-permission`/「没有访问权限」）+ cs `ErpCsTicket` restricted/审核人双主体 fail-closed 拒（预期正确）+ submitForApproval 零 `:mutation` 种子边界断言（审核人拒 = fail-closed admin-only，Non-Goal 不新增种子既有姿态）；正向：admin pur/sal/prj × 三动作族全通过（skip-check）+ 持权业务角色 审核人 reject（pur/sal）、项目经理 reject（prj）、业务「管理员」withdrawApproval（pur/sal/prj）全通过（真实 permissionToRoles 命中，非 skip-check）。注：`:mutation` 全 39 实体零角色种子（映射表 §种子现状），故 submitForApproval 正向主体 = admin（计划文中「采购员」例基于「持权」限定，实际持权角色核查后按表执行）；既有 E1.1/E1.2 spec 15/15 零回归。
- [x] Proof: census 归零复验——全仓 xbiz mutation 195 处全部携带 `<auth>`（或 Java 面等价 ActionAuthMeta），零无 auth mutation；映射表与实仓 diff 一致。
  **执行复验（2026-09-05）**：机读 census = 保留层 xbiz mutation 总数 195、无 auth 首子元素 = 0、三动作携带 auth = 117/117、逐条 permissions 与冻结映射表 diff 一致 = 117/117、mismatch = 0（VERDICT PASS）。
- [x] Fix: `roles-and-permissions.md` §action-level 声明层补审批邻接三动作 enforcement 注记（映射基线 + 117/11 计数 + 修复日期）；`docs/logs/` 日志条目。
  **执行落地（2026-09-05）**：`docs/design/roles-and-permissions.md` §action-level 增「审批邻接三动作 auth 补齐」注记段（映射基线 + cs 例外 + 种子边界 + census 归零 + 运行时 Proof 指针）；`docs/logs/2026/09-05.md` 增本 run 条目；compliance checker exit 0，漂移 = 预存 R2b+2 / R2c+5 / R12a+1（本计划 XML-only 零新增漂移）。

## Draft Review Record

- dispatch review #review-2026-08-25-201158-mission-driver-2026-08-25-1956-3-approval-adjacent-xbiz-auth-backfill-1-b305c8e3 to opencode-reviewer-2026-08-25-201158
- 2026-08-25：iteration 1，共识 accept #review-2026-08-25-201158-mission-driver-2026-08-25-1956-3-approval-adjacent-xbiz-auth-backfill-1-b305c8e3（活仓复核：39 文件×3=117 无 auth 计数逐模块精确、census 195/117 全对、E1.1 先例四处行号属实、三动作 FNPT 零声明 + ct:104 近似、前置计划 2 已 active、R2 修正 117/11 已登记均属实；review 就地修正 1 处 Major 事实基线——`ErpCsTicket` 无 `:approve`/`:reverseApprove` FNPT 声明且既有 approve/reverseApprove auth 已引用未声明权限（cs delta 零 roles 种子），原「每实体四权限点齐备」表述失真 → 基线/Goals/Phase 1 裁决/Phase 3 Proof 四处同步注入 cs 例外预裁决：映射到未声明轴语义权限点 = fail-closed admin-only 非 fail-open，successor 触发条件已登记）

## Verification

- pass test 2026-09-05-1625-closure-audit exit=0（独立结账审计现场复跑：全 reactor `mvn test` BUILD SUCCESS，13:34 min，failures=0 / errors=0）
- pass compliance 2026-09-05-1625-closure-audit exit=0（独立结账审计现场复跑：`bash docs/audits/nop-compliance-checker.sh` exit 0，漂移 = 预存 R2b+2 / R2c+5 / R12a+1，本计划 XML-only 零新增）
- pass test 2026-09-05-1705-verify-r3 exit=0（mission verify run 现场复跑：全 reactor `mvn clean install -DskipTests` BUILD SUCCESS 01:41 min + `mvn test` BUILD SUCCESS 13:54 min，failures=0 / errors=0 / 1 skipped）
- pass compliance 2026-09-05-1705-verify-r3 exit=0（mission verify run 现场复跑：`bash docs/audits/nop-compliance-checker.sh` exit 0，预存漂移 R2b 242 / R2c 1542 / R12a 71 / R12b 66 / R12c 42，本计划 XML-only 零新增）

## Closure

- dispatch audit #audit-2026-09-05-162552-mission-driver-2026-08-25-1956-3-approval-adjacent-xbiz-auth-backfill-1-eeeb284b to 2026-09-05-123532-mission-driver models={exec:opencode/glm-5.3-flash,aud:opencode/glm-5.3-flash}
- accepted #audit-2026-09-05-162552-mission-driver-2026-08-25-1956-3-approval-adjacent-xbiz-auth-backfill-1-eeeb284b：独立结账审计通过——活仓机读复核 census 195/195 保留层 xbiz mutation 全携带 `<auth>` 首子元素（审批邻接三动作 117/117，零缺失）、git diff 39 xbiz = 117 insertions / 0 deletions 且 xmllint 全部 well-formed、Phase 1 映射表 `docs/testing/permissions-enforcement-approval-adjacent-xbiz-auth-mapping.md` 在盘且与实仓一致、owner doc `roles-and-permissions.md` §action-level 注记 + `docs/logs/2026/09-05.md` + roadmap R1-E1-2 勾选三处落盘核验一致；审计现场复跑全 reactor `mvn test` BUILD SUCCESS exit 0（13:34 min）+ `nop-compliance-checker.sh` exit 0（预存漂移 R2b+2 / R2c+5 / R12a+1，本计划零新增）；运行时 Proof spec `tests/e2e/negative/e1-3-approval-adjacent-auth.smoke.spec.ts` 11 tests 实文核验非空壳（负向 restricted 三动作族 × pur/sal/prj + cs 双主体 fail-closed + 正向 admin/审核人/项目经理/业务管理员）；`ErpCsTicket` 例外为计划内预裁决 fail-closed admin-only（非 fail-open、非缺陷），successor 触发条件已登记
