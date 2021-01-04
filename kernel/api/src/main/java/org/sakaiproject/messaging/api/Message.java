package org.sakaiproject.messaging.api;

import lombok.Getter;
import lombok.Builder;

@Builder
@Getter
public class Message {

    private String tool;
    private String type;
}
