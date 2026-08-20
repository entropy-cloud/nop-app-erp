# 2026-08-20-2052-1-inv-landed-cost-mysql-rr-toctou-remediation 库存到岸成本 MySQL-RR 防重复分摊修复与 mission 记录回填

> Plan Status: completed
> Last Reviewed: 2026-08-20
> Source: docs/audits/2026-08-17-2125-multi-audit-requirement-compliance.md F1（P1）+ docs/audits/2026-08-17-2125-open-audit-requirement-compliance.md F1（P1，复用 P1-RC-092）
> Related: docs/plans/2026-08-06-1517-2-rc-ma4-a4-1-17-landed-cost-select-for-update-cross-db-lock-behavior.md（Blocker 修正先例）
> Audit: required

## Current Baseline

- **缺陷本体（P1-RC-092，arm-index :298）**：`ErpInvLandedCostProcessor.lockReceiveForAllocation`（`module-inventory/erp-inv-service/.../processor/ErpInvLandedCostProcessor.java:393-395`，`ormTemplate.lock(receive)` 锁 `erp_pur_receive` 行）与 `validateNotAlreadyAllocated`（`:397-412`，`dao.findAllByQuery` 对 `erp_inv_landed_cost` 表**非锁 SELECT**，过滤 `receiveId + approveStatus=APPROVED`，抛 `ERR_LANDED_COST_ALREADY_ALLOCATED`）。MySQL InnoDB REPEATABLE_READ（MySQL 默认）下事务 MVCC 读视图固定于首次非锁读（`ErpInvLandedCostApproveProcessor:42 requireLandedCost / :66 loadReceive`），不随 `:68 lockReceiveForAllocation` 锁获取刷新 → 并发事务 B 获 A 释放的 receive 行锁后，check 读陈旧快照看不到 A 已提交的 APPROVED sibling → 重复分摊（StockBalance 双计成本调整，TOCTOU）。H2（默认 READ_COMMITTED）/ PG / MySQL-RC 下行为正确——当前全绿验证基线均为 H2，掩蔽退化。
- **闭包失真**：roadmap `docs/backlog/requirement-compliance-roadmap.md:439` RC-R1.47 行以「已实现确认（2026-08-12 ORM 核实 C 类）」标 done ✅，仅重述 finding 已承认存在的锁 + check 两机制，未触及「check 非锁读陈旧快照」核心主张；「UK 非必需」只回应三修复选项之③，①②未处置，无裁决记录。Blocker 修正先例（plan `2026-08-06-1517-2` 结束审计）已证伪「锁 + check 足够」论断（MySQL-RR 退化，非跨方言一致成立）。
- **arm-index 状态未回填**：`docs/audits/arm-index.md:298` P1-RC-092「修复状态」列仍为 `todo（修复方向 MR1 评估…）`，违反 methodology §11.3 四点回写与 lesson 11。
- **arm-index P1-RC-015 标记非规范（P2，随本批次捆绑）**：`arm-index.md:154` 行尾缺「【修复状态：done（RC-R1.8…）】」规范格式（修复内容完整，仅格式与 85 条兄弟行不一致）——两份审计处置建议一致：「随 F1 的 arm-index 回填批次一并规范化，不单独立项」。
- **三修复选项零落地（HEAD `ac92d3091` 实仓复核）**：①全部生产 `application.yaml` 零 `transactionIsolation`；②check 仍非锁读；③ORM 仅 `UK_INV_LANDED_COST_CODE_ORG(code, orgId)`，无 `(receiveId, approveStatus)` UK。
- **测试基线**：`TestErpInvLandedCostReceiveMutex`（`module-inventory/erp-inv-service/src/test/.../processor/`）仅证锁串行化（maxOverlap≤1）与 version 守恒，不证 check 见并发提交（H2-RC 掩蔽 RR 退化）；`erp-inv-service` 现有测试全绿。

## Goals

- 实际处置 MySQL-RR TOCTOU：实现三选项之一或组合（Phase 1 裁决），使防重复分摊守卫跨隔离级别（H2-RC / PG-RC / MySQL-RR / MySQL-RC）一致有效。
- mission 记录归真：roadmap RC-R1.47 行闭包理由改写为真实裁决（引用本 plan + 实现证据）；arm-index P1-RC-092 修复状态回填 done。
- arm-index P1-RC-015 标记规范化（P2 捆绑项，来源：multi F5 / open F3）。

