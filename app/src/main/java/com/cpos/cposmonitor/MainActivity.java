package com.cpos.cposmonitor;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;

import com.cpos.cposmonitor.data.model.DeviceContent;

import com.cpos.cposmonitor.ui.login.LoginActivity;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import com.cpos.cposmonitor.MyApplication.SettingKey;

public class MainActivity extends AppCompatActivity implements MQTTService.IGetMessageCallBack {

    private com.cpos.cposmonitor.MyServiceConnection serviceConnection;
    static View mToolBarView;
    private SimpleItemRecyclerViewAdapter mAdapter = null;
    private RecyclerView mRecyclerView = null;
    private boolean mTwoPane;
    public UdpSoundServer  soundserver;
    static MainActivity mContext;
    public boolean mDetailShow = false;
    private static Charset charset = Charset.forName("utf8");

    static public MainActivity getInstance()
    {
        return mContext;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermission(String strPermission)
    {
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = 0;
            for (i = 0; i < permissions.length; i++) {
                int p = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]);
                //GRANTED=已授权  DINIED=已拒绝
                if (p != PackageManager.PERMISSION_GRANTED) {
                    //只要有一个没有授权, 就弹出授权提示
                    ActivityCompat.requestPermissions(this, permissions, 1000);//requestCode返回给回调的参数
                    break;
                }
            }
        }
    }

    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    //授权回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && hasAllPermissionsGranted(grantResults)) {
            // 获得用户授权
        } else {
            // 如果用户没有授权，那么应该说明意图，引导用户去设置里面授权。
//            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        theApp = (MyApplication)getApplication();
        mContext = this;
        mRecyclerView = (RecyclerView)findViewById(R.id.item_list);

        checkAndRequestPermission(Manifest.permission.RECORD_AUDIO);

        soundserver = new UdpSoundServer(this,10000);
        soundserver.start();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mToolBarView = toolbar;


        //大屏模式, 在屏幕上显示左右两半,右边直接显示详情
        if (findViewById(R.id.item_detail_container) != null) {
            mTwoPane = true;
        }
        mTwoPane = false;//禁止大屏功能

        serviceConnection = new MyServiceConnection();
        serviceConnection.setIGetMessageCallBack( MainActivity.this);
        Intent intent = new Intent(this, MQTTService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_action_settings) {

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.menu_action_login) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


//    @Override
//    public void OnMQTTMessageReturnError(String message)
//    {
//        // textView.setText(message);
//        //  mqttService = serviceConnection.getMqttService();
//        //  mqttService.toCreateNotification(message);
//        Snackbar.make(mToolBarView,
//                message, Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show();
//    }

    public static void msgShow(String strMsg)
    {
        Log.i("MainActivity","Show message:("+strMsg+")");
        Snackbar.make(mToolBarView,
                strMsg, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }


    @Override
    public void OnReceiveDeviceList(JSONArray device_list) {
        Log.i("MainActivity", "========== Process Device list Begin ==========");
        if (mAdapter == null ) {
            List<DeviceContent.DeviceItem> deviceItemList = DeviceContent.DeviceJSONArrayToList(device_list);
            Log.i("MainActivity",deviceItemList.toString());
            mAdapter = new SimpleItemRecyclerViewAdapter(this, deviceItemList, mTwoPane);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged(); //刷新
        }

        try {
            for (int i = 0; i < device_list.length(); i++) {
                JSONObject jsonObject = device_list.getJSONObject(i);
                Log.i("MainActivity", "List Item "+i+":"+jsonObject);
                String msg_type = jsonObject.getString("msg_type");
                if (msg_type.equals("17")) {
                    String username = jsonObject.getString("username");
                    String car_park_id = jsonObject.getString("car_park_id");
                    String device_id = jsonObject.getString("device_id");
                    String strTopic = "cpos/carpark/" + car_park_id + "/" + device_id + "/send";
                    MQTTService.subscribe(strTopic, 0);

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("MainActivity", "========== Process Device list End ==========");
    }
    @Override
    public void OnReceiveCalling(JSONArray device_in)
    {
        try {
            String msg_type = "";
            String car_park_id;
            String device_id;
            String strTopic;
            for (int i = 0; i < device_in.length(); i++) {
                JSONObject jsonObject = device_in.getJSONObject(i);
                msg_type = jsonObject.getString("msg_type");
                if (msg_type.equals("18")) {
                    car_park_id = jsonObject.getString("car_park_id");
                    device_id = jsonObject.getString("device_id");

                    int state = soundserver.getTelState();
                    if(state == UdpSoundServer.CPOS_TEL_FUN_FREE
                    || state == UdpSoundServer.CPOS_TEL_FUN_INCOMING
                    ) {

                        //回应服务器, 以便服务器拿到本机的IP和端口
                        soundserver.AnswerBackUdpServer(MyApplication.get(MyApplication.SettingKey.SETTINGS_SERVER_IP, MyApplication.DEFAULT_IP)
                                , Integer.valueOf(MyApplication.get(MyApplication.SettingKey.SETTINGS_UDP_PORT, MyApplication.DEFAULT_UDP_PORT))
                                , MyApplication.get(MyApplication.SettingKey.SETTINGS_USERNAME)
                                , Integer.valueOf(MyApplication._this.getDeviceID()),
                                0, car_park_id , Integer.valueOf(device_id)
                        );

                        String msg = "CALL IN: " + car_park_id + " - " + device_id;
                        msgShow(msg);

                        String strid;
                        strid = mAdapter.getId(car_park_id, device_id);
                        if(strid != "") {
                            //调出详情页面
                            if (!mDetailShow) {
                               if(MyApplication.exist(MyApplication.SettingKey.SETTINGS_ALERT_RING)) {
                                   MediaUtil.playRing(this);
                               }
                               if(MyApplication.exist(MyApplication.SettingKey.SETTINGS_ALERT_RING)) {
                                   MediaUtil.vibrateStart(this, new long[]{1500, 1000, 1500, 1000}, 0);
                               }
                                mDetailShow = true;
                               try {

                                   Intent intent = new Intent(this, ItemDetailActivity.class);
                                   intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, strid);
                                   startActivity(intent);
                               }
                               catch (Exception e) {

                                   //在后台时, 不会弹出接听界面, 此处可能不能执行
                                   mDetailShow = false;
                               }


                            }
                        }



                    }

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public MQTTService getMqttServer()
    {
        return serviceConnection.getMqttService();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    public void CallDevice(String strCarparkId, int iDeviceId)
    {
        soundserver.CallUdpServer(
                MyApplication.get(SettingKey.SETTINGS_SERVER_IP, MyApplication.DEFAULT_IP)
                , Integer.valueOf(MyApplication.get(SettingKey.SETTINGS_UDP_PORT, MyApplication.DEFAULT_UDP_PORT))
                , MyApplication.get(SettingKey.SETTINGS_USERNAME)
                , Integer.valueOf(MyApplication.get(SettingKey.SETTINGS_DEVICE_ID)),
                0, strCarparkId , Integer.valueOf(iDeviceId),0
        );
    }

    public void HangupDevice(String strCarparkId, int iDeviceId)
    {
        soundserver.HangupUdpServer(MyApplication.get(SettingKey.SETTINGS_SERVER_IP, MyApplication.DEFAULT_IP)
                , Integer.valueOf(MyApplication.get(SettingKey.SETTINGS_UDP_PORT, MyApplication.DEFAULT_UDP_PORT))
                , MyApplication.get(SettingKey.SETTINGS_USERNAME)
                , Integer.valueOf(MyApplication.get(SettingKey.SETTINGS_DEVICE_ID)),
                0, strCarparkId , Integer.valueOf(iDeviceId),0
        );
    }

    //
    public void AnswerDevice(String strCarparkId, int iDeviceId)
    {
        MediaUtil.stopRing();
        soundserver.AnswerUdpServer(MyApplication.get(SettingKey.SETTINGS_SERVER_IP, MyApplication.DEFAULT_IP)
                , Integer.valueOf(MyApplication.get(SettingKey.SETTINGS_UDP_PORT, MyApplication.DEFAULT_UDP_PORT))
                , MyApplication.get(SettingKey.SETTINGS_USERNAME)
                , Integer.valueOf(MyApplication.get(SettingKey.SETTINGS_DEVICE_ID)),
                0, strCarparkId , Integer.valueOf(iDeviceId)
        );
    }

    //被叫时, 用户没有接听, 超时后调用, 可以在此取消
    public void CallFree()
    {
        MediaUtil.stopRing();
        MediaUtil.virateCancle(this);

    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final MainActivity mParentActivity;
        private final List<DeviceContent.DeviceItem> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceContent.DeviceItem item = (DeviceContent.DeviceItem) view.getTag();
                if (mTwoPane) {//大屏模式
                    Bundle arguments = new Bundle();
                    arguments.putString(ItemDetailFragment.ARG_ITEM_ID, item.id);
                    ItemDetailFragment fragment = new ItemDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();
                } else {

                    //调出详情页面
                    Context context = view.getContext();
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id);
                    context.startActivity(intent);
                    mParentActivity.mDetailShow = true;

                }
            }
        };

        SimpleItemRecyclerViewAdapter(MainActivity parent,
                                      List<DeviceContent.DeviceItem> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        public String getId(String strCarParkId, String strDeviceId)
        {

            String id = "";
            DeviceContent.DeviceItem val;
                for(Iterator<DeviceContent.DeviceItem> it = mValues.iterator(); it.hasNext();){

                    val = it.next();
                    if(val.car_park_id.equals(strCarParkId) && val.device_id.equals(strDeviceId))
                    {
                        id = val.id;
                        break;
                    }
                }
                return  id;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mIdView.setText(mValues.get(position).id);

            String str;
            str = "Carpark ";
            str += mValues.get(position).car_park_id;
            str += " - ";
            str += mValues.get(position).device_id;
            str += " - ";
            str += mValues.get(position).device_remark;
            try {
                str = new String(str.getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            holder.mContentView.setText(str);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mIdView = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }
}