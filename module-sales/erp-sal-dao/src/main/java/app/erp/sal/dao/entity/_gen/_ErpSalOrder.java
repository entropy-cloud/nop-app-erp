package app.erp.sal.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.sal.dao.entity.ErpSalOrder;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  销售订单: erp_sal_order
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpSalOrder extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 报价单: QUOTATION_ID BIGINT */
    public static final String PROP_NAME_quotationId = "quotationId";
    public static final int PROP_ID_quotationId = 4;
    
    /* 销售合同: CONTRACT_ID BIGINT */
    public static final String PROP_NAME_contractId = "contractId";
    public static final int PROP_ID_contractId = 5;
    
    /* 客户: CUSTOMER_ID BIGINT */
    public static final String PROP_NAME_customerId = "customerId";
    public static final int PROP_ID_customerId = 6;
    
    /* 发货仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 7;
    
    /* 订单日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 8;
    
    /* 交货日期: DELIVERY_DATE DATE */
    public static final String PROP_NAME_deliveryDate = "deliveryDate";
    public static final int PROP_ID_deliveryDate = 9;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 10;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 11;
    
    /* 合计金额(源币不含税): AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 12;
    
    /* 合计金额(本位币不含税): AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 13;
    
    /* 合计金额(不含税): TOTAL_AMOUNT DECIMAL */
    public static final String PROP_NAME_totalAmount = "totalAmount";
    public static final int PROP_ID_totalAmount = 14;
    
    /* 合计税额: TOTAL_TAX_AMOUNT DECIMAL */
    public static final String PROP_NAME_totalTaxAmount = "totalTaxAmount";
    public static final int PROP_ID_totalTaxAmount = 15;
    
    /* 合计金额(含税): TOTAL_AMOUNT_WITH_TAX DECIMAL */
    public static final String PROP_NAME_totalAmountWithTax = "totalAmountWithTax";
    public static final int PROP_ID_totalAmountWithTax = 16;
    
    /* 整单折扣率(%): DISCOUNT_RATE DECIMAL */
    public static final String PROP_NAME_discountRate = "discountRate";
    public static final int PROP_ID_discountRate = 17;
    
    /* 折扣金额: DISCOUNT_AMOUNT DECIMAL */
    public static final String PROP_NAME_discountAmount = "discountAmount";
    public static final int PROP_ID_discountAmount = 18;
    
    /* 已收金额: RECEIVED_AMOUNT DECIMAL */
    public static final String PROP_NAME_receivedAmount = "receivedAmount";
    public static final int PROP_ID_receivedAmount = 19;
    
    /* 结算方式: SETTLEMENT_METHOD_ID BIGINT */
    public static final String PROP_NAME_settlementMethodId = "settlementMethodId";
    public static final int PROP_ID_settlementMethodId = 20;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 21;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 22;
    
    /* 收款进度: RECEIVED_STATUS VARCHAR */
    public static final String PROP_NAME_receivedStatus = "receivedStatus";
    public static final int PROP_ID_receivedStatus = 23;
    
    /* 发货进度: DELIVERY_STATUS VARCHAR */
    public static final String PROP_NAME_deliveryStatus = "deliveryStatus";
    public static final int PROP_ID_deliveryStatus = 24;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 25;
    
    /* 过账时间: POSTED_AT TIMESTAMP */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 26;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 27;
    
    /* 审核人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 28;
    
    /* 审核时间: APPROVED_AT TIMESTAMP */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 29;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 30;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 31;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 32;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 33;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 34;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 35;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 36;
    

    private static int _PROP_ID_BOUND = 37;

    
    /* relation:  */
    public static final String PROP_NAME_customer = "customer";
    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_settlementMethod = "settlementMethod";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_quotation = "quotation";
    
    /* relation:  */
    public static final String PROP_NAME_contract = "contract";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[37];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_quotationId] = PROP_NAME_quotationId;
          PROP_NAME_TO_ID.put(PROP_NAME_quotationId, PROP_ID_quotationId);
      
          PROP_ID_TO_NAME[PROP_ID_contractId] = PROP_NAME_contractId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractId, PROP_ID_contractId);
      
          PROP_ID_TO_NAME[PROP_ID_customerId] = PROP_NAME_customerId;
          PROP_NAME_TO_ID.put(PROP_NAME_customerId, PROP_ID_customerId);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_deliveryDate] = PROP_NAME_deliveryDate;
          PROP_NAME_TO_ID.put(PROP_NAME_deliveryDate, PROP_ID_deliveryDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_totalAmount] = PROP_NAME_totalAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_totalAmount, PROP_ID_totalAmount);
      
          PROP_ID_TO_NAME[PROP_ID_totalTaxAmount] = PROP_NAME_totalTaxAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_totalTaxAmount, PROP_ID_totalTaxAmount);
      
          PROP_ID_TO_NAME[PROP_ID_totalAmountWithTax] = PROP_NAME_totalAmountWithTax;
          PROP_NAME_TO_ID.put(PROP_NAME_totalAmountWithTax, PROP_ID_totalAmountWithTax);
      
          PROP_ID_TO_NAME[PROP_ID_discountRate] = PROP_NAME_discountRate;
          PROP_NAME_TO_ID.put(PROP_NAME_discountRate, PROP_ID_discountRate);
      
          PROP_ID_TO_NAME[PROP_ID_discountAmount] = PROP_NAME_discountAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_discountAmount, PROP_ID_discountAmount);
      
          PROP_ID_TO_NAME[PROP_ID_receivedAmount] = PROP_NAME_receivedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_receivedAmount, PROP_ID_receivedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_settlementMethodId] = PROP_NAME_settlementMethodId;
          PROP_NAME_TO_ID.put(PROP_NAME_settlementMethodId, PROP_ID_settlementMethodId);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_receivedStatus] = PROP_NAME_receivedStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_receivedStatus, PROP_ID_receivedStatus);
      
          PROP_ID_TO_NAME[PROP_ID_deliveryStatus] = PROP_NAME_deliveryStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_deliveryStatus, PROP_ID_deliveryStatus);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_delVersion] = PROP_NAME_delVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_delVersion, PROP_ID_delVersion);
      
          PROP_ID_TO_NAME[PROP_ID_version] = PROP_NAME_version;
          PROP_NAME_TO_ID.put(PROP_NAME_version, PROP_ID_version);
      
          PROP_ID_TO_NAME[PROP_ID_createdBy] = PROP_NAME_createdBy;
          PROP_NAME_TO_ID.put(PROP_NAME_createdBy, PROP_ID_createdBy);
      
          PROP_ID_TO_NAME[PROP_ID_createTime] = PROP_NAME_createTime;
          PROP_NAME_TO_ID.put(PROP_NAME_createTime, PROP_ID_createTime);
      
          PROP_ID_TO_NAME[PROP_ID_updatedBy] = PROP_NAME_updatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_updatedBy, PROP_ID_updatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_updateTime] = PROP_NAME_updateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_updateTime, PROP_ID_updateTime);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 报价单: QUOTATION_ID */
    private java.lang.Long _quotationId;
    
    /* 销售合同: CONTRACT_ID */
    private java.lang.Long _contractId;
    
    /* 客户: CUSTOMER_ID */
    private java.lang.Long _customerId;
    
    /* 发货仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 订单日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 交货日期: DELIVERY_DATE */
    private java.time.LocalDate _deliveryDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 合计金额(源币不含税): AMOUNT_SOURCE */
    private java.math.BigDecimal _amountSource;
    
    /* 合计金额(本位币不含税): AMOUNT_FUNCTIONAL */
    private java.math.BigDecimal _amountFunctional;
    
    /* 合计金额(不含税): TOTAL_AMOUNT */
    private java.math.BigDecimal _totalAmount;
    
    /* 合计税额: TOTAL_TAX_AMOUNT */
    private java.math.BigDecimal _totalTaxAmount;
    
    /* 合计金额(含税): TOTAL_AMOUNT_WITH_TAX */
    private java.math.BigDecimal _totalAmountWithTax;
    
    /* 整单折扣率(%): DISCOUNT_RATE */
    private java.math.BigDecimal _discountRate;
    
    /* 折扣金额: DISCOUNT_AMOUNT */
    private java.math.BigDecimal _discountAmount;
    
    /* 已收金额: RECEIVED_AMOUNT */
    private java.math.BigDecimal _receivedAmount;
    
    /* 结算方式: SETTLEMENT_METHOD_ID */
    private java.lang.Long _settlementMethodId;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 收款进度: RECEIVED_STATUS */
    private java.lang.String _receivedStatus;
    
    /* 发货进度: DELIVERY_STATUS */
    private java.lang.String _deliveryStatus;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.sql.Timestamp _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 审核人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 审核时间: APPROVED_AT */
    private java.sql.Timestamp _approvedAt;
    
    /* 逻辑删除版本: DEL_VERSION */
    private java.lang.Long _delVersion;
    
    /* 数据版本: VERSION */
    private java.lang.Integer _version;
    
    /* 创建人: CREATED_BY */
    private java.lang.String _createdBy;
    
    /* 创建时间: CREATE_TIME */
    private java.sql.Timestamp _createTime;
    
    /* 修改人: UPDATED_BY */
    private java.lang.String _updatedBy;
    
    /* 修改时间: UPDATE_TIME */
    private java.sql.Timestamp _updateTime;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    

    public _ErpSalOrder(){
        // for debug
    }

    protected ErpSalOrder newInstance(){
        ErpSalOrder entity = new ErpSalOrder();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpSalOrder cloneInstance() {
        ErpSalOrder entity = newInstance();
        orm_forEachInitedProp((value, propId) -> {
            entity.orm_propValue(propId,value);
        });
        return entity;
    }

    @Override
    public String orm_entityName() {
      // 如果存在实体模型对象，则以模型对象上的设置为准
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getName();
      return "app.erp.sal.dao.entity.ErpSalOrder";
    }

    @Override
    public int orm_propIdBound(){
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getPropIdBound();
      return _PROP_ID_BOUND;
    }

    @Override
    public Object orm_id() {
    
        return buildSimpleId(PROP_ID_id);
     
    }

    @Override
    public boolean orm_isPrimary(int propId) {
        
            return propId == PROP_ID_id;
          
    }

    @Override
    public String orm_propName(int propId) {
        if(propId >= PROP_ID_TO_NAME.length)
            return super.orm_propName(propId);
        String propName = PROP_ID_TO_NAME[propId];
        if(propName == null)
           return super.orm_propName(propId);
        return propName;
    }

    @Override
    public int orm_propId(String propName) {
        Integer propId = PROP_NAME_TO_ID.get(propName);
        if(propId == null)
            return super.orm_propId(propName);
        return propId;
    }

    @Override
    public Object orm_propValue(int propId) {
        switch(propId){
        
            case PROP_ID_id:
               return getId();
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_quotationId:
               return getQuotationId();
        
            case PROP_ID_contractId:
               return getContractId();
        
            case PROP_ID_customerId:
               return getCustomerId();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_deliveryDate:
               return getDeliveryDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
            case PROP_ID_totalAmount:
               return getTotalAmount();
        
            case PROP_ID_totalTaxAmount:
               return getTotalTaxAmount();
        
            case PROP_ID_totalAmountWithTax:
               return getTotalAmountWithTax();
        
            case PROP_ID_discountRate:
               return getDiscountRate();
        
            case PROP_ID_discountAmount:
               return getDiscountAmount();
        
            case PROP_ID_receivedAmount:
               return getReceivedAmount();
        
            case PROP_ID_settlementMethodId:
               return getSettlementMethodId();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_receivedStatus:
               return getReceivedStatus();
        
            case PROP_ID_deliveryStatus:
               return getDeliveryStatus();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
            case PROP_ID_delVersion:
               return getDelVersion();
        
            case PROP_ID_version:
               return getVersion();
        
            case PROP_ID_createdBy:
               return getCreatedBy();
        
            case PROP_ID_createTime:
               return getCreateTime();
        
            case PROP_ID_updatedBy:
               return getUpdatedBy();
        
            case PROP_ID_updateTime:
               return getUpdateTime();
        
            case PROP_ID_remark:
               return getRemark();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_quotationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_quotationId));
               }
               setQuotationId(typedValue);
               break;
            }
        
            case PROP_ID_contractId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractId));
               }
               setContractId(typedValue);
               break;
            }
        
            case PROP_ID_customerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_customerId));
               }
               setCustomerId(typedValue);
               break;
            }
        
            case PROP_ID_warehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_warehouseId));
               }
               setWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_deliveryDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_deliveryDate));
               }
               setDeliveryDate(typedValue);
               break;
            }
        
            case PROP_ID_currencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_currencyId));
               }
               setCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_exchangeRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
               break;
            }
        
            case PROP_ID_totalAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalAmount));
               }
               setTotalAmount(typedValue);
               break;
            }
        
            case PROP_ID_totalTaxAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalTaxAmount));
               }
               setTotalTaxAmount(typedValue);
               break;
            }
        
            case PROP_ID_totalAmountWithTax:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalAmountWithTax));
               }
               setTotalAmountWithTax(typedValue);
               break;
            }
        
            case PROP_ID_discountRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_discountRate));
               }
               setDiscountRate(typedValue);
               break;
            }
        
            case PROP_ID_discountAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_discountAmount));
               }
               setDiscountAmount(typedValue);
               break;
            }
        
            case PROP_ID_receivedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_receivedAmount));
               }
               setReceivedAmount(typedValue);
               break;
            }
        
            case PROP_ID_settlementMethodId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_settlementMethodId));
               }
               setSettlementMethodId(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
               break;
            }
        
            case PROP_ID_receivedStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receivedStatus));
               }
               setReceivedStatus(typedValue);
               break;
            }
        
            case PROP_ID_deliveryStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_deliveryStatus));
               }
               setDeliveryStatus(typedValue);
               break;
            }
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
               break;
            }
        
            case PROP_ID_approvedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvedBy));
               }
               setApprovedBy(typedValue);
               break;
            }
        
            case PROP_ID_approvedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
               break;
            }
        
            case PROP_ID_delVersion:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_delVersion));
               }
               setDelVersion(typedValue);
               break;
            }
        
            case PROP_ID_version:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_version));
               }
               setVersion(typedValue);
               break;
            }
        
            case PROP_ID_createdBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_createdBy));
               }
               setCreatedBy(typedValue);
               break;
            }
        
            case PROP_ID_createTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_createTime));
               }
               setCreateTime(typedValue);
               break;
            }
        
            case PROP_ID_updatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_updatedBy));
               }
               setUpdatedBy(typedValue);
               break;
            }
        
            case PROP_ID_updateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_updateTime));
               }
               setUpdateTime(typedValue);
               break;
            }
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
               break;
            }
        
           default:
              super.orm_propValue(propId,value);
        }
    }

    @Override
    public void orm_internalSet(int propId, Object value) {
        switch(propId){
        
            case PROP_ID_id:{
               onInitProp(propId);
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_quotationId:{
               onInitProp(propId);
               this._quotationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_contractId:{
               onInitProp(propId);
               this._contractId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_customerId:{
               onInitProp(propId);
               this._customerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_deliveryDate:{
               onInitProp(propId);
               this._deliveryDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalAmount:{
               onInitProp(propId);
               this._totalAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalTaxAmount:{
               onInitProp(propId);
               this._totalTaxAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalAmountWithTax:{
               onInitProp(propId);
               this._totalAmountWithTax = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_discountRate:{
               onInitProp(propId);
               this._discountRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_discountAmount:{
               onInitProp(propId);
               this._discountAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_receivedAmount:{
               onInitProp(propId);
               this._receivedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_settlementMethodId:{
               onInitProp(propId);
               this._settlementMethodId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receivedStatus:{
               onInitProp(propId);
               this._receivedStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_deliveryStatus:{
               onInitProp(propId);
               this._deliveryStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedBy:{
               onInitProp(propId);
               this._approvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_delVersion:{
               onInitProp(propId);
               this._delVersion = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_version:{
               onInitProp(propId);
               this._version = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_createdBy:{
               onInitProp(propId);
               this._createdBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_createTime:{
               onInitProp(propId);
               this._createTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_updatedBy:{
               onInitProp(propId);
               this._updatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_updateTime:{
               onInitProp(propId);
               this._updateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.Long getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.Long value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 报价单: QUOTATION_ID
     */
    public final java.lang.Long getQuotationId(){
         onPropGet(PROP_ID_quotationId);
         return _quotationId;
    }

    /**
     * 报价单: QUOTATION_ID
     */
    public final void setQuotationId(java.lang.Long value){
        if(onPropSet(PROP_ID_quotationId,value)){
            this._quotationId = value;
            internalClearRefs(PROP_ID_quotationId);
            
        }
    }
    
    /**
     * 销售合同: CONTRACT_ID
     */
    public final java.lang.Long getContractId(){
         onPropGet(PROP_ID_contractId);
         return _contractId;
    }

    /**
     * 销售合同: CONTRACT_ID
     */
    public final void setContractId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractId,value)){
            this._contractId = value;
            internalClearRefs(PROP_ID_contractId);
            
        }
    }
    
    /**
     * 客户: CUSTOMER_ID
     */
    public final java.lang.Long getCustomerId(){
         onPropGet(PROP_ID_customerId);
         return _customerId;
    }

    /**
     * 客户: CUSTOMER_ID
     */
    public final void setCustomerId(java.lang.Long value){
        if(onPropSet(PROP_ID_customerId,value)){
            this._customerId = value;
            internalClearRefs(PROP_ID_customerId);
            
        }
    }
    
    /**
     * 发货仓库: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 发货仓库: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 订单日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 订单日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 交货日期: DELIVERY_DATE
     */
    public final java.time.LocalDate getDeliveryDate(){
         onPropGet(PROP_ID_deliveryDate);
         return _deliveryDate;
    }

    /**
     * 交货日期: DELIVERY_DATE
     */
    public final void setDeliveryDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_deliveryDate,value)){
            this._deliveryDate = value;
            internalClearRefs(PROP_ID_deliveryDate);
            
        }
    }
    
    /**
     * 币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
        }
    }
    
    /**
     * 汇率: EXCHANGE_RATE
     */
    public final java.math.BigDecimal getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 合计金额(源币不含税): AMOUNT_SOURCE
     */
    public final java.math.BigDecimal getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 合计金额(源币不含税): AMOUNT_SOURCE
     */
    public final void setAmountSource(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 合计金额(本位币不含税): AMOUNT_FUNCTIONAL
     */
    public final java.math.BigDecimal getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 合计金额(本位币不含税): AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
        }
    }
    
    /**
     * 合计金额(不含税): TOTAL_AMOUNT
     */
    public final java.math.BigDecimal getTotalAmount(){
         onPropGet(PROP_ID_totalAmount);
         return _totalAmount;
    }

    /**
     * 合计金额(不含税): TOTAL_AMOUNT
     */
    public final void setTotalAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalAmount,value)){
            this._totalAmount = value;
            internalClearRefs(PROP_ID_totalAmount);
            
        }
    }
    
    /**
     * 合计税额: TOTAL_TAX_AMOUNT
     */
    public final java.math.BigDecimal getTotalTaxAmount(){
         onPropGet(PROP_ID_totalTaxAmount);
         return _totalTaxAmount;
    }

    /**
     * 合计税额: TOTAL_TAX_AMOUNT
     */
    public final void setTotalTaxAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalTaxAmount,value)){
            this._totalTaxAmount = value;
            internalClearRefs(PROP_ID_totalTaxAmount);
            
        }
    }
    
    /**
     * 合计金额(含税): TOTAL_AMOUNT_WITH_TAX
     */
    public final java.math.BigDecimal getTotalAmountWithTax(){
         onPropGet(PROP_ID_totalAmountWithTax);
         return _totalAmountWithTax;
    }

    /**
     * 合计金额(含税): TOTAL_AMOUNT_WITH_TAX
     */
    public final void setTotalAmountWithTax(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalAmountWithTax,value)){
            this._totalAmountWithTax = value;
            internalClearRefs(PROP_ID_totalAmountWithTax);
            
        }
    }
    
    /**
     * 整单折扣率(%): DISCOUNT_RATE
     */
    public final java.math.BigDecimal getDiscountRate(){
         onPropGet(PROP_ID_discountRate);
         return _discountRate;
    }

    /**
     * 整单折扣率(%): DISCOUNT_RATE
     */
    public final void setDiscountRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_discountRate,value)){
            this._discountRate = value;
            internalClearRefs(PROP_ID_discountRate);
            
        }
    }
    
    /**
     * 折扣金额: DISCOUNT_AMOUNT
     */
    public final java.math.BigDecimal getDiscountAmount(){
         onPropGet(PROP_ID_discountAmount);
         return _discountAmount;
    }

    /**
     * 折扣金额: DISCOUNT_AMOUNT
     */
    public final void setDiscountAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_discountAmount,value)){
            this._discountAmount = value;
            internalClearRefs(PROP_ID_discountAmount);
            
        }
    }
    
    /**
     * 已收金额: RECEIVED_AMOUNT
     */
    public final java.math.BigDecimal getReceivedAmount(){
         onPropGet(PROP_ID_receivedAmount);
         return _receivedAmount;
    }

    /**
     * 已收金额: RECEIVED_AMOUNT
     */
    public final void setReceivedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_receivedAmount,value)){
            this._receivedAmount = value;
            internalClearRefs(PROP_ID_receivedAmount);
            
        }
    }
    
    /**
     * 结算方式: SETTLEMENT_METHOD_ID
     */
    public final java.lang.Long getSettlementMethodId(){
         onPropGet(PROP_ID_settlementMethodId);
         return _settlementMethodId;
    }

    /**
     * 结算方式: SETTLEMENT_METHOD_ID
     */
    public final void setSettlementMethodId(java.lang.Long value){
        if(onPropSet(PROP_ID_settlementMethodId,value)){
            this._settlementMethodId = value;
            internalClearRefs(PROP_ID_settlementMethodId);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
        }
    }
    
    /**
     * 收款进度: RECEIVED_STATUS
     */
    public final java.lang.String getReceivedStatus(){
         onPropGet(PROP_ID_receivedStatus);
         return _receivedStatus;
    }

    /**
     * 收款进度: RECEIVED_STATUS
     */
    public final void setReceivedStatus(java.lang.String value){
        if(onPropSet(PROP_ID_receivedStatus,value)){
            this._receivedStatus = value;
            internalClearRefs(PROP_ID_receivedStatus);
            
        }
    }
    
    /**
     * 发货进度: DELIVERY_STATUS
     */
    public final java.lang.String getDeliveryStatus(){
         onPropGet(PROP_ID_deliveryStatus);
         return _deliveryStatus;
    }

    /**
     * 发货进度: DELIVERY_STATUS
     */
    public final void setDeliveryStatus(java.lang.String value){
        if(onPropSet(PROP_ID_deliveryStatus,value)){
            this._deliveryStatus = value;
            internalClearRefs(PROP_ID_deliveryStatus);
            
        }
    }
    
    /**
     * 已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.sql.Timestamp getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 审核人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 审核人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 审核时间: APPROVED_AT
     */
    public final java.sql.Timestamp getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 审核时间: APPROVED_AT
     */
    public final void setApprovedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
        }
    }
    
    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final java.lang.Long getDelVersion(){
         onPropGet(PROP_ID_delVersion);
         return _delVersion;
    }

    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final void setDelVersion(java.lang.Long value){
        if(onPropSet(PROP_ID_delVersion,value)){
            this._delVersion = value;
            internalClearRefs(PROP_ID_delVersion);
            
        }
    }
    
    /**
     * 数据版本: VERSION
     */
    public final java.lang.Integer getVersion(){
         onPropGet(PROP_ID_version);
         return _version;
    }

    /**
     * 数据版本: VERSION
     */
    public final void setVersion(java.lang.Integer value){
        if(onPropSet(PROP_ID_version,value)){
            this._version = value;
            internalClearRefs(PROP_ID_version);
            
        }
    }
    
    /**
     * 创建人: CREATED_BY
     */
    public final java.lang.String getCreatedBy(){
         onPropGet(PROP_ID_createdBy);
         return _createdBy;
    }

    /**
     * 创建人: CREATED_BY
     */
    public final void setCreatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_createdBy,value)){
            this._createdBy = value;
            internalClearRefs(PROP_ID_createdBy);
            
        }
    }
    
    /**
     * 创建时间: CREATE_TIME
     */
    public final java.sql.Timestamp getCreateTime(){
         onPropGet(PROP_ID_createTime);
         return _createTime;
    }

    /**
     * 创建时间: CREATE_TIME
     */
    public final void setCreateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_createTime,value)){
            this._createTime = value;
            internalClearRefs(PROP_ID_createTime);
            
        }
    }
    
    /**
     * 修改人: UPDATED_BY
     */
    public final java.lang.String getUpdatedBy(){
         onPropGet(PROP_ID_updatedBy);
         return _updatedBy;
    }

    /**
     * 修改人: UPDATED_BY
     */
    public final void setUpdatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_updatedBy,value)){
            this._updatedBy = value;
            internalClearRefs(PROP_ID_updatedBy);
            
        }
    }
    
    /**
     * 修改时间: UPDATE_TIME
     */
    public final java.sql.Timestamp getUpdateTime(){
         onPropGet(PROP_ID_updateTime);
         return _updateTime;
    }

    /**
     * 修改时间: UPDATE_TIME
     */
    public final void setUpdateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_updateTime,value)){
            this._updateTime = value;
            internalClearRefs(PROP_ID_updateTime);
            
        }
    }
    
    /**
     * 备注: REMARK
     */
    public final java.lang.String getRemark(){
         onPropGet(PROP_ID_remark);
         return _remark;
    }

    /**
     * 备注: REMARK
     */
    public final void setRemark(java.lang.String value){
        if(onPropSet(PROP_ID_remark,value)){
            this._remark = value;
            internalClearRefs(PROP_ID_remark);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getCustomer(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_customer);
    }

    public final void setCustomer(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setCustomerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_customer, refEntity,()->{
           
                           this.setCustomerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdWarehouse getWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_warehouse);
    }

    public final void setWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_warehouse, refEntity,()->{
           
                           this.setWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdSettlementMethod getSettlementMethod(){
       return (app.erp.md.dao.entity.ErpMdSettlementMethod)internalGetRefEntity(PROP_NAME_settlementMethod);
    }

    public final void setSettlementMethod(app.erp.md.dao.entity.ErpMdSettlementMethod refEntity){
   
           if(refEntity == null){
           
                   this.setSettlementMethodId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_settlementMethod, refEntity,()->{
           
                           this.setSettlementMethodId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.sal.dao.entity.ErpSalOrderLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.sal.dao.entity.ErpSalOrderLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.sal.dao.entity.ErpSalOrderLine> getLines(){
       return _lines;
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_org);
    }

    public final void setOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_org, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.sal.dao.entity.ErpSalQuotation getQuotation(){
       return (app.erp.sal.dao.entity.ErpSalQuotation)internalGetRefEntity(PROP_NAME_quotation);
    }

    public final void setQuotation(app.erp.sal.dao.entity.ErpSalQuotation refEntity){
   
           if(refEntity == null){
           
                   this.setQuotationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_quotation, refEntity,()->{
           
                           this.setQuotationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.sal.dao.entity.ErpSalContract getContract(){
       return (app.erp.sal.dao.entity.ErpSalContract)internalGetRefEntity(PROP_NAME_contract);
    }

    public final void setContract(app.erp.sal.dao.entity.ErpSalContract refEntity){
   
           if(refEntity == null){
           
                   this.setContractId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contract, refEntity,()->{
           
                           this.setContractId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
