package shalev.apps.internetchecker;

import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Utils {
    private static final Lock _lockFileOperation = new ReentrantLock();

    public static String getCurrentTime() {
        /*
        https://stackoverflow.com/questions/5369682/get-current-time-and-date-on-android
        */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss:SSS");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }

    public static void writeStrToFile(File file, String str) throws IOException {
        _lockFileOperation.lock();
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(str.getBytes());
            stream.close();
        } finally {_lockFileOperation.unlock();}
    }

    public static String readFileAsStr(File file) throws IOException {
        _lockFileOperation.lock();
        try {
        int length = (int) file.length();

            byte[] bytes = new byte[length];
            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            String contents = new String(bytes);
            return contents;
        } finally {_lockFileOperation.unlock();}
    }

    public static void appendStrToLog(String str) {
        try {
            File logFile = new File(Conf.DATA_DIR_PATH, Conf.LOG_FILE_NAME);
            if (logFile.exists() && (logFile.length() > 1024 * 1024)) //TODO:size out to Conf class
                logFile.delete(); // no more than 1 MB log file!!!

            String alreadyThere = "";
            try { alreadyThere = Utils.readFileAsStr(logFile); } catch (Exception ex) {}

            String lineToAdd = Utils.getCurrentTime() + "\t\t\t" + str;
            String wholeLogContents = lineToAdd + "\r\n" + alreadyThere;
            Utils.writeStrToFile(logFile, wholeLogContents);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
