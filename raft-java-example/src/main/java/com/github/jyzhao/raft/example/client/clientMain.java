package com.github.jyzhao.raft.example.client;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.github.jyzhao.raft.example.server.service.AuctionService;
import com.github.wenweihu86.raft.example.server.service.AuctionProto;
import com.github.wenweihu86.raft.example.server.service.ExampleProto;
import com.googlecode.protobuf.format.JsonFormat;

public class clientMain {
    enum ClientAction {
        CREATE_AUCTIONITEM,
        BID;
    };
    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient("list://127.0.0.1:30001,127.0.0.1:30002,127.0.0.1:30003");
        AuctionService auctionService = BrpcProxy.getProxy(rpcClient, AuctionService.class);
        final JsonFormat jsonFormat = new JsonFormat();

        ClientAction clientAction = ClientAction.BID;
        switch (clientAction) {
            case CREATE_AUCTIONITEM:
                String owner = "Tony";
                String name = "Gold";
                float minVal = 1234;

                AuctionProto.CreateAuctionItemRequest request = AuctionProto.CreateAuctionItemRequest.newBuilder()
                        .setOwner(owner).setName(name).setMinVal(minVal).build();
                AuctionProto.CreateAuctionItemResponse resp = auctionService.createAuctionItem(request);
                System.out.println(jsonFormat.printToString(resp));
                break;
            case BID:
                String bidder = "Tony";
                String biddedItem =  "Gold";
                float bidVal = 2345;

                AuctionProto.BidRequest request1 = AuctionProto.BidRequest.newBuilder()
                        .setBidder(bidder).setBiddedItem(biddedItem).setBidVal(bidVal).build();
                AuctionProto.BidResponse resp1 = auctionService.bid(request1);
                System.out.println(jsonFormat.printToString(resp1));
                break;
        }
    }
}
