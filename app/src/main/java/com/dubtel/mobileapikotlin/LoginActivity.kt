package com.dubtel.mobileapikotlin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.AsyncTask

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
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

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {

    internal var sharedData = SharedData.instance

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    // UI references.
    private var mUsernameView: EditText? = null
    private var mPasswordView: EditText? = null
    internal var focusView: View? = null
    internal var cancel: Boolean? = false

    private var stat = ""
    private var session = ""
    private var user = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.
        mUsernameView = findViewById(R.id.email) as EditText

        mPasswordView = findViewById(R.id.password) as EditText

        mPasswordView!!.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        val mEmailSignInButton = findViewById(R.id.email_sign_in_button) as Button
        mEmailSignInButton.setOnClickListener { attemptLogin() }

        val mSignUpButton = findViewById(R.id.sign_up_button) as Button
        mSignUpButton.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        mUsernameView!!.error = null
        mPasswordView!!.error = null

        // Store values at the time of the login attempt.
        val username = mUsernameView!!.text.toString()
        val password = mPasswordView!!.text.toString()

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            mPasswordView!!.error = "One of the fields is missing."
            focusView = mPasswordView
            cancel = true
        }
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        mAuthTask = UserLoginTask(username, password)
        mAuthTask!!.execute(null as Void?)
    }

    private fun myError() {
        focusView = mPasswordView
        mPasswordView!!.error = "Username or password is incorrect."
        focusView!!.requestFocus()
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {

            val authenticateString = "https://api.rapidsdk.com/v1/login"

            val t1 = Thread(Runnable {
                try {
                    try {

                        val postString = "email=$mEmail&password=$mPassword"
                        val postData = postString.toByteArray(StandardCharsets.UTF_8)

                        // Tell the URLConnection to use a SocketFactory from our SSLContext
                        val url = URL(authenticateString)
                        val urlConnection = url.openConnection() as HttpsURLConnection
                        urlConnection.doOutput = true
                        urlConnection.setRequestProperty("Authorization", "Basic " + sharedData.token)
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
                        if (stat == "Success") {
                            session = `object`.getString("session_id")
                            user = `object`.getString("user_id")
                        }

                    } catch (e: Exception) {
                        println("ERROR $e")
                        mAuthTask = null
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            })

            t1.start()

            try {
                t1.join()
                if (stat == "Success") {
                    sharedData.session_id = session
                    sharedData.user_id = user
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return false

        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            if (stat == "") {
                myError()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
        }
    }
}

