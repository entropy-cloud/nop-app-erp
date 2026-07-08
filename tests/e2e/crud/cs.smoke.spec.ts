import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'cs',
  entityRoute: 'ErpCsTicket',
  addFormField: 'code',
});
