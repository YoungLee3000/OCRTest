package com.nlscan.ocrtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nlscan.ocrtest.util.BitMapUtil;
import com.nlscan.ocrtest.util.Constants;
import com.nlscan.ocrtest.util.PostUtil;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostFigureActivity extends BaseActivity implements View.OnClickListener{


    //界面信息
    private Button mTakePhoto, mGenCert,mShowWords;
    private TextView  mTVTip,mWords;
    private ImageView mPicture;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final int REQUEST_PERMISSION_CODE = 267;
    private static final int TAKE_PHOTO = 189;
    private static final int CHOOSE_PHOTO = 385;
    private static final int REQ_CROP = 873;
    private static final String FILE_PROVIDER_AUTHORITY = "com.nlscan.ocrtest.provider";
    private Uri mImageUri; //用来存储拍照图片的URI
    private Uri mSmallUri; //裁剪图片的URI

    private String mFileStr = "";//裁剪后的图片路径
    private boolean myCropExit = false;//判断裁剪应用是否存在



    private String uploadUrl = "http://47.103.30.171:12000/ocr";
    private String dataUrl = "http://www.nlsmall.com/emsExpress/support.do?uploadPackage";

    //本地地址
//    private String uploadUrl = "http://192.168.74.131:8080/emsExpress/support.do?upload";
//    private String dataUrl = "http://192.168.74.131:8080/emsExpress/support.do?uploadPackage";
//
    private boolean mIfJson = true;
    private boolean gIfImage = false; //是否选了照片


    private String mCorrectResult = "0";


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

        private SoftReference<PostFigureActivity> mySoftReference;

        public MyHandler(PostFigureActivity verifyActivity) {
            this.mySoftReference = new SoftReference<>(verifyActivity);
        }

        @Override
        public void handleMessage(Message msg){
            final PostFigureActivity verifyActivity = mySoftReference.get();
            String str = (String) msg.obj;
            switch (msg.what) {
                case CHANGE_SUCCESS:
                    verifyActivity.cancelDialog();
                    verifyActivity.showResult(str);
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
        setContentView(R.layout.activity_post_figure);
        setTitle("信息确认");



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
        mShowWords = (Button)findViewById(R.id.bt_show_word);
        mTVTip = (TextView) findViewById(R.id.text_tips);
        mWords = (TextView) findViewById(R.id.text_words);




        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showBottomDialog();
                takePhoto();
            }
        });


        mShowWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ifShow = mWords.getVisibility() == View.GONE ;
                if (ifShow){
                    mShowWords.setText("关闭文字");
                    mWords.setVisibility(View.VISIBLE);
                }
                else{
                    mShowWords.setText("显示文字");
                    mWords.setVisibility(View.GONE);
                }


            }
        });

        mGenCert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mWords.setText("");
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


                            if ("".equals(mFileStr)){
                                    toastMegStep1.obj = "图片未选择";
                                    toastMegStep1.what = CHANGE_TOAST;
                                    myHandler.sendMessage(toastMegStep1);
                                    break;
                            }

                            String response1 = PostUtil.upload(uploadUrl,mFileStr);
                            String uploadResult =  PostUtil.parseJsonResult(response1,"code");
                            Log.d(Constants.TAG,"the result is " + response1);
                            if ( ! mCorrectResult.equals(uploadResult) ){
                                if (uploadResult == null || "".equals(uploadResult)){
                                    toastMegStep1.obj = "上传图片失败,请检查网络";
                                }
                                else{
                                    toastMegStep1.obj = "无法识别文字,请重新拍照";
                                }


                                toastMegStep1.what = CHANGE_TOAST;
                                myHandler.sendMessage(toastMegStep1);
                                break;
                            }
                            else{
                                toastMegStep1.obj = response1;
                                toastMegStep1.what = CHANGE_SUCCESS;
                                myHandler.sendMessage(toastMegStep1);
                            }


                            break;
                        }






                    }

                }.start();
            }
        });

    }



    private void convertBitmap2Jpg(Bitmap bitmap, String newImgpath) {
        //复制Bitmap 因为png可以为透明，jpg不支持透明，把透明底明变成白色
        //主要是先创建一张白色图片，然后把原来的绘制至上去
        Bitmap outB=bitmap.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas=new Canvas(outB);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        File file = new File(newImgpath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (outB.compress(Bitmap.CompressFormat.JPEG ,100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * 解析返回的json字符串
     * @param objStr
     */
    private void showResult(String objStr){

        mShowWords.setEnabled(true);





        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(mSmallUri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



        String data = PostUtil.parseJsonResult(objStr,"data");
        String item = PostUtil.parseJsonResult(data,"items");

        float angle = 0.0f;
        try {
            angle = Float.parseFloat(PostUtil.parseJsonResult(data,"angle")) ;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        JSONArray relArray = JSON.parseArray(item);

        if (relArray != null && relArray.size()>0){
            if (bitmap == null) return;
            int size = relArray.size();
            String[] words = new String[size];
            int [][] rects = new int[size][4];

            for (int i=0; i<size; i++){
                JSONObject relObj = (JSONObject) relArray.get(i);
                words[i] = relObj.getString("itemstring");
                JSONObject coord = (JSONObject) relObj.getJSONObject("itemcoord");
                rects[i][0] = coord.getInteger("x");
                rects[i][1] = coord.getInteger("y") ;
                rects[i][2] = rects[i][0] +  coord.getInteger("width");
                rects[i][3] = rects[i][1] + coord.getInteger("height") ;

                mWords.append(""+(i+1) + ": " + words[i] + "\n");

            }

            drawRectangles(bitmap,rects,angle);


        }





    }


    //绘制方框
    private void drawRectangles(Bitmap imageBitmap, int[][] keywordRects, float angle) {
        int left, top, right, bottom;

//        //换白底
//        convertBitmap2Jpg(imageBitmap,mFileStr);
////        //获取旋转的图片
//        Bitmap bMapRotate ;
//        Matrix matrix = new Matrix();
//        matrix.reset();
//        matrix.setRotate(-angle)  ;
//        bMapRotate = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(),
//                    imageBitmap.getHeight(), matrix, true);


        Bitmap mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888,true);//Bitmap.createBitmap(imageBitmap.getWidth(),imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);




        Bitmap bitmap = Bitmap.createBitmap(mutableBitmap.getWidth(),mutableBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(mutableBitmap,0,0,new Paint());
//

//        float tranAngle = (360 - angle);
        float sinVal = (float) Math.sin(angle/180*Math.PI);
        float cosVal = (float) Math.cos((90-angle)/180*Math.PI);
        int height = mutableBitmap.getHeight();
        int width = mutableBitmap.getWidth();
        canvas.translate(height * sinVal  ,
                width * cosVal);
        canvas.rotate(angle);




        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        Paint paint2 = new Paint();
        paint2.setColor(Color.RED);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setStrokeWidth(1.0f);
        paint2.setTextSize(36f);


        int size = keywordRects.length;


        Log.d(Constants.TAG,"the width " + mutableBitmap.getWidth()
                + " the height " + mutableBitmap.getHeight());


//        int shiftY = (int) (-mutableBitmap.getWidth()* Math.cos(-angle/180*Math.PI)  )  ;
//
//        int shiftX = (int) (mutableBitmap.getHeight()* Math.sin(-angle/180*Math.PI)  )  ;
//        Log.d(Constants.TAG,"the shift is " + shiftY);



        for (int i = 0; i < size; i++) {
            left = keywordRects[i][0] ;
            top = keywordRects[i][1]  ;
            right = keywordRects[i][2] ;
            bottom = keywordRects[i][3] ;
            canvas.drawRect(left, top, right, bottom, paint);
            canvas.drawText(""+(i+1),left-20,top+20,paint2);
        }

        canvas.save();
        mPicture.setImageBitmap(bitmap);//img: 定义在xml布局中的ImagView控件
//
        mPicture.setBackgroundColor(Color.WHITE);
        mShowWords.performClick();

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
     * 显示进度条
     * @param message
     */
    protected void showLoadingWindow(String message)
    {


        if(mDialog != null && mDialog.isShowing())
            return ;

        mDialog = new ProgressDialog(PostFigureActivity.this) ;
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
            File imageFile = createImageFile();//创建用来保存照片的文件
            mFileStr =  imageFile.getPath();
//            mImageUriFromFile = Uri.fromFile(imageFile);
//            Log.i(Constants.TAG, "takePhoto: uriFromFile " + mImageUriFromFile);
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

        Log.d(Constants.TAG,"进入裁剪");

        String originFileStr = mFileStr;
        Uri originUri = mImageUri;

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
        try {
            startActivityForResult(intent, REQ_CROP);
        } catch (Exception e) {
            e.printStackTrace();
            mFileStr = originFileStr;
            mSmallUri = originUri;
            showImageView();
        }
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
//        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDir = this.getApplicationContext().getFilesDir();
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
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode != RESULT_OK) return;



//                if (myCropExit){
//                    crop();
//                }
//                else{
//                    Log.d(Constants.TAG,"no crop");
                    mSmallUri = mImageUri;
                    showImageView();
//                }
                break;
            case CHOOSE_PHOTO:
                if (data == null) {//如果没有拍照或没有选取照片，则直接返回
                    return;
                }
                Log.i(Constants.TAG, "onActivityResult: ImageUriFromAlbum: " + data.getData());
                if (resultCode == RESULT_OK) {

                    File tempFile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                            new File(handleImageOnKitKat(data.getData())) : new File(handleImageBeforeKitKat(data.getData()));

                    mFileStr =  tempFile.getPath();

                    mImageUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                            FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, tempFile) :
                            Uri.fromFile(tempFile);
                }
//
//                if (myCropExit){
                    crop();
//                }
//                else{
//                    Log.d(Constants.TAG,"no crop");
//                    mSmallUri = mImageUri;
//                    showImageView();
//                }

                break;
            case REQ_CROP:
                showImageView();
                break;
            default:
                break;
        }
    }

    //显示拍照完成或裁剪完成的图片
    private void showImageView(){
        try {
            if (mSmallUri != null) {
                gIfImage = true;
                //压缩图片
                BitMapUtil.compressImage(mFileStr);
                Bitmap bitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(mSmallUri));
                mPicture.setImageBitmap(bitmap);
                mShowWords.setEnabled(false);
                mWords.setVisibility(View.GONE);
                mShowWords.setText("显示文字");
                mGenCert.performClick();
            } else {
                Log.i(Constants.TAG, "onActivityResult: Uri is null");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
