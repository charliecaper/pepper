const http = require("http");
const fs = require("fs");
const path = require("path");
const os = require("os");
const { exec, spawn } = require("child_process");
const WebSocket = require("ws");

const HTTP_PORT = 2000;
const WS_PORT = 9000;

// Static file serving
const PROJECT_ROOT = path.join(__dirname, "..");
const STATIC_DIR = path.join(PROJECT_ROOT, "composeApp/build/dist/js/productionExecutable");

// Get local IP address
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === "IPv4" && !iface.internal) {
        return iface.address;
      }
    }
  }
  return "localhost";
}

// Generate QR code and open it
function generateQRCode(url) {
  const qrPath = path.join(__dirname, "even.png");
  exec(`qrencode -o "${qrPath}" "${url}"`, (err) => {
    if (err) {
      console.log(`QR code generation failed (install qrencode): ${err.message}`);
      return;
    }
    console.log(`QR code saved to ${qrPath}`);
    // Open the image (macOS)
    exec(`open "${qrPath}"`, (openErr) => {
      if (openErr) {
        console.log(`Could not open QR code: ${openErr.message}`);
      }
    });
  });
}

// Check if build exists, run gradle if not
function ensureBuild(callback) {
  const indexPath = path.join(STATIC_DIR, "index.html");
  if (fs.existsSync(indexPath)) {
    console.log("Production build found.");
    callback();
    return;
  }

  console.log("Production build not found. Building...");
  const gradlew = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
  const build = spawn(gradlew, [":composeApp:jsBrowserDistribution"], {
    cwd: PROJECT_ROOT,
    stdio: "inherit",
    shell: true,
  });

  build.on("close", (code) => {
    if (code === 0) {
      console.log("Build complete.");
      callback();
    } else {
      console.error(`Build failed with code ${code}`);
      process.exit(1);
    }
  });

  build.on("error", (err) => {
    console.error(`Build error: ${err.message}`);
    process.exit(1);
  });
}

const MIME_TYPES = {
  ".html": "text/html",
  ".js": "application/javascript",
  ".mjs": "application/javascript",
  ".css": "text/css",
  ".json": "application/json",
  ".wasm": "application/wasm",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
};

function startServer() {
  const httpServer = http.createServer((req, res) => {
    let filePath = path.join(STATIC_DIR, req.url === "/" ? "index.html" : req.url);

    // Security: prevent directory traversal
    if (!filePath.startsWith(STATIC_DIR)) {
      res.writeHead(403);
      res.end("Forbidden");
      return;
    }

    fs.readFile(filePath, (err, data) => {
      if (err) {
        if (err.code === "ENOENT") {
          res.writeHead(404);
          res.end("Not found");
        } else {
          res.writeHead(500);
          res.end("Server error");
        }
        return;
      }

      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || "application/octet-stream";
      res.writeHead(200, { "Content-Type": contentType });
      res.end(data);
    });
  });

  const localIP = getLocalIP();
  const appURL = `http://${localIP}:${HTTP_PORT}/`;

  httpServer.listen(HTTP_PORT, "0.0.0.0", () => {
    console.log(`HTTP server running on ${appURL}`);
    generateQRCode(appURL);
  });

  // WebSocket relay
  const wss = new WebSocket.Server({ port: WS_PORT });
  const clients = new Set();

  wss.on("connection", (ws) => {
    clients.add(ws);
    console.log(`WS client connected (${clients.size} total)`);

    ws.on("message", (data) => {
      const msg = data.toString();
      console.log("Relaying:", msg);
      for (const client of clients) {
        if (client !== ws && client.readyState === WebSocket.OPEN) {
          client.send(msg);
        }
      }
    });

    ws.on("close", () => {
      clients.delete(ws);
      console.log(`WS client disconnected (${clients.size} total)`);
    });
  });

  console.log(`WebSocket relay running on ws://0.0.0.0:${WS_PORT}`);
}

// Main: ensure build exists, then start server
ensureBuild(startServer);
