/*
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

package org.thialfihar.android.apg.service;

import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.Preferences;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class PassphraseCacheService extends Service {
    private final IBinder mBinder = new LocalBinder();

    public static final String EXTRA_TTL = "ttl";

    public static void startCacheService(Context context) {
        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.putExtra(PassphraseCacheService.EXTRA_TTL, Preferences.getPreferences(context).getPassPhraseCacheTtl());
        context.startService(intent);
    }

    private int mPassPhraseCacheTtl = 15;
    private Handler mCacheHandler = new Handler();
    private Runnable mCacheTask = new Runnable() {
        public void run() {
            // check every ttl/2 seconds, which shouldn't be heavy on the device (even if ttl = 15),
            // and makes sure the longest a pass phrase survives in the cache is 1.5 * ttl
            int delay = mPassPhraseCacheTtl * 1000 / 2;
            // also make sure the delay is not longer than one minute
            if (delay > 60000) {
                delay = 60000;
            }

            delay = PGPMain.cleanUpCache(mPassPhraseCacheTtl, delay);
            // don't check too often, even if we were close
            if (delay < 5000) {
                delay = 5000;
            }

            mCacheHandler.postDelayed(this, delay);
        }
    };

    static private boolean mIsRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mIsRunning = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent != null) {
            mPassPhraseCacheTtl = intent.getIntExtra(EXTRA_TTL, 15);
        }
        if (mPassPhraseCacheTtl < 15) {
            mPassPhraseCacheTtl = 15;
        }
        mCacheHandler.removeCallbacks(mCacheTask);
        mCacheHandler.postDelayed(mCacheTask, 1000);
    }

    static public boolean isRunning() {
        return mIsRunning;
    }

    public class LocalBinder extends Binder {
        PassphraseCacheService getService() {
            return PassphraseCacheService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
