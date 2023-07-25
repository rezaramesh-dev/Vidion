package app.onestack.vidion.View.Activities

import android.annotation.SuppressLint
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.onestack.vidion.Models.Video
import app.onestack.vidion.Adapters.VideoAdapter
import app.onestack.vidion.Models.Folder
import app.onestack.vidion.R
import app.onestack.vidion.databinding.ActivityFoldersBinding
import java.io.File
import java.util.concurrent.TimeUnit

class FoldersActivity : AppCompatActivity() {

    companion object {
        lateinit var currentFolderVideo: ArrayList<Video>
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTheme(R.style.coolPinKNav)

        val position = intent.getIntExtra("position", 0)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = MainActivity.folderList[position].folderName

        currentFolderVideo = getVideoMediaStore(MainActivity.folderList[position].id)

        binding.rvVideo.setHasFixedSize(true)
        binding.rvVideo.setItemViewCacheSize(10)
        binding.rvVideo.itemAnimator = DefaultItemAnimator()
        binding.rvVideo.layoutManager =
            LinearLayoutManager(this@FoldersActivity, LinearLayoutManager.VERTICAL, false)
        binding.rvVideo.adapter = VideoAdapter(
            context = this@FoldersActivity,
            videoList = currentFolderVideo,
            isFolder = true,
        )

        binding.btnBack.setOnClickListener { finish() }

        binding.tvFolderName.text = intent.getStringExtra("folderName")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    private fun getVideoMediaStore(folderId: String): ArrayList<Video> {
        val videoList = ArrayList<Video>()
        val tempFolderList = ArrayList<String>()

        val selection = MediaStore.Video.Media.BUCKET_ID + " like? "

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
        //val selection = "${MediaStore.Video.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
        )

// Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val query = contentResolver.query(collection, projection, selection, arrayOf(folderId), sortOrder)
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
                    MainActivity.folderList.add(Folder(id = folderId, folderName = folder))
                }
            }
            cursor.close()
        }
        return videoList
    }
}