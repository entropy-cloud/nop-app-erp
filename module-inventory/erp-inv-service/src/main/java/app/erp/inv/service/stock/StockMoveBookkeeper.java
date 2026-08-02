package app.erp.inv.service.stock;

import app.erp.inv.service.ErpInvConfigs;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.inv.service.costing.BookingContext;
import app.erp.inv.service.costing.CostMethodResolver;
import app.erp.inv.service.costing.CostingStrategy;
import app.erp.inv.service.costing.BatchCostingStrategy;
import app.erp.inv.service.costing.FifoCostingStrategy;
import app.erp.inv.service.costing.LifoCostingStrategy;
import app.erp.inv.service.costing.MovingAverageCostingStrategy;
import app.erp.inv.service.costing.SpecificCostingStrategy;
import app.erp.inv.service.costing.StandardCostingStrategy;
import app.erp.inv.service.costing.WeightedAverageCostingStrategy;
import app.erp.inv.service.metrics.ErpInvConcurrencyMetrics;
import io.micrometer.core.instrument.Counter;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.metrics.GlobalMeterRegistry;
import io.nop.commons.util.StringHelper;
import io.nop.dao.DaoErrors;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.exceptions.JdbcException;
import io.nop.orm.IOrmSession;
import io.nop.orm.IOrmTemplate;
import io.nop.orm.OrmEntityState;
import io.nop.orm.dao.IOrmEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.isNull;

/**
 * 库存记账器：移动单 DONE 时按 {@code ErpMdMaterial.costMethod} 分派到 {@link CostingStrategy} 写不可变库存流水
 * （{@link ErpInvStockLedger}）并更新库存余额（{@link ErpInvStockBalance}）。同时提供余额维度的 upsert（供状态机预留量使用）。
 *
 * <p>权威：{@code docs/design/inventory/state-machine.md}（DONE 写流水+更新余额）、
 * {@code docs/design/inventory/cross-domain.md}（余额更新与流水写入同一事务）、
 * {@code docs/design/finance/costing-methods.md}（按物料 costMethod 分派）。
 *
 * <p>分派来源（{@link CostMethodResolver}）：{@code ErpMdMaterial.costMethod} → {@code ErpMdAcctSchema.costingMethod}
 * → {@code erp-inv.default-cost-method}；{@code erp-inv.costing-enabled=false} 时一律回退移动加权平均（兜底）。
 *
 * <p>流水 quantity 按方向带符号（入库正/出库负），{@code balanceQuantity}/{@code balanceTotalCost} 记结存快照（不可变）。
 * 入库增余额、出库扣余额、内部调拨扣源加目的（源按出库、目的按入库，成本沿用源 unitCost）。
 */
public class StockMoveBookkeeper implements BookingContext {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    CostMethodResolver costMethodResolver;

    @Inject
    MovingAverageCostingStrategy movingAverageCostingStrategy;

    @Inject
    FifoCostingStrategy fifoCostingStrategy;

    @Inject
    StandardCostingStrategy standardCostingStrategy;

    @Inject
    LifoCostingStrategy lifoCostingStrategy;

    @Inject
    WeightedAverageCostingStrategy weightedAverageCostingStrategy;

    @Inject
    SpecificCostingStrategy specificCostingStrategy;

    @Inject
    BatchCostingStrategy batchCostingStrategy;

    final Map<String, CostingStrategy> strategyByMethod = new HashMap<>();

    @jakarta.annotation.PostConstruct
    void initStrategyRegistry() {
        register(movingAverageCostingStrategy);
        register(fifoCostingStrategy);
        register(standardCostingStrategy);
        register(lifoCostingStrategy);
        register(weightedAverageCostingStrategy);
        register(specificCostingStrategy);
        register(batchCostingStrategy);
    }

    public void register(CostingStrategy strategy) {
        strategyByMethod.put(strategy.costMethod(), strategy);
    }

