package com.lzm.socketdemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button mButton;
    private EditText mEditText;
    private TextView mTextView;
    private Socket mClientSocket;
    private PrintWriter mPrintWriter;
    private static final int MESSAGE_RECEIVER_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_RECEIVER_NEW_MSG:
                    mTextView.setText(mTextView.getText() + (String)msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    mButton.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initData() {
        Intent intent = new Intent(this, TCPServer.class);
        startService(intent);
        mButton.setOnClickListener(this);
        /**
         * 启动client之前一定确保 server已经启动
         */
        new Handler().postDelayed(new Runnable() {  // 延迟消息是在子线程等待 结束后又回到主线程 最终结果还是在主线程
            @Override
            public void run() {
                new Thread(){
                    @Override
                    public void run() {
                        connectTcpServer();
                    }
                }.start();
            }
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void connectTcpServer() {
        Socket socket = null;
        while (socket == null) {
            try {
                socket = new Socket("192.168.5.7", 12345);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                Log.d(TAG, "client to server success !");
            } catch (IOException e) {
                SystemClock.sleep(1000);
                Log.d(TAG, "client to server failed !");
                e.printStackTrace();
            }
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!MainActivity.this.isFinishing()) {
                String msg = br.readLine();
                Log.d(TAG,"msg from server : " + msg);

                if (msg != null) {
                    String time = formatDateTime(System.currentTimeMillis());
                    String showMsg = "Server " + time + " : " + msg + "\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVER_NEW_MSG,showMsg).sendToTarget();
                }
            }
            if (br != null) {
                br.close();
            }
            if (mPrintWriter != null) {
                mPrintWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        mButton = (Button) findViewById(R.id.client_bt);
        mEditText = (EditText) findViewById(R.id.client_et);
        mTextView = (TextView) findViewById(R.id.client_tv);
    }

    @Override
    public void onClick(View view) {
        String msg = mEditText.getText().toString();
        if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
            mPrintWriter.println(msg);
            mEditText.setText("");
            String time = formatDateTime(System.currentTimeMillis());
            String showMsg = "Client " + time + " : " + msg + "\n";
            mTextView.setText(mTextView.getText() + showMsg);
        }
    }

    private String formatDateTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
