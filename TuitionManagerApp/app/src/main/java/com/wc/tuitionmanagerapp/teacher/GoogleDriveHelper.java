package com.wc.tuitionmanagerapp.teacher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {
    private static final String TAG = "GoogleDriveHelper";
    private static final List<Scope> DRIVE_SCOPES = Arrays.asList(
            new Scope(DriveScopes.DRIVE_FILE),
            new Scope(DriveScopes.DRIVE_METADATA),
            new Scope(DriveScopes.DRIVE)
    );

    public interface SignInCallback {
        void onSignInSuccess(GoogleSignInAccount account);
        void onSignInFailure(Exception e);
    }

    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private GoogleSignInAccount googleAccount;
    private Drive driveService;
    private SignInCallback signInCallback;

    public GoogleDriveHelper(Context context) {
        this.context = context;
    }



    public Drive getDriveService() {
        return driveService;
    }

    public GoogleSignInAccount getGoogleAccount() {
        return googleAccount;
    }

    public boolean isConnected() {
        return googleAccount != null && driveService != null;
    }

    public void setSignInCallback(SignInCallback callback) {
        this.signInCallback = callback;
    }

    public void signIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(DRIVE_SCOPES.get(0), DRIVE_SCOPES.get(1), DRIVE_SCOPES.get(2))
                .build();

        GoogleSignInAccount alreadySignedInAccount = GoogleSignIn.getLastSignedInAccount(context);

        if (alreadySignedInAccount != null) {
            boolean hasAllScopes = true;
            for (Scope scope : DRIVE_SCOPES) {
                if (!alreadySignedInAccount.getGrantedScopes().contains(scope)) {
                    hasAllScopes = false;
                    break;
                }
            }

            if (hasAllScopes) {
                Log.d(TAG, "Already signed in as " + alreadySignedInAccount.getEmail());
                setGoogleAccount(alreadySignedInAccount);
                notifySignInSuccess();
                return;
            }
        }

        GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.startActivityForResult(client.getSignInIntent(), 1002);
        } else {
            Log.e(TAG, "Context is not an activity. Cannot start sign-in.");
            notifySignInFailure(new IllegalStateException("Context is not an activity"));
        }
    }

    public void handleSignInResult(Intent result) {
        if (result == null) {
            Exception e = new NullPointerException("Sign-in result intent is null");
            Log.e(TAG, "Sign-in failed", e);
            notifySignInFailure(e);
            return;
        }

        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(account -> {
                    Log.d(TAG, "Signed in as " + account.getEmail());
                    setGoogleAccount(account);
                    notifySignInSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Unable to sign in", e);
                    notifySignInFailure(e);
                });
    }

    private void notifySignInSuccess() {
        if (signInCallback != null) {
            signInCallback.onSignInSuccess(googleAccount);
            signInCallback = null;
        }

        if (context instanceof TeacherHome) {
            ((TeacherHome) context).onDriveSignInComplete();
        } else if (context instanceof DeleteCourseMaterials) {
            ((DeleteCourseMaterials) context).onDriveSignInComplete();
        }
    }

    private void notifySignInFailure(Exception e) {
        if (signInCallback != null) {
            signInCallback.onSignInFailure(e);
            signInCallback = null;
        }

        if (context instanceof TeacherHome) {
            ((TeacherHome) context).onDriveSignInFailed(e);
        } else if (context instanceof DeleteCourseMaterials) {
            ((DeleteCourseMaterials) context).onDriveSignInFailed(e);
        }
    }

    public void setGoogleAccount(GoogleSignInAccount account) {
        this.googleAccount = account;

        GoogleAccountCredential credential = GoogleAccountCredential
                .usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        driveService = new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Tuition Manager App")
                .build();
    }

    public Intent getSignInIntent() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(DRIVE_SCOPES.get(0), DRIVE_SCOPES.get(1), DRIVE_SCOPES.get(2))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);
        return client.getSignInIntent();
    }

    public Task<String> uploadFileToDrive(Uri fileUri, String fileName, String mimeType, String parentFolderId) {
        return Tasks.call(executor, () -> {
            try {
                if (driveService == null) {
                    throw new IllegalStateException("Drive service not initialized");
                }

                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    throw new IOException("Cannot open file input stream");
                }

                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setMimeType(mimeType);
                if (parentFolderId != null && !parentFolderId.isEmpty()) {
                    fileMetadata.setParents(Collections.singletonList(parentFolderId));
                }

                InputStreamContent mediaContent = new InputStreamContent(mimeType, inputStream);
                return driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute()
                        .getId();
            } catch (Exception e) {
                Log.e(TAG, "Error uploading file", e);
                throw e;
            }
        });
    }

    public Task<String> getOrCreateFolder(String folderName) {
        return Tasks.call(executor, () -> {
            try {
                if (driveService == null) {
                    throw new IllegalStateException("Drive service not initialized");
                }

                String query = "mimeType='application/vnd.google-apps.folder' and name='" +
                        folderName + "' and trashed=false";

                FileList fileList = driveService.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("files(id,name)")
                        .execute();

                if (!fileList.getFiles().isEmpty()) {
                    return fileList.getFiles().get(0).getId();
                }

                File folderMetadata = new File();
                folderMetadata.setName(folderName);
                folderMetadata.setMimeType("application/vnd.google-apps.folder");

                File folder = driveService.files().create(folderMetadata)
                        .setFields("id")
                        .execute();

                return folder.getId();
            } catch (Exception e) {
                Log.e(TAG, "Error creating folder", e);
                throw e;
            }
        });
    }

    public Task<String> getFolderShareableLink(String folderId) {
        return Tasks.call(executor, () -> {
            try {
                if (driveService == null) {
                    throw new IllegalStateException("Drive service not initialized");
                }

                Permission userPermission = new Permission()
                        .setType("anyone")
                        .setRole("reader");

                driveService.permissions().create(folderId, userPermission)
                        .setFields("id")
                        .execute();

                return "https://drive.google.com/drive/folders/" + folderId;
            } catch (Exception e) {
                Log.e(TAG, "Error creating shareable link", e);
                throw e;
            }
        });
    }

    public Task<Void> deleteFile(String fileId) {
        return Tasks.call(executor, () -> {
            try {
                if (driveService == null) {
                    throw new IllegalStateException("Drive service not initialized");
                }

                driveService.files().delete(fileId).execute();
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error deleting file", e);
                throw e;
            }
        });
    }
}