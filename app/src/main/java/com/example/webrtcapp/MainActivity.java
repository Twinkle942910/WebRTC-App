package com.example.webrtcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.example.webrtcapp.helpers.Constants;

/**
 * TODO: Uncomment mPubNub instance variable
 */
public class MainActivity extends AppCompatActivity
{
    public static final int LAYOUT = R.layout.activity_main;

    private SharedPreferences mSharedPreferences;
    private TextView mUsernameTV;
    private EditText mCallNumET;
    // private Pubnub mPubNub;
    private String username;

    private Toolbar toolbar;

    /**
     * TODO: "Login" by subscribing to PubNub channel + Constants.SUFFIX
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(LAYOUT);

        initToolbar();

        this.mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        // Return to Log In screen if no user is logged in.
        if (!this.mSharedPreferences.contains(Constants.USER_NAME))
        {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        this.username = this.mSharedPreferences.getString(Constants.USER_NAME, "");

        this.mCallNumET  = (EditText) findViewById(R.id.call_num);
        this.mUsernameTV = (TextView) findViewById(R.id.main_username);

        this.mUsernameTV.setText(this.username);  // Set the username to the username text view

        //TODO: Create and instance of Pubnub and subscribe to standby channel
        // In pubnub subscribe callback, send user to your VideoActivity
    }

    private void initToolbar()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_settings:
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Log out, remove username from SharedPreferences, unsubscribe from PubNub, and send user back
     *   to the LoginActivity
     */
    public void signOut()
    {
        // TODO: Unsubscribe from all channels with PubNub object ( pn.unsubscribeAll() )
        SharedPreferences.Editor edit = this.mSharedPreferences.edit();
        edit.remove(Constants.USER_NAME);
        edit.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("oldUsername", this.username);
        startActivity(intent);
    }
}
