package ru.proofeek.gallery

import android.app.Activity
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home2.view.*
import ru.proofeek.gallery.databinding.ActivityMainBinding
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {


    val IMAGE_ERQUEST_CODE = 100

    lateinit var binding: ActivityMainBinding
    var timer: CountDownTimer? = null
    private val dataModel: DataModel by viewModels()
    var time: Long = 10000
    private var rightNow: Calendar = Calendar.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        supportActionBar?.hide()
        checkPermission()
/*
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_fragment, HomeFragment() )
            .commit()
*/

        dataModel.imageDuration.observe(this){
            time = it.toLong() * 1000
        }

        //Log.e("CurrentTime","${currentHour}:${currentMinute}")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.main_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun setButtonEnabled(bool: Boolean){
        binding.mainFragment.buttonIm.isEnabled = bool
        binding.mainFragment.buttonIm.isClickable = bool
    }

    fun currentHour():Int{
        rightNow = Calendar.getInstance()
        return rightNow.get(Calendar.HOUR_OF_DAY)
    }
    fun currentMinute():Int{
        rightNow = Calendar.getInstance()
        return rightNow.get(Calendar.MINUTE)
    }

    fun isTimeFunctionsOn(imagesList: ArrayList<File>){
        setButtonEnabled(false)
        val timePickerOnHour = dataModel.timePickerHourOn.value
        val timePickerOnMinute = dataModel.timePickerMinuteOn.value
        val timePickerOffHour = dataModel.timePickerHourOff.value
        val timePickerOffMinute = dataModel.timePickerMinuteOff.value

        var isStarted = false
        if(dataModel.isTimeOn.value == true){
            textViewTime.text ="Изображения появятся\nв ${dataModel.timePickerHourOn.value}:${dataModel.timePickerMinuteOn.value}"
            object : CountDownTimer(1000, 1000) {
                override fun onTick(p0: Long) {
                    Log.e("ПУК", "${currentHour()}:${currentMinute()} -- ${dataModel.timePickerHourOn.value}:${dataModel.timePickerMinuteOn.value}")
                    if(timePickerOnHour == currentHour() && timePickerOnMinute == currentMinute()){
                        imagesToScreen(imagesList)
                        isStarted = true
                        this.cancel()
                    }
                }
                override fun onFinish() {
                    this.start()
                }
            }.start()
        }else{
            imagesToScreen(imagesList)
        }

        if(dataModel.isTimeOff.value == true){
            textViewTime.text ="${textViewTime.text}\nи исчезнут в ${dataModel.timePickerHourOff.value}:${dataModel.timePickerMinuteOff.value}"
            object : CountDownTimer(1000, 1000) {
                override fun onTick(p0: Long) {
                    if(timePickerOffHour == currentHour() && timePickerOffMinute == currentMinute() && isStarted){
                        timer?.cancel()
                        recreateActivity()
                        this.cancel()
                    }
                }
                override fun onFinish() {
                    this.start()
                }
            }.start()
        }
    }

    fun recreateActivity(){
        this.recreate()
    }


    fun imagesToScreen(imagesList: ArrayList<File>){
        textViewTime.text =""

        var step = 0
            Log.e("СЕКУНДЫ", (time/1000).toString())
            timer?.cancel()
            timer = object : CountDownTimer(time, time) {
                override fun onTick(p0: Long) {
                    val bitmap = BitmapFactory.decodeFile(imagesList[step].path)
                    binding.mainFragment.imageView.setImageBitmap(bitmap)
                    Log.e("Number", step.toString())
                    if(step + 1 >= imagesList.size) step = 0 else step++
                }
                override fun onFinish() {
                    this.start()
                }
            }.start()


            Log.e("LOLOLP", "p1")
    }


    val dirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
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
            isTimeFunctionsOn(fileList)
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