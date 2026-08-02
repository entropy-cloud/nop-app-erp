package app.erp.fin.service.posting;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * finance G1/G2 故障注入测试（A3-unit，设计文档 §5.2）。
 *
 * <p>Proxy 桩 {@code IErpFinVoucherBiz.post} 抛异常（经 harness
 * {@link FaultInjectionStubs#throwingVoucherBiz()}）→ {@link NotesPostingDispatcher}
 * catch-swallow 返回 false（保持 posted=false 悬挂）。
 *
 * <p>断言契约（设计文档 §4.2）：
 * <ul>
 *   <li>A1（posted 一致性）：{@code tryPostReceivable} 返回 false（过账失败 → posted 不被置 true）</li>
 *   <li>A2（异常可观测）：dispatcher catch 非完全静默——catch 块 LOG.warn/error 留痕（异常被捕获不传播）</li>
 *   <li>A3-unit（finance dispatcher catch 可恢复性）：Facade 边界故障经真实 dispatcher catch 传播 → posted=false</li>
 * </ul>
 *
 * <p>注：finance sweep 完整链路（{@code ErpFinPostingException} 记录持久化 + 重试 + MANUAL 升级）由引擎内部
 * {@code ErpFinPostingExceptionRecorder} 承载，Facade 桩绕过 Recorder → 属 A3-integration successor（§4.3）。
 *
 * <p>纯单元测试：{@code orgId=null} 使 {@code AcctSchemaResolver.resolvePrimarySchemaId} 直接返回 null
 *（不触及 {@code daoProvider}），{@code NOTES_RECEIVABLE_RECEIVED}（非 DISCOUNTED）跳过 {@code loadDiscount}。
 */
public class TestFinPostingFaultInjection {

    @Test
    public void testNotesReceivablePostingFailureReturnsFalse() {
        NotesPostingDispatcher dispatcher = new NotesPostingDispatcher();
        FinPostingExecutor executor = new FinPostingExecutor();
        executor.voucherBiz = FaultInjectionStubs.throwingVoucherBiz();
        dispatcher.executor = executor;

        ErpFinNotesReceivable note = new ErpFinNotesReceivable();
        note.setCode("NR-FAIL-001");
        note.setOrgId(null);
        note.setAmountFunctional(new BigDecimal("1000"));

        boolean posted = dispatcher.tryPostReceivable(note, ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED);

        assertFalse(posted,
                "票据过账失败应吞异常返回 false（保持 posted=false 悬挂，A1）；catch LOG.warn 留痕（A2，A3-unit）");
    }
}
