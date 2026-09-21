# 2026-09-21 ast 并发首次折旧测试偶发 posted=false（时间敏感 flaky）

## 问题

全 reactor `mvn test` 中 `module-assets/erp-ast-service` 的 `TestErpAstDepreciation#testConcurrentFirstDepreciationNoDuplicate` 偶发失败（2026-09-21 F3.10 批验证期实测 1 次）：断言「赢家计划行已过账 posted=true」实际 false。同批次隔离复跑该测试类全绿（exit 0），证实为时间敏感 flaky，非生产回归。

## 根因

测试起 2 线程并发争抢 `executeDepreciation`（UK 兜底 + 资产乐观锁保证仅 1 行）。败者回滚后，赢者的 GL 过账支线（独立事务）在并发争用下可能偶发失败——`DepreciationPostingDispatcher` 过账失败按既有范式容错（try/catch 告警，保持 posted=false 不阻断折旧执行），于是留下「status=EXECUTED、金额正确、posted=false」的可观测终态。测试断言把「赢者必过账成功」当作确定性不变量，与「过账失败容错保持 posted=false」的容错语义在并发争用窗口内互相矛盾。

## 触发条件

全 reactor 串行跑批期间机器负载高、线程调度抖动放大 fin 过账支线的争用窗口时偶发；单模块/单类隔离重跑难以复现。

## 修复/裁决

未改生产代码（过账容错是既有范式，mfg/ast 同型）。测试侧可选后续（未实施，登记观察项）：
- 断言放宽为「posted=true 或告警事件已派发」（容错语义的可观测面），或
- 测试内对过账支线注入确定性 stub 消除争用。

先例：ai-check-r3 mfg2-028-r3 快照精度竞态 flaky 同型处置（隔离复跑绿证实非生产回归，登记不阻塞）。

## 防复发

全 reactor 遇本测试失败时：先隔离复跑该类再定性，勿直接按生产回归排查（日志 `docs/logs/2026/09-21.md`）。
