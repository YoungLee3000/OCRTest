package com.nlscan.ocrtest;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nlscan.ocrtest.util.Constants;
import com.nlscan.ocrtest.util.PostUtil;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final int CHANGE_SUCCESS = 1;

    private static final int CHANGE_PROCESS = 2;

    private static final int CHANGE_FAILURE = 3;

    private static final int CHANGE_BROAD = 4;


    private EditText mEditUser;

    private Button mBtnQuery,mBtnClear;

    private RecyclerView mRVAddress;

    private MailInfoAdapter mailInfoAdapter;

    private List<MailInfo> mailInfoList = new ArrayList<>();


    //服务器地址
    private boolean mIfJson = true;
//    private String dataUrl = "http://www.gutejersy.com/android/getAddress.php";
    private String dataUrl = "http://www.nlsmall.com/emsExpress/support.do?sendQuery";
//    private String dataUrl = "http://192.168.74.131:8080/emsExpress/support.do?sendQuery";
    private MyHandler myHandler = new MyHandler(this);

    //对话框
    private ProgressDialog mDialog;


    //扫码广播相关
    private static final String ACTION_SCAN_RESULT = "nlscan.action.SCANNER_RESULT";
    private static final String ACTION_SCAN_RESULT_MAIL = "ACTION_BAR_SCAN";


    /**
     * 普通广播
     */
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {

            final String scanResult_1=intent.getStringExtra("SCAN_BARCODE1");

            final String scanStatus=intent.getStringExtra("SCAN_STATE");


            if("ok".equals(scanStatus)){
                Message meg = Message.obtain();
                meg.what = CHANGE_BROAD;
                meg.obj = scanResult_1;
                myHandler.sendMessage(meg);
            }else{

                Toast.makeText(MainActivity.this,"扫码失败",Toast.LENGTH_SHORT).show();
            }

        }
    };


    /**
     * 邮政广播
     */
    private BroadcastReceiver  mScanReceiverMail = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            final String scanResult_1=intent.getStringExtra("EXTRA_SCAN_DATA");
            final String scanStatus=intent.getStringExtra("EXTRA_SCAN_STATE");

            if("ok".equals(scanStatus)){

                Message meg = Message.obtain();
                meg.what = CHANGE_BROAD;
                meg.obj = scanResult_1;
                myHandler.sendMessage(meg);

            }else{
                Toast.makeText(MainActivity.this,"扫码失败",Toast.LENGTH_SHORT).show();
            }

        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        mEditUser = (EditText) findViewById(R.id.edit_userID);

        mBtnQuery = (Button) findViewById(R.id.btn_query);

        mBtnClear = (Button) findViewById(R.id.btn_clear);

        mRVAddress = (RecyclerView) findViewById(R.id.rv_address);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);//不弹出输入框

        barcodeSet();//设置条码输出

        mailInfoAdapter = new MailInfoAdapter(this,mailInfoList);


        mailInfoAdapter.setOnItemClickListener(new MailInfoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                MailInfo mailInfo = mailInfoList.get(position);



                Intent intent = new Intent(MainActivity.this,EditInfoActivity.class);

                intent.putExtra(Constants.SE_NAME,mailInfo.getSenderName());
                intent.putExtra(Constants.SE_PHONE,mailInfo.getSenderPhone());
                intent.putExtra(Constants.SE_ADDRESS,mailInfo.getSenderAddress());
                intent.putExtra(Constants.RE_NAME,mailInfo.getReceiverName());
                intent.putExtra(Constants.RE_PHONE,mailInfo.getReceiverPhone());
                intent.putExtra(Constants.RE_ADDRESS,mailInfo.getReceiverAddress());
                intent.putExtra(Constants.SE_TYPE,mailInfo.getSendType());
                intent.putExtra(Constants.SE_WEIGHT,mailInfo.getSendWeight());
                intent.putExtra(Constants.SE_CODE,mailInfo.getSendCode());

                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);

            }
        });

        mRVAddress.setAdapter(mailInfoAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
//        linearLayoutManager.setStackFromEnd(true);
        mRVAddress.setLayoutManager(linearLayoutManager);

        mBtnQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String para = mEditUser.getText().toString();
                showLoadingWindow("获取数据中");

                new Thread()
                {
                    @Override
                    public void run()
                    {

                        Map<String,String> map = new HashMap<>();
                        Date d1 = new Date(System.currentTimeMillis());
                        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                        map.put("barcodedata",para);
                        map.put("optime",df.format(d1));

//                        myHandler.sendEmptyMessage(CHANGE_SUCCESS);

                        String response = PostUtil.sendPost(
                                dataUrl,map,"utf-8",mIfJson);
                        Log.d(Constants.TAG,response);
                        mailInfoList.clear();
                        mailInfoList.addAll(PostUtil.parseJson(response));

                        Message toastMeg = Message.obtain();
                        toastMeg.obj = PostUtil.parseJsonResult(response,"msg");
                        toastMeg.what = mailInfoList.size() > 0 ?
                                CHANGE_SUCCESS : CHANGE_FAILURE;

                        myHandler.sendMessage(toastMeg);


                    }

                }.start();

            }
        });

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditUser.setText("");
                mailInfoList.clear();
                mailInfoAdapter.notifyDataSetChanged();
            }
        });




    }



    //注册广播
    private void registerResultReceiver() {
        try {

            IntentFilter scanFilter = new IntentFilter(ACTION_SCAN_RESULT);
            registerReceiver(mScanReceiver,scanFilter);

            IntentFilter scanFilterMail = new IntentFilter(ACTION_SCAN_RESULT_MAIL);
            registerReceiver(mScanReceiverMail,scanFilterMail);

        } catch (Exception e) {
        }

    }

    //注销广播
    private void unRegisterResultReceiver() {
        try {
            unregisterReceiver(mScanReceiver);
            unregisterReceiver(mScanReceiverMail);
        } catch (Exception e) {
        }

    }


    /**
     * 界面唤醒时注册广播
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerResultReceiver();
    }

    /**
     * 界面销毁时注销广播
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterResultReceiver();
    }

    /**
     * 地址查询
     */
    private void addressQuery(String str){
        final String para = str;
        showLoadingWindow("获取数据中");

        new Thread()
        {
            @Override
            public void run()
            {

                Map<String,String> map = new HashMap<>();
                Date d1 = new Date(System.currentTimeMillis());
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                map.put("barcodedata",para);
                map.put("optime",df.format(d1));

//                        myHandler.sendEmptyMessage(CHANGE_SUCCESS);

                String response = PostUtil.sendPost(
                        dataUrl,map,"utf-8",mIfJson);
                Log.d(Constants.TAG,response);
                mailInfoList.clear();
                mailInfoList.addAll(PostUtil.parseJson(response));

                Message toastMeg = Message.obtain();
                toastMeg.obj = PostUtil.parseJsonResult(response,"msg");
                toastMeg.what = mailInfoList.size() > 0 ?
                        CHANGE_SUCCESS : CHANGE_FAILURE;

                myHandler.sendMessage(toastMeg);


            }

        }.start();
    }


    /**
     * 条码设置
     */
    private void barcodeSet(){
        Intent intentConfig = new Intent("ACTION_BAR_SCANCFG");

        intentConfig.putExtra("EXTRA_SCAN_MODE", 3);//广播输出
        intentConfig.putExtra("EXTRA_OUTPUT_EDITOR_ACTION_ENABLE", 0);//不输出软键盘
        sendBroadcast(intentConfig);
    }

    /**
     * 关闭进度条
     */
    protected void  cancelDialog(){
        if (mDialog != null){
            mDialog.dismiss();
        }
    }


    /**
     * 显示进度条
     * @param message
     */
    protected void showLoadingWindow(String message)
    {


        if(mDialog != null && mDialog.isShowing())
            return ;

        mDialog = new ProgressDialog(MainActivity.this) ;
        mDialog.setProgressStyle(ProgressDialog.BUTTON_NEUTRAL);// 设置进度条的形式为圆形转动的进度条
        mDialog.setCancelable(true);// 设置是否可以通过点击Back键取消
        mDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
        // 设置提示的title的图标，默认是没有的，如果没有设置title的话只设置Icon是不会显示图标的
        mDialog.setMessage(message);
        mDialog.show();
    }


    /**
     * 静态Handler
     */
    static class MyHandler extends Handler {

        private SoftReference<MainActivity> mySoftReference;

        public MyHandler(MainActivity mainActivity) {
            this.mySoftReference = new SoftReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg){
            final MainActivity mainActivity = mySoftReference.get();
            String str = (String) msg.obj;
            switch (msg.what) {

                case CHANGE_SUCCESS:
                    mainActivity.cancelDialog();
                    mainActivity.mailInfoAdapter.notifyDataSetChanged();
                    break;
                case CHANGE_PROCESS:
                    mainActivity.showLoadingWindow("数据查询中");
                    break;
                case CHANGE_FAILURE:
                    Toast.makeText(mainActivity,str,Toast.LENGTH_SHORT).show();
                    mainActivity.cancelDialog();
                    break;
                case CHANGE_BROAD:
                    mainActivity.addressQuery(str);
                    break;
            }

        }
    }





}
