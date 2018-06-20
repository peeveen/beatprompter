package com.stevenfrew.beatprompter.cloud;

public interface CloudListener {
    void onAuthenticationRequired();
    boolean shouldCancel();
}
