import type { Page } from '@playwright/test';

/**
 * 浏览器内 GraphQL 调用客户端（plan 2026-07-09-0814-2 + 2026-08-10-0741-1 P2.4 auth 修正）。
 *
 * **auth 传输（P2.4 修正）**：flux 前端（nop-chaos-flux）将 access token 存于 sessionStorage
 * （`auth:tokens:v1`），并在自身 `fetch` 拦截器中注入 auth header。但 Playwright `page.request`
 * （APIRequestContext）绕过前端拦截器做裸 HTTP 调用，且 `__Host-nop-token` cookie 因 `__Host-`
 * 前缀 Secure 约束在 `page.request` 路径不被发送。结果：action-auth OFF 时 server 不校验 → 调用
 * 等价已认证；action-auth ON 时 server 校验 → `page.request` 抵达时 `roles=[]` → 全 `nop.err.auth.no-permission`。
 *
 * **修正**：`post()` 读取 `__Host-nop-token` cookie 值并显式注入 `Authorization: Bearer <token>`
 * header（server `/graphql` 认证过滤器读 `Authorization` header 或 `__Host-nop-token` cookie——
 * P2.4 实测两者均接受）。每次 post 读 cookie（cheap）以正确支持 `loginAsRole` 身份切换
 * （negative 原语 session 清空 + 新登录 → cookie 值变化 → GraphQLClient 自然跟随）。
 */
export class GraphQLClient {
  constructor(private page: Page) {}

  private async getAccessToken(): Promise<string | null> {
    const cookies = await this.page.context().cookies();
    const tokenCookie =
      cookies.find((c) => c.name === '__Host-nop-token') ??
      cookies.find((c) => c.name === 'nop-token');
    return tokenCookie?.value ?? null;
  }

  private async post(query: string, variables?: Record<string, unknown>): Promise<any> {
    const headers: Record<string, string> = {};
    const token = await this.getAccessToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    const resp = await this.page.request.post('/graphql', {
      headers,
      data: variables ? { query, variables } : { query },
    });
    return resp.json();
  }

  async findPage<T = any>(
    entityName: string,
    fields: string,
    filter?: Record<string, unknown>,
    limit = 200,
  ): Promise<T[]> {
    const fDecl = filter ? 'query($f:Map)' : '';
    const fArg = filter ? '(query:{offset:0,limit:' + limit + ',filter:$f})' : '(query:{offset:0,limit:' + limit + '})';
    const json = await this.post(
      fDecl + `{ ${entityName}__findPage` + fArg + `{ items{ ${fields} } } }`,
      filter ? { f: filter } : undefined,
    );
    return json?.data?.[`${entityName}__findPage`]?.items || [];
  }

  async get<T = any>(entityName: string, id: string | number, fields: string): Promise<T | null> {
    const json = await this.post(`{ ${entityName}__get(id:${id}){ ${fields} } }`);
    return json?.data?.[`${entityName}__get`] ?? null;
  }

  async save<T = any>(entityName: string, data: Record<string, unknown>, fields = 'id'): Promise<T> {
    const inputType = `${entityName}__save_input`;
    const json = await this.post(
      `mutation($d:${inputType}){ ${entityName}__save(data:$d){ ${fields} } }`,
      { d: data },
    );
    return json?.data?.[`${entityName}__save`] ?? null;
  }

  async update<T = any>(entityName: string, data: Record<string, unknown>, fields: string): Promise<T> {
    const inputType = `${entityName}__update_input`;
    const json = await this.post(
      `mutation($d:${inputType}){ ${entityName}__update(data:$d){ ${fields} } }`,
      { d: data },
    );
    return json?.data?.[`${entityName}__update`] ?? null;
  }

  async delete(entityName: string, id: string | number): Promise<boolean> {
    const json = await this.post(`mutation{ ${entityName}__delete(id:${id}) }`);
    return !!json?.data?.[`${entityName}__delete`];
  }

