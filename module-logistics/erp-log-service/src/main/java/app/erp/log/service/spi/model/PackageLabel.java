package app.erp.log.service.spi.model;

/**
 * 面单标签 DTO（getPackageLabelsList 返回元素）。
 */
public class PackageLabel {
    private String parcelNo;
    private String labelUrl;
    /** 面单格式：PDF / ZPL。 */
    private String format;

    public String getParcelNo() {
        return parcelNo;
    }

    public void setParcelNo(String parcelNo) {
        this.parcelNo = parcelNo;
    }

    public String getLabelUrl() {
        return labelUrl;
    }

    public void setLabelUrl(String labelUrl) {
        this.labelUrl = labelUrl;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
