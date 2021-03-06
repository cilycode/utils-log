package com.cily.utils.log;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.cily.utils.base.StrUtils;
import com.cily.utils.base.log.Logs;
import com.cily.utils.logFile.file.WriteFileRunnable;
import com.cily.utils.logFile.queue.LogQueue;
import com.cily.utils.logFile.task.WriteFileThreadPool;
import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;

import java.io.File;
import java.util.List;

/**
 * @author cily
 * @version 1.0.0   2017-01-23  日志写数据库工具类
 */
public class DbUtils {
    private static LiteOrm liteOrm;
    private static boolean saveLog = false;
    private static boolean saveLogToFile = true;    //

    public static void init(Context cx) {
        init(cx, false);
    }

    public static void init(Context cx, boolean saveLog) {
        init(cx, saveLog, false);
    }

    public static void init(Context cx, boolean saveLog, boolean saveExternal) {
        DbUtils.saveLog = saveLog;

        if (cx == null) {
            return;
        }

//         日志保存到db还是file
        if (saveExternal) {
            if (StrUtils.isEmpty(cx.getPackageName())) {
                return;
            }

            PackageManager pm = cx.getPackageManager();
            boolean readPermission = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, cx.getPackageName()));

            boolean writePermission = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, cx.getPackageName()));


            if (!readPermission || !writePermission) {
                return;
            }

            if (saveLogToFile) {
                String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                        + cx.getPackageName() + File.separator + "logs";
                File f = new File(dir);
                if (!f.exists()) {
                    f.mkdirs();
                }
                WriteFileThreadPool.getInstance().initWriteRunnable(dir, null);
            }

            if (liteOrm == null) {
                liteOrm = LiteOrm.newSingleInstance(cx, Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                                + cx.getPackageName() + File.separator + "db_log.db");
            }
        } else {
            if (liteOrm == null) {
                liteOrm = LiteOrm.newSingleInstance(cx, "db_log.db");
            }
            if (saveLogToFile){
                String dir = cx.getFilesDir().getAbsolutePath() + File.separator + "logs";
                File f = new File(dir);
                if (!f.exists()) {
                    f.mkdirs();
                }
                WriteFileThreadPool.getInstance().initWriteRunnable(dir, null);
            }
        }

        /*if (saveLogToFile) {
            if (saveExternal) {
                if (StrUtils.isEmpty(cx.getPackageName())) {
                    return;
                }

                PackageManager pm = cx.getPackageManager();
                boolean readPermission = (PackageManager.PERMISSION_GRANTED ==
                        pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, cx.getPackageName()));

                boolean writePermission = (PackageManager.PERMISSION_GRANTED ==
                        pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, cx.getPackageName()));


                if (!readPermission || !writePermission) {
                    return;
                }
                if (liteOrm == null) {
                    liteOrm = LiteOrm.newSingleInstance(cx,
                            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                                    + cx.getPackageName() + File.separator + "db_log.db");
                }

                String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                        + cx.getPackageName() + File.separator + "logs";
                File f = new File(dir);
                if (!f.exists()) {
                    f.mkdirs();
                }

                WriteFileThreadPool.getInstance().initWriteRunnable(dir, null);
            } else {
                if (liteOrm == null) {
                    liteOrm = LiteOrm.newSingleInstance(cx, "db_log.db");
                }

                String dir = cx.getFilesDir().getAbsolutePath() + File.separator + "logs";
                File f = new File(dir);
                if (!f.exists()) {
                    f.mkdirs();
                }

                WriteFileThreadPool.getInstance().initWriteRunnable(dir, null);
            }
        } else {
            if (saveExternal) {
                if (StrUtils.isEmpty(cx.getPackageName())) {
                    return;
                }

                PackageManager pm = cx.getPackageManager();
                boolean readPermission = (PackageManager.PERMISSION_GRANTED ==
                        pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, cx.getPackageName()));

                boolean writePermission = (PackageManager.PERMISSION_GRANTED ==
                        pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, cx.getPackageName()));

                if (!readPermission || !writePermission) {
                    return;
                }

                if (liteOrm == null) {
                    liteOrm = LiteOrm.newSingleInstance(cx,
                            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                                    + cx.getPackageName() + File.separator + "db_log.db");
                }
            } else {
                if (liteOrm == null) {
                    liteOrm = LiteOrm.newSingleInstance(cx, "db_log.db");
                }
            }
        }*/
    }

    public static LiteOrm getLiteOrm() {
        return liteOrm;
    }

    public static void setSaveLog(boolean saveLog) {
        DbUtils.saveLog = saveLog;
    }

    public static boolean isSaveLog() {
        return saveLog;
    }

    private static int num = 0;

    public static boolean insert(LogBean b) {
        if (!saveLog) {
            return false;
        }
        try {
            if (saveLogToFile) {
                if (num == 0) {
                    LogQueue.getInatnce().put(b.toString());
                }
                num++;
                LogQueue.getInatnce().put(b.out());
                if (num >= 100) {
                    num = 0;
                }
                return true;
            } else {
                return b != null && liteOrm != null && liteOrm.insert(b) > -1;
            }
        } catch (Throwable e) {
            if (Logs.isConsoleLog()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static boolean update(LogBean b) {
        if (!saveLog) {
            return false;
        }

        try {
            return b != null && liteOrm != null && liteOrm.update(b) > -1;
        } catch (Throwable e) {
            if (Logs.isConsoleLog()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static boolean del(LogBean b) {
        if (!saveLog) {
            return false;
        }

        try {
            return b != null && liteOrm != null && liteOrm.delete(b) > -1;
        } catch (Throwable e) {
            if (Logs.isConsoleLog()) {
                e.printStackTrace();
            }
            return false;
        }

    }

    public static List<LogBean> searchAll() {
        try {
            return liteOrm == null ? null : liteOrm.query(LogBean.class);
        } catch (Throwable e) {
            if (Logs.isConsoleLog()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static List<LogBean> search(int limit) {
        if (limit < 1) {
            return searchAll();
        }
        try {
            return liteOrm == null ? null : liteOrm.query(new QueryBuilder<LogBean>(LogBean.class).limit(0, limit));
        } catch (Throwable e) {
            if (Logs.isConsoleLog()) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
