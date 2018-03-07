package com.stevenfrew.beatprompter;

import java.io.IOException;

class InvalidBeatPrompterFileException extends IOException {
    InvalidBeatPrompterFileException(String message)
    {
        super(message);
    }
}