## Non-Goals

- 不改动到岸成本业务逻辑本身（分摊行计算、`CostAdjustmentService.applyCostAdjust`、过账链路零改动——仅并发守卫）。
- 不重开 P1-MA2-085（resolved R1.28，R1.28 修复在 H2/PG/MySQL-RC 有效，维持不动）。
- 不处理其余 P2 finding（cs view 调色板 / R7 守卫盲区 / 银行对账红冲告警）——已登记 Follow-up Backlog（roadmap）。
- 不做 MySQL 生产环境实际部署验证（仓内无 MySQL 集成测试基线，跨方言论证以静态推理 + 平台方言机制为证据，对齐 A4.1.17 先例）。

## Task Route

- Type: `bug investigation` → `implementation-only change`（已确认实时缺陷的修复）
- Owner Docs: `docs/design/finance/costing-methods.md`（LandedCost 防重复分摊语义，roadmap RC-R1.47 行引用）、`docs/backlog/requirement-compliance-roadmap.md`、`docs/audits/arm-index.md`
- Skill Selection Basis: Phase 2 触及 Processor 并发守卫与错误码语义 → `nop-backend-dev`（BizModel/Processor/错误处理/事务边界反模式自检）；测试编写 → `nop-testing`（JunitAutoTestCase 基类与断言式先例）。Phase 1/3 为裁决与文档回填，`Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 测试基线；若 Phase 1 裁决含选项③则 ORM UK 经 `mvn clean install -DskipTests` 增量重生成 + 三方言 deploy 种子，无数据迁移——纯加性索引）。

## Execution Plan

### Phase 1 - 修复选项裁决 + 双独立子 agent 批准（保护区域：锁/数据安全类）

Status: completed
Targets: `docs/plans/2026-08-20-2052-1-inv-landed-cost-mysql-rr-toctou-remediation.md`（裁决记录落本节）
Skill: none

- Item Types: `Decision`
- Prereqs: 无（本 plan 转 active 后即可执行）

- [x] Decision: 三选项裁决并记录（选择、替代方案、残留风险），约束条件：
  - 跨方言一致（H2 / PostgreSQL / MySQL，含 MySQL 默认 RR）；
  - 多 DRAFT 合法性保持（approveStatus 是状态字段非自然键，同 receive 多张 DRAFT/REJECTED 单合法——直接 `(receiveId, approveStatus)` UK 与此冲突，选项③若采用须为变形：如 approve 时点写入的 allocation-marker 唯一行 / 可空 `allocatedReceiveId` 唯一列）；
  - 平台机制内实现（`IOrmTemplate` 锁设施 / ORM 模型驱动，不手写绕过平台的 SQL 层）；
  - 无数据删除/迁移（纯加性）。
  - 裁决建议（预研，供 Reviewer 批准或推翻）：**主选②**——`validateNotAlreadyAllocated` 改为对 sibling 行锁定读（锁定读见最新已提交版本，跨隔离级别有效；空结果集在 MySQL-RR 下取 gap lock，但两并发 approve 事务已被 receive 行锁串行化，时序不重叠，死锁面不扩大——短事务 + 锁序 receive→sibling 单向）；**次选③变形**（allocation-marker 唯一行，DB 级兜底跨隔离级别有效，但触 ORM 结构变更保护区域）；**①仅作部署加固补充**不能独任（只护及配置了 READ-COMMITTED 的部署，不修复默认 RR 部署，且依赖运维纪律）。
  - Skill: none
- [x] 双独立子 agent 批准（保护区域：锁/数据安全类，§5 门控）：Reviewer A（fresh session，按 plan-audit 审查裁决与设计证据）— task id + APPROVE 结论落盘本行。审查必含 sibling 查询访问路径核验：`erp_inv_landed_cost` 上 receiveId/approveStatus 的既有索引（现仅非唯一 `IDX` on receiveId）能否支撑锁定读、空结果集 gap-lock 范围与锁足迹是否可接受
  - **APPROVE**（`ses_fe0891ac0ffeI8QTZYc74vcivj`，2026-08-20，fresh session）：缺陷现实/平台 lock+internalAssemble 语义/map 投影无装配+逻辑删除过滤/approve 无 INSERT/索引与锁足迹（PK 记录锁严格小于 filtered FOR UPDATE next-key 足迹）/多 DRAFT 合法性/错误契约/被否替代方案（QueryBean 零查询级锁原语实证）全 CONFIRMED；附 3 项 Decision Record 文本修订（§3 PK 集完备前提重述为充分条件、残留风险 (a) 重定界、§5 死锁声明限定）——已全部折入上文 §3/§5/§6。
  - Skill: none
- [x] 双独立子 agent 批准（保护区域：锁/数据安全类，§5 门控）：Reviewer B（fresh session，独立复核同一证据并确认无遗漏风险）— task id + APPROVE 结论落盘本行。复核范围同 Reviewer A（含 sibling 查询访问路径）
  - **APPROVE**（`ses_fe088d9f1ffeiuQCKnbnM2d50F`，2026-08-20，fresh session）：PROXY vs MANAGED 装配语义（②-assemble 否决决定性依据成立）/无查询级锁/map 投影+delVersion 过滤/(id,receiveId) 不可变 + 仅评估锁后易变列/五场景语义保持/fail-safe 全确认；补充 2 项遗漏风险——A1 approve∥reverse 理论 AB-BA 环（InnoDB 自动检测，接受+记录，已折入 §5/§6(e)）、A2 PROXY 预锁属性访问纪律（已折入 §6(a2) + Phase 2 Fix 项）。
  - Skill: none

Exit Criteria:

- [x] 裁决记录完整（选择 + 全部被否替代方案理由 + 残留风险），Reviewer A / B 双 APPROVE 落盘（任一未通过则修订后重审，不得实施）

#### Phase 1 Decision Record（2026-08-20，执行者预研落盘，Reviewer A/B 修订已折入）

**裁决：主选②（锁定读），实装形态 = 「ID 一致读发现 + PROXY 锁定读 + 锁后评估」（②-proxy-lock）。**

**1. 选择（实装设计）**：`validateNotAlreadyAllocated(receiveId, currentLandedCostId)` 重写为两段，错误码 `ERR_LANDED_COST_ALREADY_ALLOCATED` + ARG_RECEIVE_ID/ARG_LANDED_COST_CODE 参数语义与 currentLandedCostId 排除逻辑不变，仅将 sibling 可见性判定从「一致读 SQL membership」改为「锁定读值评估」：

- 段 1 ID 发现（一致读，非锁）：`ormTemplate.findListByQuery(QueryBean)` map 投影——`sourceName=app.erp.inv.dao.entity.ErpInvLandedCost`，`fields=[id]`，filter `receiveId=?`，`orderBy id asc`（确定性锁序）。EQL 实体源自动附加 `delVersion` 逻辑删除过滤（`EqlTransformVisitor.addEntityFilter`，与既有 dao 查询语义一致）；map 行不装配 session 实体。
- 段 2 逐 sibling 锁定读（跳过 currentLandedCostId）：`ormTemplate.load(entityName, id)`（session 未装载 → PROXY，不触库）→ 若该实例已被本 session 早前装载且非 dirty，`session.unload` 重置为 PROXY（消除 session 级陈旧态）→ `ormTemplate.lock(sibling)`（平台 `GenSqlHelper.genLockSql` PESSIMISTIC_WRITE，`SELECT <eager fields> WHERE id=? FOR UPDATE`）→ 锁定读返回**最新已提交**行值并经 `OrmSessionImpl.internalAssemble` 对非 MANAGED（PROXY）实体装配全部字段 → 锁后评估 `approveStatus==APPROVED`，命中即抛错。

**2. 平台机制核验（nop-entropy 源码，@HEAD）**：`IOrmTemplate.lock` → `OrmSessionImpl.lock:588-614` → `EntityPersisterImpl.lock:246` → `JdbcEntityPersistDriver.lock:145-165`（executeQuery 后 `session.internalAssemble(entity, values, propIds)` 装配锁读行值）。`OrmEntityState.PROXY.isAllowLoad()=true` 可入锁。关键约束：`internalAssemble` 对 **MANAGED** 实体不覆写已初始化属性（session 范围一致性，`OrmSessionImpl:365-390`「如果已经设置过，则不再更新」）→ 必须以 PROXY（或 unload 重置）形态进入 lock 才能取到最新值；QueryBean/EQL 无查询级锁原语，`lock(entity)`（PK 等值 FOR UPDATE）是平台内唯一锁定读原语。

**3. 跨方言有效性论证**：MySQL-RR（缺陷方言）下 ID 发现虽为一致读（read view 固定于事务首读），但 `(id, receiveId)` 创建后不可变、守卫仅评估锁后读取的易变列 `approveStatus`，故只要**没有同 receiveId 的 `erp_inv_landed_cost` INSERT 在「守卫事务 read view 固定 → ID 发现」窗口内提交**，read view 内 receiveId 的 PK 集即完备（Reviewer A 修订：此前提是充分条件而非「approve 事务不 INSERT」的推论——创建路径与 approve 无锁串行化：generate 路径不取 receive 锁、`validateNoDraftExists:234-247` 亦为非锁读、CrudBizModel 通用 save 亦是潜在创建路径；缓解 = 该窗口为毫秒级事务内窗口 + 新单必然 DRAFT/UNSUBMITTED 态 + 创建后仍需先获 receive 锁才能 approve，双事务并发 approve 时序上仍被串行化）。PK 等值 FOR UPDATE = 锁定读最新已提交版本（MySQL 8.0 Ref Manual §15.7.2.4）→ 事务 B 必见事务 A 已提交的 APPROVED sibling → 抛错。守卫跨 H2-RC / PG-RC / MySQL-RR / MySQL-RC 一致有效。

**4. 被否替代方案**：

- **①部署侧 MySQL 强制 READ_COMMITTED**：仅护及显式配置 RC 的部署，不修复默认 RR 部署，依赖运维纪律——仅可作部署加固补充，不能独任，不采纳（维持 Current Baseline「①零落地」现状，不新增配置）。
- **③变形（allocation-marker 唯一行 / 可空 allocatedReceiveId UK 列）**：DB 级兜底跨隔离级别有效，但触 ORM 结构变更保护区域（新增列/UK + 三方言 deploy 种子 + 增量重生成），变更面大于②；且 reverse 红冲翻转 approveStatus 后 marker 行须同步删除/失效（否则反向后无法二次分摊），生命周期复杂化。②-proxy-lock 在平台原语内达成同等跨方言有效性，零结构变更。③留作②审查被拒时的后备。
- **②-naive（filtered `WHERE receiveId=? AND approveStatus='APPROVED' FOR UPDATE` 查询级锁定读，即预研原文形态）**：平台 QueryBean/EQL 无查询级锁原语，无法满足「平台机制内实现」约束；手写 SQL 绕过平台违反约束。且空结果集在 MySQL-RR 下于 receiveId 非唯一索引取 next-key gap lock（锁足迹 > PK 锁）。
- **②-assemble（`dao.findAllByQuery` 装配实体后逐行 `lock`）**：`internalAssemble` 不覆写 MANAGED 已初始化属性 → 锁后读值仍是装配时陈旧值，**静默失效**（锁有效但读值不刷新），等价未修复。否决——此为②实装形态选择的决定性技术依据。

**5. sibling 查询访问路径核验**（Reviewer A/B 必查项）：

- 索引支撑：ID 发现走既有非唯一索引 `IDX_INV_LANDED_COST_RECEIVE_ID`（orm.xml:1356，receiveId 等值）；sibling 集 ≤ 个位数（`validateNoDraftExists` 保证同 receive 至多 1 张非 CANCELLED 单，历史 REJECTED/CANCELLED 少量）。锁 = PK 等值（唯一索引命中已存在行）。
- gap-lock 范围与锁足迹：PK 等值锁定读对已存在行只取记录锁（InnoDB 唯一索引等值命中已存在行 → record lock only，无 gap/next-key）——锁足迹**严格小于**预研 filtered FOR UPDATE 形态（后者空结果集在 receiveId 索引取 gap lock）。
- 锁序/死锁面：锁序 receive 行 → sibling PK（id asc 单向）；两并发 approve 事务已被 receive 行锁串行化，sibling 锁窗口时序不重叠；reverseApprove 更新 sibling 行但不取 receive 锁。**死锁声明限定（Reviewer A 修订 3 + Reviewer B A1）**：approve(B) 现先取 LC-A sibling 记录锁、后经 `createAndApplyCostAdjust:334-335` flush 取 StockBalance 行锁，而 reverse(R) 先取 StockBalance 锁（`doReverseApprove` 步骤 2 flush）、最后更新 LC 头（`:185-191`）——并发 approve(LC-B) ∥ reverse(LC-A) 同 receive 时存在 {LC-A 行, 共享 StockBalance 行} AB-BA 环的**理论**可能；该环被 InnoDB 死锁检测自动回滚一个牺牲者（瞬时、可重试、无守卫绕过/无数据破坏），且成环需叠加既有 double-reverse 竞态、由乐观锁 version + InnoDB 检测兜底。裁决：接受 + 记录（不新增锁序干预——干预需 reverse 侧也取 receive 锁，变更面大于收益）；锁持有时长 = 守卫到事务结束（approve 短事务）。

**6. 残留风险**：

- (a) ID 发现盲区（Reviewer A 修订 2 重定界）：盲区 = **任何**在守卫事务 read view 固定之后提交的同 receiveId sibling INSERT（不限于「同事务 insert+approve」）。缓解 = 毫秒级事务内窗口 + receive 锁串行化（新单 approve 仍须先获 receive 锁）+ 新单创建后首态必为 DRAFT/UNSUBMITTED；watch 项：`validateNoDraftExists:234-247` 本身为非锁读（generate 侧并发双建 DRAFT 的既有竞态，非本守卫范围，登记 Phase 3 owner doc 注记提及）。
- (a2) PROXY 预锁属性访问纪律（Reviewer B A2）：`load()` 与 `lock()` 之间**禁止**访问 sibling 任何属性（如 debug 日志 `getCode()`）——PROXY 属性访问触发惰性一致读装载 → 属性初始化 → lock 不再刷新 → 陈旧静默复归。Phase 2 实现与测试强制遵守（锁前仅用已发现的 id 值）。
- (b) dirty sibling：不做 unload（dirty 实体 unload 会静默丢弃未提交修改）→ 交由 `lock` 抛 `ERR_ORM_NOT_ALLOW_LOCK_DIRTY_ENTITY` 显式失败（fail-safe）；当前无调用方在守卫前 dirty 化 sibling。
- (c) 并发物理删除 sibling：lock SELECT 无行 → `ERR_ORM_LOCK_ENTITY_FAIL` 显式失败（fail-safe）；实体逻辑删除（useLogicalDelete），ID 发现已过滤 delVersion>0，物理删除非运行时路径。
- (d) H2 测试边界：仓内 H2 测试基线 READ_COMMITTED，无法直接复现 RR 快照退化；跨方言证据 = MySQL 手册 locking-read 语义 + 平台源码路径静态论证（对齐 A4.1.17 证据范式），测试侧以顺序化 stale-window 序列 + session 陈旧态模拟补线（见 Phase 2）。
- (e) approve ∥ reverse 理论 AB-BA 环（Reviewer B A1，见 §5）：InnoDB 自动检测回滚牺牲者，瞬时可重试，接受 + 记录。

### Phase 2 - 守卫实现 + 回归测试（按 Phase 1 裁决执行）

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvLandedCostProcessor.java`（+ 裁决涉及的 ORM/model 或 config 文件）
Skill: nop-backend-dev

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1 双批准完成 ✅（Reviewer A `ses_fe0891ac0ffeI8QTZYc74vcivj` + Reviewer B `ses_fe088d9f1ffeiuQCKnbnM2d50F` 双 APPROVE；实现纪律：锁前禁访问 sibling PROXY 属性——Reviewer B A2）

