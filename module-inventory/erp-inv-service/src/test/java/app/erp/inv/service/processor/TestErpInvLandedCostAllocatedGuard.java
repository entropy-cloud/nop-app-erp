package app.erp.inv.service.processor;

import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.ErpInvConstants;
import app.erp.inv.service.ErpInvErrors;
import app.erp.pur.dao.entity.ErpPurReceive;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 到岸成本防重复分摊守卫回归测试（plan 2026-08-20-2052-1 Phase 2，P1-RC-092 MySQL-RR TOCTOU 修复）。
 *
 * <p>验证 {@link ErpInvLandedCostProcessor#validateNotAlreadyAllocated} 的锁定读语义：
 * <ul>
 *   <li>顺序化 stale-window 序列：事务 A 提交 APPROVED sibling 后，事务 B 进入守卫必抛
 *       {@link ErpInvErrors#ERR_LANDED_COST_ALREADY_ALLOCATED}（错误码 + 参数语义保持）。</li>
 *   <li>session 陈旧态：B 的 session 已持有 sibling 陈旧 MANAGED 实例（DRAFT）时，守卫的
 *       unload→lock 仍读到最新已提交版本（PROXY 锁定读装配）——若无 unload 重置，lock 不覆写
 *       MANAGED 已初始化属性，陈旧将静默复归（本测试 pin 该 hardening）。</li>
 *   <li>语义保持：无 APPROVED sibling（REJECTED/CANCELLED 历史 sibling）放行；currentLandedCostId 排除。</li>
 * </ul>
 *
 * <p>H2 测试边界（对齐 A4.1.17 证据范式）：仓内 H2 测试基线为 READ_COMMITTED，语句级快照下无法直接
 * 复现 MySQL InnoDB REPEATABLE_READ 的事务级 MVCC 读视图陈旧——跨方言有效性以锁定读语义静态论证：
 * MySQL-RR 下一致读快照固定于事务首读、不随行锁获取刷新（缺陷根因，MySQL 8.0 Ref Manual §15.7.2.3），
 * 而 SELECT ... FOR UPDATE 锁定读读最新已提交版本（§15.7.2.4），故守卫对「并发已提交的 APPROVED
 * sibling」的可见性跨 H2-RC/PG-RC/MySQL-RR/MySQL-RC 一致成立（平台链路证据见 plan Phase 1
 * Decision Record §2：OrmSessionImpl.lock → JdbcEntityPersistDriver.lock 锁读值装配）。
 *
 * <p>置于 processor 包以访问 protected {@code validateNotAlreadyAllocated}/{@code lockReceiveForAllocation}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvLandedCostAllocatedGuard extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    ErpInvLandedCostProcessor landedCostProcessor;

    /**
     * 顺序化 stale-window 序列：事务 A 提交 APPROVED 后，事务 B（新事务/新读视图）进入守卫必抛。
     * 失败模式 = ERR_LANDED_COST_ALREADY_ALLOCATED（含 sibling code 参数）。
     */
    @Test
    public void testGuardSeesCommittedApprovedSibling() {
        Long receiveId = ormTemplate.runInSession(session -> seedReceive("RCV-GUARD-1"));
        Long siblingId = ormTemplate.runInSession(session -> seedLandedCost("LC-GUARD-1-A", receiveId,
                ErpInvConstants.DOC_STATUS_DRAFT, ErpInvConstants.APPROVE_STATUS_UNSUBMITTED));
        Long currentId = ormTemplate.runInSession(session -> seedLandedCost("LC-GUARD-1-B", receiveId,
                ErpInvConstants.DOC_STATUS_DRAFT, ErpInvConstants.APPROVE_STATUS_UNSUBMITTED));

        // 事务 A（独立事务提交）：sibling 审核通过（模拟并发方已提交的 APPROVED sibling）
        inTxnSession(() -> {
            markCommittedApproved(siblingId);
            return null;
        });

        // 事务 B（新事务）：锁 receive 后进入守卫 → 必见 A 已提交的 APPROVED sibling
        NopException ex = assertThrows(NopException.class, () -> inTxnSession(() -> {
            ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
            landedCostProcessor.lockReceiveForAllocation(receive);
            landedCostProcessor.validateNotAlreadyAllocated(receiveId, currentId);
            return null;
        }));

        assertEquals(ErpInvErrors.ERR_LANDED_COST_ALREADY_ALLOCATED.getErrorCode(), ex.getErrorCode(),
                "守卫抛 ERR_LANDED_COST_ALREADY_ALLOCATED");
        assertEquals("LC-GUARD-1-A", ex.getParam(ErpInvErrors.ARG_LANDED_COST_CODE),
                "错误参数携带 sibling 单号");
        assertEquals(receiveId, ex.getParam(ErpInvErrors.ARG_RECEIVE_ID), "错误参数携带 receiveId");
    }

    /**
     * session 陈旧态 hardening：B 的 session 先装载 sibling（DRAFT，MANAGED），随后并发事务提交 APPROVED，
     * B 在同一事务内进入守卫——unload 重置 + PK 锁定读必须读到最新已提交版本并抛错。
     *
     * <p>该序列在任意隔离级别可复现「session 实体状态陈旧于最新已提交版本」窗口：无 unload 步骤的
     * proxy-lock 实现会因 internalAssemble 不覆写 MANAGED 已初始化属性而读到陈旧 DRAFT 放行（静默失效）。
     */
    @Test
    public void testGuardLockReadDefeatsStaleSessionState() throws Exception {
        Long receiveId = ormTemplate.runInSession(session -> seedReceive("RCV-GUARD-2"));
        Long siblingId = ormTemplate.runInSession(session -> seedLandedCost("LC-GUARD-2-A", receiveId,
                ErpInvConstants.DOC_STATUS_DRAFT, ErpInvConstants.APPROVE_STATUS_UNSUBMITTED));
        Long currentId = ormTemplate.runInSession(session -> seedLandedCost("LC-GUARD-2-B", receiveId,
                ErpInvConstants.DOC_STATUS_DRAFT, ErpInvConstants.APPROVE_STATUS_UNSUBMITTED));

        CountDownLatch siblingPreloaded = new CountDownLatch(1);
        CountDownLatch siblingCommitted = new CountDownLatch(1);
        AtomicReference<NopException> caught = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // 线程 B：事务内先锁 receive + 预装载 sibling（session 持有 DRAFT MANAGED 实例）→ 等 A 提交 → 守卫必抛
            Future<Void> guardFuture = pool.submit(() -> {
                ContextProvider.newContext();
                try {
                    ormTemplate.runInSession(session ->
                            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
                                ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
                                landedCostProcessor.lockReceiveForAllocation(receive);
                                // 预装载 sibling：session 实体状态 = DRAFT（陈旧于即将提交的 APPROVED）
                                daoProvider.daoFor(ErpInvLandedCost.class).getEntityById(siblingId);
                                siblingPreloaded.countDown();
                                await(siblingCommitted);
                                try {
                                    landedCostProcessor.validateNotAlreadyAllocated(receiveId, currentId);
                                } catch (NopException e) {
                                    caught.set(e);
                                    return null;
                                }
                                return null;
                            }));
                } finally {
                    ContextProvider.instance().detachContext();
                }
                return null;
            });

            // 线程 A：等 B 预装载后提交 APPROVED sibling
            pool.submit(() -> {
                ContextProvider.newContext();
                try {
                    await(siblingPreloaded);
                    ormTemplate.runInSession(session ->
                            transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
                                markCommittedApproved(siblingId);
                                return null;
                            }));
                } finally {
                    siblingCommitted.countDown();
                    ContextProvider.instance().detachContext();
                }
                return null;
            });

            guardFuture.get(90, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        NopException ex = caught.get();
        assertTrue(ex != null, "session 陈旧态下锁定读仍须见最新已提交 APPROVED sibling 并抛错");
        assertEquals(ErpInvErrors.ERR_LANDED_COST_ALREADY_ALLOCATED.getErrorCode(), ex.getErrorCode(),
                "守卫抛 ERR_LANDED_COST_ALREADY_ALLOCATED（unload→lock hardening）");
    }

    /**
     * 语义保持：REJECTED/CANCELLED 历史 sibling 不阻断（锁后评估 approveStatus）；currentLandedCostId
     * 自身排除（同单重入不误判）；零 sibling 场景放行。成功模式 = 守卫不抛。
     */
    @Test
    public void testGuardPassesWithoutApprovedSibling() {
        Long receiveId = ormTemplate.runInSession(session -> seedReceive("RCV-GUARD-3"));
        Long rejectedId = ormTemplate.runInSession(session -> seedLandedCost("LC-GUARD-3-A", receiveId,
                ErpInvConstants.DOC_STATUS_CANCELLED, ErpInvConstants.APPROVE_STATUS_REJECTED));
        Long currentId = ormTemplate.runInSession(session -> seedLandedCost("LC-GUARD-3-B", receiveId,
                ErpInvConstants.DOC_STATUS_DRAFT, ErpInvConstants.APPROVE_STATUS_UNSUBMITTED));

        // REJECTED sibling + 自身排除：放行
        inTxnSession(() -> {
            ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).getEntityById(receiveId);
            landedCostProcessor.lockReceiveForAllocation(receive);
            landedCostProcessor.validateNotAlreadyAllocated(receiveId, currentId);
            return null;
        });

        // current 自身即使持有 APPROVED 状态也排除（幂等重入由 ALREADY_APPROVED 守卫负责，此处仅证 id 排除语义）
        inTxnSession(() -> {
            markCommittedApproved(rejectedId);
            landedCostProcessor.validateNotAlreadyAllocated(receiveId, rejectedId);
            return null;
        });

        // 零 sibling 场景：放行
        Long emptyReceiveId = ormTemplate.runInSession(session -> seedReceive("RCV-GUARD-3-EMPTY"));
        inTxnSession(() -> {
            ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).getEntityById(emptyReceiveId);
            landedCostProcessor.lockReceiveForAllocation(receive);
            landedCostProcessor.validateNotAlreadyAllocated(emptyReceiveId, 999999L);
            return null;
        });
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS), "并发协作方应在 30s 内到达");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待并发协作方被中断", ie);
        }
    }

    /** session 包裹 REQUIRES_NEW 事务（守卫路径要求事务内有 ORM session）。 */
    private <T> T inTxnSession(Supplier<T> body) {
        return ormTemplate.runInSession(session ->
                transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> body.get()));
    }

    private void markCommittedApproved(Long id) {
        IEntityDao<ErpInvLandedCost> dao = daoProvider.daoFor(ErpInvLandedCost.class);
        ErpInvLandedCost lc = dao.getEntityById(id);
        lc.setApproveStatus(ErpInvConstants.APPROVE_STATUS_APPROVED);
        lc.setDocStatus(ErpInvConstants.DOC_STATUS_DONE);
        dao.updateEntity(lc);
    }

    private Long seedReceive(String code) {
        ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).newEntity();
        receive.setCode(code);
        receive.setSupplierId(5001L);
        receive.setWarehouseId(6001L);
        receive.setBusinessDate(LocalDate.of(2026, 8, 20));
        receive.setCurrencyId(7001L);
        receive.setDocStatus("CONFIRMED");
        receive.setApproveStatus("APPROVED");
        receive.setReceiveStatus("NOT_RECEIVED");
        daoProvider.daoFor(ErpPurReceive.class).saveEntity(receive);
        return receive.getId();
    }

    private Long seedLandedCost(String code, Long receiveId, String docStatus, String approveStatus) {
        ErpInvLandedCost lc = daoProvider.daoFor(ErpInvLandedCost.class).newEntity();
        lc.setCode(code);
        lc.setReceiveId(receiveId);
        lc.setAllocationMethod(ErpInvConstants.ALLOC_METHOD_BY_AMOUNT);
        lc.setDocStatus(docStatus);
        lc.setApproveStatus(approveStatus);
        lc.setTotalCostAmount(BigDecimal.TEN);
        lc.setBusinessDate(LocalDate.of(2026, 8, 20));
        daoProvider.daoFor(ErpInvLandedCost.class).saveEntity(lc);
        return lc.getId();
    }
}
