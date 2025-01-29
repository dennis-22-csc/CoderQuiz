package com.denniscode.coderquiz

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
//import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min


class StatsActivity : ComponentActivity() {
    private val iconsVisible = mutableStateOf(true)

    private fun triggerShareScreenshot() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Capture and share the screenshot
            shareScreenshot(this)

            // Once sharing is complete, call onShareComplete
            onShareComplete()
        }, 1000L)
    }

    // Function to call when share is complete
    private fun onShareComplete() {
        // Reset visibility here after sharing is completed
        iconsVisible.value = true // Set visibility back to true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val incorrectAnswers = intent.getIntExtra("INCORRECT_ANSWERS", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0)
        val timeStamp = intent.getStringExtra("TIME_STAMP") ?: "n.d"
        val categoryPerformance = intent.getBundleExtra("CATEGORY_PERFORMANCE")?.let { bundle ->
            bundle.keySet().associateWith { bundle.getFloat(it, 0.0f) }
        } ?: emptyMap()

        setContent {
            MaterialTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                ) {
                    StatsScreen(
                            correctAnswers = correctAnswers,
                            incorrectAnswers = incorrectAnswers,
                            totalQuestions = totalQuestions,
                            categoryPerformance = categoryPerformance,
                            timeStamp = timeStamp,
                            iconsVisible = iconsVisible.value,
                            onShareClick = {
                                iconsVisible.value = false
                                triggerShareScreenshot()
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsScreen(
        correctAnswers: Int,
        incorrectAnswers: Int,
        totalQuestions: Int,
        categoryPerformance: Map<String, Float>,
        timeStamp: String,
        iconsVisible: Boolean,
        onShareClick: () -> Unit
) {

    // Get the back press dispatcher
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back and Share Buttons
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button with visibility controlled by iconsVisible
            VisibilityIconButton(
                    visible = iconsVisible,
                    onClick = {
                        onBackPressedDispatcher?.onBackPressed()  // Handle the back press here
                    },
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
            )

            // Title text
            Text(
                    text = "Your stats for quiz taken $timeStamp",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
            )

            // Share button with visibility controlled by iconsVisible
            VisibilityIconButton(
                    visible = iconsVisible,
                    onClick = onShareClick,  // Hide icons and share the screenshot
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Screenshot"
            )
        }

        // Pie Chart Card
        val accuracy = if (totalQuestions > 0) {
            (correctAnswers.toFloat() / totalQuestions) * 100
        } else {
            0f
        }

        DonutChartCard(
                correct = correctAnswers,
                incorrect = incorrectAnswers,
                accuracy = accuracy
        )

        // Bar Chart Card
        HorizontalBarChartCard(
                data = categoryPerformance,
                maxValue = 100,

        )
    }
}



@Composable
fun DonutChartCard(correct: Int, incorrect: Int, accuracy: Float) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
    ) {
        Box(
                modifier = Modifier
                        .background(Color(0xFFFAFAFA))
                        .padding(24.dp)
        ) {
            Column {
                Text(
                        text = "Accuracy",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        LegendItem(color = Color.Green, label = "Correct")
                        LegendItem(color = Color.Red, label = "Incorrect")
                    }

                    Box(
                            modifier = Modifier
                                    .size(150.dp)
                                    .padding(8.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        DonutChart(correct = correct, incorrect = incorrect)
                        Text(
                                text = "${accuracy.toInt()}% Correct",
                                fontSize = 15.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(correct: Int, incorrect: Int) {
    val total = correct + incorrect
    val correctAngle = (correct.toFloat() / total) * 360
    val incorrectAngle = (incorrect.toFloat() / total) * 360

    Canvas(modifier = Modifier.size(150.dp)) {
        val canvasSize = min(size.width, size.height)
        val radius = canvasSize / 2
        val donutThickness = 40.dp.toPx()

        drawArc(
                color = Color.Green,
                startAngle = -90f,
                sweepAngle = correctAngle,
                useCenter = false,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(0f, 0f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = donutThickness)
        )

        drawArc(
                color = Color.Red,
                startAngle = -90f + correctAngle,
                sweepAngle = incorrectAngle,
                useCenter = false,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(0f, 0f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = donutThickness)
        )
    }
}


@Composable
fun LegendItem(color: Color, label: String) {
    Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier = Modifier
                        .size(16.dp)
                        .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 16.sp)
    }
}


@Composable
fun HorizontalBarChartCard(data: Map<String, Float>, maxValue: Int) {
    //val context = LocalContext.current

    Card(
            modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAFAFA)),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp,
    ) {
        Column(
                modifier = Modifier.padding(16.dp)
        ) {
            Text(
                    text = "Performance By Category",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 60.dp)
            )
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.forEach { (category, score) ->
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                    ) {

                        // Create a TextPaint object
                        val textPaint = Paint().apply {
                            textSize = 48f // Set the text size in pixels (adjust as needed)
                            typeface = Typeface.DEFAULT // Set the typeface (adjust as needed)
                        }

                        // Calculate the width of the text in pixels
                        val textWidthPx = textPaint.measureText(category)

                        // Convert the text width to DP
                        val textWidthDp = with(LocalDensity.current) { textWidthPx.toDp() }

                        // Calculate the width of the bar
                        val barWidth = maxOf((score / maxValue * 200).dp, 4.dp) // Scale bar width based on percentage score

                        // Check if the text fits inside the bar
                        val textFitsInsideBar = textWidthDp <= barWidth

                        // Show a toast to verify
                        /*Toast.makeText(
                                context,
                                if (textFitsInsideBar) "$category fits inside the bar"
                                else "$category does NOT fit inside the bar",
                                Toast.LENGTH_SHORT
                        ).show()*/
                        // Bar
                        Box(
                                modifier = Modifier
                                        .height(24.dp)
                                        .width(barWidth)
                                        .background(Color(0xFF2196F3)),
                                        //.background(MaterialTheme.colors.primary),
                                contentAlignment = Alignment.CenterStart
                        ) {
                            if (textFitsInsideBar) {
                                Text(
                                        text = category,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // Text outside the bar (if it doesn't fit)
                        if (!textFitsInsideBar) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    modifier = Modifier.wrapContentWidth() // Push text to the far right
                            )
                        }

                        // Percentage score at the far right
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "${score.toInt()}%",
                                fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

fun shareScreenshot(context: Context) {
    // Capture the screen as bitmap
    val bitmap = captureScreen(context)

    // Share the bitmap
    val contentUri = getBitmapUri(context, bitmap)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Screenshot"))
}

fun getBitmapUri(context: Context, bitmap: Bitmap): Uri {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs() // Create the directory if it doesn't exist
    val file = File(cachePath, "screenshot.png") // Create the file
    try {
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) // Write the bitmap to the file
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    // Use FileProvider to share the file
    return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
    )
}

fun captureScreen(context: Context): Bitmap {
    val view = (context as ComponentActivity).findViewById<View>(android.R.id.content)
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}



@Composable
fun VisibilityIconButton(
        visible: Boolean,
        onClick: () -> Unit,
        imageVector: ImageVector,
        contentDescription: String
) {
    if (visible) {
        IconButton(onClick = onClick) {
            Icon(imageVector = imageVector, contentDescription = contentDescription)
        }
    }
}


