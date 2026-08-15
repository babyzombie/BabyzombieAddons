package top.babyzombie.addons.module.events;

public class EventsModule {
    private EventsModule() {}
    public static void init() {
        GreatSpookModule.init();
        FruitDiggingModule.init();
        RaffleTaskModule.init();
    }
}
