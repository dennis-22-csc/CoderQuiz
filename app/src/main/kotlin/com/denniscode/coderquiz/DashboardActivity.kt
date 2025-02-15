package com.denniscode.coderquiz

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.saveable.rememberSaveable
import android.app.Activity
import android.content.Context
import android.content.Intent
import org.json.JSONException
import org.json.JSONObject

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_OK)
        val dbHelper = QuizDatabaseHelper(this)
        val quizStatsList = dbHelper.getAllQuizStats()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BottomNavHost(quizStatsList)
                }
            }
        }
    }
}

@Composable
fun BottomNavHost(quizStats: List<Map<String, Any>>) {
    var selectedScreen by rememberSaveable { mutableStateOf("stats") }

    Scaffold(
            bottomBar = {
                BottomNavigation (backgroundColor = MaterialTheme.colors.background){
                    BottomNavigationItem(
                            icon = { Box(modifier = Modifier.size(0.dp)) }, // Empty placeholder
                            label = { Text("Stats") },
                            selected = selectedScreen == "stats",
                            onClick = { selectedScreen = "stats" }
                    )
                    BottomNavigationItem(
                            icon = { Box(modifier = Modifier.size(0.dp)) }, // Empty placeholder
                            label = { Text("Infographics") },
                            selected = selectedScreen == "infographics",
                            onClick = { selectedScreen = "infographics" }
                    )
                }
            }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (selectedScreen) {
                "infographics" -> InfographicsScreen(quizStats)
                "stats" -> QuizStatsScreen(quizStats)
            }
        }
    }
}

@Composable
fun QuizStatsScreen(quizStats: List<Map<String, Any>>) {
    val totalQuestionsAnswered = quizStats.sumOf { (it["correct_answers"] as Int) + (it["incorrect_answers"] as Int) }
    val totalCorrect = quizStats.sumOf { it["correct_answers"] as Int }
    val totalIncorrect = quizStats.sumOf { it["incorrect_answers"] as Int }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your stats for today", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Thank you for challenging yourself today.", style = MaterialTheme.typography.body1)
        Spacer(modifier = Modifier.height(16.dp))

        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard("Questions Answered", totalQuestionsAnswered)
            StatCard("Correct", totalCorrect)
            StatCard("Incorrect", totalIncorrect)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Quiz Completion History", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(quizStats.size) { index ->
                QuizHistoryItem(quizStats[index]) {
                    showStats(context, quizStats[index]["id"] as String)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, count: Int) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.body1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.h5)
        }
    }
}

