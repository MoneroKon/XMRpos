package org.monerokon.xmrpos.ui.shared

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

@Composable
fun DisplayImageFromFile(file: File, modifier: Modifier = Modifier) {
    // Decode the image file into a Bitmap
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)

    // If the file is successfully decoded into a bitmap, display it
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Displayed Image",
            modifier = modifier
        )
    }
}