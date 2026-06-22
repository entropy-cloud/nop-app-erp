# 项目上下文

## 目的

保持此文件作为 AI 代理在执行有用工作之前所需的最短低变化上下文。

本文件不是执行仪表板。不要在此处镜像活动计划、当前阻塞项、当前待办事项行或日期审计/日志状态。必要时从其拥有文件中发现这些信息。

## 项目标识

- 项目名称：nop-app-erp
- 产品类型：基于 Nop Platform 构建的企业资源规划（ERP）应用骨架
- 主要用户：ERP 系统操作员/管理员以及从现实业务领域应用学习 Nop Platform 的开发人员
- 文档新鲜度：`fresh`（bootstrap 文档刚刚初始化；ORM 模型尚未设计）

**新鲜度控制：**

- 如果新鲜度为 `stale` 或 `unknown`，代理可以研究、审计和起草对齐文档，但在重新建立基线或人工确认预期行为之前，不得实现产品行为。
- 如果新鲜度为 `partially stale`，代理只能实现其需求、owner doc、codebase-map 路由和触及代码区域已明确验证为新鲜的切片；否则将切片视为 `plan-first` 或 `research-only`。
- AI 不得在无人确认或无人批准的 owner-doc 证据的情况下将陈旧文档标记为新鲜。

## 当前技术基线

- 前端栈：百度 AMIS（`.view.xml` 文件中的 JSON 驱动 UI）— 代码生成后可用
- 后端栈：Java 17+、Quarkus、Nop Platform（nop-entropy 2.0.0-SNAPSHOT）— 代码生成后可用
- 数据库/模型源：`module-<domain>/model/app-erp-<domain>.orm.xml` 中的 XML 模型

## 当前项目阶段

`nop-app-erp` 处于 **bootstrap 阶段**。只有 AGE 文档结构和空的 ORM 模型骨架存在。Maven 多模块项目（api/codegen/dao/service/web/app/delta/meta）和 Java 源将在 ORM 模型设计完成后由 `nop-cli` 生成。请参阅 `AGENTS.md` 中的"当前项目阶段"。

在 ORM 模型和生成模块存在之前：

- 不要假设 Java 模块路径、包名或视图路径已存在
- 设计和讨论工作应集中在 `module-<domain>/model/` 以及 `docs/design/`、`docs/architecture/`、`docs/requirements/`
- 下面的验证命令尚不可执行；仅在首次代码生成通过后才生效

## 验证命令

> **Bootstrap 阶段说明：** 下面的命令针对未来生成的多模块项目。由于 `mvn` 模块和 Java 源不存在，它们**尚不可执行**。仅在首次 `nop-cli` 代码生成通过完成后才生效。在模块存在且针对它们运行这些命令之前，不要报告验证成功。

| 目的 | 命令 |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| 安装依赖 | `mvn dependency:resolve -DskipTests`（需要先构建 nop-entropy 父项目；代码生成后可用） |
| 本地运行应用 | `mvn clean package -DskipTests && java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-app/target/app-erp-app-1.0-SNAPSHOT-runner.jar` |
| 类型检查/编译检查 | `mvn compile -DskipTests` |
| 构建 | `mvn clean package -DskipTests` |
| 静态检查 | `none` |
| 单元测试 | `mvn test` |
| 端到端/集成测试 | `none` |
| 生成项目（一次） | `java -jar nop-cli.jar gen -t=/nop/templates/orm model/app-erp.orm.xlsx`（ORM 设计后从项目根目录运行） |

## 当前使用的可选层

仅标记此项目实际维护的可选层。

- [ ] `docs/discussions/`
- [ ] `docs/audits/`
- [ ] `docs/testing/`
- [ ] `docs/skills/`
- [ ] `docs/analysis/`
- [ ] `docs/retrospectives/`
- [ ] `docs/lessons/`

## AI 阻塞条件

AI 必须在继续之前停止并等待人工输入，当：

- 验证命令都是占位符，无法从项目推断
- 任何更改触及数据删除、会计/财务或其他 ERP 保护区域，且没有现有测试覆盖且没有描述预期行为的 owner doc
- 任何更改在没有明确人工批准的情况下修改 XML 模型（`model/*.orm.xml`、`model/*.api.xml`）— 这些驱动代码生成
- 没有需求或 owner doc 描述更改的预期行为 — 不要在真空中实现（是否存在需求/owner doc 是根据 `docs/requirements/` 和 `docs/design/` 检查的，而非此处的字段）

这些是除 `AGENTS.md`、`docs/context/ai-autonomy-policy.md`、真相源冲突规则以及所需计划/结束审计规则之外的项目特定硬停止。

对于不影响用户可见行为、契约、保护区域或结束证据的歧义，通过在相关文档中写入假设并根据自主政策继续来解决。明确标记不确定的假设，以便人类稍后可以审查。

## AI 代理注意事项

- 如果此文件为空或陈旧，在进行大规模实现工作之前请求或创建上下文更新。
- **当前进行中的工作**：检查 `docs/plans/` 中的未完成计划，而非此文件。
- AI 自主权默认为 `implement`；它由新鲜度（上文）和保护区域（`ai-autonomy-policy.md`）控制。此处不维护每个切片的自主权值 — 自主权标签位于待办事项/路线图工作项上，而非此文件中。
- AI 可以根据实时仓库证据纠正事实上下文，但在无人确认的情况下不得将陈旧文档标记为新鲜或降级保护区域。
- 当命令仍包含 `<fill real command>` 占位符或针对不存在的模块时，不要报告验证成功。
- 构建需要 `nop-entropy` 父 POM 首先在本地 Maven 仓库中可用。
- 当前重点是 `module-<domain>/model/` 中的 ORM 模型设计；在代码生成运行之前不要假设 Java 模块/包/视图路径。