package com.example.geojson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // 初始加载 HomeFragment
        if (savedInstanceState == null) {
            loadFragment(DxfToGeoFragment())
        }

        // 设置底部导航栏点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(DxfToGeoFragment())
                    true
                }
                R.id.navigation_search -> {
                    loadFragment(EditGeoFragment())
                    true
                }
                else -> false
            }
        }
    }
    // 加载 Fragment
    private fun loadFragment(fragment: Fragment) {
        // 在 FragmentTransaction 中替换当前的 Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

}