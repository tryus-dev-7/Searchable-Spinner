package com.trydev.spinner.test;

import android.content.Context;

public class UiTools {

    public static int dpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
