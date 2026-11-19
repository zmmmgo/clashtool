const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8888;
const APK_PATH = path.join(__dirname, 'ClashTool_v1.0.apk');

const server = http.createServer((req, res) => {
    if (req.url === '/download' || req.url === '/ClashTool_v1.0.apk') {
        if (!fs.existsSync(APK_PATH)) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
            return res.end('APK 文件未找到');
        }
        const stat = fs.statSync(APK_PATH);
        res.writeHead(200, {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Length': stat.size,
            'Content-Disposition': 'attachment; filename="ClashTool_v1.0.apk"',
            'Cache-Control': 'no-cache'
        });
        const readStream = fs.createReadStream(APK_PATH);
        readStream.pipe(res);
        return;
    }

    // 主页展示移动端下载页
    const fileSizeMB = fs.existsSync(APK_PATH) ? (fs.statSync(APK_PATH).size / (1024 * 1024)).toFixed(1) : '13.4';
    const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>下载部落冲突提醒助手</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #12131A; color: #FFF; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
        .card { background: #1B1E29; border: 1px solid #FFD700; border-radius: 24px; padding: 32px 24px; max-width: 400px; width: 100%; text-align: center; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
        .icon { font-size: 54px; margin-bottom: 12px; }
        h1 { font-size: 22px; color: #FFD700; margin-bottom: 8px; }
        p { color: #A0A5B5; font-size: 14px; line-height: 1.6; margin-bottom: 24px; }
        .btn { display: block; width: 100%; background: linear-gradient(135deg, #FFD700, #FFA500); color: #000; text-decoration: none; padding: 16px; border-radius: 14px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(255,215,0,0.3); transition: transform 0.2s; }
        .btn:active { transform: scale(0.98); }
        .info { font-size: 12px; color: #6C7285; margin-top: 16px; }
    </style>
</head>
<body>
    <div class="card">
        <div class="icon">⚔️</div>
        <h1>部落冲突提醒助手</h1>
        <p>版本 v1.0 • 大小约 ${fileSizeMB} MB<br>多账号管理 • 建筑科技全自动提醒</p>
        <a href="/download" class="btn">📥 立即下载安装包 (APK)</a>
        <div class="info">下载完成后在手机通知栏或文件管理中点击安装</div>
    </div>
</body>
</html>`;

    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running at http://0.0.0.0:${PORT}`);
});
