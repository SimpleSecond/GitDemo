package com.example.dongya.gitdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button scanBtn;
    private TextView wifiResultTextView;
    private WifiManager wifiManager;
    private WifiInfo currentWifiInfo;
    private List<ScanResult> wifiList;
    private String[] str;
    private int wifiIndex;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        scanBtn = (Button) this.findViewById(R.id.scan_button);
        wifiResultTextView = (TextView) this.findViewById(R.id.wifi_result_textview);
        scanBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_button:
                lookUpScan();
                break;
        }

    }

    public void openWifi() {
        wifiManager.setWifiEnabled(true);
    }

    //    扫描线程
    class ScanWifiThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true)
            {
                currentWifiInfo = wifiManager.getConnectionInfo();
                startScan();
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }

    /**
     * 扫描wifi
     */
    public void startScan()
    {
        wifiManager.startScan();
        // 获取扫描结果
        wifiList = wifiManager.getScanResults();
        str = new String[wifiList.size()];
        String tempStr = null;
        for (int i = 0; i < wifiList.size(); i++)
        {
            tempStr = wifiList.get(i).SSID;
            if (null != currentWifiInfo
                    && tempStr.equals(currentWifiInfo.getSSID()))
            {
                tempStr = tempStr + "(已连接)";
            }
            str[i] = tempStr;
        }
    }

    /**
     * 弹出框 查看扫描结果
     */
    public void lookUpScan()
    {
        new ScanWifiThread().start();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("wifi");
        builder.setItems(str, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                wifiIndex = which;
                handler.get().sendEmptyMessage(3);
            }
        });
        builder.show();
    }

    /**
     * 获取网络ip地址
     *
     * @author passing
     *
     */
    class RefreshSsidThread extends Thread
    {

        @Override
        public void run()
        {
            boolean flag = true;
            while (flag)
            {
                currentWifiInfo = wifiManager.getConnectionInfo();
                if (null != currentWifiInfo.getSSID()
                        && 0 != currentWifiInfo.getIpAddress())
                {
                    flag = false;
                }
            }
            handler.get().sendEmptyMessage(4);
            super.run();
        }
    }

    /**
     * 连接网络
     *
     * @param index
     * @param password
     */
    public void connetionConfiguration(int index, String password)
    {
        progressDialog = ProgressDialog.show(MainActivity.this, "正在连接...",
                "请稍候...");
        new ConnectWifiThread().execute(index + "", password);
    }

    /**
     * 连接wifi
     *
     * @author passing
     *
     */
    class ConnectWifiThread extends AsyncTask<String, Integer, String>
    {

        @Override
        protected String doInBackground(String... params)
        {
            int index = Integer.parseInt(params[0]);
            if (index > wifiList.size())
            {
                return null;
            }
            // 连接配置好指定ID的网络
            WifiConfiguration config = WifiUtil.createWifiInfo(
                    wifiList.get(index).SSID, params[1], 3, wifiManager);

            int networkId = wifiManager.addNetwork(config);
            if (null != config)
            {
                wifiManager.enableNetwork(networkId, true);
                return wifiList.get(index).SSID;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (null != progressDialog)
            {
                progressDialog.dismiss();
            }
            if (null != result)
            {
                handler.get().sendEmptyMessage(0);
            }
            else
            {
                handler.get().sendEmptyMessage(1);
            }
            super.onPostExecute(result);
        }

    }

    final ThreadLocal<Handler> handler;

    {
        handler = new ThreadLocal<Handler>() {
            @Override
            protected Handler initialValue() {
                return new Handler() {

                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 0:
                                wifiResultTextView.setText("正在获取ip地址...");
                                new RefreshSsidThread().start();
                                break;
                            case 1:
                                Toast.makeText(MainActivity.this, "连接失败！", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case 3:
                                View layout = LayoutInflater.from(MainActivity.this).inflate(
                                        R.layout.custom_dialog_layout, null);
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("请输入密码").setView(layout);
                                final EditText passowrdText = (EditText) layout
                                        .findViewById(R.id.password_edittext);
                                builder.setPositiveButton("连接",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                connetionConfiguration(wifiIndex, passowrdText
                                                        .getText().toString());
                                            }
                                        }).show();
                                break;
                            case 4:
                                Toast.makeText(MainActivity.this, "连接成功！", Toast.LENGTH_SHORT)
                                        .show();
                                wifiResultTextView.setText("当前网络："
                                        + currentWifiInfo.getSSID() + " ip:"
                                        + WifiUtil.intToIp(currentWifiInfo.getIpAddress()));
                                break;
                        }
                        super.handleMessage(msg);
                    }

                };
            }
        };
    }

}
