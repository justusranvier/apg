/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
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

package org.thialfihar.android.apg.ui;

import java.io.File;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.deprecated.AskForPassphrase;
import org.thialfihar.android.apg.deprecated.PausableThread;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.Preferences;
import org.thialfihar.android.apg.util.ProgressDialogUpdater;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class BaseActivity extends SherlockFragmentActivity implements Runnable,
        ProgressDialogUpdater, AskForPassphrase.PassPhraseCallbackInterface {

    private ProgressDialog mProgressDialog = null;
    private PausableThread mRunningThread = null;
    private Thread mDeletingThread = null;

    private long mSecretKeyId = 0;
    private String mDeleteFile = null;

    protected Preferences mPreferences;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handlerCallback(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // not needed later:
        mPreferences = Preferences.getPreferences(this);

        PGPMain.initialize(this);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(Constants.path.APP_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                // ignore this for now, it's not crucial
                // that the directory doesn't exist at this point
            }
        }

        // startCacheService(this, mPreferences);
    }

    // public static void startCacheService(Activity activity, Preferences preferences) {
    // Intent intent = new Intent(activity, PassphraseCacheService.class);
    // intent.putExtra(PassphraseCacheService.EXTRA_TTL, preferences.getPassPhraseCacheTtl());
    // activity.startService(intent);
    // }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

            // TODO: needed?:
        case Id.menu.option.search:
            startSearch("", false, null, false);
            return true;

        default:
            break;

        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // in case it is a progress dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        switch (id) {
        case Id.dialog.encrypting: {
            mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
            return mProgressDialog;
        }

        case Id.dialog.decrypting: {
            mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
            return mProgressDialog;
        }

        case Id.dialog.saving: {
            mProgressDialog.setMessage(this.getString(R.string.progress_saving));
            return mProgressDialog;
        }

        case Id.dialog.importing: {
            mProgressDialog.setMessage(this.getString(R.string.progress_importing));
            return mProgressDialog;
        }

        case Id.dialog.exporting: {
            mProgressDialog.setMessage(this.getString(R.string.progress_exporting));
            return mProgressDialog;
        }

        case Id.dialog.deleting: {
            mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
            return mProgressDialog;
        }

        case Id.dialog.querying: {
            mProgressDialog.setMessage(this.getString(R.string.progress_querying));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            return mProgressDialog;
        }

        case Id.dialog.signing: {
            mProgressDialog.setMessage(this.getString(R.string.progress_signing));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            return mProgressDialog;
        }

        default: {
            break;
        }
        }
        mProgressDialog = null;

        switch (id) {

        case Id.dialog.pass_phrase: {
            return AskForPassphrase.createDialog(this, getSecretKeyId(), this);
        }

        case Id.dialog.pass_phrases_do_not_match: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setIcon(android.R.drawable.ic_dialog_alert);
            alert.setTitle(R.string.error);
            alert.setMessage(R.string.passPhrasesDoNotMatch);

            alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    removeDialog(Id.dialog.pass_phrases_do_not_match);
                }
            });
            alert.setCancelable(false);

            return alert.create();
        }

        case Id.dialog.no_pass_phrase: {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setIcon(android.R.drawable.ic_dialog_alert);
            alert.setTitle(R.string.error);
            alert.setMessage(R.string.passPhraseMustNotBeEmpty);

            alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    removeDialog(Id.dialog.no_pass_phrase);
                }
            });
            alert.setCancelable(false);

            return alert.create();
        }

        // case Id.dialog.delete_file: {
        // AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //
        // alert.setIcon(android.R.drawable.ic_dialog_alert);
        // alert.setTitle(R.string.warning);
        // alert.setMessage(this.getString(R.string.fileDeleteConfirmation, getDeleteFile()));
        //
        // alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dialog, int id) {
        // removeDialog(Id.dialog.delete_file);
        // final File file = new File(getDeleteFile());
        // showDialog(Id.dialog.deleting);
        // mDeletingThread = new Thread(new Runnable() {
        // public void run() {
        // Bundle data = new Bundle();
        // data.putInt(Constants.extras.STATUS, Id.message.delete_done);
        // try {
        // Apg.deleteFileSecurely(BaseActivity.this, file, BaseActivity.this);
        // } catch (FileNotFoundException e) {
        // data.putString(Apg.EXTRA_ERROR, BaseActivity.this.getString(
        // R.string.error_fileNotFound, file));
        // } catch (IOException e) {
        // data.putString(Apg.EXTRA_ERROR, BaseActivity.this.getString(
        // R.string.error_fileDeleteFailed, file));
        // }
        // Message msg = new Message();
        // msg.setData(data);
        // sendMessage(msg);
        // }
        // });
        // mDeletingThread.start();
        // }
        // });
        // alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dialog, int id) {
        // removeDialog(Id.dialog.delete_file);
        // }
        // });
        // alert.setCancelable(true);
        //
        // return alert.create();
        // }

        default: {
            break;
        }
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.secret_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                setSecretKeyId(bundle.getLong(SelectSecretKeyListActivity.EXTRA_KEY_ID));
            } else {
                setSecretKeyId(Id.key.none);
            }
            break;
        }

        default: {
            break;
        }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.STATUS, Id.message.progress_update);
        data.putInt(Constants.extras.PROGRESS, progress);
        data.putInt(Constants.extras.PROGRESS_MAX, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.STATUS, Id.message.progress_update);
        data.putString(Constants.extras.MESSAGE, message);
        data.putInt(Constants.extras.PROGRESS, progress);
        data.putInt(Constants.extras.PROGRESS_MAX, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void handlerCallback(Message msg) {
        Bundle data = msg.getData();
        if (data == null) {
            return;
        }

        int type = data.getInt(Constants.extras.STATUS);
        switch (type) {
        case Id.message.progress_update: {
            String message = data.getString(Constants.extras.MESSAGE);
            if (mProgressDialog != null) {
                if (message != null) {
                    mProgressDialog.setMessage(message);
                }
                mProgressDialog.setMax(data.getInt(Constants.extras.PROGRESS_MAX));
                mProgressDialog.setProgress(data.getInt(Constants.extras.PROGRESS));
            }
            break;
        }

        // case Id.message.delete_done: {
        // mProgressDialog = null;
        // deleteDoneCallback(msg);
        // break;
        // }

        case Id.message.import_done: // intentionally no break
        case Id.message.export_done: // intentionally no break
        case Id.message.query_done: // intentionally no break
        case Id.message.done: {
            mProgressDialog = null;
            doneCallback(msg);
            break;
        }

        default: {
            break;
        }
        }
    }

    public void doneCallback(Message msg) {

    }

    // public void deleteDoneCallback(Message msg) {
    // removeDialog(Id.dialog.deleting);
    // mDeletingThread = null;
    //
    // Bundle data = msg.getData();
    // String error = data.getString(Apg.EXTRA_ERROR);
    // String message;
    // if (error != null) {
    // message = getString(R.string.errorMessage, error);
    // } else {
    // message = getString(R.string.fileDeleteSuccessful);
    // }
    //
    // Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    // }

    public void passPhraseCallback(long keyId, String passPhrase) {
        // TODO: Not needed anymore, now implemented in AskForSecretKeyPass
        PGPMain.setCachedPassPhrase(keyId, passPhrase);
    }

    public void sendMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    public PausableThread getRunningThread() {
        return mRunningThread;
    }

    public void startThread() {
        mRunningThread = new PausableThread(this);
        mRunningThread.start();
    }

    public void run() {

    }

    public void setSecretKeyId(long id) {
        mSecretKeyId = id;
    }

    public long getSecretKeyId() {
        return mSecretKeyId;
    }

    protected void setDeleteFile(String deleteFile) {
        mDeleteFile = deleteFile;
    }

    protected String getDeleteFile() {
        return mDeleteFile;
    }
}
