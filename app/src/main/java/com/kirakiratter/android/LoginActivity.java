package com.kirakiratter.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import jastodon.Apps;

/**
 * @author Southrop
 */

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private SharedPreferences preferences;

    private String domain = null;
    private String clientId = null;
    private String clientSecret = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (savedInstanceState != null) {
            domain = savedInstanceState.getString("domain");
            clientId = savedInstanceState.getString("clientId");
            clientSecret = savedInstanceState.getString("clientSecret");
        }

        preferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("domain", domain);
        outState.putString("clientId", clientId);
        outState.putString("clientSecret", clientSecret);

        super.onSaveInstanceState(outState);
    }

    public void login(View view) {
        EditText textField = (EditText) findViewById(R.id.edittext_domain);
        String domain = sanitiseDomain(textField.getText().toString());

        String storedClientId = preferences.getString(BuildConfig.APPLICATION_ID + "_" +
                domain + "_clientId", null);
        String storedClientSecret = preferences.getString(BuildConfig.APPLICATION_ID + "_" +
                domain + "_clientSecret", null);

        if (storedClientId != null && storedClientSecret != null) {
            Log.d(TAG, "Found stored client tokens");
            // TODO: Request an auth token with stored client tokens
        } else {
            Log.d(TAG, "No tokens found");
            // TODO: register the app with the domain
        }
    }

    @NonNull
    private String sanitiseDomain(@NonNull String domain) {
        // Check if http or https was included in domain string
        if (domain.startsWith("http")) {
            domain = domain.replaceFirst("https?:\\/\\/", "");
        }

        // Check if username was included in domain string
        int idx = domain.indexOf('@');
        if (idx != -1) {
            domain = domain.substring(idx + 1);
        }

        // Remove stray whitespaces
        return domain.trim();
    }

}
