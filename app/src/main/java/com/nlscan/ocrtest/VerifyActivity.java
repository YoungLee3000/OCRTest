package com.nlscan.ocrtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.nlscan.ocrtest.util.BitMapUtil;
import com.nlscan.ocrtest.util.Constants;
import com.nlscan.ocrtest.util.PostUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerifyActivity extends BaseActivity implements View.OnClickListener{


    //界面信息
    private Button mTakePhoto, mGenCert;
    private TextView mTVInfo, mTVTip;
    private ImageView mPicture;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int REQUEST_PERMISSION_CODE = 267;
    private static final int TAKE_PHOTO = 189;
    private static final int CHOOSE_PHOTO = 385;
    private static final int REQ_CROP = 873;
    private static final String FILE_PROVIDER_AUTHORITY = "com.ly.mailsend.provider";
    private Uri mImageUri, mImageUriFromFile;
    private Uri mSmallUri;
    private File imageFile;

    //从主界面传来的信息
    private boolean mIfDanger = false;
    private String mTextHead;
    private String mTextContain;
    private String mUserId;

    private String reName,rePhone,reAddress,seName,sePhone,seAddress,goodType,goodWeight,goodCode;

    //上传信息
//    private String uploadUrl = "http://www.gutejersy.com/android/getFile.php";
//    private String dataUrl = "http://www.gutejersy.com/android/getPath.php";
//    private boolean mIfJson = false;

    private String uploadUrl = "http://www.nlsmall.com/emsExpress/support.do?upload";
    private String dataUrl = "http://www.nlsmall.com/emsExpress/support.do?uploadPackage";

    //本地地址
//    private String uploadUrl = "http://192.168.74.131:8080/emsExpress/support.do?upload";
//    private String dataUrl = "http://192.168.74.131:8080/emsExpress/support.do?uploadPackage";
//
    private boolean mIfJson = true;


    private boolean gIfImage = false;
    private String mFileStr = "";
    private  String mServerFileStr = "";

    private String mCorrectResult = "0000";


    //下载信息
    private String mDownloadStr = "";
    private String mCertStr = "";

    //底部弹出框按钮
    private View bInflate;
    private TextView bChoosePhoto;
    private TextView bTakePhoto;
    private TextView bCancel;
    private Dialog bDialog;

    //进度条
    private ProgressDialog mDialog;


    //静态Handler
    private static final int CHANGE_SUCCESS = 1;
    private static final int CHANGE_PROCESS = 2;
    private static final int CHANGE_MEDIUM = 3;
    private static final int CHANGE_TOAST = 4;
    private MyHandler myHandler = new MyHandler(this);

    /**
     * 静态Handler
     */
    static class MyHandler extends Handler {

        private SoftReference<VerifyActivity> mySoftReference;

        public MyHandler(VerifyActivity verifyActivity) {
            this.mySoftReference = new SoftReference<>(verifyActivity);
        }

        @Override
        public void handleMessage(Message msg){
            final VerifyActivity verifyActivity = mySoftReference.get();
            String str = (String) msg.obj;
            switch (msg.what) {
                case CHANGE_SUCCESS:
                    verifyActivity.cancelDialog();
                    verifyActivity.jumpToShow();
                    break;
                case CHANGE_PROCESS:
                    verifyActivity.showLoadingWindow("数据查询中");
                    break;
                case CHANGE_MEDIUM:
                    Toast.makeText(verifyActivity,str,Toast.LENGTH_SHORT).show();
                    break;
                case CHANGE_TOAST:
                    Toast.makeText(verifyActivity,str,Toast.LENGTH_SHORT).show();
                    verifyActivity.cancelDialog();
                    break;
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        setTitle("信息确认");




        reName = getIntent().getStringExtra(Constants.RE_NAME);
        rePhone = getIntent().getStringExtra(Constants.RE_PHONE);
        reAddress = getIntent().getStringExtra(Constants.RE_ADDRESS);

        seName = getIntent().getStringExtra(Constants.SE_NAME);
        seAddress = getIntent().getStringExtra(Constants.SE_ADDRESS);
        sePhone = getIntent().getStringExtra(Constants.SE_PHONE);

        goodCode = getIntent().getStringExtra(Constants.SE_CODE);
        goodType = getIntent().getStringExtra(Constants.SE_TYPE);
        goodWeight = getIntent().getStringExtra(Constants.SE_WEIGHT) ;

        mIfDanger = "高危物品".equals(goodType);

        /*申请读取存储的权限*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(Constants.TAG,"version larger than 23");
            if (checkSelfPermission(PERMISSION_WRITE_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(Constants.TAG,"no granted!");
                requestPermissions(new String[]{PERMISSION_WRITE_STORAGE}, REQUEST_PERMISSION_CODE);
            }
        }

        mPicture = (ImageView) findViewById(R.id.iv_picture);
        mTakePhoto = (Button) findViewById(R.id.bt_take_photo);
        mGenCert = (Button) findViewById(R.id.bt_gen_cert);
        mTVInfo  = (TextView) findViewById(R.id.text_info);
        mTVTip = (TextView) findViewById(R.id.text_tips);

        if (mIfDanger){
            mTVTip.setText("邮寄高危物品，请确认信息后点击\"拍照\"保存图片，然后点击\"寄件码生成\"按钮获取寄件码");
        }
        else {
            mTakePhoto.setVisibility(View.GONE);
            mTVTip.setText("非高危物品，确认信息后直接点击按钮生成寄件码");
        }


        final String textTotal = " 收件人信息 \n" + "   " +  reName + " " + rePhone + "\n" + reAddress + "\n" +
                                " 寄件人信息 \n" +  "   " +seName + " " + sePhone + "\n" + seAddress + "\n" +
                                " 其它信息 \n" + "   " + goodType + " " + goodWeight + "kg" + "\n" + "单号:" +goodCode  ;
        mTVInfo.setText(textTotal);

        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomDialog();
            }
        });

        mGenCert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                showLoadingWindow("上传数据中");
                new Thread()
                {
                    @Override
                    public void run()
                    {

                        while (true){
                            Message toastMegStep1 = Message.obtain();

                            //如何设置了图片则先上传
                            String[] strArray = mFileStr.split("/");
                            String fileName = strArray[strArray.length -1];

                            if (mIfDanger){
                                if ("".equals(mFileStr)){
                                    toastMegStep1.obj = "图片未选择";
                                    toastMegStep1.what = CHANGE_TOAST;
                                    myHandler.sendMessage(toastMegStep1);
                                    break;
                                }
                                //压缩图片
                                String newFileStr = BitMapUtil.compressImage(mFileStr);
                                String response1 = PostUtil.upload(uploadUrl,newFileStr);
                                String uploadResult =  PostUtil.parseJsonResult(response1,"result");
                                if ( ! mCorrectResult.equals(uploadResult) ){
                                    toastMegStep1.obj = "上传图片失败,请检查网络";
                                    toastMegStep1.what = CHANGE_TOAST;
                                    myHandler.sendMessage(toastMegStep1);
                                    break;
                                }
                                else{
                                    toastMegStep1.obj = "上传图片成功，开始上传揽件信息";
                                    toastMegStep1.what = CHANGE_MEDIUM;
                                    mServerFileStr = PostUtil.parseJsonResult(response1,"serverPath");
                                    myHandler.sendMessage(toastMegStep1);
                                }

                            }


                            //上传揽件信息
                            Message toastMegStep2 = Message.obtain();
                            Map<String,String> map = new HashMap<>();
                            Date d1 = new Date(System.currentTimeMillis());
                            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                            map.put("optime",df.format(d1));
                            map.put("sendcode",goodCode);
                            map.put("sender_name",seName);
                            map.put("sender_phone",sePhone);
                            map.put("sender_adress",seAddress);
                            map.put("receiwer_name",reName);
                            map.put("receiwer_phone",rePhone);
                            map.put("receiwer_adress",reAddress);
                            map.put("send_type",goodType);
                            map.put("send_weight",goodWeight);
                            map.put("sender_photo",mServerFileStr);

                            String response2 = PostUtil.sendPost(
                                    dataUrl,map,"utf-8",mIfJson);

                            String postResult = PostUtil.parseJsonResult(response2,"result");
                            if (! mCorrectResult.equals(postResult)){
                                toastMegStep2.obj = postResult;
                                toastMegStep2.what = CHANGE_TOAST;
                                myHandler.sendMessage(toastMegStep2);
                                break;
                            }
                            else{
                                toastMegStep2.obj = "揽件信息上传成功，开始下载寄件码";
                                toastMegStep2.what = CHANGE_MEDIUM;
                                mDownloadStr = PostUtil.parseJsonResult(response2,"barcodepath");
                                myHandler.sendMessage(toastMegStep2);
                            }


                            //下载寄件码
                            Message toastMegStep3 = Message.obtain();
                            toastMegStep3.what = CHANGE_TOAST;
                            if (mCorrectResult.equals(postResult)){
                                if (isNetworkAvailable(VerifyActivity.this)){
                                    String localFilePath =PostUtil.getDownloadFile2Cache
                                            (mDownloadStr,"mailfigure");
                                    if (!"".equals(localFilePath)){
                                        mCertStr = localFilePath;
                                        myHandler.sendEmptyMessage(CHANGE_SUCCESS);
                                    }
                                    else{
                                        toastMegStep3.obj = "下载失败";
                                        myHandler.sendMessage(toastMegStep3);
                                    }

                                }
                                else {
                                    toastMegStep3.obj = "网络未连接";
                                    myHandler.sendMessage(toastMegStep3);
                                }

                            }
                            break;
                        }






                    }

                }.start();
            }
        });

    }


    /**
     * 判断网络是否可用
     * @param context
     * @return
     */
    private boolean isNetworkAvailable(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }

        return true;
    }



    /**
     * 跳转到网证显示页面
     */
    protected void jumpToShow(){
        Intent intent = new Intent(VerifyActivity.this,QRCodeActivity.class);
        intent.putExtra("certificate",mCertStr);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }


    /**
     * 显示进度条
     * @param message
     */
    protected void showLoadingWindow(String message)
    {


        if(mDialog != null && mDialog.isShowing())
            return ;

        mDialog = new ProgressDialog(VerifyActivity.this) ;
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条的形式为圆形转动的进度条
        mDialog.setCancelable(true);// 设置是否可以通过点击Back键取消
        mDialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
        // 设置提示的title的图标，默认是没有的，如果没有设置title的话只设置Icon是不会显示图标的
        mDialog.setMessage(message);
        mDialog.show();
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
     * 取消对话框
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (bDialog !=null){
            bDialog.cancel();
        }

    }

    /**
     * 界面销毁前关闭进度条
     */
    @Override
    protected void onDestroy() {
        if (bDialog !=null){
            bDialog.cancel();
        }
        super.onDestroy();
    }

    /**
     * 显示底部对话框
     */
    public void showBottomDialog(){
        bDialog = new Dialog(this, R.style.ActionSheetDialogStyle);
        //填充对话框的布局
        bInflate = LayoutInflater.from(this).inflate(R.layout.dialog_bottom, null);
        //初始化控件
        bChoosePhoto = (TextView) bInflate.findViewById(R.id.choosePhoto);
        bTakePhoto = (TextView) bInflate.findViewById(R.id.takePhoto);
        bCancel = (TextView) bInflate.findViewById(R.id.cancel);
        bChoosePhoto.setOnClickListener(this);
        bTakePhoto.setOnClickListener(this);
        bCancel.setOnClickListener(this);
        //将布局设置给Dialog
        bDialog.setContentView(bInflate);
        //获取当前Activity所在的窗体
        Window dialogWindow = bDialog.getWindow();
        //设置Dialog从窗体底部弹出
        dialogWindow.setGravity( Gravity.BOTTOM);
        //设置Diglog宽度匹配屏幕
        dialogWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //获得窗体的属性
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.y = 20;//设置Dialog距离底部的距离
//       将属性设置给窗体
        dialogWindow.setAttributes(lp);
        bDialog.show();//显示对话框
    }


    /**
     * 设置监听事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.takePhoto:
                takePhoto();
                break;
            case R.id.choosePhoto:
                openAlbum();
                break;
            case R.id.cancel:
                bDialog.cancel();
                break;
        }

    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        Intent openAlbumIntent = new Intent(Intent.ACTION_GET_CONTENT);
//        openAlbumIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
//                                 Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        openAlbumIntent.setType("image/*");
        startActivityForResult(openAlbumIntent, CHOOSE_PHOTO);//打开相册
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//打开相机的Intent
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            imageFile = createImageFile();//创建用来保存照片的文件
            mImageUriFromFile = Uri.fromFile(imageFile);
            Log.i(Constants.TAG, "takePhoto: uriFromFile " + mImageUriFromFile);
            if (imageFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    /*7.0以上要通过FileProvider将File转化为Uri*/
                    mImageUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, imageFile);
                } else {
                    /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                    mImageUri = Uri.fromFile(imageFile);
                }
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将用于输出的文件Uri传递给相机
//                takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
//                                         Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(takePhotoIntent, TAKE_PHOTO);//打开相机
            }
        }
    }


    /**
     * 裁剪图片
     */
    private void crop() {

        /*新建用于存剪裁后图片的文件，并转化为Uri*/
        File tempImageFile = createImageFile();
        mFileStr =  tempImageFile.getPath();

        if (tempImageFile != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                /*7.0以上要通过FileProvider将File转化为Uri*/
                mSmallUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, tempImageFile);
            } else {
                /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                mSmallUri = Uri.fromFile(tempImageFile);
            }
        }
        Log.d(Constants.TAG, "mSmallUri: " + mSmallUri.toString() );
        Log.d(Constants.TAG, "mFileStr: " + mFileStr);

        /*File image = new File(getExternalCacheDir() + "/demo.jpg");
        Log.i(TAG, "crop: path " + image.getAbsolutePath());
        mSmallUri = Uri.fromFile(image);*/

        Intent intent = new Intent("com.android.camera.action.CROP");
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
//                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(mImageUri, "image/*");
        intent.putExtra("aspectX", 768);
        intent.putExtra("aspectY", 1024);
        intent.putExtra("outputX", 768);
        intent.putExtra("outputY", 1024);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);//设置为不返回缩略图
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mSmallUri);//设置大图保存到文件
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//保存的图片格式
        intent.putExtra("noFaceDetection", false);

        //给相关的包赋予uri的读写权限
        grantPermissions(this,intent,mSmallUri,true);
        startActivityForResult(intent, REQ_CROP);
    }


    /**
     * @param context
     * @param intent
     * @param uri
     * @param writeAble 是否可读
     */
    private void grantPermissions(Context context, Intent intent, Uri uri, boolean writeAble) {
        int flag = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (writeAble) {
            flag |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        }
        intent.addFlags(flag);
        List<ResolveInfo> resInfoList = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, flag);
        }
    }


    /*缩略图*/
    private void cropAndThumbnail() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(mImageUri, "image/*");//设置要缩放的图片Uri和类型
        intent.putExtra("aspectX", 768);//宽度比
        intent.putExtra("aspectY", 1024);//高度比
        intent.putExtra("outputX", 768);//输出图片的宽度
        intent.putExtra("outputY", 1024);//输出图片的高度
        intent.putExtra("scale", true);//缩放
        intent.putExtra("return-data", false);//当为true的时候就返回缩略图，false就不返回，需要通过Uri
        intent.putExtra("noFaceDetection", false);//前置摄像头

        startActivityForResult(intent, REQ_CROP);
    }

    /**
     * 创建用来存储图片的文件，以时间来命名就不会产生命名冲突
     *
     * @return 创建的图片文件
     */
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }

    /*申请权限的回调*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(Constants.TAG, "onRequestPermissionsResult: permission granted");
        } else {
            Log.i(Constants.TAG, "onRequestPermissionsResult: permission denied");
            Toast.makeText(this, "You Denied Permission", Toast.LENGTH_SHORT).show();
        }
    }

    /*相机或者相册返回来的数据*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode != RESULT_OK) return;
//                if (resultCode == RESULT_OK) {
//                    try {
//                        /*如果拍照成功，将Uri用BitmapFactory的decodeStream方法转为Bitmap*/
//                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mImageUri));
//                        Log.i(TAG, "onActivityResult: imageUri " + mImageUri);
//                        galleryAddPic(mImageUriFromFile);
//                        mPicture.setImageBitmap(bitmap);//显示到ImageView上
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                }

                crop();
                break;
            case CHOOSE_PHOTO:
                if (data == null) {//如果没有拍照或没有选取照片，则直接返回
                    return;
                }
                Log.i(Constants.TAG, "onActivityResult: ImageUriFromAlbum: " + data.getData());
                if (resultCode == RESULT_OK) {

                    File tempFile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                            new File(handleImageOnKitKat(data.getData())) : new File(handleImageBeforeKitKat(data.getData()));

                    mImageUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                            FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, tempFile) :
                            Uri.fromFile(tempFile);

