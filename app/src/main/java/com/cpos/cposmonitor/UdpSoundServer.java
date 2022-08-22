package com.cpos.cposmonitor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cpos.cposmonitor.data.model.SoundServerExchange;

import static androidx.core.content.ContextCompat.checkSelfPermission;


public class UdpSoundServer extends Thread {
    static byte CPOS_TEL_FUN_FREE = 0;//空闲
    static byte CPOS_TEL_FUN_CALL = 1;//呼叫
    static byte CPOS_TEL_FUN_HANG_UP = 2;//挂断
    static byte CPOS_TEL_FUN_ANSWER = 3;//接听
    static byte CPOS_TEL_FUN_DATA = 4;//声音BIN数据
    static byte CPOS_TEL_FUN_BUSY = 5;//忙线指令
    static byte CPOS_TEL_FUN_CONNECT = 6;//正在接通中
    static byte CPOS_TEL_FUN_INCOMING = 7;//有电话进来

    static byte UDP_DATA_HEAD = 'A';
    static byte UDP_DATATYPE_TEXT = 0x01;//文本数据
    static byte UDP_DATA_TYPE_BIN = 0x02;//二进制数据
    static byte UDP_DATA_TYPE_JSON = 0x03;//json数据
    static byte UDP_DATA_TYPE_CTRL = 0x04;//控制数据
    static byte UDP_DATA_TYPE_EXCHANGE = 0x05;//交换数据


