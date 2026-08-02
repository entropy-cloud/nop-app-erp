//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bEdiLogOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _ediDocId;

    
        @PropMeta(propId=2)
    
        public Long getEdiDocId(){
            return _ediDocId;
        }

        public void setEdiDocId(Long value){
            this._ediDocId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _direction;

    
        @PropMeta(propId=4)
    
        public String getDirection(){
            return _direction;
        }

        public void setDirection(String value){
            this._direction = value;
        }


        private String _direction_label;

    
        public String getDirection_label(){
            return _direction_label;
        }

        public void setDirection_label(String value){
            this._direction_label = value;
        }


        private String _requestPayload;

    
        @PropMeta(propId=5)
    
        public String getRequestPayload(){
            return _requestPayload;
        }

        public void setRequestPayload(String value){
            this._requestPayload = value;
        }


        private String _responsePayload;

    
        @PropMeta(propId=6)
    
        public String getResponsePayload(){
            return _responsePayload;
        }

        public void setResponsePayload(String value){
            this._responsePayload = value;
        }


        private String _resultCode;

    
        @PropMeta(propId=7)
    
        public String getResultCode(){
            return _resultCode;
        }

        public void setResultCode(String value){
            this._resultCode = value;
        }


        private String _resultMsg;

    
        @PropMeta(propId=8)
    
        public String getResultMsg(){
            return _resultMsg;
        }

        public void setResultMsg(String value){
            this._resultMsg = value;
        }


        private java.sql.Timestamp _logTime;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getLogTime(){
            return _logTime;
        }

        public void setLogTime(java.sql.Timestamp value){
            this._logTime = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=11)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=12)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=13)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=14)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=15)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=16)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _ediDoc;

        public Map<String,Object> getEdiDoc(){
            return _ediDoc;
        }

        public void setEdiDoc(Map<String,Object> value){
            this._ediDoc = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
