# 2026-08-09-2210-2 负向隔离测试框架（未授权动作拒绝 + 越权数据过滤断言原语 + 冒烟 demo）

> Plan Status: active
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P2.3
> Related: P2.1（`2026-08-09-0751-3`，done——三开关 profile 预置，enforcement 仍 OFF）；P1.5b（`2026-08-09-2107-1`，done——21 业务角色 + 平台角色种子就绪，为 E1.x 负向账号提供角色记录基础）；P2.2a（`2026-08-09-2210-1`，并行——admin 兜底 E2E 基线，与本计划无 Deps 依赖但同为 E1.1 前置）；P2.2b（角色化渐进，直接后继——逐域补角色账号 + fixture 角色参数化，本计划原语的角色登录 indirection 由 P2.2b 账号填充）；E1.1（高危分域翻转 + 负向测试，直接后继——消费本计划原语做真拒绝负向断言）；roadmap §横切关注点 4（测试语义保持不变：仅改鉴权层 fixture，不改既有业务断言）+ §执行机制 4（auth/permissions plan-first 区域）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P2.3

## Current Baseline

P2.3 是 enforcement 负向隔离验证的**原语与脚手架层**：交付可复用的「未授权动作被拒绝」+「越权数据被过滤」断言原语 + 脚手架 + 1 例可运行冒烟 demo，供 E1.x（action 级负向）/E2.x（data 级负向）消费。退出标准 = 原语交付 + 冒烟 demo 可运行（骨架交付不依赖负向账号；验收实测随 E 段）。

**既有负向断言现状（实测，业务逻辑层）**：现有 E2E 已有大量**业务逻辑负向断言**（非 enforcement）——`business-actions/_helper.ts#callMutation` 返回 `{ data, errors, json }`，spec 经 `expect(rej.errors).toBeTruthy()` + `expect(JSON.stringify(rej.errors)).toContain('<中文 token>')` 断言 NopException 拒绝（范例：`hr-payroll.action.spec.ts:214-216` markPaid UNSUBMITTED 守卫 → `ERR_SALARY_ILLEGAL_STATUS_TRANSITION` token「不允许执行该操作」；`drp-release-line.action.spec.ts` 非法迁移守卫）。`GraphQLClient.callMutation/callQuery`（`pages/GraphQLClient.ts:56-131`）统一把 NopException 经 GraphQL `errors` 数组透出（`data` 置 null）。**即「动作被拒绝」的断言机制已有业务层先例，但无 enforcement 层（权限拒绝）专用原语。**

**enforcement 拒绝形状（未知，须探查）**：当 action-auth 拦截（用户缺 FNPT 权限点）或 data-auth 过滤（行级规则排除）时，GraphQL 响应形状**尚未在运行时表征**——`DefaultActionAuthChecker`（`nop-entropy/.../DefaultActionAuthChecker.java`）拒绝时抛何种 NopException/ErrorCode、GraphQL `errors` 含何 token、HTTP status 是否 403，均须 Phase 1 经平台源 + 文档表征（运行时确认因 action-auth 翻启归 P2.4 而延后）。这是本计划原语设计的**核心输入**。

**data-auth 行过滤形状（部分已知）**：`ErpRoleDataAuthChecker`（`module-common-service`，bean `nopDataAuthChecker`）行级规则经 `getFilter()` 注入 SQL where，`__findPage` 静默返回过滤后行集（**无 errors**，total/items 减少）。故「越权数据被过滤」断言 = 断言特定行**不可见**（absent）/ 可见行集收敛，**非** errors 断言——与动作拒绝形状不同，须独立原语。

**账号现状（骨架不依赖）**：E2E 现仅 nop 账号（P1.5b 后绑平台 admin，skip-check 兜底）。负向测试主体（非 admin 受限账号）归 P2.2b。**P2.3 骨架交付不依赖负向账号**（roadmap 明示）；冒烟 demo 须在无负向账号前提下可运行（见 Goals 裁决）。