@Composable
fun QuizHistoryItem(stat: Map<String, Any>, onClick: () -> Unit) {
    val formattedDateTime = formatDateTime(stat["date_time"] as String)
    val quizTitle = extractQuizTitle(stat["quiz_category"] as String)

    Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(quizTitle, style = MaterialTheme.typography.subtitle1.copy(fontWeight=FontWeight.Medium))
            Text(formattedDateTime, style = MaterialTheme.typography.body2)
        }
        Text(
                text = "${(stat["correct_answers"] as Int) * 100 / ((stat["correct_answers"] as Int) + (stat["incorrect_answers"] as Int))}%",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InfographicsScreen(quizStats: List<Map<String, Any>>) {
    var selectedTimeFrame by remember { mutableStateOf("Hourly") } // Default timeframe
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var graphData by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    //val context = LocalContext.current

    // Observe the sheetState and update showBottomSheet accordingly
    LaunchedEffect(sheetState.currentValue) {
        showBottomSheet = sheetState.isVisible
    }

    // Ensure that the bottom sheet state reflects the visibility of the sheet
    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            sheetState.show()  // Show the bottom sheet when requested
        } else {
            sheetState.hide()  // Hide the bottom sheet when dismissed
        }
    }

    // Call updateGraphData when the screen is first composed
    LaunchedEffect(Unit) {
        updateGraphData(
                quizStats = quizStats,
                selectedTab = selectedTab,
                selectedDate = selectedDate,
                startDate = startDate,
                endDate = endDate,
                selectedTimeFrame = selectedTimeFrame,
                onGraphUpdated = { processedData ->
                    graphData = processedData
                }
        )
    }

    // Use a Box to layer the graph and the bottom sheet
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Today Button
            Button(
                    onClick = { showBottomSheet = !showBottomSheet },
                    modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(0.5f), // Adjust button width
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Today")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Down Arrow"
                    )
                }
            }

            // Spacer to create a large margin between the button and the graph
            Spacer(modifier = Modifier.height(32.dp)) // Adjust height as needed

            // Graph
            Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Take up remaining space
                            .padding(horizontal = 16.dp) // Add horizontal padding
            ) {
                DrawGraph(graphData)
            }
        }

        // Bottom sheet
        ModalBottomSheetLayout(
                sheetState = sheetState,
                scrimColor = Color.Transparent,
                sheetContent = {
                    Column(modifier = Modifier.fillMaxHeight(0.5f)) {
                        TabRow(
                                selectedTabIndex = selectedTab,
                                modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(Color.White),
                                backgroundColor = Color.White,
                                contentColor = Color.Black,
                                indicator = { tabPositions ->
                                    TabRowDefaults.Indicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                            height = 2.dp,
                                            color = Color.Blue
                                    )
                                }
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                Text("Choose Day")
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                Text("TimeFrame")
                            }
                        }
                        when (selectedTab) {
                            0 -> ChooseDayTab(
                                    selectedDate = selectedDate,
                                    onDateSelected = { selectedDate = it },
                                    onConfirm = {
                                        updateGraphData(
                                                quizStats = quizStats,
                                                selectedTab = selectedTab,
                                                selectedDate = selectedDate,
                                                startDate = startDate,
                                                endDate = endDate,
                                                selectedTimeFrame = selectedTimeFrame,
                                                onGraphUpdated = { processedData ->
                                                    graphData = processedData
                                                }
                                        )
                                        showBottomSheet = false
                                    }
                            )
                            1 -> TimeFrameTab(
                                    startDate = startDate,
                                    endDate = endDate,
                                    onStartDateSelected = { startDate = it },
                                    onEndDateSelected = { endDate = it },
                                    selectedTimeframe = selectedTimeFrame, // Pass selectedTimeframe
                                    onTimeframeSelected = { selectedTimeFrame = it }, // Update selectedTimeframe
                                    onConfirm = {
                                        updateGraphData(
                                                quizStats = quizStats,
                                                selectedTab = selectedTab,
                                                selectedDate = selectedDate,
                                                startDate = startDate,
                                                endDate = endDate,
                                                selectedTimeFrame = selectedTimeFrame,
                                                onGraphUpdated = { processedData ->
                                                    graphData = processedData
                                                }
                                        )
                                        showBottomSheet = false
                                    }
                            )
                        }
                    }
                }
        ) {
            // Empty content for the bottom sheet layout
        }
    }
}

