package com.example.task8

import android.content.Intent
import android.graphics.Bitmap
import android.icu.number.Scale
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.example.task8.ui.theme.Task8Theme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Task8Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    color = MaterialTheme.colorScheme.background
                ) {
                    downloadApp()
                }
            }
        }
    }


    suspend fun downloadPhoto(URL: String): Bitmap? {
        val deferred = CompletableDeferred<Bitmap?>()
        val handler = Handler(Looper.getMainLooper())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val image = withContext(Dispatchers.IO) {
                    Glide.with(this@MainActivity)
                        .asBitmap()
                        .load(URL)
                        .submit()
                        .get()
                }

                handler.post {
                    Toast.makeText(
                        this@MainActivity,
                        "Картинка успешно загружена",
                        Toast.LENGTH_LONG
                    ).show()
                }

                saveImage(image, URL)
                deferred.complete(image) // Завершаем deferred с успешным результатом
            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    Toast.makeText(
                        this@MainActivity,
                        "Картинку не удалось загрузить",
                        Toast.LENGTH_LONG
                    ).show()
                }
                deferred.complete(null) // Завершаем deferred с null в случае ошибки
            }
        }

        return deferred.await() // Ожидаем результат загрузки и возвращаем его
    }

    private fun saveImage(imageBitmap: Bitmap?, URL: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Создаем имя файла на основе URL
            val name = URL.substringAfterLast("/") // Извлекаем имя файла из URL
            val imageFileName = "JPEG_$name"
            val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .toString() + "/YOUR_FOLDER_NAME"
            )

            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, imageFileName)
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()

                galleryAddPic(imageFile.path)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    Toast.makeText(
                        this@MainActivity,
                        "Картинка сохранена в память устройства",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    Toast.makeText(
                        this@MainActivity,
                        "Картинку не удалось сохранить",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun galleryAddPic(imagePath: String?) {
        imagePath?.let { path ->
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val f = File(path)
            val contentUri: Uri = Uri.fromFile(f)
            mediaScanIntent.data = contentUri
            sendBroadcast(mediaScanIntent)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun downloadApp() {
    val downloadedImageList = remember {
        mutableStateListOf<Bitmap>()
    }

    val urlText = remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = urlText.value,
            onValueChange = { urlText.value = it },
            label = { Text("Введите URL фото") }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(count = downloadedImageList.toList().size, itemContent = { index: Int ->
                val image = downloadedImageList.toList().get(index)
                Image(bitmap = image.asImageBitmap(),
                    contentDescription = "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .padding(10.dp))
            })
        }

        Button(onClick = {
            val URL = urlText.value.toString()
            CoroutineScope(Dispatchers.Default).launch {
                val image = (context as MainActivity).downloadPhoto(URL)
                if (image != null) {
                    downloadedImageList.add(image)
                }
            }
        }
        ) {
            Text(text = "Скачать изображение")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Task8Theme {
        downloadApp()
    }
}



