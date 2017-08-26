package com.lzm.socketdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by lzm on 2017/8/25.
 */
public class TCPServer extends Service {

    private static final String TAG        = "TCPServer";
    private boolean mIsServiceDestoryed = false;
    private String[] mDefinedMessage    = new String[]{
            "你好啊，哈哈","请问你叫什么名字啊","今天天气不错啊","你知道吗，我可以和多个人聊天哦","据说爱笑的人运气都不会太差的哦"
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new MyServer()).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceDestoryed = true;
    }

    class MyServer implements Runnable {

        @Override
        public void run() {
            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket(12345);
            } catch (IOException e) {
                Log.d(TAG, "start tcp server failed");
                e.printStackTrace();
                return;
            }

            while (!mIsServiceDestoryed)
            {
                // 接受客户端请求
                try {
                    final Socket client = serverSocket.accept();
                    Log.d(TAG, "server connect success");
                    // 和多个客户端聊天
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                responseClient(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void responseClient(Socket client) throws IOException {
        // 接受客户端信息
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        // 客户端发消息
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
        out.println("欢迎来到聊天室！");
        while (!mIsServiceDestoryed) {
            String clientMsg = in.readLine();
            Log.d(TAG, "msg from client : " + clientMsg);
            if (clientMsg == null) {
                // 客戶端断开
                break;
            }
            int i = new Random().nextInt(mDefinedMessage.length);
            String msg = mDefinedMessage[i];
            out.println(msg);
            Log.d(TAG, "send to client : " + msg);
        }

        if (out != null) {
            out.close();
        }
        if (in != null) {
            in.close();
        }
        if (client != null) {
            client.close();
        }
    }
}
