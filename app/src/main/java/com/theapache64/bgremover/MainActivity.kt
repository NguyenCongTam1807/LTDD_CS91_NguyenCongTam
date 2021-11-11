package com.theapache64.removebgexample

import android.Manifest
import android.R.attr
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.esafirm.imagepicker.features.ImagePicker
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.theapache64.removebg.RemoveBg
import com.theapache64.removebg.utils.ErrorResponse
import com.theapache64.removebgexample.preferences.AppPreferences
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val rootPath =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
    private val projectDir by lazy {
        File("${rootPath}/Bg-remover")
    }
    private val cameraDir by lazy {
        File(rootPath, "Camera")
    }
    private lateinit var photoURI: Uri
    private var inputImage: File? = null
    private var outputImage: File? = null
    private val cameraPermissionCode = 100
    private val storagePermissionCode = 101
    private val actionCapturePhoto = 200
    private val authority = "com.theapache64.removebgexample.provider"

    var appPreferences: AppPreferences? = null
    var theme: Int? = null
    var folderMode: Boolean = false

    @BindView(R.id.iv_input)
    lateinit var ivInput: ImageView

    @BindView(R.id.iv_output)
    lateinit var ivOutput: ImageView

    @BindView(R.id.tv_input_details)
    lateinit var tvInputDetails: TextView

    @BindView(R.id.b_process)
    lateinit var bProcess: View

    @BindView(R.id.tv_progress)
    lateinit var tvProgress: TextView

    @BindView(R.id.pb_progress)
    lateinit var pbProgress: ProgressBar

    @BindView(R.id.tv_instruction)
    lateinit var tvInstruction: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)

        //set language
        val lang = if (appPreferences!!.isUsingViLocale()) {
            "vi"
        } else "en"
        val config = resources.configuration
        val locale = Locale(lang)
        Locale.setDefault(locale)
        config!!.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        //set theme
        theme = if (appPreferences!!.isInDarkMode()) R.style.DarkTheme
        else R.style.AppTheme
        setTheme(theme!!)

        //get FolderMode to display image using ImagePicker
        folderMode = appPreferences!!.isInFolderMode()

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val intent = if (id == R.id.action_settings) {
            Intent(this@MainActivity, SettingsActivity::class.java)
        } else {
            Intent(this@MainActivity, AboutActivity::class.java)
        }
        startActivity(intent)
        return true
    }

    @OnClick(R.id.b_choose_image, R.id.i_choose_image)
    fun onChooseImageClicked() {
        ImagePicker.create(this)
            .single()
            .theme(theme!!)
            .folderMode(folderMode)
            .toolbarArrowColor(getColorFromResource(attr.textColor))
            .imageFullDirectory(cameraDir.absolutePath)
            .start()
    }

    @OnClick(R.id.b_capture_image)
    fun onCaptureImageClicked() {
        val cameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val storagePermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        when {
            cameraPermission != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode
            )
            storagePermission != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionCode
            )
            else -> {
                capturePhoto()
            }
        }
    }

    private fun capturePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    toast(ex.message!!)
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                        this,
                        authority,
                        it
                    )
                    inputImage = it
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, actionCapturePhoto)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an file to save the bitmap image later
        val timeStamp: String = SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SSS").format(Date())
        if (!cameraDir.exists())
            cameraDir.mkdir()
        return File(cameraDir, "${timeStamp}.jpg")
    }

    private fun appendInputDetails(details: String) {
        tvInputDetails.text = "${tvInputDetails.text}\n$details"
    }

    private fun clearInputDetails() {
        tvInputDetails.text = ""
    }

    @OnClick(R.id.iv_input)
    fun onInputClicked() {
        if (inputImage != null) {
            showActionAlert(this, inputImage!!)
        } else {
            toast(R.string.error_no_image_selected)
        }
    }

    @OnClick(R.id.iv_output)
    fun onOutputClicked() {
        if (outputImage != null) {
            showActionAlert(this, outputImage!!)
        } else {
            toast(R.string.error_output_not_saved)
        }
    }

    private fun showActionAlert(context: Context, image: File) {
        val alert = AlertDialog.Builder(context).create()
        alert.setTitle("Choose action")
        alert.setButton(
            Dialog.BUTTON_POSITIVE, getString(R.string.bt_view)
        ) { _, _ ->
            viewImage(image)
        }
        alert.setButton(
            Dialog.BUTTON_NEUTRAL, getString(R.string.bt_ok)
        ) { _, _ ->
        }
        alert.setButton(
            Dialog.BUTTON_NEGATIVE, getString(R.string.bt_share)
        ) { _, _ ->
            shareImage(image)
        }
        alert.create()
        alert.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(getColorFromResource(R.attr.colorAccent))
        alert.getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(getColorFromResource(R.attr.colorAccent))
        alert.show()
    }

    private fun viewImage(inputImage: File) {
        val uri = FileProvider.getUriForFile(this, authority, inputImage)
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(this)
        }
    }

    private fun shareImage(inputImage: File) {
        val bitmap = BitmapFactory.decodeFile(inputImage.absolutePath)
        val sharedImageDir = File(projectDir, "shared")
        if (!sharedImageDir.exists())
            sharedImageDir.mkdirs() // create the directory to contain the lone share file
        val stream =
            FileOutputStream("$sharedImageDir/shared-no-bg.png") // overwrites this image every time
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val newFile = File(sharedImageDir, "shared-no-bg.png")
        val contentUri = FileProvider.getUriForFile(this, authority, newFile)
        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    @OnClick(R.id.b_process)
    fun onProcessClicked() {
        if (inputImage != null) {
            // Check permission
            checkPermission {
                // permission granted
                if (inputImage!!.name.contains("-no-bg").not()) {
                    tvProgress.setText(R.string.status_compressing)
                    // compress the unprocessed inputImage now
                    compressImage(inputImage!!) { bitmap ->

                        saveImage(
                            "${SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SSS").format(Date())}" +
                                    "-compressed", bitmap
                        ) { compressedImage ->
                            val compressedImageSize = compressedImage.length() / 1024
                            val originalImageSize = inputImage!!.length() / 1024

                            pbProgress.visibility = View.VISIBLE
                            tvProgress.visibility = View.VISIBLE

                            tvProgress.setText(R.string.status_uploading)
                            pbProgress.progress = 0

                            val finalImage =
                                if (compressedImageSize < originalImageSize) compressedImage else inputImage!!
                            appendInputDetails(
                                resources.getString(
                                    R.string.compressed_size,
                                    finalImage.length() / 1024
                                )
                            )

                            // inputImage saved, now upload
                            RemoveBg.from(finalImage, object : RemoveBg.RemoveBgCallback {

                                override fun onProcessing() {
                                    runOnUiThread {
                                        tvProgress.setText(R.string.status_processing)
                                    }
                                }

                                override fun onUploadProgress(progress: Float) {
                                    runOnUiThread {
                                        tvProgress.text = "Uploading ${progress.toInt()}%"
                                        pbProgress.progress = progress.toInt()
                                    }
                                }

                                override fun onError(errors: List<ErrorResponse.Error>) {
                                    runOnUiThread {
                                        val errorBuilder = StringBuilder()
                                        errors.forEach {
                                            errorBuilder.append("${it.title} : ${it.detail} : ${it.code}\n")
                                        }

                                        showErrorAlert(errorBuilder.toString())
                                        tvProgress.text = errorBuilder.toString()
                                        pbProgress.visibility = View.INVISIBLE
                                    }
                                }

                                override fun onSuccess(bitmap: Bitmap) {
                                    runOnUiThread {
                                        ivOutput.setImageBitmap(bitmap)
                                        ivOutput.visibility = View.VISIBLE
                                        bProcess.visibility = View.INVISIBLE
                                        tvProgress.visibility = View.INVISIBLE
                                        pbProgress.visibility = View.INVISIBLE
                                        tvInstruction.visibility = View.INVISIBLE
                                        val name = inputImage!!.name.substring(
                                            0,
                                            inputImage!!.name.lastIndexOf(".")
                                        ) + "-no-bg"
                                        // Save output image
                                        saveImage(name, bitmap) {
                                            outputImage = it
                                            toast(resources.getString(R.string.img_saved, name))
                                        }
                                    }
                                }

                            })
                        }
                    }
                }
                // If image has "-no-bg" in its name, meaning its background had been removed
                else toastLong(getString(R.string.tv_removed))
            }
        } // If input image is null
        else toast(R.string.error_no_image_selected)
    }

    /**
     * To show an alert message with title 'Error'
     */
    private fun showErrorAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.title_error)
            .setMessage(message)
            .create()
            .show()
    }

    /**
     * To save given bitmap into a file
     */
    private fun saveImage(fileName: String, bitmap: Bitmap, onSaved: (file: File) -> Unit) {
        // Create project dir
        if (!projectDir.exists()) {
            projectDir.mkdir()
        }

        // Create inputImage file
        val imageFile = File("$projectDir/$fileName.png")
        imageFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        onSaved(imageFile)
    }

    /**
     * To compress given inputImage file with Glide
     */
    private fun compressImage(image: File, onLoaded: (bitmap: Bitmap) -> Unit) {

        Glide.with(this)
            .asBitmap()
            .load(image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onLoaded(resource)
                }
            })
    }

    /**
     * To check WRITE_EXTERNAL_STORAGE permission on clicking "Choose Image" button
     */
    private fun checkPermission(onPermissionChecked: () -> Unit) {

        val deniedListener = DialogOnDeniedPermissionListener.Builder.withContext(this)
            .withTitle(R.string.title_permission)
            .withMessage(R.string.message_permission)
            .withButtonText(R.string.action_ok)
            .build()

        val permissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                onPermissionChecked()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                toast(R.string.error_permission)
            }
        }

        val listener = CompositePermissionListener(permissionListener, deniedListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(listener)
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != actionCapturePhoto) {
            try {
                if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
                    // IMAGE PICKED!!
                    val imagePicked = ImagePicker.getFirstImageOrNull(data)

                    if (imagePicked != null && File(imagePicked.path).length()>0) {

                        inputImage = File(imagePicked.path)

                        ivInput.visibility = View.VISIBLE

                        Glide.with(this)
                            .load(inputImage)
                            .into(ivInput)

                        // Showing process button
                        bProcess.visibility = View.VISIBLE
                        clearInputDetails()
                        appendInputDetails(resources.getString(R.string.image, inputImage!!.name))
                        appendInputDetails(resources.getString(
                                R.string.original_size, inputImage!!.length() / 1024
                            )
                        )
//                ivOutput.visibility = View.INVISIBLE

                    } else {
                        toast(R.string.error_no_image_selected)
                    }
                }
            } catch (ex: Exception) {
                ex.message?.let { toast(it) }
            }
        } else try {
            if (resultCode == RESULT_OK) {
                val imageBitmap =
                    Images.Media.getBitmap(this.applicationContext.contentResolver, photoURI)
                ivInput.setImageBitmap(imageBitmap)
                ivInput.visibility = View.VISIBLE
                bProcess.visibility = View.VISIBLE
                clearInputDetails()
                appendInputDetails(resources.getString(R.string.image, inputImage!!.name))
                appendInputDetails(
                    resources.getString(
                        R.string.original_size,
                        inputImage!!.length() / 1024
                    )
                )
            }
        } catch (ex: Exception) {
            ex.message?.let { toast(it) }
        }
    }

    private fun toast(@StringRes message: Int) {
        toast(getString(message))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toastLong(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getColorFromResource(resId: Int): Int {
        val typedValue = TypedValue()
        getTheme().resolveAttribute(resId, typedValue, true);
        val color = typedValue.resourceId
        return resources.getColor(color)
    }
}
