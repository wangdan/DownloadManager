package org.aiwen.downloader;

import org.aiwen.downloader.utils.Utils;

import java.io.File;

/**
 * Created by 王dan on 2016/12/21.
 */

class FileManager {

    /**
     * 创建临时下载文件
     *
     * @param request
     * @return
     */
    static File createTempFile(Request request) {
        File tempFile = new File(request.fileUri.getPath() + ".dt");
        long rangeBytes = request.downloadInfo.rangeBytes;

        if (tempFile.exists()) {
            DLogger.v(Utils.getDownloaderTAG(request), "临时文件已存在");

            // 临时文件长度和缓存数据不一致，删除缓存文件重新下载
            if (tempFile.length() > 0 && tempFile.length() != rangeBytes) {
                DLogger.w(Utils.getDownloaderTAG(request), "临时文件长度和缓存数据长度不一致，file(%d), range(%d)", tempFile.length(), rangeBytes);

                if (tempFile.delete()) {
                    DLogger.w(Utils.getDownloaderTAG(request), "删除临时文件");
                }
                else {
                    DLogger.w(Utils.getDownloaderTAG(request), "删除临时文件失败");
                }

                request.downloadInfo.rangeBytes = 0;
            }
            else {
                DLogger.d(Utils.getDownloaderTAG(request), "文件断点下载，file(%d), range(%d)", tempFile.length(), rangeBytes);
            }
        }
        else {
            // 创建父文件夹
            File dir = tempFile.getParentFile();
            if (dir.exists()) {
                if (!dir.isDirectory()) {
                    dir.delete();
                    dir.mkdirs();

                    DLogger.d(Utils.getDownloaderTAG(request), "新建下载目标文件夹(%s)", dir.getAbsolutePath());
                }
            }
            else {
                dir.mkdirs();

                DLogger.d(Utils.getDownloaderTAG(request), "新建下载目标文件夹(%s)", dir.getAbsolutePath());
            }
        }

        return tempFile;
    }

}
