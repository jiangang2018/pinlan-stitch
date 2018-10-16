package com.example.stitch;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.zxy.tiny.Tiny;
import com.zxy.tiny.callback.BitmapBatchCallback;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StitchActivity extends BaseActivity {
    private ImageView ivImage;
    private Mat src;
    private Bitmap stitch_result;
    private List<Mat> listImage = new ArrayList<>();
    private static final String FILE_LOCATION_RESULT = Environment.getExternalStorageDirectory() + "/Download/pinlan_stitch/";
    static int REQUEST_READ_EXTERNAL_STORAGE = 11;
    static boolean read_external_storage_granted = false;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    System.loadLibrary("stitcher");
                    //DO YOUR WORK/STUFF HERE
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(StitchActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(StitchActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }
//        createPanorama();
    }


    public void getBitmaps(Bitmap[] bitmaps) {
//        Tiny.BitmapCompressOptions options = new Tiny.BitmapCompressOptions();
//        Tiny.getInstance().source(bitmaps).batchAsBitmap().withOptions(options).batchCompress(new BitmapBatchCallback() {
//            @Override
//            public void callback(boolean isSuccess, Bitmap[] bitmaps, Throwable t) {
//                if (!isSuccess) {
//                    Toast.makeText(getApplicationContext(), "bitmaps compress bitmap failed!", Toast.LENGTH_SHORT).show();
//                }
//                for (int i = 0; i < bitmaps.length; i = i + 1) {
//                    Bitmap bitmap_temp = bitmaps[i];
//                    src = new Mat(bitmap_temp.getHeight(), bitmap_temp.getWidth(), CvType.CV_8UC4);
//                    Utils.bitmapToMat(bitmap_temp, src);
//                    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
//                    Log.d("MainActivity", "image height " + src.rows() + ", image width " + src.cols());
//                    listImage.add(src);
//                }
//            }
//        });
        for (int i = 0; i < bitmaps.length; i = i + 1) {
            Bitmap bitmap_temp = bitmaps[i];
            src = new Mat(bitmap_temp.getHeight(), bitmap_temp.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(bitmap_temp, src);
            Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
            Log.d("MainActivity", "image height " + src.rows() + ", image width " + src.cols());
            listImage.add(src);
        }
    }

    public void stitchimages(Bitmap[] bitmaps){
        getBitmaps(bitmaps);
        createPanorama();
    }

    public Bitmap stitchresult(){
        return stitch_result;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            } else {
                // permission denied
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }


    public void createPanorama() {
        new AsyncTask<Void, Void, Bitmap>() {
            ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = ProgressDialog.show(StitchActivity.this, "Building Panorama", "Please Wait");
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                int elems = listImage.size();
                long[] tempobjadr = new long[elems];
                for (int i = 0; i < elems; i++) {
                    tempobjadr[i] = listImage.get(i).getNativeObjAddr();
                }
                Mat result = new Mat();

                int stitchstatus = StitchPanorama(tempobjadr, result.getNativeObjAddr());
                Log.d("MainActivity", "result height " + result.rows() + ", result width " + result.cols());
                if (stitchstatus != 0) {
                    Log.e("MainActivity", "Stitching failed: " + stitchstatus);
                    listImage.clear();
                    return null;
                }

                Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGBA);
                Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(result, bitmap);
                stitch_result = bitmap;
                try {
                    File imagesFolder = new File(FILE_LOCATION_RESULT);
                    if (!imagesFolder.exists()) {                //如果不存在，那就建立这个文件夹
                        imagesFolder.mkdirs();
                    }
                    File image = new File(imagesFolder, "panorama_result_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream out = new FileOutputStream(image);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                listImage.clear();
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                dialog.dismiss();
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        onPermissionRequests(Manifest.permission.WRITE_EXTERNAL_STORAGE, new OnBooleanListener() {
            @Override
            public void onClick(boolean bln) {
                if (bln) {

                } else {
                    Toast.makeText(StitchActivity.this, "文件读写或无法正常使用", Toast.LENGTH_SHORT).show();
                }
            }
        });
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("wcj", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.d("wcj", "OpenCV library found inside package. Using it!");
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }
    public native int StitchPanorama(long[] imageAddressArray, long outputAddress);
}
