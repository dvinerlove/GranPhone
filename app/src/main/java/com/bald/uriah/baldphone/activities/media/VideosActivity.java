/*
 * Copyright 2019 Uriah Shaul Mandel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bald.uriah.baldphone.activities.media;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.Constants;


/**
 * Most of this class is defined at {@link MediaScrollingActivity},
 * The Constants used are defined at {@link Constants.VideosConstants}
 */
public class VideosActivity extends MediaScrollingActivity implements Constants.VideosConstants {
    private static final String TAG = VideosActivity.class.getSimpleName();

    @Override
    protected CharSequence title() {
        return getString(R.string.videos);
    }

    @Override
    protected CharSequence emptyText() {
        return getString(R.string.no_videos);
    }

    @Override
    protected int emptyIcon() {
        return R.drawable.movie_on_background;
    }

    @Override
    protected Cursor cursor(ContentResolver contentResolver) {
        return contentResolver.query(VIDEOS_URI, PROJECTION,
                null,
                null,
                SORT_ORDER
        );
    }

    @Override
    protected Class<? extends SingleMediaActivity> singleActivity() {
        return SingleVideoActivity.class;
    }

    @Override
    protected Uri uriFor(Cursor cursor) {
        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID)));
    }

    @Override
    protected CharSequence itemDescription() {
        return getText(R.string.video);
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_MEDIA_VIDEO;
    }
}
