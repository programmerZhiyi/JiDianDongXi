package com.example.memoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.memoapp.data.Memo
import com.example.memoapp.data.MemoDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import android.util.TypedValue
import android.view.Gravity
import com.google.android.material.appbar.MaterialToolbar
import android.util.Log

class MemoDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }

    private lateinit var titleView: EditText
    private lateinit var dateView: TextView
    private var memoId = 0
    private var currentImagePath: String = ""
    private val PICK_IMAGE_REQUEST = 1

    // 添加一个列表来存储所有内容（文本和图片）
    private data class ContentItem(
        val type: Type,
        var content: String
    ) {
        enum class Type { TEXT, IMAGE }
    }
    
    private val contentItems = mutableListOf<ContentItem>()
    private var currentEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_detail)

        try {
            // 初始化视图
            titleView = findViewById(R.id.tvDetailTitle)
            dateView = findViewById(R.id.tvDetailDate)

            // 获取传递的数据
            memoId = intent.getIntExtra(EXTRA_ID, 0)
            val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
            val date = intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis())

            // 设置 Toolbar
            val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = if (memoId == 0) "新建备忘录" else "编辑备忘录"

            // 显示标题和日期
            titleView.setText(title)
            dateView.text = SimpleDateFormat(
                "yyyy-MM-dd HH:mm", Locale.getDefault()
            ).format(Date(date))

            // 解析已有内容
            val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
            parseContent(content)
            displayContent()

            // 添加文本变化监听
            setupTextChangeListeners()
        } catch (e: Exception) {
            Log.e("MemoDetailActivity", "onCreate error: ${e.message}", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupTextChangeListeners() {
        titleView.addTextChangedListener(createTextWatcher(titleView))
    }

    private fun createTextWatcher(editText: EditText): android.text.TextWatcher {
        return object : android.text.TextWatcher {
            private var beforeText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeText = s?.toString() ?: ""
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val afterText = s?.toString() ?: ""
                if (beforeText != afterText) {
                    // 更新对应的内容
                    if (editText != titleView) {
                        val position = contentItems.indexOfFirst { 
                            it.type == ContentItem.Type.TEXT && 
                            it.content == beforeText 
                        }
                        if (position != -1) {
                            contentItems[position].content = afterText
                        }
                    }
                    currentEditText = editText
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        val title = titleView.text.toString().trim()
        val hasImage = contentItems.any { it.type == ContentItem.Type.IMAGE }
        val content = buildString {
            contentItems.forEach { item ->
                if (item.type == ContentItem.Type.TEXT) {
                    append(item.content)
                }
            }
        }

        // 如果是新建模式且没有任何内容（包括图片），直接返回
        if (memoId == 0 && title.isEmpty() && content.isEmpty() && !hasImage) {
            finish()
            return
        }

        // 如果有任何内容，尝试保存
        saveMemo()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun saveMemo() {
        val title = titleView.text.toString().trim()
        val content = buildString {
            contentItems.forEach { item -> 
                when (item.type) {
                    ContentItem.Type.TEXT -> append(item.content)
                    ContentItem.Type.IMAGE -> append("[image]${item.content}[/image]")
                }
            }
        }

        // 修改判断条件：如果只有图片也允许保存
        val hasImage = contentItems.any { it.type == ContentItem.Type.IMAGE }
        if (title.isEmpty() && content.isEmpty() && !hasImage) {
            Toast.makeText(this, "标题、内容或图片至少需要一项", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = MemoDatabase.getDatabase(applicationContext)
                val memo = Memo(
                    id = memoId,
                    title = title,  // 不再判断空标题
                    content = content,
                    createdAt = if (memoId == 0) Date() else Date(intent.getLongExtra(EXTRA_DATE, 0))
                )
                
                if (memoId == 0) {
                    db.memoDao().insert(memo)
                } else {
                    db.memoDao().update(memo)
                }
                
                // 清理未使用的图片
                cleanupUnusedImages()
                
                Toast.makeText(this@MemoDetailActivity, "保存成功", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MemoDetailActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 创建选项菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        return true
    }

    // 处理菜单项点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_add_image -> {
                pickImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun pickImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val imagePath = copyImageToPrivateStorage(uri)
                insertImageAtCursor(imagePath)
            }
        }
    }

    private fun copyImageToPrivateStorage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val file = File(getExternalFilesDir(null), fileName)
        
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return file.absolutePath
    }

    private fun parseContent(content: String) {
        contentItems.clear()
        
        // 使用正则表达式匹配图片标���和内容
        val pattern = "\\[image\\](.*?)\\[/image\\]".toRegex()
        var lastIndex = 0
        
        pattern.findAll(content).forEach { matchResult ->
            // 添加图片前的文本（不trim，保持原有格式）
            val textBefore = content.substring(lastIndex, matchResult.range.first)
            if (textBefore.isNotEmpty()) {
                contentItems.add(ContentItem(ContentItem.Type.TEXT, textBefore))
            }
            
            // 添加图片
            val imagePath = matchResult.groupValues[1]
            contentItems.add(ContentItem(ContentItem.Type.IMAGE, imagePath))
            
            lastIndex = matchResult.range.last + 1
        }
        
        // 添加最后的文本（不trim，保持原有格式）
        val remainingText = content.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            contentItems.add(ContentItem(ContentItem.Type.TEXT, remainingText))
        }
        
        // 确保至少有一项文本项
        if (contentItems.isEmpty()) {
            contentItems.add(ContentItem(ContentItem.Type.TEXT, ""))
        }
    }

    private fun displayContent() {
        val container = findViewById<LinearLayout>(R.id.contentContainer)
        container.removeAllViews()

        contentItems.forEach { item ->
            when (item.type) {
                ContentItem.Type.TEXT -> {
                    val editText = createEditText(item.content)
                    container.addView(editText)
                }
                ContentItem.Type.IMAGE -> {
                    val imageLayout = createImageLayout(item.content)
                    container.addView(imageLayout)
                }
            }
        }

        // 确保最后一项是文本框
        if (contentItems.isEmpty() || contentItems.last().type != ContentItem.Type.TEXT) {
            val editText = createEditText("")
            container.addView(editText)
            contentItems.add(ContentItem(ContentItem.Type.TEXT, ""))
        }
    }

    private fun createEditText(text: String): EditText {
        return EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(resources.getColor(android.R.color.black, null))
            background = null
            gravity = Gravity.TOP
            addTextChangedListener(createTextWatcher(this))
            setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
        }
    }

    private fun createImageLayout(imagePath: String): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8.dpToPx()
                bottomMargin = 8.dpToPx()
            }

            // 添加图片视图
            val imageView = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
            }
            addView(imageView)

            // 添加删除按钮
            val deleteButton = ImageButton(context).apply {
                layoutParams = FrameLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                background = ContextCompat.getDrawable(context, R.drawable.circle_background)
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                setOnClickListener { deleteImage(imagePath) }
            }
            addView(deleteButton)

            // 加载图片
            Glide.with(context)
                .load(imagePath)
                .into(imageView)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun insertImageAtCursor(imagePath: String) {
        contentItems.add(ContentItem(ContentItem.Type.IMAGE, imagePath))
        contentItems.add(ContentItem(ContentItem.Type.TEXT, ""))
        
        displayContent()
    }

    private fun deleteImage(imagePath: String) {
        val index = contentItems.indexOfFirst { it.type == ContentItem.Type.IMAGE && it.content == imagePath }
        if (index != -1) {
            // 如果图片前后都是文本，合并它们
            if (index > 0 && index < contentItems.size - 1 &&
                contentItems[index - 1].type == ContentItem.Type.TEXT &&
                contentItems[index + 1].type == ContentItem.Type.TEXT) {
                val mergedText = contentItems[index - 1].content + contentItems[index + 1].content
                contentItems[index - 1].content = mergedText
                contentItems.removeAt(index + 1)
            }
            contentItems.removeAt(index)
            displayContent()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理 Glide 加载的图片资源
        Glide.get(this).clearMemory()
    }

    private fun cleanupUnusedImages() {
        val imagesDir = getExternalFilesDir(null)
        imagesDir?.listFiles()?.forEach { file ->
            if (file.name.startsWith("image_") && !isImageUsed(file.absolutePath)) {
                file.delete()
            }
        }
    }

    private fun isImageUsed(imagePath: String): Boolean {
        // 检查图片是否被任何备忘录使用
        return contentItems.any { it.type == ContentItem.Type.IMAGE && it.content == imagePath }
    }
} 