const MAX_ID_LENGTH = 80;
const MAX_NAME_LENGTH = 32;

export default {
  async fetch(request, env) {
    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return new Response("Phrases relay is running\n", {
        headers: { "content-type": "text/plain; charset=utf-8" },
      });
    }

    const id = env.PHRASES_RELAY.idFromName("global");
    return env.PHRASES_RELAY.get(id).fetch(request);
  },
};

export class PhrasesRelay {
  constructor(state) {
    this.state = state;
    this.clients = new Set();
  }

  async fetch(request) {
    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];

    server.accept();
    this.clients.add(server);

    server.addEventListener("message", (event) => {
      let message;
      try {
        message = JSON.parse(event.data);
      } catch {
        return;
      }

      if (
        message?.type !== "phrase" ||
        typeof message.id !== "string" ||
        typeof message.name !== "string"
      ) {
        return;
      }

      const payload = JSON.stringify({
        type: "phrase",
        id: message.id.slice(0, MAX_ID_LENGTH),
        name: message.name.slice(0, MAX_NAME_LENGTH),
      });

      for (const peer of this.clients) {
        if (peer === server) continue;
        try {
          peer.send(payload);
        } catch {
          this.clients.delete(peer);
        }
      }
    });

    const remove = () => this.clients.delete(server);
    server.addEventListener("close", remove);
    server.addEventListener("error", remove);

    return new Response(null, { status: 101, webSocket: client });
  }
}
