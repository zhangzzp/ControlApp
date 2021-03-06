package com.example.zhang.controlapp;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhang.controlapp.common.EventMsg;
import com.example.zhang.controlapp.models.DeviceModel;
import com.example.zhang.controlapp.models.RoundImageDrawable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ControlDevice extends BaseActivity {

    private DeviceModel mDevice = null;
    private String ip;
    private Integer port;
    private Integer movienumber;

    private static final String TAG = "ControlD";
    private boolean keystate =true;

    private EditText sendext;
    private TextView device_name,device_host,device_port,text;
    private ImageView wifi;
    private ImageButton paly;
    private Button open,send;
    private ListView MovieList;

    private String moviename;
   // private Intent intent;

    private boolean isConnectSuccess = false;

    //private List<String> item = new ArrayList<>();
    private ArrayList<String> item= new ArrayList<String>();



    private static int i=0;

    private ServiceConnection sc;
    public SocketService socketService;

    //ProgressDialog pd ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_device);



        /*register EventBus*/
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        device_name = findViewById(R.id.device_name);
        device_host = findViewById(R.id.device_host);
        device_port = findViewById(R.id.device_port);
        text =findViewById(R.id.textView3);

        paly = findViewById(R.id.play);

        open =findViewById(R.id.open);
        send =findViewById(R.id.send);

        MovieList = findViewById(R.id.movielist);

        paly.setImageDrawable(new RoundImageDrawable(BitmapFactory.decodeResource(getResources(),R.mipmap.play)));


        wifi = findViewById(R.id.wifi);

        Intent intent = getIntent();
        DeviceModel device = (DeviceModel)intent.getSerializableExtra("deviceObject");
        mDevice =device;

        device_name.setText(device.getName());
        device_host.setText(device.getHost());
        device_port.setText(device.getPort().toString());

        //String[] arr1= {"diyi","第一"};
//         ArrayAdapter<String> list1 =new ArrayAdapter<String>(this,R.layout.movie_view,arr1);
//
//          MovieList.setAdapter(list1);


        /*先判断 Service是否正在运行 如果正在运行  给出提示  防止启动多个service*/
        if (isServiceRunning("com.example.zhang.controlapp.SocketService")) {
            Toast.makeText(this, "连接服务已运行", Toast.LENGTH_SHORT).show();
            //return;
        }else {
            onService(device);
        }

        bindSocketService();//连接socket服务。

        paly.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!keystate){
                paly.setImageDrawable(new RoundImageDrawable(BitmapFactory.decodeResource(getResources(),R.mipmap.play)));
                socketService.sendOrder("pause");
                keystate=!keystate;
                }
                else {
                    paly.setImageDrawable(new RoundImageDrawable(BitmapFactory.decodeResource(getResources(),R.mipmap.pause)));
                    socketService.sendOrder("pause");
                    keystate=!keystate;
                }
            }
        });

        open.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                socketService.sendOrder("open");
            }
        });

        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //ext.setText("");
                socketService.sendOrder("full");
            }
        });


       MovieList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               moviename=MovieList.getItemAtPosition(i).toString();
               //Toast.makeText(getApplicationContext(),"text="+text , Toast.LENGTH_SHORT).show();
               showmovie();
           }
       });

    }


    private void showmovie() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("选择影片");
        alert.setMessage("确定选择"+moviename+"影片播放吗？" + "\n" );
        alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                socketService.sendOrder(moviename);

                socketService.sendOrder("open");
            }
        });
        alert.setNegativeButton("返回", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //finish();
            }
        });
        alert.show();
    }

    private void onService (DeviceModel device){
        /*启动service*/
        Intent intent = new Intent(getApplicationContext(), SocketService.class);
        intent.putExtra("deviceObject", device);
        startService(intent);
        //Toast.makeText(getApplicationContext(), "Service is on", Toast.LENGTH_SHORT).show();

    }


    /**
     * 判断服务是否运行
     */
    private boolean isServiceRunning(final String className) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> info = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (info == null || info.size() == 0) return false;
        for (ActivityManager.RunningServiceInfo aInfo : info) {
            if (className.equals(aInfo.service.getClassName())) return true;
        }
        return false;
    }

    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }




    /*连接成功的话 socketService返回连接情况*/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void skipToMainActivity(EventMsg msg) {
        Log.i(TAG,msg.getTag());

        if(isInteger(msg.getTag())){
            movienumber=Integer.valueOf(msg.getTag()).intValue();
            Log.i(TAG,movienumber.toString());
        }
        else {
            switch (msg.getTag()) {
                case "connectSucccess":
                    /*接收到这个消息说明连接成功*/
                    isConnectSuccess = true;
                    /*连接成功后干的事*/

                    wifi.setImageResource(R.mipmap.wifi);
                    break;
                case "connectfaile":

                    isConnectSuccess = false;
                    wifi.setImageResource(R.mipmap.nowifi);

                    AlertDialog.Builder alert = new AlertDialog.Builder(this);

                    alert.setTitle("连接失败");
                    alert.setMessage("点击确定重新连接。" + "\n\n" + "点击返回重新选择机器。");

                    alert.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            socketService.reconnect();
                        }
                    });

                    alert.setNegativeButton("返回", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
                    alert.show();
                    break;
                case "ok":
                    //                socketService.sendOrder("收到了老板");
                   // text.setText(text.getText() + msg.getTag());
                    break;
                default:
                    if(i<movienumber) {
                        item.add(msg.getTag());
                        i++;
                    }
                    else{
                        String[] movie = new String[item.size()];
                        Log.i(TAG, "length=" + item.size());
                        item.toArray(movie);
                        Log.i(TAG, "item=" + item);
                        ArrayAdapter<String> list1 =new ArrayAdapter<String>(this,R.layout.movie_view,movie);
                        MovieList.setAdapter(list1);
                        i=0;
                        item.clear();
                    }

                    //socketService.sendOrder("收到了老板");
                    //text7.setText(text7.getText()+"\n"+msg.getTag());
                    break;
            }
        }
    }

    private void bindSocketService() {

        /*通过binder拿到service*/
        sc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                SocketService.SocketBinder binder = (SocketService.SocketBinder) iBinder;
                socketService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        Intent intent = new Intent(getApplicationContext(), SocketService.class);
        bindService(intent, sc, BIND_AUTO_CREATE);
    }




    /**
     * Upon options menu creation inflates the menu and sets
     * button functionalities and visibility.
     * @param menu      Menu entity to inflate.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        menu.findItem(R.id.action_new).setVisible(false);
        menu.findItem(R.id.action_save).setVisible(false);
        menu.findItem(R.id.action_help).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
            case android.R.id.home:
                //返回键，退回前关闭服务进程
                Intent intent = new Intent(this, SocketService.class);
                stopService(intent);
                this.finish();
                break;
//            case R.id.action_save:
//                if (validateFormValues()) {
//                    getFormValues();
//                    saveDeviceToDb();
//                    this.finish();
//                }
//                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*unregister EventBus*/
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        unbindService(sc);
        Intent intent = new Intent(getApplicationContext(), SocketService.class);
        stopService(intent);

    }

}
