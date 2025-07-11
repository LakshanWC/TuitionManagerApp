package com.wc.tuitionmanagerapp.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Assignment extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST_CODE = 1001;
    private static final int GOOGLE_DRIVE_SIGN_IN_REQUEST_CODE = 1002;
    private Spinner spinner;
    private TextView selectedFileName;
    private TextView selectedFileLabel;
    private Uri selectedFileUri;
    private String teacherUID;
    private String courseId;
    private List<String> courseNames = new ArrayList<>();
    private FirebaseFirestore firestoreDB;
    private GoogleDriveHelper googleDriveHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assignment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        firestoreDB = FirebaseFirestore.getInstance();
        googleDriveHelper = new GoogleDriveHelper(this);

        teacherUID = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userId", null);
        getCourseNames();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            googleDriveHelper.setGoogleAccount(account);
        }

        // Initialize views
        selectedFileName = findViewById(R.id.selectedFileName);
        selectedFileLabel = findViewById(R.id.selectedFileLabel);
        LinearLayout addMaterialCard = findViewById(R.id.addMaterialCard);

        // Set click listener for the upload card
        addMaterialCard.setOnClickListener(v -> openFilePicker());
    }

    private void getCourseNames() {
        firestoreDB.collection("courses")
                .whereArrayContains("teachers", teacherUID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        courseNames.clear();
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
        spinner = findViewById(R.id.courseSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                courses
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_PICKER_REQUEST_CODE);
    }

    private void getCourseIdForSelectedCourse(OnCourseIdReceivedListener listener) {
        String currentCourseName = spinner.getSelectedItem().toString();

        firestoreDB.collection("courses")
                .whereEqualTo("courseName", currentCourseName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        courseId = doc.getId();
                        Log.d("CourseList", "Course ID for " + currentCourseName + ": " + courseId);
                        listener.onCourseIdReceived(courseId);
                    } else {
                        Log.d("CourseList", "No course found with name: " + currentCourseName);
                        listener.onCourseIdReceived(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching course ID: ", e);
                    listener.onCourseIdReceived(null);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                if (selectedFileUri != null) {
                    String fileName = getFileNameFromUri(selectedFileUri);
                    selectedFileLabel.setVisibility(View.VISIBLE);
                    selectedFileName.setText(fileName);
                    selectedFileName.setTextColor(getResources().getColor(android.R.color.black));
                }
            }
        } else if (requestCode == GOOGLE_DRIVE_SIGN_IN_REQUEST_CODE) {
            googleDriveHelper.handleSignInResult(data);
            uploadAssignmentToDrive();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "File selection canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (scheme != null && scheme.equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }

        return fileName;
    }

    public void showDatePicker(View view) {
        final EditText dueDateInput = findViewById(R.id.dueDateInput);
        final Calendar calendar = Calendar.getInstance();
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH);
        final int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    dueDateInput.setText(formattedDate);
                }, year, month, day);

        datePickerDialog.show();
    }

    public void uploadAssignment(View view) {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            startActivityForResult(googleDriveHelper.getSignInIntent(), GOOGLE_DRIVE_SIGN_IN_REQUEST_CODE);
            return;
        }

        uploadAssignmentToDrive();
    }

    private void uploadAssignmentToDrive() {
        final EditText assignmentTitle = findViewById(R.id.assignmentNameInput);
        final EditText dueDateText = findViewById(R.id.dueDateInput);
        final String assignmentName = assignmentTitle.getText().toString();
        final String dueDate = dueDateText.getText().toString();

        if (assignmentName.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading to Google Drive...", Toast.LENGTH_SHORT).show();

        final String fileName = getFileNameFromUri(selectedFileUri);
        final String mimeType = getContentResolver().getType(selectedFileUri) != null ?
                getContentResolver().getType(selectedFileUri) : "*/*";

        // Get course ID first
        getCourseIdForSelectedCourse(new OnCourseIdReceivedListener() {
            @Override
            public void onCourseIdReceived(String courseId) {
                if (courseId == null) {
                    Toast.makeText(Assignment.this, "Failed to get course information", Toast.LENGTH_SHORT).show();
                    return;
                }

                // First create or get the folder
                googleDriveHelper.getOrCreateFolder("TuitionManagerAssignments")
                        .addOnSuccessListener(folderId -> {
                            // Then upload the file to that folder
                            googleDriveHelper.uploadFileToDrive(selectedFileUri, fileName, mimeType, folderId)
                                    .addOnSuccessListener(fileId -> {
                                        // Get the shareable link
                                        googleDriveHelper.getFolderShareableLink(folderId)
                                                .addOnSuccessListener(shareableLink -> {
                                                    saveAssignmentToFirestore(assignmentName, dueDate, shareableLink, courseId);
                                                })
                                                .addOnFailureListener(e -> {
                                                    showError("Failed to get shareable link: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        showError("Upload failed: " + e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                            showError("Failed to create folder: " + e.getMessage());
                        });
            }
        });
    }

    private void saveAssignmentToFirestore(String assignmentName, String dueDate, String fileURL, String courseId) {
        Map<String, Object> assignmentData = new HashMap<>();
        assignmentData.put("assignmentName", assignmentName);
        assignmentData.put("courseId", courseId);
        assignmentData.put("dueDate", dueDate);
        assignmentData.put("fileURL", fileURL);
        assignmentData.put("teacherId", teacherUID);
        assignmentData.put("uploadedDate", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        firestoreDB.collection("assignments")
                .add(assignmentData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Assignment uploaded successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showError("Firestore upload failed: " + e.getMessage());
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("AssignmentUpload", message);
    }

    public void goHome(View view) {
        finish();
    }

    interface OnCourseIdReceivedListener {
        void onCourseIdReceived(String courseId);
    }
}