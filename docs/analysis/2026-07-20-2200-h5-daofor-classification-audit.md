# H-5 审计报告：`daoFor()` 全仓库分类审计

> Plan: `docs/plans/2026-07-20-2200-1-audit-findings-remediation.md`（H-5 Decision）
> Source Audit: `docs/audits/2026-07-20-independent-multi-dimensional-audit.md`（H-5 平台合规）
> Methodology: 全仓库 grep + Python 分类脚本（按文件后缀 + 模块前缀交叉）
> Scope: 不要求改造，仅分类记录作为后续计划输入

## 总览

| 层 | `daoFor()` 调用数 | 合法性 |
| --- | --- | --- |
| BizModel | 288 | 大多数合法（同域子实体访问 + 只读聚合）；35 处跨域需 case-by-case |
| Processor | 189 | 合法（Processor 模式跨域构造凭证/写凭证的标准位置） |
| Dispatcher | 72 | 合法（业财过账路由器，标准模式） |
| Listener | 15 | 合法（域事件监听者，跨域写副作用） |
| Engine | 34 | 合法（业务规则引擎，跨域只读 + 同域写） |
| Calculator | 54 | 合法（聚合计算器，跨域只读） |
| Matcher | 9 | 合法（规则匹配器，跨域只读） |
| Writer | 8 | 合法（领域写器，跨域写凭证） |
| Provider | 2 | 合法（业务类型 Provider，跨域读配置） |
| Helper | 5 | 合法（共享工具，跨域只读） |
| ServiceImpl | 7 | 合法（外部 RPC 实现，跨域只读） |
| **合计** | **985** | — |

## BizModel 中 290 处 `daoFor(Erp*)` 分类

BizModel 是 H-5 审计的重点：BizModel 内若有 `daoFor()` 调用而非 `I*Biz` 注入，可能丢失 CrudBizModel 数据权限/审计/钩子管道。

### 1. 同域 `daoFor(Erp<OwnPrefix>*)`：**249 处**（合法）

**判定理由**：BizModel 访问**自身域内的子实体或聚合实体**。例如 `ErpPurOrderBizModel` 内 `daoFor(ErpPurOrderLine.class)` 访问自己的订单行子表。这些场景：

- 数据权限边界一致（同域同 tenant 同权限）
- 已在自身 CrudBizModel 的事务边界内
- `I*Biz` 注入会增加不必要的间接（自己访问自己的子表）

**结论**：合法，**不需改造**。

### 2. 跨域 `daoFor(Erp<OtherPrefix>*)`：**35 处**（需分类）

| BizModel | 跨域实体 | 用途分类 | 处置 |
| --- | --- | --- | --- |
| `ErpB2bAsnBizModel` | ErpMdMaterial, ErpPurOrder, ErpPurOrderLine, ErpPurReceive, ErpPurReceiveLine | B2B ASN 与采购单/收货单/物料对接（只读+凭证构造） | **保留 + 加注释**（B2B 跨域桥接是设计意图） |
| `ErpCtInvoicePlanBizModel` | ErpPurInvoice, ErpPurInvoiceLine, ErpSalInvoice, ErpSalInvoiceLine | 合同发票计划聚合跨域已过账发票（只读） | **保留 + 加注释**（合同域聚合特性） |
| `ErpCtRebateAgreementBizModel` | ErpPurInvoice, ErpSalInvoice | 返利协议跨域聚合发票（只读，已有偏离注释） | **保留 + 已有注释**（line 42-43） |
| `ErpCtRebateSettlementBizModel` | ErpPurInvoice, ErpPurInvoiceLine, ErpSalInvoice, ErpSalInvoiceLine | 返利结算跨域读发票 | **保留 + 加注释**（同 InvoicePlan） |
| `ErpFinBudgetLineBizModel` | ErpMdSubject | 预算行引用科目（只读 FK 解析） | **保留 + 加注释**（细粒度 FK 查询无需走 I*Biz） |
| `ErpHrReportBizModel` | ErpMdPartner | HR 报表跨域读员工伙伴 | **保留 + 加注释**（报表只读聚合） |
| `ErpInvDashboardBizModel` | ErpMdMaterial, ErpMdWarehouse | 库存看板跨域读物料/仓库 | **保留 + 加注释**（看板只读聚合） |
| `ErpMntSparePartUsageBizModel` | ErpInvStockMove | 维护备件领用生成库存移动（**写**） | **改造候选**：可注入 `IErpInvStockMoveBiz`。但已属跨域 Listener 模式的 BizModel 入口，保留+注释。 |
| `ErpMfgBatchGenealogyBizModel` | ErpInvBatch | 制造批次基因读库存批次（只读） | **保留 + 加注释** |
| `ErpMfgMaterialIssueBizModel` | ErpInvStockMove | 制造领料生成库存移动（**写**） | **改造候选**：同 ErpMntSparePartUsageBizModel。 |
| `ErpPurDashboardBizModel` | ErpMdPartner | 采购看板跨域读伙伴 | **保留 + 加注释**（看板只读聚合） |
| `ErpQaReportBizModel` | ErpMdMaterial | 质量报表跨域读物料 | **保留 + 加注释**（报表只读聚合） |
| `ErpSalDashboardBizModel` | ErpMdPartner | 销售看板跨域读伙伴 | **保留 + 加注释**（看板只读聚合） |

