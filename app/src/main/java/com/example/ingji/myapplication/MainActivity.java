package com.example.ingji.myapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static android.os.SystemClock.sleep;

public class MainActivity extends AppCompatActivity{
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    UsbSerialDriver driver;
    UsbDeviceConnection connection;
    UsbSerialPort port;
    String allText = "";
    int port_baudRate = 9600;
    int port_dataBit = 8;
    boolean portStatus = false;
    Handler handler = new Handler();
    TextView txtResult;
    Button btnUsbToSerialConnect, btnUsbToSerialSend;
    EditText editUsbToSerialSend;

    //for tcp/ip
    String ip = "192.168.1.110"; // IP
    int tcp_port = 1470; // PORT번호

    private EditText edtServerPort, edtClientPort, edtClientIPAddr, edtServerSend, edtClientSend;
    private Button btnServerConnect, btnClientConnect, btnServerSend, btnClientSend;
    private TextView textView, txtServerIP;

    //Handler handler = new Handler();
    NetworkTask myTask;
    boolean checkSend;
    String sendData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_usb_to_serial);
        myTask = new NetworkTask(ip, tcp_port);
    }
    public void onButtonClick(View v) {
        switch (v.getId()) {
            case R.id.btn_server:
                setContentView(R.layout.activity_tcpserver);
                tcpServer(v);
                break;
            case R.id.btn_client:
                setContentView(R.layout.activity_tcpclient);
                tcpClient(v);
                break;
            case R.id.btn_udp:
                setContentView(R.layout.activity_udp);
                break;
            case R.id.btn_usbToSerial:
                setContentView(R.layout.activity_usb_to_serial);
                usbToSerial(v);
                break;
        }
    }
    public void usbToSerial(View v){
        btnUsbToSerialConnect = (Button) findViewById(R.id.btn_UsbConnect);
        btnUsbToSerialSend = (Button) findViewById(R.id.btn_UsbSend);
        editUsbToSerialSend = (EditText) findViewById(R.id.edt_UsbSend);
        txtResult = (TextView) findViewById(R.id.txt_result);
        btnUsbToSerialConnect.setOnClickListener(buttonUsbToSerialConnectOnClickListener);
        btnUsbToSerialSend.setOnClickListener(buttonUsbToSerialSendOnClickListener);
    }
    View.OnClickListener buttonUsbToSerialConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(btnUsbToSerialConnect.getText().toString().trim().equals("DISCONNECT")){
                //연결 상태에서 버튼 클릭시 DISCONNECT
                btnUsbToSerialConnect.setText("CONNECT");
                portClose();
                portStatus=false;
            }else{
                btnUsbToSerialConnect.setText("DISCONNECT");
                portOpen();
                startThreading();
            }
        }
    };
    View.OnClickListener buttonUsbToSerialSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (!portStatus) {
                Toast.makeText(getApplicationContext(), "port closeDDD", Toast.LENGTH_SHORT).show();
                return;
            }
            String a =editUsbToSerialSend.getText().toString();
            byte buffer[] = new byte[16];
            buffer = a.getBytes();
            try {
                port.write(buffer, 1000);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "send write Error", Toast.LENGTH_SHORT).show();
            }
        }
    };
    public void startThreading() {
        Toast.makeText(getApplicationContext(), "Connecting ~~", Toast.LENGTH_SHORT).show();
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    receiveMessage();
                    handler.post(new Runnable() {
                        public void run() {
                            if (portStatus) {
                                txtResult.setText(allText);
                            }
                        }
                    });
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Thread sleep error", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        t.start();
    }
    public boolean portOpen() {
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Toast.makeText(getApplicationContext(), "availableDrivers is null", Toast.LENGTH_LONG).show();
            return false;
        }
        // Open a connection to the first available driver.
        driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            Toast.makeText(getApplicationContext(), "connection is null", Toast.LENGTH_LONG).show();
            //USB에 연결하기 위한 permission 을 유저에게 묻는 문구 보여주기 위해 request Permission()에서 호출
            manager.requestPermission(driver.getDevice(), mPermissionIntent);
            return false;
        }
        // Read some data! Most have just one port (port 0).
        port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(port_baudRate, port_dataBit, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "port open && setParameter exception", Toast.LENGTH_SHORT).show();
            return false;
        }
        portStatus = true;
        return true;
    }
    public void receiveMessage() {
        if (!portStatus) {
            return;
        }
        byte buffer[] = new byte[160];
        int numBytesRead = 0;
        try {
            numBytesRead = port.read(buffer, 1000);
        } catch (IOException er) {
            Toast.makeText(getApplicationContext(), "port read exception", Toast.LENGTH_SHORT).show();
            numBytesRead = -1;
        }
        try {
            String a = new String(buffer, "UTF-8");
            if (!(a.isEmpty())) {
                allText += a;
            }
        } catch (Exception err) {
            Toast.makeText(getApplicationContext(), "here", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean portClose() {
        try {
            port.close();
        } catch (IOException e2) {
            Toast.makeText(getApplicationContext(), "port close error", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public void tcpClient(View v){
        edtClientIPAddr = (EditText) findViewById(R.id.edt_ClientIP);
        edtClientPort = (EditText) findViewById(R.id.edt_ClientPort);
        edtClientSend = (EditText) findViewById(R.id.edt_ClientSend);
        btnClientConnect = (Button) findViewById(R.id.btn_ClientConnect);
        btnClientSend = (Button) findViewById(R.id.btn_ClientSend);

        btnClientConnect.setOnClickListener(buttonClientConnectOnClickListener);
        btnClientSend.setOnClickListener(buttonClientSendOnClickListener);
    }
    View.OnClickListener buttonClientConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(btnClientConnect.getText().toString().trim().equals("DISCONNECT")){
                //연결 상태에서 버튼 클릭시 DISCONNECT
                btnClientConnect.setText("CONNECT");
                myTask.onPostExecute(null);
            }
            tcp_port = Integer.parseInt(edtClientPort.getText().toString());
            ip = edtClientIPAddr.getText().toString();
            btnClientConnect.setText("DISCONNECT");
            myTask = new NetworkTask(ip, tcp_port);
            myTask.execute(1);
        }
    };
    View.OnClickListener buttonClientSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            sendData = edtClientSend.getText().toString();
            checkSend = true;
        }
    };
    public void tcpServer(View v) {
        //Server 화면 처리
        edtServerPort = (EditText) findViewById(R.id.edt_ServPort);
        edtServerSend = (EditText) findViewById(R.id.edt_ServSend);
        btnServerConnect = (Button) findViewById(R.id.btn_ServConnect);
        btnServerSend = (Button) findViewById(R.id.btn_ServSend);
        txtServerIP = (TextView) findViewById(R.id.txt_ServIP);

        //Get current device's IP Address
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        txtServerIP.setText(ip);

        //button listener
        btnServerConnect.setOnClickListener(buttonServerConnectOnClickListener);
        btnServerSend.setOnClickListener(buttonServerSendOnClickListener);
    }
    View.OnClickListener buttonServerConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(btnServerConnect.getText().toString().trim().equals("DISCONNECT")){
                //연결 상태에서 버튼 클릭시 DISCONNECT
                btnServerConnect.setText("CONNECT");
                myTask.onPostExecute(null);
            }else{
                tcp_port = Integer.parseInt(edtServerPort.getText().toString());
                ip = "";
                btnServerConnect.setText("DISCONNECT");
                myTask.dstAddress="";
                myTask.dstPort=tcp_port;
                myTask.execute(0);
            }
        }
    };
    View.OnClickListener buttonServerSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            sendData = edtServerSend.getText().toString();
            checkSend = true;
        }
    };
    public class NetworkTask extends AsyncTask<Integer, Void, Void> {
        String dstAddress;
        int dstPort;
        Socket socket;
        ServerSocket serverSocket;

        OutputStream out;
        InputStream in;
        String response;
        int checkServerClient;

        NetworkTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
            response = "";
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Integer... arg0) {
            checkServerClient = arg0[0];
            try {
                if (checkServerClient == 0) {
                    //Server
                    serverSocket = new ServerSocket(dstPort);
                    //server 에서는 socket 변수가 clientSocket
                    socket = serverSocket.accept();
                } else {
                    //Client
                    socket = new Socket(dstAddress, dstPort);
                }
                //initialization
                int bytesRead = 0;
                in = socket.getInputStream();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                while (true) {
                    //buffer clear
                    byte[] buffer = new byte[1024];
                    //읽을 값이 있을 때만  read 함수 실행
                    if (in.available() > 0) {
                        bytesRead = in.read(buffer);
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                        response = byteArrayOutputStream.toString("UTF-8");
                        publishProgress();
                    }
                    if (bytesRead == -1) {
                        return null;
                    }
                    if (checkSend) {
                        writer.println(sendData);
                        checkSend = false;
                    }
                    if (!socket.isBound()) {
                        return null;
                    }
                    if(isCancelled()){
                        return null;
                    }
                    // Client일 때 서버가 연결 끊으면 자동 종료
                    //thread sleep
                    sleep(50);
                    //if(socket.isClosed() || !socket.isConnected() || !socket.getKeepAlive()){
                    //    return null;
                    //}
                }
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
            textView = (TextView) findViewById(R.id.txt_result);
            textView.setText(response);
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "enddddddddddddddd", Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
        }
    }


}

