# 2026-08-09-0751-1 per-action-fnpt-b2b-edi-lifecycle

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.4c
> Related: mission `permissions-enforcement`；P1.3（粒度裁决，已 done，提供收敛粒度 + B2B 角色基线）；P1.1（敏感字段清单，已 done，EDI 属保密五面之一）；P1.6（xwf 语义裁决，done——b2b 无 xwf 实体，无交叉）；P1.4a/b（pur/sal/mfg/assets 审批集范式，已 done，本计划复用其 per-action FNPT 声明模式）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.4c

## Current Baseline

P1.3 已裁决映射收敛粒度 = 角色×SUBM + **敏感动作 per-action FNPT** + 兜底策略（双命名空间分离）。`roles-and-permissions.md` §action-level 灰度推进路线（L223）明确点名「**全 EDI 生命周期** 等」的 per-action FNPT 声明为 P1.4c 待补齐项（触发条件 = b2b 域 `enable-action-auth=true` 灰度批准前）。

**b2b 现状**（实测生成文件 + delta + BizModel）：

- 生成文件 `_erp-b2b.action-auth.xml`（`module-b2b/erp-b2b-web/.../_vfs/erp/b2b/auth/_erp-b2b.action-auth.xml`）：13 实体每实体仅 `:query`/`:mutation` 两 FNPT 点（坍缩桶），无 per-action 点、无 `roles` 种子。
- delta `erp-b2b.action-auth.xml`（同目录非生成文件）：已声明 **3 个 per-action FNPT** + `roles` 种子——`ErpB2bEdiDoc:markSent`→B2B 对账员（L21-24）、`ErpB2bEdiDoc:cancel`→B2B 管理员（L25-28）、`ErpB2bAsn:handleInboundWebhook`→B2B 管理员（L45-48）。其余实体（EdiFormat/AsnLine/PartnerProfile/MftConfig/MftCertificate/MftLog/CodeMapping/EdiLog）为 bare SUBM 自闭合 resource，**无 `<children>` FNPT 块**。

**b2b BizModel 非 CRUD 敏感 mutation 实测**（3 实体共 15 个 `@BizMutation` 生命周期动作，`module-b2b/erp-b2b-service`）：

- **ErpB2bEdiDocBizModel**（EDI 信封状态机，8 mutations，`.../service/entity/ErpB2bEdiDocBizModel.java`）：`createOutbound`(L56)、`markSent`✅(L64)、`markAcknowledged`(L80)、`markError`(L96)、`retry`(L111)、`cancel`✅(L129)、`createInbound`(L146)、`archive`(L156)。状态机与角色触发见 `docs/design/b2b/edi-formats.md` §7（L415-479）：SENT→ACKNOWLEDGED=对方回调/HMAC、ERROR→TO_SEND=**管理员重试**、ERROR→CANCELLED=**管理员放弃**、归档管理。
- **ErpB2bAsnBizModel**（ASN 入站生命周期，4 mutations，`.../service/entity/ErpB2bAsnBizModel.java`）：`handleInboundWebhook`✅(L65，外部 webhook 入站最高危)、`matchPurchaseOrder`(L76，ASN→PO 匹配 RECEIVED→MATCHED)、`createReceiveFromAsn`(L82，config-gated 跨域写入库草稿)、`retryMatch`(L88，手动重试匹配)。
- **ErpB2bPartnerProfileBizModel**（伙伴生命周期，3 mutations，`.../service/entity/ErpB2bPartnerProfileBizModel.java`）：`activate`(L21 CERTIFIED→PRODUCTION)、`suspend`(L30)、`deactivate`(L39)——管 `webhookSecret` 载体实体，activate/deactivate 安全敏感。
- 其余 b2b BizModel（EdiFormat/MftConfig/MftCertificate 仅 `@BizQuery findExpiringCertificates`/MftLog/CodeMapping/AsnLine/CertificationChecklist/TestExchange/PartnerCredential/EdiLog）为纯 CRUD 或仅查询，**无 per-action FNPT 需求**（模块级 `@BizMutation` grep 已确认零命中）。

