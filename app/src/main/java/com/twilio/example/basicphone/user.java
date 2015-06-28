package com.twilio.example.basicphone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


class userSettings implements Serializable {
    String code = "";
    String name = "";
    String phone = "";
}

class userCapabilities {
    String phoneInCapability = "";
    String phoneOutCapability = "";

}

public class user {
    private Context mainContext;
    private mainActivity mainAct;

    public userSettings settings = new userSettings();
    public userCapabilities capabilities= new userCapabilities();

    //public String ringerLoginUrl ="http://twilio/unapps.net/login.php";
    //public String ringerRegisterUrl ="http://twilio/unapps.net/register.php";
    public String ringerServiceUrl ="http://192.168.1.104/ringer/service.php";


    public String contacts = "";
    public String prefix = "+39";

    private ContentResolver phoneContacts;
    private String TAG = "Ringer";
    private webConnection web ;

    final static int ACTION_ERROR = 0;
    final static int ACTION_LOGIN = 1;
    final static int ACTION_REGISTER = 2;
    final static int ACTION_SET_CAPABILITIES = 3;
    final static int ACTION_REGISTRATION_OK = 4 ;





    public user(Context context, mainActivity act) {

        this.mainContext = context;
        this.mainAct = act;
        web = new webConnection(this);
        this.phoneContacts = context.getContentResolver();
        if (this.contacts.equals("")) {
            this.contacts = updateContacts();
        }
    }

    public void handleWebResponse(String webResponse) {
        JSONObject response ;
        String phoneInCapability;
        String phoneOutCapability;

        Log.d(TAG,"handling web response" + webResponse);
        try {
            response = new JSONObject(webResponse);
            int action = response.getInt("action");
            switch (action) {
                case ACTION_SET_CAPABILITIES:
                    Log.d(TAG,"login status OK");
                    JSONObject parameters = response.getJSONObject("parameters");
                    phoneOutCapability = parameters.getString("phoneOutCapability");
                    phoneInCapability = parameters.getString("phoneInCapability");
                    if ( !( phoneInCapability.equals(this.capabilities.phoneOutCapability) &&
                            phoneOutCapability.equals(this.capabilities.phoneOutCapability)
                    )
                            ){
                        // aparently user capabilities have changed since last login
                        Log.d(TAG, "capabilities have changed");
                        this.capabilities.phoneOutCapability = phoneOutCapability;
                        this.capabilities.phoneInCapability = phoneInCapability;

                        mainAct.providerLogin();
                    }

                    this.capabilities.phoneOutCapability = phoneOutCapability;
                    this.capabilities.phoneInCapability = phoneInCapability;
                    Log.d(TAG,"capabilities set to phonein:" +this.capabilities.phoneInCapability + " phoneout:" + this.capabilities.phoneOutCapability);

                    // continue with phone processing
                    mainAct.showUI();
                    mainAct.userMessage(settings.name);
                    mainAct.providerLogin();
                    break;
                case ACTION_ERROR :
                    String message = response.getString("parameters");
                    Log.e(TAG, message);
                    break;
                case ACTION_REGISTER:
                    mainAct.showSettings();
                    //registerAtRinger();
                    break;
                case ACTION_REGISTRATION_OK :
                    Log.d(TAG,"regsitered successfully");
                    mainAct.showUI();
                    loginAtRinger();
                    break;
                default:
                    Log.e(TAG,"web response error : unkonwn action " + action);
                    break;
            }

        }
        catch (JSONException e){
            Log.e(TAG,"handle web response JSON error");

        }


    }

