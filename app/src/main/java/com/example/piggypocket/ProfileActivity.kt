package com.example.piggypocket

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private var currentUser: User? = null

    // UI Components
    private lateinit var ivAvatarPreview: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etCardNumber: EditText
    private lateinit var etCardExpiry: EditText
    private lateinit var etCardHolder: EditText
    private lateinit var layoutAvatarList: LinearLayout
    private lateinit var hsvAvatars: HorizontalScrollView
    private lateinit var tvChooseAvatar: TextView

    private var selectedAvatarName: String = "ic_person_placeholder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        initViews()
        setupAvatarSelection()
        loadUserData()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
        
        setupCardNumberFormatting()
    }

    private fun initViews() {
        ivAvatarPreview = findViewById(R.id.ivAvatarPreview)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etCardNumber = findViewById(R.id.etCardNumber)
        etCardExpiry = findViewById(R.id.etCardExpiry)
        etCardHolder = findViewById(R.id.etCardHolder)
        layoutAvatarList = findViewById(R.id.layoutAvatarList)
        hsvAvatars = findViewById(R.id.hsvAvatars)
        tvChooseAvatar = findViewById(R.id.tvChooseAvatar)
    }

    private fun setupAvatarSelection() {
        tvChooseAvatar.setOnClickListener {
            hsvAvatars.visibility = if (hsvAvatars.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val avatarNames = listOf(
            "avatar_1", "avatar_2", "avatar_3", "avatar_4", 
            "avatar_5", "avatar_6", "avatar_7", "avatar_8",
            "avatar_9", "avatar_10", "avatar_11", "avatar_12"
        )

        layoutAvatarList.removeAllViews()

        for (name in avatarNames) {
            val resId = resources.getIdentifier(name, "drawable", packageName)
            
            val imageView = ImageView(this)
            val size = (80 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(0, 0, (12 * resources.displayMetrics.density).toInt(), 0)
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            
            if (resId != 0) {
                imageView.setImageResource(resId)
            } else {
                // Show placeholder if image not found yet
                imageView.setImageResource(R.drawable.ic_person_placeholder)
                imageView.alpha = 0.5f
            }
            
            imageView.setBackgroundResource(R.drawable.bg_icon_selector)
            imageView.setPadding(8, 8, 8, 8)
            imageView.tag = name

            if (name == selectedAvatarName) {
                imageView.isSelected = true
            }
            
            imageView.setOnClickListener {
                selectedAvatarName = name
                updateAvatarPreview()
                
                // Clear other selections
                for (i in 0 until layoutAvatarList.childCount) {
                    layoutAvatarList.getChildAt(i).isSelected = false
                }
                imageView.isSelected = true
            }
            
            layoutAvatarList.addView(imageView)
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val user = db.userDao().getUserById(sessionManager.getUserId())
            currentUser = user
            user?.let {
                runOnUiThread {
                    etFullName.setText(it.fullName)
                    etEmail.setText(it.email)
                    etPhone.setText(it.phoneNumber)
                    etCardNumber.setText(it.bankCardNumber)
                    etCardExpiry.setText(it.bankCardExpiry)
                    etCardHolder.setText(it.bankCardHolder)
                    
                    selectedAvatarName = it.avatarResourceName
                    updateAvatarPreview()
                    updateAvatarListSelection()
                }
            }
        }
    }

    private fun updateAvatarListSelection() {
        for (i in 0 until layoutAvatarList.childCount) {
            val child = layoutAvatarList.getChildAt(i) as? ImageView ?: continue
            val tag = child.tag as? String
            child.isSelected = (tag == selectedAvatarName)
        }
    }

    private fun updateAvatarPreview() {
        val resId = resources.getIdentifier(selectedAvatarName, "drawable", packageName)
        if (resId != 0) {
            ivAvatarPreview.setImageResource(resId)
            if (selectedAvatarName.startsWith("avatar_")) {
                ivAvatarPreview.setPadding(0, 0, 0, 0)
                ivAvatarPreview.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            } else {
                ivAvatarPreview.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                ivAvatarPreview.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            }
        } else {
            ivAvatarPreview.setImageResource(R.drawable.ic_person_placeholder)
            ivAvatarPreview.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            ivAvatarPreview.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupCardNumberFormatting() {
        etCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().replace(" ", "")
                if (input.isNotEmpty() && input.length % 4 == 0 && s?.last() != ' ' && input.length < 16) {
                    s?.append(" ")
                }
            }
        })
    }

    private fun saveProfile() {
        currentUser?.let { user ->
            val updatedUser = user.copy(
                fullName = etFullName.text.toString(),
                email = etEmail.text.toString(),
                phoneNumber = etPhone.text.toString(),
                bankCardNumber = etCardNumber.text.toString(),
                bankCardExpiry = etCardExpiry.text.toString(),
                bankCardHolder = etCardHolder.text.toString(),
                avatarResourceName = selectedAvatarName
            )

            lifecycleScope.launch {
                db.userDao().update(updatedUser)
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
