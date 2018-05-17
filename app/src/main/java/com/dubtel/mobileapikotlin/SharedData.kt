package com.dubtel.mobileapikotlin

import android.app.Application

import java.util.Base64

/**
 * Created by kundan on 6/23/2015.
 */
class SharedData constructor() {

    var session_id: String? = null
    var user_id: String? = null
    private val apiKey = "4abc7598e1f28e394d57f50396c92a160671b575776ed10d885281eb94db7259"
    private val apiSecret = "eb1f101fe943b41526ae1c5e54089834badbab836e87f73ee1b6047e30a41c68"
    private val `var` = "$apiKey:$apiSecret"
    internal var x = Base64.getEncoder().encode(`var`.toByteArray())
    val token = String(x)

    companion object {

        // Getter-Setters
        var instance = SharedData()
    }

}