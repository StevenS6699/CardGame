package structures;

import java.util.Map;

public abstract class Observer {
//    observer pattern

    public abstract void trigger(Class target, Map<String,Object> parameters);
}