    /**
     * 按行写不可变库存流水（含结存快照 balanceQuantity/balanceTotalCost）并按物料 costMethod 分派策略更新余额/成本层。
     * 入库增余额、出库扣余额、内部调拨扣源加目的。同一事务内完成（由调用方 {@code @Transactional} 保证）。
     */
    public void bookCompletion(ErpInvStockMove move, List<ErpInvStockMoveLine> lines, Long acctSchemaId) {
        for (ErpInvStockMoveLine line : lines) {
            String method = costMethodResolver.resolve(line, acctSchemaId);
            CostingStrategy strategy = resolveStrategy(method);
            if (move.getMoveType() != null && Objects.equals(move.getMoveType(), ErpInvConstants.MOVE_TYPE_INTERNAL_TRANSFER)) {
                BigDecimal carriedCost = strategy.onOutgoing(move, line, acctSchemaId, this);
                strategy.onIncoming(move, line, acctSchemaId, carriedCost, this);
            } else if (move.getMoveType() != null && Objects.equals(move.getMoveType(), ErpInvConstants.MOVE_TYPE_OUTGOING)) {
                strategy.onOutgoing(move, line, acctSchemaId, this);
            } else {
                BigDecimal unitCost = nz(line.getUnitCost());
                strategy.onIncoming(move, line, acctSchemaId, unitCost, this);
            }
        }
    }

    private CostingStrategy resolveStrategy(String method) {
        CostingStrategy strategy = strategyByMethod.get(method);
        return strategy != null ? strategy : movingAverageCostingStrategy;
    }

    /**
     * 按 物料 × 仓库 × 库位 × 批次 维度查找余额，不存在则初始化（totalQuantity=0、costMethod=移动加权平均）。
     *
     * <p>owner 维度（consignment.md §配置点）：{@code erp-inv.ownership-tracking-enabled=false}（默认关）时
     * ownerId 一律 null、不入键，与基线逐字节一致；启用时方按 ownerId 拆出独立子余额行。标准移动单不携带 ownerId，
     * 故 disabled 时透传 null（等价既有行为），enabled 时同样 null（标准移动单写 OWNED 余额，VMI 余额经转移单建立）。
     *
     * <p>并发首次 INSERT 兜底（plan 2026-07-28-1249 P0-MA2-020）：本方法仅 queue INSERT（不主动 flush，
     * 保持版本号语义不变）。自然键 UK_INV_STOCK_BALANCE_NATURAL 冲突的实际捕获发生在紧随其后的
     * {@link #updateBalanceWithRetry} 的 flush 阶段——见该方法的 SAVING 分支（evict + reload 已落地行 + 转更新路径）。
     */
    public ErpInvStockBalance upsertBalance(ErpInvStockMove move, ErpInvStockMoveLine line,
                                             Long warehouseId, Long locationId) {
        // 同事务内可能已新建余额但未刷盘，查询前先 flush 使待落库的预留量/余额可见
        ormTemplate.flushSession();
        Long ownerId = resolveOwnerKey(null);
        ErpInvStockBalance balance = findBalance(move.getOrgId(), line.getMaterialId(), line.getSkuId(),
                warehouseId, locationId, line.getBatchNo(), ownerId);
        if (balance != null) {
            return balance;
        }
        return buildNewBalanceForMove(move, line, warehouseId, locationId, ownerId);
    }

