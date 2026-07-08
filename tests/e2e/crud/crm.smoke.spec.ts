import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'crm',
  entityRoute: 'ErpCrmLead',
  addFormField: 'code',
});
