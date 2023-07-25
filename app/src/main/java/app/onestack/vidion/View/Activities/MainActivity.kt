package app.onestack.vidion.View.Activities

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import app.onestack.vidion.Models.Folder
import app.onestack.vidion.Models.Video
import app.onestack.vidion.R
import app.onestack.vidion.View.Fragments.MainFragment
import app.onestack.vidion.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val FAINAL_CODE_REQUEST: Int = 1

    //lateinit var toggle: ActionBarDrawerToggle
    private var runnable: Runnable? = null
    lateinit var dialog: BottomSheetDialog
    private lateinit var btnSetting: TextView
    private lateinit var btnPermission: ConstraintLayout

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var activity: MainActivity
        lateinit var videoList: ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
        lateinit var searchList: ArrayList<Video>
        lateinit var sortList: ArrayList<Video>
        var search: Boolean = false
        var sort: Boolean = false
        var dataChange: Boolean = false
        var adapterChange: Boolean = false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activity = this
        setUpBottomNav()
        readStoragePermission()
    }

    private fun setFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentFL, fragment)
        transaction.disallowAddToBackStack()
        transaction.commit()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getData() {
        folderList = ArrayList()
        videoList = getVideoMediaStore()
        setFragment(MainFragment())

        runnable = Runnable {
            if (dataChange) {
                videoList = getVideoMediaStore()
                dataChange = false
                adapterChange = true
            }
            try {
                Handler(Looper.getMainLooper()).postDelayed(runnable!!, 200)
            } catch (e: Exception) {
                Log.i("ERROR", e.message.toString())
            }
        }
        try {
            if (runnable != null) Handler(Looper.getMainLooper()).postDelayed(runnable!!, 0)
        } catch (e: Exception) {
            Log.i("ERROR", e.message.toString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == FAINAL_CODE_REQUEST) {
            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    folderList = ArrayList()
                    CoroutineScope(Main).launch {
                        videoList = getVideoMediaStore()
                        setFragment(MainFragment())
                        dialog.dismiss()
                    }
                } else {
                    readStoragePermission()
                    videoList = getVideoMediaStore()
                    setFragment(MainFragment())
                    dialog.dismiss()
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setUpBottomNav() {
        dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_permission_access_files, null)

        btnPermission = view.findViewById(R.id.btnPermission)
        btnSetting = view.findViewById(R.id.btnSetting)

        dialog.setContentView(view)
        dialog.setCancelable(false)

        btnPermission.setOnClickListener { readStoragePermission() }

        btnSetting.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                Log.i("", "")
            }
        }
    }

    private fun readStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Dexter.withContext(this@MainActivity).withPermission(READ_MEDIA_VIDEO)
                .withListener(object : PermissionListener {
                    @RequiresApi(Build.VERSION_CODES.M)
                    override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity, READ_MEDIA_VIDEO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) getData()
                    }

                    override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {
                        dialog.show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissionRequest: PermissionRequest,
                        permissionToken: PermissionToken
                    ) {
                        permissionToken.continuePermissionRequest()
                    }
                }).withErrorListener { }.check()
        else
            Dexter.withContext(this@MainActivity).withPermission(READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    @RequiresApi(Build.VERSION_CODES.M)
                    override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity, READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) getData()
                    }

                    override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {
                        dialog.show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissionRequest: PermissionRequest,
                        permissionToken: PermissionToken
                    ) {
                        permissionToken.continuePermissionRequest()
                    }
                }).withErrorListener { }.check()
    }

    private fun getVideoMediaStore(): ArrayList<Video> {
        val videoList = ArrayList<Video>()
        val tempFolderList = ArrayList<String>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID
        )

// Show only videos that are at least 5 minutes in duration.
        val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
        )

// Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val query = contentResolver.query(collection, projection, null, null, sortOrder)
        query?.use { cursor ->

            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val folderColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val folderIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateVideoAdded = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                val id = cursor.getString(idColumn)
                val name = cursor.getString(titleColumn)
                val folder = cursor.getString(folderColumn)
                val folderId = cursor.getString(folderIdColumn)
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(pathColumn)
                val size = cursor.getInt(sizeColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toLong()
                )
                val file = File(path.toString())
                val artUriC = Uri.fromFile(file)

                // Stores column values and the contentUri in a local object
                // that represents the media file.

                videoList += Video(
                    title = name,
                    id = id,
                    folderName = folder,
                    duration = duration,
                    size = size.toString(),
                    path = path.toString(),
                    artUri = artUriC,
                    dateAdded = dateVideoAdded.toString()
                )

                if (!tempFolderList.contains(folder)) {
                    tempFolderList.add(folder)
                    folderList.add(Folder(id = folderId, folderName = folder))
                }
            }
            cursor.close()
        }
        return videoList
    }
}