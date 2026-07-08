import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'drp',
  entityRoute: 'ErpDrpPlan',
  addFormField: 'code',
});
