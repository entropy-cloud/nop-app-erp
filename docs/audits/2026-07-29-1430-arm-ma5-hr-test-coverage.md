# ARM-MA5 hr 测试覆盖深度审计报告（A5.3）— 最高风险（比 0.16 全域最低）

> 里程碑：MA5（测试层审计 / 测试覆盖深度维度）
> Roadmap 工作项：A5.3（hr 测试覆盖深度——15 测试 / 92 mutation，比 0.16 全域最低）
> Plan：`docs/plans/2026-07-29-1430-1-ma5-s-tier-test-coverage-audit.md`（Phase 3）
> 行为基线：`docs/design/human-resource/{payroll,payroll-simulation,state-machine,shift-scheduling,competency-management}.md`
> 计数基线：`docs/testing/test-depth-classification.md` + roadmap「15 测试 / 92 mutation / 0.16」
> Skill：`docs/skills/open-ended-audit-prompt.md`（项目定制化层已注入）
> 实仓快照：2026-07-29（`find module-hr/erp-hr-service/src/test -name "Test*.java"` 排除 HrFrozenClockExtension/CodeGen = **15 测试文件 / 4264 行**）
> 裁决：**Verdict = ⚠️(P1)**——hr 测试覆盖**文件数与 roadmap 一致（15）**且**零浅测**（全部 ≥100 行，4 深 + 11 中），主路径断言强度扎实（社保/公积金数值精确 2615.52/3923.28 + net 恒等式 + 累计税跨月 + 状态机 + 幂等 ErrorCode），但**全域最低比 0.16 裁决为「真实测试缺口」**（92 mutation × 15 测试，异常路径系统性零覆盖——个税高档边界/过账悬挂/累计 JSON 解析失败/公司承担过账缺失/计提从未触发 5 大异常路径零覆盖）**非纯口径偏差**（文件数准确，口径偏差仅在 test-depth-classification.md 文档层低估 5 文件）。**0.16 比根因裁决：缺口为主 + 口径为辅**——文件数 15 准确（roadmap），但 test-depth-classification.md 登记 10 文件致外部观察者误判「比更低」。零 P0（薪酬算术缺陷为响亮崩溃 NPE 非静默错算 + parseCumulativeData 静默吞需损坏 JSON 前置 + 过账缺失可经试算平衡发现）。**3 项新 P1**（P1-MA5-007 hr 计数口径文档过时[10→15] / P1-MA5-008 hr 薪酬/过账链路异常路径系统性零覆盖[MA4 P1-MA4-019 测试层投影，归并登记] / P1-MA5-009 hr 薪酬算术边界测试缺口[个税高档/社保边界/累计健壮性，MA4 P1-MA4-016/018 测试层投影，归并登记]）+ **1 项新 P2** watch-only（P2-MA5-003 hr 测试/mutation 比 0.16 测试债务全域最高——非活跃缺陷但结构性风险）。本审计与 MA4 P1-MA4-016/017/018/019 经交叉确认：P1-MA5-008/009 标注为 MA4 同根因在测试层的系统化投影，**不重复计入 MR2**。

---

## 1. 范围与计数口径对账

### 1.1 在范围

`module-hr/erp-hr-service/src/test/java/**` 全部测试文件（排除 `HrFrozenClockExtension.java` JUnit 扩展 + `ErpHrCodeGen.java` codegen 冒烟 + web 层 CodeGen/WebPagesTest）。**15 真实测试文件**。

### 1.2 计数口径对账表

| 数据源 | 口径 | hr 文件数 | 深(≥400) | 中(100-399) | 浅(<100) | 备注 |
|--------|------|---------|---------|------------|---------|------|
| **roadmap**（A5.3） | 测试/mutation | **15** 测试 / 92 mutation / 比 0.16 | — | — | — | 文件数准确 |
| **test-depth-classification.md** | 文件行数分档 | **10** | 3 | 7 | 0 | **过时**——低估 5 文件 |
| **本审计实仓实测**（2026-07-29） | 文件行数分档 | **15** | **4** | **11** | **0** | 权威值 |

**差异根因裁决**：

1. **roadmap 15 = 实测 15（一致）**——文件数准确，**比 0.16 的分子准确**。
2. **test-depth-classification.md 10 vs 实测 15（差 5）**：历史快照过时。hr 在 MA2/MA4 周期内新增 5 测试（EmployeeTransfer/CompetencyManagement/ShiftScheduling/ReportRendering/competency 子目录 2 测试），文档未刷新。深测少计 1（实测 4 vs 文档 3）。

