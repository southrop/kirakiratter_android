package com.kirakiratter.android.util;

import java.util.Collections;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.TlsVersion;

/**
 * @author Southrop
 */

public class OkHttpClientHelper {

    public static OkHttpClient getOkHttpClient() {

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .allEnabledCipherSuites()
                .build();

        return new Builder()
                .connectionSpecs(Collections.singletonList(spec))
                .build();
    }

}
