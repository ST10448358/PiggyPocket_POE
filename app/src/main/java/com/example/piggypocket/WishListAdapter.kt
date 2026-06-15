package com.example.piggypocket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class WishListAdapter(
    private var items: List<WishListItem>,
    private val onAddSavings: (WishListItem) -> Unit,
    private val onEdit: (WishListItem) -> Unit,
    private val onDelete: (WishListItem) -> Unit
) : RecyclerView.Adapter<WishListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvWishIcon)
        val tvName: TextView = view.findViewById(R.id.tvWishName)
        val tvDate: TextView = view.findViewById(R.id.tvWishDate)
        val pbProgress: ProgressBar = view.findViewById(R.id.pbWishProgress)
        val tvSaved: TextView = view.findViewById(R.id.tvWishSaved)
        val tvRemaining: TextView = view.findViewById(R.id.tvWishRemaining)
        val btnAddSavings: AppCompatButton = view.findViewById(R.id.btnAddSavings)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wish_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isAchieved = item.currentAmount >= item.targetAmount
        
        holder.tvIcon.text = if (isAchieved) "✅" else item.icon
        holder.tvName.text = item.name
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.tvDate.text = if (isAchieved) "Completed!" else "Target: ${dateFormat.format(Date(item.targetDate))}"
        
        val progress = if (item.targetAmount > 0) (item.currentAmount / item.targetAmount * 100).toInt() else 0
        holder.pbProgress.progress = progress
        
        holder.tvSaved.text = "R${String.format("%.2f", item.currentAmount)} saved of R${String.format("%.2f", item.targetAmount)}"
        val remaining = maxOf(0.0, item.targetAmount - item.currentAmount)
        holder.tvRemaining.text = if (isAchieved) "Goal Reached! 🎉" else "R${String.format("%.2f", remaining)} to go"

        if (isAchieved) {
            holder.btnEdit.visibility = View.GONE
            holder.btnDelete.visibility = View.GONE
            holder.btnAddSavings.visibility = View.GONE
        } else {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnAddSavings.visibility = View.VISIBLE
            holder.btnAddSavings.setOnClickListener { onAddSavings(item) }
            holder.btnEdit.setOnClickListener { onEdit(item) }
            holder.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<WishListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
