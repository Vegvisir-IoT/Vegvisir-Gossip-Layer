package com.vegvisir.gossip;

import com.vegvisir.gossip.adapter.NetworkAdapter;
import com.vegvisir.network.datatype.proto.Payload;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class Gossip {

    /* Key is peer id and value the meta data for that peer */
    private Map<String, GossipConnection> connections;

    NetworkAdapter adapter;

    Random rnd = new Random(new Date().getTime() + this.hashCode());

    /**
     * Constructor or Gossip layer
     * @param adapter an adapter for the underlying network layer. This could be an adapter for android, TCP or etc.
     */
    public Gossip(NetworkAdapter adapter) {
        connections = new HashMap<>();
        this.adapter = adapter;
        this.adapter.onReceiveBlock(this::onNewPayload);
    }

    /**
     * [BLOCKING] Randomly choose a wake-up peer. This is a blocking method.
     * @return
     */
    public String randomPickAPeer() {
        List<String> view = adapter.getAvailableConnections(); // This call is blocking
        Collections.shuffle(view, rnd);
        String next;
        for (int i = 0; i < view.size(); i ++)
        {
            next = view.get(i);
            if (!connections.containsKey(next))
                connections.put(next, new GossipConnection(next));
            connections.get(next).setConnected();
            if (connections.get(next).isWakeup())
                return next;
        }
        return null;
    }

    /**
     * A view is a list of device ids that have been discovered by this node.
     * @return
     */
    public List<String> getNearbyView() {
        return adapter.getNearbyDevices();
    }

    /**
     * Send given payload to peer with id. Upper call from Blockchain
     * @param id
     * @param payload
     * @return true if remote side is still connected.
     */
    public boolean sendToPeer(String id, Payload payload) {
        boolean alive = adapter.sendBlock(id, payload);
        if (!alive)
            connections.get(id).disconnect();
        return alive;
    }

    /**
     * Upper call from blockchain.
     * Pass payload from @id to @handler, which takes payload as an argument.
     * @param id
     */
    public Payload receiveFromPeer(String id) throws InterruptedException {
        if (connections.containsKey(id)) {
            return connections.get(id).blockingGet();
        }
        return null;
    }

    /**
     * [NEW THREAD] This will dispatch a new thread to keep listening on new payloads.
     * The thread will be removed if remote side is disconnected.
     * @param id
     * @param handler
     */
    public void setHandlerForPeerMessage(String id, Consumer<Payload> handler) {
        new Thread(() -> {
            if (!connections.containsKey(id))
                return;
            while (true) {
                try {
                    handler.accept(connections.get(id).blockingGet());
                } catch (InterruptedException ex) {
                    if (!connections.get(id).isConnected())
                        return;
                }
            }
        }).start();
    }

    /**
     * Handler when a new payload arrived from peer with id.
     * @param id
     * @param payload
     */
    private void onNewPayload(String id, Payload payload) {
        connections.get(id).recvPayload(payload);
    }
}
