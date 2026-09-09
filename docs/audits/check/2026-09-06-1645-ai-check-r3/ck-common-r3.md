# ck-common-r3 — common-service U20 五维符合性审计报告（ai-check-r3 M1.15，common 抽象族唯一归属格）

> 工作项：M1.15（六单元全格；本报告 = U20 common-service 格——**§3.2 共享代码唯一归属：common 抽象族（`AbstractProcessor.illegal*` helper、`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel` 状态锁基类、共享 config/错误码基建）归本格**，各域切片只审调用点；同时承接 fin3-017-r3/fin4-022-r3/ast-028-r3/qa-027-r3/prj-023-r3 移交的「全仓同型扫描归 U20/M1.15 裁决」两项 handoff；姊妹格见 `ck-maintenance-r3.md` / `ck-aps-r3.md` / `ck-logistics-r3.md` / `ck-notify-r3.md` / `ck-master-data-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `e331c55b2`（脏面 = 1 条 untracked 计划文件，tracked 零修改）——口径同 `ck-maintenance-r3.md` 头注。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §1/§2 + §3.3 U20 行（DIM-F/S 为显式 n-a 带理由）+ §6 勘误 E1。
> 范围：`module-common-service/src/main/java` 全量（AbstractProcessor + 六 per-mutation 骨架 + AbstractErpCrudBizModel/AbstractErpImmutableCrudBizModel/ErpCrudStatusLock + ErpCommonErrors/SoDGuard/MaskHelper/MaskAuditRecorder/StringMaskFormat/UniqueConstraintHelper/DashboardUtil/ErpOrgContext/ErpOrgIsolationQueryTransformer/ErpOrgIsolationOrmInterceptor/ErpRoleDataAuthChecker + beans 注册）+ `module-common-test`（frozen-clock 基座/FaultInjectionStubs/PerfTiming/CoreMetricsSysCalendar + TestSeqStringIdProof）。查重锚 r1 `ck-common-app.md`（C8.2）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎归 fin-1；聚合横切面归 U21（app-erp-all 菜单/action-auth 聚合器行为缺陷归 U21——r1 common-002/003 涉聚合器文件，按 r1 登记格沿用本格跟踪、机制裁决归 U21/M1.16）；notify 本体归 U11。
> 零生产代码改动：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U20 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套 + **基类逐方法走查**（`AbstractProcessor.illegal*` 期望态参数/`AbstractErpCrudBizModel`/Immutable 状态锁基类/共享 config 基建）+ 双 handoff 全仓扫描裁决 + ⑮基类契约抽样（无 owner doc，以基类契约替代） | 反模式族全零（RuntimeException=0/@Inject private=0——FaultInjectionStubs:29 仅为注释提及/System.currentTimeMillis=0/@Transactional 0/LocalDate.now=0）；checker 零漂移。**基类逐方法走查**：①`AbstractProcessor`（85 行 10 成员）：`illegalStatusException(T, current, String... expected)` **确有期望态参数**（varargs 即 ARG_EXPECTED_STATUS 载体，六骨架逐一传入）✓；`requireEntityForUpdate` 纯别名零锁语义（命名误导注记，并入 013-r3 误用面）；`defaultNotFoundException` 仅绑 bizObjId（common-004 open 原样，67 站点计数复核不变）；`currentUserId` 宽 catch（全域族 pur-010 样本）；`daoProvider` protected 字段零豁免注释（~120 子类批发旁路 handle → 并入 014-r3 全仓族 U20 自身站点）。②`AbstractErpCrudBizModel`（54 行）：config kill-switch `erp-common.crud-status-lock-enabled` 默认 TRUE + defaultPrepareUpdate/Delete 双覆盖 + `ErpCrudStatusLock.java:32-39` 反射列探测（posted/approveStatus 存在性）——**缺口：平台通用 `save` mutation 走 `defaultPrepareSave` 钩子，两基类均未覆盖**（`CrudBizModel.java:517-523/:771`），373 个生成页面暴露 `__save` upsert 通道可绕状态锁/不可变守卫 = **新立 P2-CK-common-011-r3**。③`AbstractErpImmutableCrudBizModel`（33 行）：update/delete 全拒 ✓（同 save 盲区）。④共享基建：ErpCommonErrors 4 码全 `nop.err.erp.common.*`（common-010 open）；SoDGuard fail-open 仅 null-user 语义（javadoc 登记在案）；MaskHelper fail-closed 掩码 + 授权分支审计 hook ✓；MaskAuditRecorder ThreadLocal 无生产清理调用（common-001 open，全仓 grep 复核）；StringMaskFormat/UniqueConstraintHelper/DashboardUtil（common-005/006/007/009 open 原样）；ErpOrgIsolationQueryTransformer `catch (Throwable)→false` fail-open 跳过隔离过滤器零日志 = **新立 P3-CK-common-012-r3**；`AbstractSubmitForApprovalProcessor:69` 复合期望态串 `"unsubmitted / rejected"` 违反 illegal* 契约（javadoc :82 明令传状态码）= **新立 P3-CK-common-013-r3**（计划单元焦点「illegal* helper 期望态参数」直接命中）。**双 handoff 裁决**：daoFor 无豁免注释全仓族 354 文件扫描（42 有注释/312 无，域分布见 §2.3 common-014-r3）+ `new ServiceContextImpl()` 全仓族 82 站点/61 文件（14 文件有 ctx 兜底/44 裸 new，域分布见 §2.3 common-015-r3）——两族均**系统性跨域**，登记 U20 格全仓聚合 finding，per-slice ID 维持各自范围 | **finding**（5 新立：1 P2 + 4 P3；归并 10，见 §2） |
| **DIM-F 前端页面与 E2E** | —（common 层无独立页面） | `n-a`：module-common-service/common-test 无 erp-*-web 模块、无 _vfs 页面资产（冻结清单 §3.3 U20 F 行预登记「n-a（记录理由）」）；common 贡献的横切页面行为（聚合菜单/action-auth 聚合器）归 U21/M1.16 格 | **n-a**（common 层无独立页面，判定面归 U21） |
| **DIM-S seed 数据** | —（common 层无独立 seed） | `n-a`：common 两模块无 deploy/sql 种子、无 `_init-data` CSV 贡献（冻结清单 §3.3 U20 S 行预登记）；聚合 seed 口径（372 CSV + 1 SQL）归 U21 格 | **n-a**（common 层无独立 seed，判定面归 U21） |
| **DIM-T 单元测试** | §1.4 ②③（对 module-common-test 基座）+ common-test 对全域测试的一致性支撑 | `mvn test -pl module-common-service` **23/0/0/0 全绿**（= 锚点 23 零增量，两轮同值：TestErpOrgContext 12 + TestErpCrudStatusLock 1 + TestMaskAuditRecorder 10）；RECORDING = 0。**common-test 基座一致性支撑核对**：`ThreadLocalFrozenClock`（对抗平台 NopJunitExtension afterAll 时钟重置的 ensureRegistered 重挂载，javadoc 引平台源码行）+ `AbstractFrozenClockExtension`（15 域冻结时钟扩展基类）+ `FaultInjectionStubs`（无 Mockito Proxy 桩族：throwingVoucherBiz/recordingNotificationBiz）+ `PerfTiming`（warmup-K + 中位数/p95）+ `CoreMetricsSysCalendar`（CodeRule 日期段跟冻结时钟的测试 delta bean）——全域 61 文件消费 `ServiceContextImpl` 测试/job 上下文、各域 FrozenClockExtension 均构建于 AbstractFrozenClockExtension，**基座支撑一致性 pass**；`_cases` 4 快照（TestSeqStringIdProof 字符串 id 迁移语义证明族）载体完整 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套 | `--strict` **PASS exit 0** + `--self-test` **PASS**；common 探针族 CAT-2 5/CAT-3 族维持清零零回归；WHITELIST common 条目 **2**（`ErpOrgIsolationQueryTransformer.java` E3 @Description + `MaskHelper.java` C2 ROLE_* 角色名数据契约）四要素齐备实核（两文件在位 + @Description 1 行/ROLE_ 常量族 8 命中实证 + owner doc 指针 + plan 2026-09-07-1715-1 Phase 4 裁决）；`@Locale` 缺失 = 空；meta 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-common-app.md`（C8.2）全 10 条逐一比对 + r2 只读目录 + §Mission 基线快照 + 五份姊妹 r3 报告移交 handoff（fin3-017-r3/fin4-022-r3「全仓同型扫描归 U20/M1.15 裁决」、ast-028-r3/qa-027-r3 同族、prj-023-r3「全域 ServiceContextImpl 族扫描归 U20/M1.15」）。**本轮新立 5 条（1 P2 + 4 P3）**；历史 10 ID 零覆写（r1 族最大号 CK-common-010，新立自 011 起）。

