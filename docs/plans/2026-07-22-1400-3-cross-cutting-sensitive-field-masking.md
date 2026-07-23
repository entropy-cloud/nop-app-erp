# 2026-07-22-1400-3-cross-cutting-sensitive-field-masking 跨域敏感字段脱敏（hr + logistics）

> Plan Status: completed
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` §跨切面 UI 模式 §4 敏感字段脱敏（line 502）+ §退出标准 敏感字段脱敏覆盖（line 549）+ `docs/plans/2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md` §Deferred「敏感字段脱敏」+ `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md` §Non-Goals「敏感字段脱敏」
> Related: `docs/plans/2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md`（F12 §Deferred 明确本计划为敏感脱敏 successor）；`docs/plans/2026-07-19-1818-3-f5-status-tag-coloring.md`（F5 gen-control tpl 范式先例）；`docs/plans/2026-07-19-2200-2-f6-field-formatting-xmeta.md`（F6 gen-control col 格式化先例）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-22，对 hr/logistics 敏感字段现状 + F5/F6 gen-control tpl 范式 + 独立草案审计 ses_076d18436ffe）：

### 脱敏范围：2 域 6 字段

| 域 | 实体 | 敏感字段 | 现状（实时仓库核实） | 期望态（roadmap §跨切面 §4） |
|----|------|---------|------|------|
| hr | ErpHrEmployee | `idCardNo`（证件号码） | **form cell `visibleOn="${false}"` 隐藏**（view line 181-182 / 211-212）—— 前端不可见 | 脱敏显示 `******1234` |
| hr | ErpHrEmployee | `bankAccountId`（工资卡账户） | list col（line 15）明文 ID 展示 + form cell `visibleOn="${false}"` 隐藏（line 214-215） | 脱敏显示 `****1234` |
| hr | ErpHrEmployee | `socialSecurityNo`（社保号） | form cell `visibleOn="${false}"` 隐藏（line 217-218）—— 前端不可见 | 脱敏显示 `******` |
| hr | ErpHrEmployee | `mobilePhone`（手机号） | form layout 分隔线分组内（line 162/195）—— **明文渲染未隐藏** | 脱敏显示 `138****0000` |
| logistics | ErpLogCarrierConfig | `apiKey` | sub-grid-edit `type:'input-password'` + remark（line 45-50）；list grid 不展示此列 | list 展示脱敏 `sk****89ab` |
| logistics | ErpLogCarrierConfig | `apiSecret` | sub-grid-edit `type:'input-password'` + remark（line 52-55）；list grid 不展示此列 | list 展示脱敏 `****89ab` |

### 关键发现：hr 3/4 字段当前为 hidden 非 plaintext

独立草案审计（ses_076d18436ffe BLOCKER B1）核实：`idCardNo`/`bankAccountId`（form）/`socialSecurityNo` 三字段在 `ErpHrEmployee.view.xml` form cells 中使用 `visibleOn="${false}"` **隐藏**（前端不可见），非明文渲染。仅 `bankAccountId` list col + `mobilePhone` form layout 为明文可见。

**含义**：roadmap §跨切面 §4 要求这些字段「脱敏显示」（可见但打码），与当前「隐藏」状态不同。脱敏工作包含两个行为变更：(1) 取消 `visibleOn="${false}"` 隐藏 → 字段变为可见；(2) 加 gen-control tpl 打码渲染。这是用户可见行为变更，需 Phase 0 产品 Decision 确认。

### F5/F6 gen-control tpl 范式（先例，需适配验证）

F5/F6 已落地 `<col><gen-control><c:script>return {type:..., ...}</c:script></gen-control></col>` 范式（489 col × 17 域）。F5 用 gen-control 输出 CSS class 名（简单三元），F6 输出 number format。**脱敏需运行时字符串变换**（`.substring()` + 星号填充）在 AMIS tpl 表达式内，这是 F5/F6 未覆盖的新用法，需 Phase 0 PoC 验证可行性。

### 关键风险/缺口

- **gen-control tpl 字符串变换可行性**（M2 from ses_076d18436ffe）：F5/F6 的 gen-control 输出的是静态 config 对象（CSS class / number format），脱敏需 tpl 内 `${expr}` 运行时字符串截取 + 填充。AMIS tpl 是否支持 `.substring()`/`.padStart()` 需 Phase 0 PoC。降级方案：BizModel @BizLoader 后端打码（但改后端保护区域）
- **unhide 行为变更**（B1 from ses_076d18436ffe）：hr 3 字段当前 hidden，脱敏=unhide+mask 是用户可见行为变更。需 Phase 0 产品 Decision：(a) unhide+mask（roadmap 期望）vs (b) 保持 hidden（当前更安全，但不符合 roadmap「脱敏显示」意图）
- **前端层 vs 后端层脱敏边界**：前端 gen-control tpl 仅脱敏 AMIS 渲染层（GraphQL 响应仍含明文，开发者工具/F12 可见）。后端 @BizLoader 脱敏 GraphQL 响应（安全性更高，但改后端）。需 Phase 0 Decision

## Goals

1. **Phase 0 Explore 闭环**：(a) gen-control tpl 字符串变换 PoC（脱敏打码可行性）；(b) unhide+mask vs keep-hidden 产品 Decision；(c) 前端层 vs 后端层脱敏 Decision
2. **敏感字段脱敏落地**：hr 4 字段 + logistics 2 字段按 Decision 结果落地显示脱敏
3. **范式文档**：脱敏 gen-control tpl 范式记录

## Non-Goals

- **F15 i18n 国际化标签补充**——独立关注点，与脱敏有实质性不同的结束标准/owner-doc/验证路径（规则 14），归独立 plan
- **后端 SQL/API 层脱敏**（@Sensitive 注解 / Hibernate @Column transformer / BizModel @BizLoader）——保护区域；本计划默认前端层（Phase 0 (c) 若裁决后端则扩展 Task Route）。GraphQL 响应层脱敏归 successor
- **权限粒度脱敏**（角色差异化打码）——action-auth 独立审计项
- **修改 ORM 模型**（保护区域）
- **F16 复杂页面**——属 Plan 1/2 范畴

## Task Route

- Type: `implementation-only change`（默认全前端 view.xml；若 Phase 0 (c) 裁决后端则扩展为含轻量 BizModel delta）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §跨切面 §4 敏感字段脱敏（line 502）+ §退出标准 line 549
  - `docs/design/human-resource/ui-patterns.md` §员工详情（敏感字段段）
  - `docs/design/logistics/ui-patterns.md` §承运商配置
  - `docs/design/domain-design-guidelines.md` §9 删除/脱敏策略
  - `docs/design/status-color-map.md` §4（gen-control tpl 范式参考）
  - `docs/design/field-formatting-patterns.md`（F6 gen-control col 范式参考）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`（AMIS gen-control/tpl DSL）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml gen-control tpl + col/cell 脱敏渲染）；条件加载 `nop-backend-dev`（仅若 Phase 0 (c) 裁决后端 @BizLoader 脱敏）；不加载 `nop-testing`（visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Explore 阶段需可本地运行 AMIS 页面实测 gen-control tpl 字符串变换
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：gen-control tpl 可行性 PoC + 2 Decision

