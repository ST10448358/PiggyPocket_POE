package com.example.piggypocket

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WishListActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var rvWishList: RecyclerView
    private lateinit var adapter: WishListAdapter
    private lateinit var tvOverallPercent: TextView
    private lateinit var pbOverallProgress: ProgressBar
    private lateinit var tvOverallStats: TextView
    private lateinit var cvOverallProgress: CardView
    private lateinit var llEmptyState: View

    private var selectedWishIcon: String = "🎁"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wish_list)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        rvWishList = findViewById(R.id.rvWishList)
        tvOverallPercent = findViewById(R.id.tvOverallPercent)
        pbOverallProgress = findViewById(R.id.pbOverallProgress)
        tvOverallStats = findViewById(R.id.tvOverallStats)
        cvOverallProgress = findViewById(R.id.cvOverallProgress)
        llEmptyState = findViewById(R.id.llEmptyState)

        rvWishList.layoutManager = LinearLayoutManager(this)
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<FloatingActionButton>(R.id.fabAddWish).setOnClickListener {
            showWishItemDialog(null)
        }

        loadWishList()
    }

    private fun loadWishList() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val items = db.wishListDao().getWishListForUser(userId)
            
            if (items.isEmpty()) {
                llEmptyState.visibility = View.VISIBLE
                rvWishList.visibility = View.GONE
                cvOverallProgress.visibility = View.GONE
            } else {
                llEmptyState.visibility = View.GONE
                rvWishList.visibility = View.VISIBLE
                cvOverallProgress.visibility = View.VISIBLE
                setupRecyclerView(items)
                calculateOverallProgress(items)
            }
        }
    }

    private fun setupRecyclerView(items: List<WishListItem>) {
        adapter = WishListAdapter(
            items,
            onAddSavings = { item -> showAddSavingsDialog(item) },
            onEdit = { item -> showWishItemDialog(item) },
            onDelete = { item -> deleteItem(item) }
        )
        rvWishList.adapter = adapter
    }

    private fun calculateOverallProgress(items: List<WishListItem>) {
        val totalTarget = items.sumOf { it.targetAmount }
        val totalSaved = items.sumOf { it.currentAmount }
        val percent = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0
        
        tvOverallPercent.text = "$percent%"
        pbOverallProgress.progress = percent
        tvOverallStats.text = "R${String.format("%.2f", totalSaved)} saved of R${String.format("%.2f", totalTarget)}"
    }

    private fun showWishItemDialog(item: WishListItem?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wish_item, null)
        val etName = dialogView.findViewById<EditText>(R.id.etWishName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etWishAmount)
        val etAlreadySaved = dialogView.findViewById<EditText>(R.id.etWishAlreadySaved)
        val etDate = dialogView.findViewById<EditText>(R.id.etWishDate)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val btnCreate = dialogView.findViewById<Button>(R.id.btnCreate)
        val layoutWishIcons = dialogView.findViewById<LinearLayout>(R.id.layoutWishIcons)

        var selectedDate = item?.targetDate ?: System.currentTimeMillis()
        selectedWishIcon = item?.icon ?: "🎁"
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        if (item != null) {
            tvTitle.text = "Edit Wish Goal"
            etName.setText(item.name)
            etAmount.setText(item.targetAmount.toString())
            etAlreadySaved.setText(item.currentAmount.toString())
            etDate.setText(dateFormat.format(Date(item.targetDate)))
            btnCreate.text = "Update"
        }

        // Setup Icons
        for (i in 0 until layoutWishIcons.childCount) {
            val child = layoutWishIcons.getChildAt(i) as TextView
            child.isSelected = (child.tag == selectedWishIcon)
            child.setOnClickListener {
                for (j in 0 until layoutWishIcons.childCount) {
                    layoutWishIcons.getChildAt(j).isSelected = false
                }
                child.isSelected = true
                selectedWishIcon = child.tag.toString()
            }
        }

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedDate
            DatePickerDialog(this, { _, y, m, d ->
                val newCal = Calendar.getInstance()
                newCal.set(y, m, d)
                selectedDate = newCal.timeInMillis
                etDate.setText(dateFormat.format(newCal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCreate.setOnClickListener {
            val name = etName.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val saved = etAlreadySaved.text.toString().toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty() && amount > 0) {
                lifecycleScope.launch {
                    if (item == null) {
                        db.wishListDao().insert(WishListItem(
                            userId = sessionManager.getUserId(), 
                            name = name, 
                            targetAmount = amount, 
                            currentAmount = saved,
                            targetDate = selectedDate,
                            icon = selectedWishIcon
                        ))
                    } else {
                        db.wishListDao().update(item.copy(
                            name = name, 
                            targetAmount = amount, 
                            currentAmount = saved,
                            targetDate = selectedDate,
                            icon = selectedWishIcon
                        ))
                    }
                    loadWishList()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showAddSavingsDialog(item: WishListItem) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount to add"
        
        AlertDialog.Builder(this)
            .setTitle("Add Savings to ${item.name}")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    lifecycleScope.launch {
                        db.wishListDao().update(item.copy(currentAmount = item.currentAmount + amount))
                        loadWishList()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: WishListItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Are you sure you want to delete '${item.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.wishListDao().delete(item)
                    loadWishList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
