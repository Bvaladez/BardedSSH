package com.bard.android.bardedssh

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.list_item_all.view.*
import kotlinx.coroutines.*

private const val TAG = "FileAdapter"

val bundle = Bundle()


class FileAdapter(private val mList: List<FileViewModel>) : RecyclerView.Adapter<FileAdapter.FileHolder>() {
    lateinit var fileProgressBar: ProgressBar
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_all, parent, false)
        return FileHolder(view)
    }
    // binds the list items to a view
    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        val FileViewModel = mList[position]
        holder.fileTypeView.text = FileViewModel.type
        holder.fileNameView.text = FileViewModel.name
        holder.fileSizeView.text = FileViewModel.size
        holder.filePermissionsView.text = FileViewModel.permissions

        holder.fileProgressBar.visibility = View.INVISIBLE
        holder.fileTypeView.visibility = View.INVISIBLE

        if(FileViewModel.type == "DIR"){
            holder.fileImageView.setImageResource(R.drawable.folder)
        }else{
            holder.fileImageView.setImageResource(R.drawable.file)
        }

    }

    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class FileHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView), View.OnClickListener {

        val fileTypeView: TextView = itemView.findViewById(R.id.file_type)
        val fileNameView: TextView = itemView.findViewById(R.id.file_name)
        val fileSizeView: TextView = itemView.findViewById(R.id.file_size)
        val filePermissionsView: TextView = itemView.findViewById(R.id.file_permissions)
        val fileImageView: ImageView = itemView.findViewById(R.id.list_img)
        val fileProgressBar: ProgressBar = itemView.findViewById(R.id.file_progress_bar)
        var filePath = ""
        init{
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{
                onLongClick()

            }
        }

        override fun onClick(p0: View?) {
            cd(null)
        }

       fun handlePopupMenu(menuItem: MenuItem ): Boolean{
           val context = itemView.context
           when (menuItem.itemId) {
               R.id.sftp_option_text -> {
                   sftp()
                   return true
               }
               R.id.rename_option_text -> {
                   val builder = AlertDialog.Builder(context)
                   builder.setTitle("New Name/Path")
                   val input = EditText(context)
                   builder.setView(input)
                   builder.setPositiveButton("Confirm") { _, _ ->
                       val textEntered = input.text.toString().trim()
                       Log.d(TAG, textEntered)
                       mv(textEntered)
                       // Message of file name change
                   }
                   builder.setNegativeButton("Abort") { dialog, _ -> dialog.cancel() }
                   builder.show()
                   return true
               }
              R.id.copy_option_text -> {
                   val builder = AlertDialog.Builder(context)
                   builder.setTitle("Destination Relative path")
                   val input = EditText(context)
                   builder.setView(input)
                   builder.setPositiveButton("Confirm") { _, _ ->
                       val textEntered = input.text.toString().trim()
                       cp(textEntered)
                   }
                   builder.setNegativeButton("Abort") { dialog, _ -> dialog.cancel() }
                   builder.show()
                   return true
               }
               R.id.delete_option_text -> {
                   val builder = AlertDialog.Builder(context)
                   builder.setTitle("rm -r ${fileNameView.text.toString()}?")
                   builder.setPositiveButton("YES"){ _, _ ->
                       val bbuilder = AlertDialog.Builder(context)
                       bbuilder.setTitle("This will delete ${fileNameView.text.toString()} RECURSIVELY")
                       bbuilder.setPositiveButton("YES"){ _, _ ->
                           rm()
                       }
                       bbuilder.setNegativeButton("ABORT") { dialog, _ -> dialog.cancel() }
                       bbuilder.show()
                   }

                   builder.setNegativeButton("ABORT") { dialog, _ -> dialog.cancel() }
                   builder.show()
                   return true
               }

               else -> return false
           }

       }
        fun onLongClick(): Boolean{
            var file = fileNameView.text.toString()
            val context = itemView.context
            val dirMenu = PopupMenu(context, itemView)
            dirMenu.menuInflater.inflate(R.menu.dir_context_menu, dirMenu.menu)
            dirMenu.setOnMenuItemClickListener { menuItem ->
                handlePopupMenu(menuItem)

            }
            val fileMenu = PopupMenu(context, itemView)
            fileMenu.menuInflater.inflate(R.menu.file_context_menu, fileMenu.menu)
            fileMenu.setOnMenuItemClickListener { menuItem ->
                handlePopupMenu(menuItem)

            }
            if (fileTypeView.text == "FILE"){
                fileMenu.show()
            }else{
                dirMenu.show()
            }
            return true
        }

        fun cd(fileContext: String?){ // Given a fileContext cd command is run without checks
            var file: String
            if (fileContext == null){
                file = fileNameView.text.toString()
            }else{
               file = fileContext
            }
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val thread = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            thread.launch {
                if (fileTypeView.text != "DIR" && fileContext == null){
                    withContext(Dispatchers.Main ){
                        Snackbar.make(this@FileHolder.itemView,
                            "$file is a file not a directory", BaseTransientBottomBar.LENGTH_LONG).show()
                    }
                    return@launch
                }else{
                    withContext(Dispatchers.Main){ fileProgressBar.visibility = View.VISIBLE }
                    SSH.sendCommand("cd $file")
                    SSH.readChannelOutput()
                    SSH.sendCommand("\\ls -Gagh")
                    var response = SSH.readChannelOutput()
                    if (response.isNotEmpty()){
                        Log.d(TAG, "NOT EMPTY")
                        fileProgressBar.visibility = View.INVISIBLE
                        swapFragment(response)
                    }else{
                        Log.d(TAG, "EMPTY")
                        fileProgressBar.visibility = View.INVISIBLE
                        return@launch
                    }
                }
            }
        }

        fun sftp(){
            var file = fileNameView.text.toString().trim()
            var response: ArrayList<String>
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val thread = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            thread.launch {
                if (fileTypeView.text != "FILE"){
                    response = arrayListOf("ERROR: NOT FILE")
                }else{

                    SSH.sendCommand("pwd")
                    response = SSH.readChannelOutput()
                    if (response.isEmpty()){
                        //TODO msg
                        return@launch
                    }
                }
                var path = response[1].trim()
                var file_path = path.trim() + "/" + file.trim()
                var path_from_home = file_path.replace("/home/${SSH.mUser}/", "")
                val e: Exception?
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.VISIBLE }
                e = SFTP.sftp(path_from_home, SFTP.mDefaultInstallPath)
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.INVISIBLE }
                if(e!=null){
                    Log.e(TAG, e.toString())
                }
            }
        }

        fun mv(newPath: String){
            var file = fileNameView.text.toString().trim()
            var response: ArrayList<String>
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val thread = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            thread.launch {

                SSH.sendCommand("pwd")
                response = SSH.readChannelOutput()
                if (response.isEmpty()){
                        //TODO msg
                    return@launch
                }
                var path = response[1].trim()
                var filePath = path.trim() + "/" + file.trim()
                var pathFromHome = filePath.replace("/home/${SSH.mUser}/", "")
                val e: Exception?
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.VISIBLE }
                SSH.sendCommand("mv $pathFromHome $newPath")
                SSH.readChannelOutput()
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.INVISIBLE }
                cd(".")
            }
        }

        fun cp(destination: String){
            var file = fileNameView.text.toString().trim()
            var response: ArrayList<String>
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val thread = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            thread.launch {

                SSH.sendCommand("pwd")
                response = SSH.readChannelOutput()
                if (response.isEmpty()){
                    //TODO msg
                    return@launch
                }
                var path = response[1].trim()
                var filePath = path.trim() + "/" + file.trim()
                var pathFromHome = filePath.replace("/home/${SSH.mUser}/", "")
                val e: Exception?
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.VISIBLE }
                SSH.sendCommand("cp -r $pathFromHome $destination")
                SSH.readChannelOutput()
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.INVISIBLE }
                cd(".")
            }
        }

        fun rm(){
            var file = fileNameView.text.toString().trim()
            var response: ArrayList<String>
            val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
                throwable.printStackTrace()
            }
            val thread = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
            thread.launch {

                SSH.sendCommand("pwd")
                response = SSH.readChannelOutput()
                if (response.isEmpty()){
                    //TODO msg
                    return@launch
                }
                var path = response[1].trim()
                var filePath = path.trim() + "/" + file.trim()
                var pathFromHome = filePath.replace("/home/${SSH.mUser}/", "")
                val e: Exception?
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.VISIBLE }
                SSH.sendCommand("rm -r $file")
                SSH.readChannelOutput()
                withContext(Dispatchers.Main){ fileProgressBar.visibility = View.INVISIBLE }
                cd(".")
            }
        }


        fun swapFragment(response: ArrayList<String>){
            Log.d(TAG, "swapping frag")
            val bundle = Bundle()
            val fragment = FileSystemFragment.newInstance()
            val fragmentManager =
                (this.itemView.context as FragmentActivity).supportFragmentManager
            fragment.arguments = bundle
            bundle.putStringArrayList("data", response)
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                //.addToBackStack(null)
                .commit()
            return
        }
    }
}