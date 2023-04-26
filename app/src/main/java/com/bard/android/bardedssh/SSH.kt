package com.bard.android.bardedssh

import android.os.Bundle
import android.util.Log
import android.view.View
import com.jcraft.jsch.*
import kotlinx.coroutines.*
import java.io.*
import java.lang.reflect.Executable
import java.util.*


private const val TAG = "SSH"

object SSH {
    var DEBUG = false
    lateinit var mSession: Session
    lateinit var mChannel: ChannelShell
    var mUser = ""
    var mPassword = ""
    var mHost = ""
    var mPort = 22
    var mHome = ""

    @JvmStatic
    fun init(user: String, host:String, password: String, port: Int): Exception? {
        this.mUser = user
        this.mHost = host
        this.mPassword = password
        this.mPort = port
        var e: Exception?
        e = this.initSession(); if (e != null){return e}
        e = this.initChannel(); if (e != null){return e}
        return null
    }

    fun initSession(): Exception? {
        val jSch = JSch()
        try {
            this.mSession = jSch.getSession(this.mUser, this.mHost, this.mPort)
            this.mSession.setPassword(this.mPassword)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            this.mSession.setConfig(config)
            this.mSession.connect()
            return null

        } catch (e: Exception) {
            Log.e(TAG,"An error occurred while connecting to $mHost: $e")
            return e
        }
    }

    fun initChannel(): Exception?{
        try {
            Log.d(TAG, "Setting Channel")
            this.mChannel = this.mSession.openChannel("shell") as ChannelShell
            this.mChannel.connect()
            Log.d(TAG, "Channel Connected")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error while opening channel: $e")
            return e
        }
    }

    fun sendCommand(cmd: String) {
        try {
            Log.d(TAG, "Sending cmd: $cmd")
            this.mChannel.outputStream.flush()
            val out = PrintStream(this.mChannel.outputStream)
            out.println(cmd)
            out.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error while sending commands: $e")
        }
    }

    fun readChannelOutput(): ArrayList<String>{
        val buffer = ByteArray(1)
        var response = ArrayList<String>()
        var line = ""
        try {
            val `in` = this.mChannel.inputStream
            var byte = ""
            while(true) {
                while (`in`.available() > 0) {
                    val bytesToRead = 1
                    val i = `in`.read(buffer, 0, bytesToRead)
                    byte = String(buffer, 0, i)
                    if (byte == "\n"){
                        response.add(line)
                        line = ""
                    }else{
                        line += byte
                    }
                }
                try {
                    Thread.sleep(500)
                } catch (ee: Exception) {
                    Log.e(TAG, ee.toString())
                }
                if (`in`.available() == 0){
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG,"Error while reading channel output: $e")
        }
        return response
    }

    fun close() {
        this.mChannel.disconnect()
        this.mSession.disconnect()
        Log.e(TAG, "Disconnected channel and session")
    }

}

object SFTP{
    val mDefaultInstallPath = "/storage/emulated/0/Download"
    fun sftp(remotePath: String, localPath: String): Exception?{
        val jsch = JSch()
        val session = jsch.getSession(SSH.mUser, SSH.mHost, SSH.mPort)
        session.setPassword(SSH.mPassword)
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect()
        val channelSftp = session.openChannel("sftp") as ChannelSftp
        channelSftp.connect()
        try {
            channelSftp.get(remotePath, localPath)
        }catch (e: Exception){
            return e
            Log.e(TAG, "Error while getting \"$remotePath\" Error: $e")
        }
        channelSftp.disconnect()
        session.disconnect()
        return null
    }
}



object SSHO {
    var mUser: String = ""
    var mHost:  String = ""
    var mPassword: String = ""
    var mPort: Int = 22
    var mPath: String = "/home/%s".format(this.mUser)
    lateinit var mSession: Session
    lateinit var mSshChannelExec: ChannelExec
    lateinit var mSshChannelShell: ChannelShell
    lateinit var mOutputStream: OutputStream
    lateinit var mInputStream: InputStream

