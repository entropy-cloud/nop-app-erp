# ERP 设计核对清单

> 本清单用于核对 nop-app-erp 各子域设计的完善程度，确保达到并超过主流开源 ERP 产品的水平。
>
> **实现状态（2026-06-22 更新）：** 10 域 ORM 模型已升级到产品级基线（共 **145 实体**，见各 `<domain>/model/app-erp-<domain>.orm.xml`）。下方"已完成项（✅）"小节列出已在模型中落地的能力，并标注模型证据。架构对标与"超越点"分析见 `docs/architecture/competitive-comparison.md`。
>
> **类型规范：** 所有 10 域 orm.xml 已对齐 nop-entropy 官方惯例（对照 `docs-for-ai/02-core-guides/orm-model-design.md` + `code-style.md` + 源模型 `nop-auth.orm.xml`）：每列显式 `code`(UPPER_SNAKE_CASE) + `propId`(从1连续) + `stdSqlType`(仅 StdSqlType 枚举值) + `stdDataType`；字典字段用 `ext:dict=`（valueType=int，option value 10/20/30 递增）；通用字段走 domain 复用（delVersion/version/createdBy/createTime/updatedBy/updateTime/remark）+ 实体配 `useLogicalDelete`+`deleteFlagProp=delVersion`；关系用 `<join><on/></join>` 形式 + `tagSet="pub"`/`"pub,cascade-delete,insertable,updatable"`。
>
> **公共字段约定：** 所有业务单据头统一携带 `orgId`（业务组织）、`businessDate`（业务日期）、`posted`（业财过账标志）、`postedAt/postedBy`、`version`（乐观锁）；所有金额类单据头/行统一携带 `currencyId` + `exchangeRate` + `amountSource`（源币金额）+ `amountFunctional`（本位币金额）。详见 `docs/design/domain-design-guidelines.md` 的"单据标准字段约定"小节。

---

## 一、业财一体化设计（核心）

### 1.1 凭证三件套
- [ ] 凭证头（VoucherHead）：凭证字、凭证号、凭证日期、制单人、审核人
- [ ] 借贷分录行（VoucherLine）：科目编码、借方金额、贷方金额（每行只填一侧）、摘要
- [ ] 业财回链表（VoucherBillR）：凭证号、单据类型、单据号、行号

### 1.2 自动过账机制
- [ ] 统一 `onSubmit/onReverse` 钩子入口
- [ ] 审批触发同时落地库存与凭证（同事务）
- [ ] 异步过账支持（EventBus/消息队列）
- [ ] `posted` 字段标记 + 兜底扫描任务

### 1.3 凭证模板引擎
- [ ] 凭证模板表 + 占位符支持
- [ ] 按业务类型（INPUT/OUTPUT/AP_INVOICE/AR_INVOICE/PAY/RECEIPT）区分模板
- [ ] 多金额档位填充

---

## 二、库存三层模型

### 2.1 三层架构
- [ ] 库存移动单（计划/执行合一）：移动类型、来源/目标库位、数量、批次/序列号
- [ ] 不可变库存流水：数量、单价、金额、FIFO队列、源单引用
- [ ] 库存余额快照：按 item×warehouse×lot 聚合

### 2.2 库存追溯链
- [ ] 移动单上下游关联（move_orig_ids/move_dest_ids）
- [ ] 采购到货→生产领料→销售出库全链路追溯

### 2.3 作业类型参数化
- [ ] 入库/出库/调拨/生产操作类型
- [ ] 绑定默认库位与科目映射

---

## 三、单据模型设计

### 3.1 双维度类型设计
- [ ] `docType`（入库/出库/其它）+ `bizType`（采购/销售/调拨/盘点...）
- [ ] 单据唯一编号约束

### 3.2 三单链闭环
- [ ] 订单→成交/出入库→退货 完整链条
- [ ] 单据状态双维度：业务状态 + 审批状态 + 财务状态（三轴独立）

### 3.3 价税分离
- [ ] 明细表内建税字段三件套：`taxRate`/`taxAmount`/`amount`
- [ ] 含税价/不含税价/计价基准类型

---

## 四、主数据设计

### 4.1 物料与SKU
- [ ] `Material`（基础属性）+ `MaterialSku`（单位+条码+多档价格）分离
- [ ] 多单位换算支持

### 4.2 往来单位
- [ ] 供应商/客户一体表
- [ ] 应收应付余额字段（期初+累计+预收预付）

### 4.3 序列号管理
- [ ] 独立序列号台账表
- [ ] 出入库双向回链

---

## 五、科目表（COA）与成本核算

### 5.1 科目结构
- [ ] 父子树形科目 + 段值编码
- [ ] 多会计科目表并行（管理账/税务账/预算账）

