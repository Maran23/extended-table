package tools.maran.extendedtable.table.common;

import java.io.Serial;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/// [Event] designated for a commit.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class CommitEvent<S> extends Event {

    @Serial
    private static final long serialVersionUID = 2724420967630667644L;

    /// Common supertype for all commit events.
    public static final EventType<CommitEvent<?>> ANY = new EventType<>(Event.ANY, "COMMIT");

    /// The item (row) this commit event belongs to.
    private final transient S item;

    /// Creates a new [CommitEvent] instance.
    ///
    /// @param source
    ///         the source which sent the event
    /// @param target
    ///         the target the event is directed to
    /// @param eventType
    ///         the type of the event
    /// @param item
    ///         the item (row) the commit belongs to
    public CommitEvent(Object source, EventTarget target, EventType<? extends Event> eventType, S item) {
        super(source, target, eventType);
        this.item = item;
    }

    /// Returns the item (row) this commit event belongs to.
    ///
    /// @return the item
    public final S getItem() {
        return item;
    }
}
