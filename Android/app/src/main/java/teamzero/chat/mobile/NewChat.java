package teamzero.chat.mobile;

import android.content.Intent;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.List;

import database.AppDatabaseClient;
import database.StoredChatList;
import database.UsersOnDevice;

import tools.DHUtilities;
import tools.JSONConstructor;
import tools.RSAUtilities;

import users.data.OtherUsersData;

public class NewChat extends AppCompatActivity {

    TextView suggestiveTextTextView;
    FloatingActionButton searchUsersBtn;
    EditText searchUsersEditText;
    ListView usersList;
    List<OtherUsersData> usersFoundList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_chat_layout);

        // Check if the SupportActionBar is instantiated
        assert getSupportActionBar() != null;
        // If it is, display a back button on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        suggestiveTextTextView = (TextView) findViewById(R.id.searchUsersSuggestiveTextView);
        searchUsersBtn = (FloatingActionButton) findViewById(R.id.SearchUsersButton);
        searchUsersEditText = (EditText) findViewById(R.id.SearchUsersEditText);
        usersList = (ListView) findViewById(R.id.displayUsersFoundList);

        searchUsersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String searchForUser = searchUsersEditText.getText().toString();

                // If the user clicks on the search button without text input, send GETALLCONTACTS request JSON
                if(searchForUser.isEmpty()) {

                    try {
                        System.out.println(new JSONConstructor().constructGetAllContactsJSON());
                        WebSocketHandler.getSocket().sendMessageAndWait(new JSONConstructor().constructGetAllContactsJSON());

                        //Thread.sleep(500);

                        // Get response from server and parse it
                        JSONObject responseJSON = new JSONObject(WebSocketHandler.getSocket().getResponse());

                        if (responseJSON.get("REPLY").toString().equalsIgnoreCase("GETALLCONTACTS: SUCCESS")) {

                            if(responseJSON.has("contacts")) {
                                addUsersFoundToList(responseJSON);
                            }
                            else Toast.makeText(getApplicationContext(), R.string.search_all_contacts_not_found_text, Toast.LENGTH_LONG).show();

                        }
                        else Toast.makeText(getApplicationContext(), R.string.fetching_from_server_error_text, Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    // If the user gives text input, send SEARCHCONTACTS request JSON
                    try {
                        System.out.println(new JSONConstructor().constructSearchContactsJSON(searchForUser));
                        WebSocketHandler.getSocket().sendMessageAndWait(new JSONConstructor().constructSearchContactsJSON(searchForUser));

                        //Thread.sleep(500);

                        // Get response from server and parse it
                        JSONObject responseJSON = new JSONObject(WebSocketHandler.getSocket().getResponse());
                        if (responseJSON.get("REPLY").toString().equalsIgnoreCase("SEARCHCONTACTS: SUCCESS")) {

                            if(responseJSON.has("contacts")) {
                                addUsersFoundToList(responseJSON);
                            }
                            else Toast.makeText(getApplicationContext(), R.string.search_contact_not_found_text, Toast.LENGTH_LONG).show();

                        }
                        else Toast.makeText(getApplicationContext(), R.string.fetching_from_server_error_text, Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        usersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                System.out.println("-- POST-ENCODING --");

                // print for testing purposes
                System.out.println(usersFoundList.get(position).getUsername());
                System.out.println(usersFoundList.get(position).getPublicKey());

                String getPublicKeyB64String = usersFoundList.get(position).getPublicKey();

                System.out.println("public key in base64 = " + getPublicKeyB64String);


                addUserToStoredChatList(usersFoundList.get(position).getUsername(), getPublicKeyB64String);
                finish();
            }
        });
    }

    private void addUsersFoundToList(JSONObject contactList) throws JSONException {

        // First try to get an array of objects (users) from the database
        try {
            JSONArray jsonArr = contactList.getJSONArray("contacts");

            System.out.println(jsonArr);

            // Clear the List of any previously searched-for data
            usersFoundList.clear();

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject x = jsonArr.getJSONObject(i);

                OtherUsersData oud = new OtherUsersData();
                oud.setUsername(x.get("username").toString());
                oud.setStatus(x.get("IsLoggedIn").toString().equalsIgnoreCase("true") ? "online" : "offline");

                // If the public key exists, set it explicitly. Else, default will be empty string
                if(x.has("publicKey"))
                    oud.setPublicKey(x.get("publicKey").toString());
                else oud.setPublicKey("");

                //System.out.println(oud.getUsername() + " = " + oud.getPublicKey());

                usersFoundList.add(oud);
            }
        } catch(JSONException e) {
            // If there is only one user found, returned JSON will not contain '[]'
            // and therefore will not be an array, so it will be catched here

            // Clear the List of any previously searched-for data
            usersFoundList.clear();

            JSONObject x = contactList.getJSONObject("contacts");

            OtherUsersData oud = new OtherUsersData();
            oud.setUsername(x.get("username").toString());
            oud.setStatus(x.get("IsLoggedIn").toString().equalsIgnoreCase("true") ? "online" : "offline");

            if(x.has("publicKey"))
                oud.setPublicKey(x.get("publicKey").toString());
            else oud.setPublicKey("");

            usersFoundList.add(oud);
        }

        displayChatList();
    }

    public void displayChatList() {

        suggestiveTextTextView.setVisibility(View.GONE);
        usersList.setVisibility(View.VISIBLE);

        // Overridden the getView of ArrayAdapter in order to access both text1 and text2 (from simple_list_item_2) and write on them right away
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, usersFoundList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                // If the user is online, show the status in green, else in red
                text2.setTextColor(usersFoundList.get(position).getStatus().equalsIgnoreCase("online") ? Color.parseColor("#0fd443") : Color.parseColor("#d42c0f"));

                text1.setText(usersFoundList.get(position).getUsername());
                text2.setText(usersFoundList.get(position).getStatus());
                return view;
            }
        };
        usersList.setAdapter(adapter);

    }

    private void addUserToStoredChatList(final String searchUsersEditTextString, final String publicKeyb64OfUser) {

        // TODO: Add the user only if it does not exist already in local database

        class AddUserToStoredChatList extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {

                StoredChatList scl = new StoredChatList();
                scl.setUsername(searchUsersEditTextString);
                scl.setLastMessageContent("Last message here");

                // TODO: Debugging purposes only -- Will delete the 'if' and leave only what's in 'else'
                if(publicKeyb64OfUser.isEmpty()) {
                    scl.setPublicKey("BLANK");
                    scl.setSharedSecretKey("2646294A404E635266556A576E5A7234");
                }
                else {
                    scl.setPublicKey(publicKeyb64OfUser);

                    /* Start shared secret key computation */

                    //Step 1: Get DHPublicKey object of other user

                    PublicKey publicKeyOfUser = DHUtilities.computeDHPublicKeyfromBase64String(publicKeyb64OfUser);

                    //Step 2: Retrieve myPrivateKey and compute DHPrivateKey

                    String myPrivateKeyStr = AppDatabaseClient.getInstance(getApplicationContext()).getAppDatabase().usersOnDeviceDao().getUserPrivateKey(UserDetails.username);
                    System.out.println("myPrivateKeyStr: " + myPrivateKeyStr);
                    PrivateKey myPrivateKey = DHUtilities.computeDHPrivateKeyfromBase64String(myPrivateKeyStr);

                    // Case here:  initiating chat myself
                    // Step 3 : compute shared key

                    byte[] sharedKey = new byte[]{};
                    try {
                        sharedKey = DHUtilities.initiatorAgreementBasic(myPrivateKey, publicKeyOfUser);
                    } catch (GeneralSecurityException e) {
                        System.out.println("Could not compute SharedKey");
                        e.printStackTrace();
                    }

                    // TODO: This part directly takes the hardcoded DH key
                    String sharedPassphrase = DHUtilities.generateSharedKey(publicKeyb64OfUser, myPrivateKeyStr);

                    System.out.println("sharedPassphrase = " + sharedPassphrase);

                    //test
                    System.out.println(new String(sharedKey));

                    /*end shared secret key compute */

                    // set the shared key in stored chat list details as a String
                    // TODO: Set to sharedKey instead (use next line to the following one)
                    scl.setSharedSecretKey(sharedPassphrase);
                    //scl.setSharedSecretKey(new String(sharedKey));
                }

                scl.setChatBelongsTo(UserDetails.username);

                // Add the user into the local chat list database
                AppDatabaseClient.getInstance(getApplicationContext()).getAppDatabase()
                        .storedChatListDao()
                        .insert(scl);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                finish();
                //startActivity(new Intent(getApplicationContext(), ChatList.class));
                Toast.makeText(getApplicationContext(), "Chat created", Toast.LENGTH_LONG).show();
            }
        }

        AddUserToStoredChatList st = new AddUserToStoredChatList();
        st.execute();
    }

    // Finishes current activity (dismisses dialogs, closes search) and goes to the parent activity
    // The method is called when the user clicks on the back button on the upper-left hand side
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
