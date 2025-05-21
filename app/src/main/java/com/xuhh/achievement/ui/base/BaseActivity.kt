package com.xuhh.achievement.ui.base

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.xuhh.achievement.R

/**
 * 基本的Activity
 * 完成一些绑定初始化操作，简化使用代码
 */
abstract class BaseActivity<T: ViewBinding>: AppCompatActivity() {
    private lateinit var _binding:T
    val mBinding:T
        get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = initBinding()
        setContentView(_binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
    }

    abstract fun initBinding(): T
    open fun initView(){}
}