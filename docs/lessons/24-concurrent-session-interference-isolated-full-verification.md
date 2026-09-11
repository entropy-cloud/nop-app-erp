# Lesson 24: 同机姊妹会话并发干扰下的全量验证——隔离鉴别 + `git clone` 钉住 HEAD 隔离重跑

> **来源**：2026-09-02 并发构建事故处置（`docs/logs/2026/09-02.md`）+ ai-check-r3 MI.9 收官验证（2026-09-08，plan `2026-09-07-1715-3` + 执行目录 `mi9-closure-verification.md`）；落盘 plan `2026-09-11-1425-1` MG.1。
> **适用场景**：同机多 agent 会话并发编辑 + 并发 mvn 构建环境下的全量验证（known-good-baselines 登记、收官/闭包审计实跑、CI 等价门控复核）。
> **失败模式**：姊妹会话的在制编辑与构建负载使全量验证产出**伪失败**（环境属性被误判为 HEAD 回归），或共享 `~/.m2` 被姊妹会话并发 install 污染（typo 态/混态 artifact）使后续验证整体失真。

## 核心论点

全量验证结论的有效性前提 = 验证对象是**单一确定的树状态**。并发会话窗口内，"工作树 + 本地仓"不再是受控输入：在制编辑混入编译面、并发 install 污染共享 artifact 池。隔离重跑（clone 钉住 HEAD）把「环境属性 vs HEAD 属性」从猜测变成可判别实验——隔离复跑绿 = 环境属性，隔离复跑仍红 = 真回归。

## 两起独立实录

- **2026-09-02（共享 m2 污染）**：并发 nop-stream mission-driver 于 14:48 经共享 `~/.m2` 安装 typo 态 `nop-dao`（`SnowflakeSequenceGeneator`），与旧 `nop-sys-dao`（常量池引用修复拼写 `SnowflakeSequenceGenerator`）形成混态，致本仓 runner 启动 `ClassNotFoundException`——处置 = 自 nop-entropy-master 工作树 `mvn install -DskipTests -pl nop-sys/nop-sys-dao` 重建（纯构建动作零代码变更）恢复一致态后正常装载。
- **2026-09-08 MI.9（并发编辑+构建伪失败）**：姊妹会话（plan `2026-09-07-2200-1` StateMachine 重构，151 文件在制）在同工作树并发 mvn 构建 + 源码编辑，致首轮 4 次全量实跑伪失败（hr[117] ClassNotFound / md[17] getResource null / fin 13 err / assets 39 断言）——涉事模块单模块/切片隔离复跑全绿证明非 HEAD 属性（与 09-02 在案并发构建事故同型）；处置 = `git clone` 钉住 HEAD `6c495dc2e`（0 脏文件）隔离重跑全量序列全绿（33:00 min，姊妹负载下时长偏高不影响结论）；姊妹 ASCII ErrorCode 键置换对双 checker 计数无影响经真实树复证。证据固化 `mi9-closure-verification.md` + known-good-baselines MI 终态行 Git State 披露。

## 协议要素

1. **伪失败鉴别在先**：涉事模块单模块/切片隔离复跑——隔离复跑绿 = 并发干扰（环境属性），非 HEAD 回归；禁止直接对伪失败开修复。
2. **隔离重跑**：`git clone` 本仓钉住目标 HEAD（0 脏文件断言），全量验证序列在克隆树重跑；姊妹负载致时长偏高不改变结论。
3. **干扰面预披露**：Git State 登记姊妹在制编辑面（known-good-baselines 惯例），验证结论的边界条件显式化，不留"幽灵干净树"假象。
4. **计数对账兜底**：聚合测试数 / checker 计数与最近基线行逐值对账（41 Results 块聚合口径），异常差值须归因后方可登记。

## 与既有 lesson 的划界

| Lesson | 划界 |
|---|---|
| Lesson 05（失败诊断日志优先） | 05 讲单点失败的服务端归因路径；本课讲全量验证的**环境完整性鉴别**——隔离复跑是「环境 vs HEAD」判别器，二者互补 |
| Lesson 22（收官断言全表） | 22 讲断言**范围**纪律；本课讲断言**运行环境**的纯净性——全表断言跑在被污染的环境上同样产出假绿/假红 |

## 自检清单（并发会话窗口内跑全量验证前）

- [ ] 同机是否有姊妹 agent 会话在制编辑/构建？Git State 是否披露干扰面？
- [ ] 首轮伪失败是否经涉事模块隔离复跑鉴别后才下结论（而非直接开修复）？
- [ ] 高风险收官验证是否 `git clone` 钉 HEAD 隔离重跑（0 脏文件断言）？
- [ ] 共享 `~/.m2` 是否可能被姊妹会话并发 install 污染（混态/typo 态 artifact）？

## 关联

- 证据：`docs/logs/2026/09-02.md`（并发构建事故）+ `docs/logs/2026/09-08.md`（MI.9 隔离处置）+ `docs/audits/check/2026-09-06-1645-ai-check-r3/mi9-closure-verification.md` + `docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行 Git State 披露
- 关联 lesson：05（日志优先诊断）/ 22（断言范围纪律）
