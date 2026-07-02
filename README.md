# nop-app-erp

基于 Nop Platform 的 ERP 应用，18 业务域模型已设计完成，骨架代码已生成并可构建运行。

> 开发方式上，本项目采用 Attractor-Guided Engineering (AGE) 工作流——非平凡功能走"需求→设计→计划→独立子代理审计→实现→全绿验证→结束审计→日志归档"的闭环，所有变更在 git 中可追溯。文档树 `docs/` 是项目记忆而非聊天记录。详见 `AGENTS.md` 和 `docs/process/application-development-workflow.md`。

---

## 项目状态

| 阶段 | 状态 |
|---|---|
| 18 域 ORM 模型（279 实体） | 已完成，经独立审计验证 |
| Codegen 骨架（1721 Java 文件） | 已完成，可构建 |
| `mvn clean install -DskipTests` | BUILD SUCCESS（146 reactor 模块） |
| `mvn test` | BUILD SUCCESS（0 Failures/0 Errors，312+ 测试） |
| 业务逻辑深化（BizModel） | 进行中 |

核心业财管线已打通：采购→入库→库存过账→应付→付款→凭证；销售→出库→库存过账→应收→收款→凭证；资产折旧/处置/资本化；期末结账（损益结转/汇兑重估/反结账）；AR/AP 核销与往来余额管理。

---

## 业务域覆盖

内置 18 域，按需组装：

- **核心 5 域**：主数据、库存、采购、销售、财务
- **第一批扩展 5 域**：资产、项目、制造、质量、维护
- **第二批扩展 8 域**：CRM、客服、HR、APS、合同、DRP、物流、B2B

---

## 架构：模型优先，Delta 定制

每域一个独立 Maven 子工程，从 XML 模型生成全栈代码，定制层在运行时叠加，不改基线：

```
module-<domain>/model/app-erp-<domain>.orm.xml   ← 唯一真相源
       ↓ nop-cli gen
app-erp-<domain>-{codegen, dao, service, web, meta, api}
       ↓ Delta 定制（运行时叠加）
客户化适配
```

- **146 个 Maven reactor 模块**，1721+ 生成 Java 文件
- **运行时**：Quarkus uber-jar + AMIS 前端（JSON 驱动） + Nop ORM
- **定制**：Delta 层（`_vfs/_delta/`）、扩展字段 EAV、动态实体（nop-dyn）、BizLoader——优先选成本最低的方式，不破坏升级路径
- **跨域依赖**：master-data ← inventory ← purchase / sales ← finance（DAG，无循环），域间走 `I*Biz` 接口而非 ORM 强引用

---

## 与主流开源 ERP 的定位差异

对标了 Odoo、ERPNext、metasfresh、iDempiere、Tryton 等七款产品（`docs/architecture/competitive-comparison.md`），几个关键差异点：

**升级不破坏定制**：Odoo/ERPNext 的定制与核心代码耦合，版本升级时定制需手工迁移。本项目的 Delta 层在运行时与基线合并，基线升级后定制自动适配。

**原生多套账**：`ErpMdAcctSchema`（财务/管理/税务/合并/预算），凭证行/成本层/余额都带 `acctSchemaId`，一笔业务可同时入多套账——Odoo/ERPNext 无此能力。

**业财一体的可审计性**：所有业务单据有 `posted` 标志，过账幂等且可重放。`ErpFinVoucherBillR` 回链表让凭证与源单据双向追溯。`ErpFinArApItem` open-item 明细账保证每笔核销有记录。

**成本方法完整**：7 种（移动加权平均/全月一次加权/FIFO/LIFO/标准成本/个别计价/批次），`ErpInvCostLayer` 支撑多层核算，按账套可不同。

**制造深度**：21 实体覆盖 MRP/生产版本/工单（10 态）/作业卡/委外/标准成本滚算，有独立 MRP 计划实体和生产版本（SAP 风格 lot-size 区间）。

---

## 快速开始

### 前置条件

JDK 17+，Maven 3.9.3+，Git。

先构建 nop-entropy 父 POM：

```shell
git clone https://gitee.com/canonical-entropy/nop-entropy.git
cd nop-entropy
mvn -T 2C clean install -DskipTests -Dquarkus.package.type=uber-jar
```

### 构建 & 运行

```shell
mvn clean install -DskipTests
java -Dfile.encoding=UTF8 -Dquarkus.profile=dev \
  -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar
```

### 修改 ORM 模型后重新生成

模型变更后跑 `mvn clean install -DskipTests` 触发增量代码生成即可，**不要**重跑 `nop-cli gen`。

---

## 文档入口

| 用途 | 位置 |
|---|---|
| 项目身份与验证命令 | `docs/context/project-context.md` |
| 仓库结构速查 | `docs/context/codebase-map.md` |
| 竞品对标分析 | `docs/architecture/competitive-comparison.md` |
| 定制能力一览 | `docs/architecture/customization-capabilities.md` |
| 产品愿景与约束 | `docs/architecture/project-vision.md` |
| 应用基线（页面/角色/流程） | `docs/design/app-overview.md` |
| 工作项选择 | `docs/backlog/README.md` |
| 活动计划与审计证据 | `docs/plans/` |
| 每日开发日志 | `docs/logs/` |
| Nop 平台开发文档 | `../nop-entropy/docs-for-ai/`（同级目录） |

---

## License

MIT
