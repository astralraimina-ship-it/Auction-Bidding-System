package common;

public abstract class Entity {
    protected String id;
    protected String name;

    Entity(String _name, String _id){
        name = _name;
        id = _id;
    }
}
