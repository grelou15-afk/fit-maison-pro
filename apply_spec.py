"""Lit project/spec.json, project/index.html, project/icon.png et prépare le projet Android."""
import json, os, shutil, html

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
p = lambda *a: os.path.join(root, *a)
spec = json.load(open(p("project", "spec.json"), encoding="utf-8"))

assets = p("app", "src", "main", "assets"); os.makedirs(assets, exist_ok=True)
shutil.copy(p("project", "index.html"), os.path.join(assets, "index.html"))
shutil.copy(p("project", "spec.json"), os.path.join(assets, "spec.json"))

icon_dir = p("app", "src", "main", "res", "drawable-nodpi"); os.makedirs(icon_dir, exist_ok=True)
shutil.copy(p("project", "icon.png"), os.path.join(icon_dir, "ic_launcher.png"))

name = html.escape(" ".join(str(spec.get("name", "FIT MAISON PRO")).split()), quote=True)
with open(p("app", "app.properties"), "w", encoding="utf-8") as f:
    f.write("appId=%s\nappName=%s\nversionName=%s\nversionCode=%d\n" % (
        spec["package"], name, spec.get("versionName", "1.0"), int(spec.get("versionCode", 1))))

# Permissions : celles choisies dans le constructeur + celles dont le pont natif a besoin
need = {"INTERNET", "POST_NOTIFICATIONS", "SCHEDULE_EXACT_ALARM", "RECEIVE_BOOT_COMPLETED", "VIBRATE", "WAKE_LOCK"}
perms = sorted(set(spec.get("permissions", [])) | need)
xml = "\n    ".join('<uses-permission android:name="android.permission.%s" />' % x for x in perms)
mp = p("app", "src", "main", "AndroidManifest.xml")
s = open(mp, encoding="utf-8").read()
s = s.replace("<!--PERMISSIONS-->", xml)
open(mp, "w", encoding="utf-8").write(s)
print("OK :", spec["package"], spec.get("versionName"), spec.get("versionCode"), len(perms), "permissions")
