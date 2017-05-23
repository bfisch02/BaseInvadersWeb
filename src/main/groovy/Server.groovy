
import org.java_websocket.server.WebSocketServer
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake

def printerr = System.err.&println

config = new JsonSlurper().parse( args.length ? File(args[0]) : getClass().classLoader.getResource("settings.json") )

def inited = false



def sockServ = new WebSocketServer(new InetSocketAddress(config.port)) {
    public synchronized void onOpen(WebSocket webSocket, ClientHandshake handshake) {
        webSocket.send(new JsonBuilder(state).toString())
    }

    public void onMessage(WebSocket webSocket, String message) {
    }

    public void onError(WebSocket webSocket, Exception exception) {
    }

    public void onClose(WebSocket conn, int code, String reason, boolean remote ) {
    }

    public synchronized void sendAll(String message) {
        def cons = connections();
        synchronized (cons) {
            try {
                cons.findAll {c -> c.isOpen() && !c.isClosed() && !c.hasBufferedData()} .each {c ->
                    c.send(message)
                }
            } catch(Exception e) {
                printerr e
                c.close()
            }
        }
    }
}

def connectRun = {
    def state = [players:[], mines:[], bombs:[], wormholes:[], walls:[]]
    new Socket(config.biHost, config.biPort).withStreams { ins, outs ->
        ins.eachLine { line ->
            StringTokenizer st = new StringTokenizer(line)
            state.width = st.nextToken() as Integer
            state.height = st.nextToken() as Integer
            state.tick = st.nextToken() as Long
            state.totalTicks = st.nextToken() as Long
            state.tickDelay = st.nextToken() as Long
            state.bombRadius = st.nextToken() as Double

            state.players.clear()
            def loop = st.nextToken() as Integer
            for (def i = 0; i < loop; i++) {
                def p = [:]
                p.id = st.nextToken() as Integer
                p.name = st.nextToken()
                p.score = st.nextToken() as Integer
                p.px = st.nextToken() as Double
                p.py = st.nextToken() as Double
                p.vx = st.nextToken() as Double
                p.vy = st.nextToken() as Double
                state.players << p
            }

            state.mines.clear()
            loop = st.nextToken() as Integer
            for (def i = 0; i < loop; i++) {
                def m = [:]
                m.owner = st.nextToken()
                m.px = st.nextToken() as Double
                m.py = st.nextToken() as Double
                state.mines << m
            }

            state.bombs.clear()
            loop = st.nextToken() as Integer
            for (def i = 0; i < loop; i++) {
                def b = [:]
                b.delay = st.nextToken() as Integer
                b.life = st.nextToken() as Integer
                b.px = st.nextToken() as Double
                b.py = st.nextToken() as Double
                state.bombs << b
            }

            state.wormholes.clear()
            loop = st.nextToken() as Integer
            for (def i = 0; i < loop; i++) {
                def w = [:]
                w.px = st.nextToken() as Double
                w.py = st.nextToken() as Double
                w.radius = st.nextToken() as Double
                state.wormholes << w
            }

            state.walls.clear()
            loop = st.nextToken() as Integer
            for (def i = 0; i < loop; i++) {
                def w = [:]
                w.x1 = st.nextToken() as Double
                w.y1 = st.nextToken() as Double
                w.x2 = st.nextToken() as Double
                w.y2 = st.nextToken() as Double
                state.walls << w
            }

            //println ((( ( state.totalTicks - state.tick ) / ( 1000 / state.tickDelay ) ) as Integer) % 60)

            if (!inited) {
                sockServ.start();
                inited = true;
            }
            sockServ.sendAll(new JsonBuilder(state).toString())

            outs << "${state.tick}\n"
            outs.flush()
        }
    }
}

while(true) {

    try {
        connectRun()
    } catch(e) {
        e.printStackTrace();
    }

}





