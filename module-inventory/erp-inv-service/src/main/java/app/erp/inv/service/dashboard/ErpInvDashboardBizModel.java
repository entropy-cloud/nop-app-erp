package app.erp.inv.service.dashboard;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 库存看板聚合入口（{@code dashboards.md §3}）。服务型 BizObject（非实体聚合），
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合，
 * 镜像 {@code ErpFinDashboardBizModel} 范式。
 *
 * <p>KPI 口径：库存总值取自 {@link ErpInvStockBalance}（Σ totalCost）；
 * 本期出入库量取自 {@link ErpInvStockMove}（DONE 期内）关联 {@link ErpInvStockMoveLine}（Σ quantity，出库为负）；
 * 库存周转率 = 出库成本 / 平均库存（口径对齐 {@code finance/costing-methods.md}，平均库存以当前 totalCost 近似）。
 *
 * <p>预警：缺料（availableQuantity &lt; material.safetyStock）、滞销（最后出库日期 &gt; N 天 且 余量 &gt; 0）、
 * 批次效期（{@link ErpInvBatch}.expiryDate - today &lt; N 天，对齐 {@code inventory/trace-chain.md}）。
 */
@BizModel("ErpInvDashboard")
public class ErpInvDashboardBizModel {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAll();
            BigDecimal totalValue = BigDecimal.ZERO;
            for (ErpInvStockBalance b : balances) {
                totalValue = totalValue.add(nz(b.getTotalCost()));
            }

            BigDecimal[] inOut = sumMoveQtyInRange(from, to);
            BigDecimal incomingQty = inOut[0];
            BigDecimal outgoingQty = inOut[1];

