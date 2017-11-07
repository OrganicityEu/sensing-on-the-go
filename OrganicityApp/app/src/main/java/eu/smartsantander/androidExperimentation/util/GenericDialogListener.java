package eu.smartsantander.androidExperimentation.util;

import android.os.Bundle;


public abstract class GenericDialogListener {


    /**
     * Called when a dialog completes.
     * Executed by the thread that initiated the dialog.
     *
     * @param values Key-value string pairs extracted from the response.
     */
    public void onComplete(Bundle values) {
    }

    /**
     * Called when a dialog has an error.
     * Executed by the thread that initiated the dialog.
     */
    public void onError(String e) {
    }

    /**
     * Called when a dialog is canceled by the user.
     * Executed by the thread that initiated the dialog.
     */
    public void onCancel() {
    }

}