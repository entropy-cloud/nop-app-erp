# 多维审计报告

**日期**: 2026-07-20  
**审计类型**: multi-dimensional-audit  
**审计对象**: nop-app-erp 项目文档与实现代码  
**审计依据**: `docs/skills/multi-dimensional-audit-prompt.md` + `docs/skills/README.md §项目定制化层（nop-app-erp）`

---

## 裁决

**passes multi-dimensional audit**

---

## 维度发现（按严重性排序）

### 1. 需求正确性 — ✅ 无阻塞问题

- 产品基线（`product-baseline.md` / `product-scope.md`）与 18+1 域 ORM 模型高度一致，feature-inventory 列 41 项核心功能 + 10 项扩展功能 + 8 平台复用功能，均映射到对应 owner doc。
- 前端 UI 路线图 `frontend-ui-roadmap.md` 驱动当前 F4/F5/F6/F7/F8/F9/F10 批次工作，每批次均有 plan → execute → verify 闭环。
- **残余风险**: 原始输入目录 `docs/input/` 为空（仅指南文件），无原始 PM/原型输入可追溯。当前需求综合完全基于内部设计推断，缺少外部对标锚定。该风险因项目已进入运营收尾阶段而非阻塞——需求已在多轮审计中稳定。

### 2. owner-doc 对齐 — ✅ 无阻塞问题

- 18 域 × 平均 4-6 文件设计文档，+ `domain-design-guidelines.md`（769 行）统辖全局约定。架构文档 32 文件覆盖模块边界、系统基线、集成模式、服务层编排等。
- 最新日志（07-20）显示 F7 non-status visibleOn 实施后同步更新了 3 域 `ui-patterns.md`；F9 cross-doc-navigation 后新建 `cross-doc-navigation-patterns.md`；F10 tree entity views 后新建 `tree-entity-patterns.md`。文档-代码对齐纪律良好。
- **残余风险**: `domain-design-guidelines.md`（769 行）体量过大，部分章节（如 §XIX 命名约定附录）可拆至独立文件。非阻塞。

### 3. 架构或边界影响 — ✅ 无阻塞问题

- 19 域标准 8 模块链（model → codegen → dao → meta → service → web → app → api）一致遵循。模块边界 DAG（`module-boundaries.md`）被严格遵守。
- 跨域引用全部通过 `notGenCode="true"` 外部实体声明 + `I*Biz` 接口，无反向跨域依赖。保护区域（ORM 模型 `ask-first`、会计/财务 `plan-first`）被尊重。
- 近期 view.xml 改动均为应用层页面定制，零 ORM 模型/API 契约/核心业务逻辑变更。
- **残余风险**: `PAGE_ERROR_COUNT=213` 预存基线（JDK 26 / antlr 版本不兼容）覆盖全 18 域全部 page，无法区分新增页面错误。该基线自 F7 批次已稳定（213 → 213），但可能掩盖新 page build error。

### 4. 验证充分性 — ✅ 无阻塞问题

- **Maven 构建**: 每次变更后均执行 `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，~1:30-3:05min）。关键批次还做全 reactor `mvn test`（~8-11min，0 failures）。
- **单元测试**: 95+ Java JUnit 测试覆盖 BizModel、成本计算、过账、状态机等。各域 service 模块测试全绿。
- **E2E 测试**: 300+ Playwright spec 覆盖 CRUD（~41）、业务动作（~97）、看板（~22）、报表（~47）、编排（~13）、视觉回归（~15）。每日回归套件全绿。
- **验证纪律**: 日志清晰区分 `scoped -pl` 验证（增量）与 full reactor 验证（聚合基线）。closure-audit-prompt 已于 07-20 新增「强制验证范围检查」规则。
- **残余风险**: xwf（useWorkflow）审批动作浏览器层被裁决为 NOT FEASIBLE（`2026-07-09-2330-1`），相关业务路径（Payment/Receipt/Disposal/Salary 审批）仅在单测覆盖。已记录在 e2e-runbook 已知限制段。

### 5. 回归风险 — ✅ 无阻塞问题

- 近期工作均有 Playwright 回归套件验证（如 F10 后 `readonly-views.visual.spec.ts` + `list-query-filter.visual.spec.ts` 28/28 PASS）。
- 2 个已知预存 bug（`ErpMdMaterialCategory.priceValidationLevel_label` dict 值不匹配、`ErpHrDepartment.manager` to-one 关系缺 sub-selection）经 git stash 验证非当前批次引入。
- **残余风险**: notify inbox page.inbox.page.yaml AMIS adaptor `data is not defined` 缺陷（Phase 2 残留，不影响其他域）。多个 plan 重复遭遇 `webServer.port=8080 vs 8011` 默认值不匹配，建议在 playwright.config.ts 或项目根 config 中统一固化。

### 6. 路由和技能选择正确性 — ✅ 无阻塞问题

- 07-20 刚完成全面的技能系统审计（26 份日志 × 21 个技能 → 12 缺口 → 5 文件修改 + 1 新建 + README 重写）。skill 注册表、触发规则、组合使用说明均已加固。
- `development-wisdom-gate-prompt.md` 新建填补了「开发中自检」空缺。
- Plan 中均记录 `Skill:` 使用情况，符合 AGENTS.md 要求。
- **残余风险**: 无。技能系统在经过 07-20 META 审计后已达到项目当前最高成熟度。

### 7. 待办或自主权策略漂移 — ✅ 无阻塞问题

- `docs/backlog/README.md` 清晰列出 P0-P8 工作项，当前 active 项为 P6（前端 UI 完整性），与 `frontend-ui-roadmap.md` 路线图一致。
- 自主权策略（`ai-autonomy-policy.md`）将 ORM 模型标记为 `ask-first`，会计/财务为 `plan-first`。日志显示所有触及 finance service 层的计划（如 07-18 维护工时过账、过账红冲闭环）均通过计划-执行-审计流程。
- `docs/backlog/README.md` 行 79 的「核销时点动态汇兑损益」标记为 `watch-only residual`，有明确的触发条件，策略清晰。
- **残余风险**: `docs/backlog/README.md` 已扩展至 100+ 行（大量已完成项+详细描述），列表过长可能影响下一个工作项的选择效率。建议考虑归档已完成行或将活跃项与非活跃项分离。

---

## 项目定制化层检查清单

| 检查项 | 结果 |
|--------|------|
| ORM 模型保护（无人手改 `model/*.orm.xml`） | ✅ 近期工作不触及 ORM 模型 |
| 会计/财务保护区域（`plan-first`） | ✅ 所有 finance 改动经 plan + audit 流程 |
| 生成产物保护（无人手改 `_gen/` / `_` 前缀文件） | ⚠️ notify inbox page 曾有两轮编辑 `_erp-notify.action-auth.xml`（生成文件）后被 codegen 还原，第三轮改为保留层定制。该模式已记录在 `nop-platform-conformance-audit-prompt.md` 维度 13 |
| 验证命令完整性 | ✅ `mvn clean install -DskipTests` / `mvn test` / playwright 均可用 |
| 已知失败模式 1（跨模块表前缀双重拼接） | ✅ 未发现新实例 |
| 已知失败模式 5（业务异常未扩展 NopException） | ✅ 未发现（新 ErrorCode 均遵循 `NopException` 范式） |
| 已知失败模式 6（`@Inject private`） | ✅ 未发现 |
| 已知失败模式 8（propId 断续） | ✅ ORM 模型近期未变更，无新 gap |
