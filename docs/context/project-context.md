# 项目上下文

## 目的

保持此文件作为 AI 代理在执行有用工作之前所需的最短低变化上下文。

本文件不是执行仪表板。不要在此处镜像活动计划、当前阻塞项、当前待办事项行或日期审计/日志状态。必要时从其拥有文件中发现这些信息。

## 项目标识

- 项目名称：nop-app-erp
- 产品类型：基于 Nop Platform 构建的企业资源规划（ERP）应用骨架
- 主要用户：ERP 系统操作员/管理员以及从现实业务领域应用学习 Nop Platform 的开发人员
- 文档新鲜度：`fresh`（全部业务域 ORM 模型已设计完成；设计文档经多次审计验证）

**新鲜度控制：**

- 如果新鲜度为 `stale` 或 `unknown`，代理可以研究、审计和起草对齐文档，但在重新建立基线或人工确认预期行为之前，不得实现产品行为。
- 如果新鲜度为 `partially stale`，代理只能实现其需求、owner doc、codebase-map 路由和触及代码区域已明确验证为新鲜的切片；否则将切片视为 `plan-first` 或 `research-only`。
- AI 不得在无人确认或无人批准的 owner-doc 证据的情况下将陈旧文档标记为新鲜。

## 当前技术基线

- 前端栈：nop-chaos-flux 渲染（flux-only 强制，页面模型仍为 `.view.xml`；AMIS 已非渲染目标，见 `docs/architecture/view-and-page-strategy.md`）— 代码生成后可用
- 后端栈：Java 17+、Quarkus、Nop Platform（nop-entropy）— 代码生成后可用
- 数据库/模型源：`module-<domain>/model/app-erp-<domain>.orm.xml` 中的 XML 模型

## 当前项目阶段

`nop-app-erp` 处于 **业务逻辑深化与运营成熟度收尾阶段**。全部业务域 ORM 模型 + 跨域 sys 通知派发子系统 + 项目级共享模块（`module-common-test` / `module-common-service`）已就绪，所有域 codegen 骨架已生成，`app-erp-all` 聚合 app 已构建通过。

- 后续模型变更用 `mvn clean install` 增量重新生成，**不要**重跑 `nop-cli gen`
- **工程命名映射**：物理目录 `module-<domain>/` ↔ 逻辑工程名 `app-erp-<domain>` ↔ appName `erp-<简称>` ↔ moduleId `erp/<简称>` 的完整映射见 `docs/architecture/domain-module-split-analysis.md §2.0`
- 当前重点：需求-实现符合性审计修复批次（requirement-compliance，见 `docs/backlog/requirement-compliance-roadmap.md`）+ 各域细化端到端验证
- 已落地能力见 `AGENTS.md §当前项目阶段`

## 验证命令

> **当前状态**：最新已知良好基线见 `docs/testing/known-good-baselines.md`；`mvn clean install -DskipTests` 为标准构建验证入口。

| 目的 | 命令 |
|------|------|
| 构建全项目 | `mvn clean install -DskipTests` |
| 本地运行应用 | `java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar` |
| 类型检查/编译检查 | `mvn compile -DskipTests` |
| 单元测试 | `mvn test` |
| 首次生成单域模块骨架（仅首次） | `nop-cli gen module-<domain>/model/app-erp-<domain>.orm.xml -t=/nop/templates/orm` |
| 增量重新生成（模型变更后） | `mvn clean install -DskipTests`（触发 gen-orm.xgen 增量链） |
| XML well-formed 校验 | `xmllint --noout module-<domain>/model/app-erp-<domain>.orm.xml` |
| 合规性检查 | `bash docs/audits/nop-compliance-checker.sh` |

## 当前使用的可选层

仅标记此项目实际维护的可选层。

- [x] `docs/discussions/`（维护：需求澄清对话记录）
- [x] `docs/audits/`（维护：综合审计、合规审计、专业审计记录）
- [x] `docs/testing/`（维护：手动测试记录与已知良好基线）
- [x] `docs/skills/`（维护：一批可复用审查/审计方法技能 + README 项目定制化层，清单见 `docs/skills/README.md`）
- [x] `docs/analysis/`（维护：调研笔记、权衡分析、自动化路线图）
- [x] `docs/retrospectives/`（维护：实现差距分析与流程改进回顾）
- [x] `docs/lessons/`（维护：从 Bug/审计/回顾提取的持久工程经验）

## AI 阻塞条件

AI 必须在继续之前停止并等待人工输入，当：

- 任何更改触及数据删除、会计/财务或其他 ERP 保护区域，且没有描述预期行为的 owner doc
- 任何更改在没有明确人工批准的情况下修改 XML 模型（`model/*.orm.xml`、`model/*.api.xml`）— 这些驱动代码生成
- 没有 owner doc 描述更改的预期行为 — 不要在真空中实现

这些是除 `AGENTS.md`、`docs/context/ai-autonomy-policy.md`、真相源冲突规则以及所需计划/结束审计规则之外的项目特定硬停止。

对于不影响用户可见行为、契约、保护区域或结束证据的歧义，通过在相关文档中写入假设并根据自主政策继续来解决。明确标记不确定的假设，以便人类稍后可以审查。

## AI 代理注意事项

- **当前进行中的工作**：检查 `docs/backlog/README.md` 和 `docs/plans/` 中的未完成计划。
- AI 自主权默认为 `implement`；它由保护区域（`ai-autonomy-policy.md`）控制。
- AI 可以根据实时仓库证据纠正事实上下文，但在无人确认的情况下不得将陈旧文档标记为新鲜或降级保护区域。
- 构建需要 `nop-entropy` 父 POM 首先在本地 Maven 仓库中可用。
- 跨域端到端循环（如采购→入库→凭证）需先编写计划（`plan-first`）。
- 每个业务功能实现时，AI 自行根据 owner doc 和用例文档拟制对应测试。

## 已知失败模式（速查）

> 本段是简洁内联摘要，列出审计-修复任务中反复出现的失败模式，供代理在新工作开始时自检规避。详细 case study 与可复用审计方法见 `docs/lessons/` 与 `docs/skills/`。

- **Compliance 基线漂移**：功能计划新增生产代码 daoFor/import 后，结束审计未核实 `nop-compliance-checker.sh` 是否触发 actual > baseline，导致 CI red。规避：任何生产代码变更的结束审计须复跑 checker；若漂移则在闭包前开独立基线裁决计划（Fix 或 baseline-raise 带 per-site 证据），或在 closure gates 显式登记「基线漂移已知，归 successor」。权威基线见 `docs/audits/compliance-baseline.md` §BASELINE 机器可读块。
- **closure-pending 计划缺独立结束审计**：执行者自我声明完成但未由独立子代理（新会话）跑结束审计，留下 hollow 闭包。规避：每个计划完成前必须经独立结束审计（Closure Gates 硬门控），执行者不得自我审计。
- **`@Inject private` 违反 Nop IoC**：在 Nop IoC 容器中给 `@Inject` 字段加 `private`（checker R5），破坏依赖注入。规避：`@Inject` 字段必须包级可见或以上（AGENTS.md 平台规则 + `nop-backend-dev` skill 反模式表）。
- **业财过账吞异常致 posted 悬挂**：过账编排中用 `tryPost` 吞掉异常而未将单据回退至非已过账状态，导致 posted 标志与实际过账结果不一致（悬挂）。规避：过账失败必须显式回退单据状态并记录 `PostingException`，不得静默吞异常（owner doc `docs/architecture/processor-extension-pattern.md`）。

