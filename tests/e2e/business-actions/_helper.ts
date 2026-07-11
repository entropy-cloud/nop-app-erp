import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

/**
 * 业务动作浏览器层 E2E helper（plan 2026-07-09-0814-2）。
 *
 * 三个原语覆盖自定义 @BizMutation 经 GraphQL /graphql 的全栈可达性：
 *   - createViaSave：经标准 CrudBizModel __save 建前置实体（同 write.spec.ts 范式）
 *   - callMutation：经 GraphQL mutation 调自定义 @BizMutation 动作（状态机/过账型）
 *   - verifyState：经 GraphQL query __get 断言状态字段翻转
 *
 * 范式与 dashboards/_helper.ts（getDashboardKpi 经 GraphQL）、crud/_helper.ts（__save/__get）
 * 一致：loginAndNavigate 建立 nop-token 会话后 page.request.post('/graphql') 直调。
 */

export { test, expect, loginAndNavigate };

/**
 * 标记一个 GraphQL input 对象参数（自定义动作的复杂入参，如 generateMove 的 StockMoveRequest）。
 * 标量参数（Long/String/Boolean）直接以 JS 原值传入，由 callMutation 内联为 GraphQL 字面量。
 */
export interface InputArg {
  __input: true;
  type: string;
  value: Record<string, unknown>;
}

export function input(type: string, value: Record<string, unknown>): InputArg {
  return { __input: true, type, value };
}

async function extract(resp: { json: () => Promise<unknown> }, actionKey: string): Promise<{ data: any | null; errors: any[] | null; json: any }> {
  const json: any = await resp.json();
  return { data: json?.data?.[actionKey] ?? null, errors: json?.errors ?? null, json };
}

/**
 * 原语 1：经标准 __save 建前置实体。返回 selection 指定的字段（至少含 id）。
 */
export async function createViaSave(
  page: Page,
  entityName: string,
  data: Record<string, unknown>,
  selection = 'id',
): Promise<any> {
  const inputType = `${entityName}__save_input`;
  const resp = await page.request.post('/graphql', {
    data: {
      query: `mutation($d:${inputType}){ ${entityName}__save(data:$d){ ${selection} } }`,
      variables: { d: data },
    },
  });
  const { data: saved, errors } = await extract(resp, `${entityName}__save`);
  expect(errors, `${entityName}__save should not return GraphQL errors`).toBeNull();
  expect(saved, `${entityName}__save should return saved entity`).toBeTruthy();
  return saved;
}

/**
 * 原语 2：经 GraphQL mutation 调自定义 @BizMutation 动作。
 *
 * args 中：标量值（number/string/boolean）内联为 GraphQL 字面量；input() 包装的值为 input 对象参数（经 variable + 显式类型）。
 * 返回动作结果实体的 selection 字段。失败（业务异常）时 data 为 null 且 errors 含 ErrorCode——调用方可据 errors 断言非法迁移。
 */
export async function callMutation(
  page: Page,
  entityName: string,
  action: string,
  args: Record<string, unknown>,
  selection = 'id',
): Promise<{ data: any | null; errors: any[] | null; json: any }> {
  const parts: string[] = [];
  const varDecls: string[] = [];
  const vars: Record<string, unknown> = {};
  for (const [name, arg] of Object.entries(args)) {
    if (arg && typeof arg === 'object' && (arg as InputArg).__input) {
      const ia = arg as InputArg;
      varDecls.push(`$${name}:${ia.type}`);
      vars[name] = ia.value;
      parts.push(`${name}:$${name}`);
    } else {
      parts.push(`${name}:${JSON.stringify(arg)}`);
    }
  }
  const query = varDecls.length
    ? `mutation(${varDecls.join(',')}){ ${entityName}__${action}(${parts.join(',')}){ ${selection} } }`
    : `mutation{ ${entityName}__${action}(${parts.join(',')}){ ${selection} } }`;
  const payload = varDecls.length ? { query, variables: vars } : { query };
  const resp = await page.request.post('/graphql', { data: payload });
  return extract(resp, `${entityName}__${action}`);
}

/**
 * 断言 mutation 成功（data 非空、无 errors）并返回结果实体。
 */
export async function callMutationOk(
  page: Page,
  entityName: string,
  action: string,
  args: Record<string, unknown>,
  selection = 'id',
): Promise<any> {
  const { data, errors } = await callMutation(page, entityName, action, args, selection);
  expect(errors, `${entityName}__${action} should not return GraphQL errors`).toBeNull();
  expect(data, `${entityName}__${action} should succeed`).toBeTruthy();
  return data;
}

/**
 * 原语 3：经 __get 断言状态字段。返回 selection 指定的字段。
 */
export async function verifyState(
  page: Page,
  entityName: string,
  id: string | number,
  selection: string,
): Promise<any> {
  const resp = await page.request.post('/graphql', {
    data: { query: `{ ${entityName}__get(id:${id}){ ${selection} } }` },
  });
  const { data, errors } = await extract(resp, `${entityName}__get`);
  expect(errors, `${entityName}__get should not return GraphQL errors`).toBeNull();
  return data;
}

