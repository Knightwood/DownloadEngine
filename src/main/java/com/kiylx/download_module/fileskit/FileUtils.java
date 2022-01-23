package com.kiylx.download_module.fileskit;

import java.io.File;

public class FileUtils {
    public static final String EXTENSION_SEPARATOR = ".";

    public static String getExtension(String fileName) {
        if (fileName == null)
            return null;

        int extensionPos = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        int lastSeparator = fileName.lastIndexOf(File.separator);
        int index = (lastSeparator > extensionPos ? -1 : extensionPos);

        if (index == -1)
            return "";
        else
            return fileName.substring(index + 1);
    }
}
