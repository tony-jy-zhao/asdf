package com.github.jyzhao.raft.example.server.service;

import com.github.wenweihu86.raft.example.server.service.AuctionProto;

public interface AuctionService {

    AuctionProto.CreateAuctionItemResponse createAuctionItem(AuctionProto.CreateAuctionItemRequest request);

    AuctionProto.BidResponse bid(AuctionProto.BidRequest request);
}
