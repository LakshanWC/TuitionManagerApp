package com.wc.tuitionmanagerapp.teacher;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import com.wc.tuitionmanagerapp.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CourseMaterial extends AppCompatActivity {

    private static final int PICK_DOCUMENT_REQUEST_CODE = 1001;
    private static final int REQUEST_CODE_SIGN_IN = 9001;

    private LinearLayout addMaterialCard;
    private String teacherUID;
    private FirebaseFirestore firestoreDB;
    private List<String> courseNames = new ArrayList<>();
    private List<Uri> selectedUris = new ArrayList<>();

    private Drive googleDriveService;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_course_material);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestoreDB = FirebaseFirestore.getInstance();
        teacherUID = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userId", null);
        getCourseNames();

        addMaterialCard = findViewById(R.id.addMaterialCard);
        Animation wiggleAnimation = AnimationUtils.loadAnimation(this,R.anim.wiggle);
        addMaterialCard.startAnimation(wiggleAnimation);

        // Prepare GoogleSignInClient with Drive file scope
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file")) // Drive file scope
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    private void getCourseNames(){
        firestoreDB.collection("courses")
                .whereArrayContains("teachers", teacherUID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String courseName = doc.getString("courseName");
                            if (courseName != null) {
                                courseNames.add(courseName);
                            }
                        }
                        addCoursesToSpinner(courseNames);
                        Log.d("CourseList", "Courses for teacher: " + courseNames);
                    } else {
                        Log.d("CourseList", "No courses found for this teacher.");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error fetching courses: ", e));
    }

    private void addCoursesToSpinner(List<String> courses) {
        Spinner spinner = findViewById(R.id.courseSpinner);
        ArrayAdapter<String> myArrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                courses
        );
        myArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(myArrayAdapter);
    }

    public void goHome(View view){
        Intent goHomeIntent = new Intent(this, TeacherHome.class);
        startActivity(goHomeIntent);
    }

    public void selectDocument(View view){
        Intent documentIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        documentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        documentIntent.setType("*/*");
        documentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(documentIntent, PICK_DOCUMENT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_DOCUMENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            TextView fileLabel = findViewById(R.id.selectedFileLabel);
            TextView fileNameTextView = findViewById(R.id.selectedFileName);
            fileLabel.setVisibility(View.VISIBLE);
            selectedUris.clear(); // Clear previous selection

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                StringBuilder fileNames = new StringBuilder();

                for (int i = 0; i < count; i++) {
                    Uri fileUri = data.getClipData().getItemAt(i).getUri();
                    selectedUris.add(fileUri);

                    String fileName = getFileNameFromUri(fileUri);
                    fileNames.append(fileName).append("\n");

                    try {
                        getContentResolver().takePersistableUriPermission(
                                fileUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException e) {
                        Log.e("PermissionError", "Failed to persist permission: " + e.getMessage());
                    }
                }

                fileNameTextView.setText(fileNames.toString().trim());

            } else if (data.getData() != null) {
                Uri fileUri = data.getData();
                selectedUris.add(fileUri);

                String fileName = getFileNameFromUri(fileUri);
                fileNameTextView.setText(fileName);

                try {
                    getContentResolver().takePersistableUriPermission(
                            fileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException e) {
                    Log.e("PermissionError", "Failed to persist permission: " + e.getMessage());
                }
            }
        }

        // Handle Google Sign-In result
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == RESULT_OK && data != null) {
                handleSignInResult(data);
            } else {
                Log.e("GoogleSignIn", "Sign-in failed or canceled");
            }
        }
    }

    private void handleSignInResult(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(googleAccount -> {
                    Log.d("GoogleSignIn", "Signed in as: " + googleAccount.getEmail());

                    // Build Drive service with signed in account
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                            this, Collections.singletonList("https://www.googleapis.com/auth/drive.file"));
                    credential.setSelectedAccount(googleAccount.getAccount());

                    try {
                        googleDriveService = new Drive.Builder(
                                GoogleNetHttpTransport.newTrustedTransport(),
                                GsonFactory.getDefaultInstance(),
                                credential)
                                .setApplicationName("TuitionManagerApp")
                                .build();

                        // Now upload files asynchronously
                        new UploadFilesTask(this, googleDriveService, selectedUris).execute();

                    } catch (GeneralSecurityException | IOException e) {
                        Log.e("DriveInit", "Failed to create Drive client", e);
                    }
                })
                .addOnFailureListener(e -> Log.e("GoogleSignIn", "Failed to sign in", e));
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    public void uploadMaterials(View view) {
        if (selectedUris.isEmpty()) {
            Log.w("Upload", "No files selected to upload");
            return;
        }

        // Check if user already signed in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null || !account.getGrantedScopes().contains(new Scope("https://www.googleapis.com/auth/drive.file"))) {
            // Not signed in or Drive scope not granted, start sign in
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN);
        } else {
            // Already signed in, initialize Drive client and upload
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singletonList("https://www.googleapis.com/auth/drive.file"));
            credential.setSelectedAccount(account.getAccount());

            try {
                googleDriveService = new Drive.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential)
                        .setApplicationName("TuitionManagerApp")
                        .build();

                new UploadFilesTask(this, googleDriveService, selectedUris).execute();

            } catch (GeneralSecurityException | IOException e) {
                Log.e("DriveInit", "Failed to create Drive client", e);
            }
        }
    }


    // AsyncTask to upload files to Drive in background thread
    private static class UploadFilesTask extends AsyncTask<Void, String, Void> {
        private final Context context;
        private final Drive driveService;
        private final List<Uri> uris;

        UploadFilesTask(Context context, Drive driveService, List<Uri> uris) {
            this.context = context;
            this.driveService = driveService;
            this.uris = new ArrayList<>(uris); // copy to avoid concurrency issues
        }

        @Override
        protected void onPreExecute() {
            Log.d("Upload", "Starting file upload...");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (Uri uri : uris) {
                try {
                    String fileName = getFileNameFromUriStatic(context, uri);
                    java.io.File tempFile = createTempFileFromUri(context, uri, fileName);

                    File fileMetadata = new File();
                    fileMetadata.setName(fileName);

                    FileContent mediaContent = new FileContent(
                            context.getContentResolver().getType(uri),
                            tempFile);

                    File uploadedFile = driveService.files().create(fileMetadata, mediaContent).execute();

                    Log.d("Upload", "Uploaded file: " + uploadedFile.getName());

                    // Delete temp file
                    if (!tempFile.delete()) {
                        Log.w("Upload", "Temp file delete failed: " + tempFile.getAbsolutePath());
                    }

                } catch (Exception e) {
                    Log.e("Upload", "Failed to upload file", e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            Log.d("Upload", "All files uploaded.");
        }

        private static String getFileNameFromUriStatic(Context context, Uri uri) {
            String result = null;
            if ("content".equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        result = cursor.getString(nameIndex);
                    }
                }
            }
            if (result == null) {
                result = uri.getLastPathSegment();
            }
            return result;
        }

        private static java.io.File createTempFileFromUri(Context context, Uri uri, String fileName) throws IOException {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            java.io.File tempFile = java.io.File.createTempFile("upload_", "_" + fileName, context.getCacheDir());
            OutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            return tempFile;
        }
    }
}
