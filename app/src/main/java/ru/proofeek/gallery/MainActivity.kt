package ru.proofeek.gallery

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {


    val IMAGE_ERQUEST_CODE = 100

    private lateinit var button: Button
    private lateinit var imageView: ImageView
    private lateinit var path2: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        supportActionBar?.hide()
        checkPermission()
        button = findViewById(R.id.button)
        imageView = findViewById(R.id.imageView)

        button.setOnClickListener{
            dirRequest.launch(null)
        }



    }

    fun setButtonEnabled(bool: Boolean){
        button.isEnabled = bool
        button.isClickable = bool
    }

    fun imagesToScreen(imagesList: ArrayList<File>){

        setButtonEnabled(false)
        var step = 0
            //Log.e("fger", file.path)
            val timer = object: CountDownTimer(1000, 1000) {
                override fun onTick(p0: Long) {
                }
                override fun onFinish() {
                    val bitmap = BitmapFactory.decodeFile(imagesList[step].path)
                    imageView.setImageBitmap(bitmap)
                    Log.e("Number", step.toString())
                    if(step + 1 >= imagesList.size) step = 0 else step++
                    this.start()
                }
            }.start().onFinish()


            Log.e("LOLOLP", "p1")

    }


    private val dirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            // call this to persist permission across decice reboots
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // do your stuff
            val fullpath = File(uri.lastPathSegment?.replace("raw:",""))
            Log.w("fullpath", "" + fullpath)
            imageReaderNew(fullpath)
        }
    }
    fun getMimeType(url: String?): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        //Log.e("TYPE", type.toString())
        return type
    }

    private fun imageReaderNew(root: File) {
        Log.e("GG", "TI ZDEC")
        val fileList: ArrayList<File> = ArrayList()
        val listAllFiles = root.listFiles()

        if (listAllFiles != null && listAllFiles.isNotEmpty()) {
            for (currentFile in listAllFiles) {
                if (getMimeType(currentFile.path)?.contains("image", ignoreCase = true) == true) {
                    // File absolute path
                    Log.e("downloadFilePath", currentFile.getAbsolutePath())
                    // File Name
                    Log.e("downloadFileName", currentFile.getName())
                    fileList.add(currentFile.absoluteFile)
                }
            }
            Log.w("fileList", "" + fileList.size)
            Toast.makeText(this, "Выбрано ${fileList.size.toString()} изображений", Toast.LENGTH_SHORT).show()
            imagesToScreen(fileList)
        }
        else{
            Log.e("AGA", "NO FILES")
        }
    }


    private fun checkPermission(){
        if(!Utils.isPermissionGranted(this)){
            AlertDialog.Builder(this)
                .setTitle("All files permission")
                .setMessage("We need all file permissions")
                .setPositiveButton("Allow",
                    DialogInterface.OnClickListener { dialog, which ->
                        takePermission()
                    })
                .setNegativeButton("Deny",DialogInterface.OnClickListener{ dialog, which ->
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }else{
            //Toast.makeText(this, "Permission Already granted", Toast.LENGTH_LONG).show()
        }
    }





    fun takePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) run {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                val uri = Uri.fromParts("package", packageName, null)
                intent.setData(uri)
                startActivityForResult(intent, 101)

            } catch (e: Exception) {
                e.printStackTrace()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 101)
            }
        }else{
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this,permissions,101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults.isNotEmpty() && requestCode==101){
            val readExt = grantResults[0]==PackageManager.PERMISSION_GRANTED
            if(!readExt){
                takePermission()
            }
        }
    }
}