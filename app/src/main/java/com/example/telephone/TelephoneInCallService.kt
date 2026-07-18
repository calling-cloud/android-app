package com.example.telephone

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile

class TelephoneInCallService : InCallService() {
    private val callbacks = mutableMapOf<Call, Call.Callback>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        trackCall(this, call)
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                handleCallState(state)
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback)
        handleCallState(call.state)
    }

    override fun onCallRemoved(call: Call) {
        untrackCall(call)
        callbacks.remove(call)?.let(call::unregisterCallback)
        CallRecordingManager.stop()
        CallRingtoneManager.stop()
        super.onCallRemoved(call)
    }

    private fun handleCallState(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                CallRingtoneManager.play(this)
                showIncomingCallUi()
            }
            Call.STATE_ACTIVE -> CallRingtoneManager.stop()
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                CallRecordingManager.stop()
                CallRingtoneManager.stop()
            }
        }
    }

    private fun showIncomingCallUi() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                )
                .putExtra("incoming_call", true),
        )
    }

    companion object {
        private val activeCalls = mutableListOf<Call>()
        private var service: TelephoneInCallService? = null

        @Synchronized
        private fun trackCall(inCallService: TelephoneInCallService, call: Call) {
            service = inCallService
            activeCalls.remove(call)
            activeCalls.add(call)
        }

        @Synchronized
        private fun untrackCall(call: Call) {
            activeCalls.remove(call)
            if (activeCalls.isEmpty()) service = null
        }

        @Synchronized
        fun hangUpCurrentCall(): Boolean {
            val call = activeCalls.lastOrNull() ?: return false
            return runCatching {
                if (call.state == Call.STATE_RINGING) {
                    call.reject(false, null)
                } else {
                    call.disconnect()
                }
            }.onSuccess {
                activeCalls.remove(call)
                CallRecordingManager.stop()
                CallRingtoneManager.stop()
            }.isSuccess
        }

        @Synchronized
        fun answerCurrentCall(): Boolean {
            val call = activeCalls.lastOrNull { it.state == Call.STATE_RINGING } ?: return false
            return runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }.isSuccess
        }

        @Synchronized
        fun currentRingingPhone(): String? {
            val call = activeCalls.lastOrNull { it.state == Call.STATE_RINGING } ?: return null
            return call.details.handle?.schemeSpecificPart
        }

        @Synchronized
        fun currentCallState(): Int? = activeCalls.lastOrNull()?.state

        @Synchronized
        fun setCallMuted(enabled: Boolean): Boolean {
            val inCallService = service ?: return false
            return runCatching { inCallService.setMuted(enabled) }.isSuccess
        }

        @Synchronized
        fun setCallSpeaker(enabled: Boolean): Boolean {
            val inCallService = service ?: return false
            val route = if (enabled) CallAudioState.ROUTE_SPEAKER else inCallService.receiverRoute()
            return runCatching { inCallService.setAudioRoute(route) }.isSuccess
        }

        private fun TelephoneInCallService.receiverRoute(): Int {
            val supported = callAudioState?.supportedRouteMask ?: 0
            return when {
                supported and CallAudioState.ROUTE_WIRED_HEADSET != 0 -> CallAudioState.ROUTE_WIRED_HEADSET
                supported and CallAudioState.ROUTE_BLUETOOTH != 0 -> CallAudioState.ROUTE_BLUETOOTH
                else -> CallAudioState.ROUTE_EARPIECE
            }
        }
    }
}