@Composable
fun ChooseDayTab(selectedDate: Date?,
                 onDateSelected: (Date) -> Unit,  onConfirm: () -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
    ) {
        // Label
        Text(
                text = "Chosen Date",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
        )

        // Clickable Box
        Box(
                modifier = Modifier
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .clickable { showDatePicker = true }
                        .width(180.dp),
                contentAlignment = Alignment.Center
        ) {
            Text(text = selectedDate?.let { dateFormat.format(it) } ?: "Select a Date", fontSize = 16.sp, color = Color.Black)
        }

        if (showDatePicker) {
            DatePickerDialog(
                    context,
                    { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(calendar.time)
                        showDatePicker = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Button at the Bottom
        Button(
                onClick = {
                    onConfirm()
                    //Toast.makeText(context, "Selected date: $selectedDate with 24h time frame", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                        .width(160.dp)
                        .align(Alignment.CenterHorizontally)
        ) {
            Text("Confirm")
        }
    }
}



    @Composable
    fun TimeFrameTab(
            startDate: Date?,
            endDate: Date?,
            onStartDateSelected: (Date) -> Unit,
            onEndDateSelected: (Date) -> Unit,
            selectedTimeframe: String, // Add selectedTimeframe as a parameter
            onTimeframeSelected: (String) -> Unit, // Add callback for timeframe selection
            onConfirm: () -> Unit
    ) {
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    "Duration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Daily", "Weekly", "Monthly").forEach { timeframe ->
                    Card(
                            modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clickable {
                                        onTimeframeSelected(timeframe) // Update selected timeframe
                                    }
                                    .border(
                                            width = 2.dp,
                                            color = if (selectedTimeframe == timeframe) Color.Blue else Color.Gray,
                                            shape = RoundedCornerShape(8.dp)
                                    ),
                            backgroundColor = if (selectedTimeframe == timeframe) Color.LightGray else Color.White,
                    ) {
                        Box(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    timeframe,
                                    color = if (selectedTimeframe == timeframe) Color.Blue else Color.Black,
                                    fontWeight = if (selectedTimeframe == timeframe) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                    "Start Date",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                    modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .clickable { showStartDatePicker = true }
            ) {
                Text(
                        text = startDate?.let { dateFormat.format(it) } ?: "Select Start Date",
                        color = if (startDate != null) Color.Black else Color.Gray
                )
            }
            if (showStartDatePicker) {
                DatePickerDialog(
                        context,
                        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                            calendar.set(year, month, dayOfMonth)
                            onStartDateSelected(calendar.time)
                            showStartDatePicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                    "End Date",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                    modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .clickable { showEndDatePicker = true }
            ) {
                Text(
                        text = endDate?.let { dateFormat.format(it) } ?: "Select End Date",
                        color = if (endDate != null) Color.Black else Color.Gray
                )
            }
            if (showEndDatePicker) {
                DatePickerDialog(
                        context,
                        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                            calendar.set(year, month, dayOfMonth)
                            onEndDateSelected(calendar.time)
                            showEndDatePicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                    onClick = {
                        if (startDate != null && endDate != null) {
                            onConfirm() // Trigger the onConfirm callback
                            //val toastMessage =
                                    "Timeframe: $selectedTimeframe\nStart: ${dateFormat.format(startDate)}\nEnd: ${dateFormat.format(endDate)}"
                            //Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Please select both start and end dates.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Confirm")
            }
        }
    }

@Composable
fun DrawGraph(dataPoints: Map<String, Float>) {
    // Determine sorting approach based on time format
    val sortedDataPoints = dataPoints.toSortedMap { a, b ->
        when {
            a.startsWith("Week") && b.startsWith("Week") -> {
                // Sort by week number
                a.substringAfter("Week ").toInt().compareTo(b.substringAfter("Week ").toInt())
            }
            a.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && b.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                // Sort by date (yyyy-MM-dd)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(a)!!
                        .compareTo(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(b)!!)
            }
            a.matches(Regex("\\w{3} \\d{2}, \\d{4}")) && b.matches(Regex("\\w{3} \\d{2}, \\d{4}")) -> {
                // Sort by formatted date (MMM dd, yyyy)
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(a)!!
                        .compareTo(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(b)!!)
            }
            a.matches(Regex("\\w{3} \\d{4}")) && b.matches(Regex("\\w{3} \\d{4}")) -> {
                // Sort by month-year format (MMM yyyy)
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(a)!!
                        .compareTo(SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(b)!!)
            }
            else -> a.compareTo(b) // Default lexicographical sorting for unknown formats
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val leftPadding = 90f
        val rightPadding = 60f
        val topPadding = 90f
        val bottomPadding = 120f  // Increased padding for long labels
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        // Draw Y-axis
        drawLine(Color.Black, Offset(leftPadding, topPadding), Offset(leftPadding, chartHeight + topPadding), strokeWidth = 2f)

        // Draw X-axis
        drawLine(Color.Black, Offset(leftPadding, chartHeight + topPadding), Offset(size.width - rightPadding, chartHeight + topPadding), strokeWidth = 2f)

        val yMax = 100f
        val xSpacing = chartWidth / (sortedDataPoints.size + 1)

        val points = mutableListOf<Offset>()

        // Draw the points and collect their positions
        sortedDataPoints.entries.forEachIndexed { index, entry ->
            val xPos = leftPadding + (index * xSpacing)
            val yPos = topPadding + (chartHeight - (entry.value / yMax * chartHeight))

            points.add(Offset(xPos, yPos))
            drawCircle(Color.Blue, 6f, Offset(xPos, yPos))

            // Draw x-axis labels at -45 degrees
            with(drawContext.canvas.nativeCanvas) {
                save()
                rotate(-45f, xPos, chartHeight + topPadding + 50f)
                drawText(
                        entry.key,
                        xPos,
                        chartHeight + topPadding + 50f,
                        android.graphics.Paint().apply {
                            textSize = 35f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                )
                restore()
            }
        }

        // Draw connecting lines
        for (i in 0 until points.size - 1) {
            drawLine(Color.Blue, points[i], points[i + 1], strokeWidth = 3f)
        }

        // Draw y-axis percentage labels
        val yLabelCount = 5
        for (i in 0..yLabelCount) {
            val yLabelValue = (i * yMax / yLabelCount)
            val yLabelPos = topPadding + (chartHeight - (yLabelValue / yMax * chartHeight))

            drawContext.canvas.nativeCanvas.drawText(
                    "${yLabelValue.toInt()}%",
                    leftPadding - 15f,
                    yLabelPos + 15f,
                    android.graphics.Paint().apply {
                        textSize = 40f
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
            )
        }
    }
}

fun preprocessDataForGraph(
        groupedData: Map<String, Float>,
        timeFrame: String
): Map<String, Float> {
    return when (timeFrame) {
        "24h" -> {
            // Keys are already in "HH:mm" format
            groupedData
        }
        "1w" -> {
            // Convert date-based keys (e.g., "2025-01-28") to week numbers (e.g., "Week 5")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            groupedData.mapNotNull { (key, value) ->
                val date = dateFormat.parse(key)
                if (date != null) {
                    val calendar = Calendar.getInstance().apply { time = date }
                    val weekNumber = calendar.get(Calendar.WEEK_OF_MONTH)
                    "Week $weekNumber" to value
                } else {
                    null // Skip if parsing fails
                }
            }.toMap()
        }
        "1m" -> {
            // Handle "1m" format where keys are "yyyy-MM" (e.g., "2025-01")
            val inputFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

            groupedData.mapNotNull { (key, value) ->
                val date = inputFormat.parse(key)
                if (date != null) {
                    outputFormat.format(date) to value
                } else {
                    null // Skip if parsing fails
                }
            }.toMap()
        }
        "1d" -> {
            // Handle "1d" format where keys are "yyyy-MM-dd"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            groupedData.mapNotNull { (key, value) ->
                val date = inputFormat.parse(key)
                if (date != null) {
                    outputFormat.format(date) to value
                } else {
                    null // Skip if parsing fails
                }
            }.toMap()
        }
        else -> {
            throw IllegalArgumentException("Invalid timeframe: $timeFrame")
        }
    }
}

fun groupDataByTimeFrame(quizStatsList: List<Map<String, Any>>, timeFrame: String, startDate: Date?, endDate: Date?): Map<String, Float> {
    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
    val result = mutableMapOf<String, MutableList<Float>>()

    for (quizStat in quizStatsList) {
        val dateStr = quizStat["date_time"] as String
        val correctAnswers = quizStat["correct_answers"] as Int
        val incorrectAnswers = quizStat["incorrect_answers"] as Int

        val date = dateFormat.parse(dateStr) ?: continue

        // Check if the date is within the selected period
        if (startDate != null && endDate != null && (date.before(startDate) || date.after(endDate))) {
            continue
        }

        // Calculate percentage correct
        val totalAnswers = correctAnswers + incorrectAnswers
        val correctPercentage = (correctAnswers.toFloat() / totalAnswers) * 100

        // Grouping based on the time frame
        val timeKey = when (timeFrame) {
            "1d" -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
            "1w" -> {
                val daysDifference = (date.time - (startDate?.time ?: 0)) / (1000 * 60 * 60 * 24)
                val weekStartDate = Date((startDate?.time ?: 0) + ((daysDifference / 7) * 7 * 24 * 60 * 60 * 1000))
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(weekStartDate)
            }
            "1m" -> SimpleDateFormat("yyyy-MM", Locale.US).format(date)
            "24h" -> {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                timeFormat.format(date)
            }
            else -> throw IllegalArgumentException("Invalid time frame: $timeFrame")
        }

        if (!result.containsKey(timeKey)) {
            result[timeKey] = mutableListOf()
        }
        result[timeKey]?.add(correctPercentage)
    }

    val finalResult = mutableMapOf<String, Float>()
    for ((key, percentages) in result) {
        finalResult[key] = percentages.average().toFloat()
    }

    return finalResult
}

fun updateGraph(
        quizStats: List<Map<String, Any>>,
        timeFrame: String,
        startDate: Date?,
        endDate: Date?,
        onGraphUpdated: (Map<String, Float>) -> Unit
) {

    // Group data based on the selected parameters
    val groupedData = groupDataByTimeFrame(quizStats, timeFrame, startDate, endDate)

    // Preprocess the grouped data for the graph (format keys only)
    val processedData = preprocessDataForGraph(groupedData, timeFrame)

    // Notify the caller with the processed data
    onGraphUpdated(processedData)
}

fun updateGraphData(
        quizStats: List<Map<String, Any>>,
        selectedTab: Int,
        selectedDate: Date,
        startDate: Date?,
        endDate: Date?,
        selectedTimeFrame: String,
        onGraphUpdated: (Map<String, Float>) -> Unit
) {

    // Determine the dates and time frame based on the selected tab
    // Convert selectedTimeFrame based on conditions
    val convertedTimeFrame = when (selectedTimeFrame) {
        "Hourly" -> "24h"   // If selectedTimeFrame is "Hourly", assign "24h"
        "Daily" -> "1d"   // If selectedTimeFrame is "Daily", assign "1d"
        "Weekly" -> "1w"  // If selectedTimeFrame is "Weekly", assign "1w"
        "Monthly" -> "1m" // If selectedTimeFrame is "Monthly", assign "1m"
        else -> throw IllegalArgumentException("Invalid selectedTimeFrame: $selectedTimeFrame")
    }
    // Determine the dates and time frame based on the selected tab
    val (currentStartDate, currentEndDate, currentTimeFrame) = when (selectedTab) {
        0 -> {
            // For the "Choose Day" tab, set the start and end dates to the selected day
            val startOfDay = getStartOfDay(selectedDate)
            val endOfDay = getEndOfDay(selectedDate)
            Triple(startOfDay, endOfDay, "24h")
        }
        1 -> {
            // For the "Timeframe" tab, use the provided startDate and endDate
            Triple(startDate, endDate, convertedTimeFrame)
        }
        else -> throw IllegalArgumentException("Invalid tab: $selectedTab")
    }

    // Call updateGraph to process the data and update the graph
    updateGraph(
            quizStats = quizStats,
            timeFrame = currentTimeFrame,
            startDate = currentStartDate,
            endDate = currentEndDate,
            onGraphUpdated = onGraphUpdated
    )
}

fun getStartOfDay(date: Date): Date {
    val calendar = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.time
}

fun getEndOfDay(date: Date): Date {
    val calendar = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calendar.time
}


fun formatDateTime(dateTime: String): String {
    val inputFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    val outputFormat = SimpleDateFormat("yyyy, h:mma", Locale.US) // Format only year and time

    val date = inputFormat.parse(dateTime) ?: return ""
    val calendar = Calendar.getInstance().apply { time = date }

    val day = calendar.get(Calendar.DAY_OF_MONTH)

    // Determine ordinal suffix (st, nd, rd, th)
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    // Extract the correctly formatted month separately
    val month = SimpleDateFormat("MMM", Locale.US).format(date)

    // Combine everything properly
    return "$month $day$suffix ${outputFormat.format(date)}"
}

fun extractQuizTitle(quizCategory: String): String {
    return quizCategory.substringBefore(" by ").trim()
}

fun showStats(context: Context, statId: String) {
    // Instantiate the DB helper
    val dbHelper = QuizDatabaseHelper(context)

    // Retrieve the stats associated with the statId
    val quizStat = dbHelper.getQuizStatById(statId)

    // Check if the quizStat is not null before proceeding
    if (quizStat != null) {
        // Extract relevant data from the Map
        val correctAnswers = quizStat["correct_answers"] as Int
        val incorrectAnswers = quizStat["incorrect_answers"] as Int
        val totalQuestions = quizStat["total_questions"] as Int
        val timeStamp = Util.formatTimestamp(quizStat["date_time"] as String)

        val categoryPerformanceJson = quizStat["category_performance"] as String

        // Prepare the intent to pass the data to StatsActivity
        val intent = Intent(context, StatsActivity::class.java).apply {
            putExtra("CORRECT_ANSWERS", correctAnswers)
            putExtra("INCORRECT_ANSWERS", incorrectAnswers)
            putExtra("TOTAL_QUESTIONS", totalQuestions)
            putExtra("TIME_STAMP", timeStamp)
        }

        try {
            val jsonObject = JSONObject(categoryPerformanceJson)

            // Create a Bundle to hold the category performance values
            val categoryPerformanceBundle = Bundle()

            // Loop through the keys in the JSON object and put them into the Bundle
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                categoryPerformanceBundle.putFloat(key, jsonObject.getInt(key).toFloat())
            }

            // Pass the categoryPerformanceBundle with Intent
            intent.putExtra("CATEGORY_PERFORMANCE", categoryPerformanceBundle)
        } catch (e: JSONException) {
            // Handle the JSONException appropriately
            // e.printStackTrace()
            // Optionally show a user-friendly message or take other actions
        }

        // Start StatsActivity
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No data found for statId: $statId", Toast.LENGTH_LONG).show()
    }
}