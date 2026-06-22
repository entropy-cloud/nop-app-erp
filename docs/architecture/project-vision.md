# 项目愿景

## 目的

描述 `nop-app-erp` 的长期产品和工程吸引子。

## 填写内容

- 产品目标：基于 Nop Platform 的参考企业资源规划（ERP）应用，展示典型 ERP 业务领域（如主数据、单据、过账）的模型优先开发。具体业务领域范围在 ORM 模型设计阶段决定。
- 主要用户：ERP 系统操作员/管理员，以及从真实业务领域应用学习 Nop Platform 的开发人员
- 必须保持不变的约束：
  - `model/app-erp.orm.xml` 保持为持久化模型的唯一真相源
  - 切勿手动编辑生成的代码
  - 业务异常扩展 `NopException` 并使用 `ErrorCode`
  - 构建需要本地 Maven 仓库中的 `nop-entropy` 父 POM
- 明确的非目标：
  - 不是框架核心项目
  - 不是通用管理模板
  - 不复制 nop-app-mall；它是独立的 ERP 域
- 第一个生产里程碑的成功标准：ORM 模型已设计、多模块项目已生成、应用构建并运行、第一个 ERP 业务循环可端到端测试
- AI 不应静默发明的必需人工决策点：
  - 哪些 ERP 业务域在范围内（采购/销售/库存/财务/等）
  - 数据删除和会计过账语义
  - 超出 nop-auth 默认值的认证/权限模型
  - 外部集成

## 注意事项

- 保持此文档稳定且高层级。
- 不要将其变成待办事项。
- 不要重复 `docs/requirements/product-scope.md` 中的当前里程碑范围。
- 不要重复 `docs/design/app-overview.md` 中的当前应用表面。
- 将实现顺序放入 `docs/plans/` 或 `docs/requirements/`。