**既有测试基建**：`tests/e2e/fixtures.ts`（page fixture + console error 收集）、`business-actions/_helper.ts`（createViaSave/callMutation/verifyState/findItems/deleteByFilter 原语族）、`orchestration/_helper.ts`（链式编排 + 清理原语）。**编辑权限实测**（`opencode.json`）：deny 规则按扩展名限定（`**/_*.java`/`.xmeta`/`.xbiz`/`.orm.xml`/`.beans.xml`/`.i18n.yaml`/`.action-auth.xml`），**无 `**/_*.ts` deny 规则**；且 L17 显式 `"tests/e2e/**/_helper.ts": "allow"`。**即所有 `.ts` 测试文件（含 `_helper.ts`）均可自由编辑。** 负向原语**复用**既有 GraphQLClient/原语族，新增独立 helper 模块——独立模块是**关注点分离 / 清洁设计**选择（隔离负向测试关注点供 E1.x/E2.x 清晰消费），非编辑保护所致（`_helper.ts` 可编辑但职责混杂不可取）。

**enforcement 状态**：三开关 OFF（P2.1 预置）；本计划**不翻转**任何开关（归 P2.4/E1.x/E2.x）。

**缺口**：(1) 无 enforcement 拒绝（动作级）专用断言原语；(2) 无 data-auth 行过滤（数据级）专用断言原语；(3) enforcement 拒绝运行时形状未表征；(4) 无负向测试脚手架（角色登录 indirection + 独立目录/模块）。

## Goals

- **交付「未授权动作被拒绝」断言原语**：rejection-source-agnostic 的 `expectActionDenied(result, tokenHint?)` 原语——断言 GraphQL `{errors}` 存在 + 可选 token 匹配，适用于业务逻辑拒绝（已有）与 enforcement 拒绝（待 P2.4 翻启后运行时确认形状）。
- **交付「越权数据被过滤」断言原语**：`expectRowsHidden(page, entity, filter, selection)` / `expectRowsVisible(page, entity, filter, expectedCount)` 原语——断言 data-auth 行过滤后特定行不可见/可见行集收敛（基于既有 `findItems`/`findPageTotal`，封装过滤断言语义）。
- **交付负向测试脚手架**：独立 helper 模块（`tests/e2e/negative/` 子目录 + 非下划线 helper）+ 角色登录 indirection 接口（`loginAsRole(page, roleOrUser)` 占位，实际账号由 P2.2b 填充；本计划占位回退 nop admin）——使 E1.x/E2.x 可插拔负向账号。
- **表征 enforcement 拒绝形状（Phase 1 探查）**：经平台源（`DefaultActionAuthChecker` + GraphQL error 处理）+ Nop 文档（`nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md`）表征动作级 enforcement 拒绝的预期 ErrorCode/token/HTTP 形状，原语据此设计；**运行时确认延后**至 P2.4（action-auth 翻启可达）并登记为 Follow-up。
- **1 例可运行冒烟 demo**：证明 `expectActionDenied` 原语机制正确执行并正确分类一次拒绝——在无负向账号前提下，复用一个**既有业务逻辑拒绝**（如 hr-payroll markPaid UNSUBMITTED 守卫，token「不允许执行该操作」）作为 demo 主体，证明原语对 `{errors}` + token 的检测机制成立（rejection-source-agnostic 设计的体现）。data-auth 行过滤原语的运行时 demo 因需 data-auth 翻启（E2.1）登记为 Follow-up，本计划仅交付原语 + 静态签名 Proof。
- **owner doc 对齐 + 日志**：`e2e-runbook.md` 增「负向隔离测试原语」节（原语清单 + demo 范式 + enforcement 拒绝形状表征结果）；日志条目。

## Non-Goals