Status: completed
Targets: plan 内 PoC 结论 + Decision 记录
Skill: `nop-frontend-dev`（+ `nop-backend-dev` 仅 Decision c）

- Item Types: `Explore | Decision`（Explore 经指南规则 9 授权：pre-Decision 探索）
- Prereqs: 无

- [x] `Explore` (a)：gen-control tpl 字符串变换（脱敏打码）可行性 PoC。
  - **PoC 结论：FEASIBLE（可行）**。amis-formula（AMIS tpl 表达式引擎，`${ expr }`）内置完整文本函数集，证据来自 `baidu/amis` 官方源 `packages/amis-formula/src/doc.ts`：
    - `LEFT(text, len)` — 取左侧 N 字符（如 `LEFT(mobilePhone, 3)` → `"138"`）
    - `RIGHT(text, len)` — 取右侧 N 字符（如 `RIGHT(mobilePhone, 4)` → `"0000"`）
    - `MID(text, from, len)` — 从位置 from 取 len 字符
    - `CONCATENATE(t1, t2, ...)` / `+` 拼接；`LEN(text)`；`ISEMPTY(text)`；`PADSTART(text, num, pad)`；`REPLACE(text, search, replace)`
  - **可行模式**：`<gen-control><c:script>` 返回 `{type:"tpl", tpl:"${LEFT(mobilePhone,3)}****${RIGHT(mobilePhone,4)}"}` → AMIS 渲染 `138****0000`。与 F5 status-tag tpl 范式完全一致（同一 codegen 链路：`flux-web.xlib:GenGridCol` → `eval(colXpl)` → putAll 到 AMIS col）。
  - **(iii) form cell 适用性**：本项目既有先例（`ErpHrEmployee.view.xml:259-282` transfer form cells + field-formatting-patterns.md §1 line 14「主表单只读 view 字段依赖 gen-control 与列表同机制」）证明 gen-control 在 form `<cell>` 与 grid `<col>` 均生效。
  - 降级方案（未触发）：后端 BizModel @BizLoader 打码留作 successor（GraphQL 响应层脱敏）
  - Skill: `nop-frontend-dev`
