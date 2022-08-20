package com.cpos.cposmonitor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cpos.cposmonitor.data.model.DeviceContent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.Toolbar;

import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;

import android.view.MenuItem;
import android.widget.TextView;

import java.util.Calendar;

/**
 * An activity representing a single Item detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link MainActivity}.
 */
public class ItemDetailActivity extends AppCompatActivity {

    private MyApplication theApp = null;

    private  MainActivity mainActivity = null;

    private DeviceContent.DeviceItem mItem;
    boolean mTimer = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);


        theApp = (MyApplication) getApplication();

        mainActivity = MainActivity.getInstance();

        mainActivity.mDetailShow = true;

        mItem = DeviceContent.ITEM_MAP.get( getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));

        FloatingActionButton detail_call = (FloatingActionButton) findViewById(R.id.detail_call);



        detail_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mainActivity.soundserver.getTelState() == UdpSoundServer.CPOS_TEL_FUN_FREE)
                {
                    mainActivity.CallDevice(mItem.car_park_id, Integer.valueOf(mItem.device_id));
                }
                else {
                    mainActivity.AnswerDevice(mItem.car_park_id, Integer.valueOf(mItem.device_id));
                }

                if(mTimer) {
                    mTimer = false;
                    Thread threadTimer = new Thread(new Runnable() {
                        public void run() {
                            Calendar timeStart = null;
                            Calendar timeEnd = null;
                            long lSec = 0;
                            while (true) {

                                if (mainActivity.soundserver.getTelState() == UdpSoundServer.CPOS_TEL_FUN_FREE) {
                                    break;
                                }

                                try {
                                    if (timeStart == null) {
                                        timeStart = Calendar.getInstance();
                                    } else {
                                        timeEnd = Calendar.getInstance();
                                        lSec = timeEnd.getTimeInMillis() - timeStart.getTimeInMillis();
                                        lSec /= 1000;

                                        //超时未接通,挂断
                                        if (lSec > 10 && mainActivity.soundserver.getTelState() != UdpSoundServer.CPOS_TEL_FUN_BUSY) {
                                            mainActivity.HangupDevice(mItem.car_park_id, Integer.valueOf(mItem.device_id));
                                            break;
                                        }


                                        String strText;
                                        strText = Long.toString(lSec / 60) + ":" + Long.toString(lSec % 60);
                                        ((TextView) findViewById(R.id.item_detail)).setText(strText);

                                        Thread.sleep(100);//10秒

                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    });
                    threadTimer.start();
                }

            }
        });


        FloatingActionButton detail_hangup = (FloatingActionButton) findViewById(R.id.detail_hangup);
        detail_hangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mainActivity.HangupDevice(mItem.car_park_id, Integer.valueOf(mItem.device_id));

                if(mainActivity.soundserver.getTelState() == UdpSoundServer.CPOS_TEL_FUN_FREE) {

                    mainActivity.mDetailShow = false;
                    finish();
                }

            }
        });



        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don"t need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ItemDetailFragment.ARG_ITEM_ID,
                    getIntent().getStringExtra(ItemDetailFragment.ARG_ITEM_ID));
            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
          //  navigateUpTo(new Intent(this, MainActivity.class));
           // finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
          //  finish();

            //不执行父类点击事件
            return true;
        }
        //继续执行父类其他点击事件
        return super.onKeyUp(keyCode, event);
    }

}