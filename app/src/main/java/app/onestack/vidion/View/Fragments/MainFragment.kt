package app.onestack.vidion.View.Fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import app.onestack.vidion.Adapters.FolderAdapter
import app.onestack.vidion.Adapters.VideoAdapter
import app.onestack.vidion.Models.Folder
import app.onestack.vidion.Models.Video
import app.onestack.vidion.R
import app.onestack.vidion.Utils.loadPositionListPref
import app.onestack.vidion.Utils.loadSortSharePref
import app.onestack.vidion.Utils.saveSortPref
import app.onestack.vidion.View.Activities.MainActivity
import app.onestack.vidion.View.Activities.SearchActivity
import app.onestack.vidion.databinding.FragmentMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

open class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private lateinit var adapter: VideoAdapter
    private val dialog = BottomSheetDialog(MainActivity.activity)
    private lateinit var btnSortNameAZ: ConstraintLayout
    private lateinit var btnSortNameZA: ConstraintLayout
    private lateinit var btnSortNewToOld: ConstraintLayout
    private lateinit var btnSortOldToNew: ConstraintLayout
    private lateinit var btnClose: ImageView
    private lateinit var tvSortNameAZ: TextView
    private lateinit var tvSortNameZA: TextView
    private lateinit var tvSortNewToOld: TextView
    private lateinit var tvSortOldToNew: TextView
    private var grid: Boolean = false
    private var sortName: String = "A-Z"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        binding = FragmentMainBinding.bind(view)

        setUpBottomNav()

        binding.rvVideos.setHasFixedSize(true)
        binding.rvVideos.setItemViewCacheSize(100)
        binding.rvVideos.isNestedScrollingEnabled = false


        CoroutineScope(Main).launch {
            getVideos(sortName, false)
            getFolders()
        }

        binding.btnSearch.setOnClickListener {
            startActivity(Intent(MainActivity.activity, SearchActivity::class.java))
        }

        binding.btnShow.setOnClickListener {
            if (grid) {
                getVideos(sortName, false)
                grid = false
                binding.icListGrid.setImageResource(R.drawable.ic_show)
            } else {
                getVideos(sortName, true)
                grid = true
                binding.icListGrid.setImageResource(R.drawable.ic_list)
            }
        }

        return view
    }

    private fun getFolders() {
        binding.rvListFolder.setHasFixedSize(true)
        binding.rvListFolder.setItemViewCacheSize(100)
        binding.rvListFolder.isNestedScrollingEnabled = false
        binding.rvListFolder.layoutManager =
            LinearLayoutManager(MainActivity.activity, LinearLayoutManager.HORIZONTAL, false)

        val fList = ArrayList<Folder>()
        for (folder in MainActivity.folderList) {
            fList.add(folder)
        }

        fList.sortBy { it.folderName.lowercase(Locale.getDefault()) }

        MainActivity.folderList = fList

        binding.rvListFolder.adapter = FolderAdapter(MainActivity.activity, MainActivity.folderList)

        //binding.totalFolders.text = "Total Folders: ${MainActivity.folderList .size}"
    }

    private fun getVideos(sort: String, grid: Boolean) {
        binding.rvVideos.adapter = null
        if (grid) {
            binding.rvVideos.layoutManager =
                GridLayoutManager(MainActivity.activity, 4, GridLayoutManager.VERTICAL, false)
        } else {
            binding.rvVideos.layoutManager =
                LinearLayoutManager(MainActivity.activity, LinearLayoutManager.VERTICAL, false)
        }

        val vList = ArrayList<Video>()
        MainActivity.sortList = ArrayList()
        for (video in MainActivity.videoList) {
            vList.add(video)
        }

        MainActivity.sort = true

        when (sort) {
            "A-Z" -> vList.sortBy { it.title.lowercase(Locale.getDefault()) }
            "Z-A" -> vList.sortByDescending { it.title.lowercase(Locale.getDefault()) }
            "New" -> vList.sortByDescending { it.dateAdded }
            "Old" -> vList.sortBy { it.dateAdded }
        }
        MainActivity.sortList = vList

        try {
            if (MainActivity.sortList.size > 0) {
                if (grid) {
                    adapter =
                        VideoAdapter(MainActivity.activity, MainActivity.sortList, gridView = true)
                    CoroutineScope(Main).launch {
                        binding.rvVideos.adapter = adapter
                        /* binding.rvVideos.layoutManager =
                             GridLayoutManager(MainActivity.activity, 4, GridLayoutManager.VERTICAL, false)*/
                    }

                } else {
                    adapter = VideoAdapter(MainActivity.activity, MainActivity.sortList)
                    binding.rvVideos.adapter = adapter
                }


            } else {
                binding.animNoFile.visibility = View.VISIBLE
                binding.tvNoFile.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.i("", "")
        }
    }

    private fun setUpBottomNav() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_sort, null)

        btnSortNameAZ = view.findViewById(R.id.btnSortNameAZ)
        btnSortNameZA = view.findViewById(R.id.btnSortNameZA)
        btnSortNewToOld = view.findViewById(R.id.btnSortDateNew)
        btnSortOldToNew = view.findViewById(R.id.btnSortDateOld)
        btnClose = view.findViewById(R.id.btnCloseBottomSheet)
        tvSortNameAZ = view.findViewById(R.id.tvSortNameAZ)
        tvSortNameZA = view.findViewById(R.id.tvSortNameZA)
        tvSortNewToOld = view.findViewById(R.id.tvSortDateNew)
        tvSortOldToNew = view.findViewById(R.id.tvSortDAteOld)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSortNameAZ.setOnClickListener {
            configSortAZ()
        }

        btnSortNameZA.setOnClickListener {
            configSortZA()
        }

        btnSortNewToOld.setOnClickListener {
            configSortNew()
        }

        btnSortOldToNew.setOnClickListener {
            configSortOld()
        }

        dialog.setContentView(view)

        binding.btnSort.setOnClickListener { dialog.show() }
    }

    private fun configSortAZ() {
        btnSortNameAZ.setBackgroundResource(R.drawable.back_item_sort_selected)
        tvSortNameAZ.setTextColor(Color.parseColor("#6262FF"))
        btnSortNameZA.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNameZA.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortNewToOld.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNewToOld.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortOldToNew.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortOldToNew.setTextColor(Color.parseColor("#BBBBBB"))
        sortName = "A-Z"
        saveSortPref(MainActivity.activity, "A-Z")
        getVideos(sortName, grid)
        dialog.dismiss()
        CoroutineScope(Main).launch { goPosition() }
    }

    private fun configSortZA() {
        btnSortNameAZ.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNameAZ.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortNameZA.setBackgroundResource(R.drawable.back_item_sort_selected)
        tvSortNameZA.setTextColor(Color.parseColor("#6262FF"))
        btnSortNewToOld.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNewToOld.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortOldToNew.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortOldToNew.setTextColor(Color.parseColor("#BBBBBB"))
        sortName = "Z-A"
        saveSortPref(MainActivity.activity, "Z-A")
        getVideos(sortName, grid)
        dialog.dismiss()
        CoroutineScope(Main).launch { goPosition() }
    }

    private fun configSortNew() {
        btnSortNameAZ.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNameAZ.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortNameZA.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNameZA.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortNewToOld.setBackgroundResource(R.drawable.back_item_sort_selected)
        tvSortNewToOld.setTextColor(Color.parseColor("#6262FF"))
        btnSortOldToNew.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortOldToNew.setTextColor(Color.parseColor("#BBBBBB"))
        sortName = "New"
        saveSortPref(MainActivity.activity, "New")
        getVideos(sortName, grid)
        dialog.dismiss()
        CoroutineScope(Main).launch { goPosition() }
    }

    private fun configSortOld() {
        btnSortNameAZ.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNameAZ.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortNameZA.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNameZA.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortNewToOld.setBackgroundResource(R.drawable.back_item_sort_unselect)
        tvSortNewToOld.setTextColor(Color.parseColor("#BBBBBB"))
        btnSortOldToNew.setBackgroundResource(R.drawable.back_item_sort_selected)
        tvSortOldToNew.setTextColor(Color.parseColor("#6262ff"))
        sortName = "Old"
        saveSortPref(MainActivity.activity, "Old")
        getVideos(sortName, grid)
        dialog.dismiss()
        CoroutineScope(Main).launch { goPosition() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun goPosition() {
        delay(20)
        val staggeredGridLayoutManager =
            LinearLayoutManager(MainActivity.activity, LinearLayoutManager.VERTICAL, false)
        staggeredGridLayoutManager.scrollToPosition(loadPositionListPref(MainActivity.activity))
        if (grid) {
            binding.rvVideos.layoutManager =
                GridLayoutManager(MainActivity.activity, 4, GridLayoutManager.VERTICAL, false)
            (binding.rvVideos.layoutManager as GridLayoutManager).scrollToPosition(
                loadPositionListPref(MainActivity.activity)
            )
        } else {
            binding.rvVideos.layoutManager =
                LinearLayoutManager(MainActivity.activity, LinearLayoutManager.VERTICAL, false)
            (binding.rvVideos.layoutManager as LinearLayoutManager).scrollToPosition(
                loadPositionListPref(MainActivity.activity)
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        //if (PlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
        if (MainActivity.dataChange) adapter.notifyDataSetChanged()
        MainActivity.adapterChange = false

        if (loadSortSharePref(MainActivity.activity) == "A-Z")
            configSortAZ()
        else if (loadSortSharePref(MainActivity.activity) == "Z-A")
            configSortZA()
        else if (loadSortSharePref(MainActivity.activity) == "New")
            configSortNew()
        else if (loadSortSharePref(MainActivity.activity) == "Old")
            configSortOld()
    }
}