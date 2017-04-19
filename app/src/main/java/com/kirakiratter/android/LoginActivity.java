package com.kirakiratter.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.kirakiratter.android.util.OkHttpClientHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import jastodon.MastodonApiClient;
import jastodon.models.oauth.AccessToken;
import jastodon.models.oauth.ClientTokens;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Southrop
 */

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final String APP_SCOPE = "read write follow";

    @BindView(R.id.edittext_domain)
    TextInputEditText domainEditText;

    private MastodonApiClient mastodonApi;
    private SharedPreferences preferences;

    private String domain = null;
    private String clientId = null;
    private String clientSecret = null;
    private String redirectUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (savedInstanceState != null) {
            domain = savedInstanceState.getString("domain");
            clientId = savedInstanceState.getString("client_id");
            clientSecret = savedInstanceState.getString("client_secret");
        }

        ButterKnife.bind(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mastodonApi = new MastodonApiClient(OkHttpClientHelper.getOkHttpClient(), domain);

        redirectUrl = getString(R.string.redirect_uri_scheme) + "://" + getString(R.string.redirect_uri_host);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");

        super.onStart();

        if (preferences.getString("active_access_token", null) != null &&
                preferences.getString("active_domain", null) != null) {
            // If we have a token already, we are already logged in
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Uri uri = getIntent().getData();

        if (uri != null && uri.toString().startsWith(redirectUrl)) {
            handleAuthorisation(uri);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");

        outState.putString("domain", domain);
        outState.putString("client_id", clientId);
        outState.putString("client_secret", clientSecret);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        super.onStop();

        if (domain != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("active_domain", domain);
            editor.putString("active_client_id", clientId);
            editor.putString("active_client_secret", clientSecret);
            editor.apply();
        }
    }

    public void tryLogin(View view) {
        Log.d(TAG, "tryLogin");

        String domain = sanitiseDomain(domainEditText.getText().toString());

        String storedClientId = preferences.getString(domain + "_client_id", null);
        String storedClientSecret = preferences.getString(domain + "_client_secret", null);

        if (storedClientId != null && storedClientSecret != null) {
            Log.i(TAG, "Found stored client tokens");
            Log.i(TAG, "clientId:     " + clientId);
            Log.i(TAG, "clientSecret: " + clientSecret);

            login();
        } else {
            Log.i(TAG, "No stored tokens found");
            registerApp(domain);
        }
    }

    @NonNull
    private String sanitiseDomain(@NonNull String domain) {
        Log.d(TAG, "sanitiseDomain");

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

    private void registerApp(final String domain) {
        Log.d(TAG, "registerApp");

        Callback<ClientTokens> callback = new Callback<ClientTokens>() {
            @Override
            public void onResponse(Call<ClientTokens> call, Response<ClientTokens> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to register app: " + response.message());
                    return;
                }

                ClientTokens tokens = response.body();
                clientId = tokens.clientId;
                clientSecret = tokens.clientSecret;

                Log.i(TAG, "Successfully registered app");
                Log.i(TAG, "clientId:     " + clientId);
                Log.i(TAG, "clientSecret: " + clientSecret);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(domain + "_client_id", clientId);
                editor.putString(domain + "_client_secret", clientSecret);
                editor.apply();

                login();
            }

            @Override
            public void onFailure(Call<ClientTokens> call, Throwable t) {
                Log.e(TAG, "Failed to register app");
            }
        };

        mastodonApi.getAuthService().registerClient(getString(R.string.app_name), redirectUrl,
                APP_SCOPE, getString(R.string.app_website))
        .enqueue(callback);
    }

    private void login() {
        Log.d(TAG, "login");

        if (clientId == null && clientSecret == null) {
            return;
        }

        String query = "?client_id=" + clientId + "&redirect_uri=" + redirectUrl +
                "&response_type=code&scope=" + APP_SCOPE;
        String url = "https://" + domain + MastodonApiClient.OAUTH_AUTHORIZE_PATH + query;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.e(TAG, "No browser available");
        }
    }

    /*@OnTextChanged(value = R.id.edittext_domain, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void unsetError() {
        domainEditText.setError(null);
    }*/

    private void handleAuthorisation(Uri uri) {
        // If they match, then it's a redirect from the authorisation page
        String error = uri.getQueryParameter("error");
        String code = uri.getQueryParameter("code");

        if (error != null) {
            Log.e(TAG, "Login failed: " + error);
        } else if (code != null) {
            Log.i(TAG, "Successfully authorised");
            Log.i(TAG, code);

            domain = preferences.getString("active_domain", null);
            clientId = preferences.getString("active_client_id", null);
            clientSecret = preferences.getString("active_client_secret", null);

            // Swap the authorization code for an access token
            Callback<AccessToken> callback = new Callback<AccessToken>() {
                @Override
                public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Failed to retrieve access token: " + response.message());
                        return;
                    }

                    AccessToken token = response.body();
                    String accessToken = token.accessToken;

                    Log.i(TAG, "Successfully retrieved access token");
                    Log.i(TAG, "accessToken:  " + accessToken);

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(domain + "_access_token", accessToken);
                    editor.putString("active_access_token", accessToken);
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, TitleActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Call<AccessToken> call, Throwable t) {
                    Log.e(TAG, "Failed to retrieve access token");
                }
            };

            mastodonApi.getAuthService().getAccessToken(clientId, clientSecret, redirectUrl,
                    code, "authorization_code").enqueue(callback);
        } else {
            Log.e(TAG, "Unknown error occurred.");
        }
    }

}
