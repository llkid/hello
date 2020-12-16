package shi.tool;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NfcUtils {
    public static NfcAdapter nfcAdapter;
    public static IntentFilter[] intentFilters = null;
    public static PendingIntent pendingIntent = null;
    public static String[][] techList = null;

    public NfcUtils(Activity activity) {
        nfcAdapter = NfcCheck(activity);
        NfcInit(activity);
    }

    public static void NfcInit(Activity activity) {
        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            intentFilter.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }

        techList = new String[][] {{MifareClassic.class.getName()},{NfcA.class.getName()}};
        intentFilters = new IntentFilter[]{intentFilter};
    }

    public static NfcAdapter NfcCheck(Activity activity) {
        NfcAdapter nfcAdapter_ = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter_ == null) {
            Toast.makeText(activity, "该设备不支持NFC功能或NFC功能未打开！", Toast.LENGTH_SHORT).show();
            return null;
        } else {
            if (!nfcAdapter_.isEnabled()) {
                IsToSet(activity);
            } else {
                Toast.makeText(activity, "NFC功能已打开", Toast.LENGTH_SHORT).show();
            }
        }
        return nfcAdapter_;
    }

    private static void IsToSet(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("是否跳转到设置页面打开NFC功能");
        builder.setPositiveButton("确认", (dialog, which) -> {
            goToSet(activity);
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();;
        });
        builder.create().show();
    }

    private static void goToSet(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        activity.startActivity(intent);
    }

    public String readNFCFromTag(Intent intent) {
        String readResult = "数据为空";
        Parcelable[] rawArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawArray != null) {
            NdefMessage ndefMessage = (NdefMessage) rawArray[0];
            NdefRecord ndefRecord = ndefMessage.getRecords()[0];
            if (ndefRecord != null) {
                readResult = new String(ndefRecord.getPayload(), StandardCharsets.UTF_8);
            }
        }
        return readResult;
    }

    public int writeNFCToTag(String data, Intent intent) throws IOException, FormatException {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();

//        NdefRecord ndefRecord = NdefRecord.createApplicationRecord(data);
        // 0800D6145FFC4EF4
        NdefRecord autoConnectBluetoothRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                "application/vnd.bluetooth.ep.oob".getBytes(),
                new byte[0],
                toBytes(data));
        Log.d("writeNFCToTag", Arrays.toString(toBytes(data)));
        NdefRecord[] records = {autoConnectBluetoothRecord};

        NdefMessage ndefMessage = new NdefMessage(records);
        ndef.writeNdefMessage(ndefMessage);

        Log.i("writeNFCToTag", "成功写入MAC地址: " + data);
        return 0;
    }

    public String readNFCId(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        return ByteArrayToHexString(tag.getId());
    }

    private static String ByteArrayToHexString(byte [] idArray) {
        int i, j, in;
        String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuilder out = new StringBuilder();

        for (j = 0; j < idArray.length; ++j) {
            in = (int) idArray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out.append(hex[i]);
            i = in & 0x0f;
            out.append(hex[i]);
        }

        return out.toString();
    }

    public static byte[] toBytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }

        String[] tmp = str.split(":");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = tmp.length - 1; i >= 0; --i) {
            stringBuilder.append(tmp[i]);
        }
        str = "0800" + stringBuilder.toString();
        // D6 14 5F FC 4E F4
        // F4 4E FC 5F 14 D6

        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }
}
