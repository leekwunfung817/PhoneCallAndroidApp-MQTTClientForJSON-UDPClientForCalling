package com.cpos.cposmonitor;
import android.os.Bundle;
import android.os.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class UdpNioSocket extends Thread {

    private static Charset mCharset = Charset.forName("utf8");
    static DatagramChannel mChannelUdpSocket;//UDP通道
    private int mSocketPort = 0;//本地UDP端口
    private String mIpSendTo ="";//要发送到的主机ip
    private int mPortSendTo = 0;//要发送到的主机ip

    boolean mSendComplete = true;//如果数据太长或者网络不好时,可能一次发不完,要分段发送

    static Selector mSelector = null;//多路复用选择器

    ByteBuffer mSendBuffer = null;
    ByteBuffer mReadBuffer = null;
    private final static int RECV_BUF_LEN = 1024;
    private static final int SEND_BUFF_LEN = 1024;

    SelectionKey mKeyUdpChannel = null;

    public UdpSoundServer.MyHandler mHandler;

    public UdpNioSocket(int port, UdpSoundServer.MyHandler handler) {

        this.mHandler = handler;

        mSocketPort = port;
        this.mReadBuffer = ByteBuffer.allocate(RECV_BUF_LEN);
        this.mSendBuffer = ByteBuffer.allocate(SEND_BUFF_LEN);
    }
    private void register(int op){
        try {
            if (mKeyUdpChannel == null){
                mKeyUdpChannel = this.mChannelUdpSocket.register(this.mSelector, op, this);
            } else {
                mKeyUdpChannel.interestOps(op | mKeyUdpChannel.interestOps());
            }
            this.mSelector.wakeup();//立即唤醒一次(注册写事件后,立即发送)
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    public void cancelEvent(int ops){
        if (mKeyUdpChannel == null)
            return;

        mKeyUdpChannel.interestOps(mKeyUdpChannel.interestOps() & (~ops));
    }


    @Override
    public void run() {

        Iterator iterator;

        try {

            mChannelUdpSocket = DatagramChannel.open();//打开UDP通道

            mChannelUdpSocket.configureBlocking(false);//通道必须设置为非阻塞才能使用选择器

            mChannelUdpSocket.socket().bind(new InetSocketAddress(this.mSocketPort));//本机的UDP端口

            if(mSelector == null) mSelector = Selector.open();//打开选择器

            //初始化选择器,把selector读事件注册到UDP通道中
            this.register(SelectionKey.OP_READ);

            while (true) {

                int n = mSelector.select(1000l);//等待选择器事件发生, 设置超时

                if (n == 0) //超时发生
                {
                     continue;
                }

                iterator = mSelector.selectedKeys().iterator();//读取选择器,检测有几个事件发生

                while (iterator.hasNext()) {

                    SelectionKey key = (SelectionKey) iterator.next();//取出要处理的事件

                    iterator.remove();

                    if (key.isValid() && key.isReadable() && (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                        handleRead(key);

                    } else if (key.isValid() && key.isWritable() && (key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {

                        this.cancelEvent(SelectionKey.OP_WRITE);//关闭写事件,防止再次发生
                        handleWrite(key);
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            try {

                mChannelUdpSocket.close();
                mSelector.close();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }

    }

    private void handleRead(SelectionKey key) throws IOException {

        mChannelUdpSocket = (DatagramChannel) key.channel();

        mReadBuffer.clear();
        InetSocketAddress address = (InetSocketAddress) mChannelUdpSocket.receive(mReadBuffer);

        mReadBuffer.flip();

        byte[] remaining = new byte[mReadBuffer.remaining()];//接收到的数据缓冲
        mReadBuffer.get(remaining);//提取接收到的数据

        //整组数据的checksum
        int checksum = 0;
        for (int i = 0; i < remaining.length - 1; i++) {
            checksum += remaining[i];
        }

        checksum = checksum % 256;
        checksum = (256 - checksum) & 0x000000ff;
        int checksum_recv = remaining[remaining.length - 1];
        if (checksum_recv < 0) checksum_recv = 256 + checksum_recv;

        if (checksum == checksum_recv) {

            Message obtainMessage = mHandler.obtainMessage();
            Bundle bundle = new Bundle();

            //from ip:4
            //to ip:40
            byte[] bip;
            String strIp;
            try {


                bip = address.getAddress().getAddress();

                int signedByte = 0;
                int unsignedByte = 0;
                signedByte = bip[0];
                unsignedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                remaining[4] = (byte) unsignedByte;
                signedByte = bip[1];
                unsignedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                remaining[5] = (byte) unsignedByte;
                signedByte = bip[2];
                unsignedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                remaining[6] = (byte) unsignedByte;
                signedByte = bip[3];
                unsignedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                remaining[7] = (byte) unsignedByte;

                signedByte = address.getPort();
                unsignedByte = (byte) (signedByte & 0x000000ff);
                remaining[8] = (byte) unsignedByte;

                signedByte = (byte) ((signedByte >> 8) & 0x000000ff);
                unsignedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                remaining[9] = (byte) unsignedByte;

            } catch (Exception e) {
                strIp = "";
            }

            //    int unsignedByte = signedByte<0? signedByte + 256:signedByte;


            bundle.putByteArray("udp_receive", remaining);//加载数据到bundle
            obtainMessage.setData(bundle);//把bundle数据加载到消息中
            mHandler.sendMessage(obtainMessage);//发送数据
        }

        //  String msg = mCharset.decode(mReadBuffer).toString();
        //   MainActivity.msgShow(msg);

        mReadBuffer.clear();

    }

    private void handleWrite(SelectionKey key) throws IOException {

        mChannelUdpSocket = (DatagramChannel) key.channel();//没有必要,因为客户端只有一个socketchannel

        try {
            int totalSendBytes = 0;//需要发送的数据长度
            String resp = null;

            totalSendBytes = mSendBuffer.remaining();

            int sbytes = this.mChannelUdpSocket.send(mSendBuffer, new InetSocketAddress(this.mIpSendTo,
                    this.mPortSendTo));

            if (sbytes < totalSendBytes)    //实际发送数据较小,发送未完成, 需要继续发送
            {
                this.register(SelectionKey.OP_WRITE);
                mSendComplete = false;
            } else {//发送完成
                if (!mSendComplete) {
                    mSendComplete = true;//已完成, 可以进行下一个发送
                }
                mSendBuffer.rewind();//重置缓存
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int UdpEncodeSend(String ip, int port, byte[] send_buf) throws IOException {
        int res = 0;

        if (this.mSendComplete) {

            this.mIpSendTo = ip;
            this.mPortSendTo = port;

            mSendBuffer = ByteBuffer.wrap(send_buf);

            this.register(SelectionKey.OP_WRITE);

            res = 1;
        }
        return res;
    }

}