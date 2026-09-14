# Flux 渲染合规审计提示（Flux Rendering Conformance Audit）

> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` auto + dual-agent-approval、会计/财务/数据删除、**外部仓库代码 nop-chaos-flux/nop-chaos-next 修改须双独立子 agent 批准**）、验证命令（`mvn clean install -DskipTests`）、命名约定注入上下文。本提示的通用默认值在本仓库不充分。

对页面做 **flux 统一配置层**审计时使用此提示。审计范围是 flux 框架统一负责、业务页面不应重复实现或绕过的内容：页面编写路径、主题与样式契约、组件与设计模式复用、渲染与数据约定、表达式/校验语法、运行时约束、静态门禁。**业务配置层（字段布局/分组/tabs vs wizard/控件选择/列数/按钮分组）不属于本技能**——那些用 `frontend-page-ux-audit-prompt.md`。

本仓库是 **flux-only 渲染**（AMIS 仅作迁移期对照，非目标）。flux 统一配置的权威来源：
- `docs/architecture/flux-page-export-and-validation.md`（静态门禁架构契约）
- `docs/architecture/flux-integration-gotchas.md`（已知集成陷阱登记）
- `docs/design/flux-complex-pages.md`（复杂页 → flux 控件映射总表）
- `../nop-chaos-flux/flux-guide/`（flux 使用指南 18 篇 + design-patterns + examples）
- `docs/testing/e2e-runbook.md`（flux 渲染模式、静态门禁、调试三路径）

**使用场景**：
- 新增/修改页面后，怀疑绕过 flux 统一能力（手写样式/自定义 HTML/错误属性名）
- flux 页面静态门禁（`npm run validate:flux`）出现 error/warning 后的分诊
- 页面级 closure 前的 flux 合规维度（与 `frontend-page-ux-audit-prompt.md` 互补：后者审业务配置）
- flux 升级或跨仓同步后的一致性核查

**不使用场景**：
- 业务配置（布局/分组/控件选择/列数/按钮分组）→ 用 `frontend-page-ux-audit-prompt.md`
- Java 后端行为质量 → 用 `code-quality-audit-prompt.md`
- 平台源码/文档变更同步（nop-entropy 行为变化）→ 不在技能设计范围内，人工处理
- flux 控件本身 bug 的复现与修复 → 到 `nop-chaos-flux` 按其 `flux-guide/13-testing.md` 流程，按保护区域双批准执行；本技能只做页面侧合规审计

**必需输入**：
- 目标页面集合（`*.view.xml` + `*.page.yaml` + `*.flux.yaml`）
- 页面导出产物或静态门禁报告（`app-erp-all/target/flux-pages/` + `_tmp/flux-page-validation-report.json`，若已跑）
- flux-guide 相关篇目（按页面用到的控件类型选读）
- 该页面的生产渲染链路配置（`nop.web.render-mode: flux`、菜单 `component=FLUX`、ORM `ext:web-renderer="flux"`）

**预期输出**：
按 F1-F8 分组的 finding 清单（每项含：维度、控制点 `file:line`、违规类型、判定依据（flux-guide 篇目/架构文档）、修复方向）。最终裁决 `passes flux conformance` 或 `needs revision`。

```text
您是 Nop 平台 flux 渲染引擎合规专家。对目标页面集合做 flux 统一配置层审计。

首先阅读这些文件：
- `AGENTS.md`
- `docs/skills/README.md §项目定制化层`（含外部仓库保护区域）与 `§已知失败模式`
- `docs/architecture/flux-page-export-and-validation.md`（静态门禁架构契约、验证层级 D6）
- `docs/architecture/flux-integration-gotchas.md`（G-NNN 陷阱登记）
- `docs/design/flux-complex-pages.md`（§2.4 三种页面编写路径决策矩阵、§3 控件映射总表）
- `docs/testing/e2e-runbook.md`「渲染模式与 flux 调试三路径」「flux 页面静态门禁」「flux 运行时调试机制」三节
- `../nop-chaos-flux/flux-guide/` 按页面控件类型选读（02 表达式 / 03 api / 04 action / 05 数据流 / 06 校验 / 07 结构节点 / 08 tabs 状态 / 12 i18n / 14 主题 / 15 错误 / 16 monitor / 17 调试 / 18 reactions + design-patterns/）
- `docs/analysis/flux-validation-warnings-analysis.md`（历史 5 类根因：META_FIELDS 缺失 / AMIS→Flux 属性名未转换 / api 属性重复 / 渲染器属性缺失 / validations 语法不兼容）