/**
 * 原语 4：经 GraphQL query 调自定义 @BizQuery 动作（非实体 CRUD，如 suggestForTicket/renderTemplate）。
 *
 * args 中：标量值内联为 GraphQL 字面量；input() 包装的值为 input 对象参数（经 variable + 显式类型）。
 * 返回 query 结果（可能是标量/列表/对象）。失败时 data 为 null 且 errors 含 ErrorCode。
 */
export async function callQuery(
  page: Page,
  entityName: string,
  action: string,
  args: Record<string, unknown>,
): Promise<{ data: any | null; errors: any[] | null; json: any }> {
  const parts: string[] = [];
  const varDecls: string[] = [];
  const vars: Record<string, unknown> = {};
  for (const [name, arg] of Object.entries(args)) {
    if (arg && typeof arg === 'object' && (arg as InputArg).__input) {
      const ia = arg as InputArg;
      varDecls.push(`$${name}:${ia.type}`);
      vars[name] = ia.value;
      parts.push(`${name}:$${name}`);
    } else {
      parts.push(`${name}:${JSON.stringify(arg)}`);
    }
  }
  const query = varDecls.length
    ? `query(${varDecls.join(',')}){ ${entityName}__${action}(${parts.join(',')}) }`
    : `query{ ${entityName}__${action}(${parts.join(',')}) }`;
  const payload = varDecls.length ? { query, variables: vars } : { query };
  const resp = await page.request.post('/graphql', { data: payload });
  const json: any = await resp.json();
  const actionKey = `${entityName}__${action}`;
  const data = json?.data?.[actionKey] ?? null;
  const errors = json?.errors ?? null;
  return { data, errors, json };
}

/**
 * 构建 Nop FieldTreeBean 等值过滤（TreeBean Map 格式：`$type`=op, `name`/`value`=属性）。
 * Nop filter 不接受 plain-map 字段相等（报 op-is-null）；必须用 `$type:'eq'` 显式声明算子。
 */
export function eqFilter(field: string, value: unknown): Record<string, unknown> {
  return { $type: 'eq', name: field, value };
}

/**
 * 构建 Nop FieldTreeBean AND 组过滤（多条件与）。
 */
export function andFilter(...leaves: Record<string, unknown>[]): Record<string, unknown> {
  return { $type: 'and', $body: leaves };
}

/**
 * 经 __findPage 断言下游产物（过账流水/余额等）存在性：total > 0 即存在。
 * filter 经 GraphQL variable（Map 类型）传入——GraphQL 不支持带引号 key 的内联对象字面量，
 * 故 filter 必须走 variable（与 dashboards value helper 的变量范式一致）。
 */
export async function findPageTotal(
  page: Page,
  entityName: string,
  filter: Record<string, unknown>,
): Promise<number> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:1,filter:$f}){ total } }`,
      variables: { f: filter },
    },
  });
  const { data } = await extract(resp, `${entityName}__findPage`);
  return data?.total ?? 0;
}

/**
 * 经 __findPage 返回首条匹配实体（带 selection），无匹配返回 null。
 * 用于副作用编排断言（如 Request accept 生成的响应式 Visit 字段核实）。filter 走 GraphQL variable。
 */
export async function findFirst<T = any>(
  page: Page,
  entityName: string,
  filter: Record<string, unknown>,
  selection: string,
): Promise<T | null> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:1,filter:$f}){ items{ ${selection} } } }`,
      variables: { f: filter },
    },
  });
  const { data } = await extract(resp, `${entityName}__findPage`);
  const items: any[] = data?.items || [];
  return items.length > 0 ? (items[0] as T) : null;
}

/**
 * 清理（逻辑删除）匹配过滤的所有实体：findPage 全量后逐条 __delete。
 *
 * 业务动作 E2E 会创建不可逆下游产物（库存流水/余额、工单审计/调查、线索转化日志），
 * 全栈共享同一 Quarkus+H2 实例（reuseExistingServer），不清理会污染下游数值断言测试
 * （dashboard KPI / report render 假设种子基线）。每个 spec 在断言完成后必须清理自身产物。
 */
export async function deleteByFilter(
  page: Page,
  entityName: string,
  filter: Record<string, unknown>,
): Promise<number> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:500,filter:$f}){ items{ id } } }`,
      variables: { f: filter },
    },
  });
  const { data } = await extract(resp, `${entityName}__findPage`);
  const items: any[] = data?.items || [];
  for (const item of items) {
    await page.request.post('/graphql', {
      data: { query: `mutation{ ${entityName}__delete(id:${item.id}) }` },
    });
  }
  return items.length;
}

/**
 * 删除单个实体（经 __delete）。用于清理测试创建的主实体。
 */
export async function deleteById(page: Page, entityName: string, id: string | number): Promise<void> {
  await page.request.post('/graphql', {
    data: { query: `mutation{ ${entityName}__delete(id:${id}) }` },
  });
}