### 1.3 0.16 比根因裁决（缺口 vs 口径）

**裁决：缺口为主 + 口径为辅（非纯口径偏差）。**

| 假设 | 证据 | 裁决 |
|------|------|------|
| **假设 A：0.16 是「mutation 统计口径偏差」**（如 mutation 重复计/含 trivial mutation） | roadmap mutation 数 92 来自 pitest 估算。hr 92 mutation > mfg 74 > assets 61，与 hr 代码复杂度（薪酬累计预扣 7 级累进 + 社保钳制 + 模拟 What-If + 排班 + 考勤 + 招聘 + 考核）一致。无证据表明 92 含大量 trivial mutation（hr 无大量 getter/setter 桩——37 BizModel 多为状态机/计算逻辑）。 | **部分否决**——mutation 数 92 基本可信（与代码复杂度匹配），口径偏差不显著 |
| **假设 B：0.16 是「真实测试缺口」** | 15 文件覆盖 hr 全子系统但**异常路径系统性零覆盖**：个税高档边界（>960000）/ 过账悬挂 / 累计 JSON 解析失败 / 公司承担过账缺失 / 计提从未触发 5 大异常路径零覆盖（MA4 P1-MA4-019 已确认）。92 mutation 中异常路径分支占高比例，15 测试仅覆盖主路径。 | **确认**——0.16 主要反映异常路径覆盖缺口 |
| **假设 C：文件数口径偏差** | roadmap 15 = 实测 15 一致；但 test-depth-classification.md 10 < 15 致外部观察者（读文档非读代码）误判「比更低（10/92=0.11）」。 | **确认（口径为辅）**——文档低估 5 致外部误判，但真实比 0.16（15/92）本身即为全域最低 |

**综合裁决**：0.16 比**文件数分子准确**，主要反映**异常路径系统性覆盖缺口**（5 大异常路径零覆盖），非纯口径偏差。test-depth-classification.md 文档低估（10→15）是**辅助因素**（致外部误判比更低至 0.11）。**hr 全域最低比 0.16 是真实测试债务**，MR3 须补异常路径测试提升覆盖。

### 1.4 不在范围

- A4.4 hr 代码质量（done）——本审计复核其测试 finding 的测试层系统化投影
- A2.7a/b hr 状态机业务正确性（done）
- 测试修复（属 MR3）

---

## 2. 关键业务路径覆盖矩阵

| 业务链路 | 测试文件 | 覆盖档 | 断言强度 | 备注 |
|---------|---------|--------|---------|------|
| **薪酬核算引擎** | TestErpHrPayrollEngine(357) | ✅ 深 | ✅ 数值精确 | 社保钳制 2615.52/公积金 3923.28 + net 恒等式 + 累计税跨月 + runPayroll 幂等 + 审批锁 + 银行文件；**个税高档边界零覆盖**（P1-MA5-009） |
| **薪酬模拟 What-If** | TestErpHrPayrollSimulation(607) | ✅ 深 | ✅ 隔离 | createSimulation/adjustItem/冲突/审批/convertToFormal；克隆源快照不污染 |
| **薪酬审批工作流** | TestErpHrSalaryWorkflowApproval(178) | 🟡 中 | 🟡 状态 | 审批工作流；**过账悬挂零覆盖**（P1-MA5-008） |
| **员工调岗** | TestErpHrEmployeeTransfer(450) | ✅ 深 | ✅ 联动 | 调岗 + 合同/部门/岗位联动 |
| **排班** | TestErpHrShiftScheduling(494) | ✅ 深 | ✅ 排班 | 排班分配 + 换班 |
| **胜任力管理** | TestErpHrCompetencyManagement(536) | ✅ 深 | ✅ 差距 | 胜任力 + 差距分析 + 发展计划 |
| **请假** | TestErpHrLeaveEngine(295) | 🟡 中 | ✅ 余额 | 请假引擎 + 余额校验 |
| **招聘** | TestErpHrRecruitmentEngine(134) | 🟡 中 | 🟡 hire | 招聘 hire 联动 |
| **考勤** | TestErpHrAttendanceEngine(113) | 🟡 中 | 🟡 打卡 | 考勤打卡 |
| **调查** | TestErpHrSurveyCrudSmoke(163) | 🟡 中 | 🟡 CRUD | 调查 CRUD 冒烟（18 行桩的冒烟） |
| **员工引用** | TestErpHrEmployeeReferences(104) | 🟡 中 | 🟡 引用 | 员工引用关系 |
| **考核聚合** | TestAssessmentAggregator(122) | 🟡 中 | ✅ 聚合 | 考核聚合 |
| **差距分析** | TestGapAnalysisCalculator(113) | 🟡 中 | ✅ 差距 | 差距分析计算 |
| **合同到期** | TestErpHrContractExpiry(240) | 🟡 中 | ✅ 批量 | 合同批量过期 job |
| **报表** | TestErpHrReportRendering(358) | 🟡 中 | 🟡 渲染 | 报表渲染 |
| **薪酬过账（计提+公司承担）** | （无） | 🔴 零测试 | — | **P1-MA4-017 三类 PostingEvent 永不生成，零测试**（P1-MA5-008） |

