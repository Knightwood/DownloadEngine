package com.kiylx.download_module.fileskit;

import com.kiylx.download_module.file_platform.FakeFile;
import com.kiylx.download_module.file_platform.PathFile;
import com.kiylx.download_module.file_platform.system.SysCall;
import io.reactivex.annotations.NonNull;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileKitImpl implements FileKit<Path> {
    private final SysCall sysCall;

    public FileKitImpl() {
        //this.sysCall = getContext().getSysCallKit();
        this.sysCall = null;
    }

    @Override
    public FakeFile<Path> create(String path, boolean isCreateFile) {
        Path result = Paths.get(path);
        try {
            if (!isCreateFile) {//创建文件夹
                Files.createDirectories(result);
            } else {
                Files.createFile(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new PathFile(result, FakeFile.linux);
    }

    @Override
    public FakeFile<Path> find(String path) {
        return new PathFile(Paths.get(path), FakeFile.linux);
    }

    @Override
    public boolean isExist(String path, int fileKind) {
        Path path1 = Paths.get(path);
        return Files.exists(path1);
    }

    @Override
    public int is(String path) {
        Path path1 = Paths.get(path);
        if (Files.isRegularFile(path1))
            return FileKind.file;
        if (Files.isDirectory(path1))
            return FileKind.dir;
        return FileKind.NULL;
    }

    /**
     * 利用SimpleFileVisitor来递归删除文件和目录
     */
    public void rmdir(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rmdir(String path) {
        Path path1 = Paths.get(path);
        rmdir(path1);
    }

    @Override
    public boolean reName(FakeFile<Path> file, String newName) {
        Path path = file.get();
        if (path != null)
            try {
                Files.move(path, path.resolveSibling(newName));
            } catch (IOException e) {
                e.printStackTrace();
            }

        return false;
    }

    @Override
    public boolean checkSpace(String path, long size) {
        return true;
    }

    @Override
    public String getName(String filePath) {
        Path path = Paths.get(filePath).getFileName();
        return path.toString();
    }

    @Override
    public String getName(FakeFile<Path> file) {
        Path path = file.get();
        if (path == null)
            return "";
        return path.getFileName().toString();
    }

    @Override
    public FakeFile<Path> get(String filePath) {
        return new PathFile(Paths.get(filePath), FakeFile.linux);
    }


    @Override
    public void move(String path, String newPath, int fileKind) {
        Path source = Paths.get(path);
        Path newdir = Paths.get(newPath);
        try {
            Files.move(source, newdir.resolve(source.getFileName()), REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean copy(String path, String newPath, int fileKind) {
        Path source = Paths.get(path);
        Path newdir = Paths.get(newPath);
        try {
            Files.copy(source, newdir.resolve(source.getFileName()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String getExtension(FakeFile<Path> file) {
        String fileName = getName(file);
        String ext = fileName.substring(fileName.lastIndexOf("."));
        return ext;
    }

    /*
     * See http://man7.org/linux/man-pages/man3/posix_fallocate.3.html
     */
    @Override
    public void allocate(@NonNull FileDescriptor fd, long length) throws IOException {
        if (sysCall != null)
            sysCall.fallocate(fd, length);
    }

    @Override
    public void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (final IOException e) {
            /* Ignore */
        }
    }
}
