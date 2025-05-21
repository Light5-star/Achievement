package com.xuhh.achievement.ui.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * 自定义的iconfont的使用类
 * 可以直接设置text
 */
class UnicodeTextView(
    context: Context,
    attrs: AttributeSet? = null
): AppCompatTextView(context, attrs) {

    init {
        // 设置字体
        typeface = Typeface.createFromAsset(context.assets, "iconfont.ttf")
    }
}