package com.wc.tuitionmanagerapp.teacher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class NavigateUtil {
    public static void goToTeacherHome(Context context) {
        Intent intent = new Intent(context, TeacherHome.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}

