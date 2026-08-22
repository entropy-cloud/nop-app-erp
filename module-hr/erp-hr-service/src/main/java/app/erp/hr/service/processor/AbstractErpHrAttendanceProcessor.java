package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrAttendance;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 考勤打卡 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 clockIn/clockOut 共用的加载、保存与工时计算辅助（单一真相源）。子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpHrAttendanceProcessor {

    @Inject
    IDaoProvider daoProvider;

    protected IEntityDao<ErpHrAttendance> attendanceDao() {
        return daoProvider.daoFor(ErpHrAttendance.class);
    }

    protected ErpHrAttendance findAttendance(String employeeId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("date", date)));
        q.setLimit(1);
        List<ErpHrAttendance> list = attendanceDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected void saveOrUpdateAttendance(ErpHrAttendance attendance) {
        attendanceDao().saveOrUpdateEntity(attendance);
    }

    static BigDecimal computeWorkHours(LocalDateTime clockIn, LocalDateTime clockOut) {
        if (clockIn == null || clockOut == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(clockIn, clockOut).toMinutes();
        return BigDecimal.valueOf(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }
}
