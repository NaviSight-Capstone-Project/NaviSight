package edu.capstone.navisight.common.webrtc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder

object GsonSingleton {
    /**
     * The single, lazily initialized Gson instance.
     * It is created only on the first access and is thread-safe due to the use of Kotlin's 'lazy'.
     */
    val instance: Gson by lazy {
        GsonBuilder()
            // Keep special characters like '<' and '>'
            .disableHtmlEscaping()
            .create()
    }
}