            BigDecimal outgoingCost = sumOutgoingCostInRange(from, to);
            BigDecimal avgInventory = totalValue;
            // 周转率 = 出库成本 / 平均库存（平均库存以当前 totalCost 近似；为 0 时周转率 0）
            BigDecimal turnoverRate = (avgInventory != null && avgInventory.signum() > 0)
                    ? outgoingCost.divide(avgInventory, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> kpi = new LinkedHashMap<>();
            kpi.put("startDate", from);
            kpi.put("endDate", to);
            kpi.put("totalValue", totalValue);
            kpi.put("incomingQty", incomingQty);
            kpi.put("outgoingQty", outgoingQty);
            kpi.put("turnoverRate", turnoverRate);
            return kpi;
        });
    }

    @BizQuery
    public List<Map<String, Object>> getDashboardTrend(@Optional @Name("months") Integer months,
                                                        IServiceContext context) {
        int n = months == null || months <= 0 ? 12 : months;
        LocalDate today = CoreMetrics.currentDate();
        LocalDate from = today.minusMonths(n - 1L).withDayOfMonth(1);
        return ormTemplate.runInSession(session -> {
            // 月度库存价值趋势：以 StockLedger 月度净变动成本近似（incoming 正 / outgoing 负）
            List<ErpInvStockLedger> ledgers = loadLedgersInRange(from, today);
            Map<String, BigDecimal> valueByMonth = new LinkedHashMap<>();
            for (ErpInvStockLedger l : ledgers) {
                LocalDate d = l.getBusinessDate();
                if (d == null) continue;
                String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                valueByMonth.merge(key, nz(l.getTotalCost()), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                LocalDate m = from.plusMonths(i);
                String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("month", key);
                row.put("netValueChange", valueByMonth.getOrDefault(key, BigDecimal.ZERO));
                rows.add(row);
            }
            return rows;
        });
    }

    /** 仓库分布（按 warehouse 聚合 totalCost）。 */
    @BizQuery
    public List<Map<String, Object>> findWarehouseDistribution(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAll();
            Map<Long, BigDecimal> byWh = new LinkedHashMap<>();
            for (ErpInvStockBalance b : balances) {
                Long wid = b.getWarehouseId();
                if (wid == null) continue;
                byWh.merge(wid, nz(b.getTotalCost()), BigDecimal::add);
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            byWh.entrySet().stream()
                    .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("warehouseId", e.getKey());
                        row.put("totalValue", e.getValue());
                        rows.add(row);
                    });
            return rows;
        });
    }

    /** 缺料预警：StockBalance.availableQuantity < 关联 Material.safetyStock。 */
    @BizQuery
    public List<Map<String, Object>> findShortageAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAll();
            Set<Long> materialIds = new HashSet<>();
            for (ErpInvStockBalance b : balances) {
                if (b.getMaterialId() != null) materialIds.add(b.getMaterialId());
            }
            Map<Long, BigDecimal> safetyByMaterial = loadSafetyStock(materialIds);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpInvStockBalance b : balances) {
                BigDecimal safety = safetyByMaterial.get(b.getMaterialId());
                if (safety == null || safety.signum() <= 0) continue;
                if (nz(b.getAvailableQuantity()).compareTo(safety) < 0) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", b.getMaterialId());
                    row.put("warehouseId", b.getWarehouseId());
                    row.put("availableQuantity", nz(b.getAvailableQuantity()));
                    row.put("safetyStock", safety);
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * 滞销库存：最后出库日期 > N 天 且 余量 > 0。阈值 ≤0 时不触发（默认关闭）。
     */
    @BizQuery
    public List<Map<String, Object>> findSlowMovingAlert(IServiceContext context) {
        int days = AppConfig.var(
                ErpInvConstants.CONFIG_DASH_INV_SLOW_MOVING_DAYS,
                ErpInvConstants.DEFAULT_DASH_INV_SLOW_MOVING_DAYS);
        if (days <= 0) {
            return Collections.emptyList();
        }
        LocalDate cutoff = CoreMetrics.currentDate().minusDays(days);
        return ormTemplate.runInSession(session -> {
            List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAll();
            Map<Long, LocalDate> lastOutByMaterial = loadLastOutgoingDates(cutoff);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpInvStockBalance b : balances) {
                if (nz(b.getTotalQuantity()).signum() <= 0) continue;
                LocalDate lastOut = lastOutByMaterial.get(b.getMaterialId());
                // 最后出库日期早于 cutoff（或从无出库）→ 滞销
                if (lastOut == null || lastOut.isBefore(cutoff)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", b.getMaterialId());
                    row.put("warehouseId", b.getWarehouseId());
                    row.put("totalQuantity", nz(b.getTotalQuantity()));
                    row.put("lastOutDate", lastOut);
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * 批次效期预警：ErpInvBatch.expiryDate - today < N 天。阈值 ≤0 时不触发（默认关闭）。
     */
    @BizQuery
    public List<Map<String, Object>> findBatchExpiryAlert(IServiceContext context) {
        int days = AppConfig.var(
                ErpInvConstants.CONFIG_DASH_INV_BATCH_EXPIRY_DAYS,
                ErpInvConstants.DEFAULT_DASH_INV_BATCH_EXPIRY_DAYS);
        if (days <= 0) {
            return Collections.emptyList();
        }
        LocalDate today = CoreMetrics.currentDate();
        LocalDate horizon = today.plusDays(days);
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpInvBatch> dao = daoProvider.daoFor(ErpInvBatch.class);
            QueryBean q = new QueryBean();
            q.addFilter(le("expiryDate", horizon));
            List<ErpInvBatch> batches = dao.findAllByQuery(q);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpInvBatch batch : batches) {
                LocalDate exp = batch.getExpiryDate();
                if (exp == null || exp.isBefore(today)) continue;
                long remaining = ChronoUnit.DAYS.between(today, exp);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("batchNo", batch.getBatchNo());
                row.put("materialId", batch.getMaterialId());
                row.put("warehouseId", batch.getWarehouseId());
                row.put("expiryDate", exp);
                row.put("remainingDays", remaining);
                row.put("availableQuantity", nz(batch.getAvailableQuantity()));
                rows.add(row);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    /** 返回 [incomingQty, outgoingQty（绝对值）]：基于 DONE 移动单行，按 moveType 区分。 */
    private BigDecimal[] sumMoveQtyInRange(LocalDate from, LocalDate to) {
        List<ErpInvStockMove> moves = loadDoneMovesInRange(from, to);
        if (moves.isEmpty()) return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        Set<Long> moveIds = new HashSet<>();
        Set<Long> incomingMoveIds = new HashSet<>();
        Set<Long> outgoingMoveIds = new HashSet<>();
        for (ErpInvStockMove m : moves) {
            moveIds.add(m.getId());
            if (ErpInvConstants.MOVE_TYPE_INCOMING.equals(m.getMoveType())) {
                incomingMoveIds.add(m.getId());
            } else if (ErpInvConstants.MOVE_TYPE_OUTGOING.equals(m.getMoveType())) {
                outgoingMoveIds.add(m.getId());
            }
        }
        List<ErpInvStockMoveLine> lines = loadMoveLines(moveIds);
        BigDecimal incoming = BigDecimal.ZERO;
        BigDecimal outgoing = BigDecimal.ZERO;
        for (ErpInvStockMoveLine l : lines) {
            BigDecimal qty = nz(l.getQuantity());
            if (incomingMoveIds.contains(l.getMoveId())) {
                incoming = incoming.add(qty);
            } else if (outgoingMoveIds.contains(l.getMoveId())) {
                outgoing = outgoing.add(qty.abs());
            }
        }
        return new BigDecimal[]{incoming, outgoing};
    }

    private BigDecimal sumOutgoingCostInRange(LocalDate from, LocalDate to) {
        List<ErpInvStockMove> moves = loadDoneMovesInRange(from, to);
        if (moves.isEmpty()) return BigDecimal.ZERO;
        Set<Long> outgoingMoveIds = new HashSet<>();
        Set<Long> allMoveIds = new HashSet<>();
        for (ErpInvStockMove m : moves) {
            allMoveIds.add(m.getId());
            if (ErpInvConstants.MOVE_TYPE_OUTGOING.equals(m.getMoveType())) {
                outgoingMoveIds.add(m.getId());
            }
        }
        List<ErpInvStockMoveLine> lines = loadMoveLines(allMoveIds);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpInvStockMoveLine l : lines) {
            if (outgoingMoveIds.contains(l.getMoveId())) {
                sum = sum.add(nz(l.getTotalCost()));
            }
        }
        return sum;
    }

    private List<ErpInvStockMove> loadDoneMovesInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpInvConstants.DOC_STATUS_DONE));
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        return dao.findAllByQuery(q);
    }

    private List<ErpInvStockMoveLine> loadMoveLines(Set<Long> moveIds) {
        if (moveIds.isEmpty()) return Collections.emptyList();
        IEntityDao<ErpInvStockMoveLine> dao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("moveId", moveIds));
        return dao.findAllByQuery(q);
    }

    private List<ErpInvStockLedger> loadLedgersInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        QueryBean q = new QueryBean();
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        return dao.findAllByQuery(q);
    }

    private Map<Long, BigDecimal> loadSafetyStock(Set<Long> materialIds) {
        if (materialIds.isEmpty()) return Collections.emptyMap();
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", materialIds));
        Map<Long, BigDecimal> map = new HashMap<>();
        for (ErpMdMaterial m : dao.findAllByQuery(q)) {
            map.put(m.getId(), nz(m.getSafetyStock()));
        }
        return map;
    }

    /** 加载 cutoff 之后的最近出库日期，按 materialId → lastOutDate（StockMoveLine 无 warehouseId，故物料级聚合）。 */
    private Map<Long, LocalDate> loadLastOutgoingDates(LocalDate cutoff) {
        IEntityDao<ErpInvStockMove> mDao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean mq = new QueryBean();
        mq.addFilter(eq("moveType", ErpInvConstants.MOVE_TYPE_OUTGOING));
        mq.addFilter(eq("docStatus", ErpInvConstants.DOC_STATUS_DONE));
        if (cutoff != null) mq.addFilter(ge("businessDate", cutoff));
        List<ErpInvStockMove> moves = mDao.findAllByQuery(mq);
        if (moves.isEmpty()) return Collections.emptyMap();
        Set<Long> moveIds = new HashSet<>();
        for (ErpInvStockMove m : moves) moveIds.add(m.getId());
        List<ErpInvStockMoveLine> lines = loadMoveLines(moveIds);
        Map<Long, LocalDate> moveDateByMoveId = new HashMap<>();
        for (ErpInvStockMove m : moves) {
            moveDateByMoveId.put(m.getId(), m.getBusinessDate());
        }
        Map<Long, LocalDate> result = new HashMap<>();
        for (ErpInvStockMoveLine l : lines) {
            LocalDate d = moveDateByMoveId.get(l.getMoveId());
            if (d == null || l.getMaterialId() == null) continue;
            result.merge(l.getMaterialId(), d, (a, b) -> a.isAfter(b) ? a : b);
        }
        return result;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
