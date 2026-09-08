# ck-hr-org-r3 — human-resource「组织与员工」hr-1 五维符合性审计报告（ai-check-r3 M1.10）

> 工作项：M1.10（U14 × 五维 × hr-1，冻结清单 §4 映射表第 10 行 / §3.3 U14 行）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `f40b4bbae225477f69c3cef1d2a7f64dcc09f30b`（与计划基线行一致）；脏面 = 3 条 untracked（本计划 + 姊妹计划 `2026-09-08-1454-2`/`2026-09-08-1454-3`，均为 mission-driver 起草的未执行计划，零生产路径脏面——Phase 1 时点披露在案，以下全部证据引用该时点）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.2 共享代码唯一归属 + §3.3 U14 行 + §4 第 10 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：组织/岗位/员工/编制——`ErpHrDepartment*` / `ErpHrPosition*` / `ErpHrEmployee*` / `ErpHrEmploymentContract*` 的 BizModel/processor 族（4 BizModel + 3 processor + 2 状态机 Bean + ContractExpiryJob + constants/errors + 4 IErpHr* + 4 entity）+ 招聘入职→员工建档衔接消费点（`ErpHrRecruitmentHireProcessor`），物理面 `module-hr/erp-hr-{dao,service,web}` 的 `src/main`；owner docs `state-machine.md`（307 行）+ `README.md` + `use-cases.md`（组织/员工族用例）。
> 切片边界：hr-2 面（考勤 `ErpHrAttendance*` / 请假 `ErpHrLeave*` / 薪酬 `ErpHrSalary*` / `ErpHrPayroll*` / 税配置 / 排班族）归 M1.11，未触碰；招聘 BizModel 本体不在 hr-1 r3 格集合（r1 `ck-hr-org.md` C6.1 已审）；common 抽象族归 U20（M1.15）本切片只审调用点；posting 引擎内部归 fin-1（M1.1 已收官）；聚合横切面归 U21（M1.16）；notify 派发子系统本体归 U11。
> Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B，roadmap M1.10 行指定）；`code-quality-audit-prompt`（维度⑮断言抽样 + 三态裁决 + Phase 7 汇总）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（hr-1 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点①②⑧⑨⑮）+ 维度⑮断言抽样 | 反模式族全零（hr-1 范围 4 BizModel + 3 processor + 2 状态机 Bean + job + constants/errors + 4 IErpHr* + 4 entity：`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0 / `@Transactional`=0，R6 全域基线 2 为他域站点）；checker 19 规则逐项 = M0.3 快照行零漂移（R1a-c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42）；`IDaoProvider`/`IOrmTemplate` 命中 5 处全部有注释理由（Department/Position FK 列 filter unknown-query-prop → findAll+内存过滤；Employee.countReferences 同域只读聚合对齐 ErpPartyBizModel 范式；3 processor 本域实体更新 = processor-extension-pattern 惯例且 r1「验证为正确」在案）；`_gen`/`_` 前缀脏面=空；`__XGEN_FORCE_OVERRIDE__` 39 处全为 `erp-hr-meta/_vfs/dict/erp-hr/*.dict.yaml` 生成产物（工作树零脏=零手改）；聚合完整性（E1 勘误路径）`app.action-auth.xml` x:extends 含 `/erp/hr/auth/erp-hr.action-auth.xml`（L17）。15/15 维度无跳维：维度① 业务编排 Java + 状态机 Bean 均有 owner doc 背书（entity-state-machine-bean.md 契约 + 退化轴显式登记）；维度② BizModel 全经 `@Inject I*Biz`，3 处例外均为已裁决豁免面（同域只读聚合范式 + P1-MA1-022 读侧裁决），员工-as-partner 跨域写 = 生产零写入（hr 侧 partner 唯一消费 = ErpHrReportBizModel 净余额读侧，P1-MA1-022 豁免 + `ck-hr-attendance-payroll.md` 剩余风险在案）；维度⑧ 组织/岗位删除守卫在位（defaultPrepareDelete 三守卫/两守卫 + 5 个 ERR_DEPT/POSITION_* 错误码），合同/员工状态机 Bean 矩阵与 owner doc §适用对象二/五逐点一致（SUSPENDED 零 writer、三终态零 writer、初始态双 hire 写入点均经 Bean javadoc 如实登记）；维度⑨ job 双层门控 + beans.xml/job.yaml 接线在位（r1 核证 + 本次 job.yaml 复核）；③④⑤⑥⑦⑩⑪逐维 pass（NopException+erp.err.hr.* 集中、@Inject 非私有、CoreMetrics/StringHelper、CrudBizModel+@BizQuery/@BizMutation+@BizLoader、无跨模块 orm 引用、PII 角色 masking fail-closed）；维度⑮ 3 doc × 7 断言（state-machine 3 + README 2 + use-cases 2）——7/7 一致零漂移未触发扩样（详见计划 Phase 2 注记②③）。blocker=0/major=0/minor=0（新发面） | **finding**（归并态，零新立；2 复用 + 13 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + hr-1 面 = 组织架构图页 + 部门/岗位/员工/合同页 + 员工净余额报表页 | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（与基线一致）；[3/4] 325 条 ERR 全部 `variant="primary"`×dropdown-button 枚举漂移同族（既有外部漂移，successor 在案，非本切片 finding，整体 exit 1 余项即此）；hr 域命中 31 条逐条核对均为同族成员（0 条非 variant 族）；`component="AMIS"` 保留层 = 0；ORM `ext:web-renderer="flux"` 缺失 = 0。hr-1 面 6 页逐页：①org-chart（复杂手写页清单成员）`.flux.yaml`（运行时权威）= flux tree 原生组件 + `@query:ErpHrDepartment__findDepartmentTree`（REST `/r/` 约定）+ i18nEn 全承载；孪生 `.page.yaml`（回退遮蔽文件）数据源结构差异属孪生文件面，归并不新立（无 mfg-023 型参数错配，重写注释在案）；②Department `main.page.yaml` = `x:gen-extends` GenPage 纯 codegen stub（M0.4 源头链合规）；③Position / ④EmploymentContract view.xml = 保留层 `x:extends="_gen/…"` + bounded-merge + i18n-en:label；⑤Employee view.xml（389 行）= 保留层 delta 面（bounded-merge + 调动 drawer 三态 handleContract + PII 后端脱敏消费列 + archive 子表 custom 列 i18n-en）+ `ref-employee.page.yaml` = M0.4 row 11 禁改义务在位；⑥report/employee-net-balance.page.yaml = `@query:ErpHrReport__renderHtml` + `/p/ErpHrReport__download`（REST 约定）+ i18nEn。E2E：hr spec 12 个（business-actions 10 + smoke + list-value）违规选择器（`data-slot|data-testid|.cxd-`）= 0；GraphQL 命中为 runbook L229 登记 API 驱动型数据层通道（业务动作 spec 经 GraphQL 直驱 @BizMutation，L122 禁令不适用）；`E2E_ENGINE` 缺省 flux（pages/engine.ts L8） | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + HR 4 表 seed（1045-1 批）+ `md_partner`/`ar_ap_item` 跨域追加行裁决（§3.3 U14 S 行） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数；2026-09-08 15:48 实跑 BUILD SUCCESS）；`git status --porcelain _init-data/` 空（只读零 seed 变更）；资产清点 **372 CSV + 1 SQL**（= seed-data.md 对账表 M1.5 批次后最新登记行，逐数一致）；deploy `_seed_*.sql` 命中 6 文件 = module-cs×3 方言 + module-notify×3 方言，逐命中查登记处表 seed-data.md §同步义务 L107-108 均 ✅已聚合（`nop_sys_code_rule.csv` + `erp_sys_notification_template.csv`），hr 无 deploy seed 零同步义务；HR 4 表 seed 在位（erp_hr_department/employee/position/employment_contract = 2/2/3/3 数据行，1045-1 批登记）；员工-as-partner 跨域追加行裁决 = 与 seed-data.md §1045-1 批登记（L311-353）一致：`erp_md_partner` id=5 `EMP-PTN-001`（PARTNER_TYPE=EMPLOYEE，对齐 expense-claim.md 设计）+ `erp_fin_ar_ap_item` 2 行 OPEN（ARAP-EA-001 EMPLOYEE_ADVANCE/RECEIVABLE/1000 + ARAP-EC-001 EXPENSE_CLAIM/PAYABLE/300）partnerId=5 FK 链完整（partner(5) ← ar_ap_item(5,6)，净余额 700 报表可观测）——追加行为报销侧往来演示行，无 hr_employee 镜像一致性要求（两实体分离裁决 r1「验证为正确」在案 + 1045-1 批注册契约即 FK 链），核证一致 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + hr-1 范围（组织/岗位/员工/编制族 BizModel）+ employee-net-balance 跨域读取契约测试（§3.3 U14 T 行） | `mvn test -pl module-hr/erp-hr-service` **249/0/0/0 全绿**（= `cjk-baseline.md` §批注账 MI.6 批 2 行锚点 `hr 249`，零失败零回归；姊妹计划 1454-2/3 面 = prj/qa + pur/sal/inv 模块，hr 无姊妹增量待披露）。覆盖对账：hr-1 公开方法 6 个（Department findDepartmentTree 1 / Position 0 桩 / Employee transferEmployee+countReferences 2 / Contract scanExpiringContracts+expireOverdueContracts+renew 3）↔ 测试逐项落点：transferEmployee ↔ `TestErpHrEmployeeTransfer`（10 用例）、countReferences ↔ `TestErpHrEmployeeReferences`、scanExpiringContracts（L71）+ expireOverdueContracts（L87）+ renew ↔ `TestErpHrContractExpiry`（7 用例）、删除守卫 ↔ `TestErpHrDepartmentPositionDeleteGuard`（8 用例，P1-CK-hr-001 修复回归）；**缺口 1**：`findDepartmentTree` 无直接单测——同控制点缺陷维度已全部在册（P2-CK-hr-003），测试缺位按 mfg-022-r3 前例并入该 ID 追加证据，不重复立项（只读树查询，GraphQL/xmeta 层有 NonNull/必填拦截，行为风险已被在册 finding 承载）；关键业务流：employee-net-balance 跨域读取契约测试在位——`TestErpHrReportRendering` 3 用例（renderHtml / download xlsx+pdf / dataset，`netBalance=700` 断言与 DIM-S seed 裁决 700 交叉一致）；P1 考勤→薪酬→过账清单行归 hr-2（M1.11），未触碰。`SnapshotTest.RECORDING` = 0 残留；`delVersion` 命中均为逻辑删除断言/注释（非屏蔽滥用）；`_cases` 零 `*` 通配屏蔽（createTime/updateTime 框架自动屏蔽）；249 全绿 = CHECKING 态等价证明 | **pass**（附 findDepartmentTree 测试缺位注记，归并 P2-CK-hr-003） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 baseline files 单向收紧成立）；`--self-test` **PASS**（13/13）；hr 探针族（CAT-1 20）维持清零零回归（Phase 1 report mode CAT-1..4 = 0/0/0/0 一致）；WHITELIST hr 条目 = **1 条**（<3 条 → 全数核对并记录条目数）：`ErpHrConstants.java` 四要素齐备——文件路径（module-hr/erp-hr-service/…/ErpHrConstants.java）/ 理由（`HR_ROLE_ID="HR 专员"` 为 seed 角色名数据契约，运行期匹配 nop_auth_role.csv seed roleId 与 erp-hr.action-auth.xml roles 字面，改英文破坏角色解析行为，C2 窄类豁免）/ owner doc 指针（i18n-compliance.md 判定准绳表 #5 + CAT-3 行 C2②）/ 裁决来源（plan 2026-09-07-1715-1 Phase 2）——实仓复核 `ErpHrConstants` L293 `HR_ROLE_ID = "HR 专员"` 与登记一致零缺陷；`grep -L @Locale` 全量 `*Errors.java` = 0/21 缺失（`ErpHrErrors` @Locale 在位）；`git status --porcelain 'module-*/erp-*-meta/**'` + `_vfs/i18n` = 空（`_` 前缀生成 i18n yaml 零手改） | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单 `ck-hr-org.md`（C6.1：0 P0 / 1 P1 / 4 P2 / 12 P3）§Finding 追踪 17 条逐一比对 + 同单元 hr-2 域报告 `ck-hr-attendance-payroll.md`（C6.2 跨切片同域参考）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/`（无 hr 新独立登记）+ §Mission 基线快照（daoFor 读侧 P1-MA1-022、死状态 P1-MA2-039/040、调动休假告警 P2-MA2-050 均已裁决偏离不重复报告）。**本轮新立 0 条**（无 `-r3` 后缀新 ID）；历史 ID 零覆写。行号均为 T0 = HEAD `f40b4bbae` 实测（承计划 Phase 2 注记）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 2 条

| 原 ID | 修复在位证据（T0 = HEAD `f40b4bbae`） |
| --- | --- |
| P1-CK-hr-001（部门/职位删除无引用守卫） | `defaultPrepareDelete` 守卫修复在位：部门三守卫（在职员工/子部门/招聘单）+ 职位两守卫（在职员工/招聘单）+ 5 个 ERR_DEPT/POSITION_* 错误码；回归测试 `TestErpHrDepartmentPositionDeleteGuard` 8 用例全绿（含 CLOSED 招聘单放行正路径与空部门/空职位放行）。修复源自 plan-2026-08-26-0630 F 批；**注记**：跨轮索引该行状态仍 `open` = 状态回填缺口（lesson-11 同型），登记不属本只读切片义务，修复有效性以本行 HEAD 复核为准 |
| P2-CK-hr-004（通用 CRUD 无状态守卫，同型 P1-CK-pur-003 族） | F1.3 状态锁基类在位：hr 5 实体 BizModel 均 extends `AbstractErpCrudBizModel`（posted/APPROVED 拒通用 update/delete，config 默认 ON，无列实体惰性）；索引行状态已 `fixed` 与本复核一致；调用点合规，基类行为缺陷归 U20（M1.15） |

### 2.2 归并（同型 open 追加证据至原 ID）— 13 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-hr-002 | `transferEmployee` 仍不写 orgId 原样（ErpHrEmployeeTransferEmployeeProcessor L89-95：只写 departmentId/positionId/superiorId）——orgId 与部门失配沿续签合同扩散面未变 |
| P2-CK-hr-003 | `findDepartmentTree` limit 5000 双全量 + 无 orgId + empCount 无状态口径仍在；**DIM-T 追加证据**：该查询无直接单测（grep src/test/java 零命中），同控制点测试缺位并入本 ID（mfg-022-r3 前例），行为风险已由本 ID 在册维度承载 |
| P2-CK-hr-005 | hire 联动 L65-66 仍无部门/职位存在性校验（`ErpHrRecruitmentHireProcessor#createEmployeeFromRecruitment` 直接透传）——对照调动链 `requireTargetDepartment` 正向示范不对称仍在 |
| P3-CK-hr-006 | effectiveDate/hiredDate `toString()` NPE 面仍在（hire/createContractForNewEmployee + buildSuccessorCode）——GraphQL NonNull 主拦截层不变，直接 Java 调用暴露 |
| P3-CK-hr-007 | renew 无 newEndDate 边界校验仍在（仅 `assertCanRenew` 状态守卫） |
| P3-CK-hr-008 | renew 就地延长语义维持登记态（需求分歧只登记不裁决）——实现与 UC-HR-07 分歧原样 |
| P3-CK-hr-010 | targetSuperiorId 非空即写无存在性/自引用/成环校验仍在（L93-95） |
| P3-CK-hr-011 | BizModel/Processor 双副本死代码仍在——本切片逐行比对双份一致（Employee 3 helper 族 + Recruitment hire 族），零漂移即零新增风险登记 |
| P3-CK-hr-012 | 三层 cron 键漂移仍在（实核：`erp-hr-contract-expiry.job.yaml` enabled\|false 外层 + cron-expr 外层 + `erp-hr.contract-expiry-cron` 内层 execute 门控）——同型家族联合裁决面未变 |
| P3-CK-hr-013 | job 顶层 catch 仅 LOG.error 无告警通道仍在（无 `IErpSysNotificationBiz` 派发） |
| P3-CK-hr-014 | countReferences `findAllByQuery().size()` 全实体计数仍在（对照注释豁免面 = 同域只读聚合范式登记，本条为性能维度原样） |
| P3-CK-hr-015 | idCardNo 无查重仍在（ORM 无 UK、应用层零校验） |
| P3-CK-hr-016 | hire 联动 gender 硬编码 "MALE" + 中文姓名拆分假设仍在（L61 + extractFirst/LastName） |

### 2.3 新立 `-r3` — 0 条

本切片五格（DIM-B 15 维 + DIM-F 6 页 + DIM-S + DIM-T + DIM-I）未产生超出 r1 在册形态的新发缺陷；唯一新见面（`findDepartmentTree` 测试缺位）为已登记控制点的证据维度，按查重纪律归并不新立。

### 2.4 范围外注记（格集合外，维持登记态）

- **P3-CK-hr-017**（hire 初始 ACTIVE vs PROBATION 需求分歧）：招聘 BizModel 本体不在 hr-1 r3 格集合（本切片仅审 `ErpHrRecruitmentHireProcessor` 衔接消费点）；分歧裁决面维持登记态，归 hr-2（M1.11）或修复批处置。
- **P3-CK-hr-009**（招聘 mutation 入参无边界）：同属招聘 BizModel 面，范围外同上。
- **DIM-F 孪生文件面**：org-chart `.page.yaml` 回退遮蔽副本与 `.flux.yaml` 数据源结构差异——同型裁决归 P3-CK-mfg-023-r3/U21（M1.16），本切片注记归并不新立（无 mfg-023 型参数错配缺陷）。

### 2.5 归属标注（§3.2 共享代码边界）

- common 抽象族（`AbstractProcessor.illegal*`、`AbstractErpCrudBizModel` 状态锁基类）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- posting 引擎内部归 fin-1（M1.1 已收官）；聚合横切面（action-auth 聚合器/flux 导出门禁本体/seed 全量装载）归 U21（M1.16），本格仅核 hr 注册在位性；notify 派发子系统本体归 U11。
- hr-1 专属判定面（§3.3 U14 B 行）双向核证：①组织/岗位删除守卫——守卫在位且测试回归在案（§2.1 复用行）；②员工-as-partner 跨域写——生产零写入，读侧消费（ErpHrReportBizModel 净余额）为 P1-MA1-022 豁免面 + `ck-hr-attendance-payroll.md` 剩余风险在案，无双写联动要求（r1「验证为正确」裁决维持）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 1（hr-001） | 0 |
| P2 | 0 | 1（hr-004） | 3（hr-002/003/005） |
| P3 | 0 | 0 | 10（hr-006..008、010..016） |
| **合计** | **0** | **2** | **13** |

五格 verdict：DIM-B **finding**（归并态，零新立）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass**（附 findDepartmentTree 测试缺位注记归并 P2-CK-hr-003）/ DIM-I **pass**。r1 hr 17 条 ID 状态零覆写（2 fixed 复核有效 + 13 open 追加证据 + 2 范围外维持登记态）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：hr-1 四族 BizModel/processor/状态机 Bean/job/constants/errors 全读（Department/Position/Employee/Contract + Transfer/Hire/ExpireOverdue processor + Employee/Contract 状态机 Bean + ContractExpiryJob + 4 IErpHr*）+ 机械程式全套实跑（compliance checker 19 规则 / 反模式 grep 族 / codegen 安全 39 站点 / 聚合完整性 E1 路径 / validate:flux / seed 门禁 4/0/0/0 / hr 回归 249 / strict+self-test）；owner docs 3 份 × 7 断言抽样；hr-1 面 6 页逐页走查 + E2E 12 spec 纪律核对；r1 17 条逐一比对三态裁决；seed 资产清点对账 + 1045-1 批跨域追加行裁决核对；employee-net-balance 契约测试核对。
- **未深查（边界归属）**：hr-2 面（考勤/请假/薪酬/工资单/税配置/排班族 + 薪酬过账 posted 一致性，归 M1.11）；招聘 BizModel 本体（状态机 5 mutation + makeOffer/scheduleInterview，r1 已审、r3 格集合外）；`erp-hr-web` 其余页面全量契约 drift（仅 hr-1 面 6 页走查，全局面归 U21/M1.16）；`ErpHrReportBizModel` 渲染细则（报表子系统既有验收，仅结构扫描）；md 侧 `ErpMdOrganizationReferenceChecker` 实现细节（md 域）；测试代码仅作覆盖对账与行为语义消费。
- **残留风险（登记不裁决）**：① 13 条归并 open finding 修复归 M2.x（hr P1/P2 修复批），其中 P2-CK-hr-002（调动 orgId 失配沿续签扩散）与 P2-CK-hr-003（组织树双全量+无隔离+静默截断）在多组织部署与组织重组活跃场景影响面最大，建议 M2.x 优先级排序参考；② P1-CK-hr-001 跨轮索引状态回填缺口（lesson-11 同型）——修复在 HEAD 有效但索引行仍 open，状态回填归索引 owner 流程，本报告 §2.1 复核证据可直接消费；③ `validate:flux` 325 条 variant 漂移与 org-chart 孪生文件面均为批前在案外部事项（successor/U21 在案），非本切片范围；④ 本格集合仅覆盖 U14 hr-1，U14×五维×hr-2 归 M1.11——M1.17 收官前 U14 基础格完整性依赖 hr-2 报告落盘。
- **successor 触发条件**：M1.11（hr-2）承接考勤/请假/薪酬/排班/税配置族五格与本报告标注的招聘 BizModel 范围外项；M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2 open 项（P2-CK-hr-003 同时承接 findDepartmentTree 测试缺位注记）。
