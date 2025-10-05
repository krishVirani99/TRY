
import java.util.*;

/** Minimal Petri net engine used to express interlocking resources. */
class PetriNet {
    static class Place {
        final String name;
        int tokens;

        Place(String name, int tokens) {
            this.name = name;
            this.tokens = tokens;
        }
    }

    static class Transition {
        final String name;
        final Map<Place, Integer> inputs = new HashMap<>();
        final Map<Place, Integer> outputs = new HashMap<>();

        Transition(String name) {
            this.name = name;
        }

        Transition in(Place p, int w) {
            inputs.put(p, w);
            return this;
        }

        Transition out(Place p, int w) {
            outputs.put(p, w);
            return this;
        }

        boolean enabled() {
            for (Map.Entry<Place, Integer> e : inputs.entrySet())
                if (e.getKey().tokens < e.getValue())
                    return false;
            return true;
        }

        void fire() {
            for (Map.Entry<Place, Integer> e : inputs.entrySet())
                e.getKey().tokens -= e.getValue();
            for (Map.Entry<Place, Integer> e : outputs.entrySet())
                e.getKey().tokens += e.getValue();
        }
    }

    final Map<String, Place> places = new HashMap<>();

    Place place(String name, int tokens) {
        return places.computeIfAbsent(name, k -> new Place(k, tokens));
    }

    int tokens(String name) {
        return places.get(name).tokens;
    }
}