审计对象 = flux 统一配置层，**不是**业务布局/分组/控件选择（用 frontend-page-ux-audit），**不是** Java 行为质量（用 code-quality-audit）。

## §0 量化基线采集

```
# 页面资产与编写路径分布
find module-* -path '*src/main/resources/_vfs*' -name '*.page.yaml' | wc -l   # 855
find module-* -path '*src/main/resources/_vfs*' -name '*.flux.yaml' | wc -l   # 31
find module-* -path '*src/main/resources/_vfs*' -name '*.view.xml' ! -path '*_gen*' | wc -l
# 内联样式/自定义 HTML 侵入（绕过 flux 主题）
grep -rn "style=\|style: '\|className" <dir> --include="*.page.yaml" --include="*.view.xml"
# 硬编码颜色/魔法数字（应走 CSS 变量 token）
grep -rnE '#[0-9a-fA-F]{3,6}|rgb\(|px' <dir> --include="*.page.yaml" | grep -v 'i18n'
# flux 属性名疑似 AMIS 残留（属性名未转换）
grep -rnE '\b(visibleOn|hiddenOn|disabledOn|className|visible)\b' <dir> --include="*.flux.yaml"
# 数据请求约定（REST @query:/@mutation: vs 裸 /graphql）
grep -rn '/graphql' <dir> --include="*.view.xml" --include="*.page.yaml" --include="*.flux.yaml"
grep -rc '@query:\|@mutation:' <dir> --include="*.page.yaml"
# 运行时约束（列虚拟化阈值 / tabs 懒加载 / wizard valuesPath）
grep -rn 'mountOnEnter' <dir> --include="*.page.yaml"
grep -rn 'valuesPath' <dir> --include="*.page.yaml"
# 静态门禁（在项目根跑，前置：相关模块已 install）
npm run validate:flux   # 报告落 _tmp/flux-page-validation-report.json；退出码 0/1/2
```

## §1 页面编写路径合规（F1）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F1.1 | 路径选择正确性 | 标准 CRUD/tabs/子表/树形 → view.xml + flux-web GenPage；复杂页（看板/甘特/日历/收件箱/wizard）→ page.yaml 整页直写；双栈过渡 → flux.yaml 同名并存 | `flux-complex-pages.md §2.4` 决策矩阵 |
| F1.2 | 不手写平台生成物 | 直接修改 `_gen/`、`_` 前缀文件、`_app.orm.xml` → P1（重新生成被覆盖） | `AGENTS.md` 生成产物规则 |
| F1.3 | complex 槽位使用边界 | 在 `<complex>` 四槽位内嵌 flux 专有控件（gantt/kanban 等）→ P2（槽位仅承载 crud/simple/tabs/wizard/group，无透传点） | `flux-complex-pages.md §2.5` 实施裁决 |
| F1.4 | 渲染链路配置 | 菜单 `component` 非 FLUX、ORM 缺 `ext:web-renderer="flux"`、`nop.web.render-mode` 非 flux → P1 | `e2e-runbook.md` 渲染模式节 |

## §2 静态门禁（F2）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F2.1 | 门禁通过 | `npm run validate:flux` 退出码 1（存在 error）→ P1；有 warning 需逐条裁决 | `flux-page-export-and-validation.md` |
| F2.2 | m2 新鲜度 | 修改 `*.page.yaml`/`*.flux.yaml` 后未 `mvn -pl <module> install -DskipTests` 就跑门禁 → 验证的是陈旧页面（假绿） | `e2e-runbook.md` m2 警示 |
| F2.3 | 导出等价性 | 手写 flux.yaml 孪生页面的「导出 == 生产 getPage」等价性断言失效 → P1 | 同上 |
| F2.4 | 已知 5 类根因复查 | META_FIELDS 缺失 / AMIS→Flux 属性名未转换 / api 属性重复 / 渲染器属性缺失 / validations 语法不兼容 | `flux-validation-warnings-analysis.md` |

## §3 主题与样式契约（F3）