    fun initConnShell(user: String, host: String, port: Int, password: String):  ArrayList<String>{
        this.mUser = user
        this.mHost = host
        this.mPort = port
        this.mPassword = password

        val jsch = JSch()
        this.mSession = jsch.getSession(this.mUser, this.mHost, this.mPort)
        this.mSession.setPassword(this.mPassword)

        val properties = Properties()
        properties.put("StrictHostKeyChecking", "no")
        this.mSession.setConfig(properties)

        try{
            this.mSession.connect()
        }catch (e: JSchException){
            // TODO MAKE TOAST WRONG PASSWORD
            Log.e(TAG, "error in initConn: " + e.toString())
        }

        this.mSshChannelShell = this.mSession.openChannel("shell") as ChannelShell
        this.mOutputStream = ByteArrayOutputStream()
        //this.mSshChannelShell.outputStream = this.mOutputStream
        //this.mSshChannelShell.inputStream = this.mInputStream
        val commander = PrintStream(this.mSshChannelShell.outputStream, true)

        this.mSshChannelShell.connect()
        commander.println("ls -Gagh")

        // Gather result
        Thread.sleep(750)
        val outputString = this.mOutputStream.toString()
        val outputList = outputString.split("\r?\n|\r".toRegex())
        val outputArrayList = arrayListOf<String>()
        for (s in outputList){
            outputArrayList.add(s)
        }
        this.mOutputStream.flush()

        return outputArrayList
    }

    fun initConnExec(user: String, host: String, port: Int, password: String){
        this.mUser = user
        this.mHost = host
        this.mPort = port
        this.mPassword = password

        val jsch = JSch()
        this.mSession = jsch.getSession(this.mUser, this.mHost, this.mPort)
        this.mSession.setPassword(this.mPassword)

        val properties = Properties()
        properties.put("StrictHostKeyChecking", "no")
        this.mSession.setConfig(properties)

        try{
            this.mSession.connect()
        }catch (e: JSchException){
            // TODO MAKE TOAST WRONG PASSWORD
            Log.e(TAG, "error in initConn: " + e.toString())
        }

        this.mSshChannelExec = this.mSession.openChannel("exec") as ChannelExec
        //this.mSshChannel = this.mSession.openChannel("shell") as ChannelShell
        this.mOutputStream = ByteArrayOutputStream()
        this.mSshChannelExec.outputStream = this.mOutputStream

    }

    fun execCmd(cmd: String): ArrayList<String> {
        // Execute command
        this.mSshChannelExec.setCommand(cmd)
        this.mSshChannelExec.connect()

        // Gather result
        Thread.sleep(750)
        val outputString = this.mOutputStream.toString()
        val outputList = outputString.split("\r?\n|\r".toRegex())
        val outputArrayList = arrayListOf<String>()
        for (s in outputList){
            outputArrayList.add(s)
        }
        Log.d(TAG, this.mOutputStream.toString())
        this.mOutputStream.flush()

        return outputArrayList
    }

    fun exec(cmd: String): ArrayList<String> {

        val jsch = JSch()
        val session = jsch.getSession(this.mUser, this.mHost, this.mPort)
        session.setPassword(this.mPassword)

        val properties = Properties()
        properties.put("StrictHostKeyChecking", "no")
        session.setConfig(properties)

        try{
            session.connect()
        }catch (e: JSchException){
            // TODO MAKE TOAST WRONG PASSWORD
            Log.e(TAG, e.toString())
            return arrayListOf()
        }

        // Create SSH Channel
        val sshChannel = session.openChannel("exec") as ChannelExec
        val outputStream = ByteArrayOutputStream()
        sshChannel.outputStream = outputStream

        // Execute command
        sshChannel.setCommand(cmd)
        sshChannel.connect()

        // Gather result
        Thread.sleep(750)
        val outputString = outputStream.toString()
        val outputList = outputString.split("\r?\n|\r".toRegex())
        val outputArrayList = arrayListOf<String>()
        for (s in outputList){
            outputArrayList.add(s)
        }
        //Log.d(TAG, outputStream.toString())
        outputStream.reset()

        session.disconnect()
        sshChannel.disconnect()
        return outputArrayList
    }


    @Throws(InterruptedException::class)
    private fun waitForPrompt(outputStream: ByteArrayOutputStream) {
        val retries = 5
        for (x in 1 until retries) {
            Thread.sleep(500)
            if (outputStream.toString().indexOf("$") > 0) {
                print(outputStream.toString())
                outputStream.reset()
                return
            }
        }
    }
}
