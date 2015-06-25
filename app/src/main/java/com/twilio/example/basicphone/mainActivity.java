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
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
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
        CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener {
    private static final String DEFAULT_CLIENT_NAME = "jenny";

    private static final Handler handler = new Handler();

    private provider phone;
    private ringer account;



    private ImageButton mainButton;
    private EditText logTextBox;
    private AlertDialog incomingAlert;
    private RadioGroup inputSelect;
    private EditText outgoingTextBox;
    private EditText clientNameTextBox;
    private Button capabilitesButton;
    private CheckBox incomingCheckBox, outgoingCheckBox;

    private String TAG = "Ringer";
    private String contactJson = "";
    private static String prefix = "+39";
    private String skinUrl = "file:///android_asset/skins/samsung/index.html";

    private TextView statusBar;
    private TextView userBar;

    private user phoneUser;
    private device mDevice;


    public void statusMessage(String message) {
        //Log.d(TAG,"statusbar " + message);
        this.statusBar.setText(message);
    }

    public void userMessage(String message) {
        //Log.d(TAG,"userBar " + message);
        this.userBar.setText(message);
    }

    public void showPage(int page) {
        mWebView.loadUrl("javascript: show_page('" + page + "')");
    }


    private WebView mWebView;

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != skinUrl) {
                view.goBack();
                scanUrl(url);
                return true;

            } else return false;

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "page finished loading: " + url);

            String javaScriptUrl;
            javaScriptUrl = "javascript:";
            javaScriptUrl += "list_contacts('" + phoneUser.contacts + "');";
            if (phoneUser.settings.uuid.equals("")) {
                // show the settings page
                javaScriptUrl += "switch_to_page(5);";
            } else {
                // show the dialpad page
                javaScriptUrl += "switch_to_page(0);";
            }

            //phoneLine.phoneEvent("debug","got to onpagefinished event with list =" + javaScriptUrl);
            view.loadUrl(javaScriptUrl);

        }
    }

    // interprets an action sent from the webview
    private void scanUrl(String url) {
        int lastSlash;
        int end;
        String json, result, action;

        final JSONObject payload;
        JSONException exception;
        result = java.net.URLDecoder.decode(url);
        //Log.d(TAG, "interpreting url" + result);
        // trim of everything from the url except the bit after last slash
        lastSlash = result.lastIndexOf('/');
        end = result.length();
        json = result.substring(lastSlash + 1, end);

        //Log.d(TAG, "interpreting json" + json);


        action = "";
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
                    //sounds.playString(number);
                    Map<String, String> params = new HashMap<String, String>();
                    number = "client:" + number;
                    params.put("To", number);
                    phone.connect(params);

                }

            } else if (action.equals("hangup")) {
                Log.d(TAG, "hangup ");
                phone.disconnect();
            } else if (action.equals("saveUser")) {
                final JSONObject mUser;
                mUser = payload.getJSONObject("arguments");
                Log.d(TAG, "save user " + mUser.getString("name"));
                phoneUser.saveSettings(
                        mUser.getString("name"),
                        mUser.getString("code"),
                        mUser.getString("phone"),
                        mUser.getString("phone") +
                                mUser.getString("name") +
                                mUser.getString("code"));
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
            }
        } catch (JSONException e) {
            Log.e(TAG, "json parsing error");
        }
        ;


    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mainButton = (ImageButton) findViewById(R.id.main_button);
        mainButton.setOnClickListener(this);

        logTextBox = (EditText) findViewById(R.id.log_text_box);
        outgoingTextBox = (EditText) findViewById(R.id.outgoing_client);
        clientNameTextBox = (EditText) findViewById(R.id.client_name);
        clientNameTextBox.setText(DEFAULT_CLIENT_NAME);
        capabilitesButton = (Button) findViewById(R.id.capabilites_button);
        capabilitesButton.setOnClickListener(this);
        outgoingCheckBox = (CheckBox) findViewById(R.id.outgoing);
        incomingCheckBox = (CheckBox) findViewById(R.id.incoming);
        inputSelect = (RadioGroup) findViewById(R.id.input_select);
        inputSelect.setOnCheckedChangeListener(this);


        // status bar
        statusBar = (TextView) findViewById(R.id.textView);
        userBar = (TextView) findViewById(R.id.userText);
        statusMessage("initialising Ringer....");


        mDevice = new device(this);


        Log.d(TAG,"Create user");
        phoneUser = new user(this,mainActivity.this);
        phoneUser.loginAtRinger();


        phone = provider.getInstance(getApplicationContext());

        phone.setListeners(this, this, this);

        phone.login(clientNameTextBox.getText().toString(),
                outgoingCheckBox.isChecked(),
                incomingCheckBox.isChecked());


        //load the UI
        mWebView = (WebView) findViewById(R.id.webView);
        // make sure that the default browser is not invoked when a link is clicked
        mWebView.setWebViewClient(new MyWebViewClient());
        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.loadUrl(skinUrl);

        userMessage(phoneUser.settings.name);

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
    public void onDestroy() {
        super.onDestroy();

        if (phone != null) {
            phone.setListeners(null, null, null);
            phone = null;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.main_button) {
            if (!phone.isConnected()) {
                Map<String, String> params = new HashMap<String, String>();
                if (outgoingTextBox.getText().length() > 0) {
                    String number = outgoingTextBox.getText().toString();
                    if (inputSelect.getCheckedRadioButtonId() == R.id.input_text) {
                        number = "client:" + number;
                    }
                    params.put("To", number);
                }
                phone.connect(params);
            } else
                phone.disconnect();
        } else if (view.getId() == R.id.capabilites_button) {
            phone.disconnect();
            phone.login(clientNameTextBox.getText().toString(),
                    outgoingCheckBox.isChecked(),
                    incomingCheckBox.isChecked());
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.getId() == R.id.input_select) {
            if (checkedId == R.id.input_number) {
                outgoingTextBox.setInputType(InputType.TYPE_CLASS_PHONE);
                outgoingTextBox.setHint(R.string.outgoing_number);
            } else {
                outgoingTextBox.setInputType(InputType.TYPE_CLASS_TEXT);
                outgoingTextBox.setHint(R.string.outgoing_client);
            }
            outgoingTextBox.setText("");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        /*
        if (button.getId() == R.id.speaker_toggle) {
            phone.setSpeakerEnabled(isChecked);
        } else if (button.getId() == R.id.mute_toggle){
        	phone.setCallMuted(isChecked);
        }
        */
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
                            mainButton.setImageResource(R.drawable.idle);
                            statusMessage("Disconnected");
                            uiShowState("disconnected");
                            break;
                        case CONNECTED:
                            mainButton.setImageResource(R.drawable.inprogress);
                            statusMessage("Connected");
                            uiShowState("Connected");
                            break;
                        default:
                            mainButton.setImageResource(R.drawable.dialing);
                            statusMessage("Dialing");
                            uiShowState("dialing");
                            break;
                    }
                } else if (phone.hasPendingConnection()) {
                    mainButton.setImageResource(R.drawable.dialing);
                    statusMessage("Pending connection");
                    uiShowState("pending");
                } else {
                    mainButton.setImageResource(R.drawable.idle);
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
