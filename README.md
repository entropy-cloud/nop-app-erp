# nop-app-erp

#### 介绍

基于 Nop 平台（nop-entropy）构建的企业资源计划（ERP）参考应用工程。采用 Attractor-Guided Engineering（AGE）工作流进行 AI 辅助开发。

> 当前阶段：**bootstrap（骨架初始化）**。仅包含 AGE 文档结构与空的 ORM 模型骨架，Java 多模块工程尚未生成。

#### 软件架构

1. `model/app-erp.orm.xml` — XML 格式的权威数据模型（实体、字典、领域类型）
2. `app-erp-api` — 对外暴露的服务接口（代码生成后产生）
3. `app-erp-codegen` — 打包时根据 `app-erp.orm.xml` 自动生成后台工程代码
4. `app-erp-dao` — 后台数据库访问代码与实体代码
5. `app-erp-service` — 后台服务实现代码
6. `app-erp-web` — 前端页面对应的 JSON 和 JS 代码（AMIS）
7. `deploy` — 根据数据模型自动生成的数据库建表语句
8. `docs/` — AGE 文档结构（需求、设计、架构、计划、日志等）

> 上面的模块 2-7 当前均不存在，需在 ORM 模型设计完成后通过 `nop-cli` 工具生成。

#### 环境准备

- JDK 17+
- Maven 3.9.3+
- Git

#### 安装与生成

**第一步：先编译 [nop-entropy](https://gitee.com/canonical-entropy/nop-entropy) 平台**

```shell
git clone https://gitee.com/canonical-entropy/nop-entropy.git
cd nop-entropy
mvn -T 2C clean install -DskipTests -Dquarkus.package.type=uber-jar
```

**第二步：设计 ORM 模型**

编辑 `model/app-erp.orm.xml`，按 `docs/design/`、`docs/requirements/` 中的设计填充实体、字典、领域类型。

**第三步：通过 nop-cli 生成多模块工程**（待 ORM 模型完成后执行）

```shell
java -jar nop-cli.jar gen -t=/nop/templates/orm model/app-erp.orm.xlsx
```

**第四步：编译与运行**

```shell
mvn clean install -DskipTests
java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-app/target/app-erp-app-1.0-SNAPSHOT-runner.jar
```

#### 文档导航

- `START-HERE-after-copy.md` — 模板应用后的初始化检查清单
- `AGENTS.md` — AI 协作契约
- `docs/index.md` — 文档路由总入口
- `docs/context/project-context.md` — 项目身份与验证命令
- `docs/process/application-development-workflow.md` — 开发工作流

Nop 平台开发文档位于 `../nop-entropy/docs-for-ai/`（兄弟目录）。

#### License

MIT
