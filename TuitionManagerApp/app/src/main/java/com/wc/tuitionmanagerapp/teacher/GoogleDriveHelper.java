package com.wc.tuitionmanagerapp.teacher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {
    private static final String TAG = "GoogleDriveHelper";
    private static final int REQUEST_CODE_SIGN_IN = 1002;
    private static final Scope DRIVE_SCOPE = new Scope(DriveScopes.DRIVE_FILE);

    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private Drive driveService;

    public GoogleDriveHelper(Context context) {
        this.context = context;
    }

    public void signIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(DRIVE_SCOPE)
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);
        ((CourseMaterial) context).startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    public void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    // Use the authenticated account to sign in to the Drive service
                    GoogleAccountCredential credential = GoogleAccountCredential
                            .usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());

                    driveService = new Drive.Builder(
                            new NetHttpTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Tuition Manager App")
                            .build();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Unable to sign in.", e));
    }

    public Task<String> uploadFileToDrive(Uri fileUri, String fileName, String mimeType, String parentFolderId) {
        return Tasks.call(executor, () -> {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                throw new IOException("Cannot open file input stream");
            }

            // Create file metadata
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setMimeType(mimeType);
            fileMetadata.setParents(Collections.singletonList(parentFolderId));

            // Upload file
            InputStreamContent mediaContent = new InputStreamContent(mimeType, inputStream);
            Drive.Files.Create createFile = driveService.files()
                    .create(fileMetadata, mediaContent);

            // For large files, consider using resumable upload:
            // createFile.getMediaHttpUploader().setDirectUploadEnabled(false);

            File uploadedFile = createFile.execute();
            return uploadedFile.getId();
        });
    }

    public Task<String> getOrCreateFolder(String folderName) {
        return Tasks.call(executor, () -> {
            // Check if folder exists
            String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
            Drive.Files.List listRequest = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id,name)");

            FileList fileList = listRequest.execute();
            if (!fileList.getFiles().isEmpty()) {
                return fileList.getFiles().get(0).getId();
            }

            // Create new folder if it doesn't exist
            File folderMetadata = new File();
            folderMetadata.setName(folderName);
            folderMetadata.setMimeType("application/vnd.google-apps.folder");

            File folder = driveService.files().create(folderMetadata)
                    .setFields("id")
                    .execute();

            return folder.getId();
        });
    }
}