**覆盖矩阵裁决**：hr 8 条核心业务链路（薪酬/考勤/排班/工时/请假/合同/招聘/考核）**全部有测试覆盖**（无零覆盖主路径），但**薪酬过账（计提+公司承担）链路零测试**（功能未实现 P1-MA4-017）。

---

## 3. Assertion 强度分档分布

| 强度档 | 文件数 | 占比 | 特征 | 代表测试 |
|--------|--------|------|------|---------|
| **深断言**（数值精确/恒等式/隔离） | ~5 | 33% | 社保/公积金数值精确 + net 恒等式 + 累计税跨月 + 模拟隔离 + 调岗联动 | PayrollEngine/PayrollSimulation/EmployeeTransfer/ShiftScheduling/CompetencyManagement |
| **中断言**（状态/余额/聚合） | ~10 | 67% | 状态迁移 + 余额校验 + 聚合 + CRUD | SalaryWorkflowApproval/LeaveEngine/Recruitment/Attendance/Survey/References/Assessment/GapAnalysis/ContractExpiry/ReportRendering |
| **浅断言** | 0 | 0% | — | 零浅测（全域唯一与 assets 并列零浅测） |

**「伪覆盖」标记**：

1. **TestErpHrPayrollEngine**（357 行，薪酬核心）——社保/公积金**数值精确断言**（2615.52/3923.28）+ net 恒等式扎实，**但个税高档边界（>960000 累计）零覆盖**——所有测试员工月薪 ≤ 30000（年累计 ≤ 360000），永不触达末档 null NPE（P1-MA4-016）。**高档收入算术缺陷对测试不可见**（P1-MA5-009）。
2. **TestErpHrSalaryWorkflowApproval**（178 行）——审批工作流状态翻转，**但过账悬挂零覆盖**——markPaid 忽略 tryPostPayment 返回值致 posted=false 窗口（P1-MA2-048），无 mock post 抛异常测试（P1-MA5-008）。
3. **TestErpHrSurveyCrudSmoke**——调查 CRUD 冒烟（对应 ErpHrSurveyBizModel 18 行桩 P1-MA2-041），仅 CRUD 存在性，无状态机断言（桩治理待 MR1）。
4. **TestErpHrRecruitmentEngine/TestErpHrAttendanceEngine**——hire 联动/打卡存在性，业务规则断言较薄。

---

## 4. 负路径与错误处理覆盖

| 负路径类型 | 覆盖 | 证据 |
|-----------|------|------|
| 非法状态迁移（薪酬状态机） | ✅ 良好 | PayrollEngine testApprovalStateMachineAndPaidLock assertThrows ERR_SALARY_ILLEGAL_STATUS_TRANSITION |
| 重复薪酬幂等 | ✅ 良好 | PayrollEngine existsNonVoidSalary 幂等跳过 + ERR_SALARY_ALREADY_EXISTS |
| 日期重叠/余额不足 | ✅ 良好 | LeaveEngine ERR_LEAVE_DATE_OVERLAP/ERR_LEAVE_BALANCE_INSUFFICIENT |
| 重复打卡 | ✅ 良好 | AttendanceEngine ERR_ALREADY_CLOCKED_IN |
| **个税高档税率边界（>960000 累计）** | 🔴 零覆盖 | resolveBracket 末档 null NPE（P1-MA4-016），月薪 ≤30000 永不触达（P1-MA5-009） |
| **过账悬挂（posted=false）** | 🔴 零覆盖 | markPaid 忽略 tryPostPayment 返回值（P1-MA2-048），无 mock 测试（P1-MA5-008） |
| **累计 JSON 解析失败静默吞** | 🔴 零覆盖 | parseCumulativeData catch(Exception ignored)（P1-MA4-018），测试也静默吞（extractCumulativeData:347-355）（P1-MA5-009） |
| **公司承担社保/公积金过账缺失** | 🔴 零覆盖 | P1-MA4-017 三类 PostingEvent 永不生成，无负向断言（P1-MA5-008） |
| **计提过账（SALARY 270）从未触发** | 🔴 零覆盖 | tryPostAccrual 死代码，无 approve→断言计提凭证测试（P1-MA5-008） |

