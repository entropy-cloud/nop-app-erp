# 2026-07-22-1400-3-cross-cutting-sensitive-field-masking 跨域敏感字段脱敏（hr + logistics）

> Plan Status: active
> Last Reviewed: 2026-07-22
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

Status: planned
Targets: plan 内 PoC 结论 + Decision 记录
Skill: `nop-frontend-dev`（+ `nop-backend-dev` 仅 Decision c）

- Item Types: `Explore | Decision`（Explore 经指南规则 9 授权：pre-Decision 探索）
- Prereqs: 无

- [ ] `Explore` (a)：gen-control tpl 字符串变换（脱敏打码）可行性 PoC。
  - PoC 目标：以 `mobilePhone` 为试点，在 list col 加 `<gen-control><c:script>` 返回 AMIS tpl 控件，tpl 内对 `${value}` 执行字符串截取 + 星号填充（`138****0000` = 前 3 + `****` + 后 4）。验证：(i) AMIS tpl 表达式是否支持 `.substring()`/`.slice()`；(ii) 若不支持，验证 amis-formula `${'|xxxx'.replace(...)}` 或自定义 filter 是否可用；(iii) gen-control 是否可用于 form `<cell>`（非仅 col）
  - 降级方案：若 AMIS tpl 不支持运行时字符串变换，降级为后端 BizModel @BizLoader 打码（Phase 0 (c) 候选 b 自动当选）
  - Skill: `nop-frontend-dev`
- [ ] `Decision` (b)：unhide+mask vs keep-hidden 产品 Decision。
  - 背景：hr `idCardNo`/`bankAccountId`（form）/`socialSecurityNo` 当前 `visibleOn="${false}"` 隐藏；roadmap §跨切面 §4 要求「脱敏显示」。Decision：(a) 取消 `visibleOn="${false}"` + 加打码 gen-control（字段变为可见但打码，符合 roadmap）vs (b) 保持 hidden（当前更安全，但不满足 roadmap exit criterion）
  - 倾向候选 (a)：roadmap 明确要求脱敏显示，exit criterion line 549 要求覆盖
  - Skill: none
- [ ] `Decision` (c)：前端层 vs 后端层脱敏 Decision。
  - 候选 (a)：前端 gen-control tpl（不改后端，GraphQL 响应仍含明文，AMIS 渲染打码）—— 与 F5/F6 一致
  - 候选 (b)：后端 BizModel @BizLoader（GraphQL 响应即打码，安全性更高，但改后端保护区域）
  - 倾向候选 (a)（若 Explore (a) PoC 通过）：与 F5/F6 范式一致 + 不改后端。候选 (b) 归 successor（安全审计要求 API 层脱敏时）
  - Skill: `nop-frontend-dev` + `nop-backend-dev`

Exit Criteria:

- [ ] Explore (a) PoC 结论已记录（gen-control tpl 字符串变换可行/不可行 + 降级路径）
- [ ] Decision (b)(c) 已落地（unhide+mask 裁决 + 前端/后端层裁决 + 理由）

### Phase 1 — hr ErpHrEmployee 敏感字段脱敏

Status: planned
Targets: `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`（4/4 fields tagged Add）
- Prereqs: Phase 0 Explore (a) + Decision (b)(c) 完成

- [ ] `Add`：`idCardNo` 脱敏
  - 实现：按 Phase 0 Decision — 若 (b)=(a) unhide+mask：(1) 移除 form cell `visibleOn="${false}"`；(2) 加 gen-control tpl 打码（保留首末各 1-2 字符 + `******`，如 `张******1234`）；编辑态 input-password。若 (c)=(b) 后端层则改 BizModel @BizLoader
  - Skill: `nop-frontend-dev`
- [ ] `Add`：`mobilePhone` 脱敏
  - 实现：form layout 内 `mobilePhone` 明文 → gen-control tpl 打码 `138****0000`（保留前 3 后 4）；编辑态 input-password
  - Skill: `nop-frontend-dev`
