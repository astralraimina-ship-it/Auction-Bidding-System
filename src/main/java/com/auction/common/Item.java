package common;

public abstract class Item extends Entity {
    protected double startPrice;
    protected String description;
    protected double currentPrice = startPrice;

    Item(String _name, String _id, String _description, double _startPrice){
        super(_name, _id);
        description = _description;
        startPrice = _startPrice;
    }
}
