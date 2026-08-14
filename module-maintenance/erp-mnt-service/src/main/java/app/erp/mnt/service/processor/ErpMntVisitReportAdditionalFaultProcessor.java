package app.erp.mnt.service.processor;

import app.erp.mnt.biz.IErpMntRequestBiz;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ErpMntVisit reportAdditionalFault per-mutation Processor（RC-R1.31 / P1-RC-069，per-mutation Processor 范式）。
 * 自包含 IN_PROGRESS 守卫 → visit remark 追加（E3 追加语义 + 精度 1000 守卫）→ 经 {@link IErpMntRequestBiz#save}
 * 建新 OPEN 维护请求（E1：毫秒时间戳后缀 code 唯一性，重复上报零 UK 冲突）→ 返回新请求。
 * 不翻转 visit 状态（不中断本次维护）、不写 totalMinutes/result（E4 范围声明，工时归 complete 流程）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpMntVisitProcessor}。
 */
public class ErpMntVisitReportAdditionalFaultProcessor extends AbstractErpMntVisitProcessor {

    static final int REMARK_MAX_LENGTH = 1000;

    private static final DateTimeFormatter CODE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Inject
    IErpMntRequestBiz requestBiz;

    public ErpMntRequest reportAdditionalFault(Long visitId, String description, String priority,
                                               String remark, IServiceContext context) {
        ErpMntVisit visit = requireVisit(visitId, context);
        String from = visit.getStatus();
        if (!ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS.equals(from)) {
            throw illegalVisitTransition(visit, from, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS);
        }
        doAppendVisitRemark(visit, description, remark, context);
        return doCreateRequest(visit, description, priority, remark, context);
    }

    protected void doAppendVisitRemark(ErpMntVisit visit, String description, String remark,
                                       IServiceContext context) {
        String faultText = !StringHelper.isBlank(description) ? description : remark;
        String extra = "[额外故障] " + (faultText == null ? "" : faultText);
        String existing = visit.getRemark();
        String prefix = StringHelper.isEmpty(existing) ? "" : existing + "\n";
        String merged = prefix + extra;
        if (merged.length() > REMARK_MAX_LENGTH) {
            int keep = Math.max(0, REMARK_MAX_LENGTH - prefix.length());
            merged = prefix + extra.substring(0, Math.min(extra.length(), keep));
        }
        visit.setRemark(merged);
        visitDao().updateEntity(visit);
    }

    protected ErpMntRequest doCreateRequest(ErpMntVisit visit, String description, String priority,
                                            String remark, IServiceContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "REQ-VST-" + visit.getId() + "-"
                + CODE_TS_FORMAT.format(CoreMetrics.currentTimestamp().toLocalDateTime()));
        data.put("equipmentId", visit.getEquipmentId());
        data.put("requestDate", CoreMetrics.currentDate());
        data.put("description", description);
        data.put("priority", StringHelper.isBlank(priority) ? ErpMntDaoConstants.PRIORITY_NORMAL : priority);
        data.put("status", ErpMntDaoConstants.REQUEST_STATUS_OPEN);
        Long requestedBy = toLongUserId(context.getUserId());
        if (requestedBy == null) {
            requestedBy = visit.getAssignedTo();
        }
        data.put("requestedBy", requestedBy);
        if (!StringHelper.isBlank(remark)) {
            data.put("remark", remark);
        }
        return requestBiz.save(data, context);
    }

    private Long toLongUserId(String userId) {
        if (StringHelper.isEmpty(userId)) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
