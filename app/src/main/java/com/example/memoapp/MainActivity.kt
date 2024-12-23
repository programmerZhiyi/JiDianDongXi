package com.example.memoapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var recyclerView: RecyclerView
    private val undoStack = mutableListOf<UndoAction>()

    // 定义撤回动作类型
    sealed class UndoAction {
        data class Delete(val memo: Memo) : UndoAction()
        data class Add(val memo: Memo) : UndoAction()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_EDIT_MEMO = 1
        private const val REQUEST_NEW_MEMO = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            initializeViews()
            
            // 设置 Toolbar
            val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            
            setupRecyclerView()
            loadMemos()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            showError("初始化失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView.adapter = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new -> {
                startNewMemo()
                true
            }
            R.id.action_undo -> {
                undo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.recyclerView)
        } catch (e: Exception) {
            Log.e(TAG, "initializeViews error: ${e.message}", e)
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

    private fun startNewMemo() {
        val intent = Intent(this, MemoDetailActivity::class.java)
        startActivityForResult(intent, REQUEST_NEW_MEMO)
    }

    private fun loadMemos() {
        lifecycleScope.launch {
            try {
                val db = MemoDatabase.getDatabase(applicationContext)
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
                val db = MemoDatabase.getDatabase(applicationContext)
                db.memoDao().insert(memo)
                undoStack.add(UndoAction.Add(memo))
                loadMemos()
            } catch (e: Exception) {
                showError("添加备忘录失败: ${e.message}")
            }
        }
    }

    private fun deleteMemo(memo: Memo) {
        lifecycleScope.launch {
            try {
                val db = MemoDatabase.getDatabase(applicationContext)
                db.memoDao().delete(memo)
                undoStack.add(UndoAction.Delete(memo))
                loadMemos()
            } catch (e: Exception) {
                showError("删除备忘录失败: ${e.message}")
            }
        }
    }

    private fun openMemoDetail(memo: Memo) {
        val intent = Intent(this, MemoDetailActivity::class.java).apply {
            putExtra(MemoDetailActivity.EXTRA_ID, memo.id)
            putExtra(MemoDetailActivity.EXTRA_TITLE, memo.title)
            putExtra(MemoDetailActivity.EXTRA_CONTENT, memo.content)
            putExtra(MemoDetailActivity.EXTRA_DATE, memo.createdAt.time)
        }
        startActivityForResult(intent, REQUEST_EDIT_MEMO)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun undo() {
        if (undoStack.isEmpty()) {
            showError("没有可回的操作")
            return
        }

        lifecycleScope.launch {
            try {
                val db = MemoDatabase.getDatabase(applicationContext)
                when (val action = undoStack.removeLastOrNull()) {
                    is UndoAction.Delete -> db.memoDao().insert(action.memo)
                    is UndoAction.Add -> db.memoDao().delete(action.memo)
                    null -> return@launch
                }
                loadMemos()
                showError("已撤回上一步操作")
            } catch (e: Exception) {
                showError("撤回失败: ${e.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_EDIT_MEMO || requestCode == REQUEST_NEW_MEMO) 
            && resultCode == RESULT_OK) {
            loadMemos()
        }
    }
}