---

## 5. 与 MA2/MA4 已确认 finding 的测试背书关系

| Finding ID | 描述 | 测试背书 | 裁决 |
|-----------|------|---------|------|
| **P1-MA4-016** | 个税高档税率 NPE | 🔴 **零测试**——月薪 ≤30000 永不触达末档 null（P1-MA5-009） | 测试空洞 |
| **P1-MA4-017** | 业财过账链路不完整（计提+公司承担永不生成） | 🔴 **零测试**——功能未实现，无负向断言（P1-MA5-008） | 测试空洞 |
| **P1-MA4-018** | parseCumulativeData 静默吞致少预扣个税 | 🔴 **零测试**——测试也静默吞（extractCumulativeData）（P1-MA5-009） | 测试空洞 |
| **P1-MA4-019** | hr 薪酬/过账链路测试有效性系统性不足 | 🔴 **本审计系统化确认**——5 大异常路径零覆盖（P1-MA5-008/009 测试层投影） | 归并登记 |
| **P1-MA2-048** | 工资过账 tryPostPayment/tryPostAccrual 吞异常悬挂 | 🔴 **零测试**——markPaid 忽略返回值无 mock 测试 | 测试空洞（P1-MA5-008 归并） |
| **P1-MA2-039~046** | hr 多项死状态 + 桩治理 | 🟡 **部分触及**——状态机测试覆盖正向，死状态（员工 RESIGNED/合同 SUSPENDED/调查/发展计划/工时单/银行文件/排班）负向覆盖薄 | 测试存在但死状态未检出 |
| **A4.4**（整体） | hr 92 mutation 全域第二高 | 🔴 **本审计裁决 0.16 为真实测试缺口**——异常路径系统性零覆盖 | 0.16 裁决（§1.3） |

**背书关系裁决**：hr 7 类已确认 finding 中**全部零完整测试背书**。个税高档/过账悬挂/累计健壮/公司承担/计提 5 大异常路径系统性零覆盖，是 0.16 比全域最低的直接根因。

---

## 6. P0/P1/P2 finding 清单

### 6.1 P0 finding

**无 P0**——薪酬算术缺陷（P1-MA4-016 个税高档 NPE）为**响亮崩溃**非静默错算（NPE 立即抛出 + 事务回滚 + detectable）；parseCumulativeData 静默吞需损坏 JSON 前置；过账缺失可经试算平衡发现。测试空洞致缺陷不可见但无活跃数据破坏路径因测试缺失而恶化。

### 6.2 P1 finding（3 项）