### 2.1 复用 — 0 条（10/10 open 无 fixed 同型）

### 2.2 归并（同型 open 追加证据至原 ID）— 10 条 + 1 跨格新站点

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P2-CK-common-001 | `MaskAuditRecorder.java:54` ThreadLocal 去重集 + `clearThreadDedup` L187-192 定义后**全仓生产调用零命中**（module-common-service/common-test/各域 service/app 四面 grep）原样 |
| P3-CK-common-002 | `app.action-auth.xml:38-61` erp-l10n-cn 死菜单 3 URL 在位；ErpL10nCn 全仓 grep 零命中、module-l10n-cn 目录不存在原样（机制裁决归 U21） |
| P3-CK-common-003 | 同文件 L1 头注释声称合并 job 菜单 vs L5-27 x:extends 清单无 /nop/job 原样（归 U21） |
| P3-CK-common-004 | `AbstractProcessor.java:71-74` 仅绑 ARG_BIZ_OBJ_ID；67 消费站点计数复核与 r1 分布一致（ast 30/fin 20/inv 7/qa 5/prj 4/crm 1）原样 |
| P3-CK-common-005 | `DashboardUtil.java:50-70` additionalFilters 参数零读取 + 生产零调用原样（消费 r1 9 文件→本轮 8 文件微漂移注记，finding 不受影响） |
| P3-CK-common-006 | `DashboardUtil.java:47` 废弃 `BigDecimal.ROUND_HALF_UP` 整型常量原样 |
| P3-CK-common-007 | `StringMaskFormat.java:16-19/:29-32` ID_CARD len=6 揭示 5/6、MOBILE len=8 揭示 7/8 原样 |
| P3-CK-common-008 | 骨架本体正确（`AbstractReverseApproveProcessor.java:38-42,64-66` → REJECTED，修复 4dab22be3 在位）；9 文件陈旧 javadoc 逐一确认（pur 5 + sal 4 清单复核）+ **mnt 新站点归并**：`ErpMntSparePartUsageDocumentStateMachine.java:27`「silent-guard gap 暂不强制」——mnt-001 修复（assertCanConfirm 接线）后过期（本切片 §归属标注追加，mnt 格同点注记） |
| P3-CK-common-009 | `UniqueConstraintHelper.java:27-28` 仍把 ERR_SQL_DATA_INTEGRITY_VIOLATION 判为 UK 冲突原样 |
| P3-CK-common-010 | `ErpCommonErrors.java:20/33/44/50` 四码全 `nop.err.erp.common.*` 偏离 `erp.err.<short>` 约定原样 |

