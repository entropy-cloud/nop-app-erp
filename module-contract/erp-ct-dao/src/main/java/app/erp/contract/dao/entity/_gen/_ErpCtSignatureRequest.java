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

import app.erp.contract.dao.entity.ErpCtSignatureRequest;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  签章请求: erp_ct_signature_request
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCtSignatureRequest extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 合同版本: CONTRACT_VERSION_ID BIGINT */
    public static final String PROP_NAME_contractVersionId = "contractVersionId";
    public static final int PROP_ID_contractVersionId = 3;
    
    /* 签名提供商: PROVIDER VARCHAR */
    public static final String PROP_NAME_provider = "provider";
    public static final int PROP_ID_provider = 4;
    
    /* 提供商请求ID: PROVIDER_REQUEST_ID VARCHAR */
    public static final String PROP_NAME_providerRequestId = "providerRequestId";
    public static final int PROP_ID_providerRequestId = 5;
    
    /* 签章状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 6;
    
    /* 签署人(JSON): SIGNERS VARCHAR */
    public static final String PROP_NAME_signers = "signers";
    public static final int PROP_ID_signers = 7;
    
    /* 签署截止日期: SIGNING_DEADLINE DATE */
    public static final String PROP_NAME_signingDeadline = "signingDeadline";
    public static final int PROP_ID_signingDeadline = 8;
    
    /* 签署完成时间: COMPLETED_AT DATETIME */
    public static final String PROP_NAME_completedAt = "completedAt";
    public static final int PROP_ID_completedAt = 9;
    
    /* 完成证书URL: CERTIFICATE_URL VARCHAR */
    public static final String PROP_NAME_certificateUrl = "certificateUrl";
    public static final int PROP_ID_certificateUrl = 10;
    
    /* 存证编号: EVIDENCE_NO VARCHAR */
    public static final String PROP_NAME_evidenceNo = "evidenceNo";
    public static final int PROP_ID_evidenceNo = 11;
    
    /* 已签署文件: ATTACHMENT_FILE_ID VARCHAR */
    public static final String PROP_NAME_attachmentFileId = "attachmentFileId";
    public static final int PROP_ID_attachmentFileId = 12;
    
    /* 错误信息: ERROR_MSG VARCHAR */
    public static final String PROP_NAME_errorMsg = "errorMsg";
    public static final int PROP_ID_errorMsg = 13;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 14;
    
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
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_contractVersion = "contractVersion";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_providerRequest = "providerRequest";
    
    /* component:  */
    public static final String PROP_NAME_attachmentFileIdComponent = "attachmentFileIdComponent";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_contractVersionId] = PROP_NAME_contractVersionId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractVersionId, PROP_ID_contractVersionId);
      
          PROP_ID_TO_NAME[PROP_ID_provider] = PROP_NAME_provider;
          PROP_NAME_TO_ID.put(PROP_NAME_provider, PROP_ID_provider);
      
          PROP_ID_TO_NAME[PROP_ID_providerRequestId] = PROP_NAME_providerRequestId;
          PROP_NAME_TO_ID.put(PROP_NAME_providerRequestId, PROP_ID_providerRequestId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_signers] = PROP_NAME_signers;
          PROP_NAME_TO_ID.put(PROP_NAME_signers, PROP_ID_signers);
      
          PROP_ID_TO_NAME[PROP_ID_signingDeadline] = PROP_NAME_signingDeadline;
          PROP_NAME_TO_ID.put(PROP_NAME_signingDeadline, PROP_ID_signingDeadline);
      
          PROP_ID_TO_NAME[PROP_ID_completedAt] = PROP_NAME_completedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_completedAt, PROP_ID_completedAt);
      
          PROP_ID_TO_NAME[PROP_ID_certificateUrl] = PROP_NAME_certificateUrl;
          PROP_NAME_TO_ID.put(PROP_NAME_certificateUrl, PROP_ID_certificateUrl);
      
          PROP_ID_TO_NAME[PROP_ID_evidenceNo] = PROP_NAME_evidenceNo;
          PROP_NAME_TO_ID.put(PROP_NAME_evidenceNo, PROP_ID_evidenceNo);
      
          PROP_ID_TO_NAME[PROP_ID_attachmentFileId] = PROP_NAME_attachmentFileId;
          PROP_NAME_TO_ID.put(PROP_NAME_attachmentFileId, PROP_ID_attachmentFileId);
      
          PROP_ID_TO_NAME[PROP_ID_errorMsg] = PROP_NAME_errorMsg;
          PROP_NAME_TO_ID.put(PROP_NAME_errorMsg, PROP_ID_errorMsg);
      
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
    
    /* 合同版本: CONTRACT_VERSION_ID */
    private java.lang.Long _contractVersionId;
    
    /* 签名提供商: PROVIDER */
    private java.lang.String _provider;
    
    /* 提供商请求ID: PROVIDER_REQUEST_ID */
    private java.lang.String _providerRequestId;
    
    /* 签章状态: STATUS */
    private java.lang.String _status;
    
    /* 签署人(JSON): SIGNERS */
    private java.lang.String _signers;
    
    /* 签署截止日期: SIGNING_DEADLINE */
    private java.time.LocalDate _signingDeadline;
    
    /* 签署完成时间: COMPLETED_AT */
    private java.time.LocalDateTime _completedAt;
    
    /* 完成证书URL: CERTIFICATE_URL */
    private java.lang.String _certificateUrl;
    
    /* 存证编号: EVIDENCE_NO */
    private java.lang.String _evidenceNo;
    
    /* 已签署文件: ATTACHMENT_FILE_ID */
    private java.lang.String _attachmentFileId;
    
    /* 错误信息: ERROR_MSG */
    private java.lang.String _errorMsg;
    
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
    

    public _ErpCtSignatureRequest(){
        // for debug
    }

    protected ErpCtSignatureRequest newInstance(){
        ErpCtSignatureRequest entity = new ErpCtSignatureRequest();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCtSignatureRequest cloneInstance() {
        ErpCtSignatureRequest entity = newInstance();
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
      return "app.erp.contract.dao.entity.ErpCtSignatureRequest";
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
        
            case PROP_ID_contractVersionId:
               return getContractVersionId();
        
            case PROP_ID_provider:
               return getProvider();
        
            case PROP_ID_providerRequestId:
               return getProviderRequestId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_signers:
               return getSigners();
        
            case PROP_ID_signingDeadline:
               return getSigningDeadline();
        
            case PROP_ID_completedAt:
               return getCompletedAt();
        
            case PROP_ID_certificateUrl:
               return getCertificateUrl();
        
            case PROP_ID_evidenceNo:
               return getEvidenceNo();
        
            case PROP_ID_attachmentFileId:
               return getAttachmentFileId();
        
            case PROP_ID_errorMsg:
               return getErrorMsg();
        
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
        
            case PROP_ID_contractVersionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractVersionId));
               }
               setContractVersionId(typedValue);
               break;
            }
        
            case PROP_ID_provider:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_provider));
               }
               setProvider(typedValue);
               break;
            }
        
            case PROP_ID_providerRequestId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_providerRequestId));
               }
               setProviderRequestId(typedValue);
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
        
            case PROP_ID_signers:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_signers));
               }
               setSigners(typedValue);
               break;
            }
        
            case PROP_ID_signingDeadline:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_signingDeadline));
               }
               setSigningDeadline(typedValue);
               break;
            }
        
            case PROP_ID_completedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_completedAt));
               }
               setCompletedAt(typedValue);
               break;
            }
        
            case PROP_ID_certificateUrl:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_certificateUrl));
               }
               setCertificateUrl(typedValue);
               break;
            }
        
            case PROP_ID_evidenceNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_evidenceNo));
               }
               setEvidenceNo(typedValue);
               break;
            }
        
            case PROP_ID_attachmentFileId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_attachmentFileId));
               }
               setAttachmentFileId(typedValue);
               break;
            }
        
            case PROP_ID_errorMsg:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_errorMsg));
               }
               setErrorMsg(typedValue);
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
        
            case PROP_ID_contractVersionId:{
               onInitProp(propId);
               this._contractVersionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_provider:{
               onInitProp(propId);
               this._provider = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_providerRequestId:{
               onInitProp(propId);
               this._providerRequestId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_signers:{
               onInitProp(propId);
               this._signers = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_signingDeadline:{
               onInitProp(propId);
               this._signingDeadline = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_completedAt:{
               onInitProp(propId);
               this._completedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_certificateUrl:{
               onInitProp(propId);
               this._certificateUrl = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_evidenceNo:{
               onInitProp(propId);
               this._evidenceNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_attachmentFileId:{
               onInitProp(propId);
               this._attachmentFileId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_errorMsg:{
               onInitProp(propId);
               this._errorMsg = (java.lang.String)value;
               
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
     * 合同版本: CONTRACT_VERSION_ID
     */
    public final java.lang.Long getContractVersionId(){
         onPropGet(PROP_ID_contractVersionId);
         return _contractVersionId;
    }

    /**
     * 合同版本: CONTRACT_VERSION_ID
     */
    public final void setContractVersionId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractVersionId,value)){
            this._contractVersionId = value;
            internalClearRefs(PROP_ID_contractVersionId);
            
        }
    }
    
    /**
     * 签名提供商: PROVIDER
     */
    public final java.lang.String getProvider(){
         onPropGet(PROP_ID_provider);
         return _provider;
    }

    /**
     * 签名提供商: PROVIDER
     */
    public final void setProvider(java.lang.String value){
        if(onPropSet(PROP_ID_provider,value)){
            this._provider = value;
            internalClearRefs(PROP_ID_provider);
            
        }
    }
    
    /**
     * 提供商请求ID: PROVIDER_REQUEST_ID
     */
    public final java.lang.String getProviderRequestId(){
         onPropGet(PROP_ID_providerRequestId);
         return _providerRequestId;
    }

    /**
     * 提供商请求ID: PROVIDER_REQUEST_ID
     */
    public final void setProviderRequestId(java.lang.String value){
        if(onPropSet(PROP_ID_providerRequestId,value)){
            this._providerRequestId = value;
            internalClearRefs(PROP_ID_providerRequestId);
            
        }
    }
    
    /**
     * 签章状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 签章状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 签署人(JSON): SIGNERS
     */
    public final java.lang.String getSigners(){
         onPropGet(PROP_ID_signers);
         return _signers;
    }

    /**
     * 签署人(JSON): SIGNERS
     */
    public final void setSigners(java.lang.String value){
        if(onPropSet(PROP_ID_signers,value)){
            this._signers = value;
            internalClearRefs(PROP_ID_signers);
            
        }
    }
    
    /**
     * 签署截止日期: SIGNING_DEADLINE
     */
    public final java.time.LocalDate getSigningDeadline(){
         onPropGet(PROP_ID_signingDeadline);
         return _signingDeadline;
    }

    /**
     * 签署截止日期: SIGNING_DEADLINE
     */
    public final void setSigningDeadline(java.time.LocalDate value){
        if(onPropSet(PROP_ID_signingDeadline,value)){
            this._signingDeadline = value;
            internalClearRefs(PROP_ID_signingDeadline);
            
        }
    }
    
    /**
     * 签署完成时间: COMPLETED_AT
     */
    public final java.time.LocalDateTime getCompletedAt(){
         onPropGet(PROP_ID_completedAt);
         return _completedAt;
    }

    /**
     * 签署完成时间: COMPLETED_AT
     */
    public final void setCompletedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_completedAt,value)){
            this._completedAt = value;
            internalClearRefs(PROP_ID_completedAt);
            
        }
    }
    
    /**
     * 完成证书URL: CERTIFICATE_URL
     */
    public final java.lang.String getCertificateUrl(){
         onPropGet(PROP_ID_certificateUrl);
         return _certificateUrl;
    }

    /**
     * 完成证书URL: CERTIFICATE_URL
     */
    public final void setCertificateUrl(java.lang.String value){
        if(onPropSet(PROP_ID_certificateUrl,value)){
            this._certificateUrl = value;
            internalClearRefs(PROP_ID_certificateUrl);
            
        }
    }
    
    /**
     * 存证编号: EVIDENCE_NO
     */
    public final java.lang.String getEvidenceNo(){
         onPropGet(PROP_ID_evidenceNo);
         return _evidenceNo;
    }

    /**
     * 存证编号: EVIDENCE_NO
     */
    public final void setEvidenceNo(java.lang.String value){
        if(onPropSet(PROP_ID_evidenceNo,value)){
            this._evidenceNo = value;
            internalClearRefs(PROP_ID_evidenceNo);
            
        }
    }
    
    /**
     * 已签署文件: ATTACHMENT_FILE_ID
     */
    public final java.lang.String getAttachmentFileId(){
         onPropGet(PROP_ID_attachmentFileId);
         return _attachmentFileId;
    }

    /**
     * 已签署文件: ATTACHMENT_FILE_ID
     */
    public final void setAttachmentFileId(java.lang.String value){
        if(onPropSet(PROP_ID_attachmentFileId,value)){
            this._attachmentFileId = value;
            internalClearRefs(PROP_ID_attachmentFileId);
            
        }
    }
    
    /**
     * 错误信息: ERROR_MSG
     */
    public final java.lang.String getErrorMsg(){
         onPropGet(PROP_ID_errorMsg);
         return _errorMsg;
    }

    /**
     * 错误信息: ERROR_MSG
     */
    public final void setErrorMsg(java.lang.String value){
        if(onPropSet(PROP_ID_errorMsg,value)){
            this._errorMsg = value;
            internalClearRefs(PROP_ID_errorMsg);
            
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
    public final app.erp.contract.dao.entity.ErpCtContractVersion getContractVersion(){
       return (app.erp.contract.dao.entity.ErpCtContractVersion)internalGetRefEntity(PROP_NAME_contractVersion);
    }

    public final void setContractVersion(app.erp.contract.dao.entity.ErpCtContractVersion refEntity){
   
           if(refEntity == null){
           
                   this.setContractVersionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contractVersion, refEntity,()->{
           
                           this.setContractVersionId(refEntity.getId());
                       
           });
           }
       
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
    public final app.erp.contract.dao.entity.ErpCtSignatureRequest getProviderRequest(){
       return (app.erp.contract.dao.entity.ErpCtSignatureRequest)internalGetRefEntity(PROP_NAME_providerRequest);
    }

    public final void setProviderRequest(app.erp.contract.dao.entity.ErpCtSignatureRequest refEntity){
   
           if(refEntity == null){
           
                   this.setProviderRequestId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_providerRequest, refEntity,()->{
           
                           this.orm_propValue(PROP_ID_providerRequestId,
                           refEntity.getId());
                       
           });
           }
       
    }
       
   private io.nop.orm.component.OrmFileComponent _attachmentFileIdComponent;

   private static Map<String,Integer> COMPONENT_PROP_ID_MAP_attachmentFileIdComponent = new HashMap<>();
   static{
      
         COMPONENT_PROP_ID_MAP_attachmentFileIdComponent.put(io.nop.orm.component.OrmFileComponent.PROP_NAME_filePath,PROP_ID_attachmentFileId);
      
   }

   public final io.nop.orm.component.OrmFileComponent getAttachmentFileIdComponent(){
      if(_attachmentFileIdComponent == null){
          _attachmentFileIdComponent = new io.nop.orm.component.OrmFileComponent();
          _attachmentFileIdComponent.bindToEntity(this, COMPONENT_PROP_ID_MAP_attachmentFileIdComponent);
      }
      return _attachmentFileIdComponent;
   }

}
// resume CPD analysis - CPD-ON
