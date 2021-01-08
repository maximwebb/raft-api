package maximwebb.app.messages;

import java.util.UUID;

public class ChangeNameMessage extends StatusMessage {
    public final String name;

    public ChangeNameMessage(String name, UUID authorId) {
        super(authorId);
        this.name = name;
    }
}
