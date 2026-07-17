import { runAmisFormWrite } from './_helper';

runAmisFormWrite({
  entityName: 'ErpMdPartner',
  route: '/ErpMdPartner-main',
  textFields: {
    code: `AMIS-PARTNER-${Date.now()}`,
    name: 'AMIS Form Write Partner',
  },
  // Labels/options accept multiple locale variants; dict value codes (CUSTOMER/ACTIVE)
  // are locale-independent and always present in the AMIS option text ("CODE-Label").
  dictFields: [
    { label: ['类型', 'Partner Type'], option: ['CUSTOMER', '客户', 'Customer'] },
    { label: ['状态', 'Status'], option: ['ACTIVE', '启用', 'Active', 'Enabled'] },
  ],
  editTextField: { name: 'name', value: 'AMIS Form Write UPDATED' },
  verifySelection: 'id',
});
