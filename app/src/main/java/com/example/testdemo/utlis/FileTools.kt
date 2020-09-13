package com.example.testdemo.utlis

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.CursorLoader
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.testdemo.App


/**
 * Created by Void on 2020/8/17 15:32
 *
 */
object FileTools {

    fun getPath(context: Context, uri: Uri): String? {
        if ("content" == uri.scheme) {
            val projection = arrayOf("_data")
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor == null) return null
                val columnIndex: Int = cursor.getColumnIndexOrThrow("_data")
                return if (cursor.moveToFirst()) {
                    cursor.getString(columnIndex)
                } else
                    null
            } catch (e: Exception) {
            } finally {
                cursor?.close()
            }
        } else if ("file" == uri.scheme) {
            return uri.path
        }
        return null
    }

    fun getFilePathByUri(uri: Uri): String? {
        // �� file:// ��ͷ��
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            return uri.path
        }
        // ��/storage��ͷ��Ҳֱ�ӷ���
        if (isOtherDocument(uri)) {
            return uri.path
        }
        // �汾���ݵĻ�ȡ��
        var path = getFilePathByUri_BELOWAPI11(uri)
        if (path != null) {
            KLog.d("getFilePathByUri_BELOWAPI11��ȡ����·��Ϊ��$path")
            return path
        }
        path = getFilePathByUri_API11to18(uri)
        if (path != null) {
            KLog.d("getFilePathByUri_API11to18��ȡ����·��Ϊ��$path")
            return path
        }
        path = getFilePathByUri_API19(uri)
        KLog.d("getFilePathByUri_API19��ȡ����·��Ϊ��$path")
        return path
    }

    private fun getFilePathByUri_BELOWAPI11(uri: Uri): String? {
        // �� content:// ��ͷ�ģ����� content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            var path: String? = null
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = App.context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex)
                    }
                }
                cursor.close()
            }
            return path
        }
        return null
    }

    private fun getFilePathByUri_API11to18(contentUri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var result: String? = null
        val cursorLoader = CursorLoader(App.context, contentUri, projection, null, null, null)
        val cursor: Cursor = cursorLoader.loadInBackground()
        if (cursor != null) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            result = cursor.getString(column_index)
            cursor.close()
        }
        return result
    }

    private fun getFilePathByUri_API19(uri: Uri): String? {
        // 4.4��֮��� ���� content:// ��ͷ�ģ����� content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT == uri.scheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(App.context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return if (split.size > 1) {
                            Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        } else {
                            Environment.getExternalStorageDirectory().toString() + "/"
                        }
                        // This is for checking SD Card
                    }
                } else if (isDownloadsDocument(uri)) {
                    //���������ṩ��ʱӦ���ж����ع������Ƿ񱻽���
                    val stateCode = App.context.packageManager.getApplicationEnabledSetting("com.android.providers.downloads")
                    if (stateCode != 0 && stateCode != 1) {
                        return null
                    }
                    var id = DocumentsContract.getDocumentId(uri)
                    // ����������RAW��ַ�����������ֱ�ӷ���!
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    if (id.contains(":")) {
                        val tmp = id.split(":".toRegex()).toTypedArray()
                        if (tmp.size > 1) {
                            id = tmp[1]
                        }
                    }
                    var contentUri = Uri.parse("content://downloads/public_downloads")
                    KLog.d("���Դ�ӡUri: $uri")
                    try {
                        contentUri = ContentUris.withAppendedId(contentUri, id.toLong())
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    var path = getDataColumn(contentUri, null, null)
                    if (path != null) return path
                    // ����ĳЩ��������µ��ļ�������!
                    val fileName = getFileNameByUri(uri)
                    if (fileName != null) {
                        path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                        return path
                    }
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    return getDataColumn(contentUri, selection, selectionArgs)
                }
            }
        }
        return null
    }

    private fun getFileNameByUri(uri: Uri): String? {
        var relativePath = getFileRelativePathByUri_API18(uri)
        if (relativePath == null) relativePath = ""
        val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME
        )
        App.context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return relativePath + cursor.getString(index)
            }
        }
        return null
    }

    private fun getFileRelativePathByUri_API18(uri: Uri): String? {
        val projection: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection = arrayOf(
                    MediaStore.MediaColumns.RELATIVE_PATH
            )
            App.context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                    return cursor.getString(index)
                }
            }
        }
        return null
    }

    private fun getDataColumn(uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)
        try {
            App.context.contentResolver.query(uri!!, projection, selection, selectionArgs, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val column_index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(column_index)
                }
            }
        } catch (iae: IllegalArgumentException) {
            iae.printStackTrace()
        }
        return null
    }

    private fun isOtherDocument(uri: Uri?): Boolean {
        // ��/storage��ͷ��Ҳֱ�ӷ���
        if (uri != null && uri.path != null) {
            val path = uri.path
            if (path!!.startsWith("/storage")) {
                return true
            }
            if (path.startsWith("/external_files")) {
                return true
            }
        }
        return false
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean ="com.android.externalstorage.documents" == uri.authority

    private fun isDownloadsDocument(uri: Uri): Boolean = "com.android.providers.downloads.documents" == uri.authority

    private fun isMediaDocument(uri: Uri): Boolean ="com.android.providers.media.documents" == uri.authority
}