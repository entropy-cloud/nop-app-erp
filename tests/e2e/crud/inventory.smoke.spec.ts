import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'inventory',
  entityRoute: 'ErpInvStockMove',
  addFormField: 'code',
});