    /**
     * 构造一条新余额行（未持久化）。仅在 INSERT 路径调用；冲突捕获由 {@link #updateBalanceWithRetry} 负责。
     */
    private ErpInvStockBalance buildNewBalanceForMove(ErpInvStockMove move, ErpInvStockMoveLine line,
                                                       Long warehouseId, Long locationId, Long ownerId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        ErpInvStockBalance balance = dao.newEntity();
        balance.setOrgId(move.getOrgId());
        balance.setMaterialId(line.getMaterialId());
        balance.setSkuId(line.getSkuId());
        balance.setWarehouseId(warehouseId);
        balance.setLocationId(locationId);
        balance.setBatchNo(line.getBatchNo());
        balance.setTotalQuantity(BigDecimal.ZERO);
        balance.setReservedQuantity(BigDecimal.ZERO);
        balance.setLockedQuantity(BigDecimal.ZERO);
        balance.setAvailableQuantity(BigDecimal.ZERO);
        balance.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        balance.setAvgCost(BigDecimal.ZERO);
        balance.setTotalCost(BigDecimal.ZERO);
        balance.setCurrencyId(line.getCurrencyId());
        // owner 维度默认值：OWNED + null ownerId（disabled/enabled 但标准移动单均如此）
        balance.setOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_OWNED);
        if (isOwnershipTrackingEnabled()) {
            balance.setOwnerId(ownerId);
        }
        dao.saveEntity(balance);
        return balance;
    }

    // ---------- BookingContext: shared booking primitives exposed to strategies ----------

    @Override
    public void writeLedger(ErpInvStockMove move, ErpInvStockMoveLine line, Long acctSchemaId,
                             ErpInvStockBalance balance, Long warehouseId, Long locationId,
                             BigDecimal signedQty, BigDecimal unitCost, BigDecimal signedTotalCost,
                             String costMethod) {
        IEntityDao<ErpInvStockLedger> dao = daoProvider.daoFor(ErpInvStockLedger.class);
        ErpInvStockLedger ledger = dao.newEntity();
        ledger.setCode("SL-" + StringHelper.generateUUID());
        ledger.setOrgId(move.getOrgId());
        ledger.setMoveId(move.getId());
        ledger.setMoveLineId(line.getId());
        ledger.setMaterialId(line.getMaterialId());
        ledger.setSkuId(line.getSkuId());
        ledger.setWarehouseId(warehouseId);
        ledger.setLocationId(locationId);
        ledger.setQuantity(signedQty);
        ledger.setUnitCost(ErpInvConfigs.roundCost(unitCost));
        ledger.setTotalCost(signedTotalCost);
        ledger.setBalanceQuantity(balance.getTotalQuantity());
        ledger.setBalanceTotalCost(balance.getTotalCost());
        ledger.setCostMethod(costMethod);
        ledger.setAcctSchemaId(acctSchemaId);
        ledger.setCurrencyId(line.getCurrencyId());
        ledger.setBusinessDate(move.getBusinessDate());
        ledger.setBatchNo(line.getBatchNo());
        ledger.setSerialNo(line.getSerialNo());
        dao.saveEntity(ledger);
    }

    @Override
    public void recomputeAvailable(ErpInvStockBalance balance) {
        BigDecimal total = nz(balance.getTotalQuantity());
        BigDecimal reserved = nz(balance.getReservedQuantity());
        BigDecimal locked = nz(balance.getLockedQuantity());
        balance.setAvailableQuantity(total.subtract(reserved).subtract(locked));
    }

    /**
     * {@inheritDoc}
     *
     * <p>实现要点（plan 2026-07-07-0024-2 / UC-INV-08 / concurrency-and-transactions.md §模式四 +
     * plan 2026-07-28-1249 P0-MA2-020 INSERT 路径扩展）：
     * <ul>
     *   <li>已 managed 实体：经 {@link io.nop.orm.dao.IOrmEntityDao#tryUpdateWithVersionCheck}
     *       提交（生成 {@code UPDATE WHERE id=? AND version=?}）。</li>
     *   <li>冲突（受影响行数=0，平台将实例置 readonly）：evict 旧实例 + {@link IEntityDao#requireEntityById}
     *       加载新实例（readonly=false），重新执行 {@code applyDelta}，重试。</li>
     *   <li>新实体（TRANSIENT/SAVING，尚未持久化）：flush 触发 INSERT；若 flush 抛
     *       {@link DaoErrors#ERR_SQL_DUPLICATE_KEY}（自然键 UK_INV_STOCK_BALANCE_NATURAL 冲突），
     *       evict + 按自然键 reload 已落地的并发对方行 + 转入 MANAGED 路径重试 {@code applyDelta}
     *       （典型并发首次移动单同维度场景）。</li>
     *   <li>重试上限 = {@code erp-inv.concurrent-deduct-max-retry}（默认 5）；耗尽抛
     *       {@link ErpInvErrors#ERR_INV_CONCURRENT_DEDUCT_CONFLICT}（MANAGED 路径）或
     *       {@link ErpInvErrors#ERR_INV_BALANCE_INSERT_CONFLICT}（INSERT 路径）。</li>
     * </ul>
     *
     * <p>注意：平台 {@code OrmEntity.orm_readonly(boolean)} 设置器将 readonly 视为粘性（一旦 true 不可复位），
     * 故必须 evict 后重新加载新实例，而非 orm_unload+getEntityById 复用同一实例——否则后续 flush 会跳过该实体。
     *
     * <p>版本号语义：happy path（INSERT 成功）的 version 仍为 0（与既有 snapshot 一致）；
     * 仅当 INSERT 落地后再次同事务更新时 version 才自增。本方法不在 happy path 主动多 flush 一次。
     */
    @Override
    public ErpInvStockBalance updateBalanceWithRetry(ErpInvStockBalance initialBaseline,
                                                     Consumer<ErpInvStockBalance> applyDelta) {
        IOrmEntityDao<ErpInvStockBalance> dao = (IOrmEntityDao<ErpInvStockBalance>)
                daoProvider.daoFor(ErpInvStockBalance.class);
        int maxRetry = AppConfig.var(ErpInvConstants.CONFIG_CONCURRENT_DEDUCT_MAX_RETRY,
                ErpInvConstants.CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT);

        ErpInvStockBalance current = initialBaseline;
        int attempts = 0;
        while (true) {
            applyDelta.accept(current);

            OrmEntityState state = current.orm_state();
            boolean conflict;
            if (state == OrmEntityState.MANAGED) {
                conflict = !dao.tryUpdateWithVersionCheck(current);
            } else if (state == OrmEntityState.TRANSIENT) {
                // 新余额从未入 session：queue INSERT + flush 触发 DB 落地（捕获 UK 冲突）
                dao.saveEntity(current);
                conflict = !flushAndCheckConflict(current);
            } else if (state == OrmEntityState.SAVING) {
                // 已 queue INSERT：flush 触发 DB 落地（捕获 UK 冲突）。applyDelta 的字段变更随 INSERT 一次落盘
                conflict = !flushAndCheckConflict(current);
            } else {
                // 其他态（如 DELETED 等，理论不可达）：不 flush，直接返回
                return current;
            }

            if (!conflict) {
                return current;
            }

            // observability.md §5.1 指标 4（erp_concurrency_optimistic_lock_failure_total Counter）：
            // 每次乐观锁冲突（tryLock 失败 / INSERT UK 冲突）计数一次。tag=domain=inventory, operation=stock_balance_update
            ErpInvConcurrencyMetrics.recordOptimisticLockFailure(null);

            attempts++;
            if (attempts > maxRetry) {
                // 重试耗尽：额外计数一次（区分冲突频次 vs 最终放弃事件）
                ErpInvConcurrencyMetrics.recordOptimisticLockFailureExhausted(null);
                throw buildConflictExhaustedEx(state, current, attempts);
            }

            // 冲突：捕获自然键，evict 失败实例，按自然键 reload
            Long orgId = current.getOrgId();
            Long materialId = current.getMaterialId();
            Long skuId = current.getSkuId();
            Long warehouseId = current.getWarehouseId();
            Long locationId = current.getLocationId();
            String batchNo = current.getBatchNo();
            Long ownerId = current.getOwnerId();

            IOrmSession session = ormTemplate.currentSession();
            if (session != null) {
                session.evict(current);
            }

            if (state == OrmEntityState.MANAGED) {
                // UPDATE 冲突：DB 行仍存在但版本前进；按主键 reload
                current = dao.requireEntityById(current.orm_id());
            } else {
                // INSERT 冲突：DB 中已有并发对方行；按自然键 reload（找不到时退化为 newEntity 重试）
                current = findBalanceByNaturalKey(orgId, materialId, skuId, warehouseId,
                        locationId, batchNo, ownerId);
                if (current == null) {
                    // 极罕见：对方事务已回滚；构造新候选重试 INSERT
                    current = newBlankBalance(orgId, materialId, skuId, warehouseId,
                            locationId, batchNo, ownerId);
                    dao.saveEntity(current);
                }
            }
        }
    }

    /**
     * flush 当前会话。返回 true 表示无冲突（happy path），false 表示抛出并被捕获的 UK 冲突。
     * 非 UK 异常向上原样抛出。
     */
    private boolean flushAndCheckConflict(ErpInvStockBalance candidate) {
        try {
            ormTemplate.flushSession();
            return true;
        } catch (Exception e) {
            if (isUniqueConstraintViolation(e)) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 按自然键精确查询余额行。与 ORM UK 列对齐：orgId/materialId/skuId/warehouseId/locationId/batchNo/ownerId。
     * nullable 列（skuId/locationId/batchNo/ownerId）使用 IS NULL 语义匹配。
     */
    private ErpInvStockBalance findBalanceByNaturalKey(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                                        Long locationId, String batchNo, Long ownerId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", orgId));
        q.addFilter(eq("materialId", materialId));
        if (skuId != null) {
            q.addFilter(eq("skuId", skuId));
        } else {
            q.addFilter(isNull("skuId"));
        }
        q.addFilter(eq("warehouseId", warehouseId));
        if (locationId != null) {
            q.addFilter(eq("locationId", locationId));
        } else {
            q.addFilter(isNull("locationId"));
        }
        if (batchNo != null) {
            q.addFilter(eq("batchNo", batchNo));
        } else {
            q.addFilter(isNull("batchNo"));
        }
        if (ownerId != null) {
            q.addFilter(eq("ownerId", ownerId));
        } else {
            q.addFilter(isNull("ownerId"));
        }
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 构造空白余额候选（用于极罕见的"对方事务回滚后重试 INSERT"场景）。 */
    private ErpInvStockBalance newBlankBalance(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                                Long locationId, String batchNo, Long ownerId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        ErpInvStockBalance balance = dao.newEntity();
        balance.setOrgId(orgId);
        balance.setMaterialId(materialId);
        balance.setSkuId(skuId);
        balance.setWarehouseId(warehouseId);
        balance.setLocationId(locationId);
        balance.setBatchNo(batchNo);
        balance.setTotalQuantity(BigDecimal.ZERO);
        balance.setReservedQuantity(BigDecimal.ZERO);
        balance.setLockedQuantity(BigDecimal.ZERO);
        balance.setAvailableQuantity(BigDecimal.ZERO);
        balance.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        balance.setAvgCost(BigDecimal.ZERO);
        balance.setTotalCost(BigDecimal.ZERO);
        balance.setOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_OWNED);
        if (isOwnershipTrackingEnabled()) {
            balance.setOwnerId(ownerId);
        }
        return balance;
    }

    private static NopException buildConflictExhaustedEx(OrmEntityState state, ErpInvStockBalance current,
                                                          int attempts) {
        if (state == OrmEntityState.MANAGED) {
            return new NopException(ErpInvErrors.ERR_INV_CONCURRENT_DEDUCT_CONFLICT)
                    .param(ErpInvErrors.ARG_BALANCE_ID, current.orm_id())
                    .param(ErpInvErrors.ARG_ATTEMPTS, attempts);
        }
        return new NopException(ErpInvErrors.ERR_INV_BALANCE_INSERT_CONFLICT)
                .param(ErpInvErrors.ARG_MATERIAL_ID, current.getMaterialId())
                .param(ErpInvErrors.ARG_WAREHOUSE_ID, current.getWarehouseId())
                .param(ErpInvErrors.ARG_ATTEMPTS, attempts);
    }

    /**
     * 判定异常链中是否含数据库唯一约束/主键冲突。
     *
     * <p>平台 JDBC 翻译器把 {@link java.sql.SQLIntegrityConstraintViolationException} 翻译为
     * {@link JdbcException}，错误码为 {@link DaoErrors#ERR_SQL_DUPLICATE_KEY}（H2/PG/MySQL 唯一索引冲突）
     * 或 {@link DaoErrors#ERR_SQL_DATA_INTEGRITY_VIOLATION}（其他完整性违例，少数驱动归类至此）。
     * 遍历 cause 链以兼容包装异常（事务管理器 / IoC）。
     */
    static boolean isUniqueConstraintViolation(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof JdbcException) {
                String code = ((JdbcException) cur).getErrorCode();
                if (DaoErrors.ERR_SQL_DUPLICATE_KEY.getErrorCode().equals(code)
                        || DaoErrors.ERR_SQL_DATA_INTEGRITY_VIOLATION.getErrorCode().equals(code)) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    @Override
    public IDaoProvider daoProvider() {
        return daoProvider;
    }

    @Override
    public IOrmTemplate ormTemplate() {
        return ormTemplate;
    }

    ErpInvStockBalance findBalance(Long orgId, Long materialId, Long skuId, Long warehouseId,
                                   Long locationId, String batchNo, Long ownerId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
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
        // owner 维度入键仅当 ownership-tracking-enabled（默认关）。关闭时 ownerId 强制 null，等价既有行为。
        if (isOwnershipTrackingEnabled() && ownerId != null) {
            q.addFilter(eq("ownerId", ownerId));
        }
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** owner 维度开关：默认关（对齐 Odoo feature group，非 VMI 用户无感知）。 */
    public boolean isOwnershipTrackingEnabled() {
        Boolean flag = AppConfig.var(ErpInvConstants.CONFIG_OWNERSHIP_TRACKING_ENABLED, Boolean.FALSE);
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 解析余额键中的 ownerId。disabled 时一律返回 null（不入键）；enabled 时透传调用方提供的 ownerId。
     * 标准移动单不携带 ownerId，故两态下均返回 null（写 OWNED 余额或 null-owner 子余额）。
     */
    Long resolveOwnerKey(Long ownerId) {
        return isOwnershipTrackingEnabled() ? ownerId : null;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
