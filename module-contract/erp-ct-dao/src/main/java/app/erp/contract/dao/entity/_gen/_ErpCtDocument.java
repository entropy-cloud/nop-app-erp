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

import app.erp.contract.dao.entity.ErpCtDocument;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  合同文档: erp_ct_document
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCtDocument extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 关联合同: CONTRACT_ID BIGINT */
    public static final String PROP_NAME_contractId = "contractId";
    public static final int PROP_ID_contractId = 3;
    
    /* 文档编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 4;
    
    /* 文档名称: DOC_NAME VARCHAR */
    public static final String PROP_NAME_docName = "docName";
    public static final int PROP_ID_docName = 5;
    
    /* 文档类型: DOC_TYPE VARCHAR */
    public static final String PROP_NAME_docType = "docType";
    public static final int PROP_ID_docType = 6;
    
    /* 附件: ATTACHMENT_ID BIGINT */
    public static final String PROP_NAME_attachmentId = "attachmentId";
    public static final int PROP_ID_attachmentId = 7;
    
    /* 文件大小(字节): FILE_SIZE BIGINT */
    public static final String PROP_NAME_fileSize = "fileSize";
    public static final int PROP_ID_fileSize = 8;
    
    /* 文件哈希: FILE_HASH VARCHAR */
    public static final String PROP_NAME_fileHash = "fileHash";
    public static final int PROP_ID_fileHash = 9;
    
    /* MIME类型: MIME_TYPE VARCHAR */
    public static final String PROP_NAME_mimeType = "mimeType";
    public static final int PROP_ID_mimeType = 10;
    
    /* OCR文本: OCR_TEXT VARCHAR */
    public static final String PROP_NAME_ocrText = "ocrText";
    public static final int PROP_ID_ocrText = 11;
    
    /* OCR状态: OCR_STATUS VARCHAR */
    public static final String PROP_NAME_ocrStatus = "ocrStatus";
    public static final int PROP_ID_ocrStatus = 12;
    
    /* 全文检索: FULL_TEXT_SEARCH VARCHAR */
    public static final String PROP_NAME_fullTextSearch = "fullTextSearch";
    public static final int PROP_ID_fullTextSearch = 13;
    
    /* 元数据标签(JSON): METADATA_TAGS VARCHAR */
    public static final String PROP_NAME_metadataTags = "metadataTags";
    public static final int PROP_ID_metadataTags = 14;
    
    /* 保留截止日期: RETENTION_DATE DATE */
    public static final String PROP_NAME_retentionDate = "retentionDate";
    public static final int PROP_ID_retentionDate = 15;
    
    /* 归档日期: ARCHIVE_DATE DATE */
    public static final String PROP_NAME_archiveDate = "archiveDate";
    public static final int PROP_ID_archiveDate = 16;
    
    /* 销毁日期: PURGE_DATE DATE */
    public static final String PROP_NAME_purgeDate = "purgeDate";
    public static final int PROP_ID_purgeDate = 17;
    
    /* 已归档: IS_ARCHIVED BOOLEAN */
    public static final String PROP_NAME_isArchived = "isArchived";
    public static final int PROP_ID_isArchived = 18;
    
    /* 文档版本: VERSION_NO INTEGER */
    public static final String PROP_NAME_versionNo = "versionNo";
    public static final int PROP_ID_versionNo = 19;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 20;
    
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

    
    /* relation:  */
    public static final String PROP_NAME_contract = "contract";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_contractId] = PROP_NAME_contractId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractId, PROP_ID_contractId);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_docName] = PROP_NAME_docName;
          PROP_NAME_TO_ID.put(PROP_NAME_docName, PROP_ID_docName);
      
          PROP_ID_TO_NAME[PROP_ID_docType] = PROP_NAME_docType;
          PROP_NAME_TO_ID.put(PROP_NAME_docType, PROP_ID_docType);
      
          PROP_ID_TO_NAME[PROP_ID_attachmentId] = PROP_NAME_attachmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_attachmentId, PROP_ID_attachmentId);
      
          PROP_ID_TO_NAME[PROP_ID_fileSize] = PROP_NAME_fileSize;
          PROP_NAME_TO_ID.put(PROP_NAME_fileSize, PROP_ID_fileSize);
      
          PROP_ID_TO_NAME[PROP_ID_fileHash] = PROP_NAME_fileHash;
          PROP_NAME_TO_ID.put(PROP_NAME_fileHash, PROP_ID_fileHash);
      
          PROP_ID_TO_NAME[PROP_ID_mimeType] = PROP_NAME_mimeType;
          PROP_NAME_TO_ID.put(PROP_NAME_mimeType, PROP_ID_mimeType);
      
          PROP_ID_TO_NAME[PROP_ID_ocrText] = PROP_NAME_ocrText;
          PROP_NAME_TO_ID.put(PROP_NAME_ocrText, PROP_ID_ocrText);
      
          PROP_ID_TO_NAME[PROP_ID_ocrStatus] = PROP_NAME_ocrStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_ocrStatus, PROP_ID_ocrStatus);
      
          PROP_ID_TO_NAME[PROP_ID_fullTextSearch] = PROP_NAME_fullTextSearch;
          PROP_NAME_TO_ID.put(PROP_NAME_fullTextSearch, PROP_ID_fullTextSearch);
      
          PROP_ID_TO_NAME[PROP_ID_metadataTags] = PROP_NAME_metadataTags;
          PROP_NAME_TO_ID.put(PROP_NAME_metadataTags, PROP_ID_metadataTags);
      
          PROP_ID_TO_NAME[PROP_ID_retentionDate] = PROP_NAME_retentionDate;
          PROP_NAME_TO_ID.put(PROP_NAME_retentionDate, PROP_ID_retentionDate);
      
          PROP_ID_TO_NAME[PROP_ID_archiveDate] = PROP_NAME_archiveDate;
          PROP_NAME_TO_ID.put(PROP_NAME_archiveDate, PROP_ID_archiveDate);
      
          PROP_ID_TO_NAME[PROP_ID_purgeDate] = PROP_NAME_purgeDate;
          PROP_NAME_TO_ID.put(PROP_NAME_purgeDate, PROP_ID_purgeDate);
      
          PROP_ID_TO_NAME[PROP_ID_isArchived] = PROP_NAME_isArchived;
          PROP_NAME_TO_ID.put(PROP_NAME_isArchived, PROP_ID_isArchived);
      
          PROP_ID_TO_NAME[PROP_ID_versionNo] = PROP_NAME_versionNo;
          PROP_NAME_TO_ID.put(PROP_NAME_versionNo, PROP_ID_versionNo);
      
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
    
    /* 关联合同: CONTRACT_ID */
    private java.lang.Long _contractId;
    
    /* 文档编码: CODE */
    private java.lang.String _code;
    
    /* 文档名称: DOC_NAME */
    private java.lang.String _docName;
    
    /* 文档类型: DOC_TYPE */
    private java.lang.String _docType;
    
    /* 附件: ATTACHMENT_ID */
    private java.lang.Long _attachmentId;
    
    /* 文件大小(字节): FILE_SIZE */
    private java.lang.Long _fileSize;
    
    /* 文件哈希: FILE_HASH */
    private java.lang.String _fileHash;
    
    /* MIME类型: MIME_TYPE */
    private java.lang.String _mimeType;
    
    /* OCR文本: OCR_TEXT */
    private java.lang.String _ocrText;
    
    /* OCR状态: OCR_STATUS */
    private java.lang.String _ocrStatus;
    
    /* 全文检索: FULL_TEXT_SEARCH */
    private java.lang.String _fullTextSearch;
    
    /* 元数据标签(JSON): METADATA_TAGS */
    private java.lang.String _metadataTags;
    
    /* 保留截止日期: RETENTION_DATE */
    private java.time.LocalDate _retentionDate;
    
    /* 归档日期: ARCHIVE_DATE */
    private java.time.LocalDate _archiveDate;
    
    /* 销毁日期: PURGE_DATE */
    private java.time.LocalDate _purgeDate;
    
    /* 已归档: IS_ARCHIVED */
    private java.lang.Boolean _isArchived;
    
    /* 文档版本: VERSION_NO */
    private java.lang.Integer _versionNo;
    
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
    

    public _ErpCtDocument(){
        // for debug
    }

    protected ErpCtDocument newInstance(){
        ErpCtDocument entity = new ErpCtDocument();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCtDocument cloneInstance() {
        ErpCtDocument entity = newInstance();
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
      return "app.erp.contract.dao.entity.ErpCtDocument";
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
        
            case PROP_ID_contractId:
               return getContractId();
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_docName:
               return getDocName();
        
            case PROP_ID_docType:
               return getDocType();
        
            case PROP_ID_attachmentId:
               return getAttachmentId();
        
            case PROP_ID_fileSize:
               return getFileSize();
        
            case PROP_ID_fileHash:
               return getFileHash();
        
            case PROP_ID_mimeType:
               return getMimeType();
        
            case PROP_ID_ocrText:
               return getOcrText();
        
            case PROP_ID_ocrStatus:
               return getOcrStatus();
        
            case PROP_ID_fullTextSearch:
               return getFullTextSearch();
        
            case PROP_ID_metadataTags:
               return getMetadataTags();
        
            case PROP_ID_retentionDate:
               return getRetentionDate();
        
            case PROP_ID_archiveDate:
               return getArchiveDate();
        
            case PROP_ID_purgeDate:
               return getPurgeDate();
        
            case PROP_ID_isArchived:
               return getIsArchived();
        
            case PROP_ID_versionNo:
               return getVersionNo();
        
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
        
            case PROP_ID_contractId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractId));
               }
               setContractId(typedValue);
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
        
            case PROP_ID_docName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docName));
               }
               setDocName(typedValue);
               break;
            }
        
            case PROP_ID_docType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docType));
               }
               setDocType(typedValue);
               break;
            }
        
            case PROP_ID_attachmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_attachmentId));
               }
               setAttachmentId(typedValue);
               break;
            }
        
            case PROP_ID_fileSize:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fileSize));
               }
               setFileSize(typedValue);
               break;
            }
        
            case PROP_ID_fileHash:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileHash));
               }
               setFileHash(typedValue);
               break;
            }
        
            case PROP_ID_mimeType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_mimeType));
               }
               setMimeType(typedValue);
               break;
            }
        
            case PROP_ID_ocrText:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ocrText));
               }
               setOcrText(typedValue);
               break;
            }
        
            case PROP_ID_ocrStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ocrStatus));
               }
               setOcrStatus(typedValue);
               break;
            }
        
            case PROP_ID_fullTextSearch:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fullTextSearch));
               }
               setFullTextSearch(typedValue);
               break;
            }
        
            case PROP_ID_metadataTags:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_metadataTags));
               }
               setMetadataTags(typedValue);
               break;
            }
        
            case PROP_ID_retentionDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_retentionDate));
               }
               setRetentionDate(typedValue);
               break;
            }
        
            case PROP_ID_archiveDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_archiveDate));
               }
               setArchiveDate(typedValue);
               break;
            }
        
            case PROP_ID_purgeDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_purgeDate));
               }
               setPurgeDate(typedValue);
               break;
            }
        
            case PROP_ID_isArchived:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isArchived));
               }
               setIsArchived(typedValue);
               break;
            }
        
            case PROP_ID_versionNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_versionNo));
               }
               setVersionNo(typedValue);
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
        
            case PROP_ID_contractId:{
               onInitProp(propId);
               this._contractId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docName:{
               onInitProp(propId);
               this._docName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docType:{
               onInitProp(propId);
               this._docType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_attachmentId:{
               onInitProp(propId);
               this._attachmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fileSize:{
               onInitProp(propId);
               this._fileSize = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fileHash:{
               onInitProp(propId);
               this._fileHash = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mimeType:{
               onInitProp(propId);
               this._mimeType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ocrText:{
               onInitProp(propId);
               this._ocrText = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ocrStatus:{
               onInitProp(propId);
               this._ocrStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fullTextSearch:{
               onInitProp(propId);
               this._fullTextSearch = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_metadataTags:{
               onInitProp(propId);
               this._metadataTags = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_retentionDate:{
               onInitProp(propId);
               this._retentionDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_archiveDate:{
               onInitProp(propId);
               this._archiveDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_purgeDate:{
               onInitProp(propId);
               this._purgeDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_isArchived:{
               onInitProp(propId);
               this._isArchived = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_versionNo:{
               onInitProp(propId);
               this._versionNo = (java.lang.Integer)value;
               
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
     * 关联合同: CONTRACT_ID
     */
    public final java.lang.Long getContractId(){
         onPropGet(PROP_ID_contractId);
         return _contractId;
    }

    /**
     * 关联合同: CONTRACT_ID
     */
    public final void setContractId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractId,value)){
            this._contractId = value;
            internalClearRefs(PROP_ID_contractId);
            
        }
    }
    
    /**
     * 文档编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 文档编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 文档名称: DOC_NAME
     */
    public final java.lang.String getDocName(){
         onPropGet(PROP_ID_docName);
         return _docName;
    }

    /**
     * 文档名称: DOC_NAME
     */
    public final void setDocName(java.lang.String value){
        if(onPropSet(PROP_ID_docName,value)){
            this._docName = value;
            internalClearRefs(PROP_ID_docName);
            
        }
    }
    
    /**
     * 文档类型: DOC_TYPE
     */
    public final java.lang.String getDocType(){
         onPropGet(PROP_ID_docType);
         return _docType;
    }

    /**
     * 文档类型: DOC_TYPE
     */
    public final void setDocType(java.lang.String value){
        if(onPropSet(PROP_ID_docType,value)){
            this._docType = value;
            internalClearRefs(PROP_ID_docType);
            
        }
    }
    
    /**
     * 附件: ATTACHMENT_ID
     */
    public final java.lang.Long getAttachmentId(){
         onPropGet(PROP_ID_attachmentId);
         return _attachmentId;
    }

    /**
     * 附件: ATTACHMENT_ID
     */
    public final void setAttachmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_attachmentId,value)){
            this._attachmentId = value;
            internalClearRefs(PROP_ID_attachmentId);
            
        }
    }
    
    /**
     * 文件大小(字节): FILE_SIZE
     */
    public final java.lang.Long getFileSize(){
         onPropGet(PROP_ID_fileSize);
         return _fileSize;
    }

    /**
     * 文件大小(字节): FILE_SIZE
     */
    public final void setFileSize(java.lang.Long value){
        if(onPropSet(PROP_ID_fileSize,value)){
            this._fileSize = value;
            internalClearRefs(PROP_ID_fileSize);
            
        }
    }
    
    /**
     * 文件哈希: FILE_HASH
     */
    public final java.lang.String getFileHash(){
         onPropGet(PROP_ID_fileHash);
         return _fileHash;
    }

    /**
     * 文件哈希: FILE_HASH
     */
    public final void setFileHash(java.lang.String value){
        if(onPropSet(PROP_ID_fileHash,value)){
            this._fileHash = value;
            internalClearRefs(PROP_ID_fileHash);
            
        }
    }
    
    /**
     * MIME类型: MIME_TYPE
     */
    public final java.lang.String getMimeType(){
         onPropGet(PROP_ID_mimeType);
         return _mimeType;
    }

    /**
     * MIME类型: MIME_TYPE
     */
    public final void setMimeType(java.lang.String value){
        if(onPropSet(PROP_ID_mimeType,value)){
            this._mimeType = value;
            internalClearRefs(PROP_ID_mimeType);
            
        }
    }
    
    /**
     * OCR文本: OCR_TEXT
     */
    public final java.lang.String getOcrText(){
         onPropGet(PROP_ID_ocrText);
         return _ocrText;
    }

    /**
     * OCR文本: OCR_TEXT
     */
    public final void setOcrText(java.lang.String value){
        if(onPropSet(PROP_ID_ocrText,value)){
            this._ocrText = value;
            internalClearRefs(PROP_ID_ocrText);
            
        }
    }
    
    /**
     * OCR状态: OCR_STATUS
     */
    public final java.lang.String getOcrStatus(){
         onPropGet(PROP_ID_ocrStatus);
         return _ocrStatus;
    }

    /**
     * OCR状态: OCR_STATUS
     */
    public final void setOcrStatus(java.lang.String value){
        if(onPropSet(PROP_ID_ocrStatus,value)){
            this._ocrStatus = value;
            internalClearRefs(PROP_ID_ocrStatus);
            
        }
    }
    
    /**
     * 全文检索: FULL_TEXT_SEARCH
     */
    public final java.lang.String getFullTextSearch(){
         onPropGet(PROP_ID_fullTextSearch);
         return _fullTextSearch;
    }

    /**
     * 全文检索: FULL_TEXT_SEARCH
     */
    public final void setFullTextSearch(java.lang.String value){
        if(onPropSet(PROP_ID_fullTextSearch,value)){
            this._fullTextSearch = value;
            internalClearRefs(PROP_ID_fullTextSearch);
            
        }
    }
    
    /**
     * 元数据标签(JSON): METADATA_TAGS
     */
    public final java.lang.String getMetadataTags(){
         onPropGet(PROP_ID_metadataTags);
         return _metadataTags;
    }

    /**
     * 元数据标签(JSON): METADATA_TAGS
     */
    public final void setMetadataTags(java.lang.String value){
        if(onPropSet(PROP_ID_metadataTags,value)){
            this._metadataTags = value;
            internalClearRefs(PROP_ID_metadataTags);
            
        }
    }
    
    /**
     * 保留截止日期: RETENTION_DATE
     */
    public final java.time.LocalDate getRetentionDate(){
         onPropGet(PROP_ID_retentionDate);
         return _retentionDate;
    }

    /**
     * 保留截止日期: RETENTION_DATE
     */
    public final void setRetentionDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_retentionDate,value)){
            this._retentionDate = value;
            internalClearRefs(PROP_ID_retentionDate);
            
        }
    }
    
    /**
     * 归档日期: ARCHIVE_DATE
     */
    public final java.time.LocalDate getArchiveDate(){
         onPropGet(PROP_ID_archiveDate);
         return _archiveDate;
    }

    /**
     * 归档日期: ARCHIVE_DATE
     */
    public final void setArchiveDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_archiveDate,value)){
            this._archiveDate = value;
            internalClearRefs(PROP_ID_archiveDate);
            
        }
    }
    
    /**
     * 销毁日期: PURGE_DATE
     */
    public final java.time.LocalDate getPurgeDate(){
         onPropGet(PROP_ID_purgeDate);
         return _purgeDate;
    }

    /**
     * 销毁日期: PURGE_DATE
     */
    public final void setPurgeDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_purgeDate,value)){
            this._purgeDate = value;
            internalClearRefs(PROP_ID_purgeDate);
            
        }
    }
    
    /**
     * 已归档: IS_ARCHIVED
     */
    public final java.lang.Boolean getIsArchived(){
         onPropGet(PROP_ID_isArchived);
         return _isArchived;
    }

    /**
     * 已归档: IS_ARCHIVED
     */
    public final void setIsArchived(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isArchived,value)){
            this._isArchived = value;
            internalClearRefs(PROP_ID_isArchived);
            
        }
    }
    
    /**
     * 文档版本: VERSION_NO
     */
    public final java.lang.Integer getVersionNo(){
         onPropGet(PROP_ID_versionNo);
         return _versionNo;
    }

    /**
     * 文档版本: VERSION_NO
     */
    public final void setVersionNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_versionNo,value)){
            this._versionNo = value;
            internalClearRefs(PROP_ID_versionNo);
            
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
    public final app.erp.contract.dao.entity.ErpCtContract getContract(){
       return (app.erp.contract.dao.entity.ErpCtContract)internalGetRefEntity(PROP_NAME_contract);
    }

    public final void setContract(app.erp.contract.dao.entity.ErpCtContract refEntity){
   
           if(refEntity == null){
           
                   this.setContractId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contract, refEntity,()->{
           
                           this.setContractId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
