package com.wc.tuitionmanagerapp.teacher;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wc.tuitionmanagerapp.R;

import java.util.Calendar;
import java.util.Locale;

public class Assignment extends AppCompatActivity {

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
    }

    public void showDatePicker(View view) {
        final EditText dueDateInput = findViewById(R.id.dueDateInput);

        // Get current date as default
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, selectedYear, selectedMonth, selectedDay) -> {
            // Month is 0-based, so add 1
            selectedMonth += 1;

            // Format date as yyyy-MM-dd
            String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay);
            dueDateInput.setText(formattedDate);
        }, year, month, day);

        datePickerDialog.show();
    }

}