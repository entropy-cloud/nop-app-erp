package app.erp.contract.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.contract.dao.entity.ErpCtInvoicePlan;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  开票计划: erp_ct_invoice_plan
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCtInvoicePlan extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 合同行ID: CONTRACT_LINE_ID BIGINT */
    public static final String PROP_NAME_contractLineId = "contractLineId";
    public static final int PROP_ID_contractLineId = 2;
    
    /* 计划开票日期: PLAN_DATE DATE */
    public static final String PROP_NAME_planDate = "planDate";
    public static final int PROP_ID_planDate = 3;
    
    /* 开票金额: AMOUNT DECIMAL */
    public static final String PROP_NAME_amount = "amount";
    public static final int PROP_ID_amount = 4;
    
    /* 是否已开票: IS_INVOICED BOOLEAN */
    public static final String PROP_NAME_isInvoiced = "isInvoiced";
    public static final int PROP_ID_isInvoiced = 5;
    
    /* 关联发票号: INVOICE_BILL_CODE VARCHAR */
    public static final String PROP_NAME_invoiceBillCode = "invoiceBillCode";
    public static final int PROP_ID_invoiceBillCode = 6;
    
    /* 实际开票日期: INVOICE_DATE DATE */
    public static final String PROP_NAME_invoiceDate = "invoiceDate";
    public static final int PROP_ID_invoiceDate = 7;
    
    /* 开票条款: INVOICE_TERM INTEGER */
    public static final String PROP_NAME_invoiceTerm = "invoiceTerm";
    public static final int PROP_ID_invoiceTerm = 8;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 9;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 10;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 11;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 12;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 13;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 14;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 15;
    

    private static int _PROP_ID_BOUND = 16;

    
    /* relation:  */
    public static final String PROP_NAME_contractLine = "contractLine";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[16];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_contractLineId] = PROP_NAME_contractLineId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractLineId, PROP_ID_contractLineId);
      
          PROP_ID_TO_NAME[PROP_ID_planDate] = PROP_NAME_planDate;
          PROP_NAME_TO_ID.put(PROP_NAME_planDate, PROP_ID_planDate);
      
          PROP_ID_TO_NAME[PROP_ID_amount] = PROP_NAME_amount;
          PROP_NAME_TO_ID.put(PROP_NAME_amount, PROP_ID_amount);
      
          PROP_ID_TO_NAME[PROP_ID_isInvoiced] = PROP_NAME_isInvoiced;
          PROP_NAME_TO_ID.put(PROP_NAME_isInvoiced, PROP_ID_isInvoiced);
      
          PROP_ID_TO_NAME[PROP_ID_invoiceBillCode] = PROP_NAME_invoiceBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_invoiceBillCode, PROP_ID_invoiceBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_invoiceDate] = PROP_NAME_invoiceDate;
          PROP_NAME_TO_ID.put(PROP_NAME_invoiceDate, PROP_ID_invoiceDate);
      
          PROP_ID_TO_NAME[PROP_ID_invoiceTerm] = PROP_NAME_invoiceTerm;
          PROP_NAME_TO_ID.put(PROP_NAME_invoiceTerm, PROP_ID_invoiceTerm);
      
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
    
    /* 合同行ID: CONTRACT_LINE_ID */
    private java.lang.Long _contractLineId;
    
    /* 计划开票日期: PLAN_DATE */
    private java.time.LocalDate _planDate;
    
    /* 开票金额: AMOUNT */
    private java.math.BigDecimal _amount;
    
    /* 是否已开票: IS_INVOICED */
    private java.lang.Boolean _isInvoiced;
    
    /* 关联发票号: INVOICE_BILL_CODE */
    private java.lang.String _invoiceBillCode;
    
    /* 实际开票日期: INVOICE_DATE */
    private java.time.LocalDate _invoiceDate;
    
    /* 开票条款: INVOICE_TERM */
    private java.lang.Integer _invoiceTerm;
    
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
    

    public _ErpCtInvoicePlan(){
        // for debug
    }

    protected ErpCtInvoicePlan newInstance(){
        ErpCtInvoicePlan entity = new ErpCtInvoicePlan();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCtInvoicePlan cloneInstance() {
        ErpCtInvoicePlan entity = newInstance();
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
      return "app.erp.contract.dao.entity.ErpCtInvoicePlan";
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
        
            case PROP_ID_contractLineId:
               return getContractLineId();
        
            case PROP_ID_planDate:
               return getPlanDate();
        
            case PROP_ID_amount:
               return getAmount();
        
            case PROP_ID_isInvoiced:
               return getIsInvoiced();
        
            case PROP_ID_invoiceBillCode:
               return getInvoiceBillCode();
        
            case PROP_ID_invoiceDate:
               return getInvoiceDate();
        
            case PROP_ID_invoiceTerm:
               return getInvoiceTerm();
        
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
        
            case PROP_ID_contractLineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractLineId));
               }
               setContractLineId(typedValue);
               break;
            }
        
            case PROP_ID_planDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_planDate));
               }
               setPlanDate(typedValue);
               break;
            }
        
            case PROP_ID_amount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amount));
               }
               setAmount(typedValue);
               break;
            }
        
            case PROP_ID_isInvoiced:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isInvoiced));
               }
               setIsInvoiced(typedValue);
               break;
            }
        
            case PROP_ID_invoiceBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_invoiceBillCode));
               }
               setInvoiceBillCode(typedValue);
               break;
            }
        
            case PROP_ID_invoiceDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_invoiceDate));
               }
               setInvoiceDate(typedValue);
               break;
            }
        
            case PROP_ID_invoiceTerm:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_invoiceTerm));
               }
               setInvoiceTerm(typedValue);
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
        
            case PROP_ID_contractLineId:{
               onInitProp(propId);
               this._contractLineId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_planDate:{
               onInitProp(propId);
               this._planDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_amount:{
               onInitProp(propId);
               this._amount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_isInvoiced:{
               onInitProp(propId);
               this._isInvoiced = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_invoiceBillCode:{
               onInitProp(propId);
               this._invoiceBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_invoiceDate:{
               onInitProp(propId);
               this._invoiceDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_invoiceTerm:{
               onInitProp(propId);
               this._invoiceTerm = (java.lang.Integer)value;
               
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
     * 合同行ID: CONTRACT_LINE_ID
     */
    public final java.lang.Long getContractLineId(){
         onPropGet(PROP_ID_contractLineId);
         return _contractLineId;
    }

    /**
     * 合同行ID: CONTRACT_LINE_ID
     */
    public final void setContractLineId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractLineId,value)){
            this._contractLineId = value;
            internalClearRefs(PROP_ID_contractLineId);
            
        }
    }
    
    /**
     * 计划开票日期: PLAN_DATE
     */
    public final java.time.LocalDate getPlanDate(){
         onPropGet(PROP_ID_planDate);
         return _planDate;
    }

    /**
     * 计划开票日期: PLAN_DATE
     */
    public final void setPlanDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_planDate,value)){
            this._planDate = value;
            internalClearRefs(PROP_ID_planDate);
            
        }
    }
    
    /**
     * 开票金额: AMOUNT
     */
    public final java.math.BigDecimal getAmount(){
         onPropGet(PROP_ID_amount);
         return _amount;
    }

    /**
     * 开票金额: AMOUNT
     */
    public final void setAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amount,value)){
            this._amount = value;
            internalClearRefs(PROP_ID_amount);
            
        }
    }
    
    /**
     * 是否已开票: IS_INVOICED
     */
    public final java.lang.Boolean getIsInvoiced(){
         onPropGet(PROP_ID_isInvoiced);
         return _isInvoiced;
    }

    /**
     * 是否已开票: IS_INVOICED
     */
    public final void setIsInvoiced(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isInvoiced,value)){
            this._isInvoiced = value;
            internalClearRefs(PROP_ID_isInvoiced);
            
        }
    }
    
    /**
     * 关联发票号: INVOICE_BILL_CODE
     */
    public final java.lang.String getInvoiceBillCode(){
         onPropGet(PROP_ID_invoiceBillCode);
         return _invoiceBillCode;
    }

    /**
     * 关联发票号: INVOICE_BILL_CODE
     */
    public final void setInvoiceBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_invoiceBillCode,value)){
            this._invoiceBillCode = value;
            internalClearRefs(PROP_ID_invoiceBillCode);
            
        }
    }
    
    /**
     * 实际开票日期: INVOICE_DATE
     */
    public final java.time.LocalDate getInvoiceDate(){
         onPropGet(PROP_ID_invoiceDate);
         return _invoiceDate;
    }

    /**
     * 实际开票日期: INVOICE_DATE
     */
    public final void setInvoiceDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_invoiceDate,value)){
            this._invoiceDate = value;
            internalClearRefs(PROP_ID_invoiceDate);
            
        }
    }
    
    /**
     * 开票条款: INVOICE_TERM
     */
    public final java.lang.Integer getInvoiceTerm(){
         onPropGet(PROP_ID_invoiceTerm);
         return _invoiceTerm;
    }

    /**
     * 开票条款: INVOICE_TERM
     */
    public final void setInvoiceTerm(java.lang.Integer value){
        if(onPropSet(PROP_ID_invoiceTerm,value)){
            this._invoiceTerm = value;
            internalClearRefs(PROP_ID_invoiceTerm);
            
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
    public final app.erp.contract.dao.entity.ErpCtContractLine getContractLine(){
       return (app.erp.contract.dao.entity.ErpCtContractLine)internalGetRefEntity(PROP_NAME_contractLine);
    }

    public final void setContractLine(app.erp.contract.dao.entity.ErpCtContractLine refEntity){
   
           if(refEntity == null){
           
                   this.setContractLineId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contractLine, refEntity,()->{
           
                           this.setContractLineId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
