package com.xuhh.achievement

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.xuhh.achievement.databinding.ActivityMainBinding
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(createTrustAllSSLSocketFactory(), TrustAllCerts())
        .hostnameVerifier { _, _ -> true }
        .build()
    private val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置横屏
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 隐藏状态栏
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.statusBarColor = Color.TRANSPARENT

        // 设置BlurView
        setupBlurView()

        setupCharts()
        fetchData()
    }

    private fun setupBlurView() {
        val radius = 20f
        val decorView = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = decorView.background

        // 设置圆角背景
        binding.mainContainer.background = resources.getDrawable(R.drawable.blur_rounded_bg, theme)
        
        // 设置裁剪
        binding.mainContainer.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.mainContainer.clipToOutline = true

        binding.mainContainer.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
            .setBlurAutoUpdate(true)
    }

    private fun setupCharts() {
        // 设置左侧成长记录图表
        binding.growthChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // 设置图表边距
            setExtraOffsets(20f, 40f, 20f, 20f)
            
            // 设置图例样式
            legend.apply {
                form = Legend.LegendForm.CIRCLE
                textSize = 12f
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 10f  // 增加底部间距
            }
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = -45f
                setDrawGridLines(false)  // 不显示竖线
                yOffset = 10f  // 增加底部间距
            }

            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                gridLineWidth = 1f
                gridColor = Color.LTGRAY
                enableGridDashedLine(10f, 10f, 0f)  // 设置虚线
                textColor = Color.rgb(0, 188, 212)  // 青色
                setDrawAxisLine(false)  // 不显示轴线
                setDrawLabels(true)
                setLabelCount(5, true)
            }

            axisRight.apply {
                axisMinimum = 0f
                setDrawGridLines(true)  // 启用网格线
                gridLineWidth = 1f
                gridColor = Color.LTGRAY
                disableGridDashedLine()  // 使用实线
                textColor = Color.rgb(255, 152, 0)  // 橙色
                setDrawAxisLine(false)  // 不显示轴线
                setDrawLabels(true)
                setLabelCount(5, true)
            }

            // 设置图表边距
            setExtraOffsets(20f, 20f, 20f, 20f)
        }

        // 设置右侧思维能力分析图表
        binding.capabilityChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(false)  // 不显示轴线
                setDrawLabels(true)  // 显示标签
                textColor = Color.BLACK
                textSize = 12f
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(false)
                setDrawAxisLine(false)  // 不显示轴线
                setDrawLabels(false)  // 不显示标签
            }

            axisRight.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)  // 不显示轴线
                setDrawLabels(false)  // 不显示标签
            }
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            try {
                // 获取成长记录数据
                val growthResponse = withContext(Dispatchers.IO) {
                    fetchGrowthData()
                }
                updateGrowthChart(growthResponse)

                // 获取思维能力分析数据
                val capabilityResponse = withContext(Dispatchers.IO) {
                    fetchCapabilityData()
                }
                updateCapabilityChart(capabilityResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchGrowthData(): JSONObject {
        val request = Request.Builder()
            .url("https://test.shiqu.zhilehuo.com/math/growth/getAllInfo?type=0")
            .header("Cookie", "sid=8KkTk2PVpjCGeohbsWzy+3nbS14+A0302jlbFhzW4eY=")
            .build()

        val response = okHttpClient.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "{}")
    }

    private fun fetchCapabilityData(): JSONObject {
        val request = Request.Builder()
            .url("https://test.shiqu.zhilehuo.com/math/achieve/getLevelAchieve")
            .header("Cookie", "sid=8KkTk2PVpjCGeohbsWzy+3nbS14+A0302jlbFhzW4eY=")
            .build()

        val response = okHttpClient.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "{}")
    }

    private fun updateGrowthChart(response: JSONObject) {
        val data = response.optJSONObject("data")?.optJSONObject("growthCurve") ?: return
        val dates = data.optJSONArray("answerQuestionDates")
        val times = data.optJSONArray("answerTimes")
        val questions = data.optJSONArray("answerQuestionNums")

        val entries1 = ArrayList<Entry>()
        val entries2 = ArrayList<Entry>()

        var totalTime = 0
        for (i in 0 until dates.length()) {
            entries1.add(Entry(i.toFloat(), times.getInt(i).toFloat()))
            entries2.add(Entry(i.toFloat(), questions.getInt(i).toFloat()))
            totalTime += times.getInt(i)
        }

        val dataSet1 = LineDataSet(entries1, "每日学习时长(分钟)").apply {
            color = Color.rgb(0, 188, 212)  // 青色
            setCircleColor(Color.rgb(0, 188, 212))
            lineWidth = 2f
            circleRadius = 1f  // 设置最小圆点半径
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
            mode = LineDataSet.Mode.LINEAR  // 使用直线
            setDrawCircles(false)  // 不显示点
        }

        val dataSet2 = LineDataSet(entries2, "累计答题数(个)").apply {
            color = Color.rgb(255, 152, 0)  // 橙色
            setCircleColor(Color.rgb(255, 152, 0))
            lineWidth = 2f
            circleRadius = 1f  // 设置最小圆点半径
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.RIGHT
            mode = LineDataSet.Mode.LINEAR  // 使用直线
            setDrawCircles(false)  // 不显示点
        }

        binding.growthChart.data = LineData(dataSet1, dataSet2)
        binding.growthChart.xAxis.valueFormatter = IndexAxisValueFormatter(
            List(dates.length()) { i -> 
                val date = dates.getString(i)
                date.substring(5)  // 只显示月-日
            }
        )
        binding.growthChart.invalidate()

        // 更新底部统计信息
        val hours = totalTime / 60
        val minutes = totalTime % 60
        binding.totalTimeText.text = "太棒了，截至目前，你已累计学习${String.format("%02d", hours)}时${String.format("%02d", minutes)}分"
    }

    private fun updateCapabilityChart(response: JSONObject) {
        val data = response.optJSONObject("data") ?: return
        val capabilityList = data.optJSONArray("capabilityList") ?: return

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        for (i in 0 until capabilityList.length()) {
            val item = capabilityList.getJSONObject(i)
            val capability = item.optDouble("capability", 0.0)
            entries.add(BarEntry(i.toFloat(), capability.toFloat()))
            labels.add(item.optString("name", ""))
        }

        if (entries.isEmpty()) return

        val dataSet = BarDataSet(entries, "能力值").apply {
            color = Color.rgb(255, 152, 0) // 橙色
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f", value)
                }
            }
            setDrawValues(true)
            barBorderWidth = 0f
            barBorderColor = Color.TRANSPARENT
            setDrawIcons(false)
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.4f  // 增加柱子宽度
        }

        // 设置柱状图样式
        binding.capabilityChart.apply {
            // 设置图表边距
            setExtraOffsets(20f, 20f, 20f, 20f)  // 增加底部边距
            
            // 设置Y轴最大值
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(false)
                textColor = Color.BLACK
                textSize = 12f
                axisLineColor = Color.TRANSPARENT
            }
            
            // 设置自定义渲染器
            renderer = RoundedBarChartRenderer(
                this,
                animator,
                viewPortHandler,
                15f  // 设置圆角半径
            )
            
            this.data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            
            // 设置柱形样式
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            setDrawBorders(false)
            setDrawValueAboveBar(true)
            
            // 设置图表背景为白色
            setBackgroundColor(Color.WHITE)
            
            // 设置动画
            animateY(1000)
            
            // 设置图表样式
            description.isEnabled = false
            legend.isEnabled = false
            
            // 设置X轴样式
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.BLACK
                textSize = 12f
                axisLineColor = Color.TRANSPARENT
                yOffset = 10f  // 增加X轴标签的底部偏移
            }
            
            axisRight.isEnabled = false
            
            // 刷新图表
            invalidate()
        }

        // 更新底部统计信息
        val accuracy = data.optDouble("maxCapability", 0.0)
        val currentText = binding.totalTimeText.text.toString()
        binding.totalTimeText.text = "$currentText，题目总正确率为${String.format("%.1f", accuracy)}%，一起继续探索知识的宝库吧！"
    }

    private fun createTrustAllSSLSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(TrustAllCerts())
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }

    private class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    // 自定义柱状图渲染器
    private class RoundedBarChartRenderer(
        chart: BarDataProvider,
        animator: com.github.mikephil.charting.animation.ChartAnimator,
        viewPortHandler: ViewPortHandler,
        private val cornerRadius: Float = 20f // 设置圆角半径
    ) : BarChartRenderer(chart, animator, viewPortHandler) {

        override fun drawDataSet(c: Canvas, dataSet: com.github.mikephil.charting.interfaces.datasets.IBarDataSet, index: Int) {
            if (dataSet.entryCount == 0) return

            val trans = mChart.getTransformer(dataSet.axisDependency)
            mShadowPaint.color = dataSet.barShadowColor
            mBarBorderPaint.color = dataSet.barBorderColor
            mBarBorderPaint.strokeWidth = com.github.mikephil.charting.utils.Utils.convertDpToPixel(dataSet.barBorderWidth)
            val drawBorder = dataSet.barBorderWidth > 0.0f
            val phaseX = mAnimator.phaseX
            val phaseY = mAnimator.phaseY
            
            // 确保buffer数组已初始化
            if (mBarBuffers == null) {
                mBarBuffers = arrayOfNulls(mChart.barData.dataSetCount)
            }
            if (index >= mBarBuffers.size) return
            
            var buffer = mBarBuffers[index]
            if (buffer == null) {
                buffer = com.github.mikephil.charting.buffer.BarBuffer(dataSet.entryCount * 4, dataSet.entryCount, false)
                mBarBuffers[index] = buffer
            }
            
            buffer.setPhases(phaseX, phaseY)
            buffer.setDataSet(index)
            buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
            buffer.setBarWidth(mChart.barData.barWidth)
            buffer.feed(dataSet)
            
            // 确保buffer.buffer不为空
            if (buffer.buffer == null || buffer.buffer.isEmpty()) return
            
            trans.pointValuesToPixel(buffer.buffer)

            var j = 0
            while (j < buffer.size()) {
                if (j + 3 >= buffer.buffer.size) break
                
                if (mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                    if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) {
                        break
                    }

                    val left = buffer.buffer[j]
                    val top = buffer.buffer[j + 1]
                    val right = buffer.buffer[j + 2]
                    val bottom = buffer.buffer[j + 3]
                    
                    // 计算最小高度，确保即使值为0也能显示圆角
                    val minHeight = cornerRadius * 2
                    val actualHeight = bottom - top
                    val adjustedTop = if (actualHeight < minHeight) {
                        bottom - minHeight
                    } else {
                        top
                    }

                    // 计算柱子的宽度
                    val barWidth = right - left

                    // 绘制背景（浅橘色，表示100%）
                    val bgPaint = Paint(mRenderPaint).apply {
                        color = Color.argb(30, 255, 152, 0) // 浅橘色，30%透明度
                    }
                    
                    // 计算背景的高度（100%）
                    val bgTop = mChart.getTransformer(dataSet.axisDependency)
                        .getPixelForValues(0f, 100f).y.toFloat()  // 转换为Float
                    
                    // 绘制背景矩形
                    val bgRectF = RectF(left, bgTop + barWidth/2, right, bottom)
                    c.drawRect(bgRectF, bgPaint)
                    
                    // 绘制背景半圆
                    val bgCircleRect = RectF(
                        left,
                        bgTop,
                        right,
                        bgTop + barWidth
                    )
                    c.drawArc(bgCircleRect, 180f, 180f, true, bgPaint)

                    // 绘制主体柱子
                    mRenderPaint.color = if (dataSet.colors.size > 1) {
                        dataSet.getColor(j / 4)
                    } else {
                        dataSet.color
                    }

                    // 绘制主体矩形（从半圆底部开始）
                    val rectF = RectF(left, adjustedTop + barWidth/2, right, bottom)
                    c.drawRect(rectF, mRenderPaint)
                    
                    // 绘制顶部半圆
                    val circleRect = RectF(
                        left,
                        adjustedTop,
                        right,
                        adjustedTop + barWidth
                    )
                    c.drawArc(circleRect, 180f, 180f, true, mRenderPaint)
                    
                    if (drawBorder) {
                        c.drawRect(rectF, mBarBorderPaint)
                        c.drawArc(circleRect, 180f, 180f, true, mBarBorderPaint)
                    }
                }
                j += 4
            }
        }

        override fun drawValues(c: Canvas) {
            // 如果数据为空，直接返回
            if (mChart.barData == null || mChart.barData.dataSets.isEmpty()) return
            
            val dataSets = mChart.barData.dataSets
            for (i in dataSets.indices) {
                val dataSet = dataSets[i]
                if (!dataSet.isDrawValuesEnabled || dataSet.entryCount == 0) continue
                
                // 确保buffer数组已初始化
                if (mBarBuffers == null) {
                    mBarBuffers = arrayOfNulls(mChart.barData.dataSetCount)
                }
                if (i >= mBarBuffers.size) continue
                
                var buffer = mBarBuffers[i]
                if (buffer == null) {
                    buffer = com.github.mikephil.charting.buffer.BarBuffer(dataSet.entryCount * 4, dataSet.entryCount, false)
                    mBarBuffers[i] = buffer
                }
                
                if (buffer.buffer == null || buffer.buffer.isEmpty()) continue
                
                val phaseX = mAnimator.phaseX
                val phaseY = mAnimator.phaseY
                
                buffer.setPhases(phaseX, phaseY)
                buffer.setDataSet(i)
                buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
                buffer.setBarWidth(mChart.barData.barWidth)
                buffer.feed(dataSet)
                
                val trans = mChart.getTransformer(dataSet.axisDependency)
                trans.pointValuesToPixel(buffer.buffer)
                
                val valueTextSize = dataSet.valueTextSize
                val valueTextColor = dataSet.valueTextColor
                
                var j = 0
                while (j < buffer.size()) {
                    if (j + 3 >= buffer.buffer.size) break
                    
                    val x = (buffer.buffer[j] + buffer.buffer[j + 2]) / 2f
                    val y = buffer.buffer[j + 1]
                    
                    if (!mViewPortHandler.isInBoundsRight(x)) break
                    if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y)) {
                        j += 4
                        continue
                    }
                    
                    val entry = dataSet.getEntryForIndex(j / 4)
                    val formattedValue = dataSet.valueFormatter.getFormattedValue(entry.y, entry, i, mViewPortHandler)
                    
                    c.drawText(
                        formattedValue,
                        x,
                        y - valueTextSize,
                        mValuePaint.apply {
                            textSize = valueTextSize
                            color = valueTextColor
                        }
                    )
                    
                    j += 4
                }
            }
        }
    }
}