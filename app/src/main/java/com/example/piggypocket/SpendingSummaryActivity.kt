package com.example.piggypocket

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SpendingSummaryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var combinedChart: CombinedChart
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvSpendingInsight: TextView
    private lateinit var rvBreakdown: RecyclerView
    
    private lateinit var clFilterSection: View
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    
    private var startDate: Long? = null
    private var endDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_summary)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        
        combinedChart = findViewById(R.id.combinedChart)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvSpendingInsight = findViewById(R.id.tvSpendingInsight)
        rvBreakdown = findViewById(R.id.rvBreakdown)
        
        clFilterSection = findViewById(R.id.clFilterSection)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)

        rvBreakdown.layoutManager = LinearLayoutManager(this)

        setupCombinedChart()
        setupFilters()
        
        // Default to current month
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        startDate = cal.timeInMillis
        
        val endCal = Calendar.getInstance()
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endDate = endCal.timeInMillis

        tvStartDate.text = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(startDate!!))
        tvEndDate.text = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(endDate!!))
        
        loadSpendingData()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun setupFilters() {
        findViewById<ImageButton>(R.id.btnFilter).setOnClickListener {
            clFilterSection.visibility = if (clFilterSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val dateSetListener = { isStart: Boolean ->
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth, if (isStart) 0 else 23, if (isStart) 0 else 59, if (isStart) 0 else 59)
                val dateLong = selectedCal.timeInMillis
                val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                
                if (isStart) {
                    startDate = dateLong
                    tvStartDate.text = format.format(Date(dateLong))
                } else {
                    endDate = dateLong
                    tvEndDate.text = format.format(Date(dateLong))
                }
                loadSpendingData()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvStartDate.setOnClickListener { dateSetListener(true) }
        tvEndDate.setOnClickListener { dateSetListener(false) }
    }

    private fun setupCombinedChart() {
        combinedChart.description.isEnabled = false
        combinedChart.setDrawGridBackground(false)
        combinedChart.setDrawBarShadow(false)
        combinedChart.isHighlightFullBarEnabled = false
        
        // Draw bars behind lines
        combinedChart.drawOrder = arrayOf(
            CombinedChart.DrawOrder.BAR,
            CombinedChart.DrawOrder.LINE
        )

        val xAxis = combinedChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.GRAY
        xAxis.textSize = 10f
        xAxis.granularity = 1f

        val leftAxis = combinedChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LTGRAY
        leftAxis.textColor = Color.GRAY
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f

        combinedChart.axisRight.isEnabled = false
    }

    private fun loadSpendingData() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            val allExpenses = db.expenseDao().getAllExpenses(userId)
            val categoriesList = db.categoryDao().getAllCategories(userId)
            val categoriesMap = categoriesList.associateBy { it.id }

            // Filter expenses by selected period
            val filteredExpenses = allExpenses.filter { 
                it.date >= (startDate ?: 0) && it.date <= (endDate ?: Long.MAX_VALUE)
            }

            val totalSpending = filteredExpenses.sumOf { it.amount }
            tvTotalAmount.text = "R %.2f".format(totalSpending)

            // Timeline Data Preparation
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDate ?: filteredExpenses.minOfOrNull { it.date } ?: System.currentTimeMillis()
            
            val endMillis = endDate ?: filteredExpenses.maxOfOrNull { it.date } ?: System.currentTimeMillis()
            
            val labels = mutableListOf<String>()
            val barEntries = mutableListOf<BarEntry>()
            val lineEntries = mutableListOf<Entry>()
            
            var cumulativeSum = 0.0
            var index = 0f
            
            // Iterate through each day in the range
            val currentCal = Calendar.getInstance()
            currentCal.timeInMillis = calendar.timeInMillis
            
            val dayMillis = 24 * 60 * 60 * 1000L
            
            while (currentCal.timeInMillis <= endMillis) {
                val startOfDay = currentCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
                val endOfDay = currentCal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
                
                val dayExpenses = filteredExpenses.filter { it.date in startOfDay..endOfDay }
                
                // Stack by categories for the bar
                val categoryValues = FloatArray(categoriesList.size)
                categoriesList.forEachIndexed { catIdx, category ->
                    val catSpend = dayExpenses.filter { it.categoryId == category.id }.sumOf { it.amount }
                    categoryValues[catIdx] = catSpend.toFloat()
                }
                
                val dayTotal = dayExpenses.sumOf { it.amount }
                cumulativeSum += dayTotal
                
                barEntries.add(BarEntry(index, categoryValues))
                lineEntries.add(Entry(index, cumulativeSum.toFloat()))
                labels.add(dateFormat.format(Date(startOfDay)))
                
                currentCal.timeInMillis += dayMillis
                index++
                
                // Limit to 31 days to avoid over-cluttering
                if (index > 31) break 
            }

            // Create Chart Data
            val data = CombinedData()
            
            // Bar Data (Daily Spending Stacks)
            val barDataSet = BarDataSet(barEntries, "Daily Spending")
            barDataSet.colors = getCategoryColors(categoriesList.size)
            barDataSet.setDrawValues(false)
            data.setData(BarData(barDataSet))
            
            // Line Data (Cumulative Progress)
            val lineDataSet = LineDataSet(lineEntries, "Cumulative Spending")
            lineDataSet.color = Color.parseColor("#9C27B0") // Purple
            lineDataSet.lineWidth = 2.5f
            lineDataSet.setCircleColor(Color.parseColor("#9C27B0"))
            lineDataSet.circleRadius = 3f
            lineDataSet.setDrawCircleHole(false)
            lineDataSet.valueTextSize = 0f
            lineDataSet.setDrawValues(false)
            lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            data.setData(LineData(lineDataSet))

            combinedChart.data = data
            
            // Axis Configuration
            combinedChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            combinedChart.xAxis.labelCount = if (labels.size > 7) 7 else labels.size
            
            val leftAxis = combinedChart.axisLeft
            leftAxis.removeAllLimitLines()
            
            var maxVal = maxOf(cumulativeSum.toFloat(), 100f)
            
            user?.let {
                if (it.minMonthlyGoal > 0) {
                    val minLine = LimitLine(it.minMonthlyGoal.toFloat(), "Min Goal")
                    minLine.lineColor = Color.parseColor("#4CAF50") // Green
                    minLine.lineWidth = 2f
                    minLine.enableDashedLine(10f, 10f, 0f)
                    minLine.textColor = Color.parseColor("#4CAF50")
                    minLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    leftAxis.addLimitLine(minLine)
                    maxVal = maxOf(maxVal, it.minMonthlyGoal.toFloat())
                }
                
                if (it.maxMonthlyGoal > 0) {
                    val maxLine = LimitLine(it.maxMonthlyGoal.toFloat(), "Max Goal")
                    maxLine.lineColor = Color.parseColor("#F44336") // Red
                    maxLine.lineWidth = 2f
                    maxLine.enableDashedLine(10f, 10f, 0f)
                    maxLine.textColor = Color.parseColor("#F44336")
                    maxLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    leftAxis.addLimitLine(maxLine)
                    maxVal = maxOf(maxVal, it.maxMonthlyGoal.toFloat())
                }
            }
            
            leftAxis.axisMaximum = maxVal * 1.2f
            leftAxis.setDrawLimitLinesBehindData(false)

            combinedChart.animateY(1000)
            combinedChart.invalidate()

            // Update Detailed Breakdown and Insights
            updateBreakdownAndInsights(filteredExpenses, categoriesMap, totalSpending)
        }
    }

    private fun updateBreakdownAndInsights(expenses: List<Expense>, categories: Map<Int, Category>, totalSpending: Double) {
        val categoryTotals = mutableMapOf<String, Double>()
        for (expense in expenses) {
            val name = categories[expense.categoryId]?.name ?: "Unknown"
            categoryTotals[name] = categoryTotals.getOrDefault(name, 0.0) + expense.amount
        }

        val breakdownItems = mutableListOf<CategoryBreakdownAdapter.CategoryBreakdownItem>()
        var maxVal = 0.0
        var topCat = ""
        val colors = getCategoryColors(categoryTotals.size)

        categoryTotals.toList().sortedByDescending { it.second }.forEachIndexed { index, (name, amount) ->
            val percentage = if (totalSpending > 0) (amount / totalSpending) * 100 else 0.0
            breakdownItems.add(CategoryBreakdownAdapter.CategoryBreakdownItem(name, amount, percentage, colors[index % colors.size]))
            if (amount > maxVal) { maxVal = amount; topCat = name }
        }

        rvBreakdown.adapter = CategoryBreakdownAdapter(breakdownItems)
        
        if (topCat.isNotEmpty()) {
            val amountText = "R %.2f".format(maxVal)
            val fullText = "Top category: $topCat ($amountText) for this period."
            val spannable = SpannableString(fullText)
            val catStart = fullText.indexOf(topCat)
            if (catStart != -1) {
                spannable.setSpan(StyleSpan(Typeface.BOLD), catStart, catStart + topCat.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF8F00")), catStart, catStart + topCat.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            tvSpendingInsight.text = spannable
        } else {
            tvSpendingInsight.text = "No spending data available for this period."
        }
    }

    private fun getCategoryColors(size: Int): List<Int> {
        val colors = mutableListOf<Int>()
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        return colors.take(size)
    }
}
