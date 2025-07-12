package com.wc.tuitionmanagerapp.teacher;

import android.content.Intent;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class DeleteCourseMaterials extends AppCompatActivity {

    private String teacherUID;
    private List<String> courseNames = new ArrayList<>();
    private FirebaseFirestore firestoreDB;
    private GoogleDriveHelper googleDriveHelper;
    private RecyclerView materialsRecyclerView;
    private MaterialsAdapter materialsAdapter;
    private List<File> driveFiles = new ArrayList<>();
    private String pendingCourseNameToLoad = null;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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

        // Register result handler
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    googleDriveHelper.handleSignInResult(result.getData());
                });

        // RecyclerView setup
        materialsRecyclerView = findViewById(R.id.materialsRecyclerView);
        materialsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        materialsAdapter = new MaterialsAdapter(driveFiles);
        materialsRecyclerView.setAdapter(materialsAdapter);

        // Start Google sign-in
        googleSignInLauncher.launch(googleDriveHelper.getSignInIntent());
        findViewById(R.id.btndelete).setOnClickListener(v -> deleteSelectedFiles());
    }

    public void onDriveSignInComplete() {
        teacherUID = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userId", null);
        if (teacherUID == null) {
            Toast.makeText(this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        getCourseNames(); // Load courses only after sign-in is successful
    }

    public void goToMyTHome(View view) {
        NavigateUtil.goToTeacherHome(this);
    }

    private void getCourseNames() {
        firestoreDB.collection("courses")
                .whereArrayContains("teachers", teacherUID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courseNames.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String courseName = doc.getString("courseName");
                        if (courseName != null) {
                            courseNames.add(courseName);
                        }
                    }
                    addCoursesToSpinner(courseNames);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching courses", e);
                    Toast.makeText(this, "Error fetching courses", Toast.LENGTH_SHORT).show();
                });
    }

    private void addCoursesToSpinner(List<String> courses) {
        Spinner spinner = findViewById(R.id.courseSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, courses
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = (String) parent.getItemAtPosition(position);
                loadMaterialsForCourse(selectedCourse);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadMaterialsForCourse(String courseName) {
        try {
            googleDriveHelper.getOrCreateFolder(courseName)
                    .addOnSuccessListener(folderId -> listFilesInFolder(folderId))
                    .addOnFailureListener(e -> {
                        Log.e("DriveError", "Failed to access folder", e);
                        Toast.makeText(this, "Could not access course folder", Toast.LENGTH_SHORT).show();
                    });
        } catch (IllegalStateException e) {
            Toast.makeText(this, "Sign in first", Toast.LENGTH_SHORT).show();
        }
    }

    private void listFilesInFolder(String folderId) {
        Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            String query = "'" + folderId + "' in parents and trashed = false";
            return googleDriveHelper.getDriveService().files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name, mimeType, size, modifiedTime)")
                    .execute();
        }).addOnSuccessListener(fileList -> {
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

    private void deleteSelectedFiles() {
        List<String> selectedFileIds = materialsAdapter.getSelectedFileIds();
        if (selectedFileIds.isEmpty()) {
            Toast.makeText(this, "Please select files to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress bar
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        // Create a list of delete tasks
        List<Task<Void>> deleteTasks = new ArrayList<>();
        for (String fileId : selectedFileIds) {
            deleteTasks.add(googleDriveHelper.deleteFile(fileId));
        }

        // Execute all delete operations in parallel
        Tasks.whenAll(deleteTasks)
                .addOnSuccessListener(aVoid -> {
                    // Hide progress bar
                    findViewById(R.id.progressBar).setVisibility(View.GONE);

                    // Refresh the list
                    Spinner spinner = findViewById(R.id.courseSpinner);
                    String selectedCourse = (String) spinner.getSelectedItem();
                    loadMaterialsForCourse(selectedCourse);

                    Toast.makeText(this, "Files deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Hide progress bar
                    findViewById(R.id.progressBar).setVisibility(View.GONE);

                    Log.e("DriveError", "Error deleting files", e);
                    Toast.makeText(this, "Error deleting files", Toast.LENGTH_SHORT).show();
                });
    }

    private static class MaterialsAdapter extends RecyclerView.Adapter<MaterialsAdapter.ViewHolder> {
        private final List<File> files;
        private final Set<String> selectedFileIds = new HashSet<>();

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
            holder.selectionCheckbox.setChecked(selectedFileIds.contains(file.getId()));

            holder.selectionCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedFileIds.add(file.getId());
                } else {
                    selectedFileIds.remove(file.getId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        public List<String> getSelectedFileIds() {
            return new ArrayList<>(selectedFileIds);
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
