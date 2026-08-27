
package app.erp.inv.service.entity;

import app.erp.common.service.DashboardUtil;
import app.erp.inv.biz.IErpInvStockLedgerBiz;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.common.service.AbstractErpImmutableCrudBizModel;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.beans.query.QueryFieldBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;

import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 库存流水实体服务。E3.2 扩展：库存审计快照（派生视图，零 ORM）与「账面余额 = 快照派生值」一致性校验
 * （`docs/design/inventory/audit-snapshot-cycle-count.md` §1/§3 + §实现路径确认）。
 *
 * <p>快照 = 对不可变 {@link ErpInvStockLedger}（有符号数量/成本）按 {@code businessDate <= asOfDate}
 * 的余额维度（orgId/warehouseId/locationId/materialId/skuId/batchNo/ownerId）DB 级 GROUP BY + SUM 聚合，
 * 等价「期初 + 截至时点流水汇总」（流水全史即期初）；asOfDate 语义对齐 businessDate（AP-5）。
 * 跨实体读取 {@link ErpInvStockBalance}（同域只读聚合，对账比对面）经 daoFor 直查。
 */
@BizModel("ErpInvStockLedger")
public class ErpInvStockLedgerBizModel extends AbstractErpImmutableCrudBizModel<ErpInvStockLedger> implements IErpInvStockLedgerBiz {

    /** 快照维度列（对齐 ErpInvStockBalance 余额维度；serialNo 不参与——余额层无序列维度）。 */
    private static final String[] SNAPSHOT_DIMS = {
            "orgId", "warehouseId", "locationId", "materialId", "skuId", "batchNo", "ownerId"};

    @Inject
    IOrmTemplate ormTemplate;

    public ErpInvStockLedgerBizModel() {
        setEntityName(ErpInvStockLedger.class.getName());
    }

