@file:Suppress("unused")

package com.ch4vi.flowlayoutmanager

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import flowlayoutmanager.R
import android.support.v7.widget.RecyclerView.LayoutManager as BaseLayoutManager


class FlowLayoutManager(
        private val numberOfDivisions: Int,
        private val orientation: Int = RecyclerView.VERTICAL,
        private val anInterface: Interface
) : RecyclerView.LayoutManager() {
    private val TAG = this.javaClass.simpleName
    private val rectList: MutableList<Rect> = mutableListOf()
    private val viewToItemIndexMap: MutableMap<View, Int> = mutableMapOf()
    private val visibleChildren: MutableSet<Int> = mutableSetOf()
    private var verticalOffset: Int = 0
    private var horizontalOffset: Int = 0

    // region Layout Interface

    interface Interface {
        fun getProportionalSizeForChild(position: Int): Pair<Int, Int>
    }

    fun Interface.getSizeForChild(position: Int): Pair<Int, Int> {
        val (pWidth, pHeight) = this.getProportionalSizeForChild(position)
        return Pair(pWidth * columnWidth, pHeight * rowHeight)
    }

    // endregion

    // region Layout

    /*
     * We must override this method to provide the default
     * layout parameters that each child view will receive
     * when added.
     */
    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT)
    }

    /*
    * You must return true from this method if you want your
    * LayoutManager to support anything beyond "simple" item
    * animations. Enabling this causes onLayoutChildren() to
    * be called twice on each animated change; once for a
    * pre-layout, and again for the real layout.
    */
    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    /*
     * Called by RecyclerView when a view removal is triggered. This is called
     * before onLayoutChildren() in pre-layout if the views removed are not visible. We
     * use it in this case to inform pre-layout that a removal took place.
     *
     * This method is still called if the views removed were visible, but it will
     * happen AFTER pre-layout.
     */
    override fun onItemsRemoved(recyclerView: RecyclerView?, positionStart: Int, itemsRemoved: Int) {
        rectList.removeAt(rectList.lastIndex)
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        //Completely scrap the existing layout
        removeAllViews()
    }

    /*
     * This is a helper method used by RecyclerView to determine
     * if a specific child view can be returned.
     */
    override fun findViewByPosition(position: Int): View? {
        return viewToItemIndexMap.findKeyByValue(position)
    }

    /*
    * This method is your initial call from the framework. You will receive it when you
    * need to start laying out the initial set of views. This method will not be called
    * repeatedly, so don't rely on it to continually process changes during user
    * interaction.
    *
    * This method will be called when the data set in the adapter changes, so it can be
    * used to update a layout based on a new item count.
    *
    * If predictive animations are enabled, you will see this called twice. First, with
    * state.isPreLayout() returning true to lay out children in their initial conditions.
    * Then again to lay out children in their final locations.
    */
    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (itemCount <= 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }

        if (childCount <= 0 && state.isPreLayout) {
            return
        }

        if (isFirstLayoutPass) {
            updateLayout()
        }
        updateVisibleChildren()
        detachAndScrapAttachedViews(recycler)
        layoutChildren(recycler)
    }

    private fun updateLayout() {
        rectList.clear()
        var offset: LayoutState
        if (orientation == RecyclerView.VERTICAL) {
            offset = VerticalLayoutState()
        } else {
            offset = HorizontalLayoutState()
        }
        if (orientation == RecyclerView.VERTICAL) {
            for (i in 0..itemCount - 1) {
                var (width, height) = anInterface.getSizeForChild(i)
                if (offset.x <= 0) {
                    width += availableWidth - numberOfDivisions * columnWidth
                }
                val nextOffset = offset.pivot(width, height)
                if (nextOffset != null) {
                    offset = nextOffset
                    rectList.add(Rect(offset.x, offset.y, offset.x + width, offset.y + height))
                } else {
                    rectList.add(Rect(0, 0, 0, 0))
                }
            }
        } else {
            for (i in 0..itemCount - 1) {
                var (width, height) = anInterface.getSizeForChild(i)
                if (offset.y <= 0) {
                    height += availableHeight - numberOfDivisions * rowHeight
                }
                val nextOffset = offset.pivot(width, height)
                if (nextOffset != null) {
                    offset = nextOffset
                    rectList.add(Rect(offset.x, offset.y, offset.x + width, offset.y + height))
                } else {
                    rectList.add(Rect(0, 0, 0, 0))
                }
            }
        }
    }

    private fun detachAllChildren(): MutableMap<Int, View> {
        val viewCache = mutableMapOf<Int, View>()
        for (childIndex in 0..childCount - 1) {
            val view = getChildAt(childIndex)
            val itemIndex = viewToItemIndexMap[view] ?: continue
            viewCache[itemIndex] = view
        }
        viewCache.forEach { detachView(it.value) }
        return viewCache
    }

    private fun layoutChildren(recycler: RecyclerView.Recycler) {
        val viewCache = detachAllChildren()
        if (orientation == RecyclerView.VERTICAL) {
            visibleChildren.forEach { itemIndex ->
                val cachedView = viewCache[itemIndex]
                if (cachedView == null) {
                    val view = recycler.getViewForPosition(itemIndex)
                    val rect = rectList[itemIndex]
                    view.layoutParams.width = rect.width()
                    view.layoutParams.height = rect.height()
                    addView(view)
                    viewToItemIndexMap[view] = itemIndex
                    measureChildWithMargins(view, 0, 0)
                    val top = rect.top - verticalOffset
                    val bottom = rect.bottom - verticalOffset
                    layoutDecorated(view, rect.left, top, rect.right, bottom)
                } else {
                    attachView(cachedView)
                    viewCache.remove(itemIndex)
                }
            }
        } else {
            visibleChildren.forEach { itemIndex ->
                val cachedView = viewCache[itemIndex]
                if (cachedView == null) {
                    val view = recycler.getViewForPosition(itemIndex)
                    val rect = rectList[itemIndex]
                    view.layoutParams.width = rect.width()
                    view.layoutParams.height = rect.height()
                    addView(view)
                    viewToItemIndexMap[view] = itemIndex
                    measureChildWithMargins(view, 0, 0)
                    val left = rect.left - horizontalOffset
                    val right = rect.right - horizontalOffset
                    layoutDecorated(view, left, rect.top, right, rect.bottom)
                } else {
                    attachView(cachedView)
                    viewCache.remove(itemIndex)
                }
            }
        }

        viewCache.forEach { entry ->
            viewToItemIndexMap.remove(entry.value)
            recycler.recycleView(entry.value)
        }
    }

    /* Return the overall column index of this position in the global layout */
    private fun getGlobalColumnOfPosition(position: Int): Int {
        return position % numberOfDivisions
    }

    /* Return the overall row index of this position in the global layout */
    private fun getGlobalRowOfPosition(position: Int): Int {
        return position / numberOfDivisions
    }

    private fun updateVisibleChildren() {
        visibleChildren.clear()
        val viewport: Rect
        if (orientation == RecyclerView.VERTICAL) {
            viewport = Rect(0, verticalOffset, availableWidth, verticalOffset + availableHeight)
        } else {
            viewport = Rect(horizontalOffset, 0, horizontalOffset + availableWidth, availableHeight)
        }
        for (i in 0..rectList.size - 1) {
            val viewRect = rectList[i]
            if (viewport.intersects(viewRect)) {
                visibleChildren.add(i)
            }
        }
    }

    private fun getChildRectForItemIndex(itemIndex: Int): Rect? {
        return rectList[itemIndex]
    }

    private fun getChildRectForChildIndex(childIndex: Int): Rect? {
        val view = getChildAt(childIndex) ?: return null
        val itemIndex = viewToItemIndexMap[view] ?: return null
        return getChildRectForItemIndex(itemIndex)
    }

    // endregion

    // region Scroll Management

    /*
     * Use this method to tell the RecyclerView if scrolling is even possible
     * in the horizontal direction.
     */
    override fun canScrollHorizontally(): Boolean {
        return orientation == RecyclerView.HORIZONTAL && totalChildrenWidth > availableWidth
    }

    /*
     * Use this method to tell the RecyclerView if scrolling is even possible
     * in the vertical direction.
     */
    override fun canScrollVertically(): Boolean {
        return orientation == RecyclerView.VERTICAL && totalChildrenHeight > availableHeight
    }

    /*
     * This method describes how far RecyclerView thinks the contents should scroll horizontally.
     * You are responsible for verifying edge boundaries, and determining if this scroll
     * event somehow requires that new views be added or old views get recycled.
     */
    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (itemCount <= 0) return 0
        if (totalChildrenWidth < availableWidth) return 0

        val delta: Int
        if (dx > 0) { // Scroll viewport left
            if (horizontalOffset + availableWidth + dx > totalChildrenWidth) {
                delta = totalChildrenWidth - horizontalOffset - availableWidth
            } else {
                delta = dx
            }
        } else { // Scroll viewport up
            if (horizontalOffset + dx <= 0) {
                delta = -horizontalOffset
            } else {
                delta = dx
            }
        }

        offsetChildrenHorizontal(-delta)
        horizontalOffset += delta
        updateVisibleChildren()
        layoutChildren(recycler)
        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return delta
    }

    /*
     * This method describes how far RecyclerView thinks the contents should scroll vertically.
     * You are responsible for verifying edge boundaries, and determining if this scroll
     * event somehow requires that new views be added or old views get recycled.
     */
    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        if (itemCount <= 0) return 0
        if (totalChildrenHeight < availableHeight) return 0

        val delta: Int
        if (dy > 0) { // Scroll viewport down
            if (verticalOffset + availableHeight + dy > totalChildrenHeight) {
                delta = totalChildrenHeight - verticalOffset - availableHeight
            } else {
                delta = dy
            }
        } else { // Scroll viewport up
            if (verticalOffset + dy <= 0) {
                delta = -verticalOffset
            } else {
                delta = dy
            }
        }

        offsetChildrenVertical(-delta)
        verticalOffset += delta
        updateVisibleChildren()
        layoutChildren(recycler)
        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return delta
    }

    /*
     * You must override this method if you would like to support external calls
     * to shift the view to a given adapter position. In our implementation, this
     * is the same as doing a fresh layout with the given position as the top-left
     * (or first visible), so we simply set that value and trigger onLayoutChildren()
     */
    override fun scrollToPosition(position: Int) {
        if (position < 0 || position >= rectList.size) {
            Log.e(TAG, "Cannot scroll to $position, item count is ${rectList.size - 1}")
            return
        }
        val target = rectList[position]

        if (orientation == RecyclerView.VERTICAL) {
            val screenBottom = verticalOffset + availableHeight
            if (target.top >= verticalOffset && target.bottom <= screenBottom) {
                Log.d("scrollToPosition", "${target.top} don't move")
                return
            }
            val targetHeight = target.bottom - target.top
            if (target.top < verticalOffset || targetHeight > availableHeight) {
                verticalOffset = target.top
            } else {
                verticalOffset = target.bottom - availableHeight
            }
        } else {
            val screenLeft = horizontalOffset + availableWidth
            if (target.right >= horizontalOffset && target.right <= screenLeft) {
                Log.d("scrollToPosition", "${target.right} don't move")
                return
            }
            val targetWidth = target.right - target.left
            if (target.left < horizontalOffset || targetWidth > availableWidth) {
                horizontalOffset = target.left
            } else {
                horizontalOffset = target.right - availableWidth
            }
        }

        //Toss all existing views away
        removeAllViews()
        //Trigger a new view layout
        requestLayout()
    }

    /*
     * You must override this method if you would like to support external calls
     * to animate a change to a new adapter position. The framework provides a
     * helper scroller implementation (LinearSmoothScroller), which we leverage
     * to do the animation calculations.
     */
    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        recyclerView ?: return
        if (position < 0 || position >= rectList.size) {
            Log.e(TAG, "Cannot scroll to $position, item count is ${rectList.size}")
            return
        }

        /*
         * LinearSmoothScroller's default behavior is to scroll the contents until
         * the child is fully visible. It will snap to the top-left or bottom-right
         * of the parent depending on whether the direction of travel was positive
         * or negative.
         */
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            /*
             * LinearSmoothScroller, at a minimum, just need to know the vector
             * (x/y distance) to travel in order to get from the current positioning
             * to the target.
             */
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
                val target = rectList[targetPosition]
                if (orientation == RecyclerView.VERTICAL) return PointF(0F, target.top.toFloat())
                return PointF(target.right.toFloat(), 0F)
            }
        }
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    // endregion

    // region Misc Properties

    private val availableWidth: Int
        get() = width - paddingLeft - paddingRight

    private val availableHeight: Int
        get() = height - paddingTop - paddingBottom

    private val columnWidth: Int
        get() {
            if (orientation == RecyclerView.VERTICAL) return availableWidth / numberOfDivisions
            return rowHeight
        }

    private val rowHeight: Int
        get() {
            if (orientation == RecyclerView.HORIZONTAL) return availableHeight / numberOfDivisions
            return columnWidth
        }

    private val isFirstLayoutPass: Boolean
        get() = childCount <= 0

    private val totalChildrenHeight: Int
        get() = rectList.fold(0) { height, rect -> Math.max(height, rect.bottom) }

    private val totalChildrenWidth: Int
        get() = rectList.fold(0) { width, rect -> Math.max(width, rect.right) }

    // endregion

    // region LayoutState Utility Class

    private fun arrayOfZeros(size: Int): Array<Int> {
        return Array(size) {
            0
        }
    }

    private abstract class LayoutState constructor(
            val x: Int,
            val y: Int
    ) {
        protected fun Iterable<Int>.minIndex(): Int {
            return zip(0..count() - 1).fold(Pair(Int.MAX_VALUE, -1)) { p, v ->
                if (p.first > v.first) Pair(v.first, v.second) else p
            }.second
        }

        protected fun Iterable<Int>.maxIndex(): Int {
            return zip(0..count() - 1).fold(Pair(Int.MIN_VALUE, -1)) { p, v ->
                if (p.first < v.first) Pair(v.first, v.second) else p
            }.second
        }

        abstract fun pivot(width: Int, height: Int): LayoutState?
    }

    private inner class VerticalLayoutState private constructor(
            x: Int,
            y: Int,
            private var columnLimits: Array<Int>

    ) : LayoutState(x, y) {
        constructor() : this(0, 0, arrayOfZeros(numberOfDivisions))

        private fun updateColumnLimits(column: Int, width: Int, height: Int) {
            val maxRow = columnLimits.slice(IntRange(column, column + width - 1)).max() ?: 0
            for (i in column..column + width - 1) {
                columnLimits[i] = maxRow + height
            }
        }

        private fun hasSpace(model: List<Int>, col: Int, row: Int, width: Int): Boolean {
            for ((remaining, i) in (col..col + width - 1).withIndex()) {
                if (i + width - remaining > model.size) return false
                if (model[i] > row) return false
            }
            return true
        }

        private fun findGapPosition(width: Int): Pair<Int, Int>? {
            val model = columnLimits.toList()
            val minCol = model.minIndex()
            val maxCol = model.maxIndex()
            val minRow = model[minCol]
            val maxRow = model[maxCol]
            for (row in minRow..maxRow) {
                val firstCol = if (row == minRow) minCol else 0
                (firstCol..model.size - 1)
                        .filter { hasSpace(model, it, row, width) }
                        .forEach { return Pair(it, row) }
            }
            return null
        }

        override fun pivot(width: Int, height: Int): VerticalLayoutState? {
            val w = width / columnWidth
            val h = height / rowHeight
            if (w > numberOfDivisions) {
                return null
            }
            val (pX, pY) = findGapPosition(w) ?: return null
            updateColumnLimits(pX, w, h)
            return VerticalLayoutState(pX * columnWidth, pY * rowHeight, columnLimits)
        }

        override fun toString(): String {
            return "($x, $y, ${columnLimits.forEachIndexed { position, value ->
                return ("($position, $value)")
            }})"
        }
    }

    private inner class HorizontalLayoutState private constructor(
            x: Int,
            y: Int,
            private var rowLimits: Array<Int>

    ) : LayoutState(x, y) {
        constructor() : this(0, 0, arrayOfZeros(numberOfDivisions))

        private fun updateColumnLimits(row: Int, width: Int, height: Int) {
            val maxCol = rowLimits.slice(IntRange(row, row + height - 1)).max() ?: 0
            for (i in row..row + height - 1) {
                rowLimits[i] = maxCol + width
            }
        }

        private fun hasSpace(model: List<Int>, col: Int, row: Int, height: Int): Boolean {
            for ((remaining, i) in (row..row + height - 1).withIndex()) {
                if (i + height - remaining > model.size) return false
                if (model[i] > col) return false
            }
            return true
        }

        private fun findGapPosition(height: Int): Pair<Int, Int>? {
            val model = rowLimits.toList()
            val minRow = model.minIndex()
            val maxRow = model.maxIndex()
            val minCol = model[minRow]
            val maxCol = model[maxRow]
            for (col in minCol..maxCol) {
                val firstRow = if (col == minCol) minRow else 0
                (firstRow..model.size - 1)
                        .filter { hasSpace(model, col, it, height) }
                        .forEach { return Pair(col, it) }
            }
            return null
        }

        override fun pivot(width: Int, height: Int): HorizontalLayoutState? {
            val w = width / columnWidth
            val h = height / rowHeight
            if (h > numberOfDivisions) {
                return null
            }
            val (pX, pY) = findGapPosition(h) ?: return null
            updateColumnLimits(pY, w, h)
            return HorizontalLayoutState(pX * columnWidth, pY * rowHeight, rowLimits)
        }

        override fun toString(): String {
            return "($x, $y, ${rowLimits.forEachIndexed { position, value ->
                return ("($position, $value)")
            }})"
        }
    }

    // endregion

    // region Rect Extensions

    fun Rect.intersects(other: Rect): Boolean {
        return intersects(other.left, other.top, other.right, other.bottom)
    }

    // endregion

    // region Decoration

    @Suppress("JoinDeclarationAndAssignment")
    inner class InsetDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val mInsets: Int
        private var topInset = 0
        private var bottomInset = 0
        private var leftInset = 0
        private var rightInset = 0
        private var onlyOnce = true

        init {
            mInsets = context.resources.getDimensionPixelSize(R.dimen.default_card_insets)
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
            //We can supply forced insets for each item view here in the Rect
            val index = viewToItemIndexMap[view] ?: return
            val rect = rectList[index]
            Log.d("Rect", "${rect.top}, ${rect.bottom}, ${rect.left}, ${rect.right}")

            if (orientation == RecyclerView.VERTICAL) {
                leftInset = if (rect.left <= 0) 0 else mInsets / 2
                topInset = if (rect.top <= 0) 0 else mInsets / 2
                rightInset = if (rect.right >= availableWidth - numberOfDivisions) 0 else mInsets / 2
                bottomInset = if (rect.bottom >= totalChildrenHeight) 0 else mInsets / 2
            } else {
                leftInset = if (rect.left <= 0) 0 else mInsets / 2
                topInset = if (rect.top <= 0) 0 else mInsets / 2
                rightInset = if (rect.right >= totalChildrenWidth) 0 else mInsets / 2
                bottomInset = if (rect.bottom > availableHeight - numberOfDivisions) 0 else mInsets / 2
            }

            outRect.set(leftInset, topInset, rightInset, bottomInset)
            val params = view.layoutParams
            params.width = params.width - leftInset - rightInset
            params.height = params.height - topInset - bottomInset
            view.layoutParams = params as RecyclerView.LayoutParams

            if (onlyOnce) {
                Handler().post {
                    parent.adapter.notifyItemChanged(index)
                    onlyOnce = false
                }
            }
        }
    }

    // endregion
}

fun <K, V> Map<K, V>?.findKeyByValue(value: V): K? {
    this ?: return null
    if (!this.containsValue(value)) return null

    this.forEach { entry ->
        if (entry.value == value) return entry.key
    }
    return null
}