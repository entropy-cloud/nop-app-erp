package app.erp.qa.service.posting;

import app.erp.common.test.FaultInjectionStubs;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * qa G4 故障注入测试（设计文档 §5.2）。harness 首次覆盖 qa 域。
 *
 * <p>{@link NcrPostingDispatcher#dispatchScrap} 是 6 域中唯一<b>无 try-catch</b> 的过账路径——
 * executor 异常直接传播给调用方（BizModel），由调用方事务回滚 / catch-swallow 恢复。
 * MR1.16 修复（AUTO_POST 默认经事务回滚覆盖）后 posted 三件套不被误置。
 *
 * <p>断言契约（设计文档 §4.2）：
 * <ul>
 *   <li>A1（posted 一致性）：异常传播时 {@code ncr.posted} 不被置 true（{@code setPosted} 在异常点之后，不可达）</li>
 *   <li>A2（异常可观测）：异常传播至调用方 = 可观测（非静默吞咽）</li>
 * </ul>
 *
 * <p>纯单元测试：{@code materialId=null} 使 {@code resolveStockBalance} 直接返回 null（不触及 daoProvider）。
 */
public class TestQaPostingFaultInjection {

    @Test
    public void testScrapPostingFailurePropagatesAndLeavesPostedFalse() {
        NcrPostingDispatcher dispatcher = new NcrPostingDispatcher();
        NcrPostingExecutor executor = new NcrPostingExecutor();
        executor.voucherBiz = FaultInjectionStubs.throwingVoucherBiz();
        dispatcher.setExecutor(executor);

        ErpQaNonConformance ncr = new ErpQaNonConformance();
        ncr.setCode("NCR-FAIL-001");
        ncr.setQuantity(new BigDecimal("10"));
        ncr.setMaterialId(null);

        assertThrows(NopException.class, () -> dispatcher.dispatchScrap(ncr, null),
                "NCR 过账异常应传播至调用方（dispatcher 无 try-catch，A2 可观测）");
        assertFalse(Boolean.TRUE.equals(ncr.getPosted()),
                "NCR 过账异常传播时 posted 不应被置 true（A1 posted 一致性）");
    }
}
