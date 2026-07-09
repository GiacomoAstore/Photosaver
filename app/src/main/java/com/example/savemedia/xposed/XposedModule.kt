package com.example.savemedia.xposed

import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Disable FLAG_SECURE for Windows
        XposedHelpers.findAndHookMethod(
            Window::class.java,
            "setFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val mask = param.args[1] as Int
                    if (mask and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                        var flags = param.args[0] as Int
                        flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                        param.args[0] = flags
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            Window::class.java,
            "setAttributes",
            WindowManager.LayoutParams::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val lp = param.args[0] as WindowManager.LayoutParams
                    if (lp.flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                    }
                }
            }
        )

        // Disable setSecure for SurfaceView
        try {
            XposedHelpers.findAndHookMethod(
                "android.view.SurfaceView",
                lpparam.classLoader,
                "setSecure",
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = false
                    }
                }
            )
        } catch (e: Throwable) {}

        // Hook LayoutParams setFlags
        XposedHelpers.findAndHookMethod(
            WindowManager.LayoutParams::class.java,
            "setFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val mask = param.args[1] as Int
                    if (mask and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                        var flags = param.args[0] as Int
                        flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                        param.args[0] = flags
                    }
                }
            }
        )

        // Force disable in Window constructor or similar
        try {
            XposedHelpers.findAndHookConstructor(
                WindowManager.LayoutParams::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        var flags = param.args[4] as Int
                        if (flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                            flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            param.args[4] = flags
                        }
                    }
                }
            )
        } catch (e: Throwable) {}

        XposedHelpers.findAndHookMethod(
            Window::class.java,
            "addFlags",
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    var flags = param.args[0] as Int
                    if (flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                        flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                        param.args[0] = flags
                    }
                }
            }
        )
    }
}
