package org.aiwen.downloader;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wangdan on 16/12/22.
 */
class HawkTrace {

    float speed;// 当前所有下载数据的即时速度
    float averageSpeed;// 当前所有下载数据的平均速度
    AtomicInteger concurrentThread = new AtomicInteger();// 正在下载的任务数
    AtomicInteger peddingThread = new AtomicInteger();// 正在等待下载的任务数

}
