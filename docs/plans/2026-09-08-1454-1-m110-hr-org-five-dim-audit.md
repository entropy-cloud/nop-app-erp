---
status: active
mission: ai-check-r3
work-item: M1.10
group: "2026-09-08-1454"
verify: [test]
---

# 2026-09-08-1454-1 M1.10 hr hr-1 五维符合性审计（组织与员工切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）。M1.10 为当前 roadmap 文档序首个「deps 已满足且无既有计划」的工作项（M1.2~M1.4 依赖 M1.1、M1.6/M1.7 依赖 M1.5、M1.9 依赖 M1.8——三者同批已执行完毕、done 翻转归 owner/engine 依独立结束审计处置，故其依赖项暂不可开工）。
- 同 mission 先例：M1.1/M1.5/M1.8 三切片（plans `2026-09-08-1042-1/2/3`）已执行并独立闭包审计 ACCEPT，`ck-finance-posting-r3.md` / `ck-mfg-workorder-r3.md` / `ck-assets-lifecycle-r3.md` 已落盘——本计划沿用其通过的七阶段执行形态。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U14 × 五维 × hr-1**（§4 映射表第 10 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- 切片范围（U14 hr-1）：组织/岗位/员工/编制；owner doc `docs/design/human-resource/state-machine.md`（307 行，实仓核验在盘）+ `README.md` + `use-cases.md`（组织/员工族用例）；物理面 `module-hr/erp-hr-{dao,service,web}` 的 `src/main`，hr-1 实体族 = `ErpHrDepartment*` / `ErpHrPosition*` / `ErpHrEmployee*` / `ErpHrEmploymentContract*` 的 BizModel/processor 族 + 招聘入职→员工建档衔接消费点（`ErpHrRecruitmentHireProcessor`）；hr-2 面（考勤 `ErpHrAttendance*` / 请假 `ErpHrLeave*` / 薪酬 `ErpHrSalary*` / `ErpHrPayroll*` / 税配置 / 排班族）不在本切片（归 M1.11）。
- 共享代码唯一归属（冻结清单 §3.2）：common 抽象族（`AbstractProcessor.illegal*`、`AbstractErpCrudBizModel` 状态锁基类）行为缺陷归 U20（M1.15），本切片只审调用点；posting 引擎内部归 fin-1；聚合横切面归 U21；notify 派发子系统本体归 U11。hr-1 专属判定面（§3.3 U14 B 行）：组织/岗位删除守卫 / 员工-as-partner 跨域写。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md`（同域报告 `ck-hr-org.md` C6.1：0 P0 / 1 P1 / 4 P2 / 12 P3，done）+ 同单元 hr-2 域报告 `ck-hr-attendance-payroll.md`（C6.2，跨切片查重同域参考）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：hr 探针族（CAT-1 20）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。hr 白名单条目 1 条（`ErpHrConstants.java`，C2 seed 角色名契约）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；hr service 模块回归对照计数 249（cjk-baseline §批注账 MI.6 批 2 行锚点；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）；`npm run validate:flux` step [1/3] 导出 0 error（999 页 / erp 855），整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移（successor 在案，非本切片 finding）。
- 仓库现状（2026-09-08 起草时实核）：HEAD `f40b4bbae`，工作树零脏面。审计证据以实跑时点 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U14 × 五维 × hr-1 五格 verdict 未落盘；执行目录尚无 hr 切片 `ck-*.md` 报告。

## Goals

- 按冻结清单对 U14 × 五维 × hr-1 五格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-hr-org-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 hr-1 相关 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 hr-2 格（M1.11：考勤/请假/薪酬/排班/税配置族）与 U14 之外任何单元格；薪酬过账、工资单页面、考勤语义不在本切片立项。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 在位
      - Skill: none
      - 证据（2026-09-08）：目录在位，含 `ai-check-r3-index.md`、`m0-5-audit-checklists.md` 及 M1.1/M1.5/M1.8 三份姊妹报告
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、同批姊妹计划（`2026-09-08-1454-2/3`，尚待执行）披露（MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：审计时点 HEAD `f40b4bbae225477f69c3cef1d2a7f64dcc09f30b`（与计划基线一致）；`git status --porcelain` = 3 条 untracked（本计划 + 姊妹计划 `2026-09-08-1454-2`/`2026-09-08-1454-3`，均为 mission-driver 起草的未执行计划，零生产路径脏面）；姊妹计划尚待执行，与本计划无代码交叠（本计划零生产代码改动）。以下全部证据注记引用该时点
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：compliance checker 实跑 = R1a-c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42——与 M0.3 快照行逐规则一致，零漂移；CJK report mode（3430 java + 886 yaml）= CAT-1..4 = 0/0/0/0（CAT-5 注释 21158 行豁免仅统计），exit 0，与 MI 终态行对账一致；无漂移登记项

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.10 行指定，承 M1.1 行「同上」）
> Targets: `module-hr/erp-hr-dao|erp-hr-service/src/main/java`（hr-1 范围 = 组织/岗位/员工/编制族 + 招聘入职衔接消费点文件）；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：compliance checker 全规则=基线（Phase 1 注记，零新增违规）；hr-1 范围（4 BizModel + 3 processor + 2 state machine Bean + job + constants/errors + 4 IErpHr* + 4 entity）grep 族：`extends RuntimeException`=0、`@Inject private`=0、`System.currentTimeMillis`=0、`@Transactional`=0（R6 基线 2 为全域他域站点）；`IOrmTemplate`/`IDaoProvider` 命中 5 处全部有注释理由（Department/Position FK 列 filter unknown-query-prop → findAll+内存过滤注释；Employee.countReferences 同域只读聚合对齐 ErpPartyBizModel 范式注释；3 processor 本域实体更新 = processor-extension-pattern 惯例且 r1「验证为正确」在案）；`_gen`/`_` 前缀脏面=空；`__XGEN_FORCE_OVERRIDE__` 命中 39 文件全为 `erp-hr-meta/_vfs/dict/erp-hr/*.dict.yaml` 生成产物（工作树零脏=零手改，只读校验点合规）；聚合完整性：`app.action-auth.xml` x:extends 含 `/erp/hr/auth/erp-hr.action-auth.xml`（L17）在册
- [x] <Proof> 15 维度逐维走查 hr-1 范围（程序式确定性走查落 verdict；hr-1 专属焦点：①Model→Delta→Java ②跨实体 I*Biz——员工-as-partner 跨域写 `md_partner` 消费点 ⑧状态机——组织/岗位删除守卫与员工生命周期 ⑨审批流/作业 ⑮断言抽样——`state-machine.md` + `README.md`/`use-cases.md` ≥2 doc × 2 关键断言（状态名/字段名/角色名/迁移路径/ErrorCode），≥2 处漂移扩大至全部 hr-1 owner doc）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：15 维无跳维——①业务编排 Java + 状态机 Bean 均有 owner doc 背书（entity-state-machine-bean.md 契约 + 退化轴显式登记），无「能模型化却硬编码」blocker；②BizModel 全经 `@Inject I*Biz`（departmentBiz/positionBiz/employmentContractBiz/leaveRequestBiz/employeeBiz），3 处 daoFor/IOrmTemplate 例外均有注释且属已裁决豁免面（同域只读聚合范式 + P1-MA1-022 读侧裁决）；员工-as-partner 跨域写 = 生产零写入（r1 ck-hr-org.md「验证为正确」裁决无双写联动要求；hr 侧 partner 唯一消费 = ErpHrReportBizModel 净余额读侧，归 P1-MA1-022 豁免 + ck-hr-attendance-payroll.md 剩余风险在案）；⑧组织/岗位删除守卫在位（defaultPrepareDelete 三守卫/两守卫 + 5 个 ERR_DEPT/POSITION_* 错误码），合同/员工状态机 Bean 矩阵与 owner doc §适用对象二/五逐点一致（SUSPENDED 零 writer、三终态零 writer、初始态 ACTIVE 双 hire 写入点均经 Bean javadoc 如实登记）；⑨job 双层门控 + beans.xml/job.yaml 接线在案（r1 核证 + 本次 job.yaml 复核）；③④⑤⑥⑦⑩⑪逐维 pass（NopException+erp.err.hr.* 集中、@Inject 非私有、CoreMetrics/StringHelper、CrudBizModel+@BizQuery/@BizMutation+@BizLoader、无跨模块 orm 引用、PII 角色 masking fail-closed）；blocker=0/major=0/minor=0（新发面）
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样（≥2 doc × 2 断言）独立落注记，抽样对象与逐条结论在案
      - Skill: code-quality-audit-prompt
      - 证据（3 doc × 7 断言，全一致，无扩大抽样触发）：①`state-machine.md` §适用对象五「contract-status 含 SUSPENDED 且零 writer」↔ grep `setStatus(SUSPENDED)`=0 + StateMachine Bean 不含 SUSPENDED 边——一致；§适用对象二「RESIGNED/TERMINATED/RETIRED 零 writer、仅 2 处入职写 ACTIVE」↔ grep `setEmploymentStatus(` 仅 RecruitmentBizModel:150 + RecruitmentHireProcessor:63（均 ACTIVE）——一致；Bean `initialStatuses={ACTIVE,PROBATION}`/`terminalStatuses={RESIGNED,TERMINATED,RETIRED}` 与 §一/§三 逐点一致。②`README.md` §工程与模型（appName erp-hr/实体包 app.erp.hr.dao.entity/表前缀 erp_hr_/类名前缀 ErpHr*）↔ 实仓逐项一致；§状态机摘要「生效→到期/解除/中止」「试用期↔在职」为 target 行为摘要，实现漂移已由权威 doc state-machine.md §二/§五显式 Deferred 注记承载（r1 P1-MA2-039/040 resolved 裁决在案，不另立）。③`use-cases.md` UC-HR-08（前置「目标部门和职位已存在」/流程「更新 departmentId/positionId/superiorId」/「原合同→TERMINATED+创建新合同」/异常「休假冲突告警」）↔ requireTargetDepartment/requireTargetPosition + ERR_TRANSFER_TARGET_* + resolveHandleContract + warnIfLeaveConflict 逐点一致；UC-HR-05 后置「HIRED 时创建 ErpHrEmployee + employeeId 关联」↔ ErpHrRecruitmentHireProcessor.hire requireStatus(OFFERED→HIRED)+createEmployeeFromRecruitment+setEmployeeId 一致，状态链 7 值与 ErpHrConstants.RECRUITMENT_STATUS_* 一致
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-hr-org.md` §Finding 追踪 + `ck-hr-attendance-payroll.md` 同域参考 + r2 目录 + §Mission 基线快照）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-hr-{NNN}-r3`；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - 证据（DIM-B 候选全裁决，零新立）：复用 2——P1-CK-hr-001（守卫修复在 HEAD 有效：defaultPrepareDelete 5 守卫 + 5 错误码在位；修复源自 plan-2026-08-26-0630 F 批，arm-index 行状态仍 open = 回填缺口注记，lesson-11 同型，不属本只读切片义务）、P2-CK-hr-004（F1.3 状态锁基类在位：5 BizModel 均 extends AbstractErpCrudBizModel）；归并 13——P2-CK-hr-002（transferEmployee 仍不写 orgId，HEAD L89-95 实证）、P2-CK-hr-003（findDepartmentTree limit 5000 双全量 + 无 orgId + empCount 无状态口径仍在）、P2-CK-hr-005（hire 联动 L65-66 无部门/职位存在性校验仍在）、P3-CK-hr-006（effectiveDate/hiredDate toString NPE 面仍在）、P3-CK-hr-007（renew 无 newEndDate 校验仍在）、P3-CK-hr-008（renew 就地延长语义维持登记态）、P3-CK-hr-010（superiorId 非空即写无校验仍在）、P3-CK-hr-011（BizModel/Processor 双副本死代码仍在，逐行比对双份一致）、P3-CK-hr-012（三层 cron 键漂移仍在：job.yaml enabled|false + cron-expr 外层 + erp-hr.contract-expiry-cron 内层实核）、P3-CK-hr-013（job 顶层 catch 仅 LOG.error 无告警仍在）、P3-CK-hr-014（countReferences findAllByQuery().size() 仍在）、P3-CK-hr-015（idCardNo 无查重仍在）、P3-CK-hr-016（gender 硬编码 MALE 仍在）；范围外注记 1——P3-CK-hr-017（hire 初始 ACTIVE 需求分歧，维持登记态；招聘 BizModel 本体不在 hr-1 r3 格集合）；P3-CK-hr-009（招聘 mutation 入参）同属招聘 BizModel 面，范围外。已裁决偏离不重复报告：daoFor 读侧 P1-MA1-022、死状态 P1-MA2-039/040、调动休假告警 P2-MA2-050

Exit Criteria:

- [x] U14×B×hr-1 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维
      - verdict = **finding（归并态，零新立）**：机械程式全绿 + 15 维 pass；本格全部 finding 均为 r1 open 同型归并（13 项）/fixed 复用（2 项），无新发缺陷；详见 `ck-hr-org-r3.md` §覆盖矩阵
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用/归并/新立逐条在案）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-hr/erp-hr-web/src/main/resources/_vfs`（hr-1 面：组织架构图页 + 部门/岗位/员工页面）；E2E 涉 hr spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案，非本切片 finding）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据：`npm run validate:flux` 实跑 exit=1——step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（与基线一致）；[3/4] 325 条 ERR 全部 `variant="primary"`×dropdown-button 枚举漂移同族（既有外部漂移，successor 在案，非本切片 finding）；hr 域命中 31 条逐条核对均为同族成员（0 条非 variant 族），不立项；`component="AMIS"` 保留层 = 0；`grep -L ext:web-renderer="flux"` = 空（全 ORM 在册）
- [x] <Proof> hr-1 页面走查：组织架构图页（复杂手写页清单成员）+ 部门/岗位/员工 codegen+delta 面对照 view-and-page-strategy（REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定）逐页落 verdict；涉 hr E2E spec 时核对 PageObject 模式 + `E2E_ENGINE` flux 缺省 + 禁 GraphQL 断言；查重：全局面 325 ERR 族与 hr 域命中条数对账（既有外部漂移不立项）
      - Skill: none
      - 证据（hr-1 面 6 页逐页）：①org-chart（复杂手写页清单成员）`.flux.yaml`（运行时权威）= flux tree 原生组件 + `@query:ErpHrDepartment__findDepartmentTree`（REST `/r/` 约定）+ i18nEn 全承载（label/placeholder/button/空态/node 模板）；孪生 `.page.yaml`（回退遮蔽文件）为 F13 flux 重写前回退副本——数据源结构差异（findPage 平面 vs findDepartmentTree 嵌套）属孪生文件面，同型登记见 P3-CK-mfg-023-r3「同构面裁决归 U21/M1.16」，本切片按查重纪律注记归并不新立（无 mfg-023 型参数错配缺陷，重写注释在案）；②ErpHrDepartment/main.page.yaml = `x:gen-extends` GenPage 纯 codegen stub（M0.4 源头链合规）；③ErpHrPosition/④ErpHrEmploymentContract view.xml = 保留层 `x:extends="_gen/…"` + bounded-merge + i18n-en:label；⑤ErpHrEmployee view.xml（389 行）= 保留层 delta 面（bounded-merge + 调动 drawer 三态 handleContract + PII 后端脱敏消费列 + archive 子表 custom 列 i18n-en）+ `ref-employee.page.yaml` = M0.4 row 11 禁改义务在位（view.xml `i18n-en` 承载源）；⑥report/employee-net-balance.page.yaml = `@query:ErpHrReport__renderHtml` + `/p/ErpHrReport__download`（REST 约定）+ i18nEn。E2E：hr spec 12 个（business-actions 10 + smoke + list-value）违规选择器（`data-slot|data-testid|.cxd-`）= 0；GraphQL 命中为 runbook L229 登记 API 驱动型数据层通道（业务动作 spec 经 GraphQL 直驱 @BizMutation，L122 禁令不适用）；`E2E_ENGINE` 缺省 flux（pages/engine.ts L8 raw env 读取，flux 强制）；全局面 325 ERR 族 hr 命中 31 条已对账（上方）

Exit Criteria:

- [x] U14×F×hr-1 格 verdict 落盘；全局面门禁数字（导出 error 数 / flux-only 命中数）在案对账一致
      - verdict = **pass**：导出 0 error/999 页；325 ERR=既有 stub 族（hr 31 条同族对账不立项）；AMIS=0、flux-renderer 缺失=0；6 页逐页合规；E2E 纪律 hr 面 0 违规；孪生文件面注记归并 P3-CK-mfg-023-r3/U21 裁决（零新立）
- [x] hr-1 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ hr-1 相关 seed（HR 4 表 + `md_partner` 员工-as-partner 跨域追加行裁决面）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` BUILD SUCCESS（2026-09-08 15:48 实跑）
- [x] <Proof> hr-1 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ 资产清点对账（对照 seed-data.md 对账表最新登记行）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表 + HR 4 表 seed（1045-1 批）与 `md_partner`/`ar_ap_item` 跨域追加行裁决（§3.3 U14 S 行）核对：员工-as-partner 追加行 ↔ hr 员工 seed 一致性（薪酬过账 posted 一致性属 hr-2 面，不在本切片）
      - Skill: none
      - 证据：seed 脏面=空（只读零变更）；资产清点 = 372 CSV + 1 SQL（= seed-data.md 对账表 M1.5 批次后最新登记行，逐数一致）；deploy `_seed_*.sql` 命中 6 文件 = module-cs×3 方言 + module-notify×3 方言，逐命中查登记处表 seed-data.md §同步义务 L107-108 均 ✅已聚合（`nop_sys_code_rule.csv` + `erp_sys_notification_template.csv`），hr 无 deploy seed 无同步义务（「其余模块」行 grep 实证在案）；HR 4 表 seed 在位（erp_hr_department/employee/position/employment_contract = 2/2/3/3 数据行，1045-1 批登记）；员工-as-partner 跨域追加行裁决 = 与 seed-data.md §1045-1 批登记（L311-353）一致：`erp_md_partner` id=5 `EMP-PTN-001`（PARTNER_TYPE=EMPLOYEE，对齐 expense-claim.md 员工-as-partner 设计）+ `erp_fin_ar_ap_item` 2 行 OPEN（ARAP-EA-001 EMPLOYEE_ADVANCE/RECEIVABLE/1000 + ARAP-EC-001 EXPENSE_CLAIM/PAYABLE/300）partnerId=5 FK 链完整（partner(5) ← ar_ap_item(5,6)，净余额 700 报表可观测）——追加行设计为报销侧往来演示行，无 hr_employee 镜像一致性要求（两实体分离裁决 r1 ck-hr-org.md「验证为正确」在案 + 1045-1 批注册契约即 FK 链），核证一致；薪酬过账 posted 一致性属 hr-2 面未触碰

Exit Criteria:

- [x] U14×S×hr-1 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
      - verdict = **pass**：4/0/0/0 全绿 + 零 seed 变更 + 清点对账一致 + deploy 义务全登记 + 跨域追加行与注册裁决一致
- [x] seed 零变更 + 同步义务核对 + 跨域追加行裁决结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-hr/erp-hr-service`（`<SVC>` = 本模块，同冻结清单 §1.4 记法）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-hr/erp-hr-service` 全绿零失败（数字落注记，对照 known-good-baselines 各域计数 + 姊妹计划增量披露）
      - Skill: none
      - 证据（2026-09-08 19:40 实跑）：`Tests run: 249, Failures: 0, Errors: 0, Skipped: 0` BUILD SUCCESS——= 计划锚点 249（`cjk-baseline.md` §批注账 MI.6 批 2 行 `hr 249` 聚合参照），零失败零回归；姊妹计划 `2026-09-08-1454-2/3` 面 = prj/qa + pur/sal/inv 模块，hr 无姊妹增量待披露；known-good-baselines 2026-09-08 MI 终态行全 reactor 4006/0/0/1（hr 域全绿）在案
- [x] <Proof> 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 测试目录清单逐项对账（hr-1 范围 = 组织/岗位/员工/编制族 BizModel）；关键业务流清单核对——employee-net-balance 跨域读取契约测试覆盖核对（§3.3 U14 T 行）；缺口按业务关键度定级
      - Skill: none
      - 证据：hr-1 公开方法 6 个（ErpHrDepartmentBizModel findDepartmentTree 1 / ErpHrPositionBizModel 0 桩 / ErpHrEmployeeBizModel transferEmployee+countReferences 2 / ErpHrEmploymentContractBizModel scanExpiringContracts+expireOverdueContracts+renew 3）↔ 测试逐项落点：transferEmployee ↔ TestErpHrEmployeeTransfer（10 用例）、countReferences ↔ TestErpHrEmployeeReferences、scanExpiringContracts（L71）+ expireOverdueContracts（L87）+ renew ↔ TestErpHrContractExpiry（7 用例）、删除守卫 ↔ TestErpHrDepartmentPositionDeleteGuard（8 用例）。**缺口 1**：findDepartmentTree 无直接单测（grep src/test/java 零命中）——同控制点缺陷维度已全部在册（P2-CK-hr-003），按 mfg-022-r3 前例（reverseApprove 测试缺位并入同控制点 finding）归并 P2-CK-hr-003 追加证据，不重复立项；关键业务流：employee-net-balance 跨域读取契约测试在位——TestErpHrReportRendering 3 用例（renderHtml / download xlsx+pdf / dataset，`netBalance=700` 断言与 Phase 4 seed 裁决净余额 700 交叉一致）；P1 考勤→薪酬→过账清单行归 hr-2（M1.11）未触碰
- [x] <Proof> 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0（提交态零残留）+ `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据：`SnapshotTest.RECORDING` = 0 命中（零残留）；`delVersion` 命中均为逻辑删除断言/注释（TestErpHrDepartmentPositionDeleteGuard 守卫断言，非屏蔽滥用）；`_cases` autotest.yaml 零 `*` 通配屏蔽、零手工 createTime/updateTime 屏蔽行（框架自动屏蔽口径）；249 全绿 = CHECKING 态等价证明

Exit Criteria:

- [x] U14×T×hr-1 格 verdict 落盘；本地回归全绿数字在案
      - verdict = **pass**（附 findDepartmentTree 测试缺位注记，归并 P2-CK-hr-003）：249/0/0/0 全绿 = 锚点零回归；覆盖对账 6 方法逐项落点 + 缺口 1 归并在案；net-balance 契约测试在位；详见 `ck-hr-org-r3.md` §覆盖矩阵 DIM-T 行
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + hr 白名单条目核对
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据（2026-09-08 19:4x 实跑）：`--strict` **PASS exit 0**（0 new violations vs 冻结快照，170 baseline files 单向收紧成立；3430 java + 886 yaml，现树 CAT-1..4 = 0/0/0/0，CAT-5 注释 21158 行豁免仅统计）；`--self-test` **PASS**（13/13）；hr 探针族（CAT-1 20）维持清零零回归，无 MI 回归升级项
- [x] <Proof> 白名单合规核对：`docs/audits/cjk-baseline.md` §WHITELIST hr 条目全数核对（hr = 1 条 `ErpHrConstants.java`；不足 3 条时全数核对并记录条目数）四要素（文件路径/理由/owner doc 指针/裁决来源）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）；四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）
      - Skill: none
      - 证据：hr WHITELIST 条目 = **1 条**（<3 条 → 全数核对并记录条目数）：`cjk-baseline.md` L298-302 `ErpHrConstants.java` 四要素齐备——①文件路径 ②理由（`HR_ROLE_ID="HR 专员"` seed 角色名数据契约，运行期匹配 nop_auth_role.csv roleId 与 erp-hr.action-auth.xml roles 字面，C2 窄类豁免）③owner doc 指针（i18n-compliance.md 判定准绳表 #5 + CAT-3 行 C2②）④裁决来源（plan 2026-09-07-1715-1 Phase 2）——实仓复核 `ErpHrConstants` L293 `HR_ROLE_ID = "HR 专员"` 与登记一致零缺陷；`grep -L "@Locale"` 全量 `*Errors.java` = **0/21 缺失**（`ErpHrErrors` @Locale 在位）；`git status --porcelain 'module-*/erp-*-meta/**'` + `_vfs/i18n` = 空（零手改）

Exit Criteria:

- [x] U14×I×hr-1 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
      - verdict = **pass**：零回归（strict PASS + self-test PASS + hr 探针族清零维持）+ 白名单 1/1 四要素齐备（条目数 1 在案记录）；详见 `ck-hr-org-r3.md` §覆盖矩阵 DIM-I 行
- [x] 白名单四要素核对 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-hr-org-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-hr-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 证据（2026-09-08 汇总复裁决）：新立 **0** 条（五格无超 r1 在册形态的新发缺陷，无 `-r3` 后缀 ID，历史 17 ID 零覆写）；复用 2（P1-CK-hr-001 / P2-CK-hr-004——级别与 r1 登记一致，前者索引行状态 open=回填缺口注记落报告 §2.1）；归并 13（P2-002/003/005 + P3-006..008/010..016——ID 与级别逐条对照跨轮索引 §Finding 追踪 L386-402 一致，P2-CK-hr-003 追加 DIM-T 测试缺位证据）；范围外注记 2（P3-CK-hr-009/017 招聘 BizModel 面维持登记态）；DIM-F 孪生文件面归并 P3-CK-mfg-023-r3/U21 裁决；无级别升级（无 §3.1 升级触发面）
- [x] <Add> 落盘 `ck-hr-org-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - 证据：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-hr-org-r3.md` 落盘（2026-09-08）——§1 覆盖矩阵 5/5（B=finding 归并态 / F=pass / S=pass / T=pass 附注记 / I=pass）+ §2 三态裁决（复用 2 / 归并 13 / 新立 0 + 范围外 2 + 归属标注）+ §3 统计（P0=0/P1 复用1/P2 复用1+归并3/P3 归并10）+ §4 剩余风险四件套（已查/未深查/残留风险/successor 触发条件）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加本报告行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.10 行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据：本轮 `ai-check-r3-index.md` 产物清单追加 `ck-hr-org-r3.md` 行（M1.10）；跨轮 `ai-check-index.md` §报告清单追加 M1.10 行（0/0/0/0，done 括注）+ 末尾追加「r3 轮复核注记（M1.10 hr-1，HEAD f40b4bbae）」块引注记（M1.1 fin-1 同型——0 新立时归并证据以注记承载，指向原 ID 与报告 §2.2）；新立 `-r3` ID 行 = 无（0 条）
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - 证据（2026-09-08 收官实跑）：`git status --porcelain` 过滤 `module-*`/`app-erp-all` = 空（expect 空成立）；全量脏面 = 2 modified（本轮双索引）+ 3 untracked（本报告 + 姊妹计划 1454-2/3）+ 本计划注记更新——全部为审计产物面，零生产路径触碰
- [x] <Proof> 收尾回归：`mvn test -pl module-hr/erp-hr-service` 复跑全绿（审计只读不变式复证；全仓验证归收官机制）
      - Skill: none
      - 证据（2026-09-08 收官实跑）：`Tests run: 249, Failures: 0, Errors: 0, Skipped: 0` BUILD SUCCESS——与 Phase 5 首跑 249/0/0/0 一致，只读不变式复证成立

Exit Criteria:

- [x] `ck-hr-org-r3.md` 落盘且五维矩阵 5 格 verdict 完整（缺一格不算完）
- [x] 双索引行追加在案；零生产代码改动核证通过；hr service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1454-1-m110-hr-org-five-dim-audit-1-549a778d to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1454-1-m110-hr-org-five-dim-audit-1-549a778d（补 Closure Gates——按 M1.1/M1.5/M1.8 同批先例为本只读审计计划定制门控；frontmatter `status: draft` → `active`；基线引用已实仓复验——HEAD `f40b4bbae`/脏面/冻结清单 §1.5·§3.2·§3.3·§4 第 10 行·§6 勘误 E1/三项技能名/`TestErpSeedDataIntegrity`/MI 终态行数字/hr 白名单 1 条 C2/249 锚点/999+325 flux 数字逐一在盘）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-hr-org-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-hr/erp-hr-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 <域切片>」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-08.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-hr-org-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- pass test 20260908-1958-closure-r1 exit=0

> 闭包 visit 记录（2026-09-08，独立 closure audit，fresh session）：`mvn test -pl module-hr/erp-hr-service` BUILD SUCCESS（exit 0，Finished at 2026-09-08T19:58:55+08:00，Tests run: 249, Failures: 0, Errors: 0, Skipped: 0 = 计划锚点 249 零回归，与 Phase 5/7 两次全绿一致）；`node tools/check-hardcoded-cjk.mjs --strict` exit 0（0 new violations vs 冻结快照，170 baseline files）+ `--self-test` PASS——两项均闭包 visit 本 visit 实跑复证。增量口径：闭包时点工作树零生产模块改动（`git diff --name-only HEAD` 非 docs 路径 = 0），按 mission 增量构建指引以 `-pl` 定面 hr service，不清 target 全量重编；完整仓库回归归 roadmap 收官机制（Closure Gates 定制门控，MV.1 对照面 = known-good-baselines 2026-09-08 MI 终态行）。以下为执行期各 Phase 红线的 PASS 记录（转自 Phase 勾选注记，闭包 visit 复跑覆盖 `test`/`--strict`/`--self-test` 收官态等价）：

- PASS 2026-09-08（执行期 Phase 1）`bash docs/audits/nop-compliance-checker.sh`——全规则 = M0.3 快照行逐项一致零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）
- PASS 2026-09-08（执行期 Phase 1）`node tools/check-hardcoded-cjk.mjs`（report mode）——CAT-1..4 = 0/0/0/0（3430 java + 886 yaml，= MI 终态行，exit 0）
- PASS 2026-09-08（执行期 Phase 6）`node tools/check-hardcoded-cjk.mjs --strict`——exit 0（闭包 visit 已复跑同结果）
- PASS 2026-09-08（执行期 Phase 6）`node tools/check-hardcoded-cjk.mjs --self-test`——PASS 13/13（闭包 visit 已复跑同结果）
- PASS 2026-09-08（执行期 Phase 4）`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity`——Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（15:48 实跑）
- PASS 2026-09-08（执行期 Phase 3）`npm run validate:flux`——step [1/3] FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855；整体 exit 1 余项 = 325 条既有 `variant="primary"`×dropdown-button 外部漂移族（successor 在案，非本切片 finding；hr 域命中 31 条同族对账不立项）
- PASS 2026-09-08（执行期 Phase 5/7）`mvn test -pl module-hr/erp-hr-service`（×2 首跑 + 收尾复跑）——Tests run: 249, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（= cjk-baseline §批注账 MI.6 批 2 行锚点 249，零失败零回归；闭包 visit 已复跑同结果）
- PASS 2026-09-08（执行期 Phase 7）`git status --porcelain` 生产路径过滤——空（闭包 visit 复核：脏面仅 docs 审计产物面，`module-*`/`app-erp-all` 零触碰）

## Closure

Status Note: 本计划（只读五维审计切片，零生产代码改动）全部 7 Phase 执行项与退出标准 `[x]`（35/35，机械红线数字注记在案）；Closure Gates 8 项普通条目核对通过；五维矩阵 5/5 verdict 落盘（B=finding 归并态 / F=pass / S=pass / T=pass 附注记 / I=pass）+ 三态裁决（复用 2 / 归并 13 / 新立 0，历史 17 ID 零覆写）+ `ck-hr-org-r3.md` + 双索引行 + 当日日志条目均在案。ledger 协议：frontmatter `status: active` 保持，完成态由全勾选 + `## Verification` pass 线 + 本节回执派生。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计子代理（fresh session、read-only、非执行者上下文；mission-driver 流 CLOSURE_SCRIPT_CHECK → 闭包 visit 单一独立 closer）
- Evidence: 本计划 Phase 1~7 勾选注记（含全部红线数字）+ `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-hr-org-r3.md`（覆盖矩阵 5/5 + 三态裁决 + 统计 + 剩余风险四件套）+ 双索引追加行（本轮 `ai-check-r3-index.md` 产物行 + 跨轮 `ai-check-index.md` §报告清单 M1.10 行与 §Finding 追踪 r3 复核注记）+ `docs/backlog/ai-check-r3-roadmap.md` M1.10 行 + `docs/logs/2026/09-08.md` M1.10 条目 + 闭包 visit 实跑记录（见 `## Verification`）

- dispatch audit #audit-20260908-1958-m110-hr-org-1-58b76b2f to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260908-1958-m110-hr-org-1-58b76b2f：独立闭包审计 ACCEPT——35/35 勾选全绿，五维矩阵 5/5 verdict 落盘（B=finding 归并态零新立 / F/S/I=pass / T=pass 附 findDepartmentTree 测试缺位注记归并 P2-CK-hr-003；三态裁决 复用 2 / 归并 13 / 新立 0，历史 17 ID 零覆写，双索引行与报告 §2 逐条吻合）；闭包 visit 实跑 `mvn test -pl module-hr/erp-hr-service` BUILD SUCCESS exit=0（249/0/0/0 = 锚点零回归）+ `--strict`/`--self-test` 双 PASS 复跑 + `git status` 生产路径零触碰核证（只读审计零改动红线保持）；语义核对（退出标准对照实仓 / anti-hollow 不适用只读面 / deferred honesty / 文档四方一致：报告↔双索引↔roadmap 行↔日志条目）全 PASS；plan-check `--strict` 结构绿、derivedCompleted 依赖本回执 + pass 线成立
