# 项目愿景

## 目的

描述 `nop-app-erp` 的长期产品和工程吸引子。

## 产品定位

nop-app-erp 是基于 Nop 平台架构的**产品化通用 ERP 产品**，可快速定制适配各个领域的业务 ERP 系统：

- **通用 ERP 基线**：内置 18 个业务域（主数据/库存/采购/销售/财务 + 资产/项目/制造/质量/维护 + CRM/CS/HR/APS/合同/DRP/物流/B2B），覆盖中等规模 ERP 的进销存+财务一体化+制造全链及外围协作域。
- **产品化**：作为可发布的标准产品基线发布，不是一次性项目代码。基线通过 `nop-cli gen` 从 `model/*.orm.xml` 生成，遵循模型优先开发。
- **可定制**：充分利用 Nop 平台的扩展能力（Delta 定制、扩展字段 EAV、nop-dyn 动态实体、模块化组装、非下划线扩展层、BizLoader），快速适配零售、制造、贸易、医疗、教育等各领域的具体业务，**不改基线源码**。定制能力详见 `customization-capabilities.md`。
- **升级友好**：定制层与基线分离，基线升级时定制自动合并，不破坏客户化改动。

## 主要用户

- **最终用户**：ERP 系统操作员/管理员（采购员/销售员/库管员/财务员/资产管理员/项目经理/生产主管/质检员/维护人员等，角色见 `docs/design/roles-and-permissions.md`）。
- **实施方**：基于 nop-app-erp 基线定制各领域业务 ERP 的实施团队。
- **开发人员**：从真实业务领域应用学习 Nop Platform 模型优先开发的开发人员。

## 必须保持不变的约束

- 每业务域一份 `<domain>/model/app-erp-<domain>.orm.xml` 是该域持久化模型的唯一真相源；`model/*.orm.xml` 是 ask-first 保护区域。
- 切勿手动编辑生成的代码（`_gen/`、`_` 前缀文件、`_app.orm.xml`、`_service.beans.xml`）。
- 业务异常扩展 `NopException` 并使用 `ErrorCode`。
- 跨工程实体不做 ORM 层 `refEntityName` 强引用，走 `I*Biz` + Maven 依赖（DAG）。
- 禁止在 orm.xml 预置 `tenantId` 字段（租户隔离按平台标准，见 `system-baseline.md`）。
- 构建需要本地 Maven 仓库中的 `nop-entropy` 父 POM。
- 设计文档不针对任何特定竞品。

## 明确的非目标

- 不是框架核心项目（是应用层产品，基于 nop-entropy 平台）。
- 不是通用管理模板（是 ERP 领域应用）。
- 不复制 nop-app-mall（独立 ERP 域，mall 是单一电商域参考）。
- 不是项目代码（是产品化基线，定制走 Delta/扩展层而非改基线）。

## 第一个生产里程碑的成功标准

- 18 域 ORM 模型已设计并验证（当前进度：18 域共 447 实体已填充）。
- 多领域工程已通过 `nop-cli gen` 生成、应用构建并运行。
- 第一个完整业务循环（采购→入库→应付→凭证 或 销售→出库→应收→凭证）可端到端测试。
- 定制能力可演示（Delta 定制 + 扩展字段至少各一个落地样例）。

## AI 不应静默发明的必需人工决策点

- 是否启用 SaaS 多租户（影响 `useTenant` 配置）。
- 哪些业务域在交付范围内（模块组装裁剪决策）。
- 数据删除和会计过账语义。
- 超出 nop-auth 默认值的认证/权限模型。
- 外部集成（税控、银行、物流、电商平台等）。

## 注意事项

- 保持此文档稳定且高层级。
- 不要将其变成待办事项。
- 不要重复 `docs/requirements/product-scope.md` 中的当前里程碑范围。
- 不要重复 `docs/design/app-overview.md` 中的当前应用表面。
- 定制能力细节放 `customization-capabilities.md`，本文只给定位与约束。
- 将实现顺序放入 `docs/plans/` 或 `docs/requirements/`。
