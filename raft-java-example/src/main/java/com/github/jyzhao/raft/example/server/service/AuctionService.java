package com.github.jyzhao.raft.example.server.service;

public interface AuctionService {

    String createAuctionItem(String owner, String name, double minVal);

    String bid(String bidder, String biddedItem, double bidVal);
}
