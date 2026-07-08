import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'purchase',
  entityRoute: 'ErpPurOrder',
  addFormField: 'code',
});
