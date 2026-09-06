# i18n 与日志语言合规标准

> **权威地位声明**：本文档是本项目 **i18n 与日志语言判定冲突的唯一权威 owner doc**（ai-check-r3 M0.1 产出，plan `2026-09-06-1451-1`）。本文档 done 后，判定冲突以本文档为准；`docs/backlog/ai-check-r3-roadmap.md §目的` 的合规基线裁定表自即日起**冻结为历史快照，不再更新**（roadmap 规则 5 同此声明）。
>
> **探针计数不作准绳**：LOG 行数、异常参数数、CJK 行数等探针计数是口径敏感快照，本文档不内联复制任何计数作为准绳（防 `docs/lessons/13-requirement-baseline-staleness.md` 基线陈旧被消费）。权威口径以 M0.2 检测脚本 + M0.3 基线冻结为准，指针见 §探针口径指针；探针快照仅作历史参考存于 roadmap §当前基线。

## 目的

将 i18n 与日志语言的判定准绳、白名单登记格式、修复模式对照表落为稳定技术标准，使后续审计（ai-check-r3 M1 五维矩阵 DIM-I 维）与修复（MI.x 清剿批）有唯一判定依据，消除 finding 争议与标准漂移。

本文档只定义**判定标准与修复模式**，不承载基线快照（`docs/audits/cjk-baseline.md`，M0.2/M0.3 产物）、不承载检测脚本（`tools/check-hardcoded-cjk.mjs`，M0.2 产物）、不登记具体 finding。

## 判定准绳表

共 8 类裁定。每条含判定规则与依据指针；依据以「用户裁定（2026-08-31 需求原文，见 roadmap 头部）」或平台/项目 owner doc 锚点标注。

| # | 类别 | 判定 | 判定规则 | 依据 |
|---|------|------|----------|------|
| 1 | `ErrorCode.define` 中文描述 | **合规** | zh-CN 为源语言；错误描述内联中文，英文翻译后期经 i18n yaml 补充（本轮不建 827 条 en 镜像，用户裁定） | 用户裁定 + `../nop-entropy/docs-for-ai/02-core-guides/error-handling.md:119` + `docs/design/domain-design-guidelines.md` §七 |
| 2 | `*Errors.java` 接口 `@Locale("zh-CN")` | **必须补齐** | 每个域 `Erp*Errors` 接口必须标注 `@Locale("zh-CN")`，显式声明源语言；纯加性变更，不改错误码定义本身 | 用户裁定 + 平台注解 `io.nop.api.core.annotations.core.Locale`（实仓核实于 nop-api-core）；平台先例 nop-entropy `BatchErrors` / `NopAuthErrors` |
| 3 | LOG 消息中文 | **违规** | 日志面向开发者/运维，统一英文；日志不走 i18n 机制 | 用户裁定 |
| 4 | 异常路径中文散文参数 | **违规** | `.param(ARG_EXPECTED_STATUS, "非已作废")` 这类中文散文参数禁止；应传状态码/枚举名/字典值本身（错误消息语义仍由 ErrorCode 中文模板承载） | 用户裁定 |
| 5 | 运行时字符串中文（`return "中文"` / 中文 String 常量 / setter 默认业务数据 / 拼接型） | **违规，白名单制** | 运行时面不得硬编码中文；逐簇裁决（见 §修复模式对照表 CAT-3），豁免唯一通道 = 白名单显式登记（见 §白名单登记格式）；其中 `@Description("中文")` 按 E3 计划豁免登记 | 用户裁定 |
| 6 | Java 注释中文 | **豁免** | 注释不属运行时面，不计违规、不检测 | M0.1 裁定（用户需求只约束运行时面） |
| 7 | `*.page.yaml` / `*.flux.yaml` 用户可见文案中文 | **违规** | 页面用户可见文案必须有英文承载：手写页补 `i18nEn` 属性；codegen 产物改模型源 `i18n-en` 属性（禁改生成物） | F15 同型裁定（view.xml 已全覆盖，page/flux yaml 是剩余面）+ `docs/architecture/view-and-page-strategy.md` §国际化策略 |
| 8 | `_` 前缀生成 i18n yaml | **禁手改** | codegen 产物（如 `_vfs/i18n/en/_erp-*.i18n.yaml`，由 `module-*/erp-*-meta/postcompile/gen-i18n.xgen` 生成）禁手改；en 覆盖写非下划线手写文件并以 `x:extends` 继承基文件 | `../nop-entropy/docs-for-ai/02-core-guides/error-handling.md:332-351` + `docs/lessons/06-codegen-product-edit-overwrite.md` |

**判定优先级**：两表冲突时以本表为准（见头部权威声明）。规则冲突指针：真相源优先级见 `docs/context/source-of-truth-and-precedence.md`。

## 白名单登记格式

白名单是**运行时字符串中文（判定准绳表 #5，检测分级 CAT-3）的唯一豁免通道**。未登记即违规，不接受任何无登记豁免（对齐 roadmap 横切关注点 2 的「证伪/豁免路径」纪律）。

