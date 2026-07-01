package app.erp.mfg.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  委外加工单: erp_mfg_subcontract_order
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgSubcontractOrder extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 来源工单: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 4;
    
    /* 委外供应商: SUPPLIER_ID BIGINT */
    public static final String PROP_NAME_supplierId = "supplierId";
    public static final int PROP_ID_supplierId = 5;
    
    /* 外协工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 6;
    
    /* 工艺路线: ROUTING_ID BIGINT */
    public static final String PROP_NAME_routingId = "routingId";
    public static final int PROP_ID_routingId = 7;
    
    /* 生产版本: PRODUCTION_VERSION_ID BIGINT */
    public static final String PROP_NAME_productionVersionId = "productionVersionId";
    public static final int PROP_ID_productionVersionId = 8;
    
    /* 产品: PRODUCT_ID BIGINT */
    public static final String PROP_NAME_productId = "productId";
    public static final int PROP_ID_productId = 9;
    
    /* 委外日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 10;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 11;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 12;
    
    /* 加工费: PROCESSING_FEE DECIMAL */
    public static final String PROP_NAME_processingFee = "processingFee";
    public static final int PROP_ID_processingFee = 13;
    
    /* 合计金额: TOTAL_AMOUNT DECIMAL */
    public static final String PROP_NAME_totalAmount = "totalAmount";
    public static final int PROP_ID_totalAmount = 14;
    
    /* 单据状态: DOC_STATUS INTEGER */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 15;
    
    /* 审核状态: APPROVE_STATUS INTEGER */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 16;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 17;
    
    /* 过账状态: POSTED_STATUS VARCHAR */
    public static final String PROP_NAME_postedStatus = "postedStatus";
    public static final int PROP_ID_postedStatus = 18;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 19;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 20;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 21;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 22;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 23;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 24;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 25;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 26;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 27;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 28;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_workOrder = "workOrder";
    
    /* relation:  */
    public static final String PROP_NAME_supplier = "supplier";
    
    /* relation:  */
    public static final String PROP_NAME_workcenter = "workcenter";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_routing = "routing";
    
    /* relation:  */
    public static final String PROP_NAME_productionVersion = "productionVersion";
    
    /* relation:  */
    public static final String PROP_NAME_product = "product";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_supplierId] = PROP_NAME_supplierId;
          PROP_NAME_TO_ID.put(PROP_NAME_supplierId, PROP_ID_supplierId);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_routingId] = PROP_NAME_routingId;
          PROP_NAME_TO_ID.put(PROP_NAME_routingId, PROP_ID_routingId);
      
          PROP_ID_TO_NAME[PROP_ID_productionVersionId] = PROP_NAME_productionVersionId;
          PROP_NAME_TO_ID.put(PROP_NAME_productionVersionId, PROP_ID_productionVersionId);
      
          PROP_ID_TO_NAME[PROP_ID_productId] = PROP_NAME_productId;
          PROP_NAME_TO_ID.put(PROP_NAME_productId, PROP_ID_productId);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_processingFee] = PROP_NAME_processingFee;
          PROP_NAME_TO_ID.put(PROP_NAME_processingFee, PROP_ID_processingFee);
      
          PROP_ID_TO_NAME[PROP_ID_totalAmount] = PROP_NAME_totalAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_totalAmount, PROP_ID_totalAmount);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedStatus] = PROP_NAME_postedStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_postedStatus, PROP_ID_postedStatus);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
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
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 来源工单: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 委外供应商: SUPPLIER_ID */
    private java.lang.Long _supplierId;
    
    /* 外协工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 工艺路线: ROUTING_ID */
    private java.lang.Long _routingId;
    
    /* 生产版本: PRODUCTION_VERSION_ID */
    private java.lang.Long _productionVersionId;
    
    /* 产品: PRODUCT_ID */
    private java.lang.Long _productId;
    
    /* 委外日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.lang.String _exchangeRate;
    
    /* 加工费: PROCESSING_FEE */
    private java.lang.String _processingFee;
    
    /* 合计金额: TOTAL_AMOUNT */
    private java.lang.String _totalAmount;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.Integer _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.Integer _approveStatus;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账状态: POSTED_STATUS */
    private java.lang.String _postedStatus;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.lang.String _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.lang.String _amountFunctional;
    
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
    

    public _ErpMfgSubcontractOrder(){
        // for debug
    }

    protected ErpMfgSubcontractOrder newInstance(){
        ErpMfgSubcontractOrder entity = new ErpMfgSubcontractOrder();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgSubcontractOrder cloneInstance() {
        ErpMfgSubcontractOrder entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgSubcontractOrder";
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
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_supplierId:
               return getSupplierId();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_routingId:
               return getRoutingId();
        
            case PROP_ID_productionVersionId:
               return getProductionVersionId();
        
            case PROP_ID_productId:
               return getProductId();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_processingFee:
               return getProcessingFee();
        
            case PROP_ID_totalAmount:
               return getTotalAmount();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedStatus:
               return getPostedStatus();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
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
        
            case PROP_ID_workOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workOrderId));
               }
               setWorkOrderId(typedValue);
               break;
            }
        
            case PROP_ID_supplierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_supplierId));
               }
               setSupplierId(typedValue);
               break;
            }
        
            case PROP_ID_workcenterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workcenterId));
               }
               setWorkcenterId(typedValue);
               break;
            }
        
            case PROP_ID_routingId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_routingId));
               }
               setRoutingId(typedValue);
               break;
            }
        
            case PROP_ID_productionVersionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_productionVersionId));
               }
               setProductionVersionId(typedValue);
               break;
            }
        
            case PROP_ID_productId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_productId));
               }
               setProductId(typedValue);
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
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_processingFee:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_processingFee));
               }
               setProcessingFee(typedValue);
               break;
            }
        
            case PROP_ID_totalAmount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalAmount));
               }
               setTotalAmount(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
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
        
            case PROP_ID_postedStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedStatus));
               }
               setPostedStatus(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
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
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_supplierId:{
               onInitProp(propId);
               this._supplierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_routingId:{
               onInitProp(propId);
               this._routingId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_productionVersionId:{
               onInitProp(propId);
               this._productionVersionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_productId:{
               onInitProp(propId);
               this._productId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_processingFee:{
               onInitProp(propId);
               this._processingFee = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalAmount:{
               onInitProp(propId);
               this._totalAmount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedStatus:{
               onInitProp(propId);
               this._postedStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.lang.String)value;
               
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
     * 来源工单: WORK_ORDER_ID
     */
    public final java.lang.Long getWorkOrderId(){
         onPropGet(PROP_ID_workOrderId);
         return _workOrderId;
    }

    /**
     * 来源工单: WORK_ORDER_ID
     */
    public final void setWorkOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_workOrderId,value)){
            this._workOrderId = value;
            internalClearRefs(PROP_ID_workOrderId);
            
        }
    }
    
    /**
     * 委外供应商: SUPPLIER_ID
     */
    public final java.lang.Long getSupplierId(){
         onPropGet(PROP_ID_supplierId);
         return _supplierId;
    }

    /**
     * 委外供应商: SUPPLIER_ID
     */
    public final void setSupplierId(java.lang.Long value){
        if(onPropSet(PROP_ID_supplierId,value)){
            this._supplierId = value;
            internalClearRefs(PROP_ID_supplierId);
            
        }
    }
    
    /**
     * 外协工作中心: WORKCENTER_ID
     */
    public final java.lang.Long getWorkcenterId(){
         onPropGet(PROP_ID_workcenterId);
         return _workcenterId;
    }

    /**
     * 外协工作中心: WORKCENTER_ID
     */
    public final void setWorkcenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_workcenterId,value)){
            this._workcenterId = value;
            internalClearRefs(PROP_ID_workcenterId);
            
        }
    }
    
    /**
     * 工艺路线: ROUTING_ID
     */
    public final java.lang.Long getRoutingId(){
         onPropGet(PROP_ID_routingId);
         return _routingId;
    }

    /**
     * 工艺路线: ROUTING_ID
     */
    public final void setRoutingId(java.lang.Long value){
        if(onPropSet(PROP_ID_routingId,value)){
            this._routingId = value;
            internalClearRefs(PROP_ID_routingId);
            
        }
    }
    
    /**
     * 生产版本: PRODUCTION_VERSION_ID
     */
    public final java.lang.Long getProductionVersionId(){
         onPropGet(PROP_ID_productionVersionId);
         return _productionVersionId;
    }

    /**
     * 生产版本: PRODUCTION_VERSION_ID
     */
    public final void setProductionVersionId(java.lang.Long value){
        if(onPropSet(PROP_ID_productionVersionId,value)){
            this._productionVersionId = value;
            internalClearRefs(PROP_ID_productionVersionId);
            
        }
    }
    
    /**
     * 产品: PRODUCT_ID
     */
    public final java.lang.Long getProductId(){
         onPropGet(PROP_ID_productId);
         return _productId;
    }

    /**
     * 产品: PRODUCT_ID
     */
    public final void setProductId(java.lang.Long value){
        if(onPropSet(PROP_ID_productId,value)){
            this._productId = value;
            internalClearRefs(PROP_ID_productId);
            
        }
    }
    
    /**
     * 委外日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 委外日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
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
    public final java.lang.String getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.lang.String value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 加工费: PROCESSING_FEE
     */
    public final java.lang.String getProcessingFee(){
         onPropGet(PROP_ID_processingFee);
         return _processingFee;
    }

    /**
     * 加工费: PROCESSING_FEE
     */
    public final void setProcessingFee(java.lang.String value){
        if(onPropSet(PROP_ID_processingFee,value)){
            this._processingFee = value;
            internalClearRefs(PROP_ID_processingFee);
            
        }
    }
    
    /**
     * 合计金额: TOTAL_AMOUNT
     */
    public final java.lang.String getTotalAmount(){
         onPropGet(PROP_ID_totalAmount);
         return _totalAmount;
    }

    /**
     * 合计金额: TOTAL_AMOUNT
     */
    public final void setTotalAmount(java.lang.String value){
        if(onPropSet(PROP_ID_totalAmount,value)){
            this._totalAmount = value;
            internalClearRefs(PROP_ID_totalAmount);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.Integer getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.Integer getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
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
     * 过账状态: POSTED_STATUS
     */
    public final java.lang.String getPostedStatus(){
         onPropGet(PROP_ID_postedStatus);
         return _postedStatus;
    }

    /**
     * 过账状态: POSTED_STATUS
     */
    public final void setPostedStatus(java.lang.String value){
        if(onPropSet(PROP_ID_postedStatus,value)){
            this._postedStatus = value;
            internalClearRefs(PROP_ID_postedStatus);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.time.LocalDateTime getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.time.LocalDateTime value){
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
     * 源币种金额: AMOUNT_SOURCE
     */
    public final java.lang.String getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.lang.String value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final java.lang.String getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.lang.String value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
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
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgWorkOrder getWorkOrder(){
       return (app.erp.mfg.dao.entity.ErpMfgWorkOrder)internalGetRefEntity(PROP_NAME_workOrder);
    }

    public final void setWorkOrder(app.erp.mfg.dao.entity.ErpMfgWorkOrder refEntity){
   
           if(refEntity == null){
           
                   this.setWorkOrderId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_workOrder, refEntity,()->{
           
                           this.setWorkOrderId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getSupplier(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_supplier);
    }

    public final void setSupplier(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setSupplierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_supplier, refEntity,()->{
           
                           this.setSupplierId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgWorkcenter getWorkcenter(){
       return (app.erp.mfg.dao.entity.ErpMfgWorkcenter)internalGetRefEntity(PROP_NAME_workcenter);
    }

    public final void setWorkcenter(app.erp.mfg.dao.entity.ErpMfgWorkcenter refEntity){
   
           if(refEntity == null){
           
                   this.setWorkcenterId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_workcenter, refEntity,()->{
           
                           this.setWorkcenterId(refEntity.getId());
                       
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
       
    private final OrmEntitySet<app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine> getLines(){
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
    public final app.erp.mfg.dao.entity.ErpMfgRouting getRouting(){
       return (app.erp.mfg.dao.entity.ErpMfgRouting)internalGetRefEntity(PROP_NAME_routing);
    }

    public final void setRouting(app.erp.mfg.dao.entity.ErpMfgRouting refEntity){
   
           if(refEntity == null){
           
                   this.setRoutingId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_routing, refEntity,()->{
           
                           this.setRoutingId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgProductionVersion getProductionVersion(){
       return (app.erp.mfg.dao.entity.ErpMfgProductionVersion)internalGetRefEntity(PROP_NAME_productionVersion);
    }

    public final void setProductionVersion(app.erp.mfg.dao.entity.ErpMfgProductionVersion refEntity){
   
           if(refEntity == null){
           
                   this.setProductionVersionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_productionVersion, refEntity,()->{
           
                           this.setProductionVersionId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getProduct(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_product);
    }

    public final void setProduct(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setProductId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_product, refEntity,()->{
           
                           this.setProductId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