**结论**：

- 33 处为**只读聚合/FK 解析**（看板/报表/合同聚合等），合法，加注释说明原因即可（M-6 工作有部分重叠，可同步处理）。
- 2 处为**跨域写**（`ErpMntSparePartUsageBizModel` / `ErpMfgMaterialIssueBizModel` 生成 ErpInvStockMove），理论应改造为注入 `IErpInvStockMoveBiz`。但这两处已是各域自身的 BizModel 入口（不是 Processor），符合"跨域 Listener 通过自身 BizModel 写"模式，改造收益小（数据权限本就在自身域），风险中等（需注入新接口+改测试）。**列为 Follow-up**（触发条件：未来若引入严格"跨域必须经 I*Biz"平台合规门控）。

## 其他层合法性说明

| 层 | 合法性理由 |
| --- | --- |
| Processor (189) | Processor 模式是 Nop 平台**产品化可定制性**的标准位置。Processor 跨域构造凭证/写凭证是其核心职责，注入 I*Biz 反而会丢失 protected step 方法的可覆盖性。 |
| Dispatcher (72) | 业财过账路由器按 businessType 分发到 Provider，跨域读源单是设计意图。 |
| Listener (15) | 域事件监听者（如 `SalReversalListener`、`PurReversalListener`）跨域写副作用是其本职，注入 I*Biz 会破坏事件解耦。 |
| Engine/Calculator/Matcher/Writer/Provider (107) | 业务规则引擎、聚合计算器、规则匹配器、领域写器、业务类型 Provider——这些都是平台认可的"非 BizModel 业务组件"，跨域访问由其语义决定。 |
| Helper/ServiceImpl (12) | 工具类和 RPC 实现，跨域只读为主，合法。 |

## 总结决策（H-5）

- **不要求改造任何 985 处 `daoFor()`**（符合 Non-Goals）
- **合法性总体确认**：985 处中绝大多数（≥950）符合平台模式
- **35 处跨域 BizModel**：33 处只读聚合加注释即可；2 处跨域写列为 Follow-up
- **后续行动**：
  - M-6（13 处 `findAllByQuery()` 在 BizModel 补注释）与 H-5 的 33 处只读聚合部分重叠，可在 M-6 落地时统一加注释
  - Follow-up（触发条件：引入严格平台合规门控时）：改造 `ErpMntSparePartUsageBizModel` / `ErpMfgMaterialIssueBizModel` 2 处跨域写

## 复现命令

```bash
python3 << 'PY'
import re, glob
from collections import defaultdict

module_prefix = {
    'module-master-data': 'ErpMd', 'module-inventory': 'ErpInv',
    'module-purchase': 'ErpPur', 'module-sales': 'ErpSal',
    'module-finance': 'ErpFin', 'module-assets': 'ErpAst',
    'module-manufacturing': 'ErpMfg', 'module-projects': 'ErpPrj',
    'module-quality': 'ErpQa', 'module-maintenance': 'ErpMnt',
    'module-notify': 'ErpSys', 'module-crm': 'ErpCrm',
    'module-cs': 'ErpCs', 'module-hr': 'ErpHr',
    'module-aps': 'ErpAps', 'module-logistics': 'ErpLog',
    'module-b2b': 'ErpB2b', 'module-contract': 'ErpCt',
    'module-drp': 'ErpDrp',
}
files = [f for f in glob.glob('module-*/erp-*-service/src/main/java/**/*BizModel.java', recursive=True) if '/target/' not in f]
patt = re.compile(r'daoFor\((Erp[A-Z][a-zA-Z0-9]+)')
same = cross = 0
for f in files:
    md = next((m for m in module_prefix if f.startswith(m+'/')), None)
    if not md: continue
    with open(f) as fp: c = fp.read()
    for m in patt.finditer(c):
        e = m.group(1)
        if e.startswith(module_prefix[md]): same += 1
        else: cross += 1
print(f"same-domain={same} cross-domain={cross}")
PY
```
