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
import java.util.Objects;
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

    // ---------- 并发首次 INSERT 自然键冲突重试（plan 2026-07-28-1249 P0-MA2-020） ----------

    /**
     * 确定性单线程模拟：先在另一会话落地一条余额（全非空自然键），再让本会话用相同自然键的 SAVING 候选调用
     * {@link StockMoveBookkeeper#updateBalanceWithRetry}。第一次 flush 的 INSERT 命中
     * UK_INV_STOCK_BALANCE_NATURAL → 捕获 → evict + reload 已落地行 → applyDelta 在 MANAGED 行上重试 →
     * tryUpdateWithVersionCheck 成功。
     *
     * <p>断言：无重复行（行数=1），totalQuantity 为两者之和（5+4=9），version 自增（0→1）。
     *
     * <p>注：使用全非空自然键（skuId/batchNo/ownerId 均设值），因 H2 默认 NULLS DISTINCT 语义下
     * NULL 列不参与 UNIQUE 比较；全非空场景方可稳定触发 UK 冲突。
     */
    @Test
    public void testConcurrentFirstMoveSameDimensionThrowsAndRetries() {
        // 隔离会话：先落地一条全键余额 → totalQty=5, version=0
        Long balanceId = persistBalanceDirectlyAllKeys(new BigDecimal("5"), new BigDecimal("5"));

        ormTemplate.runInSession(testSession -> {
            // 本会话构造 SAVING 候选（模拟并发首轮 findBalance==null → 新建候选），相同自然键
            ErpInvStockBalance savingCandidate = buildSavingCandidateAllKeys();

            // updateBalanceWithRetry 走 SAVING 分支 → flush 触发 INSERT → UK 冲突 → evict + reload → MANAGED 路径重试
            ErpInvStockBalance updated = bookkeeper.updateBalanceWithRetry(savingCandidate, b -> {
                b.setTotalQuantity(nz(b.getTotalQuantity()).add(new BigDecimal("4")));
                b.setAvailableQuantity(nz(b.getAvailableQuantity()).add(new BigDecimal("4")));
            });

            assertEquals(0, updated.getTotalQuantity().compareTo(new BigDecimal("9")),
                    "5 (已落地) + 4 (本会话增量) = 9");
            assertTrue(updated.getVersion().compareTo(0) > 0, "tryUpdate 后 version 自增");
            return null;
        });

        // 落盘验证：仅 1 行（无重复余额），total=9，version 自增
        ormTemplate.runInSession(checkSession -> {
            long matching = countRowsByNaturalKey();
            assertEquals(1L, matching, "UK_INV_STOCK_BALANCE_NATURAL 兜底：同维度仅 1 行（无 split-quantity corruption）");

            ErpInvStockBalance finalBalance = balanceDao().getEntityById(balanceId);
            assertEquals(0, finalBalance.getTotalQuantity().compareTo(new BigDecimal("9")),
                    "落盘 total = 5 + 4 = 9");
            assertTrue(finalBalance.getVersion().compareTo(0) > 0, "version 自增");
            return null;
        });
    }

    /**
     * 多线程并发首次写同维度（plan 2026-07-28-1249 P0-MA2-020）：
     * 2 线程同时构造同自然键 SAVING 候选并调 {@link StockMoveBookkeeper#updateBalanceWithRetry}。
     * 一线程成功 INSERT，另一线程命中 UK 冲突 → reload + update 路径成功。
     *
     * <p>断言（数据完整性）：最终仅 1 行余额（无重复行 / silent split），totalQuantity = threadCount × perQty。
     *
     * <p>使用全非空自然键以稳定触发 UK 冲突（H2 默认 NULLS DISTINCT）。
     */
    @Test
    public void testConcurrentFirstMoveMultiThreadNoDuplicateRows() throws Exception {
        int threadCount = 2;
        BigDecimal perQty = new BigDecimal("5");
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
                            // 每线程独立构造 SAVING 候选（同自然键），queue INSERT 后立即调 updateBalanceWithRetry
                            ErpInvStockBalance candidate = buildSavingCandidateAllKeys();
                            bookkeeper.updateBalanceWithRetry(candidate, b -> {
                                b.setTotalQuantity(nz(b.getTotalQuantity()).add(perQty));
                                b.setAvailableQuantity(nz(b.getAvailableQuantity()).add(perQty));
                                b.setTotalCost(nz(b.getTotalCost()).add(perQty.multiply(new BigDecimal("5"))));
                            });
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
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "全部 worker 应在 60s 内完成");
            if (firstError.get() != null) {
                throw new AssertionError("worker 线程抛错: " + firstError.get().getMessage(), firstError.get());
            }
        } finally {
            pool.shutdownNow();
        }

        // 落盘断言：仅 1 行（UK + retry 保证），total = threadCount × perQty = 10
        ormTemplate.runInSession(checkSession -> {
            long matching = countRowsByNaturalKey();
            assertEquals(1L, matching,
                    "并发首次 INSERT：UK + retry 保证无重复余额行（避免 silent split-quantity corruption）");

            List<ErpInvStockBalance> rows = balanceDao().findAllByQuery(new io.nop.api.core.beans.query.QueryBean());
            ErpInvStockBalance only = rows.stream()
                    .filter(this::matchesNaturalKey)
                    .findFirst().orElseThrow();
            assertEquals(0, only.getTotalQuantity().compareTo(new BigDecimal("10")),
                    "总入量 = 2 × 5 = 10（无丢失更新、无 split）");
            return null;
        });
    }

    /** 全非空自然键测试常量（避免与 persistBalanceDirectly 的 NULL-key 行混入）。 */
    static final Long SKU_ID_ALL_KEY = 99001L;
    static final String BATCH_NO_ALL_KEY = "BATCH-CONC-001";
    static final Long OWNER_ID_ALL_KEY = 99002L;

    /**
     * 直接持久化一条全键余额（绕过 strategy），全非空自然键。立即提交可见于其他会话。
     */
    private Long persistBalanceDirectlyAllKeys(BigDecimal total, BigDecimal avgCost) {
        ErpInvStockBalance balance = balanceDao().newEntity();
        balance.setOrgId(ORG_ID);
        balance.setMaterialId(MATERIAL_ID);
        balance.setSkuId(SKU_ID_ALL_KEY);
        balance.setWarehouseId(WAREHOUSE_ID);
        balance.setLocationId(LOCATION_ID);
        balance.setBatchNo(BATCH_NO_ALL_KEY);
        balance.setOwnerId(OWNER_ID_ALL_KEY);
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

    /**
     * 构造一条全键 SAVING 候选（与 {@link #persistBalanceDirectlyAllKeys} 同自然键）。
     * queue INSERT 后返回，模拟并发 findBalance==null → 新建候选 → 紧随 updateBalanceWithRetry。
     */
    private ErpInvStockBalance buildSavingCandidateAllKeys() {
        ErpInvStockBalance candidate = balanceDao().newEntity();
        candidate.setOrgId(ORG_ID);
        candidate.setMaterialId(MATERIAL_ID);
        candidate.setSkuId(SKU_ID_ALL_KEY);
        candidate.setWarehouseId(WAREHOUSE_ID);
        candidate.setLocationId(LOCATION_ID);
        candidate.setBatchNo(BATCH_NO_ALL_KEY);
        candidate.setOwnerId(OWNER_ID_ALL_KEY);
        candidate.setTotalQuantity(BigDecimal.ZERO);
        candidate.setReservedQuantity(BigDecimal.ZERO);
        candidate.setLockedQuantity(BigDecimal.ZERO);
        candidate.setAvailableQuantity(BigDecimal.ZERO);
        candidate.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        candidate.setAvgCost(new BigDecimal("5"));
        candidate.setTotalCost(BigDecimal.ZERO);
        candidate.setCurrencyId(CURRENCY_ID);
        candidate.setOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_OWNED);
        // queue INSERT，模拟 upsertBalance 的 findBalance==null 分支
        balanceDao().saveEntity(candidate);
        return candidate;
    }

    private boolean matchesNaturalKey(ErpInvStockBalance r) {
        return Objects.equals(r.getOrgId(), ORG_ID)
                && Objects.equals(r.getMaterialId(), MATERIAL_ID)
                && Objects.equals(r.getSkuId(), SKU_ID_ALL_KEY)
                && Objects.equals(r.getWarehouseId(), WAREHOUSE_ID)
                && Objects.equals(r.getLocationId(), LOCATION_ID)
                && Objects.equals(r.getBatchNo(), BATCH_NO_ALL_KEY)
                && Objects.equals(r.getOwnerId(), OWNER_ID_ALL_KEY);
    }

    private long countRowsByNaturalKey() {
        List<ErpInvStockBalance> rows = balanceDao().findAllByQuery(new io.nop.api.core.beans.query.QueryBean());
        return rows.stream().filter(this::matchesNaturalKey).count();
    }

    private ErpInvStockMove newIncomingMove() {
        ErpInvStockMove move = moveDao().newEntity();
        move.setCode("MV-CONC-IN-" + UUID.randomUUID());
        move.setMoveType(ErpInvConstants.MOVE_TYPE_INCOMING);
        move.setOrgId(ORG_ID);
        move.setBusinessDate(LocalDate.of(2026, 7, 28));
        move.setDestWarehouseId(WAREHOUSE_ID);
        move.setDestLocationId(LOCATION_ID);
        move.setDocStatus(ErpInvConstants.DOC_STATUS_CONFIRMED);
        move.setApproveStatus(ErpInvDaoConstants.APPROVE_STATUS_UNSUBMITTED);
        move.setPosted(false);
        return move;
    }

    private ErpInvStockMoveLine newIncomingLine(BigDecimal qty) {
        ErpInvStockMoveLine line = lineDao().newEntity();
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitCost(new BigDecimal("5"));
        line.setTotalCost(qty.multiply(new BigDecimal("5")));
        line.setCurrencyId(CURRENCY_ID);
        line.setDestLocationId(LOCATION_ID);
        return line;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
