package com.github.jyzhao.raft.example.server;

public class StateMachineBidReturn {
    public boolean isSucc;
    public String message;
    public StateMachineBidReturn(boolean isSucc, String message) {
        this.isSucc = isSucc;
        this.message = message;
    }
}
