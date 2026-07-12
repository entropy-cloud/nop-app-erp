# 2026-07-12-1321-3-report-date-param-download-fix 日期参数报表下载/渲染生产修复（AMIS 时间戳/空串 → 后端宽容解析）

> Plan Status: completed
> Last Reviewed: 2026-07-13
> Mission: erp
> Work Item: 12 日期参数报表在真实用户面下载/渲染失效的生产修复（AMIS `input-date` 序列化裸 Unix 时间戳/空串 → 后端 `asDate` strict-parse `DateTimeParseException` → 无文件下载/无渲染）
> Source: 已确认实时缺陷（Fix，规则 13 不可降级）。`docs/plans/2026-07-12-0413-1-report-download-button-url-fix.md` Phase 1 Decision Record（:91-99）实测裁定：AMIS `input-date` 将日期序列化为裸 Unix 时间戳（秒），未填字段序列化为空字符串 `""`；后端 6 个报表 BizModel 的 `asDate(Map,String)` helper 对两者均 `LocalDate.parse(v.toString())` strict-parse → `DateTimeParseException` → 返回 JSON 错误而非二进制/HTML → AMIS 不触发 download/render。12 报表（crp-load/production-variance/forecast-variance/asset-depreciation/asset-disposal/downtime-summary/maintenance-history/project-cost-summary/timesheet-detail/inspection-summary/ncr-capa-summary/ar-ap-aging）在**生产**用户面经 AMIS 发裸时间戳/空串日期，下载/渲染完全失效。当前仅经测试层 `page.route` 归一化 workaround（`reports/_helper.ts:396-418` + `visual/_helper.ts:148-170`）绕过，非产品变更。0413-1 Deferred「日期参数报表空日期/默认日期下载真实可用时——需后端宽容解析空串/时间戳，或前端 AMIS 日期格式化」（触发条件已满足：AGENTS.md 当前项目阶段重点含「看板运行时视觉/浏览器回归」，12 报表用户面失效是已确认活体 UX 缺陷）。
> Related: `2026-07-12-0413-1`（下载按钮 URL 修复，Phase 1 Decision Record 首次隔离本缺陷根因 + Deferred successor 记录）、`2026-07-12-0204-1`（报表下载运行时回归层，`/p/` 直调验证后端可达性，测试层 workaround 引入源）、`2026-07-09-2330-2`（报表视觉回归全覆盖，Follow-up :161 引用本缺陷）、`2026-07-09-1728-1`（`$var` page.yaml 系统性 bug 修复先例，同类「page.yaml 从未运行时测试过」根源）、`../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md`（`/p/` 端点二进制下载专路）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **6 报表 BizModel 的 `asDate` helper 字节级相同且为唯一抛错点**：`asDate(Map<String,Object> data, String k)` 在 6 个域报表 BizModel 中逐字相同：
  - `ErpFinReportBizModel.java:204-209`
  - `ErpMfgReportBizModel.java:195-200`
  - `ErpAstReportBizModel.java:186-191`
  - `ErpMntReportBizModel.java:179-184`
  - `ErpPrjReportBizModel.java:185-190`
  - `ErpQaReportBizModel.java:179-184`
  
  实现：`Object v = data.get(k); if (v == null) return null; return v instanceof LocalDate ? (LocalDate) v : LocalDate.parse(v.toString());`——`v.toString()` 对空串 `""` 抛 `DateTimeParseException`（`LocalDate.parse("")`），对 10 位 Unix 时间戳 `"1783621109"` 抛 `DateTimeParseException`（非 ISO 格式）。**全仓库无 `DateTimeParseException` catch**（grep 确认），异常裸传播为 500 错误。
