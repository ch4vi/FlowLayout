package com.ch4vi.flowlayout

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.widget.Button
import android.widget.EditText
import com.ch4vi.flowlayoutmanager.FlowLayoutManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById(R.id.recycler_view) as RecyclerView
        recyclerView.setHasFixedSize(true)

        recyclerView.itemAnimator.addDuration = 1000
        recyclerView.itemAnimator.changeDuration = 1000
        recyclerView.itemAnimator.moveDuration = 500
        recyclerView.itemAnimator.removeDuration = 500

        val manager = FlowLayoutManager(3, RecyclerView.HORIZONTAL, object : FlowLayoutManager.Interface {
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
        recyclerView.adapter = Adapter((0..30).map { it.toString() }.toTypedArray())

        val input = findViewById(R.id.input) as EditText
        val leftButton = findViewById(R.id.left) as Button
        leftButton.text = "scroll to"
        leftButton.setOnClickListener {
            val position = input.text.toString().toInt()
            recyclerView.scrollToPosition(position)
        }

        val rightButton = findViewById(R.id.right) as Button
        rightButton.text = "smooth scroll to"
        rightButton.setOnClickListener {
            val position = input.text.toString().toInt()
            recyclerView.smoothScrollToPosition(position)
        }
    }
}