### 5.2 科目映射
- [ ] 多维决策表（伙伴组/产品类别/项目/仓库 → 科目）
- [ ] 规则引擎驱动，非硬编码

### 5.3 成本核算
- [ ] 多种成本方法：移动加权平均/FIFO/批次
- [ ] Landed cost（到岸成本）支持

---

## 六、多公司/多币种/多租户

### 6.1 多租户架构
- [ ] `orgId` 字段表达公司/组织维度
- [ ] 凭证与单据同租户校验

### 6.2 多币种支持
- [ ] 凭证行双币种字段（源币种 + 本位币）
- [ ] 汇率转换独立配置

---

## 七、应收应付核销

### 7.1 核销关系
- [ ] 多对多核销表（付款单→多张发票）
- [ ] 头级 `paidStatus`（N/Y/PART）聚合

### 7.2 欠款闭环
- [ ] 折后金额+其他费用−订金−已结算 公式明确
- [ ] 三单匹配（PO-入库-发票）支持

---

## 八、状态机设计

### 8.1 声明式状态机
- [ ] DSL/字典声明转换规则
- [ ] 集中校验，避免散落 if-else

### 8.2 双轴状态分离
- [ ] `docStatus`（业务生命周期）
- [ ] `approveStatus`（审批流）

---

## 九、流程与自动化

### 9.1 业务流程
- [ ] 声明式状态机为核心
- [ ] 可选 BPM 审批流叠加层

### 9.2 触发器链式自动化
- [ ] 报价→订单→发票→发货 链式触发

---

## 十、本地化支持

### 10.1 中国本地化
- [ ] 独立可拔 `l10n-cn` 模块
- [ ] 金税接口、增值税发票、银行对账、中国特色报表

---

## 参考对标项目

| 领域 | 首选参考 | 关键特性 |
|---|---|---|
| 业财一体 | 赤龙 ERP + ERPNext | 凭证三件套 + on_submit 自动过账 |
| 库存三层 | Odoo | stock.move/picking/quant |
| 单据设计 | 管伊佳 + 若依 | 双维度类型 + 三单链 |
| 主数据 | 管伊佳 | Material+SKU 分离 |
| 多公司/多币种 | iDempiere | AD_Client/AD_Org + C_AcctSchema |
| 凭证引擎 | Metasfresh | 类型安全注册 + EventBus |
| 状态机 | Tryton | 声明式转换规则 |

---

## 检查状态跟踪

| 域 | 设计状态 | 数据库设计 | 核对完成度 |
|---|---|---|---|
| master-data | 已完成 | 进行中 | 85% |
| inventory | 已完成 | 进行中 | 90% |
| purchase | 已完成 | 进行中 | 85% |
| sales | 已完成 | 进行中 | 85% |
| finance | 已完成 | 进行中 | 85% |
| assets | 已完成 | 进行中 | 80% |
| manufacturing | 已完成 | 进行中 | 75% |
| projects | 已完成 | 进行中 | 75% |
| maintenance | 已完成 | 进行中 | 80% |
| quality | 已完成 | 进行中 | 75% |

---

## 各域设计核对评估

### 已完成项（✅）

#### 一、业财一体化设计
- ✅ 凭证头（`ErpFinVoucher`）：凭证字、凭证号、凭证日期、`orgId`、`acctSchemaId`、制单人、审核人、过账人/时间
- ✅ 借贷分录行（`ErpFinVoucherLine`）：科目、借/贷金额、`currencyId`+`exchangeRate`+`amountSource`+`amountFunctional`（双币种）、辅助核算（往来/部门/项目/仓库/物料）、`acctSchemaId`（多账套）
- ✅ 业财回链表（`ErpFinVoucherBillR`）：凭证号、单据类型、单据号、行号
- ✅ 凭证模板表（`ErpFinVoucherTemplate`/`Line`）+ 占位符 + 按 `businessType` 区分模板
- ✅ `posted` 字段 + `postedAt`/`postedBy`：业务单据头统一携带（采购/销售/库存/资产/项目/维护/质量/制造工单）
- ✅ 兜底扫描任务：通过 `posted=false` 的源单据扫描触发补过账（见 `docs/design/finance/posting.md`）

