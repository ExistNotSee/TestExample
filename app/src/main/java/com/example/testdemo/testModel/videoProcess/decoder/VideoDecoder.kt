package com.example.testdemo.testModel.videoProcess.decoder

import android.view.SurfaceHolder
import com.example.testdemo.App
import com.example.testdemo.R
import com.example.testdemo.testModel.videoProcess.FileAttributes

/**
 * Created by Void on 2020/9/10 14:34
 *
 */
abstract class VideoDecoder {
    protected val filters = arrayOf(
            "lutyuv='u=128:v=128'",
            "hue='h=60:s=-3'",
            "lutrgb='r=0:g=0'",
            "edgedetect=low=0.1:high=0.4",
            "drawgrid=w=iw/3:h=ih/3:t=2:c=white@0.5",
            "colorbalance=bs=0.3",
            "drawbox=x=100:y=100:w=100:h=100:color=red@0.5'",
            "vflip",
            "unsharp"
    )

    //vflip is up and down, hflip is left and right
    protected val txtArray = arrayOf(
            App.context.getString(R.string.filter_sketch),
            App.context.getString(R.string.filter_distinct),
            App.context.getString(R.string.filter_warming),
            App.context.getString(R.string.filter_edge),
            App.context.getString(R.string.filter_division),
            App.context.getString(R.string.filter_equalize),
            App.context.getString(R.string.filter_rectangle),
            App.context.getString(R.string.filter_flip),
            App.context.getString(R.string.filter_sharpening)
    )

    //是否已经被释放
    protected var isRelease = false

    abstract fun setDisPlay(holder: SurfaceHolder?, fileInfo: FileAttributes)

    abstract fun setDataSource(path: String)
    abstract fun start()
    abstract fun pause()
    abstract fun seekTo(time: Int)
    abstract fun release()

    /**
     * 播放状态
     * @return true播放中
     */
    abstract fun isPlaying(): Boolean

    /**
     * 获取播放时间数据
     * @param type 1当前播放进度 2最大播放长度
     */
    abstract fun getPlayTimeIndex(type: Int): Int

}