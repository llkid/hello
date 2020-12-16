package shi.example.nfcWriter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import shi.tool.NfcUtils;
import shi.tool.SocketCallBack;

public class MainActivity extends AppCompatActivity {

    private SocketClient socketClient = null;
    private TextView textView;

    private WifiManager wifiManager;
    private static final String TAG = "NewIntent";
    private NfcUtils nfcUtils = null;
    private String macAddress = "";

    private static final String REMEMBER_PWD_PREF = "rememberPwd";
    private static final String IP_ADDRESS_PREF = "ipAddress";
    private static final String PORT_PREF = "port";
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcUtils = new NfcUtils(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        Button getMessage = (Button) findViewById(R.id.getText);
        textView = (TextView) findViewById(R.id.displayText);
        EditText editTextIP = (EditText) findViewById(R.id.editTextIP);
        EditText editTextPort = (EditText) findViewById(R.id.editTextPort);

        preferences = getSharedPreferences(REMEMBER_PWD_PREF, Context.MODE_PRIVATE);
        boolean isRemember = preferences.getBoolean(REMEMBER_PWD_PREF, false);
        if (isRemember) {
            editTextIP.setText(preferences.getString(IP_ADDRESS_PREF, ""));
            editTextPort.setText(preferences.getString(PORT_PREF, ""));
        }

        textView.setMovementMethod(ScrollingMovementMethod.getInstance());

        getMessage.setOnClickListener(v -> {
            if (checkWifiIsEnable()) {
                Log.e("Button clicked", "请确保wifi已打开");
                return;
            }

            if (editTextIP.getText().toString().isEmpty() ||
                    editTextPort.getText().toString().isEmpty())
                return;

            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(REMEMBER_PWD_PREF, true);
            editor.putString(IP_ADDRESS_PREF, editTextIP.getText().toString());
            editor.putString(PORT_PREF, editTextPort.getText().toString());
            editor.apply();

            if (socketClient == null) {
                SocketCallBack print = new SocketCallBack() {
                    @Override
                    public void Print(String info) {
                        macAddress = info;
                        showMsg(info);
                        Log.d("Button clicked", info.trim());
                    }
                };

                String ipAddress = editTextIP.getText().toString();
                int port = Integer.parseInt(editTextPort.getText().toString());
                socketClient = new SocketClient(print, ipAddress, port);
            }

            socketClient.start();
        });
    }

    private void showMsg(String info) {
        new Thread(() -> runOnUiThread(() -> {
            textView.append("获取到Mac地址：" + info + "\r\n");

            int scrollAmount = textView.getLineCount() * textView.getLineHeight()
                    - textView.getHeight();
            if (scrollAmount > 0)
                textView.scrollTo(0, scrollAmount);

            if (!info.isEmpty() && socketClient != null) {
                socketClient.disconnect();
                socketClient = null;
            }
        })).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkWifiIsEnable())
            setWifiOn();

        if (NfcUtils.nfcAdapter != null)
            NfcUtils.nfcAdapter.enableForegroundDispatch(this, NfcUtils.pendingIntent,
                    NfcUtils.intentFilters, NfcUtils.techList);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "--------------NFC-------------");
        String action = intent.getAction();
//        macAddress = "f4:4e:fc:5f:14:d6";
        if (!macAddress.isEmpty() && (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))) {
            processIntent(intent, macAddress);
        } else {
            Log.e(TAG, "no equal discovered");
        }
    }

    public void processIntent(Intent intent, String writeData) {
        Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsg == null)
            return;

        Log.e("execute here: ", Arrays.toString(rawMsg));
        NdefMessage message = (NdefMessage) rawMsg[0];
        NdefRecord[] records = message.getRecords();
        String resultPayload = new String(records[0].getPayload());
        String resultType = new String(records[0].getType());
        short resultTnf = records[0].getTnf();
        Log.e(TAG, "resultPayload: " + resultPayload + ", resultType: " + resultType
                + ", resultTnf: " + String.valueOf(resultTnf));

        try {
            // 检测卡的id
            String id = nfcUtils.readNFCId(intent);
            Log.e(TAG, "processIntent--id: " + id);
            // NfcUtils中获取卡中数据的方法
            String result = nfcUtils.readNFCFromTag(intent);
            Log.e(TAG, "processIntent--result: " + result);
            // 往卡中写数据
            int ret = nfcUtils.writeNFCToTag(writeData, intent);
            if (ret == 0)
                Toast.makeText(this, writeData, Toast.LENGTH_LONG).show();
        } catch (IOException | FormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (NfcUtils.nfcAdapter != null)
            NfcUtils.nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NfcUtils.nfcAdapter = null;
    }

    private boolean checkWifiIsEnable() {
        return wifiManager == null || !wifiManager.isWifiEnabled();
    }

    private void setWifiOn() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED)
            wifiManager.setWifiEnabled(true);
    }
}