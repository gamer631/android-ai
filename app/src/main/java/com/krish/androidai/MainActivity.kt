package com.krish.androidai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var output: TextView
    private var torch = false
    private var camId: String? = null
    private val pickerCode = 44
    private val speechCode = 8
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(36,28,36,28) }
        status = TextView(this).apply { text="ANDROID AI\nReady • Nepali + English"; textSize=23f }
        output = TextView(this).apply { text="USB OTG helper and quick voice commands"; textSize=16f; setPadding(0,20,0,20) }
        layout.addView(status); layout.addView(output)
        layout.addView(btn("🎙 Voice command / बोल्नुहोस्") { listen() })
        layout.addView(btn("🔦 Flashlight toggle") { flashlight() })
        layout.addView(btn("🔋 Battery") { output.text=battery() })
        layout.addView(btn("🕒 Time") { output.text=clock() })
        layout.addView(btn("▶ YouTube") { openPackage("com.google.android.youtube") })
        layout.addView(btn("▶ Facebook") { openPackage("com.facebook.katana") })
        layout.addView(btn("USB: choose APK") { chooseApk() })
        layout.addView(btn("Start listening service") { startService(Intent(this,ListeningService::class.java)) })
        setContentView(layout)
        val missing=mutableListOf<String>()
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO)
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.POST_NOTIFICATIONS)
        if(missing.isNotEmpty()) requestPermissions(missing.toTypedArray(),7)
        try { val cm=getSystemService(CameraManager::class.java); camId=cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true } } catch(_:Exception){}
        if(intent?.action=="android.hardware.usb.action.USB_DEVICE_ATTACHED") showUsbDialog()
    }
    private fun btn(label:String, act:()->Unit)=Button(this).apply { text=label; setOnClickListener { act() } }
    private fun showUsbDialog() { status.text="USB device detected"; AlertDialog.Builder(this).setTitle("USB detected").setMessage("Select an APK from USB storage. Android requires confirmation before installing.").setPositiveButton("Choose APK") { _,_->chooseApk() }.setNegativeButton("Later",null).show() }
    private fun chooseApk() { try { val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/vnd.android.package-archive"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; startActivityForResult(i,pickerCode) } catch(e:Exception) { Toast.makeText(this,"Open Files and choose an APK on USB",Toast.LENGTH_LONG).show() } }
    @Deprecated("Legacy picker callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==pickerCode && resultCode==RESULT_OK) {
            val uri=data?.data ?: return
            val install=Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if(Build.VERSION.SDK_INT>=26 && !packageManager.canRequestPackageInstalls()) {
                AlertDialog.Builder(this).setTitle("Allow APK installs").setMessage("Allow Android AI to install apps from this source in Settings, then select the APK again.")
                    .setPositiveButton("Settings") { _,_->startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName"))) }.setNegativeButton("Cancel",null).show()
                return
            }
            try { startActivity(install); status.text="Installer opened — confirm Install" } catch(e:Exception){ status.text="Cannot open installer: ${e.message}" }
        } else if(requestCode==speechCode && resultCode==RESULT_OK) {
            val spoken=data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            processCommand(spoken)
        }
    }
    private fun listen() { if(!SpeechRecognizer.isRecognitionAvailable(this)) { output.text="Speech recognition unavailable"; return }; try { val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ne-NP").putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"ne-NP,en-US"); startActivityForResult(i,speechCode) } catch(e:Exception) { output.text="Speech start failed: ${e.message}" } }
    private fun processCommand(spoken:String) { output.text="Heard: $spoken"; val s=spoken.lowercase(); when { "battery" in s || "ब्याट्री" in s -> output.append("\n${battery()}"); "time" in s || "समय" in s -> output.append("\n${clock()}"); "torch" in s || "flashlight" in s || "टर्च" in s -> flashlight(); "youtube" in s -> openPackage("com.google.android.youtube"); "facebook" in s -> openPackage("com.facebook.katana"); else -> output.append("\nCommand not implemented yet") } }
    private fun flashlight() { try { val id=camId ?: run { output.text="No flashlight found"; return }; torch=!torch; getSystemService(CameraManager::class.java).setTorchMode(id,torch); output.text=if(torch) "Flashlight ON" else "Flashlight OFF" } catch(e:Exception){ output.text="Torch error: ${e.message}" } }
    private fun battery():String { val b=registerReceiver(null,android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)); val n=b?.getIntExtra("level",-1) ?: -1; return "Battery: $n%" }
    private fun clock()="Time: ${SimpleDateFormat("hh:mm a",Locale.getDefault()).format(Date())}"
    private fun openPackage(pkg:String) { val i=packageManager.getLaunchIntentForPackage(pkg); if(i!=null) startActivity(i) else output.text="App not installed" }
}
