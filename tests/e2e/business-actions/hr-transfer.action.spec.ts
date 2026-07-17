import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  findFirst,
  findPageTotal,
  deleteByFilter,
  deleteById,
} from './_helper';

/**
 * hr ErpHrEmployee.transferEmployee 调动 + 合同联动浏览器层 E2E（plan 2026-07-18-0100-2 Phase 2）。
 *
 * 验证员工调动 DIRECT `@BizMutation`（无 useApproval/useWorkflow）经 GraphQL /graphql 的全栈可达性 +
 * handleContract 三分支合同联动副作用 + 三类守卫：
 *   - handleContract=YES：员工 dept/position/superior 翻转 + 原 ACTIVE 合同 TERMINATED + 新建 ACTIVE 合同
 *   - handleContract=NO：员工 dept/position/superior 翻转 + 不触及合同（原 ACTIVE 不变 + 无新建）
 *   - handleContract=AUTO（默认）：依赖 config `erp-hr.transfer-auto-handle-contract=true`（默认 true）→ 等效 YES
 *   - 守卫：RESIGNED 员工 → ERR_EMPLOYEE_NOT_TRANSFERABLE；目标 position 不属目标 dept →
 *     ERR_TRANSFER_TARGET_POSITION_NOT_FOUND
 *
 * 权威设计（use-cases.md UC-HR-08 + competency-management.md §调动）：单步直接更新调动（无状态机），
 * `ErpHrEmployeeBizModel.transferEmployee` 硬编码 `CONTRACT_STATUS_TERMINATED`（无 INACTIVE 分支）。
 *
 * 自包含 setup：建测试专用 employee(ACTIVE) + active 合同（源 dept/position）+ 目标 dept + 目标 position
 * （属目标 dept），避免污染种子员工/合同基线。清理：删新建合同 + 员工 + position + dept + 合同源。
 */

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface TransferSetup {
  employeeId: string;
  sourceContractId: string;
  sourceDeptId: string;
  sourcePositionId: string;
  targetDeptId: string;
  targetPositionId: string;
}

async function setupTransferChain(
  page: import('@playwright/test').Page,
  tag: string,
  employmentStatus = 'ACTIVE',
): Promise<TransferSetup> {
  const sourceDept = await createViaSave(
    page,
    'ErpHrDepartment',
    { code: uniq(`E2E-FROM-DEPT-${tag}`), name: `from-dept-${tag}` },
    'id',
  );
  const targetDept = await createViaSave(
    page,
    'ErpHrDepartment',
    { code: uniq(`E2E-TO-DEPT-${tag}`), name: `to-dept-${tag}` },
    'id',
  );
  const sourcePos = await createViaSave(
    page,
    'ErpHrPosition',
    {
      code: uniq(`E2E-FROM-POS-${tag}`),
      name: `from-pos-${tag}`,
      departmentId: Number(sourceDept.id),
    },
    'id',
  );
  const targetPos = await createViaSave(
    page,
    'ErpHrPosition',
    {
      code: uniq(`E2E-TO-POS-${tag}`),
      name: `to-pos-${tag}`,
      departmentId: Number(targetDept.id),
    },
    'id',
  );
  const emp = await createViaSave(
    page,
    'ErpHrEmployee',
    {
      code: uniq(`E2E-TRF-EMP-${tag}`),
      firstName: '调',
      lastName: tag,
      fullName: `调动${tag}`,
      gender: 'MALE',
      hireDate: '2023-01-01',
      employmentStatus,
      employeeType: 'FULL_TIME',
      departmentId: Number(sourceDept.id),
      positionId: Number(sourcePos.id),
      orgId: 2,
    },
    'id',
  );
  // 源 ACTIVE 合同（仅在 employmentStatus=ACTIVE 时建，便于守卫测试跳过）
  // 注：contract code 用紧凑短码——transferEmployee 的 buildSuccessorCode 会在新合同码前缀
  // "TRF-{empId}-{effectiveDate}-" + active.code（共 22 字符 + active.code），而 code 列 precision=50
  // （domain="code"），过长的源码会触发 sqlState=22001 字符截断（同 1430-1 类 buildCode overflow 缺陷）。
  let sourceContractId = '';
  if (employmentStatus === 'ACTIVE') {
    const contract = await createViaSave(
      page,
      'ErpHrEmploymentContract',
      {
        code: uniq(`C${tag}`), // 短码：C{tag}-{ms}-{seq} ≈ 20 字符 → TRF 包装 ≈ 42 字符 < 50
        employeeId: Number(emp.id),
        contractType: 'FIXED_TERM',
        signDate: '2024-01-01',
        startDate: '2024-01-01',
        endDate: '2027-12-31',
        status: 'ACTIVE',
        businessDate: '2024-01-01',
        orgId: 2,
      },
      'id',
    );
    sourceContractId = contract.id;
  }

  return {
    employeeId: emp.id,
    sourceContractId,
    sourceDeptId: sourceDept.id,
    sourcePositionId: sourcePos.id,
    targetDeptId: targetDept.id,
    targetPositionId: targetPos.id,
  };
}

