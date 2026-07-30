package app.erp.inv.service.processor;

import app.erp.pur.dao.entity.ErpPurReceive;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 到岸成本审核并发互斥测试（plan 2026-07-30-0841-2 R1.28 P1-MA2-085）。
 *
 * <p>验证 {@link ErpInvLandedCostProcessor#lockReceiveForAllocation(ErpPurReceive)} 经
 * {@link IOrmTemplate#lock}（SELECT ... FOR UPDATE）串行化并发同 receiveId 审核：
 * <ul>
 *   <li>单线程：lock 在 fresh receive 上成功获取（不抛异常、不污染 version）。</li>
 *   <li>多线程：两线程并发 lock 同一 receive，临界区不重叠（互斥），无 version 污染。</li>
 * </ul>
 *
 * <p>置于 processor 包以访问 protected {@code lockReceiveForAllocation}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLandedCostReceiveMutex extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    ErpInvLandedCostProcessor landedCostProcessor;

    @Test
    public void testLockFreshReceiveAcquiresWithoutError() {
        Long receiveId = ormTemplate.runInSession(session -> seedReceive("RCV-MUTEX-FRESH"));

        // 无并发：lock（SELECT FOR UPDATE）在事务内获取成功（不抛异常），且不污染 version（仍为 0）
        transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
            ErpPurReceive fresh = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
            landedCostProcessor.lockReceiveForAllocation(fresh);
            return null;
        });
        ErpPurReceive after = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
        assertEquals(0, after.getVersion(), "lock(SELECT FOR UPDATE) 不应污染 receive version");
    }

    /**
     * 两线程并发 lock 同一 receive：SELECT FOR UPDATE 串行化，临界区不重叠（互斥）。
     */
    @Test
    public void testLockSerializesConcurrentAccess() throws Exception {
        Long receiveId = ormTemplate.runInSession(session -> seedReceive("RCV-MUTEX-2T"));

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger inCritical = new AtomicInteger(0);
        AtomicInteger maxOverlap = new AtomicInteger(0);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ContextProvider.newContext();
                    try {
                        startGate.await();
                        transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
                            // 临界区：lock 持有至事务提交（lambda 结束）
                            ErpPurReceive r = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
                            landedCostProcessor.lockReceiveForAllocation(r);
                            int now = inCritical.incrementAndGet();
                            maxOverlap.accumulateAndGet(now, Math::max);
                            // 短暂停留以暴露并发重叠（若 lock 失效则两线程同时进入）
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            inCritical.decrementAndGet();
                            return null;
                        });
                    } catch (Throwable t) {
                        // ignore
                    } finally {
                        ContextProvider.instance().detachContext();
                        doneLatch.countDown();
                    }
                });
            }
            startGate.countDown();
            assertTrue(doneLatch.await(90, TimeUnit.SECONDS), "全部 worker 应在 90s 内完成");
        } finally {
            pool.shutdownNow();
        }

        assertTrue(maxOverlap.get() <= 1,
                "SELECT FOR UPDATE 应串行化并发 lock：临界区最大并发=" + maxOverlap.get() + "（应 ≤1）");
    }

    private Long seedReceive(String code) {
        ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).newEntity();
        receive.setCode(code);
        receive.setSupplierId(5001L);
        receive.setWarehouseId(6001L);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(7001L);
        receive.setDocStatus("CONFIRMED");
        receive.setApproveStatus("APPROVED");
        receive.setReceiveStatus("NOT_RECEIVED");
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);
        return receive.getId();
    }
}
