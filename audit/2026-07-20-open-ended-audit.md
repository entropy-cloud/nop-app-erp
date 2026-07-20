# 开放式审计报告

**日期**: 2026-07-20  
**审计类型**: open-ended-audit  
**审计对象**: nop-app-erp 项目文档与实现代码  
**审计依据**: `docs/skills/open-ended-audit-prompt.md` + `docs/skills/README.md §项目定制化层（nop-app-erp）`

---

## 裁决

**passes open-ended audit**

---

## 发现（按严重性排序）

### P1 — 代码生成文件编辑模式已记录但尚未系统性排查

**发现**: notify inbox page 实施中，AI 连续两轮编辑 `_erp-notify.action-auth.xml`（带 `_` 前缀的 codegen 生成文件），每次 `mvn install` 后即被还原。该模式在 07-20 第三轮才被正确理解和修复（改用保留层 `erp-notify.action-auth.xml` + `x:extends` 继承）。

**风险**: 同一根因可能在项目其他 `_` 前缀文件中重复发生。`docs/audits/nop-compliance-checker.sh` 应包含对 `_` 前缀文件手改的检测。

**建议**: 
- 执行一次全仓库 grep 扫描 `_vfs/**/_*.{xml,yaml}` 的 git 修改历史，确认是否有其他生成文件被手改过。
- 在 `nop-platform-conformance-audit-prompt.md` 维度 13（已新增）基础上，考虑在 CI 或 pre-commit hook 中添加生成文件写保护。

---

### P2 — `docs/logs/index.md` 未反映实际日志文件

**发现**: `docs/logs/index.md` 仅列到 06-25 附近的日志引用，而实际日志文件已增长到 07-20（共 26 个日常文件 + index + 写作指南 = 28 文件）。索引陈旧约 25 天。

**风险**: 低。日志文件按 `YYYY/MM-DD.md` 约定命名，即使 index 未更新也可按模式查找。但跨会话 AI 若仅读 index 可能误以为日志仅止于 06-25，遗漏最近 25 天的执行历史。

**建议**: 更新 `docs/logs/index.md` 使其自动指向最新日志（或直接移除静态文件列表改为动态约定描述）。

---

### P2 — 273 个计划文件积累，归档机制缺位

**发现**: `docs/plans/` 下累计 271 个计划文件（含 265 个带时间戳的 plan + 3 元计划 + 1 指南 + 1 README + 1 README 根）。绝大多数计划状态为 `completed`。

**风险**: 低。计划命名时间戳可区分，但活跃/已完成/已废弃混杂。查找当前 active plan 需 grep `Status=active`（通常只有少数几个），对 AI 首次加载造成噪音。

**建议**: 
- 考虑将 `completed` 计划归档到 `docs/archived-plans/` 或 `docs/archive/plans/`。
- 仅保留 `active` / `draft` / `superseded-with-current` 计划在 `docs/plans/` 根目录。
- 这与 `docs/archive/` 的使用规则一致（未经人工批准不移动文件到 archive，但计划文件为 AI 生成产物，AI 自主归档已完成 plan 合理）。

---

### P2 — `PAGE_ERROR_COUNT=213` 基线未被修复

**发现**: F7 变更日志记录 `ErpAllWebPagesCollectTest` 当前报 213 errors（JDK 26 / antlr `ParseCancellationException cannot be cast to RecognitionException`），覆盖全 18 域全部 page。该基线在 F7 批次前后一致（213→213），但使得 PAGE_ERROR_COUNT=0 的黄金标准无法达成。

**风险**: 中。任何新增 page.xml 语法错误会被 213 掩盖。依赖「diff 不变」的间接证明增加了审查成本。

**建议**: 将该已知错误追认为项目已知问题，写入 `docs/bugs/` 或 `docs/testing/known-good-baselines.md` 的已知失败段，并分配修复计划。或降级该测试为 `@Disabled`（带注释）直到 antlr 版本兼容性问题解决。

---

### P3 — Playwright webServer.port 默认值不匹配