**登记落点**：`docs/audits/cjk-baseline.md`（M0.2/M0.3 产物，本文档只定义格式，不预建该文件）。检测脚本以该文件为白名单数据源做文件级豁免。

**登记格式**（逐条一行/一格，四要素缺一不可）：

| 字段 | 说明 | 示例 |
|------|------|------|
| 文件路径 | 仓库相对路径，精确到文件（必要时含类/方法） | `module-fin/.../AcctDocProvider.java#buildSummaryName` |
| 理由 | 为何该中文必须保留（业务语义/字典承载/历史契约） | `@Description("中文")` 供平台 meta 消费，按 E3 计划豁免 |
| owner doc 指针 | 支撑该豁免的权威文档 | `docs/architecture/i18n-compliance.md` 判定准绳表 #5 |
| 裁决来源 | 谁、何时、依据何计划/审计裁决 | plan `2026-09-06-1451-2` M0.2 / MI.6 逐簇裁决 |

**登记纪律**：

- 白名单只增不删（删除 = 修复后从登记中移除，须在批注账记录去向）。
- 调高（新增豁免）须附裁决来源；无裁决来源的登记无效。
- MI.6 清剿批的白名单登记全部落 `docs/audits/cjk-baseline.md` 批注账，不落本文档。

## 修复模式对照表

四类违规的修复模式。检测分级（CAT-N）与 M0.2 脚本 `tools/check-hardcoded-cjk.mjs` 的五类分级一致（CAT-5 = 注释，豁免不计）。

| 分级 | 违规面 | 修复模式 | 关键边界 |
|------|--------|----------|----------|
| CAT-1 | LOG 语句含中文 | **LOG 英文化**：消息模板改英文，保留 `{}` 占位参数与参数值不变 | 仅改消息载体，禁止借机改业务行为（`docs/lessons/09-posting-exception-swallow-suspension.md`：不得改吞异常行为）；不走 i18n |
| CAT-2 | 异常路径携带中文散文参数 | **异常参数传码**：`.param(...)` 值改传状态码/枚举名/字典值本身（如 `"非已作废"` → `status` / `status.name()` / 字典 key）；否定语义（「非已作废/非终态」型）传 `"!" + 被禁状态码`（如 `"!CANCELLED"`，码集合传 `"!" + String.join(" / ", codes)`，见 plan `2026-09-07-0043-2` Phase 1 Decision） | 错误消息语义不变——中文仍由 `ErrorCode.define` 中文模板承载（判定 #1 合规）；错误码 key 与 ARG 常量名不变 |
| CAT-3 | 运行时字符串中文 | **逐簇裁决**，三选一：(a) 改字典 key / 枚举名（业务数据回归字典真相）；(b) 改英文；(c) 白名单登记（§白名单登记格式） | `@Description("中文")` 按 E3 计划豁免登记；业务数据默认值逐簇裁决，不做一刀切；登记落 `docs/audits/cjk-baseline.md` |
| CAT-4 | `*.page.yaml` / `*.flux.yaml` 用户可见文案中文无英文承载 | **补 `i18nEn` 或模型源 `i18n-en`**：手写页直接在 yaml 节点补 `i18nEn: "..."` 属性；codegen 产物改模型源（view.xml / xmeta 补 `i18n-en` 属性）后重新生成 | codegen 产物禁改生成物（`docs/lessons/06-codegen-product-edit-overwrite.md`）；codegen / 手写页的源头链判定以 M0.4 策略矩阵（`m0-4-page-yaml-source-map.md`）为准；英文译法统一取 `docs/design/i18n-glossary.md`（414 token 冻结基准，新词先扩表再使用） |

## 探针口径指针

- 检测脚本：`tools/check-hardcoded-cjk.mjs`（M0.2 交付；五类分级 + `--baseline` 快照 + `--strict` 门控）。
- 基线快照与白名单登记：`docs/audits/cjk-baseline.md`（M0.2/M0.3 产物；单向收紧，调高须独立计划裁决）。
- 历史探针快照（2026-08-31，口径敏感，仅作对照）：`docs/backlog/ai-check-r3-roadmap.md` §当前基线。

## 相关文档

- `../nop-entropy/docs-for-ai/02-core-guides/error-handling.md` — 平台 ErrorCode/i18n 机制（l.119 中文描述合规依据；l.332-351 `_` 前缀生成链与 `x:extends` 覆盖机制）
- `docs/design/domain-design-guidelines.md` §七 — ErrorCode 命名空间与使用规范
- `docs/architecture/view-and-page-strategy.md` §国际化策略 — 界面标签 `i18n-en` 模型源约定与 en 手写 Delta 落点
- `docs/errors/README.md` — 各域 `Erp*Errors.java` 真相源索引（MI.1 补 `@Locale` 的操作面）
- `docs/design/i18n-glossary.md` — 英译术语冻结基准（414 token）
- `docs/backlog/ai-check-r3-roadmap.md` — 本标准的来源与消费方（M0.1 工作项；判定冲突以本文档为唯一权威）