- **不翻转任何 enforcement 开关**（三开关保持 OFF；动作级 enforcement 运行时拒绝确认归 P2.4，data-auth 运行时过滤确认归 E2.1）。
- **不做逐域角色账号 / fixture 角色参数化的账号侧**（归 P2.2b——本计划仅交付 `loginAsRole` indirection 占位，账号由 P2.2b 填充）。
- **不做 E1.x/E2.x 真实负向测试用例**（归 E1.1/E2.3——本计划仅原语 + 脚手架 + 1 demo；roadmap 明示「验收实测随 E 段」）。
- **不改既有业务断言语义**（roadmap §横切 4：测试语义保持不变——本计划新增原语不改既有 spec 的业务断言）。
- **不修改既有 `**/_*.java`/`.orm.xml`/`.xbiz` 等 deny 保护的生产文件**（`opencode.json` deny 规则；本计划仅触 `.ts` 测试文件 + 文档，均 allow）。
- **不改 ORM / Java 业务代码 / auth 种子**（本计划纯测试侧 TS 原语 + 文档）。
- **不在本计划内运行时确认 enforcement 拒绝形状**（action-auth OFF 下不可观测；Phase 1 经源 + 文档静态表征，运行时确认 Follow-up gated on P2.4）。

## Task Route

- Type: `implementation-only change`（测试侧 TS 原语 + 脚手架 + owner doc；无 Java/ORM/契约/auth 种子变更，无 enforcement 翻转）
- Owner Docs: `docs/testing/e2e-runbook.md`（新增「负向隔离测试原语」节）；`docs/design/roles-and-permissions.md` §运行基线（负向原语就绪注记，若影响运行基线表述）
- Skill Selection Basis: roadmap P2.3 指定 `nop-testing`。本计划核心 = E2E 测试原语设计 + 1 demo + 形状表征（平台源 + 文档）。`nop-testing` 路由测试基类/原语范式选择与基线对照方法。无 BizModel/Java 业务代码。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。三开关保持 OFF（本计划不改 enforcement 运行时行为）。demo 复用既有 webServer 链 + 业务逻辑拒绝（不依赖 enforcement 翻启）。

## Execution Plan

### Phase 1 - enforcement 拒绝形状表征 + 原语 API 裁决

Status: planned
Targets: 本计划内 Decision/表征记录
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: P2.1（done）+ P1.5b（done）

- [ ] **Proof（表征）**：动作级 enforcement 拒绝形状静态表征——经平台源（`DefaultActionAuthChecker.isPermitted/isDenied` 拒绝路径抛何种异常/ErrorCode + `nop-entropy` GraphQL error 序列化把 NopException 映射到 `errors` 数组的 token/HTTP status）+ Nop 文档（`nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md` enforcement 拒绝描述）表征预期形状。产出：动作级 enforcement 拒绝的预期 `{errors:[{message/messageExtensions:{errorCode}}]}` token + 是否伴随 HTTP 403。**运行时确认延后**（action-auth OFF 不可观测），登记为 Follow-up（gated on P2.4）。若源/文档不可定论形状 → 原语设计为「errors 存在 + 可配 token + errorCode 容错」，运行时确认时收敛。
  - Skill: `nop-testing`
- [ ] **Decision**：原语 API 形状。**采纳** rejection-source-agnostic 设计——`expectActionDenied(result, opts?)`：`result` = `callMutation`/`callQuery` 返回的 `{data,errors,json}`；断言 `errors` 真值 + 可选 `opts.token`（中文 token 子串）/ `opts.errorCode`（精确 ErrorCode）匹配；返回 errors 供链式断言。考虑的替代方案：(a) enforcement 专用 `expectPermissionDenied` 硬编码权限 token——**拒绝**：enforcement 拒绝形状运行时未确认（P2.4 前），硬编码 token 风险高，且与既有业务拒绝原语重复；(b) rejection-source-agnostic 通用原语——**采纳**：复用既有 `{errors}` 信封（业务 + enforcement 同形），token/errorCode 可选，运行时确认后可加 enforcement 专用 token 常量。残留风险：源-agnostic 可能在 enforcement 拒绝形状与业务拒绝形状发散时漏检——经 Phase 1 表征 + P2.4 运行时确认收敛。
  - Skill: none