- **6 报表 BizModel 的 `asLong` helper 字节级相同且有空串同类漏洞（B1 BLOCKER 来源）**：`asLong(Map<String,Object> data, String k)` 在同 6 个 BizModel 中逐字相同，实现 `Object v = data.get(k); return v == null ? null : Long.valueOf(v.toString());`——对空串 `""` 抛 `NumberFormatException`（`Long.valueOf("")`）。9/12 日期参数报表在 `prepareDataset` 中调 `asLong` 取 `input-number` 字段（workcenterId/workOrderId/materialId/equipmentId/projectId/categoryId）。**测试层 workaround 归一化 `data` map 中所有 `""→null`（非仅日期键）**，故当前 `asLong` 空串漏洞被 workaround 掩盖——移除 workaround 后 `asLong` 同样需宽容解析。
- **日期参数经 `Map<String,Object> data` 透传（无 GraphQL coercion）**：`renderHtml` / `download` 均接收 `@Optional @Name("data") Map<String,Object> data`，日期字段作为 map value 透传至 `asDate`。GraphQL 不对 `Map<String,Object>` 内部值做类型 coercion（untyped bag），故无平台级 `LocalDate` coercion 可用——修复须在 `asDate` helper 内。
- **12 报表 page.yaml 的 `input-date` 均无 `valueFormat`/`format` 属性**：经 `rg "valueFormat|inputFormat|format:" --glob "**/pages/report/*.page.yaml"` 确认零匹配。AMIS `input-date` 默认序列化为裸 Unix 时间戳（秒），未填字段为 `""`。12 报表路径：
  1. `module-finance/erp-fin-web/.../pages/report/ar-ap-aging.page.yaml`（asOfDate，`value: "${NOW()}"` 默认=时间戳）
  2. `module-manufacturing/erp-mfg-web/.../pages/report/crp-load-report.page.yaml`（startDate/endDate）
  3. `module-manufacturing/erp-mfg-web/.../pages/report/production-variance-report.page.yaml`（startDate/endDate）
  4. `module-manufacturing/erp-mfg-web/.../pages/report/forecast-variance-report.page.yaml`（periodStart/periodEnd）
  5. `module-assets/erp-ast-web/.../pages/report/asset-depreciation-detail.page.yaml`（日期区间）
  6. `module-assets/erp-ast-web/.../pages/report/asset-disposal-detail.page.yaml`（日期区间）
  7. `module-maintenance/erp-mnt-web/.../pages/report/downtime-summary.page.yaml`（日期区间）
  8. `module-maintenance/erp-mnt-web/.../pages/report/maintenance-history.page.yaml`（日期区间）
  9. `module-projects/erp-prj-web/.../pages/report/project-cost-summary.page.yaml`（日期区间）
  10. `module-projects/erp-prj-web/.../pages/report/timesheet-detail.page.yaml`（日期区间）
  11. `module-quality/erp-qa-web/.../pages/report/inspection-summary.page.yaml`（日期区间）
  12. `module-quality/erp-qa-web/.../pages/report/ncr-capa-summary.page.yaml`（日期区间）
- **测试层 workaround 经 `page.route` 归一化所有空串/时间戳（非产品变更，应移除）**：`tests/e2e/reports/_helper.ts:396-418`（`assertAmisDownloadButton`，拦截 `**/p/*`，归一化 `data` map 中**所有** `""→null` + `/^\d{10}$/→ISO`，非仅日期键——注释 :388-395 明示「AMIS sends empty-string for unfilled optional form fields」泛指所有字段）+ `tests/e2e/visual/_helper.ts:148-170`（`assertReportRendered`，拦截 `**/graphql`，同归一化作用于 variables）。两处注释明示「测试层 AMIS 序列化 workaround，非产品变更」。生产修复后应移除以验证真实 AMIS 行为——**移除后 `asDate` + `asLong` 均需宽容解析**（Phase 1 同步修复两者）。
- **0413-1 下载按钮 URL 修复已落地（`/graphql`→`/p/`）**：全 24 报表 page.yaml 下载按钮 `api.url` 已修复为 `/p/{ErpXxxReport__download}`（0413-1 completed）。本计划修复的是日期参数序列化缺陷，与 URL 修复正交——URL 修复后日期参数报表仍因 `DateTimeParseException` 失效。

剩余差距：12 日期参数报表在生产用户面经 AMIS 发裸时间戳/空串日期 → 后端 `asDate` strict-parse 抛 `DateTimeParseException` → 下载/渲染失效。测试层 workaround 绕过而非修复。

## Goals

