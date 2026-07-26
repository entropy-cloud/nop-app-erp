import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  GraphQLClient,
  deleteById,
} from './_helper';

/**
 * Master-data ErpParty 统一 Party 身份查询 3 个 `@BizQuery` 浏览器层 E2E
 * （plan 2026-07-26-1500-2）。
 *
 * 验证 C1 `ErpPartyBizModel` 3 个 @BizQuery 方法经 GraphQL `/graphql` 全栈可达 + 字段投影 +
 * 过滤/截断行为（镜像 `party-search-picker.visual.spec.ts` wiring 但覆盖其未覆盖的字段值/过滤/截断）：
 *   (1) findParties keyword 命中——返回非空 List（≥3，跨 PARTNER/EMPLOYEE/ORGANIZATION 三类型 setup 实体）
 *       + 逐条断言 PartyRef 字段（partyType + code + name）匹配自包含 setup 实体
 *   (2) findParties partyTypes 过滤——partyTypes:[EMPLOYEE] 仅返回 EMPLOYEE setup 实体
 *       （PARTNER/ORGANIZATION setup 实体被过滤）
 *   (3) findParties keyword < 2 字符——返回空 List（MIN_KEYWORD_LENGTH 守卫，避免全表 LIKE）
 *   (4) findParties limit 截断——limit:2 三 setup 实体命中但截断为 2
 *   (5) getParty 三类型 + Organization 空字段容忍——PARTNER/EMPLOYEE code/name 断言；
 *       ORGANIZATION phone/email=null 容忍（实体无对应列）
 *   (6) findReferences——Map 结构可达（经 GraphQL 序列化为 **JSON 对象**形态——实测 schema 漂移：
 *       原 `[{k,v}]` 数组描述已过时，Map 类型不支持 selection set，原 `party-search-picker.visual.spec.ts:88`
 *       `{ k v }` selection 现触发预存错误属 schema 漂移非本 spec 引入）；断言结构非 null + object 类型可达，
 *       不硬断言具体计数值因 SPI 注册依赖运行时
 *
 * 权威设计（docs/design/master-data/unified-party-identity.md §查询策略 / §IErpPartyBiz 接口契约）：
 *   - findParties:keyword/limit/partyTypes 三参数，keyword < MIN_KEYWORD_LENGTH(=2) 返回空 List
 *   - DEFAULT_LIMIT=50 / MAX_LIMIT=200 / MIN_KEYWORD_LENGTH=2（硬编码 static final int）
 *   - Organization 实体无 phone/email 列 → PartyRef 投影为 null（容忍字段缺失）
 *
 * Explore Decision（Phase 1，落盘 plan Execution Decisions 段）：
 *   - **自包含 setup 默认**（对齐 1407-3 Decision 3）：经 `__save` 建 3 个自包含实体
 *     （partner + employee + organization），各自 code 唯一前缀（`E2E-PARTY-PN-`/`-EMP-`/`-ORG-` + ts），
 *     name 嵌入独占 keyword `E2E-PARTY-KEY-<ts>`（三实体共享同 keyword）。断言针对自包含实体
 *     字段投影值（非种子中文 name），避免未来种子编辑静默破坏字段投影断言无测试侧信号。
 *   - **GraphQL query 构造**：3 个 @BizQuery 返回复杂类型（List<PartyRef>/PartyRef/Map），
 *     `GraphQLClient.callQuery` 不带 selection set（仅适用标量返回）→ 经 `new GraphQLClient(page).raw()`
 *     内联完整 query + selection set（镜像 cs-canned-response/fin-reconciliation 范式）。
 *   - **findReferences Map 序列化**：经 GraphQL 序列化为 **JSON 对象**（实测 `{}` 或 `{"employeeAdvance":0}` 形态，
 *     非 selection set——Map 类型不支持字段选择）。原 `party-search-picker.visual.spec.ts:80-91` 描述的 `[{k,v}]` 数组
 *     形态已过时（visual spec 第 88 行 `{ k v }` selection 现触发 "[Map]不是对象类型，不支持字段选择" 错误，属预存 schema 漂移）。
 *     本 spec 不使用 selection set，断言结果为非 null 对象 + 结构可达，不硬断言具体计数值（SPI 注册依赖运行时）。
 *   - **Cleanup 策略**：自包含 setup 写入 partner/employee/org 三实体 → finally 块按 setup 实体 id 逐条
 *     __delete（reverse-dep：employee（含 partnerId 引用）→ partner；organization 无下游引用）。
 */