- [x] Fix: 按裁决实现防重复分摊守卫（主选② = sibling 锁定读替换非锁 `findAllByQuery`，保持 `ERR_LANDED_COST_ALREADY_ALLOCATED` 错误码与 ARG_RECEIVE_ID/ARG_LANDED_COST_CODE 参数语义、排除 currentLandedCostId 逻辑不变；若③变形被裁准，同步走 ORM 增量重生成）
  - 落地：`ErpInvLandedCostProcessor.validateNotAlreadyAllocated` 按 Phase 1 Decision Record ②-proxy-lock 重写（ID 一致读 map 投影发现 + PROXY `ormTemplate.load` + 非 dirty `session.unload` 重置 + `ormTemplate.lock` PK 锁定读 + 锁后评估 approveStatus；id 升序确定性锁序；锁前零属性访问纪律遵守）。零 ORM/config 变更。
  - Skill: nop-backend-dev
- [x] Add: 新增回归测试证明守卫对「并发提交的 APPROVED sibling」可见（顺序化序列模拟 stale-snapshot 窗口：事务 A 提交 APPROVED 后事务 B 进入守卫必抛；测试基线说明：仓内 H2 测试默认 READ_COMMITTED 配置下无法直接复现 RR 快照退化（H2 MVStore 本身支持 RR 隔离级别参数化，如实现侧可低成本以隔离级别参数化复现则优先实测，否则以锁定读语义静态论证补线注记——对齐 A4.1.17 证据范式，论证落测试类 javadoc/计划 Closure））；`TestErpInvLandedCostReceiveMutex` 既有断言不回归
  - 落地：新增 `TestErpInvLandedCostAllocatedGuard`（processor 包，3 组）——①`testGuardSeesCommittedApprovedSibling`（顺序化 stale-window：A 提交 APPROVED 后 B 守卫必抛 + 错误码/ARG 参数断言）；②`testGuardLockReadDefeatsStaleSessionState`（双线程 session 陈旧态 hardening：B 预装载 DRAFT MANAGED sibling → A 提交 APPROVED → B 守卫 unload→lock 仍必抛，pin 无-unload 实现静默失效的反例）；③`testGuardPassesWithoutApprovedSibling`（REJECTED/CANCELLED sibling 放行 + currentId 排除 + 零 sibling 放行）。H2-RR 隔离级别参数化经评估非低成本（须侵入 nop-autotest 数据源设施）→ 取静态论证分支：MySQL-RR 锁定读语义论证落测试类 javadoc（MySQL 8.0 §15.7.2.3/§15.7.2.4 + 平台链路）。mutex 测试 2/2 零回归。
  - Skill: nop-testing
