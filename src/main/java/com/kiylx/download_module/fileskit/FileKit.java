package com.kiylx.download_module.fileskit;

import com.kiylx.download_module.file_platform.FakeFile;
import io.reactivex.annotations.NonNull;

import java.io.*;


public interface FileKit<T> {

    /**
     * @param path         路径
     * @param isCreateFile true：创建文件；false：创建文件夹
     * @return 创建成功返回file, 失败返回null
     */
    //必须实现
    FakeFile<T> create(String path, boolean isCreateFile);
    //必须实现
    FakeFile<T> find(String path);//返回系统上的文件

    /**
     * @param path     路径
     * @param fileKind 0：文件；1：文件夹
     * @return 文件或文件夹存在，返回true
     */
    //必须实现
    boolean isExist(String path, int fileKind);

    /**
     * @param path 路径
     * @return 返回0表示是个文件，1表示是个文件夹
     * 如果路径表示不存在，返回2
     */
    int thisPathIsWhat(String path);

    /**
     * @param path 文件或文件夹的路径
     *             删除此路径的文件或文件夹下所有内容
     */
    //必须实现
    void rmdir(String path);
    /**
     * 重命名文件或文件夹
     *
     * @param file    FakeFile
     * @param newName 新名称
     * @return 命名成功返回true
     */
    boolean reName(FakeFile<T> file, String newName);

    /**
     * @param path 路径
     * @param size 期望存储能至少有这么大的空间
     * @return 如果能存下如此大小的文件，返回true
     */
    //必须实现
    boolean checkSpace(String path, long size);

    String getName(String filePath);
    //若实现本类中的getExtension方法，则必须实现此方法
    String getName(FakeFile<T> file);

    /**
     * @param filePath 路径
     * @return 返回此路径表示的文件，文件不存在返回null
     */
    FakeFile getThisPathFile(String filePath);

    /**
     * 将文件移动到新目录，保留相同的文件名，并替换目录中该名称的任何现有文件：
     *
     * @param path
     * @param newPath
     * @param fileKind
     */
    void move(String path, String newPath, int fileKind);

    boolean copy(String path, String newPath, int fileKind);

    String getExtension(FakeFile<T> file);

    void allocate(@NonNull FileDescriptor fd, long length) throws IOException;

    void closeQuietly(Closeable closeable);

    static class FileKind {
        public static final int file = 0;
        public static final int dir = 1;
        public static final int NULL = 2;
    }
}
