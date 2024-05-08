import java.io.Serializable;

public class Object implements Serializable{
    public int ID;
    public int content;

    public Object(int ID, int content){
        this.ID = ID;
        this.content = content;
    }
}
