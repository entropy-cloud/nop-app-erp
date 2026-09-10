package app.erp.common.service;

import io.nop.api.core.exceptions.NopException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * M2.8 分片③ common-013-r3 红态先行（F 族复合期望态串契约负例）：
 * {@link AbstractSubmitForApprovalProcessor#validateTransitionForSubmit} 守卫触发的
 * {@code illegalStatusException(entity, current, expected...)} 必须把每个期望状态作为
 * **独立 vararg** 传递（基类契约 javadoc：状态码本身，禁散文/复合串）；
 * 修复前把 {@code unsubmittedStatus() + " / " + rejectedStatus()} 预拼接为单一复合串传递，
 * 期望态串含 " / " 分隔符且 vararg 数量=1 → 本测试红；改双 varargs 后绿。
 *
 * <p>~120 子类继承面单点收敛：负例锚定「复合串不落参数」契约，防回归。
 */
public class TestSubmitForApprovalExpectationContract {

    /** 捕获 illegalStatusException 收到的 vararg 形态（契约观测点） */
    static class CapturingProcessor extends AbstractSubmitForApprovalProcessor<io.nop.orm.support.OrmEntity> {
        String current;
        List<String> expectedArgs = new ArrayList<>();

        CapturingProcessor() {
            super("TestEntity");
        }

        @Override
        protected NopException illegalStatusException(io.nop.orm.support.OrmEntity entity, String current,
                                                      String... expected) {
            this.current = current;
            this.expectedArgs.clear();
            for (String e : expected) {
                this.expectedArgs.add(e);
            }
            return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCommonErrors.ARG_CURRENT_STATUS, current)
                    .param(ErpCommonErrors.ARG_EXPECTED_STATUS, String.join("|", expected));
        }

        @Override
        protected io.nop.dao.api.IEntityDao<io.nop.orm.support.OrmEntity> dao() {
            throw new UnsupportedOperationException("契约测试不需要 dao");
        }

        @Override
        protected NopException notFoundException(String id) {
            return new NopException(ErpCommonErrors.ERR_ENTITY_NOT_FOUND)
                    .param(ErpCommonErrors.ARG_BIZ_OBJ_ID, id);
        }

        @Override
        protected String getApproveStatus(io.nop.orm.support.OrmEntity entity) {
            return "APPROVED";
        }

        @Override
        protected void setApproveStatus(io.nop.orm.support.OrmEntity entity, String status) {
        }

        @Override
        protected boolean isCancelled(io.nop.orm.support.OrmEntity entity) {
            return false;
        }

        @Override
        protected String unsubmittedStatus() {
            return "UNSUBMITTED";
        }

        @Override
        protected String submittedStatus() {
            return "SUBMITTED";
        }

        @Override
        protected String rejectedStatus() {
            return "REJECTED";
        }
    }

    @Test
    public void testExpectedStatusesAreIndependentVarargsNotCompositeString() {
        CapturingProcessor p = new CapturingProcessor();
        try {
            p.validateTransitionForSubmit(null, null);
            fail("APPROVED 状态提交应被守卫拒绝");
        } catch (NopException e) {
            // 契约断言 1：期望态为独立 vararg（数量 = 2：UNSUBMITTED + REJECTED），非预拼接单串
            assertEquals(2, p.expectedArgs.size(),
                    "期望态必须以独立 vararg 传递（common-013-r3），实际=" + p.expectedArgs);
            // 契约断言 2：任一期望态串不得含复合分隔符 " / "（禁复合散文串）
            for (String arg : p.expectedArgs) {
                org.junit.jupiter.api.Assertions.assertFalse(arg.contains(" / "),
                        "期望态不得为复合串（负例：\"A / B\" 预拼接违反契约），实际=" + arg);
            }
            // 契约断言 3：current 为状态码本身
            assertNotNull(p.current);
            assertEquals("APPROVED", p.current);
        }
    }
}
