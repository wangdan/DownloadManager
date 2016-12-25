package org.aiwen.downloader;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wangdan on 16/12/22.
 */
public class HawkTrace {

    AtomicInteger concurrentThread = new AtomicInteger();// 正在下载的任务数
    AtomicInteger peddingThread = new AtomicInteger();// 正在等待下载的任务数

    /**
     * 正在下载的即时速度
     *
     * @return
     */
    public float getSpeed() {
        float speed = 0.0f;

        Hawk hawk = Hawk.getInstance();
        if (hawk != null) {
            synchronized (hawk.mRequestMap) {
                Set<String> keySet = hawk.mRequestMap.keySet();
                for (String key : keySet) {
                    Request request = hawk.mRequestMap.get(key);

                    speed += request.trace.getSpeed();
                }
            }
        }

        return speed;
    }

    /**
     * 正在下载的平均速度
     *
     * @return
     */
    public float getAverageSpeed() {
        float averageSpeed = 0.0f;

        Hawk hawk = Hawk.getInstance();
        if (hawk != null) {
            synchronized (hawk.mRequestMap) {
                Set<String> keySet = hawk.mRequestMap.keySet();
                for (String key : keySet) {
                    Request request = hawk.mRequestMap.get(key);

                    averageSpeed += request.trace.getAverageSpeed();
                }
            }
        }

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
