package ruby.systems.events;

import java.util.ArrayList;
import java.util.HashMap;

public class EventBuses {
    @FunctionalInterface
    public interface Handler<T> {
        void onFired(T event);
    }

    public static class Single<T extends Event> {
        private final ArrayList<Handler<T>> handlers = new ArrayList<>();
        private final ArrayList<Handler<T>> toAdd = new ArrayList<>();
        private final ArrayList<Integer> toRemove = new ArrayList<>();

        public Handler<T> unregister(int handleIdx) {
            this.toRemove.add(handleIdx);
            return this.handlers.get(handleIdx);
        }

        public int register(Handler<T> handler) {
            this.toAdd.add(handler);
            return this.handlers.size();
        }

        public boolean fireEvent(T argument) {
            this.handlers.addAll(this.toAdd);
            for(int idx : this.toRemove.reversed())
                this.handlers.remove(idx);

            this.toAdd.clear();
            this.toRemove.clear();

            for(Handler<T> handler : new ArrayList<>(this.handlers))
                handler.onFired(argument);

            return argument.isCancelled();
        }
    }

    public static class Many<E extends Enum<E>, T extends Event> {
        private final HashMap<E, ArrayList<Handler<T>>> handlers = new HashMap<>();
        private final HashMap<E, ArrayList<Handler<T>>> toAdd = new HashMap<>();
        private final HashMap<E, ArrayList<Integer>> toRemove = new HashMap<>();

        public Handler<T> unregister(E type, int handleIdx) {
            this.toRemove.computeIfAbsent(type, k -> new ArrayList<>()).add(handleIdx);
            return this.handlers.get(type).get(handleIdx);
        }

        public int register(E type, Handler<T> handler) {
            this.toAdd.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
            return this.handlers.getOrDefault(type, new ArrayList<>()).size();
        }

        public boolean fireEvent(E type, T argument) {
            this.handlers.putIfAbsent(type, new ArrayList<>());
            ArrayList<Handler<T>> list = new ArrayList<>(this.handlers.get(type));

            this.handlers.get(type).addAll(this.toAdd.computeIfAbsent(type, k -> new ArrayList<>()));
            for(int idx : this.toRemove.computeIfAbsent(type, k -> new ArrayList<>()).reversed())
                this.handlers.get(type).remove(idx);

            this.toAdd.get(type).clear();
            this.toRemove.get(type).clear();

            for(Handler<T> handler : list)
                handler.onFired(argument);

            return argument.isCancelled();
        }
    }
}