    private AudioTrack audioTrack = null;
    private static int sampleRateInHz = 8000;
    private static int channelOutConfig = AudioFormat.CHANNEL_OUT_MONO;
    private static int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
    private int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelOutConfig, audioFormat);


    private static int channelInConfig = AudioFormat.CHANNEL_IN_MONO;
    AudioRecord audioRecord = null;
    int audioSize = 0;
    private boolean isRecording = false;
    byte[] mSoundRecordBuf = new byte[600];

    MainActivity mainActivity = null;

        public byte mTelState;

        public MyHandler mHandler;
        UdpNioSocket  mUdpServer;
        int mUdpPort = 0;
        private boolean mLift = true;

    //记录要通话的客户端的ip和端口
    //因为使用UDP服务器中转方式通讯，所以此处记录到的应该与语音服务器相同
    //如果使用P2P方式通讯，此处记录的应该是客户端的IP和端口
    String mClientIp = "";
    int mClientPort = 0;
    String mClientCarParkId = "";
    int mClientDeviceId = 0;
    int mClientDeviceType = 0;

    //本机编号
    String mMyCarParkId = "";
    int mMyDeviceId = 0;
    int mMyDeviceType = 0;
    long timeLastSoundSend = 0;//本机发送音频的时间，如果通话时，拔出麦克风，本机会停止发送，对方会因为未接收到数据而中止通话
    long timeLastSoundRecv;
    long timeNow;

    SoundServerExchange dataSound;

        public UdpSoundServer(Context context, int udpPort) {
            mainActivity = (MainActivity)context;

            bufferSize = 600;
            mHandler = new MyHandler();

           this.mUdpPort = udpPort;
            this.mUdpServer = new UdpNioSocket( this.mUdpPort, mHandler);
            this.mUdpServer.start();

        }
        public MyHandler getHandler() {
            return mHandler;
        }
        public boolean isLife() {
            return mLift;
        }

        public void setLife(boolean life) {
            this.mLift = life;
        }


    private void startRecord() {

        if (audioTrack == null) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelOutConfig, audioFormat, bufferSize,
                    AudioTrack.MODE_STREAM);
            if (audioTrack == null) {

                return;
            }

            try {
                if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.play();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            audioSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                    AudioFormat.CHANNEL_IN_MONO,
                    audioFormat);

            audioSize = 600;//REMOTE_SUBMIX
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRateInHz,
                    channelInConfig,
                    audioFormat,
                    audioSize);
            if (audioRecord == null) {

                return;
            }
            else {

                audioRecord.startRecording();

            }

            isRecording = true;
        }
    }

    private void stopRecord() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            Log.i("audioRecordTest", "停止录音");
            audioRecord.release();
            audioRecord = null;

        }

        if (audioTrack != null) {
            if (audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
        }

        mClientCarParkId = "";
        mClientDeviceId = 0;
        mClientDeviceType = 0;
        mClientIp = "";
        mClientPort = 0;
    }

        @Override
        public void run() {


            byte[] outBuf = null;


            while (mLift) {


                outBuf = mHandler.dataGet();
                if (outBuf[0] != 0) {
                    SoundServerExchange data = new SoundServerExchange(outBuf);
                    mHandler.dataClear();

                    //收到UDP发来的语音数据, 直接解码
                    if (data.fun_type == CPOS_TEL_FUN_DATA) {//声音数据

                        if (isRecording && audioTrack != null) {
                            audioTrack.write(data.data, 0, data.data.length);
                            timeLastSoundRecv = Calendar.getInstance().getTimeInMillis() / 1000;
                        }

                    } else if (data.fun_type == CPOS_TEL_FUN_CALL)//收到呼叫请求
                    {
                        timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;

                        if (getTelState() == CPOS_TEL_FUN_BUSY)    //正在通话, 又有呼入
                        {
                            //	pThis->g_socket_server.Cleanup(p_ctrl_data->client_id);
                        } else {
                            //响铃
                            //  CString str = GetAppPath() + _T("sound\\ringin.wav");
                            // PlaySound(str, NULL, SND_ASYNC | SND_NODEFAULT);
                            mClientCarParkId = data.from.str_car_park_id;
                            mClientDeviceId = data.from.device_id;
                            mClientDeviceType = data.from.device_type;
                            mClientIp = data.from.ip;
                            mClientPort = data.from.port;

                            mMyCarParkId = data.to.str_car_park_id;
                            mMyDeviceId = data.to.device_id;
                            mMyDeviceType = data.to.device_type;

                           
                            setTelState(CPOS_TEL_FUN_INCOMING);
                            // pThis->SendWndState(CPOS_TEL_FUN_INCOMING, &pSoudData->from); //向主界面发送正在呼叫的消息

                            //if (pThis->m_isAutoAnswer)
                            //{
                            //	pThis->AnswerUdpServer(udp_ip, udp_port, CString(pSoudData->to.str_car_park_id) , pSoudData->to.device_id, pSoudData->to.device_type, CString(pSoudData->from.str_car_park_id), pSoudData->from.device_id);
                            //}


                        }

                    } else if (getTelState() != CPOS_TEL_FUN_BUSY && data.fun_type == CPOS_TEL_FUN_CONNECT)//主叫时, 被叫发来的已接听消息, 本机开始播放声音
                    {
                        //计时初始化
                        timeLastSoundRecv = timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
                        startRecord();
                        //收到远程主机的应答后, 保存远程主机的IP
                        mClientCarParkId = data.from.str_car_park_id;
                        mClientDeviceId = data.from.device_id;
                        mClientDeviceType = data.from.device_type;
                        mClientIp = data.from.ip;
                        mClientPort = data.from.port;

                    } else if (getTelState() != CPOS_TEL_FUN_BUSY && data.fun_type == CPOS_TEL_FUN_ANSWER)//主叫时, 被叫发来的已接听消息, 本机开始播放声音
                    {
                        //计时初始化
                        timeLastSoundRecv = timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
                        startRecord();

                        mClientCarParkId = data.from.str_car_park_id;
                        mClientDeviceId = data.from.device_id;
                        mClientDeviceType = data.from.device_type;
                        mClientIp = data.from.ip;
                        mClientPort = data.from.port;


                        if (isRecording)//启动成功
                        {
                            setTelState(CPOS_TEL_FUN_BUSY);

                        }
                        else//启动录音失败
                        {
                            mClientCarParkId = "";
                            mClientDeviceId = 0;
                            mClientDeviceType = 0;
                            mClientIp = "";
                            mClientPort = 0;

                            setTelState(CPOS_TEL_FUN_FREE);
                        }
                    } else if (data.fun_type == CPOS_TEL_FUN_HANG_UP)//挂断
                    {

                        mainActivity.CallFree();

                        //收到对方发来的挂机指令，停止录音，结束通话
                        if (mClientCarParkId == data.from.str_car_park_id && mClientDeviceId == data.from.device_id) {

                            mLift = false;

                            mClientCarParkId = "";
                            mClientDeviceId = 0;
                            mClientDeviceType = 0;
                            mClientIp = "";
                            mClientPort = 0;

                            setTelState(CPOS_TEL_FUN_FREE);
                        }

                    }

                }

                if (getTelState() == CPOS_TEL_FUN_BUSY)    //正在通话
                {
                    //pThis->m_udpServer.UdpEncodeSend(pThis->m_lClientIp, pThis->m_uClientPort, (char *)lpHdr->lpData, lpHdr->dwBytesRecorded);
                    if (isRecording && audioRecord != null) {
                        int readResult = audioRecord.read(mSoundRecordBuf, 0, audioSize);

                        if (AudioRecord.ERROR_INVALID_OPERATION != readResult) {

                            //发送本机语音
                            SoundDataUdpServer(mClientIp, mClientPort, mMyCarParkId, mMyDeviceId, mMyDeviceType,
                                    mClientCarParkId, mClientDeviceId,
                                    mSoundRecordBuf);

                            timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
                        }
                    }
                }

                if (getTelState() == CPOS_TEL_FUN_FREE) {
                    //停止通话
                    if (isRecording) {
                        stopRecord();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    //超过5秒无数据，自动结束通话
                    timeNow = Calendar.getInstance().getTimeInMillis() / 1000;
                    long isend = timeNow - timeLastSoundSend;
                    long irecv = timeNow - timeLastSoundRecv;
                    if (irecv > 5 || isend > 3) {
                        if (isRecording) {
                            stopRecord();
                        }
                        setTelState(CPOS_TEL_FUN_FREE);

                    }
                }


            }//life
        }

        //发送指令
    //呼叫开始时,开启录音, 因为有可能未接到对方已接听的指令
        public void CallUdpServer(String strServerIp, int iServerPort, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice, int iCallDeviceType) {

            mMyCarParkId = strCarparkId;
            mMyDeviceId = iDeviceId;
            mMyDeviceType = iDeviceType;

            byte[] sendbuf = soundDataCreate(CPOS_TEL_FUN_CALL, strCarparkId, iDeviceId, iDeviceType, strCallCarPark, iCallDevice, iCallDeviceType, null);

            try {
                this.mUdpServer.UdpEncodeSend(strServerIp, iServerPort, sendbuf);
                startRecord();


              //  mTelState = CPOS_TEL_FUN_CALL;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }

    //发送指令
    public void HangupUdpServer(String strServerIp, int iServerPort, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice, int iCallDeviceType) {


        mMyCarParkId = strCarparkId;
        mMyDeviceId = iDeviceId;
        mMyDeviceType = iDeviceType;

        byte[] sendbuf = soundDataCreate(CPOS_TEL_FUN_HANG_UP, strCarparkId, iDeviceId, iDeviceType, strCallCarPark, iCallDevice, iCallDeviceType, null);

        try {
            this.mUdpServer.UdpEncodeSend(strServerIp, iServerPort, sendbuf);
            setTelState(CPOS_TEL_FUN_FREE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
    public int AnswerUdpServer(String strServerIp, int iServerPort, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice) {


        byte[] sendbuf = soundDataCreate(CPOS_TEL_FUN_ANSWER, strCarparkId, iDeviceId, iDeviceType, strCallCarPark, iCallDevice, 0, null);

        try {
            mClientIp = strServerIp;
            mClientPort = iServerPort;

            this.mUdpServer.UdpEncodeSend(strServerIp, iServerPort, sendbuf);

            mClientCarParkId = strCallCarPark;
            mClientDeviceId = iCallDevice;
            timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
            timeLastSoundRecv = timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
            setTelState(CPOS_TEL_FUN_BUSY);
            startRecord();

        } catch (IOException e) {
            e.printStackTrace();

        }

        return 1;
    }

    //回应服务器的MQTT呼叫, 以便服务器拿到本机的 IP和UDP端口
    public int AnswerBackUdpServer(String strServerIp, int iServerPort, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice) {


        byte[] sendbuf = soundDataCreate(CPOS_TEL_FUN_CONNECT, strCarparkId, iDeviceId, iDeviceType, strCallCarPark, iCallDevice, 0, null);

        try {
            mClientIp = strServerIp;
            mClientPort = iServerPort;

            this.mUdpServer.UdpEncodeSend(strServerIp, iServerPort, sendbuf);

            timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
            timeLastSoundRecv = timeLastSoundSend = Calendar.getInstance().getTimeInMillis() / 1000;
            setTelState(CPOS_TEL_FUN_INCOMING);

        } catch (IOException e) {
            e.printStackTrace();

        }

        return 1;
    }

//发送语音数据

    public int SoundDataUdpServer(String strServerIp, int iServerPort, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice, byte[] bufData) {

        byte[] sendbuf = soundDataCreate(CPOS_TEL_FUN_DATA, strCarparkId, iDeviceId, iDeviceType, strCallCarPark, iCallDevice, 0, bufData);

        try {
            mClientIp = strServerIp;
            mClientPort = iServerPort;

            this.mUdpServer.UdpEncodeSend(strServerIp, iServerPort, sendbuf);

        } catch (IOException e) {
            e.printStackTrace();

        }

        return 1;
    }

        public void setTelState(byte state)
        {
            if(state == CPOS_TEL_FUN_FREE)
            {
                mainActivity.CallFree();
            }
            mTelState = state;
        }

        public byte getTelState()
        {
            return mTelState;
        }

        static class MyHandler extends Handler {
            private static Charset mCharset = Charset.forName("utf8");
            // WeakReference<MainActivity> mActivity;
            byte[] mSoundBuf = new byte[685];
            public MyHandler() {
                //   mActivity = new WeakReference<MainActivity>(activity);
            }
            @Override
            public void handleMessage(Message message)
            {
                Bundle bundle = message.getData();
                byte[] udpData = bundle.getByteArray("udp_receive");

                   //  String msg = mCharset.decode(ByteBuffer.wrap(udpData)).toString();
                   //  MainActivity.msgShow(msg);
                System.arraycopy(udpData, 0, mSoundBuf, 0, udpData.length);
            }

            public  byte[]dataGet()
            {
               return mSoundBuf;
            }
            public  void dataClear()
            {
                 mSoundBuf[0] = 0;
            }
        }


    private byte[] soundDataCreate(int msg_type, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice, int iCallDeviceType, byte[] bufData)
    {

        int from_ip = 0;
        int from_port = 0;
        byte from_str_car_park_id[] = strCarparkId.getBytes();
        int from_device_id = iDeviceId;
        int from_device_type = iDeviceType;

        int to_ip = 0;
        int to_port = 0;
        byte[] to_str_car_park_id = strCallCarPark.getBytes();

        int to_device_id = iCallDevice;
        int to_device_type = iCallDeviceType;

        byte[] sound_data = null;

        int cmd_function = msg_type;//呼叫

        int cmd_len = 685;//整体数据的长度:

        int i = 0;
        int len = 0;
        int checksum = 0;

        byte send_buf[] = new byte[cmd_len];	//创建数据缓冲区


        if(bufData != null && bufData.length == 600)
        {
            sound_data = bufData;
        }
        else
        {
            sound_data = new byte[600];//声音数据
        }

        send_buf[len++] = 'A'; //1 BYTE 数据头
        send_buf[len++] = UDP_DATA_TYPE_EXCHANGE;	//1 BYTE 数据类型, 5表示无压缩的原始数据
        send_buf[len++] = (byte) (cmd_len & 0x000000ff); //2BYTE cmd_len
        send_buf[len++] = (byte) ((cmd_len >> 8) & 0x000000ff);  // cmd_len

        //struct data
        //以下开始结构赋值, 要注意4字节对齐

        /////////////////////////////////////
        //from

        //ip - 4 BYTES
        send_buf[len++] = (byte) (from_ip & 0x000000ff);
        send_buf[len++] = (byte) ((from_ip >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((from_ip >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((from_ip >> 24) & 0x000000ff);

        //port 2 BYTES
        send_buf[len++] = (byte) (from_port & 0x000000ff);
        send_buf[len++] = (byte) ((from_port >> 8) & 0x000000ff);

        //21个字符的
        for (i = 0; i < 21; i++)
        {

            if(i < strCarparkId.length()) {
                send_buf[len] = from_str_car_park_id[i];    //
            }
            len++;
        }

        //特别注意:
        //因为32位系统的struct需要4BYTES对齐,所以此处要做补齐
        len++;	//补齐到4字节

        //device_id 4 BYTES
        send_buf[len++] = (byte) (from_device_id & 0x000000ff);
        send_buf[len++] = (byte) ((from_device_id >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((from_device_id >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((from_device_id >> 24) & 0x000000ff);

        //device_type 4 BYTES
        send_buf[len++] = (byte) (from_device_type & 0x000000ff);
        send_buf[len++] = (byte) ((from_device_type >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((from_device_type >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((from_device_type >> 24) & 0x000000ff);

        /////////////////////////////////////
        //to

        send_buf[len++] = (byte) (to_ip & 0x000000ff);
        send_buf[len++] = (byte) ((to_ip >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((to_ip >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((to_ip >> 24) & 0x000000ff);

        //port 2 BYTES
        send_buf[len++] = (byte) (to_port & 0x000000ff);
        send_buf[len++] = (byte) ((to_port >> 8) & 0x000000ff);

        for (i = 0; i < 21; i++)
        {
            if(i < strCallCarPark.length()) {
                send_buf[len] = to_str_car_park_id[i];    //
            }
            len++;
        }

        len++;	//补齐到4字节

        //device_id 4 BYTES
        send_buf[len++] = (byte) (to_device_id & 0x000000ff);
        send_buf[len++] = (byte) ((to_device_id >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((to_device_id >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((to_device_id >> 24) & 0x000000ff);

        //device_type 4 BYTES
        send_buf[len++] = (byte) (to_device_type & 0x000000ff);
        send_buf[len++] = (byte) ((to_device_type >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((to_device_type >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((to_device_type >> 24) & 0x000000ff);

        /////////////////////////////////////////////////
        //data
        for (i = 0; i < 600; i++)
        {
            send_buf[len++] = sound_data[i];	//

        }


        //function
        send_buf[len++] = (byte) (cmd_function & 0x000000ff);
        send_buf[len++] = (byte) ((cmd_function >> 8) & 0x000000ff);
        send_buf[len++] = (byte) ((cmd_function >> 16) & 0x000000ff);
        send_buf[len++] = (byte) ((cmd_function >> 24) & 0x000000ff);

        //计算struct 的checksum

        //checksum计算不包括自己
        for(i=4; i<len; i++)
        {
            checksum += send_buf[i];
        }

        checksum = checksum % 256;
        checksum = (256 - checksum) & 0x000000ff;

        send_buf[len++] = (byte) (checksum);
        //补齐4字节
        len++;
        len++;
        len++;

        //到此struct 赋值完成
        //struct数据部分长度为680 BYTES
        /////////////////////////////////

        //整组数据的checksum
        checksum = 0;
        for(i=0; i<len; i++)
        {
            checksum += send_buf[i];
        }

        checksum = checksum % 256;
        checksum = (256 - checksum) & 0x000000ff;

        send_buf[len++] = (byte) (checksum);

        //到此结束, len应该==685表示整个数据长度正确

        return send_buf;

    }
    public void udpTest(String ip, int port, byte[] sendbuf) {


        try {
            this.mUdpServer.UdpEncodeSend(ip, port, sendbuf);
        }
        catch (IOException e)
        {
            e.printStackTrace();

        }

    }

    }