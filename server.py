import http.server
import socketserver
import os
import mimetypes

PORT = 3000

CANDIDATE_PATHS = [
    "/app/applet/golpoghor-latest.apk",
    "/golpoghor-latest.apk",
    "/app/applet/app/build/outputs/apk/debug/app-debug.apk",
    "golpoghor-latest.apk",
    "app/build/outputs/apk/debug/app-debug.apk"
]

def get_apk_path():
    for p in CANDIDATE_PATHS:
        if os.path.exists(p) and os.path.getsize(p) > 1000000:
            return p
    return None

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="bn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>গল্পঘর - APK ডাউনলোড | GolpoGhor</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body {
            font-family: system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            background-color: #FAF7F0;
            color: #332314;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            background: #FFFFFF;
            border: 1px solid #E6DCCD;
            border-radius: 24px;
            max-width: 480px;
            width: 100%;
            padding: 36px 28px;
            box-shadow: 0 10px 30px rgba(139, 98, 72, 0.08);
            text-align: center;
        }
        .icon-circle {
            width: 88px;
            height: 88px;
            background: #F4ECD8;
            border: 2px solid #C4A77D;
            border-radius: 22px;
            margin: 0 auto 20px auto;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 42px;
            box-shadow: 0 4px 14px rgba(139, 98, 72, 0.15);
        }
        h1 {
            color: #8B6248;
            font-size: 28px;
            font-weight: 700;
            margin-bottom: 8px;
        }
        .subtitle {
            color: #796A56;
            font-size: 15px;
            margin-bottom: 24px;
        }
        .download-btn {
            display: block;
            background-color: #8B6248;
            color: #FFFFFF !important;
            text-decoration: none;
            font-size: 18px;
            font-weight: 600;
            padding: 16px 28px;
            border-radius: 16px;
            margin: 0 auto 16px auto;
            transition: all 0.2s ease;
            box-shadow: 0 6px 18px rgba(139, 98, 72, 0.35);
        }
        .download-btn:hover, .download-btn:active {
            background-color: #704E38;
            transform: translateY(-2px);
            box-shadow: 0 8px 22px rgba(139, 98, 72, 0.45);
        }
        .file-info {
            display: inline-block;
            background: #F4ECD8;
            color: #5D4037;
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 500;
            margin-bottom: 24px;
        }
        .steps {
            background: #FAF7F0;
            border-radius: 16px;
            padding: 20px;
            text-align: left;
            border: 1px solid #EFE8DA;
        }
        .steps h3 {
            font-size: 15px;
            color: #8B6248;
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .steps ol {
            padding-left: 20px;
            font-size: 14px;
            line-height: 1.6;
            color: #4A3E36;
        }
        .steps li {
            margin-bottom: 8px;
        }
        .footer {
            margin-top: 24px;
            font-size: 12px;
            color: #9E8F82;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon-circle">📖</div>
        <h1>গল্পঘর (GolpoGhor)</h1>
        <p class="subtitle">প্রিমিয়াম বাংলা গল্প পড়ার সম্পূর্ণ অ্যান্ড্রয়েড অ্যাপ</p>
        
        <div class="file-info">
            ভার্সন: v1.2.0 (আপডেটেড • Theme-Aware Dark Mode) • সাইজ: ~35 MB • ফরম্যাট: Android APK
        </div>

        <a href="/golpoghor-latest.apk" class="download-btn" download="GolpoGhor.apk">
            📥 নতুন APK সরাসরি ডাউনলোড করুন
        </a>

        <div class="steps">
            <h3>📱 ফোনে ইনস্টল/আপডেট করার নিয়ম:</h3>
            <ol>
                <li>উপরের <strong>"নতুন APK সরাসরি ডাউনলোড করুন"</strong> বাটনে ট্যাপ করুন।</li>
                <li>ডাউনলোড শেষ হলে নোটিফিকেশন বার থেকে অথবা আপনার ফোনের <strong>Downloads</strong> ফোল্ডার থেকে <code>GolpoGhor.apk</code> ফাইলটিতে ক্লিক করুন।</li>
                <li>যদি "Update" দেখায় তবে <strong>Update</strong> চাপুন। যদি আগে পুরোনো অ্যাপ ইন্সটল করা থাকে তবে এটি স্বয়ংক্রিয়ভাবে v1.2.0-এ আপডেট হয়ে যাবে।</li>
                <li>অ্যাপ ওপেন করে থিম সিলেক্টর বা সেটিংস থেকে <strong>"ডার্ক মোড (Dark Mode)"</strong> অন করে ৭টি থিমের নিজস্ব আকর্ষণীয় ডার্ক লুক উপভোগ করুন!</li>
            </ol>
        </div>

        <div class="footer">
            Google AI Studio • GolpoGhor Android Release v1.2.0
        </div>
    </div>
</body>
</html>
"""

class ApkDownloadHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/", "/index.html"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Cache-Control", "no-cache")
            body = HTML_TEMPLATE.encode("utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if self.path in ("/golpoghor-latest.apk", "/download", "/GolpoGhor.apk"):
            apk_file = get_apk_path()
            if not apk_file or not os.path.exists(apk_file):
                self.send_error(404, "APK not found. Please compile the app first.")
                return

            file_size = os.path.getsize(apk_file)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="GolpoGhor.apk"')
            self.send_header("Content-Length", str(file_size))
            self.end_headers()

            with open(apk_file, "rb") as f:
                while chunk := f.read(65536):
                    self.wfile.write(chunk)
            return

        super().do_GET()

class ReusableTCPServer(socketserver.TCPServer):
    allow_reuse_address = True

if __name__ == "__main__":
    with ReusableTCPServer(("0.0.0.0", PORT), ApkDownloadHandler) as httpd:
        print(f"Serving APK on port {PORT}...")
        httpd.serve_forever()
