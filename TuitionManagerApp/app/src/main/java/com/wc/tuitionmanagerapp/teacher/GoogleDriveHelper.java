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
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {
    private static final String TAG = "GoogleDriveHelper";
    private static final Scope DRIVE_SCOPE = new Scope(DriveScopes.DRIVE_FILE);

    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private GoogleSignInAccount googleAccount;
    private Drive driveService;

    public GoogleDriveHelper(Context context) {
        this.context = context;
    }

    public Drive getDriveService() {
        return driveService;
    }


    public void signIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(DRIVE_SCOPE)
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.startActivityForResult(client.getSignInIntent(), 1002);
        } else {
            Log.e(TAG, "Context is not an activity. Cannot start sign-in.");
        }
    }

    public void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(account -> {
                    Log.d(TAG, "Signed in as " + account.getEmail());

                    // ✅ Correctly initialize Google account and Drive service
                    setGoogleAccount(account);

                    // 🔄 Notify the relevant screen (activity) of sign-in completion
                    if (context instanceof TeacherHome) {
                        ((TeacherHome) context).onDriveSignInComplete();
                    } else if (context instanceof DeleteCourseMaterials) {
                        ((DeleteCourseMaterials) context).onDriveSignInComplete();
                    }

                })
                .addOnFailureListener(e -> Log.e(TAG, "Unable to sign in.", e));
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
                .requestScopes(DRIVE_SCOPE)
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);
        return client.getSignInIntent();
    }

    public Task<String> uploadFileToDrive(Uri fileUri, String fileName, String mimeType, String parentFolderId) {
        return Tasks.call(executor, () -> {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                throw new IOException("Cannot open file input stream");
            }

            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setMimeType(mimeType);
            fileMetadata.setParents(Collections.singletonList(parentFolderId));

            InputStreamContent mediaContent = new InputStreamContent(mimeType, inputStream);
            Drive.Files.Create createFile = driveService.files().create(fileMetadata, mediaContent);

            File uploadedFile = createFile.execute();
            return uploadedFile.getId();
        });
    }

    public Task<String> getOrCreateFolder(String folderName) {
        return Tasks.call(executor, () -> {
            String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
            Drive.Files.List listRequest = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id,name)");

            FileList fileList = listRequest.execute();
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
        });
    }

    public Task<String> getFolderShareableLink(String folderId) {
        return Tasks.call(executor, () -> {
            try {
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
            driveService.files().delete(fileId).execute();
            return null;
        });
    }

}
