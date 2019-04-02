package com.vegvisir.gossip.adapter;

import com.vegvisir.network.datatype.proto.Payload;

import java.util.List;
import java.util.function.BiConsumer;

public interface NetworkAdapter {

    /**
     * Push given @payload to the sending queue for peer with @peerId
     * @param peerId a unique id for the peer node
     * @param payload the actual data to be sent
     */
    public void sendBlock(String peerId, Payload payload);

    /**
     * Broadcast given @payload to all peers
     * @param payload data to be sent
     */
    public void broadCast(Payload payload);

    /**
     * Register a handler to handle new arrived payload from other peers.
     * @param handler the handle which takes peer id as the first argument and payload as the second argument and return nothing.
     */
    public void onReceiveBlock(BiConsumer<String, Payload> handler);

    /**
     * @return a set of strings which represent the id of nearby devices.
     */
    public List<String> getNearbyDevices();
}

