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

import app.erp.contract.dao.entity.ErpCtRebateTier;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  返利阶梯: erp_ct_rebate_tier
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCtRebateTier extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 返利协议: REBATE_AGREEMENT_ID BIGINT */
    public static final String PROP_NAME_rebateAgreementId = "rebateAgreementId";
    public static final int PROP_ID_rebateAgreementId = 2;
    
    /* 起始金额: FROM_AMOUNT DECIMAL */
    public static final String PROP_NAME_fromAmount = "fromAmount";
    public static final int PROP_ID_fromAmount = 3;
    
    /* 截止金额: TO_AMOUNT DECIMAL */
    public static final String PROP_NAME_toAmount = "toAmount";
    public static final int PROP_ID_toAmount = 4;
    
    /* 返利比例: REBATE_PERCENT DECIMAL */
    public static final String PROP_NAME_rebatePercent = "rebatePercent";
    public static final int PROP_ID_rebatePercent = 5;
    
    /* 固定返利金额: REBATE_AMOUNT DECIMAL */
    public static final String PROP_NAME_rebateAmount = "rebateAmount";
    public static final int PROP_ID_rebateAmount = 6;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 7;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 8;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 9;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 10;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 11;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 12;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 13;
    

    private static int _PROP_ID_BOUND = 14;

    
    /* relation:  */
    public static final String PROP_NAME_rebateAgreement = "rebateAgreement";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[14];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_rebateAgreementId] = PROP_NAME_rebateAgreementId;
          PROP_NAME_TO_ID.put(PROP_NAME_rebateAgreementId, PROP_ID_rebateAgreementId);
      
          PROP_ID_TO_NAME[PROP_ID_fromAmount] = PROP_NAME_fromAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_fromAmount, PROP_ID_fromAmount);
      
          PROP_ID_TO_NAME[PROP_ID_toAmount] = PROP_NAME_toAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_toAmount, PROP_ID_toAmount);
      
          PROP_ID_TO_NAME[PROP_ID_rebatePercent] = PROP_NAME_rebatePercent;
          PROP_NAME_TO_ID.put(PROP_NAME_rebatePercent, PROP_ID_rebatePercent);
      
          PROP_ID_TO_NAME[PROP_ID_rebateAmount] = PROP_NAME_rebateAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_rebateAmount, PROP_ID_rebateAmount);
      
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
    
    /* 返利协议: REBATE_AGREEMENT_ID */
    private java.lang.Long _rebateAgreementId;
    
    /* 起始金额: FROM_AMOUNT */
    private java.math.BigDecimal _fromAmount;
    
    /* 截止金额: TO_AMOUNT */
    private java.math.BigDecimal _toAmount;
    
    /* 返利比例: REBATE_PERCENT */
    private java.math.BigDecimal _rebatePercent;
    
    /* 固定返利金额: REBATE_AMOUNT */
    private java.math.BigDecimal _rebateAmount;
    
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
    

    public _ErpCtRebateTier(){
        // for debug
    }

    protected ErpCtRebateTier newInstance(){
        ErpCtRebateTier entity = new ErpCtRebateTier();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCtRebateTier cloneInstance() {
        ErpCtRebateTier entity = newInstance();
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
      return "app.erp.contract.dao.entity.ErpCtRebateTier";
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
        
            case PROP_ID_rebateAgreementId:
               return getRebateAgreementId();
        
            case PROP_ID_fromAmount:
               return getFromAmount();
        
            case PROP_ID_toAmount:
               return getToAmount();
        
            case PROP_ID_rebatePercent:
               return getRebatePercent();
        
            case PROP_ID_rebateAmount:
               return getRebateAmount();
        
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
        
            case PROP_ID_rebateAgreementId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_rebateAgreementId));
               }
               setRebateAgreementId(typedValue);
               break;
            }
        
            case PROP_ID_fromAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_fromAmount));
               }
               setFromAmount(typedValue);
               break;
            }
        
            case PROP_ID_toAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_toAmount));
               }
               setToAmount(typedValue);
               break;
            }
        
            case PROP_ID_rebatePercent:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_rebatePercent));
               }
               setRebatePercent(typedValue);
               break;
            }
        
            case PROP_ID_rebateAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_rebateAmount));
               }
               setRebateAmount(typedValue);
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
        
            case PROP_ID_rebateAgreementId:{
               onInitProp(propId);
               this._rebateAgreementId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fromAmount:{
               onInitProp(propId);
               this._fromAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_toAmount:{
               onInitProp(propId);
               this._toAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_rebatePercent:{
               onInitProp(propId);
               this._rebatePercent = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_rebateAmount:{
               onInitProp(propId);
               this._rebateAmount = (java.math.BigDecimal)value;
               
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
     * 返利协议: REBATE_AGREEMENT_ID
     */
    public final java.lang.Long getRebateAgreementId(){
         onPropGet(PROP_ID_rebateAgreementId);
         return _rebateAgreementId;
    }

    /**
     * 返利协议: REBATE_AGREEMENT_ID
     */
    public final void setRebateAgreementId(java.lang.Long value){
        if(onPropSet(PROP_ID_rebateAgreementId,value)){
            this._rebateAgreementId = value;
            internalClearRefs(PROP_ID_rebateAgreementId);
            
        }
    }
    
    /**
     * 起始金额: FROM_AMOUNT
     */
    public final java.math.BigDecimal getFromAmount(){
         onPropGet(PROP_ID_fromAmount);
         return _fromAmount;
    }

    /**
     * 起始金额: FROM_AMOUNT
     */
    public final void setFromAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_fromAmount,value)){
            this._fromAmount = value;
            internalClearRefs(PROP_ID_fromAmount);
            
        }
    }
    
    /**
     * 截止金额: TO_AMOUNT
     */
    public final java.math.BigDecimal getToAmount(){
         onPropGet(PROP_ID_toAmount);
         return _toAmount;
    }

    /**
     * 截止金额: TO_AMOUNT
     */
    public final void setToAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_toAmount,value)){
            this._toAmount = value;
            internalClearRefs(PROP_ID_toAmount);
            
        }
    }
    
    /**
     * 返利比例: REBATE_PERCENT
     */
    public final java.math.BigDecimal getRebatePercent(){
         onPropGet(PROP_ID_rebatePercent);
         return _rebatePercent;
    }

    /**
     * 返利比例: REBATE_PERCENT
     */
    public final void setRebatePercent(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_rebatePercent,value)){
            this._rebatePercent = value;
            internalClearRefs(PROP_ID_rebatePercent);
            
        }
    }
    
    /**
     * 固定返利金额: REBATE_AMOUNT
     */
    public final java.math.BigDecimal getRebateAmount(){
         onPropGet(PROP_ID_rebateAmount);
         return _rebateAmount;
    }

    /**
     * 固定返利金额: REBATE_AMOUNT
     */
    public final void setRebateAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_rebateAmount,value)){
            this._rebateAmount = value;
            internalClearRefs(PROP_ID_rebateAmount);
            
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
    public final app.erp.contract.dao.entity.ErpCtRebateAgreement getRebateAgreement(){
       return (app.erp.contract.dao.entity.ErpCtRebateAgreement)internalGetRefEntity(PROP_NAME_rebateAgreement);
    }

    public final void setRebateAgreement(app.erp.contract.dao.entity.ErpCtRebateAgreement refEntity){
   
           if(refEntity == null){
           
                   this.setRebateAgreementId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_rebateAgreement, refEntity,()->{
           
                           this.setRebateAgreementId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
