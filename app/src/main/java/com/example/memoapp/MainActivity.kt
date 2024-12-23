package com.example.memoapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoapp.adapter.MemoAdapter
import com.example.memoapp.data.Memo
import com.example.memoapp.data.MemoDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var db: MemoDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var addButton: Button

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // 初始化视图
            initializeViews()

            // 初始化数据库
            initializeDatabase()
            
            // 设置 RecyclerView
            setupRecyclerView()
            
            // 设置添加按钮
            setupAddButton()
            
            // 加载数据
            loadMemos()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            showError("初始化失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除适配器引用
        recyclerView.adapter = null
        db.close()
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerView)
            titleEditText = findViewById(R.id.etTitle)
            contentEditText = findViewById(R.id.etContent)
            addButton = findViewById(R.id.btnAdd)
        } catch (e: Exception) {
            Log.e(TAG, "initializeViews error: ${e.message}", e)
            throw e
        }
    }

    private fun initializeDatabase() {
        try {
            db = MemoDatabase.getDatabase(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "initializeDatabase error: ${e.message}", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            memoAdapter = MemoAdapter(
                onDeleteClick = { memo -> deleteMemo(memo) },
                onItemClick = { memo -> openMemoDetail(memo) }
            )
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = memoAdapter
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupRecyclerView error: ${e.message}", e)
            showError("设置列表失败: ${e.message}")
        }
    }

    private fun setupAddButton() {
        try {
            addButton.setOnClickListener {
                val title = titleEditText.text.toString().trim()
                val content = contentEditText.text.toString().trim()
                
                when {
                    title.isEmpty() -> showError("标题不能为空")
                    content.isEmpty() -> showError("内容不能为空")
                    else -> {
                        addMemo(Memo(title = title, content = content))
                        clearInputs()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupAddButton error: ${e.message}", e)
            showError("设置按钮失败: ${e.message}")
        }
    }

    private fun clearInputs() {
        titleEditText.text.clear()
        contentEditText.text.clear()
    }

    private fun loadMemos() {
        lifecycleScope.launch {
            try {
                val memos = db.memoDao().getAllMemos()
                memoAdapter.submitList(memos)
            } catch (e: Exception) {
                showError("加载数据失败: ${e.message}")
            }
        }
    }

    private fun addMemo(memo: Memo) {
        lifecycleScope.launch {
            try {
                db.memoDao().insert(memo)
                loadMemos()
            } catch (e: Exception) {
                showError("添加备忘录失败: ${e.message}")
            }
        }
    }

    private fun deleteMemo(memo: Memo) {
        lifecycleScope.launch {
            try {
                db.memoDao().delete(memo)
                loadMemos()
            } catch (e: Exception) {
                showError("删除备忘录失败: ${e.message}")
            }
        }
    }

    private fun openMemoDetail(memo: Memo) {
        val intent = Intent(this, MemoDetailActivity::class.java).apply {
            putExtra(MemoDetailActivity.EXTRA_TITLE, memo.title)
            putExtra(MemoDetailActivity.EXTRA_CONTENT, memo.content)
            putExtra(MemoDetailActivity.EXTRA_DATE, memo.createdAt.time)
        }
        startActivity(intent)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}