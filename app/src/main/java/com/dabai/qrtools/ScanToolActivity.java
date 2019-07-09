package com.dabai.qrtools;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cn.simonlee.xcodescanner.core.CameraScanner;
import cn.simonlee.xcodescanner.core.GraphicDecoder;
import cn.simonlee.xcodescanner.core.NewCameraScanner;
import cn.simonlee.xcodescanner.core.OldCameraScanner;
import cn.simonlee.xcodescanner.core.ZBarDecoder;
import cn.simonlee.xcodescanner.view.AdjustTextureView;

public class ScanToolActivity extends AppCompatActivity implements CameraScanner.CameraListener, TextureView.SurfaceTextureListener, GraphicDecoder.DecodeListener, View.OnClickListener {

    private AdjustTextureView mTextureView;
    private View mScannerFrameView;
    private CameraScanner mCameraScanner;
    protected GraphicDecoder mGraphicDecoder;
    protected String TAG = "XCodeScanner";
    private Button mButton_Flash;
    private int[] mCodeType;

    TextView sc_text;
    CardView sc_card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_tool);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        //是否阻止截图
        if (Control.is_sc) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
        //dark
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        mTextureView = findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        mScannerFrameView = findViewById(R.id.scannerframe);

        mButton_Flash = findViewById(R.id.btn_flash);
        mButton_Flash.setOnClickListener(this);
        sc_text = findViewById(R.id.textView3);
        sc_card = findViewById(R.id.sc_card);



        /*
         * 注意，SDK21的设备是可以使用NewCameraScanner的，但是可能存在对新API支持不够的情况，比如红米Note3（双网通Android5.0.2）
         * 开发者可自行配置使用规则，比如针对某设备型号过滤，或者针对某SDK版本过滤
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCameraScanner = new NewCameraScanner(this);
        } else {
            mCameraScanner = new OldCameraScanner(this);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 1) {
            startActivity(new Intent(this,getClass()));
            finish();

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onRestart() {
        if (mTextureView.isAvailable()) {
            //部分机型转到后台不会走onSurfaceTextureDestroyed()，因此isAvailable()一直为true，转到前台后不会再调用onSurfaceTextureAvailable()
            //因此需要手动开启相机
            mCameraScanner.setPreviewTexture(mTextureView.getSurfaceTexture());
            mCameraScanner.setPreviewSize(mTextureView.getWidth(), mTextureView.getHeight());
            mCameraScanner.openCamera(this.getApplicationContext());
        }
        super.onRestart();
    }

    @Override
    protected void onPause() {
        mCameraScanner.closeCamera();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mCameraScanner.setGraphicDecoder(null);
        if (mGraphicDecoder != null) {
            mGraphicDecoder.setDecodeListener(null);
            mGraphicDecoder.detach();
        }
        mCameraScanner.detach();
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                // 处理返回逻辑
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCameraScanner.setPreviewTexture(surface);
        mCameraScanner.setPreviewSize(width, height);
        mCameraScanner.openCamera(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // TODO 当View大小发生变化时，要进行调整。
//        mTextureView.setImageFrameMatrix();
//        mCameraScanner.setPreviewSize(width, height);
//        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override// 每有一帧画面，都会回调一次此方法
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void openCameraSuccess(int frameWidth, int frameHeight, int frameDegree) {
        mTextureView.setImageFrameMatrix(frameWidth, frameHeight, frameDegree);
        if (mGraphicDecoder == null) {
            // mGraphicDecoder = new DebugZBarDecoder(this, mCodeType);//使用带参构造方法可指定条码识别的格式
            mGraphicDecoder = new ZBarDecoder(this);
        }
        //该区域坐标为相对于父容器的左上角顶点。
        //TODO 应考虑TextureView与ScannerFrameView的Margin与padding的情况
        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
        mCameraScanner.setGraphicDecoder(mGraphicDecoder);
    }

    @Override
    public void openCameraError() {
        Toast.makeText(this, "出错了", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void noCameraPermission() {

        /**
         * 申请权限
         */
        int checkResult1 = getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.CAMERA);
        //if(!=允许),抛出异常
        if (checkResult1 != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(this).setTitle("权限申请")
                    .setMessage("不给我相机权限，我就不工作了哦!(/≧▽≦)/")
                    .setCancelable(false)
                    .setNeutralButton("我就不给你", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setPositiveButton("授权", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1); // 动态申请读取权限
                            }
                        }
                    })
                    .show();

        }

    }

    @Override
    public void cameraDisconnected() {
        Toast.makeText(this, "断开连接", Toast.LENGTH_SHORT).show();

    }


    @Override
    public void cameraBrightnessChanged(int brightness) {

    }

    int mCount = 0;
    String mResult = null;

    void ToResult(String data) {

        if (data != null) {
            String result = data;
            Intent intent = new Intent(this, ScanResultActivity.class);
            intent.putExtra("result", result);
            startActivity(intent);
        }
    }


    @Override
    public void decodeComplete(String result, int type, int quality, int requestCode) {
        if (result == null) return;
        if (result.equals(mResult)) {
            if (++mCount > 3) {//连续四次相同则显示结果（主要过滤脏数据，也可以根据条码类型自定义规则）
                if (quality < 10) {
                    sc_card.setVisibility(View.VISIBLE);
                    sc_text.setText(result);

                } else if (quality < 100) {
                    sc_card.setVisibility(View.VISIBLE);
                    sc_text.setText(result);
                } else {
                    sc_card.setVisibility(View.VISIBLE);
                    sc_text.setText(result);
                }
            }
        } else {
            mCount = 1;
            mResult = result;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_flash: {
                if (v.isSelected()) {
                    ((Button) v).setText("开灯");
                    v.setSelected(false);
                    mCameraScanner.closeFlash();
                } else {
                    ((Button) v).setText("关灯");
                    v.setSelected(true);
                    mCameraScanner.openFlash();
                }
                break;
            }
        }
    }


    public void backRes(View view) {
        ToResult(sc_text.getText().toString());
        finish();
    }
}