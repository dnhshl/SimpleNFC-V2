package com.example.simplenfc_v2

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color.rgb
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import org.ndeftools.Message
import org.ndeftools.Record
import org.ndeftools.externaltype.AndroidApplicationRecord
import org.ndeftools.wellknown.TextRecord
import org.ndeftools.wellknown.UriRecord
import splitties.toast.toast

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivityNFC"

    private val nfcAdapter: NfcAdapter by lazy { NfcAdapter.getDefaultAdapter(applicationContext) }
    private val textView: TextView by lazy {findViewById(R.id.textview)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (nfcAdapter == null) {
            Log.i(TAG, "NFC not available")
            finish()
        }


        val intent = intent
        Log.i(TAG, "onCreate: ${intent.action}")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "onNewIntent : ${intent!!.action}")

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processNfcIntent(intent)

        }
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        Log.i(TAG, "onResume: ${intent.action}")
        enableNfcForegroundDispatch()
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "Error enabling NFC foreground dispatch", ex)
        }
    }

    override fun onPause() {
        disableNfcForegroundDispatch()
        super.onPause()
    }

    private fun disableNfcForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "Error disabling NFC foreground dispatch", ex)
        }
    }

    private fun processNfcIntent(intent: Intent) {
        if(!intent.hasExtra(NfcAdapter.EXTRA_TAG)) return

        Log.i(TAG, "handle Intent processing")
        toast(getString(R.string.nfc_received, intent.action))

        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if(messages == null || messages.size == 0) return

        for(message in messages) {
            try {
                val records : List<Record> = Message(message as NdefMessage)
                Log.i(TAG, "Message mit ${records.size} Records")
                for(record in records) {
                    when (record) {
                        is TextRecord -> doSomething(record as TextRecord)
                        is AndroidApplicationRecord -> {
                            val aar = record as AndroidApplicationRecord
                            Log.i(TAG, "Package is ${aar.packageName}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Problem parsing message : ${e.localizedMessage}")
            }
        }
    }

    fun doSomething(textRecord: TextRecord) {
        val jsonString = textRecord.text
        textView.text = jsonString
    }
}