- [x] Proof: `mvn test -pl module-inventory/erp-inv-service` 全绿（含既有 landed-cost 族测试零回归）；若新增 checker 可计数站点（daoFor 等），按 §11.2 per-site 证据登记 baseline（预期零漂移或逐站点裁决）
  - 证据：`mvn test -pl module-inventory/erp-inv-service` → **235 tests, 0 failures, 0 errors, BUILD SUCCESS**（landed-cost 族全含：AllocatedGuard 3 / Mutex 2 / EndToEnd / Reversal / AllocationEngine / ReverseFailureAlert 零回归）；`bash docs/audits/nop-compliance-checker.sh` → 全规则 actual == baseline **零漂移**（R1d 14/14、R2a 34/34、R2b 237/237、R2c 1507/1507、R2d 38/38、R3 5/5、R10 12/12、R12a-c 70/66/41）——守卫重写未新增 daoFor 站点（validateNotAlreadyAllocated 改用 ormTemplate 设施，既有 landedCostDao() 调用点不变），无需 per-site 登记或 baseline 调整。
  - Skill: nop-testing

Exit Criteria:

- [x] 守卫实现落地且模块测试全绿（失败模式：并发 sibling 场景抛 `ERR_LANDED_COST_ALREADY_ALLOCATED`，成功模式：单事务 approve 不受影响）
- [x] 新增回归测试与既有 mutex 测试同时通过（localized 验证，解除 Phase 3 回填阻塞）

