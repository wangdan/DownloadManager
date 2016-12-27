package org.aiwen.downloader;

import android.util.Log;

import org.aiwen.downloader.utils.Constants;
import org.aiwen.downloader.utils.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

/**
 * Created by 王dan on 2016/12/17.
 */

public class DLogger {

    final static String TAG = Constants.TAG + "_DLogger";

    public static boolean DEBUG = true;

    public static void v(Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.v(TAG, log);
        }

    }

    static void v(String tag, Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.v(tag, log);
        }
    }

    static void v(String tag, String msg, Throwable tr) {
        if (DEBUG) {
            String log = msg + '\n' + getStackTraceString(tr);

            Log.v(tag, log);
        }
    }

    public static void v(String tag, String format, Object... args) {
        if (DEBUG) {
            String log = String.format(format, args);

            Log.v(tag, log);
        }
    }

    static void d(Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.d(TAG, log);
        }
    }

    static void d(String tag, Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.d(tag, log);
        }
    }

    static void d(String tag, String msg, Throwable tr) {
        if (DEBUG) {
            String log = msg + '\n' + getStackTraceString(tr);

            Log.d(tag, log);
        }
    }

    static void d(String tag, String format, Object... args) {
        if (DEBUG) {
            String log = String.format(format, args);

            Log.d(tag, log);
        }
    }

    static void i(Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.i(TAG, log);
        }
    }

    static void i(String tag, Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.i(tag, log);
        }
    }

    static void i(String tag, String msg, Throwable tr) {
        if (DEBUG) {
            String log = msg + '\n' + getStackTraceString(tr);

            Log.i(tag, log);
        }
    }

    static void i(String tag, String format, Object... args) {
        if (DEBUG) {
            String log = String.format(format, args);

            Log.i(tag, log);
        }
    }

    static void w(Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.w(TAG, log);
        }
    }

    static void w(String tag, Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.w(tag, log);
        }
    }

    static void w(String tag, String msg, Throwable tr) {
        if (DEBUG) {
            String log = msg + '\n' + getStackTraceString(tr);

            Log.w(tag, log);
        }
    }

    static void w(String tag, String format, Object... args) {
        if (DEBUG) {
            String log = String.format(format, args);

            Log.w(tag, log);
        }
    }

    static void e(Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.e(TAG, log);
        }
    }

    static void e(String tag, Object o) {
        if (DEBUG) {
            String log = toJson(o);

            Log.e(tag, log);
        }
    }

    static void e(String tag, String msg, Throwable tr) {
        if (DEBUG) {
            String log = msg + '\n' + getStackTraceString(tr);

            Log.e(tag, log);
        }
    }

    static void e(String tag, String format, Object... args) {
        if (DEBUG) {
            String log = String.format(format, args);

            Log.e(tag, log);
        }
    }

    // 这个日志会打印，不会因为release版本屏蔽
    static void sysout(String msg) {
        try {
            Log.v(TAG, msg);
        } catch (Throwable e) {
        }
    }

    static void printExc(Class<?> clazz, Throwable e) {
        try {
            if (DEBUG) {
                e.printStackTrace();
            }
            else {
                String clazzName = clazz == null ? "Unknow" : clazz.getSimpleName();

                Log.v(TAG, String.format("class[%s], %s", clazzName, e + ""));
            }
        } catch (Throwable ee) {
            ee.printStackTrace();
        }
    }

    static void printStackTrace(Exception e) {
        e(Constants.TAG, e + "");

        printExc(Utils.class, e);
    }

    static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    static String toJson(Object msg) {
        if (msg instanceof String)
            return msg.toString();
        if (msg instanceof Throwable) {
            return getStackTraceString((Throwable) msg);
        }

//		String json = JSON.toJSONString(msg);
        String json = msg + "";
        if (json.length() > 500)
            json = json.substring(0, 500);

        return json;
    }

}
