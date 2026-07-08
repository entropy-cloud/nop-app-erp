
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrEmployee;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;

/**
 * 员工主数据聚合根 Biz（use-cases.md UC-HR-08 部门调动）。
 *
 * <p>除标准 CRUD 外承载 {@link #transferEmployee}：HR 选员工→填目标部门/职位/上级→设生效日期→
 * 校验 + 更新部门/职位/上级 + 按 {@code handleContract} 处理合同 + 休假冲突告警。
 */
public interface IErpHrEmployeeBiz extends ICrudBiz<ErpHrEmployee> {

    /**
     * 部门调动（use-cases.md UC-HR-08）。单步直接更新语义（无审批/状态机）。
     *
     * <p>校验：(1) 员工雇佣状态可调动（ACTIVE/PROBATION）；(2) 目标部门存在；(3) 目标职位存在且归属目标部门（若传入）；
     * (4) 调动日期与 APPROVED 休假冲突时告警（config-gated，不阻塞）。
     *
     * <p>合同处理按 {@code handleContract} 三态：
     * <ul>
     *   <li>{@code AUTO}：按 config {@code erp-hr.transfer-auto-handle-contract}（默认 true）+ 仅当存在 ACTIVE 合同才处理；</li>
     *   <li>{@code YES}：强制处理（无 ACTIVE 合同时也新建）；</li>
     *   <li>{@code NO}：跳过合同处理，仅更新部门/职位/上级。</li>
     * </ul>
     * 处理合同时原 ACTIVE 合同 status→TERMINATED，并新建 ACTIVE 合同（承袭 contractType/signDate/期限，startDate=effectiveDate）。
     *
     * @param handleContract {@code AUTO}/{@code YES}/{@code NO}，null 视为 {@code AUTO}
     * @return 调动后的员工记录（部门/职位/上级已更新）
     */
    @BizMutation
    @SingleSession
    ErpHrEmployee transferEmployee(@Name("employeeId") Long employeeId,
                                   @Name("targetDepartmentId") Long targetDepartmentId,
                                   @Name("targetPositionId") Long targetPositionId,
                                   @Name("targetSuperiorId") Long targetSuperiorId,
                                   @Name("effectiveDate") LocalDate effectiveDate,
                                   @Name("handleContract") String handleContract,
                                   IServiceContext context);
}
