import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'sales',
  entityRoute: 'ErpSalOrder',
  addFormField: 'code',
});
