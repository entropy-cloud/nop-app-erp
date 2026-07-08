import { runCrudListSmoke } from './_helper';

runCrudListSmoke({
  domain: 'logistics',
  entityRoute: 'ErpLogShipment',
  addFormField: 'code',
});