    @Override
    @BizQuery
    public Map<String, Object> getInventorySnapshot(@Optional @Name("warehouseId") String warehouseId,
                                                    @Optional @Name("materialIds") List<String> materialIds,
                                                    @Optional @Name("asOfDate") LocalDate asOfDate,
                                                    IServiceContext context) {
        LocalDate asOf = asOfDate != null ? asOfDate : CoreMetrics.currentDate();
        return ormTemplate.runInSession(session -> {
            List<Map<String, Object>> aggregated = aggregateLedgerUpTo(warehouseId, materialIds, asOf);

            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            List<Map<String, Object>> rows = new ArrayList<>(aggregated.size());
            for (Map<String, Object> row : aggregated) {
                BigDecimal qty = DashboardUtil.toBigDecimal(row.get("quantity"));
                BigDecimal cost = DashboardUtil.toBigDecimal(row.get("totalCost"));
                Map<String, Object> r = new LinkedHashMap<>();
                for (String dim : SNAPSHOT_DIMS) {
                    r.put(dim, row.get(dim));
                }
                r.put("quantity", qty);
                r.put("totalCost", cost);
                r.put("unitCost", deriveUnitCost(cost, qty));
                rows.add(r);
                totalQuantity = totalQuantity.add(qty);
                totalCost = totalCost.add(cost);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("asOfDate", asOf);
            result.put("rowCount", rows.size());
            result.put("rows", rows);
            result.put("totalQuantity", totalQuantity);
            result.put("totalCost", totalCost);
            return result;
        });
    }

    @Override
    @BizQuery
    public Map<String, Object> checkStockBalanceConsistency(@Optional @Name("asOfDate") LocalDate asOfDate,
                                                            IServiceContext context) {
        LocalDate asOf = asOfDate != null ? asOfDate : CoreMetrics.currentDate();
        return ormTemplate.runInSession(session -> {
            Map<String, Map<String, Object>> derivedByKey = new LinkedHashMap<>();
            for (Map<String, Object> row : aggregateLedgerUpTo(null, null, asOf)) {
                derivedByKey.put(balanceKey(row), row);
            }

            // 同域只读聚合：对账比对面 ErpInvStockBalance 全量加载（单组织基线量级可控）
            List<ErpInvStockBalance> balances = daoProvider().daoFor(ErpInvStockBalance.class).findAll();

            List<Map<String, Object>> mismatches = new ArrayList<>();
            for (ErpInvStockBalance bal : balances) {
                Map<String, Object> balRow = balanceToDimRow(bal);
                String key = balanceKey(balRow);
                Map<String, Object> derived = derivedByKey.remove(key);
                BigDecimal bookQty = nz(bal.getTotalQuantity());
                BigDecimal bookCost = nz(bal.getTotalCost());
                if (derived == null) {
                    if (bookQty.signum() != 0 || bookCost.signum() != 0) {
                        mismatches.add(mismatchRow(balRow, key, bookQty, bookCost,
                                BigDecimal.ZERO, BigDecimal.ZERO, "BOOK_ONLY_NO_LEDGER"));
                    }
                    continue;
                }
                BigDecimal derivedQty = DashboardUtil.toBigDecimal(derived.get("quantity"));
                BigDecimal derivedCost = DashboardUtil.toBigDecimal(derived.get("totalCost"));
                if (bookQty.compareTo(derivedQty) != 0 || bookCost.compareTo(derivedCost) != 0) {
                    mismatches.add(mismatchRow(balRow, key, bookQty, bookCost,
                            derivedQty, derivedCost, "QTY_OR_COST_MISMATCH"));
                }
            }
            for (Map<String, Object> derived : derivedByKey.values()) {
                BigDecimal derivedQty = DashboardUtil.toBigDecimal(derived.get("quantity"));
                BigDecimal derivedCost = DashboardUtil.toBigDecimal(derived.get("totalCost"));
                if (derivedQty.signum() != 0 || derivedCost.signum() != 0) {
                    mismatches.add(mismatchRow(null, balanceKey(derived),
                            BigDecimal.ZERO, BigDecimal.ZERO, derivedQty, derivedCost, "LEDGER_ONLY_NO_BALANCE"));
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("asOfDate", asOf);
            result.put("balanceRowCount", balances.size());
            result.put("derivedRowCount", derivedByKey.size() + mismatches.size());
            result.put("mismatchCount", mismatches.size());
            result.put("consistent", mismatches.isEmpty());
            result.put("mismatches", mismatches);
            return result;
        });
    }

    // ===================== helpers =====================

    /** DB 级 GROUP BY 余额维度 + SUM(quantity)/SUM(totalCost)，businessDate <= asOf（含可选仓库/物料过滤）。 */
    private List<Map<String, Object>> aggregateLedgerUpTo(String warehouseId, List<String> materialIds, LocalDate asOf) {
        QueryBean q = new QueryBean();
        q.setSourceName(ErpInvStockLedger.class.getName());
        q.addFilter(le("businessDate", asOf));
        if (warehouseId != null) {
            q.addFilter(eq("warehouseId", warehouseId));
        }
        if (materialIds != null && !materialIds.isEmpty()) {
            q.addFilter(in("materialId", materialIds));
        }
        List<QueryFieldBean> fields = new ArrayList<>(SNAPSHOT_DIMS.length + 2);
        for (String dim : SNAPSHOT_DIMS) {
            fields.add(QueryFieldBean.mainField(dim));
        }
        fields.add(QueryFieldBean.mainField("quantity").sum().alias("quantity"));
        fields.add(QueryFieldBean.mainField("totalCost").sum().alias("totalCost"));
        q.setFields(fields);
        return ormTemplate.findListByQuery(q);
    }

    private static String balanceKey(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        for (String dim : new String[]{"warehouseId", "locationId", "materialId", "skuId", "batchNo"}) {
            Object v = row.get(dim);
            sb.append(v == null ? "" : String.valueOf(v)).append('|');
        }
        return sb.toString();
    }

    private static Map<String, Object> mismatchRow(Map<String, Object> balance, String key,
                                                   BigDecimal bookQty, BigDecimal bookCost,
                                                   BigDecimal derivedQty, BigDecimal derivedCost, String type) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("balanceId", balance != null ? balance.get("id") : null);
        row.put("dimensionKey", key);
        if (balance != null) {
            row.put("warehouseId", balance.get("warehouseId"));
            row.put("locationId", balance.get("locationId"));
            row.put("materialId", balance.get("materialId"));
            row.put("skuId", balance.get("skuId"));
            row.put("batchNo", balance.get("batchNo"));
        }
        row.put("type", type);
        row.put("bookQuantity", bookQty);
        row.put("bookTotalCost", bookCost);
        row.put("derivedQuantity", derivedQty);
        row.put("derivedTotalCost", derivedCost);
        return row;
    }

    /** 余额实体 → 维度键行（与流水聚合行同构，供 balanceKey/mismatchRow 复用）。 */
    private static Map<String, Object> balanceToDimRow(ErpInvStockBalance bal) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", bal.getId());
        row.put("orgId", bal.getOrgId());
        row.put("warehouseId", bal.getWarehouseId());
        row.put("locationId", bal.getLocationId());
        row.put("materialId", bal.getMaterialId());
        row.put("skuId", bal.getSkuId());
        row.put("batchNo", bal.getBatchNo());
        row.put("ownerId", bal.getOwnerId());
        return row;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal deriveUnitCost(BigDecimal totalCost, BigDecimal quantity) {
        if (quantity == null || quantity.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
    }
}
