package com.bard.android.bardedssh

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "FileSystemFragment"


class FileSystemFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_file_system_list, container, false)
        val mRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)
        val data = arguments?.getStringArrayList("data")

        val fileView = mutableListOf<FileViewModel>()
        if (data != null){
            for (s in data){ // This parses the data only for ls -Gagh
                val text = s.trim()
                val line = text.split("\\s+".toRegex())
                val fDesignators = listOf('d', 'l', '-', 'p', 's', 'b', 'D')
                if (line.size >= 7 &&
                    fDesignators.contains(line[0].first()) &&
                    line[0].length == 10){ // surely this gets just listings after an ls command
                    val permissions = line[0]
                    var type = permissions.first().toString()
                    if (type == "d"){type = "DIR"}
                    if (type == "-"){type = "FILE"}
                    val refCount = line[1]
                    val size = line[2] + " Bytes"
                    val month = line[3]
                    val day = line[4]
                    val time = line[5]
                    val name = line[6]
                    //if (name.trim() != "." && name.trim() != ".."){ // Shouldn't care about here and there
                    fileView.add(FileViewModel(type, name, size, permissions ))
                    //}
                }
            }
        }
        mRecyclerView.layoutManager = LinearLayoutManager(this.context)
        mRecyclerView.adapter = FileAdapter(fileView)
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            FileSystemFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}