/**
 * 构造确定性独占 keyword（三实体共享，命中 findParties OR 过滤器 code/name LIKE）。
 * 独占 keyword 防止与种子中文 name 模糊匹配造成断言漂移。
 */
function uniq(tag: string): string {
  return `${tag}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

const PARTY_REF_SELECTION =
  'partyType partyId code name phone email status displayName';

interface PartyRefRow {
  partyType: string;
  partyId: string | number;
  code: string;
  name: string;
  phone: string | null;
  email: string | null;
  status: string;
  displayName: string;
}

test.describe('Master-data ErpParty unified identity query (@BizQuery) browser-layer E2E', () => {
  test('(1)-(6) findParties multi-scenario + getParty + findReferences', async ({ page }) => {
    await loginAndNavigate(page, '/md-dashboard-main');

    const ts = Date.now();
    const keyword = `E2E-PARTY-KEY-${ts}`;
    const partnerCode = `E2E-PARTY-PN-${ts}`;
    const employeeCode = `E2E-PARTY-EMP-${ts}`;
    const orgCode = `E2E-PARTY-ORG-${ts}`;
    const partnerName = `${keyword}-PARTNER`;
    const employeeName = `${keyword}-EMPLOYEE`;
    const orgName = `${keyword}-ORG`;

    const gql = new GraphQLClient(page);

    // ---- Setup: self-contained partner/employee/organization (独占 keyword name) ----
    const partner = await createViaSave(
      page,
      'ErpMdPartner',
      {
        code: partnerCode,
        name: partnerName,
        partnerType: 'CUSTOMER',
        status: 'ACTIVE',
        phone: '13800000099',
        email: `${keyword.toLowerCase()}-pn@e2e.example`,
      },
      'id',
    );
    const employee = await createViaSave(
      page,
      'ErpMdEmployee',
      {
        code: employeeCode,
        name: employeeName,
        orgId: 2,
        status: 'ACTIVE',
        phone: '13800000098',
        email: `${keyword.toLowerCase()}-emp@e2e.example`,
      },
      'id',
    );
    const organization = await createViaSave(
      page,
      'ErpMdOrganization',
      {
        code: orgCode,
        name: orgName,
        orgType: 'COMPANY',
        status: 'ACTIVE',
      },
      'id',
    );

    try {
      // ---- (1) findParties keyword 命中: ≥3 跨 PARTNER/EMPLOYEE/ORGANIZATION + 字段投影 ----
      const findPartiesJson: any = await gql.raw(
        `query{ ErpParty__findParties(keyword:${JSON.stringify(keyword)}){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        findPartiesJson?.errors,
        `findParties should not return GraphQL errors: ${JSON.stringify(findPartiesJson?.errors)}`,
      ).toBeFalsy();
      const findPartiesResult: PartyRefRow[] =
        findPartiesJson?.data?.ErpParty__findParties ?? [];
      expect(Array.isArray(findPartiesResult), 'findParties must return an array').toBe(true);
      expect(
        findPartiesResult.length,
        'findParties should return ≥3 (cross PARTNER/EMPLOYEE/ORGANIZATION setup entities)',
      ).toBeGreaterThanOrEqual(3);

      const byType = new Map<string, PartyRefRow>();
      for (const r of findPartiesResult) {
        byType.set(r.partyType, r);
      }
      // PARTNER setup 字段投影断言
      const partnerRef = byType.get('PARTNER');
      expect(partnerRef, 'findParties should return the setup PARTNER').toBeTruthy();
      expect(partnerRef!.code, 'PARTNER code projection').toBe(partnerCode);
      expect(partnerRef!.name, 'PARTNER name projection').toBe(partnerName);
      expect(partnerRef!.status, 'PARTNER status projection').toBe('ACTIVE');
      // EMPLOYEE setup 字段投影断言
      const employeeRef = byType.get('EMPLOYEE');
      expect(employeeRef, 'findParties should return the setup EMPLOYEE').toBeTruthy();
      expect(employeeRef!.code, 'EMPLOYEE code projection').toBe(employeeCode);
      expect(employeeRef!.name, 'EMPLOYEE name projection').toBe(employeeName);
      expect(employeeRef!.status, 'EMPLOYEE status projection').toBe('ACTIVE');
      // ORGANIZATION setup 字段投影断言（phone/email 容忍 null 在用例 (5) 验证）
      const orgRef = byType.get('ORGANIZATION');
      expect(orgRef, 'findParties should return the setup ORGANIZATION').toBeTruthy();
      expect(orgRef!.code, 'ORGANIZATION code projection').toBe(orgCode);
      expect(orgRef!.name, 'ORGANIZATION name projection').toBe(orgName);
      expect(orgRef!.status, 'ORGANIZATION status projection').toBe('ACTIVE');

      // ---- (2) findParties partyTypes 过滤: [EMPLOYEE] 仅返回 EMPLOYEE setup ----
      const filteredJson: any = await gql.raw(
        `query{ ErpParty__findParties(keyword:${JSON.stringify(keyword)},partyTypes:["EMPLOYEE"]){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        filteredJson?.errors,
        `findParties(partyTypes:[EMPLOYEE]) should not return GraphQL errors: ${JSON.stringify(filteredJson?.errors)}`,
      ).toBeFalsy();
      const filtered: PartyRefRow[] = filteredJson?.data?.ErpParty__findParties ?? [];
      expect(Array.isArray(filtered), 'filtered findParties must return an array').toBe(true);
      expect(
        filtered.length,
        'partyTypes:[EMPLOYEE] should return ≥1 (the setup EMPLOYEE)',
      ).toBeGreaterThanOrEqual(1);
      // 仅含 EMPLOYEE setup（PARTNER/ORGANIZATION setup 被过滤）
      const filteredTypes = new Set(filtered.map(r => r.partyType));
      expect(
        filteredTypes.has('PARTNER'),
        'partyTypes:[EMPLOYEE] must exclude PARTNER setup',
      ).toBe(false);
      expect(
        filteredTypes.has('ORGANIZATION'),
        'partyTypes:[EMPLOYEE] must exclude ORGANIZATION setup',
      ).toBe(false);
      expect(filteredTypes.has('EMPLOYEE'), 'partyTypes:[EMPLOYEE] must include EMPLOYEE').toBe(
        true,
      );
      // 字段投影针对自包含 EMPLOYEE 实体（非种子 EMP-001~003）
      const filteredEmp = filtered.find(r => r.code === employeeCode);
      expect(filteredEmp, 'filtered result should include the setup EMPLOYEE').toBeTruthy();

      // ---- (3) findParties keyword < 2 字符: 返回空 List（MIN_KEYWORD_LENGTH 守卫） ----
      const shortJson: any = await gql.raw(
        `query{ ErpParty__findParties(keyword:"a"){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        shortJson?.errors,
        `findParties(keyword:"a") should not return GraphQL errors: ${JSON.stringify(shortJson?.errors)}`,
      ).toBeFalsy();
      const shortResult: PartyRefRow[] = shortJson?.data?.ErpParty__findParties ?? [];
      expect(
        Array.isArray(shortResult),
        'findParties(keyword<2) must still return an array',
      ).toBe(true);
      expect(
        shortResult.length,
        'findParties(keyword<2 chars) must return empty List (MIN_KEYWORD_LENGTH guard)',
      ).toBe(0);

      // ---- (4) findParties limit 截断: limit:2 → List.size() ≤ 2 ----
      const limitedJson: any = await gql.raw(
        `query{ ErpParty__findParties(keyword:${JSON.stringify(keyword)},limit:2){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        limitedJson?.errors,
        `findParties(limit:2) should not return GraphQL errors: ${JSON.stringify(limitedJson?.errors)}`,
      ).toBeFalsy();
      const limited: PartyRefRow[] = limitedJson?.data?.ErpParty__findParties ?? [];
      expect(Array.isArray(limited), 'findParties(limit:2) must return an array').toBe(true);
      expect(
        limited.length,
        'findParties(limit:2) must truncate to ≤2 (3 setup entities hit, truncate observable)',
      ).toBeLessThanOrEqual(2);

      // ---- (5) getParty 三类型 + Organization 空字段容忍 ----
      const partnerGetJson: any = await gql.raw(
        `query{ ErpParty__getParty(partyType:"PARTNER",partyId:${Number(partner.id)}){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        partnerGetJson?.errors,
        `getParty(PARTNER) should not return GraphQL errors: ${JSON.stringify(partnerGetJson?.errors)}`,
      ).toBeFalsy();
      const partnerGet: PartyRefRow | null =
        partnerGetJson?.data?.ErpParty__getParty ?? null;
      expect(partnerGet, 'getParty(PARTNER) should return the setup PARTNER').toBeTruthy();
      expect(partnerGet!.code, 'getParty(PARTNER) code').toBe(partnerCode);
      expect(partnerGet!.name, 'getParty(PARTNER) name').toBe(partnerName);

      const employeeGetJson: any = await gql.raw(
        `query{ ErpParty__getParty(partyType:"EMPLOYEE",partyId:${Number(employee.id)}){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        employeeGetJson?.errors,
        `getParty(EMPLOYEE) should not return GraphQL errors: ${JSON.stringify(employeeGetJson?.errors)}`,
      ).toBeFalsy();
      const employeeGet: PartyRefRow | null =
        employeeGetJson?.data?.ErpParty__getParty ?? null;
      expect(employeeGet, 'getParty(EMPLOYEE) should return the setup EMPLOYEE').toBeTruthy();
      expect(employeeGet!.code, 'getParty(EMPLOYEE) code').toBe(employeeCode);
      expect(employeeGet!.name, 'getParty(EMPLOYEE) name').toBe(employeeName);

      const orgGetJson: any = await gql.raw(
        `query{ ErpParty__getParty(partyType:"ORGANIZATION",partyId:${Number(organization.id)}){ ${PARTY_REF_SELECTION} } }`,
      );
      expect(
        orgGetJson?.errors,
        `getParty(ORGANIZATION) should not return GraphQL errors: ${JSON.stringify(orgGetJson?.errors)}`,
      ).toBeFalsy();
      const orgGet: PartyRefRow | null = orgGetJson?.data?.ErpParty__getParty ?? null;
      expect(orgGet, 'getParty(ORGANIZATION) should return the setup ORGANIZATION').toBeTruthy();
      expect(orgGet!.code, 'getParty(ORGANIZATION) code').toBe(orgCode);
      expect(orgGet!.name, 'getParty(ORGANIZATION) name').toBe(orgName);
      // Organization 无 phone/email 列 → 投影为 null（容忍字段缺失）
      expect(orgGet!.phone, 'getParty(ORGANIZATION) phone must be null (no entity column)').toBeNull();
      expect(orgGet!.email, 'getParty(ORGANIZATION) email must be null (no entity column)').toBeNull();

      // ---- (6) findReferences: Map 结构可达（JSON object 形态），不硬断言计数 ----
      // GraphQL Map 序列化为 JSON 对象（实测 EMPLOYEE id=1 → {"employeeAdvance":0}，
      // PARTNER setup 新建 → {}），不支持 selection set。断言结果为非 null 对象。
      const refsJson: any = await gql.raw(
        `query{ ErpParty__findReferences(partyType:"PARTNER",partyId:${Number(partner.id)}) }`,
      );
      expect(
        refsJson?.errors,
        `findReferences should not return GraphQL errors: ${JSON.stringify(refsJson?.errors)}`,
      ).toBeFalsy();
      const refs = refsJson?.data?.ErpParty__findReferences;
      // Map 经 GraphQL 序列化为 JSON 对象（非数组、非 null）——SPI 计数值依赖运行时，
      // setup 实体为新创建未被下游引用 → 空 Map（`{}`）或 SPI 计数为 0 均符合既定容忍行为。
      // 不硬断言具体 key/value，仅断言结构可达（object + 非 null）。
      expect(refs, 'findReferences must return a non-null Map (JSON object form)').not.toBeNull();
      expect(
        typeof refs === 'object' && !Array.isArray(refs),
        'findReferences must return a Map serialized as JSON object (not array)',
      ).toBe(true);
    } finally {
      // ---- Cleanup (reverse dependency): employee → partner (employee.partnerId ref); org standalone ----
      // ErpMdEmployee 含 partnerId 引用，删 partner 前须先删 employee（FK 约束）
      await deleteById(page, 'ErpMdEmployee', employee.id);
      await deleteById(page, 'ErpMdPartner', partner.id);
      await deleteById(page, 'ErpMdOrganization', organization.id);
    }
  });
});
