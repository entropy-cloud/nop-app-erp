# 产品范围

## 当前里程碑（bootstrap）

- 产品摘要：将 `nop-app-erp` 初始化为 Nop Platform 上的 ERP 应用骨架。建立 AGE 文档结构和空的 ORM 模型，准备好进行域设计。
- 用户：ERP 操作员/管理员（最终）、开发人员（立即）
- MVP 范围：
  - 为 `nop-app-erp` 应用并定制的 AGE 文档结构
  - `model/app-erp.orm.xml` 骨架，具有正确的 Maven 坐标和空的字典/域/实体
  - 准备好接收第一个 ERP 业务域设计
- 延迟范围：
  - 选择特定的 ERP 业务域（采购/销售/库存/财务/等）——人工决策
  - ORM 实体设计——下一个里程碑
  - 通过 `nop-cli` 生成多模块项目——ORM 设计后
  - 应用构建/运行验证——代码生成后
- 成功指标：
  - 文档结构内部一致（占位符已解析或明确标记）
  - `model/app-erp.orm.xml` 验证为格式良好的 orm 骨架
  - `docs/context/project-context.md` 反映真实的 bootstrap 状态
- 约束：
  - Java 模块尚未存在；不要假设它们的路径是实时的
  - `nop-entropy` 父 POM 必须在未来编译之前构建

## 规则

本文件拥有当前里程碑范围。

不要在此处重复稳定的应用表面和工作流。将当前支持的行为放入 `docs/design/app-overview.md`。

将实现顺序放入计划中，而非此处。