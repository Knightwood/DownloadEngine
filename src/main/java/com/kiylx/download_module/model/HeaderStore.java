package com.kiylx.download_module.model;

import io.reactivex.annotations.NonNull;

import java.util.UUID;

/**
 * 描述header
 */
public class HeaderStore {

    @NonNull
    public UUID infoId;
    public String name;
    public String value;

    public HeaderStore(@NonNull UUID infoId, String name, String value) {
        this.infoId = infoId;
        this.name = name;
        this.value = value;
    }
}
