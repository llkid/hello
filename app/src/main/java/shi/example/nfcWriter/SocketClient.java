package shi.example.nfcWriter;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.io.InputStream;
import java.util.concurrent.Executors;

import shi.tool.SocketCallBack;

public class SocketClient {
    public String ipAddress = "192.168.16.163";
    public int port = 50250;
    static final String GBK = "GBK";

    public Socket socket;
    public SocketCallBack socketCallBack;

    public SocketClient(SocketCallBack print, String ipAddress, int port) {
        this.socketCallBack = print;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void start() {
        if (socket != null && socket.isConnected())
            return;

        Executors.newCachedThreadPool().execute(() -> {
            try {
                if (socket == null) {
                    InetAddress ip = InetAddress.getByName(ipAddress);

                    socket = new Socket(ip, port);
                }
            } catch (Exception e) {
                if (socketCallBack != null)
                    socketCallBack.Print("连接服务器失败" + e.toString());
            }

//                接收 socket 数据
            try {
                if (socket != null) {
                    InputStream inputStream = socket.getInputStream();

                    // 3M 缓存
                    byte[] buffer = new byte[1024 * 1024 * 3];
                    int len = -1;
                    while (socket.isConnected() && (len = inputStream.read(buffer)) != -1) {
                        String data = new String(buffer, 0, len, GBK);
                        // 通过回调接口将获取到的数据推送出去
                        if (socketCallBack != null)
                            socketCallBack.Print(data.trim());
                    }
                }
            } catch (Exception e) {
                if (socketCallBack != null)
                    socketCallBack.Print("socket接收信息失败" + e.toString());
            }
        });
    }

    public void Send(String data) {

        try {
            if(socket != null && socket.isConnected()) {
                byte[] bytes = data.getBytes(GBK);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(bytes);

                if (socketCallBack != null)
                    socketCallBack.Print("发送信息 -> " + data);
            } else {
                if (socketCallBack != null)
                    socketCallBack.Print("未连接服务器！清先连接后，再发送。");
            }
        } catch (Exception e) {
            if (socketCallBack != null)
                socketCallBack.Print("发送socket信息失败！" + e.toString());
        }
    }

    public void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                socket = null;
            }
        } catch (IOException ignored) {
            if (socketCallBack != null)
                socketCallBack.Print("socket断开失败!");
        }
    }

    public void abort() {
        try {
            disconnect();
            start();
        } catch (Exception e) {
            Log.d("Abort socket", e.toString());
        }
    }
}
