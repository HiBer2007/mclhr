package com.hibernet.mclhr;

/** mclhr 工作进度回调（由宿主转发到启动画面/日志）。 */
@FunctionalInterface
public interface MclhrProgress {

    /** 无进度回调。 */
    MclhrProgress NONE = (message, percent) -> {
    };

    /**
     * @param message 当前步骤
     * @param percent 0–100
     */
    void onProgress(String message, int percent);
}