| Finding ID | 描述 | 严重性 | 目标 MR | 与 MA4 关系 |
|-----------|------|-------|---------|------------|
| `P1-MA5-007` | **hr 计数口径文档过时（10→15）**：`docs/testing/test-depth-classification.md` 登记 hr 10 文件[深3/中7/浅0]，实测 **15 文件[深4/中11/浅0]**。低估 5 文件——MA2/MA4 周期内新增 EmployeeTransfer/CompetencyManagement/ShiftScheduling/ReportRendering/competency 2 测试未刷新。后果：外部观察者读文档误判 hr 比 = 10/92 = 0.11（比真实 0.16 更低），加重「全域最低」误判。 | major（文档完整性——致全域最低比被外部误判为更低） | MR3——刷新 test-depth-classification.md hr 行至 15[深4/中11/浅0]（与 P1-MA5-001/004/010 协同） | 独立登记 |
| `P1-MA5-008` | **hr 薪酬/过账链路异常路径系统性零覆盖**：4 类异常路径零覆盖——(a) 过账悬挂 posted=false（P1-MA2-048 markPaid 忽略 tryPostPayment 返回值）；(b) 公司承担社保/公积金过账缺失（P1-MA4-017 三类 PostingEvent 永不生成，无负向断言）；(c) 计提过账 SALARY 270 从未触发（tryPostAccrual 死代码，无 approve→断言计提凭证）；(d) 薪酬审批工作流过账成功路径仅正向（无 mock post 抛异常）。后果：过账悬挂/公司承担缺失/计提未触发 3 类缺陷回归无防护，业财不一致悬挂对测试不可见。 | major（测试空洞致业财不一致悬挂不可见——直接影响 GL + 实发工资） | MR3（归并 P1-MA4-019 + P1-MA4-017/MA2-048 测试补齐时一并闭合）——**不重复计入 MR2** | MA4 P1-MA4-019/017 测试层投影（归并） |
| `P1-MA5-009` | **hr 薪酬算术边界测试缺口**：2 类算术边界零覆盖——(a) 个税高档税率边界（>960000 累计，P1-MA4-016 末档 null NPE）——所有测试员工月薪 ≤30000 永不触达；(b) 累计 JSON 解析失败静默吞（P1-MA4-018 parseCumulativeData catch(Exception ignored) 致少预扣个税）——测试 extractCumulativeData:347-355 也静默吞致缺陷对测试不可见。后果：高档收入算术缺陷 + 少预扣个税对测试不可见，薪酬算术直接影响实发工资。 | major（测试空洞致薪酬算术 bug 不可见——直接影响实发工资/税务合规） | MR3（归并 P1-MA4-016/018 测试补齐时一并闭合）——**不重复计入 MR2** | MA4 P1-MA4-016/018 测试层投影（归并） |

### 6.3 P2 finding（1 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA5-003` | **hr 测试/mutation 比 0.16 全域最低测试债务**：92 mutation（全域第二高）× 15 测试 = 比 0.16 全域最低（hr 0.16 < assets 0.23 < mfg 0.39 < finance 0.47）。虽零浅测（深度合格）+ 主路径断言强度扎实，但 mutation 绝对数高 + 异常路径系统性零覆盖构成结构性测试债务。非活跃缺陷但未来 hr 功能扩展（如个税年度汇算/社保基数调整）无回归基线。 | watch-only，MR3——随 P1-MA5-008/009 异常路径测试补齐后比将提升；标注为结构性风险供 MR3 优先级排序 |

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——hr 测试覆盖**零浅测 + 主路径断言强度扎实**（社保/公积金数值精确 + net 恒等式 + 累计税跨月 + 模拟隔离），但**全域最低比 0.16 裁决为真实测试缺口**（异常路径系统性零覆盖）+ **计数文档过时（P1-MA5-007）+ 薪酬/过账异常路径系统性空洞（P1-MA5-008）+ 薪酬算术边界缺口（P1-MA5-009）** 三项问题需 MR3 修复。

### 7.2 P0 评估

**无 P0**——薪酬算术缺陷为响亮崩溃（NPE 非静默错算）+ 过账缺失可经试算平衡发现 + parseCumulativeData 静默吞需损坏 JSON 前置。测试空洞致缺陷不可见但无活跃数据破坏路径因测试缺失而恶化。

### 7.3 0.16 比根因裁决（缺口 vs 口径）

**裁决：缺口为主 + 口径为辅。**

- **缺口为主**：92 mutation × 15 测试，5 大异常路径系统性零覆盖（个税高档/过账悬挂/累计健壮/公司承担/计提），是 0.16 全域最低的直接根因。hr 代码复杂度高（薪酬累计预扣 7 级累进 + 社保钳制 + 模拟 What-If）致 mutation 绝对数高（全域第二），15 测试仅覆盖主路径。
- **口径为辅**：test-depth-classification.md 文档低估（10→15）致外部观察者误判比更低（10/92=0.11），但真实比 0.16（15/92）本身即为全域最低。
- **非纯口径偏差**：mutation 数 92 与 hr 代码复杂度匹配，无大量 trivial mutation 证据。

### 7.4 与 MA4 交叉去重

- **P1-MA5-007**（计数文档过时）独立登记 MR3
- **P1-MA5-008**（过账异常路径空洞）= P1-MA4-019/017 + P1-MA2-048 测试层投影，**归并不重复计入 MR2**
- **P1-MA5-009**（算术边界缺口）= P1-MA4-016/018 测试层投影，**归并不重复计入 MR2**

**hr 域 MA5 测试覆盖深度终态：3 P1（1 独立 + 2 归并）+ 1 P2，零 P0。** roadmap A5.3 推进至 ready（待独立 closure audit）。
