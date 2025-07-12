package ru.orangesoftware.financisto.utils;

import android.content.Context;

// import com.mtramin.rxfingerprint.RxFingerprint;

import ru.orangesoftware.financisto.R;

public class FingerprintUtils {

    public static boolean fingerprintUnavailable(Context context) {
        return true; // Fingerprint disabled - always unavailable
    }

    public static String reasonWhyFingerprintUnavailable(Context context) {
        // Fingerprint functionality disabled
        return context.getString(R.string.fingerprint_unavailable_hardware);
    }

}