#### 二、库存三层模型
- ✅ 库存移动单（`ErpInvStockMove`/`Line`）：移动类型、来源/目标库位、数量、批次/序列号、`businessDate`/`orgId`/`posted`
- ✅ 不可变库存流水（`ErpInvStockLedger`）：数量、单价、金额、`costMethod`、`acctSchemaId`、源单引用
- ✅ 库存余额快照（`ErpInvStockBalance`）：按 material×warehouse×lot 聚合，含预留/冻结/可用量
- ✅ 成本层（`ErpInvCostLayer`）：FIFO/批次/具体辨认的多层成本核算载体
- ✅ 库存预留单（`ErpInvReservation`/`Line`）：记录级预留（支撑可用量校验与按订单/工单预留）
- ✅ 拣货单（`ErpInvPickingOrder`/`Line`）：出库作业链
- ✅ 移动单上下游关联（`relatedBillType`/`relatedBillCode`）+ 采购到货→生产领料→销售出库全链路追溯
- ✅ 作业类型参数化（`erp-inv/operation-type`）+ 批次/序列号台账状态字典化

#### 三、单据模型设计
- ✅ `docStatus` + `approveStatus` 双维度设计（含制造工单 10 态、作业卡 8 态）
- ✅ 三单链闭环（请购→询价→报价→订单→出入库→发票→收付款→核销）
- ✅ 三轴状态分离：业务状态 + 审批状态 + `posted`（业财状态）
- ✅ 价税分离：`taxRate`/`taxRateId`（税率主数据）/`taxAmount`/`amount`/`amountWithTax`

#### 四、主数据设计
- ✅ `ErpMdMaterial` + `ErpMdMaterialSku` 分离（含计价方法、批次/序列号管理标志）
- ✅ 多单位换算（`ErpMdUoMConversion`：物料级 + 通用级换算系数）
- ✅ 往来单位一体表（`ErpMdPartner`）+ 多地址（`ErpMdPartnerAddress`）+ 多联系人（`ErpMdPartnerContact`）+ 多银行账户（`ErpMdBankAccount`）
- ✅ 序列号台账（`ErpInvSerialNumber`）+ 批次台账（`ErpInvBatch`）独立表
- ✅ 税率主数据（`ErpMdTaxRate`）+ 结算方式（`ErpMdSettlementMethod`）+ 职员（`ErpMdEmployee`，解耦 Partner 重载）

#### 五、科目表与成本核算
- ✅ 父子树形科目（`ErpMdSubject`）+ 段值编码 + 辅助核算开关
- ✅ 多会计核算表并行（`ErpMdAcctSchema` + `ErpMdAcctSchemaCoa`：财务/管理/税务/合并/预算账套）
- ✅ 多种成本方法（`erp-md/cost-method`：移动加权/全月加权/FIFO/LIFO/标准/具体辨认/批次）

#### 六、应收应付核销
- ✅ 应收应付明细账（`ErpFinArApItem`）：open-item 模型，逐笔核销（替代"魔法更新 Partner 余额"）
- ✅ 核销单（`ErpFinReconciliation`/`Line`）：付款/收款 ↔ 发票多对多 + 汇兑损益
- ✅ 头级 `paidStatus`/`writtenOffStatus` 聚合（采购付款/销售收款）
- ✅ 三单匹配支持（采购订单→入库→发票）

#### 七、状态机设计
- ✅ 声明式状态机设计（见各域 `state-machine.md`）
- ✅ 双轴/三轴状态分离（业务 + 审批 + 业财过账）

#### 八、多公司/多币种/多租户
- ✅ `orgId` 字段表达公司/组织维度（所有业务单据头 + 组织树 `ErpMdOrganization`）
- ✅ 双币种字段（`currencyId` + `exchangeRate` + `amountSource` + `amountFunctional`）在所有金额类单据统一
- ✅ 多账套并行核算（`acctSchemaId` 在凭证头/行 + 总账余额 + 存货估值）
- ✅ 多租户：走平台标准（不在源模型预置 `tenantId`，见 `docs/architecture/customization-capabilities.md`）

#### 九、流程与自动化
- ✅ 域间边界与交互模式定义（见 `docs/architecture/integration-and-transaction-patterns.md`）
- ✅ 跨域协作规则（同步调用 I*Biz + 事件驱动）
- ✅ 事务边界与一致性策略
- ✅ 错误处理与恢复机制

#### 十、架构约束与最佳实践
- ✅ 域设计指南（设计原则、权限安全、性能优化，含"单据标准字段约定"）
- ✅ 与 Nop Platform 对齐（技术栈、代码生成、配置管理）
- ✅ 制造全链（`ErpMfgMrpPlan`/`ProductionVersion`/`WorkOrderLine`/`MaterialIssue`/`JobCardTimeLog`/`SubcontractOrder`/`CostRollup`）
- ✅ 跨域 DAG 架构（master-data 为根，业务域间走 I*Biz 不做 ORM 强引用，无循环）
- ✅ 版本演进策略（向后兼容、数据迁移、灰度发布）

