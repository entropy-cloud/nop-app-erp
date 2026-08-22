# 2026-08-21 nop-entropy 重装 jar 后 nopSequenceGenerator IoC 初始化循环等待（bean-init-self-wait）

## 现象

- module-master-data `erp-md-service` 测试容器启动失败：`nop.err.ioc.bean-init-self-wait`（bean `nopSequenceGenerator` 初始化循环等待——同一线程等待自身初始化阶段完成）。
- 调用链：`@@initBean:$DEFAULT$nopOrmSessionFactory` → `@@setupBean:$DEFAULT$nopOrmTemplate` → `@@setupBean:nopSequenceGenerator`（`/nop/sys/beans/app-dao.beans.xml`）。
- 2026-08-21 09:33 前后重装 nop-entropy 快照 jar 到本地 Maven 仓库后出现；此前同批测试全绿。bisect 证实为**平台重装引发的 bean 初始化顺序回归**，与 id-string 迁移（plan 2026-08-21-1045-3）的 `stdDataType` 变更无关（Long→String 不触及 bean 装配顺序）。
- 复现面：md 域全部容器测试类（24 类）在回归窗口内首次启动即失败；脱离本仓测试基建以 JUnit Platform Launcher 直跑 `ErpMdWebPagesTest`（md-web，无下述 delta）同样命中，证明是平台级而非 md 测试代码问题。

## 根因

- 平台 `nop/sys/beans/app-dao.beans.xml` 的 `nopSequenceGenerator`（`SysSequenceGenerator`，`ioc:delay-method="lazyInit"`）依赖 `nopOrmTemplate`/`nopTransactionTemplate`；`nop/orm/beans/orm-defaults.beans.xml` 装配 `nopOrmSessionFactory`/`nopOrmTemplate` 时又 setup 依赖 `nopSequenceGenerator`——重装后 IoC 容器对这组 bean 的初始化排序退化，形成同线程重入等待（self-wait）。
- 平台自身已有同型先例：`orm-defaults.beans.xml` 中 `nopDefaultTransactionListener.ormTemplate` 使用 `ioc:lazy-property="true"` 延迟注入打破同类环。

## 修复（本仓 2026-08-21 已落地：test-scope VFS delta）

- `module-master-data/erp-md-service/src/test/resources/_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`：以 XML 显式 `ioc:lazy-property` 恢复容器级延迟注入（镜像平台 `nopDefaultTransactionListener.ormTemplate` 先例，`x:override="replace"`）。
- 仅测试 classpath 生效，零生产代码影响；落地后 md 24 测试类 155/155 全绿（两次独立全量复跑）。

## successor（触发条件）

- 其他模块测试容器再现 `nopSequenceGenerator` self-wait 时，可按同款 test-scope delta 处理（或待平台修复）。
- 平台侧修复（nop-entropy 恢复稳定初始化顺序）后**移除本 delta**；若后续发现 delta 与平台修复后行为冲突，同样移除并回归本条目。

## 关联

- plan `docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md` Phase 3（执行期发现修复）
- 日志 `docs/logs/2026/08-21.md`

## 2026-08-22 补记：第二环（nopOrmSessionFactory self-wait，经 common-service nopDataAuthChecker）

- 发现于：plan `2026-08-22-0731-1`（M3.3 hr 迁移）Phase 3。
- 环境时点：nop-orm jar 08-22 08:20 被并行 agent 重装（上游 nop-entropy 同日 log「self-wait 断环：orm-defaults sequenceGenerator ref 加 ignore-depends」修复后构建）；上游修复消除了 nopSequenceGenerator 环，但暴露第二环。
- 环链（堆栈逐帧）：容器启动 `DataBaseSchemaInitializer`（`ioc:after="nopOrmSessionFactory" ioc:force-init="true"`）→ `$DEFAULT$nopOrmSessionFactory` initBean（by-type 装配 `nopDataAuthEntityFilterProvider`，其 `<on-bean>nopDataAuthChecker</on-bean>` 条件拉起）→ `nopDataAuthChecker.daoProvider`（`app.erp.common.auth.ErpRoleDataAuthChecker` @Inject 字段，module-common-service）→ `$DEFAULT$nopDaoProvider`（ctor-arg ref nopOrmTemplate 无豁免）→ `$DEFAULT$nopOrmTemplate`（autowire sessionFactory）→ 同线程重入 sessionFactory（self-wait）。
- 环境性实证（非本仓代码回归）：hr 迁移改动 stash 无关；同 classpath 下 **cs 域 ORM 重测试类同样复现**（`TestErpCsCatalogFulfillmentEngine` self-wait，cs M3.5 done 状态被 08:20 重装破坏）。凡 test 容器加载 common-service（nopDataAuthChecker）+ 平台当前 jar 的域均受影响。
- 修复（hr 侧 2026-08-22 落地）：`module-hr/erp-hr-service/src/test/resources/_vfs/_delta/default/erp/common/beans/app-service.beans.xml`——`nopDataAuthChecker` 的 daoProvider 注入改 XML 显式 `ioc:lazy-property="true"`（`x:override="replace"`，根元素 `x:extends="super"` 保全同文件其余 bean，含 E4.2 MaskAuditRecorder）。ErpRoleDataAuthChecker 为 config-gated 默认关闭（getFilter→null），运行期延迟注入零语义影响。hr 237/237 绿 ×2 实证。
- successor：其他域（md/notify/aps/b2b/contract/fin/ast/cs…凡依赖 common-service）测试再现本环时按同款 delta 处理（或平台修复 nopDaoProvider/ormTemplate 豁免后统一移除全部兼容层 delta——归 M4.1 复核）。
