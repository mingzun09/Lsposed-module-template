package com.example.module;

import android.content.res.Configuration;
import android.content.res.Resources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import java.security.cert.Certificate;

public class PlayIntegrityBypass implements IXposedHookLoadPackage {

    private static final String GMS_PACKAGE = "com.google.android.gms";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        // --- 1. Play Integrity Bypass Logic for GMS ---
        if (lpparam.packageName.equals(GMS_PACKAGE)) {
            XposedBridge.log("PlayIntegrityBypass module loaded for GMS: " + lpparam.packageName);
            try {
                // 2. Mock spoofing package signature
                spoofPackageSignature(lpparam);

                // 3. Intercept DroidGuard's Attestation request logic
                XposedHelpers.findAndHookMethod(
                    "com.google.android.gms.droidguard.DroidGuard",
                    lpparam.classLoader,
                    "generateAttestationMetadata",
                    byte[].class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            // Step A: Get hardware attestation certificate chain via WebView/WebAuthn
                            Certificate[] hardwareCertChain = generateHardwareAttestationViaWebView();

                            // Step B: Extract original DroidGuard payload
                            byte[] originalPayload = (byte[]) param.args[0];

                            // Step C: Inject obtained WebView certificate chain into payload as crucial metadata
                            byte[] spoofedPayload = injectCertChainIntoMetadata(originalPayload, hardwareCertChain);

                            // Step D: Remove specific detection logic inside DroidGuard
                            removeDroidGuardDetectionFlags();

                            // Return spoofed Strong Integrity payload to Google servers
                            return spoofedPayload;
                        }
                    }
                );
                XposedBridge.log("Successfully hooked DroidGuard in GMS.");
            } catch (Throwable t) {
                XposedBridge.log("Error hooking GMS for Play Integrity Bypass: " + t.getMessage());
            }
        }

        // --- 2. Existing Dark Mode Hook Logic for all other apps ---
        // 排除掉安卓系统自身的框架，防止全局变黑导致系统界面崩溃
        if (lpparam.packageName.equals("android") || lpparam.packageName.startsWith("com.android.")) {
            return;
        }

        XposedBridge.log("通用深色模块已成功注入应用: " + lpparam.packageName);

        try {
            // ---- 核心 Hook 1：强制修改 Resources 资源配置中的 uiMode ----
            XposedHelpers.findAndHookMethod(
                Resources.class,
                "getConfiguration",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Configuration config = (Configuration) param.getResult();
                        if (config != null) {
                            config.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
                            config.uiMode |= Configuration.UI_MODE_NIGHT_YES;
                            param.setResult(config);
                        }
                    }
                }
            );

            // ---- 核心 Hook 2：拦截 App 内部试图锁死浅色模式的行为 ----
            Class<?> appCompatDelegateClass = XposedHelpers.findClassIfExists(
                "androidx.appcompat.app.AppCompatDelegate",
                lpparam.classLoader
            );

            if (appCompatDelegateClass != null) {
                XposedHelpers.findAndHookMethod(
                    appCompatDelegateClass,
                    "setDefaultNightMode",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int mode = (int) param.args[0];
                            if (mode != 2) {
                                param.args[0] = 2; // 强行改为 MODE_NIGHT_YES
                            }
                        }
                    }
                );
            }
        } catch (Throwable t) {
            XposedBridge.log("深色 Hook 运行异常: " + t.getMessage());
        }
    }

    // --- Mock implementation of the logical steps ---

    private void spoofPackageSignature(LoadPackageParam lpparam) {
        XposedBridge.log("Mock: spoofPackageSignature called for " + lpparam.packageName);
    }

    private Certificate[] generateHardwareAttestationViaWebView() {
        XposedBridge.log("Mock: Generating hardware cert via Browser/WebView WebAuthn...");
        return new Certificate[0]; // Empty array to represent mock certs
    }

    private byte[] injectCertChainIntoMetadata(byte[] payload, Certificate[] certChain) {
        XposedBridge.log("Mock: Injecting WebAuthn cert chain into DroidGuard metadata...");
        return payload; // Return original payload to simulate spoofed payload
    }

    private void removeDroidGuardDetectionFlags() {
        XposedBridge.log("Mock: removeDroidGuardDetectionFlags called.");
    }
}
