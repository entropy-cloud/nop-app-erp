package app.erp.hr.service;

import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrPosition;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 员工部门调动端到端行为测试（use-cases.md UC-HR-08）。覆盖：
 * <ul>
 *   <li>正常调动：部门/职位/上级更新（ACTIVE 状态员工）。</li>
 *   <li>不可调动状态守门（RESIGNED/TERMINATED/RETIRED 抛 ERR_EMPLOYEE_NOT_TRANSFERABLE）。</li>
 *   <li>目标部门/职位不存在守门。</li>
 *   <li>合同处理 AUTO：原 ACTIVE 合同→TERMINATED + 新建 ACTIVE 合同 startDate=effectiveDate。</li>
 *   <li>合同处理 NO：不触及合同。</li>
 *   <li>合同处理 YES：无 ACTIVE 合同时仍新建。</li>
 *   <li>休假冲突告警不阻塞：调动日期落入 APPROVED 休假区间，调动仍成功。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrEmployeeTransfer extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrEmployeeBiz employeeBiz;

    @Test
    public void testTransferUpdatesDepartmentPositionSuperior() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-NORMAL", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptFromId = seedDepartment("DEPT-FROM");
            Long deptToId = seedDepartment("DEPT-TO");
            Long posId = seedPosition("POS-DEV", deptToId);
            Long superiorId = seedEmployee("EMP-BOSS", ErpHrConstants.EMPLOYMENT_ACTIVE);
            return new Object[]{empId, deptFromId, deptToId, posId, superiorId};
        });
        Long empId = (Long) ids[0];
        Long deptToId = (Long) ids[2];
        Long posId = (Long) ids[3];
        Long superiorId = (Long) ids[4];

        ErpHrEmployee result = ormTemplate.runInSession(session -> employeeBiz.transferEmployee(
                empId, deptToId, posId, superiorId,
                LocalDate.of(2026, 7, 8), ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));

        assertEquals(deptToId, result.getDepartmentId(), "部门应更新为目标部门");
        assertEquals(posId, result.getPositionId(), "职位应更新为目标职位");
        assertEquals(superiorId, result.getSuperiorId(), "上级应更新为目标上级");

        ErpHrEmployee refreshed = daoProvider.daoFor(ErpHrEmployee.class).getEntityById(empId);
        assertEquals(deptToId, refreshed.getDepartmentId());
        assertEquals(posId, refreshed.getPositionId());
        assertEquals(superiorId, refreshed.getSuperiorId());
    }

    @Test
    public void testTransferRejectsNonTransferableStatus() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-RESIGNED", ErpHrConstants.EMPLOYMENT_RESIGNED);
            Long deptId = seedDepartment("DEPT-TARGET-R");
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> employeeBiz.transferEmployee(empId, deptId, null, null,
                        LocalDate.of(2026, 7, 8),
                        ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX)));
        assertEquals(ErpHrErrors.ERR_EMPLOYEE_NOT_TRANSFERABLE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testTransferRejectsUnknownTargetDepartment() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-DEPT-404", ErpHrConstants.EMPLOYMENT_ACTIVE);
            return new Object[]{empId};
        });
        Long empId = (Long) ids[0];

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> employeeBiz.transferEmployee(empId, 99999999L, null, null,
                        LocalDate.of(2026, 7, 8),
                        ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX)));
        assertEquals(ErpHrErrors.ERR_TRANSFER_TARGET_DEPT_NOT_FOUND.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testTransferRejectsPositionNotInTargetDepartment() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-POS-MISMATCH", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptAId = seedDepartment("DEPT-A");
            Long deptBId = seedDepartment("DEPT-B");
            Long posInBId = seedPosition("POS-IN-B", deptBId);
            return new Object[]{empId, deptAId, posInBId};
        });
        Long empId = (Long) ids[0];
        Long deptAId = (Long) ids[1];
        Long posInBId = (Long) ids[2];

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> employeeBiz.transferEmployee(empId, deptAId, posInBId, null,
                        LocalDate.of(2026, 7, 8),
                        ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX)));
        assertEquals(ErpHrErrors.ERR_TRANSFER_TARGET_POSITION_NOT_FOUND.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testTransferContractAutoTerminatesOldAndCreatesNew() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CONTRACT-AUTO", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-AUTO");
            Long oldContractId = seedContract("CTC-OLD-AUTO", empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31),
                    "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];
        Long oldContractId = (Long) ids[2];
        LocalDate effective = LocalDate.of(2026, 7, 8);

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empId, deptId, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_AUTO, CTX));

        ErpHrEmploymentContract oldContract = daoProvider.daoFor(ErpHrEmploymentContract.class)
                .getEntityById(oldContractId);
        assertEquals(ErpHrConstants.CONTRACT_STATUS_TERMINATED, oldContract.getStatus(),
                "原合同应被终止");

        ErpHrEmploymentContract newContract = findActiveContract(empId);
        assertNotNull(newContract, "应新建 ACTIVE 合同");
        assertNotEquals(oldContractId, newContract.getId());
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, newContract.getStatus());
        assertEquals(effective, newContract.getStartDate(), "新合同 startDate 应为生效日期");
        assertEquals("FIXED_TERM", newContract.getContractType(), "新合同应承袭 contractType");
    }

    @Test
    public void testTransferContractNoDoesNotTouchContracts() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CONTRACT-NO", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-NO");
            Long oldContractId = seedContract("CTC-OLD-NO", empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31),
                    "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];
        Long oldContractId = (Long) ids[2];

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empId, deptId, null, null,
                LocalDate.of(2026, 7, 8),
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));

        ErpHrEmploymentContract oldContract = daoProvider.daoFor(ErpHrEmploymentContract.class)
                .getEntityById(oldContractId);
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, oldContract.getStatus(),
                "NO 模式不应触及原合同状态");
    }

    @Test
    public void testTransferContractYesCreatesEvenWithoutActive() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-CONTRACT-YES", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-YES");
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];
        LocalDate effective = LocalDate.of(2026, 7, 8);

        assertNull(findActiveContract(empId), "前置：员工无 ACTIVE 合同");

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empId, deptId, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES, CTX));

        ErpHrEmploymentContract created = findActiveContract(empId);
        assertNotNull(created, "YES 模式即使无 ACTIVE 合同也应新建");
        assertEquals(ErpHrConstants.CONTRACT_STATUS_ACTIVE, created.getStatus());
        assertEquals(effective, created.getStartDate());
    }

    @Test
    public void testTransferLeaveConflictWarnDoesNotBlock() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-LEAVE-CONFLICT", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-LEAVE");
            seedApprovedLeave(empId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15));
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];

        ErpHrEmployee result = ormTemplate.runInSession(session -> employeeBiz.transferEmployee(
                empId, deptId, null, null,
                LocalDate.of(2026, 7, 10),
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));

        assertNotNull(result, "调动日期落入 APPROVED 休假区间应仅告警不阻塞");
        assertEquals(deptId, result.getDepartmentId());
    }

    @Test
    public void testTransferProbationEmployeeAllowed() {
        Object[] ids = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-PROBATION", ErpHrConstants.EMPLOYMENT_PROBATION);
            Long deptId = seedDepartment("DEPT-PROB");
            return new Object[]{empId, deptId};
        });
        Long empId = (Long) ids[0];
        Long deptId = (Long) ids[1];

        ErpHrEmployee result = ormTemplate.runInSession(session -> employeeBiz.transferEmployee(
                empId, deptId, null, null,
                LocalDate.of(2026, 7, 8),
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_NO, CTX));
        assertEquals(deptId, result.getDepartmentId(), "PROBATION 状态员工应允许调动");
    }

    /**
     * 回归（plan {@code 2026-07-18-0347-1}）：{@code ErpHrEmployeeBizModel.buildSuccessorCode} 长度守护。
     *
     * <p>修复前 {@code buildSuccessorCode} 拼接 {@code "TRF-" + empId + "-" + effectiveDate + "-" + active.code}
     * 无总长约束，写入 {@link ErpHrEmploymentContract#getCode()} 列（domain {@code code} precision=50）。
     * 长 active.code（&gt;≈24 字符）+ empId + ISO date 总和超过 50 → {@code sqlState=22001} 字符串右截断 →
     * {@code transferEmployee} 事务回滚 → 员工调动路径完全不可用（0100-2 E2E 实证）。
     *
     * <p>修复后：超限路径保留 {@code "TRF-" + empId + "-" + effectiveDate} 固定段 + active.code 头部截断 + MD5 前 4 hex 摘要。
     * 本测试经真实 {@code transferEmployee} 链路（非直接静态调用，因 {@code buildSuccessorCode} 在
     * {@code app.erp.hr.service.entity} 包内 package-private，而本测试在 {@code app.erp.hr.service} 包）
     * 端到端验证 4 个子用例：
     * <ul>
     *   <li>(a) 短 active.code 路径逐字符不变（拼接结果 ≤ 50）；</li>
     *   <li>(b) 长 active.code（35 字符）触发截断 + 哈希分支，不抛 22001 + code ≤ 50 + 保留固定段；</li>
     *   <li>(c) 两条不同长 active.code 经摘要产生不同 successor code；</li>
     *   <li>(d) 无 ACTIVE 合同（active=null）走 base 路径，拼接 = TRF-{empId}-{effectiveDate}。</li>
     * </ul>
     */
    @Test
    public void testLongActiveCodeDoesNotOverflowCodePrecision() {
        LocalDate effective = LocalDate.of(2026, 7, 8);
        String longActiveCode1 = "HR-EMP-DEPT-2026-Q3-CONTRACT-001-X"; // 34 chars, > 24 触发截断 + 哈希分支
        String longActiveCode2 = "HR-EMP-DEPT-2026-Q3-CONTRACT-002-Y"; // 34 chars，头部相同尾部不同
        assertEquals(34, longActiveCode1.length(), "长码字面量长度自检 (b)/(c)");
        assertEquals(34, longActiveCode2.length(), "长码字面量长度自检 (c)");

        // ===== (b) 长 active.code 端到端：不抛 22001 + 新合同 code ≤ 50 + 保留固定段 =====
        Object[] idsLong = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-LONG-CODE-1", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-LONG-1");
            Long oldContractId = seedContract(longActiveCode1, empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31), "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empIdLong1 = (Long) idsLong[0];
        Long deptIdLong1 = (Long) idsLong[1];
        String expectedLongPrefix = "TRF-" + empIdLong1 + "-" + effective.toString();

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empIdLong1, deptIdLong1, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES, CTX));

        ErpHrEmploymentContract newContractLong1 = findActiveContract(empIdLong1);
        assertNotNull(newContractLong1, "(b) 长码端到端：新 ACTIVE 合同应建立（未触发 22001）");
        assertNotNull(newContractLong1.getCode(), "(b) 新合同 code 应非空");
        assertTrue(newContractLong1.getCode().length() <= 50,
                "(b) 新合同 code 必须 ≤ code precision 50，实际=" + newContractLong1.getCode().length()
                        + " code=" + newContractLong1.getCode());
        assertTrue(newContractLong1.getCode().startsWith(expectedLongPrefix),
                "(b) 新合同 code 必须保留固定段 TRF-{empId}-{effectiveDate}：expected prefix="
                        + expectedLongPrefix + " actual=" + newContractLong1.getCode());

        // ===== (a) 短 active.code 原路径逐字符不变 =====
        String shortActiveCode = "CTC-SHORT-001"; // 13 chars，拼接结果 = 4 + len(empId) + 1 + 10 + 1 + 13 = 29 + len(empId) ≤ 50
        Object[] idsShort = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-SHORT-CODE", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-SHORT");
            Long oldContractId = seedContract(shortActiveCode, empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31), "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empIdShort = (Long) idsShort[0];
        Long deptIdShort = (Long) idsShort[1];
        String expectedShortCode = "TRF-" + empIdShort + "-" + effective.toString() + "-" + shortActiveCode;
        assertTrue(expectedShortCode.length() <= 50,
                "(a) 前置：短码拼接结果 ≤ 50，实际=" + expectedShortCode.length());

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empIdShort, deptIdShort, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES, CTX));

        ErpHrEmploymentContract newContractShort = findActiveContract(empIdShort);
        assertNotNull(newContractShort, "(a) 短码：新合同应建立");
        assertEquals(expectedShortCode, newContractShort.getCode(),
                "(a) 短码路径逐字符不变：拼接 = TRF-{empId}-{effectiveDate}-{activeCode}");

        // ===== (c) 两条不同长 active.code 经摘要产生不同 successor code =====
        // 注意：不同员工 empId 不同 → 固定段已不同 → successor code 必然不同。本用例锁定
        // 「不同输入不退化为同一 code」基本不变量；MD5 摘要保证同 empId 下不同 active.code 也不冲突（哈希唯一性）。
        Object[] idsLong2 = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-LONG-CODE-2", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-LONG-2");
            Long oldContractId = seedContract(longActiveCode2, empId,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2027, 12, 31), "FIXED_TERM");
            return new Object[]{empId, deptId, oldContractId};
        });
        Long empIdLong2 = (Long) idsLong2[0];
        Long deptIdLong2 = (Long) idsLong2[1];

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empIdLong2, deptIdLong2, null, null, effective,
                ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES, CTX));

        ErpHrEmploymentContract newContractLong2 = findActiveContract(empIdLong2);
        assertNotNull(newContractLong2, "(c) 第二条长码：新合同应建立");
        assertTrue(newContractLong2.getCode().length() <= 50,
                "(c) 第二条长码 code ≤ 50，实际=" + newContractLong2.getCode().length());
        assertNotEquals(newContractLong1.getCode(), newContractLong2.getCode(),
                "(c) 两条不同长 active.code 经 buildSuccessorCode 应产生不同 successor code");

        // ===== (d) 无源码路径走 base（active=null）：拼接 = TRF-{empId}-{effectiveDate}（无 -active.code 后缀） =====
        Object[] idsNoActive = ormTemplate.runInSession(session -> {
            Long empId = seedEmployee("EMP-NO-ACTIVE", ErpHrConstants.EMPLOYMENT_ACTIVE);
            Long deptId = seedDepartment("DEPT-NO-ACTIVE");
            return new Object[]{empId, deptId};
        });
        Long empIdNoActive = (Long) idsNoActive[0];
        Long deptIdNoActive = (Long) idsNoActive[1];
        assertNull(findActiveContract(empIdNoActive), "(d) 前置：员工无 ACTIVE 合同");
        String expectedBaseCode = "TRF-" + empIdNoActive + "-" + effective.toString();

        ormTemplate.runInSession(() -> employeeBiz.transferEmployee(empIdNoActive, deptIdNoActive, null, null,
                effective, ErpHrConstants.TRANSFER_HANDLE_CONTRACT_YES, CTX));

        ErpHrEmploymentContract newContractNoActive = findActiveContract(empIdNoActive);
        assertNotNull(newContractNoActive, "(d) 无 ACTIVE 合同：YES 模式仍应新建");
        assertEquals(expectedBaseCode, newContractNoActive.getCode(),
                "(d) active=null 走 base 路径：拼接 = TRF-{empId}-{effectiveDate}（无 -active.code 后缀）");
    }

    // ---------- helpers ----------

    private ErpHrEmploymentContract findActiveContract(Long empId) {
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", empId),
                eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE)));
        q.setLimit(1);
        java.util.List<ErpHrEmploymentContract> list =
                daoProvider.daoFor(ErpHrEmploymentContract.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedEmployee(String code, String employmentStatus) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName(code);
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(employmentStatus);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedDepartment(String code) {
        IEntityDao<ErpHrDepartment> dao = daoProvider.daoFor(ErpHrDepartment.class);
        ErpHrDepartment d = new ErpHrDepartment();
        d.setCode(code);
        d.setName(code);
        dao.saveEntity(d);
        return d.getId();
    }

    private Long seedPosition(String code, Long departmentId) {
        IEntityDao<ErpHrPosition> dao = daoProvider.daoFor(ErpHrPosition.class);
        ErpHrPosition p = new ErpHrPosition();
        p.setCode(code);
        p.setName(code);
        p.setDepartmentId(departmentId);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedContract(String code, Long employeeId, LocalDate startDate, LocalDate endDate,
                              String contractType) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setCode(code);
        c.setEmployeeId(employeeId);
        c.setContractType(contractType);
        c.setSignDate(startDate);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setStatus(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        dao.saveEntity(c);
        return c.getId();
    }

    private Long seedApprovedLeave(Long employeeId, LocalDate startDate, LocalDate endDate) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        ErpHrLeaveRequest l = new ErpHrLeaveRequest();
        l.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        l.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        l.setCode("LV-" + employeeId + "-" + startDate.toString());
        l.setEmployeeId(employeeId);
        l.setLeaveType("ANNUAL");
        l.setStartDate(startDate);
        l.setEndDate(endDate);
        l.setStatus(ErpHrConstants.LEAVE_STATUS_APPROVED);
        dao.saveEntity(l);
        return l.getId();
    }
}
