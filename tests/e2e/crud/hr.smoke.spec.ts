import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'hr',
  entityRoute: 'ErpHrEmployee',
  addFormField: 'code',
});