**Processor 类**（`.../service/processor/`，每 mutation 一个 Processor，R6.7 拆分）：EdiDoc CreateOutbound/CreateInbound、Asn HandleInboundWebhook/MatchPurchaseOrder/CreateReceiveFromAsn/RetryMatch——均为内部 `@Inject` bean，**非独立 GraphQL/FNPT 入口**；FNPT 入口 = BizModel 方法名，故声明仅按上述 15 个方法名。

**enforcement 状态**：`nop.auth.enable-action-auth=false`（默认 OFF），本计划仅"已就绪可授权"，不拦截任何调用；`roles` 属性在 enforcement OFF 时不生效，翻转后并入 `permissionToRoles` 校验。

**B2B 角色基线**（`roles-and-permissions.md` §第二批扩展域 A，L159-160）：
- **B2B 对账员**：EDI 事务处理、ASN 接收、代码映射维护、B2B 对账确认。
- **B2B 管理员**：EDI 出站自动化配置、错误升级处理、归档管理。

**缺口**：b2b EDI 全生命周期 15 个敏感 mutation 中仅 3 个（markSent/cancel/handleInboundWebhook）有独立 per-action FNPT；其余 12 个坍缩进泛化 `mutation` 桶，enforcement 翻转后丧失独立管控（违背 P1.3 收敛粒度 + §灰度推进路线点名「全 EDI 生命周期」）。

## Goals

- **补齐 b2b EDI 全生命周期 per-action FNPT 声明**：为 EDI 信封/ASN/Partner 生命周期敏感动作声明独立 per-action FNPT 点（脱离 `mutation` 桶）+ `<resource ... roles="...">` 静态 role-resource 种子，保持 enforcement OFF。覆盖 EdiDoc（markAcknowledged/markError/retry/archive）+ Asn（matchPurchaseOrder/createReceiveFromAsn/retryMatch）+ PartnerProfile（activate/suspend/deactivate）。
- **roles 种子对齐 B2B 角色基线 + EDI 状态机**：对账/匹配类→B2B 对账员；错误升级/重试/归档/伙伴配置类→B2B 管理员（按 `edi-formats.md` §7 状态机 operator vs admin 触发 + `roles-and-permissions.md` L159-160 角色职责）。
- **每集群独立交付 + 独立回归**：EDI 信封+ASN（事务流）与 PartnerProfile（凭证/安全生命周期）各自声明 + 各自核验（permissionToRoles 一致性 + xmllint well-formed + compliance 零漂移）。

## Non-Goals

- **不翻转 enforcement 开关**（归 P2.4/E1.x）。
- **不产 auth 表 CSV 种子**（角色/用户/账号归 P1.5b；本计划仅 `roles` 静态属性种子，等价 `nop_auth_role_resource` 静态种子）。
- **不做 SUBM 菜单组层 roles 映射**（SUBM 粗粒度归 P1.5a；本计划聚焦敏感动作 per-action FNPT）。
- **不改生成文件** `_erp-b2b.action-auth.xml`（真相源）；声明只在 delta 非生成文件 `erp-b2b.action-auth.xml`。
- **不动 EdiDoc `markSent`/`cancel`、Asn `handleInboundWebhook` 既有声明**（已落地，本计划仅增量补齐其余生命周期动作）。
- **不改 EDI/ASN 业务逻辑**（BizModel/Processor 已落地；本计划仅声明权限点）。
- **不改 Mft/CodeMapping/EdiLog/AsnLine 等纯 CRUD 实体**（无 per-action FNPT 需求）。
- **不触及保密字段级可见性**（EDI 属保密五面之一，字段级归 E3.1/E4.1；本计划仅 action 级）。

## Task Route

