const WebSocket = require("ws");

const WS_PORT = 9000;

const wss = new WebSocket.Server({ port: WS_PORT });
const clients = new Set();

wss.on("connection", (ws) => {
  clients.add(ws);
  console.log(`Client connected (${clients.size} total)`);

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
    console.log(`Client disconnected (${clients.size} total)`);
  });
});

console.log(`WebSocket relay server running on ws://localhost:${WS_PORT}`);
console.log(`\nFor development, run in another terminal:`);
console.log(`  ./gradlew :composeApp:jsBrowserDevelopmentRun`);
