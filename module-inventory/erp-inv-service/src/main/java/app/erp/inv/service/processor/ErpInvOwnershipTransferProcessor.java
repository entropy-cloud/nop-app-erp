package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.posting.OwnershipTransferPostingDispatcher;
import app.erp.inv.service.stock.StockMoveBookkeeper;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 所有权转移单状态机 + 同库位调账 Processor（{@code processor-extension-pattern.md} Facade + Processor）。
 * Facade {@code ErpInvOwnershipTransferBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>状态机（consignment.md §ErpInvOwnershipTransfer）：{@code DRAFT → CONFIRMED → DONE}（触发同库位 StockBalance
 * ownershipType/ownerId 重分类 + 业财过账派发）；{@code DRAFT/CONFIRMED → CANCELLED}。
 *
 * <p>关键不变量（Phase 2）：{@code sourceLocId==destLocId}（物理位置不变，仅法权变更）；
 * {@code ownership-tracking-enabled=false} 时 done 抛 {@code ERR_OWNERSHIP_TRACKING_DISABLED}；
 * {@code fromOwnershipType/toOwnershipType} 与 {@code transferType} 一致性。
 *
 * <p>DONE 调账策略（Phase 2 Decision）：同库位内对 (material×warehouse×location×batch) 余额重分类——
 * 改 ownershipType/ownerId、数量不移动；启用 owner 维度则按 ownerId 拆出独立子余额行。
 */
public class ErpInvOwnershipTransferProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    StockMoveBookkeeper bookkeeper;

    @Inject
    OwnershipTransferPostingDispatcher postingDispatcher;

    public ErpInvOwnershipTransfer confirm(Long transferId, IServiceContext context) {
        ErpInvOwnershipTransfer transfer = requireTransfer(transferId, context);
        assertStatus(transfer, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT,
                ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
        validateInvariants(transfer);
        transfer.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
        transferDao().saveOrUpdateEntity(transfer);
        return transfer;
    }

    public ErpInvOwnershipTransfer done(Long transferId, IServiceContext context) {
        ErpInvOwnershipTransfer transfer = requireTransfer(transferId, context);
        assertStatus(transfer, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED,
                ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE);
        validateInvariants(transfer);
        // owner 维度未启用不可调账（consignment.md §配置点）
        if (!bookkeeper.isOwnershipTrackingEnabled()) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRACKING_DISABLED)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode());
        }

        List<ErpInvOwnershipTransferLine> lines = loadLines(transfer.getId());
        for (ErpInvOwnershipTransferLine line : lines) {
            reclassifyBalance(transfer, line);
        }

        transfer.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE);
        transferDao().saveOrUpdateEntity(transfer);

        // 业财过账派发（VMI_CONSUME 且 vmi-auto-generate-ap 时生成应付；过账失败不阻塞终态，见 dispatcher）
        postingDispatcher.dispatchIfApplicable(transfer, lines);
        return transfer;
    }

    public ErpInvOwnershipTransfer cancel(Long transferId, IServiceContext context) {
        ErpInvOwnershipTransfer transfer = requireTransfer(transferId, context);
        String status = transfer.getDocStatus();
        if (!Objects.equals(status, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT)
                && !Objects.equals(status, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED)) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, status)
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, "DRAFT或CONFIRMED");
        }
        transfer.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED);
        transferDao().saveOrUpdateEntity(transfer);
        return transfer;
    }

    // ---------- 步骤（protected + IServiceContext 末参，供派生覆盖） ----------

    protected void validateInvariants(ErpInvOwnershipTransfer transfer) {
        // 物理位置不变约束（consignment.md §业务规则1）
        if (!Objects.equals(transfer.getSourceLocId(), transfer.getDestLocId())) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_LOC_MISMATCH)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode())
                    .param(ErpInvErrors.ARG_SOURCE_LOC_ID, transfer.getSourceLocId())
                    .param(ErpInvErrors.ARG_DEST_LOC_ID, transfer.getDestLocId());
        }
        // 转移类型与所有权类型迁移一致性
        if (!isTypeConsistent(transfer.getTransferType(),
                transfer.getFromOwnershipType(), transfer.getToOwnershipType())) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_TYPE_INCONSISTENT)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode())
                    .param(ErpInvErrors.ARG_TRANSFER_TYPE, transfer.getTransferType())
                    .param(ErpInvErrors.ARG_FROM_OWNERSHIP_TYPE, transfer.getFromOwnershipType())
                    .param(ErpInvErrors.ARG_TO_OWNERSHIP_TYPE, transfer.getToOwnershipType());
        }
    }

    /**
     * 同库位内对该 (material×warehouse×location×batch) 的 fromOwnershipType 余额重分类为 toOwnershipType：
     * 数量守恒（不移动），改 ownershipType/ownerId。启用 owner 维度时按 ownerId 拆出独立子余额行。
     */
    protected void reclassifyBalance(ErpInvOwnershipTransfer transfer, ErpInvOwnershipTransferLine line) {
        ormTemplate.flushSession();
        ErpInvStockBalance source = findBalance(transfer, line, transfer.getFromOwnershipType());
        if (source == null) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_INSUFFICIENT)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode())
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_FROM_OWNERSHIP_TYPE, transfer.getFromOwnershipType())
                    .param(ErpInvErrors.ARG_AVAILABLE_QTY, BigDecimal.ZERO)
                    .param(ErpInvErrors.ARG_REQUIRED_QTY, line.getQuantity());
        }
        BigDecimal available = nz(source.getAvailableQuantity());
        if (available.compareTo(nz(line.getQuantity())) < 0) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_INSUFFICIENT)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode())
                    .param(ErpInvErrors.ARG_MATERIAL_ID, line.getMaterialId())
                    .param(ErpInvErrors.ARG_FROM_OWNERSHIP_TYPE, transfer.getFromOwnershipType())
                    .param(ErpInvErrors.ARG_AVAILABLE_QTY, available)
                    .param(ErpInvErrors.ARG_REQUIRED_QTY, line.getQuantity());
        }

        BigDecimal qty = nz(line.getQuantity());
        BigDecimal unitCost = nz(source.getAvgCost());
        BigDecimal movedCost = qty.multiply(unitCost);

        // 源余额扣减（数量守恒：从 fromOwnershipType 子余额移出）
        source.setTotalQuantity(nz(source.getTotalQuantity()).subtract(qty));
        source.setAvailableQuantity(nz(source.getAvailableQuantity()).subtract(qty));
        source.setTotalCost(nz(source.getTotalCost()).subtract(movedCost));
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(source);

        // 目的 ownershipType 子余额 upsert（同库位，按 toOwnershipType + ownerId 拆行）
        ErpInvStockBalance target = upsertTargetBalance(transfer, line, transfer.getToOwnershipType());
        target.setTotalQuantity(nz(target.getTotalQuantity()).add(qty));
        target.setAvailableQuantity(nz(target.getAvailableQuantity()).add(qty));
        target.setTotalCost(nz(target.getTotalCost()).add(movedCost));
        if (nz(target.getTotalQuantity()).signum() > 0) {
            target.setAvgCost(nz(target.getTotalCost()).divide(nz(target.getTotalQuantity()),
                    BigDecimal.ROUND_HALF_UP));
        }
        daoProvider.daoFor(ErpInvStockBalance.class).saveOrUpdateEntity(target);
    }

    protected ErpInvStockBalance findBalance(ErpInvOwnershipTransfer transfer,
                                             ErpInvOwnershipTransferLine line, String ownershipType) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", transfer.getOrgId()));
        q.addFilter(eq("materialId", line.getMaterialId()));
        q.addFilter(eq("warehouseId", transfer.getWarehouseId()));
        q.addFilter(eq("ownershipType", ownershipType));
        if (transfer.getSourceLocId() != null) {
            q.addFilter(eq("locationId", transfer.getSourceLocId()));
        }
        if (line.getBatchNo() != null) {
            q.addFilter(eq("batchNo", line.getBatchNo()));
        }
        // 源余额的 ownerId 取决于源 ownershipType：
        //   VMI_SUPPLIER/CUSTOMER_PROVIDED → 外部方拥有（ownerId=partnerId）
        //   OWNED/CONSIGNMENT_OUT → 己方拥有（ownerId=null）
        Long sourceOwner = resolveSourceOwner(transfer, ownershipType);
        if (sourceOwner != null) {
            q.addFilter(eq("ownerId", sourceOwner));
        } else {
            q.addFilter(eq("ownerId", null));
        }
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 源 ownershipType 的 ownerId（与 {@link #resolveTargetOwner} 互补）：
     * VMI_SUPPLIER/CUSTOMER_PROVIDED 由外部方拥有（ownerId=partnerId）；
     * OWNED/CONSIGNMENT_OUT 由己方拥有（ownerId=null）。
     */
    protected Long resolveSourceOwner(ErpInvOwnershipTransfer transfer, String fromOwnershipType) {
        if (ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER.equals(fromOwnershipType)
                || ErpInvConstants.OWNERSHIP_TYPE_CUSTOMER_PROVIDED.equals(fromOwnershipType)) {
            return transfer.getPartnerId();
        }
        return null;
    }

    protected ErpInvStockBalance upsertTargetBalance(ErpInvOwnershipTransfer transfer,
                                                     ErpInvOwnershipTransferLine line, String ownershipType) {
        ErpInvStockBalance existing = findBalance(transfer, line, ownershipType);
        if (existing != null) {
            return existing;
        }
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        ErpInvStockBalance balance = dao.newEntity();
        balance.setOrgId(transfer.getOrgId());
        balance.setMaterialId(line.getMaterialId());
        balance.setSkuId(line.getSkuId());
        balance.setWarehouseId(transfer.getWarehouseId());
        balance.setLocationId(transfer.getSourceLocId());
        balance.setBatchNo(line.getBatchNo());
        balance.setTotalQuantity(BigDecimal.ZERO);
        balance.setReservedQuantity(BigDecimal.ZERO);
        balance.setLockedQuantity(BigDecimal.ZERO);
        balance.setAvailableQuantity(BigDecimal.ZERO);
        balance.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        balance.setAvgCost(BigDecimal.ZERO);
        balance.setTotalCost(BigDecimal.ZERO);
        balance.setCurrencyId(transfer.getCurrencyId());
        balance.setOwnershipType(ownershipType);
        balance.setOwnerId(resolveTargetOwner(transfer, ownershipType));
        dao.saveEntity(balance);
        return balance;
    }

    /**
     * 目的 ownershipType 的 ownerId：
     * <ul>
     *   <li>VMI_CONSUME (VMI_SUPPLIER→OWNED)：消耗后归自有，ownerId=null。</li>
     *   <li>CONSIGNMENT_RETURN (CONSIGNMENT_OUT→OWNED)：回收归自有，ownerId=null。</li>
     *   <li>OWNERSHIP_TO_CUSTOMER (OWNED→CUSTOMER_PROVIDED)：转客户，ownerId=partnerId。</li>
     * </ul>
     */
    protected Long resolveTargetOwner(ErpInvOwnershipTransfer transfer, String toOwnershipType) {
        if (ErpInvConstants.OWNERSHIP_TYPE_CUSTOMER_PROVIDED.equals(toOwnershipType)
                || ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER.equals(toOwnershipType)
                || ErpInvConstants.OWNERSHIP_TYPE_CONSIGNMENT_OUT.equals(toOwnershipType)) {
            return transfer.getPartnerId();
        }
        return null;
    }

    /**
     * 转移类型与所有权类型迁移一致性（consignment.md 字典语义）。
     */
    protected boolean isTypeConsistent(String transferType, String fromType, String toType) {
        if (ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME.equals(transferType)) {
            return ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER.equals(fromType)
                    && ErpInvConstants.OWNERSHIP_TYPE_OWNED.equals(toType);
        }
        if (ErpInvConstants.TRANSFER_TYPE_CONSIGNMENT_RETURN.equals(transferType)) {
            return ErpInvConstants.OWNERSHIP_TYPE_CONSIGNMENT_OUT.equals(fromType)
                    && ErpInvConstants.OWNERSHIP_TYPE_OWNED.equals(toType);
        }
        if (ErpInvConstants.TRANSFER_TYPE_OWNERSHIP_TO_CUSTOMER.equals(transferType)) {
            return ErpInvConstants.OWNERSHIP_TYPE_OWNED.equals(fromType)
                    && ErpInvConstants.OWNERSHIP_TYPE_CUSTOMER_PROVIDED.equals(toType);
        }
        return false;
    }

    protected ErpInvOwnershipTransfer requireTransfer(Long transferId, IServiceContext context) {
        ErpInvOwnershipTransfer transfer = transferDao().getEntityById(transferId);
        if (transfer == null) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_NOT_FOUND)
                    .param("transferId", transferId);
        }
        return transfer;
    }

    protected void assertStatus(ErpInvOwnershipTransfer transfer, String expected, String actionTarget) {
        if (!Objects.equals(transfer.getDocStatus(), expected)) {
            throw new NopException(ErpInvErrors.ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS)
                    .param(ErpInvErrors.ARG_TRANSFER_CODE, transfer.getCode())
                    .param(ErpInvErrors.ARG_CURRENT_STATUS, transfer.getDocStatus())
                    .param(ErpInvErrors.ARG_EXPECTED_STATUS, expected + "（目标：" + actionTarget + "）");
        }
    }

    protected List<ErpInvOwnershipTransferLine> loadLines(Long transferId) {
        IEntityDao<ErpInvOwnershipTransferLine> dao = daoProvider.daoFor(ErpInvOwnershipTransferLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("transferId", transferId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    protected IEntityDao<ErpInvOwnershipTransfer> transferDao() {
        return daoProvider.daoFor(ErpInvOwnershipTransfer.class);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