### Phase 3 - 四点回写 + P1-RC-015 标记规范化（mission 记录归真）

Status: completed
Targets: `docs/audits/arm-index.md`（:298 P1-RC-092 行、:154 P1-RC-015 行）、`docs/backlog/requirement-compliance-roadmap.md`（:439 RC-R1.47 行 + 头部 :52 C 类摘要行）、`docs/logs/2026/08-20.md`
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 2 全绿

- [x] Fix: arm-index :298 P1-RC-092「修复状态」→ `【修复状态：done（本 plan，附修复摘要 + 测试证据）】`（规范格式，对齐 P1-RC-011..014 先例）
  - 落地：行尾追加 `【修复状态：done（RC-R1.47，2026-08-20，plan docs/plans/2026-08-20-2052-1-inv-landed-cost-mysql-rr-toctou-remediation.md）】`——②-proxy-lock 修复摘要（含否决①/③/②-naive/②-assemble 理由）+ 双批准 task id + 测试证据（AllocatedGuard 3 组 + mutex 2/2 + 模块 235 全绿 + checker 零漂移 + A4.1.17 范式静态论证注记）。
  - Skill: none
- [x] Fix: arm-index :154 P1-RC-015 行尾补规范 `【修复状态：done（RC-R1.8，plan docs/plans/2026-08-08-1154-1-rc-mr1-r1-8-hr-timesheet-family.md）】` 标记（P2 捆绑项，来源 multi F5 / open F3；不改写既有修复内容文本）
  - 落地：行尾 `**【R1.0 展开归属】RC-R1.8**` 后追加 `**【修复状态：done（RC-R1.8，plan docs/plans/2026-08-08-1154-1-rc-mr1-r1-8-hr-timesheet-family.md）】**`，既有修复内容文本零改写。
  - Skill: none
