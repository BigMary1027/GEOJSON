package com.example.geojson

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter

class DxfToGeoFragment : Fragment() {
    private lateinit var webView: WebView
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null

    // 使用 getContentLauncher 启动文件选择器
    private val getContentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // 将选中的文件 URI 传递回 WebView
                mUploadMessage?.onReceiveValue(arrayOf(it))
            } ?: mUploadMessage?.onReceiveValue(null) // 如果没有选择文件，返回 null
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_dxf_to_geo, container, false)

        // 获取 WebView 控件
        webView = view.findViewById(R.id.webView)

        // 启用 JavaScript
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.domStorageEnabled = true

        // Handle JavaScript alerts (optional)
        webView.webChromeClient = object : WebChromeClient() {
            // 处理文件选择
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                // 保存回调
                mUploadMessage = filePathCallback

                // 启动文件选择器（可以指定 MIME 类型，例如 "image/*"、"*/*" 等）
                getContentLauncher.launch("*/*")  // 这里 "*" 表示允许选择任何类型的文件
                return true
            }
        }

        // Add JavascriptInterface to WebView to enable communication with Android
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun saveGeoJSON(geoJsonData: String) {
                saveToFile(geoJsonData)  // This method will be called when the "Download" button is clicked
            }
        }, "Android")

        // 加载网页（使用网页 URL）
        webView.loadUrl("file:///android_asset/dxfToGeoJson.html")


        requestStoragePermission()

        return view
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // 释放 WebView 资源，避免内存泄漏
        webView.destroy()
    }

    // Method to save GeoJSON data to file
    private fun saveToFile(geoJsonData: String) {
        val context = requireContext()
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Save GeoJson File")
        builder.setMessage("Enter the file name:")

        val input = EditText(context)
        input.hint="example.geojson"
        builder.setView(input)

        builder.setPositiveButton("Save"){ dialog,_->
            var fileName = input.text.toString().trim()
            if (fileName.isNotEmpty()){
                if (!fileName.endsWith(".geojson")){
                    fileName += ".geojson"
                }
                writeFileToStorage(geoJsonData,fileName)
                dialog.dismiss()
            }else{
                Toast.makeText(context,"Please enter a valid file name ending with .geojson",Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel"){ dialog,_->
            dialog.cancel()
        }
        builder.show()
    }

    // Method to write GeoJSON data to local storage
    private fun writeFileToStorage(geoJsonData: String,fileName: String) {
        val context = requireContext().applicationContext
        val dir = File(context.getExternalFilesDir(null), "GeoJSONFiles")
        if (!dir.exists()) {
            dir.mkdirs() // Create directory if it doesn't exist
        }

        val file = File(dir, fileName)
        try {
            FileOutputStream(file).use { fos ->
                fos.write(geoJsonData.toByteArray()) // Write GeoJSON data to file
            }
            Toast.makeText(requireContext(), "GeoJSON file saved to: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving GeoJSON file", Toast.LENGTH_SHORT).show()
        }
    }

    //请求权限
    private fun requestStoragePermission() {
        //申请定位权限
        val permissionList: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!permissionList.isEmpty()) {
            val permission = permissionList.toTypedArray<String>()
            ActivityCompat.requestPermissions(requireActivity(), permission, 1)
        }
    }
}