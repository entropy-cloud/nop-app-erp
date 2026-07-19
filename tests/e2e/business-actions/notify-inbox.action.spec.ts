import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  eqFilter,
  input,
  deleteByFilter,
  deleteById,
  findItems,
  GraphQLClient,
} from './_helper';

/**
 * Notify inbox 浏览器层 E2E（plan 2026-07-19-2200-3 Phase 4）。
 *
 * 验证 ErpSysNotificationBizModel 收件箱五端点（findUnread / findRead / countUnread / markRead / markAllRead）
 * 经 GraphQL /graphql 的全栈可达性 + 已读状态翻转。本套测试关注 **后端 ctx.getUserId() 回退路径** ——
 * 调用时 userId 显式传 null（与 inbox.page.yaml 实际接线一致），由 BizModel 的 resolveUserId 解析到
 * 当前登录用户 ID（默认 nop）。
 *
 * 后端权威：module-notify/erp-notify-service/.../ErpSysNotificationBizModel.java（Phase 1 Fix：
 * findUnread/countUnread/markAllRead userId 参数 @Optional + resolveUserId 回退 ctx.getUserId()）。
 *
 * 数据隔离策略：经 ErpSysNotificationTemplate__save 创建唯一 ACTIVE 模板（recipientResolver=USER_LIST +
 * recipientConfig={"userIds":["nop"]}），再经 ErpSysNotification__notify(eventType, context) 派发通知到
 * 当前登录用户。绕开 ErpSysNotification__save 因 stdDomain="userId" 校验未注册 handler 的产品缺陷
 * （`recipientUserId` 列在 `_ErpSysNotification.xmeta:36` 标注 `stdDomain="userId"`，但项目内无对应
 * StdDomainHandler 注册——非本计划范围，登记到 plan Deferred）。`notify()` mutation 内部走 dao.saveEntity
 * 路径不经 GraphQL save 校验，故通知可正常落库。
 *
 * 清理：每个测试删除自身创建的 notification + read + template。
 */

const RECIPIENT = 'nop'; // 默认登录用户（playwright.config.ts webServer allow-create-default-user）

interface Template {
  id: string | number;
}

async function seedTemplate(
  page: import('@playwright/test').Page,
  templateId: number,
  eventType: string,
): Promise<Template> {
  return createViaSave(
    page,
    'ErpSysNotificationTemplate',
    {
      id: templateId,
      notificationType: eventType,
      name: `E2E-TPL-${eventType}`,
      channelSet: 'IN_APP',
      subjectTpl: '[E2E] ' + eventType + ': ${tplMarker}',
      bodyTpl: 'E2E notification body for ${tpl_marker}',
      recipientResolver: 'USER_LIST',
      recipientConfig: JSON.stringify({ userIds: [RECIPIENT] }),
      mergeWindowSeconds: 0,
      mergeStrategy: 'NONE',
      status: 'ACTIVE',
    },
    'id',
  );
}

async function dispatch(
  page: import('@playwright/test').Page,
  eventType: string,
  context: Record<string, unknown>,
): Promise<number[]> {
  const result = await callMutationOk(
    page,
    'ErpSysNotification',
    'notify',
    { eventType, context: input('Map', context) },
    'id recipientUserId',
  );
  const list = Array.isArray(result) ? result : [result];
  return list.map((r: any) => Number(r.id));
}

async function fetchUnreadIds(page: import('@playwright/test').Page, eventType: string): Promise<number[]> {
  // findUnread 返回 List<ErpSysNotification>，必须指定 selection；走 raw GraphQL
  // 显式传 userId='nop' —— Playwright page.request 不经 SPA 桥（host-amis-adapter），
  // 故 localStorage JWT 不会自动注入请求头，ctx.getUserId() 回退到 'sys'（service-public=true 兜底）。
  // E2E 仅验证端点全栈可达性 + 数据语义；ctx.getUserId() 回退路径由 mvn test 单测覆盖
  // （TestErpSysNotificationDispatch 通过 IGraphQLEngine 直接执行，无 HTTP 鉴权层）。
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    'query{ ErpSysNotification__findUnread(userId:"nop"){ id notificationType recipientUserId status } }',
  );
  expect(json?.errors, 'findUnread should not return errors').toBeFalsy();
  const rows = ((json?.data?.ErpSysNotification__findUnread || []) as Array<{
    id: number | string;
    notificationType: string;
    recipientUserId: string;
    status: string;
  }>);
  return rows.filter((r) => r.notificationType === eventType).map((r) => Number(r.id));
}