- [x] Fix: roadmap :439 RC-R1.47 行闭包理由改写为真实裁决（废弃 C 类「已实现确认」措辞 → 引用本 plan id + Phase 1 裁决 + Phase 2 验证摘要；同步修订头部 :52 摘要行的 C 类残留措辞）
  - 落地：:439 行改写为「修复落地（2026-08-20，plan …）」——含闭包失真声明（原 2026-08-12 C 类确认经 multi/open F1 证伪）+ Phase 1 裁决摘要（四否决理由）+ 双批准 task id + Phase 2 验证摘要 + UK 非必需裁决维持；:52 头部摘要行同步改写（「check 改 PK 锁定读跨隔离级别有效，取代原『已实现确认 C 类』失真闭包」）。grep 复核：:439 行不再含「已实现确认（2026-08-12 ORM 核实 C 类）」（仅剩 :447 RC-R1.55 行同措辞——不同工作项，不在本 plan 范围）。
  - Skill: none

Exit Criteria:

- [x] 四点回写齐备（arm-index done / roadmap 真实裁决 / owner doc：costing-methods.md 若守卫机制语义对契约段可见则补实现注记，否则省略 / 日志条目），grep 复核 :439 行不再含「已实现确认（2026-08-12 ORM 核实 C 类）」失真措辞
  - 四点落位：①arm-index :298 done；②roadmap :439/:52 真实裁决；③owner doc 取「可见→补注记」分支——costing-methods.md §实现注记：到岸成本分摊算法 新增「防重复分摊守卫」条目（锁定读跨隔离级别语义 + 锁序 + 同事务禁 insert+approve 约束 + validateNoDraftExists 非锁读 watch 项）；④`docs/logs/2026/08-20.md` 条目在盘（含 full-green 验证状态）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（`ses_fe0c26d2affecuporPOi2Uzdef`，0 MAJOR / 4 MINOR）——覆盖性（三修复义务 + 捆绑 P2）、选项②技术论证、§5 双批准门控、指南合规均通过。4 MINOR 已修订：①ApproveProcessor 行号修正 `:37/:50/:52` → `:42/:66/:68`（2026-08-13 M4.34 重写后实况）；②Closure Gates「按需」措辞改为 Phase 3 退出标准客观条件引用；③H2-RR 测试边界改为「隔离级别参数化优先实测，否则静态论证补线注记」；④Reviewer A/B 审查范围显式含 sibling 查询访问路径（索引支撑 / gap-lock 范围 / 锁足迹）。

