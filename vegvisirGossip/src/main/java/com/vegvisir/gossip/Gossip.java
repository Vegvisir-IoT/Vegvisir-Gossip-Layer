package com.vegvisir.gossip;

import com.vegvisir.gossip.adapter.NetworkAdapter;
import com.vegvisir.network.datatype.proto.Payload;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
     * Randomly choose a wake-up peer.
     * @return
     */
    public String randomPickAPeer() {
        List<String> view = adapter.getNearbyDevices();
        String next;
        do {
            next = view.get(rnd.nextInt(view.size()));
            if (!connections.containsKey(next))
                connections.put(next, new GossipConnection(next));
        } while (!connections.get(next).isWakeup());
        return next;
    }

    /**
     * Send given payload to peer with id. Upper call from Blockchain
     * @param id
     * @param payload
     */
    public void sendToPeer(String id, Payload payload) {
        adapter.sendBlock(id, payload);
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
     * Handler when a new payload arrived from peer with id.
     * @param id
     * @param payload
     */
    private void onNewPayload(String id, Payload payload) {
        connections.get(id).recvPayload(payload);
    }
}
