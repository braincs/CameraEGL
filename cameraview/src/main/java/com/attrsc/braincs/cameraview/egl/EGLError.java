package com.attrsc.braincs.cameraview.egl;

/**
 * Created by shuai on 11/08/2018.
 */

public enum EGLError {
    OK(0, "ok"),
    ConfigErr(101, "config not support");

    int code;
    String msg;

    EGLError(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int value() {
        return code;
    }

    @Override
    public String toString() {
        return "{code = " + code + ", msg = " + msg + "}";
    }
}