- Type: `implementation-only change`（delta action-auth.xml per-action FNPT 声明补齐 + roles 种子，enforcement 保持 OFF，不改运行时行为）
- Owner Docs: `docs/design/roles-and-permissions.md` §action-level 声明层 + §第二批扩展域 A（B2B 角色基线）、`docs/design/b2b/edi-formats.md` §7（EDI 信封状态机）
- Skill Selection Basis: `nop-backend-dev` —— delta action-auth.xml per-action FNPT 声明属后端权限声明层工作（与 roadmap 表格 P1.4c Skill 列一致，与 P1.4a/b 同范式）；本计划聚焦声明层，不写 BizModel/Processor 代码（EDI 业务逻辑已由 2200-1 plan 落地）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（仅 delta XML 声明，enforcement 保持 OFF，不改运行时）。

## Execution Plan

### Phase 1 - EDI 信封 + ASN 生命周期 per-action FNPT 声明

Status: completed
Targets: `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/auth/erp-b2b.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Decision` / `Proof`
- Prereqs: P1.3（done）；P1.1（done，EDI 敏感面输入）

- [x] **Decision**：裁决 EDI 创建/发起动作（`createOutbound`/`createInbound`）的 FNPT 处理。考虑的替代方案：(a) 创建动作留 `:mutation` 桶不声明独立点——`createOutbound`/`createInbound` 是操作员发起的创建/发起动作（产生 TO_SEND/RECEIVED 信封），非 markError/retry/cancel 类最高危敏感动作（错误升级/重试/放弃），按 P1.3 收敛粒度不需脱离 `mutation` 桶；创建权限由 SUBM 菜单可见性（B2B 对账员/管理员可见 b2b-edi 组）覆盖，与 P1.4a submit Decision 同型。(b) 创建动作声明独立 `:createOutbound`/`:createInbound` FNPT（拒绝：过度拆分，创建属常规录入，非 per-action 层职责）。选定 (a)，创建动作留 `:mutation` 桶，本计划不声明创建独立点。残留风险：若后续要求"仅特定角色可创建 EDI 单"，须升格（successor）。
  - Skill: none
- [x] **Add**：在 delta `erp-b2b.action-auth.xml` 为 EDI 信封 + ASN 生命周期敏感动作声明独立 per-action FNPT 点，挂在对应实体 `<resource id="Erp*-main">` 的 `<children>` 下（参照既有 `ErpB2bEdiDoc:markSent`/`:cancel` 与 finance `ErpFinVoucher:post`/`:reverse` 范式）。`<permissions>` = `{Entity}:{action}`；`roles` 种子按 `edi-formats.md` §7 状态机触发主体 + `roles-and-permissions.md` L159-160 角色职责：
  - **ErpB2bEdiDoc**：`:markAcknowledged`→B2B 对账员（对账确认，按 `roles-and-permissions.md` L159 角色职责——该状态机迁移触发主体为对方回调/系统，非角色触发，enforcement 时 callback/系统路径的 admin-bypass 处理归 E1.x）、`:markError`→B2B 管理员（错误升级处理，按 `roles-and-permissions.md` L160 角色职责——`edi-formats.md` §7 的 ERROR-*exit* 迁移（retry/abandon）为管理员操作，markError 进入 ERROR 由系统异常触发）、`:retry`→B2B 管理员（管理员重试，状态机明定）、`:archive`→B2B 管理员（归档管理，`roles-and-permissions.md` L160 角色职责）。
  - **ErpB2bAsn**：`:matchPurchaseOrder`→B2B 对账员（ASN 接收/对账匹配）、`:createReceiveFromAsn`→B2B 对账员（跨域入库对账）、`:retryMatch`→B2B 对账员（匹配重试）。
  - 具体权限点 ID 执行时按生成文件 `_erp-b2b.action-auth.xml` 核验（不在本计划冻结逐点清单，遵循 P1.3「不冻结具体动作清单」残留风险）。
  - Skill: `nop-backend-dev`
- [x] **Proof**：xmllint well-formed 校验 `erp-b2b.action-auth.xml` 通过 + `permissionToRoles` 静态映射一致性自检（对账/匹配类→B2B 对账员、错误升级/重试/归档类→B2B 管理员，角色名与 `roles-and-permissions.md` §第二批扩展域 A 一致）。
  - Skill: none

Exit Criteria:

