---
调研日期: 2026-08-26
来源: E2.2 对照确认（plan `docs/plans/2026-08-26-0735-1-e2-survey-cross-confirmation.md`；素材引用 2026-08-12 批次既有报告）
分类: 对照注记（零新调研——仅引用既有报告 + 实时仓库事实核对）
状态: 已完成
---

# 低代码平台边界对照注记（Frappe/Baserow vs Nop 模型驱动）+ 工具链对照

> E2.2 交付物（roadmap §5 Milestone E2，零代码）：把 roadmap §4.1 对 08-12 批次 Frappe/Baserow 借鉴点的预归类落成可引用注记，并补工具链对照（frappe bench vs 本项目 build.sh/nop-cli）。素材全部引用既有报告 `2026-08-12-0000-frappe-framework.md`、`2026-08-12-0000-baserow.md`，不重新调研、不重新裁决 roadmap §4.1 已归类项。

## 0. 落点裁决（Decision）

**裁决：新建本注记文件，而非 `survey-index.md` 增段。**

- `survey-index.md`（`2026-06-22-0000-survey-index.md`）是 38 项目的**目录型索引**（速查导航 + 分类清单 + 能力矩阵），其价值在于稳定；对照结论属综合分析产物，混入会稀释目录职能。
- `docs/analysis/erp-survey/` 已有横向综合文档先例（`2026-06-22-0000-business-design-takeaways.md` 等 4 份列于 index「横向分析文档」节），本注记与其同类，按命名约定 `YYYY-MM-DD-0000-<topic>.md` 独立成文。
- 索引补充：08-12 批次 14 份源报告的索引行已存在于 survey-index §九（2026-08-12 增补），无需重复；本注记经 roadmap §5 E2.2 行与 §6 交付物表引用可达，不另补索引行（plan Phase 2 裁决项）。

## 1. 路径差异对照（Frappe/Baserow vs Nop 模型驱动）

### 1.1 Frappe：DocType 元数据驱动 + 框架级内置领域目录

引用 `2026-08-12-0000-frappe-framework.md` §2.1/§2.2/§2.3：

- **DocType 元数据驱动**（§2.1）：受 Semantic Web 启发，应用围绕元数据定义构建（`frappe/model/`），DocType 声明式定义字段/权限/流程；模型定义自动生成 REST API（`frappe/api/`）、后台管理界面（desk）、无代码 Report Builder。
- **框架级内置领域目录**（§2.2）：`frappe/{automation,contacts,core,custom,desk,email,geo,integrations,printing,workflow,website}` 等框架级能力目录 + 权限/认证/搜索/数据层。
- **路径差异表**（§2.3 原文）：

| 维度 | Frappe | Nop Platform（本项目采用） |
|------|--------|---------------------------|
| 模型真相源 | Python 元类 DocType（运行时元数据表） | XML（orm.xml，编译时唯一真相） |
| 代码生成 | 无（运行时解释 + 自动 API） | nop-cli gen 生成完整模块链 |
| 定制 | 覆盖 DocType/权限/脚本 | Delta 差量合并（x:extends） |
| 前端 | JS desk（生成式后台） | AMIS/flux 页面模型（`.view.xml`，见 `docs/architecture/view-and-page-strategy.md`） |
| 类型安全 | 动态 | 编译期 Java 类型 |

**结论**：哲学同源（模型即真相、语义驱动建模——报告 §3 #1），实现路径不同（Python 运行时元数据 vs Java codegen + Delta 编译期定型）。编译期唯一真相 + 类型安全是 nop 的平台级优势，本项目 19 域 `module-<domain>/model/*.orm.xml` 即该路径的落地（AGENTS.md 标准模块链 model → codegen → dao → meta → service → web → app → api）。对齐 roadmap §4.1「元数据驱动/自动 REST API/Report Builder｜frappe：Nop codegen + nop-report 已具备，不立项」。

### 1.2 Baserow：自研公式语言 / AI 助手 / 应用构建器（无代码终点形态）

引用 `2026-08-12-0000-baserow.md` §2：

