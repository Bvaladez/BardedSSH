package com.bard.android.bardedssh

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.*


private const val TAG = "HomeFragment"

class HomeFragment : Fragment(){
    lateinit var db: MachineSettingsDatabaseHelper
    var machines = ArrayList<String>()
    var machineObjs = ArrayList<MachineSettings>()
    lateinit var status_txt: TextView
    lateinit var connProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = MachineSettingsDatabaseHelper(this.requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?{
        val view = inflater.inflate(R.layout.fragment_home, container, false)
       return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        connProgressBar = view.findViewById(R.id.conn_progress_bar)
        connProgressBar.visibility = View.INVISIBLE
        status_txt = view.findViewById(R.id.status_text)
        val sshConnect_btn = view.findViewById<Button>(R.id.sshConnect_button)
        val machineSave_btn = view.findViewById<Button>(R.id.save_machine_button)
        val user_etxt = view.findViewById<EditText>(R.id.user_editText)
        val host_etxt = view.findViewById<EditText>(R.id.host_editText)
        val password_etxt = view.findViewById<EditText>(R.id.password_editText)
        val port_etxt = view.findViewById<EditText>(R.id.port_editText)

        val machine_list_view  = view.findViewById<ListView>(R.id.machines_listView)
        val arrayAdapter = ArrayAdapter( // Sets adapter for HomePage Save settings
            this.requireContext(),
            R.layout.machine_item,
            R.id.machine_name,
            machines
        )

        machine_list_view.adapter = arrayAdapter
        if(machines.isEmpty()){ // Prepopulate the list view only if needed
            for(machine in db.getAllSettings()){
                machines.add("${machine.username}@${machine.hostname}:${machine.port}")
                machineObjs.add(machine)
            }
            arrayAdapter.notifyDataSetChanged()
        }

        machine_list_view.setOnItemClickListener { parent, _, position, _ ->
            val selectedMachine = parent.getItemAtPosition(position) as String
            val idx = machineToObjIndex(selectedMachine)
            if (idx != null){
                val savedMachine = machineObjs[idx]
                host_etxt.setText(savedMachine.hostname)
                user_etxt.setText(savedMachine.username)
                password_etxt.setText(savedMachine.password)
                port_etxt.setText(savedMachine.port.toString())
            }
        }

        machine_list_view.setOnItemLongClickListener { parent, _, position, _ ->
            val selectedMachine = parent.getItemAtPosition(position) as String
            val idx = machineToObjIndex(selectedMachine)
            if (idx != null){
                val savedMachine = machineObjs[idx]
                db.deleteSettings(savedMachine)
                machines.removeAt(idx)
                machineObjs.removeAt(idx)
                arrayAdapter.notifyDataSetChanged()
            }
           true
        }

        sshConnect_btn.setOnClickListener { view->
            val host = host_etxt.text.toString()
            val user = user_etxt.text.toString()
            val password = password_etxt.text.toString()
            val port = port_etxt.text.toString().toInt()
            status_txt.text = "Connecting to $user@$host..."
            conn_progress_bar.visibility = View.VISIBLE
            onConnectShell(user, host, password, port)
        }

        machineSave_btn.setOnClickListener { view ->
            val host = host_etxt.text.toString()
            val user = user_etxt.text.toString()
            val password = password_etxt.text.toString()
            val port = port_etxt.text.toString().toInt()
            val machineSettings = MachineSettings(host, user, password, port)
            var uniqueMachine = true
            for (machine in db.getAllSettings()){
                if (machine.hostname == machineSettings.hostname &&
                    machine.username == machineSettings.username &&
                    machine.port == machineSettings.port){
                    uniqueMachine = false
                }
            }
            if (uniqueMachine){
                db.insertSettings(machineSettings)
                machines.add("${machineSettings.username}@${machineSettings.hostname}:${machineSettings.port}")
                machineObjs.add(machineSettings)
                arrayAdapter.notifyDataSetChanged()
            }
        }
        super.onViewCreated(view, savedInstanceState )
    }

    companion object {
        fun newInstance() =
            HomeFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    fun onConnectShell(user: String, host: String, password: String, port: Int){
        val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
            throwable.printStackTrace()
        }
        val thread = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
        thread.launch {
            val error: Exception? = SSH.init(user, host, password, port)
            if (error != null){ // Catches errors that occur during conn initialization
                withContext(Dispatchers.Main){
                    var e = error.toString()
                    e = e.substring(e.lastIndexOf(":") + 1).trim()
                    if (e == "Auth fail"){
                       e = "Auth failed (Username or Password)"
                    }
                    status_txt.text = e
                    connProgressBar.visibility = View.INVISIBLE
                }
                return@launch
            }
            SSH.sendCommand("\\ls -Gagh")
            val response: ArrayList<String> = SSH.readChannelOutput()
            if (response.isEmpty()){ // Connection was mad but never captured response from machine
                withContext(Dispatchers.Main){
                    status_txt.text = "Bad Response (Try again.. Or check your network connection)"
                    connProgressBar.visibility = View.INVISIBLE
                }
                return@launch
            }else{ // Good response launch new fragment ofj filesystem view
                val bundle = Bundle()
                val fragment = FileSystemFragment.newInstance()
                fragment.arguments = bundle
                bundle.putStringArrayList("data", response)
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.fragment_container, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }
    }

    fun machineToObjIndex(selectedMachine: String): Int?{
        for ((i, machine) in machines.withIndex()){
            if(machine == selectedMachine){
                return i
            }
        }
        return null
    }
}