- [x] 创建动作 FNPT 处理 Decision 落地（留 `:mutation` 桶）+ EdiDoc 4 动作（markAcknowledged/markError/retry/archive）+ Asn 3 动作（matchPurchaseOrder/createReceiveFromAsn/retryMatch）per-action FNPT 声明落地，xmllint 通过，roles 种子与 B2B 角色基线 + EDI 状态机一致。

### Phase 2 - PartnerProfile 凭证/安全生命周期 per-action FNPT 声明

Status: completed
Targets: `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/auth/erp-b2b.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Proof`
- Prereqs: Phase 1（范式确立后复用）；P1.3（done）

- [x] **Add**：在 delta `erp-b2b.action-auth.xml` 为 `ErpB2bPartnerProfile` 生命周期敏感动作声明独立 per-action FNPT 点 `ErpB2bPartnerProfile:activate`/`:suspend`/`:deactivate`，挂在 `ErpB2bPartnerProfile-main` 的 `<children>` 下（参照 Phase 1 范式）。`roles` 种子→**B2B 管理员**（伙伴 activate/suspend/deactivate 管 `webhookSecret` 载体实体 + 生产环境启停，属「EDI 出站自动化配置/安全」职责，对账员不可达）。注：当前 `ErpB2bPartnerProfile-main`（delta L64-67）为自闭合 `<resource .../>`（无 `<children>` 块，异于 EdiDoc-main/Asn-main 已有 children），实施时须转为开闭合标签再挂 children。
  - Skill: `nop-backend-dev`
- [x] **Proof**：xmllint well-formed 校验通过 + `permissionToRoles` 一致性自检（3 动作均→B2B 管理员）。
  - Skill: none

Exit Criteria:

- [x] PartnerProfile activate/suspend/deactivate per-action FNPT 声明落地，xmllint 通过，roles 种子与 B2B 管理员职责一致。

### Phase 3 - owner doc 实现注记 + 日志

Status: completed
Targets: `docs/design/roles-and-permissions.md` §action-level 声明层 + §既有种子证据
Skill: none

- Item Types: `Add` / `Fix`
- Prereqs: Phase 1 + Phase 2

- [x] **Add | Fix**：§action-level 声明层「已落地」表（L202-217）增列 b2b EDI 全生命周期 per-action FNPT 点 + roles 种子（delta 文件行号证据）——ErpB2bEdiDoc（markAcknowledged/markError/retry/archive）+ ErpB2bAsn（matchPurchaseOrder/createReceiveFromAsn/retryMatch）+ ErpB2bPartnerProfile（activate/suspend/deactivate），与既有 finance/b2b/mfg/inv/hr/pur/sal/assets 行对齐；§灰度推进路线（L223）将「全 EDI 生命周期 等」由待补齐改为已落地（首批灰度域增列 b2b 完整）；§既有种子证据块（L240）增 b2b 完整行号，并**顺带修正既有 b2b 行的行号漂移**（L240 现引「L22-27/L46-47」，实测真值为 markSent L21-24、cancel L25-28、handleInboundWebhook L45-48——与新增长行一并校正）。
  - Skill: none

Exit Criteria:

