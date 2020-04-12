package com.github.jyzhao.raft.example.server;

import com.github.wenweihu86.raft.StateMachine;

import org.apache.commons.collections4.map.HashedMap;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionStateMachine implements StateMachine {

    private String raftDataDir;
    private Map<String, Float> kv = new HashedMap<>();

    public AuctionStateMachine(String raftDataDir) {
        this.raftDataDir = raftDataDir;
    }

    @Override
    public void writeSnapshot(String snapshotDir) {
        try {
            FileOutputStream out = new FileOutputStream(snapshotDir);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(kv);
            oos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void readSnapshot(String snapshotDir) {
        Object o = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(snapshotDir));
            o = in.readObject();
            if (o instanceof ConcurrentHashMap) {
                kv = (ConcurrentHashMap)o;
            }
        } catch(IOException e) {
            System.err.println("Could not load file - " + e);
        } catch(ClassNotFoundException e) {
            System.err.println("Could not find class -" + e);
        }
    }

    @Override
    public void apply(byte[] dataBytes) {
        try {
            String req = Arrays.toString(dataBytes);
            String[] reqs = req.split(";");
            String type = reqs[0];
            String key = reqs[1];
            float val = Float.parseFloat(reqs[2]);
            switch (type) {
                case "createAuction":
                    if (!kv.containsKey(key)) {
                        kv.put(key, val);
                    }
                    break;
                case "bid":
                    if (kv.containsKey(key)
                            &&kv.get(key) < val) {
                        kv.put(key, val);
                    }
            }
        } catch (Exception ex) {
            ;
        }
    }

    public StateMachineBidReturn bid(String biddedItem, float bidVal) {
        if (!kv.containsKey(biddedItem)) {
            return new StateMachineBidReturn(false, "no such item!");
        }
        if (bidVal < kv.get(biddedItem)) {
            return new StateMachineBidReturn(false, "you should bid at least" + Double.toString(kv.get(biddedItem)));
        }
        kv.put(biddedItem, bidVal);
        return new StateMachineBidReturn(true, "success");
    }
}
