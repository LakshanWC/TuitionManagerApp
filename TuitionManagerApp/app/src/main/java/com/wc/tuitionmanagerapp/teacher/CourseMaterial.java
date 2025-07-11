package com.wc.tuitionmanagerapp.teacher;

import android.app.ProgressDialog;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.wc.tuitionmanagerapp.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CourseMaterial extends AppCompatActivity {

    private static final int PICK_DOCUMENT_REQUEST_CODE = 1001;
    private static final int REQUEST_CODE_SIGN_IN = 1002;

    // Add this with your other fields
    private GoogleDriveHelper googleDriveHelper;
    private boolean isDriveServiceReady = false;
    private LinearLayout addMaterialCard;
    private String teacherUID;
    private FirebaseFirestore firestoreDB;
    private List<String> courseNames = new ArrayList<>();
    private List<Uri> selectedUris = new ArrayList<>();

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

        // Initialize Google Drive Helper
        googleDriveHelper = new GoogleDriveHelper(this);


        firestoreDB = FirebaseFirestore.getInstance();
        teacherUID = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userId", null);
        getCourseNames();

        addMaterialCard = findViewById(R.id.addMaterialCard);
        Animation wiggleAnimation = AnimationUtils.loadAnimation(this,R.anim.wiggle);
        addMaterialCard.startAnimation(wiggleAnimation);
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
            // Handle document selection
            TextView fileLabel = findViewById(R.id.selectedFileLabel);
            TextView fileNameTextView = findViewById(R.id.selectedFileName);
            fileLabel.setVisibility(View.VISIBLE);
            selectedUris.clear(); // Clear previous selection

            if (data.getClipData() != null) {
                // Multiple files selected
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
                // Single file selected
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
        else if (requestCode == REQUEST_CODE_SIGN_IN) {
            // Handle Google Sign-In result
            if (resultCode == RESULT_OK && data != null) {
                googleDriveHelper.handleSignInResult(data);
                isDriveServiceReady = true;

                // Optional: Show success message or enable upload button
                Toast.makeText(this, "Signed in to Google Drive", Toast.LENGTH_SHORT).show();
            } else {
                // Sign-in failed
                Log.e("GoogleSignIn", "Sign in failed");
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();

                // You might want to retry sign-in or show an error message
                googleDriveHelper.signIn(); // Optional: Retry sign-in
            }
        }
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
            Toast.makeText(this, "Please select files first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isDriveServiceReady) {
            Toast.makeText(this, "Sign in to Google Drive..", Toast.LENGTH_SHORT).show();
            googleDriveHelper.signIn();
            return;
        }

        Spinner courseSpinner = findViewById(R.id.courseSpinner);
        String courseName = courseSpinner.getSelectedItem().toString();

        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading files...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create a folder first (if not exists)
        googleDriveHelper.getOrCreateFolder(courseName)
                .addOnSuccessListener(folderId -> {
                    // Now upload each file to this folder
                    uploadFilesToFolder(courseName, folderId, progressDialog);
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to create folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("DriveUpload", "Folder creation failed", e);
                });
    }

    private void uploadFilesToFolder(String courseName, String folderId, ProgressDialog progressDialog) {
        int totalFiles = selectedUris.size();
        final int[] uploadedCount = {0};

        for (Uri fileUri : selectedUris) {
            String fileName = getFileNameFromUri(fileUri);
            String mimeType = getContentResolver().getType(fileUri);
            if (mimeType == null) {
                mimeType = "*/*";
            }

            googleDriveHelper.uploadFileToDrive(fileUri, fileName, mimeType, folderId)
                    .addOnSuccessListener(fileId -> {
                        uploadedCount[0]++;
                        Log.d("DriveUpload", "Uploaded " + fileName + " to " + courseName);

                        if (uploadedCount[0] == totalFiles) {
                            progressDialog.dismiss();
                            Toast.makeText(this,
                                    "All files uploaded successfully to " + courseName,
                                    Toast.LENGTH_LONG).show();
                            // Clear selection after upload
                            selectedUris.clear();
                            TextView myTextView = findViewById(R.id.selectedFileName);
                            myTextView.setText("");
                            findViewById(R.id.selectedFileLabel).setVisibility(View.GONE);

                            setMaterialLink(courseName, folderId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this,
                                "Failed to upload " + fileName + ": " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("DriveUpload", "Upload failed: " + fileName, e);
                    });
        }
    }

    private void setMaterialLink(String courseName, String folderId) {
        googleDriveHelper.getFolderShareableLink(folderId)
                .addOnSuccessListener(folderLink -> {
                    // Update the Firestore document for the course
                    firestoreDB.collection("courses")
                            .whereEqualTo("courseName", courseName)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                        firestoreDB.collection("courses")
                                                .document(doc.getId())
                                                .update("courseMaterials", folderLink)
                                                .addOnSuccessListener(aVoid ->
                                                        Toast.makeText(this, "Drive link added to course", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e ->
                                                        Log.e("FirestoreUpdate", "Failed to update courseMaterials", e));
                                    }
                                } else {
                                    Log.e("FirestoreUpdate", "No matching course found for name: " + courseName);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e("FirestoreUpdate", "Error finding course: " + courseName, e));
                })
                .addOnFailureListener(e -> {
                    Log.e("DriveLink", "Failed to get shareable link", e);
                    Toast.makeText(this, "Failed to get shareable folder link", Toast.LENGTH_SHORT).show();
                });
    }
}