//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
////                        handleImageOnKitKat(data);//4.4之后图片解析
//                        mImageUri = Uri.fromFile(new File(handleImageOnKitKat(data)));
//                    } else {
////                        handleImageBeforeKitKat(data);//4.4之前图片解析
//                        mImageUri = Uri.fromFile(new File(handleImageBeforeKitKat(data)));
//                    }
                }
//
                crop();
                break;
            case REQ_CROP:
                try {
                    if (mSmallUri != null) {
                        gIfImage = true;
                        Bitmap bitmap = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(mSmallUri));
                        mPicture.setImageBitmap(bitmap);
                    } else {
                        Log.i(Constants.TAG, "onActivityResult: Uri is null");
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 4.4版本以下对返回的图片Uri的处理：
     * 就是从返回的Intent中取出图片Uri，直接显示就好
     * @param uri 调用系统相册之后返回的Uri
     */
    private String  handleImageBeforeKitKat(Uri uri) {
//        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
//        displayImage(imagePath);
        return imagePath;
    }

    /**
     * 4.4版本以上对返回的图片Uri的处理：
     * 返回的Uri是经过封装的，要进行处理才能得到真实路径
     * @param uri 调用系统相册之后返回的Uri
     */
    @TargetApi(19)
    private String handleImageOnKitKat(Uri uri ) {
        String imagePath = null;
//        Uri uri = data.getData();
        Log.d(Constants.TAG,"uri image : " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的Uri，则提供document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的uri，则进行普通处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的uri，则直接获取路径
            imagePath = uri.getPath();
        }
//        displayImage(imagePath);
        return imagePath;
    }

    /**
     * 将imagePath指定的图片显示到ImageView上
     */
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            mPicture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将Uri转化为路径
     * @param uri 要转化的Uri
     * @param selection 4.4之后需要解析Uri，因此需要该参数
     * @return 转化之后的路径
     */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * 将拍的照片添加到相册
     *
     * @param uri 拍的照片的Uri
     */
    private void galleryAddPic(Uri uri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        sendBroadcast(mediaScanIntent);
    }




}
