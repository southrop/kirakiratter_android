package com.kirakiratter.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.kirakiratter.android.util.OkHttpClientHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import jastodon.MastodonApiClient;
import jastodon.entities.AppTokens;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Southrop
 */

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final String APP_SCOPE = "read write follow";

    @BindView(R.id.edittext_domain) EditText domainEditText;

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
            clientId = savedInstanceState.getString("client_id");
            clientSecret = savedInstanceState.getString("client_secret");
        }

        ButterKnife.bind(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("domain", domain);
        outState.putString("client_id", clientId);
        outState.putString("client_secret", clientSecret);

        super.onSaveInstanceState(outState);
    }

    public void tryLogin(View view) {
        String domain = sanitiseDomain(domainEditText.getText().toString());

        String storedClientId = preferences.getString(domain + "_client_id", null);
        String storedClientSecret = preferences.getString(domain + "_client_secret", null);

        if (storedClientId != null && storedClientSecret != null) {
            Log.d(TAG, "Found stored client tokens");
            // TODO: Request an auth token with stored client tokens
            login();
        } else {
            Log.d(TAG, "No tokens found");
            // TODO: register the app with the domain
            registerApp();
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

        // Remove any trailing slashes
        if (domain.endsWith("/")) {
            domain = domain.replace("/", "");
        }

        // Remove stray whitespaces
        return domain.trim();
    }

    private void registerApp() {
        Callback<AppTokens> callback = new Callback<AppTokens>() {
            @Override
            public void onResponse(Call<AppTokens> call, Response<AppTokens> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "registerApp Failed: " + response.message());
                    domainEditText.setError("Failed to register with " + domain + ": " + response.message());
                    return;
                }
                AppTokens tokens = response.body();
                clientId = tokens.clientId;
                clientSecret = tokens.clientSecret;

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(domain + "_client_id", clientId);
                editor.putString(domain + "_client_secret", clientSecret);
                editor.apply();

                login();
            }

            @Override
            public void onFailure(Call<AppTokens> call, Throwable t) {
                Log.e(TAG, "registerApp Failed");
                domainEditText.setError("Failed to register with " + domain);
            }
        };

        MastodonApiClient api = new MastodonApiClient(OkHttpClientHelper.getOkHttpClient(), domain);
        api.getAppService().registerApp(
                getString(R.string.app_name),
                getString(R.string.redirect_uri_scheme) + "://" + getString(R.string.redirect_uri_host) + getString(R.string.redirect_uri_path),
                APP_SCOPE,
                getString(R.string.app_website))
        .enqueue(callback);
    }

    private void login() {

    }

    @OnTextChanged(value = R.id.edittext_domain, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    private void unsetError() {
        domainEditText.setError(null);
    }

}
