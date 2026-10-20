package com.example.myapplication.util;

import android.app.Activity;
import android.os.CancellationSignal;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.myapplication.R;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.concurrent.Executor;

/** Opens the Android credential picker and returns a Google ID token for Firebase Auth. */
public final class GoogleSignInHelper {
    public interface Callback {
        void onToken(String idToken);
        void onError(Exception error);
    }

    private GoogleSignInHelper() {}

    public static void launch(@NonNull Activity activity, @NonNull Callback callback) {
        int clientIdResource = activity.getResources().getIdentifier(
                "default_web_client_id", "string", activity.getPackageName());
        if (clientIdResource == 0) {
            Toast.makeText(activity,
                    "Firebase chưa cấu hình OAuth Google. Hãy bật Google trong Firebase Authentication và cập nhật google-services.json.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        GetGoogleIdOption googleOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(clientIdResource))
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();
        CredentialManager manager = CredentialManager.create(activity);
        Executor mainExecutor = activity.getMainExecutor();
        manager.getCredentialAsync(activity, request, new CancellationSignal(), mainExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (!(credential instanceof CustomCredential)
                                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                .equals(credential.getType())) {
                            callback.onError(new IllegalStateException("Google không trả về thông tin đăng nhập hợp lệ."));
                            return;
                        }
                        GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential
                                .createFrom(((CustomCredential) credential).getData());
                        callback.onToken(googleCredential.getIdToken());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException error) {
                        callback.onError(error);
                    }
                });
    }
}
