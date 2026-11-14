import http from "node:http";
import { WebSocketServer } from "ws";

const port = Number(process.env.PORT || 10000);
const server = http.createServer((_request, response) => {
  response.writeHead(200, { "content-type": "text/plain; charset=utf-8" });
  response.end("Phrases relay is running\n");
});

const clients = new Set();
const wss = new WebSocketServer({ server });

function leave(socket) {
  clients.delete(socket);
}

wss.on("connection", (socket) => {
  clients.add(socket);
  socket.on("message", (raw) => {
    let message;
    try {
      message = JSON.parse(raw.toString());
    } catch {
      return;
    }

    if (message.type === "phrase" && typeof message.id === "string") {
      const payload = JSON.stringify({ type: "phrase", id: message.id.slice(0, 80) });
      for (const peer of clients) {
        if (peer !== socket && peer.readyState === peer.OPEN) peer.send(payload);
      }
    }
  });
  socket.on("close", () => leave(socket));
  socket.on("error", () => leave(socket));
});

server.listen(port, "0.0.0.0", () => {
  console.log(`Phrases relay listening on ${port}`);
});
