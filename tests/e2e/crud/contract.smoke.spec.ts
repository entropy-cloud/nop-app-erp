import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'contract',
  entityRoute: 'ErpCtContract',
  addFormField: 'code',
});
