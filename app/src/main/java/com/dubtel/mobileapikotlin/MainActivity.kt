package com.dubtel.mobileapikotlin

import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

import org.json.JSONObject

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MainActivity : AppCompatActivity() {

    private var stat = ""
    private var logOutTask: UserLogOutTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logOutButton = findViewById(R.id.log_out_button) as Button
        logOutButton.setOnClickListener {
            logOutTask = UserLogOutTask()
            logOutTask!!.execute(null as Void?)
        }
    }

    inner class UserLogOutTask internal constructor() : AsyncTask<Void, Void, Boolean>() {

        private val mSessionID: String
        private val mUserID: String

        init {
            mSessionID = SharedData.instance.session_id!!
            mUserID = SharedData.instance.user_id!!
        }

        override fun doInBackground(vararg params: Void): Boolean? {

            val authenticateString = "https://api.dubtel.com/v1/logOut"

            val t1 = Thread(Runnable {
                try {
                    try {
                        // Load CAs from an InputStream
                        val cf = CertificateFactory.getInstance("X.509")

                        val fis = resources.openRawResource(R.raw.mycert)

                        val caInput = BufferedInputStream(fis)
                        val ca: Certificate
                        try {
                            ca = cf.generateCertificate(caInput)
                        } finally {
                            caInput.close()
                        }

                        // Create a KeyStore containing our trusted CAs
                        val keyStoreType = KeyStore.getDefaultType()
                        val keyStore = KeyStore.getInstance(keyStoreType)
                        keyStore.load(null, null)
                        keyStore.setCertificateEntry("ca", ca)

                        // Create a TrustManager that trusts the CAs in our KeyStore
                        val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
                        val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
                        tmf.init(keyStore)

                        // Create an SSLContext that uses our TrustManager
                        val context = SSLContext.getInstance("TLS")
                        context.init(null, tmf.trustManagers, null)

                        val postString = "session_id=$mSessionID&user_id=$mUserID"
                        val postData = postString.toByteArray(StandardCharsets.UTF_8)

                        // Tell the URLConnection to use a SocketFactory from our SSLContext
                        val url = URL(authenticateString)
                        val urlConnection = url.openConnection() as HttpsURLConnection
                        urlConnection.doOutput = true
                        urlConnection.sslSocketFactory = context.socketFactory
                        urlConnection.setRequestProperty("Authorization", "Basic " + SharedData.instance.token)
                        urlConnection.requestMethod = "POST"

                        val wr = DataOutputStream(urlConnection.outputStream)
                        wr.write(postData)
                        wr.flush()
                        wr.close()

                        val `in` = urlConnection.inputStream

                        val reader = BufferedReader(InputStreamReader(`in`, "UTF-8"))
                        val sb = StringBuilder()
                        var result: String? = null
                        try {
                            sb.append(reader.readLine())
                            result = sb.toString()
                        } finally {
                            reader.close()
                        }

                        val `object` = JSONObject(result)
                        stat = `object`.getString("status")

                    } catch (e: Exception) {
                        println("ERROR $e")
                        logOutTask = null
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            })

            t1.start()

            try {
                t1.join()
                if (stat == "Success") {
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return false

        }

        override fun onPostExecute(success: Boolean?) {
            logOutTask = null
        }

        override fun onCancelled() {
            logOutTask = null
        }
    }
}