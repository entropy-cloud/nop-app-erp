package app.erp.b2b.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.b2b.dao.entity.ErpB2bMftCertificate;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  MFT 证书: erp_b2b_mft_certificate
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bMftCertificate extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 合作伙伴: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 3;
    
    /* 证书名称: CERT_NAME VARCHAR */
    public static final String PROP_NAME_certName = "certName";
    public static final int PROP_ID_certName = 4;
    
    /* 证书类型: CERT_TYPE VARCHAR */
    public static final String PROP_NAME_certType = "certType";
    public static final int PROP_ID_certType = 5;
    
    /* 算法: ALGORITHM VARCHAR */
    public static final String PROP_NAME_algorithm = "algorithm";
    public static final int PROP_ID_algorithm = 6;
    
    /* 密钥长度: KEY_SIZE INTEGER */
    public static final String PROP_NAME_keySize = "keySize";
    public static final int PROP_ID_keySize = 7;
    
    /* 颁发者: ISSUER_NAME VARCHAR */
    public static final String PROP_NAME_issuerName = "issuerName";
    public static final int PROP_ID_issuerName = 8;
    
    /* 主题: SUBJECT_NAME VARCHAR */
    public static final String PROP_NAME_subjectName = "subjectName";
    public static final int PROP_ID_subjectName = 9;
    
    /* 序列号: SERIAL_NO VARCHAR */
    public static final String PROP_NAME_serialNo = "serialNo";
    public static final int PROP_ID_serialNo = 10;
    
    /* SHA-256指纹: FINGERPRINT_SHA256 VARCHAR */
    public static final String PROP_NAME_fingerprintSha256 = "fingerprintSha256";
    public static final int PROP_ID_fingerprintSha256 = 11;
    
    /* 颁发日期: ISSUED_AT DATE */
    public static final String PROP_NAME_issuedAt = "issuedAt";
    public static final int PROP_ID_issuedAt = 12;
    
    /* 过期日期: EXPIRES_AT DATE */
    public static final String PROP_NAME_expiresAt = "expiresAt";
    public static final int PROP_ID_expiresAt = 13;
    
    /* 启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 14;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    

    private static int _PROP_ID_BOUND = 22;

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_certName] = PROP_NAME_certName;
          PROP_NAME_TO_ID.put(PROP_NAME_certName, PROP_ID_certName);
      
          PROP_ID_TO_NAME[PROP_ID_certType] = PROP_NAME_certType;
          PROP_NAME_TO_ID.put(PROP_NAME_certType, PROP_ID_certType);
      
          PROP_ID_TO_NAME[PROP_ID_algorithm] = PROP_NAME_algorithm;
          PROP_NAME_TO_ID.put(PROP_NAME_algorithm, PROP_ID_algorithm);
      
          PROP_ID_TO_NAME[PROP_ID_keySize] = PROP_NAME_keySize;
          PROP_NAME_TO_ID.put(PROP_NAME_keySize, PROP_ID_keySize);
      
          PROP_ID_TO_NAME[PROP_ID_issuerName] = PROP_NAME_issuerName;
          PROP_NAME_TO_ID.put(PROP_NAME_issuerName, PROP_ID_issuerName);
      
          PROP_ID_TO_NAME[PROP_ID_subjectName] = PROP_NAME_subjectName;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectName, PROP_ID_subjectName);
      
          PROP_ID_TO_NAME[PROP_ID_serialNo] = PROP_NAME_serialNo;
          PROP_NAME_TO_ID.put(PROP_NAME_serialNo, PROP_ID_serialNo);
      
          PROP_ID_TO_NAME[PROP_ID_fingerprintSha256] = PROP_NAME_fingerprintSha256;
          PROP_NAME_TO_ID.put(PROP_NAME_fingerprintSha256, PROP_ID_fingerprintSha256);
      
          PROP_ID_TO_NAME[PROP_ID_issuedAt] = PROP_NAME_issuedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_issuedAt, PROP_ID_issuedAt);
      
          PROP_ID_TO_NAME[PROP_ID_expiresAt] = PROP_NAME_expiresAt;
          PROP_NAME_TO_ID.put(PROP_NAME_expiresAt, PROP_ID_expiresAt);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 合作伙伴: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 证书名称: CERT_NAME */
    private java.lang.String _certName;
    
    /* 证书类型: CERT_TYPE */
    private java.lang.String _certType;
    
    /* 算法: ALGORITHM */
    private java.lang.String _algorithm;
    
    /* 密钥长度: KEY_SIZE */
    private java.lang.Integer _keySize;
    
    /* 颁发者: ISSUER_NAME */
    private java.lang.String _issuerName;
    
    /* 主题: SUBJECT_NAME */
    private java.lang.String _subjectName;
    
    /* 序列号: SERIAL_NO */
    private java.lang.String _serialNo;
    
    /* SHA-256指纹: FINGERPRINT_SHA256 */
    private java.lang.String _fingerprintSha256;
    
    /* 颁发日期: ISSUED_AT */
    private java.time.LocalDate _issuedAt;
    
    /* 过期日期: EXPIRES_AT */
    private java.time.LocalDate _expiresAt;
    
    /* 启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
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
    

    public _ErpB2bMftCertificate(){
        // for debug
    }

    protected ErpB2bMftCertificate newInstance(){
        ErpB2bMftCertificate entity = new ErpB2bMftCertificate();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bMftCertificate cloneInstance() {
        ErpB2bMftCertificate entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bMftCertificate";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_certName:
               return getCertName();
        
            case PROP_ID_certType:
               return getCertType();
        
            case PROP_ID_algorithm:
               return getAlgorithm();
        
            case PROP_ID_keySize:
               return getKeySize();
        
            case PROP_ID_issuerName:
               return getIssuerName();
        
            case PROP_ID_subjectName:
               return getSubjectName();
        
            case PROP_ID_serialNo:
               return getSerialNo();
        
            case PROP_ID_fingerprintSha256:
               return getFingerprintSha256();
        
            case PROP_ID_issuedAt:
               return getIssuedAt();
        
            case PROP_ID_expiresAt:
               return getExpiresAt();
        
            case PROP_ID_isActive:
               return getIsActive();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
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
        
            case PROP_ID_certName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_certName));
               }
               setCertName(typedValue);
               break;
            }
        
            case PROP_ID_certType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_certType));
               }
               setCertType(typedValue);
               break;
            }
        
            case PROP_ID_algorithm:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_algorithm));
               }
               setAlgorithm(typedValue);
               break;
            }
        
            case PROP_ID_keySize:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_keySize));
               }
               setKeySize(typedValue);
               break;
            }
        
            case PROP_ID_issuerName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_issuerName));
               }
               setIssuerName(typedValue);
               break;
            }
        
            case PROP_ID_subjectName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectName));
               }
               setSubjectName(typedValue);
               break;
            }
        
            case PROP_ID_serialNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_serialNo));
               }
               setSerialNo(typedValue);
               break;
            }
        
            case PROP_ID_fingerprintSha256:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fingerprintSha256));
               }
               setFingerprintSha256(typedValue);
               break;
            }
        
            case PROP_ID_issuedAt:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_issuedAt));
               }
               setIssuedAt(typedValue);
               break;
            }
        
            case PROP_ID_expiresAt:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_expiresAt));
               }
               setExpiresAt(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_certName:{
               onInitProp(propId);
               this._certName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_certType:{
               onInitProp(propId);
               this._certType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_algorithm:{
               onInitProp(propId);
               this._algorithm = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_keySize:{
               onInitProp(propId);
               this._keySize = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_issuerName:{
               onInitProp(propId);
               this._issuerName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subjectName:{
               onInitProp(propId);
               this._subjectName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_serialNo:{
               onInitProp(propId);
               this._serialNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fingerprintSha256:{
               onInitProp(propId);
               this._fingerprintSha256 = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_issuedAt:{
               onInitProp(propId);
               this._issuedAt = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_expiresAt:{
               onInitProp(propId);
               this._expiresAt = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
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
     * 合作伙伴: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 合作伙伴: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 证书名称: CERT_NAME
     */
    public final java.lang.String getCertName(){
         onPropGet(PROP_ID_certName);
         return _certName;
    }

    /**
     * 证书名称: CERT_NAME
     */
    public final void setCertName(java.lang.String value){
        if(onPropSet(PROP_ID_certName,value)){
            this._certName = value;
            internalClearRefs(PROP_ID_certName);
            
        }
    }
    
    /**
     * 证书类型: CERT_TYPE
     */
    public final java.lang.String getCertType(){
         onPropGet(PROP_ID_certType);
         return _certType;
    }

    /**
     * 证书类型: CERT_TYPE
     */
    public final void setCertType(java.lang.String value){
        if(onPropSet(PROP_ID_certType,value)){
            this._certType = value;
            internalClearRefs(PROP_ID_certType);
            
        }
    }
    
    /**
     * 算法: ALGORITHM
     */
    public final java.lang.String getAlgorithm(){
         onPropGet(PROP_ID_algorithm);
         return _algorithm;
    }

    /**
     * 算法: ALGORITHM
     */
    public final void setAlgorithm(java.lang.String value){
        if(onPropSet(PROP_ID_algorithm,value)){
            this._algorithm = value;
            internalClearRefs(PROP_ID_algorithm);
            
        }
    }
    
    /**
     * 密钥长度: KEY_SIZE
     */
    public final java.lang.Integer getKeySize(){
         onPropGet(PROP_ID_keySize);
         return _keySize;
    }

    /**
     * 密钥长度: KEY_SIZE
     */
    public final void setKeySize(java.lang.Integer value){
        if(onPropSet(PROP_ID_keySize,value)){
            this._keySize = value;
            internalClearRefs(PROP_ID_keySize);
            
        }
    }
    
    /**
     * 颁发者: ISSUER_NAME
     */
    public final java.lang.String getIssuerName(){
         onPropGet(PROP_ID_issuerName);
         return _issuerName;
    }

    /**
     * 颁发者: ISSUER_NAME
     */
    public final void setIssuerName(java.lang.String value){
        if(onPropSet(PROP_ID_issuerName,value)){
            this._issuerName = value;
            internalClearRefs(PROP_ID_issuerName);
            
        }
    }
    
    /**
     * 主题: SUBJECT_NAME
     */
    public final java.lang.String getSubjectName(){
         onPropGet(PROP_ID_subjectName);
         return _subjectName;
    }

    /**
     * 主题: SUBJECT_NAME
     */
    public final void setSubjectName(java.lang.String value){
        if(onPropSet(PROP_ID_subjectName,value)){
            this._subjectName = value;
            internalClearRefs(PROP_ID_subjectName);
            
        }
    }
    
    /**
     * 序列号: SERIAL_NO
     */
    public final java.lang.String getSerialNo(){
         onPropGet(PROP_ID_serialNo);
         return _serialNo;
    }

    /**
     * 序列号: SERIAL_NO
     */
    public final void setSerialNo(java.lang.String value){
        if(onPropSet(PROP_ID_serialNo,value)){
            this._serialNo = value;
            internalClearRefs(PROP_ID_serialNo);
            
        }
    }
    
    /**
     * SHA-256指纹: FINGERPRINT_SHA256
     */
    public final java.lang.String getFingerprintSha256(){
         onPropGet(PROP_ID_fingerprintSha256);
         return _fingerprintSha256;
    }

    /**
     * SHA-256指纹: FINGERPRINT_SHA256
     */
    public final void setFingerprintSha256(java.lang.String value){
        if(onPropSet(PROP_ID_fingerprintSha256,value)){
            this._fingerprintSha256 = value;
            internalClearRefs(PROP_ID_fingerprintSha256);
            
        }
    }
    
    /**
     * 颁发日期: ISSUED_AT
     */
    public final java.time.LocalDate getIssuedAt(){
         onPropGet(PROP_ID_issuedAt);
         return _issuedAt;
    }

    /**
     * 颁发日期: ISSUED_AT
     */
    public final void setIssuedAt(java.time.LocalDate value){
        if(onPropSet(PROP_ID_issuedAt,value)){
            this._issuedAt = value;
            internalClearRefs(PROP_ID_issuedAt);
            
        }
    }
    
    /**
     * 过期日期: EXPIRES_AT
     */
    public final java.time.LocalDate getExpiresAt(){
         onPropGet(PROP_ID_expiresAt);
         return _expiresAt;
    }

    /**
     * 过期日期: EXPIRES_AT
     */
    public final void setExpiresAt(java.time.LocalDate value){
        if(onPropSet(PROP_ID_expiresAt,value)){
            this._expiresAt = value;
            internalClearRefs(PROP_ID_expiresAt);
            
        }
    }
    
    /**
     * 启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
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
    
}
// resume CPD analysis - CPD-ON
