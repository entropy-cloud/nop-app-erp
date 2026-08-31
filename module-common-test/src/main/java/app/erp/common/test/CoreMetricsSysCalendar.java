package app.erp.common.test;

import io.nop.api.core.time.CoreMetrics;
import io.nop.api.core.time.DefaultSysCalendar;
import io.nop.api.core.time.ISysCalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经 CoreMetrics IClock 时间线读取日期的 ISysCalendar（bug 2026-09-01-0058 ast/cs 侧修复）。
 * 平台默认 nopSysCalendar（DefaultSysCalendar）直读系统时钟，绕过冻结时钟扩展；
 * 测试 delta 覆盖 bean id nopSysCalendar 后，CodeRule 日期段（如 TK{@year}{@month} 月前缀）
 * 即随 ThreadLocalFrozenClock 冻结联动。未冻结线程委托系统真实时钟，生产行为不变。
 */
public class CoreMetricsSysCalendar implements ISysCalendar {

    @Override
    public boolean isWorkDay(LocalDate date) {
        return DefaultSysCalendar.INSTANCE.isWorkDay(date);
    }

    @Override
    public LocalDate nextWorkDay(LocalDate date) {
        return DefaultSysCalendar.INSTANCE.nextWorkDay(date);
    }

    @Override
    public LocalDate getSysDate() {
        return CoreMetrics.currentDate();
    }

    @Override
    public LocalDateTime getSysDateTime() {
        return CoreMetrics.currentDateTime();
    }
}
