package app.erp.inv.service.dashboard;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Description;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
import static io.nop.api.core.beans.FilterBeans.gt;
import static io.nop.api.core.beans.FilterBeans.lt;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;
import app.erp.common.service.DashboardUtil;

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

    /** 预警扫描的服务端硬上限：StockBalance 行数封顶，防止企业级数据量 OOM（类 D 裁决保留）。 */
    private static final int ALERT_MAX_ROWS = 5000;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Description("库存看板 KPI（库存总值/数量、库位分布、呆滞与周转指标）")
    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") LocalDate startDate,
                                                @Optional @Name("endDate") LocalDate endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = CoreMetrics.currentDate();
            LocalDate from = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate to = endDate != null ? endDate : today;

            BigDecimal totalValue = sumBalanceTotalCost();

            // 单趟合并聚合（perf-ux plan 0325-2 Decision-a：原 KPI 请求内两次全量扫描
            // （sumMoveQtyInRange + sumOutgoingCostInRange 各自 loadDoneMoves+loadMoveLines 巨型 IN）
            // 合并为一次符号拆分 GROUP BY moveId 聚合 + 一次 moves 范围映射，行级 abs 求和语义
            // 经 Σ|qᵢ| = Σₘ(P(m)−N(m)) 数学等价保持；单 test 混合符号守护 testKpiOutgoingQtyMixedSign）。
            MoveAgg agg = aggDoneMoveLinesInRange(from, to);
            BigDecimal incomingQty = agg.incomingQty;
            BigDecimal outgoingQty = agg.outgoingQtyAbs;
            BigDecimal outgoingCost = agg.outgoingCost;
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
            // 月度库存价值趋势：以 StockLedger 月度净变动成本近似（incoming 正 / outgoing 负）。
            // DB 级 GROUP BY businessDate 聚合（perf-ux plan 0325-2：原 12 个月 ledger 全量实体物化
            // 改为日期维度分组聚合，行数 ≤ 区间天数；null 业务日期行跳过，与原语义一致）。
            QueryBean q = new QueryBean();
            q.setSourceName(ErpInvStockLedger.class.getName());
            q.addFilter(ge("businessDate", from));
            q.addFilter(le("businessDate", today));
            QueryFieldBean dim = QueryFieldBean.mainField("businessDate");
            QueryFieldBean sumCost = QueryFieldBean.mainField("totalCost").sum().alias("netValueChange");
            q.setFields(Arrays.asList(dim, sumCost));
            List<Map<String, Object>> aggRows = ormTemplate.findListByQuery(q);

            Map<String, BigDecimal> valueByMonth = new LinkedHashMap<>();
            for (Map<String, Object> row : aggRows) {
                Object d = row.get("businessDate");
                if (d == null) continue;
                LocalDate date = toLocalDate(d);
                String key = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                valueByMonth.merge(key, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("netValueChange"))), BigDecimal::add);
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
            // DB 级 GROUP BY warehouseId + SUM(totalCost)，避免全表物化
            QueryBean q = new QueryBean();
            q.setSourceName(ErpInvStockBalance.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("warehouseId");
            QueryFieldBean sumCost = QueryFieldBean.mainField("totalCost").sum().alias("totalValue");
            q.setFields(Arrays.asList(dim, sumCost));
            List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                if (row.get("warehouseId") == null) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("warehouseId", row.get("warehouseId"));
                r.put("totalValue", DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("totalValue"))));
                result.add(r);
            }
            result.sort(Comparator.<Map<String, Object>, BigDecimal>comparing(
                    r -> (BigDecimal) r.get("totalValue"), Comparator.reverseOrder()));
            IEntityDao<ErpMdWarehouse> whDao = daoProvider.daoFor(ErpMdWarehouse.class);
            for (Map<String, Object> r : result) {
                String wid = (String) r.get("warehouseId");
                String warehouseName = null;
                if (wid != null) {
                    ErpMdWarehouse w = whDao.getEntityById(wid);
                    warehouseName = w != null ? w.getName() : null;
                }
                r.put("warehouseName", warehouseName);
            }
            return result;
        });
    }

    /** 缺料预警：StockBalance.availableQuantity < 关联 Material.safetyStock。 */
    @BizQuery
    public List<Map<String, Object>> findShortageAlert(IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            // 类 D 裁决：逐行比对 availableQuantity vs safetyStock 需余额明细，带硬上限的受限扫描
            QueryBean q = new QueryBean();
            q.setLimit(ALERT_MAX_ROWS);
            List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
            Set<String> materialIds = new HashSet<>();
            for (ErpInvStockBalance b : balances) {
                if (b.getMaterialId() != null) materialIds.add(b.getMaterialId());
            }
            Map<String, BigDecimal> safetyByMaterial = loadSafetyStock(materialIds);
            Map<String, String> materialNames = loadMaterialNames(materialIds);
            Map<String, String> warehouseNames = loadWarehouseNamesForBalances(balances);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpInvStockBalance b : balances) {
                BigDecimal safety = safetyByMaterial.get(b.getMaterialId());
                if (safety == null || safety.signum() <= 0) continue;
                if (DashboardUtil.nz(b.getAvailableQuantity()).compareTo(safety) < 0) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", b.getMaterialId());
                    row.put("materialName", materialNames.get(b.getMaterialId()));
                    row.put("warehouseId", b.getWarehouseId());
                    row.put("warehouseName", warehouseNames.get(b.getWarehouseId()));
                    row.put("availableQuantity", DashboardUtil.nz(b.getAvailableQuantity()));
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
            // 类 D 裁决：逐行比对 totalQuantity vs 最后出库日期需余额明细，带硬上限的受限扫描
            QueryBean q = new QueryBean();
            q.setLimit(ALERT_MAX_ROWS);
            List<ErpInvStockBalance> balances = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
            Map<String, LocalDate> lastOutByMaterial = loadLastOutgoingDates(cutoff);
            Set<String> materialIds = new HashSet<>();
            Set<String> warehouseIds = new HashSet<>();
            for (ErpInvStockBalance b : balances) {
                if (DashboardUtil.nz(b.getTotalQuantity()).signum() <= 0) continue;
                if (b.getMaterialId() != null) materialIds.add(b.getMaterialId());
                if (b.getWarehouseId() != null) warehouseIds.add(b.getWarehouseId());
            }
            Map<String, String> materialNames = loadMaterialNames(materialIds);
            Map<String, String> warehouseNames = loadWarehouseNames(warehouseIds);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpInvStockBalance b : balances) {
                if (DashboardUtil.nz(b.getTotalQuantity()).signum() <= 0) continue;
                LocalDate lastOut = lastOutByMaterial.get(b.getMaterialId());
                // 最后出库日期早于 cutoff（或从无出库）→ 滞销
                if (lastOut == null || lastOut.isBefore(cutoff)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", b.getMaterialId());
                    row.put("materialName", materialNames.get(b.getMaterialId()));
                    row.put("warehouseId", b.getWarehouseId());
                    row.put("warehouseName", warehouseNames.get(b.getWarehouseId()));
                    row.put("totalQuantity", DashboardUtil.nz(b.getTotalQuantity()));
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
            Set<String> materialIds = new HashSet<>();
            Set<String> warehouseIds = new HashSet<>();
            for (ErpInvBatch batch : batches) {
                if (batch.getMaterialId() != null) materialIds.add(batch.getMaterialId());
                if (batch.getWarehouseId() != null) warehouseIds.add(batch.getWarehouseId());
            }
            Map<String, String> materialNames = loadMaterialNames(materialIds);
            Map<String, String> warehouseNames = loadWarehouseNames(warehouseIds);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpInvBatch batch : batches) {
                LocalDate exp = batch.getExpiryDate();
                if (exp == null || exp.isBefore(today)) continue;
                long remaining = ChronoUnit.DAYS.between(today, exp);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("batchNo", batch.getBatchNo());
                row.put("materialId", batch.getMaterialId());
                row.put("materialName", materialNames.get(batch.getMaterialId()));
                row.put("warehouseId", batch.getWarehouseId());
                row.put("warehouseName", warehouseNames.get(batch.getWarehouseId()));
                row.put("expiryDate", exp);
                row.put("remainingDays", remaining);
                row.put("availableQuantity", DashboardUtil.nz(batch.getAvailableQuantity()));
                rows.add(row);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    /** 合并聚合结果：入/出库量（出库为行级 abs 语义）+ 出库成本。 */
    private static final class MoveAgg {
        BigDecimal incomingQty = BigDecimal.ZERO;
        BigDecimal outgoingQtyAbs = BigDecimal.ZERO;
        BigDecimal outgoingCost = BigDecimal.ZERO;
    }

    /**
     * 合并聚合 DONE 移动单行（perf-ux plan 0325-2 Decision-a）：符号拆分 GROUP BY moveId 投影聚合
     * （①qty&gt;0 SUM(quantity) ②qty&lt;0 SUM(quantity) ③无过滤 SUM(totalCost) 三条）+ moves 范围查询做
     * moveType 映射，内存按 moveType 桶装并合成。数学等价：出库量 Σ|qᵢ| = Σₘ(P(m)−N(m))、
     * 入库量 Σqᵢ = Σₘ(P(m)+N(m))（BigDecimal 精确无舍入）；投影聚合不携 in(moveId) 过滤
     * （行数=move_line 去重 moveId 数，天然有界，优于原计划 in() 分块多趟方案）。
     */
    private MoveAgg aggDoneMoveLinesInRange(LocalDate from, LocalDate to) {
        MoveAgg agg = new MoveAgg();
        List<ErpInvStockMove> moves = loadDoneMovesInRange(from, to);
        if (moves.isEmpty()) return agg;
        Map<String, String> moveTypeByMoveId = new HashMap<>();
        Set<String> allMoveIds = new HashSet<>();
        for (ErpInvStockMove m : moves) {
            moveTypeByMoveId.put(m.getId(), m.getMoveType());
            allMoveIds.add(m.getId());
        }

        // 单实体投影聚合三条（GROUP BY moveId，维度分组先例机制）：
        // ① 正 qty 行 SUM(quantity)（qty>0）；② 负 qty 行 SUM(quantity)（qty<0）；
        // ③ 全部行 SUM(totalCost)（无 qty 过滤——出库成本原语义=出库 move 所有行 totalCost 求和，
        //    不能用 ① 的 qty>0 投影替代：出库行 qty 常为负会被排除）。
        Map<String, BigDecimal> posQtyByMove = new HashMap<>();
        Map<String, BigDecimal> negQtyByMove = new HashMap<>();
        Map<String, BigDecimal> costByMove = new HashMap<>();
        {
            QueryBean q = new QueryBean();
            q.setSourceName(ErpInvStockMoveLine.class.getName());
            q.addFilter(gt("quantity", BigDecimal.ZERO));
            QueryFieldBean dim = QueryFieldBean.mainField("moveId");
            QueryFieldBean sumQty = QueryFieldBean.mainField("quantity").sum().alias("posQty");
            q.setFields(Arrays.asList(dim, sumQty));
            for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
                String moveId = (String) row.get("moveId");
                if (moveId == null) continue;
                posQtyByMove.put(moveId, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("posQty"))));
            }
        }
        {
            QueryBean q = new QueryBean();
            q.setSourceName(ErpInvStockMoveLine.class.getName());
            q.addFilter(lt("quantity", BigDecimal.ZERO));
            QueryFieldBean dim = QueryFieldBean.mainField("moveId");
            QueryFieldBean sumQty = QueryFieldBean.mainField("quantity").sum().alias("negQty");
            q.setFields(Arrays.asList(dim, sumQty));
            for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
                String moveId = (String) row.get("moveId");
                if (moveId == null) continue;
                negQtyByMove.put(moveId, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("negQty"))));
            }
        }
        {
            QueryBean q = new QueryBean();
            q.setSourceName(ErpInvStockMoveLine.class.getName());
            QueryFieldBean dim = QueryFieldBean.mainField("moveId");
            QueryFieldBean sumCost = QueryFieldBean.mainField("totalCost").sum().alias("lineCost");
            q.setFields(Arrays.asList(dim, sumCost));
            for (Map<String, Object> row : ormTemplate.findListByQuery(q)) {
                String moveId = (String) row.get("moveId");
                if (moveId == null) continue;
                costByMove.put(moveId, DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("lineCost"))));
            }
        }

        for (Map.Entry<String, String> e : moveTypeByMoveId.entrySet()) {
            String moveId = e.getKey();
            String moveType = e.getValue();
            BigDecimal pos = posQtyByMove.getOrDefault(moveId, BigDecimal.ZERO);
            BigDecimal neg = negQtyByMove.getOrDefault(moveId, BigDecimal.ZERO);
            if (ErpInvConstants.MOVE_TYPE_INCOMING.equals(moveType)) {
                // 入库量 = 行级原值求和 = Σ(pos+neg)
                agg.incomingQty = agg.incomingQty.add(pos).add(neg);
            } else if (ErpInvConstants.MOVE_TYPE_OUTGOING.equals(moveType)) {
                // 出库量 = 行级 abs 求和 = Σ(pos−neg)（neg ≤ 0）
                agg.outgoingQtyAbs = agg.outgoingQtyAbs.add(pos).subtract(neg);
                // 出库成本 = 出库 move 所有行 totalCost 求和（原语义，无 qty 符号过滤）
                agg.outgoingCost = agg.outgoingCost.add(costByMove.getOrDefault(moveId, BigDecimal.ZERO));
            }
        }
        return agg;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate)
            return (LocalDate) value;
        if (value instanceof java.sql.Date)
            return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date)
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        return null;
    }

    private List<ErpInvStockMove> loadDoneMovesInRange(LocalDate from, LocalDate to) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpInvConstants.DOC_STATUS_DONE));
        if (from != null) q.addFilter(ge("businessDate", from));
        if (to != null) q.addFilter(le("businessDate", to));
        return dao.findAllByQuery(q);
    }

    private Map<String, BigDecimal> loadSafetyStock(Set<String> materialIds) {
        if (materialIds.isEmpty()) return Collections.emptyMap();
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", materialIds));
        Map<String, BigDecimal> map = new HashMap<>();
        for (ErpMdMaterial m : dao.findAllByQuery(q)) {
            map.put(m.getId(), DashboardUtil.nz(m.getSafetyStock()));
        }
        return map;
    }

    private Map<String, String> loadMaterialNames(Set<String> materialIds) {
        if (materialIds.isEmpty()) return Collections.emptyMap();
        IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", materialIds));
        Map<String, String> map = new HashMap<>();
        for (ErpMdMaterial m : dao.findAllByQuery(q)) {
            map.put(m.getId(), m.getName());
        }
        return map;
    }

    private Map<String, String> loadWarehouseNames(Set<String> warehouseIds) {
        if (warehouseIds.isEmpty()) return Collections.emptyMap();
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", warehouseIds));
        Map<String, String> map = new HashMap<>();
        for (ErpMdWarehouse w : dao.findAllByQuery(q)) {
            map.put(w.getId(), w.getName());
        }
        return map;
    }

    private Map<String, String> loadWarehouseNamesForBalances(List<ErpInvStockBalance> balances) {
        Set<String> warehouseIds = new HashSet<>();
        for (ErpInvStockBalance b : balances) {
            if (b.getWarehouseId() != null) warehouseIds.add(b.getWarehouseId());
        }
        return loadWarehouseNames(warehouseIds);
    }

    /**
     * 加载 cutoff 之后的最近出库日期，按 materialId → lastOutDate（StockMoveLine 无 warehouseId，故物料级聚合）。
     * perf-ux plan 0325-2 Decision-b：原 cutoff 后全部出库 move+line 全量物化改为
     * 「outgoing DONE moves 范围查询（move 级，行数=出库单数）+ move_line GROUP BY moveId 投影聚合」，
     * 行级物化消除；物料维度天然有界，不加 setLimit（无序 limit=非确定截断）。
     */
    private Map<String, LocalDate> loadLastOutgoingDates(LocalDate cutoff) {
        IEntityDao<ErpInvStockMove> mDao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean mq = new QueryBean();
        mq.addFilter(eq("moveType", ErpInvConstants.MOVE_TYPE_OUTGOING));
        mq.addFilter(eq("docStatus", ErpInvConstants.DOC_STATUS_DONE));
        if (cutoff != null) mq.addFilter(ge("businessDate", cutoff));
        List<ErpInvStockMove> moves = mDao.findAllByQuery(mq);
        if (moves.isEmpty()) return Collections.emptyMap();
        Map<String, LocalDate> moveDateByMoveId = new HashMap<>();
        for (ErpInvStockMove m : moves) {
            moveDateByMoveId.put(m.getId(), m.getBusinessDate());
        }
        // move_line 按 moveId 维度投影聚合（行数=出库 move 数 ×物料数，天然有界）
        QueryBean lq = new QueryBean();
        lq.setSourceName(ErpInvStockMoveLine.class.getName());
        QueryFieldBean dimMove = QueryFieldBean.mainField("moveId");
        QueryFieldBean dimMat = QueryFieldBean.mainField("materialId");
        lq.setFields(Arrays.asList(dimMove, dimMat));
        Map<String, LocalDate> result = new HashMap<>();
        for (Map<String, Object> row : ormTemplate.findListByQuery(lq)) {
            String moveId = (String) row.get("moveId");
            Object mat = row.get("materialId");
            LocalDate d = moveId != null ? moveDateByMoveId.get(moveId) : null;
            if (d == null || mat == null) continue;
            result.merge((String) mat, d, (a, b) -> a.isAfter(b) ? a : b);
        }
        return result;
    }

    /**
     * DB 级 SUM(totalCost)：按 warehouseId 分组取各组 SUM 后再汇总。
     * <p>不直接用无维度全局聚合——MdxQueryExecutor 对无维度聚合会强制注入主键维度，生成非法 SQL
     * （select sum(..), o.id 无 group by）。按真实维度分组后汇总等价于全局 SUM，且 null 仓库行单独成组被计入。
     */
    private BigDecimal sumBalanceTotalCost() {
        QueryBean q = new QueryBean();
        q.setSourceName(ErpInvStockBalance.class.getName());
        QueryFieldBean dim = QueryFieldBean.mainField("warehouseId");
        QueryFieldBean sumCost = QueryFieldBean.mainField("totalCost").sum().alias("totalValue");
        q.setFields(Arrays.asList(dim, sumCost));
        List<Map<String, Object>> rows = ormTemplate.findListByQuery(q);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            total = total.add(DashboardUtil.nz(DashboardUtil.toBigDecimal(row.get("totalValue"))));
        }
        return total;
    }
}
