# 冻结时钟毫秒线顶掉 autotest TestClock → 快照跨实体 @var 合并回放漂移（C16 实证）

> 登记日期：2026-08-25。发现：integration-test mission 收尾审计（app-erp-all 全套件复跑 C16 失败）。
> 状态：已修复（module-common-test 锚定仿真时钟升级，见「修复」节）。

## 症状

`mvn test -pl app-erp-all` 偶发失败：

```
TestErpC16CsSlaNotification.testCsSlaNotificationClosedLoop:168
errorCode=nop.err.autotest.check-output-fail params={fileName=8_survey_submit_response.json5}
nop.err.match.not-equals-var-value jsonPath=data.createTime
value=2026-08-25 21:05:51.24  varName=ErpSysNotification@createTime_1  varValue=2026-08-25 21:05:51.241
```

录制时 survey 与 notification 的 `createTime` 被快照自动变量机制合并为同一跨实体 `@var`
（值相等才会合并），回放时两行插入相差 1ms 即校验失败。

## 根因链

1. autotest 默认 `useTestClock=true`（`NopTestConfig.java:68`）注入平台 `TestClock`
   ——**严格单调、永不重复**（同毫秒内 `lastTime++`），本不会出现同毫秒相等。
2. 带 `@RegisterExtension` 冻结扩展的测试类中，扩展的 `beforeAll` 晚于
   `NopJunitExtension.beforeAll` 执行，`ThreadLocalFrozenClock.ensureRegistered()` 把全局槽
   **顶掉为裸委托墙钟**（旧实现 `currentTimeMillis() → defaultClock()`）——「永不重复」保证丢失。
3. 裸墙钟下同毫秒相等（录制期合并）与 tick 边界劈开（回放期 1ms 漂移）双向皆可能。
4. 次生不一致：`currentDate()` 返回冻结七月而 ORM `createTime` 为真实八月，同一行双纪元。

## 触发面

仅带冻结时钟扩展的测试类（16 个 `*FrozenClockExtension` 子类消费方 + app-erp-all 集成套件
C07/C09/C11-C17/B9/B10 等）；无冻结扩展的类保持平台 TestClock 保证不受影响。

## 修复（2026-08-25）

`ThreadLocalFrozenClock` 升级**锚定仿真语义**（faketime 模型）：

- `install(d)` 记录 `(simBase=d@00:00, realBase=安装时真实墙钟)`；
  `currentTimeMillis() = max(sim, lastReturned+1)`，其中 `sim = simBase + (真实now − realBase)`
  ——时钟拨回参考时刻随真实时间前进 + 严格单调永不重复（平台 TestClock 同构）。
- `currentDate()/currentDateTime()` 改由同一条仿真毫秒线派生——消除双源不一致；
  elapsed ≪ 24h，派生日期恒等于参考日，存量快照字面日期列零影响。
- 证明测试：`TestThreadLocalFrozenClockAnchoredSim`（毫秒线携带冻结纪元 / 万次连读严格递增 /
  clear 后透传真实时钟）；并行隔离证明 `TestThreadLocalFrozenClockParallel` 复跑绿。

## 复发排查入口

- 快照报 `not-equals-var-value` 且 var 名跨实体（响应宿主 ≠ var 实体）→ 本模式回归。
- grep 录制产物：响应文件中出现非宿主动作实体的 `@var:Xxx@createTime(_N)` 即坏合并残留。

## successor（已完成，2026-08-25）

- 平台侧上游化落地：nop-entropy `TestClock` 增加锚定仿真能力（`installAnchor/clearAnchor/isAnchorActive`，
  静态 AtomicReference 锚点 + 毫秒线 faketime + lastTime++ 防重；日期方法派生自同一线故自动一致）。
  dual-agent 双批准（A：APPROVE 0B/0M/4m；B：APPROVE 0B/2M/2m，findings 全部落实——暖实例不回拨
  契约 javadoc + pinning 测试、跨类持久生命周期义务、clear 不回拨、翻日注记）。验证
  `TestTestClockAnchoredSim` 5/0/0；无锚点路径与原实现逐字节等价。记录于 nop-entropy
  `ai-dev/logs/2026-08/2026-08-25.md`。
- 边界说明：应用层 `ThreadLocalFrozenClock` 保持线程本地锚点变体不变（并行保险形态，Q6 决策兼容），
  未改接平台静态锚点——两实现语义同构、适用面不同（fork 内多线程隔离 vs 单一全局时间线）。
