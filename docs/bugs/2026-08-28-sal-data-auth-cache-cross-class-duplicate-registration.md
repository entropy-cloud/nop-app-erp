# sal 行级过滤器两测试类各自 new ErpRoleDataAuthChecker → data-auth-cache 跨类重复注册（顺序敏感 flake）

> 登记日期：2026-08-28。发现：mission erp-enhancement VERIFY 全量 `mvn test`（fast-fail 于 erp-sal-service，2 errors）。
> 状态：已修复（两类补 `@AfterAll` 类级注销，见「修复」节）。

## 症状

全 reactor `mvn test` 在 `module-sales/erp-sal-service` 失败（单独跑该模块或单独跑两类均绿）：

```
TestErpRoleRowFilterIsolation.testRoleRowFilterIsolatesByCreator / testAdminSeesAllAndUserFallbackNoShadow
NopException errorCode=nop.err.commons.cache.duplicate-registration params={cacheName=data-auth-cache}
  at GlobalCacheRegistry.register
  at DefaultDataAuthChecker.lazyInit
  at ErpRoleDataAuthChecker.delegate → getFilter
```

## 根因链

1. `ErpRoleDataAuthChecker#delegate()` 惰性 new `DefaultDataAuthChecker` 并 `lazyInit()`——向 **JVM 全局** `GlobalCacheRegistry` 注册名为 `data-auth-cache` 的缓存；`register` 对同名缓存抛 `ERR_CACHE_DUPLICATE_REGISTRATION`（nop-entropy `GlobalCacheRegistry.java:65-69`），不幂等。
2. erp-sal-service 测试 JVM（单 fork 串行）内有两个类各自持有 `private static ErpRoleDataAuthChecker SHARED_CHECKER`：`TestErpRoleRowFilterIsolation`（R3.4 引入）与 `dashboard/TestErpSalDashboardRowFilterCoverage`（E3.1b 引入，复制前者的共享单例模式）。per-class static 只在类内去重，跨类不去重。
3. surefire 默认 `runOrder=filesystem`，类执行顺序不保证。当 Dashboard 类先运行时，其 checker 注册 `data-auth-cache` 且测试结束不注销 → Isolation 类新建 checker → 重复注册 → 两方法在首个 checker-enabled 查询处报错。2026-08-27 22:50 全量（3925 全绿）与本失败运行同为 HEAD 附近代码，仅类顺序不同——纯顺序敏感 flake，非代码回归。
4. mnt/qa 各自只有单个此类测试类（同 JVM 无第二注册方），不受影响。

## 触发面

仅 erp-sal-service 全模块测试（或任何使两测试类同 JVM 且 Dashboard 类先行的运行方式）；全 reactor `mvn test`、`mvn test -pl module-sales/erp-sal-service` 均为潜在触发场景。

## 修复（2026-08-28）

两测试类各补 `@AfterAll static destroySharedChecker()`：类级调用 `SHARED_CHECKER.destroy()`（内部 `delegate.destroy()` → `GlobalCacheRegistry.unregister`，delegate 未创建时 no-op）并置空 static。每类生命周期内「注册一次、类末注销一次」，跨类顺序任意组合均只存在至多一个注册。

验证：定向双类同 JVM 4/4 绿；`mvn test -pl module-sales/erp-sal-service` 315/315 绿（复现原失败顺序 Dashboard→Isolation）；全 reactor `mvn test -rf :app-erp-sales-service` BUILD SUCCESS，surefire 聚合 665 reports / 3925 tests / 0 failures / 0 errors / 1 skipped。
