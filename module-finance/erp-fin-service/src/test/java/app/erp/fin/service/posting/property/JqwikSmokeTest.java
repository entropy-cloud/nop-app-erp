package app.erp.fin.service.posting.property;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * jqwik + JUnit Platform 6.x 兼容性冒烟测试（MQ Q3 Phase 1 基础设施探针）。
 *
 * <p>本类确认 jqwik-engine 经 ServiceLoader 被 surefire 的 JUnit Platform runner 拾取，
 * 且 jqwik 1.10.1 的字节码与本项目测试栈 JUnit Platform 6.0.3 二进制兼容。
 * 首跑绿即可保留作回归探针；不参与借贷平衡不变量断言（P1 见 {@code PropertyErpFinDebitCreditBalance}）。
 */
class JqwikSmokeTest {

    @Property(tries = 20, seed = "101")
    void additionIsCommutative(@ForAll int a, @ForAll int b) {
        assertTrue(a + b == b + a);
    }

    @Property(tries = 20, seed = "102")
    void positiveAmountsAreNonNegative(@ForAll("positiveAmounts") BigDecimal x) {
        assertTrue(x.signum() >= 0);
    }

    @Provide
    Arbitrary<BigDecimal> positiveAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("1000"))
                .ofScale(4);
    }
}
