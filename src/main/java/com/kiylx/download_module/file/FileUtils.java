package com.kiylx.download_module.file;

import com.kiylx.download_module.model.DownloadInfo;
import com.kiylx.download_module.utils.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtils {
    public static String calcHashSum(DownloadInfo info, boolean sha256Hash) throws IOException {
        File file = new File(info.getPath() + info.getFileName());
        if (!file.exists())
            return null;
        try (FileInputStream is = new FileInputStream(file)) {
            return (sha256Hash ? DigestUtils.makeSha256Hash(is) : DigestUtils.makeMd5Hash(is));
        }

    }
}
