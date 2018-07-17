package com.dubtel.mobileapikotlin

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.AsyncTask
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator

import com.dubtel.mobileapikotlin.R
import com.dubtel.mobileapikotlin.extensions.getColorCompat
import com.dubtel.mobileapikotlin.extensions.withStyledAttributes
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class BarChartView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    // Local Data
    var myData: Array<Float> = arrayOf(0f,0f,0f,0f,0f)
    private var barChartTask: UserBarChartTask? = null

    // Paints
    private val barPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColorCompat(R.color.bar_color)
    }
    private val axisPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColorCompat(R.color.grid_color)
        strokeWidth = resources.getDimensionPixelSize(R.dimen.bar_chart_grid_thickness).toFloat()
    }
    private val gridLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColorCompat(R.color.guide_color)
        strokeWidth = resources.getDimensionPixelSize(R.dimen.bar_chart_guide_thickness).toFloat()
    }

    // Dimens
    private var columnSpacing: Float = 0f
    private var padding: Float = 0f
    private val grid = RectF()

    // Animator
    private val animator = ValueAnimator().apply {
        interpolator = AccelerateInterpolator()
        duration = 500
        addUpdateListener { animator ->
            // Get our float from the animation. This method returns the Interpolated float.
            animatingFraction = animator.animatedFraction

            // MUST CALL THIS to ensure View re-draws;
            invalidate()
        }
    }

    private var animatingFraction: Float = 1.toFloat()

    init {
        context.withStyledAttributes(attrs, R.styleable.BarChartView) {
            columnSpacing = getDimensionPixelOffset(
                    R.styleable.BarChartView_android_spacing,
                    Math.round(2 * resources.displayMetrics.density)).toFloat()
            padding = getDimensionPixelOffset(
                    R.styleable.BarChartView_android_padding, 0).toFloat()
        }
    }

    fun animateBarsIn() = animator.apply {
        setFloatValues(0f, 1f)
        start()
    }

    override fun onDraw(canvas: Canvas) {
        barChartTask = UserBarChartTask()
        barChartTask!!.execute(null as Void?)
        Thread.sleep(1000)
        grid.set(padding, padding, width - padding, height - padding)
        canvas.apply {
            drawBottomLeftAxis(
                    gridBounds = grid,
                    paint = axisPaint)
            drawHorizontalGridLines(
                    numberOfGridLines = 10,
                    left = grid.left,
                    right = grid.right,
                    paint = gridLinePaint) {
                val gridSpacing = grid.height() / 10f
                grid.top + it * gridSpacing
            }
            drawEvenlySpacedBars(
                    inputData = myData,
                    gridBounds = grid,
                    columnSpacing = columnSpacing,
                    paint = barPaint) {
                it * animatingFraction
            }
        }
    }

    private fun Canvas.drawBottomLeftAxis(gridBounds: RectF, paint: Paint) {
        drawLine(gridBounds.left, gridBounds.bottom, gridBounds.left, gridBounds.top, paint)
        drawLine(gridBounds.left, gridBounds.bottom, gridBounds.right, gridBounds.bottom, paint)
    }

    private fun Canvas.drawHorizontalGridLines(numberOfGridLines: Int,
                                               left: Float, right: Float,
                                               paint: Paint,
                                               heightForGridLineIndex: (Int) -> Float) {
        var y: Float
        for (i in 0..numberOfGridLines) {
            y = heightForGridLineIndex(i)
            drawLine(left, y, right, y, paint)
        }
    }

    private fun <T> Canvas.drawEvenlySpacedBars(inputData: Array<T>,
                                                gridBounds: RectF,
                                                columnSpacing: Float = 0f,
                                                paint: Paint,
                                                fractionHeightForData: (T) -> Float) {
        val totalHorizontalSpacing = columnSpacing * (inputData.size + 1)
        val barWidth = (gridBounds.right - gridBounds.left - totalHorizontalSpacing) / inputData.size
        var barLeft = gridBounds.left + columnSpacing
        var barRight = barLeft + barWidth
        for (datum in inputData) {
            // Figure out top of column based on INVERSE of percentage. Bigger the percentage, the
            // smaller top is, since 100% goes to 0.
            val top = gridBounds.top + gridBounds.height() * (1f - fractionHeightForData(datum))
            drawRect(barLeft, top, barRight, grid.bottom, paint)

            // Shift over left/right column bounds
            barLeft += columnSpacing + barWidth
            barRight += columnSpacing + barWidth
        }
    }

    inner class UserBarChartTask internal constructor() : AsyncTask<Void, Void, Boolean>() {

        private val mSessionID: String
        private val mUserID: String

        init {
            mSessionID = SharedData.instance.session_id!!
            mUserID = SharedData.instance.user_id!!
        }

        override fun doInBackground(vararg params: Void): Boolean? {

            val authenticateString = "https://api.dubtel.com/v1/data/read"

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

                        val postObject = JSONObject();
                        postObject.put("session_id", mSessionID);

                        val array = JSONArray();
                        array.put("Chicago")
                        array.put("Cincinnati")
                        array.put("Columbus")
                        array.put("Denver")
                        array.put("New York")

                        postObject.put("data", array)

                        val postData = postObject.toString().toByteArray(Charsets.UTF_8)

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

                        var `object` = JSONObject(result)
                        var jsonObject = `object`.getJSONObject("data")
                        myData[0] = jsonObject.getString("Chicago").toFloat()/100
                        myData[1] = jsonObject.getString("Denver").toFloat()/100
                        myData[2] = jsonObject.getString("Cincinnati").toFloat()/100
                        myData[3] = jsonObject.getString("Columbus").toFloat()/100
                        myData[4] = jsonObject.getString("New York").toFloat()/100

                    } catch (e: Exception) {
                        println("ERROR $e")
                        barChartTask = null
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            })

            t1.start()

            try {
                t1.join()

            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return false

        }

        override fun onPostExecute(success: Boolean?) {
            barChartTask = null
        }

        override fun onCancelled() {
            barChartTask = null
        }
    }
}
