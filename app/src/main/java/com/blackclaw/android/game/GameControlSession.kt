package com.blackclaw.android.game

/** Short-lived guard against blind or runaway game input. */
object GameControlSession {
    data class Snapshot(
        val packageName: String,
        val observedAtMs: Long,
        val frameHash: Long,
        val actionCount: Int,
    )

    @Volatile private var state: Snapshot? = null

    fun observe(packageName: String, frameHash: Long, nowMs: Long = System.currentTimeMillis()): Snapshot {
        return Snapshot(packageName, nowMs, frameHash, 0).also { state = it }
    }

    fun current(): Snapshot? = state

    fun validate(packageName: String, nowMs: Long = System.currentTimeMillis()): String? {
        val snapshot = state ?: return "Call game_observe first; blind actions are not allowed."
        if (snapshot.packageName != packageName) {
            return "The app changed since observation (${snapshot.packageName} → $packageName). Observe again."
        }
        if (nowMs - snapshot.observedAtMs > GameControlPolicy.OBSERVATION_TTL_MS) {
            return "The observation expired. Call game_observe again."
        }
        if (snapshot.actionCount >= GameControlPolicy.MAX_ACTIONS_PER_OBSERVATION) {
            return "The action limit was reached; observe again before continuing."
        }
        return null
    }

    fun recordAction(newFrameHash: Long? = null) {
        val snapshot = state ?: return
        state = snapshot.copy(
            frameHash = newFrameHash ?: snapshot.frameHash,
            actionCount = snapshot.actionCount + 1,
        )
    }
}