## Closure Gates

> 完整仓库验证在此处运行一次：`docs/context/project-context.md` 列出的构建/测试命令 + `bash docs/audits/nop-compliance-checker.sh`（actual ≤ baseline，对照 `compliance-baseline.md §BASELINE`）。`docs/logs/2026/08-20.md` 条目（时间倒序，含验证状态）为计划级结束步骤，在此完成（methodology §11.3 四点回写之④）。

- [x] 范围内行为完成（守卫跨隔离级别有效 + 测试证据）
- [x] 相关文档对齐（arm-index / roadmap；costing-methods.md 按 Phase 3 退出标准的客观条件裁决——守卫机制语义对契约段可见则补实现注记，否则省略，结束记录注明所取分支）
  - 所取分支 = **补实现注记**（「防重复分摊」为审核编排契约步骤 2，守卫机制语义对契约段可见）：costing-methods.md §实现注记：到岸成本分摊算法 新增「防重复分摊守卫」条目。
- [x] 已运行验证（全仓构建 + `mvn test -pl module-inventory/erp-inv-service` + checker 零漂移）
  - `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + 全 reactor `mvn test` BUILD SUCCESS **3792/0/0/1**（surefire XML 615 文件 = 前基线 3789 + 3 新增；唯一 skip = 已知 @Disabled ErpAllWebPagesCollectTest）+ `mvn test -pl module-inventory/erp-inv-service` **235/0/0** + `bash docs/audits/nop-compliance-checker.sh` EXIT=0 全规则 actual==baseline 零漂移（R1d 14/R2a 34/R2b 237/R2c 1507/R2d 38/R3 5/R10 12/R12a-c 70/66/41）。
- [x] `docs/logs/2026/08-20.md` 条目在盘（计划级结束步骤）
- [x] 无范围内项目降级为 deferred/follow-up（P1-RC-092 为不可降级项）
- [x] 独立草案审查已完成并记录
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
  - 执行者自查：三 Phase Status=completed + 全部执行项/退出标准 [x]；日志条目含 full-green 验证状态与本门控一致；Plan Status 待独立结束审计通过后置 completed（下方门控）。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——P1 不可降级；H2 测试边界见 Phase 2 证据范式注记，非 deferral）

## Closure

Status Note: P1-RC-092 MySQL-RR TOCTOU 修复闭环：Phase 1 裁决②-proxy-lock + 双独立子 agent 批准（Reviewer A/B 双 APPROVE，修订折入）；Phase 2 守卫落地（锁定读跨 H2-RC/PG-RC/MySQL-RR/MySQL-RC 一致有效，错误码/参数/排除语义不变，零 ORM/config 变更）+ 新增 3 组回归测试；Phase 3 四点回写齐备（arm-index P1-RC-092 done + P1-RC-015 规范化 + roadmap 真实裁决 + costing-methods.md 实现注记[取「契约段可见→补注记」分支] + 日志）。验证全绿：全仓 install 156 模块 BUILD SUCCESS + 全 reactor test 3792/0/0/1（615 XML = 基线 3789+3）+ inv 模块 235/0/0 + checker 零漂移 EXIT=0。无范围内项目降级；H2-RR 测试边界按 A4.1.17 范式静态论证（非 deferral）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（fresh session，`ses_fe05d6aa9ffeoufyDIBipgfiox`，2026-08-20）
- Evidence: **PASSES closure audit**——A 计划文本一致性（三 Phase completed + 全项 [x] + 审计门控正确留白）✓；B Phase 1 完整性（裁决 + 四否决 + 双批准 task id + 修订折入 :60/:63/:81/:94/:99）✓；C 代码正确性（git diff 核验：map 投影 ID 发现无 approveStatus SQL 过滤 + PROXY/unload/lock 时序 + 锁后评估 + 参数语义 + 无预锁属性访问 + 仅 1 生产文件 + 零 ORM/config 变更）✓；D 测试充分性（3 组测试与声明一致 + 双线程 hardening 真实 pin unload→lock + javadoc MySQL-RR 静态论证注记 + mutex 未触碰，surefire XML 证实 3+2 全跑）✓；E 验证声明（审计者复跑：inv 模块 235/0/0 BUILD SUCCESS + checker EXIT=0 全规则零漂移 + 全仓 surefire XML 聚合 615 文件 3792/0/0/1 与声明吻合）✓；F 四点回写（arm-index :298/:154 / roadmap :439/:52[失真措辞仅剩 :447 RC-R1.55 越界项合法残留] / costing-methods.md 守卫注记三要素 / 日志条目在盘）✓；G 交叉核（Deferred 无降级 + Closure pending 待裁）✓。附 1 项非阻塞 cosmetic（残留空 Exit Criteria 标题，:105）——已由执行者移除。

Follow-up:

- （无——已确认缺陷不出现在此处；其余 P2 项见 roadmap `## Follow-up Backlog`）
