import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'finance',
  entityRoute: 'ErpFinVoucher',
  addFormField: 'code',
});
