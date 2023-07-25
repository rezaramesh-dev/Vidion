package app.onestack.vidion.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import app.onestack.vidion.Models.Video
import app.onestack.vidion.R
import app.onestack.vidion.View.Activities.FoldersActivity
import app.onestack.vidion.View.Activities.MainActivity
import app.onestack.vidion.View.Activities.PlayerActivity
import app.onestack.vidion.databinding.DetailsViewBinding
import app.onestack.vidion.databinding.ItemListVideosBinding
import app.onestack.vidion.databinding.RenameFieldBinding
import app.onestack.vidion.databinding.VideoMoreFeaturesBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class VideoAdapter(
    private val context: Context,
    private var videoList: ArrayList<Video>,
    private val isFolder: Boolean = false,
    private val gridView: Boolean = false,
) :
    RecyclerView.Adapter<VideoAdapter.MyHolder>() {

    class MyHolder(binding: ItemListVideosBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.tvVideoName
        val folder = binding.nameFolder
        val duration = binding.tvTimeVideo
        val image = binding.imageVideo
        val btnPlay = binding.btnPlay
        val backDu = binding.backDu
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemListVideosBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("ResourceType", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = videoList[position].title
        holder.folder.text = videoList[position].folderName
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)

        if (gridView) {
            holder.btnPlay.visibility = View.GONE
            holder.title.visibility = View.GONE
            holder.folder.visibility = View.GONE
            holder.duration.visibility = View.GONE
            holder.backDu.visibility = View.GONE
        }

        Glide.with(context)
            .asBitmap()
            .skipMemoryCache(true)
            .load(videoList[position].artUri)
            .dontAnimate()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(RequestOptions().placeholder(R.drawable.image_video))
            .into(holder.image)

        holder.root.setOnClickListener {
            when {
                videoList[position].id == PlayerActivity.nowPlayingId -> {
                    sendIntent(position = position, ref = "NowPlaying")
                }
                isFolder -> {
                    PlayerActivity.pipStatus = 1
                    sendIntent(position = position, ref = "FolderActivity")
                }
                MainActivity.search -> {
                    PlayerActivity.pipStatus = 2
                    sendIntent(position = position, ref = "SearchedVideos")
                }
                MainActivity.sort -> {
                    PlayerActivity.pipStatus = 3
                    sendIntent(position = position, ref = "SortVideos")
                }
                else -> {
                    PlayerActivity.pipStatus = 4
                    sendIntent(position = position, ref = "AllVideos")
                }
            }
        }

        holder.root.setOnLongClickListener {
            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.video_more_features, holder.root, false)
            val bindingMF = VideoMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog).create()
            dialog.show()

            bindingMF.renameBtn.setOnClickListener {
                requestPermissionR()
                dialog.dismiss()
                val customDialogRF = LayoutInflater.from(context)
                    .inflate(R.layout.rename_field, holder.root, false)
                val bindingRF = RenameFieldBinding.bind(customDialogRF)
                val dialogRF = MaterialAlertDialogBuilder(context).setView(customDialogRF)
                    .setCancelable(false)
                    .setPositiveButton("Rename") { self, _ ->
                        val currentFile = File(videoList[position].path)
                        val newName = bindingRF.renameField.text
                        if (newName != null && currentFile.exists() && newName.toString()
                                .isNotEmpty()
                        ) {
                            val newFile = File(
                                currentFile.parentFile,
                                newName.toString() + "." + currentFile.extension
                            )
                            if (currentFile.renameTo(newFile)) {
                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(newFile.toString()),
                                    arrayOf("video/*"),
                                    null
                                )
                                when {
                                    MainActivity.search -> {
                                        MainActivity.searchList[position].title = newName.toString()
                                        MainActivity.searchList[position].path = newFile.path
                                        MainActivity.searchList[position].artUri =
                                            Uri.fromFile(newFile)
                                        notifyItemChanged(position)
                                    }
                                    isFolder -> {
                                        FoldersActivity.currentFolderVideo[position].title =
                                            newName.toString()
                                        FoldersActivity.currentFolderVideo[position].path =
                                            newFile.path
                                        FoldersActivity.currentFolderVideo[position].artUri =
                                            Uri.fromFile(newFile)
                                        notifyItemChanged(position)
                                        MainActivity.dataChange = false
                                    }
                                    else -> {
                                        MainActivity.videoList[position].title = newName.toString()
                                        MainActivity.videoList[position].path = newFile.path
                                        MainActivity.videoList[position].artUri =
                                            Uri.fromFile(newFile)
                                        notifyItemChanged(position)
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Permission Denied!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("Cancel") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogRF.show()
                bindingRF.renameField.text =
                    SpannableStringBuilder.valueOf(videoList[position].title)
            }

            bindingMF.shareBtn.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                ContextCompat.startActivity(
                    context,
                    Intent.createChooser(shareIntent, "Share Video File"),
                    null
                )
            }

            bindingMF.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF = LayoutInflater.from(context)
                    .inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF = MaterialAlertDialogBuilder(context).setView(customDialogIF)
                    .setCancelable(false)
                    .setPositiveButton("OK") { self, _ ->

                        self.dismiss()
                    }
                    .create()
                //dialogIF.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }
                    .append(videoList[position].title)
                    .bold { append("\n\nDuration: ") }
                    .append(DateUtils.formatElapsedTime(videoList[position].duration / 1000))
                    .bold { append("\n\nFile Size: ") }.append(
                        Formatter.formatShortFileSize(
                            context,
                            videoList[position].size.toLong()
                        )
                    )
                    .bold { append("\n\nLocation") }.append(videoList[position].path)
                bindingIF.detailTV.text = infoText

                dialogIF.show()
            }

            bindingMF.deleteBtn.setOnClickListener {
                dialog.dismiss()
                val dialogRF = MaterialAlertDialogBuilder(context)
                    .setCancelable(false)
                    .setTitle("Delete Video?")
                    .setMessage(videoList[position].title)
                    .setPositiveButton("Yes") { self, _ ->
                        val file = File(videoList[position].path)
                        if (file.exists() && file.delete()) {
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(file.path),
                                arrayOf("video/*"),
                                null
                            )
                            when {
                                MainActivity.search -> {
                                    MainActivity.dataChange = true
                                    videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                                isFolder -> {
                                    MainActivity.dataChange = true
                                    FoldersActivity.currentFolderVideo.removeAt(position)
                                }
                                else -> {
                                    MainActivity.videoList.removeAt(position)
                                    notifyDataSetChanged()
                                }
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("No") { self, _ ->
                        self.dismiss()
                    }
                    .create()
                dialogRF.show()
            }
            return@setOnLongClickListener true
        }

    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    private fun sendIntent(position: Int, ref: String) {
        PlayerActivity.position = position
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<Video>) {
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }

    private fun requestPermissionR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.applicationContext.packageName}")
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }
}





