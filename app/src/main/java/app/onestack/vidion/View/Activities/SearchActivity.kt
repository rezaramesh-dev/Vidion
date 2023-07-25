package app.onestack.vidion.View.Activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import app.onestack.vidion.Adapters.VideoAdapter
import app.onestack.vidion.Models.Video
import app.onestack.vidion.databinding.ActivitySearchBinding
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvVideos.setHasFixedSize(true)
        binding.rvVideos.setItemViewCacheSize(100)
        binding.rvVideos.itemAnimator = DefaultItemAnimator()
        binding.rvVideos.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        try {

            val vList = ArrayList<Video>()
            MainActivity.sortList = ArrayList()
            for (video in MainActivity.videoList) {
                vList.add(video)
            }

            MainActivity.sort = true

            vList.sortBy { it.title.lowercase(Locale.getDefault()) }

            MainActivity.sortList = vList

            if (MainActivity.sortList.size > 0) {
                adapter = VideoAdapter(this, MainActivity.sortList)
                binding.rvVideos.adapter = adapter
                binding.animNoFile.visibility = View.GONE
                binding.tvNoFile.visibility = View.GONE
                binding.rvVideos.visibility = View.VISIBLE
            } else {
                binding.rvVideos.visibility = View.GONE
                binding.animNoFile.visibility = View.VISIBLE
                binding.tvNoFile.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
        }

        binding.editVideo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (binding.editVideo.length() > 0) {
                    MainActivity.searchList = ArrayList()
                    for (video in MainActivity.sortList) {
                        if (video.title.lowercase().contains(s.toString().lowercase()))
                            MainActivity.searchList.add(video)
                    }
                    MainActivity.search = true
                    if (MainActivity.searchList.size > 0) {
                        adapter.updateList(searchList = MainActivity.searchList)
                        binding.rvVideos.visibility = View.VISIBLE
                        binding.animNoFile.visibility = View.GONE
                        binding.tvNoFile.visibility = View.GONE
                    } else {
                        binding.rvVideos.visibility = View.GONE
                        binding.animNoFile.visibility = View.VISIBLE
                        binding.tvNoFile.visibility = View.VISIBLE
                    }
                } else {
                    binding.rvVideos.visibility = View.VISIBLE
                    binding.animNoFile.visibility = View.GONE
                    binding.tvNoFile.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (MainActivity.dataChange) adapter.notifyDataSetChanged()
        MainActivity.adapterChange = false
    }
}