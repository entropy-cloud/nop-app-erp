package app.erp.prj.service.posting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 工时过账汇率解析三态的派发器层单测（RC-R1.64）。
 *
 * <p>currencyId=null 短路回退 rate=1——不触达跨域查询（bizObjectManager 为 null 亦可解析），
 * 行为保持（替代 BigDecimal.ONE 硬编码路径的 null 分支语义不变）。
 * e2e 侧 currencyId=null 的 PostingEvent 因 ErpFinVoucherLine.currencyId NOT NULL
 * （finance ORM 既有约束，先于本行的行为）无法落库，故该分支在派发器层覆盖。
 */
public class TestTimesheetFxRateResolution {

    @Test
    public void testNullCurrencyShortCircuitsToRateOne() {
        TimesheetPostingDispatcher dispatcher = new TimesheetPostingDispatcher();
        assertEquals(0, dispatcher.resolveExchangeRate(null, LocalDate.of(2026, 7, 15))
                        .compareTo(BigDecimal.ONE),
                "currencyId=null → rate=1 回退（不触达币种/汇率查询）");
    }
}
