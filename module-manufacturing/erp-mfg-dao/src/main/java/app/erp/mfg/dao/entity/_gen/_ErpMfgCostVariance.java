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

import app.erp.mfg.dao.entity.ErpMfgCostVariance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  成本差异记录: erp_mfg_cost_variance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgCostVariance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工单: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 差异类型: VARIANCE_TYPE VARCHAR */
    public static final String PROP_NAME_varianceType = "varianceType";
    public static final int PROP_ID_varianceType = 4;
    
    /* 成本要素: COST_ELEMENT VARCHAR */
    public static final String PROP_NAME_costElement = "costElement";
    public static final int PROP_ID_costElement = 5;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 6;
    
    /* 工序: OPERATION_ID BIGINT */
    public static final String PROP_NAME_operationId = "operationId";
    public static final int PROP_ID_operationId = 7;
    
    /* 标准金额: STANDARD_AMOUNT DECIMAL */
    public static final String PROP_NAME_standardAmount = "standardAmount";
    public static final int PROP_ID_standardAmount = 8;
    
    /* 实际金额: ACTUAL_AMOUNT DECIMAL */
    public static final String PROP_NAME_actualAmount = "actualAmount";
    public static final int PROP_ID_actualAmount = 9;
    
    /* 差异金额: VARIANCE_AMOUNT DECIMAL */
    public static final String PROP_NAME_varianceAmount = "varianceAmount";
    public static final int PROP_ID_varianceAmount = 10;
    
    /* 差异百分比: VARIANCE_PERCENT DECIMAL */
    public static final String PROP_NAME_variancePercent = "variancePercent";
    public static final int PROP_ID_variancePercent = 11;
    
    /* 标准数量: STANDARD_QTY DECIMAL */
    public static final String PROP_NAME_standardQty = "standardQty";
    public static final int PROP_ID_standardQty = 12;
    
    /* 实际数量: ACTUAL_QTY DECIMAL */
    public static final String PROP_NAME_actualQty = "actualQty";
    public static final int PROP_ID_actualQty = 13;
    
    /* 标准单价: STANDARD_PRICE DECIMAL */
    public static final String PROP_NAME_standardPrice = "standardPrice";
    public static final int PROP_ID_standardPrice = 14;
    
    /* 实际单价: ACTUAL_PRICE DECIMAL */
    public static final String PROP_NAME_actualPrice = "actualPrice";
    public static final int PROP_ID_actualPrice = 15;
    
    /* 工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 16;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 17;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 18;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 19;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 20;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 21;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 22;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 23;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 24;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 25;
    

    private static int _PROP_ID_BOUND = 26;

    
    /* relation:  */
    public static final String PROP_NAME_workOrder = "workOrder";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_workcenter = "workcenter";
    
    /* relation:  */
    public static final String PROP_NAME_operation = "operation";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[26];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_varianceType] = PROP_NAME_varianceType;
          PROP_NAME_TO_ID.put(PROP_NAME_varianceType, PROP_ID_varianceType);
      
          PROP_ID_TO_NAME[PROP_ID_costElement] = PROP_NAME_costElement;
          PROP_NAME_TO_ID.put(PROP_NAME_costElement, PROP_ID_costElement);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_operationId] = PROP_NAME_operationId;
          PROP_NAME_TO_ID.put(PROP_NAME_operationId, PROP_ID_operationId);
      
          PROP_ID_TO_NAME[PROP_ID_standardAmount] = PROP_NAME_standardAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_standardAmount, PROP_ID_standardAmount);
      
          PROP_ID_TO_NAME[PROP_ID_actualAmount] = PROP_NAME_actualAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_actualAmount, PROP_ID_actualAmount);
      
          PROP_ID_TO_NAME[PROP_ID_varianceAmount] = PROP_NAME_varianceAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_varianceAmount, PROP_ID_varianceAmount);
      
          PROP_ID_TO_NAME[PROP_ID_variancePercent] = PROP_NAME_variancePercent;
          PROP_NAME_TO_ID.put(PROP_NAME_variancePercent, PROP_ID_variancePercent);
      
          PROP_ID_TO_NAME[PROP_ID_standardQty] = PROP_NAME_standardQty;
          PROP_NAME_TO_ID.put(PROP_NAME_standardQty, PROP_ID_standardQty);
      
          PROP_ID_TO_NAME[PROP_ID_actualQty] = PROP_NAME_actualQty;
          PROP_NAME_TO_ID.put(PROP_NAME_actualQty, PROP_ID_actualQty);
      
          PROP_ID_TO_NAME[PROP_ID_standardPrice] = PROP_NAME_standardPrice;
          PROP_NAME_TO_ID.put(PROP_NAME_standardPrice, PROP_ID_standardPrice);
      
          PROP_ID_TO_NAME[PROP_ID_actualPrice] = PROP_NAME_actualPrice;
          PROP_NAME_TO_ID.put(PROP_NAME_actualPrice, PROP_ID_actualPrice);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
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
    
    /* 工单: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 差异类型: VARIANCE_TYPE */
    private java.lang.String _varianceType;
    
    /* 成本要素: COST_ELEMENT */
    private java.lang.String _costElement;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 工序: OPERATION_ID */
    private java.lang.Long _operationId;
    
    /* 标准金额: STANDARD_AMOUNT */
    private java.math.BigDecimal _standardAmount;
    
    /* 实际金额: ACTUAL_AMOUNT */
    private java.math.BigDecimal _actualAmount;
    
    /* 差异金额: VARIANCE_AMOUNT */
    private java.math.BigDecimal _varianceAmount;
    
    /* 差异百分比: VARIANCE_PERCENT */
    private java.math.BigDecimal _variancePercent;
    
    /* 标准数量: STANDARD_QTY */
    private java.math.BigDecimal _standardQty;
    
    /* 实际数量: ACTUAL_QTY */
    private java.math.BigDecimal _actualQty;
    
    /* 标准单价: STANDARD_PRICE */
    private java.math.BigDecimal _standardPrice;
    
    /* 实际单价: ACTUAL_PRICE */
    private java.math.BigDecimal _actualPrice;
    
    /* 工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
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
    

    public _ErpMfgCostVariance(){
        // for debug
    }

    protected ErpMfgCostVariance newInstance(){
        ErpMfgCostVariance entity = new ErpMfgCostVariance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgCostVariance cloneInstance() {
        ErpMfgCostVariance entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgCostVariance";
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
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_varianceType:
               return getVarianceType();
        
            case PROP_ID_costElement:
               return getCostElement();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_operationId:
               return getOperationId();
        
            case PROP_ID_standardAmount:
               return getStandardAmount();
        
            case PROP_ID_actualAmount:
               return getActualAmount();
        
            case PROP_ID_varianceAmount:
               return getVarianceAmount();
        
            case PROP_ID_variancePercent:
               return getVariancePercent();
        
            case PROP_ID_standardQty:
               return getStandardQty();
        
            case PROP_ID_actualQty:
               return getActualQty();
        
            case PROP_ID_standardPrice:
               return getStandardPrice();
        
            case PROP_ID_actualPrice:
               return getActualPrice();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_remark:
               return getRemark();
        
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
        
            case PROP_ID_workOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workOrderId));
               }
               setWorkOrderId(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_varianceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_varianceType));
               }
               setVarianceType(typedValue);
               break;
            }
        
            case PROP_ID_costElement:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_costElement));
               }
               setCostElement(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_operationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_operationId));
               }
               setOperationId(typedValue);
               break;
            }
        
            case PROP_ID_standardAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_standardAmount));
               }
               setStandardAmount(typedValue);
               break;
            }
        
            case PROP_ID_actualAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_actualAmount));
               }
               setActualAmount(typedValue);
               break;
            }
        
            case PROP_ID_varianceAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_varianceAmount));
               }
               setVarianceAmount(typedValue);
               break;
            }
        
            case PROP_ID_variancePercent:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_variancePercent));
               }
               setVariancePercent(typedValue);
               break;
            }
        
            case PROP_ID_standardQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_standardQty));
               }
               setStandardQty(typedValue);
               break;
            }
        
            case PROP_ID_actualQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_actualQty));
               }
               setActualQty(typedValue);
               break;
            }
        
            case PROP_ID_standardPrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_standardPrice));
               }
               setStandardPrice(typedValue);
               break;
            }
        
            case PROP_ID_actualPrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_actualPrice));
               }
               setActualPrice(typedValue);
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
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_varianceType:{
               onInitProp(propId);
               this._varianceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_costElement:{
               onInitProp(propId);
               this._costElement = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_operationId:{
               onInitProp(propId);
               this._operationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_standardAmount:{
               onInitProp(propId);
               this._standardAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_actualAmount:{
               onInitProp(propId);
               this._actualAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_varianceAmount:{
               onInitProp(propId);
               this._varianceAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_variancePercent:{
               onInitProp(propId);
               this._variancePercent = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_standardQty:{
               onInitProp(propId);
               this._standardQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_actualQty:{
               onInitProp(propId);
               this._actualQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_standardPrice:{
               onInitProp(propId);
               this._standardPrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_actualPrice:{
               onInitProp(propId);
               this._actualPrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
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
     * 工单: WORK_ORDER_ID
     */
    public final java.lang.Long getWorkOrderId(){
         onPropGet(PROP_ID_workOrderId);
         return _workOrderId;
    }

    /**
     * 工单: WORK_ORDER_ID
     */
    public final void setWorkOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_workOrderId,value)){
            this._workOrderId = value;
            internalClearRefs(PROP_ID_workOrderId);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
        }
    }
    
    /**
     * 差异类型: VARIANCE_TYPE
     */
    public final java.lang.String getVarianceType(){
         onPropGet(PROP_ID_varianceType);
         return _varianceType;
    }

    /**
     * 差异类型: VARIANCE_TYPE
     */
    public final void setVarianceType(java.lang.String value){
        if(onPropSet(PROP_ID_varianceType,value)){
            this._varianceType = value;
            internalClearRefs(PROP_ID_varianceType);
            
        }
    }
    
    /**
     * 成本要素: COST_ELEMENT
     */
    public final java.lang.String getCostElement(){
         onPropGet(PROP_ID_costElement);
         return _costElement;
    }

    /**
     * 成本要素: COST_ELEMENT
     */
    public final void setCostElement(java.lang.String value){
        if(onPropSet(PROP_ID_costElement,value)){
            this._costElement = value;
            internalClearRefs(PROP_ID_costElement);
            
        }
    }
    
    /**
     * 物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 工序: OPERATION_ID
     */
    public final java.lang.Long getOperationId(){
         onPropGet(PROP_ID_operationId);
         return _operationId;
    }

    /**
     * 工序: OPERATION_ID
     */
    public final void setOperationId(java.lang.Long value){
        if(onPropSet(PROP_ID_operationId,value)){
            this._operationId = value;
            internalClearRefs(PROP_ID_operationId);
            
        }
    }
    
    /**
     * 标准金额: STANDARD_AMOUNT
     */
    public final java.math.BigDecimal getStandardAmount(){
         onPropGet(PROP_ID_standardAmount);
         return _standardAmount;
    }

    /**
     * 标准金额: STANDARD_AMOUNT
     */
    public final void setStandardAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_standardAmount,value)){
            this._standardAmount = value;
            internalClearRefs(PROP_ID_standardAmount);
            
        }
    }
    
    /**
     * 实际金额: ACTUAL_AMOUNT
     */
    public final java.math.BigDecimal getActualAmount(){
         onPropGet(PROP_ID_actualAmount);
         return _actualAmount;
    }

    /**
     * 实际金额: ACTUAL_AMOUNT
     */
    public final void setActualAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_actualAmount,value)){
            this._actualAmount = value;
            internalClearRefs(PROP_ID_actualAmount);
            
        }
    }
    
    /**
     * 差异金额: VARIANCE_AMOUNT
     */
    public final java.math.BigDecimal getVarianceAmount(){
         onPropGet(PROP_ID_varianceAmount);
         return _varianceAmount;
    }

    /**
     * 差异金额: VARIANCE_AMOUNT
     */
    public final void setVarianceAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_varianceAmount,value)){
            this._varianceAmount = value;
            internalClearRefs(PROP_ID_varianceAmount);
            
        }
    }
    
    /**
     * 差异百分比: VARIANCE_PERCENT
     */
    public final java.math.BigDecimal getVariancePercent(){
         onPropGet(PROP_ID_variancePercent);
         return _variancePercent;
    }

    /**
     * 差异百分比: VARIANCE_PERCENT
     */
    public final void setVariancePercent(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_variancePercent,value)){
            this._variancePercent = value;
            internalClearRefs(PROP_ID_variancePercent);
            
        }
    }
    
    /**
     * 标准数量: STANDARD_QTY
     */
    public final java.math.BigDecimal getStandardQty(){
         onPropGet(PROP_ID_standardQty);
         return _standardQty;
    }

    /**
     * 标准数量: STANDARD_QTY
     */
    public final void setStandardQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_standardQty,value)){
            this._standardQty = value;
            internalClearRefs(PROP_ID_standardQty);
            
        }
    }
    
    /**
     * 实际数量: ACTUAL_QTY
     */
    public final java.math.BigDecimal getActualQty(){
         onPropGet(PROP_ID_actualQty);
         return _actualQty;
    }

    /**
     * 实际数量: ACTUAL_QTY
     */
    public final void setActualQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_actualQty,value)){
            this._actualQty = value;
            internalClearRefs(PROP_ID_actualQty);
            
        }
    }
    
    /**
     * 标准单价: STANDARD_PRICE
     */
    public final java.math.BigDecimal getStandardPrice(){
         onPropGet(PROP_ID_standardPrice);
         return _standardPrice;
    }

    /**
     * 标准单价: STANDARD_PRICE
     */
    public final void setStandardPrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_standardPrice,value)){
            this._standardPrice = value;
            internalClearRefs(PROP_ID_standardPrice);
            
        }
    }
    
    /**
     * 实际单价: ACTUAL_PRICE
     */
    public final java.math.BigDecimal getActualPrice(){
         onPropGet(PROP_ID_actualPrice);
         return _actualPrice;
    }

    /**
     * 实际单价: ACTUAL_PRICE
     */
    public final void setActualPrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_actualPrice,value)){
            this._actualPrice = value;
            internalClearRefs(PROP_ID_actualPrice);
            
        }
    }
    
    /**
     * 工作中心: WORKCENTER_ID
     */
    public final java.lang.Long getWorkcenterId(){
         onPropGet(PROP_ID_workcenterId);
         return _workcenterId;
    }

    /**
     * 工作中心: WORKCENTER_ID
     */
    public final void setWorkcenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_workcenterId,value)){
            this._workcenterId = value;
            internalClearRefs(PROP_ID_workcenterId);
            
        }
    }
    
    /**
     * 业务日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 业务日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
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
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
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
    public final app.erp.mfg.dao.entity.ErpMfgBomOperation getOperation(){
       return (app.erp.mfg.dao.entity.ErpMfgBomOperation)internalGetRefEntity(PROP_NAME_operation);
    }

    public final void setOperation(app.erp.mfg.dao.entity.ErpMfgBomOperation refEntity){
   
           if(refEntity == null){
           
                   this.setOperationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_operation, refEntity,()->{
           
                           this.setOperationId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