- [x] owner doc 实现注记落地，与 delta 文件真相源一致；§灰度推进路线 b2b 全生命周期标记已补齐。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（0 blocker / 0 major / 3 minor）（ses_01a3ea5b3ffe4AAoEL3vh8upVb）。M1 markAcknowledged/archive 角色归属引证混用状态机触发主体与角色职责（实际仅 markError/retry 为状态机明定管理员触发）；M2 §既有种子证据 L240 既有 b2b 行号漂移未点名修正；M3 PartnerProfile-main 自闭合无 children 块未标注。基线 15 @BizMutation 行号 + 3 既有 FNPT + webhookSecret 载体均实测确认准确；范式与 P1.4a/b 同构；Deps（P1.3 done）确认；anti-slack 通过。
- 合并修订（iteration 1 → v2）：M1 拆分引证（markError/retry 引状态机，markAcknowledged/archive 引角色职责 + 注 callback/系统路径 admin-bypass 归 E1.x）；M2 Phase 3 显式点名修正既有 b2b 行号漂移；M3 Phase 2 标注 PartnerProfile-main 自闭合→需转开闭合标签。
- Independent draft review iteration 2: accept（0 blocker / 0 major / 3 minor，全部信息性）（ses_01a366735ffeUUkWUhITpL87N5）。M1/M2/M3 全部经实测确认已解决（PartnerProfile-main 自闭合 L64-67 实测、§既有种子证据 L240 漂移值实测匹配、edi-formats.md §7 ERROR-exit 管理员触发实测）。residual：m1 Phase 2 重复 Skill 行（cosmetic）；m2 Phase 3 类型应为 `Add | Fix`（含行号漂移修正）；m3 markError 引证应引角色基线 L160 而非状态机（ERROR-entry 由系统触发）。
- 合并修订（iteration 2 → v3）：m1 删重复 Skill 行；m2 Phase 3 Item Types 改 `Add | Fix` + 项目标 `Add | Fix`；m3 markError 引证改为角色基线 L160（错误升级处理）+ 注 ERROR-entry 由系统异常触发。
- Plan Status → active（两轮独立审查共识，0 blocker / 0 major）。

## Closure Gates

> 本计划改 delta action-auth.xml（声明层，enforcement 保持 OFF，不改运行时行为）。Closure Gates 运行 delta XML well-formed + compliance checker 对照 `known-good-baselines.md` 零漂移（横切关注点 7）。完整 build/test 在此运行一次。

- [x] 范围内行为完成（EdiDoc 4 + Asn 3 + PartnerProfile 3 = 10 个 per-action FNPT 声明 + roles 种子 + 创建动作 Decision + owner doc 注记）
- [x] 相关文档对齐（`roles-and-permissions.md` §action-level 声明层 + §灰度推进路线 + §既有种子证据）
- [x] 已运行验证：`xmllint --noout` delta 文件 + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移 + `mvn clean install -DskipTests`
- [x] 无范围内项目降级为 deferred/follow-up（创建动作留 `:mutation` 桶为 Decision 选定项，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ErpB2bEdiDoc createOutbound/createInbound 升格为独立 FNPT 点

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 Decision 已裁决创建动作留 `:mutation` 桶（创建属常规录入，非最高危敏感动作；P1.3 收敛粒度不需独立点；管控由 SUBM 可见性覆盖）。与 P1.4a submit Decision 同型。
- Successor Required: yes（触发条件 = 出现"仅特定角色可创建 EDI 单"的精细化需求，createOutbound/createInbound 须升格为独立 FNPT）

### SUBM 菜单组层 roles 映射

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SUBM 粗粒度角色映射归 P1.5a 静态种子补全；本计划聚焦敏感动作 per-action FNPT。
- Successor Required: yes（触发条件 = P1.5a 进入）

## Closure

