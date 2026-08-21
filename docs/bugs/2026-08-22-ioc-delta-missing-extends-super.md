# 2026-08-22 IoC 兼容层 delta 缺 `x:extends="super"` 整文件替换平台 beans（五域先例潜伏缺陷）

- 发现于：plan `2026-08-22-0002-1`（M2.1 finance 迁移）Phase 3
- 影响：md（M1.1）/ notify（M1.2）/ aps（M3.9）/ b2b（M3.8）/ contract（M3.6）五域 test-scope VFS delta `_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`
- 状态：fin 侧已修正（带 `x:extends="super"`）；五域先例文件未修正（各自测试不受影响，登记待各域 successor/M4.1 统一回收）

## 现象

M2.1 finance 快照重录期 `TestErpFinVoucherTemplateAuditLog` 2 用例失败：`NopSysChangeLog` 0 行落库（audit 拦截器未生效），同时按先例落 IoC delta 后容器可启动。

## 根因

先例 delta（六域 `nopSequenceGenerator` self-wait 修复，plan 2026-08-21-1045 M1.1 首创）文件根元素缺 `x:extends="super"`（`../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md` 规则 3）。VFS delta 层同路径文件在无 `x:extends="super"` 时**整文件替换**基础文件而非增量合并——平台 `nop-sys-dao/_vfs/nop/sys/beans/app-dao.beans.xml` 中的其余 bean 全部丢失：

- `nopOrmEntityChangeLogInterceptor`（`feature:on nop.orm.audit.enabled`）→ finance `tagSet="audit,audit-save"` 实体（VoucherTemplate/Line、GlMappingRule 等）字段级变更审计失效（本缺陷的暴露面）
- `nopCodeRuleGenerator` / `nopSysCalendar` / `nopCodeRule` / `nopSysDaoResourceLockManager` / `nopSysDaoLeaderElector` / `nopSysDaoMessageService` / SysDictLoader / SysI18nMessageLoader + `<import resource="_dao.beans.xml"/>` 全链丢失（五域测试未触及，静默）

五域未暴露的原因：md/notify/aps/b2b/contract 均无 audit tagSet 实体且测试不依赖同文件其余 bean；bean 容器对缺失 bean 不报错（按需收集 `ioc:collect-beans by-type IOrmInterceptor` 静默为空集）。

## 修复

fin delta（`module-finance/erp-fin-service/src/test/resources/_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`）：

1. 根元素补 `x:extends="super"`（增量合并，其余 bean 继承保全）
2. 仅 `nopSequenceGenerator` bean 保持 `x:override="replace"` + `ioc:lazy-property`（self-wait 修复本体）
3. 根元素补 `xmlns:feature="feature"`（继承的 `feature:on` 属性命名空间）

实证：interceptor 恢复创建（audit 日志 15 行落库）+ self-wait 保持修复（容器正常启动）双绿；TestErpFinVoucherTemplateAuditLog 2/2 转绿。

## 回收建议

md/notify/aps/b2b/contract 五域 delta 同步补 `x:extends="super"`（一行变更 ×5 文件）——归各域 plan 触碰时或 M4.1 统一回收；在 M4.1 前五域测试无影响（已实证无依赖面）。
