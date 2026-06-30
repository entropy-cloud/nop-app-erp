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

import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  合作伙伴档案: erp_b2b_partner_profile
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bPartnerProfile extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 关联伙伴: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 4;
    
    /* 伙伴名称: PARTNER_NAME VARCHAR */
    public static final String PROP_NAME_partnerName = "partnerName";
    public static final int PROP_ID_partnerName = 5;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 6;
    
    /* 传输协议: PROTOCOL INTEGER */
    public static final String PROP_NAME_protocol = "protocol";
    public static final int PROP_ID_protocol = 7;
    
    /* 传输端点: TRANSPORT_ENDPOINT VARCHAR */
    public static final String PROP_NAME_transportEndpoint = "transportEndpoint";
    public static final int PROP_ID_transportEndpoint = 8;
    
    /* 认证方式: AUTH_METHOD INTEGER */
    public static final String PROP_NAME_authMethod = "authMethod";
    public static final int PROP_ID_authMethod = 9;
    
    /* Webhook密钥: WEBHOOK_SECRET VARCHAR */
    public static final String PROP_NAME_webhookSecret = "webhookSecret";
    public static final int PROP_ID_webhookSecret = 10;
    
    /* 证书过期日期: CERT_EXPIRY DATE */
    public static final String PROP_NAME_certExpiry = "certExpiry";
    public static final int PROP_ID_certExpiry = 11;
    
    /* 证书指纹: CERT_FINGERPRINT VARCHAR */
    public static final String PROP_NAME_certFingerprint = "certFingerprint";
    public static final int PROP_ID_certFingerprint = 12;
    
    /* 支持格式(JSON): ALLOWED_FORMATS VARCHAR */
    public static final String PROP_NAME_allowedFormats = "allowedFormats";
    public static final int PROP_ID_allowedFormats = 13;
    
    /* 时区: TIMEZONE VARCHAR */
    public static final String PROP_NAME_timezone = "timezone";
    public static final int PROP_ID_timezone = 14;
    
    /* 联系人姓名: CONTACT_NAME VARCHAR */
    public static final String PROP_NAME_contactName = "contactName";
    public static final int PROP_ID_contactName = 15;
    
    /* 联系邮箱: CONTACT_EMAIL VARCHAR */
    public static final String PROP_NAME_contactEmail = "contactEmail";
    public static final int PROP_ID_contactEmail = 16;
    
    /* 联系电话: CONTACT_PHONE VARCHAR */
    public static final String PROP_NAME_contactPhone = "contactPhone";
    public static final int PROP_ID_contactPhone = 17;
    
    /* 备注: NOTES VARCHAR */
    public static final String PROP_NAME_notes = "notes";
    public static final int PROP_ID_notes = 18;
    
    /* 上线日期: GO_LIVE_DATE DATE */
    public static final String PROP_NAME_goLiveDate = "goLiveDate";
    public static final int PROP_ID_goLiveDate = 19;
    
    /* 归档日期: ARCHIVED_AT DATETIME */
    public static final String PROP_NAME_archivedAt = "archivedAt";
    public static final int PROP_ID_archivedAt = 20;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 21;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 22;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 23;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 24;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 25;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 26;
    

    private static int _PROP_ID_BOUND = 27;

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerName] = PROP_NAME_partnerName;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerName, PROP_ID_partnerName);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_protocol] = PROP_NAME_protocol;
          PROP_NAME_TO_ID.put(PROP_NAME_protocol, PROP_ID_protocol);
      
          PROP_ID_TO_NAME[PROP_ID_transportEndpoint] = PROP_NAME_transportEndpoint;
          PROP_NAME_TO_ID.put(PROP_NAME_transportEndpoint, PROP_ID_transportEndpoint);
      
          PROP_ID_TO_NAME[PROP_ID_authMethod] = PROP_NAME_authMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_authMethod, PROP_ID_authMethod);
      
          PROP_ID_TO_NAME[PROP_ID_webhookSecret] = PROP_NAME_webhookSecret;
          PROP_NAME_TO_ID.put(PROP_NAME_webhookSecret, PROP_ID_webhookSecret);
      
          PROP_ID_TO_NAME[PROP_ID_certExpiry] = PROP_NAME_certExpiry;
          PROP_NAME_TO_ID.put(PROP_NAME_certExpiry, PROP_ID_certExpiry);
      
          PROP_ID_TO_NAME[PROP_ID_certFingerprint] = PROP_NAME_certFingerprint;
          PROP_NAME_TO_ID.put(PROP_NAME_certFingerprint, PROP_ID_certFingerprint);
      
          PROP_ID_TO_NAME[PROP_ID_allowedFormats] = PROP_NAME_allowedFormats;
          PROP_NAME_TO_ID.put(PROP_NAME_allowedFormats, PROP_ID_allowedFormats);
      
          PROP_ID_TO_NAME[PROP_ID_timezone] = PROP_NAME_timezone;
          PROP_NAME_TO_ID.put(PROP_NAME_timezone, PROP_ID_timezone);
      
          PROP_ID_TO_NAME[PROP_ID_contactName] = PROP_NAME_contactName;
          PROP_NAME_TO_ID.put(PROP_NAME_contactName, PROP_ID_contactName);
      
          PROP_ID_TO_NAME[PROP_ID_contactEmail] = PROP_NAME_contactEmail;
          PROP_NAME_TO_ID.put(PROP_NAME_contactEmail, PROP_ID_contactEmail);
      
          PROP_ID_TO_NAME[PROP_ID_contactPhone] = PROP_NAME_contactPhone;
          PROP_NAME_TO_ID.put(PROP_NAME_contactPhone, PROP_ID_contactPhone);
      
          PROP_ID_TO_NAME[PROP_ID_notes] = PROP_NAME_notes;
          PROP_NAME_TO_ID.put(PROP_NAME_notes, PROP_ID_notes);
      
          PROP_ID_TO_NAME[PROP_ID_goLiveDate] = PROP_NAME_goLiveDate;
          PROP_NAME_TO_ID.put(PROP_NAME_goLiveDate, PROP_ID_goLiveDate);
      
          PROP_ID_TO_NAME[PROP_ID_archivedAt] = PROP_NAME_archivedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_archivedAt, PROP_ID_archivedAt);
      
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
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 关联伙伴: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 伙伴名称: PARTNER_NAME */
    private java.lang.String _partnerName;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 传输协议: PROTOCOL */
    private java.lang.Integer _protocol;
    
    /* 传输端点: TRANSPORT_ENDPOINT */
    private java.lang.String _transportEndpoint;
    
    /* 认证方式: AUTH_METHOD */
    private java.lang.Integer _authMethod;
    
    /* Webhook密钥: WEBHOOK_SECRET */
    private java.lang.String _webhookSecret;
    
    /* 证书过期日期: CERT_EXPIRY */
    private java.time.LocalDate _certExpiry;
    
    /* 证书指纹: CERT_FINGERPRINT */
    private java.lang.String _certFingerprint;
    
    /* 支持格式(JSON): ALLOWED_FORMATS */
    private java.lang.String _allowedFormats;
    
    /* 时区: TIMEZONE */
    private java.lang.String _timezone;
    
    /* 联系人姓名: CONTACT_NAME */
    private java.lang.String _contactName;
    
    /* 联系邮箱: CONTACT_EMAIL */
    private java.lang.String _contactEmail;
    
    /* 联系电话: CONTACT_PHONE */
    private java.lang.String _contactPhone;
    
    /* 备注: NOTES */
    private java.lang.String _notes;
    
    /* 上线日期: GO_LIVE_DATE */
    private java.time.LocalDate _goLiveDate;
    
    /* 归档日期: ARCHIVED_AT */
    private java.time.LocalDateTime _archivedAt;
    
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
    

    public _ErpB2bPartnerProfile(){
        // for debug
    }

    protected ErpB2bPartnerProfile newInstance(){
        ErpB2bPartnerProfile entity = new ErpB2bPartnerProfile();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bPartnerProfile cloneInstance() {
        ErpB2bPartnerProfile entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bPartnerProfile";
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
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_partnerName:
               return getPartnerName();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_protocol:
               return getProtocol();
        
            case PROP_ID_transportEndpoint:
               return getTransportEndpoint();
        
            case PROP_ID_authMethod:
               return getAuthMethod();
        
            case PROP_ID_webhookSecret:
               return getWebhookSecret();
        
            case PROP_ID_certExpiry:
               return getCertExpiry();
        
            case PROP_ID_certFingerprint:
               return getCertFingerprint();
        
            case PROP_ID_allowedFormats:
               return getAllowedFormats();
        
            case PROP_ID_timezone:
               return getTimezone();
        
            case PROP_ID_contactName:
               return getContactName();
        
            case PROP_ID_contactEmail:
               return getContactEmail();
        
            case PROP_ID_contactPhone:
               return getContactPhone();
        
            case PROP_ID_notes:
               return getNotes();
        
            case PROP_ID_goLiveDate:
               return getGoLiveDate();
        
            case PROP_ID_archivedAt:
               return getArchivedAt();
        
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_partnerName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_partnerName));
               }
               setPartnerName(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_protocol:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_protocol));
               }
               setProtocol(typedValue);
               break;
            }
        
            case PROP_ID_transportEndpoint:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_transportEndpoint));
               }
               setTransportEndpoint(typedValue);
               break;
            }
        
            case PROP_ID_authMethod:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_authMethod));
               }
               setAuthMethod(typedValue);
               break;
            }
        
            case PROP_ID_webhookSecret:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_webhookSecret));
               }
               setWebhookSecret(typedValue);
               break;
            }
        
            case PROP_ID_certExpiry:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_certExpiry));
               }
               setCertExpiry(typedValue);
               break;
            }
        
            case PROP_ID_certFingerprint:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_certFingerprint));
               }
               setCertFingerprint(typedValue);
               break;
            }
        
            case PROP_ID_allowedFormats:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_allowedFormats));
               }
               setAllowedFormats(typedValue);
               break;
            }
        
            case PROP_ID_timezone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_timezone));
               }
               setTimezone(typedValue);
               break;
            }
        
            case PROP_ID_contactName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactName));
               }
               setContactName(typedValue);
               break;
            }
        
            case PROP_ID_contactEmail:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactEmail));
               }
               setContactEmail(typedValue);
               break;
            }
        
            case PROP_ID_contactPhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_contactPhone));
               }
               setContactPhone(typedValue);
               break;
            }
        
            case PROP_ID_notes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notes));
               }
               setNotes(typedValue);
               break;
            }
        
            case PROP_ID_goLiveDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_goLiveDate));
               }
               setGoLiveDate(typedValue);
               break;
            }
        
            case PROP_ID_archivedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_archivedAt));
               }
               setArchivedAt(typedValue);
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_partnerName:{
               onInitProp(propId);
               this._partnerName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_protocol:{
               onInitProp(propId);
               this._protocol = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_transportEndpoint:{
               onInitProp(propId);
               this._transportEndpoint = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_authMethod:{
               onInitProp(propId);
               this._authMethod = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_webhookSecret:{
               onInitProp(propId);
               this._webhookSecret = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_certExpiry:{
               onInitProp(propId);
               this._certExpiry = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_certFingerprint:{
               onInitProp(propId);
               this._certFingerprint = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_allowedFormats:{
               onInitProp(propId);
               this._allowedFormats = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_timezone:{
               onInitProp(propId);
               this._timezone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_contactName:{
               onInitProp(propId);
               this._contactName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_contactEmail:{
               onInitProp(propId);
               this._contactEmail = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_contactPhone:{
               onInitProp(propId);
               this._contactPhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_notes:{
               onInitProp(propId);
               this._notes = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_goLiveDate:{
               onInitProp(propId);
               this._goLiveDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_archivedAt:{
               onInitProp(propId);
               this._archivedAt = (java.time.LocalDateTime)value;
               
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
     * 关联伙伴: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 关联伙伴: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 伙伴名称: PARTNER_NAME
     */
    public final java.lang.String getPartnerName(){
         onPropGet(PROP_ID_partnerName);
         return _partnerName;
    }

    /**
     * 伙伴名称: PARTNER_NAME
     */
    public final void setPartnerName(java.lang.String value){
        if(onPropSet(PROP_ID_partnerName,value)){
            this._partnerName = value;
            internalClearRefs(PROP_ID_partnerName);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 传输协议: PROTOCOL
     */
    public final java.lang.Integer getProtocol(){
         onPropGet(PROP_ID_protocol);
         return _protocol;
    }

    /**
     * 传输协议: PROTOCOL
     */
    public final void setProtocol(java.lang.Integer value){
        if(onPropSet(PROP_ID_protocol,value)){
            this._protocol = value;
            internalClearRefs(PROP_ID_protocol);
            
        }
    }
    
    /**
     * 传输端点: TRANSPORT_ENDPOINT
     */
    public final java.lang.String getTransportEndpoint(){
         onPropGet(PROP_ID_transportEndpoint);
         return _transportEndpoint;
    }

    /**
     * 传输端点: TRANSPORT_ENDPOINT
     */
    public final void setTransportEndpoint(java.lang.String value){
        if(onPropSet(PROP_ID_transportEndpoint,value)){
            this._transportEndpoint = value;
            internalClearRefs(PROP_ID_transportEndpoint);
            
        }
    }
    
    /**
     * 认证方式: AUTH_METHOD
     */
    public final java.lang.Integer getAuthMethod(){
         onPropGet(PROP_ID_authMethod);
         return _authMethod;
    }

    /**
     * 认证方式: AUTH_METHOD
     */
    public final void setAuthMethod(java.lang.Integer value){
        if(onPropSet(PROP_ID_authMethod,value)){
            this._authMethod = value;
            internalClearRefs(PROP_ID_authMethod);
            
        }
    }
    
    /**
     * Webhook密钥: WEBHOOK_SECRET
     */
    public final java.lang.String getWebhookSecret(){
         onPropGet(PROP_ID_webhookSecret);
         return _webhookSecret;
    }

    /**
     * Webhook密钥: WEBHOOK_SECRET
     */
    public final void setWebhookSecret(java.lang.String value){
        if(onPropSet(PROP_ID_webhookSecret,value)){
            this._webhookSecret = value;
            internalClearRefs(PROP_ID_webhookSecret);
            
        }
    }
    
    /**
     * 证书过期日期: CERT_EXPIRY
     */
    public final java.time.LocalDate getCertExpiry(){
         onPropGet(PROP_ID_certExpiry);
         return _certExpiry;
    }

    /**
     * 证书过期日期: CERT_EXPIRY
     */
    public final void setCertExpiry(java.time.LocalDate value){
        if(onPropSet(PROP_ID_certExpiry,value)){
            this._certExpiry = value;
            internalClearRefs(PROP_ID_certExpiry);
            
        }
    }
    
    /**
     * 证书指纹: CERT_FINGERPRINT
     */
    public final java.lang.String getCertFingerprint(){
         onPropGet(PROP_ID_certFingerprint);
         return _certFingerprint;
    }

    /**
     * 证书指纹: CERT_FINGERPRINT
     */
    public final void setCertFingerprint(java.lang.String value){
        if(onPropSet(PROP_ID_certFingerprint,value)){
            this._certFingerprint = value;
            internalClearRefs(PROP_ID_certFingerprint);
            
        }
    }
    
    /**
     * 支持格式(JSON): ALLOWED_FORMATS
     */
    public final java.lang.String getAllowedFormats(){
         onPropGet(PROP_ID_allowedFormats);
         return _allowedFormats;
    }

    /**
     * 支持格式(JSON): ALLOWED_FORMATS
     */
    public final void setAllowedFormats(java.lang.String value){
        if(onPropSet(PROP_ID_allowedFormats,value)){
            this._allowedFormats = value;
            internalClearRefs(PROP_ID_allowedFormats);
            
        }
    }
    
    /**
     * 时区: TIMEZONE
     */
    public final java.lang.String getTimezone(){
         onPropGet(PROP_ID_timezone);
         return _timezone;
    }

    /**
     * 时区: TIMEZONE
     */
    public final void setTimezone(java.lang.String value){
        if(onPropSet(PROP_ID_timezone,value)){
            this._timezone = value;
            internalClearRefs(PROP_ID_timezone);
            
        }
    }
    
    /**
     * 联系人姓名: CONTACT_NAME
     */
    public final java.lang.String getContactName(){
         onPropGet(PROP_ID_contactName);
         return _contactName;
    }

    /**
     * 联系人姓名: CONTACT_NAME
     */
    public final void setContactName(java.lang.String value){
        if(onPropSet(PROP_ID_contactName,value)){
            this._contactName = value;
            internalClearRefs(PROP_ID_contactName);
            
        }
    }
    
    /**
     * 联系邮箱: CONTACT_EMAIL
     */
    public final java.lang.String getContactEmail(){
         onPropGet(PROP_ID_contactEmail);
         return _contactEmail;
    }

    /**
     * 联系邮箱: CONTACT_EMAIL
     */
    public final void setContactEmail(java.lang.String value){
        if(onPropSet(PROP_ID_contactEmail,value)){
            this._contactEmail = value;
            internalClearRefs(PROP_ID_contactEmail);
            
        }
    }
    
    /**
     * 联系电话: CONTACT_PHONE
     */
    public final java.lang.String getContactPhone(){
         onPropGet(PROP_ID_contactPhone);
         return _contactPhone;
    }

    /**
     * 联系电话: CONTACT_PHONE
     */
    public final void setContactPhone(java.lang.String value){
        if(onPropSet(PROP_ID_contactPhone,value)){
            this._contactPhone = value;
            internalClearRefs(PROP_ID_contactPhone);
            
        }
    }
    
    /**
     * 备注: NOTES
     */
    public final java.lang.String getNotes(){
         onPropGet(PROP_ID_notes);
         return _notes;
    }

    /**
     * 备注: NOTES
     */
    public final void setNotes(java.lang.String value){
        if(onPropSet(PROP_ID_notes,value)){
            this._notes = value;
            internalClearRefs(PROP_ID_notes);
            
        }
    }
    
    /**
     * 上线日期: GO_LIVE_DATE
     */
    public final java.time.LocalDate getGoLiveDate(){
         onPropGet(PROP_ID_goLiveDate);
         return _goLiveDate;
    }

    /**
     * 上线日期: GO_LIVE_DATE
     */
    public final void setGoLiveDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_goLiveDate,value)){
            this._goLiveDate = value;
            internalClearRefs(PROP_ID_goLiveDate);
            
        }
    }
    
    /**
     * 归档日期: ARCHIVED_AT
     */
    public final java.time.LocalDateTime getArchivedAt(){
         onPropGet(PROP_ID_archivedAt);
         return _archivedAt;
    }

    /**
     * 归档日期: ARCHIVED_AT
     */
    public final void setArchivedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_archivedAt,value)){
            this._archivedAt = value;
            internalClearRefs(PROP_ID_archivedAt);
            
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
