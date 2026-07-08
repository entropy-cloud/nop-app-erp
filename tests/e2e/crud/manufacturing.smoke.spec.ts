import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'manufacturing',
  entityRoute: 'ErpMfgWorkOrder',
  addFormField: 'code',
});
