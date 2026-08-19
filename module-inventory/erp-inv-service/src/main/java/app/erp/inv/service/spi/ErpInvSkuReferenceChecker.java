package app.erp.inv.service.spi;

import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;
import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;
import app.erp.inv.dao.entity.ErpInvPickingOrderLine;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvSerialNumber;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.dao.entity.ErpInvStockTakeLine;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.gt;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * SKU 业务单据引用检查 SPI 生产实现——inventory 域（RC-R1.72，plan 2026-08-19-0445-1 Phase 2 D3）。
 *
 * <p>端口 {@link IErpMdSkuReferenceChecker} 声明在 master-data，本实现落在 inventory-service
 * （inventory → master-data 为合法 DAG 边；多域聚合经 md 侧 {@code ErpMdSkuReferenceCheckerRegistry}
 * List 收集器承接）。
 *
 * <p><b>活跃引用口径（D3）</b>——开放单据行 + 在手量 + 活跃配置三类：
 * <ul>
 *   <li>StockBalance.totalQuantity ≠ 0（在手量）；</li>
 *   <li>ReservationLine 经 reservation.status ∈ {OPEN, PARTIALLY_CONSUMED} 且 reservedQuantity &gt; 0；</li>
 *   <li>CostLayer.remainingQuantity &gt; 0（未耗尽成本层）；</li>
 *   <li>Batch.status = OPEN 且 availableQuantity &gt; 0；</li>
 *   <li>SerialNumber.status ∈ {IN_STOCK, RESERVED}；</li>
 *   <li>StockMoveLine/TransferOrderLine/StockTakeLine/OwnershipTransferLine 经 header docStatus ∈ {DRAFT, CONFIRMED}；</li>
 *   <li>PickingOrderLine 经 picking.docStatus ∈ {PENDING, PICKING}；</li>
 *   <li>StockLedger 不可变历史不阻断（UC-MD-06④ 历史完整）。</li>
 * </ul>
 * E3 自检：daoFor 均为 inventory 同域实体（SPI 只读 exists 判定，fin employee checker 同型先例）；
 * exists 经 limit 1 查询非全量加载；header 过滤经关联属性路径（DAO 层查询翻译）。
 */
public class ErpInvSkuReferenceChecker implements IErpMdSkuReferenceChecker {

    private static final List<String> OPEN_MOVE_STATUSES =
            List.of(ErpInvDaoConstants.MOVE_STATUS_DRAFT, ErpInvDaoConstants.MOVE_STATUS_CONFIRMED);
    private static final List<String> OPEN_OWNERSHIP_TRANSFER_STATUSES =
            List.of(ErpInvDaoConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT,
                    ErpInvDaoConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
    private static final List<String> OPEN_PICKING_STATUSES =
            List.of(ErpInvDaoConstants.PICKING_STATUS_PENDING, ErpInvDaoConstants.PICKING_STATUS_PICKING);
    private static final List<String> OPEN_RESERVATION_STATUSES =
            List.of(ErpInvDaoConstants.RESERVATION_STATUS_OPEN,
                    ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED);
    private static final List<String> ACTIVE_SERIAL_STATUSES =
            List.of(ErpInvDaoConstants.SERIAL_STATUS_IN_STOCK, ErpInvDaoConstants.SERIAL_STATUS_RESERVED);

    @Inject
    IDaoProvider daoProvider;

    @Override
    public boolean isReferencedByBill(ErpMdMaterialSku sku) {
        if (sku == null || sku.getId() == null) {
            return false;
        }
        Long skuId = sku.getId();
        return existsStockBalance(skuId)
                || existsReservationLine(skuId)
                || existsCostLayer(skuId)
                || existsBatch(skuId)
                || existsSerialNumber(skuId)
                || existsStockMoveLine(skuId)
                || existsTransferOrderLine(skuId)
                || existsStockTakeLine(skuId)
                || existsOwnershipTransferLine(skuId)
                || existsPickingOrderLine(skuId);
    }

    private boolean existsStockBalance(Long skuId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("totalQuantity", BigDecimal.ZERO));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsReservationLine(Long skuId) {
        IEntityDao<ErpInvReservationLine> dao = daoProvider.daoFor(ErpInvReservationLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("reservation.status", OPEN_RESERVATION_STATUSES));
        q.addFilter(gt("reservedQuantity", BigDecimal.ZERO));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsCostLayer(Long skuId) {
        IEntityDao<ErpInvCostLayer> dao = daoProvider.daoFor(ErpInvCostLayer.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(gt("remainingQuantity", BigDecimal.ZERO));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsBatch(Long skuId) {
        IEntityDao<ErpInvBatch> dao = daoProvider.daoFor(ErpInvBatch.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(eq("status", ErpInvDaoConstants.BATCH_STATUS_OPEN));
        q.addFilter(gt("availableQuantity", BigDecimal.ZERO));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsSerialNumber(Long skuId) {
        IEntityDao<ErpInvSerialNumber> dao = daoProvider.daoFor(ErpInvSerialNumber.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("status", ACTIVE_SERIAL_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsStockMoveLine(Long skuId) {
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("move.docStatus", OPEN_MOVE_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsTransferOrderLine(Long skuId) {
        IEntityDao<ErpInvTransferOrderLine> dao = daoProvider.daoFor(ErpInvTransferOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("transfer.docStatus", OPEN_MOVE_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsStockTakeLine(Long skuId) {
        IEntityDao<ErpInvStockTakeLine> dao = daoProvider.daoFor(ErpInvStockTakeLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("take.docStatus", OPEN_MOVE_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsOwnershipTransferLine(Long skuId) {
        IEntityDao<ErpInvOwnershipTransferLine> dao = daoProvider.daoFor(ErpInvOwnershipTransferLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("transfer.docStatus", OPEN_OWNERSHIP_TRANSFER_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsPickingOrderLine(Long skuId) {
        IEntityDao<ErpInvPickingOrderLine> dao = daoProvider.daoFor(ErpInvPickingOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("picking.docStatus", OPEN_PICKING_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }
}