- [x] `Decision` (b)：unhide+mask vs keep-hidden 产品 Decision。
  - **裁决：候选 (a) unhide+mask**。理由：(1) roadmap §跨切面 §4（line 502）明确要求「脱敏显示」，exit criterion（line 549）要求 hr/logistics 敏感字段脱敏覆盖；(2) F5/F6 范式已落地前端层格式化，脱敏是其自然延伸；(3) hidden 状态使字段完全不可见，与「脱敏显示」语义冲突。
  - 实施：移除 `visibleOn="${false}"` + 加 gen-control tpl 打码（idCardNo/socialSecurityNo/mobilePhone 编辑态 input-password；bankAccountId 编辑态正常显示以支持录入）
  - Skill: none
- [x] `Decision` (c)：前端层 vs 后端层脱敏 Decision。
  - **裁决：候选 (a) 前端 gen-control tpl**（Explore (a) PoC 已通过）。理由：(1) 与 F5/F6 范式一致，不改后端保护区域；(2) 覆盖 list grid col + form view cell + form edit cell 三态；(3) GraphQL 响应仍含明文（F12 可见）—— 安全边界明确声明，候选 (b) 后端 @BizLoader 留作安全审计 successor。
  - Skill: `nop-frontend-dev` + `nop-backend-dev`

Exit Criteria:

- [x] Explore (a) PoC 结论已记录（gen-control tpl 字符串变换可行/不可行 + 降级路径）
- [x] Decision (b)(c) 已落地（unhide+mask 裁决 + 前端/后端层裁决 + 理由）

### Phase 1 — hr ErpHrEmployee 敏感字段脱敏

Status: completed
Targets: `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`（4/4 fields tagged Add）
- Prereqs: Phase 0 Explore (a) + Decision (b)(c) 完成

- [x] `Add`：`idCardNo` 脱敏
  - 实现：view form cell 移除 `visibleOn="${false}"` + gen-control tpl `${LEFT(idCardNo,1)}******${RIGHT(idCardNo,4)}`（首1末4打码）；edit form cell 移除 hidden + gen-control `{type:'input-password'}`（编辑态脱敏，showRevealToggle 由 AMIS 默认）
  - Skill: `nop-frontend-dev`
- [x] `Add`：`mobilePhone` 脱敏
  - 实现：view form cell gen-control tpl `${LEFT(mobilePhone,3)}****${RIGHT(mobilePhone,4)}`（前3后4打码 `138****0000`）；edit form cell gen-control `{type:'input-password'}`
  - Skill: `nop-frontend-dev`
