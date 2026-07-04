
package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.biz.IErpHrShiftRotationPatternBiz;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftRotationPattern;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 轮换排班模板 BizModel（shift-scheduling.md §三）。继承 {@link CrudBizModel} 标准 CRUD，
 * 扩展 {@link #generateRotation} 按 patternData + startDate + 组成员 + staggerDays 错峰生成排班。
 *
 * <p>跨实体读 Shift（班次模板）经注入 {@link IErpHrShiftBiz}；
 * 创建/删除排班经 {@link IErpHrShiftAssignmentBiz}（同域 I*Biz 统一入口）。
 */
@BizModel("ErpHrShiftRotationPattern")
public class ErpHrShiftRotationPatternBizModel extends CrudBizModel<ErpHrShiftRotationPattern>
        implements IErpHrShiftRotationPatternBiz {

    @Inject
    IErpHrShiftBiz shiftBiz;
    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    public ErpHrShiftRotationPatternBizModel() {
        setEntityName(ErpHrShiftRotationPattern.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public List<ErpHrShiftAssignment> generateRotation(@Name("patternId") Long patternId,
                                                       @Name("groupMemberIds") List<Long> groupMemberIds,
                                                       @Name("staggerDays") int staggerDays,
                                                       @Name("startDate") LocalDate startDate,
                                                       @Name("endDate") LocalDate endDate,
                                                       @Name("regenerate") boolean regenerate,
                                                       IServiceContext context) {
        ErpHrShiftRotationPattern pattern = requireEntity(String.valueOf(patternId), null, context);
        List<String> sequence = parseAndValidateSequence(pattern, context);
        int cycleLength = sequence.size();
        if (cycleLength == 0) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_PATTERN_ID, patternId);
        }
        Map<String, Long> shiftCodeToId = buildShiftCodeMap(sequence, context);
        if (regenerate) {
            deleteExistingAssignments(groupMemberIds, startDate, endDate, context);
        }
        IEntityDao<ErpHrShiftAssignment> assignmentDao = daoProvider().daoFor(ErpHrShiftAssignment.class);
        List<ErpHrShiftAssignment> result = new ArrayList<>();
        for (int memberIdx = 0; memberIdx < groupMemberIds.size(); memberIdx++) {
            Long employeeId = groupMemberIds.get(memberIdx);
            long staggerOffset = (long) staggerDays * memberIdx;
            LocalDate memberStart = startDate.plusDays(staggerOffset);
            long dayIndex = 0;
            for (LocalDate d = memberStart; !d.isAfter(endDate); d = d.plusDays(1)) {
                String shiftCode = sequence.get((int) (dayIndex % cycleLength));
                if (!ErpHrConstants.PATTERN_OFF_SHIFT_CODE.equals(shiftCode)) {
                    Long shiftId = shiftCodeToId.get(shiftCode);
                    if (shiftId != null && findActiveAssignment(assignmentDao, employeeId, d) == null) {
                        ErpHrShiftAssignment assignment = newAssignment(assignmentDao, employeeId, shiftId, d);
                        result.add(assignment);
                    }
                }
                dayIndex++;
            }
        }
        return result;
    }

    ErpHrShiftAssignment findActiveAssignment(IEntityDao<ErpHrShiftAssignment> dao, Long employeeId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("assignmentDate", date),
                eq("status", ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED)));
        q.setLimit(1);
        List<ErpHrShiftAssignment> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    ErpHrShiftAssignment newAssignment(IEntityDao<ErpHrShiftAssignment> dao, Long employeeId, Long shiftId, LocalDate date) {
        ErpHrShiftAssignment a = new ErpHrShiftAssignment();
        a.setEmployeeId(employeeId);
        a.setShiftId(shiftId);
        a.setAssignmentDate(date);
        a.setIsAbsent(false);
        a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        dao.saveEntity(a);
        return a;
    }

    // ---------- helpers ----------

    /**
     * 解析 patternData（JSON 数组 of shiftCode），校验所有非 OFF 的 code 必须对应有效 Shift。
     */
    @SuppressWarnings("unchecked")
    List<String> parseAndValidateSequence(ErpHrShiftRotationPattern pattern, IServiceContext context) {
        String data = pattern.getPatternData();
        if (data == null || data.trim().isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_PATTERN_ID, pattern.getId());
        }
        List<String> sequence;
        try {
            Object parsed = JsonTool.parseNonStrict(data);
            if (!(parsed instanceof List)) {
                throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                        .param(ErpHrErrors.ARG_PATTERN_ID, pattern.getId());
            }
            sequence = (List<String>) parsed;
        } catch (NopException e) {
            throw e;
        } catch (Exception e) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID, e)
                    .param(ErpHrErrors.ARG_PATTERN_ID, pattern.getId());
        }
        if (sequence.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_PATTERN_ID, pattern.getId());
        }
        return sequence;
    }

    Map<String, Long> buildShiftCodeMap(List<String> sequence, IServiceContext context) {
        List<String> distinctCodes = new ArrayList<>();
        for (String c : sequence) {
            if (c == null || ErpHrConstants.PATTERN_OFF_SHIFT_CODE.equals(c)) {
                continue;
            }
            if (!distinctCodes.contains(c)) {
                distinctCodes.add(c);
            }
        }
        Map<String, Long> map = new HashMap<>();
        for (String code : distinctCodes) {
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", code));
            q.setLimit(1);
            ErpHrShift shift = shiftBiz.findFirst(q, null, context);
            if (shift == null) {
                throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                        .param(ErpHrErrors.ARG_PATTERN_ID, "shiftCode=" + code);
            }
            map.put(code, shift.getId());
        }
        return map;
    }

    void deleteExistingAssignments(List<Long> employeeIds, LocalDate startDate, LocalDate endDate,
                                   IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                in("employeeId", employeeIds),
                dateBetween("assignmentDate", startDate, endDate),
                eq("status", ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED)));
        List<ErpHrShiftAssignment> existing = assignmentBiz.findList(q, null, context);
        if (existing.isEmpty()) {
            return;
        }
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider().daoFor(ErpHrShiftAssignment.class);
        for (ErpHrShiftAssignment a : existing) {
            a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_CANCELLED);
            dao.updateEntity(a);
        }
        dao.flushSession();
    }
}
