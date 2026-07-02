package com.campusone.discussion.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull
        Integer voteValue) {

    @JsonIgnore
    @AssertTrue(message = "voteValue must be either -1 or 1")
    public boolean isVoteValueSupported() {
        return voteValue == null || voteValue == -1 || voteValue == 1;
    }
}
