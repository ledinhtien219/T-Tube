package com.carhud.aaproxy

/** Ownership for delayed work; app id alone cannot distinguish A -> B -> A. */
internal class WebTransitionState {
    var generation = 0L
        private set
    private var owner = ""

    fun begin(appId: String): Long {
        owner = appId
        return ++generation
    }

    fun owns(token: Long, appId: String): Boolean = generation == token && owner == appId
}
