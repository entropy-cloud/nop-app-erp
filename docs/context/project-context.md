# 项目上下文

## 目的

保持此文件作为 AI 代理在执行有用工作之前所需的最短低变化上下文。

本文件不是执行仪表板。不要在此处镜像活动计划、当前阻塞项、当前待办事项行或日期审计/日志状态。必要时从其拥有文件中发现这些信息。

## 项目标识

- 项目名称：nop-app-erp
- 产品类型：基于 Nop Platform 构建的企业资源规划（ERP）应用骨架
- 主要用户：ERP 系统操作员/管理员以及从现实业务领域应用学习 Nop Platform 的开发人员
- 文档新鲜度：`fresh`（18 域 ORM 模型已设计完成，279 实体；设计文档经多次审计验证）

**新鲜度控制：**

- 如果新鲜度为 `stale` 或 `unknown`，代理可以研究、审计和起草对齐文档，但在重新建立基线或人工确认预期行为之前，不得实现产品行为。
- 如果新鲜度为 `partially stale`，代理只能实现其需求、owner doc、codebase-map 路由和触及代码区域已明确验证为新鲜的切片；否则将切片视为 `plan-first` 或 `research-only`。
- AI 不得在无人确认或无人批准的 owner-doc 证据的情况下将陈旧文档标记为新鲜。

## 当前技术基线

- 前端栈：百度 AMIS（`.view.xml` 文件中的 JSON 驱动 UI）— 代码生成后可用
- 后端栈：Java 17+、Quarkus、Nop Platform（nop-entropy 2.0.0-SNAPSHOT）— 代码生成后可用
- 数据库/模型源：`module-<domain>/model/app-erp-<domain>.orm.xml` 中的 XML 模型

## 当前项目阶段

`nop-app-erp` 处于 **codegen 已完成、待 BizModel 业务逻辑深化阶段**。18 域 ORM 模型（279 实体）+ 跨域 sys 通知派发子系统（`module-notify`，3 实体）已设计完成并经审计验证。所有域的 `nop-cli gen` 骨架已生成（1730+ 个 Java 文件），包含实体类、DAO、I*Biz 接口、BizModel、XMeta、view.xml 骨架。`app-erp-all` 聚合 app 已构建通过（154 reactor 模块 / 18 域 + notify 子系统）。

- codegen 产物是标准 CRUD 空壳（`CrudBizModel<T>` + 空 `I*Biz`），需深化业务逻辑
- 后续模型变更用 `mvn clean install` 增量重新生成，**不要**重跑 `nop-cli gen`
- **工程命名映射**：物理目录 `module-<domain>/` ↔ 逻辑工程名 `app-erp-<domain>` ↔ appName `erp-<简称>` ↔ moduleId `erp/<简称>` 的完整映射见 `docs/architecture/domain-module-split-analysis.md §2.0`
- 当前重点：按 roadmap 依次深化 BizModel → ErrorCode → 页面定制 → 端到端验证
- 详见 `docs/analysis/2026-06-25-1649-ai-automation-roadmap.md`

## 验证命令

> **当前状态**：根 pom 和 `app-erp-all` 已可构建（`mvn clean install -DskipTests` 全绿，154 reactor 模块 / 18 域 + notify 子系统）。所有域 codegen 骨架已就绪。

| 目的 | 命令 |
|------|------|
| 构建全项目 | `mvn clean install -DskipTests` |
| 本地运行应用 | `java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar` |
| 类型检查/编译检查 | `mvn compile -DskipTests` |
| 单元测试 | `mvn test`（随 BizModel 实现逐步补充） |
| 首次生成单域模块骨架（仅首次） | `nop-cli gen module-<domain>/model/app-erp-<domain>.orm.xml -t=/nop/templates/orm` |
| 增量重新生成（模型变更后） | `mvn clean install -DskipTests`（触发 gen-orm.xgen 增量链） |
| XML well-formed 校验 | `xmllint --noout module-<domain>/model/app-erp-<domain>.orm.xml` |

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