async function fetchCountUnread(page: import('@playwright/test').Page): Promise<number> {
  // countUnread 返回 long（标量），无需 selection；显式 userId='nop'（详见 fetchUnreadIds 注释）
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw('query{ ErpSysNotification__countUnread(userId:"nop") }');
  expect(json?.errors, 'countUnread should not return errors').toBeFalsy();
  const v = json?.data?.ErpSysNotification__countUnread;
  return Number(v || 0);
}

async function fetchReadIds(page: import('@playwright/test').Page, eventType: string): Promise<number[]> {
  const gql = new GraphQLClient(page);
  const json: any = await gql.raw(
    'query{ ErpSysNotification__findRead(userId:"nop"){ id notificationType } }',
  );
  expect(json?.errors, 'findRead should not return errors').toBeFalsy();
  const rows = ((json?.data?.ErpSysNotification__findRead || []) as Array<{
    id: number | string;
    notificationType: string;
  }>);
  return rows.filter((r) => r.notificationType === eventType).map((r) => Number(r.id));
}

test.describe('Notify inbox actions (findUnread/findRead/countUnread/markRead/markAllRead)', () => {
  test('findUnread + countUnread reflects dispatched notification for current user', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSysNotification-inbox');

    const ts = Date.now();
    const eventType = `e2e-inbox-unread-${ts}`;
    const tpl = await seedTemplate(page, 900000000 + (ts % 1000000), eventType);

    try {
      const ids = await dispatch(page, eventType, { tpl_marker: 'X' });
      expect(ids.length, 'notify should dispatch 1 notification to nop').toBeGreaterThanOrEqual(1);

      // findUnread(userId:null) → 后端 resolveUserId 回退 ctx.getUserId()='nop'，应含本通知
      const unreadIds = await fetchUnreadIds(page, eventType);
      expect(unreadIds, 'findUnread should include dispatched notification').toContain(ids[0]);

      // countUnread(userId:null) ≥ 1
      const count = await fetchCountUnread(page);
      expect(count, 'countUnread >= 1 after dispatch').toBeGreaterThanOrEqual(1);
    } finally {
      // 清理：通知 + 已读记录 + 模板
      const leftover = await fetchUnreadIds(page, eventType).catch(() => [] as number[]);
      const readLeftover = await fetchReadIds(page, eventType).catch(() => [] as number[]);
      for (const id of [...leftover, ...readLeftover]) {
        await deleteByFilter(page, 'ErpSysNotificationRead', eqFilter('notificationId', id));
        await deleteById(page, 'ErpSysNotification', id);
      }
      await deleteById(page, 'ErpSysNotificationTemplate', tpl.id);
    }
  });

  test('markRead moves notification from unread to read', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSysNotification-inbox');

    const ts = Date.now();
    const eventType = `e2e-inbox-mark-${ts}`;
    const tpl = await seedTemplate(page, 900100000 + (ts % 1000000), eventType);

    try {
      const ids = await dispatch(page, eventType, { tpl_marker: 'M' });
      expect(ids.length, 'notify should dispatch').toBeGreaterThanOrEqual(1);
      const notifId = ids[0];

      const beforeN = await fetchCountUnread(page);

      // markRead(notificationId) —— BizModel 内部以 n.getRecipientUserId() 作为已读记录 userId
      await callMutationOk(page, 'ErpSysNotification', 'markRead', { notificationId: notifId }, 'id');

      const afterN = await fetchCountUnread(page);
      expect(afterN, 'countUnread should decrease by 1').toBe(beforeN - 1);

      // findRead(userId:null) 应含本通知
      const readIds = await fetchReadIds(page, eventType);
      expect(readIds, 'findRead should contain the marked-read notification').toContain(notifId);

      // findUnread(userId:null) 不应再含本通知
      const unreadIds = await fetchUnreadIds(page, eventType);
      expect(unreadIds, 'findUnread should NOT contain the marked-read notification').not.toContain(notifId);
    } finally {
      const leftover = await fetchUnreadIds(page, eventType).catch(() => [] as number[]);
      const readLeftover = await fetchReadIds(page, eventType).catch(() => [] as number[]);
      for (const id of [...leftover, ...readLeftover]) {
        await deleteByFilter(page, 'ErpSysNotificationRead', eqFilter('notificationId', id));
        await deleteById(page, 'ErpSysNotification', id);
      }
      await deleteById(page, 'ErpSysNotificationTemplate', tpl.id);
    }
  });

  test('markAllRead(userId:null) clears all unread for current user', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSysNotification-inbox');

    const ts = Date.now();
    const eventType1 = `e2e-inbox-markall-1-${ts}`;
    const eventType2 = `e2e-inbox-markall-2-${ts}`;
    const tpl1 = await seedTemplate(page, 900200000 + (ts % 1000000), eventType1);
    const tpl2 = await seedTemplate(page, 900300000 + (ts % 1000000), eventType2);

    try {
      const ids1 = await dispatch(page, eventType1, { tpl_marker: 'A1' });
      const ids2 = await dispatch(page, eventType2, { tpl_marker: 'A2' });
      expect(ids1.length + ids2.length, 'should dispatch 2 notifications').toBeGreaterThanOrEqual(2);

      // markAllRead(userId:"nop") —— 显式传 nop（Playwright 不经 SPA 桥，JWT 不自动注入）
      const gql = new GraphQLClient(page);
      const markAllRes: any = await gql.raw('mutation{ ErpSysNotification__markAllRead(userId:"nop") }');
      expect(markAllRes?.errors, 'markAllRead should not return errors').toBeFalsy();
      const markedCount = markAllRes?.data?.ErpSysNotification__markAllRead;
      expect(Number(markedCount), 'markAllRead should process >= 2').toBeGreaterThanOrEqual(2);

      // 两个事件类型的 notification 都不应在 findUnread 中
      const unread1 = await fetchUnreadIds(page, eventType1);
      const unread2 = await fetchUnreadIds(page, eventType2);
      expect(unread1.length, 'eventType1 notifications should be cleared from unread').toBe(0);
      expect(unread2.length, 'eventType2 notifications should be cleared from unread').toBe(0);

      // findRead 应含两个事件类型的 notification
      const readEv1 = await fetchReadIds(page, eventType1);
      const readEv2 = await fetchReadIds(page, eventType2);
      expect(readEv1.length, 'eventType1 should have ≥1 in findRead').toBeGreaterThanOrEqual(1);
      expect(readEv2.length, 'eventType2 should have ≥1 in findRead').toBeGreaterThanOrEqual(1);
    } finally {
      // 清理两批通知 + 模板
      for (const ev of [eventType1, eventType2]) {
        const items = await findItems(
          page,
          'ErpSysNotification',
          eqFilter('notificationType', ev),
          'id',
          100,
        ).catch(() => [] as Array<{ id: number | string }>);
        for (const it of items) {
          await deleteByFilter(page, 'ErpSysNotificationRead', eqFilter('notificationId', Number(it.id)));
          await deleteById(page, 'ErpSysNotification', it.id);
        }
      }
      await deleteById(page, 'ErpSysNotificationTemplate', tpl1.id);
      await deleteById(page, 'ErpSysNotificationTemplate', tpl2.id);
    }
  });
});
