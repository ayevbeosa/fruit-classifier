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

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import timber.log.Timber
import java.io.File

/**
 * The [ViewModel] that is attached to the [ClassifierActivity] class
 */
class ClassifierViewModel(private val application: Application) : ViewModel() {

    private val _modelStatus = MutableLiveData<String>()
    val modelStatus: LiveData<String>
        get() = _modelStatus

    private val _labelName = MutableLiveData<String>()
    val labelName: LiveData<String>
        get() = _labelName

    private val _labelConfidence = MutableLiveData<String>()
    val labelConfidence: LiveData<String>
        get() = _labelConfidence

    private val _filePath = MutableLiveData<String>()
    val filePath: LiveData<String>
        get() = _filePath

    /**
     * Variable to set the visibility of the Recognise button.
     * Button shouldn't enable until the model is ready
     */
    private val _recogniseButtonVisible = MutableLiveData<Boolean>()
    /**
     * If true, enable button. Else disable button.
     */
    val recogniseButtonVisible: LiveData<Boolean>
        get() = _recogniseButtonVisible

    /**
     * Variable to hold the [FirebaseAutoMLRemoteModel] for the application.
     */
    private val remoteModel = FirebaseAutoMLRemoteModel.Builder(application.getString(R.string.model_name))
        .build()

    init {
        configureHostedModelSource()
    }

    /**
     * Initial setting up of the model. The model is downloaded and loaded to the application.
     * Conditions has to be meant before download can occur.
     */
    private fun configureHostedModelSource() {
        // Set conditions
        val conditions = FirebaseModelDownloadConditions.Builder().also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                it.requireDeviceIdle()
        }.build()

        _modelStatus.value = application.getString(R.string.getting_model_ready)
        // Model not ready disable button.
        _recogniseButtonVisible.value = false

        // Prep model.
        FirebaseModelManager.getInstance()
            .download(remoteModel, conditions)
            .addOnSuccessListener {
                _modelStatus.value = application.getString(R.string.model_ready)
                // Model ready enable button.
                _recogniseButtonVisible.value = true
            }
            .addOnFailureListener {
                _modelStatus.value = application.getString(R.string.error_loading_model)
                configureHostedModelSource()
            }
    }

    /**
     * Function is called immediately after an image has been selected.
     * This in turn calls the function to begin recognition of the image.
     */
    fun setImagePath(filePath: String?) {
        // Update file path
        _filePath.value = filePath
        // Clear the views
        _labelName.value = ""
        _labelConfidence.value = ""
        // Call function to begin recognition
        beginRecognition()
    }

    /**
     * When called, it creates a [FirebaseVisionImage] object from a [Uri] object.
     * The [FirebaseVisionImage] object is processed and the resulting labels is gotten.
     */
    fun beginRecognition() {
        // Create FirebaseVisionImage object from Uri which parses the File path from the string.
        val image = FirebaseVisionImage.fromFilePath(application, Uri.fromFile(File(filePath.value)))

        // Create options for labelling -- involves setting confidence threshold.
        val labelerOptions = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel)
            .setConfidenceThreshold(0.75f)
            .build()

        // Set options for labelling.
        val labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(labelerOptions)

        _modelStatus.value = application.getString(R.string.recognising)

        // Begin labelling.
        labeler.processImage(image)
            .addOnSuccessListener { labels ->
                Timber.i("onSuccess: Recognition Completed!")
                // Recognition completed, update UI.
                updateLabels(labels)
                _modelStatus.value = application.getString(R.string.recognition_completed)
            }
            .addOnFailureListener { e ->
                Timber.e(e)
                _modelStatus.value = "An error occurred. Tap to retry"
                beginRecognition()
            }
    }

    private fun updateLabels(labels: List<FirebaseVisionImageLabel>) {
        if (labels.isNotEmpty()) {
            var highest = labels[0].confidence
            var text = labels[0].text
            labels.forEach { label ->
                Timber.i("Name: ${label.text} Confidence: ${label.confidence}")
                if (highest < label.confidence) {
                    highest = label.confidence
                    text = label.text
                }
            }
            highest *= 100
            text = "Fruit Name: ${getFruitName(text)}"
//            if (highest >= 90) {
            _labelName.value = text
            val confidence = "Confidence Level: %.2f".format(highest) + "%"
            _labelConfidence.value = confidence
//            } else {
//                _labelName.value = "Unable to recognise image"
//            }
        } else {
            Timber.i("Empty labels")
            _labelName.value = "Unable to recognise image"
        }
    }

    /**
     * Gets the name of the label text and returns
     * the corresponding name of the fruit.
     */
    private fun getFruitName(labelText: String): String =
        when (labelText) {
            "banana_1" -> "Locally known as, Igbo Banana"
            "banana_2" -> "Locally known as, Yoruba Banana"
            "green_small_apple" -> "Granny Smith Apple"
            "orange_1" -> "Sweet Hamlin Orange"
            "orange_2" -> "Clementine Orange"
            "papaya" -> "Papaya"
            "red_apple" -> "Cripps Pink Apple"
            "tangerine" -> "Tangerine"
            else -> ""
        }
}
