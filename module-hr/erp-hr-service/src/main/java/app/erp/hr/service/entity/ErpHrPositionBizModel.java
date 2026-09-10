package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import app.erp.hr.service.ErpHrErrors;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import app.erp.hr.biz.IErpHrPositionBiz;
import app.erp.hr.dao.entity.ErpHrPosition;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

// 族 A/U20 豁免登记：本类为BizModel；daoFor 目标（）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
@BizModel("ErpHrPosition")
public class ErpHrPositionBizModel extends AbstractErpCrudBizModel<ErpHrPosition> implements IErpHrPositionBiz{
    @Inject
    app.erp.hr.biz.IErpHrEmployeeBiz employeeBiz;
    @Inject
    app.erp.hr.biz.IErpHrRecruitmentBiz recruitmentBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public ErpHrPositionBizModel(){
        setEntityName(ErpHrPosition.class.getName());
    }

    /**
     * P1-CK-hr-001：职位删除引用守卫——删除前校验：在职员工、未关闭招聘单引用计数均为 0。
     * 修复前无任何守卫——有编制的职位删除后 Employee.positionId / Recruitment.positionId 悬挂
     * （hire 联动把悬挂 positionId 写入新员工，从源头污染员工档案）。
     */
    @Override
    protected void defaultPrepareDelete(ErpHrPosition entity, IServiceContext context) {
        // P1-CK-hr-001：跳过 super.defaultPrepareDelete（同 Department 模型理由）
        String positionId = entity.getId();

        // 1. 在职员工守卫（employmentStatus in {ACTIVE, PROBATION}）
        long activeEmpCount = countActiveEmployees(positionId, context);
        if (activeEmpCount > 0) {
            throw new NopException(ErpHrErrors.ERR_POSITION_HAS_EMPLOYEES)
                    .param(ErpHrErrors.ARG_POSITION_ID, positionId)
                    .param(ErpHrErrors.ARG_REF_COUNT, activeEmpCount);
        }

        // 2. 未关闭招聘单守卫
        long openRecruitmentCount = countOpenRecruitments(positionId, context);
        if (openRecruitmentCount > 0) {
            throw new NopException(ErpHrErrors.ERR_POSITION_HAS_RECRUITMENTS)
                    .param(ErpHrErrors.ARG_POSITION_ID, positionId)
                    .param(ErpHrErrors.ARG_REF_COUNT, openRecruitmentCount);
        }
    }

    private long countActiveEmployees(String positionId, IServiceContext context) {
        // P1-CK-hr-001：positionId FK 列 filter 抛 unknown-query-prop。findAll + 内存过滤。
        java.util.List<app.erp.hr.dao.entity.ErpHrEmployee> all = ormTemplate.runInSession(s ->
                daoProvider().daoFor(app.erp.hr.dao.entity.ErpHrEmployee.class).findAll());
        long count = 0;
        for (app.erp.hr.dao.entity.ErpHrEmployee e : all) {
            if (positionId.equals(e.getPositionId())) {
                String status = e.getEmploymentStatus();
                if ("ACTIVE".equals(status) || "PROBATION".equals(status)) {
                    count++;
                }
            }
        }
        return count;
    }

    private long countOpenRecruitments(String positionId, IServiceContext context) {
        // P1-CK-hr-001：positionId FK filter 抛 unknown-query-prop。findAll + 内存过滤。
        java.util.List<app.erp.hr.dao.entity.ErpHrRecruitment> all = ormTemplate.runInSession(s ->
                daoProvider().daoFor(app.erp.hr.dao.entity.ErpHrRecruitment.class).findAll());
        long count = 0;
        for (app.erp.hr.dao.entity.ErpHrRecruitment r : all) {
            if (positionId.equals(r.getPositionId())) {
                String status = r.getStatus();
                if (!"CLOSED".equals(status) && !"REJECTED".equals(status)) {
                    count++;
                }
            }
        }
        return count;
    }

}