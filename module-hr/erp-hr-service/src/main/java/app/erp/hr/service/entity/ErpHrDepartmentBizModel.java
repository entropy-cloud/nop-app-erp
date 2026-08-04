
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpHrDepartment")
public class ErpHrDepartmentBizModel extends CrudBizModel<ErpHrDepartment> implements IErpHrDepartmentBiz {

    @Inject
    IErpHrEmployeeBiz employeeBiz;

    public ErpHrDepartmentBizModel() {
        setEntityName(ErpHrDepartment.class.getName());
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

        Map<Long, Integer> empCountByDept = new HashMap<>();
        for (ErpHrEmployee e : emps) {
            if (e.getDepartmentId() != null) {
                empCountByDept.merge(e.getDepartmentId(), 1, Integer::sum);
            }
        }

        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
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
            Long pid = d.getParentId();
            if (pid != null && pid != 0 && nodeMap.containsKey(pid)) {
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
