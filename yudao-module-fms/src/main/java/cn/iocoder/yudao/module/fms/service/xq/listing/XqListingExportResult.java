package cn.iocoder.yudao.module.fms.service.xq.listing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class XqListingExportResult {
    private byte[] bytes;
    private String filename;
    private String contentType;
    private int productCount;
}
