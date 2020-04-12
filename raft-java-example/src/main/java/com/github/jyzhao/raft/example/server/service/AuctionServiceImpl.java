package com.github.jyzhao.raft.example.server.service;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.Endpoint;
import com.github.jyzhao.raft.example.server.AuctionStateMachine;
import com.github.jyzhao.raft.example.server.StateMachineBidReturn;
import com.github.wenweihu86.raft.Peer;
import com.github.wenweihu86.raft.RaftNode;
import com.github.wenweihu86.raft.proto.RaftProto;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionServiceImpl implements AuctionService {
    private RaftNode raftNode;
    private AuctionStateMachine stateMachine;
    private int leaderId;
    private Lock leaderLock = new ReentrantLock();
    private RpcClient leaderRpcClient;

    public AuctionServiceImpl(RaftNode raftNode, AuctionStateMachine stateMachine) {
        this.raftNode = raftNode;
        this.stateMachine = stateMachine;
    }

    @Override
    public String createAuctionItem(String owner, String name, double minVal) {
        if (raftNode.getLeaderId() <= 0) {
            return "fail";
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            AuctionService auctionService = BrpcProxy.getProxy(leaderRpcClient, AuctionService.class);
            return auctionService.createAuctionItem(owner, name, minVal);
        } else {
            byte[] data = ("createAuction;"+owner+";"+name+";"+String.valueOf(minVal)).getBytes();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);

            if (success) return "success";
            else return "fail";
        }
    }

    @Override
    public String bid(String bidder, String biddedItem, double bidVal) {
        if (raftNode.getLeaderId() <= 0) {
            return "fail";
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            AuctionService auctionService = BrpcProxy.getProxy(leaderRpcClient, AuctionService.class);
            return auctionService.bid(bidder, biddedItem, bidVal);
        } else {
            StateMachineBidReturn ret = stateMachine.bid(biddedItem, bidVal);
            if (!ret.isSucc) {
                return ret.message;
            }
            byte[] data = ("bid;"+bidder+";"+biddedItem+";"+String.valueOf(bidVal)).getBytes();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);
            if (success) return "success";
            else return "fail on consensus";
        }
    }

    private void onLeaderChangeEvent() {
        if (raftNode.getLeaderId() != -1
                && raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()
                && leaderId != raftNode.getLeaderId()) {
            leaderLock.lock();
            if (leaderId != -1 && leaderRpcClient != null) {
                leaderRpcClient.stop();
                leaderRpcClient = null;
                leaderId = -1;
            }
            leaderId = raftNode.getLeaderId();
            Peer peer = raftNode.getPeerMap().get(leaderId);
            Endpoint endpoint = new Endpoint(peer.getServer().getEndpoint().getHost(),
                    peer.getServer().getEndpoint().getPort());
            RpcClientOptions rpcClientOptions = new RpcClientOptions();
            rpcClientOptions.setGlobalThreadPoolSharing(true);
            leaderRpcClient = new RpcClient(endpoint, rpcClientOptions);
            leaderLock.unlock();
        }
    }

}
