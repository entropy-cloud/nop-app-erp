# 04 BizModel 中 48 处 `dao().updateEntity()` 违规使用

> 来源审计：`docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md`（H-* DAO 反模式）
> 关联计划：`docs/plans/2026-07-07-1915-1-audit-remediation-plan.md` H-2

## 问题

- 多个域的 BizModel 中直接调用 `dao().updateEntity(entity)` 全字段更新，绕过 `CrudBizModel` 标准的"乐观锁 + 部分字段更新"路径
- 估计 48 处违规调用，散布在 `module-{purchase,sales,inventory,finance,assets,projects,manufacturing,quality,maintenance,...}/erp-*-service/.../`
- 影响：绕过 `version` 乐观锁 → 并发覆盖丢失；全字段更新覆盖审计字段（`updatedBy`/`updatedAt`）→ 审计轨迹混乱；严重性：中（数据完整性 + 并发安全）

## 复现

- 环境：codegen 后的多个业务域 `*-service` 模块（BizModel / Processor）
- 触发：BizModel 中 `dao().updateEntity(entity)` 后并发两次保存同一实体 → 后写覆盖先写，无 `OptimisticLockException`
- 最小复现脚本：暂无（依赖 codegen 后的具体调用点）

## 诊断方法

- 诊断难度：直接（grep 即可定位）
- 调查路径：审计 §3.6 DAO 反模式扫描 → grep `dao().updateEntity(` 跨 `module-*/erp-*-service/` → 命中 48 处
- 决定性证据：`grep -rn "dao().updateEntity(" module-*/erp-*-service/src/main/java/ | wc -l` 输出 ≈ 48

## 根本原因

- 平台 API 误用：`CrudBizModel` 标准暴露 `save(entity)` / `update(entity)`（自动带乐观锁与审计字段），但代码生成器模板或人工编辑的 BizModel 退化为直接调 `dao().updateEntity(entity)`
- 缺少校验：项目缺少 lint 规则禁止 BizModel 直接调 `dao().update*` 系列（应统一经 `CrudBizModel` 父类方法或 `IOrmTemplate` 显式传播）

## 修复

- 此为系统性 bug，修复需在 codegen 模板层或全局替换层完成（不在本整改计划范围内）
- 待落地动作：
  - 全局替换：48 处 `dao().updateEntity(entity)` → `super.update(entity, ctx)`（或 `save(entity, ctx)` 视语义）
  - codegen 模板修正：`service-template/*.xpt` 中 BizModel 模板移除直接 DAO 调用范式
  - lint 规则：在 `docs/audits/nop-compliance-checker.sh` 增加 `dao().updateEntity(` 反模式 grep 检查
  - 单测基类增加并发保存场景的回归测试

## 测试

- 暂无自动化测试覆盖（修复未落地）
- 待落地后补充：并发保存场景的乐观锁异常测试

## 受影响的工件

- 多个域 `*-service` 模块的 BizModel / Processor / Executor 类（codegen 产物，48 处）
- `nop-entropy` 的 codegen 模板（如 `service-template/*.xpt`）— 待源仓库更新
- `docs/audits/nop-compliance-checker.sh` — 待增加反模式检查
- `docs/context/conventions.md` — DAO 使用约定（需增补"禁止 BizModel 直接调 `dao().updateEntity()`"规则）

## 未来重构注意事项

- BizModel 标准 API：所有更新操作必须经 `CrudBizModel.save/update/delete`（自动带乐观锁 + 审计字段），禁止直接调 `dao().updateEntity()` 或 `IOrmTemplate.update`
- 例外显式化：仅当确实需要绕过乐观锁（如批量后台任务）才允许直接 DAO，且必须在代码注释中记录原因

## 预防差距

- codegen 模板审查未覆盖"直接 DAO 调用"反模式
- 合规检查器 `nop-compliance-checker.sh` 当前未包含此反模式检查（M-4 整改会补充该规则）