**发现**: 07-20 日志中至少 4 处独立 plan（F6 field formatting、F4 P2 inventory child-table、notify inbox、F8/F2 search filter）遭遇同一问题：`playwright.config.ts` 默认 `webServer.port=8080` 但 `application.yaml` 实际使用 `port=8011`。每次执行需 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1` 显式覆盖。

**风险**: 低。已有标准 workaround，但增加了执行心智负担和新人的认知成本。

**建议**: 将 `playwright.config.ts` 的 `webServer.port` 改为 `8011` 或从 `application.yaml` 读取。一次性修复即可消除所有 plan 中的重复踩坑。

---

### P3 — `docs/input/` 和 `docs/discussions/` 规模与项目阶段不匹配

**发现**: 
- `docs/input/` 仅有 2 个指南文件，零原始输入。项目早期可能有原始输入但未被留存。
- `docs/discussions/` 有 6 文件，但 4 个集中在 07-19（ppt 和回环工程讨论），仅 1 个（06-29）是早期需求讨论。

**风险**: 低。项目当前阶段需求已稳定，owner docs 已覆盖。但对可追溯性有偏好。

**建议**: 无。项目已过需求澄清阶段，当前 focus 是前端 UI 完善和端到端验证，input/discussions 为可选层。

---

### P3 — 跨仓库技能碎片化

**发现**: 技能文件分布在两个位置：
1. `docs/skills/`（19 个审计/审查方法技能）- 仓库内，跨工具可移植
2. `.opencode/skills/`（nop-backend-dev/nop-frontend-dev/nop-testing 等操作技能）- opencode 工具本地加载

07-20 META 审计更新了两处文件，但 `docs/skills/README.md §与工具原生技能的关系` 明确说明这是设计选择而非缺陷。

**风险**: 低。设计意图清晰，互补而非竞争。

**建议**: 保持。当前分配合理（审计方法在 docs，操作技能在 opencode config）。

---

### P4 — buildCode 溢出模式已修复但同类模式余留 6 处

**发现**: `docs/bugs/2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md` 记录了 `ErpFinArApItemGenerator.buildCode` 的 code precision 溢出。07-18 `0347-1` 修复了 `ErpHrEmployeeBizModel.buildSuccessorCode` 同型问题。审计注释明确指出尚有 6 处 `buildXxxCode`-style 拼接方法未审计（#3~#8）。

**风险**: 低。属于已知残留风险，watch-only 状态。

**建议**: 在 `docs/lessons/` 或 `docs/bugs/` 中登记这些 6 个位置的跟踪清单，待各域深化时触发修复。

---

## 从未写下的假设

1. **项目始终运行在 JDK 26（或最新 JDK）下** — 这导致了 antlr 兼容性问题（PAGE_ERROR_COUNT=213）。未在任何文档中显式声明目标 JDK 版本。
2. **Playwright E2E 测试假设 API 服务已就绪** — `SKIP_WEBSERVER=1` 模式假设外部 JVM 进程已手动启动。未声明是否应 CI/CD 自动管理服务生命周期。
3. **154 模块全绿构建是 CI 门禁** — 每日日志显示全 reactor 构建通过，但未在仓库中见到 CI 配置文件（GitHub Actions / Jenkins）。构建成功依赖本地执行纪律而非自动门禁。

## 系统性偏差

1. **审计证据偏重正向路径** — 所有业务动作 E2E 覆盖正向路径详尽，但非法迁移守卫通常仅 1-2 个代表性的。未系统性遍历所有可能非法迁移组合。
2. **领域覆盖不均** — finance 域测试最详尽（206+ JUnit + 多 spec），而 aps/b2b/drp 等扩展域的测试密度明显更低。这在路线图中被接受（核心域优先），但可能隐藏更多缺陷。

## 重复失败模式

1. **`_` 前缀生成文件手改** — notify inbox 2 轮失败 → 已提升为 `nop-platform-conformance-audit-prompt.md` 维度 13 ✅
2. **playwright port 默认值** — 至少 4 次重复 → 仍未根因修复（playwright.config.ts 或 application.yaml 其一应为权威源）
3. **GraphQL `$var` 模板损坏** — 1249-2 发现并修复，1728-1 重构 → 已记录在 `docs/bugs/` ✅
