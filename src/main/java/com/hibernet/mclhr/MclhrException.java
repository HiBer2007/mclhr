package com.hibernet.mclhr;

/** mclhr 的统一异常：库缺失、校验失败、清单格式错误。 */
public class MclhrException extends Exception {
    public MclhrException(String message) {
        super(message);
    }

    public MclhrException(String message, Throwable cause) {
        super(message, cause);
    }
}