async function cleanupTransfer(
  page: import('@playwright/test').Page,
  s: TransferSetup,
): Promise<void> {
  // 删调动后所有合同（含源 TERMINATED + 新建 ACTIVE）—— 按 employeeId 反查
  await deleteByFilter(page, 'ErpHrEmploymentContract', eqFilter('employeeId', Number(s.employeeId)));
  await deleteById(page, 'ErpHrEmployee', s.employeeId);
  await deleteById(page, 'ErpHrPosition', s.sourcePositionId);
  await deleteById(page, 'ErpHrPosition', s.targetPositionId);
  await deleteById(page, 'ErpHrDepartment', s.sourceDeptId);
  await deleteById(page, 'ErpHrDepartment', s.targetDeptId);
}

test.describe('hr ErpHrEmployee transferEmployee DIRECT action + contract linkage', () => {
  test('handleContract=YES: emp dept/position flip + source contract TERMINATED + new ACTIVE created', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrEmployee-main');

    const s = await setupTransferChain(page, 'yes');
    const effectiveDate = '2026-08-01';

    await callMutationOk(
      page,
      'ErpHrEmployee',
      'transferEmployee',
      {
        employeeId: Number(s.employeeId),
        targetDepartmentId: Number(s.targetDeptId),
        targetPositionId: Number(s.targetPositionId),
        targetSuperiorId: 1, // 种子 HR-EMP-001（superiorId 字段 GraphQL 非空，需真实值）
        effectiveDate,
        handleContract: 'YES',
      },
      'id departmentId positionId',
    );

    // 经 verifyState __get 独立断言员工字段翻转
    const emp = await verifyState(
      page,
      'ErpHrEmployee',
      s.employeeId,
      'departmentId positionId',
    );
    expect(Number(emp.departmentId), 'emp departmentId flipped to target').toBe(Number(s.targetDeptId));
    expect(Number(emp.positionId), 'emp positionId flipped to target').toBe(Number(s.targetPositionId));

    // 源合同 status → TERMINATED（ErpHrEmployeeBizModel 硬编码 CONTRACT_STATUS_TERMINATED）
    const oldContract = await verifyState(
      page,
      'ErpHrEmploymentContract',
      s.sourceContractId,
      'status',
    );
    expect(oldContract.status, 'source contract TERMINATED after YES transfer').toBe('TERMINATED');

    // 新 ACTIVE 合同经 findFirst 按 employeeId + status=ACTIVE 反查
    const newContract = await findFirst(
      page,
      'ErpHrEmploymentContract',
      andFilter(
        eqFilter('employeeId', Number(s.employeeId)),
        eqFilter('status', 'ACTIVE'),
      ),
      'id status signDate startDate',
    );
    expect(newContract, 'new ACTIVE contract created').not.toBeNull();
    expect((newContract as any).status, 'new contract status=ACTIVE').toBe('ACTIVE');
    expect(Number((newContract as any).id), 'new contract id != source').not.toBe(Number(s.sourceContractId));
    expect((newContract as any).signDate, 'new contract signDate=effectiveDate').toBe(effectiveDate);

    await cleanupTransfer(page, s);
  });

  test('handleContract=NO: emp dept/position flip + no contract side-effect (source ACTIVE unchanged, no new)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrEmployee-main');

    const s = await setupTransferChain(page, 'no');
    const effectiveDate = '2026-08-02';

    // 调用前：1 ACTIVE 合同
    const beforeCount = await findPageTotal(
      page,
      'ErpHrEmploymentContract',
      eqFilter('employeeId', Number(s.employeeId)),
    );
    expect(beforeCount, 'precondition: 1 source contract').toBe(1);

    await callMutationOk(
      page,
      'ErpHrEmployee',
      'transferEmployee',
      {
        employeeId: Number(s.employeeId),
        targetDepartmentId: Number(s.targetDeptId),
        targetPositionId: Number(s.targetPositionId),
        targetSuperiorId: 1, // 种子 HR-EMP-001（superiorId 字段 GraphQL 非空，需真实值）
        effectiveDate,
        handleContract: 'NO',
      },
      'id',
    );

    const emp = await verifyState(
      page,
      'ErpHrEmployee',
      s.employeeId,
      'departmentId positionId',
    );
    expect(Number(emp.departmentId), 'emp departmentId flipped').toBe(Number(s.targetDeptId));
    expect(Number(emp.positionId), 'emp positionId flipped').toBe(Number(s.targetPositionId));

    // 源合同 status 仍 ACTIVE
    const oldContract = await verifyState(
      page,
      'ErpHrEmploymentContract',
      s.sourceContractId,
      'status',
    );
    expect(oldContract.status, 'source contract still ACTIVE under NO').toBe('ACTIVE');

    // 无新增合同（计数仍 1）
    const afterCount = await findPageTotal(
      page,
      'ErpHrEmploymentContract',
      eqFilter('employeeId', Number(s.employeeId)),
    );
    expect(afterCount, 'no new contract created under NO').toBe(1);

    await cleanupTransfer(page, s);
  });

  test('handleContract=AUTO (config-gated default true): equivalent to YES', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrEmployee-main');

    const s = await setupTransferChain(page, 'auto');
    const effectiveDate = '2026-08-03';

    // 显式传 handleContract=AUTO（GraphQL schema 标非空，但 BizModel.normalizeHandleContract 把
    // 任意 null/空/未知值视作 AUTO），由 config `erp-hr.transfer-auto-handle-contract=true` 默认值决定行为
    await callMutationOk(
      page,
      'ErpHrEmployee',
      'transferEmployee',
      {
        employeeId: Number(s.employeeId),
        targetDepartmentId: Number(s.targetDeptId),
        targetPositionId: Number(s.targetPositionId),
        targetSuperiorId: 1, // 种子 HR-EMP-001（superiorId 字段 GraphQL 非空，需真实值）
        effectiveDate,
        handleContract: 'AUTO',
      },
      'id',
    );

    const emp = await verifyState(
      page,
      'ErpHrEmployee',
      s.employeeId,
      'departmentId positionId',
    );
    expect(Number(emp.departmentId), 'emp departmentId flipped under AUTO').toBe(Number(s.targetDeptId));

    // AUTO 默认 = YES：源合同 TERMINATED + 新 ACTIVE 合同
    const oldContract = await verifyState(
      page,
      'ErpHrEmploymentContract',
      s.sourceContractId,
      'status',
    );
    expect(oldContract.status, 'source contract TERMINATED under AUTO (config-gated default true)').toBe('TERMINATED');

    const newContract = await findFirst(
      page,
      'ErpHrEmploymentContract',
      andFilter(
        eqFilter('employeeId', Number(s.employeeId)),
        eqFilter('status', 'ACTIVE'),
      ),
      'id',
    );
    expect(newContract, 'new ACTIVE contract created under AUTO default').not.toBeNull();

    await cleanupTransfer(page, s);
  });

  test('guards: RESIGNED employee rejected + position-not-in-target-dept rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrEmployee-main');

    // (a) RESIGNED 员工 → ERR_EMPLOYEE_NOT_TRANSFERABLE
    const sResigned = await setupTransferChain(page, 'gd-r', 'RESIGNED');
    const rej1 = await callMutation(
      page,
      'ErpHrEmployee',
      'transferEmployee',
      {
        employeeId: Number(sResigned.employeeId),
        targetDepartmentId: Number(sResigned.targetDeptId),
        targetPositionId: Number(sResigned.targetPositionId),
        targetSuperiorId: 1, // 种子 HR-EMP-001（superiorId 字段 GraphQL 非空，需真实值）
        effectiveDate: '2026-08-04',
        handleContract: 'NO',
      },
      'id',
    );
    expect(rej1.errors, 'transferEmployee on RESIGNED employee should be rejected').toBeTruthy();
    expect(JSON.stringify(rej1.errors), 'reject should carry not-transferable token').toContain('不可调动');

    // 员工字段不变（仍源 dept）
    const emp1 = await verifyState(
      page,
      'ErpHrEmployee',
      sResigned.employeeId,
      'departmentId',
    );
    expect(Number(emp1.departmentId), 'dept unchanged after RESIGNED guard reject').toBe(
      Number(sResigned.sourceDeptId),
    );

    // (b) 目标 position 不属目标 dept → ERR_TRANSFER_TARGET_POSITION_NOT_FOUND
    const sMismatch = await setupTransferChain(page, 'gd-m');
    // 用 sMismatch.sourcePositionId（属 source dept）作 target position，target dept 仍为 targetDept → 不属
    const rej2 = await callMutation(
      page,
      'ErpHrEmployee',
      'transferEmployee',
      {
        employeeId: Number(sMismatch.employeeId),
        targetDepartmentId: Number(sMismatch.targetDeptId),
        targetPositionId: Number(sMismatch.sourcePositionId), // 属 source dept，不属 target dept
        targetSuperiorId: 1, // 种子 HR-EMP-001（superiorId 字段 GraphQL 非空，需真实值）
        effectiveDate: '2026-08-05',
        handleContract: 'NO',
      },
      'id',
    );
    expect(rej2.errors, 'transferEmployee with position not in target dept should be rejected').toBeTruthy();
    expect(JSON.stringify(rej2.errors), 'reject should carry position-not-found token').toContain('目标职位');

    await cleanupTransfer(page, sResigned);
    await cleanupTransfer(page, sMismatch);
  });
});
