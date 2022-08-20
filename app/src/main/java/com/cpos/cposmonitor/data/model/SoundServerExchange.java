package com.cpos.cposmonitor.data.model;


import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class SoundServerExchange {

        public class SoundInfo
        {
            public String ip;
            public int port;
            public String str_car_park_id;
            public int device_id;
            public int device_type;

            public SoundInfo()
            {
                str_car_park_id = "";
            }
        }

        public SoundInfo from;
        public SoundInfo to;
        public byte[] data = null;
        public int fun_type = 0;
        public char checksum;
        private static Charset mCharset = Charset.forName("utf8");

        public SoundServerExchange(byte[] exchangeData) {
            data = new byte[600];
            from = new SoundInfo();
            to = new SoundInfo();

            soundDataInit(exchangeData);



        }

        public int soundDataInit(byte[] data_buf)
        {
            int res = 0;

            byte[] CarParkId = new byte[21];

            long ip = 0;

            if(data_buf[0] == 'A' && data_buf.length == 685) {

                int cmd_len = 685;//整体数据的长度:

                int i = 0;
                int len = 0;

                len++;// = 'A'; //1 BYTE 数据头
                len++;// = 5;	//1 BYTE 数据类型, 5表示无压缩的原始数据

                cmd_len = data_buf[len++];//= (byte) (cmd_len & 0x000000ff); //2BYTE cmd_len
                cmd_len |= (((int) (data_buf[len++])) << 8);//  (byte) ((cmd_len >> 8) & 0x000000ff);  // cmd_len

                //struct data
                //以下开始结构赋值, 要注意4字节对齐

                /////////////////////////////////////
                //from

                //ip - 4 BYTES
                int signedByte = 0;
                int unsignedByte = 0;
                ip = 0;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte; from.ip = Integer.toString(signedByte); ip |= signedByte; ip <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;ip <<= 8;from.ip += "." + Integer.toString(signedByte); ip |= signedByte; ip <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;ip <<= 8;from.ip += "." + Integer.toString(signedByte); ip |= signedByte; ip <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;from.ip += "." + Integer.toString(signedByte); ip |= signedByte; ip <<= 8;


                //port 2 BYTES
                signedByte = data_buf[len++];
                from.port = signedByte < 0 ? signedByte + 256 : signedByte;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                signedByte <<= 8;
                from.port |= signedByte;

                //21个字符的
                for (i = 0; i < 21; i++) {
                    CarParkId[i] = data_buf[len++];    //
                }

                from.str_car_park_id = mCharset.decode(ByteBuffer.wrap(CarParkId)).toString();

                //特别注意:
                //因为32位系统的struct需要4BYTES对齐,所以此处要做补齐
                len++;    //补齐到4字节

                //device_id 4 BYTES
                from.device_id = data_buf[len++];
                from.device_id |= ((int) (data_buf[len++])) << 8;
                from.device_id |= ((int) (data_buf[len++])) << 16;
                from.device_id |= ((int) (data_buf[len++])) << 24;


                //device_type 4 BYTES
                from.device_type = data_buf[len++];
                from.device_type |= ((int) (data_buf[len++])) << 8;
                from.device_type |= ((int) (data_buf[len++])) << 16;
                from.device_type |= ((int) (data_buf[len++])) << 24;

                /////////////////////////////////////
                //to

                ip = 0;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte; to.ip = Integer.toString(signedByte); ip |= signedByte; ip <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;ip <<= 8;to.ip += "." + Integer.toString(signedByte); ip |= signedByte; ip <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;ip <<= 8;to.ip += "." + Integer.toString(signedByte); ip |= signedByte; ip <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;to.ip += "." + Integer.toString(signedByte); ip |= signedByte; ip <<= 8;



                //port 2 BYTES
                signedByte = data_buf[len++];
                to.port = signedByte < 0 ? signedByte + 256 : signedByte; to.port <<= 8;
                signedByte = data_buf[len++];
                signedByte = signedByte < 0 ? signedByte + 256 : signedByte;
                signedByte <<= 8;
                from.port |= signedByte;



                //21个字符的
                for (i = 0; i < 21; i++) {
                    CarParkId[i] = data_buf[len++];    //
                }
                to.str_car_park_id = mCharset.decode(ByteBuffer.wrap(CarParkId)).toString();
                //特别注意:
                //因为32位系统的struct需要4BYTES对齐,所以此处要做补齐
                len++;    //补齐到4字节

                //device_id 4 BYTES
                to.device_id = data_buf[len++];
                to.device_id |= ((int) (data_buf[len++])) << 8;
                to.device_id |= ((int) (data_buf[len++])) << 16;
                to.device_id |= ((int) (data_buf[len++])) << 24;


                //device_type 4 BYTES
                to.device_type = data_buf[len++];
                to.device_type |= ((int) (data_buf[len++])) << 8;
                to.device_type |= ((int) (data_buf[len++])) << 16;
                to.device_type |= ((int) (data_buf[len++])) << 24;


                /////////////////////////////////////////////////
                //data
                for (i = 0; i < 600; i++) {
                    data[i] = data_buf[len++];    //

                }


                //function

                fun_type = data_buf[len++];
                fun_type |= ((int) (data_buf[len++])) << 8;
                fun_type |= ((int) (data_buf[len++])) << 16;
                fun_type |= ((int) (data_buf[len++])) << 24;

                res = 1;
            }


            return res;
        }

        static public byte[] soundDataCreate(int msg_type, String strCarparkId, int iDeviceId, int iDeviceType, String strCallCarPark, int iCallDevice, int iCallDeviceType)
        {

            int from_ip = 0;
            int from_port = 0;
            byte from_str_car_park_id[] = strCarparkId.getBytes();
            int from_device_id = iDeviceId;
            int from_device_type = iDeviceType;

            long to_ip = 0;
            int to_port = 0;
            byte[] to_str_car_park_id = strCallCarPark.getBytes();

            int to_device_id = iCallDevice;
            int to_device_type = iCallDeviceType;

            byte[] sound_data = new byte[600];//声音数据

            int cmd_function = msg_type;//呼叫

            int cmd_len = 685;//整体数据的长度:

            int i = 0;
            int len = 0;
            int checksum = 0;

            byte send_buf[] = new byte[cmd_len];	//创建数据缓冲区


            send_buf[len++] = 'A'; //1 BYTE 数据头
            send_buf[len++] = 5;	//1 BYTE 数据类型, 5表示无压缩的原始数据
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

    }