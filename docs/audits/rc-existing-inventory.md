# 存量清单导出（requirement-compliance M0.3 — 方案 B 全集 + successor 三源对账）

> Plan Status: completed
> 产出时间：2026-08-02
> 来源 Plan：`docs/plans/2026-08-02-1530-2-existing-inventory-export.md`（Work Item 0.3）
> Mission：requirement-compliance
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 + §4「显式人工批准记录」三判据 + §7 arm-index 衔接 + §去重协议）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（M0.3 工作项 + MA2 A2.1-A2.9 分区表 + MA3 A3.1-A3.5 域分组）
> 三源：源 1 = `docs/audits/arm-index.md`（587 行 finding 索引） / 源 2 = 各 owner doc 内嵌 documented simplification / Deferred 段落 / 源 3 = `docs/backlog/README.md`（既有 successor/deferred 追踪行）
> 复杂度分级来源：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §1.2`（昵称 arm-scope；按**域**评级 S/A/B/C）

## §导出口径与范围说明

### 导出口径（筛行规则，关键）

按 arm-index 各 finding 行 resolved 注记的**关闭方式标签**筛行（对齐 roadmap M0.3 详情 + 方法论 §4）：

- **保留（KEEP，方案 B 关闭项全集）**：关闭方式标签为
  - `方案 B 裁决（documented simplification）`
  - `documented simplification`
  - `Deferred`（关闭性 deferral，非 pending MR 的待修复）
  - **实质等同的「永久豁免登记」**（标签为 `resolved (plan ...: 读侧/写侧统一裁决...永久只读豁免 / 豁免补登于 posting-exemptions.md / data-dependency-matrix.md §9)`）——此类无生产代码逻辑变更以修复 finding 本身，关闭方式为「登记 governance 豁免 + 文档化」，与方法论 §4 判据 (ii)「owner doc 显式 documented simplification 标注」实质同构，属 MA2 复查范围（裁决豁免是否「有意设计 vs 静默降级」）。
- **排除（EXCLUDE）**：关闭方式标签为 `resolved (R*.n done)` / `fixed` / `done (R*.x)` / `方案 A 实现` / `实现…重构`（属**实现修复项**——经生产代码变更已修复，无「有意设计 vs 静默降级」复查面）。
- **不属本全集**：尚未关闭的 pending MR2/MR3/MR5/MR6 finding（标签 `MR2`/`MR3` 等待修复，非方案 B 关闭）。

### 范围边界（Non-Goals，对齐 plan）

- 本文件**只导出 + 分区 + 对账**，不复查方案 B 项是否正确（裁决属 MA2 A2.x）、不裁决 successor 是否该回队（属 MA3 A3.x）。
- **只读提取**：未修改 arm-index / owner doc / backlog/README 任何内容（§9 冻结条款 + 只读纪律）。对账差异只**登记**于本文件，不回写源文件。
- 不修改 ORM/api.xml/BizModel/Processor/view.xml（纯清单产出）。

### 复杂度分级（继承规则，复用 arm-scope §1.2）

每条方案 B 项按其所属域继承 S/A/B/C 等级；跨域项按主域归属（A2.x 分区）继承：

| 等级 | 域 |
|------|----|
| **S** | finance / manufacturing / hr |
| **A** | assets / purchase / sales / quality / crm / projects / cs / contract / b2b / inventory |
| **B** | master-data / maintenance / drp |
| **C** | logistics / notify / aps |

---

## §方案 B 全集清单（按 A2.x 分区 + 域复杂度 + 影响面排序）

> 共 **10 项**方案 B 关闭项。每项含：finding ID / 域 / 关闭方式标签 / owner doc 锚点 / 复杂度 / 主分区（A2.x）。

### A2.1 — finance 会计保护区域简化复查（6 项，复杂度 S）

| # | Finding ID | 域 | 关闭方式标签 | owner doc 锚点 | successor（→ MA3） | 复杂度 |
|---|-----------|----|-------------|----------------|---------------------|--------|
| 1 | `P0-MA2-018` | finance | `deferred` | `arm-index.md` P0 表；`posting-log.md §ErpFinVoucherBillR 索引与过账性能` | 字面 UK 方向 A/B/C/D（重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK，非降级） | **S** |
| 2 | `P1-MA2-001` | purchase+finance（主域 finance，会计过账本质） | `方案 B 裁决（documented simplification，非降级 deferred）` | `purchase/returns.md §暂估应付冲减「正向 receive→invoice 暂估冲回」`；`finance/posting.md §GRNI 暂估冲回 documented simplification` | 方案 A GRNI 自动冲回（需 inventory `repostPurchaseInput` SPI + 部分开票覆盖判定 + reverseApprove 反冲回钩子 + 跨期语义） | **S** |
| 3 | `P1-MA2-018` | finance | `documented simplification` | `finance/period-close.md §已知简化「年初余额非累计」` | GL 余额维护（过账引擎维护 opening/closing），使年初余额=累计期末 | **S** |
| 4 | `P1-MA2-019` | finance | `作用域修复 + documented simplification 残留` | `finance/period-close.md §已知简化「辅助账跨年对账作用域」` | 累计余额对账需 GL 余额维护 successor（同 P1-MA2-018 successor） | **S** |
| 5 | `P1-MA2-020` | finance | `documented simplification` | `finance/period-close.md §已知简化「反结账审批（kill-switch successor）」`；`finance/state-machine.md §已知限制` | 完整反结账审批流（反结账申请→审批→执行），解除条件=浏览器层 xwf 审批路径 | **S** |
| 6 | `P1-MA2-022` | finance | `documented simplification` | `finance/period-close.md §已知简化「FX 重估无前期 reversal（IAS 21 残留风险）」` | 前期 FX 凭证期末自动 reversal + 期间过滤 + 更新 `openAmountFunctional` | **S** |

### A2.2 — finance 非保护区域简化复查（1 项，复杂度 S）

| # | Finding ID | 域 | 关闭方式标签 | owner doc 锚点 | successor（→ MA3） | 复杂度 |
|---|-----------|----|-------------|----------------|---------------------|--------|
| 7 | `P1-MA1-016` | finance（→ assets 只读） | `resolved（永久只读豁免，登记于 data-dependency-matrix.md §9）` | `architecture/data-dependency-matrix.md §9` | assets 目标域子集=永久只读豁免（无 successor，永久接受）；读侧统一裁决已落地 | **S** |

### A2.3 — mfg 简化复查（1 项，复杂度 S）

| # | Finding ID | 域 | 关闭方式标签 | owner doc 锚点 | successor（→ MA3） | 复杂度 |
|---|-----------|----|-------------|----------------|---------------------|--------|
| 8 | `P1-MA2-038` | mfg | `resolved（同域委外写豁免扩展登记于 posting-exemptions.md §MrpReleaseService）` | `architecture/posting-exemptions.md §MrpReleaseService` | 收敛条件=委外域提供 purpose-built `createFromMrpLine` 时收敛为 I*Biz 调用 | **S** |

### A2.8 — 扩展域简化复查（1 项，复杂度 A）

| # | Finding ID | 域 | 关闭方式标签 | owner doc 锚点 | successor（→ MA3） | 复杂度 |
|---|-----------|----|-------------|----------------|---------------------|--------|
| 9 | `P1-MA1-029` | contract（→ pur/sal 跨域写） | `resolved（写侧豁免补登于 posting-exemptions.md §ErpCtInvoicePlanBizModel）` | `architecture/posting-exemptions.md §ErpCtInvoicePlanBizModel` | 收敛条件=pur/sal 域提供 purpose-built Facade 时收敛为 I*Biz 调用 | **A** |

### A2.9 — 跨域简化复查（1 项，复杂度 A，跨域涉及 9 域）

| # | Finding ID | 域 | 关闭方式标签 | owner doc 锚点 | successor（→ MA3） | 复杂度 |
|---|-----------|----|-------------|----------------|---------------------|--------|
| 10 | `P1-MA1-022` | pur+sal+ast+inv+mnt+prj+qa+drp+aps（**9 域合并**，跨域项主域=跨域 governance） | `resolved（读侧统一裁决：md 目标域子集=可迁移[successor 已命名] / fin·inv·mfg 目标域子集=永久只读豁免）` | `architecture/data-dependency-matrix.md §9` | md 目标域子集=可迁移（successor 已命名，触发=master-data I*Biz 补便捷只读方法）；Dashboard facade read-only 聚合永久接受 | **A**（跨域，按涉及域最高级：pur/sal/ast/inv/qa/prj=A，mnt/drp=B，aps=C） |

### A2.4 / A2.5 / A2.6 / A2.7 — 无方案 B 关闭项

> **A2.4（hr）/ A2.5（purchase+sales）/ A2.6（assets+inventory）/ A2.7（projects+quality）**：上述域的全部已关闭 P1 finding 经导出口径筛选后均为**实现修复项**（`resolved (R1.xx done)` / `fixed` / `方案 A 实现`），**0 项方案 B 关闭项**。各域 MA2 复查范围（A2.x 行）= 空，可直接标 done（无方案 B 项需复查）。各域 successor 项归 MA3 A3.x（见下）。

---

## §导出口径自检

| 自检项 | 结果 |
|--------|------|
| 筛行规则（保留标签） | `方案 B 裁决（documented simplification）` / `documented simplification` / `Deferred` / 实质等同的「永久豁免登记」（resolved + 豁免注册） |
| 筛行规则（排除标签） | `resolved (R*.n done)` / `fixed` / `done (R*.x)` / `方案 A 实现` / `实现…重构` |
| **保留项计数（方案 B 全集）** | **10**（P0×1 + P1×9） |
| 排除项计数（实现修复关闭） | arm-index 中全部以实现修复关闭的 P0/P1 finding（P0-MA1-021/MA2-016/017/019/020 + P1-MA1-001/008-015/017/018 + P1-MA2-002/003/009/017/021/023-037/039-076/077-099 + P1-MA3-001~061 + P1-MA4-001~025 + P1-MA6-001~005 + P1-MA7-001/007 等，约占已关闭 finding 的绝大多数；本清单不逐项枚举排除项，仅枚举保留项——精确逐项见 arm-index） |
| 不属本全集（pending MR，未关闭） | P1-MA5-001~012（MR3）/ P1-MA3-062（MR6）/ 其余 pending MR2-MR3 项 |
| **未分区项计数（须为 0）** | **0** ✅（10 项全部归属恰好一个 A2.x 分区，无重叠无遗漏） |
| 跨域项标注 | P1-MA2-001（purchase+finance，主域 finance）/ P1-MA1-016（finance→assets）/ P1-MA1-022（9 域）/ P1-MA1-029（contract→pur/sal）均显式标注主域 + 跨域涉及域 |

### 分区完整性校验

- A2.1（finance 会计保护区域）：6 项（P0-MA2-018, P1-MA2-001/018/019/020/022）
- A2.2（finance 非保护区域）：1 项（P1-MA1-016）
- A2.3（mfg）：1 项（P1-MA2-038）
- A2.4（hr）：0 项
- A2.5（purchase+sales）：0 项
- A2.6（assets+inventory）：0 项
- A2.7（projects+quality）：0 项
- A2.8（扩展域）：1 项（P1-MA1-029 contract）
- A2.9（跨域）：1 项（P1-MA1-022）
- **合计 = 10，无重叠无遗漏** ✅

---

## §successor 三源对账清单

> 三源：源 1 = arm-index 行内 successor/触发条件声明 / 源 2 = owner doc 内嵌 successor / Deferred 段落 / 源 3 = `docs/backlog/README.md` 既有追踪行。
> 每行含：successor 项 / 三源覆盖标记（S1/S2/S3，多源一致标 S1+S2 等）/ 触发条件摘要 / 是否已满足 / 当前归属 / 复杂度。

### successor 项（去重并集，按域分组）

#### finance 域（→ MA3 A3.1）

| successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 当前归属 | 复杂度 |
|--------------|---------|-------------|---------|---------|--------|
| GRNI 正向 receive→invoice 自动冲回（方案 A） | S1+S2 | 双向钩子[approve 红冲+reverseApprove 反冲回]+部分开票覆盖判定+跨期语义；inventory 域 `repostPurchaseInput` SPI 缺失 | ❌ 未满足（SPI 缺失） | backlog（owner doc returns.md §211 + period-close.md 已标注 successor） | S |
| GL 余额维护引擎（opening/closing） | S1+S2 | 补过账引擎 postVoucher 时维护 opening/closing 余额 | ❌ 未满足 | backlog（period-close.md §314 标注 successor） | S |
| 累计余额对账（辅助账跨年） | S1+S2 | GL 余额维护 successor 落地后 | ❌ 未满足（依赖上一项） | backlog（period-close.md §319） | S |
| 反结账完整审批流（xwf） | S1+S2 | 浏览器层 xwf 审批路径落地（`state-machine.md §已知限制`） | ❌ 未满足（2330-1 裁决 xwf 浏览器层 NOT FEASIBLE） | backlog（period-close.md §325） | S |
| FX 重估前期 reversal + 期间过滤（IAS 21 完整语义） | S1+S2 | IAS 21 完整语义需求 + config-gated 关闭默认 | ❌ 未满足 | backlog（period-close.md §336） | S |
| 凭证幂等键字面 UK 方向 A/B/C/D | S1 | 重构 billR 加判别列（acctSchemaId/postingType/isReversed）+ 对应 UK | ❌ 未满足（须 ask-first ORM） | backlog（P0-MA2-018 deferred plan，**Q4 裁决=P0 强制实现，MA2 复查重点**） | S |
| 多币种全域源币金额迁移（其余域 Provider） | S2 | 各域启用多币种业务路径时 | ❌ 未满足 | backlog（posting.md §453 `Deferred But Adjudicated`） | S |
| 凭证 `reversedVoucherId` 双向回链 | S2 | 报表需求驱动时 | ❌ 未满足 | backlog（state-machine.md §42） | S |

#### mfg + inventory + purchase 域（→ MA3 A3.2）

| successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 当前归属 | 复杂度 |
|--------------|---------|-------------|---------|---------|--------|
| 物料预留子系统完整写路径（mfg 侧） | S1+S2 | 库存域 `ErpInvReservation*` 写接口先行落地后，mfg 经 `IErpInvReservationBiz` 接线 | ❌ 未满足 | backlog（material-reservation.md §14；注：P1-MA3-042 owner doc 已标 Deferred，实现侧库存域 reservedQty 承载） | S |
| 委外单 MRP 释放收敛为 I*Biz 调用 | S1 | 委外域提供 purpose-built `createFromMrpLine` 时 | ❌ 未满足 | backlog（posting-exemptions.md §MrpReleaseService） | S |
| STANDARD 红冲成本不变量（FIFO 调整层物理删除边界） | S2 | 实际启用 FIFO 物料的成本调整红冲遇此场景时 | ❌ 未满足 | backlog（costing-methods.md §66） | A |
| 盘点自动生成盘盈/盘亏移动单 | S1 | owner doc §盘点单状态机 自动生成语义恢复 | ❌ 未满足（P1-MA2-062 经 R1.19 实现修复，但方案 B 路径未走；当前为 Deferred 手工） | backlog | A |
| 拣货单状态机（WMS） | S1 | WMS 上线时 | ❌ 未满足 | backlog（P1-MA2-063 owner doc Deferred） | A |
| master-data 跨域只读迁移（md 目标域子集） | S1 | master-data I*Biz 补便捷只读方法后迁移 | ❌ 未满足 | backlog（data-dependency-matrix.md §9） | B |
| md 目标域子集=可迁移（P1-MA1-022 子集） | S1 | md I*Biz 只读方法补齐 | ❌ 未满足 | backlog | B |

#### sales + assets + projects + quality 域（→ MA3 A3.3）

| successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 当前归属 | 复杂度 |
|--------------|---------|-------------|---------|---------|--------|
| 订单维度核销（receipt prepayment against order） | S1 | owner doc §L3 订单+发票双维度语义恢复 | ❌ 未满足 | backlog（P2-MA2-013 owner doc 标注本期仅发票维度） | A |
| 资产 IDLE 闲置状态机迁移 + 折旧扩展 | S1 | 资产暂停/恢复业务上线时 | ❌ 未满足 | backlog（P1-MA2-061 owner doc Deferred） | A |
| 工时单 approve/reject + 工时归集（projects/cost-collection） | S1 | projects 工时归集 successor | ❌ 未满足 | backlog（P1-MA2-043 owner doc Deferred） | S |
| 业务单据作废联动取消质检单 | S1 | quality 域 cancel 回调接线 | ❌ 未满足 | backlog（P1-MA2-064 owner doc §4 Deferred） | A |
| employee-id 行过滤（quality inspectorId / maintenance assignedTo） | S1 | ErpMdEmployee 增 userId 列 + 解析器（ask-first ORM） | ❌ 未满足 | backlog（P1-MA6-002 R3.4 successor） | A |

#### hr + crm + cs 域（→ MA3 A3.4）

| successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 当前归属 | 复杂度 |
|--------------|---------|-------------|---------|---------|--------|
| hr 员工离职/终止/退休/转正状态机 + 跨域联动 | S1 | HR 离职/退休业务流程落地（触 nop-auth UserAccount 保护区域） | ❌ 未满足 | backlog（P1-MA2-039 owner doc Deferred） | S |
| hr 银行文件 UPLOADED/CONFIRMED + 回单对账 | S1 | 银行回单自动对账 successor | ❌ 未满足 | backlog（P1-MA2-045 owner doc §七 Deferred） | S |
| crm stageId 单向递增守卫 + 漏斗统计 | S1 | owner doc §stageId 单向递增契约落地 | ❌ 未满足（P1-MA2-075 经 R1.24 实现修复，方案 B 路径未走） | backlog | A |
| cs NEW>1h / ASSIGNED>2h 滞留升级 + findSlaWarnings scheduler | S1 | 通知 successor（0642-1 范式） | ❌ 未满足 | backlog（P2-MA2-067 owner doc Deferred） | A |

#### 扩展域 + 跨域（→ MA3 A3.5）

| successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 当前归属 | 复杂度 |
|--------------|---------|-------------|---------|---------|--------|
| contract EXPIRED 自动到期 Job + 续期草稿 | S1 | nop-job 接线时 | ❌ 未满足（P1-MA2-071 经 R1.22 实现修复，方案 B 路径未走） | backlog | A |
| b2b EDI 出站自动化（TransportManager 接线 + ACK-timeout + 重试 + 升级） | S1+S2 | MFT transport 真实对接上线时（AS2/SFTP/FTPS） | ❌ 未满足（config-gated OFF + Mock transport） | backlog（P1-MA2-073 + managed-file-transfer.md Non-Goal） | A |
| contract InvoicePlan 跨域写收敛为 I*Biz | S1 | pur/sal 提供 purpose-built Facade 时 | ❌ 未满足 | backlog（posting-exemptions.md §ErpCtInvoicePlanBizModel） | A |
| logistics 部分签收 | S1 | 承运商支持部分签收回调时 | ❌ 未满足 | backlog（P1-MA2-079 owner doc §2/§4 Deferred） | C |
| 跨公司 orgId 隔离查询/写入（多公司部署） | S1 | 多组织部署启用时 | ❌ 未满足（单组织种子掩盖） | backlog（P1-MA2-093/094 经 R1.29 实现修复，但多公司部署侧仍 successor） | A（跨域） |
| 多账套 acctSchemaId 读路径隔离（报表/看板） | S1 | `multi-schema-enabled=true` 启用时 | ❌ 未满足 | backlog（P1-MA2-095 经 R1.29 实现修复，报表侧 successor） | S |
| 全域敏感动作 action-level RBAC（@BizAuth/FNPT） | S1 | owner doc §运行基线 灰度翻转 | ❌ 未满足（owner doc 显式声明有意默认） | backlog（P1-MA3-046 经 R2.7 部分修复，全域 RBAC successor） | A（跨域） |
| OPEN_AUDIT 轮次形式化 | S1 | 形式化 closure-pending 清理循环 | ❌ 未满足 | backlog（P1-MA6-005 R3.5 Deferred option B） | A（跨域） |

### successor 三源覆盖统计与计数口径修正

- **三源对账差异（计数口径修正）**：
  - roadmap 称「successor 计数 41 = arm-index 内嵌声明数」。实测三源对账后，**design-level successor（设计简化伴随后续）去重并集 ≈ 30+ 项**（上表逐行列出）。「41」口径包含了 P2 watch-only 项的 successor 注记 + owner-doc 交叉引用 + 已实现修复项的 successor 残留注记的**叙述性提及**，非纯净「待回队 successor」数。**MA3 应以上表去重并集为复查全集**，而非字面「41」。
  - **backlog/README（源 3）的 81 行 successor/deferred 提及**经对账为**E2E 测试覆盖 successor**（已大量 RELEASED/done）+ 部分 design successor 的叙述性提及——**与源 1/源 2 的 design-level successor 不同类别**。源 3 不产生独立 design successor，仅作覆盖交叉验证（见 §对账差异登记）。
- **三源覆盖分布**（上表 design-level successor）：
  - **S1+S2 多源一致**：GRNI 冲回 / GL 余额引擎 / 累计余额对账 / 反结账审批 / FX 重估 / 物料预留 / b2b EDI 自动化（7 项，最强证据，方法论 §4 三判据 (ii) 候选）
  - **仅 S1（arm-index 行内）**：其余多数项
  - **仅 S2（owner doc 内嵌）**：多币种全域迁移 / 凭证 reversedVoucherId / STANDARD 红冲边界（3 项，arm-index 无独立行，owner doc 内嵌 successor）
  - **仅 S3（backlog/README）**：0 项独立 design successor（源 3 全部为 E2E 测试 successor 或叙述性提及）

---

## §集成排序（MA2/MA3 可直接消费的待复查全集视图）

### MA2 A2.x 复查范围（方案 B 全集，按影响面排序）

> 排序键：①会计保护区域优先；②复杂度 S>A>B>C；③跨域最后。

| 优先级 | A2.x 行 | 复查范围（本清单对应行） | 复杂度 | 重点 |
|--------|---------|--------------------------|--------|------|
| **P1** | A2.1 | 6 项（P0-MA2-018 + P1-MA2-001/018/019/020/022） | S | 会计保护区域；**P0-MA2-018 经 Q4 裁决=P0 强制实现，重点核实字面 UK 方向 A/B/C/D**；GRNI/FX/年初余额核实方法论 §4 三判据 |
| P2 | A2.2 | 1 项（P1-MA1-016） | S | 永久只读豁免裁决合理性（governance 豁免，非会计逻辑） |
| P2 | A2.3 | 1 项（P1-MA2-038） | S | 同域委外写豁免（O-4）裁决合理性 |
| P3 | A2.8 | 1 项（P1-MA1-029） | A | 跨域写半治理豁免裁决合理性 |
| P3 | A2.9 | 1 项（P1-MA1-022，9 域） | A（跨域） | 读侧永久豁免 + md 可迁移 successor 裁决合理性 |
| — | A2.4/A2.5/A2.6/A2.7 | **0 项（空）** | — | 无方案 B 项；直接 done |

### MA3 A3.x 复查范围（successor 三源对账清单，按域分组）

| A3.x 行 | 复查范围（本清单 successor 对应域分组） | 重点 |
|---------|------------------------------------------|------|
| A3.1 finance | 8 项 successor | GL 余额引擎是 3 项 successor 的共同前置；P0-MA2-018 successor 经 Q4 须实现非 Deferred |
| A3.2 mfg+inv+pur | 7 项 successor | 物料预留是 mfg 多项前置；WMS/盘点自动 successor 触发条件核实 |
| A3.3 sal+ast+prj+qa | 5 项 successor | 订单维度核销 + 资产 IDLE + 工时归集 + 质检联动 + employee-id 行过滤 |
| A3.4 hr+crm+cs | 4 项 successor | hr 离职族（触 nop-auth 保护区域）+ crm/cs 自动化 |
| A3.5 扩展域+跨域 | 9 项 successor | 跨域 governance successor（多公司/RBAC/OPEN_AUDIT）触发条件多依赖部署 config |

---

## §对账差异登记（交 MA2/MA3 复查时关注，**不回写源文件**）

> 三源对账中发现的所有差异。MA2/MA3 复查时应逐项核实。

### 1. 关闭方式标签 vs 实质歧义（4 项，MA2 重点）

arm-index 中 4 项 finding 的关闭方式标签为 `resolved（...永久豁免登记 / 豁免补登）`，**字面非** `方案 B 裁决 / documented simplification / Deferred` 三标签之一，但**实质等同**（无生产代码逻辑变更以修复 finding 本身，关闭方式为登记 governance 豁免 + 文档化）。本清单按**实质**归入方案 B 全集（KEEP），MA2 复查应核实此归类是否恰当：

- `P1-MA1-016`（finance→assets 永久只读豁免）
- `P1-MA1-022`（9 域跨域只读，fin/inv/mfg 永久豁免 + md 可迁移）
- `P1-MA1-029`（contract→pur/sal 写侧豁免补登）
- `P1-MA2-038`（mfg 同域委外写豁免扩展）

若 MA2 裁决此归类不当（即认定为「实现修复」而非「方案 B」），则应从全集移除并视为已修复。

### 2. P0 + Q4 裁决张力（1 项，MA2 最高优先）

`P0-MA2-018`（凭证幂等键字面 UK）标签为 `deferred`（KEEP 三标签之一），但：
- (a) 它是 **P0**（非 P1）；
- (b) roadmap §当前基线「P0 deferred 边界声明」将其列为**既有 arm-index P0 deferred，不属本审计自动重开范围**；
- (c) **Q4 裁决=(a)「P0/P1 必须实现，禁止方案 B 无例外」** + 方法论 §5「技术不可行项须更深设计变更（如重构 billR 加判别列 + 对应 UK），非退缩到方案 B」。

三重张力：本清单按字面 `deferred` 标签**保留**（MA2 复查其 deferral 是否正当），但 MA2 复查结论几乎必然是「**Q4 强制实现**」→ 重开入 MR1（重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK）。MA2 应将此作为**最高优先级复查项**。

### 3. owner doc 内嵌 successor 但 arm-index 无独立行（3 项，MA3 关注）

以下 successor 在 owner doc（源 2）内嵌声明，但 arm-index 无独立 finding 行（属 P2 watch-only 或实现修复项的 successor 残留注记）：

- **多币种全域源币金额迁移**（`posting.md §453`，`Deferred But Adjudicated`）——arm-index 中 P1-MA2-002/009 已实现修复（P2P/O2C），但其余域 Provider 单币种 fallback 的全域迁移 successor 仅在 owner doc 内嵌。
- **凭证 `reversedVoucherId` 双向回链**（`state-machine.md §42`）——arm-index 无独立行（红冲闭环功能完整，仅双向回链为报表 successor）。
- **STANDARD 红冲成本不变量 FIFO 调整层边界**（`costing-methods.md §66`）——arm-index 中 P2-MA2-029 watch-only 承接。

MA3 应将此 3 项纳入 A3.x 复查，避免遗漏（owner doc 内嵌但 arm-index 无行 = 三源对账的核心价值）。

### 4. backlog/README（源 3）successor 性质差异（计数口径修正）

roadmap 称「successor 计数 41 = arm-index 内嵌声明数」与 backlog/README「81 行 successor/deferred 提及」存在**口径漂移**：

- backlog/README 的 81 行经对账为**E2E 测试覆盖 successor**（已大量 RELEASED/done，如 1249-2/1728-1/2330-2 等视觉/下载/数值断言 successor）+ 部分 design successor 的**叙述性提及**。
- **源 3 不产生独立 design-level successor**——它的「successor」与源 1/源 2 的「设计简化伴随后续」是**不同类别**。
- **修正口径**：MA3 复查全集 = 源 1+源 2 design-level successor 去重并集（上表 ≈ 30+ 项），**非**字面「41」或「81」。计数差异已在三源对账清单中消解。

### 5. 实现修复项携带的 successor 残留注记（MA3 关注，避免误纳 MA2）

多项 finding 经 `resolved (R*.n done)` 实现修复关闭，但其 owner doc / arm-index 注记仍保留 successor 触发条件（如 P1-MA2-061 IDLE / P1-MA2-064 质检联动 / P1-MA2-073 b2b 自动化 / P1-MA2-075 crm stageId / P1-MA2-093/094/095 多公司）。这些**不属 MA2 方案 B 复查**（已实现修复），但其 successor **属 MA3 复查**。本清单已在 successor 三源对账清单中正确分流（归 MA3 A3.x，不归 MA2 A2.x）。

### 6. 单源遗漏风险（MA3 关注）

- 物料预留子系统（`material-reservation.md` 整文件 Deferred）：S1（P1-MA3-042 经 R2.6 修复 owner doc 标注）+ S2（material-reservation.md §9 整节 Deferred）双源覆盖，但实现侧仅库存域 reservedQty 承载——MA3 A3.2 应核实「完整预留写路径」successor 是否仍需回队。

---

## §MA2/MA3 消费说明

### MA2（方案 B 关闭项复查）消费方式

- **MA2 A2.x 行复查范围 = §方案 B 全集清单对应分区行**（不是全 arm-index）。
- 每项逐条复核三判据（方法论 §4）：(i) plan 文件含独立 plan-audit 通过记录；(ii) owner doc 显式 documented simplification 标注且经人工批准；(iii) product-scope 范围裁剪登记。判据三仅当 (i)/(ii) 均不成立时兜底触发。
- **重新打开判据**（roadmap MA2 详情）：与 product-scope 冲突 / 影响报表·过账正确性 / 无显式人工批准记录 → 重新打开并入 MR1。
- **重点**：A2.1 的 P0-MA2-018（Q4 张力，§对账差异登记 #2）+ 5 项 finance 会计保护区域 documented simplification（方法论 §4 三判据核实）。
- **空分区**（A2.4/A2.5/A2.6/A2.7）可直接标 done（0 项方案 B）。
- **0.3 仅导出不评判**：方案 B 项是否「有意设计 vs 静默降级」的裁决属 MA2，不在本工作项范围。

### MA3（successor 复查）消费方式

- **MA3 A3.x 行复查范围 = §successor 三源对账清单对应域分组**（不是全 backlog/README）。
- 每项逐条核对：①触发条件是否已满足（已满足 → 回队 MA1/R1.0）；②是否该回队；③无触发条件的补登记；④backlog/README 既有行覆盖与正确性。
- **重点**：finance 域 GL 余额引擎是 3 项 successor 共同前置；P0-MA2-018 successor 经 Q4 须实现非 Deferred；owner doc 内嵌但 arm-index 无行的 3 项（§对账差异登记 #3）。
- **0.3 仅对账不裁决**：successor 是否该回队的裁决属 MA3，不在本工作项范围。

### 与既有审计的去重（方法论 §去重协议）

- 本清单的方案 B 项 + successor 项均源自 arm-index（audit-remediation MA1-MA7 既有 finding），**不新建 P1-RC-xxx 编号**——MA2/MA3 复查时复用既有 finding ID（在既有 arm-index 行追加 RC 交叉引用注记）。
- MA2/MA3 仅在发现**新根因/新功能点/新维度**时新建 P1-RC-xxx（RC = Requirement Compliance），须 grep arm-index 同域同控制点后给出「复用 or 新增」裁决。

---

## §范围裁剪声明（对齐 ai-autonomy-policy §保护区域）

本工作项为**只读提取 + 清单产出**，未修改任何真相源（arm-index / owner doc / backlog/README / ORM / api.xml / BizModel / Processor / view.xml）。对账差异仅登记于本文件，交 MA2/MA3 复查，不回写源文件（§9 冻结条款 + 只读纪律）。
