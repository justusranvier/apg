/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.ui.dialog;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.service.ApgHandler;
import org.thialfihar.android.apg.service.ApgService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class DeleteFileDialogFragment extends DialogFragment {

    private static final String ARG_DELETE_FILE = "delete_file";

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteFileDialogFragment newInstance(String deleteFile) {
        DeleteFileDialogFragment frag = new DeleteFileDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARG_DELETE_FILE, deleteFile);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final String deleteFile = getArguments().getString(ARG_DELETE_FILE);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setTitle(R.string.warning);
        alert.setMessage(this.getString(R.string.fileDeleteConfirmation, deleteFile));

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                // Send all information needed to service to edit key in other thread
                Intent intent = new Intent(activity, ApgService.class);

                // fill values for this action
                Bundle data = new Bundle();

                intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_DELETE_FILE_SECURELY);
                data.putString(ApgService.DELETE_FILE, deleteFile);
                intent.putExtra(ApgService.EXTRA_DATA, data);

                ProgressDialogFragment deletingDialog = ProgressDialogFragment.newInstance(
                        R.string.progress_deletingSecurely, ProgressDialog.STYLE_HORIZONTAL);

                // Message is received after deleting is done in ApgService
                ApgHandler saveHandler = new ApgHandler(activity, deletingDialog) {
                    public void handleMessage(Message message) {
                        // handle messages by standard ApgHandler first
                        super.handleMessage(message);

                        if (message.arg1 == ApgHandler.MESSAGE_OKAY) {
                            Toast.makeText(activity, R.string.fileDeleteSuccessful,
                                    Toast.LENGTH_SHORT).show();
                        }
                    };
                };

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(saveHandler);
                intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);

                // show progress dialog
                deletingDialog.show(activity.getSupportFragmentManager(), "deletingDialog");

                // start service with intent
                activity.startService(intent);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        alert.setCancelable(true);

        return alert.create();
    }
}