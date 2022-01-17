package com.kiylx.download_module.lib_core.model;


import com.kiylx.download_module.lib_core.interfaces.VerifyFactory;


public class VerifyFactoryImpl implements VerifyFactory {
    @Override
    public boolean singleThread() {
        return false;
    }

    @Override
    public boolean canSaveFile() {
        return false;
    }

    @Override
    public VerifyResult verify(DownloadInfo info) {
        return null;
    }

    @Override
    public boolean initFile(DownloadInfo info) {
        /* Create file if doesn't exists or replace it */
        //创建文件
        /* Uri filePath;
       try {
            filePath = fs.createFile(info.dirPath, info.fileName, false);

        } catch (IOException e) {
            ret = new StopRequest(STATUS_FILE_ERROR, e);
            return new ExecDownloadResult(ret, resList);
        }
        if (filePath == null) {
            ret = new StopRequest(STATUS_FILE_ERROR, "Unable to create file");
            return new ExecDownloadResult(ret, resList);
        }*/
        return false;
    }


}