#### 十一、Nop Platform 核心组件集成
- ✅ nop-wf 审批流程引擎（采购/销售/财务/资产审批流程）
- ✅ nop-rule 规则引擎（科目映射、凭证模板、审批条件、容差校验）
- ✅ nop-report 报表引擎（财务报表、业务报表、管理报表）

#### 十二、扩展域完善
- ✅ 资产域：折旧与财务打通机制（资本化、处置、价值调整）
- ✅ 项目域：成本归集与预算控制（工时成本、辅助核算）
- ✅ 维护域：设备与业务集成（备件消耗、停机影响排产、OEE）
- ✅ 质量域：质检与业务集成（质检触发、NCR/CAPA 闭环）

### 待完善项（⚠️）

#### 九、流程与自动化
- ⚠️ 可选 BPM 审批流叠加层
- ⚠️ 触发器链式自动化

#### 十、本地化支持
- ⚠️ 独立可拔 `l10n-cn` 模块
- ⚠️ 金税接口、增值税发票、银行对账、中国特色报表

---

## 改进建议

### 高优先级（立即实施）
1. **数据库模型落地**：基于设计文档生成 ORM XML 模型文件
2. **代码生成**：通过 nop-cli 生成 DAO、Service 层代码框架

### 中优先级（下一阶段）
1. **BPM 审批流**：作为可选叠加层引入
2. **触发器链式自动化**：报价→订单→发票→发货链式触发

### 低优先级（后续迭代）
1. **中国本地化模块**：金税接口、增值税发票、银行对账

---

## 设计文档完整性总结

| 文档 | 状态 | 说明 |
|---|---|---|
| `docs/design/domain-design-guidelines.md` | ✅ 已完善 | 域设计原则、边界、协作模式、一致性策略 |
| `docs/design/flow-overview.md` | ✅ 已完善 | 四层流程架构、核心业务流程、状态映射 |
| `docs/design/erp-design-audit-checklist.md` | ✅ 已完善 | 核对清单、完成度跟踪、改进建议 |
| `docs/design/master-data/README.md` | ✅ 已完成 | 主数据域设计 |
| `docs/design/inventory/README.md` | ✅ 已完成 | 库存域设计 |
| `docs/design/purchase/README.md` | ✅ 已完成 | 采购域设计 |
| `docs/design/sales/README.md` | ✅ 已完成 | 销售域设计 |
| `docs/design/finance/README.md` | ✅ 已完成 | 财务域设计 |
| `docs/design/assets/README.md` | ✅ 已完成 | 资产域设计 |
| `docs/design/assets/depreciation-and-posting.md` | ✅ 已完善 | 折旧与财务打通详细机制 |
| `docs/design/manufacturing/README.md` | ✅ 已完成 | 制造域设计 |
| `docs/design/projects/README.md` | ✅ 已完成 | 项目域设计 |
| `docs/design/projects/cost-collection.md` | ✅ 已完善 | 项目成本归集与预算控制 |
| `docs/design/maintenance/README.md` | ✅ 已完成 | 维护域设计 |
| `docs/design/maintenance/equipment-integration.md` | ✅ 已完善 | 设备与业务集成详细机制 |
| `docs/design/quality/README.md` | ✅ 已完成 | 质量域设计 |
| `docs/design/quality/inspection-integration.md` | ✅ 已完善 | 质检与业务集成详细机制 |
| `docs/design/**/state-machine.md` | ✅ 已完成 | 各域状态机设计 |
| `docs/design/finance/posting.md` | ✅ 已完成 | 业财打通机制 |
| `docs/design/inventory/cross-domain.md` | ✅ 已完成 | 库存跨域协作 |
| `docs/design/purchase/three-way-match.md` | ✅ 已完成 | 三单匹配规则 |

---

## 设计完善度总结

所有核心域和扩展域的设计文档已达到**超越主流开源 ERP** 的完善程度：

### 核心优势
1. **业财一体化深度整合**：凭证三件套 + 异步过账 + 业财回链
2. **库存三层模型**：移动单 + 不可变流水 + 余额快照
3. **松耦合架构**：事件驱动解耦业务域与财务域
4. **最终一致性保障**：posted 标志 + 兜底扫描机制
5. **完整的扩展域支持**：资产、项目、维护、质量四大扩展域全面完善
6. **插件化扩展**：凭证生成 Provider + 规则引擎
7. **完整审计追溯**：红字冲销 + 双向回链
8. **跨域协作协议**：明确的事件格式与交互规则

### 设计覆盖范围
- ✅ 10 个业务域全部覆盖
- ✅ 状态机设计完整（10 维度审查）
- ✅ 业财打通机制完整
- ✅ 跨域协作规则完整
- ✅ 与 Nop Platform 深度对齐

设计文档已具备直接指导代码实现的能力，可以进入数据库模型落地阶段。