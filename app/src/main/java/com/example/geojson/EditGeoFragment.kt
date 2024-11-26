package com.example.geojson

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class EditGeoFragment : Fragment() {
    private lateinit var webView: WebView
    private lateinit var getContentLauncher: ActivityResultLauncher<String>
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_edit_geo, container, false)

        // 初始化文件选择器
        getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                mUploadMessage?.onReceiveValue(arrayOf(uri))
            } else {
                mUploadMessage?.onReceiveValue(null)
            }
            mUploadMessage = null
        }

        // 获取 WebView 控件
        webView= rootView.findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                mUploadMessage = filePathCallback
                getContentLauncher.launch("*/*")
                return true
            }
        }
        webView.webViewClient = WebViewClient()

        // Add JavascriptInterface to WebView to enable communication with Android
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun saveGeoJSON(geoJsonData: String) {
                saveToFile(geoJsonData)  // This method will be called when the "Download" button is clicked
            }
        }, "Android")

        // 加载网页（使用网页 URL）
        webView.loadUrl("file:///android_asset/editGeoJSon.html")

        return rootView
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
}