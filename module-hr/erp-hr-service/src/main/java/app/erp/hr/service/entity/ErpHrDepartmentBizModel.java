
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import app.erp.hr.service.ErpHrErrors;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrRecruitmentBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrRecruitment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

// 族 A/U20 豁免登记：本类为BizModel；daoFor 目标（ErpHrDepartment、ErpHrEmployee、ErpHrRecruitment）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
@BizModel("ErpHrDepartment")
public class ErpHrDepartmentBizModel extends AbstractErpCrudBizModel<ErpHrDepartment> implements IErpHrDepartmentBiz {

    @Inject
    IErpHrEmployeeBiz employeeBiz;
    @Inject
    IErpHrRecruitmentBiz recruitmentBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public ErpHrDepartmentBizModel() {
        setEntityName(ErpHrDepartment.class.getName());
    }

    /**
     * P1-CK-hr-001：部门删除引用守卫——删除前校验：在职员工、子部门、未关闭招聘单引用计数均为 0。
     * 修复前无任何守卫——含在职员工的部门删除后 Employee.departmentId 悬挂、组织树子部门提升为根节点
     * （节点静默重组），悬挂引用从源头污染员工档案。
     */
    @Override
    protected void defaultPrepareDelete(ErpHrDepartment entity, IServiceContext context) {
        // P1-CK-hr-001：跳过 super.defaultPrepareDelete（checkChildrenNotExistsWhenDelete 走
        // tree 模型 children 加载，部门 parentId 自关联未声明 tree 模型时该路径无效；本 guard 自行校验）
        String deptId = entity.getId();

        // 1. 子部门守卫
        long subDeptCount = countSubDepartments(deptId, context);
        if (subDeptCount > 0) {
            throw new NopException(ErpHrErrors.ERR_DEPT_HAS_SUB_DEPARTMENTS)
                    .param(ErpHrErrors.ARG_DEPT_ID, deptId)
                    .param(ErpHrErrors.ARG_DEPT_NAME, entity.getName())
                    .param(ErpHrErrors.ARG_REF_COUNT, subDeptCount);
        }

        // 2. 在职员工守卫（employmentStatus in {ACTIVE, PROBATION}）
        long activeEmpCount = countActiveEmployees(deptId, context);
        if (activeEmpCount > 0) {
            throw new NopException(ErpHrErrors.ERR_DEPT_HAS_EMPLOYEES)
                    .param(ErpHrErrors.ARG_DEPT_ID, deptId)
                    .param(ErpHrErrors.ARG_DEPT_NAME, entity.getName())
                    .param(ErpHrErrors.ARG_REF_COUNT, activeEmpCount);
        }

        // 3. 未关闭招聘单守卫（recruitmentStatus not in {CLOSED, REJECTED}）
        long openRecruitmentCount = countOpenRecruitments(deptId, context);
        if (openRecruitmentCount > 0) {
            throw new NopException(ErpHrErrors.ERR_DEPT_HAS_RECRUITMENTS)
                    .param(ErpHrErrors.ARG_DEPT_ID, deptId)
                    .param(ErpHrErrors.ARG_DEPT_NAME, entity.getName())
                    .param(ErpHrErrors.ARG_REF_COUNT, openRecruitmentCount)
                    .param(ErpHrErrors.ARG_REF_TYPE, "OPEN/IN_PROGRESS");
        }
    }

    private long countSubDepartments(String deptId, IServiceContext context) {
        // P1-CK-hr-001：QueryBean filter on parentId（FK 列）在 defaultPrepareDelete 上下文抛
        // unknown-query-prop（FK 列 isQueryable=false）。改为 findAll() + 内存过滤（部门子集有限）。
        java.util.List<ErpHrDepartment> all = ormTemplate.runInSession(s ->
                daoProvider().daoFor(ErpHrDepartment.class).findAll());
        long count = 0;
        for (ErpHrDepartment d : all) {
            if (deptId.equals(d.getParentId())) {
                count++;
            }
        }
        return count;
    }

    private long countActiveEmployees(String deptId, IServiceContext context) {
        // P1-CK-hr-001：departmentId 是 FK 列，filter 抛 unknown-query-prop。findAll + 内存过滤。
        java.util.List<ErpHrEmployee> all = ormTemplate.runInSession(s ->
                daoProvider().daoFor(ErpHrEmployee.class).findAll());
        long count = 0;
        for (ErpHrEmployee e : all) {
            if (deptId.equals(e.getDepartmentId())) {
                String status = e.getEmploymentStatus();
                if ("ACTIVE".equals(status) || "PROBATION".equals(status)) {
                    count++;
                }
            }
        }
        return count;
    }

    private long countOpenRecruitments(String deptId, IServiceContext context) {
        // P1-CK-hr-001：departmentId FK filter 抛 unknown-query-prop。findAll + 内存过滤。
        java.util.List<ErpHrRecruitment> all = ormTemplate.runInSession(s ->
                daoProvider().daoFor(ErpHrRecruitment.class).findAll());
        long count = 0;
        for (ErpHrRecruitment r : all) {
            if (deptId.equals(r.getDepartmentId())) {
                String status = r.getStatus();
                if (!"CLOSED".equals(status) && !"REJECTED".equals(status)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> findDepartmentTree(@Optional @Name("keyword") String keyword, IServiceContext context) {
        QueryBean deptQuery = new QueryBean();
        deptQuery.setLimit(5000);
        List<ErpHrDepartment> depts = findList(deptQuery, null, context);

        QueryBean empQuery = new QueryBean();
        empQuery.setLimit(5000);
        List<ErpHrEmployee> emps = employeeBiz.findList(empQuery, null, context);

        Map<String, Integer> empCountByDept = new HashMap<>();
        for (ErpHrEmployee e : emps) {
            if (e.getDepartmentId() != null) {
                empCountByDept.merge(e.getDepartmentId(), 1, Integer::sum);
            }
        }

        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (ErpHrDepartment d : depts) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("name", d.getName());
            node.put("code", d.getCode());
            node.put("managerId", d.getManagerId());
            node.put("parentId", d.getParentId());
            node.put("empCount", empCountByDept.getOrDefault(d.getId(), 0));
            node.put("children", new ArrayList<>());
            nodeMap.put(d.getId(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (ErpHrDepartment d : depts) {
            Map<String, Object> node = nodeMap.get(d.getId());
            String pid = d.getParentId();
            if (pid != null && !"0".equals(pid) && nodeMap.containsKey(pid)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(pid).get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            roots = filterTree(roots, kw);
        }
        return roots;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterTree(List<Map<String, Object>> nodes, String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            List<Map<String, Object>> filteredChildren = filterTree(
                    (List<Map<String, Object>>) node.get("children"), keyword);
            String name = String.valueOf(node.getOrDefault("name", ""));
            String code = String.valueOf(node.getOrDefault("code", ""));
            if (name.toLowerCase().contains(keyword) || code.toLowerCase().contains(keyword)
                    || !filteredChildren.isEmpty()) {
                node.put("children", filteredChildren);
                result.add(node);
            }
        }
        return result;
    }
}