- **BaserowFormula**（§2.1）：ANTLR G4 语法定义词法/语法，自研 parser + 求值器，公式跨字段引用、运行时求值。
- **AI 助手 Kuma**（§2.2）：自然语言建库/建工作流/建视图，直接改元数据。
- **应用构建器**（§2.3）：`contrib/builder/` 页面/元素/数据源三元组无代码搭建业务应用，配套 automation/dashboard/database/integrations。
- **边界对照**（§3 #3）：Baserow headless & API-first，无预置 ERP 域——nop 的价值在「模型驱动 + 预置 18+1 域」。

**结论**：Baserow 代表无代码（No-Code）终点形态（自研公式语言、AI 建库、可视化构建器），与本项目低代码模型驱动路径（orm.xml → codegen 增量链）是两种平台边界。各借鉴点维持 roadmap §4.1 既有归类，无未登记矛盾：

| 借鉴点 | roadmap §4.1 归类（引用，不重新裁决） |
|--------|--------------------------------------|
| 公式语言（BaserowFormula ANTLR） | Nop XLang 已覆盖表达式需求；Excel 风格公式字段触发条件=明确需求（E3 之外，不立项） |
| AI 建表/建页面助手（Kuma 形态） | 触发条件驱动：AI 生成元数据/页面需求出现时评估；E1.1 只管业务面 AI 消费/暴露，元数据生成不在本期范围 |
| 应用构建器/无代码平台边界 | 对照 NocoBase 同类结论：无代码平台无预置 ERP 域，本项目价值在模型驱动 + 预置域（本节 1.2 边界对照行） |

## 2. 工具链对照（frappe bench vs build.sh / nop-cli）

frappe bench（`2026-08-12-0000-frappe-framework.md` §1「工具链：bench 命令行工具」+ §3 #4「一键开发/部署」）是 app/site 一体的开发-部署-升级管理工具链。本项目对应路径如下（**以 `docs/context/project-context.md` 验证命令表为权威**）：

| 职责 | frappe bench | 本项目 |
|------|--------------|--------|
| 首次生成应用骨架 | bench new-app | `nop-cli gen module-<domain>/model/app-erp-<domain>.orm.xml -t=/nop/templates/orm`（仅首次；19 域已全部生成） |
| 模型变更后再生成 | —（运行时元数据，无 codegen） | `mvn clean install -DskipTests`（触发 gen-orm.xgen 增量链；**禁止**重跑 nop-cli gen、禁止手改 `_` 前缀生成物） |
| 全量构建 | bench build/install | `build.sh`（= `mvn clean install -DskipTests -Dquarkus.package.type=uber-jar`，仓库根）或 `mvn clean install -DskipTests` |
| 本地运行 | bench start | `java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar` |
| 定制 | 覆盖 DocType/权限/脚本 | Delta 差量合并（x:extends；页面 `.view.xml` bounded-merge/Delta 覆盖） |
| XML 校验 | — | `xmllint --noout module-<domain>/model/*.orm.xml` |

**职责差异与采用路径说明**：

- bench 面向 Python 动态栈的单进程 app/site 生命周期管理（安装依赖、热加载、site 级数据库管理）；本项目工具链面向编译型 Java 多模块（Maven 156 reactor 模块，构建期 codegen + 增量再生成），运行时不可变、编译期类型安全。
- 工具链差异是 §1.1 模型驱动路径差异的自然结果（运行时元数据 → 运维型工具链；编译期真相 → 构建型工具链），**非独立能力缺口**——本项目不引入 bench 形态的 site 管理工具。
- 对应 roadmap §4.1 既有行「工具链对照（bench vs build.sh/nop-cli）｜frappe｜对照确认：E2.2 交付物补『工具链对照』注记」——本节即该交付物。

## 3. 候选工作项登记

无新增候选工作项。全部借鉴点维持 roadmap §4.1 既有归类（「对照确认不立项」或「触发条件驱动」），本注记仅引用结论并补充证据锚点；不因此产生开放结尾。
