//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bMftCertificateInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _orgId;

    
        @PropMeta(propId=2)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _partnerId;

    
        @PropMeta(propId=3)
    
        public String getPartnerId(){
            return _partnerId;
        }

        public void setPartnerId(String value){
            this._partnerId = value;
        }


        private String _certName;

    
        @PropMeta(propId=4)
    
        public String getCertName(){
            return _certName;
        }

        public void setCertName(String value){
            this._certName = value;
        }


        private String _certType;

    
        @PropMeta(propId=5)
    
        public String getCertType(){
            return _certType;
        }

        public void setCertType(String value){
            this._certType = value;
        }


        private String _algorithm;

    
        @PropMeta(propId=6)
    
        public String getAlgorithm(){
            return _algorithm;
        }

        public void setAlgorithm(String value){
            this._algorithm = value;
        }


        private Integer _keySize;

    
        @PropMeta(propId=7)
    
        public Integer getKeySize(){
            return _keySize;
        }

        public void setKeySize(Integer value){
            this._keySize = value;
        }


        private String _issuerName;

    
        @PropMeta(propId=8)
    
        public String getIssuerName(){
            return _issuerName;
        }

        public void setIssuerName(String value){
            this._issuerName = value;
        }


        private String _subjectName;

    
        @PropMeta(propId=9)
    
        public String getSubjectName(){
            return _subjectName;
        }

        public void setSubjectName(String value){
            this._subjectName = value;
        }


        private String _serialNo;

    
        @PropMeta(propId=10)
    
        public String getSerialNo(){
            return _serialNo;
        }

        public void setSerialNo(String value){
            this._serialNo = value;
        }


        private String _fingerprintSha256;

    
        @PropMeta(propId=11)
    
        public String getFingerprintSha256(){
            return _fingerprintSha256;
        }

        public void setFingerprintSha256(String value){
            this._fingerprintSha256 = value;
        }


        private java.time.LocalDate _issuedAt;

    
        @PropMeta(propId=12)
    
        public java.time.LocalDate getIssuedAt(){
            return _issuedAt;
        }

        public void setIssuedAt(java.time.LocalDate value){
            this._issuedAt = value;
        }


        private java.time.LocalDate _expiresAt;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDate getExpiresAt(){
            return _expiresAt;
        }

        public void setExpiresAt(java.time.LocalDate value){
            this._expiresAt = value;
        }


        private Boolean _isActive;

    
        @PropMeta(propId=14)
    
        public Boolean getIsActive(){
            return _isActive;
        }

        public void setIsActive(Boolean value){
            this._isActive = value;
        }


        private String _remark;

    
        @PropMeta(propId=15)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