- [ ] **Decision**：data-auth 行过滤原语 API 形状。**采纳** `expectRowsHidden(page, entity, filter, selection)`（断言匹配 filter 的行集为空——越权行被过滤）+ `expectRowsVisible(page, entity, filter, expectedCount?)`（断言可见行集收敛至 expectedCount 或 ≥1）。基于既有 `findItems`/`findPageTotal` 封装。考虑的替代方案：(a) 快照对比（前后行集 diff）——**拒绝**：E2E fresh-DB 单组织基线，快照对比增复杂度无收益；(b) 显式 absent/present 断言——**采纳**：data-auth 静默过滤（无 errors），absent/present 是唯一可观测信号。残留风险：无。
  - Skill: none
- [ ] **Decision**：脚手架落位 + `loginAsRole` indirection。**采纳** 新建 `tests/e2e/negative/` 子目录 + `negative/_helper.ts`。**编辑权限已实测确定**（`opencode.json` 无 `**/_*.ts` deny 规则 + L17 显式 allow `tests/e2e/**/_helper.ts`）→ `negative/_helper.ts` 可自由创建/编辑，**无需 Phase 1 探查**（原草案误判为须探查的开放项已订正）。独立模块落位的理由 = 关注点分离（隔离负向测试原语供 E1.x/E2.x 清晰消费），非编辑保护。`loginAsRole(page, roleOrUser)` 占位：接受角色名/用户名，本计划回退 `performLogin`（nop admin），P2.2b 账号就绪后填充真实角色登录。考虑的替代方案：(a) 复用/扩展 `business-actions/_helper.ts`（可编辑）追加负向原语——**拒绝**：职责混杂（正向业务动作原语 vs 负向隔离原语），E1.x/E2.x 消费不清晰；(b) 独立 negative 模块——**采纳**：隔离负向测试关注点。  残留风险：`loginAsRole` 占位在 P2.2b 前仅 admin（无法真负向），但骨架不依赖（roadmap 明示）。
  - Skill: none

Exit Criteria:

> Phase 1 为表征 Proof + 三项 Decision，无代码变更。表征产出动作级 enforcement 拒绝预期形状（运行时确认 Follow-up）；三项 Decision（原语 API × 2 + 脚手架落位）落地，可被 Phase 2 直接消费。

- [ ] 动作级 enforcement 拒绝形状静态表征产出（预期 token/errorCode/HTTP，或源不定论时记录容错策略 + Follow-up）
- [ ] 原语 API Decision（expectActionDenied rejection-source-agnostic + 替代方案 + 残留风险）
- [ ] data-auth 行过滤原语 API Decision（expectRowsHidden/Visible + 替代方案 + 残留风险）
- [ ] 脚手架落位 + loginAsRole indirection Decision（落位 `tests/e2e/negative/_helper.ts` + 编辑权限已实测 allow + 占位策略）

### Phase 2 - 原语 + 脚手架实现

Status: planned
Targets: `tests/e2e/negative/`（新子目录 + helper 模块）
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 1（三项 Decision）

- [ ] **Add**：新建 `tests/e2e/negative/` 子目录 + `negative/_helper.ts` 模块（`.ts` 文件 allow，可自由创建）。模块导出：(1) `expectActionDenied(result, opts?)`（断言 `errors` 真值 + 可选 token/errorCode；rejection-source-agnostic）；(2) `expectRowsHidden(page, entity, filter, selection)` + `expectRowsVisible(page, entity, filter, expectedCount?)`（封装 `findItems`/`findPageTotal` 过滤断言）；(3) `loginAsRole(page, roleOrUser)` 占位（回退 `performLogin`，P2.2b 填充）。模块顶部 JSDoc 说明 rejection-source-agnostic 设计 + enforcement 拒绝形状运行时确认 Follow-up（P2.4）。**复用策略**：从 `business-actions/_helper.ts` re-export `callMutation`/`callQuery`（消费者便利——负向 spec 单一 import 入口，避免跨模块 import 认知负担；非编辑保护所致）。
  - Skill: `nop-testing`
- [ ] **Proof**：原语静态签名校验——TS 编译通过（`npx tsc --noEmit` 或 Playwright 内建类型检查）+ 模块导出可达。证明原语 API 形状（Phase 1 Decision）正确落地。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付原语 + 脚手架模块 + 静态签名 Proof。demo 可运行性归 Phase 3。完整 E2E 套件归 Closure Gates。

