package com.example.telecom

import android.content.Intent
import android.telecom.*
import androidx.annotation.RequiresApi
import com.example.telecom.ui.CallScreenActivity

class MyInCallService : InCallService() {
    @RequiresApi(35)
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val callId = call.details.id
        val intent = Intent(this, CallScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("CALL_ID", callId)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
    }
}
