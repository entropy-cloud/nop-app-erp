package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 排班分配 per-mutation Processor 共享基类（R6.7，{@code processor-extension-pattern.md} facade protected helper 范式）。
 * 承载 assignSingle/assignBatch/copyFromPeriod 共用的创建（含 UK 冲突翻译）、加载与一人一天一排班唯一约束守卫
 * （单一真相源）。子类只编排单 mutation 步骤顺序。
 *
 * <p>持久化与存在性检查经 {@link IErpHrShiftAssignmentBiz} 管道（对齐 {@code processor-extension-pattern.md:68}
 * 「Processor 内部优先调 I*Biz 或 CrudBizModel 安全能力」），与原 BizModel 直接调 CrudBizModel.saveEntity/findCount
 * 行为等价（含并发场景下 pre-check 与 UK flush-catch 的时序窗口）。
 */
public abstract class AbstractErpHrShiftAssignmentProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpHrShiftBiz shiftBiz;

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    protected IEntityDao<ErpHrShiftAssignment> assignmentDao() {
        return daoProvider.daoFor(ErpHrShiftAssignment.class);
    }

    protected ErpHrShiftAssignment doCreateAssignment(String employeeId, String shiftId, LocalDate date,
                                                      IServiceContext context) {
        ErpHrShiftAssignment assignment = assignmentBiz.newEntity();
        assignment.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        assignment.setEmployeeId(employeeId);
        assignment.setShiftId(shiftId);
        assignment.setAssignmentDate(date);
        assignment.setIsAbsent(false);
        assignment.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        assignmentBiz.saveEntity(assignment, null, context);
        // flush 触发 INSERT，命中 UK_HR_SHIFT_ASSIGNMENT_NATURAL（并发越过 assertNoExistingAssignment/existsActive 时）
        // → 翻译为友好错误码（plan 2026-07-30-0841-2 R1.28 P1-MA2-091）
        try {
            ((io.nop.orm.dao.IOrmEntityDao<?>) assignmentDao())
                    .getOrmTemplate().flushSession();
        } catch (Exception e) {
            if (app.erp.common.service.UniqueConstraintHelper.isUniqueConstraintViolation(e)) {
                throw new NopException(ErpHrErrors.ERR_HR_SHIFT_ASSIGNMENT_DUPLICATE)
                        .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                        .param(ErpHrErrors.ARG_ASSIGNMENT_DATE, date)
                        .param(ErpHrErrors.ARG_SHIFT_ID, shiftId);
            }
            throw e;
        }
        return assignment;
    }

    protected ErpHrShift requireShift(String shiftId, IServiceContext context) {
        ErpHrShift shift = shiftBiz.get(String.valueOf(shiftId), false, context);
        if (shift == null) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_SHIFT_ID, shiftId);
        }
        return shift;
    }

    protected void assertNoExistingAssignment(String employeeId, LocalDate date, IServiceContext context) {
        if (existsActiveAssignment(employeeId, date, context)) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_DUPLICATE_ASSIGNMENT)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_ASSIGNMENT_DATE, date);
        }
    }

    protected boolean existsActiveAssignment(String employeeId, LocalDate date, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("assignmentDate", date),
                in("status", activeStatuses())));
        q.setLimit(1);
        return assignmentBiz.findCount(q, context) > 0;
    }

    protected static List<String> activeStatuses() {
        List<String> list = new ArrayList<>();
        list.add(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        list.add(ErpHrConstants.ASSIGNMENT_STATUS_PRESENT);
        list.add(ErpHrConstants.ASSIGNMENT_STATUS_ABSENT);
        return list;
    }
}
