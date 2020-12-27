package shalev.apps.internetchecker;

public class Conf {
    public static final long IM_ALARM_SERVICE_INTERVAL_MS = 15 * 60 * 1000;
    public static final long MAIN_ACTIVITY_SAMPLING_INTERVAL_MS = 7 * 1000;

    public static final String REGEX_SITE_SEPERATOR = "\\$AS\\$";

    public static final String LOGCAT_TAG_NAME = "InternetCheckerMessage";
    public static final String DATA_DIR_PATH = "/data/data/shalev.apps.internetchecker/";
    public static final String LOG_FILE_NAME = "hLog";
    public static final String UDID_FILE_NAME = "UdID";
}