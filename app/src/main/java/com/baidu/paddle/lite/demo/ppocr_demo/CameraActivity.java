package com.baidu.paddle.lite.demo.ppocr_demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.paddle.lite.demo.common.CameraSurfaceView;
import com.baidu.paddle.lite.demo.common.CommonUtils;
import com.baidu.paddle.lite.demo.common.Utils;
import com.baidu.paddle.lite.demo.common.UtilsYolov5V;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CameraActivity extends Activity implements View.OnClickListener, CameraSurfaceView.OnTextureChangedListener {
    private static final String TAG = CameraActivity.class.getSimpleName();

    CameraSurfaceView svPreview;
    TextView tvStatus;
    ImageButton btnSwitch;
    ImageButton btnShutter;

    String savedImagePath = "images/save.jpg";
    int lastFrameIndex = 0;
    long lastFrameTime;

    // Model settings of object detection
    protected String detModelPath = "ch_ppocr_mobile_v2.0_det_slim_opt.nb";
    protected String recModelPath = "ch_ppocr_mobile_v2.0_rec_slim_opt.nb";
    protected String clsModelPath = "ch_ppocr_mobile_v2.0_cls_slim_opt.nb";
    protected String labelPath = "ppocr_keys_v1.txt";
    protected String configPath = "config.txt";
    protected int cpuThreadNum = 1;
    protected String cpuPowerMode = "LITE_POWER_HIGH";


    Native predictor = new Native();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);
        String fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        CommonUtils.copyAssetsDirToSDCard(CameraActivity.this, "fonts", fileDir);


        // Init the camera preview and UI components
        initView();
        initSettings();
        // Check and request CAMERA and WRITE_EXTERNAL_STORAGE permissions
        if (!checkAllPermissions()) {
            requestAllPermissions();
        }
    }

    private void initSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        SettingsActivity.resetSettings();
    }

    public void checkAndUpdateSettings() {
        if (SettingsActivity.checkAndUpdateSettings(this)) {
            String realModelDir = getCacheDir() + "/" + SettingsActivity.modelDir;
            UtilsYolov5V.copyDirectoryFromAssetsYolov5V(this, SettingsActivity.modelDir, realModelDir);
            String realLabelPath = getCacheDir() + "/" + SettingsActivity.labelPath;
            UtilsYolov5V.copyFileFromAssetsYolov5V(this, SettingsActivity.labelPath, realLabelPath);
            predictor.init_yolov5v(
                    realModelDir,
                    realLabelPath,
                    SettingsActivity.cpuThreadNum,
                    SettingsActivity.cpuPowerMode,
                    SettingsActivity.inputWidth,
                    SettingsActivity.inputHeight,
                    SettingsActivity.inputMean,
                    SettingsActivity.inputStd,
                    SettingsActivity.scoreThreshold);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_switch:
                svPreview.switchCamera();
                break;
            case R.id.btn_shutter:
                SimpleDateFormat date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                synchronized (this) {
                    savedImagePath = Utils.getDCIMDirectory() + File.separator + date.format(new Date()).toString() + ".png";
                }
                Toast.makeText(CameraActivity.this, "Save snapshot to " + savedImagePath, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onTextureChanged(int inTextureId, int outTextureId, int textureWidth, int textureHeight) {
        String savedImagePath = "";
        synchronized (this) {
            savedImagePath = CameraActivity.this.savedImagePath;
        }
        savedImagePath = Utils.getDCIMDirectory() + File.separator + "result.jpg";
        boolean modified = predictor.process(inTextureId, outTextureId, textureWidth, textureHeight, savedImagePath);

        if (!savedImagePath.isEmpty()) {
            synchronized (this) {
                CameraActivity.this.savedImagePath = "";
            }
        }
        lastFrameIndex++;
        if (lastFrameIndex >= 30) {
            final int fps = (int) (lastFrameIndex * 1e9 / (System.nanoTime() - lastFrameTime));
            runOnUiThread(() -> tvStatus.setText(fps + "fps"));
            lastFrameIndex = 0;
            lastFrameTime = System.nanoTime();
        }
        Log.e("TAG","modified: "+ modified);
        return modified;
    }

    @Override
    public boolean onTextureChanged(Bitmap ARGB8888ImageBitmap) {
        String savedImagePath = "";
        synchronized (this) {
            savedImagePath = CameraActivity.this.savedImagePath;
        }
        savedImagePath = Utils.getDCIMDirectory() + File.separator + "result.jpg";
        boolean modified = predictor.process_yolov5v(ARGB8888ImageBitmap, savedImagePath);

        if (!savedImagePath.isEmpty()) {
            synchronized (this) {
                CameraActivity.this.savedImagePath = "";
            }
        }
        return modified;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload settings and re-initialize the predictor
        checkRun();
        checkAndUpdateSettings();
        // Open camera until the permissions have been granted
        if (!checkAllPermissions()) {
            svPreview.disableCamera();
        }
        svPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        svPreview.onPause();
    }

    @Override
    protected void onDestroy() {
        if (predictor != null) {
            predictor.release();
            predictor.release_yolov5v();
        }
        super.onDestroy();
    }

    public void initView() {
        svPreview = (CameraSurfaceView) findViewById(R.id.sv_preview);
        svPreview.setOnTextureChangedListener(this);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);
        btnShutter = (ImageButton) findViewById(R.id.btn_shutter);
        btnShutter.setOnClickListener(this);
    }

    public void checkRun() {
            try {
            Utils.copyAssets(this, labelPath);
            String labelRealDir = new File(
                    this.getExternalFilesDir(null),
                    labelPath).getAbsolutePath();

            Utils.copyAssets(this, configPath);
            String configRealDir = new File(
                    this.getExternalFilesDir(null),
                    configPath).getAbsolutePath();

            Utils.copyAssets(this, detModelPath);
            String detRealModelDir = new File(
                    this.getExternalFilesDir(null),
                    detModelPath).getAbsolutePath();

            Utils.copyAssets(this, clsModelPath);
            String clsRealModelDir = new File(
                    this.getExternalFilesDir(null),
                    clsModelPath).getAbsolutePath();

            Utils.copyAssets(this, recModelPath);
            String recRealModelDir = new File(
                    this.getExternalFilesDir(null),
                    recModelPath).getAbsolutePath();

                predictor.init(
                        this,
                        detRealModelDir,
                        clsRealModelDir,
                        recRealModelDir,
                        configRealDir,
                        labelRealDir,
                        cpuThreadNum,
                        cpuPowerMode);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(CameraActivity.this)
                    .setTitle("Permission denied")
                    .setMessage("Click to force quit the app, then open Settings->Apps & notifications->Target " +
                            "App->Permissions to grant all of the permissions.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CameraActivity.this.finish();
                        }
                    }).show();
        }
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA}, 0);
    }

    private boolean checkAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
