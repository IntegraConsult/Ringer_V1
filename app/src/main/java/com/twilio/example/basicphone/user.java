package com.twilio.example.basicphone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by admin on 22/06/2015.
 */
class userSettings implements Serializable {
    String uuid = "";
    String code = "";
    String name = "";
    String phone = "";
}

public class user {
    private Context mainContext;

    public userSettings settings = new userSettings();


    public String contacts    = "" ;
    public String prefix      = "+39";

    private ContentResolver phoneContacts;
    private String TAG = "Ringer" ;


    public user (Context context) {
        String localSettings = "";
        this.mainContext = context ;
        this.phoneContacts = context.getContentResolver();
        if (this.contacts.equals("")) {
            this.contacts = updateContacts();
        }

        //  get user the settings from file
        readSettings();



    }
    public void readSettings() {
        try
        {
            FileInputStream fileIn = new FileInputStream("settings");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            this.settings = (userSettings) in.readObject();
            in.close();
            fileIn.close();
        }catch(IOException i)
        {
            Log.d (TAG,"IO error while reading settings, probably file notfound");
            //i.printStackTrace();
            return;
        }catch(ClassNotFoundException c)
        {
            Log.d(TAG," userSettings  class not found");
            c.printStackTrace();
            return;
        }
        Log.d(TAG,"Deserialized usersettings...");
        Log.d(TAG, "name: " + this.settings.name);
        Log.d(TAG, "code: " + this.settings.code);
        Log.d(TAG, "phone: " + this.settings.phone);
        Log.d(TAG, "uuid: " + this.settings.uuid);

    }

    public void saveSettings (String name, String code, String phone, String uuid) {
        userSettings mSettings = new userSettings();
        mSettings.name = name;
        mSettings.code = code;
        mSettings.phone = phone;
        mSettings.uuid = uuid;
        try {

            FileOutputStream fos = this.mainContext.openFileOutput("settings", Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(mSettings);
            out.close();
            fos.close();

        }
        catch (FileNotFoundException ef) {
            Log.d(TAG,"file not found");
        }
        catch (IOException ei) {
            Log.d(TAG,"java io error" );
            ei.printStackTrace();
        }

    }

    public String register(){
        String uuid;
        uuid ="something crazy";
        this.settings.name = "some wild name";
        return uuid;

    }
    public String unegister () {
        return "";
    }
    public String updateContacts(){
        Cursor mCursor;
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String SORT = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC";
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String WHERE_CLAUSE = HAS_PHONE_NUMBER + " > 0";
        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
        Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA;
        String contactName, contactEmail, contactPhone;
        int contactCount = 0 ;
        String contactJson = "";
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
        }
        else {
            Log.d(TAG, "SQLLite " + mCursor.getCount() + " matches");
            contactJson="[";
            while (mCursor.moveToNext()) {
                contactName = contactEmail = contactPhone = "";
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
    /*
                        // Query and loop for every email of the contact
                        Cursor emailCursor = phoneContacts.query(EmailCONTENT_URI, null, EmailCONTACT_ID + " = ?", new String[]{contact_id}, null);
                        while (emailCursor.moveToNext()) {
                            contactEmail = emailCursor.getString(emailCursor.getColumnIndex(DATA));
                        }
                        emailCursor.close();
   */
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
            //skip last ,
            contactJson =contactJson.substring(0,contactJson.length() -1);
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
