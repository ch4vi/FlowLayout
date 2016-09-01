FlowLayout – Android Layout Manager
===================================

[ ![Download](https://api.bintray.com/packages/ch4vi/maven/flowlayout/images/download.svg) ](https://bintray.com/ch4vi/maven/flowlayout/_latestVersion)

A LayoutManager that must be used with RecyclerView inspired by [Flow Layout for iOS](https://developer.apple.com/library/ios/documentation/WindowsViews/Conceptual/CollectionViewPGforIOS/UsingtheFlowLayout/UsingtheFlowLayout.html).

The layout manager paces cells on a linear path and fits as many cells along
that line as it can. When the layout manager runs out of room on the current
line, it creates a new line and continues the layout process there.

## Compile ##
to-do

## Usage ##
First of all add a RecyclerView to your layout
```xml
    <android.support.v7.widget.RecyclerView
        android:id="@+id/recycler_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_below="@+id/button_center"
        android:background="@color/e"
        android:scrollbars="vertical" />
```

Then in your class initialize the RecyclerView, in this case with 3 columns,
vertical orientation and the first cell size will be 1x1, the second 2x2...
```kotlin
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
```

**Note** If we add a cell too big for example if we have 3 columns and 
we add a cell 4x1, this cell will be ommited in the drawing process.
    
Optionally you can add a custom space between the cells adding a dimen resource
named "default_card_insets" and setting an item decoration to your RecyclerView
like in the example
```xml
    <dimen name="default_card_insets">8dp</dimen>
```
```kotlin
    recyclerView.addItemDecoration(manager.InsetDecoration(this))
```


## Demo ##
<img src="https://raw.githubusercontent.com/ch4vi/FlowLayout/master/snapshots/vertical.gif" width="350" height="650">
<img src="https://raw.githubusercontent.com/ch4vi/FlowLayout/master/snapshots/horizontal.gif" width="350" height="650">

## Credits ##
   * [Chavi Anyó](https://github.com/ch4vi)
   * [Álvaro Vilanova](https://github.com/alvivi)

## License ##
FlowLayout is license under the Apache License.

