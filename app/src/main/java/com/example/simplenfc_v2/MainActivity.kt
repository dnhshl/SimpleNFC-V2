package com.example.simplenfc_v2

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import com.github.skjolber.ndef.Message
import com.github.skjolber.ndef.Record
import com.github.skjolber.ndef.externaltype.AndroidApplicationRecord
import com.github.skjolber.ndef.wellknown.TextRecord
import org.json.JSONException
import org.json.JSONObject

import splitties.alertdialog.alertDialog
import splitties.alertdialog.cancelButton
import splitties.alertdialog.positiveButton
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "onNewIntent : ${intent.action}")

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processNfcIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!nfcAdapter.isEnabled) {
            alertDialog (title = getString(R.string.nfc_activate_title),
                message = getString(R.string.nfc_not_activated)){
                positiveButton(R.string.activate) {
                    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                    startActivity(intent)
                }
                cancelButton { finish() }
            }.show()
        } else {
            val intent = intent
            Log.i(TAG, "onResume: ${intent.action}")
            enableNfcForegroundDispatch()
            processNfcIntent(intent)
        }
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null)
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
            nfcAdapter.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "Error disabling NFC foreground dispatch", ex)
        }
    }

    private fun processNfcIntent(intent: Intent) {
        if(!intent.hasExtra(NfcAdapter.EXTRA_TAG)) return

        // alle Messages vom NFC TAG lesen
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if(messages == null || messages.size == 0) return

        // Jede Message einzeln verarbeiten
        for(message in messages) {
            try {
                val records : List<Record> = Message(message as NdefMessage)
                // Jeden Record der Message verarbeiten
                for(record in records) {
                    when (record) {
                        // Wir reagieren nur auf TextRecords
                        is TextRecord -> parseJsonData(record.text)
                        // Ausgabe des AAR Records nur zu Info im Log
                        is AndroidApplicationRecord -> Log.i(TAG, "Package is ${record.packageName}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Problem parsing message : ${e.localizedMessage}")
            }
        }
    }


    private fun parseJsonData(jsonString : String) {
        try {
            val obj = JSONObject(jsonString)
            val id = obj.getInt("ID")
            val command = obj.getString("command").toString()

            // Aktion hier nur Ausgabe als TextView und Toast
            textView.text = command
            toast("NFC Card: ID $id, Anweisung $command")
        } catch (e : JSONException) {
            e.printStackTrace()
            Log.e(TAG, getString(R.string.error_json_parsing))
        }
    }
}