  async callMutation<T = any>(
    entityName: string,
    action: string,
    args: Record<string, unknown>,
    fields = 'id',
  ): Promise<{ data: T | null; errors: any[] | null; json: any }> {
    const parts: string[] = [];
    const varDecls: string[] = [];
    const vars: Record<string, unknown> = {};
    for (const [name, arg] of Object.entries(args)) {
      if (arg && typeof arg === 'object' && (arg as any).__input) {
        const ia = arg as { __input: true; type: string; value: Record<string, unknown> };
        varDecls.push(`$${name}:${ia.type}`);
        vars[name] = ia.value;
        parts.push(`${name}:$${name}`);
      } else {
        parts.push(`${name}:${JSON.stringify(arg)}`);
      }
    }
    const query = varDecls.length
      ? `mutation(${varDecls.join(',')}){ ${entityName}__${action}(${parts.join(',')}){ ${fields} } }`
      : `mutation{ ${entityName}__${action}(${parts.join(',')}){ ${fields} } }`;
    const json: any = await this.post(query, varDecls.length ? vars : undefined);
    return {
      data: json?.data?.[`${entityName}__${action}`] ?? null,
      errors: json?.errors ?? null,
      json,
    };
  }

  async callMutationOk<T = any>(
    entityName: string,
    action: string,
    args: Record<string, unknown>,
    fields = 'id',
  ): Promise<T> {
    const { data, errors } = await this.callMutation<T>(entityName, action, args, fields);
    if (errors) throw new Error(`${entityName}__${action} errors: ${JSON.stringify(errors)}`);
    return data!;
  }

  /**
   * Raw GraphQL POST. Returns the parsed JSON envelope. Use for scalar-returning
   * mutations (e.g. `__reverse`, `__post`, `__triggerDuePlans`) and custom
   * @BizQuery actions returning complex/list types with explicit selection sets
   * that the named helpers cannot express. Centralizes the `/graphql` transport.
   */
  async raw<T = any>(query: string, variables?: Record<string, unknown>): Promise<T> {
    return this.post(query, variables);
  }

  /**
   * Calls a custom @BizQuery action (non-CRUD query). Returns `{ data, errors, json }`.
   * Scalar args are inlined as GraphQL literals; `input()`-wrapped values become
   * typed variables. Mirrors {@link callMutation} but emits a `query` operation.
   */
  async callQuery<T = any>(
    entityName: string,
    action: string,
    args: Record<string, unknown>,
  ): Promise<{ data: T | null; errors: any[] | null; json: any }> {
    const parts: string[] = [];
    const varDecls: string[] = [];
    const vars: Record<string, unknown> = {};
    for (const [name, arg] of Object.entries(args)) {
      if (arg && typeof arg === 'object' && (arg as any).__input) {
        const ia = (arg as any) as { __input: true; type: string; value: Record<string, unknown> };
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
    const json: any = await this.post(query, varDecls.length ? vars : undefined);
    return { data: json?.data?.[`${entityName}__${action}`] ?? null, errors: json?.errors ?? null, json };
  }

  /** Returns the total count for a filtered `__findPage`. */
  async findPageTotal(entityName: string, filter: Record<string, unknown>): Promise<number> {
    const json: any = await this.post(
      `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:1,filter:$f}){ total } }`,
      { f: filter },
    );
    return json?.data?.[`${entityName}__findPage`]?.total ?? 0;
  }

  /** Returns the first item matching `filter` with `selection`, or null. */
  async findFirst<T = any>(
    entityName: string,
    filter: Record<string, unknown>,
    selection: string,
  ): Promise<T | null> {
    const json: any = await this.post(
      `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:1,filter:$f}){ items{ ${selection} } } }`,
      { f: filter },
    );
    const items: any[] = json?.data?.[`${entityName}__findPage`]?.items || [];
    return items.length > 0 ? (items[0] as T) : null;
  }

  /** Returns up to `limit` (default 500) items matching `filter` with `selection`. */
  async findItems<T = any>(
    entityName: string,
    filter: Record<string, unknown>,
    selection: string,
    limit = 500,
  ): Promise<T[]> {
    const json: any = await this.post(
      `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:${limit},filter:$f}){ items{ ${selection} } } }`,
      { f: filter },
    );
    return json?.data?.[`${entityName}__findPage`]?.items || [];
  }

  /**
   * Logically deletes (via `__delete`) every entity matching `filter`.
   * Returns the number of deleted items. Used for cleanup of side-effect
   * products that would otherwise pollute shared-DB numeric baselines.
   */
  async deleteByFilter(entityName: string, filter: Record<string, unknown>): Promise<number> {
    const items = await this.findItems<{ id: string | number }>(entityName, filter, 'id');
    for (const item of items) {
      await this.delete(entityName, item.id);
    }
    return items.length;
  }

  /** Deletes a single entity by id (thin wrapper over {@link delete}). */
  async deleteById(entityName: string, id: string | number): Promise<void> {
    await this.delete(entityName, id);
  }
}
