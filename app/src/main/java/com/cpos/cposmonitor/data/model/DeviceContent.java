package com.cpos.cposmonitor.data.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceContent {
    //所有设备数据的数组
    public static final List<DeviceItem> ITEMS = new ArrayList<DeviceItem>();
    //用id标识的数据集合，便于用id查找
    public static final Map<String, DeviceItem> ITEM_MAP = new HashMap<String, DeviceItem>();
    public static List<DeviceItem> DeviceJSONArrayToList(JSONArray jsonArray) {
        try {
            ITEMS.clear();
            String msg_type = "";
            String username;
            String car_park_id;
            String device_id;
            String device_remark;
            String device_type;
            String time;
            String version;
            String video_id;
            String video_password;

         //   HTREEITEM hItem;

            int input;
            int output;
            String alive_time;//最后通讯时间
            char bOnlineRealtime;//实时是否在线
            char bOnlineShow;//显示的是否在线
            double call_time;
            long udp_ip;
            short udp_port;

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                msg_type = jsonObject.getString("msg_type");
                if(msg_type.equals("17"))
                {
                    username = jsonObject.getString("username");
                    car_park_id = jsonObject.getString("car_park_id");
                    device_id = jsonObject.getString("device_id");
                    device_remark = jsonObject.getString("device_remark");
                    device_type = jsonObject.getString("device_type");
                    time = jsonObject.getString("time");
                    version = jsonObject.getString("version");
                    video_id = jsonObject.getString("video_id");
                    video_password = jsonObject.getString("video_password");
                    addItem(new DeviceItem(String.valueOf(i+1), username, car_park_id, device_id, device_remark, device_type,
                            time, version, video_id, video_password));

                }
            }
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        return ITEMS;
    }

    private static void addItem(DeviceItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }


    //生成详情数据，用于详情显示
    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class DeviceItem {
        public final String id;
        public final String username;
        public final String car_park_id;
        public final String device_id;
        public String device_remark;
        public final String device_type;
        public final String time;
        public final String version;
        public final String video_id;
        public final String video_password;
        public final int input;
        public final int output;
        public final String alive_time;//最后通讯时间
        public final char bOnlineRealtime;//实时是否在线
        public final char bOnlineShow;//显示的是否在线
        public final double call_time;
        public final long udp_ip;
        public final short udp_port;

        public DeviceItem(String id, String username, String car_park_id, String device_id, String device_remark, String device_type,
                          String time, String version, String video_id, String video_password) {
            this.id = id;
            this.username = username;
            this.car_park_id = car_park_id;
            this.device_id = device_id;
            this.device_remark = device_remark;
            this.device_type = device_type;
            this.time = time;
            this.version = version;
            this.video_id = video_id;
            this.video_password = video_password;

            this.input = 0;
            this.output = 0;
            this.alive_time = "";//最后通讯时间
            this.bOnlineRealtime = 0;//实时是否在线
            this.bOnlineShow = 0;//显示的是否在线
            this.call_time = 0;
            this.udp_ip = 0;
            this.udp_port = 0;
        }

        @Override
        public String toString() {
            return device_remark;
        }
    }
}