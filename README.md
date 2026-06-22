# nop-erp

#### 介绍

基于 Nop 平台（nop-entropy）架构的**产品化通用 ERP 产品**，可快速定制适配各个领域的业务 ERP 系统（零售、制造、贸易、医疗、教育等）。充分利用 Nop 平台内置的扩展能力（Delta 定制、扩展字段、动态实体、模块动态组装等），在不改基线源码的前提下实现各领域客户化。采用 Attractor-Ged Engineering（AGE）工作流进行 AI 辅助开发。

产品定位与定制能力见 `docs/architecture/project-vision.md` 与 `docs/architecture/customization-capabilities.md`。

#### 业务域范围

内置 10 个业务域，覆盖中等规模 ERP 的进销存+财务一体化+制造全链，交付时按需组装：

- **核心域（进销存+财务）**：主数据、库存、采购、销售、财务
- **扩展域**：固定资产、项目管理、制造、质量管理、设备维护

#### 软件架构

每业务域一个独立 Maven 工程，由聚合工程 `app-erp-app` 组装启动。每域工程由 `nop-cli gen` 从 `<domain>/model/app-erp-<domain>.orm.xml` 自动生成标准 8 层骨架：

1. `<domain>/model/app-erp-<domain>.orm.xml` — XML 格式的权威数据模型（实体、字典、领域类型），每域一份
2. `app-erp-<domain>-codegen` — 代码生成入口，根据 orm.xml 自动生成后台工程代码
3. `app-erp-<domain>-api` — 对外暴露的服务接口
4. `app-erp-<domain>-dao` — 数据库访问代码与实体代码
5. `app-erp-<domain>-service` — 服务实现代码（BizModel）
6. `app-erp-<domain>-web` — 前端页面（AMIS）
7. `app-erp-<domain>-meta` — XMeta 元数据与 i18n
8. `app-erp-app` — 聚合启动工程（Quarkus）
9. `deploy` — 根据数据模型自动生成的数据库建表语句
10. `docs/` — AGE 文档结构（需求、设计、架构、计划、日志等）

> 上面的模块 2-8 当前均不存在，需在 ORM 模型设计完成后通过 `nop-cli` 工具生成。模块拆分决策见 `docs/architecture/domain-module-split-analysis.md`。

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

编辑 `module-<domain>/model/app-erp-<domain>.orm.xml`，按 `docs/design/`、`docs/requirements/` 中的设计填充实体、字典、领域类型。

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
