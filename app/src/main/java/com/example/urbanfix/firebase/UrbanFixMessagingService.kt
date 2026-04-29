package com.example.urbanfix.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class UrbanFixMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(uid)
            .child("deviceToken")
            .setValue(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "UrbanFix Update"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "New message received"
        UrbanFixNotificationManager.show(applicationContext, title, body)
    }
}