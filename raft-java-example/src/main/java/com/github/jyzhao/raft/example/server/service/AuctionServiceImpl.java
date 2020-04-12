package com.github.jyzhao.raft.example.server.service;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.Endpoint;
import com.github.jyzhao.raft.example.server.AuctionStateMachine;
import com.github.jyzhao.raft.example.server.StateMachineBidReturn;
import com.github.wenweihu86.raft.Peer;
import com.github.wenweihu86.raft.RaftNode;
import com.github.wenweihu86.raft.example.server.service.AuctionProto;
import com.github.wenweihu86.raft.proto.RaftProto;
import org.apache.commons.collections4.queue.PredicatedQueue;
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
    public AuctionProto.CreateAuctionItemResponse createAuctionItem(AuctionProto.CreateAuctionItemRequest request) {
        String owner = request.getOwner();
        String name = request.getName();
        float minVal = request.getMinVal();

        AuctionProto.CreateAuctionItemResponse.Builder resp =  AuctionProto.CreateAuctionItemResponse.newBuilder();

        if (raftNode.getLeaderId() <= 0) {
            return resp.setResponse("fail").build();
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            AuctionService auctionService = BrpcProxy.getProxy(leaderRpcClient, AuctionService.class);
            return auctionService.createAuctionItem(request);
        } else {
            byte[] data = ("createAuction;"+owner+";"+name+";"+String.valueOf(minVal)).getBytes();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);

            if (success) return resp.setResponse("success").build();
            else return resp.setResponse("fail").build();
        }
    }

    @Override
    public AuctionProto.BidResponse bid(AuctionProto.BidRequest reqest) {
        AuctionProto.BidResponse.Builder resp = AuctionProto.BidResponse.newBuilder();

        if (raftNode.getLeaderId() <= 0) {
            return resp.setResponse("fail").build();
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            AuctionService auctionService = BrpcProxy.getProxy(leaderRpcClient, AuctionService.class);
            return auctionService.bid(reqest);
        } else {
            String biddedItem = reqest.getBiddedItem();
            String bidder = reqest.getBidder();
            float bidVal = reqest.getBidVal();
            StateMachineBidReturn ret = stateMachine.bid(biddedItem, bidVal);
            if (!ret.isSucc) {
                return resp.setResponse(ret.message).build();
            }
            byte[] data = ("bid;"+bidder+";"+biddedItem+";"+String.valueOf(bidVal)).getBytes();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);
            if (success) return resp.setResponse("sucess").build();
            else return resp.setResponse("fail on consensus").build();
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
