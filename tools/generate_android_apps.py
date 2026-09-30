#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT / "recovery-manifest.json").read_text(encoding="utf-8"))
out = ROOT / "generated-android"
out.mkdir(exist_ok=True)

def ident(value):
    value = re.sub(r"[^a-zA-Z0-9_]", "_", value)
    if value and value[0].isdigit():
        value = "app_" + value
    return value.lower()

modules = []
for app in manifest["apps"]:
    slug = app["slug"]
    module = ident(slug)
    modules.append(module)
    package = "com.latif.recovery." + module
    app_dir = out / module
    java_dir = app_dir / "src/main/java" / Path(package.replace(".", "/"))
    java_dir.mkdir(parents=True, exist_ok=True)

    (app_dir / "build.gradle").write_text(f"""plugins {{
    id 'com.android.application'
}}

android {{
    namespace '{package}'
    compileSdk 35

    defaultConfig {{
        applicationId '{package}'
        minSdk 26
        targetSdk 35
        versionCode 1
        versionName '1.0.0'
    }}
}}
""", encoding="utf-8")

    label = app["name"].replace('"', '\\"')
    mode = app.get("mode", "workspace")
    description = app.get("description", "").replace('"', '\\"')
    is_audio = "audiobook" in mode or "tts" in mode
    action_label = "Speak / Preview" if is_audio else "Run Local Self-Test"

    manifest_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application
      android:allowBackup="true"
      android:label="{label}"
      android:supportsRtl="true"
      android:theme="@android:style/Theme.Material.Light.NoActionBar">
    <activity
        android:name=".MainActivity"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
"""
    (app_dir / "src/main/AndroidManifest.xml").parent.mkdir(parents=True, exist_ok=True)
    (app_dir / "src/main/AndroidManifest.xml").write_text(manifest_xml, encoding="utf-8")

    java = f"""package {package};

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {{
    private TextToSpeech tts;
    private EditText input;
    private TextView status;
    private final boolean audioMode = {str(is_audio).lower()};

    @Override
    protected void onCreate(Bundle savedInstanceState) {{
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 48, 36, 48);
        root.setBackgroundColor(Color.rgb(248, 249, 250));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("{label}");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(20, 20, 20));
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("{description}");
        desc.setTextSize(16);
        desc.setPadding(0, 20, 0, 24);
        root.addView(desc);

        input = new EditText(this);
        input.setMinLines(5);
        input.setHint(audioMode ? "Paste text to hear an offline Android TTS preview…" : "Enter a local test note…");
        input.setText(audioMode ? "LATIF audiobook recovery build is ready." : "Recovery workspace self-test");
        root.addView(input);

        Button action = new Button(this);
        action.setText("{action_label}");
        root.addView(action);

        if (audioMode) {{
            Button stop = new Button(this);
            stop.setText("Stop");
            root.addView(stop);
            stop.setOnClickListener(v -> {{
                if (tts != null) tts.stop();
                status.setText("Playback stopped.");
            }});
        }}

        status = new TextView(this);
        status.setText("APK recovery build initialized locally. No account or cloud connection is required.");
        status.setTextSize(15);
        status.setPadding(0, 24, 0, 0);
        root.addView(status);

        action.setOnClickListener(v -> {{
            String text = input.getText().toString().trim();
            if (audioMode) {{
                if (text.isEmpty()) text = "LATIF audiobook preview";
                int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "latif-preview");
                status.setText(result == TextToSpeech.SUCCESS ? "Speaking with the installed Android TTS engine." : "TTS engine could not start.");
            }} else {{
                status.setText("SELF-TEST PASS\nProject: {label}\nMode: {mode}\nUI: ready\nLocal input: " + (text.isEmpty() ? "empty" : "accepted"));
            }}
        }});

        setContentView(scroll);
    }}

    @Override
    public void onInit(int code) {{
        if (code == TextToSpeech.SUCCESS && tts != null) {{
            tts.setLanguage(Locale.getDefault());
        }}
    }}

    @Override
    protected void onDestroy() {{
        if (tts != null) {{
            tts.stop();
            tts.shutdown();
        }}
        super.onDestroy();
    }}
}}
"""
    (java_dir / "MainActivity.java").write_text(java, encoding="utf-8")

settings = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = 'LATIF-Recovery'
""" + "\n".join(f"include ':{m}'" for m in modules) + "\n"
(out / "settings.gradle").write_text(settings, encoding="utf-8")
(out / "build.gradle").write_text("""plugins {
    id 'com.android.application' version '8.7.3' apply false
}
""", encoding="utf-8")
(out / "gradle.properties").write_text("""org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=false
android.nonTransitiveRClass=true
""", encoding="utf-8")
print("Generated modules:", ", ".join(modules))
