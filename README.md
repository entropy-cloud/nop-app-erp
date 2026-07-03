[![Maven Build](https://github.com/entropy-cloud/nop-app-erp/actions/workflows/maven.yml/badge.svg)](https://github.com/entropy-cloud/nop-app-erp/actions/workflows/maven.yml)
[![AGE Workflow](https://img.shields.io/badge/AGE-Attractor_Guided_Engineering-blue)](#)

# Nop App ERP

nop-app-erp 首先是 **Attractor-Guided Engineering (AGE)** 工作流在大型业务系统上的完整实践——由 mission driver 驱动、AI 全自动完成从需求到设计到计划到审计到实现到验证的开发闭环。所有决策与变更以文档为唯一记录，不依赖 AI 对话记录。详见 [AGENTS.md](AGENTS.md)。

项目基于 [Nop Platform 2.0](https://github.com/entropy-cloud/nop-entropy) 构建，覆盖 18 个业务域，贯穿企业核心经营与周边管理职能。

---

## 与主流开源 ERP 的关键差异

对标 Odoo、ERPNext、iDempiere、Tryton 等产品，完整论证见 [competitive-comparison.md](docs/architecture/competitive-comparison.md)。核心差异来自 Nop 平台的**可逆计算**架构，而非单一功能点优劣。

**模型驱动 + Delta 升级层。** 常规 ERP 的定制修改与核心代码深度耦合，大版本升级时常导致定制断裂或丢失。Nop 的 ORM XML 是唯一真相源，定制遵循 **Model→Delta→Java** 决策链，Delta 文件在运行时与基线差量合并——升级基线不破坏定制。

**原生多套账（并行账簿）。** 凭证行、存货成本层、余额均携带 `acctSchemaId`，一笔业务可同时生成财务会计账、管理会计账和税务会计账。Odoo 和 ERPNext 一笔业务只能入一套账——这不是字段差异，是架构级限制。

**业财过账三件套 + posted 兜底扫描。** 业财过账由凭证体系（头、分录行、回链）、凭证模板引擎和全部业务单据上的 `posted` 标志三组件协同。定时扫描未过账单据自动补过账，保证最终一致。回链表让任一凭证可追溯到源单据——Odoo/ERPNext 实现弱于此。

**完整成本方法（7 种）。** 移动加权平均、全月一次加权、FIFO、LIFO、标准成本、个别计价、批次。Odoo 仅 3 种（缺 LIFO 和个别计价）。

**跨域 DAG 独立部署。** 18 域构成有向无环图（DAG），各域是独立 Maven 工程，域间通过 `I*Biz` 接口调用（非数据库 FK）。可按需组装——只取采购、库存、财务即可构成贸易版，无需引入制造和质量模块。单体 ERP 做不到。

**AI 开发全闭环。** 需求综合→设计→计划→独立审计→实现→全量验证→日志，mission driver 全自动驱动。所有决策可追溯，不依赖 AI 对话记录。详见 [application-development-workflow.md](docs/process/application-development-workflow.md)。

详情见 [competitive-comparison.md](docs/architecture/competitive-comparison.md)（含 8 个逐条论证的架构杠杆、7 产品对标矩阵，以及尚未超越点的诚实声明）。

---

## 18 个业务域

| 域 | 核心功能 |
|---|---|
| **采购** Purchase | 请购转订单（防重复转化）、订单审批、入库自动生成库存移动单、发票过账（AP_INVOICE 凭证）、付款核销、三单匹配（订单 / 入库 / 发票数量价格核对）、退货红字凭证冲应付 |
| **销售** Sales | 报价转订单（防重复转化）、订单审批 + 信用额度检查（软警告 / 硬拦截）、出库自动生成库存移动单、发票过账（AR_INVOICE 凭证）、收款核销、退货退款编排（红字凭证冲应收） |
| **库存** Inventory | 三层模型（移动单→不可变流水→余额）、多成本方法（移动加权平均、FIFO 等）、可用量校验、负库存配置、追溯链（正向 / 反向 / 退货 / 批次四类查询）、存货过账自动生成凭证、成本引擎策略分派 |
| **财务** Finance | 过账引擎（凭证模板按业务类型自动生成凭证）、AR/AP 辅助账 + 核销单 + 往来余额 + 账龄查询、应收 / 应付票据（7 态 + 贴现 + 授信额度）、费用报销 + 员工借款（报销自动抵扣借款）、期末结账（损益结转 / 汇兑重估 / 模块关账 / 反结账） |
| **资产** Assets | 资本化入账（审批→建卡→折旧计划→凭证）、折旧计算（直线法/双倍余额递减/工作量法）、处置（报废/出售→凭证） |
| **制造** Manufacturing | BOM 多级展开（含虚拟件、循环引用检测、深度上限）、标准成本卷算（自底向上递归：采购件按采购价、制造件按物料成本 + 工时 × 费率） |
| **主数据** Master Data | 物料/SKU/往来单位/仓库/库位/计量单位/币种/科目表管理，SKU 多单位换算、条码扫描、定价策略、启用停用控制 |
| **CRM** | 线索→商机→报价转化引擎、活动日历/事件提醒、线索评分、销售预测、区域与配额管理 |
| **人力资源** HR | 员工档案、合同、考勤、排班调度、薪酬计算（含个税）、薪酬模拟、招聘管理、能力模型、培训记录 |
| **项目管理** Projects | 项目立项/任务分解、工时记录、项目成本归集、预算控制、工时过账 |
| **质量管理** Quality | 检验模板/质检单、NCR/CAPA 工作流、SPC 控制图、质量看板 |
| **合同管理** Contract | 合同版本管理、开票计划→发票自动触发、电子签章、阶梯价/返利计算 |
| **客户服务** CS | 工单 + SLA（服务等级协议）计时引擎、客户满意度调查、知识库 |
| **设备维护** Maintenance | 设备台账、维护计划自动生成、维护请求/工单、停机跟踪、备件消耗管理 |
| **高级排程** APS | 工序级排产引擎（有限产能、正反向排程）、自动派工、替代工艺路线推荐、ATP（可承诺量）/ CTP（可承诺产能）交期承诺 |
| **分销需求** DRP | 网络补货计算、净需求推算、安全库存优化、补货建议 |
| **物流运输** Logistics | 承运商网关三层 SPI（可接顺丰/德邦/自有车队）、运费双轨过账、运输状态机 |
| **B2B/EDI** | EDI 格式 SPI（EDIFACT/X12/自定义）+信封状态机、ASN 入库处理、合作伙伴自助 onboarding、MFT AS2/SFTP 传输 |

---

## 验证状态

```text
mvn clean install -DskipTests  → BUILD SUCCESS（146 reactor 模块）
mvn test                       → BUILD SUCCESS（312+ 测试，0 Failure / 0 Error）
```

---

## 快速开始

JDK 17+、Maven 3.9.3+、Git。

```shell
# 编译 Nop 平台
git clone https://gitee.com/canonical-entropy/nop-entropy.git
cd nop-entropy
mvn -T 2C clean install -DskipTests -Dquarkus.package.type=uber-jar

# 构建本应用
cd nop-app-erp
mvn clean install -DskipTests

# 运行
java -Dfile.encoding=UTF8 -Dquarkus.profile=dev \
  -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar
```

## 定制能力

不改基线源码，按定制复杂度从低到高排列：配置（字典 / 参数 / 编号规则）→ 扩展字段 EAV → nop-dyn 动态实体 → Delta 定制覆盖 → 非下划线保留层 → BizModel / Processor 手写。详见 [docs/architecture/customization-capabilities.md](docs/architecture/customization-capabilities.md)。

## 文档

| 内容 | 位置 |
|---|---|
| 项目身份与验证命令 | [docs/context/project-context.md](docs/context/project-context.md) |
| 仓库结构速查 | [docs/context/codebase-map.md](docs/context/codebase-map.md) |
| 竞品对标分析 | [docs/architecture/competitive-comparison.md](docs/architecture/competitive-comparison.md) |
| 定制能力 | [docs/architecture/customization-capabilities.md](docs/architecture/customization-capabilities.md) |
| 应用基线 | [docs/design/app-overview.md](docs/design/app-overview.md) |
| 工作项选择与路线图 | [docs/backlog/README.md](docs/backlog/README.md) |
| 活动计划 | [docs/plans/](docs/plans/) |
| 开发日志 | [docs/logs/](docs/logs/) |

## 相关项目

- [nop-entropy](https://github.com/entropy-cloud/nop-entropy) — Nop Platform 2.0 后端
- [nop-chaos](https://github.com/entropy-cloud/nop-chaos) — Nop Platform 2.0 前端
- [attractor-guided-engineering-template](https://github.com/anomalyco/attractor-guided-engineering-template) — AGE 工作流模板

## License

MIT
