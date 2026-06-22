package app.erp.fin.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.fin.dao.entity.ErpFinVoucherBillR;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  业财回链: erp_fin_voucher_bill_r
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinVoucherBillR extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 凭证ID: VOUCHER_ID BIGINT */
    public static final String PROP_NAME_voucherId = "voucherId";
    public static final int PROP_ID_voucherId = 2;
    
    /* 单据类型: BILL_TYPE VARCHAR */
    public static final String PROP_NAME_billType = "billType";
    public static final int PROP_ID_billType = 3;
    
    /* 单据编号: BILL_CODE VARCHAR */
    public static final String PROP_NAME_billCode = "billCode";
    public static final int PROP_ID_billCode = 4;
    
    /* 单据行号: BILL_LINE_CODE VARCHAR */
    public static final String PROP_NAME_billLineCode = "billLineCode";
    public static final int PROP_ID_billLineCode = 5;
    
    /* 业务类型: BUSINESS_TYPE INTEGER */
    public static final String PROP_NAME_businessType = "businessType";
    public static final int PROP_ID_businessType = 6;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 7;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 8;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 9;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 10;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 11;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 12;
    

    private static int _PROP_ID_BOUND = 13;

    
    /* relation:  */
    public static final String PROP_NAME_voucher = "voucher";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[13];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_voucherId] = PROP_NAME_voucherId;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherId, PROP_ID_voucherId);
      
          PROP_ID_TO_NAME[PROP_ID_billType] = PROP_NAME_billType;
          PROP_NAME_TO_ID.put(PROP_NAME_billType, PROP_ID_billType);
      
          PROP_ID_TO_NAME[PROP_ID_billCode] = PROP_NAME_billCode;
          PROP_NAME_TO_ID.put(PROP_NAME_billCode, PROP_ID_billCode);
      
          PROP_ID_TO_NAME[PROP_ID_billLineCode] = PROP_NAME_billLineCode;
          PROP_NAME_TO_ID.put(PROP_NAME_billLineCode, PROP_ID_billLineCode);
      
          PROP_ID_TO_NAME[PROP_ID_businessType] = PROP_NAME_businessType;
          PROP_NAME_TO_ID.put(PROP_NAME_businessType, PROP_ID_businessType);
      
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
    
    /* 凭证ID: VOUCHER_ID */
    private java.lang.Long _voucherId;
    
    /* 单据类型: BILL_TYPE */
    private java.lang.String _billType;
    
    /* 单据编号: BILL_CODE */
    private java.lang.String _billCode;
    
    /* 单据行号: BILL_LINE_CODE */
    private java.lang.String _billLineCode;
    
    /* 业务类型: BUSINESS_TYPE */
    private java.lang.Integer _businessType;
    
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
    

    public _ErpFinVoucherBillR(){
        // for debug
    }

    protected ErpFinVoucherBillR newInstance(){
        ErpFinVoucherBillR entity = new ErpFinVoucherBillR();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinVoucherBillR cloneInstance() {
        ErpFinVoucherBillR entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinVoucherBillR";
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
        
            case PROP_ID_voucherId:
               return getVoucherId();
        
            case PROP_ID_billType:
               return getBillType();
        
            case PROP_ID_billCode:
               return getBillCode();
        
            case PROP_ID_billLineCode:
               return getBillLineCode();
        
            case PROP_ID_businessType:
               return getBusinessType();
        
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
        
            case PROP_ID_voucherId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_voucherId));
               }
               setVoucherId(typedValue);
               break;
            }
        
            case PROP_ID_billType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_billType));
               }
               setBillType(typedValue);
               break;
            }
        
            case PROP_ID_billCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_billCode));
               }
               setBillCode(typedValue);
               break;
            }
        
            case PROP_ID_billLineCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_billLineCode));
               }
               setBillLineCode(typedValue);
               break;
            }
        
            case PROP_ID_businessType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_businessType));
               }
               setBusinessType(typedValue);
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
        
            case PROP_ID_voucherId:{
               onInitProp(propId);
               this._voucherId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_billType:{
               onInitProp(propId);
               this._billType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_billCode:{
               onInitProp(propId);
               this._billCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_billLineCode:{
               onInitProp(propId);
               this._billLineCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_businessType:{
               onInitProp(propId);
               this._businessType = (java.lang.Integer)value;
               
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
     * 凭证ID: VOUCHER_ID
     */
    public final java.lang.Long getVoucherId(){
         onPropGet(PROP_ID_voucherId);
         return _voucherId;
    }

    /**
     * 凭证ID: VOUCHER_ID
     */
    public final void setVoucherId(java.lang.Long value){
        if(onPropSet(PROP_ID_voucherId,value)){
            this._voucherId = value;
            internalClearRefs(PROP_ID_voucherId);
            
        }
    }
    
    /**
     * 单据类型: BILL_TYPE
     */
    public final java.lang.String getBillType(){
         onPropGet(PROP_ID_billType);
         return _billType;
    }

    /**
     * 单据类型: BILL_TYPE
     */
    public final void setBillType(java.lang.String value){
        if(onPropSet(PROP_ID_billType,value)){
            this._billType = value;
            internalClearRefs(PROP_ID_billType);
            
        }
    }
    
    /**
     * 单据编号: BILL_CODE
     */
    public final java.lang.String getBillCode(){
         onPropGet(PROP_ID_billCode);
         return _billCode;
    }

    /**
     * 单据编号: BILL_CODE
     */
    public final void setBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_billCode,value)){
            this._billCode = value;
            internalClearRefs(PROP_ID_billCode);
            
        }
    }
    
    /**
     * 单据行号: BILL_LINE_CODE
     */
    public final java.lang.String getBillLineCode(){
         onPropGet(PROP_ID_billLineCode);
         return _billLineCode;
    }

    /**
     * 单据行号: BILL_LINE_CODE
     */
    public final void setBillLineCode(java.lang.String value){
        if(onPropSet(PROP_ID_billLineCode,value)){
            this._billLineCode = value;
            internalClearRefs(PROP_ID_billLineCode);
            
        }
    }
    
    /**
     * 业务类型: BUSINESS_TYPE
     */
    public final java.lang.Integer getBusinessType(){
         onPropGet(PROP_ID_businessType);
         return _businessType;
    }

    /**
     * 业务类型: BUSINESS_TYPE
     */
    public final void setBusinessType(java.lang.Integer value){
        if(onPropSet(PROP_ID_businessType,value)){
            this._businessType = value;
            internalClearRefs(PROP_ID_businessType);
            
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
    public final app.erp.fin.dao.entity.ErpFinVoucher getVoucher(){
       return (app.erp.fin.dao.entity.ErpFinVoucher)internalGetRefEntity(PROP_NAME_voucher);
    }

    public final void setVoucher(app.erp.fin.dao.entity.ErpFinVoucher refEntity){
   
           if(refEntity == null){
           
                   this.setVoucherId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_voucher, refEntity,()->{
           
                           this.setVoucherId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
