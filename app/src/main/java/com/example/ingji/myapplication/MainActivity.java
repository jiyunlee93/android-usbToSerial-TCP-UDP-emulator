package com.example.ingji.myapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
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
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import static android.os.SystemClock.sleep;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    UsbSerialDriver driver;
    UsbDeviceConnection connection;
    UsbSerialPort port;
    String receivedText = "";
    int port_baudRate = 9600;
    int port_dataBit = 8;
    boolean portStatus = false;
    Handler handler = new Handler();
    TextView txtResult;

    //for tcp/ip
    String ip = "192.168.1.110"; // IP
    int tcp_port = 1470; // PORT번호

    EditText edtServerPort, edtClientPort, edtClientIPAddr, edtServerSend, edtClientSend, editUsbToSerialSend;
    EditText edtUdpLocalPort, edtUdpTargetIP, edtUdpTargetPort, edtUdpSend;
    Button btnUsbToSerialConnect, btnUsbToSerialSend, btnServerConnect, btnClientConnect, btnServerSend, btnClientSend;
    Button btnUsbToSerialreSend, btnServerreSend, btnClientreSend, btnUdpConnect, btnUdpSend, btnUdpreSend;
    TextView txtServerIP;
    //for tcp & udp
    boolean tcpThread = false;
    boolean serverStatus = false;
    boolean clientStatus = false;
    ByteArrayOutputStream byteArrayOutputStream;
    InputStream in;
    PrintWriter writer;
    ServerSocket serverSocket_task;
    Socket socket_task;

    // for UDP
    boolean udpThread = false;
    boolean udpStatus = false;
    DatagramSocket socket;


    boolean checkError = false;
    String errorString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_tcpserver);
        //initialization
        receivedText = "";
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
                udp(v);
                break;
            case R.id.btn_usbToSerial:
                setContentView(R.layout.activity_usb_to_serial);
                usbToSerial(v);
                break;
        }
    }

    class SendData extends Thread {
        public void run() {
            try {
                String sIP = edtUdpTargetIP.getText().toString();
                int sPORT = Integer.valueOf(edtUdpTargetPort.getText().toString());
                if (udpStatus) {
                    //UDP 통신용 소켓 생성
                    DatagramSocket socket = new DatagramSocket();
                    //서버 주소 변수
                    InetAddress serverAddr = InetAddress.getByName(sIP);

                    //보낼 데이터 생성
                    String temp = edtUdpSend.getText().toString();
                    byte[] buf = temp.getBytes();


                    //패킷으로 변경
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, sPORT);
                    socket.send(packet);

                    socket.receive(packet);
                    //데이터 수신되었다면 문자열로 변환
                    // String msg = new String(packet.getData());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void udp(View v) {
        edtUdpLocalPort = findViewById(R.id.edt_UdplocalPort);
        edtUdpTargetIP = findViewById(R.id.edt_UdpTargetIP);
        edtUdpTargetPort = findViewById(R.id.edt_UdpTargetPort);
        btnUdpConnect = findViewById(R.id.btn_UdpConnect);
        btnUdpreSend = findViewById(R.id.btn_ServreSend);
        edtUdpSend = findViewById(R.id.edt_UdpSend);
        btnUdpSend = findViewById(R.id.btn_UdpSend);
        btnUdpreSend = findViewById(R.id.btn_UdpreSend);
        txtResult = findViewById(R.id.txt_result);
        txtResult.setText(receivedText);
        btnUdpConnect.setOnClickListener(buttonUdpConnectOnClickListener);
        btnUdpSend.setOnClickListener(buttonUdpSendOnClickListener);
        btnUdpreSend.setOnClickListener(buttonUdpreSendOnClickListener);
    }

    public void udpReceived() {
        try {
            // 데이터를 받을 버퍼
            byte[] inbuf = new byte[256];
            // 데이터를 받을 Packet 생성
            DatagramPacket packet = new DatagramPacket(inbuf, inbuf.length);
            // 데이터 수신 // 데이터가 수신될 때까지 대기됨
            socket.receive(packet);
            if (udpStatus) {
                receivedText += new String(packet.getData());
            }
        } catch (IOException e) {
            checkError = true;
            errorString = e.toString();
        }
    }

    public void udpThread() {
        Toast.makeText(getApplicationContext(), "UDP Thread Start", Toast.LENGTH_SHORT).show();
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (udpStatus) {
                        udpReceived();
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            if (udpStatus) {
                                txtResult.setText(receivedText);
                            }
                            if (checkError) {
                                Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
                                checkError = false;
                            }
                        }
                    });
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {
                        checkError = true;
                        errorString = e.toString();
                    }
                }
            }
        });
        t.start();
    }

    View.OnClickListener buttonUdpConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (btnUdpConnect.getText().toString().equals("DISCONNECT")) {
                //DISCONNECT 코드
                udpStatus = false;
                btnUdpConnect.setText(R.string.connect);
                socket.close();
            } else {
                //CONNECT 코드
                tcp_port = Integer.valueOf(edtUdpLocalPort.getText().toString());
                try {
                    socket = new DatagramSocket(tcp_port);
                } catch (SocketException e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
                udpStatus = true;
                if (!udpThread) {
                    udpThread();
                    udpThread = true;
                }
                btnUdpConnect.setText(R.string.disconnect);
            }
        }
    };
    View.OnClickListener buttonUdpSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            SendData mSendData = new SendData();
            //보내기 시작
            mSendData.start();
        }
    };
    View.OnClickListener buttonUdpreSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            SendData mSendData = new SendData();
            //보내기 시작
            mSendData.start();
        }
    };

    public void usbToSerial(View v) {
        btnUsbToSerialConnect = findViewById(R.id.btn_UsbConnect);
        btnUsbToSerialSend = findViewById(R.id.btn_UsbSend);
        btnUsbToSerialreSend = findViewById(R.id.btn_UsbreSend);
        editUsbToSerialSend = findViewById(R.id.edt_UsbSend);
        txtResult = findViewById(R.id.txt_result);
        txtResult.setText(receivedText);
        btnUsbToSerialConnect.setOnClickListener(buttonUsbToSerialConnectOnClickListener);
        btnUsbToSerialSend.setOnClickListener(buttonUsbToSerialSendOnClickListener);
        btnUsbToSerialreSend.setOnClickListener(buttonUsbToSerialreSendOnClickListener);
    }

    View.OnClickListener buttonUsbToSerialConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (btnUsbToSerialConnect.getText().toString().trim().equals("DISCONNECT")) {
                //연결 상태에서 버튼 클릭시 DISCONNECT
                btnUsbToSerialConnect.setText(R.string.connect);
                portClose();
                portStatus = false;
            } else {
                btnUsbToSerialConnect.setText(R.string.disconnect);
                portOpen();
                usbToSerialThread();
            }
        }
    };
    View.OnClickListener buttonUsbToSerialSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (!portStatus) {
                Toast.makeText(getApplicationContext(), "port closeDDD", Toast.LENGTH_SHORT).show();
                return;
            }
            String a = editUsbToSerialSend.getText().toString();
            byte buffer[];
            buffer = a.getBytes();
            try {
                port.write(buffer, 1000);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "send write Error", Toast.LENGTH_SHORT).show();
            }
        }
    };
    View.OnClickListener buttonUsbToSerialreSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (!portStatus) {
                Toast.makeText(getApplicationContext(), "port closeDDD", Toast.LENGTH_SHORT).show();
                return;
            }
            String a = txtResult.getText().toString();
            byte buffer[];
            buffer = a.getBytes();
            try {
                port.write(buffer, 1000);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "send write Error", Toast.LENGTH_SHORT).show();
            }
        }
    };
    public void usbToSerialThread() {
        Toast.makeText(getApplicationContext(), "USB THREAD START", Toast.LENGTH_SHORT).show();
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    usbReceived();
                    handler.post(new Runnable() {
                        public void run() {
                            if (portStatus) {
                                txtResult.setText(receivedText);
                            }
                            if (checkError) {
                                checkError = false;
                                Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {
                        checkError = true;
                        errorString = e.toString();
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

    public void usbReceived() {
        if (!portStatus) {
            return;
        }
        byte buffer[] = new byte[32];
        try {
            port.read(buffer, 1000);
        } catch (IOException er) {
            checkError = true;
            errorString = er.toString();
        }
        try {
            String a = new String(buffer, "UTF-8");
            if (!(a.isEmpty())) {
                receivedText += a;
            }
        } catch (Exception err) {
            checkError = true;
            errorString = err.toString();
        }
    }

    public void portClose() {
        try {
            port.close();
        } catch (IOException e2) {
            Toast.makeText(getApplicationContext(), "port close error", Toast.LENGTH_LONG).show();
        }
        return;
    }

    public void tcpClient(View v) {
        edtClientIPAddr = findViewById(R.id.edt_ClientIP);
        edtClientPort = findViewById(R.id.edt_ClientPort);
        edtClientSend = findViewById(R.id.edt_ClientSend);
        btnClientConnect = findViewById(R.id.btn_ClientConnect);
        btnClientSend = findViewById(R.id.btn_ClientSend);
        btnClientreSend = findViewById(R.id.btn_ClientreSend);
        txtResult = findViewById(R.id.txt_result);
        txtResult.setText(receivedText);
        btnClientConnect.setOnClickListener(buttonClientConnectOnClickListener);
        btnClientSend.setOnClickListener(buttonClientSendOnClickListener);
        btnClientreSend.setOnClickListener(buttonClientreSendOnClickListener);
    }

    View.OnClickListener buttonClientConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (btnClientConnect.getText().toString().trim().equals("DISCONNECT")) {
                //연결 상태에서 버튼 클릭시 DISCONNECT
                btnClientConnect.setText(R.string.connect);
                clientStatus=false;
            } else {
                tcp_port = Integer.parseInt(edtClientPort.getText().toString());
                ip = edtClientIPAddr.getText().toString();
                btnClientConnect.setText(R.string.disconnect);
                clientStatus=true;
                if(!tcpThread){
                    tcpThread=true;
                    tcpThread();
                }
            }
        }
    };
    View.OnClickListener buttonClientSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(clientStatus){
                String sendData = edtClientSend.getText().toString();
                writer.println(sendData);
            }
        }
    };
    View.OnClickListener buttonClientreSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(clientStatus)
                writer.println(receivedText);
        }
    };
    public void tcpServer(View v) {
        //Server 화면 처리
        edtServerPort = findViewById(R.id.edt_ServPort);
        edtServerSend = findViewById(R.id.edt_ServSend);
        btnServerConnect = findViewById(R.id.btn_ServConnect);
        btnServerSend = findViewById(R.id.btn_ServSend);
        btnServerreSend = findViewById(R.id.btn_ServreSend);
        txtServerIP = findViewById(R.id.txt_ServIP);
        txtResult = findViewById(R.id.txt_result);
        txtResult.setText(receivedText);

        //Get current device's IP Address
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        txtServerIP.setText(ip);
        //button listener
        btnServerConnect.setOnClickListener(buttonServerConnectOnClickListener);
        btnServerSend.setOnClickListener(buttonServerSendOnClickListener);
        btnServerreSend.setOnClickListener(buttonServerreSendOnClickListener);
    }

    View.OnClickListener buttonServerConnectOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if (btnServerConnect.getText().toString().trim().equals("DISCONNECT")) {
                //연결 상태에서 버튼 클릭시 DISCONNECT
                btnServerConnect.setText(R.string.connect);
                serverStatus=false;
            } else {
                tcp_port = Integer.parseInt(edtServerPort.getText().toString());
                ip = "";
                serverStatus=true;
                if(!tcpThread){
                    tcpThread();
                    tcpThread=true;
                }
                btnServerConnect.setText(R.string.disconnect);
            }
        }
    };
    View.OnClickListener buttonServerSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(serverStatus){
                String sendData = edtServerSend.getText().toString();
                writer.println(sendData);
            }
        }
    };
    View.OnClickListener buttonServerreSendOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            if(serverStatus)
                writer.println(receivedText);
        }
    };
    public void tcpThread() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    if (serverStatus) {
                        //Server
                        serverSocket_task = new ServerSocket(tcp_port);
                        //server 에서는 socket 변수가 clientSocket
                        socket_task = serverSocket_task.accept();
                    } else if (clientStatus) {
                        //Client
                        socket_task = new Socket(ip, tcp_port);
                    }
                    byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    in = socket_task.getInputStream();
                    writer = new PrintWriter(socket_task.getOutputStream(), true);
                } catch (IOException e) {
                    checkError = true;
                    errorString = e.toString();
                }
                while (true) {
                    if (serverStatus || clientStatus) {
                        tcpRead();
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            txtResult.setText(receivedText);
                            if (checkError) {
                                Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
                                checkError = false;
                            }
                        }
                    });
                    sleep(50);
                }
            }
        });
        t.start();
    }
    public void tcpRead() {
        //initialization
        int bytesRead;
        try {
            if (in.available() > 0) {
                byte[] buffer = new byte[1024];
                bytesRead = in.read(buffer);
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                receivedText = byteArrayOutputStream.toString("UTF-8");
            }
            if (!socket_task.isBound()) {
                return;
            }
            sleep(30);
        } catch (IOException e) {
            checkError = true;
            errorString = e.toString();
        }
    }
}

