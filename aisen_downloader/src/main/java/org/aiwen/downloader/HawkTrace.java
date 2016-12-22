package org.aiwen.downloader;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wangdan on 16/12/22.
 */
public class HawkTrace {

    volatile float speed;// 当前所有下载数据的即时速度
    volatile float averageSpeed;// 当前所有下载数据的平均速度
    AtomicInteger concurrentThread = new AtomicInteger();// 正在下载的任务数
    AtomicInteger peddingThread = new AtomicInteger();// 正在等待下载的任务数

    /**
     * 正在下载的即时速度
     *
     * @return
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * 正在下载的平均速度
     *
     * @return
     */
    public float getAverageSpeed() {
        return averageSpeed;
    }

    /**
     * 下载中线程数
     *
     * @return
     */
    public int getConcurrentThread() {
        return concurrentThread.get();
    }

    /**
     * 等待下载线程数
     *
     * @return
     */
    public int getPeddingThread() {
        return peddingThread.get();
    }

}
