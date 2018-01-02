package talzemah.blindglasses;
public class Result {

    String name;
    float score;

    public Result() {
    }

    public Result(String name, float score) {
        this.name = name;
        this.score = score;
    }

    public String getname() {
        return name;
    }

    public float getscore() {
        return score;
    }

    public void setname(String name) {

        this.name = name;
    }

    public void setscore(float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return this.name + " " + this.score;
    }
}
