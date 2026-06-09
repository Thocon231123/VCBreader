package com.vcbreader;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VCBAccessibilityService extends AccessibilityService implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private static final String[] VCB_PACKAGES = {
        "com.VCB",
        "vn.com.vietcombank.vcbdigibank",
        "com.vietcombank.digibank"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("vi", "VN"));
            tts.setPitch(1.3f);
            tts.setSpeechRate(0.9f);
            ttsReady = true;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return;

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!isVCBPackage(packageName)) return;

        // Lấy nội dung thông báo
        CharSequence text = null;
        if (event.getText() != null && !event.getText().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence cs : event.getText()) {
                sb.append(cs).append(" ");
            }
            text = sb.toString().trim();
        }

        if (text == null || text.length() == 0) return;

        String content = text.toString();
        String speech = parseVCB(content);

        if (speech != null && ttsReady) {
            tts.stop();
            tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "vcb_" + System.currentTimeMillis());
        }
    }

    private boolean isVCBPackage(String pkg) {
        if (pkg == null) return false;
        for (String p : VCB_PACKAGES) {
            if (p.equalsIgnoreCase(pkg)) return true;
        }
        String lower = pkg.toLowerCase();
        return lower.contains("vcb") || lower.contains("vietcombank");
    }

    private String parseVCB(String text) {
        if (!text.contains("VCB")) return null;

        // Parse biến động: +148,000 hoặc -148,000
        Pattern amountPattern = Pattern.compile("([+\\-][\\d,]+)\\s*VND");
        Matcher amountMatcher = amountPattern.matcher(text);
        if (!amountMatcher.find()) return null;

        String rawAmount = amountMatcher.group(1);
        boolean isCredit = rawAmount.startsWith("+");
        String cleanAmount = rawAmount.replaceAll("[+\\-,]", "");
        String formattedAmount = formatMoney(Long.parseLong(cleanAmount));

        // Parse tên người chuyển
        String senderName = "";
        Pattern senderPattern = Pattern.compile("Ref\\s+\\S+\\.([A-Z\\s]+)\\s+chuyen tien", Pattern.CASE_INSENSITIVE);
        Matcher senderMatcher = senderPattern.matcher(text);
        if (senderMatcher.find()) {
            senderName = toTitleCase(senderMatcher.group(1).trim());
        }

        // Lấy tên chủ nhân từ SharedPreferences
        String ownerName = getSharedPreferences("vcb_prefs", MODE_PRIVATE)
            .getString("owner_name", "ông chủ");

        String action = isCredit ? "vừa cộng" : "vừa trừ";
        String sender = !senderName.isEmpty() ? ". Người chuyển: " + senderName : "";

        return "Thưa " + ownerName + ", tài khoản Vietcombank " + action + " " + formattedAmount + sender;
    }

    private String formatMoney(long amount) {
        long ty = amount / 1_000_000_000L;
        long trieu = (amount % 1_000_000_000L) / 1_000_000L;
        long nghin = (amount % 1_000_000L) / 1_000L;
        long le = amount % 1_000L;

        StringBuilder sb = new StringBuilder();
        if (ty > 0) sb.append(ty).append(" tỷ ");
        if (trieu > 0) sb.append(trieu).append(" triệu ");
        if (nghin > 0) sb.append(nghin).append(" nghìn ");
        if (le > 0) sb.append(le).append(" ");
        sb.append("đồng");
        return sb.toString().trim();
    }

    private String toTitleCase(String str) {
        String[] words = str.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
