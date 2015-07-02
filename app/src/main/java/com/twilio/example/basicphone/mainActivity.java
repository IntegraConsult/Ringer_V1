/*
 *  Copyright (c) 2011 by Twilio, Inc., all rights reserved.
 *
 *  Use of this software is subject to the terms and conditions of 
 *  the Twilio Terms of Service located at http://www.twilio.com/legal/tos
 */

package com.twilio.example.basicphone;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.EditText;

import android.widget.TextView;

import com.twilio.example.basicphone.provider.BasicConnectionListener;
import com.twilio.example.basicphone.provider.BasicDeviceListener;
import com.twilio.example.basicphone.provider.LoginListener;

import org.json.JSONException;
import org.json.JSONObject;

public class mainActivity extends Activity implements LoginListener,
        BasicConnectionListener,
        BasicDeviceListener,
        View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String DEFAULT_CLIENT_NAME = "jenny";

    private static final Handler handler = new Handler();

    private provider phone;




    private EditText logTextBox;
    private AlertDialog incomingAlert;

    private String TAG = "Ringer";

    private String skinUrl = "file:///android_asset/skins/samsung/index.html";
    private String settingsUrl = "file:///android_asset/skins/samsung/settings.html";
    private String walletUrl = "file:///android_asset/skins/samsung/wallet.html";
    private TextView statusBar;
    private TextView userBar;

    private user phoneUser;
    private device mDevice;

    public void statusAlert(String message){
        this.statusBar.setTextColor(Color.RED);
        this.statusBar.setText(message);
    }
    public void statusMessage(String message) {
        //Log.d(TAG,"statusbar " + message);

        this.statusBar.setTextColor(Color.WHITE);

        if (message.equals("Ready")){
            if (phoneUser.capabilities.phoneInCapability.equals("yes")&& phoneUser.capabilities.phoneOutCapability.equals("yes")) {
                message = "Ready to call/receive";
            }
            else if (phoneUser.capabilities.phoneInCapability.equals("yes")){
                message = "Ready to receive";
            }
            else if (phoneUser.capabilities.phoneOutCapability.equals("yes")) {
                message = "Ready to call";
            }
            else {
                message ="Not authorized";
            }
        }
        this.statusBar.setText(message);
    }

    public void userMessage(String message) {
        //Log.d(TAG,"userBar " + message);
        this.userBar.setText(message);
    }

    public void showPage(int page) {
        mWebView.loadUrl("javascript: switch_to_page('" + page + "');");
    }

    public void showSettings() {
        mWebView.loadUrl(settingsUrl);

    }

    public void showUI() {
        mWebView.loadUrl(skinUrl);

    }

    public void showWallet() {
        mWebView.loadUrl(walletUrl);
    }


    private WebView mWebView;

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if ((!url.equals(skinUrl))&&(!url.equals(settingsUrl))) {
                view.goBack();
                scanUrl(url);
                return true;

            } else return false;

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //Log.d(TAG, "page finished loading: " + url);

            String javaScriptUrl;
            javaScriptUrl ="javascript: ";
            //javaScriptUrl += "list_contacts('" + phoneUser.contacts + "');";
            //javaScriptUrl += " setWallet(" + phoneUser.capabilities.wallet + ");";
            javaScriptUrl  +=" updateUI(" + phoneUser.capabilities.wallet + ",'" +phoneUser.contacts + "');";
            //Log.d(TAG,"javascript Url =" + javaScriptUrl);

            //phoneLine.phoneEvent("debug","got to onpagefinished event with list =" + javaScriptUrl);
            view.loadUrl(javaScriptUrl);

        }

        public boolean onConsoleMessage(ConsoleMessage cm) {
            Log.d(TAG, cm.message() + " -- From line "
                    + cm.lineNumber() + " of "
                    + cm.sourceId() );
            return true;
        }
    }

    // interprets an action sent from the webview
    private void scanUrl(String url) {
        int lastSlash;
        int end;
        String json, result, action;

        final JSONObject payload;

        result = java.net.URLDecoder.decode(url);
        //Log.d(TAG, "interpreting url" + result);
        // trim of everything from the url except the bit after last slash
        lastSlash = result.lastIndexOf('/');
        end = result.length();
        json = result.substring(lastSlash + 1, end);

        //Log.d(TAG, "interpreting json" + json);


        try {


            payload = new JSONObject(json);
            action = payload.getString("action");
            Log.d(TAG, "interpreting jsonobject.action: " + action);

            if (action.equals("key")) {
                String key = payload.getString("arguments");
                //Log.d(TAG, "key " +  key);
                mDevice.play(key);
            } else if (action.equals("call")) {
                String number = payload.getString("arguments");
                Log.d(TAG, "call " + number);
                if (!number.equals("")) {
                    if (phoneUser.capabilities.phoneOutCapability.equals("yes")) {
                        //sounds.playString(number);
                        Map<String, String> params = new HashMap<String, String>();
                        phoneUser.logCall(number);
                        number = "client:" + number;
                        params.put("To", number);

                        //phone.connect(params);
                    }
                    else {
                        statusAlert("not authorized to call");
                    }

                }

            } else if (action.equals("hangup")) {
                Log.d(TAG, "hangup ");
                phoneUser.logHangup();
                //phone.disconnect();
            } else if (action.equals("saveUser")) {
                final JSONObject mUser;
                mUser = payload.getJSONObject("arguments");
                String name = mUser.getString("name");
                String code = mUser.getString("code");
                String phone = mUser.getString("phone");

                Log.d(TAG, "save user " + name);


                phoneUser.saveSettings(name,code,phone);

                phoneUser.registerAtRinger();

            } else if (action.equals("phoneVolume")) {
                int volume = payload.getInt("arguments");
                Log.d(TAG, "volume " + volume);
                switch (volume) {
                    case 0:
                        phone.setCallMuted(true);
                        break;
                    case 1:
                        phone.setCallMuted(false);
                        phone.setSpeakerEnabled(false);
                        break;
                    case 2:
                        phone.setCallMuted(false);
                        phone.setSpeakerEnabled(true);
                        break;
                }
            } else if (action.equals("updateWallet")){
                final JSONObject transaction;
                transaction = payload.getJSONObject("arguments");
                Double amount = transaction.getDouble("amount");
                String ccType = transaction.getString("ccType");
                String ccNumber = transaction.getString("ccNumber");
                String expirationMonth = transaction.getString("expirationMonth");
                String expirationYear = transaction.getString("expirationYear");
                String securityCode = transaction.getString ("securityCode");
                String nameOnCard = transaction.getString("nameOnCard");


                phoneUser.saveTransaction(amount, ccType, ccNumber,expirationMonth,expirationYear,securityCode,nameOnCard);

                phoneUser.updateWallet();

            }

        } catch (JSONException e) {
            Log.e(TAG, " main Activity parseUrl: json parsing error");
        }



    }

    public void providerLogin (){
        //phone.login(clientNameTextBox.getText().toString(),
        //        outgoingCheckBox.isChecked(),
        //        incomingCheckBox.isChecked());
        if (phoneUser.capabilities.phoneInCapability.equals("yes")||phoneUser.capabilities.phoneOutCapability.equals("yes") ) {
            phone.login(DEFAULT_CLIENT_NAME,
                    phoneUser.capabilities.phoneOutCapability.equals("yes"),
                    phoneUser.capabilities.phoneInCapability.equals("yes"));
        }
        else {
            //in case the user has no capabilities make sure that a login occurs to shut of any remainimg call in capabilities
            // the call out function in that case has already been blocked inside the scanURL function
            // (when the user presses the call button)
            phone.login(DEFAULT_CLIENT_NAME,true,false);


        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);




        logTextBox = (EditText) findViewById(R.id.log_text_box);


        // status bar
        statusBar = (TextView) findViewById(R.id.textView);
        userBar = (TextView) findViewById(R.id.userText);
        statusMessage("initialising Ringer....");


        mDevice = new device(this);

        //create a new phone user
        phoneUser = new user(this,mainActivity.this);

        //load the UI
        mWebView = (WebView) findViewById(R.id.webView);
        // make sure that the default browser is not invoked when a link is clicked
        mWebView.setWebViewClient(new MyWebViewClient());
        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //mWebView.loadUrl(skinUrl);

        phone = provider.getInstance(getApplicationContext());

        phone.setListeners(this, this, this);

        //providerUpdateCapabilities();


        //auto login phone user intop Ringer
        // this login call causes the following state changes
           // settings file is read and phoneUser.settings are initialized during ringer login
           // if login resuòlt is OK phoneUser.capabilities are set, based on the login response
           // if capabilities havce changed (always the case in the firts login after creation)
           // a login to the provider is done
        phoneUser.loginAtRinger();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (phone.handleIncomingIntent(getIntent())) {
            showIncomingAlert();
            addStatusMessage(R.string.got_incoming);
            syncMainButton();
        }
        phoneUser.loginAtRinger();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        // check if there are any pending userActions
        if (phoneUser.currentAction.action.equals("call")) {
            phoneUser.logEndConnectionActivity();

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (phone != null) {
            phone.setListeners(null, null, null);
            phone = null;
        }
    }

    @Override
    public void onClick(View view) {

    }



    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {

    }

    private void addStatusMessage(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                logTextBox.append('-' + message + '\n');
            }
        });
    }

    private void addStatusMessage(int stringId) {
        addStatusMessage(getString(stringId));
        //statusMessage(getString(stringId));
    }

    private void uiShowState(String state) {
        mWebView.loadUrl("javascript: show_state('" + state + "');");
    }

    private void syncMainButton() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (phone.isConnected()) {
                    switch (phone.getConnectionState()) {
                        case DISCONNECTED:
                            phoneUser.logDisconnected();
                            statusMessage("Disconnected");
                            uiShowState("disconnected");
                            break;
                        case CONNECTED:
                            phoneUser.logConnected();
                            statusMessage("Connected");
                            uiShowState("Connected");
                            break;
                        default:
                            phoneUser.logDialing();
                            statusMessage("Dialing");
                            uiShowState("dialing");
                            break;
                    }
                } else if (phone.hasPendingConnection()) {
                    statusMessage("Pending connection");
                    uiShowState("pending");
                } else {
                    statusMessage("Ready");
                    uiShowState("ready");

                }
            }
        });
    }

    private void showIncomingAlert() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (incomingAlert == null) {
                    incomingAlert = new AlertDialog.Builder(mainActivity.this)
                            .setTitle(R.string.incoming_call)
                            .setMessage(R.string.incoming_call_message)
                            .setPositiveButton(R.string.answer, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    phone.acceptConnection();
                                    incomingAlert = null;
                                }
                            })
                            .setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    phone.ignoreIncomingConnection();
                                    incomingAlert = null;
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    phone.ignoreIncomingConnection();
                                }
                            })
                            .create();
                    incomingAlert.show();
                }
            }
        });
    }

    private void hideIncomingAlert() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (incomingAlert != null) {
                    incomingAlert.dismiss();
                    incomingAlert = null;
                }
            }
        });
    }

    @Override
    public void onLoginStarted() {
        addStatusMessage(R.string.logging_in);
    }

    @Override
    public void onLoginFinished() {
        addStatusMessage(phone.canMakeOutgoing() ? R.string.outgoing_ok : R.string.no_outgoing_capability);
        addStatusMessage(phone.canAcceptIncoming() ? R.string.incoming_ok : R.string.no_incoming_capability);
        syncMainButton();
    }

    @Override
    public void onLoginError(Exception error) {
        if (error != null)
            addStatusMessage(String.format(getString(R.string.login_error_fmt), error.getLocalizedMessage()));
        else
            addStatusMessage(R.string.login_error_unknown);
        syncMainButton();
    }

    @Override
    public void onIncomingConnectionDisconnected() {
        hideIncomingAlert();
        addStatusMessage(R.string.incoming_disconnected);
        syncMainButton();
    }

    @Override
    public void onConnectionConnecting() {
        addStatusMessage(R.string.attempting_to_connect);
        syncMainButton();
    }

    @Override
    public void onConnectionConnected() {
        addStatusMessage(R.string.connected);
        syncMainButton();
    }

    @Override
    public void onConnectionFailedConnecting(Exception error) {
        if (error != null)
            addStatusMessage(String.format(getString(R.string.couldnt_establish_outgoing_fmt), error.getLocalizedMessage()));
        else
            addStatusMessage(R.string.couldnt_establish_outgoing);
    }

    @Override
    public void onConnectionDisconnecting() {
        addStatusMessage(R.string.disconnect_attempt);
        syncMainButton();
    }

    @Override
    public void onConnectionDisconnected() {
        addStatusMessage(R.string.disconnected);
        syncMainButton();
    }

    @Override
    public void onConnectionFailed(Exception error) {
        if (error != null)
            addStatusMessage(String.format(getString(R.string.connection_error_fmt), error.getLocalizedMessage()));
        else
            addStatusMessage(R.string.connection_error);
        syncMainButton();
    }

    @Override
    public void onDeviceStartedListening() {
        addStatusMessage(R.string.device_listening);
    }

    @Override
    public void onDeviceStoppedListening(Exception error) {
        if (error != null)
            addStatusMessage(String.format(getString(R.string.device_listening_error_fmt), error.getLocalizedMessage()));
        else
            addStatusMessage(R.string.device_not_listening);
    }

}
