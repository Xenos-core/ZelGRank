package com.zelg.zelgrank.api;

public record RankChangeResult(boolean success, String message) {
    public static RankChangeResult ok() {
        return new RankChangeResult(true, "Success");
    }

    public static RankChangeResult failure(String message) {
        return new RankChangeResult(false, message);
    }
}
