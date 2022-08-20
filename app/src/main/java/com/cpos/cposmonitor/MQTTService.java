package com.cpos.cposmonitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MQTTService extends Service {

    public static MQTTService _this;
 //   private android.widget.Toast ToastUtil;

    public interface IGetMessageCallBack {
//        public void OnMQTTMessageReturnError(String message);
        public void OnReceiveDeviceList(JSONArray device_list);
        public void OnReceiveCalling(JSONArray device_in);
    }

    public static final String TAG = MQTTService.class.getSimpleName();

    private static MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    //下面这一坨找后段要
    private String host = null;
    public static final String myWillTopic = "test";     //要订阅的主题
    public static final String strKeyPassword = "client1392419926";

    private IGetMessageCallBack callBack;
    static private boolean mLogin = false;
    Thread mLonginThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(getClass().getName(), "onCreate");
        _this = this;
        init();
    }

    public static void subscribe(String strTopic,  Integer qos) {
        Boolean retained = false;
        try {
            if (client != null) {
                client.subscribe(strTopic, qos);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void publish(String strTopic, String strMsg) {

        Integer qos = 0;
        Boolean retained = false;
        try {
            if (client != null) {

                byte send_buf[] = compress(strMsg);

                try {
                    client.publish(strTopic, send_buf, qos.intValue(), retained.booleanValue());
                } catch (Exception e) {
                    Log.e(TAG, "Library abnormal: cannot publish message ("+strTopic+")-("+strMsg+")");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SSLSocketFactory CreateSSLSocketFactory (InputStream trustStore, InputStream keyStore, String password) throws Exception {
        try{
            SSLContext ctx = null;
            SSLSocketFactory sslSockFactory=null;
            KeyStore ts;


            ts = KeyStore.getInstance("BKS");
            ts.load(trustStore, password.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ts);

            KeyStore ks = KeyStore.getInstance("BKS");
            ks.load(keyStore,  password.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(ks, password.toCharArray());
            ctx = SSLContext.getInstance("SSL");//
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslSockFactory=ctx.getSocketFactory();
            return sslSockFactory;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new MqttSecurityException(e);
        }
    }


    private void init()
    {
        // Only call after login

        try {
            if (MyApplication.get(MyApplication.SettingKey.SETTINGS_USERNAME) == null//
            && MyApplication.get(MyApplication.SettingKey.SETTINGS_PASSWORD) == null//
            ) {
                throw new NullPointerException("User didn't tried to login yet");
            }

            // 服务器地址（协议+地址+端口号）
            host = "ssl://"+MyApplication.get(MyApplication.SettingKey.SETTINGS_SERVER_IP, MyApplication.DEFAULT_IP)//
            +":"+MyApplication.get(MyApplication.SettingKey.SETTINGS_MQTT_PORT, MyApplication.DEFAULT_MQTT_PORT);
            client = new MqttAndroidClient(this, host, MyApplication.get(MyApplication.SettingKey.SETTINGS_DEVICE_ID));
            // 设置MQTT监听并且接受消息
            client.setCallback(mqttCallback);

            conOpt = new MqttConnectOptions();
            // 清除缓存
            conOpt.setCleanSession(true);
            // 设置超时时间，单位：秒
            conOpt.setConnectionTimeout(10);
            // 心跳包发送间隔，单位：秒
            conOpt.setKeepAliveInterval(20);
            // 用户名
            String username = MyApplication.get(MyApplication.SettingKey.SETTINGS_USERNAME);
            Log.i(TAG, "Username login:"+username);
            conOpt.setUserName(username);
            conOpt.setPassword(MyApplication.get(MyApplication.SettingKey.SETTINGS_PASSWORD).toCharArray());

            try {
                InputStream inputtrust = this.getApplicationContext().getResources().openRawResource(R.raw.clienttrustbksv1);
                conOpt.setSocketFactory(client.getSSLSocketFactory(inputtrust, strKeyPassword));
                Log.i(TAG, "load SSL truststore success");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "setSocketFactory SSL Error:", e);
            }

            //将字符串转换为字符串数组

            // last will message
            boolean doConnect = true;
            String message = "{\"terminal_uid\":\"" + MyApplication.get(MyApplication.SettingKey.SETTINGS_DEVICE_ID) + "\"}";

            String topic = myWillTopic;
            Integer qos = 0;
            Boolean retained = false;
            if ((!message.equals("")) || (!topic.equals(""))) {
                // 最后的遗嘱
                // MQTT本身就是为信号不稳定的网络设计的，所以难免一些客户端会无故的和Broker断开连接。
                //当客户端连接到Broker时，可以指定LWT，Broker会定期检测客户端是否有异常。
                //当客户端异常掉线时，Broker就往连接时指定的topic里推送当时指定的LWT消息。

                try {
                    conOpt.setWill(topic, message.getBytes(), qos.intValue(), retained.booleanValue());
                } catch (Exception e) {
                    Log.i(TAG, "Exception OccuMqttConnectOptionsred", e);
                    doConnect = false;
                    iMqttActionListener.onFailure(null, e);
                }
            }

            if (doConnect) {
                doClientConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "MQTT initialize failed", e);
        }
    }
    //扩容数组
    public static byte[] array_byte(int len){
        byte a[] = new byte[len];
        return a;
    }

    public static String decompress(byte[] src_data) {
        if (src_data.length == 0) {
            return null;
        }
        String strDecode = "";
        Inflater decompresser = new Inflater();
        decompresser.setInput(src_data, 0, src_data.length);
        // 对byte[]进行解压
        byte[] result = array_byte(src_data.length * 2+1);;
        int resultLength = 0;
        int i = 0;
        try {
            while (true) {
                resultLength = decompresser.inflate(result, 0, result.length); // 返回的是解压后的的数据包大小，
                if (resultLength > 0)
                {
                    strDecode += new String(result);
                }
                else
                {
                    if(decompresser.needsInput() == true)//解压结束, 需要再输入数据
                    {
                        break;
                    }

                    break;//因为数据为一次性输入,实际返回0即表示解压结束
                }
                i++;//测试数据需要解压多少次
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        decompresser.end();

        return strDecode;

    }

    //压缩
    public static byte[] compress(String data) {
        byte comp_data[]={0};
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DeflaterOutputStream zos = new DeflaterOutputStream(bos);
            zos.write(data.getBytes());
            zos.close();
            comp_data = bos.toByteArray();
           // return AbBase64.encode(bos.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return comp_data;
    }


    @Override
    public void onDestroy() {
        stopSelf();
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    // 连接MQTT服务器

    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNormal()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }


    public static void login() {
        _this.login(MyApplication.get(MyApplication.SettingKey.SETTINGS_USERNAME)
                , MyApplication.get(MyApplication.SettingKey.SETTINGS_PASSWORD)
                , MyApplication.get(MyApplication.SettingKey.SETTINGS_DEVICE_ID), 0);
    }

    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            // Log.i(TAG, "连接成功 "+arg0.getMessageId());
            String strMsg;
            strMsg = "Server Connected";
            MainActivity.msgShow(strMsg);

            if(mLonginThread == null) {
                Runnable runnable = new Runnable() {
                    public void run() {
                        while (mLogin == false) {
                            try {
                                login();
                                Thread.sleep(10000);//10秒
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        mLonginThread = null;
                    }
                };
                mLonginThread = new Thread(runnable);
                mLonginThread.start();
            }

        }



        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
           if(arg1 != null) arg1.printStackTrace();
            // 连接失败，重连
            String strMsg;
            strMsg = "Server Error: " + host;

            MainActivity.msgShow(strMsg);
            mLogin = false;

            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(10000);//10秒
                        doClientConnection();//重连

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();

        }
    };

    ////////////////


//bAutoLogin 0表示用户密码, 1表示使用MD5密码自动登入
//bSave 是否保存MD5密码
    private void login(String username, String password, String client_id , int bAutoLogin) {
        Log.i(TAG, "========== Login Process Begin ==========");
        if(username == null || username.isEmpty() || password == null || password.isEmpty() || client_id == null || client_id.isEmpty() ) {
            return;
        }
        try {
            String strDeviceName = "android_"+client_id;
            int msg_gate_type = 6;
            String strTopic;
            String strJson;
            String strTopicSubscribeClient = "";

            strTopic = "cpos/carpark/admin/receive";//服务器接收登录数据的主题
            strJson = "[{";
            strJson += "\"username\":\"" + username + "\",";
            strJson += "\"password\":\"" + password + "\",";
            strJson += "\"device_type\":\"" + Integer.toString(msg_gate_type) + "\",";
            strJson += "\"device_id\":\"" + client_id + "\",";
            strJson += "\"device_name\":\"" + strDeviceName + "\",";
            strJson += "\"auto_login\":\"" + Integer.toString(bAutoLogin) + "\",";
            strJson += "\"password_type\":\"" + Integer.toString(bAutoLogin) + "\",";
            strJson += "\"msg_type\":\"" + Integer.toString(16) + "\",";//16 - 请求登录
            char lst = strJson.charAt(strJson.length() - 1);
            if (lst == ',') {
                strJson = strJson.substring(0, strJson.length() - 1);
            }
            strJson += "}]";
            strTopicSubscribeClient = "cpos/carpark/" + username + "/" + client_id + "/receive";
            MQTTService.subscribe(strTopicSubscribeClient, 0);
            Log.i(TAG, "Login ("+strTopicSubscribeClient+") ("+strJson+")");
            MQTTService.publish(strTopic, strJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        init();

        Log.i(TAG, "========== Login Process End ==========");
    }

    private String UserDeviceRequest(String username) {
        String strJson = "[{";
        strJson += "\"username\":\"" + username + "\",";
        strJson += "\"device_id\":\"" + MyApplication.get(MyApplication.SettingKey.SETTINGS_DEVICE_ID) + "\",";
        strJson += "\"msg_type\":\"" + Integer.toString(17) + "\",";//16 - 请求登录
        char lst = strJson.charAt(strJson.length() - 1);
        if (lst == ',') {
            strJson = strJson.substring(0, strJson.length() - 1);
        }
        strJson += "}]";
        Log.i(TAG, "========== Get User Device Process Begin ==========");
        try {
            String strTopic = "cpos/carpark/admin/receive";//服务器接收登录数据的主题
            publish(strTopic,strJson);
            Log.i(TAG, "Ask for device list ("+strTopic+") ("+strJson+")");
        } catch (Exception e) {
            Log.e(TAG, "Ask device failed "+e.getMessage());
            e.printStackTrace();
        }
        Log.i(TAG, "========== Get User Device Process End ==========");
        return strJson;
    }


    private void MqttMessageReceive(String topic, String strMsgReceive)
    {
        Log.i(TAG, "========== Receive MQTT Message ========== BEGIN");
        Log.i(TAG, "Message content ("+topic+") ("+strMsgReceive+")");
        String msg_type = "";
        String username = "";
        String result = "";
        try {

            JSONArray jsonArray = new JSONArray(strMsgReceive);
            Log.i(TAG, "Message content array ("+jsonArray.toString()+")");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Log.i(TAG, "Message content Object ("+jsonObject+")");
                msg_type = jsonObject.getString("msg_type");
                if(msg_type.equals("16")) {
                    Log.i(TAG, "Login response received");
                    username = jsonObject.getString("username");
                    result = jsonObject.getString("result");
                    if(Integer.valueOf(result) == 1) {
                        MainActivity.msgShow("Login Succeed");
                        mLogin = true;
                        UserDeviceRequest(username);
                    } else {
                        MainActivity.msgShow("Login Failed");
                        mLogin = false;
//                        isError = 0;
                    }
                } else if(msg_type.equals("17")) {
                    Log.i(TAG, "Device list received");
                    Log.i(TAG, "IGetMessageCallBack:"+ callBack);
                    if (callBack != null) {
                        callBack.OnReceiveDeviceList(jsonArray);
                    }
                    break;
                } else if(msg_type.equals("18")) {
                    Log.i(TAG, "Calling received");
                    Log.i(TAG, "IGetMessageCallBack:"+ callBack);
                    if (callBack != null) {
                        callBack.OnReceiveCalling(jsonArray);
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "========== Receive MQTT Message ========== END");
    }


    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.i(TAG, "Receive Data Source： " + new String(message.getPayload()));

            String str_receive = new String(decompress(message.getPayload()));
            MqttMessageReceive(topic, str_receive);
            String str2 = topic + ";qos:" + message.getQos() + ";retained:" + message.isRetained();
            Log.i(TAG, "Receive Data Decode:" + str_receive);
            Log.i(TAG, str2);
            message.clearPayload();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.e(TAG,arg0.toString());
        }

        @Override
        public void connectionLost(Throwable arg0) {
            String strMsg;
            strMsg = "Connect Lost";
            MainActivity.msgShow(strMsg);
            iMqttActionListener.onFailure(null, null);
            // 失去连接，重连
        }
    };

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNormal() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(getClass().getName(), "onBind");
        return new CustomBinder();
    }

    public void setCallBack(IGetMessageCallBack callBack) {
        this.callBack = callBack;
    }

    public class CustomBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    public void toCreateNotification(String message) {

//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(this, MQTTService.class), PendingIntent.FLAG_UPDATE_CURRENT);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);//3、创建一个通知，属性太多，使用构造器模式
//
//        Notification notification = builder
//                .setTicker("测试标题")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle("")
//                .setContentText(message)
//                .setContentInfo("")
//                .setContentIntent(pendingIntent)//点击后才触发的意图，“挂起的”意图
//                .setAutoCancel(true)        //设置点击之后notification消失
//                .build();
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        startForeground(0, notification);
//        notificationManager.notify(0, notification);

    }
}

