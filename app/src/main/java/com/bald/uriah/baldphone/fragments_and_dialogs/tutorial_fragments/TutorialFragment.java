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

package com.bald.uriah.baldphone.fragments_and_dialogs.tutorial_fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.views.ViewPagerHolder;

public abstract class TutorialFragment extends Fragment {
    protected View root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(layoutRes(), container, false);
        attachXml();
        actualSetup();
        return root;
    }

    /**
     * Wires optional {@code bt_next}/{@code bt_back} views to the hosting
     * tutorial pager, so swiping is never required to move between pages.
     */
    protected void setupNavButtons() {
        final Activity activity = getActivity();
        if (activity == null)
            return;
        final ViewPagerHolder holder = activity.findViewById(R.id.view_pager_holder);
        if (holder == null)
            return;
        final View next = root.findViewById(R.id.bt_next);
        if (next != null)
            next.setOnClickListener(v -> {
                final int page = holder.getPageIndex();
                if (page + 1 < holder.getCount())
                    holder.setCurrentItem(page + 1);
            });
        final View back = root.findViewById(R.id.bt_back);
        if (back != null)
            back.setOnClickListener(v -> {
                final int page = holder.getPageIndex();
                if (page > 0)
                    holder.setCurrentItem(page - 1);
            });
    }

    protected abstract void attachXml();

    protected abstract void actualSetup();

    @LayoutRes
    protected abstract int layoutRes();
}
