package app.erp.prj.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工时成本过账派发器。工时 APPROVED 后组装 {@link PostingEvent}(PROJECT_COST_COLLECTION)
 * 经 {@link ProjectPostingExecutor}（独立新事务由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code REQUIRES_NEW}
 * 承接）调用财务过账引擎。
 *
 * <p>对齐 assets/sales 失败语义：过账失败吞异常记日志、保持 APPROVED+{@code posted=false}，
 * 不阻塞终态。本类为 Facade 编排层，不持久化源单据——源单据 {@code posted} 标志由调用方 BizModel
 * 在主事务内统一持久化。
 *
 * <p>借方科目解析：项目类型 {@code defaultSubjectId}（缺失抛 {@link ErpPrjErrors#ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED}）；
 * 贷方科目：{@code erp-prj.default-payroll-subject-id}（为空抛
 * {@link ErpPrjErrors#ERR_PAYROLL_SUBJECT_NOT_CONFIGURED}）。
 */
public class TimesheetPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TimesheetPostingDispatcher.class);

    @Inject
    ProjectPostingExecutor executor;
    @Inject
    IDaoProvider daoProvider;

    /**
     * 工时审批通过后调用。成功返回 true（调用方据此置 posted=true）；失败吞异常返回 false（保持 posted=false）。
     */
    public boolean tryPost(ErpPrjTimesheet timesheet) {
        PostingEvent event = buildEvent(timesheet);
        try {
            Long voucherId = executor.postEvent(event);
            return voucherId != null;
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("工时过账失败，工时单 {} 保持 APPROVED、posted=false：{}", timesheet.getCode(), e.getMessage());
            } else {
                LOG.error("工时过账异常，工时单 {} 保持 APPROVED、posted=false", timesheet.getCode(), e);
            }
            return false;
        }
    }

    /**
     * 反审批前红字冲销已过账凭证（对齐 posting.md §冲销）。冲销是硬前置，失败向上抛出阻断状态迁移。
     */
    public void reverse(ErpPrjTimesheet timesheet) {
        executor.reverse(timesheet.getCode(), ErpFinBusinessType.PROJECT_COST_COLLECTION);
    }

    private PostingEvent buildEvent(ErpPrjTimesheet timesheet) {
        ErpPrjProject project = loadProject(timesheet.getProjectId());
        ErpPrjProjectType projectType = project != null && project.getProjectTypeId() != null
                ? loadProjectType(project.getProjectTypeId()) : null;
        ErpPrjActivityType activityType = timesheet.getActivityTypeId() != null
                ? loadActivityType(timesheet.getActivityTypeId()) : null;

        String debitSubjectCode = resolveSubjectCode(
                projectType != null ? projectType.getDefaultSubjectId() : null,
                ProjectCostCollectionProvider.SUBJECT_PROJECT_COST_DEFAULT);
        if (projectType == null || projectType.getDefaultSubjectId() == null) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, timesheet.getProjectId());
        }

        String creditSubjectCode = ErpPrjConfigs.defaultPayrollSubjectCode();
        if (creditSubjectCode == null) {
            throw new NopException(ErpPrjErrors.ERR_PAYROLL_SUBJECT_NOT_CONFIGURED)
                    .param(ErpPrjErrors.ARG_SUBJECT_CODE, ErpPrjConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID);
        }

        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.PROJECT_COST_COLLECTION);
        event.setBillHeadCode(timesheet.getCode());
        event.setOrgId(timesheet.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(timesheet.getOrgId()));
        event.setCurrencyId(timesheet.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = timesheet.getWorkDate() != null ? timesheet.getWorkDate()
                : io.nop.api.core.time.CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpPrjConstants.BILL_DATA_PROJECT_ID, timesheet.getProjectId());
        billData.put(ErpPrjConstants.BILL_DATA_TASK_ID, timesheet.getTaskId());
        billData.put(ErpPrjConstants.BILL_DATA_ACTIVITY_TYPE_ID, timesheet.getActivityTypeId());
        billData.put(ErpPrjConstants.BILL_DATA_HOURS, timesheet.getHours());
        billData.put(ErpPrjConstants.BILL_DATA_COST_RATE, timesheet.getCostRate());
        billData.put(ErpPrjConstants.BILL_DATA_COST_AMOUNT, parseAmount(timesheet.getCostAmount()));
        billData.put(ErpPrjConstants.BILL_DATA_DEBIT_SUBJECT_CODE, debitSubjectCode);
        billData.put(ErpPrjConstants.BILL_DATA_CREDIT_SUBJECT_CODE, creditSubjectCode);
        billData.put(ErpPrjConstants.BILL_DATA_SOURCE_BILL_TYPE, ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET);
        if (activityType != null) {
            billData.put("ACTIVITY_TYPE_SUBJECT_CODE",
                    resolveSubjectCode(activityType.getSubjectId(), debitSubjectCode));
        }
        event.setBillData(billData);
        return event;
    }

    private ErpPrjProject loadProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        return dao.getEntityById(projectId);
    }

    private ErpPrjProjectType loadProjectType(Long projectTypeId) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        return dao.getEntityById(projectTypeId);
    }

    private ErpPrjActivityType loadActivityType(Long activityTypeId) {
        IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
        return dao.getEntityById(activityTypeId);
    }

    private Long resolveAcctSchemaId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("orgId", orgId));
        q.setLimit(1);
        List<ErpMdAcctSchema> schemas = dao.findAllByQuery(q);
        return schemas.isEmpty() ? null : schemas.get(0).getId();
    }

    private String resolveSubjectCode(Long subjectId, String defaultCode) {
        if (subjectId == null) {
            return defaultCode;
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = dao.getEntityById(subjectId);
        if (subject == null || subject.getCode() == null || subject.getCode().trim().isEmpty()) {
            return defaultCode;
        }
        return subject.getCode().trim();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