- 修复 6 报表 BizModel 的 `asDate` helper 使其宽容解析 AMIS 日期序列化：空串 `""` → `null`（按全量）、10 位 Unix 时间戳（秒）→ `LocalDate`、13 位时间戳（毫秒）→ `LocalDate`、ISO `YYYY-MM-DD` → `LocalDate`（既有路径不变）。
- 修复同 6 BizModel 的 `asLong` helper 空串漏洞：空串 `""` → `null`（按未填），与 `asDate` 同步（移除测试层 workaround 后 `asLong` 空串 `NumberFormatException` 暴露）。
- 为 12 报表 page.yaml 的 `input-date` 追加 `valueFormat: "YYYY-MM-DD"`，使前端序列化为 ISO 字符串（AMIS hygiene，减少对后端宽容解析的依赖）。
- 移除测试层日期归一化 workaround（`reports/_helper.ts` + `visual/_helper.ts`），使测试验证真实 AMIS 日期序列化行为（非 workaround 行为）。
- 验证 12 日期参数报表在真实 AMIS 日期序列化下下载（XLSX/PDF）+ 渲染（HTML）全绿。

## Non-Goals

- **不**改报表模板（`.xpt.xml`/`.xpt.xlsx`）或数据集聚合逻辑——仅修 `asDate` 日期解析 + AMIS `valueFormat`。
- **不**做下载产物字节级基线 diff（0413-1 已诚实 re-defer 为 optimization candidate）。
- **不**改非日期参数报表（12 报表以外的 12 报表无 `input-date`，不受影响）。
- **不**改 renderHtml service reload 范式（1728-1 已修复）或下载按钮 URL（0413-1 已修复）。
- **不**提取共享 `ReportDateHelper` 工具类——6 域 BizModel 分属不同 Maven 模块，共享工具类需引入跨模块依赖，归 optimization candidate（触发条件：第 7 个报表 BizModel 引入 `asDate` 时提取）。

## Task Route

- Type: `bug investigation`（已确认活体 UX 缺陷修复，后端 Java + 前端 page.yaml 生产代码变更，零 ORM/契约/模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（报表套件运行手册 + 下载功能已知限制段 + 日期归一化 workaround 段）、`docs/design/dashboards.md` §实现约定（报表渲染容器 + AMIS 取数范式）、`../nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md`（`/p/` 端点 + `Map<String,Object>` data 透传语义）
- Skill Selection Basis: `nop-backend-dev`（Java `asDate` helper 修复 + `LocalDate` 解析 + `Instant`/`ZoneOffset` 时间戳转换）；`nop-frontend-dev`（AMIS `input-date` `valueFormat` 属性 + page.yaml 定制）；`nop-testing`（移除测试层 workaround + Playwright 真实 AMIS 日期序列化回归验证）。既有 0413-1/0204-1/2330-2 报表测试范式已验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 依赖既有 webServer 启动参数（e2e-runbook.md 方式 A），无新增 JVM 属性
- 依赖 `app-erp-all/target/quarkus-app/quarkus-run.jar` 预构建（既有前置）
- 无 ORM/契约变更，故无 ask-first 保护区域门控

## Execution Plan

### Phase 1 - 后端 `asDate` + `asLong` 宽容解析修复

Status: completed
Targets: `module-finance/erp-fin-service/.../report/ErpFinReportBizModel.java`、`module-manufacturing/erp-mfg-service/.../report/ErpMfgReportBizModel.java`、`module-assets/erp-ast-service/.../report/ErpAstReportBizModel.java`、`module-maintenance/erp-mnt-service/.../report/ErpMntReportBizModel.java`、`module-projects/erp-prj-service/.../report/ErpPrjReportBizModel.java`、`module-quality/erp-qa-service/.../report/ErpQaReportBizModel.java`（6 文件 `asDate` + `asLong` helper）
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: 无