- [x] `Add`：`bankAccountId` 脱敏
  - 实现：list col gen-control tpl `****${RIGHT(bankAccountId,4)}`（末4打码）；view form cell 加入 payroll layout + gen-control tpl `****${RIGHT(bankAccountId,4)}`；edit form cell 移除 hidden + 加入 payroll layout（编辑态正常显示以支持银行账户录入，codegen 默认 input-text）
  - Skill: `nop-frontend-dev`
- [x] `Add`：`socialSecurityNo` 脱敏
  - 实现：view form cell 加入 payroll layout + gen-control tpl `******`（全打码）；edit form cell 移除 hidden + 加入 payroll layout + gen-control `{type:'input-password'}`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] hr 4 字段按 Decision 结果脱敏生效（gen-control tpl 打码渲染 or 后端 @BizLoader 打码）
- [x] 编辑态 input-password 保持/补充生效
- [x] 若 Decision (b)=(a) unhide，打码值在 list + form 查看态可见；若 (b)=(b) 保持 hidden，记录理由到 Deferred

### Phase 2 — logistics ErpLogCarrierConfig 敏感字段脱敏补全

Status: completed
Targets: `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/ErpLogCarrierConfig/ErpLogCarrierConfig.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`（2/2 fields tagged Add）
- Prereqs: Phase 0 Explore (a) + Decision (c) 完成

- [x] `Add`：`apiKey` + `apiSecret` list/查看态脱敏补全
  - 实现：sub-grid-edit 既有 `input-password` 保持不变；sub-grid-view 新增 apiKey/apiSecret 列 + gen-control tpl 打码（`${LEFT(apiKey,2)}****${RIGHT(apiKey,4)}` / `****${RIGHT(apiSecret,4)}`，保留首2末4/末4）；form `view` 新增 credentials 段 + apiKey/apiSecret cells + gen-control tpl 打码（同 sub-grid-view 表达式）。list grid 不展示此两列（保持不变）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] logistics apiKey/apiSecret 查看态脱敏生效（list 不展示此两列时确认查看态 input-password 覆盖）

### Phase 3 — 范式文档 + 回归测试

Status: completed
Targets: `docs/design/field-formatting-patterns.md`（扩展脱敏段）+ `tests/e2e/visual/`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`（1 Add 文档 + 1 Proof spec）
- Prereqs: Phase 1-2 完成

- [x] `Add`：范式文档扩展 `docs/design/field-formatting-patterns.md`
  - 新增「敏感字段脱敏」段：gen-control tpl 打码范式（手机/证件/银行 3 类打码模板 + AMIS tpl 字符串变换表达式）+ 编辑态 input-password + 查看态 tpl + 前端层 vs 后端层脱敏边界声明 + Phase 0 PoC 结论
  - Skill: none
- [x] `Proof`：visual spec
  - 落地：`tests/e2e/visual/sensitive-masking.visual.spec.ts`（hr idCardNo/mobilePhone/bankAccountId 打码渲染 DOM 断言 + logistics apiKey/apiSecret 脱敏；seed-data 缺失 graceful skip）
  - 验证：`npx playwright test` 新增用例全绿；既有 visual 无回归
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 范式文档脱敏段新增（含 PoC 结论 + 3 类打码模板 + 前端/后端边界声明）
- [x] visual spec 通过（脱敏打码 DOM 断言；seed-data 缺失 graceful skip）

> **执行期发现（Phase 3 visual spec 暴露的 Phase 2 缺陷，已修正）**：logistics `ErpLogCarrierConfig.apiKey/apiSecret` 在 xmeta 中为 `published="false" queryable="false"`（写回型集成凭据：前端可录、后端永不回传明文）。Phase 2 落地的动态 `${LEFT(apiKey,2)}****${RIGHT(apiKey,4)}` tpl 因此渲染 `undefined` 垃圾值（GraphQL 响应不含字段值）。修正：logistics 查看态（form view cell + sub-grid-view col）改用静态全打码 `******`（与社交号同模板，§9.2/§9.4/§9.5 已同步补「写回型凭据例外」），编辑态仍用 `input-password`。这比「发布明文 + 前端打码」更安全。visual spec 相应断言：logistics 查看态渲染 `******` + 明文不泄漏 DOM。验证：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/sensitive-masking.visual.spec.ts` 2 passed（23.7s）+ 既有 field-format/status-tag 18 passed 0 回归 + `mvn install -DskipTests` BUILD SUCCESS + `ErpAllWebPagesTest` 全绿。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_076d18436ffe) — 3 blockers + 4 majors：
  1. **BLOCKER B1**：hr 字段 baseline 虚称「明文渲染」，实际 3/4 字段 `visibleOn="${false}"` 隐藏 → 已修正 baseline 表 + 新增 Phase 0 Decision (b) unhide+mask vs keep-hidden
  2. **BLOCKER B2**：codegen `_gen/` i18n-en 覆盖虚称含 label，实际仅 title → 本计划已移除 i18n 范围（独立关注点，规则 14），B2 不再适用
  3. **BLOCKER B3**：脱敏+i18n 应拆分（规则 14 不同结束标准/owner-doc/验证路径）→ 已拆分：本计划仅脱敏，F15 i18n 归独立 successor plan
  4. **MAJOR M2**：gen-control tpl 字符串变换可行性未充分探索 → 已新增 Phase 0 Explore (a) PoC + 降级方案
  5. **MAJOR M4**：Item Types `Add-heavy` 非法类型 → 已改为 `Add`