flux 主题是**纯 CSS 契约**：CSS 变量 + Tailwind preset，无 runtime theme API（`flux-guide/14-theming.md`）。

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F3.1 | 内联样式侵入 | `style="..."` / `style: '...'` 手写 → P3（绕过主题；组件级 token 已覆盖表格/对话框全数值） | `flux-guide/14-theming.md` |
| F3.2 | 硬编码颜色/尺寸 | 页面硬编码 hex/rgb/px（非 CSS 变量）→ P3（主题切换失效；用 `--primary`/`--space-*`/`--table-*`/`--dialog-*` token） | 同上 |
| F3.3 | 自定义 className 滥用 | 依赖未定义的 className 或自造类名 → P3（应走 Tailwind preset utility 或 token） | 同上 |
| F3.4 | 主题覆盖位置 | 业务页面内覆盖全局 token（如重定义 `--table-body-font-size`）→ P2（应在宿主壳层统一覆盖） | 同上「宿主覆盖示例」 |
| F3.5 | 对话框尺寸契约 | 自定义固定宽度而非用 `--dialog-size-*` 六档（xs/sm/base/md/lg/xl）→ P3 | 同上 |

## §4 组件与设计模式复用（F4）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F4.1 | 优先用 flux 设计模式 | 手写等价实现而非用 `design-patterns/` 既有模式（button-group / dropdown-button / cascading-select / conditional / combo-input-table / date-fields / form-advanced-fields / picker-transfer 等）→ P3 | `flux-guide/design-patterns/` |
| F4.2 | 手写 HTML 替代组件 | gen-control/page.yaml 内大段手写 `<div>/<span>` 模拟组件（进度条/标签/badge）→ P3（应优先 content-display / progress / badge 类组件） | `design-patterns/content-display.md`、`cards.md` |
| F4.3 | 结构节点选择 | 布局用 `<div>` 堆叠而非 flux 结构节点（layout/group/tabs/collapse）→ P3 | `flux-guide/07-structural-nodes.md` |
| F4.4 | 自定义渲染器 | 引入未注册的 renderer type → P1（编译失败；`validateSchema` 会报未知 renderer） | `flux-guide/13-testing.md` |

## §5 渲染与数据约定（F5）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F5.1 | REST 数据约定 | 页面数据请求走 `/graphql` 而非 `@query:`/`@mutation:` REST 包装 → P2（违反本仓 REST 约定；历史 ErpCsTicket kbSuggestion 已改写） | 项目约定 + ErpCsTicket.view.xml:136 注释 |
| F5.2 | adaptor 作用域 | adaptor 引用 `data.xxx`（应为 `api.data`/`payload`）→ P1（运行时报错） | `nop-frontend-dev` AMIS 运行时约束 |
| F5.3 | 空串序列化 | 表单空字段提交 `""` 未转 null → P2（`@NotEmpty` 被绕过） | 同上 |
| F5.4 | data-source 用法 | 轮询/条件取数未用 data-source 能力（sendOn/interval）而手写定时器 → P3 | `design-patterns/data-source.md` |
| F5.5 | 表达式语法 | 混用 XLang `${}` 编译期插值与 flux 运行期表达式（xview `<c:script>` 为 XLang，flux JSON 的 `${}` 为运行期）→ P2（编译/渲染失败） | `flux-guide/02-expression-syntax.md`、`flux-complex-pages.md` 注释 |
| F5.6 | 表单校验语法 | 使用 AMIS 旧 validations 语法而非 flux 语法 → P2 | `flux-guide/06-form-validation.md`、`flux-validation-warnings-analysis.md` 根因 5 |

## §6 运行时约束（F6）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F6.1 | 列虚拟化认知 | 页面/测试假设 > 13 列的视口外列在 DOM 中 → P2（flux 渲染下列虚拟化行为需按实际实现核对；E2E 需 scroll 配合） | `nop-frontend-dev` AMIS 运行时约束（迁移期）；flux 侧以实际渲染实现为准 |
| F6.2 | tabs 状态与懒加载 | 复杂 tabs 未配 `mountOnEnter`/`valueOwnership` → P3（首屏全量加载/状态丢失） | `flux-guide/08-tabs-state.md`、`flux-complex-pages.md §4.6` |
| F6.3 | wizard 数据分区 | wizard 各步 form 未用 `valuesPath` 分区、`onComplete` 未聚合单请求 → P2（步骤数据互相覆盖/多次提交） | `flux-complex-pages.md §4.5` |
| F6.4 | 表单/表格渲染边界 | 使用 flux 未实现属性（property mapping 表外）→ P2（静默忽略或编译警告） | `docs/analysis/2026-07-11-flux-component-property-mapping.md` |
| F6.5 | 调试机制可用性 | 排查时未用 `window.__FLUX_DEBUG__`/monitor 落盘（E2E fixture 已默认注入）→ 方法建议 | `e2e-runbook.md` flux 运行时调试节、`flux-guide/16-monitor.md` |

