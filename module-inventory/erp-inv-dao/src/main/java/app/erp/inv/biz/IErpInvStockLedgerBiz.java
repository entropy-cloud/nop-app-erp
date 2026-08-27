
package app.erp.inv.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.orm.biz.ICrudBiz;

import app.erp.inv.dao.entity.ErpInvStockLedger;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IErpInvStockLedgerBiz extends ICrudBiz<ErpInvStockLedger>{

    /**
     * 库存审计快照（E3.2，`audit-snapshot-cycle-count.md` §1）：派生视图——对不可变流水
     * {@code businessDate <= asOfDate} 按余额维度聚合（等价「期初 + 截至时点流水汇总」，流水全史即期初）。
     *
     * @param warehouseId 可选仓库过滤
     * @param materialIds 可选物料集过滤
     * @param asOfDate    时点（缺省今天）；语义对齐 businessDate（AP-5）
     * @return {asOfDate, rowCount, rows[{orgId,warehouseId,locationId,materialId,skuId,batchNo,ownerId,quantity,totalCost,unitCost}], totalQuantity, totalCost}
     */
    @BizQuery
    Map<String, Object> getInventorySnapshot(@Optional @Name("warehouseId") String warehouseId,
                                             @Optional @Name("materialIds") List<String> materialIds,
                                             @Optional @Name("asOfDate") LocalDate asOfDate,
                                             IServiceContext context);

    /**
     * 「账面余额 = 快照派生值」一致性校验（E3.2 对账校验项，`domain-design-guidelines.md` 对账机制扩展；
     * `erp-inv-stock-check` 作业的 BizQuery 入口）。逐余额维度比对
     * {@code ErpInvStockBalance.totalQuantity/totalCost} vs 流水派生值，报告差异行（含单侧缺失）。
     */
    @BizQuery
    Map<String, Object> checkStockBalanceConsistency(@Optional @Name("asOfDate") LocalDate asOfDate,
                                                     IServiceContext context);
}
