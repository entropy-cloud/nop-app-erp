
package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvReservationBiz;
import app.erp.inv.biz.ReservationConsumeLine;
import app.erp.inv.biz.ReservationConsumeRequest;
import app.erp.inv.biz.ReservationCreateRequest;
import app.erp.inv.biz.ReservationLineRequest;
import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.service.stock.StockMoveBookkeeper;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;

/**
 * 库存预留单 BizModel（Facade + 实现）。除标准 CRUD 外，落地跨域物料预留写路径三方法
 * （{@code IErpInvReservationBiz} purpose-built 契约，mfg 工单审核/取消/完工/领料消费侧调用）：
 *
 * <ul>
 *   <li>{@link #createReservation}：建头（status=OPEN）+ 行（reservedQuantity = min(需求, 可用量)），
 *       行维度库存余额 {@code reservedQuantity += 实际预留量}（经 {@link StockMoveBookkeeper#updateBalanceWithRetry}
 *       乐观锁 + 重试——对齐 stock move 预留写并发防护，满足 A4.2.3 SP-3 跨工单并发 lost-update 义务）。</li>
 *   <li>{@link #releaseReservation}：释放未消耗部分 + 库存余额 {@code reservedQuantity -=}，
 *       头 status 按 D2 裁决映射（CANCELLED / CONSUMED / PARTIALLY_CONSUMED）。</li>
 *   <li>{@link #consumeReservation}：领料消耗（consumedQuantity+= / reservedQuantity-= / 库存余额.预留量-=），
 *       超出未消耗量部分按 min 语义封顶（超预留警告由 mfg 侧按 D1 裁决）。</li>
 * </ul>
 *
 * <p>D4 裁决（选项 A 落地）：直接在 BizModel 实现（对齐 {@code ErpInvStockMoveBizModel} 委托范式），
 * 余额写复用既有 {@link StockMoveBookkeeper} 封装入口——预留写路径单一职责在库存侧，不拆 per-mutation Processor
 * （跨域调用方是 mfg 单侧，非多业务单据复用；若未来多业务单据复用预留写再升 Processor）。
 *
 * <p>事务边界：@BizMutation 自动包装（facade 入口）。
 */
@BizModel("ErpInvReservation")
public class ErpInvReservationBizModel extends CrudBizModel<ErpInvReservation> implements IErpInvReservationBiz{

    private static final Logger LOG = LoggerFactory.getLogger(ErpInvReservationBizModel.class);

    private static final String CODE_PREFIX = "RSV-";

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    StockMoveBookkeeper bookkeeper;

