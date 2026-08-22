package app.erp.hr.service.processor;

import app.erp.hr.biz.IErpHrShiftAssignmentBiz;
import app.erp.hr.biz.IErpHrShiftBiz;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.hr.dao.entity.ErpHrShiftRotationPattern;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
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
 * ErpHrShiftRotationPattern generateRotation per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含轮换排班生成：解析 patternData（JSON 数组 of shiftCode）+ 校验 + 按 staggerDays 错峰逐成员逐日生成排班，
 * 可选 regenerate 先逻辑删除既有 SCHEDULED 排班（shift-scheduling.md §三）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>假设：原始 {@code requireEntity(String.valueOf(patternId), null, context)} 在模板不存在时抛平台
 * {@code UnknownEntityException}；此处按 R6.7 任务约定，模板不存在统一抛域错误码
 * {@link ErpHrErrors#ERR_SHIFT_ROTATION_PATTERN_INVALID} + {@link ErpHrErrors#ARG_PATTERN_ID}，
 * 与 patternData 非法/空序列的错误码一致，调用方据此判定模板无效。
 */
public class ErpHrShiftRotationPatternGenerateRotationProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpHrShiftBiz shiftBiz;

    @Inject
    IErpHrShiftAssignmentBiz assignmentBiz;

    public List<ErpHrShiftAssignment> generateRotation(String patternId, List<String> groupMemberIds, int staggerDays,
                                                       LocalDate startDate, LocalDate endDate, boolean regenerate,
                                                       IServiceContext context) {
        ErpHrShiftRotationPattern pattern = requirePattern(patternId, context);
        List<String> sequence = parseAndValidateSequence(pattern, context);
        int cycleLength = sequence.size();
        if (cycleLength == 0) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_PATTERN_ID, patternId);
        }
        Map<String, String> shiftCodeToId = buildShiftCodeMap(sequence, context);
        if (regenerate) {
            deleteExistingAssignments(groupMemberIds, startDate, endDate, context);
        }
        IEntityDao<ErpHrShiftAssignment> assignmentDao = daoProvider.daoFor(ErpHrShiftAssignment.class);
        List<ErpHrShiftAssignment> result = new ArrayList<>();
        for (int memberIdx = 0; memberIdx < groupMemberIds.size(); memberIdx++) {
            String employeeId = groupMemberIds.get(memberIdx);
            long staggerOffset = (long) staggerDays * memberIdx;
            LocalDate memberStart = startDate.plusDays(staggerOffset);
            long dayIndex = 0;
            for (LocalDate d = memberStart; !d.isAfter(endDate); d = d.plusDays(1)) {
                String shiftCode = sequence.get((int) (dayIndex % cycleLength));
                if (!ErpHrConstants.PATTERN_OFF_SHIFT_CODE.equals(shiftCode)) {
                    String shiftId = shiftCodeToId.get(shiftCode);
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

    protected ErpHrShiftRotationPattern requirePattern(String patternId, IServiceContext context) {
        ErpHrShiftRotationPattern pattern = daoProvider.daoFor(ErpHrShiftRotationPattern.class).getEntityById(patternId);
        if (pattern == null) {
            throw new NopException(ErpHrErrors.ERR_SHIFT_ROTATION_PATTERN_INVALID)
                    .param(ErpHrErrors.ARG_PATTERN_ID, patternId);
        }
        return pattern;
    }

    protected ErpHrShiftAssignment findActiveAssignment(IEntityDao<ErpHrShiftAssignment> dao, String employeeId, LocalDate date) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("assignmentDate", date),
                eq("status", ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED)));
        q.setLimit(1);
        List<ErpHrShiftAssignment> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpHrShiftAssignment newAssignment(IEntityDao<ErpHrShiftAssignment> dao, String employeeId, String shiftId, LocalDate date) {
        ErpHrShiftAssignment a = dao.newEntity();
        a.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        a.setEmployeeId(employeeId);
        a.setShiftId(shiftId);
        a.setAssignmentDate(date);
        a.setIsAbsent(false);
        a.setStatus(ErpHrConstants.ASSIGNMENT_STATUS_SCHEDULED);
        dao.saveEntity(a);
        return a;
    }

    /**
     * 解析 patternData（JSON 数组 of shiftCode），校验所有非 OFF 的 code 必须对应有效 Shift。
     */
    @SuppressWarnings("unchecked")
    protected List<String> parseAndValidateSequence(ErpHrShiftRotationPattern pattern, IServiceContext context) {
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

    protected Map<String, String> buildShiftCodeMap(List<String> sequence, IServiceContext context) {
        List<String> distinctCodes = new ArrayList<>();
        for (String c : sequence) {
            if (c == null || ErpHrConstants.PATTERN_OFF_SHIFT_CODE.equals(c)) {
                continue;
            }
            if (!distinctCodes.contains(c)) {
                distinctCodes.add(c);
            }
        }
        Map<String, String> map = new HashMap<>();
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

    protected void deleteExistingAssignments(List<String> employeeIds, LocalDate startDate, LocalDate endDate,
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
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider.daoFor(ErpHrShiftAssignment.class);
        // 逻辑删除（deleteVersionProp=delVersion 自增）使 UK_HR_SHIFT_ASSIGNMENT_NATURAL 允许同键重排：
        // 删除行 delVersion>0，重生成行 delVersion=0，互不冲突。仅状态置 CANCELLED 不变 delVersion 会触发 duplicate-key。
        for (ErpHrShiftAssignment a : existing) {
            dao.deleteEntity(a);
        }
        dao.flushSession();
    }
}
