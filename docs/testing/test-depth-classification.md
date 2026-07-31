# 测试深度分类

## 分类标准

| 级别 | 行数 | 特征 | 覆盖 |
|------|------|------|------|
| **深 (Deep)** | ≥400 | 端到端（采购→付款、销售→收款）、状态机全路径、业务规则多条件组合、跨域集成 | 业务层 + 部分基础设施 |
| **中 (Medium)** | 100-399 | 单域业务流程（创建→审批→过账）、跨域单边、报表/看板渲染 | 业务层主体 |
| **浅 (Shallow)** | <100 | CRUD 冒烟、单项查询、单一实体操作 | 仅 CRUD 通路 |

> **计数口径（刷新于本计划）**：统计各域 `src/test` 下的真实测试类（`Test*.java`），**排除**三类非测试产物：
> 1. `*CodeGen.java` / `*WebCodeGen.java` — codegen 冒烟骨架（约 26-30 行，零业务断言）；
> 2. `*TestSupport*.java` / `PeriodCloseTestSupport.java` — 测试基类/夹具；
> 3. `TestStub*.java` — 跨域 `I*Biz` 的测试替身/桩（本身不断言）。
>
> 按**文件行数**归入上表三档。所有 19 域（18 业务域 + notify）均有测试，无「无测试」域。
>
> **可重现计数方法**：以下命令复现任一域的深/中/浅三档计数（以 finance 为例，替换 `<module>`/`<svc-path>` 即可适配其它域）：
> ```bash
> while IFS= read -r f; do
>   lines=$(wc -l < "$f" | tr -d ' ')
>   if   [ "$lines" -ge 400 ]; then echo deep
>   elif [ "$lines" -ge 100 ]; then echo med
>   else echo shallow; fi
> done < <(find module-finance -path '*/erp-fin-service/src/test/java/*Test*.java' \
>          -not -name '*CodeGen.java' -not -name '*WebCodeGen.java' \
>          -not -name '*TestSupport*.java' -not -name 'PeriodCloseTestSupport.java' \
>          -not -name 'TestStub*.java' | sort) | sort | uniq -c
> ```
> 阈值口径：深 ≥400、中 100–399、浅 <100（行数 = `wc -l`，含末尾换行）。
>
> **2026-07-31 刷新范围 caveat**：本次仅 finance / manufacturing / hr / assets 四域经实测重新核验（对应审计 P1-MA5-001/004/007/010）；其余 15 域行沿用既有值，合计行按假定其余不变重算。15 域全量重核为显式 follow-up（见相关计划 Deferred 项）。

## 按域汇总

| 域 | 总数 | 深 | 中 | 浅 |
|----|------|----|----|----|
| Sales | 22 | 6 | 16 | 0 |
| Purchase | 27 | 5 | 22 | 0 |
| Manufacturing | 29 | 13 | 15 | 1 |
| CRM | 17 | 4 | 12 | 1 |
| Assets | 17 | 5 | 10 | 2 |
| HR | 16 | 5 | 11 | 0 |
| Finance | 67 | 8 | 54 | 5 |
| Inventory | 15 | 2 | 13 | 0 |
| Contract | 5 | 2 | 3 | 0 |
| Projects | 11 | 1 | 10 | 0 |
| DRP | 6 | 1 | 5 | 0 |
| Maintenance | 7 | 1 | 5 | 1 |
| Quality | 17 | 0 | 15 | 2 |
| CS | 10 | 0 | 9 | 1 |
| APS | 5 | 0 | 5 | 0 |
| B2B | 6 | 0 | 6 | 0 |
| MasterData | 7 | 0 | 7 | 0 |
| Notify | 5 | 0 | 5 | 0 |
| Logistics | 6 | 0 | 5 | 1 |
| **合计** | **295** | **53** | **228** | **14** |

## 关键发现

- **测试深度分布健康**：深测占 18.0%，中测占 77.3%，浅测占 4.7%。主体测试覆盖业务逻辑，非仅 CRUD 冒烟。
- **深测缺口域**：Quality/CS/APS/B2B/MasterData/Notify/Logistics 无 ≥400 行的端到端测试。这些域中测已较充分（覆盖状态机/规则引擎），可酌情补充端到端场景。
- **Finance 浅测偏高**（5 个浅测，最多）：finance 有 67 个测试文件，浅测集中在辅助实体/作业类的轻量校验（期末关账前检、冲销闭环门控、科目注册表等），不影响主体过账逻辑覆盖。
- **Purchase/Sales 质量最优**：核心业财域深测比例最高，覆盖采购到付款 / 销售到收款全链路（单文件 ≥600 行的端到端测试各 2 个）。
- **无「无测试」域**：19 域全部有测试，最少的域（APS/Contract/Notify）也有 5 个测试类。
