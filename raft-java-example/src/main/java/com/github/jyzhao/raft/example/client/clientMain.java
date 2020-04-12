package com.github.jyzhao.raft.example.client;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.github.jyzhao.raft.example.server.service.AuctionService;
import com.googlecode.protobuf.format.JsonFormat;

public class clientMain {
    enum ClientAction {
        CREATE_AUCTIONITEM,
        BID;
    };
    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:3001,127.0.0.1:3002,127.0.0.1:3003");
        AuctionService auctionService = BrpcProxy.getProxy(rpcClient, AuctionService.class);
        final JsonFormat jsonFormat = new JsonFormat();

        ClientAction clientAction = ClientAction.CREATE_AUCTIONITEM;
        switch (clientAction) {
            case CREATE_AUCTIONITEM:
                String owner = "Tony";
                String name = "Gold";
                double minVal = 1234;
                System.out.println(auctionService.createAuctionItem(owner, name, minVal));
                break;
            case BID:
                String bidder = "Tony";
                String biddedItem =  "Gold";
                double bidVal = 2345;
                System.out.println(auctionService.bid(bidder, biddedItem, bidVal));
                break;
        }
    }
}
