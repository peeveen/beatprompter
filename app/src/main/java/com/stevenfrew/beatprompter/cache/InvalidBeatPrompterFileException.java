package com.stevenfrew.beatprompter.cache;

import java.io.IOException;

public class InvalidBeatPrompterFileException extends IOException {
    public InvalidBeatPrompterFileException(String message)
    {
        super(message);
    }
}
