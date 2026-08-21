//__XGEN_FORCE_OVERRIDE__
    package app.erp.b2b.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpB2bMftConfigInputBean extends CrudInputBase {

    
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


        private String _protocol;

    
        @PropMeta(propId=4)
    
        public String getProtocol(){
            return _protocol;
        }

        public void setProtocol(String value){
            this._protocol = value;
        }


        private String _transportEndpoint;

    
        @PropMeta(propId=5)
    
        public String getTransportEndpoint(){
            return _transportEndpoint;
        }

        public void setTransportEndpoint(String value){
            this._transportEndpoint = value;
        }


        private String _localAs2Id;

    
        @PropMeta(propId=6)
    
        public String getLocalAs2Id(){
            return _localAs2Id;
        }

        public void setLocalAs2Id(String value){
            this._localAs2Id = value;
        }


        private String _remoteAs2Id;

    
        @PropMeta(propId=7)
    
        public String getRemoteAs2Id(){
            return _remoteAs2Id;
        }

        public void setRemoteAs2Id(String value){
            this._remoteAs2Id = value;
        }


        private String _sftpUsername;

    
        @PropMeta(propId=8)
    
        public String getSftpUsername(){
            return _sftpUsername;
        }

        public void setSftpUsername(String value){
            this._sftpUsername = value;
        }


        private Integer _sftpPort;

    
        @PropMeta(propId=9)
    
        public Integer getSftpPort(){
            return _sftpPort;
        }

        public void setSftpPort(Integer value){
            this._sftpPort = value;
        }


        private Integer _ftpsPort;

    
        @PropMeta(propId=10)
    
        public Integer getFtpsPort(){
            return _ftpsPort;
        }

        public void setFtpsPort(Integer value){
            this._ftpsPort = value;
        }


        private Boolean _ftpsImplicitTls;

    
        @PropMeta(propId=11)
    
        public Boolean getFtpsImplicitTls(){
            return _ftpsImplicitTls;
        }

        public void setFtpsImplicitTls(Boolean value){
            this._ftpsImplicitTls = value;
        }


        private Boolean _compression;

    
        @PropMeta(propId=12)
    
        public Boolean getCompression(){
            return _compression;
        }

        public void setCompression(Boolean value){
            this._compression = value;
        }


        private Boolean _encryption;

    
        @PropMeta(propId=13)
    
        public Boolean getEncryption(){
            return _encryption;
        }

        public void setEncryption(Boolean value){
            this._encryption = value;
        }


        private String _encryptionAlgo;

    
        @PropMeta(propId=14)
    
        public String getEncryptionAlgo(){
            return _encryptionAlgo;
        }

        public void setEncryptionAlgo(String value){
            this._encryptionAlgo = value;
        }


        private Boolean _signature;

    
        @PropMeta(propId=15)
    
        public Boolean getSignature(){
            return _signature;
        }

        public void setSignature(Boolean value){
            this._signature = value;
        }


        private String _signatureAlgo;

    
        @PropMeta(propId=16)
    
        public String getSignatureAlgo(){
            return _signatureAlgo;
        }

        public void setSignatureAlgo(String value){
            this._signatureAlgo = value;
        }


        private String _certId;

    
        @PropMeta(propId=17)
    
        public String getCertId(){
            return _certId;
        }

        public void setCertId(String value){
            this._certId = value;
        }


        private Boolean _active;

    
        @PropMeta(propId=18)
    
        public Boolean getActive(){
            return _active;
        }

        public void setActive(Boolean value){
            this._active = value;
        }


        private Integer _maxRetries;

    
        @PropMeta(propId=19)
    
        public Integer getMaxRetries(){
            return _maxRetries;
        }

        public void setMaxRetries(Integer value){
            this._maxRetries = value;
        }


        private Integer _retryIntervalMin;

    
        @PropMeta(propId=20)
    
        public Integer getRetryIntervalMin(){
            return _retryIntervalMin;
        }

        public void setRetryIntervalMin(Integer value){
            this._retryIntervalMin = value;
        }


        private Boolean _deadLetterEnabled;

    
        @PropMeta(propId=21)
    
        public Boolean getDeadLetterEnabled(){
            return _deadLetterEnabled;
        }

        public void setDeadLetterEnabled(Boolean value){
            this._deadLetterEnabled = value;
        }


        private String _monitorDirectory;

    
        @PropMeta(propId=22)
    
        public String getMonitorDirectory(){
            return _monitorDirectory;
        }

        public void setMonitorDirectory(String value){
            this._monitorDirectory = value;
        }


        private Integer _monitorIntervalSec;

    
        @PropMeta(propId=23)
    
        public Integer getMonitorIntervalSec(){
            return _monitorIntervalSec;
        }

        public void setMonitorIntervalSec(Integer value){
            this._monitorIntervalSec = value;
        }


        private String _remark;

    
        @PropMeta(propId=24)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