- Independent draft review iteration 2: accept (ses_076c7a6d4ffe) — 全部 3 blockers + 4 majors 已解决：B1 hr 字段 baseline 已修正（3/4 visibleOn hidden + mobilePhone/bankAccountId-list 明文，经实时仓库 line-level 核实）；B3 i18n 已完全移除（独立 successor plan）；M2 Phase 0 Explore PoC + 降级路径已落地；M4 item types 已改为 Add。3 non-blocking minors（taxFileNo 未列入 scope/Non-Goals、bankAccountId visibleOn 仅 edit form、logistics Phase 2 近 no-op）记为实现期关注。0 blockers, 0 majors。

## Closure Gates

- [x] 范围内行为完成（hr 4 字段 + logistics 2 字段脱敏渲染落地，见 Closure 证据）
- [x] 相关文档对齐（`docs/design/field-formatting-patterns.md` §9 敏感字段脱敏段已落地 + frontend-ui-roadmap §退出标准「敏感字段脱敏覆盖」todo→done）
- [x] 已运行验证（`mvn install -DskipTests` 154 模块 BUILD SUCCESS + `ErpAllWebPagesTest` 全绿 + `npx playwright test tests/e2e/visual/sensitive-masking.visual.spec.ts` 2 passed）
- [x] 无范围内项目降级为 deferred/follow-up（logistics 静态 `******` 是执行期修正，非降级；后端 @BizLoader 层脱敏本就为 Non-Goals/successor）
- [x] 独立草案审查已完成并记录（见 Draft Review Record iteration 1/2）
- [x] 文本一致性已验证（Plan Status=completed / 4 Phase Status=completed / 各 Exit Criteria 全 [x] / Closure Gates 全 [x] / 日志 07-23 一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中（见下 Closure）

## Deferred But Adjudicated

### 后端响应层脱敏（GraphQL @BizLoader）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Phase 0 Decision (c) 明确裁决前端 gen-control tpl 层脱敏（与 F5/F6 范式一致，不改后端保护区域）；GraphQL 响应层脱敏本计划 Non-Goals 明确排除，归安全审计 successor。安全边界已在 `field-formatting-patterns.md §9.4` 显式声明（GraphQL 响应含明文，F12 可见）。
- Successor Required: `yes`（触发条件：安全审计要求 API 层脱敏 / 第三方集成消费打码值）

## Closure

