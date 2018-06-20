package com.stevenfrew.beatprompter.cloud;

import java.io.IOException;

public class CloudException extends IOException {
    public CloudException(String message)
    {
        super(message);
    }
}
