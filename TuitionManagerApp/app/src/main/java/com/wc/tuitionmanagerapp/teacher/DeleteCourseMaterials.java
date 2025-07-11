package com.wc.tuitionmanagerapp.teacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.wc.tuitionmanagerapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DeleteCourseMaterials extends AppCompatActivity {

    private String teacherUID;
    private List<String> courseNames = new ArrayList<>();
    private FirebaseFirestore firestoreDB;
    private GoogleDriveHelper googleDriveHelper;
    private RecyclerView materialsRecyclerView;
    private MaterialsAdapter materialsAdapter;
    private List<File> driveFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_course_materials);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        firestoreDB = FirebaseFirestore.getInstance();

        // Initialize Google Drive Helper
        googleDriveHelper = new GoogleDriveHelper(this);

        // Initialize UI components
        materialsRecyclerView = findViewById(R.id.materialsRecyclerView);
        materialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        materialsAdapter = new MaterialsAdapter(driveFiles);
        materialsRecyclerView.setAdapter(materialsAdapter);

        // Get teacher UID and load courses
        teacherUID = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userId", null);
        getCourseNames();
    }

    private void getCourseNames() {
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

        // Add spinner item selected listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = (String) parent.getItemAtPosition(position);
                loadMaterialsForCourse(selectedCourse);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadMaterialsForCourse(String courseName) {
        // First sign in to Google Drive if not already signed in
        if (googleDriveHelper.getDriveService() == null) {
            googleDriveHelper.signIn();
            return;
        }

        // Get or create folder for this course
        googleDriveHelper.getOrCreateFolder(courseName)
                .addOnSuccessListener(folderId -> {
                    // Now list all files in this folder
                    listFilesInFolder(folderId);
                })
                .addOnFailureListener(e -> {
                    Log.e("DriveError", "Error accessing course folder", e);
                    Toast.makeText(this, "Failed to access course materials", Toast.LENGTH_SHORT).show();
                });
    }

    private void listFilesInFolder(String folderId) {
        Task<FileList> listTask = Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            String query = "'" + folderId + "' in parents and trashed = false";
            return googleDriveHelper.getDriveService().files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name, mimeType, size, modifiedTime)")
                    .execute();
        });

        listTask.addOnSuccessListener(fileList -> {
            driveFiles.clear();
            driveFiles.addAll(fileList.getFiles());
            materialsAdapter.notifyDataSetChanged();

            if (driveFiles.isEmpty()) {
                Toast.makeText(this, "No materials found for this course", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("DriveError", "Error listing files", e);
            Toast.makeText(this, "Failed to load materials", Toast.LENGTH_SHORT).show();
        });
    }

    // You'll need to create this MaterialsAdapter class
    private static class MaterialsAdapter extends RecyclerView.Adapter<MaterialsAdapter.ViewHolder> {
        private final List<File> files;

        public MaterialsAdapter(List<File> files) {
            this.files = files;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_material, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            File file = files.get(position);
            holder.materialName.setText(file.getName());
            // You can add more details like file size or modification date here
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView materialName;
            public CheckBox selectionCheckbox;

            public ViewHolder(View view) {
                super(view);
                materialName = view.findViewById(R.id.materialName);
                selectionCheckbox = view.findViewById(R.id.selectionCheckbox);
            }
        }
    }
}