- [x] `Fix`：重写 6 个 BizModel 的 `asDate(Map<String,Object> data, String k)` helper——空串 `""` → `return null`；10 位数字串（`s.matches("\\d{10}")`）→ `LocalDate.ofInstant(Instant.ofEpochSecond(Long.parseLong(s)), ZoneOffset.UTC)`；13 位数字串（`s.matches("\\d{13}")`）→ `LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneOffset.UTC)`；其余 → `LocalDate.parse(s)`（既有 ISO 路径不变）。`v instanceof LocalDate` 短路保留。Map value 为 `Number` 类型时 `v.toString()` 产出数字串，正则匹配同路径处理。6 文件逐字相同修复（Decision：fix-in-place 不提取共享工具类，见 Non-Goals 理由）。
      - Skill: `nop-backend-dev`
- [x] `Fix`：重写同 6 个 BizModel 的 `asLong(Map<String,Object> data, String k)` helper——空串 `""`（`s.trim().isEmpty()`）→ `return null`；其余 → `Long.valueOf(s)`（既有路径不变）。与 `asDate` 同步修复，解除移除测试层 workaround 后 `asLong` 空串 `NumberFormatException` 暴露。
      - Skill: `nop-backend-dev`
- [x] `Proof`：6 模块各自类型检查通过（`mvn compile -pl module-{fin|mfg|ast|mnt|prj|qa}/erp-{xx}-service -am`，本地化验证解除 Phase 2 阻塞）。
      - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 6 报表 BizModel `asDate` + `asLong` 宽容解析修复，解除 12 日期参数报表的后端 strict-parse 阻塞 + 移除 workaround 后 `asLong` 空串暴露。

- [x] 6 BizModel `asDate` 对空串/10 位时间戳/13 位时间戳/ISO 均不抛 `DateTimeParseException`（空串→null，时间戳→LocalDate，ISO→LocalDate）。
- [x] 6 BizModel `asLong` 对空串不抛 `NumberFormatException`（空串→null）。
- [x] 6 模块 `mvn compile` 通过（本地化类型检查）。

### Phase 2 - AMIS `valueFormat` + 测试层 workaround 移除 + 真实回归验证

