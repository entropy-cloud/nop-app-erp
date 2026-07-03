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

import app.erp.b2b.dao.entity.ErpB2bTestExchange;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  测试消息交换: erp_b2b_test_exchange
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bTestExchange extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 伙伴档案: PARTNER_PROFILE_ID BIGINT */
    public static final String PROP_NAME_partnerProfileId = "partnerProfileId";
    public static final int PROP_ID_partnerProfileId = 2;
    
    /* 方向: DIRECTION VARCHAR */
    public static final String PROP_NAME_direction = "direction";
    public static final int PROP_ID_direction = 3;
    
    /* EDI格式: FORMAT_CODE VARCHAR */
    public static final String PROP_NAME_formatCode = "formatCode";
    public static final int PROP_ID_formatCode = 4;
    
    /* 测试用例编号: TEST_CASE_CODE VARCHAR */
    public static final String PROP_NAME_testCaseCode = "testCaseCode";
    public static final int PROP_ID_testCaseCode = 5;
    
    /* 发送报文: SENT_PAYLOAD CLOB */
    public static final String PROP_NAME_sentPayload = "sentPayload";
    public static final int PROP_ID_sentPayload = 6;
    
    /* 接收报文: RECEIVED_PAYLOAD CLOB */
    public static final String PROP_NAME_receivedPayload = "receivedPayload";
    public static final int PROP_ID_receivedPayload = 7;
    
    /* 预期结果: EXPECTED_RESULT VARCHAR */
    public static final String PROP_NAME_expectedResult = "expectedResult";
    public static final int PROP_ID_expectedResult = 8;
    
    /* 实际结果: ACTUAL_RESULT VARCHAR */
    public static final String PROP_NAME_actualResult = "actualResult";
    public static final int PROP_ID_actualResult = 9;
    
    /* 测试通过: PASSED BOOLEAN */
    public static final String PROP_NAME_passed = "passed";
    public static final int PROP_ID_passed = 10;
    
    /* 测试人: TESTED_BY VARCHAR */
    public static final String PROP_NAME_testedBy = "testedBy";
    public static final int PROP_ID_testedBy = 11;
    
    /* 测试时间: TESTED_AT DATETIME */
    public static final String PROP_NAME_testedAt = "testedAt";
    public static final int PROP_ID_testedAt = 12;
    
    /* 备注: NOTES VARCHAR */
    public static final String PROP_NAME_notes = "notes";
    public static final int PROP_ID_notes = 13;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 14;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 15;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 16;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 17;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 18;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_partnerProfile = "partnerProfile";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerProfileId] = PROP_NAME_partnerProfileId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerProfileId, PROP_ID_partnerProfileId);
      
          PROP_ID_TO_NAME[PROP_ID_direction] = PROP_NAME_direction;
          PROP_NAME_TO_ID.put(PROP_NAME_direction, PROP_ID_direction);
      
          PROP_ID_TO_NAME[PROP_ID_formatCode] = PROP_NAME_formatCode;
          PROP_NAME_TO_ID.put(PROP_NAME_formatCode, PROP_ID_formatCode);
      
          PROP_ID_TO_NAME[PROP_ID_testCaseCode] = PROP_NAME_testCaseCode;
          PROP_NAME_TO_ID.put(PROP_NAME_testCaseCode, PROP_ID_testCaseCode);
      
          PROP_ID_TO_NAME[PROP_ID_sentPayload] = PROP_NAME_sentPayload;
          PROP_NAME_TO_ID.put(PROP_NAME_sentPayload, PROP_ID_sentPayload);
      
          PROP_ID_TO_NAME[PROP_ID_receivedPayload] = PROP_NAME_receivedPayload;
          PROP_NAME_TO_ID.put(PROP_NAME_receivedPayload, PROP_ID_receivedPayload);
      
          PROP_ID_TO_NAME[PROP_ID_expectedResult] = PROP_NAME_expectedResult;
          PROP_NAME_TO_ID.put(PROP_NAME_expectedResult, PROP_ID_expectedResult);
      
          PROP_ID_TO_NAME[PROP_ID_actualResult] = PROP_NAME_actualResult;
          PROP_NAME_TO_ID.put(PROP_NAME_actualResult, PROP_ID_actualResult);
      
          PROP_ID_TO_NAME[PROP_ID_passed] = PROP_NAME_passed;
          PROP_NAME_TO_ID.put(PROP_NAME_passed, PROP_ID_passed);
      
          PROP_ID_TO_NAME[PROP_ID_testedBy] = PROP_NAME_testedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_testedBy, PROP_ID_testedBy);
      
          PROP_ID_TO_NAME[PROP_ID_testedAt] = PROP_NAME_testedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_testedAt, PROP_ID_testedAt);
      
          PROP_ID_TO_NAME[PROP_ID_notes] = PROP_NAME_notes;
          PROP_NAME_TO_ID.put(PROP_NAME_notes, PROP_ID_notes);
      
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
    
    /* 伙伴档案: PARTNER_PROFILE_ID */
    private java.lang.Long _partnerProfileId;
    
    /* 方向: DIRECTION */
    private java.lang.String _direction;
    
    /* EDI格式: FORMAT_CODE */
    private java.lang.String _formatCode;
    
    /* 测试用例编号: TEST_CASE_CODE */
    private java.lang.String _testCaseCode;
    
    /* 发送报文: SENT_PAYLOAD */
    private java.lang.String _sentPayload;
    
    /* 接收报文: RECEIVED_PAYLOAD */
    private java.lang.String _receivedPayload;
    
    /* 预期结果: EXPECTED_RESULT */
    private java.lang.String _expectedResult;
    
    /* 实际结果: ACTUAL_RESULT */
    private java.lang.String _actualResult;
    
    /* 测试通过: PASSED */
    private java.lang.Boolean _passed;
    
    /* 测试人: TESTED_BY */
    private java.lang.String _testedBy;
    
    /* 测试时间: TESTED_AT */
    private java.time.LocalDateTime _testedAt;
    
    /* 备注: NOTES */
    private java.lang.String _notes;
    
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
    

    public _ErpB2bTestExchange(){
        // for debug
    }

    protected ErpB2bTestExchange newInstance(){
        ErpB2bTestExchange entity = new ErpB2bTestExchange();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bTestExchange cloneInstance() {
        ErpB2bTestExchange entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bTestExchange";
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
        
            case PROP_ID_partnerProfileId:
               return getPartnerProfileId();
        
            case PROP_ID_direction:
               return getDirection();
        
            case PROP_ID_formatCode:
               return getFormatCode();
        
            case PROP_ID_testCaseCode:
               return getTestCaseCode();
        
            case PROP_ID_sentPayload:
               return getSentPayload();
        
            case PROP_ID_receivedPayload:
               return getReceivedPayload();
        
            case PROP_ID_expectedResult:
               return getExpectedResult();
        
            case PROP_ID_actualResult:
               return getActualResult();
        
            case PROP_ID_passed:
               return getPassed();
        
            case PROP_ID_testedBy:
               return getTestedBy();
        
            case PROP_ID_testedAt:
               return getTestedAt();
        
            case PROP_ID_notes:
               return getNotes();
        
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
        
            case PROP_ID_partnerProfileId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerProfileId));
               }
               setPartnerProfileId(typedValue);
               break;
            }
        
            case PROP_ID_direction:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_direction));
               }
               setDirection(typedValue);
               break;
            }
        
            case PROP_ID_formatCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_formatCode));
               }
               setFormatCode(typedValue);
               break;
            }
        
            case PROP_ID_testCaseCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_testCaseCode));
               }
               setTestCaseCode(typedValue);
               break;
            }
        
            case PROP_ID_sentPayload:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sentPayload));
               }
               setSentPayload(typedValue);
               break;
            }
        
            case PROP_ID_receivedPayload:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receivedPayload));
               }
               setReceivedPayload(typedValue);
               break;
            }
        
            case PROP_ID_expectedResult:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_expectedResult));
               }
               setExpectedResult(typedValue);
               break;
            }
        
            case PROP_ID_actualResult:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actualResult));
               }
               setActualResult(typedValue);
               break;
            }
        
            case PROP_ID_passed:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_passed));
               }
               setPassed(typedValue);
               break;
            }
        
            case PROP_ID_testedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_testedBy));
               }
               setTestedBy(typedValue);
               break;
            }
        
            case PROP_ID_testedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_testedAt));
               }
               setTestedAt(typedValue);
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
        
            case PROP_ID_partnerProfileId:{
               onInitProp(propId);
               this._partnerProfileId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_direction:{
               onInitProp(propId);
               this._direction = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_formatCode:{
               onInitProp(propId);
               this._formatCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_testCaseCode:{
               onInitProp(propId);
               this._testCaseCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sentPayload:{
               onInitProp(propId);
               this._sentPayload = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receivedPayload:{
               onInitProp(propId);
               this._receivedPayload = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_expectedResult:{
               onInitProp(propId);
               this._expectedResult = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_actualResult:{
               onInitProp(propId);
               this._actualResult = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_passed:{
               onInitProp(propId);
               this._passed = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_testedBy:{
               onInitProp(propId);
               this._testedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_testedAt:{
               onInitProp(propId);
               this._testedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_notes:{
               onInitProp(propId);
               this._notes = (java.lang.String)value;
               
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
     * 伙伴档案: PARTNER_PROFILE_ID
     */
    public final java.lang.Long getPartnerProfileId(){
         onPropGet(PROP_ID_partnerProfileId);
         return _partnerProfileId;
    }

    /**
     * 伙伴档案: PARTNER_PROFILE_ID
     */
    public final void setPartnerProfileId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerProfileId,value)){
            this._partnerProfileId = value;
            internalClearRefs(PROP_ID_partnerProfileId);
            
        }
    }
    
    /**
     * 方向: DIRECTION
     */
    public final java.lang.String getDirection(){
         onPropGet(PROP_ID_direction);
         return _direction;
    }

    /**
     * 方向: DIRECTION
     */
    public final void setDirection(java.lang.String value){
        if(onPropSet(PROP_ID_direction,value)){
            this._direction = value;
            internalClearRefs(PROP_ID_direction);
            
        }
    }
    
    /**
     * EDI格式: FORMAT_CODE
     */
    public final java.lang.String getFormatCode(){
         onPropGet(PROP_ID_formatCode);
         return _formatCode;
    }

    /**
     * EDI格式: FORMAT_CODE
     */
    public final void setFormatCode(java.lang.String value){
        if(onPropSet(PROP_ID_formatCode,value)){
            this._formatCode = value;
            internalClearRefs(PROP_ID_formatCode);
            
        }
    }
    
    /**
     * 测试用例编号: TEST_CASE_CODE
     */
    public final java.lang.String getTestCaseCode(){
         onPropGet(PROP_ID_testCaseCode);
         return _testCaseCode;
    }

    /**
     * 测试用例编号: TEST_CASE_CODE
     */
    public final void setTestCaseCode(java.lang.String value){
        if(onPropSet(PROP_ID_testCaseCode,value)){
            this._testCaseCode = value;
            internalClearRefs(PROP_ID_testCaseCode);
            
        }
    }
    
    /**
     * 发送报文: SENT_PAYLOAD
     */
    public final java.lang.String getSentPayload(){
         onPropGet(PROP_ID_sentPayload);
         return _sentPayload;
    }

    /**
     * 发送报文: SENT_PAYLOAD
     */
    public final void setSentPayload(java.lang.String value){
        if(onPropSet(PROP_ID_sentPayload,value)){
            this._sentPayload = value;
            internalClearRefs(PROP_ID_sentPayload);
            
        }
    }
    
    /**
     * 接收报文: RECEIVED_PAYLOAD
     */
    public final java.lang.String getReceivedPayload(){
         onPropGet(PROP_ID_receivedPayload);
         return _receivedPayload;
    }

    /**
     * 接收报文: RECEIVED_PAYLOAD
     */
    public final void setReceivedPayload(java.lang.String value){
        if(onPropSet(PROP_ID_receivedPayload,value)){
            this._receivedPayload = value;
            internalClearRefs(PROP_ID_receivedPayload);
            
        }
    }
    
    /**
     * 预期结果: EXPECTED_RESULT
     */
    public final java.lang.String getExpectedResult(){
         onPropGet(PROP_ID_expectedResult);
         return _expectedResult;
    }

    /**
     * 预期结果: EXPECTED_RESULT
     */
    public final void setExpectedResult(java.lang.String value){
        if(onPropSet(PROP_ID_expectedResult,value)){
            this._expectedResult = value;
            internalClearRefs(PROP_ID_expectedResult);
            
        }
    }
    
    /**
     * 实际结果: ACTUAL_RESULT
     */
    public final java.lang.String getActualResult(){
         onPropGet(PROP_ID_actualResult);
         return _actualResult;
    }

    /**
     * 实际结果: ACTUAL_RESULT
     */
    public final void setActualResult(java.lang.String value){
        if(onPropSet(PROP_ID_actualResult,value)){
            this._actualResult = value;
            internalClearRefs(PROP_ID_actualResult);
            
        }
    }
    
    /**
     * 测试通过: PASSED
     */
    public final java.lang.Boolean getPassed(){
         onPropGet(PROP_ID_passed);
         return _passed;
    }

    /**
     * 测试通过: PASSED
     */
    public final void setPassed(java.lang.Boolean value){
        if(onPropSet(PROP_ID_passed,value)){
            this._passed = value;
            internalClearRefs(PROP_ID_passed);
            
        }
    }
    
    /**
     * 测试人: TESTED_BY
     */
    public final java.lang.String getTestedBy(){
         onPropGet(PROP_ID_testedBy);
         return _testedBy;
    }

    /**
     * 测试人: TESTED_BY
     */
    public final void setTestedBy(java.lang.String value){
        if(onPropSet(PROP_ID_testedBy,value)){
            this._testedBy = value;
            internalClearRefs(PROP_ID_testedBy);
            
        }
    }
    
    /**
     * 测试时间: TESTED_AT
     */
    public final java.time.LocalDateTime getTestedAt(){
         onPropGet(PROP_ID_testedAt);
         return _testedAt;
    }

    /**
     * 测试时间: TESTED_AT
     */
    public final void setTestedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_testedAt,value)){
            this._testedAt = value;
            internalClearRefs(PROP_ID_testedAt);
            
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
    public final app.erp.b2b.dao.entity.ErpB2bPartnerProfile getPartnerProfile(){
       return (app.erp.b2b.dao.entity.ErpB2bPartnerProfile)internalGetRefEntity(PROP_NAME_partnerProfile);
    }

    public final void setPartnerProfile(app.erp.b2b.dao.entity.ErpB2bPartnerProfile refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerProfileId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partnerProfile, refEntity,()->{
           
                           this.setPartnerProfileId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
