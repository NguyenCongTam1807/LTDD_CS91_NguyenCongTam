package com.congtam.backgroundremover

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.congtam.backgroundremover.backend.RemoveBg
import com.congtam.backgroundremover.backend.utils.ErrorResponse
import com.congtam.backgroundremover.databinding.FragmentHomeBinding
import com.esafirm.imagepicker.features.ImagePicker
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    companion object {
        var folderMode: Boolean = true
    }

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
    private val authority = "com.congtam.backgroundremover.provider"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bChooseImage.setOnClickListener {
            onChooseImageClicked()
        }

        binding.iChooseImage.iChooseImage.setOnClickListener {
            onChooseImageClicked()
        }

        binding.bCaptureImage.setOnClickListener {
            val cameraPermission =
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            val storagePermission =
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            when {
                cameraPermission != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.CAMERA), cameraPermissionCode
                )
                storagePermission != PackageManager.PERMISSION_GRANTED -> ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionCode
                )
                else -> {
                    capturePhoto()
                }
            }
        }

        binding.bProcess.setOnClickListener{
            if (inputImage != null) {
                // Check permission
                checkPermission {
                    // permission granted
                    if (inputImage!!.name.contains("-no-bg").not()) {
                        binding.tvProgress.setText(R.string.status_compressing)
                        // compress the unprocessed inputImage now
                        compressImage(inputImage!!) { bitmap ->
                            saveImage(
                                "${SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SSS").format(Date())}" +
                                        "-compressed", bitmap
                            ) { compressedImage ->
                                val compressedImageSize = compressedImage.length() / 1024
                                val originalImageSize = inputImage!!.length() / 1024

                                binding.pbProgress.visibility = View.VISIBLE
                                binding.tvProgress.visibility = View.VISIBLE

                                binding.tvProgress.setText(R.string.status_uploading)
                                binding.pbProgress.progress = 0

                                val finalImage =
                                    if (compressedImageSize < originalImageSize) compressedImage else inputImage!!
                                appendInputDetails(
                                    resources.getString(
                                        R.string.compressed_size,
                                        finalImage.length() / 1024
                                    )
                                )

                                // inputImage saved, now upload
                                try {
                                    //try-catch to see RemoveBg API key has been initialized
                                    RemoveBg.from(finalImage, object : RemoveBg.RemoveBgCallback {
                                        override fun onProcessing() {
                                            requireActivity().runOnUiThread {
                                                binding.tvProgress.setText(R.string.status_processing)
                                            }
                                        }

                                        override fun onUploadProgress(progress: Float) {
                                            requireActivity().runOnUiThread {
                                                binding.tvProgress.text = "Uploading ${progress.toInt()}%"
                                                binding.pbProgress.progress = progress.toInt()
                                            }
                                        }

                                        override fun onError(errors: List<ErrorResponse.Error>) {
                                            requireActivity().runOnUiThread {
                                                val errorBuilder = StringBuilder()
                                                errors.forEach {
                                                    errorBuilder.append("${it.title} : ${it.detail} : ${it.code}\n")
                                                }
                                                showErrorAlert(errorBuilder.toString())
                                                binding.tvProgress.text = errorBuilder.toString()
                                                binding.pbProgress.visibility = View.INVISIBLE
                                            }
                                        }

                                        override fun onSuccess(bitmap: Bitmap) {
                                            requireActivity().runOnUiThread {
                                                binding.ivOutput.setImageBitmap(bitmap)
                                                binding.ivOutput.visibility = View.VISIBLE
                                                binding.bProcess.visibility = View.INVISIBLE
                                                binding.tvProgress.visibility = View.INVISIBLE
                                                binding.pbProgress.visibility = View.INVISIBLE
                                                binding.tvInstruction.visibility = View.INVISIBLE
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
                                } catch(e : IllegalArgumentException) {
                                    toast(R.string.no_api_key)
                                }
                            }
                        }
                    }
                    // If image has "-no-bg" in its name, meaning its background had been removed
                    else toastLong(getString(R.string.tv_removed))
                }
            } // If input image is null
            else toast(R.string.error_no_image_selected)
        }

        binding.ivInput.setOnClickListener {
            showActionAlert(requireContext(), inputImage!!)
        }

        binding.ivOutput.setOnClickListener {
            try {
                showActionAlert(requireContext(), outputImage!!)
            } catch (e: NullPointerException) {
                toast(R.string.error_output_not_saved)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater){
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onChooseImageClicked() {
        ImagePicker.create(this)
            .single()
            .theme(R.style.ImagePicker)
            .folderMode(folderMode)
            .toolbarArrowColor(getColorFromResource(android.R.attr.textColor))
            .imageFullDirectory(cameraDir.absolutePath)
            .start()
    }

    private fun capturePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
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
                        requireContext(),
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

    private fun checkPermission(onPermissionChecked: () -> Unit) {

        val deniedListener = DialogOnDeniedPermissionListener.Builder.withContext(requireContext())
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

        Dexter.withActivity(requireActivity())
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(listener)
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != actionCapturePhoto) {
            try {
                if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
                    // IMAGE PICKED!
                    val imagePicked = ImagePicker.getFirstImageOrNull(data)

                    if (imagePicked != null && File(imagePicked.path).length()>0) {

                        inputImage = File(imagePicked.path)

                        binding.ivInput.visibility = View.VISIBLE

                        Glide.with(this)
                            .load(inputImage)
                            .into(binding.ivInput)
                        binding.iChooseImage.iChooseImage.visibility = View.INVISIBLE
                        // Showing process button
                        binding.bProcess.visibility = View.VISIBLE
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
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val imageBitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().applicationContext.contentResolver, photoURI)
                binding.ivInput.setImageBitmap(imageBitmap)
                binding.ivInput.visibility = View.VISIBLE
                binding.bProcess.visibility = View.VISIBLE
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

    private fun compressImage(image: File, onLoaded: (bitmap: Bitmap) -> Unit) {
        Glide.with(this)
            .asBitmap()
            .load(image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onLoaded(resource)
                }
            })
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

    private fun showErrorAlert(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_error)
            .setMessage(message)
            .create()
            .show()
    }

    private fun showActionAlert(context: Context, image: File) {
        val alert = androidx.appcompat.app.AlertDialog.Builder(context).create()
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
        alert.getButton(DialogInterface.BUTTON_NEUTRAL)
            .setTextColor(getColorFromResource(R.attr.itemTextColor))
        alert.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(getColorFromResource(R.attr.colorAccent))
        alert.getButton(DialogInterface.BUTTON_POSITIVE)
            .setTextColor(getColorFromResource(R.attr.colorAccent))
        alert.show()
    }

    private fun viewImage(inputImage: File) {
        val uri = FileProvider.getUriForFile(requireContext(), authority, inputImage)
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
        val contentUri = FileProvider.getUriForFile(requireContext(), authority, newFile)
        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, requireActivity().contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun appendInputDetails(details: String) {
        binding.tvInputDetails.text = "${binding.tvInputDetails.text}\n$details"
    }

    private fun clearInputDetails() {
        binding.tvInputDetails.text = ""
    }

    private fun toast(@StringRes message: Int) {
        toast(getString(message))
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun toastLong(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun getColorFromResource(resId: Int): Int {
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(resId, typedValue, true);
        val color = typedValue.resourceId
        return resources.getColor(color)
    }
}