Status: completed
Targets: 12 报表 page.yaml（`input-date` 追加 `valueFormat`）、`tests/e2e/reports/_helper.ts`（移除 `page.route` 日期归一化）、`tests/e2e/visual/_helper.ts`（移除 `page.route` 日期归一化）
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision | Fix | Proof`
- Prereqs: Phase 1（后端宽容解析落地）

- [x] `Explore` | `Decision`：AMIS `input-date` `valueFormat` 对 `${NOW()}` 默认值的影响。**核实项**：`ar-ap-aging.page.yaml:12` `value: "${NOW()}"` 产出裸时间戳——`valueFormat: "YYYY-MM-DD"` 是否将 `${NOW()}` 输出也格式化为 ISO（AMIS `valueFormat` 控制表单值的序列化形式，包括默认值），或仅控制用户选择日期后的序列化。**Decision**：若 `valueFormat` 不格式化 `${NOW()}` 默认值，则 `ar-ap-aging.page.yaml` 需额外将 `value` 改为 `${NOW()}` 经 AMIS 表达式格式化（如 `${NOW() | date:YYYY-MM-DD}`）或后端宽容解析兜底（Phase 1 已覆盖）。记录裁决与残留风险。
      - Skill: `nop-frontend-dev`
      - Execution Note: `valueFormat: "YYYY-MM-DD"` 已追加至全部 12 报表 `input-date`。`ar-ap-aging` `${NOW()}` 默认值的程序性格式化行为不可靠（AMIS 版本相关），但 Phase 1 后端宽容解析已覆盖裸时间戳（10/13 位）路径，故无需改 `value:`——后端兜底为残留风险的唯一防线，已验证全绿。
- [x] `Fix`：12 报表 page.yaml 的所有 `input-date` 追加 `valueFormat: "YYYY-MM-DD"`（使前端序列化为 ISO 字符串，减少对后端宽容解析的依赖）。逐报表核实日期字段名（startDate/endDate/periodStart/periodEnd/asOfDate）。
      - Skill: `nop-frontend-dev`
- [x] `Fix`：移除 `tests/e2e/reports/_helper.ts:396-418` 的 `page.route('**/p/*')` 日期归一化逻辑（`assertAmisDownloadButton` 内），保留其余 helper 逻辑不变。
      - Skill: `nop-testing`
- [x] `Fix`：移除 `tests/e2e/visual/_helper.ts:148-170` 的 `page.route('**/graphql')` 日期归一化逻辑（`assertReportRendered` 内），保留其余 helper 逻辑不变。
      - Skill: `nop-testing`
      - **Scope correction（执行期发现）**：移除的 `page.route` 拦截器归一化 **所有** `""→null`（非仅日期键），故移除后暴露了 2 个**非日期参数报表** BizModel 的同类空串漏洞：`ErpInvReportBizModel.asLong`（`Long.valueOf("")`→`NumberFormatException`，inv-inventory-trace 的 materialId/warehouseId/moveId）+ `ErpMdReportBizModel.asString`（返回 `""` 而非 null 致 `loadMaterials("")`/`buildPartnerListDataset("")` 加 `code=""`/`partnerType=""` 过滤匹配空集→报表数据 token 缺失，md-material-price-list/md-partner-list）。两处与 Phase 1 `asLong`/`asDate` 同类空串漏洞（B1 同源），按 Phase 1 范式同步修复（`s.trim().isEmpty()→null`），使全部报表 BizModel 对 AMIS 空串序列化宽容。此修正与计划 Non-Goal「不改非日期参数报表」的边界修正：Non-Goal 原意是不改报表模板/聚合逻辑，空串解析宽容属同类后端解析修复（非聚合逻辑变更），移除 workaround 的必要前置。
- [x] `Proof`：指定验证命令 `npx playwright test tests/e2e/reports/reports.download.spec.ts tests/e2e/reports/reports.amis-download.spec.ts tests/e2e/visual/reports.visual.spec.ts --workers=1` 全绿——验证 12 日期参数报表在**真实 AMIS 日期序列化**（无 workaround 归一化）下下载（XLSX/PDF）+ 渲染（HTML）全绿；非日期参数报表无回归。
      - Skill: `nop-testing`
      - Proof Result: 82 passed（reports.download 48 + reports.amis-download 10 + reports.visual 24），0 failures，9.9m。12 日期参数报表 + 12 非日期参数报表在真实 AMIS 序列化下全绿。

Exit Criteria:

> Phase 2 交付 12 报表 `input-date` `valueFormat` + 测试层 workaround 移除 + 真实 AMIS 日期序列化回归全绿。

- [x] 12 报表 page.yaml `input-date` 均有 `valueFormat: "YYYY-MM-DD"`（正向检查 `rg "valueFormat" --glob "**/pages/report/*.page.yaml"` 返回 23 匹配 = 1 asOfDate + 11×2 startDate/endDate/periodStart/periodEnd）。
- [x] 测试层日期归一化 workaround 已移除（`reports/_helper.ts` + `visual/_helper.ts` 无 `page.route` 日期归一化段）。
- [x] 12 日期参数报表在真实 AMIS 日期序列化下下载 + 渲染全绿（非日期参数报表无回归——inv/md 空串漏洞经 scope correction 同步修复）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0ab30af0fffe3wyH7SfJ96yO4W) — 全部 baseline 主张经实时仓库核实为真（6 BizModel `asDate` 字节级相同 + 行号精确、`LocalDate.parse(v.toString())` 抛错点、`Map<String,Object>` 透传无 coercion、零 `DateTimeParseException` catch、12 page.yaml 路径 + 无 `valueFormat`、`${NOW()}` 默认值、两处 `page.route` workaround 存在且归一化所有空串非仅日期键）。**1 BLOCKER (B1)**：`asLong` helper 有同类空串漏洞（`Long.valueOf("")`→`NumberFormatException`），9/12 报表调 `asLong` 取 `input-number` 字段，workaround 归一化所有 `""→null` 掩盖此漏洞——移除 workaround 后 `asLong` 暴露，须同步修复。3 non-blocking：S1（Phase 2 item types `Add`→`Decision` 对齐）；S2（`asDate` fix 补 Number 类型 edge case 说明）；S3（regex `\\d{10}`/`\\d{13}` 显式化）。**B1 已修订**：Current Baseline 增 `asLong` 漏洞盘点段 + Phase 1 增 `asLong` Fix 项 + Exit Criteria 增 `asLong` 空串断言 + Closure Gates 更新；S1-S3 已采纳。修订后草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（6 BizModel `asDate` + `asLong` 宽容解析 + 2 BizModel（inv `asLong`/md `asString`）空串宽容（scope correction）+ 12 page.yaml `valueFormat` + 测试层 workaround 移除 + 12 日期参数报表真实回归全绿）
- [x] 相关文档对齐（`e2e-runbook.md`「下载功能」已知限制段移除「日期参数报表 AMIS 序列化缺陷」免责声明（标 ~~已解除~~）+ 两处 helper 编排 prose（:417/:466）移除 `page.route` 归一化描述 + 记录修复）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，6 BizModel Java 变更 `asDate`+`asLong` + inv/md `asLong`/`asString` + 12 page.yaml 变更经 web 模块打包）+ `npx playwright test tests/e2e/reports/reports.download.spec.ts tests/e2e/reports/reports.amis-download.spec.ts tests/e2e/visual/reports.visual.spec.ts --workers=1`（82 passed，0 failures——12 日期参数报表真实 AMIS 序列化下载 + AMIS 下载 + 渲染层全绿，非日期参数报表无回归）
- [x] 无范围内项目降级为 deferred/follow-up（共享 `ReportDateHelper` 工具类提取 + 下载产物字节级基线 diff 为既有 optimization candidate，非本期范围降级）
- [x] 独立草案审查已完成并记录（Draft Review Record，B1 `asLong` 漏洞已修订采纳）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话 ses_0a879ddbeffeBVbKQieu7axbF0）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中（见 Closure Audit Evidence）

## Deferred But Adjudicated

### 共享 `ReportDateHelper` 工具类提取

- Classification: `optimization candidate`
- Why Not Blocking Closure: 6 域 BizModel 分属不同 Maven 模块（erp-fin-service/erp-mfg-service/...），共享工具类需引入跨模块依赖或公共模块。fix-in-place（6 文件逐字相同修复）风险更低，DRY 收益不足以证明跨模块依赖引入。
- Successor Required: `yes`（触发条件：第 7 个报表 BizModel 引入 `asDate` 时提取）

### 下载产物字节级基线 diff

- Classification: `optimization candidate`
- Why Not Blocking Closure: 承接 0413-1 同名 optimization candidate。字节级 diff 对生成器版本/字体子集/时间戳高度敏感，脆弱低信号。
- Successor Required: `yes`（触发条件：产品要求下载产物字节级一致性 + CI 无头渲染稳定性可接受时）

## Closure

Status Note: completed

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0a879ddbeffeBVbKQieu7axbF0（新会话，非执行者）
- Verdict: PASS（8/8 检查通过）
- 核实项：6 Phase-1 BizModel `asDate`（`Instant.ofEpochSecond`+`Instant.ofEpochMilli` × 12 匹配 + `import java.time.Instant`/`ZoneOffset` × 12）+ 6 `asLong` `s.trim().isEmpty()` + inv `asLong`/md `asString` 空串宽容（scope correction）+ 零旧 strict 模式残留 + 23 `valueFormat` 跨 12 page.yaml + 两 helper 零 `page.route` 日期归一化 + e2e-runbook 限制段标 ~~已解除~~。
- 非阻塞观察：(a) crm/hr BizModel 保留旧 `asLong` strict 模式（范围外，82 测试含 crm/hr 报表全绿，潜在风险仅在 crm/hr 报表未来加可选空串参数时）；(b) runbook helper-prose 已修正。
- 验证基线：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ Playwright 82 passed（reports.download 48 + reports.amis-download 10 + reports.visual 24，0 failures）。

Follow-up:

- 共享 `ReportDateHelper` 工具类提取（见 Deferred，触发条件=第 7 个报表 BizModel 引入 `asDate` 时）
- crm/hr BizModel `asLong` strict 模式潜在风险（非本期范围，触发条件=crm/hr 报表加可选空串参数时）