    public void loginAtRinger () {
        Log.d(TAG,"Log into ringer");
        //get user settings
        readSettings();

        //check length of code
        if (settings.code.length() != 4) {
            //show page 5 contains:
                 //on save.click :
                    //save user.settings to file
                    // register at ringer
            mainAct.showSettings();

        } else {
            //Log.d(TAG,"login at Ringer code passed first inspection");
            // encrypt settings
            JSONObject payload = new JSONObject();
            try {
                payload.put("action",ACTION_LOGIN);
                payload.put("code",this.settings.code);
                payload.put("phone",this.settings.phone);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String encryptedPayload = encrypt(payload.toString());
            web.get(ringerServiceUrl, encryptedPayload);
            // web.get will invoke handle webresponse inside the parent class;
        }


    }

    public void registerAtRinger() {
        readSettings();
        //encrypt settings
        JSONObject payload = new JSONObject();
        try {
            payload.put("action",ACTION_REGISTER);
            payload.put("code",this.settings.code);
            payload.put("phone",this.settings.phone);
            payload.put("name",this.settings.name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String encryptedPayload = encrypt(payload.toString());
        //doo http request call Ringerregistration?q=encryptedjson
        //3.on response loginAtRinger
        web.get(ringerServiceUrl, encryptedPayload);

    }

    public String  encrypt (String arg) {
       String encryptedSettings;
       //Log.d(TAG,"settingsString  is " + settingsString);
       //encrypt json
       encryptedSettings = arg;
       return encryptedSettings;
    }


    public void readSettings() {
        try {
            FileInputStream fileIn = this.mainContext.openFileInput("settings.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            this.settings = (userSettings) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception i) {
            Log.e(TAG, "IO error while reading settings");
            //i.printStackTrace();

        }
/**/
        Log.d(TAG, "Deserialized usersettings...");
        Log.d(TAG, "name: " + this.settings.name);
        Log.d(TAG, "code: " + this.settings.code);
        Log.d(TAG, "phone: " + this.settings.phone);
/**/
    }

    public void saveSettings(String name, String code, String phone) {
        userSettings mSettings = new userSettings();
        mSettings.name = name;
        mSettings.code = code;
        mSettings.phone = phone;
        try {

            FileOutputStream fos = this.mainContext.openFileOutput("settings.ser", Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(mSettings);
            out.close();
            fos.write(mSettings.name.getBytes());
            fos.close();

        } catch (IOException ei) {
            Log.e(TAG, "java io error");
            ei.printStackTrace();
        } catch (Exception any) {
            Log.e(TAG, "weird write error");

        }

        // just to check if all is OK
        readSettings();

    }


    public String updateContacts() {
        Cursor mCursor;
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String SORT = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC";
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String WHERE_CLAUSE = HAS_PHONE_NUMBER + " > 0";
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        String contactName, contactPhone;
        String contactJson ;
        mCursor = phoneContacts.query(

                CONTENT_URI,  // The content URI of the words table: FROM table_name
                null,         // The columns to return :col,col,col,...
                WHERE_CLAUSE,         // Selection criteria: WHERE col = value
                null,         // Selection criteria: (No exact equivalent. Selection arguments replace ? placeholders in the selection clause.)
                SORT          // The sort order for the returned rows: ORDER BY col,col,...
        );

        if (mCursor == null) {
            return "";
        }
        if (mCursor.getCount() < 1) {
            Log.d(TAG, "SQLLite no matches");
            return "";
        } else {
            Log.d(TAG, "SQLLite " + mCursor.getCount() + " matches");
            contactJson = "[";
            while (mCursor.moveToNext()) {
                contactPhone = "";
                // Gets the value from the column.
                //Log.d(TAG,"iterate result");
                String contact_id = mCursor.getString(mCursor.getColumnIndex(_ID));
                contactName = mCursor.getString(mCursor.getColumnIndex(DISPLAY_NAME));

                int hasPhoneNumber = Integer.parseInt(mCursor.getString(mCursor.getColumnIndex(HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {

                    // Query and loop for every phone number of the contact
                    Cursor phoneCursor = phoneContacts.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);
                    while (phoneCursor.moveToNext()) {
                        contactPhone = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                    }
                    phoneCursor.close();

                }
                if (!contactPhone.equals("")) {
                    // purify phone numbers
                    // change first 00 into +
                    String first2Characters = contactPhone.substring(0, Math.min(contactPhone.length(), 2));
                    if (first2Characters.equals("00")) {
                        //slice them off
                        contactPhone = "+" + contactPhone.substring(2, contactPhone.length());
                    } else {
                        String firstCharacter = contactPhone.substring(0, Math.min(contactPhone.length(), 1));
                        if (!firstCharacter.equals("+")) {
                            contactPhone = prefix + contactPhone;
                        }
                    }
                    // new entry
                    contactJson += "{\"name\":\"" + contactName + "\",\"phoneNumber\":\"" + contactPhone + "\"},";

                }


            }

            // end of while loop
            mCursor.close();
            //skip last ,
            contactJson = contactJson.substring(0, contactJson.length() - 1);
            contactJson += "]";

        }
        return contactJson;
        /*
        return "[" +
                "{\"name\":\"Ad Langenkamp\",\"phoneNumber\":\"00393336948230\"}," +
                "{\"name\":\"Els Vink\",\"phoneNumber\":\"00393272281874\"}" +
                "]";
                */
    }
}
