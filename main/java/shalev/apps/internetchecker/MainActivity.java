package shalev.apps.internetchecker;

import android.provider.Settings.Secure;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import android.util.Base64;

public class MainActivity extends AppCompatActivity {
    private Button _bt_check_again;
    private TextView _timeTaken_tv;
    private TextView _androidID_tv;
    private TextView _macAddress_tv;
    private TextView _internalIP_tv;
    private TextView _externalIPs_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBroadcastReceiver();

        doStaticChecks();
        _externalIPs_tv = (TextView) findViewById(R.id.external_ips);
        _externalIPs_tv.setMovementMethod(new ScrollingMovementMethod());

        _bt_check_again = (Button) findViewById(R.id.external_ips_check_again_bt);
        _bt_check_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { doDynamicChecks(); } });

        startSamplingLogFile();
        doDynamicChecks();
    }

    private void registerBroadcastReceiver() {
        IMBroadcastReceiver.registerService(this.getApplicationContext());
    }

    private void doStaticChecks() {
        _androidID_tv = (TextView) findViewById(R.id.android_id);
        _macAddress_tv = (TextView) findViewById(R.id.mac_address);

        _androidID_tv.setText(getString(R.string.androidID_text) + "\t\t\t\t\t" + getAndroidID());
        _macAddress_tv.setText(getString(R.string.macAddress_text) + "\t\t\t" + getMAC());
    }

    private void startSamplingLogFile() {
        (new Thread() { public void run() {
            while(true) {
                String logContents = "";
                try {
                    File logFile = new File(Conf.DATA_DIR_PATH, Conf.LOG_FILE_NAME);
                    if (logFile.exists()) logContents = Utils.readFileAsStr(logFile);
                } catch (Exception ex) {
                    Log.w(Conf.LOGCAT_TAG_NAME, "MA:sSLF");
                    ex.printStackTrace();
                } finally {
                    updateUIExternalIPs(logContents);
                    try{ Thread.sleep(Conf.MAIN_ACTIVITY_SAMPLING_INTERVAL_MS); } catch (Exception ex) {}
                }
            }}}).start();
    }

    private void updateUIExternalIPs(String logContents) {
        _externalIPs_tv = (TextView) findViewById(R.id.external_ips);

        final String finalStr =  "\r\n" + logContents + "\r\n";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _externalIPs_tv.setText(finalStr);
                _bt_check_again.setEnabled(true);
            }
        });
    }

    private void doDynamicChecks() {
        _timeTaken_tv = (TextView) findViewById(R.id.time_taken);
        _internalIP_tv = (TextView) findViewById(R.id.internal_ip);

        _timeTaken_tv.setText(getString(R.string.timeTaken_text) + "\t\t\t\t" + Utils.getCurrentTime());
        _internalIP_tv.setText(getString(R.string.internalIP_text) + "\t\t\t\t\t\t" + getInternalIP());

        InternetManager manager = InternetManager.get();
        manager.doWork();
    }

	//TODO:Out to Utils class
    private String getAndroidID() { return Secure.getString(this.getApplicationContext().getContentResolver(), Secure.ANDROID_ID); }

	//TODO:Out to Utils class
    private String getMAC() {
        //return "MyUniqueMACAddressssss";

        /*
        https://stackoverflow.com/questions/10831578/how-to-find-mac-address-of-an-android-device-programmatically
        */
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0") && !nif.getName().equalsIgnoreCase("eth0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X", (b & 0xFF)) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                if(res1.toString() == "02:00:00:00:00:00") continue;
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }

	//TODO:Out to Utils class
    private String getInternalIP() {
        /*
        https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
         */
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
