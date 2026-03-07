package com.abualzahra.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    // هذا الرابط مهم جداً ليعتقد التطبيق أن الملف من نفس نطاق الخادم، مما يسمح بعمل Firebase و Twilio
    // استبدل الرابط برابط الـ Base URL الخاص بمشروعك (بدون مسار الملف)
    private String BASE_URL = "https://9424e054-f128-42e3-bff2-a475eb231a04-00-3fw9va4yton87.sisko.replit.dev/";
    
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath = null;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        
        // تمكين User Agent
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " AbuAlZahraNativeApp/1.0");

        // إعداد WebView
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // التعامل مع الروابط الخارجية (واتساب، هاتف، إلخ)
                if (url.startsWith("tel:") || url.startsWith("whatsapp:") || url.startsWith("intent:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        // طلب الأذونات
        if (checkPermissions()) {
            loadLocalHtml();
        } else {
            requestPermissions();
        }
    }

    private void loadLocalHtml() {
        try {
            // قراءة ملف index.html من مجلد assets
            InputStream is = getAssets().open("index.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String htmlContent = new String(buffer, "UTF-8");

            // تحميل الكود مع تحديد الـ Base URL
            // هذا يجعل المتصفح يعتبر الملف جزءاً من الخادم، فيعمل Firebase و Twilio بشكل ممتاز
            webView.loadDataWithBaseURL(BASE_URL, htmlContent, "text/html", "UTF-8", null);
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ في تحميل ملف التطبيق", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkPermissions() {
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int resultCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultContact = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        return resultMic == PackageManager.PERMISSION_GRANTED && 
               resultCam == PackageManager.PERMISSION_GRANTED &&
               resultContact == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        loadLocalHtml();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // --- كود التعامل مع الكاميرا والملفات (مهم جداً لتطبيق كامل) ---
    private class MyWebChromeClient extends WebChromeClient {
        
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request.grant(request.getResources());
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(MainActivity.this,
                                    getPackageName() + ".fileprovider",
                                    photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
            return true;
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;
        if (resultCode == RESULT_OK) {
            if (data == null) {
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }
}
