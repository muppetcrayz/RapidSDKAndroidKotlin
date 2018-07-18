package com.dubtel.mobileapikotlin

import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

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

class SignUpActivity : AppCompatActivity() {

    private var firstName: EditText? = null
    private var lastName: EditText? = null
    private var email: EditText? = null
    private var password: EditText? = null
    private var signUpButton: Button? = null
    private var cancelButton: Button? = null

    private var stat = ""
    private var signUpTask: UserSignUpTask? = null
    internal var cancel: Boolean? = false
    internal var focusView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        firstName = findViewById(R.id.first_name) as EditText
        lastName = findViewById(R.id.last_name) as EditText
        email = findViewById(R.id.email_sign_up) as EditText
        password = findViewById(R.id.password_sign_up) as EditText

        signUpButton = findViewById(R.id.submit_sign_up) as Button
        signUpButton!!.setOnClickListener { attemptSignUp() }

        cancelButton = findViewById(R.id.cancel_button) as Button
        cancelButton!!.setOnClickListener {
            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun attemptSignUp() {

        val firstNameText = firstName!!.text.toString()
        val lastNameText = lastName!!.text.toString()
        val emailText = email!!.text.toString()
        val passwordText = password!!.text.toString()

        if (TextUtils.isEmpty(firstNameText) || TextUtils.isEmpty(lastNameText) || TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passwordText)) {
            password!!.error = "One of the fields is missing."
            focusView = password
            focusView!!.requestFocus()
            cancel = true
        }

        signUpTask = UserSignUpTask(firstNameText, lastNameText, emailText, passwordText)
        signUpTask!!.execute(null as Void?)

    }

    private fun myError() {
        focusView = firstName
        firstName!!.error = "Unable to register."
        focusView!!.requestFocus()
    }

    inner class UserSignUpTask internal constructor(private val mFirstName: String, private val mLastName: String, private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {

            val authenticateString = "https://api.rapidsdk.com/v1/register"

            val t1 = Thread(Runnable {
                try {
                    try {

                        val postString = "firstname=$mFirstName&lastname=$mLastName&email=$mEmail&password=$mPassword"
                        val postData = postString.toByteArray(StandardCharsets.UTF_8)

                        // Tell the URLConnection to use a SocketFactory from our SSLContext
                        val url = URL(authenticateString)
                        val urlConnection = url.openConnection() as HttpsURLConnection
                        urlConnection.doOutput = true
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
                            println(result)
                        } finally {
                            reader.close()
                        }

                        val `object` = JSONObject(result)
                        stat = `object`.getString("status")

                    } catch (e: Exception) {
                        println("ERROR $e")
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            })

            t1.start()

            try {
                t1.join()
                if (stat == "Success") {
                    val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return false

        }

        override fun onPostExecute(success: Boolean?) {
            signUpTask = null
            myError()
        }

        override fun onCancelled() {
            signUpTask = null
        }
    }
}

