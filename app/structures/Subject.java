package structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Subject {
    //    observer pattern
    protected List<Observer> observers = new ArrayList<Observer>();


    public void add(Observer observer) {
        observers.add(observer);
    }

    public void remove(Observer observer) {
        observers.remove(observer);
    }

    public void clearObservers(){
        observers = new ArrayList<Observer>();
    }

   
    public abstract void broadcastEvent(Class target,Map<String,Object> parameters);

    public List<Observer> getObservers() {
        return observers;
    }
}
