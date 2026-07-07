package com.augustoprojetos.backlogapi;

import android.os.Bundle;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

public class MainActivity extends BridgeActivity {

    private static final String OFFLINE_PAGE = "file:///android_asset/public/offline.html";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.bridge.getWebView().setWebViewClient(new BridgeWebViewClient(this.bridge) {

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadUrl(OFFLINE_PAGE);
                    return;
                }
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                // Cobre também o caso do backend responder 5xx (ex: acordando no Render)
                if (request.isForMainFrame() && errorResponse.getStatusCode() >= 500) {
                    view.loadUrl(OFFLINE_PAGE);
                    return;
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }
        });
    }
}
