package app.onestack.vidion.Adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.onestack.vidion.Models.Folder
import app.onestack.vidion.View.Activities.FoldersActivity
import app.onestack.vidion.databinding.ItemListFolderBinding

class FolderAdapter(private val context: Context, private val folderList: ArrayList<Folder>) :
    RecyclerView.Adapter<FolderAdapter.MyHolder>() {

    class MyHolder(binding: ItemListFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        val folderName = binding.tvFolderName
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(ItemListFolderBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.folderName.text = folderList[position].folderName
        holder.root.setOnClickListener {
            val intent = Intent(context, FoldersActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("folderName", folderList[position].folderName)
            ContextCompat.startActivity(context, intent, null)
        }
    }

    override fun getItemCount(): Int {
        return folderList.size
    }
}