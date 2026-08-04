
package app.erp.hr.biz;

import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.annotations.core.Name;
import io.nop.orm.biz.ICrudBiz;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

import app.erp.hr.dao.entity.ErpHrDepartment;

public interface IErpHrDepartmentBiz extends ICrudBiz<ErpHrDepartment>{

    /**
     * 部门组织树（flux tree 原生渲染）。返回嵌套树结构：每节点含 id/name/code/managerId/empCount/children。
     * 员工计数经 ErpHrEmployee.departmentId 聚合。
     */
    @BizQuery
    List<Map<String, Object>> findDepartmentTree(@Optional @Name("keyword") String keyword, IServiceContext context);
}