- [ ] `Add`：`bankAccountId` 脱敏
  - 实现：list col（line 15）+ form cell 脱敏。list col gen-control tpl 打码 `****1234`（保留末 4）；form cell 若 Decision (b)=(a) 则 unhide + 打码；编辑态正常显示（银行账户需编辑录入）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：`socialSecurityNo` 脱敏
  - 实现：若 Decision (b)=(a)：移除 form cell `visibleOn="${false}"` + gen-control tpl 打码 `******`；编辑态 input-password
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] hr 4 字段按 Decision 结果脱敏生效（gen-control tpl 打码渲染 or 后端 @BizLoader 打码）
- [ ] 编辑态 input-password 保持/补充生效
- [ ] 若 Decision (b)=(a) unhide，打码值在 list + form 查看态可见；若 (b)=(b) 保持 hidden，记录理由到 Deferred

### Phase 2 — logistics ErpLogCarrierConfig 敏感字段脱敏补全

Status: planned
Targets: `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/ErpLogCarrierConfig/ErpLogCarrierConfig.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`（2/2 fields tagged Add）
- Prereqs: Phase 0 Explore (a) + Decision (c) 完成

- [ ] `Add`：`apiKey` + `apiSecret` list/查看态脱敏补全
  - 实现：既有 sub-grid-edit `input-password` 保持；若 list grid 展示此两列则补 gen-control tpl 打码 `sk****89ab`/`****89ab`（保留末 4）；查看详情态 gen-control tpl 打码。当前 list 不展示此两列则仅需确保查看态脱敏（若查看态使用 sub-grid-view 则复用 input-password 效果）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] logistics apiKey/apiSecret 查看态脱敏生效（list 不展示此两列时确认查看态 input-password 覆盖）

### Phase 3 — 范式文档 + 回归测试

Status: planned
Targets: `docs/design/field-formatting-patterns.md`（扩展脱敏段）+ `tests/e2e/visual/`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`（1 Add 文档 + 1 Proof spec）
- Prereqs: Phase 1-2 完成

- [ ] `Add`：范式文档扩展 `docs/design/field-formatting-patterns.md`
  - 新增「敏感字段脱敏」段：gen-control tpl 打码范式（手机/证件/银行 3 类打码模板 + AMIS tpl 字符串变换表达式）+ 编辑态 input-password + 查看态 tpl + 前端层 vs 后端层脱敏边界声明 + Phase 0 PoC 结论
  - Skill: none
- [ ] `Proof`：visual spec
  - 落地：`tests/e2e/visual/sensitive-masking.visual.spec.ts`（hr idCardNo/mobilePhone/bankAccountId 打码渲染 DOM 断言 + logistics apiKey/apiSecret 脱敏；seed-data 缺失 graceful skip）
  - 验证：`npx playwright test` 新增用例全绿；既有 visual 无回归
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 范式文档脱敏段新增（含 PoC 结论 + 3 类打码模板 + 前端/后端边界声明）
- [ ] visual spec 通过（脱敏打码 DOM 断言；seed-data 缺失 graceful skip）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_076d18436ffe) — 3 blockers + 4 majors：
  1. **BLOCKER B1**：hr 字段 baseline 虚称「明文渲染」，实际 3/4 字段 `visibleOn="${false}"` 隐藏 → 已修正 baseline 表 + 新增 Phase 0 Decision (b) unhide+mask vs keep-hidden
  2. **BLOCKER B2**：codegen `_gen/` i18n-en 覆盖虚称含 label，实际仅 title → 本计划已移除 i18n 范围（独立关注点，规则 14），B2 不再适用
  3. **BLOCKER B3**：脱敏+i18n 应拆分（规则 14 不同结束标准/owner-doc/验证路径）→ 已拆分：本计划仅脱敏，F15 i18n 归独立 successor plan
  4. **MAJOR M2**：gen-control tpl 字符串变换可行性未充分探索 → 已新增 Phase 0 Explore (a) PoC + 降级方案
  5. **MAJOR M4**：Item Types `Add-heavy` 非法类型 → 已改为 `Add`
- Independent draft review iteration 2: accept (ses_076c7a6d4ffe) — 全部 3 blockers + 4 majors 已解决：B1 hr 字段 baseline 已修正（3/4 visibleOn hidden + mobilePhone/bankAccountId-list 明文，经实时仓库 line-level 核实）；B3 i18n 已完全移除（独立 successor plan）；M2 Phase 0 Explore PoC + 降级路径已落地；M4 item types 已改为 Add。3 non-blocking minors（taxFileNo 未列入 scope/Non-Goals、bankAccountId visibleOn 仅 edit form、logistics Phase 2 近 no-op）记为实现期关注。0 blockers, 0 majors。
