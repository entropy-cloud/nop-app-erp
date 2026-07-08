import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'master-data',
  entityRoute: 'ErpMdPartner',
  addFormField: 'code',
});
