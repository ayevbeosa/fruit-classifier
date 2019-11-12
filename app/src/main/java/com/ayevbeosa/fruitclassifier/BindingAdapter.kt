/*
 * Copyright (c) 2019. Ayevbeosa Iyamu. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayevbeosa.fruitclassifier

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import java.io.File

/**
 * Uses the Picasso library to load an image by URI into an [ImageView]
 */
@BindingAdapter("imagePath")
fun loadImage(imageView: ImageView, imagePath: String?) {
    if (imagePath != null) {
        Picasso.get()
            .load(File(imagePath))
            .placeholder(R.drawable.loading_animation)
            .into(imageView)
    }
}