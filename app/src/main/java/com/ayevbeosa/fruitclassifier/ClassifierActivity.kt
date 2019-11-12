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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.ayevbeosa.fruitclassifier.databinding.ActivityClassifierBinding
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * This is main view of the application.
 * When the button is clicked the user is lead to choose between taking a photo or picking from their gallery.
 * The app does a recognition on the selected image and gives the label that matches the image.
 */
class ClassifierActivity : AppCompatActivity() {

    // Variable to hold the reference to the binding object.
    private lateinit var binding: ActivityClassifierBinding
    // Reference to the ViewModel object.
    private lateinit var viewModel: ClassifierViewModel

    // The file path of the selected image file at any given time.
    private var currentImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the reference to the binding object and set the views in the layout.
        binding = DataBindingUtil.setContentView(this, R.layout.activity_classifier)

        // Set the ActionBar to a custom Toolbar object.
        setSupportActionBar(binding.toolbar)

        // Click listener for the button
        binding.recogniseButton.setOnClickListener {
            showPictureDialog()
        }

        Timber.plant(Timber.DebugTree())

        val application = requireNotNull(this).application
        // Create an instance of the ViewModel Factory.
        val viewModelFactory = ClassifierViewModelFactory(application)

        // Get a reference to the ViewModel for this activity.
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ClassifierViewModel::class.java)

        binding.viewModel = viewModel
        // Make the binding Lifecycle aware
        binding.lifecycleOwner = this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check if camera should be used.
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            setPicture()
        }
        // Check if image should be gotten from gallery.
        if (requestCode == REQUEST_GALLERY_PHOTO && resultCode == RESULT_OK) {
            val uri = data?.data
            dispatchChoosePhoto(uri!!)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if read/write permission has been granted
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFilePicker()
        }
    }

    /**
     * Creates an Intent that calls the phone's camera app.
     */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(this, R.string.error_creating_file, Toast.LENGTH_LONG).show()
                    null
                }
                // Continue only if the FIle was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.ayevbeosa.fruitclassifier.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(
                        takePictureIntent,
                        REQUEST_TAKE_PHOTO
                    )
                }
            }
        }
    }

    /**
     * Creates a file to store the captured image.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // save a file: path for use with ACTION_VIEW intents
            currentImagePath = absolutePath
        }
    }

    /**
     * Get path of selected Image from gallery.
     */
    private fun dispatchChoosePhoto(uri: Uri) {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver?.query(uri, projection, null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(projection[0])
        currentImagePath = cursor?.getString(columnIndex!!)
        cursor?.close()

        setPicture()
    }

    /**
     * Requests for runtime permission for Android M devices and above.
     *
     * If permission request is successful, operation flow continues;
     * else shows rationale for requesting for permission.
     */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Request for permission state.
            val permissionCheckedChanged =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // Checks if permission is granted.
            if (permissionCheckedChanged == PackageManager.PERMISSION_GRANTED) {
                openFilePicker()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    val alertDialog: AlertDialog? = this.let {
                        val builder = AlertDialog.Builder(it)
                        builder.apply {
                            setMessage(R.string.permission_rationale)
                            setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            setPositiveButton(R.string.give_permission) { dialog, _ ->
                                dialog.dismiss()

                                requestPermissions(
                                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                    REQUEST_WRITE_PERMISSION
                                )
                            }
                        }
                        builder.create()
                    }
                    alertDialog?.show()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
                }
            }
        } else {
            openFilePicker()
        }
    }

    /**
     * Creates an [Intent] that calls the system gallery.
     */
    private fun openFilePicker() {
        Intent().also { choosePictureIntent ->
            choosePictureIntent.type = "image/*"
            choosePictureIntent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(choosePictureIntent, REQUEST_GALLERY_PHOTO)
        }
    }

    /**
     * Creates an [AlertDialog] that shows the options for getting an image and calls the necessary
     * functions to handle an option selection.
     */
    private fun showPictureDialog() {
        // Retrieve the list of options as an array from the resource file.
        val options = resources.getStringArray(R.array.options_array)

        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                // Set the title of the AlertDialog
                setTitle(R.string.choose_an_option)
                setItems(options) { dialog, item ->
                    when (options[item]) {
                        getString(R.string.take_photo) -> dispatchTakePictureIntent()
                        getString(R.string.choose_from_gallery) -> requestPermission()
                        getString(R.string.cancel) -> dialog.dismiss()
                    }
                }
            }
            builder.create()
        }
        alertDialog?.show()
    }

    /**
     * Makes [ClassifierViewModel] aware of the current Image path.
     */
    private fun setPicture() {
        viewModel.setImagePath(currentImagePath)
    }

    companion object {
        // Constant values

        private const val REQUEST_GALLERY_PHOTO = 2
        private const val REQUEST_TAKE_PHOTO = 1
        private const val REQUEST_WRITE_PERMISSION = 3
    }
}
