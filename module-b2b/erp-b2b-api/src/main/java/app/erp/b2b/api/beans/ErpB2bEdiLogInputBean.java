//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bEdiLogInputBean extends CrudInputBase {

    
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


    }
