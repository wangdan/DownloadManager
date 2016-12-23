package org.aiwen.downloader;

import static org.aiwen.downloader.utils.Utils.realtime;

/**
 * Created by 王dan on 2016/12/21.
 */
public class ThreadTrace {

    final Request request;

    private long connectS;

    private long connectE;

    private long readS;

    private long readE;

    private long speedCountS;

    private long speedCountE;

    private long[] speedCount;

    private volatile long receiveSize;

    public ThreadTrace(Request request) {
        this.request = request;
    }

    void beginConnect() {
        connectS = realtime();
    }

    void endConnect() {
        connectE = realtime();
    }

    void benginRead() {
        readS = realtime();
    }

    void endRead() {
        readE = realtime();
    }

    void beginSpeedCount() {
        speedCountS = realtime();

        speedCount = new long[]{ receiveSize, 0 };
    }

    void endSpeedCount() {
        if (speedCount != null) {
            speedCountE = realtime();

            speedCount[1] = receiveSize;
        }
    }

    void receive(long length) {
        receiveSize += length;
    }

    public float getAverageSpeed() {
        float speed = 0.0f;

        long time;
        if (readE == 0) {
            time = realtime() - readS;
        }
        else {
            time = readE - readS;
        }

        if (time > 0) {
            speed = receiveSize * 1.0f / 1024 / (time * 1.0f / 1000);
        }

        return speed;
    }

    public float getSpeed() {
        float speed = 0.0f;

        long time = speedCountE - speedCountS;

        if (time > 0) {
            speed = (speedCount[1] - speedCount[0]) * 1.0f / 1024 / (time * 1.0f / 1000);
        }

        return speed;
    }

    public long getTime() {
        return readE - readS;
    }

    public long getRealTime() {
        return readE - connectS;
    }

}