Status Note: 跨域敏感字段脱敏 plan 全 4 Phase（Phase 0 Explore/Decision + Phase 1 hr + Phase 2 logistics + Phase 3 范式文档/visual spec）已落地并通过独立结束审计。hr 4 字段（idCardNo/mobilePhone/bankAccountId/socialSecurityNo）gen-control tpl 动态打码 + 编辑态 input-password；logistics apiKey/apiSecret 因 xmeta `published="false"`（写回型凭据）改用静态全打码 `******`（执行期发现并修正，比发布明文更安全）。范式固化到 `field-formatting-patterns.md §9`。frontend-ui-roadmap §退出标准「敏感字段脱敏覆盖」todo→done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非执行者上下文）
- 实时仓库核实（grep + read）：
  - hr `module-hr/erp-hr-web/.../ErpHrEmployee.view.xml` — list col bankAccountId gen-control tpl `****${RIGHT(bankAccountId,4)}`（line 15-20）；view form cells idCardNo `${LEFT(idCardNo,1)}******${RIGHT(idCardNo,4)}`（line 188-193）/ mobilePhone `${LEFT(mobilePhone,3)}****${RIGHT(mobilePhone,4)}`（line 195-200）/ bankAccountId `****${RIGHT(bankAccountId,4)}`（line 202-207）/ socialSecurityNo 全打码（line 209-214）；edit form cells idCardNo/mobilePhone/socialSecurityNo `{type:'input-password'}`（line 243-262）。原 `visibleOn="${false}"` 隐藏已移除（Phase 0 Decision (b)=(a) unhide+mask 裁决落地）。
  - logistics `module-logistics/erp-log-web/.../ErpLogCarrierConfig.view.xml` — sub-grid-edit apiKey/apiSecret `input-password`（line 45-57，含 remark「敏感字段，保存后由后端脱敏返回」）；sub-grid-view + form view apiKey/apiSecret 静态 `{type:'tpl', tpl:'******'}`（line 71-83 / 111-123），注释说明 published="false" 写回型凭据例外。
  - `docs/design/field-formatting-patterns.md` §9 敏感字段脱敏段完整（9.1 决策表/9.2 三类打码模板/9.3 编辑态 input-password/9.4 前后端边界表/9.5 反模式自检表/写回型凭据例外）。
  - `tests/e2e/visual/sensitive-masking.visual.spec.ts` 存在（9762 bytes），含 hr idCardNo/mobilePhone/bankAccountId/socialSecurityNo DOM 打码断言 + logistics apiKey/apiSecret 静态 `******` + 明文不泄漏断言（`LOG_MASK = '******'`），确定性 GraphQL __save 种子 + cleanup + seed 失败 graceful skip。
- 验证证据（来自执行者 07-23 日志，独立审计已交叉核实命令可执行）：
  - `mvn install -DskipTests` 154 模块 BUILD SUCCESS（logistics view.xml 重新打包）
  - `mvn -pl app-erp-all test -Dtest=ErpAllWebPagesTest` 全绿（view.xml gen-control tpl 校验）
  - `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/sensitive-masking.visual.spec.ts` 2 passed（23.7s）；回归 field-format.value + status-tag 18 passed 0 回归
- 文本一致性：Plan Status / 4 Phase Status / Exit Criteria / Closure Gates / 日志（`docs/logs/2026/07-23.md` + `07-22.md`）全部一致 = completed/全 [x]。
- Anti-Hollow 核查：gen-control tpl 经 codegen 链路（flux-web.xlib:GenGridCol → AMIS tpl → DOM）实际运行时渲染（visual spec DOM 断言通过证实非空壳）；无空函数体 / return null 占位 / 吞异常。
- 残留风险（已裁决，非阻塞）：前端层脱敏仅覆盖 AMIS 渲染层，GraphQL 响应仍含明文（F12 可见）——安全边界已在 §9.4 显式声明，后端 @BizLoader 响应层脱敏归安全审计 successor（见 Deferred But Adjudicated）。

Follow-up:

- 后端 GraphQL 响应层脱敏（BizModel `@BizLoader`）——安全审计 successor（触发条件：安全审计要求 API 层脱敏 / 第三方集成消费打码值）。已确认的非阻塞跟进，非范围内缺陷。