## §7 跨仓修改纪律（F7）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F7.1 | 修改范围 | 直接改 `nop-chaos-flux`/`nop-chaos-next`/`nop-entropy` 代码未经双独立子 agent 批准 → P1（保护区域） | `AGENTS.md` + `docs/context/ai-autonomy-policy.md` |
| F7.2 | 先复现后修复 | flux 控件问题未先在 `nop-chaos-flux` 按其 `flux-guide/13-testing.md` 复现/补测试 → 流程违规 | `e2e-runbook.md` flux 调试三路径 |
| F7.3 | 重建链正确 | 需生效 flux 改动未走 `scripts/rebuild-flux-chain.sh`（或分段 `--skip-*`）→ 流程违规 | 同上 |
| F7.4 | gotcha 登记 | 发现新集成陷阱未登记 `docs/architecture/flux-integration-gotchas.md`（G-NNN 模板）→ P3 | 该文件 §模板 |

## §8 i18n 资源渲染（F8）

| # | 检查项 | 判定阈值 | 依据 |
|---|--------|----------|------|
| F8.1 | i18n 资源路径 | flux 页面文案未走 i18n 资源（页面内硬编码）→ P3；运行时面中文违规判定以 `docs/architecture/i18n-compliance.md` 为唯一权威 | `flux-guide/12-i18n.md` + 项目 i18n 合规文档 |
| F8.2 | 生成 i18n 文件 | 手改 `_` 前缀生成 i18n yaml → P1 | `AGENTS.md` 生成产物规则 |

## 反模式自检表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 手写 style/硬编码颜色尺寸 | CSS 变量 token + Tailwind preset |
| 业务页内覆盖全局主题 token | 宿主壳层统一覆盖 |
| 手写 `<div>` 模拟组件 | flux 组件 / design-patterns 既有模式 |
| 页面数据请求走 `/graphql` | `@query:`/`@mutation:` REST 包装 |
| adaptor 里用 `data.xxx` | `api.data` / `payload` |
| XLang `${}` 与 flux 运行期 `${}` 混用 | xview `<c:script>` 只写 XLang；运行期表达式放 page.yaml/flux.yaml |
| wizard 步骤共用一个数据域 | 每步 `valuesPath` 分区 + `onComplete` 单请求聚合 |
| 复杂 tabs 无 mountOnEnter | 懒加载 + `valueOwnership` 状态声明 |
| 修改 flux 仓库代码不走批准 | 先复现测试 + 双独立子 agent 批准 |
| 改完 page.yaml 不 install 直接跑门禁 | 先 `mvn -pl <module> install -DskipTests` 再 `npm run validate:flux` |

## 输出格式

```
- [P1] <F 维度> <页面> <file:line>
  违规：<代码摘录/量化>
  依据：<flux-guide 篇目 / 架构文档 / 项目规则>
  修复：<方向>
```

结尾给按维度统计 + 最终裁决 `passes flux conformance` / `needs revision`。审计产出落 `docs/analysis/`（单页自查可不落盘，仅日志记录）。
```

## 关联

- 业务配置层审计（字段布局/分组/控件选择/列数/按钮分组）→ `docs/skills/frontend-page-ux-audit-prompt.md`
- 静态门禁架构：`docs/architecture/flux-page-export-and-validation.md`
- 集成陷阱登记：`docs/architecture/flux-integration-gotchas.md`
- 复杂页控件映射：`docs/design/flux-complex-pages.md`
- flux 使用指南（兄弟仓）：`../nop-chaos-flux/flux-guide/`（含 design-patterns/ 与 examples/）
- 运行时约束与调试：`docs/testing/e2e-runbook.md`「渲染模式与 flux 调试三路径」节
- i18n 合规唯一权威：`docs/architecture/i18n-compliance.md`