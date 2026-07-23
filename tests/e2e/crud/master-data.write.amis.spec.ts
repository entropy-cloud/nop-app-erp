import { runAmisFormWrite } from './_helper';

runAmisFormWrite({
  entityName: 'ErpMdPartner',
  route: '/ErpMdPartner-main',
  textFields: {
    code: `AMIS-PARTNER-${Date.now()}`,
    name: 'AMIS Form Write Partner',
  },
  dictFields: [
    { label: ['类型', 'Partner Type'], option: ['CUSTOMER', '客户', 'Customer'] },
  ],
  editTextField: { name: 'name', value: 'AMIS Form Write UPDATED' },
  verifySelection: 'id',
});
