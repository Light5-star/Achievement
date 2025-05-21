package com.xuhh.achievement

import com.xuhh.achievement.databinding.ActivityMainBinding
import com.xuhh.achievement.ui.base.BaseActivity

class MainActivity : BaseActivity<ActivityMainBinding>(){
    override fun initBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
}