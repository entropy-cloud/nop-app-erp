package app.erp.inv.service;

import app.erp.inv.dao.ErpInvDaoConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.service.costing.MovingAverageCostingStrategy;
import app.erp.inv.service.stock.StockMoveBookkeeper;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-INV-08 并发扣减乐观锁加固 + 并发测试框架（plan 2026-07-07-0024-2 Phase 1 Proof）。
 *
 * <p>覆盖 4 个并发行为场景，验证 {@link StockMoveBookkeeper#updateBalanceWithRetry} 实现的乐观锁 tryLock + 重试循环
 * （对齐平台 {@code concurrency-and-transactions.md} §模式四）：
 * <ul>
 *   <li>{@link #testConcurrentDeductRetrySucceeds()} — 冲突经重试最终成功（单线程模拟，确定性）。</li>
 *   <li>{@link #testConcurrentDeductRetryExhaustedThrows()} — 超过 max-retry 抛
 *       {@link ErpInvErrors#ERR_INV_CONCURRENT_DEDUCT_CONFLICT}（单线程模拟，确定性）。</li>
 *   <li>{@link #testConcurrentDeductNoOversell()} — 多线程并发扣同一余额，乐观锁保证不超扣（无丢失更新）。</li>
 *   <li>{@link #testConcurrentDeductWithNegativeStockAllowed()} — 允许负库存时并发扣减仍一致（最终可为负）。</li>
 * </ul>
 *
 * <p>多线程机制（Decision）：{@code ExecutorService} + {@code CountDownLatch} 栅栏同步起步；
 * 每线程独立 {@link IOrmTemplate#runInSession} 会话 + {@link ContextProvider} 线程本地上下文执行一次出库记账；
 * 主线程等待全部完成后断言最终余额。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvConcurrentDeduct extends JunitBaseTestCase {

    static final Long ORG_ID = 11001L;
    static final Long MATERIAL_ID = 12002L;
    static final Long WAREHOUSE_ID = 13002L;
    static final Long LOCATION_ID = 14002L;
    static final Long UOM_ID = 15002L;
    static final Long CURRENCY_ID = 16002L;
    static final Long ACCT_SCHEMA_ID = 17002L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    StockMoveBookkeeper bookkeeper;
    @Inject
    MovingAverageCostingStrategy movingAverageStrategy;

    /**
     * 冲突经重试最终成功：在测试会话内缓存 stale baseline，外部会话先扣减使版本前进，
     * 测试会话调用 {@code updateBalanceWithRetry} 触发乐观锁冲突 → evict + reload → 重新计算增量 → 成功落盘。
     *
     * <p>直接测 {@link StockMoveBookkeeper#updateBalanceWithRetry}（生产并发冲突的真实路径：
     * 平台 tryUpdateWithVersionCheck 失败 → evict + requireEntityById 刷新 baseline → 重算 + 重试），
     * 不经 strategy.onOutgoing——后者会经 upsertBalance.findAllByQuery 触发平台 lazyCheck
     * （cached 版本与 DB 不一致直接抛 ERR_ORM_ENTITY_VERSION_CHANGED），而 lazyCheck 仅在二次查询时触发，
     * 真实并发冲突路径是 tryUpdate 的 0-row 返回，不走 lazyCheck。
     */
    @Test
    public void testConcurrentDeductRetrySucceeds() {
        Long balanceId = persistBalanceDirectly(BigDecimal.TEN, new BigDecimal("5"));
        BigDecimal deductQty = new BigDecimal("4");

        ormTemplate.runInSession(outerSession -> {
            // 缓存 stale baseline（v=0, total=10）
            ErpInvStockBalance baseline = balanceDao().getEntityById(balanceId);
            assertEquals(0, baseline.getVersion().compareTo(0), "DB 默认 version=0");
            assertEquals(0, baseline.getTotalQuantity().compareTo(BigDecimal.TEN), "初始 total=10");

            // 模拟并发：另一会话扣减 -3 → DB total=7, version=1
            ormTemplate.runInNewSession(innerSession -> {
                ErpInvStockBalance concurrent = balanceDao().getEntityById(balanceId);
                concurrent.setTotalQuantity(concurrent.getTotalQuantity().subtract(new BigDecimal("3")));
                balanceDao().saveOrUpdateEntity(concurrent);
                return null;
            });

            // 直接调 updateBalanceWithRetry：第一次 tryUpdate WHERE v=0 失败（DB v=1）→ evict+reload → 重算(7-4=3) → 成功
            ErpInvStockBalance updated = bookkeeper.updateBalanceWithRetry(baseline, b -> {
                b.setTotalQuantity(b.getTotalQuantity().subtract(deductQty));
                b.setAvailableQuantity(b.getAvailableQuantity().subtract(deductQty));
            });

            assertEquals(0, updated.getTotalQuantity().compareTo(new BigDecimal("3")),
                    "重试成功后 in-memory total = 7 - 4 = 3");
            return null;
        });

        // 落盘验证（独立会话读 DB 最新值）：10 - 3 (并发) - 4 (本测试) = 3
        ormTemplate.runInSession(checkSession -> {
            ErpInvStockBalance finalBalance = balanceDao().getEntityById(balanceId);
            assertEquals(0, finalBalance.getTotalQuantity().compareTo(new BigDecimal("3")),
                    "落盘 total = 10 - 3 - 4 = 3");
            assertTrue(finalBalance.getVersion() >= 2, "version 至少自增两次（并发 +1，本测试 +1）");
            return null;
        });
    }

    /**
     * 重试耗尽抛 {@link ErpInvErrors#ERR_INV_CONCURRENT_DEDUCT_CONFLICT}：
     * max-retry=0 时第一次冲突即抛错（不重试）。
     */
    @Test
    public void testConcurrentDeductRetryExhaustedThrows() {
        Long balanceId = persistBalanceDirectly(BigDecimal.TEN, new BigDecimal("5"));
        BigDecimal deductQty = new BigDecimal("4");

        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_CONCURRENT_DEDUCT_MAX_RETRY, "0");
        try {
            ormTemplate.runInSession(outerSession -> {
                ErpInvStockBalance baseline = balanceDao().getEntityById(balanceId);

                // 外部会话扣减推进版本：DB v=0 → v=1
                ormTemplate.runInNewSession(innerSession -> {
                    ErpInvStockBalance concurrent = balanceDao().getEntityById(balanceId);
                    concurrent.setTotalQuantity(concurrent.getTotalQuantity().subtract(new BigDecimal("3")));
                    balanceDao().saveOrUpdateEntity(concurrent);
                    return null;
                });

                // 触发 updateBalanceWithRetry：第一次 tryUpdate WHERE v=0 失败，max-retry=0 → 抛错
                NopException ex = assertThrows(NopException.class, () ->
                        bookkeeper.updateBalanceWithRetry(baseline, b -> {
                            b.setTotalQuantity(b.getTotalQuantity().subtract(deductQty));
                            b.setAvailableQuantity(b.getAvailableQuantity().subtract(deductQty));
                        }));
                assertEquals(ErpInvErrors.ERR_INV_CONCURRENT_DEDUCT_CONFLICT.getErrorCode(), ex.getErrorCode(),
                        "重试耗尽抛 ERR_INV_CONCURRENT_DEDUCT_CONFLICT");
                return null;
            });
        } finally {
            AppConfig.getConfigProvider()
                    .assignConfigValue(ErpInvConstants.CONFIG_CONCURRENT_DEDUCT_MAX_RETRY,
                            String.valueOf(ErpInvConstants.CONCURRENT_DEDUCT_MAX_RETRY_DEFAULT));
        }

        // 落盘验证：仅并发会话扣减成功，本测试抛错未扣减 → total = 10 - 3 = 7
        ormTemplate.runInSession(checkSession -> {
            ErpInvStockBalance finalBalance = balanceDao().getEntityById(balanceId);
            assertEquals(0, finalBalance.getTotalQuantity().compareTo(new BigDecimal("7")),
                    "并发会话扣减 -3，本测试抛错未扣减 → total = 7");
            return null;
        });
    }

    /**
     * 多线程并发扣减同一余额：3 线程 × 每次扣 3，初始 10。乐观锁 + 重试保证全部成功，不丢失更新。
     * 最终 total = 10 - 3*3 = 1。
     */
    @Test
    public void testConcurrentDeductNoOversell() throws Exception {
        runMultiThreadedConcurrentDeduct(BigDecimal.TEN, new BigDecimal("3"), 3, false,
                new BigDecimal("1"));
    }

    /**
     * 允许负库存时并发扣减仍一致：2 线程 × 每次扣 2，初始 2。乐观锁放行（无并发限制），
     * 最终 total = 2 - 2*2 = -2（可为负且一致）。
     */
    @Test
    public void testConcurrentDeductWithNegativeStockAllowed() throws Exception {
        runMultiThreadedConcurrentDeduct(new BigDecimal("2"), new BigDecimal("2"), 2, true,
                new BigDecimal("-2"));
    }

    /**
     * 多线程并发扣减通用runner：栅栏同步起步，每线程独立 session 调 strategy.onOutgoing。
     *
     * @param initialTotal 初始余额总量
     * @param perDeductQty 每线程扣减量
     * @param threadCount  线程数
     * @param allowNegative 是否允许负库存（上下文配置，仅作语义标注；strategy 本身不依据此分支）
     * @param expectedFinal 期望最终 total = initialTotal - threadCount × perDeductQty
     */
    private void runMultiThreadedConcurrentDeduct(BigDecimal initialTotal, BigDecimal perDeductQty,
                                                  int threadCount, boolean allowNegative,
                                                  BigDecimal expectedFinal) throws Exception {
        Long balanceId = persistBalanceDirectly(initialTotal, new BigDecimal("5"));

        boolean prevNegativeFlag = AppConfig.var(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE);
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(allowNegative));

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    ContextProvider.newContext();
                    try {
                        startGate.await();
                        ormTemplate.runInSession(workerSession -> {
                            ErpInvStockMove move = newOutgoingMove();
                            moveDao().saveEntity(move);
                            ErpInvStockMoveLine line = newLine(perDeductQty);
                            line.setMoveId(move.getId());
                            lineDao().saveEntity(line);
                            movingAverageStrategy.onOutgoing(move, line, ACCT_SCHEMA_ID, bookkeeper);
                            return null;
                        });
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        ContextProvider.instance().detachContext();
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown();
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "全部 worker 线程应在 60s 内完成");
            if (firstError.get() != null) {
                throw new AssertionError("worker 线程抛错: " + firstError.get().getMessage(), firstError.get());
            }
        } finally {
            pool.shutdownNow();
            AppConfig.getConfigProvider()
                    .assignConfigValue(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, String.valueOf(prevNegativeFlag));
        }

        // 落盘断言：无丢失更新，最终 total = initial - sum(deducts)
        ormTemplate.runInSession(checkSession -> {
            ErpInvStockBalance finalBalance = balanceDao().getEntityById(balanceId);
            assertEquals(0, finalBalance.getTotalQuantity().compareTo(expectedFinal),
                    "并发扣减后 total = " + expectedFinal + "（无丢失更新）");
            return null;
        });

        // 流水断言：每个线程写 1 条出库流水
        ormTemplate.runInSession(checkSession -> {
            List<ErpInvStockLedger> ledgers = ledgerDao().findAllByQuery(
                    new io.nop.api.core.beans.query.QueryBean());
            long outLedgers = ledgers.stream().filter(l -> l.getQuantity() != null && l.getQuantity().signum() < 0).count();
            assertEquals(threadCount, outLedgers, "应写 " + threadCount + " 条出库流水（每线程一条）");
            return null;
        });
    }

    // ---------- helpers ----------

    /**
     * 直接持久化一条余额（绕过 strategy，避免触发乐观锁路径），立即提交可见于其他会话。
     * 新余额 version=0 → DB 落盘后为 version=0；首次扣减自增到 1。
     */
    private Long persistBalanceDirectly(BigDecimal total, BigDecimal avgCost) {
        ErpInvStockBalance balance = balanceDao().newEntity();
        balance.setOrgId(ORG_ID);
        balance.setMaterialId(MATERIAL_ID);
        balance.setWarehouseId(WAREHOUSE_ID);
        balance.setLocationId(LOCATION_ID);
        balance.setTotalQuantity(total);
        balance.setReservedQuantity(BigDecimal.ZERO);
        balance.setLockedQuantity(BigDecimal.ZERO);
        balance.setAvailableQuantity(total);
        balance.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        balance.setAvgCost(avgCost);
        balance.setTotalCost(total.multiply(avgCost));
        balance.setCurrencyId(CURRENCY_ID);
        balance.setOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_OWNED);
        balanceDao().saveEntityDirectly(balance);
        return balance.getId();
    }

    private ErpInvStockMove newOutgoingMove() {
        ErpInvStockMove move = moveDao().newEntity();
        move.setCode("MV-CONC-" + UUID.randomUUID());
        move.setMoveType(ErpInvConstants.MOVE_TYPE_OUTGOING);
        move.setOrgId(ORG_ID);
        move.setBusinessDate(LocalDate.of(2026, 7, 7));
        move.setSourceWarehouseId(WAREHOUSE_ID);
        move.setSourceLocationId(LOCATION_ID);
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        move.setApproveStatus(ErpInvDaoConstants.APPROVE_STATUS_UNSUBMITTED);
        move.setPosted(false);
        return move;
    }

    private ErpInvStockMoveLine newLine(BigDecimal qty) {
        ErpInvStockMoveLine line = lineDao().newEntity();
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(BigDecimal.ZERO);
        line.setTotalCost(BigDecimal.ZERO);
        line.setCurrencyId(CURRENCY_ID);
        line.setSourceLocationId(LOCATION_ID);
        return line;
    }

    private IEntityDao<ErpInvStockBalance> balanceDao() {
        return daoProvider.daoFor(ErpInvStockBalance.class);
    }

    private IEntityDao<ErpInvStockMove> moveDao() {
        return daoProvider.daoFor(ErpInvStockMove.class);
    }

    private IEntityDao<ErpInvStockMoveLine> lineDao() {
        return daoProvider.daoFor(ErpInvStockMoveLine.class);
    }

    private IEntityDao<ErpInvStockLedger> ledgerDao() {
        return daoProvider.daoFor(ErpInvStockLedger.class);
    }
}
