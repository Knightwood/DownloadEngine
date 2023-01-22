package com.kiylx.download_module.model;


import java.util.UUID;

/**
 * 描述header
 */
public class HeaderStore {

    
    public UUID infoId;
    public String name;
    public String value;

    public HeaderStore( UUID infoId, String name, String value) {
        this.infoId = infoId;
        this.name = name;
        this.value = value;
    }
}
