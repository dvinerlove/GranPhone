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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.Constants;
import com.bald.uriah.baldphone.utils.AppUtils;
import com.bumptech.glide.Glide;

/**
 * Most of this class is defined at {@link SingleMediaActivity}
 * The Constants used are defined at {@link Constants.PhotosConstants}
 */
public class SinglePhotoActivity extends SingleMediaActivity implements Constants.PhotosConstants {
    private static final String TAG = SinglePhotoActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupYoutube(8);
    }

    @Override
    protected MediaPagerAdapter mediaPagerAdapter() {
        return new PhotoPagerAdapter(this);
    }

    @Override
    protected CharSequence title() {
        return getText(R.string.photo);
    }

    @Override
    protected int deleteActionRes() {
        return R.string.delete_photo;
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_MEDIA_IMAGES;
    }

    private static class PhotoPagerAdapter extends MediaPagerAdapter {

        public PhotoPagerAdapter(SingleMediaActivity activity) {
            super(activity);
        }

        @Override
        protected Uri contentUri() {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        @Override
        protected String mimeType() {
            return "image/*";
        }

        @Override
        protected Cursor cursor(Context context) {
            return context.getContentResolver().query(IMAGES_URI,
                    PROJECTION,
                    null,
                    null,
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC"        // Ordering
            );
        }

        @Override
        protected void bindView(View view, Uri uri, Context context) {
            final ImageView pic = view.findViewById(R.id.pic);
            if (AppUtils.isValidContextForGlide(pic.getContext()))
                Glide.with(pic).load(uri).into(pic);
        }

        @Override
        protected View getView(Context context) {
            return LayoutInflater.from(context).inflate(R.layout.fragment_image, null, false);
        }
    }
}
