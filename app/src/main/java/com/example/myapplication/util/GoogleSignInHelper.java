package com.example.myapplication.util;

import android.app.Activity;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

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
            callback.onError(new IllegalStateException(
                    "Firebase chưa cấu hình OAuth Google. Hãy bật Google trong Firebase Authentication và cập nhật google-services.json."));
            return;
        }

        requestCredential(activity, callback, activity.getString(clientIdResource), false);
    }

    private static void requestCredential(@NonNull Activity activity, @NonNull Callback callback,
                                          @NonNull String webClientId, boolean compatibilityFlow) {
        GetCredentialRequest.Builder builder = new GetCredentialRequest.Builder();
        if (compatibilityFlow) {
            // Older Play services understands the standard Google ID option but not button flow.
            builder.addCredentialOption(new GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build());
        } else {
            builder.addCredentialOption(new GetSignInWithGoogleOption.Builder(webClientId).build());
        }
        GetCredentialRequest request = builder.build();

        CredentialManager.create(activity).getCredentialAsync(
                activity, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (!(credential instanceof CustomCredential)
                                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                .equals(credential.getType())) {
                            callback.onError(new IllegalStateException(
                                    "Google không trả về thông tin đăng nhập hợp lệ."));
                            return;
                        }
                        try {
                            GoogleIdTokenCredential google = GoogleIdTokenCredential
                                    .createFrom(((CustomCredential) credential).getData());
                            callback.onToken(google.getIdToken());
                        } catch (Exception e) {
                            callback.onError(e);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException error) {
                        // Người dùng tự đóng hộp chọn tài khoản, không báo lỗi.
                        if (error instanceof GetCredentialCancellationException) return;
                        if (error instanceof NoCredentialException) {
                            if (!compatibilityFlow && !activity.isFinishing() && !activity.isDestroyed()) {
                                requestCredential(activity, callback, webClientId, true);
                                return;
                            }
                            callback.onError(new Exception(
                                    "Google chưa trả về thông tin đăng nhập. Nếu đã đăng nhập Play Store, "
                                            + "hãy cập nhật Google Play services và kiểm tra tài khoản Google "
                                            + "có yêu cầu đăng nhập lại không, rồi thử lại.", error));
                            return;
                        }
                        callback.onError(error);
                    }
                });
    }
}
