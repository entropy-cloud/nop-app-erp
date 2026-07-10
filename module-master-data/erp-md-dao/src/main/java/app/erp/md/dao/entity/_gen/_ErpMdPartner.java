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

import app.erp.md.dao.entity.ErpMdPartner;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  往来单位: erp_md_partner
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdPartner extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 类型: PARTNER_TYPE VARCHAR */
    public static final String PROP_NAME_partnerType = "partnerType";
    public static final int PROP_ID_partnerType = 4;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 5;
    
    /* 联系人: CONTACT_PERSON VARCHAR */
    public static final String PROP_NAME_contactPerson = "contactPerson";
    public static final int PROP_ID_contactPerson = 6;
    
    /* 电话: PHONE VARCHAR */
    public static final String PROP_NAME_phone = "phone";
    public static final int PROP_ID_phone = 7;
    
    /* 邮箱: EMAIL VARCHAR */
    public static final String PROP_NAME_email = "email";
    public static final int PROP_ID_email = 8;
    
    /* 地址: ADDRESS VARCHAR */
    public static final String PROP_NAME_address = "address";
    public static final int PROP_ID_address = 9;
    
    /* 税号: TAX_NO VARCHAR */
    public static final String PROP_NAME_taxNo = "taxNo";
    public static final int PROP_ID_taxNo = 10;
    
    /* 信用额度: CREDIT_LIMIT DECIMAL */
    public static final String PROP_NAME_creditLimit = "creditLimit";
    public static final int PROP_ID_creditLimit = 11;
    
    /* 信用期(天): CREDIT_PERIOD_DAYS INTEGER */
    public static final String PROP_NAME_creditPeriodDays = "creditPeriodDays";
    public static final int PROP_ID_creditPeriodDays = 12;
    
    /* 应收余额: RECEIVABLE_BALANCE DECIMAL */
    public static final String PROP_NAME_receivableBalance = "receivableBalance";
    public static final int PROP_ID_receivableBalance = 13;
    
    /* 应付余额: PAYABLE_BALANCE DECIMAL */
    public static final String PROP_NAME_payableBalance = "payableBalance";
    public static final int PROP_ID_payableBalance = 14;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 15;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 16;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 17;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 18;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 19;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 20;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 21;
    
    /* 客户组: CUSTOMER_GROUP VARCHAR */
    public static final String PROP_NAME_customerGroup = "customerGroup";
    public static final int PROP_ID_customerGroup = 100;
    

    private static int _PROP_ID_BOUND = 101;

    
    /* relation:  */
    public static final String PROP_NAME_addresses = "addresses";
    
    /* relation:  */
    public static final String PROP_NAME_contacts = "contacts";
    
    /* relation:  */
    public static final String PROP_NAME_bankAccounts = "bankAccounts";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[101];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_partnerType] = PROP_NAME_partnerType;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerType, PROP_ID_partnerType);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_contactPerson] = PROP_NAME_contactPerson;
          PROP_NAME_TO_ID.put(PROP_NAME_contactPerson, PROP_ID_contactPerson);
      
          PROP_ID_TO_NAME[PROP_ID_phone] = PROP_NAME_phone;
          PROP_NAME_TO_ID.put(PROP_NAME_phone, PROP_ID_phone);
      
          PROP_ID_TO_NAME[PROP_ID_email] = PROP_NAME_email;
          PROP_NAME_TO_ID.put(PROP_NAME_email, PROP_ID_email);
      
          PROP_ID_TO_NAME[PROP_ID_address] = PROP_NAME_address;
          PROP_NAME_TO_ID.put(PROP_NAME_address, PROP_ID_address);
      
          PROP_ID_TO_NAME[PROP_ID_taxNo] = PROP_NAME_taxNo;
          PROP_NAME_TO_ID.put(PROP_NAME_taxNo, PROP_ID_taxNo);
      
          PROP_ID_TO_NAME[PROP_ID_creditLimit] = PROP_NAME_creditLimit;
          PROP_NAME_TO_ID.put(PROP_NAME_creditLimit, PROP_ID_creditLimit);
      
          PROP_ID_TO_NAME[PROP_ID_creditPeriodDays] = PROP_NAME_creditPeriodDays;
          PROP_NAME_TO_ID.put(PROP_NAME_creditPeriodDays, PROP_ID_creditPeriodDays);
      
          PROP_ID_TO_NAME[PROP_ID_receivableBalance] = PROP_NAME_receivableBalance;
          PROP_NAME_TO_ID.put(PROP_NAME_receivableBalance, PROP_ID_receivableBalance);
      
          PROP_ID_TO_NAME[PROP_ID_payableBalance] = PROP_NAME_payableBalance;
          PROP_NAME_TO_ID.put(PROP_NAME_payableBalance, PROP_ID_payableBalance);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_customerGroup] = PROP_NAME_customerGroup;
          PROP_NAME_TO_ID.put(PROP_NAME_customerGroup, PROP_ID_customerGroup);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 名称: NAME */
    private java.lang.String _name;
    
    /* 类型: PARTNER_TYPE */
    private java.lang.String _partnerType;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 联系人: CONTACT_PERSON */
    private java.lang.String _contactPerson;
    
    /* 电话: PHONE */
    private java.lang.String _phone;
    
    /* 邮箱: EMAIL */
    private java.lang.String _email;
    
    /* 地址: ADDRESS */
    private java.lang.String _address;
    
    /* 税号: TAX_NO */
    private java.lang.String _taxNo;
    
    /* 信用额度: CREDIT_LIMIT */
    private java.math.BigDecimal _creditLimit;
    
    /* 信用期(天): CREDIT_PERIOD_DAYS */
    private java.lang.Integer _creditPeriodDays;
    
    /* 应收余额: RECEIVABLE_BALANCE */
    private java.math.BigDecimal _receivableBalance;
    
    /* 应付余额: PAYABLE_BALANCE */
    private java.math.BigDecimal _payableBalance;
    
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
    
    /* 客户组: CUSTOMER_GROUP */
    private java.lang.String _customerGroup;
    

    public _ErpMdPartner(){
        // for debug
    }

    protected ErpMdPartner newInstance(){
        ErpMdPartner entity = new ErpMdPartner();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdPartner cloneInstance() {
        ErpMdPartner entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdPartner";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_partnerType:
               return getPartnerType();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_contactPerson:
               return getContactPerson();
        
            case PROP_ID_phone:
               return getPhone();
        
            case PROP_ID_email:
               return getEmail();
        
            case PROP_ID_address:
               return getAddress();
        
            case PROP_ID_taxNo:
               return getTaxNo();
        
            case PROP_ID_creditLimit:
               return getCreditLimit();
        
            case PROP_ID_creditPeriodDays:
               return getCreditPeriodDays();
        
            case PROP_ID_receivableBalance:
               return getReceivableBalance();
        
            case PROP_ID_payableBalance:
               return getPayableBalance();
        
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
        
            case PROP_ID_customerGroup:
               return getCustomerGroup();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
               break;
            }
        
            case PROP_ID_partnerType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_partnerType));
               }
               setPartnerType(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_contactPerson:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactPerson));
               }
               setContactPerson(typedValue);
               break;
            }
        
            case PROP_ID_phone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_phone));
               }
               setPhone(typedValue);
               break;
            }
        
            case PROP_ID_email:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_email));
               }
               setEmail(typedValue);
               break;
            }
        
            case PROP_ID_address:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_address));
               }
               setAddress(typedValue);
               break;
            }
        
            case PROP_ID_taxNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_taxNo));
               }
               setTaxNo(typedValue);
               break;
            }
        
            case PROP_ID_creditLimit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_creditLimit));
               }
               setCreditLimit(typedValue);
               break;
            }
        
            case PROP_ID_creditPeriodDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_creditPeriodDays));
               }
               setCreditPeriodDays(typedValue);
               break;
            }
        
            case PROP_ID_receivableBalance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_receivableBalance));
               }
               setReceivableBalance(typedValue);
               break;
            }
        
            case PROP_ID_payableBalance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_payableBalance));
               }
               setPayableBalance(typedValue);
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
        
            case PROP_ID_customerGroup:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_customerGroup));
               }
               setCustomerGroup(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_partnerType:{
               onInitProp(propId);
               this._partnerType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_contactPerson:{
               onInitProp(propId);
               this._contactPerson = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_phone:{
               onInitProp(propId);
               this._phone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_email:{
               onInitProp(propId);
               this._email = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_address:{
               onInitProp(propId);
               this._address = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_taxNo:{
               onInitProp(propId);
               this._taxNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_creditLimit:{
               onInitProp(propId);
               this._creditLimit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_creditPeriodDays:{
               onInitProp(propId);
               this._creditPeriodDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_receivableBalance:{
               onInitProp(propId);
               this._receivableBalance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_payableBalance:{
               onInitProp(propId);
               this._payableBalance = (java.math.BigDecimal)value;
               
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
        
            case PROP_ID_customerGroup:{
               onInitProp(propId);
               this._customerGroup = (java.lang.String)value;
               
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
     * 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 类型: PARTNER_TYPE
     */
    public final java.lang.String getPartnerType(){
         onPropGet(PROP_ID_partnerType);
         return _partnerType;
    }

    /**
     * 类型: PARTNER_TYPE
     */
    public final void setPartnerType(java.lang.String value){
        if(onPropSet(PROP_ID_partnerType,value)){
            this._partnerType = value;
            internalClearRefs(PROP_ID_partnerType);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 联系人: CONTACT_PERSON
     */
    public final java.lang.String getContactPerson(){
         onPropGet(PROP_ID_contactPerson);
         return _contactPerson;
    }

    /**
     * 联系人: CONTACT_PERSON
     */
    public final void setContactPerson(java.lang.String value){
        if(onPropSet(PROP_ID_contactPerson,value)){
            this._contactPerson = value;
            internalClearRefs(PROP_ID_contactPerson);
            
        }
    }
    
    /**
     * 电话: PHONE
     */
    public final java.lang.String getPhone(){
         onPropGet(PROP_ID_phone);
         return _phone;
    }

    /**
     * 电话: PHONE
     */
    public final void setPhone(java.lang.String value){
        if(onPropSet(PROP_ID_phone,value)){
            this._phone = value;
            internalClearRefs(PROP_ID_phone);
            
        }
    }
    
    /**
     * 邮箱: EMAIL
     */
    public final java.lang.String getEmail(){
         onPropGet(PROP_ID_email);
         return _email;
    }

    /**
     * 邮箱: EMAIL
     */
    public final void setEmail(java.lang.String value){
        if(onPropSet(PROP_ID_email,value)){
            this._email = value;
            internalClearRefs(PROP_ID_email);
            
        }
    }
    
    /**
     * 地址: ADDRESS
     */
    public final java.lang.String getAddress(){
         onPropGet(PROP_ID_address);
         return _address;
    }

    /**
     * 地址: ADDRESS
     */
    public final void setAddress(java.lang.String value){
        if(onPropSet(PROP_ID_address,value)){
            this._address = value;
            internalClearRefs(PROP_ID_address);
            
        }
    }
    
    /**
     * 税号: TAX_NO
     */
    public final java.lang.String getTaxNo(){
         onPropGet(PROP_ID_taxNo);
         return _taxNo;
    }

    /**
     * 税号: TAX_NO
     */
    public final void setTaxNo(java.lang.String value){
        if(onPropSet(PROP_ID_taxNo,value)){
            this._taxNo = value;
            internalClearRefs(PROP_ID_taxNo);
            
        }
    }
    
    /**
     * 信用额度: CREDIT_LIMIT
     */
    public final java.math.BigDecimal getCreditLimit(){
         onPropGet(PROP_ID_creditLimit);
         return _creditLimit;
    }

    /**
     * 信用额度: CREDIT_LIMIT
     */
    public final void setCreditLimit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_creditLimit,value)){
            this._creditLimit = value;
            internalClearRefs(PROP_ID_creditLimit);
            
        }
    }
    
    /**
     * 信用期(天): CREDIT_PERIOD_DAYS
     */
    public final java.lang.Integer getCreditPeriodDays(){
         onPropGet(PROP_ID_creditPeriodDays);
         return _creditPeriodDays;
    }

    /**
     * 信用期(天): CREDIT_PERIOD_DAYS
     */
    public final void setCreditPeriodDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_creditPeriodDays,value)){
            this._creditPeriodDays = value;
            internalClearRefs(PROP_ID_creditPeriodDays);
            
        }
    }
    
    /**
     * 应收余额: RECEIVABLE_BALANCE
     */
    public final java.math.BigDecimal getReceivableBalance(){
         onPropGet(PROP_ID_receivableBalance);
         return _receivableBalance;
    }

    /**
     * 应收余额: RECEIVABLE_BALANCE
     */
    public final void setReceivableBalance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_receivableBalance,value)){
            this._receivableBalance = value;
            internalClearRefs(PROP_ID_receivableBalance);
            
        }
    }
    
    /**
     * 应付余额: PAYABLE_BALANCE
     */
    public final java.math.BigDecimal getPayableBalance(){
         onPropGet(PROP_ID_payableBalance);
         return _payableBalance;
    }

    /**
     * 应付余额: PAYABLE_BALANCE
     */
    public final void setPayableBalance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_payableBalance,value)){
            this._payableBalance = value;
            internalClearRefs(PROP_ID_payableBalance);
            
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
     * 客户组: CUSTOMER_GROUP
     */
    public final java.lang.String getCustomerGroup(){
         onPropGet(PROP_ID_customerGroup);
         return _customerGroup;
    }

    /**
     * 客户组: CUSTOMER_GROUP
     */
    public final void setCustomerGroup(java.lang.String value){
        if(onPropSet(PROP_ID_customerGroup,value)){
            this._customerGroup = value;
            internalClearRefs(PROP_ID_customerGroup);
            
        }
    }
    
    private final OrmEntitySet<app.erp.md.dao.entity.ErpMdPartnerAddress> _addresses = new OrmEntitySet<>(this, PROP_NAME_addresses,
        null, null,app.erp.md.dao.entity.ErpMdPartnerAddress.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.md.dao.entity.ErpMdPartnerAddress> getAddresses(){
       return _addresses;
    }
       
    private final OrmEntitySet<app.erp.md.dao.entity.ErpMdPartnerContact> _contacts = new OrmEntitySet<>(this, PROP_NAME_contacts,
        null, null,app.erp.md.dao.entity.ErpMdPartnerContact.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.md.dao.entity.ErpMdPartnerContact> getContacts(){
       return _contacts;
    }
       
    private final OrmEntitySet<app.erp.md.dao.entity.ErpMdBankAccount> _bankAccounts = new OrmEntitySet<>(this, PROP_NAME_bankAccounts,
        null, null,app.erp.md.dao.entity.ErpMdBankAccount.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.md.dao.entity.ErpMdBankAccount> getBankAccounts(){
       return _bankAccounts;
    }
       
}
// resume CPD analysis - CPD-ON