- [ ] negative 子目录 + helper 模块创建，三项原语（expectActionDenied / expectRowsHidden / expectRowsVisible）+ loginAsRole 占位导出
- [ ] TS 类型检查通过（原语签名正确）

### Phase 3 - 冒烟 demo + owner doc + 日志

Status: planned
Targets: `tests/e2e/negative/<demo>.smoke.spec.ts`；`docs/testing/e2e-runbook.md`；`docs/logs/2026/08-09.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [ ] **Add**：1 例可运行冒烟 demo spec——`tests/e2e/negative/action-denied.smoke.spec.ts`：复用一个既有业务逻辑拒绝作为 demo 主体（推荐 hr-payroll markPaid UNSUBMITTED 守卫：建 UNSUBMITTED salary → `callMutation('ErpHrSalary','markPaid',...)` → `expectActionDenied(rej, { token: '不允许执行该操作' })`），证明 `expectActionDenied` 原语对 `{errors}` + token 的检测机制成立（rejection-source-agnostic：业务拒绝与 enforcement 拒绝同信封）。**setup 机制**：hr-payroll 的 `setupPayrollChain`/`cleanupSetup` 为 spec-internal 未导出，demo **重新实现最小内联 setup**（employee + ACTIVE EmploymentContract + `calculateSalary` 产出 UNSUBMITTED salary）+ 内联 cleanup（按反依赖链删 salary/contract/employee），镜像既有 hr-payroll spec 范式，自包含不污染共享 DB。demo 顶部 JSDoc 说明：此为原语机制 Proof（业务拒绝载体），enforcement 拒绝运行时确认随 E1.x（action-auth 翻启后用同原语断言真权限拒绝）。
  - Skill: `nop-testing`
- [ ] **Proof**：冒烟 demo 可运行——demo spec 经 webServer（flux 引擎，三开关 OFF）跑通绿，证明原语机制成立 + 脚手架可执行。记录为 Closure 证据。
  - Skill: `nop-testing`
- [ ] **Add**：owner doc——`e2e-runbook.md` 新增「负向隔离测试原语」节：原语清单（expectActionDenied/expectRowsHidden/expectRowsVisible + loginAsRole）+ demo 范式（业务拒绝载体）+ enforcement 拒绝形状表征结果（Phase 1 静态表征 + 运行时确认 Follow-up gated on P2.4）+ data-auth 行过滤 demo Follow-up（gated on E2.1）。`docs/logs/2026/08-09.md` 增 P2.3 条目（reverse-chronological）。
  - Skill: none

Exit Criteria:

> Phase 3 交付 1 例可运行冒烟 demo + owner doc + 日志。完整 E2E 套件零回归 + compliance 归 Closure Gates。

- [ ] 冒烟 demo spec 创建并跑通绿（原语机制 Proof）
- [ ] owner doc「负向隔离测试原语」节 + 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（0 blocker / **1 major** / 3 minor）（ses_0192128f3ffeyXYOKQED7qYT3X）。独立子代理冷重读 + 实时仓库核验：Deps（P2.1 + P1.5b done）准确、范围忠实于 P2.3、核心设计张力（rejection-source-agnostic 原语 + 业务拒绝 demo 载体 + 三 Follow-up 正确分类）经裁定**诚实合规**（roadmap「1 例负向断言可运行」+「骨架不依赖账号」明示解耦），规则 4/6/7/9/12/13 + anti-slack 通过。**M1（major，规则 1）**：`**/_*` 编辑保护前提**事实错误**——`opencode.json` deny 规则按扩展名限定（`_*.java/.xmeta/.xbiz/.orm.xml/.beans.xml/.i18n.yaml/.action-auth.xml`），**无 `_*.ts` deny**；且 L17 显式 allow `tests/e2e/**/_helper.ts` → `_helper.ts` 可自由编辑，草案 4 处「受保护」误判。修正：Current Baseline + Non-Goal + Phase 1 Decision 3 + Phase 2 Add 全部订正为「`.ts` 均 allow，独立模块 = 关注点分离设计非编辑保护」，Phase 1「编辑保护探查」开放项取消（实测确定）。minor 已采纳：m1 demo setup 机制钉死（hr-payroll setup spec-internal 未导出 → demo 重新实现最小内联 setup + 反依赖链 cleanup）；m3 re-export 理由改「消费者便利」非编辑保护。m2（Closure Gates mvn build 用于 compliance 基线稳定非 Java 编译）信息性保留。设计 robust to error（审查原话）。
- Independent draft review iteration 2: **accept**（0 blocker / 0 major / 1 minor 信息性）（ses_0191c4ef7ffetzr1YVk4MhYGUj）。独立子代理冷重读修订版 + 实时仓库复核：**M1 fully resolved**——opencode.json 实测复核（无 `_*.ts` deny + L17 allow `_helper.ts`），5 处「受保护」订正全部到位（Current Baseline / Non-Goal / Phase 1 Decision 3 probe 移除 / Phase 1 Exit Criteria / Phase 2 Add re-export 理由），Draft Review Record iteration 1 诚实记录，无新问题引入。核心设计（rejection-source-agnostic 原语 + 业务拒绝 demo + 三 Follow-up）完整保留；Deps P2.1+P1.5b done；规则 1/4/6/7/9/12/13 + anti-slack + lean exit + Closure Gates 通过。1 minor（Decision 3 重复 `Skill: none` 行，移除 probe 残留）已清理。共识达成，Plan Status → active。

## Closure Gates

> 本计划新增测试侧 TS 原语 + 脚手架 + 1 demo + owner doc。改 0 生产 Java/ORM/契约/auth 种子；三开关保持 OFF（不改运行时拦截）。Closure Gates 跑 demo 可运行 + 完整 E2E 套件零回归 + compliance checker 对照 `known-good-baselines.md` 零漂移 + 完整 build。

- [ ] 范围内行为完成（expectActionDenied/expectRowsHidden/expectRowsVisible 原语 + loginAsRole 占位 + 脚手架模块 + 1 冒烟 demo + owner doc 节）
- [ ] 相关文档对齐（`e2e-runbook.md` 负向隔离测试原语节）
- [ ] 已运行验证：冒烟 demo 跑通绿 + 全 E2E 套件零回归（flux 引擎，三开关 OFF，新原语不侵入既有 spec）+ `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 动作级 enforcement 拒绝形状运行时确认