Status Note: 全三 Phase 执行完成（executor session）。b2b EDI 全生命周期 10 个 per-action FNPT 声明 + roles 种子落地（EdiDoc 4 + Asn 3 + PartnerProfile 3），delta `erp-b2b.action-auth.xml` well-formed + permissionToRoles 一致 + compliance 零漂移 + 全 reactor `mvn clean install -DskipTests` BUILD SUCCESS（156 模块）。enforcement 保持 OFF，不改运行时行为。独立结束审计已由独立子代理（新会话，mission-driver closure-audit 2026-08-09）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver closure-audit，新会话，ses 不重用 executor 上下文），2026-08-09
- Audit Scope & Method: 严格按 `00-plan-authoring-and-execution-guide.md` 结束检查清单 + SCRIPT_CHECK_RESULT=FAIL（1 unchecked audit gate）→ 作为独立审计员对全部 6 项语义维度复核
- Evidence:
  - delta `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/auth/erp-b2b.action-auth.xml`：13 FNPT 实测行号——EdiDoc: markSent(L21-24)/cancel(L25-28)/markAcknowledged(L29-32,B2B 对账员)/markError(L33-36,B2B 管理员)/retry(L37-40,B2B 管理员)/archive(L41-44,B2B 管理员)；Asn: handleInboundWebhook(L61-64,B2B 管理员)/matchPurchaseOrder(L65-68,B2B 对账员)/createReceiveFromAsn(L69-72,B2B 对账员)/retryMatch(L73-76,B2B 对账员)；PartnerProfile: activate(L97-100)/suspend(L101-104)/deactivate(L105-108) 均 B2B 管理员（原自闭合 resource 已转开闭合挂 children）
  - `xmllint --noout` delta 文件 well-formed OK
  - `bash docs/audits/nop-compliance-checker.sh` 零漂移（本计划改 0 Java，R1-R12 扫 Java 生产代码）
  - `mvn clean install -DskipTests` 全 reactor **BUILD SUCCESS**（156 模块，1:36）；`TestErpB2bAsnInbound`（b2b-service）2 errors 经 stash 验证为 pre-existing（H2 `NOP_SYS_SEQUENCE` 序列初始化环境问题，与本计划 erp-b2b-web delta XML 声明层无关）
  - owner doc `docs/design/roles-and-permissions.md`：§action-level「已落地」表 b2b 2 行→3 行、§灰度推进路线 b2b 全生命周期移出「其余」、§既有种子证据 b2b 行扩全 13 点 + 修正行号漂移
  - roadmap `docs/backlog/permissions-enforcement-roadmap.md` P1.4c `todo`→`done`
  - 日志 `docs/logs/2026/08-09.md` P1.4c 条目追加
- 独立审计语义复核（6 项全通过）：
  1. **Phase status / items 一致性**：3 Phase 均 `completed`，Phase body 内无残留未勾选项（唯一原先未勾选项 = 本审计门控，已由独立审计员 tick 为已勾选）
  2. **Exit Criteria vs live repo**：实测 delta `erp-b2b.action-auth.xml` 全 13 FNPT 点行号与 executor 申报证据逐字一致（markAcknowledged L29-32/markError L33-36/retry L37-40/archive L41-44/matchPurchaseOrder L65-68/createReceiveFromAsn L69-72/retryMatch L73-76/PartnerProfile activate L97-100/suspend L101-104/deactivate L105-108）；roles 与 B2B 角色基线（`roles-and-permissions.md` L159-160）+ EDI 状态机（`edi-formats.md` §7）一致
  3. **Anti-Hollow**：10 个新 FNPT 点全部对应实测 `@BizMutation` 方法（ErpB2bEdiDocBizModel markAcknowledged L81/markError L97/retry L112/archive L157；ErpB2bAsnBizModel matchPurchaseOrder L77/createReceiveFromAsn L83/retryMatch L89；ErpB2bPartnerProfileBizModel activate L22/suspend L31/deactivate L40），无空壳；PartnerProfile-main 已由自闭合转开闭合挂 children（L92-110），声明可达
  4. **Five-point consistency**：Plan Status completed / 3 Phase completed / Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure evidence 实证存在——一致
  5. **Deferred honesty**：两项 Deferred（createOutbound/createInbound 升格、SUBM 菜单组层 roles）均带 successor 触发条件，无活缺陷或契约漂移隐藏为 follow-up
  6. **Docs sync**：`roles-and-permissions.md` §action-level 表 b2b 2→3 行（L207-209）+ §灰度推进路线 b2b 全生命周期移出「其余」（L224）+ §既有种子证据 b2b 行扩全 13 点 + 行号漂移修正（L241）；`logs/2026/08-09.md` P1.4c 条目；`permissions-enforcement-roadmap.md` P1.4c `todo`→`done`——全部对齐
- Audit Verdict: **approved**（0 blocker / 0 major / 0 minor）；enforcement 保持 OFF 故无运行时回归风险；本审计由独立子代理在新会话执行，未重用 executor 上下文，executor 未自我审计

Follow-up:

- `createOutbound`/`createInbound` 升格独立 FNPT（触发=「仅特定角色可创建 EDI 单」精细化需求，Deferred But Adjudicated 已记录）
- SUBM 菜单组层 roles 映射（归 P1.5a）
- EDI 字段级可见性（归 E3.1/E4.1）
