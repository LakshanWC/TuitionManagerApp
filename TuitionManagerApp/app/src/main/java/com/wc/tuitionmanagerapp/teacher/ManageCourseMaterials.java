package com.wc.tuitionmanagerapp.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.wc.tuitionmanagerapp.R;

public class ManageCourseMaterials extends AppCompatActivity {

    private boolean isDriveServiceReady = false;
    private GoogleDriveHelper googleDriveHelper;

    private static final int REQUEST_CODE_SIGN_IN = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_course_materials);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        googleDriveHelper = new GoogleDriveHelper(this);
        checkIfAlreadySignedIn();
    }

    private void checkIfAlreadySignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            isDriveServiceReady = true;
            googleDriveHelper.setGoogleAccount(account);
        } else {
            isDriveServiceReady = false;
            // Optional: prompt user to sign in now or when needed
            // signInToDrive();
        }
    }

    // Call this method to start Google Drive sign-in flow if needed
    public void signInToDrive() {
        googleDriveHelper.signIn();
    }

    // Make sure to forward activity result to googleDriveHelper so it can handle sign-in response
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            googleDriveHelper.handleSignInResult(data);

            // After sign-in completes, update status and notify user
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                isDriveServiceReady = true;
                googleDriveHelper.setGoogleAccount(account);
                Toast.makeText(this, "Google Drive signed in", Toast.LENGTH_SHORT).show();
            } else {
                isDriveServiceReady = false;
                Toast.makeText(this, "Google Drive sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void goToUploadMaterials(View view) {
        if (isDriveServiceReady) {
            Intent uploadIntent = new Intent(this, CourseMaterial.class);
            startActivity(uploadIntent);
        } else {
            Toast.makeText(this, "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show();
            signInToDrive();
        }
    }

    public void goToDeleteMaterials(View view) {
        if (isDriveServiceReady) {
            Intent deleteIntent = new Intent(this, DeleteCourseMaterials.class);
            startActivity(deleteIntent);
        } else {
            Toast.makeText(this, "Please sign in to Google Drive first", Toast.LENGTH_SHORT).show();
            signInToDrive();
        }
    }

    // Optional: getters if other classes need to check status or use helper
    public boolean isDriveServiceReady() {
        return isDriveServiceReady;
    }

    public GoogleDriveHelper getGoogleDriveHelper() {
        return googleDriveHelper;
    }

    public void goBackToTHome(View view){
        finish();
    }
}
