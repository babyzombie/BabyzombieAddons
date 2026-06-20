package top.babyzombie.addons.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired before a chat command is sent.
 * Cancellable: return true to cancel the command.
 */
public final class SendCommandEvents {

    public static final Event<BeforeSend> BEFORE_SEND =
            EventFactory.createArrayBacked(BeforeSend.class, callbacks -> command -> {
                for (BeforeSend cb : callbacks) {
                    if (cb.beforeSend(command)) return true;
                }
                return false;
            });

    public static final Event<AfterSend> AFTER_SEND =
            EventFactory.createArrayBacked(AfterSend.class, callbacks -> command -> {
                for (AfterSend cb : callbacks) {
                    cb.afterSend(command);
                }
            });

    @FunctionalInterface
    public interface BeforeSend {
        boolean beforeSend(String command);
    }

    @FunctionalInterface
    public interface AfterSend {
        void afterSend(String command);
    }
}
