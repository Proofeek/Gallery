package ru.proofeek.gallery

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home2.view.*
import ru.proofeek.gallery.databinding.ActivityMainBinding
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    lateinit var binding: ActivityMainBinding
    var timerImages: CountDownTimer? = null
    var timerOn: CountDownTimer? = null
    var timerOff: CountDownTimer? = null


    var textTime: TextView? = null
    var time: Long = 10000
    private var rightNow: Calendar = Calendar.getInstance()
    var sp: SharedPreferences? = null
    var bm: BatteryManager? = null
    var permissions =
        arrayOf("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE","android.permission.MANAGE_EXTERNAL_STORAGE")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        if (!checkPermission()) requestPermission()

        sp = PreferenceManager.getDefaultSharedPreferences(this)
        bm = this.getSystemService(BATTERY_SERVICE) as BatteryManager
        time = (sp?.getInt("bar_value", 10))!!.toLong() * 1000

        textTime = textViewTime
        scheduleJob()


    }

    private fun checkPermission(): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result =
                ContextCompat.checkSelfPermission(this@MainActivity, READ_EXTERNAL_STORAGE)
            val result1 =
                ContextCompat.checkSelfPermission(this@MainActivity, WRITE_EXTERNAL_STORAGE)
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(WRITE_EXTERNAL_STORAGE),
                80
            )
        }
    }


    /**
     * On/Off StartActivityReceiver
     *
     * Если [switchOn] true, то [StartActivityReceiver] станет активным.
     */
    fun startActivityReceiverSwitch(switchOn: Boolean) {
        val pm: PackageManager = this@MainActivity.packageManager
        val componentName = ComponentName(this@MainActivity, StartActivityReceiver::class.java)
        if (switchOn) {
            pm.setComponentEnabledSetting(
                componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            pm.setComponentEnabledSetting(
                componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * Создаёт Job Scheduler ([JobService])
     */
    private fun scheduleJob() {
        val componentName = ComponentName(this, JobService::class.java)
        val info = JobInfo.Builder(123, componentName)
            .setRequiresCharging(true)
            //.setOverrideDeadline(0)
            .build()
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = scheduler.schedule(info)
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled")
        } else {
            Log.d(TAG, "Job scheduling failed")
        }
    }

    fun cancelJob() {
        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(123)
        Log.d(TAG, "Job cancelled")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.main_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * Выключает кнопку "выбрать папку"
     */
    private fun setButtonDisable() {
        binding.mainFragment.buttonIm.isEnabled = false
        binding.mainFragment.buttonIm.isClickable = false
    }

    /**
     * @return текущий час
     */
    fun currentHour(): Int {
        rightNow = Calendar.getInstance()
        return rightNow.get(Calendar.HOUR_OF_DAY)
    }

    /**
     * @return текущую минуту
     */
    fun currentMinute(): Int {
        rightNow = Calendar.getInstance()
        return rightNow.get(Calendar.MINUTE)
    }

    /**
     * Проверяет включены ли в настройках функции "Включать в выбранное время" и "Выключать в выбранное время"
     * Если включены, запускает таймер отсчета до этого времени и выводит текст на экран
     */
    @SuppressLint("SetTextI18n")
    private fun isTimeFunctionsOn(imagesList: ArrayList<File>) {
        setButtonDisable()

        val timePickerOnHour = sp?.getInt("timePickerHourOn", 12)
        val timePickerOnMinute = sp?.getInt("timePickerMinuteOn", 0)
        val timePickerOffHour = sp?.getInt("timePickerHourOff", 16)
        val timePickerOffMinute = sp?.getInt("timePickerMinuteOff", 0)
        var isStarted = false

        if (sp?.getBoolean("switch_time_on", false) == true) {
            textTime?.text = resources.getString(R.string.imagesAppear) + " ${String.format("%02d",timePickerOnHour) }:${String.format("%02d",timePickerOnMinute)}"
            timerOn = object : CountDownTimer(1000, 1000) {
                override fun onTick(p0: Long) {
                    Log.e(
                        "ПУК",
                        "${currentHour()}:${currentMinute()} -- ${timePickerOnHour}:${timePickerOnMinute}"
                    )
                    if (timePickerOnHour == currentHour() && timePickerOnMinute == currentMinute()) {
                        imagesToScreen(imagesList)
                        isStarted = true
                        this.cancel()
                    }
                }

                override fun onFinish() {
                    this.start()
                }
            }.start()
        } else {
            imagesToScreen(imagesList)
        }

        if (sp?.getBoolean("switch_time_off", false) == true) {
            if(sp?.getBoolean("switch_time_on", false) == true){
            textTime?.text =
                "${textTime?.text}\n" + resources.getString(R.string.imagesDisappear) + " ${String.format("%02d",timePickerOffHour)}:${String.format("%02d",timePickerOffMinute)}"
            }
            timerOff = object : CountDownTimer(1000, 1000) {
                override fun onTick(p0: Long) {
                    if (timePickerOffHour == currentHour() && timePickerOffMinute == currentMinute() && isStarted) {
                        timerImages?.cancel()
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

    fun recreateActivity() {
        this.recreate()
    }


    /**
     * Выводит в ImageView получаемый [imagesList] с периодичностью, заданной в настройках
     */
    fun imagesToScreen(imagesList: ArrayList<File>) {
        textTime?.text = ""

        var step = 0
        timerImages?.cancel()
        timerImages = object : CountDownTimer(time, time) {
            override fun onTick(p0: Long) {
                val bitmap = BitmapFactory.decodeFile(imagesList[step].path)
                binding.mainFragment.imageView.setImageBitmap(bitmap)
                if (step + 1 >= imagesList.size) step = 0 else step++
            }

            override fun onFinish() {
                this.start()
            }
        }.start()

    }


    val dirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val fullpath = File(uri.lastPathSegment?.replace("raw:", ""))
            Log.w(TAG, "fullpath: " + fullpath)
            imageReaderNew(fullpath)
        }
    }


    /**
     * @return тип файла по его [пути][url]
     */
    private fun getMimeType(url: String?): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    /**
     * Загружает все изображения из папки по [пути][root] в [fileList].
     * Запускает [isTimeFunctionsOn]
     */
    private fun imageReaderNew(root: File) {
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
            /*
            Toast.makeText(
                this,
                "Выбрано ${fileList.size} изображений",
                Toast.LENGTH_SHORT
            ).show()
             */
            isTimeFunctionsOn(fileList)
        } else {
            Log.e(TAG, "NO FILES")
            Toast.makeText(
                this,
                R.string.noFiles,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Меняет булевую переменную "active" в SharedPreferences на true
     */
    override fun onStart() {
        sp!!.edit().putBoolean("active", true).apply()
        //cancelJob()
        Log.d(TAG, "active: "+ sp!!.getBoolean("active",false).toString())
        super.onStart()
    }

    /**
     * Меняет булевую переменную "active" в SharedPreferences на false.
     * Запускает [scheduleJob]
     */
    override fun onStop() {
        sp!!.edit().putBoolean("active", false).apply()

        if(!bm!!.isCharging) scheduleJob()

        Log.d(TAG, "active: "+ sp!!.getBoolean("active",false).toString())
        super.onStop()
    }
}