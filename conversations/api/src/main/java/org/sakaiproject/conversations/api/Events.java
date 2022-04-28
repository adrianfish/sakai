package org.sakaiproject.conversations.api;

import java.util.stream.Stream;

public enum Events {
    TOPIC_CREATED("conversations.topic.created"),
    TOPIC_DELETED("conversations.topic.deleted"),
    TOPIC_UPDATED("conversations.topic.updated"),
    POST_CREATED("conversations.post.created"),
    POST_DELETED("conversations.post.deleted"),
    POST_RESTORED("conversations.post.restored"),
    POST_UPDATED("conversations.post.updated"),
    REACTED_TO_TOPIC("conversations.topic.reacted"),
    UNREACTED_TO_TOPIC("conversations.topic.unreacted");

    public final String label;

    private Events(String label) {
        this.label = label;
    }

    public static Stream<Events> stream() {
        return Stream.of(Events.values());
    }
}