- Classification: `watch-only residual`
- Why Not Blocking Closure: action-auth OFF 下 enforcement 拒绝不可观测；Phase 1 经平台源 + 文档静态表征产出预期形状，原语 rejection-source-agnostic 设计容错。运行时确认（enforcement 专用 token/errorCode 收敛）随 P2.4（action-auth 翻启可达）。
- Successor Required: yes（触发条件 = P2.4 翻 enable-action-auth，运行时确认后原语可加 enforcement 专用 token 常量）

### data-auth 行过滤原语运行时 demo

- Classification: `watch-only residual`
- Why Not Blocking Closure: data-auth 双层门控 OFF 下行过滤不可观测；本计划交付原语 + 静态签名 Proof，运行时 demo（真过滤 absent 断言）随 E2.1（data-auth 翻启）。
- Successor Required: yes（触发条件 = E2.1 翻 enable-data-auth + role-row-filter-enabled）

### 负向账号主体（非 admin 受限账号）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 P2.2b（角色化渐进）——逐域补角色账号 + fixture 角色参数化。本计划 `loginAsRole` 占位回退 nop admin，骨架不依赖负向账号（roadmap 明示）；E1.x 负向真拒绝断言消费 P2.2b 账号。
- Successor Required: yes（触发条件 = P2.2b 进入填充 loginAsRole 真实角色登录）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- <待独立结束审计后填写>

Follow-up:

- <非阻塞 successor 见 §Deferred But Adjudicated：动作级 enforcement 拒绝形状运行时确认（归 P2.4，触发=翻 enable-action-auth）/ data-auth 行过滤原语运行时 demo（归 E2.1，触发=翻 data-auth 双开关）/ 负向账号主体（归 P2.2b，触发=P2.2b 进入）>
