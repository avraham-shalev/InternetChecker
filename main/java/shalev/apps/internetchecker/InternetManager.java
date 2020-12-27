package shalev.apps.internetchecker;

import android.net.SSLCertificateSocketFactory;
import android.util.Base64;
import android.util.Log;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class InternetManager {
    private static InternetManager _manager = null;

    private String _udid = "";

    private String _statsServersUrl = "";
    private String _msgsXorKey = "";
    private String _siteNoLongerSupported = "0";

    private boolean _isInstanceRunning = false;
    private final  Lock _lockOnlyOneConcurrentInstance = new ReentrantLock();
    private final  Lock _lockUpdateExternalsGlobalArray = new ReentrantLock();
    private final ArrayList<String> _externalIPs = new ArrayList<String>();

    private InternetManager() { }

    public static InternetManager get() {
        if(_manager == null) {
            _manager = new InternetManager();
            Log.d(Conf.LOGCAT_TAG_NAME, "IM:Initd");
        }
        return _manager;
    }

    public void doWork() {
        (new Thread() { public void run() {doWorkAsync();}}).start();
    }

    private void doWorkAsync() {
        if (_isInstanceRunning) {
            Log.i(Conf.LOGCAT_TAG_NAME, "IM:instAlrRun");
            return;
        }

        Log.d(Conf.LOGCAT_TAG_NAME, "IM:Locking");
        _lockOnlyOneConcurrentInstance.lock();
        Log.d(Conf.LOGCAT_TAG_NAME, "IM:Locked");

        try {
            _isInstanceRunning = true;
            retrieveUDID();
            discoverExternalIPs();
        } catch (Exception ex) {
            handleError(ex, "IM:doWA");
        } finally {
            Log.d(Conf.LOGCAT_TAG_NAME, "IM:Unlocking");
            _isInstanceRunning = false;
            _lockOnlyOneConcurrentInstance.unlock();
            Log.d(Conf.LOGCAT_TAG_NAME, "IM:Unlocked");
        }
    }

    private void handleError(Exception ex, String logcatWarningMsg) {
        try {
            throw ex;
        } catch (UnexpectedDataFromInternetException e) {
            Utils.appendStrToLog("Can not resolve IP Addresses");
        } catch (OldVersionException e) {
            Utils.appendStrToLog("Old version. Please update from Google Play");
        } catch (InterruptedException e) {
        } catch (Exception e) {
            ex.printStackTrace();
        } finally {
            if (logcatWarningMsg != "")
                Log.w(Conf.LOGCAT_TAG_NAME, logcatWarningMsg);
        }
    }

    private void retrieveUDID() {
        if (_udid != "") return;

        File udidFile = new File(Conf.DATA_DIR_PATH, Conf.UDID_FILE_NAME);
        if (!udidFile.exists())
            setUDID(udidFile);
        try {
            _udid = Utils.readFileAsStr(udidFile);
        } catch (Exception ex) {
            handleError(ex, "IM:rUdid");
        }
    }

    private void setUDID(File file) {
        try {
            String udid = generateUDID();
            Utils.writeStrToFile(file, udid);
        } catch (Exception ex) {
            handleError(ex, "IM:sUdid");
        }
    }

    private String generateUDID() throws Exception {
        try {
            String udid = UUID.randomUUID().toString();
            return udid;
        } catch(Exception ex) {Log.w("IM", "GID");}

        //if can not generate then throw Exception
        throw new Exception();
    }

    private void discoverExternalIPs() {
        _externalIPs.clear();

        String[] sitesTuples = {"WhatIsMyIPListOfSites__AndTheirRegexes"};
        retrieveExternalIPsFromSites(sitesTuples);
    }

    private String getHtmlResponse(String site, int timeoutInSeconds) {
        ArrayList<HttpURLConnection> urlConnectionArr = new ArrayList<>();
        InputStream is = null;
        InputStream bis = null;
        String response = "";

        try {
            URL url = new URL(site);
            urlConnectionArr.add((HttpURLConnection) url.openConnection());
            ignoreSslCert(urlConnectionArr);

            response = tryGetResponse(urlConnectionArr, timeoutInSeconds, is, bis);
        } catch (InterruptedException e) {
            handleError(e, "IM:gHR_intrptd");
        } catch (ExecutionException e) {
            handleError(e, "IM:gHR_execErr");
        } catch (TimeoutException e) {
            handleError(e, "IM:gHR_tout");
        } catch (Exception ex) {
            handleError(ex, "IM:gHRexcpt");
        } finally {
            cleanConnectionStreams(urlConnectionArr, is, bis);
        }

        return response;
    }

    private void ignoreSslCert(ArrayList<HttpURLConnection> urlConnectionArr) {
        HttpURLConnection urlConnection = urlConnectionArr.get(0);
        urlConnection.setConnectTimeout(1000);
        urlConnection.setReadTimeout(1000);

        //ignore ssl certificate validation
        if (urlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) urlConnection;
            httpsConn.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));
            httpsConn.setHostnameVerifier(new AllowAllHostnameVerifier());
        }
    }

    private String tryGetResponse(final ArrayList<HttpURLConnection> urlConnectionArr, int timeoutInSeconds,
                                        InputStream is, InputStream bis) throws Exception {
        String resp = "";
        Callable<InputStream> readTask = new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
                HttpURLConnection urlConnection = urlConnectionArr.get(0);
                return urlConnection.getInputStream();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<InputStream> future = executor.submit(readTask);
        is = future.get(timeoutInSeconds, TimeUnit.SECONDS);
        bis = new BufferedInputStream(is);
        resp = readStream(bis);
        return resp;
    }

    private String readStream(InputStream is) throws Exception{
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
        for (String line = r.readLine(); line != null; line =r.readLine()) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    private void cleanConnectionStreams(ArrayList<HttpURLConnection> urlConnectionArr, InputStream is, InputStream bis) {
        try { urlConnectionArr.get(0).disconnect(); } catch(Exception ex) {}
        try { is.close(); } catch(Exception ex) {}
        try { bis.close(); } catch(Exception ex) {}
    }

    private void retrieveExternalIPsFromSites(final String[] tuples) {
        final ArrayList<String> threadsCompleted = new ArrayList<String>();
        final int numOfThreads = tuples.length;

        for (String tuple : tuples) {
            String[] values = tuple.split(Conf.REGEX_SITE_SEPERATOR);
            final String site = values[0];
            final String regex = values[1];

            (new Thread() { public void run() { retrieveExternalIPFromSite(threadsCompleted, numOfThreads, site, regex); }}).start();
        }
    }

    private void retrieveExternalIPFromSite(ArrayList<String> threadsCompleted, int numOfThreads, String site, String regex) {
        boolean isLocked = false;

        try {
            String response = getHtmlResponse(site, 5);
            String externalIP = getExternalIPFromResponse(response, regex);

            _lockUpdateExternalsGlobalArray.lock();
            isLocked = true;
            if (_externalIPs.indexOf(externalIP) < 0)//Can mark this line to check if all sites are working.
                _externalIPs.add(externalIP);
        } catch (Exception ex) {
            handleError(ex, "IM:rEIPFS");
        } finally {
            if(!isLocked)
                _lockUpdateExternalsGlobalArray.lock();

            threadsCompleted.add("another thread finished executing");
            if (threadsCompleted.size() == numOfThreads) {
                doOnFinish();
            }

            _lockUpdateExternalsGlobalArray.unlock();
        }
    }

    private String getExternalIPFromResponse(String response, String regexPattern) {
        /*
        https://stackoverflow.com/questions/34893068/using-android-pattern-and-matcher-class-regex
         */
        String externalIP = "----";

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find())
            externalIP = matcher.group(1);

        return externalIP;
    }

    private void doOnFinish() {
        try {
            if (_externalIPs.indexOf("----") > -1) {
                _siteNoLongerSupported = "1";
                _externalIPs.remove("----");
            }

            sendStats(_externalIPs);
            writeExternalIPs(_externalIPs);
        } catch(Exception ex) {
            handleError(ex, "IM:doOnF");
        }
    }

    private void sendStats(ArrayList<String> externals) {
        String externalIPs = Arrays.toString(externals.toArray());
        String infoToSendStr = _udid + '|' + externalIPs + '|' + _siteNoLongerSupported;
        String infoToSend64Based = new String(Base64.encode(infoToSendStr.getBytes(Charset.forName("ASCII")), Base64.DEFAULT));
		String statsServer = "YOUR_SERVER_TO_SEND_STATS_TO_____SHOULD_BE_IN_CONF_CLASS";
		
        getHtmlResponse(statsServer + infoToSend64Based, 3);
    }

    private void writeExternalIPs(ArrayList<String> externalIPs) {
        try {
            String dataToAdd = Arrays.toString(externalIPs.toArray());
            Utils.appendStrToLog(dataToAdd);
        } catch(Exception ex) {
            handleError(ex, "IM:wExtIPs");
        }
    }
}