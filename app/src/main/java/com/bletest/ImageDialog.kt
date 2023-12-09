package com.bletest

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView

class ImageDialog(
    context: Context,
    private val imageResId: Int,
    private val onCloseClick: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.image_dialog)

        // Set the background to transparent
        window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val imageView = findViewById<ImageView>(R.id.imageView)
        val closeButton = findViewById<TextView>(R.id.btnClose)

        // Set the image resource
        //imageView.setImageResource(imageResId)

        // Set click listener for the close button
        closeButton.setOnClickListener {
            onCloseClick.invoke()
            dismiss()
        }
    }
}