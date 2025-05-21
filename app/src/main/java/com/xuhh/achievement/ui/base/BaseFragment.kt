package com.xuhh.achievement.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * 基本的Fragment
 * 完成一些绑定等初始化操作，简化使用代码
 */
abstract class BaseFragment<T: ViewBinding>: Fragment() {
    private lateinit var _binding:T
    val mBinding:T
        get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = initBinding()
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }
    abstract fun initBinding(): T
    open fun initView() {}
}