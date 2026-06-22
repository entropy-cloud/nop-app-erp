package app.erp.md.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.md.dao.entity.ErpMdBankAccount;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  银行账户: erp_md_bank_account
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdBankAccount extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 往来单位: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 2;
    
    /* 开户银行: BANK_NAME VARCHAR */
    public static final String PROP_NAME_bankName = "bankName";
    public static final int PROP_ID_bankName = 3;
    
    /* 支行: BANK_BRANCH VARCHAR */
    public static final String PROP_NAME_bankBranch = "bankBranch";
    public static final int PROP_ID_bankBranch = 4;
    
    /* 银行账号: BANK_ACCOUNT VARCHAR */
    public static final String PROP_NAME_bankAccount = "bankAccount";
    public static final int PROP_ID_bankAccount = 5;
    
    /* 账户类型: ACCOUNT_TYPE INTEGER */
    public static final String PROP_NAME_accountType = "accountType";
    public static final int PROP_ID_accountType = 6;
    
    /* 户名: ACCOUNT_HOLDER VARCHAR */
    public static final String PROP_NAME_accountHolder = "accountHolder";
    public static final int PROP_ID_accountHolder = 7;
    
    /* 是否默认: IS_DEFAULT BOOLEAN */
    public static final String PROP_NAME_isDefault = "isDefault";
    public static final int PROP_ID_isDefault = 8;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 9;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 10;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 11;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 12;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 13;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 14;
    

    private static int _PROP_ID_BOUND = 15;

    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[15];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_bankName] = PROP_NAME_bankName;
          PROP_NAME_TO_ID.put(PROP_NAME_bankName, PROP_ID_bankName);
      
          PROP_ID_TO_NAME[PROP_ID_bankBranch] = PROP_NAME_bankBranch;
          PROP_NAME_TO_ID.put(PROP_NAME_bankBranch, PROP_ID_bankBranch);
      
          PROP_ID_TO_NAME[PROP_ID_bankAccount] = PROP_NAME_bankAccount;
          PROP_NAME_TO_ID.put(PROP_NAME_bankAccount, PROP_ID_bankAccount);
      
          PROP_ID_TO_NAME[PROP_ID_accountType] = PROP_NAME_accountType;
          PROP_NAME_TO_ID.put(PROP_NAME_accountType, PROP_ID_accountType);
      
          PROP_ID_TO_NAME[PROP_ID_accountHolder] = PROP_NAME_accountHolder;
          PROP_NAME_TO_ID.put(PROP_NAME_accountHolder, PROP_ID_accountHolder);
      
          PROP_ID_TO_NAME[PROP_ID_isDefault] = PROP_NAME_isDefault;
          PROP_NAME_TO_ID.put(PROP_NAME_isDefault, PROP_ID_isDefault);
      
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
    
    /* 往来单位: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 开户银行: BANK_NAME */
    private java.lang.String _bankName;
    
    /* 支行: BANK_BRANCH */
    private java.lang.String _bankBranch;
    
    /* 银行账号: BANK_ACCOUNT */
    private java.lang.String _bankAccount;
    
    /* 账户类型: ACCOUNT_TYPE */
    private java.lang.Integer _accountType;
    
    /* 户名: ACCOUNT_HOLDER */
    private java.lang.String _accountHolder;
    
    /* 是否默认: IS_DEFAULT */
    private java.lang.Boolean _isDefault;
    
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
    

    public _ErpMdBankAccount(){
        // for debug
    }

    protected ErpMdBankAccount newInstance(){
        ErpMdBankAccount entity = new ErpMdBankAccount();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdBankAccount cloneInstance() {
        ErpMdBankAccount entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdBankAccount";
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
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_bankName:
               return getBankName();
        
            case PROP_ID_bankBranch:
               return getBankBranch();
        
            case PROP_ID_bankAccount:
               return getBankAccount();
        
            case PROP_ID_accountType:
               return getAccountType();
        
            case PROP_ID_accountHolder:
               return getAccountHolder();
        
            case PROP_ID_isDefault:
               return getIsDefault();
        
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_bankName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_bankName));
               }
               setBankName(typedValue);
               break;
            }
        
            case PROP_ID_bankBranch:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_bankBranch));
               }
               setBankBranch(typedValue);
               break;
            }
        
            case PROP_ID_bankAccount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_bankAccount));
               }
               setBankAccount(typedValue);
               break;
            }
        
            case PROP_ID_accountType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_accountType));
               }
               setAccountType(typedValue);
               break;
            }
        
            case PROP_ID_accountHolder:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_accountHolder));
               }
               setAccountHolder(typedValue);
               break;
            }
        
            case PROP_ID_isDefault:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isDefault));
               }
               setIsDefault(typedValue);
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_bankName:{
               onInitProp(propId);
               this._bankName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_bankBranch:{
               onInitProp(propId);
               this._bankBranch = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_bankAccount:{
               onInitProp(propId);
               this._bankAccount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_accountType:{
               onInitProp(propId);
               this._accountType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_accountHolder:{
               onInitProp(propId);
               this._accountHolder = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isDefault:{
               onInitProp(propId);
               this._isDefault = (java.lang.Boolean)value;
               
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
     * 往来单位: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 往来单位: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 开户银行: BANK_NAME
     */
    public final java.lang.String getBankName(){
         onPropGet(PROP_ID_bankName);
         return _bankName;
    }

    /**
     * 开户银行: BANK_NAME
     */
    public final void setBankName(java.lang.String value){
        if(onPropSet(PROP_ID_bankName,value)){
            this._bankName = value;
            internalClearRefs(PROP_ID_bankName);
            
        }
    }
    
    /**
     * 支行: BANK_BRANCH
     */
    public final java.lang.String getBankBranch(){
         onPropGet(PROP_ID_bankBranch);
         return _bankBranch;
    }

    /**
     * 支行: BANK_BRANCH
     */
    public final void setBankBranch(java.lang.String value){
        if(onPropSet(PROP_ID_bankBranch,value)){
            this._bankBranch = value;
            internalClearRefs(PROP_ID_bankBranch);
            
        }
    }
    
    /**
     * 银行账号: BANK_ACCOUNT
     */
    public final java.lang.String getBankAccount(){
         onPropGet(PROP_ID_bankAccount);
         return _bankAccount;
    }

    /**
     * 银行账号: BANK_ACCOUNT
     */
    public final void setBankAccount(java.lang.String value){
        if(onPropSet(PROP_ID_bankAccount,value)){
            this._bankAccount = value;
            internalClearRefs(PROP_ID_bankAccount);
            
        }
    }
    
    /**
     * 账户类型: ACCOUNT_TYPE
     */
    public final java.lang.Integer getAccountType(){
         onPropGet(PROP_ID_accountType);
         return _accountType;
    }

    /**
     * 账户类型: ACCOUNT_TYPE
     */
    public final void setAccountType(java.lang.Integer value){
        if(onPropSet(PROP_ID_accountType,value)){
            this._accountType = value;
            internalClearRefs(PROP_ID_accountType);
            
        }
    }
    
    /**
     * 户名: ACCOUNT_HOLDER
     */
    public final java.lang.String getAccountHolder(){
         onPropGet(PROP_ID_accountHolder);
         return _accountHolder;
    }

    /**
     * 户名: ACCOUNT_HOLDER
     */
    public final void setAccountHolder(java.lang.String value){
        if(onPropSet(PROP_ID_accountHolder,value)){
            this._accountHolder = value;
            internalClearRefs(PROP_ID_accountHolder);
            
        }
    }
    
    /**
     * 是否默认: IS_DEFAULT
     */
    public final java.lang.Boolean getIsDefault(){
         onPropGet(PROP_ID_isDefault);
         return _isDefault;
    }

    /**
     * 是否默认: IS_DEFAULT
     */
    public final void setIsDefault(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isDefault,value)){
            this._isDefault = value;
            internalClearRefs(PROP_ID_isDefault);
            
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
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