    public ErpInvReservationBizModel(){
        setEntityName(ErpInvReservation.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvReservation createReservation(@Name("request") ReservationCreateRequest request,
                                               IServiceContext context) {
        if (request == null) {
            return null;
        }
        // 幂等：同 (sourceBillType, sourceBillCode) 已存在未取消预留 → 返回既有头（不重复预留/占用）
        ErpInvReservation existing = findHeader(request.getSourceBillType(), request.getSourceBillCode());
        if (existing != null && !Objects.equals(existing.getStatus(), ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED)) {
            LOG.info("createReservation 幂等命中：sourceBillType={}, sourceBillCode={}, reservationId={}",
                    request.getSourceBillType(), request.getSourceBillCode(), existing.getId());
            return existing;
        }

        IEntityDao<ErpInvReservation> dao = reservationDao();
        ErpInvReservation header = dao.newEntity();
        header.setCode(CODE_PREFIX + StringHelper.generateUUID());
        header.setOrgId(request.getOrgId());
        header.setBusinessDate(request.getBusinessDate() != null ? request.getBusinessDate() : CoreMetrics.today());
        header.setSourceBillType(request.getSourceBillType());
        header.setSourceBillCode(request.getSourceBillCode());
        header.setReservedForPartnerId(request.getReservedForPartnerId());
        header.setRemark(request.getRemark());
        header.setStatus(ErpInvDaoConstants.RESERVATION_STATUS_OPEN);
        dao.saveEntity(header);

        List<ReservationLineRequest> lineReqs = request.getLines();
        if (lineReqs != null) {
            int lineNo = 1;
            for (ReservationLineRequest lineReq : lineReqs) {
                createReservationLine(header, lineReq, lineNo++);
            }
        }
        return header;
    }

    /**
     * 创建单条预留行：reservedQuantity = min(requested, 可用量)，库存余额 reservedQuantity 同步增加。
     * 仓库缺失或需求非正 → 跳过该行 LOG.warn（不阻断创建）。
     */
    protected void createReservationLine(ErpInvReservation header, ReservationLineRequest lineReq, int lineNo) {
        if (lineReq == null || lineReq.getMaterialId() == null || lineReq.getWarehouseId() == null
                || lineReq.getUomId() == null) {
            LOG.warn("createReservation 跳过预留行（materialId/warehouseId/uomId 缺失）：sourceBillCode={}, lineNo={}",
                    header.getSourceBillCode(), lineNo);
            return;
        }
        BigDecimal requested = nz(lineReq.getRequestedQuantity());
        if (requested.signum() <= 0) {
            return;
        }
        ErpInvStockBalance balance = findOrNewBalance(header.getOrgId(), lineReq);
        BigDecimal available = nz(balance.getAvailableQuantity());
        BigDecimal reservedQty = requested.min(available.max(BigDecimal.ZERO));

        IEntityDao<ErpInvReservationLine> lineDao = reservationLineDao();
        ErpInvReservationLine line = lineDao.newEntity();
        line.setReservationId(header.getId());
        line.setLineNo(lineNo);
        line.setMaterialId(lineReq.getMaterialId());
        line.setSkuId(lineReq.getSkuId());
        line.setWarehouseId(lineReq.getWarehouseId());
        line.setLocationId(lineReq.getLocationId());
        line.setBatchNo(lineReq.getBatchNo());
        line.setReservedQuantity(reservedQty);
        line.setConsumedQuantity(BigDecimal.ZERO);
        line.setUomId(lineReq.getUomId());
        line.setSourceLineCode(lineReq.getSourceLineCode());
        lineDao.saveEntity(line);

        if (reservedQty.signum() > 0) {
            final BigDecimal delta = reservedQty;
            bookkeeper.updateBalanceWithRetry(balance, b -> {
                b.setReservedQuantity(nz(b.getReservedQuantity()).add(delta));
                bookkeeper.recomputeAvailable(b);
            });
        }
    }

    @Override
    @BizMutation
    public ErpInvReservation releaseReservation(@Name("sourceBillType") String sourceBillType,
                                                @Name("sourceBillCode") String sourceBillCode,
                                                @Name("reason") String reason,
                                                IServiceContext context) {
        // no-op 语义（MINOR-4）：查无预留记录 → 静默返回 null（零异常零写入）
        ErpInvReservation header = findHeader(sourceBillType, sourceBillCode);
        if (header == null) {
            return null;
        }
        List<ErpInvReservationLine> lines = loadLines(header.getId());
        BigDecimal remainingTotal = BigDecimal.ZERO;
        for (ErpInvReservationLine line : lines) {
            BigDecimal remaining = nz(line.getReservedQuantity()).subtract(nz(line.getConsumedQuantity()));
            remainingTotal = remainingTotal.add(remaining);
            if (remaining.signum() <= 0) {
                continue;
            }
            releaseBalance(header.getOrgId(), line, remaining);
            // 释放未消耗部分：reservedQuantity 归位至 consumedQuantity（行释放语义，L1 ⑦）
            line.setReservedQuantity(nz(line.getConsumedQuantity()));
            reservationLineDao().updateEntity(line);
        }
        header.setStatus(resolveReleaseStatus(reason, remainingTotal));
        reservationDao().updateEntity(header);
        return header;
    }

    /**
     * 释放语义状态映射（D2 裁决）：CANCELLED（取消全释放）→ CANCELLED；
     * COMPLETED（完工释放未领料）→ 剩余=0 已全领则 CONSUMED，有剩余释放则 PARTIALLY_CONSUMED。
     */
    protected String resolveReleaseStatus(String reason, BigDecimal remainingTotal) {
        if (ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED.equals(reason)) {
            return ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED;
        }
        if (remainingTotal.signum() == 0) {
            return ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED;
        }
        return ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED;
    }

    /** 余额预留量 -= 释放量（乐观锁 + 重试）。余额行不存在则跳过（防御，不阻断释放）。 */
    protected void releaseBalance(Long orgId, ErpInvReservationLine line, BigDecimal qty) {
        ErpInvStockBalance balance = findBalance(orgId, line.getMaterialId(), line.getSkuId(),
                line.getWarehouseId(), line.getLocationId(), line.getBatchNo());
        if (balance == null) {
            LOG.warn("releaseReservation 未找到余额行，跳过余额释放：materialId={}, warehouseId={}",
                    line.getMaterialId(), line.getWarehouseId());
            return;
        }
        final BigDecimal delta = qty;
        bookkeeper.updateBalanceWithRetry(balance, b -> {
            b.setReservedQuantity(nz(b.getReservedQuantity()).subtract(delta));
            bookkeeper.recomputeAvailable(b);
        });
    }

    @Override
    @BizMutation
    public ErpInvReservation consumeReservation(@Name("request") ReservationConsumeRequest request,
                                                IServiceContext context) {
        if (request == null) {
            return null;
        }
        // no-op 语义（MINOR-4）：查无预留记录 → 静默返回 null（零异常零写入）
        ErpInvReservation header = findHeader(request.getSourceBillType(), request.getSourceBillCode());
        if (header == null) {
            return null;
        }
        // 已取消/已过期预留不再消耗（防御：正常流程不会走到）
        if (Objects.equals(header.getStatus(), ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED)) {
            return header;
        }
        List<ErpInvReservationLine> lines = loadLines(header.getId());
        if (lines.isEmpty()) {
            return header;
        }
        List<ReservationConsumeLine> consumeLines = request.getLines();
        if (consumeLines == null) {
            return header;
        }
        boolean anyConsumed = false;
        for (ReservationConsumeLine consume : consumeLines) {
            if (consume == null || consume.getMaterialId() == null) {
                continue;
            }
            BigDecimal requested = nz(consume.getQuantity());
            if (requested.signum() <= 0) {
                continue;
            }
            BigDecimal consumed = consumeFromLines(header.getOrgId(), lines, consume, requested);
            anyConsumed = anyConsumed || consumed.signum() > 0;
        }
        if (anyConsumed) {
            header.setStatus(resolveConsumeStatus(lines));
            reservationDao().updateEntity(header);
        }
        return header;
    }

    /**
     * 按消耗行匹配预留行（materialId + warehouseId 精确优先，warehouseId 缺失/不匹配时回退 materialId-only），
     * 逐行消耗：consumedQuantity += 实耗、库存余额预留量 −= 实耗。
     * 实耗 = min(请求量, 该物料预留未消耗量=reservedQuantity−consumedQuantity)（超出封顶，D1）。
     * 行 reservedQuantity 保持初始预留量不变（L1 UC-MFG-08「释放未领料的预留(reservedQty − pickedQty)」语义载体）。
     *
     * @return 实际消耗量
     */
    protected BigDecimal consumeFromLines(Long orgId, List<ErpInvReservationLine> lines,
                                          ReservationConsumeLine consume, BigDecimal requested) {
        List<ErpInvReservationLine> matched = matchLines(lines, consume);
        if (matched.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal remainingToConsume = requested;
        BigDecimal totalConsumed = BigDecimal.ZERO;
        for (ErpInvReservationLine line : matched) {
            if (remainingToConsume.signum() <= 0) {
                break;
            }
            BigDecimal lineRemaining = nz(line.getReservedQuantity()).subtract(nz(line.getConsumedQuantity()));
            if (lineRemaining.signum() <= 0) {
                continue;
            }
            BigDecimal take = remainingToConsume.min(lineRemaining);
            line.setConsumedQuantity(nz(line.getConsumedQuantity()).add(take));
            reservationLineDao().updateEntity(line);
            consumeBalance(orgId, line, take);
            totalConsumed = totalConsumed.add(take);
            remainingToConsume = remainingToConsume.subtract(take);
        }
        return totalConsumed;
    }

    protected List<ErpInvReservationLine> matchLines(List<ErpInvReservationLine> lines,
                                                     ReservationConsumeLine consume) {
        if (consume.getWarehouseId() != null) {
            List<ErpInvReservationLine> exact = new ArrayList<>();
            for (ErpInvReservationLine line : lines) {
                if (Objects.equals(line.getMaterialId(), consume.getMaterialId())
                        && Objects.equals(line.getWarehouseId(), consume.getWarehouseId())) {
                    exact.add(line);
                }
            }
            if (!exact.isEmpty()) {
                return exact;
            }
        }
        List<ErpInvReservationLine> fallback = new ArrayList<>();
        for (ErpInvReservationLine line : lines) {
            if (Objects.equals(line.getMaterialId(), consume.getMaterialId())) {
                fallback.add(line);
            }
        }
        return fallback;
    }

    /** 余额预留量 -= 实耗（乐观锁 + 重试）。余额行不存在则跳过（防御，不阻断消耗）。 */
    protected void consumeBalance(Long orgId, ErpInvReservationLine line, BigDecimal qty) {
        ErpInvStockBalance balance = findBalance(orgId, line.getMaterialId(), line.getSkuId(),
                line.getWarehouseId(), line.getLocationId(), line.getBatchNo());
        if (balance == null) {
            LOG.warn("consumeReservation 未找到余额行，跳过余额消耗：materialId={}, warehouseId={}",
                    line.getMaterialId(), line.getWarehouseId());
            return;
        }
        final BigDecimal delta = qty;
        bookkeeper.updateBalanceWithRetry(balance, b -> {
            b.setReservedQuantity(nz(b.getReservedQuantity()).subtract(delta));
            bookkeeper.recomputeAvailable(b);
        });
    }

    /** 消耗后头状态映射（D2）：全部行已领完（consumed == reserved）→ CONSUMED；部分 → PARTIALLY_CONSUMED；
     *  零消耗保持 OPEN。 */
    protected String resolveConsumeStatus(List<ErpInvReservationLine> lines) {
        boolean anyConsumed = false;
        boolean allConsumed = true;
        for (ErpInvReservationLine line : lines) {
            BigDecimal consumed = nz(line.getConsumedQuantity());
            if (consumed.signum() > 0) {
                anyConsumed = true;
            }
            if (consumed.compareTo(nz(line.getReservedQuantity())) != 0) {
                allConsumed = false;
            }
        }
        if (!anyConsumed) {
            return ErpInvDaoConstants.RESERVATION_STATUS_OPEN;
        }
        return allConsumed ? ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED
                : ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED;
    }

    // ---------- helpers ----------

    protected ErpInvReservation findHeader(String sourceBillType, String sourceBillCode) {
        if (sourceBillType == null || sourceBillCode == null) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", sourceBillType));
        q.addFilter(eq("sourceBillCode", sourceBillCode));
        q.addOrderField("id", false);
        List<ErpInvReservation> list = reservationDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected List<ErpInvReservationLine> loadLines(Long reservationId) {
        IEntityDao<ErpInvReservationLine> dao = reservationLineDao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("reservationId", reservationId));
        q.addOrderField("lineNo", false);
        return new ArrayList<>(dao.findAllByQuery(q));
    }

    /**
     * 按预留行维度查找余额（镜像 {@link StockMoveBookkeeper#upsertBalance} 的 findBalance 语义：
     * skuId 不参与过滤，locationId/batchNo 非空才过滤，owner 维度仅 ownership-tracking-enabled 时入键），
     * 不存在则新建候选（TRANSIENT，交由 updateBalanceWithRetry 的 INSERT 路径 + UK 冲突重试落盘）。
     * 查询前 flush 使同事务内已 queue 的余额/预留量可见。
     */
    protected ErpInvStockBalance findOrNewBalance(Long orgId, ReservationLineRequest lineReq) {
        ormTemplate.flushSession();
        ErpInvStockBalance balance = findBalance(orgId, lineReq.getMaterialId(), lineReq.getSkuId(),
                lineReq.getWarehouseId(), lineReq.getLocationId(), lineReq.getBatchNo());
        if (balance != null) {
            return balance;
        }
        IEntityDao<ErpInvStockBalance> dao = balanceDao();
        ErpInvStockBalance fresh = dao.newEntity();
        fresh.setOrgId(orgId);
        fresh.setMaterialId(lineReq.getMaterialId());
        fresh.setSkuId(lineReq.getSkuId());
        fresh.setWarehouseId(lineReq.getWarehouseId());
        fresh.setLocationId(lineReq.getLocationId());
        fresh.setBatchNo(lineReq.getBatchNo());
        fresh.setTotalQuantity(BigDecimal.ZERO);
        fresh.setReservedQuantity(BigDecimal.ZERO);
        fresh.setLockedQuantity(BigDecimal.ZERO);
        fresh.setAvailableQuantity(BigDecimal.ZERO);
        fresh.setCostMethod(app.erp.inv.service.ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        fresh.setAvgCost(BigDecimal.ZERO);
        fresh.setTotalCost(BigDecimal.ZERO);
        fresh.setOwnershipType(app.erp.inv.service.ErpInvConstants.OWNERSHIP_TYPE_OWNED);
        if (bookkeeper.isOwnershipTrackingEnabled()) {
            fresh.setOwnerId(null);
        }
        return fresh;
    }

    /** 按自然键查询余额（镜像 {@link StockMoveBookkeeper#findBalance} 语义：locationId/batchNo 非空才过滤，
     *  owner 维度仅 ownership-tracking-enabled 时入键——与库存移动单路径查询口径一致）。 */
    protected ErpInvStockBalance findBalance(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                             Long locationId, String batchNo) {
        IEntityDao<ErpInvStockBalance> dao = balanceDao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        if (locationId != null) {
            q.addFilter(eq("locationId", locationId));
        }
        if (batchNo != null) {
            q.addFilter(eq("batchNo", batchNo));
        }
        if (bookkeeper.isOwnershipTrackingEnabled()) {
            q.addFilter(isNull("ownerId"));
        }
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected IEntityDao<ErpInvReservation> reservationDao() {
        return daoProvider().daoFor(ErpInvReservation.class);
    }

    protected IEntityDao<ErpInvReservationLine> reservationLineDao() {
        return daoProvider().daoFor(ErpInvReservationLine.class);
    }

    protected IEntityDao<ErpInvStockBalance> balanceDao() {
        return daoProvider().daoFor(ErpInvStockBalance.class);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
