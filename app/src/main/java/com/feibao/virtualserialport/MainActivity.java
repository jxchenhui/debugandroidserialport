package com.feibao.virtualserialport;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.SystemClock;
import android.serialport.SerialPort;
import android.serialport.SerialPortFinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnconnect,btnsend;
    private EditText etsend;
    private TextView tvrecv;
    private Spinner spserial;

    private SerialPort serialPort = null;

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService recvsingleThreadExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        spserial = findViewById(R.id.spserial);
        btnconnect = findViewById(R.id.btnconnect);
        btnsend = findViewById(R.id.btnsend);
        etsend = findViewById(R.id.etsend);
        tvrecv = findViewById(R.id.tvrecv);

        SerialPortFinder.setVirtualSerialServer("192.168.1.31");

        String[] names = new SerialPortFinder().getAllDevicesPath();

        ArrayAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,names);

        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spserial.setAdapter(adapter);

        btnconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(serialPort != null){
                    serialPort.tryClose();
                    serialPort = null;
                    return;
                }
                if(spserial.getSelectedItem() == null){
                    return;
                }
                String path = spserial.getSelectedItem().toString();
                singleThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            serialPort = new SerialPort(path,9600,8,0,1,0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(serialPort != null){
                                        startRecv();
                                        spserial.setEnabled(false);
                                        btnconnect.setText("断开");
                                        Toast.makeText(MainActivity.this, "打开串口成功",Toast.LENGTH_SHORT).show();
                                    }else{

                                        Toast.makeText(MainActivity.this, "打开串口失败",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                    }
                });
            }
        });
        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = etsend.getText().toString();
                if(msg.length() == 0 || serialPort == null){
                    return;
                }

                addLog("发送:" + byteToHex(msg.getBytes()));
                singleThreadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            serialPort.getOutputStream().write(msg.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
    public static String byteToHex(byte[] bytes){
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex).append(" "); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }
    private void addLog(String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvrecv.append(msg + "\n");
                int offset=tvrecv.getLineCount()*tvrecv.getLineHeight();
                if(offset>tvrecv.getHeight()){
                    tvrecv.scrollTo(0,offset-tvrecv.getHeight());
                }
            }
        });
    }
    private void startRecv(){
        recvsingleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = serialPort.getInputStream();
                while (true){

                    try {
                        byte[] datas = new byte[1024];
                        int len = is.read(datas);
                        if(len <= 0){
                            throw new IOException();
                        }
                        byte[] realdatas = new byte[len];
                        System.arraycopy(datas,0,realdatas,0,realdatas.length);
                        String msg = "接收:" + byteToHex(realdatas);
                        addLog(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        addLog("连接已断开");
                        break;
                    }
                }
                serialPort = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnconnect.setText("连接");
                        spserial.setEnabled(true);
                    }
                });
            }
        });
    }
}