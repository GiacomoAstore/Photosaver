package com.example.savemedia.xposed

import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            Window::class.java,
            "setFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    var flags = param.args[0] as Int
                    val mask = param.args[1] as Int
                    if (mask and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                        flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                        param.args[0] = flags
                    }
                }
            }
        )

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
