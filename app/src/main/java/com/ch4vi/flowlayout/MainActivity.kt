package com.ch4vi.flowlayout

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import com.ch4vi.flowlayoutmanager.FlowLayoutManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val recyclerView = findViewById(R.id.recycler_view) as RecyclerView
        recyclerView.setHasFixedSize(true)

        recyclerView.itemAnimator.addDuration = 1000
        recyclerView.itemAnimator.changeDuration = 1000
        recyclerView.itemAnimator.moveDuration = 500
        recyclerView.itemAnimator.removeDuration = 500

        val manager = FlowLayoutManager(3, RecyclerView.VERTICAL, object : FlowLayoutManager.Interface {
            override fun getProportionalSizeForChild(position: Int): Pair<Int, Int> {
                return when (position) {
                    0 -> Pair(1, 1)
                    1 -> Pair(2, 2)
                    2 -> Pair(4, 1)
                    3 -> Pair(3, 2)
                    else -> Pair(1, 1)
                }
            }
        })
        recyclerView.layoutManager = manager
        recyclerView.addItemDecoration(manager.InsetDecoration(this))
        recyclerView.adapter = Adapter((0..30).map(Int::toString).toTypedArray())

        val input = findViewById(R.id.input) as EditText
        val leftButton = findViewById(R.id.button_left) as Button
        leftButton.text = "scroll to"
        leftButton.setOnClickListener {
            val position = input.text.toString().toInt()
            recyclerView.scrollToPosition(position)
        }

        val centerButton = findViewById(R.id.button_center) as Button
        centerButton.text = "smooth scroll to"
        centerButton.setOnClickListener {
            val position = input.text.toString().toInt()
            recyclerView.smoothScrollToPosition(position)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_orientantion, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return true
        when (item.itemId) {
            R.id.orientation_horizontal -> {
                startActivity(Intent(this, HorizontalActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
