package com.ngalabi.mobileapp.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebSettings.PluginState;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ngalabi.mobileapp.App;
import com.ngalabi.mobileapp.Config;
import com.ngalabi.mobileapp.util.GetFileInfo;
import com.ngalabi.mobileapp.R;
import com.ngalabi.mobileapp.widget.webview.WebToAppChromeClient;
import com.ngalabi.mobileapp.widget.webview.WebToAppWebClient;
import com.ngalabi.mobileapp.activity.MainActivity;
import com.ngalabi.mobileapp.widget.AdvancedWebView;
import com.ngalabi.mobileapp.widget.scrollable.ToolbarWebViewScrollListener;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class WebFragment extends Fragment implements AdvancedWebView.Listener, SwipeRefreshLayout.OnRefreshListener{

    //Layouts
    public FrameLayout rl;
    public AdvancedWebView browser;
    public SwipeRefreshLayout swipeLayout;
    public ProgressBar progressBar;

    //WebView Clients
    public WebToAppChromeClient chromeClient;
    public WebToAppWebClient webClient;

    //WebView Session
    public String mainUrl = null;
    static String URL = "url";
    public int firstLoad = 0;
    private boolean clearHistory = false;

    public WebFragment() {
        // Required empty public constructor
    }

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public void setBaseUrl(String url){
        this.mainUrl = url;
        this.clearHistory = true;
        browser.loadUrl(mainUrl);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && mainUrl == null) {
            mainUrl = getArguments().getString(URL);
            firstLoad = 0;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rl = (FrameLayout) inflater.inflate(R.layout.fragment_observable_web_view, container,
                false);

        progressBar = (ProgressBar) rl.findViewById(R.id.progressbar);
        browser = (AdvancedWebView) rl.findViewById(R.id.scrollable);
        swipeLayout = (SwipeRefreshLayout) rl.findViewById(R.id.swipe_container);

        return rl;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (Config.PULL_TO_REFRESH)
            swipeLayout.setOnRefreshListener(this);
        else
            swipeLayout.setEnabled(false);

        // Setting the webview listeners
        browser.setListener(this, this);


        // set javascript and zoom and some other settings
        browser.requestFocus();
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setBuiltInZoomControls(false);
        browser.getSettings().setAppCacheEnabled(true);
        browser.getSettings().setDatabaseEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        // Below required for geolocation
        browser.setGeolocationEnabled(true);
        // 3RD party plugins (on older devices)
        browser.getSettings().setPluginState(PluginState.ON);

        if (Config.MULTI_WINDOWS) {
            browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            browser.getSettings().setSupportMultipleWindows(true);
        }

        webClient = new WebToAppWebClient(this, browser);
        browser.setWebViewClient(webClient);

        chromeClient = new WebToAppChromeClient(this, rl, browser, swipeLayout, progressBar);
        browser.setWebChromeClient(chromeClient);

        // load url (if connection available
        if (webClient.hasConnectivity(mainUrl, true)) {
           // String pushurl = ((App) getActivity().getApplication()).getPushUrl();
           // if (pushurl != null){
            //    browser.loadUrl(pushurl);
           // } else {
                browser.loadUrl(mainUrl);
          //  }
             }
        else {
            try {
                ((MainActivity) getActivity()).hideSplash();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRefresh() {
        browser.reload();
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
        super.onPause();
        browser.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        browser.onDestroy();
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        browser.onResume();
    }


    @SuppressLint("NewApi")
    @Override
    public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (!hasPermissionToDownload(getActivity())) return;

        String filename = null;
        try {
            filename = new GetFileInfo().execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (filename == null) {
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
            filename = URLUtil.guessFileName(url, null, fileExtenstion);
        }


        if (AdvancedWebView.handleDownload(getActivity(), url, filename)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.download_done), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getActivity(), getResources().getString(R.string.download_fail), Toast.LENGTH_SHORT).show();
        }
    }


       private static boolean hasPermissionToDownload(final Activity context) {
           if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                   ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                           == PackageManager.PERMISSION_GRANTED )
               return true;

           AlertDialog.Builder builder = new AlertDialog.Builder(context);
           builder.setMessage(R.string.download_permission_explaination);
           builder.setPositiveButton(R.string.common_permission_grant, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                   // Fire off an async request to actually get the permission
                   // This will show the standard permission request dialog UI
                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                       context.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                   }
               }
           });
           AlertDialog dialog = builder.create();
           dialog.show();

           return false;
       }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        if (firstLoad == 0 && MainActivity.getCollapsingActionBar()){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ((MainActivity) Objects.requireNonNull(getActivity())).showToolbar(this);
            }
            firstLoad = 1;
        } else if (firstLoad == 0){
            firstLoad = 1;
        }
    }

    @Override
    public void onPageFinished(String url) {

        try {
            ((MainActivity) getActivity()).hideSplash();
        } catch (Exception e){
            e.printStackTrace();
        }

        if (clearHistory)
        {
            clearHistory = false;
            browser.clearHistory();
        }

        hideErrorScreen();
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onExternalPageRequest(String url) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        browser.onActivityResult(requestCode, resultCode, data);
    }

    public void showErrorScreen(String message) {
        final View stub = rl.findViewById(R.id.empty_view);
        stub.setVisibility(View.VISIBLE);

        ((TextView) stub.findViewById(R.id.title)).setText(message);
        stub.findViewById(R.id.retry_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (browser.getUrl() == null) {
                    browser.loadUrl(mainUrl);
                } else {
                    browser.loadUrl("javascript:document.open();document.close();");
                    browser.reload();
                }
            }
        });
    }

    public void hideErrorScreen(){
        final View stub = rl.findViewById(R.id.empty_view);
        if (stub.getVisibility() == View.VISIBLE)
        stub.setVisibility(View.GONE);
    }
}
