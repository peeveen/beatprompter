package com.stevenfrew.beatprompter.cache;

import java.io.IOException;

class InvalidBeatPrompterFileException extends IOException {
    InvalidBeatPrompterFileException(String message)
    {
        super(message);
    }
}