### 2.3 新立 `-r3` — 5 条（1 P2 + 4 P3）

**P2-CK-common-011-r3**（DIM-B ④/D5 状态锁基类 save 通道盲区——F1.3 家族残余面）
- **控制点**：`AbstractErpCrudBizModel.java:26-41` 与 `AbstractErpImmutableCrudBizModel.java:14-24` 仅覆盖 `defaultPrepareUpdate`/`defaultPrepareDelete`；平台通用 `save` mutation（`CrudBizModel.java:517-523` @BizMutation save → `invokeDefaultPrepareSave` → 钩子 `defaultPrepareSave` :771-779）未被任一基类覆盖；upsert 语义 `ErpXxx__save` 由 **373 个生成页面**引用（如 `_ErpPurOrder.view.xml:221`）。
- **问题**：任何调用方（GraphQL 直调或页面创建对话框带 id 的 upsert 形态）可经 `__save` 写入 posted=true/APPROVED 单据或 immutable ledger 实体——F1.3 状态锁与 F1.3-immutable 不可变守卫双盲区；ck-common-app L15 修复规格（override defaultPrepareUpdate/**defaultPrepareSave**）仅实施了 2/3。mnt/aps/log/notify 各格本轮归并注记的「守卫惰性 + save 通道」升级证据均汇于此。
- **三态裁决**：新立（r1 P1-CK-pur-003 族先于基类存在；ck-common-app 仅登记钩子缺失未登记已交付守卫的 save 形态漏洞）。P2（全域守卫强回归风险；默认页面形态 save 仅创建场景触发，故非 P1）。
- **修复方向**：M2.x——两基类补 override `defaultPrepareSave`（镜像 update/delete 锁逻辑 + immutable 全拒）；补「__save 对 posted/APPROVED 实体拒绝」失败测试；与 pur-003 族 open 项统一收口。

**P3-CK-common-012-r3**（DIM-B ⑪ 组织隔离 fail-open）
- **控制点**：`ErpOrgIsolationQueryTransformer.java:87-97`——实体模型解析（Class.forName/dao 反射）包在 `catch (Throwable e) { return false; }` 中；`false` = 「实体无 orgId 列」= 跳过隔离过滤器，且零日志。
- **问题**：隔离启用（config-gated 默认 false）下，任何类加载/反射失败（重部署窗口/类加载器失配）使该实体的读侧隔离过滤器静默失效——安全相关方向 fail-open 且不可观测。
- **三态裁决**：新立（r1 ck-common-app「验证为正确」节 L126 明确验证过该类缓存/跳过行为，未登记 catch-all 面）。P3（默认关；启用即安全面）。
- **修复方向**：M2.x——区分「无 orgId 列」与「解析失败」两态，失败路径 WARN 日志 + 可选 fail-closed 配置。

**P3-CK-common-013-r3**（DIM-B ①/⑥ illegal* 契约违反——骨架自体）
- **控制点**：`AbstractSubmitForApprovalProcessor.java:69` `illegalStatusException(entity, status, unsubmittedStatus() + " / " + rejectedStatus())` vs 契约 `AbstractProcessor.java:82`（current 与 expected 均传状态码本身，否定语义传 `"!" + 码`）。
- **问题**：基类自家骨架把两个期望态拼成单 varargs 元素（`"UNSUBMITTED / REJECTED"` 散文串），~120 个子类继承该畸形 expectedStatus 消息载荷（按离散码消费的调用方不可解析）；`requireEntityForUpdate`（:43-45，零锁纯别名）同族误用面注记。计划单元焦点「AbstractProcessor.illegal* helper 期望态参数」直接命中。
- **三态裁决**：新立（r1 common-008 仅涉 reverseApprove 陈旧 javadoc）。P3。
- **修复方向**：M2.x——改双 varargs 传参（`unsubmittedStatus(), rejectedStatus()`）+ 契约 javadoc 加负例。

**P3-CK-common-014-r3**（DIM-B ② daoFor 无豁免注释全仓族——handoff 裁决登记）
- **控制点**：全仓 `module-*/erp-*-service/src/main/java` IDaoProvider 扫描：**354 文件命中，42 文件有豁免/理由注释，312 文件无**。域分布（无注释数）：finance 58 / assets 23 / inventory 22 / projects 21 / manufacturing 21 / hr 21 / purchase 19 / sales 18 / cs 18 / quality 15 / drp 13 / maintenance 11 / contract 11 / b2b 9 / notify 5 / master-data 5 / aps 3 / logistics 2；U20 自身站点：`AbstractProcessor.java:31` protected daoProvider 字段零注释（~120 子类批发继承）。
- **问题与裁决**：同型 per-slice ID（P2-CK-fin-007 / P3-CK-fin3-017-r3 / P3-CK-fin4-022-r3 / P3-CK-ast-028-r3 / P2-CK-qa-027-r3 / P3-CK-qa-028-r3 / P3-CK-mfg2-026-r3 / P3-CK-mfg3-019-r3 / P3-CK-hr2-029-r3 / P3-CK-drp-024-r3 / P3-CK-md 缺注记录 / P3-CK-log-014-r3 本轮 / P2-CK-crm-021-r3 / P3-CK-cs-023-r3）**维持各自范围不动**；本格登记全仓聚合面：族为系统性（312/354 = 88%），多数站点存在事实理由（跨域只读聚合无 purpose-built I*Biz、引擎/工具类范式豁免）但未按 §1.1 程式②落注释——缺陷形态是「登记纪律缺失」而非「访问路径违规」。
- **三态裁决**：新立（五份姊妹报告显式移交的裁决槽位）。P3。
- **修复方向**：M2.x 全仓统一批次——逐文件补一行豁免 javadoc（对齐 DrpEngine/CarryForwardProcessor 先例措辞）或注入 I*Biz；按域分片认领，U20 格为聚合追踪点。

**P3-CK-common-015-r3**（DIM-B ②/D7 `new ServiceContextImpl()` 上下文丢弃全仓族——handoff 裁决登记）
- **控制点**：全仓 `module-*/erp-*-service/src/main/java` 扫描：**82 命中行（80 代码站点 + 2 javadoc）/ 61 文件；14 文件有 `getCtx()` 兜底范式**（`if (context == null) context = new ServiceContextImpl()` / 三元式），**44 文件裸 new**（域分布：cs 6 / manufacturing 5 / finance 4 / crm 4 / logistics 4 / hr 3 / assets 3 / maintenance 3 / contract 3 / inventory 2 / aps 2 / b2b 1 / projects 1 / drp 1）。
- **问题与裁决**：裸 new 丢弃调用方 Processor 真实 context（用户身份/数据权限不进管道）——已登记 exemplar prj-023-r3（rollbackFromTimesheet 五站点）维持不动；本格登记全仓聚合面（44 文件清单随本报告归档，修复按 exemplar 范式 `context != null ? context : ...` 批量对齐）。
- **三态裁决**：新立（prj-023-r3 显式移交槽位）。P3。
- **修复方向**：M2.x——按域分片补 ctx 兜底（镜像 ExpenseCostAggregator:182-185 范式）；job 入口（无用户上下文语义）逐站点裁决豁免。

### 2.4 归属标注（§3.2 + 跨域横切）

- **本格为 common 抽象族唯一归属**：基类行为缺陷（011/013-r3）与共享基建缺陷（012-r3）全部收口本格；各域切片仅调用点审计（六姊妹格本轮的调用点复核结论：mnt 15/15、aps 7/7、log 10/10、notify 3/3、md 25/25 基类接入合规）。
- 全仓族聚合（014/015-r3）为本格新增追踪职责：per-slice ID 不动，M2.x 按域分片认领。
- 聚合横切面归 U21：common-002/003（聚合菜单/action-auth 头注释）涉 app-erp-all 聚合器文件，沿用 r1 登记格跟踪、机制裁决归 M1.16。
- mnt 陈旧 javadoc 新站点归并 common-008（跨格注记已在 mnt 格落）。

### 2.5 维度⑮抽样记录（基类契约抽样——U20 无 owner doc 的替代面）

以基类契约为断言源 6 检查点（illegal* 期望态参数契约：六骨架传参形态核对 → 5 合规 1 违反 = **013-r3 新立**；ErpCrudStatusLock 列探测契约 vs `TestErpCrudStatusLock` 纯函数断言 ✓；Immutable 全拒契约 ✓；MaskHelper fail-closed 契约 ✓；AbstractReverseApprove REJECTED 修复态契约 ✓（common-008 复核面）；SoDGuard null-user 语义契约 ✓）。漂移 1 = 013-r3 新立。

## 3. 统计

| 级别 | 本轮新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 0 |
| P2 | 1（common-011-r3 save 通道盲区） | 0 | 1（common-001） |
| P3 | 4（common-012/013/014/015-r3） | 0 | 9（common-002..010） |
| **合计** | **5** | **0** | **10** |

五格 verdict：DIM-B **finding**（5 新立）/ DIM-F **n-a**（common 层无独立页面，判定面归 U21）/ DIM-S **n-a**（common 层无独立 seed，判定面归 U21）/ DIM-T **pass** / DIM-I **pass**。历史 10 条 r1 ID 零覆写。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：module-common-service 全部 20+ 类逐方法走查（AbstractProcessor 10 成员/六 per-mutation 骨架/两状态锁基类/ErpCrudStatusLock 列探测/九共享基建类/beans 注册）+ module-common-test 基座 5 组件 + 平台 CrudBizModel save 通道源码链（517-523/771-779）+ 双 handoff 全仓扫描（354 文件 daoFor 族 + 61 文件 ServiceContextImpl 族逐域计数）+ 机械程式全套实跑；r1 10 条全量逐条复核；r2 目录核对；基类契约 6 检查点抽样。
- **未深查（边界归属）**：平台侧 `CrudBizModel.save`/`invokeDefaultPrepareSave` 内部实现（nop-entropy 保护区，仅调用链核验）；~120 processor 子类的逐类 illegal* 传参普查（骨架级登记，实例面归 M2.x 修复批）；MaskHelper 七角色 seed 字面与 nop_auth_role.csv 的逐行对账（r1 已实证，本轮复核豁免登记未重跑）；U21 聚合器机制面。
- **残留风险（登记不裁决）**：① 10 条归并 open 修复归 M2.x——P2 common-001（ThreadLocal 跨请求审计抑制）建议与 masking 投产绑定升优先级；② 011-r3 为 F1.3 家族最后盲区，修复前各域「已审核单据不可改」承诺存在 save 旁路面（各域格已交叉注记）；③ 014/015-r3 两族（312 + 44 站点）规模要求 M2.x 按域分片而非单批——建议先骨架/范例域（apsexempt 已齐的 aps）再批量；④ 012-r3 投产触发条件：`erp.data-org-isolation-enabled` 类隔离开关启用前必须修复。
- **successor 触发条件**：M1.17 收官完整性校验本报告 5/5 格；组织隔离开关投产前 common-012-r3 必须修复；状态锁家族 M2.x 收口批次必须含 011-r3（save 通道）+ pur-003 族 open 项统一设计。
